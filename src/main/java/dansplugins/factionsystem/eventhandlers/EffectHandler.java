package dansplugins.factionsystem.eventhandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dansplugins.factionsystem.MedievalFactions;
import org.bukkit.Bukkit;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.utils.RelationChecker;
import preponderous.ponder.misc.Pair;

public class EffectHandler implements Listener {
    private final EphemeralData ephemeralData;
    private final MedievalFactions medievalFactions;
    private final RelationChecker relationChecker;

    private final List<PotionEffectType> BAD_POTION_EFFECTS = Arrays.asList(
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
    private final List<PotionType> BAD_POTION_TYPES = new ArrayList<>();

    public EffectHandler(EphemeralData ephemeralData, MedievalFactions medievalFactions, RelationChecker relationChecker) {
        this.ephemeralData = ephemeralData;
        this.medievalFactions = medievalFactions;
        this.relationChecker = relationChecker;
        initializeBadPotionTypes();
    }

    @EventHandler()
    public void handle(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        if (!potionTypeBad(cloud.getBasePotionData().getType())) {
            return;
        }
        Player attacker = getPlayerInStoredCloudPair(cloud);
        List<Player> alliedVictims = getAlliedVictims(event, attacker);
        event.getAffectedEntities().removeAll(alliedVictims);
    }

    @EventHandler()
    public void handle(LingeringPotionSplashEvent event) {
        Player thrower = (Player) event.getEntity().getShooter();
        AreaEffectCloud cloud = event.getAreaEffectCloud();
        Pair<Player, AreaEffectCloud> storedCloud = new Pair<>(thrower, cloud);
        ephemeralData.getActiveAOEClouds().add(storedCloud);
        addScheduledTaskToRemoveCloudFromEphemeralData(cloud, storedCloud);
    }

    @EventHandler()
    public void handle(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        if (!wasShooterAPlayer(potion)) {
            return;
        }
        Player attacker = (Player) potion.getShooter();

        for (PotionEffect effect : potion.getEffects()) {
            if (!potionEffectBad(effect.getType())) {
                continue;
            }
            removePotionIntensityIfAnyVictimIsAnAlliedPlayer(event, attacker);
        }
    }

    private void initializeBadPotionTypes() {
        BAD_POTION_TYPES.add(PotionType.INSTANT_DAMAGE);
        BAD_POTION_TYPES.add(PotionType.POISON);
        BAD_POTION_TYPES.add(PotionType.SLOWNESS);
        BAD_POTION_TYPES.add(PotionType.WEAKNESS);

        if (!Bukkit.getVersion().contains("1.12.2")) {
            BAD_POTION_TYPES.add(PotionType.TURTLE_MASTER);
        }
    }

    private boolean potionTypeBad(PotionType type) {
        return BAD_POTION_TYPES.contains(type);
    }


    private boolean potionEffectBad(PotionEffectType effect) {
        return BAD_POTION_EFFECTS.contains(effect);
    }

    private void addScheduledTaskToRemoveCloudFromEphemeralData(AreaEffectCloud cloud, Pair<Player, AreaEffectCloud> storedCloudPair) {
        long delay = cloud.getDuration();
        medievalFactions.getServer().getScheduler().scheduleSyncDelayedTask(medievalFactions, () -> ephemeralData.getActiveAOEClouds().remove(storedCloudPair), delay);
    }

    private void removePotionIntensityIfAnyVictimIsAnAlliedPlayer(PotionSplashEvent event, Player attacker) {
        for (LivingEntity victimEntity : event.getAffectedEntities()) {
            if (!(victimEntity instanceof Player)) {
                continue;
            }
            Player victim = (Player) victimEntity;
            if (attacker == victim) {
                continue;
            }
            if (arePlayersInFactionAndNotAtWar(attacker, victim)) {
                event.setIntensity(victimEntity, 0);
            }
        }
    }

    private boolean arePlayersInFactionAndNotAtWar(Player attacker, Player victim) {
        return relationChecker.arePlayersInAFaction(attacker, victim) && (relationChecker.arePlayersFactionsNotEnemies(attacker, victim) || relationChecker.arePlayersInSameFaction(attacker, victim));
    }

    private boolean wasShooterAPlayer(ThrownPotion potion) {
        return potion.getShooter() instanceof Player;
    }

    private Player getPlayerInStoredCloudPair(AreaEffectCloud cloud) {
        Pair<Player, AreaEffectCloud> storedCloudPair = getCloudPairStoredInEphemeralData(cloud);
        if (storedCloudPair == null) {
            return null;
        }
        return storedCloudPair.getLeft();
    }

    private List<Player> getAlliedVictims(AreaEffectCloudApplyEvent event, Player attacker) {
        List<Player> alliedVictims = new ArrayList<>();
        for (Entity potentialVictimEntity : event.getAffectedEntities()) {
            if (!(potentialVictimEntity instanceof Player)) {
                continue;
            }

            Player potentialVictim = (Player) potentialVictimEntity;

            if (attacker == potentialVictim) {
                continue;
            }

            if (bothAreInFactionAndNotAtWar(attacker, potentialVictim)) {
                alliedVictims.add(potentialVictim);
            }
        }
        return alliedVictims;
    }

    private boolean bothAreInFactionAndNotAtWar(Player attacker, Player potentialVictim) {
        return relationChecker.arePlayersInAFaction(attacker, potentialVictim)
                && (relationChecker.arePlayersFactionsNotEnemies(attacker, potentialVictim) || relationChecker.arePlayersInSameFaction(attacker, potentialVictim));
    }

    private Pair<Player, AreaEffectCloud> getCloudPairStoredInEphemeralData(AreaEffectCloud cloud) {
        for (Pair<Player, AreaEffectCloud> storedCloudPair : ephemeralData.getActiveAOEClouds()) {
            if (storedCloudPair.getRight() == cloud) {
                return storedCloudPair;
            }
        }
        return null;
    }
}