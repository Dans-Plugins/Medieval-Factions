package factionsystem.EventHandlers;

import factionsystem.Main;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import static factionsystem.Subsystems.UtilitySubsystem.isInFaction;

public class EntityDamageByEntityEventHandler {

    Main main = null;

    public EntityDamageByEntityEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(EntityDamageByEntityEvent event) {
        // this method disallows PVP between members of the same faction and between factions who are not at war
        // PVP is allowed between factionless players, players who belong to a faction and the factionless, and players whose factions are at war.

        // if this was between two players melee
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            handleIfFriendlyFire(event, attacker, victim);
        }
        else if (event.getDamager() instanceof Projectile && event.getEntity() instanceof Player) {
            Projectile arrow = (Projectile) event.getDamager();
            ProjectileSource source = arrow.getShooter();

            if (source instanceof Player){
                Player attacker = (Player) source;
                Player victim = (Player) event.getEntity();

                handleIfFriendlyFire(event, attacker, victim);
            }
        }
    }

    private void handleIfFriendlyFire(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        if (arePlayersInSameFaction(attacker, victim)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "You can't attack another player if you are part of the same faction.");
        }

        // if attacker's faction and victim's faction are not at war
        else if (arePlayersFactionsAllies(attacker, victim)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "You can't attack another player if your factions aren't at war.");
        }
    }

    private boolean arePlayersFactionsAllies(Player player1, Player player2) {
        Pair<Integer, Integer> factionIndices = getFactionIndices(player1, player2);
        int attackersFactionIndex = factionIndices.getKey();
        int victimsFactionIndex = factionIndices.getValue();

        return !(main.factions.get(attackersFactionIndex).isEnemy(main.factions.get(victimsFactionIndex).getName())) &&
                !(main.factions.get(victimsFactionIndex).isEnemy(main.factions.get(attackersFactionIndex).getName()));
    }

    private boolean arePlayersInSameFaction(Player player1, Player player2) {
        Pair<Integer, Integer> factionIndices = getFactionIndices(player1, player2);
        int attackersFactionIndex = factionIndices.getKey();
        int victimsFactionIndex = factionIndices.getValue();

        // if attacker and victim are both in a faction
        if (arePlayersInAFaction(player1, player2)){
            // if attacker and victim are part of the same faction
            return attackersFactionIndex == victimsFactionIndex;
        } else {
            return false;
        }
    }

    private boolean arePlayersInAFaction(Player player1, Player player2) {
        return isInFaction(player1.getName(), main.factions) && isInFaction(player2.getName(), main.factions);
    }

    private Pair<Integer, Integer> getFactionIndices(Player player1, Player player2){
        int attackersFactionIndex = 0;
        int victimsFactionIndex = 0;

        for (int i = 0; i < main.factions.size(); i++) {
            if (main.factions.get(i).isMember(player1.getName())) {
                attackersFactionIndex = i;
            }
            if (main.factions.get(i).isMember(player2.getName())) {
                victimsFactionIndex = i;
            }
        }

        return new ImmutablePair<>(attackersFactionIndex, victimsFactionIndex);
    }

}
