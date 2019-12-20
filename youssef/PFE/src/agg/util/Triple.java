/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
/**
 * 
 */
package agg.util;

/**
 * @author olga
 * 
 */
public class Triple<E, F, D> {
	public E first;

	public F second;
	
	public D third;
	
	public Triple(E firstE, F secondF, D thirdD) {
		this.first = firstE;
		this.second = secondF;
		this.third = thirdD;		
	}

	public boolean equals(final Triple<E, F, D> t) {
		if (this.first.equals(t.first)
				&& this.second.equals(t.second)
				&& this.third.equals(t.third))
			return true;
		
		return false;
	}
}
