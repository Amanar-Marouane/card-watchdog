package repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import entities.Card;
import entities.CreditCard;
import entities.DebitCard;
import entities.PrepaidCard;
import enums.CardType;
import services.DBConnection;
import utils.Console;
import utils.Hydrator;

public class CardRepository implements RepositoryBase<Card> {
    public static final String TABLE_NAME = "cards";
    public final DBConnection connection;

    public CardRepository(DBConnection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Card> findById(String id) {
        return pipeline(() -> {
            var conn = connection.getConnection();
            var stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE id = ? LIMIT 1");
            stmt.setString(1, id);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                // it throws use .toUpperCase()
                CardType ct = CardType.valueOf(rs.getString("card_type"));
                switch (ct) {
                    case PREPAID:
                        return Optional.of(this.fetchSupType(conn, id, PrepaidCard.class, PrepaidCard.TABLE_NAME, rs));
                    case DEBIT:
                        return Optional.of(this.fetchSupType(conn, id, DebitCard.class, DebitCard.TABLE_NAME, rs));
                    case CREDIT:
                        return Optional.of(this.fetchSupType(conn, id, CreditCard.class, CreditCard.TABLE_NAME, rs));
                    default:
                        throw new NoSuchElementException("no such type");
                }
            }
            return Optional.empty();
        });
    }

    @Override
    public List<Card> findAll() {
        return pipeline(() -> {
            var conn = connection.getConnection();
            var stmt = conn.prepareStatement("SELECT * from " + TABLE_NAME);
            var rs = stmt.executeQuery();
            List<Card> cards = new ArrayList<>();
            while (rs.next()) {
                // it throws use .toUpperCase()
                CardType ct = CardType.valueOf(rs.getString("card_type"));
                String id = rs.getString("id");
                switch (ct) {
                    case PREPAID:
                        cards.add(this.fetchSupType(conn, id, PrepaidCard.class, PrepaidCard.TABLE_NAME, rs));
                        break;
                    case DEBIT:
                        cards.add(this.fetchSupType(conn, id, DebitCard.class, DebitCard.TABLE_NAME, rs));
                        break;
                    case CREDIT:
                        cards.add(this.fetchSupType(conn, id, CreditCard.class, CreditCard.TABLE_NAME, rs));
                        break;
                    default:
                        throw new NoSuchElementException("no such type");
                }
            }
            return cards;
        });
    }

    private <T extends Card> T fetchSupType(Connection conn, String id, Class<T> clazz, String table, ResultSet baseRow)
            throws NoSuchElementException {
        return pipeline(() -> {
            var stmt = conn.prepareStatement("SELECT * FROM " + table + " WHERE card_id = ?");
            stmt.setString(1, id);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> baseMap = Hydrator.toMap(baseRow);
                Map<String, Object> subMap = Hydrator.toMap(rs);
                baseMap.putAll(subMap);
                return Hydrator.mapRow(baseMap, clazz);
            } else {
                throw new NoSuchElementException("No sub type found for card with id " + id);
            }
        });
    }

    @Override
    public Card create(Map<String, Object> data) {
        return pipeline(() -> {
            // Exclude ID if present
            Map<String, Object> filteredData = data.entrySet().stream()
                    .filter(e -> !"id".equals(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!filteredData.containsKey("card_type"))
                throw new Exception("No card type provided");

            if (filteredData.isEmpty())
                throw new Exception("No data provided");

            Map<String, Object> offer = this.getOffer(new HashMap<>(filteredData));
            filteredData.remove("offer");
            var conn = connection.getConnection();

            String fields = "(" + String.join(", ", filteredData.keySet()) + ")";
            String bindingTemplate = "(" + String.join(", ", Collections.nCopies(filteredData.size(), "?")) + ")";

            var stmt = conn.prepareStatement("INSERT INTO " + TABLE_NAME + " " + fields + " VALUES " + bindingTemplate,
                    java.sql.Statement.RETURN_GENERATED_KEYS);

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
                    return this.setAndGetSubTypeCard(id, new HashMap<>(offer), new HashMap<>(data));
                } else
                    throw new Exception("Creating user failed, no ID obtained.");
            }
        });
    }

    private Map<String, Object> getOffer(Map<String, Object> data) throws Exception, NoSuchElementException {
        CardType ct = CardType.valueOf(data.get("card_type").toString());

        int offerId = (int) data.get("offer");

        switch (ct) {
            case PREPAID:
                return PrepaidCard.getOffer(offerId);
            case CREDIT:
                return CreditCard.getOffer(offerId);
            case DEBIT:
                return DebitCard.getOffer(offerId);
            default:
                throw new NoSuchElementException("No such type");
        }
    }

    private Card setAndGetSubTypeCard(int cardId, Map<String, Object> rawOfferData,
            Map<String, Object> rawCardData)
            throws NoSuchElementException {
        return pipeline(() -> {
            rawOfferData.put("card_id", cardId);
            CardType ct = CardType.valueOf(rawCardData.get("card_type").toString());
            var conn = connection.getConnection();

            String[] columns = rawOfferData.keySet().toArray(new String[0]);
            String fields = "(" + String.join(", ", columns) + ")";
            Console.info("Feilds" + fields);
            Console.info("Table => " + getTableNameByType(ct));
            String bindingTemplate = "("
                    + String.join(", ", Collections.nCopies(columns.length, "?")) + ")";
            Console.info("bindingTemplate" + bindingTemplate);

            var stmt = conn.prepareStatement("INSERT INTO " + getTableNameByType(ct) + " " + fields
                    + " VALUES " + bindingTemplate);

            int index = 1;
            for (Object col : columns) {
                stmt.setObject(index++, rawOfferData.get(col));
            }

            stmt.executeUpdate();

            Map<String, Object> mergedData = new HashMap<>(rawCardData);
            mergedData.putAll(rawOfferData);
            switch (ct) {
                case PREPAID:
                    return Hydrator.mapRow(mergedData, PrepaidCard.class);
                case DEBIT:
                    return Hydrator.mapRow(mergedData, DebitCard.class);
                case CREDIT:
                    return Hydrator.mapRow(mergedData, CreditCard.class);
                default:
                    throw new NoSuchElementException("No such a card type");
            }
        });
    }

    private String getTableNameByType(CardType ct) {
        switch (ct) {
            case PREPAID:
                return PrepaidCard.TABLE_NAME;
            case DEBIT:
                return DebitCard.TABLE_NAME;
            case CREDIT:
                return CreditCard.TABLE_NAME;
            default:
                throw new NoSuchElementException("No such a card type");
        }
    }

    public void updateById(Card entity, Map<String, Object> fieldsToUpdate) {
        if (fieldsToUpdate.containsKey("id"))
            fieldsToUpdate.remove("id"); // prevent ID modification

        if (fieldsToUpdate.isEmpty())
            return;

        Card[] entityRef = { entity };

        entityRef[0] = pipeline(() -> {

            int offer = Integer.parseInt(fieldsToUpdate.getOrDefault("offer", -1).toString());
            fieldsToUpdate.remove("offer");

            var conn = connection.getConnection();

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
            stmt.setObject(index, entity.getId());
            stmt.executeUpdate();

            if (offer != -1) {
                Map<String, Object> offerData = this
                        .getOffer(Map.of("card_type", entity.getCardType().toString(), "offer", offer));

                var feilds = String.join(", ", offerData.keySet().stream()
                        .map(field -> field + " = ?")
                        .collect(Collectors.toList()));

                switch (entity.getCardTypeEnum()) {
                    case PREPAID:
                        var stmt2 = conn.prepareStatement(
                                "UPDATE " + PrepaidCard.TABLE_NAME + " SET " + feilds + " WHERE card_id = ?");

                        int index2 = 1;
                        for (Object value : offerData.values()) {
                            stmt2.setObject(index2++, value);
                        }
                        stmt2.setObject(index2, entity.getId());
                        stmt2.executeUpdate();
                        break;
                    case DEBIT:
                        var stmt3 = conn.prepareStatement(
                                "UPDATE " + DebitCard.TABLE_NAME + " SET " + feilds + " WHERE card_id = ?");
                        int index3 = 1;
                        for (Object value : offerData.values()) {
                            stmt3.setObject(index3++, value);
                        }
                        stmt3.setObject(index3, entity.getId());
                        stmt3.executeUpdate();
                        break;
                    case CREDIT:
                        var stmt4 = conn.prepareStatement(
                                "UPDATE " + CreditCard.TABLE_NAME + " SET " + feilds + " WHERE card_id = ?");
                        int index4 = 1;
                        for (Object value : offerData.values()) {
                            stmt4.setObject(index4++, value);
                        }
                        stmt4.setObject(index4, entity.getId());
                        stmt4.executeUpdate();
                        break;
                    default:
                        break;
                }
            }
            Map<String, Object> mergedData = new HashMap<>(fieldsToUpdate);
            mergedData.putAll(fieldsToUpdate);

            return Hydrator.mapRow(mergedData, entity.getClass());
        });
    }

    public void deleteById(String id) {
        pipeline(() -> {
            var conn = connection.getConnection();
            var stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE id = ?");
            stmt.setString(1, id);
            var rs = stmt.executeQuery();
            if (!rs.next())
                throw new NoSuchElementException("No card with id " + id);

            CardType ct = CardType.valueOf(rs.getString("card_type"));
            switch (ct) {
                case PREPAID:
                    var stmt2 = conn.prepareStatement("DELETE FROM " + PrepaidCard.TABLE_NAME + " WHERE card_id = ?");
                    stmt2.setString(1, id);
                    stmt2.executeUpdate();
                    break;
                case DEBIT:
                    var stmt3 = conn.prepareStatement("DELETE FROM " + DebitCard.TABLE_NAME + " WHERE card_id = ?");
                    stmt3.setString(1, id);
                    stmt3.executeUpdate();
                    break;
                case CREDIT:
                    var stmt4 = conn.prepareStatement("DELETE FROM " + CreditCard.TABLE_NAME + " WHERE card_id = ?");
                    stmt4.setString(1, id);
                    stmt4.executeUpdate();
                    break;
                default:
                    throw new NoSuchElementException("no such type");
            }
            var stmt5 = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE id = ?");
            stmt5.setString(1, id);
            stmt5.executeUpdate();
        });
    }
}
