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
import javax.swing.tree.TreePath;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/**
 * FilterTreeManager will cooridinate FilterTreeLstener/FilterConfigTreeModel/filterConfigtree/CheckTreeSelectionModel
 */
class FilterTreeManager {
    private JTree filterConfigtree;
    private CheckTreeSelectionModel filterConfigTreeSelectionModel;
    private FilterConfigTreeModel filterConfigTreeModel;
    private FilterTreeListener filterTreeListener;

    public JTree getFilterConfigtree() {
        return filterConfigtree;
    }

    public FilterConfigTreeModel getFilterConfigTreeModel() {
        return filterConfigTreeModel;
    }

    public CheckTreeSelectionModel getFilterConfigTreeSelectionModel() {
        return filterConfigTreeSelectionModel;
    }


    public JComponent createFilterPanel(){
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

        return makeSingleFilterTreePanel();
    }

    private JComponent makeSingleFilterTreePanel() {
        FilterConfigNode rootNode = new FilterConfigNode(FilterConfigMgr.rootConfigInfo);
        filterConfigTreeModel = new FilterConfigTreeModel(rootNode);
        filterConfigtree = new JTree(filterConfigTreeModel);
        filterConfigtree.setRootVisible(false);
        filterConfigtree.setShowsRootHandles(true);
        filterConfigtree.setEditable(false);
        filterConfigtree.setDragEnabled(true);
        filterConfigtree.putClientProperty("JTree.lineStyle", "Angled");

        //Selection model will use tree model to analysis the data
        filterConfigTreeSelectionModel = new CheckTreeSelectionModel(filterConfigTreeModel, true);

        //tree model will use selection model to set the section
        filterConfigTreeModel.setTreeSelectionModel(filterConfigTreeSelectionModel);

        filterTreeListener = new FilterTreeListener(this, true);

        CheckTreeCellRenderer renderer =new CheckTreeCellRenderer(filterConfigtree.getCellRenderer(), filterConfigTreeSelectionModel);

        filterConfigtree.setCellRenderer(renderer);

//        TreeCellEditor editor = new CheckTreeCellEditor(filterConfigtree, renderer);
//        filterConfigtree.setCellEditor(editor);
        // This allows the edit to be saved if editing is interrupted
        // by a change in selection, focus, etc -> see method detail.
        filterConfigtree.setInvokesStopCellEditing(true);

        for(int i=0; i<filterConfigtree.getRowCount(); i++)
            filterConfigtree.expandRow(i);

        FilterPopupMenu fpm = new FilterPopupMenu(filterConfigtree, this);
        filterConfigtree.addMouseListener(fpm.popupListener);
        filterConfigtree.addMouseListener(filterTreeListener);
        filterConfigTreeSelectionModel.addTreeSelectionListener(filterTreeListener);
        filterConfigTreeModel.addTreeModelListener(filterTreeListener);
        filterConfigtree.registerKeyboardAction(filterTreeListener, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_FOCUSED);
        return new JScrollPane(filterConfigtree);
    }

    public void loadFile(File file) {
        FilterConfigMgr fc = new FilterConfigMgr(filterConfigTreeModel);
        fc.loadFilterConfig(file);
        filterConfigTreeModel.attach(fc);
        filterConfigTreeModel.reCreateTreeNodes();

//        for(int i=0; i<filterConfigtree.getRowCount(); i++)
//            filterConfigtree.expandRow(i);

        //Expend 0 level
        FilterConfigNode currentNode = (FilterConfigNode) filterConfigTreeModel.getRoot();
        do {
            if (currentNode.getLevel()==0)
                filterConfigtree.expandPath(new TreePath(currentNode.getPath()));
            currentNode = (FilterConfigNode)currentNode.getNextNode();
        }
        while (currentNode != null);
    }

    public void attachLogTable(LogTableMgr logTableMgr) {
        //Attach logmodel then filter tree can update log model data
        if(filterTreeListener!=null)
            filterTreeListener.attachLogModel(logTableMgr);
    }



    public void delete(TreePath slectedTp) {
        FilterConfigNode tn = (FilterConfigNode)slectedTp.getLastPathComponent();

        List toRemove = new LinkedList();

        getDeleteList(tn,toRemove);

        for (Object aToRemove : toRemove) {
            FilterConfigNode node = (FilterConfigNode) aToRemove;
            filterConfigTreeModel.removeFromParent(node);
        }

//        filterConfigTreeModel.reCreateTreeNodes();
    }
    private void getDeleteList(FilterConfigNode tn, List toRemove){
        if(tn == null)
            return;

        Enumeration folders = tn.children();
        while (tn.children().hasMoreElements()) {
            FilterConfigNode nd=null;
            try{
                nd = (FilterConfigNode) folders.nextElement();
            } catch (Exception e){
                System.out.println(e);
                break;
            }
            getDeleteList(nd,toRemove);
        }
        toRemove.add(tn);
    }

    public void newFilterConfig(ConfigInfo filterConfig, TreePath parent) {
        ConfigInfo cn;
        FilterConfigNode tn;

        if(parent!=null){
            tn = (FilterConfigNode)parent.getLastPathComponent();
        }else{
            tn = (FilterConfigNode) filterConfigTreeModel.getRoot();
        }

        cn = (ConfigInfo) tn.getUserObject();
        if(cn.isGroup()){
            cn.addChild(filterConfig);
            FilterConfigNode newNode = new FilterConfigNode(filterConfig);
            filterConfigTreeModel.insertNodeInto(newNode, tn, tn.getChildCount());
            TreePath path =new TreePath(newNode.getPath());
            // display the new node
            filterConfigtree.scrollPathToVisible(path);
            filterConfigtree.setSelectionPath(path);
            //Trigger value changed
//            filterConfigTreeModel.valueForPathChanged(path, (Object)newNode);
        }else
            System.out.println("newFilterConfigGrp Wrong");
    }

    public void newFilterConfigGrp(ConfigInfo filterConfigGrp, TreePath parent) {
        ConfigInfo cn;
        FilterConfigNode tn;

        filterConfigGrp.setGroup(true);

        if(parent!=null){
            tn = (FilterConfigNode)parent.getLastPathComponent();
        }else{
            tn = (FilterConfigNode) filterConfigTreeModel.getRoot();
        }

        cn = (ConfigInfo) tn.getUserObject();

        if(cn.isGroup()){

            cn.addChild(filterConfigGrp);
            FilterConfigNode newNode = new FilterConfigNode(filterConfigGrp);
            filterConfigTreeModel.insertNodeInto(newNode, tn, tn.getChildCount());
            // display the new node
            filterConfigtree.scrollPathToVisible(new TreePath(newNode.getPath()));
        }else
            System.out.println("newFilterConfigGrp Wrong");
    }

    public void editFilterConfigGrp(String grpName, TreePath slectedTp) {
        FilterConfigNode tn = (FilterConfigNode)slectedTp.getLastPathComponent();
        ConfigInfo cn = (ConfigInfo) tn.getUserObject();

        if(cn != null){
            cn.setName(grpName);
//            filterConfigTreeModel.valueForPathChanged(slectedTp, (Object)cn);
        }
    }

    public ConfigInfo getConfig(TreePath slectedTp) {
        ConfigInfo cn=null;
        try{
            FilterConfigNode tn = (FilterConfigNode)slectedTp.getLastPathComponent();
            cn = (ConfigInfo) tn.getUserObject();
        }catch (Exception e){

        }

        return cn;
    }

    public void editFilterConfig(ConfigInfo filterConfig, TreePath slectedTp) {
        FilterConfigNode tn = (FilterConfigNode)slectedTp.getLastPathComponent();
        ConfigInfo cn = (ConfigInfo) tn.getUserObject();

        cn.enabled = filterConfig.enabled;
        cn.filterItem.caseSensitive = filterConfig.filterItem.caseSensitive;
        cn.filterItem.mMsgPattern = filterConfig.filterItem.mMsgPattern;
        cn.ftColor = filterConfig.ftColor;
        cn.bgColor = filterConfig.bgColor;
        try {
            cn.filterItem.initPattern();
        } catch (Exception e){
            JOptionPane.showMessageDialog(null,e.getMessage());
        }

        //Trigger value changed
        filterConfigTreeModel.valueForPathChanged(slectedTp, cn);
    }

}
