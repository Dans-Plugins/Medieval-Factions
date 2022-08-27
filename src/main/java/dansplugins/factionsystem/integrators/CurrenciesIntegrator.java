/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.integrators;

import org.bukkit.Bukkit;

/**
 * @author Daniel McCoy Stephenson
 */
public class CurrenciesIntegrator {

    public boolean isCurrenciesNotPresent() {
        return Bukkit.getServer().getPluginManager().getPlugin("Currencies") == null;
    }
}