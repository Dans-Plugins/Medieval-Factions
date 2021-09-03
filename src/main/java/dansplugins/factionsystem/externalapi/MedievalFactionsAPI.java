package dansplugins.factionsystem.externalapi;

import dansplugins.factionsystem.data.PersistentData;

public class MedievalFactionsAPI implements IMedievalFactionsAPI {

    @Override
    public FactionInfo getFaction(String factionName) {
        return new FactionInfo(PersistentData.getInstance().getFaction(factionName));
    }
}
