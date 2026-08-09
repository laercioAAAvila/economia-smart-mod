package br.com.economiamod.common.network;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.group.GroupRole;
import br.com.economiamod.common.group.GroupType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GroupStatePayload(boolean authenticated, GroupType groupType, boolean hasGroup, UUID groupId,
                                String groupName, GroupRole role, long balance, long supportBalance, int claimLimit,
                                boolean visitorBuyShop, boolean visitorSellShop,
                                List<MemberSummary> members, List<InviteSummary> invites) implements CustomPacketPayload {
    private static final UUID ZERO = new UUID(0L, 0L);
    public static final Type<GroupStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "group_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GroupStatePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GroupStatePayload decode(RegistryFriendlyByteBuf buffer) {
            boolean authenticated = buffer.readBoolean();
            GroupType type = GroupType.values()[buffer.readVarInt()];
            boolean hasGroup = buffer.readBoolean();
            UUID groupId = buffer.readUUID();
            String name = buffer.readUtf(64);
            int roleId = buffer.readVarInt();
            GroupRole role = roleId < 0 || roleId >= GroupRole.values().length ? GroupRole.MEMBER : GroupRole.values()[roleId];
            long balance = buffer.readLong();
            long support = buffer.readLong();
            int claimLimit = buffer.readVarInt();
            boolean visitorBuy = buffer.readBoolean();
            boolean visitorSell = buffer.readBoolean();
            int memberCount = Math.min(1000, Math.max(0, buffer.readVarInt()));
            List<MemberSummary> members = new ArrayList<>(memberCount);
            for (int index = 0; index < memberCount; index++) {
                members.add(new MemberSummary(buffer.readUUID(), buffer.readUtf(64),
                        GroupRole.values()[buffer.readVarInt()], buffer.readVarInt(), buffer.readLong()));
            }
            int inviteCount = Math.min(1000, Math.max(0, buffer.readVarInt()));
            List<InviteSummary> invites = new ArrayList<>(inviteCount);
            for (int index = 0; index < inviteCount; index++) {
                invites.add(new InviteSummary(buffer.readUUID(), buffer.readUUID(), buffer.readUtf(64)));
            }
            return new GroupStatePayload(authenticated, type, hasGroup, groupId, name, role, balance, support,
                    claimLimit, visitorBuy, visitorSell, members, invites);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GroupStatePayload payload) {
            buffer.writeBoolean(payload.authenticated());
            buffer.writeVarInt(payload.groupType().ordinal());
            buffer.writeBoolean(payload.hasGroup());
            buffer.writeUUID(payload.groupId() == null ? ZERO : payload.groupId());
            buffer.writeUtf(payload.groupName() == null ? "" : payload.groupName(), 64);
            buffer.writeVarInt(payload.role() == null ? GroupRole.MEMBER.ordinal() : payload.role().ordinal());
            buffer.writeLong(payload.balance());
            buffer.writeLong(payload.supportBalance());
            buffer.writeVarInt(Math.max(0, payload.claimLimit()));
            buffer.writeBoolean(payload.visitorBuyShop());
            buffer.writeBoolean(payload.visitorSellShop());
            buffer.writeVarInt(payload.members().size());
            for (MemberSummary member : payload.members()) {
                buffer.writeUUID(member.playerUuid());
                buffer.writeUtf(member.displayName(), 64);
                buffer.writeVarInt(member.role().ordinal());
                buffer.writeVarInt(member.permissionMask());
                buffer.writeLong(member.lastActiveMillis());
            }
            buffer.writeVarInt(payload.invites().size());
            for (InviteSummary invite : payload.invites()) {
                buffer.writeUUID(invite.inviteId());
                buffer.writeUUID(invite.groupId());
                buffer.writeUtf(invite.groupName(), 64);
            }
        }
    };

    @Override
    public Type<GroupStatePayload> type() {
        return TYPE;
    }

    public record MemberSummary(UUID playerUuid, String displayName, GroupRole role, int permissionMask, long lastActiveMillis) {
    }

    public record InviteSummary(UUID inviteId, UUID groupId, String groupName) {
    }
}
