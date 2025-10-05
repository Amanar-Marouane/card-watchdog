package services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import entities.Card;
import entities.CardOperation;
import entities.CreditCard;
import entities.DebitCard;
import entities.PrepaidCard;
import enums.CardStatus;
import enums.OperationType;
import repositories.CardOperationRepository;
import repositories.CardRepository;
import utils.Console;

public class CardOperationService {
    private final CardOperationRepository cardOperationRepository;
    private final CardRepository cardRepository;

    public CardOperationService(CardOperationRepository cardOperationRepository, CardRepository cardRepository) {
        this.cardOperationRepository = cardOperationRepository;
        this.cardRepository = cardRepository;
    }

    /**
     * Creates a new card operation
     * 
     * @param cardId        ID of the card
     * @param amount        Amount of the transaction
     * @param operationType Type of operation
     * @param location      Location where the transaction occurred
     * @return The created card operation
     * @throws Exception If the operation cannot be created
     */
    public CardOperation createCardOperation(String cardId, double amount, OperationType operationType, String location)
            throws Exception {
        Optional<Card> cardOpt = cardRepository.findById(cardId);
        if (cardOpt.isEmpty()) {
            throw new Exception("Card not found");
        }

        Card card = cardOpt.get();
        if (!CardStatus.ACTIVE.name().equals(card.getStatus())) {
            throw new Exception("Card is not active");
        }

        // Check if the operation exceeds limits based on card type
        checkOperationLimit(card, amount);

        // Create operation data
        LocalDateTime now = LocalDateTime.now();
        UUID operationId = UUID.randomUUID();

        Map<String, Object> operationData = new HashMap<>();
        operationData.put("id", operationId.toString()); // Store as string
        operationData.put("date", now);
        operationData.put("amount", amount);
        operationData.put("type", operationType.name());
        operationData.put("location", location);
        operationData.put("card_id", card.getId());

        // Create in database and return the created operation
        try {
            return cardOperationRepository.create(operationData);
        } catch (Exception e) {
            Console.error("Error creating operation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Get all operations for a specific card
     * 
     * @param cardId ID of the card
     * @return List of card operations
     */
    public List<CardOperation> getCardOperations(String cardId) {
        return cardOperationRepository.findCardOperationsOf(cardId);
    }

    /**
     * Get all operations for a user's cards
     * 
     * @param userId ID of the user
     * @return List of card operations
     */
    public List<CardOperation> getUserCardOperations(int userId) {
        List<Card> userCards = cardRepository.findAll()
                .stream()
                .filter(card -> card.getUserId() == userId)
                .collect(Collectors.toList());

        return userCards.stream()
                .flatMap(card -> cardOperationRepository.findCardOperationsOf(String.valueOf(card.getId())).stream())
                .collect(Collectors.toList());
    }

    /**
     * Filter operations by type
     * 
     * @param operations List of operations
     * @param type       Operation type
     * @return Filtered list of operations
     */
    public List<CardOperation> filterOperationsByType(List<CardOperation> operations, OperationType type) {
        return operations.stream()
                .filter(op -> op.getTypeEnum() == type)
                .collect(Collectors.toList());
    }

    /**
     * Filter operations by date range
     * 
     * @param operations List of operations
     * @param from       Start date
     * @param to         End date
     * @return Filtered list of operations
     */
    public List<CardOperation> filterOperationsByDateRange(List<CardOperation> operations, LocalDateTime from,
            LocalDateTime to) {
        return operations.stream()
                .filter(op -> !op.date().isBefore(from) && !op.date().isAfter(to))
                .collect(Collectors.toList());
    }

    /**
     * Check if the operation exceeds the card's limit
     * 
     * @param card   Card to check
     * @param amount Amount of the transaction
     * @throws Exception If the operation exceeds the limit
     */
    private void checkOperationLimit(Card card, double amount) throws Exception {
        switch (card.getCardTypeEnum()) {
            case DEBIT:
                checkDebitCardLimit(card, amount);
                break;
            case CREDIT:
                checkCreditCardLimit(card, amount);
                break;
            case PREPAID:
                checkPrepaidCardBalance(card, amount);
                break;
        }
    }

    private void checkDebitCardLimit(Card card, double amount) throws Exception {
        if (card instanceof DebitCard) {
            DebitCard debitCard = (DebitCard) card;

            // Get today's operations for this card
            String cardId = String.valueOf(card.getId());
            List<CardOperation> todaysOperations = cardOperationRepository.findCardOperationsOf(cardId)
                    .stream()
                    .filter(op -> op.date().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                    .collect(Collectors.toList());

            double todaysTotal = todaysOperations.stream().mapToDouble(CardOperation::amount).sum() + amount;

            if (todaysTotal > debitCard.getDailyLimit()) {
                throw new Exception("Operation exceeds daily limit of " + debitCard.getDailyLimit());
            }
        }
    }

    private void checkCreditCardLimit(Card card, double amount) throws Exception {
        if (card instanceof CreditCard) {
            CreditCard creditCard = (CreditCard) card;

            // Get this month's operations
            String cardId = String.valueOf(card.getId());
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                    .withNano(0);

            List<CardOperation> monthsOperations = cardOperationRepository.findCardOperationsOf(cardId)
                    .stream()
                    .filter(op -> !op.date().isBefore(startOfMonth))
                    .collect(Collectors.toList());

            double monthsTotal = monthsOperations.stream().mapToDouble(CardOperation::amount).sum() + amount;

            if (monthsTotal > creditCard.getMonthlyLimit()) {
                throw new Exception("Operation exceeds monthly credit limit of " + creditCard.getMonthlyLimit());
            }
        }
    }

    private void checkPrepaidCardBalance(Card card, double amount) throws Exception {
        if (card instanceof PrepaidCard) {
            PrepaidCard prepaidCard = (PrepaidCard) card;

            if (amount > prepaidCard.getAvailableBalance()) {
                throw new Exception("Insufficient balance on prepaid card");
            }

            // Update the balance after successful operation
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("available_balance", prepaidCard.getAvailableBalance() - amount);
            cardRepository.update(prepaidCard, updateData);
        }
    }
}