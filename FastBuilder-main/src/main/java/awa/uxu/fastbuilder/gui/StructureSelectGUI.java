package awa.uxu.fastbuilder.gui;

import awa.uxu.fastbuilder.FastBuilderPro;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StructureSelectGUI {

    public enum Mode {
        PREVIEW,
        BUILD
    }

    public static final String TITLE_PREVIEW = "§0§l选择结构 - 预览";
    public static final String TITLE_BUILD = "§0§l选择结构 - 建造";

    private static final Map<UUID, Integer> pages = new HashMap<>();
    private static final Map<UUID, Mode> modes = new HashMap<>();

    public static void open(Player player, Mode mode, int page) {
        List<String> names = new ArrayList<>(FastBuilderPro.get().getFileManager().getNames());
        names.sort(String::compareToIgnoreCase);

        if (names.isEmpty()) {
            player.sendMessage("§c当前没有可用结构文件");
            return;
        }

        int maxPage = Math.max(0, (names.size() - 1) / 45);
        int realPage = Math.max(0, Math.min(page, maxPage));

        pages.put(player.getUniqueId(), realPage);
        modes.put(player.getUniqueId(), mode);

        String title = mode == Mode.PREVIEW ? TITLE_PREVIEW : TITLE_BUILD;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int start = realPage * 45;
        int end = Math.min(start + 45, names.size());

        int slot = 0;
        for (int i = start; i < end; i++) {
            String name = names.get(i);
            String author = FastBuilderPro.get().getFileManager().getAuthor(name);
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + name + " §8[§f" + author + "§8]");
            meta.setLore(List.of(
                    "§7结构序号：§f" + (i + 1) + " / " + names.size(),
                    "§7当前页：§f" + (realPage + 1) + " / " + (maxPage + 1),
                    "§7模式：" + (mode == Mode.PREVIEW ? "§a预览" : "§b建造"),
                    "§7作者：§f" + author,
                    "", "§e点击选择此结构"
            ));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        if (realPage > 0) {
            inv.setItem(45, nav("§e← 上一页"));
        }
        inv.setItem(49, nav("§7返回主菜单"));
        if (realPage < maxPage) {
            inv.setItem(53, nav("§e下一页 →"));
        }

        player.openInventory(inv);
    }

    public static int getPage(UUID uuid) {
        return pages.getOrDefault(uuid, 0);
    }

    public static Mode getMode(UUID uuid) {
        return modes.getOrDefault(uuid, Mode.PREVIEW);
    }

    private static ItemStack nav(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
