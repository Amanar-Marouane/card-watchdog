package controllers;

import java.util.HashMap;

import entities.User;
import services.AuthService;
import utils.Console;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public static class Validator {

        public static boolean isValidEmail(String email) {
            return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
        }

        public static boolean isValidFullName(String fullName) {
            return fullName != null && fullName.trim().length() >= 2;
        }

        public static boolean isValidPassword(String password) {
            return password != null && password.length() >= 6;
        }

        public static boolean isValidPhoneNumber(String phoneNumber) {
            return phoneNumber != null && phoneNumber.matches("^\\+?[0-9]{7,15}$");
        }
    }

    private HashMap<String, String> registerAttempt() {
        HashMap<String, String> registry = new HashMap<>();

        registry.put("email", this.emailAttempt());
        registry.put("phone_number", this.phoneNumberAttempt());
        registry.put("fullName", this.fullNameAttempt());
        registry.put("password", this.passwordAttempt());

        return registry;
    }

    private HashMap<String, String> loginAttempt() {
        HashMap<String, String> registry = new HashMap<>();

        registry.put("email", this.emailAttempt());
        registry.put("password", this.passwordAttempt());

        return registry;
    }

    private String emailAttempt() {
        String email;
        do {
            email = Console.ask("=> Enter an email");
            if (!Validator.isValidEmail(email))
                Console.error("Invalid Email");
        } while (!Validator.isValidEmail(email));
        return email;
    }

    private String phoneNumberAttempt() {
        String phoneNumber;
        do {
            phoneNumber = Console.ask("=> Enter a phone number");
            if (!Validator.isValidPhoneNumber(phoneNumber))
                Console.error("Invalid phone number");
        } while (!Validator.isValidPhoneNumber(phoneNumber));
        return phoneNumber;
    }

    private String fullNameAttempt() {
        String fullName;
        do {
            fullName = Console.ask("=> Enter a full name");
            if (!Validator.isValidFullName(fullName))
                Console.error("Invalid full name");
        } while (!Validator.isValidFullName(fullName));
        return fullName;
    }

    private String passwordAttempt() {
        String password;
        do {
            password = Console.ask("=> Enter a password");
            if (!Validator.isValidPassword(password))
                Console.error("Invalid password");
        } while (!Validator.isValidPassword(password));

        return password;
    }

    public void register() {
        Console.info("Processing registration...");

        HashMap<String, String> register = this.registerAttempt();
        String fullName = register.get("fullName");
        String email = register.get("email");
        String password = register.get("password");
        String phoneNumber = register.get("phone_number");

        authService.register(fullName, email, phoneNumber, password);
    }

    public void login() {
        Console.info("Processing login...");

        HashMap<String, String> login = this.loginAttempt();
        String email = login.get("email");
        String password = login.get("password");

        authService.login(email, password);
    }

    public void logout() {
        authService.logout();
    }

    public void profile(User user) {
        Console.line();
        Console.success("=== Your Profile ===");
        Console.info("User ID   : " + user.id());
        Console.info("Full Name : " + user.name());
        Console.info("Email     : " + user.email());
        Console.info("Phone     : " + user.phone_number());
        Console.line();
    }
}
