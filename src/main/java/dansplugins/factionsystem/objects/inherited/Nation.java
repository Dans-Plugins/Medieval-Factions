package dansplugins.factionsystem.objects.inherited;

import dansplugins.factionsystem.objects.inherited.specification.INation;

import java.util.ArrayList;

public class Nation extends Group implements INation {

    @Override
    public void addAlly(String name) {
        // TODO: implement
    }

    @Override
    public void removeAlly(String name) {
        // TODO: implement
    }

    @Override
    public boolean isAlly(String name) {
        // TODO: implement
        return false;
    }

    @Override
    public ArrayList<String> getAllies() {
        // TODO: implement
        return null;
    }

    @Override
    public String getAlliesSeparatedByCommas() {
        // TODO: implement
        return null;
    }

    @Override
    public void requestAlly(String name) {
        // TODO: implement
    }

    @Override
    public boolean isRequestedAlly(String name) {
        // TODO: implement
        return false;
    }

    @Override
    public void removeAllianceRequest(String name) {
        // TODO: implement
    }

    @Override
    public void addEnemy(String name) {
        // TODO: implement
    }

    @Override
    public void removeEnemy(String name) {
        // TODO: implement
    }

    @Override
    public boolean isEnemy(String name) {
        // TODO: implement
        return false;
    }

    @Override
    public ArrayList<String> getEnemyFactions() {
        // TODO: implement
        return null;
    }

    @Override
    public String getEnemiesSeparatedByCommas() {
        // TODO: implement
        return null;
    }

    @Override
    public void requestTruce(String name) {
        // TODO: implement
    }

    @Override
    public boolean isTruceRequested(String name) {
        // TODO: implement
        return false;
    }

    @Override
    public void removeRequestedTruce(String name) {
        // TODO: implement
    }

    @Override
    public void addLaw(String newLaw) {
        // TODO: implement
    }

    @Override
    public boolean removeLaw(String lawToRemove) {
        // TODO: implement
        return false;
    }

    @Override
    public boolean removeLaw(int i) {
        // TODO: implement
        return false;
    }

    @Override
    public boolean editLaw(int i, String newString) {
        // TODO: implement
        return false;
    }

    @Override
    public int getNumLaws() {
        // TODO: implement
        return 0;
    }

    @Override
    public ArrayList<String> getLaws() {
        // TODO: implement
        return null;
    }

}