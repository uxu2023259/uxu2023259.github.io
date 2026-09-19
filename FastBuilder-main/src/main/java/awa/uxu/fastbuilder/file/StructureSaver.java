package awa.uxu.fastbuilder.file;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;

import java.io.File;
import java.io.FileOutputStream;

public class StructureSaver {

    public static boolean save(Location pos1,
                               Location pos2,
                               File file) {

        try {

            World world =
                    BukkitAdapter.adapt(pos1.getWorld());

            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

            BlockVector3 min =
                    BlockVector3.at(minX, minY, minZ);

            BlockVector3 max =
                    BlockVector3.at(maxX, maxY, maxZ);

            CuboidRegion region =
                    new CuboidRegion(world, min, max);

            BlockArrayClipboard clipboard =
                    new BlockArrayClipboard(region);

            clipboard.setOrigin(min);

            var editSession =
                    WorldEdit.getInstance()
                            .newEditSession(world);

            ForwardExtentCopy copy =
                    new ForwardExtentCopy(
                            editSession,
                            region,
                            clipboard,
                            min
                    );

            Operations.complete(copy);

            editSession.close();

            ClipboardFormat format =
                    BuiltInClipboardFormat.SPONGE_SCHEMATIC;

            try (ClipboardWriter writer =
                         format.getWriter(
                                 new FileOutputStream(file))) {

                writer.write(clipboard);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}