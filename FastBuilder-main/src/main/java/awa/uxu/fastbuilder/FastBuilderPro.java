package awa.uxu.fastbuilder;

import awa.uxu.fastbuilder.build.BlockEntry;
import awa.uxu.fastbuilder.build.BuildSession;
import awa.uxu.fastbuilder.build.MaterialCalculator;
import awa.uxu.fastbuilder.build.StructureCache;
import awa.uxu.fastbuilder.command.FBCommand;
import awa.uxu.fastbuilder.file.StructureFileManager;
import awa.uxu.fastbuilder.gui.GUIListener;
import awa.uxu.fastbuilder.gui.MaterialLackGUI;
import awa.uxu.fastbuilder.log.OperationLogManager;
import awa.uxu.fastbuilder.preview.PreviewManager;
import awa.uxu.fastbuilder.selection.SelectionManager;
import awa.uxu.fastbuilder.selection.SelectionWandListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FastBuilderPro extends JavaPlugin {

    private static FastBuilderPro instance;

    private final SelectionManager selectionManager = new SelectionManager();
    private final PreviewManager previewManager = new PreviewManager();
    private final StructureFileManager fileManager = new StructureFileManager();
    private final OperationLogManager operationLogManager = new OperationLogManager(this);

    private final Map<UUID, StructureCache> copied = new HashMap<>();
    private final Map<UUID, BuildSession> activeBuilds = new HashMap<>();
    private final Map<UUID, Integer> pendingRotation = new HashMap<>();
    private final Map<UUID, BuildSession.ContinueMode> pendingModes = new HashMap<>();
    private final Map<UUID, BuildSession> lastBuilds = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        fileManager.loadAll();

        FBCommand fbCommand = new FBCommand();
        if (getCommand("fb") != null) {
            getCommand("fb").setExecutor(fbCommand);
            getCommand("fb").setTabCompleter(fbCommand);
        } else {
            getLogger().warning("命令 /fb 未在 plugin.yml 中注册");
        }

        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new SelectionWandListener(), this);
        getLogger().info("FastBuilderPro 已启用");
    }

    @Override
    public void onDisable() {
        for (BuildSession session : java.util.List.copyOf(activeBuilds.values())) {
            session.cancel();
        }
        activeBuilds.clear();

        selectionManager.clearAll();
        previewManager.stopAll();

        copied.clear();
        pendingRotation.clear();
        pendingModes.clear();
        lastBuilds.clear();

        Bukkit.getScheduler().cancelTasks(this);
        instance = null;
    }

    public static FastBuilderPro get() {
        return instance;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public PreviewManager getPreviewManager() {
        return previewManager;
    }

    public StructureFileManager getFileManager() {
        return fileManager;
    }


    public OperationLogManager getOperationLogManager() {
        return operationLogManager;
    }


    public void setCopiedStructure(UUID uuid, StructureCache cache) {
        copied.put(uuid, cache);
    }

    public StructureCache getCopiedStructure(UUID uuid) {
        return copied.get(uuid);
    }

    public Map<UUID, BuildSession> getActiveBuilds() {
        return activeBuilds;
    }

    public void setPendingRotation(UUID uuid, int rotation) {
        pendingRotation.put(uuid, rotation);
    }

    public Integer getPendingRotation(UUID uuid) {
        return pendingRotation.get(uuid);
    }


    public void setPendingMode(UUID uuid, BuildSession.ContinueMode mode) {
        pendingModes.put(uuid, mode);
    }


    public boolean hasEnoughMaterialsToBuild(Player player, StructureCache cache) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        Map<Material, Integer> required = new HashMap<>();
        cache.getLayers().values().forEach(layer -> {
            for (BlockEntry entry : layer) {
                Material requiredMat = toRequiredItem(entry.data.getMaterial());
                if (requiredMat == null || !requiredMat.isItem()) {
                    continue;
                }
                required.put(requiredMat, required.getOrDefault(requiredMat, 0) + 1);
            }
        });

        Map<Material, Integer> owned = MaterialCalculator.calculateOwned(player);
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            if (owned.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    private Material toRequiredItem(Material blockMaterial) {
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

    public void clearPending(Player player) {
        pendingRotation.remove(player.getUniqueId());
        pendingModes.remove(player.getUniqueId());
        previewManager.stopPreview(player);
    }

    public boolean confirmPending(Player player) {
        Integer rotation = pendingRotation.get(player.getUniqueId());
        if (rotation == null) {
            player.sendMessage("§c当前没有待确认的预览");
            return false;
        }

        StructureCache cache = copied.get(player.getUniqueId());
        if (cache == null) {
            player.sendMessage("§c没有可确认的结构，请先预览");
            return false;
        }

        if (!hasEnoughMaterialsToBuild(player, cache)) {
            player.sendMessage("§c材料不足，无法使用 /fb confirm 开始建造");
            return false;
        }

        startBuild(player, rotation);
        return true;
    }

    public boolean continueBuild(Player player) {
        BuildSession running = activeBuilds.get(player.getUniqueId());
        if (running != null && running.isPaused() && running.getContinueMode() == BuildSession.ContinueMode.BUILD) {
            running.resume();
            return true;
        }
        return false;
    }

    public boolean continuePaste(Player player) {
        BuildSession running = activeBuilds.get(player.getUniqueId());
        if (running != null && running.isPaused() && running.getContinueMode() == BuildSession.ContinueMode.PASTE) {
            running.resume();
            return true;
        }
        return false;
    }

    public void startBuild(Player player, int rotation) {
        BuildSession running = activeBuilds.get(player.getUniqueId());
        if (running != null) {
            MaterialLackGUI.open(player, running);
            return;
        }

        StructureCache cache = copied.get(player.getUniqueId());
        if (cache == null) {
            player.sendMessage("§c没有已复制结构");
            return;
        }

        int max = getConfig().getInt("build.max-blocks");
        if (cache.getTotalBlocks() > max) {
            player.sendMessage("§c结构过大，超过限制！");
            return;
        }

        Location base = previewManager.getPreviewBase(player.getUniqueId());
        if (base == null) {
            player.sendMessage("§c没有可用预览位置，请先执行预览");
            return;
        }

        previewManager.stopPreview(player);
        selectionManager.clear(player);

        BuildSession.ContinueMode mode = pendingModes.getOrDefault(player.getUniqueId(), BuildSession.ContinueMode.BUILD);
        BuildSession session = new BuildSession(player, cache, base, rotation, mode);
        activeBuilds.put(player.getUniqueId(), session);
        lastBuilds.put(player.getUniqueId(), session);
        session.start();

        pendingRotation.remove(player.getUniqueId());
        pendingModes.remove(player.getUniqueId());
    }

    public boolean undoLastBuild(Player player) {
        BuildSession active = activeBuilds.get(player.getUniqueId());

        if (active != null) {
            boolean ok = active.undo();
            if (ok) {
                player.sendMessage("§a已撤销当前建造，并返还可校验的已消耗材料");
                return true;
            }
        }

        BuildSession last = lastBuilds.get(player.getUniqueId());
        if (last == null || last.isUndone()) {
            player.sendMessage("§c没有可撤销的建造记录");
            return false;
        }

        boolean ok = last.undo();
        if (ok) {
            player.sendMessage("§a撤销完成，并返还可校验的已消耗材料");
            return true;
        }

        player.sendMessage("§c该建造记录无法撤销");
        return false;
    }

    public void reloadPlugin() {
        reloadConfig();
        fileManager.loadAll();
        getLogger().info("FastBuilderPro 已重载完成");
    }

    public void startPaste(Player player, int rotation) {
        BuildSession running = activeBuilds.get(player.getUniqueId());
        if (running != null) {
            MaterialLackGUI.open(player, running);
            return;
        }

        StructureCache cache = copied.get(player.getUniqueId());
        if (cache == null) {
            player.sendMessage("§c没有已复制结构");
            return;
        }

        Location base = calculateCenteredFrontBase(player, cache, rotation);

        previewManager.startPreview(player, cache, base, rotation);
        setPendingRotation(player.getUniqueId(), rotation);
        setPendingMode(player.getUniqueId(), BuildSession.ContinueMode.PASTE);

        int delay = getConfig().getInt("preview.seconds") * 20;
        Bukkit.getScheduler().runTaskLater(this, () -> awa.uxu.fastbuilder.gui.ConfirmGUI.open(player), delay);
    }

    public Location calculateCenteredFrontBase(Player player,
                                               StructureCache cache,
                                               int rotation) {
        RotatedBounds bounds = getRotatedBounds(cache, rotation);

        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        if (dir.lengthSquared() == 0) {
            dir = new Vector(0, 0, 1);
        }

        Location playerBlock = player.getLocation().getBlock().getLocation();

        double centerX = (bounds.minX + bounds.maxX) / 2.0;
        double centerZ = (bounds.minZ + bounds.maxZ) / 2.0;

        World world = player.getWorld();
        double baseX = playerBlock.getBlockX() - Math.floor(centerX);
        double baseY = playerBlock.getBlockY() - bounds.minY;
        double baseZ = playerBlock.getBlockZ() - Math.floor(centerZ);

        double minForward = getMinForwardDistance(cache, rotation, baseX, baseZ, playerBlock, dir);
        double safeDistance = 2.0;

        if (minForward < safeDistance) {
            double delta = safeDistance - minForward;
            baseX += dir.getX() * delta;
            baseZ += dir.getZ() * delta;
        }

        return new Location(world, Math.floor(baseX), Math.floor(baseY), Math.floor(baseZ));
    }

    private double getMinForwardDistance(StructureCache cache,
                                         int rotation,
                                         double baseX,
                                         double baseZ,
                                         Location playerBlock,
                                         Vector dir) {
        List<BlockEntry> entries = cache.getLayers().values().stream().flatMap(List::stream).toList();
        if (entries.isEmpty()) {
            return 0;
        }

        double minForward = Double.MAX_VALUE;

        for (BlockEntry entry : entries) {
            int rx = entry.x;
            int rz = entry.z;

            switch (((rotation % 360) + 360) % 360) {
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

            double worldX = baseX + rx;
            double worldZ = baseZ + rz;
            double dx = worldX - playerBlock.getBlockX();
            double dz = worldZ - playerBlock.getBlockZ();
            double forward = dx * dir.getX() + dz * dir.getZ();
            minForward = Math.min(minForward, forward);
        }

        return minForward;
    }

    private RotatedBounds getRotatedBounds(StructureCache cache, int rotation) {
        int normalized = ((rotation % 360) + 360) % 360;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        List<BlockEntry> entries = cache.getLayers().values().stream().flatMap(List::stream).toList();

        if (entries.isEmpty()) {
            return new RotatedBounds(0, 0, 0, 0, 0, 0);
        }

        for (BlockEntry entry : entries) {
            int rx = entry.x;
            int rz = entry.z;

            switch (normalized) {
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

            minX = Math.min(minX, rx);
            minY = Math.min(minY, entry.y);
            minZ = Math.min(minZ, rz);

            maxX = Math.max(maxX, rx);
            maxY = Math.max(maxY, entry.y);
            maxZ = Math.max(maxZ, rz);
        }

        return new RotatedBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private record RotatedBounds(int minX, int minY, int minZ,
                                 int maxX, int maxY, int maxZ) {
    }
}
