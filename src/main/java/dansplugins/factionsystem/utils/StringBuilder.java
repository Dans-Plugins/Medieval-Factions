package dansplugins.factionsystem.utils;

public class StringBuilder {

    private static StringBuilder instance;

    private StringBuilder() {

    }

    public static StringBuilder getInstance() {
        if (instance == null) {
            instance = new StringBuilder();
        }
        return instance;
    }

    public String createStringFromFirstArgOnwards(String[] args) {
        java.lang.StringBuilder name = new java.lang.StringBuilder();
        for (int i = 1; i < args.length; i++) {
            name.append(args[i]);
            if (!(i == args.length - 1)) {
                name.append(" ");
            }
        }
        return name.toString();
    }

}