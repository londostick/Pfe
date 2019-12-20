/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.editor.impl;

import java.io.File;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.Hashtable;
import javax.swing.undo.UndoManager;

import agg.cons.AtomConstraint;
import agg.cons.Evaluable;
import agg.cons.Formula;
import agg.ruleappl.RuleSequence;
import agg.util.Pair;
import agg.util.XMLHelper;
import agg.util.XMLObject;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.ConcurrentRule;
import agg.xt_basis.GraGra;
import agg.xt_basis.MorphCompletionStrategy;
import agg.xt_basis.ParallelRule;
import agg.xt_basis.TypeSet;
import agg.xt_basis.Type;
import agg.xt_basis.TypeError;
import agg.xt_basis.TypeException;
import agg.xt_basis.Rule;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.Arc;
import agg.xt_basis.Node;
import agg.xt_basis.agt.AmalgamatedRule;
import agg.xt_basis.agt.RuleScheme;
import agg.attribute.AttrContext;
import agg.attribute.impl.CondTuple;
import agg.attribute.impl.CondMember;
import agg.attribute.impl.VarTuple;
import agg.attribute.impl.VarMember;
import agg.layout.evolutionary.LayoutPattern;

/**
 * An EdGraGra consists of exactly one working graph and an arbitrary number of
 * graph rules. The used object is an object of the class agg.xt_basis.GraGra
 * which provides the functionality of a simple graph grammar. The working graph
 * is an object of the class agg.editor.impl.EdGraph which used object is of
 * type agg.xt_basis.Graph; the rules are objects of the class
 * agg.editor.impl.EdRule which used objects are of type agg.xt_basis.Rule. The
 * EdGraGra has a set of layout types for the typing of nodes and arcs.
 * 
 * @author $Author: olga $
 * @version $Id: EdGraGra.java,v 1.114 2010/11/04 10:57:05 olga Exp $
 */
public class EdGraGra implements XMLObject {
	
//	public final List<String> attrUsedClasses = new ArrayList<String>();

	/** My name. */
	private String name;

	/** My used object */
	private GraGra bGraGra;

	/** My set of layout types */
	private EdTypeSet typeSet;

	/** My work graph */
	private EdGraph eGraph;

	/**
	 * My start graph. The start graph will be set after saving/loading.
	 * It will be used to reset the work graph.
	 */
	transient private EdGraph startGraph;

	/** The set of my graphs */
	private Vector<EdGraph> eGraphs = new Vector<EdGraph>();

	/** The set of my rules */
	private Vector<EdRule> eRules = new Vector<EdRule>();

	/** The set of atomic graph constraints. */
	private Vector<EdAtomic> eAtomics = new Vector<EdAtomic>();

	/** The set of graph constraints (formulas over eAtomics). */
	private Vector<EdConstraint> eConstraints = new Vector<EdConstraint>();

	/** Directory name for saving */
	private String dirName = "";

	/** File name for saving */
	private String fileName = "";

	private boolean isChanged = false;

	private Hashtable<Type, Vector<LayoutPattern>> 
	layoutPatterns = new Hashtable<Type, Vector<LayoutPattern>>();

	private EditUndoManager undoManager;
	private boolean undoEnabled = true;
	
	protected boolean animated;
	
	/**
	 * Creates an empty gragra which name is specified by the String name and
	 * with an empty used object of the type agg.xt_basis.GraGra. The empty
	 * gragra has an empty working graph and no rules.
	 */
	public EdGraGra(String name) {
		this.name = name;
		
		this.dirName = ""; // System.getProperty("user.dir");
		this.fileName = ""; // name;

		// create base gragra with empty start (base) graph
		// and no rules
		this.bGraGra = BaseFactory.theFactory().createGraGra();
		this.bGraGra.setName(name);

		// create EdTypeSet
		this.typeSet = new EdTypeSet(this.bGraGra.getTypeSet());
		
		// create EdGraph of empty base graph
		this.eGraph = new EdGraph(this.bGraGra.getGraph(), this.typeSet);
		this.eGraph.setGraGra(this);
		this.eGraphs.add(this.eGraph);
		this.startGraph = null; 
		// will be set after saving/loading to reset the eGraph

		// create first EdRule of empty base rule
		createRule("Rule");
	}

	public EdGraGra(String name, boolean directedGraphs, boolean parallelArcs) {
		this.name = name;
		
		this.dirName = ""; // System.getProperty("user.dir");
		this.fileName = ""; // name;

		// create base gragra with empty start (base) graph
		// and no rules
//		if (directedGraphs)
//			this.bGraGra = BaseFactory.theFactory().createGraGra();
//		else
			this.bGraGra = BaseFactory.theFactory().createGraGra(true, directedGraphs, parallelArcs);
		this.bGraGra.setName(name);

		// create EdTypeSet
		this.typeSet = new EdTypeSet(this.bGraGra.getTypeSet());
		
		// create EdGraph of empty base graph
		this.eGraph = new EdGraph(this.bGraGra.getGraph(), this.typeSet);
		this.eGraph.setGraGra(this);
		this.eGraphs.add(this.eGraph);
		this.startGraph = null; 
		// will be set after saving/loading to reset the eGraph

		// create first EdRule of empty base rule
		createRule("Rule");
	}
	
	/** Creates a gragra which used object is specified by the GraGra basis. */
	public EdGraGra(GraGra basis) {
		// set base gragra
		this.bGraGra = basis;

		// get gragra name
		this.name = basis.getName();
		this.dirName = ""; // System.getProperty("user.dir");
		this.fileName = "";

		// create EdTypeSet
		this.typeSet = new EdTypeSet(this.bGraGra.getTypeSet());
		if (this.getTypeGraph() != null)
			this.getTypeGraph().setGraGra(this);
		
		// create EdGraph of base graph
		if (!this.bGraGra.getListOfGraphs().isEmpty()) {
			createGraphs(this.bGraGra.getListOfGraphs());
			this.eGraph = this.eGraphs.firstElement();
		}
		this.startGraph = null; 
		// will be set after saving/loading to reset the eGraph

		// create EdRules of base rules
		createRules(this.bGraGra.getListOfRules());

		createAtomics(this.bGraGra.getListOfAtomics());
		createConstraints(this.bGraGra.getListOfConstraints());
	}

	/** Disposes <code>this</code> */
	public void dispose() {	
		if (this.undoManager != null) {
			this.undoManager.discardAllEdits();
		}
		
		while (!this.eRules.isEmpty()) {
			this.eRules.remove(0).dispose();
		}
		
		while (!this.eConstraints.isEmpty()) {
			this.eConstraints.remove(0).dispose();
		}
		
		while (!this.eAtomics.isEmpty()) {
			this.eAtomics.remove(0).dispose();
		}
		
		while (!this.eGraphs.isEmpty()) {
			this.eGraphs.remove(0).dispose();
		}
		
		if (this.startGraph != null) {
			this.startGraph.dispose();
		}
		
		Iterator<Vector<LayoutPattern>> lpIter = this.layoutPatterns.values().iterator();
		while (lpIter.hasNext()) {
			Iterator<LayoutPattern> iter = lpIter.next().iterator();
			while (iter.hasNext()) {
				iter.next().dispose();
			}			
		}
		this.layoutPatterns.clear();
		
		this.typeSet.dispose();
		
		this.eGraph = null;
		this.startGraph = null;
		this.typeSet = null;
		this.undoManager = null;
		
//		System.out.println("EdGraGra.dispose::  DONE  "+this.hashCode());
		
		BaseFactory.theFactory().destroyGraGra(this.bGraGra);
		this.bGraGra = null;
	}
	
	public void finalize() {
//		System.out.println("EdGraGra.finalize()  called  "+this.hashCode());
	}
	
	/** Returns my name */
	public String getName() {
		return this.name;
	}

	/** Sets my name.*/
	public void setName(String str) {
		this.name = str;
		this.bGraGra.setName(str);
	}

	public void setAnimated(boolean b) {
		this.animated = b;
	}
	
	public boolean isAnimated() {
		return this.animated;
	}
	
	public void resetAnimated(boolean updateAnimatedOfRule) {
		for (int i=0; i<this.eRules.size(); i++) {
			EdRule r = this.eRules.get(i);
			if (r.getBasisRule().isEnabled() && r.isAnimated())  {
				this.animated = true;
				
				if (updateAnimatedOfRule) {
					r.setAnimated(true);
				} else {
					break;
				}
			}
		}
	}
	
	public void enableUndoManager(boolean b) {
		this.undoEnabled = b;
		if (this.undoManager != null) {
			this.undoManager.enabled = this.undoEnabled;
		}
	}
	
	public boolean isUndoManagerEnabled() {
		return (this.undoManager != null && this.undoEnabled) ? true : false;
	}
	
	public UndoManager getUndoManager() {
		return this.undoManager;
	}

	public void setUndoManager(EditUndoManager anUndoManager) {
		this.undoManager = anUndoManager;
		
		if (this.typeSet.getTypeGraph() != null)
			this.typeSet.getTypeGraph().setUndoManager(this.undoManager);

		for (int i = 0; i < this.eGraphs.size(); i++) {
			EdGraph g = this.eGraphs.elementAt(i);
			g.setUndoManager(this.undoManager);
		}
		
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule r = this.eRules.elementAt(i);
			if (r != null) {
				r.setUndoManager(this.undoManager);
			}
		}
		
		for (int i = 0; i < this.eAtomics.size(); i++) {
			EdAtomic a = this.eAtomics.elementAt(i);
			a.setUndoManager(this.undoManager);
		}
	}

	/** Gets the directory name for saving the gragra */
	public String getDirName() {
		return this.dirName;
	}

	/** Sets the directory name for saving the gragra */
	public void setDirName(String str) {
		this.dirName = str;
		this.bGraGra.setDirName(str);
	}

	/** Gets the file name for saving the gragra */
	public String getFileName() {
		return this.fileName;
	}

	/** Sets the file name for saving the gragra */
	public void setFileName(String str) {
		this.fileName = str;
		this.bGraGra.setFileName(str);
	}

	/** Returns the used object */
	public agg.xt_basis.GraGra getBasisGraGra() {
		return this.bGraGra;
	}

	/**
	 * Sets the used object to an object specified by the agg.xt_basis.GraGra
	 * gra
	 */
	public void setBasisGraGra(agg.xt_basis.GraGra gra) {
		this.bGraGra = gra;
	}

	/** Returns the current working graph layout */
	public EdGraph getGraph() {
		return this.eGraph;
	}

	public EdGraph getGraph(final String graphname) {
		for (int i=0; i<this.eGraphs.size(); i++) {
			final EdGraph g = this.eGraphs.get(i);
			if (g.getName().equals(graphname)) {
				return g;
			}
		}
		return null;
	}
	
	public EdGraph getGraph(int index) {
		return (index>=0 && index<this.eGraphs.size()) ? this.eGraphs.get(index) : null;
	}
	
	public int getIndexOfGraph() {
		return this.eGraphs.indexOf(this.eGraph);
	}

	public int getIndexOfGraph(final EdGraph g) {
		return this.eGraphs.indexOf(g);
	}
	
	public Vector<EdGraph> getGraphs() {
		return this.eGraphs;
	}

	/** Returns the type graph or null */
	public EdGraph getTypeGraph() {
		return this.typeSet.getTypeGraph();
	}

	// /** Sets the working graph layout to a graph specified by the EdGraph g
	// */
	// public void setGraph(EdGraph g) {eGraph = g; }

	/**
	 * Returns the rule container
	 */
	public Vector<EdRule> getRules() {
		return this.eRules;
	}

	public Vector<EdRule> getEnabledRules() {
		final Vector<EdRule> v = new Vector<EdRule>();
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule er = this.eRules.get(i);
			if (er.getBasisRule().isEnabled()) {
				v.add(er);
			}
		}
		return v;
	}
	
	public Vector<EdRule> getRulesForLayer(int l) {
		Vector<EdRule> v = new Vector<EdRule>(5);
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule r = this.eRules.get(i);
			if (r.getBasisRule().getLayer() == l)
				v.add(r);
		}
		return v;
	}

	/**
	 * Returns the rule sequences. 
	 * The first Vector element of a Pair is a rule subsequence.
	 * The first String element of a Pair is a rule name.
	 * The second element of a Pair represents
	 * the iteration count of a rule subsequence or of an individual rule.
	 * The value for the iteration count may be "*" or a decimal.
	 */
	public List<Pair<List<Pair<String, String>>, String>> getRuleSequenceList() {
		return this.bGraGra.getRuleSequenceList();
	}

	public Vector<EdAtomic> getAtomics() {
		return this.eAtomics;
	}

	public Vector<String> getAtomicNames() {
		Vector<String> ret = new Vector<String>();
		for (int i = 0; i < this.eAtomics.size(); i++) {
			// ret.add(((EdAtomic)eAtomics.get(i)).getBasisAtomic().getName());
			ret.add(this.eAtomics.get(i).getBasisAtomic().getAtomicName());
		}
		return ret;
	}

	public Vector<Evaluable> getAtomicsAsEvaluable() {
		Vector<Evaluable> ret = new Vector<Evaluable>();
		for (int i = 0; i < this.eAtomics.size(); i++) {
			ret.add(this.eAtomics.get(i).getBasisAtomic());
		}
		return ret;
	}

//	private Vector<Object> getAtomicsAsObjects() {
//		Vector<Object> ret = new Vector<Object>();
//		for (int i = 0; i < eAtomics.size(); i++) {
//			ret.add(eAtomics.get(i).getBasisAtomic());
//		}
//		return ret;
//	}

	public Vector<EdConstraint> getConstraints() {
		return this.eConstraints;
	}

	public List<EdConstraint> getEnabledConstraints() {
		final List<EdConstraint> list = new Vector<EdConstraint>();
		for (int i = 0; i < this.eConstraints.size(); i++) {
			EdConstraint c = this.eConstraints.get(i);
			if (c.getBasisConstraint().isEnabled())
				list.add(c);
		}
		return list;
	}
	
	public boolean hasEnabledConstraints() {
		for (int i = 0; i < this.eConstraints.size(); i++) {
			EdConstraint c = this.eConstraints.get(i);
			if (c.getBasisConstraint().isEnabled()) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Reset my work graph to the Graph g. The types of the graph objects in the
	 * graph g have to be the same as of my work graph.
	 */

	public Vector<EdConstraint> getConstraintsForLayer(int l) {
		Vector<EdConstraint> v = new Vector<EdConstraint>(5);
		for (int i = 0; i < this.eConstraints.size(); i++) {
			EdConstraint c = this.eConstraints.get(i);
			Vector<Integer> layer = c.getBasisConstraint().getLayer();
			for (int j = 0; j < layer.size(); j++) {
				if (layer.get(j).intValue() == l)
					v.add(c);
			}
		}
		return v;
	}

	public Vector<String> getGraTraOptions() {
		return this.bGraGra.getGraTraOptions();
	}

	public boolean addGraph(EdGraph g) {
		if (g.getTypeSet().getBasisTypeSet().compareTo(
				this.typeSet.getBasisTypeSet())) {
			if (this.bGraGra.addGraph(g.getBasisGraph())) {
				Vector<Type> v = g.getBasisGraph().getUsedTypes();
				this.bGraGra.getTypeSet().adaptTypes(v.elements(), false);
				this.typeSet.refreshTypes();
				g.update();
				g.setGraGra(this);
				this.eGraphs.add(g);
				g.setUndoManager(this.undoManager);
				return true;
			} 
			return false;
		} 
		return false;
	}

	public boolean removeGraph(EdGraph g) {
		if (this.bGraGra == null)
			return false;
		if (g != null) {
			if (g.getBasisGraph() != null) {
				if (this.bGraGra.removeGraph(g.getBasisGraph())) {
					if (this.eGraph == g) {
						this.eGraphs.removeElement(this.eGraph);
						if (this.eGraphs.size() > 0) {						
							if (this.bGraGra.resetGraph(this.eGraphs.firstElement().getBasisGraph())) {
								this.eGraph = this.eGraphs.firstElement();
							} else {
								return false;
							}
						} 
					} else {
						this.eGraphs.removeElement(g);
					}
					this.isChanged = true;
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Adds the specified EdGraph g to the current type graph if it exists,
	 * otherwise creates a type graph and then adds the EdGraph g. The graph g
	 * is a type graph, so g.isTypeGraph() should return true. The type graph
	 * check should be disabled. The new node/edge types are added to the
	 * current types. The current type graph structure and the structure of the
	 * graph g are united dis-jointly. Double occurrences of the nodes/arcs are
	 * possible and they have to be resolved by the user manually.
	 */
	public synchronized boolean importTypeGraph(EdGraph g, boolean rewrite) {
		if (!g.isTypeGraph()) {
			return false;
		}
		boolean result = false;
		if (this.typeSet.getTypeGraph() == null) {
			this.typeSet.createTypeGraph();
		}
		
		this.typeSet.getTypeGraph().setUndoManager(this.undoManager);
		
		if (this.bGraGra.importTypeGraph(g.getBasisGraph(), rewrite)) {
			this.typeSet.refreshTypes();
			this.typeSet.getTypeGraph().deselectAll();
			this.typeSet.getTypeGraph().synchronizeWithBasis(true);
			this.typeSet.getTypeGraph().setLayoutByType(g);
			update(true);
			result = true;
		}
		
		return result;
	}

	/**
	 * Reset my host graph by the EdGraph g. The types of the graph objects of
	 * the graph g must be found in my types.
	 */
	public synchronized boolean importGraph(EdGraph g) {
		if (this.bGraGra.importGraph(g.getBasisGraph())) {
			this.typeSet.refreshTypes();
			int indx = this.eGraphs.indexOf(this.eGraph);
			if (indx != -1) {
				this.eGraphs.remove(this.eGraph);
				this.eGraph.dispose();
			}
			EdGraph impG = new EdGraph(this.bGraGra.getGraph(), this.typeSet, true);
			impG.setGraGra(this);
			impG.setUndoManager(this.undoManager);
			impG.setLayoutByIndex(g, false);
			if (indx != -1)
				this.eGraphs.add(indx, impG);
			else
				this.eGraphs.add(impG);
			this.eGraph = impG;
			return true;
		} 
		return false;
	}

	/**
	 * Add the EdGraph g to my list of (host) graphs. The types of the graph
	 * objects of the graph g must be found in my types.
	 */
	public synchronized boolean addImportGraph(EdGraph g) {
		if (this.bGraGra.addImportGraph(g.getBasisGraph())) {
			this.typeSet.refreshTypes();
			// EdGraph impG = new EdGraph(bGraGra.getGraph(), typeSet, true);
			EdGraph impG = new EdGraph(this.bGraGra.getListOfGraphs().get(this.bGraGra.getCountOfGraphs()-1),
					this.typeSet, true);
			impG.setGraGra(this);
			impG.setLayoutByIndex(g, false);
			this.eGraphs.add(impG);
			impG.setUndoManager(this.undoManager);
			// eGraph = impG;
			return true;
		} 
		return false;
	}

	/**
	 * Reset my host graph by the EdGraph g. If the specified parameter boolean
	 * adapt is true, the types of the graph objects in the specified EdGraph g
	 * could be adapted to my types. In this case only names of the types have
	 * to be equal.
	 */
	public synchronized boolean importGraph(EdGraph g, boolean adapt) {
		if (this.bGraGra.importGraph(g.getBasisGraph(), adapt)) {
			this.typeSet.refreshTypes(true);
			int indx = this.eGraphs.indexOf(this.eGraph);
			if (indx != -1) {
				this.eGraphs.remove(this.eGraph);
			}
			EdGraph impG = new EdGraph(this.bGraGra.getGraph(), this.typeSet, true);
			impG.setGraGra(this);
			impG.setLayoutByIndex(g, false);
			impG.setUndoManager(this.undoManager);
			if (indx != -1)
				this.eGraphs.add(indx, impG);
			else
				this.eGraphs.add(impG);
			this.eGraph = impG;
			return true;
		} 
		return false;
	}

	/**
	 * Add the EdGraph g to my list of (host) graphs. If the specified parameter
	 * boolean adapt is true, the types of the graph objects in the specified
	 * EdGraph g could be adapted to my types. In this case only names of the
	 * types have to be equal.
	 */
	public synchronized boolean addImportGraph(EdGraph g, boolean adapt) {
		if (this.bGraGra.addImportGraph(g.getBasisGraph(), adapt)) {
			this.typeSet.refreshTypes(true);
			// EdGraph impG = new EdGraph(bGraGra.getGraph(), typeSet, true)
			EdGraph impG = new EdGraph(this.bGraGra.getListOfGraphs().get(this.bGraGra.getCountOfGraphs()-1),
					this.typeSet, true);
			impG.setGraGra(this);
			impG.setLayoutByIndex(g, false);
			this.eGraphs.add(impG);
			impG.setUndoManager(this.undoManager);
			// eGraph = impG;
			return true;
		} 
		return false;
	}

	public void refreshAttrInstances() {
		this.typeSet.refreshAttrInstances();
		this.bGraGra.refreshAttributed();
	}
	
	/** Reset my host graph by my start graph. */
	public synchronized boolean resetGraph() {
		if (this.startGraph == null 
				|| this.startGraph == this.eGraph
				|| this.typeSet == null
				|| this.startGraph.getTypeSet() == null
				|| !this.typeSet.equals(this.startGraph.getTypeSet())) {
			return false;
		}

		EdGraph eg = this.startGraph.copy();
		eg.setGraGra(this);
		
		String origGraphName = this.eGraph.getName();
		
		if (this.bGraGra.resetGraph(eg.getBasisGraph())) {				
			int indx = this.eGraphs.indexOf(this.eGraph);				
			this.eGraphs.remove(this.eGraph);
			this.eGraph.dispose();
			
			this.eGraph = eg;
			this.eGraph.setCurrentLayoutToDefault(true);
			this.eGraph.setUndoManager(this.undoManager);
			if (indx == -1)
				this.eGraphs.add(this.eGraph);
			else
				this.eGraphs.add(indx, this.eGraph);

			this.eGraph.getBasisGraph().setName(origGraphName);
			
			this.isChanged = true;
			return true;
		} 
		return false;		
	}
	
	public synchronized boolean resetGraph(int atIndex) {
		if (this.startGraph == null 
				|| this.startGraph == this.eGraph
				|| this.typeSet == null
				|| this.startGraph.getTypeSet() == null
				|| !this.typeSet.equals(this.startGraph.getTypeSet())
				|| atIndex < 0 
				|| atIndex >= this.eGraphs.size()) {
			return false;
		}
		
		EdGraph eg = this.startGraph.copy();
		eg.setGraGra(this);
		
		String origGraphName = this.eGraphs.get(atIndex).getName();
		if (this.bGraGra.resetGraph(atIndex, eg.getBasisGraph())) {								
			this.eGraphs.remove(atIndex);
			eg.setCurrentLayoutToDefault(true);
			eg.setUndoManager(this.undoManager);
			this.eGraphs.add(atIndex, eg);
			this.eGraphs.get(atIndex).getBasisGraph().setName(origGraphName);
			this.isChanged = true;
			return true;
		} 
		return false;		
	}
	

	/**
	 * Reset my host graph by the EdGraph g. 
	 * Precondition: (g.getGraGra() == this)
	 * Additionally, the types of the graph objects of the graph
	 * g have to be similar of my types.
	 */
	public synchronized boolean resetGraph(EdGraph g) {
		if (g.getGraGra() == this) {
			if (this.bGraGra.resetGraph(g.getBasisGraph())) {
				int indxOther = this.eGraphs.indexOf(g);
				int indx = this.eGraphs.indexOf(this.eGraph);
				if (indxOther == -1 && indx != -1)
					this.eGraphs.remove(this.eGraph);
				
				this.eGraph = g;
				this.eGraph.setCurrentLayoutToDefault(true);
				this.eGraph.setGraGra(this);
				this.eGraph.setUndoManager(this.undoManager);
				if (!this.eGraphs.contains(this.eGraph)) {
					if (indx != -1)
						this.eGraphs.add(indx, this.eGraph);
					else
						this.eGraphs.add(this.eGraph);
				}
				this.isChanged = true;
				return true;
			} 
			return false;
		} 
		return false;
	}

	/**
	 * Reset my work graph by the EdGraph g without type guarantee. The types of
	 * the graph g should be similar to my types.
	 */
	public synchronized boolean resetGraphWithoutGuarantee(EdGraph g) {
		if (g != null) {
			if (this.bGraGra.resetGraphWithoutGuarantee(g.getBasisGraph())) {
				int indx = this.eGraphs.indexOf(this.eGraph);
				this.eGraphs.remove(this.eGraph);
				
				this.eGraph = new EdGraph(this.bGraGra.getGraph());
				this.eGraph.setUndoManager(this.undoManager);
				this.eGraph.setLayoutByIndex(g, false);
				this.eGraph.setCurrentLayoutToDefault(true);
				this.eGraph.setGraGra(this);
				if (indx != -1)
					this.eGraphs.add(indx, this.eGraph);
				else
					this.eGraphs.add(this.eGraph);
				this.isChanged = true;
				return true;
			} 
			return false;
		} 
		return false;
	}

	public EdGraph getStartGraph() {
		return this.startGraph;
	}

	public void destroyStartGraph() {
		if (this.startGraph != null)
			this.startGraph.dispose();
		this.startGraph = null;
	}

	/**
	 * checks all elements of this gragra for valid typing. First of all the
	 * type graph will be checked and then all other elements with the type
	 * graph. all errous elements will be marked with an sign until this method
	 * is recalled.
	 * 
	 * @return An empty {@link Collection} if all the graphs are valid typed. If
	 *         there were type errors in the graphs or the type graph was not
	 *         valid a Collection with objects of class {@link TypeError} will
	 *         returned. For each mismatch an object will delivered. You can
	 *         check if there were errors with {@link Collection#isEmpty}.
	 * 
	 * @see TypeSet#checkType
	 * @see TypeSet#checkTypeGraph
	 * @see TypeSet#enableTypeGraphCheck
	 */
	public Collection<TypeError> setLevelOfTypeGraphCheck(int level) {
		// run the check in the base layer
		Collection<TypeError> baseResult = this.getBasisGraGra()
				.setLevelOfTypeGraphCheck(level);

		// first we create a mapping from editor graphs
		// to base graphs
		WeakHashMap<GraphObject, EdGraphObject> 
		base2ed = new WeakHashMap<GraphObject, EdGraphObject>();

//		insert type graph
		putAllGraphObjectsInMap(base2ed, this.typeSet.getTypeGraph());
		
		// insert host graphs
		for (int i=0; i<this.eGraphs.size(); i++) {
			EdGraph eg = this.eGraphs.get(i);
			putAllGraphObjectsInMap(base2ed, eg);
		}
		
		// insert rules
		for (int i=0; i<this.eRules.size(); i++) {		
			EdRule er = this.eRules.get(0);
			if (er instanceof EdRuleScheme) {
				putAllGraphObjectsOfRuleSchemeInMap(base2ed, (EdRuleScheme) er);
			} else {
				putAllGraphObjectsOfRuleInMap(base2ed, er);
			}
		}

		// insert atomics
		for (int i=0; i<this.eAtomics.size(); i++) {
			EdAtomic eAtomic = this.eAtomics.get(i);
			putAllGraphObjectsInMap(base2ed, eAtomic.getLeft());
			// insert conclusions of atomic
			Iterator<EdAtomic> subIter = eAtomic.getConclusions().iterator();
			while (subIter.hasNext()) {
				EdGraph ec = subIter.next().getRight();
				putAllGraphObjectsInMap(base2ed, ec);
			}
		}
		
		// if no errors found, return empty list
		if (baseResult.isEmpty()) {
			return baseResult;
		}

		Iterator<TypeError> iter = baseResult.iterator();
		TypeError baseError;
		while (iter.hasNext()) {
			// get the error
			baseError = iter.next();

			GraphObject baseObject = baseError.getGraphObject();
			// find the object with this error
			if (baseObject != null) {
				EdGraphObject edObject = base2ed.get(baseObject);
				if (edObject != null)
					edObject.setErrorMode(true);
			}
		}

		this.update();

		return baseResult;
	}

	private void putAllGraphObjectsOfRuleSchemeInMap(
			final WeakHashMap<GraphObject, EdGraphObject> base2ed, 
			final EdRuleScheme rs) {
		
		putAllGraphObjectsOfRuleInMap(base2ed, rs.getKernelRule());
		for (int i=0; i<rs.getMultiRules().size(); i++) {
			putAllGraphObjectsOfRuleInMap(base2ed, rs.getMultiRules().get(i));
		}
				
	}
	
	private void putAllGraphObjectsOfRuleInMap(
			final WeakHashMap<GraphObject, EdGraphObject> base2ed, 
			final EdRule r) {
		putAllGraphObjectsInMap(base2ed, r.getLeft());
		putAllGraphObjectsInMap(base2ed, r.getRight());
		// insert NAC of rule
		Iterator<EdNAC> subIter = r.getNACs().iterator();
		while (subIter.hasNext()) {
			EdNAC eNAC = subIter.next();
			putAllGraphObjectsInMap(base2ed, eNAC);
		}
//		insert PAC of rule
		Iterator<EdPAC> subIter1 = r.getPACs().iterator();
		while (subIter1.hasNext()) {
			EdPAC ePAC = subIter1.next();
			putAllGraphObjectsInMap(base2ed, ePAC);
		}		
	}
	
	/**
	 * marks the type graph as unchecked, so no longer the type graph will be
	 * used for type checks.
	 * 
	 * @see TypeSet#disableTypeGraphCheck
	 */
	public void disableTypeGraphCheck() {
		this.typeSet.getBasisTypeSet().setLevelOfTypeGraphCheck(TypeSet.DISABLED);
	}// turnTypeGraphCheckOff

	public int getLevelOfTypeGraphCheck() {
		return this.typeSet.getBasisTypeSet().getLevelOfTypeGraphCheck();
	}// isTypeGraphCheckEnabled

	/**
	 * internal method to put all graph objects of a graph in a map. The map
	 * will map the base {@link GraphObject} to the {@link EdGraphObject}. For
	 * each graph object the error mode will also set to false (see
	 * {@link EdGraphObject#setErrorMode})
	 */
	private void putAllGraphObjectsInMap(Map<GraphObject, EdGraphObject> map,
			EdGraph graph) {
		Iterator<?> iter = graph.getArcs().iterator();
		EdArc actArc;
		while (iter.hasNext()) {
			actArc = (EdArc) iter.next();
			map.put(actArc.getBasisArc(), actArc);
			actArc.setErrorMode(false);
		}// while

		iter = graph.getNodes().iterator();
		EdNode actNode;
		while (iter.hasNext()) {
			actNode = (EdNode) iter.next();
			map.put(actNode.getBasisNode(), actNode);
			actNode.setErrorMode(false);
		}// while
	}

	/** Creates a new rule scheme layout. The name is specified by the String name */
	public EdRuleScheme createRuleScheme(String ruleSchemeName) {
		if (ruleSchemeName != null) {
			EdRuleScheme eRuleScheme = new EdRuleScheme(this.bGraGra.createRuleScheme(), this.typeSet);
			eRuleScheme.setUndoManager(this.undoManager);
			eRuleScheme.getBasisRule().setName(ruleSchemeName);
			eRuleScheme.setGraGra(this);
			this.eRules.addElement(eRuleScheme);
			this.isChanged = true;
			return eRuleScheme;
		}
		return null;
	}
	
	/** Creates a new rule scheme layout with a kernel rule as a copy of the specified rule
	 * and empty list of multi rules.
	 * 
	 * @return	EdRuleScheme  or null if creation failed
	 */
	public EdRuleScheme createRuleScheme(EdRule rule) {
		if (rule != null) {
			RuleScheme rs = this.bGraGra.createRuleScheme(rule.getBasisRule());
			if (rs != null) {
				EdRuleScheme eRS = new EdRuleScheme(rs, this.typeSet);				
				eRS.getKernelRule().setLayoutByIndexFrom(rule, true);
				eRS.setUndoManager(this.undoManager);
				eRS.setGraGra(this);
				int indx = this.eRules.indexOf(rule);
				if (indx >= 0)
					this.eRules.add(indx+1, eRS);
				else
					this.eRules.add(eRS);
				this.isChanged = true;
				return eRS;
			}
		}
		return null;
	}
	
	/** Creates a new rule layout. The name is specified by the String name */
	public EdRule createRule(String ruleName) {
		if (ruleName != null) {
			EdRule eRule = new EdRule(this.bGraGra.createRule(), this.typeSet);
			eRule.setUndoManager(this.undoManager);
			eRule.getBasisRule().setName(ruleName);
			eRule.setGraGra(this);
			eRule.setEditable(true);
			this.eRules.addElement(eRule);
			this.isChanged = true;
		
			return eRule;
		}
		return null;
	}

	public boolean addRule(EdRule r) {
		if (r != null && !this.eRules.contains(r)) {
			if (r.getTypeSet().getBasisTypeSet().compareTo(
					this.typeSet.getBasisTypeSet())) {
				if (this.bGraGra.getRule(r.getBasisRule().getName()) != null)
					r.getBasisRule().setName(r.getName() + "_" + this.eRules.size());
	
				if (this.bGraGra.addRule(r.getBasisRule())) {
					Vector<Type> v = r.getBasisRule().getUsedTypes();
					this.bGraGra.getTypeSet().adaptTypes(v.elements(), false);
					this.typeSet.refreshTypes();
					this.eRules.add(r);
					r.update();
					r.setUndoManager(this.undoManager);
					r.setGraGra(this);
					return true;
				}
			}
		}
		return false;
	}

//	public boolean addRuleAt(int indx, EdRule r) {
//		if (r != null && !this.eRules.contains(r)) {
//			
//		}
//		return false;
//	}
	
	public EdGraph cloneGraph() {
		return cloneGraph(true);
	}

	public EdGraph cloneGraph(boolean insertToGraGra) {
		this.eGraph.setContextUsageOfGraphObjToBasisHashCode();
		Graph graph = this.bGraGra.cloneGraph();
		if (graph == null) {
			return null;
		}
		
		graph.setName(this.eGraph.getBasisGraph().getName() + "_clone");
		EdGraph clone = new EdGraph(graph, this.typeSet);
		clone.setGraGra(this);
		clone.setLayoutByContextUsage(this.eGraph, true);
//		clone.setLayoutByIndex(eGraph, false);
		this.eGraph.unsetContextUsageOfGraphObj();
		clone.unsetContextUsageOfGraphObj();
		clone.setUndoManager(this.undoManager);
		// insert clone after original
		if (insertToGraGra) {
			this.bGraGra.addGraph(graph);
			this.eGraphs.add(clone);
			this.eGraph.setUndoManager(this.undoManager);
			this.isChanged = true;
		}
		return clone;
	}

	/**
	 * Creates an inverse rule of the given Rule <code>r</code> and adds it to the rule set 
	 * when the given parameter <code>doInsert</code> is true.
	 * Returns created inverse rule by success, otherwise null.
	 */
	public EdRule reverseRule(EdRule r, boolean doInsert) {
		final Pair<Pair<Rule,Boolean>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		inverseRulePair = BaseFactory.theFactory().reverseRule(r.getBasisRule());
//		BaseFactory.theFactory().unsetAllTransientAttrValuesOfRule(inverseRulePair.first);
		
		if (inverseRulePair != null) {
			final EdRule invR = new EdRule(inverseRulePair.first.first);
			invR.setLayoutByIndexFrom(r, true);
						
			if (doInsert) {
				int indx = this.eRules.indexOf(r)+1;
				if (indx >= 0) {
					this.bGraGra.addRuleAt(indx, invR.getBasisRule());
					this.eRules.add(indx, invR);
				}
				else {
					this.bGraGra.addRule(invR.getBasisRule());
					this.eRules.add(invR);
				}
				invR.setUndoManager(this.undoManager);
				invR.setGraGra(this);				
			}
			return invR;
		}
		
		return null;
	}
	
	/**
	 * Creates an inverse rule scheme of the given RuleScheme <code>rs</code> 
	 * and adds it to the rule set 
	 * when the given parameter <code>doInsert</code> is true.
	 */
	public EdRuleScheme reverseRuleScheme(EdRuleScheme rs, boolean doInsert) {
		final RuleScheme inv = BaseFactory.theFactory().reverseRuleScheme(rs.getBasisRuleScheme());		
		if (inv != null) {
			final EdRuleScheme invRS = new EdRuleScheme(inv, this.typeSet);
			invRS.setLayoutByIndexFrom(rs, true);						
			if (doInsert) {
				int indx = this.eRules.indexOf(rs)+1;
				if (indx >= 0) {
					this.bGraGra.addRuleAt(indx, invRS.getBasisRule());
					this.eRules.add(indx, invRS);
				}
				else {
					this.bGraGra.addRule(invRS.getBasisRule());
					this.eRules.add(invRS);
				}
				invRS.setUndoManager(this.undoManager);
				invRS.setGraGra(this);			
			}
			return invRS;
		}
		
		return null;
	}
	
	/**
	 * Creates an inverse rule of the given Rule <code>r</code> and adds it to the rule set 
	 * when the given parameter <code>doInsert</code> is true.
	 */
	public EdRule makeConcurrentRule(
			final EdRule r1, 
			final EdRule r2, 
			boolean disjoint, boolean doInsert) {
				
		final ConcurrentRule concurrentRule = new ConcurrentRule(r1.getBasisRule(), r2.getBasisRule());
		
		if (concurrentRule.getRule() != null) {
			
			final EdRule rule = new EdRule(concurrentRule.getRule());
			
			takeLayoutFromRuleToRule(
					r1, 
					rule,
					concurrentRule.getFirstLeftEmbedding(),
					concurrentRule.getFirstRightEmbedding(),
					false);
			
			takeLayoutFromRuleToRule(
					r2, 
					rule,
					concurrentRule.getSecondLeftEmbedding(),
					concurrentRule.getSecondRightEmbedding(),
					false);
			
			if (doInsert) {
				this.addRule(rule);				
			}
			return rule;
		}
		
		return null;
	}
	
	
	public EdRule makeConcurrentRuleOfRuleSeq(
			final RuleSequence seq,
			boolean byObjectFlow,
			boolean addToRuleList) {
				
		if (seq != null) {
			long t0 = System.currentTimeMillis();
			
			seq.enableCompleteObjFlowOfNodes(false);
			final ConcurrentRule concurrentRule = BaseFactory.theFactory()
						.makeConcurrentRuleOfRuleSeqBackwards(seq, byObjectFlow);	
			
			if (concurrentRule != null 
					&& concurrentRule.getRule() != null) {	
				
				System.out.println(">>> Concurrent rule creation time : "+(System.currentTimeMillis()-t0) + "ms   ");
				System.out.println(">>> Concurrent rule creation - Used memory: "+concurrentRule.usedM/1024+"k");
				
				final EdRule rule = new EdRule(concurrentRule.getRule());
				
				for (int i=0; i<seq.getRules().size(); i++) {
					Rule r = seq.getRules().get(i);
					EdRule er = this.getRule(r.getName());
					
					takeLayoutFromRuleToRule(
							er, 
							rule,
							concurrentRule.getFirstLeftEmbedding(),
							concurrentRule.getFirstRightEmbedding(),
							false);
					
					takeLayoutFromRuleToRule(
							er, 
							rule,
							concurrentRule.getSecondLeftEmbedding(),
							concurrentRule.getSecondRightEmbedding(),
							false);
				}
				
				if (addToRuleList) {
					this.addRule(rule);	
					if (rule.getBasisRule().getMatch() != null) {
						this.bGraGra.addMatch(rule.getBasisRule().getMatch());
					}
				}
//				System.out.println(">>> Concurrent rule total creation time : "+(System.currentTimeMillis()-t0) + "ms   ");
				return rule;
			}			
		} 
		return null;
	}
	
	public List<EdRule> makeConcurrentRuleOfRuleSeqForward(
			final RuleSequence seq,
			boolean completeConcurrency,
			boolean addToRuleList) {
				
		if (seq != null) {
			long t0 = System.currentTimeMillis();
			long freem0 = Runtime.getRuntime().freeMemory();
			
			final List<ConcurrentRule> 
			crs = BaseFactory.theFactory()
						.makeConcurrentRuleOfRuleSeqForward(seq, this.bGraGra, completeConcurrency);
			
			long usedm = freem0-Runtime.getRuntime().freeMemory();
			
			if (crs != null && !crs.isEmpty()) {
				System.out.println(">>> Concurrent rule creation time : "+(System.currentTimeMillis()-t0) + "ms   ");
				System.out.println(">>> Concurrent rule creation - Used memory: "+usedm/1024+"k");
				
				List<EdRule> rs = new Vector<EdRule>(crs.size());
				
				for (int j=0; j<crs.size(); j++) {
					ConcurrentRule cr = crs.get(j);
					if (cr.getRule() != null) {
				
						final EdRule rule = new EdRule(cr.getRule());
						
						int i = seq.getRules().size()-1;
		//				for (int i=0; i<seq.getRules().size(); i++) 
						{
							Rule r = seq.getRules().get(i);
							EdRule er = this.getRule(r.getName());
							
							takeLayoutFromRuleToRule(
									er, 
									rule,
									cr.getFirstLeftEmbedding(),
									cr.getFirstRightEmbedding(),
									false);
							
							takeLayoutFromRuleToRule(
									er, 
									rule,
									cr.getSecondLeftEmbedding(),
									cr.getSecondRightEmbedding(),
									false);
						}
						
						if (addToRuleList) {
							this.addRule(rule);	
							if (rule.getBasisRule().getMatch() != null) {
								this.bGraGra.addMatch(rule.getBasisRule().getMatch());
							}
						}
						rs.add(rule);
					}
				}
				return rs;
			}			
		} 
		return null;
	}
	
	public EdRule makeParallelRuleOfRules(
			final List<Rule> rules,
			boolean addToRuleList) {
				
		if (!rules.isEmpty()) {	
			long t0 = System.currentTimeMillis();
			long freem0 = Runtime.getRuntime().freeMemory();
			
			final ParallelRule
			parallelRule = new ParallelRule(this.typeSet.getBasisTypeSet(), rules);
			
			System.out.println(">>> Parallel rule creation time : "+(System.currentTimeMillis()-t0) + "ms   ");
			long usedm = freem0-Runtime.getRuntime().freeMemory();
			System.out.println(">>> Concurrent rule creation - Used memory: "+usedm/1024+"k");
			
			if (parallelRule.isValid() && parallelRule.isApplicable()) {
				final EdRule rule = new EdRule(parallelRule);
				String namestr = ""; 
				for (int i=0; i<rules.size(); i++) {
					Rule r = rules.get(i);
					EdRule er = this.getRule(r);
					namestr = namestr + r.getName() + "+";	
					takeLayoutFromRuleToRule(
								er, 
								rule,
								parallelRule.getLeftEmbedding().get(i),
								parallelRule.getRightEmbedding().get(i),
								false);
				}
				rule.getBasisRule().setName(namestr.substring(0, namestr.length()-1));
				
				if (addToRuleList) {
					this.addRule(rule);				
				}
				return rule;
			}
		} 
		return null;
	}
	
	public EdRule makeParallelRuleOfTwoRules(
			final Rule r1, 
			final Rule r2,
			boolean addToRuleList) {
							
		final ParallelRule
		parallelRule = new ParallelRule(this.typeSet.getBasisTypeSet(), r1, r2);
		
		if (parallelRule.isValid() && parallelRule.isApplicable()) {
			final EdRule rule = new EdRule(parallelRule);
			
			EdRule er1 = this.getRule(r1.getName());
			takeLayoutFromRuleToRule(
							er1, 
							rule,
							parallelRule.getLeftEmbedding().get(0),
							parallelRule.getRightEmbedding().get(0),
							false);
			EdRule er2 = this.getRule(r2.getName());	
			takeLayoutFromRuleToRule(
							er2, 
							rule,
							parallelRule.getLeftEmbedding().get(1),
							parallelRule.getRightEmbedding().get(1),
							false);
					
			if (addToRuleList) 
				this.addRule(rule);				
						
			return rule;
		}
		return null;
	}
	
	private void takeLayoutFromRuleToRule(
			final EdRule fromRule, 
			final EdRule toRule,
			final OrdinaryMorphism leftFrom2leftTo,
			final OrdinaryMorphism rightFrom2rightTo,
			boolean inverse) {
		
		final EdGraph lg = (inverse)? toRule.getRight(): toRule.getLeft();
		final EdGraph rg = (inverse)? toRule.getLeft(): toRule.getRight();
		
		// set node layout of LHS
		Vector<EdNode> nodes = fromRule.getLeft().getNodes();
		for (int i=0; i<nodes.size(); i++) {
			EdNode n = nodes.get(i);
			EdNode inv = lg.findNode(leftFrom2leftTo.getImage(n.getBasisObject()));
			if (inv != null) {
				inv.setXY(n.getX(), n.getY());
			}
		}
		// set node layout of RHS
		nodes = fromRule.getRight().getNodes();
		for (int i=0; i<nodes.size(); i++) {
			EdNode n = nodes.get(i);
			EdNode inv = rg.findNode(rightFrom2rightTo.getImage(n.getBasisObject()));
			if (inv != null) {
				inv.setXY(n.getX(), n.getY());
			}
		}
	
		final Vector<EdPAC> gacs = toRule.getNestedACs();
		for (int j=0; j<gacs.size(); j++) {
			EdPAC ac = gacs.get(j);
			EdPAC acOrig = fromRule.getNestedAC(ac.getName());
			if (acOrig != null)
				ac.setLayoutByIndex(acOrig, true);
		}
		
		final Vector<EdNAC> nacs = toRule.getNACs();
		for (int j=0; j<nacs.size(); j++) {
			EdNAC ac = nacs.get(j);
			EdNAC acOrig = fromRule.getNAC(ac.getName());
			if (acOrig != null)
				ac.setLayoutByIndex(acOrig, true);
		}
		
		final Vector<EdPAC> pacs = toRule.getPACs();
		for (int j=0; j<pacs.size(); j++) {
			EdPAC ac = pacs.get(j);
			EdPAC acOrig = fromRule.getPAC(ac.getName());
			if (acOrig != null)
				ac.setLayoutByIndex(acOrig, true);
		}
	}
	

	/**
	 * Creates a clone of the given rule. 
	 * The new rule is added at the end of the rule set.
	 */
	public EdRule cloneRule(EdRule rule) {
		return this.cloneRule(rule, false);
	}
	
	/**
	 * Creates a clone of the given rule. If the parameter doInsert is true, the
	 * copy will be inserted into the rule set after the given rule.
	 * Otherwise, the copy is added at the end of the rule set.
	 */
	public EdRule cloneRule(EdRule rule, boolean doInsert) {
		Rule r = BaseFactory.theFactory().cloneRule(rule.getBasisRule(),
									this.getTypeSet().getBasisTypeSet(), true);
		r.setName(rule.getBasisRule().getName() + "_clone");
		EdRule clone = new EdRule(r, this.getTypeSet());
		clone.setGraGra(this);
		clone.setLayoutByIndexFrom(rule, false);
		
		if (doInsert) {
			// insert clone after original
			int indx = this.eRules.indexOf(rule)+1;
			this.bGraGra.addRuleAt(indx, r);
			this.eRules.add(indx, clone);
		} else {
			this.bGraGra.addRule(r);
			this.eRules.add(clone);
		}
		this.isChanged = true;
		
		return clone;
	}
	
	/**
	 * Creates a clone of the given amalgamated rule. If the parameter doInsert is true, the
	 * copy will be inserted into the rule set after the given EdRuleScheme rs which is the owner
	 * of the amalgamated rule.
	 * Otherwise, the copy is added at the end of the rule set.
	 */
	public EdRule cloneAmalgamatedRule(EdRule rule, EdRuleScheme rs, boolean doInsert) {
		Rule r = BaseFactory.theFactory().cloneRule(rule.getBasisRule(),
									this.getTypeSet().getBasisTypeSet(), true);
		r.setName(rule.getBasisRule().getName() + "_clone");
		EdRule clone = new EdRule(r, this.getTypeSet());
		clone.setGraGra(this);
		clone.setLayoutByIndexFrom(rule, false);
		
		if (doInsert && rs != null) {
			// insert clone after original
			int indx = this.eRules.indexOf(rs)+1;
			this.bGraGra.addRuleAt(indx, r);
			this.eRules.add(indx, clone);
		} else {
			this.bGraGra.addRule(r);
			this.eRules.add(clone);
		}
		this.isChanged = true;
		
		return clone;
	}
	
	/**
	 * Makes the minimal rule from the given rule.
	 * A minimal rule comprises the effects of a given rule in a minimal context.
	 */
	public EdRule makeMinimalRule(EdRule rule, boolean doInsert) {
		Rule r = BaseFactory.theFactory().cloneRule(rule.getBasisRule(),
									this.getTypeSet().getBasisTypeSet(), false);
		r.setName(rule.getBasisRule().getName() + "_Minimal");
		EdRule clone = new EdRule(r, this.getTypeSet());
		clone.setGraGra(this);
		clone.setLayoutByIndexFrom(rule, false);
		
		clone.getLeft().setTransformChangeEnabled(true);
		clone.getRight().setTransformChangeEnabled(true);
		if (!BaseFactory.theFactory().removePreservedUnchangedObjs(r))
			clone.getBasisRule().setErrorMsg("The minimal rule is identical to the given rule.");
		else {
			clone.updateRule();
		}
		clone.getLeft().setTransformChangeEnabled(false);
		clone.getRight().setTransformChangeEnabled(false);
		
		if (doInsert) {
			// insert clone after original
			int indx = this.eRules.indexOf(rule)+1;
			this.bGraGra.addRuleAt(indx, r);
			this.eRules.add(indx, clone);
		} else {
			this.bGraGra.addRule(r);
			this.eRules.add(clone);
		}
		this.isChanged = true;
		
		return clone;
	}
	
	/**
	 * Creates a clone of the rule scheme. If the parameter doInsert is true, the
	 * copy will be inserted into its rule set.
	 */
	public EdRuleScheme cloneRuleScheme(EdRuleScheme rs, boolean doInsert) {
		RuleScheme brs = BaseFactory.theFactory().cloneRuleScheme(rs.getBasisRuleScheme(),
									this.getTypeSet().getBasisTypeSet());
		brs.setName(rs.getBasisRuleScheme().getName() + "_clone");
		EdRuleScheme clone = new EdRuleScheme(brs, this.getTypeSet());
		clone.setGraGra(this);
		clone.setLayoutByIndexFrom(rs);
		
		if (doInsert) {
			// insert clone after original
			int indx = this.eRules.indexOf(rs)+1;
			this.bGraGra.addRuleAt(indx, brs);
			this.eRules.add(indx, clone);
		} else {
			this.bGraGra.addRule(brs);
			this.eRules.add(clone);
		}
		this.isChanged = true;
		
		return clone;
	}

	
	@SuppressWarnings("unused")
	private EdRule cloneRuleOLD(EdRule r, boolean doInsert) {
		Rule rule = this.bGraGra.createRule();
		if (rule == null) {
			return null;
		}
		
		rule.setName(r.getBasisRule().getName() + "_clone");
		rule.setLayer(r.getBasisRule().getLayer());
		EdRule ruleClone = new EdRule(rule, this.typeSet);

		// because bGraGra.createRule() adds a new rule to its rules
		// automatically
		// do remove the new rule from its rules
		if (doInsert) {
			this.bGraGra.getListOfRules().remove(rule);
			// insert clone after original
			this.bGraGra.getListOfRules().add(
					this.bGraGra.getListOfRules().indexOf(r.getBasisRule()) + 1, rule);
			this.eRules.insertElementAt(ruleClone, this.eRules.indexOf(r) + 1);
		} else {
			this.eRules.add(ruleClone);
		}
		this.isChanged = true;
		
		// copy attr. context :: variables
		AttrContext ac = ruleClone.getBasisRule().getAttrContext();
		VarTuple avt = (VarTuple) ac.getVariables();
		AttrContext acOther = r.getBasisRule().getAttrContext();
		VarTuple avtOther = (VarTuple) acOther.getVariables();
		for (int i = 0; i < avtOther.getSize(); i++) {
			VarMember varOther = avtOther.getVarMemberAt(i);
			VarMember var = avt.getVarMemberAt(varOther.getName());
			if (var != null) {
				var.setMark(varOther.getMark());
				var.setInputParameter(varOther.isInputParameter());
			} else {
				avt.declare(varOther.getHandler(), varOther.getDeclaration()
						.getTypeName(), varOther.getName());
				var = avt.getVarMemberAt(varOther.getName());
				if (var != null) {
					var.setMark(varOther.getMark());
					var.setInputParameter(varOther.isInputParameter());
				}
			}
		}

		// storage table
		Hashtable<GraphObject, GraphObject> table = new Hashtable<GraphObject, GraphObject>();
		// copy LHS and RHS
		this.copyGraph(r.getLeft(), ruleClone.getLeft(), table);
		this.copyGraph(r.getRight(), ruleClone.getRight(), table);
		// copy rule morphism
		this.copyMorph(r.getBasisRule(), ruleClone.getBasisRule(), table);

		// copy NACs
		Enumeration<EdNAC> nacs = r.getNACs().elements();
		while (nacs.hasMoreElements()) {
			EdNAC cond = nacs.nextElement();
			EdNAC condClone = ruleClone.createNAC(cond.getBasisGraph().getName(), false);
			condClone.setUndoManager(this.undoManager);
			
			this.copyGraph(cond, condClone, table);
			this.copyMorph(cond.getMorphism(), condClone.getMorphism(), table);
		}

		// copy PACs 
		Enumeration<EdPAC> pacs = r.getPACs().elements();
		while (pacs.hasMoreElements()) {
			EdPAC cond = pacs.nextElement();
			EdPAC condClone = ruleClone.createPAC(cond.getBasisGraph().getName(), false);
			condClone.setUndoManager(this.undoManager);

			this.copyGraph(cond, condClone, table);
			this.copyMorph(cond.getMorphism(), condClone.getMorphism(), table);
		}
		
		Enumeration<EdPAC> gacs = r.getNestedACs().elements();
		while (gacs.hasMoreElements()) {
			EdPAC cond = gacs.nextElement();
			EdPAC condClone = ruleClone.createNestedAC(cond.getBasisGraph().getName(), false);
			condClone.setUndoManager(this.undoManager);

			this.copyGraph(cond, condClone, table);
			this.copyMorph(cond.getMorphism(), condClone.getMorphism(), table);
		}
		
		// copy attr conditions
		CondTuple act = (CondTuple) ac.getConditions();
		CondTuple actOther = (CondTuple) acOther.getConditions();
		for (int i = 0; i < actOther.getSize(); i++) {
			CondMember condOther = actOther.getCondMemberAt(i);
			CondMember cond = (CondMember) act.addCondition(condOther
					.getExprAsText());
			cond.setMark(condOther.getMark());
		}

		ruleClone.setGraGra(this);
		ruleClone.update();
		ruleClone.setUndoManager(this.undoManager);

		return ruleClone;
	}

	private void copyGraph(EdGraph from, EdGraph to,
			Hashtable<GraphObject, GraphObject> table) {
		// copy nodes
		Iterator<Node> nodes = from.getBasisGraph().getNodesSet().iterator();
		while (nodes.hasNext()) {
			Node bn1 = nodes.next();
			Node bn2 = null;
			try {
				bn2 = to.getBasisGraph().copyNode(bn1);
				table.put(bn1, bn2);
				// add a new EdNode and copy x,y position
				EdNode n1 = from.findNode(bn1);
				EdNode n2 = to.addNode(bn2, n1.getType());
				n2.setXY(n1.getX(), n1.getY());
				n2.getLNode().setFrozenByDefault(true);
			} catch (TypeException e) {
				e.printStackTrace();
			}
		}
		// copy arcs
		Iterator<Arc> arcs = from.getBasisGraph().getArcsSet().iterator();
		while (arcs.hasNext()) {
			Arc ba1 = arcs.next();
			Node src = (Node) table.get(ba1.getSource());
			Node tar = (Node) table.get(ba1.getTarget());
			Arc ba2 = null;
			try {
				ba2 = to.getBasisGraph().copyArc(ba1, src, tar);
				table.put(ba1, ba2);
				// add a new EdArc
				EdArc a1 = from.findArc(ba1);
				EdArc a2 = to.addArc(ba2, a1.getType());
				if (a2 != null) {
					if (a1.isLine()) {
						if (a1.hasAnchor())
							a2.setAnchor(new Point(a1.getAnchor()));
					} else {
						if (a1.hasAnchor()) {
							a2.setAnchor(Loop.UPPER_LEFT, 
										new Point(a1.getX(), a1.getY()));
							a2.setWidth(a1.getWidth());
							a2.setHeight(a1.getHeight());
						}
					}
					a2.setTextOffset(a1.getTextOffset().x, a1.getTextOffset().y);
					a2.getLArc().setFrozenByDefault(true);
				}
			} catch (TypeException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void copyMorph(OrdinaryMorphism from, OrdinaryMorphism to,
			Hashtable<GraphObject, GraphObject> table) {
		
		Iterator<GraphObject> dom = from.getDomainObjects().iterator();
		while (dom.hasNext()) {
			GraphObject lgo = dom.next();
			GraphObject rgo = from.getImage(lgo);
			GraphObject lgo1 = table.get(lgo);
			GraphObject rgo1 = table.get(rgo);
			try {
				to.addMapping(lgo1, rgo1);
			} catch (agg.xt_basis.BadMappingException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Sort rules by priority.
	 */
	public void sortRulesByPriority() {
		final Vector<EdRule> v = new Vector<EdRule>(this.eRules.size());
		v.addAll(this.eRules);
		this.eRules.removeAllElements();
		this.bGraGra.sortRulesByPriority();
		List<Rule> rules = this.bGraGra.getListOfRules();
		for (int i = 0; i < rules.size(); i++) {
			Rule r = rules.get(i);
			for (int j = 0; j < v.size(); j++) {
				EdRule er = v.elementAt(j);
				if (er.getBasisRule().equals(r)) {
					this.eRules.addElement(er);
					break;
				}
			}
		}
		v.removeAllElements();
	}

	/**
	 * Sort rules by layer.
	 */
	public void sortRulesByLayer() {
		Vector<EdRule> v = new Vector<EdRule>(this.eRules.size());
		v.addAll(this.eRules);		
		this.eRules.removeAllElements();
		this.bGraGra.sortRulesByLayer();
		List<Rule> rules = this.bGraGra.getListOfRules();
		for (int i = 0; i < rules.size(); i++) {
			Rule r = rules.get(i);
			for (int j = 0; j < v.size(); j++) {
				EdRule er = v.elementAt(j);
				if (er.getBasisRule().equals(r)) {
					this.eRules.addElement(er);
					break;
				}
			}
		}
		v.removeAllElements();
	}

	/**
	 * Sort constraints (formulae) by layer.
	 */
	@SuppressWarnings("deprecation")
	public void sortConstraintsByLayer() {
		Vector<EdConstraint> v = new Vector<EdConstraint>(this.eConstraints.size());
		v.addAll(this.eConstraints);
		this.eConstraints.removeAllElements();
		this.bGraGra.sortConstraintsByLayer();
		Vector<Formula> constraints = this.bGraGra.getConstraintsVec();
		for (int i = 0; i < constraints.size(); i++) {
			Formula f = constraints.elementAt(i);
			for (int j = 0; j < v.size(); j++) {
				EdConstraint c = v.elementAt(j);
				if (c.getBasisConstraint().equals(f)) {
					this.eConstraints.addElement(c);
					break;
				}
			}
		}
		v.removeAllElements();
	}

	/**
	 * Sort constraints (formulae) by layer.
	 */
	@SuppressWarnings("deprecation")
	public void sortConstraintsByPriority() {
		Vector<EdConstraint> v = new Vector<EdConstraint>(this.eConstraints.size());
		v.addAll(this.eConstraints);		
		this.eConstraints.removeAllElements();
		this.bGraGra.sortConstraintsByPriority();
		Vector<Formula> constraints = this.bGraGra.getConstraintsVec();
		for (int i = 0; i < constraints.size(); i++) {
			Formula f = constraints.elementAt(i);
			for (int j = 0; j < v.size(); j++) {
				EdConstraint c = v.elementAt(j);
				if (c.getBasisConstraint().equals(f)) {
					this.eConstraints.addElement(c);
					break;
				}
			}
		}
		v.removeAllElements();
	}

	public EdAtomic createAtomic(final String atomicName) {
		if (atomicName != null) {
			final EdAtomic a = new EdAtomic(this.bGraGra.createAtomic(atomicName), this.typeSet, atomicName);
			this.eAtomics.addElement(a);
			
			a.getLeft().setEditable(this.eGraph.isEditable());
			a.getRight().setEditable(this.eGraph.isEditable());
			
			a.setGraGra(this);
			a.setUndoManager(this.undoManager);
			
			this.isChanged = true;
			return a;
		} 
		return null;
	}

	public boolean addAtomic(EdAtomic c) {
		if (c.getTypeSet().getBasisTypeSet().compareTo(
				this.typeSet.getBasisTypeSet())) {
			if (this.bGraGra.addAtomic(c.getBasisAtomic())) {
				Vector<Type> v = c.getBasisAtomic().getUsedTypes();
				this.bGraGra.getTypeSet().adaptTypes(v.elements(), false);
				this.typeSet.refreshTypes();
				c.update();
				
				c.setGraGra(this);
				c.setUndoManager(this.undoManager);
				
				c.getLeft().setEditable(this.eGraph.isEditable());
				c.getRight().setEditable(this.eGraph.isEditable());
				
				this.eAtomics.addElement(c);
				this.isChanged = true;
				return true;
			} 
			return false;
		} 
		return false;
	}

	public EdConstraint createConstraint(String constrName) {
		EdConstraint e = null;
		if (constrName != null) {
			e = new EdConstraint(this.bGraGra.createConstraint(constrName), constrName);
			e.setVarSet(getAtomicsAsEvaluable(), getAtomicNames());
			e.setGraGra(this);
			this.eConstraints.addElement(e);
			this.isChanged = true;
		}
		return e;
	}

	public boolean addConstraint(EdConstraint c) {
		if (this.bGraGra.addConstraint(c.getBasisConstraint())) {
			c.setVarSet(getAtomicsAsEvaluable(), getAtomicNames());
			c.setGraGra(this);
			this.eConstraints.addElement(c);
			this.isChanged = true;
			return true;
		} 
		return false;
	}

	public EdGraph createTypeGraph() {
		this.bGraGra.createTypeGraph();
		EdGraph tg = this.typeSet.createTypeGraph();
		tg.setGraGra(this);
		tg.setUndoManager(this.undoManager);
		return tg;
	}
	
	public void setTypeGraph(EdGraph tg) {
		this.typeSet.setTypeGraph(tg);
		tg.setGraGra(this);
		tg.setUndoManager(this.undoManager);
	}
	
	public boolean createTypeGraphFrom(EdGraph g) {
		this.bGraGra.createTypeGraph();
		this.getTypeSet().createTypeGraph();	
		if (((agg.xt_basis.TypeGraph)this.getTypeSet().getTypeGraph()
				.getBasisGraph()).makeFromPlainGraph(g.getBasisGraph())) {
			this.bGraGra.getTypeSet().setLevelOfTypeGraph(TypeSet.ENABLED);
			this.getTypeGraph().doDefaultEvolutionaryGraphLayout(20);
			return true;
		}
		return false;
	}
	
	/**
	 * Creates a graph layout of type EdGraph for each Graph from the
	 * Enumeration basisGraphs.
	 * 
	 * @deprecated  replaced by <code>createGraphs(List<Graph> basisGraphs)</code>
	 */
	public Vector<EdGraph> createGraphs(Enumeration<Graph> basisGraphs) {
		while (basisGraphs.hasMoreElements()) {
			EdGraph g = new EdGraph(basisGraphs.nextElement(), this.typeSet);
			g.setGraGra(this);
			this.eGraphs.add(g);
		}
		return this.eGraphs;
	}

	public Vector<EdGraph> createGraphs(List<Graph> basisGraphs) {
		Iterator<Graph> graphIt = basisGraphs.iterator();
		while (graphIt.hasNext()) {
			EdGraph g = new EdGraph(graphIt.next(), this.typeSet);
			g.setGraGra(this);
			this.eGraphs.add(g);
		}
		return this.eGraphs;
	}
	
	/**
	 * Creates a rule layout of type EdRule for each Rule from the Enumeration
	 * basisRules.
	 */
	public Vector<EdRule> createRules(final List<Rule> basisRules) {
//		long time0 = System.currentTimeMillis();
		for (int i=0; i<basisRules.size(); i++) {
			final Rule basisRule = basisRules.get(i);
			EdRule r = null;
			if (basisRule instanceof RuleScheme) {
				r = new EdRuleScheme((RuleScheme) basisRule, this.typeSet);
			} else {
				r = new EdRule(basisRules.get(i), this.typeSet);
			}
			r.setGraGra(this);
			r.getLeft().setEditable(this.eGraph.isEditable());
			r.getRight().setEditable(this.eGraph.isEditable());
			r.setEditable(true);
			this.eRules.addElement(r);
		}
//		System.out.println("(Layouted) Grammar  create Rules: "
//				+ (System.currentTimeMillis() - time0) + "ms");
		return this.eRules;
	}

	/**
	 * Creates a layout of type EdAtomic for each atomic graph constraint
	 * AtomConstraint from the Enumeration basisAtomics.
	 * 
	 * @deprecated replaced by <code>createConstraints(List<Formula> formulae)</code>
	 */
	public Vector<EdAtomic> createAtomics(Enumeration<AtomConstraint> basisAtomics) {
		while (basisAtomics.hasMoreElements()) {
			EdAtomic a = new EdAtomic(basisAtomics.nextElement(), this.typeSet, "");
			a.setGraGra(this);
			a.getLeft().setEditable(this.eGraph.isEditable());
			a.getRight().setEditable(this.eGraph.isEditable());
			this.eAtomics.addElement(a);
		}
		return this.eAtomics;
	}

	/**
	 * Creates a layout of type EdAtomic for each atomic graph constraint
	 * AtomConstraint from the Enumeration basisAtomics.
	 */
	public List<EdAtomic> createAtomics(List<AtomConstraint> basisAtomics) {
		Iterator<AtomConstraint> basisAtomicsIter = basisAtomics.iterator();
		while (basisAtomicsIter.hasNext()) {
			EdAtomic a = new EdAtomic(basisAtomicsIter.next(), this.typeSet, "");
			a.setGraGra(this);
			a.getLeft().setEditable(this.eGraph.isEditable());
			a.getRight().setEditable(this.eGraph.isEditable());
			this.eAtomics.addElement(a);
		}
		return this.eAtomics;
	}
	
	/**
	 * Creates a presentation of type EdConstraint for each Formula from the
	 * Enumeration formulae.
	 * @deprecated  replaced by <code>createConstraints(List<Formula> formulae)</code>
	 */
	public Vector<EdConstraint> createConstraints(Enumeration<Formula> formulae) {
		while (formulae.hasMoreElements()) {
			Formula f = formulae.nextElement();
			EdConstraint c = new EdConstraint(f, f.getName()); 
			c.setGraGra(this);
			this.eConstraints.addElement(c);
		}
		return this.eConstraints;
	}

	/**
	 * Creates a presentation of type EdConstraint for each Formula from the
	 * Enumeration formulae.
	 */
	public List<EdConstraint> createConstraints(List<Formula> formulae) {
		Iterator<Formula> formulaeIter = formulae.iterator();
		while (formulaeIter.hasNext()) {
			Formula f = formulaeIter.next();
			EdConstraint c = new EdConstraint(f, f.getName()); 
			c.setGraGra(this);
			this.eConstraints.addElement(c);
		}
		return this.eConstraints;
	}
	
	public int getIndexOfRule(EdRule r) {
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule er = this.eRules.get(i);
			if (er == r) {
				return i;
			}
		}
		return -1;
	}
	
	/** Returns a rule layout specified by the index int i */
	public EdRule getRule(int i) {
		return this.eRules.elementAt(i);
	}

	public EdRule getRule(String rname) {
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule er = this.eRules.get(i);
			if (er.getBasisRule().getRuleScheme() == null) {
				if (er.getBasisRule().getName().equals(rname)) {
					return er;
				}
			} else {
				Rule r = er.getBasisRule().getRuleScheme().getRule(rname);
				if (r != null) {
					er = this.getRuleScheme(er.getBasisRule()
							.getRuleScheme()).getRule(rname);
					if (er != null)
						return er;					
				}				
			}
		}
		return null;
	}
	
	public EdRule getRule(Rule r) {
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule er = this.eRules.get(i);
			if (er.getBasisRule() == r) {
				return er;
			}
		}
		return null;
	}

	public EdRule getRule(Graph g) {
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule er = this.eRules.get(i);
			if (er.getBasisRule().getLeft() == g
					|| er.getBasisRule().getRight() == g) {
				return er;
			} else if (er.getBasisRule() instanceof RuleScheme) {
				EdRuleScheme rs = (EdRuleScheme) this.getRule(er.getBasisRule());
				EdRule r = rs.getRule(g);
				if (r != null)
					return r;
			}
		}
		return null;
	}
	
	public EdRuleScheme getRuleScheme(Graph g) {
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule er = this.eRules.get(i);
			if (er.getBasisRule() instanceof RuleScheme) {
				EdRuleScheme rs = (EdRuleScheme) this.getRule(er.getBasisRule());
				if (rs.getRule(g) != null)
					return rs;
			}
		}
		return null;
	}
	
	public EdRuleScheme getRuleScheme(Rule r) {
		for (int i = 0; i < this.eRules.size(); i++) {
			final EdRule er = this.eRules.get(i);
			if (er instanceof EdRuleScheme
					&& r.getRuleScheme() != null
					&& er.getBasisRule().getRuleScheme() == r.getRuleScheme()) {
				return (EdRuleScheme) er;				
			}
		}
		return null;
	}
	
	public boolean isRuleAnimated(Rule r) {
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule er = this.eRules.get(i);
			if (r instanceof AmalgamatedRule) {
				return false;
			}
			else if (er.getBasisRule().equals(r)
					&& er.isAnimated()) {
				return true;
			}
		}
		return false;
	}
	
	public EdAtomic getAtomic(int i) {
		return this.eAtomics.elementAt(i);
	}

	public EdAtomic getAtomic(String name) {
		for (int i = 0; i < this.eAtomics.size(); i++) {
			EdAtomic ac = this.eAtomics.get(i);
			if (ac.getBasisAtomic().getAtomicName().equals(name))
				return ac;
		}
		return null;
	}
	
	public EdConstraint getConstraint(int i) {
		return this.eConstraints.elementAt(i);
	}

	/**
	 * Destroys the type graph. The used basis type graph is destroyed, too.
	 */
	public void removeTypeGraph() {
		if (this.bGraGra == null)
			return;
		if (this.typeSet.getTypeGraph() == null)
			return;
		this.typeSet.removeTypeGraph();
		this.bGraGra.removeTypeGraph();
	}

	/**
	 * Destroys the type graph. The used basis type graph is destroyed, too.
	 */
	public void destroyTypeGraph() {
		if (this.bGraGra == null)
			return;
		if (this.typeSet.getTypeGraph() == null)
			return;
		this.bGraGra.destroyTypeGraph();
	}

	public EdGraph getGraphOf(Graph basisGraph) {
		for (int i = 0; i < this.eGraphs.size(); i++) {
			EdGraph eg = this.eGraphs.get(i);
			if (eg.getBasisGraph() == basisGraph)
				return eg;
		}
		return null;
	}

	/*
	 * There are searched: its host graphs, rules and nacs, atomic graph
	 * constraints, type graph.
	 */
	public EdGraph getGraphOf(EdGraphObject go) {
		// search host graphs
		for (int i = 0; i < this.eGraphs.size(); i++) {
			EdGraph g = this.eGraphs.get(i);
			if (g.getBasisGraph() == go.getBasisObject().getContext())
				return g;
		}
		// search rules
		Enumeration<EdRule> rules = this.eRules.elements();
		while (rules.hasMoreElements()) {
			EdRule r = rules.nextElement();
			if (r.getLeft().getBasisGraph() == go.getBasisObject().getContext())
				return r.getLeft();
			else if (r.getRight().getBasisGraph() == go.getBasisObject()
					.getContext())
				return r.getRight();
			else if (!r.getNACs().isEmpty()) {
				// search NACs
				for (int j = 0; j < r.getNACs().size(); j++) {
					EdNAC nac = r.getNACs().get(j);
					if (nac.getBasisGraph() == go.getBasisObject().getContext())
						return nac;
				}
			}
		}
		// search atomic graph constraints
		Enumeration<EdAtomic> atomics = this.eAtomics.elements();
		while (atomics.hasMoreElements()) {
			EdAtomic a = atomics.nextElement();
			Vector<EdAtomic> conclusions = a.getConclusions();
			if (!conclusions.isEmpty()) {
				EdAtomic c = conclusions.elementAt(0);
				// premise
				if (c.getLeft().getBasisGraph() == go.getBasisObject()
						.getContext())
					return c.getLeft();
				// conclusions
				for (int j = 0; j < conclusions.size(); j++) {
					c = conclusions.elementAt(j);
					if (c.getRight().getBasisGraph() == go.getBasisObject()
							.getContext())
						return c.getRight();
				}
			}
		}

		if (this.startGraph != null) {
			if (this.startGraph.getBasisGraph() == go.getBasisObject().getContext())
				return this.startGraph;
		}

		if (this.typeSet.getTypeGraph() != null) {
			if (this.typeSet.getTypeGraph().getBasisGraph() == go.getBasisObject()
					.getContext())
				return this.typeSet.getTypeGraph();
		}
		return null;
	}

	private boolean containsGraph(EdGraph g) {
		if (g.getBasisGraph() == null)
			return false;
		if (this.bGraGra.isElement(g.getBasisGraph()))
			return true;
		else if (this.bGraGra.getTypeGraph() == g.getBasisGraph())
			return true;

		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule r = this.eRules.get(i);
			if (r.getBasisRule().getRuleScheme() == null) {
				if (r.getBasisRule().isElement(g.getBasisGraph()))
					return true;
			} else {
				if (r.getBasisRule().getRuleScheme().isElement(g.getBasisGraph()))
					return true;
			}
		}

		for (int i = 0; i < this.eAtomics.size(); i++) {
			EdAtomic a = this.eAtomics.get(i);
			if (a.getBasisAtomic().isElement(g.getBasisGraph()))
				return true;
		}
		return false;
	}

	public Vector<EdGraphObject> getGraphObjectsOfType(EdType t,
			boolean fromTypeGraph) {
		Vector<EdGraphObject> vec = new Vector<EdGraphObject>();
		List<EdGraphObject> users = this.typeSet.getTypeUsers(t);
		for (int i = 0; i < users.size(); i++) {
			EdGraphObject go = users.get(i);
			EdGraph g = go.getContext();
			if (g != null) {
				if (g.isTypeGraph() && fromTypeGraph && g == getTypeGraph()) {
					vec.add(go);
				} else if (this.containsGraph(g)) {
					vec.add(go);
				} else if (this.startGraph == g) {
					vec.add(go);
				}
			}
		}
		return vec;
	}
	
	public Vector<String> deleteGraphObjectsOfTypeFromRule(
			final EdType t,
			final EdRule r,
			boolean addToUndo) {
		Vector<String> failed = new Vector<String>(5);
		if (r.getBasisRule().getRuleScheme() == null) {
			if (!r.deleteGraphObjectsOfType(t, addToUndo)) {
				if (failed.isEmpty())
					failed.add(r.getName());
			}
		} else {
			if (!((EdRuleScheme)r).deleteGraphObjectsOfType(t, addToUndo)) {
				if (failed.isEmpty())
					failed.add(r.getName());
			}
		}
		return failed;
	}
	
	public Vector<String> deleteGraphObjectsOfTypeFromRule(
			final EdGraphObject tgo,
			final EdRule r,
			boolean addToUndo) {
		Vector<String> failed = new Vector<String>(5);
		if (r.getBasisRule().getRuleScheme() == null) {
			if (!r.deleteGraphObjectsOfType(tgo, addToUndo)) {
				if (failed.isEmpty())
					failed.add(r.getName());
			}
		} else {
			if (!((EdRuleScheme)r).deleteGraphObjectsOfType(tgo, addToUndo)) {
				if (failed.isEmpty())
					failed.add(r.getName());
			}
		}
		return failed;
	}

	public Vector<String> deleteGraphObjectsOfTypeFromAtomicGraphConstraint(
			final EdType t,
			final EdAtomic a,
			boolean addToUndo) {
		Vector<String> failed = new Vector<String>(5);
		
		List<EdAtomic> conclusions = a.getConclusions();
		for (int i=0; i<conclusions.size(); i++) {
			EdAtomic c = conclusions.get(i);
			
			List<EdGraphObject> list = c.getLeft().getGraphObjectsOfType(t);
			for (int j=0; j<list.size(); j++) {
				EdGraphObject go = list.get(j);
				GraphObject img = c.getMorphism().getImage(go.getBasisObject());
				if (img != null) {
					EdGraphObject imggo = c.getRight().findGraphObject(img);
					if (imggo != null)
						c.addDeletedMappingToUndo(go, imggo);
				}
			}
			
			if (!c.getRight().deleteGraphObjectsOfTypeFromGraph(t, addToUndo)) {
				failed.add(c.getName());
			}
		}
		
		if (!a.getLeft().deleteGraphObjectsOfTypeFromGraph(t, addToUndo)) {
			failed.add(a.getBasisAtomic().getAtomicName());		
		}

		return failed;
	}
	
	public Vector<String> deleteGraphObjectsOfTypeFromAtomicGraphConstraint(
			final EdGraphObject tgo,
			final EdAtomic a,
			boolean addToUndo) {
		Vector<String> failed = new Vector<String>(5);
		
		List<EdAtomic> conclusions = a.getConclusions();
		for (int i=0; i<conclusions.size(); i++) {
			EdAtomic c = conclusions.get(i);
			
			List<EdGraphObject> list = c.getLeft().getGraphObjectsOfType(tgo);
			for (int j=0; j<list.size(); j++) {
				EdGraphObject go = list.get(j);
				GraphObject img = c.getMorphism().getImage(go.getBasisObject());
				if (img != null) {
					EdGraphObject imggo = c.getRight().findGraphObject(img);
					if (imggo != null)
						c.addDeletedMappingToUndo(go, imggo);
				}
			}
			
			if (!c.getRight().deleteGraphObjectsOfTypeFromGraph(tgo, addToUndo)) {
				failed.add(c.getName());
			}
		}
		
		if (!a.getLeft().deleteGraphObjectsOfTypeFromGraph(tgo, addToUndo)) {
			failed.add(a.getBasisAtomic().getAtomicName());		
		}
		
		return failed;
	}
	
	

	/**
	 * Deletes all graph objects of the specified type inside the current 
	 * host graph of this gragra. 
	 */
	public boolean deleteGraphObjectsOfTypeFromHostGraph(
			final EdGraphObject tgo,
			boolean addToUndo) {
		
		return this.getGraph().deleteGraphObjectsOfTypeFromGraph(tgo, addToUndo);
	}
	
	/**
	 * Checks, whether the kernel rule of a rule scheme uses the given type 
	 *
	 * @return the name of the rule <code>RuleScheme.KernelRule</code> or null
	 */
	public String kernelRuleContainsObjsOfType(final EdGraphObject tgo) {
		// check, if the kernel rule of a rule scheme uses the given type 
		for (int i=0; i<this.eRules.size(); i++) {	
			EdRule r = this.eRules.get(i);
			if (r instanceof EdRuleScheme) {
				EdRule kernRule = ((EdRuleScheme)r).getKernelRule();
				if (kernRule.getLeft().getGraphObjectsOfType(tgo).size() > 0
						|| kernRule.getRight().getGraphObjectsOfType(tgo).size() > 0) {
					String failed = ((EdRuleScheme)r).getName()+"."+kernRule.getName();					
					return failed;
				}
			}
		}

		return null;
	}
	
	/**
	 * Checks, whether the kernel rule of a rule scheme uses the given type 
	 *
	 * @return the name of the rule <code>RuleScheme.KernelRule</code> or null
	 */
	public String kernelRuleContainsObjsOfType(final EdType t) {
		// check, if the kernel rule of a rule scheme uses the given type 
		for (int i=0; i<this.eRules.size(); i++) {	
			EdRule r = this.eRules.get(i);
			if (r instanceof EdRuleScheme) {
				EdRule kernRule = ((EdRuleScheme)r).getKernelRule();
				if (kernRule.getLeft().getGraphObjectsOfType(t).size() > 0
						|| kernRule.getRight().getGraphObjectsOfType(t).size() > 0) {
					String failed = ((EdRuleScheme)r).getName()+"."+kernRule.getName();					
					return failed;
				}
			}
		}

		return null;
	}
	
	/**
	 * Deletes all graph objects of the specified type graph object inside all graphs of this
	 * gragra. If parameter <code>alsoFromTypeGraph</code> is
	 * <code>true</code>, the appropriate type object inside of the type
	 * graph is deleted, too. Returns a vector with names of graphs in which the
	 * deletion failed, otherwise - empty list.
	 */
	public List<String> deleteGraphObjectsOfType(
			final EdGraphObject tgo,
			boolean fromTypeGraph,
			boolean addToUndo) {
		
//		List<String> failed = this.typeSet.removeTypeUsers(tgo, addToUndo);	
//		// delete from type graph
//		if (this.typeSet.getTypeGraph() != null 
//				&& fromTypeGraph
//				&& !this.typeSet.getTypeGraph().deleteGraphObjectsOfTypeFromGraph(tgo, addToUndo)) {
//			failed.add(this.typeSet.getTypeGraph().getName());
//		}
		
		Vector<String> failed = new Vector<String>(5);
		for (int i=0; i<this.eRules.size(); i++) {	
			EdRule r = this.eRules.get(i);
			failed.addAll(deleteGraphObjectsOfTypeFromRule(tgo, r, addToUndo));
		}
		
		// delete from host graphs
		for (int i = 0; i < this.eGraphs.size(); i++) {
			EdGraph g = this.eGraphs.get(i);
			if (!g.deleteGraphObjectsOfTypeFromGraph(tgo, addToUndo)) {
				failed.add(g.getName());
			}
		}
		if (this.startGraph != null) {
			if (!this.startGraph.deleteGraphObjectsOfTypeFromGraph(tgo, false)) {
				failed.add(this.startGraph.getName()+"(StartG)");
			}
		}
				
		// delete from atomic graph constraints
		for (int i=0; i<this.eAtomics.size(); i++) {
			EdAtomic a = this.eAtomics.get(i);
			failed.addAll(deleteGraphObjectsOfTypeFromAtomicGraphConstraint(tgo, a, addToUndo));
		}
		
		// delete from type graph
		if (this.typeSet.getTypeGraph() != null 
				&& fromTypeGraph
				&& !this.typeSet.getTypeGraph().deleteGraphObjectsOfTypeFromGraph(tgo, addToUndo)) {
			failed.add(this.typeSet.getTypeGraph().getName());
		}
		
		return failed;
	}
	
	/**
	 * Deletes all graph objects of the specified type inside all graphs of this
	 * gragra. If parameter <code>alsoFromTypeGraph</code> is
	 * <code>true</code>, the appropriate type object inside of the type
	 * graph is deleted, too. Returns a vector with names of graphs in which the
	 * deletion failed, otherwise - empty list.
	 */
	public List<String> deleteGraphObjectsOfType(
			final EdType t,
			boolean fromTypeGraph,
			boolean addToUndo) {		
		
		Vector<String> failed = new Vector<String>(5);
		
		for (int i=0; i<this.eRules.size(); i++) {	
			EdRule r = this.eRules.get(i);
			failed.addAll(deleteGraphObjectsOfTypeFromRule(t, r, addToUndo));
		}
		
		// delete from host graphs
		for (int i = 0; i < this.eGraphs.size(); i++) {
			EdGraph g = this.eGraphs.get(i);
			if (!g.deleteGraphObjectsOfTypeFromGraph(t, addToUndo)) {
				failed.add(g.getName());
			}
		}
		if (this.startGraph != null) {
			if (!this.startGraph.deleteGraphObjectsOfTypeFromGraph(t, false)) {
				failed.add(this.startGraph.getName()+"(StartG)");
			}
		}
				
		// delete from atomic graph constraints
		for (int i=0; i<this.eAtomics.size(); i++) {
			EdAtomic a = this.eAtomics.get(i);
			failed.addAll(deleteGraphObjectsOfTypeFromAtomicGraphConstraint(t, a, addToUndo));
		}
		
		// delete from type graph
		if (this.typeSet.getTypeGraph() != null 
				&& fromTypeGraph
				&& !this.typeSet.getTypeGraph().deleteGraphObjectsOfTypeFromGraph(t, addToUndo)) {
			failed.add(this.typeSet.getTypeGraph().getName());
		}
		
		return failed;
	}
	


	/** Destroys the specified match. */
	public void destroyAllMatches() {
		this.bGraGra.destroyAllMatches();
	}

	/**
	 * Removes the specified by the EdRule er.
	 */
	public void removeRule(EdRule er) {
		removeRule(er, false);
	}

	/**
	 * Removes the specified EdRule er. If the parameter dispose is true, the
	 * used basis rule is destroyed.
	 */
	public boolean removeRule(EdRule er, boolean dispose) {
		if (this.bGraGra == null)
			return false;
		if (er != null) {
			if (er.getBasisRule() != null) {
				if (this.bGraGra.removeRule(er.getBasisRule())) {
					this.eRules.removeElement(er);
					if (dispose) {
						er.dispose();
						this.bGraGra.destroyRule(er.getBasisRule());
						er.unsetBasisRule();
					}
					this.isChanged = true;
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Destroys the specified EdRule er.
	 */
	public void destroyRule(EdRule er) {
		if (this.bGraGra == null)
			return;
		if (er != null) {
			if (er.getBasisRule() != null) {
				this.eRules.removeElement(er);
				this.bGraGra.removeRule(er.getBasisRule());
				er.dispose();
				this.bGraGra.destroyRule(er.getBasisRule());
				er.unsetBasisRule();
				this.isChanged = true;
			}
		}
	}

	/** Removes all rules. */
	public void removeRules() {
		while (this.eRules.size() != 0) {
			removeRule(this.eRules.elementAt(0));
		}
	}

	/**
	 * Removes an atomic constraint layout specified by the EdAtomic atom. The
	 * used object will be destroyed.
	 */
	public boolean removeAtomic(EdAtomic atom) {
		if (this.bGraGra == null)
			return false;
		if (atom == null)
			return false;
		if (atom.getMorphism() != null) {
			this.bGraGra.removeAtomic(atom.getBasisAtomic());
			this.eAtomics.removeElement(atom);
			this.isChanged = true;
			return true;
		} 
		return false;
	}

	/**
	 * Destroys the specified atomic graph constraint EdAtomic atom.
	 */
	public void destroyAtomic(EdAtomic atom) {
		if (this.bGraGra == null)
			return;
		if (atom == null)
			return;
		if (atom.getMorphism() != null) {
			if (this.eAtomics.contains(atom))
				this.eAtomics.removeElement(atom);
			this.bGraGra.destroyAtomic(atom.getBasisAtomic());
			this.isChanged = true;
		}
	}

	/** Removes all atomic constraints. */
	public void removeAtomics() {
		while (this.eAtomics.size() != 0) {
			removeAtomic(this.eAtomics.elementAt(0));
		}
	}

	/**
	 * Destroys the specified graph constraint (formula) EdConstraint constr.
	 */
	public void destroyConstraint(EdConstraint constr) {
		if (this.bGraGra == null)
			return;
		if (constr == null)
			return;
		if (constr.getBasisConstraint() != null) {
			this.eConstraints.removeElement(constr);
			this.bGraGra.destroyConstraint(constr.getBasisConstraint());
			this.isChanged = true;
		}
	}

	/**
	 * Removes a constraint layout specified by the EdConstraint constr.
	 */
	public boolean removeConstraint(EdConstraint constr) {
		if (this.bGraGra == null)
			return false;
		if (constr == null)
			return false;
		if (constr.getBasisConstraint() != null) {
			this.bGraGra.destroyConstraint(constr.getBasisConstraint());
			this.eConstraints.removeElement(constr);
			this.isChanged = true;
			return true;
		} 
		return false;
	}

	/** Removes all constraints. */
	public void removeConstraints() {
		while (this.eConstraints.size() != 0) {
			removeConstraint(this.eConstraints.elementAt(0));
		}
	}

	/** Deselects all selected objects, removes all matches */
	public void clear() {
		if (this.typeSet.getTypeGraph() != null)
			this.typeSet.getTypeGraph().clearSelected();

		for (int i = 0; i < this.eGraphs.size(); i++) {
			EdGraph g = this.eGraphs.elementAt(i);
			g.clearSelected();
		}

		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule r = this.eRules.elementAt(i);
			if (r != null) {
				r.destroyMatch();
				// r.setMatch(null);
				r.update();
				r.getLeft().clearSelected();
				r.getRight().clearSelected();

				for (int j = 0; j < r.getNACs().size(); j++)
					r.getNACs().elementAt(j).clearSelected();
			}
		}
		this.bGraGra.destroyAllMatches();

		for (int i = 0; i < this.eAtomics.size(); i++) {
			EdAtomic a = this.eAtomics.elementAt(i);
			a.getLeft().clearSelected();
			a.getRight().clearSelected();
		}
	}

	/**
	 * Set visibility of the objects of the specified type.
	 */
	public void setVisibilityOfGraphObjectsOfType(EdType type, boolean vis) {
		if (this.getTypeGraph() != null) {
			int oldhidden = this.getTypeGraph().hidden;
			if (type.isNodeType()) {
				// set visibility of the node type				
				List<EdNode> list = new Vector<EdNode>();
				
				// set visibility of the node type and all its children 
				for (int i = 0; i < this.getTypeGraph().nodes.size(); i++) { 
					EdNode n = this.getTypeGraph().nodes.get(i);
					if (type.getBasisType().isParentOf(n.getType().getBasisType())) {
						n.getType().getBasisType().setVisibilityOfObjectsOfTypeGraphNode(vis);
						this.getTypeGraph().hidden = vis ? this.getTypeGraph().hidden-1 
															: this.getTypeGraph().hidden+1;
						list.add(n);
					}
				}
				// set visibility of all in- out- arcs 
				for (int j = 0; j < this.getTypeGraph().arcs.size(); j++) { 
					EdArc a = this.getTypeGraph().arcs.get(j);	
					if (list.contains(a.getSource())
							|| list.contains(a.getTarget())) {
						a.getType().getBasisType().setVisibityOfObjectsOfTypeGraphArc(
								a.getSource().getType().getBasisType(),
								a.getTarget().getType().getBasisType(),
								vis);
						this.getTypeGraph().hidden = vis ? this.getTypeGraph().hidden-1 
															: this.getTypeGraph().hidden+1;
					}							
				}
			} else {
				// set visibility of the edge type
				Vector<EdArc> edges = this.getTypeGraph().getArcs(type);
				for (int j=0; j<edges.size(); j++) {
					EdArc a = edges.get(j);
					type.getBasisType().setVisibityOfObjectsOfTypeGraphArc(
							a.getSource().getType().getBasisType(),
							a.getTarget().getType().getBasisType(),
							vis);
					this.getTypeGraph().hidden = vis ? this.getTypeGraph().hidden-1 
												: this.getTypeGraph().hidden+1;
				}
			}
					
			// unset visibilityChecked of all complete graphs
			List<EdGraph> graphs = this.getGraphs();
			for (int i=0; i<graphs.size(); i++) {				
				graphs.get(i).visibilityChecked = (oldhidden == this.getTypeGraph().hidden);				
			}			
		}
	}
	
	/*
	 * Updates layout of all graph morphisms like rule, NAC, PAC,
	 * atomic graph constraint.
	 */
//	public synchronized void updateGraphMorphisms() {
//		// update rule with NACs, PACs
//		for (int i = 0; i < eRules.size(); i++) {
//			EdRule er = eRules.elementAt(i);
//			er.updateRule();
//			er.updateNAC();
//			er.updatePAC();
//		}
//
//		// update graph consistency constraints
//		for (int i = 0; i < eAtomics.size(); i++) {
//			EdAtomic a = eAtomics.elementAt(i);
//			a.updateRule();
//		}
//	}

//	public synchronized void updateBySelected() {
//		this.getGraph().updateBySelected();
//		
//		for (int i = 0; i < eRules.size(); i++) {			
//			EdRule er = eRules.elementAt(i);
//			er.getLeft().updateBySelected();
//			er.getRight().updateBySelected();
//		}
//	}
	
	/**
	 * Updates layout of the type graph, the host graphs, the rule graphs and
	 * constraint graphs.
	 */
	public synchronized void update() {
		// update type graph
		if (this.typeSet.getTypeGraph() != null) {
			this.typeSet.getTypeGraph().setGraGra(this);
			this.typeSet.getTypeGraph().markTypeGraph(true);
			this.typeSet.getTypeGraph().updateGraph();
		}
		// update host graphs
		// eGraph.update();
		for (int i = 0; i < this.eGraphs.size(); i++) {
			EdGraph g = this.eGraphs.get(i);
			g.setTypeSet(this.typeSet);
			g.updateGraph();
		}
		// update rules
		updateRules();
		// update graph consistency constraints
		updateConstraints();
	}

	/** Updates the graph end rule layout */
	public synchronized void update(boolean attrsVisible) {
		// update type graph
		if (this.typeSet.getTypeGraph() == null
				&& this.typeSet.getBasisTypeSet().getTypeGraph() != null)
			this.typeSet.createTypeGraph();
		if (this.typeSet.getTypeGraph() != null) {
			this.typeSet.getTypeGraph().setGraGra(this);
			this.typeSet.getTypeGraph().markTypeGraph(true);
			this.typeSet.getTypeGraph().updateGraph(true, true);
		}

		// update host graphs
		for (int i = 0; i < this.eGraphs.size(); i++) {
			EdGraph g = this.eGraphs.get(i);
			g.setTypeSet(this.typeSet);
			g.updateGraph(attrsVisible);
		}

		updateRules();
		updateConstraints();
	}

	/** Updates all the rule layout */
	public synchronized void updateRules() {
		for (int i = 0; i < this.eRules.size(); i++) {
			if (this.eRules.elementAt(i) != null) {
				EdRule er = this.eRules.elementAt(i);
				er.setTypeSet(this.typeSet);
				for (int j = 0; j < er.getNACs().size(); j++) {
					EdNAC nac = er.getNACs().elementAt(j);
					nac.setTypeSet(this.typeSet);
				}
				er.update();
			}
		}
	}

	public synchronized void updateConstraints() {
		for (int i = 0; i < this.eAtomics.size(); i++) {
			EdAtomic a = getAtomic(i);
			if (a != null) {
				a.setTypeSet(this.typeSet);
				a.update();
			}
		}
		for (int i = 0; i < this.eConstraints.size(); i++) {
			EdConstraint c = getConstraint(i);
			if (c != null) {
				c.update();
			}
		}
	}

	
	public void makeLayoutOfBasisGraphs() {
		if (this.typeSet.getTypeGraph() != null) {
			this.typeSet.getTypeGraph().layoutBasisGraph(new Dimension(800, 600));
		}
		
		for (int i = 0; i < this.eGraphs.size(); i++) {
			final EdGraph g = this.eGraphs.get(i);
			g.layoutBasisGraph(new Dimension(800, 600));
		}

		for (int i = 0; i < this.eRules.size(); i++) {
			final EdRule er = this.eRules.elementAt(i);
			er.getLeft().layoutBasisGraph(new Dimension(400, 300));
			er.getRight().layoutBasisGraph(new Dimension(400, 300));
			
			final List<EdNAC> list = er.getNACs();
			for (int j=0; j<list.size(); j++) {
				list.get(j).layoutBasisGraph(new Dimension(400, 300));
			}
		}
		
		for (int i = 0; i < this.eAtomics.size(); i++) {
			final EdAtomic a = getAtomic(i);
			a.getLeft().layoutBasisGraph(new Dimension(400, 300));
			
			final List<EdAtomic> list = a.getConclusions();
			for (int j=0; j<list.size(); j++) {
				list.get(j).getRight().layoutBasisGraph(new Dimension(400, 300));
			}
		}
	}
	
	
	// Node / Arc EdTypeSet

	/** Returns my set of layout types */
	public EdTypeSet getTypeSet() {
		return this.typeSet;
	}

	/** Sets my set of layout types specified by the EdTypeSet types */
	void setTypeSet(EdTypeSet types) {
		this.typeSet = types;
	}

	/** Returns all node types */
	public Vector<EdType> getNodeTypes() {
		return this.typeSet.getNodeTypes();
	}

	/** Sets node types. Old node types will be removed. */
	public void setNodeTypes(Vector<EdType> nTypes) {
		this.typeSet.setNodeTypes(nTypes);
	}

	/** Returns all edge types */
	public Vector<EdType> getArcTypes() {
		return this.typeSet.getArcTypes();
	}

	/** Sets edge types. Old edge types will be removed. */
	public void setArcTypes(Vector<EdType> aTypes) {
		this.typeSet.setArcTypes(aTypes);
	}

	/** Returns currently selected node type. */
	public EdType getSelectedNodeType() {
		return this.typeSet.getSelectedNodeType();
	}

	/** Selects a node type. */
	public void setSelectedNodeType(EdType et) {
		this.typeSet.setSelectedNodeType(et);
	}

	/** Returns currently selected edge type. */
	public EdType getSelectedArcType() {
		return this.typeSet.getSelectedArcType();
	}

	/** Selects an edge type. */
	public void setSelectedArcType(EdType et) {
		this.typeSet.setSelectedArcType(et);
	}

	/** Creates and adds a new node type to the type set. */
	public EdType newNodeType(String tname, int shape, Color color, 
			boolean filledShape, String iconFileName) {
		return this.typeSet.createNodeType(tname, shape, color, filledShape, iconFileName);
	}

	/** Creates and adds a new node type to the type set. */
	public EdType newNodeType(Type baseType, String tname, int shape, Color color,
			boolean filledShape, String iconFileName) {
		return this.typeSet.createNodeType(baseType, tname, shape, color, filledShape, iconFileName);
	}

	/**
	 * Creates and adds the default node layout type for the used object
	 * specified by the Type baseType.
	 */
	public EdType createDefaultNodeType(Type baseType) {
		return this.typeSet.createDefaultNodeType(baseType);
	}

	/** Creates and adds a new edge type to the type set. */
	public EdType newArcType(String tname, int shape, Color color, boolean bold) {
		return this.typeSet.createArcType(tname, shape, color, bold);
	}

	/** Creates and adds a new edge type to the type set. */
	public EdType newArcType(Type baseType, String tname, int shape, Color color, boolean bold) {
		return this.typeSet.createArcType(baseType, tname, shape, color, bold);
	}

	/**
	 * Creates and adds the default edge layout type for the used object
	 * specified by the Type baseType.
	 */
	public EdType createDefaultArcType(Type baseType) {
		return this.typeSet.createDefaultArcType(baseType);
	}

	/**
	 * Removes all the node and arc layout types, all the node and arc layouts.
	 * The used object of the graph layout , rule layouts, NAC layout, are
	 * remaining available.
	 */
	public void removeLayout() {
		this.typeSet.getNodeTypes().removeAllElements();
		this.typeSet.getArcTypes().removeAllElements();

		this.eGraph.getArcs().removeAllElements();
		this.eGraph.getNodes().removeAllElements();

		// Rules
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule r = this.eRules.elementAt(i);
			if (r != null) {
				r.getLeft().getArcs().removeAllElements();
				r.getLeft().getNodes().removeAllElements();
				r.getRight().getArcs().removeAllElements();
				r.getRight().getNodes().removeAllElements();

				// NACs
				for (int j = 0; j < r.getNACs().size(); j++) {
					EdNAC nac = r.getNACs().elementAt(j);
					nac.getArcs().removeAllElements();
					nac.getNodes().removeAllElements();
				}
			}
		}
	}

	/**
	 * This method compares instances only on the basic level :
	 * this.getBasisGraGra().compareTo(gra.getBasisGraGra()) Return TRUE if the
	 * gragras are equal otherwise FALSE.
	 */
	public boolean compareTo(EdGraGra gra) {
		if (this.bGraGra == null || gra.getBasisGraGra() == null)
			return false;
		return this.bGraGra.compareTo(gra.getBasisGraGra());
	}

	/**
	 * This method compares instances only on the basic level :
	 * this.getBasisGraGra().compareTo(gra.getBasisGraGra()). Transformation
	 * options are compared, too. Return TRUE if the gragras are equal otherwise
	 * FALSE.
	 */
	public boolean compareTo(EdGraGra gra, boolean transOption) {
		if (this.bGraGra == null || gra.getBasisGraGra() == null)
			return false;
		return this.bGraGra.compareTo(gra.getBasisGraGra(), transOption);
	}

	/**
	 * Allows to edit this grammar depending on the value of parameter b.
	 */
	public void setEditable(boolean b) {
		// type graph
		if (this.typeSet.getTypeGraph() != null)
			this.typeSet.getTypeGraph().setEditable(b);
		// work graph
		this.eGraph.setEditable(b);
		// Rules
		for (int i = 0; i < this.eRules.size(); i++) {
			EdRule r = this.eRules.elementAt(i);
			r.setEditable(b);
		}
		// atomic graph constraints
		for (int i = 0; i < this.eAtomics.size(); i++) {
			EdAtomic a = this.eAtomics.elementAt(i);
			a.setEditable(b);
		}
	}

	public boolean isEditable() {
		return this.eGraph.isEditable();
	}
	
	public void setChanged(boolean b) {
		this.isChanged = b;
	}

	public boolean isChanged() {
		return this.isChanged;
	}

	/**
	 * Returns applicable rules for the specified morphism completion strategy
	 */
	public Vector<Rule> getApplicableRules(MorphCompletionStrategy strategy) {
		boolean changed = this.isChanged;
		Vector<Rule> applicableRules = null;
		
		applicableRules = this.bGraGra.getApplicableRules(strategy);
		
		this.isChanged = changed;
		return applicableRules;
	}

	public void dismissRuleApplicability() {
		this.bGraGra.dismissRuleApplicability();
	}

	/**
	 * Checks all graphs of this GraGra due to node type multiplicity 
	 * of the specified type Node of the current type graph.
	 * 
	 * @param typeNode
	 * @return null
	 * 				if all graphs satisfy multiplicity constraint,
	 * 				otherwise - a string with names of failed graphs
	 */
	public String checkNodeTypeMultiplicity(final EdNode typeNode) {
		return this.bGraGra.checkNodeTypeMultiplicity(typeNode.getBasisNode());
	}
	
	/**
	 * Checks all graphs of this GraGra due to edge type multiplicity 
	 * of the specified type Arc of the current type graph.
	 * 
	 * @param typeArc
	 * @return null
	 * 				if all graphs satisfy multiplicity constraint,
	 * 				otherwise - a string with names of failed graphs
	 */
	public String checkEdgeTypeMultiplicity(final EdArc typeArc) {
		return this.bGraGra.checkEdgeTypeMultiplicity(typeArc.getBasisArc());
	}
	
	/**
     * Trims the capacity of used vectors to be the vector's current
     * size.
     */
	public void trimToSize() {
		this.bGraGra.trimToSize();
		this.typeSet.trimToSize();
		this.eGraphs.trimToSize();
		for (int i = 0; i < this.eGraphs.size(); i++) {
			this.eGraphs.get(i).trimToSize();
		}
		this.eRules.trimToSize();
		for (int i=0; i<this.eRules.size(); i++) {
			this.eRules.get(i).trimToSize();
		}
		this.eAtomics.trimToSize();
		for (int i=0; i<this.eAtomics.size(); i++) {
			this.eAtomics.get(i).trimToSize();
		}
		this.eConstraints.trimToSize();
		if (this.startGraph != null) 
			this.startGraph.trimToSize();
	}
	
	/*
	 * private void saveXML(XMLHelper h) { bGraGra.saveXML(h); h.addObject("",
	 * eGraph, true); int j; for (j=0; j<eRules.size(); j++) { h.addObject("",
	 * (EdRule)eRules.elementAt(j), true); } for (j = 0; j <
	 * getAtomics().size(); j++) { h.addObject("", (EdAtomic)getAtomic(j),
	 * true); } updateConstraints(); for (j = 0; j < getConstraints().size();
	 * j++) { h.addObject("", getConstraint(j), true); } }
	 * 
	 * private void loadXML(XMLHelper h) { bGraGra.loadXML(h);
	 * h.enrichObject(eGraph); int j; for (j=0; j<eRules.size(); j++) {
	 * h.enrichObject((EdRule)eRules.elementAt(j)); } //
	 * System.out.println("EdGraGra.loadXML --> get atomics"); for (j = 0; j <
	 * getAtomics().size(); j++) { h.enrichObject(getAtomic(j)); for(int i=0; i<getAtomic(j).getConclusions().size();
	 * i++) { EdAtomic c = (EdAtomic)
	 * getAtomic(j).getConclusions().elementAt(i); c.setGraGra(this); } }
	 * //System.out.println("EdGraGra.loadXML --> get constraints"); for (j = 0;
	 * j < getConstraints().size(); j++) { h.enrichObject(getConstraint(j));
	 * getConstraint(j).setVarSet(getBasisGraGra().getAtomicsVec(),
	 * getAtomicNames()); } }
	 */

	public boolean save() {
		if (this.dirName.equals("")) {
			this.dirName = System.getProperty("user.dir");
		}
		if (this.fileName.equals(""))
			return false;

		return saveToXML(this.fileName);
	}

	public boolean save(String fname) {
		if (fname.equals("")) {
			return save();
		}
		
		if (this.dirName.equals("")) {
			this.dirName = System.getProperty("user.dir");
		}
		return saveToXML(fname);
	}

	private boolean saveToXML(String fname) {
		this.trimToSize();
		
		XMLHelper xmlh = new XMLHelper();
		xmlh.addTopObject(this);
		// System.out.println(fname);
		if (xmlh.save_to_xml(this.dirName + File.separator + fname)) {
			this.isChanged = false;
			return true;
		} 
		return false;
	}

	/**
	 * save the properties and values of all elements of this gragra in the
	 * elements of the base objects in the XML file opened by the given
	 * XMLHelper.
	 * 
	 * @param h
	 *            an XMLHelper, without an open element for this gragra.
	 */
	public void XwriteObject(XMLHelper h) {
		// save the basic gragra
		// this will also save all the subobjects
		// of the basic gragra, which will be enhanced
		// with the layout informations in the next steps
	
		// add imageFileName of a node type to its additional representation
		this.typeSet.enrichAdditionalReprOfNodeType();
		
		h.addTopObject(this.bGraGra);
		h.openObject(this.bGraGra, this);

		for (int i=0; i<this.typeSet.getNodeTypes().size(); i++) {
			h.addObject("", this.typeSet.getNodeTypes().get(i), true);							
		}
		
		Enumeration<Type> types = this.layoutPatterns.keys();
		while (types.hasMoreElements()) {
			Type t = types.nextElement();
			// System.out.println("type name: "+t.getName());
			Vector<LayoutPattern> lpatternsVec = this.layoutPatterns.get(t);
			if (lpatternsVec != null && !lpatternsVec.isEmpty()) {
				h.openSubTag("Layout");
				// System.out.println("type: "+t.toString());
				h.addObject("type", t, false);
				h.addEnumeration("", lpatternsVec.elements(), true);
				h.close();
			}
		}

		h.close();

		/*
		 * add layout info for the type attributes: show/hide
		 * if(typeSet.getTypeGraph() != null){ for (Enumeration
		 * e=typeSet.getTypeGraph().getNodes().elements();
		 * e.hasMoreElements();){ EdGraphObject go = (EdGraphObject)
		 * e.nextElement(); ValueTuple attributes = (ValueTuple)
		 * go.getBasisObject().getAttribute(); if (go.getView() != null) {
		 * AttrViewSetting mvs = go.getView().getMaskedView(); int number =
		 * attributes.getSize(); for(int i=0; i<number; i++){ ValueMember vm =
		 * attributes.getValueMemberAt(i); DeclMember dm = (DeclMember)
		 * attributes.getTupleType().getMemberAt(i); boolean vis =
		 * mvs.isVisible(attributes, i); if(vis){ h.addAttrToObject(dm,
		 * "visible", "true");
		 * //System.out.println(attributes.getValueMemberAt(i).getName()+"
		 * visible: true"); } else{ h.addAttrToObject(dm, "visible", "false");
		 * //System.out.println(attributes.getValueMemberAt(i).getName()+"
		 * visible: false"); } } } } }
		 */

		// add layout info for the host graph
		// h.addObject("", eGraph, true);
		int j;
		// add layout info for the graphs
		for (j = 0; j < this.eGraphs.size(); j++) {
			h.addObject("", this.eGraphs.elementAt(j), true);
		}

		// add layout info for the rules
		for (j = 0; j < this.eRules.size(); j++) {
			h.addObject("", this.eRules.elementAt(j), true);			
		}// for j

		// add layout info for the atomic constraints
		for (j = 0; j < getAtomics().size(); j++) {
			h.addObject("", getAtomic(j), true);
		}// for j

		// add layout information for the constraints
		for (j = 0; j < getConstraints().size(); j++) {
			h.addObject("", getConstraint(j), true);
		}// for j

		// add layout info for the type graph
		if (this.typeSet.getTypeGraph() != null) {
			h.addObject("", this.typeSet.getTypeGraph(), true);
		}
	}// XwriteObject

	public void XreadObject(XMLHelper h) {
		// read the values for the basic gragra
		// and all its subobjects		
		h.peekObject(this.bGraGra, this);

//		long time0 = System.currentTimeMillis();
		for (int i=0; i<this.typeSet.getNodeTypes().size(); i++) {
			EdType et = this.typeSet.getNodeTypes().get(i);
			h.enrichObject(et);					
		}
		
		if (h.readSubTag("Layout")) {
			boolean firstsubtag = true;
			while (firstsubtag || h.readSubTag("Layout")) {
				firstsubtag = false;
				Type t = (Type) h.getObject("type", null, false);
				if (t != null) {
//					EdType et = typeSet.getTypeForName(t);
					Enumeration<?> en = h.getEnumeration("", null, true, "LayoutPattern");
					while (en.hasMoreElements()) {
						h.peekElement(en.nextElement());
						LayoutPattern lp = new LayoutPattern(t);
						h.loadObject(lp);
						addLayoutPattern(t, lp);
						h.close();
					}
				}
				h.close();
			}
			// create inheritance pattern
			Vector<Arc> inheritArcs = this.typeSet.getBasisTypeSet()
					.getInheritanceArcs();
			createInheritancePattern(inheritArcs);
			h.close();
		}

		// read layout information of type graph
		if (this.typeSet.getTypeGraph() != null) {
			h.enrichObject(this.typeSet.getTypeGraph());
			this.typeSet.getTypeGraph().setGraGra(this);
		}
//		System.out.println("(Layouted) Grammar  Types: "
//				+ (System.currentTimeMillis() - time0) + "ms");
		
		// read layout information of graphs
//		time0 = System.currentTimeMillis();
		for (int j = 0; j < this.eGraphs.size(); j++) {
			EdGraph g = this.eGraphs.elementAt(j);
			h.enrichObject(g);
		}
//		System.out.println("(Layouted) Grammar  Graphs: "
//				+ (System.currentTimeMillis() - time0) + "ms");
		
		// read layout information of atomic constraints
		for (int j = 0; j < this.eAtomics.size(); j++) {
			EdAtomic a = this.eAtomics.get(j);
			h.enrichObject(a);
		}

		// read layout information of constraints
		for (int j = 0; j < getConstraints().size(); j++) {
			EdConstraint c = getConstraint(j);
			h.enrichObject(c);

			Vector<Evaluable> atomics = new Vector<Evaluable>();
			atomics.addAll(this.bGraGra.getListOfAtomics());
			c.setVarSet(atomics, this.getAtomicNames());
		}
		
//		long time0 = System.currentTimeMillis();
		// read layout information of rules
		for (int j = 0; j < this.eRules.size(); j++) {
			EdRule r = this.eRules.elementAt(j);
			h.enrichObject(r);	
		}
//		System.out.println("(Layouted) Grammar  enrich Rules: "
//				+ (System.currentTimeMillis() - time0) + "ms");
		
		// do copy of the host graph ( the first loaded graph)
		if (this.eGraph != null)
			this.startGraph = makeStartGraphFrom(this.eGraph);
		else {
			this.startGraph = new EdGraph(this.getTypeSet());
		}
		this.isChanged = false;
		
		this.trimToSize();
	}

	private EdGraph makeStartGraphFrom(EdGraph g) {
		EdGraph sg = g.copy();
		sg.setCurrentLayoutToDefault(true);
		sg.getBasisGraph().setName(sg.getBasisGraph().getName());
		sg.setGraGra(this);
		getBasisGraGra().setStartGraph(sg.getBasisGraph());
		return sg;
	}

	// Layout Teil

	public void createInheritancePattern(Vector<Arc> inheritanceArcs) {
		for (int i = 0; i < inheritanceArcs.size(); i++) {
			Arc inharc = inheritanceArcs.get(i);
			// String inhTypeName =
			// inharc.getSource().getType().getName()+"-INHERITS-"+inharc.getTarget().getType().getName();
			Vector<LayoutPattern> v = getLayoutPatternsForType(inharc.getType());
			v.clear();
			this.createLayoutPattern("ver_tree", "edge", inharc.getType(), 'y',
					-1);
			this.createLayoutPattern("edge_length", "edge", inharc.getType(),
					150);
		}
	}

//	private Vector<LayoutPattern> getInheritancePattern(EdArc edge) {
//		if (edge.getBasisArc().isInheritance()) {
//			// String inhTypeName =
//			// edge.getBasisArc().getSource().getType().getName()+"-INHERITS-"+edge.getBasisArc().getTarget().getType().getName();
//			return getLayoutPatternsForType(edge.getBasisArc().getType());
//		}
//		return null;
//	}

	/**
	 * Creates a layout pattern with the specified parameters.
	 * 
	 * @param pname
	 *            name of a layout pattern
	 * @param pType
	 *            type of a layout pattern ("edge" only)
	 * @param type
	 *            type of an edge which will use this layout pattern
	 * @param offsetType
	 *            offset type which can be 'x' or 'y'
	 * @param offset
	 *            value which can be -1, 1 or 0.
	 */
	public void createLayoutPattern(String pname, String pType, Type type,
			char offsetType, int offset) {
		LayoutPattern lp = new LayoutPattern(pname, pType, type, offsetType,
				offset);
		Vector<LayoutPattern> v = this.layoutPatterns.get(type);
		if (v == null)
			v = new Vector<LayoutPattern>();
		for (int i = 0; i < v.size(); i++) {
			if (v.get(i).isSimilarTo(lp)) {
				v.remove(i);
				i--;
			}
		}
		v.add(lp);
		this.layoutPatterns.put(type, v);
		// System.out.println("EdGraGra.createLayoutPattern type: "+typeName+"
		// count:"+v.size());
	}

	/**
	 * Creates a layout pattern with the specified parameters.
	 * 
	 * @param pname
	 *            name of a layout pattern
	 * @param pType
	 *            type of a layout pattern ("edge" only)
	 * @param type
	 *            type of an edge which will use this layout pattern
	 * @param length
	 *            length of an edge.
	 */
	public void createLayoutPattern(String pname, String pType, Type type,
			int length) {
		LayoutPattern lp = new LayoutPattern(pname, pType, type, length);
		Vector<LayoutPattern> v = this.layoutPatterns.get(type);
		if (v == null)
			v = new Vector<LayoutPattern>();
		for (int i = 0; i < v.size(); i++) {
			if (v.get(i).isSimilarTo(lp)) {
				v.remove(i);
				i--;
			}
		}
		v.add(lp);
		this.layoutPatterns.put(type, v);
		// System.out.println("EdGraGra.createLayoutPattern type: "+typeName+"
		// count:"+v.size());
	}

	/**
	 * Creates a layout pattern with the specified parameters.
	 * 
	 * @param pName
	 *            name of a layout pattern
	 * @param pType
	 *            type of a layout pattern ("edge" only)
	 * @param type
	 *            type of an edge which will use this layout pattern
	 * @param frozen
	 *            allow to change (X,Y) position
	 */
	public void createLayoutPattern(String pName, String pType, Type type,
			boolean frozen) {
		LayoutPattern lp = new LayoutPattern(pName, pType, type, frozen);
		Vector<LayoutPattern> v = this.layoutPatterns.get(type);
		if (v == null)
			v = new Vector<LayoutPattern>();
		for (int i = 0; i < v.size(); i++) {
			if (v.get(i).isSimilarTo(lp)) {
				v.remove(i);
				i--;
			}
		}
		v.add(lp);
		this.layoutPatterns.put(type, v);
		// System.out.println("EdGraGra.createLayoutPattern type: "+typeName+"
		// count:"+v.size());
	}

	public void addLayoutPattern(Type type, LayoutPattern lp) {
		Vector<LayoutPattern> v = this.layoutPatterns.get(type);
		if (v == null)
			v = new Vector<LayoutPattern>();
		for (int i = 0; i < v.size(); i++) {
			if (v.get(i).isSimilarTo(lp)) {
				v.remove(i);
				i--;
			}
		}
		v.add(lp);
		this.layoutPatterns.put(type, v);
		// System.out.println("EdGraGra.addLayoutPattern type:
		// "+type.getName()+" count:"+v.size());

	}

	public void removeLayoutPattern(Type type) {
		this.layoutPatterns.remove(type);
		// System.out.println("Layouter.removeLayoutPattern type: "+type+"
		// count:"+layoutPatterns.size());
	}

	public void removeLayoutPattern(Type type, String patternName) {
		Vector<LayoutPattern> v = this.layoutPatterns.get(type);
		if (v == null || v.isEmpty())
			return;
		for (int i = 0; i < v.size(); i++) {
			LayoutPattern lp = v.get(i);
			if (lp.getName().equals(patternName)) {
				v.remove(lp);
				i--;
			}
		}
		this.layoutPatterns.put(type, v);
		// System.out.println("Layouter.removeLayoutPattern type: "+typeName+"
		// name: "+patternName+" count:"+v.size());
	}

	public void removeAllLayoutPattern() {
		this.layoutPatterns.clear();
		// System.out.println("Layouter.removeAllLayoutPattern size:
		// "+this.layoutPatterns.size());
	}

	public Hashtable<Type, Vector<LayoutPattern>> getLayoutPatterns() {
		return this.layoutPatterns;
	}

	public void clearLayoutPatterns() {
		this.layoutPatterns.clear();
	}

	public Vector<LayoutPattern> getLayoutPatternsForType(Type type) {
		Vector<LayoutPattern> v = this.layoutPatterns.get(type);
		if (v == null) {
			v = new Vector<LayoutPattern>();
			this.layoutPatterns.put(type, v);
		}
		return v;
	}

	public LayoutPattern getLayoutPatternForType(Type type, String patternName) {
		Vector<LayoutPattern> v = this.layoutPatterns.get(type);
		if (v == null)
			return null;

		for (int i = 0; i < v.size(); i++) {
			LayoutPattern lp = v.get(i);
			if (lp.getName().equals(patternName))
				return lp;
		}
		return null;
	}

	public void setLayoutPatterns(Hashtable<Type, Vector<LayoutPattern>> table) {
		this.layoutPatterns.clear();
		Enumeration<Type> keys = table.keys();
		while (keys.hasMoreElements()) {
			Type key = keys.nextElement();
			this.layoutPatterns.put(key, table.get(key));
		}
	}

}// class EdGraGra
// $Log: EdGraGra.java,v $
// Revision 1.114  2010/11/04 10:57:05  olga
// improved
//
// Revision 1.113  2010/10/19 19:03:49  olga
// improved
//
// Revision 1.112  2010/10/16 22:43:42  olga
// improved undo for RuleScheme graph objects
//
// Revision 1.111  2010/09/30 22:13:11  olga
// improved
//
// Revision 1.110  2010/09/30 14:11:41  olga
// delete objects of especial type -  improved
//
// Revision 1.109  2010/09/27 22:44:22  olga
// improved
//
// Revision 1.108  2010/09/27 16:21:20  olga
// improved
//
// Revision 1.107  2010/09/23 20:02:28  olga
// delete objects of especial type -  bug fixed
//
// Revision 1.106  2010/08/25 00:33:07  olga
// tuning
//
// Revision 1.105  2010/07/29 10:15:41  olga
// tuning
//
// Revision 1.104  2010/06/30 16:12:14  olga
// added - getEnabledRules()
//
// Revision 1.103  2010/06/09 11:07:50  olga
// extended due to new NestedApplCond
//
// Revision 1.102  2010/04/27 10:32:56  olga
// compute parallel rule - improved
//
// Revision 1.101  2010/04/13 12:02:28  olga
// tuning
//
// Revision 1.100  2010/04/08 14:13:19  olga
// added to makeParralelRule:  make match of each source rule, general parallel independent check
//
// Revision 1.99  2010/04/07 17:47:43  olga
// added - refreshAttributed
//
// Revision 1.98  2010/03/24 21:47:07  olga
// parallel rule
//
// Revision 1.97  2010/03/10 14:44:49  olga
// make identical rule - bug fixed
//
// Revision 1.96  2010/03/08 15:40:03  olga
// code optimizing
//
// Revision 1.95  2010/03/04 14:08:12  olga
// code optimizing
//
// Revision 1.94  2010/02/22 15:07:57  olga
// code optimizing
//
// Revision 1.93  2010/01/03 15:33:57  olga
// tuning
//
// Revision 1.92  2009/12/08 13:25:39  olga
// ConcurrentRule from RuleSequence - new
//
// Revision 1.91  2009/11/23 08:55:19  olga
// more extensions for RuleScheme
//
// Revision 1.90  2009/11/04 11:02:24  olga
// build concurrent rule - impl and test
//
// Revision 1.89  2009/10/26 10:07:23  olga
// inverting rule improved
//
// Revision 1.88  2009/10/19 16:24:58  olga
// tuning
//
// Revision 1.87  2009/10/17 21:30:03  olga
// new method:  inverseRule
//
// Revision 1.86  2009/09/21 13:59:48  olga
// copy rule improved
//
// Revision 1.85  2009/07/16 17:21:02  olga
// GUI bugs fixed
//
// Revision 1.84  2009/07/08 16:22:06  olga
// Multiplicity bug fixed;
// ARS development
//
// Revision 1.83  2009/06/30 09:50:19  olga
// agg.xt_basis.GraphObject: added: setObjectName(String), getObjectName()
// agg.xt_basis.Node, Arc: changed: save, load the object name
// agg.editor.impl.EdGraphObject: changed: String getTypeString() - contains object name if set
//
// workaround of Applicability of Rule Sequences and Object Flow
//
// Revision 1.82  2009/06/02 12:39:19  olga
// Min Multiplicity check - bug fixed
//
// Revision 1.81  2009/05/28 13:18:24  olga
// Amalgamated graph transformation - development stage
//
// Revision 1.80  2009/05/12 10:36:59  olga
// CPA: bug fixed
// Applicability of Rule Seq. : bug fixed
//
// Revision 1.79  2009/04/27 07:37:17  olga
// Copy and Paste TypeGraph- bug fixed
// CPA - dangling edge conflict when first produce second delete - extended
//
// Revision 1.78  2009/04/14 09:18:34  olga
// Edge Type Multiplicity check - bug fixed
//
// Revision 1.77  2009/03/26 15:51:14  olga
// code tuning
//
// Revision 1.76  2009/03/25 15:19:14  olga
// code tuning
//
// Revision 1.75  2009/03/12 12:02:11  olga
// code tuning
//
// Revision 1.74  2009/02/02 09:05:13  olga
// New trafo option: Reset Graph, which can be used with the option -loop over layers-
// when the user want to start a new loop at the start (not transformed) graph.
// New trafo option: Graph Consistency check at the end of (layer) graph transformation.
// Bug fixed  in copy start graph
//
// Revision 1.73  2008/12/17 09:37:41  olga
// Import of TypeGraph from  grammar (.ggx) - bug fixed
//
// Revision 1.72  2008/11/24 11:35:12  olga
// GUI tuning
//
// Revision 1.71  2008/11/06 08:45:36  olga
// Graph layout is extended by Zest Graph Layout ( eclipse zest plugin)
//
// Revision 1.70  2008/10/29 09:04:04  olga
// new sub packages of the package agg.gui: typeeditor, editor, trafo, cpa, options, treeview, popupmenu, saveload
//
// Revision 1.69  2008/10/22 14:07:55  olga
// GUI, ARS and CPA  tuning
//
// Revision 1.68  2008/10/07 11:46:07  olga
// Bug fixed: create a node of an abstract node type inside of rules
//
// Revision 1.67  2008/10/02 16:40:55  olga
// - Reset host graph - bug fixed ,
// - improved mouse event handling,
// - Applicability of rule sequences:  save and load grammar : layout data will be saved
// and loaded, too
//
// Revision 1.66  2008/09/25 08:02:51  olga
// improved graphics update during graph transformation
//
// Revision 1.65  2008/09/11 09:22:25  olga
// Some changes in CPA: new computing of conflicts after an option changed,
// Graph layout of overlapping graphs
//
// Revision 1.64  2008/09/04 07:48:42  olga
// GUI extension: hide nodes, edges
//
// Revision 1.63  2008/07/28 14:57:58  olga
// Applicability of RS - bug fixed
// Graph transformation in debug mode, rule with input parameter - bug fixed
// Code tuning
//
// Revision 1.62  2008/07/14 07:35:47  olga
// Applicability of RS - new option added, more tuning
// Node animation - new animation parameter added,
// Undo edit manager - possibility to disable it when graph transformation
// because it costs much more time and memory
//
// Revision 1.61  2008/07/09 13:34:27  olga
// Applicability of RS - bug fixed
// Delete not used node/edge type - bug fixed
// AGG help - extended
//
// Revision 1.60  2008/07/02 17:14:36  olga
// Code tuning
//
// Revision 1.59  2008/06/30 10:47:40  olga
// Applicability of Rule Sequence - tuning
// Node animation - first steps
//
// Revision 1.58  2008/06/26 14:18:47  olga
// Graph visualization tuning
//
// Revision 1.57  2008/06/18 15:35:30  olga
// Applicability of Rule Sequences - Tuning
//
// Revision 1.56  2008/04/17 10:11:07  olga
// Undo, redo edit and graph layout tuning,
//
// Revision 1.55  2008/04/11 13:29:05  olga
// Memory usage - tuning
//
// Revision 1.54  2008/04/10 14:57:25  olga
// code tuning
//
// Revision 1.53  2008/04/09 10:29:38  olga
// Bug fixed - import graph type with abstract types
//
// Revision 1.52  2008/04/09 07:38:53  olga
// Bug fixed during loading of grammar
//
// Revision 1.51  2008/04/07 14:01:02  olga
// Undo/Redo ON
//
// Revision 1.50  2008/04/07 09:36:50  olga
// Code tuning: refactoring + profiling
// Extension: CPA - two new options added
//
// Revision 1.49  2008/02/25 08:44:49  olga
// Extending of CPA: new class CriticalRulePairAtGraph to get critical
// matches of two rules at a concret graph.
//
// Revision 1.48  2008/02/18 09:37:11  olga
// - an extention of rule dependency check is implemented;
// - some bugs fixed;
// - editing of graphs improved
//
// Revision 1.47  2007/11/19 08:48:39  olga
// Some GUI usability mistakes fixed.
// Default values in node/edge of a type graph implemented.
// Code tuning.
//
// Revision 1.46  2007/11/14 14:27:23  olga
// code tuning
//
// Revision 1.45  2007/11/14 08:53:43  olga
// code tuning
//
// Revision 1.44  2007/11/05 09:18:16  olga
// code tuning
//
// Revision 1.43  2007/11/01 09:58:11  olga
// Code refactoring: generic types- done
//
// Revision 1.42  2007/10/11 08:05:04  olga
// Enumeration typing
//
// Revision 1.41  2007/09/24 09:42:33  olga
// AGG transformation engine tuning
//
// Revision 1.40  2007/09/17 10:50:01  olga
// Bug fixed in graph transformation by rules with NACs and PACs .
// AGG help docus extended by new implemented features.
//
// Revision 1.39  2007/09/10 13:05:15  olga
// In this update:
// - package xerces2.5.0 is not used anymore;
// - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
// - bugs fixed in:  usage of PACs in rules;  match completion;
// 	usage of static method calls in attr. conditions
// - graph editing: added some new features
//
// Revision 1.38 2007/06/28 15:46:20 olga
// Graph layouter,
// Type palette - select type,
// Magic arc - tuning
//
// Revision 1.37 2007/06/25 08:28:22 olga
// Tuning and Docu update
//
// Revision 1.36 2007/06/13 08:32:44 olga
// Update: V161
//
// Revision 1.35 2007/04/30 10:38:55 olga
// Reset graph : layout of the start graph as default
//
// Revision 1.34 2007/04/24 07:08:47 olga
// undo/redo match mappings
//
// Revision 1.33 2007/04/19 07:52:34 olga
// Tuning of: Undo/Redo, Graph layouter, loading grammars
//
// Revision 1.32 2007/04/12 10:24:28 olga
// undo/redo
//
// Revision 1.31 2007/04/11 10:03:35 olga
// Undo, Redo tuning,
// Simple Parser- bug fixed
//
// Revision 1.30 2007/03/28 10:00:23 olga
// - extensive changes of Node/Edge Type Editor,
// - first Undo implementation for graphs and Node/edge Type editing and
// transformation,
// - new / reimplemented options for layered transformation, for graph layouter
// - enable / disable for NACs, attr conditions, formula
// - GUI tuning
//
// Revision 1.29 2007/02/19 09:11:00 olga
// Bug during loading file fixed.
// Type editor tuning
//
// Revision 1.28 2007/02/05 12:33:44 olga
// CPA: chengeAttribute conflict/dependency : attributes with constants
// bug fixed, but the critical pairs computation has still a gap.
//
// Revision 1.27 2007/01/22 08:28:29 olga
// GUI bugs fixed
//
// Revision 1.26 2007/01/15 12:17:21 olga
// .....
//
// Revision 1.25 2007/01/11 10:21:20 olga
// Optimized Version 1.5.1beta , free for tests
//
// Revision 1.24 2006/12/13 13:33:04 enrico
// reimplemented code
//
// Revision 1.23 2006/11/01 11:17:29 olga
// Optimized agg sources of CSP algorithm, match usability,
// graph isomorphic copy,
// node/edge type multiplicity check for injective rule and match
//
// Revision 1.22 2006/08/16 11:41:16 olga
// edit mode tuning
// graph layout by node type pattern FreezingAge extended
//
// Revision 1.21 2006/08/09 07:42:18 olga
// API docu
//
// Revision 1.20 2006/08/02 09:00:57 olga
// Preliminary version 1.5.0 with
// - multiple node type inheritance,
// - new implemented evolutionary graph layouter for
// graph transformation sequences
//
// Revision 1.19 2006/05/29 07:59:41 olga
// GUI, undo delete - tuning.
//
// Revision 1.18 2006/05/22 08:27:33 olga
// CPA: Bug fixed
// Gragra trash: tuning
//
// Revision 1.17 2006/05/18 15:41:31 olga
// CPA: Bug fixed
// Import graph tuning
//
// Revision 1.16 2006/05/08 08:24:12 olga
// Some extentions of GUI: - Undo Delete button of tool bar to undo deletions
// if grammar elements like rule, NAC, graph constraints;
// - the possibility to add a new graph to a grammar or a copy of the current
// host graph;
// - to set one or more layer for consistency constraints.
// Also some bugs fixed of matching and some optimizations of CSP algorithmus
// done.
//
// Revision 1.15 2006/04/12 14:54:07 olga
// Restore attr. values of attr. type observers after type graph imported.
//
// Revision 1.14 2006/04/12 09:01:57 olga
// Layered graph constraints tuning
//
// Revision 1.13 2006/04/10 09:19:30 olga
// Import Type Graph, Import Graph - tuning.
// Attr. member type check: if class does not exist.
// Graph constraints for a layer of layered grammar.
//
// Revision 1.12 2006/04/06 15:27:30 olga
// Import Type Graph and Type Graph tuning
//
// Revision 1.11 2006/04/06 09:28:53 olga
// Tuning of Import Type Graph and Import Graph
//
// Revision 1.10 2006/04/03 08:57:50 olga
// New: Import Type Graph
// and some bugs fixed
//
// Revision 1.9 2006/03/01 09:55:46 olga
// - new CPA algorithm, new CPA GUI
//
// Revision 1.8 2006/01/16 09:38:34 olga
// GUI tuning
//
// Revision 1.7 2005/10/12 15:18:14 olga
// CPA GUI
//
// Revision 1.6 2005/09/26 08:35:15 olga
// CPA graph frames; bugs
//
// Revision 1.5 2005/09/19 09:12:14 olga
// CPA GUI tuning
//
// Revision 1.4 2005/09/12 11:16:20 olga
// A small completion of the method cloneRule().
//
// Revision 1.3 2005/09/05 10:06:45 olga
// Deleting type graph object - fixed.
//
// Revision 1.2 2005/09/01 08:22:14 olga
// Adaptation inheritance version to AGG standard:
// - remove type graph nodes/arcs,
// - GUI conformance.
//
// Revision 1.1 2005/08/25 11:56:56 enrico
// *** empty log message ***
//
// Revision 1.3 2005/07/11 09:30:20 olga
// This is test version AGG V1.2.8alfa .
// What is new:
// - saving rule option <disabled>
// - setting trigger rule for layer
// - display attr. conditions in gragra tree view
// - CPA algorithm <dependencies>
// - creating and display CPA graph with conflicts and/or dependencies
// based on (.cpx) file
//
// Revision 1.2.2.1 2005/07/04 11:41:37 enrico
// basic support for inheritance
//
// Revision 1.2 2005/06/20 13:37:04 olga
// Up to now the version 1.2.8 will be prepared.
//
// Revision 1.1 2005/05/30 12:58:02 olga
// Version with Eclipse
//
// Revision 1.35 2005/05/23 09:54:30 olga
// CPA improved: Stop of generation process or rule pair.
//
// Revision 1.34 2005/03/03 13:48:42 olga
// - Match with NACs and attr. conditions with mixed variables - error corrected
// - save/load class packages written by user
// - PACs : creating T-equivalents - improved
// - save/load matches of the rules (only one match of a rule)
// - more friendly graph/rule editor GUI
// - more syntactical checks in attr. editor
//
// Revision 1.33 2005/02/14 09:27:01 olga
// -PAC;
// -GUI, layered graph transformation anzeigen;
// -CPs.
//
// Revision 1.32 2005/01/28 14:02:32 olga
// -Fehlerbehandlung beim Typgraph check
// -Erweiterung CP GUI / CP Menu
// -Fehlerbehandlung mit identification option
// -Fehlerbehandlung bei Rule PAC
//
// Revision 1.31 2005/01/03 13:14:43 olga
// Errors handling
//
// Revision 1.30 2004/12/20 14:53:48 olga
// Changes because of matching optimisation.
//
// Revision 1.29 2004/11/15 11:24:45 olga
// Neue Optionen fuer Transformation;
// verbesserter default Graphlayout;
// Close GraGra mit Abfrage wenn was geaendert wurde statt Delete GraGra
//
// Revision 1.28 2004/10/27 10:06:55 olga
// Version 1.2.4
// Termination of LGTS
//
// Revision 1.27 2004/10/25 14:24:37 olga
// Fehlerbehandlung bei CPs und Aenderungen im zusammenhang mit
// termination-Modul
// in AGG
//
// Revision 1.26 2004/09/13 10:21:14 olga
// Einige Erweiterungen und Fehlerbeseitigung bei CPs und
// Graph Grammar Transformation
//
// Revision 1.25 2004/06/09 11:32:54 olga
// Attribute-Eingebe/Bedingungen : NAC kann jetzt eigene Variablen und
// Bedingungen
// haben.
// CP Berechnung korregiert.
//
// Revision 1.24 2004/05/19 15:41:34 olga
// Comments
//
// Revision 1.23 2004/05/13 17:54:09 olga
// Fehlerbehandlung
//
// Revision 1.22 2004/04/28 12:46:38 olga
// test CSP
//
// Revision 1.21 2004/04/15 10:49:47 olga
// Kommentare
//
// Revision 1.20 2004/01/28 17:58:38 olga
// Errors suche
//
// Revision 1.19 2003/12/18 16:26:22 olga
// Copy method and Layout
//
// Revision 1.18 2003/10/16 08:25:13 olga
// Copy rule implementiert
//
// Revision 1.17 2003/09/18 10:40:17 olga
// Tests
//
// Revision 1.16 2003/04/10 09:05:23 olga
// Aenderungen wegen serializable Ausgabe
//
// Revision 1.15 2003/03/20 13:34:11 olga
// Delete TypeGraph eingefuegt
//
// Revision 1.14 2003/03/06 14:47:59 olga
// neu: setEditable, isEditable
//
// Revision 1.13 2003/03/05 18:24:25 komm
// sorted/optimized import statements
//
// Revision 1.12 2003/02/24 13:30:11 olga
// set.getEditable Methode eingefuegt
//
// Revision 1.11 2003/02/03 17:46:54 olga
// new method : compareTo( ...)
//
// Revision 1.10 2002/12/20 11:25:56 olga
// Tests
//
// Revision 1.9 2002/12/18 11:43:26 komm
// remove of type error marks works now
//
// Revision 1.8 2002/12/16 13:44:00 komm
// renamed methods for turning off type graph check
//
// Revision 1.7 2002/11/25 15:03:51 olga
// Arbeit an den Typen.
//
// Revision 1.6 2002/11/07 15:58:26 olga
// Fehlerbehandlung
//
// Revision 1.5 2002/10/02 18:30:42 olga
// XXX
//
// Revision 1.4 2002/09/30 10:06:53 komm
// TypeCheck expanded for nodes
//
// Revision 1.3 2002/09/26 14:01:34 olga
// Beseitigung von null Fehler
//
// Revision 1.2 2002/09/23 12:24:06 komm
// added type graph in xt_basis, editor and GUI
//
// Revision 1.1.1.1 2002/07/11 12:17:07 olga
// Imported sources
//
// Revision 1.12 2001/03/15 11:51:53 olga
// Typefehler beseitigt.
//
// Revision 1.11 2001/03/08 10:53:18 olga
// Das ist Stand nach der AGG GUI Reimplementierung.
//
// Revision 1.10 2001/01/04 08:31:49 olga
// *** empty log message ***
//
// Revision 1.9 2000/12/21 09:48:51 olga
// In dieser Version wurden XML und GUI Reimplementierung zusammen gefuehrt.
//
// Revision 1.8 2000/12/07 14:23:36 matzmich
// XML-Kram
// Man beachte: xerces (/home/tfs/gragra/AGG/LIB/Xerces/xerces.jar) wird
// jetzt im CLASSPATH benoetigt.
//
// Revision 1.7.6.3 2000/12/13 13:21:17 olga
// Neu: removeRules()
//
// Revision 1.7.6.2 2000/12/04 13:25:53 olga
// Erste Stufe der GUI Reimplementierung abgeschlossen:
// - AGGAppl.java optimiert
// - Print eingebaut (GraGraPrint.java)
// - GraGraTreeView.java, GraGraEditor.java optimiert
// - Event eingebaut
// - GraTra umgestellt
//
// Revision 1.7.6.1 2000/11/06 09:32:31 olga
// Erste Version fuer neue GUI (Branch reimpl)
//
// Revision 1.7 2000/06/05 10:19:39 olga
// Kleine Aenderung, dass weniger TypeSets und Types angelegt wereden.
//
// Revision 1.6 1999/10/11 10:42:36 shultzke
// kleine Bugfixes
//
