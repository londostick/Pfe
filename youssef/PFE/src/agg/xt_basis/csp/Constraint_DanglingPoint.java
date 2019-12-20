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
import agg.xt_basis.Node;

public class Constraint_DanglingPoint extends BinaryConstraint {
	
	private GraphObject itsGraphObj;

	public Constraint_DanglingPoint(GraphObject graphobj, Variable var) {
		super(var, 0);
		this.itsGraphObj = graphobj;
	}

	public void clear() {
		this.itsVar1 = null;
		this.itsGraphObj = null;
	}
	
	public final boolean execute() {
		if (this.itsGraphObj.isNode()
				&& getVar1().getInstance() instanceof GraphObject) {
			Node y = (Node) getVar1().getInstance();
			if (y != null
					&& ((Node) this.itsGraphObj).getNumberOfArcs() != y.getNumberOfArcs()) {	
				System.out.println("DanglingPoint constraint at node FAILED!");
				return false;
			}			
		}
		return true;
	}

	public GraphObject getGraphObject() {
		return this.itsGraphObj;
	}
}
