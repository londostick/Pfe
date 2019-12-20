/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.cons;

import java.util.Vector;
import agg.util.Pair;


public class AtomApplConstraint implements Evaluable {

	private int old_tick;

	private boolean old_val;

	private Vector<Object> atomApplConditions; // Element is AtomApplCond

	private int n; // counter

	public AtomApplConstraint(Vector<Object> atomApplConditions) {
		this.atomApplConditions = atomApplConditions;
		this.old_tick = -1;
		this.old_val = false;
	}

	public boolean eval(java.lang.Object o) {
		return eval(o, -1);
	}

	@SuppressWarnings("rawtypes")
	public boolean eval(java.lang.Object o, int tick) {
		if (tick != -1 && tick == this.old_tick)
			return this.old_val;
		this.old_tick = tick;
		this.old_val = eval((Pair) o);
		return this.old_val;
	}

	public boolean eval(java.lang.Object o, boolean negate) {
		return eval(o, -1, negate);
	}

	@SuppressWarnings("rawtypes")
	public boolean eval(java.lang.Object o, int tick, boolean negate) {
		if (tick != -1 && tick == this.old_tick)
			return this.old_val;
		this.old_tick = tick;
		this.old_val = eval((Pair) o, negate);
		return this.old_val;
	}


//	 Pair<OrdinaryMorphism, OrdinaryMorphism>
	private boolean eval(Pair<?,?> pair) {
		return eval(pair, false);
	}

//	 Pair<OrdinaryMorphism, OrdinaryMorphism>
	private boolean eval(Pair<?,?> pair, boolean negate) {

		if ((this.atomApplConditions == null) 
				|| (this.atomApplConditions.size() == 0)) {
			return true;
		}
		
		// to get co-match morphism :: (OrdinaryMorphism) pair.first;
		// to get match morphism :: (Match) pair.second;
		boolean result = true;
		this.n = 0;
		for (int i = 0; i < this.atomApplConditions.size(); i++) {
			AtomApplCond aac = (AtomApplCond) this.atomApplConditions.get(i);
			result = aac.eval(pair, negate);
			this.n = this.n + aac.getConditionMatchCounter();
			if (result)
				break;
		}
		return result;
	}

	public int getConditionMatchCounter() {
		return this.n;
	}

	public String getAsString() {
		return new String("");
	}

	/* (non-Javadoc)
	 * @see agg.cons.Evaluable#evalForall(java.lang.Object, int, boolean)
	 */
	public boolean evalForall(Object o, int tick) {
		// TODO Auto-generated method stub
		return false;
	}

	public String getName() {
		return "Unnamed";
	}

}
