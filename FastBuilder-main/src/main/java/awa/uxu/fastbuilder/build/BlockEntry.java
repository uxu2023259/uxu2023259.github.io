package awa.uxu.fastbuilder.build;

import org.bukkit.block.data.BlockData;

public class BlockEntry {

    public final int x;
    public final int y;
    public final int z;
    public final BlockData data;

    public BlockEntry(int x, int y, int z, BlockData data) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.data = data.clone();
    }
}