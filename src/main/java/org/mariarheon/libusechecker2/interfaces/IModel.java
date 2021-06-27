package org.mariarheon.libusechecker2.interfaces;

import java.io.IOException;
import java.nio.file.Path;

/**
 *
 */
public interface IModel {
    void generateAspects(Path folder) throws IOException;
    void display();
}
