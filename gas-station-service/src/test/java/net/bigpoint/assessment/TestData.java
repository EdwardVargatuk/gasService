package net.bigpoint.assessment;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasStation;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestData {

    public static void prepareSuccessfulOrders(GasStation gasStation, double ordersPrice, double amount, int requestsCount) {
        GasPump gasPump = new GasPump(GasType.DIESEL, 100);
        gasStation.addGasPump(gasPump);
        gasStation.setPrice(GasType.DIESEL, ordersPrice);

        try {
            for (int i = 0; i < requestsCount; i++) {
                gasStation.buyGas(GasType.DIESEL, amount, ordersPrice);
            }
        } catch (NotEnoughGasException | GasTooExpensiveException e) {
            e.printStackTrace();
        }
    }

}
