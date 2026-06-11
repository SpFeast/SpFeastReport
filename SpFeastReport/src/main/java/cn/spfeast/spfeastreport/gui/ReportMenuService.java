package cn.spfeast.spfeastreport.gui;

import cn.spfeast.spfeastreport.SpFeastReportPlugin;
import cn.spfeast.spfeastreport.config.ReportCategoryConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReportMenuService {
    private final SpFeastReportPlugin plugin;

    public ReportMenuService(@NotNull SpFeastReportPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(@NotNull Player viewer, @NotNull OfflinePlayer target) {
        viewer.openInventory(new ReportMenuHolder(target).getInventory());
    }

    public boolean openConfirmMenu(@NotNull Player viewer, @NotNull OfflinePlayer target, @NotNull ReportMenuLayout.ReportMainItem reason) {
        ReportCategoryConfig.CategorySettings settings = plugin.getCategoryConfig().getSettings(reason);
        if (!settings.enabled()) {
            viewer.sendMessage(Component.text("This report option is temporarily unavailable.", NamedTextColor.RED));
            return false;
        }

        viewer.openInventory(new ReportMenuHolder(target, reason).getInventory());
        return true;
    }

    public boolean openConfirmMenuByActionKey(
            @NotNull Player viewer,
            @NotNull OfflinePlayer target,
            @NotNull String actionKey
    ) {
        ReportMenuLayout.ReportMainItem reason = ReportMenuLayout.getReasonByActionKey(actionKey);
        if (reason == null) {
            viewer.sendMessage(Component.text("Unknown report category.", NamedTextColor.RED));
            return false;
        }

        return openConfirmMenu(viewer, target, reason);
    }

    public @Nullable ReportMenuLayout.ReportMainItem getReasonByActionKey(@NotNull String actionKey) {
        return ReportMenuLayout.getReasonByActionKey(actionKey);
    }
}
