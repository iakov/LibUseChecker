package org.mariarheon.libusechecker2.models;

/**
 *
 */
public class TypeModel {
    private String typeName;
    private String codeTypeName;

    public TypeModel(String typeName, String codeTypeName) {
        this.typeName = typeName;
        this.codeTypeName = codeTypeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getCodeTypeName() {
        return codeTypeName;
    }
}
