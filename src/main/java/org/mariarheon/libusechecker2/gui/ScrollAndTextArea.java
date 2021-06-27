package org.mariarheon.libusechecker2.gui;

import javax.swing.*;

public class ScrollAndTextArea {
    private JScrollPane scroll;
    private JTextArea textArea;

    public ScrollAndTextArea(JScrollPane scroll, JTextArea textArea) {
        this.scroll = scroll;
        this.textArea = textArea;
    }

    public JScrollPane getScroll() {
        return scroll;
    }

    public JTextArea getTextArea() {
        return textArea;
    }
}
