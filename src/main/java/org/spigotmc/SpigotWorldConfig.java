package org.spigotmc;


import net.minecraft.server.MinecraftServer;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.goldenforge.GoldenForge;

import java.util.List;

public class SpigotWorldConfig
{

    private final String worldName;
    private final YamlConfiguration config;
    private boolean verbose;

    public SpigotWorldConfig(String worldName)
    {
        this.worldName = worldName;
        this.config = SpigotConfig.config;
        init();
    }

    public void init()
    {
        this.verbose = getBoolean( "verbose", false ); // Paper

        log( "-------- World Settings For [" + worldName + "] --------" );
        SpigotConfig.readConfig( SpigotWorldConfig.class, this );
    }

    private void log(String s)
    {
        if ( verbose )
        {
            GoldenForge.LOGGER.info( s );
        }
    }

    private void set(String path, Object val)
    {
        config.set( "world-settings.default." + path, val );
    }

    public boolean getBoolean(String path, boolean def) // Paper - private -> public
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getBoolean( "world-settings." + worldName + "." + path, config.getBoolean( "world-settings.default." + path ) );
    }

    public double getDouble(String path, double def) // Paper - private -> public
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getDouble( "world-settings." + worldName + "." + path, config.getDouble( "world-settings.default." + path ) );
    }

    public int getInt(String path) // Paper - private -> public
    {
        return config.getInt( "world-settings." + worldName + "." + path );
    }

    public int getInt(String path, int def) // Paper - private -> public
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getInt( "world-settings." + worldName + "." + path, config.getInt( "world-settings.default." + path ) );
    }

    public <T> List getList(String path, T def) // Paper - private -> public
    {
        config.addDefault( "world-settings.default." + path, def );
        return (List<T>) config.getList( "world-settings." + worldName + "." + path, config.getList( "world-settings.default." + path ) );
    }

    public String getString(String path, String def) // Paper - private -> public
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getString( "world-settings." + worldName + "." + path, config.getString( "world-settings.default." + path ) );
    }

    private Object get(String path, Object def)
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.get( "world-settings." + worldName + "." + path, config.get( "world-settings.default." + path ) );
    }

    // Crop growth rates
    public int cactusModifier;
    public int caneModifier;
    public int melonModifier;
    public int mushroomModifier;
    public int pumpkinModifier;
    public int saplingModifier;
    public int beetrootModifier;
    public int carrotModifier;
    public int potatoModifier;
    public int wheatModifier;
    public int wartModifier;
    public int vineModifier;
    public int cocoaModifier;
    public int bambooModifier;
    public int sweetBerryModifier;
    public int kelpModifier;
    private int getAndValidateGrowth(String crop)
    {
        int modifier = getInt( "growth." + crop.toLowerCase(java.util.Locale.ENGLISH) + "-modifier", 100 );
        if ( modifier == 0 )
        {
            log( "Cannot set " + crop + " growth to zero, defaulting to 100" );
            modifier = 100;
        }
        log( crop + " Growth Modifier: " + modifier + "%" );

        return modifier;
    }
    private void growthModifiers()
    {
        cactusModifier = getAndValidateGrowth( "Cactus" );
        caneModifier = getAndValidateGrowth( "Cane" );
        melonModifier = getAndValidateGrowth( "Melon" );
        mushroomModifier = getAndValidateGrowth( "Mushroom" );
        pumpkinModifier = getAndValidateGrowth( "Pumpkin" );
        saplingModifier = getAndValidateGrowth( "Sapling" );
        beetrootModifier = getAndValidateGrowth( "Beetroot" );
        carrotModifier = getAndValidateGrowth( "Carrot" );
        potatoModifier = getAndValidateGrowth( "Potato" );
        wheatModifier = getAndValidateGrowth( "Wheat" );
        wartModifier = getAndValidateGrowth( "NetherWart" );
        vineModifier = getAndValidateGrowth( "Vine" );
        cocoaModifier = getAndValidateGrowth( "Cocoa" );
        bambooModifier = getAndValidateGrowth( "Bamboo" );
        sweetBerryModifier = getAndValidateGrowth( "SweetBerry" );
        kelpModifier = getAndValidateGrowth( "Kelp" );
    }

    public double itemMerge;
    private void itemMerge()
    {
        itemMerge = getDouble("merge-radius.item", 2.5 );
        log( "Item Merge Radius: " + itemMerge );
    }

    public double expMerge;
    private void expMerge()
    {
        expMerge = getDouble("merge-radius.exp", 3.0 );
        log( "Experience Merge Radius: " + expMerge );
    }

    public int viewDistance;
    private void viewDistance()
    {
        if ( SpigotConfig.version < 12 )
        {
            set( "view-distance", null );
        }

        Object viewDistanceObject = get( "view-distance", "default" );
        viewDistance = ( viewDistanceObject ) instanceof Number ? ( (Number) viewDistanceObject ).intValue() : -1;
        if ( viewDistance <= 0 )
        {
            //viewDistance = MinecraftServer.getServer().getProperties().viewDistance;
        }

        viewDistance = Math.max( Math.min( viewDistance, 32 ), 3 );
        log( "View Distance: " + viewDistance );
    }

    public byte mobSpawnRange;
    private void mobSpawnRange()
    {
        mobSpawnRange = (byte) getInt( "mob-spawn-range", 8 ); // Paper - Vanilla
        log( "Mob Spawn Range: " + mobSpawnRange );
    }

    public int itemDespawnRate;
    private void itemDespawnRate()
    {
        itemDespawnRate = getInt( "item-despawn-rate", 6000 );
        log( "Item Despawn Rate: " + itemDespawnRate );
    }

    public int animalActivationRange = 32;
    public int monsterActivationRange = 32;
    public int raiderActivationRange = 48;
    public int miscActivationRange = 16;
    // Paper start
    public int flyingMonsterActivationRange = 32;
    public int waterActivationRange = 16;
    public int villagerActivationRange = 32;
    public int wakeUpInactiveAnimals = 4;
    public int wakeUpInactiveAnimalsEvery = 60*20;
    public int wakeUpInactiveAnimalsFor = 5*20;
    public int wakeUpInactiveMonsters = 8;
    public int wakeUpInactiveMonstersEvery = 20*20;
    public int wakeUpInactiveMonstersFor = 5*20;
    public int wakeUpInactiveVillagers = 4;
    public int wakeUpInactiveVillagersEvery = 30*20;
    public int wakeUpInactiveVillagersFor = 5*20;
    public int wakeUpInactiveFlying = 8;
    public int wakeUpInactiveFlyingEvery = 10*20;
    public int wakeUpInactiveFlyingFor = 5*20;
    public int villagersWorkImmunityAfter = 5*20;
    public int villagersWorkImmunityFor = 20;
    public boolean villagersActiveForPanic = true;
    // Paper end
    public boolean tickInactiveVillagers = true;
    private void activationRange()
    {
        boolean hasAnimalsConfig = config.getInt("entity-activation-range.animals", animalActivationRange) != animalActivationRange; // Paper
        animalActivationRange = getInt( "entity-activation-range.animals", animalActivationRange );
        monsterActivationRange = getInt( "entity-activation-range.monsters", monsterActivationRange );
        raiderActivationRange = getInt( "entity-activation-range.raiders", raiderActivationRange );
        miscActivationRange = getInt( "entity-activation-range.misc", miscActivationRange );
        // Paper start
        waterActivationRange = getInt( "entity-activation-range.water", waterActivationRange );
        villagerActivationRange = getInt( "entity-activation-range.villagers", hasAnimalsConfig ? animalActivationRange : villagerActivationRange );
        flyingMonsterActivationRange = getInt( "entity-activation-range.flying-monsters", flyingMonsterActivationRange );

        wakeUpInactiveAnimals = getInt("entity-activation-range.wake-up-inactive.animals-max-per-tick", wakeUpInactiveAnimals);
        wakeUpInactiveAnimalsEvery = getInt("entity-activation-range.wake-up-inactive.animals-every", wakeUpInactiveAnimalsEvery);
        wakeUpInactiveAnimalsFor = getInt("entity-activation-range.wake-up-inactive.animals-for", wakeUpInactiveAnimalsFor);

        wakeUpInactiveMonsters = getInt("entity-activation-range.wake-up-inactive.monsters-max-per-tick", wakeUpInactiveMonsters);
        wakeUpInactiveMonstersEvery = getInt("entity-activation-range.wake-up-inactive.monsters-every", wakeUpInactiveMonstersEvery);
        wakeUpInactiveMonstersFor = getInt("entity-activation-range.wake-up-inactive.monsters-for", wakeUpInactiveMonstersFor);

        wakeUpInactiveVillagers = getInt("entity-activation-range.wake-up-inactive.villagers-max-per-tick", wakeUpInactiveVillagers);
        wakeUpInactiveVillagersEvery = getInt("entity-activation-range.wake-up-inactive.villagers-every", wakeUpInactiveVillagersEvery);
        wakeUpInactiveVillagersFor = getInt("entity-activation-range.wake-up-inactive.villagers-for", wakeUpInactiveVillagersFor);

        wakeUpInactiveFlying = getInt("entity-activation-range.wake-up-inactive.flying-monsters-max-per-tick", wakeUpInactiveFlying);
        wakeUpInactiveFlyingEvery = getInt("entity-activation-range.wake-up-inactive.flying-monsters-every", wakeUpInactiveFlyingEvery);
        wakeUpInactiveFlyingFor = getInt("entity-activation-range.wake-up-inactive.flying-monsters-for", wakeUpInactiveFlyingFor);

        villagersWorkImmunityAfter = getInt( "entity-activation-range.villagers-work-immunity-after", villagersWorkImmunityAfter );
        villagersWorkImmunityFor = getInt( "entity-activation-range.villagers-work-immunity-for", villagersWorkImmunityFor );
        villagersActiveForPanic = getBoolean( "entity-activation-range.villagers-active-for-panic", villagersActiveForPanic );
        // Paper end
        tickInactiveVillagers = getBoolean( "entity-activation-range.tick-inactive-villagers", tickInactiveVillagers );
        log( "Entity Activation Range: An " + animalActivationRange + " / Mo " + monsterActivationRange + " / Ra " + raiderActivationRange + " / Mi " + miscActivationRange + " / Tiv " + tickInactiveVillagers );
    }

    public int playerTrackingRange = 48;
    public int animalTrackingRange = 48;
    public int monsterTrackingRange = 48;
    public int miscTrackingRange = 32;
    public int otherTrackingRange = 64;
    private void trackingRange()
    {
        playerTrackingRange = getInt( "entity-tracking-range.players", playerTrackingRange );
        animalTrackingRange = getInt( "entity-tracking-range.animals", animalTrackingRange );
        monsterTrackingRange = getInt( "entity-tracking-range.monsters", monsterTrackingRange );
        miscTrackingRange = getInt( "entity-tracking-range.misc", miscTrackingRange );
        otherTrackingRange = getInt( "entity-tracking-range.other", otherTrackingRange );
        log( "Entity Tracking Range: Pl " + playerTrackingRange + " / An " + animalTrackingRange + " / Mo " + monsterTrackingRange + " / Mi " + miscTrackingRange + " / Other " + otherTrackingRange );
    }

    public int hopperTransfer;
    public int hopperCheck;
    public int hopperAmount;
    private void hoppers()
    {
        // Set the tick delay between hopper item movements
        hopperTransfer = getInt( "ticks-per.hopper-transfer", 8 );
        if ( SpigotConfig.version < 11 )
        {
            set( "ticks-per.hopper-check", 1 );
        }
        hopperCheck = getInt( "ticks-per.hopper-check", 1 );
        hopperAmount = getInt( "hopper-amount", 1 );
        log( "Hopper Transfer: " + hopperTransfer + " Hopper Check: " + hopperCheck + " Hopper Amount: " + hopperAmount );
    }

    public int arrowDespawnRate;
    public int tridentDespawnRate;
    private void arrowDespawnRate()
    {
        arrowDespawnRate = getInt( "arrow-despawn-rate", 1200 );
        tridentDespawnRate = getInt( "trident-despawn-rate", arrowDespawnRate );
        log( "Arrow Despawn Rate: " + arrowDespawnRate + " Trident Respawn Rate:" + tridentDespawnRate );
    }

    public boolean zombieAggressiveTowardsVillager;
    private void zombieAggressiveTowardsVillager()
    {
        zombieAggressiveTowardsVillager = getBoolean( "zombie-aggressive-towards-villager", true );
        log( "Zombie Aggressive Towards Villager: " + zombieAggressiveTowardsVillager );
    }

    public boolean nerfSpawnerMobs;
    private void nerfSpawnerMobs()
    {
        nerfSpawnerMobs = getBoolean( "nerf-spawner-mobs", false );
        log( "Nerfing mobs spawned from spawners: " + nerfSpawnerMobs );
    }

    public boolean enableZombiePigmenPortalSpawns;
    private void enableZombiePigmenPortalSpawns()
    {
        enableZombiePigmenPortalSpawns = getBoolean( "enable-zombie-pigmen-portal-spawns", true );
        log( "Allow Zombie Pigmen to spawn from portal blocks: " + enableZombiePigmenPortalSpawns );
    }

    public int dragonDeathSoundRadius;
    private void keepDragonDeathPerWorld()
    {
        dragonDeathSoundRadius = getInt( "dragon-death-sound-radius", 0 );
    }

    public int witherSpawnSoundRadius;
    private void witherSpawnSoundRadius()
    {
        witherSpawnSoundRadius = getInt( "wither-spawn-sound-radius", 0 );
    }

    public int endPortalSoundRadius;
    private void endPortalSoundRadius()
    {
        endPortalSoundRadius = getInt( "end-portal-sound-radius", 0 );
    }

    public int villageSeed;
    public int desertSeed;
    public int iglooSeed;
    public int jungleSeed;
    public int swampSeed;
    public int monumentSeed;
    public int oceanSeed;
    public int outpostSeed;
    public int shipwreckSeed;
    public int slimeSeed;
    public int endCitySeed;
    public int bastionSeed;
    public int fortressSeed;
    public int mansionSeed;
    public int fossilSeed;
    public int portalSeed;
    private void initWorldGenSeeds()
    {
        villageSeed = getInt( "seed-village", 10387312 );
        desertSeed = getInt( "seed-desert", 14357617 );
        iglooSeed = getInt( "seed-igloo", 14357618 );
        jungleSeed = getInt( "seed-jungle", 14357619 );
        swampSeed = getInt( "seed-swamp", 14357620 );
        monumentSeed = getInt( "seed-monument", 10387313 );
        shipwreckSeed = getInt( "seed-shipwreck", 165745295 );
        oceanSeed = getInt( "seed-ocean", 14357621 );
        outpostSeed = getInt( "seed-outpost", 165745296 );
        endCitySeed = getInt( "seed-endcity", 10387313 );
        slimeSeed = getInt( "seed-slime", 987234911 );
        bastionSeed = getInt( "seed-bastion", 30084232 );
        fortressSeed = getInt( "seed-fortress", 30084232 );
        mansionSeed = getInt( "seed-mansion", 10387319 );
        fossilSeed = getInt( "seed-fossil", 14357921 );
        portalSeed = getInt( "seed-portal", 34222645 );
        log( "Custom Map Seeds:  Village: " + villageSeed + " Desert: " + desertSeed + " Igloo: " + iglooSeed + " Jungle: " + jungleSeed + " Swamp: " + swampSeed + " Monument: " + monumentSeed
                + " Ocean: " + oceanSeed + " Shipwreck: " + shipwreckSeed + " End City: " + endCitySeed + " Slime: " + slimeSeed + " Bastion: " + bastionSeed + " Fortress: " + fortressSeed + " Mansion: " + mansionSeed + " Fossil: " + fossilSeed + " Portal: " + portalSeed );
    }

    public float jumpWalkExhaustion;
    public float jumpSprintExhaustion;
    public float combatExhaustion;
    public float regenExhaustion;
    public float swimMultiplier;
    public float sprintMultiplier;
    public float otherMultiplier;
    private void initHunger()
    {
        if ( SpigotConfig.version < 10 )
        {
            set( "hunger.walk-exhaustion", null );
            set( "hunger.sprint-exhaustion", null );
            set( "hunger.combat-exhaustion", 0.1 );
            set( "hunger.regen-exhaustion", 6.0 );
        }

        jumpWalkExhaustion = (float) getDouble( "hunger.jump-walk-exhaustion", 0.05 );
        jumpSprintExhaustion = (float) getDouble( "hunger.jump-sprint-exhaustion", 0.2 );
        combatExhaustion = (float) getDouble( "hunger.combat-exhaustion", 0.1 );
        regenExhaustion = (float) getDouble( "hunger.regen-exhaustion", 6.0 );
        swimMultiplier = (float) getDouble( "hunger.swim-multiplier", 0.01 );
        sprintMultiplier = (float) getDouble( "hunger.sprint-multiplier", 0.1 );
        otherMultiplier = (float) getDouble( "hunger.other-multiplier", 0.0 );
    }

    public int currentPrimedTnt = 0;
    public int maxTntTicksPerTick;
    private void maxTntPerTick() {
        if ( SpigotConfig.version < 7 )
        {
            set( "max-tnt-per-tick", 100 );
        }
        maxTntTicksPerTick = getInt( "max-tnt-per-tick", 100 );
        log( "Max TNT Explosions: " + maxTntTicksPerTick );
    }

    public int hangingTickFrequency;
    private void hangingTickFrequency()
    {
        hangingTickFrequency = getInt( "hanging-tick-frequency", 100 );
    }

    public int tileMaxTickTime;
    public int entityMaxTickTime;
    private void maxTickTimes()
    {
        tileMaxTickTime = getInt("max-tick-time.tile", 50);
        entityMaxTickTime = getInt("max-tick-time.entity", 50);
        log("Tile Max Tick Time: " + tileMaxTickTime + "ms Entity max Tick Time: " + entityMaxTickTime + "ms");
    }

    public double squidSpawnRangeMin;
    private void squidSpawnRange()
    {
        squidSpawnRangeMin = getDouble("squid-spawn-range.min", 45.0D);
    }
}
