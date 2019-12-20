/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.browser.impl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import agg.editor.impl.EdGraGra;
import agg.gui.browser.GraphBrowser;
import agg.xt_basis.Arc;
import agg.xt_basis.Graph;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.TypeException;

/**
 * @author $Author: olga $
 * @version $Id: TestBrowser.java,v 1.8 2010/09/23 08:18:19 olga Exp $
 */
@SuppressWarnings("serial")
public class TestBrowser extends JPanel {

	static int ITS_WIDTH = 500;

	static int ITS_HEIGHT = 300;

	JFrame f;

	GraphBrowser browser;

	EdGraGra gragra;

	public TestBrowser(JFrame frame) {
		super(true);
		setLayout(new BorderLayout());

		this.f = frame;

		// create menubar
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		menuBar.add(createLayoutMenu());
		menuBar.add(createBasisMenu());
		add(menuBar, BorderLayout.NORTH);

		// create graph browser
		this.browser = new GraphBrowserImpl();
		add(this.browser.getPanel(), BorderLayout.CENTER);
		// oder so
		// add((GraphBrowserImpl) browser, BorderLayout.CENTER);

	}

	JMenu createFileMenu() {
		JMenu file = new JMenu("File", true);
		JMenuItem mi = file.add(new JMenuItem("Load GraGra"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TestBrowser.this.gragra = TestBrowser.this.browser.loadGraGra(TestBrowser.this.f);
				if (TestBrowser.this.gragra != null) {
					TestBrowser.this.browser.setGraph(TestBrowser.this.gragra.getGraph());
					TestBrowser.this.browser.showGraph();
				}
			}
		});
		/*
		 * mi = (JMenuItem) file.add(new JMenuItem("Load Base GraGra"));
		 * mi.addActionListener(new ActionListener() {public void
		 * actionPerformed(ActionEvent e) { GraGra base =
		 * browser.loadBaseGraGra(f); if (base != null) { gragra = new
		 * EdGraGra(base); browser.setGraph(gragra.getGraph());
		 * browser.showGraph(); } }});
		 */
		mi = file.add(new JMenuItem("Save As"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TestBrowser.this.browser.saveAs(TestBrowser.this.f);
			}
		});

		file.add(new JSeparator());

		mi = file.add(new JMenuItem("Show Graph"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TestBrowser.this.browser.setGraph(TestBrowser.this.gragra.getGraph());
				TestBrowser.this.browser.showGraph();
			}
		});

		file.add(new JSeparator());

		mi = file.add(new JMenuItem("Update"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TestBrowser.this.browser.updateGraphics();
			}
		});

		file.add(new JSeparator());

		mi = file.add(new JMenuItem("Exit"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		return file;
	}

	JMenu createLayoutMenu() {
		JMenu layout = new JMenu("Layout", true);
		JMenuItem mi = layout.add(new JMenuItem("Default"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				defaultLayout();
			}
		});
		return layout;
	}

	void defaultLayout() {
		this.browser.setGraph(this.gragra.getBasisGraGra().getGraph());
		this.browser.showGraph();
	}

	JMenu createBasisMenu() {
		JMenu basis = new JMenu("Basis", true);
		JMenuItem mi = basis.add(new JMenuItem("Modify Graph"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				modifyBaseGraGra();
			}
		});

		mi = basis.add(new JMenuItem("Show graph"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showLG();
			}
		});

		mi = basis.add(new JMenuItem("Show left rule side"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showLRS();
			}
		});

		mi = basis.add(new JMenuItem("Show right rule side"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showRRS();
			}
		});

		mi = basis.add(new JMenuItem("Show NAC"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showNAC();
			}
		});

		return basis;
	}

	void modifyBaseGraGra() {
		JOptionPane.showMessageDialog(this.f, "All arcs will be removed.");
		Graph graph = this.gragra.getBasisGraGra().getGraph();

		Iterator<Arc> arcs = graph.getArcsSet().iterator();
		while (arcs.hasNext()) {
			try {
				graph.destroyArc(arcs.next(), true, false);
			} catch (TypeException e) {
				e.printStackTrace();
			}
		}
		this.browser.setGraph(graph);
		this.browser.showGraph();
	}

	void showLG() {
		this.browser.setGraph(this.gragra.getBasisGraGra().getGraph());
		this.browser.showGraph();
	}

	void showLRS() {
		List<Rule> rules = this.gragra.getBasisGraGra().getListOfRules();
		if (!rules.isEmpty()) {
			Rule r = rules.get(0);
			this.browser.setGraph(r.getLeft());
			this.browser.showGraph();
		}
	}

	void showRRS() {
		List<Rule> rules = this.gragra.getBasisGraGra().getListOfRules();
		if (!rules.isEmpty()) {
			Rule r = rules.get(0);
			this.browser.setGraph(r.getRight());
			this.browser.showGraph();
		}
	}

	void showNAC() {
		List<Rule> rules = this.gragra.getBasisGraGra().getListOfRules();
		if (!rules.isEmpty()) {
			Rule r = rules.get(0);
			Enumeration<OrdinaryMorphism> nacs = r.getNACs();
			if (nacs.hasMoreElements()) {
				OrdinaryMorphism nac = nacs.nextElement();
				this.browser.setGraph(nac.getImage());
				this.browser.showGraph();
			}
		}
	}

	public static void main(String[] args) {

		WindowListener l = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		};

		JFrame frame = new JFrame("AGG Graph Browser (extended)");
		frame.addWindowListener(l);
		frame.setBackground(Color.white);
		frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(screenSize.width / 2 - ITS_WIDTH / 2, screenSize.height
				/ 2 - ITS_HEIGHT / 2);
		frame.setSize(ITS_WIDTH, ITS_HEIGHT);

		TestBrowser testbrowser = new TestBrowser(frame);

		frame.getContentPane().add(testbrowser, BorderLayout.CENTER);

		frame.setVisible(true);
	}
}
// $Log: TestBrowser.java,v $
// Revision 1.8  2010/09/23 08:18:19  olga
// tuning
//
// Revision 1.7  2010/03/04 14:09:16  olga
// code optimizing
//
// Revision 1.6  2010/02/22 15:03:34  olga
// code optimizing
//
// Revision 1.5  2008/04/07 09:36:56  olga
// Code tuning: refactoring + profiling
// Extension: CPA - two new options added
//
// Revision 1.4  2007/11/01 09:58:19  olga
// Code refactoring: generic types- done
//
// Revision 1.3  2007/09/24 09:42:39  olga
// AGG transformation engine tuning
//
// Revision 1.2  2007/09/10 13:05:49  olga
// In this update:
// - package xerces2.5.0 is not used anymore;
// - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
// - bugs fixed in:  usage of PACs in rules;  match completion;
// 	usage of static method calls in attr. conditions
// - graph editing: added some new features
//
// Revision 1.1 2005/08/25 11:57:00 enrico
// *** empty log message ***
//
// Revision 1.2 2005/06/20 13:37:04 olga
// Up to now the version 1.2.8 will be prepared.
//
// Revision 1.1 2005/05/30 12:58:04 olga
// Version with Eclipse
//
// Revision 1.3 2003/03/05 18:24:28 komm
// sorted/optimized import statements
//
// Revision 1.2 2003/02/24 11:20:27 komm
// appereance changed
//
// Revision 1.1.1.1 2002/07/11 12:17:16 olga
// Imported sources
//
// Revision 1.4 1999/09/09 10:25:21 mich
// Update Shared Source Working Environment
//
// Revision 1.3 1999/08/17 10:51:29 shultzke
// neues Package hinzugefuegt
//
