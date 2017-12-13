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

public class htmlHelper {
    public final static String head = "<head><style>\n" +
            "table, th, td {\n" +
            "    border: 1px solid black;\n" +
            "    border-collapse: collapse;\n" +
            "}\n" +
            "table#t01 tr:nth-child {\n" +
                "background-ftColor:#fff;\n" +
            "}\n" +
            "table#t01 th {\n"+
                "background-ftColor: #E1F5A9;\n"+
                "ftColor: black;\n"+
            "}\n"+
            "</style></head>";
    public final static String htmlHeader = "<html>"+head;
    public final static String htmlEnd = "</html>";
    public final static String title = "<tr>\n" +
            "    <th align=\"left\">Check Point</th>\n" +
            "    </tr>\n";
    public  final static String tableHeader = "<table id=\"t01\" border=\"1\">";
    public  final static String tableEnd = "</table>";
    public final static String bodyHeader = "<body>";
    public  final static String bodyEnd = "</body>";

    public static String buildCheckPoint(String checkPoint) {
        String rstStr =  htmlHelper.htmlHeader+htmlHelper.bodyHeader+
                tableHeader + title +
                "<tr><td>" + checkPoint + "</td> </tr>" + tableEnd+
                htmlHelper.bodyEnd + htmlHelper.htmlEnd;

        return rstStr;
    }
}
