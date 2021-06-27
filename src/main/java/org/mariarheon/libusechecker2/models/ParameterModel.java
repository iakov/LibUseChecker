package org.mariarheon.libusechecker2.models;

/**
 *
 */
public class ParameterModel {
    private int index;
    private String typeName;
    private String parName;

    public ParameterModel(int index, String typeName, String parName) {
        this.index = index;
        this.typeName = typeName;
        this.parName = parName;
    }

    public int getIndex() {
        return index;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getParName() {
        return parName;
    }
}
