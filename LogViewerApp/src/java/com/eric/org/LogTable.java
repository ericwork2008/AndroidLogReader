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

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;

/**
 * LogTable will show the Log
 */
public class LogTable extends JTable implements MouseWheelListener{
    private TableCellRenderer mCheckpointRenderer = new com.eric.org.CheckPointRender();

    /**
     * Ctl + mouse up/down to zoom in/out the LogTable view
     * @param e
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.isControlDown()) {
            if (e.getWheelRotation() < 0) {
                Font currentFont = getFont();
                final Font bigFont = new Font(currentFont.getName(), currentFont.getStyle(), currentFont.getSize() + 1);
                setFont(bigFont);
            } else {
                Font currentFont = getFont();
                final Font smallFont = new Font(currentFont.getName(), currentFont.getStyle(), currentFont.getSize() - 1);
                setFont(smallFont);
            }
        }else {
            getParent().dispatchEvent(e);
        }
    }

    public LogTable(LogModel model) {
        super(model);
        setTableHeader(null);
        this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.setPreferredScrollableViewportSize(Toolkit.getDefaultToolkit().getScreenSize());

        setShowGrid(false);

        setFont(new Font("Courier", Font.PLAIN, 14));

        //Set Column 0
        TableColumn column = null;
        column = this.getColumnModel().getColumn(0);
        column.setPreferredWidth(50);

        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);

        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        addListSelectionListener();

        addMouseWheelListener(this);
    }

    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);

        //  Alternate row ftColor
        if (isRowSelected(row)){
            c.setBackground(Color.BLUE);
            c.setForeground(Color.WHITE);
        } else {
            LogModel model = (LogModel) getModel();
            RenderLine rl = model.getLineToBeShown(row);
            if(rl.isFilterShotted()){
                c.setForeground(Color.decode(rl.getFtColor()));
                c.setBackground(Color.decode(rl.getBgColor()));
            }else{
                c.setForeground(Color.GRAY);
                c.setBackground(Color.WHITE);
            }
        }

        return c;
    }

    private int preferredWidth;

    public TableCellRenderer getCellRenderer(int row, int column) {
        LogModel model = (LogModel) getModel();
        RenderLine rl = model.getLineToBeShown(row);

        TableCellRenderer renderer = null;
        if ((rl != null) && (rl.isParsed())){
            renderer =  mCheckpointRenderer;
        }else {
            renderer = super.getCellRenderer(row, column);
        }

        TableColumn tableColumn = getColumnModel().getColumn(column);

        Component c = prepareRenderer(renderer, row, column);
        int width = c.getPreferredSize().width + getIntercellSpacing().width;
        preferredWidth = Math.max(preferredWidth, width);

        tableColumn.setPreferredWidth( preferredWidth );

        int height = c.getPreferredSize().height + getIntercellSpacing().height;
        int rowHeight = getRowHeight();
        rowHeight = Math.max(rowHeight, height);

        setRowHeight(row, rowHeight);
        return renderer;
    }

    /**
     * Return the total rows in the Log view
     * @return
     */
    private int getVisibleRowsInTheView() {
        return (int) (getParent().getSize().getHeight() / getRowHeight());
    }

    /**
     * Get the row distance from Top to the selected line
     * @return
     */
    private int getSelectedLineDistanceInView(){
        JViewport viewport = (JViewport)getParent();
        Point p = viewport.getViewPosition();
        int firstRow = rowAtPoint(p);
        int selectedRow = getSelectedRow();
        return selectedRow - firstRow;
    }

    public void scrollToVisible(int rowIndex, int delta) {
        Rectangle visibleRect = getVisibleRect();
        Rectangle rect1 = getCellRect(rowIndex-delta, 0, false);

        if (visibleRect.y >= rect1.y) {
            scrollRectToVisible(rect1);
        } else {
            Rectangle rect2 = getCellRect(rowIndex - delta + getVisibleRowsInTheView(), 0, false);
            int width = rect2.y - rect1.y;
            scrollRectToVisible(new Rectangle(rect1.x, rect1.y, rect1.width, rect1.height + width));
        }
    }

    public boolean getScrollableTracksViewportWidth() {
        return getPreferredSize().width < getParent().getWidth();
    }

    private void addListSelectionListener(){
        //When selection changes, provide user with row numbers for
        //both view and model.
        this.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        String selectedData = null;

                        int[] selectedRow = getSelectedRows();

                        if(selectedRow.length>0){
                            LogModel model = (LogModel) getModel();
                            int orgLine = model.screenToOriginalNumber(selectedRow[0]);
                            model.setSelectedOriginalLineNum(orgLine);
                            model.setDistance(getSelectedLineDistanceInView());
                            System.out.println("Selected Row, screen row number = " + selectedRow[0]+"; Distance:" + getSelectedLineDistanceInView()+"; Original Row number = " + orgLine);
                        }
                    }
                }
        );
    }
}
