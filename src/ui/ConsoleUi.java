package ui;

import controllers.AuthController;
import entities.User;
import services.AuthService;
import utils.Console;

public class ConsoleUi {

    private final AuthService auth;
    private final AuthController authController;

    public ConsoleUi(AuthService auth) {
        this.auth = auth;
        this.authController = new AuthController(auth);
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
                default:
                    Console.error("Invalid option!");
                    break;
            }
        } catch (Exception e) {
            Console.error(e.getMessage());
            return;
        }
    }

    public static void exit(Integer code) {
        Console.info("Goodbye!");
        Console.close();
        System.exit(code);
    }
}
