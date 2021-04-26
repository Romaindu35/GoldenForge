package org.goldenforge.util;


import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TpsMonitorManager {
    private List<ServerPlayerEntity> activesTpsMonitors = new ArrayList<>();
    private static TpsMonitorManager INSTANCE;
    private DecimalFormat FORMATER = new DecimalFormat("#.##");

    public TpsMonitorManager() {
        INSTANCE = this;
        MinecraftServer.getServer().addTickable(this::tick);
    }


    public void togglePlayer(ServerPlayerEntity player) {
        if (activesTpsMonitors.contains(player)) {
            activesTpsMonitors.remove(player);
            player.sendMessage(new StringTextComponent(TextFormatting.GOLD + "Disabling TpsMonitor"), Util.NIL_UUID);

        } else {
            activesTpsMonitors.add(player);
            player.sendMessage(new StringTextComponent(TextFormatting.GOLD + "Enabling TpsMonitor"), Util.NIL_UUID);

        }
    }

    public void tick() {

        double tps = MinecraftServer.getServer().getTPS()[0];
        if (tps > 20.0D) {
            tps = 20.0D;
        } else if (tps < 0.0D) {
            tps = 0.0D;
        }

        String tpsColor;
        if (tps >= 18) {
            tpsColor = "§2";
        } else if (tps >= 15) {
            tpsColor = "§e";
        } else {
            tpsColor = "§4";
        }

        double mspt = MinecraftServer.getServer().getAverageTickTime();
        String msptColor;
        if (mspt < 40) {
            msptColor = "§2";
        } else if (mspt < 50) {
            msptColor = "§e";
        } else {
            msptColor = "§4";
        }

        STitlePacket packet = new STitlePacket(STitlePacket.Type.ACTIONBAR, new StringTextComponent(TextFormatting.DARK_GRAY + "TPS: " + tpsColor + FORMATER.format(tps) + TextFormatting.DARK_GRAY + " MSPT: " + msptColor + FORMATER.format(mspt)));

        for (ServerPlayerEntity player : activesTpsMonitors) {
            player.connection.send(packet);
        }
    }

    public static TpsMonitorManager get() {
        return INSTANCE;
    }
}
