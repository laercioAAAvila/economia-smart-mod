package br.com.economiamod.server.invoice;

import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InvoiceQueryService {
    public Optional<InvoiceSummary> accountInvoice(UUID accountId) throws SQLException {
        String accountSql = """
                SELECT credit_principal_outstanding,
                       credit_interest_outstanding
                  FROM economy_accounts
                 WHERE id = ?
                   AND account_type = 'PLAYER'
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement accountStatement = connection.prepareStatement(accountSql)) {
            accountStatement.setObject(1, accountId);

            try (ResultSet accountResult = accountStatement.executeQuery()) {
                if (!accountResult.next()) {
                    return Optional.empty();
                }

                long principal = accountResult.getLong("credit_principal_outstanding");
                long interest = accountResult.getLong("credit_interest_outstanding");
                return Optional.of(new InvoiceSummary(
                        principal,
                        interest,
                        principal + interest,
                        openEntries(connection, accountId)
                ));
            }
        }
    }

    private List<InvoiceOpenEntry> openEntries(Connection connection, UUID accountId) throws SQLException {
        String sql = """
                SELECT e.id,
                       e.card_id,
                       e.entry_type,
                       e.remaining_amount,
                       e.description,
                       e.merchant_name,
                       e.business_date,
                       e.created_at
                  FROM economy_card_entries e
                  JOIN economy_cards c ON c.id = e.card_id
                 WHERE c.account_id = ?
                   AND e.remaining_amount > 0
                 ORDER BY CASE WHEN e.entry_type = 'DAILY_INTEREST' THEN 0 ELSE 1 END,
                          e.created_at,
                          e.id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, accountId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<InvoiceOpenEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    entries.add(new InvoiceOpenEntry(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getObject("card_id", UUID.class),
                            resultSet.getString("entry_type"),
                            resultSet.getLong("remaining_amount"),
                            resultSet.getString("description"),
                            resultSet.getString("merchant_name"),
                            resultSet.getObject("business_date", java.time.LocalDate.class),
                            resultSet.getObject("created_at", java.time.LocalDateTime.class)
                    ));
                }
                return List.copyOf(entries);
            }
        }
    }
}

