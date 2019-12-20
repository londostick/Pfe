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
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.Icon;

public class FormulaOPIcon implements Icon {

	Color color;
	int W,H;
	
	public FormulaOPIcon(Color aColor) {
		this.color = aColor;
	}

	public FormulaOPIcon(Color aColor, int w, int h) {
		this.color = aColor;
		this.W = w;
		this.H = h;
	}
	
	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.setColor(Color.white);
		g.fillRect(x, y, getIconWidth(), getIconHeight());
		g.setColor(this.color);
		
		Font font = g.getFont();
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD,14));
		
		g.drawString("N", x + 0, y + 15);
		g.drawString("A", x + 8, y + 15);
		g.drawString("C", x + 16, y + 15);
		
		g.setColor(Color.red);
		g.drawString("F", x + 20, y + 8);
		g.setColor(this.color);
		
		g.setFont(font);
	}

	public int getIconWidth() {
		return this.W; //24;
	}

	public int getIconHeight() {
		return this.H; //16;
	}

}
