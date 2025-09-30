package com.cardwatchdog;

import com.cardwatchdog.config.ConfigLoader;
import com.cardwatchdog.services.MySQLConnection;

public class Main {
    private static MySQLConnection connection;

    public static void main(String[] args) {
        configureDatabaseConnection();
        databaseTest();
    }

    public static void configureDatabaseConnection() {
        String url = ConfigLoader.get("db.url");
        String user = ConfigLoader.get("db.user");
        String password = ConfigLoader.get("db.password");
        String dbName = ConfigLoader.get("db.dbName");
        connection = new MySQLConnection(url, user, password, dbName);
    }

    public static void databaseTest() {
        try {
            connection.getConnection();
            System.out.println("Database connection test successful.");
            connection.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}