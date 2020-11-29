package factionsystem.EventHandlers;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import static factionsystem.Subsystems.UtilitySubsystem.getPlayersFaction;
import static factionsystem.Subsystems.UtilitySubsystem.sendAllPlayersInFactionMessage;

public class AsyncPlayerChatEventHandler {

    MedievalFactions main = null;

    public AsyncPlayerChatEventHandler(MedievalFactions plugin) {
        main = plugin;
    }

    public void handle(AsyncPlayerChatEvent event) {
        if (main.playersInFactionChat.contains(event.getPlayer().getUniqueId())) {
            Faction playersFaction = getPlayersFaction(event.getPlayer().getUniqueId(), main.factions);
            if (playersFaction != null) {
                String message = event.getMessage();
                sendAllPlayersInFactionMessage(playersFaction, ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ChatColor.GOLD + message);
                event.setCancelled(true);
            }
        }
    }

}
