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

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import agg.attribute.AttrType;
import agg.attribute.impl.CondMember;
import agg.attribute.impl.CondTuple;
import agg.attribute.impl.ValueTuple;
import agg.attribute.impl.ValueMember;
import agg.xt_basis.Arc;
import agg.xt_basis.BadMappingException;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.CompletionStrategySelector;
import agg.xt_basis.ConcurrentRule;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.InverseRuleConstructData;
import agg.xt_basis.Match;
import agg.xt_basis.MorphCompletionStrategy;
import agg.xt_basis.Node;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.TestStep;
import agg.xt_basis.Type;
import agg.xt_basis.TypeError;
import agg.xt_basis.TypeException;
import agg.xt_basis.TypeSet;
import agg.util.Pair;

// ****************************************************************************+
/**
 * This class implements an algorithm to decide whether two rules are dependent:
 * the first rule enables the second.
 * 
 * @author $Author: olga $
 */
public class DependencyPair extends ExcludePair {

	/*
	 * There is to check two morphisms of rule independency:
	 * first: existing of morphism i: L2 -> D1,
	 * if it doesn't exist, we have trigger dependency of (r1, r2);  
	 * second: existing of morphism j: R1 -> D2,
	 * if it doesn't exist, we have switch dependency of (r1, r2).
	 */
	
	protected boolean switchDependency;
	
	protected boolean makeConcurrentRules;
	
	protected boolean completeConcurrency = true;
	
	protected boolean maxOverlapping;
	
	protected List<ConcurrentRule> concurrentRules;
	
	// isoRight: R -> Rcopy  of Rule r1
	protected OrdinaryMorphism isoRight1; 
	 
//	 isoLeft: L -> Lcopy  of Rule r1
	protected OrdinaryMorphism isoLeft1;	
	
	// key: NAC, value: Pair
	// Pair.first: L -> Lcopy+NAC, 
	// Pair.second: NAC -> Lcopy+NAC
	protected Hashtable<OrdinaryMorphism, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
	leftNAC2extLeft;
	
	// key: PAC, value: Pair
	// Pair.first: L -> Lcopy+PAC, 
	// Pair.second: PAC -> Lcopy+PAC
	protected Hashtable<OrdinaryMorphism, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
	leftPAC2extLeft;
	
	/**
	 * Creates a new object to compute critical pairs.
	 */
	public DependencyPair() {
		super();
	}

	public void dispose() {		
		super.dispose();
	}

	public void enableSwitchDependency(boolean b) {
		this.switchDependency = b;
	}
	
	public void enableProduceConcurrentRule(boolean b) {
		this.makeConcurrentRules = b;
	}
	
	public void setCompleteConcurrency(boolean b) {
		this.completeConcurrency = b;
		this.maxOverlapping = !this.completeConcurrency;
	}
	
	public List<ConcurrentRule> getConcurrentRules() {
		return this.concurrentRules;
	}
	
	/**
	 * Makes abstract inverse rule r_1 : r.RHS -> r.LHS. 
	 * That means: 
	 * - copy r.RHS to be LHS of the result rule, 
	 * - copy t.LHS to be RHS of the result rule, 
	 * - convert rule morphism,
	 * - convert NACs and PACs,
	 * - replace attr. expression of RHS by variable, 
	 * - convert attr. conditions. 
	 * Note: the specified rule r has to be injective,
	 *  otherwise returns null.
	 */
	protected Rule makeInverseRule(final Rule r) {
		InverseRuleConstructData inverseConstruct = r.getInverseConstructData();
		if (inverseConstruct != null) {
			Rule inverseRule = inverseConstruct.getInverseRule();
			if (inverseRule != null) { 
				inverseRule.isReadyToTransform();
				this.isoLeft1 = inverseConstruct.getLorig2Rinv();
				this.isoRight1 = inverseConstruct.getRorig2Linv();
				if (r.hasNACs()) 
					this.leftNAC2extLeft = inverseConstruct.getNACsStore();
				if (r.hasPACs()) 
					this.leftPAC2extLeft = inverseConstruct.getPACsStore();
				if (!inverseConstruct.isExtended()) {
					if (extendAbstractInverseRule(r, inverseRule, this.isoRight1)) {
						inverseConstruct.setExtended(true);						
						// call this method to mark variables and conditions of the inverse rule
						inverseRule.isReadyToTransform();	
						return inverseRule;
					}
					else
						return null;
				}	
				return inverseRule;
			}
		}
		return null;
	}
		
	
	private void convertNACsLeft2Right(
			final Rule r, 
			final Rule inverseRule,
			final OrdinaryMorphism isoRHS) {		
		final List<OrdinaryMorphism> nacs = r.getNACsList();
		for (int i=0; i<nacs.size(); i++) {
			final OrdinaryMorphism nacL = nacs.get(i);
			if (r.isACShiftPossible(nacL)) {
				int tglevelcheck = r.getTypeSet().getLevelOfTypeGraphCheck();
				r.getTypeSet().setLevelOfTypeGraph(TypeSet.ENABLED);				
				final OrdinaryMorphism nacR = convertNACLeft2Right(r, nacL);
				r.getTypeSet().setLevelOfTypeGraph(tglevelcheck);
				if (nacR != null) {
					Collection<TypeError> error = r.getTypeSet().checkType(nacR.getTarget());
					if (error == null || error.isEmpty()) {
						final OrdinaryMorphism inverseNAC = BaseFactory.theFactory().createMorphism(
								inverseRule.getLeft(),
								nacR.getTarget());				
						if (nacR.completeDiagram(isoRHS, inverseNAC)) {	
							if (!nacR.isRightTotal()
									|| !nacR.doesIgnoreAttrs()) {
								inverseNAC.setName(nacR.getName());
								inverseNAC.setEnabled(nacL.isEnabled());
								inverseRule.addNAC(inverseNAC);
							}
						}
					}
					else {
						nacR.dispose(false, true);
					}
				}
			}
		}
	}
	
	private void replaceAttrValueFromTo(final GraphObject from, final GraphObject to) {
		if (from.getAttribute() != null && to.getAttribute() != null) {
			ValueTuple valuefrom = (ValueTuple) from.getAttribute();
			ValueTuple valueto = (ValueTuple) to.getAttribute();
			for (int i = 0; i < valuefrom.getSize(); i++) {
				ValueMember vmfrom = valuefrom.getValueMemberAt(i);
				ValueMember vmto = valueto.getValueMemberAt(i);
				if (vmto != null) {
					if (!vmfrom.isSet()) {
						vmto.setExpr(null);					
					} else {
						vmto.setExprAsText(vmfrom.getExprAsText());
					}
					if (!vmto.isTransient())
						vmto.setTransient(vmfrom.isTransient());
				}
			}
		}
	}
	
	private OrdinaryMorphism convertNACLeft2Right(
			final Rule r,
			final OrdinaryMorphism nac) {
		
		// isoLHS: L -> Lcopy
		OrdinaryMorphism isoLHS = r.getLeft().isomorphicCopy();
		if (isoLHS == null)
			return null;
		
		// extLHSbyNAC: NAC -> Lcopy+NAC
		OrdinaryMorphism extLHSbyNAC = extendLeftGraphByNAC(isoLHS, nac, false);
		// extLHSbyNAC.getSource() == nac.getTarget()
		applyAttrContextOfAC(extLHSbyNAC, nac);
		
		Graph extLeftGraph = extLHSbyNAC.getTarget();
		Match m = (BaseFactory.theFactory()).createMatch(r, extLeftGraph, true, "1");
		// extLeftGraph is the graph m.getTarget() 
		m.getTarget().setCompleteGraph(false);
//		m.setCompletionStrategy(new Completion_InjCSP(), true);
		// set match mapping
		Iterator<Node> lhsNodes = r.getLeft().getNodesSet().iterator();
		while (lhsNodes.hasNext()) {
			Node n = lhsNodes.next();
			Node img = (Node) isoLHS.getImage(n);
			if (img != null) {
				try {
					m.addMapping(n, img);
				} catch (BadMappingException ex) {
					m.dispose();
					extLHSbyNAC.dispose();
					isoLHS.dispose(false, true);
					return null;
				}
			} 
		}
		Iterator<Arc> lhsArcs = r.getLeft().getArcsSet().iterator();
		while (lhsArcs.hasNext()) {
			Arc a = lhsArcs.next();
			Arc img = (Arc) isoLHS.getImage(a);
			if (img != null) {
				try {
					m.addMapping(a, img);
				} catch (BadMappingException ex) {
					m.dispose();
					extLHSbyNAC.dispose();
					isoLHS.dispose(false, true);
					return null;
				}
			} 
		}
		
		if (!m.isTotal() || !m.isDanglingSatisfied()) {
			m.dispose();
			extLHSbyNAC.dispose();
			isoLHS.dispose(false, true);
			return null;
		}
					
		// make pushout (r, m) to extend (L+NAC) by new objects 
		// and so to get (R+NAC)
		try {
			OrdinaryMorphism nacR = (OrdinaryMorphism) TestStep.execute(m, true, this.equalVariableNameOfAttrMapping);
			
			// now remove objects belonging to R only
			// first remove arcs
			Iterator<Arc> rhsArcs = r.getTarget().getArcsSet().iterator();
			while (rhsArcs.hasNext()) {
				Arc a = rhsArcs.next();
				Arc aImg = (Arc) nacR.getImage(a);
				// delete arc to be created or without a mapping
				// from its preimage into the nacL
				if (!r.getInverseImage(a).hasMoreElements()
						|| (nac.getImage(r.getInverseImage(a).nextElement()) == null)) {
					try {
						nacR.getTarget().destroyArc(aImg, false, false);
					} catch (TypeException ex) {
						nacR.dispose();
						m.dispose();
						extLHSbyNAC.dispose();
						isoLHS.dispose(false, true);
						return null;
					}
				} 
			}
//			remove nodes	
			Iterator<Node> rhsNodes = r.getTarget().getNodesSet().iterator();
			while (rhsNodes.hasNext()) {
				Node n = rhsNodes.next();
				Node nImg = (Node) nacR.getImage(n);
				// delete node to be created or without a mapping
				// from its preimage into the nacL
				if (!r.getInverseImage(n).hasMoreElements()
						|| (nac.getImage(r.getInverseImage(n).nextElement()) == null)) {
					try {
						nacR.getTarget().destroyNode(nImg, false, false);
					} catch (TypeException ex) {
						nacR.dispose();
						m.dispose();
						extLHSbyNAC.dispose();
						isoLHS.dispose(false, true);
						return null;
					}	
				}
			}
			
			// replace attr value of the converted NAC
			lhsNodes = r.getLeft().getNodesSet().iterator();
			while (lhsNodes.hasNext()) {
				Node n = lhsNodes.next();
				Node nacNode = (Node) nac.getImage(n);
				Node convertedNode = (Node) isoLHS.getImage(n);				
				if (convertedNode != null 
						&& nacNode != null) {						
					// replace attr value of img by attr value of original left NAC					
					replaceAttrValueFromTo(nacNode, convertedNode);	
				}
			}			
			lhsArcs = r.getLeft().getArcsSet().iterator();
			while (lhsArcs.hasNext()) {
				Arc a = lhsArcs.next();
				Arc convertedArc = (Arc) isoLHS.getImage(a);	
				Arc nacArc = (Arc) nac.getImage(a);
				if (convertedArc != null && nacArc != null) {					
					replaceAttrValueFromTo(nacArc, convertedArc);					
				}
			}
						
			
			// we have a new right NAC
			nacR.setName(nac.getName());
			// save help info
			if (this.leftNAC2extLeft != null)
				this.leftNAC2extLeft.put(nac, new Pair<OrdinaryMorphism, OrdinaryMorphism>(isoLHS, extLHSbyNAC));
						
			return nacR;	
				
		} catch (TypeException tex) {
			m.dispose();
			extLHSbyNAC.dispose();
			isoLHS.dispose(false, true);
		}		
		return null;		
	}
	
	private void applyAttrContextOfAC(
			final OrdinaryMorphism extLHSbyCond, 
			final OrdinaryMorphism cond) {
		
		doApplyAttrContextOfNAC_PAC(extLHSbyCond, cond.getTarget().getNodesSet().iterator()); 
		doApplyAttrContextOfNAC_PAC(extLHSbyCond, cond.getTarget().getArcsSet().iterator()); 		
	}
	
	private void doApplyAttrContextOfNAC_PAC(
			final OrdinaryMorphism extLHSbyCond, 
			final Iterator<?> objs) {
				
		while (objs.hasNext()) {
			final GraphObject src = (GraphObject) objs.next();
			final GraphObject img = extLHSbyCond.getImage(src);
			if (src.getAttribute() == null || img == null)
				continue;
			
			final ValueTuple srcvt = (ValueTuple) src.getAttribute();			
			final ValueTuple imgvt = (ValueTuple) img.getAttribute();
			for (int i = 0; i < srcvt.getNumberOfEntries(); i++) {
				final ValueMember srcvm = srcvt.getValueMemberAt(i);
				if (srcvm.isSet()) {
					if (srcvm.getExpr().isVariable()
							|| srcvm.getExpr().isConstant()) {							
						final ValueMember imgvm = imgvt.getValueMemberAt(i);
						if (imgvm != null)
							imgvm.setExprAsText(srcvm.getExprAsText());	
					}
				}
			}
		}
	}
	
	private boolean convertPACsLeft2Right(
			final Rule r, 
			final Rule inverseRule,
			final OrdinaryMorphism isoRHS) {
		
		final List<OrdinaryMorphism> pacs = r.getPACsList();
		for (int i=0; i<pacs.size(); i++) {
			final OrdinaryMorphism pacL = pacs.get(i);	
			if (pacL.isEnabled() && r.isACShiftPossible(pacL)) {
				int tglevelcheck = r.getTypeSet().getLevelOfTypeGraphCheck();
				r.getTypeSet().setLevelOfTypeGraph(TypeSet.ENABLED);
				final OrdinaryMorphism pacR = convertPACLeft2Right(r, pacL);
				r.getTypeSet().setLevelOfTypeGraph(tglevelcheck);
				if (pacR != null) {
					Collection<TypeError> error = r.getTypeSet().checkType(pacR.getTarget());
					if (error == null || error.isEmpty()) {
						final OrdinaryMorphism inversePAC = BaseFactory.theFactory().createMorphism(
								inverseRule.getLeft(),
								pacR.getTarget());				
						if (pacR.completeDiagram(isoRHS, inversePAC)) {	
							if (!pacR.isRightTotal()
									|| !pacR.doesIgnoreAttrs()) {
								inversePAC.setName(pacR.getName());
								inversePAC.setEnabled(pacL.isEnabled());
								inverseRule.addPAC(inversePAC);
							}
						}
					} else {
						pacR.dispose(false, true);
						return false;
					}
				} else
					return false;	
			} else
				return false;	
		}
		return true;
	}
		
	private OrdinaryMorphism convertPACLeft2Right(
			final Rule r,
			final OrdinaryMorphism pac) {
		
		// isoLHS: L -> Lcopy
		OrdinaryMorphism isoLHS = r.getLeft().isomorphicCopy();
		if (isoLHS == null)
			return null;
		
		// extLHSbyPAC: NAC -> Lcopy+PAC
		OrdinaryMorphism extLHSbyPAC = extendLeftGraphByPAC(isoLHS, pac, false).second;
		// extLHSbyPAC.getSource() == pac.getTarget()
		applyAttrContextOfAC(extLHSbyPAC, pac);
		
		Graph extLeftGraph = extLHSbyPAC.getTarget();
		Match m = (BaseFactory.theFactory()).createMatch(r, extLeftGraph, true, "1");
		// extLeftGraph is the graph m.getTarget() 
		m.getTarget().setCompleteGraph(false);
//		m.setCompletionStrategy(new Completion_InjCSP(), true);
		// set match mapping
		Iterator<Node> lhsNodes = r.getLeft().getNodesSet().iterator();
		while (lhsNodes.hasNext()) {
			Node n = lhsNodes.next();
			Node img = (Node) isoLHS.getImage(n);
			if (img != null) {
				try {
					m.addMapping(n, img);
				} catch (BadMappingException ex) {
					m.dispose();
					extLHSbyPAC.dispose();
					isoLHS.dispose(false, true);
					return null;
				}
			}
		}
		Iterator<Arc> lhsArcs = r.getLeft().getArcsSet().iterator();
		while (lhsArcs.hasNext()) {
			Arc a = lhsArcs.next();
			Arc img = (Arc) isoLHS.getImage(a);
			if (img != null) {
				try {
					m.addMapping(a, img);
				} catch (BadMappingException ex) {
					m.dispose();
					extLHSbyPAC.dispose();
					isoLHS.dispose(false, true);
					return null;
				}
			}
		}
		
		if (!m.isTotal() || !m.isDanglingSatisfied()) {
			m.dispose();
			extLHSbyPAC.dispose();
			isoLHS.dispose(false, true);
			return null;
		}
			
		try {
			OrdinaryMorphism pacR = (OrdinaryMorphism) TestStep.execute(m, true, this.equalVariableNameOfAttrMapping);
				
			Iterator<Arc> rhsArcs = r.getTarget().getArcsSet().iterator();
			while (rhsArcs.hasNext()) {
				Arc a = rhsArcs.next();
				Arc aImg = (Arc) pacR.getImage(a);
				// delete arc to be created or without a mapping
				// from its preimage into the pacL
				if (!r.getInverseImage(a).hasMoreElements()
						|| (pac.getImage(r.getInverseImage(a).nextElement()) == null)) {
					try {
						pacR.getTarget().destroyArc(aImg, false, false);
					} catch (TypeException tex) {
						pacR.dispose();
						m.dispose();
						extLHSbyPAC.dispose();
						isoLHS.dispose(false, true);
						return null;
					}
				} 
			}
				
			Iterator<Node> rhsNodes = r.getTarget().getNodesSet().iterator();
			while (rhsNodes.hasNext()) {
				Node n = rhsNodes.next();
				Node nImg = (Node) pacR.getImage(n);
//				// delete node to be created or without a mapping
				// from its pre-image into the pacL
				if (!r.getInverseImage(n).hasMoreElements()
						|| (pac.getImage(r.getInverseImage(n).nextElement()) == null)) {
					try {
						pacR.getTarget().destroyNode(nImg, false, false);
					} catch (TypeException tex) {
						pacR.dispose();
						m.dispose();
						extLHSbyPAC.dispose();
						isoLHS.dispose(false, true);
						return null;
					}	
				}
			}
			
			// replace attr value of the converted NAC
			lhsNodes = r.getLeft().getNodesSet().iterator();
			while (lhsNodes.hasNext()) {
				Node n = lhsNodes.next();
				Node pacNode = (Node) pac.getImage(n);
				Node convertedNode = (Node) isoLHS.getImage(n);				
				if (convertedNode != null 
						&& pacNode != null) {						
					// replace attr value of img by attr value of original left NAC					
					replaceAttrValueFromTo(pacNode, convertedNode);	
				}
			}			
			lhsArcs = r.getLeft().getArcsSet().iterator();
			while (lhsArcs.hasNext()) {
				Arc a = lhsArcs.next();
				Arc convertedArc = (Arc) isoLHS.getImage(a);	
				Arc pacArc = (Arc) pac.getImage(a);
				if (convertedArc != null && pacArc != null) {					
					replaceAttrValueFromTo(pacArc, convertedArc);					
				}
			}
			
			pacR.setName(pac.getName());
			if (this.leftPAC2extLeft != null)
				this.leftPAC2extLeft.put(pac, new Pair<OrdinaryMorphism, OrdinaryMorphism>(isoLHS, extLHSbyPAC));								
			return pacR;
				
		} catch (TypeException tex) {
			m.dispose();
			extLHSbyPAC.dispose();
			isoLHS.dispose(false, true);
		}
		return null;		
	}

	private void addAttrConditionFromTo(Rule r, Rule inverseR) {	
		CondTuple conds = (CondTuple) r.getAttrContext().getConditions();
		if (conds.isEmpty())
			return;
						
		CondTuple condsOfInverseR = (CondTuple) inverseR.getAttrContext().getConditions();				
		for (int i=0; i<conds.getNumberOfEntries(); i++) {
			CondMember cond = conds.getCondMemberAt(i);
			CondMember cm = (CondMember) condsOfInverseR.addCondition(cond.getExprAsText());
			cm.setMark(cond.getMark());
		}
	}

	/*
	private void convertAttrConditionFromLeft2Right(Rule r, Rule inverseR) {	
		CondTuple conds = (CondTuple) r.getAttrContext().getConditions();
		if (conds.isEmpty())
			return;
				
		VarTuple vars = (VarTuple) r.getAttrContext().getVariables();
		
		CondTuple condsOfInverseR = (CondTuple) inverseR.getAttrContext().getConditions();		
		VarTuple varsOfInverseR = (VarTuple) inverseR.getAttrContext().getVariables();
		
		Hashtable<VarMember, Boolean>
		varLeftRight = new Hashtable<VarMember, Boolean>();
		for (int i=0; i<vars.getNumberOfEntries(); i++) {
			VarMember var = vars.getVarMemberAt(i);
			if (var.getMark() == VarMember.LHS) {
				if (variableUsed(var, r.getLeft()) ){
					VarMember varInverse = varsOfInverseR.getVarMemberAt(var.getName());
					if (variableUsed(var, r.getRight())) {
						varLeftRight.put(var, Boolean.valueOf(true));
						varInverse.setMark(VarMember.LHS);
					}
					else {
						varLeftRight.put(var, Boolean.valueOf(false));
						varInverse.setMark(VarMember.RHS);
					}
				}
			}
		}
		
		for (int i=0; i<conds.getNumberOfEntries(); i++) {
			CondMember cond = conds.getCondMemberAt(i);
			Vector<String> condVars = cond.getAllVariables();
			boolean varsOK = true;
			for (int j=0; j<condVars.size(); j++) {
				String varN = condVars.get(j);
				VarMember var = vars.getVarMemberAt(varN);
				if (var.getMark() == VarMember.LHS) {
					if (!varLeftRight.get(var).booleanValue()) 
						varsOK = false;
				}
			}
			if (varsOK) {
				CondMember cm = (CondMember) condsOfInverseR.addCondition(cond.getExprAsText());
				cm.setMark(cond.getMark());
			}
		}
	}
	 
	
	private boolean variableUsed(VarMember var, Graph g) {
		Iterator<Node> nodes = g.getNodesSet().iterator();
		while (nodes.hasNext()) {
			GraphObject obj = nodes.next();
			if (obj.getAttribute() == null) 
				continue;
			ValueTuple val = (ValueTuple) obj.getAttribute();
			for (int i = 0; i < val.getSize(); i++) {
				ValueMember vm = val.getValueMemberAt(i);
				if (vm.isSet() && vm.getExpr().isVariable()) {
					if (vm.getExprAsText().equals(var.getName()))
							return true;
				}
			}			
		}
		Iterator<Arc> arcs = g.getArcsSet().iterator();
		while (arcs.hasNext()) {
			GraphObject obj = arcs.next();
			if (obj.getAttribute() == null) 
				continue;
			ValueTuple val = (ValueTuple) obj.getAttribute();
			for (int i = 0; i < val.getSize(); i++) {
				ValueMember vm = val.getValueMemberAt(i);
				if (vm.isSet() && vm.getExpr().isVariable()) {
					if (vm.getExprAsText().equals(var.getName()))
							return true;
				}
			}			
		}
		return false;
	}
	*/
	
	private void adjustOverlappings(
			final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> overlaps,
			final Rule r1, // second rule of this DependencyPair
			final Rule inv_r2, // inverse rule of first rule 
			final Rule orig_r2, // first rule of this DependencyPair
			int count) {
//		System.out.println("DependencyPair.adjustOverlappings...");
//		System.out.println("#### r1 (second)      : "+r1.getLeft().hashCode()+"  "+r1.getRight().hashCode());
//		System.out.println("#### orig r2 (first)  : "+orig_r2.getLeft().hashCode()+"  "+orig_r2.getRight().hashCode());
//		System.out.println("#### inverse r2       : "+inv_r2.getLeft().hashCode()+"  "+inv_r2.getRight().hashCode());
		
		for (int i = 0; i < overlaps.size(); i++) {
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			pi = overlaps.get(i);

//			m1: LHS_R2 -> G
//			OrdinaryMorphism m1 = pi.first.first;
//			m2: RHS_R1 -> G
			OrdinaryMorphism m2 = pi.first.second;
							
			// rename the overlap graph 
			String nb = "( "+(count+1+i)+" ) ";
			
			if (m2.getTarget().getName().indexOf("delete-")>=0) {
//				m2.getTarget().setName(nb+"deliver-delete-dependency");
				m2.getTarget().setName(nb+CriticalPairData.PRODUCE_DELETE_D_TXT);
								
				// handle PACs
//				final List<OrdinaryMorphism> pacs = orig_r2.getPACsList();
//				for (int j=0; j<pacs.size(); j++) 
				{
//					final OrdinaryMorphism pac = pacs.get(j);				
//					OrdinaryMorphism isoLHS = this.leftPAC2extLeft.get(pac).first;
//					OrdinaryMorphism extLHS = this.leftPAC2extLeft.get(pac).second;
					
					if (pi.second != null) {	
						// pi.second.first: LHS_invR2 -> LHS_invR2copy+PAC
						if (pi.second.first.getSource() == inv_r2.getLeft()) {					
							OrdinaryMorphism isoRHSnew = BaseFactory.theFactory().createMorphism(
									this.isoRight1.getSource(), pi.second.first.getTarget());						
						
							Enumeration<GraphObject> objs = this.isoRight1.getDomain();
							while (objs.hasMoreElements()) {
								GraphObject obj = objs.nextElement();
								GraphObject img = this.isoRight1.getImage(obj);													
								GraphObject img2 = pi.second.first.getImage(img);
								if (img2 != null) {								
									try{
										isoRHSnew.addMapping(obj, img2);
									} catch (BadMappingException ex) {
//										System.out.println(ex.getStackTrace());
									}
								}
							}
//							pi.second.first: RHS_R2 -> LHS_invR2copy+PAC
							pi.second.first = isoRHSnew;							
						}
					}				
				}				
			} 
			else if (m2.getTarget().getName().indexOf("-forbid")>=0) {
//				m2.getTarget().setName(nb+"forbid-produce-dependency");
				m2.getTarget().setName(nb+CriticalPairData.FORBID_PRODUCE_D_TXT);
				
//				handle NACs				
				final List<OrdinaryMorphism> nacs = orig_r2.getNACsList();
				for (int j=0; j<nacs.size(); j++) {
					final OrdinaryMorphism nac = nacs.get(j);
					
//					OrdinaryMorphism isoLHS = this.leftNAC2extLeft.get(nac).first;
					OrdinaryMorphism extLHS = (this.leftNAC2extLeft != null
												&& this.leftNAC2extLeft.get(nac)!=null) ? 
												this.leftNAC2extLeft.get(nac).second : null;
					
					if (extLHS != null
							&& extLHS.getTarget() == pi.second.second.getSource()) {
						
						final OrdinaryMorphism isoRHSnew = BaseFactory.theFactory().createMorphism(
								this.isoRight1.getSource(), pi.second.first.getTarget());						
						
						Enumeration<GraphObject> objs = this.isoRight1.getDomain();
						while (objs.hasMoreElements()) {
							final GraphObject obj = objs.nextElement();
							final GraphObject img = this.isoRight1.getImage(obj);													
							final GraphObject img2 = pi.second.first.getImage(img);
							if (img2 != null) {								
								try{
									isoRHSnew.addMapping(obj, img2);
								} catch (BadMappingException ex) {
//									System.out.println(ex.getStackTrace());
								}
							}
						}
						pi.second.first = isoRHSnew;
						
						final OrdinaryMorphism isoNACnew = BaseFactory.theFactory().createMorphism(
								extLHS.getSource(), pi.second.second.getTarget());
						
						objs = extLHS.getDomain();
						while (objs.hasMoreElements()) {
							final GraphObject obj = objs.nextElement();
							final GraphObject img = extLHS.getImage(obj);
							final GraphObject img2 = pi.second.second.getImage(img);
							if (img2 != null) {								
								try{
									isoNACnew.addMapping(obj, img2);
								} catch (BadMappingException ex) {
//									System.out.println(ex.getStackTrace());
								}
							}						
						}
						pi.second.second = isoNACnew;
					}
				}				
			}
			else if (m2.getTarget().getName().indexOf("change-")>=0) {
//				m2.getTarget().setName(nb+"change-change-dependency");
				m2.getTarget().setName(nb+CriticalPairData.PRODUCE_CHANGE_D_TXT);
				
				// handle PACs
//				final List<OrdinaryMorphism> pacs = orig_r2.getPACsList();
//				for (int j=0; j<pacs.size(); j++) 
				{
//					final OrdinaryMorphism pac = pacs.get(j);				
//					final OrdinaryMorphism isoLHS = this.leftPAC2extLeft.get(pac).first;
//					final OrdinaryMorphism extLHS = this.leftPAC2extLeft.get(pac).second;
					
					if (pi.second != null) {	
						if (pi.second.first.getSource() == inv_r2.getLeft()) {
							
							OrdinaryMorphism isoRHSnew = BaseFactory.theFactory().createMorphism(
									this.isoRight1.getSource(), pi.second.first.getTarget());						
							
							Enumeration<GraphObject> objs = this.isoRight1.getDomain();
							while (objs.hasMoreElements()) {
								GraphObject obj = objs.nextElement();
								GraphObject img = this.isoRight1.getImage(obj);													
								GraphObject img2 = pi.second.first.getImage(img);
								if (img2 != null) {								
									try{
										isoRHSnew.addMapping(obj, img2);
									} catch (BadMappingException ex) {
//										System.out.println(ex.getStackTrace());
									}
								}
							}
							pi.second.first = isoRHSnew;						
						}
					}				
				}				
			}
		}
	}


	// ****************************************************************************+
	/**
	 * computes critical pairs of the specified kind. 
	 * Returns null if the specified rule pair is not critical, otherwise an object which can
	 * explain in which way this pair is critical. One possible object can be a
	 * <code>Vector</code> of overlapping graphs. If a kind is requested
	 * which cannot be computed an <code>InvalidAlgorithmException</code> is
	 * thrown.
	 * 
	 * @param kind
	 *            specifies the kind of critical pair
	 *            CriticalPair.
	 * @param r1
	 *            defines the first part which can be critical
	 * @param r2
	 *            the second part which can be critical
	 * @throws InvalidAlgorithmException
	 *             Thrown if a illegal algorithm is selected.
	 * @return The object which is critic of the two rules
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
			else if (kind == CONFLICTFREE) {
				return null;
			}
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
		
		Rule inverseR1 = makeInverseRule(r1);
		if (inverseR1 == null) {
			System.out.println("DependencyPair:  CANNOT make inverse rule! "
							+ " Rule  " + r1.getName()
							+ " ( attr problem? PAC? non-injective rule?)");
			this.computable = false;
			return null;
		} 
		
		this.dependencyCond1 = false; // : L2-->D1  NOT EXIST.
		this.dependencyCond2 = false; // : R1-->D2  NOT EXIST.
		this.checkSwitchDependency = false;
		int count = 0;
		if (kind == EXCLUDE) {				
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			result = isExclude(inverseR1, r2);			
			if (result != null) {
				this.dependencyCond1 = true;
				if (this.makeConcurrentRules) {
					this.concurrentRules = makeConcurrentRules(r1, inverseR1, r2, result);
				}
				resetAbstractRHS(result);
				count = result.size();
			}
			
			if (this.switchDependency) {
				this.checkSwitchDependency = true;
				this.makeConcurrentRules = false;
				this.clear();
//				this.unsetProgressIndx();
//				System.out.println("DependencyPair:: Check the second condition of Sequential Independence: R1-->D2");					
				Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
				result2 = isExclude(r2, inverseR1);
				if (result2 != null) {
					this.dependencyCond2 = true;
					resetAbstractRHS2(result2);
					adjustOverlappings(result2, r2, inverseR1, r1, count);
					if (result != null)
						result.addAll(result2);
					else
						result = result2;
				}
				this.checkSwitchDependency = false;
			}
			
			return result;
			
		} else if (kind == CONFLICTFREE) {	
			return isExclude(inverseR1, r2);			
		}
		else
			throw new InvalidAlgorithmException("No such Algorithm", kind);
	}

	protected Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	isExclude(final Rule r1, final Rule r2) {
		if (!this.makeConcurrentRules) {
			return super.isExclude(r1, r2);
		}

		// in case of building concurrent rule do this
		// set Type Multiplicity check to be TypeSet.ENABLED_MAX
		this.levelOfTypeGraphCheck = this.grammar.getTypeSet().getLevelOfTypeGraphCheck();
		if (this.levelOfTypeGraphCheck > TypeSet.ENABLED_MAX)
			this.grammar.getTypeSet().setLevelOfTypeGraph(TypeSet.ENABLED_MAX);			
			
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
		
		prepareCriticalPairContextData(r1, r2);

		boolean 
		canLHS1OverlapLHS2 = canMatchConstantAttributeLHS1intoLHS2(r1, r2);

		final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		resultOverlappings = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
			
		// check delete-use conflicts
		if (//!this.stop && 
				!this.contextC1_L1.isEmpty() && canLHS1OverlapLHS2) {
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
			deleteUseOverlappings = this.getDeleteUseConflicts_CR(r1, r2);				
			if (deleteUseOverlappings != null && !deleteUseOverlappings.isEmpty()) {
				resultOverlappings.addAll(deleteUseOverlappings);
				deleteUseOverlappings.clear();
			}
		}
		
		// check change-use attribute conflicts
		if (resultOverlappings.isEmpty()) {
			this.preservedChanged.clear();
			this.contextC1_L1.clear();
			this.boundB1_L1.clear();
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> 
			changedAttrsL1 = new Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>>();	
			// fill preservedChanged vector		
//			if (this.withNACs) {
//				ruleChangesAttributes(this.preservedChanged, r1, r2, this.contextC1_L1, this.boundB1_L1,
//							this.preservedK1_L1, changedAttrsL1, this.typesTG_NAC2);
//			} 
			// fill preservedChanged vector
//			if (this.withPACs) {
//				ruleChangesAttributes(this.preservedChanged, r1, r2, this.contextC1_L1, this.boundB1_L1,
//							this.preservedK1_L1, changedAttrsL1, this.typesTG_PAC2);
//			} else 
			{
				ruleChangesAttributes(this.preservedChanged, r1, r2, this.contextC1_L1, this.boundB1_L1,
							this.preservedK1_L1, changedAttrsL1, this.typesTG_L2);
			}							
//			final Vector<GraphObject> preservedL2_K2 = new Vector<GraphObject>(5);
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> 
			changedAttrsL2 = new Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>>();		
			if (this.preservedChanged.isEmpty()
					|| !ruleRestrictsAttributes(this.strongAttrCheck, r2, changedAttrsL2, changedAttrsL1)) {
				// here do nothing!
			} else {
				Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
				changeAttrOverlappings = getChangeAttributeConflicts(
										r1, r2, changedAttrsL1, changedAttrsL2);					
				if (changeAttrOverlappings != null && !changeAttrOverlappings.isEmpty()) {
					changeAttrOverlappings = this.getMaxCriticalPair(changeAttrOverlappings);
					resultOverlappings.addAll(changeAttrOverlappings);
					changeAttrOverlappings.clear();
				}
			}
		}
		
		// set index of the overlapping graphs		
		for (int i = 0; i < resultOverlappings.size(); i++) {			
			Pair<OrdinaryMorphism, OrdinaryMorphism> 
			morphisms = resultOverlappings.elementAt(i).first;
			int j = i + 1;
			Graph overlap = morphisms.first.getImage();
			unsetAllTransientAttrValuesOfOverlapGrah(overlap);
			String name = overlap.getName();
			if (name.indexOf('(') == 0) {
				int ii = name.indexOf(')');
				if (ii > 0)
					name = overlap.getName().substring(ii+1);
			}
			overlap.setName("( "+j+" ) "+name);
			final String r1Prefix = "r1_";
			final String r2Prefix = "r2_";
			ExcludePairHelper.renameContextVariableOfOverlappingPair(
									r1, r2, morphisms, r1Prefix, r2Prefix);		
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
	
	
	/**
	 * Extends the specified abstract inverse rule <code>inverseR1</code> 
	 * of the rule <code>r1</code> by converting of PACs and NACs 
	 * of the rule <code>r1</code>.
	 * @param r1 original rule
	 * @param inverseR1 its abstract inverse rule
	 * @param isoRHS help isomorphism: RHS of r1 --> copy RHS of r1
	 * @return true if converting was successful, otherwise false 
	 */
	private boolean extendAbstractInverseRule(
			final Rule r, 
			final Rule inverseR, 
			final OrdinaryMorphism isoRHS) {
//		System.out.println("inverse Rule : "+inverseR1.getName()+"  mappings: "+inverseR1.getSize());
		
//		this.leftNAC2extLeft = new Hashtable<OrdinaryMorphism, Pair<OrdinaryMorphism, OrdinaryMorphism>> ();
//		this.leftPAC2extLeft = new Hashtable<OrdinaryMorphism, Pair<OrdinaryMorphism, OrdinaryMorphism>> ();

//		extend inverse r1 by converting of PACs from LHS to RHS
		if (!this.convertPACsLeft2Right(r, inverseR, isoRHS)) {
			return false;
		}
		
//		extend inverse r by converting of NACs from LHS to RHS
		this.convertNACsLeft2Right(r, inverseR, isoRHS);
//		extend inverse r by converting of attr conditions
		this.addAttrConditionFromTo(r, inverseR);
		//test
//		((CondTuple)inverseR1.getAttrContext().getConditions()).showConditions();		
//		System.out.println("inverse Rule : "+inverseR1.getName()+"  mappings: "+inverseR1.getSize());
//		((VarTuple) inverseR1.getAttrContext().getVariables()).showVariables();

		return true;
	}
	
	private void resetAbstractRHS(Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> overlaps) {	
		for (int i = 0; i < overlaps.size(); i++) {
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			pi = overlaps.get(i);
			Pair<OrdinaryMorphism, OrdinaryMorphism> p = pi.first;
			OrdinaryMorphism m1 = p.first;
			OrdinaryMorphism comp1 = null;
			if (m1.getSource() == this.isoRight1.getTarget())
				comp1 = this.isoRight1.compose(m1);
			else if (m1.getSource() == this.isoLeft1.getTarget())
				comp1 = this.isoLeft1.compose(m1);
			// System.out.println(comp1);
			if (comp1 != null){
				if (comp1.getSize() == m1.getSize()) {	
					comp1.setName(m1.getName());
					unsetAllTransientAttrValuesOfOverlapGrah(comp1.getTarget());
					p.first = comp1;
				}
			}
		}
	}

	private void resetAbstractRHS2(Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> overlaps) {	
		for (int i = 0; i < overlaps.size(); i++) {
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			pi = overlaps.get(i);
			Pair<OrdinaryMorphism, OrdinaryMorphism> p = pi.first;
			OrdinaryMorphism m2 = p.second;
			OrdinaryMorphism comp2 = null;
			if (m2.getSource() == this.isoRight1.getTarget())
				comp2 =this. isoRight1.compose(m2);
			else if (m2.getSource() == this.isoLeft1.getTarget())
				comp2 = this.isoLeft1.compose(m2);

			if (comp2 != null){				
				if (comp2.getSize() == m2.getSize()) {	
					comp2.setName(m2.getName());
					unsetAllTransientAttrValuesOfOverlapGrah(comp2.getTarget());
					p.second = comp2;
				}
			}
		}
	}
	
	private ConcurrentRule makeConcurrentRule(
			final Rule originalRule1,
			final Rule inverseRule1,
			final Rule rule2,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> criticalPair) {
//		System.out.println("DependencyPair.makeConcurrentRule  of "+originalRule1.getName()+" & "+rule2.getName());

		ConcurrentRule cr = new ConcurrentRule(originalRule1, inverseRule1, rule2,
				this.isoLeft1, this.isoRight1, criticalPair.first, criticalPair.second);
		return cr;
	}
	
	private List<ConcurrentRule> makeConcurrentRules(
			final Rule originalRule1,
			final Rule inverseRule1,
			final Rule rule2,
			final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> overlaps) {
	
//		System.out.println("DependencyPair.makeConcurrentRules  of "+originalRule1.getName()+" & "+rule2.getName());

		
		final List<ConcurrentRule> list = new Vector<ConcurrentRule>(); 
		
		if (this.completeConcurrency || overlaps.size() >= 1) { //overlaps.size() == 1) {
			for (int i=overlaps.size()-1; i>=0; i--) {
//			for (int i=0; i<overlaps.size(); i++) {
				Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
				pair = overlaps.get(i);
				// to take in account only overlap. graphs without NAC part 
				// in this case:  pair.second != null
				if (pair.second != null) {
					continue;
				}			
			
				Pair<OrdinaryMorphism, OrdinaryMorphism> criticalPair = pair.first;			
				ConcurrentRule conrule = makeConcurrentRule(originalRule1, inverseRule1, rule2, criticalPair);
				if (conrule.getRule() != null) {
					list.add(conrule);
				}
			}
		} else {
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			pairs = this.getMaxCriticalPair(overlaps);
			for (int i=0; i<pairs.size(); i++) {
				Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
				pair = pairs.get(i);
				Pair<OrdinaryMorphism, OrdinaryMorphism> criticalPair = pair.first;			
				ConcurrentRule conrule = makeConcurrentRule(originalRule1, inverseRule1, rule2, criticalPair);
				if (conrule.getRule() != null) {
					list.add(conrule);
				}
			}
		}
		
		for (int i=0; i<list.size(); i++) {
			list.get(i).getRule().removeUnusedVariableOfAttrContext();
		}
		return list;
	}
	
	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getMaxCriticalPair(
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> overlaps) {
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
		result = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
		
		int max = 0;
		for (int i=0; i<overlaps.size(); i++) {
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			pair = overlaps.get(i);
			// to take in account only overlap. graphs without NAC part 
			// in this case:  pair.second != null
			if (pair.second != null) {
				continue;
			}			
			
			Pair<OrdinaryMorphism, OrdinaryMorphism> criticalPair = pair.first;
			final OrdinaryMorphism m1 = criticalPair.first;
			final OrdinaryMorphism m2 = criticalPair.second;			
			final Graph g = m1.getTarget();
			int nn = 0;
			Iterator<Node> nodes = g.getNodesSet().iterator();
			while (nodes.hasNext()) {
				GraphObject o = nodes.next();
				if (m1.getInverseImage(o).hasMoreElements()
						&& m2.getInverseImage(o).hasMoreElements()) {
//					if (o.isCritical())
						nn++;
				}
			}
			Iterator<Arc> arcs = g.getArcsSet().iterator();
			while (arcs.hasNext()) {
				GraphObject o = arcs.next();
				if (m1.getInverseImage(o).hasMoreElements()
						&& m2.getInverseImage(o).hasMoreElements()) {
//					if (o.isCritical())
						nn++;
				}
			}
			if (nn >= max) {
				max = nn;
				result.add(pair);
			}
		}
			
		return result;
	}

	protected Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getDeleteUseConflicts1(
			final Rule r1, 
			final Rule r2) {

		System.out.println("    DependencyPair.getDeleteUseConflicts::  [ "
				+ r1.getName() + ", " + r2.getName() + " ] ...");
			
		// check constant attribute of deleted objects of r1
		// against constant attribute of LHS of r2
		// The objects of appropriate type from LHS of r2 
		// should use this constant attribute
		if (!needMoreCheckDueToDelConstAttr(r1, r2)) 
			return null;

		this.cpdKind = CriticalPairData.DELETE_USE_CONFLICT;
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlaps = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
				
		final Graph g = r1.getLeft();
		int maxSize = r2.getLeft().getSize();
		Enumeration<OrdinaryMorphism> pacs2 = r2.getEnabledPACs();
		
		boolean lhs_done = false; 
		boolean perform = true;
		while (perform && !this.stop) {
			String pacName = "";
			// test PACs
			OrdinaryMorphism L2isoL2ExtendedByPAC = null;
			Pair<OrdinaryMorphism, OrdinaryMorphism> lhs_pac_pair = null;
			
			OrdinaryMorphism pac = null;
			if (this.withPACs && pacs2.hasMoreElements()) {				
				pac = pacs2.nextElement();				
				if (!pac.isEnabled()) {
					continue;
				}
			}
						
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
					}
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
					
					// make overlapping
					OrdinaryMorphism inclMorphism = null;
					Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
					localOverlaps = null;
										
					// with LHS of the second rule
					if (L2isoL2ExtendedByPAC == null) {						
						// with LHS of the second rule
						if (r2.getSource().getSize() >= inclSet.size()) 
							inclMorphism = makeInclusionMorphism(inclSet, g);						
						// with LHS of second rule only
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
						if (lhs_pac_pair.first.getTarget().getSize() >= inclSet.size()) 
							inclMorphism = makeInclusionMorphism(inclSet, g);						
						// with extended LHS by PAC of second rule 
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
//					if (pacName.isEmpty())
//						this.saveDUIndx(ncp + i, "".concat(":LHS"), this.checkSwitchDependency);
//					else
//						this.saveDUIndx(ncp + i, pacName.concat(":PAC"), this.checkSwitchDependency);
//					System.out.println("DeleteUse conflict: stop at index: "+this.duIndx);						
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
		} // while(perform && !this.stop)
		
		if (this.withPACs) {
			pacs2 = r2.getEnabledPACs();
			while (pacs2.hasMoreElements()) {
				// restore constant attribute value
				this.replaceVarAttrValueByConst(pacs2.nextElement());
			}
		}
		overlaps = this.getMaxCriticalPair(overlaps);
		
		//test overlap graphs and reduce isomorphic
		if (/*!r1.getTypeSet().isArcDirected() && */ overlaps.size() > 0) {
			 reduceCriticalPairs(overlaps);
		}
			
		overlaps.trimToSize();
		this.cpdKind = -1;
		
		System.out.println("    DependencyPair.getDeleteUseConflicts::  [ "
					+ r1.getName() + ", " + r2.getName() + " ]  " + overlaps.size()
					+ " critical overlapping(s)");
		
		return overlaps;
	}
	
	protected Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getDeleteUseConflicts_CR(
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
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlaps = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
				
		final Graph g = r1.getLeft();
		int maxSize = r2.getLeft().getSize();
		Enumeration<OrdinaryMorphism> pacs2 = r2.getEnabledPACs();
		
		boolean lhs_done = false; 
		boolean perform = true;
		while (perform && !this.stop) {
			String pacName = "";
			// test PACs
			OrdinaryMorphism L2isoL2ExtendedByPAC = null;
			Pair<OrdinaryMorphism, OrdinaryMorphism> lhs_pac_pair = null;
			
			OrdinaryMorphism pac = null;
			if (this.withPACs && pacs2.hasMoreElements()) {				
				pac = pacs2.nextElement();				
				if (!pac.isEnabled()) {
					continue;
				}
			}
			
			Vector<Vector<GraphObject>> inclusions = null;		
			this.inclAsGraph = false;
			
			int size = this.contextC1_L1.size();
			if (size > maxSize) size = maxSize;
			
			Vector<Vector<GraphObject>> 
			contextCombis = ExcludePairHelper.getInclusions(g, size, this.contextC1_L1, true);		
			// each inclusion should contain at least one object to delete
			checkInclusions(contextCombis, this.delete);
			if (contextCombis.size() == 0)
				break;
			
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
					
			if (namedObjectOnly)
				this.checkInclusionsDuetoNamedObject(contextCombis);
			
			size = this.preservedK1_L1.size();
			if (size > maxSize) size = maxSize;
			
			Vector<Vector<GraphObject>>
			preservedCombis = ExcludePairHelper.getPlainCombinedInclusions(
					new Vector<GraphObject>(this.preservedK1_L1), size, g);
			
			inclusions = ExcludePairHelper.combineInclusions(maxSize, contextCombis,
												preservedCombis, this.boundB1_L1);
						 				
			System.out.println("to check inclusions: "+inclusions.size());
			int i = inclusions.size()-1;
			while (i >= 0 && !this.stop) {
				Vector<GraphObject> inclSet = inclusions.get(i); 
				i--;
				
				// make overlapping
				OrdinaryMorphism inclMorphism = null;
				Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
				localOverlaps = null;
									
				// with LHS of the second rule
				if (L2isoL2ExtendedByPAC == null) {						
					// with LHS of the second rule
					if (r2.getSource().getSize() >= inclSet.size()) 
						inclMorphism = makeInclusionMorphism(inclSet, g);						
					// with LHS of second rule only
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
					if (lhs_pac_pair.first.getTarget().getSize() >= inclSet.size()) 
						inclMorphism = makeInclusionMorphism(inclSet, g);						
					// with extended LHS by PAC of second rule 
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
					
	//			unsetAllTransientAttrValuesOfRule(r2);
																
				if (!this.complete && !overlaps.isEmpty()) {
					break;
				}
			}
			
			inclusions = null;
			contextCombis = null;
			preservedCombis = null;
			perform = (this.withPACs && pacs2.hasMoreElements()) || !lhs_done; 
		} // while(perform && !this.stop)
		
		if (this.withPACs) {
			pacs2 = r2.getEnabledPACs();
			while (pacs2.hasMoreElements()) {
				// restore constant attribute value
				this.replaceVarAttrValueByConst(pacs2.nextElement());
			}
		}
		overlaps = this.getMaxCriticalPair(overlaps);
		
		//test overlap graphs and reduce isomorphic
		if (/*!r1.getTypeSet().isArcDirected() && */overlaps.size() > 0) {
			 reduceCriticalPairs(overlaps);
		}
			
		overlaps.trimToSize();
		this.cpdKind = -1;
		
		System.out.println("    DependencyPair.getDeleteUseConflicts::  [ "
					+ r1.getName() + ", " + r2.getName() + " ]  " + overlaps.size()
					+ " critical overlapping(s)");
		
		return overlaps;
	}

	
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
			if (r.getImage(o) != null) {
				if (isInTypes(neededTypes, o)) {
					preserved.add(o);
				}
			}
			else if (isChildInTypes(neededTypes, o)) {
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
			if (isChildInTypes(neededTypes, o)) {
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
			boolean nacAttrConst = false;
			boolean attrConst = false;
			boolean nacAttrSet = false;
			for (int j = 0; j < toproduce.size() && !nacMaybeCritical; j++) {
				GraphObject o = toproduce.get(j);
				Vector<GraphObject> v = type2gosNAC2.get(o.getType());
				if (v == null) 
					v = nac.getTarget().getElemsAndParentsOfType(o.getType());
				else
					v.addAll(nac.getTarget().getElemsAndParentsOfType(o.getType()));
				v.addAll(nac.getTarget().getChildrenOfType(o.getType())); // add also all children
				if (!v.isEmpty()) {
					type2gosNAC2.put(o.getType(), v);
					for (int i=0; i<v.size(); i++) {
						GraphObject go = v.get(i);
						if (nac.getInverseImage(go).hasMoreElements()) {
							v.remove(go);
							i--;
						}
					}
				}
				for (int i=0; i<v.size() && !nacMaybeCritical; i++) {
					GraphObject go = v.get(i);
					if (!nac.getInverseImage(go).hasMoreElements()) {
						if (go.getAttribute() != null) {
							nacAttrSet = false;
							for (int k=0; k<go.getAttribute().getNumberOfEntries(); k++) {
								ValueMember vm_nac = (ValueMember)go.getAttribute().getMemberAt(k);
								if (vm_nac.isSet()) {
									nacAttrSet = true;
									nacAttrConst = vm_nac.getExpr().isConstant();
									attrConst = true;
									ValueMember vm = (ValueMember)o.getAttribute().getMemberAt(vm_nac.getName());
									if (vm.isSet()) {
										if (vm_nac.getExpr().isConstant() && vm.getExpr().isConstant()
												&& !vm.getExprAsText().equals(vm_nac.getExprAsText())) {
											attrConst = false;
											break;
										}						
									}
								}
							}
							nacMaybeCritical = (nacAttrConst && attrConst) || nacAttrSet;
						}
						else {
							nacMaybeCritical = true;
						}
					}
				}
			}
			
			if (nacMaybeCritical) {
//				System.out.println("DependencyPair.getPotentialCriticalNACsOfR2:  nac: "+nac.getName());
				result.add(nac);
			}
		}
		result.trimToSize();
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
							if (nacCondEval == 1) { // no any conditions and variables or all satisfied
								if (firstRule) {
									result = false;
									nacStar.dispose(); nacStar = null;
									break;
								}
								else if (!hasNewConstantToCriticalInContext(n, nacStar)) {									
									result = false;
									nacStar.dispose(); nacStar = null;
									break;									
								}
								else {
									result = true;	
									nacStar.dispose(); nacStar = null;							
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
			// if the NAC is satisfied (nacStar == null) after step of rule1, 
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
}
