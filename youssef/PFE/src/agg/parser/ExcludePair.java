/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.parser;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import agg.attribute.AttrContext;
import agg.attribute.AttrInstance;
import agg.attribute.AttrMapping;
import agg.attribute.AttrType;
import agg.attribute.handler.HandlerExpr;
import agg.attribute.impl.AttrTupleManager;
import agg.attribute.impl.CondMember;
import agg.attribute.impl.CondTuple;
import agg.attribute.impl.ContextView;
import agg.attribute.impl.DeclTuple;
import agg.attribute.impl.ValueMember;
import agg.attribute.impl.ValueTuple;
import agg.attribute.impl.VarMember;
import agg.attribute.impl.VarTuple;
import agg.parser.ExcludePairContainer.Entry;
import agg.util.Pair;
import agg.util.Triple;
import agg.xt_basis.Arc;
import agg.xt_basis.BadMappingException;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.CompletionStrategySelector;
import agg.xt_basis.Completion_InjCSP;
import agg.xt_basis.GraGra;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.Match;
import agg.xt_basis.MorphCompletionStrategy;
import agg.xt_basis.Node;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.TestStep;
import agg.xt_basis.Type;
import agg.xt_basis.TypeException;
import agg.xt_basis.TypeSet;
import agg.xt_basis.csp.CompletionPropertyBits;
import agg.xt_basis.csp.Completion_InheritCSP;

/**
 * This class implements an algorithm to decide whether two rules are in conflict:
 * the first rule prohibits the second.
 * 
 * @author $Author: olga $
 * @version $Id: ExcludePair.java,v 1.147 2010/12/16 17:32:14 olga Exp $
 */
public class ExcludePair implements CriticalPair {
	
	protected int cpdKind = -1;
	
	protected boolean computable = true;
		
	protected boolean checkSwitchDependency;
	
	protected boolean dependencyCond1;
	
	protected boolean dependencyCond2;

	protected boolean withNACs = true;

	protected boolean withPACs = true;

	protected boolean r1hasEnabledNACs, r1hasEnabledPACs;

	protected boolean r2hasEnabledNACs, r2hasEnabledPACs;

	protected boolean complete = true; 
	
	protected int maxBoundOfCriticKind = 0; // <=0 unbound
	
	public boolean reduceSameMatch; 
	
	protected boolean consistentOnly;

	protected boolean dangling = true;

	protected MorphCompletionStrategy strategy;

	protected boolean ownStrategy = true;

	protected boolean ignoreIdenticalRules;

	protected GraGra grammar;

	protected boolean stop;

	protected boolean inclAsGraph;

	/* Global help containers needed for rule analysis and more */
	protected Vector<Pair<Type, Pair<Type, Type>>> 
	typesTG_L2, typesTG_NAC2, typesTG_PAC2;

	protected Vector<GraphObject> contextC1_L1, boundB1_L1, preservedK1_L1;

	protected Vector<GraphObject> contextC1_R1, boundB1_R1, preservedK1_R1;

	protected Vector<GraphObject> delete, produce, preservedChanged, danglingEdges;
	
	protected boolean criticalNACOfR2exists;
	
	protected boolean essential;
	
	public boolean strongAttrCheck;

	protected int levelOfTypeGraphCheck;
 
	protected boolean consistCheck, withNACsCheck, withPACsCheck;
	protected boolean directStrctCnfl, directStrctCnflUpToIso;
	
	private OrdinaryMorphism nacInsideOverlapGraph;
	
	private Hashtable<ValueMember, Pair<String, String>> attrMember2Expr;
	
	protected boolean withInheritance;
	
	protected boolean equalVariableNameOfAttrMapping;
	
	protected boolean danglEdge;
	
	protected boolean namedObjectOnly;
	
	protected int inclCount, inclProgress;
		
	protected Hashtable<OrdinaryMorphism,Pair<OrdinaryMorphism, OrdinaryMorphism>> ac2leftExtended; //NAC | PAC
	 
	protected int duIndx, pfIndx=-1, caIndx; 
	protected String duIndxStr, pfIndxStr, caIndxStr; 
	
	long freeM, usedM;
	
	/**
	 * Creates a new object to compute critical pairs.
	 */
	public ExcludePair() {
		((AttrTupleManager) agg.attribute.impl.AttrTupleManager
				.getDefaultManager()).setVariableContext(true);
		this.typesTG_L2 = new Vector<Pair<Type, Pair<Type, Type>>>(5);
		this.typesTG_NAC2 = new Vector<Pair<Type, Pair<Type, Type>>>(5);
		this.typesTG_PAC2 = new Vector<Pair<Type, Pair<Type, Type>>>(5);
		this.contextC1_L1 = new Vector<GraphObject>(5);
		this.boundB1_L1 = new Vector<GraphObject>(5);
		this.preservedK1_L1 = new Vector<GraphObject>(5);
		this.contextC1_R1 = new Vector<GraphObject>(5);
		this.boundB1_R1 = new Vector<GraphObject>(5);
		this.preservedK1_R1 = new Vector<GraphObject>(5);
		this.delete = new Vector<GraphObject>(5);
		this.produce = new Vector<GraphObject>(5);
		this.preservedChanged = new Vector<GraphObject>(5);
		this.danglingEdges = new Vector<GraphObject>(5);
		this.attrMember2Expr = new Hashtable<ValueMember, Pair<String, String>> (2);
		this.duIndx=-1; this.pfIndx=-1; this.caIndx=-1;
		this.duIndxStr="-1:"; this.pfIndxStr="-1:-1:"; this.caIndxStr="-1:"; 
	}

	protected void clear() {
		this.typesTG_L2.clear();
		this.typesTG_NAC2.clear();
		this.typesTG_PAC2.clear(); 
		this.contextC1_L1.clear();
		this.boundB1_L1.clear(); 
		this.preservedK1_L1.clear();
		this.contextC1_R1.clear();
		this.boundB1_R1.clear();
		this.preservedK1_R1.clear(); 
		this.delete.clear(); 
		this.produce.clear();
		this.preservedChanged.clear();
		if (this.ac2leftExtended != null) 
			this.ac2leftExtended.clear();
		if (this.attrMember2Expr != null) 
			this.attrMember2Expr.clear();
	}
	
	public void dispose() {
		this.clear();
		
		this.typesTG_L2 = null;
		this.typesTG_NAC2 = null;
		this.typesTG_PAC2 = null;
		this.contextC1_L1 = null;
		this.boundB1_L1 = null;
		this.preservedK1_L1 = null;
		this.contextC1_R1 = null;
		this.boundB1_R1 = null;
		this.preservedK1_R1 = null;
		this.delete = null;
		this.produce = null;
		this.preservedChanged = null;
		this.ac2leftExtended = null;
		this.attrMember2Expr = null;
		this.doneOverlaps = null;
		if (this.ownStrategy)
			this.strategy = null;
	}

	public void stop() {
		this.stop = true;
	}
	
	public void setGraGra(final GraGra grammar) {
		this.grammar = grammar;
	}
	
	/**
	 * Returns the number of kind of pairs which will be distinguished. There
	 * must be at least two kind of pairs. That means one kind has no conflicts
	 * and the second kind has conflicts.
	 * 
	 * @return The number of available algorithms.
	 */
	public int getNumberOfKindOfPairs() {
		return 2;
	}

	public void enableComplete(boolean enable) {
		this.complete = enable;
	}
	
	/**
	 * Returns the current maximum number of inclusions to check.
	 */
	public int getNumberOfInclusions() {
		return this.inclCount;
	}
	
	/**
	 * Returns the progress index of the current maximum number of inclusions to check.
	 */
	public int getProgressOfInclusions() {
		return this.inclProgress;
	}
	
	/**
	 * Whether NACs are enabled or not this decision is done by the NAC property
	 * bit of the morphism completion strategy:
	 * <code> withNACs = strategy.getProperties().get(CompletionPropertyBits.NAC); </code>
	 * 
	 * @deprecated replaced by setMorphismCompletionStrategy(MorphCompletionStrategy strat)
	 */
	public void enableNACs(boolean enable) {
		this.withNACs = enable;
	}

	/**
	 * Whether PACs are enabled or not this decision is done by the PAC property
	 * bit of the morphism completion strategy:
	 * <code> withPACs = strategy.getProperties().get(CompletionPropertyBits.PAC); </code>
	 * 
	 * @deprecated replaced by setMorphismCompletionStrategy(MorphCompletionStrategy strat)
	 */
	public void enablePACs(boolean enable) {
		this.withPACs = enable;
	}
	
	public void enableDirectlyStrictConfluent(boolean enable) {
		this.directStrctCnfl = enable;
	}
	
	public void enableDirectlyStrictConfluentUpToIso(boolean enable) {
		this.directStrctCnflUpToIso = enable;
	}
	
	public void enableReduce(boolean enable) {
		this.essential = enable;
	}
	
	public void enableReduceSameMatch(boolean enable) {
		this.reduceSameMatch = enable;
	}
	
	public void enableStrongAttrCheck(boolean enable) {
		this.strongAttrCheck = enable;
	}
	
	public void enableEqualVariableNameOfAttrMapping(boolean enable) {
		this.equalVariableNameOfAttrMapping = enable;
	}
	
	public void enableConsistent(boolean enable, GraGra gragra) {
		this.consistentOnly = enable;
		this.grammar = gragra;
	}

	public void enableNamedObjectOnly(boolean enable) {
		this.namedObjectOnly = enable;
	}
	
	public void setMaxBoundOfCriticKind(int bound) {
		this.maxBoundOfCriticKind = bound;
	}
	
	public void setMorphismCompletionStrategy(MorphCompletionStrategy strat) {
		this.strategy = strat;
		this.dangling = this.strategy.getProperties()
				.get(CompletionPropertyBits.DANGLING);
		
		this.withNACs = this.strategy.getProperties().get(CompletionPropertyBits.NAC);	
		this.withPACs = this.strategy.getProperties().get(CompletionPropertyBits.PAC);
		this.withPACs = this.strategy.getProperties().get(CompletionPropertyBits.GAC);
	}

	public void enableIgnoreIdenticalRules(boolean b) {
		this.ignoreIdenticalRules = b;
	}

	// ****************************************************************************+
	/**
	 * Checks whether this rule pair is critical of the specified kind. 
	 * Returns null if this pair is not critical, 
	 * otherwise returns overlapping morphisms 
	 * which show all found critical situations of these rules.  
	 * 	
	 * @param kind
	 *            specifies the algorithm kind of critical pair
	 * @param r1
	 *             first rule
	 * @param r2
	 *             second rule
	 * @throws InvalidAlgorithmException
	 *             Thrown if a illegal algorithm is selected.
	 * @return  critical overlapping of these two rules
	 */
	public Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	isCritical(int kind, Rule r1, Rule r2)
			throws InvalidAlgorithmException {
		
		this.r1hasEnabledNACs = r1.hasEnabledNACs();
		this.r1hasEnabledPACs = r1.hasEnabledPACs();
		this.r2hasEnabledNACs = r2.hasEnabledNACs();
		this.r2hasEnabledPACs = r2.hasEnabledPACs();
		
//		if (r1.hasEnabledACs(false) || r2.hasEnabledACs(false)) {
//			System.out.println("( "+r1.getName()+"  ,  "+r2.getName()+" )"+"  CPA for rules with GACs is not jet supported.");
//			throw new InvalidAlgorithmException("CPA for rules with GACs is not jet supported.", 
//					CriticalPairEvent.NOT_COMPUTABLE);
//		}
		
		if (this.ignoreIdenticalRules && r1 == r2) {
			if (kind == EXCLUDE)
				return null;
			else if (kind == CONFLICTFREE)
				return null;
			else
				throw new InvalidAlgorithmException("No such algorithm", kind);
		}

		this.ownStrategy = false;
		if (this.strategy == null) {
			this.strategy = (MorphCompletionStrategy) CompletionStrategySelector
					.getDefault().clone();
			// this.strategy.showProperties();
			this.ownStrategy = true;
		}

		if (kind == EXCLUDE) {
			return isExclude(r1, r2);
		} else if (kind == CONFLICTFREE) {
			return isExclude(r1, r2);
		}
		else
			throw new InvalidAlgorithmException("No such algorithm", kind);
	}

	protected boolean isProgressIndexSet() {
		return (this.duIndx!=-1) || (this.pfIndx!=-1) || (this.caIndx!=-1);
	}
	
	/**
	 * Checks if the first rule exclude the second rule.
	 * 
	 * @param r1
	 *            The first rule.
	 * @param r2
	 *            The second rule.
	 * @return All critical overlappings of this rule pair.
	 */
	protected Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	isExclude(final Rule r1, final Rule r2) {
		
//		System.out.println("ExcludePair.isExclude::  "+ r1.getName() + ", " + r2.getName());
//		if (this.withPACs && r1hasEnabledPACs) {
//			r1.extendByPacs();
//			r1hasEnabledPACs = false;
//		}
		
//		if (this.withPACs && r2hasEnabledPACs) {
//			r2.extendByPacs(); 
//			r2hasEnabledPACs = false;
//		}
			
//		if (this.essential) {
			// disable Type Multiplicity and Graph Constraints and NACs checking
//			this.disableConstraints();
//		} else 
		{
			// set Type Multiplicity check to be TypeSet.ENABLED_MAX
			this.levelOfTypeGraphCheck = this.grammar.getTypeSet().getLevelOfTypeGraphCheck();
			if (this.levelOfTypeGraphCheck > TypeSet.ENABLED_MAX)
				this.grammar.getTypeSet().setLevelOfTypeGraph(TypeSet.ENABLED_MAX);
		}
		
		// check global NACs of r2 before all other checks;
		// if LHS of r1 satisfy a global NAC of r2
		// which disregards all node attributes and does not contain any edges,
		// then r2 is not applicable in this case and
		// this rule pair cannot be critical in general
		if (this.withNACs && !checkGlobalNACsOfRule2(r1, r2)) {
//			System.out.println("ExcludePair.isExclude:: a global NAC of rule2  FAILED!");
			System.out.println("*** ExcludePair.isExclude::  [ "
					+ r1.getName() + ", " + r2.getName()
					+ " ]  non-critical.");
			return null;
		}
		
		System.gc();
		freeM = Runtime.getRuntime().freeMemory();
		
		this.prepareCriticalPairContextData(r1, r2);

		boolean 
		canLHS1OverlapLHS2 = canMatchConstantAttributeLHS1intoLHS2(r1, r2);

		final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		resultOverlappings = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();

		if (this.doneOverlaps != null) {
			if (!this.doneOverlaps.isEmpty()) {		
				resultOverlappings.addAll(this.doneOverlaps);
				this.doneOverlaps.clear();
				this.doneOverlaps = null;
			}
		}
		
		// check delete-use conflicts
		if (!this.isProgressIndexSet() || this.duIndx >= 0) {
			if (!this.stop && !this.contextC1_L1.isEmpty() && canLHS1OverlapLHS2) {
				if (!this.stop) {
					Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
					deleteUseOverlappings = getDeleteUseConflicts(r1, r2);
					
					if (deleteUseOverlappings != null && !deleteUseOverlappings.isEmpty()) {
						// test: CriticalPairData view
//						this.inspectCritPair(r1, r2, deleteUseOverlappings);
						
						resultOverlappings.addAll(deleteUseOverlappings);						
					}
				}
			}
		}
						
		// check produce-forbid conflicts
		if (!this.stop
				&& (!this.isProgressIndexSet() || this.pfIndx >=0)
				&& this.withNACs && !this.contextC1_R1.isEmpty()
				&& (this.complete || resultOverlappings.isEmpty())) {

			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
			produceForbidOverlappings = getProduceForbidConflicts(r1, r2);
						
			if (produceForbidOverlappings != null && !produceForbidOverlappings.isEmpty()) {
				// test: CriticalPairData view
//				this.inspectCritPair(r1, r2, produceForbidOverlappings);
						
				resultOverlappings.addAll(produceForbidOverlappings);
			}	
		}
		
		// check change-use attribute conflicts
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		changeAttributeOverlappings = null;
		if (!this.stop 
				&& (this.complete || resultOverlappings.isEmpty())) {
			this.preservedChanged.clear();
			this.contextC1_L1.clear();
			this.boundB1_L1.clear();
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> 
			changedAttrsL1 = new Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>>();
			
			// fill this.preservedChanged vector
			ruleChangesAttributes(this.preservedChanged, r1, r2, this.contextC1_L1, this.boundB1_L1,
							this.preservedK1_L1, changedAttrsL1, this.typesTG_L2);
			if (this.withNACs && r2hasEnabledNACs) {
				ruleChangesAttributes(this.preservedChanged, r1, r2, this.contextC1_L1, this.boundB1_L1,
							this.preservedK1_L1, changedAttrsL1, this.typesTG_NAC2);
			} 
			if (this.withPACs  && r2hasEnabledPACs) {
				ruleChangesAttributes(this.preservedChanged, r1, r2, this.contextC1_L1, this.boundB1_L1,
							this.preservedK1_L1, changedAttrsL1, this.typesTG_PAC2);
			}
							
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> 
			changedAttrsL2 = new Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>>();
			// changedAttrsL2 will be filled in ruleRestrictsAttributes(...)
				
			if (this.preservedChanged.isEmpty()
					|| !ruleRestrictsAttributes(this.strongAttrCheck, r2, changedAttrsL2, changedAttrsL1)) {
					// here do nothing!
			} else {
				changeAttributeOverlappings = getChangeAttributeConflicts(r1,
							r2, changedAttrsL1, changedAttrsL2);
					
				if (changeAttributeOverlappings != null && !changeAttributeOverlappings.isEmpty()) {
					resultOverlappings.addAll(changeAttributeOverlappings);
				}
			}
		}	
	
		// check produce-delete (dangling edge) conflicts
		if (!this.stop 
				&& !this.checkSwitchDependency 
				&& this.danglEdge 
				&& resultOverlappings.isEmpty()) {
			
			resetCriticalPairContextData(r2, r1);
			restrictDeleteContextDuetoDanglingEdge();
			
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
			produceDeleteOverlappings = getProduceEdgeDeleteNodeConflicts(r2, r1);			
			
			if (produceDeleteOverlappings != null && !produceDeleteOverlappings.isEmpty()) {
				resultOverlappings.addAll(produceDeleteOverlappings);
			}	
		}
		// set index of the overlapping graphs
		for (int i = 0; i < resultOverlappings.size(); i++) {			
			Pair<OrdinaryMorphism, OrdinaryMorphism> morphisms = resultOverlappings.elementAt(i).first;
			int j = i + 1;
			Graph overlap = morphisms.first.getImage();
			String name = overlap.getName();
			if (name.indexOf('(') == 0) {
				int ii = name.indexOf(')');
				if (ii > 0)
					name = overlap.getName().substring(ii+1);
			}
			overlap.setName("( "+j+" ) "+name);
			
			unsetAllTransientAttrValuesOfOverlapGrah(overlap);
						
			final String r1Prefix = "r1_";
			final String r2Prefix = "r2_";
			ExcludePairHelper.renameContextVariableOfOverlappingPair(r1, r2, morphisms, r1Prefix, r2Prefix);		
		}

		unsetAllTransientAttrValuesOfRule(r1);
		unsetAllTransientAttrValuesOfRule(r2);
			
		this.grammar.getTypeSet().setLevelOfTypeGraph(this.levelOfTypeGraphCheck);
	
		resultOverlappings.trimToSize();
		
		usedM = freeM - Runtime.getRuntime().freeMemory();
		
		if (resultOverlappings.isEmpty()) {
			System.out.println("*** ExcludePair.isExclude::  [ "
						+ r1.getName() + ", " + r2.getName()
						+ " ]  non-critical.");
			return null;
		} 
		System.out.println("*** ExcludePair.isExclude::  [ "
						+ r1.getName() + ", " + r2.getName() + " ]  "
						+ resultOverlappings.size()
						+ " critical overlapping graph(s).");

		return resultOverlappings;
	}

	protected void prepareCriticalPairContextData(final Rule r1, final Rule r2) {
		// fill this.typesTG_L2 with types used in LHS of r2
		fillTypeSubset(r2.getLeft(), this.typesTG_L2);
		// System.out.println("this.typesTG_L2: "+ this.typesTG_L2);
		
		// fill typesTG_PAC2 with types from this.typesTG_L2 
		// and types used in PACs of r2
		if (this.withPACs && r2hasEnabledPACs)
			getTypeSubsetLeft_PACs(r2, this.typesTG_L2, this.typesTG_PAC2);
		
		// fill typesTG_NAC2 with types from this.typesTG_L2 
		// and types used in NACs of r2
		if (this.withNACs && r2hasEnabledNACs)
			getTypeSubsetLeft_NACs(r2, this.typesTG_L2, this.typesTG_NAC2);

		// compute left context, boundary, preserved and delete of r1
		if (this.withPACs && r2hasEnabledPACs)
			computeLeftC_B_K(r1, this.contextC1_L1, this.boundB1_L1, this.preservedK1_L1,
					this.delete, this.typesTG_PAC2);
		else
			computeLeftC_B_K(r1, this.contextC1_L1, this.boundB1_L1, this.preservedK1_L1,
					this.delete, this.typesTG_L2);
				
		// compute right context, boundary, preserved and produce of r1
		if (this.withNACs && r2hasEnabledNACs)
			computeRightC_B_K(r1, this.contextC1_R1, this.boundB1_R1, this.preservedK1_R1,
					this.produce, this.typesTG_NAC2);
		else
			computeRightC_B_K(r1, this.contextC1_R1, this.boundB1_R1, this.preservedK1_R1,
					this.produce, this.typesTG_L2);

		// check dangling edge match condition 
		this.danglEdge = danglingEdgeAfterFirstProduceSecondDelete(r1, r2);
	
		this.typesTG_L2.trimToSize();
		this.typesTG_PAC2.trimToSize();
		this.typesTG_NAC2.trimToSize();
		this.contextC1_L1.trimToSize();
		this.boundB1_L1.trimToSize();
		this.preservedK1_L1.trimToSize();
		this.contextC1_R1.trimToSize();
		this.boundB1_R1.trimToSize();
		this.preservedK1_R1.trimToSize();
		this.delete.trimToSize();
		this.produce.trimToSize();
	}
	
	private void resetCriticalPairContextData(final Rule r1, final Rule r2) {
		// fill this.typesTG_L2 with types used in LHS of r2
		this.typesTG_L2.clear();
		fillTypeSubset(r2.getLeft(), this.typesTG_L2);
		
		// fill typesTG_PAC2 with types from this.typesTG_L2 
		// and types used in PACs of r2
		this.typesTG_PAC2.clear();
//		if (this.withPACs)
//			getTypeSubsetLeft_PACs(r2, this.typesTG_L2, this.typesTG_PAC2);
		
		// fill this.typesTG_NAC2 with types from this.typesTG_L2 
		// and types used in NACs of r2
		this.typesTG_NAC2.clear();
//		if (this.withNACs)
//			getTypeSubsetLeft_NACs(r2, this.typesTG_L2, this.typesTG_NAC2);

		// compute left context, boundary, preserved and delete of r1
		this.contextC1_L1.clear();
		this.boundB1_L1.clear();
		this.preservedK1_L1.clear();
		this.delete.clear();
		
//		if (this.withPACs)
//			computeLeftC_B_K(r1, contextC1_l1, boundB1_l1, preservedK1_l1,
//					this.delete, this.typesTG_PAC2);
//		else
			computeLeftC_B_K(r1, this.contextC1_L1, this.boundB1_L1, this.preservedK1_L1,
					this.delete, this.typesTG_L2);
	}
	
	private void restrictDeleteContextDuetoDanglingEdge() {
		for (int i=0; i<this.delete.size(); i++) {
			final GraphObject go = this.delete.get(i);
			if (go.isArc() || !this.danglingEdges.contains(go)) {
				this.delete.remove(go);
				i--;
			}
		}
	}
	
	
	/**
	 * Disable type multiplicity (max and min) constraints,
	 * graph consistency constraints, 
	 * NACs. PACs.
	 */
	protected void disableConstraints() {	
		// store current value and disable constraints
		
		this.levelOfTypeGraphCheck = this.grammar.getTypeSet().getLevelOfTypeGraphCheck();		
		this.grammar.getTypeSet().setLevelOfTypeGraph(TypeSet.ENABLED);
		
		this.consistCheck = this.consistentOnly;
		this.consistentOnly = false;
		
		this.withNACsCheck = this.withNACs;
		this.withNACs = false;	
		
		this.withPACsCheck = this.withPACs;
		this.withPACs = false;
	}
	
	/**
	 * Enable type multiplicity constraints,
	 * graph consistency constraints, 
	 * NACs, PACs.
	 */
	protected void enableConstraints() {
		this.grammar.getTypeSet().setLevelOfTypeGraph(this.levelOfTypeGraphCheck);
		
		this.consistentOnly = this.consistCheck;
		
		this.withNACs = this.withNACsCheck;
		
		this.withPACs = this.withPACsCheck;
	}
	
		
	/**
	 * Let the first rule r1 be applicable. 
	 * The second rule r2 contains a global NAC which disregards any attributes of nodes
	 * and does not contain any edges.
	 * Checks whether the second rule r2 can be applied in this case.
	 * If not, then this rule pair is not critical.
	 * @param r1
	 * @param r2
	 * @return true if the second rule r2 can be applied
	 */
	protected boolean checkGlobalNACsOfRule2(final Rule r1, final Rule r2) {		
		final List<OrdinaryMorphism> nacsR2 = r2.getNACsList();
		for (int i=0; i<nacsR2.size(); i++) {
			final OrdinaryMorphism nac = nacsR2.get(i);
			if (nac.isEnabled() 
					&& nac.isEmpty() // a global NAC
					&& !nac.getTarget().areAnyAttributesOfNodesSet() // attrs of nodes are disregarded
					&& !nac.getTarget().getArcsSet().iterator().hasNext()) { // no edges
				if (this.strategy.isInjective()) { // injective match strategy
					final Iterator<Node> nodes = nac.getTarget().getNodesSet().iterator();
					boolean countfailed = true;
					while (countfailed && nodes.hasNext()) {
						Node n = nodes.next();
						List<Node> list1 = r1.getLeft().getNodes(n.getType());	
						if (list1 != null) {
							List<Node> list = nac.getTarget().getNodes(n.getType());						
							List<Node> list2 = r2.getLeft().getNodes(n.getType());
							
							int countInNacGraph = list != null? list.size(): 0;
							int countInLHS1 = list1 != null? list1.size(): 0;						
							int countInLHS2 = list2 != null? list2.size(): 0;	
							if (countInLHS1 < (countInLHS2+countInNacGraph))
								countfailed = false;	
						}
						else 
							countfailed = false;	
					}
					if (countfailed)
						return false;
				}
			}
		}
		return true;
	}
	
	
	private MorphCompletionStrategy getLocalMorphismCompletionStrategy() {
		if (this.withInheritance)
			return new Completion_InheritCSP();
		
		return new Completion_InjCSP();
	}
	
	private boolean doCompose(
			final MorphCompletionStrategy localStrategy, 
			final  OrdinaryMorphism result, 
			final OrdinaryMorphism morph1, 
			final OrdinaryMorphism morph2) {
		boolean res;
		if (localStrategy instanceof Completion_InheritCSP) { 
			res =  result.doComposeInherit(morph1, morph2);
		} else {	
			res =  result.doCompose(morph1, morph2);
		}
		if (res) {
			replaceVarAttrByConstFromSrcToTar(result);
		}
		return res;
	}
	
	private void replaceVarAttrByConstFromSrcToTar(final OrdinaryMorphism m) {
		Enumeration<GraphObject> dom = m.getDomain();
		while (dom.hasMoreElements()) {
			GraphObject obj = dom.nextElement();
			GraphObject img = m.getImage(obj);
			if (obj.getAttribute() != null
					&& img.getAttribute() != null) {
				ValueTuple val1 = (ValueTuple) obj.getAttribute();
				ValueTuple val2 = (ValueTuple) img.getAttribute();
				for (int i=0; i<val1.getNumberOfEntries(); i++) {
					ValueMember v1 = val1.getValueMemberAt(i);
					if (v1.isSet() && v1.getExpr().isConstant()) {
						ValueMember v2 = val2.getValueMemberAt(v1.getName());
						if (v2 != null && (!v2.isSet() || !v2.getExpr().isConstant())) {
							v2.setExprAsText(v1.getExprAsText());
							v2.setTransient(v1.isTransient());
						}
					}
				}
			}					
		}
	}
	
	boolean canMatchConstantAttributeLHS1intoLHS2(Rule r1, Rule r2) {
		if (!r2.getLeft().getNodesSet().iterator().hasNext()
				|| r2.getTypeSet().hasInheritance())
			return true;

		boolean domExists = false;
		boolean domValid = false;
		boolean canMatch = true;

		Iterator<Node> nodes1 = r1.getLeft().getNodesSet().iterator();
		while (nodes1.hasNext()) {
			GraphObject go1 = nodes1.next();
			// check whether domain exists
			HashSet<GraphObject> dom2 = r2.getLeft().getTypeObjectsMap().get(
					go1.getType().convertToKey());
			if (dom2 == null) {
				Vector<Type> parents_go1 = go1.getType().getAllParents();
				for (int p=0; p<parents_go1.size(); p++) {
					Type pt = parents_go1.get(p);
					dom2 = r2.getLeft().getTypeObjectsMap().get(
							pt.convertToKey());
					if(dom2 != null)
						break;
				}
				if (dom2 == null)
				continue;
			}
			domExists = true;

			if (go1.getAttribute() == null) {
				domValid = true;
				continue;
			}
			// search for constant attribute value
			final Vector<String> attrAsConstant = new Vector<String>();
			for (int i = 0; i < go1.getAttribute().getNumberOfEntries(); i++) {
				ValueMember vm = (ValueMember) go1.getAttribute()
						.getMemberAt(i);
				if (vm.isSet() && vm.getExpr().isConstant()) {
					attrAsConstant.add(vm.getName());
				}
			}
			if (attrAsConstant.isEmpty()) {
				domValid = true;
				continue;
			} 
			// now search for constant attr value in domain
			Iterator<GraphObject> iter = dom2.iterator();
			while (iter.hasNext()) {
				GraphObject go2 = iter.next();
				canMatch = true;
				for (int j = 0; j < attrAsConstant.size(); j++) {
					String vmName = attrAsConstant.get(j);
					ValueMember vm2 = (ValueMember) go2.getAttribute()
							.getMemberAt(vmName);
					if ((vm2 == null) || !vm2.isSet() || !vm2.getExpr().isConstant())
						continue;
					else if (vm2.isSet() && vm2.getExpr().isConstant()) {
						ValueMember vm1 = (ValueMember) go1.getAttribute()
								.getMemberAt(vmName);
						if (!vm2.getExpr().equals(vm1.getExpr())) {
							canMatch = false;
							break;
						}
					}
				}
				if (canMatch) {
					domValid = true;
				}
			}
		}

		if (!domExists || !domValid)
			return false;
		
		return true;
	}

	/*
	 * Check constant attributes of a NAC graph of the rule2 agains constant attributes 
	 * of the new graph objects of the RHS of the rule1.
	 * Note: only free unmapped nodes of a NAC are checked!
	 */
	boolean canMatchConstAttrOfNAC2intoRHS1(
			final OrdinaryMorphism nac2, 
			final Graph rhs1, 
			final Vector<GraphObject> tocheckRHS1) {
		
		if (!nac2.getTarget().getNodesSet().iterator().hasNext()
				|| nac2.getTarget().getTypeSet().hasInheritance())
			return true;
		
		boolean canMatch = true;
		boolean domExists = false;
		boolean domValid = false;

		// take only unmapped nodes of a NAC graph
		final Vector<Node> nodes2 = new Vector<Node>();
		Iterator<Node> nac2nodes = nac2.getTarget().getNodesSet().iterator();		
		while (nac2nodes.hasNext()) {
			Node go2 = nac2nodes.next();
			if (!nac2.getInverseImage(go2).hasMoreElements()) {
				// free nodes only
				if (go2.getNumberOfInOutArcs() == 0)
					nodes2.add(go2);
			}
		}
		for (int k = 0; k < nodes2.size(); k++) {
			Node go2 = nodes2.get(k);
			// check whether domain exists
			HashSet<GraphObject> dom1 = rhs1.getTypeObjectsMap().get(
					go2.getType().convertToKey());
			if (dom1 == null) {
				continue;
			}
			domExists = true;

			if (go2.getAttribute() == null) {
				domValid = true;
				continue;
			}

			// search for constant attr value
			final Vector<String> attrAsConstant = new Vector<String>();
			for (int j = 0; j < go2.getAttribute().getNumberOfEntries(); j++) {
				ValueMember vm2 = (ValueMember) go2.getAttribute().getMemberAt(j);
				if (vm2.isSet() && vm2.getExpr().isConstant()) {
					attrAsConstant.add(vm2.getName());
				}
			}
			if (attrAsConstant.isEmpty()) {
				domValid = true;
				continue;
			} 
			
			// search for constant attr value in domain
			Iterator<GraphObject> iter = dom1.iterator();
			while (iter.hasNext()) {
				GraphObject go1 = iter.next();
				if (!tocheckRHS1.contains(go1))
					continue;
				canMatch = true;
				for (int j = 0; j < attrAsConstant.size(); j++) {
					String vmName = attrAsConstant.get(j);
					ValueMember 
					vm1 = (ValueMember)go1.getAttribute().getMemberAt(vmName);
					if ((vm1 == null) || !vm1.isSet() || !vm1.getExpr().isConstant())
						continue;
					else if (vm1.isSet() && vm1.getExpr().isConstant()) {
						ValueMember 
						vm2 = (ValueMember) go2.getAttribute().getMemberAt(vmName);
						if (!vm1.getExpr().equals(vm2.getExpr())) {
							canMatch = false;
							break;
						}
					}
				}
				if (canMatch) {
					domValid = true;
					// System.out.println("canMatch "+go1.getType().getName()+"
					// --> "+go2.getType().getName());
				}
			}
		}
		// System.out.println("after node check: domain exists: "+ domExists+"
		// canMatchCount: "+ canMatchCount);

		/*
		// take only unmapped edges of a NAC
		Enumeration<Arc> allarcs2 = nac2.getTarget().getArcs();
		final Vector<Arc> arcs2 = new Vector<Arc>();
		while (allarcs2.hasMoreElements()) {
			Arc go2 = allarcs2.nextElement();
			if (!nac2.getInverseImage(go2).hasMoreElements())
				arcs2.add(go2);
		}
		for (int k = 0; k < arcs2.size(); k++) {
			Arc go2 = arcs2.get(k);
			// check whether domain exists
			Vector<GraphObject> dom1 = rhs1.getTypeObjectsMap().get(
					go2.getTypeMapKey()); // .convertToKey());
			if (dom1 == null) {
				continue;
			}
			domExists = true;

			if (go2.getAttribute() == null) {
				domValid = true;
				continue;
			}
			
			// search for constant attr value
			final Vector<String> attrAsConstant = new Vector<String>();
			for (int i = 0; i < go2.getAttribute().getNumberOfEntries(); i++) {
				ValueMember vm = (ValueMember) go2.getAttribute()
						.getMemberAt(i);
				if (vm.isSet() && vm.getExpr().isConstant()) {
					attrAsConstant.add(vm.getName());
				}
			}
			if (attrAsConstant.isEmpty()) {
				domValid = true;
				continue;
			} 
			
			// search for constant attr value in domain
			for (int i = 0; i < dom1.size(); i++) {
				GraphObject go1 = dom1.get(i);
				if (tocheckRHS1 != null && !tocheckRHS1.contains(go1))
					continue;
				canMatch = true;
				for (int j = 0; j < attrAsConstant.size(); j++) {
					String vmName = attrAsConstant.get(j);
					ValueMember vm1 = (ValueMember) go1.getAttribute()
							.getMemberAt(vmName);
					if (!vm1.isSet() || !vm1.getExpr().isConstant())
						continue;
					else if (vm1.isSet() && vm1.getExpr().isConstant()) {
						ValueMember vm2 = (ValueMember) go2.getAttribute()
								.getMemberAt(vmName);
						if (!vm2.getExpr().equals(vm1.getExpr())) {
							canMatch = false;
							break;
						}
					}
				}
				if (canMatch) {
					domValid = true;
					// System.out.println("canMatch "+go1.getType().getName()+"
					// --> "+go2.getType().getName());
				}
			}
		}
*/
		if (domExists && !domValid) 
			return false;
		
		return true;
	}

	protected boolean needMoreCheckDueToDelConstAttr(final Rule r1, final Rule r2) {
		final List<GraphObject> delObjsLHS1 = r1.getElementsToDelete();
		final Hashtable<Type, Vector<GraphObject>> table = new Hashtable<Type, Vector<GraphObject>>();
		for (int i = 0; i < delObjsLHS1.size(); i++) {
			GraphObject goLHS1 = delObjsLHS1.get(i);
			if (this.canMapLeftObjDueToConstAttr(goLHS1, r1, r2, table))
				return true;
			else
				this.contextC1_L1.remove(goLHS1);
		}
		return false;
	}

	private boolean canMapLeftObjDueToConstAttr(
			final GraphObject goLHS1,
			final Rule r1, final Rule r2, 
			final Hashtable<Type, Vector<GraphObject>> table) {
		
		if (goLHS1.getAttribute() == null 
				|| goLHS1.getAttribute().getNumberOfEntries() == 0)
			return true;
		
		Vector<GraphObject> vec2 = table.get(goLHS1.getType());
		if (vec2 == null) {
			vec2 = r2.getLeft().getElementsOfTypeAsVector(goLHS1.getType());
			if (vec2.isEmpty()) {
				Vector<Type> parents = goLHS1.getType().getAllParents();
				for (int p = 1; p < parents.size(); p++) {
					Type pt = parents.get(p);
					Vector<GraphObject> v = r2.getLeft().getElementsOfTypeAsVector(pt);
					if (!v.isEmpty())
						vec2.addAll(v);
				}
			}
			final Enumeration<OrdinaryMorphism> pacs_r2 = r2.getEnabledPACs();
			while (pacs_r2.hasMoreElements()) {
				final OrdinaryMorphism pac2 = pacs_r2.nextElement();			
				if (pac2.isEnabled()) 
					vec2.addAll(pac2.getTarget().getElementsOfTypeAsVector(goLHS1.getType()));
			}
			table.put(goLHS1.getType(), vec2);
		}
		boolean canMap = true;
		ValueTuple vtLHS1 = (ValueTuple) goLHS1.getAttribute();
		// search for at least one object to map
		for (int k = 0; k < vec2.size(); k++) {
			GraphObject goLHS2 = vec2.get(k);
			ValueTuple vtLHS2 = (ValueTuple) goLHS2.getAttribute();
			canMap = true;
			if (vtLHS2 != null) {
				for (int j = 0; j < vtLHS1.getNumberOfEntries(); j++) {
					ValueMember vmLHS1 = vtLHS1.getValueMemberAt(j);			
					if (vmLHS1.isSet() && vmLHS1.getExpr().isConstant()) {
						ValueMember vmLHS2 = vtLHS2.getValueMemberAt(vmLHS1.getName());
						if (vmLHS2 != null && vmLHS2.isSet() 
								&& vmLHS2.getExpr().isConstant()
								&& !vmLHS2.getExpr().equals(vmLHS1.getExpr())) {
							canMap = false;
							break;
						}
					}
				} 
			}
			if (canMap)
				break;
		}
		return canMap;
	}

	
	/*
	private boolean needMoreCheckWhenProduceConstantAttribute(Rule r1,
			OrdinaryMorphism nac2) {
//		final Vector<GraphObject> res = new Vector<GraphObject>();
		boolean needMoreCheck = true;
		int needMoreCheckCount = 0;
		List<GraphObject> prodObjsRHS1 = r1.getElementsToCreate();
		final Hashtable<Type, Vector<GraphObject>> table = new Hashtable<Type, Vector<GraphObject>>();
		for (int i = 0; i < prodObjsRHS1.size(); i++) {
			GraphObject goRHS1 = prodObjsRHS1.get(i);
			Vector<GraphObject> vec2 = nac2.getTarget()
					.getElementsOfTypeAsVector(goRHS1.getType());
			for (int j = vec2.size() - 1; j >= 0; j--) {
				GraphObject go = vec2.get(j);
				if (nac2.getInverseImage(go).hasMoreElements())
					vec2.remove(go);
			}
			if (!vec2.isEmpty()) {
				if (goRHS1.getAttribute() == null)
					return true;
			} else
				continue;

			ValueTuple vtRHS1 = (ValueTuple) goRHS1.getAttribute();
			ValueMember vmRHS1;
			for (int j = 0; j < vtRHS1.getNumberOfEntries(); j++) {
				vmRHS1 = vtRHS1.getValueMemberAt(j);
				if (vmRHS1.isSet() && vmRHS1.getExpr().isConstant()) {
					Vector<GraphObject> vec = table.get(goRHS1.getType());
					if (vec == null) {
						vec = vec2;
						if (vec.isEmpty()) {
							break;
						} else
							table.put(goRHS1.getType(), vec);
					}
					int needMoreCheckLocalCount = 0;
					for (int k = 0; k < vec.size(); k++) {
						needMoreCheck = true;
						GraphObject goNAC2 = vec.get(k);
						ValueTuple vtNAC2 = (ValueTuple) goNAC2.getAttribute();
						ValueMember vmNAC2;
						for (int l = 0; l < vtNAC2.getNumberOfEntries(); l++) {
							vmNAC2 = vtNAC2.getValueMemberAt(l);
							if (vmNAC2.isSet() && vmNAC2.getExpr().isConstant()) {
								// System.out.println(vmRHS1.getExprAsText()+"
								// ?= "+vmNAC2.getExprAsText());
								if (!vmNAC2.getExpr().equals(vmRHS1.getExpr())) {
									needMoreCheck = false;
									break;
								}
							}
						}
						if (needMoreCheck)
							needMoreCheckLocalCount++;
					}
					if (needMoreCheckLocalCount > 0)
						needMoreCheckCount++;

				} else
					needMoreCheckCount++;
			}
		}
		return (needMoreCheckCount > 0);
	}

	private boolean needMoreCheckWhenProduceCostantAttribute(
			Vector<GraphObject> prodObjsRHS1,
			Hashtable<Type, Vector<GraphObject>> type2gosNAC2) {
		// System.out.println("ExcludePair.needMoreCheckWhenProduceCostantAttribute
		// ");
		boolean needMoreCheck = true;
		int needMoreCheckCount = 0;
		// System.out.println("prodObjsRHS1: "+prodObjsRHS1);
		for (int i = 0; i < prodObjsRHS1.size(); i++) {
			GraphObject goRHS1 = prodObjsRHS1.get(i);
			Vector<GraphObject> vec = type2gosNAC2.get(goRHS1.getType());
			// System.out.println("vec: "+vec);
			if (!vec.isEmpty()) {
				if (goRHS1.getAttribute() == null)
					return true;
			} else
				continue;

			ValueTuple vtRHS1 = (ValueTuple) goRHS1.getAttribute();
			ValueMember vmRHS1;
			for (int j = 0; j < vtRHS1.getNumberOfEntries(); j++) {
				vmRHS1 = vtRHS1.getValueMemberAt(j);
				if (vmRHS1.isSet() && vmRHS1.getExpr().isConstant()) {
					int needMoreCheckLocalCount = 0;
					for (int k = 0; k < vec.size(); k++) {
						needMoreCheck = true;
						GraphObject goNAC2 = vec.get(k);
						ValueTuple vtNAC2 = (ValueTuple) goNAC2.getAttribute();
						ValueMember vmNAC2;
						for (int l = 0; l < vtNAC2.getNumberOfEntries(); l++) {
							vmNAC2 = vtNAC2.getValueMemberAt(l);
							if (vmNAC2.isSet() && vmNAC2.getExpr().isConstant()) {
								// System.out.println(vmRHS1.getExprAsText()+"
								// ?= "+vmNAC2.getExprAsText());
								if (!vmNAC2.getExpr().equals(vmRHS1.getExpr())) {
									needMoreCheck = false;
									break;
								}
							}
						}
						if (needMoreCheck)
							needMoreCheckLocalCount++;
					}
					if (needMoreCheckLocalCount > 0)
						needMoreCheckCount++;

				} else
					needMoreCheckCount++;
			}
		}
		return (needMoreCheckCount > 0);
	}
*/
	
	protected void destroyOverlapping(Pair<OrdinaryMorphism, OrdinaryMorphism> p) {
		OrdinaryMorphism om1 = p.first;
		OrdinaryMorphism om2 = p.second;
		om1.dispose();
		om1 = null;
		om2.dispose(false, true);
		om2 = null;
	}
	
	/*
	 * Here is the max overlapping size of nodes only.
	 */
//	@SuppressWarnings("unused")
//	private int removeInclSmallerMaxOverlap(int maxOverlapSize,
//											final Vector<Vector<GraphObject>> inclusions) {
//		int maxSize = maxOverlapSize;
//		if (this.maxOverlapping) {
//			for (int l=0; l<inclusions.size(); l++) {
//				final Vector<GraphObject> inclSet = inclusions.get(l);
//				int nodeCount = getNodeCount(inclSet);
//				if (nodeCount > maxSize) {
//					maxSize = nodeCount;
//					int i = l-1;
//					while (i>=0) {
//						inclusions.remove(l);
//						l--;
//						i--;
//					}
//				} 
//				else if (nodeCount < maxSize) {
//					inclusions.remove(l);
//					l--;
//				}
//			}			
//			inclusions.trimToSize();
//		}
//		return maxSize;
//	}
	
//	private int getNodeCount(final List<GraphObject> inclSet) {
//		int count = 0;
//		for (int i=0; i<inclSet.size(); i++) {
//			if (inclSet.get(i).isNode())
//				count++;
//		}
//		return count;
//	}
	
	Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	doneOverlaps;
	
	@SuppressWarnings("unchecked")
	public void setProgressIndx(Entry e) {
		this.duIndxStr = e.duIndxStr;
		this.pfIndxStr = e.pfIndxStr;
		this.caIndxStr = e.caIndxStr;
		try {
			this.duIndx = Integer.valueOf(this.duIndxStr.split(":")[0]).intValue();
			this.pfIndx = Integer.valueOf(this.pfIndxStr.split(":")[0]).intValue();
			this.caIndx = Integer.valueOf(this.caIndxStr.split(":")[0]).intValue();
		} catch (NumberFormatException ex) {}
		
		if (e.getOverlapping() != null) {
			if (this.doneOverlaps == null)
				this.doneOverlaps = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
			else
				this.doneOverlaps.clear();
			for (int i=0; i<e.getOverlapping().size(); i++) {
				doneOverlaps.add((Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>)e.getOverlapping().get(i));
			}
		}
	}
	
	protected void unsetProgressIndx() {
		this.duIndxStr = "";
		this.pfIndxStr = "";
		this.caIndxStr = "";
		this.duIndx = -1;
		this.pfIndx = -1;
		this.caIndx = -1;
	}
	
	private int getDUIndx() {
		try {
			this.duIndx = Integer.valueOf(this.duIndxStr.split(":")[0]).intValue();
		} catch (NumberFormatException ex) {
			this.duIndx = -1;
		}
//		System.out.println("getDUIndx:  "+this.duIndx+"    "+this.duIndxStr);
		return this.duIndx;
	}
	
	private String getDUNameIndx() {
		try {
			return this.duIndxStr.split(":")[1];
		} catch (ArrayIndexOutOfBoundsException ex) {}
		return "";
	}
	
	void saveDUIndx(int i, String s, boolean switchDep) {
		//TODO also for switch dependency
		this.duIndx = i;
		String swDep = switchDep? "1": "0";
		this.duIndxStr = String.valueOf(this.duIndx)
							.concat(":").concat(s)
							.concat(":").concat(swDep);		
	}
	
	protected Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getDeleteUseConflicts(
			final Rule r1, 
			final Rule r2) {
		System.out.println("    ExcludePair.getDeleteUseConflicts::  [ "
				+ r1.getName() + ", " + r2.getName() + " ] ...");
		
		// check constant attribute of deleted objects of r1
		// against constant attribute of LHS of r2
		// The objects of appropriate type from LHS of r2 
		// should use this constant attribute
		if (!needMoreCheckDueToDelConstAttr(r1, r2)) 
			return null;
			
		this.cpdKind = CriticalPairData.DELETE_USE_CONFLICT;
		
		if (this.essential) {
			return this.getEssentialDeleteUseConflicts(r1, r2);
		}

		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlaps = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
		
		// only for dependency check
		if (this instanceof DependencyPair) {
			if (!this.checkSwitchDependency 
					&& this.duIndxStr.endsWith(":1")) {
				return overlaps;
			}
			else if (this.checkSwitchDependency 
						&& this.duIndxStr.endsWith(":0")) {
				this.duIndxStr = "";
			}
		}
		
		this.inclCount = 0;
		this.inclProgress = 0;
		this.duIndx = this.getDUIndx();
		String duNameIndx = this.duIndxStr.contains(":PAC")? this.getDUNameIndx(): "";
			
		if (this.ac2leftExtended == null)
			this.ac2leftExtended = new Hashtable<OrdinaryMorphism,Pair<OrdinaryMorphism, OrdinaryMorphism>>();
				
		final Graph g = r1.getLeft();
		int maxSize = r2.getLeft().getSize();
		Enumeration<OrdinaryMorphism> pacs2 = r2.getEnabledPACs();
		
		boolean lhs_done = false; 
		boolean perform = true;
		while (perform && !this.stop) {
			String pacName = "";
			// consider PACs
			OrdinaryMorphism L2isoL2ExtendedByPAC = null;
			Pair<OrdinaryMorphism, OrdinaryMorphism> lhs_pac_pair = null;
			
			OrdinaryMorphism pac = null;
			if (this.withPACs && pacs2.hasMoreElements()) {				
				pac = pacs2.nextElement();				
				if (!pac.isEnabled()
						|| (this.duIndx > 0 && this.duIndxStr.contains(":PAC")
								&& !pac.getName().equals(duNameIndx))) {
					continue;
				}				
			}
			// check PAC and reset maxSize if needed			
			if (pac != null) {
				boolean pacCritical = ExcludePairHelper.isCriticalPAC(pac, this.delete);
				if (pacCritical) {					
					L2isoL2ExtendedByPAC = r2.getLeft().isomorphicCopy();
					if (L2isoL2ExtendedByPAC != null) {
						// extend LHS of r2 by a PAC
						// images of the PAC objects are disjunct (injective)
						lhs_pac_pair = extendLeftGraphByPAC(L2isoL2ExtendedByPAC, pac, false);
						pacName = pac.getName();
						maxSize = L2isoL2ExtendedByPAC.getTarget().getSize();
						lhs_done = true;
					}				
				}			 
			}
			
			Vector<Vector<GraphObject>> inclusions = null;
			Vector<Vector<GraphObject>> contextCombis = null;
			this.inclAsGraph = false;
			
			int size = this.contextC1_L1.size();
			if (size > maxSize) size = maxSize;
		
			contextCombis = ExcludePairHelper.getInclusions(g, size, this.contextC1_L1, true);		
			// each inclusion should contain at least one object to delete
			checkInclusions(contextCombis, this.delete);
	
			if (contextCombis.size() == 0) {
				// continue with next PAC
				if (pacs2.hasMoreElements())
					continue;
				else
					break;	
			}
			
			if (namedObjectOnly) {
				this.checkInclusionsDuetoNamedObject(contextCombis);
				if (contextCombis.size() == 0) {
					// continue with next PAC
					if (pacs2.hasMoreElements())
						continue;
					else
						break;	
				}
			}
		
			size = this.preservedK1_L1.size();
			if (size > maxSize) size = maxSize;
		
			Vector<Vector<GraphObject>>
			preservedCombis = ExcludePairHelper.getPlainCombinedInclusions(
									new Vector<GraphObject>(this.preservedK1_L1), size, g);
							
			int ncp = 0; // the number of (contextCombis X preservedCombis)
			int nn = 0;	// the number of already checked inclusions
			int nm = contextCombis.size()-1;	// the last index of contextCombis
			
			// start a loop beginning by the last vector 
			// to combine this vector of context with all preservedCombis
			int run = 0;
			while (nm >= 0 && !this.stop) {
				if (preservedCombis.size() > 0) {
					if (!contextCombis.isEmpty()) {
						// combine one inclusion of contextCombis with all inclusions of preservedCombis
						inclusions = ExcludePairHelper.combineFirstWithSecondAboveThird(
										maxSize, contextCombis.get(nm), preservedCombis, this.boundB1_L1);
						
						contextCombis.remove(nm); 	// remove last element 
						nm = contextCombis.size()-1;	// the last index of contextCombis
						// reduce inclusions up to this.duIndx
						if (this.duIndx > 0 && this.duIndx > nn) { 
							if ((ncp + inclusions.size()-1) < this.duIndx) {					
								// the last index of all inclusions is smaller then saved duIndx, 
								// so continue to combine next context vector with all preservedCombis
								ncp = ncp + inclusions.size();
								continue;
							}
							nn =  ncp + inclusions.size();						
							int i=inclusions.size()-1;
							while (ncp + inclusions.size() > this.duIndx) {					
								inclusions.remove(i);
								i=inclusions.size()-1;
							}
							nn = nn - inclusions.size();
	//						System.out.println(nn+"    "+this.duIndx);
							ncp = this.duIndx;
							this.duIndx = -1;	// unset this.duIndx
						}
					}
				}
				else if (this.duIndx > 0) {
					// reduce contextCombis up to this.duIndx
					int i=contextCombis.size()-1;	
					while (i>=0 && i>=this.duIndx) {	
						contextCombis.remove(i);
						i=contextCombis.size()-1;
					}
					inclusions = contextCombis;
					ncp = this.duIndx;
					// unset this.duIndx
					this.duIndx = -1;
					nm = -1; // break for the while-loop over nm
				}
				else {
					// inclusions are the contextCombis
					inclusions = contextCombis;
					nm = -1; // break of the while-loop
				}
						
				this.inclCount = inclusions.size();	
				run++;
				System.out.println("(run: "+run+") to check inclusions: "+ this.inclCount +"   already checked: "+ncp);
				
				int n100 = 0, nn2 = 0;	// counter of 100
				int i=inclusions.size()-1;	
				// work from end to begin of the list,  make and check inclusion morphisms
				while (i>=0 && !this.stop) {					
					Vector<GraphObject> inclSet = inclusions.get(i); 
					i--;
					
					n100++; // counter of 100
					
					// make overlappings
					OrdinaryMorphism inclMorphism = null;
					Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
					localOverlaps = null;
							
					if (L2isoL2ExtendedByPAC == null) {
						if (r2.getSource().getSize() >= inclSet.size()) 
							inclMorphism = makeInclusionMorphism(inclSet, g);
						
						// with LHS of second rule
						if ((inclMorphism != null)
								&& (r2.getSource().getNodesCount() >= inclMorphism.getSource().getNodesCount())
								&& (r2.getSource().getArcsCount() >= inclMorphism.getSource().getArcsCount())) {
							
							localOverlaps = getOverlappingsVectorDeleteUse(r1, r2, inclMorphism);					
							setGraphNameOfDeleteUseConflict(r1, r2, localOverlaps);	
							inclMorphism.dispose(true, false); inclMorphism = null;
						}						
						lhs_done = true;
					}
					else {
						if (lhs_pac_pair.first.getTarget().getSize() >= inclSet.size()) 
							inclMorphism = makeInclusionMorphism(inclSet, g);
						
						// with LHS extended by PAC of the second rule 
						if ((inclMorphism != null)
								&& (lhs_pac_pair.first.getTarget().getNodesCount() >= inclMorphism.getSource().getNodesCount())
								&& (lhs_pac_pair.first.getTarget().getArcsCount() >= inclMorphism.getSource().getArcsCount())) {
							
							localOverlaps = getOverlappingsVectorDeleteUse(r1, r2, lhs_pac_pair, inclMorphism);						
							setGraphNameOfDeleteUseConflict(r1, r2, localOverlaps, pacName);
							for (int x=0; x<localOverlaps.size(); x++) {
								unsetAllTransientAttrValuesOfOverlapGrah(localOverlaps.get(x).first.first.getTarget());
							}
							inclMorphism.dispose(true, false); inclMorphism = null;
						}
						lhs_done = true;
					}
					this.inclProgress++;
											
//					unsetAllTransientAttrValuesOfRule(r2);
					
					if (localOverlaps != null) {
						for (int x=0; x<localOverlaps.size(); x++) {
							if (this.maxBoundOfCriticKind == 0 
									|| overlaps.size() < this.maxBoundOfCriticKind) {
								overlaps.add(localOverlaps.get(x));
							}
						}					
						
						localOverlaps.clear();	localOverlaps = null;
					}
					
					if (!this.complete && !overlaps.isEmpty()) {
						break;
					}
					if (this.maxBoundOfCriticKind > 0 
							&& overlaps.size() >= this.maxBoundOfCriticKind) {
						break;
					}
					if (n100 == 100) {
						nn2++; 
						System.out.println("checked  inclusions: "+n100*nn2+"     to check: "+(i+1));
						n100 = 0;
						inclusions.trimToSize();
					}
				} // while (i>=0 && !this.stop)	over inclusions
				if (stop) {
					// set this.duIndx 
					if (pacName.isEmpty())
						this.saveDUIndx(ncp + i, "".concat(":LHS"), this.checkSwitchDependency);
					else
						this.saveDUIndx(ncp + i, pacName.concat(":PAC"), this.checkSwitchDependency);
					System.out.println("DeleteUse conflict: stop at index: "+this.duIndx);						
				}
				// set the number of all valid inclusions
				ncp = ncp + this.inclCount;
							
				if (!this.complete && !overlaps.isEmpty()) {
					break;
				}

				if (this.maxBoundOfCriticKind > 0 
						&& overlaps.size() >= this.maxBoundOfCriticKind)  {
					break;
				}
			} // while(nm >= 0 && !this.stop)  over contextCombis
			inclusions = null;
			contextCombis = null;
			preservedCombis = null;
			perform = (this.withPACs && pacs2.hasMoreElements()) || !lhs_done; 
		} // while (perform && !this.stop)	over PACs
		
		if (!stop) {
			this.saveDUIndx(-1, "", false);
		}
		if (this.withPACs) {
			pacs2 = r2.getEnabledPACs();
			while (pacs2.hasMoreElements()) {
				// restore constant attribute value
				this.replaceVarAttrValueByConst(pacs2.nextElement());
			}
		}
		
		//test overlap graphs and reduce isomorphic
		if (/*!r1.getTypeSet().isArcDirected() &&*/ overlaps.size() > 0) {
			reduceCriticalPairs(overlaps);
		}
		
		overlaps.trimToSize();
		this.cpdKind = -1;
		
		System.out.println("    ExcludePair.getDeleteUseConflicts::  [ "
					+ r1.getName() + ", " + r2.getName() + " ]  " + overlaps.size()
					+ " critical overlapping(s)");
		System.gc();
		return overlaps;
	}

	protected boolean tryExcludePAC(
			Rule r1,
			Rule r2,
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			overlap) {
		boolean failed = false;
		OrdinaryMorphism om1 = overlap.first.first;
		OrdinaryMorphism om2 = overlap.first.second;
		Enumeration<GraphObject> gos = om2.getTarget().getElements();
		while (!failed && gos.hasMoreElements()) {
			GraphObject go = gos.nextElement();
			if (om1.getInverseImage(go).hasMoreElements()) {
				Enumeration<GraphObject> en2 = om2.getInverseImage(go);
				if (en2.hasMoreElements()) {
					GraphObject go2 = en2.nextElement();
					if (!r2.getLeft().isElement(go2))
						failed = true;
				} 
			}
			else if (om2.getInverseImage(go).hasMoreElements()) {
				failed = true;
			}
		}
		if (!failed) {
			Iterator<Arc> arcs = (new Vector<Arc>(om2.getTarget().getArcsSet())).iterator();
			while (arcs.hasNext()) {
				Arc go = arcs.next();
				if (!om1.getInverseImage(go).hasMoreElements()
						&& !om2.getInverseImage(go).hasMoreElements()) {
					try {
						om2.getTarget().destroyArc(go, false, true);
					} catch (TypeException ex) {failed = true;}
				}
			}
			Iterator<Node> nodes = (new Vector<Node>(om2.getTarget().getNodesSet())).iterator();
			while (nodes.hasNext()) {
				Node go = nodes.next();
				if (!om1.getInverseImage(go).hasMoreElements()
						&& !om2.getInverseImage(go).hasMoreElements()) {
					try {
						om2.getTarget().destroyNode(go, false, true);
					} catch (TypeException ex) {failed = true;}
				}
			}
			if (!failed) {
				overlap.second = null;
				return true;
			}
		}
		return false;
	}
	
	protected void setGraphNameOfDeleteUseConflict(Rule r1, Rule r2,
			final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			overlaps) {
		// mark critical objects
		for (int i = 0; i < overlaps.size(); i++) {
			final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			pi = overlaps.get(i);
			final Graph overlapGraph = pi.first.first.getTarget();
			
			// set name of overlap graph					
			if ((this instanceof DependencyPair) 
					&& !this.checkSwitchDependency) {	
				if (overlapGraph.getName().indexOf("produce-need(PAC:") == -1) {					
					overlapGraph.setName(CriticalPairData.PRODUCE_USE_D_TXT); //"produce-use-dependency");	
				}
				pi.first.first.setName("MorphOf_"+r1.getName());
				pi.first.second.setName("MorphOf_"+r2.getName());
			
			}
			else {						
				if (overlapGraph.getName().indexOf("delete-need(PAC:") == -1)	{				
					overlapGraph.setName(CriticalPairData.DELETE_USE_C_TXT); //"delete-use-conflict");
				// graph name containing a PAC name is set in getOverlappingsVectorDeleteUse
				}
				pi.first.first.setName("MorphOf_"+r1.getName());
				pi.first.second.setName("MorphOf_"+r2.getName());
			}
		}
	}
	
	protected void setGraphNameOfDeleteUseConflict(Rule r1, Rule r2,
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			overlaps,
			final String pacName) {
		// mark critical objects
		for (int i = 0; i < overlaps.size(); i++) {
			final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			pi = overlaps.get(i);
			final Graph overlapGraph = pi.first.first.getTarget();
				
			// set name of overlap graph					
			if ((this instanceof DependencyPair) 
					&& !this.checkSwitchDependency) {	
				if (overlapGraph.getName().indexOf("produce-need(PAC:") == -1) {					
					overlapGraph.setName(CriticalPairData.PRODUCE_USE_D_TXT); //"produce-use-dependency");	
				}
				if (pacName != null && pacName.length() != 0) {
					overlapGraph.setName(CriticalPairData.PRODUCE_NEED_D_TXT+" (PAC: " + pacName+ ")");
					overlapGraph.setHelpInfo("PAC:"+pacName);
				}
				pi.first.first.setName("MorphOf_"+r1.getName());
				pi.first.second.setName("MorphOf_"+r2.getName());		
			}
			else {						
				if (overlapGraph.getName().indexOf("delete-need(PAC:") == -1)	{				
					overlapGraph.setName(CriticalPairData.DELETE_USE_C_TXT); //"delete-use-conflict");
				// graph name containing a PAC name is set in getOverlappingsVectorDeleteUse
				}
				if (pacName != null && pacName.length() != 0) {
					overlapGraph.setName(CriticalPairData.DELETE_NEED_C_TXT+" (PAC: " + pacName+ ")");
					overlapGraph.setHelpInfo("PAC:"+pacName);
				}
				pi.first.first.setName("MorphOf_"+r1.getName());
				pi.first.second.setName("MorphOf_"+r2.getName());
			}
		}
	}
	
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getEssentialDeleteUseConflicts(
			final Rule r1, 
			final Rule r2) {
		
		final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlaps = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();

		this.cpdKind = CriticalPairData.DELETE_USE_CONFLICT;
		
		// only for dependency check
		if (this instanceof DependencyPair) {
			if (!this.checkSwitchDependency 
					&& this.duIndxStr.endsWith(":1")) {
				return overlaps;
			}
			else if (this.checkSwitchDependency 
					&& this.duIndxStr.endsWith(":0")) {
				this.duIndxStr = "";
			}
		}

		this.inclCount = 0;
		this.inclProgress = 0;
		this.duIndx = this.getDUIndx();
		String duNameIndx = this.duIndxStr.contains(":PAC")? this.getDUNameIndx(): "";
		
		Enumeration<OrdinaryMorphism> pacs2 = r2.getEnabledPACs();
		
		final Graph g = r1.getLeft();
		int maxSize = r2.getLeft().getSize();
		int size = maxSize;
				
		// check NACs and add found objs to contextC1_l1 
		// and remove from preservedK1_l1
//		findMorphismNACintoRHSAndAddToContext(r1, true, contextC1_l1,
//					preservedK1_l1, this.typesTG_L2, this.delete);
			
		// check multiplicity max==1 and add such objs to contextC1_l1 and
		// remove from preservedK1_l1
//		addToContextIfTypeMaxMultiplicitySet(r1.getLeft(), contextC1_l1,
//					preservedK1_l1, this.typesTG_L2);

		this.inclAsGraph = false;
			
		boolean lhs_done = false;
		boolean perform = true;
		while (perform && !this.stop) {
			String pacName = "";
			// consider PACs
			OrdinaryMorphism L2isoL2ExtendedByPACs = null;
			Pair<OrdinaryMorphism, OrdinaryMorphism> lhs_pac_pair = null;
			
			OrdinaryMorphism pac = null;
			if (this.withPACs && pacs2.hasMoreElements()) {				
				pac = pacs2.nextElement();
				if (this.duIndx > 0 
						&& !duNameIndx.isEmpty()
						&& !pac.getName().equals(duNameIndx))
					continue;
			}
			// check PAC and reset maxSize if needed
			if (pac != null) {
				if (pac.isEnabled()) {
					boolean pacCritical = ExcludePairHelper.isCriticalPAC(pac, this.delete);
					if (pacCritical) {					
						L2isoL2ExtendedByPACs = r2.getLeft().isomorphicCopy();
						if (L2isoL2ExtendedByPACs != null) {
							// extend LHS of r2 by a PAC
							// images of the PAC objects are disjunct (injective)
							lhs_pac_pair = extendLeftGraphByPAC(L2isoL2ExtendedByPACs, pac, false);
							pacName = pac.getName();
							maxSize = L2isoL2ExtendedByPACs.getTarget().getSize();
							lhs_done = true;
						}
					}
				}
			}
			
			Vector<Vector<GraphObject>> inclusions = null;
			size = this.contextC1_L1.size();
			if (size > maxSize) size = maxSize;	
			
			Vector<Vector<GraphObject>> 
			contextCombis = ExcludePairHelper.getInclusions(g, size, this.contextC1_L1, true);		
			// each inclusion should contain at least one object to delete
			checkInclusions(contextCombis, this.delete);				
				
			if (contextCombis.size() == 0) {
				// continue with next PAC
				if (pacs2.hasMoreElements())
					continue;
				else
					break;	
			}
				
			if (namedObjectOnly) {
				this.checkInclusionsDuetoNamedObject(contextCombis);
				if (contextCombis.size() == 0) {
					// continue with next PAC
					if (pacs2.hasMoreElements())
						continue;
					else
						break;	
				}
			}
				
			int nn = 0;
			if (!this.stop) {
				if (this.duIndx > 0) {
					// copy not computed inclusions
					inclusions = new Vector<Vector<GraphObject>>();	
					for (int i=0; i<this.duIndx && i<contextCombis.size(); i++) {
						inclusions.add(contextCombis.get(i));
					}
					nn = contextCombis.size() - this.duIndx;
					this.duIndx = -1;
				}
				else {
					inclusions = contextCombis;
				}
					
				this.inclCount = inclusions.size();			
				System.out.println("to check essential inclusions: "+ this.inclCount +"   already checked: "+(nn+1));
				nn = nn+inclusions.size();
					
				// help for system.out.println
				int nn100 = 0;
				int nn2 = 0;
	
				// make and check inclusion morphism
				int i=inclusions.size()-1;
				while (i >= 0 && !this.stop) {					
					Vector<GraphObject> inclSet = inclusions.get(i); 
					i--;
					
					nn100++; // counter of 100
						
					// make overlapping
					OrdinaryMorphism inclMorphism = null;
					Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
					localOverlaps = null;
					// with LHS of the second rule
					if (L2isoL2ExtendedByPACs == null) {
						if (r2.getSource().getSize() >= inclSet.size()) 
							inclMorphism = makeInclusionMorphism(inclSet, g);
						if ((inclMorphism != null)
								&& (r2.getSource().getNodesCount() >= inclMorphism.getSource().getNodesCount())
								&& (r2.getSource().getArcsCount() >= inclMorphism.getSource().getArcsCount())) {
							
							localOverlaps = getOverlappingsVectorDeleteUse(r1, r2, inclMorphism);						
							setGraphNameOfDeleteUseConflict(r1, r2, localOverlaps);
							overlaps.addAll(localOverlaps);	
							
							localOverlaps.clear(); localOverlaps = null;
							inclMorphism.dispose(true, false); inclMorphism = null;
						}
						lhs_done = true;
					}
					else {
						// with extended LHS by PAC of the second rule 
						if (lhs_pac_pair.first.getTarget().getSize() >= inclSet.size()) 
							inclMorphism = makeInclusionMorphism(inclSet, g);
						if ((inclMorphism != null)
								&& (lhs_pac_pair.first.getTarget().getNodesCount() >= inclMorphism.getSource().getNodesCount())
								&& (lhs_pac_pair.first.getTarget().getArcsCount() >= inclMorphism.getSource().getArcsCount())) {
							
							localOverlaps = getOverlappingsVectorDeleteUse(r1, r2, lhs_pac_pair, inclMorphism);
							setGraphNameOfDeleteUseConflict(r1, r2, localOverlaps, pacName);
							for (int x=0; x<localOverlaps.size(); x++) {
								unsetAllTransientAttrValuesOfOverlapGrah(localOverlaps.get(x).first.first.getTarget());
							}							
							overlaps.addAll(localOverlaps);	
							
							localOverlaps.clear(); localOverlaps = null;
							inclMorphism.dispose(true, false); inclMorphism = null;
						}
						lhs_done = true;
					}											
					this.inclProgress++;
											
					if (!this.complete && !overlaps.isEmpty()) {
						break;
					}
					if (nn100 == 100) {
						nn2++;
						System.out.println("checked inclusions: "+(nn100*nn2));
						nn100 = 0;
					}
					if (stop) {
						break;
					}
				} // while (i>=0 && !this.stop)
								
				if (stop) {
					// set this.duIndx 
					if (pacName.isEmpty())
						this.saveDUIndx(i, "", this.checkSwitchDependency);
					else
						this.saveDUIndx(i, pacName, this.checkSwitchDependency);
					System.out.println("DeleteUse conflict: stop at index: "+this.duIndx);
					break;
				}
				if (!this.complete && !overlaps.isEmpty()) {
					break;
				}					
			}
				
			contextCombis = null;
			inclusions = null;
			perform = (this.withPACs && pacs2.hasMoreElements()) || !lhs_done; 
		}// while(perform && !stop)
			
		if (!stop) {
			this.saveDUIndx(-1, "", false);
		}
		if (this.withPACs) {
			pacs2 = r2.getEnabledPACs();
			while (pacs2.hasMoreElements()) {
				// restore constant attribute value
				this.replaceVarAttrValueByConst(pacs2.nextElement());
			}
		}
		System.out.println("    ExcludePair.getDeleteUseConflicts::  [ "
					+ r1.getName() + ", " + r2.getName() + " ]  " + overlaps.size()
					+ " critical overlapping(s)");
		overlaps.trimToSize();
		this.cpdKind = -1;
		return overlaps;
	}
	
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getProduceEdgeDeleteNodeConflicts(
			final Rule r1, 
			final Rule r2) {
		System.out.println("    ExcludePair.getProduceEdgeDeleteNodeConflicts::  [ "
				+ r2.getName() + ", " + r1.getName() + " ] ...");
		this.cpdKind = CriticalPairData.PRODUCE_EDGE_DELTE_NODE_CONFLICT;
		this.inclCount = 0;
		
		// NOTE: r1 deletes node, r2 used node and produce an edge at it
		// 
		final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlaps = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
				
		// generate critical inclusions as Hashtable:
		// - key is Integer(size) of inclusion,
		// - object is Vector with GraphObjects;
		// each inclusion should contain at least one graph object to be deleted
		final Graph g = r1.getLeft();
		int maxSize = r2.getLeft().getSize();
		int size = maxSize;
		Vector<Vector<GraphObject>> inclusions;
						
		this.inclAsGraph = false;
		
		size = this.contextC1_L1.size();
		if (size > maxSize)
			size = maxSize;
			
		Vector<Vector<GraphObject>> 
		contextCombis = ExcludePairHelper.getInclusions(g, size, this.contextC1_L1, true);
			
		checkInclusions(contextCombis, this.delete);
				
		if (contextCombis.size() > 0) { 		
			if (namedObjectOnly) 
				this.checkInclusionsDuetoNamedObject(contextCombis);
		}
		
		if (contextCombis.size() > 0) {			
			
			if (this.essential) {	
				inclusions = contextCombis;	
				System.out.println("essential inclusions  : "+inclusions.size());
			}
			else {				
				size = this.preservedK1_L1.size();
				if (size > maxSize)
					size = maxSize;
					
				Vector<Vector<GraphObject>> 
				preservedCombis = ExcludePairHelper.getPlainCombinedInclusions(
							new Vector<GraphObject>(this.preservedK1_L1), size, g);
									
				inclusions = ExcludePairHelper.combineInclusions(maxSize, contextCombis,
							preservedCombis, this.boundB1_L1);
					
				contextCombis.removeAllElements();
				preservedCombis.removeAllElements(); 
				contextCombis = null;
				preservedCombis = null;
			} 
				
			System.out.println("to check inclusions: "+inclusions.size());
				
			// make and check inclusion morphism
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			localOverlaps = null;
			while (inclusions.size() > 0 && !this.stop) {
				this.inclCount = inclusions.size();
					
				Vector<GraphObject> inclSet = inclusions.get(0);
				inclusions.remove(inclSet);
					
				OrdinaryMorphism inclMorphism = null;
				if (r2.getSource().getSize() >= inclSet.size()) 
					inclMorphism = makeInclusionMorphism(inclSet, g);
				
				// make overlappings with LHS of the second rule
				if ((inclMorphism != null)
						&& (r2.getSource().getNodesCount() >= inclMorphism.getSource().getNodesCount())
						&& (r2.getSource().getArcsCount() >= inclMorphism.getSource().getArcsCount())) {
					
					localOverlaps = getOverlappingsVectorDeleteUse(r1, r2, inclMorphism);
							
					//TEST
					//this.inspectCritPair(r1, r2, localOverlaps);
				
					// mark critical objects
					for (int i = 0; i < localOverlaps.size(); i++) {
						final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
						pi = localOverlaps.get(i);
							
						OrdinaryMorphism m1 = pi.first.first;
						OrdinaryMorphism m2 = pi.first.second;
									
						if (this.danglingEdge(r1, r2, m1, m2)) {
							// set name of the overlap graph																				
		//					m1.getTarget().setName("produceEdge-deleteNode-conflict");
							m1.getTarget().setName(CriticalPairData.PRODUCE_EDGE_DELETE_NODE_C_TXT);
							
							// to check this conflict the rules are changed: r1 is rule2 and r2 is rule1
							// so change m1 and m2 to get right order for (rule1, rule2)				
							pi.first.first = m2;
							pi.first.second = m1;							
						} else {
							localOverlaps.remove(i);
							i--;
						}					
					}			
					overlaps.addAll(localOverlaps);
					
					localOverlaps.clear();	localOverlaps = null;
					inclMorphism.dispose(true, false); inclMorphism = null;
				}	
				if (!this.complete && !overlaps.isEmpty()) {
					break;
				}
			}
		}
		inclusions = null;
		System.out.println("    ExcludePair.getProduceEdgeDeleteNodeConflicts::  [ "
					+ r2.getName() + ", " + r1.getName() + " ]  " + overlaps.size()
					+ " critical overlapping(s)");
		overlaps.trimToSize();
		this.cpdKind = -1;
		return overlaps;
	}

	private void markDeleteUseCriticalObject(
			final Rule r2,
			final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> pi) {
		String pacName = "";
		OrdinaryMorphism m1 = pi.first.first;
		OrdinaryMorphism m2 = pi.first.second;
		Iterator<?> en = m1.getTarget().getNodesSet().iterator();
		while (en.hasNext()) {
			GraphObject o = (GraphObject) en.next();
			if (m1.getInverseImage(o).hasMoreElements()) {
				GraphObject go1 = m1.getInverseImage(o).nextElement();
				if (m2.getInverseImage(o).hasMoreElements()) {
					if (this.delete.contains(go1)) {						
						o.setCritical(true);
					}
				} else if (pi.second != null) {
					// get PAC of r2
					OrdinaryMorphism pac_r2 = pi.second.first.compose(pi.second.second);
					if (pac_r2.getInverseImage(o).hasMoreElements()) {
						GraphObject go2 = pac_r2.getInverseImage(o).nextElement();
						if (this.delete.contains(go1)) {
							o.setCritical(true);
							pacName = go2.getContext().getName();
						}
					}
				}
			}
		}
		en = m1.getTarget().getArcsSet().iterator();
		while (en.hasNext()) {
			GraphObject o = (GraphObject) en.next();
			if (m1.getInverseImage(o).hasMoreElements()) {
				GraphObject go1 = m1.getInverseImage(o).nextElement();
				if (m2.getInverseImage(o).hasMoreElements()) {
					if (this.delete.contains(go1))
						o.setCritical(true);
				} else if (pi.second != null) {
					// get PAC of r2
					OrdinaryMorphism pac_r2 = pi.second.first.compose(pi.second.second);
					if (pac_r2.getInverseImage(o).hasMoreElements()) {
						GraphObject go2 = pac_r2.getInverseImage(o).nextElement();
						if (this.delete.contains(go1)) {
							o.setCritical(true);
							pacName = go2.getContext().getName();
						}
					}
				}
			}
		}
		
		if (!"".equals(pacName)) {
			// set name of overlap graph with PAC
			if (this instanceof DependencyPair) {
				m1.getTarget().setName(CriticalPairData.PRODUCE_NEED_D_TXT + " (PAC: " + pacName + ")");
			} else {
				m1.getTarget().setName(CriticalPairData.DELETE_NEED_C_TXT + " (PAC: " + pacName + ")");
			}
		}
	}
	
	
	private void markProduceForbidCriticalObject(
			final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p) {
		Pair<OrdinaryMorphism, OrdinaryMorphism> p1 = p.first;
		Pair<OrdinaryMorphism, OrdinaryMorphism> p2 = p.second;
		OrdinaryMorphism m1 = p1.first;
		OrdinaryMorphism m2 = p1.second;
		OrdinaryMorphism isoL2 = p2.first;
		OrdinaryMorphism isoNAC2 = p2.second;
		Iterator<?> gos = m1.getTarget().getNodesSet().iterator();
		while (gos.hasNext()) {
			GraphObject o = (GraphObject) gos.next();
			if (m1.getInverseImage(o).hasMoreElements()) {
				if (m2.getInverseImage(o).hasMoreElements()) {
					GraphObject go = m1.getInverseImage(o).nextElement();
					if (this.produce.contains(go))
						o.setCritical(true);
				} else if (isoL2.getInverseImage(o).hasMoreElements()) {
					GraphObject go = isoL2.getInverseImage(o).nextElement();
					if (this.produce.contains(go)) 
						o.setCritical(true);
				} else if (isoNAC2 != null) {					
					if (isoNAC2.getInverseImage(o).hasMoreElements()) {
						GraphObject go = isoNAC2.getInverseImage(o).nextElement();
						if (this.produce.contains(go)) 
							o.setCritical(true);
					}
				} 
			}
		}
		gos = m1.getTarget().getArcsSet().iterator();
		while (gos.hasNext()) {
			GraphObject o = (GraphObject) gos.next();
			if (m1.getInverseImage(o).hasMoreElements()) {
				if (m2.getInverseImage(o).hasMoreElements()) {
					GraphObject go = m1.getInverseImage(o).nextElement();
					if (this.produce.contains(go))
						o.setCritical(true);
				} else if (isoL2.getInverseImage(o).hasMoreElements()) {
					GraphObject go = isoL2.getInverseImage(o).nextElement();
					if (this.produce.contains(go)) 
						o.setCritical(true);
				} else if (isoNAC2 != null) {					
					if (isoNAC2.getInverseImage(o).hasMoreElements()) {
						GraphObject go = isoNAC2.getInverseImage(o).nextElement();
						if (this.produce.contains(go)) 
							o.setCritical(true);
					}
				} else if (m1.getInverseImage(o).hasMoreElements()) {
					GraphObject go = m1.getInverseImage(o).nextElement();
					if (this.produce.contains(go)) 
						o.setCritical(true);
				}
			}
		}
	}
	
	
	@SuppressWarnings("unused")
	private OrdinaryMorphism getPAC(Rule r, GraphObject goOfPAC) {
		final List<OrdinaryMorphism> pacs = r.getPACsList();
		for (int l=0; l<pacs.size(); l++) {
			final OrdinaryMorphism pac = pacs.get(l);		
			Iterator<?> elems = pac.getTarget().getNodesSet().iterator();
			while (elems.hasNext()) {
				GraphObject go = (GraphObject) elems.next();
				if (goOfPAC == go || goOfPAC.getContextUsage() == go.hashCode())
					return pac;
			}
			elems = pac.getTarget().getArcsSet().iterator();
			while (elems.hasNext()) {
				GraphObject go = (GraphObject) elems.next();
				if (goOfPAC == go || goOfPAC.getContextUsage() == go.hashCode())
					return pac;
			}
		}
		return null;
	}
	
	Vector<OrdinaryMorphism> getPotentialCriticalNACsOfR2(
			final Rule r, 
			final Vector<GraphObject> toproduce) {
		Vector<OrdinaryMorphism> result = new Vector<OrdinaryMorphism>();
		final List<OrdinaryMorphism> nacs = r.getNACsList();
		for (int l=0; l<nacs.size(); l++) {
			final OrdinaryMorphism nac = nacs.get(l);
			if (!nac.isEnabled())
				continue;

			Hashtable<Type, Vector<GraphObject>> 
			type2gosNAC2 = new Hashtable<Type, Vector<GraphObject>>();
			boolean nacMaybeCritical = false;
			boolean attrTest = false;
			boolean nacAttrSet = false;
			for (int j = 0; j < toproduce.size() && !nacMaybeCritical; j++) {
				GraphObject o = toproduce.get(j);
				Vector<GraphObject> v = type2gosNAC2.get(o.getType());
				if (v == null)
					v = nac.getTarget().getElemsAndParentsOfType(o.getType());
				else
					v.addAll(nac.getTarget().getElemsAndParentsOfType(o.getType()));
				if (!v.isEmpty()) {
					type2gosNAC2.put(o.getType(), v);
					for (int i=0; i<v.size(); i++) {
						GraphObject go = v.get(i);
						if (nac.getInverseImage(go).hasMoreElements()) {
							if (!nac.getTarget().hasObjectWithVarOrConstInAttrs(true, true)) {
								v.remove(go);
								i--;
							}
						}
					}
				}
				for (int i=0; i<v.size() && !nacMaybeCritical; i++) {
					GraphObject go = v.get(i);
					Enumeration<GraphObject> en = nac.getInverseImage(go);
					if (en.hasMoreElements()) {
						if (go.getAttribute() != null) {
							GraphObject o_lhs = en.nextElement();						
							nacAttrSet = false;
							for (int k=0; k<go.getAttribute().getNumberOfEntries(); k++) {
								ValueMember vm_nac = (ValueMember)go.getAttribute().getMemberAt(k);
								ValueMember vm_lhs = (ValueMember)o_lhs.getAttribute().getMemberAt(vm_nac.getName());
								if (vm_nac.isSet()) {
									if (!vm_lhs.isSet() || !vm_nac.getExprAsText().equals(vm_lhs.getExprAsText())) {
										nacAttrSet = true;
										break;
									}
								}
							}
							nacMaybeCritical = nacAttrSet;
							if (nacMaybeCritical) {
								go.setContextUsage(nac.hashCode());
								o_lhs.setContextUsage(nac.hashCode());
							}
						}
					}
					else { // new NAC go 
						if (go.getAttribute() != null) {
							nacAttrSet = false;
							for (int k=0; k<go.getAttribute().getNumberOfEntries(); k++) {
								ValueMember vm_nac = (ValueMember)go.getAttribute().getMemberAt(k);
								if (vm_nac.isSet()) {
									nacAttrSet = true;
									ValueMember vm_rhs = (ValueMember)o.getAttribute().getMemberAt(vm_nac.getName());
									if (vm_rhs.isSet()) {
										attrTest = true;
										if (vm_nac.getExpr().isConstant() && vm_rhs.getExpr().isConstant()
												&& !vm_rhs.getExprAsText().equals(vm_nac.getExprAsText())) {
											attrTest = false;
											break;
										}						
									}
									else 
										nacAttrSet = false;	
								}
							}
							nacMaybeCritical = (attrTest) || !nacAttrSet;
						}
						else {
							nacMaybeCritical = true;
						}
						if (nacMaybeCritical)
							go.setContextUsage(nac.hashCode());
					}
				}
			}
			
			if (nacMaybeCritical) 
				result.add(nac);
		}
		result.trimToSize();
		return result;
	}
	
	private boolean danglingEdgeAfterFirstProduceSecondDelete(
			final Rule r1,
			final Rule r2) {
		
		if (this instanceof DependencyPair) {
			return false;
		}
		boolean result = false;
		final List<GraphObject> objToDelete = r2.getElementsToDelete();		
		for (int i=0; i<objToDelete.size(); i++) {
			final GraphObject go = objToDelete.get(i);
			if (go.isNode()) {
				final Enumeration<GraphObject> objsOfType = r1.getRight().getElementsOfType(go.getType());
				while (objsOfType.hasMoreElements()) {
					final GraphObject o = objsOfType.nextElement();
					// the node to delete by the rule2 is preserved by the rule1
					if (r1.getInverseImage(o).hasMoreElements()) {						
						Iterator<Arc> arcs = ((Node)o).getOutgoingArcsSet().iterator();
						while (arcs.hasNext()) {
							final Arc arc = arcs.next();
							if (!r1.getInverseImage(arc).hasMoreElements()) {
								if (!this.danglingEdges.contains(go))
									this.danglingEdges.add(go);
								result = true;
							}
						}
						arcs = ((Node)o).getIncomingArcsSet().iterator();
						while (arcs.hasNext()) {
							final Arc arc = arcs.next();
							if (!r1.getInverseImage(arc).hasMoreElements()) {
								if (!this.danglingEdges.contains(go))
									this.danglingEdges.add(go);
								result = true;
							}
						}
					}
				}
			}
		}				
		return result;
	}
	
	private boolean danglingEdge(
			final Rule r1,
			final Rule r2,
			final OrdinaryMorphism m1,
			final OrdinaryMorphism m2) {

		boolean result = false;
		final Iterator<Node> objs = m1.getTarget().getNodesSet().iterator();
		while (objs.hasNext()) {		
			final Node go = objs.next();
			if (!result && go.isCritical()) {
				if (m1.getInverseImage(go).hasMoreElements()
						&& m2.getInverseImage(go).hasMoreElements()) {					
//					final Node go1 = (Node) m1.getInverseImage(go).nextElement();				
					final Node go2 = (Node) m2.getInverseImage(go).nextElement();					
					final Node img2 = (Node) r2.getImage(go2);
					if (img2 != null) {
						Iterator<Arc> arcs = img2.getOutgoingArcsSet().iterator();
						while (arcs.hasNext()) {
							final Arc arc = arcs.next();
							if (!r2.getInverseImage(arc).hasMoreElements()) {
								result = true;
								break;
							}
						}
						arcs = img2.getIncomingArcsSet().iterator();
						while (arcs.hasNext()) {
							final Arc arc = arcs.next();
							if (!r2.getInverseImage(arc).hasMoreElements()) {
								result = true;
								break;
							}
						}
					}
				}
			} 
		}
		
		final Iterator<Arc> objs1= m1.getTarget().getArcsSet().iterator();
		while (objs1.hasNext()) {		
			final Arc go = objs1.next();
			if (go.isCritical()) {
				go.setCritical(false);
			}
		}
		
		return result;
	}
	
	private int getPFIndx() {
		try {
			this.pfIndx = Integer.valueOf(this.pfIndxStr.split(":")[0]).intValue();
		} catch (NumberFormatException ex) {
			this.pfIndx = -1;
		}
		return this.pfIndx;
	}
	
	private String getPFNameIndx() {
		try {
			return this.pfIndxStr.split(":")[2];
		} catch (ArrayIndexOutOfBoundsException ex) {}
		return "";
	}
	
	private int getPFIndx2() {
		try {
			return Integer.valueOf(this.pfIndxStr.split(":")[1]).intValue();
		} catch (ArrayIndexOutOfBoundsException ex) {}
		return -1;
	}
	
	private void savePFIndx(int i, int pci, String s, boolean switchDep) {
		//TODO also for switch dependency
		this.pfIndx = i;
		String swDep = switchDep? "1": "0";
		this.pfIndxStr = String.valueOf(this.pfIndx)
							.concat(":").concat(String.valueOf(pci))
							.concat(":").concat(s)
							.concat(":").concat(swDep);
	}
	
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getProduceForbidConflicts(
			final Rule r1,
			final Rule r2) {
		System.out.println("    ExcludePair.getProduceForbidConflicts::  [ "
				+ r1.getName() + ", " + r2.getName() + " ] ... ");
		
		final Vector<OrdinaryMorphism> 
		potentialCriticalNACsOfR2 = this.getPotentialCriticalNACsOfR2(r2, this.produce);				
		if (potentialCriticalNACsOfR2.isEmpty()) {
			return null;
		}
		
		this.cpdKind = CriticalPairData.PRODUCE_FORBID_CONFLICT;
		if (this.essential) {
			return this.getEssentialProduceForbidConflicts(r1, r2, potentialCriticalNACsOfR2);
		}
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlaps = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();

		// only for dependency check
		if (this instanceof DependencyPair) {
			if (!this.checkSwitchDependency 
					&& this.pfIndxStr.endsWith(":1")) {
				return overlaps;
			}
			else if (this.checkSwitchDependency 
					&& this.pfIndxStr.endsWith(":0")) {
				this.pfIndxStr = "";
			}
		}

		this.inclCount = 0;
		this.inclProgress = 0;
		this.pfIndx = this.getPFIndx();
		String pfNameIndx = this.getPFNameIndx();
		int pfIndxPCI = this.getPFIndx2();
		
		if (this.ac2leftExtended == null)
			this.ac2leftExtended = new Hashtable<OrdinaryMorphism,Pair<OrdinaryMorphism, OrdinaryMorphism>>();
		
		final Graph g = r1.getRight();
		
		// generate critical inclusions as Hashtable:
		// - key is Integer(size) of inclusion,
		// - object is Vector with its GraphObjects;
		// each inclusion should contain at least one graph object to be produced
				
		this.inclAsGraph = false;
		// make context combis		
		int size = this.contextC1_R1.size();
		Vector<Vector<GraphObject>> 
		contextCombis = ExcludePairHelper.getInclusions(g, size, this.contextC1_R1, true);		
		
		checkInclusions(contextCombis, this.produce);
				
		if (contextCombis.size() > 0) {	
			if (namedObjectOnly)
				this.checkInclusionsDuetoNamedObject(contextCombis);
		}
		
		if (contextCombis.size() > 0) {	
			// make preserved combis	
			size = this.preservedK1_R1.size();			
			Vector<Vector<GraphObject>>
			preservedCombisAll = ExcludePairHelper.getPlainCombinedInclusions(this.preservedK1_R1, size, g); 
					
			Vector<Vector<GraphObject>> 
			inclusions = new Vector<Vector<GraphObject>>();
					
			// to start the while-loop
			Vector<GraphObject> preservedCombis = null;
			boolean contextCombisDone = (pfIndxPCI >= 0);
			boolean perform = !contextCombis.isEmpty();	
			int pci = -1;
			while (!this.stop && perform) {
	//			System.out.println(pfIndx+"     "+pfIndxPCI+"   "+pfNameIndx);
				if (contextCombisDone) {
					if (pfIndxPCI >= 0) {
						int i = preservedCombisAll.size()-1;
						while (i > pfIndxPCI) {
							preservedCombisAll.remove(i);
							i = preservedCombisAll.size()-1;
						}
						pfIndxPCI = -1;
					}
					pci = preservedCombisAll.size()-1;
					if (!preservedCombisAll.isEmpty()) {
						preservedCombis = preservedCombisAll.remove(pci);
					}
					else {
						perform = false;
						break;
					}
				}		
				int ncp = 0;
				// do loop over NACs
				int run = 0;
				final Enumeration<OrdinaryMorphism> nacs2 = potentialCriticalNACsOfR2.elements();
				while (!this.stop && nacs2.hasMoreElements()) {							
					OrdinaryMorphism  nac = nacs2.nextElement();
					
					// to continue stopped CPA get the stopped NAC
					if (!pfNameIndx.isEmpty()) {
						if (this.pfIndx >= 0 && nac.getName().equals(pfNameIndx)) {
							pfNameIndx = "";
							if (this.pfIndx == 0) {
								this.pfIndx = -1;
								continue;
							}
						}
						else
							continue;
					}
													
					Pair<OrdinaryMorphism, OrdinaryMorphism> 
					extendedL2isoPair = this.ac2leftExtended.get(nac);
					if (extendedL2isoPair == null) 
						extendedL2isoPair = extendGraphForProduceForbidConflict(r2.getLeft(), nac);
					if (extendedL2isoPair != null)
						this.ac2leftExtended.put(nac, extendedL2isoPair);
					else
						continue;
										
					Vector<Vector<GraphObject>> 
					workInclusions = new Vector<Vector<GraphObject>>();
					int maxSize = 0;
					if (contextCombisDone) {
			//			combine context with preserved				
						if (extendedL2isoPair.first.getTarget().getSize() > maxSize) {
							maxSize = extendedL2isoPair.first.getTarget().getSize();						
							inclusions = ExcludePairHelper.combineInclusionsOf(
												maxSize, contextCombis,
												preservedCombis, this.boundB1_R1);						
							workInclusions.addAll(inclusions);
						}
					} 
					else {	
						workInclusions.addAll(contextCombis); 
					}
					
					if (workInclusions.size() > 0) {
						if (this.pfIndx > 0 && this.pfIndx < workInclusions.size()) {
							// reduce inclusions up to this.pfIndx
							int i = workInclusions.size()-1;
							while (i >= 0 && i >= this.pfIndx) {
								workInclusions.remove(i);  
								i = workInclusions.size()-1;
							}
							// unset this.pfIndx
							this.pfIndx = -1;
						}
						
						this.inclCount = workInclusions.size();	
						run++;
						System.out.println("(run: "+run+") to check inclusions: "+this.inclCount+"    already checked: "+ncp+"   of NAC: "+nac.getName());
							
						this.checkInclsProduceForbidForNAC(r1, r2, nac, extendedL2isoPair, workInclusions, g, pci, overlaps);
		
						// set the number of all valid inclusions
						ncp = ncp + this.inclCount;
						
						System.out.println("    ExcludePair.getProduceForbidConflicts::  [ "
										+ r1.getName() + ", " + r2.getName() 
										+ "  NAC: "+nac.getName()
										+ " ]  "
										+ overlaps.size() + " critical overlapping(s)");
					}			
					replaceVarAttrValueByConst(nac);
					
					workInclusions = null;
					
					if (!this.complete && !overlaps.isEmpty()) {
						break;
					}
				} // while (!this.stop && nacs.hasMoreElements())
				contextCombisDone = true;
			} // while (!this.stop && perform)	
		
			contextCombis = null;
			preservedCombis = null;
			inclusions = null;
		}
		
		if (this.ac2leftExtended != null) {
			this.ac2leftExtended.clear();
		}
		if (!stop) {
			this.savePFIndx(-1, -1, "", false);
		}
		//test overlap graphs and reduce isomorphic
		if (/*!r1.getTypeSet().isArcDirected() && */overlaps.size() > 0) {
			reduceCriticalPairs(overlaps);
		}
		overlaps.trimToSize();
		this.cpdKind = -1;
		System.gc();
		return overlaps;
	}
	
	private void checkInclsProduceForbidForNAC(
			final Rule r1, 
			final Rule r2,
			final OrdinaryMorphism nac,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> extendedL2isoPair,
			final Vector<Vector<GraphObject>> workInclusions,
			final Graph g,
			int pci,
			final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			result) {
		
		// help for system.out.println
		int n100 = 0, nn2 = 0;
		boolean todo = workInclusions.size() > 0;
		// loop over inclusions 
		int i = workInclusions.size()-1;
		while (i>=0 && !this.stop) {					
			Vector<GraphObject> inclSet = workInclusions.remove(i);
			i = workInclusions.size()-1;
			
			OrdinaryMorphism inclMorphism = makeInclusionMorphism(inclSet, g);						
			if (inclMorphism != null
					&& (extendedL2isoPair.first.getTarget().getNodesCount() >= inclMorphism.getSource().getNodesCount())
					&& (extendedL2isoPair.first.getTarget().getArcsCount() >= inclMorphism.getSource().getArcsCount())) {
				
				this.checkProduceForbidForNAC(r1, r2, nac, inclMorphism, extendedL2isoPair, result);
			}
			this.inclProgress++;
			n100++;
			
			if (!this.complete && !result.isEmpty()) {
				break;
			}
			if (n100 == 100) {
				nn2++; 
				System.out.println("checked  inclusions: "+n100*nn2+"     to check: "+(i+1));
				n100 = 0;
			}

			// max bound will be checked for each NAC
			if (this.maxBoundOfCriticKind > 0 
					&& result.size() >= this.maxBoundOfCriticKind)  {
				break;
			}

		} 
		if (stop) {
			if (!todo || i==-1)
				i = 0;
			// set this.pfIndx 
			this.savePFIndx(i, pci, nac.getName(), this.checkSwitchDependency);
			System.out.println("ProduceForbid conflict: stop at index: "+i+"   of NAC: "+nac.getName());
		}
	}
	
	private void checkProduceForbidForNAC(
			final Rule r1, 
			final Rule r2,
			final OrdinaryMorphism nac,
			OrdinaryMorphism inclMorphism,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> extendedL2isoPair,
			final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			result) {	
		
		this.nacInsideOverlapGraph = nac;
		markNacGraphObjects(nac);
		
		if (this.withInheritance) {
			extendTypeObjectsMapByChildObjs(extendedL2isoPair.first.getTarget());
		}
		// get overlapping morphisms
		final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlaps = getOverlappingsVectorProduceForbid(
											r1, r2, 
											nac,
											extendedL2isoPair, 
											inclMorphism);
		if (!overlaps.isEmpty()) {
			setGraphNameOfProduceForbidConflict(r1, r2, overlaps, nac);
			for (int x=0; x<overlaps.size(); x++) {
				if (this.maxBoundOfCriticKind == 0 
						|| result.size() < this.maxBoundOfCriticKind) {
					result.add(overlaps.get(x));
				}
			}
		}
		
		inclMorphism.dispose(true, false); inclMorphism = null;
	
		unmarkNacGraphObjects(nac);
		this.nacInsideOverlapGraph = null;
	}
	
	private Pair<OrdinaryMorphism, OrdinaryMorphism>  extendGraphForProduceForbidConflict(
			final Graph leftOfRule2,
			final OrdinaryMorphism nacOfRule2) {
		
		OrdinaryMorphism extendedL2iso = leftOfRule2.isomorphicCopy();			
		if (extendedL2iso == null)
			return null;
		
		// extend LHS of r2 by NAC,
		// do not replace constant attribute value by variable
		OrdinaryMorphism 
		extendedNAC2iso = extendLeftGraphByNAC(extendedL2iso, nacOfRule2, false); //true);
		
		// extendedL2isoPair.first is embedding of LHS of r2 into extended LHS
		// extendedL2isoPair.second is embedding of NAC of r2 into extended LHS
		final Pair<OrdinaryMorphism, OrdinaryMorphism> 
		extendedL2isoPair = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
											extendedL2iso, extendedNAC2iso);
		
		// note:
		// extendedL2iso.getTarget() == extendedNAC2iso.getTarget() ==
		// extendedL2isoPair.first.getTarget() == extendedL2isoPair.second.getTarget()
		
		return extendedL2isoPair;
	}
	
	private void setGraphNameOfProduceForbidConflict(Rule r1, Rule r2,
			final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			overlaps,
			final OrdinaryMorphism nac) {
		
		// set name of the overlap graph
		for (int i = 0; i < overlaps.size(); i++) {
			final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> pi = overlaps.get(i);
			final Graph overlapGraph = pi.first.first.getTarget();
			
			if ((this instanceof DependencyPair)
					&& !this.checkSwitchDependency) {
//				overlapGraph.setName("delete-forbid-dependency (NAC: " + nac.getName()+ ")");
				overlapGraph.setName(CriticalPairData.DELETE_FORBID_D_TXT+" (NAC: " + nac.getName()+ ")");
				overlapGraph.setHelpInfo("NAC:"+nac.getName());
				pi.first.first.setName("MorphOf_"+r1.getName());
				pi.first.second.setName("MorphOf_"+r2.getName());				
			} else {
//				overlapGraph.setName("produce-forbid-conflict (NAC: " + nac.getName()+ ")");
				overlapGraph.setName(CriticalPairData.PRODUCE_FORBID_C_TXT+" (NAC: " + nac.getName()+ ")");
				overlapGraph.setHelpInfo("NAC:"+nac.getName());
				pi.first.first.setName("MorphOf_"+r1.getName());
				pi.first.second.setName("MorphOf_"+r2.getName());
			}					
		}
	}
	
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getEssentialProduceForbidConflicts(
			final Rule r1,
			final Rule r2,
			Vector<OrdinaryMorphism> potentialCriticalNACsOfR2) {
				
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		result = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();

		this.cpdKind = CriticalPairData.PRODUCE_FORBID_CONFLICT;
		
		// only for dependency check
		if (this instanceof DependencyPair) {
			if (!this.checkSwitchDependency 
					&& this.pfIndxStr.endsWith(":1")) {
				return result;
			}
			else if (this.checkSwitchDependency 
					&& this.pfIndxStr.endsWith(":0")) {
				this.pfIndxStr = "";
			}
		}
		
		this.inclCount = 0;
		this.inclProgress = 0;
		this.pfIndx = this.getPFIndx();
		String pfNameIndx = this.getPFNameIndx();

		final Graph g = r1.getRight();		
		this.inclAsGraph = false;
		
//		if (this.essential) {
			// check Nacs and add found objs to contextC1_r1 
			// and remove from preservedK1_r1
//			findMorphismNACintoRHSAndAddToContext(r1, false, contextC1_r1,
//					preservedK1_r1, this.typesTG_NAC2, this.produce);
						
			// check multiplicity max=1 and add such objs to contextC1_r1 
			// and remove from preservedK1_r1
//			addToContextIfTypeMaxMultiplicitySet(r1.getRight(), contextC1_r1,
//					preservedK1_r1, this.typesTG_NAC2);
//		}
		
		int size = this.contextC1_R1.size();
		Vector<Vector<GraphObject>> 
		contextCombis = ExcludePairHelper.getInclusions(g, size, this.contextC1_R1, true);			
		
		checkInclusions(contextCombis, this.produce);
			
		if (contextCombis.size() > 0) {			
			if (namedObjectOnly)
				this.checkInclusionsDuetoNamedObject(contextCombis);
		}
		
		if (contextCombis.size() > 0) {				
			// make loop over NACs
			final Enumeration<OrdinaryMorphism> nacs = potentialCriticalNACsOfR2.elements();
			if (nacs.hasMoreElements() && this.ac2leftExtended == null)
				this.ac2leftExtended = new Hashtable<OrdinaryMorphism,Pair<OrdinaryMorphism, OrdinaryMorphism>>();
	
			while (!this.stop && nacs.hasMoreElements()) {						
				OrdinaryMorphism  nac = nacs.nextElement();
				
				// to continue stopped CPA get the stopped NAC
				if (!pfNameIndx.isEmpty()) {
					if ((this.pfIndx > 0 && !nac.getName().equals(pfNameIndx))
							|| (this.pfIndx == 0 && nac.getName().equals(pfNameIndx))) {
						continue;
					}
				}
	
				
				this.nacInsideOverlapGraph = nac;
				markNacGraphObjects(nac);
							
				Pair<OrdinaryMorphism, OrdinaryMorphism> 
				extendedL2isoPair = this.ac2leftExtended.get(nac);
				if (extendedL2isoPair == null) 
					extendedL2isoPair = extendGraphForProduceForbidConflict(r2.getLeft(), nac);
				if (extendedL2isoPair != null)
					this.ac2leftExtended.put(nac, extendedL2isoPair);
				else
					continue;
				
				Vector<Vector<GraphObject>> 
				workInclusions = new Vector<Vector<GraphObject>>(contextCombis);
				if (this.pfIndx > 0) {
					// reduce inclusions up to this.pfIndx
					int i = workInclusions.size()-1;
					while (i >= 0 && i >= this.pfIndx) {
						workInclusions.remove(i);  
						i = workInclusions.size()-1;
					}
					// unset this.pfIndx
					this.pfIndx = -1;
				} 
							
				this.inclCount = workInclusions.size();	
				System.out.println("to check inclusions: "+this.inclCount+"   of NAC: "+nac.getName());
				
				this.checkInclsProduceForbidForNAC(r1, r2, nac, extendedL2isoPair, workInclusions, g, -1, result);
				
				System.out.println("    ExcludePair.getProduceForbidConflicts::  [ "
								+ r1.getName() + ", " + r2.getName() 
								+ "  NAC: "+nac.getName() 
								+ " ]  "
								+ result.size() + " critical overlapping(s)");
				
				replaceVarAttrValueByConst(nac);
				workInclusions = null;
				if ((!this.complete && !result.isEmpty())) {
					break;
				}
			}	
		}
		contextCombis = null;
		if (this.ac2leftExtended != null) {
			this.ac2leftExtended.clear();
		}
		if (!stop) {
			this.savePFIndx(-1, -1, "", false);
		}
		result.trimToSize();
		this.cpdKind = -1;
		return result;
	}
			
	/**
	 * First extend the empty vector usedTypesNAC2 
	 * by elements of the vector usedTypesL2,
	 * then search enabled NACs of the rule r 
	 * and extend the vector usedTypesNAC2 by more types.
	 */
	void getTypeSubsetLeft_NACs(
			final Rule r,
			final Vector<Pair<Type, Pair<Type, Type>>> usedTypesL2,
			final Vector<Pair<Type, Pair<Type, Type>>> usedTypesNAC2) {
		usedTypesNAC2.addAll(usedTypesL2);
		final List<OrdinaryMorphism> nacs = r.getNACsList();
		for (int l=0; l<nacs.size(); l++) {
			final OrdinaryMorphism nac = nacs.get(l);	
			if (nac.isEnabled())
				fillTypeSubset(nac.getTarget(), usedTypesNAC2);
		}
	}

	/**
	 * First extend the empty vector usedTypesPAC2 
	 * by elements of the vector usedTypesL2,
	 * then search enabled PACs of the rule r 
	 * and extend the vector usedTypesPAC2 by more types.
	 */
	void getTypeSubsetLeft_PACs(
			final Rule r,
			final Vector<Pair<Type, Pair<Type, Type>>> usedTypesL2,
			final Vector<Pair<Type, Pair<Type, Type>>> usedTypesPAC2) {
		usedTypesPAC2.addAll(usedTypesL2);
		final List<OrdinaryMorphism> pacs = r.getPACsList();
		for (int l=0; l<pacs.size(); l++) {
			final OrdinaryMorphism pac = pacs.get(l);		
			if (pac.isEnabled())
				fillTypeSubset(pac.getTarget(), usedTypesPAC2);
		}
	}

	/**
	 * Search the graph g and fill the empty typeSubset by the node/edge types.
	 */
	void fillTypeSubset(final Graph g,
			final Vector<Pair<Type, Pair<Type, Type>>> typeSubset) {
		
		Iterator<Node> e = g.getNodesSet().iterator();
		while (e.hasNext()) {
			GraphObject o = e.next();
			if (!isInTypes(typeSubset, o)) {
				typeSubset.add(new Pair<Type, Pair<Type, Type>>(o.getType(),
						null));
			}
		}
		Iterator<Arc> e1 = g.getArcsSet().iterator();
		while (e1.hasNext()) {
			GraphObject o = e1.next();
			Type src = ((Arc) o).getSource().getType();
			Type tar = ((Arc) o).getTarget().getType();
			if (!isInTypes(typeSubset, o)) {
				Pair<Type, Type> srctar = new Pair<Type, Type>(src, tar);
				typeSubset.add(new Pair<Type, Pair<Type, Type>>(o.getType(),
						srctar));
			}
		}
	}


	boolean isInTypes(
			final Vector<Pair<Type, Pair<Type, Type>>> types,
			final GraphObject go) {

		for (int i = 0; i < types.size(); i++) {
			Pair<Type, Pair<Type, Type>> p = types.get(i);
			Type t = go.getType();			
			if (go.isNode()) {
				if (t.isParentOf(p.first)) {//isRelatedTo
					if (!types.contains(t) && !t.convertToKey().equals(p.first.convertToKey())){
						this.withInheritance = true;
					}
					return true;
				}
				else if (t.isChildOf(p.first)) {
					this.withInheritance = true;
					return true;
				}
				
			} else {
				Pair<Type, Type> p2 = p.second;
				if (p.first.compareTo(t)) {
					Type src_t = ((Arc)go).getSource().getType();
					Type tar_t = ((Arc)go).getTarget().getType();
					boolean srcTypeOK = false;
					boolean tarTypeOK = false;
					boolean srcT2parentT = false;
					boolean tarT2parentT = false;	
					if (src_t.isParentOf(p2.first)) {
						srcTypeOK = true;
					}
					else if (src_t.isChildOf(p2.first)) {
						srcT2parentT = true;
						srcTypeOK = true;
					} 
					
					if (tar_t.isParentOf(p2.second)) {
						tarTypeOK = true;
					}
					else if (tar_t.isChildOf(p2.second)){
						tarT2parentT = true;
						tarTypeOK = true;
					} 
					
					if (srcTypeOK && tarTypeOK) {
						if (srcT2parentT || tarT2parentT) {
							this.withInheritance = true;
						}
						return true;	
					}
				}
			}
		}
		return false;
	}

	boolean isChildInTypes(
			final Vector<Pair<Type, Pair<Type, Type>>> types,
			final GraphObject go) {

		Type t = go.getType();	
		for (int i = 0; i < types.size(); i++) {
			Pair<Type, Pair<Type, Type>> p = types.get(i);		
			if (go.isNode()) {
				if (t.convertToKey().equals(p.first.convertToKey())
						//t.isParentOf(p.first)
						) {
					return true;
				}
				else if (t.isChildOf(p.first)) {
					this.withInheritance = true;
					return true;
				}
				
			} else {
				Pair<Type, Type> p2 = p.second;
				if (p.first.compareTo(t)) {
					Type src_t = ((Arc)go).getSource().getType();
					Type tar_t = ((Arc)go).getTarget().getType();
					boolean srcTypeOK = false;
					boolean tarTypeOK = false;
					boolean srcT2childT = false;
					boolean tarT2childT = false;	
					if (src_t.isParentOf(p2.first)	) {
						srcTypeOK = true;
					}
					else if (src_t.isChildOf(p2.first)) {
						srcT2childT = true;
						srcTypeOK = true;
					} 
					
					if (tar_t.isParentOf(p2.second)) {
						tarTypeOK = true;
					}
					else if (tar_t.isChildOf(p2.second)){
						tarT2childT = true;
						tarTypeOK = true;
					} 
					
					if (srcTypeOK && tarTypeOK) {
						if (srcT2childT || tarT2childT) {
							this.withInheritance = true;
						}
						return true;	
					}
				}
			}
		}
		return false;
	}
	
	private void markNacGraphObjects(OrdinaryMorphism nac) {
		for (Iterator<Node> en = nac.getTarget().getNodesSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (!nac.getInverseImage(go).hasMoreElements()) 
				go.setContextUsage(nac.hashCode());
		}
		for (Iterator<Arc> en = nac.getTarget().getArcsSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (!nac.getInverseImage(go).hasMoreElements())
				go.setContextUsage(nac.hashCode());
		}
	}

	private void markNacGraphObjects(OrdinaryMorphism nac, Vector<GraphObject> nacRestriction) {
		for (Iterator<Node> en = nac.getTarget().getNodesSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (!nac.getInverseImage(go).hasMoreElements()) 
				go.setContextUsage(nac.hashCode());
			else if (nacRestriction.contains(go))
				go.setContextUsage(nac.hashCode());
		}
		for (Iterator<Arc> en = nac.getTarget().getArcsSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (!nac.getInverseImage(go).hasMoreElements())
				go.setContextUsage(nac.hashCode());
			else if (nacRestriction.contains(go))
				go.setContextUsage(nac.hashCode());
		}
	}
	
	private void unmarkNacGraphObjects(OrdinaryMorphism nac) {
		for (Iterator<Node> en = nac.getSource().getNodesSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (go.getContextUsage() == nac.hashCode())
				go.setContextUsage(-1);
		}
		for (Iterator<Arc> en = nac.getSource().getArcsSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (go.getContextUsage() == nac.hashCode())
				go.setContextUsage(-1);
		}
		for (Iterator<Node> en = nac.getTarget().getNodesSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (go.getContextUsage() == nac.hashCode())
				go.setContextUsage(-1);
		}
		for (Iterator<Arc> en = nac.getTarget().getArcsSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (go.getContextUsage() == nac.hashCode())
				go.setContextUsage(-1);
		}
	}

/*
	private void markPacGraphObjects(OrdinaryMorphism pac) {
		for (Iterator<Node> en = pac.getTarget().getNodesSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (!pac.getInverseImage(go).hasMoreElements()) {
				go.setContextUsage("" + pac.hashCode());
			}
		}
		for (Iterator<Arc> en = pac.getTarget().getArcsSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (!pac.getInverseImage(go).hasMoreElements()) {
				go.setContextUsage("" + pac.hashCode());
			}
		}
	}

	private void unmarkPacGraphObjects(OrdinaryMorphism pac) {
		for (Iterator<Node> en = pac.getTarget().getNodesSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (go.getContextUsage().equals(String.valueOf(pac.hashCode())))
				go.setContextUsage("");
		}
		for (Iterator<Arc> en = pac.getTarget().getArcsSet().iterator(); en.hasNext();) {
			GraphObject go = en.next();
			if (go.getContextUsage().equals(String.valueOf(pac.hashCode())))
				go.setContextUsage("");
		}
	}
*/
	
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getLeftChangeAttrConflicts(
				final Rule r1,
				final Rule r2,
				final String pacName,
				final Graph g,
				final Pair<OrdinaryMorphism, OrdinaryMorphism> lhs_pac_pair,
				final Vector<Vector<GraphObject>> inclusions) {
		
		final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
		overlapsL2 = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
		
		int i = inclusions.size()-1;
		while (i >= 0 && !this.stop) {
			Vector<GraphObject> inclSet = inclusions.get(i);
			i--;
			
			OrdinaryMorphism inclMorphism = null;
			inclMorphism = makeInclusionMorphism(inclSet, g);			
			if (inclMorphism == null) {
				continue;
			}
							
			// get overlapping morphisms
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			localOverlaps = null;
			if (lhs_pac_pair == null) {
				if (r2.getSource().getSize() >= inclSet.size()) 
					inclMorphism = makeInclusionMorphism(inclSet, g);
				
				// with LHS of second rule only
				if ((inclMorphism != null)
						&& (r2.getSource().getNodesCount() >= inclMorphism.getSource().getNodesCount())
						&& (r2.getSource().getArcsCount() >= inclMorphism.getSource().getArcsCount())) {
					
					localOverlaps = getOverlappingsVectorChangeAttr(r1, r2, null, r2.getLeft(), false, inclMorphism);				
					for (int x = 0; x < localOverlaps.size(); x++) {
						if (this.maxBoundOfCriticKind == 0 
								|| overlapsL2.size() < this.maxBoundOfCriticKind)  {
							final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
							pi = localOverlaps.get(x);
							setGraphNameOfChangeAttrConflict(r1, r2, pi, "");
							overlapsL2.add(pi);
						}
					}
					localOverlaps.clear(); localOverlaps = null;
					inclMorphism.dispose(true, false); inclMorphism = null;
				}
			} else {				
				if (lhs_pac_pair.first.getTarget().getSize() >= inclSet.size()) 
					inclMorphism = makeInclusionMorphism(inclSet, g);
				
				// with extended LHS by PAC of second rule 
				if ((inclMorphism != null)
						&& (lhs_pac_pair.first.getTarget().getNodesCount() >= inclMorphism.getSource().getNodesCount())
						&& (lhs_pac_pair.first.getTarget().getArcsCount() >= inclMorphism.getSource().getArcsCount())) {
					
					localOverlaps = getOverlappingsVectorChangeAttr(r1, r2, null, lhs_pac_pair, false, inclMorphism);					
					for (int x = 0; x < localOverlaps.size(); x++) {
						if (this.maxBoundOfCriticKind == 0 
								|| overlapsL2.size() < this.maxBoundOfCriticKind)  {
							final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
							pi = localOverlaps.get(x);
							setGraphNameOfChangeAttrConflict(r1, r2, pi, pacName);							
							overlapsL2.add(pi);
						}
					}
					localOverlaps.clear(); localOverlaps = null;
					inclMorphism.dispose(true, false); inclMorphism = null;
				}
			}
			this.inclProgress++;
			
			if (!this.complete && !overlapsL2.isEmpty()) {
				break;
			}
			if (this.stop)
				break;

			if (this.maxBoundOfCriticKind > 0 
					&& overlapsL2.size() >= this.maxBoundOfCriticKind)  {
				break;
			}
		}
		if (this.stop) {
			// set this.caIndx 
			if (pacName.isEmpty()) {
				this.saveCAIndx(i, "".concat(":LHS"), this.checkSwitchDependency);
				System.out.println("ChangeAttribute conflict: stop at index: "+i);				
			} 
			else {
				this.saveCAIndx(i, pacName.concat(":PAC"), this.checkSwitchDependency);
				System.out.println("ChangeAttribute conflict: stop at index: "+i+"   of PAC: "+pacName);
			}
		}
		return overlapsL2;
	}
	
	protected void setGraphNameOfChangeAttrConflict(Rule r1, Rule r2,
			final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			overlap,
			final String pacName) {
		// mark critical objects
		final Graph overlapGraph = overlap.first.first.getTarget();
		// set name of overlap graph
		if ((this instanceof DependencyPair)
				&& !this.checkSwitchDependency) {
				overlapGraph.setName(CriticalPairData.CHANGE_USE_ATTR_D_TXT);
			if (pacName.length() > 0) {
				overlapGraph.setName(CriticalPairData.CHANGE_NEED_ATTR_D_TXT+" (PAC: " + pacName + ")");
				overlapGraph.setHelpInfo("PAC:"+pacName);
			}
			overlap.first.first.setName("MorphOf_"+r1.getName());
			overlap.first.second.setName("MorphOf_"+r2.getName());			
		}
		else {
			overlapGraph.setName(CriticalPairData.CHANGE_USE_ATTR_C_TXT);
			if (pacName.length() > 0) {
				overlapGraph.setName(CriticalPairData.CHANGE_NEED_ATTR_C_TXT+" (PAC: " + pacName + ")");
				overlapGraph.setHelpInfo("PAC:"+pacName);
			}
			overlap.first.first.setName("MorphOf_"+r1.getName());
			overlap.first.second.setName("MorphOf_"+r2.getName());
		}
	}
	
	
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getNacChangeAttrConflicts(
			final Rule r1,
			final Rule r2,
			final Graph g,
			final Vector<Vector<GraphObject>> contextCombis,
			final Vector<Vector<GraphObject>> preservedCombis,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> changedAttrsL1,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> changedAttrsL2) {
		
		// second part: check attr conflicts over NACs 
		final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlaps = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();

		Vector<Vector<GraphObject>> inclusions = null;
		String caNameIndx = this.caIndxStr.contains(":NAC")? this.getCANameIndx(): "";
		
		final List<OrdinaryMorphism> nacs2 = r2.getNACsList();
		for (int j=0; j<nacs2.size() && !this.stop; j++) {
			final OrdinaryMorphism nac = nacs2.get(j);
			if (!nac.isEnabled()
					|| (this.caIndx > 0 && !caNameIndx.isEmpty() 
							&& !nac.getName().equals(caNameIndx))) { 
				continue;
			}
			final Vector<GraphObject> 
			nacRestriction = nacRestrictsAttribute(nac, 
									(VarTuple) r2.getAttrContext().getVariables(), 
									(CondTuple) r2.getAttrContext().getConditions(), 
									changedAttrsL2, changedAttrsL1);
			
			if (nacRestriction.size() == 0)
				continue;
			
			this.nacInsideOverlapGraph = nac;
			markNacGraphObjects(this.nacInsideOverlapGraph, nacRestriction);

			final OrdinaryMorphism extendedL2iso = r2.getLeft().isomorphicCopy();
			if (extendedL2iso == null)
				continue;
			
			final OrdinaryMorphism 
			extendedNAC2iso = extendLeftGraphByNAC(extendedL2iso, nac, false);
			
			final Pair<OrdinaryMorphism, OrdinaryMorphism> 
			extendedL2isoPair = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
												extendedL2iso, extendedNAC2iso);

			int maxSize = extendedL2iso.getTarget().getSize();	
							
			Vector<Vector<GraphObject>> 
			workContextCombis = checkInclusionsAgainstNac(contextCombis, nacRestriction);
			
			if (this.essential) {		
				inclusions = contextCombis;
			}
			else {
				inclusions = ExcludePairHelper.combineInclusions(maxSize, workContextCombis,
												preservedCombis, this.boundB1_L1);												
			}
			
			if (this.caIndx > 0 && inclusions.size() > 0
					&& this.caIndx < inclusions.size()) {
				// reduce inclusions up to this.pfIndx
				int i = inclusions.size()-1;
				while (i >= 0 && i >= this.caIndx) {
					inclusions.remove(i);  
					i = inclusions.size()-1;
				}
				// unset this.caIndx
				this.caIndx = -1;
			}
			
			this.inclCount = inclusions.size();
			System.out.println("to check inclusions: "+inclusions.size());
			
			int i = inclusions.size()-1;
			while (i >= 0 && !this.stop) {
				Vector<GraphObject> inclSet = inclusions.remove(i);
				i = inclusions.size()-1;
				
				OrdinaryMorphism inclMorphism = makeInclusionMorphism(inclSet, g);				
				if (inclMorphism == null) {
					continue;
				}
										
				// get overlapping morphisms
				final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
				overlapsN2 = getOverlappingsVectorChangeAttr(r1, r2, nac, extendedL2isoPair, true, inclMorphism);
				this.inclProgress++;
				
				inclMorphism.dispose(true, false); inclMorphism = null;
			
				for (int in = 0; in < overlapsN2.size(); in++) {
					if (this.maxBoundOfCriticKind == 0 
							|| overlaps.size() < this.maxBoundOfCriticKind) {
						final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
						pi = overlapsN2.get(in);							
						final Graph overlapGraph = pi.first.first.getTarget();
						if (this instanceof DependencyPair
								&& !this.checkSwitchDependency) {
	//						overlapGraph.setName("change-forbid-attr-dependency (NAC: " + nac.getName()+ ")");
							overlapGraph.setName(CriticalPairData.CHANGE_FORBID_ATTR_D_TXT+" (NAC: " + nac.getName()+ ")");
							overlapGraph.setHelpInfo("NAC:"+nac.getName());
						} else {
	//						overlapGraph.setName("change-forbid-attr-conflict (NAC: " + nac.getName()+ ")");
							overlapGraph.setName(CriticalPairData.CHANGE_FORBID_ATTR_C_TXT+" (NAC: " + nac.getName()+ ")");
							overlapGraph.setHelpInfo("NAC:"+nac.getName());
						}	
						overlaps.add(pi);
					}
				}				
				overlapsN2.clear(); 
				
				if (!this.complete && !overlaps.isEmpty()) {
					break;
				}

				// max bound will be checked for each NAC
				if (this.maxBoundOfCriticKind > 0 
						&& overlaps.size() >= this.maxBoundOfCriticKind)  {
					break;
				}
			}
			inclusions = null;
			if (stop) {
				// set this.caIndx 
				this.saveCAIndx(i, nac.getName().concat(":NAC"), this.checkSwitchDependency);
				System.out.println("ChangeAttribute conflict: stop at index: "+i+"   of NAC: "+nac.getName());						
			}
			unmarkNacGraphObjects(this.nacInsideOverlapGraph);	
			
			// restore constant attribute value
			replaceVarAttrValueByConst(nac);
			this.nacInsideOverlapGraph = null;
			
			if (!this.complete && !overlaps.isEmpty()) {
				break;
			}					
		}
		overlaps.trimToSize();
		return overlaps;
	}	
	
	private int getCAIndx() {
		try {
			this.caIndx = Integer.valueOf(this.caIndxStr.split(":")[0]).intValue();
		} catch (NumberFormatException ex) {
			this.caIndx = -1;
		}
		return this.caIndx;
	}
	
	private String getCANameIndx() {
		try {
			return this.caIndxStr.split(":")[1];
		} catch (ArrayIndexOutOfBoundsException ex) {}
		return "";
	}
	
	private void saveCAIndx(int i, String s, boolean switchDep) {
		this.caIndx = i;
		String swDep = switchDep? "1": "0";
		this.caIndxStr = String.valueOf(this.caIndx)
							.concat(":").concat(s)
							.concat(":").concat(swDep);
	}
	
	protected Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getChangeAttributeConflicts(
			final Rule r1,
			final Rule r2,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> changedAttrsL1,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> changedAttrsL2) {
		System.out.println("    ExcludePair.getChangeAttributeConflicts::  [ "
				+ r1.getName() + ", " + r2.getName() + " ] ... ");
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlaps = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
		
		// only for dependency check
		if (this instanceof DependencyPair) {
			if (!this.checkSwitchDependency 
					&& this.caIndxStr.endsWith(":1")) {
				return overlaps;
			}
			else if (this.checkSwitchDependency 
					&& this.caIndxStr.endsWith(":0")) {
				this.caIndxStr = "";
			}
		}
		
		this.inclAsGraph = false;
		this.cpdKind = CriticalPairData.CHANGE_ATTR_CONFLICT;
		this.inclCount = 0;
		this.inclProgress = 0;
		this.caIndx = this.getCAIndx();
		String caNameIndx = this.caIndxStr.contains(":PAC")? this.getCANameIndx(): "";

		final Graph g = r1.getLeft();		
		int maxSize = r2.getLeft().getSize();
		int size = 0;
				
		Enumeration<OrdinaryMorphism> pacs2 = r2.getEnabledPACs();
		boolean lhs_done = false;
		boolean perform = true;
		while (perform && !this.stop) {
			OrdinaryMorphism L2isoL2ExtendedByPACs = null;
			Pair<OrdinaryMorphism, OrdinaryMorphism> lhs_pac_pair = null;
			String pacName = "";
			boolean needToReduce = (this.caIndx > 0 && this.caIndxStr.contains(":LHS"))? true: false;
			
			OrdinaryMorphism pac = null;
			if (this.withPACs && pacs2.hasMoreElements()) {				
				pac = pacs2.nextElement();					
				if (!pac.isEnabled()
						|| (this.caIndx > 0 && !caNameIndx.isEmpty()
								&& !pac.getName().equals(caNameIndx))) {
					continue;
				}		
			}		
			if (pac != null) {
				boolean pacCritical = ExcludePairHelper.isCriticalPAC(pac, this.preservedChanged);				
				if (pacCritical) {						
					L2isoL2ExtendedByPACs = r2.getLeft().isomorphicCopy();
					if (L2isoL2ExtendedByPACs != null) {
						// disjoint union of own objects of PAC and LHS of r2
						lhs_pac_pair = extendLeftGraphByPAC(L2isoL2ExtendedByPACs, pac, false);
						pacName = pac.getName();
						maxSize = L2isoL2ExtendedByPACs.getTarget().getSize();
						lhs_done = true;
					} 
				}				
			} 
			
			size = this.contextC1_L1.size();
			// 3 == edge+src+tar
			if (maxSize > 3 && size > maxSize) size = maxSize;
			
			Vector<Vector<GraphObject>> 
			contextCombis = ExcludePairHelper.getInclusions(g, size, this.contextC1_L1, true);
			checkInclusions(contextCombis, this.preservedChanged);
			
			if (contextCombis.size() == 0) {
				// continue with next PAC
				if (pacs2.hasMoreElements())
					continue;
				else
					break;	
			}			
			
			Vector<Vector<GraphObject>> preservedCombis = null;
			Vector<Vector<GraphObject>> inclusions = null;
			
			if (namedObjectOnly) {
				this.checkInclusionsDuetoNamedObject(contextCombis);
				if (contextCombis.size() == 0) {
					// continue with next PAC
					if (pacs2.hasMoreElements())
						continue;
					else
						break;	
				}
			}
			
			if (this.essential) {
				inclusions = contextCombis;
			}
			else {
				size = this.preservedK1_L1.size();
				if (size > maxSize) size = maxSize;
						
				preservedCombis = ExcludePairHelper.getPlainCombinedInclusions(
						new Vector<GraphObject>(this.preservedK1_L1), size, g);
				
				inclusions = ExcludePairHelper.combineInclusions(maxSize, contextCombis,
												preservedCombis, this.boundB1_L1);
			} 

			if (needToReduce) {
				int i = inclusions.size()-1;
				while (i >= 0 && i >= this.caIndx) {
					inclusions.remove(i);  
					i = inclusions.size()-1;
				}
				// unset this.caIndx
				this.caIndx = -1;
			}
							
			this.inclCount = inclusions.size();
			System.out.println("to check inclusions: "+this.inclCount);
						
			// first part: check attr conflicts of left (+PAC) graphs of the rules
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			overlapsL2 = getLeftChangeAttrConflicts(r1, r2, pacName, g, 
												lhs_pac_pair, inclusions);
			lhs_done = true;
			
			// add to common list of overlappings
			overlaps.addAll(overlapsL2);
			
			if (!this.stop && (overlapsL2.isEmpty() || this.complete)) {
				this.inclCount = 0;
				this.inclProgress = 0;
				// second part: check attr conflicts over NACs 
				Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
				overlapsNAC = getNacChangeAttrConflicts(r1, r2, g,
													 contextCombis, preservedCombis,
													 changedAttrsL1, changedAttrsL2);
				
				// add to common list of overlappings
				overlaps.addAll(overlapsNAC);				
			}
						
			if (!this.complete && !overlaps.isEmpty()) {
				break;
			}
			contextCombis = null;
			preservedCombis = null;
			inclusions = null;
			perform = (this.withPACs && pacs2.hasMoreElements()) || !lhs_done;
		}
		
		if (!stop) {
			this.saveCAIndx(-1, "", false);
		}
		if (this.withPACs) {
			pacs2 = r2.getEnabledPACs();
			while (pacs2.hasMoreElements()) {
				// restore constant attribute value
				this.replaceVarAttrValueByConst(pacs2.nextElement());
			}
		}
		
		//test overlap graphs and reduce isomorphic pairs
		if (/*!r1.getTypeSet().isArcDirected() && */overlaps.size() > 0) {
			reduceCriticalPairs(overlaps);
		}
		
		System.out.println("    ExcludePair.getChangeAttributeConflicts::  [ "
				+ r1.getName() + ", " + r2.getName() + " ]  " + overlaps.size()
				+ " critical overlapping(s)");
		overlaps.trimToSize();	
		this.cpdKind = -1;
		System.gc();
		return overlaps;
	}


	protected void ruleChangesAttributes(
			final Vector<GraphObject> preservedChanged,
			final Rule r1,
			final Rule r2,
			final Vector<GraphObject> context,
			final Vector<GraphObject> boundary,
			final Vector<GraphObject> preserved,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> changedAttrs,
			final Vector<Pair<Type, Pair<Type, Type>>> neededTypes) {
		
//		context.clear();
//		boundary.clear();
//		preservedChanged.clear(); 
		
		for (Enumeration<GraphObject> en = r1.getDomain(); en.hasMoreElements();) {
			GraphObject goLeft = en.nextElement();
			if (isInTypes(neededTypes, goLeft)) {
				if (!preserved.contains(goLeft))
					preserved.add(goLeft);
				// now check if attrs of r1 have been changed
				GraphObject goRight = r1.getImage(goLeft);
				AttrInstance leftAttr = goLeft.getAttribute();
				AttrInstance rightAttr = goRight.getAttribute();
				if (leftAttr == null)
					continue;

				Vector<Pair<ValueMember, ValueMember>> members = changedAttrs
						.get(goLeft.getType().getAttrType());
				if (members == null)
					members = new Vector<Pair<ValueMember, ValueMember>>(2);
				boolean changed = false;
				for (int i = 0; i < leftAttr.getNumberOfEntries(); i++) {
					ValueMember 
					leftMember = (ValueMember) leftAttr.getMemberAt(i);
					ValueMember
					rightMember = (ValueMember) rightAttr.getMemberAt(i);
					boolean toadd = false;
					if (rightMember.isSet()) {
						if (!leftMember.isSet())
							toadd = true;
						else if (rightMember.getExpr().isVariable()) {
							if ((leftMember.getExpr().isVariable() 
									&& !leftMember.getExprAsText().equals(
											rightMember.getExprAsText()))
									|| leftMember.getExpr().isConstant())
								toadd = true;
						} else if (rightMember.getExpr().isConstant()) {
							if ((leftMember.getExpr().isConstant() 
									&& !leftMember.getExprAsText().equals(
											rightMember.getExprAsText()))
									|| leftMember.getExpr().isVariable()) {
								toadd = true;
							}
						} else if (rightMember.getExpr().isComplex()) {
							toadd = true;
						}
						if (toadd) {
							changed = true;
							Pair<ValueMember, ValueMember> p = new Pair<ValueMember, ValueMember>(
									leftMember, rightMember);
							members.add(p);
						}
					}
				}

				if (changed) {
					if (!members.isEmpty())
						changedAttrs.put(goLeft.getType().getAttrType(), members);
					if (!context.contains(goLeft))
						context.add(goLeft);
					if (!preservedChanged.contains(goLeft))
						preservedChanged.add(goLeft);
					preserved.remove(goLeft);
					if (goLeft.isArc()) {
						GraphObject src = ((Arc) goLeft).getSource();
						GraphObject tar = ((Arc) goLeft).getTarget();
						if (!context.contains(src))
							context.add(src);
						if (!context.contains(tar))
							context.add(tar);
						/*
						 * if(!boundary.contains(src)) boundary.add(src);
						 * if(!boundary.contains(tar)) boundary.add(tar);
						 */
						preserved.remove(src);
						preserved.remove(tar);
					}
				}
			}
		}
		restrictedAttributes(preservedChanged, r1, r2, context, boundary, preserved, neededTypes);
		
		// at the end: 
		// preservedChanged (is this.preservedChanged) contains nodes, edges to change;
		// context (is this.contextC1_l1) contains nodes to change and 
		// edges to change with source and target as boundary objects;
		// preserved (is this.preservedK1_l1) contains unchanged nodes and edges
	}

	private void restrictedAttributes(
			final Vector<GraphObject> preservedChanged,
			final Rule r1,
			final Rule r2,
			final Vector<GraphObject> context,
			final Vector<GraphObject> boundary,
			final Vector<GraphObject> preserved,
			final Vector<Pair<Type, Pair<Type, Type>>> neededTypes) {
		
		for (Enumeration<GraphObject> en = r1.getDomain(); en.hasMoreElements();) {
			GraphObject goLeft = en.nextElement();			
			if (isInTypes(neededTypes, goLeft)) {
				if (goLeft.getAttribute() == null)
					continue;

				GraphObject goRight = r1.getImage(goLeft);
				if (goRight != null && goRight.doesChangeAttr(goLeft)) {			
					// if node, check against nodes of the LHS of r2
					Iterator<?> elems = r2.getLeft().getNodesSet().iterator();
					while (elems.hasNext()) {
						GraphObject o = (GraphObject) elems.next();
						if (goLeft.getType().isParentOf(o.getType())
								|| o.getType().isParentOf(goLeft.getType())) {
							if (ExcludePairHelper.isAttributeRestricted(r2, o, null)) {								
								if (!typeContainedIn(o.getType(), preservedChanged)) 
									addChangeObjToContext(goLeft, context, preservedChanged, preserved);
							}
						}
					}
					// if edge, check against edges of the LHS of r2
					elems = r2.getLeft().getArcsSet().iterator();
					while (elems.hasNext()) {
						GraphObject o = (GraphObject) elems.next();
						if (o.getType().compareTo(goLeft.getType())) {
							if (ExcludePairHelper.isAttributeRestricted(r2, o, null)) {
								if (!typeContainedIn(o.getType(), preservedChanged)) 
									addChangeObjToContext(goLeft, context, preservedChanged, preserved);
							}
						}
					}
					
					// additionally, check against nodes/edges of the nacs of r2
					final List<OrdinaryMorphism> nacs = r2.getNACsList();
					for (int l=0; l<nacs.size(); l++) {
						final OrdinaryMorphism nac = nacs.get(l);						
						if (!nac.isEnabled())
							continue;
						elems = nac.getTarget().getNodesSet().iterator();
						while (elems.hasNext()) {
							GraphObject o = (GraphObject) elems.next();
							if (goLeft.getType().isParentOf(o.getType())
										|| o.getType().isParentOf(goLeft.getType())) {
								GraphObject o_lhs = null;
								if (nac.getInverseImage(o).hasMoreElements()) 
									o_lhs =  nac.getInverseImage(o).nextElement();
								if (ExcludePairHelper.isAttributeRestricted(r2, o, o_lhs)) {
									if (!typeContainedIn(o.getType(), preservedChanged)) 
										addChangeObjToContext(goLeft, context, preservedChanged, preserved);
								}
							}
						}
						elems = nac.getTarget().getArcsSet().iterator();
						while (elems.hasNext()) {
							GraphObject o = (GraphObject) elems.next();
							if (o.getType().compareTo(goLeft.getType())) {
								GraphObject o_lhs = null;
								if (nac.getInverseImage(o).hasMoreElements()) 
									o_lhs =  nac.getInverseImage(o).nextElement();
								if (ExcludePairHelper.isAttributeRestricted(r2, o, o_lhs)) {
									if (!typeContainedIn(o.getType(), preservedChanged)) 
										addChangeObjToContext(goLeft, context, preservedChanged, preserved);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void addChangeObjToContext(
					GraphObject goLeft, 
					final Vector<GraphObject> context,
					final Vector<GraphObject> preservedChanged,
					final Vector<GraphObject> preserved) {
		context.add(goLeft);
		preservedChanged.add(goLeft);
		preserved.remove(goLeft);
		if (goLeft.isArc()) {
			GraphObject src = ((Arc) goLeft).getSource();
			GraphObject tar = ((Arc) goLeft).getTarget();
			if (!context.contains(src))
				context.add(src);
			if (!context.contains(tar))
				context.add(tar);
			preserved.remove(src);
			preserved.remove(tar);
		}
	}
	
	private boolean typeContainedIn(Type t, final Vector<GraphObject> vec) {
		boolean found = false;
		for (GraphObject go: vec) {
			if (t.isParentOf(go.getType())
					|| go.getType().isParentOf(t)) {
				found = true;
				break;
			}
		}
		return found;
	}
	
	void ruleChangesAttributes(
			final Rule r,
			final Vector<GraphObject> preserved,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> changedAttrs) {
		
		for (Enumeration<GraphObject> en = r.getDomain(); en.hasMoreElements();) {
			GraphObject goLeft = en.nextElement();
			if (goLeft.isNode()) {
				if (!preserved.contains(goLeft))
					preserved.add(goLeft);
				// now check if attrs have been changed
				GraphObject goRight = r.getImage(goLeft);
				AttrInstance leftAttr = goLeft.getAttribute();
				AttrInstance rightAttr = goRight.getAttribute();
				if (leftAttr != null) {
					Vector<Pair<ValueMember, ValueMember>> members = changedAttrs
							.get(goLeft.getType().getAttrType());
					if (members == null)
						members = new Vector<Pair<ValueMember, ValueMember>>(2);
					boolean changed = false;
					for (int i = 0; i < leftAttr.getNumberOfEntries(); i++) {
						ValueMember leftMember = (ValueMember) leftAttr
								.getMemberAt(i);
						ValueMember rightMember = (ValueMember) rightAttr
								.getMemberAt(i);
						boolean bool = false;
						if (rightMember.isSet()) {
							if (!leftMember.isSet()) {
								bool = true;
							} else if (rightMember.getExpr().isVariable()) {
								if ((leftMember.getExpr().isVariable() && !leftMember
										.getExprAsText().equals(
												rightMember.getExprAsText()))
										|| leftMember.getExpr().isConstant())
									bool = true;
							} else if (rightMember.getExpr().isConstant()) {
								if ((leftMember.getExpr().isConstant() && !leftMember
										.getExprAsText().equals(
												rightMember.getExprAsText()))
										|| leftMember.getExpr().isVariable()) {
									bool = true;
								}
							} else if (rightMember.getExpr().isComplex()) {
								bool = true;
							}
							if (bool) {
								changed = true;
								Pair<ValueMember, ValueMember> p = new Pair<ValueMember, ValueMember>(
										leftMember, rightMember);
								members.add(p);
							}
						}
					}
					if (changed) {
						changedAttrs.put(goLeft.getType().getAttrType(),
								members);
					}
				}
			}
		}
	}

	boolean ruleRestrictsAttributes(
			boolean strongCheck,
			final Rule r,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> leftChangedAttrs,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> otherChangedAttrs) {

		final VarTuple vars = (VarTuple) r.getAttrContext().getVariables();	
		final CondTuple conds = (CondTuple) r.getAttrContext().getConditions();
		
		ruleRestrictsAtts(strongCheck, r, leftChangedAttrs, otherChangedAttrs,
				 vars, conds, r.getLeft().getNodesSet().iterator());
		ruleRestrictsAtts(strongCheck, r, leftChangedAttrs, otherChangedAttrs,
				 vars, conds, r.getLeft().getArcsSet().iterator());
		
		// check attr. setting (LHS) of r against otherChangedAttrs (RHS)
		// for this part the strong-option is already checked
		Enumeration<AttrType> keys = leftChangedAttrs.keys();
		while (keys.hasMoreElements()) {
			agg.attribute.AttrType key = keys.nextElement();
			Vector<Pair<ValueMember, ValueMember>> v = leftChangedAttrs.get(key);
			Vector<Pair<ValueMember, ValueMember>> otherv = otherChangedAttrs.get(key);
			if (otherv == null && r.getTypeSet().hasInheritance()) {
				otherv = checkAttrFamily(r, key, v, otherChangedAttrs);
			}
			if (v != null && otherv != null) {
				for (int i = 0; i < v.size(); i++) {
					Pair<ValueMember, ValueMember> p = v.get(i);
					ValueMember mL = p.first;
					if (mL != null && mL.getExpr() != null) {
						for (int j = 0; j < otherv.size(); j++) {
							Pair<ValueMember, ValueMember> 
							otherp = otherv.get(j);
							ValueMember othermR = otherp.second;
							if (othermR != null && othermR.getExpr() != null
									&&!othermR.getExprAsText().equals(mL.getExprAsText())) {							
								return true;							
							} else {
								othermR = otherp.first;
								if (othermR != null && othermR.getExpr() != null
										&& !othermR.getExprAsText().equals(mL.getExprAsText())) {
									return true;
								}
							}
						}
					}
				}
			}
		}
	
		final List<OrdinaryMorphism> nacs = r.getNACsList();
		for (int l=0; l<nacs.size(); l++) {
			final OrdinaryMorphism nac = nacs.get(l);		
			if (nac.isEnabled() 
					&& nacRestrictsAttribute(nac, vars, conds, leftChangedAttrs,
									otherChangedAttrs).size() != 0) {
				return true;
			}
		}
		
		final List<OrdinaryMorphism> pacs = r.getPACsList();
		for (int l=0; l<pacs.size(); l++) {
			final OrdinaryMorphism pac = pacs.get(l);		
			if (pac.isEnabled() 
					&& pacRestrictsAttribute(pac, vars, conds, leftChangedAttrs,
									otherChangedAttrs).size() != 0) {
				return true;
			}
		}
		
		return false;
	}
	
	private Vector<Pair<ValueMember, ValueMember>> checkAttrFamily(
			GraphObject go,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> otherChangedAttrs) {
		Vector<Pair<ValueMember, ValueMember>> 
		otherv = checkAttributeParents(go.getAttribute().getTupleType(), otherChangedAttrs);
		if (otherv == null)
			otherv = checkAttributeChilds(go, otherChangedAttrs);
		
		return otherv;
	}
	
	private Vector<Pair<ValueMember, ValueMember>> checkAttrFamily(
			Rule r,
			AttrType attrType, 
			Vector<Pair<ValueMember, ValueMember>> r_changedAttrs,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> otherChangedAttrs) {
		
		Vector<Pair<ValueMember, ValueMember>> otherv = null;
		for (int i=0; i<r_changedAttrs.size(); i++) {
			Pair<ValueMember, ValueMember> p = r_changedAttrs.get(i);
			GraphObject go = r.getSource().getNodeWithAttrMember(attrType, (ValueMember)p.first); 
//			GraphObject img = r.getTarget().getNodeWithAttrMember(attrType, (ValueMember)p.second); 
			if (go != null) {
				otherv = checkAttributeParents(go.getAttribute().getTupleType(), otherChangedAttrs);
				if (otherv == null)
					otherv = checkAttributeChilds(go, otherChangedAttrs);
				if (otherv != null)
					break;
			}
		}
		return otherv;
	}
	
	private Vector<Pair<ValueMember, ValueMember>> checkAttributeChilds(
			GraphObject go,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> otherChangedAttrs) {
		
		Vector<Pair<ValueMember, ValueMember>> otherv = null;
		Vector<Type> goTypeChilds = go.getType().getAllChildren();
		for (Type t: goTypeChilds) {
			DeclTuple attrType = (DeclTuple)t.getAttrType();	
			otherv = otherChangedAttrs.get(attrType);
			if (otherv != null)
				break;
		}
		return otherv;
	}
	
	private Vector<Pair<ValueMember, ValueMember>> checkAttributeParents(
			AttrType attrType,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> otherChangedAttrs) {
		
		Vector<Pair<ValueMember, ValueMember>> otherv = null;
		Enumeration<DeclTuple> parents = ((agg.attribute.impl.DeclTuple)attrType).getAllParents();
		while (parents.hasMoreElements()) {
			DeclTuple pardt = parents.nextElement();
			otherv = otherChangedAttrs.get(pardt);
			if (otherv != null)
				break;
		}
		return otherv;
	}
	
	private void ruleRestrictsAtts(
			boolean strongCheck,
			final Rule r,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> leftChangedAttrs,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> otherChangedAttrs,
			final VarTuple vars,
			final CondTuple conds,
			final Iterator<?> en) {
		
		while (en.hasNext()) {
			GraphObject goLeft = (GraphObject) en.next();
			AttrInstance leftAttr = goLeft.getAttribute();
			boolean bool = false;
			if (leftAttr != null) {
				Vector<Pair<ValueMember, ValueMember>> members = leftChangedAttrs
						.get(goLeft.getType().getAttrType());
				if (members == null) 
					members = new Vector<Pair<ValueMember, ValueMember>>(2);
								
				for (int i = 0; i < leftAttr.getNumberOfEntries(); i++) {
					ValueMember leftMember = (ValueMember) leftAttr.getMemberAt(i);
					if (leftMember.isSet()) {
						if (leftMember.getExpr().isConstant()) {
							Vector<Pair<ValueMember, ValueMember>> 
							otherv = otherChangedAttrs.get(leftAttr.getType());
							if (otherv == null && goLeft.isNode()) 
								otherv = checkAttrFamily(goLeft, otherChangedAttrs);
							
							if (otherv != null) {
								for (int j = 0; j < otherv.size(); j++) {
									Pair<ValueMember, ValueMember> 
									otherp = otherv.get(j);
									ValueMember othermR = otherp.second;
									if (othermR != null
											&& othermR.getName().equals(leftMember.getName())	
											&& othermR.getExpr() != null
											&& !othermR.getExprAsText().equals(
													leftMember.getExprAsText())) {
										bool = true;
									}
								}
							}
						}
						else if (leftMember.getExpr().isVariable()) {
							if (strongCheck) {	
								final VarMember var = (VarMember)vars.getMemberAt(leftMember.getExprAsText());
								bool = (var != null) 
											&& (var.isInputParameter()
													|| isVariableUsedInAttrCondition(var.getName(), conds));						

							} else {
								bool = true;
							}
						}
						if (bool) {
							Pair<ValueMember, ValueMember> 
							p = new Pair<ValueMember, ValueMember>(leftMember, null);
							members.add(p);
						}
					}
					/*
					else if (r.getImage(goLeft) == null) { // goLeft delete ???
						Pair<ValueMember, ValueMember> 
						p = new Pair<ValueMember, ValueMember>(leftMember, null);
						members.add(p);
					}
					 */
				}
				if (!members.isEmpty()) {
					leftChangedAttrs.put(goLeft.getType().getAttrType(),
							members);
				}
			}
		}
	}
	
	private boolean isVariableUsedInAttrCondition(final String varName, final CondTuple conds) {
		final Vector<String> varsOfConditions = conds.getAllVariables();
		for (int j=0; j<varsOfConditions.size(); j++) {
			if (varName.equals(varsOfConditions.get(j))) {
				return true;
			}
		}
		return false;
	}
	
	private Vector<GraphObject> nacRestrictsAttribute(
			final OrdinaryMorphism nac,
			final VarTuple vars,
			final CondTuple conds,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> leftChangedAttrs,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> otherChangedAttrs) {
		
		final Vector<GraphObject> result = new Vector<GraphObject>(5);
		final Vector<String> varNames = conds.getAllVariables();
		
		nacRestrictsAttr(nac, vars, conds, leftChangedAttrs, otherChangedAttrs,
				 result, varNames, nac.getTarget().getNodesSet().iterator());
		nacRestrictsAttr(nac, vars, conds, leftChangedAttrs, otherChangedAttrs,
				 result, varNames, nac.getTarget().getArcsSet().iterator());

		return result;
	}

	private Vector<GraphObject> nacRestrictsAttr(
			final OrdinaryMorphism nac,
			final VarTuple vars,
			final CondTuple conds,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> leftChangedAttrs,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> otherChangedAttrs,
			final Vector<GraphObject> res,
			final Vector<String> varNames,
			final Iterator<?> en) {
				
		while (en.hasNext()) {
			GraphObject go = (GraphObject) en.next();
			if (go.getAttribute() != null) {
				AttrInstance attr = go.getAttribute();
				
				Vector<Pair<ValueMember, ValueMember>> changedMembers = new Vector<Pair<ValueMember, ValueMember>>();
				if (!leftChangedAttrs.isEmpty()) {
					Vector<Pair<ValueMember, ValueMember>> 
					changed = leftChangedAttrs.get(go.getType().getAttrType());
					if (changed != null)
						changedMembers.addAll(changed);						
				}
				if (!otherChangedAttrs.isEmpty()) {
					Vector<Pair<ValueMember, ValueMember>> 
					changed = otherChangedAttrs.get(go.getType().getAttrType());
					if (changed == null)
						changed = ExcludePairHelper.getAttrMemberByParentType(
													otherChangedAttrs, go.getType().getAttrType());
					if (changed == null)
						changed = ExcludePairHelper.getAttrMemberByChildType(
													otherChangedAttrs, go.getType().getAttrType());
					if (changed != null)
						changedMembers.addAll(changed);
				}
				
				Enumeration<GraphObject> en_lhs = nac.getInverseImage(go);
				GraphObject go_l = (en_lhs.hasMoreElements())? 
										en_lhs.nextElement() :null;
				for (int i = 0; i < attr.getNumberOfEntries(); i++) {
					ValueMember member = (ValueMember) attr.getMemberAt(i);
					if (member.isSet()) {
						ValueMember mem_l = (go_l != null)? 
								(ValueMember) go_l.getAttribute().getMemberAt(member.getName()) :null;					
						if (mem_l != null && mem_l.isSet()
								&& member.getExprAsText().equals(mem_l.getExprAsText())) {
							 ; // the variable or constant is already in LHS
						}
						else {
							for (int j = 0; j < changedMembers.size(); j++) {
								Pair<ValueMember, ValueMember> p = changedMembers.get(j);
								ValueMember vm1 = p.first;
								ValueMember vm2 = p.second;
								if (member.getName().equals(vm1.getName()) 
										|| ((vm2 != null) && member.getName()
												.equals(vm2.getName()))) {
									if (this.strongAttrCheck && member.getExpr().isVariable()) {	
										final VarMember var = (VarMember)vars.getMemberAt(member.getExprAsText());
										if (var != null && (var.isInputParameter()
															|| isVariableUsedInAttrCondition(var.getName(), conds))){
											res.add(go);
											break;
										}
									} else {									
										res.add(go);
										break;
									}
								}								
							}
						}
					}
				}
			}
		}
		return res;
	}
	
	private Vector<GraphObject> pacRestrictsAttribute(
			final OrdinaryMorphism pac,
			final VarTuple vars,
			final CondTuple conds,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> leftChangedAttrs,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> otherChangedAttrs) {

		Vector<GraphObject> result = new Vector<GraphObject>(5);
		final Vector<String> varsNames = conds.getAllVariables();
		
		pacRestrictsAttr(pac, vars, conds, leftChangedAttrs, otherChangedAttrs,
				 result, varsNames, pac.getTarget().getNodesSet().iterator());
		pacRestrictsAttr(pac, vars, conds, leftChangedAttrs, otherChangedAttrs,
				 result, varsNames, pac.getTarget().getArcsSet().iterator());
		
		return result;
	}

	private Vector<GraphObject> pacRestrictsAttr(
			final OrdinaryMorphism pac,
			final VarTuple vars,
			final CondTuple conds,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> leftChangedAttrs,
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> otherChangedAttrs,
			final Vector<GraphObject> res,
			final Vector<String> varsNames,
			final Iterator<?> en) {
		
		while (en.hasNext()) {
			GraphObject go = (GraphObject) en.next();
			if (go.getAttribute() != null) {
				AttrInstance attr = go.getAttribute();
				
				Vector<Pair<ValueMember, ValueMember>> changedMembers = new Vector<Pair<ValueMember, ValueMember>>();
				if (!leftChangedAttrs.isEmpty()) {
					Vector<Pair<ValueMember, ValueMember>> 
					changed = leftChangedAttrs.get(go.getType().getAttrType());
					if (changed != null)
						changedMembers.addAll(changed);						
				}
				if (!otherChangedAttrs.isEmpty()) {
					Vector<Pair<ValueMember, ValueMember>> 
					changed = otherChangedAttrs.get(go.getType().getAttrType());
					if (changed == null)
						changed = ExcludePairHelper.getAttrMemberByParentType(
													otherChangedAttrs, go.getType().getAttrType());
					if (changed == null)
						changed = ExcludePairHelper.getAttrMemberByChildType(
													otherChangedAttrs, go.getType().getAttrType());
					if (changed != null)
						changedMembers.addAll(changed);
				}
				
				Enumeration<GraphObject> en_lhs = pac.getInverseImage(go);
				GraphObject go_l = (en_lhs.hasMoreElements())? 
										en_lhs.nextElement() :null;
				for (int i = 0; i < attr.getNumberOfEntries(); i++) {
					ValueMember member = (ValueMember) attr.getMemberAt(i);
					if (member.isSet()) {
						ValueMember mem_l = (go_l != null)? 
								(ValueMember) go.getAttribute().getMemberAt(member.getName()) :null;					
						if (mem_l != null && mem_l.isSet()
								&& member.getExprAsText().equals(mem_l.getExprAsText())) {
							 ; // the variable or constant is already in LHS
						}
						else {
							for (int j = 0; j < changedMembers.size(); j++) {
								Pair<ValueMember, ValueMember> p = changedMembers
											.get(j);
								ValueMember vm1 = p.first;
								ValueMember vm2 = p.second;
								if (member.getName().equals(vm1.getName())
										|| ((vm2 != null) && member.getName()
													.equals(vm2.getName()))) {
									if (this.strongAttrCheck && member.getExpr().isVariable()) {	
										final VarMember var = (VarMember)vars.getMemberAt(member.getExprAsText());
										if (var != null && (var.isInputParameter()
															|| isVariableUsedInAttrCondition(var.getName(), conds))){
											res.add(go);
											break;
										}
									} else {									
										res.add(go);
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		return res;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getOverlappingsVectorChangeAttr(
			final Rule r1,
			final Rule r2, 
			final OrdinaryMorphism ac, 
			Object testObject,
			boolean isNAC,
			final OrdinaryMorphism inclusionMorphism) {
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
		result = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>(1);
		
		if ((testObject instanceof Graph) && (r2.getLeft() == (Graph) testObject)) {
			result = getOverlappingsVectorAttr(r1, r2, inclusionMorphism);
		} 
		else if (isNAC && testObject instanceof Pair) {
			result = getOverlappingsVectorAttr(r1, r2, ac, (Pair) testObject, inclusionMorphism);			
		}
		else if (testObject instanceof Pair) {
			result = getOverlappingsVectorAttr(r1, r2, (Pair) testObject, inclusionMorphism);
		}
		System.gc();
		return result;
	}
	
	
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getOverlappingsVectorAttr(
			final Rule r1,
			final Rule r2, 
			final OrdinaryMorphism nac, 
			Pair<?,?> extendedL2isoPair,
			final OrdinaryMorphism inclusionMorphism) {
				
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		result = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();

		if ((((OrdinaryMorphism) extendedL2isoPair.first).getTarget().getNodesCount() < inclusionMorphism.getSource().getNodesCount())
				|| (((OrdinaryMorphism) extendedL2isoPair.first).getTarget().getArcsCount() < inclusionMorphism.getSource().getArcsCount())) 
			return result;
		
		OrdinaryMorphism extendedL2iso = (OrdinaryMorphism) extendedL2isoPair.first;
		OrdinaryMorphism extendedNAC2iso = (OrdinaryMorphism) extendedL2isoPair.second;
				
		Pair<Rule, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		rulePair = (BaseFactory.theFactory()).constructIsomorphicRule(inclusionMorphism, true, false);
		if (rulePair == null)
			return result;
		
		// rulePair.first is a rule which helps to create an overlapping graph				
		
		// mark Conflict Objects
		for (Iterator<Node> en = inclusionMorphism.getSource().getNodesSet().iterator(); en
					.hasNext();) {
			GraphObject o = en.next();
			// get object from LHS of r1
			GraphObject i_o = inclusionMorphism.getImage(o);
			// set critical if i_o should be changed
			if (this.contextC1_L1.contains(i_o)) {
				GraphObject obj = rulePair.second.first.getImage(o);
				obj.setCritical(true);
			}
		}
		for (Iterator<Arc> en = inclusionMorphism.getSource().getArcsSet().iterator(); 
		en.hasNext();) {
			GraphObject o = en.next();
			// get object from LHS of r1
			GraphObject i_o = inclusionMorphism.getImage(o);
			// set critical if i_o should be changed
			if (this.contextC1_L1.contains(i_o)) {
				GraphObject obj = rulePair.second.first.getImage(o);
				obj.setCritical(true);
			}
		}

		Graph ograph = extendedL2iso.getTarget(); // overlap graph
		MorphCompletionStrategy localStrategy = getLocalMorphismCompletionStrategy();			

		Match testm = (BaseFactory.theFactory()).createMatch(rulePair.first, ograph, true, "1");
		testm.getTarget().setCompleteGraph(false);		
		((ContextView)testm.getAttrContext()).setIgnoreOfConstContext(true);		
		testm.setCompletionStrategy(localStrategy, true);
		testm.getCompletionStrategy().initialize(testm);

		if (namedObjectOnly)
			this.addNamedObjectConstraint(testm);
		
		// find objs which changed by r1 and forbiden by nac of r2
		List<GraphObject> changedObjs = getChangedObjs(r1); 
		List<GraphObject> newNACobj = ExcludePairHelper.getObjsWithAttrValue(nac, true, true);
		List<GraphObject> criticDuetoNAC = ExcludePairHelper.getImgOfObj(extendedNAC2iso, newNACobj);
		List<Triple<GraphObject,ValueMember,String>> 
		withConstOrVarDuetoNAC = ExcludePairHelper.getObjsWithConstOrVarDuetoImg(
										testm.getAttrContext(), criticDuetoNAC, changedObjs, r1, nac, extendedNAC2iso, true, true);		
		OrdinaryMorphism rStar = null;
		Match m = null;	
		boolean nextMatch = true;
				
		while (nextMatch) {
			nextMatch = testm.nextCompletion();			
			nextMatch = nextMatch && testm.isMappingChanged();
			nextMatch = nextMatch && testm.isTypeMaxMultiplicitySatisfied();
			
			if (nextMatch) {	
//				BaseFactory.theFactory().replaceTransientTarVarBySrcVar(testm);				
				rStar = ograph.isomorphicCopy();
				if (rStar == null)
					break;
				
				m = BaseFactory.theFactory().createMatch(rulePair.first, rStar.getTarget());				
				((ContextView)m.getAttrContext()).setIgnoreOfConstContext(((ContextView)testm.getAttrContext()).isIgnoreConstContext());
				if (doCompose(localStrategy, m, testm, rStar)) {	
					if (!(localStrategy instanceof Completion_InheritCSP)
							|| BaseFactory.theFactory().replaceParentByChild(rulePair.first, m, rStar)) {
						m.adaptAttrContextValues(testm.getAttrContext());
						// check whether at least one of critical objects matchs into N2-L2
						boolean criticalOK = false;
						Iterator<Node> en = m.getSource().getNodesSet().iterator();
						while (en.hasNext()) {
							GraphObject o = en.next();
							if (o.isCritical()) {
								GraphObject i_o = m.getImage(o);
								Enumeration<GraphObject> en1 = rStar.getInverseImage(i_o);
								if (en1.hasMoreElements()) {
									GraphObject obj = en1.nextElement();
									if (!extendedL2iso.getInverseImage(obj).hasMoreElements()) 
										criticalOK = true;
									else if (extendedNAC2isoContainsImageObjWithAttrConstant(extendedNAC2iso, obj))
										criticalOK = true;																			
								}
							}
						}
						Iterator<Arc> ea = m.getSource().getArcsSet().iterator();						
						while (ea.hasNext()) {
							GraphObject o = ea.next();
							if (o.isCritical()) {
								GraphObject i_o = m.getImage(o);
								Enumeration<GraphObject> en1 = rStar.getInverseImage(i_o);
								if (en1.hasMoreElements()) {
									GraphObject obj = en1.nextElement();
									if (!extendedL2iso.getInverseImage(obj).hasMoreElements())
										criticalOK = true;										
									else if (extendedNAC2isoContainsImageObjWithAttrConstant(extendedNAC2iso, obj))
										criticalOK = true;																			
								}
							}
						}

						if (criticalOK) {
							try {
								// Variables in work graph are allowed
								OrdinaryMorphism
								ms = (OrdinaryMorphism) TestStep.execute(m, true,this.equalVariableNameOfAttrMapping);
								if ((this.grammar == null)
											|| (!this.grammar.getConstraints().hasMoreElements()))
									this.consistentOnly = false; 
								// the overlapping graph
								boolean consistentOverlap = true;
								if (ms != null && this.consistentOnly) {							
									if (!checkGraphConsistency(ms.getTarget(), r1.getLayer()))
										consistentOverlap = false;									
								}
								if (ms != null && consistentOverlap) {
									OrdinaryMorphism mStar = rulePair.second.second.compose(ms); 									
									ms.dispose(); ms = null; // ms - not more needed
									
									if (mStar != null) {
										
										boolean nac_critical = ExcludePairHelper.markObjDuetoNAC(withConstOrVarDuetoNAC, extendedNAC2iso, rStar);
										if (nac_critical)
											nac.setTextualComment(String.valueOf(CriticalPairData.CHANGE_FORBID_ATTR_CONFLICT));
										Pair<OrdinaryMorphism, OrdinaryMorphism> 
										p = getValidMatchChangeAttr(r1, r2, 
													nac, extendedL2iso, extendedNAC2iso, mStar, rStar);
										nac.setTextualComment("");
										if (p != null) {
											if (namedObjectOnly && !namedObjsConform(p))												
												p = null;
										}
										// p contains valid match1 and match2,
										// now check attributes against r2  
										if (p != null && (attributeCritical(r1, r2, p, null) 
															|| nac_critical)) {
											// now result pair contains 2 pairs:
											// in this case: first is p1;
											// second is a help pair p2 with
											// morphisms:
											// first = r2.left -> N2;
											// second = nac.target -> N2
											Pair<OrdinaryMorphism, OrdinaryMorphism> 
											p1 = new Pair<OrdinaryMorphism, OrdinaryMorphism>(mStar, rStar);
																				
											Pair<OrdinaryMorphism, OrdinaryMorphism> 
											p2 = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
														extendedL2iso, extendedNAC2iso);
											Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
											resultp = new Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>(
														p1, p2);
											
											if (this.maxBoundOfCriticKind == 0 
													|| result.size() < this.maxBoundOfCriticKind)  {
												result.addElement(resultp);
												if (result.size() == this.maxBoundOfCriticKind)
													nextMatch = false;	
											}
											else
												nextMatch = false;	
											if (!this.complete) 
												nextMatch = false;	
										} else {
											mStar.dispose(); mStar = null;
											rStar.dispose(); rStar = null;
										}
									}
								} else {
									rStar.dispose(); rStar = null;
								}
							} catch (TypeException e) {}
						} else {
							rStar.dispose(); rStar = null;
						}
					} else {
						rStar.dispose(); rStar = null;
					}
				} else {
					rStar.dispose(); rStar = null;
				}
				m.dispose(); m = null;
			} 
		} // while
		// dispose helpers
		testm.dispose(); testm = null;
		localStrategy = null;
		rulePair.second.first.dispose();
		rulePair.second.second.dispose();
		rulePair.first.disposeSuper();
		rulePair.second.first = null;
		rulePair.second.second = null;
		rulePair.first = null;
		rulePair = null;

		changedObjs = null; 
		newNACobj = null;
		criticDuetoNAC = null;
		withConstOrVarDuetoNAC = null;
		return result;
	}
	
	private boolean extendedNAC2isoContainsImageObjWithAttrConstant(
			final OrdinaryMorphism extendedNACiso, 
			final GraphObject obj){
		Enumeration<GraphObject> dom = extendedNACiso.getDomain();
		while (dom.hasMoreElements()) {
			GraphObject x = dom.nextElement();
			if (extendedNACiso.getImage(x) == obj 
					&& x.getAttribute() != null) {
				ValueTuple vt = (ValueTuple) x.getAttribute();
				for (int i=0; i<vt.getNumberOfEntries(); i++) {
					ValueMember vm = vt.getValueMemberAt(i);
					if (this.attrMember2Expr.get(vm) != null) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/*
	private void replaceExprAttrValueByVariable(final AttrInstance attrInstance) {
		if (attrInstance == null)
			return;
		
		final ValueTuple valTuple = (ValueTuple) attrInstance;		
		for (int i=0; i<valTuple.getNumberOfEntries(); i++) {
			final ValueMember mem = valTuple.getValueMemberAt(i);
			if (mem.isSet() && mem.getExpr().isComplex()) {
				final String var = "val_".concat(mem.getExprAsText());
				this.morphAttrMember2Expr.put(mem, new Pair<String,String>(var, mem.getExprAsText()));
				System.out.println("###  "+mem.getExprAsText()+"  replaced by: "+var);
				mem.setExpr(null);
				mem.setExprAsText(var);
				mem.setTransient(true);
			}
		}
	}
	*/
	
	private void replaceConstAttrValByVar(final AttrInstance attrInstance) {
		if (attrInstance == null)
			return;
		
		final ValueTuple valTuple = (ValueTuple) attrInstance;		
		for (int i=0; i<valTuple.getNumberOfEntries(); i++) {
			final ValueMember mem = valTuple.getValueMemberAt(i);
			if (mem.isSet() && mem.getExpr().isConstant()) {
				final String var = "val_".concat(mem.getExprAsText());
				this.attrMember2Expr.put(mem, new Pair<String,String>(var, mem.getExprAsText()));
				mem.setExprAsText(var);
				mem.setTransient(true);
			}
		}
	}
	
	private void storeAttrValByName(final AttrInstance attrInstance, final AttrInstance ref_attrInstance) {
		if (attrInstance == null)
			return;
		
		final ValueTuple valTuple = (ValueTuple) attrInstance;		
		for (int i=0; i<valTuple.getNumberOfEntries(); i++) {
			final ValueMember mem = valTuple.getValueMemberAt(i);
			ValueMember ref_mem = null;
			if (ref_attrInstance != null)
				ref_mem = ((ValueTuple) ref_attrInstance).getValueMemberAt(mem.getName());
			if (mem.isSet() && !mem.getExpr().isComplex()) {
				if (ref_mem == null || !ref_mem.isSet() 
						|| !ref_mem.getExprAsText().equals(mem.getExprAsText())) {
					this.attrMember2Expr.put(mem, new Pair<String,String>(mem.getName(), mem.getExprAsText()));					
				}
			}
		}
	}
	
	protected void replaceVarAttrValueByConst(final OrdinaryMorphism morph) {
		final Iterator<Node> nodes = morph.getTarget().getNodesSet().iterator();
		while (nodes.hasNext()) {
			final Node n = nodes.next();
			doReplaceVarAttrValueByConst(n.getAttribute(), morph);
		}
		final Iterator<Arc> arcs = morph.getTarget().getArcsSet().iterator();
		while (arcs.hasNext()) {
			final Arc a = arcs.next();
			doReplaceVarAttrValueByConst(a.getAttribute(), morph);
		}
	}
	
	private void doReplaceVarAttrValueByConst(
			final AttrInstance attrInstance,
			final OrdinaryMorphism morph) {
		if (attrInstance == null || this.attrMember2Expr.isEmpty())
			return;
		
		final ValueTuple valTuple = (ValueTuple) attrInstance;	
		for (int i=0; i<valTuple.getNumberOfEntries(); i++) {
			final ValueMember mem = valTuple.getValueMemberAt(i);
			if (mem.isSet() && mem.getExpr().isVariable()) {
				final Pair<String,String> p = this.attrMember2Expr.get(mem);
				if (p != null) { 
					final String expr = p.second;				
					final String var = mem.getExprAsText();
					if (mem.isTransient()) {
						mem.setExprAsText(expr);
						mem.setTransient(false);						
						((VarTuple) morph.getAttrContext().getVariables()).deleteLeafDeclaration(var);
					}
				}
			}
		}
	}
	
	protected OrdinaryMorphism extendLeftGraphByNAC(
			final OrdinaryMorphism isoLeft,
			final OrdinaryMorphism nac,
			boolean replaceConstantAttrByVariable) {

		Graph extLeft = isoLeft.getTarget();
		OrdinaryMorphism isoNAC = BaseFactory.theFactory().createMorphism(
				nac.getTarget(), extLeft);
		
		Hashtable<Node, Node> tmp = new Hashtable<Node, Node>(5);
		
		// first iterate over nodes
		Iterator<?> e = nac.getTarget().getNodesSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();

//			replaceExprAttrValueByVariable(o.getAttribute());
			
			if (replaceConstantAttrByVariable) {
				// replace and store constant attribute value
				replaceConstAttrValByVar(o.getAttribute());
			}
//			else 
			{
				Enumeration<GraphObject> en = nac.getInverseImage(o);
				if (!en.hasMoreElements())
					storeAttrValByName(o.getAttribute(), null);
				else
					storeAttrValByName(o.getAttribute(), en.nextElement().getAttribute());
			}
			
			Node n = null;
			if (!nac.getInverseImage(o).hasMoreElements()) {
				try {
					n = extLeft.copyNode((Node) o);
					n.setContextUsage(nac.hashCode());
					o.setContextUsage(nac.hashCode());					
					tmp.put((Node) o, n);					
				} catch (TypeException ex) {
//					System.out.println("extendLeftGraphByNAC:TypeException:copyNode  "+ex.getStackTrace());
				}
			} else {
				n = (Node) isoLeft.getImage(nac.getInverseImage(o).nextElement());
			}
			if (n != null) {
				try {
					isoNAC.addMapping(o, n);
				} catch (BadMappingException exc) {
////				System.out.println("extendLeftGraphByNAC:BadMappingException:Node  "+exc.getStackTrace());
				}
			}
		}
		// now iterate over arcs
		e = nac.getTarget().getArcsSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			
//			replaceExprAttrValueByVariable(o.getAttribute());
			
			if (replaceConstantAttrByVariable)
				replaceConstAttrValByVar(o.getAttribute());
			
			Arc a = null;
			if (!nac.getInverseImage(o).hasMoreElements()) {
				Node src = tmp.get(((Arc) o).getSource());
				if (src == null) {
					if (nac.getInverseImage(((Arc) o).getSource()).hasMoreElements())
						src = (Node) isoLeft.getImage(nac.getInverseImage(
								((Arc) o).getSource()).nextElement());
				}
				Node tar = tmp.get(((Arc) o).getTarget());
				if (tar == null) {
					if (nac.getInverseImage(((Arc) o).getTarget()).hasMoreElements())
						tar = (Node) isoLeft.getImage(nac.getInverseImage(
								((Arc) o).getTarget()).nextElement());
				}
				try {
					a = extLeft.copyArc((Arc) o, src, tar);
					a.setContextUsage(nac.hashCode());
					o.setContextUsage(nac.hashCode());
				} catch (TypeException ex) {
//					System.out.println("extendLeftGraphByNAC:TypeException:copyArc  "+ex.getStackTrace());
				}
			} else {
				a = (Arc) isoLeft.getImage(nac.getInverseImage(o).nextElement());
			}
			if (a != null) {
				try {
					isoNAC.addMapping(o, a);
				} catch (BadMappingException exc) {
//					System.out.println("extendLeftGraphByNAC:BadMappingException exc:NAC  "+exc.getStackTrace());
				}
			}
		}
		return isoNAC;
	}

	protected Pair<OrdinaryMorphism, OrdinaryMorphism>  extendLeftGraphByPAC(
			final OrdinaryMorphism isoLeft,
			final OrdinaryMorphism pac,
			boolean replaceConstantAttrByVariable) {
		
		Graph extLeft = isoLeft.getTarget();
		OrdinaryMorphism embedPAC = BaseFactory.theFactory().createMorphism(
				pac.getTarget(), extLeft);
		
		final Hashtable<Node, Node> tmp = new Hashtable<Node, Node>(5);
		Iterator<?> e = pac.getTarget().getNodesSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (!pac.getInverseImage(o).hasMoreElements()) {
				try {
					if (replaceConstantAttrByVariable) {
						// replace and store constant attribute value
						replaceConstAttrValByVar(o.getAttribute());
					}
					
					Node n = extLeft.copyNode((Node) o);
					n.setContextUsage(o.hashCode());
					o.setContextUsage(n.hashCode());
					tmp.put((Node) o, n);
					try {
						embedPAC.addMapping(o, n);						
					} catch (BadMappingException exc) {
//						System.out.println(exc.getStackTrace());
					}
				} catch (TypeException ex) {
//					System.out.println(ex.getStackTrace());
				}
			} else {
				try {
					embedPAC.addMapping(o, isoLeft.getImage(pac
							.getInverseImage(o).nextElement()));
				} catch (BadMappingException exc) {
//					System.out.println(exc.getStackTrace());
				}
			}
		}
		
		e = pac.getTarget().getArcsSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (!pac.getInverseImage(o).hasMoreElements()) {
				Node src = tmp.get(((Arc) o).getSource());
				if (src == null) {
					src = (Node) isoLeft.getImage(pac.getInverseImage(
							((Arc) o).getSource()).nextElement());
				}
				Node tar = tmp.get(((Arc) o).getTarget());
				if (tar == null) {
					tar = (Node) isoLeft.getImage(pac.getInverseImage(
							((Arc) o).getTarget()).nextElement());
				}
				try {
					if (replaceConstantAttrByVariable)
						replaceConstAttrValByVar(o.getAttribute());
					
					Arc a = extLeft.copyArc((Arc) o, src, tar);
					a.setContextUsage(o.hashCode());
					o.setContextUsage(a.hashCode()); 
					try {
						embedPAC.addMapping(o, a);
					} catch (BadMappingException exc) {
//						System.out.println(exc.getStackTrace());
					}
				} catch (TypeException ex) {
//					System.out.println(ex.getStackTrace());
				}
			} else {
				try {
					embedPAC.addMapping(o, isoLeft.getImage(
							pac.getInverseImage(o).nextElement()));
				} catch (BadMappingException exc) {
//					System.out.println(exc.getStackTrace());
				}
			}
		}
		return new Pair<OrdinaryMorphism, OrdinaryMorphism>(isoLeft, embedPAC);
	}
	
	private OrdinaryMorphism extendOverlapGraphByPACs(
			final OrdinaryMorphism overlap,
			final Rule r) {
		
		boolean failed = false;
		OrdinaryMorphism iso = overlap.getTarget().isoCopy();
		Graph extLeft = iso.getTarget();
		final Hashtable<Node, Node> tmp = new Hashtable<Node, Node>(5);
		Enumeration<OrdinaryMorphism> pacs = r.getPACs();
		while (!failed && pacs.hasMoreElements()) {
			OrdinaryMorphism pac = pacs.nextElement();
			if (!pac.isEnabled()) 
				continue;
			Iterator<?> e = pac.getTarget().getNodesSet().iterator();
			while (!failed && e.hasNext()) {
				GraphObject o = (GraphObject) e.next();
				if (!pac.getInverseImage(o).hasMoreElements()) {
					try {
						Node n = extLeft.copyNode((Node) o);
						n.setContextUsage(o.hashCode());
						o.setContextUsage(n.hashCode());
						tmp.put((Node) o, n);
					} catch (TypeException ex) {
						failed = true;
					}
				} 
			}
			
			e = pac.getTarget().getArcsSet().iterator();
			while (!failed && e.hasNext()) {
				GraphObject o = (GraphObject) e.next();
				if (!pac.getInverseImage(o).hasMoreElements()) {
					Node src = tmp.get(((Arc) o).getSource());
					if (src == null) {
						Enumeration<GraphObject> inv = pac.getInverseImage(((Arc) o).getSource()); 
						if (inv.hasMoreElements()) {
							GraphObject n = inv.nextElement();
							src = (Node) iso.getImage(overlap.getImage(n));
						}
					}
					failed = (src == null);
					if (!failed) {
						Node tar = tmp.get(((Arc) o).getTarget());
						if (tar == null) {
							Enumeration<GraphObject> inv = pac.getInverseImage(((Arc) o).getTarget()); 
							if (inv.hasMoreElements()) {
								GraphObject n = inv.nextElement();
								tar = (Node) iso.getImage(overlap.getImage(n));
							}
						}
						failed = (tar == null);
						if (!failed) {
							try {
								Arc a = extLeft.copyArc((Arc) o, src, tar);
								a.setContextUsage(o.hashCode());
								o.setContextUsage(a.hashCode()); 
							} catch (TypeException ex) {
								failed = true;
							}
						}
					}
				}
			}
			tmp.clear();
			if (failed) {
				iso.dispose(false, true);
				iso = null;
			}
		}
		return iso;
	}
		
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getOverlappingsVectorAttr(
			final Rule r1,
			final Rule r2, 
			final OrdinaryMorphism inclusionMorphism) {
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		result = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
		
		if ((r2.getSource().getNodesCount() < inclusionMorphism.getSource().getNodesCount())
				|| (r2.getSource().getArcsCount() < inclusionMorphism.getSource().getArcsCount())) 
			return result;
		
		final OrdinaryMorphism r2LeftIso = r2.getLeft().isomorphicCopy();
		if (r2LeftIso == null)
			return result;
				
		Pair<Rule, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		rulePair = (BaseFactory.theFactory()).constructIsomorphicRule(inclusionMorphism, true, false);
		if (rulePair == null)
			return result;
		
		// rulePair.first is a rule which helps to create an overlapping graph
		
		final Graph ograph = r2LeftIso.getTarget(); // the overlap graph
		
		MorphCompletionStrategy localStrategy = getLocalMorphismCompletionStrategy();			
		Match testm = (BaseFactory.theFactory()).createMatch(rulePair.first, ograph, true, "1");
		testm.getTarget().setCompleteGraph(false);
		testm.setCompletionStrategy(localStrategy, true);
		testm.getCompletionStrategy().initialize(testm);
		if (namedObjectOnly)
			this.addNamedObjectConstraint(testm);
		
//		r2.reflectTransientOfSimilarVar((VarTuple) testm.getAttrContext().getVariables());
		
		OrdinaryMorphism rStar = null;
		Match m = null;
		boolean nextMatch = true;
		
		while (nextMatch) {
			nextMatch = testm.nextCompletion();	
			nextMatch = nextMatch && testm.isMappingChanged();
			nextMatch = nextMatch && testm.isTypeMaxMultiplicitySatisfied();
			
			if (nextMatch) {
//				BaseFactory.theFactory().replaceTransientTarVarBySrcVar(testm);
				rStar = ograph.isomorphicCopy();
				m = (BaseFactory.theFactory()).createMatch(rulePair.first, rStar
							.getTarget());
				if (doCompose(localStrategy, m, testm, rStar)) {
					if (!(localStrategy instanceof Completion_InheritCSP)
								|| BaseFactory.theFactory().replaceParentByChild(rulePair.first, m, rStar)) {
						m.adaptAttrContextValues(testm.getAttrContext());
						try {
							// Variables inside of testm.getTarget() graph are allowed
							OrdinaryMorphism
							ms = (OrdinaryMorphism) TestStep.execute(m,true,this.equalVariableNameOfAttrMapping);
							
							if ((this.grammar == null)
									|| (!this.grammar.getConstraints().hasMoreElements()))
								this.consistentOnly = false; 
							boolean consistentOverlap = true;
							if (ms != null && this.consistentOnly) {
								if (!checkGraphConsistency(ms.getTarget(), r1.getLayer()))
									consistentOverlap = false;
							}
							if (ms != null && consistentOverlap) {
								OrdinaryMorphism mStar = rulePair.second.second.compose(ms); 
								ms.dispose(); ms = null;	// ms - not more needed
								
								if (mStar != null) {
									Pair<OrdinaryMorphism, OrdinaryMorphism> 
									p = getValidMatchChangeAttr(r1, r2, r2LeftIso, mStar, rStar);
									if (p != null) {
										if (namedObjectOnly && !namedObjsConform(p)) {	
											p.first.dispose();
											p.second.dispose();
											p = null;
										}
									}
									if (p != null && attributeCritical(r1, r2, p, null)) {
										p.first.unsetCompletionStrategy();
										p.second.unsetCompletionStrategy();
										Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
										resultp = new Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>(
												p, null);
										
										if (this.maxBoundOfCriticKind == 0 
												|| result.size() < this.maxBoundOfCriticKind)  {
											result.addElement(resultp);
											if (result.size() == this.maxBoundOfCriticKind)
												nextMatch = false;	
										}
										else
											nextMatch = false;										
										
										if (!this.complete) 
											nextMatch = false;
									} else {
										if (p != null) {
											p.first.dispose();									
											p.second.dispose();
										}
										mStar.dispose(); mStar = null;
										rStar.dispose(); rStar = null;
									}
								}
							} else {
								rStar.dispose(); rStar = null;
							}
						} catch (TypeException e) {
//							System.out.println(e.getLocalizedMessage());
						}
					} else {
						rStar.dispose(); rStar = null;
					}
				} else {
					rStar.dispose(); rStar = null;
				}
				m.dispose(); m = null;
			}
		} // while

		// dispose helpers
		testm.dispose(); testm = null;
		localStrategy = null;
		rulePair.second.first.dispose();
		rulePair.second.second.dispose();
		rulePair.first.disposeSuper();
		rulePair.second.first = null;
		rulePair.second.second = null;
		rulePair.first = null;
		rulePair = null;
		
		return result;
	}

	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getOverlappingsVectorAttr(
			final Rule r1,
			final Rule r2, 
			Pair<OrdinaryMorphism, OrdinaryMorphism> extendedByPACsL2iso,
			final OrdinaryMorphism inclusionMorphism) {
				
		final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		result = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
		
		if ((extendedByPACsL2iso.first.getTarget().getNodesCount() < inclusionMorphism.getSource().getNodesCount())
				|| (extendedByPACsL2iso.first.getTarget().getArcsCount() < inclusionMorphism.getSource().getArcsCount())) 
			return result;
		
		Pair<Rule, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		rulePair = (BaseFactory.theFactory()).constructIsomorphicRule(inclusionMorphism, true, false);
		if (rulePair == null)
			return result;
		
		// rulePair.first is a rule which helps to create an overlapping graph

		Graph ograph = extendedByPACsL2iso.first.getTarget();

		MorphCompletionStrategy localStrategy = getLocalMorphismCompletionStrategy();						
		Match testm = (BaseFactory.theFactory()).createMatch(rulePair.first, ograph, true, "1");
		testm.getTarget().setCompleteGraph(false);
		testm.setCompletionStrategy(localStrategy, true);
		testm.getCompletionStrategy().initialize(testm);
		if (namedObjectOnly)
			this.addNamedObjectConstraint(testm);
		
		OrdinaryMorphism rStar = null;
		Match m = null;
		boolean nextMatch = true;
		
		while (nextMatch) {
			nextMatch = testm.nextCompletion();
			nextMatch = nextMatch && testm.isMappingChanged();
			nextMatch = nextMatch && testm.isTypeMaxMultiplicitySatisfied();
			
			if (nextMatch) {
//				BaseFactory.theFactory().replaceTransientTarVarBySrcVar(testm);
				rStar = ograph.isomorphicCopy();
				if (rStar == null)
					break;
				
				m = (BaseFactory.theFactory()).
						createMatch(rulePair.first, rStar.getTarget());
				if (doCompose(localStrategy, m, testm, rStar)) {						
					if (!(localStrategy instanceof Completion_InheritCSP)
							|| BaseFactory.theFactory().replaceParentByChild(rulePair.first, m, rStar)) {
						m.adaptAttrContextValues(testm.getAttrContext());
						try {
							// Variables in testm.getTarget() graph are allowed
							OrdinaryMorphism 
							ms = (OrdinaryMorphism) TestStep.execute(m,true,this.equalVariableNameOfAttrMapping);
							if ((this.grammar == null)
									|| (!this.grammar.getConstraints().hasMoreElements()))
								this.consistentOnly = false;
							// overlapping graph
							boolean consistentOverlap = true;
							if (ms != null && this.consistentOnly) {
								if (!checkGraphConsistency(ms.getTarget(), r1.getLayer()))
									consistentOverlap = false;
							}
							if (ms != null && consistentOverlap) {
								OrdinaryMorphism mStar = rulePair.second.second.compose(ms);
								ms.dispose(); ms = null; // ms - not more needed
								
								if (mStar != null) {
									Pair<OrdinaryMorphism, OrdinaryMorphism>
									p = getValidMatchChangeAttr(r1, r2,
												extendedByPACsL2iso.first, mStar, rStar);
									if (p != null) {
										if (namedObjectOnly && !namedObjsConform(p)) {												
											p = null;
										}
									}
									if (p != null 
											&& attributeCritical(r1,r2,p,
													new Pair<OrdinaryMorphism, OrdinaryMorphism>(
															extendedByPACsL2iso.first, rStar))) {
										p.first.unsetCompletionStrategy();
										p.second.unsetCompletionStrategy();
										Pair<OrdinaryMorphism, OrdinaryMorphism> 
										p2 = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
													extendedByPACsL2iso.second, rStar);
										p2.first.unsetCompletionStrategy();
										p2.second.unsetCompletionStrategy();
										
										Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
										resultp = new Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>(
												p, p2);
										
										if (this.maxBoundOfCriticKind == 0 
												|| result.size() < this.maxBoundOfCriticKind)  {
											result.addElement(resultp);
											if (result.size() == this.maxBoundOfCriticKind)
												nextMatch = false;	
										}
										else
											nextMatch = false;	

										if (!this.complete) 
											nextMatch = false;
									} else {
										mStar.dispose(); mStar = null;
										rStar.dispose(); rStar = null;
									}
								}
							} else {
								rStar.dispose(); rStar = null;
							}
						} catch (TypeException e) {}
					} else {
						rStar.dispose(); rStar = null;
					}
				} else {
					rStar.dispose(); rStar = null;
				}
				m.dispose(); m = null;
			} 			
		} // while
			
		// dispose helpers
		testm.dispose(); testm = null;
		localStrategy = null;
		rulePair.second.first.dispose();
		rulePair.second.second.dispose();
		rulePair.first.disposeSuper();
		rulePair.first = null;
		rulePair.second.first = null;
		rulePair.second.second = null;
		rulePair = null;
		
		return result;
	}

	
	@SuppressWarnings("unused")
	private int getSizeOfCriticalOverlapping(final Pair<OrdinaryMorphism, OrdinaryMorphism> criticalPair) {
		int nn = 0;
		int criticals = 0;
		final OrdinaryMorphism m1 = criticalPair.first;
		final OrdinaryMorphism m2 = criticalPair.second;
		final Graph g = m1.getTarget();
		Iterator<Node> nodes = g.getNodesSet().iterator();
		while (nodes.hasNext()) {
			GraphObject o = nodes.next();
			if (m1.getInverseImage(o).hasMoreElements()
					&& m2.getInverseImage(o).hasMoreElements()) {
				nn++;
//				if (o.isCritical())
					criticals++;
			}
		}
		Iterator<Arc> arcs = g.getArcsSet().iterator();
		while (arcs.hasNext()) {
			GraphObject o = arcs.next();
			if (m1.getInverseImage(o).hasMoreElements()
					&& m2.getInverseImage(o).hasMoreElements()) {
				nn++;
//				if (o.isCritical())
					criticals++;
			}
		}
//		System.out.println("ExclutePair.getSizeOfCriticalOverlapping::  nn: "+nn+"   criticals: "+criticals);
		return nn;
	}
	
	@SuppressWarnings("unused")
	private boolean ruleCreatesNamedObject(Rule r) {
		List<GraphObject> newObjs = r.getElementsToCreate();
		for (int i=0; i<newObjs.size(); i++) {
			GraphObject go = newObjs.get(i);
			if (go.getObjectName() != null 
					&& !go.getObjectName().equals("")) {
				return true;
			}
		}
		return false;
	}
	
	private boolean namedObjsConform(Pair<OrdinaryMorphism, OrdinaryMorphism> p) {
		Iterator<Node> nodes = p.first.getTarget().getNodesSet().iterator();
		while (nodes.hasNext()) {
			Node n = nodes.next();
			if (n.getObjectName() != null && !n.getObjectName().equals("")) {
				List<GraphObject> l1 = p.first.getInverseImageList(n);
				List<GraphObject> l2 = p.second.getInverseImageList(n);
				if (l1.size() == 0 || l2.size() == 0) {
					return false;
				}
				else if (!l1.get(0).getObjectName().equals(l2.get(0).getObjectName())) {
					return false;
				}
			}
		}
		return true;
	}
	
	protected Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getOverlappingsVectorDeleteUse(
			final Rule r1,
			final Rule r2, 
			final OrdinaryMorphism inclusionMorphism) {

		final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		result = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
		
		final OrdinaryMorphism r2LeftIso = r2.getLeft().isomorphicCopy();
		if (r2LeftIso == null)
			return result;
		
		Pair<Rule, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		rulePair = (BaseFactory.theFactory()).constructIsomorphicRule(inclusionMorphism, true, false);
		if (rulePair == null)
			return result;
		
//		if (ruleCreatesNamedObject(rulePair.first))
//			return result;
		
		// rulePair.first is a rule which helps to create an overlapping graph
		
//		((VarTuple) rulePair.first.getAttrContext().getVariables()).showVariables();		
//		((VarTuple) r1.getAttrContext().getVariables()).showVariables();
//		((VarTuple) r2.getAttrContext().getVariables()).showVariables();
			
		Graph ograph = r2LeftIso.getTarget(); // overlap graph
		MorphCompletionStrategy localStrategy = getLocalMorphismCompletionStrategy();
		Match testm = BaseFactory.theFactory().createMatch(rulePair.first, ograph, true, "1");
		testm.getTarget().setCompleteGraph(false);
		testm.setCompletionStrategy(localStrategy, true);
		testm.getCompletionStrategy().initialize(testm);
		if (namedObjectOnly)
			this.addNamedObjectConstraint(testm);
		
//		r2.reflectTransientOfSimilarVar((VarTuple) testm.getAttrContext().getVariables());
		
//		((VarTuple) r1.getAttrContext().getVariables()).showVariables();
//		((VarTuple) r2.getAttrContext().getVariables()).showVariables();
		
		OrdinaryMorphism rStar = null;
		Match m = null;
		boolean nextMatch = true;
						
		while (nextMatch) {
			nextMatch = testm.nextCompletion();
//			System.out.println(testm.getErrorMsg());
			nextMatch = nextMatch && testm.isMappingChanged();			
			nextMatch = nextMatch && testm.isTypeMaxMultiplicitySatisfied();
//			System.out.println(testm.getErrorMsg());		
			if (nextMatch) {
				BaseFactory.theFactory().replaceTransientTarVarBySrcVar(testm);
				rStar = ograph.isomorphicCopy();
				// rStar.getTarget() == m.getTarget  is the overlap graph
				m = (BaseFactory.theFactory()).createMatch(rulePair.first, rStar.getTarget());
				if (doCompose(localStrategy, m, testm, rStar)) {														
					if (!(localStrategy instanceof Completion_InheritCSP)
							|| BaseFactory.theFactory().replaceParentByChild(rulePair.first, m, rStar)) {
						m.adaptAttrContextValues(testm.getAttrContext());
						try {
							// Variables in testm.getTarget() graph are allowed
							OrdinaryMorphism ms = (OrdinaryMorphism) TestStep.execute(m, true, this.equalVariableNameOfAttrMapping);
							if (ms != null) {
								if ((this.grammar == null)
										|| (!this.grammar.getConstraints().hasMoreElements()))
									this.consistentOnly = false;
								
								boolean consistentOverlap = true;
								if (this.consistentOnly) {
									// check consistency of the overlapping graph
									// ms.getTarget() == m.getTarget  is the overlap graph
									ms.getTarget().setCompleteGraph(false);
									if (!checkGraphConsistency(ms.getTarget(), r1.getLayer())){
										consistentOverlap = false;
									}
								}
								if (consistentOverlap) {
									OrdinaryMorphism mStar = rulePair.second.second.compose(ms); 															
									ms.dispose(); ms = null;	// ms - not more needed
									
									if (mStar != null) {
										// mStart is the first overlap morphism
										// rStart is the second overlap morphism
										Pair<OrdinaryMorphism, OrdinaryMorphism> 
										p = getValidMatchDeleteUse(r1, r2, r2LeftIso, mStar, rStar);	
										if (p != null) {
											if (namedObjectOnly && !namedObjsConform(p)) {												
												p = null;
											}
										}
										// now result pair contains 2 pairs:
										// in this case: first is p; second is null
										if (p != null) {
											Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
											resultp = new Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>(
													p, null);											
											markDeleteUseCriticalObject(r2, resultp);
											
											if (this.maxBoundOfCriticKind == 0 
													|| result.size() < this.maxBoundOfCriticKind)  {
												result.addElement(resultp);
												if (result.size() == this.maxBoundOfCriticKind)
													nextMatch = false;	
											}
											else
												nextMatch = false;											
											
											if (!this.complete) 
												nextMatch = false;
										} else {
											mStar.dispose(); mStar = null;
											rStar.dispose(); rStar = null;
										}
									}
								} else {
									rStar.dispose(); rStar = null;
								}
							}
						} catch (TypeException e) {}
					}  else {
						rStar.dispose(); rStar = null;
					}
				} else {
					rStar.dispose(); rStar = null;
				}
				m.dispose(); m = null;
			}
		} // while
			
		// dispose helpers
		testm.dispose(); testm = null;
		localStrategy = null;
		rulePair.second.first.dispose();
		rulePair.second.second.dispose();
		rulePair.first.disposeSuper();
		rulePair.second.first = null;
		rulePair.second.second = null;
		rulePair = null;
		r2LeftIso.dispose();		

		return result;
	}

	protected Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getOverlappingsVectorDeleteUse(
			final Rule r1,
			final Rule r2, 
			final Pair<OrdinaryMorphism, OrdinaryMorphism> extendedByPACsL2iso,
			final OrdinaryMorphism inclusionMorphism) {
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		result = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();

		Graph other = extendedByPACsL2iso.first.getTarget();
					
		Pair<Rule, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		rulePair = (BaseFactory.theFactory()).constructIsomorphicRule(inclusionMorphism, true, false);
		if (rulePair == null)
			return result;
		
//		if (ruleCreatesNamedObject(rulePair.first))
//			return result;
		
		// rulePair.first is a rule which helps to create an overlapping graph
		MorphCompletionStrategy localStrategy = getLocalMorphismCompletionStrategy();			
		Match testm = (BaseFactory.theFactory()).createMatch(rulePair.first, other, true, "1");
		testm.getTarget().setCompleteGraph(false);
		testm.setCompletionStrategy(localStrategy, true);
		testm.getCompletionStrategy().initialize(testm);
		if (namedObjectOnly)
			this.addNamedObjectConstraint(testm);
		
		OrdinaryMorphism rStar = null;
		Match m = null;
		boolean nextMatch = true;
		
		while (nextMatch) {
			nextMatch = testm.nextCompletion();
			nextMatch = nextMatch && testm.isMappingChanged();
			nextMatch = nextMatch && testm.isTypeMaxMultiplicitySatisfied();
			
			if (nextMatch) {
//				BaseFactory.theFactory().replaceTransientTarVarBySrcVar(testm);
				rStar = other.isomorphicCopy();
				if (rStar == null)
					break;
				
				m = (BaseFactory.theFactory()).createMatch(rulePair.first, rStar.getTarget());
				if (doCompose(localStrategy, m, testm, rStar)) {																				
					if (!(localStrategy instanceof Completion_InheritCSP)
							|| BaseFactory.theFactory().replaceParentByChild(rulePair.first, m, rStar)) {
						m.adaptAttrContextValues(testm.getAttrContext());
						try {
							// Variables in testm.getTarget() graph are allowed
							OrdinaryMorphism 
							ms = (OrdinaryMorphism) TestStep.execute(m,true,this.equalVariableNameOfAttrMapping);
							if (ms != null) {
								if ((this.grammar == null)
										|| (!this.grammar.getConstraints().hasMoreElements()))
									this.consistentOnly = false;
								// overlapping graph
								boolean consistentOverlap = true;
								if (this.consistentOnly) {							
									ms.getTarget().setCompleteGraph(false);
									if (!checkGraphConsistency(ms.getTarget(), r1.getLayer())) {
										consistentOverlap = false;
									}
								}
								if (consistentOverlap) {
									OrdinaryMorphism mStar = rulePair.second.second.compose(ms); 						
									ms.dispose(); ms = null; // ms - not more needed
									
									if (mStar != null) {
										Pair<OrdinaryMorphism, OrdinaryMorphism>
										p = getValidMatchDeleteUse(r1, r2,
												extendedByPACsL2iso.first, mStar, rStar);
										if (p != null) {
											if (namedObjectOnly && !namedObjsConform(p)) {												
												p = null;
											}
										}
										// now result pair contains 2 pairs:
										if (p != null) {
											Pair<OrdinaryMorphism, OrdinaryMorphism>
											p2 = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
													extendedByPACsL2iso.second, rStar);
											Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
											resultp = new Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>(
													p, p2);
											markDeleteUseCriticalObject(r2, resultp);
											
											if (this.maxBoundOfCriticKind == 0 
													|| result.size() < this.maxBoundOfCriticKind)  {
												result.addElement(resultp);
												if (result.size() == this.maxBoundOfCriticKind)
													nextMatch = false;	
											}
											else
												nextMatch = false;	
											
											if (!this.complete) 
												nextMatch = false;
										} else {
											mStar.dispose(); mStar = null;
											rStar.dispose(); rStar = null;
										}
									}
								} else {
									rStar.dispose(); rStar = null;
								}
							}
						} catch (TypeException e) {}
					} else {
						rStar.dispose(); rStar = null;
					}
				} else {
					rStar.dispose(); rStar = null;
				}
				m.dispose(); m = null;
			}
		} // while

		testm.dispose(); testm = null;
		localStrategy = null;
		rulePair.second.first.dispose();
		rulePair.second.second.dispose();
		rulePair.first.disposeSuper();
		rulePair.first = null;
		rulePair.second.first = null;
		rulePair.second.second = null;
		rulePair = null;

		return result;
	}

	private void extendTypeObjectsMapByChildObjs(Graph g) {
		Iterator<Node> nodes = g.getNodesSet().iterator();
		while (nodes.hasNext()) {
			Node n = nodes.next();
			Vector<Type> childTypes = n.getType().getChildren();
			for (int i=0; i<childTypes.size(); i++) {
				Type childT = childTypes.get(i);
				HashSet<GraphObject> objs = g.getTypeObjectsMap().get(childT.convertToKey());
				if (objs != null && !objs.contains(n)) {
					objs.add(n);
				}
			}				
		}
	}
	
	
	private boolean tryValidateNACAttrCond(
			final Rule r2, 
			final OrdinaryMorphism nac,
			final OrdinaryMorphism nacEmbedding,
			final Match match) {

		boolean result = true;
		if (nac.getAttrContext().getConditions().getNumberOfEntries() > 0) {
			final List<VarMember> list = new Vector<VarMember>(1);
			final Enumeration<GraphObject> nacdom = nacEmbedding.getDomain();
			while (nacdom.hasMoreElements()) {
				final GraphObject nacobj = nacdom.nextElement();
				final GraphObject obj2 = nacEmbedding.getImage(nacobj);				
				if (match.getInverseImage(obj2).hasMoreElements()) {					
					final GraphObject obj1 = match.getInverseImage(obj2).nextElement();
					if (obj2.getAttribute() != null) {
						for (int i=0; i<obj2.getAttribute().getNumberOfEntries(); i++) {
							ValueMember obj2mem = (ValueMember) obj2.getAttribute().getMemberAt(i);
							ValueMember obj1mem = (ValueMember) obj1.getAttribute().getMemberAt(obj2mem.getName());
							if (obj1mem != null && obj1mem.isSet() && obj1mem.getExpr().isConstant()
									&& obj2mem.isSet() && obj2mem.getExpr().isVariable()) {
								VarMember var = nac.getAttrContext().getVariables().getVarMemberAt(obj2mem.getExprAsText());
								if (var != null) {
									var.setExprAsText(obj1mem.getExprAsText());
									list.add(var);
								}
							}
						}
					}
				}
			}
		
//			((VarTuple) nac.getAttrContext().getVariables()).showVariables();
//			((CondTuple) nac.getAttrContext().getConditions()).showConditions();			
			result = this.checkAttrCondOfApplCond(nac);		

			for (int i=0; i<list.size(); i++) {
				list.get(i).undoUnification();
				list.get(i).setExpr(null);
			}
		}
		return result;
	}
	
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getOverlappingsVectorProduceForbid(
			final Rule r1, 
			final Rule r2, 
			final OrdinaryMorphism nac, 
			final Pair<OrdinaryMorphism, OrdinaryMorphism> extendedL2isoPair,
			final OrdinaryMorphism inclusionMorphism) {
		
//System.out.println("getOverlappingsVectorProduceForbid(5)  "+r1.getName()+"  &  "+r2.getName());

		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		result  = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();

		OrdinaryMorphism extendedL2iso = extendedL2isoPair.first;
		OrdinaryMorphism extendedNAC2iso = extendedL2isoPair.second;
		
		// construct rule from h
		Pair<Rule, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		rulePair = (BaseFactory.theFactory()).constructIsomorphicRule(inclusionMorphism, true, true);
		if (rulePair == null)
			return result;
		
		Rule hr = rulePair.first; // help rule to create overlappings

		OrdinaryMorphism isoLHShr = rulePair.second.first;
		OrdinaryMorphism isoRHShr = rulePair.second.second;
		
		// mark potential conflict objects
		for (Iterator<Node> en = inclusionMorphism.getSource().getNodesSet().iterator(); en
					.hasNext();) {
			GraphObject o = en.next();
			GraphObject i_o = inclusionMorphism.getImage(o);
			if (!r1.getInverseImage(i_o).hasMoreElements()) {
				GraphObject obj = isoLHShr.getImage(o);
				obj.setCritical(true);
			}
		}
		for (Iterator<Arc> en = inclusionMorphism.getSource().getArcsSet().iterator(); 
		en.hasNext();) {
			GraphObject o = en.next();
			GraphObject i_o = inclusionMorphism.getImage(o);
			if (!r1.getInverseImage(i_o).hasMoreElements()) {
				GraphObject obj = isoLHShr.getImage(o);
				obj.setCritical(true);
			}
		}

		MorphCompletionStrategy localStrategy = getLocalMorphismCompletionStrategy();			
		Match testm = (BaseFactory.theFactory()).createMatch(hr,
					extendedL2iso.getTarget(), true, "1");
		testm.getTarget().setCompleteGraph(false);		
		testm.setCompletionStrategy(localStrategy, true);
		testm.getCompletionStrategy().initialize(testm);
		if (namedObjectOnly)
			this.addNamedObjectConstraint(testm);
				
		OrdinaryMorphism rStar = null;			
		Match m = null;
		boolean nextMatch = true;
		while (nextMatch) {			
			nextMatch = testm.nextCompletion();	
			nextMatch = nextMatch && testm.isMappingChanged();	
			nextMatch = nextMatch && testm.isTypeMaxMultiplicitySatisfied();
			
			boolean condOK = this.tryValidateNACAttrCond(r2, nac, extendedNAC2iso, testm);
			
			if (nextMatch && condOK) {	
//				BaseFactory.theFactory().replaceTransientTarVarBySrcVar(testm);
				rStar = extendedL2iso.getTarget().isomorphicCopy();
				if (rStar == null)
					break;
				
				if (this.withInheritance)
					this.extendTypeObjectsMapByChildObjs(rStar.getTarget());
				
				m = (BaseFactory.theFactory()).createMatch(hr, rStar.getTarget());
				if (doCompose(localStrategy, m, testm, rStar)) {
					if (!(localStrategy instanceof Completion_InheritCSP)
							|| BaseFactory.theFactory().replaceParentByChild(hr, m, rStar)) {
						m.adaptAttrContextValues(testm.getAttrContext());
						// check whether at least one of critical objects matches
						// into N2-L2
						boolean criticalOK = false;
						Iterator<?> en = testm.getSource().getNodesSet().iterator();
						while (en.hasNext()) {
							GraphObject o = (GraphObject) en.next();							
							if (o.isCritical()) {
								GraphObject i_o = testm.getImage(o);
								if (!extendedL2iso.getInverseImage(i_o).hasMoreElements()) {
									criticalOK = true;
									m.getImage(o).setCritical(true);
								}
								else {
									if (extendedNAC2iso.getInverseImage(i_o).hasMoreElements()) {
										GraphObject t = extendedNAC2iso.getInverseImage(i_o).nextElement();
										if (t.getContextUsage() == nac.hashCode()) { 
											criticalOK = true;
											m.getImage(o).setCritical(true);
										}
									}
								}
							}
						}
						en = testm.getSource().getArcsSet().iterator();
						while (en.hasNext()) {
							GraphObject o = (GraphObject) en.next();
							if (o.isCritical()) {
								GraphObject i_o = testm.getImage(o);
								if (!extendedL2iso.
										getInverseImage(i_o).hasMoreElements()) {
									criticalOK = true;
									m.getImage(o).setCritical(true);
								}
							}
						}
						if (criticalOK) {
							try {
								// Variables in work graph are allowed
								OrdinaryMorphism
								ms = (OrdinaryMorphism) TestStep.execute(m, true ,this.equalVariableNameOfAttrMapping);
								if ((this.grammar == null)
										|| (!this.grammar.getConstraints()
												.hasMoreElements()))
									this.consistentOnly = false;
								boolean consistentOverlap = true;
								if (ms != null && this.consistentOnly) {
									ms.getTarget().setCompleteGraph(false);
									if (!checkGraphConsistency(
											ms.getTarget(), r1.getLayer()))
										consistentOverlap = false;
								}
								if (ms != null && consistentOverlap) {
									// make help mStar of first overlap morphism
									OrdinaryMorphism mStar = isoRHShr.compose(ms);					
									ms.dispose(); ms = null; // ms - not more needed
									
									if (mStar != null) {
										Pair<OrdinaryMorphism, OrdinaryMorphism> 
										p = getValidMatchProduceForbid(
														r1, r2, nac, mStar, rStar,
														extendedL2iso);
										if (p != null) {
											if (namedObjectOnly && !namedObjsConform(p)) {												
												p = null;
											}
										}
										if (p != null) {
											// now result pair contains 2 pairs:
											// in this case: first is p1;
											// second is a help pair p2 with two
											// morphisms:
											// first = r2.left -> N2;
											// second = nac.target -> N2
											Pair<OrdinaryMorphism, OrdinaryMorphism> 
											p1 = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
//													p.first, p.second);
													mStar, rStar);
											Pair<OrdinaryMorphism, OrdinaryMorphism> 
											p2 = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
													extendedL2iso,
													extendedNAC2iso);
											Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
											resultp = new Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>(
													p1, p2);
											markProduceForbidCriticalObject(resultp);
											
											if (this.maxBoundOfCriticKind == 0 
													|| result.size() < this.maxBoundOfCriticKind)  {
												result.addElement(resultp);
												if (result.size() == this.maxBoundOfCriticKind)
													nextMatch = false;	
											}
											else
												nextMatch = false;	

											if (!this.complete) 
												nextMatch = false;	
										} else {
											mStar.dispose(); mStar = null;
											rStar.dispose(); rStar = null;
										}
									}
								} else {
									rStar.dispose(); rStar = null;
								}
							} catch (TypeException e) {}
						}						
					} else {
						rStar.dispose(); rStar = null;
					}
				} else {
					rStar.dispose(); rStar = null;
				}	
				m.dispose(); m = null;
			} else 
				nextMatch = false;
		} // while

		// dispose helpers
		testm.dispose(); testm = null;
		localStrategy = null;
		isoLHShr.dispose();
		isoRHShr.dispose();
		hr.disposeSuper();
		isoLHShr = null;
		isoRHShr = null;
		hr = null;
		rulePair = null;

		return result;
	}

	


	/*
	private Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			isIsomorphicOverlapping(
					final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> overlapPairs, 
					Pair<OrdinaryMorphism, OrdinaryMorphism> overlapPair) {

		Graph overlapGraph = overlapPair.first.getTarget();
		for (int j = 0; j < overlapPairs.size() && !this.stop; j++) {
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> pj = overlapPairs.elementAt(j);
			Pair<OrdinaryMorphism, OrdinaryMorphism> p1 = pj.first;
			Graph g = p1.first.getTarget();
			Vector<OrdinaryMorphism> overlapIsos = g.getIsomorphicWith(overlapGraph, true);
			for (int i = 0; i < overlapIsos.size(); i++) {
				OrdinaryMorphism overlapIso = overlapIsos.get(i);
				if (overlapIso != null) {
					if (p1.first.isIsomorphicTo(overlapPair.first, overlapIso)) {
						if (p1.second.isIsomorphicTo(overlapPair.second,overlapIso)) {
							return pj;
						}
					}
				}
			}
		}
		return null;
	}
*/
	
	
	
	
	/*
	 * Return inclusions with size <= maxSize.
	 * @param maxSize
	 * @param inclusions  a Vector with inclusions
	 * @return
	 */
//	@SuppressWarnings("unused")
//	private Vector<Vector<GraphObject>> removeInclusionBiggerMaxSize(
//			int maxSize,
//			final Vector<Vector<GraphObject>> inclusions) {
//		final Vector<Vector<GraphObject>> result = new Vector<Vector<GraphObject>>();
//		for (int i=0; i<inclusions.size(); i++) {
//			Vector<GraphObject> vec = inclusions.get(i);
//			if (vec.size() <= maxSize)
//				result.add(vec);
//		}
//		inclusions.trimToSize();
//		return result;
//	}

	/*
	private void removeEqualInclusion(final Vector<Vector<GraphObject>> incls) {		
		final Vector<Vector<GraphObject>> tmp = new Vector<Vector<GraphObject>>();
		for (int i=incls.size()-1; i>=0; i--) {
			final Vector<GraphObject> vi = incls.get(i);
			for (int j=incls.size()-1; j>=0; j--) {
				final Vector<GraphObject> vj = incls.get(j);
				if (vj != vi) {
					if (vj.size() == vi.size()) {
//						System.out.println("removeEqualInclusion:: "+vj.size()+" == "+vi.size());
						boolean found = true;
						for (int k=0; k<vj.size() && found; k++) {
							GraphObject go = vj.get(k);
							if (!vi.contains(go)) {
								found = false;
							}								
						}
						for (int k=0; k<vi.size() && found; k++) {
							GraphObject go = vi.get(k);
							if (!vj.contains(go)) {
								found = false;
							}								
						}
						if (found) 
							tmp.add(vj);
					}
				}
			}
			if (!tmp.isEmpty()) {
				incls.removeAll(tmp);
//				System.out.println("removeEqualInclusion:: tmp.size: "+tmp.size());
				tmp.clear();
			}
		}
	}
	*/
	
	/*
	 * Compute left critical (delete) context, boundary and preserved sets of rule objects
	 * due to the given neededTypes.
	 * Fill also set toDelete.
	 */
	void computeLeftC_B_K(final Rule r, 
			final Vector<GraphObject> context,
			final Vector<GraphObject> boundary, 
			final Vector<GraphObject> preserved,
			final Vector<GraphObject> toDelete,
			final Vector<Pair<Type, Pair<Type, Type>>> neededTypes) {

		// first handle nodes
		Iterator<?> e = r.getLeft().getNodesSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (isInTypes(neededTypes, o)) {
				if (r.getImage(o) == null) {
					context.add(o);
					toDelete.add(o);
				} else 
					preserved.add(o);
			} 
		}
		// now handle edges
		e = r.getLeft().getArcsSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (isInTypes(neededTypes, o)) {
				if (r.getImage(o) == null) {
					if (!context.contains(((Arc) o).getSource())) {
						context.add(((Arc) o).getSource());
					}						
					if (!context.contains(((Arc) o).getTarget())) {
						context.add(((Arc) o).getTarget());
					}
										
					context.add(o);
					toDelete.add(o);
					// remove preserved src, tar of delete edge from preserved container
					// src, tar of delete edge are in context container only
					preserved.remove(((Arc) o).getSource());
					preserved.remove(((Arc) o).getTarget());
				} else {
					preserved.add(o);
				}
			}
		}
	}

	/*
	 * Compute right critical (produce) context, boundary and preserved sets of rule objects
	 * due to the given neededTypes.
	 * Fill also set toProduce.
	 */
	void computeRightC_B_K(final Rule r, 
			final Vector<GraphObject> context,
			final Vector<GraphObject> boundary, 
			final Vector<GraphObject> preserved,
			final Vector<GraphObject> toProduce,
			final Vector<Pair<Type, Pair<Type, Type>>> neededTypes) {
		// first handle nodes
		Iterator<?> e = r.getRight().getNodesSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (isInTypes(neededTypes, o)) {
				if (!r.getInverseImage(o).hasMoreElements()) {
					context.add(o);
					toProduce.add(o);
				} else {
					if (!this.essential)
						preserved.add(o);
//					else //if (o.getType().getSourceMax() > 0) 
//						context.add(o);
				}
			} else {
//				if (o.getType().getSourceMax() > 0) 
				{
					if (!this.essential) {
						if (!r.getInverseImage(o).hasMoreElements()) 
							toProduce.add(o);
						else 
							preserved.add(o);
					}
//					else 
//						context.add(o);
				} 
			}			
		} 
		
		// now handle edges
		e = r.getRight().getArcsSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
//			Type src_t = ((Arc) o).getSource().getType();
//			Type tar_t = ((Arc) o).getTarget().getType();
			if (isInTypes(neededTypes, o)) {
				if (!r.getInverseImage(o).hasMoreElements()) {
					/*
					 * boundary is not used now
					 * if(r.getInverseImage(((Arc)o).getSource()).hasMoreElements()){
					 * if(!boundary.contains(((Arc)o).getSource()))
					 * boundary.add(((Arc)o).getSource()); }
					 * if(r.getInverseImage(((Arc)o).getTarget()).hasMoreElements()){
					 * if(!boundary.contains(((Arc)o).getTarget()))
					 * boundary.add(((Arc)o).getTarget()); }
					 */
					
					if (!context.contains(((Arc) o).getSource())) 
						context.add(((Arc) o).getSource());					
					if (!context.contains(((Arc) o).getTarget())) 
						context.add(((Arc) o).getTarget());					
										
					context.add(o);
					toProduce.add(o);
					// remove preserved src, tar of new edge from preserved
					// container
					// src, tar of new edge are in context container only
					preserved.remove(((Arc) o).getSource());
					preserved.remove(((Arc) o).getTarget());
				} else {
					if (!this.essential) {
						preserved.add(o);
					} 
				}
			} 
			else {	
//				if (o.getType().getSourceMax(src_t, tar_t) > 0
//						|| o.getType().getTargetMax(src_t, tar_t) > 0) 
				{
					if (!this.essential) {
						if (!r.getInverseImage(o).hasMoreElements()) 
							toProduce.add(o);
						else {
							preserved.add(o);
							if (!preserved.contains(((Arc) o).getSource())
									&& !context.contains(((Arc) o).getSource()))
								preserved.add(((Arc) o).getSource());
							if (!preserved.contains(((Arc) o).getTarget())
									&& !context.contains(((Arc) o).getTarget()))
								preserved.add(((Arc) o).getTarget());
						}
					} 
//					else {
//						context.add(o);
//						if (!context.contains(((Arc) o).getTarget()))
//							context.add	(((Arc) o).getTarget());
//						if (!context.contains(((Arc) o).getSource()))
//							context.add	(((Arc) o).getSource());
//					}
					
				} 
			}
		}
	}
	
	protected OrdinaryMorphism makeInclusionMorphism(
			final Vector<GraphObject> inclusion, 
			final Graph g) {
		Node source = null;
		Node target = null;
		Graph subGraph = BaseFactory.theFactory().createGraph(g.getTypeSet());
		OrdinaryMorphism inclMorph = (BaseFactory.theFactory()).createMorphism(
				subGraph, g);
		for (int i = 0; i < inclusion.size(); i++) {
			GraphObject go = inclusion.elementAt(i);
			if (go.isNode()) {
				Node n = null;
				try {
					n = subGraph.copyNode((Node) go);
					n.setContextUsage(((Node) go).getContextUsage());
				} catch (TypeException e) {
//					System.out.println(e.getStackTrace());
				}
				try {
					inclMorph.addMapping(n, go);
				} catch (BadMappingException e) {
					System.out.println("makeInclusionMorphism::  "+e.getLocalizedMessage());
				}
			}
		}
		for (int i = 0; i < inclusion.size(); i++) {
			GraphObject go = inclusion.elementAt(i);
			if (go.isArc()) {
				Enumeration<GraphObject> sources = inclMorph.getInverseImage(((Arc) go)
						.getSource());
				if (sources.hasMoreElements()) {
					source = (Node) sources.nextElement();
				}
				Enumeration<GraphObject> targets = inclMorph.getInverseImage(((Arc) go)
						.getTarget());
				if (targets.hasMoreElements()) {
					target = (Node) targets.nextElement();
				}
				if (source == null || target == null)
					return null;
				Arc a = null;
				try {
					a = subGraph.copyArc((Arc) go, source, target);
					if (a != null) {
						a.setContextUsage(((Arc) go).getContextUsage());
						try {
							inclMorph.addMapping(a, go);
						} catch (BadMappingException e) {
							System.out.println(e.getLocalizedMessage());
						}
					}
				} catch (TypeException e) {
//					System.out.println(e.getStackTrace());
				}
			}
		}
		if (inclMorph.getSize() != inclMorph.getSource().getSize()) {
			return null;
		}
		return inclMorph;
	}

	/**
	 * Each inner vector of given incls must contain at least one GraphObject from the 
	 * given  neededGOs. Otherwise it is removed from the Vector incls.
	 */
	protected void checkInclusions(
			final Vector<Vector<GraphObject>> incls,
			final Vector<GraphObject> neededGOs) {

		for (int i = 0; i < incls.size(); i++) {
			Vector<GraphObject> v = incls.get(i);
			boolean inclOK = false;
			for (int j = 0; j < v.size(); j++) {
				GraphObject o = v.get(j);
				if (neededGOs.contains(o)) {
					inclOK = true;
					break;
				}
			}
			if (!inclOK) {
				incls.remove(v);
				i--;
			}
		}
		incls.trimToSize();
	}

	/**
	 * Each inner vector of given incls must contain at least one named GraphObject 
	 * (the name of a GraphObject is not null or empty). 
	 * Otherwise it is removed from the Vector incls.
	 */
	protected void checkInclusionsDuetoNamedObject(final Vector<Vector<GraphObject>> incls) {
		for (int i = 0; i < incls.size(); i++) {
			Vector<GraphObject> v = incls.get(i);
			boolean inclOK = false;
			for (int j = 0; j < v.size(); j++) {
				GraphObject o = v.get(j);
				if (//(o instanceof Node)  &&
						!o.getObjectName().isEmpty() ) {
					inclOK = true;
					break;
				}
			}
			if (!inclOK) {
				incls.remove(v);
				i--;
			}
		}
		incls.trimToSize();
	}
	
	protected void addNamedObjectConstraint(final Match m) {
		Iterator<Node> iter1 = m.getSource().getNodesSet().iterator();
		while (iter1.hasNext()) {
			Node go = iter1.next();
			if (!"".equals(go.getObjectName()))
				m.getCompletionStrategy().addObjectNameConstraint(go);
		}
	}
	
	protected void removeNamedObjectConstraint(final Match m) {
		Iterator<Node> iter1 = m.getSource().getNodesSet().iterator();
		while (iter1.hasNext()) {
			Node go = iter1.next();
			if (!"".equals(go.getObjectName()))
				m.getCompletionStrategy().removeObjectNameConstraint(go);
		}
	}

	private Vector<Vector<GraphObject>> checkInclusionsAgainstNac(
			final Vector<Vector<GraphObject>> incls,
			final Vector<GraphObject> nacRestriction) {
		
		Vector<Vector<GraphObject>> result = new Vector<Vector<GraphObject>>();
		for (int i = 0; i < incls.size(); i++) {
			Vector<GraphObject> v = incls.get(i);
			boolean inclOK = false;
			for (int j = 0; j < v.size(); j++) {
				GraphObject o = v.get(j);
				for (int n = 0; n < nacRestriction.size(); n++) {
					GraphObject o_n = nacRestriction.get(n);
					if (o.getType().compareTo(o_n.getType())
							|| o_n.getType().isParentOf(o.getType())
							|| o.getType().isParentOf(o_n.getType())) {
						inclOK = true;
						break;
					}
				}
				if (inclOK)
					break;
			}
			if (inclOK) {
				result.add(v);
			}
		}
		return result;
	}

	protected boolean checkGraphConsistency(final Graph g, int l) {		
		if (this instanceof LayeredExcludePair)
			return ((LayeredExcludePair) this).checkGraphConsistency(g, l);
		
//		boolean res = this.grammar.checkGraphConsistency(g);
//		if (!res) {
//			System.out.println("consistentOverlap: "+this.grammar.getConsistencyErrorMsg());
//		}
//		return res; 
		
		return this.grammar.checkGraphConsistency(g);
	}
	
	/**
	 * Checks the attributes if they are critic.
	 * 
	 * @param r1
	 *            The first rule
	 * @param r2
	 *            The second rule
	 * @param overlapping
	 *            The pair of overlapping morphisms
	 * @return true if r1 excludes r2
	 */
	protected boolean attributeCritical(
			final Rule r1, 
			final Rule r2, 
			final Pair<OrdinaryMorphism, OrdinaryMorphism> overlapping,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> helpPair) {
		
		boolean result = false;
		// global container of rule r1
		if (this.preservedChanged.isEmpty()) {
			return false;
		}
		
		// changed attributes of r1 
		List<GraphObject> changedAttributesR1 = new Vector<GraphObject>(5);
		for (int i = 0; i < this.preservedChanged.size(); i++) {			
			if (ExcludePairHelper.doesRuleChangeAttr(
					r1, this.preservedChanged.elementAt(i))) {
				changedAttributesR1.add(this.preservedChanged.elementAt(i));
			}
		}				
		if (changedAttributesR1.isEmpty()) {
			changedAttributesR1 = null;
			return false;
		}
		
		// forbidden attributes of r2 
		List<Type> forbiddenTypesR2 = ExcludePairHelper.getForbiddenTypesRule2(
												r2,
												this.attrMember2Expr);
				
		// check all changed attributes
		for (int i = 0; i < changedAttributesR1.size() /*&& !result*/; i++) {
			final GraphObject l1Object = changedAttributesR1.get(i);
			final OrdinaryMorphism l1G = overlapping.first;
			final GraphObject overlapObject = l1G.getImage(l1Object);
			final OrdinaryMorphism l2G = overlapping.second;
			Enumeration<GraphObject> l2Objects = l2G.getInverseImage(overlapObject);
			// es werden nur injektiv erzeugte ueberlappungsgraphen betrachtet
			// daher kann es nicht mehrere objekte geben.
			if (l2Objects.hasMoreElements()) {
				final GraphObject l2Object = l2Objects.nextElement();
				Vector<ValueMember> 
				changedMembersR1 = ExcludePairHelper.getChangedAttributeMember(r1, l1Object);
				if (changedMembersR1 != null) {
					for (int j = 0; j < changedMembersR1.size() && !result; j++) {
						/* Members links und rechts pruefen */
						result = ExcludePairHelper.isAttrMemberChangedFromLeftToRight(
											r1, r2,
											changedMembersR1.elementAt(j),
											l1Object,
											l2Object,
											this.nacInsideOverlapGraph,
											overlapObject);
					}
					changedMembersR1 = null;
				}
				if (result) {
					overlapObject.setCritical(true);
				}
			}
			else if (helpPair != null && helpPair.second != null) {
				// when l2Objects are not found inside of LHS2
				// try to find it inside of PAC part of the extended LHS2
				// helpPair: Pair(extendedByPACsL2iso, rStar)
				result = ExcludePairHelper.isAttrMemberChangedFromPACRule2ToRight(
										r1, 
										r2, 
										l1Object,
										overlapObject,
										helpPair);	
				
				if (result) {
					overlapObject.setCritical(true);
				}
			}
		} // for (int i = 0; i < changedAttributesR1.size()		
		
		if (!result && !r2.getNACsList().isEmpty()) {
			
			result = ExcludePairHelper.isAttrMemberChangedFromNACRule2ToRight(
								r1, 
								r2, 
								overlapping,
								this.nacInsideOverlapGraph,
								changedAttributesR1,
								forbiddenTypesR2,
								this.attrMember2Expr);
		}
		changedAttributesR1 = null;
		forbiddenTypesR2 = null;
		return result;
	}
	
/*	
	private void unsetAllTransientAttrValuesOfOverlapGrah(
			final Pair<OrdinaryMorphism, OrdinaryMorphism> overlapPair) {
		// remove all implicit transient declared variables in overlapgraph
		Graph g = overlapPair.second.getImage();
		Iterator<?> e1 = g.getNodesSet().iterator();
		while (e1.hasNext()) {
			GraphObject obj = (GraphObject) e1.next();
			if (obj.getAttribute() == null)
				continue;
			ValueTuple value = (ValueTuple) obj.getAttribute();
			for (int i = 0; i < value.getNumberOfEntries(); i++) {
				ValueMember valuem = value.getValueMemberAt(i);
				if ((valuem.getExpr() != null) && valuem.getExpr().isVariable()
						&& valuem.isTransient()) {
					valuem.setExpr(null);
				}
			}
		}
		e1 = g.getArcsSet().iterator();
		while (e1.hasNext()) {
			GraphObject obj = (GraphObject) e1.next();
			if (obj.getAttribute() == null)
				continue;
			ValueTuple value = (ValueTuple) obj.getAttribute();
			for (int i = 0; i < value.getNumberOfEntries(); i++) {
				ValueMember valuem = value.getValueMemberAt(i);
				if ((valuem.getExpr() != null) && valuem.getExpr().isVariable()
						&& valuem.isTransient()) {
					valuem.setExpr(null);
				}
			}
		}
	}
*/
	
	protected void unsetAllTransientAttrValuesOfOverlapGrah(final Graph g) {
		// remove all implicit transient declared variables in overlapgraph
		Iterator<?> e1 = g.getNodesSet().iterator();
		while (e1.hasNext()) {
			GraphObject obj = (GraphObject) e1.next();
			if (obj.getAttribute() == null)
				continue;
			ValueTuple value = (ValueTuple) obj.getAttribute();
			for (int i = 0; i < value.getNumberOfEntries(); i++) {
				ValueMember valuem = value.getValueMemberAt(i);
				if ((valuem.getExpr() != null) 
						&& valuem.getExpr().isVariable() && valuem.isTransient()) {
					valuem.setExpr(null);
				}
			}
		}
		e1 = g.getArcsSet().iterator();
		while (e1.hasNext()) {
			GraphObject obj = (GraphObject) e1.next();
			if (obj.getAttribute() == null)
				continue;
			ValueTuple value = (ValueTuple) obj.getAttribute();
			for (int i = 0; i < value.getNumberOfEntries(); i++) {
				ValueMember valuem = value.getValueMemberAt(i);
				if ((valuem.getExpr() != null) 
						&& valuem.getExpr().isVariable() && valuem.isTransient()) {
					valuem.setExpr(null);					
				}
			}
		}
	}

	protected void unsetAllTransientAttrValuesOfRule(final Rule r) {
		if (r.getLeft().isAttributed()) {
			Iterator<?> e1 = r.getLeft().getNodesSet().iterator();
			while (e1.hasNext()) {
				GraphObject obj = (GraphObject) e1.next();
				if (obj.getAttribute() == null)
					continue;
				ValueTuple value = (ValueTuple) obj.getAttribute();
				for (int i = 0; i < value.getNumberOfEntries(); i++) {
					ValueMember valuem = value.getValueMemberAt(i);
					if ((valuem.getExpr() != null)
							&& valuem.getExpr().isVariable()
							&& valuem.isTransient()) {
						String tmpname = valuem.getExprAsText();
						valuem.setExpr(null);
						if (((VarTuple) r.getAttrContext().getVariables())
								.getMemberAt(tmpname) != null)
							((VarTuple) r.getAttrContext().getVariables())
									.getTupleType().deleteMemberAt(tmpname);
					}
				}
			}
			e1 = r.getLeft().getArcsSet().iterator();
			while (e1.hasNext()) {
				GraphObject obj = (GraphObject) e1.next();
				if (obj.getAttribute() == null)
					continue;
				ValueTuple value = (ValueTuple) obj.getAttribute();
				for (int i = 0; i < value.getNumberOfEntries(); i++) {
					ValueMember valuem = value.getValueMemberAt(i);
					if ((valuem.getExpr() != null)
							&& valuem.getExpr().isVariable()
							&& valuem.isTransient()) {
						String tmpname = valuem.getExprAsText();
						valuem.setExpr(null);
						if (((VarTuple) r.getAttrContext().getVariables())
								.getMemberAt(tmpname) != null)
							((VarTuple) r.getAttrContext().getVariables())
									.getTupleType().deleteMemberAt(tmpname);
					}
				}
			}
		}
		if (r.getRight().isAttributed()) {
			Iterator<?> e1 = r.getRight().getNodesSet().iterator();
			while (e1.hasNext()) {
				GraphObject obj = (GraphObject) e1.next();
				if (obj.getAttribute() == null)
					continue;
				ValueTuple value = (ValueTuple) obj.getAttribute();
				for (int i = 0; i < value.getNumberOfEntries(); i++) {
					ValueMember valuem = value.getValueMemberAt(i);
					if ((valuem.getExpr() != null)
							&& valuem.getExpr().isVariable()
							&& valuem.isTransient()) {
						String tmpname = valuem.getExprAsText();
						valuem.setExpr(null);
						if (((VarTuple) r.getAttrContext().getVariables())
								.getMemberAt(tmpname) != null)
							((VarTuple) r.getAttrContext().getVariables())
									.getTupleType().deleteMemberAt(tmpname);
					}
				}
			}
			e1 = r.getRight().getArcsSet().iterator();
			while (e1.hasNext()) {
				GraphObject obj = (GraphObject) e1.next();
				if (obj.getAttribute() == null)
					continue;
				ValueTuple value = (ValueTuple) obj.getAttribute();
				for (int i = 0; i < value.getNumberOfEntries(); i++) {
					ValueMember valuem = value.getValueMemberAt(i);
					if ((valuem.getExpr() != null)
							&& valuem.getExpr().isVariable()
							&& valuem.isTransient()) {
						String tmpname = valuem.getExprAsText();
						valuem.setExpr(null);
						if (((VarTuple) r.getAttrContext().getVariables())
								.getMemberAt(tmpname) != null)
							((VarTuple) r.getAttrContext().getVariables())
									.getTupleType().deleteMemberAt(tmpname);
					}
				}
			}
		}
		final List<OrdinaryMorphism> nacs = r.getNACsList();
		for (int l=0; l<nacs.size(); l++) {
			final OrdinaryMorphism nac = nacs.get(l);		
			if (nac.getTarget().isAttributed()) {
				Iterator<?> e1 = nac.getTarget().getNodesSet().iterator();
				while (e1.hasNext()) {
					GraphObject obj = (GraphObject) e1.next();
					if (obj.getAttribute() == null)
						continue;
					ValueTuple value = (ValueTuple) obj.getAttribute();
					for (int i = 0; i < value.getNumberOfEntries(); i++) {
						ValueMember valuem = value.getValueMemberAt(i);
						if ((valuem.getExpr() != null)
								&& valuem.getExpr().isVariable()
								&& valuem.isTransient()) {
							String tmpname = valuem.getExprAsText();
							valuem.setExpr(null);
							if (((VarTuple) r.getAttrContext().getVariables())
									.getMemberAt(tmpname) != null)
								((VarTuple) r.getAttrContext().getVariables())
										.getTupleType().deleteMemberAt(tmpname);
						}
					}
				}
				e1 = nac.getTarget().getArcsSet().iterator();
				while (e1.hasNext()) {
					GraphObject obj = (GraphObject) e1.next();
					if (obj.getAttribute() == null)
						continue;
					ValueTuple value = (ValueTuple) obj.getAttribute();
					for (int i = 0; i < value.getNumberOfEntries(); i++) {
						ValueMember valuem = value.getValueMemberAt(i);
						if ((valuem.getExpr() != null)
								&& valuem.getExpr().isVariable()
								&& valuem.isTransient()) {
							String tmpname = valuem.getExprAsText();
							valuem.setExpr(null);
							if (((VarTuple) r.getAttrContext().getVariables())
									.getMemberAt(tmpname) != null)
								((VarTuple) r.getAttrContext().getVariables())
										.getTupleType().deleteMemberAt(tmpname);
						}
					}
				}
			}
		}
		
		final List<OrdinaryMorphism> pacs = r.getPACsList();
		for (int l=0; l<pacs.size(); l++) {
			final OrdinaryMorphism pac = pacs.get(l);		
			if (pac.getTarget().isAttributed()) {
				Iterator<?> e1 = pac.getTarget().getNodesSet().iterator();
				while (e1.hasNext()) {
					GraphObject obj = (GraphObject) e1.next();
					if (obj.getAttribute() == null)
						continue;
					ValueTuple value = (ValueTuple) obj.getAttribute();
					for (int i = 0; i < value.getNumberOfEntries(); i++) {
						ValueMember valuem = value.getValueMemberAt(i);
						if ((valuem.getExpr() != null)
								&& valuem.getExpr().isVariable()
								&& valuem.isTransient()) {
							String tmpname = valuem.getExprAsText();
							valuem.setExpr(null);
							if (((VarTuple) r.getAttrContext().getVariables())
									.getMemberAt(tmpname) != null)
								((VarTuple) r.getAttrContext().getVariables())
										.getTupleType().deleteMemberAt(tmpname);
						}
					}
				}
				e1 = pac.getTarget().getArcsSet().iterator();
				while (e1.hasNext()) {
					GraphObject obj = (GraphObject) e1.next();
					if (obj.getAttribute() == null)
						continue;
					ValueTuple value = (ValueTuple) obj.getAttribute();
					for (int i = 0; i < value.getNumberOfEntries(); i++) {
						ValueMember valuem = value.getValueMemberAt(i);
						if ((valuem.getExpr() != null)
								&& valuem.getExpr().isVariable()
								&& valuem.isTransient()) {
							String tmpname = valuem.getExprAsText();
							valuem.setExpr(null);
							if (((VarTuple) r.getAttrContext().getVariables())
									.getMemberAt(tmpname) != null)
								((VarTuple) r.getAttrContext().getVariables())
										.getTupleType().deleteMemberAt(tmpname);
						}
					}
				}
			}
		}
	}

	/*
	private void reduceIfSimilar(
			final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
			overlappings) {
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
		vec = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>(overlappings.size());
		vec.addAll(overlappings);
		while (!vec.isEmpty()) {
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p0 = vec.get(0);
			for (int i=0; i<overlappings.size(); i++) {
				Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> pi = overlappings.get(i);
				if (pi != p0) {
					Pair<OrdinaryMorphism, OrdinaryMorphism> p = checkIfSimilar(p0.first, pi.first);
					if (p != null) {
						overlappings.remove(pi);
						i--;
					}
				}			
			}
			vec.remove(p0);
		}
	}
	
	// not used now
	
	protected boolean reduce;
	
	private Pair<OrdinaryMorphism, OrdinaryMorphism> checkIfSimilar(
			final Pair<OrdinaryMorphism, OrdinaryMorphism> p1, 
			final Pair<OrdinaryMorphism, OrdinaryMorphism> p2) {
		
		Pair<OrdinaryMorphism, OrdinaryMorphism> p = null;
		OrdinaryMorphism first1 = null;
		OrdinaryMorphism second1 = null;
		OrdinaryMorphism first2 = null;
		OrdinaryMorphism second2 = null;
		OrdinaryMorphism morph1 = null;
		OrdinaryMorphism morph2 = null;
		Graph overlap1 = p1.first.getImage();
		int gSize1 = overlap1.getSize();

		int mSize1 = 0;
		Iterator<?> e = overlap1.getNodesSet().iterator();
		while (e.hasNext() && !this.stop) {
			GraphObject o = (GraphObject) e.next();
			if (p1.first.getInverseImage(o).hasMoreElements()
					|| p1.second.getInverseImage(o).hasMoreElements())
				mSize1++;
		}
		e = overlap1.getArcsSet().iterator();
		while (e.hasNext() && !this.stop) {
			GraphObject o = (GraphObject) e.next();
			if (p1.first.getInverseImage(o).hasMoreElements()
					|| p1.second.getInverseImage(o).hasMoreElements())
				mSize1++;
		}

		Graph overlap2 = p2.first.getImage();
		int gSize2 = overlap2.getSize();

		int mSize2 = 0;
		e = overlap2.getNodesSet().iterator();
		while (e.hasNext() && !this.stop) {
			GraphObject o = (GraphObject) e.next();
			if (p2.first.getInverseImage(o).hasMoreElements()
					|| p2.second.getInverseImage(o).hasMoreElements())
				mSize2++;
		}
		e = overlap2.getArcsSet().iterator();
		while (e.hasNext() && !this.stop) {
			GraphObject o = (GraphObject) e.next();
			if (p2.first.getInverseImage(o).hasMoreElements()
					|| p2.second.getInverseImage(o).hasMoreElements())
				mSize2++;
		}

		if (mSize1 != mSize2)
			return null;
		if ((!reduce && (gSize1 == gSize2)) || reduce) {
			if (gSize1 < gSize2) {
				morph1 = (BaseFactory.theFactory()).createMorphism(overlap1,
						overlap2);
				morph2 = (BaseFactory.theFactory()).createMorphism(overlap1,
						overlap2);
				first1 = p1.first;
				first2 = p2.first;
				second1 = p1.second;
				second2 = p2.second;
				if (morph1.makeDiagram(first1, first2)) {
					// System.out.println("1:makeDiagram: "+morph1);
					if (morph2.makeDiagram(second1, second2)) {
						if (morph1.isPartialIsomorphicTo(morph2)) {
							p = p2;
						}
					}
				}
			} else {
				morph1 = (BaseFactory.theFactory()).createMorphism(overlap2,
						overlap1);
				morph2 = (BaseFactory.theFactory()).createMorphism(overlap2,
						overlap1);
				first2 = p2.first;
				first1 = p1.first;
				second2 = p2.second;
				second1 = p1.second;
				if (morph1.makeDiagram(first2, first1)) {
					if (morph2.makeDiagram(second2, second1)) {
						if (morph1.isPartialIsomorphicTo(morph2)) {
							p = p1;
						}
					}
				}
			}
		}
		if (morph1 != null)
			morph1.dispose();
		morph1 = null;
		if (morph2 != null)
			morph2.dispose();
		morph2 = null;
		return p;
	}
	
	private Pair<OrdinaryMorphism, OrdinaryMorphism> getValidMatchChangeAttr(
			final Rule r1, 
			final Rule r2, 
			final Pair<OrdinaryMorphism, OrdinaryMorphism> overlapping) {
//		System.out.println("### getValidMatch1Match2ChangeAttr(Rule,Rule, ...");
		OrdinaryMorphism morph1 = overlapping.first;
		((ContextView) morph1.getAttrContext()).setVariableContext(true);
		Match m1 = BaseFactory.theFactory().makeMatch(r1, morph1);
		if (m1 == null)
			return null;
		
//		System.out.println(m1.getSource());
//		System.out.println(m1.getTarget());
		
		m1.setCompletionStrategy(this.strategy, true);
		boolean isValid = isChangeAttrMatchValid(r1, m1, null, true, overlapping);
		if (!isValid) {
			m1.dispose();
			m1 = null;
			return null;
		}

		OrdinaryMorphism morph2 = overlapping.second;
		((ContextView) morph2.getAttrContext()).setVariableContext(true);
		Match m2 = BaseFactory.theFactory().makeMatch(r2, morph2);		
		if (m2 == null) {
			m1.dispose();
			m1 = null;
			return null;
		}
		m2.setCompletionStrategy(this.strategy, true);
		isValid = isChangeAttrMatchValid(r2, m2, null, false, overlapping);
		if (!isValid) {
			m1.dispose();
			m1 = null;
			m2.dispose();
			m2 = null;
			return null;
		}
		
		if (this.reduceSameMatch)
			if (isSameRuleAndSameMatch(r1, r2, m1, m2)) {
				m1.dispose();
				m1 = null;
				m2.dispose();
				m2 = null;
				return null;
			}
		
		return new Pair<OrdinaryMorphism, OrdinaryMorphism>(m1, m2);
	}
*/

	private Pair<OrdinaryMorphism, OrdinaryMorphism> getValidMatchChangeAttr(
			final Rule r1, 
			final Rule r2,
			final OrdinaryMorphism isoL2Ext,
			final OrdinaryMorphism overlap1,
			final OrdinaryMorphism overlap2) {

		OrdinaryMorphism mo1 = overlap1;
		((ContextView) mo1.getAttrContext()).setVariableContext(true);
		Match m1 = BaseFactory.theFactory().makeMatch(r1, mo1);
		if (m1 == null)
			return null;
		
		m1.setCompletionStrategy(this.strategy, true);
		boolean isValid = isChangeAttrMatchValid(r1, r2, m1, null, null, true, overlap1, overlap2);
		if (!isValid) {
			m1.dispose(); m1 = null;
			return null;
		}

		// make match of rule2
		OrdinaryMorphism mo2 = BaseFactory.theFactory().createMorphism(
									isoL2Ext.getSource(), overlap2.getTarget());
		mo2.doCompose(isoL2Ext, overlap2);
		((ContextView) mo2.getAttrContext()).setVariableContext(true);
		Match m2 = BaseFactory.theFactory().makeMatch(r2, mo2);
		if (m2 == null) {
			m1.dispose(); m1 = null;
			mo2.dispose(); mo2 = null;
			return null;
		}
		
		m2.setCompletionStrategy(this.strategy, true);
		isValid = isChangeAttrMatchValid(r1, r2, m2, null, null, false, overlap1, overlap2);
		if (!isValid) {
			mo2.dispose(); mo2 = null;
			m1.dispose(); m1 = null;
			m2.dispose(); m2 = null;
			return null;
		}
		
		// additionally: if rule1 has PACs and rule2 has NACs, 
		// extend the graph overlapping.first.getTarget() by PACs of rule1
		// and check NACs of rule2
		if (r1hasEnabledPACs && r2hasEnabledNACs) {
			OrdinaryMorphism extOverlap = extendOverlapGraphByPACs(overlap1, r1);
			if (extOverlap != null) {
				OrdinaryMorphism mo2p = BaseFactory.theFactory().createMorphism(m2.getSource(), extOverlap.getTarget());
				mo2p.doCompose(m2, extOverlap);
				Match m2p = BaseFactory.theFactory().makeMatch(r2, mo2p);
				if (m2p != null) {
					isValid = checkNACs(r2, m2p, null, false);
					m2p.dispose(); m2p = null;
					mo2p.dispose(); mo2p = null;
					extOverlap.dispose(); extOverlap = null;
					if (!isValid) {						
						m1.dispose(); m1 = null;
						m2.dispose(); m2 = null;						
						return null;
					}
				}
			}
		}
		
		if (this.reduceSameMatch) {
			if (isSameRuleAndSameMatch(r1, r2, m1, m2)) {
				m1.dispose(); m1 = null;
				m2.dispose(); m2 = null;
				return null;
			}
		}
		
		if (this.directStrctCnfl || this.directStrctCnflUpToIso) {
			Pair<OrdinaryMorphism,OrdinaryMorphism> pair1 = BaseFactory.theFactory().makePO(r1, m1, true, false);
			Pair<OrdinaryMorphism,OrdinaryMorphism> pair2 = BaseFactory.theFactory().makePO(r2, m2, true, false);
			if (pair1 != null && pair2 != null) {
				OrdinaryMorphism po1 = pair1.first;
				OrdinaryMorphism po2 = pair2.first;	
				if (isDirectlyStrictConfluent(po1, po2)
						// test
						|| (this.directStrctCnflUpToIso
								&& isIsomorphicTo(pair1.second.getImage(), pair2.second.getImage())) ) {							
					po1.dispose();
					po2.dispose();
					m1.dispose(); m1 = null;			
					m2.dispose(); m2 = null;
					return null;
				}	
			}
		}
		
		return new Pair<OrdinaryMorphism, OrdinaryMorphism>(m1, m2);
	}

	private boolean isIsomorphicTo(Graph g1, Graph g2) {
		if (g1 == g2) {
			return true; 
		}
		Vector<OrdinaryMorphism> list = g1.getIsomorphicWith(g2, true);
		if (list != null && list.size() > 0) {
			for (int i=0; i<list.size(); i++) {
				OrdinaryMorphism h = list.get(i);
				if (h != null && h.checkConstants()) {
					return true;
				}
			}
		}
		return false;
	}
	
	private Pair<OrdinaryMorphism, OrdinaryMorphism> getValidMatchDeleteUse(
			final Rule r1,
			final Rule r2, 
			final OrdinaryMorphism isoL2ext,
			final OrdinaryMorphism overlap1,
			final OrdinaryMorphism overlap2) {

		if (this instanceof DependencyPair
				&& ExcludePairHelper.node2ToNode1_ChildToParentMap(r1, overlap1, overlap2))
			return null;
		
		OrdinaryMorphism mo1 = overlap1;
		((ContextView) mo1.getAttrContext()).setVariableContext(true);
		Match m1 = BaseFactory.theFactory().makeMatch(r1, mo1);
		if (m1 == null)
			return null;
		
		if (this instanceof DependencyPair)
			m1.setCompletionStrategy(new Completion_InjCSP(), true);
		else
			m1.setCompletionStrategy(this.strategy, true);
		
		
		boolean isValid = isDeleteUseMatchValid(r1, m1, null, true, overlap1, overlap2);
		if (!isValid) {
			m1.dispose();
			m1 = null;
			return null;
		}
		
		// prepare morphisms for match of rule2
		OrdinaryMorphism mo2t = BaseFactory.theFactory().createMorphism(
						isoL2ext.getSource(), overlap2.getTarget());
		mo2t.doCompose(isoL2ext, overlap2);
				
		if (this.cpdKind == CriticalPairData.PRODUCE_EDGE_DELTE_NODE_CONFLICT) {
			// for this check the rules are switched: the first rule is now the second and vice versa
			// additionally: if rule2 has PACs and rule1 has NACs, 
			// extend the graph overlapping.second.getTarget() by PACs
			// and check NACs of rule1
			if (r1hasEnabledNACs && r2hasEnabledPACs
					) {
				OrdinaryMorphism extOverlap = extendOverlapGraphByPACs(mo2t, r2);
				if (extOverlap != null) {
					OrdinaryMorphism mo1p = BaseFactory.theFactory().createMorphism(m1.getSource(), extOverlap.getTarget());
					mo1p.doCompose(m1, extOverlap);
					Match m1p = BaseFactory.theFactory().makeMatch(r1, mo1p);
					if (m1p != null) {
						isValid = checkNACs(r1, m1p, null, true);
						if (!isValid) {
							m1p.dispose(); m1p = null;
							mo1p.dispose(); mo1p = null;
							extOverlap.dispose(); extOverlap = null;
							mo2t.dispose(); mo2t = null;
							m1.dispose(); m1 = null;
							return null;
						}
						m1p.dispose(); m1p = null;
						mo1p.dispose(); mo1p = null;
						extOverlap.dispose(); extOverlap = null;
					}
				}
			}
		}
		
		// make match of rule2
		((ContextView) mo2t.getAttrContext()).setVariableContext(true);
		Match m2 = BaseFactory.theFactory().makeMatch(r2, mo2t);
		if (m2 == null) {
			m1.dispose(); m1 = null;
			mo2t.dispose(); mo2t = null;
			return null;
		}
		
		m2.setCompletionStrategy(this.strategy, true);
		isValid = isDeleteUseMatchValid(r2, m2, null, false, overlap1, overlap2);		
		if (!isValid) {
			m1.dispose(); m1 = null;
			mo2t.dispose();
			m2.dispose(); m2 = null;
			return null;
		}
		if (this.cpdKind != CriticalPairData.PRODUCE_EDGE_DELTE_NODE_CONFLICT) {
			// additionally: if rule1 has PACs and rule2 has NACs, 
			// extend the graph overlapping.first.getTarget() by PACs
			// and check NACs of rule2
			if (r1hasEnabledPACs && r2hasEnabledNACs) {
				OrdinaryMorphism extOverlap = extendOverlapGraphByPACs(overlap1, r1);
				if (extOverlap != null) {
					OrdinaryMorphism mo2p = BaseFactory.theFactory().createMorphism(m2.getSource(), extOverlap.getTarget());
					mo2p.doCompose(m2, extOverlap);
					Match m2p = BaseFactory.theFactory().makeMatch(r2, mo2p);
					if (m2p != null) {
						isValid = checkNACs(r2, m2p, null, false);
						m2p.dispose(); m2p = null;
						mo2p.dispose(); mo2p = null;
						extOverlap.dispose(); extOverlap = null;
						if (!isValid) {
							m1.dispose(); m1 = null;
							mo2t.dispose(); mo2t = null;
							m2.dispose(); m2 = null;						
							return null;
						}
					}
				}
			}
		}
				
		if (this.reduceSameMatch
				&& isSameRuleAndSameMatch(r1, r2, m1, m2)) {
			m1.dispose(); m1 = null;
			mo2t.dispose();
			m2.dispose(); m2 = null;
			return null;
		}
		
		if (this.directStrctCnfl || this.directStrctCnflUpToIso) {
			Pair<OrdinaryMorphism,OrdinaryMorphism> pair1 = BaseFactory.theFactory().makePO(r1, m1, true, false);
			Pair<OrdinaryMorphism,OrdinaryMorphism> pair2 = BaseFactory.theFactory().makePO(r2, m2, true, false);
			if (pair1 != null && pair2 != null) {
				OrdinaryMorphism po1 = pair1.first;
				OrdinaryMorphism po2 = pair2.first;	
				if (isDirectlyStrictConfluent(po1, po2)
						// test
						|| (this.directStrctCnflUpToIso
								&& isIsomorphicTo(pair1.second.getImage(), pair2.second.getImage())) ) {							
					po1.dispose();
					po2.dispose();
					m1.dispose(); m1 = null;
					m2.dispose(); m2 = null;
					return null;
				}	
			}
		}
		
		return new Pair<OrdinaryMorphism, OrdinaryMorphism>(m1, m2);
	}

	private boolean isSameRuleAndSameMatch(
			final Rule r1, 
			final Rule r2, 
			final Match m1, 
			final Match m2) {
		
		if (r1 != r2)
			return false;
		if (m1.getTarget() != m2.getTarget()){
			return false;
		}
	
		int nn = 0;
		Enumeration<GraphObject> dom1 = m1.getDomain();
		while (dom1.hasMoreElements()) {
			GraphObject img1 = m1.getImage(dom1.nextElement());
			if (m2.getInverseImage(img1).hasMoreElements())
				nn++;
		}
		if	(nn == m2.getSize())
			return true;
					
		return false;
	}
	
	private Pair<OrdinaryMorphism, OrdinaryMorphism> getValidMatchProduceForbid(
			final Rule r1,
			final Rule r2, 
			final OrdinaryMorphism nac2,
			final OrdinaryMorphism overlap1,
			final OrdinaryMorphism overlap2, 
			final OrdinaryMorphism isoL2ext) {

		if (!(this instanceof DependencyPair) && !ExcludePairHelper.newNode_TypeToTypeMap(r1, overlap1))
			return null;
		
		// make inverse rule1
		Pair<Pair<Rule,Boolean>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		inverseR1pair = BaseFactory.theFactory().reverseRule(r1);
		if (inverseR1pair == null)
			return null;
		
		OrdinaryMorphism isoE = overlap1.getTarget().isomorphicCopy();
		if (isoE == null)
			return null;
		
		Rule inverseR1 = inverseR1pair.first.first;		
		OrdinaryMorphism isoLHS1 = inverseR1pair.second.first;
		OrdinaryMorphism isoRHS1 = inverseR1pair.second.second;
		
		OrdinaryMorphism mo1p = overlap1.completeDiagram(isoRHS1);
		// make match of inverse rule1
		OrdinaryMorphism mo1pt = mo1p.compose(isoE);
		((ContextView) mo1pt.getAttrContext()).setVariableContext(true);

		Match m1pt = BaseFactory.theFactory().makeMatch(inverseR1, mo1pt, "");
		boolean isValid = false;
		if (m1pt != null) {
			m1pt.setCompletionStrategy(new Completion_InjCSP(), true);
			isValid = isInverseMatchValid(true, m1pt);
			if (!isValid) {
				m1pt.dispose(); m1pt = null;
				return null;
			}
		} else {
			return null;
		}
		// apply inverse r1 at extended overlapGraph
		OrdinaryMorphism ms1 = null;
		try {
			ms1 = (OrdinaryMorphism) TestStep.execute(m1pt, true, this.equalVariableNameOfAttrMapping);
		} catch (TypeException e) {}
		if (ms1 == null) {
			m1pt.dispose(); m1pt = null;
			mo1pt.dispose(); mo1pt = null;
			return null;
		}

		// make match m1 : L1 -> overlapGraph
		OrdinaryMorphism mo1t = isoLHS1.compose(ms1);
//		ms1.dispose(); ms1 = null;
		
		Match m1t = BaseFactory.theFactory().makeMatch(r1, mo1t, "");
		if (m1t == null) {
			m1pt.dispose(); m1pt = null;
			mo1pt.dispose(); mo1pt = null;
			mo1t.dispose(); mo1t = null;
			return null;
		}

		Pair<OrdinaryMorphism, OrdinaryMorphism> 
		overlapping = new Pair<OrdinaryMorphism, OrdinaryMorphism> (overlap1, overlap2);
		
		isValid = isProduceForbidMatchValid(r1, m1t, null, true, overlapping);
		if (!isValid) {
			m1pt.dispose(); m1pt = null;
			mo1pt.dispose(); mo1pt = null;
			mo1t.dispose(); mo1t = null;
			m1t.dispose(); m1t = null;
			return null;
		}
			
		final Hashtable<GraphObject,GraphObject> adjusted = new Hashtable<GraphObject,GraphObject>();
		final OrdinaryMorphism mo1 = BaseFactory.theFactory().createMorphism(
				m1t.getSource(), isoE.getSource());
		final Enumeration<GraphObject> e = m1t.getDomain();
		while (e.hasMoreElements()) {
			final GraphObject o = e.nextElement();
			final GraphObject i = m1t.getImage(o);
			if (isoE.getInverseImage(i).hasMoreElements()) {
				final GraphObject img = isoE.getInverseImage(i).nextElement();
				if (adjustAttrOfObjFromObjIfConstValue(img, i)) {
					adjusted.put(img, i);
				}
				try {
					mo1.addMapping(o, img);
				} catch (BadMappingException ex) {}
			}
		}

		Match m1 = BaseFactory.theFactory().makeMatch(r1, mo1, "");
		if (m1 == null) {
			return null;
		}
		
		OrdinaryMorphism po1 = isoE.invert();
			
		final Enumeration<GraphObject> keys = adjusted.keys();
		while (keys.hasMoreElements()) {
			final GraphObject o1 = keys.nextElement();
			final GraphObject o2 = adjusted.get(o1);
			adjustAttrOfObjFromObjIfConstValue(o2, o1);
		}
		
		// make match of rule2
		OrdinaryMorphism mo2pt = overlap2.compose(isoE);
		OrdinaryMorphism mo2t = isoL2ext.compose(mo2pt);

		Match m2t = BaseFactory.theFactory().makeMatch(r2, mo2t, "");
		if (m2t != null) {
			// to check m2 is needed because of other NACs
			m2t.setCompletionStrategy(this.strategy, true);
			isValid = isProduceForbidMatchValid(r2, m2t, nac2, false, overlapping);
		}
		else {
			m1t.dispose();
			mo1.dispose();
			m1.dispose(); m1 = null;
			mo2t.dispose(); mo2t = null;
			mo2pt.dispose(); mo2pt = null;
			return null;
		}
		if (!isValid) {
			m1t.dispose();
			mo1.dispose();
			m1.dispose(); m1 = null;
			m2t.dispose(); m2t = null;
			mo2t.dispose(); mo2t = null;
			mo2pt.dispose(); mo2pt = null;
			return null;
		}
		 
		OrdinaryMorphism mo2 = BaseFactory.theFactory()
					.createMorphism(m2t.getSource(), isoE.getSource());
		Enumeration<GraphObject> e2 = m2t.getDomain();
		while (e2.hasMoreElements()) {
			GraphObject o = e2.nextElement();
			GraphObject i = m2t.getImage(o);			
			if (isoE.getInverseImage(i).hasMoreElements()) {
				try {
				mo2.addMapping(o, isoE.getInverseImage(i)
						.nextElement());
				} catch (BadMappingException ex) {}
			}
		}
		Match m2 = BaseFactory.theFactory().makeMatch(r2, mo2, "");
		if (m2 == null) {
			m1t.dispose();
			mo1.dispose();
			m1.dispose(); m1 = null;
			m2t.dispose();
			mo2.dispose(); mo2 = null;			
			return null;
		}
		
		// additionally: if rule1 has PACs and rule2 has NACs, 
		// extend the graph overlapping.first.getTarget() by PACs
		// and check NACs of rule2
		if (r1hasEnabledPACs && r2hasEnabledNACs) {
			OrdinaryMorphism extOverlap = extendOverlapGraphByPACs(overlapping.first, r1);
			if (extOverlap != null) {
				OrdinaryMorphism mo2p = BaseFactory.theFactory()
						.createMorphism(m2.getSource(), extOverlap.getTarget());
				mo2p.doCompose(m2, extOverlap);
				Match m2p = BaseFactory.theFactory().makeMatch(r2, mo2p);
				if (m2p != null) {
					isValid = checkNACs(r2, m2p, null, false);
					if (!isValid) {
						m2p.dispose(); m2p = null;
						mo2p.dispose(); mo2p = null;
						extOverlap.dispose(); extOverlap = null;
						m1.dispose(); m1 = null;
						mo2t.dispose(); mo2t = null;
						m2.dispose(); m2 = null;						
						return null;
					}
					m2p.dispose(); m2p = null;
					mo2p.dispose(); mo2p = null;
					extOverlap.dispose(); extOverlap = null;
				}
			}
		}
		
		if (this.reduceSameMatch
				&& isSameRuleAndSameMatch(r1, r2, m1, m2)) {
			m1t.dispose();
			mo1.dispose();
			m1.dispose(); m1 = null;
			m2t.dispose();
			mo2.dispose();				
			m2.dispose(); m2 = null;
			return null;
		}
		
		if (this.directStrctCnfl
				&& !r1.isDeleting() && !r2.isDeleting()) {
			Pair<OrdinaryMorphism,OrdinaryMorphism> pair2 = BaseFactory.theFactory().makePO(m2t.getRule(), m2t, true, false);
			if (pair2 != null) {
				OrdinaryMorphism po2 = pair2.first;					
				if (isDirectlyStrictConfluent(po1, po2)) {							
					po2.dispose();
					ms1.dispose();
					m1t.dispose();
					mo1.dispose();
					m1.dispose(); m1 = null;
					m2t.dispose();
					mo2.dispose();				
					m2.dispose(); m2 = null;
					return null;
				}	
			}
		}
		
		return new Pair<OrdinaryMorphism, OrdinaryMorphism>(m1, m2);
	}

	private boolean isDirectlyStrictConfluent(OrdinaryMorphism m1, OrdinaryMorphism m2) {
		if (m1.getOriginal() == m2.getOriginal()) {
			OrdinaryMorphism iso = new OrdinaryMorphism(m1.getImage(), m2.getImage(),
									m1.getAttrManager().newContext(AttrMapping.MATCH_MAP));
			((ContextView) iso.getAttrContext()).setVariableContext(true);
						
			Iterator<Node> nodes = m1.getOriginal().getNodesCollection().iterator();
			while (nodes.hasNext()) {
				GraphObject go = nodes.next();
				GraphObject img1 = m1.getImage(go);
				GraphObject img2 = m2.getImage(go);
				if (img1 == null && img2 == null)
					continue;
				try {
					iso.addMapping(img1, img2);
				} catch(BadMappingException ex) {
					return false;
				}
			}
			Iterator<Arc> arcs = m1.getOriginal().getArcsCollection().iterator();
			while (arcs.hasNext()) {
				GraphObject go = arcs.next();
				GraphObject img1 = m1.getImage(go);
				GraphObject img2 = m2.getImage(go);
				if (img1 == null && img2 == null)
					continue;
				try {
					iso.addMapping(img1, img2);
				} catch(BadMappingException ex) {
					return false;
				}
			}
			if (!iso.checkConstants()) {
				iso.dispose();
				return false;
			}
//			if (iso.makeIsomorphic() && m1.isCommutative(m2, iso)) {
			if (isIsoAndCommutative(iso, m1, m2)) {
				return true;
			}					
		}
		return false;
	}
	
	public boolean isIsoAndCommutative(
			final OrdinaryMorphism iso,
			final OrdinaryMorphism m1, 
			final OrdinaryMorphism m2) {
		// vergleiche Knotenanzahl 
		if (iso.getOriginal().getNodesCount() != iso.getImage().getNodesCount()) {
			return false;
		}
		// vergleiche Kantenanzahl
		if (iso.getOriginal().getArcsCount() != iso.getOriginal().getArcsCount()) {
			return false;
		}
		
		if (iso.isTotal() && m1.isCommutative(m2, iso)) {
				return true;
		}
		
		boolean result = false;
		iso.setCompletionStrategy(new Completion_InjCSP());
		
		while (iso.nextCompletion()) {
			result = true;
			
			// additionally, check type of source - target nodes in case of
			// Typegraph with Node Type Inheritance
			if (iso.getOriginal().getTypeSet().getTypeGraph() != null
					&& iso.getOriginal().getTypeSet().hasInheritance()) {
				Iterator<Node> origs = iso.getOriginal().getNodesCollection().iterator();
				while (origs.hasNext() && result) {
					final Node orig = origs.next();
					if (!orig.getType().compareTo(iso.getImage(orig).getType())) {
						result = false;
					}
				}
			}
			
			if (result && m1.isCommutative(m2, iso)) {
				return true;
			}
		}
		return true;
	}
	
	
	private boolean adjustAttrOfObjFromObjIfConstValue(
			final GraphObject src, 
			final GraphObject tar) {
		boolean result = false;
		if (src.getAttribute() != null) {				
			final ValueTuple srcValue = (ValueTuple) src.getAttribute();
			final ValueTuple tarValue = (ValueTuple) tar.getAttribute();
			for (int i = 0; i < srcValue.getNumberOfEntries(); i++) {
				final ValueMember srcMem = srcValue.getValueMemberAt(i);
				final ValueMember tarMem = tarValue.getValueMemberAt(i);
				if (tarMem != null &&srcMem.isSet() && tarMem.isSet()
						&& srcMem.getExpr().isConstant() && tarMem.getExpr().isConstant()
						&& !srcMem.getExprAsText().equals(tarMem.getExprAsText())) {
					// TODO: check this attr value setting
					HandlerExpr srcexpr = srcMem.getExpr().getCopy();
					HandlerExpr tarexpr = tarMem.getExpr().getCopy();
					srcMem.setExprAsObject(tarexpr);
					tarMem.setExprAsObject(srcexpr);
					result = true;
				}							
			}
		}
		return result;
	}
	
	private Pair<OrdinaryMorphism, OrdinaryMorphism> getValidMatchChangeAttr(
			final Rule r1,
			final Rule r2,
			final OrdinaryMorphism nac,
			final OrdinaryMorphism isoL2ext,
			final OrdinaryMorphism isoNAC2ext,
			final OrdinaryMorphism overlap1,
			final OrdinaryMorphism overlap2) {
				
		// make match of rule1
		Match m1 = BaseFactory.theFactory().makeMatch(r1, overlap1);
		if (m1 != null) {
			m1.setCompletionStrategy(this.strategy, true);
			boolean isValid = isChangeAttrMatchValid(r1, r2, m1, null, null, true, overlap1, overlap2);
			if (!isValid) {
				m1.dispose(); m1 = null;
				return null;
			}
		}
		else return null;

		// make match of rule2
		OrdinaryMorphism mo2 = isoL2ext.compose(overlap2);
		Match m2 = BaseFactory.theFactory().makeMatch(r2, mo2);
		// to check m2 is needed because of the NACs
		if (m2 != null) {
			m2.setCompletionStrategy(this.strategy, true); 	
			boolean isValid = isChangeAttrMatchValid(r1, r2, m2, nac, isoNAC2ext, false, overlap1, overlap2);
			if (!isValid) {
				m1.dispose(); m1 = null;
				m2.dispose(); m2 = null;
				return null;
			}
		}
		else {
			m1.dispose(); m1 = null;
			mo2.dispose(); mo2 = null;
			return null;
		}
		
		// additional test: make step of rule1 because of the critical NAC of rule2
		if (nac.getTextualComment().equals(String.valueOf(CriticalPairData.CHANGE_FORBID_ATTR_CONFLICT))) {
			OrdinaryMorphism t1 = overlap1.getTarget().isomorphicCopy();
			OrdinaryMorphism tom1 = overlap1.compose(t1);
			Match tm1 = BaseFactory.theFactory().makeMatch(r1, tom1);		
			OrdinaryMorphism ms1 = null;
			try {
				ms1 = (OrdinaryMorphism) TestStep.execute(tm1, true);
			} catch (TypeException e) {}
			if (ms1 == null) {
				tm1.dispose(); tm1 = null;
				tom1.dispose(); tom1 = null;
				t1.dispose(); t1 = null;
				return null;
			}
			// make test match of rule2 because of its NACs
			if (!checkNac2afterStep1(t1, r2, nac, isoL2ext, overlap2)) {
				m1.dispose(); m1 = null;
				m2.dispose(); m2 = null;
				return null;
			}					
		}
		
		// additionally: if rule1 has PACs and rule2 has NACs, 
		// extend the graph overlapping.first.getTarget() by PACs of rule1
		// and check NACs of rule2
		if (r1hasEnabledPACs && r2hasEnabledNACs) {
			OrdinaryMorphism extOverlap = extendOverlapGraphByPACs(overlap1, r1);
			if (extOverlap != null) {
				OrdinaryMorphism mo2p = BaseFactory.theFactory().createMorphism(m2.getSource(), extOverlap.getTarget());
				mo2p.doCompose(m2, extOverlap);
				Match m2p = BaseFactory.theFactory().makeMatch(r2, mo2p);
				if (m2p != null) {
					boolean isValid = checkNACs(r2, m2p, null, false);
					m2p.dispose(); m2p = null;
					mo2p.dispose(); mo2p = null;
					extOverlap.dispose(); extOverlap = null;
					if (!isValid) {
						m1.dispose(); m1 = null;
						m2.dispose(); m2 = null;						
						return null;
					}
				}
			}
		}

		if (this.reduceSameMatch) {
			if (isSameRuleAndSameMatch(r1, r2, m1, m2)) {
				m1.dispose(); m1 = null;
				m2.dispose(); m2 = null;
				return null;
			}
		}
		
		if (this.directStrctCnfl || this.directStrctCnflUpToIso) {
			Pair<OrdinaryMorphism,OrdinaryMorphism> pair1 = BaseFactory.theFactory().makePO(r1, m1, true, false);
			Pair<OrdinaryMorphism,OrdinaryMorphism> pair2 = BaseFactory.theFactory().makePO(r2, m2, true, false);
			if (pair1 != null && pair2 != null) {
				OrdinaryMorphism po1 = pair1.first;
				OrdinaryMorphism po2 = pair2.first;	
				if (isDirectlyStrictConfluent(po1, po2)
						// test
						|| (this.directStrctCnflUpToIso
								&& isIsomorphicTo(pair1.second.getImage(), pair2.second.getImage())) ) {							
					po1.dispose(); 
					po2.dispose();
					m1.dispose(); m1 = null;			
					m2.dispose(); m2 = null;
					return null;
				}	
			}
		}
		return new Pair<OrdinaryMorphism, OrdinaryMorphism>(m1, m2);
	}

	boolean checkNac2afterStep1(
			final OrdinaryMorphism step1,
			final Rule r2,
			final OrdinaryMorphism nac,
			final OrdinaryMorphism isoL2ext,
			final OrdinaryMorphism overlap2) {							
		// make test match of rule2 because of its NACs
		OrdinaryMorphism tom2 = overlap2.compose(step1);
		OrdinaryMorphism ttom2 = isoL2ext.compose(tom2);
		Match tm2 = BaseFactory.theFactory().makeMatch(r2, ttom2);
		if (tm2 != null) {
			// if the NAC is not satisfied (nacStar != null) after step of rule1, 
			// then overlapping is critical
			OrdinaryMorphism nacStar = (OrdinaryMorphism)tm2.checkNAC(nac, true); 
			if (nacStar != null) {
				tm2.dispose(); tm2 = null;
				ttom2.dispose(); ttom2 = null;
				tom2.dispose(); tom2 = null;
				return true;
			}
		}
		else {
			ttom2.dispose(); ttom2 = null;
			tom2.dispose(); tom2 = null;
		}		
		return false;
	}
	

	private List<GraphObject> getChangedObjs(Rule r1) {	
		List<GraphObject> v = new Vector<GraphObject>(5);
		v.addAll(this.preservedChanged);
		/*  // global container of rule r1
		if (!this.preservedChanged.isEmpty()) {		
			// changed attributes of r1 
			List<GraphObject> v = new Vector<GraphObject>(5);
			for (int i = 0; i < this.preservedChanged.size(); i++) {			
				if (ExcludePairHelper.doesRuleChangeAttr(
						r1, this.preservedChanged.elementAt(i))) {
					v.add(this.preservedChanged.elementAt(i));
				}
			}
			return v;
		}
		return null;
		*/
		return v;
	}
	
	private boolean tryValidateAttrCond(final Match m) {		
		boolean result = true;
		if (m.getAttrContext().getConditions().getNumberOfEntries() > 0) {
			final List<VarMember> list = new Vector<VarMember>(1);	
//			((VarTuple) m.getAttrContext().getVariables()).showVariables();
//			((CondTuple) m.getAttrContext().getConditions()).showConditions();						
			// check attr condition which is marking by CondMember.LHS
			result = checkAttrCondOfLHS(m);
			// additionally, check attr condition which is NOT marking by CondMember.LHS
			result = result && this.checkAttrCondOfMorph(m);
		
			for (int i=0; i<list.size(); i++) {
				list.get(i).undoUnification();
				list.get(i).setExpr(null);
			}
		}
		return result;
	}
	
	/*
	 * Returns 1 (TRUE) if the nac has not any attr. conditions and variables;
	 * returns 1 (TRUE) if for all attr. conditions of the nac: 
	 * 	- all variables are set and attr. conditions are satisfied;
	 * returns -1 (FALSE) if for all attr. conditions of the nac: 
	 * 	- all variables are set and at least one attr. conditions is not satisfied;
	 * otherwise - 0 (UNDEFINED).
	 */
	int tryValidateNACAttrCond(
			final OrdinaryMorphism nac,
			final OrdinaryMorphism nacStar,
			final OrdinaryMorphism m) {
		
		if (m.getAttrContext().getVariables().getNumberOfEntries() == 0)
			return 1;
		int allConds = m.getAttrContext().getConditions().getNumberOfEntries();
		if (allConds > 0) {
			final List<VarMember> list = new Vector<VarMember>(1);		
					
			Enumeration<GraphObject> objs = nacStar.getDomain();									
			while (objs.hasMoreElements()) {
				GraphObject obj_om = objs.nextElement();
				if (obj_om.getAttribute() == null)
					continue;
				GraphObject obj = nacStar.getImage(obj_om);									
				if (obj != null && obj.getAttribute() != null) {														
					for (int i=0; i<obj_om.getAttribute().getNumberOfEntries(); i++) {
						ValueMember obj_om_mem = (ValueMember) obj_om.getAttribute().getMemberAt(i);
						ValueMember obj_mem = (ValueMember) obj.getAttribute().getMemberAt(obj_om_mem.getName());
						if (obj_om_mem.isSet() && obj_om_mem.getExpr().isVariable()) {
							if (obj_mem != null && obj_mem.isSet() && obj_mem.getExpr().isConstant()) {									
								VarMember var = m.getAttrContext().getVariables().getVarMemberAt(obj_om_mem.getExprAsText());
								if (var != null) {
									var.setExprAsText(obj_mem.getExprAsText());
									list.add(var);									
								}
							}
						}
					}
				}
			}
//			((VarTuple) m.getAttrContext().getVariables()).showVariables();
//			((CondTuple) m.getAttrContext().getConditions()).showConditions();						
			
			int evalRes = 0;
			int nacConds = 0;
			CondTuple conds = (CondTuple) m.getAttrContext().getConditions();
			for (int i = 0; i < conds.getSize(); i++) {
				CondMember cond = conds.getCondMemberAt(i);
				if (cond.isEnabled() && cond.getMark() != CondMember.LHS) {
					nacConds++;
					evalRes = 1;
					if (!cond.isDefinite() || !cond.areVariablesSet()) {
						evalRes = 0;
						break;
					}
					if (!cond.isTrue()) {
						evalRes = -1;
						break;
					}
				}
			}			
			for (int i=0; i<list.size(); i++) {
				list.get(i).undoUnification();
				list.get(i).setExpr(null);
			}
			if (nacConds == 0) { 
				if (allConds == 0)
					return 1;
				else return 0;
			}
			else
				return evalRes;
		}
		return 0;
	}
	
	boolean tryValidateAttrCond(
			final boolean firstRule,
			final Match m,
			final OrdinaryMorphism overlap1,
			final OrdinaryMorphism overlap2) {
		
		boolean result = true;
		if (m.getAttrContext().getConditions().getNumberOfEntries() > 0) {
			List<VarMember> list = new Vector<VarMember>(1);		
			if (overlap1 != null && overlap2 != null) {
				if (firstRule) 
					list = applyConstantToVar(m, overlap1, overlap2);
				else
					list = applyConstantToVar(m, overlap2, overlap1);
			}
//			((VarTuple) m.getAttrContext().getVariables()).showVariables();
//			((CondTuple) m.getAttrContext().getConditions()).showConditions();						
			// check attr condition which is marking by CondMember.LHS
			result = checkAttrCondOfLHS(m);
			// additionally, check attr condition which is NOT marking by CondMember.LHS
			result = result && this.checkAttrCondOfMorph(m);
		
			for (int i=0; i<list.size(); i++) {
				list.get(i).undoUnification();
				list.get(i).setExpr(null);
			}
		}
		return result;
	}
	
	private List<VarMember> applyConstantToVar(
			final Match m,
			final OrdinaryMorphism overlap1,
			final OrdinaryMorphism overlap2) {
		
		List<VarMember> list = new Vector<VarMember>(1);		
		final Enumeration<GraphObject> dom1 = overlap1.getDomain();
		while (dom1.hasMoreElements()) {
			final GraphObject obj1 = dom1.nextElement();
			final GraphObject obj = overlap1.getImage(obj1);
			if (overlap2.getInverseImage(obj).hasMoreElements()) {
				final GraphObject obj2 = overlap2.getInverseImage(obj).nextElement();
				if (obj1.getAttribute() != null && obj2.getAttribute() != null && obj.getAttribute() != null) {
					for (int i=0; i<obj1.getAttribute().getNumberOfEntries(); i++) {
						ValueMember obj1mem = (ValueMember) obj1.getAttribute().getMemberAt(i);
						ValueMember obj2mem = (ValueMember) obj2.getAttribute().getMemberAt(obj1mem.getName());
						ValueMember objmem = (ValueMember) obj.getAttribute().getMemberAt(obj1mem.getName());
						if (objmem != null && objmem.isSet() && !objmem.getExpr().isConstant()) {
							if (obj1mem.isSet() && obj1mem.getExpr().isVariable()
									&& obj2mem != null && obj2mem.isSet() && obj2mem.getExpr().isConstant()) {
								VarMember var = m.getAttrContext().getVariables().getVarMemberAt(obj1mem.getExprAsText());											
								if (var != null) {
									var.setExprAsText(obj2mem.getExprAsText());
									list.add(var);
								}
							}
						}
					}
				}
			}
		}
		return list;
	}
				
	/*
	 * Checks attribute condition of type <code>CondMember</code> which is enabled,<br>
	 * and is only for LHS of a rule (its marking is <code>CondMember.LHS</code>),<br> 
	 * and all used variables are definite and set<br>
	 * then returns the value of condition evaluation.<br>
	 * Otherwise, returns true.
	 */
	private boolean checkAttrCondOfLHS(OrdinaryMorphism m) {
		CondTuple conds = (CondTuple) m.getAttrContext().getConditions();
		for (int i = 0; i < conds.getSize(); i++) {
			CondMember cond = conds.getCondMemberAt(i);
			if (cond.isEnabled() && cond.getMark() == CondMember.LHS
					&& cond.isDefinite() && cond.areVariablesSet()
					&& !cond.isTrue()) {				
				System.out.println("Attr cond: " + cond.getExprAsText()+ "  failed.");
//				((VarTuple) m.getAttrContext().getVariables()).unsetVariables();
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Checks all attribute conditions of type <code>CondMember</code> which are enabled,<br>
	 * and are not for LHS of a rule (its mark is not <code>CondMember.LHS</code>),<br> 
	 * and all variables are definite and set,<br>
	 * then returns the value of condition evaluation.<br>
	 * Otherwise returns TRUE.
	 */
	private boolean checkAttrCondOfApplCond(OrdinaryMorphism m) {
		CondTuple conds = (CondTuple) m.getAttrContext().getConditions();
		for (int i = 0; i < conds.getSize(); i++) {
			CondMember cond = conds.getCondMemberAt(i);
			if (cond.isEnabled() && cond.getMark() != CondMember.LHS
					&& cond.isDefinite() && cond.areVariablesSet()
					&& !cond.isTrue()) {
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Checks attribute condition of type <code>CondMember</code> 
	 * of the attribute context of the given morphism <code>m</code>.
	 * The given morphism <code>m</code> is a NAC or PAC morphism.
	 * This attribute condition is enabled,
	 * and not marking by <code>CondMember.LHS</code>, 
	 * and all used variables are definite and set.
	 * If at least one of these points failed, returns true.
	 * 
	 * In case of NAC it returns true if at least one attribute condition 
	 * is evaluated to false, otherwise returns false.

	 * In case of PAC it returns false if at least one attribute condition 
	 * is evaluated to false, otherwise returns true.
	 * 
	 */
	private boolean checkAttrCondOfMorph(OrdinaryMorphism m) {
		boolean defaultResult = true;
		
		CondTuple conds = (CondTuple) m.getAttrContext().getConditions();
		for (int i = 0; i < conds.getSize(); i++) {
			CondMember cond = conds.getCondMemberAt(i);
			if (!cond.isEnabled() || cond.getMark() == CondMember.LHS) { 			
					continue;
			}
			
			if (cond.isDefinite() && cond.areVariablesSet()) {
				
				if (cond.getMark() >= CondMember.NAC
						|| cond.getMark() <= CondMember.NAC_PAC_LHS) {
					
					defaultResult = false;
					if (!cond.isTrue())
						return true;
				}
				else if (cond.getMark() >= CondMember.PAC
						|| cond.getMark() <= CondMember.PAC_LHS) {
					
					if (!cond.isTrue())
						return false;
				}
			}
		}
		return defaultResult;
	}
	
	private boolean isDeleteUseMatchValid(
			final Rule r,
			final Match m,
			final OrdinaryMorphism nac,
			boolean firstRule,
			final OrdinaryMorphism overlap1,
			final OrdinaryMorphism overlap2) {
		
		m.getTarget().setCompleteGraph(false);
		if (m.isTotal()
				&& m.isValid(true)
				&& tryValidateAttrCond(firstRule, m, overlap1, overlap2)
				&& checkNACs(r, m, nac, firstRule)) {
			return true;
		}
		return false;
	}	
	
	/*
	 * Returns TRUE if all checked PACs are valid: 
	 * - PAC graph structure is found in m.getTarget() 
	 * Otherwise returns FALSE.
	 */
	private boolean checkPACs(final Rule r, final Match m,
			final OrdinaryMorphism pac_NotToCheck, boolean firstRule) {	
		
		boolean result = true;
		if (this.withPACs) {
			final List<OrdinaryMorphism> pacs = r.getPACsList();
			for (int l=0; l<pacs.size(); l++) {
				final OrdinaryMorphism pac = pacs.get(l);					
				if (pac.isEnabled() && (pac_NotToCheck == null || pac_NotToCheck != pac)) {
					OrdinaryMorphism pacStar = (OrdinaryMorphism) m.checkPAC(pac, true);
					if (pacStar != null) {
						if (firstRule) {
							if (!hasVariableInContext(pac, r.getAttrContext())
									&& !hasConstantToVariableInContext(pacStar)
									&& !hasNewConstantToVariableInContext(pac, pacStar)
									) {							
								pacStar.dispose(); pacStar = null;						
							} 
							pacStar.dispose(); pacStar = null;							
						} else 
							if (!hasVariableInContext(pac, r.getAttrContext())
								&& !hasConstantToVariableInContext(pacStar)
								&& !hasNewConstantToVariableInContext(pac, pacStar)
								) {
							pacStar.dispose(); pacStar = null;
						} else {
							pacStar.dispose(); pacStar = null;
							result = false;
							break;
						}
					}
					else {
						result = false;
						break;
					}
				}
			}
		}
		return result;
	}
	
	/*
	 * Returns TRUE if all checked NACs are valid: 
	 * - NAC graph structure is not found in m.getTarget() 
	 * - or the NAC structure is found, but the attribut context is not valid
	 *		( NAC attr. conditions are TRUE, no any NAC Star mappings with (constant -> variable) ).
	 * Otherwise returns FALSE.
	 */
	private boolean checkNACs(final Rule r, final Match m,
			final OrdinaryMorphism nac_NotToCheck, boolean firstRule) {	
		
		boolean result = true;
		if (this.withNACs) {
			final List<OrdinaryMorphism> nacs = r.getNACsList();
			for (int l=0; l<nacs.size(); l++) {
				final OrdinaryMorphism nac = nacs.get(l);					
				if (nac.isEnabled() && (nac_NotToCheck == null || nac_NotToCheck != nac)) {
					OrdinaryMorphism nacStar = (OrdinaryMorphism) m.checkNAC(nac, true);
					if (nacStar != null) {
						if (firstRule) {
							if (!hasVariableInContext(nac, r.getAttrContext())
									&& !hasConstantToVariableInContext(nacStar)
									&& !hasNewConstantToVariableInContext(nac, nacStar)
									) {							
								nacStar.dispose(); nacStar = null;
								result = false;							
								break;
							} 
							nacStar.dispose(); nacStar = null;							
						} else if (!hasVariableInContext(nac, r.getAttrContext())
								&& !hasConstantToVariableInContext(nacStar)
								&& !hasNewConstantToVariableInContext(nac, nacStar)
								) {
							nacStar.dispose(); nacStar = null;
							result = false;
							break;
						} else {
							nacStar.dispose(); nacStar = null;
						}
					}
				}
			}
		}
		return result;
	}
	
	private boolean isProduceForbidMatchValid(
			final Rule r, 
			final Match m,
			OrdinaryMorphism nac,
			boolean firstRule,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> overlapping) {

		boolean result = true;
		m.getTarget().setCompleteGraph(false);
		
		if (!m.isTotal()) {
			result = false;
		} 
		else if (!m.isValid(true)) {
			result = false;
		} 
		else if (!tryValidateAttrCond(firstRule, m, overlapping.first, overlapping.second)) {
			result = false;
		}		
		else {
			if (this.withNACs) {
				final List<OrdinaryMorphism> nacs = r.getNACsList();
				for (int l=0; l<nacs.size(); l++) {
					final OrdinaryMorphism n = nacs.get(l);	
					if (n.isEnabled() /*&& (n != nac)*/) { 
						OrdinaryMorphism nacStar = (OrdinaryMorphism) m.checkNAC(n, true);
						if (nacStar != null) {							 
							if (firstRule) {
								if (!hasVariableInContext(n, r.getAttrContext())
										&& !hasConstantToVariableInContext(nacStar)
										&& !hasNewConstantToVariableInContext(n, nacStar)) {
									nacStar.dispose(); nacStar = null;
									result = false;
									break;
								}
								else {
									nacStar.dispose(); nacStar = null;
								}
							} 
							else if (!hasVariableInContext(n, r.getAttrContext())
										&& !hasConstantToVariableInContext(nacStar)
										&& !hasNewConstantToVariableInContext(n, nacStar)) {
									nacStar.dispose(); nacStar = null;
									result = false;
									break;
							} 
							else {
								nacStar.dispose(); nacStar = null;
							}
						}
					}
				}
			}
		}

		return result;
	}

	boolean isChangeAttrMatchValid(
			final Rule r1,
			final Rule r2, 
			final Match m,
			final OrdinaryMorphism nac,
			final OrdinaryMorphism isoNAC2ext,
			boolean firstRule, 
			final OrdinaryMorphism overlap1,
			final OrdinaryMorphism overlap2) {
		
		Rule r = firstRule? r1: r2;
		boolean result = true;
		m.getTarget().setCompleteGraph(false);	
		
		if (!m.isTotal()) {
			result = false;
		} 
		else if (!m.isValid(true)) {
			result = false;
		}
		else if (!tryValidateAttrCond(firstRule, m, overlap1, overlap2)) {
			result = false;
		}
		else {
			if (this.withNACs) {				
				final List<OrdinaryMorphism> nacs = r.getNACsList();
				for (int l=0; l<nacs.size(); l++) {
					final OrdinaryMorphism n = nacs.get(l);					
					if (n.isEnabled()) {
						OrdinaryMorphism nacStar = (OrdinaryMorphism) m.checkNAC(n, true);
						if (nacStar != null) { 
							int nacCondEval = tryValidateNACAttrCond(n, nacStar, m);
							if (nacCondEval == 1) { // no any conditions and variables
								if (!firstRule) {
									if (hasNewConstantToCriticalInContext(null, nacStar)
											|| hasConstantToVariableInContext(nacStar)
											|| hasVariableInContext(n, r.getAttrContext())) {														
										result = true;									
										nacStar.dispose(); nacStar = null;
									}
									else {
										result = false;
										nacStar.dispose(); nacStar = null;
										break;
									}
								}
								else {
									result = false;
									nacStar.dispose(); nacStar = null;
									break;
								}
							}
							else if (nacCondEval == -1) {
								result = true;	
								nacStar.dispose(); nacStar = null;							
							}
							else if (nacCondEval == 0) { //UNDEFINED
								// TODO TEST MORE
								if (nac == null || n != nac) {
//									if (firstRule) 
									{
										if (!hasVariableInContext(n, r.getAttrContext())
												&& !hasConstantToVariableInContext(nacStar)
												&& !hasNewConstantToVariableInContext(n, nacStar)) {
											result = false;
											nacStar.dispose(); nacStar = null;
											break;
										} 								
									}
									/* second rule
									else if (!hasVariableInContext(n, r.getAttrContext())
											&& !hasConstantToVariableInContext(nacStar)
											&& !hasNewConstantToVariableInContext(n, nacStar)) {										
										result = false;
										nacStar.dispose(); nacStar = null;
										break;
									} 
									*/
									nacStar.dispose(); nacStar = null;
								}
							}
						}
					}
				}
			}
		}

		return result;
	}

	private boolean isInverseMatchValid(final boolean firstRule, final Match m) {
		m.getTarget().setCompleteGraph(false);
		if (!m.isTotal()) {
			return false;
		} else if (!m.isValid(true)) {
			return false;
		} else if (!tryValidateAttrCond(m)) {
			return false;
		}
		return true;
	}
	

	/*
	 * Checks variables of the morph context (if relatedAttrContext is null) or of the relatedAttrContext.
	 * Returns TRUE if a variable is an input parameter or is in an attr. condition which is not valid or false.
	 * Otherwise returns FALSE.
	 */
	boolean hasVariableInContext(final OrdinaryMorphism morph, final AttrContext relatedAttrContext) {		
		final VarTuple vars = (VarTuple) morph.getAttrContext().getVariables();
		final CondTuple conds = (CondTuple) morph.getAttrContext().getConditions();
		
		boolean result = hasVarInContext(morph, vars, conds, relatedAttrContext,
								morph.getTarget().getNodesSet().iterator())
						|| hasVarInContext(morph, vars, conds, relatedAttrContext,
								morph.getTarget().getArcsSet().iterator());
						
		return result;
	}

	/*
	 * Checks variables of the vars (if relatedAttrContext is null) or of the relatedAttrContext.
	 * Returns TRUE if a variable is an input parameter or is in an attr. condition which is not valid or false.
	 * Otherwise returns FALSE.
	 */
	private boolean hasVarInContext(
			final OrdinaryMorphism morph, 
			final VarTuple vars,
			final CondTuple conds,
			final AttrContext relatedAttrContext,
			final Iterator<?> e) {		
		
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (o.getAttribute() == null) {
				continue;
			}
			ValueTuple vt = (ValueTuple) o.getAttribute();
			for (int k = 0; k < vt.getSize(); k++) {
				ValueMember vm = vt.getValueMemberAt(k);
				if (vm.isSet()) {
					if (vm.getExpr().isVariable()) {
						VarMember varm = (relatedAttrContext == null)?					
									vars.getVarMemberAt(vm.getExprAsText()):
									((VarTuple)relatedAttrContext.getVariables()).getVarMemberAt(vm.getExprAsText());							
						
						if (varm == null) {
							return true;
						}
						else {
							if (varm.isInputParameter()) {
								return true;
							}
							Enumeration<GraphObject> en = morph.getInverseImage(o);
							if (!en.hasMoreElements()) { 
								// object o is in morph.target only
								if (conds.isEmpty()) 
									return true;
								else {
									boolean condOK = true;
									boolean varInCond = false;
									for (int i = 0; i < conds.getSize(); i++) {
										CondMember cm = (CondMember) conds.getValueMemberAt(i);									
										if (cm.isEnabled()
												&& cm.getAllVariableNamesOfExpression().contains(varm.getName())) {
											varInCond = true;
											if (cm.isDefinite() && cm.areVariablesSet() && cm.isTrue()) {
												// take next condition
											}
											else {	
												condOK = false;
												break;												
											}
										}
									}
									if (varInCond)
										return !condOK;
									else
										return true;
								}
							}
							else { // object o is also in morph.source 
								for (int i = 0; i < conds.getSize(); i++) {
									CondMember cm = (CondMember) conds.getValueMemberAt(i);
									if (cm.getAllVariableNamesOfExpression().contains(varm.getName())) {
										return true;
									}
								}
								
								GraphObject oi = en.nextElement();
								ValueMember vm_oi = ((ValueTuple) oi.getAttribute()).getValueMemberAt(vm.getName());
								if (!vm_oi.isSet() || !vm.getExprAsText().equals(vm_oi.getExprAsText())) {
									return true;
								}
							}
						}						
					}
				}
			} 
		}		
		return false;
	}

	
	/*
	 * Here motph.target == co_motph.source
	 * Returns TRUE if at least one object of the morph.target graph without any pre-image 
	 * has an attr. mapping with constant -> !constant into its image of the co_morph.
	 * Otherwise returns FALSE.
	 */
	boolean hasNewConstantToVariableInContext(
			final OrdinaryMorphism morph,
			final OrdinaryMorphism co_morph) {		
		boolean result = hasNewConstToVarInContext(morph, co_morph, morph.getTarget().getNodesSet().iterator())
						|| hasNewConstToVarInContext(morph, co_morph, morph.getTarget().getArcsSet().iterator());
		return result;
	}

	/*
	 * Here objs == motph.target graph objects == co_motph.source graph objects.
	 * Returns TRUE if at least one object of objs without any pre-image of the morph
	 * has an attr. mapping with constant -> variable into its image of the co_morph.
	 * Otherwise returns FALSE.
	 */
	private boolean hasNewConstToVarInContext(
			final OrdinaryMorphism morph,
			final OrdinaryMorphism co_morph,
			final Iterator<?> objs) {

		while (objs.hasNext()) {
			GraphObject o = (GraphObject) objs.next();
			if (o.getAttribute() == null)
				continue;
			
			GraphObject co_obj = co_morph.getImage(o);
			if (!morph.getInverseImage(o).hasMoreElements() 
					&& co_obj != null) {
				ValueTuple vt = (ValueTuple) o.getAttribute();
				ValueTuple co_vt = (ValueTuple) co_obj.getAttribute();
				for (int k = 0; k < vt.getSize(); k++) {
					ValueMember vm = vt.getValueMemberAt(k);
					ValueMember co_vm = co_vt.getValueMemberAt(vm.getName());
					if (co_vm != null 
							&& (vm.getExpr() != null && vm.getExpr().isConstant())
							&& (co_vm.getExpr() != null && !co_vm.getExpr().isConstant())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	boolean hasConstantToVariableInContext(final OrdinaryMorphism morph) {
		final Enumeration<GraphObject> e = morph.getDomain();
		while (e.hasMoreElements()) {
			final GraphObject o = e.nextElement();
			final GraphObject img = morph.getImage(o); 
			if (o.getAttribute() == null || img.getAttribute() == null)
				continue;
			 
			final ValueTuple vt = (ValueTuple) o.getAttribute();
			final ValueTuple vtimg = (ValueTuple) img.getAttribute();
			for (int k = 0; k < vt.getSize(); k++) {
				final ValueMember vm = vt.getValueMemberAt(k);
				final ValueMember vmimg = vtimg.getValueMemberAt(vm.getName());
				if (vmimg != null &&
						(vm.getExpr() != null && vm.getExpr().isConstant())
						&& (vmimg.getExpr() != null && vmimg.getExpr().isVariable())) {
					return true;
				}
			}
		}
		return false;
	}
	
	protected boolean hasNewConstantToCriticalInContext(
			final OrdinaryMorphism morph,
			final OrdinaryMorphism co_morph) {
		final Enumeration<GraphObject> e = co_morph.getDomain();
		while (e.hasMoreElements()) {
			final GraphObject o = e.nextElement();
			final GraphObject img = co_morph.getImage(o); 
			if (o.getAttribute() == null || img.getAttribute() == null)
				continue;
			
			if (morph == null || !morph.getInverseImage(o).hasMoreElements())
			{
				final ValueTuple vt = (ValueTuple) o.getAttribute();
				final ValueTuple vtimg = (ValueTuple) img.getAttribute();
				for (int k = 0; k < vt.getSize(); k++) {
					final ValueMember vm = vt.getValueMemberAt(k);
					final ValueMember vmimg = vtimg.getValueMemberAt(vm.getName());
					if (vmimg != null
							&& (vm.getExpr() != null && vm.getExpr().isConstant())
							&& (vmimg.getExpr() != null && img.isCritical())) {	
						return true;
					}
				}
			}
		}
		return false;
	}
	
	protected boolean reduceCriticalPairs(
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> v) {
		
		boolean reduced = false;
		int size = v.size();
		boolean found = true;
		while ((size > 0) && found) {
			found = false;
			for (int i = 0; i<size && !found; i++) {
				Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p1i = v.elementAt(i);
				Pair<OrdinaryMorphism, OrdinaryMorphism> p1 = p1i.first;
				for (int j = i + 1; j<size && !found; j++) {
					Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
					p2i = v.elementAt(j);
					Pair<OrdinaryMorphism, OrdinaryMorphism> p2 = p2i.first;
					Pair<OrdinaryMorphism, OrdinaryMorphism> p = checkIfSimilar(p1, p2);
					if (p != null) {
						found = true;
						v.remove(p2i);	
						Graph g = p2i.first.first.getTarget();
						p2i.first.first.dispose();
						p2i.first.second.dispose();
						if (p2i.second != null) {
							p2i.second.first.dispose();
							if (p2i.second.second != null)
								p2i.second.second.dispose();
						}
						g.dispose();
						reduced = true;
					}				
				}
			}
			size = v.size();
		}
		return reduced;
	}

	private Pair<OrdinaryMorphism, OrdinaryMorphism> checkIfSimilar(
			Pair<OrdinaryMorphism, OrdinaryMorphism> p1, 
			Pair<OrdinaryMorphism, OrdinaryMorphism> p2) {
		
		Graph overlap1 = p1.first.getImage();
		Graph overlap2 = p2.first.getImage();
		
		if (overlap1.getSize() == overlap2.getSize()) {
			if (ExcludePairHelper
					.checkIfMorphSimilar(overlap1, overlap2, p1.first, p2.first, p1.second, p2.second))
				return p2;
			else
				return null;
		}
		else
			return null;
	}
	
	@SuppressWarnings("unused")
	private void inspectCritPair(
			final Rule r1, 
			final Rule r2,
			final List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
			overlappings) {
		
		CriticalPairData data = new CriticalPairData(r1, r2, overlappings);
		while (data.hasCriticals()) {
			if (data.next()) {
				System.out.println(r1.getName());
				System.out.println(r2.getName());
				System.out.println(data.getRule1().getName());
				System.out.println(data.getRule2().getName());
				
				OrdinaryMorphism mo1 = data.getMorph1();
				OrdinaryMorphism mo2 = data.getMorph2();
				
				System.out.println(data.getMorph1().getName());
				System.out.println(data.getMorph2().getName());
				
				OrdinaryMorphism m1 = data.getMorph1DueToLHS();
				OrdinaryMorphism m2 = data.getMorph2DueToLHS();
				OrdinaryMorphism m3 = data.getMorph2DueToNAC();
			}
		}
	}
}

/*
 * $Log: ExcludePair.java,v $
 * Revision 1.147  2010/12/16 17:32:14  olga
 * tuning
 *
 * Revision 1.146  2010/12/15 16:57:56  olga
 * restriction added - CPA for rules with GACs not implemented
 *
 * Revision 1.145  2010/11/28 22:15:26  olga
 * tuning
 *
 * Revision 1.144  2010/11/17 14:06:38  olga
 * improved
 *
 * Revision 1.143  2010/11/16 23:33:09  olga
 * tuning
 *
 * Revision 1.142  2010/11/11 17:18:20  olga
 * tuning
 *
 * Revision 1.141  2010/11/07 20:48:11  olga
 * tuning
 *
 * Revision 1.140  2010/11/06 20:18:45  olga
 * tuning
 *
 * Revision 1.139  2010/11/06 18:33:50  olga
 * extended and improved
 *
 * Revision 1.138  2010/11/04 11:01:32  olga
 * tuning
 *
 * Revision 1.137  2010/10/07 20:07:09  olga
 * GraTra option GACs added
 *
 * Revision 1.136  2010/09/20 14:30:11  olga
 * tuning
 *
 * Revision 1.135  2010/08/12 14:53:28  olga
 * tuning
 *
 * Revision 1.134  2010/07/29 10:03:45  olga
 * tuning
 *
 * Revision 1.133  2010/06/28 07:53:06  olga
 * tuning
 *
 * Revision 1.132  2010/06/09 10:56:06  olga
 * tuning
 *
 * Revision 1.131  2010/05/03 08:00:56  olga
 * tuning
 *
 * Revision 1.130  2010/04/29 15:15:13  olga
 * tuning and tests
 *
 * Revision 1.129  2010/04/28 15:16:42  olga
 * tuning
 *
 * Revision 1.128  2010/04/27 10:38:34  olga
 * computing tuning
 *
 * Revision 1.127  2010/04/21 23:38:00  olga
 * CPA optimization
 *
 * Revision 1.126  2010/04/19 11:35:08  olga
 * CPA: bug fixed in attr check with NAC
 *
 * Revision 1.125  2010/04/08 09:22:48  olga
 * essential CPs : change attribute conflict - bug fixed
 *
 * Revision 1.124  2010/03/20 19:27:21  olga
 * docu
 *
 * Revision 1.123  2010/03/08 15:46:42  olga
 * code optimizing
 *
 * Revision 1.122  2010/03/04 14:11:14  olga
 * code optimizing
 *
 * Revision 1.121  2010/02/22 15:01:57  olga
 * code optimizing
 *
 * Revision 1.120  2009/10/12 08:26:39  olga
 * ApplOfRS-improved and bug fixed
 *
 * Revision 1.119  2009/10/05 14:00:04  olga
 * ARS check, attr. condition check - improved
 *
 * Revision 1.118  2009/10/05 08:52:34  olga
 * RSA check - bug fixed
 *
 * Revision 1.117  2009/09/03 15:53:25  olga
 * checking attr condition during CPA - improved
 *
 * Revision 1.116  2009/09/02 12:37:11  olga
 * Checking attribute  condition - bug fixed
 *
 * Revision 1.115  2009/08/03 16:54:22  olga
 * CPA , essential pairs - bug fixed
 *
 * Revision 1.114  2009/07/30 10:44:53  olga
 * Code tuning
 *
 * Revision 1.113  2009/07/30 07:51:30  olga
 * Code tuning
 *
 * Revision 1.112  2009/07/30 07:35:44  olga
 * Extension of CPA process
 *
 * Revision 1.111  2009/07/29 16:32:32  olga
 * Match bug fixed
 *
 * Revision 1.110  2009/07/20 07:54:11  olga
 * Appl. RuleSequence - tuning
 *
 * Revision 1.109  2009/07/16 17:21:02  olga
 * GUI bugs fixed
 *
 * Revision 1.108  2009/07/14 12:16:31  olga
 * Multiplicity bug fixed
 *
 * Revision 1.107  2009/07/08 16:22:01  olga
 * Multiplicity bug fixed;
 * ARS development
 *
 * Revision 1.106  2009/06/30 09:50:19  olga
 * agg.xt_basis.GraphObject: added: setObjectName(String), getObjectName()
 * agg.xt_basis.Node, Arc: changed: save, load the object name
 * agg.editor.impl.EdGraphObject: changed: String getTypeString() - contains object name if set
 *
 * workaround of Applicability of Rule Sequences and Object Flow
 *
 * Revision 1.105  2009/06/02 12:39:25  olga
 * Min Multiplicity check - bug fixed
 *
 * Revision 1.104  2009/05/28 13:18:23  olga
 * Amalgamated graph transformation - development stage
 *
 * Revision 1.103  2009/05/12 10:54:25  olga
 * CPA : bug fixed
 *
 * Revision 1.102  2009/05/12 10:36:58  olga
 * CPA: bug fixed
 * Applicability of Rule Seq. : bug fixed
 *
 * Revision 1.101  2009/04/27 07:37:15  olga
 * Copy and Paste TypeGraph- bug fixed
 * CPA - dangling edge conflict when first produce second delete - extended
 *
 * Revision 1.100  2009/04/23 06:29:13  olga
 * CSP: bug fixed
 *
 * Revision 1.99  2009/04/21 13:21:49  olga
 * CPA: bug fixed
 *
 * Revision 1.98  2009/04/20 08:50:45  olga
 * CPA: bug fixed
 *
 * Revision 1.97  2009/03/19 09:31:06  olga
 * CPE: attr check improved
 *
 * Revision 1.96  2009/03/12 10:57:45  olga
 * some changes in CPA of managing names of the attribute variables.
 *
 * Revision 1.95  2009/03/04 13:06:10  olga
 * New extension: Export/Import to/from ColorGraph
 *
 * Revision 1.94  2009/02/26 16:31:42  olga
 * Code tuning
 *
 * Revision 1.93  2009/01/29 14:30:35  olga
 * CPA - bug fixed
 *
 * Revision 1.92  2008/11/13 08:26:21  olga
 * some tests
 *
 * Revision 1.91  2008/10/22 14:07:55  olga
 * GUI, ARS and CPA  tuning
 *
 * Revision 1.90  2008/10/15 14:58:39  olga
 * - Bug fixed: import type graph with inheritance
 * - Bug fixed: edge type Multiplicity check in CPA
 *
 * Revision 1.89  2008/10/07 07:44:46  olga
 * Bug fixed: usage static methods of user own classes in attribute condition
 *
 * Revision 1.88  2008/09/22 10:02:38  olga
 * tests only
 *
 * Revision 1.87  2008/09/11 09:22:26  olga
 * Some changes in CPA: new computing of conflicts after an option changed,
 * Graph layout of overlapping graphs
 *
 * Revision 1.86  2008/07/30 06:27:14  olga
 * Applicability of RS , concurrent rule - handling of attributes improved
 *
 * Revision 1.85  2008/07/28 17:13:10  olga
 * NACStarMorphism - bug fixed
 *
 * Revision 1.84  2008/07/21 10:03:28  olga
 * Code tuning
 *
 * Revision 1.83  2008/07/17 15:51:50  olga
 * GraphEditor - graph scaling tuning
 *
 * Revision 1.82  2008/07/10 07:42:34  olga
 * Applicability of RS - tuning
 *
 * Revision 1.81  2008/07/09 13:34:26  olga
 * Applicability of RS - bug fixed
 * Delete not used node/edge type - bug fixed
 * AGG help - extended
 *
 * Revision 1.80  2008/07/07 07:52:28  olga
 * Applicability of Rule Sequences - bug fixed
 *
 * Revision 1.79  2008/06/30 10:47:39  olga
 * Applicability of Rule Sequence - tuning
 * Node animation - first steps
 *
 * Revision 1.78  2008/06/18 15:35:29  olga
 * Applicability of Rule Sequences - Tuning
 *
 * Revision 1.77  2008/05/22 15:21:06  olga
 * ARS - implementing further details
 *
 * Revision 1.76  2008/05/07 08:40:33  olga
 * ...
 *
 * Revision 1.75  2008/05/07 08:37:55  olga
 * Applicability of Rule Sequences with NACs
 *
 * Revision 1.74  2008/05/05 09:11:51  olga
 * Graph parser - bug fixed.
 * New AGG feature - Applicability of Rule Sequences - in working.
 *
 * Revision 1.73  2008/04/10 10:53:14  olga
 * Draw graphics tuning
 *
 * Revision 1.72  2008/04/09 13:05:25  olga
 * strong attribute check at constant value fixed
 *
 * Revision 1.71  2008/04/09 07:38:53  olga
 * Bug fixed during loading of grammar
 *
 * Revision 1.70  2008/04/07 09:36:51  olga
 * Code tuning: refactoring + profiling
 * Extension: CPA - two new options added
 *
 * Revision 1.69  2008/02/25 08:44:48  olga
 * Extending of CPA: new class CriticalRulePairAtGraph to get critical
 * matches of two rules at a concret graph.
 *
 * Revision 1.68  2008/02/18 13:43:04  olga
 * Code tuning
 *
 * Revision 1.67  2008/02/18 09:37:10  olga
 * - an extention of rule dependency check is implemented;
 * - some bugs fixed;
 * - editing of graphs improved
 *
 * Revision 1.66  2008/01/07 09:08:40  olga
 * - Applying an injective / non-injective rule at non-injective match - bug fixed;
 * - Moving nodes/edges in edit mode "Select" - .;
 * - CPA: Title of the overlapping graph frame for "delete-use" conflict - bug fixed
 *
 * Revision 1.65  2007/12/20 12:52:16  olga
 * code tuning
 *
 * Revision 1.64  2007/12/19 11:04:59  olga
 * Code tuning
 *
 * Revision 1.63  2007/12/17 08:33:30  olga
 * Editing inheritance relations - bug fixed;
 * CPA: dependency of rules - bug fixed
 *
 * Revision 1.62  2007/12/13 13:48:18  olga
 * code tuning
 *
 * Revision 1.61  2007/12/12 09:21:55  olga
 * code tuning
 *
 * Revision 1.60  2007/12/10 08:42:58  olga
 * CPA of grammar with node type inheritance for attributed graphs - bug fixed
 *
 * Revision 1.59  2007/12/05 08:57:01  olga
 * Delete a conclusion of an Atomic graph constraint : bug fixed
 * Graph visualization update after the marking "Abstract" of a type node in the type graph : bug fixed
 * CPA : some bug fixed; code tuning
 *
 * Revision 1.58  2007/12/03 08:35:12  olga
 * - Some bugs fixed in visualization of morphism mappings after deleting and creating
 * nodes, edges
 * - implemented: matching with non-injective NAC and Match morphism
 *
 * Revision 1.57  2007/11/21 09:59:45  olga
 * Update V1.6.2.1:
 * new features: - default attr value can be set in a type graph and used during  transformation (experimental phase)
 * - currently selected node and edge type are shown in the bottom right corner of the AGG GUI
 * - Critical pair analysis for grammar with node type inheritance (experimental phase)
 *
 * Revision 1.56  2007/11/19 08:48:39  olga
 * Some GUI usability mistakes fixed.
 * Default values in node/edge of a type graph implemented.
 * Code tuning.
 *
 * Revision 1.55  2007/11/14 14:27:23  olga
 * code tuning
 *
 * Revision 1.54  2007/11/14 08:53:43  olga
 * code tuning
 *
 * Revision 1.53  2007/11/12 08:48:56  olga
 * Code tuning
 *
 * Revision 1.52  2007/11/08 12:57:00  olga
 * working on CPA inconsistency for rules with pacs and inheritance
 * bugs are possible
 *
 * Revision 1.51  2007/11/05 09:18:22  olga
 * code tuning
 *
 * Revision 1.50  2007/11/01 09:58:18  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.49  2007/10/24 08:21:30  olga
 * CPA with inheritance: implementierung and test
 *
 * Revision 1.47  2007/10/22 09:46:50  olga
 * Inheritance: bug fixed
 *
 * Revision 1.46  2007/10/22 09:03:16  olga
 * First implementation of CPA for grammars with node type inheritance.
 * Only for internal tests.
 *
 * Revision 1.45  2007/10/18 15:30:06  olga
 * CPA bug fixed
 *
 * Revision 1.44  2007/10/17 14:48:02  olga
 * EdRule: bug fixed
 * Code tuning
 *
 * Revision 1.43  2007/10/10 14:30:34  olga
 * Enumeration typing
 *
 * Revision 1.42  2007/10/10 07:44:27  olga
 * CPA: bug fixed
 * GUI, AtomConstraint: bug fixed
 *
 * Revision 1.41  2007/10/04 07:44:28  olga
 * Code tuning
 *
 * Revision 1.40  2007/09/27 15:53:18  olga
 * CPA  tuning
 *
 * Revision 1.39  2007/09/27 08:42:46  olga
 * CPA: new option  -ignore pairs with same rules and same matches-
 *
 * Revision 1.38  2007/09/24 09:42:38  olga
 * AGG transformation engine tuning
 * Revision 1.37 2007/09/10 13:05:40 olga In this
 * update: - package xerces2.5.0 is not used anymore; - class
 * com.objectspace.jgl.Pair is replaced by the agg own generic class
 * agg.util.Pair; - bugs fixed in: usage of PACs in rules; match completion;
 * usage of static method calls in attr. conditions - graph editing: added some
 * new features Revision 1.36 2007/07/09 08:00:27 olga GUI tuning
 * 
 * Revision 1.35 2007/07/05 14:10:33 olga CSP test and tuning
 * 
 * Revision 1.34 2007/07/04 14:45:53 olga test only
 * 
 * Revision 1.33 2007/06/18 08:16:00 olga New extentions by drawing edge.
 * 
 * Revision 1.32 2007/06/14 07:37:58 olga Bug fixed in the added check for
 * constant attr value
 * 
 * Revision 1.31 2007/06/13 13:31:55 olga tuning
 * 
 * Revision 1.30 2007/06/13 12:30:33 olga new attribute check for critical pairs
 * added: it checks the constant attribute value of an object in the LHS of the
 * first rule against the constant attribute value of an compatible object in
 * the LHS of the second rule. Here the LHS of the second rule is a value domain
 * of the constant attribute. If any equal constant is not found and there is no
 * other possibility to overlap the LHS1 and LHS2 then only over objects with
 * constant attribute value, then these two rules are non-conflicting rules.
 * Similar for NAC: the target graph af a NAC of the second rule agains the RHS
 * of the first rule. Here only objects of a NAC without mapping from the LHS
 * and produced objects of the RHS are considered.
 * 
 * Revision 1.29 2007/06/13 08:32:57 olga Update: V161
 * 
 * Revision 1.28 2007/03/28 10:00:54 olga - extensive changes of Node/Edge Type
 * Editor, - first Undo implementation for graphs and Node/edge Type editing and
 * transformation, - new / reimplemented options for layered transformation, for
 * graph layouter - enable / disable for NACs, attr conditions, formula - GUI
 * tuning
 * 
 * Revision 1.27 2007/02/07 08:38:45 olga CPA bug fixed
 * 
 * Revision 1.26 2007/02/05 12:33:39 olga CPA: chengeAttribute
 * conflict/dependency : attributes with constants bug fixed, but the critical
 * pairs computation has still a gap.
 * 
 * Revision 1.25 2007/01/31 09:19:29 olga Bug fixed in case of transformating
 * attributed grammar with inheritance and non-injective match
 * 
 * Revision 1.24 2007/01/11 10:21:14 olga Optimized Version 1.5.1beta , free for
 * tests
 * 
 * Revision 1.23 2006/12/13 13:32:59 enrico reimplemented code
 * 
 * Revision 1.22 2006/11/01 11:17:30 olga Optimized agg sources of CSP
 * algorithm, match usability, graph isomorphic copy, node/edge type
 * multiplicity check for injective rule and match
 * 
 * Revision 1.21 2006/05/18 15:41:30 olga CPA: Bug fixed Import graph tuning
 * 
 * Revision 1.20 2006/04/10 09:19:30 olga Import Type Graph, Import Graph -
 * tuning. Attr. member type check: if class does not exist. Graph constraints
 * for a layer of layered grammar.
 * 
 * Revision 1.19 2006/03/13 10:04:42 olga CPA tuning
 * 
 * Revision 1.18 2006/03/09 11:07:29 olga CPs tuning
 * 
 * Revision 1.17 2006/03/08 14:08:13 olga test
 * 
 * Revision 1.16 2006/03/08 13:48:20 olga test
 * 
 * Revision 1.15 2006/03/08 09:14:59 olga CPs mistake fixed
 * 
 * Revision 1.14 2006/03/06 10:04:06 olga CPs ...
 * 
 * Revision 1.13 2006/03/06 09:36:39 olga CPs tuning
 * 
 * Revision 1.12 2006/03/02 12:03:23 olga CPA: check host graph - done
 * 
 * Revision 1.11 2006/03/01 15:49:33 olga tests
 * 
 * Revision 1.10 2006/03/01 15:28:11 olga tests
 * 
 * Revision 1.8 2006/03/01 09:55:46 olga - new CPA algorithm, new CPA GUI
 * 
 * Revision 1.5 2005/09/26 08:35:15 olga CPA graph frames; bugs
 * 
 * Revision 1.4 2005/09/19 09:12:14 olga CPA GUI tuning
 * 
 * Revision 1.3 2005/09/12 10:34:02 olga Bug fixed in produce-forbid check.
 * 
 * Revision 1.2 2005/08/25 15:28:13 olga Null pointer in hasConstantContext
 * fixed.
 * 
 * Revision 1.1 2005/08/25 11:56:58 enrico *** empty log message ***
 * 
 * Revision 1.4 2005/07/13 08:13:37 olga Some code optimization only
 * 
 * Revision 1.3 2005/07/11 09:30:20 olga This is test version AGG V1.2.8alfa .
 * What is new: - saving rule option <disabled> - setting trigger rule for layer -
 * display attr. conditions in gragra tree view - CPA algorithm <dependencies> -
 * creating and display CPA graph with conflicts and/or dependencies based on
 * (.cpx) file
 * 
 * Revision 1.2 2005/06/20 13:37:03 olga Up to now the version 1.2.8 will be
 * prepared.
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.60 2005/05/23 09:54:30 olga CPA improved: Stop of generation
 * process or rule pair.
 * 
 * Revision 1.59 2005/05/09 11:42:04 olga -CPs: error of dangling condition
 * check fixed. -Transformation: attr. exception handling. -Omondo XMI to XSL:
 * importing
 * 
 * Revision 1.58 2005/04/14 12:30:28 olga Tests
 * 
 * Revision 1.57 2005/04/11 13:43:05 olga Dangling by CPs fixed.
 * 
 * Revision 1.56 2005/04/11 13:06:13 olga Errors during CPA are corrected.
 * 
 * Revision 1.55 2005/03/24 11:15:17 olga ...
 * 
 * Revision 1.54 2005/03/21 12:19:28 olga ...
 * 
 * Revision 1.53 2005/03/21 09:22:57 olga ...
 * 
 * Revision 1.52 2005/03/16 12:02:10 olga
 * 
 * only little changes
 * 
 * Revision 1.51 2005/03/03 13:48:42 olga - Match with NACs and attr. conditions
 * with mixed variables - error corrected - save/load class packages written by
 * user - PACs : creating T-equivalents - improved - save/load matches of the
 * rules (only one match of a rule) - more friendly graph/rule editor GUI - more
 * syntactical checks in attr. editor
 * 
 * Revision 1.50 2005/02/14 09:27:01 olga -PAC; -GUI, layered graph
 * transformation anzeigen; -CPs.
 * 
 * Revision 1.49 2005/02/09 11:10:32 olga Fehler bei Post Application
 * Constraints behoben. Import/Export Fehler mit aggXXX.jar behoben. CPs
 * berechnung : Variablenumbenennung in Overlapgraphen
 * 
 * Revision 1.48 2005/01/31 13:00:49 olga Das Laden mit TypeGraph check
 * ENABLED_MAX_MIN - OK CP Berechnung angeppasst.
 * 
 * Revision 1.47 2005/01/28 14:02:32 olga -Fehlerbehandlung beim Typgraph check
 * -Erweiterung CP GUI / CP Menu -Fehlerbehandlung mit identification option
 * -Fehlerbehandlung bei Rule PAC
 * 
 * Revision 1.46 2005/01/03 13:14:43 olga Errors handling
 * 
 * Revision 1.45 2004/12/20 14:53:48 olga Changes because of matching
 * optimisation.
 * 
 * Revision 1.44 2004/10/25 14:24:38 olga Fehlerbehandlung bei CPs und
 * Aenderungen im zusammenhang mit termination-Modul in AGG
 * 
 * Revision 1.43 2004/09/27 09:15:54 olga ...
 * 
 * Revision 1.42 2004/09/23 08:26:43 olga Fehler bei CPs weg, Debug output in
 * file
 * 
 * Revision 1.41 2004/09/13 10:21:14 olga Einige Erweiterungen und
 * Fehlerbeseitigung bei CPs und Graph Grammar Transformation
 * 
 * Revision 1.40 2004/07/15 11:13:10 olga CPs letzter Schliff
 * 
 * Revision 1.39 2004/07/12 07:31:41 olga ...
 * 
 * Revision 1.38 2004/06/24 07:06:38 olga ..
 * 
 * Revision 1.37 2004/06/23 08:26:57 olga CPs sind endlich OK.
 * 
 * Revision 1.36 2004/06/21 08:35:33 olga immer noch CPs
 * 
 * Revision 1.35 2004/06/17 10:21:50 olga Start-Transformation mit Anhalten nach
 * einer Ableitung; CPs Korrektur und Optimierung
 * 
 * Revision 1.34 2004/06/14 12:34:19 olga CP Analyse and Transformation
 * 
 * Revision 1.33 2004/06/09 11:32:54 olga Attribute-Eingebe/Bedingungen : NAC
 * kann jetzt eigene Variablen und Bedingungen haben. CP Berechnung korregiert.
 * 
 * Revision 1.32 2004/04/19 11:39:30 olga Graphname als String ohne Blanks
 * 
 * Revision 1.31 2004/03/29 12:15:25 olga Tests
 * 
 * Revision 1.30 2004/03/26 12:44:12 olga ...
 * 
 * Revision 1.29 2004/03/25 12:34:04 olga ....
 * 
 * Revision 1.28 2004/03/18 17:41:53 olga
 * 
 * rrektur an CPs und XML converter
 * 
 * Revision 1.27 2004/03/08 10:19:20 olga tests
 * 
 * Revision 1.26 2004/03/04 16:18:09 olga CP test
 * 
 * Revision 1.25 2003/06/18 15:17:29 olga Tests
 * 
 * Revision 1.24 2003/03/17 15:36:01 olga Xread, Xwrite erweitert
 * 
 * Revision 1.23 2003/03/06 14:45:35 olga Tests
 * 
 * Revision 1.22 2003/03/05 18:24:09 komm sorted/optimized import statements
 * 
 * Revision 1.21 2003/03/05 14:53:35 olga CP optimierung
 * 
 * Revision 1.20 2003/03/03 17:46:16 olga Optimierung
 * 
 * Revision 1.19 2003/02/27 08:51:54 olga r1DeletesR2Uses geaendert
 * 
 * Revision 1.18 2003/02/26 17:20:40 olga r1ProducesR2Forbids jetzt ohne Step
 * mit r2
 * 
 * Revision 1.17 2003/02/26 10:08:26 olga r1DeletesR2Used geaendert
 * 
 * Revision 1.16 2003/02/24 17:51:22 olga Berechnung getestet
 * 
 * Revision 1.15 2003/02/24 13:28:38 olga NACs
 * 
 * Revision 1.14 2003/02/20 17:38:53 olga NAC tests
 * 
 * Revision 1.13 2003/02/13 15:08:21 olga NACs bei CPs
 * 
 * Revision 1.12 2003/02/03 17:49:31 olga Tests
 * 
 * Revision 1.11 2003/01/20 12:11:46 olga Tests raus
 * 
 * Revision 1.10 2003/01/15 16:31:03 olga Match.isValid(boolean allowVariables)
 * wird benutzt
 * 
 * Revision 1.9 2003/01/15 11:37:06 olga Kleine Aenderung
 * 
 * Revision 1.8 2002/12/20 11:26:33 olga Tests
 * 
 * Revision 1.7 2002/12/09 17:53:56 olga Graph name - Verbesserung
 * 
 * Revision 1.6 2002/11/11 10:43:25 komm added support for multiplicity check
 * 
 * Revision 1.5 2002/11/07 16:04:17 olga Tests
 * 
 * Revision 1.4 2002/10/02 18:30:57 olga XXX
 * 
 * Revision 1.3 2002/09/30 10:32:19 komm added TypeException handling
 * 
 * Revision 1.2 2002/09/26 13:59:16 olga Folgeaenderung
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:23 olga Imported sources
 * 
 * Revision 1.13 2001/08/16 14:14:08 olga LayerFunction erweitert:
 * ExtendedLayerFunction erbt LayerFunction (checkLayer ueberschrieben)
 * WeakLayerFunction erbt LayerFunction ( checkLayer ueberschrieben)
 * WeakExtendedLayerFunction erbt WeakLayerFunction ( checkLayer ueberschrieben)
 * 
 * Revision 1.12 2001/08/02 15:22:15 olga Error-Meldungen eingebaut in
 * LayerFunction und die Anzeige dieser Meldungen in GUI.
 * 
 * Revision 1.11 2001/07/04 10:45:33 olga Mehr Testarbeit.
 * 
 * Revision 1.10 2001/06/26 17:28:03 olga Parser test
 * 
 * Revision 1.9 2001/06/18 13:37:46 olga Bei Critical Pair ein neuer Menuitem:
 * Debug, wo man einzelne Regelpaare testen kann. System.gc() eingefuegt.
 * 
 * Revision 1.8 2001/06/13 16:49:34 olga Parser Classen Optimierung.
 * 
 * Revision 1.7 2001/05/14 12:02:59 olga Zusaetzliche Parser Events Aufrufe
 * eingebaut, um bessere Kommunikation mit GUI Status Anzeige zu ermoeglichen.
 * 
 * Revision 1.6 2001/04/11 14:59:18 olga Stop Method eingefuegt.
 * 
 * Revision 1.5 2001/03/29 12:01:33 olga MorphCompletionStrategy: dangling
 * condition wieder ON
 * 
 * Revision 1.4 2001/03/22 15:53:25 olga Zusaetzliche Events Meldungen
 * eingebaut.
 * 
 * Revision 1.3 2001/03/08 10:42:50 olga Die Parser Version aus parser branch
 * wurde in Head uebernommen.
 * 
 * Revision 1.1.2.16 2001/01/28 13:14:51 shultzke API fertig
 * 
 * Revision 1.1.2.15 2001/01/03 09:44:59 shultzke TODO's bis auf laden und
 * speichern erledigt. Wann meldet sich endlich Michael?
 * 
 * Revision 1.1.2.14 2001/01/01 21:24:32 shultzke alle Parser fertig inklusive
 * Layout
 * 
 * Revision 1.1.2.13 2000/12/10 20:26:08 shultzke Parser kann XML
 * 
 * Revision 1.1.2.12 2000/12/10 14:55:48 shultzke um Layer erweitert
 * 
 * Revision 1.1.2.11 2000/10/25 13:43:37 shultzke parser start dialog
 * 
 * Revision 1.1.2.10 2000/09/28 12:32:21 shultzke Attribute verbessert
 * 
 * Revision 1.1.2.9 2000/09/27 13:21:09 shultzke bis auf die Attribute scheint
 * die Excludebeziehung zu stehen
 * 
 * Revision 1.1.2.8 2000/09/26 13:01:49 shultzke OverlapSetBerechnung erweitert
 * 
 * Revision 1.1.2.7 2000/09/25 13:52:52 shultzke Report.trace veraendert
 * ExcludePair: Attribute in exclude aufgenommen. Fast vollstaendig
 * implementiert
 * 
 * Revision 1.1.2.6 2000/09/21 14:02:19 shultzke ExcludePair bei nacs erweitert
 * 
 * Revision 1.1.2.5 2000/07/19 08:24:14 shultzke jetzt wieder uebersetzbar
 * 
 * Revision 1.1.2.4 2000/07/18 20:03:53 shultzke Exclude ohne Nac sollten
 * funktionieren
 * 
 * Revision 1.1.2.3 2000/07/17 16:12:43 shultzke exlude berechnung verschluckt
 * stdout und rechnet nicht richtig
 * 
 * Revision 1.1.2.2 2000/07/16 18:52:26 shultzke *** empty log message ***
 * 
 * Revision 1.1.2.1 2000/07/12 07:58:36 shultzke merged
 * 
 * Revision 1.2 2000/07/10 15:09:37 shultzke additional representtion
 * hinzugefuegt
 * 
 */

