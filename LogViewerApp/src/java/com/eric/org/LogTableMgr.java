/*
 * Copyright (c) 2017. Eric Niu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eric.org;

import com.eric.org.Util.PlugInMgr;
import com.eric.org.util.LogLine;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class LogTableMgr {
    private LogModel lm;
    private JDialog waitingDlg;

    public LogTable getLogTable() {
        return lt;
    }

    public com.eric.org.LogTableListener logchangeListener = null;
    private LogTable lt;

    private PlugInMgr pluginMgr = new PlugInMgr();

    private LogModel getLogModel() {
        return lm;
    }

    public ListSelectionModel getSelectionModel() {
        return lt.getSelectionModel();
    }

    public JScrollPane createLogView() {
        //Log Table
        lm = new LogModel(this);
        lt = new LogTable(lm);

        JScrollPane logViewPanel = new JScrollPane(lt, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        return logViewPanel;
    }

    public void applyFilter() {
        lm.applyFilter();
    }

    public void tangleFilter() {
        lm.hideShowFilter();
    }

    public void mergeLogFile(File otherfile) {
        loadLogFile(otherfile, true);
    }

    //Param b = ture means the file need merged to the present
    public void loadLogFile(File file, boolean b) {
        waitingDlg = new JDialog();

        JPanel panel = new JPanel();

        JLabel label = new JLabel("Loading...");
        panel.add(label);

        ClassLoader cldr = this.getClass().getClassLoader();

        try {
            java.net.URL imageURL = cldr.getResource("loading.gif");
            assert imageURL != null;
            ImageIcon loadingIcon = new ImageIcon(new ImageIcon(imageURL).getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));
            JLabel iconLabel = new JLabel();
            iconLabel.setIcon(loadingIcon);
            loadingIcon.setImageObserver(iconLabel);
            panel.add(iconLabel);
        } catch (Exception e) {

        }

        waitingDlg.add(panel);
        waitingDlg.setAlwaysOnTop(true);

        waitingDlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        waitingDlg.setSize(300, 75);
        waitingDlg.setUndecorated(true);

        Toolkit toolkit = waitingDlg.getToolkit();
        Dimension size = toolkit.getScreenSize();

        waitingDlg.setLocation(size.width / 2 - waitingDlg.getWidth() / 2,
                size.height / 2 - waitingDlg.getHeight() / 2);
        waitingDlg.setVisible(true);

        boolean isMerge = b;
        if(!isMerge) {
            lm.reset();
            LoadLogFileWorker lw = new LoadLogFileWorker(file, lm);
            lw.execute();
        }else {
            MergeLogFileWorker mw = new MergeLogFileWorker(file, lm);
            mw.execute();
        }

    }

    public boolean findWord(String findWords, boolean caseSensitive, boolean forward) {
        int startLineNumber = 0;
        int[] selectedRow = getLogTable().getSelectedRows();
        if (selectedRow.length > 0) {
            if (forward)
                startLineNumber = selectedRow[0] + 1;
            else
                startLineNumber = selectedRow[0] - 1;
        } else {
            if (forward)
                startLineNumber = 0;
            else
                startLineNumber = getLogTable().getRowCount();
        }
        return getLogModel().findAndScrollToWords(findWords, caseSensitive, forward, startLineNumber);
    }

    public void copySelectionToClipboard() {
        String selectedData = "";


        int[] selectedRow = getLogTable().getSelectedRows();

        for (int aSelectedRow : selectedRow) {
            selectedData += getLogTable().getValueAt(aSelectedRow, 0) + "\n";
        }
        System.out.println("Selected Log are: " + selectedData);

        StringSelection stringSelection = new StringSelection(selectedData);
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);
    }

    public PlugInMgr getPlugInMgr() {
        return pluginMgr;
    }

    public void addLogContentChangeListener(com.eric.org.LogTableListener logchangeListener) {
        this.logchangeListener = logchangeListener;
    }

    private class LoadLogFileWorker extends SwingWorker<TableModel, String> {

        private final File file;
        private final LogModel model;

        private LoadLogFileWorker(File file, LogModel model) {
            this.file = file;
            this.model = model;
        }

        @Override
        protected TableModel doInBackground() throws Exception {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s;
            while ((s = br.readLine()) != null) {
                publish(s);
            }
            return model;
        }

        @Override
        protected void process(List<String> chunks) {
            LogLine prev = null;
            for (String s : chunks) {
                LogLine sl = new LogLine(s);

                model.addRow(sl);
            }
        }

        @Override
        protected void done() {
            waitingDlg.setVisible(false);
            waitingDlg = null;
        }
    }

    private class MergeLogFileWorker extends SwingWorker<TableModel, String> {

        private final File file;
        private final LogModel model;

        private MergeLogFileWorker(File file, LogModel model) {
            this.file = file;
            this.model = model;
        }

        @Override
        protected TableModel doInBackground() throws Exception {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s;
            while ((s = br.readLine()) != null) {
                publish(s);
            }
            return model;
        }

        @Override
        protected void process(List<String> chunks) {
            LogLine prev = null;
            for (String s : chunks) {
                LogLine sl = new LogLine(s);
                model.insertRow(sl);
            }
        }

        @Override
        protected void done() {
            waitingDlg.setVisible(false);
            waitingDlg = null;
        }
    }
}
