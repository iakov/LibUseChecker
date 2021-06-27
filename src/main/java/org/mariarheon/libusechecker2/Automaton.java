package org.mariarheon.libusechecker2;

import org.mariarheon.libusechecker2.models.AutomatonModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Automaton {
    private String state;
    private final AutomatonModel model;
    private final StringConsumer consumer;
    private final int objID;
    private final String instantiationLocation;
    private final String[] instantiationStackTrace;
    private Map<String, Object> varValues;

    public Automaton(AutomatonModel model, String state, StringConsumer consumer, int objID,
                     String instantiationLocation, String[] instantiationStackTrace,
                     List<Object> varValues) throws Exception {
        this.instantiationLocation = instantiationLocation;
        this.instantiationStackTrace = instantiationStackTrace;
        this.objID = objID;
        this.model = model;
        this.state = state;
        this.consumer = consumer;
        System.out.println(this.model);
        if (!this.model.stateIsFinished.containsKey(state)) {
            throw new Exception("[error] [missing-state] No state named " + state + " in automaton " + model.name +
                    ", but it is used in ResultAssignmentStatement.");
        }
        if (varValues.size() != model.varTypes.size()) {
            throw new Exception("[error] [bad-var-values-count] Wrong count of variable values. Used " + varValues.size() + "; required " + model.varTypes.size());
        }
        this.varValues = model.valuesByVarsOrder(varValues);

        consumer.consume("Automaton " + model.name + " #" + objID + " created (STATE=" + this.state + ", " + varValuesUserFriendly() + ").");
    }

    public String varValuesUserFriendly() {
        var varValuesRes = varValues.entrySet().stream().map(x -> x.getKey() + "=" + x.getValue()).collect(Collectors.toList());
        return String.join(", ", varValuesRes);
    }

    public Map<String, Object> getVarValues() {
        return varValues;
    }

    public void setVarValueWithCheck(String varName, Object varValue) throws Exception {
        model.checkType(varName, varValue);
        varValues.put(varName, varValue);
    }

    public int getID() {
        return objID;
    }

    public void checkStateAndShift(String methodName, String[] stackTrace) {
        if (!this.model.methodToStateModification.containsKey(methodName)) {
            return;
        }
        var stateModification = this.model.methodToStateModification.get(methodName);
        if (!stateModification.containsKey(state)) {
            String error = "[error] [wrong-call-order] During method \"" +
                    methodName + "\" execution the automaton \"" + this.model.name + "\" #" + objID + " was in the state \"" + state + "\".";
            String st = Util.combinedStackTrace(stackTrace);
            consumer.consumeError(error, st);
        } else {
            this.state = stateModification.get(state);
        }
    }

    public void verifyFinishState() {
        if (!this.model.hasFinishStates) {
            return;
        }
        if (!this.model.stateIsFinished.get(state)) {
            String error = "[error] [wrong-finishstate] the actual finish state of automaton \"" + this.model.name +
                    "\" #" + objID + " created at " + instantiationLocation + " is \"" + this.state + "\", which is not acceptable finishstate.";
            String st = Util.combinedStackTrace(this.instantiationStackTrace);
            consumer.consumeError(error, st);
        }
    }
}
