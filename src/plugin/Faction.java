package plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Faction {
    private String name = "defaultName";
    private String description = "defaultDescription";
    private ArrayList<String> members;

    // constructor
    Faction(String initialName) {
        changeName(initialName);
    }

    void changeName(String newName) {
        name = newName;
    }

    void changeDescription(String newDesc) {
        description = newDesc;
    }

    void addMember(String playerName) {
        members.add(playerName);
    }

    void removeMember(String playerName) {
        members.remove(playerName);
    }

    boolean save() {
        try {
            File saveFile = new File(name + ".txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for faction " + name + " created.");
            } else {
                System.out.println("Save file for faction " + name + " already exists. Altering.");
            }

            FileWriter saveWriter = new FileWriter(name + ".txt");

            // actual saving takes place here
            saveWriter.write(name + "\n");
            saveWriter.write(description);

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
            File loadFile = new File(filename);
            Scanner loadReader = new Scanner(loadFile);
            int counter = 0;

            // actual loading
            if (loadReader.hasNextLine()) {
                changeName(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                changeDescription(loadReader.nextLine());
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
