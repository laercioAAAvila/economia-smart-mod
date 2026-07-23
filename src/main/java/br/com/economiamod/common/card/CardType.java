package br.com.economiamod.common.card;

public enum CardType {
    DEBIT(true, false),
    CREDIT(false, true),
    DEBIT_CREDIT(true, true);

    private final boolean debit;
    private final boolean credit;

    CardType(boolean debit, boolean credit) {
        this.debit = debit;
        this.credit = credit;
    }

    public boolean hasDebit() {
        return debit;
    }

    public boolean hasCredit() {
        return credit;
    }
}

