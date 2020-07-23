package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Util.Pair;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.LingeringPotionSplashEvent;

public class LingeringPotionSplashEventHandler {

    Main main = null;

    public LingeringPotionSplashEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(LingeringPotionSplashEvent event) {
        Player thrower = (Player) event.getEntity().getShooter();
        AreaEffectCloud cloud = event.getAreaEffectCloud();

        Pair<Player, AreaEffectCloud> storedCloud  = new Pair<>(thrower, cloud);
        main.activeAOEClouds.add(storedCloud);

        // Add scheduled task to remove the cloud from the activeClouds list
        long delay = cloud.getDuration();
        main.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable() {
            public void run(){
                main.activeAOEClouds.remove(storedCloud);
            }
        }, delay);
    }
}
