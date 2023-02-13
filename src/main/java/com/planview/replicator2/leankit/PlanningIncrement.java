package com.planview.replicator2.leankit;

import java.util.Date;

public class PlanningIncrement {
    public String id, label;
    public Date startDate, endDate;
    public IncrementSeries series;
    public ParentIncrement parent;
}
