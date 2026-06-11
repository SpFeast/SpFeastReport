package cn.spfeast.spfeastreport.commands;

import cn.spfeast.spfeastreport.SpFeastReportPlugin;
import cn.spfeast.spfeastreport.permission.PermissionNodes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ReportCheckCommand implements CommandExecutor {
    private final SpFeastReportPlugin plugin;

    public ReportCheckCommand(@NotNull SpFeastReportPlugin plugin) {
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

        if (!player.hasPermission(PermissionNodes.COMMAND_REPORTCHECK)
                || !player.hasPermission(PermissionNodes.REVIEW_VIEW)) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        plugin.getReportCheckMenuService().openCategoryMenu(player);
        return true;
    }
}
