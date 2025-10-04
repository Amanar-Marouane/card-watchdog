package entities;

import java.util.UUID;

import enums.AlertLevel;

public record FraudAlert(
        UUID id,
        String description,
        String level,
        int cardId) {
    public static final String TABLE_NAME = "fraud_alerts";

    public FraudAlert(String description, String level, int cardId) {
        this(UUID.randomUUID(), description, level, cardId);
    }

    // Constructor with enum for business logic convenience
    public FraudAlert(String description, AlertLevel level, int cardId) {
        this(UUID.randomUUID(), description, level.name(), cardId);
    }

    public AlertLevel getLevelEnum() {
        return AlertLevel.valueOf(level);
    }
}
