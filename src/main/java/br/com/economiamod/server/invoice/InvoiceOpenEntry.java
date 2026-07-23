package br.com.economiamod.server.invoice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceOpenEntry(
        UUID entryId,
        UUID cardId,
        String entryType,
        long remainingAmount,
        String description,
        String merchantName,
        LocalDate businessDate,
        LocalDateTime createdAt
) {
    public boolean isInterest() {
        return "DAILY_INTEREST".equals(entryType);
    }
}

