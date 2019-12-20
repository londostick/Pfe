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

import java.util.EventListener;
import java.util.EventObject;

/**
 * Listens for events from several options.
 */
public interface OptionEventListener extends EventListener {

	public void optionEventOccurred(EventObject e);

}
