package br.com.economiamod.server.command;

import br.com.economiamod.common.menu.BankCounterMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BankCounterAccessService {
    public boolean hasValidAccess(CommandSourceStack source, ServerPlayer player) {
        if (player.containerMenu instanceof BankCounterMenu menu && menu.stillValid(player)) {
            return true;
        }
        source.sendFailure(Component.translatable("commands.economia.bank_counter.required"));
        return false;
    }
}
