package dansplugins.factionsystem.eventhandlers.helper;

import dansplugins.factionsystem.data.PersistentData;
import org.bukkit.entity.Player;
import preponderous.ponder.misc.Pair;

public class RelationChecker {
    private static RelationChecker instance;

    private RelationChecker() {

    }

    public static RelationChecker getInstance() {
        if (instance == null) {
            instance = new RelationChecker();
        }
        return instance;
    }

    public boolean arePlayersInAFaction(Player player1, Player player2) {
        return PersistentData.getInstance().isInFaction(player1.getUniqueId()) && PersistentData.getInstance().isInFaction(player2.getUniqueId());
    }

    public boolean playerNotInFaction(Player player) {
        return PersistentData.getInstance().getPlayersFaction(player.getUniqueId()) == null;
    }

    public boolean playerInFaction(Player player) {
        return PersistentData.getInstance().isInFaction(player.getUniqueId());
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

        return !(PersistentData.getInstance().getFactionByIndex(attackersFactionIndex).isEnemy(PersistentData.getInstance().getFactionByIndex(victimsFactionIndex).getName())) &&
                !(PersistentData.getInstance().getFactionByIndex(victimsFactionIndex).isEnemy(PersistentData.getInstance().getFactionByIndex(attackersFactionIndex).getName()));
    }

    private Pair<Integer, Integer> getFactionIndices(Player player1, Player player2) {
        int attackersFactionIndex = 0;
        int victimsFactionIndex = 0;

        for (int i = 0; i < PersistentData.getInstance().getNumFactions(); i++) {
            if (PersistentData.getInstance().getFactionByIndex(i).isMember(player1.getUniqueId())) {
                attackersFactionIndex = i;
            }
            if (PersistentData.getInstance().getFactionByIndex(i).isMember(player2.getUniqueId())) {
                victimsFactionIndex = i;
            }
        }

        return new Pair<>(attackersFactionIndex, victimsFactionIndex);
    }
}