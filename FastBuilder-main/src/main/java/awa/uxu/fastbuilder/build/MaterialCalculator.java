package awa.uxu.fastbuilder.build;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MaterialCalculator {

    // 统计结构需要的材料
    public static Map<Material, Integer> calculateRequired(StructureCache cache) {

        Map<Material, Integer> map = new HashMap<>();

        cache.getLayers().values().forEach(layer -> {

            for (BlockEntry entry : layer) {

                Material mat = entry.data.getMaterial();

                map.put(mat, map.getOrDefault(mat, 0) + 1);
            }
        });

        return map;
    }

    // 统计玩家拥有的材料
    public static Map<Material, Integer> calculateOwned(Player player) {

        Map<Material, Integer> map = new HashMap<>();

        for (ItemStack item : player.getInventory().getContents()) {

            if (item == null) continue;

            map.put(item.getType(),
                    map.getOrDefault(item.getType(), 0)
                            + item.getAmount());
        }

        return map;
    }

    // 计算可建造百分比
    public static int calculateBuildablePercent(
            Map<Material, Integer> required,
            Map<Material, Integer> owned) {

        double minRatio = 1.0;

        for (var entry : required.entrySet()) {

            Material mat = entry.getKey();
            int need = entry.getValue();
            int have = owned.getOrDefault(mat, 0);

            double ratio = (double) have / need;

            minRatio = Math.min(minRatio, ratio);
        }

        return (int) (minRatio * 100);
    }
}