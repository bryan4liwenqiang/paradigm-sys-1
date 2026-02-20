package com.example.sdlc.mapping.model;

import java.util.ArrayList;
import java.util.List;

public class Trace {
    private List<String> appliedRules = new ArrayList<>();
    private List<String> droppedByCondition = new ArrayList<>();

    public List<String> getAppliedRules() {
        return appliedRules;
    }

    public void setAppliedRules(List<String> appliedRules) {
        this.appliedRules = appliedRules;
    }

    public List<String> getDroppedByCondition() {
        return droppedByCondition;
    }

    public void setDroppedByCondition(List<String> droppedByCondition) {
        this.droppedByCondition = droppedByCondition;
    }
}
