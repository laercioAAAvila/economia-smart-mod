package br.com.economiamod;

import br.com.economiamod.client.ClientMenuScreens;
import br.com.economiamod.client.ModKeyMappings;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.registry.ModBlockEntities;
import br.com.economiamod.registry.ModCreativeTabs;
import br.com.economiamod.registry.ModItems;
import br.com.economiamod.registry.ModMenus;
import br.com.economiamod.common.network.ModNetwork;
import br.com.economiamod.server.command.EconomiaCommands;
import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.event.CommercialBlockEvents;
import br.com.economiamod.server.event.EconomyServerEvents;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(EconomiaMod.MOD_ID)
public final class EconomiaMod {
    public static final String MOD_ID = "economia";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EconomiaMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModNetwork.register(modEventBus);
        EconomyServerConfig.register(modContainer);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientMenuScreens::registerScreens);
            modEventBus.addListener(ModKeyMappings::register);
        }
        NeoForge.EVENT_BUS.addListener(EconomyServerEvents::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(EconomyServerEvents::onServerStopped);
        NeoForge.EVENT_BUS.addListener(EconomyServerEvents::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(EconomiaCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(CommercialBlockEvents::onBlockPlaced);
        NeoForge.EVENT_BUS.addListener(CommercialBlockEvents::onBlockBroken);

        LOGGER.info("Economia Mod carregado. O sistema financeiro permanecera bloqueado ate a persistencia SQL estar ativa.");
    }
}
