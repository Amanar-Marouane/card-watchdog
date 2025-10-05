package entities;

import enums.AlertLevel;

public record FraudAlert(
        int id,
        String description,
        String level,
        int cardId) {

    // Constructor with enum for business logic convenience
    public FraudAlert(int id, String description, AlertLevel level, int cardId) {
        this(id, description, level.name(), cardId);
    }

    public AlertLevel getLevelEnum() {
        return AlertLevel.valueOf(level);
    }
}
