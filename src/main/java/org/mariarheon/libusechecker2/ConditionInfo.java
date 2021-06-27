package org.mariarheon.libusechecker2;

public class ConditionInfo {
    private final boolean satisfied;
    private final String formatString;
    private final String condName;

    public ConditionInfo(boolean satisfied, String formatString, String condName) {
        this.satisfied = satisfied;
        this.formatString = formatString;
        this.condName = condName;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public String getFormatString() {
        return formatString;
    }

    public String getCondName() {
        return condName;
    }
}
