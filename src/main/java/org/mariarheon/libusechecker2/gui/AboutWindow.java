package org.mariarheon.libusechecker2.gui;

import javax.swing.*;
import java.awt.*;

/**
 *
 */
public class AboutWindow extends JFrame {
    public AboutWindow() {
        int w = 600;
        int h = 400;
        //int padding = 5;

        var ta = new JTextArea("");
        ta.setText("Hello\nworld");
        ta.setEditable(false);
        ta.setLineWrap(true);

        var scroll = new JScrollPane(ta);
        //scroll.setBounds(padding, padding, w - padding*2, h - padding*2);
        this.add(scroll);
        this.setVisible(true);
        this.getContentPane().setPreferredSize(new Dimension(w, h));
        this.setTitle("Library Specification Verifier: How to Use");
        this.pack();
    }


}
