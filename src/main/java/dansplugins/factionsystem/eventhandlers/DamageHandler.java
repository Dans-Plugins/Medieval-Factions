package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Duel;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.services.LocalChunkService;
import dansplugins.factionsystem.services.LocalConfigService;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockShearEntityEvent;
import org.bukkit.event.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel McCoy Stephenson
 */
public class DamageHandler implements Listener {

    public DamageHandler() {
        initializeBadPotionTypes();
    }

    /**
     * This method disallows PVP between members of the same faction and between factions who are not at war
     * PVP is allowed between factionless players, players who belong to a faction and the factionless, and players whose factions are at war.
     */
    @EventHandler()
    public void handle(EntityDamageByEntityEvent event) {
        Player attacker = getAttacker(event);
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

        Player victim = getVictim(event);
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

    @EventHandler()
    public void handle(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();

        if (potionTypeBad(cloud.getBasePotionData().getType())){
            // Search to see if cloud is in the stored list in MedievalFactions.getInstance()
            for (Pair<Player, AreaEffectCloud> storedCloudPair : EphemeralData.getInstance().getActiveAOEClouds()){
                if (storedCloudPair.getRight() == cloud){
                    //Check player is not allied with effected entities if any allied remove entity from list.
                    Player attacker = storedCloudPair.getLeft();

                    List<Player> alliedVictims = new ArrayList<>();
                    for (Entity potentialVictimEntity : event.getAffectedEntities()){
                        if (potentialVictimEntity instanceof Player){
                            Player potentialVictim = (Player) potentialVictimEntity;

                            if (attacker == potentialVictim){
                                continue;
                            }

                            // If both are in a faction and not at war.
                            if (arePlayersInAFaction(attacker, potentialVictim) &&
                                    (arePlayersFactionsNotEnemies(attacker, potentialVictim) ||
                                            arePlayersInSameFaction(attacker, potentialVictim))) {
                                alliedVictims.add(potentialVictim);
                            }
                        }
                    }

                    // Remove attacker's allies from the list
                    event.getAffectedEntities().removeAll(alliedVictims);
                }
            }
        }
    }

    @EventHandler()
    public void handle(LingeringPotionSplashEvent event) {
        Player thrower = (Player) event.getEntity().getShooter();
        AreaEffectCloud cloud = event.getAreaEffectCloud();

        Pair<Player, AreaEffectCloud> storedCloud  = new Pair<>(thrower, cloud);
        EphemeralData.getInstance().getActiveAOEClouds().add(storedCloud);

        // Add scheduled task to remove the cloud from the activeClouds list
        long delay = cloud.getDuration();
        MedievalFactions.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(MedievalFactions.getInstance(), new Runnable() {
            public void run(){
                EphemeralData.getInstance().getActiveAOEClouds().remove(storedCloud);
            }
        }, delay);
    }

    @EventHandler()
    public void handle(PlayerDeathEvent event) {
        event.getEntity();
        Player player = event.getEntity();

        if (LocalConfigService.getInstance().getBoolean("playersLosePowerOnDeath")) {
            // decrease dying player's power
            PowerRecord playersPowerRecord = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());
            int powerLost = playersPowerRecord.decreasePowerByTenPercent();
            if (powerLost != 0) {
                player.sendMessage(ChatColor.RED + "You lost " + powerLost + " power."); // TODO: add locale message
            }
        }

        // if player's cause of death was another player killing them
        if (player.getKiller() != null) {
            Player killer = player.getKiller();

            PowerRecord record = PersistentData.getInstance().getPlayersPowerRecord(killer.getUniqueId());
            if (record != null) {
                if (record.increasePowerByTenPercent()){
                    killer.sendMessage(ChatColor.GREEN + LocalLocaleService.getInstance().getText("PowerLevelHasIncreased"));
                }
            }
        }
    }

    @EventHandler()
    public void handle(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();

        // If shooter was not player ignore.
        if (!(potion.getShooter() instanceof Player)) return;
        Player attacker = (Player) potion.getShooter();

        for(PotionEffect effect : potion.getEffects()) {
            // Is potion effect bad?
            if (potionEffectBad(effect.getType())) {

                // If any victim is a allied player remove potion intensity
                for (LivingEntity victimEntity : event.getAffectedEntities()) {
                    if (victimEntity instanceof Player){
                        Player victim = (Player) victimEntity;

                        // People can still hurt themselves, let's encourage skill!
                        if (attacker == victim){
                            continue;
                        }

                        // If players are in faction and not at war
                        if (arePlayersInAFaction(attacker, victim) &&
                                (arePlayersFactionsNotEnemies(attacker, victim) ||
                                        arePlayersInSameFaction(attacker, victim))) {
                            event.setIntensity(victimEntity, 0);
                        }
                    }
                }
            }
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
        if (entity instanceof ArmorStand
                || entity instanceof ItemFrame) {
            return true;
        }
        return false;

    }

    private Player getVictim(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            return (Player) event.getEntity();
        }
        else {
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
        if (!arePlayersInAFaction(attacker, victim) || attacker.getUniqueId().equals(victim.getUniqueId())){
            // Factionless can fight anyone.
            // Don't block self damage.
            return;
        }
        else if (arePlayersInSameFaction(attacker, victim)) {
            Faction faction = PersistentData.getInstance().getPlayersFaction(attacker.getUniqueId());
            boolean friendlyFireAllowed = (boolean) faction.getFlags().getFlag("allowfriendlyFire");
            if (!friendlyFireAllowed) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CannotAttackFactionMember"));
            }
        }
        else if (arePlayersFactionsNotEnemies(attacker, victim)) { // if attacker's faction and victim's faction are not at war
            if (MedievalFactions.getInstance().getConfig().getBoolean("warsRequiredForPVP")) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CannotAttackNonWarringPlayer"));
            }
        }
    }

    private boolean arePlayersFactionsNotEnemies(Player player1, Player player2) {
        Pair<Integer, Integer> factionIndices = getFactionIndices(player1, player2);
        int attackersFactionIndex = factionIndices.getLeft();
        int victimsFactionIndex = factionIndices.getRight();

        return !(PersistentData.getInstance().getFactions().get(attackersFactionIndex).isEnemy(PersistentData.getInstance().getFactions().get(victimsFactionIndex).getName())) &&
                !(PersistentData.getInstance().getFactions().get(victimsFactionIndex).isEnemy(PersistentData.getInstance().getFactions().get(attackersFactionIndex).getName()));
    }

    private Pair<Integer, Integer> getFactionIndices(Player player1, Player player2){
        int attackersFactionIndex = 0;
        int victimsFactionIndex = 0;

        for (int i = 0; i < PersistentData.getInstance().getFactions().size(); i++) {
            if (PersistentData.getInstance().getFactions().get(i).isMember(player1.getUniqueId())) {
                attackersFactionIndex = i;
            }
            if (PersistentData.getInstance().getFactions().get(i).isMember(player2.getUniqueId())) {
                victimsFactionIndex = i;
            }
        }

        return new Pair<>(attackersFactionIndex, victimsFactionIndex);
    }

    private boolean arePlayersInSameFaction(Player player1, Player player2) {
        Pair<Integer, Integer> factionIndices = getFactionIndices(player1, player2);
        int attackersFactionIndex = factionIndices.getLeft();
        int victimsFactionIndex = factionIndices.getRight();
        return arePlayersInAFaction(player1, player2) && attackersFactionIndex == victimsFactionIndex;
    }

    private boolean arePlayersInAFaction(Player player1, Player player2) {
        return PersistentData.getInstance().isInFaction(player1.getUniqueId()) && PersistentData.getInstance().isInFaction(player2.getUniqueId());
    }

    private List<PotionType> BAD_POTION_TYPES = new ArrayList<>();

    private void initializeBadPotionTypes() {
        BAD_POTION_TYPES.add(PotionType.INSTANT_DAMAGE);
        BAD_POTION_TYPES.add(PotionType.POISON);
        BAD_POTION_TYPES.add(PotionType.SLOWNESS);
        BAD_POTION_TYPES.add(PotionType.WEAKNESS);

        if (!Bukkit.getVersion().contains("1.12.2")) {
            BAD_POTION_TYPES.add(PotionType.TURTLE_MASTER);
        }
    }

    private boolean potionTypeBad(PotionType type){
        return BAD_POTION_TYPES.contains(type);
    }

    // Placed lower as it goes with the method below it.
    private  List<PotionEffectType> BAD_POTION_EFFECTS = Arrays.asList(
            PotionEffectType.BLINDNESS,
            PotionEffectType.CONFUSION,
            PotionEffectType.HARM,
            PotionEffectType.HUNGER,
            PotionEffectType.POISON,
            PotionEffectType.SLOW,
            PotionEffectType.SLOW_DIGGING,
            PotionEffectType.UNLUCK,
            PotionEffectType.WEAKNESS,
            PotionEffectType.WITHER
    );

    private boolean potionEffectBad(PotionEffectType effect) {
        return BAD_POTION_EFFECTS.contains(effect);
    }
}