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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JButton;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.JTable;
import javax.swing.DefaultCellEditor;
import javax.swing.table.DefaultTableModel;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JDialog;
import javax.swing.border.TitledBorder;
import javax.swing.border.Border;
import javax.swing.SwingUtilities;

import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdRule;
import agg.gui.help.HtmlBrowser;
import agg.ruleappl.RuleSequence;
import agg.util.Pair;


@SuppressWarnings("serial")
public class RuleSequenceDialog extends JDialog implements TableModelListener,
		ListSelectionListener {

	protected final JFrame applFrame;
	
	protected EdGraGra gragra;
	
	protected List<EdRule> rules;
	
	protected JTable ruleList, groupList, groupRuleList;

	protected JScrollPane scrollRuleList, scrollGroupList, scrollGroupRuleList;

	protected List<Pair<List<Pair<String, String>>, String>> groups;
	protected List<Pair<String, String>> group;
	
	@SuppressWarnings("rawtypes")
	protected JList ruleSequenceTextList;

	protected JScrollPane scrollpaneSequenceText;

	final protected List<String> ruleNames  = new Vector<String>();
	final protected List<String> groupNames = new Vector<String>();

	protected JButton addGroup, deleteGroup, addRule, deleteRule, close, cancel, help, objectFlow;

	boolean enabled;
	public boolean showWarning;
	
	protected Integer groupCount = Integer.valueOf(0);

	protected MouseListener ml;

	protected ListSelectionListener lsl;

	protected int fromIndx, toIndx, selGroupIndx = -1, selRuleIndx = -1;

	final protected List<String> groupListColumnNames; 
	final protected List<String> groupRuleListColumnNames;

	protected JDialog dialog;

	protected HtmlBrowser helpBrowser;

	final protected String title = "Transformation by Rule Sequence";
		
	final protected Hashtable<RuleSequence, ObjectFlowDesktop> 
	objFlowDesktopList = new Hashtable<RuleSequence, ObjectFlowDesktop>();
	protected ObjectFlowDesktop objFlowDesktop;
	
	
	public RuleSequenceDialog(final JFrame frame, final Point location) {
		super(); //frame);
		
		this.applFrame = frame;
		this.dialog = this;
		this.showWarning = true;

		setModal(false);
		setTitle(this.title);

		this.groups = new Vector<Pair<List<Pair<String, String>>, String>>();
		this.group = new Vector<Pair<String, String>>();
		
		this.groupListColumnNames = new Vector<String>(2);
		this.groupListColumnNames.add("Subsequence");
		this.groupListColumnNames.add("Iterations");

		this.groupRuleListColumnNames = new Vector<String>(2);
		this.groupRuleListColumnNames.add("Rule");
		this.groupRuleListColumnNames.add("Iterations");

		final JPanel content = initContentPane();
		
		JScrollPane scroll = new JScrollPane(content);
		scroll.setPreferredSize(new Dimension(550, 750));
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(scroll, BorderLayout.CENTER);
		validate();

		setLocation(location);
		pack();
	}

	public void extendTitle(String name) {
		String str = " ".concat(name).concat(" - ");
		super.setTitle(str.concat(this.title));
	}
	
	public void refresh(
			final List<EdRule> edrules, 
			final List<Pair<List<Pair<String, String>>, String>> subsequences) {
			for (int i = 0; i < this.ruleNames.size(); i++) {
				String rname = this.ruleNames.get(i);
				if (this.gragra.getRule(rname) == null) {
					this.ruleNames.remove(i);
					i--;
				}
			}
			
			this.updateRuleSequences(subsequences);
	}
	
	
	public void updateRules(final List<EdRule> edrules) {
		if (this.rules != edrules) {
//		if (this.rules == null || !this.rules.equals(edrules)) {
			this.groups = null;
			this.group = null;
			clearGroups();
		}
		
		this.rules = edrules;
		if (this.ruleList != null) {
			this.scrollRuleList.getViewport().remove(this.ruleList);			
			if (this.rules != null && !this.rules.isEmpty()) {
				this.ruleNames.clear();
				for (int i = 0; i < edrules.size(); i++) {
					EdRule r = edrules.get(i);
					this.ruleNames.add(r.getBasisRule().getName());
				}
			
				this.ruleList = new JTable(this.ruleNames.size(), 1);
				this.ruleList.getModel().addTableModelListener(this);
				for (int i = 0; i < this.ruleNames.size(); i++) {
					this.ruleList.getModel().setValueAt(this.ruleNames.get(i), i, 0);
					((DefaultCellEditor) this.ruleList.getCellEditor(i, 0))
							.getComponent().setEnabled(false);
				}
				this.scrollRuleList.getViewport().setView(this.ruleList);								
			} else {				
				this.ruleList = new JTable(0, 1);			
				this.scrollRuleList.getViewport().setView(this.ruleList);
			}	
			this.ruleList.getSelectionModel().clearSelection();
			
			updateObjectFlow();
		}
	}

	public void setGraGra(final EdGraGra gra) {
		this.gragra = gra;
	}
	
	
	public void updateRuleSequencesList() {
		this.updateRuleSequencesTextList(this.getRuleSequencesText(this.groups));
	}
		
	public void updateRuleSequences(
			final List<Pair<List<Pair<String, String>>, String>> sequences) {
		clearGroups();
		
		if (sequences != null) {			
			this.groups.addAll(sequences);
//			addAllElements(sequences, groups);
			updateGroupList();
			updateRuleSequencesTextList(getRuleSequencesText(this.groups));
		} else {
			this.setVisible(false);
		}
	}

	
	/**
	 * Returns a Vector with elements of type Pair(Vector, String) which
	 * represents a rule sequence. A Vector of Pair represents a rule sequence. A
	 * String of Pair is the value for the iteration count of a rule sequence,
	 * Furthermore, a Vector contains of Pair(String, String). Here the first
	 * String is the name of a rule, the second String is the value for the
	 * iteration count of a rule. The value for the iteration count can be "*"
	 * or an integer > 0.
	 */
	public List<Pair<List<Pair<String, String>>, String>> getRuleSequences() {
		return this.groups;
	}

	public void enableGUI(boolean b) {
		this.enabled = b;
		this.ruleList.setEnabled(b);
		this.groupList.setEnabled(b);
		this.groupRuleList.setEnabled(b);
		this.addGroup.setEnabled(b);
		this.deleteGroup.setEnabled(b);
		this.addRule.setEnabled(b);
		this.deleteRule.setEnabled(b);
		this.close.setEnabled(b);
		this.cancel.setEnabled(b);
		this.help.setEnabled(b);
		this.ruleSequenceTextList.setEnabled(b);
	}

	public boolean isGUIEnabled() {
		return this.enabled;
	}
	
	@SuppressWarnings("rawtypes")
	private JPanel initContentPane() {
		Border border = new TitledBorder("");

		this.ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isMiddleMouseButton(e)) 
					RuleSequenceDialog.this.dialog.setCursor(new Cursor(Cursor.MOVE_CURSOR));
					
					if (e.getSource() == RuleSequenceDialog.this.groupList) {
						RuleSequenceDialog.this.fromIndx = RuleSequenceDialog.this.groupList.rowAtPoint(new Point(e.getX(), e
								.getY()));
						if (RuleSequenceDialog.this.groupList.columnAtPoint(new Point(e.getX(), e.getY())) == 0)
							((DefaultCellEditor) RuleSequenceDialog.this.groupList.getCellEditor(RuleSequenceDialog.this.fromIndx,
									0)).getComponent().setEnabled(false);
						else
							((DefaultCellEditor) RuleSequenceDialog.this.groupList.getCellEditor(RuleSequenceDialog.this.fromIndx,
									1)).getComponent().setEnabled(true);
						
					} else if (e.getSource() == RuleSequenceDialog.this.groupRuleList) {
						RuleSequenceDialog.this.fromIndx = RuleSequenceDialog.this.groupRuleList.rowAtPoint(new Point(e.getX(), e
								.getY()));
						if (RuleSequenceDialog.this.groupRuleList.columnAtPoint(new Point(e.getX(), e
								.getY())) == 0)
							((DefaultCellEditor) RuleSequenceDialog.this.groupRuleList.getCellEditor(
									RuleSequenceDialog.this.fromIndx, 0)).getComponent().setEnabled(false);
						else
							((DefaultCellEditor) RuleSequenceDialog.this.groupRuleList.getCellEditor(
									RuleSequenceDialog.this.fromIndx, 1)).getComponent().setEnabled(true);
					}
			}
			
			public void mouseReleased(MouseEvent e) {
				if (e.getSource() == RuleSequenceDialog.this.groupList) {
					RuleSequenceDialog.this.toIndx = RuleSequenceDialog.this.groupList
							.rowAtPoint(new Point(e.getX(), e.getY()));
					if (RuleSequenceDialog.this.toIndx >= 0) {
						RuleSequenceDialog.this.selGroupIndx = RuleSequenceDialog.this.toIndx;
					
						if (SwingUtilities.isMiddleMouseButton(e)) {
							Pair<List<Pair<String, String>>, String> v = RuleSequenceDialog.this.groups
								.get(RuleSequenceDialog.this.fromIndx);
							RuleSequenceDialog.this.groups.remove(v);
							RuleSequenceDialog.this.groups.add(RuleSequenceDialog.this.toIndx, v);
							((DefaultTableModel) RuleSequenceDialog.this.groupList.getModel()).moveRow(
									RuleSequenceDialog.this.fromIndx, RuleSequenceDialog.this.fromIndx, RuleSequenceDialog.this.toIndx);
							updateGroups();
							updateRuleSequencesTextList(getRuleSequencesText(RuleSequenceDialog.this.groups));
							RuleSequenceDialog.this.groupList.changeSelection(RuleSequenceDialog.this.selGroupIndx, 0, false, false);
						} 
					}
				} else if (e.getSource() == RuleSequenceDialog.this.groupRuleList) {
					RuleSequenceDialog.this.toIndx = RuleSequenceDialog.this.groupRuleList.rowAtPoint(new Point(e.getX(), e
							.getY()));
					if (RuleSequenceDialog.this.toIndx >= 0) {
						RuleSequenceDialog.this.selRuleIndx = RuleSequenceDialog.this.toIndx;
						
						if (SwingUtilities.isMiddleMouseButton(e)) {				
							Pair<String, String> v = RuleSequenceDialog.this.group.get(RuleSequenceDialog.this.fromIndx);
							RuleSequenceDialog.this.group.remove(v);
							RuleSequenceDialog.this.group.add(RuleSequenceDialog.this.toIndx, v);
							((DefaultTableModel) RuleSequenceDialog.this.groupRuleList.getModel()).moveRow(
									RuleSequenceDialog.this.fromIndx, RuleSequenceDialog.this.fromIndx, RuleSequenceDialog.this.toIndx);					
							updateRuleSequencesTextList(getRuleSequencesText(RuleSequenceDialog.this.groups));
							RuleSequenceDialog.this.groupRuleList.changeSelection(RuleSequenceDialog.this.selRuleIndx, 0, false, false);
							RuleSequenceDialog.this.ruleSequenceTextList.setSelectedIndex(RuleSequenceDialog.this.selGroupIndx);
						}
					}
				}
				RuleSequenceDialog.this.dialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		};

		JPanel p = new JPanel(new BorderLayout());

		JPanel p0 = new JPanel(new GridLayout(0, 1));
		p0.setLayout(new GridBagLayout());

		JPanel p1 = new JPanel(new GridBagLayout());
		p1.setBorder(border);
		JPanel p11 = new JPanel(new BorderLayout());
		JPanel p12 = new JPanel(new BorderLayout());
		JLabel l = new JLabel(" Add a new rule subsequence by clicking ");
		this.addGroup = new JButton("New Subsequence");
		this.addGroup.setEnabled(false);
		this.addGroup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (RuleSequenceDialog.this.ruleList.getRowCount() != 0) { 
					RuleSequenceDialog.this.groupCount = Integer.valueOf(RuleSequenceDialog.this.groups.size());
					Vector<Pair<String, String>> seqvec = new Vector<Pair<String, String>>();
					Pair<List<Pair<String, String>>, String> 
					seqvecpair = new Pair<List<Pair<String, String>>, String>(seqvec, "1");
					RuleSequenceDialog.this.groups.add(seqvecpair);
					RuleSequenceDialog.this.group = RuleSequenceDialog.this.groups.get(RuleSequenceDialog.this.groupCount.intValue()).first;
					RuleSequenceDialog.this.groupCount = Integer.valueOf(RuleSequenceDialog.this.groupCount.intValue()+1);
					updateGroupList();
					int indx = RuleSequenceDialog.this.groupCount.intValue() - 1;
	//				RuleSequenceDialog.this.groupList.getSelectionModel().setLeadSelectionIndex(indx);
					RuleSequenceDialog.this.groupList.changeSelection(indx, 0, false, false);
					RuleSequenceDialog.this.selGroupIndx = indx;
					RuleSequenceDialog.this.groupRuleList.removeAll();
				}
			}
		});
		p11.add(l, BorderLayout.CENTER);
		p11.add(this.addGroup, BorderLayout.EAST);
		p11.add(new JLabel("       "), BorderLayout.SOUTH);
		l = new JLabel(" Select a rule   &  add to the currently selected subsequence ");
		this.ruleList = new JTable(0, 1);
		this.scrollRuleList = new JScrollPane(this.ruleList);
		this.scrollRuleList.setPreferredSize(new Dimension(300, 100));
		this.addRule = new JButton("Add");
		JPanel addRuleP = makeAddRuleButtonPanel(this.addRule);
		this.addRule.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (RuleSequenceDialog.this.group != null) {				
					int[] selValues = RuleSequenceDialog.this.ruleList.getSelectedRows();
					for (int i = 0; i < selValues.length; i++) {
						String rulename = (String) RuleSequenceDialog.this.ruleList.getValueAt(
								selValues[i], 0);
						Pair<String, String> rulepair = new Pair<String, String>(
								rulename, "1");
						RuleSequenceDialog.this.group.add(rulepair);
					}
					updateGroupRuleList();
					updateRuleSequencesTextList(getRuleSequencesText(RuleSequenceDialog.this.groups));
					RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().refresh();
				}
			}
		});
		p12.add(l, BorderLayout.NORTH);
		p12.add(this.scrollRuleList, BorderLayout.CENTER);
		p12.add(addRuleP, BorderLayout.EAST);
		
		constrainBuild(p1, p11, 
				0, 0, 
				1, 1, 
				GridBagConstraints.NONE,
				GridBagConstraints.NORTH, 
				0.0, 0.0, 
				10, 5, 5, 5);	
		constrainBuild(p1, p12, 
				GridBagConstraints.RELATIVE, 1, 
				1, 1, 
				GridBagConstraints.BOTH,
				GridBagConstraints.NORTH, 
				1.0, 1.0, 
				5, 5, 10, 5);

		JPanel p2 = new JPanel(new GridBagLayout());
		p2.setBorder(border);
		JPanel p21 = new JPanel(new BorderLayout());
		JPanel p22 = new JPanel(new BorderLayout());
		l = new JLabel(
				" Select a subsequence    &   set the max count of iterations  ");
		this.groupNames.clear();
		this.scrollGroupList = new JScrollPane();
		this.scrollGroupList.setPreferredSize(new Dimension(300, 100));
		this.groupList = createGroupList(this.groupNames);
		this.deleteGroup = new JButton("Delete Subsequence");
		this.deleteGroup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (RuleSequenceDialog.this.selGroupIndx == -1) {
					return;
				}
				RuleSequenceDialog.this.groups.remove(RuleSequenceDialog.this.selGroupIndx);
				RuleSequenceDialog.this.groupCount = Integer.valueOf(RuleSequenceDialog.this.groupCount.intValue()-1);
				updateGroupList();
				if (!RuleSequenceDialog.this.groups.isEmpty()) {
					RuleSequenceDialog.this.group = RuleSequenceDialog.this.groups.get(RuleSequenceDialog.this.groups.size()-1).first;
					RuleSequenceDialog.this.selGroupIndx--;
					if (RuleSequenceDialog.this.selGroupIndx == -1 && !RuleSequenceDialog.this.groups.isEmpty())
						RuleSequenceDialog.this.selGroupIndx = 0;
					RuleSequenceDialog.this.groupList.changeSelection(RuleSequenceDialog.this.selGroupIndx, 0, false, false);
					RuleSequenceDialog.this.selRuleIndx = -1;
				} else {
					RuleSequenceDialog.this.group.clear();// = new Vector<Pair<String, String>>();
					updateGroupRuleList();
					RuleSequenceDialog.this.selGroupIndx = -1;
					RuleSequenceDialog.this.selRuleIndx = -1;
					RuleSequenceDialog.this.groupCount = Integer.valueOf(0);
				}
				updateRuleSequencesTextList(getRuleSequencesText(RuleSequenceDialog.this.groups));
				RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().refresh();
			}
		});
		JPanel deleteGroupPanel = makeButtonPanel(this.deleteGroup);
		p21.add(l, BorderLayout.NORTH);
		p21.add(this.scrollGroupList, BorderLayout.CENTER);
		p21.add(deleteGroupPanel, BorderLayout.SOUTH);
		l = new JLabel(" Select a rule    &    set the max count of iterations ");
		this.scrollGroupRuleList = new JScrollPane();
		this.scrollGroupRuleList.setPreferredSize(new Dimension(300, 100));
		this.groupRuleList = createGroupRuleList(this.group);
		this.deleteRule = new JButton("Delete Rule");
		this.deleteRule.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (RuleSequenceDialog.this.selRuleIndx == -1) {
					return;
				}
				RuleSequenceDialog.this.group.remove(RuleSequenceDialog.this.selRuleIndx);
				updateGroupRuleList();
				RuleSequenceDialog.this.selRuleIndx--;
				if (RuleSequenceDialog.this.selRuleIndx == -1 && !RuleSequenceDialog.this.group.isEmpty())
					RuleSequenceDialog.this.selRuleIndx = 0;
				RuleSequenceDialog.this.groupRuleList.getSelectionModel().setLeadSelectionIndex(
						RuleSequenceDialog.this.selRuleIndx);
				updateRuleSequencesTextList(getRuleSequencesText(RuleSequenceDialog.this.groups));
				RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().refresh();
			}
		});
		JPanel deleteRulePanel = makeButtonPanel(this.deleteRule);
		p22.add(l, BorderLayout.NORTH);
		p22.add(this.scrollGroupRuleList, BorderLayout.CENTER);
		p22.add(deleteRulePanel, BorderLayout.SOUTH);
		
		constrainBuild(p2, p21, 0, 0, 1, 1, GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 1.0, 0.0, 5, 5, 5, 5);
		constrainBuild(p2, p22, 
				GridBagConstraints.RELATIVE, 1, 
				1, 1, 
				GridBagConstraints.BOTH,
				GridBagConstraints.NORTH, 
				1.0, 1.0, 
				5, 5, 5, 5);
		
		JPanel p3 = new JPanel(new BorderLayout());
		p3.setBorder(border);
		p3.add(new JLabel(" Transformation rule sequence defined "),
				BorderLayout.NORTH);

		this.ruleSequenceTextList = new JList();

		this.scrollpaneSequenceText = new JScrollPane(this.ruleSequenceTextList);
		this.scrollpaneSequenceText.setPreferredSize(new Dimension(250, 100));
		p3.add(this.scrollpaneSequenceText, BorderLayout.CENTER);

		JPanel p4 = new JPanel(new GridBagLayout());
		this.close = new JButton("Close");
		this.close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {	
				closeObjectFlowDeskTop();
				setVisible(false);
			}
		});
		
		this.objectFlow = this.makeObjectFlowButton();
		
		this.cancel = new JButton("Cancel");
		this.cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearGroups();
				updateRuleSequencesTextList(getRuleSequencesText(RuleSequenceDialog.this.groups));
				setVisible(false);
			}
		});
		this.help = new JButton("Help");
		this.help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (RuleSequenceDialog.this.helpBrowser != null) {
					RuleSequenceDialog.this.helpBrowser.dispose();
					RuleSequenceDialog.this.helpBrowser = null;
				}
				if (RuleSequenceDialog.this.helpBrowser == null) {
					RuleSequenceDialog.this.helpBrowser = new HtmlBrowser("RuleSequencesHelp.html");
					RuleSequenceDialog.this.helpBrowser.setSize(500, 600);
					RuleSequenceDialog.this.helpBrowser.setLocation(20, 20);
					RuleSequenceDialog.this.helpBrowser.setVisible(true);
				}
			}
		});

		constrainBuild(p4, this.close, 0, 0, 1, 1, GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 1.0, 0.0, 5, 25, 5, 25);
		constrainBuild(p4, this.objectFlow, 1, 0, 1, 1, GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 1.0, 0.0, 5, 25, 5, 25);
		constrainBuild(p4, this.cancel, 2, 0, 1, 1, GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 1.0, 0.0, 5, 25, 5, 25);
		constrainBuild(p4, this.help, 3, 0, 1, 1, GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 1.0, 0.0, 5, 25, 5, 25);

		constrainBuild(p0, p1, 
				GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE,  
				1, 1, 
				GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 
				1.0, 1.0, 
				5, 5, 5, 5);
		constrainBuild(p0, p2, 
				GridBagConstraints.RELATIVE, 1,
				1, 1, 
				GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 
				1.0, 1.0, 
				5, 5, 5, 5);		
		constrainBuild(p0, p3, 0, 2, 1, 1, GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 1.0, 0.0, 5, 5, 5, 5);
		
		p.add(p0, BorderLayout.CENTER);
		p.add(p4, BorderLayout.SOUTH);
		return p;
	}

	void closeObjectFlowDeskTop() {
		Iterator<ObjectFlowDesktop> elems = this.objFlowDesktopList.values().iterator();
		while (elems.hasNext()) {
			elems.next().setVisible(false);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void valueChanged(ListSelectionEvent e) {
//		System.out.println("valueChanged::  "+e.getSource());
		if (e.getSource() instanceof DefaultListSelectionModel) {
			int indx = ((DefaultListSelectionModel) e.getSource())
					.getLeadSelectionIndex();
			if (indx != -1 && !this.groups.isEmpty()) {
				this.group = this.groups.get(indx).first;
				updateGroupRuleList();
				if (indx != this.ruleSequenceTextList.getSelectedIndex()) {
					this.ruleSequenceTextList.setSelectedIndex(indx);
				}
			}
		} else if (e.getSource() instanceof JList) {
			int indx = ((JList) e.getSource()).getSelectionModel()
					.getLeadSelectionIndex();
			if (indx != -1 && !this.groups.isEmpty() && indx != this.selGroupIndx) {
				this.groupList.changeSelection(indx, 0, false, false);
			}
		}
	}

	private JTable createGroupList(List<String> names) {
		updateGroupList();
		return this.groupList;
	}

	private JTable createGroupRuleList(List<Pair<String, String>> names) {
		updateGroupRuleList();
		return this.groupRuleList;
	}

	private JPanel makeButtonPanel(JButton b) {
		JPanel p = new JPanel(new GridLayout(1, 0));
		p.add(b);
		p.add(new JLabel("    "));
		p.add(new JLabel("    "));
		return p;
	}

	private JPanel makeAddRuleButtonPanel(JButton b) {
		JPanel p = new JPanel(new GridLayout(0, 1));
		p.add(new JLabel("       "));
		p.add(new JLabel("       "));
		p.add(b);
		p.add(new JLabel("       "));
		p.add(new JLabel("       "));
		return p;
	}

	
	private JButton makeObjectFlowButton() {
		JButton b = new JButton("Object Flow");
		b.setToolTipText("Click here to open | refresh Object Flow dialog");
		b.setEnabled(true);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().containsRuleLoop()
						&& RuleSequenceDialog.this.objFlowDesktop != null
						&& RuleSequenceDialog.this.objFlowDesktop.getGraGra() != RuleSequenceDialog.this.gragra) {
					RuleSequenceDialog.this.showWarning = true;					
				}
				
				RuleSequenceDialog.this.objFlowDesktop = RuleSequenceDialog.this.objFlowDesktopList.get(RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence());
				if (RuleSequenceDialog.this.objFlowDesktop == null) {
					// create new Object Flow desktop
					RuleSequenceDialog.this.objFlowDesktop = new ObjectFlowDesktop(
							RuleSequenceDialog.this.applFrame, 
							RuleSequenceDialog.this.gragra, 
							RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence(),
							new Point(RuleSequenceDialog.this.dialog.getLocation().x+100, RuleSequenceDialog.this.dialog.getLocation().y+50));
				
					RuleSequenceDialog.this.objFlowDesktopList.put(
							RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence(), 
							RuleSequenceDialog.this.objFlowDesktop);
					
					if (RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().containsRuleLoop()) {
						RuleSequenceDialog.this.showWarning = (showWarnDialog() == 0);
					}
				} 
				else {
					// evntl. refresh already existing Object Flow desktop
					if (RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().doesCheckAtGraph()
							&& (RuleSequenceDialog.this.gragra.getBasisGraGra().getGraph() 
									!= RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().getGraph())) {
						int answer = 0;
						Object[] options = { "OK", "Cancel" };
						if (RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().isObjFlowActive()
								|| RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().isChecked()) {
							answer = JOptionPane.showOptionDialog(
									RuleSequenceDialog.this.dialog,
								"<html><body>"
								+"Currently selected sequence contains an object flow \n"
								+"or is already checked at the graph  \""
								+RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().getGraph().getName()
								+"\" \n"
								+"The object flow resp. results are not more valid after graph reset.",
								"Reset Graph", JOptionPane.DEFAULT_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
						}
						if (answer == 0) {
							RuleSequenceDialog.this.gragra.getBasisGraGra()
										.getCurrentRuleSequence()
											.setGraph(RuleSequenceDialog.this.gragra.getBasisGraGra().getGraph());			
//							gragra.getBasisGraGra().getCurrentRuleSequence().setCheckAtGraph(true);
						} 
					} 
					
					RuleSequenceDialog.this.objFlowDesktop.refresh();	

					if (RuleSequenceDialog.this.showWarning
							&& RuleSequenceDialog.this.gragra.getBasisGraGra().getCurrentRuleSequence().containsRuleLoop()) 
						RuleSequenceDialog.this.showWarning = (showWarnDialog() == 0);

				}
				
				RuleSequenceDialog.this.objFlowDesktop.setVisible(true);
			}
		});
		return b;
	}
	
	public void closeObjectFlow() {
		if (!this.objFlowDesktopList.isEmpty()) {
			Iterator<ObjectFlowDesktop> collect = this.objFlowDesktopList.values().iterator();
			while (collect.hasNext()) {
				ObjectFlowDesktop ofd = collect.next();				
				ofd.setVisible(false);				
			}			
		}
	}
	
	protected int showWarnDialog() {
		Object[] options = { "OK", "Do not warn again" };	
		int answer = JOptionPane.showOptionDialog(					
				RuleSequenceDialog.this.dialog,
				"Please note:\n"
				+"The ( * ) iterations of a rule will be converted to 2 times.\n\n",
				"Warning", 
				JOptionPane.DEFAULT_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE, 
				null,
				options, options[1]);
		return answer;
	}
	
	protected void updateObjectFlow() {
		if (!this.objFlowDesktopList.isEmpty()) {
			Iterator<ObjectFlowDesktop> collect = this.objFlowDesktopList.values().iterator();
			while (collect.hasNext()) {
				ObjectFlowDesktop ofd = collect.next();	
				if (ofd.isVisible())
					ofd.refresh();			
			}
		}
	}
	
	protected List<String> getRuleSequencesText(List<Pair<List<Pair<String, String>>, String>> sequences) {
		Vector<String> v = new Vector<String>(sequences.size());
		for (int i = 0; i < sequences.size(); i++) {
			Pair<List<Pair<String, String>>, String> g = sequences.get(i);
			String grpStr = "";
			List<Pair<String, String>> grpRules = g.first;
			long grpIters = -1;
			String grpItersStr = g.second;
			if (grpItersStr.equals("*"))
				grpStr = grpStr + "( ";
			else {
				try {
					grpIters = (new Long(g.second)).longValue();
					if (grpRules.size() > 1 || grpIters > 1)
						grpStr = grpStr + "( ";
				} catch (java.lang.NumberFormatException ex) {}
			}
			for (int j = 0; j < grpRules.size(); j++) {
				Pair<String, String> p = grpRules.get(j);
				String rulename = p.first;
				grpStr = grpStr + rulename;
				long ruleIters = -1;
				String ruleItersStr = p.second;
				if (ruleItersStr.equals("*"))
					grpStr = grpStr + "{" + ruleItersStr + "}";
				else {
					ruleIters = (new Long(p.second)).longValue();
					if (ruleIters > 1)
						grpStr = grpStr + "{" + ruleIters + "}";
				}
				grpStr = grpStr + " ";
			}
			if (grpItersStr.equals("*"))
				grpStr = grpStr + ")";
			else if (grpRules.size() > 1 || grpIters > 1)
				grpStr = grpStr + ")";

			if (grpRules.size() > 0) {
				if (grpItersStr.equals("*"))
					grpStr = grpStr + "{" + grpItersStr + "}";
				else if (grpIters > 1)
					grpStr = grpStr + "{" + grpIters + "}";
			}
			if (grpStr.length() > 0)
				grpStr = grpStr + "\n";

			v.add(grpStr);
		}
		return v;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void updateRuleSequencesTextList(List<String> sequenceText) {
		this.scrollpaneSequenceText.getViewport().remove(this.ruleSequenceTextList);
		this.ruleSequenceTextList.removeAll();
		if (sequenceText != null) {
			this.ruleSequenceTextList = new JList((Vector<String>)sequenceText);
			this.ruleSequenceTextList.addListSelectionListener(this);
			this.ruleSequenceTextList.getSelectionModel().setSelectionMode(
					ListSelectionModel.SINGLE_SELECTION);
		} else {
			this.ruleSequenceTextList = new JList();
		}
		this.scrollpaneSequenceText.setViewportView(this.ruleSequenceTextList);
	}

	protected String updateRuleSequencesText(
			List<Pair<List<Pair<String, String>>, String>> sequences) {
		String s = "";
		for (int i = 0; i < sequences.size(); i++) {
			Pair<List<Pair<String, String>>, String> g = sequences.get(i);
			String grpStr = "";
			List<Pair<String, String>> grpRules = g.first;
			long grpIters = -1;
			String grpItersStr = g.second;
			if (grpItersStr.equals("*"))
				grpStr = grpStr.concat("( ");
			else {
				grpIters = (new Long(g.second)).longValue();
				if (grpRules.size() > 1 || grpIters > 1)
					grpStr = grpStr.concat("( ");
			}
			for (int j = 0; j < grpRules.size(); j++) {
				Pair<String, String> p = grpRules.get(j);
				String rulename = p.first;
				grpStr = grpStr.concat(rulename);
				long ruleIters = -1;
				String ruleItersStr = p.second;
				if (ruleItersStr.equals("*")){
					grpStr = grpStr.concat("{");
					grpStr = grpStr.concat(ruleItersStr);
					grpStr = grpStr.concat("}");
				}
				else {
					ruleIters = (new Long(p.second)).longValue();
					if (ruleIters > 1) {
						grpStr = grpStr.concat("{");
						grpStr = grpStr.concat(String.valueOf(ruleIters)); 
						grpStr = grpStr.concat("}");
					}
				}
				grpStr = grpStr.concat(" ");
			}
			if (grpItersStr.equals("*"))
				grpStr = grpStr.concat(")");
			else if (grpRules.size() > 1 || grpIters > 1)
				grpStr = grpStr.concat(")");

			if (grpRules.size() > 0) {
				if (grpItersStr.equals("*")) {
					grpStr = grpStr.concat("{"); 
					grpStr = grpStr.concat(grpItersStr);
					grpStr = grpStr.concat("}");
				}
				else if (grpIters > 1) {
					grpStr = grpStr + "{" + grpIters + "}";
				}
			} else {
				grpStr = "()";
			}
			grpStr = grpStr.concat("\n");
			s = s.concat(grpStr);
		}
		return s;
	}

	public String getRuleSequencesText() {
		return updateRuleSequencesText(this.groups);
	}

	public boolean isEmpty() {
		if (this.rules == null || this.rules.isEmpty())
			return true;
		
		return false;
	}

	protected void clear() {
		updateRules(null);
		clearGroups();
	}

	protected void clearGroups() {
		this.groups = new Vector<Pair<List<Pair<String, String>>, String>>();		
		this.group = new Vector<Pair<String, String>>();
		this.groupCount = Integer.valueOf(0);
		
		this.updateGroupList();
		this.updateGroupRuleList();
		this.updateRuleSequencesTextList(null);
	}

	protected void updateGroups() {
		for (int i = 0; i < this.groupList.getRowCount(); i++) {
			this.groupList.getModel().setValueAt(String.valueOf(i + 1), i, 0);
		}
	}

	protected void updateGroupList() {
		List<List<String>> data = new Vector<List<String>>(this.groups.size());
		for (int i = 0; i < this.groups.size(); i++) {
			List<String> rd = new Vector<String>(2);
			rd.add(String.valueOf(i + 1));
			rd.add(this.groups.get(i).second);
			data.add(rd);
		}
		
		if (this.groupList != null)
			this.scrollGroupList.getViewport().remove(this.groupList);
		
		this.groupList = new JTable((Vector<List<String>>)data, (Vector<String>)this.groupListColumnNames);
		this.groupList.getModel().addTableModelListener(this);
		this.groupList.addMouseListener(this.ml);
		this.scrollGroupList.getViewport().setView(this.groupList);

		this.groupList.getSelectionModel().setSelectionMode(0);
		this.groupList.getSelectionModel().addListSelectionListener(this);
	}

	protected void updateGroupRuleList() {
		List<List<String>> data = new Vector<List<String>>(this.group.size());
		for (int i = 0; i < this.group.size(); i++) {
			Pair<String, String> p = this.group.get(i);
			Vector<String> rd = new Vector<String>(2);
			rd.add(p.first);
			rd.add(p.second);

			data.add(rd);
		}
		
		if (this.groupRuleList != null)
			this.scrollGroupRuleList.getViewport().remove(this.groupRuleList);
		
		this.groupRuleList = new JTable((Vector<List<String>>)data, (Vector<String>)this.groupRuleListColumnNames);
		this.groupRuleList.getModel().addTableModelListener(this);
		this.groupRuleList.addMouseListener(this.ml);
		this.scrollGroupRuleList.getViewport().setView(this.groupRuleList);
		this.groupRuleList.getSelectionModel().setSelectionMode(0);
	}

	protected void updateGroupRuleList(List<Pair<String, String>> aGroup) {
		List<List<String>> data = new Vector<List<String>>(aGroup.size());
		for (int i = 0; i < aGroup.size(); i++) {
			Pair<String, String> p = aGroup.get(i);
			Vector<String> rd = new Vector<String>(2);
			rd.add(p.first);
			rd.add(p.second);

			data.add(rd);
		}
		
		if (this.groupRuleList != null)
			this.scrollGroupRuleList.getViewport().remove(this.groupRuleList);
		
		this.groupRuleList = new JTable((Vector<List<String>>)data, (Vector<String>)this.groupRuleListColumnNames);
		this.groupRuleList.getModel().addTableModelListener(this);
		this.groupRuleList.addMouseListener(this.ml);
		this.scrollGroupRuleList.getViewport().setView(this.groupRuleList);
		this.groupRuleList.getSelectionModel().setSelectionMode(0);
	}
	
	public void tableChanged(TableModelEvent e) {
//		 System.out.println("TableModelEvent: "+e.getSource()+"   "+e.getFirstRow()+" "+e.getLastRow());
		// System.out.println(groupList+" "+groupRuleList);
		if (e.getSource() == this.groupList.getModel()) {
			int indx = this.groupList.getSelectedRow();
			if (indx != -1) {
				boolean ok = false;
				String iters = (String) this.groupList.getModel()
						.getValueAt(indx, 1);
				if (iters.equals("*"))
					ok = true;
				else {
					try {
//						Long nb = 
						new Long((String) this.groupList.getModel().getValueAt(indx, 1));
							ok = true;
					} catch (NumberFormatException ex) {
						this.groupList.getModel().setValueAt("1", indx, 1);
						iters = "1";
						ok = true;
					}
				}

				if (ok) {
					this.groups.get(indx).second = iters;

					updateRuleSequencesTextList(getRuleSequencesText(this.groups));
					this.gragra.getBasisGraGra().getCurrentRuleSequence().refresh();
				}
			}
		} else if (e.getSource() == this.groupRuleList.getModel()) {
			if (this.group != null && !this.group.isEmpty()) {
				int row = this.groupRuleList.getSelectedRow();
				if (row != -1) {
					boolean ok = false;
					String iters = (String) this.groupRuleList.getModel()
							.getValueAt(row, 1);
					if (iters.equals("*"))
						ok = true;
					else {
						try {
//							Long nb = 
							new Long((String) this.groupRuleList.getModel().getValueAt(row, 1));
								ok = true;
						} catch (NumberFormatException ex) {
							this.groupRuleList.getModel().setValueAt("1", row, 1);
							iters = "1";
							ok = true;
						}
					}

					if (ok) {
						int i = this.groupList.getSelectedRow();
						Pair<String, String> p = this.groups.get(i).first.get(row);
						p.second = iters;

						this.group.get(row).second = iters;

						updateRuleSequencesTextList(getRuleSequencesText(this.groups));
						this.gragra.getBasisGraGra().getCurrentRuleSequence().refresh();
					}					
				}
			}
		}
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
