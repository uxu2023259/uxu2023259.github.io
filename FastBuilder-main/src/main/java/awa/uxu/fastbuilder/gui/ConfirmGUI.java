package awa.uxu.fastbuilder.gui;

import awa.uxu.fastbuilder.FastBuilderPro;
import awa.uxu.fastbuilder.build.MaterialCalculator;
import awa.uxu.fastbuilder.build.StructureCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class ConfirmGUI {

    public static final String TITLE = "§8材料确认";

    private static final Map<UUID, Integer> pages = new HashMap<>();

    public static int getPage(UUID uuid) {
        return pages.getOrDefault(uuid, 0);
    }

    public static void setPage(UUID uuid, int page) {
        if (page < 0) page = 0;
        pages.put(uuid, page);
    }

    public static void open(Player player) {
        open(player, pages.getOrDefault(player.getUniqueId(), 0));
    }

    public static void open(Player player, int page) {

        StructureCache cache =
                FastBuilderPro.get()
                        .getCopiedStructure(player.getUniqueId());

        if (cache == null) {
            player.sendMessage("§c没有已复制结构");
            return;
        }

        pages.put(player.getUniqueId(), page);

        Inventory inv =
                Bukkit.createInventory(null, 54, TITLE);

        Map<Material, Integer> required =
                MaterialCalculator.calculateRequired(cache);

        Map<Material, Integer> owned =
                MaterialCalculator.calculateOwned(player);

        List<Map.Entry<Material, Integer>> sorted =
                required.entrySet()
                        .stream()
                        .sorted((a, b) ->
                                Integer.compare(b.getValue(), a.getValue()))
                        .collect(Collectors.toList());

        int start = page * 45;
        int end = Math.min(start + 45, sorted.size());

        boolean hasNextPage = end < sorted.size(); // ⭐ 修复点

        int slot = 0;

        for (int i = start; i < end; i++) {

            var entry = sorted.get(i);
            Material mat = entry.getKey();

            if (!mat.isItem())
                continue;

            int need = entry.getValue();
            int have = owned.getOrDefault(mat, 0);

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName("§f" + mat.name());
            meta.setLore(List.of(
                    "§7需要: §e" + need,
                    "§7拥有: §a" + have
            ));

            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        // ===== 上一页 =====
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName("§e← 上一页");
            prev.setItemMeta(prevMeta);
            inv.setItem(45, prev);
        }

        // ===== 下一页 =====
        if (hasNextPage) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName("§e下一页 →");
            next.setItemMeta(nextMeta);
            inv.setItem(53, next);
        }

        // ===== 开始按钮 =====
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§l✔ 开始建造");
        confirmMeta.setLore(List.of(
                "§7按照当前预览位置施工",
                "",
                "§e点击确认开始"
        ));
        confirm.setItemMeta(confirmMeta);
        inv.setItem(46, confirm);

        // ===== 取消按钮 =====
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§l✖ 取消建造");
        cancelMeta.setLore(List.of(
                "§7取消本次建造",
                "§7并清除预览"
        ));
        cancel.setItemMeta(cancelMeta);
        inv.setItem(52, cancel);

        // ⭐ 你刚才漏掉的关键一步
        player.openInventory(inv);
    }
}