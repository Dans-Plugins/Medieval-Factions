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
    private final MedievalFactions medievalFactions;

    private FiefsAPI fi_api = null;

    public FiefsIntegrator() {
        this.medievalFactions = MedievalFactions.getMedievalFactions();
        if (isFiefsPresent()) {
            if (this.medievalFactions.isDebugEnabled()) {
                System.out.println("[DEBUG] Fiefs was found successfully!");
            }
            fi_api = new FiefsAPI();
        } else {
            if (this.medievalFactions.isDebugEnabled()) {
                System.out.println("[DEBUG] Fiefs was not found!");
            }
        }
    }

    public boolean isFiefsPresent() {
        return (Bukkit.getServer().getPluginManager().getPlugin("Fiefs") != null);
    }

    public FiefsAPI getAPI() {
        return fi_api;
    }
}