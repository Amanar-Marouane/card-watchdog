package com.cardwatchdog.config;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new RuntimeException("database.properties not found! Copy database.properties.example");
            }
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load database.properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
