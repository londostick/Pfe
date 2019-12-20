/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.cons;

import java.util.Iterator;
import java.util.Vector;
import java.util.Enumeration;
import agg.attribute.AttrContext;
import agg.attribute.AttrInstance;
import agg.attribute.AttrInstanceMember;
import agg.attribute.AttrVariableMember;
import agg.attribute.AttrConditionTuple;
import agg.attribute.parser.javaExpr.SimpleNode;
import agg.attribute.parser.javaExpr.ASTPrimaryExpression;
import agg.attribute.parser.javaExpr.ASTExpression;
import agg.attribute.handler.SymbolTable;
import agg.attribute.handler.HandlerType;
import agg.attribute.handler.HandlerExpr;
import agg.attribute.handler.AttrHandlerException;
import agg.attribute.handler.impl.javaExpr.JexExpr;
import agg.attribute.impl.ContextView;
import agg.attribute.impl.DeclMember;
import agg.attribute.impl.ValueTuple;
import agg.attribute.impl.ValueMember;
import agg.attribute.impl.VarMember;
import agg.attribute.impl.VarTuple;
import agg.attribute.impl.CondMember;
import agg.attribute.impl.CondTuple;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.Completion_InjCSP;
import agg.xt_basis.GraphObject;
import agg.xt_basis.Graph;
import agg.xt_basis.Arc;
import agg.xt_basis.BadMappingException;
import agg.xt_basis.Node;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Match;
import agg.xt_basis.TypeException;
import agg.xt_basis.csp.Completion_CSP;
import agg.util.Pair;

public class AtomApplCond implements Evaluable {

	private int old_tick;

	private boolean old_val;

	private AtomConstraint itsAtomConstraint;

//	private MorphCompletionStrategy strategy;

	/* The precondition. */
	private OrdinaryMorphism cond; // R--cond-->S

	private OrdinaryMorphism po_t; // S--po_t-->T

	private OrdinaryMorphism po_q; // C--po_q-->T
	
	private Vector<AtomApplCond> equivalents; 

	private int n; // premise match counter

	private final BaseFactory bf = BaseFactory.theFactory();
	
	
	public AtomApplCond() {
		init(null, null, null);
	}

	public AtomApplCond(final AtomConstraint itsSource, 
						final OrdinaryMorphism x,
						final OrdinaryMorphism pot, 
						final OrdinaryMorphism poq) {
		this.itsAtomConstraint = itsSource;
		init(x, pot, poq);
		this.equivalents = createEquivalents();
	}

	private void init(final OrdinaryMorphism x, 
					final OrdinaryMorphism pot,
					final OrdinaryMorphism poq) {
		this.cond = x;
		this.po_t = pot;
		this.po_q = poq;
		this.old_tick = -1;
		this.old_val = false;
	}
	
	public boolean eval(final Object o) {
		return eval(o, -1);
	}
	
	@SuppressWarnings("rawtypes")
	public boolean eval(final Object o, final int tick) {
		if (tick != -1 && tick == this.old_tick) {
			return this.old_val;
		}
		this.old_tick = tick;
		this.old_val = eval((Pair) o);
		return this.old_val;
	}

	public boolean eval(final Object o, final boolean negation) {
		return eval(o, -1, negation);
	}
	
	@SuppressWarnings("rawtypes")
	public boolean eval(final Object o, final int tick, final boolean negation) {
		if (tick != -1 && tick == this.old_tick) {
			return this.old_val;
		}
		this.old_tick = tick;
		this.old_val = eval((Pair) o, negation);
		return this.old_val;
	}

	public Graph getBase() {
		if (this.cond == null) {
			return null;
		}
		return this.cond.getOriginal();
	}

	public Graph getPre() {
		if (this.cond == null) {
			return null;
		}
		return this.cond.getImage();
	}

	public void setPreCondition(final OrdinaryMorphism x) {
		this.cond = x;
	}

	public void setPoMorphs(final OrdinaryMorphism pot, final OrdinaryMorphism poq) {
		this.po_t = pot;
		this.po_q = poq;
	}

	public OrdinaryMorphism getPreCondition() {
		return this.cond;
	}

	public OrdinaryMorphism getT() {
		return this.po_t;
	}

	public OrdinaryMorphism getQ() {
		return this.po_q;
	}

	public AtomConstraint getSourceAtomConstraint() {
		return this.itsAtomConstraint;
	}

	public Vector<AtomApplCond> getEquivalents() {
		return this.equivalents;
	}

	public boolean isValid() {
		// Note: this.cond does not need to be injective
		if ((this.cond == null )
				||(this.po_t == null) 
				|| (this.po_q == null)
				|| !this.cond.isTotal()			
				|| (getPre() != this.po_t.getOriginal())
				|| (this.po_t.getImage() != this.po_q.getImage()))
			return false;
		
		return true;
	}

	private boolean isEvaluable(final VarTuple vars, final CondMember condMem, final Graph g) {
		boolean result = true;
		final Vector<String> varnames = g.getVariableNamesOfAttributes();
		if (condMem.isEnabled() && !condMem.isEvaluable(vars)) {
			final Vector<String> condVars = condMem.getAllVariables();
			for (int j = 0; j < condVars.size(); j++) {
				final String vn = condVars.elementAt(j);
				if (!vars.getVarMemberAt(vn).isSet() 
						&& varnames.contains(vn)) {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	private boolean eval(final Pair<?,?> pair) {
		return eval(pair, false);
	}

	private boolean eval(final Pair<?, ?> pair, final boolean negation) {
//		System.out.println(this.getClass().getName()+".eval   >>>  "+this.itsAtomConstraint.getName());
		// get co-match morphism
		final OrdinaryMorphism co_match = (OrdinaryMorphism) pair.first;
//		showMorphismData(co_match);
		
		
		// get match morphism
		final Match orig_match = (Match) (OrdinaryMorphism) pair.second;
		
		if ((co_match == null) 
				|| (orig_match == null) 
				|| !getBase().equals(co_match.getOriginal())) { // RHS of rule
			return false;
		}
		
		// original match completion strategy
//		strategy = orig_match.getCompletionStrategy();
		
		final Graph H = co_match.getImage();
		final Graph S = getPre();
		
		// extend attr context of the current match by variables of graph S
		AttrContext ac = orig_match.getAttrContext();
		declareVariables(ac, S);
		
		/*
		 * co_match: R->H satisfies this condition, if for each p: S->H, with m =
		 * p o cond where S is getPre() and p injective and total, there is a f:
		 * T->H, s.t. f o po_t = p, f o po_t, f o po_q injective, f total. So we first
		 * need to construct all p with p o cond = m. First construct a limited p'
		 * with p' o cond = co_match by completeDiagram(), then construct all
		 * possible totalizations. Unfortunately nextCompletion() only works for
		 * the first time with a preset partial mapping. So we have to look at
		 * each completion.
		 */
		OrdinaryMorphism p_morph = this.bf.createMorphism(S, H, true);
		// Graph S can contain expressions, do unset it
		p_morph.unsetOriginalAttrsIfExpression();
		
		OrdinaryMorphism p = this.bf.createMatchfromMorph(p_morph, ac);
		// NOTE: make own completion strategy of p
		final Completion_CSP strategy1 = new Completion_InjCSP();
		
//		if (strategy.getProperties().get(CompletionPropertyBits.INJECTIVE)) {
//			if (strategy.getProperties().get(CompletionPropertyBits.DANGLING))
//				strategy1.getProperties().set(CompletionPropertyBits.DANGLING);
//		} else {
//			strategy1.getProperties().clear(CompletionPropertyBits.INJECTIVE);
//			if (strategy.getProperties().get(CompletionPropertyBits.DANGLING))
//				strategy1.getProperties().set(CompletionPropertyBits.DANGLING);
//			if (strategy.getProperties().get(CompletionPropertyBits.IDENTIFICATION))
//				strategy1.getProperties().set(CompletionPropertyBits.IDENTIFICATION);
//		}
		p.setCompletionStrategy(strategy1, true);
						
		declareConditions(p.getAttrContext(), 
						getT().getAttrContext().getConditions());
		p.removeUnusedVariableOfAttrContext(); 
		p.disableUnusedAttrCondition();
		
		// test output to see value of variables and mappings
//		((VarTuple)p.getAttrContext().getVariables()).showVariables();		
//		showMorphismData(p);
		
		OrdinaryMorphism f = null, fmatch = null, check = null;
		boolean premiseOK = false;
		boolean conclusionOK = false;
		boolean found = false;
		
		this.n = 0;
		while ( !found && p.nextCompletionWithConstantsChecking()) {
//			System.out.println("****  p.nextCompletionWithConstantsChecking");
			check = this.cond.compose(p);
			premiseOK = check.isIsomorphicTo(co_match);
			
			// test output to see value of variables and mappings	
//			((VarTuple)p.getAttrContext().getVariables()).showVariables();
//			this.showMorphismData(p);
			
			this.bf.destroyMorphism(check);
			if (!premiseOK) {
				continue;
			}
						
			// check conditions of atomic graph constraint
			renameVariableInCondition(p);			
			premiseOK = checkAttrCondition(p);
			
			if (premiseOK) {
//				System.out.println("****  premiseOK");
				this.n++;
				found = false;
				conclusionOK = false;
				/* Now try to find f-morphism as given above. First try to meet
				 * f o po_t==p at least on domain(p).
				 */
				if (f == null) {
					f = p.completeDiagram(this.po_t);
				}
				else {
					// clear and reinit source graph of the f morphism
					f.clear();
					f.setSource(this.po_t.getImage());
					if (!p.completeDiagram(this.po_t, f)) {						
						f = null;
					}
				}
				
				if (f != null) {
					// note: make own completion strategy of fmatch
					// Note: strategy2 should be non-injective !!
					final Completion_CSP strategy2 = new Completion_CSP(true);	
//					final Completion_CSP strategy2 = new Completion_CSP();
//					strategy2.getProperties().clear(CompletionPropertyBits.INJECTIVE);

//					if (strategy.getProperties().get(CompletionPropertyBits.DANGLING)) 
//						strategy2.getProperties().set(CompletionPropertyBits.DANGLING);
//					if (strategy.getProperties().get(CompletionPropertyBits.IDENTIFICATION))	
//						strategy2.getProperties().set(CompletionPropertyBits.IDENTIFICATION);						
//					strategy2.showProperties();

					/* Now look if the attributes match. */
					if (fmatch == null) {
						fmatch = this.bf.createMorphfromMorph(f, 
										orig_match.getAttrContext());
						fmatch.setCompletionStrategy(strategy2, true);
					} else {
						fmatch.clear();
						if (this.bf.createMatchfromMorph(f, fmatch, 
										orig_match.getAttrContext())) {
							fmatch.getCompletionStrategy().initialize(fmatch);
						} else {
							fmatch = null;
						}
					}
				
					if (fmatch != null) {
						ac = fmatch.getAttrContext();	
						// test output to see value of variables and mappings	
//						((VarTuple)fmatch.getAttrContext().getVariables()).showVariables();
//						this.showMorphismData(p);

						/*
						 * And now try different completions of fmatch for the
						 * rest of requirements (the combinations with po_t and
						 * po_q), not injective! the part on which it must be
						 * injective we check out self.
						 */
						if (!fmatch.isTotal()) {
							// f = T --> H : not total, try  nextCompletion
														
							while (fmatch.nextCompletionWithConstantsChecking()) {							
								// f o t : Injective ? f o q : Injective ?
//								System.out.println("p:: "+p.getSource().hashCode()+"   "+p.getTarget().hashCode());
								conclusionOK = checkConclusionMatchStructure(fmatch);
								if (!conclusionOK) {
									continue;
								} 
//								showMorphismData(fmatch);
								
								// handle attribute context of fmatch
								renameVariableInCondition(fmatch);
								
								final Vector<String> helpVars = new Vector<String>();
								final Vector<String> testVars = new Vector<String>();
								
								// set value of variable of po_t 
								// by value inside of graph H 
								setValueToVariable(fmatch, getT().getImage(),
													helpVars, testVars);
								
								// add condition of po_t to fmatch
								declareConditions(fmatch.getAttrContext(), 
											getT().getAttrContext().getConditions());
								markUsedVariables(fmatch);
								markAttrConditions(fmatch.getAttrContext());
								
								// test output to see value of variables and mappings	
//								((VarTuple)fmatch.getAttrContext().getVariables()).showVariables();
//								this.showMorphismData(p);
								
								// check condition of fmatch
								conclusionOK = checkAttrCondition(fmatch);
								
								// do unset variable of po_t and help value
								unsetVariable((VarTuple) fmatch.getAttrContext().getVariables(),
												helpVars, testVars);
								
								found = conclusionOK;
//								System.out.println("conclusionOK: "+conclusionOK);
								if (found) {
//									System.out.println(fmatch.getDomainObjects());
//									System.out.println(fmatch.getSource());
								
									break;
								}
							} // while(fmatch.nextCompletion())	
							
						} else {
							// fmatch already total
					
							// f o t : Injective && f o q : Injective ?								
							conclusionOK = checkConclusionMatchStructure(fmatch);
							conclusionOK = fmatch.checkConstants();
							if (conclusionOK) {
//								showMorphismData(fmatch);
								
								// set value of variable of po_t 
								// by value inside of graph H 
								final Vector<String> helpVars = new Vector<String>();
								final Vector<String> testVars = new Vector<String>();
								setValueToVariable(fmatch, getT().getImage(), 
													helpVars, testVars);

								// add conditions of po_t to fmatch
								declareConditions(
											fmatch.getAttrContext(), 
											getT().getAttrContext().getConditions());
								markUsedVariables(fmatch);
								markAttrConditions(fmatch.getAttrContext());
									
								// test output to see value of variables and mappings	
//								((VarTuple)fmatch.getAttrContext().getVariables()).showVariables();
//								this.showMorphismData(p);
									
								// check condition of fmatch
								conclusionOK = checkAttrCondition(fmatch); 
//								System.out.println(this.getClass().getName()+" >>>2)  fmatch:  "+conclusionOK);
									
								unsetVariable((VarTuple) fmatch.getAttrContext().getVariables(), 
													helpVars, testVars);
									
								found = conclusionOK;
//								System.out.println("(total) conclusionOK: "+conclusionOK);
							}
						}
						f.clear();
						fmatch.clear();
						
						if (!found) {
							// test this.equivalents
							for (int l = 0; l < this.equivalents.size() && !found; l++) {
								AtomApplCond eq = this.equivalents.elementAt(l);
//								System.out.println("check equivalent ....");
								if (eq.eval(pair, negation)) {
									found = true;
//									System.out.println("check equivalent ....  :"+  found);
								}
							}
						}
						
					}
				}
			} // if(premiseOK)
		} // while(p.nextComletion
		
		p.enableUnusedAttrCondition();
		
		if (this.n == 0) {
			found = true;
		}
		
		this.bf.destroyMorphism(p);
		this.bf.destroyMorphism(p_morph);
		this.bf.destroyMorphism(f);
		this.bf.destroyMorphism(fmatch);
		this.bf.destroyMorphism(check);
		
		return found;
	}

	private boolean checkAttrCondition(final OrdinaryMorphism morph) {
		boolean result = true;
		final VarTuple vars = (VarTuple) morph.getAttrContext().getVariables();			
		final AttrConditionTuple condTuple = morph.getAttrContext().getConditions();
		for (int k = 0; k < condTuple.getNumberOfEntries(); k++) {
			final CondMember condMem = (CondMember) condTuple.getMemberAt(k);
			if (condMem.isEnabled()) {				
				if (!condMem.isDefinite() 
						|| condMem.isTrue()
						|| !isEvaluable(vars, condMem, morph.getSource())) {
					result = true;
				} else {
					result = false;
					break;
				}
			}
		}
		return result;
	} 
	
	// f o t : Injective && f o q : Injective ?
	private boolean checkConclusionMatchStructure(final OrdinaryMorphism conclMatch) {
		if (!conclMatch.isInjective()) {
			boolean ok = true;
			Enumeration<GraphObject> conclMatchCodom = conclMatch.getCodomain();
			while (conclMatchCodom.hasMoreElements() /*&& ok*/) {
				GraphObject o = conclMatchCodom.nextElement();
				Enumeration<GraphObject> oInverses = conclMatch.getInverseImage(o);
				GraphObject inv1 = oInverses.nextElement();
				GraphObject inv_q1 = null;				
				if (this.po_q.getInverseImage(inv1).hasMoreElements()) { 
					inv_q1 = this.po_q.getInverseImage(inv1).nextElement();
				}
					
				// ab hier kann kritisch werden!						
				if (inv_q1 != null) {
					while (oInverses.hasMoreElements()) {
						GraphObject inv = oInverses.nextElement();	
						GraphObject inv_q = null;
						if (this.po_q.getInverseImage(inv).hasMoreElements()) {
							inv_q = this.po_q.getInverseImage(inv).nextElement();
						}		
						if (inv_q != null
								&& !this.itsAtomConstraint.getInverseImage(inv_q1).hasMoreElements()
								&& !this.itsAtomConstraint.getInverseImage(inv_q).hasMoreElements()) {										
							ok = false;
							break;
						}								
					}
				}
			}
			if (!ok) {
//				?????
//				return false;
			}
				
		}
		
		
		// f o t : Injective ?
		final OrdinaryMorphism check1 = this.po_t.compose(conclMatch);
		boolean conclusionOK = check1.isInjective();
		if (conclusionOK) {
			// f o q : Injective ?
			final OrdinaryMorphism check2 = this.po_q.compose(conclMatch);
			conclusionOK = check2.isInjective();
			
			this.bf.destroyMorphism(check1);
			this.bf.destroyMorphism(check2);			
		} 
		return conclusionOK;
	}
	
	public int getConditionMatchCounter() {
		return this.n;
	}

	public String getAsString() {
		return new String("");
	}

	/*
	 * R -----> S <------P 
	 *        / |        |
	 *       /  t   PO   |
	 *      /   |        |
	 *     /   \|/      \|/
	 *    t'    T<---q---C
	 *    |    /        /
	 *    | iso        /
	 *    |  /        /
	 *    | /        / 
	 *   \|/        /  
	 *    T'<------q'   
	 *                        
	 */
	private Vector<AtomApplCond> createEquivalents() {
//		System.out.println(this.getClass().getName()+".createEquivalents()...");
		final Vector<AtomApplCond> v = new Vector<AtomApplCond>(0);
		
		final Enumeration<GraphObject> eq = this.po_q.getCodomain();
		while (eq.hasMoreElements()) {
			final GraphObject objTq = eq.nextElement();			 
			if (!this.po_t.getInverseImage(objTq).hasMoreElements()) { 
				// objTq is from C without P
				if (this.po_q.getInverseImage(objTq).hasMoreElements()) {
//					GraphObject objC = this.po_q.getInverseImage(objTq).nextElement();
					
					final Enumeration<GraphObject> et = this.po_t.getCodomain();
					while (et.hasMoreElements()) {
						final GraphObject objTt = et.nextElement();
						if (!this.po_q.getInverseImage(objTt).hasMoreElements()) {
							// objTt is from S without P, also from R
							
							boolean canGlue = false;
							if (objTq.isNode() 
										&& objTt.isNode() 
										&& objTq.getType().isParentOf(objTt.getType())) {
								canGlue = true;
//								System.out.println("glue nodes: "+	objTt+"   "+objTq+"   "+canGlue);
							} else if (objTq.isArc() 
											&& objTt.isArc() 
											&& objTq.getType().compareTo(objTt.getType())) {
								// sources or targets should be free
								canGlue = canGlueArcs((Arc) objTt, (Arc) objTq);
//								System.out.println("glue arcs: "+	objTt+"   "+objTq+"   "+canGlue);
							}

							if (canGlue) {
								boolean shouldDestroy = false;
								OrdinaryMorphism 
								isoT = this.po_t.getTarget().isomorphicCopy();
								if (isoT != null) {
									isoT.setCompletionStrategy(
												new Completion_CSP(true), true);										
									OrdinaryMorphism 
									equivalentT = this.bf.createMorphism(
														this.po_t.getSource(),
														isoT.getTarget(), true);
									// set available mappings from po_t
									try {
										addMappings(equivalentT, this.po_t, isoT);
									} catch (BadMappingException ex) {
										System.out.println(ex.getLocalizedMessage());
									}
										
									OrdinaryMorphism 
									equivalentQ = this.bf.createMorphism(
														this.po_q.getSource(),
														isoT.getTarget(), true);
									// set available mappings from po_q
									try {
										addMappings(equivalentQ, this.po_q, isoT);
									} catch (BadMappingException ex) {
										System.out.println(ex.getLocalizedMessage());
									}
										
									if (equivalentT.isTotal()
												&& equivalentQ.isTotal()) {
										// find objects to glue
										final GraphObject glue = isoT.getImage(objTq);
										final GraphObject keep = isoT.getImage(objTt);
										final GraphObject obj_glue = equivalentQ
													.getInverseImage(glue)
													.nextElement();
										
										boolean mappingOK = true;									
										if (glue.getAttribute() == null
													&& keep.getAttribute() == null) {
											try {
												mappingOK = isoT.getTarget().glue(keep, glue);
											} catch (TypeException e) {
												mappingOK = false;
											}
										} 
										else if (glue.getAttribute() != null
														&& keep.getAttribute() != null) {
											try {
												if (isoT.getTarget().glue(keep, glue)) {
													resetAttrsAfterGlue(keep, obj_glue);												
												} 
												else 
													mappingOK = false;
											} catch (TypeException e) {
												mappingOK = false;
											}
										}
										 
										if (mappingOK) {
											try {
												if (obj_glue.isNode()) {
													equivalentQ.addMapping(obj_glue, keep);
												} else {
													// to set edge mapping, 
													// first set source, target mappings 
													equivalentQ.addMapping(((Arc)obj_glue).getSource(),
															((Arc)keep).getSource());
													equivalentQ.addMapping(((Arc)obj_glue).getTarget(),
															((Arc)keep).getTarget());
													equivalentQ.addMapping(obj_glue, keep);
												}
											} catch (BadMappingException e) {
	//											System.out.println(e.getLocalizedMessage());
												mappingOK = false;
											}
										}
										
										boolean allTotal = true;
										if (mappingOK) {
											if (!equivalentT.isTotal()) {
												allTotal = equivalentT.nextCompletion();
											}
											if (!equivalentQ.isTotal()) {
												allTotal = allTotal && equivalentQ.nextCompletion();
											}
										}

										try {
											isoT.addMapping(objTt, keep);
										} catch (BadMappingException e) {
											mappingOK = false;
										}

										if (mappingOK) { 
											if (!isoT.isTotal())										
												allTotal = allTotal && isoT.nextCompletion();
										}
										else {
											allTotal = false;
										}
//										System.out.println("all is Total:  "+	allTotal);	
										if (allTotal) {
											declareConditions(equivalentT.getAttrContext(),
																this.po_t.getAttrContext()
																		.getConditions());
											declareConditions(equivalentQ.getAttrContext(),
																this.po_q.getAttrContext()
																		.getConditions());

											v.add(new AtomApplCond(this.itsAtomConstraint,
																	this.cond,
																	equivalentT,
																	equivalentQ));
//											break;
											
										} else {
											shouldDestroy = true;
										}
										
									} else {
										shouldDestroy = true;
									}
									
									if (shouldDestroy) {
										this.bf.destroyMorphism(equivalentQ);
										equivalentQ = null;
										this.bf.destroyMorphism(equivalentT);
										equivalentT = null;
										this.bf.destroyMorphism(isoT);
										isoT = null;
									}
								}
							}
						}
					}
				}
			}
		}
		return v;
	}

	// sources or targets should be free
	private boolean canGlueArcs(final Arc objTt, final Arc objTq) {
		boolean canGlue = false;
		boolean srcOK = false;
		boolean tarOK = false;
		if ((objTt.getSource() == objTq.getSource())
				&& (objTt.getTarget() == objTq.getTarget())) {
			canGlue = true;
		}
		else {
			if (!this.po_t.getInverseImage(objTq.getSource())
										.hasMoreElements()
					&& !this.itsAtomConstraint.getInverseImage(
								this.po_q.getInverseImage(
										objTq.getSource()).nextElement())
										.hasMoreElements()
					&& !this.po_q.getInverseImage(objTt.getSource())
										.hasMoreElements()) {
				srcOK = true;
			}
			if (!this.po_t.getInverseImage(objTq.getTarget())
								.hasMoreElements()
					&& !this.itsAtomConstraint.getInverseImage(
								this.po_q.getInverseImage(
										objTq.getTarget()).nextElement())
										.hasMoreElements()
					&& !this.po_q.getInverseImage(objTt.getTarget())
								.hasMoreElements()) {
				tarOK = true;
			}			
			canGlue = srcOK || tarOK;
		}
		return canGlue;
	}

	private void resetAttrsAfterGlue(
			final GraphObject keep, 
			final GraphObject obj_glue) {
		
		if (keep.isNode()) {
			resetAttribute(
					(ValueTuple) obj_glue.getAttribute(),
					(ValueTuple) keep.getAttribute());
		}
		else {
			resetAttribute(
					(ValueTuple) obj_glue.getAttribute(),
					(ValueTuple) keep.getAttribute());
			resetAttribute(
					(ValueTuple) ((Arc) obj_glue).getSource().getAttribute(),
					(ValueTuple) ((Arc) keep).getSource().getAttribute());
			resetAttribute(
					(ValueTuple) ((Arc) obj_glue).getTarget().getAttribute(),
					(ValueTuple) ((Arc) keep).getTarget().getAttribute());
		}
	}
	
	private void addMappings(
				final OrdinaryMorphism tarMorph, 
				final OrdinaryMorphism srcMorph1,
				final OrdinaryMorphism srcMorph2) throws BadMappingException {
		
		// set available mappings from po_t
		final Iterator<Node> tDomNodes = srcMorph1.getSource().getNodesSet().iterator();
		while (tDomNodes.hasNext()) {
			final GraphObject o = tDomNodes.next();
			final GraphObject i = srcMorph2.getImage(srcMorph1.getImage(o));
			if (i != null) {			
				try {
					tarMorph.addMapping(o, i);
				} catch (BadMappingException ex) {
					throw ex;
				}
			}
		}
		final Iterator<Arc> tDomArcs = srcMorph1.getSource().getArcsSet().iterator();
		while (tDomArcs.hasNext()) {
			final Arc o = tDomArcs.next();
			final Arc i = (Arc) srcMorph2.getImage(srcMorph1.getImage(o));
			if (i != null) {
//				if (tarMorph.getImage(o.getSource()) == i.getSource()
//						&& tarMorph.getImage(o.getTarget()) == i.getTarget()) {
					try {					
						tarMorph.addMapping(o, i);
					} catch (BadMappingException ex) {
						throw ex;
					}
//				}
//				else {
//					throw new BadMappingException("Edge mapping failed! Source and target of an edge must be mapped before.");
//				}
			}
		}
	}
	
	private void resetAttribute(final ValueTuple from, final ValueTuple to) {
		if (to == null || from == null) 
			return;
		for (int i = 0; i < to.getNumberOfEntries(); i++) {
			final ValueMember amTo = to.getValueMemberAt(i);
			final ValueMember amFrom = from.getValueMemberAt(i);
			if ((!amTo.isSet() || amTo.getExpr().isVariable())
					&& (amFrom.isSet() && amFrom.getExpr().isVariable()))
				amTo.setExprAsText(amFrom.getExprAsText());
		}
	}

	private void renameVariableInCondition(final OrdinaryMorphism morph) {
		final Graph C = this.po_q.getSource();
//		Graph T = this.po_q.getTarget();

		final CondTuple conds = (CondTuple) morph.getAttrContext().getConditions();
		final Vector<String> condVars = conds.getAllVariables();
		
		for (int j = 0; j < condVars.size(); j++) {			
			doRenameVarInCond(conds, C.getNodesSet().iterator());
			doRenameVarInCond(conds, C.getArcsSet().iterator());			
		}
	}

	private void doRenameVarInCond(
			final CondTuple conds,
			final Iterator<?> elems) {
		
		while (elems.hasNext()) {
			final GraphObject goC = (GraphObject) elems.next();
			if (goC.getAttribute() == null) {
				continue;
			}
			
			final ValueTuple valueC = (ValueTuple) goC.getAttribute();
			for (int i = 0; i < valueC.getSize(); i++) {
				final ValueMember valC = valueC.getValueMemberAt(i);
				if (valC.isSet() && valC.getExpr().isVariable()) {
					final GraphObject goT = this.po_q.getImage(goC);
					if (goT != null) {
						final ValueTuple valueT = (ValueTuple) goT.getAttribute();
						final ValueMember valT = valueT.getValueMemberAt(i);
						if (valT.isSet()
								&& valT.getExpr().isVariable()
								&& !valT.getExprAsText().equals(
											valC.getExprAsText())) {
							renameVariableInCondition(conds, valC
										.getExprAsText(), valT.getExprAsText());
						} else if (!valT.isSet()) {
							valT.setExprAsText(valC.getExprAsText());
						}
					}
				}
			}
		}
	}
	
	private void renameVariableInCondition(
			final CondTuple conds, 
			final String from,
			final String to) {
		// rename variables in conditions
		for (int j = 0; j < conds.getSize(); j++) {
			final CondMember cm = conds.getCondMemberAt(j);
			final Vector<String> v1 = cm.getAllVariables();
			if (v1.contains(from)) {
				final JexExpr oldExpr = (JexExpr) cm.getExpr();
				final Vector<String> variables = new Vector<String>();
				oldExpr.getAllVariables(variables);
				findPrimaryAndReplace(
						(SimpleNode) oldExpr.getAST(), 
						from, to,
						null);
			}
		}
	}

	private void findPrimaryAndReplace(final SimpleNode node, 
										final String from, 
										final String to,
										final VarTuple vars) {

		for (int j = 0; j < node.jjtGetNumChildren(); j++) {
			SimpleNode snode = (SimpleNode) node.jjtGetChild(j);
			if (snode instanceof ASTPrimaryExpression) {
				for (int j1 = 0; j1 < snode.jjtGetNumChildren(); j1++) {
					SimpleNode n1 = (SimpleNode) snode.jjtGetChild(j1);
					if (n1 instanceof ASTExpression) {
						findPrimaryAndReplace(n1, from, to, vars);
					}
				}

				if (snode.getString().equals(from)) {
					SymbolTable symbs = SimpleNode.getSymbolTable(); 

					boolean to_found = false;
					ContextView context = (ContextView) symbs;
					VarTuple vt = (VarTuple) context.getVariables();
					for (int i = 0; i < vt.getSize(); i++) {
						VarMember vm = vt.getVarMemberAt(i);
						if (vm.getName().equals(to)) {
							to_found = true;
							HandlerType t = vm.getDeclaration().getType();
							try {
								HandlerExpr expression = vm.getHandler()
										.newHandlerExpr(t, to);
								SimpleNode test = (SimpleNode) expression
										.getAST().jjtGetChild(0);
								node.replaceChild(snode, test);
							} catch (AttrHandlerException ex) {
							}
						}
					}
					if (!to_found && (vars != null)) {
						for (int i = 0; i < vars.getSize(); i++) {
							VarMember vm = vars.getVarMemberAt(i);
							if (vm.getName().equals(to)) {
								to_found = true;
								HandlerType t = vm.getDeclaration().getType();
								try {
									HandlerExpr expression = vm.getHandler()
											.newHandlerExpr(t, to);
									SimpleNode test = (SimpleNode) expression
											.getAST().jjtGetChild(0);
									node.replaceChild(snode, test);
								} catch (AttrHandlerException ex) {
								}
							}
						}
					}
				}
			} else {
				findPrimaryAndReplace(snode, from, to, vars);
			}
		}
	}

	/**
	 * Not declared variables of the Graph g will be declared in attribute
	 * context of the OrdinaryMorphism fmatch and restored into Vector helpVars.
	 * The variables of the source graph of the OrdinaryMorphism fmatch those
	 * are not in helpVars will be stored into Vector testVars. The attribute
	 * values of the image graph of the fmatch will be adopted by the help
	 * variables.
	 */
	private void setValueToVariable(final OrdinaryMorphism fmatch, final Graph g,
			final Vector<String> helpVars, final Vector<String> testVars) {
		
		// jetzt werden Variablen aus po_t durch Values aus H belegt
		final AttrContext ac = fmatch.getAttrContext();
		final Vector<String> v = declareVariables(ac, g);
		
		while (v.elements().hasMoreElements()) {
			final String vn = v.elements().nextElement();
			helpVars.add(vn);
			v.remove(vn);
		}
		
		doSetValueToVar(ac, fmatch, helpVars, testVars,
						fmatch.getSource().getNodesSet().iterator());
		doSetValueToVar(ac, fmatch, helpVars, testVars,
				fmatch.getSource().getArcsSet().iterator());		
	}

	private void doSetValueToVar(
			final AttrContext ac,
			final OrdinaryMorphism fmatch,
			final Vector<String> helpVars, 
			final Vector<String> testVars,
			final Iterator<?> elems) {
		
		while (elems.hasNext()) {
			final GraphObject go = (GraphObject) elems.next();
			if (go.getAttribute() == null) {
				continue;
			}
			final GraphObject img = fmatch.getImage(go);
			final AttrInstance ai = go.getAttribute();
			if (img != null) {
				for (int i = 0; i < ai.getNumberOfEntries(); i++) {
					final AttrInstanceMember am = (AttrInstanceMember) ai
							.getMemberAt(i);
					if (am.isSet() && am.getExpr().isVariable()) {
						final String varname = am.getExprAsText();
						final AttrInstance aimg = img.getAttribute();
						final String value = ((ValueTuple) aimg).getValueAsString(i);
						if (value != null) {
							if (!helpVars.contains(varname)) {
								AttrVariableMember vm = ac.getVariables()
										.getVarMemberAt(varname);
								vm.setExprAsText(value);
								testVars.addElement(varname);
							}

							for (int j = 0; j < helpVars.size(); j++) {
								final String vn = helpVars.get(j);
								if (varname.equals(vn)) {
									final AttrVariableMember vm = ac.getVariables()
											.getVarMemberAt(varname);
									if (!vm.isSet()) {
										vm.setExprAsText(value);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Unset variable of VarTuple varTuple, if it was used as a test or a help
	 * variable. Vector testVars contains the names of test variables. Vector
	 * helpVars contains the names of help variables.
	 */
	private void unsetVariable(final VarTuple varTuple, final Vector<String> helpVars,
			final Vector<String> testVars) {
		// unset help vars
		for (int k = 0; k < varTuple.getNumberOfEntries(); k++) {
			final VarMember varm = varTuple.getVarMemberAt(k);
			for (int j = 0; j < helpVars.size(); j++) {
				if (varm.getName().equals(helpVars.get(j))) {
					varm.setExpr(null);
				}
			}
		}
		// unset test vars
		for (int j = 0; j < testVars.size(); j++) {
			varTuple.getVarMemberAt(testVars.get(j)).setExpr(null);
		}
	}

	/**
	 * Declare variables of the graph g in attribute context ac, if there are
	 * not already declared. Returns vector with names of the new declared
	 * variables.
	 */
	private Vector<String> declareVariables(final AttrContext ac, final Graph g) {
		final Vector<String> results = new Vector<String>();
		
		doDeclareVars(ac, g.getNodesSet().iterator(), results);
		doDeclareVars(ac, g.getArcsSet().iterator(), results);
		
		return results;
	}

	private void doDeclareVars(
			final AttrContext ac, 
			final Iterator<?> elems, 
			final Vector<String> results) {
		
		final ContextView cv = (ContextView) ac;
		while (elems.hasNext()) {
			final GraphObject go = (GraphObject) elems.next();
			if (go.getAttribute() == null) {
				continue;
			}
			final AttrInstance ai = go.getAttribute();
			for (int i = 0; i < ai.getNumberOfEntries(); i++) {
				final AttrInstanceMember am = (AttrInstanceMember) ai.getMemberAt(i);
				if (am.isSet() && am.getExpr().isVariable()) {
					final String varname = am.getExprAsText();
					if (!cv.isDeclared(varname)) {
						final DeclMember decl = (DeclMember) am.getDeclaration();
						cv.addDecl(decl.getHandler(), decl.getTypeName(), varname);
						results.add(varname);
					}
				}
			}
		}
	}
	
	/**
	 * Add attribute condition member from AttrConditionTuple conds to
	 * AttrContext ac, if it is not already in ac.
	 */
	private void declareConditions(final AttrContext ac, final AttrConditionTuple conds) {
		final AttrConditionTuple condTuple = ac.getConditions();
		for (int i = 0; i < conds.getNumberOfEntries(); i++) {
			final CondMember 
			cm = (CondMember) conds.getMemberAt(i);
			boolean found = false;
			for (int i1 = 0; i1 < condTuple.getNumberOfEntries(); i1++) {
				final CondMember 
				cm0 = (CondMember) condTuple.getMemberAt(i1);
				if (cm0.getExprAsText().equals(cm.getExprAsText())) {
					found = true;	
					break;
				}
			}
			if (!found) {
//				CondMember condm = (CondMember) 
				condTuple.addCondition(cm.getExprAsText());
			}
		}
	}
	
	private void markAttrConditions(final AttrContext attrContext) {
		final VarTuple avt = (VarTuple) attrContext.getVariables();
		final CondTuple act  = (CondTuple) attrContext.getConditions();
	
		for (int k = 0; k < act.getSize(); k++) {
			final CondMember cm = act.getCondMemberAt(k);		
			cm.setMark(CondMember.LHS);
			final Vector<String> vars = cm.getAllVariables();
			if (!vars.isEmpty()) {
				for (int i=0; i<vars.size(); i++) {					
					final VarMember var = avt.getVarMemberAt(vars.get(i));
					if (var != null) {							
						if (var.getMark() == VarMember.RHS) {
							cm.setMark(CondMember.RHS);
							break;
						}					
					}
				}
			}
		}
	}
	
	private void markUsedVariables(final OrdinaryMorphism m) {
		final VarTuple avt = (VarTuple) m.getAttrContext().getVariables();
		// mark used variables: RHS, LHS
		for (Iterator<Node> e1 = m.getTarget().getNodesSet().iterator(); e1.hasNext();) {
			final GraphObject o = e1.next();
			if (o.getAttribute() == null)
				continue;
			final ValueTuple vt = (ValueTuple) o.getAttribute();
			for (int k = 0; k < vt.getSize(); k++) {
				final ValueMember vm = vt.getValueMemberAt(k);
				if (vm.isSet() && vm.getExpr().isVariable()) {
					final VarMember var = avt.getVarMemberAt(vm.getExprAsText());
					if (var != null)
						var.setMark(VarMember.RHS); // 1
				}
			}
		}
		
		for (Iterator<Arc> e2 = m.getTarget().getArcsSet().iterator(); e2.hasNext();) {
			final GraphObject o = e2.next();
			if (o.getAttribute() == null)
				continue;
			ValueTuple vt = (ValueTuple) o.getAttribute();
			for (int k = 0; k < vt.getSize(); k++) {
				ValueMember vm = vt.getValueMemberAt(k);
				if (vm.isSet() && vm.getExpr().isVariable()) {
					VarMember var = avt.getVarMemberAt(vm.getExprAsText());
					if (var != null)
						var.setMark(VarMember.RHS); // 1
				}
			}
		}
		
		for (Iterator<Node> e1 = m.getSource().getNodesSet().iterator(); e1.hasNext();) {
			final GraphObject o = e1.next();
			if (o.getAttribute() == null)
				continue;
			final ValueTuple vt = (ValueTuple) o.getAttribute();
			for (int k = 0; k < vt.getSize(); k++) {
				final ValueMember vm = vt.getValueMemberAt(k);
				if (vm.isSet() && vm.getExpr().isVariable()) {
					final VarMember var = avt.getVarMemberAt(vm.getExprAsText());
					if (var != null)
						var.setMark(VarMember.LHS); // 0
				}
			}
		}
		for (Iterator<Arc> e2 = m.getSource().getArcsSet().iterator(); e2.hasNext();) {
			final GraphObject o = e2.next();
			if (o.getAttribute() == null)
				continue;
			final ValueTuple vt = (ValueTuple) o.getAttribute();
			for (int k = 0; k < vt.getSize(); k++) {
				final ValueMember vm = vt.getValueMemberAt(k);
				if (vm.isSet() && vm.getExpr().isVariable()) {
					final VarMember var = avt.getVarMemberAt(vm.getExprAsText());
					if (var != null)
						var.setMark(VarMember.LHS); // 0
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see agg.cons.Evaluable#evalForall(java.lang.Object, int, boolean)
	 */
	public boolean evalForall(Object o, int tick) {
		// TODO Auto-generated method stub
		return false;
	}

	public String getName() {
		return "Unnamed";
	}
	
/*	
	private void showMorphismData(final OrdinaryMorphism m) {
		System.out.println("Morphism data:  source graph, target graph, mappings::");
//		System.out.println(m.getSource());
//		System.out.println(m.getTarget());
		final Enumeration<GraphObject> dom = m.getDomain();
		while(dom.hasMoreElements()) {
			GraphObject go = dom.nextElement();
			System.out.println(go +" >>> "+m.getImage(go));
		}
	}
	*/
	
}
