package org.goldenforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.versions.forge.ForgeVersion;
import org.goldenforge.GoldenForge;
import org.goldenforge.GoldenForgeEntryPoint;
import org.goldenforge.util.TpsMonitorManager;

public class GoldenForgeCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        LiteralArgumentBuilder<CommandSource> mainCommand = Commands.literal("goldenforge");
        LiteralArgumentBuilder<CommandSource> versionSubCmd = Commands.literal("version");
        LiteralArgumentBuilder<CommandSource> tpsMonitorSubCmd = Commands.literal("tpsmonitor");

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

        mainCommand.then(versionSubCmd);
        mainCommand.then(tpsMonitorSubCmd);
        dispatcher.register(mainCommand);
    }
}
