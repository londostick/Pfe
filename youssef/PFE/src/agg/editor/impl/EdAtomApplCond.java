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

import agg.cons.AtomApplCond;

public class EdAtomApplCond {

	private AtomApplCond atomApplCond;

	private EdRule rule;

	private String name;

	public EdAtomApplCond(String n, EdRule rule, AtomApplCond atomCond) {
		this.name = n;
		this.rule = rule;
		this.atomApplCond = atomCond;
	}

	public void dispose() {
		this.rule = null;
		this.atomApplCond = null;
//		System.out.println("EdAtomApplCond.dispose()  DONE  "+this.hashCode());
	}
	
	public void finalize() {
//		System.out.println("EdAtomApplCond.finalize()  called  "+this.hashCode());
	}
	
	public String getName() {
		return this.name;
	}

	public EdRule getRule() {
		return this.rule;
	}

	public AtomApplCond getAtomApplCond() {
		return this.atomApplCond;
	}

}
