package org.goldenforge;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import net.minecraftforge.fml.loading.FMLPaths;
import org.bspfsystems.yamlconfiguration.configuration.InvalidConfigurationException;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.spigotmc.SpigotConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class BukkitConfiguration {

    private static File CONFIG_FILE;
    public static YamlConfiguration config;
    public static void init(File configFile)
    {
        CONFIG_FILE = configFile;
        config = new YamlConfiguration();
        try
        {
            config.load( CONFIG_FILE );
        } catch ( IOException ex )
        {
        } catch ( InvalidConfigurationException ex )
        {
            GoldenForge.LOGGER.fatal("Could not load spigot.yml, please correct your syntax errors", ex );
            throw Throwables.propagate( ex );
        }

        config.options().copyDefaults( true );
        config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(BukkitConfiguration.class.getClassLoader().getResourceAsStream("configurations/bukkit.yml"), Charsets.UTF_8)));

    }

    public static int getTicksPerAnimalSpawns() {
        return config.getInt("ticks-per.animal-spawns");
    }

    public static int getTicksPerMonsterSpawns() {
        return config.getInt("ticks-per.monster-spawns");
    }

    public static int getTicksPerWaterSpawns() {
        return config.getInt("ticks-per.water-spawns");
    }

    public static int getTicksPerWaterAmbientSpawns() {
        return config.getInt("ticks-per.water-ambient-spawns");
    }

    public static int getTicksPerAmbientSpawns() {
        return config.getInt("ticks-per.ambient-spawns");
    }


    public static int getMonsterSpawn() {
        return config.getInt("spawn-limits.monsters");
    }

    public static int getAnimalSpawn() {
        return config.getInt("spawn-limits.animals");
    }

    public static int getwaterAnimalSpawnSpawn() {
        return config.getInt("spawn-limits.water-animals");
    }

    public static int getwaterAmbientSpawn() {
        return config.getInt("spawn-limits.water-ambient");
    }

    public static int getambienSpawn() {
        return config.getInt("spawn-limits.ambient");
    }

}
