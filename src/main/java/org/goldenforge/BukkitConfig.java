package org.goldenforge;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import net.minecraft.server.MinecraftServer;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.bspfsystems.yamlconfiguration.configuration.InvalidConfigurationException;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.spigotmc.SpigotConfig;
import org.spigotmc.WatchdogThread;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class BukkitConfig {

    private static File CONFIG_FILE;
    private static final String HEADER = "# This is the main configuration file for Bukkit.\n" +
            "# As you can see, there's actually not that much to configure without any plugins.\n" +
            "# For a reference for any variable inside this file, check out the Bukkit Wiki at\n" +
            "# https://www.spigotmc.org/go/bukkit-yml\n" +
            "# \n" +
            "# If you need help on this file, feel free to join us on irc or leave a message\n" +
            "# on the forums asking for advice.\n" +
            "# \n" +
            "# IRC: #spigot @ irc.spi.gt\n" +
            "#    (If this means nothing to you, just go to https://www.spigotmc.org/go/irc )\n" +
            "# Forums: https://www.spigotmc.org/\n" +
            "# Bug tracker: https://www.spigotmc.org/go/bugs";
    /*========================================================================*/
    public static YamlConfiguration config;
    static int version;
    private static boolean verbose;
    private static boolean fatalError;
    /*========================================================================*/

    public static void init(File configFile) {
        CONFIG_FILE = configFile;
        config = new YamlConfiguration();
        try {
            config.load(CONFIG_FILE);
        } catch (IOException ex) {
        } catch (InvalidConfigurationException ex) {
            GoldenForge.LOGGER.fatal("Could not load bukkit.yml, please correct your syntax errors", ex);
            throw Throwables.propagate(ex);
        }
        config.options().header(HEADER);
        config.options().copyDefaults(true);
        verbose = getBoolean("verbose", false);


        version = getInt("config-version", 20);
        set("config-version", 20);
        readConfig(BukkitConfig.class, null);
    }

    protected static void logError(String s) {
        GoldenForge.LOGGER.fatal(s);
    }

    protected static void fatal(String s) {
        fatalError = true;
        throw new RuntimeException("Fatal paper.yml config error: " + s);
    }

    protected static void log(String s) {
        if (verbose) {
            GoldenForge.LOGGER.info(s);
        }
    }

    static void readConfig(Class<?> clazz, Object instance) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (InvocationTargetException ex) {
                        throw Throwables.propagate(ex.getCause());
                    } catch (Exception ex) {
                        GoldenForge.LOGGER.error("Error invoking " + method, ex);
                    }
                }
            }
        }

        try {
            config.save(CONFIG_FILE);
        } catch (IOException ex) {
            GoldenForge.LOGGER.error( "Could not save " + CONFIG_FILE, ex);
        }
    }

    private static void set(String path, Object val) {
        config.set(path, val);
    }

    private static boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, config.getBoolean(path));
    }

    private static double getDouble(String path, double def) {
        config.addDefault(path, def);
        return config.getDouble(path, config.getDouble(path));
    }

    private static float getFloat(String path, float def) {
        // TODO: Figure out why getFloat() always returns the default value.
        return (float) getDouble(path, (double) def);
    }

    private static int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInt(path, config.getInt(path));
    }

    private static <T> List getList(String path, T def) {
        config.addDefault(path, def);
        return (List<T>) config.getList(path, config.getList(path));
    }

    private static String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString(path, config.getString(path));
    }

    public static int ticksPerAnimalSpawn = 400;
    private static void ticksPerAnimalSpawn() {
        ticksPerAnimalSpawn = getInt("ticks-per.animal-spawns", 400);
    }

    public static int ticksPerMonsterSpawn = 1;
    private static void ticksPerMonsterSpawn() {
        ticksPerMonsterSpawn = getInt("ticks-per.monster-spawns", 1);
    }

    public static int ticksPerWaterSpawn = 1;
    private static void ticksPerWaterSpawn() {
        ticksPerWaterSpawn = getInt("ticks-per.water-spawns", 1);
    }

    public static int ticksPerWaterAmbientSpawn = 1;
    private static void ticksPerWaterAmbientSpawn() {
        ticksPerWaterAmbientSpawn = getInt("ticks-per.water-ambient-spawns", 1);
    }

    public static int getTicksPerAmbientSpawn = 1;
    private static void getTicksPerAmbientSpawn() {
        getTicksPerAmbientSpawn = getInt("ticks-per.ambient-spawns", 1);
    }


    public static int monstersSpawnLimits = 70;
    public static void monstersSpawnLimits() {
        monstersSpawnLimits = getInt("spawn-limits.monsters", 70);
    }

    public static int animalsSpawnLimits = 10;
    public static void animalsSpawnLimits() {
        animalsSpawnLimits = getInt("spawn-limits.animals", 10);
    }

    public static int waterAnimalsSpawnLimits = 15;
    public static void waterAnimalsSpawnLimits() {
        waterAnimalsSpawnLimits = getInt("spawn-limits.water-animals", 15);
    }

    public static int waterAmbientSpawnLimits = 20;
    public static void waterAmbientSpawnLimits() {
        waterAmbientSpawnLimits = getInt("spawn-limits.water-ambient", 20);
    }

    public static int ambientSpawnLimits = 15;
    public static void ambientSpawnLimits() {
        ambientSpawnLimits = getInt("spawn-limits.ambient", 15);
    }


}
