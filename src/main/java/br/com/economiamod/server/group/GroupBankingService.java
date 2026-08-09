package br.com.economiamod.server.group;

import br.com.economiamod.common.group.GroupMembership;
import br.com.economiamod.common.group.GroupSummary;
import br.com.economiamod.server.account.AccountQueryService;
import br.com.economiamod.server.transaction.AccountFinancialService;
import br.com.economiamod.server.transaction.FinancialOperationResult;
import java.sql.SQLException;
import java.util.UUID;

public final class GroupBankingService {
    private final GroupRepository groupRepository = new GroupRepository();
    private final AccountQueryService accountQueryService = new AccountQueryService();
    private final AccountFinancialService financialService = new AccountFinancialService();

    public FinancialOperationResult deposit(UUID playerUuid, UUID groupId, boolean supportFund,
                                            long amount, String idempotencyKey) throws SQLException {
        requireMembership(playerUuid, groupId);
        GroupSummary group = requireGroup(groupId);
        UUID personalAccount = accountQueryService.findActiveAccountIdByPlayer(playerUuid)
                .orElseThrow(() -> new IllegalStateException("player account missing"));
        UUID collectiveAccount = collectiveAccount(group, supportFund);
        return financialService.transfer(playerUuid, personalAccount, collectiveAccount, amount, null, idempotencyKey);
    }

    public FinancialOperationResult withdraw(UUID playerUuid, UUID groupId, boolean supportFund,
                                             long amount, String idempotencyKey) throws SQLException {
        GroupMembership membership = requireMembership(playerUuid, groupId);
        if (!supportFund && membership.groupType() == br.com.economiamod.common.group.GroupType.CLAN
                && !membership.role().leadsClan()) {
            throw new SecurityException("clan treasury requires leadership");
        }
        GroupSummary group = requireGroup(groupId);
        UUID personalAccount = accountQueryService.findActiveAccountIdByPlayer(playerUuid)
                .orElseThrow(() -> new IllegalStateException("player account missing"));
        UUID collectiveAccount = collectiveAccount(group, supportFund);
        return financialService.transfer(playerUuid, collectiveAccount, personalAccount, amount, null, idempotencyKey);
    }

    private GroupMembership requireMembership(UUID playerUuid, UUID groupId) throws SQLException {
        return groupRepository.membership(playerUuid, groupId)
                .orElseThrow(() -> new SecurityException("group membership required"));
    }

    private GroupSummary requireGroup(UUID groupId) throws SQLException {
        return groupRepository.group(groupId).orElseThrow(() -> new IllegalStateException("group missing"));
    }

    private UUID collectiveAccount(GroupSummary group, boolean supportFund) {
        if (supportFund) {
            if (group.supportAccountId() == null) {
                throw new IllegalArgumentException("private property has no support fund");
            }
            return group.supportAccountId();
        }
        return group.accountId();
    }
}
