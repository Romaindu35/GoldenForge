package com.tuinity.tuinity.config;

import com.destroystokyo.paper.util.SneakyThrow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.TicketType;
import org.apache.logging.log4j.Level;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.goldenforge.GoldenForge;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class TuinityConfig {

    public static final String CONFIG_HEADER = "Configuration file for Tuinity.";
    public static final int CURRENT_CONFIG_VERSION = 2;

    private static final Object[] EMPTY = new Object[0];

    private static File configFile;
    public static YamlConfiguration config;
    private static int configVersion;
    public static boolean createWorldSections = true;

    public static void init(final File file) {
        // TODO remove this in the future...
        final File tuinityConfig = new File(file.getParent(), "tuinity.yml");
        if (!tuinityConfig.exists()) {
            final File oldConfig = new File(file.getParent(), "concrete.yml");
            oldConfig.renameTo(tuinityConfig);
        }
        TuinityConfig.configFile = file;
        final YamlConfiguration config = new YamlConfiguration();
        config.options().header(CONFIG_HEADER);
        config.options().copyDefaults(true);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (final Exception ex) {
                GoldenForge.LOGGER.log(Level.ERROR, "Failure to create tuinity config", ex);
            }
        } else {
            try {
                config.load(file);
            } catch (final Exception ex) {
                GoldenForge.LOGGER.log(Level.ERROR, "Failure to load tuinity config", ex);
                SneakyThrow.sneaky(ex); /* Rethrow, this is critical */
                throw new RuntimeException(ex); // unreachable
            }
        }

        TuinityConfig.load(config);
    }

    public static void load(final YamlConfiguration config) {
        TuinityConfig.config = config;
        TuinityConfig.configVersion = TuinityConfig.getInt("config-version-please-do-not-modify-me", CURRENT_CONFIG_VERSION);
        TuinityConfig.set("config-version-please-do-not-modify-me", CURRENT_CONFIG_VERSION);

        for (final Method method : TuinityConfig.class.getDeclaredMethods()) {
            if (method.getReturnType() != void.class || method.getParameterCount() != 0 ||
                    !Modifier.isPrivate(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(null, EMPTY);
            } catch (final Exception ex) {
                SneakyThrow.sneaky(ex); /* Rethrow, this is critical */
                throw new RuntimeException(ex); // unreachable
            }
        }

        /* We re-save to add new options */
        try {
            config.save(TuinityConfig.configFile);
        } catch (final Exception ex) {
            GoldenForge.LOGGER.log(Level.ERROR, "Unable to save tuinity config", ex);
        }
    }

    static void set(final String path, final Object value) {
        TuinityConfig.config.set(path, value);
    }

    static boolean getBoolean(final String path, final boolean dfl) {
        TuinityConfig.config.addDefault(path, Boolean.valueOf(dfl));
        return TuinityConfig.config.getBoolean(path, dfl);
    }

    static int getInt(final String path, final int dfl) {
        TuinityConfig.config.addDefault(path, Integer.valueOf(dfl));
        return TuinityConfig.config.getInt(path, dfl);
    }

    static long getLong(final String path, final long dfl) {
        TuinityConfig.config.addDefault(path, Long.valueOf(dfl));
        return TuinityConfig.config.getLong(path, dfl);
    }

    static double getDouble(final String path, final double dfl) {
        TuinityConfig.config.addDefault(path, Double.valueOf(dfl));
        return TuinityConfig.config.getDouble(path, dfl);
    }

    static String getString(final String path, final String dfl) {
        TuinityConfig.config.addDefault(path, dfl);
        return TuinityConfig.config.getString(path, dfl);
    }

    public static boolean tickWorldsInParallel;

    /**
     * if tickWorldsInParallel == true, then this value is used as a default only for worlds
     */
    public static int tickThreads;

    /*
    private static void worldticking() {
        tickWorldsInParallel = TuinityConfig.getBoolean("tick-worlds-in-parallel", false);
        tickThreads = TuinityConfig.getInt("server-tick-threads", 1); // will be 4 in the future
    }*/

    public static int delayChunkUnloadsBy;

    private static void delayChunkUnloadsBy() {
        delayChunkUnloadsBy = TuinityConfig.getInt("delay-chunkunloads-by", 5) * 20;
        if (delayChunkUnloadsBy >= 0) {
            TicketType.DELAYED_UNLOAD.timeout = delayChunkUnloadsBy;
        }
    }

    public static boolean lagCompensateBlockBreaking;

    private static void lagCompensateBlockBreaking() {
        lagCompensateBlockBreaking = TuinityConfig.getBoolean("lag-compensate-block-breaking", true);
    }

    public static final class PacketLimit {
        public final double packetLimitInterval;
        public final double maxPacketRate;
        public final ViolateAction violateAction;

        public PacketLimit(final double packetLimitInterval, final double maxPacketRate, final ViolateAction violateAction) {
            this.packetLimitInterval = packetLimitInterval;
            this.maxPacketRate = maxPacketRate;
            this.violateAction = violateAction;
        }

        public static enum ViolateAction {
            KICK, DROP;
        }
    }

    public static boolean useNewLightEngine;

    private static void useNewLightEngine() {
        useNewLightEngine = TuinityConfig.getBoolean("use-new-light-engine", true);
    }

    public static boolean sendFullPosForHardCollidingEntities;

    private static void sendFullPosForHardCollidingEntities() {
        sendFullPosForHardCollidingEntities = TuinityConfig.getBoolean("send-full-pos-for-hard-colliding-entities", true);
    }

    public static int playerMinChunkLoadRadius;
    public static double playerMaxConcurrentChunkSends;
    public static double playerMaxConcurrentChunkLoads;
    public static boolean playerAutoConfigureSendViewDistance;
    public static boolean enableMC162253Workaround;
    public static double playerTargetChunkSendRate;
    public static boolean playerFrustumPrioritisation;

    private static void newPlayerChunkManagement() {
        playerMinChunkLoadRadius = TuinityConfig.getInt("player-chunks.min-load-radius", 2);
        playerMaxConcurrentChunkSends = TuinityConfig.getDouble("player-chunks.max-concurrent-sends", 5.0);
        playerMaxConcurrentChunkLoads = TuinityConfig.getDouble("player-chunks.max-concurrent-loads", -6.0);
        playerAutoConfigureSendViewDistance = TuinityConfig.getBoolean("player-chunks.autoconfig-send-distance", true);
        // this costs server bandwidth. latest phosphor or starlight on the client fixes mc162253 anyways.
        enableMC162253Workaround = TuinityConfig.getBoolean("player-chunks.enable-mc162253-workaround", true);
        playerTargetChunkSendRate = TuinityConfig.getDouble("player-chunks.target-chunk-send-rate", -35.0);
        playerFrustumPrioritisation = TuinityConfig.getBoolean("player-chunks.enable-frustum-priority", false);
    }

    public static final class WorldConfig {

        public final String worldName;
        public String configPath;
        ConfigurationSection worldDefaults;

        public WorldConfig(final String worldName) {
            this.worldName = worldName;
            this.init();
        }

        public void init() {
            this.worldDefaults = TuinityConfig.config.getConfigurationSection("world-settings.default");
            if (this.worldDefaults == null) {
                this.worldDefaults = TuinityConfig.config.createSection("world-settings.default");
            }

            String worldSectionPath = TuinityConfig.configVersion < 1 ? this.worldName : "world-settings.".concat(this.worldName);
            ConfigurationSection section = TuinityConfig.config.getConfigurationSection(worldSectionPath);
            this.configPath = worldSectionPath;
            if (TuinityConfig.createWorldSections) {
                if (section == null) {
                    section = TuinityConfig.config.createSection(worldSectionPath);
                }
                TuinityConfig.config.set(worldSectionPath, section);
            }

            this.load();
        }

        public void load() {
            for (final Method method : WorldConfig.class.getDeclaredMethods()) {
                if (method.getReturnType() != void.class || method.getParameterCount() != 0 ||
                        !Modifier.isPrivate(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    method.invoke(this, EMPTY);
                } catch (final Exception ex) {
                    SneakyThrow.sneaky(ex); /* Rethrow, this is critical */
                    throw new RuntimeException(ex); // unreachable
                }
            }

            if (TuinityConfig.configVersion < 1) {
                ConfigurationSection oldSection = TuinityConfig.config.getConfigurationSection(this.worldName);
                TuinityConfig.config.set("world-settings.".concat(this.worldName), oldSection);
                TuinityConfig.config.set(this.worldName, null);
            }

            /* We re-save to add new options */
            try {
                TuinityConfig.config.save(TuinityConfig.configFile);
            } catch (final Exception ex) {
                GoldenForge.LOGGER.log(Level.ERROR, "Unable to save tuinity config", ex);
            }
        }

        /**
         * update world defaults for the specified path, but also sets this world's config value for the path
         * if it exists
         */
        void set(final String path, final Object val) {
            final ConfigurationSection config = TuinityConfig.config.getConfigurationSection(this.configPath);
            this.worldDefaults.set(path, val);
            if (config != null && config.get(path) != null) {
                config.set(path, val);
            }
        }

        boolean getBoolean(final String path, final boolean dfl) {
            final ConfigurationSection config = TuinityConfig.config.getConfigurationSection(this.configPath);
            this.worldDefaults.addDefault(path, Boolean.valueOf(dfl));
            if (TuinityConfig.configVersion < 1) {
                if (config != null && config.getBoolean(path) == dfl) {
                    config.set(path, null);
                }
            }
            return config == null ? this.worldDefaults.getBoolean(path) : config.getBoolean(path, this.worldDefaults.getBoolean(path));
        }

        int getInt(final String path, final int dfl) {
            final ConfigurationSection config = TuinityConfig.config.getConfigurationSection(this.configPath);
            this.worldDefaults.addDefault(path, Integer.valueOf(dfl));
            if (TuinityConfig.configVersion < 1) {
                if (config != null && config.getInt(path) == dfl) {
                    config.set(path, null);
                }
            }
            return config == null ? this.worldDefaults.getInt(path) : config.getInt(path, this.worldDefaults.getInt(path));
        }

        long getLong(final String path, final long dfl) {
            final ConfigurationSection config = TuinityConfig.config.getConfigurationSection(this.configPath);
            this.worldDefaults.addDefault(path, Long.valueOf(dfl));
            if (TuinityConfig.configVersion < 1) {
                if (config != null && config.getLong(path) == dfl) {
                    config.set(path, null);
                }
            }
            return config == null ? this.worldDefaults.getLong(path) : config.getLong(path, this.worldDefaults.getLong(path));
        }

        double getDouble(final String path, final double dfl) {
            final ConfigurationSection config = TuinityConfig.config.getConfigurationSection(this.configPath);
            this.worldDefaults.addDefault(path, Double.valueOf(dfl));
            if (TuinityConfig.configVersion < 1) {
                if (config != null && config.getDouble(path) == dfl) {
                    config.set(path, null);
                }
            }
            return config == null ? this.worldDefaults.getDouble(path) : config.getDouble(path, this.worldDefaults.getDouble(path));
        }

        String getString(final String path, final String dfl) {
            final ConfigurationSection config = TuinityConfig.config.getConfigurationSection(this.configPath);
            this.worldDefaults.addDefault(path, dfl);
            return config == null ? this.worldDefaults.getString(path) : config.getString(path, this.worldDefaults.getString(path));
        }

        /** ignored if {@link TuinityConfig#tickWorldsInParallel} == false */
        public int threads;

        /*
        private void worldthreading() {
            final int threads = this.getInt("tick-threads", -1);
            this.threads = threads == -1 ? TuinityConfig.tickThreads : threads;
        }*/

        public int spawnLimitMonsters;
        public int spawnLimitAnimals;
        public int spawnLimitWaterAmbient;
        public int spawnLimitWaterAnimals;
        public int spawnLimitAmbient;

        private void perWorldSpawnLimit() {
            final String path = "spawn-limits";

            this.spawnLimitMonsters = this.getInt(path + ".monsters", -1);
            this.spawnLimitAnimals = this.getInt(path + ".animals", -1);
            this.spawnLimitWaterAmbient = this.getInt(path + ".water-ambient", -1);
            this.spawnLimitWaterAnimals = this.getInt(path + ".water-animals", -1);
            this.spawnLimitAmbient = this.getInt(path + ".ambient", -1);
        }

        public Long populatorSeed;
        public boolean useRandomPopulatorSeed;

        private void populatorSeed() {
            final String seedString = this.getString("worldgen.seeds.populator", "default");
            if (seedString.equalsIgnoreCase("random")) {
                this.useRandomPopulatorSeed = true;
            } else if (!seedString.equalsIgnoreCase("default")) {
                this.populatorSeed = Long.parseLong(seedString);
            }
        }

    }

}