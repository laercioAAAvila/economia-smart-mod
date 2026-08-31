package br.com.economiamod.server.web;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** One-time short-lived login tickets created inside Minecraft and redeemed by the website. */
public final class WebLoginTicketService {
    public static final WebLoginTicketService INSTANCE = new WebLoginTicketService();
    public static final int DEFAULT_TTL_SECONDS = 120;

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int RAW_LENGTH = 16;
    private static final int MAX_TICKETS = 4096;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    private WebLoginTicketService() {
    }

    public String create(UUID accountId, UUID playerUuid) {
        if (accountId == null || playerUuid == null) {
            throw new IllegalArgumentException("accountId/playerUuid required");
        }
        purgeExpired();
        tickets.entrySet().removeIf(entry -> accountId.equals(entry.getValue().accountId())
                || playerUuid.equals(entry.getValue().playerUuid()));
        if (tickets.size() >= MAX_TICKETS) {
            throw new IllegalStateException("web login ticket capacity reached");
        }

        String raw;
        do {
            StringBuilder builder = new StringBuilder(RAW_LENGTH);
            for (int index = 0; index < RAW_LENGTH; index++) {
                builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            raw = builder.toString();
        } while (tickets.containsKey(raw));

        tickets.put(raw, new Ticket(accountId, playerUuid, Instant.now().plusSeconds(DEFAULT_TTL_SECONDS)));
        return grouped(raw);
    }

    public Optional<Ticket> redeem(String suppliedToken) {
        String raw = normalize(suppliedToken);
        if (raw.length() != RAW_LENGTH) {
            return Optional.empty();
        }
        Ticket ticket = tickets.remove(raw);
        if (ticket == null || Instant.now().isAfter(ticket.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(ticket);
    }

    public void clear() {
        tickets.clear();
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(RAW_LENGTH);
        for (int index = 0; index < value.length(); index++) {
            char current = Character.toUpperCase(value.charAt(index));
            if (current == '-' || Character.isWhitespace(current)) {
                continue;
            }
            if ((current >= 'A' && current <= 'Z') || (current >= '0' && current <= '9')) {
                result.append(current);
            } else {
                return "";
            }
        }
        return result.toString();
    }

    private String grouped(String raw) {
        return raw.substring(0, 4) + "-" + raw.substring(4, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16);
    }

    public record Ticket(UUID accountId, UUID playerUuid, Instant expiresAt) {
    }
}
