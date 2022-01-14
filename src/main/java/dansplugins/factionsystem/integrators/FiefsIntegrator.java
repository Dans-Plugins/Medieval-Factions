/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.integrators;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.fiefs.externalapi.FiefsAPI;
import org.bukkit.Bukkit;

/**
 * @author Daniel McCoy Stephenson
 */
public class FiefsIntegrator {
    private static FiefsIntegrator instance;

    private FiefsAPI fi_api = null;

    private FiefsIntegrator() {
        if (isFiefsPresent()) {
            if (MedievalFactions.getInstance().isDebugEnabled()) { System.out.println("[DEBUG] Fiefs was found successfully!"); }
            fi_api = new FiefsAPI();
        }
        else {
            if (MedievalFactions.getInstance().isDebugEnabled()) { System.out.println("[DEBUG] Fiefs was not found!"); }
        }
    }

    public static FiefsIntegrator getInstance() {
        if (instance == null) {
            instance = new FiefsIntegrator();
        }
        return instance;
    }

    public boolean isFiefsPresent() {
        return (Bukkit.getServer().getPluginManager().getPlugin("Fiefs") != null);
    }

    public FiefsAPI getAPI() {
        return fi_api;
    }
}