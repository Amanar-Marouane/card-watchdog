package entities;

import enums.CardType;
import enums.CardStatus;

final public class PrepaidCard extends Card {
    public static final String TABLE_NAME = "prepaid_cards";
    private double soldeDisponible;

    public PrepaidCard(String expirationDate, String status, int userId,
            double soldeDisponible) {
        super(expirationDate, status, CardType.PREPAID.name(), userId);
        this.soldeDisponible = soldeDisponible;
    }

    // Constructor with enum for business logic convenience
    public PrepaidCard(String expirationDate, CardStatus status, int userId,
            double soldeDisponible) {
        super(expirationDate, status.name(), CardType.PREPAID.name(), userId);
        this.soldeDisponible = soldeDisponible;
    }

    public double getSoldeDisponible() {
        return soldeDisponible;
    }

    public void setSoldeDisponible(double soldeDisponible) {
        this.soldeDisponible = soldeDisponible;
    }
}
