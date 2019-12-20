/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.attribute.gui.impl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import agg.attribute.AttrContext;
import agg.attribute.AttrManager;
import agg.attribute.gui.AttrContextEditor;
import agg.attribute.gui.AttrEditorManager;

/**
 * Editor for a context (rule, match etc.).
 * 
 * @author $Author: olga $
 * @version $Id: ContextEditor.java,v 1.4 2010/08/18 09:24:53 olga Exp $
 */
public class ContextEditor extends AbstractEditor implements AttrContextEditor,
		ScrollPaneConstants {

	protected AttrContext attrContext;

	protected JPanel condPanel, varPanel;

	protected JSplitPane varAndCondSplitPane;

	protected JTextArea outputTextArea;

	protected JScrollPane outputScrollPane;

	protected ConditionTupleEditor conditionEditor;

	protected VariableTupleEditor variableEditor;

	public ContextEditor(AttrManager m, AttrEditorManager em) {
		super(m, em);
	}

	/** Creates all subviews. */
	protected void genericCreateAllViews() {

		// Variables

		this.variableEditor = new VariableTupleEditor(getAttrManager(),
				getEditorManager());
		this.varPanel = new JPanel(new BorderLayout());
		this.varPanel.setBackground(new Color(205, 230, 205));
		this.varPanel.add(this.variableEditor.getComponent(), BorderLayout.CENTER);
		this.variableEditor.getComponent().setBackground(new Color(205, 230, 205));
		this.varPanel.setBorder(BorderFactory.createTitledBorder(new BevelBorder(
				BevelBorder.RAISED), "Parameters and Variables",
				TitledBorder.CENTER, TitledBorder.TOP));
		// varPanel.setPreferredSize( new Dimension( 300, 300 ));

		// Conditions

		this.conditionEditor = new ConditionTupleEditor(getAttrManager(),
				getEditorManager());
		this.condPanel = new JPanel(new BorderLayout());
		this.condPanel.setBackground(new Color(205, 230, 205));
		this.condPanel.add(this.conditionEditor.getComponent(), BorderLayout.CENTER);
		this.conditionEditor.getComponent().setBackground(new Color(205, 230, 205));
		this.condPanel.setBorder(BorderFactory.createTitledBorder(new BevelBorder(
				BevelBorder.RAISED), "Conditions", TitledBorder.CENTER,
				TitledBorder.TOP));
		// condPanel.setPreferredSize( new Dimension( 100, 300 ));

		// Context mappings information.

		createOutputTextArea();
	}

	protected void arrangeMainPanel() {
	}

	/** Must create mainPanel and set it up. */
	protected void genericCustomizeMainLayout() {
		this.varAndCondSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				this.varPanel, this.condPanel);
		// Box mappingBox = Box.createHorizontalBox();
		// mappingBox.add( outputScrollPane );

		this.mainPanel = new JPanel(new BorderLayout());
		this.mainPanel.add(this.varAndCondSplitPane, BorderLayout.CENTER);
		// mainPanel.add( mappingBox, BorderLayout.SOUTH );
		this.mainPanel.setPreferredSize(new Dimension(500, 400));
		this.mainPanel.addComponentListener(this);
		resize();
	}

	protected void createOutputTextArea() {
		this.outputTextArea = new JTextArea(5, 10);
		this.outputTextArea.setEditable(false);
		this.outputTextArea.setLineWrap(false);
		this.outputScrollPane = new JScrollPane(this.outputTextArea,
				VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
		// outputTA.setBackground( Color.gray );
		this.outputScrollPane.setPreferredSize(new Dimension(300, 50));
	}

	protected void resize() {
		this.varAndCondSplitPane.setDividerLocation(0.67);
		this.mainPanel.revalidate();
		this.mainPanel.repaint();
	}

	public void componentResized(ComponentEvent e) {
		resize();
	}

	public void componentShown(ComponentEvent e) {
		resize();
	}

	// AttrContextEditor interface

	public void setContext(AttrContext anAttrContext) {
		this.attrContext = anAttrContext;
		this.conditionEditor.setTuple(this.attrContext.getConditions());
		this.variableEditor.setTuple(this.attrContext.getVariables());		
	}

	public AttrContext getContext() {
		return this.attrContext;
	}

	public ConditionTupleEditor getConditionEditor() {
		return this.conditionEditor;
	}

	public VariableTupleEditor getVariableEditor() {
		return this.variableEditor;
	}
	
	public void resetVariableEditorComponent() {
		this.varPanel.add(this.variableEditor.getComponent(), BorderLayout.CENTER);
	}
	
	public void setAttrManager(AttrManager m) {
		super.setAttrManager(m);
		this.conditionEditor.setAttrManager(m);
		this.variableEditor.setAttrManager(m);
	}

	public void setEditorManager(AttrEditorManager m) {
		super.setEditorManager(m);
		this.conditionEditor.setEditorManager(m);
		this.variableEditor.setEditorManager(m);
	}
}
/*
 * $Log: ContextEditor.java,v $
 * Revision 1.4  2010/08/18 09:24:53  olga
 * tuning
 *
 * Revision 1.3  2008/07/14 07:35:48  olga
 * Applicability of RS - new option added, more tuning
 * Node animation - new animation parameter added,
 * Undo edit manager - possibility to disable it when graph transformation
 * because it costs much more time and memory
 *
 * Revision 1.2  2007/09/10 13:05:30  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.1 2005/08/25 11:56:58 enrico ***
 * empty log message ***
 * 
 * Revision 1.2 2005/06/20 13:37:03 olga Up to now the version 1.2.8 will be
 * prepared.
 * 
 * Revision 1.1 2005/05/30 12:58:04 olga Version with Eclipse
 * 
 * Revision 1.3 2003/03/05 18:24:11 komm sorted/optimized import statements
 * 
 * Revision 1.2 2002/09/23 12:23:49 komm added type graph in xt_basis, editor
 * and GUI
 * 
 * Revision 1.1.1.1 2002/07/11 12:16:57 olga Imported sources
 * 
 * Revision 1.9 2000/04/05 12:07:43 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 * Revision 1.8 1999/12/22 12:37:02 shultzke The user cannot edit the context of
 * graphs. Only in rules it is possible.
 * 
 * Revision 1.7 1999/12/06 16:30:05 olga *** empty log message ***
 * 
 * Revision 1.6 1999/09/13 10:00:55 shultzke ContextEditor dezent eingefaerbt
 * 
 * Revision 1.5 1999/08/17 07:32:17 shultzke GUI leicht geaendert
 */
