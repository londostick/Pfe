/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.util;

import java.util.Comparator;

public class IntComparator<T> implements Comparator<T> {

	public int compare(T o1, T o2) {
		Integer int1 = (Integer) o1;
		Integer int2 = (Integer) o2;
		if (int1.intValue() == int2.intValue())
			return 0;
		else if (int1.intValue() < int2.intValue())
			return -1;
		else
			return 1;
	}

}
