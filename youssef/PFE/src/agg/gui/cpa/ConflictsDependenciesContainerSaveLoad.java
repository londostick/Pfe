/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
/**
 * 
 */
package agg.gui.cpa;

import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdGraph;
import agg.parser.ConflictsDependenciesContainer;
import agg.parser.PairContainer;
import agg.util.XMLHelper;

/**
 * @author olga
 *
 */
public class ConflictsDependenciesContainerSaveLoad extends
		ConflictsDependenciesContainer {

	private EdGraGra grammar;
	
	public ConflictsDependenciesContainerSaveLoad() {
		super();		
	}
	
	public ConflictsDependenciesContainerSaveLoad(
			final PairContainer conflict,
			final PairContainer dependency,
			final EdGraph graph,
			final EdGraGra gragra) {
				
		super(conflict, dependency, graph);
		this.grammar = gragra;		
	}
	
	public EdGraGra getPairsGraGra() {
		return this.grammar;
	}
	
	protected boolean writeLayoutGrammar(XMLHelper h) {
		if (this.grammar != null) {
			h.addObject("GraphTransformationSystem", this.grammar, false);
			return true;
		} 
		return false;
	}
	
	
	protected void readLayoutGrammar(XMLHelper h) {
		this.grammar = new EdGraGra(this.pairsGrammar);
//		this.grammar.getBasisGraGra().prepareRuleInfo();
		h.enrichObject(this.grammar);
	}
	

	
}
