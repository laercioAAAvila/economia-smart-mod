package br.com.economiamod.server.invoice;

record AccountDebtSnapshot(String status, long balance, long principalOutstanding, long interestOutstanding) {
}

