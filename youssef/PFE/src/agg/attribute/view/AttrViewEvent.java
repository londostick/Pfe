/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
// $Id: AttrViewEvent.java,v 1.3 2007/09/10 13:05:51 olga Exp $
package agg.attribute.view;

import agg.attribute.AttrEvent;

/**
 * Attribute event interface for delivering information about attribute changes
 * to clients.
 * 
 * @see agg.attribute.AttrTuple
 */
public interface AttrViewEvent extends AttrEvent {

	/** A member was moved. */
	public static final int MEMBER_MOVED = 210;

	/** A member was shown or hidden . */
	public static final int MEMBER_VISIBILITY = 220;

	//
	// Public Methods
	//

	/** Getting the view setting. */
	public AttrViewSetting getView();
}
