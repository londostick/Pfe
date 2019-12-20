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
 * This class contains the algorithm to decide if one rule exludes another rule.
 * 
 * @author $Author: olga $
 * @version $Id: SimpleExcludePair.java,v 1.9 2010/09/20 14:30:10 olga Exp $
 */
public class SimpleExcludePair extends ExcludePair {

	public SimpleExcludePair() {
		super();
		this.complete = false;
	}
}
