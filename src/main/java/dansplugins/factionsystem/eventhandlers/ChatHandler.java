package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
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

            String prefixColor = (String) playersFaction.getFlags().getFlag("prefixColor");
            String prefix = playersFaction.getPrefix();

            if (MedievalFactions.getInstance().getConfig().getBoolean("playersChatWithPrefixes")) {
                // add prefix
                event.setFormat(ColorChecker.getInstance().getColorByName(prefixColor) + "" + "[" + prefix + "]" + "" + ChatColor.WHITE + "" + " <%s> %s");
            }

            // check for faction chat
            if (EphemeralData.getInstance().isPlayerInFactionChat(event.getPlayer())) {
                String message = event.getMessage();
                if (MedievalFactions.getInstance().getConfig().getBoolean("chatSharedInVassalageTrees")) {
                    ArrayList<Faction> factionsInVassalageTree = PersistentData.getInstance().getFactionsInVassalageTree(playersFaction);
                    for (Faction faction : factionsInVassalageTree) {
                        if (MedievalFactions.getInstance().getConfig().getBoolean("showPrefixesInFactionChat")) {
                            Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ColorChecker.getInstance().getColorByName(prefixColor) + "" + "[" + prefix + "] " + "" + ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ColorChecker.getInstance().getColorByName(factionChatColor) + message);
                        }
                        else {
                            Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ColorChecker.getInstance().getColorByName(factionChatColor) + message);
                        }
                    }
                }
                else {
                    if (MedievalFactions.getInstance().getConfig().getBoolean("showPrefixesInFactionChat")) {
                        Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ColorChecker.getInstance().getColorByName(prefixColor) + "" + "[" + prefix + "] " + "" + ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ColorChecker.getInstance().getColorByName(factionChatColor) + message);
                    }
                    else {
                        Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ColorChecker.getInstance().getColorByName(factionChatColor) + message);
                    }
                }

                event.setCancelled(true);
            }

        }
    }

}
