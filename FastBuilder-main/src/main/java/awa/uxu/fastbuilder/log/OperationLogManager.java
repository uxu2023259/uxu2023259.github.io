package awa.uxu.fastbuilder.log;

import awa.uxu.fastbuilder.FastBuilderPro;
import awa.uxu.fastbuilder.build.BlockEntry;
import awa.uxu.fastbuilder.build.BuildSession;
import awa.uxu.fastbuilder.build.MaterialCalculator;
import awa.uxu.fastbuilder.build.StructureCache;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class OperationLogManager {

    private final FastBuilderPro plugin;
    private final File logFolder;
    private final SimpleDateFormat ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ROOT);

    public OperationLogManager(FastBuilderPro plugin) {
        this.plugin = plugin;
        this.logFolder = new File(plugin.getDataFolder(), "log");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
    }

    public void logFbCommand(Player player, String[] args) {
        try {
            String cmd = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
            String now = ts.format(new Date());

            String fileName = sanitize(player.getName()) + "_" + now + "_" + sanitize(cmd) + ".yml";
            File out = new File(logFolder, fileName);

            YamlConfiguration yml = new YamlConfiguration();

            yml.set("id.entity", player.getEntityId());
            yml.set("id.name", player.getName());
            yml.set("id.ip", player.getAddress() == null ? "unknown" : player.getAddress().getAddress().getHostAddress());
            yml.set("id.uuid", player.getUniqueId().toString());

            yml.set("time.epoch", System.currentTimeMillis());
            yml.set("time.formatted", new Date().toString());

            yml.set("command.raw", "/fb " + String.join(" ", args));
            yml.set("command.sub", cmd);
            yml.set("command.undo", "undo".equals(cmd));

            yml.set("player.location", formatLoc(player.getLocation()));

            var sel = plugin.getSelectionManager();
            Location p1 = sel.getPos1(player);
            Location p2 = sel.getPos2(player);
            if (p1 != null && p2 != null) {
                int dx = Math.abs(p1.getBlockX() - p2.getBlockX()) + 1;
                int dy = Math.abs(p1.getBlockY() - p2.getBlockY()) + 1;
                int dz = Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1;
                yml.set("selection.pos1", formatLoc(p1));
                yml.set("selection.pos2", formatLoc(p2));
                yml.set("selection.size", dx + "x" + dy + "x" + dz);
                yml.set("selection.volume", dx * dy * dz);
                yml.set("copy.from", formatRegion(p1, p2));
            }

            Location previewBase = plugin.getPreviewManager().getPreviewBase(player.getUniqueId());
            yml.set("paste.to", previewBase == null ? "none" : formatLoc(previewBase));

            StructureCache copied = plugin.getCopiedStructure(player.getUniqueId());
            if (copied != null) {
                yml.set("structure.totalBlocks", copied.getTotalBlocks());
                Map<Material, Integer> materials = MaterialCalculator.calculateRequired(copied);
                yml.set("structure.materials", materials.entrySet().stream()
                        .map(e -> e.getKey().name() + ":" + e.getValue()).toList());
            }

            BuildSession session = plugin.getActiveBuilds().get(player.getUniqueId());
            if (session != null) {
                yml.set("session.mode", session.getContinueMode().name());
                yml.set("session.paused", session.isPaused());
                yml.set("session.finished", session.isFinished());
                yml.set("session.canceled", session.isCanceled());
                yml.set("session.elapsedMs", session.getElapsedMillis());
            }

            yml.save(out);
        } catch (Exception ignored) {
        }
    }

    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }

    private String formatLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private String formatRegion(Location p1, Location p2) {
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        return p1.getWorld().getName() + ":[" + minX + "," + minY + "," + minZ + "]->[" + maxX + "," + maxY + "," + maxZ + "]";
    }
}
