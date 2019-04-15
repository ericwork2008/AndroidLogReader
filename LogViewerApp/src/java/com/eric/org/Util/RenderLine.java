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

package com.eric.org.Util;

import com.eric.org.util.LogLine;

import java.awt.Color;

/**
 * Represent the Line which will be render on the screen.
 * RenderLine have the properties which have the passed checkpoint string, etc.
 */
public class RenderLine {
    private LogLine mLogline;

    private int mOrgLineNumber; // Original line number

    //CheckPointStr is the output by log plugin parser.
    private String mCheckPointStr;
    private boolean haveParserResult = false;

    public String getFtColor() {
        return ftColor;
    }

    private String ftColor = "#"+Integer.toHexString(Color.BLACK.getRGB()& 0xffffff);

    public String getBgColor() {
        return bgColor;
    }

    private String bgColor = "#"+Integer.toHexString(Color.WHITE.getRGB()& 0xffffff);

    public boolean isFilterShotted() {
        return filterShotted;
    }

    public void setFilterShotted(boolean filterShotted, String ftColor, String bgColor) {
        this.filterShotted = filterShotted;
        this.ftColor=ftColor;
        this.bgColor = bgColor;
    }

    private boolean filterShotted = true;

    public RenderLine(LogLine ll, int ln) {
        mLogline = ll;
        mOrgLineNumber = ln;
        mCheckPointStr = null;
        haveParserResult = false;
    }
    public RenderLine(String chkp) {
        mLogline = null;
        mOrgLineNumber = -1;
        mCheckPointStr = chkp;
        haveParserResult = false;
    }

    public LogLine getLogline() {
        return mLogline;
    }

    public int getOrgLineNo(){
        return mOrgLineNumber;
    }


    public String getChkPointStr() {
        return mCheckPointStr;
    }
    public boolean isParsed() {
        return haveParserResult;
    }

    public void setParsed() {
        haveParserResult = true;
    }
    public void setChkPointStr(String checkpoint) {
        mCheckPointStr = checkpoint;
    }
}
