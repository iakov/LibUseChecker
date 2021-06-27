package org.mariarheon.libusechecker2;

public interface StringConsumer {
    void consume(String value);
    void consumeError(String error, String stackTrace);
}
