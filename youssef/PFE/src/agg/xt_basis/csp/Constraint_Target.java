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

public class Constraint_Target extends BinaryConstraint {
	
	public Constraint_Target(Variable tar, Variable arc) {
		super(tar, arc, 3);
	}

	public void clear() {
		this.itsVar1 = null;
		this.itsVar2 = null;
	}
	
	/**
	 * Return true iff the current instance of <code>tar</code> is the target
	 * object of the instance of <code>arc</code>.
	 * <p>
	 * Pre: (1) tar.getInstance(), arc.getInstance() instanceof GraphObject.
	 */
	public final boolean execute() {
		boolean result = (getVar1().getInstance().equals(((Arc) getVar2().getInstance()).getTarget()));
		// System.out.println("Constraint_Target.execute:: "+result+"
		// "+((GraphObject)getVar1().getGraphObject()).getType().getName()+"
		// "+((Arc) getVar2().getInstance()).getTarget().getType().getName());
		return result;
	}
}
