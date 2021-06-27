package org.mariarheon.libusechecker2.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutomatonModel {
    public final String name;
    public final boolean hasFinishStates;
    public final Map<String, Boolean> stateIsFinished;
    // close -> {created->closed, binded->closed, closed->closed}
    public final Map<String, Map<String, String>> methodToStateModification;
    public final Map<String, VarTypeModel> varTypes;
    private final List<String> varsOrder;

    public AutomatonModel(String name, boolean hasFinishStates, Map<String, Boolean> stateIsFinished,
                          Map<String, Map<String, String>> methodToStateModification,
                          Map<String, VarTypeModel> varTypes,
                          List<String> varsOrder) {
        this.name = name;
        this.hasFinishStates = hasFinishStates;
        this.stateIsFinished = stateIsFinished;
        this.methodToStateModification = methodToStateModification;
        this.varTypes = varTypes;
        this.varsOrder = varsOrder;
        assert(varTypes.size() == varsOrder.size());
    }

    public Map<String, Object> valuesByVarsOrder(List<Object> varValues) throws Exception {
        var res = new HashMap<String, Object>();
        for (int i = 0; i < varValues.size(); i++) {
            var varName = varsOrder.get(i);
            var value = varValues.get(i);
            checkType(varName, value);
            res.put(varName, value);
        }
        return res;
    }

    public void checkType(String varName, Object varValue) throws Exception {
        var varType = varTypes.get(varName);
        if (varType == VarTypeModel.BOOLEAN) {
            if (!(varValue instanceof Boolean)) {
                throw new Exception("[error] [bad-variable-value] Bad variable value for automaton variable " + varName + ". Should be bool");
            }
        } else if (varType == VarTypeModel.INT) {
            if (!(varValue instanceof Integer)) {
                throw new Exception("[error] [bad-variable-value] Bad variable value for automaton variable " + varName + ". Should be int");
            }
        } else if (varType == VarTypeModel.STRING) {
            if (!(varValue instanceof String)) {
                throw new Exception("[error] [bad-variable-value] Bad variable value for automaton variable " + varName + ". Should be String");
            }
        } else if (varType == VarTypeModel.DOUBLE) {
            if (!(varValue instanceof Double)) {
                throw new Exception("[error] [bad-variable-value] Bad variable value for automaton variable " + varName + ". Should be Double");
            }
        } else {
            throw new Exception("[error] [unsupported-variable-type] Unsupported variable type for automaton variable " + varName);
        }
    }
}
