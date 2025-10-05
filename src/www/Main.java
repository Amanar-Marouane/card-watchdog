package www;

import config.ConfigLoader;
import repositories.CardOperationRepository;
import repositories.CardRepository;
import repositories.FraudAlertRepository;
import repositories.UserRepository;
import services.AuthService;
import services.CardOperationService;
import services.CardService;
import services.DBConnection;
import services.FraudDetectionService;
import ui.ConsoleUi;

public class Main {
    private static DBConnection connection;
    private static AuthService authService;
    private static CardService cardService;
    private static CardOperationService cardOperationService;
    private static UserRepository userRepository;
    private static CardRepository cardRepository;
    private static CardOperationRepository cardOperationRepository;
    private static FraudAlertRepository fraudAlertRepository;
    private static FraudDetectionService fraudDetectionService;

    public static void main(String[] args) {
        // Load configuration
        configureDatabaseConnection();

        // Test DB connection
        databaseTest();

        // Initialize repositories and services
        reposInit();

        // Start the application
        ConsoleUi menu = new ConsoleUi(authService, cardService, cardOperationService);
        menu.run();

        // Exit the application
        ConsoleUi.exit(0);
    }

    private static void configureDatabaseConnection() {
        String url = ConfigLoader.get("db.url");
        String user = ConfigLoader.get("db.user");
        String password = ConfigLoader.get("db.password");
        String dbName = ConfigLoader.get("db.dbName");
        connection = new DBConnection(url, user, password, dbName);
    }

    private static void databaseTest() {
        try {
            connection.getConnection();
            System.out.println("Database connection test successful.");
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleUi.exit(1);
        }
    }

    private static void reposInit() {
        userRepository = new UserRepository(connection);
        cardRepository = new CardRepository(connection);
        cardOperationRepository = new CardOperationRepository(connection);
        fraudAlertRepository = new FraudAlertRepository(connection);

        authService = new AuthService(userRepository);
        cardService = new CardService(cardRepository);
        fraudDetectionService = new FraudDetectionService(fraudAlertRepository, cardRepository,
                cardOperationRepository);
        cardOperationService = new CardOperationService(cardOperationRepository, cardRepository, fraudDetectionService);
    }
}