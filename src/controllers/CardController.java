package controllers;

import java.util.List;

import entities.Card;
import entities.User;
import services.CardService;
import utils.Console;

public class CardController {
    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    public void index(User currentUser) {
        Console.line();
        Console.info("Your Cards:");
        List<Card> userCards = cardService.getUserCards(currentUser.id());

        if (userCards.isEmpty()) {
            Console.info("You don't have any cards yet.");
        } else {
            for (int i = 0; i < userCards.size(); i++) {
                Card card = userCards.get(i);
                Console.info((i + 1) + ") " + card.getCardType() + " card - " +
                        card.getCardNumber() + " - " + card.getStatus());
            }
        }
        Console.line();
    }

    public void show(User currentUser) {
        List<Card> userCards = cardService.getUserCards(currentUser.id());

        if (userCards.isEmpty()) {
            Console.info("You don't have any cards yet.");
            return;
        }

        for (int i = 0; i < userCards.size(); i++) {
            Console.info((i + 1) + ") " + userCards.get(i).getCardType() + " card - " +
                    userCards.get(i).getCardNumber());
        }

        String choice = Console.ask("Select a card to view details (or 0 to cancel): ");

        try {
            int cardIndex = Integer.parseInt(choice);

            if (cardIndex == 0) {
                return;
            }

            if (cardIndex < 1 || cardIndex > userCards.size()) {
                Console.error("Invalid card selection");
                return;
            }

            Card selectedCard = userCards.get(cardIndex - 1);
            Console.line();
            Console.info("Card Details:");
            Console.info(cardService.getCardDetails(selectedCard));
        } catch (NumberFormatException e) {
            Console.error("Please enter a valid number");
        }
    }

    public void create(User currentUser) {
        Console.line();
        Console.info("Create a new card:");
        Console.info("1) Debit Card");
        Console.info("2) Credit Card");
        Console.info("3) Prepaid Card");
        Console.info("0) Cancel");
        Console.line();

        String cardTypeChoice = Console.ask("Select card type: ");

        if (cardTypeChoice.equals("0")) {
            return;
        }

        try {
            Card newCard;
            switch (cardTypeChoice) {
                case "1":
                    showCardOffers("Debit");
                    int debitOfferChoice = Integer.parseInt(Console.ask("Select an offer (1-3): "));
                    newCard = cardService.createDebitCard(currentUser.id(), debitOfferChoice);
                    break;
                case "2":
                    showCardOffers("Credit");
                    int creditOfferChoice = Integer.parseInt(Console.ask("Select an offer (1-3): "));
                    newCard = cardService.createCreditCard(currentUser.id(), creditOfferChoice);
                    break;
                case "3":
                    showCardOffers("Prepaid");
                    int prepaidOfferChoice = Integer.parseInt(Console.ask("Select an offer (1-3): "));
                    newCard = cardService.createPrepaidCard(currentUser.id(), prepaidOfferChoice);
                    break;
                default:
                    Console.error("Invalid card type selection");
                    return;
            }

            Console.success("Card created successfully!");
            Console.info("Card Number: " + newCard.getCardNumber());
            Console.info("Expiration Date: " + newCard.getExpirationDate());
        } catch (Exception e) {
            Console.error("Failed to create card: " + e.getMessage());
        }
    }

    private void showCardOffers(String cardType) {
        Console.line();
        Console.info(cardType + " Card Offers:");

        if (cardType.equals("Debit")) {
            Console.info("1) Basic: Daily limit of 500");
            Console.info("2) Standard: Daily limit of 1000");
            Console.info("3) Premium: Daily limit of 2000");
        } else if (cardType.equals("Credit")) {
            Console.info("1) Basic: Monthly limit of 2000, Interest Rate 2.5%");
            Console.info("2) Standard: Monthly limit of 5000, Interest Rate 3.5%");
            Console.info("3) Premium: Monthly limit of 10000, Interest Rate 5%");
        } else if (cardType.equals("Prepaid")) {
            Console.info("1) Basic: 50 preloaded balance");
            Console.info("2) Standard: 100 preloaded balance");
            Console.info("3) Premium: 200 preloaded balance");
        }
        Console.line();
    }

    public void update(User currentUser) {
        List<Card> userCards = cardService.getUserCards(currentUser.id());

        if (userCards.isEmpty()) {
            Console.info("You don't have any cards yet.");
            return;
        }

        for (int i = 0; i < userCards.size(); i++) {
            Console.info((i + 1) + ") " + userCards.get(i).getCardType() + " card - " +
                    userCards.get(i).getCardNumber() + " - " + userCards.get(i).getStatus());
        }

        String choice = Console.ask("Select a card to manage (or 0 to cancel): ");

        try {
            int cardIndex = Integer.parseInt(choice);

            if (cardIndex == 0) {
                return;
            }

            if (cardIndex < 1 || cardIndex > userCards.size()) {
                Console.error("Invalid card selection");
                return;
            }

            Card selectedCard = userCards.get(cardIndex - 1);
            manageCardOptions(selectedCard);

        } catch (NumberFormatException e) {
            Console.error("Please enter a valid number");
        }
    }

    private void manageCardOptions(Card card) {
        Console.line();
        Console.info("Manage Card: " + card.getCardNumber());
        Console.info("Current Status: " + card.getStatus());
        Console.info("1) Activate Card");
        Console.info("2) Suspend Card");
        Console.info("3) Block Card");
        Console.info("4) Renew Card");
        Console.info("0) Cancel");
        Console.line();

        String choice = Console.ask("Select an action: ");

        try {
            String cardId = String.valueOf(card.getId());

            switch (choice) {
                case "1":
                    cardService.activateCard(cardId);
                    Console.success("Card activated successfully");
                    break;
                case "2":
                    cardService.suspendCard(cardId);
                    Console.success("Card suspended successfully");
                    break;
                case "3":
                    cardService.blockCard(cardId);
                    Console.success("Card blocked successfully");
                    break;
                case "4":
                    cardService.renewCard(cardId);
                    Console.success("Card renewed successfully");
                    break;
                case "0":
                    return;
                default:
                    Console.error("Invalid option");
            }
        } catch (Exception e) {
            Console.error("Error: " + e.getMessage());
        }
    }

    public void delete(User currentUser) {
        List<Card> userCards = cardService.getUserCards(currentUser.id());

        if (userCards.isEmpty()) {
            Console.info("You don't have any cards yet.");
            return;
        }

        for (int i = 0; i < userCards.size(); i++) {
            Console.info((i + 1) + ") " + userCards.get(i).getCardType() + " card - " +
                    userCards.get(i).getCardNumber());
        }

        String choice = Console.ask("Select a card to delete (or 0 to cancel): ");

        try {
            int cardIndex = Integer.parseInt(choice);

            if (cardIndex == 0) {
                return;
            }

            if (cardIndex < 1 || cardIndex > userCards.size()) {
                Console.error("Invalid card selection");
                return;
            }

            Card selectedCard = userCards.get(cardIndex - 1);

            if (Console.confirm("Are you sure you want to delete this card?")) {
                cardService.deleteCard(String.valueOf(selectedCard.getId()));
                Console.success("Card deleted successfully");
            } else {
                Console.info("Card deletion cancelled.");
            }
        } catch (NumberFormatException e) {
            Console.error("Please enter a valid number");
        }
    }
}
