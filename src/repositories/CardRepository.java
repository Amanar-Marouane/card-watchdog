package repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import entities.Card;
import entities.CreditCard;
import entities.DebitCard;
import entities.PrepaidCard;
import enums.CardType;
import services.DBConnection;
import utils.CaseConverter;
import utils.Hydrator;

public class CardRepository extends RepositoryBase implements RepositoryContract<Card> {
    public static final String TABLE_NAME = "cards";
    public final DBConnection connection;

    public CardRepository(DBConnection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Card> findById(String id) {
        return executeSafely(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * FROM " + TABLE_NAME + " WHERE id = ? LIMIT 1", id);

            if (rs.next()) {
                return Optional.of(createCardFromResultSet(conn, rs));
            }
            return Optional.empty();
        });
    }

    @Override
    public List<Card> findAll() {
        return executeSafely(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * from " + TABLE_NAME);
            List<Card> cards = new ArrayList<>();

            while (rs.next()) {
                cards.add(createCardFromResultSet(conn, rs));
            }
            return cards;
        });
    }

    /**
     * Creates a card entity from a result set based on its type
     */
    private Card createCardFromResultSet(Connection conn, ResultSet rs) throws Exception {
        CardType ct = CardType.valueOf(rs.getString("card_type"));
        String id = rs.getString("id");

        return switch (ct) {
            case PREPAID -> fetchSupType(conn, id, PrepaidCard.class, PrepaidCard.TABLE_NAME, rs);
            case DEBIT -> fetchSupType(conn, id, DebitCard.class, DebitCard.TABLE_NAME, rs);
            case CREDIT -> fetchSupType(conn, id, CreditCard.class, CreditCard.TABLE_NAME, rs);
        };
    }

    /**
     * Fetches the subtype-specific data and creates the appropriate card entity
     */
    private <T extends Card> T fetchSupType(Connection conn, String id, Class<T> clazz, String table, ResultSet baseRow)
            throws Exception {
        return executeSafely(() -> {
            var rs = executeQuery(conn, "SELECT * FROM " + table + " WHERE card_id = ?", id);

            if (rs.next()) {
                Map<String, Object> baseMap = Hydrator.resultSetToMap(baseRow);
                Map<String, Object> subMap = Hydrator.resultSetToMap(rs);
                baseMap.putAll(subMap);
                return Hydrator.mapRow(baseMap, clazz);
            } else {
                throw new NoSuchElementException("No sub type found for card with id " + id);
            }
        });
    }

    @Override
    public Card create(Map<String, Object> data) {
        return executeSafely(() -> {
            Map<String, Object> filteredData = filterID(data);

            if (!filteredData.containsKey("card_type"))
                throw new Exception("No card type provided");

            Map<String, Object> offer = getOffer(filteredData);
            filteredData.remove("offer");

            // Insert base card record and get ID
            int cardId = insertBaseCard(filteredData);

            // Insert subtype data and get complete card object
            return insertSubTypeCard(cardId, offer, filteredData);
        });
    }

    /**
     * Insert the base card record and return the generated ID
     */
    private int insertBaseCard(Map<String, Object> cardData) throws Exception {
        var conn = connection.getConnection();
        String fields = fieldsOf(cardData);
        String bindingTemplate = bindingTemplateOf(cardData);

        var stmt = conn.prepareStatement(
                "INSERT INTO " + TABLE_NAME + " " + fields + " VALUES " + bindingTemplate,
                java.sql.Statement.RETURN_GENERATED_KEYS);

        int index = 1;
        for (Object value : cardData.values()) {
            stmt.setObject(index++, value);
        }

        int affectedRows = stmt.executeUpdate();
        if (affectedRows == 0)
            throw new Exception("Creating card failed, no rows affected.");

        try (var generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            } else
                throw new Exception("Creating card failed, no ID obtained.");
        }
    }

    /**
     * Insert subtype card data and return the complete card
     */
    private Card insertSubTypeCard(int cardId, Map<String, Object> offerData, Map<String, Object> cardData)
            throws Exception {
        Map<String, Object> subCardData = new HashMap<>(offerData);
        subCardData.put("card_id", cardId);
        CardType ct = CardType.valueOf(cardData.get("card_type").toString());
        var conn = connection.getConnection();

        String fields = fieldsOf(subCardData);
        String bindingTemplate = bindingTemplateOf(subCardData);
        String tableName = getTableNameByType(ct);

        var stmt = conn.prepareStatement("INSERT INTO " + tableName + " " + fields + " VALUES " + bindingTemplate);

        int index = 1;
        for (Object value : subCardData.values()) {
            stmt.setObject(index++, value);
        }

        stmt.executeUpdate();

        // Combine data for hydration
        Map<String, Object> mergedData = new HashMap<>(cardData);
        mergedData.putAll(subCardData);
        mergedData.put("id", cardId);

        return createCardInstance(ct, mergedData);
    }

    /**
     * Create a card instance of the appropriate type
     */
    private Card createCardInstance(CardType cardType, Map<String, Object> data) throws Exception {
        return switch (cardType) {
            case PREPAID -> Hydrator.mapRow(data, PrepaidCard.class);
            case DEBIT -> Hydrator.mapRow(data, DebitCard.class);
            case CREDIT -> Hydrator.mapRow(data, CreditCard.class);
        };
    }

    private Map<String, Object> getOffer(Map<String, Object> data) throws Exception {
        CardType ct = CardType.valueOf(data.get("card_type").toString());
        int offerId = (int) data.get("offer");

        return switch (ct) {
            case PREPAID -> PrepaidCard.getOffer(offerId);
            case CREDIT -> CreditCard.getOffer(offerId);
            case DEBIT -> DebitCard.getOffer(offerId);
        };
    }

    private String getTableNameByType(CardType ct) {
        return switch (ct) {
            case PREPAID -> PrepaidCard.TABLE_NAME;
            case DEBIT -> DebitCard.TABLE_NAME;
            case CREDIT -> CreditCard.TABLE_NAME;
        };
    }

    @Override
    public void update(Card entity, Map<String, Object> data) {
        if (data.isEmpty())
            return;
        Map<String, Object> fieldsToUpdate = new HashMap<>(data);
        Card[] entityRef = { entity };

        entityRef[0] = executeSafely(() -> {
            // Extract offer and remove ID
            int offer = Integer.parseInt(fieldsToUpdate.getOrDefault("offer", -1).toString());
            fieldsToUpdate.remove("offer");
            fieldsToUpdate.remove("id");

            // Split fields between base and subtype tables
            CardType cardType = entity.getCardTypeEnum();
            var fieldMaps = separateFields(cardType, fieldsToUpdate);
            Map<String, Object> baseCardFields = fieldMaps.get("base");
            Map<String, Object> subtypeFields = fieldMaps.get("subtype");

            var conn = connection.getConnection();

            // Update base card fields if any
            updateBaseCardFields(conn, entity.getId(), baseCardFields);

            // Update subtype-specific fields if any
            updateSubtypeFields(conn, entity.getId(), cardType, subtypeFields);

            // Handle special offer update if needed
            if (offer != -1) {
                updateCardOffer(conn, entity, offer);
            }

            // Return updated entity
            Map<String, Object> mergedData = new HashMap<>(baseCardFields);
            mergedData.putAll(subtypeFields);
            mergedData.put("id", entity.getId());
            return Hydrator.mapRow(mergedData, entity.getClass());
        });
    }

    /**
     * Separates fields into base card fields and subtype-specific fields
     */
    private Map<String, Map<String, Object>> separateFields(CardType cardType, Map<String, Object> allFields) {
        Map<String, Object> baseCardFields = new HashMap<>();
        Map<String, Object> subtypeFields = new HashMap<>();
        Set<String> subtypeFieldNames = getSubtypeFieldNames(cardType);

        for (Map.Entry<String, Object> entry : new HashMap<>(allFields).entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            String snakeCase = CaseConverter.camelToSnake(field);
            if (subtypeFieldNames.contains(field) || subtypeFieldNames.contains(snakeCase)) {
                subtypeFields.put(field, value);
                allFields.remove(field); // Remove it from the original map
            } else {
                baseCardFields.put(field, value);
            }
        }

        Map<String, Map<String, Object>> result = new HashMap<>();
        result.put("base", baseCardFields);
        result.put("subtype", subtypeFields);
        return result;
    }

    /**
     * Update fields in the base cards table
     */
    private void updateBaseCardFields(Connection conn, int cardId, Map<String, Object> fields) throws Exception {
        if (fields.isEmpty())
            return;

        String setClause = setClauseOf(fields);
        var stmt = conn.prepareStatement("UPDATE " + TABLE_NAME + " SET " + setClause + " WHERE id = ?");

        int index = 1;
        for (Object value : fields.values()) {
            stmt.setObject(index++, value);
        }
        stmt.setObject(index, cardId);
        stmt.executeUpdate();
    }

    /**
     * Update fields in the subtype table
     */
    private void updateSubtypeFields(Connection conn, int cardId, CardType cardType,
            Map<String, Object> fields) throws Exception {
        if (fields.isEmpty())
            return;

        String subtableSetClause = setClauseOf(fields);
        var subtypeStmt = conn.prepareStatement(
                "UPDATE " + getTableNameByType(cardType) +
                        " SET " + subtableSetClause +
                        " WHERE card_id = ?");

        int subtypeIndex = 1;
        for (Object value : fields.values()) {
            subtypeStmt.setObject(subtypeIndex++, value);
        }
        subtypeStmt.setObject(subtypeIndex, cardId);
        subtypeStmt.executeUpdate();
    }

    /**
     * Update card with a specific offer
     */
    private void updateCardOffer(Connection conn, Card entity, int offerId) throws Exception {
        Map<String, Object> offerData = getOffer(Map.of(
                "card_type", entity.getCardType().toString(),
                "offer", offerId));

        String offerSetClause = setClauseOf(offerData);
        var offerStmt = conn.prepareStatement(
                "UPDATE " + getTableNameByType(entity.getCardTypeEnum()) +
                        " SET " + offerSetClause +
                        " WHERE card_id = ?");

        int offerIndex = 1;
        for (Object value : offerData.values()) {
            offerStmt.setObject(offerIndex++, value);
        }
        offerStmt.setObject(offerIndex, entity.getId());
        offerStmt.executeUpdate();
    }

    // Helper method to get field names for specific card subtypes
    private Set<String> getSubtypeFieldNames(CardType cardType) {
        Set<String> fieldNames = new HashSet<>();

        switch (cardType) {
            case CREDIT:
                fieldNames.add("monthly_limit");
                fieldNames.add("monthlyLimit");
                fieldNames.add("interest_rate");
                fieldNames.add("interestRate");
                break;
            case DEBIT:
                fieldNames.add("daily_limit");
                fieldNames.add("dailyLimit");
                break;
            case PREPAID:
                fieldNames.add("available_balance");
                fieldNames.add("availableBalance");
                break;
        }

        return fieldNames;
    }

    @Override
    public void deleteById(String id) {
        executeSafely(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * FROM " + TABLE_NAME + " WHERE id = ?", id);

            if (!rs.next()) {
                throw new NoSuchElementException("No card with id " + id);
            }

            // Delete from subtype table first
            CardType ct = CardType.valueOf(rs.getString("card_type"));
            String subtypeTable = getTableNameByType(ct);

            executeUpdate(conn, "DELETE FROM " + subtypeTable + " WHERE card_id = ?", id);

            // Then delete from base table
            executeUpdate(conn, "DELETE FROM " + TABLE_NAME + " WHERE id = ?", id);
        });
    }

    public List<Card> findAllByUserId(String userId) {
        return executeSafely(() -> {
            var conn = connection.getConnection();
            var rs = executeQuery(conn, "SELECT * from " + TABLE_NAME + " WHERE user_id = ?", userId);
            List<Card> cards = new ArrayList<>();

            while (rs.next()) {
                cards.add(createCardFromResultSet(conn, rs));
            }
            return cards;
        });
    }
}
