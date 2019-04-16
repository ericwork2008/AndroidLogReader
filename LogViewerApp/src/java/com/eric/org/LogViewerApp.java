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

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import com.eric.org.config.FilterConfigMgr;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.prefs.Preferences;

class LogViewerApp extends JFrame {
    private static Preferences prefs;

    private JLabel statusLabel = null;
    private com.eric.org.LogTableMgr logTableMgr;

    private com.eric.org.FilterTreeManager filterTreeManager = FilterTreeManager.getInstance();

    static String  adbPath = "";

    private LogViewerApp() {
        try {
            Application application = Application.getApplication();

            java.net.URL imgURL = getClass().getClassLoader().getResource("icon.jpeg");
            if (imgURL != null) {
                ImageIcon icon = new ImageIcon(imgURL);
                application.setDockIconImage(icon.getImage());
            }

            // add our adapter as a listener on the application object
            application.addApplicationListener(new ApplicationAdapterImp(this));

        } catch (Exception e1) {
////            LOGGER.warn("Can't load icon: " + e1.getMessage());
        }

        createUI();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        //Android Device
        AndroidDebugBridge.init(false);

        AndroidDebugBridge debugBridge = AndroidDebugBridge.createBridge(adbPath, true);
        if (debugBridge == null) {
            System.err.println("Invalid ADB location.");
            System.exit(1);
        }

        AndroidDebugBridge.addDeviceChangeListener(new DeviceChangeListener());

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent winEvt) {
                logTableMgr.saveCapturedLog();
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        prefs = Preferences.userNodeForPackage(
                LogViewerApp.class).node(
                LogViewerApp.class.getName().replaceFirst(".*\\.", ""));

        String version = System.getProperty("java.version");
        String mainver = version;
        if (version.startsWith("1.")) {
            mainver = version.substring(2);
        }

        if (mainver.charAt(0) < '8') {
            System.out.println("Java version have to be at least 1.8 or 8.x, you version is " + version);
            JOptionPane.showMessageDialog(null, "Java version have to at least 1.7, your version is " + version, "Java is too old", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isMacOs = osName.startsWith("mac os x");

        try{
            adbPath = System.getenv("ANDROID_HOME");
        }catch (Exception e){
            adbPath = "";
        }
        if (adbPath == null || adbPath.isEmpty())
            adbPath = System.getProperty("user.home");
        if (isMacOs) {
            adbPath += "/Library/Android/sdk/platform-tools/adb";
        }else{
            adbPath += "\\AppData\\Local\\Android\\sdk\\platform-tools\\adb";
        }

        if (isMacOs) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "LogViewer");
            System.setProperty("apple.awt.application.name", "LogViewer");
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
            e.printStackTrace();
        }
        LogViewerApp simple = new LogViewerApp();

        simple.setVisible(true);
    }

    private void createUI() {
        JMenuBar menubar = createMenuBar();
        setJMenuBar(menubar);

        //Log View
        logTableMgr = new com.eric.org.LogTableMgr();
        JScrollPane logViewPanel = logTableMgr.createLogView();

        //Filter Panel
        JComponent filterPanel = filterTreeManager.createFilterPanel();

        //Attach logTableMgr then filter tree can update log model data
        filterTreeManager.attachLogTable(logTableMgr);

        //Create a split pane with the two scroll panes in it.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                logViewPanel, filterPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(500);

        //Provide minimum sizes for the two components in the split pane
        Dimension minimumSize = new Dimension(100, 50);
        logViewPanel.setMinimumSize(minimumSize);
        filterPanel.setMinimumSize(minimumSize);

        add(splitPane, BorderLayout.CENTER);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //Add Status bar
        JPanel statusPanel = new JPanel();
        add(statusPanel, BorderLayout.SOUTH);
        statusPanel.setPreferredSize(new Dimension(getWidth(), 16));
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusLabel = new JLabel("ADB Device status");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusPanel.add(statusLabel);

        pack();
        Toolkit toolkit = getToolkit();
        Dimension size = toolkit.getScreenSize();
        setSize(size);

        setTitle("LogViewer");

        setLocation(size.width / 2 - getWidth() / 2,
                size.height / 2 - getHeight() / 2);


        setLocationRelativeTo(null);

        // Connect the label with a drag and drop listener
        new DropTarget(logViewPanel, new FileDragDropListener(logTableMgr));
    }

    private JMenuBar createMenuBar() {
        //====== File =====
        JMenu fileMenu = new JMenu("File");

        JMenuItem openItem = new JMenuItem("Open...");
        openItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                openFile();
            }
        });

        JMenuItem mergeItem = new JMenuItem("Merge Android Adb Log...");
        mergeItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mergeFile();
            }
        });

        JMenuItem closeItem = new JMenuItem("Close", null);
        closeItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }
        });

        fileMenu.add(openItem);
        fileMenu.add(mergeItem);
        fileMenu.addSeparator();
        fileMenu.add(closeItem);

        //====== Search =====
        JMenu searchMenu = new JMenu("Edit");
        JMenuItem findItem = new JMenuItem("Find...");
        findItem.setMnemonic(KeyEvent.VK_F);
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        findItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                new com.eric.org.FindDlg(logTableMgr).setVisible(true);
            }
        });

        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.setMnemonic(KeyEvent.VK_C);
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        copyItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                logTableMgr.copySelectionToClipboard();
            }
        });

        searchMenu.add(findItem);
        searchMenu.add(copyItem);

        //====== Log Setting =====
        JMenu viewMenu = new JMenu("Capture Log Setting");
        JCheckBoxMenuItem captureLogCheck = new JCheckBoxMenuItem("Capture Log Automatically");
        captureLogCheck.setMnemonic(KeyEvent.VK_L);
        captureLogCheck.setDisplayedMnemonicIndex(5);
        captureLogCheck.setSelected(true);
        captureLogCheck.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {

                if (e.getStateChange() == ItemEvent.SELECTED) {
                    logTableMgr.isAutoCaptureLog = true;
//                    toolbar.setVisible(true);
                } else {
//                    toolbar.setVisible(false);
                    logTableMgr.isAutoCaptureLog = false;
                }
            }
        });
        viewMenu.add(captureLogCheck);

        //====== Filter =====
        JMenu toolsMenu = new JMenu("Filter");
        JMenuItem openFilterItem = new JMenuItem("Load Filter");
        openFilterItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                loadFilterFile();
            }
        });

        JMenuItem saveFilterItem = new JMenuItem("Save Filter");
        saveFilterItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                saveFilterFile();
            }
        });

        JMenuItem hideFilterItem = new JMenuItem("Hide/Unhide Filter");
        hideFilterItem.setMnemonic(KeyEvent.VK_S);
        hideFilterItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        hideFilterItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                logTableMgr.tangleFilter();
            }
        });
        toolsMenu.add(openFilterItem);
        toolsMenu.add(saveFilterItem);
        toolsMenu.add(hideFilterItem);


        //====== About =====
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");

        aboutItem.addActionListener(new ActionListener() {
            final String APP_VERSION = "0.9";
            public void actionPerformed(ActionEvent event) {
                String aboutMsg = "Text Log Analysis for Android\n CopyWrite EricNiu 2016 \n\n Version "+ APP_VERSION + "\n";
                aboutMsg += "Now we have PlugIn List : \n" +
                        "---" + logTableMgr.getPlugInMgr().getPlugInList();
                JOptionPane.showMessageDialog(getContentPane(), aboutMsg);
            }
        });
        helpMenu.add(aboutItem);

        JMenuBar menubar = new JMenuBar();

        menubar.add(fileMenu);
        menubar.add(searchMenu);
        menubar.add(viewMenu);
        menubar.add(toolsMenu);
        menubar.add(helpMenu);

        return menubar;
    }

    /**
     * Load Filter File
     */
    private void loadFilterFile() {
        File file = null;

        FileDialog fDialog = new FileDialog(this, "Load", FileDialog.LOAD);
        fDialog.setFilenameFilter(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });
        String filterDirStr = prefs.get("filterDir", null);
        if (filterDirStr == null)
            filterDirStr = "./";

        File filterDir = new File(filterDirStr);
        if (filterDir.isDirectory() && filterDir.canRead()) {
            fDialog.setDirectory(filterDirStr);
        }

        fDialog.setVisible(true);
        fDialog.setModal(true);
        String filename = fDialog.getFile();
        if (filename == null) {
            System.out.println("You cancelled the choice");
        } else {
            String filePath = fDialog.getDirectory();
            prefs.put("filterDir", filePath);
            System.out.println("You chose " + filePath + "/" + filename);
            String path = fDialog.getDirectory() + fDialog.getFile();
            file = new File(path);

            filterTreeManager.loadFile(file);

            logTableMgr.applyFilter();
        }
    }

    /**
     * Save Filter File
     */
    private void saveFilterFile() {
        File file = null;
        FileDialog fDialog = new FileDialog(this, "Save", FileDialog.SAVE);
        fDialog.setFilenameFilter(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });
        String filterDirStr = prefs.get("filterDir", null);
        if (filterDirStr == null)
            filterDirStr = "./";

        File filterDir = new File(filterDirStr);
        if (filterDir.isDirectory() && filterDir.canRead()) {
            fDialog.setDirectory(filterDirStr);
        }

        fDialog.setVisible(true);
        fDialog.setModal(true);
        String filename = fDialog.getFile();
        if (filename == null) {
            System.out.println("You cancelled the choice");
        } else {
            String filePath = fDialog.getDirectory();
            prefs.put("filterDir", filePath);
            System.out.println("You chose " + filePath + "/" + filename);
            String path = fDialog.getDirectory() + fDialog.getFile();
            file = new File(path);

            FilterConfigMgr.saveFilterConfig(file);
        }
    }

    /**
     * Open Log file
     */
    private void openFile() {
        File[] files = chooseFile();
        if (files == null) {
            System.out.println("You cancelled the choice");
        } else {
            logTableMgr.loadLogFile(files[0]);
            setTitle(files[0].getAbsolutePath());
        }
    }

    /**
     * Merge Two Android Log file
     */
    private void mergeFile() {
        File[] files = chooseFile();
        if (files == null || files.length <= 0) {
            System.out.println("You cancelled the choice");
        } else {
            logTableMgr.mergeLogFile(files);
        }
    }

    private File[] chooseFile() {
        FileDialog fDialog = new FileDialog(this, "Load", FileDialog.LOAD);

        fDialog.setFilenameFilter(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".txt") || name.endsWith(".log");
            }
        });

        String logDirStr = prefs.get("logDir", null);
        if (logDirStr == null)
            logDirStr = "./";

        File logDir = new File(logDirStr);
        if (logDir.isDirectory() && logDir.canRead()) {
            fDialog.setDirectory(logDirStr);
        }

        fDialog.setMultipleMode(true);
        fDialog.setVisible(true);
        fDialog.setModal(true);
        File[] files = fDialog.getFiles();
        String filePath = fDialog.getDirectory();
        prefs.put("logDir", filePath);
        return files;
    }

    /**
     *  Drap Drop  Open File
     * @param e
     */
    // our "callback" method. this method is called by the OpenFileHandlerImp
    // when a "handleOpenFile" event is received.
    private void handleOpenFileEvent(ApplicationEvent e) {
//        java.io.File[] files = (File[]) e.getFilename();
        File file = new File(e.getFilename());
        logTableMgr.loadLogFile(file);
    }

    class FileDragDropListener implements DropTargetListener {
        com.eric.org.LogTableMgr mLogTableMgr;

        public FileDragDropListener(com.eric.org.LogTableMgr logTableMgr) {
            this.mLogTableMgr = logTableMgr;
        }

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {

        }

        @Override
        public void dragOver(DropTargetDragEvent dtde) {

        }

        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {

        }

        @Override
        public void dragExit(DropTargetEvent dte) {

        }

        @Override
        public void drop(DropTargetDropEvent event) {
            // Accept copy drops
            event.acceptDrop(DnDConstants.ACTION_COPY);

            // Get the transfer which can provide the dropped item data
            Transferable transferable = event.getTransferable();

            // Get the data formats of the dropped item
            DataFlavor[] flavors = transferable.getTransferDataFlavors();

            // Loop through the flavors
            for (DataFlavor flavor : flavors) {
                try {
                    // If the drop items are files
                    if (flavor.isFlavorJavaFileListType()) {
                        // Get all of the dropped files
                        @SuppressWarnings("unchecked")
                        java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(flavor);

                        mLogTableMgr.loadLogFile(files.get(0));

                        setTitle(files.get(0).getAbsolutePath());
                    }
                } catch (Exception e) {
                    // Print out the error stack
                    e.printStackTrace();
                }
            }

            // Inform that the drop is complete
            event.dropComplete(true);
        }
    }

    /**
     * Extend the Mac OS X ApplicationAdapter class, and just implement the
     * handleOpenFile() method so we can handle drag and drop events.
     */
    class ApplicationAdapterImp extends ApplicationAdapter {
        private LogViewerApp handler;

        // the main class passes a reference to itself to us when we are constructed
        public ApplicationAdapterImp(LogViewerApp handler) {
            this.handler = handler;
        }

        public void handleOpenFile(ApplicationEvent var1) {
            handler.handleOpenFileEvent(var1);
        }
    }

    /**
     * Android Device connect
     */
    class DeviceChangeListener implements IDeviceChangeListener {
        @Override
        public void deviceChanged(IDevice device, int arg1) {
            // not implement
        }

        @Override
        public void deviceConnected(IDevice device) {
            System.out.println(String.format("%s connected", device.getSerialNumber()));
            statusLabel.setText("ADB Device connected:" + device.getSerialNumber());
            logTableMgr.captureLog();
        }

        @Override
        public void deviceDisconnected(IDevice device) {
            System.out.println(String.format("%s disconnected", device.getSerialNumber()));
            statusLabel.setText("ADB Device disconnected");
            logTableMgr.stopCaptureLog();
        }
    }

}
