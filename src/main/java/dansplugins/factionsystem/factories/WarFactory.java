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
    private static WarFactory instance;

    private WarFactory() {

    }

    public static WarFactory getInstance() {
        if (instance == null) {
            instance = new WarFactory();
        }
        return instance;
    }

    public void createWar(Faction attacker, Faction defender) {
        createWar(attacker, defender, "testreason");
    }

    public void createWar(Faction attacker, Faction defender, String reason) {
        War war = new War(attacker, defender, reason);

        // TODO: inform factions of war here instead of in the declare war command

        PersistentData.getInstance().addWar(war);
    }
}