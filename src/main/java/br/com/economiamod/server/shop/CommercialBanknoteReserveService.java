package br.com.economiamod.server.shop;

import br.com.economiamod.common.money.MoneyStackCalculator;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryMutationRepository;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryRemovalRepository;
import br.com.economiamod.server.commercial.inventory.CommercialInventoryType;
import br.com.economiamod.server.commercial.inventory.CommercialItemSnapshot;
import br.com.economiamod.server.commercial.inventory.ItemStackSnapshotMapper;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CommercialBanknoteReserveService {
    private final CommercialInventoryMutationRepository mutationRepository = new CommercialInventoryMutationRepository();
    private final CommercialInventoryRemovalRepository removalRepository = new CommercialInventoryRemovalRepository();
    private final ItemStackSnapshotMapper mapper = new ItemStackSnapshotMapper();

    public boolean canAddExact(Connection connection, UUID commercialBlockId, long amount) throws SQLException {
        for (Map.Entry<Item, Integer> entry : MoneyStackCalculator.buildBanknotes(amount).entrySet()) {
            if (!mutationRepository.canAddToSlots(connection, commercialBlockId, CommercialInventoryType.CASH_RESERVE, snapshot(entry), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public boolean addExact(Connection connection, UUID commercialBlockId, long amount) throws SQLException {
        if (!canAddExact(connection, commercialBlockId, amount)) {
            return false;
        }
        for (Map.Entry<Item, Integer> entry : MoneyStackCalculator.buildBanknotes(amount).entrySet()) {
            boolean added = mutationRepository.addToSlots(connection, commercialBlockId, CommercialInventoryType.CASH_RESERVE, snapshot(entry), entry.getValue());
            if (!added) {
                return false;
            }
        }
        return true;
    }

    public boolean canRemoveExact(Connection connection, UUID commercialBlockId, long amount) throws SQLException {
        for (Map.Entry<Item, Integer> entry : MoneyStackCalculator.buildBanknotes(amount).entrySet()) {
            if (!removalRepository.canRemove(connection, commercialBlockId, CommercialInventoryType.CASH_RESERVE, snapshot(entry), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public boolean removeExact(Connection connection, UUID commercialBlockId, long amount) throws SQLException {
        if (!canRemoveExact(connection, commercialBlockId, amount)) {
            return false;
        }
        for (Map.Entry<Item, Integer> entry : MoneyStackCalculator.buildBanknotes(amount).entrySet()) {
            boolean removed = removalRepository.remove(connection, commercialBlockId, CommercialInventoryType.CASH_RESERVE, snapshot(entry), entry.getValue());
            if (!removed) {
                return false;
            }
        }
        return true;
    }

    private CommercialItemSnapshot snapshot(Map.Entry<Item, Integer> entry) {
        return mapper.fromStack(new ItemStack(entry.getKey(), entry.getValue()));
    }
}
