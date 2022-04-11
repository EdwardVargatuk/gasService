package net.bigpoint.assessment.service;

import static net.bigpoint.assessment.TestData.prepareSuccessfulOrders;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasStation;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class GasServiceTest {

    private GasStation gasStation;

    @BeforeEach
    void setUp() {
        gasStation = new GasService();
    }

    @Test
    void addGasPump_ShouldAddNewPump_WhenInvoked() {
        int expectedSize = 1;
        double expectedAmount = 10.0;
        GasType expectedGasType = GasType.DIESEL;
        GasPump gasPump = new GasPump(expectedGasType, expectedAmount);

        gasStation.addGasPump(gasPump);

        assertThat(gasStation.getGasPumps()).hasSize(expectedSize);
        assertThat(gasStation.getGasPumps())
                .anyMatch(pump -> pump.getGasType().equals(expectedGasType))
                .anyMatch(pump -> pump.getRemainingAmount() == expectedAmount);
    }

    @Test
    void addGasPump_ShouldAddManyPumps_WhenInvokedFewTimes() {
        int expectedSize = 3;
        GasPump gasPump = new GasPump(GasType.DIESEL, 10);

        gasStation.addGasPump(gasPump);
        gasStation.addGasPump(gasPump);
        gasStation.addGasPump(gasPump);

        assertThat(gasStation.getGasPumps()).hasSize(expectedSize);
    }

    @Test
    void getGasPumps_ShouldReturnAllPumps_WhenPumpsWasAdded() {
        int expectedSize = 2;
        GasPump gasPump = new GasPump(GasType.DIESEL, 10);
        gasStation.addGasPump(gasPump);
        gasStation.addGasPump(gasPump);

        Collection<GasPump> gasPumps = gasStation.getGasPumps();

        assertThat(gasPumps).hasSize(expectedSize);
    }

    @Test
    void getGasPumps_ShouldReturnZero_WhenPumpsWasNotAdded() {

        Collection<GasPump> gasPumps = gasStation.getGasPumps();

        assertThat(gasPumps).isEmpty();
    }

    @Test
    void getGasPumps_ShouldReturnUnmodifiableCollection_WhenTryingToModify() {
        int expectedSize = 2;
        GasPump gasPump = new GasPump(GasType.DIESEL, 10);
        gasStation.addGasPump(gasPump);
        gasStation.addGasPump(gasPump);

        Collection<GasPump> gasPumps = gasStation.getGasPumps();
        gasPumps.remove(gasPump);
        Collection<GasPump> gasPumpsAfterModifying = gasStation.getGasPumps();

        assertThat(gasPumpsAfterModifying).hasSize(expectedSize);
    }

    @Test
    void getPrice_ShouldReturnPriceForGasType_IfPriceForGasTypeIsSet() {
        double expectedGasPrice = 15.5;
        gasStation.setPrice(GasType.DIESEL, expectedGasPrice);

        double price = gasStation.getPrice(GasType.DIESEL);

        assertThat(price).isEqualTo(expectedGasPrice);
    }

    @Test
    void getPrice_ShouldReturnZeroPrice_IfPriceForGasTypeNotSet() {
        double expectedGasPrice = 0;

        double price = gasStation.getPrice(GasType.DIESEL);

        assertThat(price).isEqualTo(expectedGasPrice);
    }

    @Test
    void setPrice_ShouldSetPriceForGasType_WhenInvoked() {
        double expectedGasPrice = 15.5;

        gasStation.setPrice(GasType.DIESEL, expectedGasPrice);
        double price = gasStation.getPrice(GasType.DIESEL);

        assertThat(price).isEqualTo(expectedGasPrice);
    }

    @Test
    void setPrice_ShouldRewritePriceForGasType_WhenChangedGasPrice() {
        double expectedGasPrice = 15.5;
        gasStation.setPrice(GasType.DIESEL, expectedGasPrice * 100);

        gasStation.setPrice(GasType.DIESEL, expectedGasPrice);
        double price = gasStation.getPrice(GasType.DIESEL);

        assertThat(price).isEqualTo(expectedGasPrice);
    }

    @ParameterizedTest
    @CsvSource({"10.0,500.0", "2.5,125.0"})
    void buyGas_ShouldReturnTotalPriceOfSoldGas_WhenRequestedAmountAreTheSameOrLowerThanAvailableAmount(double requestedAmount,
            double expectedTotalPrice) {
        double amount = 10.0;
        double price = 50.0;
        GasPump gasPump = new GasPump(GasType.DIESEL, amount);
        gasStation.addGasPump(gasPump);
        gasStation.setPrice(GasType.DIESEL, price);
        double totalPrice = 0;

        try {
            totalPrice = gasStation.buyGas(GasType.DIESEL, requestedAmount, price);
        } catch (NotEnoughGasException | GasTooExpensiveException e) {
            log.warn("Test of buying has threw an exception: ", e);
        }

        assertThat(totalPrice).isEqualTo(expectedTotalPrice);
    }

    @ParameterizedTest
    @CsvSource({"50.0,500.0", "1000.0,500.0"})
    void buyGas_ShouldReturnTotalPriceOfSoldGasAccordingToGasPrice_WhenRequestedMaxPriceAreTheSameOrHigherThanCurrentPrice(double requestedMaxPrice,
            double expectedTotalPrice) {
        double amount = 10.0;
        double price = 50.0;
        GasPump gasPump = new GasPump(GasType.DIESEL, amount);
        gasStation.addGasPump(gasPump);
        gasStation.setPrice(GasType.DIESEL, price);
        double totalPrice = 0;

        try {
            totalPrice = gasStation.buyGas(GasType.DIESEL, amount, requestedMaxPrice);
        } catch (NotEnoughGasException | GasTooExpensiveException e) {
            log.warn("Test of buying has threw an exception: ", e);
        }

        assertThat(totalPrice).isEqualTo(expectedTotalPrice);
    }

    @Test
    void buyGas_ShouldThrowException_WhenNotEnoughGasOfTargetType() {

        gasStation.setPrice(GasType.DIESEL, 10.0);

        Assertions.assertThrows(NotEnoughGasException.class, () -> gasStation.buyGas(GasType.DIESEL, 99.0, 10.0));
    }

    @Test
    void buyGas_ShouldThrowException_WhenMaxRequestedPriceIsLowerThanPriceForGasType() {
        GasPump gasPump = new GasPump(GasType.DIESEL, 10.0);
        gasStation.addGasPump(gasPump);
        gasStation.setPrice(GasType.DIESEL, 100.0);

        Assertions.assertThrows(GasTooExpensiveException.class, () -> gasStation.buyGas(GasType.DIESEL, 1.0, 1.0));
    }

    @Test
    void getNumberOfSales_ShouldReturnAllSuccessfulSales_WhenSuccessfulSalesExists() {
        int expectedNumberOfSales = 3;
        prepareSuccessfulOrders(gasStation, 10.0, 1.0, expectedNumberOfSales);

        int numberOfSales = gasStation.getNumberOfSales();

        assertThat(numberOfSales).isEqualTo(expectedNumberOfSales);
    }

    @Test
    void getNumberOfSales_ShouldReturnZero_WhenSuccessfulSalesAbsent() {

        int numberOfSales = gasStation.getNumberOfSales();

        assertThat(numberOfSales).isZero();
    }

    @ParameterizedTest
    @CsvSource({"5.0,5.0", "10.5,1.1"})
    void getRevenue_ShouldReturnRevenueAccordingToAllSuccessfulSales_WhenSuccessfulSalesExists(double orderPrice, double amountInLiters) {
        int requestsCount = 3;
        double expectedRevenue = orderPrice * amountInLiters * requestsCount;
        prepareSuccessfulOrders(gasStation, orderPrice, amountInLiters, requestsCount);

        double revenue = gasStation.getRevenue();

        assertThat(revenue).isEqualTo(expectedRevenue);
    }

    @Test
    void getRevenue_ShouldReturnZero_WhenSuccessfulSalesAbsent() {

        double revenue = gasStation.getRevenue();

        assertThat(revenue).isZero();
    }

    @Test
    void getNumberOfCancellationsNoGas_ShouldReturnCorrectNumberOfFailedTransactions_WhenBuyGasIsFailedBecauseWasNoGas() {
        int expectedFailedOrdersNumber = 1;
        gasStation.setPrice(GasType.DIESEL, 10.0);

        try {
            gasStation.buyGas(GasType.DIESEL, 99.0, 10.0);
        } catch (NotEnoughGasException | GasTooExpensiveException e) {
            log.info("Cancelled transaction of buying gas with NotEnoughGasException");
        }

        int numberOfCancellationsTooExpensive = gasStation.getNumberOfCancellationsNoGas();

        assertThat(numberOfCancellationsTooExpensive).isEqualTo(expectedFailedOrdersNumber);
    }

    @Test
    void getNumberOfCancellationsTooExpensive_ShouldReturnCorrectNumberOfFailedTransactions_WhenBuyGasIsFailedBecauseGasWasTooExpensive() {
        int expectedFailedOrdersNumber = 1;
        GasPump gasPump = new GasPump(GasType.DIESEL, 10.0);
        gasStation.addGasPump(gasPump);
        gasStation.setPrice(GasType.DIESEL, 100.0);

        try {
            gasStation.buyGas(GasType.DIESEL, 1.0, 1.0);
        } catch (NotEnoughGasException | GasTooExpensiveException e) {
            log.info("Cancelled transaction of buying gas with GasTooExpensiveException");
        }

        int numberOfCancellationsTooExpensive = gasStation.getNumberOfCancellationsTooExpensive();

        assertThat(numberOfCancellationsTooExpensive).isEqualTo(expectedFailedOrdersNumber);
    }
}
