package awa.uxu.fastbuilder.gui;

import awa.uxu.fastbuilder.FastBuilderPro;
import awa.uxu.fastbuilder.file.StructureSaver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SaveInputManager {

    private static final Map<UUID, Location> editingSigns = new HashMap<>();
    private static final Map<UUID, Material> originalTypes = new HashMap<>();

    public static void openSignInput(Player player) {
        Location signLoc = player.getLocation().getBlock().getLocation().add(0, 2, 0);
        Block block = signLoc.getBlock();

        editingSigns.put(player.getUniqueId(), signLoc);
        originalTypes.put(player.getUniqueId(), block.getType());

        block.setType(Material.OAK_SIGN, false);

        if (!(block.getState() instanceof Sign sign)) {
            player.sendMessage("§c打开告示牌输入失败，请重试");
            cleanup(player);
            return;
        }

        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(0, Component.text("§a请输入结构名"));
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(1, Component.text("§7仅限字母数字_"));
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(2, Component.empty());
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(3, Component.empty());
        sign.update(true, false);

        player.openSign(sign);
    }

    public static boolean handleSignSubmit(SignChangeEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Location loc = editingSigns.get(uuid);
        if (loc == null) {
            return false;
        }

        if (!event.getBlock().getLocation().equals(loc)) {
            return false;
        }

        String name = event.getLine(0) == null ? "" : event.getLine(0).trim();
        // 支持中文/字母/数字/下划线/短横线/空格，去除文件系统敏感字符
        name = name.replaceAll("[\\/:*?\"<>|]", "").trim();
        name = name.replaceAll("\\s+", " ");

        var sel = FastBuilderPro.get().getSelectionManager();
        var pos1 = sel.getPos1(player);
        var pos2 = sel.getPos2(player);

        if (pos1 == null || pos2 == null) {
            player.sendMessage("§c请先设置 pos1 和 pos2");
            cleanup(player);
            return true;
        }

        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            player.sendMessage("§c结构名不能为空，请重新操作");
            cleanup(player);
            return true;
        }

        File file = new File(FastBuilderPro.get().getDataFolder() + "/str/" + name + ".schem");
        boolean success = StructureSaver.save(pos1, pos2, file);
        if (success) {
            FastBuilderPro.get().getFileManager().setAuthor(name, player.getName());
            FastBuilderPro.get().reloadPlugin();
        }
        player.sendMessage(success ? "§a结构保存成功：" + name + " §7(作者: " + player.getName() + ")" : "§c保存失败");

        cleanup(player);
        return true;
    }

    public static void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = editingSigns.remove(uuid);
        Material old = originalTypes.remove(uuid);

        if (loc == null || old == null) {
            return;
        }

        Block block = loc.getBlock();
        block.setType(old, false);
    }
}
