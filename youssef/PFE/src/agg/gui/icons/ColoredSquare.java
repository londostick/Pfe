/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
//$Id: ColoredSquare.java,v 1.3 2010/08/23 07:33:26 olga Exp $

package agg.gui.icons;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

public class ColoredSquare implements Icon {

	Color color;

	public ColoredSquare(Color c) {
		this.color = c;
	}

	public void setColor(Color c) {
		this.color = c;
	}

	public Color getColor() {
		return this.color;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		Color oldColor = g.getColor();
		g.setColor(this.color);
		g.fill3DRect(x, y, getIconWidth(), getIconHeight(), true);
		g.setColor(oldColor);
	}

	public int getIconWidth() {
		return 14;
	}

	public int getIconHeight() {
		return 14;
	}
}
