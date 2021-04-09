package org.goldenforge;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.goldenforge.scheduler.GoldenScheduler;

public class GoldenForge {
    public static final Logger LOGGER = LogManager.getLogger("GoldenForge");

    private static String name = "GoldenForge";
    private static String version = "1.0.0";

    public static GoldenScheduler getScheduler() {
        return MinecraftServer.getServer().getScheduler();
    }

    public static String getName() {
        return name;
    }

    public static String getVersion() {
        return version;
    }

    public static final boolean isEnabled() {
        return true;
    }

    public static boolean isPrimaryThread() {
        return Thread.currentThread().equals(MinecraftServer.getServer().serverThread); /*|| Thread.currentThread().equals(net.minecraft.server.MinecraftServer.getServer().shutdownThread*/ // Paper - Fix issues with detecting main thread properly, the only time Watchdog will be used is during a crash shutdown which is a "try our best" scenario
    }
}
