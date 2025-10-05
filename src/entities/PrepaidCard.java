package entities;

import java.util.Map;

import enums.CardStatus;
import enums.CardType;

final public class PrepaidCard extends Card {
    public static final String TABLE_NAME = "prepaid_cards";
    private double availableBalance;
    public static Map<String, Object> OFFER1 = Map.of("available_balance", 2000.0);
    public static Map<String, Object> OFFER2 = Map.of("available_balance", 10000.0);
    public static Map<String, Object> OFFER3 = Map.of("available_balance", 15000.0);

    public PrepaidCard(int id, String expirationDate, String status, int userId,
            double availableBalance) {
        super(id, expirationDate, status, CardType.PREPAID.name(), userId);
        this.availableBalance = availableBalance;
    }

    // Constructor with enum for business logic convenience
    public PrepaidCard(int id, String expirationDate, CardStatus status, int userId,
            double availableBalance) {
        super(id, expirationDate, status.name(), CardType.PREPAID.name(), userId);
        this.availableBalance = availableBalance;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(double availableBalance) {
        this.availableBalance = availableBalance;
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
