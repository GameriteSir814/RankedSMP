package com.rankedsmp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final RankedSMP plugin;
    private final RankData rankData;
    private final TablistManager tablistManager;

    public PlayerListener(RankedSMP plugin, RankData rankData, TablistManager tablistManager) {
        this.plugin = plugin;
        this.rankData = rankData;
        this.tablistManager = tablistManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Refresh tablist a tick later so the player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, tablistManager::refreshAll, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, tablistManager::refreshAll, 2L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            rankData.handleKill(killer, victim);
            rankData.incrementKillCount(killer);
            tablistManager.refreshAll();
        }
    }
}
