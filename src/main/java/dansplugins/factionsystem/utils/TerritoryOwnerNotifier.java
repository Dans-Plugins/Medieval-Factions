/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalActionBarService;
import dansplugins.factionsystem.services.LocalLocaleService;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 */
public class TerritoryOwnerNotifier {
    private static TerritoryOwnerNotifier instance;

    private TerritoryOwnerNotifier() {

    }

    public static TerritoryOwnerNotifier getInstance() {
        if (instance == null) {
            instance = new TerritoryOwnerNotifier();
        }
        return instance;
    }

    public void sendPlayerTerritoryAlert(Player player, Faction holder) {
        ChatColor territoryAlertColor = getColor(holder);
        String title = getTitle(holder);
        setActionBar(holder, player, territoryAlertColor, title);
        sendAlert(player, territoryAlertColor, title);
    }

    private String getTitle(Faction holder) {
        if (holder != null) {
            return holder.getName();
        }
        else {
            return LocalLocaleService.getInstance().getText("Wilderness");
        }
    }

    private ChatColor getColor(Faction holder) {
        String territoryAlertColorString;
        if (holder != null) {
            territoryAlertColorString = (String) holder.getFlags().getFlag("territoryAlertColor");
        } else {
            territoryAlertColorString = MedievalFactions.getInstance().getConfig().getString("territoryAlertColor");
        }
        return ColorChecker.getInstance().getColorByName(territoryAlertColorString);
    }

    private void setActionBar(Faction holder, Player player, ChatColor territoryAlertColor, String title) {
        if (MedievalFactions.getInstance().getConfig().getBoolean("territoryIndicatorActionbar")) {
            LocalActionBarService actionBar = LocalActionBarService.getInstance(MedievalFactions.getInstance());

            if (holder == null) {
                actionBar.clearPlayerActionBar(player);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(territoryAlertColor + title));
            }
            else {
                actionBar.showPersistentActionBarMessage(player, new TextComponent(territoryAlertColor + title));
            }
        }
    }

    private void sendAlert(Player player, ChatColor territoryAlertColor, String title) {
        if (MedievalFactions.getInstance().getConfig().getBoolean("territoryAlertPopUp")) {
            int fadeIn = 10;
            int stay = 70;
            int fadeOut = 20;
            player.sendTitle(territoryAlertColor + title, null, fadeIn, stay, fadeOut);
        }
        else {
            player.sendMessage(territoryAlertColor + title);
        }
    }
}