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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import agg.util.Pair;
import agg.xt_basis.Arc;
import agg.xt_basis.BadMappingException;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.Node;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;

/**
 * This class allows a more comfortable access to the critical pairs of two rules.
 * The critical pairs have to be computed before.<br>
 * <ul>
 * <li>Use <code>PairContainer.getCriticalPairData(Rule, Rule)</code> 
 * to get an instance of this class.</li>
 * <li>Use <code>hasCriticals()</code> and <code>next()</code> 
 * to set the current critical pair.</li>
 * <li>Use <code>getMorph1()</code>, <code>getMorph2()</code>, <code>getMorph(Rule)</code> 
 * to get the first or second embedding of the rule graph into the critical graph.<br>
 * Depending of the critical kind the rule graph may be LHS, RHS or a special created graph.</li>
 * <li>Use <code>getMorph1DueToLHS()</code>, <code>getMorph2DueToLHS()</code>
 * to get the embedding of the LHS of the rule into the critical graph.</li>
 * <li>Use <code>getCriticalDataOfKind(int)</code> to get the pairs of the special conflict/dependency kind.<br>
 * (see for kind: <tt>CriticalPairData.DELETE_USE_CONFLICT, 
 * CriticalPairData.DELETE_NEED_CONFLICT, 
 * CriticalPairData.PRODUCE_FORBID_CONFLICT, 
 * CriticalPairData.DELETE_FORBID_DEPENDENCY, ...</tt>
 * </li>
 * </ul>
 * 
 * @author olga
 */
public class CriticalPairData {

	// kind of conflict
	public static final int DELETE_USE_CONFLICT = 0; // r1.LHS --> r2.LHS
	public static final int DELETE_NEED_CONFLICT = 1; // r1.LHS --> r2.PAC
	public static final int PRODUCE_FORBID_CONFLICT = 2; // r1.RHS --> r2.NAC
	public static final int PRODUCE_EDGE_DELTE_NODE_CONFLICT = 3; // r1.RHS --> r2.LHS
	public static final int CHANGE_ATTR_CONFLICT = 40; // r1.LHS --> r2.LHS
	public static final int CHANGE_USE_ATTR_CONFLICT = 4; // r1.LHS --> r2.LHS
	public static final int CHANGE_NEED_ATTR_CONFLICT = 5; // r1.LHS --> r2.PAC
	public static final int CHANGE_FORBID_ATTR_CONFLICT = 6; // r1.LHS --> r2.NAC
	// kind of dependency
	public static final int DELETE_FORBID_DEPENDENCY = 7; // r1.LHS --> r2.NAC
	public static final int PRODUCE_USE_DEPENDENCY = 8; // r1.RHS --> r2.LHS
	public static final int PRODUCE_NEED_DEPENDENCY = 9;	// r1.RHS --> r2.PAC
	public static final int CHANGE_USE_ATTR_DEPENDENCY = 10; // r1.LHS --> r2.LHS
	public static final int CHANGE_NEED_ATTR_DEPENDENCY = 11; // r1.LHS --> r2.LHS + PAC
	public static final int CHANGE_FORBID_ATTR_DEPENDENCY = 12; // r1.LHS --> r2.NAC
	public static final int PRODUCE_DELETE_DEPENDENCY = 13; // r2 deletes, r1 preserves/produces (DELIVER-DELETE) (Leen, s.143)
	public static final int READ_DELETE_DEPENDENCY = 131; 
	public static final int CREATE_DELETE_DEPENDENCY = 132; 
	public static final int FORBID_PRODUCE_DEPENDENCY = 14; // r2 produces,  r1 forbids 
	public static final int PRODUCE_CHANGE_DEPENDENCY = 15; // r2 changes,  r1 changes
	
	// search text for conflict
	public static final String DELETE_USE_C_TXT = "delete-use-conflict";
	public static final String DELETE_NEED_C_TXT = "delete-need-conflict";
	public static final String PRODUCE_FORBID_C_TXT = "produce-forbid-conflict";
	public static final String PRODUCE_EDGE_DELETE_NODE_C_TXT = "produceEdge-deleteNode-conflict";
	public static final String CHANGE_USE_ATTR_C_TXT = "change-use-attr-conflict";
	public static final String CHANGE_NEED_ATTR_C_TXT = "change-need-attr-conflict";
	public static final String CHANGE_FORBID_ATTR_C_TXT = "change-forbid-attr-conflict";
	// search text for dependency
	public static final String PRODUCE_USE_D_TXT = "produce-use-dependency";
	public static final String PRODUCE_NEED_D_TXT = "produce-need-dependency";
	public static final String DELETE_FORBID_D_TXT = "delete-forbid-dependency";
	public static final String CHANGE_USE_ATTR_D_TXT = "change-use-attr-dependency";
	public static final String CHANGE_NEED_ATTR_D_TXT = "change-need-attr-dependency";
	public static final String CHANGE_FORBID_ATTR_D_TXT = "change-forbid-attr-dependency";
	public static final String PRODUCE_DELETE_D_TXT = "deliver-delete-dependency"; //"delete-switch-dependency"
	public static final String FORBID_PRODUCE_D_TXT = "forbid-produce-dependency"; //"forbid-switch-dependency"	
	public static final String PRODUCE_CHANGE_D_TXT = "deliver-change-dependency";  //"change-switch-dependency" or "change-change-dependency"
	
	// old code	
	public static final String DELETE_SWITCH_D_TXT = "delete-switch-dependency";
	public static final String FORBID_SWITCH_D_TXT = "forbid-switch-dependency";
	public static final String CHANGE_SWITCH_D_TXT = "change-switch-dependency";

	
	private Rule r1; 
	private Rule r2;
	
	private List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	overlaps;
	
	private Iterator<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	iterator;
	
	private Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
	pair;

	private HashMap<Integer,List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>
	map;
	
	private HashMap<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>,Integer>
	map2;
	
	int size = 0;
	
	/**
	 * Constructs an instance of <tt>CriticalPairData</tt> with the specified rules
     * and a list of critical overlapping pairs.
	 */
	public CriticalPairData(
			final Rule rule1, 
			final Rule rule2,
			final List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
			overlappings) {
		
		this.r1 = rule1;
		this.r2 = rule2;
		this.overlaps = overlappings;
		this.iterator = this.overlaps.iterator();
		this.size = overlappings.size();
		this.map = new HashMap<Integer,List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>();
		this.map2 = new HashMap<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>,Integer>();
		this.fillMap(overlappings);
	}
	
	/**
	 * Resets critical pairs iterator to be able to start from the beginning.
	 */
	public void resetIterator() {
		this.iterator = this.overlaps.iterator();
	}
	
	/**
	 * Returns the first rule.
	 */
	public Rule getRule1() {
		return this.r1;
	}
	
	/**
	 * Returns the second rule.
	 */
	public Rule getRule2() {
		return this.r2;
	}
	
	/**
	 * Returns the kind of the current critical overlapping pair.
	 * (see for kind: <tt>CriticalPairData.DELETE_USE_CONFLICT, 
	 * CriticalPairData.DELETE_NEED_CONFLICT, 
	 * CriticalPairData.PRODUCE_FORBID_CONFLICT, 
	 * CriticalPairData.DELETE_FORBID_DEPENDENCY, ...</tt>
	 */
	public int getKindOfCurrentCritical() {
		if (this.pair != null) {
			Integer k = this.map2.get(this.pair);
			if (k != null)
				return k.intValue();
		}
		return -1;
	}
	
	/**
	 * Checks whether the critical pairs contain conflicts or dependencies of the specified kind.
	 * (see for kind: <tt>CriticalPairData.DELETE_USE_CONFLICT, 
	 * CriticalPairData.DELETE_NEED_CONFLICT, 
	 * CriticalPairData.PRODUCE_FORBID_CONFLICT, 
	 * CriticalPairData.DELETE_FORBID_DEPENDENCY, ...</tt>
	 */
	public boolean hasCriticalsOfKind(int kind) {
		if (this.map.get(Integer.valueOf(kind)) != null)
			return true;
		return false;
	}
	
	/**
	 * Returns critical pairs of the specified kind.
	 * (see for kind: <tt>CriticalPairData.DELETE_USE_CONFLICT, 
	 * CriticalPairData.DELETE_NEED_CONFLICT, 
	 * CriticalPairData.PRODUCE_FORBID_CONFLICT, 
	 * CriticalPairData.DELETE_FORBID_DEPENDENCY, ...</tt>
	 */
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getCriticalsOfKind(int kind) {
		if (kind == -1) {
			if (!this.map.isEmpty()) {
				return this.map.values().iterator().next();
			}
		}
		
		return this.map.get(Integer.valueOf(kind));
	}
	
	/**
	 * Returns the critical pair data of the special conflict/dependency kind.<br>
	 * (see for kind: <tt>CriticalPairData.DELETE_USE_CONFLICT, 
	 * CriticalPairData.DELETE_NEED_CONFLICT, 
	 * CriticalPairData.PRODUCE_FORBID_CONFLICT, 
	 * CriticalPairData.DELETE_FORBID_DEPENDENCY, ...</tt>
	 */
	public CriticalPairData getCriticalDataOfKind(int kind) {
		Integer key = Integer.valueOf(kind);
		List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
		l = this.map.get(key);
		if (l != null) {
			return new CriticalPairData(this.r1, this.r2, l);
		} 
			
		return null;
	}
	
	public int getSize() {
		return this.size;
	}
	
	public int getSizeOf(int kind) {
		List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		list = this.map.get(Integer.valueOf(kind));
		return (list != null)? list.size(): 0;
	}
	
	/**
	 * Checks whether the next critical overlapping pair exists.
	 */
	public boolean hasCriticals() {
		return this.iterator.hasNext();
	}
	
	/**
	 * Sets the next available critical pair to be the current pair.
	 */
	public boolean next() {
		if (this.iterator.hasNext()) {
			this.pair = this.iterator.next();
			return true;
		} else
			return false;
	}
	
	/**
	 * Returns the graph embedding of the first rule into the critical graph
	 * of the current overlapping pair.
	 */
	public OrdinaryMorphism getMorph1() {
		if (this.pair == null && this.iterator.hasNext()) 
			this.pair = this.iterator.next();
		
		if (this.pair != null) {
			return this.pair.first.first;
		}
		
		return null;
	}
	
	/**
	 * Returns the graph embedding of the second rule into the critical graph
	 * of the current overlapping pair.
	 */
	public OrdinaryMorphism getMorph2() {
		if (this.pair == null && this.iterator.hasNext()) 
			this.pair = this.iterator.next();
		
		if (this.pair != null) {
			return this.pair.first.second;
		}
		
		return null;
	}
	
	/**
	 * Returns the graph embedding of the specified rule into the critical graph
	 * of the current critical overlapping pair.
	 */
	public OrdinaryMorphism getMorph(final Rule r) {
		if (r == this.r1) 
			return getMorph1();
		else if (r == this.r2) 
			return getMorph2();
		else
			return null;
	}
	
	/**
	 * Returns the LHS graph embedding of the first rule into the critical graph
	 * of the current overlapping pair.
	 */
	public OrdinaryMorphism getMorph1DueToLHS() {
		if (this.pair == null && this.iterator.hasNext()) 
			this.pair = this.iterator.next();
		
		if (this.pair != null) {
			if (this.pair.second == null)
				return this.pair.first.first;
			else {
				return this.adjustMorph1(this.pair.first.first);
//				return this.adjustMorph1(this.pair.first.first, this.pair.second.first);
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the LHS graph embedding of the second rule into the critical graph
	 * of the current overlapping pair.
	 */
	public OrdinaryMorphism getMorph2DueToLHS() {
		if (this.pair == null && this.iterator.hasNext()) 
			this.pair = this.iterator.next();
		
		if (this.pair != null) {
			if (this.pair.second == null)
				return this.pair.first.second;
			else if (this.r2.getLeft() == this.pair.second.first.getSource()
					&& this.pair.first.second.getSource() == this.pair.second.first.getTarget()) {
				return this.adjustMorph2(this.pair.first.second, this.pair.second.first);
			}
			else if (this.r2.getLeft() == this.pair.first.second.getSource() 
					&& this.pair.second.second != null
					&& this.pair.second.first.getTarget() == this.pair.second.second.getSource()) {
				return this.pair.first.second;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns a NAC graph embedding of the second rule into the critical graph
	 * of the current overlapping pair.
	 */
	public OrdinaryMorphism getMorph2DueToNAC() {
		if (this.pair == null && this.iterator.hasNext()) 
			this.pair = this.iterator.next();
		
		if (this.pair != null) {
			if (this.pair.second == null)
				return null;
			else if (this.r2.getLeft() == this.pair.second.first.getSource()
					&& this.pair.first.second.getSource() == this.pair.second.second.getTarget()) {
				return this.adjustMorph2NAC(this.pair.first.second, this.pair.second.second);
			}
		}
		
		return null;
	}
	
	/**
	 * If existing, returns a PAC graph embedding of the second rule into the critical graph
	 * of the current overlapping pair,
	 * otherwise - NULL.
	 */
	public OrdinaryMorphism getMorph2DueToPAC() {
		if (this.pair == null && this.iterator.hasNext()) 
			this.pair = this.iterator.next();
		
		if (this.pair != null) {
			if (this.pair.second == null)
				return null;
			else {
				/*
				int hlhs2 = this.r2.getLeft().hashCode();
				OrdinaryMorphism pac = this.r2.getPAC("Pac12");
				int hpacsrc = pac.getSource().hashCode();
				int hpactrg = pac.getTarget().hashCode();
				
				int hp21src = this.pair.second.first.getSource().hashCode(); // PAC graph
				int hp21trg = this.pair.second.first.getTarget().hashCode();
				int hp22src = this.pair.second.second.getSource().hashCode();
				int hp22trg = this.pair.second.second.getTarget().hashCode();
				int hp12src = this.pair.first.second.getSource().hashCode();  // LHS2
				int hp12trg = this.pair.first.second.getTarget().hashCode();
				
				int hp11src = this.pair.first.first.getSource().hashCode();
				int hp11trg = this.pair.first.first.getTarget().hashCode();
				*/
				if (this.r2.getLeft() == this.pair.first.second.getSource()		
						&& this.pair.second.first.getTarget() == this.pair.second.second.getSource()) {
					return this.adjustMorph2PAC(this.pair.first.second, this.pair.second.first, this.pair.second.second);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the critical graph of the current critical overlapping pair.
	 */
	public Graph getCriticalGraph() {
		if (this.pair == null && this.iterator.hasNext()) 
			this.pair = this.iterator.next();
		
		if (this.pair != null) {
			return this.pair.first.first.getTarget();
		}
		return null;
	}
	
	/**
	 * Returns the overlapping graph objects of the current critical overlapping pair.
	 * The result list contains at least the critical graph objects.
	 */
	public List<GraphObject> getOverlapGraphObjects() {
		if (this.pair == null && this.iterator.hasNext()) 
			this.pair = this.iterator.next();
		
		if (this.pair != null) {
			List<GraphObject> list = new Vector<GraphObject>();
			Graph g = this.pair.first.first.getTarget();
			
			Iterator<Node> nodes = g.getNodesCollection().iterator();
			while (nodes.hasNext()) {
				GraphObject go = nodes.next();
				if (this.pair.first.first.getInverseImage(go).hasMoreElements()
						&& this.pair.first.second.getInverseImage(go).hasMoreElements()) {
					list.add(go);
				}
			}
			Iterator<Arc> arcs = g.getArcsCollection().iterator();
			while (arcs.hasNext()) {
				GraphObject go = arcs.next();
				if (this.pair.first.first.getInverseImage(go).hasMoreElements()
						&& this.pair.first.second.getInverseImage(go).hasMoreElements()) {
					list.add(go);
				}
			}
			return list;
		}
		return null;
	}
	
	/**
	 * Returns the critical graph objects of the current critical overlapping pair.
	 * The result list only contains the graph objects which determine the critical place of the graph.
	 */
	public List<GraphObject> getCriticalGraphObjects() {
		if (this.pair == null && this.iterator.hasNext()) 
			this.pair = this.iterator.next();
		
		if (this.pair != null) {
			List<GraphObject> list = new Vector<GraphObject>();
			Graph g = this.pair.first.first.getTarget();
			
			Iterator<Node> nodes = g.getNodesCollection().iterator();
			while (nodes.hasNext()) {
				GraphObject go = nodes.next();
				if (this.pair.first.first.getInverseImage(go).hasMoreElements()
						&& this.pair.first.second.getInverseImage(go).hasMoreElements()
						&& go.isCritical()) {
					list.add(go);
				}
			}
			Iterator<Arc> arcs = g.getArcsCollection().iterator();
			while (arcs.hasNext()) {
				GraphObject go = arcs.next();
				if (this.pair.first.first.getInverseImage(go).hasMoreElements()
						&& this.pair.first.second.getInverseImage(go).hasMoreElements()
						&& go.isCritical()) {
					list.add(go);
				}
			}
			return list;
		}
		return null;
	}
	
	private OrdinaryMorphism adjustMorph1(final OrdinaryMorphism m1) {
		
		if (m1.getSource() == this.r1.getRight()) {
			OrdinaryMorphism om = BaseFactory.theFactory().createMorphism(
												this.r1.getLeft(), m1.getTarget());
			Enumeration<GraphObject> dom = m1.getDomain();
			while (dom.hasMoreElements()) {
				GraphObject go = dom.nextElement();
				Enumeration<GraphObject> inverse = this.r1.getInverseImage(go);
				if (inverse.hasMoreElements()) {
					GraphObject goL = inverse.nextElement();
					try {
						om.addMapping(goL, m1.getImage(go));
					} catch (BadMappingException ex) {
						return null;
					}
				} 	
			}
			return om;
		}
		return m1;
	}
	
	@SuppressWarnings("unused")
	private OrdinaryMorphism adjustMorph1(
			final OrdinaryMorphism m1, final OrdinaryMorphism help1) {
		
		if (m1.getSource() == this.r1.getRight()) {
			OrdinaryMorphism om = BaseFactory.theFactory().createMorphism(
												this.r1.getLeft(), m1.getTarget());
			Enumeration<GraphObject> dom = m1.getDomain();
			while (dom.hasMoreElements()) {
				GraphObject go = dom.nextElement();
				Enumeration<GraphObject> inverse = this.r1.getInverseImage(go);
				if (inverse.hasMoreElements()) {
					GraphObject goL = inverse.nextElement();
					try {
						om.addMapping(goL, m1.getImage(go));
					} catch (BadMappingException ex) {
						return null;
					}
				} 	
			}
			return om;
		}
		return m1;
	}
	
	private OrdinaryMorphism adjustMorph2(
			final OrdinaryMorphism m2, final OrdinaryMorphism help2) {
		
		if (m2.getSource() != this.r2.getLeft() 
				&& m2.getSource() == help2.getTarget()
				&& this.r2.getLeft() == help2.getSource()) {
			OrdinaryMorphism om = BaseFactory.theFactory().createMorphism(
											this.r2.getLeft(), m2.getTarget());		
			Enumeration<GraphObject> dom = m2.getDomain();
			while (dom.hasMoreElements()) {
				GraphObject go = dom.nextElement();
				Enumeration<GraphObject> inverse = help2.getInverseImage(go);
				if (inverse.hasMoreElements()) {
					GraphObject goL = inverse.nextElement(); 
					try {
						om.addMapping(goL, m2.getImage(go));
					} catch (BadMappingException ex) {
						return null;
					}
				}
			}
			return om;
		}
		return m2;
	}
	
	private OrdinaryMorphism adjustMorph2NAC(
			final OrdinaryMorphism m2, final OrdinaryMorphism help2) {
		
		if (m2.getSource() == help2.getTarget()
				&& this.r2.hasNAC(help2.getSource())) {
			
			OrdinaryMorphism om = BaseFactory.theFactory().createMorphism(
											help2.getSource(), m2.getTarget());		
			Enumeration<GraphObject> dom = m2.getDomain();
			while (dom.hasMoreElements()) {
				GraphObject go = dom.nextElement();
				Enumeration<GraphObject> inverse = help2.getInverseImage(go);
				if (inverse.hasMoreElements()) {
					GraphObject goN = inverse.nextElement(); 
					try {
						om.addMapping(goN, m2.getImage(go));
					} catch (BadMappingException ex) {
						return null;
					}
				}
			}
			return om;
		}
		return m2;
	}
	
	private OrdinaryMorphism adjustMorph2PAC(
			final OrdinaryMorphism m2, final OrdinaryMorphism help1, final OrdinaryMorphism help2) {
		
		if (help1.getTarget() == help2.getSource()
				&& this.r2.hasPAC(help1.getSource())) {
			
			OrdinaryMorphism om = BaseFactory.theFactory().createMorphism(
											help1.getSource(), m2.getTarget());		
			Enumeration<GraphObject> dom = help1.getDomain();
			while (dom.hasMoreElements()) {
				GraphObject go = dom.nextElement();
				GraphObject img = help2.getImage(help1.getImage(go));
				if (img != null) {
					try {
						om.addMapping(go, img);
					} catch (BadMappingException ex) {
						return null;
					}
				}
			}
			return om;
		}
		return null;
	}
	
	/*
	 * Tries to construct a left application condition : r.LHS --> criticalGraph.
	 * The specified rule is one of the rules of this rule pair.
	 * The criticalGraph is the target graph of the current critical pair.<br>
	 * Use <tt>Rule.addNAC(OrdinaryMorphism)</tt> to be able to use the result as a NAC.  
	 */
	@SuppressWarnings("unused")
	private OrdinaryMorphism makeLeftACFromCriticalGraph(final Rule r) {
		if (this.pair == null && this.iterator.hasNext()) 
			this.pair = this.iterator.next();
		
		if (this.pair != null) {
			if (r == this.r1) 		
				return ExcludePairHelper.makeLeftACFromGraph(this.pair, r, true, false);
			else if (r == this.r2) 		
				return ExcludePairHelper.makeLeftACFromGraph(this.pair, r, false, false);
		}
		
		return null;
	}
	
	
	private void fillMap(
			final List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
			list) {
		
		for (int i=0; i<list.size(); i++) {
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
			p = list.get(i);
			String gname = p.first.first.getTarget().getName(); 
			
			if (addToList(p, gname, CriticalPairData.DELETE_USE_C_TXT, DELETE_USE_CONFLICT));
			else if (addToList(p, gname, CriticalPairData.DELETE_NEED_C_TXT, DELETE_NEED_CONFLICT));
			else if (addToList(p, gname, CriticalPairData.PRODUCE_FORBID_C_TXT, PRODUCE_FORBID_CONFLICT));
			else if (addToList(p, gname, CriticalPairData.PRODUCE_EDGE_DELETE_NODE_C_TXT, PRODUCE_EDGE_DELTE_NODE_CONFLICT));
			else if (addToList(p, gname, CriticalPairData.CHANGE_USE_ATTR_C_TXT, CHANGE_USE_ATTR_CONFLICT));
			else if (addToList(p, gname, CriticalPairData.CHANGE_NEED_ATTR_C_TXT, CHANGE_NEED_ATTR_CONFLICT));
			else if (addToList(p, gname, CriticalPairData.CHANGE_FORBID_ATTR_C_TXT, CHANGE_FORBID_ATTR_CONFLICT));
			
			else if (addToList(p, gname, CriticalPairData.DELETE_FORBID_D_TXT, DELETE_FORBID_DEPENDENCY));
			else if (addToList(p, gname, CriticalPairData.PRODUCE_USE_D_TXT, PRODUCE_USE_DEPENDENCY));
			else if (addToList(p, gname, CriticalPairData.PRODUCE_NEED_D_TXT, PRODUCE_NEED_DEPENDENCY));
			else if (addToList(p, gname, CriticalPairData.CHANGE_USE_ATTR_D_TXT, CHANGE_USE_ATTR_DEPENDENCY));
			else if (addToList(p, gname, CriticalPairData.CHANGE_FORBID_ATTR_D_TXT, CHANGE_FORBID_ATTR_DEPENDENCY));
			else if (addToList(p, gname, CriticalPairData.CHANGE_NEED_ATTR_D_TXT, CHANGE_NEED_ATTR_DEPENDENCY));
			
			else if (addToList(p, gname, CriticalPairData.PRODUCE_DELETE_D_TXT, PRODUCE_DELETE_DEPENDENCY)) {
				addToList_(p, gname, CriticalPairData.PRODUCE_DELETE_D_TXT, READ_DELETE_DEPENDENCY);
				addToList_(p, gname, CriticalPairData.PRODUCE_DELETE_D_TXT, CREATE_DELETE_DEPENDENCY);
			}
			else if (addToList(p, gname, CriticalPairData.FORBID_PRODUCE_D_TXT, FORBID_PRODUCE_DEPENDENCY));			
			else if (addToList(p, gname, CriticalPairData.PRODUCE_CHANGE_D_TXT, PRODUCE_CHANGE_DEPENDENCY));
			
			// old code
			else if (addToList(p, gname, CriticalPairData.DELETE_SWITCH_D_TXT, PRODUCE_DELETE_DEPENDENCY));
			else if (addToList(p, gname, CriticalPairData.FORBID_SWITCH_D_TXT, FORBID_PRODUCE_DEPENDENCY));			
			else if (addToList(p, gname, CriticalPairData.CHANGE_SWITCH_D_TXT, PRODUCE_CHANGE_DEPENDENCY));
		}
	}
	
	private boolean addToList(
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p,
			String graphName,
			String searchTxt,
			int kind) {
	
		if (graphName.indexOf(searchTxt) != -1) {
			Integer key = Integer.valueOf(kind);
			map2.put(p, key);
			List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
			l = this.map.get(key);
			if (l == null) {
				l = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
				this.map.put(key, l);
			}
			l.add(p);
			return true;
		}
		return false;
	}
	
	private boolean addToList_(
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p,
			String graphName,
			String searchTxt,
			int kind) {
	
		if (graphName.indexOf(searchTxt) != -1) {
//			System.out.println(r1.getQualifiedName()+"  ,  "+r2.getQualifiedName());
			if (kind == READ_DELETE_DEPENDENCY) {
				Integer key = Integer.valueOf(kind);
				List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
				l1 = null; 
				OrdinaryMorphism m1 = p.first.second;
				OrdinaryMorphism m2 = p.first.first;
				Enumeration<GraphObject> dom1 = m1.getDomain();
				while (dom1.hasMoreElements()) {
					GraphObject o1 = dom1.nextElement();
					GraphObject o = m1.getImage(o1);
					if (m2.getInverseImage(o).hasMoreElements()) {
						if (o.isCritical() && r1.getInverseImage(o1).hasMoreElements()) {
							map2.put(p, key);	
							l1 = this.map.get(key);							
							if (l1 == null) {
								l1 = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
								this.map.put(key, l1);
							}
							l1.add(p);
						}
					}
				}
				if (l1 != null) {
//					System.out.println("READ_DELETE_DEPENDENCY");
					return true;
				}
			}
			else 
			if (kind == CREATE_DELETE_DEPENDENCY) {
				Integer key = Integer.valueOf(kind);
				List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
				l1 = null; 
				OrdinaryMorphism m1 = p.first.second;
				OrdinaryMorphism m2 = p.first.first;
				Enumeration<GraphObject> dom1 = m1.getDomain();
				while (dom1.hasMoreElements()) {
					GraphObject o1 = dom1.nextElement();
					GraphObject o = m1.getImage(o1);
					if (m2.getInverseImage(o).hasMoreElements()) {
						if (o.isCritical() && !r1.getInverseImage(o1).hasMoreElements()) {
							map2.put(p, key);	
							l1 = this.map.get(key);							
							if (l1 == null) {
								l1 = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
								this.map.put(key, l1);
							}
							l1.add(p);
						}
					}
				}
				if (l1 != null) {
//					System.out.println("CREATE_DELETE_DEPENDENCY");
					return true;
				}
			}
		}
		return false;
	}
	
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getDeleteUseConflicts() {
		return this.map.get(Integer.valueOf(CriticalPairData.DELETE_USE_CONFLICT));
	}
	
	/*
	 * PAC
	 */
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getDeleteNeedConflicts() {
		return this.map.get(Integer.valueOf(CriticalPairData.DELETE_NEED_CONFLICT));
	}
	
	/*
	 * NAC
	 */
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getProduceForbidConflicts() {
		return this.map.get(Integer.valueOf(CriticalPairData.PRODUCE_FORBID_CONFLICT));
	}
	
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getChangeUseAttributeConflicts() {
		return this.map.get(Integer.valueOf(CriticalPairData.CHANGE_USE_ATTR_CONFLICT));
	}
	
	/*
	 * PAC
	 */
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getChangeNeedAttributeConflicts() {
		return this.map.get(Integer.valueOf(CriticalPairData.CHANGE_NEED_ATTR_CONFLICT));
	}
	
	/*
	 * NAC
	 */
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getChangeForbidAttributeConflicts() {
		return this.map.get(Integer.valueOf(CriticalPairData.CHANGE_FORBID_ATTR_CONFLICT));
	}
	
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getProduceEdgeDeleteNodeConflicts() {
		return this.map.get(Integer.valueOf(CriticalPairData.PRODUCE_EDGE_DELTE_NODE_CONFLICT));
	}
	
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getProduceUseDependencies() {
		return this.map.get(Integer.valueOf(CriticalPairData.PRODUCE_USE_DEPENDENCY));
	}
	
	/*
	 * PAC
	 */
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getProduceNeedDependencies() {
		return this.map.get(Integer.valueOf(CriticalPairData.PRODUCE_NEED_DEPENDENCY));
	}
	
	/*
	 * NAC
	 */
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getDeleteForbidDependencies() {
		return this.map.get(Integer.valueOf(CriticalPairData.DELETE_FORBID_DEPENDENCY));
	}
	
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getChangeUseDependencies() {
		return this.map.get(Integer.valueOf(CriticalPairData.CHANGE_USE_ATTR_DEPENDENCY));
	}
	
	/*
	 * NAC
	 */
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getChangeForbidDependencies() {
		return this.map.get(Integer.valueOf(CriticalPairData.CHANGE_FORBID_ATTR_DEPENDENCY));
	}
	
	/*
	 * PAC
	 */
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getChangeNeedDependencies() {
		return this.map.get(Integer.valueOf(CriticalPairData.CHANGE_NEED_ATTR_DEPENDENCY));
	}
	
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getDeliverDeleteDependencies() {
		return this.map.get(Integer.valueOf(CriticalPairData.PRODUCE_DELETE_DEPENDENCY));
	}
	
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getForbidProduceDependencies() {
		return this.map.get(Integer.valueOf(CriticalPairData.FORBID_PRODUCE_DEPENDENCY));
	}
	
	public List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
	getChangeChangeDependencies() {
		return this.map.get(Integer.valueOf(CriticalPairData.PRODUCE_CHANGE_DEPENDENCY));
	}
	
	
	public static boolean isSwitchDependency(String str) {
		if (str.indexOf(PRODUCE_DELETE_D_TXT) >= 0 
				|| str.indexOf(FORBID_PRODUCE_D_TXT) >= 0
				|| str.indexOf(PRODUCE_CHANGE_D_TXT) >= 0)
			return true;
		return false;
	}
	
	
}
