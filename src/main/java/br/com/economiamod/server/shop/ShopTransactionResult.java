package br.com.economiamod.server.shop;

public record ShopTransactionResult(
        ShopTransactionResultType type,
        long amount
) {
    public static ShopTransactionResult completed(long amount) {
        return new ShopTransactionResult(ShopTransactionResultType.COMPLETED, amount);
    }

    public static ShopTransactionResult invalid(ShopTransactionResultType type) {
        return new ShopTransactionResult(type, 0L);
    }
}
