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
 * the exclude algorithm. Further on the used graph grammar is layered.
 * 
 * @author $Author: olga $
 * @version $Id: LayeredExcludePairContainer.java,v 1.7 2007/06/13 08:32:58 olga
 *          Exp $
 */
public class LayeredExcludePairContainer extends ExcludePairContainer implements
		LayeredPairContainer {

	@SuppressWarnings("deprecation")
	private LayerFunction layerFunc; // not more needed

	private int layer = -1;

	protected LayeredExcludePair layeredPair;

	protected LayeredSimpleExcludePair layeredSimplePair;

	/**
	 * Creates a container for critical pairs for a layered graph grammar.
	 * 
	 * @param gragra
	 *            The graph grammar.
	 * @param layer
	 *            The layer function.
	 * @deprecated
	 */
	public LayeredExcludePairContainer(GraGra gragra, LayerFunction layer) {
		super(gragra);
		// this.layerFunc = layer;
	}

	/**
	 * Creates a new container for critical pairs. An invalid layer function is
	 * used. It is necessary to set a valid layer function.
	 * 
	 * @param gragra
	 *            The graph grammar.
	 */
	public LayeredExcludePairContainer(GraGra gragra) {
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
		Report.trace("LayeredExcludePairContainer: starte computeCritical", 2);
		// System.out.println("LayeredExcludePairContainer.computeCritical(r1,
		// r2): "+r1.getName()+" "+r2.getName()+" "+this.getEntry(r1,
		// r2).state);
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

		if (r1.getLayer() != r2.getLayer()) {// test rule layer
			this.getEntry(r1, r2).state = Entry.NOT_RELATED;
			addEntry(r1, r2, false, null);
			addQuadruple(this.excludeContainer, r1, r2, false, null);
			addQuadruple(this.conflictFreeContainer, r1, r2, false, null);
			firePairEvent(new CriticalPairEvent(this, r1, r2, ""));
			return;
		}

		if ((this.layer > -1) && (r1.getLayer() != this.layer)) {
			return;
		}

		if ((this.getEntry(r1, r2).state == Entry.SCHEDULED_FOR_COMPUTING)
				|| (this.getEntry(r1, r2).state == Entry.NOT_SET)) {
			getEntry(r1, r2).setState(Entry.COMPUTING_IS_RUNNING);

			firePairEvent(new CriticalPairEvent(this, r1, r2,
					"Computing critical rule pair  [  " + r1.getName()
							+ "  ,  " + r2.getName() + "  ]"));

			if (!this.complete) {
				this.layeredPair = null;

				this.layeredSimplePair = new LayeredSimpleExcludePair();
				this.excludePair = this.layeredSimplePair;
			} else {
				this.layeredSimplePair = null;

				this.layeredPair = new LayeredExcludePair();
				this.excludePair = this.layeredPair;
			}

			setOptionsOfExcludePair();
			
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> overlapping = null;
			try {
				if (this.layeredPair != null) {
					overlapping = this.layeredPair.isCritical(CriticalPair.EXCLUDE, r1, r2);
				} else if (this.layeredSimplePair != null) {
					overlapping = this.layeredSimplePair.isCritical(CriticalPair.EXCLUDE, r1, r2);
				}
			} catch (InvalidAlgorithmException iae) {
				// System.out.println(iae.getLocalizedMessage());
			}

			if (this.excludePair != null)
				this.excludePair.dispose();
			this.excludePair = null;
			this.layeredPair = null;
			this.layeredSimplePair = null;
//			System.gc();

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

			if (overlapping != null)
				firePairEvent(new CriticalPairEvent(this, r1, r2, "<"
						+ r1.getName() + ">  and  <" + r2.getName()
						+ ">  have critical pairs"));
			else
				firePairEvent(new CriticalPairEvent(this, r1, r2, "<"
						+ r1.getName() + ">  and  <" + r2.getName()
						+ ">  have no critical pairs"));
		}
		Report.trace("LayeredExcludePairContainer: beende computeCritical", -2);
	}

	protected void fillContainers() {
		super.fillContainers();
		if (this.layer > -1)
			this.isComputed = false;
	}

	/**
	 * Sets a layer function to layer a certain object.
	 * 
	 * @param layer
	 *            A specific layer function.
	 * @deprecated not more used
	 */
	public void setLayer(LayerFunction layer) {
		this.layerFunc = layer;
	}

	/**
	 * Returns a layer function from a certain object.
	 * 
	 * @return A specific layer function.
	 * @deprecated not more used
	 */
	public LayerFunction getLayer() {
		return this.layerFunc;
	}

	/**
	 * Sets a layer to compute. If layer = -1 then all layers should be
	 * computed.
	 * 
	 * @param layer
	 */
	public void setLayer(int layer) {
		this.layer = layer;
		// System.out.println("LayeredExcludePairContainer::: compute layer:
		// "+layer);
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
		if (h.isTag("CriticalPairs", this)) {
			// System.out.println("LayerExcludePairContainer.XreadObject ...");
			Rule r1 = null;
			Rule r2 = null;
			boolean b = false;
			Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> allOverlappings = null;
			Vector<String> tagnames = new Vector<String>(1);
			Vector<String> tagnames2 = new Vector<String>(1);

			this.grammar = BaseFactory.theFactory().createGraGra();
			h.getObject("", this.grammar, true);

			tagnames.add("excludeContainer");
			tagnames.add("conflictsContainer");
			tagnames.add("conflictContainer");

			tagnames2.add("dependencyContainer");
			tagnames2.add("dependenciesContainer");

			boolean switchDependency = false;
			
			if (h.readSubTag(tagnames)) {
				String kind = h.readAttr("kind");
				this.conflictKind = CriticalPair.CONFLICT;
			}
			else if (h.readSubTag(tagnames2)) {
				this.conflictKind = CriticalPair.TRIGGER_DEPENDENCY;
				if (h.readAttr("kind").equals("trigger_switch_dependency")) {
					switchDependency = true;
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
						else if (r1.getLayer() != r2.getLayer()) // test rule
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
		h.close();
	}

}
/*
 * $Log: LayeredExcludePairContainer.java,v $
 * Revision 1.19  2010/11/04 11:01:31  olga
 * tuning
 *
 * Revision 1.18  2010/09/20 14:30:10  olga
 * tuning
 *
 * Revision 1.17  2010/03/04 14:11:34  olga
 * code optimizing
 *
 * Revision 1.16  2009/03/12 10:57:46  olga
 * some changes in CPA of managing names of the attribute variables.
 *
 * Revision 1.15  2008/04/10 10:53:14  olga
 * Draw graphics tuning
 *
 * Revision 1.14  2008/04/07 09:36:51  olga
 * Code tuning: refactoring + profiling
 * Extension: CPA - two new options added
 *
 * Revision 1.13  2008/02/18 09:37:10  olga
 * - an extention of rule dependency check is implemented;
 * - some bugs fixed;
 * - editing of graphs improved
 *
 * Revision 1.12  2007/11/01 09:58:18  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.11  2007/10/10 07:44:27  olga
 * CPA: bug fixed
 * GUI, AtomConstraint: bug fixed
 *
 * Revision 1.10  2007/09/27 08:42:46  olga
 * CPA: new option  -ignore pairs with same rules and same matches-
 *
 * Revision 1.9  2007/09/24 09:42:38  olga
 * AGG transformation engine tuning
 *
 * Revision 1.8  2007/09/10 13:05:41  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.7 2007/06/13 08:32:58
 * olga Update: V161
 * 
 * Revision 1.6 2006/12/13 13:32:59 enrico reimplemented code
 * 
 * Revision 1.5 2006/03/01 09:55:46 olga - new CPA algorithm, new CPA GUI
 * 
 * Revision 1.4 2005/10/10 08:05:16 olga Critical Pair GUI and CPA graph
 * 
 * Revision 1.3 2005/09/27 08:41:12 olga CPs GUI tuning.
 * 
 * Revision 1.2 2005/09/19 09:12:14 olga CPA GUI tuning
 * 
 * Revision 1.1 2005/08/25 11:56:57 enrico *** empty log message ***
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
 * Revision 1.12 2005/05/23 09:54:30 olga CPA improved: Stop of generation
 * process or rule pair.
 * 
 * Revision 1.11 2004/12/20 14:53:48 olga Changes because of matching
 * optimisation.
 * 
 * Revision 1.10 2004/10/27 13:09:15 olga Anpassung von LayerFunction von
 * LayerFunction
 * 
 * Revision 1.9 2004/09/23 08:26:43 olga Fehler bei CPs weg, Debug output in
 * file
 * 
 * Revision 1.8 2004/09/13 10:21:14 olga Einige Erweiterungen und
 * Fehlerbeseitigung bei CPs und Graph Grammar Transformation
 * 
 * Revision 1.7 2004/06/14 12:34:19 olga CP Analyse and Transformation
 * 
 * Revision 1.6 2004/01/28 17:58:38 olga Errors suche
 * 
 * Revision 1.5 2004/01/22 17:51:18 olga tests
 * 
 * Revision 1.4 2003/03/17 15:36:02 olga Xread, Xwrite erweitert
 * 
 * Revision 1.3 2003/03/05 18:24:08 komm sorted/optimized import statements
 * 
 * Revision 1.2 2003/03/03 17:46:17 olga Optimierung
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:24 olga Imported sources
 * 
 * Revision 1.4 2001/08/02 15:22:16 olga Error-Meldungen eingebaut in
 * LayerFunction und die Anzeige dieser Meldungen in GUI.
 * 
 * Revision 1.3 2001/05/14 12:03:00 olga Zusaetzliche Parser Events Aufrufe
 * eingebaut, um bessere Kommunikation mit GUI Status Anzeige zu ermoeglichen.
 * 
 * Revision 1.2 2001/03/08 10:44:53 olga Neue Files aus parser branch in Head
 * eingefuegt.
 * 
 * Revision 1.1.2.5 2001/01/28 13:14:55 shultzke API fertig
 * 
 * Revision 1.1.2.4 2001/01/14 14:48:20 shultzke commentare ergaenzt
 * 
 * Revision 1.1.2.3 2001/01/11 11:36:08 shultzke Laden und speichern der
 * kritischen Paare geht, es fehlt nur noch das Laden fuer den Parser.
 * 
 * Revision 1.1.2.2 2001/01/01 21:24:32 shultzke alle Parser fertig inklusive
 * Layout
 * 
 * Revision 1.1.2.1 2000/12/10 14:55:48 shultzke um Layer erweitert
 * 
 */
