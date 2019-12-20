/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
// $Id: GraTraMatchOptionGUI.java,v 1.8 2010/10/07 20:08:03 olga Exp $

package agg.gui.options;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionListener;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
//import javax.swing.border.Border;

import agg.gui.icons.CompletionIcon;
import agg.gui.trafo.GraGraTransform;
import agg.xt_basis.CompletionStrategySelector;
import agg.xt_basis.GraTraOptions;
import agg.xt_basis.MorphCompletionStrategy;
import agg.xt_basis.csp.CompletionPropertyBits;

@SuppressWarnings("serial")
public class GraTraMatchOptionGUI extends AbstractOptionGUI // implements
// ActionListener
{

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public GraTraMatchOptionGUI(GraGraTransform trans) {
		super();
		this.transform = trans;

		GridBagLayout gridbag = new GridBagLayout();
		setLayout(gridbag);
//		GridBagConstraints c = new GridBagConstraints();		
//		Border border = new TitledBorder("");
		// Border border = BorderFactory.createEtchedBorder();

		// create panel for strategy
		JPanel stratPanel = new JPanel();
		stratPanel.setLayout(new GridLayout(0, 1));
		JLabel stratLabel = new JLabel("Match completion strategy");
		CompletionIcon icon = new CompletionIcon(Color.black);
		icon.setEnabled(true);
		stratLabel.setIcon(icon);
		stratPanel.add(stratLabel);

		Enumeration<MorphCompletionStrategy> strategies = CompletionStrategySelector.getStrategies();

		while (strategies.hasMoreElements()) {
			MorphCompletionStrategy mcs = strategies.nextElement();
			this.strategyNames.addElement(CompletionStrategySelector.getName(mcs));
		}

		if (this.strategy == null) {
			/* default ist CSP strategy */
			this.strategy = CompletionStrategySelector.getDefault();
			// strategy.showProperties();
		}

		this.strategyComboBox = new JComboBox(this.strategyNames);
		this.strategyComboBox.setSelectedItem(CompletionStrategySelector
				.getName(this.strategy));
		this.strategyComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					String stratName = (String) e.getItem();
					Enumeration<MorphCompletionStrategy> strats = CompletionStrategySelector
							.getStrategies();
					while (strats.hasMoreElements()) {
						MorphCompletionStrategy mcs = strats.nextElement();
						String name = CompletionStrategySelector.getName(mcs);
						if (stratName.equals(name)) {
							GraTraMatchOptionGUI.this.strategy = mcs;
							setStrategyProperties(GraTraMatchOptionGUI.this.strategy);
							GraTraMatchOptionGUI.this.transform.setCompletionStrategy(GraTraMatchOptionGUI.this.strategy);
						}
					}
				}
			}
		});

		stratPanel.add(this.strategyComboBox);

		// create panel for match properties
		JPanel matchPanel = new JPanel(new BorderLayout());
		matchPanel.setBorder(new TitledBorder("  Match conditions  "));
		
		JPanel matchP = new JPanel(new GridLayout(1, 0));
		JPanel matchP1 = new JPanel(new GridLayout(0, 1));
		JPanel matchP2 = new JPanel(new GridLayout(0, 1));
		matchP.add(matchP1);
		matchP.add(matchP2);
		
		this.supportbits = this.strategy.getSupportedProperties();
		this.activebits = this.strategy.getProperties();
		for (int i = 0; i < CompletionPropertyBits.BITNAME.length; i++) {
			this.bitName = CompletionPropertyBits.BITNAME[i];
			this.bitNames.addElement(this.bitName);
			JCheckBox cb = new JCheckBox(this.bitName, null, true);
			if (cb.getText().equals(GraTraOptions.INJECTIVE)) {
				this.injCB = cb;
				matchP1.add(cb);
			}
			else if (cb.getText().equals(GraTraOptions.DANGLING)) {
				this.dangCB = cb;
				matchP1.add(cb);
			}
			else if (cb.getText().equals(GraTraOptions.IDENTIFICATION)) {
				this.identCB = cb;
				matchP1.add(cb);
			}
			else if (cb.getText().equals(GraTraOptions.NACS)) {
				cb.setToolTipText("Negative Application Conditions");
				matchP2.add(cb);
			}
			else if (cb.getText().equals(GraTraOptions.PACS)) {
				cb.setToolTipText("Positive Application Conditions");
				matchP2.add(cb);
			}
			else if (cb.getText().equals(GraTraOptions.GACS)) {
				cb.setToolTipText("General Application Conditions");
				matchP2.add(cb);
			}
			cb.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					for (int j = 0; j < GraTraMatchOptionGUI.this.checkboxBitNames.size(); j++) {
						JCheckBox elem = GraTraMatchOptionGUI.this.checkboxBitNames.elementAt(j);
						if (e.getSource() == elem) {
							if (elem.isSelected()) {
								GraTraMatchOptionGUI.this.activebits.set(j);
								if (elem == GraTraMatchOptionGUI.this.injCB)
									GraTraMatchOptionGUI.this.identCB.setEnabled(false);
							} else {
								GraTraMatchOptionGUI.this.activebits.clear(j);
								if (elem == GraTraMatchOptionGUI.this.injCB) {
									GraTraMatchOptionGUI.this.identCB.setEnabled(true);
								}
							}
							GraTraMatchOptionGUI.this.transform.updateGraTraOption(elem.getText(), elem.isSelected());
						}
						fireOptionEvent(new agg.gui.parser.event.OptionEvent(
								elem));
					}
				}
			});
			this.checkboxBitNames.addElement(cb);
			this.checkboxBitNames.lastElement().setEnabled(this.supportbits.get(i));
			this.checkboxBitNames.lastElement().setSelected(this.activebits.get(i));
		}
		// add check box randomized CSP search domain
		JPanel matchP3 = new JPanel(new GridLayout(0, 1));
		matchP3.add(new JLabel(" ---------------------------"));
		randomCSPDomain = new JCheckBox("Randomized CSP search domain", null, true);
		randomCSPDomain.setSelected(this.strategy.isRandomisedDomain());
		randomCSPDomain.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				GraTraMatchOptionGUI.this.strategy.setRandomisedDomain(randomCSPDomain.isSelected());
				GraTraMatchOptionGUI.this.transform.updateGraTraOption(GraTraOptions.DETERMINED_CSP_DOMAIN, !randomCSPDomain.isSelected());
			}
		});
		matchP3.add(randomCSPDomain);
		matchPanel.add(matchP, BorderLayout.CENTER);
		matchPanel.add(matchP3, BorderLayout.SOUTH);
		
		JPanel consistencyPanel = new JPanel();
		consistencyPanel.setBorder(new TitledBorder(
				"  Consistency check during transformation  "));
		consistencyPanel.setLayout(new GridLayout(0, 1));
		ButtonGroup group = new ButtonGroup();
		this.consistencyRB1 = new JRadioButton("consistent transformations only");
		group.add(this.consistencyRB1);
		consistencyPanel.add(this.consistencyRB1);
		this.consistencyRB1.setActionCommand("consistentOnly");
		this.consistencyRB1.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (GraTraMatchOptionGUI.this.consistencyRB1.isSelected()) {
					GraTraMatchOptionGUI.this.consistency = true;
					GraTraMatchOptionGUI.this.consistencyCheckAfterGraphTrafoCB.setSelected(false);
					GraTraMatchOptionGUI.this.consistencyCheckAfterGraphTrafo = false;
				}
				GraTraMatchOptionGUI.this.transform.updateGraTraOption(GraTraOptions.CONSISTENT_ONLY, GraTraMatchOptionGUI.this.consistency);
			}
		});
		this.consistencyRB2 = new JRadioButton(
				"stop after inconsistent transformation");
		group.add(this.consistencyRB2);
		consistencyPanel.add(this.consistencyRB2);
		this.consistencyRB2.setActionCommand("inconsistentStop");
		this.consistencyRB2.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (GraTraMatchOptionGUI.this.consistencyRB2.isSelected()) {
					GraTraMatchOptionGUI.this.consistency = false;
					GraTraMatchOptionGUI.this.consistencyCheckAfterGraphTrafoCB.setSelected(false);
					GraTraMatchOptionGUI.this.consistencyCheckAfterGraphTrafo = false;
				}
				GraTraMatchOptionGUI.this.transform.updateGraTraOption(GraTraOptions.CONSISTENT_ONLY, GraTraMatchOptionGUI.this.consistency);
			}
		});
		
		consistencyPanel.add(new JLabel(" Consistency check at the end of ( layer ) graph trafo"));
		
		this.consistencyCheckAfterGraphTrafoCB = new JCheckBox("consistent at the end", null, false);
		consistencyPanel.add(this.consistencyCheckAfterGraphTrafoCB);
		this.consistencyCheckAfterGraphTrafo = false;
		this.consistencyCheckAfterGraphTrafoCB.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				GraTraMatchOptionGUI.this.consistencyCheckAfterGraphTrafo = GraTraMatchOptionGUI.this.consistencyCheckAfterGraphTrafoCB.isSelected();
				GraTraMatchOptionGUI.this.consistencyRB1.setEnabled(!GraTraMatchOptionGUI.this.consistencyCheckAfterGraphTrafo);
				GraTraMatchOptionGUI.this.consistencyRB2.setEnabled(!GraTraMatchOptionGUI.this.consistencyCheckAfterGraphTrafo);
				GraTraMatchOptionGUI.this.transform.updateGraTraOption(GraTraOptions.CONSISTENCY_CHECK_AFTER_GRAPH_TRAFO, GraTraMatchOptionGUI.this.consistencyCheckAfterGraphTrafo);
			}
		});
		
//		JLabel consistencyLabel1 = new JLabel(
//				"  ( If a grammar doesn't contain any graph constraints,");
//		consistencyPanel.add(consistencyLabel1);
//		JLabel consistencyLabel2 = new JLabel(
//				"    the consistency option has no effect. )");
//		consistencyPanel.add(consistencyLabel2);

		// rule applicability
		JPanel ruleApplPanel = new JPanel();
		ruleApplPanel.setBorder(new TitledBorder(
				"  Rule applicability on the host graph  "));
		ruleApplPanel.setLayout(new GridLayout(0, 1));
		this.checkRuleApplCB = new JCheckBox("check", null, false);
		ruleApplPanel.add(this.checkRuleApplCB);
		this.checkRuleAppl = false;
		this.checkRuleApplCB.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				GraTraMatchOptionGUI.this.checkRuleAppl = GraTraMatchOptionGUI.this.checkRuleApplCB.isSelected();
				GraTraMatchOptionGUI.this.transform.updateGraTraOption(GraTraOptions.CHECK_RULE_APPLICABILITY,
						GraTraMatchOptionGUI.this.checkRuleAppl);
			}
		});

		// create panel for display settings
		JPanel displayPanel = new JPanel(new GridLayout(0, 1));
		displayPanel.setBorder(new TitledBorder("  Graph display settings  "));
		
		this.selectMatchCB = new JCheckBox("select objects of match",
				null, false);
		displayPanel.add(this.selectMatchCB);
		this.selectMatch = false;
		this.selectMatchCB.setActionCommand("selectMatch");
		this.selectMatchCB.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				GraTraMatchOptionGUI.this.selectMatch = GraTraMatchOptionGUI.this.selectMatchCB.isSelected();
			}
		});
		
		this.showGraphAfterStepCB = new JCheckBox("show after step", null, true);
		displayPanel.add(this.showGraphAfterStepCB);
		this.showGraphAfterStep = true;
		this.showGraphAfterStepCB.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				GraTraMatchOptionGUI.this.showGraphAfterStep = GraTraMatchOptionGUI.this.showGraphAfterStepCB.isSelected();				
			}
		});
		this.waitAfterStepCB = new JCheckBox("wait after step", null, false);
		displayPanel.add(this.waitAfterStepCB);
		this.waitAfterStepCB.setActionCommand("waitAfterStep");
		this.waitAfterStep = false;
		this.waitAfterStepCB.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				GraTraMatchOptionGUI.this.waitAfterStep = GraTraMatchOptionGUI.this.waitAfterStepCB.isSelected();	
			}
		});

		this.selectNewAfterStepCB = new JCheckBox("select new objects after step",
				null, false);
		displayPanel.add(this.selectNewAfterStepCB);
		this.selectNewAfterStep = false;
		this.selectNewAfterStepCB.setActionCommand("selectNewAfterStep");
		this.selectNewAfterStepCB.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				GraTraMatchOptionGUI.this.selectNewAfterStep = GraTraMatchOptionGUI.this.selectNewAfterStepCB.isSelected();
			}
		});

		constrainBuild(this, stratPanel, 0, 0, 1, 1, GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 1.0, 0.0, 5, 5, 5, 5);
		constrainBuild(this, matchPanel, 0, 1, 1, 1, GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 1.0, 0.0, 5, 5, 5, 5);
		constrainBuild(this, consistencyPanel, 0, 2, 1, 1,
				GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1.0, 0.0,
				5, 5, 5, 5);
		constrainBuild(this, ruleApplPanel, 0, 3, 1, 1,
				GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1.0, 0.0,
				5, 5, 5, 5);
		constrainBuild(this, displayPanel, 0, 4, 1, 1, GridBagConstraints.BOTH,
				GridBagConstraints.CENTER, 1.0, 0.0, 5, 5, 5, 5);
		validate();
	}

	public Dimension getPreferredSize() {
		return new Dimension(400, 690);
	}

	/**
	 * Returns the text for the tab title.
	 * 
	 * @return <I>Transformation</I> is returned.
	 */
	public String getTabTitle() {
		return "General";
	}

	/**
	 * Returns the text for the tab tip.
	 * 
	 * @return <I>Transformation Option</I> is returned.
	 */
	public String getTabTip() {
		return "General Transformation Options";
	}

	/**
	 * Updates options setting.
	 */
	public void update() {
		update(this.transform.getGraTraOptionsList());
	}

	/**
	 * Updates GUI of transformation options .
	 */
	public void update(Vector<String> optionNames) {
//		System.out.println("GraTraMatchOptionGUI.update(Vector):: " +optionNames);
		if (optionNames.isEmpty())
			return;
		
		// set strategy
		if (optionNames.contains("CSP"))
			this.strategyComboBox.setSelectedItem("CSP");
		else if (optionNames.contains("CSP w/o BJ"))
			this.strategyComboBox.setSelectedItem("CSP w/o BJ");
		
		String stratName = (String) this.strategyComboBox.getSelectedItem();
		Enumeration<MorphCompletionStrategy> strats = CompletionStrategySelector.getStrategies();
		while (strats.hasMoreElements()) {
			MorphCompletionStrategy mcs = strats.nextElement();
			String name = CompletionStrategySelector.getName(mcs);
			if (stratName.equals(name)) {
				this.strategy = mcs;
				break;
			}
		}
		
		if (this.strategy == null)
			this.strategy = CompletionStrategySelector.getDefault();
		
		// set match conditions
		this.supportbits = this.strategy.getSupportedProperties();
		this.activebits = this.strategy.getProperties();
		
		for (int j = 0; j < this.checkboxBitNames.size(); j++) {
			JCheckBox elem = this.checkboxBitNames.elementAt(j);
			elem.setEnabled(this.supportbits.get(j));

			if (elem.isEnabled()) {
				if (elem.getText().equals(GraTraOptions.INJECTIVE)) {
					if (optionNames.contains(GraTraOptions.INJECTIVE)) {
						elem.setSelected(true);
						this.activebits.set(j);
					} else {
						elem.setSelected(false);
						this.activebits.clear(j);
					}
				} else if (elem.getText().equals(GraTraOptions.DANGLING)) {
					if (optionNames.contains(GraTraOptions.DANGLING)) {
						elem.setSelected(true);
						this.activebits.set(j);
					} else {
						elem.setSelected(false);
						this.activebits.clear(j);
					}
				} else if (elem.getText().equals(GraTraOptions.IDENTIFICATION)) {
					if (optionNames.contains(GraTraOptions.IDENTIFICATION)) {
						elem.setSelected(true);
						this.activebits.set(j);
					} else {
						elem.setSelected(false);
						this.activebits.clear(j);
					}
				} else if (elem.getText().equals(GraTraOptions.NACS)) {
					if (optionNames.contains(GraTraOptions.NACS)) {
						elem.setSelected(true);
						this.activebits.set(j);
					} else {
						elem.setSelected(false);
						this.activebits.clear(j);
					}
				} else if (elem.getText().equals(GraTraOptions.PACS)) {
					if (optionNames.contains(GraTraOptions.PACS)) {
						elem.setSelected(true);
						this.activebits.set(j);
					} else {
						elem.setSelected(false);
						this.activebits.clear(j);
					}
				} else if (elem.getText().equals(GraTraOptions.GACS)) {
					if (optionNames.contains(GraTraOptions.GACS)) {
						elem.setSelected(true);
						this.activebits.set(j);
					} else {
						elem.setSelected(false);
						this.activebits.clear(j);
					}
				}
				
				fireOptionEvent(new agg.gui.parser.event.OptionEvent(elem));
			}
		}
		if (this.injCB.isSelected())
			this.identCB.setEnabled(false);
		else
			this.identCB.setEnabled(true);
		
		if (optionNames.contains(GraTraOptions.DETERMINED_CSP_DOMAIN)) {
			this.randomCSPDomain.setSelected(false);
			this.strategy.setRandomisedDomain(false);
		} else {
			this.randomCSPDomain.setSelected(true);
			this.strategy.setRandomisedDomain(true);
		}
	
		// set consistency
		if (optionNames.contains(GraTraOptions.CONSISTENT_ONLY)) {
			this.consistencyRB1.setSelected(true);
			this.consistency = true;
		} else {
			this.consistencyRB2.setSelected(true);
			this.consistency = false;
		}
		
		if (optionNames.contains(GraTraOptions.CONSISTENCY_CHECK_AFTER_GRAPH_TRAFO)) {
			this.consistencyCheckAfterGraphTrafoCB.setSelected(true);
			this.consistencyCheckAfterGraphTrafo = true;
			this.consistencyRB1.setEnabled(false);
			this.consistencyRB2.setEnabled(false);
		} else {
			this.consistencyCheckAfterGraphTrafoCB.setSelected(false);
			this.consistencyCheckAfterGraphTrafo = false;
			this.consistencyRB1.setEnabled(true);
			this.consistencyRB2.setEnabled(true);
		}
		
		if (optionNames.contains(GraTraOptions.SELECT_NEW_AFTER_STEP)) {
			this.selectNewAfterStep = true;
			this.selectNewAfterStepCB.setSelected(true);
		}
		
		if (optionNames.contains(GraTraOptions.WAIT_AFTER_STEP)) {
			this.waitAfterStep = true;
			this.waitAfterStepCB.setSelected(true);
		}
	}


	/** Gets the morphism completion strategy */
	public MorphCompletionStrategy getMorphCompletionStrategy() {
		return this.strategy;
	}

	/**
	 * Returns TRUE if the option -consistency check during transformation- is
	 * set.
	 */
	public boolean consistencyEnabled() {
		return this.consistency;
	}

	public boolean consistencyCheckAfterGraphTrafoEnabled() {
		return this.consistencyCheckAfterGraphTrafo;
	}
	
	public boolean selectMatchObjectsEnabled() {
		return this.selectMatch;
	}
	
	/** Returns TRUE if the option -show graph after step- is set. */
	public boolean showGraphAfterStepEnabled() {
		return this.showGraphAfterStep;
	}

	/** Returns TRUE if the option -wait after step- is set. */
	public boolean waitAfterStepEnabled() {
		return this.waitAfterStep;
	}

	/**
	 * Returns TRUE if the option -check rule applicability on current graph- is
	 * set.
	 */
	public boolean checkRuleApplicabilityEnabled() {
		return this.checkRuleAppl;
	}

	/** Returns TRUE if the option -apply parallel- is set. */
	public boolean applyParallelEnabled() {
		return this.parallelMatching;
	}


	/** Returns TRUE if the option -select new after step- is set. */
	public boolean selectNewAfterStepEnabled() {
		return this.selectNewAfterStep;
	}

	public void addActionListener(String option, ActionListener l) {
		if (option.equals(GraTraOptions.WAIT_AFTER_STEP))
			this.waitAfterStepCB.addActionListener(l);
	}

	void setStrategyProperties(MorphCompletionStrategy s) {
		this.supportbits = s.getSupportedProperties();
		this.activebits = s.getProperties();
		for (int i = 0; i < CompletionPropertyBits.BITNAME.length; i++) {
			this.checkboxBitNames.elementAt(i).setSelected(this.activebits.get(i));
			this.checkboxBitNames.elementAt(i).setEnabled(this.supportbits.get(i));
		}
		this.identCB.setEnabled(!this.injCB.isSelected());
//		s.showProperties();
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

	public void executeOnClose() {}
	

	GraGraTransform transform;

	MorphCompletionStrategy strategy;

	@SuppressWarnings("rawtypes")
	JComboBox strategyComboBox;

	JCheckBox injCB, identCB, dangCB,
			//PACsCB, NACsCB, 
			checkRuleApplCB, parallelMatchingCB, 
			selectMatchCB, showGraphAfterStepCB,
			waitAfterStepCB, selectNewAfterStepCB,
			consistencyCheckAfterGraphTrafoCB,
			randomCSPDomain;

	JRadioButton consistencyRB1, consistencyRB2;

	Vector<String> strategyNames = new Vector<String>(3);

	Vector<String> bitNames = new Vector<String>(3);

	String bitName = "";

	Vector<JCheckBox> checkboxBitNames = new Vector<JCheckBox>(3);

	BitSet supportbits;

	BitSet activebits;

	JPanel mainPanel;

	boolean consistency, showGraphAfterStep, waitAfterStep, 
			selectMatch, selectNewAfterStep,
			checkRuleAppl, parallelMatching,
			consistencyCheckAfterGraphTrafo;

}
