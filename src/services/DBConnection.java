package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private String url;
    private String user;
    private String password;
    private String dbName;

    public DBConnection(String url, String user, String password, String dbName) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.dbName = dbName;
    }

    public Connection getConnection() throws SQLException {
        // 1. Connect without DB to create it if it doesn't exist
        try (Connection tmpConn = DriverManager.getConnection(url, user, password);
                Statement stmt = tmpConn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "`");
        }

        // 2. Connect to the actual database
        return DriverManager.getConnection(url + dbName, user, password);
    }
}
