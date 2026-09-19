package awa.uxu.tpslag;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ABotCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final BotManager botManager;

    public ABotCommand(JavaPlugin plugin, BotManager botManager) {
        this.plugin = plugin;
        this.botManager = botManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            botManager.clearAllBots();
            sender.sendMessage("§a仿真 Bot 已清理");
            return true;
        }

        int count = readInt(args, 0, plugin.getConfig().getInt("bots.default-count", 20));
        double speed = readDouble(args, 1, plugin.getConfig().getDouble("bots.default-speed", 4.0));
        int duration = readInt(args, 2, plugin.getConfig().getInt("bots.default-duration-seconds", 30));
        String name = args.length >= 4 ? args[3] : plugin.getConfig().getString("bots.default-name", "Bot-%X");

        int spawned = botManager.spawnBots(count, speed, duration, name);
        if (spawned <= 0) {
            sender.sendMessage("§c没有在线玩家，无法创建仿真 Bot");
            return true;
        }

        sender.sendMessage("§a已创建 " + spawned + " 个仿真 Bot, 速度=" + speed + ", 持续=" + duration + "s, 名称模板=" + name);
        return true;
    }

    private int readInt(String[] args, int index, int fallback) {
        if (index >= args.length) {
            return fallback;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException ignored) {
            return fallback;
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
            return List.of("10", "30", "100", "stop");
        }
        if (args.length == 2) {
            return List.of("2", "4", "8");
        }
        if (args.length == 3) {
            return List.of("15", "30", "60");
        }
        if (args.length == 4) {
            return List.of("Bot-%X", "Atk-%X");
        }
        return List.of();
    }
}
