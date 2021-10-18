package dansplugins.factionsystem.objects.inherited.specification.modifiers;

import dansplugins.factionsystem.objects.inherited.specification.modifiers.Diplomatic;

import java.util.ArrayList;

public interface Lawful {

    // laws
    void addLaw(String newLaw);
    boolean removeLaw(String lawToRemove);
    boolean removeLaw(int i);
    boolean editLaw(int i, String newString);
    int getNumLaws();
    ArrayList<String> getLaws();

}