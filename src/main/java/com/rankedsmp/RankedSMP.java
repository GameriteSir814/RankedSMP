package com.rankedsmp;

import org.bukkit.plugin.java.JavaPlugin;

public class RankedSMP extends JavaPlugin {

    private static RankedSMP instance;
    private RankData rankData;
    private TablistManager tablistManager;

    @Override
    public void onEnable() {
        instance = this;

        rankData = new RankData(this);
        tablistManager = new TablistManager(this, rankData);

        getServer().getPluginManager().registerEvents(new PlayerListener(this, rankData, tablistManager), this);
        getCommand("ranks").setExecutor(new RanksCommand(rankData));

        // Refresh tablist every 2 seconds so size rank stays current
        getServer().getScheduler().runTaskTimer(this, tablistManager::refreshAll, 40L, 40L);

        getLogger().info("RankedSMP enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("RankedSMP disabled.");
    }

    public static RankedSMP getInstance() {
        return instance;
    }

    public RankData getRankData() {
        return rankData;
    }

    public TablistManager getTablistManager() {
        return tablistManager;
    }
}
