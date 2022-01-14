/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.externalapi;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.UUID;


/**
 * @author Daniel McCoy Stephenson
 */
public interface IMedievalFactionsAPI {

    // accessors
    String getAPIVersion();
    String getVersion();
    MF_Faction getFaction(String factionName);
    MF_Faction getFaction(Player player);
    MF_Faction getFaction(UUID playerUUID);
    boolean isPlayerInFactionChat(Player player);
    boolean isPrefixesFeatureEnabled();
    boolean isChunkClaimed(Chunk chunk);
    int getPower(Player player);
    int getPower(UUID playerUUID);

    // mutators
    void forcePlayerToLeaveFactionChat(UUID uuid);
    void increasePower(Player player, int amount);
    void decreasePower(Player player, int amount);
}
