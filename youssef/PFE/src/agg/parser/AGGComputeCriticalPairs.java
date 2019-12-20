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

/**
 * This class computes conflicts and dependencies of rule pairs.
 * 
 * @author $Author: olga $
 * @version $ID
 */
public class AGGComputeCriticalPairs {
	public static void main(String[] args) {
		agg.parser.ComputeCriticalPairs ccp = new agg.parser.ComputeCriticalPairs();
		ccp.run(args);
	}
}
