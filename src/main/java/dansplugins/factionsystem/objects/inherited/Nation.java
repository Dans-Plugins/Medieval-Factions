package dansplugins.factionsystem.objects.inherited;

import dansplugins.factionsystem.objects.inherited.specification.INation;

import java.util.ArrayList;

public class Nation extends Group implements INation {

    protected ArrayList<String> allyFactions = new ArrayList<>();
    protected ArrayList<String> attemptedAlliances = new ArrayList<>();
    protected ArrayList<String> enemyFactions = new ArrayList<>();
    protected ArrayList<String> attemptedTruces = new ArrayList<>();

    protected ArrayList<String> laws = new ArrayList<>();

    @Override
    public void addAlly(String factionName) {
        if (!containsIgnoreCase(allyFactions, factionName)) {
            allyFactions.add(factionName);
        }
    }

    @Override
    public void removeAlly(String factionName) {
        removeIfContainsIgnoreCase(allyFactions, factionName);
    }

    @Override
    public boolean isAlly(String factionName) {
        return containsIgnoreCase(allyFactions, factionName);
    }

    @Override
    public ArrayList<String> getAllies() {
        return allyFactions;
    }

    @Override
    public String getAlliesSeparatedByCommas() {
        String allies = "";
        for (int i = 0; i < allyFactions.size(); i++) {
            allies = allies + allyFactions.get(i);
            if (i != allyFactions.size() - 1) {
                allies = allies + ", ";
            }
        }
        return allies;
    }

    @Override
    public void requestAlly(String factionName) {
        if (!containsIgnoreCase(attemptedAlliances, factionName)) {
            attemptedAlliances.add(factionName);
        }
    }

    @Override
    public boolean isRequestedAlly(String factionName) {
        return containsIgnoreCase(attemptedAlliances, factionName);
    }

    @Override
    public void removeAllianceRequest(String factionName) {
        attemptedAlliances.remove(factionName);
    }

    @Override
    public void addEnemy(String factionName) {
        if (!containsIgnoreCase(enemyFactions, factionName)) {
            enemyFactions.add(factionName);
        }
    }

    @Override
    public void removeEnemy(String factionName) {
        removeIfContainsIgnoreCase(enemyFactions, factionName);
    }

    @Override
    public boolean isEnemy(String factionName) {
        return containsIgnoreCase(enemyFactions, factionName);
    }

    @Override
    public ArrayList<String> getEnemyFactions() {
        return enemyFactions;
    }

    @Override
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

    @Override
    public void requestTruce(String factionName) {
        if (!containsIgnoreCase(attemptedTruces, factionName)) {
            attemptedTruces.add(factionName);
        }
    }

    @Override
    public boolean isTruceRequested(String factionName) {
        return containsIgnoreCase(attemptedTruces, factionName);
    }

    @Override
    public void removeRequestedTruce(String factionName) {
        removeIfContainsIgnoreCase(attemptedTruces, factionName);
    }

    @Override
    public void addLaw(String newLaw) {
        laws.add(newLaw);
    }

    @Override
    public boolean removeLaw(String lawToRemove) {
        if (containsIgnoreCase(laws, lawToRemove)) {
            laws.remove(lawToRemove);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeLaw(int i) {
        if (laws.size() > i) {
            laws.remove(i);
            return true;
        }
        return false;
    }

    @Override
    public boolean editLaw(int i, String newString) {
        if (laws.size() > i) {
            laws.set(i, newString);
            return true;
        }
        return false;
    }

    @Override
    public int getNumLaws() {
        return laws.size();
    }

    @Override
    public ArrayList<String> getLaws() {
        return laws;
    }

    // helper methods ---------------

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

}