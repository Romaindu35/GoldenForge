package com.destroystokyo.paper.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.bukkit.craftbukkit.util.Waitable;
import org.goldenforge.scheduler.BukkitTask;
import org.spigotmc.AsyncCatcher;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class MCUtil {
    public static final ThreadPoolExecutor asyncExecutor = new ThreadPoolExecutor(
            0, 2, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactoryBuilder().setNameFormat("Paper Async Task Handler Thread - %1$d").build()
    );


    public static final ThreadPoolExecutor cleanerExecutor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactoryBuilder().setNameFormat("Paper Object Cleaner").build()
    );

    public static long getCoordinateKey(final Entity entity) {
        return ((long)(MCUtil.fastFloor(entity.getZ()) >> 4) << 32) | ((MCUtil.fastFloor(entity.getX()) >> 4) & 0xFFFFFFFFL);
    }

    public static long getCoordinateKey(final BlockPos blockPos) {
        return ((long)(blockPos.getZ() >> 4) << 32) | ((blockPos.getX() >> 4) & 0xFFFFFFFFL);
    }

    public static long getCoordinateKey(final ChunkPos pair) {
        return ((long)pair.z << 32) | (pair.x & 0xFFFFFFFFL);
    }

    public static long getCoordinateKey(final int x, final int z) {
        return ((long)z << 32) | (x & 0xFFFFFFFFL);
    }

    public static int getCoordinateX(final long key) {
        return (int)key;
    }

    public static int getCoordinateZ(final long key) {
        return (int)(key >>> 32);
    }

    public static int getChunkCoordinate(final double coordinate) {
        return MCUtil.fastFloor(coordinate) >> 4;
    }

    public static int getBlockCoordinate(final double coordinate) {
        return MCUtil.fastFloor(coordinate);
    }

    public static long getBlockKey(final int x, final int y, final int z) {
        return ((long)x & 0x7FFFFFF) | (((long)z & 0x7FFFFFF) << 27) | ((long)y << 54);
    }

    public static long getBlockKey(final BlockPos pos) {
        return ((long)pos.getX() & 0x7FFFFFF) | (((long)pos.getZ() & 0x7FFFFFF) << 27) | ((long)pos.getY() << 54);
    }

    public static long getBlockKey(final Entity entity) {
        return getBlockKey(getBlockCoordinate(entity.getX()), getBlockCoordinate(entity.getY()), getBlockCoordinate(entity.getZ()));
    }

    public static int fastFloor(double x) {
        int truncated = (int)x;
        return x < (double)truncated ? truncated - 1 : truncated;
    }

    public static boolean isMainThread() {
        return MinecraftServer.getServer().isSameThread();
    }

    public static void scheduleAsyncTask(Runnable run) {
        asyncExecutor.execute(run);
    }

    public static List<ChunkPos> getSpiralOutChunks(BlockPos blockposition, int radius) {
        List<ChunkPos> list = com.google.common.collect.Lists.newArrayList();

        list.add(new ChunkPos(blockposition.getX() >> 4, blockposition.getZ() >> 4));
        for (int r = 1; r <= radius; r++) {
            int x = -r;
            int z = r;

            // Iterates the edge of half of the box; then negates for other half.
            while (x <= r && z > -r) {
                list.add(new ChunkPos((blockposition.getX() + (x << 4)) >> 4, (blockposition.getZ() + (z << 4)) >> 4));
                list.add(new ChunkPos((blockposition.getX() - (x << 4)) >> 4, (blockposition.getZ() - (z << 4)) >> 4));

                if (x < r) {
                    x++;
                } else {
                    z--;
                }
            }
        }
        return list;
    }

    public static double distanceSq(BlockPos pos1, BlockPos pos2) {
        return distanceSq(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(distanceSq(x1, y1, z1, x2, y2, z2));
    }

    public static double distanceSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2);
    }

    public static float normalizeYaw(float f) {
        float f1 = f % 360.0F;

        if (f1 >= 180.0F) {
            f1 -= 360.0F;
        }

        if (f1 < -180.0F) {
            f1 += 360.0F;
        }

        return f1;
    }

    public static int getTicketLevelFor(ChunkStatus status) {
        // TODO make sure the constant `33` is correct on future updates. See getChunkAt(int, int, ChunkStatus, boolean)
        return 33 + ChunkStatus.getDistance(status);
    }

    public static int floor(double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    public static <T> Runnable registerCleaner(Object obj, T resource, java.util.function.Consumer<T> cleaner) {
        return registerCleaner(obj, () -> cleaner.accept(resource));
    }

    public static Runnable registerCleaner(Object obj, Runnable run) {
        // Wrap callback in its own method above or the lambda will leak object
        Runnable cleaner = makeCleanerCallback(run);
        co.aikar.cleaner.Cleaner.register(obj, cleaner);
        return cleaner;
    }

    private static Runnable makeCleanerCallback(Runnable run) {
        return once(() -> cleanerExecutor.execute(run));
    }

    public static Runnable once(Runnable run) {
        AtomicBoolean ran = new AtomicBoolean(false);
        return () -> {
            if (ran.compareAndSet(false, true)) {
                run.run();
            }
        };
    }

    public static BukkitTask scheduleTask(int ticks, Runnable runnable, String taskName) {
        return MinecraftServer.getServer().getScheduler().scheduleInternalTask(runnable, ticks, taskName);
    }

    private static Queue<Runnable> getProcessQueue() {
        return MinecraftServer.getServer().processQueue;
    }

    public static <T> T ensureMain(String reason, Supplier<T> run) {
        if (AsyncCatcher.enabled && Thread.currentThread() != MinecraftServer.getServer().serverThread) {
            if (reason != null) {
                new IllegalStateException("Asynchronous " + reason + "! Blocking thread until it returns ").printStackTrace();
            }
            Waitable<T> wait = new Waitable<T>() {
                @Override
                protected T evaluate() {
                    return run.get();
                }
            };
            getProcessQueue().add(wait);
            try {
                return wait.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        }
        return run.get();
    }

    // assumes the sets have the same comparator, and if this comparator is null then assume T is Comparable
    public static <T> void mergeSortedSets(final java.util.function.Consumer<T> consumer, final java.util.Comparator<? super T> comparator, final java.util.SortedSet<T>...sets) {
        final ObjectRBTreeSet<T> all = new ObjectRBTreeSet<>(comparator);
        // note: this is done in log(n!) ~ nlogn time. It could be improved if it were to mimic what mergesort does.
        for (java.util.SortedSet<T> set : sets) {
            if (set != null) {
                all.addAll(set);
            }
        }
        all.forEach(consumer);
    }

}
