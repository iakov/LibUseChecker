package org.mariarheon.libusechecker2.gui;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ErrorsWindow extends JFrame {
    private List<String> errors;
    private List<String> stackTraces;

    public ErrorsWindow(Path errorFilePath) {
        errors = new ArrayList<String>();
        stackTraces = new ArrayList<String>();
        int w = 1000;
        int h = 700;
        //int padding = 5;

        var stTextArea = new JTextArea("");
        stTextArea.setText("something");
        stTextArea.setEditable(false);
        stTextArea.setLineWrap(true);

        int padding = 15;

        var stScroll = new JScrollPane(stTextArea);
        stScroll.setBounds(w/2 + padding, padding, w/2 - padding*2, h - padding*2);

        var errorsJList = new JList<String>();
        errorsJList.setCellRenderer(new MyCellRenderer(w/2 - padding*2 - 150));
        errorsJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        var errorsScroll = new JScrollPane(errorsJList);
        errorsScroll.setBounds(padding, padding, w/2 - padding*2, h - padding*2);

        errorsJList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int ind = errorsJList.getSelectedIndex();
                String errorVal;
                if (ind < 0) {
                    errorVal = "";
                } else {
                    errorVal = stackTraces.get(ind);
                }
                stTextArea.setText(errorVal);
            }
        });

        this.setLayout(null);
        this.add(errorsScroll);
        this.add(stScroll);
        this.setVisible(true);
        this.getContentPane().setPreferredSize(new Dimension(w, h));
        this.setTitle("Library Specification Verifier: Errors");
        this.pack();

        DataInputStream errorsFile = null;
        try {
            errorsFile = new DataInputStream(new FileInputStream(errorFilePath.toFile()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (errorsFile == null) {
            return;
        }
        while (true) {
            try {
                String error = errorsFile.readUTF();
                String stackTrace = errorsFile.readUTF();
                errors.add(error);
                stackTraces.add(stackTrace);
            } catch (EOFException e) {
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        String[] errorsAr = new String[errors.size()];
        errors.toArray(errorsAr);
        errorsJList.setListData(errorsAr);
    }


}

class MyCellRenderer extends DefaultListCellRenderer {
    public static final String HTML_1 = "<html><body style='border-bottom: 1px solid #ccc; padding: 5px 0; width: ";
    public static final String HTML_2 = "px'>";
    public static final String HTML_3 = "</html>";
    private final int width;

    public MyCellRenderer(int width) {
        this.width = width;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        String text = HTML_1 + width + HTML_2 + value.toString() + HTML_3;
        return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
    }

}