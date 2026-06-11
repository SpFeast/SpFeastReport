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

            String title = yaml.getString(basePath + ".title", item.title());
            boolean enabled = yaml.getBoolean(basePath + ".enabled", item.enabled());
            boolean saveLocation = yaml.getBoolean(basePath + ".save_location", item.saveLocation());
            String recordFile = yaml.getString(basePath + ".record_file", defaultRecordFile);
            String idPrefix = yaml.getString(basePath + ".id_prefix", defaultPrefix(item.title()));

            settingsByKey.put(
                    item.actionKey(),
                    new CategorySettings(
                            item.actionKey(),
                            title,
                            enabled,
                            saveLocation,
                            recordFile,
                            idPrefix
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
            String idPrefix
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
}
