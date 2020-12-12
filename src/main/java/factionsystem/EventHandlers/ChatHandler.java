package factionsystem.EventHandlers;

import factionsystem.Data.EphemeralData;
import factionsystem.Objects.Faction;
import factionsystem.Data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import static factionsystem.Subsystems.UtilitySubsystem.getPlayersFaction;
import static factionsystem.Subsystems.UtilitySubsystem.sendAllPlayersInFactionMessage;

public class ChatHandler implements Listener {

    @EventHandler()
    public void handle(AsyncPlayerChatEvent event) {
        if (EphemeralData.getInstance().getPlayersInFactionChat().contains(event.getPlayer().getUniqueId())) {
            Faction playersFaction = getPlayersFaction(event.getPlayer().getUniqueId(), PersistentData.getInstance().getFactions());
            if (playersFaction != null) {
                String message = event.getMessage();
                sendAllPlayersInFactionMessage(playersFaction, ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ChatColor.GOLD + message);
                event.setCancelled(true);
            }
        }
    }

}
