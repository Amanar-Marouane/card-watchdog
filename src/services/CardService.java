package services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import entities.Card;
import entities.CreditCard;
import entities.DebitCard;
import entities.PrepaidCard;
import enums.CardStatus;
import enums.CardType;
import repositories.CardRepository;

public class CardService {
    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public List<Card> getUserCards(int userId) {
        return cardRepository.findAll().stream()
                .filter(card -> card.getUserId() == userId)
                .collect(Collectors.toList());
    }

    public Optional<Card> getCardById(String cardId) {
        return cardRepository.findById(cardId);
    }

    public Card createDebitCard(int userId, int offerIndex) throws Exception {
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("card_number", UUID.randomUUID().toString());
        cardData.put("expiration_date", generateExpirationDate());
        cardData.put("status", CardStatus.ACTIVE.name());
        cardData.put("card_type", CardType.DEBIT.name());
        cardData.put("user_id", userId);
        cardData.put("offer", offerIndex);

        return cardRepository.create(cardData);
    }

    public Card createCreditCard(int userId, int offerIndex) throws Exception {
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("card_number", UUID.randomUUID().toString());
        cardData.put("expiration_date", generateExpirationDate());
        cardData.put("status", CardStatus.ACTIVE.name());
        cardData.put("card_type", CardType.CREDIT.name());
        cardData.put("user_id", userId);
        cardData.put("offer", offerIndex);

        return cardRepository.create(cardData);
    }

    public Card createPrepaidCard(int userId, int offerIndex) throws Exception {
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("card_number", UUID.randomUUID().toString());
        cardData.put("expiration_date", generateExpirationDate());
        cardData.put("status", CardStatus.ACTIVE.name());
        cardData.put("card_type", CardType.PREPAID.name());
        cardData.put("user_id", userId);
        cardData.put("offer", offerIndex);

        return cardRepository.create(cardData);
    }

    public void activateCard(String cardId) throws Exception {
        Optional<Card> cardOpt = cardRepository.findById(cardId);
        if (cardOpt.isEmpty()) {
            throw new Exception("Card not found");
        }

        Card card = cardOpt.get();
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", CardStatus.ACTIVE.name());
        cardRepository.update(card, updateData);
    }

    public void suspendCard(String cardId) throws Exception {
        Optional<Card> cardOpt = cardRepository.findById(cardId);
        if (cardOpt.isEmpty()) {
            throw new Exception("Card not found");
        }

        Card card = cardOpt.get();
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", CardStatus.SUSPENDED.name());
        cardRepository.update(card, updateData);
    }

    public void blockCard(String cardId) throws Exception {
        Optional<Card> cardOpt = cardRepository.findById(cardId);
        if (cardOpt.isEmpty()) {
            throw new Exception("Card not found");
        }

        Card card = cardOpt.get();
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", CardStatus.BLOCKED.name());
        cardRepository.update(card, updateData);
    }

    public void renewCard(String cardId) throws Exception {
        Optional<Card> cardOpt = cardRepository.findById(cardId);
        if (cardOpt.isEmpty()) {
            throw new Exception("Card not found");
        }

        Card card = cardOpt.get();
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("expiration_date", generateExpirationDate());
        updateData.put("status", CardStatus.ACTIVE.name());
        cardRepository.update(card, updateData);
    }

    public void deleteCard(String cardId) {
        cardRepository.deleteById(cardId);
    }

    private String generateExpirationDate() {
        LocalDate expirationDate = LocalDate.now().plusYears(3);
        return expirationDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
    }

    public String getCardDetails(Card card) {
        StringBuilder details = new StringBuilder();
        details.append("ID: ").append(card.getId()).append("\n");
        details.append("Card Number: ").append(card.getCardNumber()).append("\n");
        details.append("Expiration: ").append(card.getExpirationDate()).append("\n");
        details.append("Status: ").append(card.getStatus()).append("\n");
        details.append("Type: ").append(card.getCardType()).append("\n");

        if (card instanceof DebitCard) {
            DebitCard debitCard = (DebitCard) card;
            details.append("Daily Limit: ").append(debitCard.getDailyLimit()).append("\n");
        } else if (card instanceof CreditCard) {
            CreditCard creditCard = (CreditCard) card;
            details.append("Monthly Limit: ").append(creditCard.getMonthlyLimit()).append("\n");
            details.append("Interest Rate: ").append(creditCard.getInterestRate()).append("%\n");
        } else if (card instanceof PrepaidCard) {
            PrepaidCard prepaidCard = (PrepaidCard) card;
            details.append("Available Balance: ").append(prepaidCard.getAvailableBalance()).append("\n");
        }

        return details.toString();
    }
}
