/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.typeeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeListener;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JColorChooser;
import javax.swing.JButton;

@SuppressWarnings("serial")
public class ColorChooserDialog extends JPanel {

	JDialog dialog;

	private JColorChooser tcc;

//	private JLabel banner;

//	private Color color;

	private JButton closeButton;

	public ColorChooserDialog() {
		super(new BorderLayout());

		// Set up color chooser for setting type color
		this.tcc = new JColorChooser(Color.yellow);

		this.closeButton = new JButton();
		this.closeButton.setActionCommand("close");
		this.closeButton.setText("Close");
		this.closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ColorChooserDialog.this.dialog.setVisible(false);
			}
		});

		add(this.tcc, BorderLayout.CENTER);
		add(this.closeButton, BorderLayout.PAGE_END);

		setOpaque(true);
	}

	public void showColorDialog(Frame parent, String title, Color initColor) {
//		color = 
		JColorChooser.showDialog(parent, title, initColor);
	}

	public void showColorDialog(Dialog parent, String title, Color initColor) {
//		color = 
		JColorChooser.showDialog(parent, title, initColor);
	}

	public void showColorDialog(Frame parent, Point location) {
		if (this.dialog == null) {
			this.dialog = new JDialog(parent);
			this.dialog.setModal(true);
			this.dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent evt) {
					ColorChooserDialog.this.dialog.setVisible(false);
				}
			});
			this.dialog.setContentPane(this);
			this.dialog.validate();
			this.dialog.pack();
		}
		this.dialog.setLocation(location);
		this.dialog.setVisible(true);
	}

	public void showColorDialog(Dialog parent, Point location) {
		if (this.dialog == null) {
			this.dialog = new JDialog(parent);
			this.dialog.setModal(true);
			this.dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent evt) {
					ColorChooserDialog.this.dialog.setVisible(false);
				}
			});
			this.dialog.setContentPane(this);
			this.dialog.validate();
			this.dialog.pack();
		}
		this.dialog.setLocation(location);
		this.dialog.setVisible(true);
	}

	public void addChangeListener(ChangeListener l) {
		this.tcc.getSelectionModel().addChangeListener(l);
	}

	public Color getColor() {
		return this.tcc.getColor();
	}

}
