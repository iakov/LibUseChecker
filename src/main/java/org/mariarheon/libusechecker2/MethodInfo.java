package org.mariarheon.libusechecker2;

import java.util.HashMap;
import java.util.Map;

public class MethodInfo {
    private int methodIndex;
    private int targetId;
    private int returnObjectId;
    private ConditionInfo[] preconditions;
    private ConditionInfo[] postconditions;
    private String sourceLocation;
    private boolean automatonDestroyed;
    private String[] stackTrace;
    private Map<String, Integer> intParamValues;
    private Map<String, Double> doubleParamValues;
    private Map<String, Boolean> boolParamValues;
    private Map<String, String> stringParamValues;
    private int intResult;
    private double doubleResult;
    private boolean boolResult;
    private String stringResult;
    // 0 - undefined
    // 1 - int
    // 2 - double
    // 3 - boolean
    // 4 - String
    private int resultType;

    public MethodInfo() {
        this.intParamValues = new HashMap<String, Integer>();
        this.doubleParamValues = new HashMap<String, Double>();
        this.boolParamValues = new HashMap<String, Boolean>();
        this.stringParamValues = new HashMap<String, String>();
        this.methodIndex = -1;
        this.targetId = -1;
        this.returnObjectId = -1;
        this.resultType = 0;
    }

    public MethodInfo(int methodIndex, int targetId, int returnObjectId) {
        this.intParamValues = new HashMap<String, Integer>();
        this.doubleParamValues = new HashMap<String, Double>();
        this.boolParamValues = new HashMap<String, Boolean>();
        this.stringParamValues = new HashMap<String, String>();
        this.methodIndex = methodIndex;
        this.targetId = targetId;
        this.returnObjectId = returnObjectId;
        this.resultType = 0;
    }

    public void setPreconditions(ConditionInfo[] preconditions) {
        this.preconditions = preconditions;
    }

    public void setPostconditions(ConditionInfo[] postconditions) {
        this.postconditions = postconditions;
    }

    public int getMethodIndex() {
        return methodIndex;
    }

    public void setMethodIndex(int methodIndex) {
        this.methodIndex = methodIndex;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public int getReturnObjectId() {
        return returnObjectId;
    }

    public void setReturnObjectId(int returnObjectId) {
        this.returnObjectId = returnObjectId;
    }

    public static MethodInfo forConstructor(int methodIndex, int returnObjectId) {
        return new MethodInfo(methodIndex, -1, returnObjectId);
    }

    public static MethodInfo forUsualMethod(int methodIndex, int targetId, int returnObjectId) {
        return new MethodInfo(methodIndex, targetId, returnObjectId);
    }

    public static MethodInfo forVoidMethod(int methodIndex, int targetId) {
        return new MethodInfo(methodIndex, targetId, -1);
    }

    public static MethodInfo forDestroyedSignal(int targetId) {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.automatonDestroyed = true;
        methodInfo.targetId = targetId;
        return methodInfo;
    }

    public ConditionInfo[] getPreconditions() {
        return preconditions;
    }

    public ConditionInfo[] getPostconditions() {
        return postconditions;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public boolean isDestroyedSignal() {
        return this.automatonDestroyed;
    }

    public void setStackTrace(String[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    public void setStackTrace(StackTraceElement[] stackTrace) {
        String[] res = new String[stackTrace.length];
        for (int i = 0; i < stackTrace.length; i++) {
            res[i] = stackTrace[i].toString();
        }
        this.stackTrace = res;
    }

    public String[] getStackTrace() {
        return this.stackTrace;
    }

    public Map<String, Integer> getIntParamValues() {
        return intParamValues;
    }

    public void setIntParamValues(Map<String, Integer> intParamValues) {
        this.intParamValues = intParamValues;
    }

    public Map<String, Double> getDoubleParamValues() {
        return doubleParamValues;
    }

    public void setDoubleParamValues(Map<String, Double> doubleParamValues) {
        this.doubleParamValues = doubleParamValues;
    }

    public Map<String, Boolean> getBoolParamValues() {
        return boolParamValues;
    }

    public void setBoolParamValues(Map<String, Boolean> boolParamValues) {
        this.boolParamValues = boolParamValues;
    }

    public Map<String, String> getStringParamValues() {
        return stringParamValues;
    }

    public void setStringParamValues(Map<String, String> stringParamValues) {
        this.stringParamValues = stringParamValues;
    }

    public void addParamValue(String param, Object value) {
        if (value instanceof Integer) {
            intParamValues.put(param, (int)value);
        } else if (value instanceof Double) {
            doubleParamValues.put(param, (double)value);
        } else if (value instanceof Boolean) {
            boolParamValues.put(param, (boolean)value);
        } else if (value instanceof String) {
            stringParamValues.put(param, (String)value);
        }
    }

    public void setResultValue(Object value) {
        // 0 - undefined
        // 1 - int
        // 2 - double
        // 3 - boolean
        // 4 - String
        if (value instanceof Integer) {
            resultType = 1;
            intResult = (int)value;
        } else if (value instanceof Double) {
            resultType = 2;
            doubleResult = (double) value;
        } else if (value instanceof Boolean) {
            resultType = 3;
            boolResult = (boolean) value;
        } else if (value instanceof String) {
            resultType = 4;
            stringResult = (String) value;
        }
    }

    public Object getResultValue() {
        if (resultType == 1) {
            return intResult;
        }
        if (resultType == 2) {
            return doubleResult;
        }
        if (resultType == 3) {
            return boolResult;
        }
        if (resultType == 4) {
            return stringResult;
        }
        return null;
    }

    public Map<String, Object> getAllParamValues() {
        Map<String, Object> res = new HashMap<String, Object>();
        res.putAll(intParamValues);
        res.putAll(doubleParamValues);
        res.putAll(boolParamValues);
        res.putAll(stringParamValues);
        return res;
    }
}