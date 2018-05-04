package com.jetbrains.python.inspection;

public class NodeResult {

    enum NodeResultType { SKIP, INT, BOOL }

    // false - boolean node, true - int node
    private NodeResultType type;
    private Boolean boolValue;
    private int intValue;

    public NodeResult() {
        this.type = NodeResultType.SKIP;
        this.boolValue = null;
    }

    public NodeResult(Boolean boolValue) {
        this.type = NodeResultType.BOOL;
        this.boolValue = boolValue;
    }

    public NodeResult(Integer intValue) {
        this.type = NodeResultType.INT;
        this.intValue = intValue;
    }

    public NodeResultType getType() {
        return this.type;
    }

    public Boolean getBoolValue() {
        return this.boolValue;
    }

    public int getIntValue() {
        return this.intValue;
    }

}
