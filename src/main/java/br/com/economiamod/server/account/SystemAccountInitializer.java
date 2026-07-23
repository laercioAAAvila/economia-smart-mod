package br.com.economiamod.server.account;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.account.AccountType;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class SystemAccountInitializer {
    public void initialize() throws SQLException {
        createSystemAccount(AccountType.SYSTEM_TREASURY, "Tesouraria do Sistema");
        createSystemAccount(AccountType.SYSTEM_CASH, "Caixa do Sistema");
        createSystemAccount(AccountType.SYSTEM_CURRENCY_ISSUANCE, "Emissao Monetaria do Sistema");
    }

    private void createSystemAccount(AccountType accountType, String username) throws SQLException {
        String sql = """
                INSERT INTO economy_accounts(
                    id,
                    player_uuid,
                    username,
                    username_normalized,
                    password_hash,
                    password_salt,
                    password_algorithm,
                    account_type,
                    status,
                    balance,
                    configured_credit_limit,
                    credit_principal_outstanding,
                    credit_interest_outstanding,
                    created_at,
                    updated_at,
                    last_login_at,
                    version
                )
                VALUES (?, NULL, ?, NULL, NULL, NULL, NULL, ?, ?, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 1)
                ON CONFLICT (id) DO NOTHING
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, SystemAccountIds.id(accountType));
            statement.setString(2, username);
            statement.setString(3, accountType.name());
            statement.setString(4, AccountStatus.ACTIVE.name());
            statement.executeUpdate();
        }
    }
}
