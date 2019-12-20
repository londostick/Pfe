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
package agg.gui.editor;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * @author olga
 *
 */
public class GraphMorphismEditorMouseMotionAdapter implements MouseMotionListener {

	private final GraphMorphismEditor editor;
	
	public GraphMorphismEditorMouseMotionAdapter(final GraphMorphismEditor editor) {
		this.editor = editor;
		this.editor.addMouseMotionListener(this);
	}
	
	public void mouseMoved(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
	}

}
