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
import com.eric.org.util.PlugIn;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Vector;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
 
public class PlugInMgr {
    private static final String EXTERNAL_PLUGINS_ALT_DIR_NAME = "external";
    private static final String PRIVATE_DIR_NAME = ".LogViewer";
    /** The list of installed plugins */
    private Vector<PlugIn> mPlugins = new Vector<>();

    public PlugInMgr() {
        loadPlugins();
    }

    private void addPlugin(PlugIn plugin) {
        mPlugins.add(plugin);
    }

    private void loadPlugins(){
//        addPlugin(new ImsParserPlugIn());

        loadExternalPlugins();
    }

    public String getParsedResult(LogLine line){
        String rst = "";
        for (PlugIn pi: mPlugins) {
            rst = pi.getPaseredResult(line);
        }
        return rst;
    }

    private PlugIn loadPlugin(String className) {
        try {
            Class<?> cls = Class.forName(className);
            return (PlugIn)cls.newInstance();
        } catch (Throwable e) {
//            printErr(1, "Failed to load plugin: " + className);
            return null;
        }
    }

    private File[] getPluginDirs() {
        File homeDir = new File(System.getProperty("user.home"));
        return new File[]{
                new File(homeDir, PRIVATE_DIR_NAME),
                new File(".", EXTERNAL_PLUGINS_ALT_DIR_NAME)
        };
    }

    @SuppressWarnings("ThrowablePrintedToSystemOut")
    private void loadExternalJavaPlugin(File jar) throws Exception {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jar))) {// Note: printOut will not work here, since a listener is not set yet
            System.out.println("Loading plugins from: " + jar.getAbsolutePath());
            Manifest mf = jis.getManifest();
            if (mf != null) {
                String pluginClassName = mf.getMainAttributes().getValue("LogViewer-Plugin");
                if (pluginClassName != null) {
                    System.out.println("pluginClassName: " + pluginClassName);
                    try {
                        URL[] urls = {jar.toURI().toURL()};
                        URLClassLoader cl = new URLClassLoader(urls, getClass().getClassLoader());
                        Class<?> extClass = Class.forName(pluginClassName, true, cl);
                        PlugIn ext = (PlugIn) extClass.newInstance();
                        ext.load();

                        addPlugin(ext);
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }
        }
    }
    private void loadExternalPlugins() {

        File[] pluginDirs = getPluginDirs();
        for (File pluginDir : pluginDirs) {
            if (pluginDir.exists() && pluginDir.isDirectory()) {
                String[] files = pluginDir.list();
                for (String fn : files) {
                    File f = new File(pluginDir, fn);
                    try {
                        if (fn.endsWith(".jar")) {
                            loadExternalJavaPlugin(f);
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading external plugin: " + f.getAbsolutePath());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String getPlugInList() {
        String plugInList = "";
        for (PlugIn pi:mPlugins) {
            plugInList += pi.getName() + "\n";
        }
        return plugInList;
    }
}
