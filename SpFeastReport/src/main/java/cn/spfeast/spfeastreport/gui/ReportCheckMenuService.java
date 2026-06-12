package cn.spfeast.spfeastreport.gui;

import cn.spfeast.spfeastreport.SpFeastReportPlugin;
import cn.spfeast.spfeastreport.permission.PermissionNodes;
import cn.spfeast.spfeastreport.storage.ReportStorage;
import cn.spfeast.spfeastreport.util.DurationText;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ReportCheckMenuService {
    private final SpFeastReportPlugin plugin;

    public ReportCheckMenuService(@NotNull SpFeastReportPlugin plugin) {
        this.plugin = plugin;
    }

    public void openCategoryMenu(@NotNull Player viewer) {
        openCategoryMenu(viewer, ReportCheckMenuHolder.RecordScope.PENDING_ONLY);
    }

    public void openCategoryMenu(@NotNull Player viewer, @NotNull ReportCheckMenuHolder.RecordScope scope) {
        ReportCheckMenuHolder holder = new ReportCheckMenuHolder(scope);
        ReportCheckMenuLayout.populateCategoryMenu(
                holder.getInventory(),
                plugin.getReportStorage().getCategoryOverviews(scope.includesReviewed()),
                scope
        );
        viewer.openInventory(holder.getInventory());
    }

    public boolean openCategoryReports(
            @NotNull Player viewer,
            @NotNull ReportMenuLayout.ReportMainItem category,
            int requestedPage,
            @NotNull ReportCheckMenuHolder.RecordScope scope
    ) {
        if (category.type() != ReportMenuLayout.ReportMainItemType.REASON) {
            viewer.sendMessage(Component.text("Unknown report category.", NamedTextColor.RED));
            return false;
        }

        List<ReportStorage.ReportView> reports = plugin.getReportStorage().getReportsForCategory(category, scope.includesReviewed());
        int totalPages = Math.max(1, (int) Math.ceil((double) reports.size() / ReportCheckMenuLayout.REPORT_ENTRY_SLOTS.length));
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));

        ReportCheckMenuHolder holder = new ReportCheckMenuHolder(scope, category, page, totalPages);
        ReportCheckMenuLayout.populateReportListMenu(holder.getInventory(), scope, category, reports, page, totalPages);
        viewer.openInventory(holder.getInventory());
        return true;
    }

    public @Nullable ReportStorage.ReportView getReportForSlot(
            @NotNull ReportMenuLayout.ReportMainItem category,
            @NotNull ReportCheckMenuHolder.RecordScope scope,
            int page,
            int rawSlot
    ) {
        int slotIndex = slotIndex(rawSlot);
        if (slotIndex < 0) {
            return null;
        }

        List<ReportStorage.ReportView> reports = plugin.getReportStorage().getReportsForCategory(category, scope.includesReviewed());
        int reportIndex = page * ReportCheckMenuLayout.REPORT_ENTRY_SLOTS.length + slotIndex;
        if (reportIndex < 0 || reportIndex >= reports.size()) {
            return null;
        }

        return reports.get(reportIndex);
    }

    public boolean openReportDetail(
            @NotNull Player viewer,
            @NotNull ReportMenuLayout.ReportMainItem category,
            @NotNull ReportCheckMenuHolder.RecordScope scope,
            int sourcePage,
            @NotNull ReportStorage.ReportView report
    ) {
        List<ReportStorage.ReportView> reports = plugin.getReportStorage().getReportsForCategory(category, scope.includesReviewed());
        int totalPages = Math.max(1, (int) Math.ceil((double) reports.size() / ReportCheckMenuLayout.REPORT_ENTRY_SLOTS.length));
        ReportCheckMenuHolder holder = new ReportCheckMenuHolder(scope, category, sourcePage, totalPages, report.reportId());
        ReportCheckMenuLayout.populateReportDetailMenu(
                holder.getInventory(),
                report,
                viewer.hasPermission(PermissionNodes.REVIEW_TELEPORT),
                viewer.hasPermission(PermissionNodes.REVIEW_ACTION),
                viewer.hasPermission(PermissionNodes.REVIEW_ACTION),
                plugin.isSpfeastApiAvailable() && plugin.isSpfeastBansAvailable()
        );
        viewer.openInventory(holder.getInventory());
        return true;
    }

    public boolean openPunishmentMenu(
            @NotNull Player viewer,
            @NotNull ReportMenuLayout.ReportMainItem category,
            @NotNull ReportCheckMenuHolder.RecordScope scope,
            int sourcePage,
            int totalPages,
            @NotNull ReportStorage.ReportView report
    ) {
        var settings = plugin.getCategoryConfig().getSettings(category);
        java.util.List<String> durations = settings.punishmentDurations();
        if (durations == null || durations.isEmpty()) {
            durations = java.util.List.of("7d", "30d", "90d", "180d", "360d");
        }

        boolean chatPunishment = category.actionKey() != null
                && category.actionKey().toLowerCase(java.util.Locale.ROOT).startsWith("chat_");
        int windowDays = settings.punishmentWindowDays();
        var details = chatPunishment
                ? plugin.getSpfeastBansHistory().getMuteDetails(java.util.UUID.fromString(report.targetUuid()), windowDays, 5)
                : plugin.getSpfeastBansHistory().getBanDetails(java.util.UUID.fromString(report.targetUuid()), windowDays, 5);

        int boost = 0;
        String boostThresholdText = settings.severityBoostIfMaxDurationAtLeast();
        if (boostThresholdText != null) {
            Long thresholdMillis = DurationText.parseDurationMillis(boostThresholdText);
            if (thresholdMillis != null && thresholdMillis > 0L && details.maxDurationMillis() >= thresholdMillis) {
                boost = 1;
            }
        }

        String recommended = null;
        if (!durations.isEmpty()) {
            int index = Math.min(details.count() + boost, durations.size() - 1);
            recommended = durations.get(index);
        }

        ReportCheckMenuHolder holder = ReportCheckMenuHolder.punishment(scope, category, sourcePage, totalPages, report.reportId());
        ReportCheckMenuLayout.populatePunishmentMenu(holder.getInventory(), report, settings, durations, recommended, details);
        viewer.openInventory(holder.getInventory());
        return true;
    }

    public @Nullable ReportStorage.ReportView getReportById(
            @NotNull ReportMenuLayout.ReportMainItem category,
            @NotNull ReportCheckMenuHolder.RecordScope scope,
            @NotNull String reportId
    ) {
        for (ReportStorage.ReportView report : plugin.getReportStorage().getReportsForCategory(category, scope.includesReviewed())) {
            if (reportId.equalsIgnoreCase(report.reportId())) {
                return report;
            }
        }
        return null;
    }

    public boolean teleportToReportLocation(@NotNull Player viewer, @NotNull ReportStorage.ReportView report) {
        if (!report.hasLocation()) {
            viewer.sendMessage(Component.text("This report does not have a saved location.", NamedTextColor.RED));
            return false;
        }

        World world = Bukkit.getWorld(report.world());
        if (world == null) {
            viewer.sendMessage(Component.text("The saved world for this report is not available.", NamedTextColor.RED));
            return false;
        }

        viewer.teleport(new Location(world, report.x() + 0.5D, report.y(), report.z() + 0.5D));
        viewer.sendMessage(Component.text("Teleported to the saved report location.", NamedTextColor.GREEN));
        return true;
    }

    private int slotIndex(int rawSlot) {
        for (int i = 0; i < ReportCheckMenuLayout.REPORT_ENTRY_SLOTS.length; i++) {
            if (ReportCheckMenuLayout.REPORT_ENTRY_SLOTS[i] == rawSlot) {
                return i;
            }
        }
        return -1;
    }
}
