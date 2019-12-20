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
import java.util.Iterator;
import java.util.LinkedHashSet;

import agg.util.csp.Query;
import agg.util.csp.Variable;
import agg.xt_basis.Arc;
import agg.xt_basis.UndirectedArc;
import agg.xt_basis.Node;

public class Query_OutgoingIncoming extends Query {
	
	private String arcKey;
	private String arcKey2;	
	private boolean withNTI; // graph with Node Type Inheritance
	
	/**
	 * Construct myself to be a binary query for outgoing arcs of
	 * <code>obj</code> with abstraction <code>abs</code>.
	 */
	public Query_OutgoingIncoming(Variable obj, Variable tar) {
		super(obj, tar, 6);
		
		this.arcKey = ((Arc)this.itsTarget.getGraphObject()).convertToKey();
		this.arcKey2 = ((UndirectedArc)this.itsTarget.getGraphObject()).convertToInverseKey();
		this.withNTI = ((Arc)this.itsTarget.getGraphObject()).getContext().getTypeSet().hasInheritance();
	}
	
	public final HashSet<?> execute() {//Arc
		// nodes of an undirected graph store all arcs as the outgoing arcs 
		if (this.withNTI) {
			return ((Node) getSourceInstance(0)).getOutgoingArcsSet();
		} 
		
		HashSet<Arc> outs = ((Node) getSourceInstance(0)).getOutgoingArcsSet();
		HashSet<Arc> result = new LinkedHashSet<Arc>(); 
		Iterator<Arc> iter = outs.iterator();
		while (iter.hasNext()) {
			Arc a = iter.next();
			if (a.convertToKey().equals(this.arcKey)
					|| a.convertToKey().equals(this.arcKey2)) {
				result.add(a);
			}
		}
		
		return result;
	}
	
	public final int getSize() {
		return getTarget().getTypeQuery().getAvgOutgoingDegree();
	}

	/**
	 * Return the name of this class.
	 */
	public final String getKind() {
		return "Query_OutgoingIncoming";
	}

	/* (non-Javadoc)
	 * @see agg.util.csp.Query#isDomainEmpty()
	 */
	@Override
	public boolean isDomainEmpty() {
		// TODO Auto-generated method stub
		return false;
	}
}
