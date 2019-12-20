/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import agg.gui.treeview.GraGraTreeView;

public class AGGConstraints {

//	private final AGGAppl parent;

	private final Vector<JMenu> menus;

	public AGGConstraints(AGGAppl appl, GraGraTreeView tree) {
//		parent = appl;
		this.menus = new Vector<JMenu>(1);
		createMenus(tree);
	}

	public Enumeration<JMenu> getMenus() {
		return this.menus.elements();
	}

	private void createMenus(GraGraTreeView view) {
		this.menus.clear();
		JMenu m = new JMenu("Consistency Check");
		m.setMnemonic('o');

		JMenuItem it = m.add(new JMenuItem("Check Atomics"));
		it.setActionCommand("checkAtomics");
		it.addActionListener(view.getActionAdapter());

		it = m.add(new JMenuItem("Check Constraints"));
		it.setActionCommand("checkConstraints");
		it.addActionListener(view.getActionAdapter());

		this.menus.add(m);
	}

}
