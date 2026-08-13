package br.com.economiamod.server.commercial;

import br.com.economiamod.common.block.CommercialBlockType;
import br.com.economiamod.server.account.AccountQueryService;
import br.com.economiamod.server.account.AccountBalanceSummary;
import br.com.economiamod.server.account.BankServerIdentityService;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryRepository;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryType;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class CommercialBlockSqlService {
    private final CommercialInventoryRepository inventoryRepository = new CommercialInventoryRepository();
    private final AccountQueryService accountQueryService = new AccountQueryService();

    public void registerPlacedBlock(
            UUID commercialBlockId,
            CommercialBlockType blockType,
            UUID ownerPlayerUuid,
            UUID placedByPlayerUuid,
            ResourceLocation dimension,
            BlockPos pos
    ) throws SQLException {
        String sql = """
                INSERT INTO economy_commercial_blocks(
                    id,
                    server_uuid,
                    block_type,
                    owner_player_uuid,
                    linked_account_id,
                    owner_name,
                    owner_account_number,
                    placed_by_player_uuid,
                    dimension,
                    block_x,
                    block_y,
                    block_z,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (id) DO UPDATE
                    SET server_uuid = EXCLUDED.server_uuid,
                        block_type = EXCLUDED.block_type,
                        owner_player_uuid = EXCLUDED.owner_player_uuid,
                        linked_account_id = EXCLUDED.linked_account_id,
                        owner_name = EXCLUDED.owner_name,
                        owner_account_number = EXCLUDED.owner_account_number,
                        placed_by_player_uuid = EXCLUDED.placed_by_player_uuid,
                        dimension = EXCLUDED.dimension,
                        block_x = EXCLUDED.block_x,
                        block_y = EXCLUDED.block_y,
                        block_z = EXCLUDED.block_z,
                        status = 'ACTIVE',
                        updated_at = CURRENT_TIMESTAMP,
                        removed_at = NULL
                """;

        UUID linkedAccountId = null;
        String ownerName = null;
        String ownerAccountNumber = null;
        if (ownerPlayerUuid != null) {
            linkedAccountId = accountQueryService.findActiveAccountIdByPlayer(ownerPlayerUuid).orElse(null);
            if (linkedAccountId != null) {
                AccountBalanceSummary summary = accountQueryService.findBalanceSummary(linkedAccountId).orElse(null);
                if (summary != null) {
                    ownerName = summary.username();
                    ownerAccountNumber = summary.accountNumber();
                }
            }
        }

        try (Connection connection = EconomyDatabase.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, commercialBlockId);
            statement.setObject(2, BankServerIdentityService.INSTANCE.current());
            statement.setString(3, blockType.name());
            statement.setObject(4, ownerPlayerUuid);
            statement.setObject(5, linkedAccountId);
            statement.setString(6, ownerName);
            statement.setString(7, ownerAccountNumber);
            statement.setObject(8, placedByPlayerUuid);
            statement.setString(9, dimension.toString());
            statement.setInt(10, pos.getX());
            statement.setInt(11, pos.getY());
            statement.setInt(12, pos.getZ());
            statement.executeUpdate();
        }

        ensureDefaultInventories(commercialBlockId, blockType);
    }

    public void markRemoved(UUID commercialBlockId) throws SQLException {
        String sql = """
                UPDATE economy_commercial_blocks
                   SET status = 'REMOVED',
                       updated_at = CURRENT_TIMESTAMP,
                       removed_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                   AND (server_uuid = ? OR server_uuid IS NULL)
                   AND status = 'ACTIVE'
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, commercialBlockId);
            statement.setObject(2, BankServerIdentityService.INSTANCE.current());
            statement.executeUpdate();
        }
    }

    private void ensureDefaultInventories(UUID commercialBlockId, CommercialBlockType blockType) throws SQLException {
        switch (blockType) {
            case SELL_SHOP -> {
                inventoryRepository.ensureSlots(commercialBlockId, CommercialInventoryType.PRODUCT_STOCK, 16);
                inventoryRepository.ensureSlots(commercialBlockId, CommercialInventoryType.CASH_RESERVE, 16);
            }
            case BUY_SHOP -> {
                inventoryRepository.ensureSlots(commercialBlockId, CommercialInventoryType.PURCHASED_ITEMS, 16);
                inventoryRepository.ensureSlots(commercialBlockId, CommercialInventoryType.CASH_RESERVE, 16);
            }
            case BANK_COUNTER -> {
                inventoryRepository.ensureSlots(commercialBlockId, CommercialInventoryType.BANK_STOCK, 16);
                inventoryRepository.ensureSlots(commercialBlockId, CommercialInventoryType.GOLD_RESERVE, 16);
            }
            case MAIL -> inventoryRepository.ensureSlots(commercialBlockId, CommercialInventoryType.MAIL_RECEIVED, 18);
            case ATM -> {
            }
        }
    }
}
