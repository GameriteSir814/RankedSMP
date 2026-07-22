package com.rankedsmp;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks three independent leaderboards:
 *
 * 1. PvP Rank  — position earned/stolen by killing other players.
 *                First kill ever on the server = #1. Killing a ranked player
 *                steals their rank; they shift down. Dying to an unranked
 *                player gives that player your rank; you become rank+1.
 *
 * 2. Kill Rank — sorted by total lifetime kill count (most kills = #1).
 *                Recalculated automatically, never manually set.
 *
 * 3. Size Rank — sorted by current scale attribute value (biggest = #1).
 *                Read live from the Evolution SMP plugin's attribute.
 *                Recalculated automatically every 2 seconds.
 */
public class RankData {

    // PvP rank stored on the player's PDC
    private final NamespacedKey pvpRankKey;
    // Kill count stored on the player's PDC (separate from EvolutionSMP's)
    private final NamespacedKey killCountKey;

    // Next PvP rank to assign. Starts at 1, increments as new players earn ranks.
    private int nextPvpRank = 1;

    public RankData(RankedSMP plugin) {
        pvpRankKey = new NamespacedKey(plugin, "pvp_rank");
        killCountKey = new NamespacedKey(plugin, "kill_count");
    }

    // ─────────────────────────────────────────────
    //  PvP Rank
    // ─────────────────────────────────────────────

    /** -1 = unranked */
    public int getPvpRank(Player p) {
        Integer v = p.getPersistentDataContainer().get(pvpRankKey, PersistentDataType.INTEGER);
        return v == null ? -1 : v;
    }

    public void setPvpRank(Player p, int rank) {
        p.getPersistentDataContainer().set(pvpRankKey, PersistentDataType.INTEGER, rank);
    }

    public void clearPvpRank(Player p) {
        p.getPersistentDataContainer().remove(pvpRankKey);
    }

    /**
     * Called when killer kills victim.
     *
     * Cases:
     * A) Killer unranked, victim unranked  → killer gets nextPvpRank, victim stays unranked (no rank to steal)
     * B) Killer unranked, victim ranked    → killer takes victim's rank, victim shifts to rank+1, everyone below shifts down... actually everyone above victim shifts - no. Victim drops one: everyone between victim's old rank and (victim's old rank + 1) shifts. Simplest: victim += 1.
     * C) Killer ranked,   victim unranked  → killer keeps rank, victim stays unranked
     * D) Killer ranked,   victim ranked    → killer takes victim's rank if victim rank < killer rank (victim was higher); victim gets killer's old rank. If killer was already higher rank (lower number), nothing changes in position order but victim still drops 1.
     *
     * Simplified ruleset matching your description:
     * - Killer always takes victim's rank (if victim was ranked and victim rank < killer rank or killer unranked)
     * - If victim unranked and killer unranked → killer earns nextPvpRank
     * - Victim always drops one rank position
     */
    public void handleKill(Player killer, Player victim) {
        int killerRank = getPvpRank(killer);
        int victimRank = getPvpRank(victim);

        if (victimRank == -1) {
            // Victim was unranked
            if (killerRank == -1) {
                // Both unranked — killer earns the next available rank
                setPvpRank(killer, nextPvpRank);
                nextPvpRank++;
            }
            // If killer already ranked and victim unranked — no rank change
        } else {
            // Victim was ranked — killer steals their rank
            // Shift everyone who currently holds a rank >= victimRank down by 1
            // (including the victim themselves, who will be set below)
            for (Player online : Bukkit.getOnlinePlayers()) {
                int r = getPvpRank(online);
                if (r >= victimRank && !online.getUniqueId().equals(killer.getUniqueId())) {
                    setPvpRank(online, r + 1);
                }
            }
            // Also fix offline players stored in PDC isn't possible without
            // iterating all offline players - we do it on join instead (see PlayerListener).
            // Killer takes the now-vacated rank
            setPvpRank(killer, victimRank);

            // nextPvpRank might need bumping if we've issued ranks beyond it
            nextPvpRank = Math.max(nextPvpRank, getHighestPvpRankOnline() + 1);
        }
    }

    private int getHighestPvpRankOnline() {
        int max = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            int r = getPvpRank(p);
            if (r > max) max = r;
        }
        return max;
    }

    // ─────────────────────────────────────────────
    //  Kill Count (for Kill Rank leaderboard)
    // ─────────────────────────────────────────────

    public int getKillCount(Player p) {
        Integer v = p.getPersistentDataContainer().get(killCountKey, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public void incrementKillCount(Player p) {
        int current = getKillCount(p);
        p.getPersistentDataContainer().set(killCountKey, PersistentDataType.INTEGER, current + 1);
    }

    /**
     * Returns online players sorted by kill count descending.
     * Position in list = kill rank (index 0 = #1).
     */
    public List<Player> getKillRankingOnline() {
        return new java.util.ArrayList<>(Bukkit.getOnlinePlayers()).stream()
                .sorted((a, b) -> Integer.compare(getKillCount(b), getKillCount(a)))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    //  Size Rank (read from scale attribute live)
    // ─────────────────────────────────────────────

    public double getSize(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.SCALE);
        if (attr == null) return 1.0;
        return attr.getBaseValue();
    }

    /**
     * Returns online players sorted by size descending.
     * Position in list = size rank (index 0 = #1).
     */
    public List<Player> getSizeRankingOnline() {
        return new java.util.ArrayList<>(Bukkit.getOnlinePlayers()).stream()
                .sorted((a, b) -> Double.compare(getSize(b), getSize(a)))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    //  Kill rank position for a specific player
    // ─────────────────────────────────────────────

    /** 1-based kill rank among online players. -1 if no kills. */
    public int getKillRank(Player p) {
        if (getKillCount(p) == 0) return -1;
        List<Player> ranked = getKillRankingOnline();
        int idx = ranked.indexOf(p);
        return idx == -1 ? -1 : idx + 1;
    }

    /** 1-based size rank among online players. */
    public int getSizeRank(Player p) {
        List<Player> ranked = getSizeRankingOnline();
        int idx = ranked.indexOf(p);
        return idx == -1 ? -1 : idx + 1;
    }
}
