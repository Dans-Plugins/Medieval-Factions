package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatHandler implements Listener {

    @EventHandler()
    public void handle(AsyncPlayerChatEvent event) {
        if (EphemeralData.getInstance().getPlayersInFactionChat().contains(event.getPlayer().getUniqueId())) {
            Faction playersFaction = PersistentData.getInstance().getPlayersFaction(event.getPlayer().getUniqueId());
            if (playersFaction != null) {
                String message = event.getMessage();
                Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ChatColor.GOLD + message);
                event.setCancelled(true);
            }
        }
    }

}
