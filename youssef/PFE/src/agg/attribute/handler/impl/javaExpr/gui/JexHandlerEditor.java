/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.attribute.handler.impl.javaExpr.gui;

//import agg.attribute.gui.TextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import agg.attribute.handler.AttrHandler;
import agg.attribute.handler.gui.HandlerCustomizingEditor;
import agg.attribute.handler.gui.impl.AbstractHandlerEditor;
import agg.attribute.handler.impl.javaExpr.JexHandler;
import agg.attribute.parser.javaExpr.ClassResolver;
import agg.attribute.util.RowDragEvent;
import agg.attribute.util.RowDragListener;
import agg.attribute.util.TableRowDragger;

/**
 * Old awt-widgets, should be translated to swing.
 * 
 * @version $Id: JexHandlerEditor.java,v 1.4 2010/09/23 08:13:48 olga Exp $
 * @author $Author: olga $
 */
public class JexHandlerEditor extends AbstractHandlerEditor implements
		HandlerCustomizingEditor, ScrollPaneConstants, ListSelectionListener,
		RowDragListener {

	protected JexHandler handler;

	protected ClassResolver classResolver;

	protected Vector<String> packages;

	protected PackageTableModel tableModel;

	protected JPanel mainPanel;

	protected JPanel packageP, buttonP, entriesP, inputP;

	protected JTable entriesL;

	protected JTextField inputTF;

	protected Action insertAction, appendAction, deleteAction;

	@SuppressWarnings("serial")
	public JexHandlerEditor(AttrHandler h) {
		super();
		this.handler = (JexHandler) h;
		this.classResolver = this.handler.getClassResolver();

		// Package list

		this.tableModel = new PackageTableModel();
		this.entriesL = new JTable(this.tableModel);
		this.entriesL.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.entriesL.setRowSelectionAllowed(true);
		this.entriesL.setColumnSelectionAllowed(false);
		this.entriesL.getSelectionModel().addListSelectionListener(this);
		// this.entriesL.setMaximumSize(new Dimension( 100, 5 ));
		this.entriesL.setPreferredScrollableViewportSize(new Dimension(200, 100));

		TableRowDragger rowDragger = new TableRowDragger(this.entriesL);
		rowDragger.addRowDragListener(this);

		// Package list decoration

		JScrollPane listScrollPane = new JScrollPane(this.entriesL);
		/*
		 * JScrollPane listScrollPane = new JScrollPane( listScrollPane1,
		 * VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED );
		 * listScrollPane.setBorder( new BevelBorder( BevelBorder.LOWERED ));
		 */
		listScrollPane.setPreferredSize(new Dimension(200, 100));

		this.entriesP = new JPanel();
		this.entriesP.setLayout(new BorderLayout());
		this.entriesP.add(listScrollPane, "Center");
		this.entriesP.setBorder(BorderFactory.createTitledBorder(new EtchedBorder(),
				"Searched Packages", TitledBorder.CENTER, TitledBorder.TOP));

		this.entriesP.setPreferredSize(new Dimension(200, 100));
		// Tool bar

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		this.insertAction = new AbstractAction("Insert") {
			public void actionPerformed(ActionEvent ev) {
				int sel = JexHandlerEditor.this.entriesL.getSelectedRow();
				if (sel == -1)
					sel = 0;
				JexHandlerEditor.this.packages.insertElementAt("", sel);
				JexHandlerEditor.this.tableModel.update();
				JexHandlerEditor.this.entriesL.editCellAt(sel, 0);
			}
		};
		toolBar.add(this.insertAction);
		toolBar.addSeparator();

		this.appendAction = new AbstractAction("Append") {
			public void actionPerformed(ActionEvent ev) {
				int sel = JexHandlerEditor.this.entriesL.getSelectedRow();
				if (sel == -1)
					sel = JexHandlerEditor.this.packages.size() - 1;
				appendPackageAt("", sel);
				JexHandlerEditor.this.tableModel.update();
				JexHandlerEditor.this.entriesL.editCellAt(sel + 1, 0);
			}
		};
		toolBar.add(this.appendAction);
		toolBar.addSeparator();

		this.deleteAction = new AbstractAction("Remove") {
			public void actionPerformed(ActionEvent ev) {
				int sel = JexHandlerEditor.this.entriesL.getSelectedRow();
				if (sel != -1) {
					JexHandlerEditor.this.packages.removeElementAt(sel);
					JexHandlerEditor.this.tableModel.update();
					updateResolver();
				}
			}
		};
		toolBar.add(this.deleteAction);

		// Tool bar decoration
		JPanel toolPanel = new JPanel();
		toolPanel.add(toolBar);
		// toolPanel.setPreferredSize( new Dimension( 300, 20 ));
		// buttonP.setBorder( new BevelBorder( BevelBorder.RAISED ));

		this.packageP = new JPanel();
		this.packageP.setLayout(new BorderLayout());
		this.packageP.add(this.entriesP, "Center");
		this.packageP.add(toolPanel, "South");
		this.packageP.setPreferredSize(new Dimension(200, 200));

		this.mainPanel = this.packageP; // new JPanel( new BorderLayout());
		// this.mainPanel.add( this.packageP, "Center" );
		this.mainPanel.setPreferredSize(new Dimension(200, 200));
		// this.mainPanel.setBorder( new BevelBorder( BevelBorder.LOWERED ));

		updateList();
	}

	protected void appendPackageAt(String p, int pos) {
		if (pos == this.packages.size() - 1) {
			this.packages.addElement(p);
		} else {
			this.packages.insertElementAt(p, pos + 1);
		}
	}

	//
	// Implementing the RowDragListener interface
	//

	/** Just acknowledge the fact. */
	public void draggingStarted(RowDragEvent ev) {
//		int src = 
		ev.getSourceRow();
		// System.out.println("Started dragging row "+src);
	}

	/** Updates the class resolver. */
	public void draggingStopped(RowDragEvent ev) {
//		int src = 
		ev.getSourceRow();
		// System.out.println("Stopped dragging row "+src);
		updateResolver();
	}

	/** Moving a package name within the local list. */
	public void draggingMoved(RowDragEvent ev) {
		/*
		 * System.out.println ("draggingMoved("+ev.getSourceRow()+", "+
		 * ev.getTargetRow()+")");
		 */
		int src = ev.getSourceRow();
		int dest = ev.getTargetRow();
		if (dest == -1 || src == -1)
			return;
		if (dest == src)
			return;
		// System.out.println("Source row="+src+"; dest. row="+dest);
		String p = this.packages.elementAt(src);

		if (src < dest) {
			appendPackageAt(p, dest);
			this.packages.removeElementAt(src);
		} else {
			this.packages.insertElementAt(p, dest);
			this.packages.removeElementAt(src + 1);
		}
		this.tableModel.update();
	}

	/** ListSelectionListener interface implementation. */
	public void valueChanged(ListSelectionEvent ev) {
		int row = ev.getFirstIndex();
		this.deleteAction.setEnabled(row != -1);
	}

	public Component getComponent() {
		return this.mainPanel;
	}

	public AttrHandler getAttrHandler() {
		return this.handler;
	}

	public void setAttrHandler(AttrHandler h) {
		this.handler = (JexHandler) h;
		this.classResolver = this.handler.getClassResolver();
		this.packages = this.classResolver.getPackages();
		updateList();
	}

	protected void updateList() {
		this.packages = this.classResolver.getPackages();
		this.tableModel.update();
	}

	protected void updateResolver() {
		this.classResolver.setPackages(this.packages);
	}

	@SuppressWarnings("serial")
	class PackageTableModel extends AbstractTableModel {
		public int getRowCount() {
			if (JexHandlerEditor.this.packages == null)
				updateList();
			return JexHandlerEditor.this.packages.size();
		}

		public int getColumnCount() {
			return 1;
		}

		public Class<?> getColumnClass(int column) {
			return "".getClass();
		}

		public String getColumnName(int column) {
			return null; // "Package Name";
		}

		public boolean isCellEditable(int row, int column) {
			return true;
		}

		public Object getValueAt(int row, int column) {
			return JexHandlerEditor.this.packages.elementAt(row);
		}

		public void setValueAt(Object value, int row, int column) {
			String text = (String) value;
			if (text == null || text.trim().length() == 0) {
				JexHandlerEditor.this.packages.removeElementAt(row);
				update();
			} else {
				JexHandlerEditor.this.packages.setElementAt(text, row);
			}
			updateResolver();
		}

		public void update() {
			fireTableDataChanged();
		}
	}
}

/*
 * $Log: JexHandlerEditor.java,v $
 * Revision 1.4  2010/09/23 08:13:48  olga
 * tuning
 *
 * Revision 1.3  2007/11/05 09:18:22  olga
 * code tuning
 *
 * Revision 1.2  2007/09/10 13:05:53  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.1 2005/08/25 11:57:00 enrico ***
 * empty log message ***
 * 
 * Revision 1.2 2005/06/20 13:37:04 olga Up to now the version 1.2.8 will be
 * prepared.
 * 
 * Revision 1.1 2005/05/30 12:58:04 olga Version with Eclipse
 * 
 * Revision 1.3 2003/03/05 18:24:28 komm sorted/optimized import statements
 * 
 * Revision 1.2 2002/09/23 12:23:54 komm added type graph in xt_basis, editor
 * and GUI
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:01 olga Imported sources
 * 
 * Revision 1.8 2000/06/05 14:07:25 shultzke Debugausgaben fuer V1.0.0b
 * geloescht
 * 
 * Revision 1.7 2000/04/05 12:08:48 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 * Revision 1.6 2000/03/03 11:40:05 shultzke *** empty log message ***
 */
