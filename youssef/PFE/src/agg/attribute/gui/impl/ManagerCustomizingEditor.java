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
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
//import javax.swing.table.TableModel;

import agg.attribute.AttrManager;
import agg.attribute.gui.AttrEditor;
import agg.attribute.gui.AttrEditorManager;
import agg.attribute.handler.AttrHandler;

/**
 * Customizing of an attribute manager. To be completed.
 * 
 * @author $Author: olga $
 * @version $Id: ManagerCustomizingEditor.java,v 1.1 2005/08/25 11:56:58 enrico
 *          Exp $
 */
public class ManagerCustomizingEditor extends AbstractEditor implements
		AttrEditor, ScrollPaneConstants {

	public ManagerCustomizingEditor(AttrManager m, AttrEditorManager em) {
		super(m, em);
	}

	protected JTable handlerList;

	protected JPanel handlerListP;

	protected Box messageBox;

	protected String handlerData[][];

	protected String headers[] = null;

	@SuppressWarnings("serial")
	protected void genericCreateAllViews() {

		// List of handlers
		if (this.headers == null) {
			String h[] = { "Name", "Class", "Editor Class" };
			this.headers = h;
		}
		AttrHandler handlers[] = getAttrManager().getHandlers();
		this.handlerData = new String[handlers.length][];
		for (int i = 0; i < handlers.length; i++) {
			AttrHandler h = handlers[i];
			Object handlerEditor = getHandlerEditorManager()
					.getCustomizingEditor(h);
			String handlerEditorName = "(None)";
			if (handlerEditor != null) {
				handlerEditorName = handlerEditor.getClass().getName();
			}
			String row[] = { h.getName(), h.getClass().getName(),
					handlerEditorName };
			this.handlerData[i] = row;
		}

//		TableModel model = 
		new AbstractTableModel() {
			public Object getValueAt(int row, int column) {
				return ManagerCustomizingEditor.this.handlerData[row][column];
			}

			public int getRowCount() {
				return ManagerCustomizingEditor.this.handlerData.length;
			}

			public int getColumnCount() {
				return 3;
			}

			public String getColumnName(int column) {
				return ManagerCustomizingEditor.this.headers[column];
			}
		};

		this.handlerList = new JTable(this.handlerData, this.headers);
		// handlerList = new JTable( model );
		this.handlerList.getTableHeader().setReorderingAllowed(false);
		JScrollPane tabScrollPane = new JScrollPane(this.handlerList);
		tabScrollPane.setPreferredSize(new Dimension(800, 100));
		JScrollPane listScrollPane = new JScrollPane(tabScrollPane,
				VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
		listScrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED));
		listScrollPane.setPreferredSize(new Dimension(250, 100));
		this.handlerListP = new JPanel();
		this.handlerListP.setLayout(new BorderLayout());
		this.handlerListP.add(listScrollPane, "Center");
		this.handlerListP.setBorder(BorderFactory.createTitledBorder(
				new EtchedBorder(), "Registered Attribute Handlers",
				TitledBorder.LEFT, TitledBorder.TOP));

		// Under construction message

		this.messageBox = Box.createVerticalBox();
		this.messageBox.add(new JLabel("For now, just displaying."));
		this.messageBox.add(new JLabel(
				"Registration of handlers at runtime would be nice ..."));
		// messageBox.setBorder(new EtchedBorder());
	}

	protected void arrangeMainPanel() {
	}

	protected void genericCustomizeMainLayout() {
		this.mainPanel = new JPanel(new BorderLayout());
		this.mainPanel.add(this.handlerListP, BorderLayout.NORTH);
		this.mainPanel.add(this.messageBox, BorderLayout.CENTER);
	}
}

/*
 * $Log: ManagerCustomizingEditor.java,v $
 * Revision 1.4  2010/08/18 09:24:53  olga
 * tuning
 *
 * Revision 1.3  2007/11/05 09:18:19  olga
 * code tuning
 *
 * Revision 1.2  2007/09/10 13:05:30  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.1 2005/08/25 11:56:58
 * enrico *** empty log message ***
 * 
 * Revision 1.2 2005/06/20 13:37:03 olga Up to now the version 1.2.8 will be
 * prepared.
 * 
 * Revision 1.1 2005/05/30 12:58:04 olga Version with Eclipse
 * 
 * Revision 1.3 2003/03/05 18:24:11 komm sorted/optimized import statements
 * 
 * Revision 1.2 2002/09/23 12:23:50 komm added type graph in xt_basis, editor
 * and GUI
 * 
 * Revision 1.1.1.1 2002/07/11 12:16:57 olga Imported sources
 * 
 * Revision 1.6 2000/04/05 12:07:55 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 * Revision 1.5 1999/12/22 12:36:36 shultzke The user cannot edit the context of
 * graphs. Only in rules it is possible.
 */
