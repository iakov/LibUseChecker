package org.mariarheon.libusechecker2.gradle;

public class Dependency {
    // "implementation", "compile", etc. (the same as in build.gradle file in dependencies section)
    private String type;
    private String group;
    private String name;
    private String version;

    public Dependency(String type, String group, String name, String version) {
        this.type = type;
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return String.format("%s '%s:%s:%s'", type, group, name, version);
    }

    public boolean equalsExceptVersion(Dependency dependency) {
        return type.equals(dependency.type) && group.equals(dependency.group) && name.equals(dependency.name);
    }
}
