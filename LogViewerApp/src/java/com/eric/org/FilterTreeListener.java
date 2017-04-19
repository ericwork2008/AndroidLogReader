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
import com.eric.org.config.FilterConfigMgr;
import santhosh.CheckTreeSelectionModel;
import santhosh.PreorderEnumeration;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
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

public class FilterTreeListener extends MouseAdapter  implements
        TreeModelListener, ActionListener,
        TreeSelectionListener,
        DragGestureListener, DropTargetListener,
        DragSourceListener {
    private CheckTreeSelectionModel selectionModel;
    private com.eric.org.FilterConfigTreeModel fcm;
    private com.eric.org.LogTableMgr logTableMgr;
    private JTree tree = null;
    private int hotspot = new JCheckBox().getPreferredSize().width;

    private TreePath SelectedTreePath = null;
    private com.eric.org.FilterConfigNode SelectedNode=null;

    /** Variables needed for DnD */
    private DragSource dragSource = null;
    private DragSourceContext dragSourceContext = null;

    public FilterTreeListener(com.eric.org.FilterTreeManager filterTreeManager, boolean dig){
        this.tree = filterTreeManager.getFilterConfigtree();
        this.selectionModel = filterTreeManager.getFilterConfigTreeSelectionModel();
        this.fcm = filterTreeManager.getFilterConfigTreeModel();


        dragSource = DragSource.getDefaultDragSource() ;

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

    private void treeChanged(){
        tree.treeDidChange();
    }

    private VetoableChangeListener changeListener = new VetoableChangeListener(){

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
        } catch (NullPointerException exc) {}

        System.out.println("The user has finished editing the node.");
        System.out.println("New value: " + node.getUserObject());

        if(changeListener!=null){
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

    private com.eric.org.LogTableListener logchangeListener = new com.eric.org.LogTableListener(){

        @Override
        public void onContentChanged() {
//            fcm.reload();
            tree.repaint();
//            fcm.nodesChanged((TreeNode) fcm.getRoot(),null);
        }
    };

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
            try{
                dragSource.startDrag(dge, cursor, transferable, this);
            }catch (Exception e){
                System.out.print(e);
            }

        }
    }
    /** Convenience method to test whether drop location is valid
     @param destination The destination path
     @param dropper The path for the node to be dropped
     @return null if no problems, otherwise an explanation
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
        if ( dropper.isDescendant(destination))
            return "Destination node cannot be a descendant.";

        //Test 4.
        if ( dropper.getParentPath().equals(destination))
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
        if (!tr.isDataFlavorSupported( ConfigInfo.INFO_FLAVOR))
        {
            event.rejectDrag();
            return;
        }


        //set cursor location. Needed in setCursor method
        Point cursorLocationBis = event.getLocation();
        TreePath destinationPath =
                tree.getPathForLocation(cursorLocationBis.x, cursorLocationBis.y);
        if(destinationPath==null){
            destinationPath = new TreePath(tree.getModel().getRoot());
        }
        SelectedTreePath = tree.getSelectionPath();
        // if destination path is okay accept drop...
        if (testDropTarget(destinationPath, SelectedTreePath) == null){
            event.acceptDrag(DnDConstants.ACTION_MOVE ) ;
        } else {
            event.rejectDrag() ;
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
            if (!tr.isDataFlavorSupported( ConfigInfo.INFO_FLAVOR))
            {
                e.rejectDrop();
                return;
            }

            //cast into appropriate data type
            ConfigInfo childInfo = (ConfigInfo) tr.getTransferData( ConfigInfo.INFO_FLAVOR );

            //get new parent node
            Point loc = e.getLocation();
            TreePath destinationPath = tree.getPathForLocation(loc.x, loc.y);
            if(destinationPath==null){
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
                    e.acceptDrop (DnDConstants.ACTION_MOVE);
                    if(destNode.getAllowsChildren()){
                        destNode.add(newChild,fcm);
                    }else{
                        com.eric.org.FilterConfigNode parentNode = (com.eric.org.FilterConfigNode) destNode.getParent();
                        int index = parentNode.getIndex(destNode);
                        parentNode.insert(newChild,index,fcm);
                        destNode = parentNode;
                    }
                }
                else e.acceptDrop (DnDConstants.ACTION_COPY);
            }
            catch (java.lang.IllegalStateException ils) {
                e.rejectDrop();
            }

            e.getDropTargetContext().dropComplete(true);

            //expand nodes appropriately - this probably isnt the best way...
            fcm.reload(oldParent);
            fcm.reload(destNode);
            TreePath parentPath = new TreePath(destNode.getPath());
            tree.expandPath(parentPath);
        }
        catch (IOException io) { e.rejectDrop(); }
        catch (UnsupportedFlavorException ufe) {e.rejectDrop();}
    }

    public interface CheckBoxCustomizer{
        boolean showCheckBox(TreePath path);
    }

    private CheckBoxCustomizer checkBoxCustomer = null;
    public void setCheckBoxCustomer(CheckBoxCustomizer checkBoxCustomer){
        this.checkBoxCustomer = checkBoxCustomer;
//        renderer.checkBoxCustomer = checkBoxCustomer;
    }

    private void toggleSelection(TreePath path){
        if(path==null)
            return;

        boolean selected = selectionModel.isPathSelected(path, selectionModel.isDigged());

        if(selectionModel.isDigged() && !selected && selectionModel.isPartiallySelected(path))
            selected = !selected;
        selectionModel.removeTreeSelectionListener(this);
        if(selected){
            selectionModel.removeSelectionPath(path);
        }else{
            selectionModel.addSelectionPath(path);
        }
        selectionModel.addTreeSelectionListener(this);
        treeChanged();
        //Clear filter checked list
        FilterConfigMgr.disableAllFilter();
        System.out.println("Filter list size after clear "+FilterConfigMgr.getActiveFilterList().size());
        ArrayList<TreePath> selectionPaths = getAllCheckedPaths();
        System.out.println("Selection size:"+selectionPaths.size());
        if(selectionPaths!=null){
            for (TreePath selectionPath : selectionPaths) {
                FilterConfigNode tn = null;
                tn = (FilterConfigNode) selectionPath.getLastPathComponent();
                ConfigInfo cn = (ConfigInfo) tn.getUserObject();
                cn.enabled = true;
            }
            System.out.println("Filter list size after rebuild "+FilterConfigMgr.getActiveFilterList().size());

            if(changeListener!=null){
                try {
                    changeListener.vetoableChange(new PropertyChangeEvent(this, "filter changed", null, null));
                } catch (PropertyVetoException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void mouseClicked(MouseEvent me){
        TreePath path = tree.getPathForLocation(me.getX(), me.getY());

        if (SwingUtilities.isLeftMouseButton(me)){
            if(path==null)
                return;
            if(checkBoxCustomer!=null && !checkBoxCustomer.showCheckBox(path))
                return;
            if(me.getX()>tree.getPathBounds(path).x+hotspot)
                return;

            toggleSelection(path);
        } else if (SwingUtilities.isRightMouseButton(me)){

        }
    }

//TreeSelectionListener
private com.eric.org.FilterConfigNode getSelectedNode() {
        SelectedNode = (com.eric.org.FilterConfigNode)tree.getSelectionPath().getLastPathComponent();
        return SelectedNode;
    }

    public void valueChanged(TreeSelectionEvent e){
//        tree.treeDidChange();

        SelectedTreePath = e.getNewLeadSelectionPath();
        if (SelectedTreePath == null) {
            SelectedNode = null;
            return;
        }
        SelectedNode = (com.eric.org.FilterConfigNode)SelectedTreePath.getLastPathComponent();
    }

    private void addChildPaths(TreePath path, TreeModel model, List result){
        Object item = path.getLastPathComponent();
        int childCount = model.getChildCount(item);
        for(int i = 0; i<childCount; i++)
            result.add(path.pathByAddingChild(model.getChild(item, i)));
    }

    private ArrayList getDescendants(TreePath paths[], TreeModel model){
        ArrayList result = new ArrayList();
        Stack pending = new Stack();
        pending.addAll(Arrays.asList(paths));
        while(!pending.isEmpty()){
            TreePath path = (TreePath)pending.pop();
            addChildPaths(path, model, pending);
            result.add(path);
        }
        return result;
    }


    private ArrayList getAllCheckedPaths(){
        System.out.println("selectionModel.getSelectionPaths().length:"+selectionModel.getSelectionPaths().length);
        return getDescendants(selectionModel.getSelectionPaths(), tree.getModel());
    }

    public Enumeration getAllCheckedNodes(){
        List<Object> selected = new ArrayList<>();
        for(TreePath treePath : selectionModel.getSelectionPaths()){
            selected.add(treePath.getLastPathComponent());
        }
        if(selectionModel.isDigged())
            return new PreorderEnumeration(tree.getModel(), selected.toArray());
        else
            return Collections.enumeration(selected);
    }

//ActionListener

    public void actionPerformed(ActionEvent e){
        toggleSelection(tree.getSelectionPath());
    }
}
