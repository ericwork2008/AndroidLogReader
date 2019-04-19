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

import com.eric.org.config.ConfigInfo;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class FilterPopupMenu {
    private JPopupMenu popup;
    public MousePopupListener popupListener;
    private JTree owner;

    private TreePath slectedTp = null;
    private com.eric.org.FilterTreeManager ftm = FilterTreeManager.getInstance();

    public FilterPopupMenu(JTree owner){
        this.owner = owner;

        popupListener=new MousePopupListener();

        popup = new JPopupMenu();
//        addMouseListener(new MousePopupListener());
    }

    private ActionListener menuListener = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            String actionCommand = event.getActionCommand();
            System.out.println("Popup menu item ["
                    + actionCommand + "] was pressed.");

            if(Objects.equals(actionCommand, "New Filter")){
                FilterConfigDlg fcd = new FilterConfigDlg("filter");
                fcd.setModal(true);
                ConfigInfo rst = fcd.showDialog();
                rst.setGroup(false);
                ftm.newFilterConfig(rst, slectedTp);
            }

            if(Objects.equals(actionCommand, "New Group")){
                JOptionPane optionPane = new JOptionPane("Input"
                        , JOptionPane.PLAIN_MESSAGE
                        , JOptionPane.DEFAULT_OPTION
                        , null, null, "Please Enter Group name:");
                optionPane.setWantsInput(true);
                JDialog dialog = optionPane.createDialog(null, "Input");

                Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
                dialog.setLocation(size.width/2 ,size.height/2);
                dialog.setModal(true);
                dialog.setVisible(true);

//                String grpName = JOptionPane.showInputDialog(owner, "Enter Group name:");
                String grpName = (String) optionPane.getInputValue();
                if((grpName!=null) && (!grpName.isEmpty()))
                    ftm.newFilterConfigGrp(new ConfigInfo(grpName),slectedTp);
            }

            if(Objects.equals(actionCommand, "Delete")){
                if(slectedTp != null)
                    ftm.delete(slectedTp);
            }

            if(Objects.equals(actionCommand, "Edit")){
                FilterConfigDlg fcd = new FilterConfigDlg("filter");
                fcd.setModal(true);
                fcd.loadConfig( ftm.getConfig(slectedTp));
                ConfigInfo rst = fcd.showDialog();
                if(rst != null)
                    ftm.editFilterConfig(fcd.getFilterConfig(),slectedTp);
            }
//            ftm.getFilterTreeModel().reload();
        }
    };
    // An inner class to show when popup events occur
    class PopupPrintListener implements PopupMenuListener {
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            System.out.println("Popup menu will be visible!");
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            System.out.println("Popup menu will be invisible!");
        }

        public void popupMenuCanceled(PopupMenuEvent e) {
            System.out.println("Popup menu is hidden!");
        }
    }
    // An inner class to check whether mouse events are the popup trigger
    class MousePopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            checkPopup(e);
        }

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)){
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();

                    slectedTp = owner.getPathForLocation(e.getX(), e.getY());
                    ConfigInfo cn = ftm.getConfig(slectedTp);
                    if(cn==null){
                        return;
                    }
                    if(cn.isGroup()){
                        String grpName = JOptionPane.showInputDialog(owner, "Enter New Group name:");
                        if((grpName!=null) && (!grpName.isEmpty()))
                            ftm.editFilterConfigGrp(grpName,slectedTp);
                    }else {
                        FilterConfigDlg fcd = new FilterConfigDlg("filter");
                        fcd.setModal(true);
                        fcd.loadConfig( ftm.getConfig(slectedTp));
                        ConfigInfo rst = fcd.showDialog();
                        if(rst != null)
                            ftm.editFilterConfig(fcd.getFilterConfig(),slectedTp);
                    }
                }
            } else if (SwingUtilities.isRightMouseButton(e)){
                int selRow = owner.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = owner.getPathForLocation(e.getX(), e.getY());
                owner.setSelectionPath(selPath);
                if (selRow>-1){
                    owner.setSelectionRow(selRow);
                }
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                int selRow = owner.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = owner.getPathForLocation(e.getX(), e.getY());
                owner.setSelectionPath(selPath);
                if (selRow > -1) {
                    owner.setSelectionRow(selRow);
                }
            }
            checkPopup(e);
        }

        private void checkPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                JMenuItem item;
                popup.removeAll();
                slectedTp = owner.getPathForLocation(e.getX(), e.getY());
                if(slectedTp != null){
                    System.out.println(slectedTp.toString());
                    ConfigInfo cn = ftm.getConfig(slectedTp);
                    if(cn.isGroup()){
                        popup.add(item = new JMenuItem("New Group", null));
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        item.addActionListener(menuListener);
                        popup.add(item = new JMenuItem("New Filter", null));
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        item.addActionListener(menuListener);
                        popup.add(item = new JMenuItem("Delete", null));
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        item.addActionListener(menuListener);
                        popup.setBorder(new BevelBorder(BevelBorder.RAISED));
                        popup.addPopupMenuListener(new PopupPrintListener());
                    }else{
                        popup.add(item = new JMenuItem("Delete", null));
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        item.addActionListener(menuListener);
                        popup.add(item = new JMenuItem("Edit", null));
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        item.addActionListener(menuListener);
                        popup.setBorder(new BevelBorder(BevelBorder.RAISED));
                        popup.addPopupMenuListener(new PopupPrintListener());
                    }
                }else {
                    popup.add(item = new JMenuItem("New Group", null));
                    item.setHorizontalTextPosition(JMenuItem.RIGHT);
                    item.addActionListener(menuListener);
                    popup.add(item = new JMenuItem("New Filter", null));
                    item.setHorizontalTextPosition(JMenuItem.RIGHT);
                    item.addActionListener(menuListener);
                }
                popup.show(owner, e.getX(), e.getY());
            }
        }
    }
}
