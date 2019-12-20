/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
//$Id: EditorTitle.java,v 1.3 2010/08/25 08:22:51 olga Exp $

package agg.gui.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;
import javax.swing.JPanel;

import agg.gui.icons.ColoredSquare;

@SuppressWarnings("serial")
public class EditorTitle extends JPanel implements MouseListener {

	private JLabel iconLabel;

	private ColoredSquare colorIcon;

	private JPanel modePanel;

	public EditorTitle(String leftStr, String rightStr) {
		setLayout(new BorderLayout());
		addMouseListener(this);

		JPanel textPanel = new JPanel(new BorderLayout());
		textPanel.setBackground(Color.lightGray);
		textPanel.add(new JLabel(leftStr), BorderLayout.CENTER);

		JLabel modeLabel = new JLabel(rightStr);
		this.colorIcon = new ColoredSquare(Color.green);
		this.iconLabel = new JLabel(this.colorIcon);
		this.modePanel = new JPanel(new BorderLayout());
		this.modePanel.addMouseListener(this);
		this.modePanel.setEnabled(true);
		this.modePanel.setBackground(Color.lightGray);
		this.modePanel.add(modeLabel, BorderLayout.WEST);
		this.modePanel.add(this.iconLabel, BorderLayout.EAST);

		add(textPanel, BorderLayout.WEST);
		add(this.modePanel, BorderLayout.EAST);
	}

	public boolean isRed() {
		if (this.colorIcon.getColor() == Color.red)
			return true;
		
		return false;
	}

	public void mousePressed(MouseEvent e) {
		if (e.getSource() == this.modePanel) {
			if (this.colorIcon.getColor() == Color.red)
				this.colorIcon.setColor(Color.green);
			else
				this.colorIcon.setColor(Color.red);
			this.iconLabel.update(this.iconLabel.getGraphics());
		}
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

}
