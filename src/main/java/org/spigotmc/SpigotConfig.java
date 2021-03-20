package org.spigotmc;

import com.google.common.base.Throwables;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.bspfsystems.yamlconfiguration.configuration.InvalidConfigurationException;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.bukkit.ChatColor;
import org.goldenforge.GoldenForge;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;

public class SpigotConfig
{

    private static File CONFIG_FILE;
    private static final String HEADER = "This is the main configuration file for Spigot.\n"
            + "As you can see, there's tons to configure. Some options may impact gameplay, so use\n"
            + "with caution, and make sure you know what each option does before configuring.\n"
            + "For a reference for any variable inside this file, check out the Spigot wiki at\n"
            + "http://www.spigotmc.org/wiki/spigot-configuration/\n"
            + "\n"
            + "If you need help with the configuration or have any questions related to Spigot,\n"
            + "join us at the IRC or drop by our forums and leave a post.\n"
            + "\n"
            + "IRC: #spigot @ irc.spi.gt ( http://www.spigotmc.org/pages/irc/ )\n"
            + "Forums: http://www.spigotmc.org/\n"
            + "\n \n GOLDENFORGE WARNING"
            + "\n NOT OPTIONS ARE NOT IMPLEMENTED NOW";
    /*========================================================================*/
    public static YamlConfiguration config;
    static int version;
    /*========================================================================*/

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

        config.options().header( HEADER );
        config.options().copyDefaults( true );

        version = getInt( "config-version", 12 );
        set( "config-version", 12 );
        readConfig( SpigotConfig.class, null );
    }

    static void readConfig(Class<?> clazz, Object instance)
    {
        for ( Method method : clazz.getDeclaredMethods() )
        {
            if ( Modifier.isPrivate( method.getModifiers() ) )
            {
                if ( method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE )
                {
                    try
                    {
                        method.setAccessible( true );
                        method.invoke( instance );
                    } catch ( InvocationTargetException ex )
                    {
                        throw Throwables.propagate( ex.getCause() );
                    } catch ( Exception ex )
                    {
                        GoldenForge.LOGGER.fatal("Error invoking " + method, ex );
                    }
                }
            }
        }
        // Paper start
        SpigotConfig.save();
    }
    public static void save() {
        // Paper end
        try
        {
            config.save( CONFIG_FILE );
        } catch ( IOException ex )
        {
            GoldenForge.LOGGER.fatal("Could not save " + CONFIG_FILE, ex );
        }
    }

    private static void set(String path, Object val)
    {
        config.set( path, val );
    }

    private static boolean getBoolean(String path, boolean def)
    {
        config.addDefault( path, def );
        return config.getBoolean( path, config.getBoolean( path ) );
    }

    private static int getInt(String path, int def)
    {
        config.addDefault( path, def );
        return config.getInt( path, config.getInt( path ) );
    }

    private static <T> List getList(String path, T def)
    {
        config.addDefault( path, def );
        return (List<T>) config.getList( path, config.getList( path ) );
    }

    private static String getString(String path, String def)
    {
        config.addDefault( path, def );
        return config.getString( path, config.getString( path ) );
    }

    private static double getDouble(String path, double def)
    {
        config.addDefault( path, def );
        return config.getDouble( path, config.getDouble( path ) );
    }

    public static boolean logCommands;
    private static void logCommands()
    {
        logCommands = getBoolean( "commands.log", true );
    }

    public static int tabComplete;
    public static boolean sendNamespaced;
    private static void tabComplete()
    {
        if ( version < 6 )
        {
            boolean oldValue = getBoolean( "commands.tab-complete", true );
            if ( oldValue )
            {
                set( "commands.tab-complete", 0 );
            } else
            {
                set( "commands.tab-complete", -1 );
            }
        }
        tabComplete = getInt( "commands.tab-complete", 0 );
        sendNamespaced = getBoolean( "commands.send-namespaced", true );
    }

    public static String whitelistMessage;
    public static String serverFullMessage;
    public static String serverStartingMessage;
    private static String transform(String s)
    {
        return ChatColor.translateAlternateColorCodes( '&', s ).replaceAll( "\\\\n", "\n" );
    }
    private static void messages()
    {

        whitelistMessage = transform( getString( "messages.whitelist", "You are not whitelisted on this server!" ) );
        serverFullMessage = transform( getString( "messages.server-full", "The server is full!" ) );
        serverStartingMessage = transform( getString( "messages.server-starting", "Server is still starting! Please wait before reconnecting." ) );
    }

    //TODO: implement this
    public static int timeoutTime = 60;
    public static boolean restartOnCrash = true;
    public static String restartScript = "./start.sh";
    public static String restartMessage;
    private static void watchdog()
    {
        timeoutTime = getInt( "settings.timeout-time", timeoutTime );
        restartOnCrash = getBoolean( "settings.restart-on-crash", restartOnCrash );
        restartScript = getString( "settings.restart-script", restartScript );
        restartMessage = transform( getString( "messages.restart", "Server is restarting" ) );
        //WatchdogThread.doStart( timeoutTime, restartOnCrash ); // Paper - moved to PaperConfig
    }

    //TODO: implement bungeecord support
    public static boolean bungee;
    private static void bungee() {
        if ( version < 4 )
        {
            set( "settings.bungeecord", false );
            System.out.println( "Oudated config, disabling BungeeCord support!" );
        }
        bungee = getBoolean( "settings.bungeecord", false );
    }

    private static void nettyThreads()
    {
        int count = getInt( "settings.netty-threads", 4 );
        System.setProperty( "io.netty.eventLoopThreads", Integer.toString( count ) );
        GoldenForge.LOGGER.info( "Using {} threads for Netty based IO", count );
    }


    public static int playerSample;
    private static void playerSample()
    {
        playerSample = Math.max( getInt( "settings.sample-count", 12 ), 0 ); // Paper - Avoid negative counts
        GoldenForge.LOGGER.info("Server Ping Player Sample Count: {}", playerSample ); // Paper - Use logger
    }

    public static int playerShuffle;
    private static void playerShuffle()
    {
        playerShuffle = getInt( "settings.player-shuffle", 0 );
    }

    public static boolean silentCommandBlocks;
    private static void silentCommandBlocks()
    {
        silentCommandBlocks = getBoolean( "commands.silent-commandblock-console", false );
    }

    public static int userCacheCap;
    private static void userCacheCap()
    {
        userCacheCap = getInt( "settings.user-cache-size", 1000 );
    }

    public static boolean saveUserCacheOnStopOnly;
    private static void saveUserCacheOnStopOnly()
    {
        saveUserCacheOnStopOnly = getBoolean( "settings.save-user-cache-on-stop-only", false );
    }

    public static double movedWronglyThreshold;
    private static void movedWronglyThreshold()
    {
        movedWronglyThreshold = getDouble( "settings.moved-wrongly-threshold", 0.0625D );
    }

    public static double movedTooQuicklyMultiplier;
    private static void movedTooQuicklyMultiplier()
    {
        movedTooQuicklyMultiplier = getDouble( "settings.moved-too-quickly-multiplier", 10.0D );
    }

    public static boolean debug;
    private static void debug()
    {
        debug = getBoolean( "settings.debug", false );

        if ( debug && !LogManager.getRootLogger().isTraceEnabled() )
        {
            // Enable debug logging
            LoggerContext ctx = (LoggerContext) LogManager.getContext( false );
            Configuration conf = ctx.getConfiguration();
            conf.getLoggerConfig( LogManager.ROOT_LOGGER_NAME ).setLevel( org.apache.logging.log4j.Level.ALL );
            ctx.updateLoggers( conf );
        }

        if ( LogManager.getRootLogger().isTraceEnabled() )
        {
            GoldenForge.LOGGER.info("Debug logging is enabled" );
        } else
        {
            GoldenForge.LOGGER.info("Debug logging is disabled" );
        }
    }

    public static boolean disableAdvancementSaving;
    public static List<String> disabledAdvancements;
    private static void disabledAdvancements() {
        disableAdvancementSaving = getBoolean("advancements.disable-saving", false);
        disabledAdvancements = getList("advancements.disabled", Arrays.asList(new String[]{"minecraft:story/disabled"}));
    }

    public static boolean logVillagerDeaths;
    private static void logVillagerDeaths() {
        logVillagerDeaths = getBoolean("settings.log-villager-deaths", true);
    }

    public static boolean disablePlayerDataSaving;
    private static void disablePlayerDataSaving() {
        disablePlayerDataSaving = getBoolean("players.disable-saving", false);
    }
}
