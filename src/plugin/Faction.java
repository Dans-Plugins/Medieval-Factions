package plugin;

public class Faction {
    private String name;

    // constructor
    Faction(String initialName) {
        changeName(initialName);
    }

    void changeName(String newName) {
        name = newName;
    }


}
