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
package agg.xt_basis;

/**
 * @author olga
 *
 */
public class GraphKind {

	/**
	 * possible graph kind
	 */
	public final static String GRAPH = "GRAPH"; // default, when any other kind isn't set

	public final static String TG = "TG"; // TypeGraph
	
	public final static String HOST = "HOST"; // host graph
	
	public final static String LHS = "LHS"; // of Rule
	
	public final static String RHS = "RHS"; // of Rule
	
	public final static String NAC = "NAC"; // of Rule
	
	public final static String PAC = "PAC"; // of Rule
	
	public final static String AC = "AC"; // of Rule
	
	public final static String PREMISE = "PREMISE"; // of AtomConstraint
	
	public final static String CONCLUSION = "CONCLUSION"; // of AtomConstraint
	
	public final static String SOURCE = "SOURCE"; // of OrdinaryMorphism
	
	public final static String TARGET = "TARGET"; // of OrdinaryMorphism
	
}
