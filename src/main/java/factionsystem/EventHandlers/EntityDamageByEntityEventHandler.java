package factionsystem.EventHandlers;

import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static factionsystem.Subsystems.UtilitySubsystem.isInFaction;

public class EntityDamageByEntityEventHandler {

    Main main = null;

    public EntityDamageByEntityEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(EntityDamageByEntityEvent event) {
        // this method disallows PVP between members of the same faction and between factions who are not at war
        // PVP is allowed between factionless players, players who belong to a faction and the factionless, and players whose factions are at war.

        // if this was between two players
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            int attackersFactionIndex = 0;
            int victimsFactionIndex = 0;

            for (int i = 0; i < main.factions.size(); i++) {
                if (main.factions.get(i).isMember(attacker.getName())) {
                    attackersFactionIndex = i;
                }
                if (main.factions.get(i).isMember(victim.getName())) {
                    victimsFactionIndex = i;
                }
            }

            // if attacker and victim are both in a faction
            if (isInFaction(attacker.getName(), main.factions) && isInFaction(victim.getName(), main.factions)) {
                // if attacker and victim are part of the same faction
                if (attackersFactionIndex == victimsFactionIndex) {
                    event.setCancelled(true);
                    attacker.sendMessage(ChatColor.RED + "You can't attack another player if you are part of the same faction.");
                    return;
                }

                // if attacker's faction and victim's faction are not at war
                if (!(main.factions.get(attackersFactionIndex).isEnemy(main.factions.get(victimsFactionIndex).getName())) &&
                        !(main.factions.get(victimsFactionIndex).isEnemy(main.factions.get(attackersFactionIndex).getName()))) {
                    event.setCancelled(true);
                    attacker.sendMessage(ChatColor.RED + "You can't attack another player if your factions aren't at war.");
                }
            }
        }
    }

}
