/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
//$Id: GraGraTreeModel.java,v 1.9 2010/08/25 00:34:40 olga Exp $

package agg.gui.treeview;

import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdNAC;
import agg.editor.impl.EdNestedApplCond;
import agg.editor.impl.EdPAC;
import agg.editor.impl.EdRule;
import agg.editor.impl.EdRuleScheme;
import agg.gui.treeview.nodedata.GraGraTreeNodeData;

/**
 * The class GraGraTreeModel extends JTreeModel to define valueForPathChanged.
 * This method is called as a result of the user editing a value in the tree. If
 * you allow editing in your tree, are using TreeNodes and the user object of
 * the TreeNodes is of the class GraGraTreeNodeData.
 * 
 * @author $Author: olga $
 * @version $Id: GraGraTreeModel.java,v 1.9 2010/08/25 00:34:40 olga Exp $
 */
@SuppressWarnings("serial")
public class GraGraTreeModel extends DefaultTreeModel {

	/**
	 * Creates a new instance of GraGraTreeModel with root specified by the
	 * TreeNode newRoot.
	 */
	public GraGraTreeModel(TreeNode newRoot) {
		super(newRoot);
	}

	public GraGraTreeModel(JFrame frame, TreeNode newRoot) {
		super(newRoot);
		this.applFrame = frame;
	}

	/**
	 * Calls the super class method setString() if the path item would be
	 * changed.
	 */
	public void valueForPathChanged(TreePath path, Object newValue) {
		/* Update the user object. */
		DefaultMutableTreeNode aNode = (DefaultMutableTreeNode) path
				.getLastPathComponent();
		GraGraTreeNodeData data = (GraGraTreeNodeData) aNode.getUserObject();
		// String oldValue = data.toString();
		if (checkNewValue(aNode, data, (String) newValue)) {
			data.setString((String) newValue);
			/*
			 * Since we've changed how the data is to be displayed, message
			 * nodeChanged.
			 */
			nodeChanged(aNode);
		}
	}

	private boolean checkNewValue(DefaultMutableTreeNode aNode,
			GraGraTreeNodeData sd, String name) {
		if (sd.isGraGra()) {
			if (!isValid(getGraGraNames(), name, sd.toString())) {
				warning(this.applFrame, name);
				return false;
			} 
			return true;
		} else if (sd.isGraph()) {
			EdGraGra egra = getGraGra(aNode);
			if (!isValid(getGraGraChildrenNames(egra), name, sd.toString())) {
				warning(this.applFrame, name);
				return false;
			} 
			return true;
		} 
		else if (sd.isRuleScheme()) {
			EdGraGra egra = getGraGra(aNode);
			if (!isValid(getGraGraChildrenNames(egra), name, sd.toString())) {
				warning(this.applFrame, name);
				return false;
			} 
			return true;
		}
		else if (sd.isRule()) {
			if (sd.isKernelRule() || sd.isMultiRule() || sd.isAmalgamatedRule()) {
				EdRuleScheme ruleScheme = getRuleScheme(aNode);
				if (!isValid(getRuleSchemeChildrenNames(ruleScheme), name, sd.toString())) {
					warning(this.applFrame, name);
					return false;
				} 
				return true;
			} 
			
			EdGraGra egra = getGraGra(aNode);
			if (!isValid(getGraGraChildrenNames(egra), name, sd.toString())) {
				warning(this.applFrame, name);
				return false;
			} 
			return true;
			
		} else if (sd.isNAC()) {
			EdRule erule = getRule(aNode);
			if (!isValid(getRuleChildrenNames(erule), name, sd.toString())) {
				warning(this.applFrame, name);
				return false;
			} 
			return true;
		} else if (sd.isPAC()) {
			EdRule erule = getRule(aNode);
			if (!isValid(getRuleChildrenNames(erule), name, sd.toString())) {
				warning(this.applFrame, name);
				return false;
			} 
			return true;
		} else if (sd.isNestedAC()) {
			EdRule erule = getRule(aNode);
			if (erule == null) {
				EdNestedApplCond ac = getParentCond(aNode);
				if (!isValid(this.getNestedChildrenNames(ac), name, sd.toString())) {
					warning(this.applFrame, name);
					return false;
				}
			}
			else {
				if (!isValid(getRuleChildrenNames(erule), name, sd.toString())) {
					warning(this.applFrame, name);
					return false;
				} 
			}
			return true;
		} else
			return true;
	}

	public DefaultMutableTreeNode getTreeNodeOfGraGraRule(
			DefaultMutableTreeNode gragraNode, Object searchedObj) {
		if (!(searchedObj instanceof EdRule))
			return null;
		
		for (int j = 0; j < gragraNode.getChildCount(); j++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) gragraNode
					.getChildAt(j); // graph|rule
			GraGraTreeNodeData sd = (GraGraTreeNodeData) node.getUserObject();
//			if (sd.isTypeGraph() && (searchedObj instanceof EdGraph)
//					&& sd.getGraph().equals(searchedObj)) {
//				return node;
//			} else if (sd.isGraph() && (searchedObj instanceof EdGraph)
//					&& sd.getGraph().equals(searchedObj)) {
//				return node;
//			} else 
			
			if (sd.isRule()) {
				if (sd.isRuleScheme() || sd.isKernelRule()
						|| sd.isMultiRule() || sd.isAmalgamatedRule()) {
					return null;
				}
				else 
					if ((searchedObj instanceof EdRule)
						&& sd.getRule().equals(searchedObj)) {
					return node;
				}
			} 
			
//			else if (sd.isAtomic() && (searchedObj instanceof EdAtomic)
//					&& sd.getAtomic().equals(searchedObj)) {
//				return node;
//			} else if (sd.isConstraint() && (searchedObj instanceof EdConstraint)
//					&& sd.getConstraint().equals(searchedObj)) {
//				return node;
//			}
		}
		return null;
	}

	public boolean isValid(Vector<String> names, String newName, String oldName) {
		String testold = oldName;
		String testnew = newName;
		int ind = testold.lastIndexOf(']');	
		testold = testold.substring(ind+1);
		ind = testnew.lastIndexOf(']');
		testnew = testnew.substring(ind+1);
		
		for (int i = 0; i < names.size(); i++) {
			if (!names.elementAt(i).equals(testold)) {
				if (names.elementAt(i).equals(testnew))
					return false;
			}
		}
		return true;
	}

	public String makeNewName(EdGraGra gragra, String name) {
		String result = name;
		if (!this.isValid(this.getGraGraChildrenNames(gragra), name, ""))
			result = name.concat("_");
		return result;
	}
	
	public String makeNewName(EdRule rule, String name) {
		String result = name;
		if (!this.isValid(this.getRuleChildrenNames(rule), name, ""))
			result = name.concat("_");
		return result;
	}
	
	public String makeNewName(EdNestedApplCond ac, String name) {
		String result = name;
		if (!this.isValid(this.getNestedChildrenNames(ac), name, ""))
			result = name.concat("_");
		return result;
	}
	
	public void ruleNameChanged(EdGraGra gragra, boolean layered) {
		for (int i = 0; i < this.root.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.root
					.getChildAt(i); // GraGra
			GraGraTreeNodeData sdGra = (GraGraTreeNodeData) node
					.getUserObject();
			if (sdGra.getGraGra().equals(gragra)) {
				for (int j = 0; j < node.getChildCount(); j++) {
					DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) node
							.getChildAt(j); // graph|rule name
					GraGraTreeNodeData sd = (GraGraTreeNodeData) node1
							.getUserObject();
					if (sd.isRule()) {
						String tag = "";
						String tag1 = "";
						if (!sd.getRule().getBasisRule().isEnabled())
							tag = "[D]";
						if (layered)
							tag1 = "[" + sd.getRule().getBasisRule().getLayer()
									+ "]";
						sd.setString(tag, tag1, sd.getRule().getBasisRule()
								.getName());
						nodeChanged(node1);
					}
				}
				break;
			}
		}
	}

	public void ruleNameChanged(EdGraGra gragra, boolean layered, boolean priority) {
		for (int i = 0; i < this.root.getChildCount(); i++) {
			// child is EdGraGra
			DefaultMutableTreeNode 
			node = (DefaultMutableTreeNode) this.root.getChildAt(i);
			GraGraTreeNodeData sdGra = (GraGraTreeNodeData) node
					.getUserObject();
			if (sdGra.getGraGra().equals(gragra)) {
				for (int j = 0; j < node.getChildCount(); j++) {
					DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) node
							.getChildAt(j); // graph|rule name
					GraGraTreeNodeData sd = (GraGraTreeNodeData) node1
							.getUserObject();
					if (sd.isRule() && sd.getRule() != null) {
						String tag = "";
						String tag1 = "";
						if (!sd.getRule().getBasisRule().isEnabled())
							tag = "[D]";
						if (layered)
							tag1 = "[" + sd.getRule().getBasisRule().getLayer()
									+ "]";
						else if (priority)
							tag1 = "["
									+ sd.getRule().getBasisRule().getPriority()
									+ "]";
						sd.setString(tag, tag1, sd.getRule().getBasisRule()
								.getName());
						nodeChanged(node1);
					}
				}
				break;
			}
		}
	}

	public void ruleNameChanged(DefaultMutableTreeNode ruleNode, boolean layered) {
		GraGraTreeNodeData sd = (GraGraTreeNodeData) ruleNode.getUserObject();
		if (sd.isRule()) {
			String tag = "";
			String tag1 = "";
			if (!sd.getRule().getBasisRule().isEnabled())
				tag = "[D]";
			if (layered)
				tag1 = "[" + sd.getRule().getBasisRule().getLayer() + "]";
			sd.setString(tag, tag1, sd.getRule().getBasisRule().getName());
			nodeChanged(ruleNode);
		}
	}

	public void ruleNameChanged(DefaultMutableTreeNode ruleNode,
			boolean layered, boolean priority) {
		GraGraTreeNodeData sd = (GraGraTreeNodeData) ruleNode.getUserObject();
		if (sd.isRule()) {
			String tag = "";
			String tag1 = "";
			if (!sd.getRule().getBasisRule().isEnabled())
				tag = "[D]";
			if (layered)
				tag1 = "[" + sd.getRule().getBasisRule().getLayer() + "]";
			else if (priority)
				tag1 = "[" + sd.getRule().getBasisRule().getPriority() + "]";
			sd.setString(tag, tag1, sd.getRule().getBasisRule().getName());
			nodeChanged(ruleNode);
		}
	}

	public void constraintNameChanged(EdGraGra gragra, boolean layered) {
		for (int i = 0; i < this.root.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.root
					.getChildAt(i); // GraGra
			GraGraTreeNodeData sdGra = (GraGraTreeNodeData) node
					.getUserObject();
			if (sdGra.getGraGra().equals(gragra)) {
				for (int j = 0; j < node.getChildCount(); j++) {
					DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) node
							.getChildAt(j); 
					GraGraTreeNodeData sd = (GraGraTreeNodeData) node1
							.getUserObject();
					if (sd.isConstraint()) {// constraint name
						String tagD = "";
						if (!sd.getConstraint().getBasisConstraint()
								.isEnabled())
							tagD = "[D]";
						String tag = "";
						Vector<Integer> layer = sd.getConstraint().getBasisConstraint()
								.getLayer();
						if (layered && !layer.isEmpty()) {
							tag = "["
									+ sd.getConstraint().getBasisConstraint()
											.getLayerAsString() + "]";
						}
						sd.setString(tagD, tag, sd.getConstraint()
								.getBasisConstraint().getName());
						nodeChanged(node1);
					}
				}
				break;
			}
		}
	}

	public void constraintNameChanged(EdGraGra gragra, boolean layered,
			boolean priority) {
		for (int i = 0; i < this.root.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.root
					.getChildAt(i); // GraGra
			GraGraTreeNodeData sdGra = (GraGraTreeNodeData) node
					.getUserObject();
			if (sdGra.getGraGra().equals(gragra)) {
				for (int j = 0; j < node.getChildCount(); j++) {
					DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) node
							.getChildAt(j); 
					GraGraTreeNodeData sd = (GraGraTreeNodeData) node1
							.getUserObject();
					if (sd.isConstraint()) {
						String tagD = "";
						if (!sd.getConstraint().getBasisConstraint()
								.isEnabled())
							tagD = "[D]";
						String tag = "";
						Vector<Integer> layer = sd.getConstraint().getBasisConstraint()
								.getLayer();
						Vector<Integer> prior = sd.getConstraint().getBasisConstraint()
								.getPriority();
						if (layered && !layer.isEmpty()) {
							tag = "["
									+ sd.getConstraint().getBasisConstraint()
											.getLayerAsString() + "]";
						} else if (priority && !prior.isEmpty()) {
							tag = "["
									+ sd.getConstraint().getBasisConstraint()
											.getPriorityAsString() + "]";
						}
						sd.setString(tagD, tag, sd.getConstraint()
								.getBasisConstraint().getName());
						nodeChanged(node1);
					}
				}
				break;
			}
		}
	}

	public void constraintNameChanged(DefaultMutableTreeNode node,
			boolean layered) {
		GraGraTreeNodeData sd = (GraGraTreeNodeData) node.getUserObject();
		if (sd.isConstraint()) {
			String tagD = "";
			if (!sd.getConstraint().getBasisConstraint().isEnabled())
				tagD = "[D]";
			String tag = "";
			Vector<Integer> layer = sd.getConstraint().getBasisConstraint().getLayer();
			if (layered && !layer.isEmpty()) {
				tag = "["
						+ sd.getConstraint().getBasisConstraint()
								.getLayerAsString() + "]";
			}
			sd.setString(tagD, tag, sd.getConstraint().getBasisConstraint()
					.getName());
			nodeChanged(node);
		}
	}

	public void constraintNameChanged(DefaultMutableTreeNode node,
			boolean layered, boolean priority) {
		GraGraTreeNodeData sd = (GraGraTreeNodeData) node.getUserObject();
		if (sd.isConstraint()) {
			String tagD = "";
			if (!sd.getConstraint().getBasisConstraint().isEnabled())
				tagD = "[D]";
			String tag = "";
			Vector<Integer> layer = sd.getConstraint().getBasisConstraint().getLayer();
			Vector<Integer> prior = sd.getConstraint().getBasisConstraint()
					.getPriority();
			if (layered && !layer.isEmpty()) {
				tag = "["
						+ sd.getConstraint().getBasisConstraint()
								.getLayerAsString() + "]";
			} else if (priority && !prior.isEmpty()) {
				tag = "["
						+ sd.getConstraint().getBasisConstraint()
								.getPriorityAsString() + "]";
			}
			sd.setString(tagD, tag, sd.getConstraint().getBasisConstraint()
					.getName());
			nodeChanged(node);
		}
	}

	private void warning(JFrame fr, String str) {
		String test = str;
		int ind = test.indexOf("]");
		while (ind > 0) {
			test = test.substring(ind+1);
			ind = test.indexOf("]");
		}
		JOptionPane.showMessageDialog(fr, "Name  \"" + test + "\"  exists.",
				"Warning", JOptionPane.ERROR_MESSAGE);
	}

	public Vector<String> getGraGraNames() {
		Vector<String> gragraNames = new Vector<String>();
		for (int i = 0; i < this.root.getChildCount(); i++) {
			gragraNames
					.addElement(((DefaultMutableTreeNode) this.root.getChildAt(i))
							.toString());
		}
		return gragraNames;
	}

	public Vector<String> getGraGraChildrenNames(EdGraGra eGra) {
		Vector<String> childrenNames = new Vector<String>();
		for (int i = 0; i < eGra.getGraphs().size(); i++) {
			childrenNames.add(eGra.getGraphs().get(i)
					.getBasisGraph().getName());
		}
		
		for (int i = 0; i < eGra.getRules().size(); i++) {
			if (eGra.getRules().elementAt(i) != null) {
				EdRule er = eGra.getRules().elementAt(i);
				childrenNames.add(er.getBasisRule().getName());
				
				for (int j = 0; j < er.getNACs().size(); j++) {
					EdNAC enac = er.getNACs().elementAt(j);
					childrenNames.add(enac.getBasisGraph()
							.getName());
				}
				for (int j = 0; j < er.getPACs().size(); j++) {
					EdPAC epac = er.getPACs().elementAt(j);
					childrenNames.add(epac.getBasisGraph()
							.getName());
				}
				for (int j = 0; j < er.getNestedACs().size(); j++) {
					EdPAC epac = er.getNestedACs().elementAt(j);
					childrenNames.add(epac.getBasisGraph()
							.getName());
				}
			}

		}
		return childrenNames;
	}

	private Vector<String> getRuleChildrenNames(EdRule eRule) {
		Vector<String> childrenNames = new Vector<String>();
//		childrenNames.add(eRule.getName());
		for (int i = 0; i < eRule.getNACs().size(); i++) {
			childrenNames.add(eRule.getNACs().elementAt(i)
					.getBasisGraph().getName());
		}
		for (int i = 0; i < eRule.getPACs().size(); i++) {
			childrenNames.add(eRule.getPACs().elementAt(i)
					.getBasisGraph().getName());
		}
		for (int i = 0; i < eRule.getNestedACs().size(); i++) {
			childrenNames.add(eRule.getNestedACs().elementAt(i)
					.getBasisGraph().getName());
		}
		return childrenNames;
	}

	private Vector<String> getNestedChildrenNames(EdNestedApplCond ac) {
		Vector<String> childrenNames = new Vector<String>();
		for (int i = 0; i < ac.getNestedACs().size(); i++) {
			childrenNames.add(ac.getNestedACs().get(i)
					.getBasisGraph().getName());
		}
		return childrenNames;
	}
	
	private Vector<String> getRuleSchemeChildrenNames(EdRuleScheme eRuleScheme) {
		Vector<String> ruleSchemeChildrenNames = new Vector<String>();
		ruleSchemeChildrenNames.add(eRuleScheme.getBasisRuleScheme().getSchemeName());
		ruleSchemeChildrenNames.add(eRuleScheme.getBasisRuleScheme().getKernelRule().getName());
		for (int i = 0; i < eRuleScheme.getMultiRules().size(); i++) {
			ruleSchemeChildrenNames.add(eRuleScheme.getMultiRules().get(i)
					.getBasisRule().getName());
		}
		if (eRuleScheme.getAmalgamatedRule() != null)
			ruleSchemeChildrenNames.add(eRuleScheme.getAmalgamatedRule().getName());
		return ruleSchemeChildrenNames;
	}
	
	/*
	private Vector<String> getAllNames() {
		Vector<String> allNames = new Vector<String>();
		allNames.addElement(((DefaultMutableTreeNode) root).toString());
		for (int i = 0; i < root.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) root
					.getChildAt(i);
			allNames.add(node.toString()); // GraGra name
			for (int j = 0; j < node.getChildCount(); j++) {
				DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) node
						.getChildAt(j);
				allNames.addElement(node1.toString()); // graph / rule name
				if (!node1.isLeaf()) {
					for (int k = 0; j < node1.getChildCount(); k++) {
						DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) node1
								.getChildAt(k);
						allNames.addElement(node2.toString()); // NAC / PAC
						// name
					}
				}
			}
		}
		// System.out.println(">>> allNames.size = "+ allNames.size());
		return allNames;
	}
*/
	
	private EdGraGra getGraGra(DefaultMutableTreeNode aNode) {
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) aNode
				.getParent();
		GraGraTreeNodeData data = (GraGraTreeNodeData) parent.getUserObject();
		// System.out.println(data.toString());
		if (data.isGraGra())
			return data.getGraGra();
		
		return null;
	}

	private EdRule getRule(DefaultMutableTreeNode aNode) {
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) aNode
				.getParent();
		GraGraTreeNodeData data = (GraGraTreeNodeData) parent.getUserObject();
		if (data.isRule())
			return data.getRule();
		
		return null;
	}

	private EdRuleScheme getRuleScheme(DefaultMutableTreeNode aNode) {
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) aNode
				.getParent();
		GraGraTreeNodeData data = (GraGraTreeNodeData) parent.getUserObject();
		if (data.isRuleScheme())
			return data.getRuleScheme();
		
		return null;
	}
	
	private EdNestedApplCond getParentCond(DefaultMutableTreeNode aNode) {
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) aNode
				.getParent();
		GraGraTreeNodeData data = (GraGraTreeNodeData) parent.getUserObject();
		if (data.isNestedAC())
			return data.getNestedAC();
		
		return null;
	}
	
	private JFrame applFrame;

}
