package br.com.economiamod.server.job;

import br.com.economiamod.server.interest.InterestAccrualResult;

public record DailyJobRunResult(DailyJobRunResultType type, InterestAccrualResult interestResult) {
    public static DailyJobRunResult completed(InterestAccrualResult interestResult) {
        return new DailyJobRunResult(DailyJobRunResultType.COMPLETED, interestResult);
    }

    public static DailyJobRunResult alreadyCompleted() {
        return new DailyJobRunResult(DailyJobRunResultType.ALREADY_COMPLETED, null);
    }
}

