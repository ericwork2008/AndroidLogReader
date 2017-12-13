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

import com.eric.org.Util.RenderLines;
import com.eric.org.config.ConfigInfo;
import com.eric.org.config.FilterConfigMgr;
import com.eric.org.util.LogLine;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

public class LogModel extends AbstractTableModel {
    private static JProgressBar progressBar = null;
    private static JDialog progressDlg = null;

    private static FilterWorker lw = null;
    //Filter can be reset when filter in progress
    private static volatile boolean resetFilterProgress = false;
    //Remember previous selection
    private static int previousSelectedOriginalLineNum = 0;
    private static int previousSelectedScreenfocusLineNum = 0;
    private static int deltaLineNumber;//previous selection gap to the top
    //Hide or unhide Log
    private static boolean mHideLine = false;

    private com.eric.org.LogTableMgr owner;

    private static int screenLineNo = 0;
    private int mLn = 0;
    private static int last_find_index = 0; // used to record last find index when merge

    //Showing Line Map
    private HashMap<Integer, Integer> showingLineMap = new HashMap<>();

    public RenderLines getRenderLines() {
        return renderLines;
    }

    private com.eric.org.Util.RenderLines renderLines = new com.eric.org.Util.RenderLines();

    public LogModel(com.eric.org.LogTableMgr logTableMgr) {
        owner = logTableMgr;
    }


    //
    public void setDeltaLineNumber(int deltaLineNumber) {
        LogModel.deltaLineNumber = deltaLineNumber;
    }

    public int getSelectedOriginalLineNum() {
        return previousSelectedOriginalLineNum;
    }

    public void setSelectedOriginalLineNum(int focusLineNum) {
        LogModel.previousSelectedOriginalLineNum = focusLineNum;
    }

    public synchronized int screenToOriginalNumber(int screenNum) {
        return showingLineMap.get(screenNum);
    }

    public synchronized int originalToScreenLineNumber(int originalNum) {
        int rst = -1;
        int count = showingLineMap.size();
        for (int i = 0; i < count; i++) {
            if (showingLineMap.get(i) == originalNum) {
                rst = i;
                break;
            }
        }
        return rst;
    }
    public void insertRow(LogLine sl) {
        resetRender();
        //find and insert row
        last_find_index = findLineBefore(last_find_index,renderLines,sl);
//        renderLines.insertElementAt();
    }
    private int findLineBefore(int last,com.eric.org.Util.RenderLines rls, LogLine sl) {
        for (int i = last; i<rls.size(); i++) {
//            LogLine tl = rls.get(i);
//            if(tl.)
        }
        return 0;
    }
    //Original Data Store
    public void addRow(LogLine ll) {
        com.eric.org.Util.RenderLine rl = new com.eric.org.Util.RenderLine(ll, mLn);
        renderLines.add(rl);
        mLn++;
        List<com.eric.org.config.ConfigInfo> filterTable = FilterConfigMgr.getActiveFilterList();
        updateScreenLog(rl, filterTable);
        dataChanged();
    }

    public void delRow(int row) {
        renderLines.remove(row);
    }

    private void dataChanged(){
        fireTableDataChanged();
        if(owner.logchangeListener != null){
            owner.logchangeListener.onContentChanged();
        }
    }

    //Reset Render and orginal Data
    public void reset() {
        resetRender();
        renderLines.clear();

        mHideLine = false;
        screenLineNo = 0;
    }

    /*
     *  Only reset render without clear original log lines
     */
    private synchronized void resetRender() {
        showingLineMap.clear();
        clearFilterHitCount();
        mLn = 0;

        int rows = getRowCount();
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
    }

    private void clearFilterHitCount() {
        //clear filter hit count
        List<com.eric.org.config.ConfigInfo> filterTable = FilterConfigMgr.getActiveFilterList();
        if(filterTable ==null){
            return;
        }
        for (com.eric.org.config.ConfigInfo fc: filterTable) {
            fc.hitCount = 0;
        }
    }


    @Override
    public int getRowCount() {
        return showingLineMap.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (row < showingLineMap.size()) {
            com.eric.org.Util.RenderLine rl = renderLines.get(showingLineMap.get(row));
            String parsedRst = owner.getPlugInMgr().getParsedResult(rl.getLogline());
            if ((parsedRst != null) && (!parsedRst.isEmpty())) {
                rl.setParsed();
                rl.setChkPointStr(parsedRst);
            }

            return rl.getLogline().line;
        }
        return null;
    }

    public void setValueAt(Object value, int row, int col) {
        System.out.println("Setting value at " + row + "," + col
                + " to " + value
                + " (an instance of "
                + value.getClass() + ")");
        renderLines.set(row, (com.eric.org.Util.RenderLine) value);
        this.dataChanged();
        System.out.println("New value of data:");

    }

    public String getColumnName(int col) {
        String logFileName = "";
        return logFileName;
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public Class getColumnClass(int c) {
        if (renderLines.size() > 0) {
            return getValueAt(0, c).getClass();
        }
        return null;
    }

    public com.eric.org.Util.RenderLine getRenderLine(int row) {
        int index = 0;
        if (row>=0 && row < showingLineMap.size())
            index = showingLineMap.get(row);
        if(index <0) {
            System.out.println("ERROR: index = "+index);
        } else {
            return renderLines.get(index);
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
        if(renderLines.size() == 0){
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

    public boolean findAndScrollToWords(String findWords, boolean caseSensitive, boolean fwd, int startLineNumber) {
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
            com.eric.org.Util.RenderLine msg = renderLines.get(orgLine);
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



    private class FilterWorker extends SwingWorker<TableModel, String> {

        private final LogModel model;

        private FilterWorker(LogModel model) {
            this.model = model;
        }

        @Override
        protected TableModel doInBackground() throws Exception {
            int total = model.renderLines.size();

            int l = 0;
            screenLineNo = 0;

            List<com.eric.org.config.ConfigInfo> filterTable = FilterConfigMgr.getActiveFilterList();
            for (int i = 0; i < total; i++) {
                if (resetFilterProgress) {
                    resetFilterProgress = false;
                    i = 0;
                    screenLineNo = 0;
                    l = 0;
                    setProgress(0);
                    resetRender();
                    dataChanged();
                    model.owner.getLogTable().revalidate();
                    filterTable = FilterConfigMgr.getActiveFilterList();
                }

                com.eric.org.Util.RenderLine rl = model.renderLines.get(i);

                updateScreenLog(rl, filterTable);

                publish("");
                if (rl.getOrgLineNumber() <= previousSelectedOriginalLineNum) {
                    previousSelectedScreenfocusLineNum = screenLineNo - 1;
//                    System.out.println("previousSelectedOriginalLineNum: " + previousSelectedOriginalLineNum);
//                    System.out.println("previousSelectedScreenfocusLineNum: " + previousSelectedScreenfocusLineNum);
                }
                l++;
                int progressRatio = (int) ((double) l / total * 100);
                setProgress(progressRatio);
            }

            return model;
        }

        @Override
        protected void process(List<String> chunks) {
            dataChanged();
        }

        @Override
        protected void done() {
            progressBar = null;
            progressDlg.setVisible(false);
            progressDlg.dispose();
            progressDlg = null;
            lw = null;
            dataChanged();
            if(isValidScreenLineNumber(previousSelectedScreenfocusLineNum)){
                owner.getLogTable().scrollToVisible(previousSelectedScreenfocusLineNum, deltaLineNumber);
                System.out.println("scrollToVisible: " + previousSelectedScreenfocusLineNum + " deltaLineNumber:" +deltaLineNumber);
                owner.getLogTable().setRowSelectionInterval(previousSelectedScreenfocusLineNum, previousSelectedScreenfocusLineNum);
            }
//            if(owner.logchangeListener != null){
//                owner.logchangeListener.onContentChanged();
//            }
        }
    }

    private boolean isValidScreenLineNumber(int num){
        return (num >= 0)&&(num<showingLineMap.size());
    }

    private static com.eric.org.config.ConfigInfo getShottedItemFromFilterTable(LogLine ll, List<com.eric.org.config.ConfigInfo> filterTable){
        com.eric.org.config.ConfigInfo lfi = null;
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

    private void updateScreenLog(com.eric.org.Util.RenderLine rl, List<com.eric.org.config.ConfigInfo> fltList) {

        if (mHideLine || fltList ==null) {
            // fill all line
            showingLineMap.put(screenLineNo, rl.getOrgLineNumber());
            screenLineNo++;

        } else {
            if (fltList.size() > 0) {
                com.eric.org.config.ConfigInfo fi = getShottedItemFromFilterTable(rl.getLogline(), fltList);
                if (fi != null) {
                    // Returning true indicates this row should be shown.
                    showingLineMap.put(screenLineNo, rl.getOrgLineNumber());
                    rl.setFilterShotted(true, fi.ftColor,fi.bgColor );
                    fi.hitCount++;
                    screenLineNo++;
                } else {
                    rl.setFilterShotted(false, com.eric.org.config.ConfigInfo.DEFAULT_FT_COLOR, com.eric.org.config.ConfigInfo.DEFAULT_BG_COLOR);
                }
            } else {
                rl.setFilterShotted(true, com.eric.org.config.ConfigInfo.DEFAULT_FT_COLOR, com.eric.org.config.ConfigInfo.DEFAULT_BG_COLOR);
                // fill all line
                showingLineMap.put(screenLineNo, rl.getOrgLineNumber());
                screenLineNo++;
            }
        }
    }
}
