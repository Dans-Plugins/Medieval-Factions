package dansplugins.factionsystem.utils;

import java.util.ArrayList;

public class ArgumentParser {

    private static ArgumentParser instance;

    private ArgumentParser() {

    }

    public static ArgumentParser getInstance() {
        if (instance == null) {
            instance = new ArgumentParser();
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

    public ArrayList<String> getArgumentsInsideSingleQuotes(String[] args) {
        ArrayList<String> toReturn = new ArrayList<>();

        String argumentString = String.join(" ", args);

        int index = 0;
        while (true) {
            int start = findIndexOfFirstSingleQuote(index, argumentString);
            if (start == -1) {
                break;
            }
            int end = findIndexOfFirstSingleQuote(start + 1, argumentString);

            if (end == -1) {
                break;
            }

            toReturn.add(argumentString.substring(start + 1, end));
//            System.out.println("DEBUG: argument '" + toReturn.get(toReturn.size() - 1) + "' found!");
            index = end + 1;
        }

        return toReturn;
    }

    private int findIndexOfFirstSingleQuote(int startingIndex, String argumentString) {

        for (int i = startingIndex; i < argumentString.length(); i++) {

            if (argumentString.charAt(i) == '\'') {
//                System.out.println("DEBUG: first index of a single quote character in '" + argumentString + "' is " + i);
                return i;
            }

        }

        return -1;
    }

}