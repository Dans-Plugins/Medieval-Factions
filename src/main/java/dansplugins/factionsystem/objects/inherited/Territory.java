package dansplugins.factionsystem.objects.inherited;

import dansplugins.factionsystem.objects.inherited.specification.ITerritory;

public class Territory implements ITerritory {

    protected String holder;

    @Override
    public void setHolder(String newHolder) {
        holder = newHolder;
    }

    @Override
    public String getHolder() {
        return holder;
    }

}