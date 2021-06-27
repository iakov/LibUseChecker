package org.mariarheon.libusechecker2.maven;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PomXmlFile {
    private final Document doc;
    private final Element root;
    private final Path pomXmlPath;

    public PomXmlFile(String pomXmlPath) throws ParserConfigurationException, IOException, SAXException {
        this.pomXmlPath = Paths.get(pomXmlPath);;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        File file = new File(pomXmlPath);
        doc = builder.parse(file);
        root = doc.getDocumentElement();
    }

    public void addProperties() {
        var propertiesElement = addElementIfNotExist(root, "properties");
        addElementIfNotExistAndSetTextIfEmpty(propertiesElement, "project.build.sourceEncoding", "UTF-8");
        addElementIfNotExistAndSetTextIfEmpty(propertiesElement, "maven.compiler.source", "1.8");
        addElementIfNotExistAndSetTextIfEmpty(propertiesElement, "maven.compiler.target", "1.8");
    }

    public void addBuildHelperMavenPlugin(String outputPath, String outputRegex,
                                          String senderProjectPath, String senderProjectRegex) {
        var pluginElement = findPlugin("org.codehaus.mojo", "build-helper-maven-plugin");
        if (pluginElement == null) {
            var buildElement = addElementIfNotExist(this.root, "build");
            var pluginsElement = addElementIfNotExist(buildElement, "plugins");
            pluginElement = addElement(pluginsElement, "plugin");
            addElement(pluginElement, "groupId", "org.codehaus.mojo");
            addElement(pluginElement, "artifactId", "build-helper-maven-plugin");
        }
        var versionElements = byTag(pluginElement, "version");
        if (versionElements.isEmpty()) {
            addElement(pluginElement, "version", "3.2.0");
        }
        var executionsElement = addElementIfNotExist(pluginElement, "executions");
        var executionElement = findExecution(executionsElement, "generate-sources", "add-source");
        if (executionElement == null) {
            executionElement = addElement(executionsElement, "execution");
            addElement(executionElement, "phase", "generate-sources");
            var goalsElement = addElement(executionElement, "goals");
            addElement(goalsElement, "goal", "add-source");
        }
        var configurationElement = addElementIfNotExist(executionElement, "configuration");
        var sourcesElement = addElementIfNotExist(configurationElement, "sources");
        var aspectResSourceElement = findSource(sourcesElement, outputRegex);
        if (aspectResSourceElement == null) {
            addElement(sourcesElement, "source", outputPath);
        } else {
            aspectResSourceElement.setTextContent(outputPath);
        }
        var senderSourceElement = findSource(sourcesElement, senderProjectRegex);
        if (senderSourceElement == null) {
            addElement(sourcesElement, "source", senderProjectPath);
        } else {
            senderSourceElement.setTextContent(senderProjectPath);
        }
    }

    public void addAspectjMavenPlugin() {
        var pluginElement = findPlugin("se.haleby.aspectj", "aspectj-maven-plugin");
        if (pluginElement == null) {
            var buildElement = addElementIfNotExist(this.root, "build");
            var pluginsElement = addElementIfNotExist(buildElement, "plugins");
            pluginElement = addElement(pluginsElement, "plugin");
            addElement(pluginElement, "groupId", "se.haleby.aspectj");
            addElement(pluginElement, "artifactId", "aspectj-maven-plugin");
        }
        var versionElements = byTag(pluginElement, "version");
        if (versionElements.isEmpty()) {
            addElement(pluginElement, "version", "1.12.7");
        }
        var executionsElement = addElementIfNotExist(pluginElement, "executions");
        var executionElement = addElementIfNotExist(executionsElement, "execution");
        var goalsElement = addElementIfNotExist(executionElement, "goals");
        addElementIfNotExistAndSetTextIfEmpty(goalsElement, "goal", "compile");
        var configurationElement = addElementIfNotExist(pluginElement, "configuration");
        addElementIfNotExistAndSetTextIfEmpty(configurationElement, "source", "${maven.compiler.source}");
        addElementIfNotExistAndSetTextIfEmpty(configurationElement, "target", "${maven.compiler.target}");
        addElementIfNotExistAndSetTextIfEmpty(configurationElement, "complianceLevel", "${maven.compiler.target}");
        addElementIfNotExistAndSetTextIfEmpty(configurationElement, "encoding", "${project.build.sourceEncoding}");
    }

    public void addDependencyIfNotExist(String groupId,
                                         String artifactId, String version) {
        var dependenciesElement = addElementIfNotExist(root, "dependencies");
        var dependencyElement = findDependencyElement(dependenciesElement, groupId, artifactId);
        if (dependencyElement == null) {
            dependencyElement = addElement(dependenciesElement, "dependency");
            addElement(dependencyElement, "groupId", groupId);
            addElement(dependencyElement, "artifactId", artifactId);
            addElement(dependencyElement, "version", version);
        }
    }

    private Element findDependencyElement(Element dependenciesElement, String groupId, String artifactId) {
        var dependencyElements = byTag(dependenciesElement, "dependency");
        for (var dependencyElement : dependencyElements) {
            String foundGroupId = getOneValue(byTag(dependencyElement, "groupId"));
            String foundArtifactId = getOneValue(byTag(dependencyElement, "artifactId"));
            if (foundGroupId.equals(groupId) && foundArtifactId.equals(artifactId)) {
                return dependencyElement;
            }
        }
        return null;
    }

    private Element addElementIfNotExist(Element parent, String tagName) {
        var elements = byTag(parent, tagName);
        if (elements.isEmpty()) {
            return addElement(parent, tagName);
        }
        return elements.get(0);
    }

    private Element addElementIfNotExistAndSetTextIfEmpty(Element parent, String tagName, String value) {
        var element = addElementIfNotExist(parent, tagName);
        if (element.getTextContent().isEmpty()) {
            element.setTextContent(value);
        }
        return element;
    }

    private Element addElement(Element parent, String tagName, String value) {
        var newElement = addElement(parent, tagName);
        newElement.setTextContent(value);
        return newElement;
    }

    private Element addElement(Element parent, String tagName) {
        Element element = doc.createElement(tagName);
        parent.appendChild(element);
        return element;
    }

    // returns required <plugin>-Element or null if not found
    private Element findPlugin(String groupId, String artifactId) {
        var pluginIdList = byTags(this.root, new String[] {
                "build",
                "plugins",
                "plugin"
        });
        for (var pluginId : pluginIdList) {
            String foundGroupId = getOneValue(byTag(pluginId, "groupId"));
            String foundArtifactId = getOneValue(byTag(pluginId, "artifactId"));
            if (foundGroupId.equals(groupId) && foundArtifactId.equals(artifactId)) {
                return pluginId;
            }
        }
        return null;
    }

    private Element findExecution(Element executionsElement, String phase, String goal) {
        var executionElements = byTag(executionsElement, "execution");
        for (var executionElement : executionElements) {
            var phaseElement = byTag(executionElement, "phase");
            if (!getOneValue(phaseElement).equals(phase)) {
                continue;
            }
            var goalElements = byTags(executionElement, new String[] { "goals", "goal" });
            for (var goalElement : goalElements) {
                if (goalElement.getTextContent().equals(goal)) {
                    return executionElement;
                }
            }
        }
        return null;
    }

    private String getOneValue(List<Element> elements) {
        if (elements.size() <= 0) {
            return "";
        }
        return elements.get(0).getTextContent();
    }

    private List<Element> byTag(Element parent, String tag) {
        List<Element> res = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node childNode = children.item(i);
            if (childNode instanceof Element) {
                Element childElement = (Element) childNode;
                if (childElement.getTagName().equals(tag)) {
                    res.add(childElement);
                }
            }
        }
        return res;
    }

    private List<Element> byTags(Element parent, String[] tagHierarchy) {
        List<Element> currentLevel = new ArrayList<>();
        currentLevel.add(parent);
        for (String tag : tagHierarchy) {
            List<Element> newLevel = new ArrayList<>();
            for (var element : currentLevel) {
                var newElements = byTag(element, tag);
                newLevel.addAll(newElements);
            }
            currentLevel = newLevel;
        }
        return currentLevel;
    }

    private Element findSource(Element sourcesElement, String verificationRegex) {
        Pattern pattern = Pattern.compile(verificationRegex);
        var sourceElements = byTag(sourcesElement, "source");
        for (var sourceElement : sourceElements) {
            String sourceElementContent = sourceElement.getTextContent();
            Matcher matcher = pattern.matcher(sourceElementContent);
            if (matcher.find()) {
                return sourceElement;
            }
        }
        return null;
    }

    public String toString(int indent) {
        try {
            // Turn xml string into a document
            Document document = doc;

            // Remove whitespaces outside tags
            document.normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
                    document,
                    XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                node.getParentNode().removeChild(node);
            }

            // Setup pretty print options
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // Return pretty print xml string
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void save(int indent) throws IOException {
        Files.writeString(pomXmlPath, toString(indent), StandardCharsets.UTF_8);
    }
}
