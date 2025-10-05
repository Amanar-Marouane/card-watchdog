package controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import entities.Card;
import entities.CardOperation;
import entities.User;
import enums.OperationType;
import services.CardOperationService;
import services.CardService;
import utils.Console;

public class CardOperationController {
    private final CardOperationService cardOperationService;
    private final CardService cardService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public CardOperationController(CardOperationService cardOperationService, CardService cardService) {
        this.cardOperationService = cardOperationService;
        this.cardService = cardService;
    }

    /**
     * Create a new card operation
     * 
     * @param currentUser The current logged-in user
     */
    public void create(User currentUser) {
        List<Card> userCards = cardService.getUserCards(currentUser.id());

        if (userCards.isEmpty()) {
            Console.info("You don't have any cards yet.");
            return;
        }

        // Display cards
        Console.line();
        Console.info("Select a card for the operation:");
        for (int i = 0; i < userCards.size(); i++) {
            Console.info((i + 1) + ") " + userCards.get(i).getCardType() + " card - " +
                    userCards.get(i).getCardNumber() + " - " + userCards.get(i).getStatus());
        }
        Console.info("0) Cancel");
        Console.line();

        String cardChoice = Console.ask("Enter your choice: ");
        if (cardChoice.equals("0")) {
            return;
        }

        try {
            int cardIndex = Integer.parseInt(cardChoice) - 1;
            if (cardIndex < 0 || cardIndex >= userCards.size()) {
                Console.error("Invalid card selection");
                return;
            }

            Card selectedCard = userCards.get(cardIndex);

            // Select operation type
            Console.line();
            Console.info("Select operation type:");
            Console.info("1) Purchase");
            Console.info("2) Withdrawal");
            Console.info("3) Online Payment");
            Console.info("0) Cancel");
            Console.line();

            String typeChoice = Console.ask("Enter your choice: ");
            if (typeChoice.equals("0")) {
                return;
            }

            OperationType operationType;
            switch (typeChoice) {
                case "1":
                    operationType = OperationType.ACHAT;
                    break;
                case "2":
                    operationType = OperationType.RETRAIT;
                    break;
                case "3":
                    operationType = OperationType.PAIEMENTENLIGNE;
                    break;
                default:
                    Console.error("Invalid operation type");
                    return;
            }

            // Enter amount
            String amountStr = Console.ask("Enter amount: ");
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Console.error("Amount must be greater than zero");
                    return;
                }
            } catch (NumberFormatException e) {
                Console.error("Invalid amount format");
                return;
            }

            // Enter location
            String location = Console.ask("Enter location: ");
            if (location.trim().isEmpty()) {
                Console.error("Location cannot be empty");
                return;
            }

            // Create the operation
            try {
                CardOperation operation = cardOperationService.createCardOperation(
                        String.valueOf(selectedCard.getId()),
                        amount,
                        operationType,
                        location);

                Console.success("Operation created successfully!");
                Console.info("Operation ID: " + operation.id());
                Console.info("Amount: " + operation.amount());
                Console.info("Type: " + operation.type());
                Console.info("Location: " + operation.location());
                Console.info("Date: " + operation.date().format(DATE_FORMATTER));
            } catch (Exception e) {
                Console.error("Failed to create operation: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            Console.error("Please enter a valid number");
        }
    }

    /**
     * Show operations history for a card
     * 
     * @param currentUser The current logged-in user
     */
    public void history(User currentUser) {
        List<Card> userCards = cardService.getUserCards(currentUser.id());

        if (userCards.isEmpty()) {
            Console.info("You don't have any cards yet.");
            return;
        }

        // Display cards
        Console.line();
        Console.info("Select a card to view operations:");
        for (int i = 0; i < userCards.size(); i++) {
            Console.info((i + 1) + ") " + userCards.get(i).getCardType() + " card - " +
                    userCards.get(i).getCardNumber());
        }
        Console.info("0) Cancel");
        Console.line();

        String cardChoice = Console.ask("Enter your choice: ");
        if (cardChoice.equals("0")) {
            return;
        }

        try {
            int cardIndex = Integer.parseInt(cardChoice) - 1;
            if (cardIndex < 0 || cardIndex >= userCards.size()) {
                Console.error("Invalid card selection");
                return;
            }

            Card selectedCard = userCards.get(cardIndex);
            List<CardOperation> operations = cardOperationService
                    .getCardOperations(String.valueOf(selectedCard.getId()));

            if (operations.isEmpty()) {
                Console.info("No operations found for this card.");
                return;
            }

            // Display operations
            Console.line();
            Console.info("Operations for card " + selectedCard.getCardNumber() + ":");
            for (CardOperation op : operations) {
                Console.line();
                Console.info("Date: " + op.date().format(DATE_FORMATTER));
                Console.info("Type: " + op.type());
                Console.info("Amount: " + op.amount());
                Console.info("Location: " + op.location());
            }
            Console.line();

            // Display filtering options
            Console.info("Filter options:");
            Console.info("1) Filter by type");
            Console.info("2) Filter by date range");
            Console.info("0) Back");

            String filterChoice = Console.ask("Enter your choice: ");
            switch (filterChoice) {
                case "1":
                    filterByType(operations);
                    break;
                case "2":
                    filterByDateRange(operations);
                    break;
                case "0":
                    return;
                default:
                    Console.error("Invalid option");
            }

        } catch (NumberFormatException e) {
            Console.error("Please enter a valid number");
        }
    }

    /**
     * Filter operations by type
     * 
     * @param operations List of operations to filter
     */
    private void filterByType(List<CardOperation> operations) {
        Console.line();
        Console.info("Select operation type:");
        Console.info("1) Purchase");
        Console.info("2) Withdrawal");
        Console.info("3) Online Payment");
        Console.line();

        String typeChoice = Console.ask("Enter your choice: ");

        OperationType operationType;
        switch (typeChoice) {
            case "1":
                operationType = OperationType.ACHAT;
                break;
            case "2":
                operationType = OperationType.RETRAIT;
                break;
            case "3":
                operationType = OperationType.PAIEMENTENLIGNE;
                break;
            default:
                Console.error("Invalid operation type");
                return;
        }

        List<CardOperation> filteredOperations = cardOperationService.filterOperationsByType(operations, operationType);
        displayOperations(filteredOperations, "Filtered by type: " + operationType.name());
    }

    /**
     * Filter operations by date range
     * 
     * @param operations List of operations to filter
     */
    private void filterByDateRange(List<CardOperation> operations) {
        Console.line();
        Console.info("Enter date range (format: yyyy-MM-dd HH:mm)");
        String fromStr = Console.ask("From: ");
        String toStr = Console.ask("To: ");

        try {
            LocalDateTime from = LocalDateTime.parse(fromStr, DATE_FORMATTER);
            LocalDateTime to = LocalDateTime.parse(toStr, DATE_FORMATTER);

            if (from.isAfter(to)) {
                Console.error("'From' date must be before 'To' date");
                return;
            }

            List<CardOperation> filteredOperations = cardOperationService.filterOperationsByDateRange(operations, from,
                    to);
            displayOperations(filteredOperations, "Filtered by date range");
        } catch (DateTimeParseException e) {
            Console.error("Invalid date format. Use yyyy-MM-dd HH:mm");
        }
    }

    /**
     * Display a list of operations
     * 
     * @param operations List of operations to display
     * @param header     Header text to show
     */
    private void displayOperations(List<CardOperation> operations, String header) {
        Console.line();
        Console.info(header);

        if (operations.isEmpty()) {
            Console.info("No operations match the filter criteria.");
            return;
        }

        for (CardOperation op : operations) {
            Console.line();
            Console.info("Date: " + op.date().format(DATE_FORMATTER));
            Console.info("Type: " + op.type());
            Console.info("Amount: " + op.amount());
            Console.info("Location: " + op.location());
        }
        Console.line();
    }
}