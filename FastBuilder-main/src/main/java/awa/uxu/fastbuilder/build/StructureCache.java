package awa.uxu.fastbuilder.build;

import java.util.*;

public class StructureCache {

    private final Map<Integer, List<BlockEntry>> layers = new TreeMap<>();
    private int totalBlocks = 0;

    public void addBlock(int y, BlockEntry entry) {
        layers.computeIfAbsent(y, k -> new ArrayList<>()).add(entry);
        totalBlocks++;
    }

    public Map<Integer, List<BlockEntry>> getLayers() {
        return layers;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }
}