package com.rankedsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RanksCommand implements CommandExecutor {

    private final RankData rankData;

    public RanksCommand(RankData rankData) {
        this.rankData = rankData;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("=== LEADERBOARDS ===", NamedTextColor.GOLD));

        // PvP Rank
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("⚔ PvP Rank", NamedTextColor.RED));
        List<Player> pvp = new java.util.ArrayList<>(Bukkit.getOnlinePlayers()).stream()
                .filter(p -> rankData.getPvpRank(p) != -1)
                .sorted((a, b) -> Integer.compare(rankData.getPvpRank(a), rankData.getPvpRank(b)))
                .collect(java.util.stream.Collectors.toList());
        if (pvp.isEmpty()) {
            sender.sendMessage(Component.text("  No one ranked yet.", NamedTextColor.GRAY));
        } else {
            for (Player p : pvp) {
                sender.sendMessage(Component.text("  #" + rankData.getPvpRank(p) + " " + p.getName(), NamedTextColor.WHITE));
            }
        }

        // Kill Rank
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("☠ Kill Rank", NamedTextColor.YELLOW));
        List<Player> kills = rankData.getKillRankingOnline();
        boolean anyKills = false;
        for (int i = 0; i < Math.min(kills.size(), 10); i++) {
            Player p = kills.get(i);
            int k = rankData.getKillCount(p);
            if (k > 0) {
                sender.sendMessage(Component.text("  #" + (i + 1) + " " + p.getName() + " — " + k + " kills", NamedTextColor.WHITE));
                anyKills = true;
            }
        }
        if (!anyKills) sender.sendMessage(Component.text("  No kills yet.", NamedTextColor.GRAY));

        // Size Rank
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("⬆ Size Rank", NamedTextColor.AQUA));
        List<Player> sizes = rankData.getSizeRankingOnline();
        for (int i = 0; i < Math.min(sizes.size(), 10); i++) {
            Player p = sizes.get(i);
            double s = rankData.getSize(p);
            sender.sendMessage(Component.text("  #" + (i + 1) + " " + p.getName() + " — " + String.format("%.4f", s) + "x", NamedTextColor.WHITE));
        }

        sender.sendMessage(Component.text(""));
        return true;
    }
}
