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

class AboutDlg extends JDialog {

    public AboutDlg() {
        JPanel basic = new JPanel();
        basic.setLayout(new BoxLayout(basic, BoxLayout.Y_AXIS));
        add(basic);

        JPanel topPanel = new JPanel(new BorderLayout(0, 0));
        topPanel.setMaximumSize(new Dimension(450, 0));
        JLabel hint = new JLabel("Android Log Viewer");
        hint.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));
        topPanel.add(hint);

        ImageIcon icon = new ImageIcon("jdev.png");
        JLabel label = new JLabel(icon);
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPanel.add(label, BorderLayout.EAST);

        JSeparator separator = new JSeparator();
        separator.setForeground(Color.gray);

        topPanel.add(separator, BorderLayout.SOUTH);

        basic.add(topPanel);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 15));
        JTextPane pane = new JTextPane();

        pane.setContentType("text/html");
        String text = "<p>This is my project when study Java programing</p>";
        pane.setText(text);
        pane.setEditable(false);
        textPanel.add(pane);

        basic.add(textPanel);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton closeBtn = new JButton("Close");
        closeBtn.setMnemonic(KeyEvent.VK_C);
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        bottom.add(closeBtn);
        basic.add(bottom);

        bottom.setMaximumSize(new Dimension(450, 0));

        setTitle("About");
        setModalityType(ModalityType.APPLICATION_MODAL);
        setSize(new Dimension(350, 250));
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        getRootPane().setDefaultButton(closeBtn);
    }
}
