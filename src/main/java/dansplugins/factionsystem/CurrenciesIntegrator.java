package dansplugins.factionsystem;

import org.bukkit.Bukkit;

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
