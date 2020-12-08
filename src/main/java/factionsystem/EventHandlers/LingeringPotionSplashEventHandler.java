package factionsystem.EventHandlers;

import factionsystem.MedievalFactions;
import factionsystem.Util.Pair;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.LingeringPotionSplashEvent;

public class LingeringPotionSplashEventHandler {

    public void handle(LingeringPotionSplashEvent event) {
        Player thrower = (Player) event.getEntity().getShooter();
        AreaEffectCloud cloud = event.getAreaEffectCloud();

        Pair<Player, AreaEffectCloud> storedCloud  = new Pair<>(thrower, cloud);
        MedievalFactions.getInstance().activeAOEClouds.add(storedCloud);

        // Add scheduled task to remove the cloud from the activeClouds list
        long delay = cloud.getDuration();
        MedievalFactions.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(MedievalFactions.getInstance(), new Runnable() {
            public void run(){
                MedievalFactions.getInstance().activeAOEClouds.remove(storedCloud);
            }
        }, delay);
    }
}
