package org.goldenforge;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import org.goldenforge.util.TpsMonitorManager;

public class ForgeEventHandler {

    @SubscribeEvent
    public static void serverStarted(FMLServerStartedEvent event) {
        new TpsMonitorManager();
    }
}
