package repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import entities.CardOperation;
import services.DBConnection;
import utils.Console;
import utils.Hydrator;

public class CardOperationRepository implements RepositoryBase<CardOperation> {
    public static final String TABLE_NAME = "card_operations";
    private DBConnection connection;

    public CardOperationRepository(DBConnection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<CardOperation> findById(String id) {
        return pipeline(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * from " + TABLE_NAME + " WHERE id = ? LIMIT 1", id);
            if (rs.next())
                return Optional.of(Hydrator.mapRow(Hydrator.toMap(rs), CardOperation.class));
            return Optional.empty();
        });
    }

    @Override
    public List<CardOperation> findAll() {
        return pipeline(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * FROM " + TABLE_NAME);
            ArrayList<CardOperation> co = new ArrayList<>();
            while (rs.next()) {
                co.add(Hydrator.mapRow(Hydrator.toMap(rs), CardOperation.class));
            }
            return co;
        });
    }

    @Override
    public CardOperation create(Map<String, Object> data) {
        return pipeline(() -> {
            var conn = connection.getConnection();
            var stmt = conn.prepareStatement(
                    "INSERT INTO " + TABLE_NAME + " " + fieldsOf(data) + " VALUES "
                            + bindingTemplateOf(data));

            int index = 1;
            for (Object val : data.values()) {
                // Special handling for UUID values
                if (val instanceof UUID) {
                    stmt.setString(index++, val.toString());
                } else {
                    stmt.setObject(index++, val);
                }
            }

            int isAffected = stmt.executeUpdate();
            if (isAffected == 0)
                throw new Exception("Failed to create Card Operation");

            return Hydrator.mapRow(data, CardOperation.class);
        });
    }

    @Override
    public void deleteById(String id) {
        pipeline(() -> {
            var conn = connection.getConnection();
            executeUpdate(conn, "DELETE FROM " + TABLE_NAME + " WHERE id = ?", id);
        });
    }

    @Override
    public void update(CardOperation co, Map<String, Object> data) {
        CardOperation[] cardOperationRef = { co };
        cardOperationRef[0] = pipeline(() -> {
            Map<String, Object> filteredData = filterToCOU(data);
            var conn = connection.getConnection();
            var stmt = conn
                    .prepareStatement("UPDATE " + TABLE_NAME + " " + setClauseOf(filteredData) + " WHERE id = ?");

            int index = 1;
            for (Object val : data.values()) {
                stmt.setObject(index++, val);
            }
            stmt.setString(index, co.id().toString());
            stmt.executeUpdate();

            return Hydrator.mapRow(filteredData, CardOperation.class);
        });
    }

    public List<CardOperation> findCardOperationsOf(String cardId) {
        return pipeline(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * FROM " + TABLE_NAME + " WHERE card_id = ?", cardId);
            ArrayList<CardOperation> co = new ArrayList<>();
            while (rs.next()) {
                try {
                    CardOperation operation = Hydrator.mapRow(Hydrator.toMap(rs), CardOperation.class);
                    co.add(operation);
                } catch (Exception e) {
                    // Log the error but continue processing other operations
                    Console.error("Failed to hydrate operation: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return co;
        });
    }
}
