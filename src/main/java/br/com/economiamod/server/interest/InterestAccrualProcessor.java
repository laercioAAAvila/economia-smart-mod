package br.com.economiamod.server.interest;

import br.com.economiamod.common.account.AccountStatus;
import br.com.economiamod.common.credit.InterestCalculation;
import br.com.economiamod.common.credit.InterestCalculator;
import br.com.economiamod.common.credit.InterestMode;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InterestAccrualProcessor {
    private static final int CANDIDATE_BATCH_SIZE = 500;

    private final InterestAccrualRepository repository;
    private final InterestAccrualWriter writer;

    public InterestAccrualProcessor() {
        this(new InterestAccrualRepository(), new InterestAccrualWriter());
    }

    InterestAccrualProcessor(InterestAccrualRepository repository, InterestAccrualWriter writer) {
        this.repository = repository;
        this.writer = writer;
    }

    public InterestAccrualResult process(LocalDate accrualDate) throws SQLException {
        if (!EconomyServerConfig.CREDIT_INTEREST_ENABLED.get()) {
            return new InterestAccrualResult(0, 0, 0L);
        }

        int processed = 0;
        int skipped = 0;
        long interestAmount = 0L;
        UUID lastAccountId = null;
        UUID lastCardId = null;

        while (true) {
            List<CardCandidate> candidates = repository.candidatesAfter(lastAccountId, lastCardId, CANDIDATE_BATCH_SIZE);
            if (candidates.isEmpty()) {
                break;
            }

            for (CardCandidate candidate : candidates) {
                CardAccrualOutcome outcome = processCard(candidate, accrualDate);
                if (outcome.processed()) {
                    processed++;
                    interestAmount += outcome.interestAmount();
                } else {
                    skipped++;
                }
            }

            CardCandidate last = candidates.get(candidates.size() - 1);
            lastAccountId = last.accountId();
            lastCardId = last.cardId();
        }

        return new InterestAccrualResult(processed, skipped, interestAmount);
    }

    private CardAccrualOutcome processCard(CardCandidate candidate, LocalDate accrualDate) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                repository.lockAccount(connection, candidate.accountId());
                repository.lockCard(connection, candidate.cardId());

                CardAccrualOutcome outcome = calculateAndWrite(connection, candidate, accrualDate);
                connection.commit();
                return outcome;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private CardAccrualOutcome calculateAndWrite(Connection connection, CardCandidate candidate, LocalDate accrualDate) throws SQLException {
        if (repository.alreadyAccrued(connection, candidate.cardId(), accrualDate)) {
            return CardAccrualOutcome.skipped();
        }

        InterestCardDebt cardDebt = repository.cardDebt(connection, candidate.cardId()).orElse(null);
        InterestAccountDebt accountDebt = repository.accountDebt(connection, candidate.accountId()).orElse(null);
        if (cardDebt == null || accountDebt == null || !AccountStatus.ACTIVE.name().equals(accountDebt.status())) {
            return CardAccrualOutcome.skipped();
        }

        long eligiblePrincipal = repository.eligiblePrincipal(connection, candidate.cardId(), accrualDate);
        if (eligiblePrincipal <= 0L && cardDebt.interestOutstanding() <= 0L) {
            writer.insertAccrual(connection, candidate, accrualDate, configuredMode(), 0L, cardDebt.roundingRemainder(), 0L, cardDebt.roundingRemainder(), null);
            return CardAccrualOutcome.processed(0L);
        }

        InterestMode mode = configuredMode();
        InterestCalculation calculation = InterestCalculator.calculate(
                mode,
                eligiblePrincipal,
                cardDebt.interestOutstanding(),
                cardDebt.roundingRemainder(),
                EconomyServerConfig.CREDIT_INTEREST_DAILY_RATE_BPS.get()
        );

        UUID transactionId = writeInterestSideEffects(connection, candidate, accrualDate, accountDebt.balance(), calculation);
        writer.insertAccrual(
                connection,
                candidate,
                accrualDate,
                mode,
                calculation.calculationBase(),
                calculation.remainderBefore(),
                calculation.interestAmount(),
                calculation.remainderAfter(),
                transactionId
        );
        return CardAccrualOutcome.processed(calculation.interestAmount());
    }

    private UUID writeInterestSideEffects(Connection connection, CardCandidate candidate, LocalDate accrualDate, long accountBalance, InterestCalculation calculation) throws SQLException {
        if (calculation.interestAmount() <= 0L) {
            writer.updateRemainder(connection, candidate.cardId(), calculation.remainderAfter());
            return null;
        }

        UUID transactionId = UUID.randomUUID();
        writer.increaseInterest(connection, candidate, calculation.interestAmount(), calculation.remainderAfter());
        writer.insertTransaction(connection, transactionId, candidate, accrualDate, calculation.interestAmount());
        writer.insertLedger(connection, transactionId, candidate.accountId(), calculation.interestAmount(), accountBalance);
        writer.insertCardInterestEntry(connection, transactionId, candidate.cardId(), calculation.interestAmount(), accrualDate);
        return transactionId;
    }

    private InterestMode configuredMode() {
        return InterestMode.valueOf(EconomyServerConfig.CREDIT_INTEREST_MODE.get().toUpperCase(java.util.Locale.ROOT));
    }

    private record CardAccrualOutcome(boolean processed, long interestAmount) {
        static CardAccrualOutcome processed(long interestAmount) {
            return new CardAccrualOutcome(true, interestAmount);
        }

        static CardAccrualOutcome skipped() {
            return new CardAccrualOutcome(false, 0L);
        }
    }
}
