package awa.uxu.tpslag;

import org.jetbrains.annotations.Nullable;

/**
 * Optional service hook for other plugins.
 * Register this interface in Bukkit ServicesManager to provide custom bot join messages.
 */
public interface BotJoinMessageProvider {
    /**
     * @param botName simulated bot name
     * @return join message; return null to fallback to TPSlag defaults
     */
    @Nullable
    String resolveJoinMessage(String botName);
}
