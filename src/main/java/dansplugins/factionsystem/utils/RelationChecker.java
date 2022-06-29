package dansplugins.factionsystem.utils;

import org.bukkit.entity.Player;

import dansplugins.factionsystem.data.PersistentData;
import preponderous.ponder.misc.Pair;

public class RelationChecker {
    private final PersistentData persistentData;

    public RelationChecker(PersistentData persistentData) {
        this.persistentData = persistentData;
    }

    public boolean arePlayersInAFaction(Player player1, Player player2) {
        return persistentData.isInFaction(player1.getUniqueId()) && persistentData.isInFaction(player2.getUniqueId());
    }

    public boolean playerNotInFaction(Player player) {
        return persistentData.getPlayersFaction(player.getUniqueId()) == null;
    }

    public boolean playerInFaction(Player player) {
        return persistentData.isInFaction(player.getUniqueId());
    }

    public boolean arePlayersInSameFaction(Player player1, Player player2) {
        Pair<Integer, Integer> factionIndices = getFactionIndices(player1, player2);
        int attackersFactionIndex = factionIndices.getLeft();
        int victimsFactionIndex = factionIndices.getRight();
        return arePlayersInAFaction(player1, player2) && attackersFactionIndex == victimsFactionIndex;
    }

    public boolean arePlayersFactionsNotEnemies(Player player1, Player player2) {
        Pair<Integer, Integer> factionIndices = getFactionIndices(player1, player2);
        int attackersFactionIndex = factionIndices.getLeft();
        int victimsFactionIndex = factionIndices.getRight();

        return !(persistentData.getFactionByIndex(attackersFactionIndex).isEnemy(persistentData.getFactionByIndex(victimsFactionIndex).getName())) &&
                !(persistentData.getFactionByIndex(victimsFactionIndex).isEnemy(persistentData.getFactionByIndex(attackersFactionIndex).getName()));
    }

    private Pair<Integer, Integer> getFactionIndices(Player player1, Player player2) {
        int attackersFactionIndex = 0;
        int victimsFactionIndex = 0;

        if (player1 != null && player2 != null) {
            for (int i = 0; i < persistentData.getNumFactions(); i++) {
                if (persistentData.getFactionByIndex(i).isMember(player1.getUniqueId())) {
                    attackersFactionIndex = i;
                }
                if (persistentData.getFactionByIndex(i).isMember(player2.getUniqueId())) {
                    victimsFactionIndex = i;
                }
            }
        }
        return new Pair<>(attackersFactionIndex, victimsFactionIndex);
    }
}