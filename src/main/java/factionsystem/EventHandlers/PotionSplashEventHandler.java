package factionsystem.EventHandlers;

import factionsystem.Main;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;


public class PotionSplashEventHandler {

    Main main = null;

    private static List<PotionEffectType> BAD_POTION_EFFECTS = Arrays.asList(
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

    public PotionSplashEventHandler(Main plugin) {
        main = plugin;
    }

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
                        if (main.utilities.arePlayersInAFaction(attacker, victim) &&
                                (main.utilities.arePlayersFactionsNotEnemies(attacker, victim) ||
                                        main.utilities.arePlayersInSameFaction(attacker, victim))) {
                            event.setIntensity(victimEntity, 0);
                        }
                    }
                }
            }
        }
    }

    private boolean potionEffectBad(PotionEffectType effect) {
        return BAD_POTION_EFFECTS.contains(effect);
    }
}