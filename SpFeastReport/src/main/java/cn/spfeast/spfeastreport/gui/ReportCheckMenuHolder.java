package cn.spfeast.spfeastreport.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReportCheckMenuHolder implements InventoryHolder {
    private final MenuPage page;
    private final RecordScope scope;
    private final ReportMenuLayout.ReportMainItem category;
    private final int currentPage;
    private final int totalPages;
    private final String reportId;
    private final Inventory inventory;

    public ReportCheckMenuHolder() {
        this(RecordScope.PENDING_ONLY);
    }

    public ReportCheckMenuHolder(@NotNull RecordScope scope) {
        this.page = MenuPage.CATEGORY;
        this.scope = scope;
        this.category = null;
        this.currentPage = 0;
        this.totalPages = 1;
        this.reportId = null;
        this.inventory = Bukkit.createInventory(this, ReportCheckMenuLayout.MENU_SIZE, page.title(scope, null, 0, 1));
    }

    public ReportCheckMenuHolder(
            @NotNull RecordScope scope,
            @NotNull ReportMenuLayout.ReportMainItem category,
            int currentPage,
            int totalPages
    ) {
        this.page = MenuPage.REPORT_LIST;
        this.scope = scope;
        this.category = category;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.reportId = null;
        this.inventory = Bukkit.createInventory(this, ReportCheckMenuLayout.MENU_SIZE, page.title(scope, category, currentPage, totalPages));
    }

    public ReportCheckMenuHolder(
            @NotNull RecordScope scope,
            @NotNull ReportMenuLayout.ReportMainItem category,
            int currentPage,
            int totalPages,
            @NotNull String reportId
    ) {
        this.page = MenuPage.REPORT_DETAIL;
        this.scope = scope;
        this.category = category;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.reportId = reportId;
        this.inventory = Bukkit.createInventory(this, ReportCheckMenuLayout.DETAIL_MENU_SIZE, page.title(scope, category, currentPage, totalPages));
    }

    public @NotNull MenuPage getPage() {
        return page;
    }

    public @Nullable ReportMenuLayout.ReportMainItem getCategory() {
        return category;
    }

    public @NotNull RecordScope getScope() {
        return scope;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public @Nullable String getReportId() {
        return reportId;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public enum MenuPage {
        CATEGORY {
            @Override
            @NotNull Component title(
                    @NotNull RecordScope scope,
                    @Nullable ReportMenuLayout.ReportMainItem category,
                    int currentPage,
                    int totalPages
            ) {
                return ReportCheckMenuLayout.categoryMenuTitle(scope);
            }
        },
        REPORT_LIST {
            @Override
            @NotNull Component title(
                    @NotNull RecordScope scope,
                    @Nullable ReportMenuLayout.ReportMainItem category,
                    int currentPage,
                    int totalPages
            ) {
                return ReportCheckMenuLayout.reportListTitle(scope, category, currentPage, totalPages);
            }
        },
        REPORT_DETAIL {
            @Override
            @NotNull Component title(
                    @NotNull RecordScope scope,
                    @Nullable ReportMenuLayout.ReportMainItem category,
                    int currentPage,
                    int totalPages
            ) {
                return ReportCheckMenuLayout.reportDetailTitle(scope, category);
            }
        };

        abstract @NotNull Component title(
                @NotNull RecordScope scope,
                @Nullable ReportMenuLayout.ReportMainItem category,
                int currentPage,
                int totalPages
        );
    }

    public enum RecordScope {
        PENDING_ONLY,
        ALL;

        public boolean includesReviewed() {
            return this == ALL;
        }
    }
}
