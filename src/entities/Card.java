package entities;

import java.util.UUID;

import enums.CardStatus;
import enums.CardType;

abstract public sealed class Card permits CreditCard, DebitCard, PrepaidCard {
    private int id;
    private UUID cardNumber;
    private String expirationDate;
    private String status; // String for DB/reflection compatibility
    private String cardType; // String for DB/reflection compatibility
    private int userId;

    public Card(int id, String expirationDate, String status, String cardType, int userId) {
        this.id = id;
        this.cardNumber = UUID.randomUUID();
        this.expirationDate = expirationDate;
        this.status = status;
        this.cardType = cardType;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public UUID getCardNumber() {
        return cardNumber;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getStatus() {
        return status;
    }

    public String getCardType() {
        return cardType;
    }

    public int getUserId() {
        return userId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCardNumber(UUID cardNumber) {
        this.cardNumber = cardNumber;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Enum helper methods for business logic
    public CardStatus getStatusEnum() {
        return CardStatus.valueOf(status);
    }

    public CardType getCardTypeEnum() {
        return CardType.valueOf(cardType);
    }

    public void setStatus(CardStatus status) {
        this.status = status.name();
    }

    public void setCardType(CardType cardType) {
        this.cardType = cardType.name();
    }

}
