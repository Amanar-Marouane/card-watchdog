package utils;

import java.util.Scanner;

public class Console {
    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    private static final Scanner scanner = new Scanner(System.in);

    public static void line() {
        System.out.println(WHITE + "--------------------------------------------------" + RESET);
    }

    public static void info(String msg) {
        System.out.println(BLUE + msg + RESET);
    }

    public static void error(String msg) {
        System.out.println(RED + "[ERROR] " + msg + RESET);
    }

    public static void success(String msg) {
        System.out.println(GREEN + msg + RESET);
    }

    public static void warning(String msg) {
        System.out.println(YELLOW + "[WARNING] " + msg + RESET);
    }

    public static void print(String msg) {
        System.out.println(msg);
    }

    public static String ask(String prompt) {
        // Print the prompt in color, then reset so input is normal color
        System.out.print(CYAN + prompt + "\n=> " + RESET);

        // Read input normally
        String answer = scanner.nextLine();

        // Return the raw answer only
        return answer.trim();
    }

    // I call this before exiting the app to prevent leaking
    public static void close() {
        scanner.close();
    }

    public static boolean confirm(String prompt) {
        while (true) {
            String response = ask(prompt + " (y/N)").toLowerCase();
            if (response.equals("yes") || response.equals("y")) {
                return true;
            } else if (response.equals("no") || response.equals("n") || response.isEmpty()) {
                return false;
            } else {
                warning("Please answer with 'y/yes' or 'n/no'");
            }
        }
    }
}
