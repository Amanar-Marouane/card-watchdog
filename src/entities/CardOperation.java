package entities;

import java.time.LocalDateTime;
import java.util.UUID;

import enums.OperationType;

public record CardOperation(
        UUID id,
        LocalDateTime date,
        double montant,
        String type, // String for DB/reflection compatibility
        String lieu,
        int idCarte) { // Changed from UUID to int
    public static final String TABLE_NAME = "card_operations";

    public CardOperation(LocalDateTime date, double montant, String type, String lieu, int idCarte) {
        this(UUID.randomUUID(), date, montant, type, lieu, idCarte);
    }

    // Constructor with enum for business logic convenience
    public CardOperation(LocalDateTime date, double montant, OperationType type, String lieu, int idCarte) {
        this(UUID.randomUUID(), date, montant, type.name(), lieu, idCarte);
    }

    public OperationType getTypeEnum() {
        return OperationType.valueOf(type);
    }
}
