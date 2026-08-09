package br.com.economiamod.common.claim;

public enum DirectPaymentMethod {
    CASH,
    DEBIT,
    CREDIT;

    public static DirectPaymentMethod parse(String value) {
        if (value == null) {
            return DEBIT;
        }
        try {
            return valueOf(value.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return DEBIT;
        }
    }
}
