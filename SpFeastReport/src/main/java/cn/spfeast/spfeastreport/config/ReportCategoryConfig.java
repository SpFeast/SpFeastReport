package cn.spfeast.spfeastreport.config;

import cn.spfeast.spfeastreport.SpFeastReportPlugin;
import cn.spfeast.spfeastreport.gui.ReportMenuLayout;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReportCategoryConfig {
    private static final String CONFIG_FILE_NAME = "categories.yml";
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z]+");

    private final SpFeastReportPlugin plugin;
    private final File configFile;
    private final Map<String, CategorySettings> settingsByKey = new LinkedHashMap<>();

    public ReportCategoryConfig(@NotNull SpFeastReportPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Failed to create plugin data folder.");
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        settingsByKey.clear();

        Instant now = Instant.now();
        yaml.set("meta.file_format", 2);
        yaml.set("meta.updated_at_iso", now.toString());
        yaml.set("meta.updated_at_display", DISPLAY_TIME_FORMAT.format(now));

        for (ReportMenuLayout.ReportMainItem item : ReportMenuLayout.ReportMainItem.values()) {
            if (item.type() != ReportMenuLayout.ReportMainItemType.REASON) {
                continue;
            }

            String basePath = "categories." + item.actionKey();
            String defaultRecordFile = "reports/" + item.actionKey() + ".yml";

            yaml.addDefault(basePath + ".title", item.title());
            yaml.addDefault(basePath + ".slot", item.slot());
            yaml.addDefault(basePath + ".material", item.material().name());
            yaml.addDefault(basePath + ".enabled", item.enabled());
            yaml.addDefault(basePath + ".save_location", item.saveLocation());
            yaml.addDefault(basePath + ".record_file", defaultRecordFile);
            yaml.addDefault(basePath + ".id_prefix", defaultPrefix(item.title()));
            yaml.addDefault(basePath + ".ban.template_key", "");
            yaml.addDefault(basePath + ".ban.duration", "");
            yaml.addDefault(basePath + ".ban.reason", "");
            yaml.addDefault(basePath + ".punishment.mode", defaultPunishmentMode(item.actionKey()));
            yaml.addDefault(basePath + ".punishment.key", "");
            yaml.addDefault(basePath + ".punishment.window_days", 30);
            yaml.addDefault(basePath + ".punishment.durations", List.of("7d", "30d", "90d", "180d", "360d"));
            yaml.addDefault(basePath + ".punishment.severity_boost_if_max_duration_at_least", "");

            String title = yaml.getString(basePath + ".title", item.title());
            boolean enabled = yaml.getBoolean(basePath + ".enabled", item.enabled());
            boolean saveLocation = yaml.getBoolean(basePath + ".save_location", item.saveLocation());
            String recordFile = yaml.getString(basePath + ".record_file", defaultRecordFile);
            String idPrefix = yaml.getString(basePath + ".id_prefix", defaultPrefix(item.title()));
            String banTemplateKey = yaml.getString(basePath + ".ban.template_key", "");
            String banDuration = yaml.getString(basePath + ".ban.duration", "");
            String banReason = yaml.getString(basePath + ".ban.reason", "");
            String punishmentMode = yaml.getString(basePath + ".punishment.mode", defaultPunishmentMode(item.actionKey()));
            String punishmentKey = yaml.getString(basePath + ".punishment.key", "");
            int punishmentWindowDays = Math.max(1, yaml.getInt(basePath + ".punishment.window_days", 30));
            List<String> punishmentDurations = yaml.getStringList(basePath + ".punishment.durations");
            String severityBoost = yaml.getString(basePath + ".punishment.severity_boost_if_max_duration_at_least", "");

            String normalizedMode = normalizeMode(punishmentMode, item.actionKey());
            String resolvedKey = blankToNull(punishmentKey);
            List<String> resolvedDurations = new ArrayList<>(punishmentDurations);

            if ("BAN".equals(normalizedMode)) {
                if (resolvedKey == null) {
                    resolvedKey = blankToNull(banTemplateKey);
                }
                if (resolvedDurations.isEmpty()) {
                    String legacyDuration = blankToNull(banDuration);
                    if (legacyDuration != null) {
                        resolvedDurations = List.of(legacyDuration);
                    }
                }
            }

            settingsByKey.put(
                    item.actionKey(),
                    new CategorySettings(
                            item.actionKey(),
                            title,
                            enabled,
                            saveLocation,
                            recordFile,
                            idPrefix,
                            normalizedMode,
                            resolvedKey,
                            punishmentWindowDays,
                            List.copyOf(resolvedDurations),
                            blankToNull(severityBoost),
                            blankToNull(banReason)
                    )
            );
        }

        yaml.options().copyDefaults(true);
        saveYaml(yaml);
    }

    public @NotNull CategorySettings getSettings(@NotNull ReportMenuLayout.ReportMainItem item) {
        return Objects.requireNonNull(
                settingsByKey.get(item.actionKey()),
                "Missing category settings for " + item.actionKey()
        );
    }

    public @NotNull Map<String, CategorySettings> getAllSettings() {
        return Map.copyOf(settingsByKey);
    }

    public File getConfigFile() {
        return configFile;
    }

    private void saveYaml(@NotNull YamlConfiguration yaml) {
        try {
            yaml.save(configFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save categories.yml: " + exception.getMessage());
        }
    }

    public record CategorySettings(
            String actionKey,
            String title,
            boolean enabled,
            boolean saveLocation,
            String recordFile,
            String idPrefix,
            String punishmentMode,
            String punishmentKey,
            int punishmentWindowDays,
            List<String> punishmentDurations,
            String severityBoostIfMaxDurationAtLeast,
            String banReason
    ) {
    }

    private static @NotNull String defaultPrefix(@NotNull String title) {
        Matcher matcher = WORD_PATTERN.matcher(title);
        StringBuilder builder = new StringBuilder();
        while (matcher.find() && builder.length() < 3) {
            builder.append(Character.toUpperCase(matcher.group().charAt(0)));
        }

        if (builder.isEmpty()) {
            return "REP";
        }

        if (builder.length() == 1) {
            String cleaned = title.replaceAll("[^A-Za-z]", "").toUpperCase();
            return cleaned.length() >= 3 ? cleaned.substring(0, 3) : (cleaned + "RP").substring(0, 3);
        }

        return builder.toString();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        return value.isBlank() ? null : value;
    }

    private static @NotNull String defaultPunishmentMode(@NotNull String actionKey) {
        String lower = actionKey.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("chat_")) {
            return "MUTE";
        }
        return "BAN";
    }

    private static @NotNull String normalizeMode(String mode, @NotNull String actionKey) {
        if (mode == null || mode.isBlank()) {
            return defaultPunishmentMode(actionKey);
        }
        String upper = mode.trim().toUpperCase(java.util.Locale.ROOT);
        if ("MUTE".equals(upper) || "BAN".equals(upper)) {
            return upper;
        }
        return defaultPunishmentMode(actionKey);
    }
}
