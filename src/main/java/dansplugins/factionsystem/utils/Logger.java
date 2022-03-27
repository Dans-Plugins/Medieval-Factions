/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils;

import java.util.logging.Level;

import dansplugins.factionsystem.MedievalFactions;

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

    /**
     * Log a debug message if the debug flag is enabled.
     * @param message The message to log.
     */
    public void debug(String message) {
        if (MedievalFactions.getInstance().isDebugEnabled()) {
            MedievalFactions.getInstance().getLogger().log(Level.INFO, "[Medieval Factions DEBUG] " + message);
        }
    }

    /**
     * Log a message to the console.
     * @param message The message to log.
     */
    public void print(String message) {
        MedievalFactions.getInstance().getLogger().log(Level.INFO, "[Medieval Factions] " + message);
    }

    /**
     * Log an error to the console.
     * @param message The message to log.
     */
    public void error(String message) {
        MedievalFactions.getInstance().getLogger().log(Level.SEVERE, "[Medieval Factions ERROR] " + message);
    }
}