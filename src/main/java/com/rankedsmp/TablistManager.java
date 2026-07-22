package com.rankedsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Writes three leaderboards into the tab list header for every online player.
 * The footer shows each individual player's own ranks.
 *
 * Layout (header, shown to everyone):
 *
 *  ── PvP Rank ──       ── Kill Rank ──      ── Size Rank ──
 *  #1 PlayerA           #1 PlayerB (32k)      #1 PlayerC (4.00x)
 *  #2 PlayerB           #2 PlayerA (18k)      #2 PlayerA (2.00x)
 *  ...                  ...                   ...
 */
public class TablistManager {

    private final RankedSMP plugin;
    private final RankData rankData;

    public TablistManager(RankedSMP plugin, RankData rankData) {
        this.plugin = plugin;
        this.rankData = rankData;
    }

    public void refreshAll() {
        Component header = buildHeader();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Component footer = buildFooter(p);
            p.sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    private Component buildHeader() {
        List<Player> pvpOrder = getSortedByPvpRank();
        List<Player> killOrder = rankData.getKillRankingOnline();
        List<Player> sizeOrder = rankData.getSizeRankingOnline();

        int rows = Math.max(Math.max(pvpOrder.size(), killOrder.size()), sizeOrder.size());
        rows = Math.min(rows, 10); // cap at top 10 per column

        Component out = Component.newline()
                .append(col("⚔ PvP Rank", NamedTextColor.RED))
                .append(gap())
                .append(col("☠ Kill Rank", NamedTextColor.YELLOW))
                .append(gap())
                .append(col("⬆ Size Rank", NamedTextColor.AQUA))
                .append(Component.newline());

        for (int i = 0; i < rows; i++) {
            // PvP column
            if (i < pvpOrder.size()) {
                Player p = pvpOrder.get(i);
                int rank = rankData.getPvpRank(p);
                out = out.append(entry("#" + rank + " " + p.getName(), NamedTextColor.RED));
            } else {
                out = out.append(blank());
            }
            out = out.append(gap());

            // Kill column
            if (i < killOrder.size()) {
                Player p = killOrder.get(i);
                int kills = rankData.getKillCount(p);
                if (kills > 0) {
                    out = out.append(entry("#" + (i + 1) + " " + p.getName() + " (" + kills + ")", NamedTextColor.YELLOW));
                } else {
                    out = out.append(blank());
                }
            } else {
                out = out.append(blank());
            }
            out = out.append(gap());

            // Size column
            if (i < sizeOrder.size()) {
                Player p = sizeOrder.get(i);
                double size = rankData.getSize(p);
                out = out.append(entry("#" + (i + 1) + " " + p.getName() + " (" + String.format("%.2f", size) + "x)", NamedTextColor.AQUA));
            } else {
                out = out.append(blank());
            }

            out = out.append(Component.newline());
        }

        return out;
    }

    private Component buildFooter(Player p) {
        int pvp = rankData.getPvpRank(p);
        int kill = rankData.getKillRank(p);
        int size = rankData.getSizeRank(p);

        String pvpStr  = pvp  == -1 ? "Unranked" : "#" + pvp;
        String killStr = kill == -1 ? "Unranked" : "#" + kill;
        String sizeStr = size == -1 ? "?" : "#" + size;

        return Component.newline()
                .append(Component.text("Your ranks — ", NamedTextColor.GRAY))
                .append(Component.text("PvP: ", NamedTextColor.RED))
                .append(Component.text(pvpStr, NamedTextColor.WHITE))
                .append(Component.text("  Kills: ", NamedTextColor.YELLOW))
                .append(Component.text(killStr, NamedTextColor.WHITE))
                .append(Component.text("  Size: ", NamedTextColor.AQUA))
                .append(Component.text(sizeStr, NamedTextColor.WHITE))
                .append(Component.newline());
    }

    /** Returns online players who have a PvP rank, sorted by rank ascending. */
    private List<Player> getSortedByPvpRank() {
        return new java.util.ArrayList<>(Bukkit.getOnlinePlayers()).stream()
                .filter(p -> rankData.getPvpRank(p) != -1)
                .sorted((a, b) -> Integer.compare(rankData.getPvpRank(a), rankData.getPvpRank(b)))
                .collect(java.util.stream.Collectors.toList());
    }

    private Component col(String text, NamedTextColor color) {
        return Component.text(text, color, TextDecoration.BOLD)
                .append(Component.text("          ", NamedTextColor.DARK_GRAY));
    }

    private Component gap() {
        return Component.text("   ", NamedTextColor.DARK_GRAY);
    }

    private Component entry(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    private Component blank() {
        return Component.text("                    ", NamedTextColor.DARK_GRAY);
    }
}
