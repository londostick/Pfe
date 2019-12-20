/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
//$Id: ColorLongDashLineIcon.java,v 1.3 2010/08/23 07:33:26 olga Exp $

package agg.gui.icons;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import agg.editor.impl.Line;

public class ColorLongDashLineIcon implements Icon {

	Color color;

	public ColorLongDashLineIcon(Color c) {
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
		Line line = new Line(x, y + 7, x + 20, y + 7);
		line.setColor(this.color);
		line.drawColorDotLine(g, 6, 2);
		g.setColor(oldColor);
	}

	public int getIconWidth() {
		return 20;
	}

	public int getIconHeight() {
		return 14;
	}
}
