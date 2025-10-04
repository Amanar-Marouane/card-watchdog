package utils;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import entities.Card;
import entities.CreditCard;
import entities.DebitCard;
import entities.PrepaidCard;

public class Hydrator {
    public static class CaseConverter {
        // Convert from snake_case to camelCase
        public static String snakeToCamel(String snake) {
            if (snake == null || snake.isEmpty()) {
                return snake;
            }

            // Use regex to find all occurrences of underscore followed by a character
            Pattern pattern = Pattern.compile("_([a-zA-Z])");
            Matcher matcher = pattern.matcher(snake);
            StringBuffer sb = new StringBuffer();

            // Replace each _x with X (uppercase)
            while (matcher.find()) {
                matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
            }
            matcher.appendTail(sb);

            return sb.toString();
        }

        // Convert from camelCase to snake_case
        public static String camelToSnake(String camel) {
            if (camel == null || camel.isEmpty()) {
                return camel;
            }

            // Replace each uppercase letter with _lowercase
            String result = camel.replaceAll("([A-Z])", "_$1").toLowerCase();

            // Handle case where the string starts with an underscore due to first char
            // being uppercase
            return result.startsWith("_") ? result.substring(1) : result;
        }

        // Find corresponding field name using case conversion if direct match fails
        public static String findMatchingFieldName(String columnName, Class<?> clazz) {
            // First try direct match
            try {
                clazz.getDeclaredField(columnName);
                return columnName;
            } catch (NoSuchFieldException e) {
                // Try camelCase version
                String camelCase = snakeToCamel(columnName);
                try {
                    clazz.getDeclaredField(camelCase);
                    return camelCase;
                } catch (NoSuchFieldException e2) {
                    // Try in superclass
                    Class<?> superClass = clazz.getSuperclass();
                    if (superClass != null) {
                        try {
                            superClass.getDeclaredField(columnName);
                            return columnName;
                        } catch (NoSuchFieldException e3) {
                            try {
                                superClass.getDeclaredField(camelCase);
                                return camelCase;
                            } catch (NoSuchFieldException e4) {
                                // Not found in class or superclass
                                return null;
                            }
                        }
                    }
                    return null;
                }
            }
        }
    }

    public static <T> T mapRow(ResultSet rs, Class<T> clazz) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();

        if (clazz.isRecord()) {
            var recordComponents = clazz.getRecordComponents();
            var constructor = clazz.getDeclaredConstructor(
                    Arrays.stream(recordComponents)
                            .map(RecordComponent::getType)
                            .toArray(Class[]::new));

            Object[] args = Arrays.stream(recordComponents)
                    .map(c -> {
                        try {
                            // Try with direct name match first
                            try {
                                return rs.getObject(c.getName());
                            } catch (SQLException e) {
                                // Try with snake_case conversion
                                String snakeCase = CaseConverter.camelToSnake(c.getName());
                                return rs.getObject(snakeCase);
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }).toArray();

            T obj = (T) constructor.newInstance(args);
            return obj;
        }

        // Special handling for Card subclasses
        if (Card.class.isAssignableFrom(clazz)) {
            return createCardInstance(rs, clazz);
        }

        // Default approach for other classes
        T obj = clazz.getDeclaredConstructor().newInstance();
        int columnCount = meta.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = meta.getColumnLabel(i);
            Object value = rs.getObject(i);

            // Find matching field using case converter
            String fieldName = CaseConverter.findMatchingFieldName(columnName, clazz);
            if (fieldName != null) {
                try {
                    Field field = findField(clazz, fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(obj, value);
                    }
                } catch (NoSuchFieldException ignored) {
                    // Field already checked by findMatchingFieldName, so this shouldn't happen
                }
            }
        }
        return obj;
    }

    public static <T> T mapRow(Map<String, Object> data, Class<T> clazz) throws Exception {
        // Convert map keys to match Java naming conventions
        Map<String, Object> convertedData = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String camelCaseKey = CaseConverter.snakeToCamel(entry.getKey());
            convertedData.put(camelCaseKey, entry.getValue());
            // Also keep the original key for backward compatibility
            convertedData.put(entry.getKey(), entry.getValue());
        }

        if (clazz.isRecord()) {
            // Handle records with case conversion
            var recordComponents = clazz.getRecordComponents();
            var constructor = clazz.getDeclaredConstructor(
                    Arrays.stream(recordComponents)
                            .map(RecordComponent::getType)
                            .toArray(Class[]::new));

            Object[] args = Arrays.stream(recordComponents)
                    .map(c -> convertedData.get(c.getName()))
                    .toArray();

            return (T) constructor.newInstance(args);
        }

        // Special handling for Card subclasses
        if (Card.class.isAssignableFrom(clazz)) {
            return createCardInstance(convertedData, clazz);
        }

        // Default approach for other classes
        T obj = clazz.getDeclaredConstructor().newInstance();

        for (Map.Entry<String, Object> entry : convertedData.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            try {
                // Try to find the field with current name
                Field field = findField(clazz, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    field.set(obj, value);
                }
            } catch (NoSuchFieldException ignored) {
                // Field not in class, skip
            }
        }

        return obj;
    }

    public static Map<String, Object> toMap(ResultSet rs) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = meta.getColumnLabel(i);
            Object value = rs.getObject(i);
            result.put(columnName, value);

            // Also add camelCase version for Java compatibility
            result.put(CaseConverter.snakeToCamel(columnName), value);
        }

        return result;
    }

    // Helper method to find a field by name, including superclass fields
    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                try {
                    return superClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                    // Field not found in superclass either
                }
            }
            throw e;
        }
    }

    // Create Card instances based on type
    @SuppressWarnings("unchecked")
    private static <T> T createCardInstance(ResultSet rs, Class<T> clazz) throws Exception {
        // Try different column name patterns to handle both direct columns and aliases
        int id;
        try {
            id = rs.getInt("id");
        } catch (SQLException e) {
            try {
                // Try using the table-prefixed column name
                id = rs.getInt("cards.id");
            } catch (SQLException e2) {
                // If both fail, use card_id as a fallback
                id = rs.getInt("card_id");
            }
        }

        String expirationDate = getStringFromResult(rs, "expiration_date", "cards.expiration_date");
        String status = getStringFromResult(rs, "status", "cards.status");
        int userId = getIntFromResult(rs, "user_id", "cards.user_id");

        if (clazz == CreditCard.class) {
            // Try both old and new column names for compatibility
            double monthlyLimit;
            double interestRate;
            try {
                monthlyLimit = rs.getDouble("monthly_limit");
            } catch (SQLException e) {
                monthlyLimit = rs.getDouble("plafond_mensuel");
            }

            try {
                interestRate = rs.getDouble("interest_rate");
            } catch (SQLException e) {
                interestRate = rs.getDouble("taux_interet");
            }

            return (T) new CreditCard(id, expirationDate, status, userId, monthlyLimit, interestRate);
        } else if (clazz == DebitCard.class) {
            double dailyLimit;
            try {
                dailyLimit = rs.getDouble("daily_limit");
            } catch (SQLException e) {
                dailyLimit = rs.getDouble("plafond_journalier");
            }

            return (T) new DebitCard(id, expirationDate, status, userId, dailyLimit);
        } else if (clazz == PrepaidCard.class) {
            double availableBalance;
            try {
                availableBalance = rs.getDouble("available_balance");
            } catch (SQLException e) {
                availableBalance = rs.getDouble("solde_disponible");
            }

            return (T) new PrepaidCard(id, expirationDate, status, userId, availableBalance);
        }

        throw new IllegalArgumentException("Unsupported card type: " + clazz.getName());
    }

    /**
     * Get string from ResultSet trying multiple column names in order
     * 
     * @param rs
     * @param columnNames
     * @return
     * @throws SQLException
     */
    private static String getStringFromResult(ResultSet rs, String... columnNames) throws SQLException {
        for (String columnName : columnNames) {
            try {
                return rs.getString(columnName);
            } catch (SQLException e) {
                // Try next column name
            }
        }
        throw new SQLException("None of the specified column names exist: " + String.join(", ", columnNames));
    }

    /**
     * Get int from ResultSet trying multiple column names in order
     * 
     * @param rs
     * @param columnNames
     * @return
     * @throws SQLException
     */
    private static int getIntFromResult(ResultSet rs, String... columnNames) throws SQLException {
        for (String columnName : columnNames) {
            try {
                return rs.getInt(columnName);
            } catch (SQLException e) {
                // Try next column name
            }
        }
        throw new SQLException("None of the specified column names exist: " + String.join(", ", columnNames));
    }

    @SuppressWarnings("unchecked")
    private static <T> T createCardInstance(Map<String, Object> data, Class<T> clazz) throws Exception {
        // With case conversion - check for both conventions
        int id = 0;
        if (data.containsKey("id")) {
            id = ((Number) data.get("id")).intValue();
        }

        // Get fields with fallbacks between snake_case and camelCase
        String expirationDate = getStringFromMap(data, "expirationDate", "expiration_date");
        String status = getStringFromMap(data, "status");
        int userId = getIntFromMap(data, "userId", "user_id");

        if (clazz == CreditCard.class) {
            double monthlyLimit = getDoubleFromMap(data, "monthlyLimit", "monthly_limit", "plafondMensuel",
                    "plafond_mensuel");
            double interestRate = getDoubleFromMap(data, "interestRate", "interest_rate", "tauxInteret",
                    "taux_interet");
            return (T) new CreditCard(id, expirationDate, status, userId, monthlyLimit, interestRate);
        } else if (clazz == DebitCard.class) {
            double dailyLimit = getDoubleFromMap(data, "dailyLimit", "daily_limit", "plafondJournalier",
                    "plafond_journalier");
            return (T) new DebitCard(id, expirationDate, status, userId, dailyLimit);
        } else if (clazz == PrepaidCard.class) {
            double availableBalance = getDoubleFromMap(data, "availableBalance", "available_balance", "soldeDisponible",
                    "solde_disponible");
            return (T) new PrepaidCard(id, expirationDate, status, userId, availableBalance);
        }

        throw new IllegalArgumentException("Unsupported card type: " + clazz.getName());
    }

    // Helper methods to get values from map with multiple possible keys
    private static String getStringFromMap(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (data.containsKey(key) && data.get(key) != null) {
                return data.get(key).toString();
            }
        }
        return null;
    }

    private static int getIntFromMap(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (data.containsKey(key) && data.get(key) != null) {
                return ((Number) data.get(key)).intValue();
            }
        }
        return 0;
    }

    private static double getDoubleFromMap(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (data.containsKey(key) && data.get(key) != null) {
                return ((Number) data.get(key)).doubleValue();
            }
        }
        return 0.0;
    }
}
