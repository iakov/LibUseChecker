package org.mariarheon.libusechecker2.models;

import ru.spbstu.insys.libsl.parser.IExpr;

import java.util.List;

public class ResultAssignmentModel {
    private final String automatonName;
    private final String automatonState;
    private final List<IExpr> varValues;

    public ResultAssignmentModel(String automatonName, String automatonState, List<IExpr> varValues) {
        this.automatonName = automatonName;
        this.automatonState = automatonState;
        this.varValues = varValues;
    }

    public String getAutomatonName() {
        return automatonName;
    }

    public String getAutomatonState() {
        return automatonState;
    }

    public List<IExpr> getVarValues() {
        return varValues;
    }
}
