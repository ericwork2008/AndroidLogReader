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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.*;
import java.util.List;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;

/**
 * LogTableMgr used to coordinate LogTable and LogModel to show the log in the following processes:
 *  1) Load Log
 *  2) Merge Log
 * @see LogTable
 * @see LogModel
 */
public class LogTableMgr {
    private JDialog waitingDlg;

    public LogTableListener mLogchangeListener = null;
    private LogModel lm;
    private LogTable lt;
    private JScrollPane logViewPanel;

    public LogTable getLogTable() {
        return lt;
    }
    private LogModel getLogModel() {
        return lm;
    }

    public JScrollPane createLogView() {
        //Log Table
        lm = new LogModel(this);
        lt = new LogTable(lm);

        logViewPanel = new JScrollPane(lt);
//        logViewPanel.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
        logViewPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        return logViewPanel;
    }
    public void setupAdjListener(){
        //Don't set the Adj Listener when capturing log.
        if(!isCapturingLog)
            logViewPanel.getVerticalScrollBar().addAdjustmentListener(adjListener);
    }
    public void clearAdjListener() {
        logViewPanel.getVerticalScrollBar().removeAdjustmentListener(adjListener);
    }
    AdjustmentListener adjListener = new AdjustmentListener(){
        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            lt.resizeVisables();
        }
    };
    /**
     * Apply Filter to log
     */
    public void applyFilter() {
        lm.applyFilter();
    }

    /**
     * Show and hide the log with filter
     */
    public void tangleFilter() {
        lm.hideShowFilter();
    }

    public void mergeLogFile(File[] files) {
        JFrame parentFrame = new JFrame();
        String logDirStr = PreferenceSetting.getLogDir();

        File logDir = new File(logDirStr);

        JFileChooser fileChooser = new JFileChooser();
        if (logDir.isDirectory() && logDir.canRead()) {
            fileChooser.setCurrentDirectory(logDir);
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Specify a file to save");

        fileChooser.setSelectedFile(new File("merged.txt"));
        int userSelection = fileChooser.showSaveDialog(parentFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            System.out.println("Save as file: " + fileToSave.getAbsolutePath());
            String filePath = fileChooser.getCurrentDirectory().getAbsolutePath();

            PreferenceSetting.setLogDir(filePath);
            MergeLogFileWorker mw = new MergeLogFileWorker(files, fileToSave, lm);
            mw.execute();
        }
    }

    public void loadLogFile(File file) {
        needSaveCaptureLog = false;

        waitingDlg = new JDialog();
        waitingDlg.setTitle("Loading...");

        JProgressBar progressBar;
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        waitingDlg.add(BorderLayout.CENTER, progressBar);

        ClassLoader cldr = this.getClass().getClassLoader();

        waitingDlg.setAlwaysOnTop(true);

        waitingDlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        waitingDlg.setSize(300, 75);
        waitingDlg.setUndecorated(true);

        Toolkit toolkit = waitingDlg.getToolkit();
        Dimension size = toolkit.getScreenSize();

        waitingDlg.setLocation(size.width / 2 - waitingDlg.getWidth() / 2,
                size.height / 2 - waitingDlg.getHeight() / 2);
        waitingDlg.setVisible(true);
        waitingDlg.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        lm.reset();
        LoadLogFileWorker lw = new LoadLogFileWorker(file, progressBar, lm);
        lw.execute();
    }
    public void captureLog() {
        if(isAutoCaptureLog) {
            needSaveCaptureLog = true;
            lm.reset();
            CaptureLogWorker lw = new CaptureLogWorker(lm);
            lw.execute();
        }
    }

    static boolean isAutoCaptureLog = true;
    static boolean isCapturingLog = false;

    public void  stopCaptureLog() {
        isCapturingLog = false;
    }

    boolean needSaveCaptureLog = false;
    public void saveCapturedLog() {
        if(!needSaveCaptureLog)
            return;

        String logDirStr = PreferenceSetting.getLogDir();

        File logDir = new File(logDirStr);

        JFileChooser fileChooser = new JFileChooser();
        if (logDir.isDirectory() && logDir.canRead()) {
            fileChooser.setCurrentDirectory(logDir);
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Specify a file to save");

        fileChooser.setSelectedFile(new File("adblog.txt"));
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            System.out.println("Save as file: " + fileToSave.getAbsolutePath());
            String filePath = fileChooser.getCurrentDirectory().getAbsolutePath();

            PreferenceSetting.setLogDir(filePath);
            lm.saveCapturedLog(fileToSave);
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
    private PlugInMgr pluginMgr = new PlugInMgr();

    public PlugInMgr getPlugInMgr() {
        return pluginMgr;
    }

    public void addLogContentChangeListener(com.eric.org.LogTableListener logchangeListener) {
        this.mLogchangeListener = logchangeListener;
    }
    private class CaptureLogWorker extends SwingWorker<TableModel, String> {
        private final LogModel model;

        private CaptureLogWorker(LogModel model) {
            this.model = model;
        }

        @Override
        protected TableModel doInBackground() {
            isCapturingLog = true;
            model.reset();
            try {
                String adbPath =  LogViewerApp.adbPath;
                String commandArray= adbPath + " logcat -b all";
                Process process = Runtime.getRuntime().exec(commandArray);
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String s = br.readLine();
                while (true && isCapturingLog) {
                    if(s!=null) publish(s);
                    s = br.readLine();
                }
            } catch (IOException e) {
            }

            return model;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String s : chunks) {
                LogLine sl = new LogLine(s);
                model.addRow(sl);
            }
            model.fireTableDataChanged();
            lt.resizeVisables();
        }

        @Override
        protected void done() {
            isCapturingLog = false;

            setupAdjListener();
        }
    }

    private class LoadLogFileWorker extends SwingWorker<TableModel, String> {
        private JProgressBar progressBar;
        private int readedLineCount=0;
        private int lineCount=0;
        private final File file;
        private final LogModel model;

        private LoadLogFileWorker(File file, JProgressBar pb, LogModel model) {
            this.file = file;
            this.model = model;
            this.progressBar = pb;
        }

        @Override
        protected TableModel doInBackground() throws Exception {
            //Get file lines count
            LineNumberReader reader = null;
            try {
                reader = new LineNumberReader(new FileReader(file));
                while ((reader.readLine()) != null);
                lineCount = reader.getLineNumber();
            } catch (Exception ex) {
                lineCount = 100;
            } finally {
                if(reader != null)
                    reader.close();
            }

            BufferedReader br = new BufferedReader(new FileReader(file));
            String s;

            while ((s = br.readLine()) != null) {
                publish(s);
                readedLineCount++;
            }
            return model;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String s : chunks) {
                LogLine sl = new LogLine(s);
                model.addRow(sl);
            }
            model.fireTableDataChanged();
            progressBar.setValue(readedLineCount*100/lineCount);
        }

        @Override
        protected void done() {
            waitingDlg.setVisible(false);
            waitingDlg.setCursor(null);
            waitingDlg = null;
            setupAdjListener();
        }
    }

    private class MergeLogFileWorker extends SwingWorker<TableModel, String> {
        private final File[] files;
        private final File toSave;
        private final LogModel model;

        private MergeLogFileWorker(File[] files, File fileToSave, LogModel model) {
            this.files = files;
            this.toSave = fileToSave;
            this.model = model;
        }

        @Override
        protected TableModel doInBackground() throws Exception {
            BufferedReader[] brs = new BufferedReader[files.length];
            for(int index=0; index<files.length; index++){
                brs[index]= new BufferedReader(new FileReader(files[index]));
            }
            FileWriter fileWriter = new FileWriter(toSave);

            int INDEX_MAX = files.length;
            int workingIndex = Integer.MAX_VALUE;
            boolean isFinished[] = new boolean[INDEX_MAX];
            String[] farStrings = new String[INDEX_MAX];
            long[] farTs = new long[INDEX_MAX];

            //Initialization
            for(int i=0; i<INDEX_MAX;i++) {
                isFinished[i] = false;
            }

            //
            while(true) {
                //Read the lines need read
                if(workingIndex==Integer.MAX_VALUE){
                    for(int i=0; i<INDEX_MAX;i++){
                        if(!isFinished[i]) {
                            farStrings[i] = brs[i].readLine();
                            if (farStrings[i] == null) {
                                isFinished[i] = true;
                            }
                        }
                    }
                }else if(workingIndex>=0 && workingIndex<INDEX_MAX) {
                    farStrings[workingIndex]=brs[workingIndex].readLine();
                    if(farStrings[workingIndex]==null){
                        isFinished[workingIndex]=true;
                    }
                }

                //If all file reach the end, break
                boolean finishall = true;
                for(int j=0; j<INDEX_MAX; j++) {
                    if(!isFinished[j])
                        finishall = false;
                }
                if(finishall)
                    break;

                //Parse all head lines to get TS value
                for(int i=0; i<INDEX_MAX; i++) {
                    if(farStrings[i]!=null) {
                        farTs[i] = getTS(farStrings[i]);
                    } else {
                        farTs[i]=Long.MAX_VALUE;
                    }
                    if(workingIndex == Integer.MAX_VALUE)
                        workingIndex = i;
                }
                //Find the earlier line index.
                for(int j=0; j<INDEX_MAX; j++) {
                    if(farTs[workingIndex]>(farTs[j])) {
                        workingIndex = j;
                    }
                }
                //Write the earlier line to target file.
                if(workingIndex>=0 && workingIndex<INDEX_MAX) {
                    if(farStrings[workingIndex]==null)
                        System.out.println("ERROR");
                    fileWriter.write(farStrings[workingIndex]);
                    fileWriter.write('\n');
                }
            }
            //Close all files
            for(int i=0; i<=INDEX_MAX;i++) {
                brs[i].close();
            }
            fileWriter.close();
            return model;
        }

        @Override
        protected void process(List<String> chunks) {
        }

        @Override
        protected void done() {
            JOptionPane.showConfirmDialog(null,"Saved to "+ toSave,
                    "Saved", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
        }

        private long getTS(String line) {
            long ts = 0;

            if (line.length() <= 33) return ts;

            // Read time stamp
            try {
                int month = Integer.parseInt(line.substring(0, 2));
                int day = Integer.parseInt(line.substring(3, 5));
                int hour = Integer.parseInt(line.substring(6, 8));
                int min = Integer.parseInt(line.substring(9, 11));
                int sec  = Integer.parseInt(line.substring(12, 14));
                int ms = Integer.parseInt(line.substring(15, 18));
                ts = month;
                ts = ts * 31 + day;
                ts = ts * 24 + hour;
                ts = ts * 60 + min;
                ts = ts * 60 + sec;
                ts = ts * 1000 + ms;
            } catch (NumberFormatException nfe) { }
            return ts;
        }

    }
}
