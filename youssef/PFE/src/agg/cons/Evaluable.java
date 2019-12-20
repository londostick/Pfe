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

public interface Evaluable {
	public boolean eval(java.lang.Object o);

	public boolean eval(java.lang.Object o, int tick);

	public boolean eval(java.lang.Object o, boolean negation);

	public boolean eval(java.lang.Object o, int tick, boolean negation);
	
	public boolean evalForall(java.lang.Object o, int tick);
	
}
