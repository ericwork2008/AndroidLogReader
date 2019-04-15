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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;

class FilterConfigDlg extends JDialog {
    private JTextArea textField;
    private JCheckBox caseCheckBox;
    private JCheckBox enableCheckBox;
    private JButton ftColorButton;
    private JButton bgColorButton;

    private ConfigInfo fci = null;

    public FilterConfigDlg(String group) {
        JLabel label = new JLabel("Filter Text:");
        textField = new JTextArea();
        Font font = new Font("Arial", Font.PLAIN, 20    );
        textField.setFont(font);
        caseCheckBox = new JCheckBox("Match Case");
        enableCheckBox = new JCheckBox("Enable");

        JButton okButton = new JButton("Ok");
        JButton cancelButton = new JButton("Cancel");
        getRootPane().setDefaultButton(cancelButton);

        ftColorButton = new JButton("Front Color");
        bgColorButton = new JButton("Background Color");

        okButton.setMnemonic(KeyEvent.VK_ENTER);
        cancelButton.setMnemonic(KeyEvent.VK_ESCAPE);

        okButton.addActionListener(okListener);
        cancelButton.addActionListener(cancelListener);

        ftColorButton.addActionListener(frontColorListener);
        bgColorButton.addActionListener(bgColorListener);

        // remove redundant default border of check boxes - they would hinder
        // correct spacing and aligning (maybe not needed on some look and feels)
        caseCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        enableCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));


        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addComponent(label)
                .addGroup(layout.createParallelGroup(LEADING)
                        .addComponent(textField)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addComponent(ftColorButton)
                                        .addComponent(bgColorButton))
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addComponent(enableCheckBox)
                                        .addComponent(caseCheckBox))))
                .addGroup(layout.createParallelGroup(LEADING)
                        .addComponent(okButton)
                        .addComponent(cancelButton))
        );

        layout.linkSize(SwingConstants.HORIZONTAL, okButton, cancelButton);

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(BASELINE)
                        .addComponent(label)
                        .addComponent(textField)
                        .addComponent(okButton))
                .addGroup(layout.createParallelGroup(LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(ftColorButton)
                                        .addComponent(enableCheckBox))
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(bgColorButton)
                                        .addComponent(caseCheckBox)))
                        .addComponent(cancelButton))
        );

        setTitle("FilterConfigDlg");
        setPreferredSize(this.getParent().getPreferredSize());

        setMinimumSize(new Dimension(700,200));
        pack();

        Toolkit toolkit = getToolkit();
        Dimension size = toolkit.getScreenSize();

        setLocation(size.width/2 - getWidth()/2,
                size.height/2 - getHeight()/2);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private ActionListener okListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            // prepares response on the basis of some data introduced by a user; dialogResponse is called from JFrame after Dialog is closed
            fci = new ConfigInfo("");
            fci.filterItem.caseSensitive = caseCheckBox.isSelected();
            fci.filterItem.mMsgPattern = textField.getText();
            fci.enabled = enableCheckBox.isSelected();
            fci.ftColor = "#"+Integer.toHexString(textField.getForeground().getRGB()& 0xffffff);
            fci.bgColor = "#"+Integer.toHexString(textField.getBackground().getRGB()& 0xffffff);

            try {
                fci.filterItem.initPattern();
            } catch (Exception ex){
                JOptionPane.showMessageDialog(getContentPane(),ex.getMessage());
            }
            setVisible(false);
            dispose();
        }
    };
    private ActionListener cancelListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            // prepares response on the basis of some data introduced by a user; dialogResponse is called from JFrame after Dialog is closed
            fci = null;
            setVisible(false);
            dispose();
        }
    };

    private ActionListener frontColorListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Color newColor = JColorChooser.showDialog(
                    FilterConfigDlg.this,
                    "Choose Front Color",
                    ftColorButton.getBackground());
            textField.setForeground(newColor);
        }
    };
    private ActionListener bgColorListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Color newColor = JColorChooser.showDialog(
                    FilterConfigDlg.this,
                    "Choose Background Color",
                    bgColorButton.getBackground());
            textField.setBackground(newColor);
        }
    };
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(
                            "javax.swing.plaf.metal.MetalLookAndFeel");
                    //  "com.sun.java.swing.plaf.motif.MotifLookAndFeel");
                    //UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                new FilterConfigDlg("group").setVisible(true);
            }
        });
    }

    public ConfigInfo getFilterConfig() {
        return fci;
    }

    public void loadConfig(ConfigInfo config) {
        textField.setText(config.filterItem.mMsgPattern);
        enableCheckBox.setSelected(config.enabled);
        caseCheckBox.setSelected(config.filterItem.caseSensitive);
        textField.setForeground(Color.decode(config.ftColor));
        textField.setBackground(Color.decode(config.bgColor));
    }

    public ConfigInfo showDialog() {
        setVisible(true);

        return fci;
    }
}
