package dansplugins.factionsystem.objects.domain;

import dansplugins.factionsystem.objects.domain.specification.IWar;

import java.time.LocalDateTime;
import java.util.Map;

public class War implements IWar {
    private String attacker;
    private String defender;
    private String reason;
    private LocalDateTime date;

    public War(Faction attacker, Faction defender, String reason) {
        this.attacker = attacker.getName();
        this.defender = defender.getName();
        this.reason = reason;
        date = LocalDateTime.now();
    }

    @Override
    public String getAttacker() {
        return attacker;
    }

    @Override
    public String getDefender() {
        return defender;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public Map<String, String> save() {
        // TODO: implement
        return null;
    }

    @Override
    public void load(Map<String, String> map) {
        // TODO: implement
    }
}