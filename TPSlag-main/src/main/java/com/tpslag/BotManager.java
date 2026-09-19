package awa.uxu.tpslag;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Real-entity bot simulator (no ArmorStand).
 */
public final class BotManager {
    private final JavaPlugin plugin;
    private final List<EntityBot> bots = new ArrayList<>();
    private BukkitTask movementTask;
    private BukkitTask cleanupTask;

    public BotManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int spawnBots(int count, double speed, int durationSeconds, String namePattern) {
        clearAllBots();

        List<Player> online = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (online.isEmpty()) {
            return 0;
        }

        int safeCount = Math.max(1, Math.min(count, 400));
        double safeSpeed = Math.max(0.2, Math.min(speed, 20.0));
        int safeDuration = Math.max(1, Math.min(durationSeconds, 3600));

        World world = online.get(0).getWorld();
        EntityType botType = parseType(plugin.getConfig().getString("bots.entity-type", "ZOMBIE"));

        for (int i = 0; i < safeCount; i++) {
            Player anchor = online.get(i % online.size());
            Location spawnLoc = anchor.getLocation().clone().add(
                ThreadLocalRandom.current().nextDouble(-6, 6),
                0,
                ThreadLocalRandom.current().nextDouble(-6, 6)
            );
            spawnLoc.setY(Math.max(spawnLoc.getY(), world.getHighestBlockYAt(spawnLoc) + 1.0D));

            LivingEntity entity = (LivingEntity) world.spawnEntity(spawnLoc, botType);
            entity.setCustomNameVisible(true);
            String botName = formatName(namePattern);
            entity.setCustomName(botName);
            entity.setSilent(true);
            entity.setCanPickupItems(false);
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true);
            entity.setAI(false);
            entity.setCollidable(true);

            bots.add(new EntityBot(entity.getUniqueId(), anchor.getUniqueId(), safeSpeed));
            announceBotJoin(botName);
        }

        movementTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::moveBots, 1L, 1L);
        cleanupTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::clearAllBots, safeDuration * 20L);
        return bots.size();
    }

    public void clearAllBots() {
        if (movementTask != null) {
            movementTask.cancel();
            movementTask = null;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        for (EntityBot bot : bots) {
            if (bot.entityId == null) {
                continue;
            }
            var entity = Bukkit.getEntity(bot.entityId);
            if (entity != null) {
                entity.remove();
            }
        }
        bots.clear();
    }

    private void moveBots() {
        if (bots.isEmpty()) {
            return;
        }

        List<EntityBot> dead = new ArrayList<>();
        for (EntityBot bot : bots) {
            var e = Bukkit.getEntity(bot.entityId);
            if (!(e instanceof LivingEntity entity) || !entity.isValid()) {
                dead.add(bot);
                continue;
            }

            Player anchor = Bukkit.getPlayer(bot.anchorPlayerId);
            if (anchor == null || !anchor.isOnline()) {
                continue;
            }

            Location target = anchor.getLocation().clone().add(
                ThreadLocalRandom.current().nextDouble(-3, 3),
                0,
                ThreadLocalRandom.current().nextDouble(-3, 3)
            );
            target.setY(Math.max(target.getY(), target.getWorld().getHighestBlockYAt(target) + 1.0D));

            Location current = entity.getLocation();
            Vector step = target.toVector().subtract(current.toVector());
            if (step.lengthSquared() > 0.0001D) {
                step.normalize().multiply(bot.speed / 20.0D);
                Location next = current.add(step);
                next.setY(Math.max(next.getY(), next.getWorld().getHighestBlockYAt(next) + 1.0D));
                entity.teleport(next);
            }

            // 附加一点交互压：模拟 bot 持续尝试方块交互。
            for (int i = 0; i < 3; i++) {
                int dx = ThreadLocalRandom.current().nextInt(-1, 2);
                int dz = ThreadLocalRandom.current().nextInt(-1, 2);
                target.getWorld().getBlockAt(target.getBlockX() + dx, target.getBlockY() - 1, target.getBlockZ() + dz).getType();
            }
        }

        bots.removeAll(dead);
    }

    private EntityType parseType(String name) {
        try {
            EntityType type = EntityType.valueOf(name.toUpperCase());
            if (type.isSpawnable() && type.isAlive()) {
                return type;
            }
        } catch (Exception ignored) {
        }
        return EntityType.ZOMBIE;
    }

    private void announceBotJoin(String botName) {
        String message = resolveJoinMessageFromHook(botName);
        if (message == null || message.isBlank()) {
            String template = plugin.getConfig().getString("bots.default-join-message", "§e%name joined the game");
            message = template.replace("%name", botName);
        }

        SimulatedBotJoinMessageEvent event = new SimulatedBotJoinMessageEvent(botName, message);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled() && event.getJoinMessage() != null && !event.getJoinMessage().isBlank()) {
            Bukkit.broadcastMessage(event.getJoinMessage());
        }
    }

    private String resolveJoinMessageFromHook(String botName) {
        RegisteredServiceProvider<BotJoinMessageProvider> registration = Bukkit.getServicesManager()
            .getRegistration(BotJoinMessageProvider.class);
        if (registration == null) {
            return null;
        }
        BotJoinMessageProvider provider = registration.getProvider();
        if (provider == null) {
            return null;
        }
        return provider.resolveJoinMessage(botName);
    }

    private String formatName(String pattern) {
        int value = ThreadLocalRandom.current().nextInt(10_000);
        String random4 = String.format("%04d", value);
        return pattern.replace("%X", random4);
    }

    private static final class EntityBot {
        private final UUID entityId;
        private final UUID anchorPlayerId;
        private final double speed;

        private EntityBot(UUID entityId, UUID anchorPlayerId, double speed) {
            this.entityId = entityId;
            this.anchorPlayerId = anchorPlayerId;
            this.speed = speed;
        }
    }
}
