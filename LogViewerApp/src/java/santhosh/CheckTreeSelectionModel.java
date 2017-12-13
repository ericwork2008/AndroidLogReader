package santhosh;

import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.ArrayList;
import java.util.Stack;

/*
 * CheckTree have two selections: one is normal selection and another is checkSelection (checkbox selected)
 *
 * In our check selection model, it contains only the selected node but not all its descendants.
 * i.e. let us say Node A has children B, C, D;
 * When user checks Node A, the tree shows B, C and D also checked. But the selectionModel actually contains only A,
 */

// @author Santhosh Kumar T - santhosh@in.fiorano.com
@SuppressWarnings("ALL")
public class CheckTreeSelectionModel extends DefaultTreeSelectionModel{
    private TreeModel model;
    private boolean dig = true;

    public CheckTreeSelectionModel(TreeModel model, boolean dig){
        this.model = model;
        this.dig = dig;
        setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    }

    public boolean isDigged(){
        return dig;
    }

    // tests whether there is any unselected node in the subtree of given path
    public boolean isPartiallySelected(TreePath path){
        if(isPathSelected(path, true))
            return false;
        TreePath[] selectionPaths = getSelectionPaths();
        if(selectionPaths==null)
            return false;
        for (TreePath selectionPath : selectionPaths) {
            if (isDescendant(path, selectionPath))
                return true;
        }
        return false;
    }

    // tells whether given path is selected.
    // if dig is true, then a path is assumed to be selected, if
    // one of its ancestor is selected.
    public boolean isPathSelected(TreePath path, boolean dig){
        if(!dig)
            return super.isPathSelected(path);
        while(path!=null && !super.isPathSelected(path))
            path = path.getParentPath();
        return path!=null;
    }

    // is path2 descendant of path1
    private boolean isDescendant(TreePath path1, TreePath path2){
        return path1.isDescendant(path2);
    }

    public void setSelectionPaths(TreePath[] paths){
        System.out.println("setSelectionPaths size:"+ paths.length);
        clearSelection();
        if(dig){
            for (TreePath path : paths) addSelectionPath(path);
        }else
            super.setSelectionPaths(paths);
    }

    @SuppressWarnings("unchecked")
    public void addSelectionPaths(TreePath[] paths){
        System.out.println("addSelectionPaths :"+paths+" size="+ paths.length);
        if(!dig){
            super.addSelectionPaths(paths);
            return;
        }

        // unselect all descendants of paths[]
        for (TreePath path : paths) {
            TreePath[] selectionPaths = getSelectionPaths();
            if (selectionPaths == null)
                break;
            ArrayList toBeRemoved = new ArrayList();
            for (TreePath selectionPath : selectionPaths) {
                if (isDescendant(path, selectionPath)) //p2 is desendant of p1
                    toBeRemoved.add(selectionPath);
            }
            super.removeSelectionPaths((TreePath[]) toBeRemoved.toArray(new TreePath[0]));
        }

        // if all siblings are selected then unselect them and select parent recursively
        // otherwize just select that path.
        for (TreePath path1 : paths) {
            TreePath path = path1;
            TreePath temp = null;
            while (areSiblingsSelected(path)) {
                temp = path;
                if (path.getParentPath() == null)
                    break;
                path = path.getParentPath();
            }
            if (temp != null) {
                if (temp.getParentPath() != null)
                    addSelectionPath(temp.getParentPath());
                else {
                    if (!isSelectionEmpty())
                        removeSelectionPaths(getSelectionPaths());
                    super.addSelectionPaths(new TreePath[]{temp});
                }
            } else
                super.addSelectionPaths(new TreePath[]{path});
        }
    }

    // tells whether all siblings of given path are selected.
    private boolean areSiblingsSelected(TreePath path){
        TreePath parent = path.getParentPath();
        if(parent==null)
            return true;
        Object node = path.getLastPathComponent();
        Object parentNode = parent.getLastPathComponent();

        int childCount = model.getChildCount(parentNode);
        for(int i = 0; i<childCount; i++){
            Object childNode = model.getChild(parentNode, i);
            if(childNode==node)
                continue;
            if(!isPathSelected(parent.pathByAddingChild(childNode),dig))
                return false;
        }
        return true;
    }

    public void removeSelectionPaths(TreePath[] paths){
        if(!dig){
            super.removeSelectionPaths(paths);
            return;
        }

        for (TreePath path : paths) {
            if (path.getPathCount() == 1)
                _removeSelectionPath(path);
            else
                toggleRemoveSelection(path);
        }
    }

    private void _removeSelectionPath(TreePath path){
        ArrayList list = new ArrayList();
//        list.addChild(path);
        TreePath[] seletedPaths = super.getSelectionPaths();
        for(TreePath selectedPath: seletedPaths){
            if(isDescendant(path,selectedPath)){
                list.add(selectedPath);
            }
        }
        super.removeSelectionPaths((TreePath[])list.toArray(new TreePath[list.size()]));
    }

    // if any ancestor node of given path is selected then unselect it
    //  and selection all its descendants except given path and descendants.
    // otherwise just unselect the given path
    private void toggleRemoveSelection(TreePath path){
        Stack stack = new Stack();
        TreePath parent = path.getParentPath();
        while(parent!=null && !isPathSelected(parent)){
            stack.push(parent);
            parent = parent.getParentPath();
        }
        if(parent!=null)
            stack.push(parent);
        else{
            _removeSelectionPath(path);
            return;
        }

        while(!stack.isEmpty()){
            TreePath temp = (TreePath)stack.pop();
            TreePath peekPath = stack.isEmpty() ? path : (TreePath)stack.peek();
            Object node = temp.getLastPathComponent();
            Object peekNode = peekPath.getLastPathComponent();
            int childCount = model.getChildCount(node);
            for(int i = 0; i<childCount; i++){
                Object childNode = model.getChild(node, i);
                if(childNode!=peekNode)
                    super.addSelectionPaths(new TreePath[]{temp.pathByAddingChild(childNode)});
            }
        }
        super.removeSelectionPaths(new TreePath[]{parent});
    }
}
