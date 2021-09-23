package dansplugins.factionsystem.utils;

import dansplugins.factionsystem.MedievalFactions;

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
            System.out.println("[Medieval Factions] " + message);
        }
    }

}
