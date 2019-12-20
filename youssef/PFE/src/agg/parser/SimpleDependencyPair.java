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
 * This class contains the algorithm to decide if one rule dependes of another
 * rule.
 * 
 * @author $Author: olga $
 */
public class SimpleDependencyPair extends DependencyPair {

	protected SimpleDependencyPair() {
		super();
		this.complete = false;
	}

}
