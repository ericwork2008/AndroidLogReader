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

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class FilterTreeListener extends MouseAdapter implements
        TreeModelListener, ActionListener,
        TreeSelectionListener,
        DragGestureListener, DropTargetListener,
        DragSourceListener, MouseWheelListener,MouseMotionListener{
    private com.eric.org.FilterConfigTreeModel fcm;
    private com.eric.org.LogTableMgr logTableMgr;
    private JTree tree = null;
    private int hotspot = new JCheckBox().getPreferredSize().width;

    private TreePath SelectedTreePath = null;
    private com.eric.org.FilterConfigNode SelectedNode = null;

    /**
     * Variables needed for DnD
     */
    private DragSource dragSource = null;
    private DragSourceContext dragSourceContext = null;

    public FilterTreeListener(boolean dig) {
        this.tree = FilterTreeManager.getInstance().getFilterConfigtree();
        this.fcm = FilterTreeManager.getInstance().getFilterTreeModel();


        dragSource = DragSource.getDefaultDragSource();

        DragGestureRecognizer dgr = dragSource.createDefaultDragGestureRecognizer(
                this.tree,                             //DragSource
                DnDConstants.ACTION_MOVE, //specifies valid actions
                this                              //DragGestureListener
        );


        /* Eliminates right mouse clicks as valid actions - useful especially
         * if you implement a JPopupMenu for the JTree
         */
        dgr.setSourceActions(dgr.getSourceActions() & ~InputEvent.BUTTON3_MASK);

        /* First argument:  Component to associate the target with
         * Second argument: DropTargetListener
         */
        DropTarget dropTarget = new DropTarget(this.tree, this);

        //unnecessary, but gives FileManager look
//        putClientProperty("JTree.lineStyle", "Angled");
//        MetalTreeUI ui = (MetalTreeUI) getUI();
    }

    private void treeChanged() {
        tree.treeDidChange();
    }

    private VetoableChangeListener changeListener = new VetoableChangeListener() {

        @Override
        public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
            logTableMgr.applyFilter();
        }
    };

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        com.eric.org.FilterConfigNode node;
        node = (com.eric.org.FilterConfigNode)
                (e.getTreePath().getLastPathComponent());

        /*
         * If the event lists children, then the changed
         * node is the child of the node we have already
         * gotten.  Otherwise, the changed node and the
         * specified node are the same.
         */
        try {
            int index = e.getChildIndices()[0];
            node = (com.eric.org.FilterConfigNode)
                    (node.getChildAt(index));
        } catch (NullPointerException exc) {
        }

        System.out.println("The user has finished editing the node.");
        System.out.println("New value: " + node.getUserObject());

        if (changeListener != null) {
            try {
                changeListener.vetoableChange(new PropertyChangeEvent(this, "filter changed", null, null));
            } catch (PropertyVetoException pe) {
                pe.printStackTrace();
            }
        }

    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {

    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {

    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {

    }

    public void attachLogModel(com.eric.org.LogTableMgr logTableMgr) {
        this.logTableMgr = logTableMgr;

        this.logTableMgr.addLogContentChangeListener(logchangeListener);
    }

    //When Log data changed, Filter view need show how many log line matched.
    private com.eric.org.LogTableListener logchangeListener = new com.eric.org.LogTableListener() {
        @Override
        public void onContentChanged() {
            tree.repaint();
        }
    };

    @SuppressWarnings("ThrowablePrintedToSystemOut")
    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        //Get the selected node
        com.eric.org.FilterConfigNode dragNode = getSelectedNode();
        if (dragNode != null) {

            //Get the Transferable Object
            Transferable transferable = (Transferable) dragNode.getUserObject();
            /* ********************** CHANGED ********************** */

            //Select the appropriate cursor;
            Cursor cursor = DragSource.DefaultCopyNoDrop;
            int action = dge.getDragAction();
            if (action == DnDConstants.ACTION_MOVE)
                cursor = DragSource.DefaultMoveDrop;


            //In fact the cursor is set to NoDrop because once an action is rejected
            // by a dropTarget, the dragSourceListener are no more invoked.
            // Setting the cursor to no drop by default is so more logical, because
            // when the drop is accepted by a component, then the cursor is changed by the
            // dropActionChanged of the default DragSource.
            /* ****************** END OF CHANGE ******************** */

            //begin the drag
            try {
                dragSource.startDrag(dge, cursor, transferable, this);
            } catch (Exception e) {
                System.out.print(e);
            }

        }
    }

    /**
     * Convenience method to test whether drop location is valid
     *
     * @param destination The destination path
     * @param dropper     The path for the node to be dropped
     * @return null if no problems, otherwise an explanation
     */
    private String testDropTarget(TreePath destination, TreePath dropper) {
        //Typical Tests for dropping

        //Test 1.
        boolean destinationPathIsNull = destination == null;
        if (destinationPathIsNull)
            return "Invalid drop location.";

        //Test 2. will insert before the destination
//        FilterConfigNode node = (FilterConfigNode) destination.getLastPathComponent();
//        if ( !node.getAllowsChildren() )
//            return "This node does not allow children";

        if (destination.equals(dropper))
            return "Destination cannot be same as source";

        //Test 3.
        if (dropper.isDescendant(destination))
            return "Destination node cannot be a descendant.";

        //Test 4.
        if (dropper.getParentPath().equals(destination))
            return "Destination node cannot be a parent.";

        return null;
    }

    @Override
    public void dragEnter(DragSourceDragEvent dsde) {

    }

    @Override
    public void dragOver(DragSourceDragEvent dsde) {

    }

    @Override
    public void dropActionChanged(DragSourceDragEvent dsde) {

    }

    @Override
    public void dragExit(DragSourceEvent dse) {

    }

    @Override
    public void dragDropEnd(DragSourceDropEvent dsde) {

    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragOver(DropTargetDragEvent event) {

        // Get the transfer which can provide the dropped item data
        Transferable tr = event.getTransferable();

        //flavor not supported, reject drop
        if (!tr.isDataFlavorSupported(ConfigInfo.INFO_FLAVOR)) {
            event.rejectDrag();
            return;
        }


        //set cursor location. Needed in setCursor method
        Point cursorLocationBis = event.getLocation();
        TreePath destinationPath =
                tree.getPathForLocation(cursorLocationBis.x, cursorLocationBis.y);
        if (destinationPath == null) {
            destinationPath = new TreePath(tree.getModel().getRoot());
        }
        SelectedTreePath = tree.getSelectionPath();
        // if destination path is okay accept drop...
        if (testDropTarget(destinationPath, SelectedTreePath) == null) {
            event.acceptDrag(DnDConstants.ACTION_MOVE);
        } else {
            event.rejectDrag();
        }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public void drop(DropTargetDropEvent e) {
        try {
            Transferable tr = e.getTransferable();

            //flavor not supported, reject drop
            if (!tr.isDataFlavorSupported(ConfigInfo.INFO_FLAVOR)) {
                e.rejectDrop();
                return;
            }

            //cast into appropriate data type
            ConfigInfo childInfo = (ConfigInfo) tr.getTransferData(ConfigInfo.INFO_FLAVOR);

            //get new parent node
            Point loc = e.getLocation();
            TreePath destinationPath = tree.getPathForLocation(loc.x, loc.y);
            if (destinationPath == null) {
                destinationPath = new TreePath(tree.getModel().getRoot());
            }
//            SelectedTreePath = tree.getSelectionPath();
            final String msg = testDropTarget(destinationPath, SelectedTreePath);
            if (msg != null) {
                e.rejectDrop();

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(
                                tree, msg, "Error Dialog", JOptionPane.ERROR_MESSAGE
                        );
                    }
                });
                return;
            }

            com.eric.org.FilterConfigNode destNode = (com.eric.org.FilterConfigNode) destinationPath.getLastPathComponent();

            //get old parent node
            com.eric.org.FilterConfigNode oldParent = (com.eric.org.FilterConfigNode) getSelectedNode().getParent();

            int action = e.getDropAction();
            boolean moveAction = (action == DnDConstants.ACTION_MOVE);

            //make new child node
            com.eric.org.FilterConfigNode newChild = new com.eric.org.FilterConfigNode(childInfo);

            try {
                if (moveAction) oldParent.remove(getSelectedNode());

                if (moveAction) {
                    e.acceptDrop(DnDConstants.ACTION_MOVE);
                    if (destNode.getAllowsChildren()) {
                        destNode.add(newChild, fcm);
                    } else {
                        com.eric.org.FilterConfigNode parentNode = (com.eric.org.FilterConfigNode) destNode.getParent();
                        int index = parentNode.getIndex(destNode);
                        parentNode.insert(newChild, index, fcm);
                        destNode = parentNode;
                    }
                } else e.acceptDrop(DnDConstants.ACTION_COPY);
            } catch (java.lang.IllegalStateException ils) {
                e.rejectDrop();
            }

            e.getDropTargetContext().dropComplete(true);

            //expand nodes appropriately - this probably isnt the best way...
            fcm.reload(oldParent);
            fcm.reload(destNode);
            TreePath parentPath = new TreePath(destNode.getPath());
            tree.expandPath(parentPath);
        } catch (IOException | UnsupportedFlavorException io) {
            e.rejectDrop();
        }
    }
    class TreeTransferHandler extends TransferHandler {
        DataFlavor nodesFlavor;
        DataFlavor[] flavors = new DataFlavor[1];
        DefaultMutableTreeNode[] nodesToRemove;

        public TreeTransferHandler() {
            try {
                String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
                        ";class=\"" +
                        javax.swing.tree.DefaultMutableTreeNode[].class.getName() +
                        "\"";
                nodesFlavor = new DataFlavor(mimeType);
                flavors[0] = nodesFlavor;
            } catch(ClassNotFoundException e) {
                System.out.println("ClassNotFound: " + e.getMessage());
            }
        }

        public boolean canImport(TransferHandler.TransferSupport support) {
            if(!support.isDrop()) {
                return false;
            }
            support.setShowDropLocation(true);
            if(!support.isDataFlavorSupported(nodesFlavor)) {
                return false;
            }
            // Do not allow a drop on the drag source selections.
            JTree.DropLocation dl =
                    (JTree.DropLocation)support.getDropLocation();
            JTree tree = (JTree)support.getComponent();
            int dropRow = tree.getRowForPath(dl.getPath());
            int[] selRows = tree.getSelectionRows();
            for (int selRow : selRows) {
                if (selRow == dropRow) {
                    return false;
                }
            }
            // Do not allow MOVE-action drops if a non-leaf node is
            // selected unless all of its children are also selected.
            int action = support.getDropAction();
            if(action == MOVE) {
                return haveCompleteNode(tree);
            }
            // Do not allow a non-leaf node to be copied to a level
            // which is less than its source level.
            TreePath dest = dl.getPath();
            DefaultMutableTreeNode target =
                    (DefaultMutableTreeNode)dest.getLastPathComponent();
            TreePath path = tree.getPathForRow(selRows[0]);
            DefaultMutableTreeNode firstNode =
                    (DefaultMutableTreeNode)path.getLastPathComponent();
            return firstNode.getChildCount() <= 0 ||
                    target.getLevel() >= firstNode.getLevel();
        }

        private boolean haveCompleteNode(JTree tree) {
            int[] selRows = tree.getSelectionRows();
            TreePath path = tree.getPathForRow(selRows[0]);
            DefaultMutableTreeNode first =
                    (DefaultMutableTreeNode)path.getLastPathComponent();
            int childCount = first.getChildCount();
            // first has children and no children are selected.
            if(childCount > 0 && selRows.length == 1)
                return false;
            // first may have children.
            for(int i = 1; i < selRows.length; i++) {
                path = tree.getPathForRow(selRows[i]);
                DefaultMutableTreeNode next =
                        (DefaultMutableTreeNode)path.getLastPathComponent();
                if(first.isNodeChild(next)) {
                    // Found a child of first.
                    if(childCount > selRows.length-1) {
                        // Not all children of first are selected.
                        return false;
                    }
                }
            }
            return true;
        }

        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree)c;
            TreePath[] paths = tree.getSelectionPaths();
            if(paths != null) {
                // Make up a node array of copies for transfer and
                // another for/of the nodes that will be removed in
                // exportDone after a successful drop.
                List<DefaultMutableTreeNode> copies =
                        new ArrayList<>();
                List<DefaultMutableTreeNode> toRemove =
                        new ArrayList<>();
                DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode)paths[0].getLastPathComponent();
                DefaultMutableTreeNode copy = copy(node);
                copies.add(copy);
                toRemove.add(node);
                for(int i = 1; i < paths.length; i++) {
                    DefaultMutableTreeNode next =
                            (DefaultMutableTreeNode)paths[i].getLastPathComponent();
                    // Do not allow higher level nodes to be added to list.
                    if(next.getLevel() < node.getLevel()) {
                        break;
                    } else if(next.getLevel() > node.getLevel()) {  // child node
                        copy.add(copy(next));
                        // node already contains child
                    } else {                                        // sibling
                        copies.add(copy(next));
                        toRemove.add(next);
                    }
                }
                DefaultMutableTreeNode[] nodes =
                        copies.toArray(new DefaultMutableTreeNode[copies.size()]);
                nodesToRemove =
                        toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
                return new NodesTransferable(nodes);
            }
            return null;
        }

        /** Defensive copy used in createTransferable. */
        private DefaultMutableTreeNode copy(TreeNode node) {
            return new DefaultMutableTreeNode(node);
        }

        protected void exportDone(JComponent source, Transferable data, int action) {
            if((action & MOVE) == MOVE) {
                JTree tree = (JTree)source;
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                // Remove nodes saved in nodesToRemove in createTransferable.
                for(int i = 0; i < nodesToRemove.length; i++) {
                    model.removeNodeFromParent(nodesToRemove[i]);
                }
            }
        }

        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        public boolean importData(TransferHandler.TransferSupport support) {
            if(!canImport(support)) {
                return false;
            }
            // Extract transfer data.
            DefaultMutableTreeNode[] nodes = null;
            try {
                Transferable t = support.getTransferable();
                nodes = (DefaultMutableTreeNode[])t.getTransferData(nodesFlavor);
            } catch(UnsupportedFlavorException ufe) {
                System.out.println("UnsupportedFlavor: " + ufe.getMessage());
            } catch(java.io.IOException ioe) {
                System.out.println("I/O error: " + ioe.getMessage());
            }
            // Get drop location info.
            JTree.DropLocation dl =
                    (JTree.DropLocation)support.getDropLocation();
            int childIndex = dl.getChildIndex();
            TreePath dest = dl.getPath();
            DefaultMutableTreeNode parent =
                    (DefaultMutableTreeNode)dest.getLastPathComponent();
            JTree tree = (JTree)support.getComponent();
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            // Configure for drop mode.
            int index = childIndex;    // DropMode.INSERT
            if(childIndex == -1) {     // DropMode.ON
                index = parent.getChildCount();
            }
            // Add data to model.
            for (DefaultMutableTreeNode node : nodes) {
                model.insertNodeInto(node, parent, index++);
            }
            return true;
        }

        public String toString() {
            return getClass().getName();
        }

        public class NodesTransferable implements Transferable {
            DefaultMutableTreeNode[] nodes;

            public NodesTransferable(DefaultMutableTreeNode[] nodes) {
                this.nodes = nodes;
            }

            public Object getTransferData(DataFlavor flavor)
                    throws UnsupportedFlavorException {
                if(!isDataFlavorSupported(flavor))
                    throw new UnsupportedFlavorException(flavor);
                return nodes;
            }

            public DataFlavor[] getTransferDataFlavors() {
                return flavors;
            }

            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return nodesFlavor.equals(flavor);
            }
        }
    }
    public interface CheckBoxCustomizer {
        boolean showCheckBox(TreePath path);
    }

    private CheckBoxCustomizer checkBoxCustomer = null;

    public void setCheckBoxCustomer(CheckBoxCustomizer checkBoxCustomer) {
        this.checkBoxCustomer = checkBoxCustomer;
//        renderer.checkBoxCustomer = checkBoxCustomer;
    }

    /**
     * In the design, Group enabled basing on if all leaves selected or not
     * Here we doesn't us the tree selection Model
     * @param treePath
     */
    private void toggleSelection(TreePath treePath) {
        if (treePath == null)
            return;

        FilterConfigNode node = (FilterConfigNode)treePath.getLastPathComponent();
        System.out.println("Toggle " + ((ConfigInfo)node.getUserObject()).toString());
        if (node.isAllLeafSelected() || node.isPartialLeafSelected()) {
            node.unCheckSelfandAllChild();
        } else {
            node.selectAllLeaf();
        }
        tree.treeDidChange();

        if (changeListener != null) {
            try {
                changeListener.vetoableChange(new PropertyChangeEvent(this, "filter changed", null, null));
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }
        }
    }

    public void mouseClicked(MouseEvent me) {
        TreePath path = tree.getPathForLocation(me.getX(), me.getY());

        if (SwingUtilities.isLeftMouseButton(me)) {
            if (path == null)
                return;
            if (checkBoxCustomer != null && !checkBoxCustomer.showCheckBox(path))
                return;
            if (me.getX() > tree.getPathBounds(path).x + hotspot)
                return;

            toggleSelection(path);
        } else if (SwingUtilities.isRightMouseButton(me)) {
            int selRow = tree.getRowForLocation(me.getX(), me.getY());
            TreePath selPath = tree.getPathForLocation(me.getX(), me.getY());
            tree.setSelectionPath(selPath);
            if (selRow>-1){
                tree.setSelectionRow(selRow);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        tree.setDragEnabled(false);
    }

    //TreeSelectionListener
    private com.eric.org.FilterConfigNode getSelectedNode() {
        try {
            SelectedNode = (com.eric.org.FilterConfigNode) tree.getSelectionPath().getLastPathComponent();
            return SelectedNode;
        } catch (Exception e){
            System.out.println("getSelectedNode, Error: " + e);
        }

        return null;
    }

    public void valueChanged(TreeSelectionEvent e) {
//        tree.treeDidChange();

        SelectedTreePath = e.getNewLeadSelectionPath();
        if (SelectedTreePath == null) {
            SelectedNode = null;
            return;
        }
        SelectedNode = (com.eric.org.FilterConfigNode) SelectedTreePath.getLastPathComponent();
    }

    private void addChildPaths(TreePath path, TreeModel model, List result) {
        Object item = path.getLastPathComponent();
        int childCount = model.getChildCount(item);
        for (int i = 0; i < childCount; i++)
            result.add(path.pathByAddingChild(model.getChild(item, i)));
    }

    private ArrayList getDescendants(TreePath[] paths, TreeModel model) {
        ArrayList result = new ArrayList();
        Stack pending = new Stack();
        pending.addAll(Arrays.asList(paths));
        while (!pending.isEmpty()) {
            TreePath path = (TreePath) pending.pop();
            addChildPaths(path, model, pending);
            result.add(path);
        }
        return result;
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.isControlDown()) {
            if (e.getWheelRotation() < 0) {
                System.out.println("scrolled up");
                Font currentFont = this.tree.getFont();
                final Font bigFont = new Font(currentFont.getName(), currentFont.getStyle(), currentFont.getSize() + 1);
                this.tree.setFont(bigFont);
            } else {
                System.out.println("scrolled down");
                Font currentFont = this.tree.getFont();
                final Font smallFont = new Font(currentFont.getName(), currentFont.getStyle(), currentFont.getSize() - 1);
                this.tree.setFont(smallFont);
            }
        }else {
            this.tree.getParent().dispatchEvent(e);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
    }
//ActionListener

    public void actionPerformed(ActionEvent e) {
        toggleSelection(tree.getSelectionPath());
    }



}
