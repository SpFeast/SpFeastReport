package cn.spfeast.spfeastreport.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ReportMenuLayout {
    public static final int MENU_SIZE = 54;
    public static final Component MENU_TITLE = Component.text("Report");
    public static final int CONFIRM_MENU_SIZE = 27;

    public static final int TARGET_HEAD_SLOT = 4;
    public static final int INFO_SLOT = 48;
    public static final int CLOSE_SLOT = 49;
    public static final int CONFIRM_SUBMIT_SLOT = 11;
    public static final int CONFIRM_TARGET_SLOT = 13;
    public static final int CONFIRM_CANCEL_SLOT = 15;

    private ReportMenuLayout() {
    }

    public static void populateMainMenu(@NotNull Inventory inventory, @NotNull OfflinePlayer target) {
        inventory.setItem(TARGET_HEAD_SLOT, createTargetHead(target));

        for (ReportMainItem item : ReportMainItem.values()) {
            inventory.setItem(item.slot(), createMenuItem(item));
        }
    }

    public static void populateConfirmMenu(
            @NotNull Inventory inventory,
            @NotNull OfflinePlayer target,
            @Nullable ReportMainItem selectedReason
    ) {
        if (selectedReason == null) {
            return;
        }

        inventory.setItem(CONFIRM_SUBMIT_SLOT, createConfirmActionItem(Material.GREEN_TERRACOTTA, "Submit Report", NamedTextColor.GREEN));
        inventory.setItem(CONFIRM_TARGET_SLOT, createConfirmTargetHead(target, selectedReason));
        inventory.setItem(CONFIRM_CANCEL_SLOT, createConfirmActionItem(Material.RED_TERRACOTTA, "Cancel Report", NamedTextColor.RED));
    }

    public static @NotNull Component confirmMenuTitle(@Nullable ReportMainItem selectedReason) {
        String reasonTitle = selectedReason != null ? confirmReasonTitle(selectedReason) : "Report";
        return text("Report " + reasonTitle, NamedTextColor.GRAY);
    }

    public static ReportMainItem getItemBySlot(int slot) {
        for (ReportMainItem item : ReportMainItem.values()) {
            if (item.slot() == slot) {
                return item;
            }
        }
        return null;
    }

    public static @Nullable ReportMainItem getReasonByActionKey(@NotNull String actionKey) {
        for (ReportMainItem item : ReportMainItem.values()) {
            if (item.type() == ReportMainItemType.REASON && actionKey.equalsIgnoreCase(item.actionKey())) {
                return item;
            }
        }
        return null;
    }

    private static @NotNull ItemStack createTargetHead(@NotNull OfflinePlayer target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.displayName(
                    text("/report ", NamedTextColor.GRAY)
                            .append(text(displayNameOf(target), NamedTextColor.AQUA))
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @NotNull ItemStack createConfirmTargetHead(
            @NotNull OfflinePlayer target,
            @NotNull ReportMainItem selectedReason
    ) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.displayName(
                    text("/report ", NamedTextColor.GRAY)
                            .append(text(displayNameOf(target), NamedTextColor.AQUA))
            );
            meta.lore(List.of(
                    text("Report " + displayNameOf(target) + " for " + confirmReasonTitle(selectedReason).toLowerCase(), NamedTextColor.YELLOW)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @NotNull ItemStack createConfirmActionItem(
            @NotNull Material material,
            @NotNull String title,
            @NotNull NamedTextColor color
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(text(title, color));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @NotNull ItemStack createMenuItem(@NotNull ReportMainItem item) {
        ItemStack stack = new ItemStack(item.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(text(item.title(), item.color()));
            meta.lore(item.lore());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static @NotNull String displayNameOf(@NotNull OfflinePlayer target) {
        return target.getName() != null ? target.getName() : "Unknown Player";
    }

    public enum ReportMainItem {
        CHAT_ABUSE_SCAM(
                20,
                Material.WRITABLE_BOOK,
                "Chat Abuse/Scam",
                NamedTextColor.GREEN,
                ReportMainItemType.REASON,
                "chat_abuse_scam",
                true,
                false,
                buildLore(
                        line("Using offensive chat, scamming, or", NamedTextColor.WHITE),
                        line("engaging in abusive chat behavior", NamedTextColor.WHITE),
                        Component.empty(),
                        line("Click to select.", NamedTextColor.YELLOW)
                )
        ),
        CHEATING_HACKING(
                21,
                Material.DIAMOND_SWORD,
                "Cheating (Hacking)",
                NamedTextColor.GREEN,
                ReportMainItemType.REASON,
                "cheating_hacking",
                true,
                true,
                buildLore(
                        line("Using disallowed modifications to gain", NamedTextColor.WHITE),
                        line("an unfair advantage", NamedTextColor.WHITE),
                        Component.empty(),
                        line("Click to select.", NamedTextColor.YELLOW)
                )
        ),
        BAD_NAME(
                22,
                Material.NAME_TAG,
                "Bad Name",
                NamedTextColor.GREEN,
                ReportMainItemType.REASON,
                "bad_name",
                true,
                false,
                buildLore(
                        line("Having an inappropriate username", NamedTextColor.WHITE),
                        Component.empty(),
                        line("Click to select.", NamedTextColor.YELLOW)
                )
        ),
        GUILD_NAME_TAG(
                23,
                Material.GREEN_BANNER,
                "Guild Name/Tag",
                NamedTextColor.GREEN,
                ReportMainItemType.REASON,
                "guild_name_tag",
                false,
                false,
                buildLore(
                        line("Inappropriate guild information", NamedTextColor.WHITE),
                        Component.empty(),
                        line("Click to select.", NamedTextColor.YELLOW)
                )
        ),
        CROSS_TEAMING(
                24,
                Material.COMPASS,
                "Cross Teaming",
                NamedTextColor.GREEN,
                ReportMainItemType.REASON,
                "cross_teaming",
                true,
                true,
                buildLore(
                        line("Working with the other team to gain", NamedTextColor.WHITE),
                        line("an advantage", NamedTextColor.WHITE),
                        Component.empty(),
                        line("Click to select.", NamedTextColor.YELLOW)
                )
        ),
        BAD_SKIN_CAPE(
                29,
                Material.LEATHER,
                "Bad Skin/Cape",
                NamedTextColor.GREEN,
                ReportMainItemType.REASON,
                "bad_skin_cape",
                true,
                false,
                buildLore(
                        line("Having an inappropriate skin or cape", NamedTextColor.WHITE),
                        Component.empty(),
                        line("Click to select.", NamedTextColor.YELLOW)
                )
        ),
        BAD_PET_NAME(
                30,
                Material.COW_SPAWN_EGG,
                "Bad Pet Name",
                NamedTextColor.GREEN,
                ReportMainItemType.REASON,
                "bad_pet_name",
                false,
                false,
                buildLore(
                        line("Having an inappropriate pet name", NamedTextColor.WHITE),
                        Component.empty(),
                        line("Click to select.", NamedTextColor.YELLOW)
                )
        ),
        STATS_BOOSTING(
                31,
                Material.TNT,
                "Stats Boosting",
                NamedTextColor.GREEN,
                ReportMainItemType.REASON,
                "stats_boosting",
                true,
                true,
                buildLore(
                        line("Using bots, other players, or", NamedTextColor.WHITE),
                        line("services to illegitimately boost or", NamedTextColor.WHITE),
                        line("improve game stats", NamedTextColor.WHITE),
                        Component.empty(),
                        line("Click to select.", NamedTextColor.YELLOW)
                )
        ),
        REPORT_INFORMATION(
                INFO_SLOT,
                Material.BOOK,
                "Use this menu to report a player breaking our rules",
                NamedTextColor.GREEN,
                ReportMainItemType.INFORMATION,
                null,
                false,
                false,
                buildLore(
                        line("Hover over the specific report type", NamedTextColor.WHITE),
                        line("for detailed information and then", NamedTextColor.WHITE),
                        line("select the closest option for the", NamedTextColor.WHITE),
                        line("rule being broken.", NamedTextColor.WHITE),
                        Component.empty(),
                        text("Visit ", NamedTextColor.WHITE)
                                .append(text("https://hypixel.net/rules", NamedTextColor.AQUA))
                                .append(text(" for", NamedTextColor.WHITE)),
                        line("more information.", NamedTextColor.WHITE)
                )
        ),
        CLOSE(
                CLOSE_SLOT,
                Material.BARRIER,
                "关闭",
                NamedTextColor.RED,
                ReportMainItemType.NAVIGATION,
                null,
                false,
                false,
                List.of()
        );

        private final int slot;
        private final Material material;
        private final String title;
        private final NamedTextColor color;
        private final ReportMainItemType type;
        private final String actionKey;
        private final boolean enabled;
        private final boolean saveLocation;
        private final List<Component> lore;

        ReportMainItem(
                int slot,
                Material material,
                String title,
                NamedTextColor color,
                ReportMainItemType type,
                String actionKey,
                boolean enabled,
                boolean saveLocation,
                List<Component> lore
        ) {
            this.slot = slot;
            this.material = material;
            this.title = title;
            this.color = color;
            this.type = type;
            this.actionKey = actionKey;
            this.enabled = enabled;
            this.saveLocation = saveLocation;
            this.lore = lore;
        }

        public int slot() {
            return slot;
        }

        public Material material() {
            return material;
        }

        public String title() {
            return title;
        }

        public NamedTextColor color() {
            return color;
        }

        public ReportMainItemType type() {
            return type;
        }

        public String actionKey() {
            return actionKey;
        }

        public boolean enabled() {
            return enabled;
        }

        public boolean saveLocation() {
            return saveLocation;
        }

        public List<Component> lore() {
            return lore;
        }
    }

    public enum ReportMainItemType {
        REASON,
        NAVIGATION,
        INFORMATION
    }

    private static @NotNull List<Component> buildLore(Component... lines) {
        return List.of(lines);
    }

    private static @NotNull Component line(@NotNull String text, @NotNull NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static @NotNull Component text(@NotNull String text, @NotNull NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static @NotNull String confirmReasonTitle(@NotNull ReportMainItem item) {
        return item.title().replace(" (", "/").replace(")", "");
    }
}
