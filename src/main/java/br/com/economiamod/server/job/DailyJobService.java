package br.com.economiamod.server.job;

import br.com.economiamod.server.interest.InterestAccrualProcessor;
import br.com.economiamod.server.interest.InterestAccrualResult;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

public final class DailyJobService {
    public DailyJobRunResult runCreditInterest(LocalDate businessDate) throws SQLException {
        if (isCompleted(DailyJobType.CREDIT_INTEREST, businessDate)) {
            return DailyJobRunResult.alreadyCompleted();
        }

        markRunning(DailyJobType.CREDIT_INTEREST, businessDate);
        try {
            InterestAccrualResult result = new InterestAccrualProcessor().process(businessDate);
            markCompleted(DailyJobType.CREDIT_INTEREST, businessDate);
            return DailyJobRunResult.completed(result);
        } catch (SQLException | RuntimeException exception) {
            markFailed(DailyJobType.CREDIT_INTEREST, businessDate, exception.getMessage());
            throw exception;
        }
    }

    private boolean isCompleted(DailyJobType jobType, LocalDate businessDate) throws SQLException {
        String sql = """
                SELECT status
                  FROM economy_daily_job_runs
                 WHERE job_type = ?
                   AND business_date = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, jobType.name());
            statement.setObject(2, businessDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && DailyJobStatus.COMPLETED.name().equals(resultSet.getString("status"));
            }
        }
    }

    private void markRunning(DailyJobType jobType, LocalDate businessDate) throws SQLException {
        String sql = """
                INSERT INTO economy_daily_job_runs(id, job_type, business_date, status, started_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (job_type, business_date)
                DO UPDATE SET status = EXCLUDED.status,
                              started_at = CURRENT_TIMESTAMP,
                              completed_at = NULL,
                              failure_reason = NULL
                WHERE economy_daily_job_runs.status <> 'COMPLETED'
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, jobType.name());
            statement.setObject(3, businessDate);
            statement.setString(4, DailyJobStatus.RUNNING.name());
            statement.executeUpdate();
        }
    }

    private void markCompleted(DailyJobType jobType, LocalDate businessDate) throws SQLException {
        String sql = """
                UPDATE economy_daily_job_runs
                   SET status = ?,
                       completed_at = CURRENT_TIMESTAMP,
                       failure_reason = NULL
                 WHERE job_type = ?
                   AND business_date = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DailyJobStatus.COMPLETED.name());
            statement.setString(2, jobType.name());
            statement.setObject(3, businessDate);
            statement.executeUpdate();
        }
    }

    private void markFailed(DailyJobType jobType, LocalDate businessDate, String failureReason) throws SQLException {
        String sql = """
                UPDATE economy_daily_job_runs
                   SET status = ?,
                       failure_reason = ?,
                       completed_at = NULL
                 WHERE job_type = ?
                   AND business_date = ?
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DailyJobStatus.FAILED.name());
            statement.setString(2, trimFailureReason(failureReason));
            statement.setString(3, jobType.name());
            statement.setObject(4, businessDate);
            statement.executeUpdate();
        }
    }

    private String trimFailureReason(String failureReason) {
        if (failureReason == null) {
            return null;
        }
        return failureReason.length() <= 255 ? failureReason : failureReason.substring(0, 255);
    }
}

