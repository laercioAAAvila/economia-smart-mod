package br.com.economiamod.server.transaction;

public record MenuPaymentResult(boolean success, String code) {
    public static MenuPaymentResult completed() {
        return new MenuPaymentResult(true, "completed");
    }

    public static MenuPaymentResult denied(String code) {
        return new MenuPaymentResult(false, code);
    }
}
