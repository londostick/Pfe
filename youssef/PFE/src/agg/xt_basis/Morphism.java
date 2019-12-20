/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
//#################################################################################

package agg.xt_basis;

import java.util.Enumeration;

import agg.util.Disposable;

/**
 * Minimal interface for (read only) operation on a graph morphism. This
 * interface does NOT provide method declarations for the construction of a
 * morphism, i.e. adding of object mappings, nor does it provide the
 * capabilities of an observable.
 */
public interface Morphism extends Disposable {

	/** Set my name. */
	public abstract void setName(String n);

	/** Return my name. */
	public abstract String getName();

	/** Return my source graph. */
	public abstract Graph getOriginal();

	/** Return my target graph. */
	public abstract Graph getImage();

	/**
	 * Return an Enumeration of the graphobjects out of my source graph which
	 * are actually taking part in one of my mappings. Enumeration elements are
	 * of type <code>GraphObject</code>.
	 * 
	 * @see agg.xt_basis.GraphObject
	 */
	public abstract Enumeration<GraphObject> getDomain();

	/**
	 * Return an Enumeration of the graphobjects out of my target graph which
	 * are actually taking part in one of my mappings. Enumeration elements are
	 * of type <code>GraphObject</code>.
	 * 
	 * @see agg.xt_basis.GraphObject
	 */
	public abstract Enumeration<GraphObject> getCodomain();

	/**
	 * Return the image of the specified object.
	 * 
	 * @return <code>null</code> if the object is not in domain.
	 */
	public abstract GraphObject getImage(GraphObject o);

	/**
	 * Return an Enumeration of the inverse images of the specified object.
	 * Enumeration will be empty when the object is not in codomain. Enumeration
	 * elements are of type <code>GraphObject</code>.
	 * 
	 * @see agg.xt_basis.GraphObject
	 */
	public abstract Enumeration<GraphObject> getInverseImage(GraphObject o);

	/** Return <code>true</code> iff I am a total morphism. */
	public abstract boolean isTotal();

}// ##########################################################################

