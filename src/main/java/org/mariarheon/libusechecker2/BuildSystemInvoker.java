package org.mariarheon.libusechecker2;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class BuildSystemInvoker {
    private final Path projectPath;
    private final Path buildSystemPath;
    private final String goals;
    private ConversationWithTheProcess conversationThread;
    // isGradle is true for gradle projects and false for maven projects
    private final boolean isGradle;

    public BuildSystemInvoker(boolean isGradle, Path buildSystemPath, Path projectPath, String goals) {
        this.isGradle = isGradle;
        this.goals = goals;
        this.projectPath = projectPath.normalize();
        this.buildSystemPath = buildSystemPath.normalize();
    }

    public void start(ConversationItemConsumer consumer) {
        var builder = new ProcessBuilder();
        var args = new ArrayList<String>();
        args.add(buildSystemPath.toString());
        args.addAll(Arrays.asList(goals.split("\\s+")));
        builder.command(args);
        builder.redirectErrorStream(true);
        builder.directory(projectPath.toFile());
        conversationThread = new ConversationWithTheProcess(builder, consumer);
        conversationThread.start();
    }

    public void stop() {
        conversationThread.stopProcess();
    }
}
