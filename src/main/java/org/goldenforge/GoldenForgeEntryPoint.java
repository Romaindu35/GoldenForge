package org.goldenforge;

import net.minecraftforge.fml.ModList;
import org.goldenforge.metrics.Metrics;

public class GoldenForgeEntryPoint {

    public GoldenForgeEntryPoint() {

        int pluginId = 10746; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(pluginId);

        // Optional: Add custom charts
        //metrics.addCustomChart(new Metrics.SingleLineChart("Mods counts", () -> ModList.get().getMods().size()));

    }
}
