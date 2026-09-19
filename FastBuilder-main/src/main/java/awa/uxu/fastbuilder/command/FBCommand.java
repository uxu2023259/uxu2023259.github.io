package awa.uxu.fastbuilder.command;

import awa.uxu.fastbuilder.FastBuilderPro;
import awa.uxu.fastbuilder.file.StructureSaver;
import awa.uxu.fastbuilder.gui.ConfirmGUI;
import awa.uxu.fastbuilder.gui.ProgressGUI;
import awa.uxu.fastbuilder.gui.MainMenuGUI;
import awa.uxu.fastbuilder.gui.SaveInputManager;
import awa.uxu.fastbuilder.gui.StructureSelectGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class FBCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行");
            return true;
        }

        FastBuilderPro.get().getOperationLogManager().logFbCommand(player, args);

        if (args.length == 0) {
            MainMenuGUI.open(player);
            return true;
        }

        if (!player.hasPermission("fb.use") && !player.hasPermission("fb.*")) {
            player.sendMessage("§c你没有权限");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pos1" -> FastBuilderPro.get().getSelectionManager().setPos1(player);
            case "pos2" -> FastBuilderPro.get().getSelectionManager().setPos2(player);
            case "copy" -> {
                var cache = FastBuilderPro.get().getSelectionManager().copy(player);
                if (cache != null) {
                    FastBuilderPro.get().setCopiedStructure(player.getUniqueId(), cache);
                }
            }
            case "save" -> handleSave(player, args);
            case "menu", "gui" -> MainMenuGUI.open(player);
            case "list" -> handleList(player);
            case "build" -> handleBuildOrPreview(player, args, true);
            case "preview" -> handleBuildOrPreview(player, args, false);
            case "previewlist" -> StructureSelectGUI.open(player, StructureSelectGUI.Mode.PREVIEW, 0);
            case "buildlist" -> StructureSelectGUI.open(player, StructureSelectGUI.Mode.BUILD, 0);
            case "undo" -> FastBuilderPro.get().undoLastBuild(player);
            case "confirm" -> FastBuilderPro.get().confirmPending(player);
            case "cancel" -> {
                FastBuilderPro.get().clearPending(player);
                player.sendMessage("§e已取消待确认预览");
            }
            case "quick" -> handleQuickBuild(player, args);
            case "paste" -> {
                if (args.length < 2 && FastBuilderPro.get().continuePaste(player)) {
                    return true;
                }

                int rotation = parseRotation(args, 1, player);
                if (rotation != -1) {
                    FastBuilderPro.get().startPaste(player, rotation);
                }
            }
            case "progress" -> ProgressGUI.open(player);
            case "reload" -> {
                if (!player.hasPermission("fb.reload")) {
                    player.sendMessage("§c你没有权限");
                    return true;
                }

                FastBuilderPro.get().reloadPlugin();
                player.sendMessage("§aFastBuilderPro 重载完成！");
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleSave(Player player, String[] args) {
        if (args.length < 2) {
            player.closeInventory();
            SaveInputManager.openSignInput(player);
            return;
        }

        var sel = FastBuilderPro.get().getSelectionManager();
        var pos1 = sel.getPos1(player);
        var pos2 = sel.getPos2(player);

        if (pos1 == null || pos2 == null) {
            player.sendMessage("§c请先设置 pos1 和 pos2");
            return;
        }

        File file = new File(FastBuilderPro.get().getDataFolder() + "/str/" + args[1] + ".schem");
        boolean success = StructureSaver.save(pos1, pos2, file);

        if (success) {
            FastBuilderPro.get().getFileManager().setAuthor(args[1], player.getName());
            FastBuilderPro.get().reloadPlugin();
        }

        player.sendMessage(success ? "§a结构保存成功！作者: " + player.getName() : "§c保存失败");
    }

    private void handleList(Player player) {
        var names = FastBuilderPro.get().getFileManager().getNames();

        if (names.isEmpty()) {
            player.sendMessage("§c没有结构文件");
            return;
        }

        player.sendMessage("§a可用结构:");
        for (String name : names) {
            String author = FastBuilderPro.get().getFileManager().getAuthor(name);
            player.sendMessage(" §7- §e" + name + " §8(作者: §f" + author + "§8)");
        }
    }

    private void handleBuildOrPreview(Player player, String[] args, boolean withConfirm) {
        if (args.length < 2) {
            if (withConfirm && FastBuilderPro.get().continueBuild(player)) {
                return;
            }
            player.sendMessage(withConfirm ? "§c用法: /fb build <结构> [角度]" : "§c用法: /fb preview <结构> [角度]");
            return;
        }

        int rotation = parseRotation(args, 2, player);
        if (rotation == -1) {
            return;
        }

        var cache = FastBuilderPro.get().getFileManager().get(args[1]);

        if (cache == null) {
            player.sendMessage("§c结构不存在或格式不受支持");
            return;
        }

        FastBuilderPro.get().setCopiedStructure(player.getUniqueId(), cache);

        Location base = FastBuilderPro.get().calculateCenteredFrontBase(player, cache, rotation);

        FastBuilderPro.get().getPreviewManager().startPreview(player, cache, base, rotation);

        if (withConfirm) {
            FastBuilderPro.get().setPendingRotation(player.getUniqueId(), rotation);
            FastBuilderPro.get().setPendingMode(player.getUniqueId(), awa.uxu.fastbuilder.build.BuildSession.ContinueMode.BUILD);

            int delay = FastBuilderPro.get().getConfig().getInt("preview.seconds") * 20;
            Bukkit.getScheduler().runTaskLater(FastBuilderPro.get(), () -> ConfirmGUI.open(player), delay);
        }
    }

    private void handleQuickBuild(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /fb quick <结构> [角度]");
            return;
        }

        int rotation = parseRotation(args, 2, player);
        if (rotation == -1) {
            return;
        }

        var cache = FastBuilderPro.get().getFileManager().get(args[1]);
        if (cache == null) {
            player.sendMessage("§c结构不存在或格式不受支持");
            return;
        }

        FastBuilderPro plugin = FastBuilderPro.get();
        if (!plugin.hasEnoughMaterialsToBuild(player, cache)) {
            player.sendMessage("§c材料不足，无法使用 quick 快速建造");
            return;
        }

        plugin.setCopiedStructure(player.getUniqueId(), cache);
        Location base = plugin.calculateCenteredFrontBase(player, cache, rotation);
        plugin.getPreviewManager().startPreview(player, cache, base, rotation);
        plugin.setPendingRotation(player.getUniqueId(), rotation);
        plugin.setPendingMode(player.getUniqueId(), awa.uxu.fastbuilder.build.BuildSession.ContinueMode.BUILD);

        player.sendMessage("§a已生成预览：输入 §e/fb confirm §a立即开始，或 §e/fb cancel §a取消");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = List.of(
                    "pos1", "pos2", "copy", "save",
                    "list", "build", "preview", "quick",
                    "paste", "undo", "confirm", "cancel", "progress", "menu", "gui", "reload"
            );

            return subs.stream()
                    .filter(s -> player.hasPermission("fb." + s) || player.hasPermission("fb.*"))
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            if ((args[0].equalsIgnoreCase("build") && player.hasPermission("fb.build"))
                    || (args[0].equalsIgnoreCase("preview") && player.hasPermission("fb.preview"))
                    || (args[0].equalsIgnoreCase("quick") && player.hasPermission("fb.quick"))) {

                return FastBuilderPro.get().getFileManager().getNames().stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
            }

            if (args[0].equalsIgnoreCase("paste") && player.hasPermission("fb.paste")) {
                return List.of("0", "90", "180", "270").stream()
                        .filter(s -> s.startsWith(args[1]))
                        .toList();
            }
        }

        if (args.length == 3) {
            if ((args[0].equalsIgnoreCase("build") && player.hasPermission("fb.build"))
                    || (args[0].equalsIgnoreCase("preview") && player.hasPermission("fb.preview"))
                    || (args[0].equalsIgnoreCase("quick") && player.hasPermission("fb.quick"))) {

                return List.of("0", "90", "180", "270").stream()
                        .filter(s -> s.startsWith(args[2]))
                        .toList();
            }
        }

        return Collections.emptyList();
    }

    private void sendHelp(Player player) {
        player.sendMessage("§8§m--------------------------------");
        player.sendMessage("§a§lFastBuilderPro");
        player.sendMessage("§7快速复制与自动建造系统");
        player.sendMessage("");
        player.sendMessage("§e/fb build <结构> [角度] §7(暂停后可直接 /fb build 继续)");
        player.sendMessage("§e/fb paste <角度> §7(暂停后可直接 /fb paste 继续)");
        player.sendMessage("§e/fb preview <结构> [角度]");
        player.sendMessage("§e/fb quick <结构> [角度] §7(预览后用 /fb confirm 一键开始)");
        player.sendMessage("§e/fb save <名称>");
        player.sendMessage("§e/fb list");
        player.sendMessage("§e/fb progress");
        player.sendMessage("§e/fb undo");
        player.sendMessage("§e/fb confirm §7(确认当前预览并开始)");
        player.sendMessage("§e/fb cancel §7(取消当前预览)");
        player.sendMessage("§e/fb menu §7(打开中文功能面板)");
        player.sendMessage("§e/fb reload");
        player.sendMessage("");
        player.sendMessage("§7角度: §f0 / 90 / 180 / 270");
        player.sendMessage("§8§m--------------------------------");
    }

    private int parseRotation(String[] args,
                              int index,
                              Player player) {

        int rotation = 0;

        if (args.length > index) {
            try {
                rotation = Integer.parseInt(args[index]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c角度必须是数字");
                return -1;
            }
        }

        if (!(rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270)) {
            player.sendMessage("§c角度只能是 0/90/180/270");
            return -1;
        }

        return rotation;
    }
}
