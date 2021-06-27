package org.mariarheon.libusechecker2;

import org.jvnet.winp.WinProcess;

import javax.swing.*;
import java.io.*;

public class ConversationWithTheProcess extends Thread {
    private final ProcessBuilder builder;
    private final ConversationItemConsumer consumer;
    private Process process;

    public ConversationWithTheProcess(ProcessBuilder builder, ConversationItemConsumer consumer) {
        this.builder = builder;
        this.consumer = consumer;
    }

    public void stopProcess() {
        if (process != null) {
            var winProcess = new WinProcess(process);
            winProcess.killRecursively();
            // process.destroyForcibly();
        }
    }

    @Override
    public void run() {
        try {
            process = builder.start();
        } catch (IOException e) {
            final String msg = e.getMessage();
            SwingUtilities.invokeLater(() -> consumer.error(msg));
            return;
        }
        var hook = new Thread(() -> {
            if (process != null) {
                var winProcess = new WinProcess(process);
                winProcess.killRecursively();
            }
        });
        Runtime.getRuntime().addShutdownHook(hook);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            writer.close();

            String line;
            // while ((line = Util.interruptibleReadLine(reader)) != null) {
            while ((line = reader.readLine()) != null) {
                final String theLine = line;
                SwingUtilities.invokeLater(() -> consumer.processOutput(theLine));
            }

            int exitCode = process.waitFor();
            SwingUtilities.invokeLater(() -> consumer.finished(exitCode));
        } catch (IOException | InterruptedException e) {
            final String msg = e.getMessage();
            SwingUtilities.invokeLater(() -> consumer.error(msg));
        } finally {
            Runtime.getRuntime().removeShutdownHook(hook);
            process.destroy();
            process = null;
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    /* nothing */
                }
            }
        }
    }
}
