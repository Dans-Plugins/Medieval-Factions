package dansplugins.factionsystem.objects.specification.generic;

import java.util.ArrayList;

public interface Nation extends Group, Diplomatic {

    // laws
    void addLaw(String newLaw);
    boolean removeLaw(String lawToRemove);
    boolean removeLaw(int i);
    boolean editLaw(int i, String newString);
    int getNumLaws();
    ArrayList<String> getLaws();

}