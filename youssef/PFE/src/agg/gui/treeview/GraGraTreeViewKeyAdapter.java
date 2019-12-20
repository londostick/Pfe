/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.treeview;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdRule;
import agg.editor.impl.EdRuleScheme;
import agg.gui.event.TreeViewEvent;
import agg.gui.treeview.nodedata.GraGraTreeNodeData;
import agg.gui.treeview.nodedata.GrammarTreeNodeData;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.Rule;
import agg.xt_basis.agt.RuleScheme;


public class GraGraTreeViewKeyAdapter extends KeyAdapter {
	
	private GraGraTreeView treeView;
	private TreePath selPath;
	private DefaultMutableTreeNode graNode;
	private EdGraGra gragra;
	private boolean copyRule=false;
	private EdRule ruleCopy;
	private int indx;
	
	public GraGraTreeViewKeyAdapter(GraGraTreeView treeview) {
		super();
		this.treeView = treeview;
		treeView.addKeyListener(this);
	}
	
	public void keyPressed(KeyEvent e) {
//		System.out.println("pressed:::   "+e.getKeyCode()+"    "+KeyEvent.getKeyText(e.getKeyCode()));
		if (!performShortKeyEvent(e, true))
			propagateShortKeyToEditor(e);
	}
	
//	public void keyReleased(KeyEvent e) {
//		System.out.println("released:::   "+e.getKeyCode()+"    "+KeyEvent.getKeyText(e.getKeyCode()));
//		if (!performShortKeyEvent(e, true))
//			propagateShortKeyToEditor(e);
//	}
	
	protected boolean performShortKeyEvent(KeyEvent e, boolean treeviewowner) {
		final int keyCode = e.getKeyCode();
		
		if (e.isControlDown()) {
			final String typedKey = KeyEvent.getKeyText(keyCode);
			if (e.isAltDown()) {
				if (typedKey.equals("T"))
					this.treeView.executeCommand("newTypeGraph");
				else if (typedKey.equals("G"))
					this.treeView.executeCommand("newGraph");
				else if (typedKey.equals("R"))
					this.treeView.executeCommand("newRule");
				else if (typedKey.equals("A"))
					this.treeView.executeCommand("newAtomic");
				else if (typedKey.equals("C"))
					this.treeView.executeCommand("newConstraint");
				else
					return false;
			}
			else if (e.isShiftDown()) {
				if (typedKey.equals("G"))
					this.treeView.resetGraph();
				else
					return false;
			}
			else {
				if (typedKey.equals("C"))
					copySelected();
				else if (typedKey.equals("V"))
					pasteSelected();
				else if (typedKey.equals("N"))
					this.treeView.executeCommand("newGraGra");
				else if (typedKey.equals("O"))
					this.treeView.executeCommand("open");
				else if (typedKey.equals("S"))
					this.treeView.executeCommand("save");
				else if (typedKey.equals("W"))
					this.treeView.delete("GraGra");
				else if (typedKey.equals("Q"))
					this.treeView.executeCommand("exit");
				else
					return false;
			}
		} 
		else if (e.isShiftDown()) {
			if (e.isAltDown()) {
				final String typedKey = KeyEvent.getKeyText(keyCode);
				if (typedKey.equals("G"))
					this.treeView.executeCommand("importGGX");
				else if (typedKey.equals("X"))
					this.treeView.executeCommand("importGXL");
				else if (typedKey.equals("O"))
					this.treeView.executeCommand("importOMONDOXMI");
				else if (typedKey.equals("N"))
					this.treeView.executeCommand("newNAC");
				else if (typedKey.equals("L"))
					this.treeView.setRuleLayer();
				else if (typedKey.equals("P"))
					this.treeView.setRulePriority();
				else if (typedKey.equals("C"))
					this.treeView.executeCommand("newConclusion");
				else if (typedKey.equals("R"))
					this.treeView.reloadGraGra();
				else if (typedKey.equals("D"))
					this.treeView.copyRule();
				else
					return false;
			}
			else {				
				final String typedKey = KeyEvent.getKeyText(keyCode);
				if (typedKey.equals("J"))
					this.treeView.executeCommand("exportGraphJPEG");
				else if (typedKey.equals("X"))
					this.treeView.executeCommand("exportGXL");
				else if (typedKey.equals("T"))
					this.treeView.executeCommand("exportGTXL");
				else
					return false;
			}
		} 
		else if (e.isAltDown()) {
			final String typedKey = KeyEvent.getKeyText(keyCode);
			if (typedKey.equals("S"))
				this.treeView.executeCommand("saveAs");
			else
				return false;
		} 
		else if (KeyEvent.getKeyText(keyCode).equals("F")) {
			this.treeView.getFileMenu().doClick();
		}
		else if (KeyEvent.getKeyText(keyCode).equals("Delete")
				|| KeyEvent.getKeyText(keyCode).equals("Entf")) {
			if (this.treeView.getSelectedPath() != null) {
				this.treeView.deleteTreeNode(
						(DefaultMutableTreeNode) this.treeView.getSelectedPath().getLastPathComponent(), 
						this.treeView.getSelectedPath(), true);
			}
			else if (treeviewowner)
				return false;
		}
		else
			return false;

		return true;
	}

	protected void propagateShortKeyToEditor(final KeyEvent e) {
		this.treeView.fireTreeViewEvent(new TreeViewEvent(this,
				TreeViewEvent.TRANSFER_SHORTKEY, e));
	}
	
	
	protected void copySelected() {
		this.copyRule = false;
		this.graNode = null;
		this.ruleCopy = null;
		this.gragra = null;
		this.indx = -1;
		this.selPath = this.treeView.getSelectedPath();
		if (this.selPath != null) {
			DefaultMutableTreeNode rnode = (DefaultMutableTreeNode) this.selPath.getLastPathComponent();
			GraGraTreeNodeData rd = (GraGraTreeNodeData) rnode.getUserObject();
			if (rd.isRuleScheme()) {
				TreePath graPath = this.selPath.getParentPath();
				if (graPath != null) {
					this.graNode = (DefaultMutableTreeNode) graPath.getLastPathComponent();
					if (this.graNode.getUserObject() instanceof GrammarTreeNodeData) {
						// copy a rule scheme
						GraGraTreeNodeData 
						graData = (GraGraTreeNodeData) this.graNode.getUserObject();
						if (graData.getGraGra().isEditable()) {
							RuleScheme r = BaseFactory.theFactory().cloneRuleScheme(rd.getRuleScheme().getBasisRuleScheme(),
									rd.getRule().getTypeSet().getBasisTypeSet());
							r.setName(rd.getRuleScheme().getBasisRuleScheme().getName() + "_clone");
							this.ruleCopy = new EdRuleScheme(r, rd.getRuleScheme().getTypeSet());
							((EdRuleScheme) this.ruleCopy).setLayoutByIndexFrom(rd.getRuleScheme());
							
							this.indx = this.graNode.getIndex(rnode) + 1;
							this.copyRule = true;
							this.gragra = graData.getGraGra();
							this.treeView.fireTreeViewEvent(new TreeViewEvent(this,
									TreeViewEvent.RULE_COPY,
									"<Ctrl + C>  Copy  RuleScheme  -  successful"));
						}
					}
				}
			}
			else if (rd.isRule()) {
				// copy a rule
				TreePath graPath = this.selPath.getParentPath();
				if (graPath != null) {
					this.graNode = (DefaultMutableTreeNode) graPath.getLastPathComponent();
					if (this.graNode.getUserObject() instanceof GrammarTreeNodeData) {
						// copy a simple rule
						GraGraTreeNodeData 
						graData = (GraGraTreeNodeData) this.graNode.getUserObject();
						if (graData.getGraGra().isEditable()) {						
							Rule r = BaseFactory.theFactory().cloneRule(rd.getRule().getBasisRule(),
									rd.getRule().getTypeSet().getBasisTypeSet(), true);
							r.setName(rd.getRule().getBasisRule().getName() + "_clone");
							this.ruleCopy = new EdRule(r, rd.getRule().getTypeSet());
							this.ruleCopy.setLayoutByIndexFrom(rd.getRule(), false);
		
							this.indx = this.graNode.getIndex(rnode) + 1;
							this.copyRule = true;
							this.gragra = graData.getGraGra();
							this.treeView.fireTreeViewEvent(new TreeViewEvent(this,
									TreeViewEvent.RULE_COPY,
									"<Ctrl + C>  Copy  Rule  -  successful"));
						}
					}
				}
			}
		}
	}
	
	protected void pasteSelected() {
		if (this.copyRule) {
			this.selPath = this.treeView.getSelectedPath();
			if (this.selPath != null) {
				DefaultMutableTreeNode rnode = (DefaultMutableTreeNode) this.selPath.getLastPathComponent();
				GraGraTreeNodeData rd = (GraGraTreeNodeData) rnode.getUserObject();
				if (rd.isRule()) {
					TreePath graPath = this.selPath.getParentPath();
					if (graPath != null) {
						this.graNode = (DefaultMutableTreeNode) graPath.getLastPathComponent();						
						GraGraTreeNodeData graData = (GraGraTreeNodeData) this.graNode.getUserObject();
						if (graData instanceof GrammarTreeNodeData) {
							// copy and paste rule inside the same grammar only
							if (((GrammarTreeNodeData)graData).getGraGra() == this.gragra) {
								this.indx = this.graNode.getIndex(rnode) + 1;
								int r_indx = graData.getGraGra().getRules().indexOf(rd.getRule()) + 1;
								if (this.ruleCopy instanceof EdRuleScheme) {
									this.ruleCopy.setGraGra(graData.getGraGra());								
									graData.getGraGra().getBasisGraGra().getListOfRules().add(r_indx, 
											((EdRuleScheme)this.ruleCopy).getBasisRuleScheme());
									graData.getGraGra().getRules().add(r_indx, ((EdRuleScheme)this.ruleCopy));
									this.treeView.putRuleSchemeIntoTree((EdRuleScheme)this.ruleCopy, this.graNode, this.indx);
									TreePath tp = this.treeView.getTreePathOfGrammarElement((EdRuleScheme)this.ruleCopy);									
									this.treeView.selectPath(tp);

									this.treeView.fireTreeViewEvent(new TreeViewEvent(this,
											TreeViewEvent.RULE_ADDED, tp)); //this.selPath));
									this.treeView.fireTreeViewEvent(new TreeViewEvent(this,
											TreeViewEvent.RULE_COPY,
											"<Ctrl + V>  Paste  RuleScheme  -  successful"));
									this.copyRule = false;
								}
								else {
									this.ruleCopy.setGraGra(graData.getGraGra());
									graData.getGraGra().getBasisGraGra().getListOfRules().add(r_indx, this.ruleCopy.getBasisRule());
									graData.getGraGra().getRules().add(r_indx, this.ruleCopy);
									this.treeView.putRuleIntoTree(this.ruleCopy, this.graNode, this.indx);
									TreePath tp = this.treeView.getTreePathOfGrammarElement(this.ruleCopy);									
									this.treeView.selectPath(tp);
									
									this.treeView.fireTreeViewEvent(new TreeViewEvent(this,
											TreeViewEvent.RULE_ADDED, tp)); //this.selPath));
									this.treeView.fireTreeViewEvent(new TreeViewEvent(this,
											TreeViewEvent.RULE_COPY,
											"<Ctrl + V>  Paste  Rule  -  successful"));
									this.copyRule = false;
								}
							}
						}
					}
				}
			}
		}
	}
	
}
