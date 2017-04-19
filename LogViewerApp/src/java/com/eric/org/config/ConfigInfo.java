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

package com.eric.org.config;

import com.eric.org.util.LogFilterItem;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ConfigInfo will represent the filter config
 * when isGroup is true, it is means it is a group folder
 */
public class ConfigInfo implements Transferable, Serializable {
    public static final String DEFAULT_FT_COLOR = "#"+Integer.toHexString(Color.BLACK.getRGB()& 0xffffff);
    public static final String DEFAULT_BG_COLOR = "#"+Integer.toHexString(Color.WHITE.getRGB()& 0xffffff);

    final public static DataFlavor INFO_FLAVOR =
            new DataFlavor(ConfigInfo.class, "ConfigInfo Information");

    static DataFlavor flavors[] = {INFO_FLAVOR };

    public LogFilterItem filterItem;
    public boolean enabled;
    public String match_type;
    public String ftColor;
    public String bgColor;
    public int hitCount;

    private boolean isGroup=true;

    private String mName = null; //Group Name
    private ConfigInfo mParent = null;
    private List<ConfigInfo> mChildren = null;

    public ConfigInfo(String s) {
        mName = s;
        filterItem = new LogFilterItem("", false);
        this.enabled = false;
        this.match_type = "matches_text";
        this.ftColor = DEFAULT_FT_COLOR;
        this.bgColor = DEFAULT_BG_COLOR;
        hitCount = 0;
    }
    private ConfigInfo(boolean enabled, String text, String match_type, boolean case_sensitive, String ftColor, String bgColor) {
        filterItem = new LogFilterItem(text, case_sensitive);
        this.enabled = enabled;
        this.match_type = match_type;
        this.ftColor = ftColor;
        this.bgColor = bgColor;
        hitCount = 0;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public boolean isGroup(){
        return isGroup;
    }

    public String toString() {
        String rst="";
        //For Group, return group Name
        if(!isGroup()&&filterItem!=null){
            rst = filterItem.mMsgPattern;
            if(hitCount > 0)
                rst = rst + " (" + hitCount +")";
        }else{
            rst=mName;
        }

        return rst;
    }

    public void addChild(ConfigInfo info) {
        info.setParent(this);
        if(mChildren==null){
            mChildren = new ArrayList<>();
        }

        mChildren.add(info);
    }
    public void insert(ConfigInfo info, int i) {
        info.setParent(this);
        if(mChildren==null){
            mChildren = new ArrayList<>();
        }

        mChildren.add(i,info);
    }

    public void removeChild(ConfigInfo child){
        mChildren.remove(child);
        child.setParent(null);
    }
    public String getName() {
        return mName;
    }

    public ConfigInfo getParent() {
        return mParent;
    }

    private void setParent(ConfigInfo mParent) {
        this.mParent = mParent;
    }

    public List<ConfigInfo> getChildren() {
        return mChildren;
    }

    //Clear all children
    public void clear() {
        //Termination case
        if(mChildren==null || mChildren.size()==0)
            return;

        //Recursive clear children
        int cnt = mChildren.size();
        for(int i = cnt-1; i > 0; i--){
            ConfigInfo tmp = mChildren.get(i);
            tmp.clear();
        }
        mChildren.clear();
    }

    /*
     * Combine and create a LogPaser Table From sub log filter and personal table
     */
    public List<ConfigInfo> getSubActiveFilterConfigList() {
        List<ConfigInfo> rst = new ArrayList<>();

        if(!isGroup()){
            if(this.enabled)
                rst.add(this);
        }else {
            //Get all filter from sub view list
            if(mChildren == null){
                return null;
            }
            int cnt = mChildren.size();
            for (ConfigInfo aMChildren : mChildren) {
                List<ConfigInfo> tmp = aMChildren.getSubActiveFilterConfigList();
                if (tmp == null) {
                    return rst;
                }
                int cnt2 = tmp.size();
                for (ConfigInfo aTmp : tmp) {
                    rst.add(aTmp);
                }
            }
        }

        return rst;
    }


    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[0];
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(INFO_FLAVOR);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(INFO_FLAVOR)) {
            return this;
        }
        else throw new UnsupportedFlavorException(flavor);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }


}
