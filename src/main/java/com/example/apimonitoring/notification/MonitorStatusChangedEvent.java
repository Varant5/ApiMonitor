package com.example.apimonitoring.notification;

import java.time.Instant;

/**
 * Published when a monitor goes DOWN or comes back UP.
 *
 * Carries plain values (no JPA entities), so listeners can run in another thread
 * without touching lazy-loaded relations.
 *
 * @param previousStatus status of the previous check, or null if this is the first check
 * @param errorMessage   e.g. "Read timed out"; null for a normal HTTP response
 */
public record MonitorStatusChangedEvent(
        Long monitorId,
        String monitorName,
        String monitorUrl,
        Long ownerId,
        String previousStatus,
        String currentStatus,
        Integer httpStatusCode,
        String errorMessage,
        Instant checkedAt
) {

    public boolean isRecovery() {
        return "UP".equals(currentStatus);
    }

    /**
     * Notify when the status really changed:
     * UP -> DOWN (outage), DOWN -> UP (recovery), and a first check that is already DOWN.
     * A first check that is UP is the normal state, so it is not worth an email.
     */
    public static boolean isWorthNotifying(String previousStatus, String currentStatus) {
        if (previousStatus == null) {
            return "DOWN".equals(currentStatus);
        }
        return !previousStatus.equals(currentStatus);
    }
}
