/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.icons;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

public class NewPACIcon implements Icon {

	boolean isEnabled;

	Color color;

	public NewPACIcon(Color aColor) {
		this.color = aColor;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		if (this.isEnabled) {
			g.setColor(Color.white);
			g.fillRect(x, y, getIconWidth(), getIconHeight());
			g.setColor(this.color);
		} else {
			g.setColor(Color.gray.brighter());
			g.fillRect(x, y, getIconWidth(), getIconHeight());
			g.setColor(Color.gray.darker());
		}
		g.drawString("P", x + 1, y + 8);
		g.drawString("A", x + 5, y + 11);
		g.drawString("C", x + 9, y + 15);
	}

	public int getIconWidth() {
		return 16;
	}

	public int getIconHeight() {
		return 16;
	}

	public void setEnabled(boolean enabled) {
		this.isEnabled = enabled;
	}
}
