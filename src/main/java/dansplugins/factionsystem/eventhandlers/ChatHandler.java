/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.utils.ColorChecker;
import dansplugins.factionsystem.utils.Messenger;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;

/**
 * @author Daniel McCoy Stephenson
 */
public class ChatHandler implements Listener {
    private String factionChatColor;
    private String prefixColor;
    private String prefix;
    private String message;

    @EventHandler()
    public void handle(AsyncPlayerChatEvent event) {
        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(event.getPlayer().getUniqueId());
        if (playersFaction == null) {
            return;
        }
        initializeValues(playersFaction, event);
        if (MedievalFactions.getInstance().getConfig().getBoolean("playersChatWithPrefixes")) {
            addPrefix(event, prefixColor, prefix);
        }
        if (EphemeralData.getInstance().isPlayerInFactionChat(event.getPlayer())) {
            sendMessage(playersFaction, prefixColor, prefix, event, factionChatColor, message);
            event.setCancelled(true);
        }
    }

    private void initializeValues(Faction playersFaction, AsyncPlayerChatEvent event) {
        factionChatColor = MedievalFactions.getInstance().getConfig().getString("factionChatColor");
        prefixColor = (String) playersFaction.getFlags().getFlag("prefixColor");
        prefix = playersFaction.getPrefix();
        message = event.getMessage();
    }

    private void sendMessage(Faction playersFaction, String prefixColor, String prefix, AsyncPlayerChatEvent event, String factionChatColor, String message) {
        if (MedievalFactions.getInstance().getConfig().getBoolean("chatSharedInVassalageTrees")) {
            sendMessageToVassalageTree(playersFaction, prefixColor, prefix, event, factionChatColor, message);
        }
        else {
            sendMessageToFaction(playersFaction, prefix, prefixColor, event, factionChatColor, message);
        }
    }

    private void addPrefix(AsyncPlayerChatEvent event, String prefixColor, String prefix) {
        event.setFormat(ColorChecker.getInstance().getColorByName(prefixColor) + "" + "[" + prefix + "] " + ChatColor.WHITE + " %s: %s");
    }

    private void sendMessageToVassalageTree(Faction playersFaction, String prefixColor, String prefix, AsyncPlayerChatEvent event, String factionChatColor, String message) {
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

    private void sendMessageToFaction(Faction playersFaction, String prefix, String prefixColor, AsyncPlayerChatEvent event, String factionChatColor, String message) {
        if (MedievalFactions.getInstance().getConfig().getBoolean("showPrefixesInFactionChat")) {
            Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ColorChecker.getInstance().getColorByName(prefixColor) + "" + "[" + prefix + "] " + "" + ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ColorChecker.getInstance().getColorByName(factionChatColor) + message);
        }
        else {
            Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.WHITE + "" + event.getPlayer().getName() + ": " + ColorChecker.getInstance().getColorByName(factionChatColor) + message);
        }
    }
}