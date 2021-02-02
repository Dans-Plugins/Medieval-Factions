package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ColorChecker;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;

public class ChatHandler implements Listener {

    @EventHandler()
    public void handle(AsyncPlayerChatEvent event) {
        String factionChatColor = MedievalFactions.getInstance().getConfig().getString("factionChatColor");

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(event.getPlayer().getUniqueId());

        if (playersFaction != null) {

            if (MedievalFactions.getInstance().getConfig().getBoolean("playersChatWithPrefixes")) {
                // add prefix
                String prefix = playersFaction.getPrefix();
                event.setFormat("[" + prefix + "] <%s> %s");
            }

            // check for faction chat
            if (EphemeralData.getInstance().getPlayersInFactionChat().contains(event.getPlayer().getUniqueId())) {
                String message = event.getMessage();
                if (MedievalFactions.getInstance().getConfig().getBoolean("chatSharedInVassalageTrees")) {
                    ArrayList<Faction> factionsInVassalageTree = PersistentData.getInstance().getFactionsInVassalageTree(playersFaction);
                    for (Faction faction : factionsInVassalageTree) {
                        Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ColorChecker.getInstance().getColorByName(factionChatColor) + message);
                    }
                }
                else {
                    Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ColorChecker.getInstance().getColorByName(factionChatColor) + message);
                }


                event.setCancelled(true);
            }

        }
    }

}
