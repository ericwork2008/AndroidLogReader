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

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class FilterConfigTreeModel extends DefaultTreeModel {
    private santhosh.CheckTreeSelectionModel mTreeSelectionModel;

    public FilterConfigTreeModel(TreeNode nd) {
        super(nd);
        FilterConfigNode root = (FilterConfigNode)this.getRoot();
        root.removeAllChildren();
        this.reload();
    }

    /*
     * Rebuild tree node from the full filter config tree
     */
    public void rebuildTreeNode() {
        FilterConfigNode root = FilterTreeManager.getInstance().getRoot();
        root.removeAllChildren();

        if(FilterConfigMgr.rootConfigInfo.getChildren() != null) {
            for (ConfigInfo ci : FilterConfigMgr.rootConfigInfo.getChildren()) {
                addChildNode(ci,root);
            }
        }

    }

    public void reCreateTreeNodes() {
        FilterConfigNode root = FilterTreeManager.getInstance().getRoot();
        root.removeAllChildren();

        mTreeSelectionModel.clearSelection();
        for (ConfigInfo ci : FilterConfigMgr.rootConfigInfo.getChildren()) {
            addChildNode(ci,root);
        }

        this.setRoot(root);
        this.reload();
    }
    public void updateTreeSelection() {
        mTreeSelectionModel.clearSelection();
        FilterConfigNode root = (FilterConfigNode)this.getRoot();
        updateNodeSelection(root);
    }
    public void updateNodeSelection(FilterConfigNode parentNode) {
        for (int index = 0; index<parentNode.getChildCount(); index++) {
            FilterConfigNode nd = (FilterConfigNode)parentNode.getChildAt(index);
            ConfigInfo ci = (ConfigInfo)nd.getUserObject();
            if(ci.enabled)
                mTreeSelectionModel.addSelectionPath(getPath(nd));

            updateNodeSelection(nd);
        }
    }
    public void insertChildNode(ConfigInfo configIf, FilterConfigNode parentNode, int index) {
        FilterConfigNode tmp = new FilterConfigNode(configIf);
        parentNode.insert(tmp,index);

        if(configIf.isGroup()){
            //Add Grp
            if(configIf.getChildren()==null)
                return;

            for (ConfigInfo ci : configIf.getChildren()) {
                addChildNode(ci,tmp);
            }
        }

        if(configIf.enabled)
            mTreeSelectionModel.addSelectionPath(getPath(tmp));
//        else //Only added it into the selection Model. We haven't remove logic
//            mTreeSelectionModel.removeSelectionPath(getPath(tmp));
    }

    //Add a node of configIf under parentNode
    public void addChildNode(ConfigInfo configIf, DefaultMutableTreeNode parentNode) {
        FilterConfigNode tmp = new FilterConfigNode(configIf);
        parentNode.add(tmp);

        if(configIf.isGroup()){
            //Add Grp
            if(configIf.getChildren()==null)
                return;

            for (ConfigInfo ci : configIf.getChildren()) {
                addChildNode(ci,tmp);
            }
        }

        if(configIf.enabled)
            mTreeSelectionModel.addSelectionPath(getPath(tmp));
//        else
//            mTreeSelectionModel.removeSelectionPath(getPath(tmp));

    }


    private TreePath getPath(TreeNode treeNode) {
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

    public void setTreeSelectionModel(santhosh.CheckTreeSelectionModel tsm){
        mTreeSelectionModel = tsm;
    }


    public void removeFromParent(FilterConfigNode tn) {
        ConfigInfo cn = (ConfigInfo) tn.getUserObject();
        cn.getParent().removeChild(cn);

        this.removeNodeFromParent(tn);
    }


    public void attach(FilterConfigMgr fc) {
        FilterConfigMgr fc1 = fc;
    }



}
