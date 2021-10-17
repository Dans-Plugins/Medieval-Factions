package dansplugins.factionsystem.objects.domain.specification;

import org.bukkit.entity.Player;

public interface IDuel {
    enum DuelState {INVITED, DUELLING, WINNER, TIED}

    DuelState getStatus();
    void setStatus(DuelState state);
    boolean isChallenged(Player player);
    Player getChallenged();
    boolean isChallenger(Player player);
    Player getChallenger();
    double getChallengerHealth();
    double getChallengedHealth();
    boolean hasPlayer(Player player);
    void resetHealth();
    void setWinner(Player player);
    Player getWinner();
    void setLoser(Player player);
    Player getLoser();
    void acceptDuel();
    void finishDuel(boolean tied);
}
