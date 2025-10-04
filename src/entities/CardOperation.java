package entities;

import java.time.LocalDateTime;
import java.util.UUID;

import enums.OperationType;

public record CardOperation(
        UUID id,
        LocalDateTime date,
        double amount,
        String type,
        String location,
        int cardId) {
    public static final String TABLE_NAME = "card_operations";

    public CardOperation(LocalDateTime date, double amount, String type, String location, int cardId) {
        this(UUID.randomUUID(), date, amount, type, location, cardId);
    }

    // Constructor with enum for business logic convenience
    public CardOperation(LocalDateTime date, double amount, OperationType type, String location, int cardId) {
        this(UUID.randomUUID(), date, amount, type.name(), location, cardId);
    }

    public OperationType getTypeEnum() {
        return OperationType.valueOf(type);
    }
}
