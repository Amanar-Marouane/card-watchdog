package utils;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

    /**
     * Cast a value to the target type, handling common Java types
     * 
     * @param value      The value to cast
     * @param targetType The target type class
     * @return The cast value
     */
    @SuppressWarnings("all")
    public static Object castValueToType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // Handle primitive types and their wrappers
        if (targetType == int.class || targetType == Integer.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                return Integer.parseInt(value.toString());
            }
        } else if (targetType == long.class || targetType == Long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else {
                return Long.parseLong(value.toString());
            }
        } else if (targetType == double.class || targetType == Double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else {
                return Double.parseDouble(value.toString());
            }
        } else if (targetType == float.class || targetType == Float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            } else {
                return Float.parseFloat(value.toString());
            }
        } else if (targetType == short.class || targetType == Short.class) {
            if (value instanceof Number) {
                return ((Number) value).shortValue();
            } else {
                return Short.parseShort(value.toString());
            }
        } else if (targetType == byte.class || targetType == Byte.class) {
            if (value instanceof Number) {
                return ((Number) value).byteValue();
            } else {
                return Byte.parseByte(value.toString());
            }
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else if (targetType == char.class || targetType == Character.class) {
            if (value instanceof Character) {
                return value;
            } else {
                String str = value.toString();
                return str.isEmpty() ? '\0' : str.charAt(0);
            }
        }
        // Handle String type
        else if (targetType == String.class) {
            return value.toString();
        }
        // Handle UUID type
        else if (targetType == UUID.class) {
            if (value instanceof UUID) {
                return value;
            } else {
                return UUID.fromString(value.toString());
            }
        }
        // Handle date/time types
        else if (targetType == LocalDateTime.class) {
            if (value instanceof LocalDateTime) {
                return value;
            } else if (value instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) value).toLocalDateTime();
            } else if (value instanceof Date) {
                return ((Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            }
        } else if (targetType == LocalDate.class) {
            if (value instanceof LocalDate) {
                return value;
            } else if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate();
            } else if (value instanceof Date) {
                return ((Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            }
        } else if (targetType == LocalTime.class) {
            if (value instanceof LocalTime) {
                return value;
            } else if (value instanceof java.sql.Time) {
                return ((java.sql.Time) value).toLocalTime();
            }
        }
        // Handle BigDecimal
        else if (targetType == BigDecimal.class) {
            if (value instanceof BigDecimal) {
                return value;
            } else if (value instanceof Number) {
                return new BigDecimal(value.toString());
            } else {
                return new BigDecimal(value.toString());
            }
        }
        // Handle enums
        else if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, value.toString());
        }

        // Default case: return the value if it's already assignable to the target type
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // If we can't cast, return the original value
        Console.warn("Could not cast value of type " + value.getClass() + " to " + targetType);
        return value;
    }

    public static <T> T mapRow(ResultSet rs, Class<T> clazz) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();

        // Handle records
        if (clazz.isRecord()) {
            var recordComponents = clazz.getRecordComponents();
            var constructor = clazz.getDeclaredConstructor(
                    Arrays.stream(recordComponents)
                            .map(RecordComponent::getType)
                            .toArray(Class[]::new));

            Object[] args = Arrays.stream(recordComponents)
                    .map(c -> {
                        try {
                            Class<?> fieldType = c.getType();
                            Object value;

                            // Try direct column
                            try {
                                value = rs.getObject(c.getName());
                            } catch (SQLException e) {
                                // Fallback to snake_case
                                String snakeCase = CaseConverter.camelToSnake(c.getName());
                                value = rs.getObject(snakeCase);
                            }

                            // Cast to the correct type
                            return castValueToType(value, fieldType);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray();

            return (T) constructor.newInstance(args);
        }

        // Special handling for Card subclasses
        if (Card.class.isAssignableFrom(clazz)) {
            return createCardInstance(toMap(rs), clazz);
        }

        // Default class handling
        T obj = clazz.getDeclaredConstructor().newInstance();
        int columnCount = meta.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = meta.getColumnLabel(i);
            Object value = rs.getObject(i);

            // Find matching field
            String fieldName = CaseConverter.findMatchingFieldName(columnName, clazz);
            if (fieldName != null) {
                try {
                    Field field = findField(clazz, fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(obj, castValueToType(value, field.getType()));
                    }
                } catch (NoSuchFieldException ignored) {
                    // already checked by findMatchingFieldName
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
                    .map(c -> {
                        Object value = convertedData.get(c.getName());
                        return castValueToType(value, c.getType());
                    })
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
                Field field = findField(clazz, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    field.set(obj, castValueToType(value, field.getType()));
                }
            } catch (NoSuchFieldException ignored) {
                // skip
            }
        }

        return obj;
    }

    // Simplified toMap method
    public static Map<String, Object> toMap(ResultSet rs) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = meta.getColumnLabel(i);
            Object value = rs.getObject(i);

            // Add both the original column name and camelCase version
            result.put(columnName, value);
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
