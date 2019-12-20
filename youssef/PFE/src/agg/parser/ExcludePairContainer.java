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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import agg.xt_basis.BadMappingException;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.CompletionStrategySelector;
import agg.xt_basis.Completion_InjCSP;
import agg.xt_basis.GraGra;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.MorphCompletionStrategy;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.Node;
import agg.xt_basis.Arc;
import agg.xt_basis.TestStep;
import agg.xt_basis.TypeException;
import agg.xt_basis.Match;
import agg.attribute.impl.ContextView;
import agg.attribute.impl.ValueMember;
import agg.util.XMLHelper;
import agg.util.Pair;

// ****************************************************************************+
/**
 * This Container contains only conflict free and exclude relations.
 * 
 * @author $Author: olga $
 * @version $Id: ExcludePairContainer.java,v 1.92 2010/12/21 16:33:19 olga Exp $
 */
public class ExcludePairContainer implements PairContainer, Runnable {

	public class Entry {
		public static final int NOT_SET = 0; // status|state

		public static final int SCHEDULED_FOR_COMPUTING = 1; // state

		public static final int COMPUTING_IS_RUNNING = 2; // state

		public static final int COMPUTED = 3; // state
		
		public static final int COMPUTED2 = 31; // state
		
		public static final int COMPUTED12 = 32; // state		
		
		public static final int NOT_RELATED = 4; // state

		public static final int DISABLED = 5; // state
		
		public static final int NON_RELEVANT = 6; // status
		
		public static final int NOT_COMPUTABLE = 7; // status
		
		public static final int NOT_COMPLETE_COMPUTABLE = 8; // status

		boolean isCritical = false;

		boolean isRelationVisible = true;

		boolean isRuleVisible = true;

		int state = NOT_SET;

		int status = NOT_SET;
		
		int duIndx=-1, pfIndx=-1, caIndx=-1; 		
		String duIndxStr="-1:", pfIndxStr="-1:-1:", caIndxStr="-1:"; 
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlapping;
		
		
		public boolean isCritical() {
			return this.isCritical;
		}

		public void setState(int s) {
			if (s == 1 || s == 2 || s == 3 || s == 31 || s == 32)
				this.state = s;
			else
				this.state = 0;
		}
		
		public int getState() {
			return this.state;
		}

		public void setStatus(int s) {
			this.status = s;
		}
		
		public int getStatus() {
			return this.status;
		}
		
		public Vector<?> getOverlapping() {
			return this.overlapping;
		}

		public void setOverlapping(Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> v) {
			this.overlapping.addAll(v);
		}
		
		public boolean isRuleVisible() {
			return this.isRuleVisible;
		}

		public boolean isRelationVisible() {
			return this.isRelationVisible;
		}

		public void setProgressIndx(final ExcludePair p) {
			this.duIndxStr = p.duIndxStr;
			this.pfIndxStr = p.pfIndxStr;
			this.caIndxStr = p.caIndxStr;
			try {
				this.duIndx = Integer.valueOf(this.duIndxStr.split(":")[0]).intValue();
				this.pfIndx = Integer.valueOf(this.pfIndxStr.split(":")[0]).intValue();
				this.caIndx = Integer.valueOf(this.caIndxStr.split(":")[0]).intValue();
			} catch (NumberFormatException ex) {}
		}
		
		public void unsetProgressIndx() {
			this.duIndx=-1; this.pfIndx=-1; this.caIndx=-1; 		
			this.duIndxStr="-1:"; this.pfIndxStr="-1:-1:"; this.caIndxStr="-1:"; 
		}
		
		public void setIndexOfDelUseProgress(String indxStr) {
			try {
				this.duIndx = Integer.valueOf(indxStr.split(":")[0]).intValue();
				this.duIndxStr = indxStr;
			} 
			catch (NumberFormatException ex) {
				this.duIndxStr = "-1:";
				this.duIndx = -1;
			}
			catch (ArrayIndexOutOfBoundsException ex1) {
				this.duIndxStr = "-1:";
				this.duIndx = -1;
			}
		}
		
		public void setIndexOfProdForbidProgress(String indxStr) {
			this.pfIndxStr = indxStr;
			try {
				this.pfIndx = Integer.valueOf(this.pfIndxStr.split(":")[0]).intValue();
				this.pfIndxStr = indxStr;
			} 
			catch (NumberFormatException ex) {
				this.pfIndxStr = "-1:-1";
				this.pfIndx = -1;
			}
			catch (ArrayIndexOutOfBoundsException ex1) {
				this.pfIndxStr = "-1:-1";
				this.pfIndx = -1;
			}
		}
		
		public void setIndexOfChangeAttrProgress(String indxStr) {
			try {
				this.caIndx = Integer.valueOf(this.caIndxStr.split(":")[0]).intValue();
				this.caIndxStr = indxStr;
			} 
			catch (NumberFormatException ex) {
				this.caIndxStr = "-1:";
				this.caIndx = -1;
			}
			catch (ArrayIndexOutOfBoundsException ex1) {
				this.caIndxStr = "-1:";
				this.caIndx = -1;
			}
		}
		
		public boolean isProgressIndexSet() {
			return (this.duIndx!=-1) || (this.pfIndx!=-1) || (this.caIndx!=-1);
		}
		
		public void clear() {
			this.isCritical = false;
			if (this.overlapping != null)
				this.overlapping.clear();
			this.overlapping = null;
			this.state = Entry.NOT_SET;
			this.status = Entry.NOT_SET;
			this.duIndx=-1; this.pfIndx=-1; this.caIndx=-1; 		
			this.duIndxStr="-1:"; this.pfIndxStr="-1:-1:"; this.caIndxStr="-1:"; 
		}
	}

	/***    Class Entry  END  ****/
	

	/**
	 * This container stores the conflict relations.
	 */
	protected Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	excludeContainer = null;

	/**
	 * true, if each calculation should be done in an extra Thread. This makes
	 * only sense on computer with a lot of CPUs (at least 2).
	 */
	protected boolean calculateParallel = false;

	/**
	 * This container stores the conflict free relations.
	 */
	protected Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	conflictFreeContainer = null;

	/**
	 * Contains for each Pair one Entry. The structure is a hash table of
	 * hash tables of Entry.
	 */
	public Hashtable<Rule, Hashtable<Rule, Entry>> 
	commonContainer = new Hashtable<Rule, Hashtable<Rule, Entry>>();

	protected int conflictKind = CriticalPair.CONFLICT;

	/**
	 * The graph grammar
	 */
	protected GraGra grammar;

	/**
	 * <code>rules</code> is the horizontal list and <code>rules2</code> is the vertical list.
	 * If <code>rules</code> is equal to <code>rules2</code>, we have symmetrical rule matrix.
	 */
	protected List<Rule> rules, rules2;
	
	protected boolean asymmetrical;
	
	/**
	 * true if all the relations are computed.
	 */
	protected boolean isComputed;

	/**
	 * true if one pair relations are computed.
	 */
	protected boolean isComputedLocal;

	final protected Vector<ParserEventListener> listener = new Vector<ParserEventListener>(
			2);

	/** true if global generating process will be stopped */
	protected boolean stop;

	/**
	 * true if global generating process is running.
	 */
	protected boolean isAlive;

	protected ExcludePair excludePair;

	protected boolean notCompleteComputable;
	
	protected boolean complete,
			reduce, reduceSameMatch,
			withNACs, withPACs,
			consistent,
			directStrctCnfl, directStrctCnflUpToIso,
			strongAttrCheck;
	
	protected boolean namedObjectOnly;
	
	protected int maxBoundOfCriticKind = 0; // <=0 unbound
	
	protected boolean equalVariableNameOfAttrMapping;
	
	protected boolean useHostGraph;

	// in excludeContainerForTestGraph: keys are overlapgraphs
	// in values are graph object mappings:
	// keys are graph objects of an overlapgraph, 
	// values -  objects of the test graph
	final protected Hashtable<Graph, Vector<Hashtable<GraphObject, GraphObject>>> 
	excludeContainerForTestGraph = new Hashtable<Graph, Vector<Hashtable<GraphObject, GraphObject>>>();

	protected Graph testGraph;

	protected MorphCompletionStrategy strategy;

	protected boolean isEmpty = true;

	protected boolean ignoreIdenticalRules = false;

	protected  Hashtable<ValueMember, Pair<String, String>> storeMap;
	
	long freeM, usedM;
	
	/**
	 * Creates a new container for critical pairs.
	 * 
	 * @param gragra
	 *            The graph grammar.
	 */
	public ExcludePairContainer(GraGra gragra) {
		initAllContainer();
		this.isComputed = false;
		this.stop = false;
		this.isAlive = false;

		this.complete = true;
		this.withNACs = true;
		this.consistent = true;

		this.setGrammar(gragra);
	}

	/**
	 * Returns currently active ExcludePair of the pair container.
	 */
	public ExcludePair getActiveExcludePair() {
		return this.excludePair;
	}
	
	/**
	 * Starts the filling of all containers parallel.
	 */
	public void run() {
		// System.out.println("ExcludePairContainer.run ...");
		Runtime.getRuntime().gc();
		long time0 = System.currentTimeMillis();
		// System.out.println("CPA: Start time: "+time0+"s");
//		long maxMem = Runtime.getRuntime().maxMemory()/1024;
		// //System.out.println("Max memory: "+maxMem+"k");
//		long freem0 = Runtime.getRuntime().freeMemory();
		// System.out.println("Free memory: "+freem0+"k");
		this.freeM = 0;
		
		this.stop = false;
		this.isAlive = true;
		
		if (this.useHostGraph) {			
			firePairEvent(new ParserMessageEvent(this, //ParserEvent.FINISHED,
			"Thread  -  Checking Host Graph  -  started."));
		}
		
		firePairEvent(new ParserMessageEvent(this,
				"Thread  - Critical pairs -  runs ..."));
		// compute CPs
		fillContainers();
		
		if (this.stop) {
			System.out.println("Critical pairs - Used time: "+ (System.currentTimeMillis() - time0) + "ms");
			System.out.println("Critical pairs - Used memory: "+usedM/1024+"k");
			firePairEvent(new ParserMessageEvent(this,
					"Thread  -  Critical pairs  -  was stopped."));
			this.stop = false;
		} else {			
			System.out.println("Critical pairs - Used time: "+ (System.currentTimeMillis() - time0) + "ms");
			System.out.println("Critical pairs - Used memory: "+this.usedM/1024+"k");

			firePairEvent(new ParserMessageEvent(this, ParserEvent.FINISHED,
					"Thread  -  Critical pairs  -  finished."));
			
			if(this.useHostGraph) {			
				firePairEvent(new ParserMessageEvent(this, ParserEvent.FINISHED,
				"Thread  -  Checking Host Graph  -  finished."));
			}
		}
		this.isAlive = false;
	}

	/*
	private void updateExcludeContainerAfterCheckHostGraph() {
		Enumeration<Rule> keys1 = this.excludeContainer.keys();
		while (keys1.hasMoreElements()) {
			Rule r1 = keys1.nextElement();
			Enumeration<Rule> keys2 = this.excludeContainer.get(r1).keys();
			while (keys2.hasMoreElements()) {
				Rule r2 = keys2.nextElement();
				getCriticalForGraph(r1, r2);				
			}			
		}		
	}
	*/
	
	/**
	 * Stops the process of computing critical pairs.
	 */
	public void stop() {
		this.stop = true;
		if (this.excludePair != null) {
			this.isComputed = false;
			this.excludePair.stop = true;
		}
	}

	/**
	 * If the parameter is <code>true</code> initiates the stop of the process 
	 * of computing critical pairs.
	 */
	public void setStop(boolean b) {
		// System.out.println("ExcludePairContainer.setStop:: "+b);
		this.stop = b;
		if (this.stop)
			stop();
	}

	/**
	 * Returns <code>true</code> if the process of computing critical pairs
	 * was stopped (not finished).
	 */
	public boolean wasStopped() {
		return this.stop;
	}

	public void enableIgnoreIdenticalRules(boolean b) {
		this.ignoreIdenticalRules = b;
	}

	// ****************************************************************************+

	/**
	 * computes the critical part of two rules. This can be a
	 * <code>Vector</code> of overlaping graphs.
	 * 
	 * @param r1
	 *            The first part of a critical pair
	 * @param r2
	 *            The second part of a critical pair
	 * @param kind
	 *            The kind of critical pair
	 * @throws InvalidAlgorithmException
	 *             Thrown if a illegal algorithm is chosen.
	 * @return The critical object.
	 */
//	public Object getCritical(Rule r1, Rule r2, int kind)
//			throws InvalidAlgorithmException {
//
//		/*
//		 * diese Methode darf nicht synchronized sein, da Anfragen an
//		 * verschiedenen kinds sehr wohl parallel laufen duerfen. Des weiteren
//		 * sollte die private Methode das lock uebernehmen. isComputed bei
//		 * private getCritic gucken.
//		 */
//		return getCritical(r1, r2, getContainer(kind));
//	}
	
	/**
	 * computes the critical part of two rules. This can be a
	 * <code>Vector</code> of overlaping graphs.
	 * 
	 * @param r1
	 *            The first part of a critical pair
	 * @param r2
	 *            The second part of a critical pair
	 * @param kind
	 *            The kind of critical pair
	 * @throws InvalidAlgorithmException
	 *             Thrown if a illegal algorithm is chosen.
	 * @return The critical object.
	 */
	public Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getCriticalPair(
			Rule r1, Rule r2, int kind)
			throws InvalidAlgorithmException {

		/*
		 * diese Methode darf nicht synchronized sein, da Anfragen an
		 * verschiedenen kinds sehr wohl parallel laufen duerfen. Des weiteren
		 * sollte die private Methode das lock uebernehmen. isComputed bei
		 * private getCritic gucken.
		 */
		return getCriticalPair(r1, r2, getContainer(kind));
	}

	
	/**
	 * Returns <code>CriticalPairData</code> object which allows an access to the
	 * computed critical pairs of the specified rule pair in a more readable way.
	 * @see <code>CriticalPairData</code>
	 * 
	 * @return  critical pair data if it is already computed, otherwise null
	 */
	public CriticalPairData getCriticalPairData(Rule r1, Rule r2) {
		Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
		secondPart = this.excludeContainer.get(r1);
		if (secondPart != null) {
			Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
			p = secondPart.get(r2);
			if (p != null) {
				if (p.first.booleanValue()) {
					return new CriticalPairData(r1, r2, p.second);
				} 
			}
		}
		
		return null;
	}
	
	/**
	 * Returns a list of <code>CriticalPairData</code>  which allows an access to the
	 * computed critical pairs of the specified kind of conflict in a more readable way.
	 * @see <code>CriticalPairData</code>
	 * 
	 * @return  critical pair data if it is already computed, otherwise null
	 */
	public List<CriticalPairData> getCriticalPairDataOfKind(String kind) {
		List<CriticalPairData> list = new Vector<CriticalPairData>();
		Enumeration<Rule> r1Iter = this.excludeContainer.keys();
		while (r1Iter.hasMoreElements()) {
			Rule r1 = r1Iter.nextElement();
			Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
			secondPart = this.excludeContainer.get(r1);
			if (secondPart != null) {
				Enumeration<Rule> r2Iter = secondPart.keys();
				while (r2Iter.hasMoreElements()) {
					Rule r2 = r2Iter.nextElement();
				
					Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
					p = secondPart.get(r2);
					if (p != null
							&& p.first.booleanValue()) {
						Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
						kindList = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
						for (int i=0; i<p.second.size(); i++) {
							Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
							cp = p.second.get(i);
							
							if (kind.indexOf(CriticalPairData.PRODUCE_DELETE_D_TXT) != -1) {
								if (cp.first.first.getTarget().getName().indexOf(CriticalPairData.PRODUCE_USE_D_TXT) != -1
										&& isProduceUseDelete(cp.first.first.getTarget(), cp.first, r2)) {
									kindList.add(cp);	
								}							
							}
							else if (kind.indexOf(CriticalPairData.PRODUCE_CHANGE_D_TXT) != -1) {
								if (cp.first.first.getTarget().getName().indexOf(CriticalPairData.PRODUCE_USE_D_TXT) != -1
										&& isProduceUseChange(cp.first.first.getTarget(), cp.first, r2)) {
									kindList.add(cp);	
								}							
							}
							else if (cp.first.first.getTarget().getName().indexOf(kind) != -1) {
								kindList.add(cp);	
							}
						}
						if (kindList.size() > 0) {
							CriticalPairData data = new CriticalPairData(r1, r2, kindList);
							list.add(data);
						} else {
							kindList = null;
						}
					}
				}
			}
		}
		return list;
	}
	
	private boolean isProduceUseDelete(
			final Graph overlapGraph,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> cpair,
			final Rule r2) {
		Enumeration<GraphObject> dom2 = cpair.second.getDomain();
		while (dom2.hasMoreElements()) {
			GraphObject go2 = dom2.nextElement();
			GraphObject go = cpair.second.getImage(go2);
			if (go.isCritical() && r2.getImage(go2) == null)
				return true;
		}
		return false;
	}
	
	private boolean isProduceUseChange(
			final Graph overlapGraph,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> cpair,
			final Rule r2) {
		Enumeration<GraphObject> dom2 = cpair.second.getDomain();
		while (dom2.hasMoreElements()) {
			GraphObject go2 = dom2.nextElement();
			GraphObject img2 = r2.getImage(go2);
			if (img2 != null) {
				GraphObject go = cpair.second.getImage(go2);				
				if (go.isCritical()) {
					if (go2.getAttribute() != null && img2.getAttribute() != null
							&& go2.isAttrMemValDifferent(img2)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	
	/**
	 * @deprecated  replaced by <code>getCriticalPair(Rule r1, Rule r2, int kind)</code>
	 */
	public Object getCritical(Rule r1, Rule r2, int kind)
	throws InvalidAlgorithmException {
		return getCriticalPair(r1, r2, getContainer(kind));
	}
	
	/**
	 * @deprecated  replaced by <code>getCriticalPair(Rule r1, Rule r2, int kind, boolean local)</code>
	 */
	public Object getCritical(Rule r1, Rule r2, int kind, boolean local)
	throws InvalidAlgorithmException {
		if (local)
			return getCriticalPair(r1, r2, getContainer(kind, r1, r2));
		
		return getCriticalPair(r1, r2, getContainer(kind));
	}
	
	
	public Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getCriticalPair(
			Rule r1, Rule r2, int kind, boolean local)
			throws InvalidAlgorithmException {

		if (local) {
//			Entry e = this.getEntry(r1, r2);
//			if (e.isProgressIndexSet()) {
//				return this.continueComputeCriticalPair(r1, r2, getContainer(kind, r1, r2));
//			} 
			
			return getCriticalPair(r1, r2, getContainer(kind, r1, r2));
		}
		
		return getCriticalPair(r1, r2, getContainer(kind));
	}

	public Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	continueComputeCriticalPair(
			Rule r1, Rule r2, int kind, boolean local)
			throws InvalidAlgorithmException {

		if (local) {
			Entry e = this.getEntry(r1, r2);
			if (e.isProgressIndexSet()) {
				return this.continueComputeCriticalPair(r1, r2, getContainer(kind, r1, r2));
			} 
			
			return getCriticalPair(r1, r2, getContainer(kind, r1, r2));
		}
		
		return getCriticalPair(r1, r2, getContainer(kind));
	}
	
	/**
	 * Computes the critical part of two rules and checks it on the specified
	 * Graph g. When an overlapping graph is not critical for the given Graph g
	 * it is removed from the critical part. Returns the critical object if it
	 * exists otherwise null. The specified rules are enabled and applicable to
	 * the Graph g, otherwise returns null.
	 * 
	 * @param r1
	 *            The first part of a critical pair
	 * @param r2
	 *            The second part of a critical pair
	 * @param g
	 *            The graph on which these two rules should be checked
	 * @return The critical object
	 *         <code>Hashtable<Graph,Vector<Hashtable<GraphObject,GraphObject>>></code>,
	 *         where a Graph is an overlapping graph and is a key, a Vector<Hashtable<GraphObject,GraphObject>>
	 *         contains found critical matches and is a value. Furthermore, a
	 *         Hashtable<GraphObject,GraphObject> defines a critical match of a
	 *         key-Graph into the specified Graph g. Here a key-GraphObject
	 *         belongs to a key-Graph which is an overlapping graph, a
	 *         value-GraphObject belongs to the specified Graph g.
	 */
	public Hashtable<Graph, Vector<Hashtable<GraphObject, GraphObject>>> getCriticalForGraph(
			final Rule r1, 
			final Rule r2, 
			final Graph g) {
//		 System.out.println("ExcludePairContainer.getCriticalForGraph... r1: "
//		 +r1.getName()+" r2: "+r2.getName()+" g: "+g.getName() +"  useHostGraph: "+useHostGraph);

		 if (!r1.isEnabled() || !r2.isEnabled()) {
			this.getEntry(r1, r2).state = Entry.DISABLED;
			Entry entry = addEntry(r1, r2, false, null);
			entry.status = Entry.NON_RELEVANT;
			addQuadruple(this.excludeContainer, r1, r2, false, null);
			addQuadruple(this.conflictFreeContainer, r1, r2, false, null);
			firePairEvent(new CriticalPairEvent(this, r1, r2,
					CriticalPairEvent.UNCRITICAL, "<" + r1.getName()
							+ ">  and  <" + r2.getName()
							+ ">  have not any critical pairs"));
			return null;
		}

		if (!r1.isApplicable(g, this.strategy) || !r2.isApplicable(g, this.strategy)) {
			this.getEntry(r1, r2).state = Entry.COMPUTED;
			Entry entry = addEntry(r1, r2, false, null);
			entry.status = Entry.NON_RELEVANT;
			addQuadruple(this.excludeContainer, r1, r2, false, null);
			addQuadruple(this.conflictFreeContainer, r1, r2, true, null);
			firePairEvent(new CriticalPairEvent(this, r1, r2,
					CriticalPairEvent.UNCRITICAL, "<" + r1.getName()
							+ ">  and  <" + r2.getName()
							+ ">  have not any critical pairs"));
			return null;
		}

		try {			
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			criticalObj = getCriticalPair(r1, r2, CriticalPair.EXCLUDE, true);
			if (criticalObj != null) {
				if (checkCritical(r1, r2, g)) {

					for (int i = 0; i < criticalObj.size(); i++) {
						Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
						pi = criticalObj.get(i);
						Pair<OrdinaryMorphism, OrdinaryMorphism> p1 = pi.first; // overlapping morphisms pair
						Pair<OrdinaryMorphism, OrdinaryMorphism> p2 = pi.second; // help pair

						OrdinaryMorphism m1 = p1.first;
						Graph overlapG = m1.getTarget();
						if (this.excludeContainerForTestGraph.get(overlapG) == null) {
							if (p2 != null) {
								m1 = p2.first;
								overlapG = m1.getTarget();
								if (this.excludeContainerForTestGraph.get(overlapG) == null) {
									criticalObj.remove(i);
									i--;
								}
							} else {
								criticalObj.remove(i);
								i--;
							}
						}
					}
					
					Entry entry = this.getEntry(r1, r2, true);
					if (criticalObj.isEmpty())
						entry.status = Entry.NON_RELEVANT;
				}
			}

		} catch (InvalidAlgorithmException ex) {}

		return this.excludeContainerForTestGraph;
	}

	public Vector<Pair<Hashtable<GraphObject, GraphObject>, Hashtable<GraphObject, GraphObject>>> getCriticalPairAtGraph(
			final Rule r1, 
			final Rule r2) {
		
		CriticalRulePairAtGraph test = new CriticalRulePairAtGraph(r1, r2, this.testGraph);
		test.setMorphismCompletionStrategy(this.strategy);
		
		Vector<Pair<Hashtable<GraphObject, GraphObject>, Hashtable<GraphObject, GraphObject>>>
		result = test.isCriticalAtGraph();
		
		// select match graph objects
		/*
		if (result != null && !result.isEmpty()) {
			Pair<Hashtable<GraphObject, GraphObject>, Hashtable<GraphObject, GraphObject>>
			p = result.get(0);
			Hashtable<GraphObject, GraphObject> m1 = p.first;
			Hashtable<GraphObject, GraphObject> m2 = p.second;
			
			Enumeration<GraphObject> keys = m1.keys();
			while (keys.hasMoreElements()) {
				GraphObject o = keys.nextElement();
				GraphObject i = m1.get(o);
				o.selected = true;
				i.selected = true;
			}
			
			keys = m2.keys();
			while (keys.hasMoreElements()) {
				GraphObject o = keys.nextElement();
				GraphObject i = m2.get(o);
				o.selected = true;
				i.selected = true;
			}
		}
		*/
		
		return result;
	}
	
	public Hashtable<Graph, Vector<Hashtable<GraphObject, GraphObject>>> getCriticalForGraph(
			final Rule r1, 
			final Rule r2) {
		if (this.useHostGraph && this.testGraph != null)
			return this.getCriticalForGraph(r1, r2, this.testGraph);
		
		return null;
	}

	/**
	 * Returns the critical object of two rules.
	 * 
	 * @param r1
	 *            The first rule
	 * @param r2
	 *            The second rule
	 * @param container
	 *            The container.
	 * @return The critical object is a Vector an element as a Pair.
	 */
	protected synchronized Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getCriticalPair(
			final Rule r1,
			final Rule r2,
			final Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> container) {
//		System.out.println("Excl.PC.getCritical(Rule r1, Rule r2, ...");
		if (this.stop)
			return null;

		Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
		secondPart = container.get(r1);
		/* falls r1/rX schon mal berechnet wurden, gibt es secondPart */
		if (secondPart != null) {
			Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
			p = secondPart.get(r2);
			/*
			 * wurden r1/r2 berechnet so existiert secondPart. ABER wird gerade
			 * r1/r3 berechnet, so existiert noch kein p fuer r1/r4. p waehre in
			 * dem fall null.
			 */
			if (p != null) {
				if (p.first.booleanValue()) {
					firePairEvent(new CriticalPairEvent(this, r1, r2,
							"rule pair  [ " + r1.getName() + " , "
									+ r2.getName() + " ]  done"));					
					return p.second;
				} 
				firePairEvent(new CriticalPairEvent(this, r1, r2,
							"rule pair  [ " + r1.getName() + " , "
									+ r2.getName() + " ]  done"));
				return null;
			} 
				
			computeCritical(r1, r2);
			firePairEvent(new CriticalPairEvent(this, r1, r2,
						"rule pair  [ " + r1.getName() + " , " + r2.getName()
								+ " ]  done"));
			return getCriticalPair(r1, r2, container);			
		} 
			
		computeCritical(r1, r2);
		firePairEvent(new CriticalPairEvent(this, r1, r2, "rule pair  [ "
					+ r1.getName() + " , " + r2.getName() + " ]  done"));
		return getCriticalPair(r1, r2, container);
	}

	public synchronized Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>> getLHSoverlappings(
			final Rule r1, 
			final Rule r2, 
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> criticalPairs) {

		Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		matches = new Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>>(5);
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		cps = criticalPairs;
		if (cps == null || cps.isEmpty()) {
			cps = getCriticalPair(r1, r2, this.excludeContainer);
		}
		if (cps != null && !cps.isEmpty()) {
			for (int i = 0; i < cps.size(); i++) {
				Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> pi = cps.get(i);
				Vector<OrdinaryMorphism> triple = getValidMatch1Match2(r1, r2, pi);
				if (triple != null && !triple.isEmpty()) {
					Pair<OrdinaryMorphism, OrdinaryMorphism> 
					overlapPair = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
							triple.get(0), triple.get(1));
					Pair<OrdinaryMorphism, OrdinaryMorphism> 
					isoPair = getIsomorphicOverlapping(matches, overlapPair);
					if (isoPair == null) {
						matches.add(overlapPair);
					}
				}
			}
		}
		return matches;
	}

	private synchronized Hashtable<String, Pair<OrdinaryMorphism, OrdinaryMorphism>> getLHSoverlappings(
			final Rule r1, 
			final Rule r2, 
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> criticalPairs,
			final Hashtable<String, OrdinaryMorphism> overlapGraphIso) {
		
		// in case of produce-forbid
		// the Vector overlapGraphIso will be filled with isomorphisms :
		// iso = overlapGraph -> overlapGraphAfterApplyReverseR1 .
		// Hashtable to return will contain a pair with match1 and match2,
		// the target of the matches is overlapGraphAfterApplyReverseR1,
		// the sources - LHS1 and LHS2.
		// The key of this table is the hashcode of a critical pair.
		
		Hashtable<String, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		result = new Hashtable<String, Pair<OrdinaryMorphism, OrdinaryMorphism>>(5);
		
		Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		matches = new Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>>(5);
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		cps = criticalPairs;
		if (cps == null || cps.isEmpty()) {
			cps = getCriticalPair(r1, r2, this.excludeContainer);
		}
		
		if (cps != null && !cps.isEmpty()) {
			for (int i = 0; i < cps.size(); i++) {
				Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
				pi = cps.get(i);
				
				Vector<OrdinaryMorphism> 
				triple = getValidMatch1Match2(r1, r2, pi);
				
				if (triple != null && !triple.isEmpty()) {
					Pair<OrdinaryMorphism, OrdinaryMorphism> 
					overlapPair = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
							triple.get(0), triple.get(1));
					Pair<OrdinaryMorphism, OrdinaryMorphism> 
					isoPair = getIsomorphicOverlapping(matches, overlapPair);
					if (isoPair == null) {
						matches.add(overlapPair);
						result.put(String.valueOf(pi.hashCode()), overlapPair);
						if (triple.get(2) != null) {
							overlapGraphIso.put(String.valueOf(pi.hashCode()),
									triple.get(2));
						}
					}
				}
			}
		}
		return result;
	}

	private Vector<OrdinaryMorphism> getValidMatch1Match2(
			final Rule r1, 
			final Rule r2,
			final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> criticalPair) {
		// System.out.println("ExcludePairContainer.getValidMatch1Match2 ... rule1: "+r1.getName()+" rule2: "+r2.getName());
		
		BaseFactory bf = BaseFactory.theFactory();
		Vector<OrdinaryMorphism> result = new Vector<OrdinaryMorphism>(3);
		Pair<OrdinaryMorphism, OrdinaryMorphism> p1 = criticalPair.first;
		Pair<OrdinaryMorphism, OrdinaryMorphism> p2 = criticalPair.second;
		if (p2 == null) {
			// delete-use conflict match
			// change-use attribute conflict match
			result.add(p1.first);
			result.add(p1.second);
			result.add(null);
			return result;
		} 
		OrdinaryMorphism overlapMorph1prime = p1.first;
		OrdinaryMorphism overlapMorph2prime = p1.second;
		OrdinaryMorphism L2iso = p2.first;
		if (overlapMorph1prime.getSource() == r1.getRight()) {
			// produce-forbid conflict match
			// make inverse rule1
			Pair<Pair<Rule,Boolean>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			inverseR1pair = bf.reverseRule(r1);
			if (inverseR1pair == null) {
				return null;
			}
			Rule inverseR1 = inverseR1pair.first.first;
			OrdinaryMorphism isoLeftR1 = inverseR1pair.second.first;
			OrdinaryMorphism isoRightR1 = inverseR1pair.second.second;
			OrdinaryMorphism 
			morph1prime = overlapMorph1prime.completeDiagram(isoRightR1);
			OrdinaryMorphism targetIso = morph1prime.getTarget().isomorphicCopy();
			if (targetIso == null) {
				morph1prime.dispose();
				return null;
			}
			
			// make match of inverse rule1
			OrdinaryMorphism morph1primeTest = morph1prime
					.compose(targetIso);
			((ContextView) morph1primeTest.getAttrContext())
					.setVariableContext(true);
			Match m1primeTest = bf.makeMatch(inverseR1, morph1primeTest);
			if (m1primeTest == null) {
				morph1primeTest.dispose();
				morph1primeTest = null;
				return null;
			}
			m1primeTest.setCompletionStrategy(new Completion_InjCSP());
			// apply inverted r1 to extended overlapGraph
			OrdinaryMorphism ms = null;
			try {
				ms = (OrdinaryMorphism) TestStep.execute(m1primeTest, true, this.equalVariableNameOfAttrMapping);
			} catch (TypeException e) {}				
			if (ms == null) {
				bf.destroyMorphism(m1primeTest);
				m1primeTest = null;
				bf.destroyMorphism(morph1primeTest);
				morph1primeTest = null;
				return null;
			}
			// make match m1 : L1 -> overlapGraph
			OrdinaryMorphism morph1test = isoLeftR1.compose(ms);
			ms.dispose(); 
			
			Match m1test = bf.makeMatch(r1, morph1test);
			// make match of rule2
			OrdinaryMorphism morph2primeTest = overlapMorph2prime
						.compose(targetIso);
			OrdinaryMorphism morph2test = L2iso.compose(morph2primeTest);
			Match m2test = bf.makeMatch(r2, morph2test);
				
			overlapMorph2prime.getTarget().unsetTransientAttrValues();
				
			result.add(m1test);
			result.add(m2test);
			result.add(targetIso);
			return result;
		} 
		// change-forbid attributes conflict match
		OrdinaryMorphism morph2primeTest = L2iso
				.compose(overlapMorph2prime);
		Match m2test = bf.makeMatch(r2, morph2primeTest);
			
		overlapMorph2prime.getTarget().unsetTransientAttrValues();
			
		result.add(overlapMorph1prime);
		result.add(m2test);
		result.add(null);
		return result;
	}

	private Pair<OrdinaryMorphism, OrdinaryMorphism> getIsomorphicOverlapping(
			Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>> overlapPairs,
			Pair<OrdinaryMorphism, OrdinaryMorphism> overlapPair) {
		Graph overlapGraph = overlapPair.first.getTarget();
		for (int j = 0; j < overlapPairs.size() && !this.stop; j++) {
			Pair<OrdinaryMorphism, OrdinaryMorphism> p1 = overlapPairs
					.elementAt(j);
			Graph g = p1.first.getTarget();
			Hashtable<GraphObject, GraphObject> partialMap = getMorphismMap(
					p1.first, overlapPair.first);
			Vector<OrdinaryMorphism> overlapIsos = g.getIsomorphicWith(overlapGraph, partialMap);
			if (overlapIsos != null) {
				for (int i = 0; i < overlapIsos.size(); i++) {
					OrdinaryMorphism overlapIso = overlapIsos.get(i);
					if (overlapIso != null) {
						if (p1.first.isIsomorphicTo(overlapPair.first,
								overlapIso)) {
							if (p1.second.isIsomorphicTo(overlapPair.second,
									overlapIso)) {
								return p1;
							}
						}
					}
				}
			}
		}
		return null;
	}

	private Hashtable<GraphObject, GraphObject> getMorphismMap(
			OrdinaryMorphism m1, OrdinaryMorphism m2) {
		// pre-condition: m1.source == m2.source
		Hashtable<GraphObject, GraphObject> map = new Hashtable<GraphObject, GraphObject>(
				m1.getSize());
		Enumeration<GraphObject> e = m1.getDomain();
		while (e.hasMoreElements()) {
			GraphObject o1 = e.nextElement();
			GraphObject o = m1.getImage(o1);
			GraphObject i = m2.getImage(o1);
			if (o != null && i != null)
				map.put(o, i);
		}
		return map;
	}

	
	// ****************************************************************************+
	/**
	 * Returns the number of containers.
	 * 
	 * @return The number.
	 */
	public int getNumberOfContainers() {
		return 2;
	}

	public Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	getExcludeContainer() {
		return this.excludeContainer;
	}

	public Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	getConflictContainer() {
		return this.excludeContainer;
	}

	public Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	getConflictFreeContainer() {
		return this.conflictFreeContainer;
	}

	
	/**
	 * This container is a <code>Hashtable</code> with a rule as key. The
	 * value will be a set of rules.
	 * 
	 * @param kind
	 *            The desired algorithm: <code>CriticalPair.CONFLICT</code>,
	 *            <code>CriticalPair.CONFLICTFREE</code>
	 * @throws InvalidAlgorithmException
	 *             Thrown if a illegal algorithm is choosen
	 * @return The container.
	 */
	public Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	getContainer(int kind) throws InvalidAlgorithmException {
		// System.out.println("Excl.PC.getContainer... kind: "+kind);
		// System.out.println("isComputed: "+isComputed);
		if (!this.isComputed && !this.stop) {
			fillContainers();
		}

		if (kind == CriticalPair.EXCLUDE) {
			return this.excludeContainer;
		} else if (kind == CriticalPair.CONFLICTFREE) {
			return this.conflictFreeContainer;
		} else {
			throw new InvalidAlgorithmException("No such algorithm", kind);
		}
	}
	
	public Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	getContainer(
			int kind, 
			final List<Rule> ruleList, 
			boolean asymmetrical) throws InvalidAlgorithmException {
		
		if (!this.isComputed) {
			fillContainers(ruleList, asymmetrical);
		}

		if (kind == CriticalPair.EXCLUDE) {
			return this.excludeContainer;
		} else if (kind == CriticalPair.CONFLICTFREE) {
			return this.conflictFreeContainer;
		} else {
			throw new InvalidAlgorithmException("No such algorithm", kind);
		}
	}

	public Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	getContainer(
			int kind, 
			final Rule r1, 
			final Rule r2) throws InvalidAlgorithmException {
		
		this.isComputedLocal = this.isComputed;
		if (!this.isComputedLocal) {
			if ((kind == CriticalPair.CONFLICTFREE)
					&& (this.conflictFreeContainer != null)
					&& this.conflictFreeContainer.containsKey(r1)) {
				this.isComputedLocal = true;
			} else if ((kind == CriticalPair.EXCLUDE || kind == CriticalPair.CONFLICT)
					&& (this.excludeContainer != null)
					&& this.excludeContainer.containsKey(r1)) {
				this.isComputedLocal = true;
			}
		}
		if (!this.isComputedLocal && !this.stop) {
			fillContainers(r1, r2);
		}

		if (kind == CriticalPair.EXCLUDE) {
			return this.excludeContainer;
		} else if (kind == CriticalPair.CONFLICTFREE) {
			return this.conflictFreeContainer;
		} else {
			throw new InvalidAlgorithmException("No such algorithm", kind);
		}
	}

	// ****************************************************************************+
	/**
	 * This method computes which rules are in a relation of a special kind.
	 * 
	 * @param kind
	 *            The kind of critical pair
	 * @param rule
	 *            The rule which is the first part of a critical pair
	 * @throws InvalidAlgorithmException
	 *             Thrown if a illegal algorithm is chosen
	 * @return The set of all the critic rules
	 */
	public Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> getCriticalSet(int kind, Rule rule)
			throws InvalidAlgorithmException {
		/* getContainer throws Exception */
		return getCriticalSet(getContainer(kind), rule);
	}

	private Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getCriticalSet(
			Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> container,
			Rule rule) {
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		resultVector = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
		
		Vector<Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
		tmpVector = new Vector<Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>();
		
		Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
		value = container.get(rule);
		
		for (Enumeration<Rule> keys = value.keys(); keys.hasMoreElements();) {
			Rule key = keys.nextElement();
			Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> p = value.get(key);
			if (p.first.booleanValue())
				tmpVector.addElement(p.second);
		}
		for (int i = 0; i < tmpVector.size(); i++) {
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> element = tmpVector.elementAt(i);
			for (int j = 0; j < element.size(); j++) {
				resultVector.addElement(element.elementAt(j));
			}
		}
		return resultVector;
	}

	// ****************************************************************************+
	/**
	 * Sets the graph grammar for the critical pairs.
	 * 
	 * @param gragra
	 *            The graph grammar
	 */
	public void setGrammar(GraGra gragra) {
		this.grammar = gragra;
		if (this.grammar != null) {
			if (this.grammar.getMorphismCompletionStrategy() != null) {
				this.strategy = (MorphCompletionStrategy) this.grammar
						.getMorphismCompletionStrategy().clone();
			}
			// not still possible 
//			this.calculateParallel = this.grammar.getGraTraOptions().contains("PARALLEL");
								
//			this.setRules(this.grammar.getRulesWithIntegratedMultiRulesOfRuleScheme());
			this.setRules(this.grammar.getRulesWithIntegratedRulesOfRuleScheme());
		}
	}

	
	/**
	 * Returns the graph grammar of the critical pairs.
	 * 
	 * @return The graph grammar.
	 */
	public GraGra getGrammar() {
		return this.grammar;
	}

	/**
	 * Set rule list to be analyzed. 
	 * The rule matrix contains the same 
	 * rule set in horizontal and vertical direction.
	 */
	public void setRules(final List<Rule> ruleList) {
		if (this.storeMap == null) 
			this.storeMap = new Hashtable<ValueMember, Pair<String, String>>();
		else
			this.storeMap.clear();
		BaseFactory.theFactory().replaceExprByVarInApplConds(ruleList, this.storeMap);
		
		this.rules = ruleList;
		this.rules2 = new Vector<Rule>(ruleList);
	}
	
	/**
	 * Set rule lists to be analyzed. 
	 * The rule matrix contains the first list in horizontal 
	 * and the second list in vertical direction.
	 */
	public void setRules(final List<Rule> ruleList, final List<Rule> ruleList2) {
		if (this.storeMap == null) 
			this.storeMap = new Hashtable<ValueMember, Pair<String, String>>();
		else
			this.storeMap.clear();
		
		// horizontal rules
		this.rules = ruleList;
		// vertical rules
		this.rules2 = ruleList2;
		
		BaseFactory.theFactory().replaceExprByVarInApplConds(this.rules, this.storeMap);
		if (this.rules != this.rules2) {
			for (int i=0; i<rules2.size(); i++) {
				Rule r = rules2.get(i);
				if (!rules.contains(r))
					BaseFactory.theFactory().replaceExprByVarInApplConds(r, storeMap);
			}
//			BaseFactory.theFactory().replaceExprByVarInApplConds(this.rules2, this.storeMap);
		}
	}
	
	public void restoreExprReplacedByVarInApplConds() {
		BaseFactory.theFactory().restoreExprByVarInApplConds(this.rules, this.storeMap);
		if (this.rules != this.rules2) {
			for (int i=0; i<rules2.size(); i++) {
				Rule r = rules2.get(i);
				if (!rules.contains(r))
					BaseFactory.theFactory().restoreExprByVarInApplConds(r, storeMap);
			}
//			BaseFactory.theFactory().restoreExprByVarInApplConds(this.rules2, this.storeMap);
		}
	}
	
	/**
	 * Returns the rule list in horizontal direction of the rule matrix.
	 */
	public List<Rule> getRules() {
		return this.rules;
	}
	
	/**
	 * Returns the rule list in vertical direction of the rule matrix.
	 */
	public List<Rule> getRules2() {
		return this.rules2;
	}
	
	/**
	 * This method has an effect if the rule matrix contains the same 
	 * rule set in horizontal and vertical direction. <br>
	 * If the parameter is <code>true</code> then only the right top triangle 
	 * of the rule matrix will be computed.
	 */
	public void setComputeAsymmetrical(boolean b) {
		this.asymmetrical = b;
	}
	
	public void setMorphCompletionStrategy(MorphCompletionStrategy strat) {
		this.strategy = strat;
	}
	
	public MorphCompletionStrategy getMorphCompletionStrategy() {
		return this.strategy;
	}
	
	// ****************************************************************************+
	/**
	 * Initials all containers. So there are at least empty objects to store the
	 * exclude relation.
	 */
	public void initAllContainer() {
		this.conflictFreeContainer = new Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>>();
		this.excludeContainer = new Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>>();
		this.commonContainer = new Hashtable<Rule, Hashtable<Rule, Entry>>();
	}

	
	/**
	 * Fills all containers with the critical pairs.
	 */
	protected void fillContainers() {
		/*
		 * diese Methode darf nicht synchronized sein, da sie sehr viel Zeit
		 * beansprucht und dadurch das Objekt locked. 
		 * computeCritical(r1, r2) ist besser geeignet
		 */
		if (this.useHostGraph && this.grammar != null) {
			this.grammar.getApplicableRules(this.testGraph, this.strategy);
		}

		if (!this.useHostGraph) {
			this.isComputed = false;
		}
		
		if (this.rules != null && !this.rules.isEmpty()
				&& this.rules2 != null && !this.rules2.isEmpty()) {
			// do nothing
		} else if (this.grammar != null) {
			this.rules = new Vector<Rule>(this.grammar.getListOfRules());
			this.rules2 = new Vector<Rule>(this.grammar.getListOfRules());
		}

		boolean asymmetric = this.asymmetrical 
									&& (this.rules.equals(this.rules2));
		
		/* 2 Schleifen um alle Regeln mit allen Regeln zu ueberpruefen. */
		// horizontal - this.rules
		// vertical   - this.rules2
		int indx=0;		
		for (int j=0; j<this.rules2.size() && !this.stop; j++) {
			Rule r1 = this.rules2.get(j);
			if (asymmetric) {
				indx = j;
			}	
			
			for (int i = indx; i < this.rules.size() && !this.stop; i++) {
				Rule r2 = this.rules.get(i);
				
				this.scheduleForComputing(r1, r2);
			}
		}
		if (!this.useHostGraph && !this.stop) {
			this.isComputed = true;
		}
	}

	protected void fillContainers(final List<Rule> ruleList, boolean asymmetric) {
//		 System.out.println("ExclPairContainer.fillContainers(List<Rule> ruleList) ..");
		/*
		 * diese Methode darf nicht synchronized sein, da sie sehr viel Zeit
		 * beansprucht und dadurch das Objekt locked. 
		 * computeCritical(r1, r2) ist besser geeignet
		 */
		if (!this.useHostGraph) {
			this.isComputed = false;
		}

		Vector<Rule> ruleList2 = new Vector<Rule>();
		ruleList2.addAll(ruleList);
	
		/* 2 Schleifen um alle Regeln mit allen Regeln zu ueberpruefen. */
		int indx=0;
		for (int j=0; j<ruleList2.size() && !this.stop; j++) {
			Rule r1 = ruleList2.get(j);
			if (asymmetric) {
				indx = j;
			}
			for (int i = indx; i < ruleList.size() && !this.stop; i++) {
				Rule r2 = ruleList.get(i);
				this.scheduleForComputing(r1, r2);
			}
		}
		if (!this.useHostGraph && !this.stop) {
			this.isComputed = true;
		}
	}

	protected void scheduleForComputing(Rule r1, Rule r2) {
		// System.out.println("ExclPairContainer.scheduleForComputing(r1, r2):
		// "+this.getEntry(r1, r2).state);

		if (this.useHostGraph) {
			Entry entry = this.getEntry(r1, r2);
			int state = entry.state;
			if (state == Entry.COMPUTED
					|| state == Entry.COMPUTED2
					|| state == Entry.COMPUTED12) {
				if (r1.isApplicable() && r2.isApplicable()) {
					if (computeCritical(r1, r2, this.testGraph))
						firePairEvent(new CriticalPairEvent(this, r1, r2,
								CriticalPairEvent.CRITICAL));
					else {
						entry.status = Entry.NON_RELEVANT;
						firePairEvent(new CriticalPairEvent(this, r1, r2,
								CriticalPairEvent.UNCRITICAL));
					}					
				} else {
					entry.status = Entry.NON_RELEVANT;
					firePairEvent(new CriticalPairEvent(this, r1, r2,
							CriticalPairEvent.NON_RELEVANT));
				}
			} else {				
				firePairEvent(new CriticalPairEvent(this, r1, r2,
						CriticalPairEvent.NON_RELEVANT));
			}
			return;
		}

		if (this.ignoreIdenticalRules && r1 == r2) {
			addEntry(r1, r2, false, null);
			this.getEntry(r1, r2).state = Entry.NOT_RELATED; //Entry.COMPUTED;
			addQuadruple(this.excludeContainer, r1, r2, false, null);
			addQuadruple(this.conflictFreeContainer, r1, r2, true, null);
			firePairEvent(new CriticalPairEvent(this, r1, r2, "<"
					+ r1.getName() + ">  and  <" + r2.getName()
					+ ">  should not be computed."));
			return;
		}
		// mark pair
		if (this.getEntry(r1, r2).state == Entry.NOT_SET)
			this.getEntry(r1, r2).state = Entry.SCHEDULED_FOR_COMPUTING;

		if (this.calculateParallel) {
			// start computing in another Thread
			new ComputingThread(this, r1, r2);
		} else {
			// start computing directly
			computeCritical(r1, r2);
		}
	}// scheduleForComputing

	/**
	 * Fills all containers with the critical pairs.
	 */
	protected void fillContainers(Rule r1, Rule r2) {
		/*
		 * Da die Regeln 2x durchgegangen werden muessen, ist es praktischer die
		 * Regeln in einen Vector zu stopfen, den man immer wieder zur
		 * Verfuegung hat.
		 */
		this.isComputedLocal = false;
		// mark pair
		if (this.getEntry(r1, r2).state == Entry.NOT_SET)
			this.getEntry(r1, r2).state = Entry.SCHEDULED_FOR_COMPUTING;

		computeCritical(r1, r2);
		this.isComputedLocal = true;
	}

	/**
	 * Computes if the first rule exclude the second rule. The result is added
	 * to the container.
	 * 
	 * @param r1
	 *            The first rule.
	 * @param r2
	 *            The second rule.
	 */
	protected synchronized void computeCritical(Rule r1, Rule r2) {
		// mark Entry
		Entry e = this.getEntry(r1, r2);
		if (!r1.isEnabled() || !r2.isEnabled()) { // test disabled rule
			e.state = Entry.DISABLED;
			addEntry(r1, r2, false, null);
			addQuadruple(this.excludeContainer, r1, r2, false, null);
			addQuadruple(this.conflictFreeContainer, r1, r2, false, null);
			firePairEvent(new CriticalPairEvent(this, r1, r2, "<"
					+ r1.getName() + ">  and  <" + r2.getName()
					+ ">  should not be computed."));
			return;
		}

		if ((e.state == Entry.SCHEDULED_FOR_COMPUTING)
				|| (e.state == Entry.NOT_SET)) {
			
			e.state = Entry.COMPUTING_IS_RUNNING;			
			firePairEvent(new CriticalPairEvent(this, r1, r2,
					"Computing critical rule pair  [  " + r1.getName()
							+ "  ,  " + r2.getName() + "  ]"));

			if (!this.complete)
				this.excludePair = new SimpleExcludePair();
			else
				this.excludePair = new ExcludePair();					
//			this.excludePair.setProgressIndx(e);			
			setOptionsOfExcludePair();

			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			overlapping = null;
			try {
				overlapping = this.excludePair.isCritical(CriticalPair.EXCLUDE, r1, r2);
				e = this.getEntry(r1, r2, true);
				e.setProgressIndx(this.excludePair);
			} catch (InvalidAlgorithmException iae) {
//				if (iae.getKindOfInvalidAlgorithm() == CriticalPairEvent.NOT_COMPUTABLE) {
//					e.status = Entry.NOT_COMPUTABLE;
//				}				
			}
			
			this.usedM = this.usedM + this.excludePair.usedM;
			
			this.excludePair.dispose();
			this.excludePair = null;

			boolean critic = (overlapping != null);

			// new container
			addEntry(r1, r2, critic, overlapping);

			/*
			 * Wenn overlapping Elemente enthaelt sind r1/r2 kritisch critic
			 * wird daher true. Alle wichtigen Informationen werden eingetragen.
			 * Enthaelt r1/r2 keine Elementen, so wird critic auf false gesetzt.
			 * Wenn excludeContainer nach r1/r2 gefragt wird, liefert die
			 * Antwort auch false. overlapping kann daher null sein.
			 */

			/*
			 * Achtung, wenn r1 r2 nicht kritisch ist gibt es keine
			 * Ueberlappungen
			 */
			addQuadruple(this.excludeContainer, r1, r2, critic, overlapping);

			/*
			 * conflictfree braucht keine ueberlappungsgraphen daher ist das
			 * letzte Argument null
			 */
			addQuadruple(this.conflictFreeContainer, r1, r2, !critic, null);

			if (overlapping != null) {						
				firePairEvent(new CriticalPairEvent(this, r1, r2,
						CriticalPairEvent.CRITICAL, "<" + r1.getName()
								+ ">  and  <" + r2.getName()
								+ ">  have critical pairs"));
				
				//TEST CriticalPairData
//				CriticalPairData cpd = this.getCriticalPairData(r1, r2);
//				System.out.println("rule1 has name: " + cpd.getRule1().getName());
//				System.out.println("rule2 has name: " + cpd.getRule2().getName());
//				System.out.println("morph1 has name: " + cpd.getMorph1().getName());
//				System.out.println("morph2 has name: " + cpd.getMorph2().getName());
			}
			else {
				firePairEvent(new CriticalPairEvent(this, r1, r2,
						CriticalPairEvent.UNCRITICAL, "<" + r1.getName()
								+ ">  and  <" + r2.getName()
								+ ">  have not any critical pairs"));
			}
		}
	}

	protected synchronized Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	continueComputeCriticalPair(
			Rule r1, 
			Rule r2,
			final Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
			container) {
		// get Entry
		Entry e = this.getEntry(r1, r2, true);
		if (!e.isProgressIndexSet()) {
			return null;
		}
		e.state = Entry.COMPUTING_IS_RUNNING;				
		firePairEvent(new CriticalPairEvent(this, r1, r2,
					"Computing critical rule pair  [  " + r1.getName()
							+ "  ,  " + r2.getName() + "  ]"));
		if (!this.complete)
			this.excludePair = new SimpleExcludePair();
		else
			this.excludePair = new ExcludePair();	
		
		this.excludePair.setProgressIndx(e);			
		setOptionsOfExcludePair();

		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlapping = null;
		try {
			overlapping = this.excludePair.isCritical(CriticalPair.EXCLUDE, r1, r2);			
			e = this.getEntry(r1, r2, true);
			e.setProgressIndx(this.excludePair);
		} catch (InvalidAlgorithmException iae) {}
			
		this.excludePair.dispose();
		this.excludePair = null;

		boolean critic = (overlapping != null);
		addEntry(r1, r2, critic, overlapping);
		addQuadruple(this.excludeContainer, r1, r2, critic, overlapping);
		addQuadruple(this.conflictFreeContainer, r1, r2, !critic, null);
		
		if (overlapping != null) {	
			firePairEvent(new CriticalPairEvent(this, r1, r2,
						CriticalPairEvent.CRITICAL, "<" + r1.getName()
								+ ">  and  <" + r2.getName()
								+ ">  have critical pairs"));
		}
		else {
			firePairEvent(new CriticalPairEvent(this, r1, r2,
						CriticalPairEvent.UNCRITICAL, "<" + r1.getName()
								+ ">  and  <" + r2.getName()
								+ ">  have not any critical pairs"));
		}
		
		firePairEvent(new CriticalPairEvent(this, r1, r2, "rule pair  [ "
				+ r1.getName() + " , " + r2.getName() + " ]  done"));
		return getCriticalPair(r1, r2, container);
	}
	
	@SuppressWarnings("deprecation")
	protected void setOptionsOfExcludePair() {
		this.excludePair.enableNACs(this.withNACs);
		this.excludePair.enablePACs(this.withPACs);
		this.excludePair.enableIgnoreIdenticalRules(this.ignoreIdenticalRules);
		this.excludePair.enableReduceSameMatch(this.reduceSameMatch);	
		this.excludePair.enableConsistent(this.consistent, this.grammar);
		this.excludePair.enableStrongAttrCheck(this.strongAttrCheck);
		this.excludePair.enableEqualVariableNameOfAttrMapping(this.equalVariableNameOfAttrMapping);
		this.excludePair.enableReduce(this.reduce);
		this.excludePair.enableDirectlyStrictConfluent(this.directStrctCnfl);
		this.excludePair.enableDirectlyStrictConfluentUpToIso(this.directStrctCnflUpToIso);
		this.excludePair.setMorphismCompletionStrategy(this.grammar.getMorphismCompletionStrategy());
		this.excludePair.enableNamedObjectOnly(this.namedObjectOnly);
		this.excludePair.setMaxBoundOfCriticKind(this.maxBoundOfCriticKind);
	}
	
	public void refreshOptions(final CriticalPairOption op) {
		this.withNACs = op.nacsEnabled();
		this.withPACs = op.pacsEnabled();
		this.ignoreIdenticalRules = op.ignoreIdenticalRulesEnabled();
		this.reduceSameMatch = op.reduceSameMatchEnabled();	
		this.consistent = op.consistentEnabled();		
		this.strongAttrCheck = op.strongAttrCheckEnabled();
		this.equalVariableNameOfAttrMapping = op.equalVariableNameOfAttrMappingEnabled();
		this.reduce = op.reduceEnabled();
		this.directStrctCnfl = op.directlyStrictConflEnabled();
		this.directStrctCnflUpToIso = op.directlyStrictConflUpToIsoEnabled();
		this.namedObjectOnly = op.namedObjectEnabled();
		this.maxBoundOfCriticKind = op.getMaxBoundOfCriticKind();
	}
	
	protected synchronized boolean computeCritical(Rule r1, Rule r2, Graph g) {

		int state = this.getEntry(r1, r2).state;
		if (state == Entry.COMPUTED
				|| state == Entry.COMPUTED2
				|| state == Entry.COMPUTED12) {
			firePairEvent(new CriticalPairEvent(this, r1, r2,
					"Computing critical rule pair  [  " + r1.getName()
							+ "  ,  " + r2.getName() + "  ]"));

			if (checkCritical(r1, r2, g)) {
				firePairEvent(new CriticalPairEvent(this, r1, r2, "<"
						+ r1.getName() + ">  and  <" + r2.getName()
						+ "> are critical on the host graph"));
				return true;
			}
			firePairEvent(new CriticalPairEvent(this, r1, r2, "<"
						+ r1.getName() + ">  and  <" + r2.getName()
						+ "> are not critical on the host graph"));
			return false;
		} 
		return false;
	}

	/**
	 * Adds four important data to a container.
	 * 
	 * This method is synchronized, because the underlaying container aren't
	 * protected and this method is called asynchronly by the method
	 * computeCritical.
	 * 
	 * @param container
	 *            The container the data are for
	 * @param r1
	 *            The first rule
	 * @param r2
	 *            The seconf rule
	 * @param critic
	 *            true if the first rule excludes the second rule.
	 * @param overlapping
	 *            The set of overlapping graphs of the first and second rule.
	 */
	protected synchronized void addQuadruple(
			final Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> container,
			final Rule r1, 
			final Rule r2, 
			boolean critic,
			final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> overlapping) {
		
		if (container.containsKey(r1)) {
			Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
			secondPart = container.get(r1);			
			Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
			p = new Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>(
					Boolean.valueOf(critic), overlapping);
			secondPart.put(r2, p);
		} else {
			Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
			secondPart = new Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>();
			Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
			p = new Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>(
					Boolean.valueOf(critic), overlapping);
			secondPart.put(r2, p);
			container.put(r1, secondPart);
		}
		
		Entry entry = this.getEntry(r1, r2, true);
		if (entry == null) {
			entry = addEntry(r1, r2, critic, overlapping);
			if (critic) { 
				if (this.excludePair != null) {
					if (!this.excludePair.dependencyCond1 && this.excludePair.dependencyCond2)					 
						entry.setState(Entry.COMPUTED2);
					else if (this.excludePair.dependencyCond1 && this.excludePair.dependencyCond2)
						entry.setState(Entry.COMPUTED12);
				} else {
					if (entry.state == Entry.COMPUTED
							&&  entry.overlapping != null) {
						entry.setState(getEntryState(overlapping));
					}
				}
			}
		}
		
		// additionally, set status NOT_COMPLETE_COMPUTABLE if needed
		if (r1.hasEnabledACs(false) || r2.hasEnabledACs(false)) {
			this.notCompleteComputable = true;
			System.out.println("( "+r1.getName()+"  ,  "+r2.getName()+" )"+"  CPA for rules with GACs is not yet supported.");
			entry.status = Entry.NOT_COMPLETE_COMPUTABLE;
		}
	}

	private int getEntryState(final Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> overlapping) {
		int entryState = Entry.COMPUTED;
		boolean dependCond1 = false;
		boolean dependCond2 = false;
		for (int i=0; i<overlapping.size(); i++) {
			if (overlapping.get(i) != null) {
				String overlapGraphName = overlapping.get(i).first.first.getTarget().getName();
				if(overlapGraphName.indexOf("-switch-dependency")>=0
						|| overlapGraphName.indexOf(CriticalPairData.PRODUCE_DELETE_D_TXT)>=0 //"deliver-delete-dependency"
						|| overlapGraphName.indexOf(CriticalPairData.FORBID_PRODUCE_D_TXT)>=0 //"forbid-produce-dependency")
						|| overlapGraphName.indexOf(CriticalPairData.PRODUCE_CHANGE_D_TXT)>=0 //"deliver-change-dependency"
						|| overlapGraphName.indexOf("change-change-dependency")>=0) {
					dependCond2 = true;
				}
				else if(overlapGraphName.indexOf("-dependency")>=0)
					dependCond1 = true;
			} else {
				overlapping.remove(i); i--;
			}
		}
		if (!dependCond1 && dependCond2)
			entryState = Entry.COMPUTED2;
		else if (dependCond1 && dependCond2)
			entryState = Entry.COMPUTED12;
		
		return entryState;
	}
	
	// TODO: JavaDoc
	protected synchronized Entry addEntry(Rule r1, Rule r2, boolean critical,
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> overlapping) {

		Entry entry = this.getEntry(r1, r2);
		// mark as computed
		if (entry.state != Entry.DISABLED 
				&& entry.state != Entry.NOT_RELATED)
			entry.state = Entry.COMPUTED;
		// save data
		entry.isCritical = critical;
		entry.overlapping = overlapping;		
		this.isEmpty = false;				
		return entry;
	}

	
	public synchronized Entry getEntry(Rule r1, Rule r2) {
		// get the second level Hashtable or create a new one
		Hashtable<Rule, Entry> secondPart = this.commonContainer.get(r1);
		if (secondPart == null) {
			secondPart = new Hashtable<Rule, Entry>();
			this.commonContainer.put(r1, secondPart);
		}
		// now get the entry for this pair or create a new one
		Entry entry = secondPart.get(r2);
		if (entry == null) {
			entry = new Entry();
			secondPart.put(r2, entry);
		}
		return entry;
	}

	// getEntry that already existent
	public synchronized Entry getEntry(Rule r1, Rule r2, boolean alreadyExists) {
		// get the second level Hashtable or create a new one
		Hashtable<Rule, Entry> secondPart = this.commonContainer.get(r1);
		if (secondPart == null) {
			if (alreadyExists)
				return null;
			
			secondPart = new Hashtable<Rule, Entry>();
			this.commonContainer.put(r1, secondPart);
			Entry entry = new Entry();
			secondPart.put(r2, entry);
		}
		
		// now get the entry for this pair
		return secondPart.get(r2);
	}

	public synchronized Entry getEntryCopy(Rule r1, Rule r2) {
		Entry entry = getEntry(r1, r2, true);
		if (entry != null) {
			Entry entry1 = new Entry();
			entry1.isCritical = entry.isCritical;
			entry1.isRelationVisible = entry.isRelationVisible;
			entry1.isRuleVisible = entry.isRelationVisible;
			entry1.state = entry.state;
			entry1.status = entry.status;
			entry1.overlapping.addAll(entry.overlapping);
			return entry1;
		}
		return null;
	}
	
	public synchronized void clearEntry(Rule r1, Rule r2) {
		Entry entry = this.getEntry(r1, r2, true);
		if (entry != null) {
			if (this.excludeContainer.get(r1) != null) {
				Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
				p = this.excludeContainer.get(r1).get(r2);
				if (p.first.booleanValue()) {
					Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> v = p.second;
					for (int i = 0; i < v.size(); i++) {
						Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> cpi = v.elementAt(i);
						Pair<OrdinaryMorphism, OrdinaryMorphism> cp = cpi.first;
						OrdinaryMorphism m1 = cp.first;
						OrdinaryMorphism m2 = cp.second;
						m1.dispose();
						m2.dispose();
						m1 = null;
						m2 = null;
						cp = null;
						v.removeElementAt(i);
						i--;
					}
				}
				this.excludeContainer.get(r1).remove(r2);
			}

			if (this.conflictFreeContainer != null && this.conflictFreeContainer.get(r1) != null)
				this.conflictFreeContainer.get(r1).remove(r2);

			if (this.commonContainer.get(r1) != null)
				this.commonContainer.get(r1).remove(r2);

			entry.clear();
		}
	}

	public synchronized void setEntryRelationVisible(Rule rule1, Rule rule2,
			boolean vis, boolean local) {
		Entry entry = this.getEntry(rule1, rule2, true);
		if (entry != null && entry.isCritical()) {
			entry.isRelationVisible = vis;
			if (!local) {
				if (vis)
					firePairEvent(new CriticalPairEvent(this, rule1, rule2,
							CriticalPairEvent.SHOW_ENTRY));
				else
					firePairEvent(new CriticalPairEvent(this, rule1, rule2,
							CriticalPairEvent.HIDE_ENTRY));
				// System.out.println("entry.isRelationVisible:
				// "+entry.isRelationVisible);
			}
		}
	}

	public synchronized void setEntryRuleVisible(Rule rule1, Rule rule2,
			boolean vis, boolean local, boolean context) {
		if (rule1 != rule2)
			return;
		Entry entry = this.getEntry(rule1, rule2, true);
		if (entry != null) {
			entry.isRuleVisible = vis;
			if (context) {
				// set Rule Context to vis
				for (Enumeration<Rule> keys = this.excludeContainer.keys(); keys
						.hasMoreElements();) {
					Rule r1 = keys.nextElement();
					if (r1 == rule1) {
						Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
						secondPart = this.excludeContainer.get(r1);
						for (Enumeration<Rule> k2 = secondPart.keys(); k2
								.hasMoreElements();) {
							Rule r2 = k2.nextElement();
							ExcludePairContainer.Entry entry1 = getEntry(r1, r2);
							// if(entry.isCritical())
							{
								entry1.isRelationVisible = vis;
								if (vis)
									firePairEvent(new CriticalPairEvent(this,
											rule1, rule2,
											CriticalPairEvent.SHOW_ENTRY));
								else
									firePairEvent(new CriticalPairEvent(this,
											rule1, rule2,
											CriticalPairEvent.HIDE_ENTRY));
							}
						}
						for (Enumeration<Rule> k2 = secondPart.keys(); k2
								.hasMoreElements();) {
							Rule r2 = k2.nextElement();
							if (r2 != rule1) {
								ExcludePairContainer.Entry entry1 = getEntry(
										r2, r1);
								// if(entry.isCritical())
								{
									entry1.isRelationVisible = vis;
									if (vis)
										firePairEvent(new CriticalPairEvent(
												this, rule1, rule2,
												CriticalPairEvent.SHOW_ENTRY));
									else
										firePairEvent(new CriticalPairEvent(
												this, rule1, rule2,
												CriticalPairEvent.HIDE_ENTRY));
								}
							}
						}
						break;
					}
				}
			}
			if (!local) {
				if (vis)
					firePairEvent(new CriticalPairEvent(this, rule1, rule2,
							CriticalPairEvent.SHOW_ENTRY));
				else
					firePairEvent(new CriticalPairEvent(this, rule1, rule2,
							CriticalPairEvent.HIDE_ENTRY));

			}
		}
	}

	/**
	 * return if this pair of rules is already computed.
	 */
	public int getState(Rule r1, Rule r2) {
		return this.getEntry(r1, r2).state;
	}

	public boolean reduceCriticalPairs() {
		// System.out.println("reduceCriticalPairs... ");
		boolean reduced = false;
		for (Enumeration<Rule> keys = this.excludeContainer.keys(); keys.hasMoreElements();) {
			Rule r1 = keys.nextElement();
			// System.out.println("ExcludePC:: reduce: "+r1.getName());
			Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
			secondPart = this.excludeContainer.get(r1);
			for (Enumeration<Rule> k2 = secondPart.keys(); k2.hasMoreElements();) {
				Rule r2 = k2.nextElement();
				// System.out.println("ExcludePC:: reduce: "+r2.getName());
				Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> pair 
				= secondPart.get(r2);
				Boolean b = pair.first;
				if (b.booleanValue()) {
					Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> v = pair.second;
					int size = v.size();
					// System.out.println(size);
					boolean found = true;
					while ((size > 0) && found) {
						// System.out.println("while: "+size);
						found = false;
						for (int i = 0; i < size; i++) {
							Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p1i = v.elementAt(i);
							Pair<OrdinaryMorphism, OrdinaryMorphism> p1 = p1i.first;
							for (int j = i + 1; j < size; j++) {
								Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p2i = v.elementAt(j);
								Pair<OrdinaryMorphism, OrdinaryMorphism> p2 = p2i.first;
								// System.out.println("checkIfSimilar ");
								Pair<OrdinaryMorphism, OrdinaryMorphism> p = checkIfSimilar(p1, p2);
								// System.out.println("after checkIfSimilar
								// "+p);
								if (p != null) {
									// System.out.println(p+" "+p1+" i= "+i+" j=
									// "+j );
									boolean first = false;
									if (p == p1) {
										first = true;
										i--;
									}
									j--;
									found = true;
									v.removeElement(p2i);
									BaseFactory.theFactory().destroyMorphism(p.first);
									BaseFactory.theFactory().destroyMorphism(p.second);							
									p = null;
									p2i = null;
									reduced = true;
									size = v.size();
									if (first)
										break;
								}
							}
						}
					}
				}
			}
		}
		if (reduced)
			firePairEvent(new ParserMessageEvent(
					this,
					"Reduction of critical pairs is done.  Please select a rule pair to see results."));
		else
			firePairEvent(new ParserMessageEvent(this, "Nothings to reduce."));
		return reduced;
	}

	// only optional, if reduce is true
	private Pair<OrdinaryMorphism, OrdinaryMorphism> checkIfSimilar(
									Pair<OrdinaryMorphism, OrdinaryMorphism> p1, 
									Pair<OrdinaryMorphism, OrdinaryMorphism> p2) {
		
		Graph overlap1 = p1.first.getImage();
		int n1 = 0;
		Iterator<?> e = overlap1.getNodesSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (p1.first.getInverseImage(o).hasMoreElements()
					|| p1.second.getInverseImage(o).hasMoreElements())
				n1++;
		}
		e = overlap1.getArcsSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (p1.first.getInverseImage(o).hasMoreElements()
					|| p1.second.getInverseImage(o).hasMoreElements())
				n1++;
		}
		
		Graph overlap2 = p2.first.getImage();
		int n2 = 0;
		e = overlap2.getNodesSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (p2.first.getInverseImage(o).hasMoreElements()
					|| p2.second.getInverseImage(o).hasMoreElements())
				n2++;
		}
		e = overlap2.getArcsSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (p2.first.getInverseImage(o).hasMoreElements()
					|| p2.second.getInverseImage(o).hasMoreElements())
				n2++;
		}
		
		if (n1 != n2)
			return null;

		Pair<OrdinaryMorphism, OrdinaryMorphism> p = null;
		OrdinaryMorphism first1 = p1.first;
		OrdinaryMorphism first2 = p2.first;
		OrdinaryMorphism second1 = p1.second;
		OrdinaryMorphism second2 = p2.second;

		if (overlap1.getSize() <= overlap2.getSize()) {
			if (ExcludePairHelper.checkIfMorphSimilar(overlap1, overlap2, first1, first2, second1, second2))
				p = p2;
		} else {
			if (ExcludePairHelper.checkIfMorphSimilar(overlap2, overlap1, first2, first1, second2, second1))
				p = p1;
		}
		return p;
	}
	
	public void checkConsistency() {
		// System.out.println("ExcludePairContainer.checkConsistency()");
		boolean inconsistent = false;
		boolean cpExists = false;
		for (Enumeration<Rule> keys = this.excludeContainer.keys(); keys.hasMoreElements();) {
			Rule r1 = keys.nextElement();
			Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
			secondPart = this.excludeContainer
					.get(r1);
			for (Enumeration<Rule> k2 = secondPart.keys(); k2.hasMoreElements();) {
				Rule r2 = k2.nextElement();
				Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
				pair = secondPart.get(r2);
				Boolean b = pair.first;
				if (b.booleanValue()) {
					// System.out.println(r1.getName()+" "+r2.getName());
					cpExists = true;
					Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> v = pair.second;
					int size = v.size();
					for (int i = 0; i < size; i++) {
						Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> pi = v.elementAt(i);
						Pair<OrdinaryMorphism, OrdinaryMorphism> p = pi.first;
						Graph g = p.first.getImage();
						if (!this.grammar.checkGraphConsistency(g)) {
							inconsistent = true;
							// System.out.println(r1.getName()+"
							// "+r2.getName());
							// System.out.println("critical graph INCONSISTENT,
							// remove it");
							v.removeElement(pi);
							BaseFactory.theFactory().destroyMorphism(p.first);
							BaseFactory.theFactory().destroyMorphism(p.second);
							p = null;
							pi = null;
							i--;
							size = v.size();
						}
					}
					if (v.size() == 0) {
						// move (r1,r2) from exclude- to conflict free container
						moveEntryFromExcludeToConflictFreeContainer(r1, r2);
					}
				}
			}
		}
		if (inconsistent)
			firePairEvent(new ParserMessageEvent(
					this,
					"Consistency check of critical pairs is done.  Please select a rule pair to see results."));
		else if (cpExists)
			firePairEvent(new ParserMessageEvent(this,
					"All critical pairs were consistent."));
		else
			firePairEvent(new ParserMessageEvent(this,
					"There are no critical pairs."));
		// System.out.println("Consistency.check() END");
	}

	protected void moveEntryFromExcludeToConflictFreeContainer(Rule r1, Rule r2) {
		Entry entry = this.getEntry(r1, r2, true);
		entry.isCritical = false;
		entry.overlapping = null;
		Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
		secondPart = this.excludeContainer.get(r1);
		if (secondPart != null) {
			Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
			pair = secondPart.get(r2);
			pair.first = Boolean.valueOf(false);
			pair.second = null;
			if (this.conflictFreeContainer != null )
				this.conflictFreeContainer.put(r1, secondPart);
		}
	}

	protected void removeEntryFromExcludeContainer(Rule r1, Rule r2) {
		Entry entry = this.getEntry(r1, r2, true);
		entry.isCritical = false;
		entry.overlapping = null;
		Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
		secondPart = this.excludeContainer.get(r1);
		if (secondPart != null) {
			Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
			pair = secondPart.get(r2);
			pair.first = Boolean.valueOf(false);
			pair.second = null;			
		}
	}
	
	protected boolean checkCritical(Rule r1, Rule r2, Graph testgraph) {
//		 System.out.println("\nExcludePairContainer.checkCritical  ("
//		 + r1.getName()+", "+r2.getName() +")  at "+testgraph.getName());
		
		if (this.strategy == null)
			this.strategy = (MorphCompletionStrategy) 
						CompletionStrategySelector.getDefault().clone();
		// strategy.showProperties();
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		criticalOverlappings = getCriticalPair(r1, r2, this.excludeContainer);
		if (criticalOverlappings == null)
			return false;

		boolean critical = false;
		final Hashtable<String, OrdinaryMorphism> 
		overlapGraphIsos = new Hashtable<String, OrdinaryMorphism>(5);
		
		Hashtable<String, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		criticalMatches = getLHSoverlappings(r1, r2, criticalOverlappings, overlapGraphIsos);
		
		for (int j = 0; j < criticalOverlappings.size(); j++) {
			
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			criticalOverlap = criticalOverlappings.elementAt(j);
			
			Pair<OrdinaryMorphism, OrdinaryMorphism> 
			p = criticalMatches.get(String.valueOf(criticalOverlap.hashCode()));
			
			if (p != null) {
				Graph overlapG = p.first.getImage();
				
				Pair<OrdinaryMorphism, OrdinaryMorphism> p1 = criticalOverlap.first;
				Pair<OrdinaryMorphism, OrdinaryMorphism> p2 = criticalOverlap.second;
						
				Vector<Hashtable<GraphObject, GraphObject>> 
				mvec = new Vector<Hashtable<GraphObject, GraphObject>>();

				OrdinaryMorphism m = (BaseFactory.theFactory()).createMorphism(
						overlapG, testgraph);
				
				// test output
//				((VarTuple)m.getAttrContext().getVariables()).showVariables();
								
				OrdinaryMorphism overlapGraphIsom = overlapGraphIsos
						.get(String.valueOf(criticalOverlap.hashCode()));
								
				m.setCompletionStrategy(this.strategy);
				boolean hasCompletion = false;
				
				while (m.nextCompletionWithConstantsChecking()) {
					hasCompletion = true;
					boolean match2Critical = false;
					
					// check match of r1
					if (p1.first.getSource() == r1.getLeft()) {
						Match m1test = isCriticalMatchValidAtGraph(r1, p1.first, m);
						
//						check match of r2	
						if (m1test != null) {				
							if (p1.second.getSource() == r2.getLeft()) {								
								Match m2test = isCriticalMatchValidAtGraph(r2, p1.second, m);
								if (m2test != null) {
									if (p1.first.getTarget().getName().
											indexOf("-attr-conflict") >= 0) {
										if (!applyRule1Match1CheckMatch2(r1, m1test, r2, m2test))
											match2Critical = true;
									} else
										match2Critical = true;
								}
							} 
						}
					}
					else if (p1.first.getSource() == r1.getRight()) {
						Match m1test =  isCriticalMatchValidAtGraph(r1, p.first, m);
						
//						check match of r2	
						if (m1test != null) {
							Match m2test = isCriticalMatchValidAtGraph(r2, p.second, m);
							if (m2test != null) {
								if (!applyRule1Match1CheckMatch2NACs(r1, m1test, r2, m2test))
									match2Critical = true;
							}
						}					
					}
						
					if (!match2Critical)
						continue;
					
					critical = true;
					
					Hashtable<GraphObject, GraphObject>
					objs = new Hashtable<GraphObject, GraphObject>();
					
					Enumeration<GraphObject> e = m.getDomain();
					while (e.hasMoreElements()) {
						GraphObject o = e.nextElement();
						GraphObject img = m.getImage(o);
						if (overlapGraphIsom != null) {
							Enumeration<GraphObject> 
							invs = overlapGraphIsom.getInverseImage(o);
							if (invs.hasMoreElements()) {
								GraphObject orig = invs.nextElement();
								if (orig != null && img != null) {
									if (p1.first.getSource() == r1.getRight()) {
										boolean added = false;
										Enumeration<GraphObject> 
										rhs1 = p1.first.getInverseImage(orig);
										if (rhs1.hasMoreElements()) {
											GraphObject or1 = rhs1.nextElement();
											Enumeration<GraphObject> 
											lhs1 = r1.getInverseImage(or1);
											if (lhs1.hasMoreElements()) {
												objs.put(orig, img);
												added = true;
											}
										}

										if (!added && p2 != null && p2.first != null) {
											OrdinaryMorphism isoL2 = p2.first;
											Enumeration<GraphObject> 
											objsN2 = p1.second.getInverseImage(orig);
											if (objsN2.hasMoreElements()) {
												GraphObject objN2 = objsN2.nextElement();
												Enumeration<GraphObject> 
												lhs2 = isoL2.getInverseImage(objN2);
												if (lhs2.hasMoreElements()) {
													objs.put(orig, img);
												}
											}
										}
									}
								}
							} else {
								objs.put(o, img);
							}
						} else if (o != null && img != null) {
							objs.put(o, img);
						}
					}
					if (!objs.isEmpty()
							|| m.getSource().isEmpty())
						mvec.add(objs);
				}

				if (!mvec.isEmpty()) {
					if (p2 != null
							&& overlapGraphIsos.get(String.valueOf(
									criticalOverlap.hashCode())) != null) {
						overlapG = overlapGraphIsos.get(
								String.valueOf(criticalOverlap.hashCode()))
								.getOriginal();
						this.excludeContainerForTestGraph.put(overlapG, mvec);
					} else {
						this.excludeContainerForTestGraph.put(overlapG, mvec);
					}
//					System.out.println(r1.getName()+" & "+r2.getName()
//							 	+" CRITICAL on the specified graph! overlapG: "
//							 	+overlapG.hashCode()+" ?= "+testGraph.hashCode()+"  matches: "+mvec.size());
				}
				if (!hasCompletion) {
					(BaseFactory.theFactory()).destroyMorphism(m);
					m = null;
				}
			}
		}
		
		if (critical)
			return true;
		
		return false;
	}

	private boolean applyRule1Match1CheckMatch2(
			final Rule r1, 
			final Match m1test, 
			final Rule r2, 
			final Match m2test) {
	
		if (!m1test.getTarget().isAttributed())
			return true;
		
//		System.out.println("applyRule1Match1CheckMatch2... :: -attr-conflict");
		boolean attrsOK = true;
		OrdinaryMorphism isoG = m1test.getTarget().isomorphicCopy();
		if (isoG == null)
			return false;
		
		Match m1 = BaseFactory.theFactory().createMatch(r1, isoG.getTarget());
		if (m1.doCompose(m1test, isoG)) {
			m1.setCompletionStrategy(this.strategy, true);
//			make test step (r1,m1)				
			OrdinaryMorphism com1 = null;
//			TestStep s = new TestStep();
			try {
				com1 = (OrdinaryMorphism) TestStep.execute(m1);
			} catch (TypeException ex) {
				m1.dispose();
				m1 = null;
				return true;
			}
			
			if (com1 != null) {
				// check change-use-attr-conflict
				Enumeration<GraphObject> dom2 = m2test.getDomain();
				while (dom2.hasMoreElements()) {
					GraphObject o = dom2.nextElement();
					GraphObject img1 = m2test.getImage(o);
					GraphObject img2 = isoG.getImage(img1);
					if (!img1.compareTo(img2)) {
						attrsOK = false;
						break;
					}
				}
				if (attrsOK) {
					Match m2 = BaseFactory.theFactory().createMatch(r2, isoG.getTarget());
					if (m2.doCompose(m2test, isoG)) {							
						if (m2.areTotalIdentDanglSatisfied()) {
							final List<OrdinaryMorphism> nacs2 = r2.getNACsList();
							for (int l=0; l<nacs2.size(); l++) {
								final OrdinaryMorphism nac2 = nacs2.get(l);							
								OrdinaryMorphism nac2Star = (OrdinaryMorphism) m2.checkNAC(nac2);
								if (nac2Star != null) {
									attrsOK = false;
									nac2Star.dispose();
									break;
								}
							}
							
						}					
					}
					m2.dispose();
				}
				com1.dispose();	
			}			
		}
		m1.dispose();
		isoG.dispose();
//		System.out.println("applyRule1Match1CheckMatch2... :: -attr-conflict  "+attrsOK);
		return attrsOK;
	}
	
	private boolean applyRule1Match1CheckMatch2NACs(
			final Rule r1, 
			final Match m1test, 
			final Rule r2, 
			final Match m2test) {
	
		if (!m1test.getTarget().isAttributed())
			return true;

//		System.out.println("applyRule1Match1CheckMatch2... :: -attr-conflict");
		OrdinaryMorphism isoG = m1test.getTarget().isomorphicCopy();
		if (isoG == null)
			return false;
		
		Match m1 = BaseFactory.theFactory().createMatch(r1, isoG.getTarget());
		if (m1.doCompose(m1test, isoG)) {
			m1.setCompletionStrategy(this.strategy, true);
//			make test step (r1,m1)				
			OrdinaryMorphism com1 = null;
//			TestStep s = new TestStep();
			try {
				com1 = (OrdinaryMorphism) TestStep.execute(m1);
			} catch (TypeException ex) {
				m1.dispose();
				m1 = null;
				return true;
			}
			if (com1 != null) {
				Match m2 = BaseFactory.theFactory().createMatch(r2, isoG.getTarget());
				if (m2.doCompose(m2test, isoG)) {							
					if (m2.areTotalIdentDanglSatisfied()) {	
						final List<OrdinaryMorphism> nacs2 = r2.getNACsList();
						for (int l=0; l<nacs2.size(); l++) {
							final OrdinaryMorphism nac2 = nacs2.get(l);						
							OrdinaryMorphism nac2Star = (OrdinaryMorphism) m2.checkNAC(nac2);
							if (nac2Star != null) {
//								boolean critical = false;
								Enumeration<GraphObject> nac2StarCodom = nac2Star.getCodomain();
								while (nac2StarCodom.hasMoreElements()) {
									GraphObject o = nac2StarCodom.nextElement();
									Enumeration<GraphObject> preimgR1 = com1.getInverseImage(o);
									if (preimgR1.hasMoreElements()) {
										GraphObject preimg = preimgR1.nextElement();
										if (!r1.getInverseImage(preimg).hasMoreElements()) {
//											critical = true;
											nac2Star.dispose();
											com1.dispose();
											m1.dispose();
											m2.dispose();
											isoG.dispose();
											return false;
										}
									}
								}
							}
						}
					}
				}
				com1.dispose();
				m2.dispose();
			}
		}
		m1.dispose();
		isoG.dispose();
		
		return true;
	}
	
	
	private Match isCriticalMatchValidAtGraph(
			final Rule r, 
			final OrdinaryMorphism criticalMorph,
			final OrdinaryMorphism testMorph) {

		boolean result = true;
		// create match
		Match testMatch = BaseFactory.theFactory().createMatch(r, testMorph.getTarget());

		// set mapping
		Iterator<Node> rLHSnodes = r.getLeft().getNodesSet().iterator();
		while (result && rLHSnodes.hasNext()) {
			Node orig = rLHSnodes.next();
			Node img = (Node) testMorph.getImage(criticalMorph.getImage(orig));
			if (img != null) {
				try {
					testMatch.addMapping(orig, img);
				} catch (BadMappingException ex) {
					result = false;
				}
			}
		}
		Iterator<Arc> rLHSarcs = r.getLeft().getArcsSet().iterator();
		while (rLHSarcs.hasNext()) {
			Arc orig = rLHSarcs.next();
			Arc img = (Arc) testMorph.getImage(criticalMorph.getImage(orig));
			if (img != null) {
				try {
					testMatch.addMapping(orig, img);
				} catch (BadMappingException ex) {
					result = false;
				}
			}
		}
		
		if (result) {
			if (!testMatch.nextCompletion()
					|| !testMatch.isValid())
				result = false;
		}		
		
//		if (result && (!testMatch.areTotalityIdentificationDanglingSatisfied()
//						|| !testMatch.areNACsSatisfied()
//						|| !testMatch.arePACsSatisfied())) {		
//			result = false;
//		}
		
		if (!result) {
			testMatch.dispose();
			testMatch = null;
		}
		return testMatch;
	}
	
	public Hashtable<Graph, Vector<Hashtable<GraphObject, GraphObject>>> getExcludeContainerForTestGraph() {
		return this.excludeContainerForTestGraph;
	}

	/** Clears the rule pair containers. */
	public void clear() {
		Rule r1 = null;
		for (Enumeration<Rule> keys1 = this.excludeContainer.keys(); keys1
				.hasMoreElements();) {
			r1 = keys1.nextElement();
			// System.out.println(r1.getName());
			Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
			table2 = this.excludeContainer.get(r1);
			for (Enumeration<Rule> keys2 = table2.keys(); keys2.hasMoreElements();) {
				Rule r2 = keys2.nextElement();
				// System.out.println(r1.getName()+" "+r2.getName());
				// firePairEvent(new CriticalPairEvent(this, r1, r2,
				// CriticalPairEvent.REMOVE_RELATION_ENTRY));
				clearEntry(r1, r2);
			}
		}
		this.excludeContainerForTestGraph.clear();
		this.useHostGraph = false;
		this.testGraph = null;
		this.isComputed = false;
		this.isComputedLocal = false;
		this.isEmpty = true;
		
		firePairEvent(new CriticalPairEvent(this, null, null,
				CriticalPairEvent.REMOVE_ENTRIES));
	}

	protected void writeOverlapMorphisms(XMLHelper h, Rule r1, Rule r2,
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> overlapping) {
		Pair<OrdinaryMorphism, OrdinaryMorphism> p1 = overlapping.first;
		OrdinaryMorphism first = p1.first;
		// write first (left) overlap morphism
		h.openSubTag("Morphism");
		h.addAttr("name", first.getName());
		if (first.getSource() == r1.getLeft())
			h.addAttr("source", "LHS");
		else if (first.getSource() == r1.getRight())
			h.addAttr("source", "RHS");
		Enumeration<GraphObject> e = first.getDomain();
		while (e.hasMoreElements()) {
			GraphObject s = e.nextElement();
			h.openSubTag("Mapping");
			h.addObject("orig", s, false);
			h.addObject("image", first.getImage(s), false);
			h.close();
		}
		h.close();

		// write second (right) overlap morphism
		OrdinaryMorphism second = p1.second;
		Pair<OrdinaryMorphism, OrdinaryMorphism> p2 = overlapping.second;
		if (p2 == null) {
			if (second.getSource() == r2.getLeft()) {
				h.openSubTag("Morphism");
				h.addAttr("name", second.getName());
				h.addAttr("source", "LHS");
				e = second.getDomain();
				while (e.hasMoreElements()) {
					GraphObject s = e.nextElement();
					h.openSubTag("Mapping");
					h.addObject("orig", s, false);
					h.addObject("image", second.getImage(s), false);
					h.close();
				}
			}
		} else {
			OrdinaryMorphism morphL2iso = p2.first;
			OrdinaryMorphism morphNAC2iso = p2.second;
			h.openSubTag("Morphism");
			h.addAttr("name", second.getName());
			h.addAttr("source", "NAC+LHS");
			// second.target : N2 = NAC2+LHS2
			e = second.getDomain();
			while (e.hasMoreElements()) {
				GraphObject src = e.nextElement();
				GraphObject t = second.getImage(src);
				GraphObject s = null;
				if (morphL2iso.getInverseImage(src).hasMoreElements())
					s = morphL2iso.getInverseImage(src)
							.nextElement();
				else if (morphNAC2iso.getInverseImage(src).hasMoreElements())
					s = morphNAC2iso.getInverseImage(src)
							.nextElement();
				if (s != null) {
					h.openSubTag("Mapping");
					h.addObject("orig", s, false);
					h.addObject("image", t, false);
					h.close();
				}
			}
		}
		h.close();
	}

	protected Pair<OrdinaryMorphism, OrdinaryMorphism> readOldOverlappingMorphisms(XMLHelper h, Rule r1, Rule r2,
			String firstName, Graph overlapGraph) {
		OrdinaryMorphism first = BaseFactory.theFactory().createMorphism(
				r1.getLeft(), overlapGraph);
		OrdinaryMorphism second = BaseFactory.theFactory().createMorphism(
				r2.getLeft(), overlapGraph);
		first.setName(firstName.replaceAll(" ", ""));
		while (h.readSubTag("Mapping")) {
			GraphObject o = (GraphObject) h.getObject("orig", null, false);
			GraphObject i = (GraphObject) h.getObject("image", null, false);
			if (o != null && i != null) {
				first.addMapping(o, i);
			}
			h.close();
		}
		h.close();
		if (h.readSubTag("Morphism")) {
			String name = h.readAttr("name");
			second.setName(name.replaceAll(" ", ""));
			while (h.readSubTag("Mapping")) {
				GraphObject o = (GraphObject) h.getObject("orig", null, false);
				GraphObject i = (GraphObject) h.getObject("image", null, false);
				if (o != null && i != null) {
					second.addMapping(o, i);
				}
				h.close();
			}
			h.close();
		}
		return new Pair<OrdinaryMorphism, OrdinaryMorphism>(first, second);
	}

	protected Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> readOverlappingMorphisms(XMLHelper h, Rule r1,
			Rule r2, Graph overlapGraph) {
		// read first overlap morphism
		Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> result = null;
		OrdinaryMorphism first = null;
		OrdinaryMorphism second = null;
		Pair<OrdinaryMorphism, OrdinaryMorphism> p = null, p1 = null, p2 = null;
		if (h.readSubTag("Morphism")) {
			String name = h.readAttr("name");
			String source = h.readAttr("source");
			if (source.equals("")) {
				p = readOldOverlappingMorphisms(h, r1, r2, name, overlapGraph);
				return new Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>(p, null);
			}
			if (source.equals("LHS"))
				first = BaseFactory.theFactory().createMorphism(r1.getLeft(),
						overlapGraph);
			else if (source.equals("RHS"))
				first = BaseFactory.theFactory().createMorphism(r1.getRight(),
						overlapGraph);
			if (first != null) {
			first.setName(name.replaceAll(" ", ""));
			while (h.readSubTag("Mapping")) {
				GraphObject o = (GraphObject) h.getObject("orig", null, false);
				GraphObject i = (GraphObject) h.getObject("image", null, false);
				if (o != null && i != null) {
					first.addMapping(o, i);
				}
				h.close();
			}
			}
			h.close();
		}
		// read second overlap morphism
		if (h.readSubTag("Morphism")) {
			OrdinaryMorphism morphL2iso = null;
			OrdinaryMorphism morphNAC2iso = null;
			String name = h.readAttr("name");
			String source = h.readAttr("source");
			if (source.equals("LHS"))
				second = BaseFactory.theFactory().createMorphism(r2.getLeft(),
						overlapGraph);
			else if (source.equals("NAC+LHS")) {
				OrdinaryMorphism nac = null;
				final List<OrdinaryMorphism> nacs = r2.getNACsList();
				for (int l=0; l<nacs.size(); l++) {
					final OrdinaryMorphism n = nacs.get(l);				
					if (overlapGraph.getHelpInfoAboutNAC().indexOf(n.getName()) != -1) {
						nac = n;
						break;
					}
				}
				morphL2iso = r2.getLeft().isomorphicCopy();
				if (morphL2iso != null)
					morphNAC2iso = extendLeftGraph(morphL2iso, nac);
				Graph N2 = morphL2iso.getTarget();
				second = BaseFactory.theFactory().createMorphism(N2,
						overlapGraph);
			}
			if (second != null) {
				second.setName(name.replaceAll(" ", ""));
				while (h.readSubTag("Mapping")) {
					GraphObject o = (GraphObject) h.getObject("orig", null,
							false);
					GraphObject i = (GraphObject) h.getObject("image", null,
							false);
					if (o != null && i != null) {
						if (source.equals("LHS"))
							second.addMapping(o, i);
						else if (source.equals("NAC+LHS")) {
							GraphObject s = null;
							if (morphL2iso != null)
								s = morphL2iso.getImage(o);
							if (s == null && morphNAC2iso != null)
								s = morphNAC2iso.getImage(o);
							if (s != null)
								second.addMapping(s, i);
						}
					}
					h.close();
				}
				if (source.equals("NAC+LHS"))
					p2 = new Pair<OrdinaryMorphism, OrdinaryMorphism>(
							morphL2iso, morphNAC2iso);
			}
			h.close();
		}
		if (first != null && second != null) {
			p1 = new Pair<OrdinaryMorphism, OrdinaryMorphism>(first, second);
			result = new Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>(p1, p2);
		}
		return result;
	}

	private OrdinaryMorphism extendLeftGraph(OrdinaryMorphism isoLeft,
			OrdinaryMorphism nac) {
		if (isoLeft == null || nac == null)
			return null;
		Graph extLeft = isoLeft.getTarget();
		OrdinaryMorphism isoNAC = BaseFactory.theFactory().createMorphism(
				nac.getTarget(), extLeft);
		Hashtable<Node, Node> tmp = new Hashtable<Node, Node>(5);
		Iterator<?> e = nac.getTarget().getNodesSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (!nac.getInverseImage(o).hasMoreElements()) {
				try {
					Node n = extLeft.copyNode((Node) o);
					tmp.put((Node) o, n);
					isoNAC.addMapping(o, n);
				} catch (TypeException ex) {
				}
			} else
				isoNAC.addMapping(o, isoLeft.getImage(nac
						.getInverseImage(o).nextElement()));
		}
		e = nac.getTarget().getArcsSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (!nac.getInverseImage(o).hasMoreElements()) {
				Node src = tmp.get(((Arc) o).getSource());
				if (src == null) {
					src = (Node) isoLeft.getImage(nac.getInverseImage(
							((Arc) o).getSource()).nextElement());
				}
				Node tar = tmp.get(((Arc) o).getTarget());
				if (tar == null) {
					tar = (Node) isoLeft.getImage(nac.getInverseImage(
							((Arc) o).getTarget()).nextElement());
				}
				try {
					Arc a = extLeft.copyArc((Arc) o, src, tar);
					isoNAC.addMapping(o, a);
				} catch (TypeException ex) {
				}
			} else
				isoNAC.addMapping(o, isoLeft.getImage(nac
						.getInverseImage(o).nextElement()));

		}
		return isoNAC;
	}

	protected void resetRules(final List<Rule> list, final List<Rule> list2) {
		if (list != null && !list.isEmpty()) {
			if (this.rules == null) 
				this.rules = new Vector<Rule>();
			else
				this.rules.clear();
			
			this.rules.addAll(list);
		}
		
		if (list2 != null && !list2.isEmpty()) {
			if (this.rules2 == null) 
				this.rules2 = new Vector<Rule>();
			else
				this.rules2.clear();
			
			this.rules2.addAll(list2);
		} 		
	}
	
	
	/**
	 * Writes the contents of this object to a file in a xml format.
	 * 
	 * @param h
	 *            A helper object for storing.
	 */
	public void XwriteObject(XMLHelper h) {
		h.openNewElem("CriticalPairs", this);
		h.addObject("GraGra", getGrammar(), true);
		h.openSubTag("conflictsContainer");
		h.addAttr("kind", "exclude");
		
		// Inhalt von excludeContainer schreiben (save)
		for (Enumeration<Rule> keys = this.excludeContainer.keys(); keys.hasMoreElements();) {
			Rule r1 = keys.nextElement();

			h.openSubTag("Rule");
			h.addObject("R1", r1, false);

			Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
			secondPart = this.excludeContainer.get(r1);

			for (Enumeration<Rule> k2 = secondPart.keys(); k2.hasMoreElements();) {
				Rule r2 = k2.nextElement();

				h.openSubTag("Rule");
				h.addObject("R2", r2, false);
				Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
				p = secondPart.get(r2);
				Boolean b = p.first;

				h.addAttr("bool", b.toString());
				if (b.booleanValue()) {
					Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> v = p.second;

					for (int i = 0; i < v.size(); i++) {
						h.openSubTag("Overlapping_Pair");
						Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p2i = v.elementAt(i);
						Pair<OrdinaryMorphism, OrdinaryMorphism> p2 = p2i.first;
						OrdinaryMorphism first = p2.first;
						Graph overlapping = first.getImage();
						h.addObject("", overlapping, true);
						// write overlapping graph
						Iterator<?> e = overlapping.getNodesSet().iterator();
						while (e.hasNext()) {
							GraphObject o = (GraphObject) e.next();
							if (o.isCritical()) {
								h.openSubTag("Critical");
								h.addObject("object", o, false);
								h.close();
							}
						}
						e = overlapping.getArcsSet().iterator();
						while (e.hasNext()) {
							GraphObject o = (GraphObject) e.next();
							if (o.isCritical()) {
								h.openSubTag("Critical");
								h.addObject("object", o, false);
								h.close();
							}
						}
						
						writeOverlapMorphisms(h, r1, r2, p2i);

						// first.writeMorphism(h);
						// ((OrdinaryMorphism) p2.second).writeMorphism(h);

						h.close();
					}
				}
				h.close();
			}

			h.close();
		}
		h.close();
		h.openSubTag("conflictFreeContainer");
		for (Enumeration<Rule> keys = this.excludeContainer.keys(); keys.hasMoreElements();) {
			Rule r1 = keys.nextElement();

			h.openSubTag("Rule");
			h.addObject("R1", r1, false);

			Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
			secondPart = this.conflictFreeContainer.get(r1);

			for (Enumeration<Rule> k2 = secondPart.keys(); k2.hasMoreElements();) {
				Rule r2 = k2.nextElement();

				h.openSubTag("Rule");
				h.addObject("R2", r2, false);
				Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> p = secondPart.get(r2);
				Boolean b = p.first;

				h.addAttr("bool", b.toString());
				h.close();
			}

			h.close();
		}
		h.close();
		h.close();
	}

	/**
	 * Reads the contents of a xml file to restore this object.
	 * 
	 * @param h
	 *            A helper object for loading.
	 */
	@SuppressWarnings("unused")
	public void XreadObject(XMLHelper h) {
		// System.out.println("ExcludePairContainer.XreadObject....");
		if (h.isTag("CriticalPairs", this)) {
			Rule r1 = null;
			Rule r2 = null;
			boolean b = false;
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> allOverlappings = null;
			Vector<String> tagnames = new Vector<String>(1);
			Vector<String> tagnames2 = new Vector<String>(1);

			this.grammar = BaseFactory.theFactory().createGraGra();
			// loads the data in the predefined object
			h.getObject("", this.grammar, true);

			tagnames.add("conflictContainer");
			tagnames.add("conflictsContainer");
			tagnames.add("excludeContainer");

			tagnames2.add("dependencyContainer");
			tagnames2.add("dependenciesContainer");

//			boolean switchDependency = false;
			
			if (h.readSubTag(tagnames)) {
				String kind = h.readAttr("kind");
				this.conflictKind = CriticalPair.CONFLICT;
			}
			else if (h.readSubTag(tagnames2)) {
				this.conflictKind = CriticalPair.TRIGGER_DEPENDENCY;
				if (h.readAttr("kind").equals("trigger_switch_dependency")) {
//					switchDependency = true;
					this.conflictKind = CriticalPair.TRIGGER_SWITCH_DEPENDENCY;
				}
			}

			if (this.conflictKind == CriticalPair.CONFLICT
					|| this.conflictKind == CriticalPair.TRIGGER_DEPENDENCY
					|| this.conflictKind == CriticalPair.TRIGGER_SWITCH_DEPENDENCY) {
				Enumeration<?> r1s = h.getEnumeration("", null, true, "Rule");
				if (!r1s.hasMoreElements())
					r1s = h.getEnumeration("", null, true, "Regel");
				while (r1s.hasMoreElements()) {
					h.peekElement(r1s.nextElement());

					/*
					 * da ein referenziertes object geholt werden soll. muss nur
					 * angegeben werden wie der Membername heisst.
					 */
					r1 = (Rule) h.getObject("R1", null, false);
					Enumeration<?> r2s = h.getEnumeration("", null, true, "Rule");
					if (!r2s.hasMoreElements())
						r2s = h.getEnumeration("", null, true, "Regel");
					while (r2s.hasMoreElements()) {
						h.peekElement(r2s.nextElement());
						r2 = (Rule) h.getObject("R2", null, false);
						// System.out.println(r1.getName()+" "+r2.getName());
						String bool = h.readAttr("bool");
						b = false;
						allOverlappings = null;
						if (bool.equals("true")) {
							b = true;
							allOverlappings = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
							Enumeration<?> overlappings = h.getEnumeration("",
									null, true, "Overlapping_Pair");
							while (overlappings.hasMoreElements()) {
								h.peekElement(overlappings.nextElement());

								Graph g = (Graph) h.getObject("", BaseFactory.theFactory().createGraph(
										this.grammar.getTypeSet()), true);

								while (h.readSubTag("Critical")) {
									GraphObject o = (GraphObject) h.getObject(
											"object", null, false);
									if (o != null)
										o.setCritical(true);
									h.close();
								}

								Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p = readOverlappingMorphisms(
										h, r1, r2, g);
								allOverlappings.addElement(p);

								h.close();
							}
						}
						addQuadruple(this.excludeContainer, r1, r2, b,
								allOverlappings);
						h.close();
					}
					h.close();
				}
				h.close();
				// System.out.println("excludeContainer
				// "+excludeContainer+"\n");
			}
			
			if (h.readSubTag("conflictFreeContainer")) {
				Enumeration<?> r1s = h.getEnumeration("", null, true, "Rule");
				if (!r1s.hasMoreElements())
					r1s = h.getEnumeration("", null, true, "Regel");
				while (r1s.hasMoreElements()) {
					h.peekElement(r1s.nextElement());

					/*
					 * da ein referenziertes object geholt werden soll. muss nur
					 * angegeben werden wie der Membername heisst.
					 */
					r1 = (Rule) h.getObject("R1", null, false);
					Enumeration<?> r2s = h.getEnumeration("", null, true, "Rule");
					if (!r2s.hasMoreElements())
						r2s = h.getEnumeration("", null, true, "Regel");
					while (r2s.hasMoreElements()) {
						h.peekElement(r2s.nextElement());
						r2 = (Rule) h.getObject("R2", null, false);
						// System.out.println(r1.getName()+" "+r2.getName());
						String bool = h.readAttr("bool");
						b = false;

						if (bool.equals("true"))
							b = true;
						addQuadruple(this.conflictFreeContainer, r1, r2, b, null);

						if (!r1.isEnabled()) // test disabled rule
							this.getEntry(r1, r2).state = Entry.DISABLED;

						h.close();
					}
					h.close();
				}
				h.close();
				// System.out.println("conflictFreeContainer
				// "+conflictFreeContainer+"\n");
			}
		}
		h.close();
		
		// isComputed = true;

	}

	/**
	 * Provides a human readable text of the critical pairs.
	 * 
	 * @return The text.
	 */
	public String toString() {
		String result = super.toString() + "\n" + getGrammar().toString()
				+ "\n";

		result += "ConflictsContainer " + this.excludeContainer + "\n\n";
		result += "conflictFreeContainer " + this.conflictFreeContainer + "\n";
		return result;
	}

	/**
	 * Returns <code>true</code> if the rule pair container is empty.
	 */
	public boolean isEmpty() {
		return this.isEmpty;
	}

	/**
	 * Returns <code>true</code> if all conflicts of all rule pairs 
	 * of the container are computed.<br>
	 * Note: In case of a host graph is used to determine critical situations -
	 * this method returns <code>false</code> only.
	 */
	public boolean isComputed() {
		return this.isComputed;
	}

	/**
	 * Returns <code>true</code> if the process of computing critical pairs is running.
	 */
	public boolean isAlive() {		
		return this.isAlive;
	}

	// -----------------------------------------------------------------------+
	/**
	 * Adds a PairEventListener.
	 * 
	 * @param l
	 *            The listener.
	 */
	public void addPairEventListener(ParserEventListener l) {
		if (!this.listener.contains(l)) {
			this.listener.addElement(l);
		}
	}

	// -----------------------------------------------------------------------+
	/**
	 * Removes a PairEventListener
	 * 
	 * @param l
	 *            The listener.
	 */
	public void removePairEventListener(ParserEventListener l) {
		if (this.listener.contains(l))
			this.listener.removeElement(l);
	}

	// ***********************************************************************+
	/**
	 * Sends a event to all its listeners.
	 * 
	 * @param event
	 *            The event which will be sent
	 */
	protected synchronized void firePairEvent(ParserEvent event) {
		for (int i = 0; i < this.listener.size(); i++) {
			this.listener.elementAt(i).parserEventOccured(event);
		}
	}

	public void enableComplete(boolean enable) {
		this.complete = enable;
		
		if (this.excludePair != null) {
			this.excludePair.enableComplete(enable);				
		}
	}

	public void enableReduce(boolean enable) {
		this.reduce = enable;
	}

	public void enableReduceSameMatch(boolean enable) {
		this.reduceSameMatch = enable;
	}
	
	public void enableDirectlyStrictConfluent(boolean enable) {
		this.directStrctCnfl = enable;
	}
	
	public void enableDirectlyStrictConfluentUpToIso(boolean enable) {
		this.directStrctCnflUpToIso = enable;
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
	
	public void enableConsistent(boolean enable) {
		this.consistent = enable;
	}

	public void enableStrongAttrCheck(boolean enable) {
		this.strongAttrCheck = enable;
	}
	
	/** 
	 * If enable is true, the critical pairs are computed with respect to named object only. 
	 */
	public void enableNamedObjectOnly(boolean enable) {
		this.namedObjectOnly = enable;
	}
	
	public void enableMaxBoundOfCriticKind(int bound) {
		this.maxBoundOfCriticKind = bound;
	}
	
	public void enableEqualVariableNameOfAttrMapping(boolean enable) {
		this.equalVariableNameOfAttrMapping = enable;
	}
	
	/**
	 * Set and use (if the first parameter is <code>true</code>) 
	 * the given host graph and strategy in the process of computing critical 
	 * situations of the rule pairs. 
	 */
	public void enableUseHostGraph(boolean enable, Graph g,
			MorphCompletionStrategy strat) {
		this.useHostGraph = enable;
		this.excludeContainerForTestGraph.clear();
		if (this.useHostGraph) {
			this.testGraph = g;
			this.strategy = (MorphCompletionStrategy) strat.clone();
		} else {
			this.testGraph = null;
		}
	}

	/**
	 * Set and use (if the first parameter is <code>true</code>) 
	 * the given host graph in the process of computing critical situations
	 * of the rule pairs.
	 */
	public void enableUseHostGraph(boolean enable, Graph g) {
		this.useHostGraph = enable;
		this.excludeContainerForTestGraph.clear();
		if (this.useHostGraph)
			this.testGraph = g;
		else
			this.testGraph = null;
	}

	/**
	 * Returns <code>true</code>) 
	 * if a host graph is used in the process of computing critical situations
	 * of the rule pairs.
	 */
	public boolean useHostGraphEnabled() {
		return this.useHostGraph;
	}

	public int getKindOfConflict() {
		return this.conflictKind;
	}

	/**
	 * @deprecated
	 * @see agg.parser.PairContainer#getLayer()
	 */
	public LayerFunction getLayer() {
		return null;
	}


}

// End of ExcludePairContainer.java
/*
 * $Log: ExcludePairContainer.java,v $
 * Revision 1.92  2010/12/21 16:33:19  olga
 * improved - CPA for rules with GACs not implemented
 *
 * Revision 1.91  2010/12/20 20:05:36  olga
 * improved - show CPA Graph
 *
 * Revision 1.90  2010/12/17 15:45:11  olga
 * tuning
 *
 * Revision 1.89  2010/12/16 17:32:14  olga
 * tuning
 *
 * Revision 1.88  2010/12/15 16:57:56  olga
 * restriction added - CPA for rules with GACs not implemented
 *
 * Revision 1.87  2010/11/16 23:33:08  olga
 * tuning
 *
 * Revision 1.86  2010/11/14 19:01:19  olga
 * tuning
 *
 * Revision 1.85  2010/11/07 20:48:10  olga
 * tuning
 *
 * Revision 1.84  2010/11/06 18:33:50  olga
 * extended and improved
 *
 * Revision 1.83  2010/11/04 11:01:31  olga
 * tuning
 *
 * Revision 1.82  2010/09/20 14:30:11  olga
 * tuning
 *
 * Revision 1.81  2010/08/12 14:53:28  olga
 * tuning
 *
 * Revision 1.80  2010/06/22 11:12:13  olga
 * tuning
 *
 * Revision 1.79  2010/06/09 10:56:06  olga
 * tuning
 *
 * Revision 1.78  2010/04/29 15:15:13  olga
 * tuning and tests
 *
 * Revision 1.77  2010/04/27 10:38:34  olga
 * computing tuning
 *
 * Revision 1.76  2010/04/12 21:17:33  olga
 * improved
 *
 * Revision 1.75  2010/04/12 16:21:10  olga
 * tuning
 *
 * Revision 1.74  2010/04/12 14:40:46  olga
 * Critical pairs table - extended
 *
 * Revision 1.73  2010/03/08 15:46:42  olga
 * code optimizing
 *
 * Revision 1.72  2010/03/04 14:11:14  olga
 * code optimizing
 *
 * Revision 1.71  2010/02/22 15:02:05  olga
 * code optimizing
 *
 * Revision 1.70  2010/01/31 16:47:15  olga
 * extend CPA by checking with multi rules of rule schemes
 *
 * Revision 1.69  2010/01/27 19:35:19  olga
 * tests
 *
 * Revision 1.68  2009/11/23 09:00:39  olga
 * tuning
 *
 * Revision 1.67  2009/05/28 13:18:23  olga
 * Amalgamated graph transformation - development stage
 *
 * Revision 1.66  2009/05/12 10:36:58  olga
 * CPA: bug fixed
 * Applicability of Rule Seq. : bug fixed
 *
 * Revision 1.65  2009/03/19 09:31:05  olga
 * CPE: attr check improved
 *
 * Revision 1.64  2009/03/12 10:57:45  olga
 * some changes in CPA of managing names of the attribute variables.
 *
 * Revision 1.63  2008/11/13 08:26:21  olga
 * some tests
 *
 * Revision 1.62  2008/09/11 09:22:26  olga
 * Some changes in CPA: new computing of conflicts after an option changed,
 * Graph layout of overlapping graphs
 *
 * Revision 1.61  2008/05/07 08:37:55  olga
 * Applicability of Rule Sequences with NACs
 *
 * Revision 1.60  2008/05/05 09:11:51  olga
 * Graph parser - bug fixed.
 * New AGG feature - Applicability of Rule Sequences - in working.
 *
 * Revision 1.59  2008/04/10 10:53:14  olga
 * Draw graphics tuning
 *
 * Revision 1.58  2008/04/07 09:36:51  olga
 * Code tuning: refactoring + profiling
 * Extension: CPA - two new options added
 *
 * Revision 1.57  2008/02/25 08:44:48  olga
 * Extending of CPA: new class CriticalRulePairAtGraph to get critical
 * matches of two rules at a concret graph.
 *
 * Revision 1.56  2008/02/18 09:37:10  olga
 * - an extention of rule dependency check is implemented;
 * - some bugs fixed;
 * - editing of graphs improved
 *
 * Revision 1.55  2007/12/13 13:01:22  olga
 * check CPs on aa host graph - bug fixed
 *
 * Revision 1.54  2007/12/10 08:42:58  olga
 * CPA of grammar with node type inheritance for attributed graphs - bug fixed
 *
 * Revision 1.53  2007/11/19 08:48:39  olga
 * Some GUI usability mistakes fixed.
 * Default values in node/edge of a type graph implemented.
 * Code tuning.
 *
 * Revision 1.52  2007/11/12 08:48:56  olga
 * Code tuning
 *
 * Revision 1.51  2007/11/08 12:57:00  olga
 * working on CPA inconsistency for rules with pacs and inheritance
 * bugs are possible
 *
 * Revision 1.50  2007/11/05 09:18:22  olga
 * code tuning
 *
 * Revision 1.49  2007/11/01 09:58:18  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.48  2007/10/22 09:03:16  olga
 * First implementation of CPA for grammars with node type inheritance.
 * Only for internal tests.
 *
 * Revision 1.47  2007/10/10 14:30:33  olga
 * Enumeration typing
 *
 * Revision 1.46  2007/10/10 07:44:27  olga
 * CPA: bug fixed
 * GUI, AtomConstraint: bug fixed
 *
 * Revision 1.45  2007/10/04 07:44:27  olga
 * Code tuning
 *
 * Revision 1.44  2007/09/27 15:53:18  olga
 * CPA  tuning
 *
 * Revision 1.43  2007/09/27 08:42:46  olga
 * CPA: new option  -ignore pairs with same rules and same matches-
 *
 * Revision 1.42  2007/09/24 09:42:38  olga
 * AGG transformation engine tuning
 *
 * Revision 1.41  2007/09/10 13:05:42  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.40 2007/06/18 08:16:00 olga
 * New extentions by drawing edge.
 * 
 * Revision 1.39 2007/04/30 10:39:20 olga tests
 * 
 * Revision 1.38 2007/03/28 10:00:53 olga - extensive changes of Node/Edge Type
 * Editor, - first Undo implementation for graphs and Node/edge Type editing and
 * transformation, - new / reimplemented options for layered transformation, for
 * graph layouter - enable / disable for NACs, attr conditions, formula - GUI
 * tuning
 * 
 * Revision 1.37 2007/02/05 12:33:38 olga CPA: chengeAttribute
 * conflict/dependency : attributes with constants bug fixed, but the critical
 * pairs computation has still a gap.
 * 
 * Revision 1.36 2007/01/11 10:21:14 olga Optimized Version 1.5.1beta , free for
 * tests
 * 
 * Revision 1.35 2006/12/13 13:32:59 enrico reimplemented code
 * 
 * Revision 1.34 2006/11/01 11:17:30 olga Optimized agg sources of CSP
 * algorithm, match usability, graph isomorphic copy, node/edge type
 * multiplicity check for injective rule and match
 * 
 * Revision 1.33 2006/03/13 10:04:42 olga CPA tuning
 * 
 * Revision 1.32 2006/03/08 15:51:55 olga CPs : converting critical overlappings
 * in LHS overlappings done
 * 
 * Revision 1.31 2006/03/08 13:26:11 olga private to public
 * 
 * Revision 1.30 2006/03/08 09:14:59 olga CPs mistake fixed
 * 
 * Revision 1.29 2006/03/06 14:47:27 olga CPs ...
 * 
 * Revision 1.28 2006/03/06 12:18:14 olga CPs...
 * 
 * Revision 1.27 2006/03/06 10:52:38 olga CPs...
 * 
 * Revision 1.26 2006/03/06 10:16:32 olga CPs
 * 
 * Revision 1.25 2006/03/06 10:04:06 olga CPs ...
 * 
 * Revision 1.24 2006/03/06 09:36:39 olga CPs tuning
 * 
 * Revision 1.23 2006/03/02 12:03:23 olga CPA: check host graph - done
 * 
 * Revision 1.22 2006/03/01 15:33:08 olga tests
 * 
 * Revision 1.21 2006/03/01 15:28:11 olga tests
 * 
 * Revision 1.20 2006/03/01 14:59:01 olga CPA : new method to get LHS
 * overlappings of critical overlappings
 * 
 * Revision 1.19 2006/03/01 09:55:46 olga - new CPA algorithm, new CPA GUI
 * 
 * Revision 1.18 2006/01/19 14:25:59 olga Comments off
 * 
 * Revision 1.17 2006/01/16 09:44:43 olga tests
 * 
 * Revision 1.16 2005/12/21 14:46:34 olga tests
 * 
 * Revision 1.15 2005/11/30 12:15:12 olga Used run time
 * 
 * Revision 1.14 2005/11/16 14:32:29 olga Time output improved
 * 
 * Revision 1.13 2005/10/24 09:04:49 olga GUI tuning
 * 
 * Revision 1.12 2005/10/19 08:58:45 olga GUI tuning
 * 
 * Revision 1.11 2005/10/13 13:23:05 olga CPA
 * 
 * Revision 1.10 2005/10/12 15:18:14 olga CPA GUI
 * 
 * Revision 1.9 2005/10/12 12:19:01 olga CPA GUI
 * 
 * Revision 1.8 2005/10/12 10:00:56 olga CPA GUI tuning
 * 
 * Revision 1.7 2005/10/10 08:05:16 olga Critical Pair GUI and CPA graph
 * 
 * Revision 1.6 2005/09/27 11:13:25 olga CPs ...
 * 
 * Revision 1.5 2005/09/27 08:41:12 olga CPs GUI tuning.
 * 
 * Revision 1.4 2005/09/26 16:41:20 olga CPA graph, CPs - visualization
 * 
 * Revision 1.3 2005/09/26 08:35:15 olga CPA graph frames; bugs
 * 
 * Revision 1.2 2005/09/19 09:12:14 olga CPA GUI tuning
 * 
 * Revision 1.1 2005/08/25 11:56:58 enrico *** empty log message ***
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
 * Revision 1.35 2005/05/23 09:54:30 olga CPA improved: Stop of generation
 * process or rule pair.
 * 
 * Revision 1.34 2005/04/11 13:06:13 olga Errors during CPA are corrected.
 * 
 * Revision 1.33 2005/03/16 12:02:10 olga
 * 
 * only little changes
 * 
 * Revision 1.32 2005/03/03 13:48:42 olga - Match with NACs and attr. conditions
 * with mixed variables - error corrected - save/load class packages written by
 * user - PACs : creating T-equivalents - improved - save/load matches of the
 * rules (only one match of a rule) - more friendly graph/rule editor GUI - more
 * syntactical checks in attr. editor
 * 
 * Revision 1.31 2005/01/28 14:02:32 olga -Fehlerbehandlung beim Typgraph check
 * -Erweiterung CP GUI / CP Menu -Fehlerbehandlung mit identification option
 * -Fehlerbehandlung bei Rule PAC
 * 
 * Revision 1.30 2004/12/20 14:53:48 olga Changes because of matching
 * optimisation.
 * 
 * Revision 1.29 2004/09/23 08:26:43 olga Fehler bei CPs weg, Debug output in
 * file
 * 
 * Revision 1.28 2004/09/13 10:21:14 olga Einige Erweiterungen und
 * Fehlerbeseitigung bei CPs und Graph Grammar Transformation
 * 
 * Revision 1.27 2004/06/23 08:26:57 olga CPs sind endlich OK.
 * 
 * Revision 1.26 2004/06/21 08:35:34 olga immer noch CPs
 * 
 * Revision 1.25 2004/06/14 12:34:19 olga CP Analyse and Transformation
 * 
 * Revision 1.24 2004/04/15 10:49:48 olga Kommentare
 * 
 * Revision 1.23 2004/01/28 17:58:38 olga Errors suche
 * 
 * Revision 1.22 2003/12/18 16:27:08 olga test.
 * 
 * Revision 1.21 2003/06/26 11:44:40 olga Events-behandlung
 * 
 * Revision 1.20 2003/05/26 15:40:14 olga Test
 * 
 * Revision 1.19 2003/05/23 11:07:15 komm missing import added
 * 
 * Revision 1.18 2003/05/19 08:35:37 komm new option paralell and todos removed
 * 
 * Revision 1.16 2003/03/05 18:24:08 komm sorted/optimized import statements
 * 
 * Revision 1.15 2003/03/03 17:46:16 olga Optimierung
 * 
 * Revision 1.14 2003/02/05 15:53:36 olga GUI
 * 
 * Revision 1.13 2003/02/03 17:49:31 olga Tests
 * 
 * Revision 1.12 2003/01/22 16:21:04 olga Nach dem Laden von CPs bessere
 * Anpassung der Instanz von deieser Klasse
 * 
 * Revision 1.11 2003/01/20 17:04:15 olga Critical Pair Table Anpassung
 * 
 * Revision 1.10 2003/01/20 10:46:29 komm new events for new GUI
 * 
 * Revision 1.9 2003/01/13 14:26:39 komm removed paneTest
 * 
 * Revision 1.8 2002/12/16 09:19:25 olga Die Klasse paneTest, die hier benutzt
 * wird, soll hier raus und eventuell von
 * agg.gui.parser.CriticalPairAnalysisGUI.
 * 
 * Revision 1.7 2002/12/12 09:26:00 olga Fehlerbeseitigung
 * 
 * Revision 1.6 2002/12/05 13:32:05 olga Nur Textausgabe in Statusbar
 * verbessert.
 * 
 * Revision 1.5 2002/12/04 13:58:15 komm removed multithreading, because of
 * unexpected errors
 * 
 * Revision 1.4 2002/11/25 09:49:34 komm neu formatiert
 * 
 * Revision 1.3 2002/11/11 13:51:39 komm TypeException handling
 * 
 * Revision 1.2 2002/11/11 10:43:26 komm added support for multiplicity check
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:23 olga Imported sources
 * 
 * Revision 1.12 2001/08/08 14:46:30 olga Default Layer Option Einstellung ist
 * RCDN_LAYER.
 * 
 * Revision 1.11 2001/08/02 15:22:15 olga Error-Meldungen eingebaut in
 * LayerFunction und die Anzeige dieser Meldungen in GUI.
 * 
 * Revision 1.10 2001/06/26 17:28:03 olga Parser test
 * 
 * Revision 1.9 2001/06/18 13:37:46 olga Bei Critical Pair ein neuer Menuitem:
 * Debug, wo man einzelne Regelpaare testen kann. System.gc() eingefuegt.
 * 
 * Revision 1.8 2001/06/13 16:49:35 olga Parser Classen Optimierung.
 * 
 * Revision 1.7 2001/05/14 12:02:59 olga Zusaetzliche Parser Events Aufrufe
 * eingebaut, um bessere Kommunikation mit GUI Status Anzeige zu ermoeglichen.
 * 
 * Revision 1.6 2001/04/11 14:59:19 olga Stop Method eingefuegt.
 * 
 * Revision 1.5 2001/03/29 12:01:34 olga MorphCompletionStrategy: dangling
 * condition wieder ON
 * 
 * Revision 1.4 2001/03/22 15:53:26 olga Zusaetzliche Events Meldungen
 * eingebaut.
 * 
 * Revision 1.3 2001/03/08 10:42:51 olga Die Parser Version aus parser branch
 * wurde in Head uebernommen.
 * 
 * Revision 1.1.2.18 2001/01/29 07:34:21 shultzke Unbedeutende Aenderungen
 * 
 * Revision 1.1.2.17 2001/01/28 13:14:52 shultzke API fertig
 * 
 * Revision 1.1.2.16 2001/01/14 14:48:20 shultzke commentare ergaenzt
 * 
 * Revision 1.1.2.15 2001/01/11 11:36:07 shultzke Laden und speichern der
 * kritischen Paare geht, es fehlt nur noch das Laden fuer den Parser.
 * 
 * Revision 1.1.2.14 2001/01/10 15:09:51 shultzke load and save fast fertig
 * 
 * Revision 1.1.2.13 2001/01/03 09:45:00 shultzke TODO's bis auf laden und
 * speichern erledigt. Wann meldet sich endlich Michael?
 * 
 * Revision 1.1.2.12 2000/12/12 13:27:43 shultzke erste Versuche kritische Paare
 * mit XML abzuspeichern
 * 
 * Revision 1.1.2.11 2000/12/10 14:55:48 shultzke um Layer erweitert
 * 
 * Revision 1.1.2.10 2000/11/06 14:02:36 shultzke der Parser laeuft, wenn auch
 * langsam
 * ----------------------------------------------------------------------
 * 
 * Revision 1.1.2.9 2000/11/02 14:50:14 shultzke der konfliktteil des parsers
 * wird etwas trickreich
 * 
 * Revision 1.1.2.8 2000/11/01 14:55:25 shultzke conflictfree part fast fertig
 * 
 * Revision 1.1.2.7 2000/11/01 12:19:21 shultzke erste Regelanwendung im parser
 * CVs: ----------------------------------------------------------------------
 * 
 * Revision 1.1.2.6 2000/09/19 20:56:36 shultzke *** empty log message ***
 * 
 * Revision 1.1.2.5 2000/09/18 14:11:22 shultzke erstes Geruest des Parsers
 * erstellt. Und parallelen Ablauf der ExcludePairs synchronisiert
 * 
 * Revision 1.1.2.4 2000/07/18 20:03:55 shultzke Exclude ohne Nac sollten
 * funktionieren
 * 
 * Revision 1.1.2.3 2000/07/16 18:52:28 shultzke *** empty log message ***
 * 
 * Revision 1.1.2.2 2000/07/13 14:42:31 shultzke erste schritte zum container
 * auffuellen
 * 
 * Revision 1.1.2.1 2000/07/12 07:58:39 shultzke merged
 * 
 * Revision 1.2 2000/07/10 15:09:38 shultzke additional representation
 * hinzugefuegt
 * 
 * Revision 1.1 2000/07/09 17:12:54 shultzke grob die GUI eingebunden
 * 
 */
