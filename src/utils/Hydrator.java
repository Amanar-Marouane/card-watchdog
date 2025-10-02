package utils;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Hydrator {
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
                            return rs.getObject(c.getName());
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }).toArray();

            T obj = (T) constructor.newInstance(args);
            return obj;
        }

        T obj = clazz.getDeclaredConstructor().newInstance();
        int columnCount = meta.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = meta.getColumnLabel(i); // alias or column name
            Object value = rs.getObject(i);

            try {
                Field field = clazz.getDeclaredField(columnName);
                field.setAccessible(true);
                field.set(obj, value);
            } catch (NoSuchFieldException ignored) {
                // column not in class, skip
            }
        }
        return obj;
    }

    // not in use anymore, wait until finishing all repositories
    public static <T> T updateEntityWith(T entity, Map<String, Object> updatedValues) {
        for (Map.Entry<String, Object> entry : updatedValues.entrySet()) {
            String fieldName = entry.getKey();
            Object newValue = entry.getValue();

            try {
                Field field = entity.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);

                Class<?> fieldType = field.getType();
                if (newValue != null && !fieldType.isAssignableFrom(newValue.getClass())) {
                    // convert Integer -> int, String -> int, etc...
                    newValue = convertType(newValue, fieldType);
                }

                field.set(entity, newValue);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return entity;
    }

    private static Object convertType(Object value, Class<?> targetType) {
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value.toString());
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value.toString());
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        }
        return value;
    }

    // not in use anymore, wait until finishing all repositories
    public static Map<String, Object> getFieldsWithValues(Object obj) throws IllegalAccessException {
        Map<String, Object> result = new HashMap<>();
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true); // allow access to private fields
            result.put(field.getName(), field.get(obj));
        }

        return result;
    }
}
