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
package agg.editor.impl;

import java.util.Hashtable;

import javax.swing.undo.StateEdit;
import javax.swing.undo.StateEditable;

/**
 * @author olga
 *
 */
@SuppressWarnings("serial")
public class EditStateEdit extends StateEdit {

	
	public EditStateEdit(StateEditable anObject) {
        super(anObject);
	}
	
	public EditStateEdit(StateEditable anObject, String name) {
		super(anObject, name);
	}
	
	public StateEditable getObject() {
		return super.object;
	}
	
	public Hashtable<Object,Object> getPreState() {
		return this.preState;
	}
}
