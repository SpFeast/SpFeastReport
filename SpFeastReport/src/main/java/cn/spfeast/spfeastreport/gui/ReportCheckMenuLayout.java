package cn.spfeast.spfeastreport.gui;

import cn.spfeast.spfeastreport.config.ReportCategoryConfig;
import cn.spfeast.spfeastreport.integration.SpfeastBansHistory;
import cn.spfeast.spfeastreport.storage.ReportStorage;
import cn.spfeast.spfeastreport.util.DurationText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReportCheckMenuLayout {
    public static final int MENU_SIZE = 54;
    public static final int DETAIL_MENU_SIZE = 27;
    public static final int PUNISHMENT_MENU_SIZE = 27;
    public static final int[] REPORT_ENTRY_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    public static final int BACK_SLOT = 45;
    public static final int PREVIOUS_PAGE_SLOT = 48;
    public static final int PAGE_INFO_SLOT = 49;
    public static final int NEXT_PAGE_SLOT = 50;
    public static final int SCOPE_TOGGLE_SLOT = 47;
    public static final int CLOSE_SLOT = 53;
    public static final int EMPTY_STATE_SLOT = 22;
    public static final int DETAIL_TARGET_SLOT = 4;
    public static final int DETAIL_TP_SLOT = 11;
    public static final int DETAIL_BAN_SLOT = 13;
    public static final int DETAIL_NO_ERROR_SLOT = 15;
    public static final int DETAIL_NO_TP_BAN_SLOT = 12;
    public static final int DETAIL_NO_TP_NO_ERROR_SLOT = 14;
    public static final int DETAIL_BACK_SLOT = 18;
    public static final int DETAIL_CLOSE_SLOT = 26;
    public static final int PUNISHMENT_TARGET_SLOT = 4;
    public static final int PUNISHMENT_DURATION_7D_SLOT = 9;
    public static final int PUNISHMENT_DURATION_30D_SLOT = 11;
    public static final int PUNISHMENT_DURATION_90D_SLOT = 13;
    public static final int PUNISHMENT_DURATION_180D_SLOT = 15;
    public static final int PUNISHMENT_DURATION_360D_SLOT = 17;
    public static final int PUNISHMENT_HISTORY_SLOT = 22;
    public static final int PUNISHMENT_BACK_SLOT = 18;
    public static final int PUNISHMENT_CLOSE_SLOT = 26;
    private static final int[] CHAT_PUNISHMENT_SLOTS = {
            PUNISHMENT_DURATION_30D_SLOT,
            PUNISHMENT_DURATION_90D_SLOT,
            PUNISHMENT_DURATION_180D_SLOT
    };

    private static final int[] PUNISHMENT_DURATION_SLOTS = {
            PUNISHMENT_DURATION_7D_SLOT,
            PUNISHMENT_DURATION_30D_SLOT,
            PUNISHMENT_DURATION_90D_SLOT,
            PUNISHMENT_DURATION_180D_SLOT,
            PUNISHMENT_DURATION_360D_SLOT
    };
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private ReportCheckMenuLayout() {
    }

    public static @NotNull Component categoryMenuTitle(@NotNull ReportCheckMenuHolder.RecordScope scope) {
        return text(scope == ReportCheckMenuHolder.RecordScope.ALL ? "All Report Categories" : "Report Categories", NamedTextColor.GRAY);
    }

    public static @NotNull Component reportListTitle(
            @NotNull ReportCheckMenuHolder.RecordScope scope,
            @Nullable ReportMenuLayout.ReportMainItem category,
            int currentPage,
            int totalPages
    ) {
        String categoryTitle = category != null ? compactCategoryTitle(category.title()) : "Reports";
        String prefix = scope == ReportCheckMenuHolder.RecordScope.ALL ? "All " : "";
        return text(prefix + categoryTitle + " [" + (currentPage + 1) + "/" + totalPages + "]", NamedTextColor.GRAY);
    }

    public static @NotNull Component reportDetailTitle(
            @NotNull ReportCheckMenuHolder.RecordScope scope,
            @Nullable ReportMenuLayout.ReportMainItem category
    ) {
        String categoryTitle = category != null ? compactCategoryTitle(category.title()) : "Report";
        String prefix = scope == ReportCheckMenuHolder.RecordScope.ALL ? "All " : "";
        return text("View " + prefix + categoryTitle, NamedTextColor.GRAY);
    }

    public static @NotNull Component punishmentMenuTitle(@Nullable ReportMenuLayout.ReportMainItem category) {
        String categoryTitle = category != null ? compactCategoryTitle(category.title()) : "Report";
        return text("Punish: " + categoryTitle, NamedTextColor.GRAY);
    }

    public static void populateCategoryMenu(
            @NotNull Inventory inventory,
            @NotNull List<ReportStorage.CategoryOverview> overviews,
            @NotNull ReportCheckMenuHolder.RecordScope scope
    ) {
        for (ReportStorage.CategoryOverview overview : overviews) {
            inventory.setItem(overview.item().slot(), createCategoryItem(overview));
        }
        inventory.setItem(SCOPE_TOGGLE_SLOT, createScopeToggleItem(scope));
        inventory.setItem(CLOSE_SLOT, createSimpleItem(Material.BARRIER, "Close", NamedTextColor.RED));
    }

    public static void populateReportListMenu(
            @NotNull Inventory inventory,
            @NotNull ReportCheckMenuHolder.RecordScope scope,
            @NotNull ReportMenuLayout.ReportMainItem category,
            @NotNull List<ReportStorage.ReportView> reports,
            int currentPage,
            int totalPages
    ) {
        int startIndex = currentPage * REPORT_ENTRY_SLOTS.length;
        int endIndex = Math.min(startIndex + REPORT_ENTRY_SLOTS.length, reports.size());

        if (reports.isEmpty()) {
            inventory.setItem(
                    EMPTY_STATE_SLOT,
                    createSimpleItem(
                            Material.PAPER,
                            "No Reports Yet",
                            NamedTextColor.YELLOW,
                            List.of(
                                    line("There are no saved reports in this category.", NamedTextColor.GRAY)
                            )
                    )
            );
        } else {
            int slotIndex = 0;
            for (int i = startIndex; i < endIndex; i++) {
                inventory.setItem(REPORT_ENTRY_SLOTS[slotIndex++], createReportItem(reports.get(i)));
            }
        }

        inventory.setItem(BACK_SLOT, createSimpleItem(Material.ARROW, "Back", NamedTextColor.YELLOW));
        inventory.setItem(SCOPE_TOGGLE_SLOT, createScopeToggleItem(scope));
        inventory.setItem(PREVIOUS_PAGE_SLOT, createSimpleItem(Material.PAPER, "Previous Page", NamedTextColor.YELLOW));
        inventory.setItem(
                PAGE_INFO_SLOT,
                createSimpleItem(
                        Material.BOOK,
                        category.title(),
                        NamedTextColor.AQUA,
                        List.of(
                                line(scope == ReportCheckMenuHolder.RecordScope.ALL ? "Mode: All Records" : "Mode: Pending Records", NamedTextColor.GRAY),
                                line("Page: " + (currentPage + 1) + "/" + totalPages, NamedTextColor.GRAY),
                                line("Reports: " + reports.size(), NamedTextColor.GRAY)
                        )
                )
        );
        inventory.setItem(NEXT_PAGE_SLOT, createSimpleItem(Material.PAPER, "Next Page", NamedTextColor.YELLOW));
        inventory.setItem(CLOSE_SLOT, createSimpleItem(Material.BARRIER, "Close", NamedTextColor.RED));
    }

    public static void populateReportDetailMenu(
            @NotNull Inventory inventory,
            @NotNull ReportStorage.ReportView report,
            boolean canTeleport,
            boolean canBan,
            boolean canMarkNoError,
            boolean banIntegrationAvailable
    ) {
        inventory.setItem(DETAIL_TARGET_SLOT, createDetailTargetItem(report));

        if (report.isPending()) {
            if (report.hasLocation()) {
                inventory.setItem(DETAIL_TP_SLOT, createTeleportItem(report, canTeleport));
                inventory.setItem(DETAIL_BAN_SLOT, createBanItem(report, canBan, banIntegrationAvailable));
                inventory.setItem(DETAIL_NO_ERROR_SLOT, createNoErrorItem(report, canMarkNoError));
            } else {
                inventory.setItem(DETAIL_NO_TP_BAN_SLOT, createBanItem(report, canBan, banIntegrationAvailable));
                inventory.setItem(DETAIL_NO_TP_NO_ERROR_SLOT, createNoErrorItem(report, canMarkNoError));
            }
        } else {
            if (report.hasLocation()) {
                inventory.setItem(DETAIL_TP_SLOT, createTeleportItem(report, canTeleport));
            }
            inventory.setItem(DETAIL_BAN_SLOT, createReviewedStatusItem(report));
        }

        inventory.setItem(DETAIL_BACK_SLOT, createSimpleItem(Material.ARROW, "Back", NamedTextColor.YELLOW));
        inventory.setItem(DETAIL_CLOSE_SLOT, createSimpleItem(Material.BARRIER, "Close", NamedTextColor.RED));
    }

    public static void populatePunishmentMenu(
            @NotNull Inventory inventory,
            @NotNull ReportStorage.ReportView report,
            @NotNull ReportCategoryConfig.CategorySettings settings,
            @NotNull List<String> durations,
            @Nullable String recommendedDuration,
            @NotNull SpfeastBansHistory.HistoryDetails historyDetails
    ) {
        boolean chatPunishment = report.categoryKey() != null
                && report.categoryKey().toLowerCase(java.util.Locale.ROOT).startsWith("chat_");

        inventory.setItem(PUNISHMENT_TARGET_SLOT, createPunishmentTargetItem(report, chatPunishment));

        if (chatPunishment) {
            inventory.setItem(CHAT_PUNISHMENT_SLOTS[0], createChatMuteOptionItem("Major Chat", "majorchat", "7d"));
            inventory.setItem(CHAT_PUNISHMENT_SLOTS[1], createChatMuteOptionItem("Minor Chat", "minorchat", "1d"));
            inventory.setItem(CHAT_PUNISHMENT_SLOTS[2], createChatMuteOptionItem("Under Review", "underreview", "1d"));
        } else {
            int limit = Math.min(PUNISHMENT_DURATION_SLOTS.length, durations.size());
            for (int i = 0; i < limit; i++) {
                String durationText = durations.get(i);
                inventory.setItem(
                        PUNISHMENT_DURATION_SLOTS[i],
                        createDurationItem(durationText, durationText.equalsIgnoreCase(recommendedDuration))
                );
            }
        }

        inventory.setItem(PUNISHMENT_HISTORY_SLOT, createHistoryItem(settings, chatPunishment, historyDetails));
        inventory.setItem(PUNISHMENT_BACK_SLOT, createSimpleItem(Material.ARROW, "Back", NamedTextColor.YELLOW));
        inventory.setItem(PUNISHMENT_CLOSE_SLOT, createSimpleItem(Material.BARRIER, "Close", NamedTextColor.RED));
    }

    public static int punishmentDurationIndex(int rawSlot) {
        for (int i = 0; i < PUNISHMENT_DURATION_SLOTS.length; i++) {
            if (PUNISHMENT_DURATION_SLOTS[i] == rawSlot) {
                return i;
            }
        }
        return -1;
    }

    private static @NotNull ItemStack createCategoryItem(@NotNull ReportStorage.CategoryOverview overview) {
        ItemStack item = new ItemStack(overview.item().material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(text(overview.settings().title(), NamedTextColor.GREEN));
            meta.lore(List.of(
                    line("Reports: " + overview.totalReports(), NamedTextColor.WHITE),
                    line("Enabled: " + (overview.settings().enabled() ? "Yes" : "No"), overview.settings().enabled() ? NamedTextColor.GREEN : NamedTextColor.RED),
                    Component.empty(),
                    line("Click to view.", NamedTextColor.YELLOW)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @NotNull ItemStack createScopeToggleItem(@NotNull ReportCheckMenuHolder.RecordScope scope) {
        if (scope == ReportCheckMenuHolder.RecordScope.ALL) {
            return createSimpleItem(
                    Material.LIME_DYE,
                    "View Pending Records",
                    NamedTextColor.GREEN,
                    List.of(
                            line("Switch back to only unresolved reports.", NamedTextColor.GRAY)
                    )
            );
        }

        return createSimpleItem(
                Material.CLOCK,
                "View All Records",
                NamedTextColor.YELLOW,
                List.of(
                        line("Include processed reports and their result status.", NamedTextColor.GRAY)
                )
        );
    }

    private static @NotNull ItemStack createReportItem(@NotNull ReportStorage.ReportView report) {
        ItemStack item = createTargetHead(report);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(text(displayTargetName(report), recordNameColor(report)));
            List<Component> lore = new java.util.ArrayList<>();
            lore.add(line("Report ID: " + safe(report.reportId()), NamedTextColor.YELLOW));
            lore.add(line("Status: " + prettyStatus(report.reviewStatus()), reviewStatusColor(report)));
            lore.add(line("Time: " + safe(report.createdAtDisplay()), NamedTextColor.WHITE));
            lore.add(Component.empty());
            lore.add(line("Reporter: " + safe(report.reporterName()), NamedTextColor.GREEN));
            lore.add(line("Reporter UUID: " + safe(report.reporterUuid()), NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(line("Target: " + displayTargetName(report), NamedTextColor.RED));
            lore.add(line("Target UUID: " + safe(report.targetUuid()), NamedTextColor.DARK_GRAY));
            if (report.hasLocation()) {
                lore.add(Component.empty());
                lore.add(line("World: " + safe(report.world()), NamedTextColor.GRAY));
                lore.add(line(
                        "Location: " + report.x() + ", " + report.y() + ", " + report.z(),
                        NamedTextColor.GRAY
                ));
            }
            appendReviewLore(lore, report, false);
            lore.add(Component.empty());
            lore.add(line("Click to open details.", NamedTextColor.YELLOW));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @NotNull ItemStack createDetailTargetItem(@NotNull ReportStorage.ReportView report) {
        ItemStack item = createTargetHead(report);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(text(displayTargetName(report), recordNameColor(report)));
            java.util.ArrayList<Component> lore = new java.util.ArrayList<>();
            lore.add(line("Report ID: " + safe(report.reportId()), NamedTextColor.YELLOW));
            lore.add(line("Category: " + safe(report.categoryTitle()), NamedTextColor.GREEN));
            lore.add(line("Status: " + prettyStatus(report.reviewStatus()), reviewStatusColor(report)));
            lore.add(line("Time: " + safe(report.createdAtDisplay()), NamedTextColor.WHITE));
            lore.add(Component.empty());
            lore.add(line("Reporter: " + safe(report.reporterName()), NamedTextColor.GREEN));
            lore.add(line("Reporter UUID: " + safe(report.reporterUuid()), NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(line("Target UUID: " + safe(report.targetUuid()), NamedTextColor.DARK_GRAY));
            if (report.hasLocation()) {
                lore.add(Component.empty());
                lore.add(line("World: " + safe(report.world()), NamedTextColor.GRAY));
                lore.add(line("Location: " + report.x() + ", " + report.y() + ", " + report.z(), NamedTextColor.GRAY));
            }
            appendReviewLore(lore, report, true);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @NotNull ItemStack createTeleportItem(@NotNull ReportStorage.ReportView report, boolean canTeleport) {
        if (!canTeleport) {
            return createSimpleItem(
                    Material.GRAY_DYE,
                    "Teleport Locked",
                    NamedTextColor.DARK_GRAY,
                    List.of(
                            line("You do not have permission to teleport", NamedTextColor.GRAY),
                            line("to saved report locations.", NamedTextColor.GRAY)
                    )
            );
        }

        return createSimpleItem(
                Material.ENDER_PEARL,
                "Teleport To Location",
                NamedTextColor.AQUA,
                List.of(
                        line("World: " + safe(report.world()), NamedTextColor.GRAY),
                        line("Location: " + report.x() + ", " + report.y() + ", " + report.z(), NamedTextColor.GRAY),
                        Component.empty(),
                        line("Click to teleport.", NamedTextColor.YELLOW)
                )
        );
    }

    private static @NotNull ItemStack createBanItem(@NotNull ReportStorage.ReportView report, boolean canBan, boolean banIntegrationAvailable) {
        boolean chatPunishment = report.categoryKey() != null
                && report.categoryKey().toLowerCase(java.util.Locale.ROOT).startsWith("chat_");
        java.util.ArrayList<Component> lore = new java.util.ArrayList<>();
        lore.add(line("Category: " + safe(report.categoryTitle()), NamedTextColor.WHITE));
        lore.add(Component.empty());
        if (!canBan) {
            lore.add(line("You do not have permission to use", NamedTextColor.GRAY));
            lore.add(line("the punishment action.", NamedTextColor.GRAY));
            return createSimpleItem(Material.GRAY_TERRACOTTA, "Action Locked", NamedTextColor.DARK_GRAY, lore);
        }

        if (!banIntegrationAvailable) {
            lore.add(line("This feature requires SpFeastBans", NamedTextColor.GRAY));
            lore.add(line("and spfeastApi to be installed.", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(line("Click for instructions.", NamedTextColor.YELLOW));
            return createSimpleItem(Material.GRAY_TERRACOTTA, "Action Unavailable", NamedTextColor.DARK_GRAY, lore);
        }

        lore.add(line(chatPunishment ? "Click to mute via spfeastApi." : "Click to ban via spfeastApi.", NamedTextColor.YELLOW));
        return createSimpleItem(
                Material.RED_TERRACOTTA,
                chatPunishment ? "Mute Player" : "Ban Player",
                NamedTextColor.RED,
                lore
        );
    }

    private static @NotNull ItemStack createPunishmentTargetItem(@NotNull ReportStorage.ReportView report, boolean chatPunishment) {
        ItemStack item = createTargetHead(report);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(text(displayTargetName(report), NamedTextColor.AQUA));
            meta.lore(List.of(
                    line("Category: " + safe(report.categoryTitle()), NamedTextColor.GRAY),
                    line("Action: " + (chatPunishment ? "Mute" : "Ban"), NamedTextColor.GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @NotNull ItemStack createDurationItem(@NotNull String durationText, boolean recommended) {
        java.util.ArrayList<Component> lore = new java.util.ArrayList<>();
        if (recommended) {
            lore.add(line("Recommended based on history.", NamedTextColor.GREEN));
            lore.add(Component.empty());
        }
        lore.add(line("Click to apply.", NamedTextColor.YELLOW));
        return createSimpleItem(
                Material.CLOCK,
                durationText,
                recommended ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                lore
        );
    }

    private static @NotNull ItemStack createChatMuteOptionItem(
            @NotNull String title,
            @NotNull String reasonKey,
            @NotNull String durationText
    ) {
        return createSimpleItem(
                Material.CLOCK,
                title,
                NamedTextColor.YELLOW,
                List.of(
                        line("Reason Key: " + reasonKey, NamedTextColor.GRAY),
                        line("Duration: " + durationText, NamedTextColor.GRAY),
                        Component.empty(),
                        line("Click to mute.", NamedTextColor.YELLOW)
                )
        );
    }

    private static @NotNull ItemStack createHistoryItem(
            @NotNull ReportCategoryConfig.CategorySettings settings,
            boolean chatPunishment,
            @NotNull SpfeastBansHistory.HistoryDetails details
    ) {
        java.util.ArrayList<Component> lore = new java.util.ArrayList<>();
        lore.add(line("Mode: " + (chatPunishment ? "MUTE" : "BAN"), NamedTextColor.GRAY));
        lore.add(line("Window: " + settings.punishmentWindowDays() + "d", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(line("Count: " + details.count(), NamedTextColor.WHITE));
        lore.add(line("Max Duration: " + DurationText.formatDuration(details.maxDurationMillis()), NamedTextColor.WHITE));
        if (!details.recentEntries().isEmpty()) {
            lore.add(Component.empty());
            lore.add(line("Recent:", NamedTextColor.GRAY));
            for (SpfeastBansHistory.HistoryEntry entry : details.recentEntries()) {
                String when = DISPLAY_TIME_FORMAT.format(Instant.ofEpochMilli(entry.createdAtMillis()));
                String duration = DurationText.formatDuration(entry.durationMillis());
                lore.add(line(when + " | " + duration + " | " + entry.key(), NamedTextColor.DARK_GRAY));
            }
        }

        return createSimpleItem(Material.BOOK, "History", NamedTextColor.AQUA, lore);
    }

    private static @NotNull ItemStack createNoErrorItem(@NotNull ReportStorage.ReportView report, boolean canMarkNoError) {
        java.util.ArrayList<Component> lore = new java.util.ArrayList<>();
        if (!canMarkNoError) {
            lore.add(line("You do not have permission to mark", NamedTextColor.GRAY));
            lore.add(line("reports as no error.", NamedTextColor.GRAY));
            return createSimpleItem(Material.GRAY_TERRACOTTA, "No Error Locked", NamedTextColor.DARK_GRAY, lore);
        }

        lore.add(line("Mark this report as reviewed with", NamedTextColor.GRAY));
        lore.add(line("no action required.", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(line("Click to mark.", NamedTextColor.YELLOW));
        return createSimpleItem(Material.LIME_TERRACOTTA, "No Error", NamedTextColor.GREEN, lore);
    }

    private static @NotNull ItemStack createReviewedStatusItem(@NotNull ReportStorage.ReportView report) {
        return createSimpleItem(
                Material.PAPER,
                "Review Completed",
                reviewStatusColor(report),
                List.of(
                        line("Status: " + prettyStatus(report.reviewStatus()), reviewStatusColor(report)),
                        line("Reviewed By: " + safe(report.reviewActorName()), NamedTextColor.WHITE),
                        line("Reviewed At: " + safe(report.reviewedAtDisplay()), NamedTextColor.WHITE)
                )
        );
    }

    private static @NotNull ItemStack createTargetHead(@NotNull ReportStorage.ReportView report) {
        if (report.targetUuid() == null || report.targetUuid().isBlank()) {
            return new ItemStack(Material.PAPER);
        }

        try {
            OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(report.targetUuid()));
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                item.setItemMeta(meta);
            }
            return item;
        } catch (IllegalArgumentException ignored) {
            return new ItemStack(Material.PAPER);
        }
    }

    private static @NotNull ItemStack createSimpleItem(
            @NotNull Material material,
            @NotNull String title,
            @NotNull NamedTextColor color
    ) {
        return createSimpleItem(material, title, color, List.of());
    }

    private static @NotNull ItemStack createSimpleItem(
            @NotNull Material material,
            @NotNull String title,
            @NotNull NamedTextColor color,
            @NotNull List<Component> lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(text(title, color));
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @NotNull Component line(@NotNull String text, @NotNull NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static @NotNull Component text(@NotNull String text, @NotNull NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static @NotNull String safe(@Nullable String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    private static @NotNull String compactCategoryTitle(@NotNull String title) {
        return title.replace("Cheating (Hacking)", "Cheating/Hacking");
    }

    private static @NotNull String displayTargetName(@NotNull ReportStorage.ReportView report) {
        return safe(report.targetName());
    }

    private static @NotNull String prettyStatus(@Nullable String status) {
        if (status == null || status.isBlank()) {
            return "Pending";
        }
        if ("pending".equalsIgnoreCase(status)) {
            return "Pending";
        }
        if ("no_error".equalsIgnoreCase(status)) {
            return "No Error";
        }
        if ("banned".equalsIgnoreCase(status)) {
            return "Banned";
        }
        if ("muted".equalsIgnoreCase(status)) {
            return "Muted";
        }
        return status;
    }

    private static @NotNull NamedTextColor reviewStatusColor(@NotNull ReportStorage.ReportView report) {
        if (report.isBanned()) {
            return NamedTextColor.RED;
        }
        if (report.isMuted()) {
            return NamedTextColor.GOLD;
        }
        if (report.isMarkedNoError()) {
            return NamedTextColor.GREEN;
        }
        return NamedTextColor.YELLOW;
    }

    private static @NotNull NamedTextColor recordNameColor(@NotNull ReportStorage.ReportView report) {
        if (report.isBanned()) {
            return NamedTextColor.RED;
        }
        if (report.isMuted()) {
            return NamedTextColor.GOLD;
        }
        if (report.isMarkedNoError()) {
            return NamedTextColor.GREEN;
        }
        return NamedTextColor.AQUA;
    }

    private static void appendReviewLore(
            @NotNull java.util.List<Component> lore,
            @NotNull ReportStorage.ReportView report,
            boolean includeDetails
    ) {
        if (report.isPending()) {
            return;
        }

        lore.add(Component.empty());
        if (includeDetails) {
            lore.add(line("Reviewed By: " + safe(report.reviewActorName()), NamedTextColor.WHITE));
            lore.add(line("Reviewed At: " + safe(report.reviewedAtDisplay()), NamedTextColor.WHITE));
        }
    }
}
