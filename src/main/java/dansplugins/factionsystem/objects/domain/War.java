package dansplugins.factionsystem.objects.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dansplugins.factionsystem.objects.domain.specification.IWar;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class War implements IWar {
    private String attacker;
    private String defender;
    private String reason;
    private LocalDateTime date;
    private boolean active;

    public War(Faction attacker, Faction defender, String reason) {
        this.attacker = attacker.getName();
        this.defender = defender.getName();
        this.reason = reason;
        date = LocalDateTime.now();
        active = true;
    }

    public War(Map<String, String> data) {
        this.load(data);
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
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public Map<String, String> save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();;
        Map<String, String> saveMap = new HashMap<>();

        saveMap.put("attacker", gson.toJson(attacker));
        saveMap.put("defender", gson.toJson(defender));
        saveMap.put("reason", gson.toJson(reason));
        saveMap.put("date", gson.toJson(date));
        saveMap.put("active", gson.toJson(active));
        return saveMap;
    }

    @Override
    public void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        attacker = gson.fromJson(data.get("attacker"), String.class);
        defender = gson.fromJson(data.get("defender"), String.class);
        reason = gson.fromJson(data.get("reason"), String.class);
        date = gson.fromJson(data.get("date"), LocalDateTime.class); // TODO: review this and make sure it works
        active = Boolean.parseBoolean(gson.fromJson(data.get("active"), String.class));
    }
}