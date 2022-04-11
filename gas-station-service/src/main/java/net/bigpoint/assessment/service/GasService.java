package net.bigpoint.assessment.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasStation;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;
import net.bigpoint.assessment.model.FailureReason;
import net.bigpoint.assessment.model.Order;

/**
 * Implementation of GasStation that simulates buying gas by customers. Is Thread-safe
 */
public class GasService implements GasStation {

    private final ReentrantLock reLock = new ReentrantLock(true);
    private final List<GasPump> pumps = Collections.synchronizedList(new ArrayList<>());
    private final List<Order> orders = new ArrayList<>();
    private final Map<GasType, Double> prices = new ConcurrentHashMap<>();

    @Override
    public void addGasPump(GasPump gasPump) {
        pumps.add(gasPump);
    }

    @Override
    public Collection<GasPump> getGasPumps() {
        return new ArrayList<>(pumps);
    }

    @Override
    public double buyGas(GasType gasType, double amountInLiters, double maxPricePerLiter) throws NotEnoughGasException, GasTooExpensiveException {
        double price = getPrice(gasType);
        validateGasPrice(maxPricePerLiter, price);

        pumpGas(gasType, amountInLiters);

        double totalPrice = calcTotalPrice(amountInLiters, price);
        addOrder(totalPrice, true, null);
        return totalPrice;
    }

    @Override
    public double getRevenue() {
        return orders.stream()
                .filter(Order::isSuccess)
                .mapToDouble(Order::getTotalPrice)
                .sum();
    }

    @Override
    public int getNumberOfSales() {
        return (int) orders.stream()
                .filter(Order::isSuccess)
                .count();
    }

    @Override
    public int getNumberOfCancellationsNoGas() {
        return (int) orders.stream()
                .filter(order -> !order.isSuccess()).map(Order::getFailureReason)
                .filter(reason -> reason != null && reason.equals(FailureReason.NOT_ENOUGH_GAS))
                .count();
    }

    @Override
    public int getNumberOfCancellationsTooExpensive() {
        return (int) orders.stream()
                .filter(order -> !order.isSuccess()).map(Order::getFailureReason)
                .filter(reason -> reason != null && reason.equals(FailureReason.GAS_EXPENSIVE))
                .count();
    }

    @Override
    public double getPrice(GasType gasType) {
        Double price = prices.get(gasType);
        return price != null ? price : 0;
    }

    @Override
    public void setPrice(GasType gasType, double price) {
        prices.put(gasType, price);
    }

    /**
     * Thread-safe process of pumping gas
     * Also include choosing of pump to avoid a situation where one thread is now pumping a large amount of gas
     * and other threads are trying to buy gas and may get the same pump, that could be already empty after the pumping by previous thread.
     *
     * @param gasType desired gas type the customer wants to buy
     * @param amount  The amount of gas the customer wants to buy
     * @throws NotEnoughGasException in case if not enough gas of target type can be provided by any single gasPump
     */
    private void pumpGas(GasType gasType, double amount) throws NotEnoughGasException {
        reLock.lock();
        try {
            GasPump gasPump = chooseGasPump(gasType, amount);
            gasPump.pumpGas(amount);
        } finally {
            reLock.unlock();
        }
    }

    private double calcTotalPrice(double amountInLiters, double price) {
        return amountInLiters * price;
    }

    /**
     * Returns GasPump for pumping gas
     * Chooses any random available gasPump
     *
     * @param gasType        desired gas type the customer wants to buy
     * @param amountInLiters The amount of gas the customer wants to buy
     * @return GasPump available for gas pumping
     * @throws NotEnoughGasException in case if not enough gas of target type can be provided by any single gasPump
     */
    private GasPump chooseGasPump(GasType gasType, double amountInLiters) throws NotEnoughGasException {
        Optional<GasPump> pumpOptional = pumps.stream()
                .filter(gasPump -> gasPump.getGasType().equals(gasType))
                .filter(gasPump -> gasPump.getRemainingAmount() >= amountInLiters)
                .findAny();
        if (pumpOptional.isEmpty()) {
            addOrder(0, false, FailureReason.NOT_ENOUGH_GAS);
            throw new NotEnoughGasException();
        }
        return pumpOptional.get();
    }


    private void validateGasPrice(double maxPricePerLiter, double price) throws GasTooExpensiveException {
        if (price > maxPricePerLiter) {
            addOrder(0, false, FailureReason.GAS_EXPENSIVE);
            throw new GasTooExpensiveException();
        }
    }

    /**
     * Added new order to the orders. Thread-safe method
     *
     * @param totalPrice    total price of order
     * @param success       status of the order
     * @param failureReason optional reason in case of failure
     */
    private synchronized void addOrder(double totalPrice, boolean success, FailureReason failureReason) {
        orders.add(new Order(totalPrice, success, failureReason, Instant.now()));
    }
}
