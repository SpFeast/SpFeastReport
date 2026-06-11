package cn.spfeast.spfeastreport.gui;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReportMenuHolder implements InventoryHolder {
    private final MenuPage page;
    private final OfflinePlayer target;
    private final ReportMenuLayout.ReportMainItem selectedReason;
    private final Inventory inventory;

    public ReportMenuHolder(OfflinePlayer target) {
        this(MenuPage.MAIN, target, null);
    }

    public ReportMenuHolder(@NotNull OfflinePlayer target, @NotNull ReportMenuLayout.ReportMainItem selectedReason) {
        this(MenuPage.CONFIRM, target, selectedReason);
    }

    private ReportMenuHolder(
            @NotNull MenuPage page,
            @NotNull OfflinePlayer target,
            @Nullable ReportMenuLayout.ReportMainItem selectedReason
    ) {
        this.page = page;
        this.target = target;
        this.selectedReason = selectedReason;
        this.inventory = Bukkit.createInventory(this, page.size(), page.title(selectedReason));

        if (page == MenuPage.MAIN) {
            ReportMenuLayout.populateMainMenu(this.inventory, target);
        } else {
            ReportMenuLayout.populateConfirmMenu(this.inventory, target, selectedReason);
        }
    }

    public OfflinePlayer getTarget() {
        return target;
    }

    public MenuPage getPage() {
        return page;
    }

    public @Nullable ReportMenuLayout.ReportMainItem getSelectedReason() {
        return selectedReason;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public enum MenuPage {
        MAIN {
            @Override
            int size() {
                return ReportMenuLayout.MENU_SIZE;
            }

            @Override
            @NotNull net.kyori.adventure.text.Component title(@Nullable ReportMenuLayout.ReportMainItem selectedReason) {
                return ReportMenuLayout.MENU_TITLE;
            }
        },
        CONFIRM {
            @Override
            int size() {
                return ReportMenuLayout.CONFIRM_MENU_SIZE;
            }

            @Override
            @NotNull net.kyori.adventure.text.Component title(@Nullable ReportMenuLayout.ReportMainItem selectedReason) {
                return ReportMenuLayout.confirmMenuTitle(selectedReason);
            }
        };

        abstract int size();

        abstract @NotNull net.kyori.adventure.text.Component title(@Nullable ReportMenuLayout.ReportMainItem selectedReason);
    }
}
