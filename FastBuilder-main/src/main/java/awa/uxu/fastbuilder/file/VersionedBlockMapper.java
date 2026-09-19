package awa.uxu.fastbuilder.file;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

public final class VersionedBlockMapper {

    private static final Map<String, String> LEGACY_NAME_MAP = new HashMap<>();

    static {
        // 1.13 flattening legacy names
        LEGACY_NAME_MAP.put("minecraft:grass", "minecraft:short_grass");
        LEGACY_NAME_MAP.put("minecraft:double_plant", "minecraft:tall_grass");
        LEGACY_NAME_MAP.put("minecraft:long_grass", "minecraft:short_grass");
        LEGACY_NAME_MAP.put("minecraft:stationary_water", "minecraft:water");
        LEGACY_NAME_MAP.put("minecraft:stationary_lava", "minecraft:lava");
        LEGACY_NAME_MAP.put("minecraft:lit_redstone_lamp", "minecraft:redstone_lamp");
        LEGACY_NAME_MAP.put("minecraft:lit_redstone_ore", "minecraft:redstone_ore");
        LEGACY_NAME_MAP.put("minecraft:powered_repeater", "minecraft:repeater");
        LEGACY_NAME_MAP.put("minecraft:unpowered_repeater", "minecraft:repeater");
        LEGACY_NAME_MAP.put("minecraft:powered_comparator", "minecraft:comparator");
        LEGACY_NAME_MAP.put("minecraft:unpowered_comparator", "minecraft:comparator");
        LEGACY_NAME_MAP.put("minecraft:wooden_door", "minecraft:oak_door");
        LEGACY_NAME_MAP.put("minecraft:wooden_slab", "minecraft:oak_slab");
        LEGACY_NAME_MAP.put("minecraft:wooden_pressure_plate", "minecraft:oak_pressure_plate");
        LEGACY_NAME_MAP.put("minecraft:wooden_button", "minecraft:oak_button");

        // newer renames
        LEGACY_NAME_MAP.put("minecraft:grass_path", "minecraft:dirt_path");
        LEGACY_NAME_MAP.put("minecraft:cave_air", "minecraft:air");
    }

    private VersionedBlockMapper() {
    }

    public static BlockData tryMap(String rawState) {
        if (rawState == null || rawState.isBlank()) {
            return null;
        }

        String cleaned = rawState.trim().replace('"', ' ').trim();
        String mapped = mapNamePreserveProperties(cleaned);

        try {
            return Bukkit.createBlockData(mapped);
        } catch (Exception ignored) {
            String withoutProperties = mapped.contains("[") ? mapped.substring(0, mapped.indexOf('[')) : mapped;
            try {
                return Bukkit.createBlockData(withoutProperties);
            } catch (Exception ignoredToo) {
                return null;
            }
        }
    }

    private static String mapNamePreserveProperties(String state) {
        String name = state;
        String properties = "";

        int idx = state.indexOf('[');
        if (idx >= 0) {
            name = state.substring(0, idx);
            properties = state.substring(idx);
        }

        String mappedName = LEGACY_NAME_MAP.getOrDefault(name, name);
        return mappedName + properties;
    }
}
