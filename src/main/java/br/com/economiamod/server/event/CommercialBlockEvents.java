package br.com.economiamod.server.event;

import br.com.economiamod.server.commercial.CommercialBlockLifecycleService;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class CommercialBlockEvents {
    private static final CommercialBlockLifecycleService LIFECYCLE_SERVICE = new CommercialBlockLifecycleService();

    private CommercialBlockEvents() {
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        LIFECYCLE_SERVICE.onBlockPlaced(event);
    }

    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        LIFECYCLE_SERVICE.onBlockBroken(event);
    }
}
