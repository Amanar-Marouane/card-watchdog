package ui;

import controllers.AuthController;
import controllers.CardController;
import controllers.CardOperationController;
import entities.User;
import services.AuthService;
import services.CardService;
import services.CardOperationService;
import utils.Console;

public class ConsoleUi {

    private final AuthService auth;
    private final AuthController authController;
    private final CardController cardController;
    private final CardOperationController cardOperationController;

    public ConsoleUi(AuthService auth, CardService cardService, CardOperationService cardOperationService) {
        this.auth = auth;
        this.authController = new AuthController(auth);
        this.cardController = new CardController(cardService);
        this.cardOperationController = new CardOperationController(cardOperationService, cardService);
    }

    public void run() {
        while (true) {
            if (!auth.isAuthenticated()) {
                showAuthMenu();
            } else {
                showUserMenu();
            }
        }
    }

    private void showAuthMenu() {
        Console.line();
        Console.info("Welcome to the Bank System!");
        Console.info("Please choose an option:");
        Console.info("  1) Register");
        Console.info("  2) Login");
        Console.info("  0) Exit");
        Console.line();

        String opt = Console.ask("Enter choice: ");
        switch (opt) {
            case "0":
                exit(0);
            case "1":
                authController.register();
                break;
            case "2":
                authController.login();
                break;
            default:
                Console.error("Invalid option!");
                break;
        }
    }

    private void showUserMenu() {
        try {
            User u = auth.requireAuthentication();

            Console.line();
            Console.success("Welcome back, " + u.name() + "!");
            Console.line();

            Console.info("Choose an option:");
            Console.info("  1) Logout");
            Console.info("  2) View Profile");
            Console.info("  3) Manage Cards");
            Console.info("  0) Exit");
            Console.line();

            String opt = Console.ask("Enter choice: ");
            switch (opt) {
                case "0":
                    exit(0);
                case "1":
                    authController.logout();
                    break;
                case "2":
                    authController.profile(u);
                    break;
                case "3":
                    showCardMenu(u);
                    break;
                default:
                    Console.error("Invalid option!");
                    break;
            }
        } catch (Exception e) {
            Console.error(e.getMessage());
            return;
        }
    }

    private void showCardMenu(User user) {
        while (true) {
            Console.line();
            Console.info("Card Management");
            Console.info("Choose an option:");
            Console.info("  1) View My Cards");
            Console.info("  2) View Card Details");
            Console.info("  3) Add New Card");
            Console.info("  4) Manage Card Status");
            Console.info("  5) Delete Card");
            Console.info("  6) Card Operations");
            Console.info("  0) Back to Main Menu");
            Console.line();

            String opt = Console.ask("Enter choice: ");
            switch (opt) {
                case "0":
                    return;
                case "1":
                    cardController.index(user);
                    break;
                case "2":
                    cardController.show(user);
                    break;
                case "3":
                    cardController.create(user);
                    break;
                case "4":
                    cardController.update(user);
                    break;
                case "5":
                    cardController.delete(user);
                    break;
                case "6":
                    showCardOperationMenu(user);
                    break;
                default:
                    Console.error("Invalid option!");
                    break;
            }
        }
    }

    private void showCardOperationMenu(User user) {
        while (true) {
            Console.line();
            Console.info("Card Operations");
            Console.info("Choose an option:");
            Console.info("  1) New Operation (Purchase/Withdrawal/Payment)");
            Console.info("  2) View Operation History");
            Console.info("  0) Back to Card Menu");
            Console.line();

            String opt = Console.ask("Enter choice: ");
            switch (opt) {
                case "0":
                    return;
                case "1":
                    cardOperationController.create(user);
                    break;
                case "2":
                    cardOperationController.history(user);
                    break;
                default:
                    Console.error("Invalid option!");
                    break;
            }
        }
    }

    public static void exit(Integer code) {
        Console.info("Goodbye!");
        Console.close();
        System.exit(code);
    }
}
