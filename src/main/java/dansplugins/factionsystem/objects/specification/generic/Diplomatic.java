package dansplugins.factionsystem.objects.specification.generic;

import java.util.ArrayList;

public interface Diplomatic {

    // allies
    void addAlly(String name);
    void removeAlly(String name);
    boolean isAlly(String name);
    ArrayList<String> getAllies();
    String getAlliesSeparatedByCommas();

    // requests
    void requestAlly(String name);
    boolean isRequestedAlly(String name);
    void removeAllianceRequest(String name);

    // enemies
    void addEnemy(String name);
    void removeEnemy(String name);
    boolean isEnemy(String name);
    ArrayList<String> getEnemyFactions();
    String getEnemiesSeparatedByCommas();

    // truces
    void requestTruce(String name);
    boolean isTruceRequested(String name);
    void removeRequestedTruce(String name);

}