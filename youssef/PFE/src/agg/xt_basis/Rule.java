/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.xt_basis;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Hashtable;

import agg.attribute.AttrInstance;
import agg.attribute.AttrConditionTuple;
import agg.attribute.AttrVariableTuple;
import agg.attribute.AttrContext;
import agg.attribute.AttrMapping;
import agg.attribute.impl.AttrTupleManager;
import agg.attribute.impl.DeclMember;
import agg.attribute.impl.DeclTuple;
import agg.attribute.impl.VarTuple;
import agg.attribute.impl.VarMember;
import agg.attribute.impl.ValueTuple;
import agg.attribute.impl.ValueMember;
import agg.attribute.impl.CondTuple;
import agg.attribute.impl.CondMember;
import agg.cons.AtomConstraint;
import agg.cons.Convert;
import agg.cons.AtomApplCond;
import agg.cons.EvalSet;
import agg.cons.Evaluable;
import agg.cons.Formula;
import agg.util.XMLHelper;
import agg.util.XMLObject;
import agg.util.Pair;
import agg.xt_basis.agt.RuleScheme;
import agg.xt_basis.csp.CompletionPropertyBits;

/**
 * At the moment, AGG implements the DPO approach by switching on the
 * dangling-edge condition by default. Switching off the dangling condition allows
 * AGG to simulate the SPO approach. (SG, Aug.1999)
 * 
 * @version $Id: Rule.java,v 1.121 2010/12/16 17:31:39 olga Exp $
 * @author $Author: olga $
 */
public class Rule extends OrdinaryMorphism implements XMLObject {

	protected Formula itsFormula = new Formula(true);	
	protected String formStr = "true";
	protected String formReadStr = "true";
	
	final protected 
	Vector<OrdinaryMorphism> itsACs = new Vector<OrdinaryMorphism>();	
	final protected 
	Vector<OrdinaryMorphism> itsNACs = new Vector<OrdinaryMorphism>();
	final protected 
	Vector<OrdinaryMorphism> itsPACs = new Vector<OrdinaryMorphism>();
	
	// containers for PostApplicationConditions
	transient protected boolean generatePostConstraints;	
	protected Vector<AtomConstraint> itsUsedAtomics; 
	protected Vector<Formula> itsUsedFormulas; 
	transient protected Vector<String> constraintNameSet; 
	transient protected Vector<Formula> constraints;
	transient protected Vector<EvalSet> atom_conditions;

	protected Vector<ShiftedPAC> itsShiftedPACs; 

	transient protected boolean applicable;

	protected boolean parallelMatching;
	
	protected boolean randomCSPDomain;
	
	protected boolean startParallelMatchByFirstCSPVar;

	protected int layer;

	protected int priority;

	protected boolean triggerOfLayer;
	
	transient protected boolean isReady;

	transient protected boolean isDeleting, isNodeDeleting, isCreating, isChanging,
								hasEnabledGACs;

	transient protected List<GraphObject> preserved;
	
	transient protected List<GraphObject> created;
	
	transient protected List<GraphObject> deleted;
	
	transient protected List<GraphObject> forbiden;

	transient protected Hashtable<GraphObject, GraphObject> changedPreserved;

	transient protected List<String> typesWhichNeedMultiplicityCheck;
		
	protected Hashtable<Node, Type> abstractType2childType;
	
	protected Match itsMatch;
		
	protected boolean notApplicable, waitBeforeApply;
			
	private InverseRuleConstructData invConstruct;
	
	
	/**
	 * Create myself.
	 */
	protected Rule() {
		super();
		this.itsName = "Rule";
		
		this.itsOrig.setName("Left");
		this.itsOrig.setKind(GraphKind.LHS);
		
		this.itsImag.setName("Right");
		this.itsImag.setKind(GraphKind.RHS);
		
		this.itsAttrContext = this.itsAttrManager.newContext(AttrMapping.PLAIN_MAP);
		this.itsOrig.setAttrContext(this.itsAttrManager.newLeftContext(this.itsAttrContext));
		this.itsImag.setAttrContext(this.itsAttrManager.newRightContext(this.itsAttrContext));

		this.generatePostConstraints = true;
		this.applicable = true;
	}

	/**
	 * Create myself.
	 */
	protected Rule(TypeSet types) {	
		super(BaseFactory.theFactory().createGraph(types), 
				BaseFactory.theFactory().createGraph(types));
		
		this.itsName = "Rule";
		
		this.itsOrig.setName("Left");
		this.itsOrig.setKind(GraphKind.LHS);
		
		this.itsImag.setName("Right");
		this.itsImag.setKind(GraphKind.RHS);
		
		this.itsAttrContext = this.itsAttrManager.newContext(AttrMapping.PLAIN_MAP);
		this.itsOrig.setAttrContext(this.itsAttrManager.newLeftContext(this.itsAttrContext));
		this.itsImag.setAttrContext(this.itsAttrManager.newRightContext(this.itsAttrContext));

		this.generatePostConstraints = true;
		this.applicable = true;
	}

	/**
	 * Create myself.
	 * 
	 * @param left
	 *            my left graph.
	 * @param right
	 *            my right graph.
	 */
	protected Rule(Graph left, Graph right) {
		super(left, right);
		this.itsName = "Rule";

		this.itsOrig.setName("Left");
		this.itsOrig.setKind(GraphKind.LHS);
		
		this.itsImag.setName("Right");
		this.itsImag.setKind(GraphKind.RHS);
		
		this.itsAttrContext = this.itsAttrManager.newContext(AttrMapping.PLAIN_MAP);
		this.itsOrig.setAttrContext(this.itsAttrManager.newLeftContext(this.itsAttrContext));
		this.itsImag.setAttrContext(this.itsAttrManager.newRightContext(this.itsAttrContext));

		this.generatePostConstraints = true;
		this.applicable = true;
	}

	protected Rule(Graph left, Graph right, AttrContext cont) {
		super(left, right, cont);
		this.itsName = "Rule";

		this.itsOrig.setName("Left");
		this.itsOrig.setKind(GraphKind.LHS);
		
		this.itsImag.setName("Right");
		this.itsImag.setKind(GraphKind.RHS);
		
		this.itsAttrContext = cont;
		this.itsOrig.setAttrContext(this.itsAttrManager.newLeftContext(cont));
		this.itsImag.setAttrContext(this.itsAttrManager.newRightContext(cont));

		this.generatePostConstraints = true;
		this.applicable = true;
	}
		
	public void disposeSuper() {
		super.dispose();
		this.itsMatch = null;
		this.typesWhichNeedMultiplicityCheck = null;
		this.changed = false;
	}
	
	public void dispose() {
		super.dispose();		

		while (!this.itsNACs.isEmpty()) {
			this.itsNACs.get(0).dispose(false, true);
			this.itsNACs.remove(0);
		}
		this.itsNACs.trimToSize();
		
		while (!this.itsPACs.isEmpty()) {
			this.itsPACs.get(0).dispose(false, true);
			this.itsPACs.remove(0);
		}
		this.itsPACs.trimToSize();
		
		while (!this.itsACs.isEmpty()) {
			this.itsACs.get(0).dispose(false, true);
			this.itsACs.remove(0);
		}
		this.itsACs.trimToSize();
		
		this.disposeInverseConstruct();
		
		this.itsOrig.dispose();
		this.itsImag.dispose();
		
		this.itsMatch = null;
		this.typesWhichNeedMultiplicityCheck = null;
		this.changed = false;
//		System.out.println("Rule.dispose:: DONE  "+this.hashCode());
	}
	
	public void finalize() {
//		System.out.println("Rule.finalize()  called  "+this.hashCode());
	}
		
	public void setName(String n) {
		this.itsName = n;
		this.itsFormula.setName("Formula.".concat(n));
	}
	
	/**
	 * Returns its name
	 */
	public String getQualifiedName() {
		return super.getName();
	}
	
	public boolean hasChanged() {
		return this.changed 
					|| this.itsOrig.hasChanged() 
					|| this.itsImag.hasChanged();
	}
	
	private void disposeMatch() {
		if (this.itsMatch != null) {
			this.itsMatch.dispose();
			this.itsMatch = null;
		}		
	}
	
	public void clearRule() {
		disposeMatch();
		while (!this.itsNACs.isEmpty()) {
			this.itsNACs.get(0).dispose(false, true);
			this.itsNACs.remove(0);
		}		
		while (!this.itsPACs.isEmpty()) {
			this.itsPACs.get(0).dispose(false, true);
			this.itsPACs.remove(0);
		}		
		while (!this.itsACs.isEmpty()) {
			this.itsACs.get(0).dispose(false, true);
			this.itsACs.remove(0);
		}
		
		super.clear();
		this.changed = false;
		this.itsOrig.clear();
		this.itsImag.clear();
					
		if (this.typesWhichNeedMultiplicityCheck != null) {			
			this.typesWhichNeedMultiplicityCheck.clear();
			this.typesWhichNeedMultiplicityCheck = null;
		}
	}
	
	public void disposeResultsOfNestedACs() {
		for (int i=0; i<this.itsACs.size(); i++) {
			NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
			ac.disposeResults();
		}
//		System.gc();
	}
	
	/**
	 * Checks if the specified graph is its LHS, RHS, 
	 * target of a NAC, target of a PAC graph.
	 */
	public boolean isElement(Graph g) {
		if (this.itsOrig == g || this.itsImag == g) {
			return true;
		}
		for (int i = 0; i < this.itsNACs.size(); i++) {
			OrdinaryMorphism om = this.itsNACs.get(i);
			if (om.getTarget() == g) {
				return true;
			}
		}
		for (int i = 0; i < this.itsPACs.size(); i++) {
			OrdinaryMorphism om = this.itsPACs.get(i);
			if (om.getTarget() == g) {
				return true;
			}
		}
		
		for (int i = 0; i < this.itsACs.size(); i++) {
			OrdinaryMorphism om = this.itsACs.get(i);
			if (om.getTarget() == g) {
				return true;
			}
		}
		
		return false;
	}

	/** Returns its left graph. */
	public final Graph getLeft() {
		return this.itsOrig;
	}

	/** Returns its right graph. */
	public final Graph getRight() {
		return this.itsImag;
	}

	public boolean isNotApplicable() {
		return this.notApplicable; 
	}
	
	/**
	 * Returns the value which is set after <code>setApplicable(boolean appl)</code> called.
	 */
	public boolean isApplicable() {
		return !this.notApplicable && this.applicable;
	}
	
	/**
	 * Checks whether this rule is applicable at the specified graph by the
	 * specified matching strategy or not.
	 * 
	 * <b>Pre:</b> check <code>isReadyToTransform()</code> should be done before
	 *
	 */
	public boolean isApplicable(Graph g, MorphCompletionStrategy strategy) {
		return isApplicable(g, strategy, false);
	}

	/**
	 * Checks whether this rule is applicable at the specified graph by the
	 * specified matching strategy or not.
	 */
	public boolean isApplicable(
			final Graph g,
			final MorphCompletionStrategy strategy,
			final boolean doCheckIfReadyToTransform) {
		
		boolean result = this.enabled; //true;
		
		if (result && doCheckIfReadyToTransform) {
			result = this.isReadyToTransform();
		}

		if (result) {			
			result = false;												
			Match m = BaseFactory.theFactory().createMatch(this, g);
			if (m != null) {
				m.setCompletionStrategy(strategy, true);
				m.enableInputParameter(false);
				
//				((VarTuple) this.getAttrContext().getVariables()).showVariables();
//				((VarTuple) m.getAttrContext().getVariables()).showVariables();
				
				if (m.nextCompletion()) {
					result = true;
				} 
//				else {
//					System.out.println("Rule.isApplicable:: ERROR: "+m.getErrorMsg()+"   "+this.errorMsg);
//				}
				m.dispose();
			}
		}
		return result;
	}

	public void enableInputParameter(final boolean enable) {
		VarTuple vars = (VarTuple) this.getAttrContext().getVariables();
		for (int i=0; i<vars.getNumberOfEntries(); i++) {
			VarMember vm = vars.getVarMemberAt(i);
			if (vm.isInputParameter()) {
				vm.setEnabled(enable);
				enableAttrConditionWithInputParameter(vm.getName(), enable);
			}
		}
	}
	
	private void enableAttrConditionWithInputParameter(final String ipName, final boolean enable) {
		CondTuple conds = (CondTuple) this.getAttrContext().getConditions();
		for (int i=0; i<conds.getNumberOfEntries(); i++) {
			CondMember cond = conds.getCondMemberAt(i);
			if (cond.getAllVariables().contains(ipName)) {
				cond.setEnabled(enable);
			}
		}
	}
	
	/**
	 * Checks whether the LHS of this rule is applicable at the specified graph by the
	 * specified matching strategy.
	 */
	public boolean isLeftApplicable(
			final Graph g, 
			final MorphCompletionStrategy strategy,
			final boolean doCheckIfReadyToTransform) {
		boolean result = true;
		if (doCheckIfReadyToTransform) {
			result = this.isReadyToTransform();
		}
		if (result) {
			result = false;
			
			Hashtable<OrdinaryMorphism, Boolean> applcond2enable = new Hashtable<OrdinaryMorphism, Boolean>(
					this.itsNACs.size()+this.itsPACs.size()+this.itsACs.size());
			// store nac.isEnabled() setting and disable nac 
			for (int i = 0; i < this.itsNACs.size(); i++) {
				OrdinaryMorphism nac = this.itsNACs.get(i);
				applcond2enable.put(nac, Boolean.valueOf(nac.isEnabled()));
				nac.setEnabled(false);
			}
			// store pac.isEnabled() setting and disable nac 
			for (int i = 0; i < this.itsPACs.size(); i++) {
				OrdinaryMorphism pac = this.itsPACs.get(i);
				applcond2enable.put(pac, Boolean.valueOf(pac.isEnabled()));
				pac.setEnabled(false);
			}
			
			// store ac.isEnabled() setting and disable ac 
			for (int i = 0; i < this.itsACs.size(); i++) {
				OrdinaryMorphism ac = this.itsACs.get(i);
				applcond2enable.put(ac, Boolean.valueOf(ac.isEnabled()));
				ac.setEnabled(false);
			}
			
			Match m = BaseFactory.theFactory().createMatch(this, g);
			if (m != null) {
				m.setCompletionStrategy(strategy);
				while (m.nextCompletion()) {
					result = true;
					break;
				}
			}
			BaseFactory.theFactory().destroyMatch(m);

			// restore enable setting
			for (int i = 0; i < this.itsNACs.size(); i++) {
				this.itsNACs.get(i).setEnabled(applcond2enable.get(this.itsNACs.get(i)).booleanValue());
			}
			for (int i = 0; i < this.itsPACs.size(); i++) {
				this.itsPACs.get(i).setEnabled(applcond2enable.get(this.itsPACs.get(i)).booleanValue());
			}
			for (int i = 0; i < this.itsACs.size(); i++) {
				this.itsACs.get(i).setEnabled(applcond2enable.get(this.itsACs.get(i)).booleanValue());
			}
		}

		return result;
	}
	
	public void enableNACs(boolean enable) {
		for (int i = 0; i < this.itsNACs.size(); i++) {
			this.itsNACs.get(i).setEnabled(enable);
		}
	}

	public void enablePACs(boolean enable) {
		for (int i = 0; i < this.itsPACs.size(); i++) {
			this.itsPACs.get(i).setEnabled(enable);
		}
	}

	public void setApplicable(boolean appl) {
		this.applicable = appl;
	}

	public TypeSet getTypeSet() {
		return getLeft().getTypeSet();
	}

//	private Graph createCondGraph(final TypeSet types) {
//		return types.isArcDirected()? new Graph(types): new UndirectedGraph(types);
//	}
	
	/**
	 * Creates and adds a new (nested) application condition (GAC).
	 * Note, because a new morphism is empty and the LHS graph is not, 
	 * it is not a morphism in terms of theory, 
	 * which demands an application condition to be a total morphism.
	 * 
	 * @return an empty morphism <code>ac</code> with
	 *         <code>ac.getOriginal() == this.getOriginal()</code>.
	 */
	public NestedApplCond createNestedAC() {
		final NestedApplCond ac = new NestedApplCond(
				getLeft(), 
				BaseFactory.theFactory().createGraph(getRight().getTypeSet()), 
				getRight().getAttrContext());
		
		this.itsACs.add(ac);
		AttrContext acContext = ac.getAttrContext(); //getLeft().getAttrContext();
		ac.getImage().setAttrContext(acContext);
		ac.getImage().setKind(GraphKind.AC);
		return ac;
	}
	
	/**
	 * Creates and adds a new (nested) application condition (GAC).
	 * The target graph of the new GAC is constructed due to RHS of this rule.
	 */
	public NestedApplCond createNestedACDuetoRHS() {
		final NestedApplCond nac = createNestedAC();
		makeACDuetoRHS(nac);
		return nac;
	}
	
	/**
	 * Adds the specified morphism representing a nested application condition.
	 * <b>Pre:</b> <code>ac.getOriginal() == this.getOriginal()</code>.
	 */
	public boolean addNestedAC(final OrdinaryMorphism ac) {
		return this.addNestedAC(-1, ac);
	}

	/**
	 * Adds the specified morphism representing a nested application condition 
	 * in the list at the specified index. 
	 * <b>Pre:</b> <code>ac.getOriginal() == this.getOriginal()</code>.
	 */
	public boolean addNestedAC(int indx, final OrdinaryMorphism ac) {
		if (!this.itsACs.contains(ac)) {
			ac.getTarget().setKind(GraphKind.AC);
			if (indx >= 0 && indx < this.itsACs.size())
				this.itsACs.add(indx, ac);
			else
				this.itsACs.add(ac);
			this.changed = true;
			return true;
		} 
		return false;
	}
	
	public void enableNestedAC(boolean enable) {
		for (int i = 0; i < this.itsACs.size(); i++) {
			this.itsACs.get(i).setEnabled(enable);
		}
	}
	
	/**
	 * Destroys the specified application condition. 
	 * The image graph of the <code>ac</code>
	 * morphism would be destroyed, too.
	 */
	public void destroyNestedAC(final OrdinaryMorphism ac) {
		this.itsACs.removeElement(ac);
		ac.getImage().dispose();
	}

	/**
	 * Returns true, if it contains nested application conditions.
	 */
	public boolean hasNestedACs() {
		return !this.itsACs.isEmpty();
	}
	
	/**
	 * Return an enumeration of nested application conditions.
	 * An element is of type <code>OrdinaryMorphism</code>.
	 */
	public Enumeration<OrdinaryMorphism> getNestedACs() {
		return this.itsACs.elements();
	}

	public List<NestedApplCond> getEnabledACs() {
		List<NestedApplCond> list = new Vector<NestedApplCond>(this.itsACs.size());
		for (int i = 0; i < this.itsACs.size(); i++) {
			NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
			if (ac.isEnabled())
				list.add(ac);
		}
		return list;
	}
	
	
	public List<OrdinaryMorphism> getNestedACsList() {
		return this.itsACs;
	}
	
	public List<Evaluable> getEnabledGeneralACsAsEvaluable() {
		List<Evaluable> list = new Vector<Evaluable>(this.itsACs.size());
		for (int i = 0; i < this.itsACs.size(); i++) {
			NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
			if (ac.isEnabled())
				list.add(ac);
		}
		return list;
	}
	
	/**
	 * Returns an OrdinaryMorphism representing 
	 * a nested application condition with the specified name.
	 */
	public OrdinaryMorphism getNestedAC(String name) {
		for (int i = 0; i < this.itsACs.size(); i++) {
			OrdinaryMorphism ac = this.itsACs.get(i);
			if (ac.getName().equals(name))
				return ac;
		}
		return null;
	}

	public OrdinaryMorphism getNestedAC(int indx) {
		if (indx >= 0 && indx < this.itsACs.size())
			return this.itsACs.get(indx);
		else
			return null;
	}
	
	/**
	 * Removes the specified application condition.
	 * 
	 * @return <code>false</code> if <code>ac</code> is not found,
	 *  otherwise - <code>true</code>.
	 */
	public final boolean removeNestedAC(OrdinaryMorphism ac) {
		boolean enAC = ac.isEnabled();
		if (this.itsACs.removeElement(ac)) {
			if (enAC) {
				this.itsFormula.patchOutEvaluable((NestedApplCond) ac, true);
				this.refreshFormula(new Vector<Evaluable>(this.getEnabledACs()));
			}
			return true;
		} 		
		return false;
	}
	
	public boolean nestedACIsUsingVariable(
			final VarMember var, 
			final AttrConditionTuple act) {
		
		for (int i=0; i<this.itsACs.size(); i++) {
			final OrdinaryMorphism ac = this.itsACs.get(i);					
			if (ac.getTarget().isUsingVariable(var)) {
				return true;
			} 
			Vector<String> acVars = ac.getTarget()
						.getVariableNamesOfAttributes();
			for (int j = 0; j < acVars.size(); j++) {
				String varName = acVars.get(j);
				for (int k = 0; k < act.getNumberOfEntries(); k++) {
					CondMember cond = (CondMember) act.getMemberAt(k);
					Vector<String> condVars = cond.getAllVariables();
					if (condVars.contains(varName)
							&& condVars.contains(var.getName())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Creates a new negative application condition (NAC) and add it to its NACs.
	 * Note, because a new morphism is empty and the LHS graph is not, 
	 * it is not a morphism in terms of theory, 
	 * which demands a NAC to be a total morphism.
	 * 
	 * @return an empty morphism <code>nac</code> with
	 *         <code>nac.getOriginal() == this.getOriginal()</code>.
	 */
	public OrdinaryMorphism createNAC() {
		final OrdinaryMorphism nac = new OrdinaryMorphism(
				getLeft(), 
				BaseFactory.theFactory().createGraph(getRight().getTypeSet()), 
				getRight().getAttrContext());
		
		this.itsNACs.addElement(nac);
		AttrContext nacContext = nac.getAttrContext(); //getLeft().getAttrContext();
		nac.getImage().setAttrContext(nacContext);
		nac.getImage().setKind(GraphKind.NAC);
		return nac;
	}

	/**
	 * Creates a new negative application condition (NAC) and add it to its NACs.
	 * The target graph of the new NAC is constructed due to RHS of this rule.
	 */
	public OrdinaryMorphism createNACDuetoRHS() {
		final OrdinaryMorphism nac = createNAC();
		makeACDuetoRHS(nac);
		return nac;
	}
	
	public void makeACDuetoRHS(final OrdinaryMorphism morph) {	
		HashMap<Node,Node> map = new HashMap<Node,Node>();
		Iterator<Node> nodes = this.itsImag.getNodesSet().iterator();
		while (nodes.hasNext()) {
			Node nr = nodes.next();
			Enumeration<GraphObject> l = this.getInverseImage(nr);
			if (l.hasMoreElements()) {	
				Node nl = (Node)l.nextElement(); 			
				try {
					Node n = morph.getTarget().copyNode(nl);
					try {						
						morph.addMapping(nl, n);
						while (l.hasMoreElements()) {
							morph.addMapping((Node)l.nextElement(), n);
						}
						map.put(nr, n);
					} catch (BadMappingException ex) {}
				} catch (TypeException e) {}
			}
			else {
				try {
					Node n = morph.getTarget().copyNode(nr);
					if (n.getAttribute() != null)
						((agg.attribute.impl.ValueTuple)n.getAttribute()).unsetValueAsExpr();
					map.put(nr, n);
				} catch (TypeException e) {}
			}
		}
		Iterator<Arc> arcs = this.itsImag.getArcsSet().iterator();
		while (arcs.hasNext()) {
			Arc ar = arcs.next();
			Enumeration<GraphObject> l = this.getInverseImage(ar);
			if (l.hasMoreElements()) {	
				Arc al = (Arc) l.nextElement();
				try {
					Arc a = morph.getTarget().copyArc(al, (Node)morph.getImage(al.getSource()), (Node)morph.getImage(al.getTarget()));
					try {						
						morph.addMapping(al, a);
						while (l.hasMoreElements()) {													
							morph.addMapping(l.nextElement(), a);							
						}
					} catch (BadMappingException ex) {}
				} catch (TypeException e) {}
			}
			else {
				try {
					Node s = (Node)map.get(ar.getSource());
					Node t = (Node)map.get(ar.getTarget());
					Arc a = morph.getTarget().copyArc(ar, s, t);
					if (a.getAttribute() != null)
						((agg.attribute.impl.ValueTuple)a.getAttribute()).unsetValueAsExpr();
				} catch (TypeException e) {}
			}
		}
		map.clear(); map = null;
	}
	
	/**
	 * Adds the specified morphism representing a negative application condition (NAC).
	 * <p>
	 * <b>Pre:</b> <code>nac.getOriginal() == this.getOriginal()</code>.
	 */
	public boolean addNAC(final OrdinaryMorphism nac) {
		return this.addNAC(-1, nac);
	}

	/**
	 * Adds the specified morphism representing a negative application condition (NAC)
	 * to the list at the specified index.
	 * <p>
	 * <b>Pre:</b> <code>nac.getOriginal() == this.getOriginal()</code>.
	 */
	public boolean addNAC(int indx, final OrdinaryMorphism nac) {
		if (!this.itsNACs.contains(nac)) {
			nac.getTarget().setKind(GraphKind.NAC);
			if (indx >= 0 && indx < this.itsNACs.size())
				this.itsNACs.add(indx, nac);
			else
				this.itsNACs.add(nac);
			this.changed = true;
			return true;
		} 
			
		return false;
	}
	
	/**
	 * Destroys the specified NAC from my NACs. 
	 * The image graph of the nac morphism would be destroyed, too.
	 */
	public void destroyNAC(OrdinaryMorphism nac) {
		this.itsNACs.removeElement(nac);
		nac.getImage().dispose();
	}

	/**
	 * Returns true if it contains a NAC.
	 */
	public boolean hasNACs() {
		return !this.itsNACs.isEmpty();
	}
	
	/**
	 * Returns true, if there is at least one enabled NAC.
	 */
	public boolean hasEnabledNACs() {
		for (OrdinaryMorphism n: this.itsNACs) {
			if (n.isEnabled())
				return true;
		}
		return false;
	}
	
	/**
	 * Returns an enumeration of my NACs.
	 */
	public Enumeration<OrdinaryMorphism> getNACs() {
		return this.itsNACs.elements();
	}

	/**
	 * @deprecated replaced by <code>getNACsList()</code>
	 */
	public Vector<OrdinaryMorphism> getNACsVector() {
		return this.itsNACs;
	}

	public List<OrdinaryMorphism> getNACsList() {
		return this.itsNACs;
	}
	
	/**
	 * Returns an OrdinaryMorphism representing a NAC with the specified name.
	 */
	public OrdinaryMorphism getNAC(String name) {
		for (int i = 0; i < this.itsNACs.size(); i++) {
			OrdinaryMorphism nac = this.itsNACs.get(i);
			if (nac.getName().equals(name))
				return nac;
		}
		return null;
	}

	/**
	 * Returns an OrdinaryMorphism representing a NAC at the specified index.
	 */
	public OrdinaryMorphism getNAC(int indx) {
		if (indx >= 0 && indx < this.itsNACs.size())
			return this.itsNACs.get(indx);
		else
			return null;
	}
	
	/**
	 * Returns an OrdinaryMorphism representing a NAC with the target as specified graph.
	 */
	public OrdinaryMorphism getNAC(final Graph g) {
		for (int i = 0; i < this.itsNACs.size(); i++) {
			OrdinaryMorphism ac = this.itsNACs.get(i);
			if (ac.getTarget() == g)
				return ac;
		}
		return null;
	}
	
	/**
	 * Returns true if the specified Graph g is the target graph 
	 * of an OrdinaryMorphism representing a NAC.
	 */
	public boolean hasNAC(final Graph g) {
		for (int i = 0; i < this.itsNACs.size(); i++) {
			OrdinaryMorphism ac = this.itsNACs.get(i);
			if (ac.getTarget() == g)
				return true;
		}
		return false;
	}
	
	/**
	 * Removes a negative application condition.
	 * 
	 * @return <code>false</code> if <code>nac</code> is not found,
	 * otherwise - <code>true</code>.
	 */
	public final boolean removeNAC(OrdinaryMorphism nac) {
		if (this.itsNACs.removeElement(nac)) {
			return true;
		} 
		return false;
	}
	
	 /**
	 * Creates a new positive application condition (PAC) and add it to its PACs.
	 * Note, because a new morphism is empty and the LHS is not, 
	 * it is not a morphism in terms of theory, 
	 * which demands a PAC to be a total morphism.
     *
	 * @return an empty morphism <code>pac</code> with
	 *         <code>pac.getOriginal() == this.getOriginal()</code>.
	 */
	public OrdinaryMorphism createPAC() {
		final OrdinaryMorphism pac = new OrdinaryMorphism(
				getLeft(), 
				BaseFactory.theFactory().createGraph(getRight().getTypeSet()), 
				getRight().getAttrContext());
		
		this.itsPACs.addElement(pac);
		AttrContext pacContext = pac.getAttrContext(); //getLeft().getAttrContext();
		pac.getImage().setAttrContext(pacContext);
		pac.getImage().setKind(GraphKind.PAC);
		return pac;
	}

	/**
	 * Adds the specified morphism representing a positive application condition (PAC).
	 * <b>Pre:</b> <code>pac.getOriginal() == this.getOriginal()</code>.
	 */
	public boolean addPAC(final OrdinaryMorphism pac) {
		return this.addPAC(-1, pac);
	}

	/**
	 * Adds the specified morphism representing a positive application condition (PAC) 
	 * in the list at the specified index. 
	 * <b>Pre:</b> <code>pac.getOriginal() == this.getOriginal()</code>.
	 */
	public boolean addPAC(int indx, final OrdinaryMorphism pac) {
		if (!this.itsPACs.contains(pac)) {
			pac.getTarget().setKind(GraphKind.PAC);
			if (indx >= 0 && indx < this.itsPACs.size())
				this.itsPACs.add(indx, pac);
			else
				this.itsPACs.add(pac);
			this.changed = true;
			return true;
		} 
		return false;
	}
	
	public void addShiftedPAC(final List<OrdinaryMorphism> list) {
		final ShiftedPAC shiftedPAC = new ShiftedPAC(list);
		if (this.itsShiftedPACs == null)
			itsShiftedPACs = new Vector<ShiftedPAC>();
		this.itsShiftedPACs.add(shiftedPAC);		
	}
	
	public List<ShiftedPAC> getShiftedPACs() {
		return this.itsShiftedPACs;
	}
	
	public boolean isShiftedPAC(final OrdinaryMorphism pac) {
		if (this.itsShiftedPACs == null)
			return false;
		for (int i=0; i<this.itsShiftedPACs.size(); i++) {
			if (this.itsShiftedPACs.get(i).contains(pac))
				return true;
		}
		return false;
	}
	
	/**
	 * Destroys the specified pac from my set of PACs. 
	 * The image graph of the pac
	 * morphism would be destroyed, too.
	 */
	public void destroyPAC(final OrdinaryMorphism pac) {
		this.itsPACs.removeElement(pac);
		pac.getImage().dispose();
	}

	/**
	 * Returns true, if there is at least one PAC.
	 */
	public boolean hasPACs() {
		return !this.itsPACs.isEmpty();
	}
	
	/**
	 * Returns true, if there is at least one enabled PAC.
	 */
	public boolean hasEnabledPACs() {
		for (OrdinaryMorphism p: this.itsPACs) {
			if (p.isEnabled())
				return true;
		}
		return false;
	}
	
	/**
	 * Return an enumeration of my PACs with elements of type
	 * <code>OrdinaryMorphism</code>.
	 */
	public Enumeration<OrdinaryMorphism> getPACs() {
		return this.itsPACs.elements();
	}

	/**
	 * Return an enumeration of my enabled PACs with elements of type
	 * <code>OrdinaryMorphism</code>.
	 */
	public Enumeration<OrdinaryMorphism> getEnabledPACs() {
		Vector<OrdinaryMorphism> v = new Vector<OrdinaryMorphism>(2);
		for (OrdinaryMorphism p: this.itsPACs) {
			if (p.isEnabled())
				v.add(p);
		}
		return v.elements();
	}
	
	/**
	 * @deprecated replaced by <code>getPACsList()</code>
	 * 
	 * Return the Vector of my PACs with elements of type
	 * <code>OrdinaryMorphism</code>.
	 */
	public Vector<OrdinaryMorphism> getPACsVector() {
		return this.itsPACs;
	}

	public List<OrdinaryMorphism> getPACsList() {
		return this.itsPACs;
	}
	
	/**
	 * Returns an OrdinaryMorphism representing a PAC with the specified name.
	 */
	public OrdinaryMorphism getPAC(String name) {
		for (int i = 0; i < this.itsPACs.size(); i++) {
			OrdinaryMorphism pac = this.itsPACs.get(i);
			if (pac.getName().equals(name))
				return pac;
		}
		return null;
	}

	/**
	 * Returns an OrdinaryMorphism representing a PAC at the specified index.
	 */
	public OrdinaryMorphism getPAC(int indx) {
		if (indx >= 0 && indx < this.itsPACs.size())
			return this.itsPACs.get(indx);
		else
			return null;
	}
	
	/**
	 * Returns an OrdinaryMorphism representing a PAC with target as specified Graph.
	 */
	public OrdinaryMorphism getPAC(final Graph g) {
		for (int i = 0; i < this.itsPACs.size(); i++) {
			OrdinaryMorphism ac = this.itsPACs.get(i);
			if (ac.getTarget() == g)
				return ac;
		}
		return null;
	}
	
	/**
	 * Returns true if the specified Graph g is the target graph 
	 * of an OrdinaryMorphism representing a PAC.
	 */
	public boolean hasPAC(final Graph g) {
		for (int i = 0; i < this.itsPACs.size(); i++) {
			OrdinaryMorphism ac = this.itsPACs.get(i);
			if (ac.getTarget() == g)
				return true;
		}
		return false;
	}
	
	/**
	 * Removes the specified positive application condition.
	 * 
	 * @return <code>false</code> if <code>pac</code> is not found,
	 *  otherwise - <code>true</code>.
	 */
	public final boolean removePAC(OrdinaryMorphism pac) {
		if (this.itsPACs.removeElement(pac)) {
			return true;
		} 
		
		return false;
	}

	// /////////////////////////////////////

	/**
	 * Returns FALSE if the specified nodeType is an abstract type and 
	 * used in the RHS to create a node,
	 * otherwise - TRUE.
	 */
	public boolean checkCreateAbstractNode(Type nodeType) {
		Iterator<Node> en = getTarget().getNodesSet().iterator();
		while (en.hasNext()) {
			Node n = en.next();
			if (n.getType().equals(nodeType)) {
				if (!this.getInverseImage(n).hasMoreElements())
					return false;
			}
		}
		return true;
	}

	public TypeError checkNewNodeRequiresArc() {			
		final Iterator<Node> elems = this.getRight().getNodesSet().iterator();
		while (elems.hasNext()) {
			final GraphObject obj = elems.next();
			if (!this.getInverseImage(obj).hasMoreElements()) {
				List<String> list = this.getRight().getTypeSet().nodeRequiresArc((Node) obj);			
				if (list != null && !list.isEmpty()) {
					TypeError actError = new TypeError(TypeError.TO_LESS_ARCS,
							"Node type  " 
							+ "\""+obj.getType().getName()+ "\" \n"
							+ "requires edge(s) of type: \n" 
							+ list.toString(), obj.getType());
					actError.setContainingGraph(this.getRight());
					return actError;
				}
			}
		}
		return null;
	}
	
	/**
	 * Try to destroy all graph objects of the specified type from its graphs
	 * (LHS, RHS, NACs, PACs, graph constraints).
	 */
	public boolean destroyObjectsOfType(Type t) {
		if (getLeft().destroyObjectsOfType(t)) {
			if (getRight().destroyObjectsOfType(t)) {
				for (int j = 0; j < this.itsNACs.size(); j++) {
					OrdinaryMorphism nac = this.itsNACs.get(j);
					if (!nac.getTarget().destroyObjectsOfType(t))
						return false;
				}
				for (int j = 0; j < this.itsPACs.size(); j++) {
					OrdinaryMorphism pac = this.itsPACs.get(j);
					if (!pac.getTarget().destroyObjectsOfType(t))
						return false;
				}
				for (int j = 0; j < this.itsACs.size(); j++) {
					OrdinaryMorphism ac = this.itsACs.get(j);
					if (!ac.getTarget().destroyObjectsOfType(t))
						return false;
				}
				
				// delete from rule application conditions
				Vector<EvalSet> atom_conds = getAtomApplConds();
				for (int i = 0; i < atom_conds.size(); i++) {
					Vector<?> v = atom_conds.get(i).getSet();
					for (int j = 0; j < v.size(); j++) {
						Vector<?> v1 = ((EvalSet) v.get(j)).getSet();
						for (int k = 0; k < v1.size(); k++) {
							agg.cons.AtomApplCond aac = (agg.cons.AtomApplCond) v1
									.get(k);
							OrdinaryMorphism cond = aac.getPreCondition();
							OrdinaryMorphism tm = aac.getT();
							OrdinaryMorphism qm = aac.getQ();
							cond.getSource().destroyObjectsOfType(t);
							cond.getTarget().destroyObjectsOfType(t);
							tm.getTarget().destroyObjectsOfType(t);
							qm.getSource().destroyObjectsOfType(t);
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Try to destroy all graph objects of the specified types from its graphs
	 * (LHS, RHS, NACs, PACs, graph constraints).
	 * Returns names of the failed types.
	 */
	public Vector<String> destroyObjectsOfTypes(Vector<Type> types) {
		Vector<String> failed = new Vector<String>(5);
		for (int i = 0; i < types.size(); i++) {
			Type t = types.get(i);
			if (!destroyObjectsOfType(t)) {
				String s = "Rule:  " + getName() + "   Type:  " + t.getName();
				failed.add(s);
			}
		}
		return failed;
	}

	/**
	 * Returns a copy of this rule by using its types.
	 */
	public Rule getClone() {
		return BaseFactory.theFactory().cloneRule(this);
	}

	/**
	 * Returns a copy of this rule by using the specified types.
	 */
	public Rule getClone(TypeSet types) {
		return BaseFactory.theFactory().cloneRule(this, types, true);
	}
	
	/**
	 * Return its morphism between the left and right graphs.
	 */
	public final OrdinaryMorphism getMorphism() {
		return this;
	}

	/**
	 * Returns its graph constraints 
	 * which can be converted to the post application constraints.
	 */
	public Vector<Formula> getConstraints() {
		return (this.constraints != null) ? this.constraints : new Vector<Formula>(0);
	}
	
	/**
	 * Checks the type compatibility of two graph objects.
	 * The first object should belong to the LHS, the second - to the RHS,
	 * to be used for a mapping of the rule morphism.
	 */
	protected boolean checkType(Type orig, Type image) {
		return orig.compareTo(image);
	}

	public void createAttrInstanceWhereNeeded() {
		this.itsOrig.createAttrInstanceWhereNeeded();
		this.itsImag.createAttrInstanceWhereNeeded();
		
		for (int i=0; i<this.itsNACs.size(); i++) {
			this.itsNACs.get(i).getTarget().createAttrInstanceWhereNeeded();
		}
		for (int i=0; i<this.itsPACs.size(); i++) {
			this.itsPACs.get(i).getTarget().createAttrInstanceWhereNeeded();
		}
		for (int i=0; i<this.itsACs.size(); i++) {
			this.itsACs.get(i).getTarget().createAttrInstanceWhereNeeded();
		}
	}
	
	public void createAttrInstanceOfTypeWhereNeeded(final Type t) {
		this.itsOrig.createAttrInstanceOfTypeWhereNeeded(t);
		this.itsImag.createAttrInstanceOfTypeWhereNeeded(t);
		
		for (int i=0; i<this.itsNACs.size(); i++) {
			this.itsNACs.get(i).getTarget().createAttrInstanceOfTypeWhereNeeded(t);
		}
		for (int i=0; i<this.itsPACs.size(); i++) {
			this.itsPACs.get(i).getTarget().createAttrInstanceOfTypeWhereNeeded(t);
		}
		for (int i=0; i<this.itsACs.size(); i++) {
			this.itsACs.get(i).getTarget().createAttrInstanceOfTypeWhereNeeded(t);
		}
	}
	
	/**
	 * Generates rule post application conditions from its constraints(formulas).
	 * Returns error message if something gone wrong, otherwise - empty.
	 */
	public String convertUsedFormulas() {
		if (this.itsUsedAtomics != null && this.itsUsedAtomics.size() > 0 
				&& this.itsUsedFormulas != null && this.itsUsedFormulas.size() > 0) {
			String msg = "";
			Vector<EvalSet> fin = new Vector<EvalSet>();
			Vector<String> names = new Vector<String>();
	
			// clear Post Appl. Conditions
			if (this.constraints == null)
				constraints = new Vector<Formula>();
			else
				this.constraints.clear();
			setAtomApplConds(null, null);
		
			final Hashtable<AtomConstraint,EvalSet> atomic2set = new Hashtable<AtomConstraint,EvalSet>();
			final Hashtable<String,String> failedAtomic2error = new Hashtable<String,String>();
			
			int tgLevel = this.getTypeSet().getLevelOfTypeGraphCheck();
			if (tgLevel > TypeSet.ENABLED_MAX)
				this.getTypeSet().setLevelOfTypeGraph(TypeSet.ENABLED_MAX);
			
			for (int j = 0; j < this.itsUsedAtomics.size(); j++) {
				AtomConstraint a = this.itsUsedAtomics.elementAt(j);
				if (!a.isValid()) {
					msg = "Atomic  \"" + a.getAtomicName() + "\"  is not valid. <br>";
					this.itsUsedAtomics.clear();
					this.itsUsedFormulas.clear();
					return msg;
				}
				
				((AttrTupleManager) AttrTupleManager.getDefaultManager())
						.setVariableContext(true);
				
				Convert conv = new Convert(this, a);
				Vector<Object> v = conv.convert();
				
				((AttrTupleManager) AttrTupleManager.getDefaultManager())
				.setVariableContext(false);
				
				final EvalSet set = new EvalSet(v);
				fin.add(set);
				names.add(a.getAtomicName());
				
				if (!v.isEmpty()) {
					atomic2set.put(a, set);
				} 
				
				if (!"".equals(conv.getErrorMsg())) {
					failedAtomic2error.put(a.getAtomicName(), conv.getErrorMsg());
				}
			}
			this.getTypeSet().setLevelOfTypeGraph(tgLevel);
	
			if (!failedAtomic2error.isEmpty()) {			
				msg = "Error(s) during creating Post Application Condition : <br>";			
			}
			
			for (int j = 0; j < this.itsUsedFormulas.size(); j++) {
				Formula f = this.itsUsedFormulas.elementAt(j);
				if (!f.isEnabled()) {
					continue;
				}
				Vector<Evaluable> v = new Vector<Evaluable>();
				String s = f.getAsString(v);
	//			System.out.println(s);
	//			System.out.println(v);
				// In v the atomics used in f are noted.
				// In fin the set of _all_new atomics are noted
				// (though they are real formulas now) in the original order.
				// This means, we need a translation.
				// I.e. we build a new vector as the source of a new formula
				// only containing the base formulas
				// corresponding to the atomic at that index.
				boolean formulaOK = true;
				Vector<Evaluable> v2 = new Vector<Evaluable>();
				for (int k = 0; k < v.size(); k++) {
					Object e = v.get(k);
					boolean convertOK = false;
					int k2;
					for (k2 = 0; k2 < this.itsUsedAtomics.size(); k2++) {
		
						if (this.itsUsedAtomics.get(k2) == e) {
							final String atomicName = this.itsUsedAtomics.get(k2).getAtomicName();
	//						System.out.println(atomicName));
							Evaluable set = atomic2set.get(e);
							if (set != null) {
								v2.add(set);
								convertOK = true;
								break;
							} 
																
							int indx = names.indexOf(atomicName);
							fin.remove(indx);
							names.remove(indx);
						}
					}
					if (!convertOK) {
						formulaOK = false;
						break;
					}
				}
				if (formulaOK) {
					Formula f2 = new Formula(v2, s);
					this.constraints.add(f2);
				} 
			}
				
			if (fin.isEmpty()) {
				this.itsUsedAtomics.clear();
				this.itsUsedFormulas.clear();				
			} else {
				this.setAtomApplConds(fin, names);
			}
			
			deleteTransientContextVariables(getSource());
			deleteTransientContextVariables(getTarget());
			this.removeUnusedVariableOfAttrContext();
			
			String msg1 = "Cannot convert atomic(s) :\n";
			String msg2 = "";
			final Enumeration<String> failedAtomic = failedAtomic2error.keys();
			while (failedAtomic.hasMoreElements()) {
				String name = failedAtomic.nextElement();
				String error = failedAtomic2error.get(name);
				msg2 = msg2.concat(" - ").concat(name).concat(" - ").concat("\n");
				msg2 = msg2.concat(error).concat("\n");
			}
			if (!"".equals(msg2)) {
				msg1 = msg1.concat(msg2);
				msg = msg.concat(msg1);
			}			
			return msg;
		}
		else {
			return "Cannot create post application conditions. There isn't any formula selected. <br>";
		}
	}

	/**
	 * Set a vector of atomic graph constraints used for generating post
	 * application conditions. Elements of the usedAtomic are of the type
	 * agg.cons.AtomConstraint .
	 * 
	 * private void setUsedAtomics(Vector usedAtomics) { itsUsedAtomics =
	 * usedAtomics; }
	 */

	/**
	 * Set its constraints (formulas) which will be used for generating its post
	 * application conditions. 
	 */
	public void setUsedFormulas(List<Formula> formulasToUse) {
		if (!formulasToUse.isEmpty()) {
			if (this.itsUsedFormulas == null)
				itsUsedFormulas = new Vector<Formula>();
			else
				this.itsUsedFormulas.clear();
			if (this.itsUsedAtomics == null)
				itsUsedAtomics = new Vector<AtomConstraint>(); 
			else
				this.itsUsedAtomics.clear();
			
			this.itsUsedFormulas.addAll(formulasToUse);
			for (int i = 0; i < this.itsUsedFormulas.size(); i++) {
				Formula f = this.itsUsedFormulas.get(i);
				Vector<Evaluable> vec = new Vector<Evaluable>();
				String form = f.getAsString(vec);
				for (int j = 0; j < vec.size(); j++) {
					if (vec.get(j) instanceof AtomConstraint) {
						AtomConstraint ac = (AtomConstraint) vec.get(j);
						this.itsUsedAtomics.addElement(ac);
					} else {
						System.out
								.println("Rule.setUsedFormulas(Vector<Formula> usedFormulas):  formula: "
										+ form + "   FAILED!");
					}
				}
			}
		}
	}

	/**
	 * Return a vector of atomic graph constraints used for generating post
	 * application conditions. Elements of the usedAtomic are of the type
	 * agg.cons.AtomConstraint .
	 */
	public Vector<AtomConstraint> getUsedAtomics() {
		return (this.itsUsedAtomics != null) ? this.itsUsedAtomics : new Vector<AtomConstraint>(0);
	}

	/**
	 * Return the vector of constraints (formulas) used for generating post
	 * application conditions. Elements of the usedFormulas are of the type
	 * agg.cons.Formula .
	 */
	public Vector<Formula> getUsedFormulas() {
		return (this.itsUsedFormulas != null) ? this.itsUsedFormulas : new Vector<Formula>(0);
	}

	/**
	 * Clears its lists of graph constraints only if the specified
	 * atomic graph constraint belongs to its constraints. 
	 */
	public void clearConstraints(AtomConstraint ac) {
		if (this.itsUsedAtomics != null && this.itsUsedAtomics.contains(ac)) {
			this.clearConstraints();
		}
	}
	
	/**
	 * Clears its lists of graph constraints only if the specified
	 * graph constraint belongs to its constraints. 
	 */
	public void clearConstraints(Formula f) {
		if (this.itsUsedFormulas != null && this.itsUsedFormulas.contains(f)) {
			this.clearConstraints();
		}
	}
	
	/**
	 * Clears lists of its graph constraints.
	 */
	public void clearConstraints() {
		if (this.itsUsedAtomics != null)
			this.itsUsedAtomics.clear();
		if (this.itsUsedFormulas != null)
			this.itsUsedFormulas.clear();
		if (this.constraints != null)
			this.constraints.clear();
		setAtomApplConds(null, null);
	}

	/**
	 * Set the specified post application conditions to its conditions.
	 */
	public void setAtomApplConds(Vector<EvalSet> v, Vector<String> names) {
		if (this.atom_conditions == null)
			atom_conditions = new Vector<EvalSet>();
		else
			this.atom_conditions.clear();
		if (this.constraintNameSet == null)
			constraintNameSet = new Vector<String>(); 
		else
			this.constraintNameSet.clear();
		
		if (v != null)
			this.atom_conditions.addAll(v);
		if (names != null)
			this.constraintNameSet.addAll(names);
		
		if (this.constraintNameSet.size() < this.atom_conditions.size()) {
			for (int i = this.constraintNameSet.size(); i < this.atom_conditions.size(); i++)
				this.constraintNameSet.add("Unknown Name " + i);
		}
	}

	public Vector<EvalSet> getAtomApplConds() {
		return (this.atom_conditions != null) ? this.atom_conditions : new Vector<EvalSet>(0);
	}

	public Vector<String> getConstraintNames() {
		return (this.constraintNameSet != null) ? this.constraintNameSet : new Vector<String>(0);
	}

	/**
	 * Removes the specified application condition from its 
	 * post application conditions.
	 */
	public void removeConstraint(EvalSet constraint) {
		if (this.atom_conditions != null && this.atom_conditions.contains(constraint)) {
			int i = this.atom_conditions.indexOf(constraint);
			this.atom_conditions.removeElement(constraint);
			this.constraintNameSet.removeElementAt(i);
		}
	}

	/**
	 * Removes the specified atomic application condition from its 
	 * post application conditions.
	 */
	public void removeAtomApplCond(AtomApplCond atom) {
		if (this.atom_conditions != null) {
			int i = 0;
			while (i < this.atom_conditions.size()) {
				Vector<?> v = this.atom_conditions.get(i).getSet();
				int j = 0;
				while (j < v.size()) {
					Vector<?> v1 = ((EvalSet) v.get(j)).getSet();
					int k = 0;
					while (k < v1.size()) {
						AtomApplCond aac = (AtomApplCond) v1.get(k);
						if (atom.equals(aac)) {
							v1.removeElement(atom);
							// System.out.println("AtomApplCond: DONE");
							k = v1.size();
						} else
							k++;
					}
					if (v1.size() == 0) {
						v.removeElementAt(j);
						j = v.size();
					} else
						j++;
				}
				if (v.size() == 0) {
					this.atom_conditions.removeElementAt(i);
					this.constraintNameSet.removeElementAt(i);
					i = this.atom_conditions.size();
				} else
					i++;
			}
		}
	}

	/**
	 * Clears its application constraints.
	 */
	public void removeApplConditions() {
		clearConstraints();
	}

	// /**
	// * Enable or disable this rule to make it usable during graph
	// transformation.
	// */
	// public void setEnabled(boolean enable) {
	// enabled = enable;
	// // System.out.println("Rule.setEnabled "+enabled);
	// }
	//
	// /**
	// * Returns TRUE if this rule is usable during graph transformation
	// otherwise FALSE.
	// */
	// public boolean isEnabled() {
	// return enabled;
	// }

	/**
	 * Set this rule to be a trigger rule of its layer. That means: This rule
	 * will be the first rule to apply on its layer. It can be applyed one time
	 * only. All other rules on this layer can be applyed so long as possible.
	 * 
	 * @param trigger
	 */
	public void setTriggerForLayer(boolean trigger) {
		this.triggerOfLayer = trigger;
	}

	/** Checks if this rule is a trigger rule of its layer. */
	public boolean isTriggerOfLayer() {
		return this.triggerOfLayer;
	}

	/**
	 * Returns its layer. The layer will be used by layered grammar.
	 */
	public int getLayer() {
		return this.layer;
	}

	/**
	 * Sets its layer. The layer is used by layered grammar.
	 */
	public void setLayer(int l) {
		this.layer = l;
	}

	/**
	 * Returns my priority. The priority cann be used during graph
	 * transformation.
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * Sets my priority. The priority cann be used during graph transformation.
	 */
	public void setPriority(int p) {
		this.priority = p;
	}

	/**
     * Trims the capacity of used vectors to be the vector's current
     * size.
     */
	public void trimToSize() {
		super.trimToSize();
		
		this.itsNACs.trimToSize();
		for (int i = 0; i < this.itsNACs.size(); i++) {
			this.itsNACs.elementAt(i).trimToSize();
		}
		this.itsPACs.trimToSize();
		for (int i = 0; i < this.itsPACs.size(); i++) {
			this.itsPACs.elementAt(i).trimToSize();
		}
		this.itsACs.trimToSize();
		for (int i = 0; i < this.itsACs.size(); i++) {
			this.itsACs.elementAt(i).trimToSize();
		}
		
		if (this.itsUsedAtomics != null) {
			this.itsUsedAtomics.trimToSize();
			for (int i = 0; i < this.itsUsedAtomics.size(); i++) {
				this.itsUsedAtomics.get(i).trimToSize();
			}
		}	
		if (this.itsUsedFormulas != null) {
			this.itsUsedFormulas.trimToSize();
			for (int i = 0; i < this.itsUsedFormulas.size(); i++) {
				this.itsUsedFormulas.trimToSize();
			}
		}
		if (this.constraints != null) {
			this.constraints.trimToSize();
			for (int i = 0; i < this.constraints.size(); i++) {
				this.constraints.trimToSize();		
			}
		}
		if (this.atom_conditions != null)
			this.atom_conditions.trimToSize();
		if (this.constraintNameSet != null)
			this.constraintNameSet.trimToSize();
		
		if (this.itsShiftedPACs != null)
			this.itsShiftedPACs.trimToSize();
	}
	
	public void refreshAttributed() {
		getLeft().refreshAttributed();
		getRight().refreshAttributed();
		for (int i = 0; i < this.itsNACs.size(); i++) {
			this.itsNACs.elementAt(i).getTarget().refreshAttributed();
		}
		for (int i = 0; i < this.itsPACs.size(); i++) {
			this.itsPACs.elementAt(i).getTarget().refreshAttributed();
		}
		for (int i = 0; i < this.itsACs.size(); i++) {
			this.itsACs.elementAt(i).getTarget().refreshAttributed();
		}
	}

	/** Returns true if this rule, its NACs or PACs are using the specified type
	 * which is an element (node or edge) of a type graph.  
	 */
	public boolean isUsingType(GraphObject typeObj) {
		if (getLeft().isUsingType(typeObj))
			return true;
		if (getRight().isUsingType(typeObj))
			return true;
		for (int i = 0; i < this.itsNACs.size(); i++) {
			if (this.itsNACs.elementAt(i).getTarget().isUsingType(typeObj))
				return true;
		}
		for (int i = 0; i < this.itsPACs.size(); i++) {
			if (this.itsPACs.elementAt(i).getTarget().isUsingType(typeObj))
				return true;
		}
		for (int i = 0; i < this.itsACs.size(); i++) {
			this.itsACs.elementAt(i).getTarget().refreshAttributed();
		}
		return false;
	}

	public void removeUnusedVariableOfAttrContext() {
		DeclTuple vars = ((VarTuple) this.getAttrContext().getVariables()).getTupleType();		
		for (int i=0; i<vars.getNumberOfEntries(); i++) {
			DeclMember vm = (DeclMember) vars.getMemberAt(i);
			String var = vm.getName();
			if (!this.getSource().getVariableNamesOfAttributes().contains(var)) {
				if (!this.getRight().getVariableNamesOfAttributes().contains(var)) {					
					if (!isUsedInTargetGraph(this.getNACs(), var)) {
						if (!isUsedInTargetGraph(this.getPACs(), var)) {
							if (!isUsedInNestedGraphs(this.getNestedACs(), var)) {
								vars.getTupleType().deleteMemberAt(var);						
	//							System.out.println("Rule.removeVariableOfAttrContext::  removed: "+var);
								i--;
							}
						}
					} 
				}
			}
		}
	}
	
	private boolean isUsedInTargetGraph(Enumeration<OrdinaryMorphism> list, String varName) {
		while (list.hasMoreElements()) { 
			Graph g = list.nextElement().getTarget();
			if (g.getVariableNamesOfAttributes().contains(varName)) {
				return true;
			}
		}
		return false;
	}

	private boolean isUsedInNestedGraphs(Enumeration<OrdinaryMorphism> list, String varName) {
		while (list.hasMoreElements()) { 
			OrdinaryMorphism m = list.nextElement();
			if (m.getTarget().getVariableNamesOfAttributes().contains(varName)) {
				return true;
			}
			if (m instanceof NestedApplCond) {
				Vector<OrdinaryMorphism> nl = new Vector<OrdinaryMorphism>();
				for (int i=0; i<((NestedApplCond)m).getNestedACs().size(); i++) {
					nl.add(((NestedApplCond)m).getNestedACs().get(i));
				}
				if (isUsedInNestedGraphs(nl.elements(), varName))
					return true;			
			}
		}
		return false;
	}
	
	public void setWaitBeforeApplyEnabled(boolean b) {
		this.waitBeforeApply = b;
	}
	
	public boolean isWaitBeforeApplyEnabled() {
		return this.waitBeforeApply;
	}
	
	/*
	 * Create and return a new subrule. It is automatically added to my set of
	 * subrules. The new subrule, the subrule's set of NACs, and its left and
	 * right-hand side graphs which are newly created are initially empty.
	 * <p>
	 * Note that the newly created left and right side subgraphs are not
	 * automatically removed when the subrule itself is destroyed.
	 * 
	 * @see agg.xt_basis.Rule#getSubRules
	 */
//	public final SubRule createSubRule() {
//		SubRule sr = new SubRule(this, getLeft().createSubGraph(), getRight()
//				.createSubGraph());
//		if (itsSubRules == null)
//			itsSubRules = new Vector<SubRule>(5, 1);
//		itsSubRules.addElement(sr);
//		return sr;
//	}

	/**
	 * Create and return a new subrule. It is automatically added to my set of
	 * subrules. The new subrule's left and right-hand side graphs are set to be
	 * <code>left</code> and <code>right</code>, respectively. The subrule
	 * is filled so that it maps all the objects of <code>left</code> in the
	 * same way I do. The set of NACs of the new subrule is initially empty.
	 * <p>
	 * <b>Pre:</b>
	 * <ol>
	 * <li> <code>left</code> is subgraph of <code>this.getLeft()</code>.
	 * <li> <code>right</code> is subgraph of <code>this.getRight()</code>.
	 * </ol>
	 * 
	 * @see agg.xt_basis.Rule#getSubRules
	 * @see agg.xt_basis.Rule#getLeft
	 * @see agg.xt_basis.Rule#getRight
	 */
//	public final SubRule createSubRule(SubGraph left, SubGraph right) {
//		SubRule sr = new SubRule(this, left, right);
//		if (itsSubRules == null)
//			itsSubRules = new Vector<SubRule>(5, 1);
//		itsSubRules.addElement(sr);
//		return sr;
//
//	}
//
//	public final SubRule createSubRule(SubGraGra sgg) {
//		SubRule sr = new SubRule(this, new SubGraph(this.getLeft()),
//				new SubGraph(this.getRight()));
//		itsSubRules.addElement(sr);
//		if (sgg != null)
//			sgg.addRule(sr);
//		return sr;
//	}

	/**
	 * Remove a subrule from my set of subrules.
	 * 
	 * @return <code>false</code> iff <code>sr</code> was not an element of
	 *         my set of subrules.
	 */
//	public final boolean destroySubRule(SubRule sr) {
//		if (sr != null) {
//			if (itsSubRules.remove(sr)) {
//				sr.dispose();
//				return true;
//			}
//		}
//		return false;
//	}

	/**
	 * Return an Enumeration of all of my subrules (not including myself).
	 * Enumeration elements are of type <code>SubRule</code>.
	 */
//	public final Enumeration<SubRule> getSubRules() {
//		return itsSubRules.elements();
//	}

	/**
	 * Implements the interface of XMLObject
	 */
	public void XwriteObject(XMLHelper h) {
		this.changed = false;
		
		h.openNewElem("Rule", this);
		h.addAttr("name", this.itsName);
		
		if (!"".equals(this.formStr))
			h.addAttr("formula", this.formStr);
		if (!this.enabled)
			h.addAttr("enabled", "false");
		if (this.triggerOfLayer)
			h.addAttr("trigger", "true");
		if (this.parallelMatching)
			h.addAttr("parallel", "true");
		if (this.startParallelMatchByFirstCSPVar)
			h.addAttr("parallelByFirst", "true");
		if (this.waitBeforeApply)
			h.addAttr("waitBeforeApply", "true");
		
		AttrContext context = getAttrContext();
		h.addObject("", context.getVariables(), true);

		getSource().setKind(GraphKind.LHS);
		h.addObject("", getSource(), true);
		getSource().setKind(GraphKind.RHS);
		h.addObject("", getTarget(), true);
//		String namestr = this.getName();
		writeMorphism(h);

		// NACs
		Enumeration<OrdinaryMorphism> nacs = getNACs();
		// PACs
		Enumeration<OrdinaryMorphism> pacs = getPACs();
		// nested ACs
		Enumeration<OrdinaryMorphism> nested = getNestedACs();
		
		// Attr context conditions
		AttrConditionTuple condt = context.getConditions();
		int num = condt.getNumberOfEntries();

		if (nested.hasMoreElements()
				|| nacs.hasMoreElements() 
				|| pacs.hasMoreElements() 
				|| (num > 0)
				|| (this.itsUsedAtomics != null && this.itsUsedAtomics.size() > 0)) {
			h.openSubTag("ApplCondition");
			// NACs
			while (nacs.hasMoreElements()) {
				OrdinaryMorphism m = nacs.nextElement();
				m.getTarget().setKind(GraphKind.NAC);
				h.openSubTag("NAC");
				if (!m.isEnabled())
					h.addAttr("enabled", "false");
				h.addObject("", m.getTarget(), true);
				m.writeMorphism(h);
				h.close();
			}
			// PACs
			while (pacs.hasMoreElements()) {
				OrdinaryMorphism m = pacs.nextElement();
				m.getTarget().setKind(GraphKind.PAC);
				h.openSubTag("PAC");
				if (!m.isEnabled())
					h.addAttr("enabled", "false");
				h.addObject("", m.getTarget(), true);
				m.writeMorphism(h);
				h.close();
			}
			// nested ACs
			while (nested.hasMoreElements()) {
				OrdinaryMorphism m = nested.nextElement();
				m.getTarget().setKind(GraphKind.AC);
				h.openSubTag("NestedAC");
				if (!m.isEnabled())
					h.addAttr("enabled", "false");
				h.addObject("", m.getTarget(), true);
				m.writeMorphism(h);
				
				((NestedApplCond) m).writeNestedApplConds(h);
				
				h.close();
			}			
			// Attr context conditions
			if (num > 0) {
				h.openSubTag("AttrCondition");
				h.addObject("", condt, true);
				h.close();
			}
			// Post Application Constraints
			if ((this.itsUsedAtomics != null && this.itsUsedAtomics.size() > 0) 
					&& (this.itsUsedFormulas != null && this.itsUsedFormulas.size() > 0)) {
				h.openSubTag("PostApplicationCondition");
				// save formulas
				for (int i = 0; i < this.itsUsedFormulas.size(); i++) {
					Formula f = this.itsUsedFormulas.elementAt(i);
					h.openSubTag("FormulaRef");
					h.addObject("f", f, false);
					h.close();
				}
				h.close();
			}
			
			h.close(); // ApplCondition
		}

		// TaggedValue layer
		h.openSubTag("TaggedValue");
		h.addAttr("Tag", "layer");
		h.addAttr("TagValue", this.layer);
		h.close();

		// TaggedValue priority
		h.openSubTag("TaggedValue");
		h.addAttr("Tag", "priority");
		h.addAttr("TagValue", this.priority);
		h.close();

		h.close();
	}

	/**
	 * Implements the interface of XMLObject
	 */
	public void XreadObject(XMLHelper h) {
		if (h.isTag("Rule", this)) {
			
			String attrStr = h.readAttr("formula");
			if (!"".equals(attrStr))
				this.formStr = attrStr;
			else
				this.formStr = "true";
			this.setTextualComment("Formula: ".concat(this.formStr));
			
			attrStr = h.readAttr("enabled");
			if (attrStr != null && attrStr.equals("false"))
				this.enabled = false;
			else
				this.enabled = true;

			attrStr = h.readAttr("trigger");
			if (attrStr != null && attrStr.equals("true"))
				this.triggerOfLayer = true;
			else
				this.triggerOfLayer = false;

			attrStr = h.readAttr("parallel");
			if (attrStr != null && attrStr.equals("true"))
				this.parallelMatching = true;
			else
				this.parallelMatching = false;

			attrStr = h.readAttr("parallelByFirst");
			if (attrStr != null && attrStr.equals("true"))
				this.startParallelMatchByFirstCSPVar = true;
			else
				this.startParallelMatchByFirstCSPVar = false;
			
			attrStr = h.readAttr("waitBeforeApply");
			if (attrStr != null && attrStr.equals("true"))
				this.waitBeforeApply = true;
			else
				this.waitBeforeApply = false;
			
			h.enrichObject(getAttrContext().getVariables());
			h.getObject("", getSource(), true);
			h.getObject("", getTarget(), true);
			readMorphism(h);
			
			this.itsOrig.setName("LeftOf_" + getName());
			this.itsOrig.setKind(GraphKind.LHS);
			this.itsOrig.setHelpInfo(this.getName());
			
			this.itsImag.setName("RightOf_" + getName());
			this.itsImag.setKind(GraphKind.RHS);
			this.itsImag.setHelpInfo(this.getName());
			
			Vector<Formula> tmpFormulas = new Vector<Formula>(); // of PostApplicationCondition
			
//			Vector<NestedApplCond> nacs = new Vector<NestedApplCond>();
//			Vector<NestedApplCond> pacs = new Vector<NestedApplCond>();
//			boolean needConvertToFormula = false;
			
			if (h.readSubTag("ApplCondition")) {
				while (h.readSubTag("NAC")) {
					boolean nacEnabled = true;
					Object nacattr_enabled = h.readAttr("enabled");
					if ((nacattr_enabled != null)
							&& ((String) nacattr_enabled).equals("false"))
						nacEnabled = false;

					OrdinaryMorphism nac = createNAC();
//					NestedApplCond nac = createNestedAC();
//					nacs.add(nac);
//					needConvertToFormula = true;
					
					nac.getTarget().setHelpInfo(this.getName());
					
					nac.getTarget().xyAttr = this.getLeft().xyAttr;
					
					h.getObject("", nac.getTarget(), true);
					nac.readMorphism(h);
					h.close();
					nac.setEnabled(nacEnabled);
					nac.getTarget().setHelpInfo("");
					if (nac.getName().isEmpty())
						nac.setName("nac".concat(String.valueOf(this.itsNACs.size())));
				}
				while (h.readSubTag("PAC")) {
					boolean pacEnabled = true;
					Object pacattr_enabled = h.readAttr("enabled");
					if ((pacattr_enabled != null)
							&& ((String) pacattr_enabled).equals("false"))
						pacEnabled = false;

					OrdinaryMorphism pac = createPAC();
//					NestedApplCond pac = createNestedAC();					
//					pacs.add(pac);
//					needConvertToFormula = true;
					
					pac.getTarget().setHelpInfo(this.getName());
					
					pac.getTarget().xyAttr = this.getLeft().xyAttr;
					
					h.getObject("", pac.getTarget(), true);
					pac.readMorphism(h);
					h.close();
					pac.setEnabled(pacEnabled);
					pac.getTarget().setHelpInfo("");
					if (pac.getName().isEmpty())
						pac.setName("pac".concat(String.valueOf(this.itsPACs.size())));
				}
				while (h.readSubTag("NestedAC")) {
//					needConvertToFormula = false;
					boolean acEnabled = true;
					Object acattr_enabled = h.readAttr("enabled");
					if ((acattr_enabled != null)
							&& ((String) acattr_enabled).equals("false"))
						acEnabled = false;

					NestedApplCond ac = createNestedAC();
					ac.getTarget().setHelpInfo(this.getName());
					
					ac.getTarget().xyAttr = this.getLeft().xyAttr;
					
					h.getObject("", ac.getTarget(), true);
					ac.readMorphism(h);
					
					ac.readNestedApplConds(h);
					
					h.close();
					ac.setEnabled(acEnabled);
					ac.getTarget().setHelpInfo("");	
					if (ac.getName().isEmpty())
						ac.setName("gac".concat(String.valueOf(this.itsACs.size())));
				}
				if (h.readSubTag("AttrCondition")) {
					AttrConditionTuple condt = getAttrContext().getConditions();
					if (condt != null)
						h.enrichObject(condt);
					h.close();
				}

				// read Post Application Constraints
				if (h.readSubTag("PostApplicationCondition")) {
					// System.out.println("PostApplicationCondition");
					// read formulas
					while (h.readSubTag("FormulaRef")) {
						Formula f = new Formula(true);
						f.setName("");
						Formula f1 = (Formula) h.getObject("f", null, false);
						// System.out.println("Formula: "+f1);
						if (f1 != null)
							tmpFormulas.addElement(f1);
						h.close();
					}

					h.close();

					// generatePostConstraints = true;
//					setUsedFormulas(tmpFormulas);
//					convertUsedFormulas();
				}

				h.close();
			}

			// read layer
			if (h.readSubTag("TaggedValue")) {
				int v = 0;
				String t = h.readAttr("Tag");
				// read old attribute
				int v1 = h.readIAttr("Value");
				// read new attribute
				int v2 = h.readIAttr("TagValue");
				if (v1 > 0)
					v = v1;
				else if (v2 > 0)
					v = v2;
				if (t.equals("layer"))
					this.layer = v;
				h.close();
			}

			// read priority
			if (h.readSubTag("TaggedValue")) {
				int v = 0;
				String t = h.readAttr("Tag");
				// read old attribute
				int v1 = h.readIAttr("Value");
				// read new attribute
				int v2 = h.readIAttr("TagValue");
				if (v1 > 0)
					v = v1;
				else if (v2 > 0)
					v = v2;
				if (t.equals("priority"))
					this.priority = v;
				h.close();
			}

			/*
			 * alte Variante : NOT MORE USED! / read Post Application
			 * Constraints generatePostConstraints = false;
			 * if(h.readSubTag("TaggedValue")) { String t = h.readAttr("Tag");
			 * int v = h.readIAttr("Value"); if(t.equals("post_constraints")) {
			 * if(v != 0) generatePostConstraints = true; } h.close(); }
			 */

			h.close();

			this.applicable = true;
			
			setUsedFormulas(tmpFormulas);
			
			this.itsOrig.setHelpInfo("");
			this.itsImag.setHelpInfo("");

//			if ( needConvertToFormula && "true".equals(this.formStr)) {
//				convertToFormula(pacs, nacs);
//			} 
//			else 
				this.setFormula(this.formStr);
			
		}
	}

	@SuppressWarnings("unused")
	private boolean convertToFormula(
			final List<NestedApplCond> pacs,
			final List<NestedApplCond> nacs) {
		
		final List<Evaluable> vars = new Vector<Evaluable>(this.itsACs.size());
		
		if (this.itsACs.size() == 0) {
			this.formStr = "true";
			this.formReadStr = this.formStr;
			return true;
		}		
		
		String tmp = "";
		int indx = -1;
		for (int i=0; i<pacs.size(); i++) {
			NestedApplCond ac = pacs.get(i);
			if (ac.isEnabled()) {
				indx++;
				vars.add(ac);								
				if (vars.size() == 1)
					tmp = tmp.concat(String.valueOf(indx+1));
				else
					tmp = tmp.concat("&").concat(String.valueOf(indx+1));
			}
		}
		for (int i=0; i<nacs.size(); i++) {
			NestedApplCond ac = nacs.get(i);
			if (ac.isEnabled()) {
				indx++;
				vars.add(ac);								
				if (vars.size() == 1)
					tmp = tmp.concat("!".concat(String.valueOf(indx+1)));
				else
					tmp = tmp.concat("&!").concat(String.valueOf(indx+1));
			}
		}
		if ("".equals(tmp)) {
			this.formStr = "true";
			this.formReadStr = this.formStr;
			return true;
		}
		
		if (this.itsFormula.setFormula(vars, tmp)) {
			this.formStr = this.itsFormula.getAsString(vars);
			this.formReadStr = this.formStr;
//			System.out.println("Rule: "+this.getName()+"   formula: "+this.formStr);
			return true;
		}
		
		return false;
	}
	
	// ------ additional methods according to Gabi's new AGG design ---------
	// ----------- attention: yet untested! (SG, Aug.1999) ------------------

	/**
	 * Returns an inverted rule. This rule has to be injective,
	 * otherwise - returns null.
	 * The attribute mappings are NOT inverted, thus: the resulting left and
	 * right-hand side graphs are not attributed anymore.
	 */
	public Rule invertSimplex() {
		if (!this.isInjective()) {
			return (null);
		} 
		Rule inverse = new Rule();
		Graph lgraph = this.getLeft();
		Graph rgraph = this.getRight();
		Graph linverse = inverse.getLeft();
		Graph rinverse = inverse.getRight();
		OrdinaryMorphism lmorph = new OrdinaryMorphism(lgraph, rinverse);
		OrdinaryMorphism rmorph = new OrdinaryMorphism(rgraph, linverse);
		Iterator<Node> rnodes = rgraph.getNodesSet().iterator();
		while (rnodes.hasNext()) {
			Node rNode = rnodes.next();
			Node linverseNode = null;
			try {
				linverseNode = linverse.createNode(rNode.getType());
			} catch (TypeException e) {
				// if the old rule was well typed, the new
				// rule should be also well typed
				e.printStackTrace();
			}
			rmorph.addMapping(rNode, linverseNode);
		}
		Iterator<Node> lnodes = lgraph.getNodesSet().iterator();
		while (lnodes.hasNext()) {
			Node lNode = lnodes.next();
			Node rinverseNode = null;
			try {
				rinverseNode = rinverse.createNode(lNode.getType());
			} catch (TypeException e) {
				// if the old rule was well typed, the new
				// rule should be also well typed
				e.printStackTrace();
			}
			lmorph.addMapping(lNode, rinverseNode);
			GraphObject rn = this.getImage(lNode);
			if (rn != null) {
				inverse.addMapping(rmorph.getImage(rn), rinverseNode);
			}
		}
		Iterator<Arc> rarcs = rgraph.getArcsSet().iterator();
		while (rarcs.hasNext()) {
			Arc rArc = rarcs.next();
			Node linverseSource = (Node) rmorph.getImage(rArc.getSource());
			Node linverseTarget = (Node) rmorph.getImage(rArc.getTarget());
			Arc linverseArc = null;
			try {
				linverseArc = linverse.createArc(rArc.getType(),
						linverseSource, linverseTarget);
				rmorph.addMapping(rArc, linverseArc);
			} catch (TypeException ex) {
			}
		}

		Iterator<Arc> larcs = lgraph.getArcsSet().iterator();
		while (larcs.hasNext()) {
			Arc lArc = larcs.next();
			Node rinverseSource = (Node) lmorph.getImage(lArc.getSource());
			Node rinverseTarget = (Node) lmorph.getImage(lArc.getTarget());
			Arc rinverseArc = null;
			try {
				rinverseArc = rinverse.createArc(lArc.getType(),
						rinverseSource, rinverseTarget);
				lmorph.addMapping(lArc, rinverseArc);
			} catch (TypeException ex) {}
			GraphObject ra = this.getImage(lArc);
			if (ra != null) {
				inverse.addMapping(rmorph.getImage(ra), rinverseArc);
			}
		}
		return (inverse);
		
	}

	/**
	 * Tries to invert this rule. The rule has to be injective.
	 * The attribute mappings are NOT inverted, thus the resulting
	 * left and right-hand side graphs are not attributed anymore. 
	 * 
	 * Returns the pair with an inverted rule as the first element 
	 * and a help pair of two graph morphisms as the second element. 
	 * The first morphism is between the LHS of this
	 * and the RHS of the inverted rule, 
	 * the second morphism is between the RHS of this
	 * and the LHS of the inverted rule. 
	 * If this rule is not injective - returns null.
	 */
	public Pair<Rule, Pair<OrdinaryMorphism, OrdinaryMorphism>> invertComplex() {
		if (!this.isInjective()) {
			return (null);
		} 
		Rule inverse = new Rule();
		Graph lgraph = this.getLeft();
		Graph rgraph = this.getRight();
		Graph linverse = inverse.getLeft();
		Graph rinverse = inverse.getRight();
		OrdinaryMorphism lmorph = new OrdinaryMorphism(lgraph, rinverse);
		OrdinaryMorphism rmorph = new OrdinaryMorphism(rgraph, linverse);
		Iterator<Node> rnodes = rgraph.getNodesSet().iterator();
		while (rnodes.hasNext()) {
			Node rNode = rnodes.next();
			Node linverseNode = null;
			try {
				linverseNode = linverse.createNode(rNode.getType());
			} catch (TypeException e) {
				// if the old rule was well typed, the new
				// rule should be also well typed
				e.printStackTrace();
			}
			rmorph.addMapping(rNode, linverseNode);
		}
		Iterator<Node> lnodes = lgraph.getNodesSet().iterator();
		while (lnodes.hasNext()) {
			Node lNode = lnodes.next();
			Node rinverseNode = null;
			try {
				rinverseNode = rinverse.createNode(lNode.getType());
			} catch (TypeException e) {
				// if the old rule was well typed, the new
				// rule should be also well typed
				e.printStackTrace();
			}
			lmorph.addMapping(lNode, rinverseNode);
			GraphObject rn = this.getImage(lNode);
			if (rn != null) {
				inverse.addMapping(rmorph.getImage(rn), rinverseNode);
			}
		}
		Iterator<Arc> rarcs = rgraph.getArcsSet().iterator();
		while (rarcs.hasNext()) {
			Arc rArc = rarcs.next();
			Node linverseSource = (Node) rmorph.getImage(rArc.getSource());
			Node linverseTarget = (Node) rmorph.getImage(rArc.getTarget());
			Arc linverseArc = null;
			try {
				linverseArc = linverse.createArc(rArc.getType(),
						linverseSource, linverseTarget);
				rmorph.addMapping(rArc, linverseArc);
			} catch (TypeException ex) {}
		}
		Iterator<Arc> larcs = lgraph.getArcsSet().iterator();
		while (larcs.hasNext()) {
			Arc lArc = larcs.next();
			Node rinverseSource = (Node) lmorph.getImage(lArc.getSource());
			Node rinverseTarget = (Node) lmorph.getImage(lArc.getTarget());
			Arc rinverseArc = null;
			try {
				rinverseArc = rinverse.createArc(lArc.getType(),
							rinverseSource, rinverseTarget);
				lmorph.addMapping(lArc, rinverseArc);
			} catch (TypeException ex) {}
			GraphObject ra = this.getImage(lArc);
			if (ra != null) {
				inverse.addMapping(rmorph.getImage(ra), rinverseArc);
			}
		}
		Pair<OrdinaryMorphism, OrdinaryMorphism> information = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
					lmorph, rmorph);
		return (new Pair<Rule, Pair<OrdinaryMorphism, OrdinaryMorphism>>(
					inverse, information));
	}

	/**
	 * A plain rule returns null.<br>
	 * Its subclasses <code>KernelRule</code>, 
	 * <code>MultiRule</code>, <code>RuleScheme</code>,
	 * <code>AmalgamatedRule</code>  overide this method
	 * to return its <code>RuleScheme</code>.
	 */
	public RuleScheme getRuleScheme() {
		return null;
	}
	
	/** Returns my current match */
	public Match getMatch() {
		return this.itsMatch;
	}

	/**
	 * Compares attribute value of the specified objects due to
	 * constant value of the first object. 
	 * Failed attribute value of the second object will be unset.
	 * Checks all members of the attribute tuple.
	 * 
	 * @param src  first object (an object of the LHS of a rule)
	 * @param tgt 	second object (an object of a NAC, PAC of a rule) 
	 * @return	true if attribute value is equal, otherwise false
	 */
	public boolean compareConstantAttributeValue(
			final GraphObject src, 
			final GraphObject tgt) {
		boolean result = true;
		if (src.getAttribute() != null
				&& tgt.getAttribute() != null) {
			final ValueTuple tgtValue = (ValueTuple) tgt.getAttribute();
			final ValueTuple srcValue = (ValueTuple) src.getAttribute();
			for (int i=0; i<srcValue.getNumberOfEntries(); i++) {
				final ValueMember lhsvm = srcValue.getValueMemberAt(i);
				final ValueMember tgtvm = tgtValue.getValueMemberAt(lhsvm.getName());
				if (lhsvm.isSet() 
						&& lhsvm.getExpr().isConstant()
						&& tgtvm != null && tgtvm.isSet()
						&& !lhsvm.getExprAsText().equals(tgtvm.getExprAsText())) {
					result = false;				
					tgtvm.setExpr(null);
				}
			}
		}
		return result;
	}
	
	/**
	 * Compares attribute value of the specified objects due to
	 * constant value of the first object. 
	 * Failed attribute value of the second object will be unset.
	 * The check broken after at least one attribute failed.
	 * 
	 * @param src  first object (an object of the LHS of a rule)
	 * @param tgt 	second object (an object of a NAC, PAC of a rule) 
	 * @return	true if attribute value is equal, otherwise false 
	 */
	public boolean compareConstAttrValueOfMapObjs(
			final GraphObject src, final GraphObject tgt) {
		
		if (src.getAttribute() != null
				&& tgt.getAttribute() != null) {
			final ValueTuple tgtValue = (ValueTuple) tgt.getAttribute();
			final ValueTuple srcValue = (ValueTuple) src.getAttribute();
			for (int i=0; i<srcValue.getNumberOfEntries(); i++) {
				final ValueMember srcvm = srcValue.getValueMemberAt(i);
				final ValueMember tgtvm = tgtValue.getValueMemberAt(srcvm.getName());
				if (srcvm.isSet() 
						&& srcvm.getExpr().isConstant()
						&& tgtvm.isSet()
						&& !srcvm.getExprAsText().equals(tgtvm.getExprAsText())) {
					tgtvm.setExpr(null);
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Compares its LHS, RHS, morphism, NACs, PACs and attribute context 
	 * to the appropriate elements of the specified rule.
	 * Returns true if all elements are equal.
	 */
	public boolean compareTo(Rule r) {
		// System.out.println("Rule.compareTo");
		Pair<Boolean,String> errMsgHolder = null;
		// compare rule morphism
		if (!((OrdinaryMorphism) this).compareTo(r)) {
			// System.out.println("Rule: "+getName()+" :: Mapping failed!");
			errMsgHolder = new Pair<Boolean,String>(Boolean.valueOf(true),
													"Rule content is different.");
			return false;
		}
		// compare NACs
		errMsgHolder = compareApplConds(this.getNACsList(), r.getNACsList(), "NAC");
		if (errMsgHolder != null)
			return false;	
		
		// compare PACs
		errMsgHolder = compareApplConds(this.getPACsList(), r.getPACsList(), "PAC");
		if (errMsgHolder != null)
			return false;
		
		// compare nested ACs
		errMsgHolder = compareApplConds(this.getNestedACsList(), r.getNestedACsList(), "nested AC");
		if (errMsgHolder != null)
			return false;
		
		// compare rule context
		VarTuple var = (VarTuple) getAttrContext().getVariables();
		VarTuple varOther = (VarTuple) r.getAttrContext().getVariables();
		if (!var.compareTo(varOther)) {
			errMsgHolder = new Pair<Boolean,String>(Boolean.valueOf(true),
													"Variable rule context is different.");
			return false;
		}
		CondTuple cond = (CondTuple) getAttrContext().getConditions();
		CondTuple condOther = (CondTuple) r.getAttrContext().getConditions();
		if (!cond.compareTo(condOther)) {
			errMsgHolder = new Pair<Boolean,String>(Boolean.valueOf(true),
													"Conditional rule context is different.");
			return false;
		}
		return true;
	}

	private Pair<Boolean,String> compareApplConds(
			final List<OrdinaryMorphism> applConds, 
			final List<OrdinaryMorphism> otherApplConds,
			String what) {
		// compare ACs
		Vector<OrdinaryMorphism> another = new Vector<OrdinaryMorphism>();
		another.addAll(otherApplConds);
		if (applConds.size() != another.size()) {
			// System.out.println("Rule: "+getName()+" NACs discrepancy!");
			Pair<Boolean,String> errMsgHolder = new Pair<Boolean,String>(
														Boolean.valueOf(true),
														"Number of "+what+"s is different.");
			return errMsgHolder;
		}
		OrdinaryMorphism ac = null;
		for (int i = 0; i < applConds.size(); i++) {
			ac = applConds.get(i);
			for (int j = another.size() - 1; j >= 0; j--) {
				OrdinaryMorphism ac1 = another.elementAt(j);
				if (ac.compareTo(ac1)) {
					another.remove(ac1);
					break;
				}
			}
		}
		if (another.size() != 0 && ac != null) {
			Pair<Boolean,String> errMsgHolder = new Pair<Boolean,String>(
													Boolean.valueOf(true),
													what+":  " + ac.getName()+ "  is different.");
			return errMsgHolder;
		}
		return null;
	}
	
	public List<GraphObject> getInputParameterObjectsLeft(final List<String> inputParams) {
		return getInputParameterObjects(this.getLeft(), inputParams);
	}
	
	public List<GraphObject> getInputParameterObjectsRight(final List<String> inputParams) {
		return getInputParameterObjects(this.getRight(), inputParams);
	}
	
    private List<GraphObject> getInputParameterObjects(final Graph g, final List<String> inputParams) {
		List<GraphObject> goIP = new Vector<GraphObject>();
		Enumeration<GraphObject> elems = g.getElements();
		while (elems.hasMoreElements()) {
			GraphObject go = elems.nextElement();
			if (go.getAttribute() != null) {
				ValueTuple val = (ValueTuple)go.getAttribute(); 
				for (int i=0; i<val.getNumberOfEntries(); i++) {
					ValueMember mem = val.getEntryAt(i);
					if (mem.isSet() && mem.getExpr().isVariable()) {				
						if (inputParams.contains(mem.getExprAsText())) {
							goIP.add(go);
						}
					}
				}
			}
		}
		return goIP;
    }
    
	public List<GraphObject> getLeftInputParameterObjects() {
		List<GraphObject> list = new Vector<GraphObject>();
		VarTuple var = (VarTuple) getAttrContext().getVariables();
		Enumeration<GraphObject> elems = this.itsOrig.getElements();
		while (elems.hasMoreElements()) {
			GraphObject go = elems.nextElement();
			if (go.getAttribute() != null) {
				ValueTuple val = (ValueTuple) go.getAttribute();
				for (int i=0; i<val.getNumberOfEntries(); i++) {
					ValueMember mem = val.getValueMemberAt(i);
					if (mem.isSet() && mem.getExpr().isVariable()) {
						if (var.getVarMemberAt(mem.getExprAsText()) != null
								&& var.getVarMemberAt(mem.getExprAsText()).isInputParameter()) {
							list.add(go);
						}
					}
				}
			}
		}
//		System.out.println(list);
		return list;
	}
	
	public List<GraphObject> getRightInputParameterObjects() {
		List<GraphObject> list = new Vector<GraphObject>();
		VarTuple var = (VarTuple) getAttrContext().getVariables();
		Enumeration<GraphObject> elems = this.itsImag.getElements();
		while (elems.hasMoreElements()) {
			GraphObject go = elems.nextElement();
			if (go.getAttribute() != null) {
				ValueTuple val = (ValueTuple) go.getAttribute();
				for (int i=0; i<val.getNumberOfEntries(); i++) {
					ValueMember mem = val.getValueMemberAt(i);
					if (mem.isSet() && mem.getExpr().isVariable()) {
						if (var.getVarMemberAt(mem.getExprAsText()) != null
								&& var.getVarMemberAt(mem.getExprAsText()).isInputParameter()) {
							list.add(go);
						}
					}
				}
			}
		}
		return list;
	}
	
	public Vector<String> getInputParameterNames() {
		Vector<String> inputParams = new Vector<String>(1);
		VarTuple var = (VarTuple) getAttrContext().getVariables();
		for (int i = 0; i < var.getNumberOfEntries(); i++) {
			VarMember varm = var.getVarMemberAt(i);
			if (varm.isInputParameter())
				inputParams.add(varm.getName());
		}
		return inputParams;
	}
		
	/**
	 * Returns variables of the attribute context of this rule
	 * which are used as input parameter for the rule application.
	 */
	public Vector<VarMember> getInputParameters() {
		Vector<VarMember> inputParams = new Vector<VarMember>(1);
		VarTuple var = (VarTuple) getAttrContext().getVariables();
		for (int i = 0; i < var.getNumberOfEntries(); i++) {
			VarMember varm = var.getVarMemberAt(i);
			if (varm.isInputParameter())
				inputParams.addElement(varm);
		}
		return inputParams;
	}

	public Vector<VarMember> getInputParametersLeft() {
		Vector<VarMember> inputParams = new Vector<VarMember>(1);
		VarTuple var = (VarTuple) getAttrContext().getVariables();
		for (int i = 0; i < var.getNumberOfEntries(); i++) {
			VarMember vm = var.getVarMemberAt(i);
			if (vm.isInputParameter()
					&& vm.getMark() == VarMember.LHS) {
				inputParams.addElement(vm);
			}
		}
		return inputParams;
	}
	
	public Vector<VarMember> getInputParametersRight() {
		Vector<VarMember> inputParams = new Vector<VarMember>(1);
		VarTuple var = (VarTuple) getAttrContext().getVariables();
		for (int i = 0; i < var.getNumberOfEntries(); i++) {
			VarMember vm = var.getVarMemberAt(i);
			if (vm.isInputParameter()
					&& (vm.getMark() == VarMember.RHS 
							|| vm.getMark() == VarMember.NAC)) {
				inputParams.addElement(vm);
			}
		}
		return inputParams;
	}
	
	/**
	 * Returns variables of the attribute context of this rule
	 * which are used by attributes of the specified graph object
	 * as an input parameter for the rule application.
	 */
	public Vector<VarMember> getInputParametersOfGraphObject(final GraphObject go, final VarTuple var) {		
		if (go.getAttribute() == null)
			return new Vector<VarMember>();
		
		Vector<VarMember> inputParams = new Vector<VarMember>(1);
		ValueTuple attrVal = (ValueTuple)go.getAttribute();		
		for (int i = 0; i < attrVal.getNumberOfEntries(); i++) {
			ValueMember vm = attrVal.getValueMemberAt(i);
			if (vm.isSet() && vm.getExpr().isVariable()) {
				VarMember varm = var.getVarMemberAt(vm.getExprAsText());
				if (varm != null && varm.isInputParameter())
					inputParams.addElement(varm);
			}			
		}
		return inputParams;
	} 
	
	public Vector<VarMember> getNonInputParametersOfNewGraphObjects() {
		VarTuple var = (VarTuple) getAttrContext().getVariables();
		Vector<VarMember> params = new Vector<VarMember>(1);
		final Enumeration<GraphObject> objs = this.itsImag.getElements();
		while (objs.hasMoreElements()) {
			GraphObject go = objs.nextElement();
			if (go.getAttribute() == null
					|| this.itsCodomObjects.contains(go))
				continue;
						
			ValueTuple attrVal = (ValueTuple)go.getAttribute();		
			for (int i = 0; i < attrVal.getNumberOfEntries(); i++) {
				ValueMember vm = attrVal.getValueMemberAt(i);
				if (vm.isSet() && vm.getExpr().isVariable()) {
					VarMember varm = var.getVarMemberAt(vm.getExprAsText());
					if (varm != null && !varm.isInputParameter())
						params.addElement(varm);
				}			
			}			
		}
		return params;
	}
	
	public Vector<VarMember> getNonInputParameters() {
		VarTuple var = (VarTuple) getAttrContext().getVariables();
		Vector<VarMember> params = new Vector<VarMember>(1);
		
		for (int i = 0; i < var.getNumberOfEntries(); i++) {
			VarMember v = var.getVarMemberAt(i);
			if (!v.isInputParameter())
				params.addElement(v);			
		}					
		return params;
	}
	
	/**
	 * always returns TRUE. It is not yet implemented!
	 */
	public boolean areNACsValid() {
//		for (int i = 0; i < itsNACs.size(); i++) {
//			OrdinaryMorphism nac = itsNACs.get(i);
//			if (!isNACValid(nac) {
//				return false;
//			}
//		}
		return true;
	}

	
//	public boolean isGlobalNAC(OrdinaryMorphism nac) {
//		return nac.isEmpty();		
//	}
//	
//	public boolean isGlobalPAC(OrdinaryMorphism pac) {
//		return pac.isEmpty();		
//	}
	
	/**
	 * always returns TRUE. It is not yet implemented!
	 */
	public boolean isNACValid(OrdinaryMorphism nac) {
		return true;
	}

	/**
	 * Shift of an application condition is not possible when it may cause a dangling edge.
	 */
	public boolean isACShiftPossible(OrdinaryMorphism ac) {
		Iterator<Arc> arcs = ac.getTarget().getArcsCollection().iterator();
		while (arcs.hasNext()) {
			Arc a = arcs.next();
			if (!ac.getInverseImage(a).hasMoreElements()) {
				if (ac.getInverseImage(a.getSource()).hasMoreElements()) {
					if (this.getImage(ac.getInverseImage(a.getSource()).nextElement()) == null)
						return false;
				}
				if (ac.getInverseImage(a.getTarget()).hasMoreElements()) {
					if (this.getImage(ac.getInverseImage(a.getTarget()).nextElement()) == null)
						return false;
				}
			}
		}
		return true;
	}
	
	
	/**
	 * Checks dangling edges of the given pac.
	 * Returns true if no dangling edge exists, otherwise false.
	 */
	public boolean isPACValid(OrdinaryMorphism ac) {
		if (ac.isEnabled()) {
			final Iterator<Node> objects = this.itsOrig.getNodesSet().iterator();
			while (objects.hasNext()) {
				final Node x = objects.next();
				if (this.getImage(x) == null) {
					final Node y = (Node) ac.getImage(x);
					if (y != null
							&& x.getNumberOfArcs() != y.getNumberOfArcs()) {	
						this.setErrorMsg(ac.getName()+"  -  PAC failed (dangling edge)");
//						this.setErrorMsg(this.getName()+":    "+ac.getName()+"  -  PAC failed (dangling edge)");
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Checks dangling edges of the its pacs.
	 * Returns true if no dangling edge exists, otherwise false.
	 */
	public boolean arePACsValid() {
		for (int i=0; i<this.itsPACs.size(); i++) {
			OrdinaryMorphism ac = this.itsPACs.get(i);
			if (!this.isPACValid(ac)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks dangling edges of the given general application condition ac.
	 * Returns true if no dangling edge exists, otherwise false.
	 */
	public boolean isGACValid(NestedApplCond ac) {
		if (ac.isEnabled()) {
			return ac.isValid();
		}
		return true;
	}
	
	/**
	 * Checks dangling edges of the its general application conditions.
	 * Returns true if no dangling edge exists, otherwise false.
	 */
	public boolean areGACsValid() {
		for (int i=0; i<this.itsACs.size(); i++) {
			NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
			if (!this.isGACValid(ac)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks dangling edges of the its pacs and general acs.
	 * Returns true if no dangling edge exists, otherwise false.
	 */
	public boolean areApplCondsValid() {
		for (int i=0; i<this.itsPACs.size(); i++) {
			OrdinaryMorphism ac = this.itsPACs.get(i);
			if (!this.isPACValid(ac)) {
				return false;
			}
		}
		for (int i=0; i<this.itsACs.size(); i++) {
			NestedApplCond ac = (NestedApplCond)this.itsACs.get(i);
			if (!ac.isValid()) {
				return false;
			}
		}
		return true;
	}
	
	/** Copies nodes and edges of its PACs in the LHS resp. RHS and extends the rule mapping.
	 * The PACs will be disabled.<br>
	 */
	public boolean extendByPacs() {
		for (int i=0; i<this.itsPACs.size(); i++) {
			OrdinaryMorphism pac = this.itsPACs.get(i);
			if (pac.isEnabled()) {
				extendByPac(pac);
				pac.setEnabled(false);
			}
		}		
		return true;
	}
	
	private boolean extendByPac(OrdinaryMorphism pac) {
		Hashtable<Node,Node> n2n = new Hashtable<Node,Node>();
		Iterator<Node> nodes = pac.getTarget().getNodesCollection().iterator();
		while (nodes.hasNext()) {
			Node pn = nodes.next();
			Enumeration<GraphObject> en = pac.getInverseImage(pn);
			if (!en.hasMoreElements()) {
				try {
					Node nln = this.itsOrig.copyNode(pn);
					n2n.put(pn, nln);
					Node nrn = this.itsImag.copyNode(pn);
					n2n.put(nln, nrn);
					try {
						this.addMapping(nln, nrn);
						nln.setContextUsage(pac.hashCode());
						nrn.setContextUsage(pac.hashCode());
					} catch (BadMappingException ex1) {
						return false;
					}
				} catch (TypeException ex) {
					return false;
				}				
			}
			else {
				while (en.hasMoreElements()) {
					Node ln = (Node) en.nextElement();
					n2n.put(pn, ln);
					Node rn = (Node) this.getImage(ln);
					if (rn != null) {
						n2n.put(ln, rn);						
					}
					break;
				}
			}
		}
		Iterator<Arc> arcs = pac.getTarget().getArcsCollection().iterator();
		while (arcs.hasNext()) {
			Arc pa = arcs.next();
			Enumeration<GraphObject> en = pac.getInverseImage(pa);
			if (!en.hasMoreElements()) {
				try {
					Node srcl = n2n.get(pa.getSource());
					Node tarl = n2n.get(pa.getTarget());
					if (srcl != null && tarl != null) {
						Arc nla = this.getOriginal().copyArc(pa, srcl, tarl);
						Node srcr = n2n.get(srcl);
						Node tarr = n2n.get(tarl);
						if (srcr != null && tarr != null) {
							Arc nra = this.getImage().copyArc(pa, srcr, tarr);
							if (nla != null && nra != null) {							
								try {
									this.addMapping(nla, nra);
									nla.setContextUsage(pac.hashCode());
									nra.setContextUsage(pac.hashCode());
								} catch (BadMappingException ex1) {
									return false;
								}
							}
						}
					} 
				} catch (TypeException ex) {
					return false;
				}				
			}
		}
		return true;
	}
	
	/** Undo the copy of its PACs done by <code>extendByPacs</code>.
	 * The PACs will be enabled.<br>
	 */
	public boolean extendByPacsUndo() {
		for (int i=0; i<this.itsPACs.size(); i++) {
			OrdinaryMorphism pac = this.itsPACs.get(i);
			if (extendByPacUndo(pac))
				pac.setEnabled(true);
		}		
		return true;
	}
	
	private boolean extendByPacUndo(OrdinaryMorphism pac) {
		boolean res = false;
		Iterator<Arc> arcsL = this.itsOrig.getArcsCollection().iterator();
		while (arcsL.hasNext()) {
			Arc aL = arcsL.next();
			if (aL.getContextUsage() == pac.hashCode()) {
				Arc aR = (Arc) this.getImage(aL);
				this.removeMapping(aL, aR);		
				try {
					this.itsImag.destroyArc(aR);
					this.itsOrig.destroyArc(aL);
					res = true;
				}
				catch (TypeException ex) {
					String exstr = ex.getLocalizedMessage();
				}
			}
		}			
		Iterator<Node> nodes = this.itsOrig.getNodesCollection().iterator();
		while (nodes.hasNext()) {
			Node nL = nodes.next();
			if (nL.getContextUsage() == pac.hashCode()) {
				Node nR = (Node) this.getImage(nL);
				this.removeMapping(nL, nR);		
				try {
					this.itsImag.destroyNode(nR);
					this.itsOrig.destroyNode(nL);
					res = true;
				}
				catch (TypeException ex) {
					String exstr = ex.getLocalizedMessage();
				}
			}
		}
		return res;
	}
	
	/**
	 * Checks existing variables of the attribute context against the attribute
	 * context of its current match and adjusts the attribute context of its match,
	 * if needed.
	 */
	public void adjustAttrContextOfMatch(boolean inputParameterOnly) {
		if (this.itsMatch != null) {
			this.itsMatch.adjustAttrInputParameter(inputParameterOnly);
			this.itsMatch.adjustAttrCondition();
		}
	}

	/**
	 * Set the value of variables of rule attribute context to null.
	 */
	public void unsetValueOfContextVariable(boolean inputParameterOnly) {
		VarTuple vt = (VarTuple) getAttrContext().getVariables();
		for (int i = 0; i < vt.getNumberOfEntries(); i++) {
			VarMember vm = vt.getVarMemberAt(i);
			if (inputParameterOnly) {
				if (vm.isInputParameter())
					vm.setExpr(null);
			} else
				vm.setExpr(null);
		}
	}

	/**
	 * Attribute context variable which is an input parameter is no more input parameter
	 * after this method applied.
	 */
	public void unsetInputParameter() {
		AttrVariableTuple avt = getAttrContext().getVariables();
		for (int i = 0; i < avt.getNumberOfEntries(); i++) {
			VarMember vm = (VarMember) avt.getMemberAt(i);
			if (vm.isInputParameter())
				vm.setInputParameter(false);
		}
	}
	
	/**
	 * Returns the name of an input parameter whithout value, otherwise - null.
	 */
	public String getInputParameterWithoutValue() {
		AttrVariableTuple avt = getAttrContext().getVariables();
		for (int i = 0; i < avt.getNumberOfEntries(); i++) {
			VarMember vm = (VarMember) avt.getMemberAt(i);
			if (vm.isInputParameter() && !vm.isSet())
				return vm.getName();
		}
		return null;
	}

	/**
	 * Returns name(s) of the variables of the attribute context which are used
	 * as input parameter(s) of this rule and they are not set.
	 * If the specified parameter is TRUE then the LHS
	 * (NACs, PACs) with an input parameter for matching are taken in account.
	 * If the specified parameter is FALSE then the RHS
	 * (NACs, PACs) with an input parameter for matching are taken in acount.
	 *  
	 * Returns null if all input parameter are set.
	 */
	public String getInputParameterWithoutValue(boolean left) {
		AttrVariableTuple avt = getAttrContext().getVariables();
		for (int i = 0; i < avt.getNumberOfEntries(); i++) {
			VarMember vm = (VarMember) avt.getMemberAt(i);
			if (vm.isInputParameter() && !vm.isSet()) {
				if (left) {
					Vector<String> vars = getLeft().getVariableNamesOfAttributes();
					for (int j = 0; j < vars.size(); j++) {
						if (vars.get(j).equals(vm.getName()))
							return vm.getName();
					}
				} else {
					Vector<String> vars = getRight().getVariableNamesOfAttributes();
					for (int j = 0; j < vars.size(); j++) {
						if (vars.get(j).equals(vm.getName()))
							return vm.getName();
					}
				}
				for (int j = 0; j < this.itsNACs.size(); j++) {
					OrdinaryMorphism nac = this.itsNACs.get(j);
					Vector<String> vars = nac.getTarget()
							.getVariableNamesOfAttributes();
					for (int k = 0; k < vars.size(); k++) {
						if (vars.get(k).equals(vm.getName()))
							return vm.getName();
					}
				}
				for (int j = 0; j < this.itsPACs.size(); j++) {
					OrdinaryMorphism pac = this.itsPACs.get(j);
					Vector<String> vars = pac.getTarget()
							.getVariableNamesOfAttributes();
					for (int k = 0; k < vars.size(); k++) {
						if (vars.get(k).equals(vm.getName()))
							return vm.getName();
					}
				}
				for (int j = 0; j < this.itsACs.size(); j++) {
					OrdinaryMorphism ac = this.itsACs.get(j);
					Vector<String> vars = ac.getTarget()
							.getVariableNamesOfAttributes();
					for (int k = 0; k < vars.size(); k++) {
						if (vars.get(k).equals(vm.getName()))
							return vm.getName();
					}
				}
			}
		}
		return null;
	}

	
	private void deleteUnusedVars(Vector<VarMember> used) {
		VarTuple vars = (VarTuple) this.getAttrContext().getVariables();
		for (int i=0; i<vars.getNumberOfEntries(); i++) {
			VarMember vm = vars.getVarMemberAt(i);
			if (!used.contains(vm)) {
				vars.getTupleType().deleteMemberAt(vm.getName());
//				vars.showVariables();
			}
		}
	}
	
	/**
	 * Checks attribute setting of RHS, variable declarations and attribute
	 * conditions. 
	 * If all checks successful, it prepares infos about this rule.
	 * The method getErrorMessage() gives more information about fails.
	 */
	public boolean isReadyToTransform() {
		this.isReady = true;
		if (!this.enabled)
			return true;
		
		// check usage of abstract types of the RHS
		final Vector<String> abstractTypesOfRHS = new Vector<String>(1);
		Iterator<Node> enumer = this.itsImag.getNodesSet().iterator();
		while (enumer.hasNext()) {
			GraphObject o = enumer.next();
			Enumeration<GraphObject> inverse = getInverseImage(o);
			if (!inverse.hasMoreElements() && o.getType().isAbstract()) {
				abstractTypesOfRHS.add(o.getType().getName());
			}
		}

		this.isReady = abstractTypesOfRHS.isEmpty();
		if (!this.isReady) {
			this.errorMsg = this.errorMsg.concat("RHS: creating abstract nodes not allowed!  ").concat(abstractTypesOfRHS.toString());
			return false;
		}
		
		// check  PAC is valid: check dangling edge of nodes to delete which are used in a PAC
		for (int l=0; l<this.itsPACs.size(); l++) {
			this.isReady = this.isPACValid(this.itsPACs.get(l));
			if (!this.isReady) {
				return false;
			}			
		}	
		
		//   check attributes
		if (!isAttributed()) {
			return true;
		}
			
		this.applyDefaultAttrValuesOfTypeGraph(this.itsImag);
		
		AttrVariableTuple avt = this.itsAttrContext.getVariables();
		AttrConditionTuple act = this.itsAttrContext.getConditions();
		this.errorMsg = "";
		
		// get used variable and its declaration: (type, name)
		Vector<Pair<String, String>> varDecls = getVariableDeclarations();

		// add vars of NACs to varDecls
		for (int l=0; l<this.itsNACs.size(); l++) {				
			addVarDecl(this.itsNACs.get(l).getImage(), varDecls);			
		}

		// add vars of PACs to varDecls
		for (int l=0; l<this.itsPACs.size(); l++) {
			addVarDecl(this.itsPACs.get(l).getImage(), varDecls);				
		}

		// add vars of nested ACs to varDecls
		for (int l=0; l<this.itsACs.size(); l++) {
			addVarDecl(this.itsACs.get(l).getImage(), varDecls);				
		}
		
		
		// check: same variable name , different type :: should not happen!
		this.isReady = checkDoubleVarDecl(varDecls);
		if (!this.isReady) {
			return false;
		}
						
		// check used variables
		this.isReady = checkUsedVariables(avt, varDecls);
		if (!this.isReady) {
			return false;
		}
		
		// mark used variables: RHS, NAC, PAC, LHS
		markUsedVariables(avt);
		
		// check and mark the attr. conditions
		this.isReady = markAttrConditions(avt, act);
		if (!this.isReady) {
			return false;
		} 
				
		// find objects: to preserve, to delete, to create, to change,
		// also types which need to be checked due to multiplicity
		this.prepareRuleInfo();
		
		// adjust the attribute conditions (mark, enabled)
		// of my match, if it exists
		if (this.itsMatch != null) {
			if (this.itsMatch.getRule() == this) 
				this.itsMatch.adjustAttrCondition();
			else {
				this.itsMatch.dispose();
				this.itsMatch = null;
			}
		}
		
		// check attribute settings of the new objects
		this.isReady = this.checkAttributesOfNewObjects(avt);
		if (!this.isReady) {
			return false;
		}

		return this.isReady;
	}
	
	public boolean nacIsUsingVariable(
			final VarMember var, 
			final AttrConditionTuple act) {
		
		for (int i=0; i<this.itsNACs.size(); i++) {
			final OrdinaryMorphism nac = this.itsNACs.get(i);					
			if (nac.getTarget().isUsingVariable(var)) {
				return true;
			} 
			Vector<String> nacVars = nac.getTarget()
						.getVariableNamesOfAttributes();
			for (int j = 0; j < nacVars.size(); j++) {
				String varName = nacVars.get(j);
				for (int k = 0; k < act.getNumberOfEntries(); k++) {
					CondMember cond = (CondMember) act.getMemberAt(k);
					Vector<String> condVars = cond.getAllVariables();
					if (condVars.contains(varName)
							&& condVars.contains(var.getName())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean pacIsUsingVariable(
			final VarMember var, 
			final AttrConditionTuple act) {
		
		for (int i=0; i<this.itsPACs.size(); i++) {
			final OrdinaryMorphism pac = this.itsPACs.get(i);					
			if (pac.getTarget().isUsingVariable(var)) {
				return true;
			} 
			Vector<String> pacVars = pac.getTarget()
						.getVariableNamesOfAttributes();
			for (int j = 0; j < pacVars.size(); j++) {
				String varName = pacVars.get(j);
				for (int k = 0; k < act.getNumberOfEntries(); k++) {
					CondMember cond = (CondMember) act.getMemberAt(k);
					Vector<String> condVars = cond.getAllVariables();
					if (condVars.contains(varName)
							&& condVars.contains(var.getName())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	protected void applyDefaultAttrValuesOfTypeGraph(
			final Graph g,
			final Iterator<?> iter) {
		
		boolean right = g == this.getRight();
		while (iter.hasNext()) {
			GraphObject o = (GraphObject)iter.next();
			if (o.getAttribute() == null) {
				if ((o.getType().getAttrType() != null)
						&& (o.getType().getAttrType().getNumberOfEntries() != 0)) {
					o.createAttributeInstance();
				} else
					continue;
			}
			if (right && !this.getInverseImage(o).hasMoreElements()) {
				if (o.isNode())
					g.applyDefaultAttrValuesOfTypeGraph((Node)o, null);
				else 
					g.applyDefaultAttrValuesOfTypeGraph((Arc)o, null);
			}
		}
	}
	
	/*
	 * Use the attribute values of the nodes and edges of the Type Graph as default values
	 * for the attributes of the specified graph.
	 */
	public void applyDefaultAttrValuesOfTypeGraph(final Graph g) {
		this.applyDefaultAttrValuesOfTypeGraph(g, g.getNodesSet().iterator());
		this.applyDefaultAttrValuesOfTypeGraph(g, g.getArcsSet().iterator());
	}
	
	protected boolean isAttributed() {		
		boolean attributed = this.itsOrig.isAttributed() 
								|| this.itsImag.isAttributed();		
		 
		for (int l=0; !attributed && l<this.itsNACs.size() ; l++) {		
			attributed = this.itsNACs.get(l).getImage().isAttributed();			
		}
		 
		for (int l=0; !attributed && l<this.itsPACs.size(); l++) {
			attributed = this.itsPACs.get(l).getImage().isAttributed();				
		}
		 
		for (int l=0; !attributed && l<this.itsACs.size(); l++) {
			attributed = this.itsACs.get(l).getImage().isAttributed();				
		}
		return attributed;
	}
	
	private void addVarDecl(final Graph g, final Vector<Pair<String, String>> varDecls) {
		addVarDecl(g.getNodesSet().iterator(), varDecls);
		addVarDecl(g.getArcsSet().iterator(), varDecls);
	}
	
	private void addVarDecl(final Iterator<?>elems, final Vector<Pair<String, String>> varDecls) {
		while (elems.hasNext()) {
			GraphObject o = (GraphObject) elems.next();
			if (o.getAttribute() != null) {
				AttrInstance attr = o.getAttribute();
				ValueTuple vt = (ValueTuple) attr;
				for (int k = 0; k < vt.getSize(); k++) {
					ValueMember vm = vt.getValueMemberAt(k);
					if (vm.isSet() && vm.getExpr().isVariable()) {
						String n = vm.getExprAsText();
						String t = vm.getDeclaration().getTypeName();

//						System.out.println(o.getContext().getName()+"   "+n+"    "+t);
						
						Pair<String, String> p = new Pair<String, String>(t, n);
						boolean found = false;
						for (int j = 0; j < varDecls.size(); j++) {
							Pair<String, String> pj = varDecls.elementAt(j);
							if (t.equals(pj.first) && n.equals(pj.second)) {
								found = true;
								break;
							}
						}
						if (!found) {
							varDecls.addElement(p);
						}
					}
				}
			}
		}
	}
	
	private boolean checkDoubleVarDecl(final Vector<Pair<String, String>> varDecls) {
		boolean result = true;
		// check: same variable name , different type :: should not happen!
		for (int j = 0; result && j < varDecls.size(); j++) {
			Pair<String, String> pj = varDecls.elementAt(j);
			for (int jj = j + 1; result && jj < varDecls.size(); jj++) {
				Pair<String, String> pjj = varDecls.elementAt(jj);
				if (pj.second.equals(pjj.second) && !pj.first.equals(pjj.first)) {
					if (!("Object".equals(pj.first) 
								|| "java.lang.Object".equals(pj.first))
							&& !("Object".equals(pjj.first) 
									|| "java.lang.Object".equals(pjj.first))) {
						this.errorMsg = "Variable has multiple declaration : ".concat(pj.second);
						result = false;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * All attributes of the elements of the RHS to create
	 * have to be set.
	 */
	private boolean checkAttributesOfNewObjects(final AttrVariableTuple avt) {
		return checkAttrsOfNewObjs(avt, this.itsImag.getNodesSet().iterator())
				&& checkAttrsOfNewObjs(avt, this.itsImag.getArcsSet().iterator());
	}
	
	private boolean checkAttrsOfNewObjs(
			final AttrVariableTuple avt,
			final Iterator<?> elems) {
		
		final boolean result = true;
		
		while (elems.hasNext()) {
			final GraphObject o = (GraphObject) elems.next();
			if (this.itsCodomObjects.contains(o)) {
				continue;
			}
			
			if (o.getAttribute() == null) {
				if ((o.getType().getAttrType() != null)
						&& (o.getType().getAttrType().getNumberOfEntries() != 0)) {
					this.errorMsg = "Type: <".concat(o.getType().getName()).concat(">  -  attribute not set.");
					return false;
				}
				continue;
			}

			final AttrInstance attr = o.getAttribute();
			
			ValueTuple typeObjectAttr = null;
			if (o instanceof Node) {
				final Node typeNode = o.getType().getTypeGraphNodeObject();
				if (typeNode != null) {
					typeObjectAttr = (ValueTuple) typeNode.getAttribute();
				}
			} else {
				final Arc typeEdge = o.getType().getTypeGraphArcObject(((Arc)o).getSourceType(), ((Arc)o).getTargetType());
				if (typeEdge != null) {
					typeObjectAttr = (ValueTuple) typeEdge.getAttribute();
				}
			}
						
			final ValueTuple vt = (ValueTuple) attr;
			for (int k = 0; k < vt.getSize(); k++) {
				final ValueMember vm = vt.getValueMemberAt(k);
				if (vm.isSet()) {
					if (vm.getExpr().isVariable()) {
						final VarMember var = avt.getVarMemberAt(vm.getExprAsText());
						if (var == null) {
							this.errorMsg = "Variable :  ";
							this.errorMsg = this.errorMsg.concat(vm.getExprAsText());
							this.errorMsg = this.errorMsg.concat("   is not declared.");
							return false;
						}
						if (!var.isInputParameter() && vm.getExpr() == null) {
							final Vector<String> 
							leftVars = this.itsOrig.getVariableNamesOfAttributes();
							if (!leftVars.contains(var.getName())) {
								this.errorMsg = "Variable :  ";
								this.errorMsg = this.errorMsg.concat(var.getName());
								this.errorMsg = this.errorMsg.concat("   in the RHS of the rule should be an input parameter");
								this.errorMsg = this.errorMsg.concat("\nor already declared in the LHS.");
								return false;
							}
						}
					} else if (vm.getExpr().isComplex()) {
						if (!vm.isValid()) {
							this.errorMsg = "Not all attributes of the RHS are correct.\nPlease check expression :  ";
							this.errorMsg = this.errorMsg.concat(vm.getExprAsText());
							return false;
						}
						final Vector<String> vec = vm.getAllVariableNamesOfExpression();
						final Vector<String> 
						vecLeft = this.itsOrig.getVariableNamesOfAttributes();
						for (int l = 0; l < vec.size(); l++) {
							final String n = vec.elementAt(l);
							boolean found = false;
							for (int ll = 0; ll < vecLeft.size(); ll++) {
								String nn = vecLeft.elementAt(ll);
								if (n.equals(nn)) {
									found = true;
								}
							}
							if (!found) {
								final VarMember m = avt.getVarMemberAt(n);
								if (m != null && !m.isSet() && !m.isInputParameter()) {
									this.errorMsg = "Not all attributes in the RHS of the rule are correct.";
									this.errorMsg = this.errorMsg.concat("\nPlease check variable :  ");
									this.errorMsg = this.errorMsg.concat(n);
									return false;
								}
							}
						}
					}
				} else //if (!getInverseImage(o).hasMoreElements()) 
				{
					// look for default attr value in the type graph
					boolean failed = true;
					if (typeObjectAttr != null) {
						final ValueMember tnvm = typeObjectAttr.getValueMemberAt(vm.getName());						
						if (tnvm != null && tnvm.isSet()) {
							vm.setExprAsText(tnvm.getExprAsText()); 
							if (vm.isSet()) {
								failed = false;
							}
						}
					}
					if (failed) {
						if (vm.getDeclaration().getType() == null) {
							vm.setExprAsText("null");
						} 
						else if (!this.getTypeSet().isEmptyAttrAllowed()) {					
							this.errorMsg = "Not all attributes in the RHS of the rule are set.";
							return false;
						}
					}
				}
			}
		}
		return result;
	}
	
	private boolean checkUsedVariables(
			final AttrVariableTuple avt,
			final Vector<Pair<String, String>>varDecls) {
		
		Vector<VarMember> used = new Vector<VarMember>(5);
		
		boolean result = true;
		for (int i = 0; i < varDecls.size(); i++) {
			final Pair<String, String> p = varDecls.elementAt(i);
			String typeName1 = p.first;
			
			boolean isClass1 = false;
			final String className1 = isClassName(typeName1);
			if (className1 != null) {
				isClass1 = true;
			}

			boolean isClass2 = false;
			String className2 = null;
			String typeName2 = "";
			final String varName = p.second;
			VarMember varm = ((VarTuple) avt).getVarMemberAt(varName);
			if (varm == null) {	
				className2 = isClassName(varName);
				if (className2 != null) {
					typeName2 = className2;
				}
			} else if (varm.getDeclaration() == null) {
				this.errorMsg = "Variable: ".concat(varName).concat("  isn't declared!");
				return false;
			} else {
				typeName2 = varm.getDeclaration().getTypeName();
				className2 = isClassName(typeName2);
			}
			
			if (className2 != null) {
				isClass2 = true;
			}

			if (className1 != null && className2 != null) {
				if (!className1.equals(className2)) {
					if (!className1.equals("java.lang.Object")
							&& !className2.equals("java.lang.Object")) {
						this.errorMsg = "Variable: " + varName
							+ "  has wrong type." + "\nIt should be :  "
							+ className1 + " .";
						return false;
					}
				} else if (!typeName1.equals(typeName2)) {
					final String 
					packageName = className1.substring(0, className1.lastIndexOf("."));
					if (packageName.equals("java.lang") && varm != null) {
						varm.getDeclaration().setType(typeName1);
					}
					else {
						this.errorMsg = "Variable: " + varName
							+ "  has wrong type." + "\nIt should be :  "
							+ typeName1 + " .";
						return false;
					}
				}
			} else if (!isClass1 && !isClass2 && !typeName1.equals(typeName2)) {
				this.errorMsg = "Variable: " + varName + "  has wrong type."
					+ "\nIt should be :  " + typeName1 + " .";
				return false;
			}
			used.add(varm);
		}
		
		this.deleteUnusedVars(used);
		
		return result;
	}
	
	private boolean markAttrConditions(
			final AttrVariableTuple avt,
			final AttrConditionTuple act) {
		
		boolean result = true;
		// check and mark the attr. conditions
		for (int k = 0; k < ((CondTuple) act).getSize(); k++) {
			final CondMember cm = ((CondTuple) act).getCondMemberAt(k);
			if (cm.getExpr() == null) {
				this.errorMsg = "Condition:  " + cm + "  is not defined.";
				return false;
			} else if (!cm.isValid()) {
				this.errorMsg = "Condition:  " + cm
						+ "  is not valid.\nPlease check variables of it.";
				return false;
			}
			
			final Vector<String> vars = cm.getAllVariables();
			if (!vars.isEmpty()) {
				boolean mixedNAC = false;
				boolean mixedPAC = false;
				boolean mixed = false;
				String name0 = vars.elementAt(0);
				if (((VarTuple) avt).isDeclared(name0)) {
					final VarMember var0 = avt.getVarMemberAt(vars.elementAt(0));
					int mark = var0.getMark();
					for (int j = 1; j < vars.size(); j++) {
						final String name = vars.elementAt(j);
						if (((VarTuple) avt).isDeclared(name)) {
							final VarMember var = avt.getVarMemberAt(name);
							if (mark == VarMember.LHS) {
								if (var.getMark() == VarMember.NAC) {
									mixedNAC = true;
								} else if (var.getMark() == VarMember.PAC) {
									mixedPAC = true;
								}
							} else if (mark == VarMember.NAC) {
								if (var.getMark() == VarMember.LHS) {
									mixedNAC = true;
								} else if (var.getMark() == VarMember.PAC) {
									mixed = true;
								}
							} else if (mark == VarMember.PAC) {
								if (var.getMark() == VarMember.LHS) {
									mixedPAC = true;
								} else if (var.getMark() == VarMember.NAC) {
									mixed = true;
								}
							} 
						} else {
							if(isClassName(name) == null) {
								this.errorMsg = "Variable: " + name
										+ "\nof condition: " + cm.getExprAsText()
										+ "\nis not declared.";
								return false;
							}
						}
					}
					if (mixedNAC && mixedPAC) {
						cm.setMark(CondMember.NAC_PAC_LHS);
					} else if (mixedNAC) {
						cm.setMark(CondMember.NAC_LHS);
					} else if (mixedPAC) {
						cm.setMark(CondMember.PAC_LHS);
					} else if (mixed) {
						cm.setMark(CondMember.NAC_PAC);
					} else if (mark == VarMember.NAC) {
						cm.setMark(CondMember.NAC);
					} else if (mark == VarMember.PAC) {
						cm.setMark(CondMember.PAC);
					} 
					else if (mark == VarMember.RHS) {
						cm.setMark(CondMember.RHS);
					} 
					else {
						cm.setMark(CondMember.LHS);
					}
				} else {				
					if(isClassName(name0) == null) {
						this.errorMsg = "Variable: " + name0 + "\nof condition: "
								+ cm.getExprAsText() + "\nis not declared.";
						return false;
					}
				}
			}
		}
		return result;
	}
	
	private void markUsedVariables(final AttrVariableTuple avt) {
		// mark used variables: 
		// inside RHS
		markUsedVars(this.itsImag.getNodesSet().iterator(), 
				this.itsImag.getArcsSet().iterator(),
				avt, VarMember.RHS); 

		// inside NACs	
		for (int l=0; l<this.itsNACs.size(); l++) {
			Graph g = this.itsNACs.get(l).getImage();
			markUsedVars(g.getNodesSet().iterator(), 
					g.getArcsSet().iterator(),
					avt, VarMember.NAC); 
		}
		// inside PACs	
		for (int l=0; l<this.itsPACs.size(); l++) {
			Graph g = this.itsPACs.get(l).getImage();
			markUsedVars(g.getNodesSet().iterator(), 
					g.getArcsSet().iterator(),
					avt, VarMember.PAC); 
			
		}
		// inside nested AC	
		markUsedVarsOfNestedACs(this.itsACs, avt);
//		for (int l=0; l<this.itsACs.size(); l++) {
//			Graph g = this.itsACs.get(l).getImage();
//			markUsedVars(g.getNodesSet().iterator(), 
//					g.getArcsSet().iterator(),
//					avt, VarMember.PAC); 
//			
//		}
		// finally inside LHS
		markUsedVars(this.itsOrig.getNodesSet().iterator(), 
				this.itsOrig.getArcsSet().iterator(),
				avt, VarMember.LHS); 
	}
	
	private void markUsedVarsOfNestedACs(List<?> nestedACs, AttrVariableTuple avt) {
		for (int i=0; i<nestedACs.size(); i++) {
			OrdinaryMorphism nestAC = (OrdinaryMorphism) nestedACs.get(i);
			Graph g = nestAC.getImage();
			markUsedVars(g.getNodesSet().iterator(), 
					g.getArcsSet().iterator(),
					avt, VarMember.PAC); 
			markUsedVarsOfNestedACs(((NestedApplCond)nestAC).getNestedACs(), avt);
		}
	}
	
	private void markUsedVars(
			final Iterator<Node> nodes, 
			final Iterator<Arc> arcs,
			final AttrVariableTuple avt,
			int mark) {
		while (nodes.hasNext()) {
			final GraphObject o = nodes.next();
			if (o.getAttribute() != null) {
				final ValueTuple vt = (ValueTuple) o.getAttribute();
				for (int k = 0; k < vt.getSize(); k++) {
					final ValueMember vm = vt.getValueMemberAt(k);
					if (vm.isSet()) {
						if (vm.getExpr().isVariable()) {					
							final VarMember var = avt.getVarMemberAt(vm.getExprAsText());
							if (var != null)
								var.setMark(mark);
						}
						else if (vm.getExpr().isComplex()) {
							Vector<String> vec = new Vector<String>(3);
							vm.getExpr().getAllVariables(vec);
							for (String s: vec) {
								VarMember var = avt.getVarMemberAt(s);
								if (var != null)
									var.setMark(mark);
							}
						}
					}
				}
			}
		}
		while (arcs.hasNext()) {
			final GraphObject o = arcs.next();
			if (o.getAttribute() != null) {
				final ValueTuple vt = (ValueTuple) o.getAttribute();
				for (int k = 0; k < vt.getSize(); k++) {
					final ValueMember vm = vt.getValueMemberAt(k);
					if (vm.isSet()) {
						if (vm.getExpr().isVariable()) {
							final VarMember var = avt.getVarMemberAt(vm.getExprAsText());
							if (var != null)
								var.setMark(mark);
						}
						else if (vm.getExpr().isComplex()) {
							Vector<String> vec = new Vector<String>(3);
							vm.getExpr().getAllVariables(vec);
							for (String s: vec) {
								VarMember var = avt.getVarMemberAt(s);
								if (var != null)
									var.setMark(mark);
							}
						}
					}
				}
			}
		}			
	}
	
	
	/**
	 * Prepares info about this rule: 
	 * node, edges to preserve, change, delete, create;
	 * types which should be checked due to node resp. edge type multiplicity.
	 * These infos can be called by methods: 
	 * getElementsToPreserve(), getElementsToChange(), getElementsToDelete(),
	 * getElementsToCreate(), getTypesWhichNeedMultiplicityCheck.
	 */
	public void prepareRuleInfo() {
		this.preserved = this.findElementsToPreserve();
		this.created = this.findElementsToCreate();
		this.deleted = this.findElementsToDelete();
		this.changedPreserved = this.findElementsToChange();
		this.typesWhichNeedMultiplicityCheck = this.findTypesWhichNeedMultiplicityCheck();
		this.hasEnabledGACs = this.hasEnabledACs(true);
		
		if ("true".equals(this.formStr))
			this.setDefaultFormulaTrue();
		else if ("false".equals(this.formStr))
			this.setDefaultFormulaFalse();				
	}

	/*
	 * Update infos about this rule (creating, deleting, changing) after
	 * the specified graph object of the LHS or RHS is removed.
	 * @param obj   removed object 
	 * @deprecated the update of rule infos is always done in the method
	 * 				this.isReadyToTransform()
	 */
	/*
	private void updateInfosAfterRemove(final GraphObject obj) {
		if (preserved != null && preserved.contains(obj)) {
			preserved.remove(obj);
		}
		else if (created != null && created.contains(obj)) {
			created.remove(obj);
		}
		else if (deleted != null && deleted.contains(obj)) {
			deleted.remove(obj);
		}
		if (changedPreserved != null && changedPreserved.contains(obj)) {
				changedPreserved.remove(obj);
		}
	}
*/
	/*
	private Class<?> getStaticClass(String name) {
		Class<?> klass = null;
		try {
			// check static class
			klass = Class.forName(name);
			System.out.println("Rule.getStaticClass:: for "+name+"  =>  Class "+klass);
			return klass;
		} catch (ClassNotFoundException cnfe) {
			System.out.println("Rule.getStaticClass:: ClassNotFoundException: "+cnfe.getMessage());	
			return null;
		}
	}
	*/
	
	/**
	 * Return true if its left and right graphs are empty
	 * and there aren't any application conditions,
	 * otherwise - false.
	 */
	public boolean isEmptyRule() {
		return (this.itsOrig.isEmpty() 
				&& this.itsImag.isEmpty()
				&& this.itsNACs.isEmpty() 
				&& this.itsPACs.isEmpty()
				&& this.itsACs.isEmpty());
	}
	
	public List<Type> getTypesOfLeftGraph() {
		final List<Type> list = new Vector<Type>();
		for (final Iterator<Node> en = getLeft().getNodesSet().iterator(); en.hasNext();) {
			final Node o = en.next();
			if (!list.contains(o.getType()))
				list.add(o.getType());
		}
		for (final Iterator<Arc> en = getLeft().getArcsSet().iterator(); en.hasNext();) {
			final Arc o = en.next();
			if (!list.contains(o.getType()))
				list.add(o.getType());
		}
		return list;
	}
	
	public List<Type> getTypeOfObjectToDelete() {
		final List<Type> list = new Vector<Type>();
		for (final Iterator<Node> en = getLeft().getNodesSet().iterator(); en.hasNext();) {
			final Node o = en.next();
			if (getImage(o) == null
					&& !list.contains(o.getType()))
				list.add(o.getType());
		}
		for (final Iterator<Arc> en = getLeft().getArcsSet().iterator(); en.hasNext();) {
			final Arc o = en.next();
			if (getImage(o) == null
					&& !list.contains(o.getType()))
				list.add(o.getType());
		}
		return list;
	}
	
	public List<Type> getTypeOfObjectToCreate() {
		final List<Type> list = new Vector<Type>();
		for (Iterator<Node> en = getRight().getNodesSet().iterator(); en.hasNext();) {
			GraphObject o = en.next();
			if (!getInverseImage(o).hasMoreElements()
					&& !list.contains(o.getType()))
				list.add(o.getType());
		}
		for (Iterator<Arc> en = getRight().getArcsSet().iterator(); en.hasNext();) {
			GraphObject o = en.next();
			if (!getInverseImage(o).hasMoreElements()
					&& !list.contains(o.getType()))
				list.add(o.getType());
		}
		return list;
	}
	
	/*
	 *
	 * @return
	 */
	public List<String> getTypesWhichNeedMultiplicityCheck() {
		if (this.typesWhichNeedMultiplicityCheck == null)			
			this.typesWhichNeedMultiplicityCheck = findTypesWhichNeedMultiplicityCheck();
		
		this.itsOrig.changed = false;
		this.itsImag.changed = false;

		return this.typesWhichNeedMultiplicityCheck;
	}
	
	private List<String> findTypesWhichNeedMultiplicityCheck() {
		final List<String> list = new Vector<String>();

		final List<GraphObject> list1 = new Vector<GraphObject>();
		list1.addAll(this.getElementsToCreate());
		list1.addAll(this.getElementsToDelete());
		for (int i=0; i<list1.size(); i++) {
			final GraphObject go = list1.get(i);
			final String typekey = go.convertToKey();
			if (!list.contains(typekey)) {
				if (go.isNode()) {
					int min = go.getType().getSourceMin();
					int max = go.getType().getSourceMax();
					if (min > 0 || max > 0) {						
						list.add(typekey);
						final List<Type> children = go.getType().getChildren();
						for (int ch=0; ch<children.size(); ch++) {
							list.add(children.get(ch).convertToKey());
						}
					}
				} else {
					int srcMin = go.getType().getSourceMin(((Arc)go).getSource().getType(),
								((Arc)go).getTarget().getType());
					int srcMax = go.getType().getSourceMax(((Arc)go).getSource().getType(),
								((Arc)go).getTarget().getType());
					int tarMin = go.getType().getTargetMin(((Arc)go).getSource().getType(),
								((Arc)go).getTarget().getType());
					int tarMax = go.getType().getTargetMax(((Arc)go).getSource().getType(),
								((Arc)go).getTarget().getType());
					if (srcMin > 0 || tarMin > 0 || srcMax > 0 || tarMax > 0) {
							list.add(typekey);
					}
				}
			}
		}		
		return list;
	}

	/**
	 * Returns true if this rule will create new graph elements,
	 * otherwise - false.
	 */
	public boolean isCreating() {
		// LHS graph size > rule mapping size
		this.isCreating = this.itsImag.getSize() > this.getCodomainSize();
		if (this.isCreating) {
			this.created = findElementsToCreate();
		}
		return this.isCreating;
	}

	/**
	 * @deprecated replaced by <code>Vector<GraphObject> getElementsToCreate()</code>
	 */
	public List<GraphObject> getObjectsToCreate() {
		return getElementsToCreate();
	}

	/**
	 * @deprecated use <code>getElementsToCreate().size()</code>
	 */
	public int getNumberOfObjectsToCreate() {
		return getElementsToCreate().size();
	}

	/**
	 * Returns true if this rule will delete some graph elements,
	 * otherwise - false.
	 */
	public boolean isDeleting() {
		// LHS graph size > rule mapping size
		this.isDeleting = this.itsOrig.getSize() > this.getDomainSize();
		if (this.isDeleting) {
			this.deleted = findElementsToDelete();			
		}
		return this.isDeleting;
	}
	
	public boolean isNodeDeleting() {
		this.isNodeDeleting = false;
		if (this.isDeleting) {
			for (final Iterator<Node> en = this.itsOrig.getNodesSet().iterator(); en.hasNext();) {
				if (getImage(en.next()) == null) {
					this.isNodeDeleting = true;
					break;
				}
			}
		}
		return this.isNodeDeleting;
	}

	/**
	 * Returns true if rule is deleting on nodes and after deleting a dangling-edge problem
	 * may occurred.
	 * 
	 * Otherwise returns false.
	 */
	public boolean mayCauseDanglingEdge() {	
		final List<Node> delNodes = this.findNodesToDelete();
		if (delNodes.isEmpty()) {
			return false;
		}
		
		boolean result = false;
		for (int i=0; i<delNodes.size() && !result; i++) {
			final Node n = delNodes.get(i);		
			final Vector<Arc> 
			inheritedArcs = this.getTypeSet().getInheritedArcs(n.getType());
			if (inheritedArcs.size() > 0) {	
				// TypeGraph exists and arcs at Node of type n.getType()
				for (int j=0; j<inheritedArcs.size() && !result; j++) {
					final Arc a = inheritedArcs.get(j);
					if (a.getSourceType().isParentOf(n.getType())) {
						int number = n.getNumberOfOutgoingArcsOfTypeToTargetType(a.getType(), a.getTarget().getType());
						if (number > 0) {
							int tarMax = a.getType().getTargetMax(a.getSource().getType(),
											a.getTarget().getType());
							if (tarMax != TypeSet.UNDEFINED
									&& number != tarMax) {
								result = true;		
							}							
						} 
						else if (!this.hasNacWhichForbidsArc(a, n))
							result = true;
//						else
//							result = true;
					}
					else if (a.getTargetType().isParentOf(n.getType())) {
						int number = n.getNumberOfIncomingArcsOfTypeFromSourceType(a.getType(), a.getSource().getType());
						if (number > 0 ) {
							int srcMax = a.getType().getSourceMax(a.getSource().getType(),
											a.getTarget().getType());
							if (srcMax != TypeSet.UNDEFINED
									&& number != srcMax) {
								result = true;
							}
						}
						else if (!this.hasNacWhichForbidsArc(a, n))
							result = true;
//						else
//							result = true;
					}
				}
			} 
		}
		return result;
	}
	
	private boolean hasNacWhichForbidsArc(Arc typeArc, Node lhsNode) {
		for (int i=0; i<this.itsNACs.size(); i++) {
			OrdinaryMorphism nac = this.itsNACs.get(i);
			if (!nac.isEnabled())
					continue;
			Iterator<Arc> arcs = nac.getTarget().getArcsCollection().iterator();
			while (arcs.hasNext()) {
				Arc a = arcs.next();
				if (!nac.getInverseImage(a).hasMoreElements()
						&& a.getType() == typeArc.getType()) {
					Node n = (Node)nac.getImage(lhsNode);
					if (n == a.getSource()) {
						return true;
					}
					else if (n == a.getTarget()) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean isArcDeleting() {
		if (this.isDeleting) {
			for (final Iterator<Arc> en = getLeft().getArcsSet().iterator(); en.hasNext();) {
				if (getImage(en.next()) == null) {
					return true;
				}
			}
		}
		return false;
	}
	
	/*
	 * Checks whether this rule deletes an edge of the given type 
	 * and the specified source and target nodes.
	 * The nodes must be contained in the LHS of this rule. 
	 */
	public boolean isArcDeleting(final Node src, final Type arct, final Node tar) {
		if (this.itsOrig.getNodesSet().contains(src)
				&& this.itsOrig.getNodesSet().contains(tar)) {			
			for (final Iterator<Arc> en = src.getOutgoingArcs(arct, tar.getType()).iterator(); en.hasNext();) {
				if (getImage(en.next()) == null) {
					return true;
				}
			}
		}
		return false;
	}
	
	/*
	 * Checks whether this rule deletes the specified edge.
	 * The edge must be contained in the LHS of this rule. 
	 */
	public boolean isArcDeleting(final Arc a) {
		if (this.itsOrig.getArcsSet().contains(a)
				&& this.getImage(a) == null) {			
			return true;
		}
		return false;
	}
	
	/*
	 * Checks whether this rule creates an edge of the given type 
	 * and the specified source and target nodes.
	 * The nodes must be contained in the LHS of this rule. 
	 */
	public boolean isArcCreating(final Node src, final Type arct, final Node tar) {
		if (this.isCreating
				&& this.itsOrig.getNodesSet().contains(src)
				&& this.itsOrig.getNodesSet().contains(tar)) {
			for (final Iterator<Arc> en = this.itsImag.getArcsSet().iterator(); en.hasNext();) {
				Arc a = en.next();
				if (a.getType().compareTo(arct)
						&& !this.getInverseImage(a).hasMoreElements()) {
					List<GraphObject> inv1 = this.getInverseImageList(a.getSource());
					if (inv1.contains(src)) {
						List<GraphObject> inv2 = this.getInverseImageList(a.getTarget());
						if (inv2.contains(tar)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	/*
	 * Checks whether this rule creates the specified edge.
	 * The edge must be contained in the RHS of this rule. 
	 * The source and target nodes must be preserved by this rule. 
	 */
	public boolean isArcCreating(final Arc a) {
		if (//this.itsImag.getArcsSet().contains(a) &&
				!this.getInverseImage(a).hasMoreElements()
				&& this.getInverseImage(a.getSource()).hasMoreElements()
				&& this.getInverseImage(a.getTarget()).hasMoreElements()) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * @deprecated replaced by <code>Vector<GraphObject> getElementsToDelete()</code>
	 */
	public List<GraphObject> getObjectsToDelete() {
		return getElementsToDelete();
	}

	/**
	 * @deprecated use <code>getElementsToDelete().size()</code>
	 */
	public int getNumberOfObjectsToDelete() {
		return getElementsToDelete().size();
	}
		
	/**
	 * Returns elements of the LHS which should be preserved.
	 */
	public List<GraphObject> getElementsToPreserve() {
		if (this.preserved == null
				|| this.itsImag.changed) {
			this.preserved = findElementsToPreserve();
		}
		return this.preserved;
	}

	/**
	 * Returns elements of the RHS to create.
	 */
	public List<GraphObject> getElementsToCreate() {
		if (this.created == null
				|| this.itsImag.changed) { 
			this.created = findElementsToCreate();
		}		
		return this.created;		
	}

	/**
	 * Returns elements of the LHS to delete.
	 */
	public List<GraphObject> getElementsToDelete() {
		if (this.deleted == null
				|| this.itsOrig.changed) {
			this.deleted = findElementsToDelete();
		}
		return this.deleted;
	}
	
	/**
	 * Returns preserved elements which attributes should be changed. 
	 * The key is an object of the LHS, the value - its image of the RHS.
	 */
	public Hashtable<GraphObject, GraphObject> getElementsToChange() {
		if (this.changedPreserved == null
				|| this.itsOrig.changed)
			this.changedPreserved = findElementsToChange();
			
		return this.changedPreserved;		
	}

	private List<GraphObject> findElementsToPreserve() {
		List<GraphObject> vec = new Vector<GraphObject>();
		vec.addAll(this.itsDomObjects);
		return vec;
	}

	private List<GraphObject> findElementsToCreate() {
		List<GraphObject> vec = new Vector<GraphObject>();
		vec.addAll(this.findNodesToCreate());
		vec.addAll(this.findArcsToCreate());

		this.isCreating = !vec.isEmpty();
		return vec;
	}

	private List<Node> findNodesToCreate() {
		List<Node> vec = new Vector<Node>();
		
		for (Iterator<Node> en = getRight().getNodesSet().iterator(); en.hasNext();) {
			Node o = en.next();
			if (!getInverseImage(o).hasMoreElements())
				vec.add(o);
		}
		return vec;
	}
	
	private List<Arc> findArcsToCreate() {
		List<Arc> vec = new Vector<Arc>();
		
		for (Iterator<Arc> en = getRight().getArcsSet().iterator(); en.hasNext();) {
			Arc o = en.next();
			if (!getInverseImage(o).hasMoreElements())
				vec.add(o);
		}
		return vec;
	}
	
	private List<GraphObject> findElementsToDelete() {
		final List<GraphObject> vec = new Vector<GraphObject>();		
		vec.addAll(findNodesToDelete());		
		vec.addAll(findArcsToDelete());		
		return vec;
	}

	private List<Node> findNodesToDelete() {
		final List<Node> vec = new Vector<Node>();
		for (final Iterator<Node> en = getLeft().getNodesSet().iterator(); en.hasNext();) {
			final Node o = en.next();
			if (getImage(o) == null)
				vec.add(o);
		}
		this.isDeleting = !vec.isEmpty();
		this.isNodeDeleting = this.isDeleting;
		
		return vec;
	}
	
	private Vector<Arc> findArcsToDelete() {
		final Vector<Arc> vec = new Vector<Arc>();
		for (final Iterator<Arc> en = getLeft().getArcsSet().iterator(); en.hasNext();) {
			final Arc o = en.next();
			if (getImage(o) == null)
				vec.addElement(o);
		}
		this.isDeleting = this.isDeleting || !vec.isEmpty();
		return vec;
	}
	
	/**
	 * Returns true if this rule will change some attributes of the graph elements,
	 * otherwise - false.
	 */
	public boolean isChanging() {
		for (int i=0; i<this.itsDomObjects.size(); i++) {
			GraphObject go = this.itsDomObjects.get(i);
			if (isChangingAttribute(go, getImage(go))) {
				this.isChanging = true;
				break;
			}
		}
		return this.isChanging;
	}

	/*
	 * Returns preserved graph objects to be changed. The key is a graph object
	 * of the LHS, the value is its image object of the RHS
	 */
	private Hashtable<GraphObject, GraphObject> findElementsToChange() {
		Hashtable<GraphObject, GraphObject> set = new Hashtable<GraphObject, GraphObject>();
		for (int i=0; i<this.itsDomObjects.size(); i++) {
			GraphObject go = this.itsDomObjects.get(i);
			if (isChangingAttribute(go, getImage(go))) {
				set.put(go, getImage(go));
			}
		}
		this.isChanging = !set.isEmpty();
		return set;
	}

	private boolean isChangingAttribute(GraphObject obj,
			GraphObject img) {
		if (img.getAttribute() == null
				|| img.getAttribute().getNumberOfEntries() == 0)
			return false;

		ValueTuple vtObj = (ValueTuple) obj.getAttribute();
		ValueTuple vtImg = (ValueTuple) img.getAttribute();
		for (int i = 0; i < vtObj.getNumberOfEntries(); i++) {
			ValueMember vmObj = vtObj.getValueMemberAt(i);
			ValueMember vmImg = vtImg.getValueMemberAt(vmObj.getName());
			if (vmImg != null && vmImg.isSet()) {
				if (!vmObj.isSet())
					return true;
				else if (!vmImg.getExprAsText().equals(vmObj.getExprAsText()))
					return true;
			}
		}
		return false;
	}

	/*
	 * protected boolean differentVariablesUsed(Graph g, Enumeration nacs) {
	 * AttrContext ac = getAttrContext(); VarTuple avt = (VarTuple)
	 * ac.getVariables(); Hashtable<VarMember,Boolean> used = new Hashtable<VarMember,Boolean>(avt.getSize());
	 * for (int i = 0; i < avt.getSize(); i++) { VarMember var =
	 * avt.getVarMemberAt(i); used.put(var, Boolean.valueOf(false)); } Enumeration e =
	 * g.getElements(); while (nacs.hasMoreElements()) { used = new Hashtable<VarMember,Boolean>(avt.getSize());
	 * for (int i = 0; i < avt.getSize(); i++) { VarMember var =
	 * avt.getVarMemberAt(i); used.put(var, Boolean.valueOf(false)); }
	 * OrdinaryMorphism m = (OrdinaryMorphism) nacs.nextElement(); e =
	 * m.getTarget().getElements(); while (e.hasMoreElements()) { GraphObject o =
	 * (GraphObject) e.nextElement(); if(o.getAttribute() == null) continue;
	 * AttrInstance attr = o.getAttribute(); ValueTuple vt = (ValueTuple) attr;
	 * if (m.getInverseImage(o).hasMoreElements()) { GraphObject orig =
	 * (GraphObject) m.getInverseImage(o).nextElement(); ValueTuple vtOrig =
	 * (ValueTuple) orig.getAttribute(); for (int k = 0; k < vt.getSize(); k++) {
	 * ValueMember vm = vt.getValueMemberAt(k); ValueMember vmOrig =
	 * vtOrig.getValueMemberAt(k); if (vmOrig.isSet() && vm.isSet()) { if
	 * (vmOrig.getExpr().isVariable() && vm.getExpr().isVariable()) { //
	 * System.out.println(vm.getExpr()); if
	 * (!vmOrig.getExprAsText().equals(vm.getExprAsText())) return false; } } } } } }
	 * return true; }
	 * 
	 * 
	 * protected boolean areSameVariablesUsed() { AttrContext ac =
	 * getAttrContext(); VarTuple avt = (VarTuple) ac.getVariables(); Hashtable<VarMember,Boolean>
	 * used = new Hashtable<VarMember,Boolean>(avt.getSize()); for (int i = 0;
	 * i < avt.getSize(); i++) { VarMember var = avt.getVarMemberAt(i);
	 * used.put(var, Boolean.valueOf(false)); } Vector<VarMember> result = new
	 * Vector<VarMember>(); Enumeration e = getLeft().getElements(); while
	 * (e.hasMoreElements()) { GraphObject o = (GraphObject) e.nextElement();
	 * if(o.getAttribute() == null) continue; AttrInstance attr =
	 * o.getAttribute(); ValueTuple vt = (ValueTuple) attr; for (int k = 0; k <
	 * vt.getSize(); k++) { ValueMember vm = vt.getValueMemberAt(k); if
	 * (vm.isSet()) { if (vm.getExpr().isVariable()) { //
	 * System.out.println(vm.getExpr()); VarMember var =
	 * avt.getVarMemberAt(vm.getExprAsText()); if (((Boolean)
	 * used.get(var)).booleanValue() == false) used.put(var, Boolean.valueOf(true));
	 * else { if (!result.contains(var)) result.add(var); } } } } } if
	 * (result.size() != 0) return true; else return false; }
	 */

	/**
	 * Restores variable declarations of the RHS, NACs and PACs. 
	 * The reason is: the variables declarations can be lost after a step.
	 * Before the next application of this rule can be done 
	 * the lost variable declarations have to be restored.
	 * This method is called during Critical Pair Analysis.
	 */
	protected void restoreVariableDeclaration() {
		VarTuple vart = (VarTuple) getAttrContext().getVariables();

		if (this.itsImag.isAttributed()) {
			// check vars of RHS
			this.restoreVarDecl(this.itsImag, vart);
		}
		// check vars of NACs
		for (int l=0; l<this.itsNACs.size(); l++) {
			OrdinaryMorphism nac = this.itsNACs.get(l);
			if (nac.getImage().isAttributed()) {
				this.restoreVarDecl(nac.getImage(), vart);
			}
		}
		// check vars of PACs
		for (int l=0; l<this.itsPACs.size(); l++) {
			OrdinaryMorphism pac = this.itsPACs.get(l);
			if (pac.getImage().isAttributed()) {
				this.restoreVarDecl(pac.getImage(), vart);
			}
		}
		// check vars of nested ACs
		for (int l=0; l<this.itsACs.size(); l++) {
			OrdinaryMorphism ac = this.itsACs.get(l);
			if (ac.getImage().isAttributed()) {
				this.restoreVarDecl(ac.getImage(), vart);
			}
		}
	}

	private void restoreVarDecl(final Graph g, final VarTuple vart) {
		Enumeration<GraphObject> en1 = g.getElements();
		while (en1.hasMoreElements()) {
			GraphObject o = en1.nextElement();
			if (o.getAttribute() == null)
				continue;
			AttrInstance attr = o.getAttribute();
			ValueTuple vt = (ValueTuple) attr;
			for (int k = 0; k < vt.getSize(); k++) {
				ValueMember vm = vt.getValueMemberAt(k);
				if (vm.isSet() && vm.getExpr().isVariable()) {
					String n = vm.getExprAsText();
					String t = vm.getDeclaration().getTypeName();
					VarMember varm = vart.getVarMemberAt(n);
					String t_varm = "";
					if (varm != null) 
						t_varm = varm.getDeclaration().getTypeName();
					
					if (varm == null || !t.equals(t_varm)) {
						vart.declare(vm.getDeclaration().getHandler(),
										t, n);
						((VarMember) vart.getMemberAt(n))
								.setTransient(true);
					}
				}
			}
		}	
	}
	
	public Vector<Type> getUsedTypes() {
		// get types of LHS and RHS
		final Vector<Type> vec = super.getUsedTypes();
		// add types of NACs
		for (int i = 0; i < this.itsNACs.size(); i++) {
			OrdinaryMorphism om = this.itsNACs.get(i);
			addUsedTypes(om.getTarget(), vec);
		}
		// add types of PACs
		for (int i = 0; i < this.itsPACs.size(); i++) {
			OrdinaryMorphism om = this.itsPACs.get(i);
			addUsedTypes(om.getTarget(), vec);
		}
		// add types of nested ACs
		for (int i = 0; i < this.itsACs.size(); i++) {
			OrdinaryMorphism om = this.itsACs.get(i);
			addUsedTypes(om.getTarget(), vec);
		}
		return vec;
	}

	private void addUsedTypes(final Graph g, final List<Type> vec) {
		Iterator<Node> nodes= g.getNodesSet().iterator();
		while (nodes.hasNext()) {
			GraphObject o = nodes.next();
			if (!vec.contains(o.getType()))
				vec.add(o.getType());
		}
		Iterator<Arc> arcs= g.getArcsSet().iterator();
		while (arcs.hasNext()) {
			GraphObject o = arcs.next();
			if (!vec.contains(o.getType()))
				vec.add(o.getType());
		}
	}
	
	/**
	 * Returns error message if this rule is not ready to transform.
	 * 
	 * @see agg.xt_basis.Rule#isReadyToTransform().
	 */
	public String getErrorMsg() {
		return this.errorMsg;
	}

	/**
	 * Returns true if this rule can make a match basically. 
	 * It works for INJECTIVE matching, only.
	 */
	public boolean canMatch(Graph g, MorphCompletionStrategy strategy) {
		// check graph size if injective morphism
		if (strategy.getProperties().get(CompletionPropertyBits.INJECTIVE)) {
			if ((getLeft().getNodesCount() > g.getNodesCount())
					|| (getLeft().getArcsCount() > g.getArcsCount()))
				return false;
		}
		// check types: all types of the orig. graph should be in image, too
		Vector<Type> origTypes = getLeft().getUsedTypes();
		
		// TODO::mit PACs  origTypes erweitern
		
		Vector<Type> otherTypes = g.getUsedAndInheritedTypes();
		for (int i = 0; i < origTypes.size(); i++) {
			if (!otherTypes.contains(origTypes.get(i)))
				return false;
		}
		return true;
	}

	/**
	 * Set its match to the specified parameter.
	 */
	public void setMatch(Match m) {
		this.itsMatch = m;
	}

	/**
	 * Reset target graph of its match, if it exists.
	 */
	public void resetTargetOfMatch(Graph g) {
		if (this.itsMatch != null) {
			this.itsMatch.setTarget(g);
		}
	}

	public void setParallelMatchingEnabled(boolean b) {
		this.parallelMatching = b;
	}

	public boolean isParallelApplyEnabled() {
		return this.parallelMatching;
	}

	public void setRandomizedCSPDomain(boolean b) {
		this.randomCSPDomain = b;
	}
	
	public boolean isRandomizedCSPDomain() {
		return this.randomCSPDomain;
	}
	
	/**
	 * Allows to define the CSP solver has to do 
	 * next match completion starting always by first CSP variable.<br>
	 * This works for parallel match only. 
	 * The method <code>setParallelMatchingEnabled(true)</code> should be called before.
	 */
	public void setStartParallelMatchingByFirst(boolean b) {
		this.startParallelMatchByFirstCSPVar = b;
	}
	
	/**
	 * Set value of the input parameter of its attribute context.
	 * The specified parameters contain:
	 * String - is a name of an input parameter,
	 * first Object of a Vector - is the value,
	 * second Object of a Vector - is the type of this input parameter.
	 */
	public void setInputParameters(HashMap<String, Vector<Object>> parameters) {
		VarTuple var = (VarTuple) getAttrContext().getVariables();
		int j = 0;
		for (int i = 0; i < var.getNumberOfEntries(); i++) {
			VarMember varm = var.getVarMemberAt(i);
			if (varm.isInputParameter()) {
				Vector<Object> valuePair = parameters.get(varm.getName());
				Object value = valuePair.get(0);
				String type = (String) valuePair.get(1);
				if (type.equals("int") || type.equals("boolean")
						|| type.equals("float") || type.equals("double")
						|| type.equals("short") || type.equals("long")) {
					varm.setExprAsEvaluatedText(value.toString());
				}
				else {
					varm.setExprAsObject(value);
				}
				j++;
			}
			if (j > parameters.size()) {
				break;
			}
		}
	}

	protected boolean evalDefaultFormula() {
		if (this.itsMatch == null) 
			return false;
		
		if (this.itsACs.size() == 0) {
			return true;
		}
		
		int n = this.itsACs.size();
		final List<Evaluable> vars = new Vector<Evaluable>(n);
		
		String tmp = "";
		int indx = -1;
		for (int i=0; i<this.itsACs.size(); i++) {	
			NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
			if (ac.isEnabled()) {
				indx++;
				ac.setRelatedMorphism(this.itsMatch);
				vars.add(ac);
								
				if (indx == 0) {
					if (this.formStr.equals("false"))
						tmp = tmp.concat("!".concat(String.valueOf(vars.size())));
					else
						tmp = tmp.concat(String.valueOf(vars.size()));
				}
				else {
					if (this.formStr.equals("false"))
						tmp = tmp.concat("&!").concat(String.valueOf(vars.size()));
					else
						tmp = tmp.concat("&").concat(String.valueOf(vars.size()));
				}
			}
		}
//		System.out.println("Test formula of (nested) appl conds:  " + tmp);
		
		boolean res = this.itsFormula.setFormula(vars, tmp)
						&& this.itsFormula.eval(this.itsMatch.getImage());
		if (!res) {
			this.itsMatch.setErrorMsg("Formula:  " + tmp + "  is violated!");
		}
		return res;
	}
	
	public boolean setDefaultFormulaTrue() {		
		if (this.itsACs.size() == 0) {
			this.formStr = "true";
			this.formReadStr = "true";
			return true;
		}		
		
		final List<Evaluable> vars = new Vector<Evaluable>(this.itsACs.size());
		for (int i=0; i<this.itsACs.size(); i++) {
			NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
			if (ac.isEnabled()) {
				vars.add(ac);								
			}
		}
		String tmp = "";
		for (int i=0; i<vars.size(); i++) {
			String tmp1 = (i == 0)? tmp.concat(String.valueOf(i+1)):
							tmp.concat("&").concat(String.valueOf(i+1));
			tmp = tmp1;
		}
		if ("".equals(tmp)) {
			this.formStr = "true";
			this.formReadStr = "true";
			return true;
		}
		
		if (this.itsFormula.setFormula(vars, tmp)) {
			this.formStr = this.itsFormula.getAsString(vars);
			this.formReadStr = this.itsFormula.getAsString(vars, this.getNameOfEnabledACs());
//			System.out.println(this.formReadStr);
//			this.setTextualComment("Formula: ".concat(this.formReadStr));
			return true;
		}
		return false;
	}		
	
	public boolean setDefaultFormulaFalse() {
		if (this.itsACs.size() == 0) {
			this.formStr = "true";
			this.formReadStr = "true";
			return true;
		}
		
		final List<Evaluable> vars = new Vector<Evaluable>(this.itsACs.size());
		for (int i=0; i<this.itsACs.size(); i++) {
			NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
			if (ac.isEnabled()) {
				vars.add(ac);								
			}
		}
		String tmp = "";
		for (int i=0; i<vars.size(); i++) {
			String tmp1 = (i == 0)? tmp.concat(String.valueOf(i+1)):
							tmp.concat("&").concat(String.valueOf(i+1));
			tmp = tmp1;
		}
		
		if ("".equals(tmp)) {
			this.formStr = "true";
			this.formReadStr = "true";
			return true;
		} else {
			tmp ="!(".concat(tmp).concat(")");
		}
		
		if (this.itsFormula.setFormula(vars, tmp)) {
			this.formStr = this.itsFormula.getAsString(vars);
			this.formReadStr = this.itsFormula.getAsString(vars, this.getNameOfEnabledACs());
//			System.out.println(this.formReadStr);
//			this.setTextualComment("Formula: ".concat(this.formReadStr));
			return true;
		}
		return false;
	}
	
	public boolean evalFormula() {
		boolean result = true;
		if (this.itsMatch != null && this.itsACs.size() != 0) {
			for (int i=0; i<this.itsACs.size(); i++) {
				NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
				if (ac.isEnabled()) {
					ac.setRelatedMorphism(this.itsMatch);
				}
			}

			if (this.formStr.equals("true")) 
				this.setDefaultFormulaTrue();
			else if (this.formStr.equals("false")) 
				this.setDefaultFormulaFalse();
							
			result = this.itsFormula.eval(this.itsMatch.getImage());		
			
			if (!result) { 
				this.itsMatch.setErrorMsg("Formula:  " + this.formReadStr + "  is violated!");
			}
			
			this.disposeResultsOfNestedACs();
			
			return result;
		} 
		else
			return true;
	}
	
	public void setFormula(Formula f) {
//		this.itsFormula = f;
//		this.formulaStr = this.itsFormula.getAsString(this.getEnabledGeneralACsAsEvaluable());
//		this.setTextualComment("Formula: ".concat(this.formulaStr));
		
		this.setFormula(f.getAsString(this.getEnabledGeneralACsAsEvaluable()), this.getEnabledACs());
	}
	
	/**
	 * Set a boolean formula represented by the specified bnf string
	 * above nested application conditions.
	 * @param bnf
	 */
	public boolean setFormula(String bnf) {
//		final List<NestedApplCond> vars = new Vector<NestedApplCond>(this.itsACs.size());
//		for (int i=0; i<this.itsACs.size(); i++) {	
//			vars.add((NestedApplCond) this.itsACs.get(i));
//		}		
		return this.setFormula(bnf, this.getEnabledACs());
	}
	
	/**
	 * Set a boolean formula represented by the specified bnf string
	 * above nested application conditions.
	 * @param bnf
	 */
	public boolean setFormula(String bnf, final List<NestedApplCond> list) {
		if (bnf.equals("true")) {
//			this.formStr = bnf;
//			this.formReadStr = bnf;
//			return true;
			return this.setDefaultFormulaTrue();
		}
		else if (bnf.equals("false")) {
			return this.setDefaultFormulaFalse();
		}
		
		final List<Evaluable> vars = new Vector<Evaluable>();
		for (int i=0; i<list.size(); i++) {	
			NestedApplCond ac = list.get(i);
			if (ac.isEnabled()) {
				vars.add(ac);
			}
		}
		if (vars.isEmpty()) {
			this.formStr = "true";
			this.formReadStr = "true";
			return true;
		}	
		if (this.itsFormula.setFormula(vars, bnf)) {
			this.formStr = this.itsFormula.getAsString(vars);
			this.formReadStr = this.itsFormula.getAsString(vars, this.getNameOfEnabledACs());
//			System.out.println(this.formReadStr);
			this.setTextualComment("Formula: ".concat(this.formReadStr));
			return true;
		} else
			return false;
	}
	
	public boolean refreshFormula(final List<Evaluable> vars) {
		String bnf = this.formStr;
		if (this.itsFormula.setFormula(vars, bnf)) {
			this.formStr = this.itsFormula.getAsString(vars);
			this.formReadStr = this.itsFormula.getAsString(vars, this.getNameOfEnabledACs());
			this.setTextualComment("Formula: ".concat(this.formReadStr));
			return true;
		}
		else {
			this.formStr = "true";
			this.formReadStr = "true";
		}
		return false;
	}
	
	/**
	 * Returns the formula string as internal represantation 
	 * like this: (1&2).>br>
	 * This method shoud be used for all actions relationg to Formula objects.
	 */
	public String getFormulaStr() {
		return this.formStr;
	}
	
	/**
	 * Returns the formula string as readable represantation 
	 * like this: (nameOf1 & nameOf2).<br>
	 * This method shoud be used for messages.
	 */
	public String getFormulaText() {
		return this.formReadStr;
	}
	
	public Formula getFormula() {
		return this.itsFormula;
	}
	
	/**
	 * Returns true, if it contains enabled nested application conditions.
	 */
	public boolean hasEnabledACs(boolean checkBefore) {
		if (checkBefore) {
			this.hasEnabledGACs = false;
			for (int i=0; i<this.itsACs.size(); i++) {		
				NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
				if (ac.isEnabled()) {
					this.hasEnabledGACs = true;
					break;
				}
			}
		}
		return this.hasEnabledGACs;
	}
	
	/**
	 * Returns a list with names of enabled general application conditions. 
	 */
	public List<String> getNameOfEnabledACs() {
		final List<String> vars = new Vector<String>();
		for (int i=0; i<this.itsACs.size(); i++) {		
			NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
			if (ac.isEnabled())
				vars.add(ac.getName());
		}
		return vars;
	}
	
	/**
	 * Returns a list with names of enabled general application conditions
	 * and its nested ACs inclusively. 
	 */
	public List<String> getNameOfEnabledNestedACs() {
		final List<String> vars = new Vector<String>();
		for (int i=0; i<this.itsACs.size(); i++) {		
			NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
			if (ac.isEnabled())
				vars.add(ac.getName());
			vars.addAll(ac.getNameOfEnabledNestedACs());
		}
		return vars;
	}
	
	/**
	 * Returns a list with names of all general application conditions
	 * and its nested ACs inclusively. 
	 */
	public List<String> getNameOfNestedACs() {
		final List<String> vars = new Vector<String>();
		for (int i=0; i<this.itsACs.size(); i++) {		
			NestedApplCond ac = (NestedApplCond) this.itsACs.get(i);
			vars.add(ac.getName());
			vars.addAll(ac.getNameOfEnabledNestedACs());
		}
		return vars;
	}
	
	/**
	 * Makes the minimal rule from the given rule.
	 * A minimal rule comprises the effects of a given rule in a minimal context.
	 */
	public Rule getMinimalRule() {
		return BaseFactory.theBaseFactory.makeMinimalOfRule(this);
	}
	
	/**
	 * Returns an inverse construction of this rule.<br>
	 * This rule has to be injective, otherwise returns null.<br>
	 * <br>
	 * Note: This method is mainly used during critical pair analysis. 
	 */
	public InverseRuleConstructData getInverseConstructData() {
		if (this.isInjective()) {
			if (this.invConstruct == null) {
				this.invConstruct = new InverseRuleConstructData(this);
			}
			return this.invConstruct;
		}
		return null;
	}
	
	/**
	 * This method does not destroy the Rule and OrdinaryMorphism instances of the inverse construction.
	 * They must be disposed by the user object explicitly. 
	 * The local pair references set to null, only.
	 */
	public void disposeInverseConstruct() {
		if (this.invConstruct != null) {
			this.invConstruct.dispose();
			this.invConstruct = null;
		}
	}
	
	/**
	 * Destroys the Rule and OrdinaryMorphism instances of the inverse construction.
	 * The local pair references set to null.
	 */
	public void destroyInverseConstruct() {
		if (this.invConstruct != null) {
			this.invConstruct.destroy();
			this.invConstruct = null;
		}
	}
	
	public void initSignatur() {
		((VarTuple)this.getAttrContext().getVariables()).initSignaturOrder();		
	}
	
	public void disposeSignatur() {
		((VarTuple)this.getAttrContext().getVariables()).disposeSignaturOrder();	
	}
	
	public List<Integer> getSignaturOrder() {
		return ((VarTuple)this.getAttrContext().getVariables()).getSignaturOrder();
	}
	
	public String getSignatur() {
		VarTuple vars = (VarTuple)this.getAttrContext().getVariables();
		String s = this.getName().concat("(");
		String s1 = "";
		List<Integer> order = vars.getSignaturOrder();
		for (int i = 0; i < order.size(); i++) {
			VarMember m = (VarMember) vars.getMemberAt(order.get(i).intValue());
			String nt = m.getName().concat(":").concat(m.getDeclaration().getTypeName());
			s1 = s1.concat(nt);
			if (i < (order.size()-1))
				s1 = s1.concat(", ");
		}
		String s2 = "";
		for (int i = 0; i < vars.getSize(); i++) {
			VarMember m = (VarMember) vars.getMemberAt(i);
			if (m.isOutputParameter()) {
				if (!s1.isEmpty())
					s2 = s2.concat(", ");			
				s2 = s2.concat("out ");
				String nt = m.getName().concat(":").concat(m.getDeclaration().getTypeName());
				s2 = s2.concat(nt);
				break;
			}
		}
		s = s.concat(s1).concat(s2);
		s = s.concat(")");
		return s;
	}
	
	public void addInToSignatur(int indxOfVar) {
		((VarTuple)this.getAttrContext().getVariables()).addToSignaturOrder(indxOfVar);
	}
	
	public void removeInFromSignatur(int indxOfVar) {
		((VarTuple)this.getAttrContext().getVariables()).removeFromSignaturOrder(indxOfVar);
	}
	
	public void addOutToSignatur(int indxOfVar) {
		VarTuple vars = (VarTuple)this.getAttrContext().getVariables();
		for (int i = 0; i < vars.getSize(); i++) {
			VarMember m = (VarMember) vars.getMemberAt(i);
			if (i == indxOfVar)
				m.setOutputParameter(true);
			else
				m.setOutputParameter(false);
		}				
	}
	
	public void removeOutFromSignatur(int indxOfVar) {
		VarTuple vars = (VarTuple)this.getAttrContext().getVariables();
		VarMember m = (VarMember) vars.getMemberAt(indxOfVar);
		if (m != null)
			m.setOutputParameter(false);
	}
}

