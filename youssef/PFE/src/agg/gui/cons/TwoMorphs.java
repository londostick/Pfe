/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.cons;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import agg.cons.AtomApplCond;
import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdRule;
import agg.editor.impl.EdGraph;
import agg.editor.impl.EdAtomic;
import agg.editor.impl.EdNode;
import agg.gui.editor.GraphCanvas;
import agg.gui.editor.GraphPanel;
import agg.gui.saveload.GraphicsExportJPEG;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.OrdinaryMorphism;


@SuppressWarnings("serial")
public class TwoMorphs extends JPanel {

	private GraphPanel graphs[];

	private JLabel titles[];

	private JPanel panels[];

	private EdGraGra layout;

	private EdRule ruleLayout;

	private OrdinaryMorphism morph1, morph2;

	private JSplitPane split, main;

	final protected MouseListener ml;
	final protected JPopupMenu graphMenu = new JPopupMenu("Graph");
	final protected JMenuItem miLayoutGraph = new JMenuItem("Layout Graph");
	final protected JMenuItem miGraphExportJPG = new JMenuItem("Export JPEG");
	protected GraphPanel activeGraphPanel;
	protected GraphicsExportJPEG exportJPEG;
	
	public TwoMorphs() {
		super(new BorderLayout());

		this.graphs = new GraphPanel[3];
		this.titles = new JLabel[3];
		this.panels = new JPanel[3];

		for (int i = 0; i < 3; i++) {
			this.graphs[i] = new GraphPanel();
			this.titles[i] = new JLabel();
			this.panels[i] = new JPanel(new BorderLayout());
			this.panels[i].add(this.titles[i], BorderLayout.NORTH);
			this.panels[i].add(this.graphs[i], BorderLayout.CENTER);
			this.graphs[i].setPreferredSize(new Dimension(200, 250));
			this.graphs[i].setEditMode(agg.gui.editor.EditorConstants.VIEW);
		}
		this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.panels[0],
				this.panels[1]);

		this.split.setOneTouchExpandable(true);
		this.split.setContinuousLayout(true);
		this.main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.split, this.panels[2]);

		this.main.setOneTouchExpandable(true);
		this.main.setContinuousLayout(true);

		add(this.main, BorderLayout.CENTER);
		
		makeGraphMenu();
		
		this.ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					if (e.getSource() instanceof JPanel) {
						if (e.getSource() instanceof GraphCanvas) {
							TwoMorphs.this.activeGraphPanel = ((GraphCanvas) e.getSource())
									.getViewport();
							TwoMorphs.this.graphMenu.show(TwoMorphs.this.activeGraphPanel,
									e.getX()+5,
									e.getY()+5);
						}
					}
				}
			}
			
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					if (e.getSource() instanceof JPanel) {
						if (e.getSource() instanceof GraphCanvas) {
							TwoMorphs.this.activeGraphPanel = ((GraphCanvas) e.getSource())
									.getViewport();
							TwoMorphs.this.graphMenu.show(TwoMorphs.this.activeGraphPanel, 
									e.getX()+5, 
									e.getY()+5);
						}
					}
				}
			}
		};
		
		this.graphs[0].getCanvas().addMouseListener(this.ml);
		this.graphs[1].getCanvas().addMouseListener(this.ml);
		this.graphs[2].getCanvas().addMouseListener(this.ml);
	}

	private void makeGraphMenu() {
		this.graphMenu.add(this.miLayoutGraph);
		this.miLayoutGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (TwoMorphs.this.activeGraphPanel != null
						&& TwoMorphs.this.activeGraphPanel.getGraph() != null) {
					makeLayout(TwoMorphs.this.activeGraphPanel.getCanvas().getGraph());
					TwoMorphs.this.activeGraphPanel.updateGraphics();
				}
			}
		});
		
		this.graphMenu.add(this.miGraphExportJPG);
		this.miGraphExportJPG.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (TwoMorphs.this.activeGraphPanel != null
						&& TwoMorphs.this.activeGraphPanel.getGraph() != null) {	
					TwoMorphs.this.exportJPEG.save(TwoMorphs.this.activeGraphPanel.getCanvas());
				}
			}
		});
		
		this.graphMenu.pack();
		this.graphMenu.setBorderPainted(true);
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
	}
	
	protected void makeLayout(EdGraph g) {
		final List<EdNode> visiblenodes = g.getVisibleNodes();
		g.setCurrentLayoutToDefault(false);
		g.getDefaultGraphLayouter().setEnabled(true);
		Dimension dim = g.getDefaultGraphLayouter().getNeededPanelSize(visiblenodes);
		if (dim.width < 550)
			dim.width = 550;
		if (dim.height < 450)
			dim.height = 450;
		g.getDefaultGraphLayouter().setPanelSize(dim);
		g.getDefaultGraphLayouter().setEnabled(true);
		g.doDefaultEvolutionaryGraphLayout(g.getDefaultGraphLayouter(),
						100, 10);
	}
	
	public void setExportJPEG(GraphicsExportJPEG jpg) {
		this.exportJPEG = jpg;
	}
	
	/** Gets my preferred dimension */
	public Dimension getPreferredSize() {
		return new Dimension(600, 250);
	}

	public void setMorphisms(OrdinaryMorphism m1, OrdinaryMorphism m2) {
		if (m1 != null && m2 != null && !m1.getImage().equals(m2.getOriginal()))
			return;
		this.morph1 = m1;
		this.morph2 = m2;
		paint();
	}

	public void setAtomApplCond(AtomApplCond cond) {
		if (cond != null) {
			setMorphisms(cond.getPreCondition(), cond.getT());
		} else
			setMorphisms(null, null);
	}

	public void reset() {
	}

	public void setGraGra(EdGraGra gra) {
		this.layout = gra;
	}

	public void setRule(EdRule rule) {
		this.ruleLayout = rule;
	}

	public EdGraGra getGraGra() {
		return this.layout;
	}

	public EdRule getRule() {
		return this.ruleLayout;
	}

	private String getGraphName(int i) {
		switch (i) {
		case 0:
			return "R = right rule side";
		case 1:
			return "S = overlap R + premise";
		case 2:
			return "T = pushout";
		}
		return "";
	}

	private EdGraph setGraph(Graph g, int i) {
		GraphPanel gp = this.graphs[i];
		JLabel title = this.titles[i];
		if (g == null) {
			gp.setGraph(null);
			gp.updateGraphics();
			return null;
		} 
		if (this.layout == null) {
			return null;
		}
		EdGraph eg = new EdGraph(g, this.layout.getTypeSet());
		eg.setCurrentLayoutToDefault(false);
		eg.updateGraph();
		eg.setCurrentLayoutToDefault(true);
		gp.setGraph(eg);
		title.setText("  " + getGraphName(i));
		return eg;	
	}

	/*
	 * The base graph of EdGraph to has to be a graph (left or right graph of a
	 * rule) of the base gragra of EdGraGra from
	 */
	private void copyLayout(EdGraph from, EdGraph to) {
		if (from == null || to == null)
			return;
		for (int k = 0; k < to.getNodes().size(); k++) {
			EdNode n = to.getNodes().elementAt(k);
			for (int j = 0; j < from.getNodes().size(); j++) {
				EdNode en = from.getNodes().elementAt(j);
				if (en.getBasisNode().getContextUsage() ==
						n.getBasisNode().getContextUsage()) {
					n.setXY(en.getX(), en.getY());
					break;
				}
			}
		}
	}

	private void addMarks(HashMap<GraphObject, Integer> h, OrdinaryMorphism m) {		
		int i = 0;
		Iterator<?> iter = h.keySet().iterator();
		while (iter.hasNext()) {		
			int j = h.get(iter.next()).intValue();
			if (j > i)
				i = j;
		}
		
		Enumeration<GraphObject> graphObjects = m.getCodomain();
		graphObjects = m.getCodomain();
		while (graphObjects.hasMoreElements()) {
			GraphObject go = graphObjects.nextElement();
			Enumeration<GraphObject> inverse = m.getInverseImage(go);
			GraphObject inv = null;
			if (inverse.hasMoreElements())
				inv = inverse.nextElement();
			if (h.get(go) != null && inv != null) {
				/*
				 * Image already has a number. Apply it to all sources
				 * (eventually overwriting already set sources).
				 */
				Integer number = h.get(go);
				h.put(inv, number);
				while (inverse.hasMoreElements()) {
					inv = inverse.nextElement();
					h.put(inv, number);
				}
			} else if (h.get(go) == null) {
				Integer number = null;
				inverse = m.getInverseImage(go);
				while (inverse.hasMoreElements()) {
					inv = inverse.nextElement();
					number = h.get(inv);
					if (number != null)
						break;
				}
				if (number == null)
					number = Integer.valueOf(++i);
				h.put(go, number);
				inverse = m.getInverseImage(go);
				while (inverse.hasMoreElements()) {
					inv = inverse.nextElement();
					if (h.get(inv) == null)
						h.put(inv, number);
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private HashMap convertToStringHashMap(HashMap<GraphObject, Integer> h) {
		HashMap<GraphObject, String> result = new HashMap<GraphObject, String>();
		Iterator<GraphObject> iter = h.keySet().iterator();
		while (iter.hasNext()) {
			GraphObject key = iter.next();
			result.put(key, h.get(key).toString());
		}
		return result;
	}

	private void paint() {
		if (this.morph1 == null || this.morph2 == null) {
			setGraph(null, 0);
			setGraph(null, 1);
			setGraph(null, 2);
			return;
		}
		if (this.split.getDividerLocation() == 0)
			this.split.setDividerLocation(this.getSize().width / 2 - 20);
		if (this.main.getDividerLocation() == 0)
			this.main.setDividerLocation(this.getSize().width / 3 - 20);

		EdGraph g0 = setGraph(this.morph1.getOriginal(), 0);
		copyLayout(this.ruleLayout.getRight(), g0);

		EdGraph g1 = setGraph(this.morph1.getImage(), 1);
		copyLayout(this.ruleLayout.getRight(), g1);

		EdGraph g2 = setGraph(this.morph2.getImage(), 2);

		for (int i = 0; i < this.layout.getAtomics().size(); i++) {
			EdAtomic atom = this.layout.getAtomics().get(i);
			EdAtomic atomConcl = atom.getConclusions().get(0);
			EdGraph premise = atomConcl.getLeft();
			EdGraph concl = atomConcl.getLeft();
			copyLayout(premise, g1);
			copyLayout(concl, g2);
		}
		copyLayout(g1, g2);

		HashMap<GraphObject, Integer> h = new HashMap<GraphObject, Integer>();
		addMarks(h, this.morph1);
		addMarks(h, this.morph2);
		@SuppressWarnings("rawtypes")
		HashMap stringmap = convertToStringHashMap(h);
		this.graphs[0].getGraph().setMorphismMarks(stringmap, true);
		this.graphs[1].getGraph().setMorphismMarks(stringmap, true);
		this.graphs[2].getGraph().setMorphismMarks(stringmap, true);
		this.graphs[0].updateGraphics();
		this.graphs[1].updateGraphics();
		this.graphs[2].updateGraphics();
	}

}
