package br.com.economiamod.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import br.com.economiamod.server.location.PlayerLocationRepository;
import br.com.economiamod.server.location.PlayerLocation;
import br.com.economiamod.common.network.OpenSharedLocationPayload;
import java.util.UUID;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EconomiaCommands {
    private static final AdminCommandHandlers ADMIN_COMMANDS = new AdminCommandHandlers();

    private EconomiaCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("economia")
                .then(Commands.literal("_localizacao")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(context -> openSharedLocation(
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("conta")
                                .then(Commands.literal("deletar")
                                        .then(Commands.argument("usuario", StringArgumentType.word())
                                                .then(Commands.literal("confirmar")
                                                        .executes(context -> ADMIN_COMMANDS.deleteAccount(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "usuario")
                                                        ))))))
                        .then(Commands.literal("bloco")
                                .then(Commands.literal("dar")
                                        .then(Commands.literal("caixa-eletronico")
                                                .executes(context -> ADMIN_COMMANDS.giveAtm(context.getSource(), context.getSource().getPlayerOrException()))
                                                .then(Commands.argument("jogador", EntityArgument.player())
                                                        .executes(context -> ADMIN_COMMANDS.giveAtm(context.getSource(), EntityArgument.getPlayer(context, "jogador")))))
                                        .then(Commands.literal("loja-venda")
                                                .executes(context -> ADMIN_COMMANDS.giveSellShop(context.getSource(), context.getSource().getPlayerOrException()))
                                                .then(Commands.argument("jogador", EntityArgument.player())
                                                        .executes(context -> ADMIN_COMMANDS.giveSellShop(context.getSource(), EntityArgument.getPlayer(context, "jogador")))))
                                        .then(Commands.literal("loja-compra")
                                                .executes(context -> ADMIN_COMMANDS.giveBuyShop(context.getSource(), context.getSource().getPlayerOrException()))
                                                .then(Commands.argument("jogador", EntityArgument.player())
                                                        .executes(context -> ADMIN_COMMANDS.giveBuyShop(context.getSource(), EntityArgument.getPlayer(context, "jogador")))))
                                        .then(Commands.literal("bancada-banco")
                                                .executes(context -> ADMIN_COMMANDS.giveBankCounter(context.getSource(), context.getSource().getPlayerOrException()))
                                                .then(Commands.argument("jogador", EntityArgument.player())
                                                        .executes(context -> ADMIN_COMMANDS.giveBankCounter(context.getSource(), EntityArgument.getPlayer(context, "jogador")))))))
                        .then(Commands.literal("juros")
                                .then(Commands.literal("processar")
                                        .then(Commands.argument("data", StringArgumentType.word())
                                                .executes(context -> ADMIN_COMMANDS.processInterest(context.getSource(), StringArgumentType.getString(context, "data"))))))
                        .then(Commands.literal("banco")
                                .then(Commands.literal("resetar")
                                        .then(Commands.literal("confirmar")
                                                .executes(context -> ADMIN_COMMANDS.resetDatabase(context.getSource())))))
                        .then(Commands.literal("reload")
                                .executes(context -> ADMIN_COMMANDS.unavailable(context.getSource())))));
    }

    private static int openSharedLocation(net.minecraft.server.level.ServerPlayer player, String rawId) {
        try {
            PlayerLocation location = new PlayerLocationRepository().find(UUID.fromString(rawId)).orElse(null);
            if (location == null) return 0;
            PacketDistributor.sendToPlayer(player, new OpenSharedLocationPayload(
                    location.name(), location.dimension(), location.x(), location.y(), location.z()));
            return 1;
        } catch (Exception exception) {
            return 0;
        }
    }
}
