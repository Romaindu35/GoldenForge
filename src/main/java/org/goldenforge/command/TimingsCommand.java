package org.goldenforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;

public class TimingsCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        dispatcher.register(Commands.literal("timings").executes(TimingsCommand::execute));

    }

    private static int execute(CommandContext<CommandSource> ctx) {
        String message = TextFormatting.RED + "Paper timings is not available in goldenforge. \n" +
                TextFormatting.GREEN + "we recommend you to use the spark mod. \n";

        String download = TextFormatting.GOLD + "Click here for download.";
        String downloadUrl = "https://ci.lucko.me/job/spark/lastSuccessfulBuild/artifact/spark-forge/build/libs/spark-forge.jar";

        if (ModList.get().isLoaded("spark")) {
            download = TextFormatting.GOLD + "Spark is already install, use it with /spark";
            downloadUrl = "https://spark.lucko.me/docs/Command-Usage";
        }

        String finalDownloadUrl = downloadUrl;
        IFormattableTextComponent downloadComponent = new StringTextComponent(download).withStyle((c) -> {
            return c.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, finalDownloadUrl));
        });
        IFormattableTextComponent component = new StringTextComponent(message).append(downloadComponent);

        ctx.getSource().sendSuccess(component, false);

        return 1;
    }
}
