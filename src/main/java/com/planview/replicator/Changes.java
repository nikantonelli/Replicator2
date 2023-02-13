package com.planview.replicator;

public class Changes {
    private Integer currChgRow;
    private Integer currItmRow;

    public Changes() {

    }

    public Integer getChangeRow() {
        return currChgRow;
    }

    public Integer getItemRow() {
        return currItmRow;
    }

    public Changes(Integer startChg, Integer startItm) {
        currChgRow = startChg;
        currItmRow = startItm;
    }
}
