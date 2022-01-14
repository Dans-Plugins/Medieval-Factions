/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.eventhandlers.helper.RelationChecker;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Duel;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalChunkService;
import dansplugins.factionsystem.services.LocalLocaleService;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import preponderous.ponder.misc.Pair;

/**
 * @author Daniel McCoy Stephenson
 */
public class DamageHandler implements Listener {

    /**
     * This method disallows PVP between members of the same faction and between factions who are not at war
     * PVP is allowed between factionless players, players who belong to a faction and the factionless, and players whose factions are at war.
     * It also handles damage to entities by players.
     */
    @EventHandler()
    public void handle(EntityDamageByEntityEvent event) {
        Player attacker = getPlayerInStoredCloudPair(event);
        Player victim = getVictim(event);
        handlePlayerVersusPlayer(attacker, victim, event);
        handleEntityDamage(attacker, event);
    }

    private void handlePlayerVersusPlayer(Player attacker, Player victim, EntityDamageByEntityEvent event) {
        if (victim == null) {
            return;
        }

        if (arePlayersDueling(attacker, victim)) {
            endDuelIfNecessary(attacker, victim, event);
        }
        else {
            handleIfFriendlyFire(event, attacker, victim);
        }
    }

    private void handleEntityDamage(Player attacker, EntityDamageByEntityEvent event) {
        if (attacker == null) {
            return;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(attacker.getUniqueId());
        if (playersFaction == null) {
            event.setCancelled(true);
            return;
        }

        if (isEntityProtected(event.getEntity())) {
            cancelDamageIfNecessary(event, playersFaction);
        }
    }

    private void cancelDamageIfNecessary(EntityDamageByEntityEvent event, Faction playersFaction) {
        ClaimedChunk claimedChunk = getClaimedChunkAtLocation(event.getEntity().getLocation());
        if (claimedChunk == null) {
            return;
        }

        if (!isHolderPlayersFaction(claimedChunk, playersFaction)) {
            event.setCancelled(true);
        }
    }

    private boolean isHolderPlayersFaction(ClaimedChunk claimedChunk, Faction playersFaction) {
        return !claimedChunk.getHolder().equalsIgnoreCase(playersFaction.getName());
    }

    private ClaimedChunk getClaimedChunkAtLocation(Location location) {
        Chunk chunk = location.getChunk();
        return LocalChunkService.getInstance().getClaimedChunk(chunk);
    }

    private boolean isEntityProtected(Entity entity) {
        return entity instanceof ArmorStand || entity instanceof ItemFrame;

    }

    private Player getVictim(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            return (Player) event.getEntity();
        }
        else {
            return null;
        }
    }

    private Player getPlayerInStoredCloudPair(EntityDamageByEntityEvent event) {
        if (wasDamageWasBetweenPlayers(event)) {
            return (Player) event.getDamager();
        }
        else if (wasPlayerWasDamagedByAProjectile(event) && wasProjectileShotByPlayer(event)) {
            return (Player) getProjectileSource(event);
        }
        else {
            return null;
        }
    }

    private ProjectileSource getProjectileSource(EntityDamageByEntityEvent event) {
        Projectile projectile = (Projectile) event.getDamager();
        return projectile.getShooter();
    }

    private boolean wasProjectileShotByPlayer(EntityDamageByEntityEvent event) {
        ProjectileSource projectileSource = getProjectileSource(event);
        return projectileSource instanceof Player;
    }

    private void endDuelIfNecessary(Player attacker, Player victim, EntityDamageEvent event) {
        Duel duel = EphemeralData.getInstance().getDuel(attacker, victim);
        if (isDuelActive(duel) && isVictimDead(victim.getHealth(), event.getFinalDamage())) {
            duel.setLoser(victim);
            duel.finishDuel(false);
            EphemeralData.getInstance().getDuelingPlayers().remove(this);
            event.setCancelled(true);
        }
    }

    private boolean isVictimDead(double victimHealth, double finalDamage) {
        return victimHealth - finalDamage <= 0;
    }

    private boolean isDuelActive(Duel duel) {
        return duel.getStatus().equals(Duel.DuelState.DUELLING);
    }

    private boolean arePlayersDueling(Player attacker, Player victim) {
        Duel duel = EphemeralData.getInstance().getDuel(attacker, victim);
        return duel != null;
    }

    private boolean wasPlayerWasDamagedByAProjectile(EntityDamageByEntityEvent event) {
        return event.getDamager() instanceof Projectile && event.getEntity() instanceof Player;
    }

    private boolean wasDamageWasBetweenPlayers(EntityDamageByEntityEvent event) {
        return event.getDamager() instanceof Player && event.getEntity() instanceof Player;
    }

    private void handleIfFriendlyFire(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        if (RelationChecker.getInstance().arePlayersInSameFaction(attacker, victim)) {
            Faction faction = PersistentData.getInstance().getPlayersFaction(attacker.getUniqueId());
            boolean friendlyFireAllowed = (boolean) faction.getFlags().getFlag("allowfriendlyFire");
            if (!friendlyFireAllowed) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CannotAttackFactionMember"));
            }
        }
        else if (RelationChecker.getInstance().arePlayersFactionsNotEnemies(attacker, victim)) { // if attacker's faction and victim's faction are not at war
            if (MedievalFactions.getInstance().getConfig().getBoolean("warsRequiredForPVP")) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CannotAttackNonWarringPlayer"));
            }
        }
    }
}