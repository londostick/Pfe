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

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import agg.editor.impl.EdGraph;
import agg.gui.treeview.GraGraTreeView;
import agg.gui.treeview.nodedata.GraGraTextualComment;
import agg.gui.treeview.nodedata.GraGraTreeNodeData;

@SuppressWarnings("serial")
public class GraphPopupMenu extends JPopupMenu {

	public GraphPopupMenu(GraGraTreeView tree) {
		super("Graph");
		this.treeView = tree;

		JMenuItem mi = add(new JMenuItem("Reset"));
		mi.setActionCommand("resetGraph");
//		mi.addActionListener(this.treeView);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				treeView.resetGraph(graph);
			}
		});
		// mi.setMnemonic('r');
		
		mi = add(new JMenuItem("Delete                   Delete"));
		mi.setActionCommand("deleteGraph");
//		mi.addActionListener(this.treeView);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (treeView.hasMultipleSelection())
					treeView.delete("selected");
				else
					treeView.deleteGraph(node, path, true);
			}
		});
		mi.setEnabled(false);
		this.delete = mi;

		addSeparator();

		mi = add(new JMenuItem("Textual Comments"));
		mi.setActionCommand("commentGraph");
//		mi.addActionListener(this.treeView);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editComments();
			}
		});

		pack();
		setBorderPainted(true);
	}

	public boolean invoked(int x, int y) {
		if (this.treeView == null) {
			return false;
		}
		if (this.treeView.getTree().getRowForLocation(x, y) != -1) {
			this.path = this.treeView.getTree().getPathForLocation(x, y);
			if (this.path.getPath().length == 3) {
				this.node = (DefaultMutableTreeNode) this.path.getLastPathComponent();
				this.data = (GraGraTreeNodeData) node.getUserObject();
				if (this.data != null && this.data.isGraph() && !this.data.isTypeGraph()) {
					this.graph = this.treeView.getGraph(this.node);
					if (this.graph.getGraGra().getGraphs().size() > 1)
						this.delete.setEnabled(true);
					else
						this.delete.setEnabled(false);
					return true;
				}
			}
		}
		return false;
	}

	void editComments() {
		if (this.graph != null) {
			this.treeView.cancelCommentsEdit();
			Point p = this.treeView.getPopupMenuLocation();
			if (p == null) 
				p = new Point(200,200);
			GraGraTextualComment 
			comments = new GraGraTextualComment(this.treeView.getFrame(), p.x,
						p.y, this.graph.getBasisGraph());

			if (comments != null)
				comments.setVisible(true);
		}
	}
	
	
	private GraGraTreeView treeView;
	TreePath path;
	DefaultMutableTreeNode node;
	GraGraTreeNodeData data;
	EdGraph graph;
	
	private JMenuItem delete;
}
