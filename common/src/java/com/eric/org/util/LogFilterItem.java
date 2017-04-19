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

package com.eric.org.util;

import java.io.Serializable;
import java.util.regex.Pattern;


public class LogFilterItem implements Serializable {
    public boolean caseSensitive;
    public String mMsgPattern;
    public Pattern mPatten;

    public LogFilterItem(String mMsgPattern, boolean caseSensitive) {
        this.mMsgPattern = mMsgPattern;
        this.caseSensitive = caseSensitive;
        if(caseSensitive)
            mPatten = Pattern.compile(mMsgPattern);
        else
            mPatten = Pattern.compile(mMsgPattern,Pattern.CASE_INSENSITIVE);
    }

    public LogFilterItem() {

    }

    public void initPattern(){
        if(caseSensitive)
            mPatten = Pattern.compile(mMsgPattern);
        else
            mPatten = Pattern.compile(mMsgPattern,Pattern.CASE_INSENSITIVE);
    }

    /*
     * This function post process the msg analysis according to the pattern
     * When there is style change, return true
     */
    @SuppressWarnings("SameReturnValue")
    public String getParsedResult(LogLine sl) {
        return null;
    }


    public String toString(){
        return mMsgPattern;
    }
}
