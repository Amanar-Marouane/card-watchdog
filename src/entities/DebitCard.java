package entities;

import enums.CardType;
import enums.CardStatus;

final public class DebitCard extends Card {
    public static final String TABLE_NAME = "debit_cards";
    private double plafondJournalier;

    public DebitCard(String expirationDate, String status, int userId, double plafondJournalier) {
        super(expirationDate, status, CardType.DEBIT.name(), userId);
        this.plafondJournalier = plafondJournalier;
    }

    // Constructor with enum for business logic convenience
    public DebitCard(String expirationDate, CardStatus status, int userId,
            double plafondJournalier) {
        super(expirationDate, status.name(), CardType.DEBIT.name(), userId);
        this.plafondJournalier = plafondJournalier;
    }

    public double getPlafondJournalier() {
        return plafondJournalier;
    }

    public void setPlafondJournalier(double plafondJournalier) {
        this.plafondJournalier = plafondJournalier;
    }
}
