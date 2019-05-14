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

import com.eric.org.Util.RenderLine;
import com.eric.org.config.ConfigInfo;
import com.eric.org.config.FilterConfigMgr;
import com.eric.org.util.LogLine;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

public class LogModel extends AbstractTableModel {
    private static JProgressBar progressBar = null;
    private static JDialog progressDlg = null;

    //Filter can be reset when filter in applying progress
    private static volatile boolean resetFilterProgress = false;

    //Remember previous selection
    private static int previousSelectedRowNumInFullLogLines = 0;
    private static int previousSelectedScreenfocusRowNum = 0;
    private static int distance;//previous selection distance to the top

    //Hide or unhide Log
    private static boolean mHideLine = false;

    private com.eric.org.LogTableMgr owner;

    //Synchronized operations on showingLineMap&fullLogLines
    /**
     *  Full Log lines  --Applied Filter--> Filtered Lines  --> Show on the screen
     */
    //Filtered Lines Row <-> the row in the full log Lines
    private ConcurrentHashMap<Integer, Integer> showingLineMap = new ConcurrentHashMap<>();

    /**
     * Full Log Lines
     */
    private Vector<RenderLine> fullLogLines = new Vector<>();

    public LogModel(com.eric.org.LogTableMgr logTableMgr) {
        owner = logTableMgr;
    }

    private static int filteredRowNo = 0;
    private int mLn = 0; //counter

    public void setDistance(int distance) {
        LogModel.distance = distance;
    }

    public void setSelectedOriginalLineNum(int focusLineNum) {
        LogModel.previousSelectedRowNumInFullLogLines = focusLineNum;
    }

    public synchronized int screenToOriginalNumber(int screenNum) {
        return showingLineMap.get(screenNum);
    }

    /**
     * Add one log line
     * @param ll
     */
    public synchronized void addRow(LogLine ll) {
        com.eric.org.Util.RenderLine rl = new com.eric.org.Util.RenderLine(ll, mLn);
        //fullLogLines will record the full log lines
        fullLogLines.add(rl);
        mLn++;
        List<ConfigInfo> filterTable = FilterConfigMgr.getActiveFilterList();
        addToFilteredLines(rl, filterTable);
    }

    private void dataChanged(){
        fireTableDataChanged();
        if(owner.mLogchangeListener != null){
            owner.mLogchangeListener.onContentChanged();
        }
    }

    //Reset Render and orginal Data
    public synchronized void reset() {
        resetRender();
        fullLogLines.clear();
        filteredRowNo = 0;
    }

    /*
     *  Only reset filtered Lines without clear original log lines
     */
    private synchronized void resetRender() {
        int rows = showingLineMap.size();
        System.out.println("rows = "+rows );
        if (rows<1) {
            System.out.println("ERROR: rows < 1" );
        } else {
            try {
                fireTableRowsDeleted(0, rows - 1);
                owner.getLogTable().firePropertyChange("tablereset",false,false);
            } catch (Exception e) {
                System.out.println("rows="+rows+" Exception: " + e.getMessage());
            }
        }
        showingLineMap.clear();
        clearFilterHitCount();
        owner.clearAdjListener();
        mLn = 0;
    }

    private void clearFilterHitCount() {
        //clear filter hit count
        List<ConfigInfo> filterTable = FilterConfigMgr.getActiveFilterList();
        if(filterTable ==null) {
            return;
        }
        for (com.eric.org.config.ConfigInfo fc: filterTable) {
            fc.hitCount = 0;
        }
    }


    @Override
    public synchronized int getRowCount() {
        return showingLineMap.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public synchronized Object getValueAt(int row, int col) {
        if (row < showingLineMap.size()) {
            RenderLine rl = fullLogLines.get(showingLineMap.get(row));
            String parsedRst = owner.getPlugInMgr().getParsedResult(rl.getLogline());
            if ((parsedRst != null) && (!parsedRst.isEmpty())) {
                rl.setParsed();
                rl.setChkPointStr(parsedRst);
            }

            return rl.getLogline().line;
        }
        return null;
    }

    public String getColumnName(int col) {
        String logFileName = "";
        return logFileName;
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public Class getColumnClass(int c) {
        if (fullLogLines.size() > 0) {
            return getValueAt(0, c).getClass();
        }
        return null;
    }

    public synchronized RenderLine getLineToBeShown(int row) {
        if(showingLineMap.size()<=0)
            return null;

        int index = 0;
        if (row>=0 && row < showingLineMap.size())
            index = showingLineMap.get(row);
        if(index <0) {
            System.out.println("ERROR: index = "+index);
        } else {
            if(fullLogLines.size()>0)
                return fullLogLines.get(index);
        }
        return null;
    }

    public void hideShowFilter() {
        mHideLine = !mHideLine;
        applyFilter();
    }


    private PropertyChangeListener taskProgressListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (Objects.equals("progress", evt.getPropertyName())) {
                int progress = (Integer) evt.getNewValue();
                if (progressBar != null)
                    progressBar.setValue(progress);
//                System.out.println("Progress = "+progress);
            }
        }
    };
    public void applyFilter() {
        //Check the filter progress, if there is on going process
        // Reset the process to the beginning
        if(fullLogLines.size() == 0){
            return;
        }
        resetRender();

        if (progressBar == null) {
            progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);

            progressDlg = new JDialog();
            progressDlg.add(BorderLayout.CENTER, progressBar);
            progressDlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            progressDlg.setSize(300, 75);

            progressDlg.setAlwaysOnTop(true);

            Toolkit toolkit = progressDlg.getToolkit();
            Dimension size = toolkit.getScreenSize();

            progressDlg.setLocation(size.width / 2 - progressDlg.getWidth() / 2,
                    size.height / 2 - progressDlg.getHeight() / 2);

            progressDlg.setVisible(true);

            FilterWorker lw = new FilterWorker(this);
            lw.addPropertyChangeListener(taskProgressListener);
            lw.execute();
        } else {
            resetFilterProgress = true;
        }
        progressBar.setValue(0);
    }

    public synchronized boolean findAndScrollToWords(String findWords, boolean caseSensitive, boolean fwd, int startLineNumber) {
        int findedLineNum = -1;
        boolean find = false;

        int total = showingLineMap.size();

        int begin,end,step;

        if(startLineNumber<0)
            startLineNumber=0;
        if(startLineNumber>total)
            startLineNumber=total;

        if(fwd){
            begin = startLineNumber;
            end = total-1;
            step = 1;
        }else {
            begin = startLineNumber;
            end = 0;
            step = -1;
        }
        int i = begin;
        boolean cont = true;
        System.out.println("findAndScrollToWords, begin="+begin+"end="+end);
        do{
            int orgLine = showingLineMap.get(i);
            RenderLine msg = fullLogLines.get(orgLine);
            String source = msg.getLogline().line;

            if (caseSensitive) {
                find = source.contains(findWords);
            } else {
                find = source.toLowerCase().contains(findWords.toLowerCase());
            }
            if (find) {
                findedLineNum = i;
                break;
            }
            i = i+step;

            if(fwd){
                cont = (i<=end);
            }else {
                cont = (i>=end);
            }
        }while (cont);


        if (find) {
            owner.getLogTable().scrollToVisible(findedLineNum, 10);
            owner.getLogTable().setRowSelectionInterval(findedLineNum,findedLineNum);
        }

        return find;
    }

    public void saveCapturedLog(File file) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            for(int i = 0; i<fullLogLines.size();i++) {
                fileWriter.write(fullLogLines.get(i).getLogline().msg);
                fileWriter.write('\n');
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Save File Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

        }
    }
    /**
     * Apply Filter
     */
    private class FilterWorker extends SwingWorker<TableModel, String> {

        private final LogModel model;

        private FilterWorker(LogModel model) {
            this.model = model;
        }
        int progressRatio = 0;
        @Override
        protected TableModel doInBackground() {
            int l = 0;
            filteredRowNo = 0;

            List<com.eric.org.config.ConfigInfo> filterTable = FilterConfigMgr.getActiveFilterList();
            for (int i = 0; i < model.fullLogLines.size(); i++) {
                if (resetFilterProgress) {
                    resetFilterProgress = false;
                    i = 0;
                    filteredRowNo = 0;
                    l = 0;
                    setProgress(0);
                    resetRender();
                    dataChanged();
                    model.owner.getLogTable().revalidate();
                    filterTable = FilterConfigMgr.getActiveFilterList();
                }

                com.eric.org.Util.RenderLine rl = model.fullLogLines.get(i);
                addToFilteredLines(rl, filterTable);

                publish("");

                l++;
                progressRatio = (int) ((double) l /  model.fullLogLines.size() * 100);
                if(i%1000==0){
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
//                        e.printStackTrace();
                    }
                }
            }
            return model;
        }

        @Override
        protected void process(List<String> chunks) {
            setProgress(progressRatio);
            dataChanged();
        }

        @Override
        protected synchronized void done() {
            progressBar = null;
            progressDlg.setVisible(false);
            progressDlg.dispose();
            progressDlg = null;
            dataChanged();

            //Get new screen row no.
            int selectedRowNumInShowingLogLines=0;
            for(int i=0; i<showingLineMap.size(); i++){
                if(showingLineMap.get(i)== previousSelectedRowNumInFullLogLines)
                    selectedRowNumInShowingLogLines = i;
            }
            if(isValidScreenLineNumber(selectedRowNumInShowingLogLines)){
                owner.getLogTable().scrollToVisible(selectedRowNumInShowingLogLines, distance);
                System.out.println("scrollToVisible: " + selectedRowNumInShowingLogLines + " distance:" + distance);
                owner.getLogTable().setRowSelectionInterval(selectedRowNumInShowingLogLines, selectedRowNumInShowingLogLines);
            }
//            if(owner.mLogchangeListener != null){
//                owner.mLogchangeListener.onContentChanged();
//            }
            owner.getLogTable().resizeVisables();
            owner.setupAdjListener();
        }
    }

    private synchronized boolean isValidScreenLineNumber(int num){
        return (num >= 0)&&(num<showingLineMap.size());
    }

    private static ConfigInfo getShottedItemFromFilterTable(LogLine ll, List<ConfigInfo> filterTable) {
        ConfigInfo lfi = null;
        boolean interesting = false;
        int tc = filterTable.size();
        for (ConfigInfo aFilterTable : filterTable) {
            lfi = aFilterTable;

            Matcher m = lfi.filterItem.mPatten.matcher(ll.line);
            if (m.find()) {
                interesting = true;
                break;
            }
        }
        if(!interesting) lfi = null;

        return lfi;
    }

    private synchronized void addToFilteredLines(RenderLine rl, List<ConfigInfo> fltList) {
        if (mHideLine || fltList ==null) {
            // fill all line
            showingLineMap.put(filteredRowNo, rl.getOrgLineNo());
            filteredRowNo++;
        } else {
            if (fltList.size() > 0) {
                ConfigInfo fi = getShottedItemFromFilterTable(rl.getLogline(), fltList);
                if (fi != null) {
                    // Returning true indicates this row should be shown.
                    showingLineMap.put(filteredRowNo, rl.getOrgLineNo());
                    rl.setFilterShotted(true, fi.ftColor,fi.bgColor );
                    fi.hitCount++;
                    filteredRowNo++;
                } else {
                    rl.setFilterShotted(false, com.eric.org.config.ConfigInfo.DEFAULT_FT_COLOR, com.eric.org.config.ConfigInfo.DEFAULT_BG_COLOR);
                }
            } else {
                rl.setFilterShotted(true, com.eric.org.config.ConfigInfo.DEFAULT_FT_COLOR, com.eric.org.config.ConfigInfo.DEFAULT_BG_COLOR);
                // fill all line
                showingLineMap.put(filteredRowNo, rl.getOrgLineNo());
                filteredRowNo++;
            }
        }
    }
}
