package br.com.economiamod.server.claim;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.ItemStack;

public final class ClaimPurchaseSessionService {
    public static final ClaimPurchaseSessionService INSTANCE = new ClaimPurchaseSessionService();
    private static final long TIMEOUT_MILLIS = 5L * 60L * 1_000L;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    private ClaimPurchaseSessionService() {
    }

    public void open(UUID playerUuid, UUID anchorId, ItemStack authenticatedCard) {
        ItemStack card = authenticatedCard.copy();
        card.setCount(1);
        sessions.put(playerUuid, new Session(anchorId, card, System.currentTimeMillis() + TIMEOUT_MILLIS));
    }

    public ItemStack consume(UUID playerUuid, UUID anchorId) {
        Session session = sessions.remove(playerUuid);
        if (session == null || !session.anchorId().equals(anchorId)
                || session.expiresAtMillis() < System.currentTimeMillis()) {
            return ItemStack.EMPTY;
        }
        return session.card();
    }

    public void clear(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    public void clearAll() {
        sessions.clear();
    }

    private record Session(UUID anchorId, ItemStack card, long expiresAtMillis) {
    }
}
