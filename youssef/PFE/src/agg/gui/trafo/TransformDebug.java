/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.trafo;

import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;

import agg.attribute.AttrConditionTuple;
import agg.attribute.AttrContext;
import agg.attribute.AttrException;
import agg.attribute.AttrVariableTuple;
import agg.attribute.impl.VarMember;
import agg.attribute.impl.CondMember;
import agg.editor.impl.EdGraph;
import agg.editor.impl.EdRule;
import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdRuleScheme;
import agg.gui.event.EditEvent;
import agg.gui.event.EditEventListener;
import agg.gui.event.TransformEvent;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.Completion_NAC;
import agg.xt_basis.DefaultGraTraImpl;
import agg.xt_basis.GraTra;
import agg.xt_basis.GraTraEvent;
import agg.xt_basis.GraTraEventListener;
import agg.xt_basis.GraTraOptions;
import agg.xt_basis.MorphCompletionStrategy;
import agg.xt_basis.ParallelRule;
import agg.xt_basis.StaticStep;
import agg.xt_basis.TypeException;
import agg.xt_basis.Match;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.Type;
import agg.xt_basis.agt.AmalgamatedRule;
import agg.xt_basis.agt.KernelRule;
import agg.xt_basis.agt.MultiRule;
import agg.xt_basis.agt.RuleScheme;
import agg.xt_basis.csp.CompletionPropertyBits;
import agg.xt_basis.csp.Completion_PartialInjCSP;
import agg.util.Pair;

/**
 * The class TransformDebug implements so called step-by-step graph transformation.
 * 
 * @author $Author: olga $
 * @version $Id: TransformDebug.java,v 1.33 2010/12/01 20:27:48 olga Exp $
 */
public class TransformDebug implements GraTraEventListener, EditEventListener {

	/** Creates a new instance */
	public TransformDebug(final GraGraTransform transform) {
		this.gragraTransform = transform;
		this.gratra = new DefaultGraTraImpl();
		this.gratra.addGraTraListener(this);
		this.completeInputParameterNotSet = false;
		this.stepInputParameterNotSet = false;
	}
	
	public void dispose() {
		this.gratra.dispose();
		this.match = null;
		this.lastValidMatch = null;
		this.rule = null;
		this.ac = null;
		this.act = null;
		this.avt = null;
	}
	
	public void refreshAsGraTraListener() {
		this.gratra.addGraTraListener(this);
	}
	
	public Vector<Rule> getApplicableRules(final EdGraGra gragra) {
		// strategy.showProperties();
		Vector<Rule> applicableRules = new Vector<Rule>();
		Object test = gragra.getBasisGraGra().isReadyToTransform();
		boolean testg = gragra.getBasisGraGra().isGraphReadyForTransform();
		if (test != null) {
			if (test instanceof Rule)
				JOptionPane.showMessageDialog(null,
						"Something is wrong.\nPlease check attribute settings of the rule \""
								+ ((Rule) test).getName() + "\".");
		} else if (!testg) {
			JOptionPane
					.showMessageDialog(null,
							"Something is wrong.\nPlease check attribute settings of the host graph.");
		}
		else {
			applicableRules.addAll(gragra.getApplicableRules(this.strategy));
			this.gragraTransform.fireTransform(new TransformEvent(this,
					TransformEvent.APPLICABLE_RULES, applicableRules));
		}
		return applicableRules;
	}

	/* Set all rules to be applicable or not and return all rules. */
	public Vector<EdRule> getApplicableRules(EdGraGra gragra, boolean applicable) {
		for (int i = 0; i < gragra.getRules().size(); i++) {
			EdRule r = gragra.getRules().elementAt(i);
			r.setApplicable(applicable);
		}
		this.gragraTransform.fireTransform(new TransformEvent(this,
				TransformEvent.APPLICABLE_RULES, gragra.getRules()));
		return gragra.getRules();
	}

	/** Sets the current completion strategy */
	public void setCompletionStrategy(MorphCompletionStrategy strat) {
		this.strategy = strat;
		this.gratra.setCompletionStrategy(this.strategy);		
	}

	/** Sets the current rule to apply */
	public void setRule(EdRule r) {
		this.rule = r;
		this.ac = null;
		this.avt = null;
		this.act = null;
		
		if (this.rule == null) {
			this.gratra.setGraGra(null);
			return;
		}
			
		if ((this.gratra.getGraGra() == null)
				|| (this.gratra.getGraGra() != this.rule.getGraGra().getBasisGraGra())) {
			this.rule.getGraGra().getBasisGraGra().setGraTraOptions(this.strategy);
			this.gratra.setGraGra(this.rule.getGraGra().getBasisGraGra());
		}
		this.gratra.setHostGraph(this.rule.getGraGra().getBasisGraGra().getGraph());
		
		this.completeInputParameterNotSet = true;
		this.stepInputParameterNotSet = true;
	}

	/** Returns the current rule. */
	public EdRule getRule() {
		return this.rule;
	}

	/** Returns the current match */
	public Match getMatch() {
		return this.match;
	}

	private Match getMatch(EdRule r) {
		Match m = null;
		if (r.getBasisRule().getRuleScheme() != null) {
			if (r.getBasisRule() instanceof KernelRule
					|| r.getBasisRule() instanceof MultiRule
					|| r.getBasisRule() instanceof AmalgamatedRule) {
				m = r.getBasisRule().getMatch();
			}
		}
		else {
			m = r.getBasisRule().getMatch();
		}
		
		return m;
	}

	/** Creates a new match */
	public void matchDef() {
		if (this.rule == null) {
			return;
		}
		else if (!this.rule.getName().equals(this.lastErrorMsg.second)) {
			this.lastErrorMsg.second = this.rule.getName();
		}
		this.lastErrorMsg.first = "";
		
		if (!checkIfReadyForTransform())
			return;

//		gragraTransform.fireTransform(new TransformEvent(this,
//				TransformEvent.MATCH_DEF));

		this.match = getMatch(this.rule);

		if (this.match == null) {
			newMatch();
		} else {
			clearMatch();
			setMatch();
			
//			gragraTransform.fireTransform(new TransformEvent(this,
//					TransformEvent.CLEAR_MATCH));
			resetTargetGraphOfMatchIfNeeded();
			this.match.setTypeObjectsMapChanged(true);
			this.match.setPartialMorphismCompletion(false);
			
		}
		
		this.gragraTransform.fireTransform(new TransformEvent(this,
				TransformEvent.CLEAR_MATCH));
		
		this.gragraTransform.fireTransform(new TransformEvent(this,
				TransformEvent.MATCH_DEF));
	}

	int count = -1;
	
	/** Completes the current match */
	public void nextCompletion() {
		if (this.rule == null) {
			return;
		}
		else if (!this.rule.getName().equals(this.lastErrorMsg.second)) {
			this.lastErrorMsg.first = "";
			this.lastErrorMsg.second = this.rule.getName();
		}

		if (!checkIfReadyForTransform())
			return;

		this.gragraTransform.fireTransform(new TransformEvent(this,
				TransformEvent.NEXT));
		
//		System.gc();
//		long t0 = java.lang.Runtime.getRuntime().freeMemory();
//		System.out.println("Free memory:  "+t0);
		
		this.match = getMatch(this.rule);
		if (this.match == null) {
			this.completeInputParameterNotSet = true;
			this.stepInputParameterNotSet = true;

			newMatch();
			
			// test parallel rule
//			if (this.rule.getBasisRule() instanceof ParallelRule) {
//				this.completeInputParameterNotSet = false;
//				this.matchIsValid = ((ParallelRule)this.rule.getBasisRule()).makeMatch(this.gratra.getGraGra().getGraph(), 
//						this.gratra.getGraGra().getMorphismCompletionStrategy());
//			} else
				
			if (this.match != null && this.match.canComplete()) {
				if (areAllInputParameterSet(this.rule.getLeft(), true)) {
					this.completeInputParameterNotSet = false;
					this.matchIsValid = doNextCompletion();
				} else
					return;
			}
		} else {
			setMatch();
			resetTargetGraphOfMatchIfNeeded();
			if (this.match.isEmpty()) {
				if (!this.match.getRule().isParallelApplyEnabled())
					this.match.getCompletionStrategy().resetSolver(true);
			}
//			partialMatchCompletion = match.hasPartialMorphismCompletion();
			
			if (!this.match.isTotal()) {
				this.matchIsValid = false;
				if (areAllInputParameterSet(this.rule.getLeft(), true)) {
					this.completeInputParameterNotSet = false;
					this.matchIsValid = doNextCompletion();
				} else
					return;
			} else { // isTotal()
				this.completeInputParameterNotSet = true;
				this.stepInputParameterNotSet = true;
				if (areAllInputParameterSet(this.rule.getLeft(), true)) {
					this.completeInputParameterNotSet = false;
					if (this.matchIsValid) {
						this.completeInputParameterNotSet = true;
						if (areAllInputParameterSet(this.rule.getLeft(), true)) {
							this.completeInputParameterNotSet = false;
							this.matchIsValid = doNextCompletion();
						} else
							return;
					} else if (!this.match.isAttrConditionSatisfied()) {
						this.matchIsValid = doNextCompletion();
					} 
					else if (!this.match.areNACsSatisfied()) {
						this.matchIsValid = doNextCompletion();
					} 
					else if (!this.match.arePACsSatisfied()) {
						this.matchIsValid = doNextCompletion();
					} 
					else if (!this.match.getRule().evalFormula()) {
						this.matchIsValid = doNextCompletion();
					}
					else if (!this.match.isParallelArcSatisfied()) {
						this.matchIsValid = doNextCompletion();
					}
					else {
						this.matchIsValid = this.match.isValid();
					}
				} else
					return;
			}
		}
//		long t1 = java.lang.Runtime.getRuntime().freeMemory();
//		System.out.println("Free memory:  "+t1);
//		System.out.println("Used memory:  "+(t0-t1));
//		System.gc();
//		t1 = java.lang.Runtime.getRuntime().freeMemory();
//		System.out.println("after GC: Free memory:  "+t1);
//		System.out.println("Used memory:  "+(t0-t1));
		
		if (this.matchIsValid) {
			this.rule.update();
			this.lastValidMatch = this.match;
			this.gragraTransform.fireTransform(new TransformEvent(this,
					TransformEvent.MATCH_COMPLETED, this.match));
		} else {
			String msg = this.lastErrorMsg.first;
			destroyMatch();
			// clearMatch();
			// match.setTypeObjectsMapChanged(true);
			if (this.gragraTransform.selectMatchObjectsEnabled()) {
				this.rule.getGraGra().getGraph().deselectAll();
			}			
			this.gragraTransform.fireTransform(new TransformEvent(this,
					TransformEvent.NO_COMPLETION, msg));
		}
	}

	
	/** Uses the current match to the step transformation */
	public synchronized void step() {
		if (this.rule == null || !checkIfReadyForTransform()) {
			return;			
		}
		else if (!this.rule.getName().equals(this.lastErrorMsg.second)) {
			this.lastErrorMsg.first = "";
			this.lastErrorMsg.second = this.rule.getName();
		}
		
		this.gragraTransform.fireTransform(new TransformEvent(this,
				TransformEvent.STEP));
		this.gratra.setGraTraOptions(this.gragraTransform.getGraTraOptions());		
		
		
		if (this.rule.getBasisRule().getRuleScheme() != null) {
			this.rule.getBasisRule().getRuleScheme().clearMatchesOfMultiRules();
			this.completeInputParameterNotSet = false;
			this.stepInputParameterNotSet = true;
			if (areAllInputParameterSet(this.rule.getBasisRule().getRuleScheme(), true)) {				
				if (!this.gratra.apply(this.rule.getBasisRule().getRuleScheme())) {
					destroyMatch();
					String errmsg = this.rule.getBasisRule().getRuleScheme().getErrorMsg();
					if ("".equals(errmsg)) 
						errmsg = "Amalgamated match failed.";				
					this.rule.updateMatch();
					this.gragraTransform.fireTransform(new TransformEvent(this,
							TransformEvent.CANNOT_TRANSFORM, errmsg));
				}
			}
			return;
		}
		
		this.match = getMatch(this.rule);

		if (this.match == null) {
			this.completeInputParameterNotSet = false;
			this.stepInputParameterNotSet = true;

			newMatch();					
			
			if (this.match != null && this.match.canComplete()) {
				if (areAllInputParameterSet(this.rule.getLeft(), true)) {
					this.stepInputParameterNotSet = false;
					this.matchIsValid = doNextCompletion();
				} else {
					return;
				}
			}
		} else {						
			setMatch();
			resetTargetGraphOfMatchIfNeeded();

			if (this.match.hasPartialMorphismCompletion()) {
//				partialMatchCompletion = true;
			}
			else {
//				partialMatchCompletion = false;
				if (this.match.isEmpty()
						&& !this.match.getRule().isParallelApplyEnabled()) {
					this.match.getCompletionStrategy().resetSolver(true);
				}
			}

			if (!this.match.isTotal()) {
				this.matchIsValid = false;
				if (areAllInputParameterSet(this.rule.getLeft(), true)) {
					this.stepInputParameterNotSet = false;
					this.matchIsValid = doNextCompletion();
					if (!this.matchIsValid
							&& this.match.getRule().isParallelApplyEnabled()) {
						clearMatch();
						this.match.setTypeObjectsMapChanged(true);
						this.matchIsValid = doNextCompletion();
					}
					if (!this.matchIsValid) {
						String msg = (this.lastErrorMsg.first.length() > 0)?
										this.lastErrorMsg.first:
											"The rule  \""+this.rule.getName()+"\"  doesn't match.";

						destroyMatch();
						this.gragraTransform.fireTransform(new TransformEvent(this,
								TransformEvent.CANNOT_TRANSFORM, msg));
						return;						
					}
				} else {
					return;
				}
			} else if (this.match.equals(this.lastValidMatch)) {
				this.matchIsValid = true;
			} else if (this.matchIsValid) {
			}
			else { // match is total
				boolean totalOK = true;
				this.completeInputParameterNotSet = false;
				if (areAllInputParameterSet(this.rule.getRight(), false)) {
					this.stepInputParameterNotSet = false;			
				
				if (!this.match.areNACsSatisfied()) {
					this.lastErrorMsg.first = this.match.getErrorMsg();
					totalOK = false;
				} else if (!this.match.arePACsSatisfied()) {
					this.lastErrorMsg.first = this.match.getErrorMsg();
					totalOK = false;
				}
				else if (!this.match.getRule().evalFormula()) {
					this.lastErrorMsg.first = this.match.getErrorMsg();
					totalOK = false;
				}
				else if (!this.match.isParallelArcSatisfied()) {
					this.lastErrorMsg.first = this.match.getErrorMsg();
					totalOK = false;
				}
				
				if (totalOK && this.match.isValid()) {
					this.matchIsValid = true;
				}
				
				} else {
					return;
				}
			}
		}

		if (this.matchIsValid) {
			this.completeInputParameterNotSet = false;
			if ( 
//				((rule instanceof EdRuleScheme) 
//					&& areAllInputParameterSet((EdRuleScheme) rule, false))
//						|| 
						areAllInputParameterSet(this.rule.getRight(), false)) {
				this.stepInputParameterNotSet = false;
				if (isReadyToTransform()) {
					this.rule.addDeletedMatchMappingToUndo();
					this.rule.undoManagerEndEdit();

					// send event for animation
					this.gragraTransform.fireTransform(new TransformEvent(this,
							TransformEvent.MATCH_VALID, this.match, ""));
					
//					count++;
//					if (count == -1) {
//						// test minimal rule of trafo span
//						minimalRuleOfTrafoSpan(rule.getBasisRule(), match);
//						count++;
//					} else if (mod1 == null) {
//						setMod1();					
//					} else if (mod2 == null) {
//						setMod2();					
//					} else {
//						// test merge of graph modifications
//						merge(mod1, mod2);
//					} 
						
					this.gratra.apply(this.match);
					
				} else {
					destroyMatch();
				}
			}
		} else {
			String msg = (this.lastErrorMsg.first.length() > 0)?
							this.lastErrorMsg.first:
							"The rule  \""+this.rule.getName()+"\"  doesn't match.";
			destroyMatch();
			this.gragraTransform.fireTransform(new TransformEvent(this,
					TransformEvent.CANNOT_TRANSFORM, msg));
		}
	}

	// This method is used by editEventOccurred(EditEvent) only.
	private boolean stepByMatch() {
		if (isReadyToTransform()) {

			this.rule.addDeletedMatchMappingToUndo();
			this.rule.undoManagerEndEdit();

			// send event for animation
			this.gragraTransform.fireTransform(new TransformEvent(this,
					TransformEvent.MATCH_VALID, this.match, ""));
			
			this.gratra.apply(this.match);
			return true;
		}
		return false;
	}

	private boolean isTotalMatchValid(Match m) {
		if (!this.match.areNACsSatisfied()) {
			this.lastErrorMsg.first = this.match.getErrorMsg();
		} else if (!this.match.arePACsSatisfied()) {
			this.lastErrorMsg.first = this.match.getErrorMsg();
		}
		else if (!this.match.isValid()) {
			this.lastErrorMsg.first = this.match.getErrorMsg();
		}
		else {
			this.matchIsValid = true;
			return true;
		}
		return false;
	}
	
	/*
	private boolean applyRuleScheme(final EdRule r) {
		// ask what to do	
		RuleScheme rs = r.getBasisRule().getRuleScheme();		
		Object[] options = { "Apply Rule Scheme", "Apply Rule", "Cancel" };
		int answer = 0;		
		answer = JOptionPane.showOptionDialog(null,
				"Please choose what should be done.",
				"Rule Scheme:  "+rs.getSchemeName(), 
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);		
		if (answer == 0) {
			gratra.apply(rs);
			return true;
		}
		return false;
	}
	
	private void refineAbstractNodesOfRHSToCreate() {		
		Vector<EdNode> abstractNodes = rule.getAbstractNodesOfRHSToCreate();
		if (!abstractNodes.isEmpty()) {
			Hashtable<Node, Type> abstract2child = new Hashtable<Node, Type>();
			for (int i=0; i<abstractNodes.size(); i++) {
				EdNode node = abstractNodes.get(i);
//				rule.getRight().deselectAll();
//				rule.getRight().select(node);
				EdType abstractTyp = node.getType();
				SelectChildOfNodeTypeDialog childDialog = new SelectChildOfNodeTypeDialog(null, rule.getTypeSet(), abstractTyp);
				EdType childType = childDialog.getSelectedChildType();
				abstract2child.put(node.getBasisNode(), childType.getBasisType());
//				System.out.println("selected child type: "+childType.getName());
			}
//			rule.getRight().deselectAll();
//			System.out.println("selected child type: "+abstract2child);
//			rule.getBasisRule().setRefinementOfAbstractNodesOfRHSToCreate(abstract2child);
		}
	}
	
	*/
	
	
	/** Implements GraTraEventListener.graTraEventOccurred */
	public void graTraEventOccurred(GraTraEvent e) {
//		System.out.println("TransformDebug.graTraEventOccurred : "+e.getMessage());
		this.msgGraTra = e.getMessage();
		if (this.msgGraTra == GraTraEvent.NO_COMPLETION) {
			System.out.println("TransformDebug.graTraEventOccurred : "+e.getMessage());
			if (e.getMatch().getRule().isParallelApplyEnabled()) {
				e.getMatch().getCompletionStrategy().resetSolver(true);				
			}
		}
		else if (this.msgGraTra == GraTraEvent.MATCH_VALID) {		
			this.rule.getGraGra().getGraph().unsetNodeNumberChanged();
					
			if (this.gragraTransform.selectMatchObjectsEnabled()) {
				this.rule.getGraGra().getGraph().updateAlongMorph(e.getMatch());
			}	
			
		} else if (this.msgGraTra == GraTraEvent.STEP_COMPLETED) {			
			if (this.match == null) {
				this.match = e.getMatch();				
			}
				
			this.rule.getGraGra().getGraph().setXYofNewNode(this.rule, this.match, this.match.getCoMorphism());
					
			if (this.rule.isAnimated()) {
				this.gragraTransform.fireTransform(new TransformEvent(this,
						TransformEvent.ANIMATED_NODE, this.match));
			} else {
				this.gragraTransform.getEditor().doStandardLayoutProc();
			}
			
			if (this.gragraTransform.selectNewAfterStepEnabled()) {
				this.rule.getGraGra().getGraph().updateAlongMorph(this.match.getCoMorphism(), this.rule.getBasisRule());
			}
						
			if (!this.rule.getBasisRule().isParallelApplyEnabled()) {
				destroyMatch();
			}
			else {
				clearMatch();
			}
			
			this.gragraTransform.fireTransform(new TransformEvent(this,
					TransformEvent.STEP_COMPLETED));
			
		} else if (this.msgGraTra == GraTraEvent.CANNOT_TRANSFORM) {
			String msg = (this.match != null 
							&& !"".equals(this.match.getErrorMsg()))? 
									this.match.getErrorMsg(): e.getMessageText();
			if ("".equals(msg)) {
				msg = "Undefined error occured. \nPlease check the rule  "+this.rule.getName()+"  resp. its match.";
			}
			destroyMatch();
			// clearMatch();
			this.rule.getGraGra().getGraph().update();
			this.gragraTransform.fireTransform(new TransformEvent(this,
					TransformEvent.CANNOT_TRANSFORM, msg));
			
		} else if (this.msgGraTra == GraTraEvent.INCONSISTENT) {
			String msg = "";
			if (this.rule != null && this.rule.getBasisRule() != null) {
				msg = "Inconsistency of the host graph after the rule < "
					+ this.rule.getBasisRule().getName() + ">  !";
				
				this.gragraTransform.fireTransform(new TransformEvent(this,
						TransformEvent.INCONSISTENT, msg));
				if (!this.rule.getBasisRule().isParallelApplyEnabled()) {
					destroyMatch();
				}
				else {
					clearMatch();
				}
				this.rule.getGraGra().getGraph().update();
			}
			else {
				msg = "Inconsistency of the host graph after " +
						"this rule applied!";
				this.gragraTransform.fireTransform(new TransformEvent(this,
						TransformEvent.INCONSISTENT, msg));
			}

		} else if (this.msgGraTra == GraTraEvent.MATCH_FAILED) {
			System.out.println("TransformDebug.graTraEventOccurred : "+e.getMessage());
		}
	}

	/** Implements EditEventListener.editEventOccurred */
	public void editEventOccurred(EditEvent e) {
		if (e.getMsg() == EditEvent.INPUT_PARAMETER_OK) {
			if (this.match == null
					|| this.rule.getBasisRule().getRuleScheme() != null) {
				// we have to do with a RuleScheme
				if (this.rule != null 
						&& this.rule.getBasisRule() != null			
						&& this.rule.getBasisRule().getRuleScheme() != null) {
//					((VarTuple) rule.getBasisRule().getRuleScheme().getAttrContext().getVariables()).showVariables();						
					this.rule.getBasisRule().getRuleScheme().applyValueOfInputParameter();
//					rule.getBasisRule().getRuleScheme().adaptAttrContextValuesFromExistingObjMapping();
					if (this.completeInputParameterNotSet) {
						this.completeInputParameterNotSet = false;
						nextCompletion();
					} else if (this.stepInputParameterNotSet) {
						this.stepInputParameterNotSet = false;
						if (!this.gratra.apply(this.rule.getBasisRule().getRuleScheme())) {
							this.gragraTransform.fireTransform(new TransformEvent(this,
										TransformEvent.CANNOT_TRANSFORM, 
										"Amalgamated match failed."));
						}
					}
				}
			}			
			else {
				if (this.completeInputParameterNotSet) {
					this.completeInputParameterNotSet = false;
					nextCompletion();
				} else if (this.stepInputParameterNotSet) {
					this.stepInputParameterNotSet = false;
					if (!this.matchIsValid) {
						if (this.match.isTotal())
							this.matchIsValid = isTotalMatchValid(this.match);
						else {
							this.matchIsValid = doNextCompletion();
							if (!this.matchIsValid) {
//								String msg = (this.match != null)? this.match.getErrorMsg() : "";
								destroyMatch();
								this.gragraTransform.fireTransform(new TransformEvent(this,
										TransformEvent.NO_COMPLETION, ""));
							}								
						}
					} 
				}
	
				if (this.matchIsValid && !this.stepInputParameterNotSet) {
					if (!stepByMatch() && this.match != null) {
						String msg = this.match.getErrorMsg();
						destroyMatch();
						// clearMatch();
						this.gragraTransform.fireTransform(new TransformEvent(this,
								TransformEvent.NO_COMPLETION, msg));
					}
				}
			}
		}
	}

	private void newMatch() {
		if (this.rule.getBasisRule().getRuleScheme() != null) {
			if (this.rule.getBasisRule() instanceof KernelRule) {
				this.match = this.rule.getGraGra().getRuleScheme(this.rule.getBasisRule()).getBasisRuleScheme()
						.getKernelMatch(this.rule.getGraGra().getBasisGraGra().getGraph());			
			} 
			else if (this.rule.getBasisRule() instanceof MultiRule) {
				RuleScheme rs = this.rule.getGraGra().getRuleScheme(this.rule.getBasisRule()).getBasisRuleScheme();
				rs.getKernelMatch(this.rule.getGraGra().getBasisGraGra().getGraph());
				this.match = ((MultiRule) this.rule.getBasisRule()).getMatch(rs.getKernelRule());			
			} 
			else if (this.rule.getBasisRule() instanceof AmalgamatedRule) {
				this.match = this.rule.getBasisRule().getMatch();
			}
		}
		else {
			this.match = this.rule.getGraGra().getBasisGraGra().createMatch(this.rule.getBasisRule());
		}
				
		if (this.match != null) {
			this.match.addObserver(this.rule.getLeft());
			this.match.addObserver(this.rule.getGraGra().getGraph());
			
			if (this.rule.getBasisRule() instanceof ParallelRule) {
				if (this.gragraTransform.getGraTraOptions().hasOption(GraTraOptions.NACS)
						|| this.gragraTransform.getGraTraOptions().hasOption(GraTraOptions.PACS)
						|| this.gragraTransform.getGraTraOptions().hasOption(GraTraOptions.GACS)) {
					this.match.setCompletionStrategy(
							new Completion_NAC(new Completion_PartialInjCSP()), true);					
				} else {
					this.match.setCompletionStrategy(new Completion_PartialInjCSP(), true);
				}
				adjustMatchComplStrategyOfParallelRule();
			} 
			else {
				this.match.setCompletionStrategy(
						(MorphCompletionStrategy) this.strategy.clone(),
						true);
			}
			
			if (this.rule.getBasisRule() instanceof KernelRule
					|| this.rule.getBasisRule() instanceof MultiRule) {
				this.match.getCompletionStrategy().getProperties()
					.set(CompletionPropertyBits.DANGLING, false);
			}
			
			// strategy.showProperties();
	
			this.ac = this.match.getAttrContext();
			this.avt = this.ac.getVariables();
			this.act = this.ac.getConditions();
			
			this.lastErrorMsg.first = "";
			this.lastErrorMsg.second = "";
			
			this.matchIsValid = false;
			this.lastValidMatch = null;
//			this.partialMatchCompletion = false;
		}
	}

	private void adjustMatchComplStrategyOfParallelRule() {
		if (this.gragraTransform.getGraTraOptions().hasOption(GraTraOptions.NACS))
			this.match.getCompletionStrategy().setProperty(GraTraOptions.NACS);
		else
			this.match.getCompletionStrategy().removeProperty(GraTraOptions.NACS);
		
		if (this.gragraTransform.getGraTraOptions().hasOption(GraTraOptions.PACS))
			this.match.getCompletionStrategy().setProperty(GraTraOptions.PACS);
		else
			this.match.getCompletionStrategy().removeProperty(GraTraOptions.PACS);
		
		if (this.gragraTransform.getGraTraOptions().hasOption(GraTraOptions.GACS))
			this.match.getCompletionStrategy().setProperty(GraTraOptions.GACS);
		else
			this.match.getCompletionStrategy().removeProperty(GraTraOptions.GACS);
		
		if (this.gragraTransform.getGraTraOptions().hasOption(GraTraOptions.INJECTIVE))				
			this.match.getCompletionStrategy().setProperty(GraTraOptions.INJECTIVE);
		else
			this.match.getCompletionStrategy().removeProperty(GraTraOptions.INJECTIVE);
		
		if (this.gragraTransform.getGraTraOptions().hasOption(GraTraOptions.DANGLING))
			this.match.getCompletionStrategy().setProperty(GraTraOptions.DANGLING);
		else
			this.match.getCompletionStrategy().removeProperty(GraTraOptions.DANGLING);
		
		if (this.gragraTransform.getGraTraOptions().hasOption(GraTraOptions.IDENTIFICATION))
			this.match.getCompletionStrategy().setProperty(GraTraOptions.IDENTIFICATION);
		else
			this.match.getCompletionStrategy().removeProperty(GraTraOptions.IDENTIFICATION);
//		this.match.getCompletionStrategy().showProperties();
	}
	
	private void setMatch() {
		if (this.rule.getMatch() != this.match) {
			this.ac = this.match.getAttrContext();
			this.avt = this.ac.getVariables();
			this.act = this.ac.getConditions();
			
			this.matchIsValid = false;
			this.lastValidMatch = null;
//			this.partialMatchCompletion = false;
		}
	}

	private boolean doNextCompletion() {
		if (this.match == null)
			return false;
				
		this.rule.addCreatedMatchMappingToUndo();
		boolean completionDone = false;
		while (this.match.nextCompletion()) {
			completionDone = true;
			if (this.match.isValid()) {
				this.lastErrorMsg.first = "";
				this.rule.undoManagerEndEdit();				
				return true;
			} 
			this.lastErrorMsg.first = this.match.getErrorMsg();
			this.match.clear();		
		}
		if (!completionDone)
			this.lastErrorMsg.first = this.match.getErrorMsg();
		
		this.match.clear();
		this.rule.undoManagerLastEditDie();
		return false;
	}

	private void resetTargetGraphOfMatchIfNeeded() {
		if (this.match != null
				&& this.match.getTarget() != this.rule.getGraGra().getGraph()
						.getBasisGraph()) {
			this.match.resetTarget(this.rule.getGraGra().getGraph().getBasisGraph());
			this.match.setTypeObjectsMapChanged(true);
		}
	}

	public void destroyMatch() {
//		long t0 = java.lang.Runtime.getRuntime().freeMemory();
//		System.out.println("destroyMatch   Free memory:  "+t0);

		if (this.match == null || this.match.getRule() == null) {
			return;
		}
		
		if (this.rule.getBasisRule().getRuleScheme() == null) {
			// plain rule
			this.rule.getGraGra().getBasisGraGra().destroyMatch(this.match);
		}
//		else if (this.rule.getBasisRule().getRuleScheme() != null) 
//			this.rule.getBasisRule().getRuleScheme().disposeMatch();			
				
		this.rule.update();
		
		this.match = null;
		this.ac = null;
		this.act = null;
		this.avt = null;
		this.lastValidMatch = null;
		this.matchIsValid = false;
		this.completeInputParameterNotSet = true;
		this.stepInputParameterNotSet = true;
		
//		System.gc();
//		long t1 = java.lang.Runtime.getRuntime().freeMemory();
//		System.out.println("after GC:  destroyMatch   Free memory:  "+t1);
//		System.out.println("destroyMatch   more memory:  "+(t1-t0));
	}

	protected void clearMatch() {
//		long t0 = java.lang.Runtime.getRuntime().freeMemory();
//		System.out.println("clearMatch   Free memory:  "+t0);
		if (this.match != null) {
			this.match.clear();
			this.rule.update();
		}
		this.lastValidMatch = null;
		this.matchIsValid = false;
//		this.partialMatchCompletion = false;
		
		this.completeInputParameterNotSet = true;
		this.stepInputParameterNotSet = true;
		
//		System.gc();
//		long t1 = java.lang.Runtime.getRuntime().freeMemory();
//		System.out.println("clearMatch   Free memory:  "+t1);
//		System.out.println("clearMatch   Used memory:  "+(t1-t0));
	}

	private boolean areAllInputParameterSet(RuleScheme rs, boolean left) {
		if (rs.getAmalgamatedRule() == null
				&& !rs.isInputParameterSet(left)) {
			int answer = parameterWarning(rs.getName());
			if (answer == JOptionPane.YES_OPTION) {
				
				this.gragraTransform.fireTransform(new TransformEvent(this,
						TransformEvent.INPUT_PARAMETER_NOT_SET, 
						rs));
			} else {
				this.gragraTransform
				.fireTransform(new TransformEvent(
						this,
						TransformEvent.CANCEL,
						rs));
//				rs.disposeMatch();
				rs.disposeAmalgamatedRule();
			}
			return false;
		}
		return true;
	}
	
	private boolean areAllInputParameterSet(EdGraph g, boolean left) {
		if (this.match == null)
			return true;

		this.ac = this.match.getAttrContext();
		this.avt = this.ac.getVariables();

		if ((this.avt == null) 
				|| (this.avt.getNumberOfEntries() == 0)
				|| this.avt.areInputParametersSet()) {
			return true;
		}
		
		this.act = this.ac.getConditions();
		for (int i = 0; i < this.avt.getNumberOfEntries(); i++) {
			VarMember v = this.avt.getVarMemberAt(i);
			if (v.isInputParameter() && !v.isSet()) {
				if (g.isUsingVariable(v)) {
					int answer = parameterWarning(this.match.getRule().getName());
					if (answer == JOptionPane.YES_OPTION) {
						this.gragraTransform.fireTransform(new TransformEvent(this,
								TransformEvent.INPUT_PARAMETER_NOT_SET, this.match));
					} else {
						this.gragraTransform
						.fireTransform(new TransformEvent(
								this,
								TransformEvent.CANCEL,
								this.match));
					}
					return false;
				} else if (left) {
					final List<OrdinaryMorphism> nacs = this.rule.getBasisRule().getNACsList();
					for (int l=0; l<nacs.size(); l++) {
						final OrdinaryMorphism nac = nacs.get(l);					
						if (nac.getTarget().isUsingVariable(v)) {
							int answer = parameterWarning(this.match.getRule().getName());
							if (answer == JOptionPane.YES_OPTION) {
								this.gragraTransform
										.fireTransform(new TransformEvent(
												this,
												TransformEvent.INPUT_PARAMETER_NOT_SET,
												this.match));
							}
							else {
								this.gragraTransform
								.fireTransform(new TransformEvent(
										this,
										TransformEvent.CANCEL,
										this.match));
							}						
							return false;
						} 
						Vector<String> nacVars = nac.getTarget()
									.getVariableNamesOfAttributes();
						for (int j = 0; j < nacVars.size(); j++) {
							String varName = nacVars.get(j);
							for (int k = 0; k < this.act.getNumberOfEntries(); k++) {
								CondMember cond = (CondMember) this.act
											.getMemberAt(k);
								Vector<String> condVars = cond.getAllVariables();
								if (condVars.contains(varName)
										&& condVars.contains(v.getName())) {
									int answer = parameterWarning(this.match.getRule().getName());
									if (answer == JOptionPane.YES_OPTION) {
										this.gragraTransform
													.fireTransform(new TransformEvent(
															this,
															TransformEvent.INPUT_PARAMETER_NOT_SET,
															this.match));
									} else {
										this.gragraTransform
											.fireTransform(new TransformEvent(
													this,
													TransformEvent.CANCEL,
													this.match));
									}
									return false;
								}
							}
						}
						
					}
				}
			}
		}
		return true;
	}

	/*
	private boolean areAllInputParameterSet() {
		if (match != null) {
			ac = match.getAttrContext();
			avt = ac.getVariables();
		}
		if (avt == null)
			return true;
		if (!avt.areInputParametersSet()) {
			int answer = parameterWarning(match.getRule().getName());
			if (answer == JOptionPane.YES_OPTION) {
				gragraTransform.fireTransform(new TransformEvent(this,
						TransformEvent.INPUT_PARAMETER_NOT_SET, match));
			} else {
				gragraTransform
				.fireTransform(new TransformEvent(
						this,
						TransformEvent.CANCEL,
						match));
			}
			return false;
		}
		return true;
	}
*/
	
	private boolean checkIfReadyForTransform() {
		Type t = this.gratra.getGraGra().doAttrTypesExist();
		if (t != null) {
			this.gragraTransform.fireTransform(new TransformEvent(this,
					TransformEvent.NOT_READY_TO_TRANSFORM,
					"Not all attribute members of the type :  \"" + t.getName()
							+ "\"  are declared correctly."));
			return false;
		}
		Pair<Object, String> p = this.gratra.getGraGra()
				.checkInheritedAttributesValid();
		if (p != null) {
			this.gragraTransform.fireTransform(new TransformEvent(this,
					TransformEvent.NOT_READY_TO_TRANSFORM, p.second));
			return false;
		}
		if (!this.gratra.getGraGra().getGraph().isReadyForTransform()) {
			this.gragraTransform
					.fireTransform(new TransformEvent(this,
							TransformEvent.NOT_READY_TO_TRANSFORM,
							"Not all attributes of objects of the host graph are set."));
			return false;
		} 
		else if (this.rule instanceof EdRuleScheme) {
			if (!((EdRuleScheme)this.rule).getBasisRuleScheme().isReadyToTransform()) {
				String msgStr = this.rule.getName().concat("   is not ready to transform");
				this.gragraTransform.fireTransform(new TransformEvent(this,
						TransformEvent.NOT_READY_TO_TRANSFORM, msgStr));
				return false;
			}
		}
		else if (!this.rule.getBasisRule().isReadyToTransform()) {
			this.gragraTransform.fireTransform(new TransformEvent(this,
					TransformEvent.NOT_READY_TO_TRANSFORM, this.rule.getBasisRule()
							.getErrorMsg()));
			return false;
		}
		else {
			p = this.gratra.getGraGra().isGraphConstraintReadyForTransform();
			if (p != null) {
				this.gragraTransform.fireTransform(new TransformEvent(this,
						TransformEvent.NOT_READY_TO_TRANSFORM, p.second));
				return false;
			}
		}
		return true;
	}

	private boolean isReadyToTransform() {
		if (this.match != null) {
			this.ac = this.match.getAttrContext();
//			((VarTuple)this.ac.getVariables()).showVariables();
			try {
				this.ac.getVariables().getAttrManager().checkIfReadyToTransform(this.ac);
			} catch (AttrException ex) {
				String s = ex.getLocalizedMessage();
//				System.out.println("TransformDebug.isReadyToTransform::  AttrException: " + s);
				this.gragraTransform.fireTransform(new TransformEvent(this,
						TransformEvent.CANNOT_TRANSFORM, s));
				return false;
			}
			return true;
		} 
		return false;
	}

	private int parameterWarning(String ruleName) {
		Object[] options = { "Set", "Cancel" };
		int answer = JOptionPane.showOptionDialog(null,
				"Input parameter of the rule  \" " + ruleName
						+ " \"  not set!\nDo you want to set parameter?",
				"Warning", JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		return answer;
	}

	@SuppressWarnings("unused")
	private void minimalRuleOfTrafoSpan(final Rule r, Match m) {
		JOptionPane.showMessageDialog(null, "Test : The Minimal Rule of direct Transformation Span");
		try {
			Pair<OrdinaryMorphism,OrdinaryMorphism> po = StaticStep.executeColim(r, m, false, false);
			this.rule.getGraGra().addGraph(new EdGraph(po.first.getTarget()));
//			this.rule.getGraGra().save();
			
			Rule minr = BaseFactory.theFactory().makeMinimalRule(po.first);
			if (minr != null) {
				EdRule minRule = new EdRule(minr);
				if (this.rule.getGraGra().addRule(minRule)) {
					this.rule.getGraGra().save();
					JOptionPane.showMessageDialog(null, "", "Please reload this GraGra to see the Minimal Rule.", JOptionPane.INFORMATION_MESSAGE);
				}
			} else {
				JOptionPane.showMessageDialog(null, "", "Construction of minimal rule FAILED!", JOptionPane.ERROR_MESSAGE);
			}
		} catch (TypeException ex) {}
	}
	
	Pair<OrdinaryMorphism,OrdinaryMorphism> mod1,mod2;
	
	@SuppressWarnings("unused")
	private void setMod1() {
		if (this.mod1 == null) {
			try {
				this.mod1 = StaticStep.executeColim(this.rule.getBasisRule(), this.match, false, false);
			} catch (TypeException ex) {}
		}
	}
	
	@SuppressWarnings("unused")
	private void setMod2() {
		if (this.mod2 == null) {
			try {
				this.mod2 = StaticStep.executeColim(this.rule.getBasisRule(), this.match, false, false);
			} catch (TypeException ex) {}
		}
	}
	
	@SuppressWarnings("unused")
	private void merge(Pair<OrdinaryMorphism,OrdinaryMorphism> po1,
			Pair<OrdinaryMorphism,OrdinaryMorphism> po2) {
		JOptionPane.showMessageDialog(null, "Test : Merging of graph modifications ");
		Pair<OrdinaryMorphism,OrdinaryMorphism> mrg = BaseFactory.theFactory().makeMerge(po1.first, po2.first);
		if (mrg != null) {
			this.rule.getGraGra().addGraph(new EdGraph(mrg.second.getTarget()));
			this.rule.getGraGra().save();
			JOptionPane.showMessageDialog(null, "", "Please reload this GraGra to see the modified graph.", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(null, "", "Merging of graph modifications FAILED.", JOptionPane.ERROR_MESSAGE);
		}
		this.mod1 = null; this.mod2 = null;
	}
	
	private final GraGraTransform gragraTransform;

	EdRule rule;

	Match match;

//	private boolean partialMatchCompletion;

	private Match lastValidMatch;

	private final Pair<String,String>  lastErrorMsg = new Pair<String,String>("",""); 
	
	private MorphCompletionStrategy strategy;

	private boolean matchIsValid;

	private final GraTra gratra;

	private int msgGraTra;

//	private boolean stepCompleted;

	private boolean stepInputParameterNotSet;
	
	private boolean completeInputParameterNotSet;

	private AttrContext ac;

	private AttrVariableTuple avt;

	private AttrConditionTuple act;

//	private boolean inheritanceWarningSent;
}
