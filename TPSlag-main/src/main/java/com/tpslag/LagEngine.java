package awa.uxu.tpslag;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

public final class LagEngine {
    private final JavaPlugin plugin;
    private BukkitTask task;

    private double targetTps;
    private int workload;
    private long extraDelayNanos;

    // 控制器状态（用于平滑和抗抖动）
    private double filteredTps;
    private double errorIntegral;
    private double lastError;

    private World lagWorld;
    private int baseChunkX;
    private int baseChunkZ;
    private boolean active;
    private final Deque<ChangedBlock> changedBlocks = new ArrayDeque<>();

    public LagEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(double targetTps) {
        stop();
        this.targetTps = clamp(targetTps, 1.0, 20.0);

        this.filteredTps = 20.0D;
        this.errorIntegral = 0.0D;
        this.lastError = 0.0D;

        this.workload = baseWorkloadForTarget(this.targetTps);
        this.extraDelayNanos = baseDelayForTarget(this.targetTps);

        prepareLagChunks();
        if (lagWorld == null) {
            this.active = false;
            return;
        }

        this.active = true;
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickController, 1L, 1L);
    }

    public void stop() {
        active = false;
        if (task != null) {
            task.cancel();
            task = null;
        }

        restoreChangedBlocks();
        releaseLagChunks();

        workload = 0;
        extraDelayNanos = 0L;
        filteredTps = 20.0D;
        errorIntegral = 0.0D;
        lastError = 0.0D;
    }

    public boolean isRunning() {
        return active && task != null;
    }

    public String status() {
        if (!isRunning()) {
            return "LagEngine: stopped";
        }
        return "LagEngine: running (targetTps=" + String.format("%.2f", targetTps)
            + ", filteredTps=" + String.format("%.2f", filteredTps)
            + ", workload=" + workload
            + ", extraDelayMs=" + String.format("%.2f", extraDelayNanos / 1_000_000.0D)
            + ", area=" + operationPosition() + ")";
    }

    public String operationPosition() {
        if (lagWorld == null) {
            return "N/A";
        }
        int centerX = (baseChunkX << 4) + 8;
        int centerZ = (baseChunkZ << 4) + 8;
        int y = lagWorld.getHighestBlockYAt(centerX, centerZ) + 1;
        return lagWorld.getName() + " @ chunk(" + baseChunkX + "," + baseChunkZ + ") block(" + centerX + "," + y + "," + centerZ + ")";
    }

    private void tickController() {
        if (!active || lagWorld == null) {
            return;
        }

        double measuredTps = Bukkit.getServer().getTPS()[0];
        if (Double.isNaN(measuredTps) || measuredTps <= 0.0) {
            measuredTps = 20.0D;
        }

        // EMA 平滑读数，减少抖动控制。
        double alpha = 0.25D;
        filteredTps = filteredTps * (1.0D - alpha) + measuredTps * alpha;

        double error = filteredTps - targetTps; // >0 说明 TPS 偏高，需要加压
        double deadband = 0.05D;

        if (error > deadband) {
            errorIntegral = clamp(errorIntegral + error, 0.0D, 120.0D);
        } else if (error < -deadband) {
            // 防积分饱和：低于目标时快速泄放积分
            errorIntegral = Math.max(0.0D, errorIntegral * 0.75D + error);
        } else {
            errorIntegral *= 0.92D;
        }

        double derivative = error - lastError;
        lastError = error;

        // PI-D（偏保守），并偏向“宁可略低于目标，也尽量不高于目标”
        double kpMs = 2.10D;
        double kiMs = 0.030D;
        double kdMs = 0.90D;

        double correctionMs = kpMs * error + kiMs * errorIntegral + kdMs * derivative;

        // 上冲抑制：若超过目标阈值，直接附加补偿
        if (measuredTps > targetTps + 0.10D) {
            correctionMs += (measuredTps - targetTps) * 1.25D;
        }

        long desiredDelay = baseDelayForTarget(targetTps) + (long) (correctionMs * 1_000_000.0D);
        desiredDelay = clampLong(desiredDelay, 0L, 260_000_000L);

        // 低于目标时快速减压；高于目标时渐进加压，避免振荡
        long maxRisePerTick = 3_000_000L;
        long maxFallPerTick = 8_000_000L;
        long deltaDelay = desiredDelay - extraDelayNanos;
        if (deltaDelay > 0) {
            extraDelayNanos += Math.min(deltaDelay, maxRisePerTick);
        } else if (deltaDelay < 0) {
            extraDelayNanos -= Math.min(-deltaDelay, maxFallPerTick);
        }

        int desiredWorkload = baseWorkloadForTarget(targetTps)
            + (int) Math.round(Math.max(0.0D, error) * 165.0D)
            + (int) Math.round(errorIntegral * 2.2D);

        if (measuredTps < targetTps - 0.15D) {
            desiredWorkload -= 70;
        }

        desiredWorkload = clampInt(desiredWorkload, 1, 4_000);
        int deltaWork = desiredWorkload - workload;
        int maxWorkRise = 18;
        int maxWorkFall = 42;
        if (deltaWork > 0) {
            workload += Math.min(deltaWork, maxWorkRise);
        } else if (deltaWork < 0) {
            workload -= Math.min(-deltaWork, maxWorkFall);
        }

        restoreChangedBlocks();
        forceChunkWorkload();
        cpuBurn();
        allocationPressure();

        if (extraDelayNanos > 0 && active) {
            LockSupport.parkNanos(extraDelayNanos);
        }
    }

    private int baseWorkloadForTarget(double target) {
        int base = (int) Math.round((20.0D - target) * 95.0D);
        return clampInt(base, 30, 2_000);
    }

    private long baseDelayForTarget(double target) {
        long nanos = (long) (((20.0D - target) / 20.0D) * 34_000_000L);
        return clampLong(nanos, 0L, 160_000_000L);
    }

    private void prepareLagChunks() {
        String worldName = plugin.getConfig().getString("lag.world", "world");
        lagWorld = Bukkit.getWorld(worldName);
        if (lagWorld == null) {
            lagWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
        if (lagWorld == null) {
            return;
        }

        int offsetChunks = Math.max(64, plugin.getConfig().getInt("lag.chunk-offset", 512));
        int spawnChunkX = lagWorld.getSpawnLocation().getBlockX() >> 4;
        int spawnChunkZ = lagWorld.getSpawnLocation().getBlockZ() >> 4;
        baseChunkX = spawnChunkX + offsetChunks;
        baseChunkZ = spawnChunkZ + offsetChunks;

        for (int cx = baseChunkX - 1; cx <= baseChunkX + 1; cx++) {
            for (int cz = baseChunkZ - 1; cz <= baseChunkZ + 1; cz++) {
                lagWorld.setChunkForceLoaded(cx, cz, true);
                lagWorld.getChunkAt(cx, cz).load(true);
            }
        }

        markOperationCenter();
    }

    private void markOperationCenter() {
        if (lagWorld == null) {
            return;
        }
        int centerX = (baseChunkX << 4) + 8;
        int centerZ = (baseChunkZ << 4) + 8;
        int y = lagWorld.getHighestBlockYAt(centerX, centerZ) + 1;
        Block marker = lagWorld.getBlockAt(centerX, y, centerZ);
        changedBlocks.addLast(new ChangedBlock(lagWorld, centerX, y, centerZ, marker.getBlockData().clone()));
        marker.setType(Material.BEACON, false);
    }

    private void releaseLagChunks() {
        if (lagWorld == null) {
            return;
        }

        for (int cx = baseChunkX - 1; cx <= baseChunkX + 1; cx++) {
            for (int cz = baseChunkZ - 1; cz <= baseChunkZ + 1; cz++) {
                lagWorld.setChunkForceLoaded(cx, cz, false);
            }
        }

        lagWorld = null;
        baseChunkX = 0;
        baseChunkZ = 0;
    }

    private void forceChunkWorkload() {
        if (lagWorld == null) {
            return;
        }

        int blockWrites = Math.min(1200, 80 + workload * 2);
        int blockReads = Math.min(5000, 300 + workload * 6);
        int centerX = (baseChunkX << 4) + 8;
        int centerZ = (baseChunkZ << 4) + 8;

        for (int i = 0; i < blockReads; i++) {
            int rx = ThreadLocalRandom.current().nextInt(-24, 25);
            int rz = ThreadLocalRandom.current().nextInt(-24, 25);
            int x = centerX + rx;
            int z = centerZ + rz;
            int y = lagWorld.getHighestBlockYAt(x, z);
            lagWorld.getBlockAt(x, y, z).getType();
        }

        for (int i = 0; i < blockWrites; i++) {
            int rx = ThreadLocalRandom.current().nextInt(-24, 25);
            int rz = ThreadLocalRandom.current().nextInt(-24, 25);
            int x = centerX + rx;
            int z = centerZ + rz;
            int y = lagWorld.getHighestBlockYAt(x, z) + 1;
            Block block = lagWorld.getBlockAt(x, y, z);

            Material original = block.getType();
            if (original == Material.BEDROCK || original == Material.BARRIER || original == Material.END_PORTAL_FRAME) {
                continue;
            }

            changedBlocks.addLast(new ChangedBlock(lagWorld, x, y, z, block.getBlockData().clone()));
            block.setType((i % 2 == 0) ? Material.OBSERVER : Material.REDSTONE_LAMP, true);
        }
    }

    private void cpuBurn() {
        int loops = 40_000 + workload * 2_500;
        double value = 0.0D;
        for (int i = 1; i <= loops; i++) {
            value += Math.sqrt(i) * Math.sin(i * 0.0017D) * Math.cos(i * 0.0011D);
        }
        if (value == Double.MIN_VALUE) {
            plugin.getLogger().fine("noop");
        }
    }

    private void allocationPressure() {
        int allocLoops = Math.min(1800, 60 + workload * 2);
        for (int i = 0; i < allocLoops; i++) {
            byte[] bytes = new byte[4096];
            bytes[0] = (byte) i;
        }
    }

    private void restoreChangedBlocks() {
        while (!changedBlocks.isEmpty()) {
            ChangedBlock changed = changedBlocks.pollFirst();
            if (changed.world == null) {
                continue;
            }
            Block block = changed.world.getBlockAt(changed.x, changed.y, changed.z);
            block.setBlockData(changed.oldData, false);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class ChangedBlock {
        private final World world;
        private final int x;
        private final int y;
        private final int z;
        private final BlockData oldData;

        private ChangedBlock(World world, int x, int y, int z, BlockData oldData) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.oldData = oldData;
        }
    }
}
