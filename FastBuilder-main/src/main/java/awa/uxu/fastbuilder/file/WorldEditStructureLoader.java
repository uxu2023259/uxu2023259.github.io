package awa.uxu.fastbuilder.file;

import awa.uxu.fastbuilder.build.BlockEntry;
import awa.uxu.fastbuilder.build.StructureCache;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.block.data.BlockData;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class WorldEditStructureLoader {

    public static StructureCache load(File file, int rotation) {
        try {
            List<ClipboardFormat> formats = resolveFormats(file);
            for (ClipboardFormat format : formats) {
                try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                    Clipboard clipboard = reader.read();
                    StructureCache cache = fromClipboard(clipboard, rotation);
                    if (cache != null) {
                        return cache;
                    }
                } catch (Exception ignored) {
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static StructureCache load(File file) {
        return load(file, 0);
    }

    private static List<ClipboardFormat> resolveFormats(File file) {
        List<ClipboardFormat> formats = new ArrayList<>();

        ClipboardFormat byFile = ClipboardFormats.findByFile(file);
        if (byFile != null) {
            formats.add(byFile);
        }

        for (ClipboardFormat format : ClipboardFormats.getAll()) {
            if (!formats.contains(format)) {
                formats.add(format);
            }
        }

        return formats;
    }

    private static StructureCache fromClipboard(Clipboard clipboard, int rotation) {
        StructureCache cache = new StructureCache();

        BlockVector3 min = clipboard.getMinimumPoint();
        BlockVector3 max = clipboard.getMaximumPoint();

        int baseX = min.x();
        int baseY = min.y();
        int baseZ = min.z();

        int normalizedRotation = ((rotation % 360) + 360) % 360;

        for (int y = min.y(); y <= max.y(); y++) {
            for (int x = min.x(); x <= max.x(); x++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    BlockVector3 vec = BlockVector3.at(x, y, z);
                    var state = clipboard.getBlock(vec);

                    if (state.getBlockType().getMaterial().isAir()) {
                        continue;
                    }

                    BlockData blockData = adaptBlockState(state.toString(), state);
                    if (blockData == null) {
                        continue;
                    }

                    int relX = x - baseX;
                    int relY = y - baseY;
                    int relZ = z - baseZ;

                    int newX = relX;
                    int newZ = relZ;

                    switch (normalizedRotation) {
                        case 90 -> {
                            newX = -relZ;
                            newZ = relX;
                        }
                        case 180 -> {
                            newX = -relX;
                            newZ = -relZ;
                        }
                        case 270 -> {
                            newX = relZ;
                            newZ = -relX;
                        }
                        default -> {
                        }
                    }

                    cache.addBlock(relY, new BlockEntry(newX, relY, newZ, blockData));
                }
            }
        }

        return cache;
    }

    private static BlockData adaptBlockState(String rawState, com.sk89q.worldedit.world.block.BlockState worldEditState) {
        try {
            return BukkitAdapter.adapt(worldEditState);
        } catch (Exception ignored) {
            return VersionedBlockMapper.tryMap(rawState);
        }
    }
}
