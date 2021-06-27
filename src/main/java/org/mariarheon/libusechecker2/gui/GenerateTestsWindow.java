package org.mariarheon.libusechecker2.gui;

import org.mariarheon.libusechecker2.ConversationItemConsumer;
import org.mariarheon.libusechecker2.TestGeneratorInvoker;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class GenerateTestsWindow extends JFrame {
    private JTextField tfKexPath;
    private JTextArea cmdTextArea;
    private ConversationItemConsumer consumer;

    public GenerateTestsWindow(ConversationItemConsumer consumer) {
        this.consumer = consumer;
        int padding = 15;
        int w = 800;
        int h = 500;
        this.setLayout(null);
        int height = 30;
        int top = 15;

        var lblKexPath = new JLabel("Path to Kex project (folder) (your jar-file should be copied there):");
        lblKexPath.setBounds(padding, top, w - padding*2, height);
        this.add(lblKexPath);

        top = 55;
        tfKexPath = new JTextField();
        tfKexPath.setText("path/to/kex/folder");
        tfKexPath.setBounds(padding, top, w - padding*2, height);
        this.add(tfKexPath);

        top = 100;
        var lblCmd = new JLabel("Command for generating tests:");
        lblCmd.setBounds(padding, top, w - padding*2, height);
        this.add(lblCmd);

        top = 140;
        cmdTextArea = new JTextArea("");
        cmdTextArea.setText(getCustomText());
        var cmdScroll = new JScrollPane(cmdTextArea);
        cmdScroll.setBounds(padding, top, w - padding*2, 300);
        this.add(cmdScroll);

        top = 450;
        var btnGenTests = new JButton("Generate Tests");
        btnGenTests.setBounds(padding, top, w - padding*2, height);
        btnGenTests.addActionListener(this::genTests);
        this.add(btnGenTests);

        this.setVisible(true);
        this.getContentPane().setPreferredSize(new Dimension(w, h));
        this.setTitle("Library Specification Verifier: Generate Tests");
        this.pack();
    }

    private String getCustomText() {
        String text = "path/to/java\n";
        text += "-Xmx4096m\n";
        text += "-Djava.security.manager\n";
        text += "-Djava.security.policy==kex.policy\n";
        text += "-jar\n";
        text += "kex-runner\\target\\kex-runner-0.0.1-jar-with-dependencies.jar\n";
        text += "--classpath\n";
        text += "Example.jar\n";
        text += "--target\n";
        text += "org.example.fuzzing.Some2\n";
        text += "--output\n";
        text += "temp\n";
        text += "--log\n";
        text += "test.log";
        return text;
    }

    private void genTests(ActionEvent ev) {
        var testGeneratorInvoker = new TestGeneratorInvoker();
        testGeneratorInvoker.start(consumer, tfKexPath.getText(), Arrays.asList(cmdTextArea.getText().split("\n").clone()));
    }
}
