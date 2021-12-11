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
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DamageEffectsAndDeathHandler implements Listener {

    public DamageEffectsAndDeathHandler() {
        initializeBadPotionTypes();
    }

    @EventHandler()
    public void handle(EntityDamageByEntityEvent event) {
   	
        // this method disallows PVP between members of the same faction and between factions who are not at war
        // PVP is allowed between factionless players, players who belong to a faction and the factionless, and players whose factions are at war.
        Logger.getInstance().log("EntityDamageByIntity" + event.toString());

        // if this was between two players melee
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();
        	// if these players are actively duelling then we don't want to handle friendly fire.
            Duel duel = EphemeralData.getInstance().getDuel(attacker, victim);
            if (duel == null)
            {
            	handleIfFriendlyFire(event, attacker, victim);	
            }
            else if (duel.getStatus().equals(Duel.DuelState.DUELLING))
            {
            	if (victim.getHealth() - event.getFinalDamage() <= 0)
            	{
        			duel.setLoser(victim);
            		duel.finishDuel(false);
            		EphemeralData.getInstance().getDuelingPlayers().remove(this);
            		event.setCancelled(true);
            	}
            }
            else
            {
            	handleIfFriendlyFire(event, attacker, victim);
            }
        }
        else if (event.getDamager() instanceof Projectile && event.getEntity() instanceof Player) {
            Projectile arrow = (Projectile) event.getDamager();
            ProjectileSource source = arrow.getShooter();

            if (source instanceof Player){
                Player attacker = (Player) source;
                Player victim = (Player) event.getEntity();

            	// if these players are actively duelling then we don't want to handle friendly fire.
                Duel duel = EphemeralData.getInstance().getDuel(attacker, victim);
                if (duel == null)
                {
                	handleIfFriendlyFire(event, attacker, victim);	
                }
                else if (duel.getStatus().equals(Duel.DuelState.DUELLING))
                {
                	if (victim.getHealth() - event.getFinalDamage() <= 0)
                	{
            			duel.setLoser(victim);
                		duel.finishDuel(false);
                		EphemeralData.getInstance().getDuelingPlayers().remove(this);
                		event.setCancelled(true);
                	}
                }
                else
                {
                	handleIfFriendlyFire(event, attacker, victim);
                }
            }
        }

        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            Location location = null;

            if (event.getEntity() instanceof ArmorStand) {
                ArmorStand armorStand = (ArmorStand) event.getEntity();

                if (!(event.getDamager() instanceof Player)) {
                    return;
                }

                // get chunk that armor stand is in
                location = armorStand.getLocation();
            }
            else if (event.getEntity() instanceof ItemFrame) {
                Logger.getInstance().log("ItemFrame interaction captured in EntityDamageByEntityEvent!");
                ItemFrame itemFrame = (ItemFrame) event.getEntity();

                if (!(event.getDamager() instanceof Player)) {
                    return;
                }

                // get chunk that armor stand is in
                location = itemFrame.getLocation();
            }

            if (location != null) {
                Chunk chunk = location.getChunk();
                ClaimedChunk claimedChunk = LocalChunkService.getInstance().getClaimedChunk(chunk);

                // if chunk is not claimed, return
                if (claimedChunk == null) {
                    return;
                }

                String holderFactionName = claimedChunk.getHolder();

                Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                if (playersFaction == null) {
                    event.setCancelled(true);
                    return;
                }

                String playersFactionName = playersFaction.getName();

                // if holder is not the same as player's faction
                if (!holderFactionName.equalsIgnoreCase(playersFactionName)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleIfFriendlyFire(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        if (!arePlayersInAFaction(attacker, victim)){
            // Factionless can fight anyone.
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

        // if attacker and victim are both in a faction
        if (arePlayersInAFaction(player1, player2)){
            // if attacker and victim are part of the same faction
            return attackersFactionIndex == victimsFactionIndex;
        } else {
            return false;
        }
    }

    private boolean arePlayersInAFaction(Player player1, Player player2) {
        return PersistentData.getInstance().isInFaction(player1.getUniqueId()) && PersistentData.getInstance().isInFaction(player2.getUniqueId());
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
            for (PowerRecord record : PersistentData.getInstance().getPlayerPowerRecords()) {
                if (record.getPlayerUUID().equals(player.getUniqueId())) {
                    record.decreasePowerByTenPercent();
                    if (PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel() > 0) {
                        player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("AlertPowerLevelDecreased"));
                    }
                }
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

        // if player is in faction
        if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {

            // if player is in land claimed by their faction
            double[] playerCoords = new double[2];
            playerCoords[0] = player.getLocation().getChunk().getX();
            playerCoords[1] = player.getLocation().getChunk().getZ();

            // check if land is claimed
            if (LocalChunkService.getInstance().isClaimed(player.getLocation().getChunk(), PersistentData.getInstance().getClaimedChunks()))
            {
                ClaimedChunk chunk = LocalChunkService.getInstance().getClaimedChunk(player.getLocation().getChunk());
                // if holder is player's faction
                if (chunk.getHolder().equalsIgnoreCase(PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName()) && PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getAutoClaimStatus() == false) {

                    // if not killed by another player
                    if (!(player.getKiller() instanceof Player)) {

                        // player keeps items
                        // event.setKeepInventory(true); // TODO: fix this duplicating items

                    }

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
