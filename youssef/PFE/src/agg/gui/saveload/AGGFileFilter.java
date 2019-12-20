/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.saveload;


/**
 * This is a filter for file names. If this filter is set in a load or save
 * dialog only files with a special extension are displayed. This filter only
 * shows files with <CODE>.cpx</CODE> extension.
 * 
 * @version $ID
 * @author $Author: olga $
 */
public class AGGFileFilter extends ExtensionFileFilter {

	/**
	 * Creates a new filter.
	 */
	public AGGFileFilter() {
		super();
	}

	/**
	 * Creates a new filter.
	 */
	public AGGFileFilter(String extension, String description) {
		super(extension, description);
	}
}
