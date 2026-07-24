package br.com.economiamod.server.account;

import java.util.UUID;

record AccountDeletionTarget(UUID accountId, UUID playerUuid, String username, String accountNumber) {
}
