/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.termination;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import agg.xt_basis.Completion_InjCSP;
import agg.xt_basis.GraGra;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.Rule;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.Type;
import agg.xt_basis.RuleLayer;
import agg.xt_basis.Arc;
import agg.util.IntComparator;
import agg.util.OrderedSet;
import agg.util.Pair;


//import com.objectspace.jgl.OrderedSet;
//import com.objectspace.jgl.OrderedSetIterator;

/**
 * This class implements termination conditions of Layered Graph Grammar.
 * 
 * @author $Author: olga $
 * @version $Id: TerminationLGTS.java,v 1.29 2010/09/23 08:26:15 olga Exp $
 */
public class TerminationLGTS implements TerminationLGTSInterface {

	/** The graph grammar */
	private GraGra grammar;

	private List<Rule> listOfRules;
	
	private boolean layered, priority;

	private Vector<Rule> deletionRule;

	private Vector<Rule> nondeletionRule;

	private Vector<Rule> creationRule;

	private Hashtable<Rule, Integer> ruleLayer;

	private Hashtable<Type, Integer> creationLayer;

	private Hashtable<Type, Integer> deletionLayer;

	private boolean generateRuleLayer;

	private Hashtable<Rule, Integer> oldRuleLayer;

	private int maxl;

	private Hashtable<Integer, HashSet<Rule>> invertedRuleLayer;

	private OrderedSet<Integer> orderedRuleLayerSet;

	private Hashtable<Integer, HashSet<Object>> invertedTypeDeletionLayer;

	private OrderedSet<Integer> orderedTypeDeletionLayerSet;

	private Hashtable<Integer, HashSet<Object>> invertedTypeCreationLayer;

	private OrderedSet<Integer> orderedTypeCreationLayerSet;

	private Integer //startLayer, 
					startRuleLayer;

	private Vector<Integer> orderedRuleLayer;

	// private Vector orderedTypeDeletionLayer;
	// private Vector orderedTypeCreationLayer;
	private Hashtable<Integer, Pair<Boolean, Vector<Rule>>> resultTypeDeletion;

	private Hashtable<Integer, Pair<Boolean, Vector<Rule>>> resultDeletion;

	private Hashtable<Integer, Pair<Boolean, Vector<Rule>>> resultNonDeletion;

	private Hashtable<Integer, List<String>> errorMsg;
	private Hashtable<Integer, List<String>> errorMsgDeletion1;
	private Hashtable<Integer, List<String>> errorMsgDeletion2;
	private Hashtable<Integer, List<String>> errorMsgNonDeletion;

	private Hashtable<Integer, Vector<Type>> deletionType;

	private boolean needCorrection = false;

	/** The error message if termination conditions are not valid */
	private String errMsg;

	/** true if termination conditions are valid */
	private boolean valid;

	
	public TerminationLGTS() {}

	public void dispose() {
		if (this.grammar != null)
			unsetLayer();
		this.grammar = null;
	}
	
	/**
	 * Initialize a termination layers of the grammar. Initially the termination
	 * conditions are invalid.
	 * 
	 * @param gra
	 *           the graph grammar.
	 */
	public void setGrammar(GraGra gra) {
		if (gra == null) {
			unsetLayer();
			this.grammar = null;
		} else {
			this.grammar = gra;
			this.listOfRules = this.getListOfEnabledRules();
			this.errMsg = "";
			this.valid = false;			
			this.oldRuleLayer = new Hashtable<Rule, Integer>();			
			setKind();			
			initRuleLayer(this.grammar);
			initCreationLayer(this.grammar);
			initDeletionLayer(this.grammar);
			initOrderedRuleLayer(this.grammar);
			this.deletionType = new Hashtable<Integer, Vector<Type>>();
			initResults();
		}
	}

	public void resetGrammar() {
		if (this.grammar != null) {
			this.listOfRules = this.getListOfEnabledRules();
			this.errMsg = "";
			this.valid = false;			
			setKind();
			reinitRuleLayer();
			reinitCreationLayer();
			reinitDeletionLayer();
			reinitOrderedRuleLayer();
			this.deletionType.clear();
			reinitResults();
		}
	}
	
	public GraGra getGrammar() {
		return this.grammar;
	}

	public List<Rule> getListOfEnabledRules() {
		List<Rule> list = new Vector<Rule>();
		for (int i=0; i<this.grammar.getListOfRules().size(); i++) {
			Rule r = this.grammar.getListOfRules().get(i);
			if (r.isEnabled()) {
				list.add(r);			
			}
		}
		return list;
	}
	
	public boolean hasGrammarChanged() {
		if (this.grammar != null) {
			boolean changed = false;
			if (this.grammar.hasRuleChangedEvailability()
					|| (this.layered && this.grammar.trafoByPriority())
					|| (this.priority && this.grammar.isLayered()) 
					|| (this.layered && this.grammar.hasRuleChangedLayer())
					|| (this.priority && this.grammar.hasRuleChangedPriority())
					|| (this.layered && !this.grammar.isLayered()) )  {
				changed = true;
				this.setGrammar(this.grammar);
			}
			
			return changed;
		} 
		return false;
	}
	
	public List<Rule> getListOfRules() {
		return this.listOfRules;
	}
	
	public Hashtable<Integer, HashSet<Rule>> getInvertedRuleLayer() {
		return this.invertedRuleLayer;
	}

	public Vector<Integer> getOrderedRuleLayer() {
		return this.orderedRuleLayer;
	}

	public Hashtable<Integer, HashSet<Object>> getInvertedTypeDeletionLayer() {
		return this.invertedTypeDeletionLayer;
	}

	// public Vector getOrderedTypeDeletionLayer()
	// { return orderedTypeDeletionLayer; }

	public Hashtable<Integer, HashSet<Object>> getInvertedTypeCreationLayer() {
		return this.invertedTypeCreationLayer;
	}

	// public Vector getOrderedTypeCreationLayer()
	// { return orderedTypeCreationLayer; }

	/**
	 * This table maps an Integer layer number to a Vector of Types 
	 * the objects of which (nodes resp. edges) will be deleted by some rules. 
	 */
	public Hashtable<Integer, Vector<Type>> getDeletionType() {
		return this.deletionType;
	}

	/**
	 * The result table maps an Integer layer number to a Pair
	 * with a Boolean result for a Vector of Rules. 
	 */
	public Hashtable<Integer, Pair<Boolean, Vector<Rule>>> getResultTypeDeletion() {
		return this.resultTypeDeletion;
	}

	/**
	 * The result table maps an Integer layer number to a Pair
	 * with a Boolean result for a Vector of Rules. 
	 */
	public Hashtable<Integer, Pair<Boolean, Vector<Rule>>> getResultDeletion() {
		return this.resultDeletion;
	}

	/**
	 * The result table maps an Integer layer number to a Pair
	 * with a Boolean result for a Vector of Rules. 
	 */
	public Hashtable<Integer, Pair<Boolean, Vector<Rule>>> getResultNondeletion() {
		return this.resultNonDeletion;
	}

	public void resetLayer() {
		this.maxl = 0;		
		setKind();		
		initRuleLayer(this.oldRuleLayer);
		initCreationLayer(this.grammar);
		initDeletionLayer(this.grammar);
		initOrderedRuleLayer(this.grammar);
		this.deletionType = new Hashtable<Integer, Vector<Type>>();
		initResults();
	}
	
	private void setKind() {
//		System.out.println(this.getClass().getName()+".seKind()");
//		System.out.println(this.grammar.isLayered()+"   "+this.grammar.isPriority());
//		System.out.println(this.layered +"    "+this.priority);
		
		this.priority = this.grammar.trafoByPriority();
		this.layered = this.grammar.isLayered() || !this.priority;
		this.oldRuleLayer.clear();
		this.saveRuleLayerInto(this.oldRuleLayer);
		
//		System.out.println(this.layered +"    "+this.priority);
	}
	
	private void unsetLayer() {
		this.creationLayer.clear();		
		this.deletionLayer.clear();
		this.deletionType.clear();
		this.deletionRule.clear();
		this.creationRule.clear();
		this.nondeletionRule.clear();
		this.invertedRuleLayer.clear();
//		this.invertedTypeDeletionLayer.clear();
		this.ruleLayer.clear();
		this.oldRuleLayer.clear();
		this.orderedRuleLayerSet.clear();
//		this.orderedTypeDeletionLayerSet.clear();
//		this.invertedTypeCreationLayer.clear();
//		this.orderedTypeCreationLayerSet.clear();
		this.resultTypeDeletion.clear();
		this.resultDeletion.clear();
		this.resultNonDeletion.clear();
		clearErrors();		
	}

	private void initRuleLayer(GraGra gragra) {
		this.ruleLayer = new Hashtable<Rule, Integer>();
		this.deletionRule = new Vector<Rule>();
		this.nondeletionRule = new Vector<Rule>();
		this.creationRule = new Vector<Rule>();
		Iterator<Rule> rules = this.listOfRules.iterator();
		while (rules.hasNext()) {
			Rule rule = rules.next();	
//			System.out.println(rule.getName()+"  layer: "+rule.getLayer()+"  prior: "+rule.getPriority());
				if (this.priority)
					this.ruleLayer.put(rule, Integer.valueOf(rule.getPriority()));
				else 
					this.ruleLayer.put(rule, Integer.valueOf(rule.getLayer()));
				if (isDeleting(rule)) {
					this.deletionRule.add(rule);
//					System.out.println("Deleting rule:  "+rule.getName());
					
//					if (isCreating(rule)) {
//						creationRule.add(rule);
//						System.out.println("Deleting and Creating rule:  "+rule.getName());
//					}
				} else {
					if (isCreating(rule)) {
						this.creationRule.add(rule);
//						System.out.println("Creating rule:  "+rule.getName());
					}
					this.nondeletionRule.add(rule);
				}
		}
	}

	private void reinitRuleLayer() {
		this.ruleLayer.clear();
		this.deletionRule.clear();
		this.nondeletionRule.clear();
		this.creationRule.clear();
		Iterator<Rule> rules = this.listOfRules.iterator();
		while (rules.hasNext()) {
			Rule rule = rules.next();			
			if (this.priority)
				this.ruleLayer.put(rule, Integer.valueOf(rule.getPriority()));
			else 
				this.ruleLayer.put(rule, Integer.valueOf(rule.getLayer()));
			
			if (isDeleting(rule)) {
				this.deletionRule.add(rule);
//				System.out.println("Deleting rule:  "+rule.getName());
			} else {
				if (isCreating(rule)) {
					this.creationRule.add(rule);
//					System.out.println("Creating rule:  "+rule.getName());
				}
				this.nondeletionRule.add(rule);
			}
		}
	}

	/*
	private void initRuleLayer(int init) {
		for (Enumeration<Rule> keys = ruleLayer.keys(); keys.hasMoreElements();) {
			Rule rule = keys.nextElement();
			if (rule.isEnabled()) {
				ruleLayer.put(rule, Integer.valueOf(init));
			}
		}
	}
*/
	
	public void initRuleLayer(Hashtable<?, Integer> init) {
		for (Enumeration<?> keys = init.keys(); keys.hasMoreElements();) {
			Rule rule = (Rule) keys.nextElement();
			if (rule.isEnabled()) {
				Integer rl = init.get(rule);
				this.ruleLayer.put(rule, Integer.valueOf(rl.intValue()));
			}
		}
	}

	private void initCreationLayer(GraGra gragra) {
		this.creationLayer = new Hashtable<Type, Integer>();
		Enumeration<Type> types = gragra.getTypes();
		while (types.hasMoreElements()) {
			Type t = types.nextElement();
			this.creationLayer.put(t, Integer.valueOf(0));
		}
	}

	private void reinitCreationLayer() {
		this.creationLayer.clear();
		Enumeration<Type> types = this.grammar.getTypes();
		while (types.hasMoreElements()) {
			Type t = types.nextElement();
			this.creationLayer.put(t, Integer.valueOf(0));
		}
	}
	
	private void initCreationLayer(int init) {
		for (Enumeration<Type> keys = this.creationLayer.keys(); keys.hasMoreElements();) {
			Type t = keys.nextElement();
			this.creationLayer.put(t, Integer.valueOf(init));
		}
	}

	private void initDeletionLayer(GraGra gragra) {
		this.deletionLayer = new Hashtable<Type, Integer>();
		Enumeration<Type> types = gragra.getTypes();
		while (types.hasMoreElements()) {
			Type t = types.nextElement();
			this.deletionLayer.put(t, Integer.valueOf(0));
		}
	}

	private void reinitDeletionLayer() {
		this.deletionLayer.clear();
		Enumeration<Type> types = this.grammar.getTypes();
		while (types.hasMoreElements()) {
			Type t = types.nextElement();
			this.deletionLayer.put(t, Integer.valueOf(0));
		}
	}
	
	private void initDeletionLayer(int init) {
		for (Enumeration<Type> keys = this.deletionLayer.keys(); keys.hasMoreElements();) {
			Type t = keys.nextElement();
			this.deletionLayer.put(t, Integer.valueOf(init));
		}
	}

	private void initOrderedRuleLayer(GraGra gragra) {
		RuleLayer layer = new RuleLayer(this.listOfRules);
		this.invertedRuleLayer = layer.invertLayer();
		this.startRuleLayer = layer.getStartLayer();
		this.orderedRuleLayerSet = new OrderedSet<Integer>(new IntComparator<Integer>());
		for (Enumeration<Integer> en = this.invertedRuleLayer.keys(); en.hasMoreElements();)
			this.orderedRuleLayerSet.add(en.nextElement());
	}

	private void reinitOrderedRuleLayer() {
		RuleLayer layer = new RuleLayer(this.listOfRules);
		this.invertedRuleLayer = layer.invertLayer();
		this.startRuleLayer = layer.getStartLayer();
		this.orderedRuleLayerSet.clear();
		for (Enumeration<Integer> en = this.invertedRuleLayer.keys(); en.hasMoreElements();)
			this.orderedRuleLayerSet.add(en.nextElement());
	}
	
	private void initOrderedTypeDeletionLayer() {
		TypeLayer layer = new TypeLayer(this.deletionLayer);
		this.invertedTypeDeletionLayer = layer.invertLayer();
//		startLayer = layer.getStartLayer();
		this.invertedTypeDeletionLayer = layer.invertLayer();
		this.orderedTypeDeletionLayerSet = new OrderedSet<Integer>(new IntComparator<Integer>());
		for (Enumeration<Integer> en = this.invertedTypeDeletionLayer.keys(); en
				.hasMoreElements();)
			this.orderedTypeDeletionLayerSet.add(en.nextElement());
	}

	private void initOrderedTypeCreationLayer() {
		TypeLayer layer = new TypeLayer(this.creationLayer);
		this.invertedTypeCreationLayer = layer.invertLayer();
//		startLayer = layer.getStartLayer();
		this.invertedTypeCreationLayer = layer.invertLayer();
		this.orderedTypeCreationLayerSet = new OrderedSet<Integer>(new IntComparator<Integer>());
		for (Enumeration<Integer> en = this.invertedTypeCreationLayer.keys(); 
				en.hasMoreElements();) {
			this.orderedTypeCreationLayerSet.add(en.nextElement());			
		}
	}
	
	private void initResults() {
		this.orderedRuleLayer = new Vector<Integer>();
		this.resultTypeDeletion = new Hashtable<Integer, Pair<Boolean, Vector<Rule>>>();
		this.resultDeletion = new Hashtable<Integer, Pair<Boolean, Vector<Rule>>>();
		this.resultNonDeletion = new Hashtable<Integer, Pair<Boolean, Vector<Rule>>>();
		
		this.errorMsg = new Hashtable<Integer, List<String>>();
		this.errorMsgDeletion1 = new Hashtable<Integer, List<String>>();
		this.errorMsgDeletion2 = new Hashtable<Integer, List<String>>();
		this.errorMsgNonDeletion = new Hashtable<Integer, List<String>>();
	}

	private void reinitResults() {
		this.orderedRuleLayer.clear();
		this.resultTypeDeletion.clear();
		this.resultDeletion.clear();
		this.resultNonDeletion.clear();
		clearErrors();
	}
	
	public void initAll(boolean generate) {
		if (generate) {
			// initRuleLayer(0);
			initRuleLayer(this.oldRuleLayer);
			initCreationLayer(0);
			initDeletionLayer(0);
			initResults();
			this.maxl = 0;
		} else
			resetLayer();
	}

	private boolean isDeleting(Rule r) {
		for (Enumeration<GraphObject> elems = r.getLeft().getElements(); elems
				.hasMoreElements();) {
			GraphObject go = elems.nextElement();
			if (r.getImage(go) == null)
				return true;
		}
		return false;
	}

	private boolean isCreating(Rule r) {
		for (Enumeration<GraphObject> elems = r.getRight().getElements(); elems
				.hasMoreElements();) {
			GraphObject go = elems.nextElement();
			if (!r.getInverseImage(go).hasMoreElements())
				return true;
		}
		return false;
	}

	public Vector<Object> getCreatedTypesOnDeletionLayer(Integer layer) {
		Vector<Object> types = new Vector<Object>();
		for (Enumeration<Rule> en = this.deletionRule.elements(); en.hasMoreElements();) {
			Rule r = en.nextElement();
			for (Enumeration<GraphObject> elems = r.getRight().getElements(); elems
					.hasMoreElements();) {
				GraphObject go = elems.nextElement();
				if (!r.getInverseImage(go).hasMoreElements()) {
					Type t = go.getType();
					Integer tLayer = this.creationLayer.get(t);
					if ((tLayer.intValue() == layer.intValue())
							&& !types.contains(t))
						types.add(t);
				}
			}
		}
		return types;
	}

	private void generateCreationLayer() {
		for (Enumeration<Rule> en = this.nondeletionRule.elements(); en.hasMoreElements();) {
			Rule r = en.nextElement();
			for (Enumeration<Type> types = this.creationLayer.keys(); types
					.hasMoreElements();) {
				Type t = types.nextElement();
				setCreationLayer(r, t);
			}
		}
	}

	private void setCreationLayer(Rule r, Type t) {
		for (Enumeration<GraphObject> en = r.getRight().getElements(); en.hasMoreElements();) {
			GraphObject grob = en.nextElement();
			if (!r.getInverseImage(grob).hasMoreElements()) {
				if (grob.getType().compareTo(t)) {
					Integer rl = this.ruleLayer.get(r);
					Integer cl = this.creationLayer.get(t);
					if (cl.intValue() <= rl.intValue()) {
						int l = rl.intValue() + 1;
						this.creationLayer.put(t, Integer.valueOf(l));
						if (l > this.maxl)
							this.maxl = l;
					}
				}
			}
		}
		// now for preserved graph objects
		for (Enumeration<GraphObject> en = r.getLeft().getElements(); en.hasMoreElements();) {
			GraphObject grob = en.nextElement();
			if (r.getImage(grob) != null) {
				if (grob.getType().compareTo(t)) {
					Integer rl = this.ruleLayer.get(r);
					Integer cl = this.creationLayer.get(t);
					if (cl.intValue() > rl.intValue()) {
						if (this.generateRuleLayer) {
							this.ruleLayer.put(r, Integer.valueOf(cl.intValue()));
							rl = this.ruleLayer.get(r);
							this.needCorrection = true;
						} 
						else {
							this.creationLayer.put(t, Integer.valueOf(rl.intValue()));
						}
					}
				}
			}
		}
	}

	/*
	private boolean increaseRuleLayer(Enumeration<Rule> rules, Type t,
			Rule excludedRule) {
		boolean increased = false;
		while (rules.hasMoreElements()) {
			Rule r = rules.nextElement();
			if (!r.equals(excludedRule)) {
				if (usesType(t, r)) {
					if (layered)
						r.setLayer(r.getLayer() + 1);
					else if (priority)
						r.setPriority(r.getPriority() + 1);
					else
						r.setLayer(r.getLayer() + 1);
					increased = true;
				}
			}
		}
		return increased;
	}

	
	private boolean usesType(Type t, Rule r) {
		for (Enumeration<GraphObject> elems = r.getLeft().getElements(); elems
				.hasMoreElements();) {
			GraphObject go = elems.nextElement();
			if (r.getImage(go) != null)
				if (go.getType().compareTo(t))
					return true;
		}
		return false;
	}

	private void passCreationLayer() {
		for (Enumeration<Rule> en = creationRule.elements(); en.hasMoreElements();) {
			Rule r = en.nextElement();
			for (Enumeration<Type> types = creationLayer.keys(); types
					.hasMoreElements();) {
				Type t = types.nextElement();
				// System.out.print("creation type: <"+t.getStringRepr()+">");
				Integer rl = ruleLayer.get(r);
				Integer cl = creationLayer.get(t);
				if (cl.intValue() <= rl.intValue()) {
					int l = rl.intValue() + 1;
					creationLayer.put(t, Integer.valueOf(l));
					cl = creationLayer.get(t);
					if (l > maxl)
						maxl = l;
				}
			}
		}
	}
*/
	
	private void generateDeletionLayer() {
		if (this.generateRuleLayer) {
			// set rule layer of deletion rules to maxl first
			for (Enumeration<Rule> en = this.deletionRule.elements(); en.hasMoreElements();) {
				Rule r = en.nextElement();
				Integer rl = getRuleLayer().get(r);
				if (rl.intValue() < this.maxl)
					this.ruleLayer.put(r, Integer.valueOf(this.maxl));
			}
		}

		for (Enumeration<Rule> en = this.deletionRule.elements(); en.hasMoreElements();) {
			Rule r = en.nextElement();
			for (Enumeration<Type> types = this.deletionLayer.keys(); types
					.hasMoreElements();) {
				Type t = types.nextElement();
				setDeletionLayer(r, t);
			}
		}
		// set deletion layer of unused types to maxl
		for (Enumeration<Object> en = getDeletionLayer().keys(); en.hasMoreElements();) {
			Type key = (Type) en.nextElement();
			Integer dl = getDeletionLayer().get(key);
			Integer cl = this.creationLayer.get(key);
			if (dl.intValue() < cl.intValue())
				this.deletionLayer.put(key, Integer.valueOf(cl.intValue()));
		}
	}

	private void setDeletionLayer(Rule r, Type t) {
		for (Enumeration<GraphObject> en = r.getLeft().getElements(); en.hasMoreElements();) {
			// first graph objects to delete
			GraphObject grob = en.nextElement();
			if (r.getImage(grob) == null) {
				if (grob.getType().compareTo(t)) {
					Integer rl = this.ruleLayer.get(r);
					Integer cl = this.creationLayer.get(t);
					Integer dl = this.deletionLayer.get(t);

					int l = cl.intValue(); // + 1;
					if (l > this.maxl)
						this.maxl = l;

					if (this.generateRuleLayer && (rl.intValue() < this.maxl)) {
						this.ruleLayer.put(r, Integer.valueOf(this.maxl));
						rl = this.ruleLayer.get(r);
					}

					if (dl.intValue() < cl.intValue()) {
						this.deletionLayer.put(t, Integer.valueOf(this.maxl));
						dl = this.deletionLayer.get(t);
					}
					if ((dl.intValue() > rl.intValue()) && this.generateRuleLayer) {
						this.ruleLayer.put(r, Integer.valueOf(dl.intValue()));
						rl = this.ruleLayer.get(r);
					}
					if (dl.intValue() == 0) {
						this.deletionLayer.put(t, Integer.valueOf(rl.intValue()));
						dl = this.deletionLayer.get(t);
//						System.out.println("setDeletionLayer:: "+r.getName()+"   <"+t.getName()+">   cl: "+cl+"   dl: "+dl);
					}
				}
			}
		}
		// now for new graph objects
		for (Enumeration<GraphObject> en = r.getRight().getElements(); en.hasMoreElements();) {
			GraphObject grob = en.nextElement();
			if (!r.getInverseImage(grob).hasMoreElements()) {
				if (grob.getType().compareTo(t)) {
					Integer rl = this.ruleLayer.get(r);
//					Integer dl = deletionLayer.get(t);
					Integer cl = this.creationLayer.get(t);
					if (cl.intValue() <= rl.intValue()) {
						// if(cl.intValue() < rl.intValue()) {
						this.creationLayer.put(t, Integer.valueOf(rl.intValue() + 1));
						cl = this.creationLayer.get(t);
					}
				}
			}
		}
	}

	private void clearErrors() {
		this.errorMsg.clear();
		this.errorMsgDeletion1.clear();
		this.errorMsgDeletion2.clear();
		this.errorMsgNonDeletion.clear();
	}

	/**
	 * Checks layer conditions .
	 * 
	 * @return true if conditions are valid.
	 */
	public boolean checkTermination() {
		clearErrors();
		
		if (this.generateRuleLayer) {
			initAll(this.generateRuleLayer);
		}
		generateCreationLayer();
		generateDeletionLayer();
		int n = this.listOfRules.size();
		while (this.needCorrection && n >= 0) {
			this.needCorrection = false;
			generateCreationLayer();
			generateDeletionLayer();
			n--;
		}

		// check totality of rule layer function
		for (Iterator<Rule> en = this.listOfRules.iterator(); en.hasNext();) {
			Rule r = en.next();
			Integer rl = this.ruleLayer.get(r);
			/* layer function must be total*/
			if (rl == null) {
				this.errMsg = "Termination check failed.\n\n" + "Rule :  <"
							+ r.getName() + "> \n"
							+ "The condition that \n"
							+ "rl  is a total function \n" + "is not satisfied.";
				return false;
			}
		}
		// check totality of deletion/creation layer functions
		for (Enumeration<Type> en = this.grammar.getTypes(); en.hasMoreElements();) {
			Type t = en.nextElement();
			Integer dl = this.deletionLayer.get(t);
			Integer cl = this.creationLayer.get(t);
			/* layer function must be total */
			if (cl == null) {
				this.errMsg = "Termination check failed.\n\n" + "Type :  <"
						+ t.getStringRepr() + ">\n"
						+ "The condition that \n" + "cl  is total function \n"
						+ "is not satisfied.";
				return false;
			} else if (dl == null) {
				this.errMsg = "Termination check failed.\n\n" + "Type :  <"
						+ t.getStringRepr() + ">\n"
						+ "The condition that\n" + "dl  is total function\n"
						+ "is not satisfied.";
				return false;
			}
		}
		boolean result = checkTerminationConditions();
		
		initOrderedTypeDeletionLayer();
		initOrderedTypeCreationLayer();
		
		this.valid = this.setValidResult();
		
		return result;
	}

	private void addErrorMessage(
			final Hashtable<Integer, List<String>> msgContainer, 
			final Integer key, 
			final String msg) {
		List<String> errList = msgContainer.get(key);
		if (errList == null) {
			errList = new Vector<String>();
			msgContainer.put(key, errList);
		}
		errList.add(this.errMsg);
	}
	
	private boolean checkTerminationConditions() {
		Integer currentLayer = this.startRuleLayer; 
		int i=0;
		boolean nextLayerExists = true;
		while (nextLayerExists && (currentLayer != null)) {
			// get rules for layer
			HashSet<Rule> rulesForLayer = this.invertedRuleLayer.get(currentLayer);
			if (rulesForLayer != null) {
				this.orderedRuleLayer.addElement(currentLayer);

				Vector<Rule> currentRules = new Vector<Rule>();
				Iterator<?> en = rulesForLayer.iterator();
				while (en.hasNext()) {
					Rule rule = (Rule) en.next();
					currentRules.addElement(rule);
				}
			
				if (checkTypeDeletion(currentLayer, currentRules)) {
					Pair<Boolean, Vector<Rule>> value = new Pair<Boolean, Vector<Rule>>(
							Boolean.valueOf(true), currentRules);
					this.resultTypeDeletion.put(currentLayer, value);
				} else {
					Pair<Boolean, Vector<Rule>> value = new Pair<Boolean, Vector<Rule>>(
							Boolean.valueOf(false), currentRules);
					this.resultTypeDeletion.put(currentLayer, value);
				}

				if (checkNonDeletionLayer(currentRules)) {
					Pair<Boolean, Vector<Rule>> value = new Pair<Boolean, Vector<Rule>>(
							Boolean.valueOf(true), currentRules);
					this.resultNonDeletion.put(currentLayer, value);
				} else {
					Pair<Boolean, Vector<Rule>> value = new Pair<Boolean, Vector<Rule>>(
							Boolean.valueOf(false), currentRules);
					this.resultNonDeletion.put(currentLayer, value);
				}

				if (checkDeletionLayer(currentRules)) {
					Pair<Boolean, Vector<Rule>> value = new Pair<Boolean, Vector<Rule>>(
							Boolean.valueOf(true), currentRules);
					this.resultDeletion.put(currentLayer, value);
				} else {
					Pair<Boolean, Vector<Rule>> value = new Pair<Boolean, Vector<Rule>>(
							Boolean.valueOf(false), currentRules);
					this.resultDeletion.put(currentLayer, value);
				}
			}
//			OrderedSetIterator osi = this.orderedRuleLayerSet.find(currentLayer);
//			if ((osi == null) || osi.atEnd())
//				nextLayerExists = false;
//			else {
//				osi.advance();
//				currentLayer = (Integer) osi.get();
//			}
			i++;
			if (i < orderedRuleLayerSet.size()) {
				currentLayer = orderedRuleLayerSet.get(i);
			}
			else {
				nextLayerExists = false;
			}
		}

		return true;
	}

	/**
	 * Checks first (type) deletion condition of rules on a certain layer .
	 * These rules must to delete at least one node or edge of a certain type.
	 * 
	 * @param rules
	 *            belong to the same rule layer
	 * @return true if condition is satisfied.
	 */
	private boolean checkTypeDeletion(Integer layer, Vector<Rule> rules) {
		// Deletion Layer Conditions (1)
		boolean checkOK = true;
		// 1) check: each rule decreases the number of graph items
		for (int j = 0; j < rules.size(); j++) {
			Rule r = rules.elementAt(j);

			// each rule has to delete
			if (this.deletionRule.contains(r)) {
				if (r.getLeft().getSize() < r.getRight().getSize()) {
					checkOK = false;				
					this.errMsg = "Rule <" + r.getName() 
							+ "> does not decrease the number of graph items of one special type.";
					addErrorMessage(this.errorMsgDeletion1, layer, this.errMsg);
					break;
				}
			} else if (/*r.isEmptyRule() && */r.isTriggerOfLayer()) {
				
			}
			else {				
				this.errMsg = "Rule <" + r.getName() 
						+ "> does not decrease the number of graph items.";
				addErrorMessage(this.errorMsgDeletion1, layer, this.errMsg);	
				return false;
			}
		}
		if (checkOK)
			return true;

		// or
		// 2) check: each rule decreases the number of graph items of one
		// special type
		Hashtable<Pair<Type, Object>, Vector<Rule>> deletedType = new Hashtable<Pair<Type, Object>, Vector<Rule>>();
		for (int j = 0; j < rules.size(); j++) {
			Rule r = rules.elementAt(j);
			
			// each rule has to delete is already checked
			// check one special type
			for (Enumeration<GraphObject> en = r.getLeft().getElements(); en
					.hasMoreElements();) {
				GraphObject o = en.nextElement();
				if (r.getImage(o) == null) {
					boolean containsKey = false;
					Type t = o.getType();
					Pair<Type, Object> delt = new Pair<Type, Object>(t, null);
					if (o.isArc())
						delt = new Pair<Type, Object>(t, new Pair<Type, Type>(
								((Arc) o).getSource().getType(), ((Arc) o)
										.getTarget().getType()));
				
					Pair<Type, Object> t1 = null;
					Enumeration<Pair<Type, Object>> e = deletedType.keys();
					while (e.hasMoreElements()) {
						t1 = e.nextElement();
						if (t.isRelatedTo(t1.first)) {
							if (t1.second == null && delt.second == null) {
								containsKey = true;
								break;
							}
							if (t1.second != null && delt.second != null) {
								Pair<?,?> t1sec = (Pair<?,?>) t1.second;
								Pair<?,?> deltsec = (Pair<?,?>) delt.second;
								if (((Type)deltsec.first).isRelatedTo((Type)t1sec.first)
										&& ((Type)deltsec.second)
												.isRelatedTo((Type)t1sec.second)) {
									containsKey = true;
									break;
								}
							}
						}
					}
					if (containsKey) {
						if (!deletedType.get(t1).contains(r))
							deletedType.get(t1).add(r);
					} else {
						Vector<Rule> v = new Vector<Rule>(rules.size());
						v.add(r);
						deletedType.put(delt, v);
					}
				}
			}
		}
		
		Vector<Type> ltypes = new Vector<Type>();
		for (Enumeration<Pair<Type, Object>> en = deletedType.keys(); en.hasMoreElements();) {
			Pair<Type, Object> key = en.nextElement();
			Type t = key.first;
			Vector<Rule> v = deletedType.get(key);		
			if (v.size() == rules.size()) {
				for (int j = 0; j < rules.size(); j++) {
					Rule r = rules.elementAt(j);
					if (key.second == null) { // node type
						if (r.getLeft().getElementsOfTypeAsVector(t).size() <= r
								.getRight().getElementsOfTypeAsVector(t).size()) {							
							this.errMsg = "Rule <" + r.getName()
									+ "> does not decrease the number of graph items of one special type <"
									+t.getName()+">";
							addErrorMessage(this.errorMsgDeletion1, layer, this.errMsg);
							return false;
						}
					} else { // arc type
						if (r.getLeft().getElementsOfTypeAsVector(t,
								(Type) ((Pair<?,?>) key.second).first,
								(Type) ((Pair<?,?>) key.second).second).size() <= r
								.getRight().getElementsOfTypeAsVector(t,
										(Type) ((Pair<?,?>) key.second).first,
										(Type) ((Pair<?,?>) key.second).second)
								.size()) {							
							this.errMsg = "Rule <" + r.getName()
									+ "> does not decrease the number of graph items of one special type <"
									+t.getName()+">";
							addErrorMessage(this.errorMsgDeletion1, layer, this.errMsg);
							return false;
						}
					}
				}
				ltypes.add(t);
			}
		}
		if (ltypes.size() != 0) {
			this.deletionType.put(layer, ltypes);
			return true;
		} 
		this.errMsg = "Rules do not decrease the number of graph items.";
		addErrorMessage(this.errorMsgDeletion1, layer, this.errMsg);
		return false;
	}

	/**
	 * Checks deletion cond. of rules on a certain layer.
	 * 
	 * @return true if condition is satisfied.
	 */
	private boolean checkDeletionLayer(Vector<Rule> rules) {
		// Deletion Layer Conditions (2)
		boolean result = true;
		HashSet<GraphObject> deletionSet = new HashSet<GraphObject>();
		HashSet<GraphObject> creationSet = new HashSet<GraphObject>();
		for (int j = 0; j < rules.size(); j++) {
			deletionSet.clear();
			creationSet.clear();
			Rule rule = rules.elementAt(j);
		
			if (this.deletionRule.contains(rule)) {
				Integer rl = this.ruleLayer.get(rule);

				Graph leftGraph = rule.getLeft();
				Graph rightGraph = rule.getRight();
				/* alle geloeschten Objekte suchen */
				for (Enumeration<GraphObject> en = leftGraph.getElements(); en
						.hasMoreElements();) {
					GraphObject grob = en.nextElement();
					if (rule.getImage(grob) == null)
						deletionSet.add(grob);
				}
				/* 1. is deleting at least one item */
				if (deletionSet.isEmpty()) {
					result = false;				
					this.errMsg = "Rule <"+ rule.getName() + ">"
							+"  does not delete at least one graph item.";	
					addErrorMessage(this.errorMsgDeletion2, rl, this.errMsg);
					break;
				}

				/* 2. 0<= cl(l)<=dl(l)<=n */
				for (Enumeration<Object> en = getDeletionLayer().keys(); en
						.hasMoreElements()
						&& result;) {
					Type key = (Type) en.nextElement();
					Integer dl = getDeletionLayer().get(key);
					Integer cl = getCreationLayer().get(key);
					if (!(0 <= cl.intValue())
							|| !(cl.intValue() <= dl.intValue())) {
						result = false;
						this.errMsg = "Type  <"
								+ key.getStringRepr()+ ">"
								+ ", rl = "+rl.intValue()
								+ ", cl = "+cl.intValue()
								+ ", dl = "+ dl.intValue()
								+ "  do not satisfy condition:"
								+ " 0 <= cl <= dl <= rl";
						addErrorMessage(this.errorMsgDeletion2, dl, this.errMsg);
						break;
					}
				}

				/* 3. dl(l) <= rl(r) */
				for (Iterator<?> en = deletionSet.iterator(); en.hasNext()
						&& result;) {
					GraphObject grob = (GraphObject) en.next();
					Type t = grob.getType();
					Integer dl = getDeletionLayer().get(t);
					if (dl.intValue() > rl.intValue()) {
						result = false;
						this.errMsg = "Rule <"+ rule.getName()+ ">, "
								+ "Type <"+ t.getStringRepr()+ ">"
								+ ", dl = "+ dl.intValue()
								+ ", rl = "+rl.intValue()
								+ " do not satisfy condition"
								+ " dl <= rl.";
						addErrorMessage(this.errorMsgDeletion2, rl, this.errMsg);
						break;
					}
				}
				if (!result)
					break;

				/* alle erzeugten Objekte suchen */
				for (Enumeration<GraphObject> en = rightGraph.getElements(); en
						.hasMoreElements();) {
					GraphObject grob = en.nextElement();
					if (!rule.getInverseImage(grob).hasMoreElements())
						creationSet.add(grob);
				}
				
				/* 4. cl(l) > rl(r) */
				for (Iterator<?> en = creationSet.iterator(); en.hasNext()
						&& result;) {
					GraphObject grob = (GraphObject) en.next();
					Type t = grob.getType();
					Integer cl = getCreationLayer().get(t);
					if (cl.intValue() <= rl.intValue()) {
						result = false;
						this.errMsg = "Rule <"+ rule.getName()+ ">, "
								+ "Type <"+ t.getStringRepr()+ ">"
								+ ", cl = "+ cl.intValue()
								+ ", rl = "+rl.intValue()
								+ " do not satisfy condition"
								+ " cl > rl.";
						addErrorMessage(this.errorMsgDeletion2, rl, this.errMsg);
						break;
					}
				}
			} else {
				result = false;
				break;
			}
		}

		return result;
	}

	/**
	 * Checks nondeletion cond. of rules on a certain layer.
	 * 
	 * @return true if condition is satisfied.
	 */
	private boolean checkNonDeletionLayer(Vector<Rule> rules) {
		// Creation Layer Conditions (3)
		boolean result = true;
		HashSet<GraphObject> preservedSet = new HashSet<GraphObject>();
		HashSet<GraphObject> creationSet = new HashSet<GraphObject>();
		
		for (int j = 0; j < rules.size(); j++) {
			Rule rule = rules.elementAt(j);
			
			int errKey = rule.getLayer();
			if (this.priority)
				errKey = rule.getPriority();
			
			if (this.nondeletionRule.contains(rule)) {
				
				/* rule is total */
				if (!rule.isTotal()) {
					this.errMsg = "Rule <" + rule.getName()
							+ "> is not total.";
					addErrorMessage(this.errorMsgNonDeletion, new Integer(errKey), this.errMsg);
					return false;
				}
				/* 1. rule is injective */
				if (!rule.isInjective()) {
					this.errMsg = "Rule <" + rule.getName()
							+ "> is not injective.";
					addErrorMessage(this.errorMsgNonDeletion, new Integer(errKey), this.errMsg);
					return false;
				}
				/* 2. rule has a NAC */
				else if (rule.isCreating()
						&& rule.getNACsList().isEmpty()) {
					this.errMsg = "Rule <" + rule.getName()
							+ "> does not have any NAC.";
					addErrorMessage(this.errorMsgNonDeletion, new Integer(errKey), this.errMsg);
					return false;
				} 
				/* 2. NAC : L -> N with N -> R injective */
				else if (!this.ruleWithRightInjNAC(errKey, rule)) {						
					return false;
				}

				Integer rl = this.ruleLayer.get(rule);
				creationSet.clear();
				preservedSet.clear();
				Graph leftGraph = rule.getLeft();
				Graph rightGraph = rule.getRight();
				/* alle erhaltende Objekte suchen */
				for (Enumeration<GraphObject> en = leftGraph.getElements(); en
						.hasMoreElements();) {
					GraphObject grob = en.nextElement();
					if (rule.getImage(grob) != null)
						preservedSet.add(grob);
				}

				/* alle erzeugten Objekte suchen */
				for (Enumeration<GraphObject> en = rightGraph.getElements(); en
						.hasMoreElements();) {
					GraphObject grob = en.nextElement();
					if (!rule.getInverseImage(grob).hasMoreElements())
						creationSet.add(grob);
				}

				/* 3. for preserved objects: cl(l) <= rl(r) */
				for (Iterator<?> en = preservedSet.iterator(); en.hasNext()
						&& result;) {
					GraphObject grob = (GraphObject) en.next();
					Type t = grob.getType();
					Integer cl = getCreationLayer().get(t);
					if (cl.intValue() > rl.intValue()) {
//						if (this.ruleHasRightInjectiveNAC(rule, t)) {}
//						else 
						{
							result = false;
							this.errMsg = "Nondeletion Layer Condition ( Nondeletion ): \n"
									+ "Termination check failed.\n\n"
									+ "Rule :  <"
									+ rule.getName()
									+ "> \n"
									+ "Type :  <"
									+ t.getStringRepr()
									+ "> \n"
									+ "The condition that \n"
									+ "r preserves nodes and edges with label \n"
									+ "l  such that  cl(l) <= rl(r) \n"
									+ "is not satisfied. \n"
									+ "( cl = "
									+ cl.intValue()
									+ " , "
									+ "rl = "
									+ rl.intValue() + " )";
							break;
						}
					}
				}

				/* 4. for created objects: cl(l) > rl(r) */
				for (Iterator<?> en = creationSet.iterator(); en.hasNext()
						&& result;) {
					GraphObject grob = (GraphObject) en.next();
					Type t = grob.getType();
					Integer cl = getCreationLayer().get(t);
					if (cl.intValue() <= rl.intValue()) {
						result = false;
						
						this.errMsg = "Nondeletion Layer Condition ( Nondeletion ): \n"
								+ "Termination check failed.\n\n"
								+ "Rule :  <"
								+ rule.getName()
								+ "> \n"
								+ "Type :  <"
								+ t.getStringRepr()
								+ "> \n"
								+ "The condition that \n"
								+ "r  creates only nodes and edges with label \n"
								+ "l  such that  cl(l) > rl(r) \n"
								+ "is not satisfied. \n"
								+ "( cl = "
								+ cl.intValue()
								+ " , "
								+ "rl = "
								+ rl.intValue() + " )";
						
						break;
					}
				}
			} else {
				result = false;
				break;
			}
		}
		return result;
	}

	/** exists a NAC : L -> N with N -> R injective 
	 */
	private boolean ruleWithRightInjNAC(int errKey, final Rule rule) {
		/* 2. NAC : L -> N with N -> R injective */
		final List<OrdinaryMorphism> nacs = rule.getNACsList();
		if (nacs.isEmpty()) {
			return false;
		}
		
		boolean result = false;
		for (int l=0; l<nacs.size() && !result; l++) {
			final OrdinaryMorphism nac = nacs.get(l);	
			if (nac.isEnabled()) {
				boolean failed = false;
				OrdinaryMorphism nprime = BaseFactory.theFactory()
						.createMorphism(nac.getTarget(),
								rule.getRight());
				nprime.setCompletionStrategy(new Completion_InjCSP());
				Enumeration<GraphObject> dom = rule.getDomain();
				while (dom.hasMoreElements()) {
					GraphObject grob = dom.nextElement();
					GraphObject nacob = nac.getImage(grob);
					if (nacob != null) {
						try {
							nprime.addMapping(nacob, rule
									.getImage(grob));
						} catch (agg.xt_basis.BadMappingException ex) {
							failed = true;
							break;
						}
					}
				}	
				// at least one NAC exists so that n':N->R injective 
				if (!failed)
					result = nprime.nextCompletionWithConstantsChecking();
			}
		}
		
		if (!result) {
			this.errMsg = "Rule <"+ rule.getName()+ "> "
					+ "does not have any right injective NACs.";
			addErrorMessage(this.errorMsgNonDeletion, new Integer(errKey), this.errMsg);
		}
		return result;					
	}
	
	/* exists a NAC : L -> N with N -> R injective 
	 */
	/*
	private boolean ruleHasRightInjectiveNAC(int errKey, final Rule rule, final Type t) {
		// 2. NAC : L -> N with N -> R injective 
		final List<OrdinaryMorphism> nacs = rule.getNACsList();
		if (nacs.isEmpty()) {
			return false;
		}
		
		boolean result = false;
		for (int l=0; l<nacs.size(); l++) {
			final OrdinaryMorphism nac = nacs.get(l);		
			final Enumeration<GraphObject> ttyped = nac.getTarget().getElementsOfType(t);
			if (!ttyped.hasMoreElements()) {
				continue;
			}
			boolean fobidden = false;
			while (ttyped.hasMoreElements()) {
				final GraphObject o = ttyped.nextElement();
				if (!nac.getInverseImage(o).hasMoreElements()) {
					fobidden = true;
					break;
				}
			}
			if (!fobidden) {
				continue;
			}
			
			OrdinaryMorphism nprime = BaseFactory.theFactory()
					.createMorphism(nac.getTarget(),
							rule.getRight());
			nprime.setCompletionStrategy(new Completion_InjCSP());
			Enumeration<GraphObject> dom = rule.getDomain();
			while (dom.hasMoreElements()) {
				GraphObject grob = dom.nextElement();
				GraphObject nacob = nac.getImage(grob);
				if (nacob != null) {
					try {
						nprime.addMapping(nacob, rule
								.getImage(grob));
						if (nprime.getImage(nacob) == null) {
							this.errMsg = "Rule <"+ rule.getName()+ "> : "
									+ "Mapping of N': N->R accross  N<-L->R  failed.";
							addErrorMessage(this.errorMsgNonDeletion, new Integer(errKey), this.errMsg);
							return false;
						}
					} catch (agg.xt_basis.BadMappingException ex) {
						this.errMsg = "Rule <"+ rule.getName()+ "> : "
								+ "Mapping of N': N->R accross  N<-L->R  failed.";
						addErrorMessage(this.errorMsgNonDeletion, new Integer(errKey), this.errMsg);
						return false;
					}
				}
			}

			// at least one NAC exists so that n':N->R injective 
			result = true;
			if (!nprime.nextCompletionWithConstantsChecking()) {
				this.errMsg = "Rule <"+ rule.getName()+ "> "
						+ "does not have any right injective NACs.";			
				addErrorMessage(this.errorMsgNonDeletion, new Integer(errKey), this.errMsg);
				result = false;
			}
		}
		
		return result;					
	}
	*/

	/**
	 * A fast check on validity.
	 * 
	 * @return true if the layer function is valid.
	 */
	public boolean isValid() {
		return this.valid;
	}
	
	private boolean setValidResult() {
		boolean result = true;
		for (int i = 0; i < this.orderedRuleLayer.size(); i++) {
			Integer currentLayer = this.orderedRuleLayer.elementAt(i);
//			System.out.println("Layer: "+currentLayer.intValue());
			boolean localresult = false;
			
			Pair<Boolean, Vector<Rule>> p = this.resultTypeDeletion.get(currentLayer);
			if (p != null) { 
				localresult = p.first.booleanValue();
			}
			
			if (!localresult) {
				p = this.resultNonDeletion.get(currentLayer);
				localresult = p.first.booleanValue();
				if (!localresult) {
					p = this.resultDeletion.get(currentLayer);
					localresult = p.first.booleanValue();
					if (localresult) {
//						System.out.println("Layer: "+currentLayer.intValue()+"  Deletion_2: "+localresult);
						this.errorMsgDeletion1.remove(currentLayer);
						this.errorMsgDeletion2.remove(currentLayer);
						this.errorMsgNonDeletion.remove(currentLayer);
					}
				} else {
//					System.out.println("Layer: "+currentLayer.intValue()+"  NonDeletion: "+localresult);
					this.errorMsgDeletion1.remove(currentLayer);
					this.errorMsgDeletion2.remove(currentLayer);
					this.errorMsgNonDeletion.remove(currentLayer);
				}
			} else {
//				System.out.println("Layer: "+currentLayer.intValue()+" Deletion_1:  "+localresult);
				this.errorMsgDeletion1.remove(currentLayer);
				this.errorMsgDeletion2.remove(currentLayer);
				this.errorMsgNonDeletion.remove(currentLayer);
			}
			
			result = result && localresult;
		}
		
		return result;
	}

	/**
	 * Returns an error message if the layer function is not valid.
	 * 
	 * @return The error message.
	 */
	public String getErrorMessage() {
		String str = getErrorOfTypeDeletion(10);
		String str1 = getErrorOfDeletion(10);
		String str2 = getErrorOfNonDeletion(10);
		if (!str1.equals("")) {
			str = str.concat("\n\n");
			str = str.concat(str1);
		}
		if (!str2.equals("")) {
			str = str.concat("\n\n");
			str = str.concat(str2);
		}
		return str;
	}

	private String getErrorOfTypeDeletion(int maxErrors) {
		int n = 0;
		String str0 = "*** (Type) Deletion Layer Condition ( Deletion_1 ) ***";
		String str = "";
		Enumeration<Integer> keys = this.errorMsgDeletion1.keys();
		while (keys.hasMoreElements() && n<=maxErrors) {
			final List<String> list = this.errorMsgDeletion1.get(keys.nextElement());
			for (int i=0; i<list.size(); i++) {
				str = str.concat("\n");
				str = str.concat(list.get(i));
				n++;
				if (n>maxErrors) {
					str = str.concat("\n ... ");
					break;
				}
			}
		}
		if (!str.equals(""))
			str = str0.concat(str);
		
		return str;
	}
	
	private String getErrorOfDeletion(int maxErrors) {
		int n = 0;
		String str0 = "*** Deletion Layer Condition ( Deletion_2 ) ***";
		String str = "";
		Enumeration<Integer> keys = this.errorMsgDeletion2.keys();
		while (keys.hasMoreElements() && n<=maxErrors) {
			final List<String> list = this.errorMsgDeletion2.get(keys.nextElement());
			for (int i=0; i<list.size(); i++) {
				str = str.concat("\n");
				str = str.concat(list.get(i));
				n++;
				if (n>maxErrors) {
					str = str.concat("\n ... ");
					break;
				}
			}
		}
		if (!str.equals(""))
			str = str0.concat(str);
		
		return str;
	}
	
	private String getErrorOfNonDeletion(int maxErrors) {
		int n = 0;
		String str0 = "*** Nondeletion Layer Condition ( Nondeletion ) ***";
		String str = "";
		Enumeration<Integer> keys = this.errorMsgNonDeletion.keys();
		while (keys.hasMoreElements() && n<=maxErrors) {
			final List<String> list = this.errorMsgNonDeletion.get(keys.nextElement());
			for (int i=0; i<list.size(); i++) {
				str = str.concat("\n");
				str = str.concat(list.get(i));
				if (n>maxErrors) {
					str = str.concat("\n ... ");
					break;
				}
			}
		}
		if (!str.equals(""))
			str = str0.concat(str);
		
		return str;
	}

	/**
	 * Returns the rule layer of the layer function.
	 * 
	 * @return The rule layer.
	 */
	public Hashtable<Rule, Integer> getRuleLayer() {
		int size = this.listOfRules.size();
		
		if (size != this.ruleLayer.size()) {
			initRuleLayer(this.grammar);
			return this.ruleLayer;
		}

		Iterator<Rule> en = this.listOfRules.iterator();
		while (en.hasNext()) {
			Object key = en.next();
			if (!this.ruleLayer.containsKey(key)) {
				initRuleLayer(this.grammar);
				return this.ruleLayer;
			}
		}
		
		return this.ruleLayer;
	}

	public int getRuleLayer(Rule r) {
		if (this.ruleLayer.containsKey(r))
			return this.ruleLayer.get(r).intValue();
		
		return 0;
	}

	/**
	 * The result table maps a Type to a layer on which it is created.
	 * @return The creation layer.
	 */
	public Hashtable<Object, Integer> getCreationLayer() {
		int size = 0;
		Enumeration<Type> en = this.grammar.getTypes();
		while (en.hasMoreElements()) {
			en.nextElement();
			size++;
		}
		if (size != this.creationLayer.size()) {
			initCreationLayer(this.grammar);
			return new Hashtable<Object, Integer>(this.creationLayer);
		}

		en = this.grammar.getTypes();
		while (en.hasMoreElements()) {
			Object key = en.nextElement();
			if (!this.creationLayer.containsKey(key)) {
				initCreationLayer(this.grammar);
				return new Hashtable<Object, Integer>(this.creationLayer);
			}
		}
		
		return new Hashtable<Object, Integer>(this.creationLayer);
	}
	
	/**
	 * Returns creation layer of the specified type.
	 */
	public int getCreationLayer(Type t) {
		if (this.creationLayer.containsKey(t))
			return this.creationLayer.get(t).intValue();
		
		return 0;
	}

	/**
	 * The result table maps a Type to a layer on which it is deleted.
	 * @return The deletion layer.
	 */
	public Hashtable<Object, Integer> getDeletionLayer() {
		int size = 0;
		Enumeration<Type> en = this.grammar.getTypes();
		while (en.hasMoreElements()) {
			en.nextElement();
			size++;
		}

		if (size != this.deletionLayer.size()) {
			initDeletionLayer(this.grammar);
			return new Hashtable<Object, Integer>(this.deletionLayer);
		}

		en = this.grammar.getTypes();
		while (en.hasMoreElements()) {
			Object key = en.nextElement();
			if (!this.deletionLayer.containsKey(key)) {
				initDeletionLayer(this.grammar);
				return new Hashtable<Object, Integer>(this.deletionLayer);
			}
		}
		
		return new Hashtable<Object, Integer>(this.deletionLayer);
	}
	
	/**
	 * Returns deletion layer of the specified type.
	 */
	public int getDeletionLayer(Type t) {
		if (this.deletionLayer.containsKey(t))
			return this.deletionLayer.get(t).intValue();
		
		return 0;
	}

	/**
	 * Returns the smallest layer of the rule layer.
	 * 
	 * @return The smallest layer.
	 */
	public Integer getStartLayer() {
		int startL = Integer.MAX_VALUE;
		Integer result = null;
		for (Enumeration<Rule> keys = this.ruleLayer.keys(); keys.hasMoreElements();) {
			Object key = keys.nextElement();
			Integer layer = this.ruleLayer.get(key);
			if (layer.intValue() < startL) {
				startL = layer.intValue();
				result = layer;
			}
		}
		return result;
	}

	/**
	 * Inverts a layer function so that the layer is the key and the value is a
	 * set.
	 * 
	 * @param layer
	 *            The layer function will be inverted.
	 * @return The inverted layer function.
	 */
	public Hashtable<Integer, HashSet<Rule>> invertLayer(
			Hashtable<Rule, Integer> layer) {
		Hashtable<Integer, HashSet<Rule>> inverted = new Hashtable<Integer, HashSet<Rule>>();
		for (Enumeration<Rule> keys = layer.keys(); keys.hasMoreElements();) {
			Rule key = keys.nextElement();
			Integer value = layer.get(key);
			HashSet<Rule> invertedValue = inverted.get(value);
			if (invertedValue == null) {
				invertedValue = new HashSet<Rule>();
				invertedValue.add(key);
				inverted.put(value, invertedValue);
			} else {
				invertedValue.add(key);
			}
		}
		return inverted;
	}

	public void saveRuleLayer() {
		for (Enumeration<Rule> keys = this.ruleLayer.keys(); keys.hasMoreElements();) {
			Rule r = keys.nextElement();
			Integer layer = this.ruleLayer.get(r);
			if (this.layered)
				r.setLayer(layer.intValue());
			else if (this.priority)
				r.setPriority(layer.intValue());
			else
				r.setLayer(layer.intValue());
		}
		saveRuleLayerInto(this.oldRuleLayer);
	}

	private void saveRuleLayerInto(Hashtable<Rule, Integer> table) {
		for (Iterator<Rule> e = this.listOfRules.iterator(); e.hasNext();) {
			Rule r = e.next();			
			if (this.layered)
				table.put(r, Integer.valueOf(r.getLayer()));
			else if (this.priority)
				table.put(r, Integer.valueOf(r.getPriority()));
			else
				table.put(r, Integer.valueOf(r.getLayer()));
		}
	}

	public void setGenerateRuleLayer(boolean b) {
		this.generateRuleLayer = b;
	}

	public void showLayer() {
		 System.out.println(" RULE LAYER");
		for (Enumeration<Rule> keys = this.ruleLayer.keys(); keys.hasMoreElements();) {
			Rule r = keys.nextElement();
			Integer layer = this.ruleLayer.get(r);
			 System.out.println(layer.intValue()+" "+r.getName());
		}
		 System.out.println(" CREATION LAYER");
		for (Enumeration<Type> keys = this.creationLayer.keys(); keys.hasMoreElements();) {
			Type t = keys.nextElement();
			Integer layer = this.creationLayer.get(t);
			 System.out.println(layer.intValue()+" "+t.getStringRepr());
		}
		 System.out.println(" DELETION LAYER");
		for (Enumeration<Type> keys = this.deletionLayer.keys(); keys.hasMoreElements();) {
			Type t = keys.nextElement();
			Integer layer = this.deletionLayer.get(t);
			 System.out.println(layer.intValue()+" "+t.getStringRepr());
		}
	}

	/**
	 * Returns the layer function in a human readable way.
	 * 
	 * @return The text.
	 */
	public String toString() {
		String resultString = super.toString() + " LayerFunction:\n";
		resultString += "\tRuleLayer:\n";
		resultString += getRuleLayer().toString() + "\n";
		resultString += "\tCreationLayer:\n";
		resultString += getCreationLayer().toString() + "\n";
		resultString += "\tDeletionLayer:\n";
		resultString += getDeletionLayer().toString() + "\n";
		return resultString;
	}

	public int getCreationLayer(GraphObject t) {
		return 0;
	}
	
	public int getDeletionLayer(GraphObject t) {		
			return 0;
	}

	/* (non-Javadoc)
	 * @see agg.termination.TerminationLGTSInterface#getDeletionTypeObject()
	 */
	public Hashtable<Integer, Vector<GraphObject>> getDeletionTypeObject() {
		return null;
	}
	
}
