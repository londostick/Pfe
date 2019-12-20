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

import agg.util.csp.BinaryConstraint;
import agg.util.csp.Variable;
import agg.xt_basis.Arc;

public class Constraint_TargetSource extends BinaryConstraint {
	
	public Constraint_TargetSource(Variable src, Variable arc) {
		super(src, arc, 3);
	}

	public void clear() {
		this.itsVar1 = null;
		this.itsVar2 = null;
	}
	
	/**
	 * Return true iff the current instance of <code>src</code> is the source
	 * or target object of the instance of an undirected <code>arc</code>.
	 * <p>
	 * Pre: (1) src.getInstance(), arc.getInstance() instanceof GraphObject.
	 */
	public final boolean execute() {
		boolean result = (getVar1().getInstance() == ((Arc) getVar2().getInstance()).getTarget()
						|| getVar1().getInstance() == ((Arc) getVar2().getInstance()).getSource());
		return result;
	}
}
