package dansplugins.factionsystem.objects.specification.generic;

import java.util.ArrayList;

public interface Feudal {

    // type
    boolean isVassal(String faction);
    boolean isLiege();

    // liege
    void setLiege(String newLiege);
    String getLiege();
    boolean hasLiege();
    boolean isLiege(String faction);

    // vassalage
    void clearVassals();
    int getNumVassals();
    ArrayList<String> getVassals();
    String getVassalsSeparatedByCommas();
    void addAttemptedVassalization(String factionName);
    boolean hasBeenOfferedVassalization(String factionName);
    void removeAttemptedVassalization(String factionName);

}
