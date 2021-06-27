package org.mariarheon.libusechecker2;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Util {
    public static String interruptibleReadLine(BufferedReader reader)
            throws InterruptedException, IOException {
        Pattern line = Pattern.compile("^(.*)\\R");
        Matcher matcher;
        boolean interrupted;

        StringBuilder result = new StringBuilder();
        int chr = -1;
        do {
            if (reader.ready()) {
                chr = reader.read();
                if (chr == -1) {
                    if (result.toString().isEmpty()) {
                        return null;
                    } else {
                        var m = line.matcher(result.toString());
                        return (m.matches() ? m.group(1) : result.toString());
                    }
                } else {
                    result.append((char) chr);
                }
            }
            matcher = line.matcher(result.toString());
            interrupted = Thread.interrupted(); // resets flag, call only once
            if (false) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
        } while (!interrupted && !matcher.matches());
        if (interrupted) {
            throw new InterruptedException();
        }
        return (matcher.matches() ? matcher.group(1) : "");
    }

    public static String insertAtIndex(String sourceString, String insertedString, int index) {
        return sourceString.substring(0, index) + insertedString + sourceString.substring(index);
    }

    public static void printStackTrace(StringConsumer consumer, String[] stackTrace) {
        consumer.consume(combinedStackTrace(stackTrace));
    }

    public static String combinedStackTrace(String[] stackTrace) {
        return String.join("\n", stackTrace);
    }
}
