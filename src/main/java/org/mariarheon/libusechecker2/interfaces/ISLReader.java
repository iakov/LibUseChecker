package org.mariarheon.libusechecker2.interfaces;

import java.io.InputStream;

public interface ISLReader {
    IModel read(InputStream inputStream) throws Exception;
}
