package br.com.economiamod.server.network;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.card.CardItemDataService;
import br.com.economiamod.common.card.CardType;
import br.com.economiamod.common.invoice.InvoiceItemDataService;
import br.com.economiamod.common.menu.AtmMenu;
import br.com.economiamod.common.network.AtmAccountSummaryPayload;
import br.com.economiamod.common.network.AtmCardsPayload;
import br.com.economiamod.common.network.AtmOperationHistoryPayload;
import br.com.economiamod.common.network.AtmSessionStatePayload;
import br.com.economiamod.common.network.SecureAccountPayload;
import br.com.economiamod.server.account.AccountCreditLimitResultType;
import br.com.economiamod.server.account.AccountBalanceSummary;
import br.com.economiamod.server.account.AccountIdentity;
import br.com.economiamod.server.account.AccountOperationHistoryService;
import br.com.economiamod.server.account.AccountPasswordVerificationResultType;
import br.com.economiamod.server.account.AccountQueryService;
import br.com.economiamod.server.account.AccountService;
import br.com.economiamod.server.account.AuthenticateAccountResult;
import br.com.economiamod.server.account.ChangePasswordResult;
import br.com.economiamod.server.account.CreateAccountResult;
import br.com.economiamod.server.card.CardCreditLimitResultType;
import br.com.economiamod.server.card.CardCreditLimitService;
import br.com.economiamod.server.card.CardDebitDailyLimitResultType;
import br.com.economiamod.server.card.CardDebitDailyLimitService;
import br.com.economiamod.server.card.CardIssueRequest;
import br.com.economiamod.server.card.CardIssueResult;
import br.com.economiamod.server.card.CardIssueResultType;
import br.com.economiamod.server.card.CardIssueService;
import br.com.economiamod.server.card.CardManagementService;
import br.com.economiamod.server.card.CardSecurityResultType;
import br.com.economiamod.server.card.CardSecurityService;
import br.com.economiamod.server.card.CardValidationResult;
import br.com.economiamod.server.card.CardValidationResultType;
import br.com.economiamod.server.card.CardValidationService;
import br.com.economiamod.server.cash.CashAccountOperationResult;
import br.com.economiamod.server.cash.CashAccountOperationResultType;
import br.com.economiamod.server.cash.CashAccountOperationService;
import br.com.economiamod.server.cash.CashInventoryService;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.invoice.InvoicePaymentResult;
import br.com.economiamod.server.invoice.InvoicePaymentResultType;
import br.com.economiamod.server.invoice.InvoicePaymentService;
import br.com.economiamod.server.invoice.InvoiceQueryService;
import br.com.economiamod.server.operation.EconomyOperationService;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import br.com.economiamod.server.security.PasswordService;
import br.com.economiamod.server.session.BankSession;
import br.com.economiamod.server.session.BankSessionService;
import br.com.economiamod.server.transaction.AccountFinancialService;
import br.com.economiamod.server.transaction.FinancialOperationResult;
import br.com.economiamod.server.transaction.FinancialOperationResultType;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SecureAccountPayloadHandler {
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_PASSWORD_LENGTH = 12;
    private static final long CREDIT_REQUEST_COOLDOWN_MILLIS = 10_000L;
    private static final AccountService ACCOUNT_SERVICE = new AccountService(new PasswordService());
    private static final AccountQueryService ACCOUNT_QUERY_SERVICE = new AccountQueryService();
    private static final CardValidationService CARD_VALIDATION_SERVICE = new CardValidationService(new CardItemDataService());
    private static final CardCreditLimitService CARD_CREDIT_LIMIT_SERVICE = new CardCreditLimitService();
    private static final CardDebitDailyLimitService CARD_DEBIT_DAILY_LIMIT_SERVICE = new CardDebitDailyLimitService(new CardItemDataService());
    private static final CardIssueService CARD_ISSUE_SERVICE = new CardIssueService();
    private static final CardSecurityService CARD_SECURITY_SERVICE = new CardSecurityService();
    private static final CardManagementService CARD_MANAGEMENT_SERVICE = new CardManagementService();
    private static final AccountOperationHistoryService ACCOUNT_HISTORY_SERVICE = new AccountOperationHistoryService();
    private static final InvoiceItemDataService INVOICE_ITEM_DATA_SERVICE = new InvoiceItemDataService();
    private static final AccountFinancialService ACCOUNT_FINANCIAL_SERVICE = new AccountFinancialService();
    private static final InvoicePaymentService INVOICE_PAYMENT_SERVICE = new InvoicePaymentService();
    private static final InvoiceQueryService INVOICE_QUERY_SERVICE = new InvoiceQueryService();
    private static final CashAccountOperationService CASH_OPERATIONS = new CashAccountOperationService(
            new CashInventoryService(),
            new AccountFinancialService(),
            new EconomyOperationService()
    );
    private static final Map<UUID, Long> CREDIT_REQUEST_COOLDOWNS = new ConcurrentHashMap<>();

    private SecureAccountPayloadHandler() {
    }

    public static void handle(SecureAccountPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!EconomyDatabaseState.isAvailable()) {
            serverPlayer.sendSystemMessage(Component.translatable("commands.economia.unavailable"));
            return;
        }

        try {
            switch (payload.action()) {
                case LOGIN -> login(serverPlayer, payload);
                case CREATE_ACCOUNT -> createAccount(serverPlayer, payload);
                case CHANGE_PASSWORD -> changePassword(serverPlayer, payload);
                case CARD_LOGIN -> cardLogin(serverPlayer);
                case RECOVER_PASSWORD -> recoverPassword(serverPlayer, payload);
                case SESSION_STATE -> syncSession(serverPlayer);
                case ACCOUNT_SUMMARY -> syncAccountSummary(serverPlayer);
                case UPDATE_CARD_CREDIT -> updateCardCredit(serverPlayer, payload);
                case TRANSFER -> transfer(serverPlayer, payload);
                case GOLD_PRICE_REFRESH -> refreshGoldPrices(serverPlayer);
                case WITHDRAW -> withdraw(serverPlayer, payload);
                case ISSUE_CARD -> issueCard(serverPlayer, payload);
                case UPDATE_ACCOUNT_CREDIT -> updateAccountCredit(serverPlayer, payload);
                case UNBLOCK_CARD -> unblockCard(serverPlayer);
                case REQUEST_ACCOUNT_CREDIT -> requestAccountCredit(serverPlayer);
                case LOGOUT -> logout(serverPlayer);
                case SET_CARD_SLOT_MODE -> setCardSlotMode(serverPlayer, payload);
                case UPDATE_DEBIT_DAILY_LIMIT -> updateDebitDailyLimit(serverPlayer, payload);
                case PAY_INVOICE -> payInvoice(serverPlayer, payload);
                case ISSUE_INVOICE -> issueInvoice(serverPlayer);
                case PAY_ALL_INVOICES -> payAllInvoices(serverPlayer, payload);
                case REFRESH_CARDS -> syncCards(serverPlayer);
                case BLOCK_CARD_BY_ID -> blockCardById(serverPlayer, payload);
                case DISABLE_CARD_BY_ID -> disableCardById(serverPlayer, payload);
                case DEPOSIT -> deposit(serverPlayer, payload);
                case OPERATION_HISTORY -> syncOperationHistory(serverPlayer);
            }
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao processar acao segura de conta bancaria.", exception);
            serverPlayer.sendSystemMessage(Component.translatable("commands.economia.account.action.failed"));
        }
    }

    private static void login(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        if (!validUsernameAndPassword(payload.username(), payload.password())) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.invalid_input"));
            return;
        }

        char[] password = payload.password().toCharArray();
        try {
            AuthenticateAccountResult result = ACCOUNT_SERVICE.authenticate(payload.username(), password);
            switch (result.type()) {
                case AUTHENTICATED -> {
                    BankSessionService.INSTANCE.startSession(player, result.accountId(), result.username(), result.accountNumber(), true);
                    player.sendSystemMessage(Component.translatable("commands.economia.account.login.success", result.username()));
                    syncSession(player, true, result.username(), result.accountNumber(), true);
                    syncAccountSummary(player, result.accountId());
                }
                case INACTIVE_ACCOUNT -> player.sendSystemMessage(Component.translatable("commands.economia.account.login.inactive"));
                case NOT_FOUND, INVALID_PASSWORD -> player.sendSystemMessage(Component.translatable("commands.economia.account.login.invalid"));
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static void cardLogin(ServerPlayer player) throws SQLException {
        if (!(player.containerMenu instanceof AtmMenu atmMenu)) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card_login.open_atm"));
            return;
        }

        CardValidationResult card = CARD_VALIDATION_SERVICE.validate(atmMenu.cardStack());
        if (card.type() != CardValidationResultType.VALID) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card_login.invalid"));
            return;
        }

        AccountIdentity identity = ACCOUNT_QUERY_SERVICE.findActiveIdentity(card.accountId()).orElse(new AccountIdentity("", ""));
        BankSessionService.INSTANCE.startSession(player, card.accountId(), identity.username(), identity.accountNumber(), false, card.cardId());
        player.sendSystemMessage(Component.translatable("commands.economia.account.login.success_card", identity.accountNumber()));
        syncSession(player, true, identity.username(), identity.accountNumber(), false);
        syncAccountSummary(player, card.accountId());
    }

    private static void createAccount(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        if (!validPassword(payload.password())) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.password.length"));
            return;
        }
        if (!validUsernameAndPassword(payload.username(), payload.password())) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.invalid_input"));
            return;
        }

        char[] password = payload.password().toCharArray();
        try {
            CreateAccountResult result = ACCOUNT_SERVICE.createPlayerAccount(player.getUUID(), payload.username(), password);
            switch (result.type()) {
                case CREATED -> {
                    player.sendSystemMessage(Component.translatable("commands.economia.account.create.success"));
                    login(player, payload);
                }
                case PLAYER_ALREADY_HAS_ACCOUNT -> player.sendSystemMessage(Component.translatable("commands.economia.account.create.player_exists"));
                case USERNAME_ALREADY_USED -> player.sendSystemMessage(Component.translatable("commands.economia.account.create.username_exists"));
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static void changePassword(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        if (!validPassword(payload.password()) || !validPassword(payload.newPassword())) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.password.length"));
            return;
        }

        BankSession session = BankSessionService.INSTANCE.findActiveSession(player).orElse(null);
        if (session == null) {
            player.sendSystemMessage(Component.translatable("commands.economia.session.required"));
            return;
        }

        char[] currentPassword = payload.password().toCharArray();
        char[] newPassword = payload.newPassword().toCharArray();
        try {
            ChangePasswordResult result = ACCOUNT_SERVICE.changePassword(session.accountId(), currentPassword, newPassword);
            switch (result.type()) {
                case CHANGED -> {
                    BankSessionService.INSTANCE.logout(player);
                    if (player.containerMenu instanceof AtmMenu atmMenu) {
                        atmMenu.returnCardToPlayer(player);
                    }
                    syncSession(player, false, "", "", false);
                    PacketDistributor.sendToPlayer(player, new AtmAccountSummaryPayload(false, 0L, 0L, 0L, 0L, 0L));
                    player.sendSystemMessage(Component.translatable("commands.economia.account.password.changed"));
                }
                case INACTIVE_ACCOUNT -> player.sendSystemMessage(Component.translatable("commands.economia.account.login.inactive"));
                case NOT_FOUND -> {
                    BankSessionService.INSTANCE.logout(player);
                    player.sendSystemMessage(Component.translatable("commands.economia.session.invalid"));
                }
                case INVALID_PASSWORD -> player.sendSystemMessage(Component.translatable("commands.economia.account.password.invalid"));
            }
        } finally {
            Arrays.fill(currentPassword, '\0');
            Arrays.fill(newPassword, '\0');
        }
    }

    private static void recoverPassword(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        if (payload.username() == null || payload.username().trim().isEmpty() || !validPassword(payload.newPassword())) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.password.length"));
            return;
        }

        char[] newPassword = payload.newPassword().toCharArray();
        try {
            ChangePasswordResult result = ACCOUNT_SERVICE.recoverPassword(player.getUUID(), payload.username(), newPassword);
            switch (result.type()) {
                case CHANGED -> player.sendSystemMessage(Component.translatable("commands.economia.account.password.recovered"));
                case INACTIVE_ACCOUNT -> player.sendSystemMessage(Component.translatable("commands.economia.account.login.inactive"));
                case NOT_FOUND -> player.sendSystemMessage(Component.translatable("commands.economia.account.recover.not_found"));
                case INVALID_PASSWORD -> player.sendSystemMessage(Component.translatable("commands.economia.account.password.invalid"));
            }
        } finally {
            Arrays.fill(newPassword, '\0');
        }
    }

    private static void updateCardCredit(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        BankSession session = BankSessionService.INSTANCE.findActiveSession(player).orElse(null);
        if (session == null) {
            player.sendSystemMessage(Component.translatable("commands.economia.session.required"));
            return;
        }
        if (!(player.containerMenu instanceof AtmMenu atmMenu)) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card_credit.open_atm"));
            return;
        }

        long limit;
        try {
            limit = Long.parseLong(payload.newPassword());
        } catch (NumberFormatException exception) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.invalid_input"));
            return;
        }

        CardCreditLimitResultType result = CARD_CREDIT_LIMIT_SERVICE.updateLimit(session.accountId(), atmMenu.cardStack(), limit);
        if (result == CardCreditLimitResultType.UPDATED) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card_credit.updated", limit));
            syncAccountSummary(player, session.accountId());
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.card_credit." + result.name().toLowerCase()));
    }

    private static void updateAccountCredit(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null || !verifySensitiveActionPassword(player, session, payload.password())) {
            return;
        }

        long limit;
        try {
            limit = Long.parseLong(payload.username());
        } catch (NumberFormatException exception) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.invalid_input"));
            return;
        }

        AccountCreditLimitResultType result = ACCOUNT_SERVICE.updateConfiguredCreditLimit(session.accountId(), limit);
        if (result == AccountCreditLimitResultType.UPDATED) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.account_credit.updated", limit));
            syncAccountSummary(player, session.accountId());
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.account_credit." + result.name().toLowerCase()));
    }

    private static void requestAccountCredit(ServerPlayer player) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null) {
            return;
        }
        if (creditRequestOnCooldown(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.account_credit.cooldown"));
            return;
        }

        AccountCreditLimitResultType result = ACCOUNT_SERVICE.requestCreditLimitByBalance(session.accountId());
        if (result == AccountCreditLimitResultType.UPDATED) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.account_credit.requested"));
            syncAccountSummary(player, session.accountId());
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.account_credit." + result.name().toLowerCase()));
    }

    private static void updateDebitDailyLimit(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null) {
            return;
        }
        if (!(player.containerMenu instanceof AtmMenu atmMenu)) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card_login.open_atm"));
            return;
        }
        long limit;
        try {
            limit = Long.parseLong(payload.username());
        } catch (NumberFormatException exception) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.invalid_input"));
            return;
        }
        CardDebitDailyLimitResultType result = CARD_DEBIT_DAILY_LIMIT_SERVICE.updateLimit(session.accountId(), atmMenu.cardStack(), limit);
        if (result == CardDebitDailyLimitResultType.UPDATED) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.debit_daily_limit.updated", limit));
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.debit_daily_limit." + result.name().toLowerCase()));
    }

    private static void payInvoice(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null || !verifySensitiveActionPassword(player, session, payload.password())) {
            return;
        }
        if (!invoicePaymentWindowOpen()) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice.unavailable"));
            return;
        }
        if (!(player.containerMenu instanceof AtmMenu atmMenu)) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card_login.open_atm"));
            return;
        }
        var invoice = INVOICE_ITEM_DATA_SERVICE.read(atmMenu.invoiceStack()).orElse(null);
        if (invoice == null || !session.accountId().equals(invoice.accountId())) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice.missing"));
            return;
        }
        var summary = INVOICE_QUERY_SERVICE.accountInvoice(session.accountId()).orElse(null);
        if (summary == null || summary.openEntries().isEmpty()) {
            atmMenu.clearInvoiceSlot();
            player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice.no_debt"));
            return;
        }
        if (!summary.openEntries().get(0).entryId().equals(invoice.entryId())) {
            atmMenu.clearInvoiceSlot();
            atmMenu.refreshInvoiceSlot(session.accountId());
            player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice.reissued"));
            return;
        }
        InvoicePaymentResult result = INVOICE_PAYMENT_SERVICE.payAccount(
                player.getUUID(),
                session.accountId(),
                invoice.amount(),
                "atm:invoice:" + player.getUUID() + ":" + stableRequestId(payload.requestId())
        );
        if (result.type() == InvoicePaymentResultType.COMPLETED || result.type() == InvoicePaymentResultType.DUPLICATE_COMPLETED) {
            atmMenu.clearInvoiceSlot();
            atmMenu.refreshInvoiceSlot(session.accountId());
            player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice.paid", result.paidAmount(), result.balanceAfter()));
            syncAccountSummary(player, session.accountId());
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice." + result.type().name().toLowerCase()));
    }

    private static void issueInvoice(ServerPlayer player) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null) {
            return;
        }
        if (!(player.containerMenu instanceof AtmMenu atmMenu)) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card_login.open_atm"));
            return;
        }
        if (!invoicePaymentWindowOpen()) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice.unavailable"));
            return;
        }
        var summary = INVOICE_QUERY_SERVICE.accountInvoice(session.accountId()).orElse(null);
        if (summary == null || summary.openEntries().isEmpty()) {
            atmMenu.clearInvoiceSlot();
            player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice.no_debt"));
            return;
        }
        atmMenu.clearInvoiceSlot();
        atmMenu.refreshInvoiceSlot(session.accountId());
        player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice.reissued"));
    }

    private static void payAllInvoices(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null || !verifySensitiveActionPassword(player, session, payload.password())) {
            return;
        }
        if (!invoicePaymentWindowOpen()) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice.unavailable"));
            return;
        }
        InvoicePaymentResult result = INVOICE_PAYMENT_SERVICE.payAccount(
                player.getUUID(),
                session.accountId(),
                Long.MAX_VALUE,
                "atm:invoice-all:" + player.getUUID() + ":" + stableRequestId(payload.requestId())
        );
        if (player.containerMenu instanceof AtmMenu atmMenu) {
            atmMenu.clearInvoiceSlot();
            atmMenu.refreshInvoiceSlot(session.accountId());
        }
        if (result.type() == InvoicePaymentResultType.COMPLETED || result.type() == InvoicePaymentResultType.DUPLICATE_COMPLETED) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice.paid", result.paidAmount(), result.balanceAfter()));
            syncAccountSummary(player, session.accountId());
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.invoice." + result.type().name().toLowerCase()));
    }

    private static void logout(ServerPlayer player) {
        boolean loggedOut = BankSessionService.INSTANCE.logout(player);
        if (player.containerMenu instanceof AtmMenu atmMenu) {
            atmMenu.returnCardToPlayer(player);
        }
        syncSession(player, false, "", "", false);
        PacketDistributor.sendToPlayer(player, new AtmAccountSummaryPayload(false, 0L, 0L, 0L, 0L, 0L));
        PacketDistributor.sendToPlayer(player, new AtmOperationHistoryPayload(List.of()));
        player.sendSystemMessage(Component.translatable(loggedOut ? "commands.economia.logout.success" : "commands.economia.logout.no_session"));
    }

    private static void setCardSlotMode(ServerPlayer player, SecureAccountPayload payload) {
        if (!(player.containerMenu instanceof AtmMenu atmMenu)) {
            return;
        }
        BankSessionService.INSTANCE.findActiveSession(player)
                .ifPresentOrElse(
                        session -> atmMenu.setSessionAccountNumber(session.accountNumber()),
                        () -> atmMenu.setSessionAccountNumber("")
                );
        try {
            atmMenu.setCardSlotMode(AtmMenu.CardSlotMode.valueOf(payload.username()));
        } catch (IllegalArgumentException exception) {
            atmMenu.setCardSlotMode(AtmMenu.CardSlotMode.LOGIN);
        }
        BankSessionService.INSTANCE.findActiveSession(player)
                .ifPresent(session -> atmMenu.refreshInvoiceSlot(session.accountId()));
    }

    private static void blockCardById(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        updateListedCard(player, payload, true);
    }

    private static void disableCardById(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        updateListedCard(player, payload, false);
    }

    private static void updateListedCard(ServerPlayer player, SecureAccountPayload payload, boolean block) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null) {
            return;
        }
        UUID cardId;
        try {
            cardId = UUID.fromString(payload.username());
        } catch (IllegalArgumentException exception) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card_list.invalid_card"));
            return;
        }
        CardSecurityResultType result = block
                ? CARD_MANAGEMENT_SERVICE.blockCard(session.accountId(), cardId)
                : CARD_MANAGEMENT_SERVICE.disableCard(session.accountId(), cardId);
        if (result == CardSecurityResultType.UPDATED) {
            player.sendSystemMessage(Component.translatable(block ? "commands.economia.atm.card_list.blocked" : "commands.economia.atm.card_list.disabled"));
            syncCards(player, session.accountId());
            syncAccountSummary(player, session.accountId());
            return;
        }
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card_list." + result.name().toLowerCase()));
    }

    private static void withdraw(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null || !verifySensitiveActionPassword(player, session, payload.password())) {
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(payload.username());
        } catch (NumberFormatException exception) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.invalid_input"));
            return;
        }
        if (amount <= 0L) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.invalid_input"));
            return;
        }

        Long banknoteValue = null;
        if (!payload.newPassword().isBlank()) {
            try {
                banknoteValue = Long.parseLong(payload.newPassword());
            } catch (NumberFormatException exception) {
                player.sendSystemMessage(Component.translatable("commands.economia.atm.operation.invalid_denomination"));
                return;
            }
        }

        CashAccountOperationResult result = CASH_OPERATIONS.withdraw(player, session, amount, banknoteValue, "atm:withdraw:" + player.getUUID() + ":" + stableRequestId(payload.requestId()));
        if (result.type() == CashAccountOperationResultType.COMPLETED) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.withdraw.success", result.amount(), result.balanceAfter()));
            syncAccountSummary(player, session.accountId());
            syncOperationHistory(player, session.accountId());
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.operation." + result.type().name().toLowerCase()));
    }

    private static void deposit(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null) {
            return;
        }

        CashAccountOperationResult result = CASH_OPERATIONS.depositAll(player, session, "atm:deposit:" + player.getUUID() + ":" + stableRequestId(payload.requestId()));
        if (result.type() == CashAccountOperationResultType.COMPLETED) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.deposit.success", result.amount(), result.balanceAfter()));
            syncAccountSummary(player, session.accountId());
            syncOperationHistory(player, session.accountId());
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.operation." + result.type().name().toLowerCase()));
    }

    private static void issueCard(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null) {
            return;
        }

        CardType cardType;
        try {
            cardType = CardType.valueOf(payload.username());
        } catch (IllegalArgumentException exception) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.invalid_input"));
            return;
        }

        CardIssueResult result = CARD_ISSUE_SERVICE.issue(new CardIssueRequest(session.accountId(), cardType, player.getGameProfile().getName(), 0L));
        if (result.type() == CardIssueResultType.ISSUED) {
            var stack = new CardItemDataService().createCardStack(
                    result.cardType(),
                    result.cardId(),
                    result.securityVersion(),
                    result.accountNumber(),
                    result.cardName(),
                    result.individualCreditLimit()
            );
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card.success"));
            syncAccountSummary(player, session.accountId());
            syncCards(player, session.accountId());
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.card." + result.type().name().toLowerCase()));
    }

    private static void unblockCard(ServerPlayer player) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null) {
            return;
        }
        if (!(player.containerMenu instanceof AtmMenu atmMenu)) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.card_login.open_atm"));
            return;
        }

        CardSecurityResultType result = CARD_SECURITY_SERVICE.unblockCard(session.accountId(), atmMenu.cardStack());
        if (result == CardSecurityResultType.UPDATED || result == CardSecurityResultType.ALREADY_ACTIVE) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.security.card_unblocked"));
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.security." + result.name().toLowerCase()));
    }

    private static void transfer(ServerPlayer player, SecureAccountPayload payload) throws SQLException {
        BankSession session = requireSession(player);
        if (session == null || !verifySensitiveActionPassword(player, session, payload.newPassword())) {
            return;
        }

        String accountNumber = payload.username() == null ? "" : payload.username().trim();
        if (!accountNumber.matches("[0-9]{6}")) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.transfer.invalid_account"));
            return;
        }
        if (accountNumber.equals(session.accountNumber())) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.transfer.same_account"));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(payload.password());
        } catch (NumberFormatException exception) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.invalid_input"));
            return;
        }
        if (amount <= 0L) {
            player.sendSystemMessage(Component.translatable("commands.economia.account.invalid_input"));
            return;
        }

        UUID destinationAccountId = ACCOUNT_QUERY_SERVICE.findActiveAccountIdByNumber(accountNumber).orElse(null);
        if (destinationAccountId == null) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.transfer.account_not_found"));
            return;
        }
        if (session.accountId().equals(destinationAccountId)) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.transfer.same_account"));
            return;
        }

        FinancialOperationResult result = ACCOUNT_FINANCIAL_SERVICE.transfer(
                player.getUUID(),
                session.accountId(),
                destinationAccountId,
                amount,
                null,
                "atm:transfer:" + player.getUUID() + ":" + stableRequestId(payload.requestId())
        );
        if (result.type() == FinancialOperationResultType.COMPLETED || result.type() == FinancialOperationResultType.DUPLICATE_COMPLETED) {
            player.sendSystemMessage(Component.translatable("commands.economia.atm.transfer.success", amount, accountNumber));
            syncAccountSummary(player, session.accountId());
            syncOperationHistory(player, session.accountId());
            return;
        }
        player.sendSystemMessage(Component.translatable("commands.economia.atm.transfer." + result.type().name().toLowerCase()));
    }

    private static void refreshGoldPrices(ServerPlayer player) {
        if (player.containerMenu instanceof AtmMenu atmMenu) {
            atmMenu.refreshGoldPricing(player);
        }
    }

    private static BankSession requireSession(ServerPlayer player) {
        BankSession session = BankSessionService.INSTANCE.findActiveSession(player).orElse(null);
        if (session == null) {
            player.sendSystemMessage(Component.translatable("commands.economia.session.required"));
        }
        return session;
    }

    private static boolean verifySensitiveActionPassword(ServerPlayer player, BankSession session, String passwordText) throws SQLException {
        char[] password = passwordText == null ? new char[0] : passwordText.toCharArray();
        try {
            AccountPasswordVerificationResultType result = ACCOUNT_SERVICE.verifyPassword(session.accountId(), password);
            if (result == AccountPasswordVerificationResultType.VALID) {
                return true;
            }
            if (result == AccountPasswordVerificationResultType.INVALID_PASSWORD) {
                handleInvalidSensitivePassword(player, session);
            } else {
                player.sendSystemMessage(Component.translatable("commands.economia.account.password." + result.name().toLowerCase()));
            }
            return false;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static void handleInvalidSensitivePassword(ServerPlayer player, BankSession session) throws SQLException {
        player.sendSystemMessage(Component.translatable("commands.economia.account.password.invalid"));
        if (session.showUsername()) {
            return;
        }
        if (session.loginCardId() != null) {
            CARD_SECURITY_SERVICE.blockCard(session.accountId(), session.loginCardId());
        }
        BankSessionService.INSTANCE.logout(player);
        if (player.containerMenu instanceof AtmMenu atmMenu) {
            atmMenu.returnCardToPlayer(player);
        }
        syncSession(player, false, "", "", false);
        PacketDistributor.sendToPlayer(player, new AtmAccountSummaryPayload(false, 0L, 0L, 0L, 0L, 0L));
        PacketDistributor.sendToPlayer(player, new AtmOperationHistoryPayload(List.of()));
        player.sendSystemMessage(Component.translatable("commands.economia.atm.security.card_blocked"));
    }

    private static void syncSession(ServerPlayer player, boolean loggedIn, String username, String accountNumber, boolean showUsername) {
        PacketDistributor.sendToPlayer(player, new AtmSessionStatePayload(
                loggedIn,
                username == null ? "" : username,
                accountNumber == null ? "" : accountNumber,
                showUsername
        ));
    }

    private static void syncSession(ServerPlayer player) {
        BankSessionService.INSTANCE.findActiveSession(player)
                .ifPresentOrElse(
                        session -> {
                            syncSession(player, true, session.username(), session.accountNumber(), session.showUsername());
                            safeSyncAccountSummary(player, session.accountId());
                            safeSyncCards(player, session.accountId());
                            safeSyncOperationHistory(player, session.accountId());
                        },
                        () -> {
                            syncSession(player, false, "", "", false);
                            PacketDistributor.sendToPlayer(player, new AtmAccountSummaryPayload(false, 0L, 0L, 0L, 0L, 0L));
                            PacketDistributor.sendToPlayer(player, new AtmCardsPayload(List.of()));
                            PacketDistributor.sendToPlayer(player, new AtmOperationHistoryPayload(List.of()));
                        }
                );
    }

    private static void syncAccountSummary(ServerPlayer player) throws SQLException {
        BankSession session = BankSessionService.INSTANCE.findActiveSession(player).orElse(null);
        if (session == null) {
            PacketDistributor.sendToPlayer(player, new AtmAccountSummaryPayload(false, 0L, 0L, 0L, 0L, 0L));
            PacketDistributor.sendToPlayer(player, new AtmOperationHistoryPayload(List.of()));
            return;
        }
        syncAccountSummary(player, session.accountId());
    }

    private static void syncAccountSummary(ServerPlayer player, java.util.UUID accountId) throws SQLException {
        AccountBalanceSummary summary = ACCOUNT_QUERY_SERVICE.findBalanceSummary(accountId).orElse(null);
        if (summary == null) {
            PacketDistributor.sendToPlayer(player, new AtmAccountSummaryPayload(false, 0L, 0L, 0L, 0L, 0L));
            return;
        }
        PacketDistributor.sendToPlayer(player, new AtmAccountSummaryPayload(
                true,
                summary.balance(),
                summary.availableBalance(),
                summary.configuredCreditLimit(),
                summary.totalDebt(),
                summary.globalCreditAvailable()
        ));
    }

    private static void syncOperationHistory(ServerPlayer player) throws SQLException {
        BankSession session = BankSessionService.INSTANCE.findActiveSession(player).orElse(null);
        if (session == null) {
            PacketDistributor.sendToPlayer(player, new AtmOperationHistoryPayload(List.of()));
            return;
        }
        syncOperationHistory(player, session.accountId());
    }

    private static void syncOperationHistory(ServerPlayer player, UUID accountId) throws SQLException {
        PacketDistributor.sendToPlayer(player, new AtmOperationHistoryPayload(ACCOUNT_HISTORY_SERVICE.recentEntries(accountId)));
    }

    private static void safeSyncOperationHistory(ServerPlayer player, UUID accountId) {
        try {
            syncOperationHistory(player, accountId);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao sincronizar historico da conta.", exception);
            PacketDistributor.sendToPlayer(player, new AtmOperationHistoryPayload(List.of()));
        }
    }

    private static void safeSyncAccountSummary(ServerPlayer player, java.util.UUID accountId) {
        try {
            syncAccountSummary(player, accountId);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao sincronizar resumo da conta.", exception);
            PacketDistributor.sendToPlayer(player, new AtmAccountSummaryPayload(false, 0L, 0L, 0L, 0L, 0L));
        }
    }

    private static void syncCards(ServerPlayer player) throws SQLException {
        BankSession session = BankSessionService.INSTANCE.findActiveSession(player).orElse(null);
        if (session == null) {
            PacketDistributor.sendToPlayer(player, new AtmCardsPayload(List.of()));
            return;
        }
        syncCards(player, session.accountId());
    }

    private static void syncCards(ServerPlayer player, UUID accountId) throws SQLException {
        PacketDistributor.sendToPlayer(player, new AtmCardsPayload(CARD_MANAGEMENT_SERVICE.cardsForAccount(accountId)));
    }

    private static void safeSyncCards(ServerPlayer player, UUID accountId) {
        try {
            syncCards(player, accountId);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao sincronizar cartoes da conta.", exception);
            PacketDistributor.sendToPlayer(player, new AtmCardsPayload(List.of()));
        }
    }

    private static boolean validUsernameAndPassword(String username, String password) {
        return username != null && !username.trim().isEmpty() && username.length() <= 32 && validPassword(password);
    }

    private static boolean validPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH && password.length() <= MAX_PASSWORD_LENGTH;
    }

    private static String stableRequestId(UUID requestId) {
        return (requestId == null ? UUID.randomUUID() : requestId).toString();
    }

    private static boolean creditRequestOnCooldown(UUID playerId) {
        long now = System.currentTimeMillis();
        Long previous = CREDIT_REQUEST_COOLDOWNS.putIfAbsent(playerId, now);
        if (previous == null) {
            return false;
        }
        if (now - previous < CREDIT_REQUEST_COOLDOWN_MILLIS) {
            return true;
        }
        CREDIT_REQUEST_COOLDOWNS.put(playerId, now);
        return false;
    }

    private static boolean invoicePaymentWindowOpen() {
        LocalDate today = LocalDate.now(ZoneId.of(EconomyServerConfig.ECONOMY_TIME_ZONE.get()));
        int dueDay = Math.min(today.lengthOfMonth(), EconomyServerConfig.CREDIT_INVOICE_DUE_DAY.get());
        int availableDay = Math.max(1, dueDay - EconomyServerConfig.CREDIT_INVOICE_AVAILABLE_DAYS_BEFORE.get());
        return today.getDayOfMonth() >= availableDay;
    }
}
