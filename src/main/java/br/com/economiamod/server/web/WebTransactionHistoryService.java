package br.com.economiamod.server.web;

import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class WebTransactionHistoryService {
    List<Entry> recent(UUID accountId, int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String sql = """
                SELECT t.id,
                       t.transaction_type,
                       t.status,
                       t.amount,
                       t.origin,
                       l.entry_type,
                       l.balance_before,
                       l.balance_after,
                       source_account.account_number AS source_account_number,
                       destination_account.account_number AS destination_account_number,
                       t.created_at
                  FROM economy_ledger_entries l
                  JOIN economy_transactions t ON t.id = l.transaction_id
                  LEFT JOIN economy_accounts source_account ON source_account.id = t.source_account_id
                  LEFT JOIN economy_accounts destination_account ON destination_account.id = t.destination_account_id
                 WHERE l.account_id = ?
                 ORDER BY t.created_at DESC, t.id DESC
                 LIMIT ?
                """;
        List<Entry> entries = new ArrayList<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);
            statement.setInt(2, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String direction = resultSet.getString("entry_type");
                    String counterparty = "CREDIT".equals(direction)
                            ? resultSet.getString("source_account_number")
                            : resultSet.getString("destination_account_number");
                    entries.add(new Entry(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getString("transaction_type"),
                            resultSet.getString("status"),
                            resultSet.getLong("amount"),
                            resultSet.getString("origin"),
                            direction,
                            resultSet.getLong("balance_before"),
                            resultSet.getLong("balance_after"),
                            counterparty == null ? "" : counterparty,
                            resultSet.getString("created_at")
                    ));
                }
            }
        }
        return entries;
    }

    record Entry(UUID transactionId, String type, String status, long amount, String origin,
                 String direction, long balanceBefore, long balanceAfter,
                 String counterpartyAccount, String createdAt) {
    }
}
