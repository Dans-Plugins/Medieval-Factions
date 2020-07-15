package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import static factionsystem.Subsystems.UtilitySubsystem.getPlayersFaction;
import static factionsystem.Subsystems.UtilitySubsystem.sendAllPlayersInFactionMessage;

public class AsyncPlayerChatEventHandler {

    Main main = null;

    public AsyncPlayerChatEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(AsyncPlayerChatEvent event) {
        if (main.playersInFactionChat.contains(event.getPlayer().getName())) {
            Faction playersFaction = getPlayersFaction(event.getPlayer().getName(), main.factions);
            if (playersFaction != null) {
                String message = String.format("%s: %s", event.getPlayer().getName(), event.getMessage());
                sendAllPlayersInFactionMessage(playersFaction, ChatColor.GOLD + "" + message);
                event.setCancelled(true);
            }
        }
    }

}
