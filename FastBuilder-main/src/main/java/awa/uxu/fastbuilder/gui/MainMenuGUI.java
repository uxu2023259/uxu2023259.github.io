package awa.uxu.fastbuilder.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class MainMenuGUI {

    public static final String TITLE = "§0§lFastBuilder 控制面板";

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        fill(inv, Material.GRAY_STAINED_GLASS_PANE);

        inv.setItem(10, create(Material.ARROW, "§6§l设置起点 pos1", List.of(
                "§7功能：设置选区第一个点", "§7教程：手持箭左键方块设置 pos1", "§7或直接点击本按钮执行 /fb pos1", "", "§e点击后会关闭菜单"
        )));
        inv.setItem(11, create(Material.TIPPED_ARROW, "§6§l设置终点 pos2", List.of(
                "§7功能：设置选区第二个点", "§7教程：手持箭右键方块设置 pos2", "§7或直接点击本按钮执行 /fb pos2", "", "§e点击后会关闭菜单"
        )));
        inv.setItem(12, create(Material.CHEST, "§b§l复制选区 copy", List.of(
                "§7功能：复制 pos1~pos2 的结构", "§7前置：必须先设置 pos1 和 pos2", "", "§e点击后会关闭菜单"
        )));

        inv.setItem(14, create(Material.BOOK, "§a§l预览结构 preview", List.of(
                "§7功能：打开结构列表（支持翻页）", "§7点击某个结构后执行预览", "§7默认角度为 0°", "", "§e点击后进入结构列表"
        )));
        inv.setItem(15, create(Material.BRICKS, "§a§l开始建造 build", List.of(
                "§7功能：打开结构列表（支持翻页）", "§7点击某个结构后开始建造流程", "§7默认角度为 0°，后续可 confirm", "", "§e点击后进入结构列表"
        )));
        inv.setItem(16, create(Material.SLIME_BLOCK, "§d§l粘贴已复制结构 paste", List.of(
                "§7功能：将 copy 的结构粘贴到前方", "§7默认角度为 0°", "", "§e点击后会关闭菜单"
        )));

        inv.setItem(28, create(Material.MAP, "§e§l查看进度 progress", List.of(
                "§7功能：查看当前建造进度", "§7可在进度界面取消建造", "", "§e点击后进入进度界面"
        )));
        inv.setItem(29, create(Material.BARRIER, "§c§l撤销上次建造 undo", List.of(
                "§7功能：撤销上次 build/paste", "§7不会返还玩家手动破坏部分的材料", "", "§e点击后会关闭菜单"
        )));
        inv.setItem(30, create(Material.OAK_SIGN, "§9§l保存结构 save", List.of(
                "§7功能：弹出告示牌输入结构名后保存", "§7前置：必须设置 pos1 和 pos2", "", "§e点击后关闭菜单并打开告示牌"
        )));

        inv.setItem(32, create(Material.CLOCK, "§d§l快速建造 quick", List.of(
                "§7功能：一步完成预览 + 待确认", "§7建议命令：/fb quick <结构> [角度]", "§7GUI 内可先点 build 选结构", "", "§e点击后会提示命令用法"
        )));
        inv.setItem(33, create(Material.LIME_WOOL, "§a§l确认开始 confirm", List.of(
                "§7功能：确认当前待确认预览并开工", "§7对应命令：/fb confirm", "", "§e点击后会关闭菜单"
        )));
        inv.setItem(34, create(Material.RED_WOOL, "§c§l取消预览 cancel", List.of(
                "§7功能：取消当前待确认预览", "§7对应命令：/fb cancel", "", "§e点击后会关闭菜单"
        )));

        inv.setItem(49, create(Material.KNOWLEDGE_BOOK, "§f§l完整使用说明", List.of(
                "§7推荐流程：", "§f1) 手持箭左/右键方块设置 pos1/pos2", "§f2) copy 复制", "§f3) quick 或 build 开始流程", "§f4) confirm 开工 / cancel 取消", "§f5) progress 查看进度，必要时 undo"
        )));

        player.openInventory(inv);
    }

    private static void fill(Inventory inv, Material material) {
        ItemStack glass = new ItemStack(material);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, glass);
        }
    }

    private static ItemStack create(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
