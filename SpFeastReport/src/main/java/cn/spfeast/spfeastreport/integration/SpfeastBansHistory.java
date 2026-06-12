package cn.spfeast.spfeastreport.integration;

import cn.spfeast.spfeastreport.SpFeastReportPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class SpfeastBansHistory {
    private final SpFeastReportPlugin plugin;

    public SpfeastBansHistory(@NotNull SpFeastReportPlugin plugin) {
        this.plugin = plugin;
    }

    public @NotNull HistoryStats getBanStats(@NotNull UUID targetUuid, int windowDays) {
        return readStats(targetUuid, windowDays, historyFile("bans-history.yml"), true);
    }

    public @NotNull HistoryStats getMuteStats(@NotNull UUID targetUuid, int windowDays) {
        return readStats(targetUuid, windowDays, historyFile("mutes-history.yml"), false);
    }

    public @NotNull HistoryDetails getBanDetails(@NotNull UUID targetUuid, int windowDays, int limit) {
        return readDetails(targetUuid, windowDays, limit, historyFile("bans-history.yml"), true);
    }

    public @NotNull HistoryDetails getMuteDetails(@NotNull UUID targetUuid, int windowDays, int limit) {
        return readDetails(targetUuid, windowDays, limit, historyFile("mutes-history.yml"), false);
    }

    private File historyFile(@NotNull String fileName) {
        Plugin bans = plugin.getServer().getPluginManager().getPlugin("SpFeastBans");
        if (bans == null) {
            bans = plugin.getServer().getPluginManager().getPlugin("spfeastbans");
        }
        if (bans == null) {
            bans = plugin.getServer().getPluginManager().getPlugin("SpfeastBans");
        }
        if (bans == null) {
            return null;
        }

        return new File(new File(bans.getDataFolder(), "history"), fileName);
    }

    private @NotNull HistoryStats readStats(@NotNull UUID targetUuid, int windowDays, File file, boolean ban) {
        if (file == null || !file.exists()) {
            return HistoryStats.empty();
        }

        long now = System.currentTimeMillis();
        long sinceMillis = now - (long) Math.max(1, windowDays) * 86_400_000L;
        int count = 0;
        long maxDurationMillis = 0L;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String uuidText = section.getString("uuid");
            if (uuidText == null || uuidText.isBlank()) {
                continue;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidText);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            if (!targetUuid.equals(uuid)) {
                continue;
            }

            long createdAt = section.getLong("created-at", 0L);
            if (createdAt < sinceMillis) {
                continue;
            }

            long durationMillis = ban ? banDuration(section, createdAt) : muteDuration(section, createdAt);
            if (durationMillis > maxDurationMillis) {
                maxDurationMillis = durationMillis;
            }
            count++;
        }

        return new HistoryStats(count, maxDurationMillis);
    }

    private @NotNull HistoryDetails readDetails(@NotNull UUID targetUuid, int windowDays, int limit, File file, boolean ban) {
        if (file == null || !file.exists()) {
            return HistoryDetails.empty();
        }

        long now = System.currentTimeMillis();
        long sinceMillis = now - (long) Math.max(1, windowDays) * 86_400_000L;
        java.util.ArrayList<HistoryEntry> entries = new java.util.ArrayList<>();
        long maxDurationMillis = 0L;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String uuidText = section.getString("uuid");
            if (uuidText == null || uuidText.isBlank()) {
                continue;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidText);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            if (!targetUuid.equals(uuid)) {
                continue;
            }

            long createdAt = section.getLong("created-at", 0L);
            if (createdAt < sinceMillis) {
                continue;
            }

            long durationMillis = ban ? banDuration(section, createdAt) : muteDuration(section, createdAt);
            if (durationMillis > maxDurationMillis) {
                maxDurationMillis = durationMillis;
            }

            String typeKey = ban ? section.getString("template", "unknown") : section.getString("reason-key", "unknown");
            long expiresAt = section.getLong("expires-at", -1L);
            entries.add(new HistoryEntry(createdAt, expiresAt, durationMillis, typeKey));
        }

        entries.sort(Comparator.comparingLong(HistoryEntry::createdAtMillis).reversed());
        int capped = Math.max(0, Math.min(limit, entries.size()));
        List<HistoryEntry> recent = capped == 0 ? List.of() : List.copyOf(entries.subList(0, capped));
        return new HistoryDetails(entries.size(), maxDurationMillis, recent);
    }

    private long banDuration(@NotNull ConfigurationSection section, long createdAt) {
        long originalDuration = section.getLong("original-duration", -1L);
        if (originalDuration > 0L) {
            return originalDuration;
        }
        long expiresAt = section.getLong("expires-at", -1L);
        if (expiresAt > 0L && expiresAt > createdAt) {
            return expiresAt - createdAt;
        }
        return 0L;
    }

    private long muteDuration(@NotNull ConfigurationSection section, long createdAt) {
        long expiresAt = section.getLong("expires-at", 0L);
        if (expiresAt > createdAt) {
            return expiresAt - createdAt;
        }
        return 0L;
    }

    public record HistoryStats(int count, long maxDurationMillis) {
        public static @NotNull HistoryStats empty() {
            return new HistoryStats(0, 0L);
        }
    }

    public record HistoryEntry(long createdAtMillis, long expiresAtMillis, long durationMillis, @NotNull String key) {
    }

    public record HistoryDetails(int count, long maxDurationMillis, @NotNull List<HistoryEntry> recentEntries) {
        public static @NotNull HistoryDetails empty() {
            return new HistoryDetails(0, 0L, List.of());
        }
    }
}
