package org.mariarheon.libusechecker2.gradle;

public class Plugin {
    private String id;
    private String version;

    public Plugin(String id, String version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean equalsExceptVersion(Plugin plugin) {
        return id.equals(plugin.id);
    }

    @Override
    public String toString() {
        return String.format("id \"%s\" version \"%s\"", id, version);
    }
}
