package br.com.economiamod.server.web;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.network.AtmCardsPayload;
import br.com.economiamod.server.account.AccountBalanceSummary;
import br.com.economiamod.server.account.AccountQueryService;
import br.com.economiamod.server.card.CardManagementService;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.gold.GoldDynamicPricingService;
import br.com.economiamod.server.gold.GoldPriceSnapshot;
import br.com.economiamod.server.invoice.InvoiceOpenEntry;
import br.com.economiamod.server.invoice.InvoiceQueryService;
import br.com.economiamod.server.invoice.InvoiceSummary;
import br.com.economiamod.server.persistence.EconomyDatabase;
import br.com.economiamod.server.transaction.AccountFinancialService;
import br.com.economiamod.server.transaction.FinancialOperationResult;
import br.com.economiamod.server.transaction.FinancialOperationResultType;
import br.com.economiamod.server.transaction.IdempotencyKeys;
import br.com.economiamod.server.transaction.RequestFingerprint;
import br.com.economiamod.server.transaction.TransactionOrigin;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Authenticated HTTP API used by the optional website. The API is intentionally available only
 * with PostgreSQL and loopback binding. Financial writes delegate to the same services used by the
 * Minecraft server, so ledger, locks and idempotency stay centralized.
 */
public final class WebEconomyApi {
    public static final WebEconomyApi INSTANCE = new WebEconomyApi();
    private static final int MAX_BODY_BYTES = 8 * 1024;

    private final Gson gson = new Gson();
    private final AccountQueryService accountQueryService = new AccountQueryService();
    private final AccountFinancialService financialService = new AccountFinancialService();
    private final WebTransactionHistoryService historyService = new WebTransactionHistoryService();
    private final CardManagementService cardManagementService = new CardManagementService();
    private final InvoiceQueryService invoiceQueryService = new InvoiceQueryService();
    private final GoldDynamicPricingService goldPricingService = new GoldDynamicPricingService();
    private final WebApiSessionService sessions = new WebApiSessionService();
    private final LoginRateLimiter loginLimiter = new LoginRateLimiter();
    private final RequestRateLimiter requestLimiter = new RequestRateLimiter();
    private final AtomicReference<HttpServer> server = new AtomicReference<>();
    private final AtomicReference<ExecutorService> executor = new AtomicReference<>();

    private WebEconomyApi() {
    }

    public synchronized void startIfEnabled() throws IOException {
        if (!EconomyServerConfig.WEB_API_ENABLED.get()) {
            return;
        }
        if (!EconomyDatabase.isPostgreSql()) {
            EconomiaMod.LOGGER.warn("Web API ignorada: por seguranca ela so e habilitada no modo PostgreSQL.");
            return;
        }
        if (server.get() != null) {
            return;
        }

        String bind = EconomyServerConfig.WEB_API_BIND.get().trim();
        InetAddress address = InetAddress.getByName(bind);
        if (!address.isLoopbackAddress()) {
            throw new IOException("webApi.bind deve ser loopback (ex.: 127.0.0.1). Publique somente por proxy HTTPS.");
        }

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(address, EconomyServerConfig.WEB_API_PORT.get()), 32);
        ExecutorService apiExecutor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "economia-web-api");
            thread.setDaemon(true);
            return thread;
        });
        httpServer.setExecutor(apiExecutor);
        httpServer.createContext("/api/v1/health", this::health);
        httpServer.createContext("/api/v1/auth/token", this::tokenLogin);
        httpServer.createContext("/api/v1/auth/session", this::session);
        httpServer.createContext("/api/v1/auth/logout", this::logout);
        httpServer.createContext("/api/v1/account", this::account);
        httpServer.createContext("/api/v1/transfers", this::transfer);
        httpServer.createContext("/api/v1/transactions", this::transactions);
        httpServer.createContext("/api/v1/cards", this::cards);
        httpServer.createContext("/api/v1/credit", this::credit);
        httpServer.createContext("/api/v1/gold", this::gold);
        httpServer.start();
        executor.set(apiExecutor);
        server.set(httpServer);
        EconomiaMod.LOGGER.info("Economia Web API ativa em http://{}:{}/api/v1 (somente loopback).",
                bind, EconomyServerConfig.WEB_API_PORT.get());
    }

    public synchronized void stop() {
        HttpServer current = server.getAndSet(null);
        if (current != null) {
            current.stop(0);
        }
        ExecutorService currentExecutor = executor.getAndSet(null);
        if (currentExecutor != null) {
            currentExecutor.shutdownNow();
        }
        sessions.clear();
        WebLoginTicketService.INSTANCE.clear();
        requestLimiter.clear();
    }

    private void health(HttpExchange exchange) throws IOException {
        if (!preflightOrAllowed(exchange, "GET")) {
            return;
        }
        respond(exchange, 200, Map.of("ok", true, "database", "postgresql", "time", Instant.now().toString()));
    }

    private void tokenLogin(HttpExchange exchange) throws IOException {
        if (!preflightOrAllowed(exchange, "POST")) {
            return;
        }
        String remote = remoteAddress(exchange);
        int maxAttempts = EconomyServerConfig.WEB_API_LOGIN_MAX_ATTEMPTS.get();
        int window = EconomyServerConfig.WEB_API_LOGIN_WINDOW_SECONDS.get();
        if (!loginLimiter.allow(remote, maxAttempts, window)) {
            respond(exchange, 429, error("too_many_attempts"));
            return;
        }

        TokenLoginRequest request;
        try {
            request = parseJson(exchange, TokenLoginRequest.class);
        } catch (BadRequestException exception) {
            respond(exchange, 400, error(exception.getMessage()));
            return;
        }
        if (request == null || request.token == null || request.token.isBlank() || request.token.length() > 32) {
            loginLimiter.recordFailure(remote, window);
            respond(exchange, 401, error("invalid_token"));
            return;
        }

        Optional<WebLoginTicketService.Ticket> ticket = WebLoginTicketService.INSTANCE.redeem(request.token);
        request.token = null;
        if (ticket.isEmpty()) {
            loginLimiter.recordFailure(remote, window);
            respond(exchange, 401, error("invalid_token"));
            return;
        }

        try {
            Optional<AccountBalanceSummary> summary = accountQueryService.findBalanceSummary(ticket.get().accountId());
            if (summary.isEmpty()) {
                respond(exchange, 401, error("invalid_token"));
                return;
            }
            loginLimiter.clear(remote);
            int timeout = EconomyServerConfig.WEB_API_SESSION_TIMEOUT_SECONDS.get();
            String bearer = sessions.create(ticket.get().accountId(), ticket.get().playerUuid(), timeout);
            AccountBalanceSummary account = summary.get();
            respond(exchange, 200, Map.of(
                    "token", bearer,
                    "tokenType", "Bearer",
                    "expiresIn", timeout,
                    "username", safe(account.username()),
                    "accountNumber", safe(account.accountNumber())
            ));
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha interna ao trocar token web por sessao; dados sensiveis omitidos.", exception);
            respond(exchange, 503, error("service_unavailable"));
        }
    }

    private void session(HttpExchange exchange) throws IOException {
        if (!preflightOrAllowed(exchange, "GET")) {
            return;
        }
        WebApiSessionService.Session session = authenticated(exchange);
        if (session == null) {
            return;
        }
        try {
            Optional<AccountBalanceSummary> summary = accountQueryService.findBalanceSummary(session.accountId());
            if (summary.isEmpty()) {
                sessions.remove(bearerToken(exchange));
                respond(exchange, 401, error("unauthorized"));
                return;
            }
            AccountBalanceSummary account = summary.get();
            respond(exchange, 200, Map.of(
                    "authenticated", true,
                    "username", safe(account.username()),
                    "accountNumber", safe(account.accountNumber()),
                    "expiresAt", session.expiresAt().toString()
            ));
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao validar sessao da Web API.", exception);
            respond(exchange, 503, error("service_unavailable"));
        }
    }

    private void logout(HttpExchange exchange) throws IOException {
        if (!preflightOrAllowed(exchange, "POST")) {
            return;
        }
        String token = bearerToken(exchange);
        if (token == null) {
            respond(exchange, 401, error("unauthorized"));
            return;
        }
        sessions.remove(token);
        respond(exchange, 200, Map.of("ok", true));
    }

    private void account(HttpExchange exchange) throws IOException {
        if (!preflightOrAllowed(exchange, "GET")) {
            return;
        }
        WebApiSessionService.Session session = authenticated(exchange);
        if (session == null) {
            return;
        }
        try {
            Optional<AccountBalanceSummary> summary = accountQueryService.findBalanceSummary(session.accountId());
            if (summary.isEmpty()) {
                respond(exchange, 404, error("account_not_found"));
                return;
            }
            AccountBalanceSummary account = summary.get();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("username", safe(account.username()));
            body.put("accountNumber", safe(account.accountNumber()));
            body.put("balance", account.balance());
            body.put("availableBalance", account.availableBalance());
            body.put("configuredCreditLimit", account.configuredCreditLimit());
            body.put("creditPrincipalOutstanding", account.creditPrincipalOutstanding());
            body.put("creditInterestOutstanding", account.creditInterestOutstanding());
            body.put("totalDebt", account.totalDebt());
            body.put("globalCreditAvailable", account.globalCreditAvailable());
            respond(exchange, 200, body);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao consultar conta pela Web API.", exception);
            respond(exchange, 503, error("service_unavailable"));
        }
    }

    private void transfer(HttpExchange exchange) throws IOException {
        if (!preflightOrAllowed(exchange, "POST")) {
            return;
        }
        WebApiSessionService.Session session = authenticated(exchange);
        if (session == null) {
            return;
        }
        String throttleKey = "transfer:" + remoteAddress(exchange) + ":" + session.accountId();
        if (!requestLimiter.allowAndRecord(throttleKey, 20, 60)) {
            respond(exchange, 429, error("too_many_requests"));
            return;
        }

        String idempotencyHeader = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        final String externalKey;
        try {
            externalKey = IdempotencyKeys.requireValid(idempotencyHeader);
        } catch (IllegalArgumentException exception) {
            respond(exchange, 400, error("missing_or_invalid_idempotency_key"));
            return;
        }

        TransferRequest request;
        try {
            request = parseJson(exchange, TransferRequest.class);
        } catch (BadRequestException exception) {
            respond(exchange, 400, error(exception.getMessage()));
            return;
        }
        if (request == null || request.destinationAccount == null
                || !request.destinationAccount.matches("\\d{6}") || request.amount <= 0L) {
            respond(exchange, 400, error("invalid_transfer"));
            return;
        }

        try {
            Optional<UUID> destination = accountQueryService.findActiveAccountIdByNumber(request.destinationAccount);
            if (destination.isEmpty()) {
                respond(exchange, 404, error("destination_not_found"));
                return;
            }
            if (destination.get().equals(session.accountId())) {
                respond(exchange, 400, error("same_account"));
                return;
            }

            String internalKey = "web:" + session.accountId() + ":" + RequestFingerprint.of(externalKey).substring(0, 48);
            FinancialOperationResult result = financialService.transfer(session.playerUuid(), session.accountId(),
                    destination.get(), request.amount, null, internalKey, TransactionOrigin.WEB);
            if (result.type() == FinancialOperationResultType.COMPLETED
                    || result.type() == FinancialOperationResultType.DUPLICATE_COMPLETED) {
                respond(exchange, 200, Map.of(
                        "status", result.type().name(),
                        "transactionId", result.transactionId() == null ? "" : result.transactionId().toString()
                ));
                return;
            }
            int status = result.type() == FinancialOperationResultType.IDEMPOTENCY_CONFLICT ? 409 : 422;
            respond(exchange, status, error(result.type().name().toLowerCase()));
        } catch (SQLException | ArithmeticException | IllegalArgumentException exception) {
            EconomiaMod.LOGGER.warn("Falha ao executar transferencia pela Web API.", exception);
            respond(exchange, 503, error("service_unavailable"));
        }
    }

    private void transactions(HttpExchange exchange) throws IOException {
        if (!preflightOrAllowed(exchange, "GET")) {
            return;
        }
        WebApiSessionService.Session session = authenticated(exchange);
        if (session == null) {
            return;
        }
        try {
            respond(exchange, 200, Map.of("transactions", historyService.recent(session.accountId(), 100)));
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao consultar historico pela Web API.", exception);
            respond(exchange, 503, error("service_unavailable"));
        }
    }

    private void cards(HttpExchange exchange) throws IOException {
        if (!preflightOrAllowed(exchange, "GET")) {
            return;
        }
        WebApiSessionService.Session session = authenticated(exchange);
        if (session == null) {
            return;
        }
        try {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (AtmCardsPayload.CardSummary card : cardManagementService.cardsForAccount(session.accountId())) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", safe(card.cardName()));
                row.put("type", safe(card.cardType()));
                row.put("status", safe(card.status()));
                row.put("creditLimit", card.individualCreditLimit());
                row.put("debt", card.debt());
                row.put("debitDailyLimit", card.debitDailyLimit());
                rows.add(row);
            }
            respond(exchange, 200, Map.of("cards", rows));
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao consultar cartoes pela Web API.", exception);
            respond(exchange, 503, error("service_unavailable"));
        }
    }

    private void credit(HttpExchange exchange) throws IOException {
        if (!preflightOrAllowed(exchange, "GET")) {
            return;
        }
        WebApiSessionService.Session session = authenticated(exchange);
        if (session == null) {
            return;
        }
        try {
            Optional<AccountBalanceSummary> accountResult = accountQueryService.findBalanceSummary(session.accountId());
            if (accountResult.isEmpty()) {
                respond(exchange, 404, error("account_not_found"));
                return;
            }
            AccountBalanceSummary account = accountResult.get();
            InvoiceSummary invoice = invoiceQueryService.accountInvoice(session.accountId())
                    .orElse(new InvoiceSummary(0L, 0L, 0L, List.of()));

            List<Map<String, Object>> entries = new ArrayList<>();
            for (InvoiceOpenEntry entry : invoice.openEntries()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("type", safe(entry.entryType()));
                row.put("amount", entry.remainingAmount());
                row.put("description", safe(entry.description()));
                row.put("merchant", safe(entry.merchantName()));
                row.put("businessDate", entry.businessDate() == null ? "" : entry.businessDate().toString());
                row.put("createdAt", entry.createdAt() == null ? "" : entry.createdAt().toString());
                entries.add(row);
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("configuredLimit", account.configuredCreditLimit());
            body.put("availableCredit", account.globalCreditAvailable());
            body.put("principalOutstanding", invoice.principalOutstanding());
            body.put("interestOutstanding", invoice.interestOutstanding());
            body.put("totalDebt", invoice.totalDebt());
            body.put("dueDay", EconomyServerConfig.CREDIT_INVOICE_DUE_DAY.get());
            body.put("availableDaysBeforeDue", EconomyServerConfig.CREDIT_INVOICE_AVAILABLE_DAYS_BEFORE.get());
            body.put("entries", entries);
            respond(exchange, 200, body);
        } catch (SQLException exception) {
            EconomiaMod.LOGGER.warn("Falha ao consultar credito pela Web API.", exception);
            respond(exchange, 503, error("service_unavailable"));
        }
    }

    private void gold(HttpExchange exchange) throws IOException {
        if (!preflightOrAllowed(exchange, "GET")) {
            return;
        }
        if (authenticated(exchange) == null) {
            return;
        }
        try {
            GoldPriceSnapshot snapshot = goldPricingService.currentSnapshot();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("enabled", EconomyServerConfig.BANK_GOLD_ENABLED.get());
            body.put("dynamicPricing", EconomyServerConfig.DYNAMIC_PRICING_ENABLED.get());
            body.put("baseNuggetValue", snapshot.baseNuggetValue());
            body.put("buyBps", snapshot.buyBps());
            body.put("buyPercent", Math.max(1L, Math.round(snapshot.buyBps() / 100.0D)));
            body.put("sellPercent", 100L);
            body.put("nuggetValue", snapshot.nuggetBuyValue());
            body.put("ingotValue", snapshot.ingotBuyValue());
            body.put("blockValue", snapshot.blockBuyValue());
            body.put("demandLevel", snapshot.demandLevel());
            body.put("idleLevel", snapshot.idleLevel());
            body.put("recentMintedNuggetUnits", snapshot.recentMintedNuggetUnits());
            respond(exchange, 200, body);
        } catch (SQLException | RuntimeException exception) {
            EconomiaMod.LOGGER.warn("Falha ao consultar cotacao do ouro pela Web API.", exception);
            respond(exchange, 503, error("service_unavailable"));
        }
    }

    private WebApiSessionService.Session authenticated(HttpExchange exchange) throws IOException {
        String token = bearerToken(exchange);
        Optional<WebApiSessionService.Session> session = sessions.find(token);
        if (session.isEmpty()) {
            respond(exchange, 401, error("unauthorized"));
            return null;
        }
        return session.get();
    }

    private String bearerToken(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = auth.substring(7).trim();
        return token.length() >= 32 && token.length() <= 128 ? token : null;
    }

    private boolean preflightOrAllowed(HttpExchange exchange, String method) throws IOException {
        addSecurityHeaders(exchange.getResponseHeaders());
        if (!originAllowed(exchange)) {
            respond(exchange, 403, error("origin_not_allowed"));
            return false;
        }
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            Headers headers = exchange.getResponseHeaders();
            headers.set("Access-Control-Allow-Methods", method + ", OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Authorization, Content-Type, Idempotency-Key");
            headers.set("Access-Control-Max-Age", "600");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return false;
        }
        if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", method + ", OPTIONS");
            respond(exchange, 405, error("method_not_allowed"));
            return false;
        }
        return true;
    }

    private boolean originAllowed(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        String configured = EconomyServerConfig.WEB_API_ALLOWED_ORIGIN.get().trim();
        if (origin == null || origin.isBlank()) {
            return true;
        }
        if (configured.isBlank() || !origin.equals(configured)) {
            return false;
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", configured);
        exchange.getResponseHeaders().set("Vary", "Origin");
        return true;
    }

    private void addSecurityHeaders(Headers headers) {
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        headers.set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
    }

    private <T> T parseJson(HttpExchange exchange, Class<T> type) throws IOException, BadRequestException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
            throw new BadRequestException("content_type_must_be_json");
        }
        byte[] data;
        try (InputStream input = exchange.getRequestBody()) {
            data = input.readNBytes(MAX_BODY_BYTES + 1);
        }
        if (data.length > MAX_BODY_BYTES) {
            throw new BadRequestException("request_too_large");
        }
        try {
            return gson.fromJson(new String(data, StandardCharsets.UTF_8), type);
        } catch (JsonParseException exception) {
            throw new BadRequestException("invalid_json");
        }
    }

    private Map<String, Object> error(String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        return body;
    }

    private void respond(HttpExchange exchange, int status, Object body) throws IOException {
        addSecurityHeaders(exchange.getResponseHeaders());
        byte[] json = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, json.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(json);
        } finally {
            exchange.close();
        }
    }

    private String remoteAddress(HttpExchange exchange) {
        String direct = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (exchange.getRemoteAddress().getAddress().isLoopbackAddress()) {
            String forwarded = exchange.getRequestHeaders().getFirst("X-Real-IP");
            if (forwarded != null) {
                String candidate = forwarded.trim();
                if (!candidate.isBlank() && candidate.length() <= 64 && candidate.indexOf(',') < 0
                        && candidate.matches("[0-9A-Fa-f:.]+")) {
                    return candidate;
                }
            }
        }
        return direct;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class TokenLoginRequest {
        String token;
    }

    private static final class TransferRequest {
        String destinationAccount;
        long amount;
    }

    private static final class BadRequestException extends Exception {
        BadRequestException(String message) {
            super(message);
        }
    }
}
