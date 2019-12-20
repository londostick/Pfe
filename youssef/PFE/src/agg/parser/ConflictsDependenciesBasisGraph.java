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

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import agg.parser.ExcludePairContainer;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.GraGra;
import agg.xt_basis.Graph;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.TypeSet;
import agg.xt_basis.Rule;
import agg.xt_basis.Type;
import agg.xt_basis.TypeException;
import agg.xt_basis.Node;
import agg.xt_basis.Arc;
import agg.xt_basis.agt.RuleScheme;
import agg.attribute.facade.impl.DefaultInformationFacade;
import agg.attribute.facade.InformationFacade;
import agg.attribute.impl.ValueTuple;
import agg.attribute.AttrType;
import agg.attribute.handler.AttrHandler;
import agg.util.Pair;

public class ConflictsDependenciesBasisGraph {

	ExcludePairContainer conflictCont;

	ExcludePairContainer dependCont;

	GraGra grammar;

	Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	conflicts;

	Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	dependencies;

	Graph conflictGraph, dependGraph, combiGraph;

	Hashtable<Node, Rule> node2rule;
	
	
	public ConflictsDependenciesBasisGraph(
									ExcludePairContainer conflictsContainer,
									ExcludePairContainer dependenciesContainer) {
		this.conflictCont = conflictsContainer;
		this.dependCont = dependenciesContainer;
		initTables();
		createGraphs();
		if (this.combiGraph != null)
			optimizeLayout(this.combiGraph);
		else if (this.conflictGraph != null)
			optimizeLayout(this.conflictGraph);
		else if (this.dependGraph != null)
			optimizeLayout(this.dependGraph);
	}

	public ConflictsDependenciesBasisGraph(ExcludePairContainer conflictsContainer,
											ExcludePairContainer dependenciesContainer, 
											Graph combiCPAgraph) {
		this.conflictCont = conflictsContainer;
		this.dependCont = dependenciesContainer;
		initTables();
		if (combiCPAgraph != null && !combiCPAgraph.isEmpty()) {
			this.combiGraph = combiCPAgraph;
			createGraphs(this.combiGraph);
		}
		else
			createGraphs();
		if (this.combiGraph != null)
			optimizeLayout(this.combiGraph);
		else if (this.conflictGraph != null)
			optimizeLayout(this.conflictGraph);
		else if (this.dependGraph != null)
			optimizeLayout(this.dependGraph);
	}
	
	/**
	 * Returns conflict graph. 
	 * The nodes represent the rules and the edges - conflict relations.
	 */
	public Graph getConflictsGraph() {
		return this.conflictGraph;
	}

	/**
	 * Returns dependency graph. 
	 * The nodes represent the rules and the edges - dependency relations.
	 */
	public Graph getDependenciesGraph() {
		return this.dependGraph;
	}

	/**
	 * Returns conflict-dependency graph. 
	 * The nodes represent the rules and the edges - conflict and dependency relations.
	 */
	public Graph getConflictsDependenciesGraph() {
		return this.combiGraph;
	}

	/**
	 * Replace the nodes representing a rule scheme by only one node. 
	 * The name of the new node is the name of the rule scheme.
	 * The edges of replaced nodes will be redirected to/from the new node. 
	 * This method should be used before <code>get...Graph()</code> methods.
	 * 
	 * @param g
	 * 		a graph which combines conflict and dependency between rule nodes 
	 */
	@SuppressWarnings("unused")
	private void collapseRuleSchemes(Graph g) {
		if (g != null) {
			collapseRuleSchemes(g, "c");
			collapseRuleSchemes(g, "d");		
		}
	}
	
	/**
	 * Replace the nodes representing a rule scheme by only one node. 
	 * The name of the new node is the name of the rule scheme.
	 * The edges of replaced nodes will be redirected to/from the new node. 
	 * This method should be used before <code>get...Graph()</code> methods.
	 * 
	 * @param g
	 * 		a graph which contains conflict or dependency edges between rule nodes
	 * @param tname
	 * 		the name of the arc type
	 */
	public void collapseRuleSchemes(Graph g, String tname) {
		if (this.node2rule == null) 
			return;
		
		Hashtable<RuleScheme, List<Node>> map = new Hashtable<RuleScheme, List<Node>>();
		Iterator<Node> iter = g.getNodesSet().iterator();
		while (iter.hasNext()) {
			Node n = iter.next();
			Rule r = this.node2rule.get(n);
			if (r != null && r.getRuleScheme() != null) {
				List<Node> l = map.get(r.getRuleScheme());
				if (l == null) {
					l = new Vector<Node>(5);
					map.put(r.getRuleScheme(), l);
				}
				if (!l.contains(n))
					l.add(n);
			}
		}
		Type nt = g.getTypeSet().getTypeByName("RuleScheme");
		if (nt == null) {
			nt = g.getTypeSet().createNodeType(true);
			nt.setStringRepr("RuleScheme");
			nt.setAdditionalRepr("[NODE]");
			nt.getAttrType().addMember(
				DefaultInformationFacade.self().getJavaHandler(), "String", "name");
		}

		List<Arc> ll = new Vector<Arc>();
		
		Iterator<RuleScheme> rsIter = map.keySet().iterator();
		while (rsIter.hasNext()) {
			RuleScheme rs = rsIter.next();
			List<Node> l = map.get(rs);
			try {
				Node rsn = g.createNode(nt);
				ValueTuple vt = (ValueTuple) rsn.getAttribute();
				vt.getValueMemberAt("name").setExprAsObject(rs.getName());

				for (Node n : l) {
					Iterator<Arc> arcsIn = n.getIncomingArcsSet().iterator();
					while (arcsIn.hasNext()) {
						Arc a = arcsIn.next();	
						if (!ll.contains(a))
							ll.add(a);
						if (a.getType().getName().equals(tname)) {
							if (!a.isDirected()) {
								if (g.getArcs(a.getSource(), rsn) == null)
									g.createArc(a.getType(), (Node)a.getSource(), rsn);
								if (g.getArcs(rsn, a.getSource()) == null) 	
									g.createArc(a.getType(), rsn, (Node)a.getSource());
							}
							else if (a.isVisible() && a.isDirected()) {
								if (g.getArcs(a.getSource(), rsn) == null)						
									g.createArc(a.getType(), (Node)a.getSource(), rsn);	
							}
						}
					}

					Iterator<Arc> arcsOut = n.getOutgoingArcsSet().iterator();
					while (arcsOut.hasNext()) {
						Arc a = arcsOut.next();
						if (a.isLoop()) {
							continue;
						}
						if (!ll.contains(a))
							ll.add(a);
						if (a.getType().getName().equals(tname)) {
							if (!a.isDirected()) { 
								if (g.getArcs(rsn, a.getTarget()) == null)
									g.createArc(a.getType(), rsn, (Node)a.getTarget());
								if (g.getArcs(a.getTarget(), rsn) == null)
									g.createArc(a.getType(), (Node)a.getTarget(), rsn);
							}
							else if (a.isVisible() && a.isDirected()) { 
								if (g.getArcs(rsn, a.getTarget()) == null)
									g.createArc(a.getType(), rsn, (Node)a.getTarget());
							}
						}
					}	
				}
			} catch (TypeException e) {}
		}
		
		for (Arc a : ll) {
			try {
				g.destroyArc(a, false);
			} catch (TypeException e) {}
		}
		ll.clear();
		
		List<Arc> la = new Vector<Arc>();
		List<Arc> arcs = new Vector<Arc>(g.getArcsSet());
		for (Arc a : arcs) {
			if (!a.getType().getName().equals(tname)) {
				if (!la.contains(a))
					la.add(a);
			}
			else {
				if (!a.isVisible()) {
					if (!la.contains(a))
						la.add(a);
				}		
				if (!a.isDirected()) {			
					try {
						g.createArc(a.getType(),  (Node)a.getSource(), (Node)a.getTarget());
						g.createArc(a.getType(), (Node)a.getTarget(),  (Node)a.getSource());
						if (!la.contains(a))
							la.add(a);
					} catch (TypeException e) {}
				}
			}
		}
		for (Arc a : la) {
			try {
				g.destroyArc(a, false);
			} catch (TypeException e) {}
		}
		la.clear();
		
		Iterator<List<Node>> ln = map.values().iterator();
		while (ln.hasNext()) {
			List<Node> l = ln.next();
			for (Node n : l) {
				try {
					g.destroyNode(n, false);
				} catch (TypeException e) {}
			}
		}
	}
	
	private void initTables() {
		if (this.conflictCont != null) {
			this.conflicts = this.conflictCont.getExcludeContainer();
			this.grammar = this.conflictCont.getGrammar();
		}
		if (this.dependCont != null) {
			this.dependencies = this.dependCont.getExcludeContainer();
			if (this.grammar == null)
				this.grammar = this.dependCont.getGrammar();
		}
	}

	private void createGraphs() {
		if ((this.conflicts == null) && (this.dependencies == null))
			return;
		
		Hashtable<String, Node> common = new Hashtable<String, Node>();
		Hashtable<String, Node> local = new Hashtable<String, Node>();
		node2rule = new Hashtable<Node, Rule>();

		TypeSet types = null;
		if (this.conflicts != null) {
			this.conflictGraph = BaseFactory.theFactory().createGraph();
			this.conflictGraph.setName("Conflicts of Rules");
			types = this.conflictGraph.getTypeSet();
		}
		if (this.dependencies != null) {
			if (types != null)
				this.dependGraph = BaseFactory.theFactory().createGraph(types);
			else {
				this.dependGraph = BaseFactory.theFactory().createGraph();
				types = this.dependGraph.getTypeSet();
			}
			this.dependGraph.setName("Dependencies of Rules");			
		}
		if (this.conflictGraph != null && this.dependGraph != null) {
			this.combiGraph = BaseFactory.theFactory().createGraph(types);
			this.combiGraph.setName("CPA Graph: Conflicts (red) - Dependencies (blue) of Rules");
		}
		if (types != null) {
			Type nodeType = types.createNodeType(true);
			Type arcTypeConflict = types.createArcType(false);
			Type arcTypeDepend = types.createArcType(false);
			
			nodeType.setStringRepr("Rule");
			nodeType.setAdditionalRepr("[NODE]");
			arcTypeConflict.setStringRepr("c");
			arcTypeConflict
					.setAdditionalRepr(":SOLID_LINE:java.awt.Color[r=255,g=0,b=0]::[EDGE]:");
			arcTypeDepend.setStringRepr("d");
			arcTypeDepend
					.setAdditionalRepr(":DOT_LINE:java.awt.Color[r=0,g=0,b=255]::[EDGE]:");
	
			InformationFacade info = DefaultInformationFacade.self();
			AttrHandler javaHandler = info.getJavaHandler();
			AttrType attrType = nodeType.getAttrType();
			attrType.addMember(javaHandler, "String", "name");
	
			if (this.conflicts != null) {
				for (Enumeration<Rule> keys1 = this.conflicts.keys(); keys1.hasMoreElements();) {
					Rule r1 = keys1.nextElement();
					if (r1.isEnabled()) {
						Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
						table = this.conflicts.get(r1);
						for (Enumeration<Rule> keys2 = table.keys(); keys2
								.hasMoreElements();) {
							Rule r2 = keys2.nextElement();
							if (r2.isEnabled()) {
								ExcludePairContainer.Entry entry = this.conflictCont.getEntry(r1, r2);
								Node nr1 = local.get(r1.getQualifiedName());
								if (nr1 == null) {
									nr1 = createNode(this.conflictGraph, nodeType, r1);
									local.put(r1.getQualifiedName(), nr1);
									node2rule.put(nr1, r1);
									if (r1 == r2)
										nr1.setVisible(entry.isRuleVisible());
								}
								if (this.combiGraph != null) {
									Node nr = common.get(r1.getQualifiedName());
									if (nr == null) {
										nr = createNode(this.combiGraph, nodeType, r1);
										common.put(r1.getQualifiedName(), nr);
										node2rule.put(nr, r1);
										if (r1 == r2)
											nr.setVisible(entry.isRuleVisible());
									}
								}
								Node nr2 = local.get(r2.getQualifiedName());
								if (nr2 == null) {
									nr2 = createNode(this.conflictGraph, nodeType, r2);
									local.put(r2.getQualifiedName(), nr2);
									node2rule.put(nr2, r2);
								}
								if (this.combiGraph != null) {
									Node nr = common.get(r2.getQualifiedName());
									if (nr == null) {
										nr = createNode(this.combiGraph, nodeType, r2);
										common.put(r2.getQualifiedName(), nr);
										node2rule.put(nr, r2);
									}
								}
								Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
								p = table.get(r2);
								boolean rel = p.first.booleanValue();							
								if (rel) {
									// create edge if rule relation
									createEdge(this.conflictGraph, arcTypeConflict, nr1, nr2);
									if (this.combiGraph != null) {
										createEdge(this.combiGraph, arcTypeConflict, 
												common.get(r1.getQualifiedName()), common.get(r2.getQualifiedName()));
									}
								}
							}
						}
					}
				}
			}
	
			if (this.dependencies != null) {
				local.clear();
				for (Enumeration<Rule> keys1 = this.dependencies.keys(); keys1
						.hasMoreElements();) {
					Rule r1 = keys1.nextElement();
					if (r1.isEnabled()) {
						Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
						table = this.dependencies.get(r1);
						for (Enumeration<Rule> keys2 = table.keys(); keys2
								.hasMoreElements();) {
							Rule r2 = keys2.nextElement();
							if (r2.isEnabled()) {
								ExcludePairContainer.Entry entry = this.dependCont
										.getEntry(r1, r2);
								Node nr1 = local.get(r1.getQualifiedName());
								if (nr1 == null) {
									nr1 = createNode(this.dependGraph, nodeType, r1);
									local.put(r1.getQualifiedName(), nr1);
									node2rule.put(nr1, r1);
									if (r1 == r2)
										nr1.setVisible(entry.isRuleVisible());
								}
								if (this.combiGraph != null) {
									Node nr = common.get(r1.getQualifiedName());
									if (nr == null) {
										nr = createNode(this.combiGraph, nodeType, r1);
										common.put(r1.getQualifiedName(), nr);
										node2rule.put(nr, r1);
										if (r1 == r2)
											nr.setVisible(entry.isRuleVisible());
									}
								}
								Node nr2 = local.get(r2.getQualifiedName());
								if (nr2 == null) {
									nr2 = createNode(this.dependGraph, nodeType, r2);
									local.put(r2.getQualifiedName(), nr2);
									node2rule.put(nr2, r2);
								}
								if (this.combiGraph != null) {
									Node nr = common.get(r2.getQualifiedName());
									if (nr == null) {
										nr = createNode(this.combiGraph, nodeType, r2);
										common.put(r2.getQualifiedName(), nr);
										node2rule.put(nr, r2);
									}
								}
								Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> 
								p = table.get(r2);							
								boolean rel = p.first.booleanValue();
								if (rel) {
									createEdge(this.dependGraph, arcTypeDepend, nr1, nr2);
									if (this.combiGraph != null) {
										createEdge(this.combiGraph, arcTypeDepend, 
												common.get(r1.getQualifiedName()), common.get(r2.getQualifiedName()));
									}
								}
							}
						}
					}
				}
			}			
			common.clear();
			common = null;
			local.clear();
			local = null;
		}
	}

	private void createGraphs(Graph combiCPAGraph) {
		if ((this.conflicts == null) && (this.dependencies == null))
			return;
		
		List<Rule> rlist = this.grammar.getRulesWithIntegratedRulesOfRuleScheme();
		Hashtable<String, Rule> name2rule = new Hashtable<String, Rule>();
		for (Rule r : rlist) {
			name2rule.put(r.getQualifiedName(), r);
		}
		
		node2rule = new Hashtable<Node, Rule>();
		
		TypeSet types = combiCPAGraph.getTypeSet();
		Type arcTypeConflict = types.getTypeByName("c");
		Type arcTypeDepend = types.getTypeByName("d");
		
		if (this.conflicts != null) {
			// copy combiGraph and delete dependency edges
			this.conflictGraph = combiCPAGraph.copy();
			this.conflictGraph.setName("Conflicts of Rules");
			Vector<Arc> list = this.conflictGraph.getArcs(arcTypeDepend);
			if (list != null) {
				for (Arc a : list) {
					try {
						this.conflictGraph.destroyArc(a);
					} catch (TypeException ex) {}
					
				}
			}
		}
		if (this.dependencies != null) {	
			// copy combiGraph and delete conflict edges
			this.dependGraph = combiCPAGraph.copy();
			this.dependGraph.setName("Dependencies of Rules");	
			Vector<Arc> list = this.dependGraph.getArcs(arcTypeConflict);
			if (list != null) {
				for (Arc a : list) {
					try {
						this.dependGraph.destroyArc(a);
					} catch (TypeException ex) {}
				}
			}
		}
		// fill node2rule hashtable
		Iterator<Node> iter = this.dependGraph.getNodesSet().iterator();
		while (iter.hasNext()) {
			Node n = iter.next();
			String name = n.getAttribute().getValueAsString(0).replace("\"", "");
			Rule r = name2rule.get(name);
			if (r != null) {
				node2rule.put(n,  r);
			}
		}
		iter = this.conflictGraph.getNodesSet().iterator();
		while (iter.hasNext()) {
			Node n = iter.next();
			String name = n.getAttribute().getValueAsString(0).replace("\"", "");
			Rule r = name2rule.get(name);
			if (r != null) {
				node2rule.put(n,  r);
			}
		}
	}
	
	
	private void optimizeLayout(Graph g) {
		// replace two edges between the same nodes through one not directed edge
		Iterator<Arc> e = g.getArcsSet().iterator();
		while (e.hasNext()) {
			Arc a = e.next();

			if (a.getSource() == a.getTarget())
				continue;
			if (!a.isDirected())
				continue;

			Iterator<Arc> e1 = g.getArcsSet().iterator();
			while (e1.hasNext()) {
				Arc a1 = e1.next();

				if (a1.getSource() == a1.getTarget())
					continue;
				if (!a1.isDirected())
					continue;

				if (a != a1) {
					if (a.getType().getName().equals(a1.getType().getName())
							&& (a.getSource() == a1.getTarget())
							&& (a.getTarget() == a1.getSource())) {
						a.setVisible(false);
						a1.setDirected(false);
						break;
					}
				}
			}
		}
	}

	private Node createNode(Graph g, Type t, Rule r) {
		Node n = getNode(g, r);
		if (n == null) {
			try {
				n = g.createNode(t);
				ValueTuple vt = (ValueTuple) n.getAttribute();
				String rname = r.getQualifiedName();
				vt.getValueMemberAt("name").setExprAsObject(rname);
			} catch (TypeException e) {
			}
		}
		return n;
	}

	private Node getNode(Graph g, Rule r) {
		Iterator<Node> e = g.getNodesSet().iterator();
		while (e.hasNext()) {
			Node n = e.next();
			if (((String) n.getAttribute().getValueAt("name")).equals(r
					.getQualifiedName()))
				return n;
		}
		return null;
	}

	private Arc createEdge(Graph g, Type t, Node n1, Node n2) {
		if (t == null || n1 == null || n2 == null)
			return null;
		Arc a = getEdge(g, t, n1, n2);
		if (a == null) {
			try {
				a = g.createArc(t, n1, n2);
				if (n1 != n2) {
					Arc a1 = getEdge(g, t, n2, n1);
					if (a1 != null) {
						a.setDirected(false);
						a1.setVisible(false);
					}
				}
			} catch (TypeException e) {
			}
		}
		return a;
	}

	@SuppressWarnings("unused")
	private Arc createEdge(Graph g, Type t, Rule r1, Rule r2) {
		if (t == null || r1 == null || r2 == null)
			return null;
		Node n1 = getNode(g, r1);
		Node n2 = getNode(g, r2);
		return createEdge(g, t, n1, n2);
	}

	private Arc getEdge(Graph g, Type t, Node n1, Node n2) {
		if (t == null || n1 == null || n2 == null)
			return null;
		Iterator<Arc> e = g.getArcsSet().iterator();
		while (e.hasNext()) {
			Arc a = e.next();
			if (!a.getType().getName().equals(t.getName()))
				continue;
			Object src = a.getSource().getAttribute().getValueAt("name");
			Object tar = a.getTarget().getAttribute().getValueAt("name");
			Object name1 = n1.getAttribute().getValueAt("name");
			Object name2 = n2.getAttribute().getValueAt("name");
			if ((src != null && ((String) src).equals(name1))
					&& (tar != null && ((String) tar).equals(name2)))
				return a;
		}
		return null;
	}

}
