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

import java.util.Enumeration;


/**
 * Container is the interface that is implemented by all of the
 * Generic Container Library containers.
 */

public interface Container extends Cloneable {
  /**
   * Return a shallow copy of myself.
   */
  public Object clone();

  /**
   * Return a string that describes me.
   */
  public String toString();

  /**
   * Return true if I'm equal to a specified object.
   * @param object The object to compare myself against.
   * @return true if I'm equal to the specified object.
   */
  public boolean equals( Object object );

  /**
   * Return the number of objects that I contain.
   */
  public int size();

  /**
   * Return the maximum number of objects that I can contain.
   */
  public int maxSize();

  /**
   * Return true if I contain no objects.
   */
  public boolean isEmpty();

  /**
   * Remove all of my objects.
   */
  public void clear();

  /**
   * Return an Enumeration of the components in this container
   */
  @SuppressWarnings("rawtypes")
public Enumeration elements();

  /**
   * Return an iterator positioned at my first item.
   */
  public ForwardIterator begin();

  /**
   * Return an iterator positioned immediately after my last item.
   */
  public ForwardIterator end();

  /**
   * Add an object to myself. If appropriate, return the object that 
   * displaced it, otherwise return null.
   */
  public Object add( Object object );


}
