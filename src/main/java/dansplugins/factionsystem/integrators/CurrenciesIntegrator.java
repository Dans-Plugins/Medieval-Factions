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
    private static CurrenciesIntegrator instance;

    private CurrenciesIntegrator() {

    }

    public static CurrenciesIntegrator getInstance() {
        if (instance == null) {
            instance = new CurrenciesIntegrator();
        }
        return instance;
    }

    public boolean isCurrenciesPresent() {
        return (Bukkit.getServer().getPluginManager().getPlugin("Currencies") != null);
    }
}