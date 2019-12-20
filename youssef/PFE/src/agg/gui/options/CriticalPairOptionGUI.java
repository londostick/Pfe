/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.options;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Dictionary;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import agg.gui.IconResource;
import agg.gui.cpa.CriticalPairAnalysis;
import agg.parser.CriticalPairOption;
import agg.parser.OptionEventListener;
import agg.parser.ParserOption;
import agg.parser.ExcludePairContainer;


/**
 * This is the panel for the option of the critical pair analysis. This option
 * sets the algorithm and the some display settings for the critical pair
 * analysis.
 * 
 * @version $Id: CriticalPairOptionGUI.java,v 1.13 2010/11/16 23:32:19 olga Exp $
 * @author $Author: olga $
 */
@SuppressWarnings("serial")
public class CriticalPairOptionGUI extends AbstractOptionGUI implements
		ItemListener, ActionListener, ChangeListener, OptionEventListener {


	private CriticalPairOption cpOption;

	CriticalPairAnalysis cpa;
	
	JButton displaySwitch;

	JButton generalSwitch;

	private static final int MAX = 20;

	JSlider numberCriticalPairs;

	JInternalFrame virtualGraph;

	JSlider verticalSize;

	JSlider horizontalSize;

	@SuppressWarnings("rawtypes")
	JComboBox algorithms;
	
//	JMenu algorithms;

	@SuppressWarnings("rawtypes")
	JComboBox layers;

	JCheckBox layered, complete, reduce, consistent, 
				attrCheck, equalVariableNameOfAttrMapping,
				ignoreIdentical, reduceSameMatch,
				directStrctCnfl, directStrctCnflUpToIso,
				criticalStyleGreen, criticalStyleBlackBold,
				namedObject;
	
	JTextField maxBoundOfCriticKind;
	
	JButton moreAboutConsist;
	Color bgc;
	
	JPanel firstPriorityOption;

	JPanel secondPriorityOption;

	ParserGUIOption guiOption;

	ParserOption pOption;

	CriticalPairOptionGUI dialog;
	
	/**
	 * Creates a new gui for the options.
	 * 
	 * @param cpOption
	 *            The algorithm options
	 * @param guiOption
	 *            Options for the display settings.
	 * @param pOption
	 *            Parser option for synchronization
	 */
	public CriticalPairOptionGUI(CriticalPairAnalysis cpa, CriticalPairOption cpOption,
			ParserGUIOption guiOption, ParserOption pOption) {
		super();
		
		this.cpa = cpa;
		this.cpOption = cpOption;
		this.guiOption = guiOption;
		this.pOption = pOption;
		this.firstPriorityOption = makeFirstPriorityOption();
		this.secondPriorityOption = makeSecondPriorityOption();

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		setLayout(gridbag);

		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weighty = 1.0;
		c.weightx = 1.0;
		add(this.firstPriorityOption, c);
		validate();
		
		this.dialog = this;
	}

	public Dimension getPreferredSize() {
		return new Dimension(400, 690);
	}

	/**
	 * Sets the GUI options for display settings.
	 * 
	 * @param pguiOption
	 *            The GUI options for display settings.
	 */
	public void setGUIOption(ParserGUIOption pguiOption) {
		this.guiOption = pguiOption;
	}

	public void setParserOption(ParserOption pOption) {
		this.pOption = pOption;
	}

	private JPanel makeFirstPriorityOption() {
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.NORTHWEST;
		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
//		optionPanel.setBorder(new TitledBorder("          General Settings    "));

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.insets = new Insets(5, 0, 5, 0);
		c.weightx = 1.0;
		c.weighty = 0.0;
		
		JPanel algorithmP = makeCriticalPairAlgorithm();
		optionPanel.add(algorithmP, c);

		JPanel completeP = makeComplete();
		optionPanel.add(completeP, c);

		JPanel consistentP = makeConsistent();
		optionPanel.add(consistentP, c);

		JPanel attrCheckP = makeAttrCheck();
		optionPanel.add(attrCheckP, c);
		
		JPanel ignoreIdenticalRulesP = makeIgnoreCriticalPairs();
		optionPanel.add(ignoreIdenticalRulesP, c);

		JPanel namedObjP = makeCriticalPairsByNamedObject();
		optionPanel.add(namedObjP, c);

		JPanel maxBoundP = this.makeMaxBoundOfCriticKind();
		optionPanel.add(maxBoundP, c);
		
		JPanel reduceP = makeEssential();
		optionPanel.add(reduceP, c);
		
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.gridwidth = GridBagConstraints.RELATIVE;
		JPanel placeHolder2 = new JPanel();
//		 placeHolder2.setBackground(java.awt.Color.yellow);
		placeHolder2.setPreferredSize(new Dimension(200, 20));
		optionPanel.add(placeHolder2, c);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0.0;
		c.insets = new Insets(10, 0, 10, 20);
		this.displaySwitch = new JButton(ParserOptionGUI.DISPLAYSETTINGS);
		this.displaySwitch.addActionListener(this);
		optionPanel.add(this.displaySwitch, c);

		return optionPanel;
	}

	private JPanel makeSecondPriorityOption() {
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.NORTHWEST;
		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
//		optionPanel.setBorder(new TitledBorder(" Display Settings "));

		c.weightx = 1.0;
		c.weighty = 0.01;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.insets = new Insets(5, 0, 5, 0);

		c.weighty = 0.0;
		JPanel pairOption = makeCriticalPairDisplay();
		optionPanel.add(pairOption, c);

		c.weighty = 1.0;
		JPanel pairSize = makePairSize();
		optionPanel.add(pairSize, c);


		c.weighty = 0.0;
		JPanel criticalObjStyle = makeCriticalDrawingStyle();
		optionPanel.add(criticalObjStyle, c);
		
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.gridwidth = GridBagConstraints.RELATIVE;
		JPanel placeHolder2 = new JPanel();
		// placeHolder2.setBackground(java.awt.Color.yellow);
		placeHolder2.setPreferredSize(new Dimension(200, 20));
		optionPanel.add(placeHolder2, c);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0.0;
		c.insets = new Insets(5, 0, 5, 20);
		this.generalSwitch = new JButton(ParserOptionGUI.GENERALSETTINGS);
		this.generalSwitch.addActionListener(this);
		optionPanel.add(this.generalSwitch, c);

		return optionPanel;
	}

	private JPanel makeCriticalPairDisplay() {
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.weightx = 1.0;

		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
		optionPanel.setBorder(new TitledBorder(" Number of displayed critical pairs "));
		c.anchor = GridBagConstraints.WEST;
		JSlider slider = new JSlider(SwingConstants.HORIZONTAL,
				ParserGUIOption.SHOWNOPAIRS, MAX, 5);
		optionPanel.add(slider, c);

		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setPaintTrack(true);
		slider.setMajorTickSpacing(slider.getMinorTickSpacing() * 5);
		slider.setSnapToTicks(true);

		Dictionary<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		for (int i = slider.getMinimum(); i < slider.getMaximum(); i++) {
			if (i % 5 == 0)
				labelTable.put(Integer.valueOf(i), new JLabel("" + i));
		}

		JLabel all = new JLabel("All");
		labelTable.put(Integer.valueOf(slider.getMaximum()), all);
		slider.setLabelTable(labelTable);

		JLabel invisible = new JLabel("None");
		labelTable.put(Integer.valueOf(slider.getMinimum()), invisible);
		slider.setLabelTable(labelTable);
		this.numberCriticalPairs = slider;
		this.numberCriticalPairs.addChangeListener(this);

		return optionPanel;
	}

	private JPanel makeCriticalDrawingStyle() {
		final JPanel optionPanel = makeInitialOptionPanel("");
		optionPanel.setBorder(new TitledBorder(" Set style how to draw critical objects "));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		final JPanel p = new JPanel(new GridLayout(2,0));
		this.criticalStyleGreen = new JCheckBox("bold green", true);
		this.criticalStyleGreen.addActionListener(this);
		
		this.criticalStyleBlackBold = new JCheckBox("bold black", false);
		this.criticalStyleBlackBold.addActionListener(this);
		
		final ButtonGroup group = new ButtonGroup();
		group.add(this.criticalStyleGreen);
		group.add(this.criticalStyleBlackBold);
		
		p.add(this.criticalStyleGreen);
		p.add(this.criticalStyleBlackBold);
		
		optionPanel.add(p, c);
		
		return optionPanel;
	}
	
	private JPanel makePairSize() {
		JPanel optionPanel = makeInitialOptionPanel("");
		optionPanel.setBorder(new TitledBorder(" Set initial critical pair window size "));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		JDesktopPane virtualDesktop = new JDesktopPane();
		this.virtualGraph = new JInternalFrame("Virtual Overlapping Graph", false,
				false, false, false);
		this.virtualGraph.setVisible(true);
		ImageIcon internalFrameIcon = IconResource.getIconFromURL(IconResource
				.getURLOverlapGraph());
		this.virtualGraph.setFrameIcon(internalFrameIcon);
		virtualDesktop.add(this.virtualGraph);
		try {
			this.virtualGraph.setSelected(true);
		} catch (java.beans.PropertyVetoException pve) {
		}

		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1;
		c.weighty = 1.0;
		c.weightx = 1.0;
		optionPanel.add(virtualDesktop, c);

		this.verticalSize = new JSlider(SwingConstants.VERTICAL, 80, 500, 200);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.weightx = 0.0;
		this.verticalSize.addChangeListener(this);
		this.verticalSize.setInverted(true);
		optionPanel.add(this.verticalSize, c);

		this.horizontalSize = new JSlider(SwingConstants.HORIZONTAL, 120, 800, 200);
		c.weightx = 1.0;
		c.weighty = 0.0;
		this.horizontalSize.addChangeListener(this);
		optionPanel.add(this.horizontalSize, c);

		this.virtualGraph.setSize(this.horizontalSize.getValue() / 2, this.verticalSize
				.getValue() / 2);
		optionPanel.add(new JLabel("Scale: 1:2"), c);

		return optionPanel;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JPanel makeCriticalPairAlgorithm() {
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;

		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
		optionPanel.setBorder(new TitledBorder(" Select the kind of critical pairs   &   layer to compute "));

//		c.gridwidth = GridBagConstraints.REMAINDER;
//		c.weightx = 1.0;
//		optionPanel.add(new JPanel(), c);
		
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;	
		this.algorithms = new JComboBox();
		this.algorithms.addItemListener(this);
		optionPanel.add(this.algorithms, c);
		
		this.algorithms.addItem(ParserOptionGUI.EXCLUDEONLY);
		this.algorithms.addItem(ParserOptionGUI.TRIGGER_DEPEND);
		this.algorithms.addItem(ParserOptionGUI.TRIGGER_SWITCH_DEPEND);		
						
		c.insets = new Insets(0, 10, 0, 0);
		this.layered = new JCheckBox("layered", false);
		this.layered.addActionListener(this);
		optionPanel.add(this.layered, c);

		c.insets = new Insets(0, 10, 0, 0);
		this.layers = new JComboBox();
		this.layers.addActionListener(this);
		this.layers.addItem("All");
		this.layers.setEnabled(false);
		optionPanel.add(this.layers, c);

		c.insets = new Insets(0, 5, 0, 0);
		JLabel lLayers = new JLabel("Layer");
		optionPanel.add(lLayers, c);
		
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		optionPanel.add(new JPanel(), c);
		return optionPanel;
	}

	private JPanel makeComplete() {
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;

		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
		optionPanel.setBorder(new TitledBorder(" Select completeness of critical pairs "));

//		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;

		c.insets = new Insets(0, 0, 0, 20);
		this.complete = new JCheckBox("complete", this.cpOption.completeEnabled());
		this.complete.addActionListener(this);
		optionPanel.add(this.complete, c);

		JButton moreAbout = new JButton("More about");
		moreAbout.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(
						CriticalPairOptionGUI.this.dialog,
						"<HTML><BODY>"
						+"If not selected, search up to first critical match."
						+"</BODY></HTML>",
						"  complete  ", javax.swing.JOptionPane.INFORMATION_MESSAGE);
			}
		});
		optionPanel.add(moreAbout);
		return optionPanel;
	}

	private JPanel makeEssential() {
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;

		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
		optionPanel.setBorder(new TitledBorder(" Compute essential critical pairs "));

//		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;

		c.insets = new Insets(0, 0, 0, 20);
		this.reduce = new JCheckBox(
				"essential", this.cpOption.reduceEnabled());
		this.reduce.addActionListener(this);
		optionPanel.add(this.reduce, c);

		JButton moreAbout = new JButton("More about");
		moreAbout.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {				
				JOptionPane.showMessageDialog(
						CriticalPairOptionGUI.this.dialog,
						"<HTML><BODY>"
						+"An essential critical pair exists for each conflict reason.<br>"
						+ "It expresses the conflict caused by exactly this conflict<br>"
						+"reason in a minimal context. Essential critical pairs<br>"
						+"is a subset of critical pairs.<br><br>"
						+"Please note:"
						+"<ul>"
						+"<li> <font color=\"#FF0000\">Multiplicity constraints of types </font> </li>"
						+"<li> <font color=\"#FF0000\">NACs</font> </li>"
						+"<li> <font color=\"#FF0000\">Graph consistency constraints</font> </li>"
						+"</ul>"
						+"are not taken into account during computing essential critical pairs.<br>"
						+"Therefore <font color=\"#FF0000\">delete-use</font> "
						+" and <font color=\"#FF0000\">attribute-change</font> conflicts would be detected only."						
						+"</BODY></HTML>",
						"  essential  ", 
						javax.swing.JOptionPane.INFORMATION_MESSAGE);
			}
		});
		optionPanel.add(moreAbout);
		
		return optionPanel;
	}

	private JPanel makeConsistent() {
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;

		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
		optionPanel.setBorder(new TitledBorder(" Select consistency check of critical pairs "));

//		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;

		c.insets = new Insets(0, 0, 0, 20);
		this.consistent = new JCheckBox("consistent", this.cpOption.consistentEnabled());
		this.consistent.addActionListener(this);
		optionPanel.add(this.consistent, c);		

		moreAboutConsist = new JButton("More about");
		moreAboutConsist.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				moreAboutConsist.setBackground(bgc);
				moreAboutConsist.setText("More about");
				JOptionPane.showMessageDialog(
						CriticalPairOptionGUI.this.dialog,
						"<HTML><BODY>"
						+"If selected, each critical graph will be checked<br>"
						+"due to enabled graph consistency constraints of the grammar.<br><br>"
						+"Please note, because of a critical graph is the minimal graph <br>"
						+"of a conflict situation, it is not always possible to check all <br>"
						+"used atomic graph constraints and to evaluate formulae.<br><br>"
						+"Furthermore, attribute conditions, constant values of attributes,<br>"
						+"multiple usage of a variable to detect duplication of objects -<br>"
						+"such things cannot be evaluated at critical graphs in which <br>"
						+"the attributes are not set or rather set by variables.<br><br>"
						+"It is advisable to use for CPA such consistency constraints which aim<br>"
						+"to forbid some graph structures but do not deal with attribute values.<br><br>"
						+"</BODY></HTML>",
						" CPA : Graph Consistency Constraints ", javax.swing.JOptionPane.INFORMATION_MESSAGE);
				CriticalPairOptionGUI.this.dialog.setVisible(true);
			}
		});
		optionPanel.add(moreAboutConsist);
		
		return optionPanel;
	}

	void highlightMoreAboutConsist(boolean sel) {
		boolean warn = false; // TODO
		if (warn) {
			if (sel) {
				bgc = moreAboutConsist.getBackground();
				moreAboutConsist.setBackground(Color.magenta);
				moreAboutConsist.setText("Read More");
			}
			else {
				moreAboutConsist.setBackground(bgc);
				moreAboutConsist.setText("More about");
			}
		}
	}
	
	private JPanel makeAttrCheck() {
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;

		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
		optionPanel.setBorder(new TitledBorder(" Select attribute check of critical pairs "));

//		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;

		c.insets = new Insets(0, 0, 0, 20);
		this.attrCheck = new JCheckBox("strong", this.cpOption.strongAttrCheckEnabled());
		this.attrCheck.addActionListener(this);
		optionPanel.add(this.attrCheck, c);
		
//		c.insets = new Insets(0, 0, 0, 0);
//		equalVariableNameOfAttrMapping = new JCheckBox("similar variable", false);		
//		equalVariableNameOfAttrMapping.addActionListener(this);
//		optionPanel.add(equalVariableNameOfAttrMapping, c);
		
		this.cpOption.enableEqualVariableNameOfAttrMapping(this.cpOption.equalVariableNameOfAttrMappingEnabled()); 
		
		JButton moreAbout = new JButton("More about");
		moreAbout.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(
						CriticalPairOptionGUI.this.dialog,
						"<HTML><BODY>"
						+"If - strong - is selected, extended attribute checking will take place<br>"												
						+ "when the first rule does change an attribute value of a graph<br>"
						+ "object and the second rule uses this attribute value<br>"
						+ "- as a target value of an input parameter, <br>"
						+ "- as a target value of a variable which is a part of an attribute condition.<br>"
						+ "The number of critical pairs may decrease.<br>"
						
//						+ "<br>If - similar variable - is selected, the names of attribute variables <br>"
//						+ "of the first and second rule are taken in account. That means, <br>"
//						+ "the attributes of nodes and edges could only be mapped <br>"
//						+ "when the names of the used variables are equal.<br>"
//						+ "The number of critical pairs may decrease."
						
						+"<br>Please note: An overview list with variables and variable equalities<br>"
						+"of an overlapping graph is available by using background pop-up <br>"
						+"menu of the graph panel.<br>"
						+"</BODY></HTML>",
						"  strong ", javax.swing.JOptionPane.INFORMATION_MESSAGE);
				CriticalPairOptionGUI.this.dialog.setVisible(true);
			}
		});
		optionPanel.add(moreAbout);
		
		
		
		return optionPanel;
	}
	
	private JPanel makeIgnoreCriticalPairs() {
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;

		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
		optionPanel.setBorder(new TitledBorder(" Ignore critical pairs "));

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;

		c.insets = new Insets(0, 0, 0, 0);
		this.ignoreIdentical = new JCheckBox("of same rules", this.cpOption.ignoreIdenticalRulesEnabled());
		this.ignoreIdentical.addActionListener(this);
		optionPanel.add(this.ignoreIdentical, c);

		c.insets = new Insets(0, 0, 0, 0);
		this.reduceSameMatch = new JCheckBox("of same rules and same matches", this.cpOption.reduceSameMatchEnabled());
		this.ignoreIdentical.setEnabled(!this.reduceSameMatch.isSelected());
		this.reduceSameMatch.addActionListener(this);
		optionPanel.add(this.reduceSameMatch, c);
		
		GridBagConstraints c1 = new GridBagConstraints();
		c1.fill = GridBagConstraints.BOTH;
		c1.gridwidth = GridBagConstraints.RELATIVE;
		c1.gridheight = 1;
		c1.weightx = 0.0;
		c1.anchor = GridBagConstraints.WEST;
		JPanel optionPanel1 = makeInitialOptionPanel(false, "", c1);
		
		c1.weightx = 1.0;

		c1.insets = new Insets(0, 0, 5, 0);
		this.directStrctCnfl = new JCheckBox("directly strict confluent", 
													this.cpOption.directlyStrictConflEnabled());
		this.directStrctCnfl.addActionListener(this);
		optionPanel1.add(this.directStrctCnfl, c1);
			
		JButton moreAbout1 = new JButton("More about");
		moreAbout1.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(
						CriticalPairOptionGUI.this.dialog,
						"<HTML><BODY>"
						+"If a critical pair is directly strict confluent <br>"
						+"via only one step direct transformations <b>t1</b> and <b>t2</b>, <br>"
						+"we say that <b>(t1, t2)</b> is a strict solution of the critical pair. <br>"
						+"<br>Such a critical pair can be ignored. <br><br>"
						+"</BODY></HTML>",
						"  directly strict confluent  ", javax.swing.JOptionPane.INFORMATION_MESSAGE);
			}
		});
		optionPanel1.add(moreAbout1);	
		c.insets = new Insets(0, 0, 0, 0);
		optionPanel.add(optionPanel1, c);
		
		GridBagConstraints c2 = new GridBagConstraints();
		c2.fill = GridBagConstraints.BOTH;
		c2.gridwidth = GridBagConstraints.RELATIVE;
		c2.gridheight = 1;
		c2.weightx = 0.0;
		c2.anchor = GridBagConstraints.WEST;
		JPanel optionPanel2 = makeInitialOptionPanel(false, "", c2);
		
		c2.weightx = 1.0;
		
		c2.insets = new Insets(0, 0, 5, 0);
		this.directStrctCnflUpToIso = new JCheckBox("directly strict confluent up to isomorphism", 
													this.cpOption.directlyStrictConflUpToIsoEnabled());
		this.directStrctCnflUpToIso.addActionListener(this);
		optionPanel2.add(this.directStrctCnflUpToIso, c2);
		
		JButton moreAbout2 = new JButton("More about");
		moreAbout2.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(
						CriticalPairOptionGUI.this.dialog,
						"<HTML><BODY>"
						+"If a critical pair is directly strict confluent up to isomorphism <br>"
						+"via only one step direct transformations <b>t1</b> and <b>t2</b>, <br>"
						+"we say that <b>(t1, t2)</b> is a strict solution of the critical pair. <br>"
						+"<br>Such a critical pair can be ignored. <br><br>"
						+"</BODY></HTML>",
						"  directly strict confluent up to isomorphism  ", javax.swing.JOptionPane.INFORMATION_MESSAGE);
			}
		});
		optionPanel2.add(moreAbout2);
		c.insets = new Insets(0, 0, 0, 0);
		optionPanel.add(optionPanel2, c);
		
		return optionPanel;
	}

	private JPanel makeCriticalPairsByNamedObject() {
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;

		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
		optionPanel.setBorder(new TitledBorder(" Critical pairs due to named objects "));

//		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;

		c.insets = new Insets(0, 0, 0, 0);
		this.namedObject = new JCheckBox("equal object names of overlapping objects", this.cpOption.namedObjectEnabled());
		this.namedObject.addActionListener(this);
		optionPanel.add(this.namedObject, c);
		
		JButton moreAbout = new JButton("More about");
		moreAbout.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(
						CriticalPairOptionGUI.this.dialog,
						"<HTML><BODY>"
						+"This option means that the overlappings of two graphs  <br>"												
						+ "of the involved rules are generated above nodes with <br>"
						+ "equal object names only.<br><br>"
						+"(The nodes of the same graph of the same rule must have <br>"
						+"different object names. But the object names between two <br>"
						+"rules should be equal.)<br><br>"
						+"The number of critical pairs may decrease drastically.<br><br>"
						+"</BODY></HTML>",
						"  equal object names of overlapping objects  ", javax.swing.JOptionPane.INFORMATION_MESSAGE);
				CriticalPairOptionGUI.this.dialog.setVisible(true);
			}
		});
		optionPanel.add(moreAbout);
				
		return optionPanel;
	}
	
	private JPanel makeMaxBoundOfCriticKind() {
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;

		JPanel optionPanel = makeInitialOptionPanel(true, "", c);
		optionPanel.setBorder(new TitledBorder(" Maximal amount of results per rule pair and conflict kind "));

//		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;

		c.insets = new Insets(0, 0, 0, 0);
		
		JPanel p = new JPanel();
		JLabel l = new JLabel("                    ");
		String maxbound = String.valueOf(this.cpOption.getMaxBoundOfCriticKind());
		String txt = maxbound.isEmpty() || maxbound.equals("0")? "unbound" : maxbound;
		this.maxBoundOfCriticKind = new JTextField(txt, 5);
		this.maxBoundOfCriticKind.setFont(new Font("SansSerif", Font.BOLD, 12));
		this.maxBoundOfCriticKind.setEditable(true); 
		p.add(this.maxBoundOfCriticKind);
		p.add(l);
		
		this.maxBoundOfCriticKind.addActionListener(cpa);
		this.maxBoundOfCriticKind.addActionListener(this);
		this.maxBoundOfCriticKind.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				setMaxBoundOfCP(((JTextField)e.getSource()).getText());
			}
		});
	
		optionPanel.add(p, c);
		
		JButton moreAbout = new JButton("More about");
		moreAbout.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(
						CriticalPairOptionGUI.this.dialog,
						"<HTML><BODY>"
						+"This option is only helpful when a rule pair may have a lot of critical graphs<br>"
						+"and therefore the need of memory and time to generate and check they is very high.<br>"
						+"To avoid out of memory or boring waiting of the total results one can use this option.<br>"
						+"The analysis process of a critical kind of a rule pair will terminate after <br>"	
						+"the number of critical graphs reaches the given maximal amount.<br><br>"
						
						+"The default value is 0 (\"unbound\"). It can be reset by an integer value > 0.<br><br>"
										
						+"</BODY></HTML>",
						"  Maximal amount of results per rule pair and conflict kind", javax.swing.JOptionPane.INFORMATION_MESSAGE);
				CriticalPairOptionGUI.this.dialog.setVisible(true);
			}
		});
		optionPanel.add(moreAbout);
				
		return optionPanel;
	}
	
	/**
	 * Returns a icon for the tab.
	 * 
	 * @return For now <I>null</I>
	 */
	public Icon getIcon() {
		return null;
	}

	/**
	 * Returns the text for the tab title.
	 * 
	 * @return <I>Critical Pair</I> is returned
	 */
	public String getTabTitle() {
		return "Critical Pairs";
	}

	/**
	 * Returns the text for the tab tip.
	 * 
	 * @return <I>Critical Pair Analysis</I> is returned
	 */
	public String getTabTip() {
		return "Options of Critical Pair Analysis";
	}

	@SuppressWarnings("unchecked")
	public void initLayers(Vector<String> v) {
		this.layers.removeAllItems();
		this.layers.addItem("All");
		for (int i = 0; i < v.size(); i++)
			this.layers.addItem(v.get(i));
	}

	/**
	 * Updates the display of the settings.
	 */
	public void update() {
		if (this.guiOption == null)
			this.numberCriticalPairs.setValue(this.numberCriticalPairs.getMaximum());
		else if (this.guiOption.getNumberOfCriticalPair() == ParserGUIOption.SHOWALLPAIRS)
			this.numberCriticalPairs.setValue(this.numberCriticalPairs.getMaximum());
		else
			this.numberCriticalPairs.setValue(this.guiOption.getNumberOfCriticalPair());

		if (this.guiOption == null) {
			this.verticalSize.setValue(200);
			this.horizontalSize.setValue(200);
		} else {
			this.verticalSize.setValue((int) this.guiOption.getCriticalPairWindowSize()
					.getHeight());
			this.horizontalSize.setValue((int) this.guiOption.getCriticalPairWindowSize()
					.getWidth());
		}

		if (this.cpOption == null)
			return;
		
		if (this.cpOption.getCriticalPairAlgorithm() == CriticalPairOption.EXCLUDEONLY) {
			this.algorithms.setSelectedItem(ParserOptionGUI.EXCLUDEONLY);
		} else if (this.cpOption.getCriticalPairAlgorithm() == CriticalPairOption.TRIGGER_DEPEND) {
			this.algorithms.setSelectedItem(ParserOptionGUI.TRIGGER_DEPEND);
		} else if (this.cpOption.getCriticalPairAlgorithm() == CriticalPairOption.TRIGGER_SWITCH_DEPEND) {
			this.algorithms.setSelectedItem(ParserOptionGUI.TRIGGER_SWITCH_DEPEND);
		}
		
		this.layered.setSelected(this.cpOption.layeredEnabled());

		if (this.layered.isSelected())
			this.layers.setEnabled(true);
		else
			this.layers.setEnabled(false);

		this.complete.setSelected(this.cpOption.completeEnabled());
		this.consistent.setSelected(this.cpOption.consistentEnabled());
		this.attrCheck.setSelected(this.cpOption.strongAttrCheckEnabled());
		this.reduceSameMatch.setSelected(this.cpOption.reduceSameMatchEnabled());
		this.ignoreIdentical.setSelected(this.cpOption.ignoreIdenticalRulesEnabled());
		this.reduce.setSelected(this.cpOption.reduceEnabled());
		
		this.directStrctCnfl.setSelected(this.cpOption.directlyStrictConflEnabled());
		this.directStrctCnflUpToIso.setSelected(this.cpOption.directlyStrictConflUpToIsoEnabled());
		
		this.namedObject.setSelected(this.cpOption.namedObjectEnabled());
		this.setMaxBoundOfCP(String.valueOf(this.cpOption.getMaxBoundOfCriticKind()));
	}

	/**
	 * Listens for events of the algorithm list.
	 * 
	 * @param e
	 *            The event from the combobox
	 */
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if (source == this.algorithms) {
			if (this.algorithms.getSelectedItem()
					.equals(ParserOptionGUI.EXCLUDEONLY)) {
				this.cpOption
						.setCriticalPairAlgorithm(CriticalPairOption.EXCLUDEONLY);
			} else if (this.algorithms.getSelectedItem()
					.equals(ParserOptionGUI.TRIGGER_DEPEND)) {
				this.cpOption
						.setCriticalPairAlgorithm(CriticalPairOption.TRIGGER_DEPEND);
			} else if (this.algorithms.getSelectedItem()
					.equals(ParserOptionGUI.TRIGGER_SWITCH_DEPEND)) {
				this.cpOption
						.setCriticalPairAlgorithm(CriticalPairOption.TRIGGER_SWITCH_DEPEND);
			}
		}
	}

	public void addActionListener(ActionListener l) {
		if (l instanceof GraTraOptionGUI)
			this.layered.addActionListener(l);		
	}

	/**
	 * Receives events. In this case the method listens for a button that
	 * changes between display and algorithm setting.
	 * 
	 * @param e
	 *            The event from the button
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		// System.out.println("CriticalPairOptionGUI.actioPerformed:: "+source);
		if (source.equals(this.displaySwitch) || source.equals(this.generalSwitch)) {
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weighty = 1.0;
			c.weightx = 1.0;
			if (source.equals(this.displaySwitch)) {
				remove(this.firstPriorityOption);
				add(this.secondPriorityOption, c);
			} else if (source.equals(this.generalSwitch)) {
				remove(this.secondPriorityOption);
				add(this.firstPriorityOption, c);
			}
			revalidate();
			this.secondPriorityOption.repaint();
			this.firstPriorityOption.repaint();
		} else if (source.equals(this.layered)) {
			this.cpOption.enableLayered(this.layered.isSelected());

			if (this.layered.isSelected())
				this.layers.setEnabled(true);
			else
				this.layers.setEnabled(false);
		} else if (source.equals(this.layers)) {
			if (this.layers.getSelectedItem() != null) {
				String l = this.layers.getSelectedItem().toString();
				if (l.equals("All"))
					l = "-1";
				this.cpOption.setLayer((Integer.valueOf(l)).intValue());
			}
		} else if (source.equals(this.complete)) {
			this.cpOption.enableComplete(this.complete.isSelected());
		}
		else if (source.equals(this.ignoreIdentical)) {
			if (this.ignoreIdentical.isSelected()) {
				this.reduceSameMatch.setSelected(false);
				this.reduceSameMatch.setEnabled(false);
			} else {
				this.reduceSameMatch.setEnabled(true);
			}
			this.cpOption.enableIgnoreIdenticalRules(this.ignoreIdentical.isSelected());			
		}
		else if (source.equals(this.reduceSameMatch)) {
			if (this.reduceSameMatch.isSelected()) {
				this.ignoreIdentical.setSelected(false);
				this.ignoreIdentical.setEnabled(false);
			} else {
				this.ignoreIdentical.setEnabled(true);
			}
			this.cpOption.enableReduceSameMatch(this.reduceSameMatch.isSelected());			
		}
		else if (source.equals(this.reduce)) {
			this.cpOption.enableReduce(this.reduce.isSelected());
		}
		else if (source.equals(this.directStrctCnfl)) {
			this.cpOption.enableDirectlyStrictConfl(this.directStrctCnfl.isSelected());
		}
		else if (source.equals(this.directStrctCnflUpToIso)) {
			this.cpOption.enableDirectlyStrictConflUpToIso(this.directStrctCnflUpToIso.isSelected());
		}
		else if (source.equals(this.consistent)) {
			highlightMoreAboutConsist(this.consistent.isSelected());
			this.cpOption.enableConsistent(this.consistent.isSelected());			
		} 
		else if (source.equals(this.attrCheck)) {
			this.cpOption.enableStrongAttrCheck(this.attrCheck.isSelected());
		} else if (source.equals(this.equalVariableNameOfAttrMapping)) {
			this.cpOption.enableEqualVariableNameOfAttrMapping(this.equalVariableNameOfAttrMapping.isSelected());
		} 
		else if (source.equals(this.namedObject)) {
			this.cpOption.enableNamedObject(this.namedObject.isSelected());			
		}
		else if (source.equals(this.maxBoundOfCriticKind)) {
			setMaxBoundOfCP(this.maxBoundOfCriticKind.getText());
			this.grabFocus();			
		}
		else if (source.equals(this.criticalStyleGreen)) {
			this.guiOption.setDrawingStyleOfCriticalObjects(0);
		}
		else if (source.equals(this.criticalStyleBlackBold)) {
			this.guiOption.setDrawingStyleOfCriticalObjects(1);
		}
		else if (source instanceof JRadioButton) {
			if (((JRadioButton) source).getActionCommand().equals("layered")) {
				this.layered.setSelected(true);
				this.cpOption.enableLayered(this.layered.isSelected());
			} else if (((JRadioButton) source).getActionCommand().equals(
					"priority")) {
				this.cpOption.enablePriority(true);
				this.layered.setSelected(false);
				this.cpOption.enableLayered(false);
			} else {
				this.layered.setSelected(false);
				this.cpOption.enableLayered(false);
				this.cpOption.enablePriority(false);
			}
		}
	}

	protected void setMaxBoundOfCP(String val) {
		int maxOpVal = this.cpOption.getMaxBoundOfCriticKind();
		int maxbound = 0;
		try {
			maxbound = Integer.valueOf(val).intValue();
			if (maxbound <= 0) 
				maxbound = 0;
			if (maxbound == 0) 
				this.maxBoundOfCriticKind.setText("unbound");				
			else 
				this.maxBoundOfCriticKind.setText(val);
		} 
		catch (Exception ex) {
			maxbound = 0;
			this.maxBoundOfCriticKind.setText("unbound");
		}
		if (maxbound >= 0 && maxbound != maxOpVal) {
			this.cpOption.setMaxBoundOfCriticKind(maxbound);
			if (cpa.getConflictPairContainer() != null) {
				((ExcludePairContainer)cpa.getConflictPairContainer()).enableMaxBoundOfCriticKind(maxbound);
			}
			if (cpa.getDependencyPairContainer() != null) {
				((ExcludePairContainer)cpa.getDependencyPairContainer()).enableMaxBoundOfCriticKind(maxbound);
			}
		}
	}
	
	protected void refreshMaxBoundOfCP() {
		boolean ok = false;
		int maxOpVal = this.cpOption.getMaxBoundOfCriticKind();
		int maxbound = 0;
		String val = this.maxBoundOfCriticKind.getText();
		if (val.equals("unbound")) {
			maxbound = 0;
			ok = true;
		}
		else {
			try {
				maxbound = Integer.valueOf(val).intValue();
				if (maxbound == 0)
					ok = true;
				else if (maxbound > 0)
					ok = true;
				else  // restore
					this.maxBoundOfCriticKind.setText(String.valueOf(maxOpVal));
			} 
			catch (Exception ex) { // restore
				this.maxBoundOfCriticKind.setText(String.valueOf(maxOpVal));
			}
		}
		if (ok && maxbound >= 0 && maxbound != maxOpVal) {
			this.cpOption.setMaxBoundOfCriticKind(maxbound);
			if (cpa.getConflictPairContainer() != null) 
				((ExcludePairContainer)cpa.getConflictPairContainer()).enableMaxBoundOfCriticKind(maxbound);
			if (cpa.getDependencyPairContainer() != null) 
				((ExcludePairContainer)cpa.getDependencyPairContainer()).enableMaxBoundOfCriticKind(maxbound);
		}
	}
	
	public JComponent getComponentMaxBoundOfCriticKind() {
		return this.maxBoundOfCriticKind;
	}
	
	/**
	 * Listens for events of the sliders for the internal frame size.
	 * 
	 * @param e
	 *            The events of the sliders
	 */
	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
		if (source.equals(this.verticalSize) || source.equals(this.horizontalSize)) {
			this.virtualGraph.setSize(this.horizontalSize.getValue() / 2, this.verticalSize
					.getValue() / 2);
			if (this.guiOption == null)
				this.guiOption.setCriticalPairWindowSize(200, 200);
			else
				this.guiOption.setCriticalPairWindowSize(this.horizontalSize.getValue(),
						this.verticalSize.getValue());
		} else if (source.equals(this.numberCriticalPairs)) {
			int op = this.numberCriticalPairs.getValue();
			if (this.guiOption != null) {
				if (op == MAX) {
					this.guiOption
							.setNumberOfCriticalPair(ParserGUIOption.SHOWALLPAIRS);
				} else {
					this.guiOption.setNumberOfCriticalPair(op);
				}
			}
		}
	}

	/* Implements java.util.EventListener */
	public void optionEventOccurred(EventObject e) {
//		System.out.println("CriticalPairOptionGUI.optionEventOccurred: "+e.getSource());
		if (e.getSource() instanceof JRadioButton) {
			JRadioButton cb = (JRadioButton) e.getSource();
			if (cb.getActionCommand().equals("layered")) {
				this.cpOption.enableLayered(cb.isSelected());
				this.layered.doClick();
			}
		} else if (e.getSource() instanceof ParserOption) {
			boolean b = ((ParserOption) e.getSource()).layerEnabled();
			this.cpOption.enableLayered(b);
			if (b && !this.layered.isSelected())
				this.layered.doClick();
			else if (!b && this.layered.isSelected())
				this.layered.doClick();
			if (this.layered.isSelected())
				this.layers.setEnabled(true);
		}
	}	

	public void executeOnClose() {
		this.refreshMaxBoundOfCP();
	}
}
/*
 * $Log: CriticalPairOptionGUI.java,v $
 * Revision 1.13  2010/11/16 23:32:19  olga
 * improved
 *
 * Revision 1.12  2010/11/14 19:00:07  olga
 * improved - apply loaded CPA options
 *
 * Revision 1.11  2010/11/07 20:50:09  olga
 * extended by new options
 *
 * Revision 1.10  2010/11/04 10:58:30  olga
 * tuning
 *
 * Revision 1.9  2010/08/23 07:33:42  olga
 * tuning
 *
 * Revision 1.8  2009/08/05 14:31:33  olga
 * Code tuning
 *
 * Revision 1.7  2009/08/03 16:54:08  olga
 * CPA , essential pairs - bug fixed
 *
 * Revision 1.6  2009/04/20 08:50:45  olga
 * CPA: bug fixed
 *
 * Revision 1.5  2009/03/25 15:19:16  olga
 * code tuning
 *
 * Revision 1.4  2009/03/19 09:31:06  olga
 * CPE: attr check improved
 *
 * Revision 1.3  2009/03/12 12:27:41  olga
 * Consistency check of critical graphs in CPA by default OFF
 *
 * Revision 1.2  2009/03/12 10:57:47  olga
 * some changes in CPA of managing names of the attribute variables.
 *
 * Revision 1.1  2008/10/29 09:04:13  olga
 * new sub packages of the package agg.gui: typeeditor, editor, trafo, cpa, options, treeview, popupmenu, saveload
 *
 * Revision 1.21  2008/09/11 09:22:26  olga
 * Some changes in CPA: new computing of conflicts after an option changed,
 * Graph layout of overlapping graphs
 *
 * Revision 1.20  2008/07/23 17:02:31  olga
 * Graph layout bugs fixed
 *
 * Revision 1.19  2008/05/19 09:19:33  olga
 * Applicability of Rule Sequence - reworking
 *
 * Revision 1.18  2008/05/14 07:43:27  olga
 * Applicability of Rule Sequences - bugs fixed
 *
 * Revision 1.17  2008/04/07 09:36:56  olga
 * Code tuning: refactoring + profiling
 * Extension: CPA - two new options added
 *
 * Revision 1.16  2008/02/18 09:37:10  olga
 * - an extention of rule dependency check is implemented;
 * - some bugs fixed;
 * - editing of graphs improved
 *
 * Revision 1.15  2007/11/19 08:48:41  olga
 * Some GUI usability mistakes fixed.
 * Default values in node/edge of a type graph implemented.
 * Code tuning.
 *
 * Revision 1.14  2007/10/10 07:44:28  olga
 * CPA: bug fixed
 * GUI, AtomConstraint: bug fixed
 *
 * Revision 1.13  2007/09/27 08:43:32  olga
 * tuning
 *
 * Revision 1.12  2007/09/27 08:42:47  olga
 * CPA: new option  -ignore pairs with same rules and same matches-
 *
 * Revision 1.11  2007/09/10 13:05:45  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.10 2007/03/28 10:01:13 olga -
 * extensive changes of Node/Edge Type Editor, - first Undo implementation for
 * graphs and Node/edge Type editing and transformation, - new / reimplemented
 * options for layered transformation, for graph layouter - enable / disable for
 * NACs, attr conditions, formula - GUI tuning
 * 
 * Revision 1.9 2007/02/05 12:33:44 olga CPA: chengeAttribute
 * conflict/dependency : attributes with constants bug fixed, but the critical
 * pairs computation has still a gap.
 * 
 * Revision 1.8 2007/01/11 10:21:19 olga Optimized Version 1.5.1beta , free for
 * tests
 * 
 * Revision 1.7 2006/12/13 13:33:05 enrico reimplemented code
 * 
 * Revision 1.6 2006/03/01 09:55:47 olga - new CPA algorithm, new CPA GUI
 * 
 * Revision 1.5 2005/12/21 14:48:46 olga GUI tuning
 * 
 * Revision 1.4 2005/10/10 08:05:16 olga Critical Pair GUI and CPA graph
 * 
 * Revision 1.3 2005/09/26 16:41:20 olga CPA graph, CPs - visualization
 * 
 * Revision 1.2 2005/09/19 09:12:14 olga CPA GUI tuning
 * 
 * Revision 1.1 2005/08/25 11:56:55 enrico *** empty log message ***
 * 
 * Revision 1.3 2005/07/13 08:13:37 olga Some code optimization only
 * 
 * Revision 1.2 2005/07/11 09:30:20 olga This is test version AGG V1.2.8alfa .
 * What is new: - saving rule option <disabled> - setting trigger rule for layer -
 * display attr. conditions in gragra tree view - CPA algorithm <dependencies> -
 * creating and display CPA graph with conflicts and/or dependencies based on
 * (.cpx) file
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.12 2005/05/23 09:54:30 olga CPA improved: Stop of generation
 * process or rule pair.
 * 
 * Revision 1.11 2005/01/28 14:02:32 olga -Fehlerbehandlung beim Typgraph check
 * -Erweiterung CP GUI / CP Menu -Fehlerbehandlung mit identification option
 * -Fehlerbehandlung bei Rule PAC
 * 
 * Revision 1.10 2004/12/20 14:53:48 olga Changes because of matching
 * optimisation.
 * 
 * Revision 1.9 2004/10/25 14:24:37 olga Fehlerbehandlung bei CPs und
 * Aenderungen im zusammenhang mit termination-Modul in AGG
 * 
 * Revision 1.8 2004/09/13 10:21:14 olga Einige Erweiterungen und
 * Fehlerbeseitigung bei CPs und Graph Grammar Transformation
 * 
 * Revision 1.7 2004/06/23 08:26:57 olga CPs sind endlich OK.
 * 
 * Revision 1.6 2004/06/21 08:35:33 olga immer noch CPs
 * 
 * Revision 1.5 2004/04/15 10:49:48 olga Kommentare
 * 
 * Revision 1.4 2003/03/05 18:24:10 komm sorted/optimized import statements
 * 
 * Revision 1.3 2002/09/26 13:59:50 olga GUI- Arbeit
 * 
 * Revision 1.2 2002/09/19 16:22:38 olga Arbeit im wesentlichen an GUI.
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:19 olga Imported sources
 * 
 * Revision 1.7 2001/08/16 14:06:50 olga Aenderungen wegen Layers.
 * 
 * Revision 1.6 2001/07/09 13:12:44 olga Aenderungen an GUI. Version heisst ab
 * jetzt 1.1
 * 
 * Revision 1.5 2001/06/26 17:24:48 olga Unwesentliche Aenderung.
 * 
 * Revision 1.4 2001/06/13 16:48:17 olga Neue Option "complete" fuer
 * CP-Vollstaendigkeit eingefuert.
 * 
 * Revision 1.3 2001/03/22 15:52:29 olga GUI an den veraenderten GraphEditor
 * angepasst.
 * 
 * Revision 1.2 2001/03/08 11:02:42 olga Parser Anbindung gemacht. Stand nach
 * AGG GUI Reimplementierung. Stand nach der AGG GUI Reimplementierung.Das ist
 * Stand nach der AGG GUI Reimplementierung und Parser Anbindung.
 * 
 * Revision 1.1.2.4 2001/01/28 13:14:42 shultzke API fertig
 * 
 * Revision 1.1.2.3 2000/12/21 13:46:01 shultzke optionen weiter veraendert
 * 
 * Revision 1.1.2.2 2000/12/19 12:11:42 shultzke Parseroptiongui und
 * criticalpairoptionGUI getrennt
 * 
 * Revision 1.1.2.1 2000/12/18 13:33:32 shultzke Optionen veraendert
 * 
 */
