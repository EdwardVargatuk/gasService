package net.bigpoint.assessment.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Simulation of stored data about clients order
 */
@EqualsAndHashCode
@AllArgsConstructor
@Getter
public final class Order {
    private double totalPrice;
    private boolean success;
    private final FailureReason failureReason;
    private final Instant createdAt;
}
