package factionsystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Faction {
    private String name = "defaultName";
    private String description = "defaultDescription";
    private ArrayList<String> members = new ArrayList<>();
    private String owner = "defaultOwner";
    private ArrayList<String> invited = new ArrayList<>();
    private ArrayList<String> enemyFactions = new ArrayList<>();
    private boolean autoclaim = false;
    private ArrayList<String> officers = new ArrayList<>();

    // player constructor
    public Faction(String initialName, String creator) {
        setName(initialName);
        setOwner(creator);
    }

    // server constructor
    Faction(String initialName) {
        setName(initialName);
    }

    public void addOfficer(String newOfficer) {
        officers.add(newOfficer);
    }

    public void removeOfficer(String officerToRemove) {
        officers.removeIf(officer -> officer.equalsIgnoreCase(officerToRemove));
    }

    public boolean isOfficer(String playerName) {
        for (String officer : officers) {
            if (officer.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    ArrayList<String> getMemberArrayList() {
        return members;
    }

    public void toggleAutoClaim() {
        autoclaim = !autoclaim;
    }

    public boolean getAutoClaimStatus() {
        return autoclaim;
    }

    public void addEnemy(String factionName) {
        enemyFactions.add(factionName);
    }

    public void removeEnemy(String factionName) {
        enemyFactions.remove(factionName);
    }

    public boolean isEnemy(String factionName) {
        for (String faction : enemyFactions) {
            if (faction.equalsIgnoreCase(factionName)) {
                return true;
            }
        }
        return false;
    }

    public String getEnemiesSeparatedByCommas() {
        String enemies = "";
        for (int i = 0; i < enemyFactions.size(); i++) {
            enemies = enemies + enemyFactions.get(i);
            if (i != enemyFactions.size() - 1) {
                enemies = enemies + ", ";
            }
        }
        return enemies;
    }

    public void invite(String playerName) {
        invited.add(playerName);
    }

    public void uninvite(String playerName) {
        invited.remove(playerName);
    }

    public boolean isInvited(String playerName) {
        for (String player : invited) {
            if (player.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    ArrayList<String> getMemberList() {
        ArrayList<String> membersCopy = members;
        return membersCopy;
    }

    public int getPopulation() {
        return members.size();
    }

    public void setOwner(String playerName) {
        owner = playerName;
    }

    public boolean isOwner(String playerName) {
        if (playerName.equalsIgnoreCase(owner)) {
            return true;
        }
        else {
            return false;
        }
    }

    public String getOwner() {
        return owner;
    }

    void setName(String newName) {
        name = newName;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String newDesc) {
        description = newDesc;
    }

    String getDescription() {
        return description;
    }

    public void addMember(String playerName) {
        members.add(playerName);
    }

    public void removeMember(String playerName) {
        members.remove(playerName);
    }

    public boolean isMember(String playerName) {
        boolean membership = false;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).equalsIgnoreCase(playerName)) {
                membership = true;
            }
        }
        return membership;
    }

    boolean save(ArrayList<Faction> factions) {
        try {
            File saveFolder = new File("./plugins/medievalfactions/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/medievalfactions/" + name + ".txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for faction " + name + " created.");
            } else {
                System.out.println("Save file for faction " + name + " already exists. Altering.");
            }

            FileWriter saveWriter = new FileWriter("./plugins/medievalfactions/" + name + ".txt");

            // actual saving takes place here
            saveWriter.write(name + "\n");
            saveWriter.write(owner + "\n");
            saveWriter.write(description + "\n");

            for (int i = 0; i < members.size(); i++) {
                saveWriter.write(members.get(i) + "\n");
            }

            saveWriter.write("0" + "\n");

            for (int i = 0; i < enemyFactions.size(); i++) {

                // if enemy faction exists, save it
                for (Faction faction : factions) {
                    if (faction.getName().equalsIgnoreCase(enemyFactions.get(i))) {
                        saveWriter.write(enemyFactions.get(i) + "\n");
                    }
                }

            }

            saveWriter.write("0" + "\n");

            for (String officer : officers) {
                saveWriter.write(officer + "\n");
            }

            saveWriter.close();

            System.out.println("Successfully saved faction " + name + ".");
            return true;

        } catch (IOException e) {
            System.out.println("An error occurred saving the faction named " + name);
            e.printStackTrace();
            return false;
        }

    }

    boolean load(String filename) {
        try {
            File loadFile = new File("./plugins/medievalfactions/" + filename);
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            if (loadReader.hasNextLine()) {
                setName(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                setOwner(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                setDescription(loadReader.nextLine());
            }

            while (loadReader.hasNextLine()) {
                String temp = loadReader.nextLine();

                if (temp.equalsIgnoreCase("0")) {
                    break;
                }

                members.add(temp);
            }

            while (loadReader.hasNextLine()) {
                String temp = loadReader.nextLine();

                if (temp.equalsIgnoreCase("0")) {
                    break;
                }

                officers.add(temp);
            }

            loadReader.close();
            System.out.println("Faction " + name + " successfully loaded.");
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred loading the file " + filename + ".");
            e.printStackTrace();
            return false;
        }
    }

}