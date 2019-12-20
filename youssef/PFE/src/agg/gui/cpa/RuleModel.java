/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.cpa;

//import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import agg.cons.AtomConstraint;
import agg.xt_basis.GraGra;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;

/**
 * This class models a set of rules as a tree. This tree is used to display all
 * rules of a selected graph grammar.
 * 
 * @version $Id: RuleModel.java,v 1.4 2010/09/23 08:18:50 olga Exp $
 * @author $Author: olga $
 */
public class RuleModel implements TreeModel {

	@SuppressWarnings("serial")
	public class TreeData extends DefaultMutableTreeNode {

		boolean atomic;

		boolean rule;

		boolean nac;

		boolean root;

		TreeData(Object o) {
			super(o);
			// initialisieren
			this.rule = false;
			this.nac = false;
			this.root = false;

			if (o instanceof String)
				this.root = true;
			else if (o instanceof AtomConstraint)
				this.atomic = true;
			else if (o instanceof Rule)
				this.rule = true;
			else if (o instanceof OrdinaryMorphism)
				this.nac = true;
		}

		public OrdinaryMorphism getData() {
			Object tmpObj = getUserObject();
			if (isRule() || isNAC() || isAtomic())
				return (OrdinaryMorphism) tmpObj;
			
			return null;
		}

		public String toString() {
			Object tmpObj = getUserObject();
			if (isRule() || isNAC() || isAtomic())
				return ((OrdinaryMorphism) tmpObj).getName();
			
			return tmpObj.toString();
		}

		public boolean isAtomic() {
			return this.atomic;
		}

		public boolean isNAC() {
			return this.nac;
		}

		public boolean isRule() {
			return this.rule;
		}

		public boolean isRoot() {
			return this.root;
		}
	}

	private Vector<TreeModelListener> treeModelListeners;

	private TreeData rootNode;

	private GraGra grammar;

//	private boolean showAtomics;

	private boolean withNACs;

	/**
	 * Creates a new model for a set of rule. These rules are given by a graph
	 * grammar.
	 * 
	 * @param gragra
	 *            The grammar provides the set of rules.
	 */
	public RuleModel(GraGra gragra, boolean atomics, boolean nacs) {
		this.treeModelListeners = new Vector<TreeModelListener>();
		if (gragra != null)
			this.rootNode = new TreeData((atomics ? "Atomics of" : "Rules of ")
					+ gragra.getName());
		else
			this.rootNode = new TreeData("--EMPTY--");
		this.grammar = gragra;
//		showAtomics = atomics;
		this.withNACs = nacs;
	}

	/*
	private Vector<TreeData> getAtomics() {
		Vector<TreeData> tmpVector = new Vector<TreeData>();
		if (grammar != null) {
			Enumeration<?> en = grammar.getAtomics();
			while (en.hasMoreElements()) {
				tmpVector.addElement(new TreeData(en.nextElement()));
			}
		}
		return tmpVector;
	}
*/
	
	private Vector<TreeData> getRules() {
		Vector<TreeData> tmpVector = new Vector<TreeData>();
		if (this.grammar != null) {
			Iterator<?> en = this.grammar.getListOfRules().iterator();
			while (en.hasNext()) {
				tmpVector.add(new TreeData(en.next()));
			}
		}
		return tmpVector;
	}

	private Vector<TreeData> getNACs(Rule r) {
		Vector<TreeData> tmpVector = new Vector<TreeData>();
		final List<OrdinaryMorphism> nacs = r.getNACsList();
		for (int l=0; l<nacs.size(); l++) {		
			tmpVector.addElement(new TreeData(nacs.get(l)));
		}
		return tmpVector;
	}

	// ////////////// TreeModel interface implementation ///////////////////////

	/**
	 * Adds a listener for the TreeModelEvent posted after the tree changes.
	 * 
	 * @param l
	 *            The listen will be registered.
	 */
	public void addTreeModelListener(TreeModelListener l) {
		if (!this.treeModelListeners.contains(l))
			this.treeModelListeners.addElement(l);
	}

	/**
	 * Returns the child of parent at index index in the parent's child array.
	 * 
	 * @param parent
	 *            The parent of a leave in the tree.
	 * @param index
	 *            The index of the child.
	 * @return The child
	 */
	public Object getChild(Object parent, int index) {
		if (parent == getRoot()) {
			return getRules().elementAt(index);
		} 
		TreeData td = (TreeData) parent;
		OrdinaryMorphism morph = td.getData();
		if (morph != null && this.withNACs && morph instanceof Rule) {
			return getNACs((Rule) morph).elementAt(index);
		}
		
		// hier muessen NACs sein. NACs haben keine Kinder
		return null;
	}

	/**
	 * Returns the number of children of parent.
	 * 
	 * @param parent
	 *            The parent of the child.
	 * @return The amount of children of the given parent.
	 */
	public int getChildCount(Object parent) {
		if (parent == getRoot())
			return getRules().size();
		
		TreeData td = (TreeData) parent;
		OrdinaryMorphism morph = td.getData();
		if (morph != null && this.withNACs && morph instanceof Rule) {
			int result = getNACs((Rule) morph).size();
			return result;
		}
		
		// NACs haben Null Kinder
		return 0;
	}

	/**
	 * Returns the index of child in parent.
	 * 
	 * @param parent
	 *            The parent of the child.
	 * @param child
	 *            The child the index is searched for.
	 * @return The index of the given child.
	 */
	public int getIndexOfChild(Object parent, Object child) {
		if (parent == getRoot())
			return getRules().indexOf(child);
		
		TreeData td = (TreeData) parent;
		OrdinaryMorphism morph = td.getData();
		if (morph != null && this.withNACs && morph instanceof Rule)
			return getNACs((Rule) morph).indexOf(child);
		
		return 0;
	}

	/**
	 * Returns the root of the tree.
	 * 
	 * @return The root of the tree.
	 */
	public Object getRoot() {
		return this.rootNode;
	}

	/**
	 * Returns true if node is a leaf.
	 * 
	 * @param node
	 *            The examined node in a tree.
	 * @return For rules true is returned.
	 */
	public boolean isLeaf(Object node) {
		if (node == getRoot())
			return false;
		
		// getChildCount liefert fuer NACs immer Null
		// bei Rules ist es abhaengig, ob NACs vorhanden sind.
		return getChildCount(node) == 0;
		
	}

	/**
	 * Removes a listener previously added with addTreeModelListener().
	 * 
	 * @param l
	 *            The listener
	 */
	public void removeTreeModelListener(TreeModelListener l) {
		this.treeModelListeners.removeElement(l);
	}

	/**
	 * Messaged when the user has altered the value for the item identified by
	 * path to newValue. Not used by this model.
	 * 
	 * @param path
	 *            The path to a node.
	 * @param newValue
	 *            The new value for a node.
	 */
	public void valueForPathChanged(TreePath path, Object newValue) {
		System.out.println("*** valueForPathChanged : " + path + " --> "
				+ newValue);
	}

}
/*
 * $Log: RuleModel.java,v $
 * Revision 1.4  2010/09/23 08:18:50  olga
 * tuning
 *
 * Revision 1.3  2010/03/08 15:41:21  olga
 * code optimizing
 *
 * Revision 1.2  2009/05/12 10:37:02  olga
 * CPA: bug fixed
 * Applicability of Rule Seq. : bug fixed
 *
 * Revision 1.1  2008/10/29 09:04:12  olga
 * new sub packages of the package agg.gui: typeeditor, editor, trafo, cpa, options, treeview, popupmenu, saveload
 *
 * Revision 1.4  2008/04/07 09:36:56  olga
 * Code tuning: refactoring + profiling
 * Extension: CPA - two new options added
 *
 * Revision 1.3  2007/11/01 09:58:18  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.2  2007/09/10 13:05:45  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.1 2005/08/25 11:56:55 enrico *** empty
 * log message ***
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.4 2004/12/20 14:53:48 olga Changes because of matching
 * optimisation.
 * 
 * Revision 1.3 2004/04/15 10:49:48 olga Kommentare
 * 
 * Revision 1.2 2003/03/05 18:24:10 komm sorted/optimized import statements
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:19 olga Imported sources
 * 
 * Revision 1.2 2001/03/08 11:02:49 olga Parser Anbindung gemacht. Stand nach
 * AGG GUI Reimplementierung. Stand nach der AGG GUI Reimplementierung.Das ist
 * Stand nach der AGG GUI Reimplementierung und Parser Anbindung.
 * 
 * Revision 1.1.2.4 2001/01/28 13:14:47 shultzke API fertig
 * 
 * Revision 1.1.2.3 2000/08/10 12:22:12 shultzke Ausserdem wird nicht mehr eine
 * neues GUIObject erzeugt, wenn zur ParserGUI umgeschaltet wird. Einige Klassen
 * wurden umbenannt. Alle Events sind in ein eigenes Eventpackage geflogen.
 * 
 * Revision 1.1.2.2 2000/07/09 17:12:39 shultzke grob die GUI eingebunden
 * 
 */
