package dansplugins.factionsystem.objects.specification.generic;

import java.util.ArrayList;

public interface Diplomatic {

    // allies
    void addAlly(String factionName);
    void removeAlly(String factionName);
    boolean isAlly(String factionName);
    ArrayList<String> getAllies();
    String getAlliesSeparatedByCommas();

    // requests
    void requestAlly(String factionName);
    boolean isRequestedAlly(String factionName);
    void removeAllianceRequest(String factionName);

    // enemies
    void addEnemy(String factionName);
    void removeEnemy(String factionName);
    boolean isEnemy(String factionName);
    ArrayList<String> getEnemyFactions();
    String getEnemiesSeparatedByCommas();

    // truces
    void requestTruce(String factionName);
    boolean isTruceRequested(String factionName);
    void removeRequestedTruce(String factionName);

}