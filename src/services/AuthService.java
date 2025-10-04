package services;

import java.util.Map;
import java.util.Optional;

import entities.User;
import repositories.UserRepository;
import utils.Console;

public class AuthService {
    private User currentUser;
    private boolean authenticated;
    private UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.authenticated = false;
    }

    public boolean register(String fullName, String email, String phoneNumber) {
        if (this.authenticated) {
            Console.warning("You must log out first before registering a new account.");
            return false;
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            Console.error("Registration failed: Email already exists.");
            return false;
        }

        try {
            userRepository.create(Map.of(
                    "name", fullName,
                    "email", email,
                    "phone_number", phoneNumber));

            Console.success("Registration successful!");
            return true;
        } catch (Exception e) {
            Console.error("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public boolean login(String email) {
        if (this.authenticated) {
            Console.warning("You are already logged in. Please log out first.");
            return false;
        }

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            Console.error("Login failed: User not found.");
            return false;
        }

        this.establishSession(user.get());
        Console.success("Login successful!");
        return true;
    }

    public synchronized void logout() {
        if (!this.authenticated) {
            Console.info("No active session to logout.");
            return;
        }

        Console.info("Processing logout...");
        clearSession();
        Console.success("Logout completed successfully!");
    }

    public Optional<User> getCurrentUser() {
        return this.authenticated ? Optional.ofNullable(this.currentUser) : Optional.empty();
    }

    public boolean isAuthenticated() {
        return this.authenticated && this.currentUser != null;
    }

    public User requireAuthentication() throws Exception {
        if (!isAuthenticated()) {
            Console.error("Authentication required. Please login to continue.");
            throw new Exception("[Access] No authenticated user found. Please login first.");
        }
        return this.currentUser;
    }

    public void requireNoAuthentication() throws Exception {
        if (isAuthenticated()) {
            Console.error("Operation not allowed: User already authenticated. Please logout first.");
            throw new Exception("[Access] User is already authenticated. Please logout first.");
        }
    }

    private synchronized void establishSession(User user) {
        this.currentUser = user;
        this.authenticated = true;
    }

    private synchronized void clearSession() {
        this.currentUser = null;
        this.authenticated = false;
    }
}