package dansplugins.factionsystem.data;

import dansplugins.factionsystem.objects.Duel;
import dansplugins.factionsystem.objects.Gate;
import dansplugins.factionsystem.utils.Pair;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class EphemeralData {

    // instance
    private static EphemeralData instance;

    private HashMap<UUID, Gate> creatingGatePlayers = new HashMap<>();

    private ArrayList<UUID> lockingPlayers = new ArrayList<>();
    private ArrayList<UUID> unlockingPlayers = new ArrayList<>();

    // Left user granting access, right user receiving access;
    private HashMap<UUID, UUID> playersGrantingAccess = new HashMap<>();
    private ArrayList<UUID> playersCheckingAccess = new ArrayList<>();

    // Left user granting access, right user receiving access;
    private HashMap<UUID, UUID> playersRevokingAccess = new HashMap<>();
    private ArrayList<UUID> playersInFactionChat = new ArrayList<>();
    private ArrayList<UUID> adminsBypassingProtections = new ArrayList<>();

    // List of players who made the cloud and the cloud itself in a pair
    private ArrayList<Pair<Player, AreaEffectCloud>> activeAOEClouds = new ArrayList<>();

    // duels
    private ArrayList<Duel> duelingPlayers = new ArrayList<>();

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

    // specific getters ---

    public Duel getDuel(Player player, Player target)
    {
        for (Duel duel : getDuelingPlayers())
        {
            if (duel.hasPlayer(player) && duel.hasPlayer(target))
            {
                return duel;
            }
        }
        return null;
    }

}
