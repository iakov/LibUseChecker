package org.mariarheon.libusechecker2;

import org.mariarheon.libusechecker2.gui.MainWindow;
import org.mariarheon.libusechecker2.maven.PomXmlFile;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        var wind = new MainWindow();
        wind.start();
    }
}


