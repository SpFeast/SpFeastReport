package cn.spfeast.spfeastreport.storage;

import cn.spfeast.spfeastreport.SpFeastReportPlugin;
import cn.spfeast.spfeastreport.config.ReportCategoryConfig;
import cn.spfeast.spfeastreport.gui.ReportMenuLayout;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReportStorage {
    private static final String REPORTS_FOLDER = "reports";
    private static final String INDEX_FILE_NAME = "index.yml";
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter ID_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMdd").withZone(ZoneId.systemDefault());

    private final SpFeastReportPlugin plugin;
    private final File reportsDirectory;
    private final File indexFile;

    public ReportStorage(@NotNull SpFeastReportPlugin plugin) {
        this.plugin = plugin;
        this.reportsDirectory = new File(plugin.getDataFolder(), REPORTS_FOLDER);
        this.indexFile = new File(reportsDirectory, INDEX_FILE_NAME);

        if (!reportsDirectory.exists() && !reportsDirectory.mkdirs()) {
            plugin.getLogger().warning("Failed to create reports directory.");
        }

        migrateCategoryFiles();
        rebuildIndex();
    }

    public synchronized @NotNull StoredReport saveReport(
            @NotNull Player reporter,
            @NotNull OfflinePlayer target,
            @NotNull ReportMenuLayout.ReportMainItem reportType
    ) throws IOException {
        ReportCategoryConfig.CategorySettings settings = plugin.getCategoryConfig().getSettings(reportType);
        File categoryFile = new File(plugin.getDataFolder(), settings.recordFile());
        File parentFile = categoryFile.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            plugin.getLogger().warning("Failed to create report category directory: " + parentFile.getAbsolutePath());
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(categoryFile);
        Instant now = Instant.now();
        List<Map<String, Object>> reports = readNormalizedReports(yaml, settings);

        int reportNumber = reports.size() + 1;
        String reportId = buildReportId(settings.idPrefix(), now, reportNumber);

        Map<String, Object> reportEntry = new LinkedHashMap<>();
        reportEntry.put("report_number", reportNumber);
        reportEntry.put("report_id", reportId);
        reportEntry.put("category_key", settings.actionKey());
        reportEntry.put("category_title", settings.title());
        reportEntry.put("created_at_iso", now.toString());
        reportEntry.put("created_at_display", DISPLAY_TIME_FORMAT.format(now));
        reportEntry.put("created_at_epoch_millis", now.toEpochMilli());

        Map<String, Object> reporterSection = new LinkedHashMap<>();
        reporterSection.put("name", reporter.getName());
        reporterSection.put("uuid", reporter.getUniqueId().toString());
        reportEntry.put("reporter", reporterSection);

        Map<String, Object> targetSection = new LinkedHashMap<>();
        targetSection.put("name", target.getName());
        targetSection.put("uuid", target.getUniqueId().toString());
        reportEntry.put("target", targetSection);

        if (settings.saveLocation()) {
            Map<String, Object> serverSection = new LinkedHashMap<>();
            serverSection.put("world", reporter.getWorld().getName());
            serverSection.put("x", reporter.getLocation().getBlockX());
            serverSection.put("y", reporter.getLocation().getBlockY());
            serverSection.put("z", reporter.getLocation().getBlockZ());
            reportEntry.put("server", serverSection);
        }

        reports.add(reportEntry);
        writeCategoryFile(yaml, settings, reports, categoryFile, now);
        rebuildIndex();

        return new StoredReport(reportId, categoryFile, reports.size());
    }

    public File getReportsDirectory() {
        return reportsDirectory;
    }

    public File getIndexFile() {
        return indexFile;
    }

    public synchronized @NotNull List<CategoryOverview> getCategoryOverviews(boolean includeReviewed) {
        List<CategoryOverview> overviews = new ArrayList<>();
        for (ReportMenuLayout.ReportMainItem item : ReportMenuLayout.ReportMainItem.values()) {
            if (item.type() != ReportMenuLayout.ReportMainItemType.REASON) {
                continue;
            }

            ReportCategoryConfig.CategorySettings settings = plugin.getCategoryConfig().getSettings(item);
            File categoryFile = new File(plugin.getDataFolder(), settings.recordFile());
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(categoryFile);
            List<Map<String, Object>> reports = readNormalizedReports(yaml, settings);
            int totalReports = includeReviewed ? reports.size() : activeReports(reports).size();
            overviews.add(new CategoryOverview(item, settings, totalReports));
        }
        return overviews;
    }

    public synchronized @NotNull List<ReportView> getReportsForCategory(
            @NotNull ReportMenuLayout.ReportMainItem item,
            boolean includeReviewed
    ) {
        if (item.type() != ReportMenuLayout.ReportMainItemType.REASON) {
            return List.of();
        }

        ReportCategoryConfig.CategorySettings settings = plugin.getCategoryConfig().getSettings(item);
        File categoryFile = new File(plugin.getDataFolder(), settings.recordFile());
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(categoryFile);
        List<Map<String, Object>> reports = readNormalizedReports(yaml, settings);
        if (!includeReviewed) {
            reports = activeReports(reports);
        }
        List<ReportView> views = new ArrayList<>();

        for (Map<String, Object> report : reports) {
            Map<String, Object> reporter = nestedSection(report.get("reporter"));
            Map<String, Object> target = nestedSection(report.get("target"));
            Map<String, Object> server = nestedSection(report.get("server"));
            Map<String, Object> review = nestedSection(report.get("review"));

            views.add(new ReportView(
                    stringValue(report.get("report_id")),
                    intValue(report.get("report_number")),
                    stringValue(report.get("category_key")),
                    stringValue(report.get("category_title")),
                    stringValue(report.get("created_at_iso")),
                    stringValue(report.get("created_at_display")),
                    longValue(report.get("created_at_epoch_millis")),
                    stringValue(reporter.get("name")),
                    stringValue(reporter.get("uuid")),
                    stringValue(target.get("name")),
                    stringValue(target.get("uuid")),
                    stringValue(server.get("world")),
                    numberValue(server.get("x")),
                    numberValue(server.get("y")),
                    numberValue(server.get("z")),
                    stringValue(review.get("status")),
                    stringValue(review.get("actor_name")),
                    stringValue(review.get("actor_uuid")),
                    stringValue(review.get("reviewed_at_iso")),
                    stringValue(review.get("reviewed_at_display"))
            ));
        }

        views.sort(Comparator.comparingLong(ReportView::createdAtEpochMillis).reversed());
        return views;
    }

    public synchronized @NotNull ReportUpdateResult markReportNoError(
            @NotNull ReportMenuLayout.ReportMainItem item,
            @NotNull String reportId,
            @NotNull Player actor
    ) throws IOException {
        if (item.type() != ReportMenuLayout.ReportMainItemType.REASON) {
            return ReportUpdateResult.NOT_FOUND;
        }

        ReportCategoryConfig.CategorySettings settings = plugin.getCategoryConfig().getSettings(item);
        File categoryFile = new File(plugin.getDataFolder(), settings.recordFile());
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(categoryFile);
        List<Map<String, Object>> reports = readNormalizedReports(yaml, settings);

        for (Map<String, Object> report : reports) {
            if (!reportId.equalsIgnoreCase(stringValue(report.get("report_id")))) {
                continue;
            }

            Map<String, Object> review = nestedSection(report.get("review"));
            String currentStatus = stringValue(review.get("status"));
            if ("no_error".equalsIgnoreCase(currentStatus)) {
                return ReportUpdateResult.ALREADY_MARKED;
            }

            Instant now = Instant.now();
            review.put("status", "no_error");
            review.put("actor_name", actor.getName());
            review.put("actor_uuid", actor.getUniqueId().toString());
            review.put("reviewed_at_iso", now.toString());
            review.put("reviewed_at_display", DISPLAY_TIME_FORMAT.format(now));
            report.put("review", review);

            writeCategoryFile(yaml, settings, reports, categoryFile, now);
            rebuildIndex();
            return ReportUpdateResult.UPDATED;
        }

        return ReportUpdateResult.NOT_FOUND;
    }

    private void migrateCategoryFiles() {
        for (ReportMenuLayout.ReportMainItem item : ReportMenuLayout.ReportMainItem.values()) {
            if (item.type() != ReportMenuLayout.ReportMainItemType.REASON) {
                continue;
            }

            ReportCategoryConfig.CategorySettings settings = plugin.getCategoryConfig().getSettings(item);
            File categoryFile = new File(plugin.getDataFolder(), settings.recordFile());
            if (!categoryFile.exists()) {
                continue;
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(categoryFile);
            List<Map<String, Object>> reports = readNormalizedReports(yaml, settings);
            try {
                writeCategoryFile(yaml, settings, reports, categoryFile, Instant.now());
            } catch (IOException exception) {
                plugin.getLogger().warning("Failed to migrate report file " + categoryFile.getName() + ": " + exception.getMessage());
            }
        }
    }

    private void rebuildIndex() {
        Instant now = Instant.now();
        YamlConfiguration index = new YamlConfiguration();
        index.set("meta.file_format", 2);
        index.set("meta.updated_at_iso", now.toString());
        index.set("meta.updated_at_display", DISPLAY_TIME_FORMAT.format(now));
        index.set("meta.categories_config", relativeToPluginData(plugin.getCategoryConfig().getConfigFile()));
        index.set("meta.reports_directory", relativeToPluginData(reportsDirectory));

        int totalReportsAll = 0;
        String lastReportId = null;
        long lastReportEpoch = -1L;

        Map<String, TargetSummary> targetSummaries = new LinkedHashMap<>();

        for (ReportMenuLayout.ReportMainItem item : ReportMenuLayout.ReportMainItem.values()) {
            if (item.type() != ReportMenuLayout.ReportMainItemType.REASON) {
                continue;
            }

            ReportCategoryConfig.CategorySettings settings = plugin.getCategoryConfig().getSettings(item);
            File categoryFile = new File(plugin.getDataFolder(), settings.recordFile());
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(categoryFile);
            List<Map<String, Object>> reports = readNormalizedReports(yaml, settings);
            String categoryPath = "categories." + settings.actionKey();

            index.set(categoryPath + ".title", settings.title());
            index.set(categoryPath + ".slot", item.slot());
            index.set(categoryPath + ".material", item.material().name());
            index.set(categoryPath + ".enabled", settings.enabled());
            index.set(categoryPath + ".save_location", settings.saveLocation());
            index.set(categoryPath + ".record_file", settings.recordFile());
            index.set(categoryPath + ".id_prefix", settings.idPrefix());
            index.set(categoryPath + ".total_reports", reports.size());

            if (!reports.isEmpty()) {
                Map<String, Object> lastReport = reports.get(reports.size() - 1);
                String reportId = stringValue(lastReport.get("report_id"));
                long epoch = longValue(lastReport.get("created_at_epoch_millis"));

                index.set(categoryPath + ".last_report_id", reportId);
                index.set(categoryPath + ".last_report_at_iso", stringValue(lastReport.get("created_at_iso")));
                index.set(categoryPath + ".last_report_at_display", stringValue(lastReport.get("created_at_display")));

                if (epoch > lastReportEpoch) {
                    lastReportEpoch = epoch;
                    lastReportId = reportId;
                }
            }

            totalReportsAll += reports.size();
            collectTargetSummaries(targetSummaries, reports, settings);
        }

        index.set("stats.total_reports_all", totalReportsAll);
        index.set("stats.last_report_id", lastReportId);
        if (lastReportEpoch > 0L) {
            Instant lastInstant = Instant.ofEpochMilli(lastReportEpoch);
            index.set("stats.last_report_at_iso", lastInstant.toString());
            index.set("stats.last_report_at_display", DISPLAY_TIME_FORMAT.format(lastInstant));
        }

        for (Map.Entry<String, TargetSummary> entry : targetSummaries.entrySet()) {
            String targetPath = "targets." + entry.getKey();
            TargetSummary summary = entry.getValue();

            index.set(targetPath + ".last_known_name", summary.lastKnownName);
            index.set(targetPath + ".total_reports", summary.totalReports);
            index.set(targetPath + ".last_report_id", summary.lastReportId);
            index.set(targetPath + ".last_report_at_iso", summary.lastReportAtIso);
            index.set(targetPath + ".last_report_at_display", summary.lastReportAtDisplay);

            for (Map.Entry<String, CategorySummary> categoryEntry : summary.categories.entrySet()) {
                String categoryPath = targetPath + ".categories." + categoryEntry.getKey();
                CategorySummary categorySummary = categoryEntry.getValue();
                index.set(categoryPath + ".title", categorySummary.title);
                index.set(categoryPath + ".count", categorySummary.count);
                index.set(categoryPath + ".last_report_id", categorySummary.lastReportId);
                index.set(categoryPath + ".last_report_at_iso", categorySummary.lastReportAtIso);
                index.set(categoryPath + ".last_report_at_display", categorySummary.lastReportAtDisplay);
            }
        }

        saveYaml(index, indexFile, "index.yml");
    }

    private void writeCategoryFile(
            @NotNull YamlConfiguration yaml,
            @NotNull ReportCategoryConfig.CategorySettings settings,
            @NotNull List<Map<String, Object>> reports,
            @NotNull File categoryFile,
            @NotNull Instant now
    ) throws IOException {
        yaml.set("meta.category_key", settings.actionKey());
        yaml.set("meta.category_title", settings.title());
        yaml.set("meta.file_format", 2);
        yaml.set("meta.updated_at_iso", now.toString());
        yaml.set("meta.updated_at_display", DISPLAY_TIME_FORMAT.format(now));
        yaml.set("meta.record_file", settings.recordFile());
        yaml.set("meta.save_location", settings.saveLocation());
        yaml.set("meta.id_prefix", settings.idPrefix());

        yaml.set("stats.total_reports", reports.size());
        if (!reports.isEmpty()) {
            Map<String, Object> lastReport = reports.get(reports.size() - 1);
            yaml.set("stats.last_report_id", stringValue(lastReport.get("report_id")));
            yaml.set("stats.last_report_number", intValue(lastReport.get("report_number")));
            yaml.set("stats.last_report_at_iso", stringValue(lastReport.get("created_at_iso")));
            yaml.set("stats.last_report_at_display", stringValue(lastReport.get("created_at_display")));
        } else {
            yaml.set("stats.last_report_id", null);
            yaml.set("stats.last_report_number", null);
            yaml.set("stats.last_report_at_iso", null);
            yaml.set("stats.last_report_at_display", null);
        }

        yaml.set("reports", null);
        for (Map<String, Object> report : reports) {
            String reportId = stringValue(report.get("report_id"));
            if (reportId != null) {
                yaml.set("reports." + reportId, report);
            }
        }
        yaml.save(categoryFile);
        formatCategoryFile(categoryFile);
    }

    private @NotNull List<Map<String, Object>> readNormalizedReports(
            @NotNull YamlConfiguration yaml,
            @NotNull ReportCategoryConfig.CategorySettings settings
    ) {
        List<Map<String, Object>> reports = new ArrayList<>();
        List<?> rawList = yaml.getList("reports");
        if (rawList != null) {
            int index = 1;
            for (Object element : rawList) {
                if (!(element instanceof Map<?, ?> rawMap)) {
                    continue;
                }
                reports.add(normalizeRecordMap(rawMap, settings, index++));
            }
            return reports;
        }

        ConfigurationSection section = yaml.getConfigurationSection("reports");
        if (section == null) {
            return reports;
        }

        List<Map<String, Object>> oldRecords = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection recordSection = section.getConfigurationSection(key);
            if (recordSection == null) {
                continue;
            }
            oldRecords.add(sectionToMap(recordSection));
        }

        oldRecords.sort(Comparator.comparingLong(map -> extractEpochMillis(map, settings)));
        int index = 1;
        for (Map<String, Object> oldRecord : oldRecords) {
            reports.add(normalizeRecordMap(oldRecord, settings, index++));
        }
        return reports;
    }

    private @NotNull String relativeToPluginData(@NotNull File file) {
        return plugin.getDataFolder().toPath().relativize(file.toPath()).toString().replace('\\', '/');
    }

    private void saveYaml(@NotNull YamlConfiguration yaml, @NotNull File file, @NotNull String displayName) {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save " + displayName + ": " + exception.getMessage());
        }
    }

    public record StoredReport(String reportId, File file, int totalReports) {
    }

    public record CategoryOverview(
            ReportMenuLayout.ReportMainItem item,
            ReportCategoryConfig.CategorySettings settings,
            int totalReports
    ) {
    }

    public record ReportView(
            String reportId,
            int reportNumber,
            String categoryKey,
            String categoryTitle,
            String createdAtIso,
            String createdAtDisplay,
            long createdAtEpochMillis,
            String reporterName,
            String reporterUuid,
            String targetName,
            String targetUuid,
            String world,
            Integer x,
            Integer y,
            Integer z,
            String reviewStatus,
            String reviewActorName,
            String reviewActorUuid,
            String reviewedAtIso,
            String reviewedAtDisplay
    ) {
        public String normalizedReviewStatus() {
            if (reviewStatus == null || reviewStatus.isBlank()) {
                return "pending";
            }
            return reviewStatus.toLowerCase(java.util.Locale.ROOT);
        }

        public boolean isPending() {
            return "pending".equals(normalizedReviewStatus());
        }

        public boolean hasLocation() {
            return world != null && x != null && y != null && z != null;
        }

        public boolean isMarkedNoError() {
            return "no_error".equals(normalizedReviewStatus());
        }

        public boolean isBanned() {
            return "banned".equals(normalizedReviewStatus());
        }
    }

    public enum ReportUpdateResult {
        UPDATED,
        ALREADY_MARKED,
        NOT_FOUND
    }

    private @NotNull Map<String, Object> normalizeRecordMap(
            @NotNull Map<?, ?> rawMap,
            @NotNull ReportCategoryConfig.CategorySettings settings,
            int fallbackNumber
    ) {
        Instant createdAt = parseInstant(firstNonNull(rawMap.get("created_at_iso"), rawMap.get("created_at")));
        long epochMillis = longValue(firstNonNull(rawMap.get("created_at_epoch_millis"), createdAt.toEpochMilli()));
        int reportNumber = intValue(firstNonNull(rawMap.get("report_number"), fallbackNumber));
        String reportId = stringValue(rawMap.get("report_id"));
        if (reportId == null || reportId.length() > 16 || reportId.contains("-") && reportId.split("-").length > 3) {
            reportId = buildReportId(settings.idPrefix(), createdAt, reportNumber);
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("report_number", reportNumber);
        normalized.put("report_id", reportId);
        normalized.put("category_key", settings.actionKey());
        normalized.put("category_title", settings.title());
        normalized.put("created_at_iso", createdAt.toString());
        normalized.put("created_at_display", DISPLAY_TIME_FORMAT.format(createdAt));
        normalized.put("created_at_epoch_millis", epochMillis);

        Map<String, Object> reporter = nestedSection(rawMap.get("reporter"));
        Map<String, Object> target = nestedSection(rawMap.get("target"));
        normalized.put("reporter", reporter);
        normalized.put("target", target);

        Object serverSection = rawMap.get("server");
        if (settings.saveLocation() && serverSection instanceof Map<?, ?> serverMap) {
            normalized.put("server", new LinkedHashMap<>(castMap(serverMap)));
        }

        Map<String, Object> review = nestedSection(rawMap.get("review"));
        if (!review.isEmpty()) {
            normalized.put("review", review);
        }

        return normalized;
    }

    private void collectTargetSummaries(
            @NotNull Map<String, TargetSummary> targetSummaries,
            @NotNull List<Map<String, Object>> reports,
            @NotNull ReportCategoryConfig.CategorySettings settings
    ) {
        for (Map<String, Object> report : reports) {
            Map<String, Object> target = nestedSection(report.get("target"));
            String targetUuid = stringValue(target.get("uuid"));
            if (targetUuid == null || targetUuid.isBlank()) {
                continue;
            }

            TargetSummary summary = targetSummaries.computeIfAbsent(targetUuid, ignored -> new TargetSummary());
            summary.lastKnownName = stringValue(target.get("name"));
            summary.totalReports += 1;
            summary.lastReportId = stringValue(report.get("report_id"));
            summary.lastReportAtIso = stringValue(report.get("created_at_iso"));
            summary.lastReportAtDisplay = stringValue(report.get("created_at_display"));

            CategorySummary categorySummary = summary.categories.computeIfAbsent(settings.actionKey(), ignored -> new CategorySummary());
            categorySummary.title = settings.title();
            categorySummary.count += 1;
            categorySummary.lastReportId = stringValue(report.get("report_id"));
            categorySummary.lastReportAtIso = stringValue(report.get("created_at_iso"));
            categorySummary.lastReportAtDisplay = stringValue(report.get("created_at_display"));
        }
    }

    private long extractEpochMillis(@NotNull Map<String, Object> rawMap, @NotNull ReportCategoryConfig.CategorySettings settings) {
        Object epochValue = rawMap.get("created_at_epoch_millis");
        if (epochValue != null) {
            return longValue(epochValue);
        }
        return parseInstant(firstNonNull(rawMap.get("created_at_iso"), rawMap.get("created_at"))).toEpochMilli();
    }

    private @NotNull String buildReportId(@NotNull String prefix, @NotNull Instant createdAt, int reportNumber) {
        return prefix.toUpperCase() + "-" + ID_DATE_FORMAT.format(createdAt) + "-" + String.format("%04d", reportNumber);
    }

    private @NotNull Instant parseInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException ignored) {
                try {
                    return ZonedDateTime.parse(text, DISPLAY_TIME_FORMAT).toInstant();
                } catch (DateTimeParseException ignoredToo) {
                    return Instant.now();
                }
            }
        }
        return Instant.now();
    }

    private static Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private static @NotNull Map<String, Object> nestedSection(Object value) {
        if (value instanceof ConfigurationSection section) {
            return sectionToMap(section);
        }
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>(castMap(map));
        }
        return new LinkedHashMap<>();
    }

    private static @NotNull Map<String, Object> sectionToMap(@NotNull ConfigurationSection section) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection childSection) {
                result.put(key, sectionToMap(childSection));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private static @NotNull Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private static @NotNull List<Map<String, Object>> activeReports(@NotNull List<Map<String, Object>> reports) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> report : reports) {
            Map<String, Object> review = nestedSection(report.get("review"));
            String status = stringValue(review.get("status"));
            if ("no_error".equalsIgnoreCase(status)) {
                continue;
            }
            filtered.add(report);
        }
        return filtered;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return 0L;
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return 0;
    }

    private static Integer numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return null;
    }

    private void formatCategoryFile(@NotNull File categoryFile) throws IOException {
        String content = Files.readString(categoryFile.toPath(), StandardCharsets.UTF_8);
        String newline = content.contains("\r\n") ? "\r\n" : "\n";
        String[] lines = content.split("\\R", -1);
        StringBuilder formatted = new StringBuilder();

        boolean insideReports = false;
        boolean seenFirstRecord = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if ("reports:".equals(line)) {
                insideReports = true;
                seenFirstRecord = false;
            } else if (insideReports && !line.isEmpty() && !line.startsWith(" ")) {
                insideReports = false;
            }

            boolean isReportEntry = insideReports && line.matches("^  [^\\s].*:$");
            if (isReportEntry && seenFirstRecord) {
                formatted.append(newline);
            }

            formatted.append(line);
            if (i < lines.length - 1) {
                formatted.append(newline);
            }

            if (isReportEntry) {
                seenFirstRecord = true;
            }
        }

        Files.writeString(categoryFile.toPath(), formatted.toString(), StandardCharsets.UTF_8);
    }

    private static final class TargetSummary {
        private String lastKnownName;
        private int totalReports;
        private String lastReportId;
        private String lastReportAtIso;
        private String lastReportAtDisplay;
        private final Map<String, CategorySummary> categories = new LinkedHashMap<>();
    }

    private static final class CategorySummary {
        private String title;
        private int count;
        private String lastReportId;
        private String lastReportAtIso;
        private String lastReportAtDisplay;
    }
}
