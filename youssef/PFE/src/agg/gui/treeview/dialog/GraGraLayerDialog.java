/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.treeview.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

//import agg.editor.impl.EdGraGra;
import agg.util.IntComparator;
import agg.util.OrderedSet;
import agg.xt_basis.Rule;
import agg.xt_basis.RuleLayer;


/**
 * This class provides a window for a user dialog. This dialog is necessary to
 * enter the grammar layers.
 * 
 * @author $Author: olga $
 * @version $Id: GraGraLayerDialog.java,v 1.3 2010/09/23 08:23:05 olga Exp $
 */
@SuppressWarnings("serial")
public class GraGraLayerDialog extends JDialog implements ActionListener {

	private JPanel contentPane;

	private JPanel rulePanel;

	private JPanel buttonPanel;

	private JScrollPane ruleScrollPane;

	private JTable ruleTable;

	private JButton closeButton;

	private JButton cancelButton;

	private boolean isCancelled;

	private RuleLayer layer;

//	private EdGraGra gragra;

	boolean changed = false;

	/** This class models a hashtable for a table. */
	public class HashTableModel extends DefaultTableModel {

		Hashtable<Rule, Integer> table;

		RuleLayer ruleLayer;

		/**
		 * Creates a new model with hashtable and the title for the column of
		 * the table.
		 * 
		 * @param table
		 *            The hashtable for the modle.
		 * @param columnNames
		 *            The array with the column names.
		 */
		public HashTableModel(Hashtable<Rule, Integer> table,
				String[] columnNames) {
			super();
			for (int i = 0; i < columnNames.length; i++) {
				addColumn(columnNames[i]);
			}
			this.table = table;
			Enumeration<?> keys = this.table.keys();
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				Object value = this.table.get(key);
				Vector<Object> tmpVector = new Vector<Object>();
				tmpVector.addElement(key);
				tmpVector.addElement(value);
				addRow(tmpVector);
			}
		}

		/**
		 * Creates a new model with hashtable and the title for the column of
		 * the table.
		 * 
		 * @param layer
		 *            The rule layer with hashtable for the model.
		 * @param columnNames
		 *            The array with the column names.
		 */
		public HashTableModel(RuleLayer layer, String[] columnNames) {
			super();
			for (int i = 0; i < columnNames.length; i++) {
				addColumn(columnNames[i]);
			}
			this.table = layer.getRuleLayer();
			this.ruleLayer = layer;
			Integer startLayer = layer.getStartLayer();
			Hashtable<Integer, HashSet<Rule>> invertedRuleLayer = layer.invertLayer();
			
			OrderedSet<Integer> ruleLayerSet = new OrderedSet<Integer>(new IntComparator<Integer>());
			for (Enumeration<Integer> en = invertedRuleLayer.keys(); en
					.hasMoreElements();) {
				ruleLayerSet.add(en.nextElement());
			}
			int i = 0;

			Integer currentLayer = startLayer;
			boolean nextLayerExists = true;
			while (nextLayerExists && (currentLayer != null)) {
				HashSet<Rule> rulesForLayer = invertedRuleLayer.get(currentLayer);
				Iterator<Rule> en = rulesForLayer.iterator();
				while (en.hasNext()) {
					Rule rule = en.next();
					Vector<Object> tmpVector = new Vector<Object>();
					tmpVector.addElement(rule);
					tmpVector.addElement(Integer.valueOf(rule.getLayer()));
					addRow(tmpVector);
				}
				// set next Layer				
				i++;
				if (i < ruleLayerSet.size()) {
					currentLayer = ruleLayerSet.get(i);
				}
				else {
					nextLayerExists = false;
				}
			}
		}

		/**
		 * This method decides if a cell of a table is editable or not.
		 * 
		 * @param rowIndex
		 *            The index of the row of the cell.
		 * @param columnIndex
		 *            The index of the column of the cell.
		 * @return The layer function can only entered in the second column. So
		 *         for any other column <CODE>false</CODE> is returned.
		 */
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 1;
		}

		/**
		 * Returns the value of a cell.
		 * 
		 * @param row
		 *            The index of the row of the cell.
		 * @param column
		 *            The index of the column of the cell.
		 * @return The object of the underlaying model of this table.
		 */
		public Object getValueAt(int row, int column) {
			Object result = super.getValueAt(row, column);
			if (result instanceof Rule) {
				result = ((Rule) result).getName();
			}
			return result;
		}

		/**
		 * Sets a new value to a cell.
		 * 
		 * @param aValue
		 *            The new value of a cell.
		 * @param row
		 *            The index of the row of the cell.
		 * @param column
		 *            The index of the column of the cell.
		 */
		public void setValueAt(Object aValue, int row, int column) {
			Object key = super.getValueAt(row, 0);
			try {
				Integer i = Integer.valueOf((String) aValue);
				super.setValueAt(i, row, column);
				if (key instanceof Rule)
					this.table.put((Rule) key, i);
			} catch (NumberFormatException nfe) {
			}
		}

		public Hashtable<Rule, Integer> getTable() {
			return this.table;
		}

	}

	/**
	 * Creates new form GraGraLayerGUI
	 * 
	 * @param parent
	 *            The parent frame of this gui.
	 * @param layer
	 *            The layer function must be changed.
	 */
	public GraGraLayerDialog(JFrame parent, RuleLayer layer) {
		super(parent, true);
		// System.out.println("GraGraLayerGUI parent: "+parent);
		setTitle("Rule Layer");
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				exitForm(evt);
			}
		});
		this.layer = layer;
		if (parent != null)
			setLocationRelativeTo(parent);
		else
			setLocation(300, 100);
		initComponents();
	}

	/**
	 * This method is called from within the constructor to initialize the
	 * dialog.
	 */
	private void initComponents() {
		this.contentPane = new JPanel(new BorderLayout());
		this.contentPane.setBackground(Color.lightGray);

		this.rulePanel = new JPanel(new BorderLayout());
		this.rulePanel.setBackground(Color.orange);
		this.rulePanel.setBorder(new TitledBorder("Set Rule Layer"));
		// ruleTable = new JTable(new HashTableModel(layer.getRuleLayer(),new
		// String [] {"Rule Name", "Layer Number"}));
		this.ruleTable = new JTable(new HashTableModel(this.layer, new String[] { "Rule",
				"Layer" }));
		TableColumn layerColumn = this.ruleTable.getColumn("Layer");
		layerColumn.setMaxWidth(50);
		int hght = getHeight(this.ruleTable.getRowCount(), this.ruleTable.getRowHeight()) + 10;
		// System.out.println("this.ruleTable Height: "+hght);
		this.ruleTable.doLayout();
		this.ruleScrollPane = new JScrollPane(this.ruleTable);
		this.ruleScrollPane.setPreferredSize(new Dimension(200, hght));
		this.rulePanel.add(this.ruleScrollPane);

		this.buttonPanel = new JPanel(new GridBagLayout());
		this.closeButton = new JButton();
		this.closeButton.setActionCommand("close");
		this.closeButton.setText("Close");
		this.closeButton.setToolTipText("Accept entries and close dialog.");
		this.closeButton.addActionListener(this);

		this.cancelButton = new JButton();
		this.isCancelled = false;
		this.cancelButton.setActionCommand("cancel");
		this.cancelButton.setText("Cancel");
		this.cancelButton.setToolTipText("Reject entries and close dialog.");
		this.cancelButton.addActionListener(this);

		constrainBuild(this.buttonPanel, this.closeButton, 0, 0, 1, 1,
				GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1.0, 0.0,
				5, 10, 10, 5);
		constrainBuild(this.buttonPanel, this.cancelButton, 1, 0, 1, 1,
				GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1.0, 0.0,
				5, 5, 10, 10);
		this.contentPane.add(this.rulePanel, BorderLayout.CENTER);
		this.contentPane.add(this.buttonPanel, BorderLayout.SOUTH);
		this.contentPane.revalidate();

		setContentPane(this.contentPane);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		validate();
		pack();
	}

	/** Exit the Application */
	void exitForm(WindowEvent evt) {
		setVisible(false);
		dispose();
	}

	public void showGUI() {
		setVisible(true);
	}

	public boolean hasChanged() {
		return this.changed;
	}

	private void acceptValues() {
		Hashtable<Rule, Integer> table = ((HashTableModel) this.ruleTable.getModel()).getTable();
		for (Enumeration<?> e = table.keys(); e.hasMoreElements();) {
			Object key = e.nextElement();
			Integer l = table.get(key);
			if (l.intValue() != ((Rule) key).getLayer()) {
				((Rule) key).setLayer(l.intValue());
				this.changed = true;
			}
		}
	}

	/**
	 * This handels the clicks on the different buttons.
	 * 
	 * @param e
	 *            The event from the buttons.
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == this.closeButton) {
			acceptValues();
			setVisible(false);
			dispose();
		} else if (source == this.cancelButton) {
			this.isCancelled = true;
			setVisible(false);
			dispose();
		}
	}

//	public void setGraGra(EdGraGra gra) {
//		gragra = gra;
//	}

	public boolean isCancelled() {
		return this.isCancelled;
	}

	private int getHeight(int rowCount, int rowHeight) {
		int h = (rowCount + 1) * rowHeight;
		if (rowCount > 10)
			h = (10 + 2) * rowHeight;
		return h;
	}

	// constrainBuild() method
	private void constrainBuild(Container container, Component component,
			int grid_x, int grid_y, int grid_width, int grid_height, int fill,
			int anchor, double weight_x, double weight_y, int top, int left,
			int bottom, int right) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = grid_x;
		c.gridy = grid_y;
		c.gridwidth = grid_width;
		c.gridheight = grid_height;
		c.fill = fill;
		c.anchor = anchor;
		c.weightx = weight_x;
		c.weighty = weight_y;
		c.insets = new Insets(top, left, bottom, right);
		((GridBagLayout) container.getLayout()).setConstraints(component, c);
		container.add(component);
	}

}
/*
 * $Log: GraGraLayerDialog.java,v $
 * Revision 1.3  2010/09/23 08:23:05  olga
 * tuning
 *
 * Revision 1.2  2010/03/08 15:44:59  olga
 * code optimizing
 *
 * Revision 1.1  2009/05/12 10:36:52  olga
 * CPA: bug fixed
 * Applicability of Rule Seq. : bug fixed
 *
 * Revision 1.1  2008/10/29 09:04:11  olga
 * new sub packages of the package agg.gui: typeeditor, editor, trafo, cpa, options, treeview, popupmenu, saveload
 *
 * Revision 1.7  2007/11/19 08:48:41  olga
 * Some GUI usability mistakes fixed.
 * Default values in node/edge of a type graph implemented.
 * Code tuning.
 *
 * Revision 1.6  2007/11/01 09:58:12  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.5  2007/09/10 13:05:26  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.4 2006/12/13 13:32:54 enrico
 * reimplemented code
 * 
 * Revision 1.3 2006/11/01 11:17:29 olga Optimized agg sources of CSP algorithm,
 * match usability, graph isomorphic copy, node/edge type multiplicity check for
 * injective rule and match
 * 
 * Revision 1.2 2006/04/19 09:17:20 olga Layered Graph Constraints done
 * 
 * Revision 1.1 2005/08/25 11:56:53 enrico *** empty log message ***
 * 
 * Revision 1.3 2005/07/11 09:30:20 olga This is test version AGG V1.2.8alfa .
 * What is new: - saving rule option <disabled> - setting trigger rule for layer -
 * display attr. conditions in gragra tree view - CPA algorithm <dependencies> -
 * creating and display CPA graph with conflicts and/or dependencies based on
 * (.cpx) file
 * 
 * Revision 1.2 2005/06/20 13:37:03 olga Up to now the version 1.2.8 will be
 * prepared.
 * 
 * Revision 1.1 2005/05/30 12:58:02 olga Version with Eclipse
 * 
 * Revision 1.9 2005/03/03 13:48:42 olga - Match with NACs and attr. conditions
 * with mixed variables - error corrected - save/load class packages written by
 * user - PACs : creating T-equivalents - improved - save/load matches of the
 * rules (only one match of a rule) - more friendly graph/rule editor GUI - more
 * syntactical checks in attr. editor
 * 
 * Revision 1.8 2005/01/28 14:02:32 olga -Fehlerbehandlung beim Typgraph check
 * -Erweiterung CP GUI / CP Menu -Fehlerbehandlung mit identification option
 * -Fehlerbehandlung bei Rule PAC
 * 
 * Revision 1.7 2004/12/20 14:53:48 olga Changes because of matching
 * optimisation.
 * 
 * Revision 1.6 2004/10/25 14:24:37 olga Fehlerbehandlung bei CPs und
 * Aenderungen im zusammenhang mit termination-Modul in AGG
 * 
 * Revision 1.5 2003/12/18 16:26:41 olga GUI
 * 
 * Revision 1.4 2003/03/05 18:24:20 komm sorted/optimized import statements
 * 
 * Revision 1.3 2002/09/23 14:14:28 olga GUI fertig.
 * 
 * Revision 1.2 2002/09/19 16:22:24 olga Arbeit im wesentlichen an GUI.
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:10 olga Imported sources
 * 
 * Revision 1.5 2001/09/24 16:39:35 olga Korrektur an LayerFunction und
 * LayerGUI.
 * 
 * Revision 1.4 2001/08/16 14:05:43 olga Aenderungen wegen Layers bei
 * Transformation, Parsieren und CP
 * 
 * Revision 1.3 2001/07/19 15:18:56 olga Arbeit an GUI
 * 
 * Revision 1.2 2001/07/09 13:12:33 olga Aenderungen an GUI. Version heisst ab
 * jetzt 1.1
 * 
 * Revision 1.1 2001/06/13 16:46:39 olga Kleine Korrektur wegen GraGra Layer.
 * 
 * Revision 1.3 2001/05/14 11:52:57 olga Parser GUI Optimierung
 * 
 * Revision 1.2 2001/03/08 11:02:44 olga Parser Anbindung gemacht. Stand nach
 * AGG GUI Reimplementierung. Stand nach der AGG GUI Reimplementierung.Das ist
 * Stand nach der AGG GUI Reimplementierung und Parser Anbindung.
 * 
 * Revision 1.1.2.8 2001/01/28 13:14:44 shultzke API fertig
 * 
 * Revision 1.1.2.7 2001/01/14 14:48:19 shultzke commentare ergaenzt
 * 
 * Revision 1.1.2.6 2001/01/03 09:44:54 shultzke TODO's bis auf laden und
 * speichern erledigt. Wann meldet sich endlich Michael?
 * 
 * Revision 1.1.2.5 2000/12/26 10:00:03 shultzke Layered Parser hinzugefuegt
 * 
 * Revision 1.1.2.4 2000/12/21 13:46:01 shultzke optionen weiter veraendert
 * 
 * Revision 1.1.2.3 2000/12/18 13:33:33 shultzke Optionen veraendert
 * 
 * Revision 1.1.2.2 2000/12/12 13:27:41 shultzke erste Versuche kritische Paare
 * mit XML abzuspeichern
 * 
 * Revision 1.1.2.1 2000/12/10 14:55:47 shultzke um Layer erweitert
 * 
 */
