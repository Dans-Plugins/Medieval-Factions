package dansplugins.factionsystem.objects.inherited;

import dansplugins.factionsystem.objects.inherited.specification.IPlayerRecord;

import java.util.UUID;

public class PlayerRecord implements IPlayerRecord {

    private UUID playerUUID = UUID.randomUUID();

    public PlayerRecord(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    @Override
    public void setPlayerUUID(UUID UUID) {
        playerUUID = UUID;
    }

    @Override
    public UUID getPlayerUUID() {
        return playerUUID;
    }

}