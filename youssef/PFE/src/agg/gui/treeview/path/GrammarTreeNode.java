/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
/**
 * 
 */
package agg.gui.treeview.path;

import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import agg.attribute.impl.CondMember;
import agg.attribute.impl.CondTuple;
import agg.cons.AtomApplCond;
import agg.cons.EvalSet;
import agg.cons.Formula;
import agg.editor.impl.EdAtomApplCond;
import agg.editor.impl.EdAtomic;
import agg.editor.impl.EdConstraint;
import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdGraph;
import agg.editor.impl.EdNAC;
import agg.editor.impl.EdNestedApplCond;
import agg.editor.impl.EdPAC;
import agg.editor.impl.EdRule;
import agg.editor.impl.EdRuleConstraint;
import agg.editor.impl.EdRuleScheme;
import agg.gui.AGGAppl;
import agg.gui.event.TreeViewEvent;
import agg.gui.saveload.GraGraLoad;
import agg.gui.saveload.GraGraSave;
import agg.gui.treeview.GraGraTreeModel;
import agg.gui.treeview.GraGraTreeView;
import agg.gui.treeview.nodedata.AmalgamatedRuleTreeNodeData;
import agg.gui.treeview.nodedata.ApplFormulaTreeNodeData;
import agg.gui.treeview.nodedata.AtomicGraphConstraintTreeNodeData;
import agg.gui.treeview.nodedata.ConclusionAttrConditionTreeNodeData;
import agg.gui.treeview.nodedata.ConclusionTreeNodeData;
import agg.gui.treeview.nodedata.ConstraintTreeNodeData;
import agg.gui.treeview.nodedata.GraGraTreeNodeData;
import agg.gui.treeview.nodedata.GrammarTreeNodeData;
import agg.gui.treeview.nodedata.GraphTreeNodeData;
import agg.gui.treeview.nodedata.KernelRuleTreeNodeData;
import agg.gui.treeview.nodedata.MultiRuleTreeNodeData;
import agg.gui.treeview.nodedata.NACTreeNodeData;
import agg.gui.treeview.nodedata.NestedACTreeNodeData;
import agg.gui.treeview.nodedata.PACTreeNodeData;
import agg.gui.treeview.nodedata.RuleApplConstraintTreeNodeData;
import agg.gui.treeview.nodedata.RuleAtomicApplConstraintTreeNodeData;
import agg.gui.treeview.nodedata.RuleAttrCondTreeNodeData;
import agg.gui.treeview.nodedata.RuleSchemeTreeNodeData;
import agg.gui.treeview.nodedata.RuleSequenceTreeNodeData;
import agg.gui.treeview.nodedata.RuleTreeNodeData;
import agg.gui.treeview.nodedata.TypeGraphTreeNodeData;
import agg.ruleappl.RuleSequence;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.TypeSet;


/**
 * @author olga
 *
 */
@SuppressWarnings("serial")
public class GrammarTreeNode extends DefaultMutableTreeNode {

	private final int KERNEL = 0;
	private final int MULTI = 1;
	private final int AMALGAMATED = 2;
	
	
	public GrammarTreeNode() {}
	
	public GrammarTreeNode(final EdGraGra gragra) {
		super(new GrammarTreeNodeData(gragra));
		
		((GrammarTreeNodeData) this.getUserObject()).setTreeNode(this);
	}
	
	/** Saves the specified gragra */
	public void saveGraGra(
			final GraGraTreeView treeView,
			final GraGraSave gragraSave, 
			final String directory, 
			final EdGraGra gragra) {
		
		treeView.fireTreeViewEvent(new TreeViewEvent(this, TreeViewEvent.SAVE));
	
		// set gragra to save
		if (gragra.getDirName().equals(""))
			gragraSave.setDirName(directory);
		
		gragraSave.setGraGra(gragra, gragra.getDirName(), 
										gragra.getFileName());
		gragraSave.save();
		
		treeView.fireTreeViewEvent(new TreeViewEvent(this, TreeViewEvent.SAVED));
	}
	
	/** Saves the specified gragra */
	public String saveAsGraGra(final GraGraTreeView treeView,
			final GraGraSave gragraSave, 
			final String directory, 
			final EdGraGra gragra) {


		treeView.fireTreeViewEvent(new TreeViewEvent(this, TreeViewEvent.SAVE));
	
		gragraSave.setDirName(directory);
		gragraSave.setGraGra(gragra, gragra.getDirName(), gragra.getFileName());
		if (gragraSave.saveAs()) {			
			((AGGAppl) treeView.getFrame())
							.addToFrameTitle(gragra.getDirName(), gragra.getFileName());
		}
		treeView.fireTreeViewEvent(new TreeViewEvent(this, TreeViewEvent.SAVED));
		return gragraSave.getDirName();
	}

	/** Saves the base grammar of the specified gragra */
	public void saveAsBaseGraGra(
			final GraGraTreeView treeView,
			final GraGraSave gragraSave, 
			final String directory, 
			final EdGraGra gragra) {

		treeView.fireTreeViewEvent(new TreeViewEvent(this, TreeViewEvent.SAVE));

		gragraSave.setGraGra(gragra);
		gragraSave.setBaseGraGra(gragra.getBasisGraGra(), "", "");
		gragraSave.saveAsBase();
		
		treeView.fireTreeViewEvent(new TreeViewEvent(this, TreeViewEvent.SAVED));
	}

	/** Reloads the specified gragra */
	public EdGraGra  reloadCurrentGraGra(
			final GraGraTreeView treeView,
			final TreePath selPath,
			final GraGraLoad gragraLoad, 
			final EdGraGra gra) {
				
		/* reload gragra */
		gragraLoad.setGraGra(gra, gra.getDirName(), gra.getFileName());
		gragraLoad.reload();
		if (gragraLoad.getGraGra() != null) {
			EdGraGra gragra = gragraLoad.getGraGra();
			BaseFactory.theFactory().notify(gragra.getBasisGraGra());

			this.refreshCurrentGraGra(treeView, selPath, gragra);
					
			return gragra;
		}
		return null;
	}
	
	/** Refresh the specified gragra */
	public void refreshCurrentGraGra(
			final GraGraTreeView treeView,
			final TreePath selPath,
			final EdGraGra gra) {
				
		/* collapse selected tree path */
		if (treeView.getTree().isExpanded(selPath))
			treeView.getTree().collapsePath(selPath);

		/* remove all gragra's children */
		DefaultMutableTreeNode 
		graNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
		while (graNode.getChildCount() > 0) {
			DefaultMutableTreeNode 
			child = (DefaultMutableTreeNode) graNode.getChildAt(0);
			treeView.getTreeModel().removeNodeFromParent(child);
			((GraGraTreeNodeData) child.getUserObject()).dispose();				
		}
		graNode.removeAllChildren();
		
		/* set gragra */
		GrammarTreeNodeData graNodeData = new GrammarTreeNodeData(gra);
		graNode.setUserObject(graNodeData);
		graNodeData.setTreeNode(graNode);
		treeView.getTreeModel().nodeChanged(graNode);

		int graIndex = treeView.getTree().getRowForPath(selPath);
		updateTreeNodeData(treeView, graNode, gra);
			
		if (!treeView.getTree().isExpanded(treeView.getTree().getPathForRow(graIndex)))
			treeView.getTree().expandPath(treeView.getTree().getPathForRow(graIndex));
						
		gra.setChanged(false);						
	}
	
	public void updateTreeNodeData(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode graNode, 
			final EdGraGra gragra) {
		
		insertGraGraDataIntoTree(treeView, graNode, gragra); 
	}
	
	
	public int insertIntoTree(
			final GraGraTreeView treeView) {
//		System.out.println("GrammarTreeNode.insertIntoTree...");
		
		final GraGraTreeModel treeModel = treeView.getTreeModel();
		DefaultMutableTreeNode modelRoot = (DefaultMutableTreeNode) treeModel.getRoot();
		treeModel.insertNodeInto(this, modelRoot, modelRoot.getChildCount());
		
		if (!treeView.getTree().isExpanded(treeView.getTree().getPathForRow(0)))
			treeView.getTree().expandPath(treeView.getTree().getPathForRow(0));
		
		// System.out.println(treeView.getTree().getRowCount());
		int graIndex = treeView.getTree().getRowCount() - 1;
		
		EdGraGra gragra = ((GrammarTreeNodeData) this.getUserObject()).getGraGra();
		insertGraGraDataIntoTree(treeView, this, gragra);
//		System.out.println(graIndex+"   "+treeView.getTree().getPathForRow(graIndex));

		if (!treeView.getTree().isExpanded(treeView.getTree().getPathForRow(graIndex)))
			treeView.getTree().expandPath(treeView.getTree().getPathForRow(graIndex));
		
		return graIndex;
	}
	
	public TreePath deleteTypeGraph(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode delNode, 
			final TreePath selPath,
			boolean withWarning) {
		
		final GraGraTreeNodeData data = (GraGraTreeNodeData) delNode.getUserObject();
		final TreePath graPath = selPath.getParentPath();		
		if (graPath != null) {
			DefaultMutableTreeNode graNode = (DefaultMutableTreeNode) graPath
					.getLastPathComponent();
			GraGraTreeNodeData graData = (GraGraTreeNodeData) graNode
					.getUserObject();
			if (graData.getGraGra().getBasisGraGra().getLevelOfTypeGraphCheck() == TypeSet.DISABLED) {
				if (data.isTypeGraph() 
						&& (data.getGraph() != treeView.getCurrentGraph())) {
					int answer = treeView.removeWarning("Type Graph");
					if (answer == JOptionPane.YES_OPTION) {
						int row = treeView.getTree().getRowForPath(selPath);
						treeView.fireTreeViewEvent(new TreeViewEvent(this,
								TreeViewEvent.DELETED, selPath));
						treeView.getTreeModel().removeNodeFromParent(delNode);
						treeView.getGraGraStore().storeTypeGraph(graData.getGraGra(), graData
								.getGraGra().getTypeGraph());
						graData.getGraGra().removeTypeGraph();
						treeView.setEditPath(row);
						treeView.setFlagForNew();
						treeView.fireTreeViewEvent(new TreeViewEvent(this,
								TreeViewEvent.SELECTED, treeView.getEditorPath()));
						return treeView.getSelectedPath();
					}
				} 
				else {
					int answer = treeView.removeCurrentObjectWarning("Type Graph");
					if (answer == JOptionPane.YES_OPTION) {
						int row = treeView.getTree().getRowForPath(selPath);
						treeView.fireTreeViewEvent(new TreeViewEvent(this,
									TreeViewEvent.DELETED, selPath));
						treeView.getTreeModel().removeNodeFromParent(delNode);
						treeView.getGraGraStore().storeTypeGraph(graData.getGraGra(), graData
								.getGraGra().getTypeGraph());
						graData.getGraGra().removeTypeGraph();					
						treeView.setEditPath(row);
						treeView.setFlagForNew();
						treeView.fireTreeViewEvent(new TreeViewEvent(this,
								TreeViewEvent.SELECTED, treeView.getEditorPath()));
						graData.getGraGra().setChanged(true);
						return treeView.getSelectedPath();
					}
				}
			} else {
				JOptionPane.showMessageDialog(treeView.getFrame(),
						"Please disable the type graph first.",
						"", JOptionPane.WARNING_MESSAGE);
			}
		}
		return selPath;
	}

	public TreePath deleteGraph(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode delNode, 
			final TreePath selPath,
			boolean withWarning) {
		final GraGraTreeNodeData data = (GraGraTreeNodeData) delNode.getUserObject();
		final TreePath graPath = selPath.getParentPath();
		if (graPath != null) {
			final DefaultMutableTreeNode graNode = (DefaultMutableTreeNode) graPath
					.getLastPathComponent();
			final GraGraTreeNodeData graData = (GraGraTreeNodeData) graNode
					.getUserObject();
			if (graData.getGraGra().getGraphs().size() > 1) {
				if (data.getGraph() != treeView.getCurrentGraph()) {
					int answer = withWarning? treeView.removeWarning("Host Graph"): 0;
					if (answer == JOptionPane.YES_OPTION) {
						int row = treeView.getTree().getRowForPath(selPath);
						treeView.fireTreeViewEvent(new TreeViewEvent(this,
								TreeViewEvent.DELETED, selPath));
						treeView.getTreeModel().removeNodeFromParent(delNode);
						treeView.getGraGraStore()
								.storeGraph(graData.getGraGra(), data.getGraph());
						graData.getGraGra().removeGraph(data.getGraph());
						row--;
						treeView.setEditPath(row);
						treeView.setFlagForNew();
						treeView.fireTreeViewEvent(new TreeViewEvent(this,
								TreeViewEvent.SELECTED, treeView.getEditorPath()));
						return treeView.getSelectedPath();
					}
				} 
				else {
					int answer = withWarning? treeView.removeCurrentObjectWarning("Host Graph"): 0;
					if (answer == JOptionPane.YES_OPTION) {
						int row = treeView.getTree().getRowForPath(selPath);
						treeView.fireTreeViewEvent(new TreeViewEvent(this,
								TreeViewEvent.DELETED, selPath));
						treeView.getTreeModel().removeNodeFromParent(delNode);
						treeView.getGraGraStore().storeGraph(graData.getGraGra(), data
								.getGraph());
						graData.getGraGra().removeGraph(data.getGraph());
						row--;
						treeView.setEditPath(row);
						treeView.setFlagForNew();
						treeView.fireTreeViewEvent(new TreeViewEvent(this,
								TreeViewEvent.SELECTED, treeView.getEditorPath()));
						return treeView.getSelectedPath();
					}
				}
			} 
			graData.getGraGra().setChanged(true);
		}
		return selPath;
	}

	public TreePath deleteRule(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode delNode,
			final TreePath selPath,
			boolean withWarning) {
		final GraGraTreeNodeData data = (GraGraTreeNodeData) delNode.getUserObject();
		final TreePath graPath = selPath.getParentPath();
		if (graPath != null) {
			final DefaultMutableTreeNode graNode = (DefaultMutableTreeNode) graPath
					.getLastPathComponent();
			final GraGraTreeNodeData graData = (GraGraTreeNodeData) graNode
					.getUserObject();
			if (data.getRule() != treeView.getCurrentRule()) {
				int answer = withWarning? treeView.removeWarning("Rule"): 0;
				if (answer == JOptionPane.YES_OPTION && delNode.getParent() != null) {
					int row = treeView.getTree().getRowForPath(selPath);
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.DELETED, selPath));
					treeView.getTreeModel().removeNodeFromParent(delNode);
					final EdRule r = data.getRule();
					treeView.getGraGraStore().storeRule(r.getGraGra(), r);
					graData.getGraGra().removeRule(r);
					// graData.getGraGra().destroyRule(r);
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.RULE_DELETED, selPath));
					row--;
					treeView.setEditPath(row);					
					treeView.setFlagForNew();
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.SELECTED, treeView.getEditorPath()));
					return treeView.getSelectedPath();
				}
			} 
			else {
				int answer = withWarning? treeView.removeCurrentObjectWarning("Rule"): 0;
				if (answer == JOptionPane.YES_OPTION && delNode.getParent() != null) {
					int row = treeView.getTree().getRowForPath(selPath);
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.DELETED, selPath));
					treeView.getTreeModel().removeNodeFromParent(delNode);
					EdRule r = data.getRule();
					treeView.getGraGraStore().storeRule(r.getGraGra(), r);
					graData.getGraGra().removeRule(r);	
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.RULE_DELETED, selPath));
					row--;
					treeView.setEditPath(row);					
					treeView.setFlagForNew();
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.SELECTED, treeView.getEditorPath()));
						
					if ((((GraGraTreeNodeData)((DefaultMutableTreeNode)treeView.getSelectedPath()
								.getLastPathComponent()).getUserObject()).isGraph()
							&& !treeView.getCurrentGraGra().getRules().isEmpty())) {						
						row++;
						treeView.setEditPath(row);					
						treeView.setFlagForNew();
						treeView.fireTreeViewEvent(new TreeViewEvent(this,
								TreeViewEvent.SELECTED, treeView.getEditorPath()));
					}
					return treeView.getSelectedPath();
				}
			}
		}
		return selPath;
	}

	public TreePath deleteRuleScheme(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode delNode,
			final TreePath selPath,
			boolean withWarning) {

		final GraGraTreeNodeData data = (GraGraTreeNodeData) delNode.getUserObject();
		final TreePath graPath = selPath.getParentPath();
		if (graPath != null) {
			DefaultMutableTreeNode graNode = (DefaultMutableTreeNode) graPath
					.getLastPathComponent();
			GraGraTreeNodeData graData = (GraGraTreeNodeData) graNode
					.getUserObject();
			if (data.getRuleScheme() != treeView.getCurrentRuleScheme()) {
				int answer = withWarning? treeView.removeWarning("RuleScheme"): 0;
				if (answer == JOptionPane.YES_OPTION) {
					int row = treeView.getTree().getRowForPath(selPath);
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.DELETED, selPath));
					treeView.getTreeModel().removeNodeFromParent(delNode);
					EdRule rs = data.getRuleScheme();
					treeView.getGraGraStore().storeRuleScheme(rs.getGraGra(), (EdRuleScheme)rs);
					graData.getGraGra().removeRule(rs);
	
					row--;
					treeView.setEditPath(row);					
					treeView.setFlagForNew();
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.SELECTED, treeView.editorPath));
					return treeView.getSelectedPath();
				}
			} else {
				int answer = withWarning? treeView.removeCurrentObjectWarning("RuleScheme"): 0;
				if (answer == JOptionPane.YES_OPTION) {
					int row = treeView.getTree().getRowForPath(selPath);
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.DELETED, selPath));
					treeView.getTreeModel().removeNodeFromParent(delNode);
					EdRule rs = data.getRule();
					treeView.getGraGraStore().storeRuleScheme(rs.getGraGra(), (EdRuleScheme)rs);
					graData.getGraGra().removeRule(rs);
	
					row--;
					treeView.setEditPath(row);					
					treeView.setFlagForNew();
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.SELECTED, treeView.editorPath));
					
					if ((((GraGraTreeNodeData)((DefaultMutableTreeNode)
							treeView.selPath.getLastPathComponent())
												.getUserObject()).isGraph()
							&& !treeView.getCurrentGraGra().getRules().isEmpty())) {						
						row++;
						treeView.setEditPath(row);					
						treeView.setFlagForNew();
						treeView.fireTreeViewEvent(new TreeViewEvent(this,
								TreeViewEvent.SELECTED, treeView.editorPath));
					}
					return treeView.getSelectedPath();
				}
			}
		}
		return selPath;
	}
	
	public TreePath deleteRuleSequence(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode delNode,
			final TreePath selPath,
			boolean withWarning) {
		final GraGraTreeNodeData data = (GraGraTreeNodeData) delNode.getUserObject();
		final TreePath graPath = selPath.getParentPath();
		if (graPath != null) {
			final DefaultMutableTreeNode graNode = (DefaultMutableTreeNode) graPath
					.getLastPathComponent();
			final GraGraTreeNodeData graData = (GraGraTreeNodeData) graNode
					.getUserObject();
			if (data.getRuleSequence() == treeView.getCurrentRuleSequence()) {				
				int row = treeView.getTree().getRowForPath(selPath);
				int answer = withWarning? treeView.removeCurrentObjectWarning("Rule Sequence"): 0;
				if (answer == JOptionPane.YES_OPTION) {					
					treeView.getTreeModel().removeNodeFromParent(delNode);
					graData.getGraGra().getBasisGraGra().removeRuleSequence(data.getRuleSequence());
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.RULE_SEQUENCE_DELETED, selPath));
					row--;
					treeView.setEditPath(row);					
					treeView.setFlagForNew();
					treeView.fireTreeViewEvent(new TreeViewEvent(this,
							TreeViewEvent.SELECTED, treeView.getEditorPath()));
					return treeView.getSelectedPath();
				}
			}
		}
		return selPath;
	}

	private void insertGraGraDataIntoTree(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode graNode, 
			final EdGraGra gragra) {
		
		/* create the type graph node */
		final EdGraph typeGraph = gragra.getTypeSet().getTypeGraph();
		if (typeGraph != null) {	
			insertTypeGraphIntoTree(treeView.getTreeModel(), graNode, typeGraph);			
		}

		/* create tree nodes of graphs*/
		// System.out.println(gragra.getGraphs().size());
		for (int i = 0; i < gragra.getGraphs().size(); i++) {
			final EdGraph g = gragra.getGraphs().get(i);
			insertGraphIntoTree(treeView.getTreeModel(), graNode, g);			
		}
		
		/* create tree nodes of rules*/
		for (int i = 0; i < gragra.getRules().size(); i++) {
			final EdRule r = gragra.getRules().elementAt(i);
			if (r instanceof EdRuleScheme) {
				insertRuleSchemeIntoTree(treeView, graNode, (EdRuleScheme) r);
			} else {
				insertRuleIntoTree(treeView, graNode, r);
			}
		}
		
		/* create atomics nodes */
		for (int i = 0; i < gragra.getAtomics().size(); i++) {
			EdAtomic a = gragra.getAtomics().elementAt(i);
			insertAtomicGraphConstraintIntoTree(treeView.getTreeModel(), graNode, a);
		}
		
		/* create constraints (formulas) nodes */
		for (int i = 0; i < gragra.getConstraints().size(); i++) {
			final EdConstraint c = gragra.getConstraints().get(i);
			insertFormulaIntoTree(treeView.getTreeModel(), graNode, c);
		}
		
		/* create rule sequence nodes */
		if (gragra.getBasisGraGra().trafoByRuleSequence()) {
			for (int i = 0; i < gragra.getBasisGraGra().getRuleSequences().size(); i++) {
				final RuleSequence rs = gragra.getBasisGraGra().getRuleSequences().get(i);
				insertRuleSequenceIntoTree(treeView.getTreeModel(), graNode, rs);
			}
		}
		
//		if (gragra.getConstraints().size() != 0) 
		{
			if (gragra.getBasisGraGra().isLayered()) {
				treeView.getTreeModel().ruleNameChanged(gragra, true);
				treeView.getTreeModel().constraintNameChanged(gragra, true);
			}
			else if (gragra.getBasisGraGra().trafoByPriority()) {
				treeView.getTreeModel().ruleNameChanged(gragra, false, true);
				treeView.getTreeModel().constraintNameChanged(gragra, false, true);
			}
		}
	}
	
	private void insertTypeGraphIntoTree(
			final GraGraTreeModel treeModel,
			final DefaultMutableTreeNode graNode,
			final EdGraph typeGraph) {
		
		EdGraGra gragra = ((GrammarTreeNodeData) graNode.getUserObject()).getGraGra();
		TypeGraphTreeNodeData sd = new TypeGraphTreeNodeData(typeGraph);
		DefaultMutableTreeNode typeGraphNode = new DefaultMutableTreeNode(sd);
		sd.setTreeNode(typeGraphNode);
		String mode = "";
		switch (gragra.getBasisGraGra().getTypeSet()
				.getLevelOfTypeGraphCheck()) {
		case TypeSet.DISABLED:
			mode = "[D]";
			break;
		case TypeSet.ENABLED:
			mode = "[E]";
			break;
		case TypeSet.ENABLED_MAX:
			mode = "[Em]";
			break;
		case TypeSet.ENABLED_MAX_MIN:
			mode = "[Emm]";
			break;
		default:
			mode = "[?]";
		}
		final String name = mode + typeGraph.getBasisGraph().getName();
		sd.setString(name);
		treeModel.insertNodeInto(typeGraphNode, graNode, graNode
				.getChildCount());
	}
	
	private void insertGraphIntoTree(
			final GraGraTreeModel treeModel,
			final DefaultMutableTreeNode graNode,
			final EdGraph graph) {
		
		final GraGraTreeNodeData sd = new GraphTreeNodeData(graph);
		final DefaultMutableTreeNode graphNode = new DefaultMutableTreeNode(sd);
		sd.setTreeNode(graphNode);
		sd.setString(graph.getBasisGraph().getName());
		treeModel.insertNodeInto(graphNode, graNode, graNode
				.getChildCount());
	}
	
	public void insertRuleIntoTree(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode graNode,
			final EdRule rule) {
		
		RuleTreeNodeData sd = new RuleTreeNodeData(rule);
		final DefaultMutableTreeNode ruleNode = new DefaultMutableTreeNode(sd);
		sd.setTreeNode(ruleNode);
		treeView.getTreeModel().insertNodeInto(ruleNode, graNode, graNode.getChildCount());
		
		/* insert formula above nested ACs into rule tree*/
		insertRuleFormulaIntoTree(treeView, ruleNode,rule.getBasisRule().getFormulaStr());		
		/* create Nested AC tree nodes */
		for (int j = 0; j < rule.getNestedACs().size(); j++) {
			final EdNestedApplCond ac = (EdNestedApplCond) rule.getNestedACs().get(j);
			insertNestedACIntoTree(treeView, ruleNode, ac);
		}	
		
		/* create NAC tree nodes */
		for (int j = 0; j < rule.getNACs().size(); j++) {
			final EdNAC nac = rule.getNACs().elementAt(j);
			insertNACIntoTree(treeView.getTreeModel(), ruleNode, nac);
		}
		/* create PAC tree nodes */
		for (int j = 0; j < rule.getPACs().size(); j++) {
			final EdPAC pac = rule.getPACs().elementAt(j);
			insertPACIntoTree(treeView.getTreeModel(), ruleNode, pac);
		}
		
		/* create attr condition tree nodes */
		CondTuple conds = (CondTuple) rule.getBasisRule().getAttrContext()
				.getConditions();
		for (int c = 0; c < conds.getSize(); c++) {
			final CondMember cond = (CondMember) conds.getMemberAt(c);
			insertRuleAttrConditionIntoTree(treeView.getTreeModel(), ruleNode, cond);
		}
		
		/* create post application conditions */
		insertRuleConstraintsIntoTree(treeView.getTreeModel(), ruleNode);
	}
	
	private void insertRuleSchemeIntoTree(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode graNode,
			final EdRuleScheme ruleScheme) {
		
		RuleSchemeTreeNodeData sd = new RuleSchemeTreeNodeData(ruleScheme);
		final DefaultMutableTreeNode ruleSchemeNode = new DefaultMutableTreeNode(sd);
		sd.setTreeNode(ruleSchemeNode);
		treeView.getTreeModel().insertNodeInto(ruleSchemeNode, graNode, graNode.getChildCount());
		
		// make kernel rule tree node of rule scheme
		insertSchemeRuleIntoTree(treeView, ruleSchemeNode, ruleScheme.getKernelRule(), this.KERNEL);
       
		// make multi rule tree node of rule scheme
		for (int i=0; i<ruleScheme.getMultiRules().size(); i++) {
			EdRule multiRule = ruleScheme.getMultiRules().get(i);
			insertSchemeRuleIntoTree(treeView, ruleSchemeNode, multiRule, this.MULTI);
		}
		
		// make amalgamated rule tree node of rule scheme
		if (ruleScheme.getAmalgamatedRule() != null) {
			insertSchemeRuleIntoTree(treeView, ruleSchemeNode, 
					ruleScheme.getAmalgamatedRule(), this.AMALGAMATED);
		}
	}
	
	private void insertSchemeRuleIntoTree(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode ruleSchemeNode,
			final EdRule schemeRule,
			int kind) {
		
		GraGraTreeNodeData sdRule = null;
		if (kind == this.KERNEL) 
			sdRule = new KernelRuleTreeNodeData(schemeRule);
		else if (kind == this.MULTI) 
			sdRule = new MultiRuleTreeNodeData(schemeRule);
		else if (kind == this.AMALGAMATED) 
			sdRule = new AmalgamatedRuleTreeNodeData(schemeRule);
		
		if (sdRule != null) {
			final DefaultMutableTreeNode ruleNode = new DefaultMutableTreeNode(sdRule);
	        sdRule.setTreeNode(ruleNode);
	        treeView.getTreeModel().insertNodeInto(ruleNode, ruleSchemeNode, ruleSchemeNode.getChildCount());
	        
	        /* insert formula above nested ACs into rule tree*/
	        insertRuleFormulaIntoTree(treeView, ruleNode, schemeRule.getBasisRule().getFormulaStr());
	        /* create nested AC tree nodes */
			for (int j = 0; j < schemeRule.getNestedACs().size(); j++) {
				final EdNestedApplCond ac = (EdNestedApplCond) schemeRule.getNestedACs().get(j);
				insertNestedACIntoTree(treeView, ruleNode, ac);
			}
	        
			/* create NAC tree nodes */
			for (int j = 0; j < schemeRule.getNACs().size(); j++) {
				final EdNAC nac = schemeRule.getNACs().get(j);
				insertNACIntoTree(treeView.getTreeModel(),ruleNode, nac);
			}
			/* create PAC tree nodes */
			for (int j = 0; j < schemeRule.getPACs().size(); j++) {
				final EdPAC pac = schemeRule.getPACs().get(j);
				insertPACIntoTree(treeView.getTreeModel(), ruleNode, pac);
			}
			
			/* create attr condition tree nodes */
			final CondTuple conds = (CondTuple) schemeRule.getBasisRule().getAttrContext()
					.getConditions();
			for (int c = 0; c < conds.getSize(); c++) {
				final CondMember cond = (CondMember) conds.getMemberAt(c);
				insertRuleAttrConditionIntoTree(treeView.getTreeModel(), ruleNode, cond);	
			}
		}
	}
	
	private void insertNestedACIntoTree(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode parNode,
			final EdNestedApplCond ac) {
		
		NestedACTreeNodeData acsd = new NestedACTreeNodeData(ac);
		final DefaultMutableTreeNode acNode = new DefaultMutableTreeNode(acsd);
		acsd.setTreeNode(acNode);
		treeView.getTreeModel().insertNodeInto(acNode, parNode, parNode.getChildCount());
		
		for (int i=0; i<ac.getNestedACs().size(); i++) {
			EdNestedApplCond acCh = ac.getNestedACs().get(i);
			insertNestedACIntoTree(treeView, acNode, acCh);
		}
		insertNestedACFormulaIntoTree(treeView, acNode, ac.getNestedMorphism().getFormulaText());
	}
	
	private void insertNACIntoTree(
			final GraGraTreeModel treeModel,
			final DefaultMutableTreeNode ruleNode,
			final EdNAC nac) {
		
		NACTreeNodeData nacsd = new NACTreeNodeData(nac);
		DefaultMutableTreeNode nacNode = new DefaultMutableTreeNode(nacsd);
		nacsd.setTreeNode(nacNode);
		treeModel.insertNodeInto(nacNode, ruleNode, ruleNode.getChildCount());
	}
	
	private void insertPACIntoTree(
			final GraGraTreeModel treeModel,
			final DefaultMutableTreeNode ruleNode,
			final EdPAC pac) {
		
		PACTreeNodeData pacsd = new PACTreeNodeData(pac);
		DefaultMutableTreeNode pacNode = new DefaultMutableTreeNode(pacsd);
		pacsd.setTreeNode(pacNode);
		treeModel.insertNodeInto(pacNode, ruleNode, ruleNode.getChildCount());
	}
	
	private void insertRuleAttrConditionIntoTree(
			final GraGraTreeModel treeModel,
			final DefaultMutableTreeNode ruleNode,
			final CondMember condition) {
		
		EdRule rule = null;
		if (ruleNode.getUserObject() instanceof RuleTreeNodeData)
			rule = ((RuleTreeNodeData) ruleNode.getUserObject()).getRule();
		else if (ruleNode.getUserObject() instanceof KernelRuleTreeNodeData)
			rule = ((KernelRuleTreeNodeData) ruleNode.getUserObject()).getRule();
		else if (ruleNode.getUserObject() instanceof MultiRuleTreeNodeData)
			rule = ((MultiRuleTreeNodeData) ruleNode.getUserObject()).getRule();
		
		if (rule != null) {
			final String condStr = condition.getExprAsText();
			RuleAttrCondTreeNodeData conddata = new RuleAttrCondTreeNodeData(condition, rule);
			conddata.setString(condStr);
			DefaultMutableTreeNode condchild = new DefaultMutableTreeNode(conddata);
			conddata.setTreeNode(condchild);
			treeModel.insertNodeInto(condchild, ruleNode, ruleNode.getChildCount());
		}
	}
	
	private void insertRuleFormulaIntoTree(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode ruleNode,
			final String formula) {
		EdRule rule = null;
		if (ruleNode.getUserObject() instanceof RuleTreeNodeData)
			rule = ((RuleTreeNodeData) ruleNode.getUserObject()).getRule();
		else if (ruleNode.getUserObject() instanceof KernelRuleTreeNodeData)
			rule = ((KernelRuleTreeNodeData) ruleNode.getUserObject()).getRule();
		else if (ruleNode.getUserObject() instanceof MultiRuleTreeNodeData)
			rule = ((MultiRuleTreeNodeData) ruleNode.getUserObject()).getRule();
		
		if (rule != null && !"true".equals(formula)) {
			List<String> allVars = rule.getBasisRule().getNameOfEnabledACs();
//			List<Integer> vars = 
			Formula.getFromStringAboveList(formula, allVars);
			
			ApplFormulaTreeNodeData conddata = new ApplFormulaTreeNodeData(formula, true, rule);
			DefaultMutableTreeNode condchild = new DefaultMutableTreeNode(conddata);
			conddata.setTreeNode(condchild);
			treeView.getTreeModel().insertNodeInto(condchild, ruleNode, 0);
		} 
	}

	private void insertNestedACFormulaIntoTree(
			final GraGraTreeView treeView,
			final DefaultMutableTreeNode acNode,
			final String formula) {
		EdNestedApplCond ac = null;
		if (acNode.getUserObject() instanceof NestedACTreeNodeData)
			ac = ((NestedACTreeNodeData) acNode.getUserObject()).getNestedAC();
		
		if (ac != null && !"true".equals(formula)) {
			List<String> allVars = ac.getNestedMorphism().getNameOfEnabledACs();
//			List<Integer> vars = 
			Formula.getFromStringAboveList(formula, allVars);
			
			ApplFormulaTreeNodeData conddata = new ApplFormulaTreeNodeData(formula, true, ac);
			DefaultMutableTreeNode condchild = new DefaultMutableTreeNode(conddata);
			conddata.setTreeNode(condchild);
			treeView.getTreeModel().insertNodeInto(condchild, acNode, 0);		
		} 
	}
	
	private void insertRuleConstraintsIntoTree(
			final GraGraTreeModel treeModel,
			final DefaultMutableTreeNode rnode) {
		// System.out.println("GraGraTreeNode.insertRuleConstraintsIntoTree ");
		
		GraGraTreeNodeData sd = (GraGraTreeNodeData) rnode.getUserObject();
		if (!sd.isRule()) {
			return;
		}
		EdRule er = sd.getRule();
		// tree.collapsePath(path);
		Vector<EvalSet> atoms = er.getBasisRule().getAtomApplConds();
		// System.out.println("Atomics: "+atoms.size());
		Vector<String> names = er.getBasisRule().getConstraintNames();
		String name;
		for (int i = 0; i < atoms.size(); i++) {
			EvalSet es = atoms.get(i);
			// System.out.println("s = R x P : "+es.getSet().size());
			name = "PAC_" + names.get(i);
			EdRuleConstraint rc = new EdRuleConstraint(name, er, es);
			RuleApplConstraintTreeNodeData subsd = new RuleApplConstraintTreeNodeData(rc);
			subsd.setString(name);
			DefaultMutableTreeNode child = new DefaultMutableTreeNode(subsd);
			subsd.setTreeNode(child);
			treeModel.insertNodeInto(child, rnode, rnode.getChildCount());
			// System.out.println("Conclusions: "+es.getSet().size());
			for (int j = 0; j < es.getSet().size(); j++) {
				// AtomApplCond cond = (AtomApplCond) es.getSet().get(j);
				Vector<?> set = ((EvalSet) es.getSet().get(j)).getSet();
				// System.out.println("Conclusions: "+set.size());
				for (int k = 0; k < set.size(); k++) {
					AtomApplCond cond = (AtomApplCond) set.elementAt(k);
					String condName = cond.getSourceAtomConstraint().getName();
					String n = (k + (j * set.size()) + 1) + "_" + condName;
					EdAtomApplCond aac = new EdAtomApplCond(n, er, cond);
					RuleAtomicApplConstraintTreeNodeData conddata = new RuleAtomicApplConstraintTreeNodeData(aac);
					conddata.setString(n);
					DefaultMutableTreeNode condchild = new DefaultMutableTreeNode(conddata);
					conddata.setTreeNode(condchild);
					treeModel.insertNodeInto(condchild, child, child.getChildCount());
					for (int l = 0; l < cond.getEquivalents().size(); l++) {
						AtomApplCond eq = cond.getEquivalents().elementAt(l);
//						String eqName = eq.getSourceAtomConstraint().getName();
						String eqn = n + "_Eq" + l;
						EdAtomApplCond aacEq = new EdAtomApplCond(eqn, er, eq);
						RuleAtomicApplConstraintTreeNodeData eqdata = new RuleAtomicApplConstraintTreeNodeData(
								aacEq);
						eqdata.setString(eqn);
						DefaultMutableTreeNode eqchild = new DefaultMutableTreeNode(eqdata);
						eqdata.setTreeNode(eqchild);
						treeModel.insertNodeInto(eqchild, child, child.getChildCount());
					}
				}
			}
		}
		// tree.expandPath(path);
	}
	
	private void insertAtomicGraphConstraintIntoTree(
			final GraGraTreeModel treeModel,
			final DefaultMutableTreeNode graNode,
			final EdAtomic atomicGC) {
		
		AtomicGraphConstraintTreeNodeData sd = new AtomicGraphConstraintTreeNodeData(atomicGC);
		final DefaultMutableTreeNode node = new DefaultMutableTreeNode(sd);
		sd.setTreeNode(node);
		treeModel.insertNodeInto(node, graNode, graNode.getChildCount());
		for (int j = 0; j < atomicGC.getConclusions().size(); j++) {
			final EdAtomic concl = atomicGC.getConclusions().elementAt(j);
			ConclusionTreeNodeData conclsd = new ConclusionTreeNodeData(concl);
			final DefaultMutableTreeNode conclNode = new DefaultMutableTreeNode(conclsd);
			conclsd.setTreeNode(conclNode);
			treeModel.insertNodeInto(conclNode, node, node.getChildCount());
			
			final CondTuple conds = (CondTuple) concl.getBasisAtomic().getAttrContext().getConditions();
			for (int c = 0; c < conds.getSize(); c++) {
				final CondMember cond = (CondMember) conds.getMemberAt(c);
				insertAttrConditionOfAtomicGC(treeModel, conclNode, cond);			
			}
		}
	}
	
	private void insertAttrConditionOfAtomicGC(
			final GraGraTreeModel treeModel,
			final DefaultMutableTreeNode conclusionNode,
			final CondMember condition) {
		
		EdAtomic conclusion = ((ConclusionTreeNodeData) conclusionNode.getUserObject()).getConclusion();
		String condStr = condition.getExprAsText();
		ConclusionAttrConditionTreeNodeData 
		conddata = new ConclusionAttrConditionTreeNodeData(condition,conclusion);
		conddata.setString(condStr);
		DefaultMutableTreeNode condchild = new DefaultMutableTreeNode(conddata);
		conddata.setTreeNode(condchild);
		treeModel.insertNodeInto(condchild, conclusionNode, conclusionNode.getChildCount());
	}
	
	private void insertFormulaIntoTree(
			final GraGraTreeModel treeModel,
			final DefaultMutableTreeNode graNode,
			final EdConstraint formula) {
		
		ConstraintTreeNodeData sd = new ConstraintTreeNodeData(formula);
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(sd);
		sd.setTreeNode(node);
		treeModel.insertNodeInto(node, graNode, graNode.getChildCount());
	}
	
	private void insertRuleSequenceIntoTree(
			final GraGraTreeModel treeModel,
			final DefaultMutableTreeNode graNode,
			final RuleSequence ruleSequence) {
		
		RuleSequenceTreeNodeData sd = new RuleSequenceTreeNodeData(ruleSequence);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(sd);
        sd.setTreeNode(node);
        treeModel.insertNodeInto(node, graNode, graNode.getChildCount());
	}

	public static void expand(final GraGraTreeView treeView, final TreeNode treenode, final TreePath treepath) {
		treeView.getTree().expandPath(treepath);
		int row = treeView.getTree().getRowForPath(treepath);
		expandpath(treeView, treenode, row);
		treeView.getTree().treeDidChange();
	}
	
	static void expandpath(final GraGraTreeView treeView, final TreeNode treenode, int row) {
		for (int i=0; i<treenode.getChildCount(); i++) {
			TreeNode child = treenode.getChildAt(i);
			if (child.getChildCount() > 0) {
				row++;
				treeView.getTree().expandRow(row);
				expandpath(treeView, child, row);
			}			
		}	
	}
	
}
