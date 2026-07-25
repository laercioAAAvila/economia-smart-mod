package br.com.economiamod.server.account;

import br.com.economiamod.common.network.AtmOperationHistoryPayload;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AccountOperationHistoryService {
    private static final int RETENTION_BUSINESS_DAYS = 5;
    private static final int LIMIT = 16;
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    public List<AtmOperationHistoryPayload.Entry> recentEntries(UUID accountId) throws SQLException {
        String sql = """
                SELECT t.idempotency_key, t.transaction_type, l.entry_type, l.amount, l.created_at
                  FROM economy_ledger_entries l
                  JOIN economy_transactions t ON t.id = l.transaction_id
                 WHERE l.account_id = ?
                   AND t.status = 'COMPLETED'
                   AND l.entry_type IN ('DEBIT', 'CREDIT')
                   AND l.created_at >= ?
                   AND (
                        t.transaction_type IN ('WITHDRAW', 'TRANSFER')
                        OR t.idempotency_key LIKE 'shop-cash-change:%'
                        OR t.idempotency_key LIKE 'mail-cash-change:%'
                   )
                 ORDER BY l.created_at DESC
                 LIMIT ?
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            statement.setTimestamp(2, Timestamp.valueOf(cutoff()));
            statement.setInt(3, LIMIT);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AtmOperationHistoryPayload.Entry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    String operationKey = operationKey(resultSet.getString("idempotency_key"), resultSet.getString("transaction_type"));
                    if (operationKey.isBlank()) {
                        continue;
                    }
                    entries.add(new AtmOperationHistoryPayload.Entry(
                            operationKey,
                            directionKey(resultSet.getString("entry_type")),
                            resultSet.getLong("amount"),
                            occurredAt(resultSet.getTimestamp("created_at"))
                    ));
                }
                return entries;
            }
        }
    }

    private LocalDateTime cutoff() {
        LocalDateTime current = LocalDateTime.now(ZoneId.of(EconomyServerConfig.ECONOMY_TIME_ZONE.get()));
        int remaining = RETENTION_BUSINESS_DAYS;
        while (remaining > 0) {
            current = current.minusDays(1);
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                remaining--;
            }
        }
        return current;
    }

    private String operationKey(String idempotencyKey, String transactionType) {
        String key = idempotencyKey == null ? "" : idempotencyKey;
        if (key.startsWith("shop-cash-change:")) {
            return "screen.economia.atm.history.shop_change";
        }
        if (key.startsWith("mail-cash-change:")) {
            return "screen.economia.atm.history.mail_change";
        }
        if ("WITHDRAW".equals(transactionType)) {
            return "screen.economia.atm.history.withdraw";
        }
        if ("TRANSFER".equals(transactionType)) {
            return "screen.economia.atm.history.transfer";
        }
        return "";
    }

    private String directionKey(String entryType) {
        return "CREDIT".equals(entryType)
                ? "screen.economia.atm.history.in"
                : "screen.economia.atm.history.out";
    }

    private String occurredAt(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return timestamp.toLocalDateTime().format(DISPLAY_TIME);
    }
}
