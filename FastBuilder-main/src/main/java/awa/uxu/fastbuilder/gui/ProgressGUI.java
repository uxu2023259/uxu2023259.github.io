package awa.uxu.fastbuilder.gui;

import awa.uxu.fastbuilder.FastBuilderPro;
import awa.uxu.fastbuilder.build.BuildSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class ProgressGUI {

    public static final String TITLE = "§8建造进度";

    public static void open(Player player) {

        BuildSession session =
                FastBuilderPro.get()
                        .getActiveBuilds()
                        .get(player.getUniqueId());

        if (session == null) {
            player.sendMessage("§c你当前没有建造任务");
            return;
        }

        Inventory inv =
                Bukkit.createInventory(null, 27, TITLE);

        player.openInventory(inv);

        // 🔥 实时刷新任务
        new BukkitRunnable() {

            @Override
            public void run() {

                BuildSession current =
                        FastBuilderPro.get()
                                .getActiveBuilds()
                                .get(player.getUniqueId());

                // 如果任务不存在或玩家关闭GUI
                if (current == null
                        || player.getOpenInventory() == null
                        || !player.getOpenInventory()
                        .getTitle().equals(TITLE)) {

                    cancel();
                    return;
                }

                update(inv, current);
            }

        }.runTaskTimer(FastBuilderPro.get(), 0L, 10L);
    }

    private static void update(Inventory inv,
                               BuildSession session) {

        int placed = session.getPlaced();
        int total = session.getTotalBlocks();
        int percent = session.getPercent();

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();

        meta.setDisplayName("§a建造进度");
        meta.setLore(List.of(
                "§7进度: §e" + percent + "%",
                "§7已放置: §a" + placed,
                "§7总方块: §b" + total,
                "§7剩余: §c" + (total - placed)
        ));

        info.setItemMeta(meta);

        inv.setItem(13, info);

        ItemStack cancel =
                new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta =
                cancel.getItemMeta();

        cancelMeta.setDisplayName("§c取消建造");
        cancel.setItemMeta(cancelMeta);

        inv.setItem(22, cancel);
    }
}