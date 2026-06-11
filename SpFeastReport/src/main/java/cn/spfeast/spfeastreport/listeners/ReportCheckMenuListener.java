package cn.spfeast.spfeastreport.listeners;

import cn.spfeast.spfeastreport.SpFeastReportPlugin;
import cn.spfeast.spfeastreport.gui.ReportCheckMenuHolder;
import cn.spfeast.spfeastreport.gui.ReportCheckMenuLayout;
import cn.spfeast.spfeastreport.gui.ReportMenuLayout;
import cn.spfeast.spfeastreport.permission.PermissionNodes;
import cn.spfeast.spfeastreport.storage.ReportStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.io.IOException;

public final class ReportCheckMenuListener implements Listener {
    private final SpFeastReportPlugin plugin;

    public ReportCheckMenuListener(SpFeastReportPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ReportCheckMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) {
            return;
        }

        if (rawSlot == ReportCheckMenuLayout.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        if (holder.getPage() == ReportCheckMenuHolder.MenuPage.CATEGORY) {
            handleCategoryClick(player, holder, rawSlot);
            return;
        }

        if (holder.getPage() == ReportCheckMenuHolder.MenuPage.REPORT_DETAIL) {
            handleDetailClick(player, holder, rawSlot);
            return;
        }

        handleReportListClick(player, holder, rawSlot);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ReportCheckMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleCategoryClick(Player player, ReportCheckMenuHolder holder, int rawSlot) {
        if (rawSlot == ReportCheckMenuLayout.SCOPE_TOGGLE_SLOT) {
            ReportCheckMenuHolder.RecordScope nextScope = holder.getScope() == ReportCheckMenuHolder.RecordScope.ALL
                    ? ReportCheckMenuHolder.RecordScope.PENDING_ONLY
                    : ReportCheckMenuHolder.RecordScope.ALL;
            plugin.getReportCheckMenuService().openCategoryMenu(player, nextScope);
            return;
        }

        ReportMenuLayout.ReportMainItem clickedItem = ReportMenuLayout.getItemBySlot(rawSlot);
        if (clickedItem == null || clickedItem.type() != ReportMenuLayout.ReportMainItemType.REASON) {
            return;
        }

        plugin.getReportCheckMenuService().openCategoryReports(player, clickedItem, 0, holder.getScope());
    }

    private void handleReportListClick(Player player, ReportCheckMenuHolder holder, int rawSlot) {
        if (rawSlot == ReportCheckMenuLayout.BACK_SLOT) {
            plugin.getReportCheckMenuService().openCategoryMenu(player, holder.getScope());
            return;
        }

        if (rawSlot == ReportCheckMenuLayout.SCOPE_TOGGLE_SLOT) {
            if (holder.getCategory() != null) {
                ReportCheckMenuHolder.RecordScope nextScope = holder.getScope() == ReportCheckMenuHolder.RecordScope.ALL
                        ? ReportCheckMenuHolder.RecordScope.PENDING_ONLY
                        : ReportCheckMenuHolder.RecordScope.ALL;
                plugin.getReportCheckMenuService().openCategoryReports(player, holder.getCategory(), 0, nextScope);
            }
            return;
        }

        if (rawSlot == ReportCheckMenuLayout.PREVIOUS_PAGE_SLOT) {
            if (holder.getCategory() != null) {
                plugin.getReportCheckMenuService().openCategoryReports(player, holder.getCategory(), holder.getCurrentPage() - 1, holder.getScope());
            }
            return;
        }

        if (rawSlot == ReportCheckMenuLayout.NEXT_PAGE_SLOT) {
            if (holder.getCategory() != null) {
                plugin.getReportCheckMenuService().openCategoryReports(player, holder.getCategory(), holder.getCurrentPage() + 1, holder.getScope());
            }
            return;
        }

        if (holder.getCategory() == null) {
            return;
        }

        ReportStorage.ReportView report = plugin.getReportCheckMenuService().getReportForSlot(
                holder.getCategory(),
                holder.getScope(),
                holder.getCurrentPage(),
                rawSlot
        );
        if (report != null) {
            plugin.getReportCheckMenuService().openReportDetail(player, holder.getCategory(), holder.getScope(), holder.getCurrentPage(), report);
        }
    }

    private void handleDetailClick(Player player, ReportCheckMenuHolder holder, int rawSlot) {
        if (holder.getCategory() == null || holder.getReportId() == null) {
            player.closeInventory();
            return;
        }

        if (rawSlot == ReportCheckMenuLayout.DETAIL_BACK_SLOT) {
            plugin.getReportCheckMenuService().openCategoryReports(player, holder.getCategory(), holder.getCurrentPage(), holder.getScope());
            return;
        }

        if (rawSlot == ReportCheckMenuLayout.DETAIL_CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        ReportStorage.ReportView report = plugin.getReportCheckMenuService().getReportById(holder.getCategory(), holder.getScope(), holder.getReportId());
        if (report == null) {
            player.sendMessage(Component.text("This report entry could not be found.", NamedTextColor.RED));
            plugin.getReportCheckMenuService().openCategoryReports(player, holder.getCategory(), holder.getCurrentPage(), holder.getScope());
            return;
        }

        if (rawSlot == ReportCheckMenuLayout.DETAIL_TP_SLOT) {
            if (!player.hasPermission(PermissionNodes.REVIEW_TELEPORT)) {
                player.sendMessage(Component.text("You do not have permission to teleport to report locations.", NamedTextColor.RED));
                return;
            }
            plugin.getReportCheckMenuService().teleportToReportLocation(player, report);
            return;
        }

        if (rawSlot == ReportCheckMenuLayout.DETAIL_BAN_SLOT) {
            if (!player.hasPermission(PermissionNodes.REVIEW_ACTION)) {
                player.sendMessage(Component.text("You do not have permission to use the ban action.", NamedTextColor.RED));
                return;
            }
            if (!report.isPending()) {
                player.sendMessage(Component.text("This report has already been processed.", NamedTextColor.YELLOW));
                return;
            }
            player.sendMessage(Component.text("Ban command integration is reserved for the next step.", NamedTextColor.YELLOW));
            return;
        }

        if (rawSlot == ReportCheckMenuLayout.DETAIL_NO_ERROR_SLOT) {
            if (!player.hasPermission(PermissionNodes.REVIEW_ACTION)) {
                player.sendMessage(Component.text("You do not have permission to mark reports as no error.", NamedTextColor.RED));
                return;
            }
            if (!report.isPending()) {
                player.sendMessage(Component.text("This report has already been processed.", NamedTextColor.YELLOW));
                return;
            }
            handleMarkNoError(player, holder, report);
        }
    }

    private void handleMarkNoError(Player player, ReportCheckMenuHolder holder, ReportStorage.ReportView report) {
        try {
            ReportStorage.ReportUpdateResult result = plugin.getReportStorage().markReportNoError(
                    holder.getCategory(),
                    report.reportId(),
                    player
            );
            if (result == ReportStorage.ReportUpdateResult.ALREADY_MARKED) {
                player.sendMessage(Component.text("This report is already marked as no error.", NamedTextColor.YELLOW));
            } else if (result == ReportStorage.ReportUpdateResult.NOT_FOUND) {
                player.sendMessage(Component.text("This report entry could not be found.", NamedTextColor.RED));
                plugin.getReportCheckMenuService().openCategoryReports(player, holder.getCategory(), holder.getCurrentPage(), holder.getScope());
                return;
            } else {
                player.sendMessage(Component.text("The report has been marked as no error.", NamedTextColor.GREEN));
            }

            ReportStorage.ReportView refreshedReport = plugin.getReportCheckMenuService().getReportById(
                    holder.getCategory(),
                    holder.getScope(),
                    report.reportId()
            );
            if (refreshedReport != null) {
                plugin.getReportCheckMenuService().openReportDetail(
                        player,
                        holder.getCategory(),
                        holder.getScope(),
                        holder.getCurrentPage(),
                        refreshedReport
                );
            } else {
                plugin.getReportCheckMenuService().openCategoryReports(player, holder.getCategory(), holder.getCurrentPage(), holder.getScope());
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to mark report as no error: " + exception.getMessage());
            player.sendMessage(Component.text("Failed to update this report.", NamedTextColor.RED));
        }
    }
}
