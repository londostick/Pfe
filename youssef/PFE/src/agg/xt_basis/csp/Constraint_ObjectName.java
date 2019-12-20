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
import agg.xt_basis.GraphObject;

public class Constraint_ObjectName extends BinaryConstraint {
	
	private GraphObject itsGraphObj;

	public Constraint_ObjectName(GraphObject graphobj, Variable var) {
		super(var, 0);
		this.itsGraphObj = graphobj;
	}

	public void clear() {
		this.itsVar1 = null;
		this.itsGraphObj = null;
	}
	
	/**
	 * Return true iff the current instance of <code>obj</code> is type
	 * compatible with the GraphObject that has been passed to my constructor.
	 * In this case, "type" means an element of the cartesian product of
	 * <code>Type</code> x {Node,Arc}.
	 * <p>
	 * Pre: (1) obj.getInstance() instanceof GraphObject.
	 */
	public final boolean execute() {
//		if (getVar1().getInstance() instanceof GraphObject) 
			if (this.itsGraphObj.getObjectName().equals(((GraphObject) getVar1().getInstance()).getObjectName())) {
				return true;
			}
		
		return false;
	}

	public GraphObject getGraphObject() {
		return this.itsGraphObj;
	}
}
