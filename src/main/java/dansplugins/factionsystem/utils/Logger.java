package dansplugins.factionsystem.utils;

import dansplugins.factionsystem.MedievalFactions;

import java.util.logging.Level;

/**
 * @author Daniel McCoy Stephenson
 */
public class Logger {
    private static Logger instance;

    private Logger() {

    }

    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public void log(String message) {
        if (MedievalFactions.getInstance().isDebugEnabled()) {
            MedievalFactions.getInstance().getLogger().log(Level.INFO, "[Medieval Factions] " + message);
        }
    }
}