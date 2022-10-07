/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils;

import dansplugins.factionsystem.MedievalFactions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * @author Daniel McCoy Stephenson
 */
public class Logger {
    private final MedievalFactions medievalFactions;

    public Logger(MedievalFactions medievalFactions) {
        this.medievalFactions = medievalFactions;
    }

    /**
     * Log a debug message if the debug flag is enabled.
     *
     * @param message The message to log.
     */
    public void debug(String message) {
        if (medievalFactions.isDebugEnabled()) {
            medievalFactions.getLogger().log(Level.INFO, "[Medieval Factions DEBUG] " + message);
            logToFile("[DEBUG] " + message);
        }
    }

    /**
     * Log a message to the console.
     *
     * @param message The message to log.
     */
    public void print(String message) {
        medievalFactions.getLogger().log(Level.INFO, "[Medieval Factions] " + message);
        logToFile("[INFO] " + message);
    }

    /**
     * Log an error to the console.
     *
     * @param message The message to log.
     */
    public void error(String message) {
        medievalFactions.getLogger().log(Level.SEVERE, "[Medieval Factions ERROR] " + message);
        logToFile("[ERROR] " + message);
    }

    private void logToFile(String message) {
        // add time to message
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = LocalDateTime.now().format(formatter);
        String dateMessage = "[" + formattedDateTime + "] " + message;

        // append to file
        File file = new File("plugins/MedievalFactions/logs.txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(dateMessage);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            error("An error occurred while logging to file.");
        }
    }
}