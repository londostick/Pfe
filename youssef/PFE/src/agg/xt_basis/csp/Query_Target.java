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

import java.util.HashSet;
//import java.util.LinkedHashSet;

import agg.util.csp.Query;
import agg.util.csp.Variable;
import agg.xt_basis.Arc;
import agg.xt_basis.GraphObject;

public class Query_Target extends Query {
	
	final private HashSet<GraphObject> itsSet = new HashSet<GraphObject>(1);

	/**
	 * Construct myself to be a unary query for the target of <code>arc</code>.
	 */
	public Query_Target(Variable arc, Variable tar) {
		super(arc, tar, 0);

//		itsSet.setSize(1);
	}

	public final HashSet<?> execute() {//GraphObject
		this.itsSet.clear();
		this.itsSet.add(((Arc) getSourceInstance(0)).getTarget());
		return this.itsSet;
	}
	
	public final int getSize() {
		return 1;
	}

	/**
	 * Return the name of this class.
	 */
	public final String getKind() {
		return "Query_Target";
	}

	public boolean isDomainEmpty() {
		return false;
	}

}
