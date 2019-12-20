/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.cpa;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowAdapter;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Hashtable;
import java.util.EventObject;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdGraph;
import agg.editor.impl.EdNode;
import agg.editor.impl.EdArc;
import agg.gui.AGGAppl;
import agg.gui.event.TreeViewEvent;
import agg.gui.event.TreeViewEventListener;
import agg.gui.options.CriticalPairOptionGUI;
import agg.gui.options.ParserGUIOption;
import agg.gui.options.ParserOptionGUI;
import agg.gui.parser.GUIExchange;
import agg.gui.parser.LayerGUI;
import agg.gui.parser.PairIOGUI;
import agg.gui.parser.event.OptionListener;
import agg.gui.parser.event.StatusMessageEvent;
import agg.gui.parser.event.StatusMessageListener;
import agg.gui.parser.event.ParserGUIListener;
import agg.gui.parser.event.ParserGUIEvent;
import agg.gui.saveload.GraphicsExportJPEG;
import agg.gui.treeview.GraGraTreeView;

import agg.parser.CriticalPairOption;
import agg.parser.CriticalPair;
import agg.parser.ExcludePairContainer;
import agg.parser.DependencyPairContainer;
import agg.parser.PriorityExcludePairContainer;
import agg.parser.PriorityDependencyPairContainer;
import agg.parser.LayerOption;
import agg.parser.LayeredExcludePairContainer;
import agg.parser.LayeredDependencyPairContainer;
import agg.parser.PairContainer;
import agg.parser.ParserEvent;
import agg.parser.ParserEventListener;
import agg.parser.ParserFactory;
import agg.parser.ParserMessageEvent;
import agg.parser.ParserOption;
import agg.parser.OptionEventListener;
import agg.xt_basis.GraGra;
import agg.xt_basis.Rule;
import agg.xt_basis.RuleLayer;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.attribute.impl.AttrTupleManager;
import agg.util.Pair;

/**
 * The class creates an AGG critical pair analyzer .
 * 
 * @author $Author: olga $
 * @version $ID
 */
public class CriticalPairAnalysis implements TreeViewEventListener,
		ParserEventListener, OptionListener, // parser options
		OptionEventListener, // CP options 
		ParserGUIListener, ActionListener, StatusMessageListener {

	public boolean allowNodeTypeInheritance;
	
	/** Creates a new instance of the AGG analysis */
	public CriticalPairAnalysis(AGGAppl appl, GraGraTreeView graTreeView) {
		this.parent = appl;
		this.treeView = graTreeView;
		this.listener = new Vector<ParserEventListener>();
		this.pmlistener = new Vector<StatusMessageListener>();

		this.cpOption = new CriticalPairOption();
		this.cpOption.addOptionListener(this);
		this.cpOptionGUI = new CriticalPairOptionGUI(this, this.cpOption, this.option, this.pOption);
		this.pairsGUI = new CriticalPairAnalysisGUI(appl, this.option);
		this.pairsIOGUI = new PairIOGUI(this.parent);

		this.back = new JMenu("               Back to AGG Editor             ");
		this.back.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				backToMainGUI(false);
			}
		});
		
		this.menus = new Vector<JMenu>(2);
		createAnalysisMenu();

		addCPAnalysisEventListener(this);
		this.pairsGUI.addStatusMessageListener(this);

		this.changer = new GUIExchange(this.parent);

		cpa = this;

		this.wl = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (e.getSource() instanceof JFrame) {
					if (((JFrame) e.getSource()) == CriticalPairAnalysis.this.hostGraphFrame) {
						// unset old critical graph objects of host graph
						EdGraph hostg = CriticalPairAnalysis.this.treeView.getCurrentGraGra().getGraph();
						Iterator<?> en = hostg.getBasisGraph().getNodesSet().iterator();
						while (en.hasNext()) {
							GraphObject o = (GraphObject) en.next();
							o.setCritical(false);
							EdNode n = hostg.findNode(o);
							if (n != null)
								n.clearMorphismMark();						
						}
						en = hostg.getBasisGraph().getArcsSet().iterator();
						while (en.hasNext()) {
							GraphObject o = (GraphObject) en.next();
							o.setCritical(false);							
							EdArc a = hostg.findArc(o);
							if (a != null)
								a.clearMorphismMark();
						}
						hostg.deselectAll();
						hostg.update();
						CriticalPairAnalysis.this.treeView.graphDidChange();
						if (CriticalPairAnalysis.this.overlapGraphs != null) {
							CriticalPairAnalysis.this.overlapGraphs.clear();
							CriticalPairAnalysis.this.overlapGraphs = null;
						}
						if (CriticalPairAnalysis.this.hostGraphCPA
								.getPairContainer(CriticalPair.CONFLICT) != null) {
							if (CriticalPairAnalysis.this.pairsGUI
									.getCriticalPairPanel(CriticalPair.CONFLICT) != null)
								CriticalPairAnalysis.this.pairsGUI.getCriticalPairPanel(
										CriticalPair.CONFLICT).refreshView();
							CriticalPairAnalysis.this.hostGraphCPA
									.getPairContainer(CriticalPair.CONFLICT)
									.enableUseHostGraph(false, null, null);
						} else if (CriticalPairAnalysis.this.hostGraphCPA
								.getPairContainer(CriticalPair.TRIGGER_DEPENDENCY) != null) {
							if (CriticalPairAnalysis.this.pairsGUI
									.getCriticalPairPanel(CriticalPair.TRIGGER_DEPENDENCY) != null)
								CriticalPairAnalysis.this.pairsGUI.getCriticalPairPanel(
										CriticalPair.TRIGGER_DEPENDENCY).refreshView();
							CriticalPairAnalysis.this.hostGraphCPA.getPairContainer(
									CriticalPair.TRIGGER_DEPENDENCY)
									.enableUseHostGraph(false, null, null);

						}

						CriticalPairAnalysis.this.pairsGraGra.setChanged(false);
						resetRuleApplicable(CriticalPairAnalysis.this.pairsGraGra.getBasisGraGra());
						if (CriticalPairAnalysis.this.selectedGraGra != null) {
							CriticalPairAnalysis.this.selectedGraGra.setChanged(false);
							resetRuleApplicable(CriticalPairAnalysis.this.selectedGraGra.getBasisGraGra());
						}
						CriticalPairAnalysis.this.treeView.getTree().treeDidChange();
						CriticalPairAnalysis.this.pairsSaved = true;
						CriticalPairAnalysis.this.separatedFrames.remove(e.getSource());
					} else {
						CriticalPairAnalysis.this.separatedFrames.remove(e.getSource());
					}
				}
			}

			public void windowActivated(WindowEvent e) {
			}
		};

		((JFrame) this.parent).addWindowListener(this.wl);
	}

	JMenu back;
	private void addBackToAGGMenuBar(AGGAppl appl) {
		appl.addMenu(back);
	}
	
	private void removeBackFromAGGMenuBar(AGGAppl appl) {
		appl.removeMenu(back);
	}
	
	public Enumeration<JMenu> getMenus() {
		return this.menus.elements();
	}

	public EdGraGra getGraGra() {
		return this.pairsGraGra;
	}
	
	public boolean isEmpty() {
		return this.pairsGUI.isEmpty();
	}
	
	public CriticalPairAnalysisGUI getCriticalPairAnalysisGUI() {
		return this.pairsGUI;
	}

	public CriticalPairOption getCriticalPairOption() {
		return this.cpOption;
	}

	public CriticalPairOptionGUI getCriticalPairOptionGUI() {
		return this.cpOptionGUI;
	}

	public void setCriticalPairOption(CriticalPairOption cpOpt) {
		this.cpOption = cpOpt;
	}

	public void setCriticalPairOptionGUI(CriticalPairOptionGUI cpOptionGUI) {
		this.cpOptionGUI = cpOptionGUI;
	}

	public void setLayerOption(LayerOption lo) {
		this.lOption = lo;
	}

	public void setParserOption(ParserOption pOption) {
		this.pOption = pOption;
		this.cpOptionGUI.setParserOption(pOption);
		pOption.addOptionListener(this.cpOptionGUI);
	}

	/** Sets the GUI options for display settings. */
	public void setGUIOption(ParserGUIOption guiOption) {
		this.option = guiOption;
		this.cpOptionGUI.setGUIOption(guiOption);
		this.pairsGUI.setGUIOption(guiOption);
	}

	/** Sets the gragra to analyze */
	public void setGraGra(EdGraGra gra) {
		if (this.pairsGraGra != null) {
			this.pairsGraGra.setEditable(true);
			this.gragraChanged.remove(this.pairsGraGra);
		}
		this.pairsGraGra = gra;
		this.pairsGUI.setGraGra(this.pairsGraGra);
		this.pairsContainer = null;
		this.pairsContainer2 = null;
		this.conflictDependGraph = null;
		this.cpaGraph = null;
		this.ruleList1 = null;
		this.ruleList2 = null;
		
		if (this.pairsGraGra != null) {
			this.rulesCP.setEnabled(true);
			this.resetCP.setEnabled(true);
			this.startCP.setEnabled(true);
			this.debugCP.setEnabled(true);
			
			if (this.pairsGraGra.getBasisGraGra().isLayered())
				this.cpOptionGUI
						.initLayers(this.pairsGraGra.getBasisGraGra().getEnabledLayers());
			
			this.gragraChanged
					.put(this.pairsGraGra, new Boolean(this.pairsGraGra.isChanged()));
		} else {
			this.rulesCP.setEnabled(false);
			this.resetCP.setEnabled(false);
			this.unlockCP.setEnabled(false);
			this.startCP.setEnabled(false);
			this.stopCP.setEnabled(false);
			this.debugCP.setEnabled(false);
			this.emptyCP.setEnabled(false);
			this.loadCP.setEnabled(true);
			this.saveCP.setEnabled(false);
			this.showCP.setEnabled(false);
			this.checkHostGraphCP.setEnabled(false);
			this.backCP.setEnabled(false);
		}
	}

	public void setExportJPEG(GraphicsExportJPEG jpg) {
		this.exportJPEG = jpg;
		if (this.pairsGUI != null)
			this.pairsGUI.getGraphDesktop().setExportJPEG(this.exportJPEG);
	}

	/** Implements TreeViewEventListener */
	public void treeViewEventOccurred(TreeViewEvent e) {
		int msgkey = e.getMsg();
		if (msgkey == TreeViewEvent.SELECTED) {
			if (e.getData().isGraGra()) {
				this.selectedGraGra = e.getData().getGraGra();
				if (this.pairsGraGra == null) {
					setGraGra(this.selectedGraGra);
				} else if (this.pairsGUI.getGraphDesktop().hasEmptyComponents()) {
					setGraGra(this.selectedGraGra);
				} 
				else if (this.changer.isSet()) {
					// this is the case when .cpx file was loaded 
					// the gragra is already set
				}
				else if (this.pairsSaved) {
					if (!this.pairsGraGra.getBasisGraGra().compareTo(
							this.selectedGraGra.getBasisGraGra(), true)
							|| (layerUsed() != this.cpOption.layeredEnabled())) {
						setGraGra(this.selectedGraGra);
					}
				}
			}
		}
		if (msgkey == TreeViewEvent.DELETED) {
			if (e.getData().isGraGra()) {
				if (this.pairsGraGra == e.getData().getGraGra()) {
					this.pairsGUI.reinitGraphDesktop();
					setGraGra(null);
					this.pairsSaved = true;
				}
				if (this.selectedGraGra == e.getData().getGraGra()) {
					this.selectedGraGra = null;				
				}
			}
		}
	}

	/* Implements agg.parser.ParserEventListener */
	public void parserEventOccured(ParserEvent e) {
		if ((e.getMessage().indexOf("Critical") != -1)
				&& (e.getMessage().indexOf("finished") != -1)) {
			this.pairsSaved = false;
			updateCPAgraph();

			this.startCP.setEnabled(true);
			this.stopCP.setEnabled(false);
			this.consistCP.setEnabled(true);
			this.loadCP.setEnabled(true);
			this.saveCP.setEnabled(true);
			this.showCP.setEnabled(true);
			this.checkHostGraphCP.setEnabled(true);
			if (this.backCP.isEnabled()) { // in CPA GUI
				this.emptyCP.setEnabled(true);
			} else {
				this.unlockCP.setEnabled(true);
				this.rulesCP.setEnabled(true);
				this.resetCP.setEnabled(true);
				this.debugCP.setEnabled(true);
				this.checkHostGraphCP.setEnabled(true);
			} 
			fireParserEvent(new ParserMessageEvent(this,
					" Please select a pair of rules to see results."));
			
		} else if ((e.getMessage().indexOf("Checking Host Graph ") != -1)
			&& (e.getMessage().indexOf("started") != -1)) {
			if (this.hostGraphCPA != null) {
				javax.swing.JOptionPane
				.showMessageDialog(
						this.parent,
						"Checking of the host graph has started. Please wait for the finish-message.",
						"  CPA  ", javax.swing.JOptionPane.INFORMATION_MESSAGE);
				
			}		
		} else if ((e.getMessage().indexOf("Checking Host Graph") != -1)
				&& (e.getMessage().indexOf("finished") != -1)) {
//			System.out.println("CriticalPairAnalysis.parserEventOccured: "+e.getMessage());
			if (this.hostGraphCPA != null) {
				javax.swing.JOptionPane
				.showMessageDialog(
					this.parent,
					"Checking of the host graph finished. Please select a rule pair to see results.",
					"  CPA  ", javax.swing.JOptionPane.INFORMATION_MESSAGE);
			}			
		} else if (e.getMessage().indexOf("rule pair") != -1) {
			this.pairsSaved = false;
			// System.out.println("CriticalPairAnalysis.parserEventOccured ::
			// rule pair ");
			if (!this.stopCP.isEnabled()) { // one rule pair computing
				if (e.getMessage().indexOf("done") == -1) {
					this.startCP.setEnabled(false);
					this.loadCP.setEnabled(false);
					if (this.backCP.isEnabled()) {
//						reduceCP.setEnabled(false);
						this.consistCP.setEnabled(false);
						this.saveCP.setEnabled(false);
						this.showCP.setEnabled(false);
						this.checkHostGraphCP.setEnabled(false);
					}
				} else if (e.getMessage().indexOf("done") != -1) {
					this.startCP.setEnabled(true);
					this.loadCP.setEnabled(true);
					if (this.backCP.isEnabled()) {
//						reduceCP.setEnabled(true);
						this.consistCP.setEnabled(true);
						this.saveCP.setEnabled(true);
						this.showCP.setEnabled(true);
						this.checkHostGraphCP.setEnabled(true);
					}
				}
			} else {
				// System.out.println("Parser event:: --> stopCP.isEnabled
				// DISABLE it! ");
				// stopCP.setEnabled(false);
			}
		} else if ((e.getMessage().indexOf("Critical") != -1)
				&& (e.getMessage().indexOf("stopped") != -1)) {
			this.pairsSaved = false;
			updateCPAgraph();
		}
	}

	/* Implements agg.gui.parser.event.OptionListener */
	public void optionEventOccurred(agg.gui.parser.event.OptionEvent e) {
		// System.out.println("CriticalPairAnalysis.optionEventOccurred
		// (agg.gui.parser.event.OptionListener) :: "+e.getSource());
		if (e.getSource() instanceof JCheckBox) {
			JCheckBox cb = (JCheckBox) e.getSource();
			// System.out.println(cb.getText()+" "+cb.isSelected());
			if (cb.getText().equals("NACs")) {
				this.cpOption.enableNacs(cb.isSelected());
			} else if (cb.getText().equals("PACs")) {
				this.cpOption.enablePacs(cb.isSelected());
			}
		}
	}

	/* Implements agg.parser.OptionEventListener */
	public void optionEventOccurred(EventObject e) {
		if (e.getSource() instanceof CriticalPairOption) {
			if (this.pairsContainer != null) {
				setCPoptions((ExcludePairContainer) this.pairsContainer);
				
				if (this.pairsContainer instanceof LayeredExcludePairContainer) {
					((LayeredExcludePairContainer) this.pairsContainer)
							.setLayer(this.cpOption.getLayer());
				}
			}
			if (this.pairsContainer2 != null) {
				((DependencyPairContainer) this.pairsContainer2).
					enableSwitchDependency(this.cpOption.switchDependencyEnabled());
				setCPoptions((ExcludePairContainer) this.pairsContainer2);
				
				if (this.pairsContainer2 instanceof LayeredDependencyPairContainer) {
					((LayeredDependencyPairContainer) this.pairsContainer2)
							.setLayer(this.cpOption.getLayer());
				}
			}
		}
		// separated frames
		Enumeration<JFrame> en = this.separatedFrames.keys();
		while (en.hasMoreElements()) {
			Object key = en.nextElement();
			CriticalPairAnalysisSeparated cpas = this.separatedFrames.get(key);
			ExcludePairContainer excludePC = cpas
					.getPairContainer(CriticalPairOption.EXCLUDEONLY);
			ExcludePairContainer excludePC2 = cpas
					.getPairContainer(CriticalPairOption.TRIGGER_DEPEND);
			// System.out.println(excludePC);
			if (excludePC != null) {
				setCPoptions(excludePC);

//				if (excludePC instanceof LayeredExcludePairContainer) {
					// ((LayeredExcludePairContainer)excludePC).setLayer(this.cpOption.getLayer());
//				}
			}
			if (excludePC2 != null) {
				((DependencyPairContainer) excludePC2).
				enableSwitchDependency(this.cpOption.switchDependencyEnabled());
				setCPoptions(excludePC2);
				
//				if (excludePC2 instanceof LayeredDependencyPairContainer) {
					// ((LayeredDependencyPairContainer)excludePC2).setLayer(this.cpOption.getLayer());
//				}
			}
		}
	}

	private void setCPoptions(ExcludePairContainer pc) {
		pc.enableComplete(this.cpOption.completeEnabled());
		pc.enableReduce(this.cpOption.reduceEnabled());
		pc.enableConsistent(this.cpOption.consistentEnabled());
		pc.enableIgnoreIdenticalRules(this.cpOption.ignoreIdenticalRulesEnabled());
		pc.enableReduceSameMatch(this.cpOption.reduceSameMatchEnabled());
		pc.enableStrongAttrCheck(this.cpOption.strongAttrCheckEnabled());
		pc.enableEqualVariableNameOfAttrMapping(
				this.cpOption.equalVariableNameOfAttrMappingEnabled());
		pc.enableNamedObjectOnly(this.cpOption.namedObjectEnabled());
		pc.enableMaxBoundOfCriticKind(this.cpOption.getMaxBoundOfCriticKind());
		
		if (!(pc instanceof DependencyPairContainer)) {
			pc.enableDirectlyStrictConfluent(this.cpOption.directlyStrictConflEnabled());
			pc.enableDirectlyStrictConfluentUpToIso(
					this.cpOption.directlyStrictConflUpToIsoEnabled());
		}
	}
	
	/** Creates the Critical Pair menu. */
	protected void createAnalysisMenu() {
		/* create Critical pair menu */
		this.pairsMenu = new JMenu("Critical Pair Analysis");
		this.pairsMenu.setMnemonic('C');
		
		this.resetCP = new JMenuItem("Reset");
		this.resetCP.setEnabled(false);
		this.resetCP.setMnemonic('s');
		this.pairsMenu.add(this.resetCP);
		resetCPaddActionListener();

		this.unlockCP = new JMenuItem("Unlock");
		this.unlockCP.setEnabled(false);
		this.unlockCP.setMnemonic('U');
		this.pairsMenu.add(this.unlockCP);
		unlockCPaddActionListener();
		
		this.rulesCP = new JMenuItem("Set Rules");
		this.rulesCP.setEnabled(false);
//		this.rulesCP.setMnemonic('s');
		this.pairsMenu.add(this.rulesCP);
		rulesCPaddActionListener();
		this.pairsMenu.addSeparator();
		
		this.startCP = new JMenu("Generate");
		this.startCP.setMnemonic('G');
		this.startCPconflicts = new JMenuItem("Conflicts");
		this.startCPconflicts.setMnemonic('C');
		this.startCPdependencies = new JMenuItem("Dependencies");
		this.startCPdependencies.setMnemonic('D');
		this.startCP.add(this.startCPconflicts);
		this.startCP.add(this.startCPdependencies);
		this.startCP.setEnabled(false);
		this.pairsMenu.add(this.startCP);
		startCPaddActionListener();
		this.pairsMenu.addSeparator();
		
		this.stopCP = new JMenuItem("Stop");
		this.stopCP.setEnabled(false);
		this.stopCP.setMnemonic('o');
		this.pairsMenu.add(this.stopCP);
		stopCPaddActionListener();
		
		this.emptyCP = new JMenuItem("Empty");
		this.emptyCP.setEnabled(false);
		this.emptyCP.setMnemonic('y');
		this.pairsMenu.add(this.emptyCP);
		emptyCPaddActionListener();
		this.pairsMenu.addSeparator();
		
		this.debugCP = new JMenuItem("Debug");
		this.debugCP.setEnabled(false);
		this.debugCP.setMnemonic('D');
		this.pairsMenu.add(this.debugCP);
		debugCPaddActionListener();
		this.pairsMenu.addSeparator();
		
		this.consistCP = new JMenuItem("Check Consistency");
		this.consistCP.setEnabled(false);
		this.consistCP.setMnemonic('k');
		this.pairsMenu.add(this.consistCP);
		consistCPaddActionListener();

		this.checkHostGraphCP = new JMenuItem("Check Host Graph");
		this.checkHostGraphCP.setEnabled(false);
		this.checkHostGraphCP.setMnemonic(KeyEvent.VK_H);
		this.pairsMenu.add(this.checkHostGraphCP);
		checkHostGraphCPaddActionListener();
		this.pairsMenu.addSeparator();

		this.loadCP = new JMenu("Load");
		this.loadCP.setMnemonic('L');
		this.pairsMenu.add(this.loadCP);
		this.loadCPcpx = new JMenuItem("In This Window");
		this.loadCPcpx.setMnemonic('i');
		this.loadCPcpx.setDisplayedMnemonicIndex(5);
		this.loadSeparateCPcpx = new JMenuItem("In New Window");
		this.loadSeparateCPcpx.setMnemonic('N');
		this.loadSeparateCPcpx.setDisplayedMnemonicIndex(3);
		this.loadCP.add(this.loadCPcpx);
		this.loadCP.add(this.loadSeparateCPcpx);
		this.loadCPaddActionListener();
		
		this.saveCP = new JMenuItem("Save");
		this.saveCP.setEnabled(false);
		this.saveCP.setMnemonic('v');
		this.pairsMenu.add(this.saveCP);
		saveCPaddActionListener();
		
		this.showCP = new JMenu("Show");
		this.showCP.setMnemonic('w');
		this.showCP.setEnabled(false);
		this.showConflictCP = this.showCP.add(new JMenuItem("Conflicts"));
		this.showConflictCP.setMnemonic('i');
		this.showDependencyCP = this.showCP.add(new JMenuItem("Dependencies"));
		this.showDependencyCP.setMnemonic('n');
		this.cpaCombiGraphCP = this.showCP.add(new JMenuItem("CPA Graph"));
		this.cpaCombiGraphCP.setMnemonic('G');
		this.pairsMenu.add(this.showCP);
		showCPaddActionListener();
		this.pairsMenu.addSeparator();
		
//		this.consistCP = new JMenuItem("Check Consistency");
//		this.consistCP.setEnabled(false);
//		this.consistCP.setMnemonic('k');
//		this.pairsMenu.add(this.consistCP);
//		consistCPaddActionListener();
//
//		this.checkHostGraphCP = new JMenuItem("Check Host Graph");
//		this.checkHostGraphCP.setEnabled(false);
//		this.checkHostGraphCP.setMnemonic(KeyEvent.VK_H);
//		this.pairsMenu.add(this.checkHostGraphCP);
//		checkHostGraphCPaddActionListener();
//		this.pairsMenu.addSeparator();
		
		this.backCP = new JMenuItem("Back to AGG Editor");
		this.backCP.setEnabled(false);
		this.backCP.setMnemonic('b');
		this.pairsMenu.add(this.backCP);
		backCPaddActionListener();

		this.menus.addElement(this.pairsMenu);
	}

	private void rulesCPaddActionListener() {
		this.rulesCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setRulesForCPA(CriticalPairAnalysis.this.selectedGraGra);
			}
		});
	}
	
	private SelectRulesForCPAnalysisDialog rulesDialog;
	
	void setRulesForCPA(EdGraGra selGraGra) {
		if (resetGraGra(this.selectedGraGra)) {
			this.rulesDialog = new SelectRulesForCPAnalysisDialog(this.parent, 
					this.selectedGraGra.getBasisGraGra(), 
														new Point(300, 300));
			this.rulesDialog.setVisible(true);
			
			this.ruleList1 = this.rulesDialog.getExtendedRuleList1(); // columns
			this.ruleList2 = this.rulesDialog.getExtendedRuleList2(); // rows
			
			this.debugCPA();
		}
	}
	
	private void resetCPaddActionListener() {
		this.resetCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetCPAGraGra(CriticalPairAnalysis.this.selectedGraGra);
			}
		});
	}

	void resetCPAGraGra(EdGraGra selGraGra) {
		if (resetGraGra(this.selectedGraGra)) {
			if (this.selectedGraGra.getBasisGraGra().isLayered())
				this.cpOptionGUI.initLayers(this.selectedGraGra.getBasisGraGra()
						.getEnabledLayers());
		}
	}

	boolean resetGraGra(EdGraGra selGraGra) {
		if (!this.backCP.isEnabled()) { // remain in main_GUI
			if (selGraGra == null)
				return false;
			if (!doAllowInheritance(selGraGra.getBasisGraGra())) {
				inheritanceWarning();
				return false;
			}
			if (!areRulesInjective(selGraGra.getBasisGraGra())
					|| !checkIfReadyToTransform(selGraGra)) {
				return false;
			}
			if (this.pairsGUI.getGraGra() != null) {
				if (this.pairsGUI.isOnePairThreadAlive())
					this.pairsGUI.stopOnePairThread();
				if (!this.stopCP.isEnabled()) {
					removeEventListenersFromPairContainer(this.pairsContainer);
					removeEventListenersFromPairContainer(this.pairsContainer2);
					this.pairsGUI.reinitGraphDesktop();
					this.pairsContainer = null;
					this.pairsContainer2 = null;
					this.cpaGraph = null;
					this.conflictDependGraph = null;
					this.pairsGraGra = selGraGra;
					this.gragraChanged.put(this.pairsGraGra, new Boolean(this.pairsGraGra
							.isChanged()));
					this.pairsGUI.setGraGra(this.pairsGraGra);
					this.pairsSaved = true;
					this.resetDone = true;
				} else {
					javax.swing.JOptionPane
							.showMessageDialog(
									null,
									"Critical pair computation is running!.\nPlease wait or stop the computation.",
									"Warning",
									javax.swing.JOptionPane.WARNING_MESSAGE);
				}
				this.startCP.setEnabled(true);
			}
		}
		return true;
	}

	protected void unlockAllGraGras() {
		if (this.pairsGraGra != null 
				&& this.pairsGraGra.getBasisGraGra() != null
				&& this.pairsGraGra != this.selectedGraGra) {
			this.pairsGraGra.setEditable(true);
		} 
		if (this.selectedGraGra != null) {
			this.selectedGraGra.setEditable(true);
		}
		Enumeration<EdGraGra> keys = this.gragraChanged.keys();
		while (keys.hasMoreElements()) {
			EdGraGra gra = keys.nextElement();
			if (gra.getBasisGraGra() != null) {
				gra.setEditable(true);
				if (this.gragraChanged.get(gra) != null)
					gra.setChanged(this.gragraChanged.get(gra).booleanValue());
				else
					gra.setChanged(false);
			}
		}
		this.gragraChanged.clear();
	}

	private void unlockCPaddActionListener() {
		this.unlockCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!CriticalPairAnalysis.this.backCP.isEnabled()) { // remain in main_GUI
					unlockAllGraGras();
					
					((AttrTupleManager) agg.attribute.impl.AttrTupleManager
							.getDefaultManager()).setVariableContext(false);
					if (CriticalPairAnalysis.this.selectedGraGra != null) {
						CriticalPairAnalysis.this.selectedGraGra.setEditable(true);
						CriticalPairAnalysis.this.selectedGraGra.updateRules();
					}

					CriticalPairAnalysis.this.isLocked = false;
					fireParserEvent(new ParserMessageEvent(this,
							"Grammar isn't locked anymore."));
				}
			}
		});
	}

	private void startCPaddActionListener() {
		// startCP.addActionListener(new ActionListener(){
		this.startCPconflicts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				unlockAllGraGras();
				generateConflicts();
			}
		});

		this.startCPdependencies.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				unlockAllGraGras();
				generateDependencies();
			}
		});
	}

	public boolean compareCurrentGraGraToSelectedGraGra() {
		if (this.selectedGraGra == null || this.pairsGraGra == null
				|| this.pairsGraGra.getBasisGraGra() == null)
			return true;
		if (this.pairsGraGra == this.selectedGraGra) {	
			if (this.cpOption.layeredEnabled()
					&& (this.cpOption.layeredEnabled() != layerUsed())) {
				this.message = "layered";
				return false;
			}
			
			return true;
			
		}
		if (!this.pairsGraGra.getBasisGraGra().compareTo(
							this.selectedGraGra.getBasisGraGra(), true)
				&& !this.pairsSaved)
			return false;		
		else if ((this.cpOption.layeredEnabled() && !layerUsed()) && !this.pairsSaved)
			return false;
		else if ((!this.cpOption.layeredEnabled() && layerUsed()) && !this.pairsSaved)
			return false;
		else
			return true;
	}

	public void generateConflicts() {
		if (!compareCurrentGraGraToSelectedGraGra()) {
			int answer = gragraWarning("");
			if (answer == JOptionPane.YES_OPTION) {
				saveCriticalPairs();
			} else if (answer == 2)
				return;

			if (!resetGraGra(this.selectedGraGra)) {
				javax.swing.JOptionPane.showMessageDialog(null,
						"Something gone wrong. Cannot set selected grammar",
						"Grammar failed.",
						javax.swing.JOptionPane.WARNING_MESSAGE);
				return;
			}
		} else if (!this.isLocked)
			resetWarning(false);

		if ((this.pairsGraGra == null) || (this.pairsGraGra.getBasisGraGra() == null)) {
			javax.swing.JOptionPane.showMessageDialog(null,
					"There isn't any graph grammar available.", "Warning",
					javax.swing.JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		if (!doAllowInheritance(this.pairsGraGra.getBasisGraGra())) {
			inheritanceWarning();
			return;
		}
		if (!areRulesInjective(this.pairsGraGra.getBasisGraGra())) {
			return;
		}
		if (!this.changer.isSet()) { // main_GUI , change to CP_GUI
			if (checkIfReadyToTransform(this.pairsGraGra)) {
				this.cpOption
						.setCriticalPairAlgorithm(CriticalPairOption.EXCLUDEONLY);
				this.cpOptionGUI.update();
				this.pairsContainer = generateNew(this.pairsContainer);
				this.changeToCPAgui(this.pairsGUI.getContainer());
			}
		} else { // remain in CP_GUI
			this.cpOption.setCriticalPairAlgorithm(CriticalPairOption.EXCLUDEONLY);
			this.cpOptionGUI.update();
			if (this.pairsContainer == null)
				this.pairsContainer = generateNew(this.pairsContainer);
			else
				generate(this.pairsContainer);
		}
	}

	public void generateDependencies() {
		if (!compareCurrentGraGraToSelectedGraGra()) {
			int answer = gragraWarning("");
			if (answer == JOptionPane.YES_OPTION) {
				saveCriticalPairs();
			} else if (answer == 2)
				return;

			if (!resetGraGra(this.selectedGraGra)) {
				javax.swing.JOptionPane.showMessageDialog(null,
						"Something gone wrong. Cannot set selected grammar.",
						"Grammar failed",
						javax.swing.JOptionPane.WARNING_MESSAGE);
				return;
			}
		} else if (!this.isLocked)
			resetWarning(false);

		if ((this.pairsGraGra == null) || (this.pairsGraGra.getBasisGraGra() == null)) {
			javax.swing.JOptionPane.showMessageDialog(null,
					"No graph grammar is available.", "Warning",
					javax.swing.JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (!doAllowInheritance(this.pairsGraGra.getBasisGraGra())) {
			inheritanceWarning();
			return;
		}
		if (!areRulesInjective(this.pairsGraGra.getBasisGraGra())) {
			return;
		}
		if (!this.changer.isSet()) { // main_GUI , change to CP_GUI
			if (checkIfReadyToTransform(this.pairsGraGra)) {
				if (this.cpOption.getCriticalPairAlgorithm() == CriticalPairOption.EXCLUDEONLY) {
					this.cpOption
						.setCriticalPairAlgorithm(CriticalPairOption.TRIGGER_DEPEND);
					this.cpOptionGUI.update();
				}
				this.pairsContainer2 = generateNew(this.pairsContainer2);
				this.changeToCPAgui(this.pairsGUI.getContainer());
			}
		} else {// remain in CP_GUI
//			this.cpOption.setCriticalPairAlgorithm(CriticalPairOption.TRIGGER_DEPEND);
//			this.cpOptionGUI.update();
			if (this.cpOption.getCriticalPairAlgorithm() == CriticalPairOption.EXCLUDEONLY) {
				this.cpOption
					.setCriticalPairAlgorithm(CriticalPairOption.TRIGGER_DEPEND);
				this.cpOptionGUI.update();
			}
			if (this.pairsContainer2 == null)
				this.pairsContainer2 = generateNew(this.pairsContainer2);

			else
				generate(this.pairsContainer2);
		}
	}

	private PairContainer generateNew(PairContainer pcontainer) {
		PairContainer pc = pcontainer;
		if (pc == null) {
			resetLayerFunction();
			pc = makeEmptyCriticalPairs(this.cpOption.getCriticalPairAlgorithm());
			if (pc == null) {
				javax.swing.JOptionPane.showMessageDialog(null,
						"Generating critical pairs failed.", "Warning",
						javax.swing.JOptionPane.WARNING_MESSAGE);
				return null;
			}
			if (checkIfReadyToTransform(this.pairsGraGra)) {
				
				if (this.ruleList1 != null && this.ruleList2 != null) {
					pc.setRules(this.ruleList1, this.ruleList2);
				}
				
				resetCP_GUI(this.pairsGraGra, pc, true);
				this.rulesCP.setEnabled(false);
				this.resetCP.setEnabled(false);
				this.unlockCP.setEnabled(false);
				this.debugCP.setEnabled(false);
				this.backCP.setEnabled(true);

				this.changeToCPAgui(this.pairsGUI.getContainer());
				// start a new CP-Thread
				this.pairsGraGra.setEditable(false);
				this.isLocked = true;
				
				ParserFactory.generateCriticalPairs(pc);				
				fireParserEvent(new ParserMessageEvent(this,
						"Generate critical pairs ...  Please wait ..."));

				this.startCP.setEnabled(false);
				this.stopCP.setEnabled(true);
				this.emptyCP.setEnabled(false);
				this.consistCP.setEnabled(false);
				this.loadCP.setEnabled(false);
				this.saveCP.setEnabled(false);
				this.showCP.setEnabled(false);
				this.checkHostGraphCP.setEnabled(false);
			} else {
				javax.swing.JOptionPane.showMessageDialog(null,
						"Generating critical pairs failed.", "Warning",
						javax.swing.JOptionPane.WARNING_MESSAGE);
				this.backCP.setEnabled(true);
				return null;
			}
		} else { // pairContainer exists
			this.changeToCPAgui(this.pairsGUI.getContainer());
			generate(pc);
		}
		return pc;
	}

	private void generate(PairContainer pc) {
		if (pc != null && pc.isAlive()) {
			javax.swing.JOptionPane.showMessageDialog(null,
					"Generating is already running.", "Warning",
					javax.swing.JOptionPane.WARNING_MESSAGE);
			return;
		}
		fireParserEvent(new ParserMessageEvent(this,
				"Generating critical pairs ... "));
		if (pc != null && ((ExcludePairContainer) pc).isComputed()) {
			this.rulesCP.setEnabled(false);
			this.resetCP.setEnabled(false);
			this.unlockCP.setEnabled(false);
			this.startCP.setEnabled(true);
			this.stopCP.setEnabled(false);
			this.emptyCP.setEnabled(true);
			this.consistCP.setEnabled(true);
			this.loadCP.setEnabled(true);
			this.saveCP.setEnabled(true);
			this.showCP.setEnabled(true);
			this.checkHostGraphCP.setEnabled(true);
			this.backCP.setEnabled(true);
			fireParserEvent(new ParserMessageEvent(this,
					"Generate critical pairs ... Done"));
		} else if (this.pairsGUI.isOnePairThreadAlive()) {
			this.rulesCP.setEnabled(false);
			this.resetCP.setEnabled(false);
			this.unlockCP.setEnabled(false);
			this.startCP.setEnabled(false);
			this.stopCP.setEnabled(true);
			this.emptyCP.setEnabled(true);
			this.consistCP.setEnabled(false);
			this.loadCP.setEnabled(false);
			this.saveCP.setEnabled(false);
			this.showCP.setEnabled(false);
			this.checkHostGraphCP.setEnabled(false);
			this.backCP.setEnabled(true);
			fireParserEvent(new ParserMessageEvent(this,
					"Generating critical pairs of the selected rules is still running ..."));
		} else {
			if (checkIfReadyToTransform(this.pairsGraGra)) {
				this.pairsGraGra.setEditable(false);
				this.isLocked = true;
				PairContainer pc_tmp = null;
				if (resetLayerFunction()) {
					pc_tmp = makeEmptyCriticalPairs(this.cpOption
							.getCriticalPairAlgorithm());
					if (pc_tmp == null) {
						javax.swing.JOptionPane.showMessageDialog(null,
								"Generating critical pairs failed.",
								"Warning",
								javax.swing.JOptionPane.WARNING_MESSAGE);
						return;
					}
					if (pc_tmp.getKindOfConflict() == CriticalPair.CONFLICT)
						this.pairsContainer = pc_tmp;
					else if (pc_tmp.getKindOfConflict() == CriticalPair.TRIGGER_DEPENDENCY)
						this.pairsContainer2 = pc_tmp;
				}
				if (pc_tmp != null) {
					if (this.ruleList1 != null && this.ruleList2 != null) {
						pc_tmp.setRules(this.ruleList1, this.ruleList2);
					}
					resetCP_GUI(this.pairsGraGra, pc_tmp, false);
					ParserFactory.generateCriticalPairs(pc_tmp);
					
				} else {
					if (pc != null 
							&& this.ruleList1 != null && this.ruleList2 != null) {
						pc.setRules(this.ruleList1, this.ruleList2);
					}
					
					resetCP_GUI(this.pairsGraGra, pc, false);
					ParserFactory.generateCriticalPairs(pc);
				}
				this.rulesCP.setEnabled(false);
				this.resetCP.setEnabled(false);
				this.unlockCP.setEnabled(false);
				this.startCP.setEnabled(false);
				this.stopCP.setEnabled(true);
				this.emptyCP.setEnabled(false);
				this.consistCP.setEnabled(false);
				this.loadCP.setEnabled(false);
				this.saveCP.setEnabled(false);
				this.showCP.setEnabled(false);
				this.checkHostGraphCP.setEnabled(false);
				this.backCP.setEnabled(true);
				fireParserEvent(new ParserMessageEvent(this,
						"Generating critical pairs ... Please wait ..."));
			} else {
				javax.swing.JOptionPane.showMessageDialog(null,
						"Generating critical pairs failed.", "Warning",
						javax.swing.JOptionPane.WARNING_MESSAGE);
				fireParserEvent(new ParserMessageEvent(this,
						"Generating critical pairs failed. "));
				this.backCP.setEnabled(true);
				return;
			}
		}
	}

	private void stopCPaddActionListener() {
		this.stopCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PairContainer pc = CriticalPairAnalysis.this.pairsGUI.getActivePairContainer();
				if (pc != null && pc.isAlive())
					pc.stop();
				else {
					if (CriticalPairAnalysis.this.pairsContainer != null) {
						if (CriticalPairAnalysis.this.pairsContainer.isAlive())
							CriticalPairAnalysis.this.pairsContainer.stop();
						else if (CriticalPairAnalysis.this.pairsContainer.getActiveExcludePair() != null)
							CriticalPairAnalysis.this.pairsContainer.getActiveExcludePair().stop();							
					}
					if (CriticalPairAnalysis.this.pairsContainer2 != null) { 
						if (CriticalPairAnalysis.this.pairsContainer2.isAlive())
							CriticalPairAnalysis.this.pairsContainer2.stop();
						else if (CriticalPairAnalysis.this.pairsContainer2.getActiveExcludePair() != null)
							CriticalPairAnalysis.this.pairsContainer2.getActiveExcludePair().stop();
					}
				}
				if (!CriticalPairAnalysis.this.backCP.isEnabled()) { // main_GUI
					CriticalPairAnalysis.this.rulesCP.setEnabled(true);
					CriticalPairAnalysis.this.resetCP.setEnabled(true);
					CriticalPairAnalysis.this.unlockCP.setEnabled(true);
					CriticalPairAnalysis.this.startCP.setEnabled(true);
					CriticalPairAnalysis.this.stopCP.setEnabled(false);
					CriticalPairAnalysis.this.debugCP.setEnabled(true);
					CriticalPairAnalysis.this.emptyCP.setEnabled(false);
//					reduceCP.setEnabled(false);
					CriticalPairAnalysis.this.consistCP.setEnabled(false);
					CriticalPairAnalysis.this.loadCP.setEnabled(true);
					CriticalPairAnalysis.this.saveCP.setEnabled(false);
					CriticalPairAnalysis.this.showCP.setEnabled(true);
					CriticalPairAnalysis.this.checkHostGraphCP.setEnabled(true);
				} else { // CP_GUI
					CriticalPairAnalysis.this.startCP.setEnabled(true);
					CriticalPairAnalysis.this.stopCP.setEnabled(false);
					CriticalPairAnalysis.this.emptyCP.setEnabled(true);
//					reduceCP.setEnabled(true);
					CriticalPairAnalysis.this.consistCP.setEnabled(true);
					CriticalPairAnalysis.this.loadCP.setEnabled(true);
					CriticalPairAnalysis.this.saveCP.setEnabled(true);
					CriticalPairAnalysis.this.showCP.setEnabled(true);
					CriticalPairAnalysis.this.checkHostGraphCP.setEnabled(true);
				}
			}
		});
	}

	private void consistCPaddActionListener() {
		this.consistCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (CriticalPairAnalysis.this.changer.isSet()) { // CP_GUI
					PairContainer pc = CriticalPairAnalysis.this.pairsGUI.getActivePairContainer();
					if (pc == null) {
						if (CriticalPairAnalysis.this.pairsContainer != null 
								&& CriticalPairAnalysis.this.pairsContainer2 == null)
							pc = CriticalPairAnalysis.this.pairsContainer;
						else if (CriticalPairAnalysis.this.pairsContainer2 != null
								&& CriticalPairAnalysis.this.pairsContainer == null)
							pc = CriticalPairAnalysis.this.pairsContainer2;
					}
					
					if (pc == CriticalPairAnalysis.this.pairsContainer) {
						if (!CriticalPairAnalysis.this.pairsContainer.isEmpty()) {
							if (!CriticalPairAnalysis.this.pairsContainer.getGrammar().getConstraints().hasMoreElements()) {
								JOptionPane.showMessageDialog(null, 
										"Nothing to check. Any constraint doesn't exist.");
								return;
							}
							
							Thread t = new Thread() {
								public void run() {
									((ExcludePairContainer) CriticalPairAnalysis.this.pairsContainer)
											.checkConsistency();
								}
							};
							t.setPriority(4);
							t.start();
							while (t.isAlive()) {}

							CriticalPairAnalysis.this.pairsGUI.setCriticalPairs(CriticalPairAnalysis.this.pairsContainer);
						}
					} else if (pc == CriticalPairAnalysis.this.pairsContainer2) {
						if (!CriticalPairAnalysis.this.pairsContainer2.isEmpty()) {
							if (!CriticalPairAnalysis.this.pairsContainer2.getGrammar().getConstraints().hasMoreElements()) {
								JOptionPane.showMessageDialog(null, 
										"Nothing to check. Any constraint doesn't exist.");
								return;
							}
							
							Thread t = new Thread() {
								public void run() {
									((ExcludePairContainer) CriticalPairAnalysis.this.pairsContainer2)
											.checkConsistency();
								}
							};
							t.setPriority(4);
							t.start();
							while (t.isAlive()) {}

							CriticalPairAnalysis.this.pairsGUI.setCriticalPairs(CriticalPairAnalysis.this.pairsContainer2);
						}
					} else
						javax.swing.JOptionPane
								.showMessageDialog(
										null,
										"Cannot check. \nPlease select a pair table first.",
										"",
										javax.swing.JOptionPane.WARNING_MESSAGE);
				}
			}
		});
	}

	private void emptyCPaddActionListener() {
		this.emptyCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (CriticalPairAnalysis.this.backCP.isEnabled()) { // in CP_GUI
					if (CriticalPairAnalysis.this.pairsGUI.isOnePairThreadAlive())
						CriticalPairAnalysis.this.pairsGUI.stopOnePairThread();
					PairContainer pc = CriticalPairAnalysis.this.pairsGUI.getActivePairContainer();
					String title = "Rule Conflicts";
					if (pc == null) {
						if (CriticalPairAnalysis.this.pairsContainer != null 
								&& CriticalPairAnalysis.this.pairsContainer2 == null) {
							pc = CriticalPairAnalysis.this.pairsContainer;
							title = "Rule Conflicts";
						}
						else if (CriticalPairAnalysis.this.pairsContainer2 != null
								&& CriticalPairAnalysis.this.pairsContainer == null) {
							pc = CriticalPairAnalysis.this.pairsContainer2;
							title = "Rule Dependencies";
						}
					} else if (pc == CriticalPairAnalysis.this.pairsContainer) {
						title = "Rule Conflicts";
					} else if (pc == CriticalPairAnalysis.this.pairsContainer2) {
						title = "Rule Dependencies";
					}
					
					if (pc != null) {
						int answer = JOptionPane.YES_OPTION;
						if (pc.isComputed() || !pc.isEmpty()) {
							answer = emptyWarning(title);
						}
						if (answer == JOptionPane.YES_OPTION) {
							pc.clear();
							pc.refreshOptions(cpOption);
							
							if (pc.getKindOfConflict() == CriticalPair.CONFLICT)
								CriticalPairAnalysis.this.pairsGUI.getGraphDesktop()
										.getConflictPairPanel().refreshView();
							else
								CriticalPairAnalysis.this.pairsGUI.getGraphDesktop().getDependPairPanel()
										.refreshView();
							CriticalPairAnalysis.this.pairsGUI.update();
							fireParserEvent(new ParserMessageEvent(
									this,
									"Empty critical pairs generated. "
											+ "Choose a pair of rules to check it. "));
							CriticalPairAnalysis.this.startCP.setEnabled(true);
							CriticalPairAnalysis.this.loadCP.setEnabled(true);
							CriticalPairAnalysis.this.saveCP.setEnabled(true);
						}
					} else
						javax.swing.JOptionPane
								.showMessageDialog(
										null,
										"Cannot make empty. \nPlease select a pair table first.",
										"",
										javax.swing.JOptionPane.WARNING_MESSAGE);
				}
			}
		});
	}

	void showPairContainer(int kindOfConflict) {
		if (kindOfConflict == CriticalPair.CONFLICT) {
			if (this.pairsContainer == null) {
				this.pairsContainer = makeEmptyCriticalPairs(CriticalPairOption.EXCLUDEONLY);
				resetCP_GUI(this.pairsGraGra, this.pairsContainer, true);
				fireParserEvent(new ParserMessageEvent(this,
						"Empty critical pairs generated. "
								+ "Choose a pair of rules to check it. "));
			} else {
				this.pairsGUI.getGraphDesktop().addCriticalPairTable(
						this.pairsGUI.getGraphDesktop().getConflictPairPanel(), "");
			}
			this.startCP.setEnabled(true);
			this.loadCP.setEnabled(true);
			this.saveCP.setEnabled(true);
		} else if (kindOfConflict == CriticalPair.TRIGGER_DEPENDENCY) {
			if (this.pairsContainer2 == null) {
				this.pairsContainer2 = makeEmptyCriticalPairs(CriticalPairOption.TRIGGER_DEPEND);
				resetCP_GUI(this.pairsGraGra, this.pairsContainer2, true);
				fireParserEvent(new ParserMessageEvent(this,
						"Empty critical pairs generated. "
								+ "Choose a pair of rules to check it. "));
			} else {
				this.pairsGUI.getGraphDesktop().addCriticalPairTable(
						this.pairsGUI.getGraphDesktop().getDependPairPanel(), "");
			}
			this.startCP.setEnabled(true);
			this.loadCP.setEnabled(true);
			this.saveCP.setEnabled(true);
		}
	}

	int emptyWarning(String obj) {
		Object[] options = { "YES", "NO" };
		int answer = JOptionPane
				.showOptionDialog(
						null,
						"Are you sure, you want to delete already computed rule pairs?",
						"Delete   " + obj, JOptionPane.DEFAULT_OPTION,
						JOptionPane.WARNING_MESSAGE, null, options, options[1]);
		return answer;
	}

	int gragraWarning(String obj) {
		Object[] options = { "YES", "NO", "CANCEL" };
		String s = "";
		if (obj != null && !obj.equals(""))
			s = "\n( " + obj + " )";
		int answer = JOptionPane
				.showOptionDialog(
						null,
						"Currently selected grammar is different from the grammar of critical pairs."
								+ s
								+ "\nCritical pairs are not empty. You can lose results."
								+ "\nDo you want to save the results first?",
						"Different grammar   ", JOptionPane.DEFAULT_OPTION,
						JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		return answer;
	}

	private void saveCPaddActionListener() {
		this.saveCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (CriticalPairAnalysis.this.pairsGUI.isGenerating()) {
					fireParserEvent(new ParserMessageEvent(this,
							"Cannot save.  " + "Computing is still running ..."));
					return;
				}
				if (CriticalPairAnalysis.this.pairsGUI.isOnePairThreadAlive()) {
					fireParserEvent(new ParserMessageEvent(this,
							"Cannot save.  " + "Computing is still running ..."));
					return;
				}
				if (CriticalPairAnalysis.this.changer.isSet()) {
					saveCriticalPairs();
				}
			}
		});
	}

	void saveCriticalPairs() {
		ConflictsDependenciesContainerSaveLoad cdPC = new ConflictsDependenciesContainerSaveLoad(
				this.pairsContainer, this.pairsContainer2, this.cpaGraph, this.pairsGraGra);
		
		this.pairsIOGUI.setCriticalPairContainer(cdPC);
		this.pairsIOGUI.save();
		if (this.pairsIOGUI.fileIsSaved()) {
			this.treeView.setFileDirectory(this.pairsIOGUI.getDirectoryName());
			fireParserEvent(new ParserMessageEvent(this,
					"Critical pairs are saved."));
			this.pairsSaved = true;
		}
		this.pairsSaved = true;
	}

	private void debugCPaddActionListener() {
		this.debugCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				CriticalPairAnalysis.this.debugCPA();
			}
		});
	}

	void debugCPA() {
		if ((this.pairsGraGra == null)
				|| (this.pairsGraGra.getBasisGraGra() == null)) {
			javax.swing.JOptionPane.showMessageDialog(null,
					"There isn't any graph grammar available.", "Warning",
					javax.swing.JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (!doAllowInheritance(this.pairsGraGra.getBasisGraGra())) {
			inheritanceWarning();
			return;
		}
		if (!areRulesInjective(this.pairsGraGra.getBasisGraGra())
				|| !checkIfReadyToTransform(this.pairsGraGra)) {
			return;
		}

		if (!this.changer.isSet()) { // main_GUI, change to CP_GUI
			if (!compareCurrentGraGraToSelectedGraGra()) {
				int answer = gragraWarning("");
				// System.out.println("answer: "+answer);
				if (answer == JOptionPane.YES_OPTION) {
					saveCriticalPairs();
				} else if (answer == 2)
					return;

				setGraGra(this.selectedGraGra);
				if (!resetGraGra(this.selectedGraGra)) {
					javax.swing.JOptionPane
							.showMessageDialog(
									null,
									"Something gone wrong. Cannot set selected grammar",
									"Grammar failed.",
									javax.swing.JOptionPane.WARNING_MESSAGE);
					return;
				}
			} 
			else if (!this.isLocked && !this.resetDone) {
				resetWarning(true);
			}
			
			unlockAllGraGras();

			// locking gragra
			this.pairsGraGra.setEditable(false);
			this.isLocked = !this.pairsGraGra.isEditable();
			this.changeToCPAgui(this.pairsGUI.getContainer());

			this.rulesCP.setEnabled(false);
			this.resetCP.setEnabled(false);
			this.unlockCP.setEnabled(false);
			this.debugCP.setEnabled(false);
			this.emptyCP.setEnabled(true);
			this.consistCP.setEnabled(true);
			this.saveCP.setEnabled(true);
			this.showCP.setEnabled(true);
			this.checkHostGraphCP.setEnabled(true);
			this.backCP.setEnabled(true);

			if (this.pairsContainer == null
					&& this.cpOption.getCriticalPairAlgorithm() 
								== CriticalPairOption.EXCLUDEONLY) {
				resetLayerFunction();
				this.pairsContainer = makeEmptyCriticalPairs(
						this.cpOption.getCriticalPairAlgorithm());
				if (this.pairsContainer == null) {
					javax.swing.JOptionPane.showMessageDialog(null,
							"Creating empty critical pairs failed.",
							"Warning",
							javax.swing.JOptionPane.WARNING_MESSAGE);
					return;
				}
				
				if (this.ruleList1 != null && this.ruleList2 != null) {
					this.pairsContainer.setRules(this.ruleList1, this.ruleList2);
				}
				
				resetCP_GUI(this.pairsGraGra, this.pairsContainer, true);
				fireParserEvent(new ParserMessageEvent(this,
						"Choose a pair of rules to check it."));
			} else if (this.pairsContainer2 == null
					&& (this.cpOption.getCriticalPairAlgorithm() 
											== CriticalPairOption.TRIGGER_DEPEND
							|| this.cpOption.getCriticalPairAlgorithm() 
											== CriticalPairOption.TRIGGER_SWITCH_DEPEND)) {
				resetLayerFunction();
				this.pairsContainer2 = makeEmptyCriticalPairs(this.cpOption
						.getCriticalPairAlgorithm());
				if (this.pairsContainer2 == null) {
					javax.swing.JOptionPane.showMessageDialog(null,
							"Creating empty critical pairs failed.",
							"Warning",
							javax.swing.JOptionPane.WARNING_MESSAGE);
					return;
				}
				
				if (this.ruleList1 != null && this.ruleList2 != null) {
					this.pairsContainer2.setRules(this.ruleList1, this.ruleList2);						
				}
				
				resetCP_GUI(this.pairsGraGra, CriticalPairAnalysis.this.pairsContainer2, true);
				fireParserEvent(new ParserMessageEvent(this,
						"Choose a pair of rules to check it."));
			} else { // pairsContainer or pairsContainer2 exists
				if (this.pairsGUI.pairsComputed()) {
					this.saveCP.setEnabled(true);
					fireParserEvent(new ParserMessageEvent(this,
							"Generating critical pairs ... Done"));
				} else if (this.pairsGUI.isOnePairThreadAlive()) {
					this.pairsGUI.getCriticalPairPanel().refreshView();
					this.pairsGUI.getCriticalPairPanel2().refreshView();
					this.stopCP.setEnabled(true);
					this.startCP.setEnabled(false);
					this.emptyCP.setEnabled(true);
					this.consistCP.setEnabled(false);
					this.loadCP.setEnabled(false);
					this.saveCP.setEnabled(false);
					this.showCP.setEnabled(false);
					this.checkHostGraphCP.setEnabled(false);
					fireParserEvent(new ParserMessageEvent(this,
							"Generating critical pairs of the selected rule pair is still running ..."));
				} else if (this.stopCP.isEnabled()) {
					CriticalPairAnalysis.this.emptyCP.setEnabled(false);
					this.consistCP.setEnabled(false);
					this.loadCP.setEnabled(false);
					this.saveCP.setEnabled(false);
					this.showCP.setEnabled(false);
					this.checkHostGraphCP.setEnabled(false);
					fireParserEvent(new ParserMessageEvent(this,
							"Generating critical pairs ... Continuing ..."));
				} else {
					fireParserEvent(new ParserMessageEvent(this,
							"Please choose a pair of rules to check it. "));
					this.startCP.setEnabled(true);
					this.emptyCP.setEnabled(true);
					this.consistCP.setEnabled(true);
					this.loadCP.setEnabled(true);
					this.saveCP.setEnabled(true);
					this.showCP.setEnabled(true);
					this.checkHostGraphCP.setEnabled(true);
				}
			}
		}
	}
	
	private void loadCPaddActionListener() {
		this.loadCPcpx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				unlockAllGraGras();
				
				if (loadPairContainer(".cpx") 
						&& CriticalPairAnalysis.this.pairsGraGra != null) {
					Object[] options = { "YES", "NO" };
					int answer = JOptionPane.showOptionDialog(null, 
							"Do you want to put the grammar of critical pairs into GraGras treeview?", 
							"Critical Pair Anlysis",
							JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
							options, options[0]);
					if (answer == JOptionPane.YES_OPTION) {
//						pairsGraGra.makeLayoutOfBaseGraphs();
						CriticalPairAnalysis.this.treeView.addGraGra(CriticalPairAnalysis.this.pairsGraGra);					
					}
				}
			}
		});

		this.loadSeparateCPcpx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final CriticalPairAnalysisSeparated separatedFrame = loadPairContainerSeparated(".cpx");
				
				if (separatedFrame.getGraGra() != null) {
					Object[] options = { "YES", "NO" };
					int answer = JOptionPane.showOptionDialog(null, 
							"Do you want to put the grammar of critical pairs into GraGras treeview?", 
							"Critical Pair Anlysis",
							JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
							options, options[0]);
					if (answer == JOptionPane.YES_OPTION) {
						separatedFrame.getGraGra().setChanged(false);
//						separatedFrame.getGraGra().makeLayoutOfBaseGraphs();
						CriticalPairAnalysis.this.treeView.addGraGra(separatedFrame.getGraGra());
					}
					separatedFrame.toFront();
				}
			}
		});

		/*
		 * loadCPdpx.addActionListener(new ActionListener(){ public void
		 * actionPerformed(ActionEvent e){ loadPairContainer(".dpx"); }});
		 * 
		 * loadSeparateCPdpx.addActionListener(new ActionListener(){ public void
		 * actionPerformed(ActionEvent e){ loadPairContainerSeparated(".dpx");
		 * }});
		 */
	}

	private void checkHostGraphCPaddActionListener() {
		this.checkHostGraphCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (CriticalPairAnalysis.this.treeView.getCurrentGraGra() != null) {
					if (CriticalPairAnalysis.this.pairsGUI.isOnePairThreadAlive())
						return;
					if (CriticalPairAnalysis.this.pairsGUI.isGenerating())
						return;
					if (!doAllowInheritance(CriticalPairAnalysis.this.treeView.getCurrentGraGra().getBasisGraGra())) {
						inheritanceWarning();
						return;
					}
					
					if (!CriticalPairAnalysis.this.treeView.getCurrentGraGra()
							.getBasisGraGra()
							.isGraphReadyForTransform()) {
						javax.swing.JOptionPane
						.showMessageDialog(
								null,
								" It was not possible to check the host graph.\nMaybe not all attributes of the host graph are set.",
								"Warning",
								javax.swing.JOptionPane.WARNING_MESSAGE);
						return;
					}
						
					int kind = CriticalPair.CONFLICT;
					if (CriticalPairAnalysis.this.pairsContainer == null && CriticalPairAnalysis.this.pairsContainer2 == null)
						CriticalPairAnalysis.this.hostGraphCPA = new CriticalPairAnalysisSeparated(
								CriticalPairAnalysis.this.parent, CriticalPairAnalysis.this.pairsIOGUI, CriticalPairAnalysis.this.cpOptionGUI, CriticalPairAnalysis.this.cpOption,
								CriticalPairAnalysis.this.lOption, CriticalPairAnalysis.this.pOption, CriticalPairAnalysis.this.option);
					else if (CriticalPairAnalysis.this.pairsContainer != null && CriticalPairAnalysis.this.pairsContainer2 != null) {
						if (CriticalPairAnalysis.this.pairsGUI.getActivePairContainer() != null)
							kind = CriticalPairAnalysis.this.pairsGUI.getActivePairContainer()
									.getKindOfConflict();
						if (kind == CriticalPair.CONFLICT) {
							CriticalPairAnalysis.this.hostGraphCPA = new CriticalPairAnalysisSeparated(
									(JFrame) CriticalPairAnalysis.this.parent,
									(ExcludePairContainer) CriticalPairAnalysis.this.pairsContainer,
									null, 
									CriticalPairAnalysis.this.cpOption, 
									CriticalPairAnalysis.this.lOption, 
									CriticalPairAnalysis.this.pOption, 
									CriticalPairAnalysis.this.option);
						}
						else if (kind == CriticalPair.TRIGGER_DEPENDENCY
								|| kind == CriticalPair.TRIGGER_SWITCH_DEPENDENCY) {
							CriticalPairAnalysis.this.hostGraphCPA = null;
							javax.swing.JOptionPane.showMessageDialog(null,
									"Sorry. To check rule dependency at the host graph isn't possible.", "Warning",
									javax.swing.JOptionPane.WARNING_MESSAGE);							
						}
					} else if (CriticalPairAnalysis.this.pairsContainer != null
							&& !CriticalPairAnalysis.this.pairsContainer.isEmpty()) {
						CriticalPairAnalysis.this.hostGraphCPA = new CriticalPairAnalysisSeparated(
								(JFrame) CriticalPairAnalysis.this.parent,
								(ExcludePairContainer) CriticalPairAnalysis.this.pairsContainer, 
								null,
								CriticalPairAnalysis.this.cpOption, 
								CriticalPairAnalysis.this.lOption, 
								CriticalPairAnalysis.this.pOption, 
								CriticalPairAnalysis.this.option);
					} else if (CriticalPairAnalysis.this.pairsContainer2 != null
							&& !CriticalPairAnalysis.this.pairsContainer2.isEmpty()) {
						CriticalPairAnalysis.this.hostGraphCPA = null;
						javax.swing.JOptionPane.showMessageDialog(null,
								"Sorry. To check rule dependency at the host graph isn't possible.", "Warning",
								javax.swing.JOptionPane.WARNING_MESSAGE);						
					} else {
						javax.swing.JOptionPane.showMessageDialog(null,
								"Sorry. There is nothing to check.", "Warning",
								javax.swing.JOptionPane.WARNING_MESSAGE);
						return;
					}

					if (CriticalPairAnalysis.this.hostGraphCPA != null) {

						if (CriticalPairAnalysis.this.changer.isSet())
							backToMainGUI(true);

						CriticalPairAnalysis.this.hostGraphFrame = CriticalPairAnalysis.this.hostGraphCPA.getFrame();
						CriticalPairAnalysis.this.separatedFrames.put(CriticalPairAnalysis.this.hostGraphFrame, CriticalPairAnalysis.this.hostGraphCPA);
						CriticalPairAnalysis.this.hostGraphFrame.addWindowListener(CriticalPairAnalysis.this.wl);
						CriticalPairAnalysis.this.x = CriticalPairAnalysis.this.x + 100;
						CriticalPairAnalysis.this.y = 50;
						CriticalPairAnalysis.this.hostGraphCPA.setLocation(CriticalPairAnalysis.this.x, CriticalPairAnalysis.this.y);
						fireParserEvent(new ParserMessageEvent(this,
								"Critical pairs are loaded. Checking the host graph. Please wait ..."));

						if (CriticalPairAnalysis.this.hostGraphCPA.getCriticalPairAnalysisGUI()
								.getCriticalPairPanel(kind) != null) {
							CriticalPairAnalysis.this.hostGraphCPA.getCriticalPairAnalysisGUI()
									.getGraphDesktop()
									.addParserGUIListener(cpa);
							CriticalPairAnalysis.this.hostGraphCPA.getCriticalPairAnalysisGUI()
									.getCriticalPairPanel(kind)
									.addParserGUIListener(cpa);

							ExcludePairContainer 
							epc = CriticalPairAnalysis.this.hostGraphCPA.getPairContainer(kind);
							if (epc != null) {
								if (CriticalPairAnalysis.this.treeView
										.getCurrentGraGra()
										.getBasisGraGra()
										.getTypeSet()
										.contains(epc.getGrammar().getTypeSet())) {
									epc.enableUseHostGraph(
													true,
													CriticalPairAnalysis.this.treeView.getCurrentGraGra()
															.getBasisGraGra()
															.getGraph(),
													CriticalPairAnalysis.this.treeView
															.getCurrentGraGra()
															.getBasisGraGra()
															.getMorphismCompletionStrategy());
										
										ParserFactory.generateCriticalPairs(epc);
										while (epc.isAlive()) {}
										
										fireParserEvent(new ParserMessageEvent(
												this, " "));
										CriticalPairAnalysis.this.startCP.setEnabled(true);
										CriticalPairAnalysis.this.stopCP.setEnabled(false);
										CriticalPairAnalysis.this.emptyCP.setEnabled(true);
//										reduceCP.setEnabled(true);
										CriticalPairAnalysis.this.consistCP.setEnabled(true);
										CriticalPairAnalysis.this.loadCP.setEnabled(true);
										CriticalPairAnalysis.this.saveCP.setEnabled(true);
										CriticalPairAnalysis.this.showCP.setEnabled(true);
										CriticalPairAnalysis.this.checkHostGraphCP.setEnabled(true);

										CriticalPairAnalysis.this.hostGraphCPA
												.getCriticalPairAnalysisGUI()
												.getGraphDesktop()
												.setIconOfCPAGraph(true);
										CriticalPairAnalysis.this.hostGraphCPA
												.getCriticalPairAnalysisGUI()
												.getGraphDesktop()
												.setIconOfPairTable(
														CriticalPairAnalysis.this.hostGraphCPA
																.getCriticalPairAnalysisGUI()
																.getCriticalPairPanel2(),
														true);
										
										CriticalPairAnalysis.this.hostGraphCPA.showFrame();										
								} else {
									CriticalPairAnalysis.this.separatedFrames.remove(CriticalPairAnalysis.this.hostGraphFrame);
									CriticalPairAnalysis.this.hostGraphFrame.dispose();
									javax.swing.JOptionPane
											.showMessageDialog(
													null,
													"It was not possible to check the host graph.\nMismatch between types of the host graph and a CPA-grammar.",
													"Warning",
													javax.swing.JOptionPane.WARNING_MESSAGE);
								}
							}
						}
					}
				} else
					javax.swing.JOptionPane.showMessageDialog(null,
							"There isn't any graph to check.", "Warning",
							javax.swing.JOptionPane.WARNING_MESSAGE);
			}
		});
	}

	private void showCPaddActionListener() {
		this.showConflictCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showPairContainer(CriticalPair.CONFLICT);
			}
		});

		this.showDependencyCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showPairContainer(CriticalPair.TRIGGER_DEPENDENCY);
			}
		});

		showCPAGraphCPaddActionListener();
	}

	private void showCPAGraphCPaddActionListener() {
		this.cpaCombiGraphCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showCPAGraph();
			}
		});
	}

	public void showCPAGraph() {
		if (this.pairsContainer != null || this.pairsContainer2 != null) {
			if (this.cpaGraph != null) {
				if (this.conflictDependGraph != null) {
					if (this.pairsContainer != null)
						this.pairsGUI.getGraphDesktop().getConflictPairPanel()
								.removeParserGUIListener(this.conflictDependGraph);
					if (this.pairsContainer2 != null)
						this.pairsGUI.getGraphDesktop().getDependPairPanel()
								.removeParserGUIListener(this.conflictDependGraph);
					this.pairsGUI.getGraphDesktop()
							.removeActionListenerFromCPAGraphMenu(
									this.conflictDependGraph);
					
					this.conflictDependGraph.dispose();
					this.cpaGraph = null;
				}

				this.pairsGUI.getGraphDesktop().removeAllGraphFrames();
				this.pairsGUI.getGraphDesktop().removeRuleFrames();
				this.pairsGUI.getGraphDesktop().removeCPAGraphFrame();
				this.pairsGUI.getGraphDesktop().getDesktop().repaint();

				this.conflictDependGraph = new ConflictsDependenciesGraph(
						(ExcludePairContainer) this.pairsContainer,
						(ExcludePairContainer) this.pairsContainer2, null, false);
				
				this.cpaGraph = this.conflictDependGraph.getGraph();
				if (this.cpaGraph != null) {
					
					if (this.pairsContainer != null)
						this.pairsGUI.getGraphDesktop().getConflictPairPanel()
								.addParserGUIListener(this.conflictDependGraph);
					if (this.pairsContainer2 != null)
						this.pairsGUI.getGraphDesktop().getDependPairPanel()
								.addParserGUIListener(this.conflictDependGraph);
					this.pairsGUI.getGraphDesktop().addActionListenerToCPAGraphMenu(
							this.conflictDependGraph);
					this.conflictDependGraph.setGraphDesktop(this.pairsGUI.getGraphDesktop());
					try {
						this.pairsGUI.getGraphDesktop().addGraph(this.cpaGraph, 400, 300)
								.setIcon(false);
					} catch (java.beans.PropertyVetoException pve) {}
					this.pairsGUI.getGraphDesktop().refresh();
				}
			} else {
				this.conflictDependGraph = new ConflictsDependenciesGraph(
						(ExcludePairContainer) this.pairsContainer,
						(ExcludePairContainer) this.pairsContainer2);
				
				this.cpaGraph = this.conflictDependGraph.getGraph();
				if (this.cpaGraph != null) {
					if (this.pairsContainer != null)
						this.pairsGUI.getGraphDesktop().getConflictPairPanel()
								.addParserGUIListener(this.conflictDependGraph);
					if (this.pairsContainer2 != null)
						this.pairsGUI.getGraphDesktop().getDependPairPanel()
								.addParserGUIListener(this.conflictDependGraph);
					this.pairsGUI.getGraphDesktop().addActionListenerToCPAGraphMenu(
							this.conflictDependGraph);
					this.conflictDependGraph.setGraphDesktop(this.pairsGUI
							.getGraphDesktop());
										
					this.pairsGUI.getGraphDesktop().getDesktop().repaint();
					try {
						this.pairsGUI.getGraphDesktop().addGraph(this.cpaGraph, 400, 300)
								.setIcon(false);
					} catch (java.beans.PropertyVetoException pve) {}
										
				} else
					javax.swing.JOptionPane.showMessageDialog(null,
							"There is nothing to show.", "Warning",
							javax.swing.JOptionPane.WARNING_MESSAGE);
			}
		} else {
			loadPairContainerSeparated(".cpx");
		}
	}

	private void backCPaddActionListener() {
		this.backCP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((AttrTupleManager) agg.attribute.impl.AttrTupleManager
						.getDefaultManager()).setVariableContext(false);
				if (CriticalPairAnalysis.this.changer.isSet()) {
					backToMainGUI(false);
				}
			}
		});
	}

	void backToMainGUI(boolean warning) {
		if (warning) {
			Object[] options = { "YES", "NO" };
			int answer = JOptionPane
					.showOptionDialog(
							null,
							"Do you want to change to graph editor to observe the host graph?",
							"", JOptionPane.DEFAULT_OPTION,
							JOptionPane.WARNING_MESSAGE, null, options,
							options[1]);
			if (answer != JOptionPane.YES_OPTION)
				return;
		}

		this.changeToAGGgui();
		if (!this.stopCP.isEnabled()) {
			this.rulesCP.setEnabled(true);
			this.resetCP.setEnabled(true);
			this.unlockCP.setEnabled(true);
			this.debugCP.setEnabled(true);
			this.emptyCP.setEnabled(false);
		} else {
			this.rulesCP.setEnabled(false);
			this.resetCP.setEnabled(false);
			this.unlockCP.setEnabled(false);
			this.debugCP.setEnabled(true);
			this.emptyCP.setEnabled(false);
			this.loadCP.setEnabled(false);
		}
//		reduceCP.setEnabled(false);
		this.consistCP.setEnabled(false);
		this.saveCP.setEnabled(false);
		this.showCP.setEnabled(false);
		this.backCP.setEnabled(false);

		//test
		this.pairsGraGra.setEditable(true);
		this.pairsGraGra.getBasisGraGra().removeShiftedApplConditionsFromMultiRules();
		
		if (this.pairsContainer != null)
			this.pairsContainer.restoreExprReplacedByVarInApplConds();
		else if (this.pairsContainer2 != null)
			this.pairsContainer2.restoreExprReplacedByVarInApplConds();
		
		if ((this.pairsContainer instanceof LayeredExcludePairContainer)
				|| (this.pairsContainer2 instanceof LayeredDependencyPairContainer)) {
			this.treeView.getTreeModel().ruleNameChanged(this.pairsGraGra, true);
			this.treeView.getTree().treeDidChange();
		}
		this.pairsGraGra.update();
		if (this.gragraChanged.get(this.pairsGraGra) != null)
			this.pairsGraGra.setChanged(this.gragraChanged.get(this.pairsGraGra)
					.booleanValue());
		else
			this.pairsGraGra.setChanged(false);
		this.resetDone = false;
		fireParserEvent(new ParserMessageEvent(this,
				"back to AGG editor ... The grammar  <" + this.pairsGraGra.getName()
						+ ">  is still used by CPA."));
	}

	private void updateCPAgraph() {
		if (this.cpaGraph != null) {
			if (this.conflictDependGraph != null) {
				this.conflictDependGraph.updateGraphAlongPairContainer();
				this.cpaGraph.makeGraphObjectsOfNewBasisObjects(false);
				this.cpaGraph.setTransformChangeEnabled(true);
				this.cpaGraph.updateGraph();
				this.cpaGraph.setTransformChangeEnabled(false);
				this.pairsGUI.getGraphDesktop().refresh();
			}
		}
	}

	public void occured(ParserGUIEvent e) {
		if (e.getSource() instanceof agg.gui.cpa.CriticalPairPanel) {
			this.overlapGraphs = null;
//			if (this.pairsGUI.getCriticalPairs() != null)
//				this.pairsGUI.getCriticalPairs().refreshOptions(cpOption);
//			if (this.pairsGUI.getCriticalPairs2() != null)
//				this.pairsGUI.getCriticalPairs2().refreshOptions(cpOption);
		}

		if (e.getSource() instanceof GraphDesktop) {
			this.graphDesktop = (GraphDesktop) e.getSource();
			if (e.getData() instanceof Graph) {
				if (this.overlapGraphs == null)
					this.overlapGraphs = new Hashtable<Graph, Pair<Vector<Hashtable<GraphObject, GraphObject>>, JButton>>();
				
				this.overlapGraph = (Graph) e.getData();	
				
				if (this.overlapGraphs.get(this.overlapGraph) == null) {
					
					ExcludePairContainer epc = this.hostGraphCPA
							.getPairContainer(CriticalPair.CONFLICT);
					if (epc == null) {
						this.overlapGraphs = null;
						return;
					}
					Hashtable<Graph, Vector<Hashtable<GraphObject, GraphObject>>> 
					ht = epc.getExcludeContainerForTestGraph();

					Vector<Hashtable<GraphObject, GraphObject>> 
					matches = ht.get(this.overlapGraph);	
					
					if (matches != null) {						
						this.hostGraphMappings = new Vector<Hashtable<GraphObject, GraphObject>>(
								matches.size());
						this.hostGraphMappings.addAll(matches);
						if (this.hostGraphMappings.size() > 1) {
							this.nextMatchAtHostGraphButton = this.graphDesktop
									.addNextButton(this.overlapGraph,
											"Click here to get next match at host graph");
							this.nextMatchAtHostGraphButton.addActionListener(this);
						} else
							this.nextMatchAtHostGraphButton = null;

						Pair<Vector<Hashtable<GraphObject, GraphObject>>, JButton> 
						pair = new Pair<Vector<Hashtable<GraphObject, GraphObject>>, JButton>(
								this.hostGraphMappings, this.nextMatchAtHostGraphButton);
						this.overlapGraphs.put(this.overlapGraph, pair);

						if (this.hostGraphMappings.size() > 0) {
							Hashtable<GraphObject, GraphObject> 
							objs = this.hostGraphMappings.elementAt(0);
							
							showCriticalMatch(this.treeView.getCurrentGraGra()
									.getGraph(), this.graphDesktop
									.getInternalLayoutGraph(this.overlapGraph), objs);
							this.treeView.graphDidChange();
							this.hostGraphMappings.remove(0);							
						} else {
							showCriticalMatch(this.treeView.getCurrentGraGra()
									.getGraph(), null, null);
							this.treeView.graphDidChange();
						}
					} else {
						showCriticalMatch(this.treeView.getCurrentGraGra()
								.getGraph(), null, null);
						this.treeView.graphDidChange();
					}
				} else {
					Pair<Vector<Hashtable<GraphObject, GraphObject>>, JButton> 
					pair = this.overlapGraphs.get(this.overlapGraph);
					
					this.hostGraphMappings = pair.first;
					this.nextMatchAtHostGraphButton = pair.second;
					 
					if (this.hostGraphMappings != null
							&& this.hostGraphMappings.size() > 0) {
						Hashtable<GraphObject, GraphObject> 
						objs = this.hostGraphMappings.elementAt(0);
					
						showCriticalMatch(this.treeView.getCurrentGraGra()
								.getGraph(), this.graphDesktop
								.getInternalLayoutGraph(this.overlapGraph), objs);
						this.treeView.graphDidChange();
						this.hostGraphMappings.removeElementAt(0);						
					}
				}
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			if (((JButton) e.getSource()) == this.nextMatchAtHostGraphButton) {
				if (this.hostGraphMappings != null && this.hostGraphMappings.size() > 0) {
					Hashtable<GraphObject, GraphObject> 
					objs = this.hostGraphMappings.elementAt(0);
					
					showCriticalMatch(this.treeView.getCurrentGraGra().getGraph(),
							this.graphDesktop.getInternalLayoutGraph(this.overlapGraph),
							objs);
					this.treeView.graphDidChange();
					this.hostGraphMappings.remove(0);
				} else {
					JOptionPane.showMessageDialog(this.hostGraphFrame,
							"No more matches.");
				}
			}
		}
		else if (e.getSource() == this.cpOptionGUI.getComponentMaxBoundOfCriticKind()) {
			if (this.pairsContainer != null)
				this.pairsContainer.refreshOptions(this.cpOption);
			if (this.pairsContainer2 != null)
				this.pairsContainer2.refreshOptions(this.cpOption);
		}
		
	}

	public void refreshOptionsOfPairContainer() {
		if (this.pairsContainer != null)
			this.pairsContainer.refreshOptions(this.cpOption);
		if (this.pairsContainer2 != null)
			this.pairsContainer2.refreshOptions(this.cpOption);
	}
	
	public PairContainer getConflictPairContainer() {
		return this.pairsContainer;
	}
	
	public PairContainer getDependencyPairContainer() {
		return this.pairsContainer2;
	}
	
	public void newMessage(StatusMessageEvent sme) {
		if (sme.getMessage().indexOf("is running") >= 0) {
			this.stopCP.setEnabled(true);
		} else if (sme.getMessage().indexOf("finished") >= 0) {
			this.stopCP.setEnabled(false);
		}
	}

	protected boolean loadPairContainer(String filefilter) {
		if (this.pairsGUI.isOnePairThreadAlive())
			this.pairsGUI.stopOnePairThread();

		this.pairsIOGUI.setFileFilter(filefilter);
		this.pairsIOGUI.setDirectoryName(this.treeView.getFileDirectory(), "");
		
		Object o = this.pairsIOGUI.load(true);
		if (o == null) {
			this.cpOption.enableLayered(false);
			return false;
		}
		this.treeView.setFileDirectory(this.pairsIOGUI.getDirectoryName());
				
		if (this.pairsIOGUI.isCombined()) {
//			ConflictsDependenciesContainer cdc = (ConflictsDependenciesContainer) o;
			
			ConflictsDependenciesContainerSaveLoad cdc = (ConflictsDependenciesContainerSaveLoad) o;
						
			if (this.cpaGraph != null) {
				if (this.conflictDependGraph != null) {
					if (this.pairsContainer != null)
						this.pairsGUI.getGraphDesktop().getConflictPairPanel()
								.removeParserGUIListener(this.conflictDependGraph);
					if (this.pairsContainer2 != null)
						this.pairsGUI.getGraphDesktop().getDependPairPanel()
								.removeParserGUIListener(this.conflictDependGraph);
					this.pairsGUI.getGraphDesktop()
							.removeActionListenerFromCPAGraphMenu(
									this.conflictDependGraph);
					this.conflictDependGraph = null;
				}
				this.cpaGraph = null;
			}

			this.cpOption.setOptionsFromList(cdc.getLoadedCPAOptions());
			
			if (this.pairsContainer != null)
				removeEventListenersFromPairContainer(this.pairsContainer);
			if (this.pairsContainer2 != null)
				removeEventListenersFromPairContainer(this.pairsContainer2);
			this.pairsGUI.reinitGraphDesktop();

			this.pairsContainer = null;
			this.pairsContainer2 = null;
			this.pairsGUI.getGraphDesktop().getDesktop().repaint();

			if (cdc.isPriority()) {
				if (cdc.getPriorityExcludePairContainer() != null)
					this.pairsContainer = cdc.getPriorityExcludePairContainer();
				if (cdc.getPriorityDependencyPairContainer() != null)
					this.pairsContainer2 = cdc.getPriorityDependencyPairContainer();
				this.cpOption.enablePriority(true);
				this.cpOption.enableLayered(false);
			} else if (cdc.isLayered()) {
				if (cdc.getLayeredExcludePairContainer() != null)
					this.pairsContainer = cdc.getLayeredExcludePairContainer();
				if (cdc.getLayeredDependencyPairContainer() != null)
					this.pairsContainer2 = cdc.getLayeredDependencyPairContainer();
				this.cpOption.enableLayered(true);
				this.cpOption.enablePriority(false);
			} else {
				if (cdc.getExcludePairContainer() != null) {
					this.pairsContainer = cdc.getExcludePairContainer();
					this.cpOption.setCriticalPairAlgorithm(this.pairsContainer.getKindOfConflict());
				}
				if (cdc.getDependencyPairContainer() != null) {
					this.pairsContainer2 = cdc.getDependencyPairContainer();
					this.cpOption.setCriticalPairAlgorithm(this.pairsContainer2.getKindOfConflict());
				}
				this.cpOption.enablePriority(false);
				this.cpOption.enableLayered(false);
			}

			if (cdc.getContainerCount() == 2) {
//				this.pairsGraGra = new EdGraGra(this.pairsContainer.getGrammar());
				this.pairsGraGra = cdc.getPairsGraGra();
				this.pairsGUI.setGraGra(this.pairsGraGra);
				resetCP_GUI(this.pairsGraGra, this.pairsContainer, true);
				resetCP_GUI(this.pairsGraGra,this. pairsContainer2, true);
			} else if (cdc.getContainerCount() == 1) {
				if (this.pairsContainer != null) {
//					test
//					((ExcludePairContainer)this.pairsContainer).reduceCriticalPairs();
					
//					this.pairsGraGra = new EdGraGra(this.pairsContainer.getGrammar());
					this.pairsGraGra = cdc.getPairsGraGra();
					this.pairsGUI.setGraGra(this.pairsGraGra);
					resetCP_GUI(this.pairsGraGra, this.pairsContainer, true);
				} else if (this.pairsContainer2 != null) {
//					this.pairsGraGra = new EdGraGra(this.pairsContainer2.getGrammar());
					this.pairsGraGra = cdc.getPairsGraGra();
					this.pairsGUI.setGraGra(this.pairsGraGra);
					resetCP_GUI(this.pairsGraGra, this.pairsContainer2, true);
				}
			}

			this.cpaGraph = cdc.getCPAGraph();
			if (this.cpaGraph != null) {
				this.conflictDependGraph = new ConflictsDependenciesGraph(
						(ExcludePairContainer) this.pairsContainer,
						(ExcludePairContainer) this.pairsContainer2, this.cpaGraph, true);
				
				this.conflictDependGraph.setGraphDesktop(this.pairsGUI.getGraphDesktop());
				this.pairsGUI.getGraphDesktop().addActionListenerToCPAGraphMenu(
						this.conflictDependGraph);
				
				if (this.pairsContainer != null)
					this.pairsGUI.getGraphDesktop().getConflictPairPanel()
							.addParserGUIListener(this.conflictDependGraph);
				if (this.pairsContainer2 != null)
					this.pairsGUI.getGraphDesktop().getDependPairPanel()
							.addParserGUIListener(this.conflictDependGraph);
			
				this.pairsGUI.getGraphDesktop().addGraph(this.cpaGraph, 400, 300);
				try {
					if (this.pairsGUI.getGraphDesktop().getInternalCPAGraphFrame().isIcon())
						this.pairsGUI.getGraphDesktop().getInternalCPAGraphFrame().setIcon(false);					
				} catch (java.beans.PropertyVetoException pve) {}

				this.pairsGUI.getGraphDesktop().refresh();
			}

			if (this.pairsGraGra != null) {
				this.pairsGraGra.setChanged(false);
				if (this.pairsGraGra.getBasisGraGra().isLayered())
					this.cpOptionGUI.initLayers(this.pairsGraGra.getBasisGraGra()
							.getEnabledLayers());
			}
			this.cpOptionGUI.update();
			
			if (!this.changer.isSet()) { // main_GUI, change to CP_GUI
				this.changeToCPAgui(this.pairsGUI.getContainer());

				this.rulesCP.setEnabled(false);
				this.resetCP.setEnabled(false);
				this.startCP.setEnabled(true);
				this.stopCP.setEnabled(false);
				this.debugCP.setEnabled(false);
				this.emptyCP.setEnabled(true);
//				reduceCP.setEnabled(true);
				this.consistCP.setEnabled(true);
				this.saveCP.setEnabled(true);
				this.backCP.setEnabled(true);
				this.showCP.setEnabled(true);
				this.checkHostGraphCP.setEnabled(true);
			}
			fireParserEvent(new ParserMessageEvent(this,
					"Critical pairs are loaded."));

			this.pairsSaved = true;
			this.pairsGraGra.setChanged(false);
			return true;
		} 
		fireParserEvent(new ParserMessageEvent(this,
					"Load ritical pairs failed."));
		return false;
	}

	protected  CriticalPairAnalysisSeparated loadPairContainerSeparated(String filefilter) {
		this.pairsIOGUI.setDirectoryName(this.treeView.getFileDirectory(), "");
		this.pairsIOGUI.setFileFilter(filefilter);
		final CriticalPairAnalysisSeparated separatedFrame = new CriticalPairAnalysisSeparated(
				this.parent, this.pairsIOGUI, this.cpOptionGUI, this.cpOption, this.lOption, this.pOption,
				this.option);
		if (separatedFrame.isReady()) {
			separatedFrame.setExportJPEG(this.exportJPEG);
			this.x = this.x + 100;
			this.y = 50;
			separatedFrame.setLocation(this.x, this.y);
			separatedFrame.showFrame();
			this.separatedFrames.put(separatedFrame.getFrame(), separatedFrame);
			separatedFrame.getFrame().addWindowListener(this.wl);
			fireParserEvent(new ParserMessageEvent(this,
					"Critical pairs are loaded."));
			this.treeView.setFileDirectory(this.pairsIOGUI.getDirectoryName());
		}
		
		return separatedFrame;
	}

	protected void inheritanceWarning() {
		javax.swing.JOptionPane
				.showMessageDialog(
						null,
						"Sorry!\nThis item is not available for the graph grammar \nwith node type inheritance.",
						"Warning", javax.swing.JOptionPane.WARNING_MESSAGE);
	}

	protected void resetWarning(boolean warn) {
		int answer = warn? -1: 0;
		if (warn) {
			Object[] options = { "Reset", "Keep" };
			answer = JOptionPane.showOptionDialog(null,
					"Do you want to reset the grammar for critical pair analysis?",
					"Warning", JOptionPane.DEFAULT_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		}
		if (answer == JOptionPane.YES_OPTION) {
			resetCPAGraGra(this.selectedGraGra);
		} else {
			this.gragraChanged.put(this.selectedGraGra, new Boolean(this.selectedGraGra
					.isChanged()));
		}
	}

	private void showCriticalMatch(EdGraph hostg, EdGraph overlapg,
			Hashtable<GraphObject, GraphObject> map) {
		// unset old criticals
		hostg.deselectAll();
		Iterator<?> en = hostg.getBasisGraph().getNodesSet().iterator();
		while (en.hasNext()) {
			GraphObject o = (GraphObject) en.next();
			o.setCritical(false);
			EdNode n = hostg.findNode(o);
			if (n != null)
				n.clearMorphismMark();			
		}
		en = hostg.getBasisGraph().getArcsSet().iterator();
		while (en.hasNext()) {
			GraphObject o = (GraphObject) en.next();
			o.setCritical(false);
			EdArc a = hostg.findArc(o);
			if (a != null)
				a.clearMorphismMark();
		}
		// set new criticals
		if( overlapg == null) {
			hostg.update();
			return;
		}
		
		Enumeration<?> en1 = map.keys();
		while (en1.hasMoreElements()) {
			GraphObject o = (GraphObject) en1.nextElement();
			GraphObject i = map.get(o);
			i.setCritical(true);
			EdNode no = overlapg.findNode(o);
			if (no != null) {
				EdNode ni = hostg.findNode(i);
				if (ni != null)
					ni.addMorphismMark(no.getMorphismMark());
			} else {
				EdArc ao = overlapg.findArc(o);
				if (ao != null) {
					EdArc ai = hostg.findArc(i);
					if (ai != null)
						ai.addMorphismMark(ao.getMorphismMark());
				}
			}
			// System.out.println("Critical obj of host graph set");
		}

		// select critical graph objects
		for (int i = 0; i < hostg.getNodes().size(); i++) {
			EdNode n = hostg.getNodes().elementAt(i);
			if (n.getBasisNode().isCritical())
				hostg.setSelectedNode(n);
		}
		for (int i = 0; i < hostg.getArcs().size(); i++) {
			EdArc a = hostg.getArcs().elementAt(i);
			if (a.getBasisArc().isCritical())
				hostg.setSelectedArc(a);
		}
		
		hostg.update();
	}

	// -----------------------------------------------------------------------+
	/**
	 * Adds a CP Analysis Event Listener.
	 * 
	 * @param l
	 *            The listener.
	 */
	public void addCPAnalysisEventListener(ParserEventListener l) {
		if (!this.listener.contains(l))
			this.listener.add(0, l);
	}

	// -----------------------------------------------------------------------+
	/**
	 * Removes a CP Analysis Event Listener
	 * 
	 * @param l
	 *            The listener.
	 */
	public void removeCPAnalysisEventListener(ParserEventListener l) {
		if (this.listener.contains(l))
			this.listener.removeElement(l);
	}

	// ***********************************************************************+
	/**
	 * Sends a event to all its listeners.
	 * 
	 * @param event
	 *            The event which will be sent
	 */
	synchronized void fireParserEvent(ParserEvent e) {
		for (int i = 0; i < this.listener.size(); i++)
			this.listener.elementAt(i).parserEventOccured(e);
	}

	/**
	 * register your <CODE>StatusMessageListener</CODE> to receive messages
	 * 
	 * @param l
	 *            the listener which listen to my messages
	 */
	public void addStatusMessageListener(StatusMessageListener l) {
		if (!this.pmlistener.contains(l))
			this.pmlistener.add(0, l);
		this.pairsGUI.addStatusMessageListener(l);
	}


	protected boolean resetLayerFunction() {
		if (this.cpOption.layeredEnabled()) {
			if (((this.pairsContainer == null) || (this.pairsContainer.getLayer() == null))
					&& ((this.pairsContainer2 == null) || (this.pairsContainer2
							.getLayer() == null))) {
				RuleLayer tmpRL = new RuleLayer(this.pairsGraGra.getBasisGraGra().getEnabledRules());
				this.rlayer = new RuleLayer(this.pairsGraGra.getBasisGraGra().getEnabledRules()); //getListOfRules());
				LayerGUI lgui = new LayerGUI(this.parent, this.rlayer);
				lgui.showGUI();
				if (lgui.isCancelled()) {
					this.cpOption.enableLayered(false);
					this.cpOptionGUI.update();
				} else if (tmpRL.compareTo(this.rlayer)) {
					return false;
				}
				return true;
			} 
			return false;
			
		} else if (((this.pairsContainer != null) && (this.pairsContainer.getLayer() != null))
					|| ((this.pairsContainer2 != null) && (this.pairsContainer2
							.getLayer() != null))) {
			return true;
		} else {
			return false;
		}
	}

	protected PairContainer makeEmptyCriticalPairs(int kindOfAlgorithm) {
		if (kindOfAlgorithm == CriticalPairOption.EXCLUDEONLY) {
			if (this.pairsContainer != null) {
				this.pairsContainer.clear();
				this.pairsContainer.refreshOptions(cpOption);
				return this.pairsContainer;
			} 
			
			PairContainer pc = ParserFactory.createEmptyCriticalPairs(
					this.pairsGraGra.getBasisGraGra(), kindOfAlgorithm, this.cpOption
								.layeredEnabled());
			if (this.conflictDependGraph != null && pc != null) {
				pc.addPairEventListener(this.conflictDependGraph);
				this.conflictDependGraph.setConflictPairContainer(pc);
			}
			return pc;
			
		} else if (kindOfAlgorithm == CriticalPairOption.TRIGGER_DEPEND
				|| kindOfAlgorithm == CriticalPairOption.TRIGGER_SWITCH_DEPEND) {
			if (this.pairsContainer2 != null) {
				this.pairsContainer2.clear();
				this.pairsContainer2.refreshOptions(cpOption);
				return this.pairsContainer2;
			} 
			
			PairContainer pc = ParserFactory.createEmptyCriticalPairs(
					this.pairsGraGra.getBasisGraGra(), kindOfAlgorithm, this.cpOption
								.layeredEnabled());
			if (this.conflictDependGraph != null && pc != null) {
				pc.addPairEventListener(this.conflictDependGraph);
				this.conflictDependGraph.setDependencyPairContainer(pc);
			}

			return pc;
			
		} else
			return null;
	}

	void resetCP_GUI(EdGraGra gragra, PairContainer pc, boolean newpc) {
		if ((gragra == null) || gragra.getRules().isEmpty())
			return;

		if (pc != null) {
			this.pairsGUI.showGACsWarn = true;
			if (this.pairsGUI.getGraGra() == gragra) {
				if (newpc) {
					addEventListenersToPairContainer(pc);
					this.pairsGUI.setCriticalPairs(pc);
				} else {
					this.pairsGUI.update();
				}
				setCPoptions((ExcludePairContainer) pc);
				
				if (pc instanceof LayeredExcludePairContainer) {
					((LayeredExcludePairContainer) pc).setLayer(this.cpOption
							.getLayer());
				} else if (pc instanceof LayeredDependencyPairContainer) {
					((LayeredDependencyPairContainer) pc).setLayer(this.cpOption
							.getLayer());
				}
			}
		}
		for (int i = 0; i < this.separatedFrames.size(); i++) {
			if (this.separatedFrames.get(new Integer(i)) != null) {
				CriticalPairAnalysisSeparated cpas = this.separatedFrames
						.get(new Integer(i));
				ExcludePairContainer excludePC = cpas
						.getPairContainer(CriticalPairOption.EXCLUDEONLY);
				ExcludePairContainer excludePC2 = cpas
						.getPairContainer(CriticalPairOption.TRIGGER_DEPEND);
				
				if (excludePC != null) {
					setCPoptions(excludePC);
				}
				if (excludePC2 != null) {
					setCPoptions(excludePC2);
				}
			}
		}
	}

	boolean checkIfReadyToTransform(EdGraGra gragra) {
		Pair<Object, String> pair = gragra.getBasisGraGra().isReadyToTransform(true);
		if (pair != null && !(pair.first instanceof Graph)) {
			Object test = pair.first;
			if (test != null) {
				javax.swing.JOptionPane.showMessageDialog(null,
						"Cannot set the grammar to analyze.\n"
								+ pair.second, "Warning",
						javax.swing.JOptionPane.WARNING_MESSAGE);
				return false;
			}
		}
		return true;
	}

	private void removeEventListenersFromPairContainer(PairContainer pc) {
		if (pc == null)
			return;
		for (int i = 0; i < this.listener.size(); i++) {
			if (pc instanceof LayeredDependencyPairContainer) {
				((LayeredDependencyPairContainer) pc).stop();
				((LayeredDependencyPairContainer) pc)
						.removePairEventListener(this.listener.elementAt(i));
			} else if (pc instanceof LayeredExcludePairContainer) {
				((LayeredExcludePairContainer) pc).stop();
				((LayeredExcludePairContainer) pc)
						.removePairEventListener(this.listener.elementAt(i));
			} else if (pc instanceof DependencyPairContainer) {
				((DependencyPairContainer) pc).stop();
				((DependencyPairContainer) pc).removePairEventListener(this.listener
						.elementAt(i));
			} else if (pc instanceof ExcludePairContainer) {
				((ExcludePairContainer) pc).stop();
				((ExcludePairContainer) pc).removePairEventListener(this.listener
						.elementAt(i));
			}
		}
	}

	private void addEventListenersToPairContainer(PairContainer pc) {
		for (int i = 0; i < this.listener.size(); i++) {
			if (pc instanceof LayeredDependencyPairContainer)
				((LayeredDependencyPairContainer) pc)
						.addPairEventListener(this.listener.elementAt(i));
			else if (pc instanceof LayeredExcludePairContainer)
				((LayeredExcludePairContainer) pc)
						.addPairEventListener(this.listener.elementAt(i));
			else if (pc instanceof PriorityDependencyPairContainer)
				((PriorityDependencyPairContainer) pc)
						.addPairEventListener(this.listener.elementAt(i));
			else if (pc instanceof PriorityExcludePairContainer)
				((PriorityExcludePairContainer) pc)
						.addPairEventListener(this.listener.elementAt(i));
			else if (pc instanceof DependencyPairContainer)
				((DependencyPairContainer) pc).addPairEventListener(this.listener
						.elementAt(i));
			else if (pc instanceof ExcludePairContainer)
				((ExcludePairContainer) pc).addPairEventListener(this.listener
						.elementAt(i));
		}
	}

	private boolean layerUsed() {
		if ((this.pairsContainer != null)
				&& (this.pairsContainer instanceof LayeredExcludePairContainer))
			return true;
		else if ((this.pairsContainer2 != null)
				&& (this.pairsContainer2 instanceof LayeredDependencyPairContainer))
			return true;
		else if ((this.pairsContainer == null) && (this.pairsContainer2 == null))
			return true;
		else
			return false;
	}

	void resetRuleApplicable(GraGra gra) {
		for (int i = 0; i < gra.getListOfRules().size(); i++) {
			Rule r = gra.getListOfRules().get(i);
			r.setApplicable(true);
		}
	}

	boolean doAllowInheritance(GraGra gra) {
		return (!gra.getTypeSet().usesInheritance() || this.allowNodeTypeInheritance);
	}
	
	boolean areRulesInjective(GraGra gra) {
		Vector<Rule> noninjectives = gra.getNonInjectiveRules();
		String text = "\n[ ";
		for (int i = 0; i < noninjectives.size(); i++) {
			text = text + noninjectives.get(i).getName() + "  ";
		}
		text = text + "]";
		if (!gra.getNonInjectiveRules().isEmpty()) {
			javax.swing.JOptionPane
					.showMessageDialog(
							null,
							"Sorry!\nThis item isn't available for the graph grammar \nwith non-injective rules."
									+ text, "Warning",
							javax.swing.JOptionPane.WARNING_MESSAGE);
			return false;
		} 
		return true;
	}

	private void changeToCPAgui(Component c) {
		this.addBackToAGGMenuBar(this.parent);
		this.changer.changeWith(c);
	}
	
	private void changeToAGGgui() {
		((AttrTupleManager) agg.attribute.impl.AttrTupleManager
				.getDefaultManager()).setVariableContext(false);
		this.removeBackFromAGGMenuBar(this.parent);
		this.changer.restore();
	}

	protected static CriticalPairAnalysis cpa; // this

	protected GUIExchange changer;

	protected ParserGUIOption option;

	protected ParserOptionGUI pOptionGUI;

	protected ParserOption pOption;

	protected LayerOption lOption;

	protected CriticalPairOptionGUI cpOptionGUI;

	protected CriticalPairOption cpOption;

	protected CriticalPairAnalysisGUI pairsGUI;

	protected PairContainer pairsContainer, pairsContainer2;

	protected ConflictsDependenciesGraph conflictDependGraph;

	protected PairIOGUI pairsIOGUI;

	protected JMenu pairsMenu, startCP, loadCP, cpaGraphCP, showCP;

	protected JMenuItem rulesCP, resetCP, unlockCP, 
			startCPconflicts, startCPdependencies, stopCP,
//			reduceCP, 
			consistCP, debugCP, emptyCP, loadCPcpx, loadCPdpx,
			loadSeparateCPcpx, loadSeparateCPdpx, saveCP, showConflictCP,
			showDependencyCP, cpaCombiGraphCP, checkHostGraphCP, backCP;

	protected Vector<ParserEventListener> listener;

	protected Vector<StatusMessageListener> pmlistener;

	protected AGGAppl parent;

	protected EdGraGra pairsGraGra;
	
	protected EdGraph cpaGraph;
	
	protected RuleLayer rlayer;

	protected GraGraTreeView treeView;

	protected EdGraGra selectedGraGra;

	protected List<Rule> ruleList1, ruleList2;
	
	protected Vector<JMenu> menus;

	protected Hashtable<EdGraGra, Boolean> gragraChanged = new Hashtable<EdGraGra, Boolean>();

	protected boolean isWarned = false;

	protected boolean isLocked = true, resetDone = false;;

	protected Hashtable<JFrame, CriticalPairAnalysisSeparated> 
	separatedFrames = new Hashtable<JFrame, CriticalPairAnalysisSeparated>();

	protected CriticalPairAnalysisSeparated hostGraphCPA;
	Hashtable<Graph, Vector<Hashtable<GraphObject, GraphObject>>> hostGraphCPAcontainer;
	
	// CriticalPairAnalysisSeparated combinedCPA;
	protected JFrame hostGraphFrame;

	protected Hashtable<Graph, Pair<Vector<Hashtable<GraphObject, GraphObject>>, JButton>> overlapGraphs;

	protected Graph overlapGraph;

	protected Vector<Hashtable<GraphObject, GraphObject>> hostGraphMappings;

	protected JButton nextMatchAtHostGraphButton;

	protected GraphDesktop graphDesktop;

	protected int x = 100, y = 50;

	protected int mouseX = -1, mouseY = -1;

	protected WindowListener wl;

	protected JFrame lastActiveFrame;

	protected boolean pairsSaved = false;

	protected GraphicsExportJPEG exportJPEG;
	
	protected String message = "";
	
}
