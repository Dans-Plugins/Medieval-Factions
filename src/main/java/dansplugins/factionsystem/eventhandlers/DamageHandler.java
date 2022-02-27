/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.eventhandlers.helper.RelationChecker;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Duel;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.Logger;

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
        Player attacker = getAttacker(event);
        Player victim = getVictim(event);

        if (attacker == null || victim == null) {
            Logger.getInstance().log("Attacker and/or victim was null in the DamageHandler class.");
            return;
        }

        handlePlayerVersusPlayer(attacker, victim, event);
        handleEntityDamage(attacker, event);
    }

    /**
     * Cases:
     * 1) Players are dueling
     * 2) Victim is not in a faction or attacker is not in a faction.
     * 3) Players are in the same faction
     * 4) Players are not in the same faction but are not enemies.
     */
    private void handlePlayerVersusPlayer(Player attacker, Player victim, EntityDamageByEntityEvent event) {

        // case 1
        if (arePlayersDueling(attacker, victim)) {
            endDuelIfNecessary(attacker, victim, event);
            return;
        }

        // case 2
        if (RelationChecker.getInstance().playerNotInFaction(attacker) || RelationChecker.getInstance().playerNotInFaction(victim)) {
            // allow since factionless don't have PVP restrictions
            return;
        }

        // case 3
        if (RelationChecker.getInstance().arePlayersInSameFaction(attacker, victim)){
            handleFriendlyFire(event, attacker, victim);
            return;
        }

        // case 4
        if (RelationChecker.getInstance().arePlayersFactionsNotEnemies(attacker, victim)) {
            handleNonEnemyFire(event, attacker, victim);
        }
    }

    private void handleEntityDamage(Player attacker, EntityDamageByEntityEvent event) {
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
        return claimedChunk.getHolder().equalsIgnoreCase(playersFaction.getName());
    }

    private ClaimedChunk getClaimedChunkAtLocation(Location location) {
        Chunk chunk = location.getChunk();
        return PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(chunk);
    }

    private boolean isEntityProtected(Entity entity) {
        return entity instanceof ArmorStand || entity instanceof ItemFrame;

    }

    private Player getVictim(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            return (Player) event.getEntity();
        } else {
            return null;
        }
    }

    private Player getAttacker(EntityDamageByEntityEvent event) {
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
            EphemeralData.getInstance().getDuelingPlayers().remove(duel);
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
        if (attacker == null) {
            return false;
        }
        Duel duel = EphemeralData.getInstance().getDuel(attacker, victim);
        return duel != null;
    }

    private boolean wasPlayerWasDamagedByAProjectile(EntityDamageByEntityEvent event) {
        return event.getDamager() instanceof Projectile && event.getEntity() instanceof Player;
    }

    private boolean wasDamageWasBetweenPlayers(EntityDamageByEntityEvent event) {
        return event.getDamager() instanceof Player && event.getEntity() instanceof Player;
    }

    /**
     * This method is intended to prevent friendly fire if it is not allowed in the faction.
     */
    private void handleFriendlyFire(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        Faction faction = PersistentData.getInstance().getPlayersFaction(attacker.getUniqueId());
        boolean friendlyFireAllowed = (boolean) faction.getFlags().getFlag("allowFriendlyFire");
        if (!friendlyFireAllowed) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CannotAttackFactionMember"));
        }
    }

    private void handleNonEnemyFire(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        if (MedievalFactions.getInstance().getConfig().getBoolean("warsRequiredForPVP")) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CannotAttackNonWarringPlayer"));
        }
    }
}