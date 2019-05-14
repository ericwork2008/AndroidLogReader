package com.eric.org;
import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

public class PreferenceSetting {
    private static Preferences prefs = null;
    static {
        prefs = Preferences.userNodeForPackage(
                LogViewerApp.class).node(
                LogViewerApp.class.getName().replaceFirst(".*\\.", ""));
    }

    public static String getFilterDir() {
        String filterDirStr = prefs.get("filterDir", null);
        if (filterDirStr == null)
            filterDirStr = ".";
        return filterDirStr;
    }
    public static void setFilterDir(String filePath) {
        if(filePath != null)
            prefs.put("filterDir", filePath);
    }

    public static String getLogDir() {
        String logDirStr = prefs.get("logDir", null);
        if (logDirStr == null)
            logDirStr = ".";
        return logDirStr;
    }
    public static void setLogDir(String filePath) {
        if(filePath != null)
            prefs.put("logDir", filePath);
    }

    public static boolean getAutoCaptureLog() {
        boolean isAutoCapture =false;
        String autoLogStr = prefs.get("autoCaptureLog", null);
        if (autoLogStr!=null && autoLogStr.equalsIgnoreCase("true"))
            isAutoCapture = true;
        return isAutoCapture;
    }
    public static void setAutoCaptureLog(boolean autoCapture) {
        if(autoCapture)
            prefs.put("autoCaptureLog", "true");
        else
            prefs.put("autoCaptureLog", "false");
    }

    public Image getAppIcon(){
        Image img = new ImageIcon(LogViewerApp.class.getResource("icon.jpeg")).getImage();
        return img;
    }
}
