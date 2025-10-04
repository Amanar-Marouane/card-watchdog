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
        return pipeline(() -> {
            var conn = connectionService.getConnection();
            var stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE id = ? LIMIT 1");
            stmt.setString(1, id);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.ofNullable(Hydrator.mapRow(rs, User.class));
            }
            return Optional.empty();
        });
    }

    @Override
    public List<User> findAll() {
        return pipeline(() -> {
            var conn = connectionService.getConnection();
            var stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME);
            var rs = stmt.executeQuery();
            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(Hydrator.mapRow(rs, User.class));
            }
            return users;
        });
    }

    @Override
    public User create(Map<String, Object> data) {
        return pipeline(() -> {
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
                    filteredData.put("id", id);
                    return Hydrator.mapRow(filteredData, User.class);
                } else
                    throw new Exception("Creating user failed, no ID obtained.");
            }
        });
    }

    @Override
    public void deleteById(String id) {
        pipeline(() -> {
            var conn = connectionService.getConnection();
            var stmt = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE id = ?");
            stmt.setString(1, id);
            stmt.executeUpdate();
        });
    }

    @Override
    public void updateById(User user, Map<String, Object> fieldsToUpdate) {
        final User[] userRef = { user };

        fieldsToUpdate.remove("id"); // prevent ID modification
        if (fieldsToUpdate.isEmpty())
            return;

        userRef[0] = pipeline(() -> {
            var conn = connectionService.getConnection();

            // Build SET clause
            var setClause = fieldsToUpdate.keySet().stream()
                    .map(field -> field + " = ?")
                    .collect(Collectors.joining(", "));

            var stmt = conn.prepareStatement(
                    "UPDATE " + TABLE_NAME + " SET " + setClause + " WHERE id = ?");

            int index = 1;
            for (Object value : fieldsToUpdate.values()) {
                stmt.setObject(index++, value);
            }
            stmt.setObject(index, user.id());

            stmt.executeUpdate();

            // Return updated immutable user
            fieldsToUpdate.put("id", user.id());
            return Hydrator.mapRow(fieldsToUpdate, User.class);
        });
    }

    public Optional<User> findByEmail(String email) {
        return pipeline(() -> {
            var conn = connectionService.getConnection();
            var stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE email = ? LIMIT 1");
            stmt.setString(1, email);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(Hydrator.mapRow(rs, User.class));
            }
            return Optional.empty();
        });
    }

}
