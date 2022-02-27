/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.objects.domain.Duel;
import dansplugins.factionsystem.objects.domain.Gate;
import preponderous.ponder.misc.Pair;

/**
 * @author Daniel McCoy Stephenson
 */
public class EphemeralData {
    private static EphemeralData instance;

    private final HashMap<UUID, Gate> creatingGatePlayers = new HashMap<>();

    private final ArrayList<UUID> lockingPlayers = new ArrayList<>();
    private final ArrayList<UUID> unlockingPlayers = new ArrayList<>();
    private final ArrayList<UUID> forcefullyUnlockingPlayers = new ArrayList<>();

    // Left user granting access, right user receiving access;
    private final HashMap<UUID, UUID> playersGrantingAccess = new HashMap<>();
    private final ArrayList<UUID> playersCheckingAccess = new ArrayList<>();

    // Left user granting access, right user receiving access;
    private final HashMap<UUID, UUID> playersRevokingAccess = new HashMap<>();
    private final ArrayList<UUID> playersInFactionChat = new ArrayList<>();
    private final ArrayList<UUID> adminsBypassingProtections = new ArrayList<>();

    // List of players who made the cloud and the cloud itself in a pair
    private final ArrayList<Pair<Player, AreaEffectCloud>> activeAOEClouds = new ArrayList<>();

    // duels
    private final ArrayList<Duel> duelingPlayers = new ArrayList<>();

    private EphemeralData() {

    }

    public static EphemeralData getInstance() {
        if (instance == null) {
            instance = new EphemeralData();
        }
        return instance;
    }

    // arraylist getters ---

    public HashMap<UUID, Gate> getCreatingGatePlayers() {
        return creatingGatePlayers;
    }

    public ArrayList<UUID> getLockingPlayers() {
        return lockingPlayers;
    }

    public ArrayList<UUID> getUnlockingPlayers() {
        return unlockingPlayers;
    }

    public HashMap<UUID, UUID> getPlayersGrantingAccess() {
        return playersGrantingAccess;
    }

    public ArrayList<UUID> getPlayersCheckingAccess() {
        return playersCheckingAccess;
    }

    public HashMap<UUID, UUID> getPlayersRevokingAccess() {
        return playersRevokingAccess;
    }

    public ArrayList<UUID> getPlayersInFactionChat() {
        return playersInFactionChat;
    }

    public ArrayList<UUID> getAdminsBypassingProtections() {
        return adminsBypassingProtections;
    }

    public ArrayList<Pair<Player, AreaEffectCloud>> getActiveAOEClouds() {
        return activeAOEClouds;
    }

    public ArrayList<Duel> getDuelingPlayers() {
        return duelingPlayers;
    }

    public ArrayList<UUID> getForcefullyUnlockingPlayers() {
        return forcefullyUnlockingPlayers;
    }

    // specific getters ---

    public Duel getDuel(Player player, Player target) {
        for (Duel duel : getDuelingPlayers()) {
            if (duel.hasPlayer(player) && duel.hasPlayer(target)) {
                return duel;
            }
        }
        return null;
    }

    public boolean isPlayerInFactionChat(Player player) {
        return getPlayersInFactionChat().contains(player.getUniqueId());
    }
}