package dansplugins.factionsystem.objects.inherited.specification;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public interface IPlayerRecord {
    void setPlayerUUID(UUID UUID);
    UUID getPlayerUUID();
}