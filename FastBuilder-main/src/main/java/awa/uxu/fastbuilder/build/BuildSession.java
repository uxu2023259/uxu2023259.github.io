package awa.uxu.fastbuilder.build;

import awa.uxu.fastbuilder.FastBuilderPro;
import awa.uxu.fastbuilder.gui.MaterialLackGUI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Door;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuildSession {

    public enum ContinueMode {
        BUILD,
        PASTE
    }

    private final Player player;
    private final Location baseLocation;
    private final int rotation;
    private final ContinueMode continueMode;

    private final int totalBlocks;
    private final List<BlockEntry> pendingEntries;
    private final List<PlacedRecord> placedRecords = new ArrayList<>();
    private final Map<BlockPosKey, BlockData> preBuildSnapshot = new HashMap<>();

    private int placed = 0;
    private BukkitRunnable task;
    private BossBar bossBar;

    private boolean paused = false;
    private boolean finished = false;
    private boolean undone = false;
    private boolean canceled = false;

    private long startedAt = 0L;
    private long endedAt = 0L;

    private record BlockPosKey(String world, int x, int y, int z) {
    }

    private static class PlacedRecord {
        private final Location location;
        private final BlockData expectedPlaced;
        private final Material consumedMaterial;

        private PlacedRecord(Location location,
                             BlockData expectedPlaced,
                             Material consumedMaterial) {
            this.location = location;
            this.expectedPlaced = expectedPlaced;
            this.consumedMaterial = consumedMaterial;
        }
    }

    public BuildSession(Player player,
                        StructureCache cache,
                        Location baseLocation,
                        int rotation,
                        ContinueMode continueMode) {

        this.player = player;
        this.baseLocation = baseLocation.clone();
        this.rotation = rotation;
        this.continueMode = continueMode;

        this.pendingEntries = new ArrayList<>(
                cache.getLayers()
                        .values()
                        .stream()
                        .flatMap(List::stream)
                        .map(this::normalizeEntry)
                        .filter(java.util.Objects::nonNull)
                        .toList()
        );

        addMissingPistonBasesForHeads();
        removeIncompleteDoors();

        this.totalBlocks = pendingEntries.size();
        capturePreBuildSnapshot();
    }

    public void start() {
        if (task != null) {
            return;
        }

        if (startedAt == 0L) {
            startedAt = System.currentTimeMillis();
        }

        bossBar = Bukkit.createBossBar("§a建造中...", BarColor.GREEN, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setProgress(0);

        runTask();
    }

    private void runTask() {
        int tickInterval = FastBuilderPro.get().getConfig().getInt("build.tick-interval");
        int blocksPerTick = Math.max(1, FastBuilderPro.get().getConfig().getInt("build.blocks-per-tick"));

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (paused || finished || undone) {
                    return;
                }

                int placedThisTick = 0;

                for (int i = 0; i < blocksPerTick; i++) {
                    BlockEntry next = findNextBuildableEntry();

                    if (next == null) {
                        if (pendingEntries.isEmpty()) {
                            finish();
                        } else {
                            pause();
                        }
                        return;
                    }

                    if (tryPlace(next)) {
                        placedThisTick++;
                    }
                }

                if (placedThisTick == 0 && !pendingEntries.isEmpty()) {
                    pause();
                    return;
                }

                updateBossBar();

                if (pendingEntries.isEmpty()) {
                    finish();
                }
            }
        };

        task.runTaskTimer(FastBuilderPro.get(), 0L, tickInterval);
    }

    private BlockEntry findNextBuildableEntry() {
        boolean creative = player.getGameMode() == GameMode.CREATIVE;

        Iterator<BlockEntry> iterator = pendingEntries.iterator();
        while (iterator.hasNext()) {
            BlockEntry entry = iterator.next();
            Location targetLoc = toWorldLocation(entry);
            Block targetBlock = targetLoc.getBlock();

            if (!targetBlock.getType().isAir()) {
                iterator.remove();
                continue;
            }

            if (creative) {
                iterator.remove();
                return entry;
            }

            Material required = getRequiredInventoryMaterial(entry.data.getMaterial());
            if (required == null || hasMaterial(required)) {
                iterator.remove();
                return entry;
            }
        }

        return null;
    }

    private boolean tryPlace(BlockEntry entry) {
        Location targetLoc = toWorldLocation(entry);
        Block targetBlock = targetLoc.getBlock();

        if (!targetBlock.getType().isAir()) {
            return false;
        }

        Material consumedMat = null;

        if (player.getGameMode() != GameMode.CREATIVE) {
            Material requiredMat = getRequiredInventoryMaterial(entry.data.getMaterial());

            if (requiredMat != null) {
                if (!removeOne(requiredMat)) {
                    pendingEntries.add(entry);
                    return false;
                }
                consumedMat = requiredMat;
            }
        }

        targetBlock.setBlockData(entry.data, false);
        placed++;

        placedRecords.add(new PlacedRecord(
                targetLoc.clone(),
                entry.data.clone(),
                consumedMat
        ));

        return true;
    }

    private BlockEntry normalizeEntry(BlockEntry entry) {
        Material material = entry.data.getMaterial();

        if (material == Material.MOVING_PISTON) {
            boolean sticky = entry.data.getAsString().contains("type=sticky");
            BlockData replacement = Bukkit.createBlockData(Material.PISTON_HEAD);
            copyFacing(entry.data, replacement);

            String raw = replacement.getAsString();
            if (sticky && !raw.contains("type=sticky")) {
                replacement = Bukkit.createBlockData(raw.replace("type=normal", "type=sticky"));
            }
            return new BlockEntry(entry.x, entry.y, entry.z, replacement);
        }

        return entry;
    }

    private void addMissingPistonBasesForHeads() {
        Set<String> occupied = new HashSet<>();
        for (BlockEntry entry : pendingEntries) {
            occupied.add(posKey(entry.x, entry.y, entry.z));
        }

        List<BlockEntry> toAdd = new ArrayList<>();

        for (BlockEntry entry : pendingEntries) {
            Material material = entry.data.getMaterial();
            if (material != Material.PISTON_HEAD && material != Material.MOVING_PISTON) {
                continue;
            }

            if (!(entry.data instanceof Directional directional)) {
                continue;
            }

            var facing = directional.getFacing();
            int baseX = entry.x - facing.getModX();
            int baseY = entry.y - facing.getModY();
            int baseZ = entry.z - facing.getModZ();
            String baseKey = posKey(baseX, baseY, baseZ);

            if (occupied.contains(baseKey)) {
                continue;
            }

            boolean sticky = entry.data.getAsString().contains("type=sticky");
            BlockData base = Bukkit.createBlockData(sticky ? Material.STICKY_PISTON : Material.PISTON);
            copyFacing(entry.data, base);

            toAdd.add(new BlockEntry(baseX, baseY, baseZ, base));
            occupied.add(baseKey);
        }

        pendingEntries.addAll(toAdd);
    }

    private String posKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private void copyFacing(BlockData source, BlockData target) {
        if (source instanceof Directional from && target instanceof Directional to) {
            to.setFacing(from.getFacing());
        }
    }

    private void removeIncompleteDoors() {
        Map<String, BlockEntry> lookup = new HashMap<>();
        for (BlockEntry entry : pendingEntries) {
            lookup.put(entry.x + "," + entry.y + "," + entry.z, entry);
        }

        pendingEntries.removeIf(entry -> {
            if (!(entry.data instanceof Door door)) {
                return false;
            }

            int otherY = door.getHalf() == Bisected.Half.BOTTOM ? entry.y + 1 : entry.y - 1;
            BlockEntry counterpart = lookup.get(entry.x + "," + otherY + "," + entry.z);
            return !(counterpart != null && counterpart.data instanceof Door);
        });
    }

    private Material getRequiredInventoryMaterial(Material blockMaterial) {
        if (blockMaterial == null) {
            return null;
        }

        if (blockMaterial.isItem()) {
            return blockMaterial;
        }

        return switch (blockMaterial) {
            case REDSTONE_WIRE -> Material.REDSTONE;
            case WALL_TORCH -> Material.TORCH;
            case SOUL_WALL_TORCH -> Material.SOUL_TORCH;
            case REDSTONE_WALL_TORCH -> Material.REDSTONE_TORCH;
            default -> null;
        };
    }

    private boolean hasMaterial(Material material) {
        if (material == null || !material.isItem()) {
            return false;
        }
        return player.getInventory().containsAtLeast(new ItemStack(material, 1), 1);
    }

    private boolean removeOne(Material material) {
        if (material == null || !material.isItem()) {
            return false;
        }

        ItemStack probe = new ItemStack(material, 1);
        if (!player.getInventory().containsAtLeast(probe, 1)) {
            return false;
        }

        Map<Integer, ItemStack> remain = player.getInventory().removeItem(probe);
        return remain.isEmpty();
    }

    private Location toWorldLocation(BlockEntry entry) {
        int rx = entry.x;
        int rz = entry.z;

        switch (rotation) {
            case 90 -> {
                rx = -entry.z;
                rz = entry.x;
            }
            case 180 -> {
                rx = -entry.x;
                rz = -entry.z;
            }
            case 270 -> {
                rx = entry.z;
                rz = -entry.x;
            }
            default -> {
            }
        }

        return baseLocation.clone().add(rx, entry.y, rz);
    }

    private void capturePreBuildSnapshot() {
        if (pendingEntries.isEmpty()) {
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockEntry entry : pendingEntries) {
            Location loc = toWorldLocation(entry);
            minX = Math.min(minX, loc.getBlockX());
            minY = Math.min(minY, loc.getBlockY());
            minZ = Math.min(minZ, loc.getBlockZ());
            maxX = Math.max(maxX, loc.getBlockX());
            maxY = Math.max(maxY, loc.getBlockY());
            maxZ = Math.max(maxZ, loc.getBlockZ());
        }

        // 扩一圈，回滚流体/红石传播造成的新变化
        minX--; minY--; minZ--;
        maxX++; maxY++; maxZ++;

        String world = player.getWorld().getName();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(player.getWorld(), x, y, z);
                    preBuildSnapshot.put(new BlockPosKey(world, x, y, z), loc.getBlock().getBlockData().clone());
                }
            }
        }
    }

    private void restorePreBuildSnapshot() {
        Set<BlockPosKey> restored = new HashSet<>();

        List<PlacedRecord> safeOrder = new ArrayList<>(placedRecords);
        safeOrder.sort(Comparator
                .comparing((PlacedRecord r) -> r.expectedPlaced.getMaterial().isSolid())
                .thenComparingInt(r -> -r.location.getBlockY()));

        for (PlacedRecord record : safeOrder) {
            BlockPosKey key = new BlockPosKey(
                    player.getWorld().getName(),
                    record.location.getBlockX(),
                    record.location.getBlockY(),
                    record.location.getBlockZ()
            );

            BlockData beforeData = preBuildSnapshot.get(key);
            if (beforeData == null) {
                continue;
            }

            Block block = record.location.getBlock();
            if (!block.getBlockData().matches(beforeData)) {
                block.setBlockData(beforeData, false);
            }
            restored.add(key);
        }

        for (Map.Entry<BlockPosKey, BlockData> entry : preBuildSnapshot.entrySet()) {
            BlockPosKey key = entry.getKey();
            if (!player.getWorld().getName().equals(key.world())) {
                continue;
            }

            if (restored.contains(key)) {
                continue;
            }

            Block block = player.getWorld().getBlockAt(key.x(), key.y(), key.z());
            block.setBlockData(entry.getValue(), false);
        }
    }

    private void pause() {
        if (paused || finished || undone) {
            return;
        }

        paused = true;

        if (task != null) {
            task.cancel();
            task = null;
        }

        player.sendMessage("§c材料不足，建造已暂停。请使用 /fb " + (continueMode == ContinueMode.BUILD ? "build" : "paste") + " 继续。");
        MaterialLackGUI.open(player, this);
    }

    public void resume() {
        if (finished || undone || !paused) {
            return;
        }

        paused = false;
        runTask();

        player.sendMessage("§a继续建造");
    }

    private void finish() {
        finished = true;

        if (task != null) {
            task.cancel();
            task = null;
        }

        applyFinalUpdates();
        endedAt = System.currentTimeMillis();

        if (bossBar != null) {
            bossBar.setProgress(1.0);
            bossBar.removeAll();
            bossBar = null;
        }

        FastBuilderPro.get().getActiveBuilds().remove(player.getUniqueId());
        player.sendMessage("§a建造完成！");
    }

    private void applyFinalUpdates() {
        for (PlacedRecord record : placedRecords) {
            Block block = record.location.getBlock();
            if (!block.getBlockData().matches(record.expectedPlaced)) {
                continue;
            }
            block.setBlockData(block.getBlockData(), true);
        }
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (!finished && !undone) {
            canceled = true;
            endedAt = System.currentTimeMillis();
        }

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        FastBuilderPro.get().getActiveBuilds().remove(player.getUniqueId());

        if (!finished && !undone) {
            player.sendMessage("§c建造已取消");
        }
    }

    public boolean undo() {
        if (undone || preBuildSnapshot.isEmpty()) {
            return false;
        }

        cancel();

        Map<Material, Integer> refund = new EnumMap<>(Material.class);

        List<PlacedRecord> safeOrder = new ArrayList<>(placedRecords);
        safeOrder.sort(Comparator
                .comparing((PlacedRecord r) -> r.expectedPlaced.getMaterial().isSolid())
                .thenComparingInt(r -> -r.location.getBlockY()));

        for (PlacedRecord record : safeOrder) {
            if (record.consumedMaterial == null) {
                continue;
            }

            Block current = record.location.getBlock();
            if (!current.getBlockData().matches(record.expectedPlaced)) {
                continue;
            }

            refund.put(record.consumedMaterial,
                    refund.getOrDefault(record.consumedMaterial, 0) + 1);
        }

        restorePreBuildSnapshot();

        for (Map.Entry<Material, Integer> entry : refund.entrySet()) {
            ItemStack stack = new ItemStack(entry.getKey(), entry.getValue());
            Map<Integer, ItemStack> left = player.getInventory().addItem(stack);

            if (!left.isEmpty()) {
                left.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
        }

        undone = true;
        endedAt = System.currentTimeMillis();
        placed = 0;
        pendingEntries.clear();
        placedRecords.clear();

        return true;
    }

    private void updateBossBar() {
        if (bossBar == null || totalBlocks <= 0) {
            return;
        }

        bossBar.setProgress(Math.min(1.0, placed * 1.0 / totalBlocks));
    }

    public int getPlaced() {
        return placed;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public int getPercent() {
        if (totalBlocks == 0) {
            return 0;
        }
        return (int) ((placed * 100.0) / totalBlocks);
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isUndone() {
        return undone;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public ContinueMode getContinueMode() {
        return continueMode;
    }

    public long getElapsedMillis() {
        if (startedAt == 0L) {
            return 0L;
        }
        long end = endedAt == 0L ? System.currentTimeMillis() : endedAt;
        return Math.max(0L, end - startedAt);
    }

    public Map<Material, Integer> getRemainingMissing() {
        Map<Material, Integer> needed = new EnumMap<>(Material.class);

        for (BlockEntry entry : pendingEntries) {
            Material mat = getRequiredInventoryMaterial(entry.data.getMaterial());
            if (mat == null || !mat.isItem()) {
                continue;
            }
            needed.put(mat, needed.getOrDefault(mat, 0) + 1);
        }

        Map<Material, Integer> owned = new EnumMap<>(Material.class);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            owned.put(item.getType(), owned.getOrDefault(item.getType(), 0) + item.getAmount());
        }

        Map<Material, Integer> missing = new EnumMap<>(Material.class);
        for (Map.Entry<Material, Integer> entry : needed.entrySet()) {
            int need = entry.getValue();
            int have = owned.getOrDefault(entry.getKey(), 0);
            if (need > have) {
                missing.put(entry.getKey(), need - have);
            }
        }

        return missing;
    }
}
