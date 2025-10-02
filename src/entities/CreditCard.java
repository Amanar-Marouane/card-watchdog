package entities;

import enums.CardType;
import enums.CardStatus;

final public class CreditCard extends Card {
    public static final String TABLE_NAME = "credit_cards";
    private double plafondMensuel;
    private double tauxInteret;

    public CreditCard(String expirationDate, String status, int userId, double plafondMensuel,
            double tauxInteret) {
        super(expirationDate, status, CardType.CREDIT.name(), userId);
        this.plafondMensuel = plafondMensuel;
        this.tauxInteret = tauxInteret;
    }

    // Constructor with enum for business logic convenience
    public CreditCard(String expirationDate, CardStatus status, int userId, double plafondMensuel,
            double tauxInteret) {
        super(expirationDate, status.name(), CardType.CREDIT.name(), userId);
        this.plafondMensuel = plafondMensuel;
        this.tauxInteret = tauxInteret;
    }

    public double getPlafondMensuel() {
        return plafondMensuel;
    }

    public double getTauxInteret() {
        return tauxInteret;
    }

    public void setPlafondMensuel(double plafondMensuel) {
        this.plafondMensuel = plafondMensuel;
    }

    public void setTauxInteret(double tauxInteret) {
        this.tauxInteret = tauxInteret;
    }
}
