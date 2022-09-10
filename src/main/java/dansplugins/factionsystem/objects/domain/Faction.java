/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.CurrenciesIntegrator;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.integrators.FiefsIntegrator;
import dansplugins.factionsystem.objects.helper.FactionFlags;
import dansplugins.factionsystem.objects.inherited.Nation;
import dansplugins.factionsystem.objects.inherited.specification.Feudal;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import preponderous.ponder.misc.abs.Savable;

import java.lang.reflect.Type;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Daniel McCoy Stephenson
 */
public class Faction extends Nation implements Feudal, Savable {
    private final ConfigService configService;
    private final LocaleService localeService;
    private final FiefsIntegrator fiefsIntegrator;
    private final CurrenciesIntegrator currenciesIntegrator;
    private final DynmapIntegrator dynmapIntegrator;
    private final Logger logger;
    private final PersistentData persistentData;
    private final MedievalFactions medievalFactions;
    private final PlayerService playerService;

    private final ArrayList<Gate> gates = new ArrayList<>();
    private final FactionFlags flags;
    private final ArrayList<String> attemptedVassalizations = new ArrayList<>();
    private ArrayList<String> vassals = new ArrayList<>();
    private String liege = "none";
    private String prefix = "none";
    private Location factionHome = null;
    private int bonusPower = 0;
    private boolean autoclaim = false;

    public Faction(String initialName, UUID creator, ConfigService configService, LocaleService localeService, FiefsIntegrator fiefsIntegrator, CurrenciesIntegrator currenciesIntegrator, DynmapIntegrator dynmapIntegrator, Logger logger, PersistentData persistentData, MedievalFactions medievalFactions, PlayerService playerService) {
        this.configService = configService;
        this.localeService = localeService;
        this.fiefsIntegrator = fiefsIntegrator;
        this.currenciesIntegrator = currenciesIntegrator;
        this.dynmapIntegrator = dynmapIntegrator;
        this.logger = logger;
        this.persistentData = persistentData;
        this.medievalFactions = medievalFactions;
        this.playerService = playerService;
        setName(initialName);
        setOwner(creator);
        prefix = initialName;
        flags = new FactionFlags(configService, localeService, fiefsIntegrator, currenciesIntegrator, dynmapIntegrator, logger, this.playerService);
        flags.initializeFlagValues();
    }

    public Faction(ConfigService configService, LocaleService localeService, FiefsIntegrator fiefsIntegrator, CurrenciesIntegrator currenciesIntegrator, DynmapIntegrator dynmapIntegrator, Logger logger, PersistentData persistentData, MedievalFactions medievalFactions, PlayerService playerService, String initialName) {
        this.configService = configService;
        this.localeService = localeService;
        this.fiefsIntegrator = fiefsIntegrator;
        this.currenciesIntegrator = currenciesIntegrator;
        this.dynmapIntegrator = dynmapIntegrator;
        this.logger = logger;
        this.persistentData = persistentData;
        this.medievalFactions = medievalFactions;
        this.playerService = playerService;
        setName(initialName);
        prefix = initialName;
        flags = new FactionFlags(configService, localeService, fiefsIntegrator, currenciesIntegrator, dynmapIntegrator, logger, this.playerService);
        flags.initializeFlagValues();
    }

    public Faction(Map<String, String> data, ConfigService configService, LocaleService localeService, FiefsIntegrator fiefsIntegrator, CurrenciesIntegrator currenciesIntegrator, DynmapIntegrator dynmapIntegrator, Logger logger, PersistentData persistentData, MedievalFactions medievalFactions, PlayerService playerService) {
        this.configService = configService;
        this.localeService = localeService;
        this.fiefsIntegrator = fiefsIntegrator;
        this.currenciesIntegrator = currenciesIntegrator;
        this.dynmapIntegrator = dynmapIntegrator;
        this.logger = logger;
        this.persistentData = persistentData;
        this.medievalFactions = medievalFactions;
        this.playerService = playerService;
        flags = new FactionFlags(configService, localeService, fiefsIntegrator, currenciesIntegrator, dynmapIntegrator, logger, this.playerService);
        this.load(data);
    }

    public int getTotalGates() {
        return gates.size();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String newPrefix) {
        prefix = newPrefix;
    }

    public Location getFactionHome() {
        return factionHome;
    }

    public void setFactionHome(Location l) {
        factionHome = l;
    }

    public FactionFlags getFlags() {
        return flags;
    }

    public int getBonusPower() {
        return bonusPower;
    }

    public void setBonusPower(int i) {
        if (!configService.getBoolean("bonusPowerEnabled") || !((boolean) getFlags().getFlag("acceptBonusPower"))) {
            return;
        }
        bonusPower = i;
    }

    public void toggleAutoClaim() {
        autoclaim = !autoclaim;
    }

    public boolean getAutoClaimStatus() {
        return autoclaim;
    }

    public String getTopLiege() {
        Faction topLiege = persistentData.getFaction(liege);
        String liegeName = liege;
        while (topLiege != null) {
            topLiege = persistentData.getFaction(topLiege.getLiege());
            if (topLiege != null) {
                liegeName = topLiege.getName();
            }
        }
        return liegeName;
    }

    public int calculateCumulativePowerLevelWithoutVassalContribution() {
        int powerLevel = 0;
        for (UUID playerUUID : members) {
            try {
                powerLevel += persistentData.getPlayersPowerRecord(playerUUID).getPower();
            } catch (Exception e) {
                System.out.println(localeService.get("ErrorPlayerPowerRecordForUUIDNotFound"));
            }
        }
        return powerLevel;
    }

    public int calculateCumulativePowerLevelWithVassalContribution() {
        int vassalContribution = 0;
        double percentage = configService.getDouble("vassalContributionPercentageMultiplier");
        for (String factionName : vassals) {
            Faction vassalFaction = persistentData.getFaction(factionName);
            if (vassalFaction != null) {
                vassalContribution += vassalFaction.getCumulativePowerLevel() * percentage;
            }
        }
        return calculateCumulativePowerLevelWithoutVassalContribution() + vassalContribution;
    }

    public int getCumulativePowerLevel() {
        int withoutVassalContribution = calculateCumulativePowerLevelWithoutVassalContribution();
        int withVassalContribution = calculateCumulativePowerLevelWithVassalContribution();

        if (vassals.size() == 0 || (withoutVassalContribution < (getMaximumCumulativePowerLevel() / 2))) {
            return withoutVassalContribution + bonusPower;
        } else {
            return withVassalContribution + bonusPower;
        }
    }

    public int getMaximumCumulativePowerLevel() {     // get max power without vassal contribution
        int maxPower = 0;

        for (UUID playerUUID : members) {
            try {
                maxPower += persistentData.getPlayersPowerRecord(playerUUID).maxPower();
            } catch (Exception e) {
                System.out.println(localeService.get("ErrorPlayerPowerRecordForUUIDNotFound"));
            }
        }
        return maxPower;
    }

    public int calculateMaxOfficers() {
        int officersPerXNumber = configService.getInt("officerPerMemberCount");
        int officersFromConfig = members.size() / officersPerXNumber;
        return 1 + officersFromConfig;
    }

    public List<ClaimedChunk> getClaimedChunks() {
        return persistentData.getChunksClaimedByFaction(getName());
    }

    public boolean isWeakened() {
        return calculateCumulativePowerLevelWithoutVassalContribution() < (getMaximumCumulativePowerLevel() / 2);
    }

    /**
     * Method to automatically handle all data changes when a Faction changes their name.
     *
     * @param oldName of the Faction (dependent).
     * @param newName of the Faction (dependent).
     */
    public void updateData(String oldName, String newName) {
        if (isAlly(oldName)) {
            removeAlly(oldName);
            addAlly(newName);
        }
        if (isEnemy(oldName)) {
            removeEnemy(oldName);
            addEnemy(newName);
        }
        if (isLiege(oldName)) {
            setLiege(newName);
        }
        if (isVassal(oldName)) {
            removeVassal(oldName);
            addVassal(newName);
        }
    }

    public void addGate(Gate gate) {
        gates.add(gate);
    }

    public void removeGate(Gate gate) {
        gates.remove(gate);
    }

    public ArrayList<Gate> getGates() {
        return gates;
    }

    public boolean hasGateTrigger(Block block) {
        for (Gate g : gates) {
            if (g.getTrigger().getX() == block.getX() && g.getTrigger().getY() == block.getY() && g.getTrigger().getZ() == block.getZ() &&
                    g.getTrigger().getWorld().equalsIgnoreCase(block.getWorld().getName())) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Gate> getGatesForTrigger(Block block) {
        ArrayList<Gate> gateList = new ArrayList<>();
        for (Gate g : gates) {
            if (g.getTrigger().getX() == block.getX() && g.getTrigger().getY() == block.getY() && g.getTrigger().getZ() == block.getZ() &&
                    g.getTrigger().getWorld().equalsIgnoreCase(block.getWorld().getName())) {
                gateList.add(g);
            }
        }
        return gateList;
    }

    public boolean isVassal(String faction) {
        return (containsIgnoreCase(vassals, faction));
    }

    public boolean isLiege() {
        return vassals.size() > 0;
    }

    public String getLiege() {
        return liege;
    }

    public void setLiege(String newLiege) {
        liege = newLiege;
    }

    public boolean hasLiege() {
        return !liege.equalsIgnoreCase("none");
    }

    public boolean isLiege(String faction) {
        return liege.equalsIgnoreCase(faction);
    }

    public void addVassal(String name) {
        if (!containsIgnoreCase(vassals, name)) {
            vassals.add(name);
        }
    }

    public void removeVassal(String name) {
        removeIfContainsIgnoreCase(vassals, name);
    }

    public boolean addOfficer(UUID newOfficer) {
        if (officers.size() < calculateMaxOfficers() && !officers.contains(newOfficer)) {
            officers.add(newOfficer);
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "Faction{" +
                "members=" + members +
                ", enemyFactions=" + enemyFactions +
                ", officers=" + officers +
                ", allyFactions=" + allyFactions +
                ", laws=" + laws +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", owner=" + owner +
                ", cumulativePowerLevel=" + getCumulativePowerLevel() +
                ", liege=" + liege +
                '}';
    }


    public String getVassalsSeparatedByCommas() {
        StringBuilder toReturn = new StringBuilder();
        for (int i = 0; i < vassals.size(); i++) {
            toReturn.append(vassals.get(i));
            if (i != vassals.size() - 1) {
                toReturn.append(", ");
            }
        }
        return toReturn.toString();
    }

    public void addAttemptedVassalization(String factionName) {
        if (!containsIgnoreCase(attemptedVassalizations, factionName)) {
            attemptedVassalizations.add(factionName);
        }
    }

    public boolean hasBeenOfferedVassalization(String factionName) {
        return containsIgnoreCase(attemptedVassalizations, factionName);
    }

    public void removeAttemptedVassalization(String factionName) {
        removeIfContainsIgnoreCase(attemptedVassalizations, factionName);
    }

    private boolean containsIgnoreCase(ArrayList<String> list, String str) {
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    private void removeIfContainsIgnoreCase(ArrayList<String> list, String str) {
        String toRemove = "";
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                toRemove = string;
                break;
            }
        }
        list.remove(toRemove);
    }

    public void clearVassals() {
        vassals.clear();
    }

    public int getNumVassals() {
        return vassals.size();
    }

    public ArrayList<String> getVassals() {
        return vassals;
    }

    @Override
    public Map<String, String> save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, String> saveMap = new HashMap<>();

        saveMap.put("members", gson.toJson(members));
        saveMap.put("enemyFactions", gson.toJson(enemyFactions));
        saveMap.put("officers", gson.toJson(officers));
        saveMap.put("allyFactions", gson.toJson(allyFactions));
        saveMap.put("laws", gson.toJson(laws));
        saveMap.put("name", gson.toJson(name));
        saveMap.put("vassals", gson.toJson(vassals));
        saveMap.put("description", gson.toJson(description));
        saveMap.put("owner", gson.toJson(owner));
        saveMap.put("location", gson.toJson(saveLocation(gson)));
        saveMap.put("liege", gson.toJson(liege));
        saveMap.put("prefix", gson.toJson(prefix));
        saveMap.put("bonusPower", gson.toJson(bonusPower));

        ArrayList<String> gateList = new ArrayList<>();
        for (Gate gate : gates) {
            Map<String, String> map = gate.save();
            gateList.add(gson.toJson(map));
        }
        saveMap.put("factionGates", gson.toJson(gateList));

        saveMap.put("integerFlagValues", gson.toJson(flags.getIntegerValues()));
        saveMap.put("booleanFlagValues", gson.toJson(flags.getBooleanValues()));
        saveMap.put("doubleFlagValues", gson.toJson(flags.getDoubleValues()));
        saveMap.put("stringFlagValues", gson.toJson(flags.getStringValues()));

        return saveMap;
    }

    private Map<String, String> saveLocation(Gson gson) {
        Map<String, String> saveMap = new HashMap<>();

        if (factionHome != null && factionHome.getWorld() != null) {
            saveMap.put("worldName", gson.toJson(factionHome.getWorld().getName()));
            saveMap.put("x", gson.toJson(factionHome.getX()));
            saveMap.put("y", gson.toJson(factionHome.getY()));
            saveMap.put("z", gson.toJson(factionHome.getZ()));
        }

        return saveMap;
    }

    @Override
    public void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Type arrayListTypeString = new TypeToken<ArrayList<String>>() {
        }.getType();
        Type arrayListTypeUUID = new TypeToken<ArrayList<UUID>>() {
        }.getType();
        Type stringToIntegerMapType = new TypeToken<HashMap<String, Integer>>() {
        }.getType();
        Type stringToBooleanMapType = new TypeToken<HashMap<String, Boolean>>() {
        }.getType();
        Type stringToDoubleMapType = new TypeToken<HashMap<String, Double>>() {
        }.getType();
        Type stringToStringMapType = new TypeToken<HashMap<String, String>>() {
        }.getType();

        members = gson.fromJson(data.get("members"), arrayListTypeUUID);
        enemyFactions = gson.fromJson(data.get("enemyFactions"), arrayListTypeString);
        officers = gson.fromJson(data.get("officers"), arrayListTypeUUID);
        allyFactions = gson.fromJson(data.get("allyFactions"), arrayListTypeString);
        laws = gson.fromJson(data.get("laws"), arrayListTypeString);
        name = gson.fromJson(data.get("name"), String.class);
        description = gson.fromJson(data.get("description"), String.class);
        owner = UUID.fromString(gson.fromJson(data.get("owner"), String.class));
        factionHome = loadLocation(gson.fromJson(data.get("location"), stringToStringMapType), gson);
        liege = gson.fromJson(data.getOrDefault("liege", "none"), String.class);
        vassals = gson.fromJson(data.getOrDefault("vassals", "[]"), arrayListTypeString);
        prefix = loadPrefixOrDefault(gson, data, getName());
        bonusPower = gson.fromJson(data.getOrDefault("bonusPower", "0"), Integer.TYPE);

        ArrayList<String> gateList = gson.fromJson(data.get("factionGates"), arrayListTypeString);
        if (gateList != null) {
            for (String item : gateList) {
                Gate gate = new Gate(medievalFactions, configService);
                gate.load(item);
                gates.add(gate);
            }
        } else {
            System.out.println(localeService.get("MissingFactionGatesJSONCollection"));
        }

        flags.setIntegerValues(gson.fromJson(data.getOrDefault("integerFlagValues", "[]"), stringToIntegerMapType));
        flags.setBooleanValues(gson.fromJson(data.getOrDefault("booleanFlagValues", "[]"), stringToBooleanMapType));
        flags.setDoubleValues(gson.fromJson(data.getOrDefault("doubleFlagValues", "[]"), stringToDoubleMapType));
        flags.setStringValues(gson.fromJson(data.getOrDefault("stringFlagValues", "[]"), stringToStringMapType));

        flags.loadMissingFlagsIfNecessary();

        if (!configService.getBoolean("bonusPowerEnabled") || !((boolean) getFlags().getFlag("acceptBonusPower"))) {
            bonusPower = 0;
        }
    }

    private String loadPrefixOrDefault(Gson gson, Map<String, String> data, String def) {
        try {
            return gson.fromJson(data.getOrDefault("prefix", def), String.class);
        } catch (Exception e) {
            return def;
        }
    }

    private Location loadLocation(HashMap<String, String> data, Gson gson) {
        if (data.size() != 0) {
            World world = getServer().createWorld(new WorldCreator(gson.fromJson(data.get("worldName"), String.class)));
            double x = gson.fromJson(data.get("x"), Double.TYPE);
            double y = gson.fromJson(data.get("y"), Double.TYPE);
            double z = gson.fromJson(data.get("z"), Double.TYPE);
            return new Location(world, x, y, z);
        }
        return null;
    }
}