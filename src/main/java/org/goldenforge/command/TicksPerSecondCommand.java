package org.goldenforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.StringUtils;

public class TicksPerSecondCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        dispatcher.register(Commands.literal("tps").executes(TicksPerSecondCommand::executeTPS));
        dispatcher.register(Commands.literal("tpsmemory").executes(TicksPerSecondCommand::executeMEMORY));

    }

    private static int executeTPS(CommandContext<CommandSource> ctx) {

        CommandSource sender = ctx.getSource();
        // Paper start - Further improve tick handling
        double[] tps = MinecraftServer.getServer().getTPS();
        String[] tpsAvg = new String[tps.length];

        for ( int i = 0; i < tps.length; i++) {
            tpsAvg[i] = format( tps[i] );
        }
        sender.sendSuccess(new StringTextComponent(TextFormatting.GOLD + "TPS from last 5s, 1m, 5m, 15m: " + StringUtils.join(tpsAvg, ", ")), false);
        // Paper end

        return 1;
    }

    private static int executeMEMORY(CommandContext<CommandSource> ctx) {

        CommandSource sender = ctx.getSource();
        // Paper start - Further improve tick handling
        sender.sendSuccess(new StringTextComponent(TextFormatting.GOLD + "Current Memory Usage: " + TextFormatting.GREEN + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)) + "/" + (Runtime.getRuntime().totalMemory() / (1024 * 1024)) + " mb (Max: " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " mb)"), false);
        if (!hasShownMemoryWarning) {
            sender.sendSuccess(new StringTextComponent(TextFormatting.RED + "Warning: " + TextFormatting.GOLD + " Memory usage on modern garbage collectors is not a stable value and it is perfectly normal to see it reach max. Please do not pay it much attention."), false);
            hasShownMemoryWarning = true;
        }
        // Paper end

        return 1;
    }

    private static boolean hasShownMemoryWarning; // Paper
    private static String format(double tps) // Paper - Made static
    {
        return ( ( tps > 18.0 ) ? TextFormatting.GREEN : ( tps > 16.0 ) ? TextFormatting.YELLOW : TextFormatting.RED ).toString()
                + ( ( tps > 21.0 ) ? "*" : "" ) + Math.min( Math.round( tps * 100.0 ) / 100.0, 20.0 ); // Paper - only print * at 21, we commonly peak to 20.02 as the tick sleep is not accurate enough, stop the noise
    }
}
