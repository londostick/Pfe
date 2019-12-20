/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.parser;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventObject;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import agg.gui.options.AbstractOptionGUI;
import agg.parser.LayerOption;
import agg.parser.OptionEventListener;

/**
 * This gui shows the preferences for the layer function. The user can choose
 * between a basic function and a function with NAC check.
 * 
 * @version $Id: LayerOptionGUI.java,v 1.4 2010/09/23 08:20:54 olga Exp $
 * @author $Author: olga $
 */
@SuppressWarnings("serial")
public class LayerOptionGUI extends AbstractOptionGUI implements ItemListener,
		OptionEventListener {

	/**
	 * the choice of the different function
	 */
	@SuppressWarnings("rawtypes")
	JComboBox layerType;

	public static final String RCD_LAYER = "Rule, Creation, Deletion, Rule must delete";

	public static final String RCDN_LAYER = RCD_LAYER + ", NAC check";

	public static final String WEAK_RCD_LAYER = "Rule, Creation, Deletion";

	public static final String WEAK_RCDN_LAYER = WEAK_RCD_LAYER + ", NAC check";

	/**
	 * The option
	 * 
	 * @serial this is serializable because a super class is serializable
	 */
	private LayerOption lOption;

	/**
	 * Creates a new gui with specified settings.
	 * 
	 * @param lOption
	 *            specifies the option to modify
	 */
	public LayerOptionGUI(LayerOption lOption) {
		super();
		this.lOption = lOption;

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		setLayout(gridbag);

		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weighty = 0.0;
		c.weightx = 1.0;
		add(makeLayerType(), c);

		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weighty = 1.0;
		c.weightx = 1.0;
		add(new JPanel(), c);
		validate();
	}

	public Dimension getPreferredSize() {
		return new Dimension(350, 480);
	}

	/*
	 * private void addIcon(JPanel optionPanel){ GridBagConstraints c = new
	 * GridBagConstraints(); c.fill = GridBagConstraints.NONE; c.gridwidth = 1;
	 * c.gridheight = 2; c.weightx = 0.0; c.weighty = 0.0; c.insets = new
	 * Insets(1,1,1,1); ImageIcon optionImage = ; JLabel optionLabel = new
	 * JLabel(optionImage); //optionLabel.setSize(50,50);//89,99);
	 * optionLabel.setHorizontalAlignment(JLabel.CENTER);
	 * optionLabel.setVerticalAlignment(JLabel.CENTER);
	 * optionLabel.setHorizontalTextPosition(JLabel.CENTER);
	 * optionLabel.setVerticalTextPosition(JLabel.CENTER);
	 * optionPanel.add(optionLabel,c); }
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JPanel makeLayerType() {
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;

		JPanel optionPanel = makeInitialOptionPanel(true, 
				"Select algorithm for layer function", c);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		optionPanel.add(new JPanel(), c);
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.WEST;
		this.layerType = new JComboBox();
		this.layerType.addItem(RCDN_LAYER);
		this.layerType.addItem(WEAK_RCDN_LAYER);
		this.layerType.addItem(RCD_LAYER);
		this.layerType.addItem(WEAK_RCD_LAYER);
		this.layerType.addItemListener(this);
		optionPanel.add(this.layerType, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		optionPanel.add(new JPanel(), c);

		return optionPanel;
	}

	/**
	 * Receives events if another layer function is selected in the gui.
	 * 
	 * @param e
	 *            the event for the change
	 */
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if (source == this.layerType) {
			if (this.layerType.getSelectedItem().equals(RCDN_LAYER)) {
				this.lOption.setLayer(LayerOption.RCDN_LAYER);
			} else if (this.layerType.getSelectedItem().equals(WEAK_RCDN_LAYER)) {
				this.lOption.setLayer(LayerOption.WEAK_RCDN_LAYER);
			} else if (this.layerType.getSelectedItem().equals(RCD_LAYER)) {
				this.lOption.setLayer(LayerOption.RCD_LAYER);
			} else if (this.layerType.getSelectedItem().equals(WEAK_RCD_LAYER)) {
				this.lOption.setLayer(LayerOption.WEAK_RCD_LAYER);
			}
		}
	}

	/**
	 * Returns a icon for the tab.
	 * 
	 * @return the icon
	 */
	public Icon getIcon() {
		return null;
	}

	/**
	 * Returns the text for the tab title.
	 * 
	 * @return <I>Layer</I> is returned
	 */
	public String getTabTitle() {
		return "Layer";
	}

	/**
	 * Returns the text for the tab tip.
	 * 
	 * @return <I>Layer Function</I> is returned
	 */
	public String getTabTip() {
		return "Layer Function";
	}

	/**
	 * Updates the gui to the setting of the option.
	 */
	public void update() {
		switch (this.lOption.getLayer()) {
		case LayerOption.RCDN_LAYER:
			this.layerType.setSelectedItem(RCDN_LAYER);
			break;
		case LayerOption.WEAK_RCDN_LAYER:
			this.layerType.setSelectedItem(WEAK_RCDN_LAYER);
			break;
		case LayerOption.RCD_LAYER:
			this.layerType.setSelectedItem(RCD_LAYER);
			break;
		case LayerOption.WEAK_RCD_LAYER:
			this.layerType.setSelectedItem(WEAK_RCD_LAYER);
			break;
		default:
			break;
		}
	}

	/* Implements java.util.EventListener */
	public void optionEventOccurred(EventObject e) {
		System.out.println("LayerOptionGUI.optionEventOccurred");
		if (e.getSource() instanceof LayerOption)
			update();
		/*
		 * if(this.layerType.getSelectedItem().equals(RCDN_LAYER)){
		 * this.lOption.setLayer(LayerOption.RCDN_LAYER); } else
		 * if(this.layerType.getSelectedItem().equals(WEAK_RCDN_LAYER)){
		 * this.lOption.setLayer(LayerOption.WEAK_RCDN_LAYER); } else
		 * if(this.layerType.getSelectedItem().equals(RCD_LAYER)){
		 * this.lOption.setLayer(LayerOption.RCD_LAYER); } else
		 * if(this.layerType.getSelectedItem().equals(WEAK_RCD_LAYER)){
		 * this.lOption.setLayer(LayerOption.WEAK_RCD_LAYER); }
		 */
		// System.out.println(this.lOption.getLayer()+"
		// "+this.layerType.getSelectedItem());
	}

	public void executeOnClose() {}
	

}
/*
 * $Log: LayerOptionGUI.java,v $
 * Revision 1.4  2010/09/23 08:20:54  olga
 * tuning
 *
 * Revision 1.3  2008/10/29 09:04:12  olga
 * new sub packages of the package agg.gui: typeeditor, editor, trafo, cpa, options, treeview, popupmenu, saveload
 *
 * Revision 1.2  2007/09/10 13:05:45  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.1 2005/08/25 11:56:55 enrico ***
 * empty log message ***
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.4 2003/03/05 18:24:09 komm sorted/optimized import statements
 * 
 * Revision 1.3 2002/09/26 13:59:50 olga GUI- Arbeit
 * 
 * Revision 1.2 2002/09/19 16:22:39 olga Arbeit im wesentlichen an GUI.
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:19 olga Imported sources
 * 
 * Revision 1.7 2001/08/16 14:06:50 olga Aenderungen wegen Layers.
 * 
 * Revision 1.6 2001/08/08 14:45:15 olga Menu Critical Pair/Empty eingebaut;
 * wird mit Debug benutzt; Empty entleert die CP Container, sonst werden sie
 * behalten, solange Parser Grammatik nicht geaender wurde.
 * 
 * Bei LayerOption als default Einstellung : RCDN_LAYER (mit NACs check).
 * 
 * Wenn CP geladen werden und sie wurden mit Layers berechnet, aber die Option
 * Layered fuer Parser und CP momentan nicht selektiert ist, wird sie selektiert
 * (im umgekerten Fall - unselektiert); das bleibt dann als globale Einstellung
 * geltend.
 * 
 * Revision 1.5 2001/07/09 13:12:44 olga Aenderungen an GUI. Version heisst ab
 * jetzt 1.1
 * 
 * Revision 1.4 2001/06/26 17:24:48 olga Unwesentliche Aenderung.
 * 
 * Revision 1.3 2001/03/22 15:52:30 olga GUI an den veraenderten GraphEditor
 * angepasst.
 * 
 * Revision 1.2 2001/03/08 11:02:44 olga Parser Anbindung gemacht. Stand nach
 * AGG GUI Reimplementierung. Stand nach der AGG GUI Reimplementierung.Das ist
 * Stand nach der AGG GUI Reimplementierung und Parser Anbindung.
 * 
 * Revision 1.1.2.4 2001/01/14 14:48:20 shultzke commentare ergaenzt
 * 
 * Revision 1.1.2.3 2001/01/03 09:44:55 shultzke TODO's bis auf laden und
 * speichern erledigt. Wann meldet sich endlich Michael?
 * 
 * Revision 1.1.2.2 2000/12/26 10:00:03 shultzke Layered Parser hinzugefuegt
 * 
 * Revision 1.1.2.1 2000/12/18 13:33:34 shultzke Optionen veraendert
 * 
 */
