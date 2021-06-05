package org.spigotmc;

import net.minecraft.server.MinecraftServer;
import org.goldenforge.GoldenForge;

public class AsyncCatcher
{

    public static boolean enabled = true;
    public static boolean shuttingDown = false; // Paper

    public static void catchOp(String reason)
    {
        if ( ( enabled || com.tuinity.tuinity.util.TickThread.STRICT_THREAD_CHECKS ) && !GoldenForge.isPrimaryThread() ) // Tuinity
        {
            MinecraftServer.LOGGER.fatal("Thread " + Thread.currentThread().getName() + " failed thread check for reason: Asynchronous " + reason, new Throwable()); // Tuinity - not all exceptions are printed
            throw new IllegalStateException( "Asynchronous " + reason + "!" );
        }
    }
}
