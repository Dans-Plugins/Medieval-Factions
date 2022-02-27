/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.domain;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.objects.inherited.PlayerRecord;
import preponderous.ponder.misc.abs.Savable;

/**
 * @author Daniel McCoy Stephenson
 */
public class ActivityRecord extends PlayerRecord implements Savable {
    private int logins = 0;
    private int powerLost = 0;
    private ZonedDateTime lastLogout = ZonedDateTime.now();

    public ActivityRecord(UUID uuid, int logins) {
        playerUUID = uuid;
        this.logins = logins;
        this.powerLost = 0;
    }

    public ActivityRecord(Map<String, String> data) {
        this.load(data);
    }

    public int getPowerLost() {
        return powerLost;
    }

    public void setPowerLost(int power) {
        powerLost = power;
    }

    public void incrementPowerLost() {
        powerLost += MedievalFactions.getInstance().getConfig().getInt("powerDecreaseAmount");
    }

    public ZonedDateTime getLastLogout() {
        return lastLogout;
    }

    public void setLastLogout(ZonedDateTime date) {
        lastLogout = date;
    }

    public void incrementLogins() {
        logins++;
    }

    public int getLogins() {
        return logins;
    }

    public int getMinutesSinceLastLogout() {
        if (lastLogout == null) {
            return 0;
        }
        ZonedDateTime now = ZonedDateTime.now();
        Duration duration = Duration.between(lastLogout, now);
        double totalSeconds = duration.getSeconds();
        return (int) totalSeconds / 60;
    }

    /**
     * Method to obtain the current session length in dd:hh:mm:ss
     * <p>
     * If days are not found, hh:mm:ss are returned.
     * </p>
     *
     * @return formatted String dd:hh:mm:ss
     * @author Callum
     */
    public String getActiveSessionLength() {
        if (lastLogout == null) {
            return "00:00:00";
        }
        final ZonedDateTime now = ZonedDateTime.now();
        final Duration duration = Duration.between(lastLogout, now);
        long totalSeconds = duration.getSeconds();
        final long days = TimeUnit.SECONDS.toDays(totalSeconds);
        totalSeconds -= TimeUnit.DAYS.toSeconds(days); // Remove Days from Total.
        final long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        totalSeconds -= TimeUnit.HOURS.toSeconds(hours); // Remove Hours from Total.
        final long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds);
        totalSeconds -= TimeUnit.MINUTES.toSeconds(minutes); // Remove Minutes from Total.
        final long seconds = totalSeconds; // Last one is just the remainder.
        final String d = pad(days), h = pad(hours), m = pad(minutes), s = pad(seconds);
        return (d.equalsIgnoreCase("00") ? "" : d + ":") + h + ":" + m + ":" + s;
    }

    public String getTimeSinceLastLogout() {
        if (lastLogout != null) {
            ZonedDateTime now = ZonedDateTime.now();
            Duration duration = Duration.between(lastLogout, now);
            double totalSeconds = duration.getSeconds();
            int minutes = (int) totalSeconds / 60;
            int hours = minutes / 60;
            int days = hours / 24;
            int hoursSince = hours - (days * 24);
            return days + " days and " + hoursSince + " hours";
        } else {
            return null;
        }
    }

    @Override
    public Map<String, String> save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Map<String, String> saveMap = new HashMap<>();
        saveMap.put("playerUUID", gson.toJson(playerUUID.toString()));
        saveMap.put("logins", gson.toJson(logins));
        saveMap.put("lastLogout", gson.toJson(lastLogout.toString()));
        saveMap.put("powerLost", gson.toJson(powerLost));

        return saveMap;
    }

    @Override
    public void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        playerUUID = UUID.fromString(gson.fromJson(data.get("playerUUID"), String.class));
        logins = gson.fromJson(data.get("logins"), Integer.TYPE);
        lastLogout = ZonedDateTime.parse(gson.fromJson(data.get("lastLogout"), String.class), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        powerLost = gson.fromJson(data.get("powerLost"), Integer.TYPE);
    }

    /**
     * Method to pad a value with a zero to its left.
     *
     * @param value to pad
     * @return 00 or 0(0-9) or 10-(very big numbers)
     * @author Callum
     */
    private String pad(Number value) {
        String tmp = String.valueOf(value);
        return tmp.length() == 0 ? ("00") : (tmp.length() == 1 ? ("0" + value) : (tmp));
    }
}