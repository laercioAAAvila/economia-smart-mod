package br.com.economiamod.server.account;

import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class BankWorldIdentitySavedData extends SavedData {
    private static final String DATA_NAME = "economia_server_identity";
    private static final String UUID_KEY = "server_uuid";
    private final UUID serverUuid;

    private BankWorldIdentitySavedData() {
        this(UUID.randomUUID());
        setDirty();
    }

    private BankWorldIdentitySavedData(UUID serverUuid) {
        this.serverUuid = serverUuid;
    }

    public static UUID get(MinecraftServer server) {
        SavedData.Factory<BankWorldIdentitySavedData> factory = new SavedData.Factory<>(
                BankWorldIdentitySavedData::new,
                (tag, provider) -> load(tag)
        );
        return server.overworld().getDataStorage().computeIfAbsent(factory, DATA_NAME).serverUuid;
    }

    private static BankWorldIdentitySavedData load(CompoundTag tag) {
        try {
            return new BankWorldIdentitySavedData(UUID.fromString(tag.getString(UUID_KEY)));
        } catch (IllegalArgumentException exception) {
            return new BankWorldIdentitySavedData();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putString(UUID_KEY, serverUuid.toString());
        return tag;
    }
}
