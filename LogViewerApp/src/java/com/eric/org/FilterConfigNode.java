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

import javax.swing.tree.DefaultMutableTreeNode;

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
}
