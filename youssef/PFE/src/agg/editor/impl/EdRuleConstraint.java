/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.editor.impl;

import agg.cons.EvalSet;

public class EdRuleConstraint {

	private EvalSet evalset;

	private EdRule rule;

	private String name;

	public EdRuleConstraint(String n, EdRule rule, EvalSet constraint) {
		this.name = n;
		this.rule = rule;
		this.evalset = constraint;
	}

	public void dispose() {
		this.rule = null;
		this.evalset = null;
//		System.out.println("EdRuleConstraint.dispose()  DONE  "+this.hashCode());
	}
	
	public void finalize() {
//		System.out.println("EdRuleConstraint.finalize()  called  "+this.hashCode());
	}
	
	public String getName() {
		return this.name;
	}

	public EdRule getRule() {
		return this.rule;
	}

	public EvalSet getConstraint() {
		return this.evalset;
	}

}
