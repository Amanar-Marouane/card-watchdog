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

    public CardOperation(String id, LocalDateTime date, double amount, String type, String location, int cardId) {
        this(UUID.fromString(id), date, amount, type, location, cardId);
    }

    // Simple constructor that generates a UUID automatically
    public CardOperation(LocalDateTime date, double amount, String type, String location, int cardId) {
        this(UUID.randomUUID(), date, amount, type, location, cardId);
    }

    // Constructor with enum for business logic convenience
    public CardOperation(LocalDateTime date, double amount, OperationType type, String location, int cardId) {
        this(UUID.randomUUID(), date, amount, type.name(), location, cardId);
    }

    public CardOperation(String id, LocalDateTime date, double amount, OperationType type, String location,
            int cardId) {
        this(UUID.fromString(id), date, amount, type.name(), location, cardId);
    }

    public OperationType getTypeEnum() {
        return OperationType.valueOf(type);
    }
}
