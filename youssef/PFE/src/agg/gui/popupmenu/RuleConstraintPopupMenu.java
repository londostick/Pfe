/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.popupmenu;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import agg.gui.treeview.GraGraTreeView;
import agg.gui.treeview.nodedata.GraGraTreeNodeData;

@SuppressWarnings("serial")
public class RuleConstraintPopupMenu extends JPopupMenu {

	public RuleConstraintPopupMenu(GraGraTreeView tree) {
		super("RuleConstraint");
		this.treeView = tree;
		JMenuItem mi = add(new JMenuItem("Delete                  Delete"));
		mi.setActionCommand("deleteRuleConstraint");
		mi.addActionListener(this.treeView.getActionAdapter());
		setBorderPainted(true);
		pack();
	}

	public boolean invoked(int x, int y) {
		if (this.treeView == null) {
			return false;
		}
		if (this.treeView.getTree().getRowForLocation(x, y) != -1) {
			TreePath path = this.treeView.getTree().getPathForLocation(x, y);
			if (path.getPath().length == 4) {
				DefaultMutableTreeNode aNode = (DefaultMutableTreeNode) path
						.getLastPathComponent();
				GraGraTreeNodeData sd = (GraGraTreeNodeData) aNode
						.getUserObject();
				if (sd != null && sd.isRuleConstraint())
					return true;
			}
		}
		return false;
	}

	private GraGraTreeView treeView;
}
