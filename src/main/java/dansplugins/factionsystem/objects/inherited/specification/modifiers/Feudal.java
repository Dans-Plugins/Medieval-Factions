package dansplugins.factionsystem.objects.inherited.specification.modifiers;

import java.util.ArrayList;

public interface Feudal {

    // type
    boolean isVassal(String name);
    boolean isLiege();

    // liege
    void setLiege(String newLiege);
    String getLiege();
    boolean hasLiege();
    boolean isLiege(String name);

    // vassalage
    void addVassal(String name);
    void removeVassal(String name);
    void clearVassals();
    int getNumVassals();
    ArrayList<String> getVassals();
    String getVassalsSeparatedByCommas();
    void addAttemptedVassalization(String name);
    boolean hasBeenOfferedVassalization(String name);
    void removeAttemptedVassalization(String name);

}
