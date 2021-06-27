package org.mariarheon.libusechecker2.gradle;

import org.mariarheon.libusechecker2.Util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildGradleFile {
    private final String PLUGINS_SECTION = "plugins";
    private final String DEPENDENCIES_SECTION = "dependencies";
    private final int INDENT = 2;
    private String fileContent;
    private final Path buildGradlePath;

    public BuildGradleFile(String pathToBuildGradle) throws IOException {
        this.buildGradlePath = Paths.get(pathToBuildGradle);
        fileContent = Files.readString(buildGradlePath, StandardCharsets.UTF_8);
    }

    private String getSectionContent(String sectionName) {
        return getSectionContent(new String[] { sectionName });
    }

    private String getSectionContent(String[] sectionHierarchy) {
        if (!sectionExists(sectionHierarchy)) {
            return "";
        }
        int startContentIndex = getSectionStartContentIndex(sectionHierarchy);
        int endContentIndex = getSectionEndContentIndex(sectionHierarchy);
        return fileContent.substring(startContentIndex, endContentIndex);
    }

    private List<Dependency> getDependencies() {
        List<Dependency> res = new ArrayList<>();
        String dependenciesSection = getSectionContent(DEPENDENCIES_SECTION);
        String regex = "\\b(\\w+)((\\s*['\"](.+?):(.+?):(.+?)['\"])|(\\s+group\\s*:\\s*['\"](.+?)['\"]\\s*,\\s*name\\s*:\\s*['\"](.+?)['\"]\\s*,\\s*version\\s*:\\s*['\"](.+?)['\"]))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dependenciesSection);
        while (matcher.find()) {
            String type, group, name, version;
            type = matcher.group(1);
            if (matcher.group(4) != null) {
                group = matcher.group(4);
                name = matcher.group(5);
                version = matcher.group(6);
            } else {
                group = matcher.group(8);
                name = matcher.group(9);
                version = matcher.group(10);
            }
            Dependency dependency = new Dependency(type, group, name, version);
            res.add(dependency);
        }
        return res;
    }

    public void addDependency(String type, String group, String name, String version) {
        addDependency(new Dependency(type, group, name, version));
    }

    public void addDependency(Dependency dependency) {
        if (!dependencyPresent(dependency)) {
            addNewDependency(dependency);
        }
    }

    private void addNewDependency(Dependency dependency) {
        addSection(DEPENDENCIES_SECTION);
        int endContentIndex = getSectionEndContentIndex(DEPENDENCIES_SECTION);
        fileContent = Util.insertAtIndex(fileContent, tabs(1) + dependency + "\n", endContentIndex);
    }

    private boolean dependencyPresent(Dependency dependency) {
        var existedDependencies = getDependencies();
        for (var existedDependency : existedDependencies) {
            if (existedDependency.equalsExceptVersion(dependency)) {
                return true;
            }
        }
        return false;
    }

    private boolean sectionExists(String sectionName) {
        return sectionExists(new String[] { sectionName });
    }

    private boolean sectionExists(String[] sectionHierarchy) {
        return getSectionStartContentIndex(sectionHierarchy) > -1;
    }

    private void addSection(String[] sectionHierarchy) {
        if (sectionHierarchy.length <= 0) {
            return;
        }
        List<String> sections = new ArrayList<String>();
        for (int i = 0; i < sectionHierarchy.length; i++) {
            List<String> sections1 = new ArrayList<>(sections);
            sections1.add(sectionHierarchy[i]);
            String[] sections1AsArray = sections1.toArray(new String[]{});
            String[] sectionsAsArray = sections.toArray(new String[]{});
            System.out.println("sectionsAsArray: " + String.join(",", sectionsAsArray));
            System.out.println("sections1AsArray: " + String.join(",", sections1AsArray));
            System.out.println("sectionExists: " + sectionExists(sections1AsArray));
            if (!sectionExists(sections1AsArray)) {
                String sectionName = sections1AsArray[i];
                if (i == 0) {
                    fileContent += String.format("%s {\n}", sectionName);
                } else {
                    int endIndex = getSectionEndContentIndex(sectionsAsArray);
                    String inserted = String.format("%s%s {\n%s}\n%s", tabs(1), sectionName, tabs(i), tabs(i - 1));
                    fileContent = Util.insertAtIndex(fileContent, inserted, endIndex);
                }
            }
            sections.add(sectionHierarchy[i]);
        }
    }

    private void addSection(String sectionName) {
        if (sectionExists(sectionName)) {
            return;
        }
        String newSection = String.format("%s {\n}", sectionName);
        if (sectionName.equals(PLUGINS_SECTION)) {
            fileContent = String.format("%s\n\n", newSection) + fileContent;
        } else {
            fileContent += String.format("\n\n%s", newSection);
        }
    }

    private int getSectionStartContentIndex(String sectionName) {
        return getSectionStartContentIndex(new String[] { sectionName });
    }

    // or -1 if not exist
    private int getSectionStartContentIndex(String[] sectionHierarchy) {
        int res = 0;
        String snippet = fileContent;
        for (var sectionName : sectionHierarchy) {
            int startIndex = getSectionStartContentIndexForSnippet(snippet, sectionName);
            if (startIndex == -1) {
                return -1;
            }
            int endIndex = findEndBrace(startIndex, snippet);
            snippet = snippet.substring(startIndex, endIndex);
            res += startIndex;
        }
        return res;
    }

    private int getSectionStartContentIndexForSnippet(String snippet, String sectionName) {
        String regex = "\\b" + sectionName + "\\s*\\{";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(snippet);
        if (!matcher.find()) {
            return -1;
        }
        return matcher.end();
    }

    private int getSectionEndContentIndex(String sectionName) {
        return getSectionEndContentIndex(new String[] { sectionName });
    }

    private int getSectionEndContentIndex(String[] sectionHierarchy) {
        int startContentIndex = getSectionStartContentIndex(sectionHierarchy);
        if (startContentIndex == -1) {
            return -1;
        }
        return findEndBrace(startContentIndex);
    }

    private int findEndBrace(int startIndex, String snippet) {
        int braceNumber = 0;
        for (int i = startIndex; i < snippet.length(); i++) {
            if (snippet.charAt(i) == '{') {
                braceNumber++;
            } else if (snippet.charAt(i) == '}') {
                if (braceNumber <= 0) {
                    return i;
                }
                braceNumber--;
            }
        }
        return snippet.length();
    }

    private int findEndBrace(int startIndex) {
        return findEndBrace(startIndex, fileContent);
    }

    @Override
    public String toString() {
        return fileContent;
    }

    public void addPlugin(String id, String version) {
        addPlugin(new Plugin(id, version));
    }

    public void addPlugin(Plugin plugin) {
        if (!pluginPresent(plugin)) {
            addNewPlugin(plugin);
        }
    }

    private void addNewPlugin(Plugin plugin) {
        addSection(PLUGINS_SECTION);
        int endContentIndex = getSectionEndContentIndex(PLUGINS_SECTION);
        fileContent = Util.insertAtIndex(fileContent, tabs(1) + plugin + "\n", endContentIndex);
    }

    private boolean pluginPresent(Plugin plugin) {
        var existedPlugins = getPlugins();
        for (var existedPlugin : existedPlugins) {
            if (existedPlugin.equalsExceptVersion(plugin)) {
                return true;
            }
        }
        return false;
    }

    private List<Plugin> getPlugins() {
        List<Plugin> res = new ArrayList<>();
        String pluginsSection = getSectionContent(PLUGINS_SECTION);
        String regex = "\\bid\\s*['\"](.+?)['\"]\\s*version\\s*['\"](.+?)['\"]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(pluginsSection);
        while (matcher.find()) {
            String id, version;
            id = matcher.group(1);
            version = matcher.group(2);
            Plugin plugin = new Plugin(id, version);
            res.add(plugin);
        }
        return res;
    }

    // true if was successfully added.
    // false if it already exists.
    public boolean addMainSrcDir() {
        if (mainJavaSrcDirsExist()) {
            return false;
        }
        addSrcDir("src/main/java", null);
        return true;
    }

    public void addSrcDir(String dir) {
        addSrcDir(dir, null);
    }

    public void addSrcDir(String dir, String verificationRegex) {
        if (verificationRegex != null && setNewDirIfExists(dir, verificationRegex)) {
            return;
        }
        var sectionHierarchy = new String[] {"sourceSets", "main", "java"};
        addSection(sectionHierarchy);
        int endIndex = getSectionEndContentIndex(sectionHierarchy);
        String src = String.format("srcDirs '%s'", dir);
        String inserted = String.format("%s%s\n%s", tabs(1), src, tabs(2));
        fileContent = Util.insertAtIndex(fileContent, inserted, endIndex);
    }

    // true if srcDir was found which satisfied verificationRegex and it was successfully changed to "dir".
    // otherwise, false
    private boolean setNewDirIfExists(String dir, String verificationRegex) {
        String[] sectionHierarchy = new String[] {"sourceSets", "main", "java"};
        String content = getSectionContent(sectionHierarchy);
        int baseIndex = getSectionStartContentIndex(sectionHierarchy);
        String regex = "srcDirs\\s*['\"](.+?)['\"]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String srcDir = matcher.group(1);
            Pattern pattern2 = Pattern.compile(verificationRegex);
            Matcher matcher2 = pattern2.matcher(srcDir);
            if (matcher2.find()) {
                int startIndex = baseIndex + matcher.start(1);
                int endIndex = baseIndex + matcher.end(1);
                fileContent = fileContent.substring(0, startIndex) +
                        dir + fileContent.substring(endIndex);
                return true;
            }
        }
        return false;
    }

    private boolean mainJavaSrcDirsExist() {
        String content = getSectionContent(new String[] {"sourceSets", "main", "java"});
        return content.contains("srcDirs");
    }

    private String tabs(int count) {
        return " ".repeat(count * INDENT);
    }

    public void save() throws IOException {
        Files.writeString(buildGradlePath, fileContent, StandardCharsets.UTF_8);
    }
}
