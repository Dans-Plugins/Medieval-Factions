package plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Faction {
    private String name;

    // constructor
    Faction(String initialName) {
        changeName(initialName);
    }

    void changeName(String newName) {
        name = newName;
    }

    void save() {
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

        } catch (IOException e) {
            System.out.println("An error occured saving the faction named " + name);
            e.printStackTrace();
        }


    }


}
