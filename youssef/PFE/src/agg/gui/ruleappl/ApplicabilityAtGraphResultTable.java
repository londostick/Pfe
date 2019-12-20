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
package agg.gui.ruleappl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import agg.ruleappl.ApplRuleSequence;
import agg.ruleappl.ApplicabilityConstants;
import agg.ruleappl.RuleSequence;
import agg.util.Pair;

/**
 * @author olga
 *
 */
public class ApplicabilityAtGraphResultTable {

	static final Color RED = new Color(255, 10, 50); //(255, 210, 160); 
	static final Color GREEN = new Color(155, 255, 105);
	static final Color ORANGE = new Color(255, 255, 100);
	
	protected ApplRuleSequence ars;
	protected RuleSequence ruleSequence;
	protected final Hashtable<RuleSequence, Pair<JDialog, JDialog>> 
	sequence2table = new Hashtable<RuleSequence, Pair<JDialog, JDialog>>();
	protected final Hashtable<JButton, JDialog> 
	close2table = new Hashtable<JButton, JDialog>();
	protected JScrollPane scrollpaneTable, scrollpaneTable2;
	protected JDialog tableFrame, tableFrame2;
	protected JTable resultTable, resultTable2;
	
	public ApplicabilityAtGraphResultTable(ApplRuleSequence applrs) {
		this.ars = applrs;
	}
	
	public void clear() {
//		this.closeAllResultTables();
		this.sequence2table.clear();	
		this.ruleSequence = null;
	}
	
	public int closeResultTables(final RuleSequence rseq) {
		int nb = 0;
		Pair<JDialog, JDialog> p = this.sequence2table.get(rseq);		
		if (p != null) {
			if (p.first != null) {
				nb++;
				p.first.setVisible(false);
			}
			if (p.second != null) {
				nb++;
				p.second.setVisible(false);
			}
		}
		this.sequence2table.remove(rseq);
		return nb;
	}
	
	public void closeAllResultTables() {
		Iterator<Pair<JDialog, JDialog>> iter = this.sequence2table.values().iterator();
		while (iter.hasNext()) {
			Pair<JDialog, JDialog> p = iter.next();
			if (p.first != null)
				p.first.setVisible(false);
			if (p.second != null)
				p.second.setVisible(false);
		}
		
//		this.sequence2table.clear();
	}
	
	public void showApplicabilityResult(
			final Point location,
			final int indx) {
		
		this.ruleSequence = this.ars.getRuleSequence(indx);	
    	String graphName = this.ruleSequence.getGraph().getName();
    	
        if (this.sequence2table.get(this.ruleSequence) == null
        		|| this.sequence2table.get(this.ruleSequence).first == null) { 
        	
        	final Pair<Boolean, String> result = this.ruleSequence.getApplicabilityResult();	
        	
        	if (result == null) {
        		JOptionPane.showMessageDialog(null, "This sequence isn't checked.");
        		return;
        	}
        	
        	this.resultTable = this.createResultTable(this.ruleSequence.getRuleNames());	       	
        	createResultTableFrame(this.resultTable, graphName);      	
        	this.setTitleOfTableFrame(graphName, this.tableFrame);
        	
        	if (this.sequence2table.get(this.ruleSequence) == null) {
        		this.sequence2table.put(this.ruleSequence, new Pair<JDialog, JDialog>(this.tableFrame, null));
        	} else {
        		this.sequence2table.get(this.ruleSequence).first = this.tableFrame;
        	}
        	       	
        	makeResultTableEntries(this.ruleSequence, result.first.booleanValue(), result.second);
        	
        	this.tableFrame.setLocation(location.x, location.y);
        	this.tableFrame.setVisible(true);
         	
        } else {  
        	Pair<JDialog, JDialog> p = this.sequence2table.get(this.ruleSequence);
        	this.tableFrame = p.first;
        	if (!this.tableFrame.isVisible()) {
            	this.sequence2table.remove(this.ruleSequence);
            	this.showApplicabilityResult(location, indx);          
        	} else {
        		this.setTitleOfTableFrame(graphName, this.tableFrame);
        		this.tableFrame.setVisible(true);
        	}
        }
	}
	
	public void showNonApplicabilityResult(
			final Point location,
			int indx) {
		
		this.ruleSequence = this.ars.getRuleSequence(indx);	
		String graphName = this.ruleSequence.getGraph().getName();
		
        if (this.sequence2table.get(this.ruleSequence) == null
        		|| this.sequence2table.get(this.ruleSequence).second == null) {
        	
        	this.resultTable2 = this.createResultTable2(this.ruleSequence.getRuleNames());       	
        	createResultTableFrame2(this.resultTable2, graphName);       	
        	this.setTitleOfTableFrame(graphName, this.tableFrame2);
        	
        	if (this.sequence2table.get(this.ruleSequence) == null) {
        		this.sequence2table.put(this.ruleSequence, new Pair<JDialog, JDialog>(null, this.tableFrame2));
        	} else {
        		this.sequence2table.get(this.ruleSequence).second = this.tableFrame2;
        	}
		
        	Pair<Boolean, String> result = this.ars.getNonApplicabilityResult(indx);
        	
        	if (result == null) {
        		JOptionPane.showMessageDialog(null, "This sequence isn't checked.");
        		return;
        	}
        	
        	makeResultTable2Entries(this.ruleSequence, result.first.booleanValue(), result.second);
        	      	
        	this.tableFrame2.setLocation(location.x, location.y);
        	this.tableFrame2.setVisible(true);
         	
        } else {    
        	Pair<JDialog, JDialog> p = this.sequence2table.get(this.ruleSequence);
        	this.tableFrame2 = p.second;
        	if (!this.tableFrame2.isVisible()) {
            	this.sequence2table.remove(this.ruleSequence);
            	this.showNonApplicabilityResult(location, indx);         
        	} else {
        		this.setTitleOfTableFrame(graphName, this.tableFrame2);
        		this.tableFrame2.setVisible(true);
        	}
        }
	}
	
	private void createResultTableFrame(JTable table, String graphName) {
		this.scrollpaneTable = new JScrollPane(table);
		
		// create a dialog to show the result table
		this.tableFrame = new JDialog();
		this.tableFrame.setTitle(" Applicability of Rule Sequence "); 
		
		this.tableFrame.setModal(false);
		this.tableFrame.getContentPane().setLayout(new BorderLayout());
		this.tableFrame.getContentPane().add(this.scrollpaneTable,
				BorderLayout.CENTER);
		
		JButton closeButton = makeCloseButton(this.tableFrame);
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ApplicabilityAtGraphResultTable.this.close2table.get(e.getSource()).setVisible(false);
			}
		});
		this.close2table.put(closeButton, this.tableFrame);
		
		int fheight = 100;
		if (table.getRowCount() > 0) {
			fheight = table.getCellRect(0, 0, true).height
					* (table.getRowCount() + 6);
		}
		this.tableFrame.setSize(new Dimension(800, fheight));
		this.tableFrame.validate();
	}
	
	private void createResultTableFrame2(JTable table, String graphName) {
		this.scrollpaneTable2 = new JScrollPane(table);

		// create a dialog to show the result table
		this.tableFrame2 = new JDialog();
		this.tableFrame2.setTitle(" Non-Applicability of Rule Sequence "); 
		
		this.tableFrame2.setModal(false);
		this.tableFrame2.getContentPane().setLayout(new BorderLayout());
		this.tableFrame2.getContentPane().add(this.scrollpaneTable2,
				BorderLayout.CENTER);
		
		JButton closeButton = makeCloseButton(this.tableFrame2);
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ApplicabilityAtGraphResultTable.this.close2table.get(e.getSource()).setVisible(false);
			}
		});
		this.close2table.put(closeButton, this.tableFrame2);
		
		int fheight = 100;
		if (table.getRowCount() > 0) {
			fheight = table.getCellRect(0, 0, true).height
					* (table.getRowCount() + 6);
		}
		this.tableFrame2.setSize(new Dimension(800, fheight));
		this.tableFrame2.validate();
	}
	
	private JButton makeCloseButton(JDialog dialog) {
		JPanel closePanel = new JPanel(new GridLayout(1, 0));
		JButton closeButton = new JButton();
		closeButton.setText("Close");
		closePanel.add(new JLabel("   "));
		closePanel.add(new JLabel("   "));
		closePanel.add(closeButton);
		closePanel.add(new JLabel("   "));
		closePanel.add(new JLabel("   "));
		dialog.getContentPane().add(closePanel, BorderLayout.SOUTH);
		return closeButton;
	}
	
	private void setTitleOfTableFrame(String graphName, JDialog tableframe) {
		if (tableframe == this.tableFrame) {
			tableframe.setTitle(" Applicability of Rule Sequence at Graph: "+graphName);
		}
		else if (tableframe == this.tableFrame2) {
			tableframe.setTitle(" Non-Applicability of Rule Sequence at Graph: "+graphName);
		}		
	}
	
	private void setTableItemValue(
			final RuleSequence rseq, 			
			final int row, 
			final int col,
			final String ruleName, 
			final String criterion) {
		JLabel l = new JLabel("");
		Pair<Boolean, List<String>> pair = rseq.getRuleResult(row, ruleName, criterion);
//		System.out.println("setTableItemValue::  pair:  "+pair);
		if (pair == null) {
			l.setBackground(Color.LIGHT_GRAY);
		} else {
//			System.out.println("setTableItemValue::  "+ruleName+" : "+pair.first+"   "+pair.second);
			l.setText(pair.second.get(1));
			if (pair.first.booleanValue()) {
				l.setBackground(GREEN);
			} else {
				l.setBackground(ORANGE);
			}
		}	
		this.resultTable.getModel().setValueAt(l, row, col);
	}

	private void makeResultTableEntries(
					final RuleSequence rseq,
					final boolean result, 
					final String condition) {
		
		List<String> ruleNames = rseq.getRuleNames();
		
//		System.out.println("\n "+ruleNames);
//		System.out.println(result+"    "+condition);
		
		for (int row = 0; row < ruleNames.size(); row++) {
			for (int col = 1; col < this.resultTable.getColumnCount(); col++) {
					if (col == 1) {
						this.setTableItemValue(rseq, row, col, ruleNames.get(row), ApplicabilityConstants.INITIALIZATION);
					} else if (col == 2) {
						this.setTableItemValue(rseq, row, col, ruleNames.get(row), ApplicabilityConstants.NO_NODE_DELETING);
					} else if (col == 3) {
						this.setTableItemValue(rseq, row, col, ruleNames.get(row), ApplicabilityConstants.NO_IMPEDING_PREDECESSORS);
					} else if (col == 4) {
						this.setTableItemValue(rseq, row, col, ruleNames.get(row), ApplicabilityConstants.PURE_ENABLING_PREDECESSOR);
					} else if (col == 5) {
						this.setTableItemValue(rseq, row, col, ruleNames.get(row), ApplicabilityConstants.PARTIAL_ENABLING_PREDECESSOR);						
					} else if (col == 6) {
						this.setTableItemValue(rseq, row, col, ruleNames.get(row), ApplicabilityConstants.DIRECT_ENABLING_PREDECESSOR);
					} else if (col == 7) {
						this.setTableItemValue(rseq, row, col, ruleNames.get(row), ApplicabilityConstants.PREDECESSOR_NOT_NEEDED);
					} 
			}	
		}
	}
	
/*
	private void setTable2ItemValue(
			final RuleSequence rseq, 			
			final int row, 
			final int col,
			final String ruleName, 
			final String criterion) {
		JLabel l = new JLabel("");
		Pair<Boolean, List<String>> pair = rseq.getRuleResult(row, ruleName, criterion);
//		System.out.println("setTableItemValue::  pair:  "+pair);
		if (pair == null) {
			l.setBackground(Color.LIGHT_GRAY);
		} else {
//			System.out.println("setTableItemValue::  "+ruleName+" : "+pair.first+"   "+pair.second);
			l.setText(pair.second.get(1));
			if (pair.first.booleanValue()) {
				l.setBackground(GREEN);
			} else {
				l.setBackground(ORANGE);
			}
		}	
		resultTable2.getModel().setValueAt(l, row, col);
	}
	*/
	
	private void makeResultTable2Entries(
			final RuleSequence rseq,
			final boolean result, 
			final String condition) {
//		System.out.println("makeResultTable2Entries::"+result+"   "+condition); 
		List<String> ruleNames = rseq.getRuleNames();
		for (int row = 0; row < ruleNames.size(); row++) {
			for (int col = 1; col < this.resultTable2.getColumnCount(); col++) {			
				JLabel l = new JLabel("");
				if (result) {
					if (condition.equals(ApplicabilityConstants.INITIALIZATION_ERROR)) {
						if (row == 0 && col == 1) {
							l.setBackground(RED);	
							this.resultTable2.getModel().setValueAt(l, row, col);
						} else if (row != 0 && col == 2) {
							setNoEnablingPredecessor(rseq, row, ruleNames.get(row));	
						} else {
							l.setBackground(Color.LIGHT_GRAY);	
							this.resultTable2.getModel().setValueAt(l, row, col);
						}
					} else if (condition.equals(ApplicabilityConstants.NO_ENABLING_PREDECESSOR)) {
						if (row == 0 && col == 1) {
							setInitializationError(rseq, row, ruleNames.get(row));
						} else if (row != 0 && col == 2) {
							setNoEnablingPredecessor(rseq, row, ruleNames.get(row));
//							l.setBackground(RED);	
//							resultTable2.getModel().setValueAt(l, row, col);
						}  else {
							l.setBackground(Color.LIGHT_GRAY);	
							this.resultTable2.getModel().setValueAt(l, row, col);
						}
					}
			
				} else {			
					if (row == 0 && col == 1) {
						setInitializationError(rseq, row, ruleNames.get(row));
					} else if (row != 0 && col == 2) {
						setNoEnablingPredecessor(rseq, row, ruleNames.get(row));
					} else {
						l.setBackground(Color.LIGHT_GRAY);	
						this.resultTable2.getModel().setValueAt(l, row, col);
					} 
				}
				
			}
		}
	}

/*
	private String makeRuleKey(final int row, final String rName, final String criterion) {
		String key = String.valueOf(row);
		key = key.concat(rName);
		key = key.concat(criterion);
		return key;
	}
*/	
 
 	
	private void setInitializationError(RuleSequence rseq, int row, String ruleName) {
		JLabel l = new JLabel("");
		Pair<Boolean, List<String>> pair = rseq.getRuleResult(row, ruleName, ApplicabilityConstants.INITIALIZATION_ERROR);
		if (pair == null) {
			l.setBackground(Color.LIGHT_GRAY);
		} else if (row == 0) {
			if (pair.first.booleanValue()) {
				l.setBackground(RED);
			} else {
				l.setBackground(ORANGE);
			}			
		} else {
			l.setBackground(Color.LIGHT_GRAY);
		}
		this.resultTable2.getModel().setValueAt(l, row, 1);
	}
	
	private void setNoEnablingPredecessor(RuleSequence rseq, int row, String ruleName) {
		JLabel l = new JLabel("");
		Pair<Boolean, List<String>> pair = rseq.getRuleResult(row, ruleName, ApplicabilityConstants.NO_ENABLING_PREDECESSOR);
//		System.out.println("setNoEnablingPredecessor::  "+row+"   "+ruleName+"   "+pair);
		if (pair == null) {
			l.setBackground(Color.LIGHT_GRAY);
		} else {
//			System.out.println("setNoEnablingPredecessor::  "+row+"   "+ruleName+"   "+pair.second.get(1)+"   "+pair.first.booleanValue());
			l.setText(pair.second.get(1));
			if (pair.first.booleanValue()) {
				l.setBackground(RED);
			}
			else {
				l.setBackground(ORANGE);	
			}
		}	
		this.resultTable2.getModel().setValueAt(l, row, 2);
	}	
	
	
	@SuppressWarnings("serial")
	private JTable createResultTable(final List<String> sequence) {
		final int columnSize1 = 8;
		
		TableModel dataModel = new DefaultTableModel(new String[] { 
				"Rule / Criteria",
				"(1) initialization", 
				"(2) no node-deleting rules", 
				"(3) no impeding predecessors", 
				"(4a) pure enabling predecessor",
				"(4b) partial enabling predecessor",
				"(4c) direct enabling predecessor(s)",
				"(4d) not needed"}, columnSize1) {
			
			public int getColumnCount() {
				return columnSize1;
			}

			public int getRowCount() {
				if (ApplicabilityAtGraphResultTable.this.ruleSequence != null)
					return ApplicabilityAtGraphResultTable.this.ruleSequence.getRules().size();
				
				return -1;
			}

			public Object getValueAt(int row, int col) {
				if (row < super.getRowCount() && col < super.getColumnCount()) {
					return super.getValueAt(row, col);
				} 
				
				return null;
			}
		};
		
		JTable table = new JTable(dataModel);
		table.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		for (int i=0; i<columnSize1; i++) {
			table.setDefaultRenderer(table.getColumnClass(i),
					new MyTableCellRenderer());
		}
		
		for (int row = 0; row < sequence.size(); row++) {
			String ruleName = sequence.get(row);
			JLabel rulel = new JLabel(ruleName);
			table.getModel().setValueAt(rulel, row, 0);
			
			for (int col = 1; col < columnSize1; col++) {
				JLabel l = new JLabel("");
				table.getModel().setValueAt(l, row, col);
			}
		}
		return table;
	}
	
	@SuppressWarnings("serial")
	private JTable createResultTable2(final List<String> sequence) {
		final int columnSize2 = 3;
		
		TableModel dataModel = new DefaultTableModel(new String[] { 
				"Rule / Criteria",
				"(1) initialization error", 
				"(2) no enabling predecessor"}, columnSize2) {
			
			public int getColumnCount() {
				return columnSize2;
			}

			public int getRowCount() {				
				return ApplicabilityAtGraphResultTable.this.ruleSequence.getRules().size();
			}

			public Object getValueAt(int row, int col) {
				if (row < super.getRowCount() && col < super.getColumnCount()) {
					return super.getValueAt(row, col);
				} 
				return null;
			}
		};
		
		JTable table = new JTable(dataModel);
		table.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		for (int i=0; i<columnSize2; i++) {
			table.setDefaultRenderer(table.getColumnClass(i),
					new MyTableCellRenderer());
		}
		
		for (int row = 0; row < sequence.size(); row++) {
			String ruleName = sequence.get(row);
			JLabel rulel = new JLabel(ruleName);
			table.getModel().setValueAt(rulel, row, 0);
			
			for (int col = 1; col < columnSize2; col++) {
				JLabel l = new JLabel("");
				table.getModel().setValueAt(l, row, col);
			}
		}
		return table;
	}
	
	@SuppressWarnings("serial")
	class MyTableCellRenderer extends JLabel implements TableCellRenderer {

		public MyTableCellRenderer() {
			setOpaque(true);
		}

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column)

		{
			setOpaque(true);

			if (value instanceof JLabel) {
				JLabel l = (JLabel) value;
				if (column > 0) {
					setBackground(l.getBackground());
					setText(l.getText());
					return this; 
				} 
				return new JLabel(l.getText());
			
			} else if (value instanceof String)
				return new JLabel((String) value);
			else
				return new JLabel("");
		}
	}

}
