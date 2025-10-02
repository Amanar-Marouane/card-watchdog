package repositories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import entities.User;
import services.DBConnection;
import utils.Hydrator;

public class UserRepository implements RepositoryBase<User> {
    public static final String TABLE_NAME = "users";
    private final DBConnection connectionService;

    public UserRepository(DBConnection connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public Optional<User> findById(String id) {
        try {
            var conn = connectionService.getConnection();
            var stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE id = ? LIMIT 1");
            stmt.setString(1, id);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(Hydrator.mapRow(rs, User.class));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        try {
            var conn = connectionService.getConnection();
            var stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME);
            var rs = stmt.executeQuery();
            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(Hydrator.mapRow(rs, User.class));
            }
            return users;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User create(Map<String, Object> data) {
        try {
            // Exclude ID if present
            Map<String, Object> filteredData = data.entrySet().stream()
                    .filter(e -> !"id".equals(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            var conn = connectionService.getConnection();

            // Build field list and binding template
            String fields = "(" + String.join(", ", filteredData.keySet()) + ")";
            String bindingTemplate = "(" + String.join(", ", Collections.nCopies(filteredData.size(), "?")) + ")";

            var stmt = conn.prepareStatement(
                    "INSERT INTO " + TABLE_NAME + " " + fields + " VALUES " + bindingTemplate,
                    java.sql.Statement.RETURN_GENERATED_KEYS);

            // Set parameters
            int index = 1;
            for (Object value : filteredData.values()) {
                stmt.setObject(index++, value);
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0)
                throw new Exception("Creating user failed, no rows affected.");

            try (var generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return new User(
                            id,
                            (String) data.get("name"),
                            (String) data.get("email"),
                            (String) data.get("phone_number"),
                            (String) data.get("password"));
                } else {
                    throw new Exception("Creating user failed, no ID obtained.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            var conn = connectionService.getConnection();
            var stmt = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE id = ?");
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User updateById(String id, Map<String, Object> fieldsToUpdate) {
        // Prevent modifying the ID
        if (fieldsToUpdate.containsKey("id")) {
            fieldsToUpdate.remove("id");
        }

        if (fieldsToUpdate.isEmpty())
            return null; // nothing to update

        try {
            var conn = connectionService.getConnection();

            // Build SET clause: "name = ?, age = ?, ..."
            var setClause = fieldsToUpdate.keySet().stream()
                    .map(field -> field + " = ?")
                    .collect(Collectors.joining(", "));

            // Prepare statement
            var stmt = conn.prepareStatement(
                    "UPDATE " + TABLE_NAME + " SET " + setClause + " WHERE id = ?");

            // Set parameters for the fields
            int index = 1;
            for (Object value : fieldsToUpdate.values()) {
                stmt.setObject(index++, value);
            }

            // Set the ID parameter
            stmt.setObject(index, id);

            // Execute update
            stmt.executeUpdate();

            return new User(
                    Integer.parseInt(id),
                    (String) fieldsToUpdate.get("name"),
                    (String) fieldsToUpdate.get("email"),
                    (String) fieldsToUpdate.get("phone_number"),
                    (String) fieldsToUpdate.get("password"));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<User> findByEmail(String email) {
        try {
            var conn = connectionService.getConnection();
            var stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE email = ? LIMIT 1");
            stmt.setString(1, email);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(Hydrator.mapRow(rs, User.class));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

}
