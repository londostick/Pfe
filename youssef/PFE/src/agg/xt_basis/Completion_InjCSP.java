package agg.xt_basis;

import agg.xt_basis.csp.CompletionPropertyBits;
import agg.xt_basis.csp.Completion_CSP;

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
 * An implementation of morphism completion as a Constraint Satisfaction Problem (CSP), 
 * considering injective solutions only.
 */
public class Completion_InjCSP extends Completion_CSP {
	
	public Completion_InjCSP() {
		super();
		// set default properties:
		getProperties().set(CompletionPropertyBits.INJECTIVE);
		getProperties().set(CompletionPropertyBits.DANGLING);
		getProperties().set(CompletionPropertyBits.IDENTIFICATION);
	}

	public Completion_InjCSP(boolean randomizeDomain) {
		super(randomizeDomain);
		// set default properties:
		getProperties().set(CompletionPropertyBits.INJECTIVE);
		getProperties().set(CompletionPropertyBits.DANGLING);
		getProperties().set(CompletionPropertyBits.IDENTIFICATION);
	}

}
