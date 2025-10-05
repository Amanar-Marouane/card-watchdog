package repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import entities.FraudAlert;
import enums.AlertLevel;
import services.DBConnection;
import utils.Hydrator;

public class FraudAlertRepository extends RepositoryBase implements RepositoryContract<FraudAlert> {
    public static final String TABLE_NAME = "fraud_alerts";
    private final DBConnection connection;

    public FraudAlertRepository(DBConnection connection) {
        this.connection = connection;
    }

    @Override
    public List<FraudAlert> findAll() {
        return executeSafely(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * FROM " + TABLE_NAME);
            ArrayList<FraudAlert> alerts = new ArrayList<>();
            while (rs.next()) {
                alerts.add(Hydrator.mapRow(Hydrator.resultSetToMap(rs), FraudAlert.class));
            }
            return alerts;
        });
    }

    @Override
    public FraudAlert create(Map<String, Object> data) {
        return executeSafely(() -> {
            Map<String, Object> filteredData = filterID(data);
            List<Object> params = new ArrayList<>();
            for (String col : filteredData.keySet()) {
                params.add(filteredData.get(col));
            }

            var conn = connection.getConnection();
            int insertedId = executeUpdate(conn, "INSERT INTO " + TABLE_NAME + " " + fieldsOf(filteredData) + " VALUES "
                    + bindingTemplateOf(filteredData), filteredData.values().toArray());
            filteredData.put("id", insertedId);
            return Hydrator.mapRow(filteredData, FraudAlert.class);
        });
    }

    @Override
    public Optional<FraudAlert> findById(String id) {
        return executeSafely(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * FROM " + TABLE_NAME + " WHERE id = ? LIMIT 1", id);
            if (rs.next())
                return Optional.of(Hydrator.mapRow(Hydrator.resultSetToMap(rs), FraudAlert.class));
            return Optional.empty();
        });
    }

    @Override
    public void update(FraudAlert alert, Map<String, Object> fieldsToUpdate) {
        final FraudAlert[] alertRef = { alert };
        alertRef[0] = executeSafely(() -> {
            Map<String, Object> filteredData = filterID(fieldsToUpdate);
            var conn = connection.getConnection();
            executeUpdate(conn, "UPDATE " + TABLE_NAME + " SET " + setClauseOf(filteredData) + " WHERE id = ?",
                    filteredData);

            filteredData.put("id", alert.id());
            return Hydrator.mapRow(filteredData, FraudAlert.class);
        });
    }

    @Override
    public void deleteById(String id) {
        executeSafely(() -> {
            var conn = connection.getConnection();
            executeUpdate(conn, "DELETE FROM " + TABLE_NAME + " WHERE id = ?", id);
        });
    }

    public List<FraudAlert> findByCardId(int cardId) {
        return executeSafely(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * FROM " + TABLE_NAME + " WHERE card_id = ?", cardId);
            ArrayList<FraudAlert> alerts = new ArrayList<>();
            while (rs.next()) {
                alerts.add(Hydrator.mapRow(Hydrator.resultSetToMap(rs), FraudAlert.class));
            }
            return alerts;
        });
    }

    public List<FraudAlert> findByAlertLevelWhereCardId(AlertLevel level, int cardId) {
        return executeSafely(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * FROM " + TABLE_NAME + " WHERE car_id = ? AND level = ?", cardId,
                    level);
            ArrayList<FraudAlert> alerts = new ArrayList<>();
            while (rs.next()) {
                alerts.add(Hydrator.mapRow(Hydrator.resultSetToMap(rs), FraudAlert.class));
            }
            return alerts;
        });
    }
}
