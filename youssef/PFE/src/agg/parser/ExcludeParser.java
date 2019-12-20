/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
// This class belongs to the following package:
package agg.parser;

import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import agg.xt_basis.BadMappingException;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.MorphCompletionStrategy;
import agg.xt_basis.Node;
import agg.xt_basis.Arc;
import agg.xt_basis.Match;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.TypeException;
import agg.util.Pair;


//****************************************************************************+
/**
 * This class provides a parser which needs critical pairs. The criticl pair
 * must be <code>ExcludePair</code>. So objects has to be instaciated with
 * <code>ExcludePairContainer</code>. To be independent of a grammar it is
 * necessary to instanciate a object with a host graph and stop graph
 * seperately.
 * 
 * @author $Author: olga $
 * @version $Id: ExcludeParser.java,v 1.17 2010/08/18 09:26:52 olga Exp $
 */
public class ExcludeParser extends AbstractParser implements Runnable {

	/**
	 * Main part of a backtracking algorithm
	 */
	protected Stack<Object> stack;

	protected boolean stop;

	protected boolean correct;

	/**
	 * Creates a new parser. This parser uses critical pair analysis for
	 * optimized parsing.
	 * 
	 * @param grammar
	 *            The graph grammar.
	 * @param hostGraph
	 *            The host graph.
	 * @param stopGraph
	 *            The stop graph.
	 * @param excludeContainer
	 *            The critical pairs.
	 */
	public ExcludeParser(agg.xt_basis.GraGra grammar, Graph hostGraph,
			Graph stopGraph, ExcludePairContainer excludeContainer) {
		super(grammar, hostGraph, stopGraph, excludeContainer);
		this.stack = new Stack<Object>();
		this.stop = false;
	}

	// ****************************************************************************+
	/**
	 * Starts the parser. The result is true if the parser can parse the graph
	 * 
	 * @return true if the graph can be parsed.
	 */
	@SuppressWarnings("rawtypes")
	public boolean parse() {
		System.out.println("Starting exclude parser with CPA ... ");
		this.correct = true;
		fireParserEvent(new ParserMessageEvent(this,
				"Starting exclude parser ..."));

		Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> conflictFree = null;
		try {
			fireParserEvent(new ParserMessageEvent(this,
					"Computting conflict free pairs. Please wait ..."));
			conflictFree = this.pairContainer
					.getContainer(CriticalPair.CONFLICTFREE);
			fireParserEvent(new ParserMessageEvent(this,
					"Computting conflict free pairs done"));
		} catch (InvalidAlgorithmException iae) {
			fireParserEvent(new ParserErrorEvent(iae, "ERROR: "
					+ iae.getMessage()));
			return false;
		}

		Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> exclude = null;
		try {
			fireParserEvent(new ParserMessageEvent(this,
					"Computting exclude pairs. Please wait ..."));
			exclude = this.pairContainer.getContainer(CriticalPair.EXCLUDE);
			fireParserEvent(new ParserMessageEvent(this,
					"Computting exclude pairs done"));		
		} catch (InvalidAlgorithmException iae) {
			fireParserEvent(new ParserErrorEvent(iae, "ERROR: "
					+ iae.getMessage()));
			return false;
		}

		Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> conflictFreeLight 
		= new Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>>();
		Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> excludeLight 
		= new Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>>();
		makeLightContainer(exclude, excludeLight);
		makeLightContainer(conflictFree, conflictFreeLight);

		/*
		 * makeLightContainer kann nur die Elemente filtern, in denen alle teile
		 * false liefern. Mischformen fallen durch
		 */
		for (Enumeration<Rule> keys = conflictFreeLight.keys(); keys
				.hasMoreElements();) {
			Object key = keys.nextElement();
			if (excludeLight.containsKey(key)) {
				conflictFreeLight.remove(key);
			}
		}
		/*
		 * haelt alle Matche, die kritisch sind, damit nicht an einer Stelle
		 * immer wieder angesetzt wird
		 */
		RuleInstances eri = new RuleInstances();

		fireParserEvent(new ParserMessageEvent(this, "Parser initialized"));

		while (!this.stop && !this.graph.isIsomorphicTo(this.stopGraph) && this.correct) {
			boolean ruleApplied = false;
		
			/* zuerst sollen alle konfliktfreien Regeln probiert werden. */
			for (Enumeration<Rule> keys = conflictFreeLight.keys(); keys
					.hasMoreElements()
					&& !ruleApplied;) {
				Rule r = keys.nextElement();
//				System.out.println("try to apply rule: "+r.getName());
				fireParserEvent(new ParserMessageEvent(this,
						"Searching for simple match"));
				Match m = BaseFactory.theFactory().createMatch(r,
						getHostGraph());
				m.setCompletionStrategy((MorphCompletionStrategy) this.grammar
						.getMorphismCompletionStrategy().clone(), true);

				while (!ruleApplied && m.nextCompletion()) {
					if (m.isValid()) {
						if (applyRule(m)) {
							ruleApplied = true;
							try {
								Thread.sleep(this.delay);
							} catch (InterruptedException ex1) {
							}
						}
					}
				}
				m.dispose();
			} // end for(Enumeration keys = conflictFreeLight.keys();
			/* Die konfliktfreien Regeln sind abgearbeitet */

			/* Die Excluderegeln muessen ueberprueft werden */
			if (!ruleApplied && !this.stop) {
				/*
				 * Zuerst wird ein beliebiger Ansatz einer Regel gesucht. Dann
				 * muss der Ansatz geprueft werden, ob ein Ueberlappungsgraph
				 * mit diesem Ansatz eingebettet werden kann. Findet sich keine
				 * Einbettung, so kann die Regel angewendet werden. Sonst wird
				 * nach einem anderen Ansatz gesucht.
				 */
				Match savedMatch = null;
				for (Enumeration<Rule> keys = excludeLight.keys(); keys
						.hasMoreElements()
						&& !ruleApplied && !this.stop;) {
					Rule r = keys.nextElement();
//					System.out.println("try to apply rule: "+r.getName());
					Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>> inclusions = findInclusions(r, CriticalPair.EXCLUDE);
					fireParserEvent(new ParserMessageEvent(this,
							"Searching for difficult match"));
					Match m = BaseFactory.theFactory().createMatch(r,
							getHostGraph());
					m.setCompletionStrategy((MorphCompletionStrategy) this.grammar
							.getMorphismCompletionStrategy().clone(), true);

					boolean validMatch = false;
					while (!ruleApplied && m.nextCompletion()) {
						if (m.isValid()) {
							if (!isMatchCritic(m, inclusions)) {
								if (applyRule(m)) {
									ruleApplied = true;
									try {
										Thread.sleep(this.delay);
									} catch (InterruptedException ex1) {
									}
								}
							} else {
								if (!validMatch && savedMatch == null) {
									if (!eri.isIn(m)) {
										validMatch = true;
										savedMatch = m;
									}
								}
							}
						} // end if(m.isValid())
					}
					if (!validMatch) {
						/*
						 * Match darf nur geloescht werden, wenn er nicht
						 * savedMatch ist
						 */
						BaseFactory.theFactory().destroyMatch(m);
					}
				} // end for(Enumeration keys = excludeLight.keys();

				/*
				 * wenn keine Regel angewendet wurde, dann kann nur noch eine
				 * kritische Regel angewendet werden.
				 */
				if (!ruleApplied && (savedMatch != null) && !this.stop) {
					OrdinaryMorphism copyMorph = getHostGraph().isomorphicCopy();
					if (copyMorph != null) {
						fireParserEvent(new ParserMessageEvent(copyMorph, "IsoCopy"));
						eri.add(savedMatch);
						/*
						 * ERI muss nicht kopiert werden, da nur an
						 * Entscheidungsstellen der Match/die Matches gemerkt werden
						 * mssen, die uns m�licherweise auf einen Holzweg fhren. Der
						 * Match in ERI ist eine Stufe tiefer (also nach
						 * Regelanwendung, denn wir loeschen) nicht mehr verfgbar.
						 * Dadurch kann ein neues ERI erzeugt werden. Auf dem Stack
						 * liegen dann nur die Ableitungen, die uns in eine
						 * Sackgasse gefhrt haben.
						 */
						Pair<Graph, RuleInstances> tmpPair = new Pair<Graph, RuleInstances>(
								getHostGraph(), eri);
						this.stack.push(tmpPair);
						eri = new RuleInstances();
						/*
						 * Die Regel muss auf den kopierten Graphen mit DEMSELBEN
						 * kopierten Match angewendet werden.
						 */
						setHostGraph(copyMorph.getImage());
						OrdinaryMorphism tmpMorph = savedMatch.compose(copyMorph);
						Match n = tmpMorph.makeMatch(savedMatch.getRule());
						n.setCompletionStrategy((MorphCompletionStrategy) this.grammar
								.getMorphismCompletionStrategy().clone(), true);
	
						boolean notFound = false;
						while (!n.isValid() && !notFound) {
							if (!n.nextCompletion())
								notFound = true;
						}
						if (!notFound) {
							if (applyRule(n)) {
								ruleApplied = true;
								try {
									Thread.sleep(this.delay);
								} catch (InterruptedException ex1) {
								}
							}
						}
					}
				} // end if(!ruleApplied && (savedMatch != null)
				if (!ruleApplied) {
					try {
						Pair<?,?> tmpPair = (Pair) this.stack.pop();
						/* backtrack */
						setHostGraph((Graph)tmpPair.first);
						eri = (RuleInstances)tmpPair.second;
					} catch (EmptyStackException ioe) {
						/* Stack ist leer */
						fireParserEvent(new ParserErrorEvent(this,
								"ERROR: This graph is not part of the language"));
						this.correct = false;
					}
				} // end if(!ruleApplied
			} // end if(!ruleApplied
		} // end while(!graph.isIsomorphicWith(stopGraph) && correct
		/* Fertig mit den Excluderegeln */

		while (!this.stack.empty()) {
			try {
				fireParserEvent(new ParserMessageEvent(this, "Cleaning stack."));
				Pair<?,?> tmpPair = (Pair) this.stack.pop();				
				Graph g = (Graph)tmpPair.first;
				g.dispose();
				tmpPair.second = null;
			} catch (EmptyStackException ioe) {
			}
		}

		fireParserEvent(new ParserMessageEvent(this,
				"Stopping parser. Result is " + this.correct + "."));
		return this.correct;
	}

	/**
	 * Clears some internal stuff.
	 */
	protected void finalize() {
		getHostGraph().dispose();
	}

	/* MUSs in ExcludePairContainer gehoert */
	/**
	 * A container stores all pairs with the information if two rules critic or
	 * not. This method filters all pairs that are not critic.
	 * 
	 * @param in
	 *            The complete container.
	 * @param out
	 *            The new filtered container.
	 */
	protected void makeLightContainer(
			Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> in,
			Hashtable<Rule, Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>> out) {
		// Report.println("-------------------------------------------",Report.CONTAINER);
		Enumeration<Rule> keys = in.keys();
		while (keys.hasMoreElements()) {
			Rule key = keys.nextElement();
			if (key != null) {
				// Report.println("erster key "+key,Report.CONTAINER);
				Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> value = in
						.get(key);
				// Report.println("erster Wert "+value,Report.CONTAINER);
				boolean allTrue = true;
				Enumeration<Rule> keys2 = value.keys();
				while (keys2.hasMoreElements() && allTrue) {
					Rule key2 = keys2.nextElement();
					if (key == key2)
						continue;
					// Report.println("zweiter key "+key2,Report.CONTAINER);
					Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>> p = value.get(key2);
					// Report.println("zweiter Wert "+p,Report.CONTAINER);
					Boolean first = p.first;
					// allTrue = allTrue && first.booleanValue();
					if (first.booleanValue()) {
						if (out.containsKey(key)) {
							Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> secondPart = out
									.get(key);
							secondPart.put(key2, p);
						} else {
							Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>> secondPart 
							= new Hashtable<Rule, Pair<Boolean, Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>>>();
							secondPart.put(key2, p);
							out.put(key, secondPart);
						}
					}
				}
			}
			// else System.out.println("ExcludeParser.makeLightContainer key ==
			// null");
			/*
			 * if(allTrue){ out.put(key,value); }
			 */
		}
	}

	/* finds all inclusions from the value of the exclude hashtable */
	/**
	 * Returns all inclusion of the overlapping of a given rule into the host
	 * graph.
	 * 
	 * @param r1
	 *            The rule
	 * @param kind
	 *            The critical pair algorithm.
	 * @return A set of morphisms from the overlapping graph into the host
	 *         graph.
	 */
	protected Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>> findInclusions(Rule r1, int kind) {
		Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>> resultVector = new Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>>();
		Vector<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> criticVector = null;
		try {
			criticVector = this.pairContainer.getCriticalSet(kind, r1);
		} catch (InvalidAlgorithmException iae) {
			fireParserEvent(new ParserErrorEvent(this, "Cannot get Container"));
			return resultVector;
		}
		for (int i = 0; i < criticVector.size(); i++) {
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> p = criticVector.elementAt(i);
			// Pair result = new Pair(p.first.first, p.first.second);
			Pair<OrdinaryMorphism, OrdinaryMorphism> morphisms = p.first;
			if (p.second != null) {
				if (morphisms.first.getSource() == r1
						.getRight()) {
					// handle produce-forbid overlapping
					Graph overlap = morphisms.first.getImage();
					OrdinaryMorphism overlapIso = overlap.isomorphicCopy();
					if (overlapIso != null) {
						Iterator<?> e =  morphisms.first.getSource().getArcsSet().iterator();
						while (e.hasNext()) {
							Arc obj = (Arc) e.next();
							if (!r1.getInverseImage(obj).hasMoreElements()) {
								try {
									overlapIso.getTarget()
									.destroyObject(overlapIso.getImage(morphisms.first.getImage(obj)));
								} catch (TypeException exc) {}
							}
						}
						e = morphisms.first.getSource().getNodesSet().iterator();
						while (e.hasNext()) {
							Node obj = (Node) e.next();
							if (!r1.getInverseImage(obj).hasMoreElements()) {
								try {
									overlapIso.getTarget()
									.destroyObject(overlapIso.getImage(morphisms.first.getImage(obj)));
								} catch (TypeException exc) {}
							}
						}
						OrdinaryMorphism matchMorph1 = r1.compose(morphisms.first).compose(overlapIso);
						Match match1 = BaseFactory.theFactory().makeMatch(r1,
								matchMorph1);
						// match1.getTarget().unsetTransientAttrValues();
						morphisms.first = match1;
						// result = new Pair(match1, p.first.second);
						// System.out.println("RHS: makeMatch...: "+match1);
					}
				}
			}
			Graph overlapGraph = morphisms.first.getImage();
			// Graph overlapGraph = result.first.getImage();
			OrdinaryMorphism inclusion = BaseFactory.theFactory()
					.createMorphism(overlapGraph, getHostGraph());
			boolean graphOk = false;
			while (inclusion.nextCompletion() && !graphOk) {
				if (inclusion.isTotal() && inclusion.isInjective()) {
					graphOk = true;
					resultVector.addElement(morphisms);
					// resultVector.addElement(result);
				}
			}
			BaseFactory.theFactory().destroyMorphism(inclusion);
		}
		return resultVector;
	}

	/**
	 * Checks if a current match is critic.
	 * 
	 * @param m
	 *            The match.
	 * @param inclusions
	 *            The set of inclusions from overlapping graphs into the host
	 *            graph.
	 * @return true if with this one rule exclude another rule.
	 */
	protected boolean isMatchCritic(Match m, Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>> inclusions) {
		/* check3a */
		// Report.trace("isMatchCritic: anfang",2);
		boolean critic = false;
		OrdinaryMorphism o = null;
		OrdinaryMorphism composed = null;
		for (int i = 0; i < inclusions.size(); i++) {
//			Report.println("ExcludeParser.isMatchCritic:: checke Inklusion #"
//					+ i + " von " + inclusions.size(), Report.PARSER);
			Pair<OrdinaryMorphism, OrdinaryMorphism> morphisms = inclusions.elementAt(i);
			Graph overlapGraph = morphisms.first.getImage();
			o = BaseFactory.theFactory().createMorphism(overlapGraph,
					getHostGraph());
			OrdinaryMorphism overlapMorph = morphisms.first;
			OrdinaryMorphism inverted = overlapMorph.invert();
			composed = inverted.compose(m);
			/*
			 * agg.util.Debug.printlnMorph(m,"der Match m");
			 * agg.util.Debug.printlnMorph(overlapMorph,"overlapMorph");
			 * agg.util.Debug.printlnMorph(inverted,"inverted");
			 */
			// System.out.println(i+": composed match: "+composed);
			while (composed.nextCompletion() && !critic) {
				/* agg.util.Debug.printlnMorph(composed,"composed"); */
				Vector<GraphObject> leftNodes = new Vector<GraphObject>();
				for (Enumeration<GraphObject> en = m.getDomain(); en.hasMoreElements();) {
					GraphObject grob = en.nextElement();
					if (grob.isNode())
						leftNodes.addElement(grob);
				}
				for (int k = 0; k < leftNodes.size(); k++) {
					Node n = (Node) leftNodes.elementAt(k);
					/*
					 * System.out.println("versuche "+n+" "+k+" zu mappen auf
					 * "+m.getImage(n));
					 */
					try {
						o.addMapping(overlapMorph.getImage(n), m.getImage(n));
					} catch (BadMappingException bme) {
						// System.err.println("ExcludeParser.isMatchCritic::
						// small panic, but hopefully only performance lost");
						// n+" an stelle "+k+" konnte nicht gemappt werden.");
					}
				}
				while (o.nextCompletion() && !critic) {
					// System.out.println("suche next Completion of overlap
					// morphism: "+(j++));
					/*
					 * agg.util.Debug.printlnMorph(o,"der o Morph");
					 * System.out.print("\t"+(j++));
					 */
					if (o.isIsomorphicTo(composed))
						critic = true;
				}
			}
			BaseFactory.theFactory().destroyMorphism(composed);
			BaseFactory.theFactory().destroyMorphism(o);
		}
//		Report.trace("isMatchCritic: Ende " + critic, -2);
		// System.out.println("isMatchCritic: Ende "+critic);
		return critic;
	}

	/**
	 * Usually this method is invoked by the start method from the class <CODE>Thread</CODE>.
	 * This method starts the parse method.
	 */
	public void run() {
		fireParserEvent(new ParserMessageEvent(this,
				"Thread - Parsing -  runs ..."));
		parse();
		if (this.stop) {
			fireParserEvent(new ParserMessageEvent(this,
					"Thread - Parsing -  was stopped."));
			this.stop = false;
		} else
			fireParserEvent(new ParserMessageEvent(this,
					"Thread - Parsing -  finished. Result is " + this.correct + "."));
	}

	/**
	 * Stops the running.
	 */
	public void stop() {
		this.stop = true;
	}

	public boolean wasStopped() {
		return this.stop;
	}

}

// End of ExcludeParser.java
/*
 * $Log: ExcludeParser.java,v $
 * Revision 1.17  2010/08/18 09:26:52  olga
 * tuning
 *
 * Revision 1.16  2010/06/09 10:56:07  olga
 * tuning
 *
 * Revision 1.15  2010/03/04 14:11:13  olga
 * code optimizing
 *
 * Revision 1.14  2008/05/05 09:11:51  olga
 * Graph parser - bug fixed.
 * New AGG feature - Applicability of Rule Sequences - in working.
 *
 * Revision 1.13  2008/04/07 09:36:51  olga
 * Code tuning: refactoring + profiling
 * Extension: CPA - two new options added
 *
 * Revision 1.12  2007/11/05 09:18:22  olga
 * code tuning
 *
 * Revision 1.11  2007/11/01 09:58:17  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.10  2007/10/22 09:03:17  olga
 * First implementation of CPA for grammars with node type inheritance.
 * Only for internal tests.
 *
 * Revision 1.9  2007/09/10 13:05:41  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.8 2007/06/13 08:32:58 olga Update:
 * V161
 * 
 * Revision 1.7 2007/04/11 10:03:36 olga Undo, Redo tuning, Simple Parser- bug
 * fixed
 * 
 * Revision 1.6 2007/03/28 10:00:58 olga - extensive changes of Node/Edge Type
 * Editor, - first Undo implementation for graphs and Node/edge Type editing and
 * transformation, - new / reimplemented options for layered transformation, for
 * graph layouter - enable / disable for NACs, attr conditions, formula - GUI
 * tuning
 * 
 * Revision 1.5 2006/11/09 10:31:05 olga Matching error fixed
 * 
 * Revision 1.4 2006/03/13 10:04:42 olga CPA tuning
 * 
 * Revision 1.3 2006/03/02 12:03:23 olga CPA: check host graph - done
 * 
 * Revision 1.2 2006/03/01 09:55:46 olga - new CPA algorithm, new CPA GUI
 * 
 * Revision 1.1 2005/08/25 11:56:57 enrico *** empty log message ***
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.4 2004/12/20 14:53:48 olga Changes because of matching
 * optimisation.
 * 
 * Revision 1.3 2004/01/28 17:58:38 olga Errors suche
 * 
 * Revision 1.2 2003/03/05 18:24:08 komm sorted/optimized import statements
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:23 olga Imported sources
 * 
 * Revision 1.11 2001/07/04 10:45:34 olga Mehr Testarbeit.
 * 
 * Revision 1.10 2001/06/18 13:37:46 olga Bei Critical Pair ein neuer Menuitem:
 * Debug, wo man einzelne Regelpaare testen kann. System.gc() eingefuegt.
 * 
 * Revision 1.9 2001/06/13 16:49:35 olga Parser Classen Optimierung.
 * 
 * Revision 1.8 2001/05/31 11:57:55 olga Zusaetzliche Parser Meldungen fuer GUI
 * eingefuegt.
 * 
 * Revision 1.7 2001/05/14 12:03:00 olga Zusaetzliche Parser Events Aufrufe
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
 * Revision 1.3 2001/03/14 17:32:05 olga nur Tests
 * 
 * Revision 1.1.2.16 2001/01/28 13:14:53 shultzke API fertig
 * 
 * Revision 1.1.2.15 2001/01/03 09:45:00 shultzke TODO's bis auf laden und
 * speichern erledigt. Wann meldet sich endlich Michael?
 * 
 * Revision 1.1.2.14 2001/01/02 12:28:58 shultzke Alle Optionen angebunden
 * 
 * Revision 1.1.2.13 2001/01/01 21:24:32 shultzke alle Parser fertig inklusive
 * Layout
 * 
 * Revision 1.1.2.12 2000/12/26 10:00:03 shultzke Layered Parser hinzugefuegt
 * 
 * Revision 1.1.2.11 2000/12/04 12:26:47 shultzke drei parser stehen zur
 * verfuegung
 * 
 * Revision 1.1.2.10 2000/11/28 09:54:50 shultzke stack aufgeraeumt nach parsing
 * 
 * Revision 1.1.2.9 2000/11/27 13:16:45 shultzke referenzparser SimpleParser
 * implementiert
 * 
 * Revision 1.1.2.8 2000/11/14 14:02:11 shultzke check3a(isMatchCritic) bug
 * fixed
 * 
 * Revision 1.1.2.7 2000/11/08 14:58:02 shultzke parser erste stufe fertig
 * 
 * Revision 1.1.2.6 2000/11/06 14:02:36 shultzke der Parser laeuft, wenn auch
 * langsam
 * ----------------------------------------------------------------------
 * 
 * Revision 1.1.2.5 2000/11/02 14:50:14 shultzke der konfliktteil des parsers
 * wird etwas trickreich
 * 
 * Revision 1.1.2.4 2000/11/01 14:55:25 shultzke conflictfree part fast fertig
 * 
 * Revision 1.1.2.3 2000/11/01 12:19:21 shultzke erste Regelanwendung im parser
 * CVs: ----------------------------------------------------------------------
 * 
 * Revision 1.1.2.2 2000/10/25 13:43:44 shultzke parser start dialog
 * 
 * Revision 1.1.2.1 2000/09/18 14:11:24 shultzke erstes Geruest des Parsers
 * erstellt. Und parallelen Ablauf der ExcludePairs synchronisiert
 * 
 */
