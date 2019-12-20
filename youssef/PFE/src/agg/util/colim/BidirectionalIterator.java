/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.util.colim;

/**
 * BidirectionalIterator is the interface of all iterators that can
 * read and/or write one item at a time in a forwards or backwards direction.
 */

public interface BidirectionalIterator extends ForwardIterator {
	
  public Object clone();

  /**
   * Retreat by one.
   */
  public void retreat();

  /**
   * Retreat by a specified amount.
   * @param n The amount to retreat.
   */
  public void retreat( int n );
}
