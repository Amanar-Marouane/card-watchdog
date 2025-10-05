package services;

import entities.Card;
import entities.CardOperation;
import entities.FraudAlert;
import enums.AlertLevel;
import enums.CardStatus;
import repositories.CardOperationRepository;
import repositories.CardRepository;
import repositories.FraudAlertRepository;
import utils.Console;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FraudDetectionService {
    private final FraudAlertRepository fraudAlertRepository;
    private final CardRepository cardRepository;
    private final CardOperationRepository cardOperationRepository;

    // Define threshold constants
    private static final double DEBIT_HIGH_AMOUNT_THRESHOLD = 10000.0;
    private static final double CREDIT_HIGH_AMOUNT_THRESHOLD = 20000.0;
    private static final double PREPAID_HIGH_AMOUNT_THRESHOLD = 5000.0;
    private static final int RAPID_LOCATION_CHANGE_MINUTES = 10;
    private static final int MULTIPLE_TRANSACTIONS_MINUTES = 2;
    private static final int MULTIPLE_TRANSACTIONS_COUNT = 3;
    private static final int ESCALATION_HOURS = 24;

    public FraudDetectionService(
            FraudAlertRepository fraudAlertRepository,
            CardRepository cardRepository,
            CardOperationRepository cardOperationRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.cardRepository = cardRepository;
        this.cardOperationRepository = cardOperationRepository;
    }

    public boolean checkForFraud(Card card, CardOperation newOperation) {
        // Check card status first
        if (!CardStatus.ACTIVE.name().equals(card.getStatus())) {
            createAlert("Transaction attempted on non-active card", AlertLevel.AVERTISSEMENT,
                    String.valueOf(card.getId()));
            return true;
        }

        // Get recent operations for this card
        List<CardOperation> recentOperations = cardOperationRepository
                .findCardOperationsOf(String.valueOf(card.getId()));

        // Check for high amount transactions
        if (isHighAmountTransaction(card, newOperation.amount())) {
            return true;
        }

        // Check for rapid geographical changes
        if (checkRapidGeographicalChange(recentOperations, newOperation)) {
            return true;
        }

        // Check for multiple transactions in short time
        if (checkMultipleTransactionsShortTime(recentOperations, newOperation)) {
            return true;
        }

        // Check for escalation (multiple warnings in last 24 hours)
        if (checkForEscalation(String.valueOf(card.getId()))) {
            return true;
        }

        return false;
    }

    private boolean isHighAmountTransaction(Card card, double amount) {
        double threshold;
        AlertLevel level;

        switch (card.getCardTypeEnum()) {
            case DEBIT:
                threshold = DEBIT_HIGH_AMOUNT_THRESHOLD;
                break;
            case CREDIT:
                threshold = CREDIT_HIGH_AMOUNT_THRESHOLD;
                break;
            case PREPAID:
                threshold = PREPAID_HIGH_AMOUNT_THRESHOLD;
                break;
            default:
                threshold = DEBIT_HIGH_AMOUNT_THRESHOLD; // Default
        }

        if (amount > threshold) {
            level = amount > threshold * 1.5 ? AlertLevel.CRITIQUE : AlertLevel.AVERTISSEMENT;
            String message = "High amount transaction detected: " + amount + " (threshold: " + threshold + ")";

            createAlert(message, level, String.valueOf(card.getId()));
            updateCardStatus(card, level);
            return true;
        }
        return false;
    }

    private boolean checkRapidGeographicalChange(List<CardOperation> operations, CardOperation newOperation) {
        for (CardOperation op : operations) {
            // Skip if it's the same location
            if (op.location().equals(newOperation.location())) {
                continue;
            }

            // Check time difference (less than 10 minutes)
            Duration duration = Duration.between(op.date(), newOperation.date());
            if (Math.abs(duration.toMinutes()) < RAPID_LOCATION_CHANGE_MINUTES) {
                String message = "Rapid geographical change detected: "
                        + op.location() + " to " + newOperation.location()
                        + " in " + duration.toMinutes() + " minutes";

                createAlert(message, AlertLevel.CRITIQUE, String.valueOf(newOperation.cardId()));
                updateCardStatus(cardRepository.findById(String.valueOf(newOperation.cardId())).orElse(null),
                        AlertLevel.CRITIQUE);
                return true;
            }
        }
        return false;
    }

    private boolean checkMultipleTransactionsShortTime(List<CardOperation> operations, CardOperation newOperation) {
        // Filter operations in the last few minutes
        LocalDateTime cutoffTime = newOperation.date().minusMinutes(MULTIPLE_TRANSACTIONS_MINUTES);

        List<CardOperation> recentOps = operations.stream()
                .filter(op -> !op.id().equals(newOperation.id()))
                .filter(op -> op.date().isAfter(cutoffTime))
                .collect(Collectors.toList());

        // Count recent operations + the new one
        if (recentOps.size() + 1 >= MULTIPLE_TRANSACTIONS_COUNT) {
            String message = "Multiple transactions detected in short time: "
                    + (recentOps.size() + 1) + " transactions in less than "
                    + MULTIPLE_TRANSACTIONS_MINUTES + " minutes";

            createAlert(message, AlertLevel.AVERTISSEMENT, String.valueOf(newOperation.cardId()));
            updateCardStatus(cardRepository.findById(String.valueOf(newOperation.cardId())).orElse(null),
                    AlertLevel.AVERTISSEMENT);
            return true;
        }
        return false;
    }

    private boolean checkForEscalation(String cardId) {
        List<FraudAlert> recentAlerts = fraudAlertRepository.findByCardId(Integer.parseInt(cardId)).stream()
                .filter(alert -> alert.getLevelEnum() == AlertLevel.AVERTISSEMENT)
                .collect(Collectors.toList());

        // If we have multiple warnings, escalate to CRITIQUE
        if (recentAlerts.size() >= 2) {
            String message = "Escalation: Multiple warnings detected in last " + ESCALATION_HOURS + " hours";
            createAlert(message, AlertLevel.CRITIQUE, cardId);
            updateCardStatus(cardRepository.findById(cardId).orElse(null), AlertLevel.CRITIQUE);
            return true;
        }
        return false;
    }

    private void createAlert(String description, AlertLevel level, String cardId) {
        try {
            // Log a clearer alert message
            switch (level) {
                case INFO:
                    Console.info("Fraud Detection - INFO: " + description + " [Card ID: " + cardId + "]");
                    break;

                case AVERTISSEMENT:
                    Console.warn("Fraud Detection - WARNING: " + description + " [Card ID: " + cardId + "]");
                    Console.warn("Transaction will be declined and card suspended");
                    break;

                case CRITIQUE:
                    Console.alert("Fraud Detection - CRITICAL: " + description + " [Card ID: " + cardId + "]");
                    Console.alert("Transaction will be declined and card blocked");
                    break;
            }

            // Create the fraud alert record
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("description", description);
            alertData.put("level", level.name());
            alertData.put("card_id", cardId);

            fraudAlertRepository.create(alertData);

            // Update card status immediately
            updateCardStatus(cardRepository.findById(cardId).orElse(null), level);

        } catch (Exception e) {
            Console.error("Error creating fraud alert: " + e.getMessage());
        }
    }

    private void updateCardStatus(Card card, AlertLevel level) {
        if (card == null)
            return;

        try {
            Map<String, Object> updateData = new HashMap<>();

            switch (level) {
                case AVERTISSEMENT:
                    updateData.put("status", CardStatus.SUSPENDED.name());
                    Console.warn("Card " + card.getId() + " has been suspended due to potential fraud");
                    break;

                case CRITIQUE:
                    updateData.put("status", CardStatus.BLOCKED.name());
                    Console.error("Card " + card.getId() + " has been blocked due to potential fraud");
                    break;

                default:
                    // No status change for INFO level
                    break;
            }

            if (!updateData.isEmpty()) {
                cardRepository.update(card, updateData);
            }
        } catch (Exception e) {
            Console.error("Error updating card status: " + e.getMessage());
        }
    }

    public boolean canProcessOperation(Card card) {
        // Check if the card status allows operations
        return CardStatus.ACTIVE.name().equals(card.getStatus());
    }
}
