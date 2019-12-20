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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.tree.TreePath;

import agg.xt_basis.Arc;
import agg.xt_basis.BadMappingException;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.Node;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.TypeError;
import agg.xt_basis.TypeSet;
import agg.xt_basis.agt.RuleScheme;
import agg.gui.IconResource;
import agg.attribute.AttrTuple;
import agg.attribute.AttrVariableTuple;
import agg.attribute.impl.ValueMember;
import agg.editor.impl.EdGraph;
import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdMorphism;
import agg.editor.impl.EdNode;
import agg.editor.impl.EdPAC;
import agg.editor.impl.EdRule;
import agg.editor.impl.EdNAC;
import agg.gui.editor.GraphCanvas;
import agg.gui.editor.GraphEditor;
import agg.gui.editor.GraphPanel;
import agg.gui.editor.RuleEditor;
import agg.gui.options.ParserGUIOption;
import agg.gui.parser.event.ParserGUIEvent;
import agg.gui.parser.event.ParserGUIListener;
import agg.gui.saveload.GraphicsExportJPEG;
import agg.parser.CriticalPairData;
import agg.parser.Report;
import agg.parser.PairContainer;
import agg.parser.CriticalPair;
import agg.util.Pair;


/**
 * The graph desktop shows many overlapping graphs at the critical pair
 * analysis. The internal frames can be configured.
 * 
 * @version $Id: GraphDesktop.java,v 1.26 2010/12/21 13:40:34 olga Exp $
 * @author $Author: olga $
 */
public class GraphDesktop implements InternalFrameListener {
	
	int dx = 0; int dy = 0;

	protected final JFrame parentFrame;
	
	protected JDesktopPane desktop;

	protected JScrollPane jsp;

	protected ImageIcon internalFrameIcon;

	protected int nextX, nextY;

	protected EdGraGra layout;

	protected Hashtable<Graph, Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>> 
	overlappings;

	protected Hashtable<Graph, EdGraph> internalLayoutGraphs;

	protected Hashtable<Graph, JInternalFrame> internalGraphFrames;

	protected Dimension internalFrameSize;

	protected ParserGUIOption option;

	protected Vector<ParserGUIListener> listener;

	protected JInternalFrame cpaGraphFrame, conflictFrame, dependFrame, activeGraphFrame;

	final protected MouseListener ml;

	final protected JPopupMenu graphMenu = new JPopupMenu("Graph");

	final protected JMenu miShapeC = new JMenu();

	final protected JMenu miShapeD = new JMenu();

	final protected JPopupMenu cpaGraphMenu = new JPopupMenu("CPA Graph");

	final protected JMenuItem miC = new JMenuItem("Show Conflicts");

	final protected JMenuItem miD = new JMenuItem("Show Dependencies");

	final protected JMenuItem miAll = new JMenuItem("Show All");

	final protected JMenuItem miRefresh = new JMenuItem("Refresh");
	
	final protected JMenuItem miStraightEdges = new JMenuItem("Straight Edges");

	final protected JMenuItem miHide = new JMenuItem("Hide Node/Edge");

	final protected JMenuItem miLayoutGraph = new JMenuItem("Layout Graph");

	final protected JMenuItem miGraphExportJPG = new JMenuItem("Export JPEG");
	
	final protected JMenuItem miExportJPG_graphMenu = new JMenuItem("Export JPEG");
	
	final protected JMenuItem miAddToGraphs_graphMenu = new JMenuItem("Add To Host Graphs");

	final protected JMenu miAddToNACs_graphMenu = new JMenu("Add To NACs");
	
	final protected JMenuItem miLayout_graphMenu = new JMenuItem("Layout Graph");
	
	final protected JMenuItem miVarEqual_graphMenu = new JMenuItem("Variable Equalities");
	
	final JMenu confsMenu = new JMenu(" Show ");
	
	final JMenu depsMenu = new JMenu(" Show ");
	
	protected JMenuItem miFirstRule, miSecondRule, miAllConfs, miAllDeps;
	
	protected CriticalPairPanel conflictPanel, dependPanel;

	protected int myW, myH;

	protected RuleEditor ruleEdit1;

	protected RuleEditor ruleEdit2;

	protected JInternalFrame ruleFrame1, ruleFrame2;

	protected EdRule layoutRule1, layoutRule2;

	protected GraphPanel activeGraphPanel;

	protected GraphicsExportJPEG exportJPEG;
		
	final protected Hashtable<EdGraph, VariableEqualityDialog> varEqualityDialogs;
	protected Point locationOnScreen;
	
	
	/**
	 * Creates a new desktop. The layout is given by a graph grammar. The
	 * display settings are given by user defined options.
	 * 
	 * @param layout
	 *            The layout for graphs.
	 * @param option
	 *            The options for display settings.
	 */
	public GraphDesktop(final JFrame parFrame, EdGraGra layout, ParserGUIOption option) {
		setLayout(layout);
		this.parentFrame = parFrame;
		this.listener = new Vector<ParserGUIListener>();
		this.overlappings = new Hashtable<Graph, Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
		this.internalGraphFrames = new Hashtable<Graph, JInternalFrame>();
		this.internalLayoutGraphs = new Hashtable<Graph, EdGraph>();
		this.varEqualityDialogs = new Hashtable<EdGraph, VariableEqualityDialog>();
		this.desktop = new JDesktopPane();
		this.myW = 500;
		this.myH = 500;
		this.desktop.setPreferredSize(new Dimension(this.myW, this.myH));
		this.jsp = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		this.jsp.setViewportView(this.desktop);
		this.jsp.setBackground(Color.white);
		this.jsp.getHorizontalScrollBar().getModel().setValueIsAdjusting(true);
		this.jsp.getVerticalScrollBar().getModel().setValueIsAdjusting(true);

		this.internalFrameIcon = IconResource.getIconFromURL(IconResource
				.getURLOverlapGraph());
		this.internalFrameSize = new Dimension(200, 200);
		this.option = option;
		this.nextX = 5;
		this.nextY = 5;

		makeGraphMenu();
		makeCPAGraphMenu();

		this.ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				showGraphMenu(e); 
			}

			public void mouseReleased(MouseEvent e) {
				showGraphMenu(e);
			}
		};		
	}

	void showGraphMenu(MouseEvent e) {
		if (e.isPopupTrigger()) {
			if (e.getSource() instanceof JPanel) {
				if (e.getSource() instanceof GraphCanvas) {
					this.activeGraphPanel = ((GraphCanvas) e.getSource()).getViewport();
				}
				if (this.cpaGraphFrame != null 
						&& this.cpaGraphFrame.isVisible()
						&& this.activeGraphFrame == this.cpaGraphFrame) {
					this.cpaGraphMenu
							.show(e.getComponent(), e.getX(), e.getY());
					setPickedGraphObjectOfCPAgraph(e.getX(), e.getY());
				} else {
					if (this.ruleEdit1 != null
							&& (this.activeGraphPanel == this.ruleEdit1.getLeftPanel()
								|| this.activeGraphPanel == this.ruleEdit1.getRightPanel())) {
						this.miAddToGraphs_graphMenu.setEnabled(false);
						this.miAddToNACs_graphMenu.setEnabled(false);
						if (this.activeGraphPanel.getGraph().getPicked(e.getX(), e.getY()) == null)
							this.graphMenu.show(e.getComponent(), e.getX(), e.getY());
					} else if (this.ruleEdit2 != null
							&& (this.activeGraphPanel == this.ruleEdit2.getLeftPanel()
								|| this.activeGraphPanel == this.ruleEdit2.getRightPanel()
								|| this.activeGraphPanel == this.ruleEdit2.getLeftCondPanel())) {
						this.miAddToGraphs_graphMenu.setEnabled(false);	
						this.miAddToNACs_graphMenu.setEnabled(false);
						if (this.activeGraphPanel.getGraph().getPicked(e.getX(), e.getY()) == null)
							this.graphMenu.show(e.getComponent(), e.getX(), e.getY());
					}
					else if (!this.activeGraphPanel.getGraph().isCPAgraph()) {
						this.miAddToGraphs_graphMenu.setEnabled(true);
						this.miAddToNACs_graphMenu.setEnabled(true);
						this.miFirstRule.setText("Of First  Rule: "+this.ruleEdit1.getRule().getName());
						this.miSecondRule.setText("Of Second Rule: "+this.ruleEdit2.getRule().getName());
						if (this.activeGraphPanel.getGraph().getPicked(e.getX(), e.getY()) == null) {
							this.locationOnScreen = new Point(
									this.parentFrame.getLocation().x
										+this.desktop.getLocation().x
										+this.activeGraphPanel.getLocation().x
										+e.getX(),
									this.parentFrame.getLocation().y
										+this.desktop.getLocation().y
										+this.activeGraphPanel.getLocation().y
										+e.getY());	
							this.graphMenu.show(e.getComponent(),
									e.getX(), e.getY());
						}
					}
				}
			}
		}
	}
	

	
	protected void showVarEqualityDialog(final EdGraph graph, final Point location) {
		if (this.varEqualityDialogs.get(graph) != null) {
			this.varEqualityDialogs.get(graph).setVisible(true);
		}
		else if (!graph.getBasisGraph().getHelpInfoAboutVariableEquality().equals("")) {			
			this.varEqualityDialogs.put(graph, (new VariableEqualityDialog(graph, location)));
		}		
	}
	
	
	private void createRuleEditor1() {
		this.ruleEdit1 = new RuleEditor(null);
		this.ruleEdit1.setRuleDividerLocation(150);
		((JPanel) this.ruleEdit1.getLeftPanel().getCanvas())
		.addMouseListener(this.ml);
		((JPanel) this.ruleEdit1.getRightPanel().getCanvas())
		.addMouseListener(this.ml);
		((JPanel) this.ruleEdit1.getNACPanel().getCanvas())
		.addMouseListener(this.ml);
		if (this.exportJPEG != null) {
			this.ruleEdit1.setExportJPEG(this.exportJPEG);
		}
	}
	
	private void createRuleEditor2() {
		this.ruleEdit2 = new RuleEditor(null);
		this.ruleEdit2.setRuleDividerLocation(150);
		((JPanel) this.ruleEdit2.getLeftPanel().getCanvas())
		.addMouseListener(this.ml);
		((JPanel) this.ruleEdit2.getRightPanel().getCanvas())
		.addMouseListener(this.ml);
		((JPanel) this.ruleEdit2.getNACPanel().getCanvas())
		.addMouseListener(this.ml);
		if (this.exportJPEG != null) {
			this.ruleEdit2.setExportJPEG(this.exportJPEG);
		}
	}
	
	protected Object setPickedGraphObjectOfCPAgraph(int x, int y) {
		if (this.cpaGraphFrame == null)
			return null;
		Component c = this.cpaGraphFrame.getContentPane().getComponent(0);
		if (c instanceof GraphEditor) {
			return ((GraphEditor) c).getGraph().getPicked(x, y);
		}
		return null;
	}

	/**
	 * Sets the layout for the left hand side of the rules.
	 * 
	 * @param layout
	 *            The layout
	 */
	public void setLayout(EdGraGra layout) {
		this.layout = layout;
		this.nextX = 5;
		this.nextY = 5;
	}

	/**
	 * Sets the GUI options for display settings.
	 * 
	 * @param option
	 *            The GUI options for display settings.
	 */
	public void setGUIOption(ParserGUIOption option) {
		this.option = option;
	}

	public void setExportJPEG(GraphicsExportJPEG jpg) {
		this.exportJPEG = jpg;
		if (this.ruleEdit1 != null && this.ruleEdit2 != null) {
			this.ruleEdit1.setExportJPEG(this.exportJPEG);
			this.ruleEdit2.setExportJPEG(this.exportJPEG);
		}
	}
		

	/**
	 */
	public JInternalFrame addCriticalPairTable(CriticalPairPanel cppanel,
			String tableName) {
		boolean newFrame = false;
		String name = "Critical Pairs";
		if (!tableName.equals(""))
			name = tableName;
		JInternalFrame cpIFrame = null;
		if (cppanel.getPairContainer().getKindOfConflict() == CriticalPair.CONFLICT) {
			if (this.conflictFrame == null) {
				cpIFrame = new JInternalFrame(name, true, true, true, true);
				this.conflictFrame = cpIFrame;
				setMenuBarOfConflictTableFrame(this.confsMenu);
				this.conflictPanel = cppanel;
				newFrame = true;
				this.nextX = 0;
				if (this.dependFrame == null)
					this.nextY = 50;
				else
					this.nextY = 100;
			} else
				cpIFrame = this.conflictFrame;
		} else if (cppanel.getPairContainer().getKindOfConflict() == CriticalPair.TRIGGER_DEPENDENCY
				|| cppanel.getPairContainer().getKindOfConflict() == CriticalPair.TRIGGER_SWITCH_DEPENDENCY) {
			if (this.dependFrame == null) {
				cpIFrame = new JInternalFrame(name, true, true, true, true);
				this.dependFrame = cpIFrame;
				setMenuBarOfDependencyTableFrame(this.depsMenu);
				this.dependPanel = cppanel;
				newFrame = true;
				this.nextX = 50;
				if (this.conflictFrame == null)
					this.nextY = 50;
				else
					this.nextY = 100;
			} else
				cpIFrame = this.dependFrame;
		}
		if (cpIFrame != null) {
			if (newFrame) {
				cpIFrame.addInternalFrameListener(this);
				int fw = cppanel.getTableWidth() + 110;
				int fh = cppanel.getTableHeight() + 80;
				if (fw > 600)
					fw = 600;
				if (fh > 400)
					fh = 400;
				cpIFrame.setSize(new Dimension(fw, fh));
				cpIFrame.setFrameIcon(this.internalFrameIcon);
				cpIFrame.getContentPane().add(cppanel);
				cpIFrame.setLocation(this.nextX, this.nextY);
				this.desktop.add(cpIFrame);
				cpIFrame.setVisible(true);
				// setMenuBar(cpIFrame);
				try {
					cpIFrame.setIcon(false);
					cpIFrame.setSelected(true);
				} catch (java.beans.PropertyVetoException pve) {
				}
			} else if (!cpIFrame.isVisible()) {
				this.desktop.add(cpIFrame);
				cpIFrame.setVisible(true);
				try {
					cpIFrame.setIcon(false);
					cpIFrame.setSelected(true);
				} catch (java.beans.PropertyVetoException pve) {
				}
			} else {
				try {
					if (cpIFrame.isIcon())
						cpIFrame.setIcon(false);
					cpIFrame.setSelected(true);
				} catch (java.beans.PropertyVetoException pve) {
				}
			}
		}
		return cpIFrame;
	}

	/**
	 * Add a new graph to the desktop. The layout is take from the grammar.
	 * 
	 * @param g
	 *            The new graph
	 */
	public JInternalFrame addGraph(Graph g) {
		if ((this.option == null)
				|| (this.option.getNumberOfCriticalPair() <= this.desktop.getComponentCount())) {
			return null;
		}

		GraphEditor gege = null;

		JInternalFrame newGraph = new JInternalFrame(g.getName(), true, false,
				true, true);
		this.internalGraphFrames.put(g, newGraph);

		newGraph.addInternalFrameListener(this);
		newGraph.setSize(this.internalFrameSize);
		newGraph.setFrameIcon(this.internalFrameIcon);

		
		EdGraph eg = new EdGraph(g, this.layout.getTypeSet());
		this.internalLayoutGraphs.put(g, eg);
		eg.setEditable(false);
		eg.setDrawingStyleOfCriticalObjects(this.option.getDrawingStyleOfCriticalObjects());
		
		gege = new GraphEditor();
		gege.setGraph(eg);
		
		eg.makeInitialUpdateOfNodes();
		eg.doDefaultEvolutionaryGraphLayout(10);
		
		gege.setExportJPEG(this.exportJPEG);
		((JPanel) gege.getGraphPanel().getCanvas()).addMouseListener(this.ml);
		gege.setEditMode(agg.gui.editor.EditorConstants.MOVE);
		gege.setTitle("    ");

		newGraph.getContentPane().add(gege);
		this.desktop.add(newGraph);
		
		try {
			newGraph.setIcon(true);
			newGraph.setSelected(false);
			newGraph.setVisible(true);
			
			newGraph.getDesktopIcon().setLocation(this.nextX, this.nextY);
			if ((this.nextX + newGraph.getDesktopIcon().getSize().width) >= this.desktop
					.getSize().width) {
//				int w = this.nextX + newGraph.getDesktopIcon().getSize().width;
				int h = this.desktop.getSize().height;
				this.nextX = 5;
				this.nextY = this.nextY + newGraph.getDesktopIcon().getSize().height;
				if ((this.nextY + newGraph.getDesktopIcon().getSize().height) >= h) {
					h = this.nextY + newGraph.getDesktopIcon().getSize().height;
					this.desktop.setPreferredSize(new Dimension(this.myW, h));
				}
			}
			this.nextX = this.nextX + newGraph.getDesktopIcon().getSize().width;
		} catch (java.beans.PropertyVetoException pve) {}
		
		return newGraph;
	}

	public JInternalFrame addGraph(EdGraph eg, int w1, int h1) {
		if (eg == null
				|| this.option == null
				|| this.option.getNumberOfCriticalPair() <= this.desktop.getComponentCount()) {
			return null;
		}
		
		boolean newFrame = true;
		GraphEditor gege = null;
		
		eg.setEditable(false);
		eg.setDrawingStyleOfCriticalObjects(this.option.getDrawingStyleOfCriticalObjects());
		
		JInternalFrame graphFrame = this.internalGraphFrames.get(eg.getBasisGraph());
		if (graphFrame == null) {
			graphFrame = new JInternalFrame(eg.getBasisGraph().getName(), true,
					true, true, true);
			this.internalGraphFrames.put(eg.getBasisGraph(), graphFrame);
			this.internalLayoutGraphs.put(eg.getBasisGraph(), eg);

			graphFrame.addInternalFrameListener(this);
			graphFrame.setSize(new Dimension(w1, h1));
			graphFrame.setFrameIcon(this.internalFrameIcon);
			
			if (!eg.isCPAgraph()) {
				eg.updateGraph();
			}

			gege = new GraphEditor();
			gege.setExportJPEG(this.exportJPEG);

			graphFrame.getContentPane().add(gege);

			if (eg.isCPAgraph()) {
				if (!eg.getBasisGraph().isEmpty()) {
					int fw = eg.getGraphDimension().width;
					int fh = eg.getGraphDimension().height;
					if (fw > 600)
						fw = 600;
					else if (fw < 400)
						fw = 400;
					if (fh > 400)
						fh = 400;
					else if (fh < 300)
						fh = 300;
					graphFrame.setSize(new Dimension(fw, fh));
				} else {
					graphFrame.setSize(new Dimension(300, 200));
				}
				
				this.cpaGraphFrame = graphFrame;	
				this.cpaGraphFrame.addMouseListener(this.ml);
			} 
			
			((JPanel) gege.getGraphPanel().getCanvas())
						.addMouseListener(this.ml);
			
		} else {
			newFrame = false;
			if (eg.isCPAgraph()) {
				Component c = graphFrame.getContentPane().getComponent(0);
				if (c instanceof GraphEditor) {
					gege = (GraphEditor) c;
					gege.setExportJPEG(this.exportJPEG);
				}
				if (!eg.getBasisGraph().isEmpty()) {
					int fw = eg.getGraphDimension().width;
					int fh = eg.getGraphDimension().height;
					if (fw > 600)
						fw = 600;
					else if (fw < 400)
						fw = 400;
					if (fh > 400)
						fh = 400;
					else if (fh < 300)
						fh = 300;
					graphFrame.setSize(new Dimension(fw, fh));
				} else {
					graphFrame.setSize(new Dimension(300, 200));
				}
				try {
					graphFrame.setSelected(true);
				} catch (java.beans.PropertyVetoException e) {}
			}
		}

		if (gege != null) {
			gege.setGraph(eg);
			gege.setEditMode(agg.gui.editor.EditorConstants.MOVE);
			gege.setTitle("    ");
		}

		this.desktop.add(graphFrame);
		
		try {
			if (!eg.isCPAgraph()) {
				graphFrame.setIcon(true);
				graphFrame.setSelected(false);
			}
			graphFrame.setVisible(true);
		} catch (java.beans.PropertyVetoException pve) {
			System.out.println("GraphDesktop.addGraph:: java.beans.PropertyVetoException:  "+pve.getMessage());
		}
		
		if (newFrame) {
			graphFrame.getDesktopIcon().setLocation(this.nextX, this.nextY);
			this.nextX = this.nextX + graphFrame.getDesktopIcon().getSize().width;
			if ((this.nextX + graphFrame.getDesktopIcon().getSize().width) >= this.desktop
					.getSize().width) {
//				int w = this.nextX + graphFrame.getDesktopIcon().getSize().width;
				int h = this.desktop.getSize().height;
				this.nextX = 5;
				this.nextY = this.nextY + graphFrame.getDesktopIcon().getSize().height;
				if ((this.nextY + graphFrame.getDesktopIcon().getSize().height) >= h) {
					h = this.nextY + graphFrame.getDesktopIcon().getSize().height;
					this.desktop.setPreferredSize(new Dimension(this.myW, h));
				}
			}
			this.nextX = this.nextX + graphFrame.getDesktopIcon().getSize().width;
		}
		return graphFrame;
	}

	public JInternalFrame addRule1(Rule rule, int w1, int h1) {
		if ((this.option == null)
				|| (this.option.getNumberOfCriticalPair() <= this.desktop.getComponentCount())) {
			return null;
		}
		
		if (!(rule instanceof RuleScheme)) {
			if (this.layout != null) {
				this.layoutRule1 = this.layout.getRule(rule);
				if (this.layoutRule1 == null) {
					this.layoutRule1 = new EdRule(rule);
					this.layoutRule1.getLeft().doDefaultEvolutionaryGraphLayout(10);
					this.layoutRule1.getRight().doDefaultEvolutionaryGraphLayout(10);
				} else {
					if (!this.layoutRule1.getLeft().hasDefaultLayout())
						this.layoutRule1.getLeft().doDefaultEvolutionaryGraphLayout(10);
					if (!this.layoutRule1.getRight().hasDefaultLayout())
						this.layoutRule1.getRight().doDefaultEvolutionaryGraphLayout(10);
				}
			} else {
				this.layoutRule1 = new EdRule(rule);
				this.layoutRule1.getLeft().doDefaultEvolutionaryGraphLayout(10);
				this.layoutRule1.getRight().doDefaultEvolutionaryGraphLayout(10);
			}
			this.layoutRule1.update();
			this.layoutRule1.getLeft().setEditable(false);
			this.layoutRule1.getRight().setEditable(false);
		}
		else {
			this.layoutRule1 = null;
		}
		
		if (this.ruleFrame1 == null) {
			createRuleEditor1();
			this.ruleFrame1 = new JInternalFrame("", true, true, true, true);
			this.ruleFrame1.setFrameIcon(this.internalFrameIcon);
			this.ruleFrame1.setTitle("     Rule 1 ");
			this.ruleFrame1.addInternalFrameListener(this);
			this.ruleFrame1.getContentPane().add(this.ruleEdit1);
			this.ruleFrame1.setSize(new Dimension(w1, h1));
			this.desktop.add(this.ruleFrame1);
		}

		this.ruleEdit1.setRule(this.layoutRule1);
		this.ruleEdit1.setNAC(null);
		this.ruleEdit1.setEditMode(agg.gui.editor.EditorConstants.MOVE);
		this.ruleEdit1.setRuleTitle(rule.getQualifiedName(), "");
		this.ruleEdit1.enableSynchronMoveOfMappedObjects(false);

		this.ruleFrame1.setVisible(false);
		try {
			this.ruleFrame1.setIcon(true);
			this.ruleFrame1.setSelected(false);
			this.ruleFrame1.setVisible(true);
		} catch (java.beans.PropertyVetoException pve) {
		}

		this.nextX = this.ruleFrame1.getDesktopIcon().getSize().width;
		this.nextY = 5;
		this.ruleFrame1.getDesktopIcon().setLocation(this.nextX, this.nextY);
		return this.ruleFrame1;
	}

	public JInternalFrame addRule2(Rule rule, int w1, int h1) {
		if ((this.option == null)
				|| (this.option.getNumberOfCriticalPair() <= this.desktop.getComponentCount())
//				|| (rule instanceof RuleScheme)
		) {
			return null;
		}
		
		if (!(rule instanceof RuleScheme)) {
			if (this.layout != null) {
				this.layoutRule2 = this.layout.getRule(rule);
				if (this.layoutRule2 == null) {
					this.layoutRule2 = new EdRule(rule);
					this.layoutRule2.getLeft().doDefaultEvolutionaryGraphLayout(10);
					this.layoutRule2.getRight().doDefaultEvolutionaryGraphLayout(10);
				} else if (this.layoutRule2 == this.layoutRule1) {
					this.layoutRule2 = new EdRule(rule);
					setRuleLayout(this.layoutRule2, this.layoutRule1);
				} else {
					if (!this.layoutRule2.getLeft().hasDefaultLayout())
						this.layoutRule2.getLeft().doDefaultEvolutionaryGraphLayout(10);
					if (!this.layoutRule2.getRight().hasDefaultLayout())
						this.layoutRule2.getRight().doDefaultEvolutionaryGraphLayout(10);
				}
			} else {
				this.layoutRule2 = new EdRule(rule);
				this.layoutRule2.getLeft().doDefaultEvolutionaryGraphLayout(10);
				this.layoutRule2.getRight().doDefaultEvolutionaryGraphLayout(10);
			}
			this.layoutRule2.update();
			this.layoutRule2.getLeft().setEditable(false);
			this.layoutRule2.getRight().setEditable(false);
		} 
		else {
			this.layoutRule2 = null;
		}
			
		
		if (this.ruleFrame2 == null) {
			createRuleEditor2();
			this.ruleFrame2 = new JInternalFrame("", true, true, true, true);
			this.ruleFrame2.setFrameIcon(this.internalFrameIcon);
			this.ruleFrame2.setTitle("     Rule 2 ");
			this.ruleFrame2.addInternalFrameListener(this);
			this.ruleFrame2.getContentPane().add(this.ruleEdit2);
			this.ruleFrame2.setSize(new Dimension(w1, h1));
			this.desktop.add(this.ruleFrame2);
		}

		this.ruleEdit2.setRule(this.layoutRule2);
		this.ruleEdit2.setNAC(null);
		this.ruleEdit2.setEditMode(agg.gui.editor.EditorConstants.MOVE);
		this.ruleEdit2.setRuleTitle(rule.getQualifiedName(), "");
		this.ruleEdit2.enableSynchronMoveOfMappedObjects(false);

		this.ruleFrame2.setVisible(false);
		try {
			this.ruleFrame2.setIcon(true);
			this.ruleFrame2.setSelected(false);
			this.ruleFrame2.setVisible(true);
		} catch (java.beans.PropertyVetoException pve) {
		}

		this.nextX = this.ruleFrame1.getDesktopIcon().getSize().width * 2;
		this.nextY = 5;
		this.ruleFrame2.getDesktopIcon().setLocation(this.nextX, this.nextY);
		return this.ruleFrame2;
	}

	private void setRuleLayout(EdRule to, EdRule from) {
		to.getLeft().setLayoutByBasisObject(from.getLeft());
		to.getRight().setLayoutByBasisObject(from.getRight());
		Vector<EdNAC> nacsTo = to.getNACs();
		Vector<EdNAC> nacsFrom = from.getNACs();
		for (int i = 0; i < nacsTo.size(); i++) {
			EdGraph nacGto = nacsTo.get(i);
			for (int j = 0; j < nacsFrom.size(); j++) {
				EdGraph nacGfrom = nacsFrom.get(j);
				if (nacGto.getBasisGraph() == nacGfrom.getBasisGraph()) {
					nacGto.setLayoutByBasisObject(nacGfrom);
					break;
				}
			}
		}
		Vector<EdPAC> pacsTo = to.getPACs();
		Vector<EdPAC> pacsFrom = from.getPACs();
		for (int i = 0; i < pacsTo.size(); i++) {
			EdGraph pacGto = pacsTo.get(i);
			for (int j = 0; j <pacsFrom.size(); j++) {
				EdGraph pacGfrom = pacsFrom.get(j);
				if (pacGto.getBasisGraph() == pacGfrom.getBasisGraph()) {
					pacGto.setLayoutByBasisObject(pacGfrom);
					break;
				}
			}
		}
	}

	public void setIconOfRules(boolean b) {
		if (this.ruleFrame1 != null && !this.ruleFrame1.isIcon()) {
			try {
				this.ruleFrame1.setIcon(b);
			} catch (java.beans.PropertyVetoException ex) {
			}
		}
		if (this.ruleFrame2 != null && !this.ruleFrame2.isIcon()) {
			try {
				this.ruleFrame2.setIcon(b);
			} catch (java.beans.PropertyVetoException ex) {
			}
		}
	}

	private EdNAC resetNAC(RuleEditor edit, EdRule rule, String nacName, Color bgcolor) {
		if (rule == null  || nacName == null || nacName.length() == 0)
			return null;
		
		EdNAC nacGraph = null;
		if (rule.getNACs().isEmpty())
			edit.setNAC(null);
		else {
			for (int i = 0; i < rule.getNACs().size(); i++) {
				nacGraph = rule.getNACs().get(i);
				if (nacName.equals(nacGraph.getName())) {
					edit.setNAC(nacGraph);
					edit.setNACTitle("NAC: " + nacGraph.getName());
					edit.getNACPanel().setBackground(bgcolor); //new Color(255, 255, 165));
					edit.getLeftPanel().setBackground(bgcolor); //new Color(255, 255, 165));
					return nacGraph;
				}
			}
		}
		return nacGraph;
	}

	private EdPAC resetPAC(RuleEditor edit, EdRule rule, String pacName, Color bgcolor) {
		if (rule == null || pacName == null || pacName.length() == 0)
			return null;
		
		EdPAC pacGraph = null;
		if (rule.getPACs().isEmpty())
			edit.setPAC(null);
		else {
			for (int i = 0; i < rule.getPACs().size(); i++) {
				pacGraph = rule.getPACs().get(i);
				if (pacName.equals(pacGraph.getName())) {
					edit.setPAC(pacGraph);
					edit.setPACTitle("PAC: " + pacGraph.getName());
					edit.getPACPanel().setBackground(bgcolor); //new Color(255, 255, 165));
					edit.getLeftPanel().setBackground(bgcolor); //new Color(255, 255, 165));
					return pacGraph;
				}
			}
		}
		return pacGraph;
	}
	
	public JInternalFrame getInternalCPAGraphFrame() {
		return this.cpaGraphFrame;
	}
	
	public JInternalFrame getInternalGraphFrame(Graph g) {
		return this.internalGraphFrames.get(g);
	}

	public EdGraph getInternalLayoutGraph(Graph g) {
		return this.internalLayoutGraphs.get(g);
	}

	public JButton addNextButton(Graph g, String text) {
		JInternalFrame f = this.internalGraphFrames.get(g);
		JButton next = new JButton(text);
		f.getContentPane().add(next, BorderLayout.SOUTH);
		return next;
	}

	/**
	 * Shows a little internal frame with a message that two rule are not
	 * critic.
	 * 
	 * @param first
	 *            The first rule.
	 * @param second
	 *            The second rule.
	 */
	public void notCriticFrame(Rule first, Rule second) {
		notCriticFrame(first, second, "");
	}
	
	/**
	 * Shows a little internal frame with a message that two rule are not
	 * critic.
	 * 
	 * @param first
	 *            the first rule
	 * @param second
	 *            the second rule
	 * @param text 
	 * 				text to show in the frame         
	 */
	public void notCriticFrame(Rule first, Rule second, String text) {
		resetNAC(this.ruleEdit1, this.layoutRule1, null, Color.WHITE);
		this.ruleEdit1.setDividerLocation(0, 0);
		resetNAC(this.ruleEdit2, this.layoutRule2, null, Color.WHITE);
		this.ruleEdit2.setDividerLocation(0, 0);

		Report.trace("starte notCriticFrame", 1);
		JInternalFrame newGraph = new JInternalFrame("not critic rules", false,
				false, false, false);
		newGraph.setVisible(true);
		newGraph.setSize(300, 150);
		newGraph.setFrameIcon(this.internalFrameIcon);
		newGraph.getContentPane().setLayout(new BorderLayout());
		java.net.URL url = ClassLoader.getSystemClassLoader()
								.getResource("agg/lib/icons/ok2.gif");
		if (url != null) {
			ImageIcon icon = new ImageIcon(url);
			Image image = icon.getImage();
			Image scaledImage = image.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
			ImageIcon scaledIcon = new ImageIcon(scaledImage);
			JLabel l = new JLabel(scaledIcon);
			newGraph.getContentPane().add(l, BorderLayout.WEST);
		}
		
		JPanel messagePanel = new JPanel();
		messagePanel.setLayout(new GridBagLayout());
		JLabel message1 = new JLabel("r1: "+first.getName());
		JLabel message2 = new JLabel("doesn't exclude");
		JLabel message3 = new JLabel("r2: "+second.getName());
		JLabel message4 = null;
		if (text.length() != 0)
			message4 = new JLabel(text);
		
		GridBagConstraints c = new GridBagConstraints();

		c.gridwidth = GridBagConstraints.REMAINDER;
		messagePanel.add(message1, c);
		messagePanel.add(message2, c);
		messagePanel.add(message3, c);
		if (message4 !=  null)
			messagePanel.add(message4, c);
		newGraph.getContentPane().add(messagePanel, BorderLayout.CENTER);
		Report.println("DesktopSize " + this.desktop.getSize(), Report.TRACE);
		int posX = (int) this.desktop.getSize().getWidth();
		int posY = (int) this.desktop.getSize().getHeight();

		posX = posX / 2;
		posY = posY / 2;
		int width = (int) newGraph.getSize().getWidth();
		int height = (int) newGraph.getSize().getHeight();

		width = width / 2;
		height = height / 2;
		newGraph.setLocation(posX - width, posY - height);

		this.desktop.add(newGraph);
		Report.trace("beende notCriticFrame", -1);
	}

	
	/**
	 * Sets the size of the internal frame for a overlapping graph.
	 * 
	 * @param size
	 *            The window size
	 */
	public void setOverlappingGraphWindowSize(Dimension size) {
		this.internalFrameSize = size;
		JInternalFrame[] allFrames = this.desktop.getAllFrames();
		if (allFrames != null)
			for (int i = 0; i < allFrames.length; i++) {
				allFrames[i].setSize(this.internalFrameSize);
			}
	}

	/**
	 * Adds a new overlapping graph with the two morphisms from the left sides.
	 * 
	 * @param graph
	 *            The new overlapping graph.
	 * @param pair
	 *            A pair of morphisms
	 */
	public void addOverlapping(Graph graph, Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> pair) {
		this.overlappings.put(graph, pair);
	}

	public CriticalPairPanel getActivePairPanel() {
		JInternalFrame[] allFrames = this.desktop.getAllFrames();
		if (allFrames != null) {
			for (int i = 0; i < allFrames.length; i++) {
				JInternalFrame f = allFrames[i];
				Component c = f.getContentPane().getComponent(0);
				if (c instanceof CriticalPairPanel) {
					if (((CriticalPairPanel) c).isActive())
						return (CriticalPairPanel) c;
				} 
			}
		}
		return null;
	}

	public CriticalPairPanel getConflictPairPanel() {
		return this.conflictPanel;
	}

	public CriticalPairPanel getDependPairPanel() {
		return this.dependPanel;
	}
	
	
	/**
	 * Returns the component to display the desktop. This component can be set
	 * in a frame, panel or something like that.
	 * 
	 * @return The desktop
	 */
	public Component getComponent() {
		return this.jsp;
	}

	/**
	 * Returns the desktop object of the graph desktop.
	 * 
	 * @return The desktop
	 */
	public JDesktopPane getDesktop() {
		return this.desktop;
	}

	public void setIconOfPairTable(boolean b) {
		for (int i = this.desktop.getAllFrames().length - 1; i >= 0; i--) {
			JInternalFrame f = this.desktop.getAllFrames()[i];
			Component c = f.getContentPane().getComponent(0);
			if (c instanceof CriticalPairPanel) {
				try {
					f.setIcon(b);
				} catch (java.beans.PropertyVetoException ex) {
				}
			}
		}
	}

	public void setIconOfPairTable(CriticalPairPanel p, boolean b) {
		if (p == null)
			return;
		for (int i = this.desktop.getAllFrames().length - 1; i >= 0; i--) {
			JInternalFrame f = this.desktop.getAllFrames()[i];
			Component c = f.getContentPane().getComponent(0);
			if (c instanceof CriticalPairPanel && (CriticalPairPanel) c == p) {
				try {
					f.setIcon(b);
				} catch (java.beans.PropertyVetoException ex) {
				}
			}
		}
	}

	public void setIconOfCPAGraph(boolean b) {
		if (this.cpaGraphFrame == null)
			return;
		try {
			this.cpaGraphFrame.setIcon(b);
		} catch (java.beans.PropertyVetoException ex) {
		}
	}

	public void removeCPAGraphFrame() {
		JInternalFrame[] allFrames = this.desktop.getAllFrames();
		if (allFrames != null) {
			for (int i = 0; i < allFrames.length; i++) {
				JInternalFrame f = allFrames[i];
				Component c = f.getContentPane().getComponent(0);
				if (c instanceof GraphEditor) {
					GraphEditor gege = (GraphEditor) c;
					EdGraph eg = gege.getGraph();
					if (eg.isCPAgraph()) {
						if (f.isIcon()) {
							this.desktop.remove(f.getDesktopIcon());
						} else {
							this.desktop.remove(f);
						}
					}
				}
			}
		}
	}

	private void removeVarEqualityDialogs() {
		final Iterator<VariableEqualityDialog> iterator = this.varEqualityDialogs.values().iterator();
		while (iterator.hasNext()) {
			iterator.next().setVisible(false);
		}
		this.varEqualityDialogs.clear();
	}
	
	/**
	 * Removes all frames from the this.desktop.
	 */
	public void removeAllFrames() {
		removeVarEqualityDialogs();
		this.overlappings.clear();
		this.desktop.removeAll();
		this.nextX = 5;
		this.nextY = 5;
	}

	/**
	 * Removes all graph frames from the desktop. Critical pair table remains.
	 */
	public void removeAllGraphFrames() {
		removeVarEqualityDialogs();
		this.overlappings = new Hashtable<Graph, Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>();
		
		for (int i = this.desktop.getAllFrames().length - 1; i >= 0; i--) {
			JInternalFrame f = this.desktop.getAllFrames()[i];
			if (f == this.cpaGraphFrame)
				continue;
			Component c = f.getContentPane().getComponent(0);
			if (c instanceof GraphEditor) {
//				String gname = ((GraphEditor) c).getGraph().getBasisGraph()
//						.getName();
//				if ((gname.indexOf("Combined") == -1)
//						&& (gname.indexOf("Conflicts") == -1)
//						&& (gname.indexOf("Dependencies") == -1)) {
					if (f.isIcon()) {
						this.desktop.remove(f.getDesktopIcon());
					} else {
						this.desktop.remove(f);
					}
//				}
			} else if (c instanceof JLabel) {
				this.desktop.remove(f);
			}
		}
	
		this.nextX = 5;
		this.nextY = 5;
		this.nextX = this.nextX + 160 * 3;
	}

	public void removePairPanelFrame(CriticalPairPanel p) {
		JInternalFrame[] allFrames = this.desktop.getAllFrames();
		if (allFrames != null) {
			for (int i = 0; i < allFrames.length; i++) {
				JInternalFrame f = allFrames[i];
				Component c = f.getContentPane().getComponent(0);
				if (c instanceof CriticalPairPanel) {
					if (p == (CriticalPairPanel) c) {
						this.desktop.remove(i);
						return;
					}
				}
			}
		}
	}

	public void removePairPanelFrame(PairContainer p) {
		JInternalFrame[] allFrames = this.desktop.getAllFrames();
		if (allFrames != null) {
			for (int i = 0; i < allFrames.length; i++) {
				JInternalFrame f = allFrames[i];
				Component c = f.getContentPane().getComponent(0);
				if (c instanceof CriticalPairPanel) {
					if (((CriticalPairPanel) c).getPairContainer() == p) {
						this.desktop.remove(i);
						return;
					}
				}
			}
		}
	}

	public void removeRuleFrame(int indx) {
		JInternalFrame[] allFrames = this.desktop.getAllFrames();
		if (allFrames != null) {
			for (int i = 0; i < allFrames.length; i++) {
				JInternalFrame f = allFrames[i];
				if (indx == 1 && f == this.ruleFrame1) {
					this.desktop.remove(i);
					this.ruleFrame1 = null;
					break;
				} else if (indx == 2 && f == this.ruleFrame2) {
					this.desktop.remove(i);
					this.ruleFrame2 = null;
					break;
				}
			}
		}
	}

	public void removeRuleFrames() {
		JInternalFrame[] allFrames = this.desktop.getAllFrames();
		if (allFrames != null) {
			for (int i = allFrames.length - 1; i >= 0; i--) {
				JInternalFrame f = allFrames[i];
				if (f == this.ruleFrame1) {
					this.desktop.remove(i);
					this.ruleFrame1 = null;
				} else if (f == this.ruleFrame2) {
					this.desktop.remove(i);
					this.ruleFrame2 = null;
				}
			}
		}
	}

	public void reinitComponents() {
		if (this.conflictPanel != null) {
			removePairPanelFrame(this.conflictPanel);
			this.conflictPanel = null;
			this.conflictFrame = null;
		}
		if (this.dependPanel != null) {
			removePairPanelFrame(this.dependPanel);
			this.dependPanel = null;
			this.dependFrame = null;
		}
		if (this.cpaGraphFrame != null) {
			removeCPAGraphFrame();
			this.cpaGraphFrame = null;
		}
		this.internalLayoutGraphs.clear();
		this.internalGraphFrames.clear();
		if (this.ruleEdit1 != null)
			removeRuleFrame(1);
		if (this.ruleEdit2 != null)
			removeRuleFrame(2);
		removeAllFrames();
	}
	
	public void refresh() {
		// System.out.println("GraphDesktop.refresh ...");
		removeAllGraphFrames();
		JInternalFrame[] allFrames = this.desktop.getAllFrames();
		if (allFrames != null) {
			for (int i = 0; i < allFrames.length; i++) {
				JInternalFrame f = allFrames[i];
				Component c = f.getContentPane().getComponent(0);
				if (c instanceof CriticalPairPanel) {
					((CriticalPairPanel) c).refreshView();
				}
			}
		}
		if (this.cpaGraphFrame != null) {
			Component c = this.cpaGraphFrame.getContentPane().getComponent(0);
			if (c instanceof GraphEditor)
				((GraphEditor) c).updateGraphics();
		}
		if (this.ruleEdit1 != null)
			this.ruleEdit1.updateGraphics();
		if (this.ruleEdit2 != null)
			this.ruleEdit2.updateGraphics();
	}

	public void refreshCPAGraph() {
		if (this.cpaGraphFrame != null) {
			Component c = this.cpaGraphFrame.getContentPane().getComponent(0);
			if (c instanceof GraphEditor) {
				((GraphEditor) c).updateGraphics();
			}
		}
	}

	public boolean hasEmptyComponents() {
		if (this.conflictFrame != null
				&& !((CriticalPairPanel) this.conflictFrame.getContentPane()
					.getComponent(0)).isEmpty()) {
				return false;
		} else if (this.dependFrame != null
				&& !((CriticalPairPanel) this.dependFrame.getContentPane()
					.getComponent(0)).isEmpty()) {
				return false;
		} else {
			return true;
		}
	}

	/**
	 * Register a new listener.
	 * 
	 * @param pgl
	 *            A new listener.
	 */
	public void addParserGUIListener(ParserGUIListener pgl) {
		this.listener.addElement(pgl);
	}

	/**
	 * Removes a listener.
	 * 
	 * @param pgl
	 *            The listener.
	 */
	public void removeParserGUIListener(ParserGUIListener pgl) {
		this.listener.remove(pgl);
	}

	void fireParserGUIEvent(Object data) {
		ParserGUIEvent event = new ParserGUIEvent(this, data);
		for (int i = 0; i < this.listener.size(); i++) {
			ParserGUIListener l = this.listener.elementAt(i);
			l.occured(event);
		}
	}

	// ======================================================================
	// Internal Frame Listener
	// ======================================================================
	/**
	 * This method is invoked when a internal frame is activated. This method is
	 * responsible for the update of the morphism.
	 * 
	 * @param e
	 *            The event from the internal frame
	 */
	public void internalFrameActivated(InternalFrameEvent e) {
		// Invoked when an internal frame is activated.	

		JInternalFrame jif = (JInternalFrame) e.getSource();
		Component c = jif.getContentPane().getComponent(0);
		
		if (jif == this.cpaGraphFrame
				&& this.activeGraphFrame != this.cpaGraphFrame) {
			if (this.ruleFrame1 != null && this.ruleFrame2 != null) {
				try {
					this.ruleFrame1.setIcon(true);
					this.ruleFrame2.setIcon(true);
				} catch (java.beans.PropertyVetoException pve) {}
			}
			if (!this.internalGraphFrames.isEmpty()) {
				Enumeration<JInternalFrame> en = this.internalGraphFrames.elements();
				while (en.hasMoreElements()) {
					JInternalFrame item = en.nextElement();
					if (item != this.cpaGraphFrame) {
						try {						
							item.setIcon(true);
						} catch (java.beans.PropertyVetoException pve) {}
					}
				}
			}						
			
			this.activeGraphFrame = jif;				
		} 
		else if (c instanceof GraphEditor) {
			// first deactivate last active frame
			if (this.activeGraphFrame != null) {
				Component co = this.activeGraphFrame.getContentPane()
						.getComponent(0);
				if (co instanceof GraphEditor) {
					GraphEditor gege = (GraphEditor) co;										
					Color bgcolor = Color.WHITE;
					if (gege.getGraphPanel() != null)
						gege.getGraphPanel().setBackground(bgcolor);
					if (this.ruleEdit1 != null && this.ruleEdit1.getLeftPanel() != null) {
						this.ruleEdit1.getLeftPanel().setBackground(bgcolor);					
						this.ruleEdit1.getRightPanel().setBackground(bgcolor);
					}
					if (this.ruleEdit2 != null && this.ruleEdit2.getLeftPanel() != null) {
						this.ruleEdit2.getNACPanel().setBackground(bgcolor);					
						this.ruleEdit2.getLeftPanel().setBackground(bgcolor);
						this.ruleEdit2.setNAC(null);
					}

					EdGraph eg = gege.getGraph();
					eg.clearMarks();
				}
			}
			this.activeGraphFrame = jif;
			if (this.activeGraphFrame.getX() == 0 && this.activeGraphFrame.getY() == 0) {
				this.activeGraphFrame.setLocation(dx, dy);
				dx = (dx==0)? 50 : 0;
				dy = (dy==0)? 50 : 0;
			}
			GraphEditor gege = (GraphEditor) c;
			EdGraph eg = gege.getGraph();
			Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> 
			morphs = this.overlappings.get(eg.getBasisGraph());
			if (morphs == null)
				return;

//			if (this.varEqualityDialogs.get(eg) != null
//					&& this.varEqualityDialogs.get(eg).isVisible()) {
//				this.varEqualityDialogs.get(eg).setEnabled(true);
//			}
			
			String nacName = eg.getBasisGraph().getHelpInfoAboutNAC();
			String pacName = eg.getBasisGraph().getHelpInfoAboutPAC();
			
			OrdinaryMorphism o1 = morphs.first.first;
			OrdinaryMorphism o2 = morphs.first.second;

			// set background color of overlapping panels
			Color bgcolor = new Color(255, 255, 165);
			gege.getGraphPanel().setBackground(bgcolor);
			
			if (CriticalPairData.isSwitchDependency(eg.getBasisGraph().getName())) {
				if (o1.getSource() == this.layoutRule2.getBasisRule().getLeft())
					this.ruleEdit2.getLeftPanel().setBackground(bgcolor);
				else if (o1.getSource() == this.layoutRule2.getBasisRule().getRight())
					this.ruleEdit2.getRightPanel().setBackground(bgcolor);
				
				EdNAC nac1 = resetNAC(this.ruleEdit1, this.layoutRule1, nacName, bgcolor);
				if (nac1 != null)
					this.ruleEdit1.getNACPanel().setBackground(bgcolor);					
				
				if (morphs.second != null) {
					if (morphs.second.first.getSource() == this.layoutRule1.getBasisRule().getLeft()) {
						this.ruleEdit1.getLeftPanel().setBackground(bgcolor);
						this.ruleEdit1.getRightPanel().setBackground(Color.WHITE);
					}
					else if (morphs.second.first.getSource() == this.layoutRule1.getBasisRule().getRight()) {
						this.ruleEdit1.getRightPanel().setBackground(bgcolor);
						this.ruleEdit1.getLeftPanel().setBackground(Color.WHITE);
					}
				} else {
					this.ruleEdit1.getRightPanel().setBackground(bgcolor);
				}
				
//				set morphism marks
				int indx = 0;
				EdMorphism numbers = new EdMorphism(null);
				Pair<OrdinaryMorphism, OrdinaryMorphism> morphsN2 = morphs.second;
				indx = numbers.makeVDiagram_NAC(this.layoutRule2.getBasisRule(), 
												this.layoutRule1.getBasisRule(), 
												o1, o2, morphsN2, indx);
				if (nac1 != null)
					indx = numbers.completeMorphismMarks(nac1.getMorphism(), numbers.getFirstTarget(), indx);
				
				eg.setMorphismMarks(numbers.getSourceOfMorphism(), true);
				setMorphismMarks(this.layoutRule2, null, numbers.getTargetOfMorphism(1), indx);
				setMorphismMarks(this.layoutRule1, nac1, numbers.getTargetOfMorphism(2), indx);

				fireParserGUIEvent(numbers);
				fireParserGUIEvent(eg.getBasisGraph());
			} else
			if (eg.getBasisGraph().getName().indexOf("produceEdge-deleteNode-")>=0) {								
				this.ruleEdit1.getLeftPanel().setBackground(bgcolor);				
				this.ruleEdit2.getLeftPanel().setBackground(bgcolor);
				
				// set morphism marks
				int indx = 0;
				EdMorphism numbers = new EdMorphism(null);
				indx = numbers.makeVDiagram(this.layoutRule1.getBasisRule(), 
											this.layoutRule2.getBasisRule(), 
											o1, o2, indx);

				eg.setMorphismMarks(numbers.getSourceOfMorphism(), true);
				setMorphismMarks(this.layoutRule1, null, numbers.getTargetOfMorphism(1), indx);
				setMorphismMarks(this.layoutRule2, null, numbers.getTargetOfMorphism(2), indx);
						
				fireParserGUIEvent(numbers);
				fireParserGUIEvent(eg.getBasisGraph());
			}			
			else {
				EdPAC pac2 = null;
				EdNAC nac2 = null;
				if (o1.getSource() == this.layoutRule1.getBasisRule().getLeft())
					this.ruleEdit1.getLeftPanel().setBackground(bgcolor);
				else if (o1.getSource() == this.layoutRule1.getBasisRule().getRight())
					this.ruleEdit1.getRightPanel().setBackground(bgcolor);
			
				nac2 = resetNAC(this.ruleEdit2, this.layoutRule2, nacName, bgcolor);							
				if (nac2 != null) {
					this.ruleEdit2.getNACPanel().setBackground(bgcolor);
				}

				pac2 = resetPAC(this.ruleEdit2, this.layoutRule2, pacName, bgcolor);
				if (pac2 != null) {
					this.ruleEdit2.getPACPanel().setBackground(bgcolor);
				}
				
				this.ruleEdit2.getLeftPanel().setBackground(bgcolor);
						
				// set morphism marks
				EdMorphism numbers = new EdMorphism(null);
				int indx = 0;
				
				Pair<OrdinaryMorphism, OrdinaryMorphism> condMorph2 = morphs.second;
				
				if (pac2 != null) {
					indx = numbers.makeVDiagram_PAC(
							this.layoutRule1.getBasisRule(), 
							this.layoutRule2.getBasisRule(), 
							o1, o2, condMorph2, pac2.getMorphism(), indx);
				}
				
				if (nac2 != null) {
					indx = numbers.makeVDiagram_NAC(
								this.layoutRule1.getBasisRule(), 
								this.layoutRule2.getBasisRule(), 
								o1, o2, condMorph2, indx);
				}
				else {
					indx = numbers.makeVDiagram(this.layoutRule1.getBasisRule(), 
												this.layoutRule2.getBasisRule(), 
												o1, o2, indx);
				}
				
				eg.setMorphismMarks(numbers.getSourceOfMorphism(), true);
				setMorphismMarks(this.layoutRule1, null, numbers.getTargetOfMorphism(1), indx);
				
				if (pac2 != null)
					setMorphismMarks(this.layoutRule2, pac2, numbers.getTargetOfMorphism(2), indx);
				
				if (nac2 != null)
					setMorphismMarks(this.layoutRule2, nac2, numbers.getTargetOfMorphism(2), indx);
				else 
					setMorphismMarks(this.layoutRule2, pac2, numbers.getTargetOfMorphism(2), indx);
								
				fireParserGUIEvent(numbers);
				fireParserGUIEvent(eg.getBasisGraph());
				
				// test CP-Data content
//				testCPData(this.layoutRule1.getBasisRule(), this.layoutRule2.getBasisRule());
			}
			
			gege.updateGraphics();

			// deactivate critical pair panels
			JInternalFrame[] allFrames = this.desktop.getAllFrames();
			if (allFrames != null) {
				for (int i = 0; i < allFrames.length; i++) {
					JInternalFrame f = allFrames[i];
					Component comp = f.getContentPane().getComponent(0);
					if (comp instanceof CriticalPairPanel)
						((CriticalPairPanel) comp).active = false;
					else
						return;
				}
			}						
		} else if (c instanceof CriticalPairPanel) {
			((CriticalPairPanel) c).active = true;
			if (this.ruleEdit1 != null) {
				this.ruleEdit1.getLeftPanel().setBackground(Color.WHITE);					
				this.ruleEdit1.getRightPanel().setBackground(Color.WHITE);
			}
			if (this.ruleEdit2 != null) {					
				this.ruleEdit2.getLeftPanel().setBackground(Color.WHITE);
				if (this.ruleEdit2.getLeftCondPanel() != null)
					this.ruleEdit2.getLeftCondPanel().setBackground(Color.WHITE);
			}
			fireParserGUIEvent(c);
		} else if (jif == this.ruleFrame1) {
			int xpos = (this.ruleFrame1.getX() < 0) ? 0 : this.ruleFrame1.getX();
			int ypos = (this.ruleFrame1.getY() < 0) ? 0 : this.ruleFrame1.getY();
			this.ruleFrame1.setLocation(xpos, ypos);
		} else if (jif == this.ruleFrame2) {
			int xpos = (this.ruleFrame2.getX() <= 0) ? 0 : this.ruleFrame2.getX();
			int ypos = (this.ruleFrame2.getY() <= 0) ? 50 : this.ruleFrame2.getY();
			this.ruleFrame2.setLocation(xpos, ypos);
		}
	}

	private void setMorphismMarks(EdRule r, EdGraph condGraph, HashMap<?,?> map, int lastMark) {
		if (condGraph == null) 
			r.setMorphismMarks(map);
		else if (condGraph instanceof EdNAC)
			r.setMorphismMarks(map, (EdNAC)condGraph);
		else if (condGraph instanceof EdPAC)
			r.setMorphismMarks(map, (EdPAC)condGraph);
	}

	
	/**
	 * This method is invoked when a internal frame is closed.
	 * 
	 * @param e
	 *            The event from the internal frame.
	 */
	public void internalFrameClosed(InternalFrameEvent e) {
		// Invoked when an internal frame has been closed.
		JInternalFrame jif = (JInternalFrame) e.getSource();
		Component c = jif.getContentPane().getComponent(0);
		if (c instanceof GraphEditor) {			
			GraphEditor gege = (GraphEditor) c;
			EdGraph eg = gege.getGraph();
			if (eg.isCPAgraph()) {
				if (jif.isIcon()) {
					this.desktop.remove(jif.getDesktopIcon());
				} else {
					this.desktop.remove(jif);
				}
			} else {
				if (this.varEqualityDialogs.get(eg) != null) {
					this.varEqualityDialogs.get(eg).setVisible(false);
					this.varEqualityDialogs.remove(eg);
				}
			}
		} 
		else if (c instanceof CriticalPairPanel) {
			if (jif.isIcon()) {
				this.desktop.remove(jif.getDesktopIcon());
			} else {
				this.desktop.remove(jif);
			}
		}
		else if (c instanceof RuleEditor) {
			RuleEditor re = (RuleEditor) c;
			if (jif.isIcon()) {
				this.desktop.remove(jif.getDesktopIcon());
			} else {
				this.desktop.remove(jif);
			}
			if (re == this.ruleEdit1)
				this.ruleFrame1 = null;
			else if (re == this.ruleEdit2)
				this.ruleFrame2 = null;
		}
	}

	/**
	 * This method is invoked when a internal frame is closing.
	 * 
	 * @param e
	 *            The event from the internal frame.
	 */
	public void internalFrameClosing(InternalFrameEvent e) {
		// Invoked when an internal frame is in the process of being closed.
	}

	/**
	 * This method is invoked when a internal frame is deactevated.
	 * 
	 * @param e
	 *            The event from the internal frame.
	 */
	public void internalFrameDeactivated(InternalFrameEvent e) {
		// Invoked when an internal frame is de-activated.
	}

	/**
	 * This method is invoked when a internal frame is deiconified.
	 * 
	 * @param e
	 *            The event from the internal frame.
	 */
	public void internalFrameDeiconified(InternalFrameEvent e) {
		// Invoked when an internal frame is de-iconified.
//		JInternalFrame jif = (JInternalFrame) e.getSource();
//		Component c = jif.getContentPane().getComponent(0);
//		if (c instanceof GraphEditor) {			
//			GraphEditor gege = (GraphEditor) c;
//			EdGraph eg = gege.getGraph();
//			if (!eg.isCPAgraph()
//					&& !eg.getBasisGraph().getHelpInfoAboutVariableEquality().equals("")) {
//				this.desktop.setToolTipText(
//						"Use graph panel background pop-up menu to see variable equality");
//			}
//		}
	}

	/**
	 * This method is invoked when a internal frame is iconified.
	 * 
	 * @param e
	 *            The event from the internal frame.
	 */
	public void internalFrameIconified(InternalFrameEvent e) {
		// Invoked when an internal frame is iconified.		
		JInternalFrame jif = (JInternalFrame) e.getSource();
		Component c = jif.getContentPane().getComponent(0);
		if (c instanceof CriticalPairPanel) {
			if (((CriticalPairPanel) c).getPairContainer().getKindOfConflict() == CriticalPair.CONFLICT)
				jif.getDesktopIcon().setLocation(5, 5);
			else
				jif.getDesktopIcon().setLocation(5,
						jif.getDesktopIcon().getSize().height);
		} else if (jif == this.cpaGraphFrame) {
			jif.getDesktopIcon().setLocation(5,
					jif.getDesktopIcon().getSize().height * 2);
		} 
		else {
			if (c instanceof GraphEditor) {			
				GraphEditor gege = (GraphEditor) c;
				EdGraph eg = gege.getGraph();
				if (this.varEqualityDialogs.get(eg) != null) {
					this.varEqualityDialogs.get(eg).setVisible(false);
					this.varEqualityDialogs.remove(eg);
				}					
			}
		}
		
	}

	/**
	 * This method is invoked when a internal frame is opened.
	 * 
	 * @param e
	 *            The event from the internal frame.
	 */
	public void internalFrameOpened(InternalFrameEvent e) {
	}

	private void makeCPAGraphMenu() {
		this.cpaGraphMenu.add(this.miC);
		this.cpaGraphMenu.add(this.miD);
		this.cpaGraphMenu.add(this.miAll);
		this.cpaGraphMenu.addSeparator();
		
		this.cpaGraphMenu.add(this.miHide);
		this.cpaGraphMenu.addSeparator();
		this.cpaGraphMenu.add(this.miStraightEdges);
		this.cpaGraphMenu.addSeparator();

		makeCPAEdgeShapeMenu(this.miShapeC, "Conflict Edge Style", "Conflict");
		this.cpaGraphMenu.add(this.miShapeC);
		makeCPAEdgeShapeMenu(this.miShapeD, "Dependency Edge Style", "Dependency");
		this.cpaGraphMenu.add(this.miShapeD);
		this.cpaGraphMenu.addSeparator();

		this.cpaGraphMenu.add(this.miLayoutGraph);
		this.cpaGraphMenu.addSeparator();
		
		this.cpaGraphMenu.add(this.miRefresh);
		this.cpaGraphMenu.addSeparator();
		
		this.cpaGraphMenu.add(this.miGraphExportJPG);
		this.miGraphExportJPG.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (GraphDesktop.this.activeGraphPanel != null
						&& GraphDesktop.this.activeGraphPanel.getGraph() != null) {					
					GraphDesktop.this.exportJPEG.save(GraphDesktop.this.activeGraphPanel.getCanvas());
				}
			}
		});

		this.cpaGraphMenu.pack();
		this.cpaGraphMenu.setBorderPainted(true);
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
	}

	private void makeGraphMenu() {
		this.graphMenu.add(this.miVarEqual_graphMenu);
		this.miVarEqual_graphMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (GraphDesktop.this.activeGraphPanel != null
						&& GraphDesktop.this.activeGraphPanel.getGraph() != null) {				
					
					showVarEqualityDialog(GraphDesktop.this.activeGraphPanel.getGraph(), 
												GraphDesktop.this.locationOnScreen);
				}
			}
		});
		
		this.graphMenu.addSeparator();
		
		this.graphMenu.add(this.miAddToGraphs_graphMenu);
		this.miAddToGraphs_graphMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ((GraphDesktop.this.activeGraphPanel != null)
						&& (GraphDesktop.this.activeGraphPanel.getGraph() != null)
						&& (GraphDesktop.this.layout != null)) {
					addGraphToHostGraphs();
				}
			}
		});

		this.graphMenu.add(this.miAddToNACs_graphMenu);
		this.miFirstRule = this.miAddToNACs_graphMenu.add(new JMenuItem("Of First Rule"));
		this.miFirstRule.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addGraphToNACs(true);
			}
		});
		this.miSecondRule = this.miAddToNACs_graphMenu.add(new JMenuItem("Of Second Rule"));
		this.miSecondRule.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addGraphToNACs(false);
			}
		});
				
		this.graphMenu.addSeparator();
		
		this.graphMenu.add(this.miLayout_graphMenu);
		this.miLayout_graphMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (GraphDesktop.this.activeGraphPanel != null
						&& GraphDesktop.this.activeGraphPanel.getGraph() != null) {
					makeLayout(GraphDesktop.this.activeGraphPanel.getGraph(), 
								GraphDesktop.this.activeGraphPanel.getSize());
					GraphDesktop.this.activeGraphPanel.updateGraphics();
				}
			}
		});
		
		this.graphMenu.addSeparator();
		
		this.graphMenu.add(this.miExportJPG_graphMenu);
		this.miExportJPG_graphMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (GraphDesktop.this.activeGraphPanel != null
						&& GraphDesktop.this.activeGraphPanel.getGraph() != null) {
					GraphDesktop.this.activeGraphPanel.setBackground(Color.white);
					GraphDesktop.this.exportJPEG.save(GraphDesktop.this.activeGraphPanel.getCanvas());
					GraphDesktop.this.activeGraphPanel.setBackground(new Color(255, 255, 165));
				}
			}
		});
		
		this.graphMenu.pack();
		this.graphMenu.setBorderPainted(true);
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
	}

	void addGraphToHostGraphs() {
		int levelOfTGcheck = this.layout.getLevelOfTypeGraphCheck();
		this.layout.getTypeSet().getBasisTypeSet().setLevelOfTypeGraph(TypeSet.ENABLED);
		
		EdGraph g = this.activeGraphPanel.getGraph().copy();
		g.unsetAttributeValueWhereVariable();
		// because g should be added to host graphs,
		// remove all variables of node / edge attributes
	
		if (this.layout.addGraph(g)) {
				
			Collection<TypeError> errors = this.layout.setLevelOfTypeGraphCheck(levelOfTGcheck);			
			if (errors == null || errors.isEmpty()) {
				if (this.parentFrame instanceof agg.gui.AGGAppl) {									
					TreePath path = ((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView()
											.getTreePathOfGrammarElement(this.layout);
					((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().getTree().setSelectionPath(path);
					((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().selectPath(
								((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().getTree().getRowForPath(path));
					this.layout.setEditable(true);
					if (((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().addGraph(this.layout, g)) {
						JOptionPane.showMessageDialog(
							this.parentFrame,
								"<html><body>"
								+"You will see currently added graph in the GraGra tree view <br>"
								+ "when you are back to main AGG GUI. <br>"
								+"Please check attribute settings of nodes and edges."
								+"</html></body>");
					}
					this.layout.setEditable(false);
				} else {
					JOptionPane.showMessageDialog(
							null,
								"<html><body>"
								+"To see the saved graph in the GraGra tree view, <br>"
								+ "please reload the grammar of CPA <br>"
								+ "when you are back to main AGG GUI. <br>"
								+"Please check attribute settings of nodes and edges."
								+"</html></body>");
				}
			} else {
	//			System.out.println(errors.iterator().next().getMessage());
				if (this.parentFrame instanceof agg.gui.AGGAppl) {
					// set level of TypeGraph to enabled
					TreePath path = ((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView()
											.getTreePathOfGrammarElement(this.layout.getTypeGraph());
					if (path != null) {
						((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().selectPath(
									((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().getTree().getRowForPath(path));
						((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().setTypeGraphLevel(TypeSet.ENABLED);
					}
					// add graph to current gragra
					path = ((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView()
											.getTreePathOfGrammarElement(this.layout);
					if (path != null) {
						boolean graEditable = this.layout.isEditable();
						if (!graEditable)
							this.layout.setEditable(true);
						((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().selectPath(
									((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().getTree().getRowForPath(path));
						((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().addGraph(this.layout, g);
						if (!graEditable)
							this.layout.setEditable(false);
					}
															
					JOptionPane.showMessageDialog(
							this.parentFrame,
								"<html><body>"
								+"You will see currently added graph in the GraGra tree view <br>"
								+ "when you are back to main AGG GUI.<br> <br>"
								+ "<font color=\"#FF0000\">WARNING!</font><br>"
								+"Maybe the level of the Type Graph was changed to "
								+ "<font color=\"#FF0000\">enabled</font>."				
								+"</html></body>");
				} else {									
					JOptionPane.showMessageDialog(
								null,
								"<html><body>"
								+"To see currently saved graph in the GraGra tree view, <br>"
								+ "please reload the grammar of CPA <br>"
								+ "when you are back to main AGG GUI.<br> <br>"
								+ "<font color=\"#FF0000\">WARNING!</font><br>"
								+"Maybe the level of the Type Graph was changed to "
								+ "<font color=\"#FF0000\">enabled</font>."				
								+"</html></body>");
				}
			}
		}
	}
	
	/*
	 * Make a <code>filter NAC</code> from the selected overlapping graph
	 * and add it to the first rule.
	 */
	void addGraphToNACs(boolean firstRule) {
		if (this.activeGraphPanel != null
				&& this.activeGraphPanel.getGraph() != null
				&& this.layout != null) {
			
			int levelOfTGcheck = this.layout.getLevelOfTypeGraphCheck();
			this.layout.getTypeSet().getBasisTypeSet().setLevelOfTypeGraph(TypeSet.ENABLED);
			
			// get the rule
			EdRule r = firstRule? this.ruleEdit1.getRule(): this.ruleEdit2.getRule();		
			// get the critical graph 
			EdGraph g = this.activeGraphPanel.getGraph();
			// get attr variable names equality
			Hashtable<String,String> 
			varEqualName = VariableEqualityDialog.getVarNameEquality(g.getBasisGraph().getHelpInfoAboutVariableEquality());
			// get critical pair
			Pair<Pair<OrdinaryMorphism,OrdinaryMorphism>,Pair<OrdinaryMorphism,OrdinaryMorphism>> 
			cp = this.overlappings.get(g.getBasisGraph());
			boolean mapOK = true;
			JLabel errMsg = new JLabel();
			
			OrdinaryMorphism bnac = this.makeNAC(cp, r.getBasisRule(), g.getBasisGraph(), firstRule, errMsg);
//			OrdinaryMorphism bnac = CriticalPairData.makeNACFromGraph(cp, r.getBasisRule(), g.getBasisGraph(), firstRule);
			mapOK = (bnac != null && !bnac.isEmpty());

			if (mapOK) {
				insertNACIntoGrammar(bnac, g, r, varEqualName, firstRule);
			} else {
				JOptionPane.showMessageDialog(
						null,
						"It was not possible to create a NAC based on this critical graph.\n"
						+errMsg.getText(),
						"Rule:  "+r.getName(),
						JOptionPane.ERROR_MESSAGE);
			}
			this.layout.getTypeSet().getBasisTypeSet().setLevelOfTypeGraph(levelOfTGcheck);
		}
	}
	
	private OrdinaryMorphism makeNAC(
			final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
			p,
			final Rule r, 
			final Graph g,
			boolean firstRule,
			JLabel errMsg) {
		
		// make help iso-morphism
		OrdinaryMorphism iso = g.isoCopy();
		if (iso == null)
			return null;
		
		iso.getTarget().setName(iso.getTarget().getName().replace("_copy", ""));
		// create a nac morphism : r.LHS --> iso.target
		OrdinaryMorphism bnac = BaseFactory.theFactory().createMorphism(
												r.getLeft(), iso.getTarget());
		Pair<OrdinaryMorphism,OrdinaryMorphism> cp1 = p.first;
		Pair<OrdinaryMorphism,OrdinaryMorphism> cp2 = p.second;
		OrdinaryMorphism o1 = cp1.first;
		OrdinaryMorphism o2 = cp1.second;
		boolean mapOK = true;
		// set nac mappings
		if (firstRule) {				
			Enumeration<GraphObject> dom = o1.getDomain();
			while (dom.hasMoreElements()) {
				GraphObject go = dom.nextElement();
				try {
					if (go.getContext() == r.getLeft()) {
						bnac.addMapping(go, iso.getImage(o1.getImage(go)));
					} else if (go.getContext() == r.getRight()) {
						Enumeration<GraphObject> inverse = r.getInverseImage(go);
						if (inverse.hasMoreElements()) {
							GraphObject goL = inverse.nextElement(); 				
							bnac.addMapping(goL, iso.getImage(o1.getImage(go)));
						} else {
							errMsg.setText("One of critical objects has reference to a new RHS object.");						
							mapOK = false;
							break;
						}
					} 
				} catch (BadMappingException ex) {
//					System.out.println(ex.getMessage());
					mapOK = false;
				}
			}
			mapOK = mapOK && !bnac.isEmpty();
		} else {
			Enumeration<GraphObject> dom = o2.getDomain();
			while (dom.hasMoreElements()) {
				GraphObject go = dom.nextElement();
				try {
					if (go.getContext() == r.getLeft()) {
						bnac.addMapping(go, iso.getImage(o2.getImage(go)));
					} else if (go.getContext() == cp2.first.getTarget()) {
						Enumeration<GraphObject> inverse = cp2.first.getInverseImage(go);
						if (inverse.hasMoreElements()) {
							GraphObject goL = inverse.nextElement(); 				
							bnac.addMapping(goL, iso.getImage(o2.getImage(go)));
						} 
					}
				} catch (BadMappingException ex) {
//					System.out.println(ex.getMessage());
					mapOK = false;
				}
			}
			mapOK = mapOK && !bnac.isEmpty();
		}
		iso.dispose();
		if (!mapOK) {
			bnac.dispose(false, true);
			return null;
		}
		
		return bnac;
	}

	
	private void insertNACIntoGrammar(
			final OrdinaryMorphism bnac, 
			final EdGraph g, 
			final EdRule r,
			final Hashtable<String,String> varEqualName,
			boolean firstRule) {
		
		// make layout nac and add it to layout rule
		EdNAC nac = new EdNAC(bnac);
		nac.setLayoutByIndex(g, true);			
		
		if (this.parentFrame instanceof agg.gui.AGGAppl) {									
			TreePath path = ((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView()
									.getTreePathOfGrammarElement(r);
			((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().getTree().setSelectionPath(path);
			((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().selectPath(
						((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().getTree().getRowForPath(path));
			// add nac to rule and into tree 
			if (((agg.gui.AGGAppl) this.parentFrame).getGraGraTreeView().addNAC(r, nac)) {
				// adjust attr variables 
				renameEqualVar(nac.getBasisGraph(), r.getBasisRule().getAttrContext().getVariables(), varEqualName, firstRule);
				r.getBasisRule().addToAttrContext(nac.getBasisGraph().getAttrContext());
				
				JOptionPane.showMessageDialog(
							this.parentFrame,
							"<html><body>"
							+"You will see currently added NAC in the GraGra tree view <br>"
							+ "when you are back to main AGG GUI. <br>"
							+"Please check attribute settings of nodes and edges."
							+"</html></body>",
							"Rule:  "+r.getName(),
							JOptionPane.INFORMATION_MESSAGE);
			}
		} else {
			// add nac to rule
			r.addNAC(nac);
			// adjust attr variables 
			renameEqualVar(nac.getBasisGraph(), r.getBasisRule().getAttrContext().getVariables(), varEqualName, false);
			r.getBasisRule().addToAttrContext(nac.getBasisGraph().getAttrContext());
			
			JOptionPane.showMessageDialog(
						null,
						"<html><body>"
						+"To see the saved NAC in the GraGra tree view, <br>"
						+ "please reload the grammar of CPA <br>"
						+ "when you are back to main AGG GUI. <br>"
						+"Please check attribute settings of nodes and edges."
						+"</html></body>",
						"Rule:  "+r.getName(),
						JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	private void renameEqualVar(Graph g, AttrVariableTuple attrCont, Hashtable<String,String> varEqualName, boolean firstRule) {
		Iterator<Node> nodes = g.getNodesCollection().iterator();
		while (nodes.hasNext()) {
			Node go = nodes.next();
			if (go.getAttribute() != null)
				renameEqualVar(go, attrCont, varEqualName, firstRule);
		}
		Iterator<Arc> arcs = g.getArcsCollection().iterator();
		while (arcs.hasNext()) {
			Arc go = arcs.next();
			if (go.getAttribute() != null)
				renameEqualVar(go, attrCont, varEqualName, firstRule);
		}
	}
	
	private void renameEqualVar(GraphObject go, AttrVariableTuple attrCont, Hashtable<String,String> varEqualName, boolean firstRule) {
		AttrTuple attr = go.getAttribute();
		for (int i=0; i<attr.getNumberOfEntries(); i++) {
			ValueMember m = (ValueMember)attr.getMemberAt(i);
			if (m.isSet() && m.getExpr().isVariable()) {
				String vnameG = m.getExprAsText();										
				if (firstRule) {
					if (attrCont.getMemberAt(vnameG.replace("r1_", "")) != null) {
						String vR = vnameG.replace("r1_", "");
						m.setExprAsText(vR);
					} else {
						Enumeration<String> names1 = varEqualName.keys();
						while (names1.hasMoreElements()) {
							String n1 = names1.nextElement().replace("[", "");
							if (vnameG.contains(n1)) {
								String vR = n1.replace("r1_", "");
								m.setExprAsText(vR);
							}
						}
					}
				} else {
					if (attrCont.getMemberAt(vnameG.replace("r2_", "")) != null) {
						String vR = vnameG.replace("r2_", "");
						m.setExprAsText(vR);
					} else {
						Iterator<String> names2 = varEqualName.values().iterator();
						while (names2.hasNext()) {
							String n2 = names2.next().replace("]", "");
							if (vnameG.contains(n2)) {
								String vR = n2.replace("r2_", "");
								m.setExprAsText(vR);
							}
						}
					}
				}
			}
		}
	}
	
	protected void makeLayout(EdGraph g, Dimension d) {
		g.updateVisibility();
		final List<EdNode> visiblenodes = g.getVisibleNodes();

		g.setCurrentLayoutToDefault(false);
		g.getDefaultGraphLayouter().setEnabled(true);
		Dimension dim = g.getDefaultGraphLayouter().getNeededPanelSize(visiblenodes);
		if (dim.width < 350)
			dim.width = 350;
		if (dim.width < d.width)
			dim.width = d.width;
		if (dim.height < 350)
			dim.height = 350;
		if (dim.height < d.height)
			dim.height = d.height;
		g.getDefaultGraphLayouter().setPanelSize(dim);
		g.getDefaultGraphLayouter().allowChangePanelSize(false);
		g.getDefaultGraphLayouter().setEnabled(true);
		g.doDefaultEvolutionaryGraphLayout(
				g.getDefaultGraphLayouter(), 100, 10);
	}

	private JMenu makeCPAEdgeShapeMenu(final JMenu shape, String title,
			String kind) {
		// Arc Style
		shape.setText(title);
		final JMenuItem miSolid = shape.add(new JMenuItem("Solid Line"));
		miSolid.setHorizontalTextPosition(SwingConstants.RIGHT);
		miSolid.setIcon((new agg.gui.icons.ColorSolidLineIcon(Color.black)));
		miSolid.setActionCommand(kind + "Solid");

		final JMenuItem miDot = shape.add(new JMenuItem("Dot Line"));
		miDot.setHorizontalTextPosition(SwingConstants.RIGHT);
		miDot.setIcon(new agg.gui.icons.ColorDotLineIcon(Color.black));
		miDot.setActionCommand(kind + "Dot");

		final JMenuItem miDash = shape.add(new JMenuItem("Dash Line"));
		miDash.setHorizontalTextPosition(SwingConstants.RIGHT);
		miDash.setIcon(new agg.gui.icons.ColorDashLineIcon(Color.black));
		miDash.setActionCommand(kind + "Dash");
		return shape;
	}

	public void addActionListenerToCPAGraphMenu(ActionListener l) {
		this.miC.addActionListener(l);
		this.miD.addActionListener(l);
		this.miAll.addActionListener(l);
		this.miRefresh.addActionListener(l);
		this.miStraightEdges.addActionListener(l);
		this.miHide.addActionListener(l);
		this.miLayoutGraph.addActionListener(l);
		for (int i = 0; i < this.miShapeC.getItemCount(); i++) {
			this.miShapeC.getItem(i).addActionListener(l);
		}
		for (int i = 0; i < this.miShapeD.getItemCount(); i++) {
			this.miShapeD.getItem(i).addActionListener(l);
		}
	}

	public void removeActionListenerFromCPAGraphMenu(ActionListener l) {
		this.miC.removeActionListener(l);
		this.miD.removeActionListener(l);
		this.miAll.removeActionListener(l);
		this.miRefresh.removeActionListener(l);
		this.miStraightEdges.removeActionListener(l);
		this.miHide.removeActionListener(l);
		this.miLayoutGraph.removeActionListener(l);
		for (int i = 0; i < this.miShapeC.getItemCount(); i++) {
			this.miShapeC.getItem(i).removeActionListener(l);
		}
		for (int i = 0; i < this.miShapeD.getItemCount(); i++) {
			this.miShapeD.getItem(i).removeActionListener(l);
		}
	}

	
	private void setMenuBarOfConflictTableFrame(final JMenu m) {
		final JMenuBar mb = new JMenuBar();
		mb.add(m);
		this.conflictFrame.setJMenuBar(mb);
		
		JMenuItem item = makeConflictMenuItem("delete - use conflict", m);
		item.setActionCommand(CriticalPairData.DELETE_USE_C_TXT);
		item = makeConflictMenuItem("delete - need ( PAC ) conflict", m);
		item.setActionCommand(CriticalPairData.DELETE_NEED_C_TXT);
		m.addSeparator();
		item = makeConflictMenuItem("produce - forbid ( NAC ) conflict", m);
		item.setActionCommand(CriticalPairData.PRODUCE_FORBID_C_TXT);
		item = makeConflictMenuItem("produce Edge - delete Node conflict", m);
		item.setActionCommand(CriticalPairData.PRODUCE_EDGE_DELETE_NODE_C_TXT);
		m.addSeparator();
		item = makeConflictMenuItem("change - use attr-conflict", m);
		item.setActionCommand(CriticalPairData.CHANGE_USE_ATTR_C_TXT);
		item = makeConflictMenuItem("change - forbid ( NAC ) attr-conflict", m);
		item.setActionCommand(CriticalPairData.CHANGE_FORBID_ATTR_D_TXT);
		item = makeConflictMenuItem("change - need ( PAC ) attr-conflict", m);
		item.setActionCommand(CriticalPairData.CHANGE_NEED_ATTR_C_TXT);
		m.addSeparator();
		item = makeConflictMenuItem("All Conflicts", m);
		item.setActionCommand("ALL");
		this.miAllConfs = item;
		
		m.addSeparator();
		m.addSeparator();
		final JMenuItem mi = m.add(new JMenuItem(" Export JPEG "));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (GraphDesktop.this.exportJPEG != null)
					GraphDesktop.this.exportJPEG.save(GraphDesktop.this.conflictFrame);
			}
		});
	}

	private JMenuItem makeConflictMenuItem(String txt, final JMenu m) {
		JMenuItem mi = new JMenuItem(txt);
		mi.setForeground(Color.red);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (((JMenuItem)e.getSource()).getActionCommand().equals("ALL")) {
					m.setForeground(Color.black);
					m.setText(" Show ");
					GraphDesktop.this.conflictPanel.refreshView();
					fireParserGUIEvent(null);
				}
				else {
					m.setForeground(Color.red);
					m.setText(" Show  :       "+((JMenuItem)e.getSource()).getText());
					GraphDesktop.this.conflictPanel.showCriticalPairsOfKind(((JMenuItem)e.getSource()).getActionCommand());	
					fireParserGUIEvent(null);
				}
			}
		});
		m.add(mi);
		return mi;
	}
	
	public void doClickShowAllConflicts() {
		this.confsMenu.setForeground(Color.black);
		this.confsMenu.setText(" Show ");
	}
	
	private JMenuItem makeDependMenuItem(String txt, final JMenu m) {
		JMenuItem mi = new JMenuItem(txt);
		mi.setForeground(Color.blue);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (((JMenuItem)e.getSource()).getActionCommand().equals("ALL")) {
					m.setForeground(Color.black);
					m.setText(" Show ");
					GraphDesktop.this.dependPanel.refreshView();
					fireParserGUIEvent(null);
				}
				else {
					m.setForeground(Color.blue);
					m.setText(" Show  :       "+((JMenuItem)e.getSource()).getText());
					GraphDesktop.this.dependPanel.showCriticalPairsOfKind(((JMenuItem)e.getSource()).getActionCommand());
					fireParserGUIEvent(null);
				}
			}
		});
		m.add(mi);
		return mi;
	}
	
	public void doClickShowAllDepends() {
		this.depsMenu.setForeground(Color.black);
		this.depsMenu.setText(" Show ");
	}
	
	private void setMenuBarOfDependencyTableFrame(final JMenu m) {
		final JMenuBar mb = new JMenuBar();
		mb.add(m);
		this.dependFrame.setJMenuBar(mb);
		
		JMenuItem item = makeDependMenuItem("produce - use dependency", m);
		item.setActionCommand(CriticalPairData.PRODUCE_USE_D_TXT);
		item = makeDependMenuItem("produce - <use & delete> dependency", m); 
		item.setActionCommand(CriticalPairData.PRODUCE_DELETE_D_TXT);
		item = makeDependMenuItem("produce - <use & change> dependency", m);
		item.setActionCommand(CriticalPairData.PRODUCE_CHANGE_D_TXT);
		m.addSeparator();
		item = makeDependMenuItem("produce - need ( PAC ) dependency", m);
		item.setActionCommand(CriticalPairData.PRODUCE_NEED_D_TXT);
		m.addSeparator();
		item = makeDependMenuItem("delete - forbid ( NAC ) dependency", m);	
		item.setActionCommand(CriticalPairData.DELETE_FORBID_D_TXT);
		m.addSeparator();
		item = makeDependMenuItem("change - use attr-dependency", m);
		item.setActionCommand(CriticalPairData.CHANGE_USE_ATTR_D_TXT);
		item = makeDependMenuItem("change - forbid ( NAC ) attr-dependency", m);
		item.setActionCommand(CriticalPairData.CHANGE_FORBID_ATTR_D_TXT);
		m.addSeparator();
		item = makeDependMenuItem("All Dependencies", m);
		item.setActionCommand("ALL");
		this.miAllDeps = item;
		
		m.addSeparator();
		m.addSeparator();
		
		final JMenuItem mi = m.add(new JMenuItem(" Export JPEG "));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (GraphDesktop.this.exportJPEG != null)
					GraphDesktop.this.exportJPEG.save(GraphDesktop.this.dependFrame);
			}
		});
	}

	
	private void testCPData(Rule r1, Rule r2) {
		CriticalPairPanel panel = null;
		if (this.conflictPanel != null && this.conflictPanel.isEnabled())
			panel = this.conflictPanel;
		else if (this.dependPanel != null && this.dependPanel.isEnabled())
			panel = this.dependPanel;
		if (panel == null)
			return;
		
		CriticalPairData cpdata = panel.getPairContainer().getCriticalPairData(r1, r2);
		List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
		delUse = cpdata.getDeleteUseConflicts();
		List<Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>>
		delNeed = cpdata.getDeleteNeedConflicts();
		System.out.println("CriticalPairData  elements::");
		cpdata.resetIterator();
		while (cpdata.hasCriticals()) {
			cpdata.next();
			Graph g = cpdata.getCriticalGraph();
			String gname = g.getName();
			System.out.println(gname);
			
			String ghelpNAC = g.getHelpInfoAboutNAC();
			String ghelpPAC = g.getHelpInfoAboutPAC();
			
			OrdinaryMorphism duetoNAC = cpdata.getMorph2DueToNAC();
			if (duetoNAC != null) {
				Vector<GraphObject> out = duetoNAC.getDomainObjects();
				System.out.println("due to NAC: "+duetoNAC.getSource().getName()+" ::: "+out);
			}
			OrdinaryMorphism duetoPAC = cpdata.getMorph2DueToPAC();
			if (duetoPAC != null) {
				Vector<GraphObject> out = duetoPAC.getDomainObjects();
				System.out.println("due to PAC: "+duetoPAC.getSource().getName()+" ::: "+out);
			}
			OrdinaryMorphism duetoLHS2 = cpdata.getMorph2DueToLHS();
			if (duetoLHS2 != null) {
				Vector<GraphObject> out = duetoLHS2.getDomainObjects();
				System.out.println("due to LHS2: "+duetoLHS2.getSource().getName()+" ::: "+out);
			}					
		}
	}
	
	
}

/*
 * $Log: GraphDesktop.java,v $
 * Revision 1.26  2010/12/21 13:40:34  olga
 * improved - show CPA Graph
 *
 * Revision 1.25  2010/12/20 20:43:37  olga
 * tuning
 *
 * Revision 1.24  2010/12/20 20:07:14  olga
 * improved - show CPA Graph
 *
 * Revision 1.23  2010/12/17 15:44:31  olga
 * improved - show CPA Graph
 *
 * Revision 1.22  2010/11/06 18:31:54  olga
 * extended and improved
 *
 * Revision 1.21  2010/11/04 10:57:36  olga
 * improved
 *
 * Revision 1.20  2010/09/23 08:18:50  olga
 * tuning
 *
 * Revision 1.19  2010/08/16 09:33:18  olga
 * improved
 *
 * Revision 1.18  2010/08/16 08:57:29  olga
 * tuning
 *
 * Revision 1.17  2010/08/12 14:55:14  olga
 * tuning
 *
 * Revision 1.16  2010/08/09 14:11:34  olga
 * improved
 *
 * Revision 1.15  2010/08/09 14:09:56  olga
 * extended by possibility to add a critical graph as NAC of the first or second rule
 *
 * Revision 1.14  2010/08/09 14:01:05  olga
 * extended by possibility to add a critical graph as NAC of the first or second rule
 *
 * Revision 1.13  2010/08/05 14:12:58  olga
 * tuning
 *
 * Revision 1.12  2010/07/29 10:12:12  olga
 * tuning
 *
 * Revision 1.11  2010/06/09 11:06:51  olga
 * tuning
 *
 * Revision 1.10  2010/03/08 15:41:21  olga
 * code optimizing
 *
 * Revision 1.9  2009/08/05 14:15:11  olga
 * CPA: add critical graph to the current gragra - improved
 *
 * Revision 1.8  2009/05/28 13:18:28  olga
 * Amalgamated graph transformation - development stage
 *
 * Revision 1.7  2009/04/27 07:37:17  olga
 * Copy and Paste TypeGraph- bug fixed
 * CPA - dangling edge conflict when first produce second delete - extended
 *
 * Revision 1.6  2009/03/25 15:19:17  olga
 * code tuning
 *
 * Revision 1.5  2009/03/19 10:07:50  olga
 * code tuning
 *
 * Revision 1.4  2009/03/19 09:31:08  olga
 * CPE: attr check improved
 *
 * Revision 1.3  2009/02/23 09:01:56  olga
 * Convert Atomic Graph Constraints to Post Application Condition of Rule - bug fixed, reworked and improved error messaging
 * Graph copy - bug fixed
 * Code tuning
 *
 * Revision 1.2  2008/11/13 08:26:20  olga
 * some tests
 *
 * Revision 1.1  2008/10/29 09:04:11  olga
 * new sub packages of the package agg.gui: typeeditor, editor, trafo, cpa, options, treeview, popupmenu, saveload
 *
 * Revision 1.42  2008/09/22 10:03:48  olga
 * tests
 *
 * Revision 1.41  2008/09/11 09:22:26  olga
 * Some changes in CPA: new computing of conflicts after an option changed,
 * Graph layout of overlapping graphs
 *
 * Revision 1.40  2008/09/04 07:50:27  olga
 * GUI extension: hide nodes, edges
 *
 * Revision 1.39  2008/07/21 10:03:28  olga
 * Code tuning
 *
 * Revision 1.38  2008/05/05 09:11:52  olga
 * Graph parser - bug fixed.
 * New AGG feature - Applicability of Rule Sequences - in working.
 *
 * Revision 1.37  2008/04/07 09:36:56  olga
 * Code tuning: refactoring + profiling
 * Extension: CPA - two new options added
 *
 * Revision 1.36  2008/02/25 08:44:49  olga
 * Extending of CPA: new class CriticalRulePairAtGraph to get critical
 * matches of two rules at a concret graph.
 *
 * Revision 1.35  2008/02/18 09:37:10  olga
 * - an extention of rule dependency check is implemented;
 * - some bugs fixed;
 * - editing of graphs improved
 *
 * Revision 1.34  2007/12/10 08:42:58  olga
 * CPA of grammar with node type inheritance for attributed graphs - bug fixed
 *
 * Revision 1.33  2007/11/19 08:48:41  olga
 * Some GUI usability mistakes fixed.
 * Default values in node/edge of a type graph implemented.
 * Code tuning.
 *
 * Revision 1.32  2007/11/12 08:48:57  olga
 * Code tuning
 *
 * Revision 1.31  2007/11/08 12:57:00  olga
 * working on CPA inconsistency for rules with pacs and inheritance
 * bugs are possible
 *
 * Revision 1.30  2007/11/05 09:18:21  olga
 * code tuning
 *
 * Revision 1.29  2007/11/01 09:58:18  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.28  2007/10/24 08:21:29  olga
 * CPA with inheritance: implementierung and test
 *
 * Revision 1.27  2007/09/27 08:42:47  olga
 * CPA: new option  -ignore pairs with same rules and same matches-
 *
 * Revision 1.26  2007/09/24 09:42:39  olga
 * AGG transformation engine tuning
 * Revision 1.25 2007/09/10 13:05:44 olga In this
 * update: - package xerces2.5.0 is not used anymore; - class
 * com.objectspace.jgl.Pair is replaced by the agg own generic class
 * agg.util.Pair; - bugs fixed in: usage of PACs in rules; match completion;
 * usage of static method calls in attr. conditions - graph editing: added some
 * new features Revision 1.24 2007/07/09 08:00:29 olga GUI tuning
 * 
 * Revision 1.23 2007/06/13 08:33:07 olga Update: V161
 * 
 * Revision 1.22 2007/04/19 07:52:45 olga Tuning of: Undo/Redo, Graph layouter,
 * loading grammars
 * 
 * Revision 1.21 2007/03/29 09:52:04 olga Bug fixed: CPA graph - update and show
 * popup menu
 * 
 * Revision 1.20 2007/03/28 10:01:12 olga - extensive changes of Node/Edge Type
 * Editor, - first Undo implementation for graphs and Node/edge Type editing and
 * transformation, - new / reimplemented options for layered transformation, for
 * graph layouter - enable / disable for NACs, attr conditions, formula - GUI
 * tuning
 * 
 * Revision 1.19 2007/02/05 12:33:44 olga CPA: chengeAttribute
 * conflict/dependency : attributes with constants bug fixed, but the critical
 * pairs computation has still a gap.
 * 
 * Revision 1.18 2007/01/11 10:21:18 olga Optimized Version 1.5.1beta , free for
 * tests
 * 
 * Revision 1.17 2006/12/13 13:33:04 enrico reimplemented code
 * 
 * Revision 1.16 2006/11/01 11:17:30 olga Optimized agg sources of CSP
 * algorithm, match usability, graph isomorphic copy, node/edge type
 * multiplicity check for injective rule and match
 * 
 * Revision 1.15 2006/08/02 09:00:57 olga Preliminary version 1.5.0 with -
 * multiple node type inheritance, - new implemented evolutionary graph layouter
 * for graph transformation sequences
 * 
 * Revision 1.14 2006/05/17 06:57:16 olga CPA graph: set conflict/dependency
 * edge style
 * 
 * Revision 1.13 2006/05/08 08:24:12 olga Some extentions of GUI: - Undo Delete
 * button of tool bar to undo deletions if grammar elements like rule, NAC,
 * graph constraints; - the possibility to add a new graph to a grammar or a
 * copy of the current host graph; - to set one or more layer for consistency
 * constraints. Also some bugs fixed of matching and some optimizations of CSP
 * algorithmus done.
 * 
 * Revision 1.12 2006/03/13 10:04:42 olga CPA tuning
 * 
 * Revision 1.11 2006/03/06 09:15:36 olga Type sorting inconsistency of unnamed
 * typs eliminated
 * 
 * Revision 1.10 2006/03/02 12:03:23 olga CPA: check host graph - done
 * 
 * Revision 1.9 2006/03/01 09:55:47 olga - new CPA algorithm, new CPA GUI
 * 
 * Revision 1.8 2005/12/21 14:48:46 olga GUI tuning
 * 
 * Revision 1.7 2005/10/24 13:37:32 olga Pop-up menu of CPA graph
 * 
 * Revision 1.6 2005/10/13 10:27:39 olga CPA GUI , CPA Graph
 * 
 * Revision 1.5 2005/10/12 10:00:56 olga CPA GUI tuning
 * 
 * Revision 1.4 2005/10/10 08:05:16 olga Critical Pair GUI and CPA graph
 * 
 * Revision 1.3 2005/09/26 08:35:15 olga CPA graph frames; bugs
 * 
 * Revision 1.2 2005/09/19 09:12:14 olga CPA GUI tuning
 * 
 * Revision 1.1 2005/08/25 11:56:55 enrico *** empty log message ***
 * 
 * Revision 1.3 2005/07/11 09:30:20 olga This is test version AGG V1.2.8alfa .
 * What is new: - saving rule option <disabled> - setting trigger rule for layer -
 * display attr. conditions in gragra tree view - CPA algorithm <dependencies> -
 * creating and display CPA graph with conflicts and/or dependencies based on
 * (.cpx) file
 * 
 * Revision 1.2 2005/06/20 13:37:04 olga Up to now the version 1.2.8 will be
 * prepared.
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.18 2005/01/28 14:02:32 olga -Fehlerbehandlung beim Typgraph check
 * -Erweiterung CP GUI / CP Menu -Fehlerbehandlung mit identification option
 * -Fehlerbehandlung bei Rule PAC
 * 
 * Revision 1.17 2004/11/15 11:24:45 olga Neue Optionen fuer Transformation;
 * verbesserter default Graphlayout; Close GraGra mit Abfrage wenn was geaendert
 * wurde statt Delete GraGra
 * 
 * Revision 1.16 2004/06/14 12:34:19 olga CP Analyse and Transformation
 * 
 * Revision 1.15 2004/04/19 11:39:30 olga Graphname als String ohne Blanks
 * 
 * Revision 1.14 2003/03/05 18:24:09 komm sorted/optimized import statements
 * 
 * Revision 1.13 2003/03/03 17:46:59 olga GUI
 * 
 * Revision 1.12 2003/02/13 17:08:09 olga GUI Anpassung
 * 
 * Revision 1.11 2003/02/03 17:49:11 olga GUI
 * 
 * Revision 1.10 2003/01/22 16:19:31 olga CP-Tabelle verbessert
 * 
 * Revision 1.9 2003/01/20 12:10:55 olga CriticalPairPanel anpassung
 * 
 * Revision 1.8 2003/01/20 10:46:59 komm CriticalPairPanel integrated
 * 
 * Revision 1.7 2003/01/15 16:30:20 olga Critical pairs table eingebaut (test)
 * 
 * Revision 1.6 2003/01/13 14:28:30 komm no change
 * 
 * Revision 1.5 2002/12/09 17:53:26 olga GUI - Verbesserung
 * 
 * Revision 1.4 2002/11/25 15:06:05 olga Verbesserte Ueberlappungsgraph Anzeige
 * 
 * Revision 1.3 2002/11/07 16:03:05 olga Anzeige von Overlap-Graphen in CPA
 * verbessert
 * 
 * Revision 1.2 2002/10/02 18:30:55 olga XXX
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:19 olga Imported sources
 * 
 * Revision 1.3 2001/04/11 14:57:00 olga Arbeit an der GUI.
 * 
 * Revision 1.2 2001/03/08 11:02:43 olga Parser Anbindung gemacht. Stand nach
 * AGG GUI Reimplementierung. Stand nach der AGG GUI Reimplementierung.Das ist
 * Stand nach der AGG GUI Reimplementierung und Parser Anbindung.
 * 
 * Revision 1.1.2.13 2001/01/28 13:14:44 shultzke API fertig
 * 
 * Revision 1.1.2.12 2001/01/03 09:44:54 shultzke TODO's bis auf laden und
 * speichern erledigt. Wann meldet sich endlich Michael?
 * 
 * Revision 1.1.2.11 2001/01/02 12:28:57 shultzke Alle Optionen angebunden
 * 
 * Revision 1.1.2.10 2000/09/25 13:51:56 shultzke Report.trace veraendert
 * 
 * Revision 1.1.2.9 2000/08/10 12:22:12 shultzke Ausserdem wird nicht mehr eine
 * neues GUIObject erzeugt, wenn zur ParserGUI umgeschaltet wird. Einige Klassen
 * wurden umbenannt. Alle Events sind in ein eigenes Eventpackage geflogen.
 * 
 * Revision 1.1.2.8 2000/08/07 10:38:52 shultzke Option erweitert
 * 
 * Revision 1.1.2.7 2000/07/24 07:47:23 shultzke not critic-Fenster eingebaut
 * 
 * Revision 1.1.2.6 2000/07/19 13:59:59 shultzke *** empty log message ***
 * 
 * Revision 1.1.2.5 2000/07/16 18:52:24 shultzke *** empty log message ***
 * 
 * Revision 1.1.2.4 2000/07/12 14:33:44 shultzke Morphismen koennen jetzt besser
 * gemalt werden
 * 
 * Revision 1.1.2.3 2000/07/10 15:08:09 shultzke additional representtion
 * hinzugefuegt
 * 
 * Revision 1.1.2.2 2000/07/09 17:12:35 shultzke grob die GUI eingebunden
 * 
 */
