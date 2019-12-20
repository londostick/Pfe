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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableColumn;

import agg.attribute.AttrInstance;
import agg.attribute.AttrInstanceMember;
import agg.attribute.AttrManager;
import agg.attribute.AttrTuple;
import agg.attribute.gui.AttrEditorManager;
import agg.attribute.gui.AttrTupleEditor;
import agg.attribute.impl.AttrSession;
import agg.attribute.view.AttrViewEvent;
import agg.attribute.view.AttrViewObserver;
import agg.attribute.view.AttrViewSetting;

/**
 * Provides all necessary functionality for a lightweight editor of an attribute
 * tuple. Extending classes just need to redefine createTableModel() to set up a
 * simple editor with desired columns, headers etc. For row dragging, tool bar
 * actions etc. consider extending
 * 
 * @author $Author: olga $
 * @version $Id: BasicTupleEditor.java,v 1.5 2010/08/18 09:24:52 olga Exp $
 */
public class BasicTupleEditor extends AbstractEditor implements
		AttrTupleEditor, AttrViewObserver, TupleTableModelConstants,
		ScrollPaneConstants {

	/** Edited attribute tuple. */
	protected AttrTuple tuple;

	/** View to utilize when accessing members. */
	protected AttrViewSetting viewSetting;

	/** Table model, defines which columns to show, edit, their headers etc. */
	protected TupleTableModel tableModel;

	/** The handler selection editor for the current tuple's attribute manager. */
	protected HandlerSelectionEditor handlerSelectionEditor;

	/** Table widget. */
	protected JTable tableView;

	/** Scroll pane containing the table widget. */
	protected JScrollPane tableScrollPane;

	/** Creating the tuple editor. */
	public BasicTupleEditor(AttrManager m, AttrEditorManager em) {
		super(m, em);
		setViewSetting(m.getDefaultOpenView()); // ensure viewSetting is not
		// null;
	}

	/**
	 * This decides about the table properties: columns to display, expandable
	 * or not etc.
	 * 
	 * @see TupleTableModel
	 */
	protected TupleTableModel createTableModel() {
		int columns[] = { NAME, EXPR };
		TupleTableModel tm = new TupleTableModel(this);
		tm.setColumnArray(columns);
		tm.setExtensible(false);
		return tm;
	}

	protected void arrangeMainPanel() {
	}

	/**
	 * This is called automatically by the parent (AbstractEditor) constructor.
	 */
	protected void genericCreateAllViews() {
		createTableView();
	}

	/**
	 * Every tuple editor class has its own version of this method. Is called
	 * automatically by the parent (AbstractEditor) constructor.
	 */
	protected void genericCustomizeMainLayout() {
		this.mainPanel = new JPanel(new BorderLayout());
		this.mainPanel.add(this.tableScrollPane, BorderLayout.CENTER);
	}

	/** Default implementation of table creation. The heart of the editor. */
	protected void createTableView() {
		// Generic; override, please, to define your own table column layout.
		this.tableModel = createTableModel();

		this.tableView = new JTable(this.tableModel);
		this.tableView.setRowHeight(this.tableView.getRowHeight() + 2);
		String[] names = new String[this.tableModel.getColumnCount()];
		for (int i = 0; i < this.tableModel.getColumnCount(); i++) {
			names[i] = this.tableModel.getColumnName(i);
		}
		Enumeration<TableColumn> columns = this.tableView.getColumnModel().getColumns();
		for (; columns.hasMoreElements();) {
			TableColumn tc = columns.nextElement();
			String name = this.tableModel.getColumnName(tc.getModelIndex());
			if (name == "OK" || name == "In" || name == "Out") {
				tc.setMinWidth(30);
				tc.setMaxWidth(70);
			} else if (name == "Shown") {
				tc.setMinWidth(30);
				tc.setMaxWidth(60);
				tc.setPreferredWidth(60);
			} else if (name == "Handler") {
				tc.setMinWidth(50);
				tc.setMaxWidth(100);
				tc.setPreferredWidth(80);
			} else if (name == "Type" || name == "Name") {
				tc.setMinWidth(40);
				tc.setPreferredWidth(70);
			} else if (name == "Expression") {
				tc.setMinWidth(60);
				tc.setPreferredWidth(300);
			} else if (name == "Yields") {
				tc.setMinWidth(80);
				tc.setPreferredWidth(100);
			} else {
				tc.setPreferredWidth(100);
			}
		}
		new MemberEditorDispatcher(this);
		// Decorating.
		this.tableScrollPane = new JScrollPane(this.tableView);
		this.tableScrollPane.setMinimumSize(new Dimension(100, 50));
	}

	/** Start observing an attribute tuple relative to a view. */
	protected void registerAsObserver() {
		if (this.tuple != null) {
			this.viewSetting.addObserver(this, this.tuple);
		}
	}

	/** Stop observing an attribute tuple relative to a view. */
	protected void deregisterAsObserver() {
		if (this.tuple != null) {
			this.viewSetting.removeObserver(this, this.tuple);
		}
	}

	/**
	 * If the edited tuple is an AttrInstance, its currently selected member is
	 * returned.
	 */
	public AttrInstanceMember getSelectedMember() {
		if (this.tuple == null || !(this.tuple instanceof AttrInstance)) {
			return null;
		}
		int selectedRow = this.tableView.getSelectedRow();
		if (selectedRow >= this.tuple.getNumberOfEntries(this.viewSetting)) {
			return null;
		}
		AttrInstanceMember member = (AttrInstanceMember) this.tableModel.getMember(
				this.tuple, selectedRow);
		return member;
	}

	//
	// Public methods.
	//

	/** Called by MemberEditorDispatcher. */
	public JTable getTableView() {
		return this.tableView;
	}

	/** Called by MemberEditorDispatcher. */
	public TupleTableModel getTableModel() {
		return this.tableModel;
	}

	/** Called by MemberEditorDispatcher. */
	public HandlerSelectionEditor getHandlerSelectionEditor() {
		return this.handlerSelectionEditor;
	}

	// Implementation of the AttrTupleEditor interface

	/** Setting the tuple to display and edit. */
	public void setTuple(AttrTuple anAttrTuple) {
		deregisterAsObserver();
		this.tuple = anAttrTuple;
		registerAsObserver();
		this.handlerSelectionEditor = HandlerSelectionEditor
				.getHandlerSelectionEditor(getAttrManager());
		attributeChanged(null);
	} // setTuple()

	/** Returns the tuple to display and edit. */
	public AttrTuple getTuple() {
		return this.tuple;
	} // getTuple()

	public void setViewSetting(AttrViewSetting anAttrViewSetting) {
		if (anAttrViewSetting == null) {
			AttrSession.warn(this, "Tried to set a null view setting!", true);
			return;
		}
		deregisterAsObserver();
		this.viewSetting = anAttrViewSetting;
		// System.out.println("BasicTupleEditor: setze den View "+this.tuple+"
		// "+this.viewSetting);
		registerAsObserver();
		attributeChanged(null);
	} // setViewSetting()

	public AttrViewSetting getViewSetting() {
		return this.viewSetting;
	} // getViewSetting()

	// AttrViewObserver interface

	/** React to attribute changes. */
	public void attributeChanged(AttrViewEvent event) {
//		if (event == null) {
//			 System.out.println("BasicTupleEditor.attributeChanged "+this+"   "+event);			
//		}
		if (this.tableModel != null && event != null) {
			this.tableModel.attributeChanged(event);
			firePropertyChange();
		}
	}

	/** Implemented: no, don't save me. */
	public boolean isPersistentFor(AttrTuple at) {
		return false;
	}

	// PropertyEditor

	/** Same as #getComponent(). */
	public Component getCustomEditor() {
		return getComponent();
	}

	/** Same as #setTuple( Object ). */
	public void setValue(Object val) {
		setTuple((AttrTuple) val);
	}

	public void paintValue(Graphics gfx, Rectangle box) {
		getComponent().paintAll(gfx);
	}

}
/*
 * $Log: BasicTupleEditor.java,v $
 * Revision 1.5  2010/08/18 09:24:52  olga
 * tuning
 *
 * Revision 1.4  2007/11/01 09:58:17  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.3  2007/09/10 13:05:30  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.2 2005/11/07 09:38:07 olga Null
 * pointer during retype attr. member fixed.
 * 
 * Revision 1.1 2005/08/25 11:56:58 enrico *** empty log message ***
 * 
 * Revision 1.2 2005/06/20 13:37:03 olga Up to now the version 1.2.8 will be
 * prepared.
 * 
 * Revision 1.1 2005/05/30 12:58:04 olga Version with Eclipse
 * 
 * Revision 1.4 2003/12/18 16:25:33 olga .
 * 
 * Revision 1.3 2003/03/05 18:24:10 komm sorted/optimized import statements
 * 
 * Revision 1.2 2002/09/23 12:23:49 komm added type graph in xt_basis, editor
 * and GUI
 * 
 * Revision 1.1.1.1 2002/07/11 12:16:57 olga Imported sources
 * 
 * Revision 1.9 2000/04/05 12:07:41 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 * Revision 1.8 1999/12/22 12:37:06 shultzke The user cannot edit the context of
 * graphs. Only in rules it is possible.
 * 
 * Revision 1.7 1999/10/07 11:52:53 olga *** empty log message ***
 * 
 * Revision 1.6 1999/08/17 07:32:12 shultzke GUI leicht geaendert
 */
