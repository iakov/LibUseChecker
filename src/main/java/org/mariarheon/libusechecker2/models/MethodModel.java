package org.mariarheon.libusechecker2.models;

import ru.spbstu.insys.libsl.parser.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class MethodModel {
    private final int index;
    private final boolean isConstructor;
    private final String returnType;
    private final String className;
    private final String methodName;
    private final List<ParameterModel> parameters;
    private final ResultAssignmentModel resultAssignment;
    private final List<PreconditionDecl> preconditions;
    private final List<PostconditionDecl> postconditions;
    private final List<WhenDecl> whens;
    private final List<RequiresDecl> requiresList;
    private final List<EnsuresDecl> ensuresList;
    private final List<SetVarDecl> setVarList;

    public MethodModel(int index, boolean isConstructor, String returnType, String className, String methodName,
                       List<ParameterModel> parameters, ResultAssignmentModel resultAssignment,
                       List<PreconditionDecl> preconditions, List<PostconditionDecl> postconditions,
                       List<WhenDecl> whens, List<RequiresDecl> requiresList, List<EnsuresDecl> ensuresList,
                       List<SetVarDecl> setVarList) {
        this.index = index;
        this.isConstructor = isConstructor;
        this.returnType = returnType;
        this.className = className;
        this.methodName = methodName;
        this.parameters = parameters;
        this.resultAssignment = resultAssignment;
        this.preconditions = preconditions;
        this.postconditions = postconditions;
        this.whens = whens;
        this.requiresList = requiresList;
        this.ensuresList = ensuresList;
        this.setVarList = setVarList;
    }

    public int getIndex() {
        return index;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getSimpleType(String fullType) {
        int dotIndex = fullType.lastIndexOf('.');
        if (dotIndex == -1) {
            return fullType;
        }
        return fullType.substring(dotIndex + 1);
    }

    public String getSimpleReturnType() {
        if (isConstructor) {
            return "";
        }
        return getSimpleType(getReturnType());
    }

    public String getSimpleClassName() {
        return getSimpleType(className);
    }

    public String getParameterNames() {
        return parameters.stream().map(ParameterModel::getParName).collect(Collectors.joining(", "));
    }

    public String getSimpleSignature() {
        var simpleReturnType = getSimpleReturnType();
        return String.format("%s%s%s%s%s(%s)", simpleReturnType,
                simpleReturnType.isEmpty() ? "" : " ",
                getSimpleClassName(),
                isConstructor ? "" : ".",
                isConstructor ? "" : getMethodName(), getParameterNames());
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<ParameterModel> getParameters() {
        return parameters;
    }

    /**
     * Returns null if ResultAssignment does not exist
     * @return ResultAssignment or null
     */
    public ResultAssignmentModel getResultAssignment() {
        return resultAssignment;
    }

    public List<PreconditionDecl> getPreconditions() {
        return preconditions;
    }

    public List<PostconditionDecl> getPostconditions() {
        return postconditions;
    }

    public List<WhenDecl> getWhens() {
        return whens;
    }

    public List<RequiresDecl> getRequiresList() {
        return requiresList;
    }

    public List<EnsuresDecl> getEnsuresList() {
        return ensuresList;
    }

    public List<SetVarDecl> getSetVarList() {
        return setVarList;
    }
}
