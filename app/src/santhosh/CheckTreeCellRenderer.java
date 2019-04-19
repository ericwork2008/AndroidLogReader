package santhosh;

import com.eric.org.FilterConfigNode;
import com.eric.org.FilterTreeListener;
import com.eric.org.FilterTreeManager;
import com.eric.org.config.ConfigInfo;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;

public class CheckTreeCellRenderer extends JPanel implements TreeCellRenderer {
    private santhosh.CheckTreeSelectionModel selectionModel;
    private TreeCellRenderer delegate;

    private santhosh.TristateCheckBox checkBox = new santhosh.TristateCheckBox("");
    public FilterTreeListener.CheckBoxCustomizer checkBoxCustomer = null;

    public CheckTreeCellRenderer(TreeCellRenderer delegate, santhosh.CheckTreeSelectionModel selectionModel) {
        this.delegate = delegate;
        this.selectionModel = selectionModel;

        setLayout(new BorderLayout());

        setOpaque(false);
        checkBox.setOpaque(false);
//        checkBox.setBorderPainted(true);
        checkBox.setBorderPaintedFlat(true);
//        Border raisedB = BorderFactory.createRaisedBevelBorder();
//        checkBox.setBorder(raisedB);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component defaultRenderComp = delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        Font f = defaultRenderComp.getFont();
        f = new Font("Dialog", Font.PLAIN, f.getSize());
        defaultRenderComp.setFont(f);

        Dimension d = defaultRenderComp.getPreferredSize();
        double minWidth = 300;
        if(d.getWidth()>minWidth)
            minWidth = d.getWidth();
        d.setSize(minWidth,d.getHeight());

        defaultRenderComp.setPreferredSize(d);
        defaultRenderComp.setBackground(Color.WHITE);
        defaultRenderComp.setForeground(Color.BLACK);
        setFont(tree.getFont());
        int rowHeight = tree.getRowHeight();
        rowHeight = Math.max(rowHeight, getPreferredSize().height);
        tree.setRowHeight(rowHeight);

        final Icon closed =
                (Icon) UIManager.get("InternalFrame.maximizeIcon");
        final Icon open =
                (Icon) UIManager.get("InternalFrame.minimizeIcon");


        removeAll();

        TreePath path = tree.getPathForRow(row);
        if (path != null) {
//            if(checkBoxCustomer!=null && !checkBoxCustomer.showCheckBox(path))
//                return renderer;
            FilterConfigNode tn = (FilterConfigNode) path.getLastPathComponent();
            if (tn.isAllLeafSelected()) {
                checkBox.getTristateModel().setState(santhosh.TristateState.SELECTED);
            } else {
                checkBox.getTristateModel().setState(selectionModel.isDigged() && tn.isPartialLeafSelected() ? santhosh.TristateState.INDETERMINATE : santhosh.TristateState.DESELECTED);
            }
//Calling setXxxIcon doesn't effect the current renderer, but the future renderer.
            //That is. If you call setOpenIcon AFTER you've already called super.getTreeCellRendererComponent,
            //it will not effect the current renderer, but it will effect the next call to super.getTreeCellRendererComponent
            //as the set method is simply setting the value of class variable.

            if ((value != null) && (value instanceof FilterConfigNode)) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

                if(value == node) {
                    ((DefaultTreeCellRenderer) defaultRenderComp).setOpaque(false);
                }else{
                    ((DefaultTreeCellRenderer) defaultRenderComp).setOpaque(true);
                }
                ConfigInfo userObject = (ConfigInfo) ((FilterConfigNode) value).getUserObject();
                if (!(userObject.isGroup())) {
                    ((DefaultTreeCellRenderer) defaultRenderComp).setIcon(null);
                    if(!userObject.isGroup()) {
                        defaultRenderComp.setBackground(Color.decode(userObject.bgColor));
                        defaultRenderComp.setForeground(Color.decode(userObject.ftColor));
                    }
                } else {
                    Icon groupIcon = UIManager.getIcon("FileView.directoryIcon");
                    ((DefaultTreeCellRenderer) defaultRenderComp).setIcon(groupIcon);
                }
                add(checkBox, BorderLayout.WEST);
                add(defaultRenderComp, BorderLayout.CENTER);
                return this;
            }
        }
        return defaultRenderComp;
    }
}
