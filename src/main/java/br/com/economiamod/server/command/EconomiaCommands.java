package br.com.economiamod.server.command;

import br.com.economiamod.common.card.CardType;
import br.com.economiamod.server.command.BankGoldCommandHandlers.GoldRedeemUnit;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class EconomiaCommands {
    private static final AdminCommandHandlers ADMIN_COMMANDS = new AdminCommandHandlers();
    private static final AtmCommandHandlers ATM_COMMANDS = new AtmCommandHandlers();
    private static final BankGoldCommandHandlers BANK_GOLD_COMMANDS = new BankGoldCommandHandlers();
    private static final GeneralCommandHandlers GENERAL_COMMANDS = new GeneralCommandHandlers();

    private EconomiaCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("economia")
                .then(Commands.literal("status")
                        .executes(context -> GENERAL_COMMANDS.status(context.getSource())))
                .then(Commands.literal("saldo")
                        .executes(context -> GENERAL_COMMANDS.balance(context.getSource())))
                .then(Commands.literal("logout")
                        .executes(context -> GENERAL_COMMANDS.logout(context.getSource())))
                .then(Commands.literal("login")
                        .then(Commands.argument("usuario", StringArgumentType.word())
                                .executes(context -> GENERAL_COMMANDS.secureUiPending(context.getSource()))))
                .then(Commands.literal("conta")
                        .then(Commands.literal("criar")
                                .then(Commands.argument("usuario", StringArgumentType.word())
                                        .executes(context -> GENERAL_COMMANDS.secureUiPending(context.getSource()))))
                        .then(Commands.literal("alterar-senha")
                                .executes(context -> GENERAL_COMMANDS.secureUiPending(context.getSource()))))
                .then(Commands.literal("atm")
                        .then(Commands.literal("depositar-tudo")
                                .executes(context -> ATM_COMMANDS.depositAll(context.getSource())))
                        .then(Commands.literal("sacar")
                                .then(Commands.argument("valor", IntegerArgumentType.integer(1))
                                        .executes(context -> ATM_COMMANDS.withdraw(context.getSource(), IntegerArgumentType.getInteger(context, "valor")))))
                        .then(Commands.literal("cartao")
                                .then(Commands.literal("debito")
                                        .executes(context -> ATM_COMMANDS.issueCard(context.getSource(), CardType.DEBIT, 0L)))
                                .then(Commands.literal("credito")
                                        .then(Commands.argument("limite", IntegerArgumentType.integer(0))
                                                .executes(context -> ATM_COMMANDS.issueCard(context.getSource(), CardType.CREDIT, IntegerArgumentType.getInteger(context, "limite")))))
                                .then(Commands.literal("debito_credito")
                                        .then(Commands.argument("limite", IntegerArgumentType.integer(0))
                                                .executes(context -> ATM_COMMANDS.issueCard(context.getSource(), CardType.DEBIT_CREDIT, IntegerArgumentType.getInteger(context, "limite")))))))
                .then(Commands.literal("banco")
                        .then(Commands.literal("ouro")
                                .then(Commands.literal("emitir")
                                        .executes(context -> BANK_GOLD_COMMANDS.mintSelectedGold(context.getSource())))
                                .then(Commands.literal("resgatar")
                                        .then(Commands.argument("unidades", IntegerArgumentType.integer(1))
                                                .executes(context -> BANK_GOLD_COMMANDS.redeemGold(context.getSource(), IntegerArgumentType.getInteger(context, "unidades")))
                                                .then(Commands.literal("pepita")
                                                        .executes(context -> BANK_GOLD_COMMANDS.redeemGold(context.getSource(), IntegerArgumentType.getInteger(context, "unidades"), GoldRedeemUnit.NUGGET)))
                                                .then(Commands.literal("barra")
                                                        .executes(context -> BANK_GOLD_COMMANDS.redeemGold(context.getSource(), IntegerArgumentType.getInteger(context, "unidades"), GoldRedeemUnit.INGOT)))
                                                .then(Commands.literal("bloco")
                                                        .executes(context -> BANK_GOLD_COMMANDS.redeemGold(context.getSource(), IntegerArgumentType.getInteger(context, "unidades"), GoldRedeemUnit.BLOCK)))))))
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
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
                                .executes(context -> GENERAL_COMMANDS.unavailable(context.getSource())))));
    }
}
