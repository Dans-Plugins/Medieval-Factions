/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class LocalActionBarService {
    private static LocalActionBarService instance;
    private final Map<Player, TextComponent> playerActionBarMessages = new HashMap<>();

    private LocalActionBarService(Plugin plugin) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                this::sendPlayerActionBarMessages, 5, 20);
    }

    public static LocalActionBarService getInstance(Plugin plugin) {
        if (instance == null) {
            instance = new LocalActionBarService(plugin);
        }
        return instance;
    }

    public void showPersistentActionBarMessage(Player player, TextComponent message) {
        playerActionBarMessages.put(player, message);
        this.sendPlayerActionBarMessage(player, message);
    }

    public void clearPlayerActionBar(Player player) {
        playerActionBarMessages.remove(player);
        this.sendPlayerActionBarMessage(player, new TextComponent(""));
    }

    private void sendPlayerActionBarMessages () {
        playerActionBarMessages.forEach(this::sendPlayerActionBarMessage);
    }

    private void sendPlayerActionBarMessage (Player player, TextComponent message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
    }
}
