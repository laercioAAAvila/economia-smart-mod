package br.com.economiamod.common.money;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public enum MoneyDenomination {
    NOTE_1(1),
    NOTE_2(2),
    NOTE_5(5),
    NOTE_10(10),
    NOTE_20(20),
    NOTE_50(50),
    NOTE_100(100),
    NOTE_200(200);

    public static final List<MoneyDenomination> DESCENDING = Arrays.stream(values())
            .sorted(Comparator.comparingLong(MoneyDenomination::value).reversed())
            .toList();

    private final long value;

    MoneyDenomination(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }
}

