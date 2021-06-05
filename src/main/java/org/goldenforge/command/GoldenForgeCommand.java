package org.goldenforge.command;

import com.destroystokyo.paper.io.chunk.ChunkTaskManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.versions.forge.ForgeVersion;
import org.bukkit.ChatColor;
import org.goldenforge.GoldenForge;
import org.goldenforge.GoldenForgeEntryPoint;
import org.goldenforge.util.TpsMonitorManager;

import java.util.ArrayList;
import java.util.List;

public class GoldenForgeCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        LiteralArgumentBuilder<CommandSource> mainCommand = Commands.literal("goldenforge");
        LiteralArgumentBuilder<CommandSource> versionSubCmd = Commands.literal("version");
        LiteralArgumentBuilder<CommandSource> tpsMonitorSubCmd = Commands.literal("tpsmonitor");
        LiteralArgumentBuilder<CommandSource> dumpwaitingCmd = Commands.literal("dumpwaiting");
        LiteralArgumentBuilder<CommandSource> chunkinfoCmd = Commands.literal("chunkinfo");

        versionSubCmd.executes((ctx) -> {
            String gfVersion = String.format(TextFormatting.GOLD + "This server is using GoldenForge build number %s \n", GoldenForge.getVersion());
            String forgeVersion = String.format(TextFormatting.GOLD + "GoldenForge run with forge version %s", ForgeVersion.getVersion());

            IFormattableTextComponent message = new StringTextComponent(gfVersion).append(forgeVersion);

            ctx.getSource().sendSuccess(message, false);
            return 1;
        });

        tpsMonitorSubCmd.executes((ctx) -> {

            if (ctx.getSource().getEntity() instanceof ServerPlayerEntity) {
                TpsMonitorManager.get().togglePlayer((ServerPlayerEntity) ctx.getSource().getEntity());
            }
           return 1;
        });

        dumpwaitingCmd.executes((ctx) -> {
            ChunkTaskManager.dumpAllChunkLoadInfo();
           return 1;
        });

        chunkinfoCmd.executes((ctx) -> {

            Iterable<ServerWorld> worlds = MinecraftServer.getServer().getAllLevels();

            int accumulatedTotal = 0;
            int accumulatedInactive = 0;
            int accumulatedBorder = 0;
            int accumulatedTicking = 0;
            int accumulatedEntityTicking = 0;

            for (ServerWorld world : worlds) {

                int total = 0;
                int inactive = 0;
                int border = 0;
                int ticking = 0;
                int entityTicking = 0;

                for (ChunkHolder chunk : world.getChunkSource().chunkMap.updatingChunks.getUpdatingMap().values()) { // Tuinity - change updating chunks map
                    if (chunk.getFullChunkIfCached() == null) {
                        continue;
                    }

                    ++total;

                    ChunkHolder.LocationType state = ChunkHolder.getFullChunkStatus(chunk.getTicketLevel());

                    switch (state) {
                        case INACCESSIBLE:
                            ++inactive;
                            continue;
                        case BORDER:
                            ++border;
                            continue;
                        case TICKING:
                            ++ticking;
                            continue;
                        case ENTITY_TICKING:
                            ++entityTicking;
                            continue;
                    }
                }

                accumulatedTotal += total;
                accumulatedInactive += inactive;
                accumulatedBorder += border;
                accumulatedTicking += ticking;
                accumulatedEntityTicking += entityTicking;

                ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.BLUE + "Chunks in " + TextFormatting.GREEN + world.dimension().location() + TextFormatting.DARK_AQUA + ":"), false);
                ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.BLUE + "Total: " + TextFormatting.DARK_AQUA + total + TextFormatting.BLUE + " Inactive: " + TextFormatting.DARK_AQUA
                        + inactive + TextFormatting.BLUE + " Border: " + TextFormatting.DARK_AQUA + border + TextFormatting.BLUE + " Ticking: "
                        + TextFormatting.DARK_AQUA + ticking + TextFormatting.BLUE + " Entity: " + TextFormatting.DARK_AQUA + entityTicking), false);

            }
//            if (worlds > 1) {
//                sender.sendMessage(ChatColor.BLUE + "Chunks in " + ChatColor.GREEN + "all listed worlds" + ChatColor.DARK_AQUA + ":");
//                sender.sendMessage(ChatColor.BLUE + "Total: " + ChatColor.DARK_AQUA + accumulatedTotal + ChatColor.BLUE + " Inactive: " + ChatColor.DARK_AQUA
//                        + accumulatedInactive + ChatColor.BLUE + " Border: " + ChatColor.DARK_AQUA + accumulatedBorder + ChatColor.BLUE + " Ticking: "
//                        + ChatColor.DARK_AQUA + accumulatedTicking + ChatColor.BLUE + " Entity: " + ChatColor.DARK_AQUA + accumulatedEntityTicking);
//            }

           return 1;
        });

        mainCommand.then(versionSubCmd);
        mainCommand.then(tpsMonitorSubCmd);
        mainCommand.then(chunkinfoCmd);
        mainCommand.then(dumpwaitingCmd);
        dispatcher.register(mainCommand);
    }
}
