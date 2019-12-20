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

import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

import agg.util.IntComparator;
import agg.util.OrderedSet;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.GraGra;
import agg.xt_basis.Graph;
import agg.xt_basis.Match;
import agg.xt_basis.MorphCompletionStrategy;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.RuleLayer;


// ---------------------------------------------------------------------------+
/**
 * This class provides a parser which works without critical pair analysis. So a
 * simple backtracking algorithm is implemented. The only optimization can made
 * by the layer function.
 * 
 * @see ParserFactory#createParser createParser(...)
 * @author $Author: olga $ Parser Group
 * @version $Id: LayeredSimpleParser.java,v 1.14 2010/08/18 09:26:52 olga Exp $
 */
public class LayeredSimpleParser extends SimpleParser {

	/**
	 * The layer function for the parser.
	 */
	protected RuleLayer layer;

	/**
	 * Creates a new parser.
	 * 
	 * @param grammar
	 *            The graph grammar.
	 * @param hostGraph
	 *            The host graph.
	 * @param stopGraph
	 *            The stop graph.
	 * @param layer
	 *            The layer function.
	 * @deprecated
	 */
	public LayeredSimpleParser(GraGra grammar, Graph hostGraph,
			Graph stopGraph, LayerFunction layer) {
		super(grammar, hostGraph, stopGraph);
		// this.layer = layer;
	}

	/**
	 * Creates a new parser.
	 * 
	 * @param grammar
	 *            The graph grammar.
	 * @param hostGraph
	 *            The host graph.
	 * @param stopGraph
	 *            The stop graph.
	 */
	public LayeredSimpleParser(GraGra grammar, Graph hostGraph,
			Graph stopGraph, RuleLayer layer) {
		super(grammar, hostGraph, stopGraph);
		this.layer = layer;
	}

	// -----------------------------------------------------------------------+
	/**
	 * Starts the parser.
	 * 
	 * @return true if the graph can be parsed.
	 */
	@SuppressWarnings("rawtypes")
	public boolean parse() {
//		System.out.println("### Starting layered simple parser ...");
		fireParserEvent(new ParserMessageEvent(this,
		"Starting layered simple parser ..."));
		
		Stack<TripleData> stack = new Stack<TripleData>();
		this.correct = true;
		
		Hashtable<Integer, HashSet<Rule>>  invertedRuleLayer = this.layer.invertLayer();		
		OrderedSet<Integer> ruleLayer = new OrderedSet<Integer>(new IntComparator<Integer>());
		for (Enumeration<Integer> en = invertedRuleLayer.keys(); en.hasMoreElements();)
			ruleLayer.add(en.nextElement());
		
		Integer currentLayer = this.layer.getStartLayer();
		int i=0;

		/*
		 * haelt alle Matche, die kritisch sind, damit nicht an einer Stelle
		 * immer wieder angesetzt wird
		 */
		RuleInstances eri = new RuleInstances();
		boolean ruleApplied = false;
		while (!getHostGraph().isIsomorphicTo(this.stopGraph) && this.correct
				&& (currentLayer != null) && !this.stop) {
			ruleApplied = false;
			fireParserEvent(new ParserMessageEvent(this, "Searching for match"));
			HashSet rulesForLayer = invertedRuleLayer.get(currentLayer);
			Match m = findMatch(getHostGraph(), rulesForLayer.iterator(), eri);

			if (m != null) {
				/* auf Stack pushen */
				OrdinaryMorphism copyMorph = getHostGraph().isomorphicCopy();
				if (copyMorph != null) {
					fireParserEvent(new ParserMessageEvent(copyMorph, "IsoCopy"));
					eri.add(m);
					/*
					 * ERI muss nicht kopiert werden, da nur an Entscheidungsstellen
					 * der Match/die Matches gemerkt werden mssen, die uns
					 * moeglicherweise auf einen Holzweg fuehren. Der Match in ERI
					 * ist eine Stufe tiefer (also nach Regelanwendung, denn wir
					 * l&ouml;schen) nicht mehr verfgbar. Dadurch kann ein neues ERI
					 * erzeugt werden. Auf dem Stack liegen dann nur die
					 * Ableitungen, die uns in eine Sackgasse gefhrt haben.
					 */
					TripleData tmpTriple = new TripleData(getHostGraph(), eri, currentLayer);
					stack.push(tmpTriple);
					eri = new RuleInstances();
					/*
					 * Die Regel muss auf den kopierten Graphen mit DEMSELBEN
					 * kopierten Match angewendet werden.
					 */
					setHostGraph(copyMorph.getImage());
					OrdinaryMorphism tmpMorph = m.compose(copyMorph);
					Match n = tmpMorph.makeMatch(m.getRule());
					n.setCompletionStrategy((MorphCompletionStrategy) this.grammar
							.getMorphismCompletionStrategy().clone(), true);
	
					boolean found = true;
					while (!this.stop && !n.isValid() && found) {
						if (!n.nextCompletion())
							found = false;
					}
	
					if (found) {
						if (applyRule(n)) {
							ruleApplied = true;
							try {
								Thread.sleep(this.delay);
							} catch (InterruptedException ex1) {
							}
						}
					}
				}
			}
			if (m == null || !ruleApplied) {
				i++;
				boolean nextLayerExists = true;
				if (i < ruleLayer.size()) {
					currentLayer = ruleLayer.get(i);
				}
				else {
					nextLayerExists = false;
				}
				
				/* backtracking*/
				if (!nextLayerExists) {
					try {
						TripleData tmpTriple = stack.pop();
						/* backtrack */
						setHostGraph(tmpTriple.getHostGraph());
						eri = tmpTriple.getRuleInstance();
						currentLayer = tmpTriple.getLayer();
					} catch (EmptyStackException ioe) {
						/* Stack ist leer */
						fireParserEvent(new ParserErrorEvent(this,
								"ERROR: This graph is not part of the language"));
						this.correct = false;
					}
				}
			}
		}
		while (!stack.empty()) {
			try {
				fireParserEvent(new ParserMessageEvent(this, "Cleaning stack."));
				TripleData tmpTriple = stack.pop();
				Graph g = tmpTriple.getHostGraph();
				BaseFactory.theFactory().destroyGraph(g);
			} catch (EmptyStackException ioe) {
			}
		}
		fireParserEvent(new ParserMessageEvent(this,
				"Stopping parser. Result is " + this.correct + "."));
		System.out
				.println("### LayeredSimpleParser ... Stopping parser. Result is "
						+ this.correct + ".");
		return this.correct;
	}

}

/*
 * End of Parser.java
 * ----------------------------------------------------------------------------
 * $Log: LayeredSimpleParser.java,v $
 * Revision 1.14  2010/08/18 09:26:52  olga
 * tuning
 *
 * Revision 1.13  2010/06/09 10:56:06  olga
 * tuning
 *
 * Revision 1.12  2009/07/08 16:22:02  olga
 * Multiplicity bug fixed;
 * ARS development
 *
 * Revision 1.11  2008/11/19 13:04:17  olga
 * Parser tuning
 *
 * Revision 1.10  2008/04/07 09:36:50  olga
 * Code tuning: refactoring + profiling
 * Extension: CPA - two new options added
 *
 * Revision 1.9  2007/11/05 09:18:22  olga
 * code tuning
 *
 * Revision 1.8  2007/11/01 09:58:17  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.7  2007/09/10 13:05:39  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.6 2007/06/13 08:32:56 olga
 * Update: V161
 * 
 * Revision 1.5 2007/04/11 10:03:36 olga Undo, Redo tuning, Simple Parser- bug
 * fixed
 * 
 * Revision 1.4 2007/03/28 10:00:52 olga - extensive changes of Node/Edge Type
 * Editor, - first Undo implementation for graphs and Node/edge Type editing and
 * transformation, - new / reimplemented options for layered transformation, for
 * graph layouter - enable / disable for NACs, attr conditions, formula - GUI
 * tuning
 * 
 * Revision 1.3 2006/12/13 13:32:59 enrico reimplemented code
 * 
 * Revision 1.2 2006/11/09 10:31:05 olga Matching error fixed
 * 
 * Revision 1.1 2005/08/25 11:56:57 enrico *** empty log message ***
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.4 2004/12/20 14:53:48 olga Changes because of matching
 * optimisation.
 * 
 * Revision 1.3 2003/03/05 18:24:08 komm sorted/optimized import statements
 * 
 * Revision 1.2 2002/09/19 16:20:15 olga Nur Testausgaben weg.
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:24 olga Imported sources
 * 
 * Revision 1.5 2001/09/24 16:41:33 olga
 * 
 * Arbeit an LayerFunction und LayeredParser.
 * 
 * Revision 1.4 2001/05/31 11:57:57 olga Zusaetzliche Parser Meldungen fuer GUI
 * eingefuegt.
 * 
 * Revision 1.3 2001/05/14 12:03:01 olga Zusaetzliche Parser Events Aufrufe
 * eingebaut, um bessere Kommunikation mit GUI Status Anzeige zu ermoeglichen.
 * 
 * Revision 1.2 2001/03/08 10:44:55 olga Neue Files aus parser branch in Head
 * eingefuegt.
 * 
 * Revision 1.1.2.3 2001/01/28 13:14:56 shultzke API fertig
 * 
 * Revision 1.1.2.2 2001/01/02 12:29:00 shultzke Alle Optionen angebunden
 * 
 * Revision 1.1.2.1 2000/12/26 10:00:05 shultzke Layered Parser hinzugefuegt
 * 
 */
