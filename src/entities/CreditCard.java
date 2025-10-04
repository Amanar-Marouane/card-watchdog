package entities;

import java.util.Map;

import enums.CardStatus;
import enums.CardType;

final public class CreditCard extends Card {
    public static final String TABLE_NAME = "credit_cards";
    private double monthlyLimit;
    private double interestRate;
    public static Map<String, Object> OFFER1 = Map.of("monthly_limit", 2000.0, "interest_rate", 2.5);
    public static Map<String, Object> OFFER2 = Map.of("monthly_limit", 5000.0, "interest_rate", 3.5);
    public static Map<String, Object> OFFER3 = Map.of("monthly_limit", 10000.0, "interest_rate", 5.0);

    public CreditCard(int id, String expirationDate, String status, int userId, double monthlyLimit,
            double interestRate) {
        super(id, expirationDate, status, CardType.CREDIT.name(), userId);
        this.monthlyLimit = monthlyLimit;
        this.interestRate = interestRate;
    }

    // Constructor with enum for business logic convenience
    public CreditCard(int id, String expirationDate, CardStatus status, int userId, double monthlyLimit,
            double interestRate) {
        super(id, expirationDate, status.name(), CardType.CREDIT.name(), userId);
        this.monthlyLimit = monthlyLimit;
        this.interestRate = interestRate;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
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
