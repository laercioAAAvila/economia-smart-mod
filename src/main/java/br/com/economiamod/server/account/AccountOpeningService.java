package br.com.economiamod.server.account;

import br.com.economiamod.common.claim.DirectPaymentMethod;
import br.com.economiamod.server.security.PasswordService;
import br.com.economiamod.server.transaction.MenuPaymentResult;
import br.com.economiamod.server.transaction.MenuPaymentService;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;

public final class AccountOpeningService {
    private final AccountService accounts = new AccountService(new PasswordService());
    private final MenuPaymentService payments = new MenuPaymentService();

    public AccountOpeningResult open(ServerPlayer player, String username, char[] password,
                                     Container cash, UUID requestId) throws SQLException {
        CreateAccountResult prepared = accounts.createPlayerAccount(player.getUUID(),
                player.getGameProfile().getName(), username, password, requestId);
        if (prepared.type() == CreateAccountResultType.PLAYER_ALREADY_HAS_ACCOUNT) {
            return AccountOpeningResult.denied("account_limit");
        }
        if (prepared.type() == CreateAccountResultType.USERNAME_ALREADY_USED) {
            return AccountOpeningResult.denied("username_exists");
        }
        if (prepared.alreadyActive()) {
            return AccountOpeningResult.created(prepared.accountId());
        }

        if (prepared.openingFee() > 0L) {
            MenuPaymentResult payment = payments.pay(player, DirectPaymentMethod.CASH,
                    net.minecraft.world.item.ItemStack.EMPTY, cash, prepared.openingFee(),
                    "Abertura de conta", "account-opening:" + prepared.accountId());
            if (!payment.success()) {
                accounts.deletePendingAccount(prepared.accountId());
                return AccountOpeningResult.denied("payment_" + payment.code());
            }
        }
        accounts.activatePendingAccount(prepared.accountId());
        return AccountOpeningResult.created(prepared.accountId());
    }
}
