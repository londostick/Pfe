/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.xt_basis.csp;

import agg.util.csp.SolutionStrategy;
import agg.util.csp.Solution_Backtrack;

/**
 * An implementation of morphism completion as a Constraint Satisfaction Problem
 * (CSP). It searches for injective solutions only per default, and it does not
 * use backjumping.
 */
public class Completion_CSP_NoBJ extends Completion_CSP {
	public Completion_CSP_NoBJ() {
		super(true);
		
		getProperties().set(CompletionPropertyBits.INJECTIVE);
		getProperties().set(CompletionPropertyBits.DANGLING);
		
		super.itsName = "CSP w/o BJ";
	}

	protected SolutionStrategy createSolutionStrategy() {
		return new Solution_Backtrack(getProperties().get(CompletionPropertyBits.INJECTIVE));
	}
}
