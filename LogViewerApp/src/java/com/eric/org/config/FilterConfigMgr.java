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

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;

/*Example
<LogViewer showOnlyFilteredLines="True">
   <groups>
        <group name = "IMS Registration">
        <filter enabled="y" excluding="n" ftColor="0000ff" type="matches_text" case_sensitive="n" regex="y" text="ImsSenderRxr.*(?:]&lt;|]&gt;)" />
        <filter enabled="n" excluding="n" type="matches_text" case_sensitive="n" regex="n" text="placeCall" />
        <filter enabled="n" excluding="n" type="matches_text" case_sensitive="n" regex="n" text="PhoneUtils" />
        </group>
    <groups>
</LogViewer>
*/

public class FilterConfigMgr {

    public static com.eric.org.config.ConfigInfo rootConfigInfo = new com.eric.org.config.ConfigInfo("Filter Root");

    public FilterConfigMgr(com.eric.org.FilterConfigTreeModel fm) {

    }


    private static DocumentBuilderFactory dbFactory = null;
    private static DocumentBuilder dBuilder = null;
    private static Document doc = null;

    public FilterConfigMgr() {
    }

    public void loadFilterConfig(File file) {
        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            System.out.println("Root element :"
                    + doc.getDocumentElement().getNodeName());
            System.out.println("==================");

            Node rootNode = doc.getDocumentElement();

            rootConfigInfo.clear();

            loadNode(rootConfigInfo,rootNode);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Filter XML Format Error", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadNode(com.eric.org.config.ConfigInfo parent, Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element) node;
            String nodeName = node.getNodeName();

            if(nodeName.equalsIgnoreCase("filters")){
                Node childNode = node.getFirstChild();
                if(childNode != null){
                    loadNode(parent,childNode);
                }
                assert childNode != null;
                while( childNode.getNextSibling()!=null ){
                    childNode = childNode.getNextSibling();
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        loadNode(parent,childNode);
                    }
                }
            }
            if(nodeName.equalsIgnoreCase("group")){
                String nameAtt = e.getAttribute("name");
                System.out.println("-------Group---------");
                System.out.println("grpName : "+nameAtt);

                com.eric.org.config.ConfigInfo fcg = new com.eric.org.config.ConfigInfo(nameAtt);
                fcg.setGroup(true);

                Node childNode = node.getFirstChild();
                if(childNode != null){
                    loadNode(fcg,childNode);
                    while( childNode.getNextSibling()!=null ){
                        childNode = childNode.getNextSibling();
                        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                            loadNode(fcg,childNode);
                        }
                    }
                }

                parent.addChild(fcg);
            }
            if(nodeName.equalsIgnoreCase("filter")){
                com.eric.org.config.ConfigInfo ci = new com.eric.org.config.ConfigInfo("");
                ci.setGroup(false);

                System.out.println("-----------Filter-----------");
                Element eElement = (Element) node;
                ci.enabled = Boolean.parseBoolean(eElement.getAttribute("enabled"));
                System.out.println("enabled : " + ci.enabled);

                ci.match_type = eElement.getAttribute("type");
                System.out.println("type : " + ci.match_type);

                ci.filterItem.mMsgPattern = eElement.getAttribute("text");
                System.out.println("text : " + ci.filterItem.mMsgPattern);

                ci.filterItem.caseSensitive = Boolean.parseBoolean(eElement.getAttribute("case_sensitive"));
                System.out.println("case_sensitive : " + ci.filterItem.caseSensitive);

                ci.ftColor = eElement.getAttribute("ftColor");
                if(ci.ftColor.isEmpty())
                    ci.ftColor = com.eric.org.config.ConfigInfo.DEFAULT_FT_COLOR;

                System.out.println("ftColor : " + ci.ftColor);

                ci.bgColor = eElement.getAttribute("bgColor");
                if(ci.bgColor.isEmpty())
                    ci.bgColor = com.eric.org.config.ConfigInfo.DEFAULT_BG_COLOR;

                System.out.println("bgColor : " + ci.bgColor);

                ci.filterItem.initPattern();

                parent.addChild(ci);
            }
        }
    }

    public static void saveFilterConfig(File file) {
        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.newDocument();

            // root element
            Element rootElement = doc.createElement("filters");
            doc.appendChild(rootElement);

            // setting attribute to element
            Attr attr = doc.createAttribute("showOnlyFilteredLines");
            attr.setValue("true");
            rootElement.setAttributeNode(attr);

            saveNode(rootConfigInfo,rootElement);

            // write the content into xml file
            TransformerFactory transformerFactory =
                    TransformerFactory.newInstance();
            Transformer transformer =
                    transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);

            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
            // Output to console for testing
            StreamResult consoleResult =
                    new StreamResult(System.out);
            transformer.transform(source, consoleResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void saveNode(com.eric.org.config.ConfigInfo parent, Node node) {
        if(parent == null){
            System.out.println("saveNode: parent == null");
            return;
        }
        List<ConfigInfo> childs = parent.getChildren();
        if(childs == null)
            return;

        for (com.eric.org.config.ConfigInfo cn : childs) {
            if(cn.isGroup()){
                Element group = doc.createElement("group");

                Attr name = doc.createAttribute("name");
                name.setValue(String.valueOf(cn.getName()));
                group.setAttributeNode(name);

                saveNode(cn,group);

                node.appendChild(group);
            }else{
                Element filter = doc.createElement("filter");

                Attr enabled = doc.createAttribute("enabled");
                enabled.setValue(String.valueOf(cn.enabled));
                filter.setAttributeNode(enabled);

                Attr type = doc.createAttribute("type");
                type.setValue(cn.match_type);
                filter.setAttributeNode(type);

                Attr text = doc.createAttribute("text");
                text.setValue(cn.filterItem.mMsgPattern);
                filter.setAttributeNode(text);

                Attr case_sensitive = doc.createAttribute("case_sensitive");
                case_sensitive.setValue(String.valueOf(cn.filterItem.caseSensitive));
                filter.setAttributeNode(case_sensitive);

                Attr ftColor = doc.createAttribute("ftColor");
                ftColor.setValue(cn.ftColor);
                filter.setAttributeNode(ftColor);

                Attr bgColor = doc.createAttribute("bgColor");
                bgColor.setValue(cn.bgColor);
                filter.setAttributeNode(bgColor);

                node.appendChild(filter);
            }
        }
    }

    private static void unCheckChildFilter(com.eric.org.config.ConfigInfo parent) {
        if((parent == null)||parent.getChildren() ==null) return;

        for (com.eric.org.config.ConfigInfo cn : parent.getChildren()) {
            if(cn.isGroup()){
                unCheckChildFilter(cn);
            }else{
                cn.enabled=false;
            }
        }
    }

    @SuppressWarnings("SameReturnValue")
    public boolean getItemChecked(int row) {
        return true;
    }

    static public List<com.eric.org.config.ConfigInfo> getActiveFilterList(){
        return rootConfigInfo.getSubActiveFilterConfigList();
    }


    public static void disableAllFilter() {
        unCheckChildFilter(rootConfigInfo);
    }
}
