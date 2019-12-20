/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
// $Id: RulePopupMenu.java,v 1.18 2010/09/19 16:25:01 olga Exp $

package agg.gui.popupmenu;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import agg.editor.impl.EdRule;
import agg.gui.treeview.GraGraTreeView;
import agg.xt_basis.agt.AmalgamatedRule;


@SuppressWarnings("serial")
public class AmalgamRulePopupMenu extends JPopupMenu {

	public AmalgamRulePopupMenu(GraGraTreeView tree) {
		super("Amalgamated Rule");
		this.treeView = tree;


		JMenuItem miDelete = add(new JMenuItem(
				"Delete                                              Delete"));
		miDelete.setActionCommand("deleteRule");
		miDelete.addActionListener(this.treeView.getActionAdapter());

		addSeparator();
		
		JMenuItem miCopy = add(new JMenuItem("Copy into rule set "));
		miCopy.setActionCommand("copyRule");
		miCopy.addActionListener(this.treeView.getActionAdapter());

		pack();
		setBorderPainted(true);
	}

	public boolean invoked(int x, int y) {
		if (this.treeView == null) {
			return false;
		}
		
		if (this.treeView.getTree().getRowForLocation(x, y) != -1) {
			int pl = this.treeView.getTree().getPathForLocation(x, y).getPath().length;
			if (pl == 4) {
				TreePath path = this.treeView.getTree().getPathForLocation(x, y);
				EdRule rule = this.treeView.getRule((DefaultMutableTreeNode) path
						.getLastPathComponent());
				
				if (rule != null
						&& rule.getBasisRule() instanceof AmalgamatedRule) {
					return true;
				} 
			} 
		} 
		return false;
	}		
	
	GraGraTreeView treeView;

	
//	private JMenuItem miComment;
		
}
