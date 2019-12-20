/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.parser;

import agg.xt_basis.Rule;

class ComputingThread extends Thread {
	Rule r1;

	Rule r2;

	ExcludePairContainer container;

	ComputingThread(ExcludePairContainer container, Rule r1, Rule r2) {
		this.r1 = r1;
		this.r2 = r2;
		this.container = container;
		this.start();
	}

	public void run() {
		this.container.computeCritical(this.r1, this.r2);
	}

}
