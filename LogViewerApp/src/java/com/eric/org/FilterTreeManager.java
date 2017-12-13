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
import com.eric.org.config.FilterConfigMgr;
import santhosh.CheckTreeCellRenderer;
import santhosh.CheckTreeSelectionModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import static javax.swing.GroupLayout.Alignment.LEADING;

/**
 * FilterTreeManager will cooridinate FilterTreeLstener/FilterConfigTreeModel/filterConfigtree/CheckTreeSelectionModel
 */
public class FilterTreeManager {
    private JTree filterConfigtree;
    private CheckTreeSelectionModel filterConfigTreeSelectionModel;
    private FilterConfigNode originalTreeRoot;
    private FilterConfigTreeModel filterTreeModel;
    private FilterTreeListener filterTreeListener;

    private static FilterTreeManager _instance;

    public static FilterTreeManager getInstance() {
        if (_instance != null) {
            return _instance;
        }
        return _instance = new FilterTreeManager();
    }

    private FilterTreeManager() {
    }
    private String filterFilterText;

    public String getFilterFilterText() {
        return filterFilterText;
    }

    public JTree getFilterConfigtree() {
        return filterConfigtree;
    }

    public FilterConfigTreeModel getFilterTreeModel() {
        return filterTreeModel;
    }

    public CheckTreeSelectionModel getFilterConfigTreeSelectionModel() {
        return filterConfigTreeSelectionModel;
    }


    public JComponent createFilterPanel() {
        final JPanel box = new JPanel(new BorderLayout());

        //Text filter
        final JPanel filterTxPanel = new JPanel(new FlowLayout(SwingConstants.LEADING));
        JLabel filterLabel = new JLabel("Filter Text:");
        final JTextField textField = new JTextField(10);
        filterTxPanel.add(filterLabel);
        filterTxPanel.add(textField);

//        ly.setHorizontalGroup(ly.createSequentialGroup()
//                .addComponent(filterLabel)
//                .addComponent(textField)
//        );
//        ly.setVerticalGroup(ly.createParallelGroup()
//                .addComponent(filterLabel)
//                .addComponent(textField)
//        );

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onChange();
            }

            public void onChange() {
                filterTree(textField.getText() );
            }
        });


        //Filter Tree panel
        JComponent filterTreePanel = makeSingleFilterTreePanel();

        box.add(filterTxPanel, BorderLayout.NORTH);
        box.add(Box.createVerticalGlue());
        box.add(filterTreePanel,BorderLayout.CENTER);

        //Add tab
//        JTabbedPane tabbedPane = new JTabbedPane();
//        JComponent panel1 = makeSingleFilterTreePanel(true);
//
//        tabbedPane.addTab("CheckPoint",panel1);
//        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
//
//        JTextField one = new JTextField("one");
//        tabbedPane.add("one",one);
//
//        JTextField two = new JTextField("two");
//        tabbedPane.add("two",two);
//
//        tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
//        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
//        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        return box;
    }

    private void filterTree(String filteredText) {
        //get a copy
        DefaultMutableTreeNode filteredRoot = copyNode(originalTreeRoot);

        if (!filteredText.trim().toString().equals("")) {
            TreeNodeBuilder b = new TreeNodeBuilder(filteredText);
            filteredRoot = b.prune(filteredRoot);
        }

        filterTreeModel.setRoot(filteredRoot);
        filterConfigtree.setModel(filterTreeModel);
        filterConfigtree.updateUI();

        //Only expand the selected path
        TreePath[] selectionPaths = filterConfigTreeSelectionModel.getSelectionPaths();
        if (selectionPaths != null) {
            for (TreePath selectionPath : selectionPaths) {
                filterConfigtree.expandPath(selectionPath);
            }
        }
    }

    /**
     * Clone/Copy a tree node. TreeNodes in Swing don't support deep cloning.
     *
     * @param orig to be cloned
     * @return cloned copy
     */
    private FilterConfigNode copyNode(FilterConfigNode orig) {
        if(orig==null){
            return null;
        }
        FilterConfigNode newOne = new FilterConfigNode((ConfigInfo) orig.getUserObject());
        Enumeration enm = orig.children();

        while (enm.hasMoreElements()) {

            FilterConfigNode child = (FilterConfigNode) enm.nextElement();
            newOne.add(copyNode(child));
        }
        return newOne;
    }

    private JComponent makeSingleFilterTreePanel() {

        //Add Tree
        originalTreeRoot = new FilterConfigNode(FilterConfigMgr.rootConfigInfo);
        filterTreeModel = new FilterConfigTreeModel(originalTreeRoot);

        filterConfigtree = new JTree(filterTreeModel);
        filterConfigtree.setRootVisible(false);
        filterConfigtree.setShowsRootHandles(true);
        filterConfigtree.setEditable(false);
        filterConfigtree.setDragEnabled(true);
        filterConfigtree.setDropMode(DropMode.ON_OR_INSERT);
        filterConfigtree.putClientProperty("JTree.lineStyle", "Angled");
//        filterConfigtree.setFillsViewportHeight(true);

        //Selection model will use tree model to analysis the data
        filterConfigTreeSelectionModel = new CheckTreeSelectionModel(filterTreeModel, true);

        //tree model will use selection model to set the section
        filterTreeModel.setTreeSelectionModel(filterConfigTreeSelectionModel);

        filterTreeListener = new FilterTreeListener(true);

        CheckTreeCellRenderer renderer = new CheckTreeCellRenderer(filterConfigtree.getCellRenderer(), filterConfigTreeSelectionModel);

        filterConfigtree.setCellRenderer(renderer);

//        TreeCellEditor editor = new CheckTreeCellEditor(filterConfigtree, renderer);
//        filterConfigtree.setCellEditor(editor);
        // This allows the edit to be saved if editing is interrupted
        // by a change in selection, focus, etc -> see method detail.
        filterConfigtree.setInvokesStopCellEditing(true);

        for (int i = 0; i < filterConfigtree.getRowCount(); i++)
            filterConfigtree.expandRow(i);

        FilterPopupMenu fpm = new FilterPopupMenu(filterConfigtree);
        filterConfigtree.addMouseListener(fpm.popupListener);
        filterConfigtree.addMouseListener(filterTreeListener);
        filterConfigTreeSelectionModel.addTreeSelectionListener(filterTreeListener);
        filterTreeModel.addTreeModelListener(filterTreeListener);
        filterConfigtree.registerKeyboardAction(filterTreeListener, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_FOCUSED);
        filterConfigtree.addMouseWheelListener(filterTreeListener);
        return new JScrollPane(filterConfigtree);
    }

    public void loadFile(File file) {
        FilterConfigMgr fc = new FilterConfigMgr(filterTreeModel);
        fc.loadFilterConfig(file);
        filterTreeModel.attach(fc);
        filterTreeModel.reCreateTreeNodes();
        filterConfigtree.updateUI();

//        for(int i=0; i<filterConfigtree.getRowCount(); i++)
//            filterConfigtree.expandRow(i);

        //Expend 0 level
        FilterConfigNode currentNode = (FilterConfigNode) filterTreeModel.getRoot();
        do {
            if (currentNode.getLevel() == 0)
                filterConfigtree.expandPath(new TreePath(currentNode.getPath()));
            currentNode = (FilterConfigNode) currentNode.getNextNode();
        }
        while (currentNode != null);
    }

    public void attachLogTable(LogTableMgr logTableMgr) {
        //Attach logmodel then filter tree can update log model data
        if (filterTreeListener != null)
            filterTreeListener.attachLogModel(logTableMgr);
    }


    public void delete(TreePath slectedTp) {
        FilterConfigNode tn = (FilterConfigNode) slectedTp.getLastPathComponent();

        List toRemove = new LinkedList();

        getDeleteList(tn, toRemove);

        for (Object aToRemove : toRemove) {
            FilterConfigNode node = (FilterConfigNode) aToRemove;
            filterTreeModel.removeFromParent(node);
        }

//        filterTreeModel.reCreateTreeNodes();
    }

    private void getDeleteList(FilterConfigNode tn, List toRemove) {
        if (tn == null)
            return;

        Enumeration folders = tn.children();
        while (tn.children().hasMoreElements()) {
            FilterConfigNode nd = null;
            try {
                nd = (FilterConfigNode) folders.nextElement();
            } catch (Exception e) {
                System.out.println(e);
                break;
            }
            getDeleteList(nd, toRemove);
        }
        toRemove.add(tn);
    }

    public void newFilterConfig(ConfigInfo filterConfig, TreePath parent) {
        ConfigInfo cn;
        FilterConfigNode tn;

        if (parent != null) {
            tn = (FilterConfigNode) parent.getLastPathComponent();
        } else {
            tn = (FilterConfigNode) filterTreeModel.getRoot();
        }

        cn = (ConfigInfo) tn.getUserObject();
        if (cn.isGroup()) {
            cn.addChild(filterConfig);
            FilterConfigNode newNode = new FilterConfigNode(filterConfig);
            filterTreeModel.insertNodeInto(newNode, tn, tn.getChildCount());
            TreePath path = new TreePath(newNode.getPath());
            // display the new node
            filterConfigtree.scrollPathToVisible(path);
            filterConfigtree.setSelectionPath(path);
            //Trigger value changed
//            filterTreeModel.valueForPathChanged(path, (Object)newNode);
        } else
            System.out.println("newFilterConfigGrp Wrong");
    }

    public void newFilterConfigGrp(ConfigInfo filterConfigGrp, TreePath parent) {
        ConfigInfo cn;
        FilterConfigNode tn;

        filterConfigGrp.setGroup(true);

        if (parent != null) {
            tn = (FilterConfigNode) parent.getLastPathComponent();
        } else {
            tn = (FilterConfigNode) filterTreeModel.getRoot();
        }

        cn = (ConfigInfo) tn.getUserObject();

        if (cn.isGroup()) {

            cn.addChild(filterConfigGrp);
            FilterConfigNode newNode = new FilterConfigNode(filterConfigGrp);
            filterTreeModel.insertNodeInto(newNode, tn, tn.getChildCount());
            // display the new node
            filterConfigtree.scrollPathToVisible(new TreePath(newNode.getPath()));
        } else
            System.out.println("newFilterConfigGrp Wrong");
    }

    public void editFilterConfigGrp(String grpName, TreePath slectedTp) {
        FilterConfigNode tn = (FilterConfigNode) slectedTp.getLastPathComponent();
        ConfigInfo cn = (ConfigInfo) tn.getUserObject();

        if (cn != null) {
            cn.setName(grpName);
//            filterTreeModel.valueForPathChanged(slectedTp, (Object)cn);
        }
    }

    public ConfigInfo getConfig(TreePath slectedTp) {
        ConfigInfo cn = null;
        try {
            FilterConfigNode tn = (FilterConfigNode) slectedTp.getLastPathComponent();
            cn = (ConfigInfo) tn.getUserObject();
        } catch (Exception e) {

        }

        return cn;
    }

    public void editFilterConfig(ConfigInfo filterConfig, TreePath slectedTp) {
        FilterConfigNode tn = (FilterConfigNode) slectedTp.getLastPathComponent();
        ConfigInfo cn = (ConfigInfo) tn.getUserObject();

        cn.enabled = filterConfig.enabled;
        cn.filterItem.caseSensitive = filterConfig.filterItem.caseSensitive;
        cn.filterItem.mMsgPattern = filterConfig.filterItem.mMsgPattern;
        cn.ftColor = filterConfig.ftColor;
        cn.bgColor = filterConfig.bgColor;
        try {
            cn.filterItem.initPattern();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }

        //Trigger value changed
        filterTreeModel.valueForPathChanged(slectedTp, cn);
    }

    public FilterConfigNode getOriginalRoot() {
        return originalTreeRoot;
    }
}
