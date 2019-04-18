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

import java.util.List;
import java.util.regex.Matcher;

public class LogFilterHelper {
    /*
     * Get Filter Item from a Filter Table
     */
    public static LogFilterItem getShottedItemFromFilterTable(LogLine ll, List<LogFilterItem> filterTable){
        LogFilterItem lfi = null;
        boolean interesting = false;
        int tc = filterTable.size();
        for (LogFilterItem aFilterTable : filterTable) {
            lfi = aFilterTable;

            Matcher m = lfi.mPatten.matcher(ll.line);
            if (m.find()) {
                interesting = true;
                break;
            }
        }
        if(!interesting) lfi = null;

        return lfi;
    }


    public static LogFilterItem getItemFromFilterTablewithTag(LogLine ll, List<LogFilterItem> filterTable){
        LogFilterItem lfi = null;
        boolean interesting = false;
        int tc = filterTable.size();
        for (LogFilterItem aFilterTable : filterTable) {
            lfi = aFilterTable;

            Matcher m = lfi.mPatten.matcher(ll.msg);
            if (m.find()) {
                interesting = true;
                break;
            }
        }
        if(!interesting) lfi = null;

        return lfi;
    }
}
