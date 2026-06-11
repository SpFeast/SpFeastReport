package cn.spfeast.spfeastreport.listeners;

import cn.spfeast.spfeastreport.SpFeastReportPlugin;
import cn.spfeast.spfeastreport.config.ReportCategoryConfig;
import cn.spfeast.spfeastreport.gui.ReportMenuHolder;
import cn.spfeast.spfeastreport.gui.ReportMenuLayout;
import cn.spfeast.spfeastreport.storage.ReportStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.io.IOException;

public final class ReportMenuListener implements Listener {
    private final SpFeastReportPlugin plugin;

    public ReportMenuListener(SpFeastReportPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ReportMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot == ReportMenuLayout.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        if (holder.getPage() == ReportMenuHolder.MenuPage.CONFIRM) {
            handleConfirmMenuClick(player, holder, rawSlot);
            return;
        }

        ReportMenuLayout.ReportMainItem clickedItem = ReportMenuLayout.getItemBySlot(rawSlot);
        if (clickedItem == null || clickedItem.type() != ReportMenuLayout.ReportMainItemType.REASON) {
            return;
        }

        openConfirmMenu(player, holder, clickedItem);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ReportMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void openConfirmMenu(
            Player player,
            ReportMenuHolder holder,
            ReportMenuLayout.ReportMainItem clickedItem
    ) {
        plugin.getReportMenuService().openConfirmMenu(player, holder.getTarget(), clickedItem);
    }

    private void handleConfirmMenuClick(Player player, ReportMenuHolder holder, int rawSlot) {
        if (rawSlot == ReportMenuLayout.CONFIRM_CANCEL_SLOT) {
            plugin.getReportMenuService().openMainMenu(player, holder.getTarget());
            return;
        }

        if (rawSlot != ReportMenuLayout.CONFIRM_SUBMIT_SLOT) {
            return;
        }

        ReportMenuLayout.ReportMainItem selectedReason = holder.getSelectedReason();
        if (selectedReason == null) {
            player.closeInventory();
            return;
        }

        try {
            plugin.getReportStorage().saveReport(player, holder.getTarget(), selectedReason);
            player.closeInventory();
            player.sendMessage(Component.text("Submitting Report...", NamedTextColor.GRAY));
            player.sendMessage(
                    Component.text("Thanks for your ", NamedTextColor.GREEN)
                            .append(Component.text(selectedReason.title(), NamedTextColor.GREEN))
                            .append(Component.text(" report. We understand your", NamedTextColor.GREEN))
            );
            player.sendMessage(
                    Component.text("concerns and will review it as soon as possible.", NamedTextColor.GREEN)
            );
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save report: " + exception.getMessage());
            player.sendMessage(Component.text("Failed to record your report.", NamedTextColor.RED));
        }
    }
}
