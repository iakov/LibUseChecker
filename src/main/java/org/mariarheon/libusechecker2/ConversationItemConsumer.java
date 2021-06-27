package org.mariarheon.libusechecker2;

public interface ConversationItemConsumer {
    void start();
    void processOutput(String message);
    void finished(int exitCode);
    void error(String msg);
}
