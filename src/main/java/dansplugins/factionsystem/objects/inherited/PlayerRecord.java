package dansplugins.factionsystem.objects.inherited;

import dansplugins.factionsystem.objects.inherited.specification.IPlayerRecord;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class PlayerRecord implements IPlayerRecord {
    protected UUID playerUUID = null;

    @Override
    public void setPlayerUUID(UUID UUID) {
        playerUUID = UUID;
    }

    @Override
    public UUID getPlayerUUID() {
        return playerUUID;
    }
}