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
import javax.swing.table.TableCellRenderer;
import java.awt.*;

class CheckPointRender extends JPanel implements TableCellRenderer {
    private JLabel parserRsltLable = null;
    private JLabel loglineLabl = null;

    public CheckPointRender() {
        setOpaque(true); //MUST do this for background to show up
    }

    public Component getTableCellRendererComponent(
            JTable table, Object color,
            boolean isSelected, boolean hasFocus,
            int row, int column) {

        LogModel model = (LogModel) table.getModel();
        RenderLine rl = model.getLineToBeShown(row);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if(parserRsltLable==null){
            parserRsltLable = new JLabel();
            add(parserRsltLable, this);
        }
        parserRsltLable.setOpaque(true);
        parserRsltLable.setBorder(BorderFactory.createEmptyBorder());
        parserRsltLable.setFont(table.getFont());
        parserRsltLable.setBackground(new Color(255,255,153));


        if(rl.isParsed()){
            parserRsltLable.setText(rl.getChkPointStr());
        }

        if(loglineLabl == null){
            loglineLabl = new JLabel();
            add(loglineLabl,this);
        }
        loglineLabl.setFont(table.getFont());
        loglineLabl.setText(rl.getLogline().line);


        int rowHeight = table.getRowHeight();

        rowHeight = Math.max(rowHeight, getPreferredSize().height);

        table.setRowHeight(row, rowHeight);

        if(table.isRowSelected(row)){
            parserRsltLable.setOpaque(false);
            parserRsltLable.setForeground(Color.WHITE);
            loglineLabl.setForeground(Color.WHITE);
        }else{
            parserRsltLable.setForeground(Color.BLACK);
            loglineLabl.setForeground(Color.BLACK);
        }

        return this;
    }

}
