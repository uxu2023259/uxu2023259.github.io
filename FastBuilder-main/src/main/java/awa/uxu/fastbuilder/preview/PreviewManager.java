package awa.uxu.fastbuilder.preview;

import awa.uxu.fastbuilder.FastBuilderPro;
import awa.uxu.fastbuilder.build.BlockEntry;
import awa.uxu.fastbuilder.build.StructureCache;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PreviewManager {

    private final Map<UUID, BukkitRunnable> previewTasks = new HashMap<>();
    private final Map<UUID, Location> previewBases = new HashMap<>();

    public void startPreview(Player player,
                             StructureCache cache,
                             Location base,
                             int rotation) {

        stopPreview(player);

        // 不再重复定义 base
        previewBases.put(player.getUniqueId(), base.clone());

        int normalizedRotation = ((rotation % 360) + 360) % 360;

        BukkitRunnable task = new BukkitRunnable() {

            @Override
            public void run() {

                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                Location frozen = previewBases.get(player.getUniqueId());
                if (frozen == null) return;

                for (List<BlockEntry> layer : cache.getLayers().values()) {

                    for (BlockEntry entry : layer) {

                        int x = entry.x;
                        int y = entry.y;
                        int z = entry.z;

                        int rx = x;
                        int rz = z;

                        switch (normalizedRotation) {
                            case 90 -> {
                                rx = -z;
                                rz = x;
                            }
                            case 180 -> {
                                rx = -x;
                                rz = -z;
                            }
                            case 270 -> {
                                rx = z;
                                rz = -x;
                            }
                        }

                        Location loc = frozen.clone().add(rx, y, rz);

                        loc.getWorld().spawnParticle(
                                Particle.END_ROD,
                                loc.getX() + 0.5,
                                loc.getY() + 0.5,
                                loc.getZ() + 0.5,
                                1,
                                0, 0, 0,
                                0
                        );
                    }
                }
            }
        };

        task.runTaskTimer(FastBuilderPro.get(), 0L, 15L);
        previewTasks.put(player.getUniqueId(), task);
    }

    public void stopPreview(Player player) {

        UUID uuid = player.getUniqueId();

        if (previewTasks.containsKey(uuid)) {
            previewTasks.get(uuid).cancel();
            previewTasks.remove(uuid);
        }

        previewBases.remove(uuid);
    }


    public void stopAll() {

        for (BukkitRunnable task : previewTasks.values()) {
            task.cancel();
        }

        previewTasks.clear();
        previewBases.clear();
    }
    public Location getPreviewBase(UUID uuid) {
        return previewBases.get(uuid);
    }
}