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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import agg.editor.impl.EdNestedApplCond;
import agg.editor.impl.EdRule;
import agg.gui.AGGAppl;
import agg.gui.treeview.GraGraTreeView;
import agg.gui.treeview.dialog.FormulaGraphGUI;
import agg.gui.treeview.nodedata.ApplFormulaTreeNodeData;
import agg.gui.treeview.nodedata.GraGraTreeNodeData;
import agg.gui.treeview.path.GrammarTreeNode;
import agg.xt_basis.NestedApplCond;
import agg.xt_basis.agt.KernelRule;


@SuppressWarnings("serial")
public class KernelRulePopupMenu extends JPopupMenu {

	public KernelRulePopupMenu(GraGraTreeView tree) {
		super("Kernel Rule");
		this.treeView = tree;

		this.miAC = add(new JMenuItem("New GAC (General Application Condition)"));
		this.miAC.setActionCommand("newNestedAC");
		this.miAC.addActionListener(this.treeView.getActionAdapter());

		this.miAC1 = new JMenuItem("Make GAC due to RHS");
		this.add(miAC1);
		this.miAC1.setActionCommand("makeGACFromRHS");
		this.miAC1.addActionListener(this.treeView.getActionAdapter());
		
		this.miFormula = add(new JMenuItem("Set Formula above GACs"));
		this.miFormula.setActionCommand("setFormulaAboveACs");
		this.miFormula.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setFormula();
			}
		});
		
		addSeparator();
		
		this.miNAC = add(new JMenuItem(
				"New NAC                                 Shift+Alt+N"));
		this.miNAC.setActionCommand("newNAC");
		this.miNAC.addActionListener(this.treeView.getActionAdapter());
		// miNAC.setMnemonic('N');

		this.miNAC1 = add(new JMenuItem(
				"Make NAC due to RHS               "));
		this.miNAC1.setActionCommand("makeNACFromRHS");
		this.miNAC1.addActionListener(this.treeView.getActionAdapter());
		
		addSeparator();
		
		this.miPAC = add(new JMenuItem("New PAC                                 "));// Shift+Alt+A
		this.miPAC.setActionCommand("newPAC");
		this.miPAC.addActionListener(this.treeView.getActionAdapter());
		// miPAC.setMnemonic('P');
		
		addSeparator();
		
		this.miAttrContext = add(new JMenuItem("Attribute Context"));
		this.miAttrContext.setActionCommand("attrContext");
		this.miAttrContext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((AGGAppl)treeView.getFrame()).getGraGraEditor().loadRuleAttrContextInEditor(rule);
			}
		});

		addSeparator();
		
		this.miComment = add(new JMenuItem("Textual Comments"));
		this.miComment.setActionCommand("commentRule");
		this.miComment.addActionListener(this.treeView.getActionAdapter());
		// miComment.setMnemonic('T');

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
				this.path = this.treeView.getTree().getPathForLocation(x, y);
				this.node = (DefaultMutableTreeNode) this.path.getLastPathComponent();
				this.data = (GraGraTreeNodeData) this.node.getUserObject();
				this.rule = this.treeView.getRule((DefaultMutableTreeNode) 
													this.path.getLastPathComponent());

				if (this.rule != null
						&& this.rule.getBasisRule() instanceof KernelRule) {
					
					GrammarTreeNode.expand(this.treeView, this.node, this.path);
					this.posX = x; this.posY = y;
					
					return true;
				} 
			} 
		} 
		return false;
	}

	
	void setFormula() {	
		String ownerName = "rule : "+this.rule.getBasisRule().getName();
		FormulaGraphGUI d = new FormulaGraphGUI(this.treeView.getFrame(), ownerName,
				" Graph editor of Formula above General Application Conditions ", 
				" An empty graph is the case where all nodes are connected by AND.",
				true);
		d.setExportJPEG(this.treeView.getGraphicsExportJPEG());
		
		String oldformula = this.rule.getBasisRule().getFormulaStr();
				
//		List<String> allVars = this.rule.getBasisRule().getNameOfEnabledACs();
		List<EdNestedApplCond> allNestedACs = this.rule.getEnabledACs();
		List<NestedApplCond> list = makeFrom(allNestedACs);
		
		d.setVarsAsObjs(allNestedACs, oldformula);
		d.setLocation(this.posX+20, this.posY+20);
		
		while (true) {
			d.setVisible(true);
			if (!d.isCanceled()) {
				boolean formulaChanged = d.isChanged();
				String f = d.getFormula();
				if (!this.rule.getBasisRule().setFormula(f, list)) {
					JOptionPane
					.showMessageDialog(
							this.treeView.getFrame(),
							"The formula definition failed. Please correct.",
							" Formula failed ", JOptionPane.WARNING_MESSAGE);
					continue;
				} else {
					f = this.rule.getBasisRule().getFormulaStr();
				}
				if (formulaChanged) {
					insertFormulaIntoRuleTreeNode(f, allNestedACs);
					this.rule.getGraGra().setChanged(true);	
					break;
				} else
					break;
			} else break;
		}	
	}
	

	private List<NestedApplCond> makeFrom(
			List<EdNestedApplCond> list) {
		final List<NestedApplCond> result = new Vector<NestedApplCond>(list.size(), 0);
		for (int i=0; i<list.size(); i++) {
			result.add(list.get(i).getNestedMorphism());
		}
		return result;
	}

	void insertFormulaIntoRuleTreeNode(
			final String f, 
			final List<EdNestedApplCond> allVarsObj) {
		if (f.length() > 0) {			
			Object child = this.treeView.getTreeModel().getChild(this.node, 0);
			if (child != null) {
				// remove existing formula from rule subtree
				if (((GraGraTreeNodeData) ((DefaultMutableTreeNode) child)
						.getUserObject()).isApplFormula()) {
					this.treeView.getTreeModel().removeNodeFromParent((DefaultMutableTreeNode)child);
				}
				
				// add formula tree node into rule subtree
				if (!"true".equals(f)) {
					final ApplFormulaTreeNodeData conddata = new ApplFormulaTreeNodeData(f, true, this.rule);
					conddata.setString(f);
					final DefaultMutableTreeNode condnode = new DefaultMutableTreeNode(
								conddata);
					conddata.setTreeNode(condnode);
					this.treeView.getTreeModel().insertNodeInto(condnode, this.node, 0);
				}
			}
		}	
	}
		
	
	GraGraTreeView treeView;
	TreePath path;
	DefaultMutableTreeNode node;
	GraGraTreeNodeData data;
	EdRule rule;

	int posX, posY; 
	
	private JMenuItem miAC, miAC1, miFormula, miNAC, miNAC1, miPAC, miComment, miAttrContext;	
}
