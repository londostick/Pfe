/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.util.csp;


/**
 * UnaryFunction is the interface that must be implemented by all unary function objects.
 * Every UnaryFunction object must define a single method called execute() that takes
 * a single object as its argument and returns an object. UnaryFunction objects are often
 * built to operate on a specific kind of object and must therefore cast the input parameter
 * in order to process it.
  */

public interface UnaryFunction {
  /**
   * Return the result of executing with a single Object.
   * @param object The object to process.
   * @return The result of processing the input Object.
   */
  Object execute( Object object );


  }
