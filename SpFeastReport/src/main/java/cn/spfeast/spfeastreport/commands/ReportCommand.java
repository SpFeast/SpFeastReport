package cn.spfeast.spfeastreport.commands;

import cn.spfeast.spfeastreport.SpFeastReportPlugin;
import cn.spfeast.spfeastreport.permission.PermissionNodes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReportCommand implements CommandExecutor {
    private final SpFeastReportPlugin plugin;

    public ReportCommand(SpFeastReportPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家在游戏内执行。");
            return true;
        }

        if (!player.hasPermission(PermissionNodes.COMMAND_REPORT)) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("`/report` 的主页面后面再加，当前先用 /report <玩家名>。", NamedTextColor.YELLOW));
            return true;
        }

        String targetName = args[0];
        player.sendMessage(Component.text("Please wait...", NamedTextColor.GRAY));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = findReportTarget(targetName);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }

                if (target == null) {
                    player.sendMessage(Component.text("I couldn't find the player you want to report!", NamedTextColor.RED));
                    return;
                }

                plugin.getReportMenuService().openMainMenu(player, target);
            });
        });

        return true;
    }

    private @Nullable OfflinePlayer findReportTarget(@NotNull String targetName) {
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget != null) {
            return onlineTarget;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (offlineTarget.hasPlayedBefore()) {
            return offlineTarget;
        }

        return null;
    }
}
