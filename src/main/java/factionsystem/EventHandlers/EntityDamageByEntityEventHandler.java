package factionsystem.EventHandlers;

import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

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
        if (!main.utilities.arePlayersInAFaction(attacker, victim)){
            // Factionless can fight anyone.
            return;
        }
        else if (main.utilities.arePlayersInSameFaction(attacker, victim)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "You can't attack another player if you are part of the same faction.");
        }

        // if attacker's faction and victim's faction are not at war
        else if (main.utilities.arePlayersFactionsNotEnemies(attacker, victim)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "You can't attack another player if your factions aren't at war.");
        }
    }

}
