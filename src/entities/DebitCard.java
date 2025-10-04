package entities;

import java.util.Map;

import enums.CardStatus;
import enums.CardType;

final public class DebitCard extends Card {
    public static final String TABLE_NAME = "debit_cards";
    private double dailyLimit;
    public static Map<String, Object> OFFER1 = Map.of("daily_limit", 500.0);
    public static Map<String, Object> OFFER2 = Map.of("daily_limit", 1000.0);
    public static Map<String, Object> OFFER3 = Map.of("daily_limit", 2000.0);

    public DebitCard(int id, String expirationDate, String status, int userId, double dailyLimit) {
        super(id, expirationDate, status, CardType.DEBIT.name(), userId);
        this.dailyLimit = dailyLimit;
    }

    // Constructor with enum for business logic convenience
    public DebitCard(int id, String expirationDate, CardStatus status, int userId,
            double dailyLimit) {
        super(id, expirationDate, status.name(), CardType.DEBIT.name(), userId);
        this.dailyLimit = dailyLimit;
    }

    public double getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(double dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public static Map<String, Object> getOffer(int offerIdx) throws Exception {
        switch (offerIdx) {
            case 1:
                return OFFER1;
            case 2:
                return OFFER2;
            case 3:
                return OFFER3;
            default:
                throw new Exception("No such offer");
        }
    }
}
