package com.tuinity.tuinity.chunk;

import com.destroystokyo.paper.util.MCUtil;
import com.destroystokyo.paper.util.misc.PlayerAreaMap;
import com.destroystokyo.paper.util.misc.PooledLinkedHashSets;
import com.tuinity.tuinity.config.TuinityConfig;
import com.tuinity.tuinity.util.TickThread;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SUpdateChunkPositionPacket;
import net.minecraft.network.play.server.SUpdateViewDistancePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.TicketType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public final class PlayerChunkLoader {

    public static final int MIN_VIEW_DISTANCE = 2;
    public static final int MAX_VIEW_DISTANCE = 32;

    public static final int TICK_TICKET_LEVEL = 31;
    public static final int LOADED_TICKET_LEVEL = 33;

    protected final ChunkManager chunkMap;
    protected final Reference2ObjectLinkedOpenHashMap<ServerPlayerEntity, PlayerLoaderData> playerMap = new Reference2ObjectLinkedOpenHashMap<>(512, 0.7f);
    protected final ReferenceLinkedOpenHashSet<PlayerLoaderData> chunkSendQueue = new ReferenceLinkedOpenHashSet<>(512, 0.7f);

    protected final TreeSet<PlayerLoaderData> chunkLoadQueue = new TreeSet<>((final PlayerLoaderData p1, final PlayerLoaderData p2) -> {
        if (p1 == p2) {
            return 0;
        }

        final ChunkPriorityHolder holder1 = p1.loadQueue.peekFirst();
        final ChunkPriorityHolder holder2 = p2.loadQueue.peekFirst();

        final int priorityCompare = Double.compare(holder1 == null ? Double.MAX_VALUE : holder1.priority, holder2 == null ? Double.MAX_VALUE : holder2.priority);

        if (priorityCompare != 0) {
            return priorityCompare;
        }

        final int idCompare = Integer.compare(p1.player.getId(), p2.player.getId());

        if (idCompare != 0) {
            return idCompare;
        }

        // last resort
        return Integer.compare(System.identityHashCode(p1), System.identityHashCode(p2));
    });

    protected final TreeSet<PlayerLoaderData> chunkSendWaitQueue = new TreeSet<>((final PlayerLoaderData p1, final PlayerLoaderData p2) -> {
        if (p1 == p2) {
            return 0;
        }

        final int timeCompare = Long.compare(p1.nextChunkSendTarget, p2.nextChunkSendTarget);
        if (timeCompare != 0) {
            return timeCompare;
        }

        final int idCompare = Integer.compare(p1.player.getId(), p2.player.getId());

        if (idCompare != 0) {
            return idCompare;
        }

        // last resort
        return Integer.compare(System.identityHashCode(p1), System.identityHashCode(p2));
    });


    // no throttling is applied below this VD for loading

    /**
     * The chunks to be sent to players, provided they're send-ready. Send-ready means the chunk and its 1 radius neighbours are loaded.
     */
    public final PlayerAreaMap broadcastMap;

    /**
     * The chunks to be brought up to send-ready status. Send-ready means the chunk and its 1 radius neighbours are loaded.
     */
    public final PlayerAreaMap loadMap;

    /**
     * Areamap used only to remove tickets for send-ready chunks. View distance is always + 1 of load view distance. Thus,
     * this map is always representing the chunks we are actually going to load.
     */
    public final PlayerAreaMap loadTicketCleanup;

    /**
     * The chunks to brought to ticking level. Each chunk must have 2 radius neighbours loaded before this can happen.
     */
    public final PlayerAreaMap tickMap;

    /**
     * -1 if defaulting to [load distance], else always in [2, load distance]
     */
    protected int rawSendDistance = -1;

    /**
     * -1 if defaulting to [tick view distance + 1], else always in [tick view distance + 1, 32 + 1]
     */
    protected int rawLoadDistance = -1;

    /**
     * Never -1, always in [2, 32]
     */
    protected int rawTickDistance = -1;

    // methods to bridge for API

    public int getTargetViewDistance() {
        return this.getTickDistance();
    }

    public void setTargetViewDistance(final int distance) {
        this.setTickDistance(distance);
    }

    public int getTargetNoTickViewDistance() {
        return this.getLoadDistance() - 1;
    }

    public void setTargetNoTickViewDistance(final int distance) {
        this.setLoadDistance(distance == -1 ? -1 : distance + 1);
    }

    public int getTargetSendDistance() {
        return this.rawSendDistance == -1 ? this.getLoadDistance() : this.rawSendDistance;
    }

    public void setTargetSendDistance(final int distance) {
        this.setSendDistance(distance);
    }

    // internal methods

    public int getSendDistance() {
        final int loadDistance = this.getLoadDistance();
        return this.rawSendDistance == -1 ? loadDistance : Math.min(this.rawSendDistance, loadDistance);
    }

    public void setSendDistance(final int distance) {
        if (distance != -1 && (distance < MIN_VIEW_DISTANCE || distance > MAX_VIEW_DISTANCE + 1)) {
            throw new IllegalArgumentException(Integer.toString(distance));
        }
        this.rawSendDistance = distance;
    }

    public int getLoadDistance() {
        final int tickDistance = this.getTickDistance();
        return this.rawLoadDistance == -1 ? tickDistance + 1 : Math.max(tickDistance + 1, this.rawLoadDistance);
    }

    public void setLoadDistance(final int distance) {
        if (distance != -1 && (distance < MIN_VIEW_DISTANCE || distance > MAX_VIEW_DISTANCE + 1)) {
            throw new IllegalArgumentException(Integer.toString(distance));
        }
        this.rawLoadDistance = distance;
    }

    public int getTickDistance() {
        return this.rawTickDistance;
    }

    public void setTickDistance(final int distance) {
        if (distance < MIN_VIEW_DISTANCE || distance > MAX_VIEW_DISTANCE) {
            throw new IllegalArgumentException(Integer.toString(distance));
        }
        this.rawTickDistance = distance;
    }

    /*
      Players have 3 different types of view distance:
      1. Sending view distance
      2. Loading view distance
      3. Ticking view distance

      But for configuration purposes (and API) there are:
      1. No-tick view distance
      2. Tick view distance
      3. Broadcast view distance

      These aren't always the same as the types we represent internally.

      Loading view distance is always max(no-tick + 1, tick + 1)
      - no-tick has 1 added because clients need an extra radius to render chunks
      - tick has 1 added because it needs an extra radius of chunks to load before they can be marked ticking

      Loading view distance is defined as the radius of chunks that will be brought to send-ready status, which means
      it loads chunks in radius load-view-distance + 1.

      The maximum value for send view distance is the load view distance. API can set it lower.
     */

    public PlayerChunkLoader(final ChunkManager chunkMap, final PooledLinkedHashSets<ServerPlayerEntity> pooledHashSets) {
        this.chunkMap = chunkMap;
        this.broadcastMap = new PlayerAreaMap(pooledHashSets,
                (ServerPlayerEntity player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
                 PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayerEntity> newState) -> {
                    if (player.needsChunkCenterUpdate) {
                        player.needsChunkCenterUpdate = false;
                        player.connection.send(new SUpdateChunkPositionPacket(currPosX, currPosZ));
                    }
                    PlayerChunkLoader.this.onChunkEnter(player, rangeX, rangeZ);
                },
                (ServerPlayerEntity player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
                 PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayerEntity> newState) -> {
                    PlayerChunkLoader.this.onChunkLeave(player, rangeX, rangeZ);
                });
        this.loadMap = new PlayerAreaMap(pooledHashSets,
                null,
                (ServerPlayerEntity player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
                 PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayerEntity> newState) -> {
                    if (newState != null) {
                        return;
                    }
                    PlayerChunkLoader.this.isTargetedForPlayerLoad.remove(MCUtil.getCoordinateKey(rangeX, rangeZ));
                });
        this.loadTicketCleanup = new PlayerAreaMap(pooledHashSets,
                null,
                (ServerPlayerEntity player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
                 PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayerEntity> newState) -> {
                    if (newState != null) {
                        return;
                    }
                    ChunkPos chunkPos = new ChunkPos(rangeX, rangeZ);
                    PlayerChunkLoader.this.chunkMap.level.getChunkSource().releaseTickingTicket(TicketType.PLAYER, chunkPos, LOADED_TICKET_LEVEL, chunkPos);
                    if (PlayerChunkLoader.this.chunkTicketTracker.remove(chunkPos.toLong())) {
                        --PlayerChunkLoader.this.concurrentChunkLoads;
                    }
                });
        this.tickMap = new PlayerAreaMap(pooledHashSets,
                (ServerPlayerEntity player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
                 PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayerEntity> newState) -> {
                    if (newState.size() != 1) {
                        return;
                    }
                    Chunk chunk = PlayerChunkLoader.this.chunkMap.level.getChunkSource().getChunkAtIfLoadedMainThreadNoCache(rangeX, rangeZ);
                    if (chunk == null || !chunk.areNeighboursLoaded(2)) {
                        return;
                    }

                    ChunkPos chunkPos = new ChunkPos(rangeX, rangeZ);
                    PlayerChunkLoader.this.chunkMap.level.getChunkSource().registerTickingTicket(TicketType.PLAYER, chunkPos, TICK_TICKET_LEVEL, chunkPos);
                },
                (ServerPlayerEntity player, int rangeX, int rangeZ, int currPosX, int currPosZ, int prevPosX, int prevPosZ,
                 PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayerEntity> newState) -> {
                    if (newState != null) {
                        return;
                    }
                    ChunkPos chunkPos = new ChunkPos(rangeX, rangeZ);
                    PlayerChunkLoader.this.chunkMap.level.getChunkSource().releaseTickingTicket(TicketType.PLAYER, chunkPos, TICK_TICKET_LEVEL, chunkPos);
                });
    }

    protected final LongOpenHashSet isTargetedForPlayerLoad = new LongOpenHashSet();
    protected final LongOpenHashSet chunkTicketTracker = new LongOpenHashSet();

    // rets whether the chunk is at a loaded stage that is ready to be sent to players
    public boolean isChunkPlayerLoaded(final int chunkX, final int chunkZ) {
        final long key = MCUtil.getCoordinateKey(chunkX, chunkZ);
        final ChunkHolder chunk = this.chunkMap.getVisibleChunkIfPresent(key);

        if (chunk == null) {
            return false;
        }

        return chunk.getSendingChunk() != null && this.isTargetedForPlayerLoad.contains(key);
    }

    public boolean isChunkSent(final ServerPlayerEntity player, final int chunkX, final int chunkZ) {
        final PlayerLoaderData data = this.playerMap.get(player);
        if (data == null) {
            return false;
        }

        return data.hasSentChunk(chunkX, chunkZ);
    }

    protected int getMaxConcurrentChunkSends() {
        double config = TuinityConfig.playerMaxConcurrentChunkSends;
        return Math.max(1, config <= 0 ? (int)Math.ceil(-config * this.chunkMap.level.players().size()) : (int)config);
    }

    protected int getMaxChunkLoads() {
        double config = TuinityConfig.playerMaxConcurrentChunkLoads;
        return Math.max(1, (config <= 0 ? (int)Math.ceil(-config * MinecraftServer.getServer().getPlayerCount()) : (int)config) * 9);
    }

    protected double getTargetSendRatePerPlayer() {
        double config = TuinityConfig.playerTargetChunkSendRate;
        return config <= 0 ? -config : config / MinecraftServer.getServer().getPlayerCount();
    }

    public void onChunkPlayerTickReady(final int chunkX, final int chunkZ) {
        final ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        this.chunkMap.level.getChunkSource().registerTickingTicket(TicketType.PLAYER, chunkPos, TICK_TICKET_LEVEL, chunkPos);
    }

    public void onChunkSendReady(final int chunkX, final int chunkZ) {
        final long chunkKey = MCUtil.getCoordinateKey(chunkX, chunkZ);

        final PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayerEntity> playersInSendRange = this.broadcastMap.getObjectsInRange(chunkX, chunkZ);

        if (playersInSendRange == null) {
            return;
        }

        final Object[] rawData = playersInSendRange.getBackingSet();
        for (int i = 0, len = rawData.length; i < len; ++i) {
            final Object raw = rawData[i];

            if (!(raw instanceof ServerPlayerEntity)) {
                continue;
            }
            this.onChunkEnter((ServerPlayerEntity)raw, chunkX, chunkZ);
        }

        // now let's try and queue mid tick logic again
    }

    public void onChunkEnter(final ServerPlayerEntity player, final int chunkX, final int chunkZ) {
        final PlayerLoaderData data = this.playerMap.get(player);

        if (data == null) {
            return;
        }

        if (data.hasSentChunk(chunkX, chunkZ) || !this.isChunkPlayerLoaded(chunkX, chunkZ)) {
            // if we don't have player tickets, then the load logic will pick this up and queue to send
            return;
        }

        final long playerPos = this.broadcastMap.getLastCoordinate(player);
        final int playerChunkX = MCUtil.getCoordinateX(playerPos);
        final int playerChunkZ = MCUtil.getCoordinateZ(playerPos);
        final int manhattanDistance = Math.abs(playerChunkX - chunkX) + Math.abs(playerChunkZ - chunkZ);

        final ChunkPriorityHolder holder = new ChunkPriorityHolder(chunkX, chunkZ, manhattanDistance, 0.0);
        data.sendQueue.add(holder);
    }

    public void onChunkLoad(final int chunkX, final int chunkZ) {
        if (this.chunkTicketTracker.remove(MCUtil.getCoordinateKey(chunkX, chunkZ))) {
            --this.concurrentChunkLoads;
        }
    }

    public void onChunkLeave(final ServerPlayerEntity player, final int chunkX, final int chunkZ) {
        final PlayerLoaderData data = this.playerMap.get(player);

        if (data == null) {
            return;
        }

        data.unloadChunk(chunkX, chunkZ);
    }

    public void addPlayer(final ServerPlayerEntity player) {
        TickThread.ensureTickThread("Cannot add player async");
        if (!player.isRealPlayer) {
            return;
        }
        final PlayerLoaderData data = new PlayerLoaderData(player, this);
        if (this.playerMap.putIfAbsent(player, data) == null) {
            data.update();
        }
    }

    public void removePlayer(final ServerPlayerEntity player) {
        TickThread.ensureTickThread("Cannot remove player async");
        if (!player.isRealPlayer) {
            return;
        }

        final PlayerLoaderData loaderData = this.playerMap.remove(player);
        if (loaderData == null) {
            return;
        }
        loaderData.remove();
        this.chunkLoadQueue.remove(loaderData);
        this.chunkSendQueue.remove(loaderData);
        this.chunkSendWaitQueue.remove(loaderData);
        synchronized (this.sendingChunkCounts) {
            final int count = this.sendingChunkCounts.removeInt(loaderData);
            if (count != 0) {
                concurrentChunkSends.getAndAdd(-count);
            }
        }
    }

    public void updatePlayer(final ServerPlayerEntity player) {
        TickThread.ensureTickThread("Cannot update player async");
        if (!player.isRealPlayer) {
            return;
        }
        final PlayerLoaderData loaderData = this.playerMap.get(player);
        if (loaderData != null) {
            loaderData.update();
        }
    }

    public PlayerLoaderData getData(final ServerPlayerEntity player) {
        return this.playerMap.get(player);
    }

    public void tick() {
        TickThread.ensureTickThread("Cannot tick async");
        for (final PlayerLoaderData data : this.playerMap.values()) {
            data.update();
        }
        this.tickMidTick();
    }

    protected static final AtomicInteger concurrentChunkSends = new AtomicInteger();
    protected final Reference2IntOpenHashMap<PlayerLoaderData> sendingChunkCounts = new Reference2IntOpenHashMap<>();
    private void trySendChunks() {
        final long time = System.nanoTime();
        // drain entries from wait queue
        while (!this.chunkSendWaitQueue.isEmpty()) {
            final PlayerLoaderData data = this.chunkSendWaitQueue.first();

            if (data.nextChunkSendTarget > time) {
                break;
            }

            this.chunkSendWaitQueue.pollFirst();

            this.chunkSendQueue.add(data);
        }

        if (this.chunkSendQueue.isEmpty()) {
            return;
        }

        final int maxSends = this.getMaxConcurrentChunkSends();
        final double sendRate = this.getTargetSendRatePerPlayer();
        final long nextDeadline = (long)((1 / sendRate) * 1.0e9) + time;
        for (;;) {
            if (this.chunkSendQueue.isEmpty()) {
                break;
            }
            final int currSends = concurrentChunkSends.get();
            if (currSends >= maxSends) {
                break;
            }

            if (!concurrentChunkSends.compareAndSet(currSends, currSends + 1)) {
                continue;
            }

            // send chunk

            final PlayerLoaderData data = this.chunkSendQueue.removeFirst();

            final ChunkPriorityHolder queuedSend = data.sendQueue.pollFirst();
            if (queuedSend == null) {
                concurrentChunkSends.getAndDecrement(); // we never sent, so decrease
                // stop iterating over players who have nothing to send
                if (this.chunkSendQueue.isEmpty()) {
                    // nothing left
                    break;
                }
                continue;
            }

            if (!this.isChunkPlayerLoaded(queuedSend.chunkX, queuedSend.chunkZ)) {
                throw new IllegalStateException();
            }

            data.nextChunkSendTarget = nextDeadline;
            this.chunkSendWaitQueue.add(data);

            synchronized (this.sendingChunkCounts) {
                this.sendingChunkCounts.addTo(data, 1);
            }

            data.sendChunk(queuedSend.chunkX, queuedSend.chunkZ, () -> {
                synchronized (this.sendingChunkCounts) {
                    final int count = this.sendingChunkCounts.getInt(data);
                    if (count == 0) {
                        // disconnected, so we don't need to decrement: it will be decremented for us
                        return;
                    }
                    if (count == 1) {
                        this.sendingChunkCounts.removeInt(data);
                    } else {
                        this.sendingChunkCounts.put(data, count - 1);
                    }
                }

                concurrentChunkSends.getAndDecrement();
            });
        }
    }

    protected int concurrentChunkLoads;
    private void tryLoadChunks() {
        if (this.chunkLoadQueue.isEmpty()) {
            return;
        }

        final int maxLoads = this.getMaxChunkLoads();
        for (;;) {
            final PlayerLoaderData data = this.chunkLoadQueue.pollFirst();

            final ChunkPriorityHolder queuedLoad = data.loadQueue.peekFirst();
            if (queuedLoad == null) {
                if (this.chunkLoadQueue.isEmpty()) {
                    break;
                }
                continue;
            }

            if (this.isChunkPlayerLoaded(queuedLoad.chunkX, queuedLoad.chunkZ)) {
                // already loaded!
                data.loadQueue.pollFirst(); // already loaded so we just skip
                this.chunkLoadQueue.add(data);

                // ensure the chunk is queued to send
                this.onChunkSendReady(queuedLoad.chunkX, queuedLoad.chunkZ);
                continue;
            }

            final long chunkKey = MCUtil.getCoordinateKey(queuedLoad.chunkX, queuedLoad.chunkZ);

            final double priority = queuedLoad.priority;
            // while we do need to rate limit chunk loads, the logic for sending chunks requires that tickets are present.
            // when chunks are loaded (i.e spawn) but do not have this player's tickets, they have to wait behind the
            // load queue. To avoid this problem, we check early here if tickets are required to load the chunk - if they
            // aren't required, it bypasses the limiter system.
            boolean unloadedTargetChunk = false;
            unloaded_check:
            for (int dz = -1; dz <= 1; ++dz) {
                for (int dx = -1; dx <= 1; ++dx) {
                    final int offX = queuedLoad.chunkX + dx;
                    final int offZ = queuedLoad.chunkZ + dz;
                    if (this.chunkMap.level.getChunkSource().getChunkAtIfLoadedMainThreadNoCache(offX, offZ) == null) {
                        unloadedTargetChunk = true;
                        break unloaded_check;
                    }
                }
            }
            if (unloadedTargetChunk && priority > 0.0) {
                // priority > 0.0 implies rate limited chunks

                final int currentChunkLoads = this.concurrentChunkLoads;
                if (currentChunkLoads >= maxLoads) {
                    // don't poll, we didn't load it
                    this.chunkLoadQueue.add(data);
                    break;
                }
            }

            // can only poll after we decide to load
            data.loadQueue.pollFirst();

            // now that we've polled we can re-add to load queue
            this.chunkLoadQueue.add(data);

            // add necessary tickets to load chunk up to send-ready
            for (int dz = -1; dz <= 1; ++dz) {
                for (int dx = -1; dx <= 1; ++dx) {
                    final int offX = queuedLoad.chunkX + dx;
                    final int offZ = queuedLoad.chunkZ + dz;
                    final ChunkPos chunkPos = new ChunkPos(offX, offZ);

                    this.chunkMap.level.getChunkSource().registerTickingTicket(TicketType.PLAYER, chunkPos, LOADED_TICKET_LEVEL, chunkPos);
                    if (this.chunkMap.level.getChunkSource().getChunkAtIfLoadedMainThreadNoCache(offX, offZ) != null) {
                        continue;
                    }

                    if (priority > 0.0 && this.chunkTicketTracker.add(MCUtil.getCoordinateKey(offX, offZ))) {
                        // wont reach here if unloadedTargetChunk is false
                        ++this.concurrentChunkLoads;
                    }
                }
            }

            // mark that we've added tickets here
            this.isTargetedForPlayerLoad.add(chunkKey);

            // it's possible all we needed was the player tickets to queue up the send.
            if (this.isChunkPlayerLoaded(queuedLoad.chunkX, queuedLoad.chunkZ)) {
                // yup, all we needed.
                this.onChunkSendReady(queuedLoad.chunkX, queuedLoad.chunkZ);
            }
        }
    }

    public void tickMidTick() {
        // try to send more chunks
        this.trySendChunks();

        // try to queue more chunks to load
        this.tryLoadChunks();
    }

    static final class ChunkPriorityHolder {
        public final int chunkX;
        public final int chunkZ;
        public final int manhattanDistanceToPlayer;
        public final double priority;

        public ChunkPriorityHolder(final int chunkX, final int chunkZ, final int manhattanDistanceToPlayer, final double priority) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.manhattanDistanceToPlayer = manhattanDistanceToPlayer;
            this.priority = priority;
        }
    }

    public static final class PlayerLoaderData {

        protected static final float FOV = 110.0f;
        protected static final double PRIORITISED_DISTANCE = 12.0 * 16.0;

        // Player max sprint speed is approximately 8m/s
        protected static final double LOOK_PRIORITY_SPEED_THRESHOLD = (10.0/20.0) * (10.0/20.0);
        protected static final double LOOK_PRIORITY_YAW_DELTA_RECALC_THRESHOLD = 3.0f;

        protected double lastLocX = Double.NEGATIVE_INFINITY;
        protected double lastLocZ = Double.NEGATIVE_INFINITY;

        protected int lastChunkX;
        protected int lastChunkZ;

        // this is corrected so that 0 is along the positive x-axis
        protected float lastYaw = Float.NEGATIVE_INFINITY;

        protected int lastSendDistance = Integer.MIN_VALUE;
        protected int lastLoadDistance = Integer.MIN_VALUE;
        protected int lastTickDistance = Integer.MIN_VALUE;
        protected boolean usingLookingPriority;

        protected final ServerPlayerEntity player;
        protected final PlayerChunkLoader loader;

        // warning: modifications of this field must be aware that the loadQueue inside PlayerChunkLoader uses this field
        // in a comparator!
        protected final ArrayDeque<ChunkPriorityHolder> loadQueue = new ArrayDeque<>();
        protected final LongOpenHashSet sentChunks = new LongOpenHashSet();

        protected final TreeSet<ChunkPriorityHolder> sendQueue = new TreeSet<>((final ChunkPriorityHolder p1, final ChunkPriorityHolder p2) -> {
            final int distanceCompare = Integer.compare(p1.manhattanDistanceToPlayer, p2.manhattanDistanceToPlayer);
            if (distanceCompare != 0) {
                return distanceCompare;
            }

            final int coordinateXCompare = Integer.compare(p1.chunkX, p2.chunkX);
            if (coordinateXCompare != 0) {
                return coordinateXCompare;
            }

            return Integer.compare(p1.chunkZ, p2.chunkZ);
        });

        protected int sendViewDistance = -1;
        protected int loadViewDistance = -1;
        protected int tickViewDistance = -1;

        protected long nextChunkSendTarget;

        public PlayerLoaderData(final ServerPlayerEntity player, final PlayerChunkLoader loader) {
            this.player = player;
            this.loader = loader;
        }

        // these view distance methods are for api
        public int getTargetSendViewDistance() {
            final int tickViewDistance = this.tickViewDistance == -1 ? this.loader.getTickDistance() : this.tickViewDistance;
            final int loadViewDistance = Math.max(tickViewDistance + 1, this.loadViewDistance == -1 ? this.loader.getLoadDistance() : this.loadViewDistance);
            final int clientViewDistance = this.getClientViewDistance();
            final int sendViewDistance = Math.min(loadViewDistance, this.sendViewDistance == -1 ? (!TuinityConfig.playerAutoConfigureSendViewDistance || clientViewDistance == -1 ? this.loader.getSendDistance() : clientViewDistance + 1) : this.sendViewDistance);
            return sendViewDistance;
        }

        public void setTargetSendViewDistance(final int distance) {
            if (distance != -1 && (distance < MIN_VIEW_DISTANCE || distance > MAX_VIEW_DISTANCE + 1)) {
                throw new IllegalArgumentException(Integer.toString(distance));
            }
            this.sendViewDistance = distance;
        }

        public int getTargetNoTickViewDistance() {
            return (this.loadViewDistance == -1 ? this.getLoadDistance() : this.loadViewDistance) - 1;
        }

        public void setTargetNoTickViewDistance(final int distance) {
            if (distance != -1 && (distance < MIN_VIEW_DISTANCE || distance > MAX_VIEW_DISTANCE)) {
                throw new IllegalArgumentException(Integer.toString(distance));
            }
            this.loadViewDistance = distance == -1 ? -1 : distance + 1;
        }

        public int getTargetTickViewDistance() {
            return this.tickViewDistance == -1 ? this.loader.getTickDistance() : this.tickViewDistance;
        }

        public void setTargetTickViewDistance(final int distance) {
            if (distance < MIN_VIEW_DISTANCE || distance > MAX_VIEW_DISTANCE) {
                throw new IllegalArgumentException(Integer.toString(distance));
            }
            this.tickViewDistance = distance;
        }

        protected int getLoadDistance() {
            final int tickViewDistance = this.tickViewDistance == -1 ? this.loader.getTickDistance() : this.tickViewDistance;

            return Math.max(tickViewDistance + 1, this.loadViewDistance == -1 ? this.loader.getLoadDistance() : this.loadViewDistance);
        }

        public boolean hasSentChunk(final int chunkX, final int chunkZ) {
            return this.sentChunks.contains(MCUtil.getCoordinateKey(chunkX, chunkZ));
        }

        public void sendChunk(final int chunkX, final int chunkZ, final Runnable onChunkSend) {
            if (this.sentChunks.add(MCUtil.getCoordinateKey(chunkX, chunkZ))) {
                this.player.getLevel().getChunkSource().chunkMap.updateChunkTracking(this.player,
                        new ChunkPos(chunkX, chunkZ), new IPacket[2], false, true); // unloaded, loaded
                this.player.connection.connection.execute(onChunkSend);
            } else {
                throw new IllegalStateException();
            }
        }

        public void unloadChunk(final int chunkX, final int chunkZ) {
            if (this.sentChunks.remove(MCUtil.getCoordinateKey(chunkX, chunkZ))) {
                this.player.getLevel().getChunkSource().chunkMap.updateChunkTracking(this.player,
                        new ChunkPos(chunkX, chunkZ), null, true, false); // unloaded, loaded
            }
        }

        protected static boolean triangleIntersects(final double p1x, final double p1z, // triangle point
                                                    final double p2x, final double p2z, // triangle point
                                                    final double p3x, final double p3z, // triangle point

                                                    final double targetX, final double targetZ) { // point
            // from barycentric coordinates:
            // targetX = a*p1x + b*p2x + c*p3x
            // targetZ = a*p1z + b*p2z + c*p3z
            // 1.0     = a*1.0 + b*1.0 + c*1.0
            // where a, b, c >= 0.0
            // so, if any of a, b, c are less-than zero then there is no intersection.

            // d = ((p2z - p3z)(p1x - p3x) + (p3x - p2x)(p1z - p3z))
            // a = ((p2z - p3z)(targetX - p3x) + (p3x - p2x)(targetZ - p3z)) / d
            // b = ((p3z - p1z)(targetX - p3x) + (p1x - p3x)(targetZ - p3z)) / d
            // c = 1.0 - a - b

            final double d = (p2z - p3z)*(p1x - p3x) + (p3x - p2x)*(p1z - p3z);
            final double a = ((p2z - p3z)*(targetX - p3x) + (p3x - p2x)*(targetZ - p3z)) / d;

            if (a < 0.0 || a > 1.0) {
                return false;
            }

            final double b = ((p3z - p1z)*(targetX - p3x) + (p1x - p3x)*(targetZ - p3z)) / d;
            if (b < 0.0 || b > 1.0) {
                return false;
            }

            final double c = 1.0 - a - b;

            return c >= 0.0 && c <= 1.0;
        }

        public void remove() {
            this.loader.broadcastMap.remove(this.player);
            this.loader.loadMap.remove(this.player);
            this.loader.loadTicketCleanup.remove(this.player);
            this.loader.tickMap.remove(this.player);
        }

        protected int getClientViewDistance() {
            return this.player.clientViewDistance == null ? -1 : this.player.clientViewDistance.intValue();
        }

        public void update() {
            final int tickViewDistance = this.tickViewDistance == -1 ? this.loader.getTickDistance() : this.tickViewDistance;
            // load view cannot be less-than tick view + 1
            final int loadViewDistance = Math.max(tickViewDistance + 1, this.loadViewDistance == -1 ? this.loader.getLoadDistance() : this.loadViewDistance);
            // send view cannot be greater-than load view
            final int clientViewDistance = this.getClientViewDistance();
            final int sendViewDistance = Math.min(loadViewDistance, this.sendViewDistance == -1 ? (!TuinityConfig.playerAutoConfigureSendViewDistance || clientViewDistance == -1 ? this.loader.getSendDistance() : clientViewDistance + 1) : this.sendViewDistance);

            final double posX = this.player.getX();
            final double posZ = this.player.getZ();
            final float yaw = MCUtil.normalizeYaw(this.player.yRot + 90.0f); // mc yaw 0 is along the positive z axis, but obviously this is really dumb - offset so we are at positive x-axis

            // in general, we really only want to prioritise chunks in front if we know we're moving pretty fast into them.
            final boolean useLookPriority = TuinityConfig.playerFrustumPrioritisation && (this.player.getDeltaMovement().magnitudeXZSquared() > LOOK_PRIORITY_SPEED_THRESHOLD ||
                    this.player.abilities.flying);

            // make sure we're in the send queue
            this.loader.chunkSendWaitQueue.add(this);

            if (
                // has view distance stayed the same?
                    sendViewDistance == this.lastSendDistance
                            && loadViewDistance == this.lastLoadDistance
                            && tickViewDistance == this.lastTickDistance

                            && (this.usingLookingPriority ? (
                                    // has our block stayed the same (this also accounts for chunk change)?
                                    MathHelper.floor(this.lastLocX) == MathHelper.floor(posX)
                                    && MathHelper.floor(this.lastLocZ) == MathHelper.floor(posZ)
                            ) : (
                                    // has our chunk stayed the same
                                    (MathHelper.floor(this.lastLocX) >> 4) == (MathHelper.floor(posX) >> 4)
                                    && (MathHelper.floor(this.lastLocZ) >> 4) == (MathHelper.floor(posZ) >> 4)
                            ))

                            // has our decision about look priority changed?
                            && this.usingLookingPriority == useLookPriority

                            // if we are currently using look priority, has our yaw stayed within recalc threshold?
                            && (!this.usingLookingPriority || Math.abs(yaw - this.lastYaw) <= LOOK_PRIORITY_YAW_DELTA_RECALC_THRESHOLD)
            ) {
                // nothing we care about changed, so we're not re-calculating
                return;
            }

            final int centerChunkX = MathHelper.floor(posX) >> 4;
            final int centerChunkZ = MathHelper.floor(posZ) >> 4;

            this.player.needsChunkCenterUpdate = true;
            this.loader.broadcastMap.addOrUpdate(this.player, centerChunkX, centerChunkZ, sendViewDistance);
            this.player.needsChunkCenterUpdate = false;
            this.loader.loadMap.addOrUpdate(this.player, centerChunkX, centerChunkZ, loadViewDistance);
            this.loader.loadTicketCleanup.addOrUpdate(this.player, centerChunkX, centerChunkZ, loadViewDistance + 1);
            this.loader.tickMap.addOrUpdate(this.player, centerChunkX, centerChunkZ, tickViewDistance);

            if (sendViewDistance != this.lastSendDistance) {
                // update the view radius for client
                // note that this should be after the map calls because the client wont expect unload calls not in its VD
                // and it's possible we decreased VD here
                this.player.connection.send(new SUpdateViewDistancePacket(sendViewDistance - 1)); // client already expects the 1 radius neighbours, so subtract 1.
            }

            this.lastLocX = posX;
            this.lastLocZ = posZ;
            this.lastYaw = yaw;
            this.lastSendDistance = sendViewDistance;
            this.lastLoadDistance = loadViewDistance;
            this.lastTickDistance = tickViewDistance;
            this.usingLookingPriority = useLookPriority;

            this.lastChunkX = centerChunkX;
            this.lastChunkZ = centerChunkZ;

            // points for player "view" triangle:

            // obviously, the player pos is a vertex
            final double p1x = posX;
            final double p1z = posZ;

            // to the left of the looking direction
            final double p2x = PRIORITISED_DISTANCE * Math.cos(Math.toRadians(yaw + (double)(FOV / 2.0))) // calculate rotated vector
                    + p1x; // offset vector
            final double p2z = PRIORITISED_DISTANCE * Math.sin(Math.toRadians(yaw + (double)(FOV / 2.0))) // calculate rotated vector
                    + p1z; // offset vector

            // to the right of the looking direction
            final double p3x = PRIORITISED_DISTANCE * Math.cos(Math.toRadians(yaw - (double)(FOV / 2.0))) // calculate rotated vector
                    + p1x; // offset vector
            final double p3z = PRIORITISED_DISTANCE * Math.sin(Math.toRadians(yaw - (double)(FOV / 2.0))) // calculate rotated vector
                    + p1z; // offset vector

            // now that we have all of our points, we can recalculate the load queue

            final List<ChunkPriorityHolder> loadQueue = new ArrayList<>();

            // clear send queue, we are re-sorting
            this.sendQueue.clear();

            final int searchViewDistance = Math.max(loadViewDistance, sendViewDistance);

            for (int dx = -searchViewDistance; dx <= searchViewDistance; ++dx) {
                for (int dz = -searchViewDistance; dz <= searchViewDistance; ++dz) {
                    final int chunkX = dx + centerChunkX;
                    final int chunkZ = dz + centerChunkZ;
                    final int squareDistance = Math.max(Math.abs(dx), Math.abs(dz));

                    if (this.hasSentChunk(chunkX, chunkZ)) {
                        // already sent (which means it is also loaded)
                        continue;
                    }

                    final boolean loadChunk = squareDistance <= loadViewDistance;
                    final boolean sendChunk = squareDistance <= sendViewDistance;

                    final boolean prioritised = useLookPriority && triangleIntersects(
                            // prioritisation triangle
                            p1x, p1z, p2x, p2z, p3x, p3z,

                            // center of chunk
                            (double)((chunkX << 4) | 8), (double)((chunkZ << 4) | 8)
                    );


                    final int manhattanDistance = (Math.abs(dx) + Math.abs(dz));

                    final double priority;

                    if (squareDistance <= TuinityConfig.playerMinChunkLoadRadius) {
                        // priority should be negative, and we also want to order it from center outwards
                        // so we want (0,0) to be the smallest, and (minLoadedRadius,minLoadedRadius) to be the greatest
                        priority = -((2 * TuinityConfig.playerMinChunkLoadRadius + 1) - (dx + dz));
                    } else {
                        if (prioritised) {
                            // we don't prioritise these chunks above others because we also want to make sure some chunks
                            // will be loaded if the player changes direction
                            priority = (double)manhattanDistance / 6.0;
                        } else {
                            priority = (double)manhattanDistance;
                        }
                    }

                    final ChunkPriorityHolder holder = new ChunkPriorityHolder(chunkX, chunkZ, manhattanDistance, priority);

                    if (!this.loader.isChunkPlayerLoaded(chunkX, chunkZ)) {
                        if (loadChunk) {
                            loadQueue.add(holder);
                        }
                    } else {
                        // loaded but not sent: so queue it!
                        if (sendChunk) {
                            this.sendQueue.add(holder);
                        }
                    }
                }
            }

            loadQueue.sort((final ChunkPriorityHolder p1, final ChunkPriorityHolder p2) -> {
                return Double.compare(p1.priority, p2.priority);
            });

            // we're modifying loadQueue, must remove
            this.loader.chunkLoadQueue.remove(this);

            this.loadQueue.clear();
            this.loadQueue.addAll(loadQueue);

            // must re-add
            this.loader.chunkLoadQueue.add(this);
        }
    }
}
