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

import entities.Card;
import entities.CreditCard;
import entities.DebitCard;
import entities.PrepaidCard;

public class Hydrator {
    /**
     * Cast a value to the target type, handling common Java types
     * 
     * @param value      The value to cast
     * @param targetType The target type class
     * @return The cast value
     */
    public static Object castValueToType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // If value is already assignable to target type, return it directly
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // Use switch for better performance
        switch (targetType.getName()) {
            // Primitive types and wrappers
            case "int":
            case "java.lang.Integer":
                return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());

            case "long":
            case "java.lang.Long":
                return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());

            case "double":
            case "java.lang.Double":
                return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());

            case "float":
            case "java.lang.Float":
                return value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(value.toString());

            case "short":
            case "java.lang.Short":
                return value instanceof Number ? ((Number) value).shortValue() : Short.parseShort(value.toString());

            case "byte":
            case "java.lang.Byte":
                return value instanceof Number ? ((Number) value).byteValue() : Byte.parseByte(value.toString());

            case "boolean":
            case "java.lang.Boolean":
                return value instanceof Boolean ? value : Boolean.parseBoolean(value.toString());

            case "char":
            case "java.lang.Character":
                if (value instanceof Character)
                    return value;
                String str = value.toString();
                return str.isEmpty() ? '\0' : str.charAt(0);

            // Common reference types
            case "java.lang.String":
                return value.toString();

            case "java.util.UUID":
                return value instanceof UUID ? value : UUID.fromString(value.toString());

            case "java.time.LocalDateTime":
                if (value instanceof LocalDateTime)
                    return value;
                if (value instanceof java.sql.Timestamp)
                    return ((java.sql.Timestamp) value).toLocalDateTime();
                if (value instanceof Date)
                    return ((Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                break;

            case "java.time.LocalDate":
                if (value instanceof LocalDate)
                    return value;
                if (value instanceof java.sql.Date)
                    return ((java.sql.Date) value).toLocalDate();
                if (value instanceof Date)
                    return ((Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                break;

            case "java.time.LocalTime":
                if (value instanceof LocalTime)
                    return value;
                if (value instanceof java.sql.Time)
                    return ((java.sql.Time) value).toLocalTime();
                break;

            case "java.math.BigDecimal":
                if (value instanceof BigDecimal)
                    return value;
                return new BigDecimal(value.toString());
        }

        // If we can't cast, log warning and return the original value
        Console.warn("Could not cast value of type " + value.getClass() + " to " + targetType);
        return value;
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

    public static Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
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
