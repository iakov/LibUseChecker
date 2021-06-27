package org.mariarheon.libusechecker2;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CompilerInvoker {
    private final Path pathToAjc;
    private final Path pathToAspectjrtJar;
    private final Path pathToTestProject;
    private final Path pathToOutputFolder;
    private final Path pathToOutputJar;
    private final Path pathToJavaExe;
    private final Path pathToSenderProject;
    private final String mainClass;
    private ConversationWithTheProcess conversationThread;
    private final String userLibs;

    public CompilerInvoker(Path pathToAjc, Path pathToTestProject, Path pathToOutputFolder,
                           Path pathToJavaExe, Path pathToSenderProject, String mainClass,
                           String classPath) {
        this.pathToAjc = pathToAjc.normalize();
        this.pathToAspectjrtJar = pathToAjc.resolve("../../lib/aspectjrt.jar").normalize();
        this.pathToTestProject = pathToTestProject.resolve("src").normalize();
        this.pathToOutputFolder = pathToOutputFolder.normalize();
        this.pathToOutputJar = this.pathToOutputFolder.resolve("result.jar").normalize();
        this.pathToJavaExe = pathToJavaExe.normalize();
        this.pathToSenderProject = pathToSenderProject.normalize();
        this.mainClass = mainClass;
        this.userLibs = classPath;
    }

    public void compile(ConversationItemConsumer consumer) {
        var builder = new ProcessBuilder();
        File dir = new File(pathToSenderProject.resolve("libs").toString());
        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".jar"));
        assert files != null;
        var jarLibs = Arrays.stream(files).map(File::getAbsolutePath).collect(Collectors.joining(";"));
        builder.command(pathToAjc.toString(),
                "-classpath",
                "\"" + jarLibs + ";" + pathToAspectjrtJar + userLibsStr() + "\"", // quotes for windows 10; do not remove!
                "-sourceroots",
                "\"" + pathToSenderProject.resolve("src") + ";" + pathToOutputFolder + ";" + pathToTestProject + "\"", // quotes for windows 10; do not remove!
                "-source",
                "1.8",
                "-outjar",
                "\"" + pathToOutputJar.toString() + "\""); // quotes for windows 10; do not remove!
        builder.redirectErrorStream(true);
        var conversationThread = new ConversationWithTheProcess(builder, consumer);
        conversationThread.start();
    }

    public void start(ConversationItemConsumer consumer) {
        var builder = new ProcessBuilder();
        builder.command(pathToJavaExe.toString(),
                "-cp",
                pathToSenderProject.resolve("libs") + "/*;" + pathToAspectjrtJar + ";" + pathToOutputJar + userLibsStr(),
                mainClass);
        builder.redirectErrorStream(true);
        conversationThread = new ConversationWithTheProcess(builder, consumer);
        conversationThread.start();
    }

    public String userLibsStr() {
        String userLibsStr = "";
        if (userLibs != null && !userLibs.isBlank()) {
            userLibsStr = ";" + userLibs;
        }
        return userLibsStr;
    }

    public void stop() {
        conversationThread.stopProcess();
    }
}
