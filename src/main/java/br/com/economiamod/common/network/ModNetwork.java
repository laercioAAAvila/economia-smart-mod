package br.com.economiamod.common.network;

import br.com.economiamod.server.network.SecureAccountPayloadHandler;
import br.com.economiamod.server.network.BankCounterActionPayloadHandler;
import br.com.economiamod.server.network.MailActionPayloadHandler;
import br.com.economiamod.server.network.ShopActionPayloadHandler;
import br.com.economiamod.server.network.ShopReferencePayloadHandler;
import br.com.economiamod.server.network.MapActionPayloadHandler;
import br.com.economiamod.server.network.GroupActionPayloadHandler;
import br.com.economiamod.server.network.ChatChannelPayloadHandler;
import br.com.economiamod.server.network.ClaimAnchorActionPayloadHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ModNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SecureAccountPayload.TYPE, SecureAccountPayload.STREAM_CODEC, SecureAccountPayloadHandler::handle);
        registrar.playToServer(ShopActionPayload.TYPE, ShopActionPayload.STREAM_CODEC, ShopActionPayloadHandler::handle);
        registrar.playToServer(ShopReferencePayload.TYPE, ShopReferencePayload.STREAM_CODEC, ShopReferencePayloadHandler::handle);
        registrar.playToServer(BankCounterActionPayload.TYPE, BankCounterActionPayload.STREAM_CODEC, BankCounterActionPayloadHandler::handle);
        registrar.playToServer(MailActionPayload.TYPE, MailActionPayload.STREAM_CODEC, MailActionPayloadHandler::handle);
        registrar.playToServer(MapActionPayload.TYPE, MapActionPayload.STREAM_CODEC, MapActionPayloadHandler::handle);
        registrar.playToServer(GroupActionPayload.TYPE, GroupActionPayload.STREAM_CODEC, GroupActionPayloadHandler::handle);
        registrar.playToServer(ChatChannelPayload.TYPE, ChatChannelPayload.STREAM_CODEC, ChatChannelPayloadHandler::handle);
        registrar.playToServer(ShareLocationPayload.TYPE, ShareLocationPayload.STREAM_CODEC, ChatChannelPayloadHandler::handleShare);
        registrar.playToServer(ClaimAnchorActionPayload.TYPE, ClaimAnchorActionPayload.STREAM_CODEC, ClaimAnchorActionPayloadHandler::handle);
        registrar.playToClient(AtmSessionStatePayload.TYPE, AtmSessionStatePayload.STREAM_CODEC, ClientboundAtmSessionStateHandler::handle);
        registrar.playToClient(AtmAccountSummaryPayload.TYPE, AtmAccountSummaryPayload.STREAM_CODEC, ClientboundAtmAccountSummaryHandler::handle);
        registrar.playToClient(AtmCardsPayload.TYPE, AtmCardsPayload.STREAM_CODEC, ClientboundAtmCardsHandler::handle);
        registrar.playToClient(AtmOperationHistoryPayload.TYPE, AtmOperationHistoryPayload.STREAM_CODEC, ClientboundAtmOperationHistoryHandler::handle);
        registrar.playToClient(MailStatePayload.TYPE, MailStatePayload.STREAM_CODEC, ClientboundMailStateHandler::handle);
        registrar.playToClient(MapDataPayload.TYPE, MapDataPayload.STREAM_CODEC, ClientboundMapDataHandler::handle);
        registrar.playToClient(GroupStatePayload.TYPE, GroupStatePayload.STREAM_CODEC, ClientboundGroupStateHandler::handle);
        registrar.playToClient(OpenSharedLocationPayload.TYPE, OpenSharedLocationPayload.STREAM_CODEC, ClientboundOpenSharedLocationHandler::handle);
    }
}
