package plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Faction {
    private String name = "";

    // constructor
    Faction(String initialName) {
        changeName(initialName);
    }

    void changeName(String newName) {
        name = newName;
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
            saveWriter.write(name);

            saveWriter.close();

            System.out.println("Successfuly saved faction " + name + ".");
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
            changeName(loadReader.nextLine());

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
