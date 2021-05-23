package org.goldenforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;

public class GoldenForgeCommands {


    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        TicksPerSecondCommand.register(dispatcher);
        TimingsCommand.register(dispatcher);
        GoldenForgeCommand.register(dispatcher);
    }
}
