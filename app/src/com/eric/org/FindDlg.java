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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;

class FindDlg extends JDialog {
    private LogTableMgr ltm;

    private JButton nextButton;
    private JButton prevButton;

    private JTextField textField;
    private JCheckBox caseCheckBox;

    private ActionListener nextListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String findWords = textField.getText();
            boolean casesen = caseCheckBox.isSelected();

            boolean find = ltm.findWord(findWords, casesen, true);
            if(!find){
                JOptionPane.showMessageDialog(getContentPane(), "No (more) matches");
            }
        }
    };
    private ActionListener prevListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String findWords = textField.getText();
            boolean casesen = caseCheckBox.isSelected();
            boolean find = ltm.findWord(findWords, casesen, false);
            if(!find){
                JOptionPane.showMessageDialog(getContentPane(), "No (more) matches");
            }
        }
    };

    public FindDlg(LogTableMgr ltm) {
//        super(owner);

        this.ltm = ltm;
        createUI();
    }
    private FindDlg() {
        createUI();
    }
    private void createUI() {
        JLabel label = new JLabel("Find What:");
        textField = new JTextField();
        caseCheckBox = new JCheckBox("Match Case");

        nextButton = new JButton("Next");
        prevButton = new JButton("Previous");

        nextButton.setMnemonic(KeyEvent.VK_N);
        Action nextbuttonAction = new AbstractAction("Next") {

            @Override
            public void actionPerformed(ActionEvent evt) {
                nextListener.actionPerformed(evt);
            }
        };

        String nextkey = "Next";

        nextButton.setAction(nextbuttonAction);

        nextbuttonAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);

        nextButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), nextkey);

        nextButton.getActionMap().put(nextkey, nextbuttonAction);
//        nextButton.setDisplayedMnemonicIndex();
        prevButton.setMnemonic(KeyEvent.VK_P);
        Action prevbuttonAction = new AbstractAction("Previous") {

            @Override
            public void actionPerformed(ActionEvent evt) {
                prevListener.actionPerformed(evt);
            }
        };

        String prevkey = "Previous";

        prevButton.setAction(prevbuttonAction);

        prevbuttonAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);

        prevButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), prevkey);

        prevButton.getActionMap().put(prevkey, prevbuttonAction);
        // remove redundant default border of check boxes - they would hinder
        // correct spacing and aligning (maybe not needed on some look and feels)
        caseCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

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
                                        .addComponent(caseCheckBox))))
                .addGroup(layout.createParallelGroup(LEADING)
                        .addComponent(nextButton)
                        .addComponent(prevButton))
        );

        layout.linkSize(SwingConstants.HORIZONTAL, nextButton, prevButton);

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(BASELINE)
                        .addComponent(label)
                        .addComponent(textField)
                        .addComponent(nextButton))
                .addGroup(layout.createParallelGroup(LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(caseCheckBox)))
                        .addComponent(prevButton))
        );
        getRootPane().setDefaultButton(nextButton);

        setTitle("Find");
        pack();
        Toolkit toolkit = getToolkit();
        Dimension size = toolkit.getScreenSize();

        setMinimumSize(new Dimension(700,150));
        pack();

        setLocation(size.width/2 - getWidth()/2,
                size.height/2 - getHeight()/2);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

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
                new FindDlg().setVisible(true);
            }
        });
    }
}
