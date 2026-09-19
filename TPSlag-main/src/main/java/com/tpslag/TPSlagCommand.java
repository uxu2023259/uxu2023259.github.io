package awa.uxu.tpslag;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class TPSlagCommand implements CommandExecutor, TabCompleter {
    private final LagEngine lagEngine;

    public TPSlagCommand(LagEngine lagEngine) {
        this.lagEngine = lagEngine;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e用法: /tpslag <start|stop|status|pos> [tps]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                double tps = readDouble(args, 1, 12.0D);
                lagEngine.start(tps);
                if (!lagEngine.isRunning()) {
                    sender.sendMessage("§c启动失败：未找到可用世界，无法创建压测区域");
                    return true;
                }
                sender.sendMessage("§aTPS 压力器已启动: " + lagEngine.status());
                sender.sendMessage("§6操作位置: §f" + lagEngine.operationPosition());
                return true;
            }
            case "stop" -> {
                lagEngine.stop();
                sender.sendMessage("§aTPS 压力器已彻底停止（任务/区块强制加载/临时方块修改均已清理）");
                return true;
            }
            case "status" -> {
                sender.sendMessage("§b" + lagEngine.status());
                if (lagEngine.isRunning()) {
                    sender.sendMessage("§6操作位置: §f" + lagEngine.operationPosition());
                }
                return true;
            }
            case "pos" -> {
                sender.sendMessage("§6操作位置: §f" + lagEngine.operationPosition());
                return true;
            }
            default -> {
                sender.sendMessage("§c未知子命令: " + args[0]);
                return true;
            }
        }
    }

    private double readDouble(String[] args, int index, double fallback) {
        if (index >= args.length) {
            return fallback;
        }
        try {
            return Double.parseDouble(args[index]);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("start", "stop", "status", "pos");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return List.of("5", "10", "12", "15", "18");
        }
        return List.of();
    }
}
