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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

public class StopIcon implements Icon {

	boolean isEnabled;

	Color color;

	public StopIcon(Color aColor) {
		this.color = aColor;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		if (this.isEnabled)
			g.setColor(this.color);
		else
			g.setColor(Color.gray.darker());
		g.drawString(" S ", x + 2, y + 13);
		if (this.isEnabled)
			g.setColor(Color.black);
		else
			g.setColor(Color.gray.darker());
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(2.0f));
		g2.drawLine(x, y + 10, x + getIconWidth() / 2, y + getIconHeight());
		g2.drawLine(x + getIconWidth() / 2, y + getIconHeight(), x
				+ getIconWidth(), y + 2);

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
