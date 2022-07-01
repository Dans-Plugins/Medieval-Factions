/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.factories;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.War;

/**
 * @author Daniel McCoy Stephenson
 */
public class WarFactory {
    private final PersistentData persistentData;

    public WarFactory(PersistentData persistentData) {
        this.persistentData = persistentData;
    }

    public void createWar(Faction attacker, Faction defender) {
        createWar(attacker, defender, "testreason");
    }

    public void createWar(Faction attacker, Faction defender, String reason) {
        War war = new War(attacker, defender, reason);

        // TODO: inform factions of war here instead of in the declare war command

        persistentData.addWar(war);
    }
}