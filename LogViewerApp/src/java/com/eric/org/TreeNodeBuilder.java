package com.eric.org;

import com.eric.org.config.ConfigInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.NoSuchElementException;

/**
 * Class that prunes off all leaves which do not match the search string.
 *
 * @author Oliver.Watkins
 */

public class TreeNodeBuilder {

    private String textToMatch;

    public TreeNodeBuilder(String textToMatch) {
        this.textToMatch = textToMatch.toLowerCase();
    }

    public DefaultMutableTreeNode prune(DefaultMutableTreeNode root) {
        removeBadLeaves(root);
        return root;
    }

    private void removeNode(DefaultMutableTreeNode node) {
        if (node == null||node.isRoot())
            return;
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent != null)
            parent.remove(node);
    }

    //DFS to remove the node which doesn't contain the filter text
    private void removeBadLeaves(DefaultMutableTreeNode node) {
        //Base
        if (node == null)
            return;

        ConfigInfo ci = (ConfigInfo) node.getUserObject();
        if (ci.isGroup()) {
            if (node.getChildCount() == 0) {
                removeNode(node);
                return;
            }
        }
        //Match the text in group and config filter
        if (ci.isGroup()) {
            if (ci.getName().toLowerCase().contains(textToMatch))
                return;
        } else if (!ci.filterItem.mMsgPattern.toLowerCase().contains(textToMatch)) {
                removeNode(node);
                return;
        }

        if (ci.isGroup()) {
            //loop all children
            int count = node.getChildCount();
            for (int i = count-1; i>=0; i--){
                DefaultMutableTreeNode leaf;
                try {
                    leaf = (DefaultMutableTreeNode) node.getChildAt(i);
                    removeBadLeaves(leaf);
                } catch (NoSuchElementException | NullPointerException e) {
                    return;
                }
            }
            //If All child clean, we need remove this node
            if (node.getChildCount() == 0) {
                removeNode(node);
                return;
            }
        }

    }
}