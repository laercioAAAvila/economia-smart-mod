package br.com.economiamod.server.transaction;

record AccountFinancialSnapshot(String status, long balance, long principalOutstanding, long interestOutstanding) {
}

