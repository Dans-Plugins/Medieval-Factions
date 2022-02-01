/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.inherited;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class PlayerRecord {
    protected UUID playerUUID = null;

    public void setPlayerUUID(UUID UUID) {
        playerUUID = UUID;
    }
    public UUID getPlayerUUID() {
        return playerUUID;
    }
}