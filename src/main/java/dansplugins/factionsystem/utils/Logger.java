/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils;

import dansplugins.factionsystem.MedievalFactions;

import java.util.logging.Level;

/**
 * @author Daniel McCoy Stephenson
 */
public class Logger {
    private final MedievalFactions medievalFactions;

    public Logger() {
        this.medievalFactions = MedievalFactions.getMedievalFactions();
    }

    /**
     * Log a debug message if the debug flag is enabled.
     *
     * @param message The message to log.
     */
    public void debug(String message) {
        if (medievalFactions.isDebugEnabled()) {
            medievalFactions.getLogger().log(Level.INFO, "[Medieval Factions DEBUG] " + message);
        }
    }

    /**
     * Log a message to the console.
     *
     * @param message The message to log.
     */
    public void print(String message) {
        medievalFactions.getLogger().log(Level.INFO, "[Medieval Factions] " + message);
    }

    /**
     * Log an error to the console.
     *
     * @param message The message to log.
     */
    public void error(String message) {
        medievalFactions.getLogger().log(Level.SEVERE, "[Medieval Factions ERROR] " + message);
    }
}