package org.mariarheon.libusechecker2.gui;

import org.mariarheon.libusechecker2.*;
import org.mariarheon.libusechecker2.gradle.BuildGradleFile;
import org.mariarheon.libusechecker2.interfaces.IModel;
import org.mariarheon.libusechecker2.maven.PomXmlFile;
import org.mariarheon.libusechecker2.models.SpecModel;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class MainWindow extends JFrame {
    private static final String KEY_GOALS = "goals";
    private static final String KEY_MAVEN_GRADLE_PROJECT = "start_manually";
    private static final String KEY_AJC_PATH = "ajc_path";
    private static final String KEY_TESTED_PROJECT_PATH = "tested_project_path";
    private static final String KEY_OUTPUT_FOLDER_PATH = "output_folder_path";
    private static final String KEY_BUILD_SYSTEM_PATH = "build_system_path";
    private static final String KEY_JAVA_EXE_PATH = "java_exe_path";
    private static final String KEY_SENDER_PROJECT_PATH = "sender_project_path";
    private static final String KEY_LIBSL_PATH = "libsl_path";
    private static final String KEY_MAIN_CLASS = "main_class";
    private static final String KEY_USER_CLASS_PATH = "user_class_path";

    private final int w = 600*2;
    private final int h = 700;
    private IModel model;
    private final Map<String, String> pathByKey;
    private JTextArea taLog;
    private JTextArea taOutput;
    private JScrollPane logScroll;
    private JScrollPane outputScroll;
    private JButton btnReadModel;
    private JButton btnGenerateAspects;
    private JButton btnCompile;
    private JButton btnRun;
    private JButton btnInjectBuildFile;
    private JButton btnStartProjectWithBuildSystem;
    private JButton btnGenerateTests;
    private boolean isRunning = false;
    private CompilerInvoker invoker;
    private boolean mavenGradleProject;
    private JCheckBox cbMavenGradleProject;
    private Retriever retriever;
    private Thread retrieverThread;
    private boolean buildSystemIsRunning;
    private BuildSystemInvoker buildSystemInvoker;

    public MainWindow() {
        super();
        pathByKey = new HashMap<>();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setPreferredSize(new Dimension(w, h));
        this.setLayout(null);
        this.setResizable(false);
        this.pack();
        this.setTitle("Library Specification Verifier");
        this.setJMenuBar(createMenu());
    }

    private JMenuBar createMenu() {
        var res = new JMenuBar();
        var actions = new JMenu("Actions");
        var showErrors = new JMenuItem("Show errors...");
        showErrors.addActionListener(e -> {
            var errorsWindow = new ErrorsWindow(Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH), "errors.bsl"));
        });
        var removeLogFiles = new JMenuItem("Remove log files");
        removeLogFiles.addActionListener(e -> {
            Path mainLogPath = Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH), "main.log");
            Path outputLogPath = Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH), "output.log");
            if (Files.exists(mainLogPath)) {
                try {
                    Files.delete(mainLogPath);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            if (Files.exists(outputLogPath)) {
                try {
                    Files.delete(outputLogPath);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        var exit = new JMenuItem("Exit");
        exit.addActionListener(e -> {
            this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        });
        actions.add(showErrors);
        actions.add(removeLogFiles);
        actions.addSeparator();
        actions.add(exit);
        var how = new JMenu("Help");
        var howToUse = new JMenuItem("How to Use...");
        how.add(howToUse);
        howToUse.addActionListener(e -> {
            var aboutWindow = new AboutWindow();
        });
        res.add(actions);
        res.add(how);
        return res;
    }

    public void start() {
        var pref = Preferences.userNodeForPackage(org.mariarheon.libusechecker2.Main.class);
        var shouldStartManually = pref.get(KEY_MAVEN_GRADLE_PROJECT, "true").equals("true");
        start(shouldStartManually);
    }

    public void start(boolean mavenGradleProject) {
        this.mavenGradleProject = mavenGradleProject;
        this.getContentPane().removeAll();
        this.revalidate();
        this.repaint();
        int o = 30;
        int step = 55;

        addCheckBox();
        if (mavenGradleProject) {
            addRow(o, KEY_BUILD_SYSTEM_PATH, "Path to the build system:", true, true);
            addRow(o + step, KEY_TESTED_PROJECT_PATH, "Path to tested project:", true, false);
            addRow(o + step * 2, KEY_OUTPUT_FOLDER_PATH, "Path to output folder:", true, false);
            addRow(o + step * 3, KEY_SENDER_PROJECT_PATH, "Path to sender project:", true, false);
            addRow(o + step * 4, KEY_LIBSL_PATH, "Path to *.lsl file with LibSL specifications:", true, true);
            addRow(o + step * 5, KEY_GOALS, "Goals (clean test):", false, false);
            addButtons(o + step * 6);
            addTextAreas(o + step * 7, false);
        } else {
            addRow(o, KEY_AJC_PATH, "Path to ajc.bat (Windows) or ajc (other):", true, true);
            addRow(o + step, KEY_TESTED_PROJECT_PATH, "Path to tested project:", true, false);
            addRow(o + step * 2, KEY_OUTPUT_FOLDER_PATH, "Path to output folder:", true, false);
            addRow(o + step * 3, KEY_JAVA_EXE_PATH, "Path to java executable:", true, true);
            addRow(o + step * 4, KEY_SENDER_PROJECT_PATH, "Path to sender project:", true, false);
            addRow(o + step * 5, KEY_LIBSL_PATH, "Path to *.lsl file with LibSL specifications:", true, true);
            addRow(o + step * 6, KEY_MAIN_CLASS, "Main class:", false, false);
            addRow(o + step * 7, KEY_USER_CLASS_PATH, "Classpath:", false, false);
            addButtons(o + step * 8);
            addTextAreas(o + step * 9, false);
        }

        this.setVisible(true);
    }

    private void addTextAreas(int topOffset, boolean onlyLog) {
        int leftPadding = 5;
        int betweenTextAreas = 10;
        int ctrlWidth = onlyLog ? w - leftPadding * 2 : (w - leftPadding * 2 - betweenTextAreas) / 2;
        var logScrollAndTA = createScrolledTextArea("Logs:",
                topOffset, leftPadding, ctrlWidth);
        if (!onlyLog) {
            var outputScrollAndTA = createScrolledTextArea("Tested Project Output:",
                    topOffset, leftPadding + ctrlWidth + betweenTextAreas, ctrlWidth);
            outputScroll = outputScrollAndTA.getScroll();
            taOutput = outputScrollAndTA.getTextArea();
        }
        logScroll = logScrollAndTA.getScroll();
        taLog = logScrollAndTA.getTextArea();
    }

    private void addCheckBox() {
        int padding = 9;
        int cbHeight = 20;

        cbMavenGradleProject = new JCheckBox("maven- or gradle- project");
        cbMavenGradleProject.setSelected(mavenGradleProject);
        cbMavenGradleProject.setBounds(padding, padding, w - padding*2, cbHeight);
        this.add(cbMavenGradleProject);

        cbMavenGradleProject.addActionListener(ev -> {
            var pref = Preferences.userNodeForPackage(org.mariarheon.libusechecker2.Main.class);
            pref.put(KEY_MAVEN_GRADLE_PROJECT, mavenGradleProject ? "false" : "true");
            start(!mavenGradleProject);
        });
    }

    private void addRow(int offset, String key, String lblContent, boolean withFileChooser, boolean isFile) {
        int padding = 9;
        int lblHeight = 20;
        int tbHeight = 20;
        int btnWidth = 30;
        int distBetweenTbAndBtn = 7;

        int tbRowTop = offset + padding + lblHeight + 6;
        int btnLeft = w - padding - btnWidth;
        int tbWidth = withFileChooser ? btnLeft - distBetweenTbAndBtn - padding : w - padding*2;

        var lblPath = new JLabel(lblContent);
        lblPath.setBounds(padding, offset + padding, w - padding*2, lblHeight);
        this.add(lblPath);

        var tbPath = new JTextField();
        tbPath.setBounds(padding, tbRowTop, tbWidth, tbHeight);
        this.add(tbPath);
        var pref = Preferences.userNodeForPackage(org.mariarheon.libusechecker2.Main.class);
        TextFieldChangeListener.apply(tbPath, () -> pathByKey.put(key, tbPath.getText()));
        tbPath.setText(pref.get(key, ""));
        var tbChangeListener = TextFieldChangeListener.apply(tbPath, () -> pref.put(key, tbPath.getText()));

        if (withFileChooser) {
            var btnSetFile = new JButton("...");
            btnSetFile.setBounds(btnLeft, tbRowTop, btnWidth, tbHeight);
            btnSetFile.addActionListener(e -> {
                var chooser = new JFileChooser();
                if (!isFile) {
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                }
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    var filePath = chooser.getSelectedFile().getAbsolutePath();
                    tbChangeListener.turnOff();
                    tbPath.setText(filePath);
                    tbChangeListener.turnOn();
                    // for Windows: will be saved in register:
                    // \HKEY_CURRENT_USER\Software\ JavaSoft\Prefs\org.mariarheon.libusechecker2
                    pref.put(key, filePath);
                }
            });
            this.add(btnSetFile);
        }
    }

    private void addButtons(int offset) {
        int leftPadding = 5;
        int topPadding = 12;
        int btnCount = mavenGradleProject ? 6 : 5;
        int distBetweenBtns = 9;
        int btnHeight = 30;

        int btnWidth = (w - leftPadding*2 - distBetweenBtns * (btnCount - 1)) / btnCount;

        var btnArray = new JButton[btnCount];
        for (int i = 0; i < btnCount; i++) {
            btnArray[i] = new JButton();
            btnArray[i].setBounds(leftPadding + (btnWidth + distBetweenBtns) * i, offset + topPadding, btnWidth, btnHeight);
            this.add(btnArray[i]);
        }

        btnReadModel = btnArray[0];
        btnGenerateAspects = btnArray[1];
        if (mavenGradleProject) {
            btnInjectBuildFile = btnArray[2];
            btnRun = btnArray[3];
            btnStartProjectWithBuildSystem = btnArray[4];
            btnGenerateTests = btnArray[5];
        } else {
            btnCompile = btnArray[2];
            btnRun = btnArray[3];
            btnGenerateTests = btnArray[4];
        }

        btnReadModel.setText("Read Model");
        btnGenerateAspects.setText("Generate Aspects");
        btnGenerateTests.setText("Generate Tests");
        if (mavenGradleProject) {
            btnInjectBuildFile.setText("Inject build file");
            btnRun.setText("Run Retriever Server");
            btnStartProjectWithBuildSystem.setText("Start tests");
        } else {
            btnCompile.setText("Compile");
            btnRun.setText("Run");
        }

        btnReadModel.addActionListener(this::readModel);
        btnGenerateAspects.addActionListener(this::generateAspects);
        if (mavenGradleProject) {
            btnInjectBuildFile.addActionListener(this::injectBuildFile);
            btnStartProjectWithBuildSystem.addActionListener(this::startProjectWithBuildSystem);
        } else {
            btnCompile.addActionListener(this::compile);
        }
        btnRun.addActionListener(this::run);
        btnGenerateTests.addActionListener(this::generateTests);
    }

    private void generateTests(ActionEvent ev) {
        var consumer = new ConversationItemConsumer() {
            @Override
            public void start() {
                logln(timestamp() + "Kex started.");
            }

            @Override
            public void processOutput(String message) {
                outputln(timestamp() + message);
            }

            @Override
            public void finished(int exitCode) {
                if (exitCode == 0) {
                    logln(timestamp() + "Kex successfully closed.");
                } else {
                    logln(timestamp() + "Kex closed with " + exitCode + " exit code.");
                }
                logln("");
                end();
            }

            @Override
            public void error(String msg) {
                logln(timestamp() + "Error happened during conversation with Kex process: " + msg + "\n");
                end();
            }

            private void end() {
                endIt();
            }
        };
        var w = new GenerateTestsWindow(consumer);
    }

    private void startProjectWithBuildSystem(ActionEvent ev) {
        if (badSenderOrOutputFolder()) {
            return;
        }
        if (buildSystemIsRunning) {
            if (buildSystemInvoker != null) {
                logln("Trying to stop gradle...");
                buildSystemInvoker.stop();
                //btnStartProjectWithBuildSystem.setText("Start tests");
                //gradleProjectIsRunning = false;
                //logln("");
                //logln("GRADLE STOPPED");
            }
            return;
        }
        taOutput.setText("");
        logln("");
        logln("===============");
        logln("= START TESTS =");
        logln("===============");
        logln("");
        Path projectPath = Paths.get(pathByKey.get(KEY_TESTED_PROJECT_PATH)).normalize();
        Path buildGradlePath = projectPath.resolve("build.gradle").normalize();
        Path pomXmlPath = projectPath.resolve("pom.xml").normalize();
        boolean isGradle = Files.exists(buildGradlePath);
        boolean isMaven = Files.exists(pomXmlPath);
        if (!isGradle && !isMaven) {
            logln("This functionality only works for projects with build.gradle- or pom.xml- files, but the file "
                    + buildGradlePath.toString() + " or " + pomXmlPath.toString() + " was not found =(");
            return;
        }
        buildSystemIsRunning = true;
        btnStartProjectWithBuildSystem.setText("Stop tests");
        Path buildSystemPath = Paths.get(pathByKey.get(KEY_BUILD_SYSTEM_PATH));
        String goals = pathByKey.get(KEY_GOALS);
        buildSystemInvoker = new BuildSystemInvoker(isGradle, buildSystemPath, projectPath, goals);
        String projectType = isGradle ? "gradle" : "maven";
        var consumer = new ConversationItemConsumer() {
            @Override
            public void start() {
                logln(timestamp() + projectType + " started.");
            }

            @Override
            public void processOutput(String message) {
                outputln(timestamp() + message);
            }

            @Override
            public void finished(int exitCode) {
                if (exitCode == 0) {
                    logln(timestamp() + projectType + " successfully closed.");
                } else {
                    logln(timestamp() + projectType + " closed with " + exitCode + " exit code.");
                }
                logln("");
                end();
            }

            @Override
            public void error(String msg) {
                logln(timestamp() + "Error happened during conversation with " + projectType + " process: " + msg + "\n");
                end();
            }

            private void end() {
                btnStartProjectWithBuildSystem.setText("Start tests");
                buildSystemIsRunning = false;
            }
        };
        buildSystemInvoker.start(consumer);
        /*
        var t = new Thread(() -> {
            try (ProjectConnection connection = GradleConnector.newConnector()
                    .forProjectDirectory(new File(projectPath.toString()))
                    .connect()) {
                BuildLauncher build = connection.newBuild();

                //select tasks to run:
                build.forTasks("test");

                //include some build arguments:
                // build.withArguments("-i", "--project-dir", "some-project-dir");

                //configure the standard input:
                build.setStandardInput(new ByteArrayInputStream("".getBytes()));

                //in case you want the build to use java different than default:
                // build.setJavaHome(new File("/path/to/java"));

                //if your build needs crazy amounts of memory:
                // build.setJvmArguments("-Xmx2048m", "-XX:MaxPermSize=512m");

                build.addProgressListener((org.gradle.tooling.ProgressEvent progressEvent) -> {
                    SwingUtilities.invokeLater(() -> {
                        logln(progressEvent.getDescription());
                        //logln("display_name: " + progressEvent.getDisplayName());
                        //logln("event_time: " + new Date(progressEvent.getEventTime()));
                        //logln("descriptor: " + progressEvent.getDescriptor().toString());
                    });
                });

                //kick the build off:
                build.run();
            }
        });
        t.start();
        */
    }

    private void run(ActionEvent ev) {
        if (badSenderOrOutputFolder()) {
            return;
        }
        if (model == null) {
            logln("Model is not set. First, use \"Read Model\" button.\n");
            return;
        }
        if (!mavenGradleProject) {
            taOutput.setText("");
        }
        if (isRunning) {
            if (mavenGradleProject) {
                endIt();
            } else {
                invoker.stop();
            }
        } else {
            setRunning(true);
            logln(timestamp() + "Start retrieving-server...");
            var errorFilePath = Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH), "errors.bsl");
            final DataOutputStream errorFile;
            try {
                errorFile = new DataOutputStream(new FileOutputStream(errorFilePath.toFile()));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to create file: " + errorFilePath);
            }

            var retrieverConsumer = new StringConsumer() {
                @Override
                public void consume(String value) {
                    SwingUtilities.invokeLater(() -> logln(timestamp() + value));
                }
                @Override
                public void consumeError(String error, String stackTrace) {
                    SwingUtilities.invokeLater(() -> {
                        var error2 = timestamp() + error;
                        var stackTrace2 = timestamp() + stackTrace;
                        logln(error2);
                        logln(stackTrace2);
                        try {
                            errorFile.writeUTF(error2);
                            errorFile.writeUTF(stackTrace2);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            };
            var verifier = new SLVerifier((SpecModel) model, retrieverConsumer);
            retriever = new Retriever(verifier, retrieverConsumer);
            retrieverThread = new Thread(retriever);
            if (mavenGradleProject) {
                retriever.whenFinished(() -> {
                    SwingUtilities.invokeLater(() -> {
                        setRunning(false);
                        buttonsSetEnabled(true);
                        try {
                            errorFile.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        logln(timestamp() + "Retriever-server stopped");
                        logln("");
                    });
                });
            }
            buttonsSetEnabled(false);
            btnRun.setEnabled(true);
            retrieverThread.start();
            if (mavenGradleProject) {
                logln(timestamp() + "Start retriever-server...");
            } else {
                logln(timestamp() + "Start compiled program (" + Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH)).resolve("result.jar").normalize().toString() + ")...");
            }
            if (!mavenGradleProject) {
                invoker = new CompilerInvoker(
                        Paths.get(pathByKey.get(KEY_AJC_PATH)),
                        Paths.get(pathByKey.get(KEY_TESTED_PROJECT_PATH)),
                        Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH)),
                        Paths.get(pathByKey.get(KEY_JAVA_EXE_PATH)),
                        Paths.get(pathByKey.get(KEY_SENDER_PROJECT_PATH)),
                        pathByKey.get(KEY_MAIN_CLASS),
                        pathByKey.get(KEY_USER_CLASS_PATH)
                );
                invoker.start(new ConversationItemConsumer() {
                    @Override
                    public void start() {
                        logln(timestamp() + "result.jar started.");
                    }

                    @Override
                    public void processOutput(String message) {
                        outputln(timestamp() + message);
                    }

                    @Override
                    public void finished(int exitCode) {
                        if (exitCode == 0) {
                            logln(timestamp() + "result.jar successfully closed.");
                        } else {
                            logln(timestamp() + "result.jar closed with " + exitCode + " exit code.");
                        }
                        logln("");
                        end();
                    }

                    @Override
                    public void error(String msg) {
                        logln(timestamp() + "Error happened during conversation with \"result.jar\" process: " + msg + "\n");
                        end();
                    }

                    private void end() {
                        endIt();
                    }
                });
            }
        }
    }

    private void endIt() {
        setRunning(false);
        btnRun.setEnabled(false);
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        buttonsSetEnabled(true);
                        btnRun.setEnabled(true);
                        retriever.closeSocket();
                        retrieverThread.interrupt();
                        try {
                            retrieverThread.join();
                        } catch (InterruptedException interruptedException) {
                            logln(timestamp() + "Retriever thread was interrupted =(: " + interruptedException.getMessage());
                        }
                    }
                },
                1000
        );
    }

    private void injectBuildGradle(Path projectPath, Path buildGradlePath, Path outputFolderPath, Path senderProjectPath) {
        String escapedOutputFolderPath = outputFolderPath.toString().replace("\\", "\\\\");
        String escapedSenderProjectPath = senderProjectPath.toString().replace("\\", "\\\\");

        BuildGradleFile bgFile = null;
        try {
            bgFile = new BuildGradleFile(buildGradlePath.toString());
        } catch (IOException e) {
            logln("Failed to open file " + buildGradlePath);
            e.printStackTrace();
            return;
        }
        Path buildGradleOrigPath = projectPath.resolve("build.gradle.orig");
        if (!Files.exists(buildGradleOrigPath)) {
            try {
                Files.copy(buildGradlePath, buildGradleOrigPath);
            } catch (IOException e) {
                logln("Failed to back up build.gradle file =(");
                e.printStackTrace();
                return;
            }
            logln("build.gradle file was successfully backed up as build.gradle.orig");
        }
        logln("Source build.gradle:");
        logln("====================");
        logln("");
        logln(bgFile.toString());
        logln("");
        logln("build.gradle after injection:");
        logln("=============================");
        logln("");

        bgFile.addPlugin("io.freefair.aspectj.post-compile-weaving", "5.3.0");
        bgFile.addDependency("implementation", "com.google.code.gson", "gson", "2.8.6");
        bgFile.addDependency("implementation", "org.aspectj", "aspectjrt", "1.9.6");
        bgFile.addDependency("implementation", "org.aspectj", "aspectjweaver", "1.9.6");
        bgFile.addMainSrcDir();
        bgFile.addSrcDir(escapedSenderProjectPath,
                "Sender((\\\\\\\\)|(\\/))src((\\\\\\\\)|(\\/))?$");
        bgFile.addSrcDir(escapedOutputFolderPath,
                "aspect_res((\\\\\\\\)|(\\/))?$");

        logln(bgFile.toString());
        logln("");
        try {
            bgFile.save();
        } catch (IOException e) {
            logln("Failed to save modifications to build.gralde file =(");
            e.printStackTrace();
            return;
        }
        logln("build.gradle file was successfully modified =)");
    }

    private void injectPomXml(Path projectPath, Path pomXmlPath, Path outputFolderPath, Path senderProjectPath) {
        String outputFolderPathStr = outputFolderPath.toString();
        String senderProjectPathStr = senderProjectPath.toString();

        PomXmlFile pxFile = null;
        try {
            pxFile = new PomXmlFile(pomXmlPath.toString());
        } catch (Exception e) {
            logln("Failed to open file " + pomXmlPath);
            e.printStackTrace();
            return;
        }
        Path pomXmlOrigPath = projectPath.resolve("pom.xml.orig");
        if (!Files.exists(pomXmlOrigPath)) {
            try {
                Files.copy(pomXmlPath, pomXmlOrigPath);
            } catch (IOException e) {
                logln("Failed to back up pom.xml file =(");
                e.printStackTrace();
                return;
            }
            logln("pom.xml file was successfully backed up as pom.xml.orig");
        }
        int indent = 2;
        logln("Source pom.xml:");
        logln("===============");
        logln("");
        logln(pxFile.toString(indent));
        logln("");
        logln("pom.xml after injection:");
        logln("========================");
        logln("");

        pxFile.addProperties();
        pxFile.addBuildHelperMavenPlugin(outputFolderPathStr, "aspect_res((\\\\)|(\\/))?$",
                senderProjectPathStr, "Sender((\\\\)|(\\/))src((\\\\)|(\\/))?$");
        pxFile.addAspectjMavenPlugin();
        pxFile.addDependencyIfNotExist("com.google.code.gson", "gson", "2.8.6");
        pxFile.addDependencyIfNotExist("org.aspectj", "aspectjrt", "1.9.6");
        pxFile.addDependencyIfNotExist("org.aspectj", "aspectjweaver", "1.9.6");

        logln(pxFile.toString(indent));
        logln("");
        try {
            pxFile.save(indent);
        } catch (IOException e) {
            logln("Failed to save modifications to pom.xml file =(");
            e.printStackTrace();
            return;
        }
        logln("pom.xml file was successfully modified =)");
    }

    private void injectBuildFile(ActionEvent ev) {
        if (badSenderOrOutputFolder()) {
            return;
        }
        logln("");
        logln("=======================");
        logln("=  INJECT BUILD FILE  =");
        logln("=======================");
        logln("");
        if (model == null) {
            logln("Model is not set. First, use \"Read Model\" button.\n");
            return;
        }
        Path projectPath = Paths.get(pathByKey.get(KEY_TESTED_PROJECT_PATH)).normalize();
        Path buildGradlePath = projectPath.resolve("build.gradle").normalize();
        Path pomXmlPath = projectPath.resolve("pom.xml").normalize();
        boolean isGradle = Files.exists(buildGradlePath);
        boolean isMaven = Files.exists(pomXmlPath);
        if (!isGradle && !isMaven) {
            logln("This functionality only works for projects with build.gradle-file or pom.xml-file, but the file "
                    + buildGradlePath.toString() + " or "
                    + pomXmlPath.toString() + " was not found =(");
            return;
        }
        Path outputFolderPath = Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH)).normalize();
        Path senderProjectPath = Paths.get(pathByKey.get(KEY_SENDER_PROJECT_PATH)).resolve("src").normalize();
        if (isGradle) {
            logln("Determined project type: GRADLE");
            injectBuildGradle(projectPath, buildGradlePath, outputFolderPath, senderProjectPath);
        } else {
            logln("Determined project type: MAVEN");
            injectPomXml(projectPath, pomXmlPath, outputFolderPath, senderProjectPath);
        }
    }

    private void compile(ActionEvent ev) {
        if (badSenderOrOutputFolder()) {
            return;
        }
        if (model == null) {
            logln("Model is not set. First, use \"Read Model\" button.\n");
            return;
        }
        logln("Compiling tested project together with aspects and sender project...");
        var invoker = new CompilerInvoker(
                Paths.get(pathByKey.get(KEY_AJC_PATH)),
                Paths.get(pathByKey.get(KEY_TESTED_PROJECT_PATH)),
                Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH)),
                Paths.get(pathByKey.get(KEY_JAVA_EXE_PATH)),
                Paths.get(pathByKey.get(KEY_SENDER_PROJECT_PATH)),
                pathByKey.get(KEY_MAIN_CLASS),
                pathByKey.get(KEY_USER_CLASS_PATH)
        );
        buttonsSetEnabled(false);
        invoker.compile(new ConversationItemConsumer() {
            @Override
            public void start() {
                logln("ajc started.");
            }

            @Override
            public void processOutput(String message) {
                logln("ajc: " + message);
            }

            @Override
            public void finished(int exitCode) {
                if (exitCode == 0) {
                    logln("The project compiled successfully.");
                    logln(Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH)).resolve("result.jar").normalize().toString()
                            + " successfully created.");
                } else {
                    logln("The project was not compiled successfully:");
                    logln("ajc finished with " + exitCode + " exit code.");
                }
                logln("");
                buttonsSetEnabled(true);
            }

            @Override
            public void error(String msg) {
                logln("Error happened during conversation with \"ajc\" process: " + msg + "\n");
                buttonsSetEnabled(true);
            }
        });
    }

    private boolean badSenderOrOutputFolder() {
        boolean success = true;
        String senderPath = pathByKey.get(KEY_SENDER_PROJECT_PATH);
        String senderSuffix = "Sender";
        if (!senderPath.endsWith(senderSuffix) && !senderPath.endsWith(senderSuffix + "/") && !senderPath.endsWith(senderSuffix + "\\")) {
            logln("Sender path should be ended with \"Sender\" folder");
            success = false;
        }
        String outputPath = pathByKey.get(KEY_OUTPUT_FOLDER_PATH);
        String outputSuffix = "aspect_res";
        if (!outputPath.endsWith(outputSuffix) && !outputPath.endsWith(outputSuffix + "/") && !outputPath.endsWith(outputSuffix + "\\")) {
            logln("Output folder path should be ended with \"aspect_res\" folder");
            success = false;
        }
        return !success;
    }

    private void readModel(ActionEvent ev) {
        if (badSenderOrOutputFolder()) {
            return;
        }
        logln("Reading the model...");
        try (var inputStream = new BufferedInputStream(new FileInputStream(pathByKey.get(KEY_LIBSL_PATH)))) {
            var reader = new SLReader();
            model = reader.read(inputStream);
            logln("The model was successfully loaded.\n");
        } catch (Exception ex) {
            logln(ex.getMessage() + "\n");
        }
    }

    private void generateAspects(ActionEvent ev) {
        if (badSenderOrOutputFolder()) {
            return;
        }
        if (model == null) {
            logln("Model is not set. First, use \"Read Model\" button.\n");
            return;
        }
        logln("Generating aspects for interception in the output folder...");
        try {
            var outputFolderPath = pathByKey.get(KEY_OUTPUT_FOLDER_PATH);
            model.generateAspects(Paths.get(outputFolderPath));
            String outputFolderStr = Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH)).normalize().toString();
            String senderProjectStr = Paths.get(pathByKey.get(KEY_SENDER_PROJECT_PATH)).resolve("src").normalize().toString();
            logln("Aspects were generated successfully in \"" + outputFolderPath + "\"");
            logln("");
            logln("For maven project add the following in <project> in pom.xml: (then use 'compile'-goal and start the project)");
            logln("============================================================");
            logln("");
            logln("    <properties>");
            logln("        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>");
            logln("        <maven.compiler.source>1.8</maven.compiler.source>");
            logln("        <maven.compiler.target>1.8</maven.compiler.target>");
            logln("    </properties>");
            logln("");
            logln("    <build>");
            logln("        <plugins>");
            logln("            <plugin>");
            logln("                <groupId>org.codehaus.mojo</groupId>");
            logln("                <artifactId>build-helper-maven-plugin</artifactId>");
            logln("                <version>3.2.0</version>");
            logln("                <executions>");
            logln("                    <execution>");
            logln("                        <phase>generate-sources</phase>");
            logln("                        <goals>");
            logln("                            <goal>add-source</goal>");
            logln("                        </goals>");
            logln("                        <configuration>");
            logln("                            <sources>");
            logln(String.format("                                <source>%s</source>", outputFolderStr));
            logln(String.format("                                <source>%s</source>", senderProjectStr));
            logln("                            </sources>");
            logln("                        </configuration>");
            logln("                    </execution>");
            logln("                </executions>");
            logln("            </plugin>");
            logln("            <plugin>");
            logln("                <groupId>se.haleby.aspectj</groupId>");
            logln("                <artifactId>aspectj-maven-plugin</artifactId>");
            logln("                <configuration>");
            logln("                    <source>${maven.compiler.source}</source>");
            logln("                    <target>${maven.compiler.target}</target>");
            logln("                    <complianceLevel>${maven.compiler.target}</complianceLevel>");
            logln("                    <encoding>${project.build.sourceEncoding}</encoding>");
            logln("                </configuration>");
            logln("                <version>1.12.7</version>");
            logln("                <executions>");
            logln("                    <execution>");
            logln("                        <goals>");
            logln("                            <goal>compile</goal>");
            logln("                        </goals>");
            logln("                    </execution>");
            logln("                </executions>");
            logln("            </plugin>");
            logln("        </plugins>");
            logln("    </build>");
            logln("");
            logln("    <dependencies>");
            logln("        <dependency>");
            logln("            <groupId>com.google.code.gson</groupId>");
            logln("            <artifactId>gson</artifactId>");
            logln("            <version>2.8.6</version>");
            logln("        </dependency>");
            logln("        <dependency>");
            logln("            <groupId>org.aspectj</groupId>");
            logln("            <artifactId>aspectjrt</artifactId>");
            logln("            <version>1.9.6</version>");
            logln("        </dependency>");
            logln("        <dependency>");
            logln("            <groupId>org.aspectj</groupId>");
            logln("            <artifactId>aspectjweaver</artifactId>");
            logln("            <version>1.9.6</version>");
            logln("        </dependency>");
            logln("    </dependencies>");
            logln("");
            logln("For gralde project add the following in your build.gradle (OR USE \"INJECT BUILD FILE\" BUTTON (recommended)):");
            logln("==========================================================");
            logln("");
            logln("plugins {");
            logln("    id \"io.freefair.aspectj.post-compile-weaving\" version \"5.3.0\"");
            logln("}");
            logln("");
            logln("dependencies {");
            logln("    implementation 'com.google.code.gson:gson:2.8.6'");
            logln("    implementation group: 'org.aspectj', name: 'aspectjrt', version: '1.9.6'");
            logln("    implementation group: 'org.aspectj', name: 'aspectjweaver', version: '1.9.6'");
            logln("}");
            logln("");
            logln("sourceSets {");
            logln("    main {");
            logln("        java {");
            logln("            srcDirs 'src/main/java'");
            logln(String.format("            srcDirs '%s'", outputFolderStr.replace("\\", "\\\\")));
            logln(String.format("            srcDirs '%s'", senderProjectStr.replace("\\", "\\\\")));
            logln("        }");
            logln("    }");
            logln("}");
            logln("");
            /*
            logln("IN ORDER TO RUN TESTS for gralde project add the following in your build.gradle:");
            logln("===============================================================================");
            logln("");
            logln("plugins {");
            logln("    id \"io.freefair.aspectj.post-compile-weaving\" version \"5.3.0\"");
            logln("}");
            logln("");
            logln("dependencies {");
            logln("    testImplementation 'com.google.code.gson:gson:2.8.6'");
            logln("    testImplementation group: 'org.aspectj', name: 'aspectjrt', version: '1.9.6'");
            logln("    testImplementation group: 'org.aspectj', name: 'aspectjweaver', version: '1.9.6'");
            logln("}");
            logln("");
            logln("sourceSets {");
            logln("    test {");
            logln("        java {");
            logln("            srcDirs 'src/test/java'");
            logln(String.format("            srcDirs '%s'", outputFolderStr.replace("\\", "\\\\")));
            logln(String.format("            srcDirs '%s'", senderProjectStr.replace("\\", "\\\\")));
            logln("        }");
            logln("    }");
            logln("}");
            logln("");
             */
        } catch (IOException ex) {
            logln(ex.getMessage() + "\n");
        }
    }

    private final SimpleDateFormat dFormat = new SimpleDateFormat("HH:mm:ss");

    private String timestamp() {
        return dFormat.format(new Date()) + " ";
    }

    private void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
        if (isRunning) {
            btnRun.setText(mavenGradleProject ? "Stop Retriever Server" : "Stop");
        } else {
            btnRun.setText(mavenGradleProject ? "Run Retriever Server" : "Run");
        }
    }

    private void buttonsSetEnabled(boolean enabled) {
        cbMavenGradleProject.setEnabled(enabled);
        btnReadModel.setEnabled(enabled);
        btnGenerateAspects.setEnabled(enabled);
        if (!mavenGradleProject) {
            btnCompile.setEnabled(enabled);
        }
        btnRun.setEnabled(enabled);
    }

    private ScrollAndTextArea createScrolledTextArea(String caption, int topOffset, int leftOffset, int ctrlWidth) {
        int bottomPadding = 5;
        int lblHeight = 20;

        int taTop = topOffset + lblHeight + 6;
        int taHeight = h - taTop - bottomPadding - this.getJMenuBar().getHeight();

        var lblLog = new JLabel(caption);
        lblLog.setBounds(leftOffset, topOffset, ctrlWidth, lblHeight);
        this.add(lblLog);

        var ta = new JTextArea("");
        ta.setEditable(false);
        ta.setLineWrap(true);

        var scroll = new JScrollPane(ta);
        scroll.setBounds(leftOffset, taTop, ctrlWidth, taHeight);
        this.add(scroll);

        return new ScrollAndTextArea(scroll, ta);
    }

    private Date lastUpdated = new Date();
    private String virtualLog = "";
    private final OnceFuture onceFuture = new OnceFuture(() -> {
        SwingUtilities.invokeLater(this::applyVirtualLog);
    }, 500, TimeUnit.MILLISECONDS, true);

    private void applyVirtualLog() {
        taLog.setText(virtualLog);
        JScrollBar vertical = logScroll.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    private void log2(String message) {
        onceFuture.scheduleOrReschedule();
        virtualLog += message;
        int maxSymbolsInLog = 5000;
        if (virtualLog.length() > maxSymbolsInLog) {
            virtualLog = virtualLog.substring(virtualLog.length() - maxSymbolsInLog);
        }
        Date now = new Date();
        int timeout = 300 /* ms */;
        if (now.getTime() - lastUpdated.getTime() >= timeout) {
            lastUpdated = now;
            applyVirtualLog();
        }
        /*
        taLog.setText(taLog.getText() + message);
        trunkTextArea(taLog);
         */
    }

    private void output2(String message) {
        taOutput.setText(taOutput.getText() + message);
        trunkTextArea(taOutput);
        JScrollBar vertical = outputScroll.getVerticalScrollBar();
        vertical.setValue( vertical.getMaximum() );
    }

    private void log(String message) {
        try {
            Files.writeString(Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH), "main.log"), message,
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log2(message);
    }

    private void logln(String message) {
        log(message + "\n");
    }

    private void output(String message) {
        try {
            Files.writeString(Paths.get(pathByKey.get(KEY_OUTPUT_FOLDER_PATH), "output.log"), message,
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        output2(message);
    }

    private void outputln(String message) {
        output(message + "\n");
    }

    final int SCROLL_BUFFER_SIZE = 100;
    private void trunkTextArea(JTextArea txtWin)
    {
        int numLinesToTrunk = txtWin.getLineCount() - SCROLL_BUFFER_SIZE;
        if(numLinesToTrunk > 0)
        {
            try
            {
                int posOfLastLineToTrunk = txtWin.getLineEndOffset(numLinesToTrunk - 1);
                txtWin.replaceRange("",0,posOfLastLineToTrunk);
            }
            catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }
}

