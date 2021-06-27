package org.mariarheon.libusechecker2;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestGeneratorInvoker {
    private ConversationWithTheProcess conversationThread;

    public TestGeneratorInvoker() {
    }

    public void start(ConversationItemConsumer consumer, String path2Kex, List<String> cmdWithArgs) {
        var builder = new ProcessBuilder();
        builder.directory(new File(path2Kex));
        builder.command(cmdWithArgs);
        builder.redirectErrorStream(true);
        conversationThread = new ConversationWithTheProcess(builder, consumer);
        conversationThread.start();
    }

    public void stop() {
        conversationThread.stopProcess();
    }
}
