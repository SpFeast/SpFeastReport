package cn.spfeast.spfeastreport;

import cn.spfeast.spfeastreport.commands.ReportCommand;
import cn.spfeast.spfeastreport.commands.ReportCheckCommand;
import cn.spfeast.spfeastreport.config.ReportCategoryConfig;
import cn.spfeast.spfeastreport.gui.ReportCheckMenuService;
import cn.spfeast.spfeastreport.gui.ReportMenuService;
import cn.spfeast.spfeastreport.listeners.ReportCheckMenuListener;
import cn.spfeast.spfeastreport.listeners.ReportMenuListener;
import cn.spfeast.spfeastreport.storage.ReportStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SpFeastReportPlugin extends JavaPlugin {
    private ReportCategoryConfig categoryConfig;
    private ReportMenuService reportMenuService;
    private ReportCheckMenuService reportCheckMenuService;
    private ReportStorage reportStorage;

    @Override
    public void onEnable() {
        categoryConfig = new ReportCategoryConfig(this);
        reportMenuService = new ReportMenuService(this);
        reportCheckMenuService = new ReportCheckMenuService(this);
        reportStorage = new ReportStorage(this);
        registerCommands();
        getServer().getPluginManager().registerEvents(new ReportMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ReportCheckMenuListener(this), this);
        getLogger().info("[SpFeastReport] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[SpFeastReport] Plugin disabled.");
    }

    private void registerCommands() {
        PluginCommand reportCommand = Objects.requireNonNull(
                getCommand("report"),
                "report command is missing from plugin.yml"
        );
        reportCommand.setExecutor(new ReportCommand(this));

        PluginCommand reportCheckCommand = Objects.requireNonNull(
                getCommand("reportcheck"),
                "reportcheck command is missing from plugin.yml"
        );
        reportCheckCommand.setExecutor(new ReportCheckCommand(this));
    }

    public ReportStorage getReportStorage() {
        return reportStorage;
    }

    public ReportCategoryConfig getCategoryConfig() {
        return categoryConfig;
    }

    public ReportMenuService getReportMenuService() {
        return reportMenuService;
    }

    public ReportCheckMenuService getReportCheckMenuService() {
        return reportCheckMenuService;
    }
}
