package dansplugins.factionsystem;

public class LocaleManager {

    private static LocaleManager instance;

    private LocaleManager() {

    }

    public static LocaleManager getInstance() {
        if (instance == null) {
            instance = new LocaleManager();
        }
        return instance;
    }



}
