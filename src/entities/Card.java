package entities;

import java.util.UUID;

import enums.CardStatus;
import enums.CardType;

abstract public sealed class Card permits CreditCard, DebitCard, PrepaidCard {
    private int id;
    private UUID card_number;
    private String expiration_date;
    private String status; // String for DB/reflection compatibility
    private String card_type; // String for DB/reflection compatibility
    private int user_id;

    public Card(int id, String expirationDate, String status, String cardType, int userId) {
        this.id = id;
        this.card_number = UUID.randomUUID();
        this.expiration_date = expirationDate;
        this.status = status;
        this.card_type = cardType;
        this.user_id = userId;
    }

    public int getId() {
        return id;
    }

    public UUID getCardNumber() {
        return card_number;
    }

    public String getExpirationDate() {
        return expiration_date;
    }

    public String getStatus() {
        return status;
    }

    public String getCardType() {
        return card_type;
    }

    public int getUserId() {
        return user_id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCardNumber(UUID cardNumber) {
        this.card_number = cardNumber;
    }

    public void setExpirationDate(String expirationDate) {
        this.expiration_date = expirationDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCardType(String cardType) {
        this.card_type = cardType;
    }

    public void setUserId(int userId) {
        this.user_id = userId;
    }

    // Enum helper methods for business logic
    public CardStatus getStatusEnum() {
        return CardStatus.valueOf(status);
    }

    public CardType getCardTypeEnum() {
        return CardType.valueOf(card_type);
    }

    public void setStatus(CardStatus status) {
        this.status = status.name();
    }

    public void setCardType(CardType cardType) {
        this.card_type = cardType.name();
    }

}
