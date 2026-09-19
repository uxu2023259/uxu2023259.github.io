package awa.uxu.fastbuilder.gui;

import awa.uxu.fastbuilder.FastBuilderPro;
import awa.uxu.fastbuilder.build.BuildSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class MaterialLackGUI {

    public static final String TITLE = "§c材料不足";

    public static void open(Player player, BuildSession session) {

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        Map<Material, Integer> missing =
                session.getRemainingMissing();

        int slot = 0;

        for (var entry : missing.entrySet()) {

            if (slot >= 45)
                break;

            Material mat = entry.getKey();
            int amount = entry.getValue();

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName("§c缺少: §f" + mat.name());
            meta.setLore(List.of(
                    "§7还需要: §e" + amount
            ));

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // 继续按钮
        ItemStack resume = new ItemStack(Material.LIME_WOOL);
        var rMeta = resume.getItemMeta();
        rMeta.setDisplayName("§a继续建造");
        resume.setItemMeta(rMeta);
        inv.setItem(46, resume);

        // 取消按钮
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        var cMeta = cancel.getItemMeta();
        cMeta.setDisplayName("§c取消建造");
        cancel.setItemMeta(cMeta);
        inv.setItem(52, cancel);

        player.openInventory(inv);
    }
}