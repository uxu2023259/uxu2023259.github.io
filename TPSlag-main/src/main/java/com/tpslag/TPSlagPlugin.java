package awa.uxu.tpslag;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class TPSlagPlugin extends JavaPlugin {
    private LagEngine lagEngine;
    private BotManager botManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.lagEngine = new LagEngine(this);
        this.botManager = new BotManager(this);

        registerCommand("tpslag", new TPSlagCommand(lagEngine));
        registerCommand("abot", new ABotCommand(this, botManager));

        if (getConfig().getBoolean("lag.enabled-on-startup", false)) {
            double targetTps = getConfig().getDouble("lag.startup-target-tps", 12.0D);
            lagEngine.start(targetTps);
            getLogger().info("LagEngine enabled on startup.");
        }
    }

    @Override
    public void onDisable() {
        if (lagEngine != null) {
            lagEngine.stop();
        }
        if (botManager != null) {
            botManager.clearAllBots();
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command " + name + " was not defined in plugin.yml");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }
}
