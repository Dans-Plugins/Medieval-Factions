package dansplugins.factionsystem.objects.inherited;

import dansplugins.factionsystem.objects.inherited.specification.INation;

import java.util.ArrayList;

public class Nation extends Group implements INation {

    @Override
    public void addAlly(String name) {

    }

    @Override
    public void removeAlly(String name) {

    }

    @Override
    public boolean isAlly(String name) {
        return false;
    }

    @Override
    public ArrayList<String> getAllies() {
        return null;
    }

    @Override
    public String getAlliesSeparatedByCommas() {
        return null;
    }

    @Override
    public void requestAlly(String name) {

    }

    @Override
    public boolean isRequestedAlly(String name) {
        return false;
    }

    @Override
    public void removeAllianceRequest(String name) {

    }

    @Override
    public void addEnemy(String name) {

    }

    @Override
    public void removeEnemy(String name) {

    }

    @Override
    public boolean isEnemy(String name) {
        return false;
    }

    @Override
    public ArrayList<String> getEnemyFactions() {
        return null;
    }

    @Override
    public String getEnemiesSeparatedByCommas() {
        return null;
    }

    @Override
    public void requestTruce(String name) {

    }

    @Override
    public boolean isTruceRequested(String name) {
        return false;
    }

    @Override
    public void removeRequestedTruce(String name) {

    }

    @Override
    public void addLaw(String newLaw) {

    }

    @Override
    public boolean removeLaw(String lawToRemove) {
        return false;
    }

    @Override
    public boolean removeLaw(int i) {
        return false;
    }

    @Override
    public boolean editLaw(int i, String newString) {
        return false;
    }

    @Override
    public int getNumLaws() {
        return 0;
    }

    @Override
    public ArrayList<String> getLaws() {
        return null;
    }

}