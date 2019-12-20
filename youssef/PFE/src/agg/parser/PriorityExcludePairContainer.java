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
import java.util.Vector;

import agg.xt_basis.BaseFactory;
import agg.xt_basis.GraGra;
import agg.xt_basis.Graph;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.GraphObject;
import agg.util.XMLHelper;
import agg.util.Pair;

/**
 * This class provides a container for critical pairs. The critical pairs uses
 * the exclude algorithm.
 * 
 * @author $Author: olga $
 */
public class PriorityExcludePairContainer extends ExcludePairContainer {

	/**
	 * Creates a new container for critical pairs. An invalid layer function is
	 * used. It is necessary to set a valid layer function.
	 * 
	 * @param gragra
	 *            The graph grammar.
	 */
	public PriorityExcludePairContainer(GraGra gragra) {
		super(gragra);
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
//		System.out
//				.println("PriorityExcludePairContainer.computeCritical(r1, r2): "
//						+ r1.getName()
//						+ "   "
//						+ r2.getName()
//						+ "   "
//						+ this.getEntry(r1, r2).state);
		// mark Entry
		if (!r1.isEnabled() || !r2.isEnabled()) { // test disabled rule
			this.getEntry(r1, r2).state = Entry.DISABLED;
			addEntry(r1, r2, false, null);
			addQuadruple(this.excludeContainer, r1, r2, false, null);
			addQuadruple(this.conflictFreeContainer, r1, r2, false, null);
			firePairEvent(new CriticalPairEvent(this, r1, r2, "<"
					+ r1.getName() + ">  and  <" + r2.getName()
					+ ">  should not be computed."));
			return;
		}

		if (r1.getPriority() != r2.getPriority()) {
			this.getEntry(r1, r2).state = Entry.NOT_RELATED;
			addEntry(r1, r2, false, null);
			addQuadruple(this.excludeContainer, r1, r2, false, null);
			addQuadruple(this.conflictFreeContainer, r1, r2, false, null);
			firePairEvent(new CriticalPairEvent(this, r1, r2, ""));
			return;
		}

		if ((this.getEntry(r1, r2).state == Entry.SCHEDULED_FOR_COMPUTING)
				|| (this.getEntry(r1, r2).state == Entry.NOT_SET)) {
			getEntry(r1, r2).setState(Entry.COMPUTING_IS_RUNNING);
			firePairEvent(new CriticalPairEvent(this, r1, r2,
					"Computing critical rule pair  [  " + r1.getName()
							+ "  ,  " + r2.getName() + "  ]"));

			if (!this.complete) {
				PrioritySimpleExcludePair pair = new PrioritySimpleExcludePair();
				this.excludePair = pair;
			} else {
				PriorityExcludePair pair = new PriorityExcludePair();
				this.excludePair = pair;				
			}

			setOptionsOfExcludePair();
			
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
			overlapping = null;
			try {
				if (this.excludePair != null) {
					overlapping = this.excludePair.isCritical(CriticalPair.EXCLUDE, r1,r2);
				}
			} catch (InvalidAlgorithmException iae) {
//				System.out.println(iae.getLocalizedMessage());
//				if (iae.getKindOfInvalidAlgorithm() == CriticalPairEvent.NOT_COMPUTABLE) {
//					Entry entry = this.getEntry(r1, r2);
//					entry.state = Entry.NOT_COMPUTABLE;
//				}
			}

			if (this.excludePair != null)
				this.excludePair.dispose();
			this.excludePair = null;

			boolean critic = (overlapping != null);

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
			
			if (overlapping != null)
				firePairEvent(new CriticalPairEvent(this, r1, r2, "<"
						+ r1.getName() + ">  and  <" + r2.getName()
						+ ">  have critical pairs"));
			else
				firePairEvent(new CriticalPairEvent(this, r1, r2, "<"
						+ r1.getName() + ">  and  <" + r2.getName()
						+ ">  have no critical pairs"));
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
		h.openSubTag("conflictContainer");
		h.addAttr("kind", "exclude");
		
		// Inhalt von excludeContainer schreiben (save)
		for (Enumeration<Rule> keys = this.excludeContainer.keys(); keys.hasMoreElements();) {
			Rule r1 = keys.nextElement();
			h.openSubTag("Rule");
			h.addObject("R1", r1, false);

			Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> 
			secondPart = this.excludeContainer
					.get(r1);
			for (Enumeration<Rule> k2 = secondPart.keys(); k2.hasMoreElements();) {
				Rule r2 = k2.nextElement();
				h.openSubTag("Rule");
				h.addObject("R2", r2, false);
				Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> p = secondPart.get(r2);
				Boolean b = p.first;
				h.addAttr("bool", b.toString());
				if (b.booleanValue()) {
					Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> v = p.second;
					for (int i = 0; i < v.size(); i++) {
						Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p2i = v.elementAt(i);
						Pair<OrdinaryMorphism, OrdinaryMorphism> p2 = p2i.first;
						h.openSubTag("Overlapping_Pair");
						OrdinaryMorphism first = p2.first;
						Graph overlapping = first.getImage();
						h.addObject("", overlapping, true);

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
			secondPart = this.conflictFreeContainer
					.get(r1);
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
	public void XreadObject(XMLHelper h) {
		if (h.isTag("CriticalPairs", this)) {

			Rule r1 = null;
			Rule r2 = null;
			boolean b = false;
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> allOverlappings = null;

			this.grammar = BaseFactory.theFactory().createGraGra();
			h.getObject("", this.grammar, true);

			if (h.readSubTag("conflictContainer"))
				this.conflictKind = CriticalPair.CONFLICT;

			if (this.conflictKind == CriticalPair.CONFLICT) {
				Enumeration<?> r1s = h.getEnumeration("", null, true, "Rule");
				if (!r1s.hasMoreElements())
					r1s = h.getEnumeration("", null, true, "Regel");
				while (r1s.hasMoreElements()) {
					h.peekElement(r1s.nextElement());
					r1 = (Rule) h.getObject("R1", null, false);
					Enumeration<?> r2s = h.getEnumeration("", null, true, "Rule");
					if (!r2s.hasMoreElements())
						r2s = h.getEnumeration("", null, true, "Regel");
					while (r2s.hasMoreElements()) {
						h.peekElement(r2s.nextElement());
						r2 = (Rule) h.getObject("R2", null, false);
						// System.out.println(r1.getName()+" "+r2.getName());
						String bool = h.readAttr("bool");
						allOverlappings = null;
						b = false;
						if (bool.equals("true")) {
							b = true;
							allOverlappings = new Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
							Enumeration<?> overlappings = h.getEnumeration("",
									null, true, "Overlapping_Pair");
							while (overlappings.hasMoreElements()) {
								h.peekElement(overlappings.nextElement());
								Graph g = (Graph) h.getObject("", new Graph(),
										true);
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
			}/* Ende readSubTag("excludeContainer") */
			if (h.readSubTag("conflictFreeContainer")) {
				// System.out.println(conflictFreeContainer);
				Enumeration<?> r1s = h.getEnumeration("", null, true, "Rule");
				if (!r1s.hasMoreElements())
					r1s = h.getEnumeration("", null, true, "Regel");
				while (r1s.hasMoreElements()) {
					h.peekElement(r1s.nextElement());
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
						else if (r1.getPriority() != r2.getPriority()) // test rule
							// layer
							this.getEntry(r1, r2).state = Entry.NOT_RELATED;

						h.close();
					}
					h.close();
				}
				h.close();
				// System.out.println("conflictFreeContainer
				// "+conflictFreeContainer+"\n");
			}
		}
		// isComputed = true;
		h.close();
	}

}
