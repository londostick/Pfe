/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.termination;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.ScrollPaneConstants;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JDialog;
import javax.swing.border.TitledBorder;

import agg.xt_basis.Arc;
import agg.xt_basis.GraphObject;
import agg.xt_basis.Node;
import agg.xt_basis.Rule;
import agg.termination.TerminationLGTS;
import agg.termination.TerminationLGTSInterface;
import agg.termination.TerminationLGTSTypedByTypeGraph;
import agg.util.Pair;


/**
 * Shows a table with a row for a layer and a column for a termination
 * condition, so that each element stands for rules on certain layer. The color
 * of the pairs shows the state of them (green - condition is satisfied, rot -
 * not satisfied).
 * 
 * @version $Id: LayerTerminationCondTable.java,v 1.3 2006/12/13 13:33:05 enrico
 *          Exp $
 * @author $Author: olga $
 */
@SuppressWarnings("serial")
public class LayerTerminationCondTable extends JDialog implements
		ActionListener {

	static final Color NOT_VALID = Color.red;

	static final Color VALID = Color.green;

	/** the Buttons for the entries. Hashtable[Integer, Vector[Rule]] */
	private Hashtable<Integer, Hashtable<String, JButton>> 
	buttons = new Hashtable<Integer, Hashtable<String, JButton>>();

	private TerminationLGTSInterface termination;

	/** the layer for a Button. */
	private Hashtable<JButton, Integer> firstLayers = new Hashtable<JButton, Integer>();

	/** the condition for a Button. */
	private Hashtable<JButton, String> secondConds = new Hashtable<JButton, String>();

	private JPanel panel;

	private JPanel tablePanel;

	private JScrollPane scrLayer;

//	private JScrollPane scrInfo;

	private RuleTable tableRule;

	private TypeTable tableTypeDeletion;

	private TypeTable tableTypeCreation;

	private JButton lastButton;

	private Vector<Pair<String, String>> conds;

	private int w, h;


	/**
	 * It is used in a constructor.
	 */
	static private Vector<Pair<String, String>> getConditionName() {
		Vector<Pair<String, String>> names = new Vector<Pair<String, String>>(3);
		names.add(new Pair<String, String>("Deletion_1",
				"Type Deletion Layer Condition"));
		names.add(new Pair<String, String>("Deletion_2",
				"Deletion Layer Condition"));
		names.add(new Pair<String, String>("Nondeletion",
				"Nondeletion Layer Condition"));
		return names;
	}

	/**
	 * constructs a new dialog panel for the given layers and termination
	 * conditions
	 */
	public LayerTerminationCondTable(TerminationLGTSInterface termination) {
		super(new JFrame(), false);
		
		setTitle("Termination of LGTS");
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				setVisible(false);
				dispose();
			}
		});

		this.termination = termination;

		GridBagLayout gridbag = new GridBagLayout();

		JPanel main = new JPanel(new BorderLayout());
		main.setBackground(Color.lightGray);

		this.panel = new JPanel(gridbag);

		// create the ScrollPane
		this.scrLayer = new JScrollPane();
		this.scrLayer.setBackground(Color.orange);
		this.scrLayer.setBorder(new TitledBorder(" Termination Conditions of LGTS "));

		// the head of the rows
		JPanel rowHead = new JPanel();
		rowHead.setLayout(new GridLayout(termination.getOrderedRuleLayer()
				.size(), 1));
		Enumeration<Integer> en = termination.getOrderedRuleLayer().elements();
		while (en.hasMoreElements()) {
			String text = en.nextElement().toString();
			JLabel act = new JLabel(" Layer " + text + " ");
			act.setToolTipText("");
			rowHead.add(act);
		}

		// the head of the columns
		JPanel colHead = new JPanel();
		colHead.setLayout(new GridLayout(1, 3));
		this.conds = getConditionName();
		for (int i = 0; i < this.conds.size(); i++) {
			Pair<String, String> cond = this.conds.get(i);
			String text = cond.first;
			JLabel act = new JLabel(" " + text);
			act.setToolTipText("");
			colHead.add(act);
		}

		// create the center panel with a button for each rule
		this.tablePanel = new JPanel();
		this.tablePanel.setLayout(new GridLayout(termination.getOrderedRuleLayer()
				.size(), 3));

		int i = 0;
		while (i < termination.getOrderedRuleLayer().size()) {
			int ii = 0;
			while (ii < 3) {
				JButton act = new JButton("   ");

				act.setToolTipText(this.conds.elementAt(ii).first);
				act.setMinimumSize(new Dimension(act.getHeight(), act
						.getHeight()));
				act.addActionListener(this);
				// System.out.println(termination.getOrderedRuleLayer().elementAt(i));
				// System.out.println(this.conds.elementAt(ii));
				this.addButton(termination.getOrderedRuleLayer().elementAt(i),
						this.conds.elementAt(ii).first, act);
				this.tablePanel.add(act);
				refreshView(termination.getOrderedRuleLayer().elementAt(i),
						this.conds.elementAt(ii).first, act);
				ii++;
			} 
			i++;
		} 

		// get the preferred size for the center panel
		Dimension dim = this.tablePanel.getPreferredSize();
		// System.out.println("tablePanel "+dim);
		Dimension dim1 = new Dimension();
		dim1.setSize(dim.getWidth() + 50, dim.getHeight() + 20);
		// calculate minimum/prefered size for column header
		Dimension dim2 = new Dimension();
		dim2.setSize(dim1.getWidth(), colHead.getPreferredSize().getHeight());
		colHead.setMinimumSize(dim2);
		colHead.setPreferredSize(dim2);

		// calculate minimum/preferred size for row header
		dim2 = new Dimension();
		dim2.setSize(rowHead.getPreferredSize().getWidth(), dim1.getHeight());
		rowHead.setPreferredSize(dim2);
		rowHead.setMinimumSize(dim2);

		// set the table panel to its actual size
		this.tablePanel.setPreferredSize(dim1);
		this.tablePanel.setMinimumSize(dim1);
		// construct JScrollPane
		this.scrLayer.setRowHeaderView(rowHead);
		this.scrLayer.setColumnHeaderView(colHead);
		this.scrLayer.setViewportView(this.tablePanel);
		this.scrLayer.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER,
				new JLabel(""));

		constrainBuild(this.panel, this.scrLayer, 0, 0, 1, 1, GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 1.0, 0.0, 5, 5, 5, 5);

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});
		JPanel closeP = new JPanel(new GridLayout(1, 3));
		closeP.add(new JLabel("  "));
		closeP.add(closeButton);
		closeP.add(new JLabel("  "));
		
		main.add(this.panel, BorderLayout.CENTER);
		main.add(closeP, BorderLayout.SOUTH);
		main.revalidate();

		JScrollPane scroll = new JScrollPane(main);
		scroll.setPreferredSize(new Dimension(300, 250));
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(scroll);
		validate();
		
		setLocation(200, 100);
		pack();
	}

	public void showGUI() {
		setVisible(true);
		this.w = getWidth();
	}

	/**
	 * Gets called, if a button is pressed
	 */
	synchronized public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		Integer layer = this.firstLayers.get(source);
		String second = this.secondConds.get(source);
		
		if (this.lastButton != null)
			this.lastButton.setText("");
		if ((layer != null) && (second != null)) {
			if (this.scrLayer != null)
				this.panel.remove(this.scrLayer);
			if (this.tableRule != null)
				this.panel.remove(this.tableRule);
			if (this.tableTypeDeletion != null)
				this.panel.remove(this.tableTypeDeletion);
			if (this.tableTypeCreation != null)
				this.panel.remove(this.tableTypeCreation);

			((JButton) source).setText("X");			
			this.h = (int) (this.scrLayer.getPreferredSize().getHeight());
			constrainBuild(this.panel, this.scrLayer, 0, 0, 1, 1,
					GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1.0,
					0.0, 5, 5, 5, 5);

			Vector<String> rules = getRules(layer, second);
			if (rules.size() != 0) {
				this.tableRule = new RuleTable(rules, " Rules of the Layer  "
						+ layer);
				this.tableRule.setPreferredSize(new Dimension(this.w, this.tableRule
						.getTableHeight()));
				this.h = this.h + (int) (this.tableRule.getPreferredSize().getHeight());
				constrainBuild(this.panel, this.tableRule, 0, 1, 1, 1,
						GridBagConstraints.BOTH, GridBagConstraints.CENTER,
						1.0, 0.0, 5, 5, 10, 5);
				if (second.equals("Deletion_1")) {
					Vector<String> deletionTypeTypes = getTypes(layer, "Deletion_1");
					this.tableTypeDeletion = new TypeTable(deletionTypeTypes,
							" Type - Deletion ");
					this.tableTypeDeletion.setPreferredSize(new Dimension(this.w,
							this.tableTypeDeletion.getTableHeight()));
					this.h = this.h + (int) (this.tableTypeDeletion.getPreferredSize()
									.getHeight());
					constrainBuild(this.panel, this.tableTypeDeletion, 0, 2, 1, 1,
							GridBagConstraints.BOTH, GridBagConstraints.CENTER,
							1.0, 0.0, 5, 5, 10, 5);
				} else if (second.equals("Deletion_2")) {
					Vector<String> deletionTypes = getTypes(layer, "Deletion_2");
					this.tableTypeDeletion = new TypeTable(deletionTypes,
							" Type  Deletion of the Layer  " + layer);
					this.tableTypeDeletion.setPreferredSize(new Dimension(this.w,
							this.tableTypeDeletion.getTableHeight()));
					this.h = this.h + (int) (this.tableTypeDeletion.getPreferredSize()
									.getHeight());
					constrainBuild(this.panel, this.tableTypeDeletion, 0, 2, 1, 1,
							GridBagConstraints.BOTH, GridBagConstraints.CENTER,
							1.0, 0.0, 5, 5, 10, 5);

					Vector<String> creationTypes = getCreatedTypesOnDeletionLayer(layer);
					if (creationTypes.size() != 0) {
						this.tableTypeCreation = new TypeTable(creationTypes,
								" Type  Creation of the Layer  " + layer);
						this.tableTypeCreation.setPreferredSize(new Dimension(this.w,
								this.tableTypeCreation.getTableHeight()));
						this.h = this.h + (int) (this.tableTypeCreation.getPreferredSize()
										.getHeight());
						constrainBuild(this.panel, this.tableTypeCreation, 0, 3, 1, 1,
								GridBagConstraints.BOTH,
								GridBagConstraints.CENTER, 1.0, 0.0, 5, 5, 10,
								5);
					}
				} else if (second.equals("Nondeletion")) {
					Vector<String> creationTypes = getTypes(layer, "Nondeletion");
					this.tableTypeCreation = new TypeTable(creationTypes,
							" Type  Creation of the Layer  " + layer);
					this.tableTypeCreation.setPreferredSize(new Dimension(this.w,
							this.tableTypeCreation.getTableHeight()));
					constrainBuild(this.panel, this.tableTypeCreation, 0, 2, 1, 1,
							GridBagConstraints.BOTH, GridBagConstraints.CENTER,
							1.0, 0.0, 5, 5, 10, 5);
					this.h = this.h + (int) (this.tableTypeCreation.getPreferredSize()
									.getHeight());
				}
			}
			this.h = this.h + 50;
			this.panel.setPreferredSize(new Dimension(this.w, this.h));
			this.panel.revalidate();

			setSize(this.w + 50, this.h + 80);
			validate();
			this.lastButton = (JButton) source;
		}

	}

	/**
	 * adds the button to the internal structur, so it can be adressed for
	 * relabeling.
	 */
	void addButton(Integer layer, String condName, JButton button) {
		// create buttons-Hashtable
		Hashtable<String, JButton> hash1 = this.buttons.get(layer);
		if (hash1 == null) {
			hash1 = new Hashtable<String, JButton>();
			this.buttons.put(layer, hash1);
		}
		hash1.put(condName, button);
		// save for reverse search
		this.firstLayers.put(button, layer);
		this.secondConds.put(button, condName);
	}

	/** returns the button for the given rule pair (r1,r2) */
	JButton getButton(Integer layer, String condName) {
		Hashtable<String, JButton> hash1 = this.buttons.get(layer);
		if (hash1 == null) {
			return null;
		}
		return hash1.get(condName);
	} // getButton

	/** force the panel to update all buttons */
	public void refreshView() {
		Enumeration<Integer> en1 = this.buttons.keys();
		while (en1.hasMoreElements()) {
			Integer first = en1.nextElement();
			Enumeration<String> en2 = this.buttons.get(first).keys();
			while (en2.hasMoreElements()) {
				String second = en2.nextElement();
				refreshView(first, second);
			} // while en2.hasMoreElements
		} // while en1.hasMoreElements
	} // reload

	/** renews the button for the given pair (first, second). */
	void refreshView(Integer first, String second) {
		// the button for the pair
		refreshView(first, second, getButton(first, second));

	} // refreshView

	/** renews the button for the given pair (layer, cond). */
	void refreshView(Integer layer, String cond, JButton button) {
		boolean value = getValue(layer, cond);
		if (value) {
			button.setBackground(VALID);
			button.setText(" ");
		} else {
			button.setBackground(NOT_VALID);
			button.setText(" ");
		}
	}

	private boolean getValue(Integer layer, String condName) {
		if (condName.equals("Deletion_1")) {
			Pair<Boolean, Vector<Rule>> value = this.termination
					.getResultTypeDeletion().get(layer);
			return value.first.booleanValue();
		} else if (condName.equals("Deletion_2")) {
			Pair<Boolean, Vector<Rule>> value = this.termination.getResultDeletion()
					.get(layer);
			return value.first.booleanValue();
		} else if (condName.equals("Nondeletion")) {
			Pair<Boolean, Vector<Rule>> value = this.termination
					.getResultNondeletion().get(layer);
			return value.first.booleanValue();
		} else
			return false;
	}

	@SuppressWarnings("unused")
	private Vector<String> getRules(Integer layer, String condName) {
		Vector<String> names = new Vector<String>();
		boolean result = false;
		if (condName.equals("Deletion_1")) {
			Pair<Boolean, Vector<Rule>> p = this.termination.getResultTypeDeletion()
					.get(layer);
			result = p.first.booleanValue();
		} else if (condName.equals("Deletion_2")) {
			Pair<Boolean, Vector<Rule>> p = this.termination.getResultDeletion()
					.get(layer);
			result = p.first.booleanValue();
		} else if (condName.equals("Nondeletion")) {
			Pair<Boolean, Vector<Rule>> p = this.termination.getResultNondeletion()
					.get(layer);
			result = p.first.booleanValue();
		}

		HashSet<?> rulesForLayer = this.termination
								.getInvertedRuleLayer().get(layer);
		Iterator<?> en = rulesForLayer.iterator();
		while (en.hasNext()) {
			Rule rule = (Rule) en.next();
			names.addElement(rule.getName());
		}

		return names;
	}

	private Vector<String> getTypes(Integer layer, String condName) {
		Vector<String> names = new Vector<String>();
		HashSet<?> typesForLayer = null;
		if (condName.equals("Deletion_1")) {
			if (this.termination instanceof TerminationLGTS) {
				if ((this.termination.getDeletionType() != null)
						&& this.termination.getDeletionType().containsKey(layer)) {
					Vector<agg.xt_basis.Type> types = this.termination.getDeletionType().get(layer);
					if (types != null) {
						Enumeration<agg.xt_basis.Type> en = types.elements();
						while (en.hasMoreElements()) {
							agg.xt_basis.Type t = en.nextElement();
							if (t.getStringRepr().equals(""))
								names.addElement("(unnamed)");
							else
								names.addElement(t.getStringRepr());
						}
					}
				}
			}
			else if (this.termination instanceof TerminationLGTSTypedByTypeGraph) {				
				if ((this.termination.getDeletionTypeObject() != null)
						&& this.termination.getDeletionTypeObject().containsKey(layer)) {
					Vector<GraphObject> typeObjs = this.termination.getDeletionTypeObject().get(layer);
					if (typeObjs != null) {
						Enumeration<GraphObject> en = typeObjs.elements();
						while (en.hasMoreElements()) {
							GraphObject tobj = en.nextElement();
							if (tobj.isNode()) {								
								if (tobj.getType().getStringRepr().equals(""))							
									names.add("(unnamed)");
								else
									names.add(tobj.getType().getStringRepr());
							}
							else {
								names.add(getTypeStringOfEdge((Arc) tobj)); 
							}
						}
					}
				}
			}
			return names;
		} else if (condName.equals("Deletion_2")) {
			if ((this.termination.getInvertedTypeDeletionLayer() != null)
						&& this.termination.getInvertedTypeDeletionLayer().containsKey(layer))
				typesForLayer = this.termination.getInvertedTypeDeletionLayer().get(layer);
		} else if (condName.equals("Nondeletion")) {
			if ((this.termination.getInvertedTypeCreationLayer() != null)
					&& this.termination.getInvertedTypeCreationLayer().containsKey(layer))
				typesForLayer = this.termination.getInvertedTypeCreationLayer().get(layer);
		}

		if (typesForLayer != null) {
			Iterator<?> en = typesForLayer.iterator();
			while (en.hasNext()) {
				final Object tobj = en.next();
				if (tobj instanceof agg.xt_basis.Type) {
					final agg.xt_basis.Type t = (agg.xt_basis.Type) tobj;
					if (t.getStringRepr().equals(""))
						names.addElement("(unnamed)");
					else
						names.addElement(t.getStringRepr());
				}
				else if (tobj instanceof Node) {					
					names.add(this.getTypeStringOfNode((Node) tobj));			
				}
				else if (tobj instanceof Arc) {
					names.add(getTypeStringOfEdge((Arc) tobj));
				}
			}
		}
		
		return names;
	}

	private Vector<String> getCreatedTypesOnDeletionLayer(Integer layer) {
		final Vector<String> names = new Vector<String>();
		final Vector<Object> types = this.termination.getCreatedTypesOnDeletionLayer(layer);
		final Enumeration<Object> en = types.elements();
		while (en.hasMoreElements()) {
			final Object tobj = en.nextElement();
			if (tobj instanceof agg.xt_basis.Type) {
				final agg.xt_basis.Type t = (agg.xt_basis.Type) tobj;
				if (t.getStringRepr().equals(""))
					names.addElement("(unnamed)");
				else
					names.addElement(t.getStringRepr());
			}
			else if (tobj instanceof Node) {					
				names.add(this.getTypeStringOfNode((Node) tobj));			
			}
			else if (tobj instanceof Arc) {
				names.add(getTypeStringOfEdge((Arc) tobj));
			}
		}
		return names;
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

	private String getTypeStringOfNode(final Node go) {
		String s = go.getType().getStringRepr();
		if (s.equals(""))
			s = "(unnamed)";
		return s;
	}
	
	private String getTypeStringOfEdge(final Arc go) {
		String s = getTypeStringOfNode((Node) go.getSource());
		s = s.concat("--");

		String s1 = go.getType().getStringRepr();
		if (s1.equals(""))
			s1 = "(unnamed)";
		s = s.concat(s1);
		
		s = s.concat("->");
		s = s.concat(getTypeStringOfNode((Node) go.getTarget()));
		return s;
	}
	
}
