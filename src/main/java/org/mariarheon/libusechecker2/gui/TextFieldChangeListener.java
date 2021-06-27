package org.mariarheon.libusechecker2.gui;

import org.w3c.dom.Text;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TextFieldChangeListener implements DocumentListener {
    private final MyChangeListener myChangeListener;
    private boolean activated;

    private TextFieldChangeListener(MyChangeListener myChangeListener) {
        this.myChangeListener = myChangeListener;
        this.activated = true;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        if (activated) {
            myChangeListener.changeHappened();
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        if (activated) {
            myChangeListener.changeHappened();
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        if (activated) {
            myChangeListener.changeHappened();
        }
    }

    public static TextFieldChangeListener apply(JTextField textField, MyChangeListener myChangeListener) {
        var res = new TextFieldChangeListener(myChangeListener);
        textField.getDocument().addDocumentListener(res);
        return res;
    }

    public void turnOn() {
        activated = true;
    }

    public void turnOff() {
        activated = false;
    }
}
