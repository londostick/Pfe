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

import agg.xt_basis.Arc;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.ConcurrentRule;
import agg.xt_basis.GraGra;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.Node;
import agg.util.XMLHelper;
import agg.util.Pair;

// ****************************************************************************+
/**
 * This Container contains only conflict free and dependency relations.
 * 
 * @author $Author: olga $
 */
public class DependencyPairContainer extends ExcludePairContainer {

	protected boolean switchDependency;
	
	protected boolean makeConcurrentRules;
	
	protected boolean completeConcurrency = true;
	
	protected List<ConcurrentRule> concurrentRules;
	
	/**
	 * Creates a new container for critical pairs.
	 * 
	 * @param gragra
	 *            The graph grammar.
	 */
	public DependencyPairContainer(GraGra gragra) {
		super(gragra);
		this.conflictKind = CriticalPair.TRIGGER_DEPENDENCY;
		this.completeConcurrency = true;
	}

	public Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> 
	getDependencyContainer() {
		return super.getExcludeContainer();
	}

	public Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	getCriticalPair(Rule r1, Rule r2, int kind, boolean local)
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
	
	
	public void enableSwitchDependency(boolean b) {
		this.switchDependency = b;
		if (this.conflictKind == CriticalPair.TRIGGER_DEPENDENCY && this.switchDependency)
			this.conflictKind = CriticalPair.TRIGGER_SWITCH_DEPENDENCY;
		else if (this.conflictKind == CriticalPair.TRIGGER_SWITCH_DEPENDENCY && !this.switchDependency)
			this.conflictKind = CriticalPair.TRIGGER_DEPENDENCY;
	}
	
	public void enableProduceConcurrentRule(boolean b) {
		this.makeConcurrentRules = b;
	}
	
	public void setCompleteConcurrency(boolean b) {
		this.completeConcurrency = b;
	}
	
	public List<ConcurrentRule> getConcurrentRules() {
		return this.concurrentRules;
	}
	
	public DependencyPair getCurrentDependencyPair() {
		return (DependencyPair) this.excludePair;
	}
	
	/**
	 * Computes if the second rule depends of the first rule. The result is
	 * added to the container.
	 * 
	 * @param r1
	 *            The first rule.
	 * @param r2
	 *            The second rule.
	 */
	protected synchronized void computeCritical(Rule r1, Rule r2) {
		Report.trace("ExcludePairContainer: starte computeCritical"
				+ this.getEntry(r1, r2).state + "   " + r1.getName() + "    "
				+ r2.getName(), 2);
		// System.out.println("ExcludePairContainer: starte computeCritical,
		// state: "+this.getEntry(r1, r2).state+", "+r1.getName()+"
		// "+r2.getName());
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
				this.excludePair = new SimpleDependencyPair();
			else {
				this.excludePair = new DependencyPair();
				this.excludePair.setGraGra(this.grammar);
			}
//			this.excludePair.setProgressIndx(e);
			setOptionsOfExcludePair();
			
			((DependencyPair) this.excludePair).enableSwitchDependency(this.switchDependency);
			((DependencyPair) this.excludePair).enableProduceConcurrentRule(this.makeConcurrentRules);
			((DependencyPair) this.excludePair).setCompleteConcurrency(this.completeConcurrency);
			
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			overlapping = null;
			try {
				overlapping = this.excludePair.isCritical(CriticalPair.EXCLUDE, r1, r2);
				e = this.getEntry(r1, r2, true);
				e.setProgressIndx(this.excludePair);
				
				if (!this.excludePair.computable) {
					e.setStatus(Entry.NOT_COMPUTABLE);
				} else if (overlapping != null) {
					this.concurrentRules = ((DependencyPair) this.excludePair).getConcurrentRules();
				}
			} catch (InvalidAlgorithmException iae) {
//				System.out.println(iae.getLocalizedMessage());
//				if (iae.getKindOfInvalidAlgorithm() == CriticalPairEvent.NOT_COMPUTABLE) {
//					e(r1, r2).status = Entry.NOT_COMPUTABLE;
//				}
			}

			boolean critic = (overlapping != null);
			
//			add to container
			addEntry(r1, r2, critic, overlapping);
			if (critic) { 
				if (!this.excludePair.dependencyCond1 && this.excludePair.dependencyCond2)
					e.setState(Entry.COMPUTED2);
				else if (this.excludePair.dependencyCond1 && this.excludePair.dependencyCond2)
					e.setState(Entry.COMPUTED12);
				else
					e.setState(Entry.COMPUTED);
			}
			
			this.usedM = this.usedM + this.excludePair.usedM;
			
			this.excludePair.dispose();
			this.excludePair = null;
			
			/*
			 * Wenn overlapping Elemente enthaelt sind r1/r2 kritisch critic
			 * wird daher true. Alle wichtigen Informationen werden eingetragen.
			 * Enthaelt r1/r2 keine Elementen, so wird critic auf false gesetzt.
			 * Wenn excludeContainer nach r1/r2 gefragt wird, liefert die
			 * Antwort auch false. overlapping kann daher null sein.
			 */

			addQuadruple(this.excludeContainer, r1, r2, critic, overlapping);
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
			this.excludePair = new SimpleDependencyPair();
		else
			this.excludePair = new DependencyPair();	
		
		this.excludePair.setProgressIndx(e);			
		setOptionsOfExcludePair();

		((DependencyPair) this.excludePair).enableSwitchDependency(this.switchDependency);
		((DependencyPair) this.excludePair).enableProduceConcurrentRule(this.makeConcurrentRules);
		((DependencyPair) this.excludePair).setCompleteConcurrency(this.completeConcurrency);
		
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
		overlapping = null;
		try {
			overlapping = this.excludePair.isCritical(CriticalPair.EXCLUDE, r1, r2);			
			e = this.getEntry(r1, r2, true);
			e.setProgressIndx(this.excludePair);
			if (!this.excludePair.computable) {
				e.setStatus(Entry.NOT_COMPUTABLE);
			} else if (overlapping != null) {
				this.concurrentRules = ((DependencyPair) this.excludePair).getConcurrentRules();
			}
		} catch (InvalidAlgorithmException iae) {}
			
		boolean critic = (overlapping != null);
		
//		add to container
		Entry entry = addEntry(r1, r2, critic, overlapping);
		if (critic) { 
			if (!this.excludePair.dependencyCond1 && this.excludePair.dependencyCond2)
				entry.setState(Entry.COMPUTED2);
			else if (this.excludePair.dependencyCond1 && this.excludePair.dependencyCond2)
				entry.setState(Entry.COMPUTED12);
			else
				entry.setState(Entry.COMPUTED);
		}
		
		this.excludePair.dispose();
		this.excludePair = null;

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
	
	protected Pair<OrdinaryMorphism, OrdinaryMorphism> 
	readOldOverlappingMorphisms(
			XMLHelper h, 
			Rule r1, 
			Rule r2,
			String firstName, 
			Graph overlapGraph) {
		
		OrdinaryMorphism first = BaseFactory.theFactory().createMorphism(
				r1.getRight(), overlapGraph);
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

	/**
	 * Writes the contents of this object to a file in a xml format.
	 * 
	 * @param h
	 *            A helper object for storing.
	 */
	public void XwriteObject(XMLHelper h) {
		h.openNewElem("CriticalPairs", this);
		h.addObject("GraGra", getGrammar(), true);
		h.openSubTag("dependenciesContainer");
		
		String kind = "trigger_dependency";
		if (this.switchDependency)
			kind = "trigger_switch_dependency";
		h.addAttr("kind", kind);
		
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
					Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
					v = p.second;

					for (int i = 0; i < v.size(); i++) {
						Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
						p2i = v.elementAt(i);
						Pair<OrdinaryMorphism, OrdinaryMorphism> p2 = p2i.first;
						h.openSubTag("Overlapping_Pair");
						OrdinaryMorphism first = p2.first;
						Graph overlapping = first.getImage();
						/*
						 * durch das true macht der String am Anfang keinen Sinn
						 */
						h.addObject("", overlapping, true);

						for (Iterator<Node> e = overlapping.getNodesSet().iterator(); 
								e.hasNext();) {
							GraphObject o = e.next();
							if (o.isCritical()) {
								h.openSubTag("Critical");
								h.addObject("object", o, false);
								h.close();
							}
						}
						for (Iterator<Arc> e = overlapping.getArcsSet().iterator(); 
								e.hasNext();) {
							GraphObject o = e.next();
							if (o.isCritical()) {
								h.openSubTag("Critical");
								h.addObject("object", o, false);
								h.close();
							}
						}
						
						/*
						 * first.writeMorphism(h); ((OrdinaryMorphism)
						 * p2.second).writeMorphism(h);
						 */

						writeOverlapMorphisms(h, r1, r2, p2i);

						h.close();
					}
				}
				h.close();
			}

			h.close();
		}
		h.close();
		if (this.conflictFreeContainer != null) {
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
					Boolean b = secondPart.get(r2).first;
	
					h.addAttr("bool", b.toString());
					h.close();
				}
	
				h.close();
			}
			h.close();
		}
		h.close();
	}

	/**
	 * Reads the contents of a xml file to restore this object.
	 * 
	 * @param h
	 *            A helper object for loading.
	 */
	public void XreadObject(XMLHelper h) {
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

			this.switchDependency = false;
			
			if (h.readSubTag(tagnames2)) {				
				this.conflictKind = CriticalPair.TRIGGER_DEPENDENCY;	
				
				if (h.readAttr("kind").equals("trigger_switch_dependency")) {
					this.switchDependency = true;
					this.conflictKind = CriticalPair.TRIGGER_SWITCH_DEPENDENCY;
				}
			}
			else if (h.readSubTag(tagnames)) {
				this.conflictKind = CriticalPair.CONFLICT;
			}
			
			if (this.conflictKind == CriticalPair.TRIGGER_DEPENDENCY
					|| this.conflictKind == CriticalPair.TRIGGER_SWITCH_DEPENDENCY
					|| this.conflictKind == CriticalPair.CONFLICT) {
				// System.out.println("conflictKind : "+conflictKind);
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
								/*
								 * OrdinaryMorphism first =
								 * BaseFactory.theFactory().createMorphism(r1.getRight(),
								 * g); first.readMorphism(h);
								 * //System.out.println("DependPairCont.Xread::
								 * "+first.getSize()); OrdinaryMorphism second =
								 * BaseFactory.theFactory().createMorphism(r2.getLeft(),
								 * g); second.readMorphism(h);
								 * allOverlappings.addElement(new Pair(first,
								 * second));
								 */

								Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
								p = readOverlappingMorphisms(h, r1, r2, g);
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
		// //isComputed = true;
		h.close();
	}

	/**
	 * Provides a human readable text of the critical pairs.
	 * 
	 * @return The text.
	 */
	public String toString() {
		String result = super.toString() + "\n" + getGrammar().toString()
				+ "\n";
		result += "dependenciesContainer " + this.excludeContainer + "\n\n";
		result += "conflictFreeContainer " + this.conflictFreeContainer + "\n";
		return result;
	}

}
