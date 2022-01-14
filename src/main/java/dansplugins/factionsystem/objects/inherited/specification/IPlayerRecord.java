/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.inherited.specification;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public interface IPlayerRecord {
    void setPlayerUUID(UUID UUID);
    UUID getPlayerUUID();
}