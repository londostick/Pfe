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

import java.util.EventObject;

//****************************************************************************+
/**
 * GraTraEvents liefern Nachrichten &uuml;ber den Status einer Transformation
 * oder aufgetretene Fehler.
 * 
 * @author Gabi
 */
@SuppressWarnings("serial")
public class GraTraEvent extends EventObject {

	public static final int INPUT_PARAMETER_NOT_SET = 1;

	public static final int STEP_COMPLETED = 2;

	public static final int NO_COMPLETION = 3;

	public static final int CANNOT_TRANSFORM = 4;

	public static final int TRANSFORM_FINISHED = 5;

	public static final int NOT_READY_TO_TRANSFORM = 6;

	public static final int NEW_MATCH = 7;

	public static final int MATCH_VALID = 8;

	public static final int RULE_FAILED = 9;

	public static final int GRAPH_FAILED = 10;

	public static final int INCONSISTENT = 11;

	public static final int TRANSFORM_STOP = 12;

	public static final int GRAPH_INCOMPLETE = 13;

	public static final int ATOMIC_GC_FAILED = 14;

	public static final int ATTR_TYPE_FAILED = 15;

	public static final int LAYER_FINISHED = 16;
	
	public static final int DESTROY_MATCH = 17;
	
	public static final int TRANSFORM_START = 18;
	
	public static final int TRANSFORM_FAILED = 19;
	
	public static final int RESET_GRAPH = 20;
	
	public static final int MATCH_FAILED = 21;
	
	public static final int RULE = 22;
	
	public static final int RULE_SCHEME = 23;
	
	public static final int STEP_COMPLETED_INVALID = 24;
	
	
	protected int message;

	protected Graph graph;
	protected Rule rule;
	protected Match match;

	protected String text;

	
	public GraTraEvent(Object source, int _message, Graph g) {
		super(source);
		this.message = _message;
		this.graph = g;
		this.text = "";
	}

	public GraTraEvent(Object source, int _message, Rule r) {
		super(source);
		this.message = _message;
		this.rule = r;
		this.text = "";
	}
	
	public GraTraEvent(Object source, int _message, Match m) {
		super(source);
		this.message = _message;
		this.match = m;
		this.text = "";
	}

	public GraTraEvent(Object source, int _message, Match m, String text) {
		super(source);
		this.message = _message;
		this.match = m;
		this.text = text;
	}

	public GraTraEvent(Object source, int _message) {
		super(source);
		this.message = _message;
		this.text = "";
	}

	public GraTraEvent(Object source, int _message, String text) {
		super(source);
		this.message = _message;
		this.text = text;
	}

	public int getMessage() {
		return this.message;
	}

	public Graph getGraph() {
		return this.graph;
	}

	public Match getMatch() {
		return this.match;
	}

	public Rule getRule() {
		return this.rule;
	}
	
	public Morphism getCoMatch() {
		if (this.match != null)
			return this.match.getCoMorphism();
		
		return null;		
	}

	public String getMessageText() {
		return this.text;
	}

}
