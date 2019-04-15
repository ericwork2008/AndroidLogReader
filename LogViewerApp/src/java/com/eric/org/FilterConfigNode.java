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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilterConfigNode extends DefaultMutableTreeNode {
    public FilterConfigNode(ConfigInfo cn) {
        super(cn);
    }

    @Override
    public boolean isLeaf() {
        ConfigInfo cn = (ConfigInfo) getUserObject();

        return !cn.isGroup();
    }

    @Override
    public boolean getAllowsChildren() {
        ConfigInfo cn = (ConfigInfo) getUserObject();

        return cn.isGroup();
    }

    public void add(DefaultMutableTreeNode child, com.eric.org.FilterConfigTreeModel fcm) {
        ConfigInfo childPI = (ConfigInfo) child.getUserObject();

        //Add
        ConfigInfo newParent = (ConfigInfo) getUserObject();
        newParent.addChild(childPI);

        //Add new Node
        fcm.addChildNode(childPI,this);
    }
    public void insert(DefaultMutableTreeNode child, int index, com.eric.org.FilterConfigTreeModel fcm) {
        ConfigInfo childPI = (ConfigInfo) child.getUserObject();

        ConfigInfo destPI = (ConfigInfo) getUserObject();

        //insert
        destPI.insert(childPI,index);

        fcm.insertChildNode(childPI,this,index);
    }
    public void remove(DefaultMutableTreeNode child) {
        super.remove(child);

        ConfigInfo childPI = (ConfigInfo) child.getUserObject();

        ConfigInfo ParentPI = (ConfigInfo) getUserObject();
        if (ParentPI != null) ParentPI.removeChild(childPI);

    }

    public void storeNodeExpendState(JTree tree, FilterConfigNode node) {
        ConfigInfo cn = (ConfigInfo) getUserObject();

        if(tree.isExpanded(getPath(node)))
            cn.isExpended = true;
        else
            cn.isExpended = false;

        for(int index=0; index < getChildCount(); index++) {
            FilterConfigNode fcn = (FilterConfigNode)getChildAt(index);
            fcn.storeNodeExpendState(tree, fcn);
        }
    }

    public void setNodeExpandedState(JTree tree, DefaultMutableTreeNode node) {
        ArrayList<DefaultMutableTreeNode> list = Collections.list(node.children());
        for (DefaultMutableTreeNode treeNode : list) {
            setNodeExpandedState(tree, treeNode);
        }

        TreePath path = new TreePath(node.getPath());
        ConfigInfo cn = (ConfigInfo) getUserObject();
        if(cn.isGroup()){
            if (cn.isExpended) {
                tree.expandPath(path);
            } else {
                tree.collapsePath(path);
            }
        }
    }
    /*
     * uncheck all subChild
     */
    public void unCheckSelfandAllChild() {
        ConfigInfo cn = (ConfigInfo) getUserObject();
        cn.enabled = false;
        for(int index=0; index < getChildCount(); index++) {
            FilterConfigNode fcn = (FilterConfigNode)getChildAt(index);
            fcn.unCheckSelfandAllChild();
        }
    }
    /*
     * Select all subChild leaves
     */
    public void selectAllLeaf() {
        ConfigInfo cn = (ConfigInfo) getUserObject();
        if(!cn.isGroup())
            cn.enabled = true;
        for(int index=0; index < getChildCount(); index++) {
            FilterConfigNode fcn = (FilterConfigNode)getChildAt(index);
            fcn.selectAllLeaf();
        }
    }

    public boolean isAllLeafSelected() {
        ConfigInfo cn = (ConfigInfo) getUserObject();
        if(this.isLeaf() && !cn.enabled)
            return false;

        for(int index=0; index < getChildCount(); index++) {
            FilterConfigNode fcn = (FilterConfigNode)getChildAt(index);
            boolean childSelected = fcn.isAllLeafSelected();
            if(!childSelected)
                return false;
        }
        return true;
    }
    private boolean haveLeafSelected() {
        ConfigInfo cn = (ConfigInfo) getUserObject();
        if(this.isLeaf() && cn.enabled)
            return true;

        for(int index=0; index < getChildCount(); index++) {
            FilterConfigNode fcn = (FilterConfigNode)getChildAt(index);
            boolean childSelected = fcn.haveLeafSelected();
            if(childSelected)
                return true;
        }

        return false;
    }
    public boolean isPartialLeafSelected() {
        return !isAllLeafSelected() && haveLeafSelected();
    }

    public TreePath getPath(TreeNode treeNode) {
        List<Object> nodes = new ArrayList<>();
        if (treeNode != null) {
            nodes.add(treeNode);
            treeNode = treeNode.getParent();
            while (treeNode != null) {
                nodes.add(0, treeNode);
                treeNode = treeNode.getParent();
            }
        }

        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
    }
}
