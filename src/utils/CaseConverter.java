package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaseConverter {
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
}
