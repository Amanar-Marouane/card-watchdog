package entities;

import java.util.UUID;

import enums.AlertLevel;

public record FraudAlert(
        UUID id,
        String description,
        String niveau, // String for DB/reflection compatibility
        int idCarte) { // Changed from UUID to int
    public static final String TABLE_NAME = "fraud_alerts";

    public FraudAlert(String description, String niveau, int idCarte) {
        this(UUID.randomUUID(), description, niveau, idCarte);
    }

    // Constructor with enum for business logic convenience
    public FraudAlert(String description, AlertLevel niveau, int idCarte) {
        this(UUID.randomUUID(), description, niveau.name(), idCarte);
    }

    public AlertLevel getNiveauEnum() {
        return AlertLevel.valueOf(niveau);
    }
}
