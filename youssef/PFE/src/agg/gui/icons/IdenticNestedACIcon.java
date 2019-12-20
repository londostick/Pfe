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


public class IdenticNestedACIcon extends TextIcon {


	public IdenticNestedACIcon(String name, boolean enabled) {
		super(name, enabled);
	}

	public void paintIcon(Component comp, Graphics grs, int xp, int yp) {
		if (comp == null || grs == null)
			return;
		
		if (this.isEnabled) {
			if (this.col == null)
				grs.setColor(Color.black);
			else
				grs.setColor(this.col);
		} else
			grs.setColor(Color.gray.darker());

		grs.drawString("I", xp, yp + 15);
		grs.drawString("G", xp + 5, yp + 8);
		grs.drawString("A", xp + 2, yp + 15);
		grs.drawString("C", xp + 9, yp + 15);	
	}

}
