package awa.uxu.fastbuilder.selection;

import awa.uxu.fastbuilder.build.BlockEntry;
import awa.uxu.fastbuilder.build.StructureCache;
import awa.uxu.fastbuilder.FastBuilderPro;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SelectionManager {

    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final Map<UUID, BukkitRunnable> particleTasks = new HashMap<>();

    /* ========== 设置选区 ========== */

    public void setPos1(Player player) {

        Location blockLoc = player.getLocation()
                .getBlock()
                .getRelative(BlockFace.DOWN)
                .getLocation()
                .clone();

        setPos1At(player, blockLoc);
    }

    public void setPos2(Player player) {

        Location blockLoc = player.getLocation()
                .getBlock()
                .getRelative(BlockFace.DOWN)
                .getLocation()
                .clone();

        setPos2At(player, blockLoc);
    }

    public void setPos1At(Player player, Location location) {
        if (location == null) {
            return;
        }

        pos1.put(player.getUniqueId(), location.getBlock().getLocation().clone());

        player.sendMessage("§a已设置 Pos1: §f"
                + location.getBlockX() + ", "
                + location.getBlockY() + ", "
                + location.getBlockZ());
        startParticleTask(player);
    }

    public void setPos2At(Player player, Location location) {
        if (location == null) {
            return;
        }

        pos2.put(player.getUniqueId(), location.getBlock().getLocation().clone());

        player.sendMessage("§a已设置 Pos2: §f"
                + location.getBlockX() + ", "
                + location.getBlockY() + ", "
                + location.getBlockZ());
        startParticleTask(player);
    }

    public boolean hasSelection(Player player) {
        return pos1.containsKey(player.getUniqueId())
                && pos2.containsKey(player.getUniqueId());
    }

    /* ========== 复制结构 ========== */

    public StructureCache copy(Player player) {

        if (!hasSelection(player)) {
            player.sendMessage("§c请先设置选区");
            return null;
        }

        Location l1 = pos1.get(player.getUniqueId());
        Location l2 = pos2.get(player.getUniqueId());

        int minX = Math.min(l1.getBlockX(), l2.getBlockX());
        int minY = Math.min(l1.getBlockY(), l2.getBlockY());
        int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ());

        int maxX = Math.max(l1.getBlockX(), l2.getBlockX());
        int maxY = Math.max(l1.getBlockY(), l2.getBlockY());
        int maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());

        StructureCache cache = new StructureCache();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {

                    Block block = l1.getWorld().getBlockAt(x, y, z);

                    if (block.getType() == Material.AIR)
                        continue;

                    BlockData data = block.getBlockData().clone();


                    BlockEntry entry = new BlockEntry(
                            x - minX,
                            y - minY,
                            z - minZ,
                            data
                    );

                    cache.addBlock(entry.y, entry);
                }
            }
        }

        player.sendMessage("§a复制完成，共 " + cache.getTotalBlocks() + " 方块");
        return cache;
    }
    public Location getPos1(Player player) {
        return pos1.get(player.getUniqueId());
    }

    public Location getPos2(Player player) {
        return pos2.get(player.getUniqueId());
    }
    public void clear(Player player) {

        UUID uuid = player.getUniqueId();

        pos1.remove(uuid);
        pos2.remove(uuid);

        if (particleTasks.containsKey(uuid)) {
            particleTasks.get(uuid).cancel();
            particleTasks.remove(uuid);
        }
    }

    public void clearAll() {

        for (BukkitRunnable task : particleTasks.values()) {
            task.cancel();
        }

        particleTasks.clear();
        pos1.clear();
        pos2.clear();
    }

    /* ========== 粒子边框 ========== */

    private void startParticleTask(Player player) {

        UUID uuid = player.getUniqueId();

        if (particleTasks.containsKey(uuid)) {
            particleTasks.get(uuid).cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {

                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (!hasSelection(player)) return;

                drawSelection(player);
            }
        };

        task.runTaskTimer(FastBuilderPro.get(), 0L, 10L);

        particleTasks.put(uuid, task);
    }

    private void drawSelection(Player player) {

        Location l1 = pos1.get(player.getUniqueId());
        Location l2 = pos2.get(player.getUniqueId());

        int minX = Math.min(l1.getBlockX(), l2.getBlockX());
        int minY = Math.min(l1.getBlockY(), l2.getBlockY());
        int minZ = Math.min(l1.getBlockZ(), l2.getBlockZ());

        int maxX = Math.max(l1.getBlockX(), l2.getBlockX());
        int maxY = Math.max(l1.getBlockY(), l2.getBlockY());
        int maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());

        World world = l1.getWorld();

        for (int x = minX; x <= maxX; x++) {
            spawn(world, x, minY, minZ);
            spawn(world, x, minY, maxZ);
            spawn(world, x, maxY, minZ);
            spawn(world, x, maxY, maxZ);
        }

        for (int y = minY; y <= maxY; y++) {
            spawn(world, minX, y, minZ);
            spawn(world, minX, y, maxZ);
            spawn(world, maxX, y, minZ);
            spawn(world, maxX, y, maxZ);
        }

        for (int z = minZ; z <= maxZ; z++) {
            spawn(world, minX, minY, z);
            spawn(world, maxX, minY, z);
            spawn(world, minX, maxY, z);
            spawn(world, maxX, maxY, z);
        }
    }

    private void spawn(World world, int x, int y, int z) {

        double offset = -0.05; // 轻微向外

        world.spawnParticle(
                Particle.DUST,
                x + 0.5 + offset,
                y + 0.5 + offset,
                z + 0.5 + offset,
                1,
                0, 0, 0,
                0,
                new Particle.DustOptions(
                        Color.fromRGB(0, 255, 0),
                        1.2f
                )
        );
    }
}