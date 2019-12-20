/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.ImageIcon;
//import javax.swing.border.TitledBorder;

import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdGraph;
import agg.gui.popupmenu.EditPopupMenu;
import agg.gui.popupmenu.EditSelPopupMenu;
import agg.gui.popupmenu.ModePopupMenu;
import agg.gui.saveload.GraphicsExportJPEG;

/**
 * 
 * @author $Author: olga $
 * @version $Id: GraphEditor.java,v 1.14 2010/11/04 10:58:08 olga Exp $
 */
@SuppressWarnings("serial")
public class GraphEditor extends JPanel {

	private final GraphEditorMouseAdapter mouseAdapter;
	
	/** Creates a graph editor. */
	public GraphEditor() {
		this(null);
	}
	
	/**
	 * Creates a graph editor. The parent editor is specified by the
	 * GraGraEditorImpl anEditor or NULL.
	 */
	public GraphEditor(GraGraEditor anEditor) {
		super(new BorderLayout());
		this.setBackground(Color.white);
		this.setForeground(Color.WHITE);
		
		this.mouseAdapter = new GraphEditorMouseAdapter(this);
		
		this.graphName = "";
		this.gragraName = "";

		this.graphPanel = new GraphPanel(this);
		this.graphPanel.getCanvas().addMouseListener(this.mouseAdapter);

		this.title = new JLabel(this.titleKind);
		this.buttonPanel = new JPanel();
		this.exportJPEGButton = createExportJPEGButton();
		if (this.exportJPEGButton != null) 
			this.buttonPanel.add(this.exportJPEGButton);
		this.titlePanel = new JPanel(new BorderLayout());
		this.titlePanel.add(this.title, BorderLayout.WEST);
		this.titlePanel.add(this.buttonPanel, BorderLayout.EAST);

		add(this.titlePanel, BorderLayout.NORTH);
		add(this.graphPanel, BorderLayout.CENTER);

		this.gragraEditor = anEditor;
		if (this.gragraEditor != null)
			this.applFrame = anEditor.getParentFrame();
		else
			this.applFrame = null;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#getApplFrame()
	 */
	public JFrame getApplFrame() {
		return this.applFrame;
	}
	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setCursorOfApplFrame(java.awt.Cursor)
	 */
	public void setCursorOfApplFrame(Cursor cursor) {
		if (this.applFrame != null)
			this.applFrame.setCursor(cursor);
	}
	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#addButton(javax.swing.JButton)
	 */
	public void addButton(JButton btn) {
		this.buttonPanel.removeAll();
		if (btn != null)
			this.buttonPanel.add(btn);
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#removeTitlePanel()
	 */
	public void removeTitlePanel() {
		this.remove(this.titlePanel);
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#allowToShowPopupMenu(boolean)
	 */
	public void allowToShowPopupMenu(boolean b) {
		this.doNotShowPopupMenu = !b;
	}
	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#isPopupMenuAllowed()
	 */
	public boolean isPopupMenuAllowed() {
		return !this.doNotShowPopupMenu;
	}
	
	private JButton createExportJPEGButton() {
		java.net.URL url = ClassLoader.getSystemClassLoader()
								.getResource("agg/lib/icons/print.gif");
		if (url != null) {
			ImageIcon image = new ImageIcon(ClassLoader
					.getSystemResource("agg/lib/icons/print.gif"));
			// System.out.println(image);
			JButton b = new JButton(image);
			b.setToolTipText("Export Graph JPEG");
			b.setMargin(new Insets(-5, 0, -5, 0));
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (GraphEditor.this.exportJPEG != null)
						if (!GraphEditor.this.exportJPEG.save(GraphEditor.this.graphPanel.getCanvas())) {
							JOptionPane.showMessageDialog(GraphEditor.this.applFrame, 
									"Cannot export to JPEG."
									+"\nThere are problems with the Class "
									+"\n    com.sun.image.codec.jpeg.JPEGImageEncoder "
									+"\nand the currently used JAVA 1.6 version.",
									"Expost failed",
									JOptionPane.ERROR_MESSAGE);
						}
				}
			});
			b.setEnabled(false);
			return b;
		}
		return null;
	}

	public Dimension getMinimumSize() {
		return new Dimension(100, 100);
	}

	public Dimension getPreferredSize() {
		return new Dimension(this.prefW, this.prefH);
	}

	public void setPreferredSize(Dimension preferredSize) {
		super.setPreferredSize(preferredSize);
		this.prefW = preferredSize.width;
		this.prefH = preferredSize.height;
	}
	
	public void setTitle(String str) {
		this.graphName = "";
		this.gragraName = "";
		this.title.setText(" "+str);
	}

	public void setTitle(String str1, String str2) {
		if (!str1.equals("") && !str2.equals("")) {
			this.graphName = str1;
			this.gragraName = str2;
			this.title.setText(this.titleKind + this.graphName + "  of  " + this.gragraName);
		} else if (!str1.equals("") && str2.equals("")) {
			this.graphName = str1;
			if (!this.gragraName.equals(""))
				this.title.setText(this.titleKind + this.graphName + "  of  " + this.gragraName);
			else
				this.title.setText(this.titleKind + this.graphName);
		} else if (str1.equals("") && !str2.equals("")) {
			this.gragraName = str2;
			if (!this.graphName.equals(""))
				this.title.setText(this.titleKind + this.graphName + "  of  " + this.gragraName);
			else
				this.title.setText(this.titleKind + this.gragraName);
		} else if (str1.equals("") && str2.equals("")) {
			this.graphName = str1;
			this.gragraName = str2;
			this.title.setText(this.titleKind);
		}
	}


	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#getGraphPanel()
	 */
	public GraphPanel getGraphPanel() {
		return this.graphPanel;
	}

	public GraphPanel getPanelOfLocationOnScreen(Point p) {
		if (p == null)
			return null;
		Point p2 = new Point(20, 20);
		p2.x = p.x - this.graphPanel.getLocationOnScreen().x;
		p2.y = p.y - this.graphPanel.getLocationOnScreen().y;
		if (this.graphPanel.contains(p2))
			return this.graphPanel;
		return null;
	}
	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#getGraph()
	 */
	public EdGraph getGraph() {
		return this.eGraph;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#getGraGra()
	 */
	public EdGraGra getGraGra() {
		return this.eGra;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#getGraGraEditor()
	 */
	public GraGraEditor getGraGraEditor() {
		return this.gragraEditor;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#getMsg()
	 */
	public String getMsg() {
		return this.msg;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setMsg(java.lang.String)
	 */
	public void setMsg(String str) {
		this.msg = str;
	}
	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#hasGraph()
	 */
	public boolean hasGraph() {
		if (this.eGraph != null)
			return true;
		
		return false;
	}

	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setGraph(agg.editor.impl.EdGraph)
	 */
	public void setGraph(EdGraph eg) {
		this.eGraph = eg;
		
		this.titleKind = " ";
		if (this.eGraph == null) {
			setTitle("    ");
			this.graphPanel.setGraph(null);
			if (this.exportJPEGButton != null)
				this.exportJPEGButton.setEnabled(false);
			this.eGra = null;
			this.isEmpty = true;
			return;
		} 
//		else if (eg.isTypeGraph()) {
//			this.titleKind = "[ TG ]  ";
//		}
//		else {
//			this.titleKind = "[ Graph ]  ";
//		}
		
		this.isEmpty = false;
		
		this.eGra = this.eGraph.getGraGra();

		if (this.eGra != null)
			setTitle(this.eGraph.getBasisGraph().getName(), this.eGra.getName());
		else
			setTitle(this.eGraph.getBasisGraph().getName(), "");

		
		if (!this.eGraph.isTypeGraph()) {
			this.graphPanel.getCanvas().setAttributeVisible(this.graphAttrsVisible);
			this.eGraph.setStaticNodePosition(
							this.staticNodePositionForGraphLayouter);
		} else {
			this.graphPanel.getCanvas().setAttributeVisible(this.typeGraphAttrsVisible);
		}
		
		this.graphPanel.setGraph(this.eGraph, true);

		this.eGraph.setStaticNodePosition(this.staticNodePositionForGraphLayouter);
		
		this.straightenArcs = this.eGraph.isStraightenArcsEnabled();
//		this.eGraph.setStraightenArcs(this.straightenArcs);
		
		if (this.exportJPEG != null 
				&& this.exportJPEGButton != null) {
			this.exportJPEGButton.setEnabled(true);
		}
	}

	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setGraphAttributeVisible(boolean)
	 */
	public void setGraphAttributeVisible(boolean vis){
		this.graphAttrsVisible = vis;
		if (this.getGraph() == null
				|| !this.getGraph().isTypeGraph()) {
			this.graphPanel.getCanvas().setAttributeVisible(this.graphAttrsVisible);
			this.graphPanel.updateGraphics();
		}
	}
	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setTypeGraphAttributeVisible(boolean)
	 */
	public void setTypeGraphAttributeVisible(boolean vis){
		this.typeGraphAttrsVisible = vis;
		if (this.getGraph() != null
				&& this.getGraph().isTypeGraph()) {
			this.graphPanel.getCanvas().setAttributeVisible(this.typeGraphAttrsVisible);
			this.graphPanel.updateGraphics();
		}
	}
	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setExportJPEG(agg.gui.GraphicsExportJPEG)
	 */
	public void setExportJPEG(GraphicsExportJPEG jpg) {
		this.exportJPEG = jpg;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#clear()
	 */
	public void clear() {
		this.setGraph(null);
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#isEmpty()
	 */
	public boolean isEmpty() {
		return this.isEmpty;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#hasOneSelection()
	 */
	public boolean hasOneSelection() {
		if (this.eGraph == null)
			return false;
		return this.graphPanel.getGraph().hasOneSelection();
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#hasSelection()
	 */
	public boolean hasSelection() {
		return (this.eGraph != null && this.eGraph.hasSelection())? true: false;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#updateGraphics()
	 */
	public void updateGraphics() {
		this.graphPanel.updateGraphics();
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#updateGraphics(boolean)
	 */
	public void updateGraphics(boolean graphDimensionCheck) {
		this.graphPanel.updateGraphics(graphDimensionCheck);
	}


	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setAttrEditor(agg.attribute.gui.AttrTopEditor)
	 */
//	public void setAttrEditor(AttrTopEditor attrEditor) {
//		this.attrEditor = attrEditor;
//	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setModePopupMenu(agg.gui.ModePopupMenu)
	 */
	public void setModePopupMenu(ModePopupMenu pm) {
		this.modePopupMenu = pm;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setEditPopupMenu(agg.gui.EditPopupMenu)
	 */
	public void setEditPopupMenu(EditPopupMenu pm) {
		this.editPopupMenu = pm;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setEditSelPopupMenu(agg.gui.EditSelPopupMenu)
	 */
	public void setEditSelPopupMenu(EditSelPopupMenu pm) {
		this.editSelPopupMenu = pm;
	}

	/* Mode menu procedures */

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setEditMode(int)
	 */
	public void setEditMode(int mode) {
		switch (mode) {
		case EditorConstants.DRAW:
			drawModeProc();
			break;
		case EditorConstants.SELECT:
			selectModeProc();
			break;
		case EditorConstants.MOVE:
			moveModeProc();
			break;
		case EditorConstants.ATTRIBUTES:
			attributesModeProc();
			break;
//		case EditorConstants.INTERACT_RULE:
//			ruleDefModeProc();
//			break;
//		case EditorConstants.INTERACT_NAC:
//			nacDefModeProc();
//			break;
//		case EditorConstants.INTERACT_PAC:
//			pacDefModeProc();
//			break;	
//		case EditorConstants.INTERACT_AC:
//			acDefModeProc();
//			break;
		case EditorConstants.INTERACT_MATCH:
			matchDefModeProc();
			break;
		case EditorConstants.COPY:
			duplicateModeProc();
			break;
		case EditorConstants.PASTE:
			pasteModeProc();
			break;
		case EditorConstants.MAP:
			mapModeProc();
			break;
		case EditorConstants.UNMAP:
			unmapModeProc();
			break;
		case EditorConstants.SET_PARENT:
			setParentModeProc();
			break;
		case EditorConstants.UNSET_PARENT:
			unsetParentModeProc();
			break;
		case EditorConstants.MAPSEL:
			mapselModeProc();
			break;
		case EditorConstants.UNMAPSEL:
			unmapselModeProc();
			break;
		case agg.gui.editor.EditorConstants.VIEW:
			viewModeProc();
			break;
		default:
			break;
		}
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#getMode()
	 */
	public int getMode() {
		return this.graphPanel.getEditMode();
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setEditCursor(java.awt.Cursor)
	 */
	public void setEditCursor(Cursor cur) {
		this.graphPanel.setEditCursor(cur);
	}
	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#drawModeProc()
	 */
	public void drawModeProc() {
		// if (this.graphPanel.getEditMode() == EditorConstants.DRAW) return;
		this.graphPanel.setEditMode(EditorConstants.DRAW);
		this.graphPanel.setEditCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		this.msg = "Click on the background to get a node / on a source node and a target node to get an edge.";
	}

	private void selectModeProc() {
		if (this.graphPanel.getEditMode() == EditorConstants.SELECT)
			return;
		this.graphPanel.setEditMode(EditorConstants.SELECT);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		this.msg = "Click on an object to select it.";
	}

	private void moveModeProc() {
		if (this.graphPanel.getEditMode() == EditorConstants.MOVE)
			return;
		this.graphPanel.setEditMode(EditorConstants.MOVE);
		this.graphPanel.setEditCursor(new Cursor(Cursor.MOVE_CURSOR));
		this.msg = "Press and drag the button when the cursor points to an object.";
	}

	private void attributesModeProc() {
		if (this.graphPanel.getEditMode() == EditorConstants.ATTRIBUTES)
			return;
		this.graphPanel.setEditMode(EditorConstants.ATTRIBUTES);
		this.graphPanel.setEditCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		this.msg = "Click on an object to activate the attribute editor.";
	}

	/*
	private void ruleDefModeProc() {
		if (this.graphPanel.getEditMode() == EditorConstants.INTERACT_RULE)
			return;
		this.graphPanel.setEditMode(EditorConstants.INTERACT_RULE);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	private void nacDefModeProc() {
		if (this.graphPanel.getEditMode() == EditorConstants.INTERACT_NAC)
			return;
		this.graphPanel.setEditMode(EditorConstants.INTERACT_NAC);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	private void pacDefModeProc() {
		if (this.graphPanel.getEditMode() == EditorConstants.INTERACT_PAC)
			return;
		this.graphPanel.setEditMode(EditorConstants.INTERACT_PAC);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
	}
	
	private void acDefModeProc() {
		if (this.graphPanel.getEditMode() == EditorConstants.INTERACT_AC)
			return;
		this.graphPanel.setEditMode(EditorConstants.INTERACT_AC);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
	}
*/
	
	private void matchDefModeProc() {
//		if (this.graphPanel.getEditMode() == EditorConstants.INTERACT_MATCH)
//			return;
		this.graphPanel.setEditMode(EditorConstants.INTERACT_MATCH);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
	}
	
	private void mapModeProc() {
		this.graphPanel.setEditMode(EditorConstants.MAP);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	private void unmapModeProc() {
		this.graphPanel.setEditMode(EditorConstants.UNMAP);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	private void setParentModeProc() {
		this.graphPanel.setEditMode(EditorConstants.SET_PARENT);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	private void unsetParentModeProc() {
		this.graphPanel.setEditMode(EditorConstants.UNSET_PARENT);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	private void mapselModeProc() {
		if (this.graphPanel.getEditMode() == EditorConstants.MAPSEL)
			return;
		this.graphPanel.setEditMode(EditorConstants.MAPSEL);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	private void unmapselModeProc() {
		if (this.graphPanel.getEditMode() == EditorConstants.UNMAPSEL)
			return;
		this.graphPanel.setEditMode(EditorConstants.UNMAPSEL);
		this.graphPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	private void duplicateModeProc() {
		if (this.eGraph == null)
			return;
		this.graphPanel.setLastEditMode(this.graphPanel.getEditMode());
		this.graphPanel.setLastEditCursor(this.graphPanel.getEditCursor());
		this.graphPanel.setEditMode(EditorConstants.COPY);
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		this.msg = "To place a copy click on the background of the panel.";
	}

	private void pasteModeProc() {
		if (this.eGraph == null)
			return;
		this.graphPanel.setEditMode(EditorConstants.PASTE);
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		this.msg = "To place a copy click on the background of the panel.";
	}
	
	private void viewModeProc() {
		this.graphPanel.setEditMode(EditorConstants.VIEW);
	}


	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#deleteProc()
	 */
	public boolean deleteProc() {
		if (this.eGraph == null || this.graphPanel.getEditMode() == EditorConstants.VIEW)
			return false;

		if (hasSelection()) {
			this.graphPanel.deleteSelected();
			this.graphPanel.updateGraphicsAfterDelete();
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#copyProc()
	 */
	public void copyProc() {
		if (this.eGraph == null || this.graphPanel.getEditMode() == EditorConstants.VIEW)
			return;
		if (!hasSelection()) {
			this.msg = "Copy -> There isn't any object selected.";
			return;
		} 
		this.graphPanel.setLastEditMode(this.graphPanel.getEditMode());
		this.msg = "";
		setEditMode(EditorConstants.COPY);	
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#selectAllProc()
	 */
	public void selectAllProc() {
		if (this.eGraph == null)
			return;
		this.graphPanel.selectAll();
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#selectNodeTypeProc()
	 */
	public void selectNodeTypeProc() {
		if (this.eGraph == null)
			return;
		this.graphPanel.selectNodesOfSelectedNodeType();
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#selectArcTypeProc()
	 */
	public void selectArcTypeProc() {
		if (this.eGraph == null)
			return;
		this.graphPanel.selectArcsOfSelectedArcType();
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#deselectAllProc()
	 */
	public void deselectAllProc() {
		if (this.eGraph == null)
			return;
		this.graphPanel.deselectAll();
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setStraightenArcs(boolean)
	 */
	public void setStraightenArcs(boolean b) {
		this.straightenArcs = b;
		if (this.eGraph != null && !this.eGraph.isTypeGraph()) {
			if (b && this.eGraph.isStraightenArcsEnabled()) {
				// reset to FALSE with aim to straighten manually bounded arcs
				// if b is true
				this.eGraph.setStraightenArcs(false);
			}
			this.eGraph.setStraightenArcs(this.straightenArcs);
		}
	}
	
	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#straightenArcsProc()
	 */
	public void straightenArcsProc() {
		if (this.eGraph != null)
			this.graphPanel.straightenSelectedArcs();
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setGraphToCopy(agg.editor.impl.EdGraph)
	 */
	public void setGraphToCopy(EdGraph g) {
		if (this.eGraph != null)
			this.eGraph.setGraphToCopy(g);
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#getSelectedAsGraphCopy()
	 */
	public EdGraph getSelectedAsGraph() {
		if (this.eGraph != null) {
			this.sourceOfCopy = this.eGraph;
			return this.eGraph.getSelectedAsGraph();
		} 
		return null;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#setSourceOfCopy(agg.editor.impl.EdGraph)
	 */
	public void setSourceOfCopy(EdGraph g) {
		this.sourceOfCopy = g;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#getSourceOfCopy()
	 */
	public EdGraph getSourceOfCopy() {
		return this.sourceOfCopy;
	}

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#enableStaticNodePositionForGraphLayouter(boolean)
	 */
	public void enableStaticNodePositionForGraphLayouter(boolean enable) {
		this.staticNodePositionForGraphLayouter = enable;
		if (this.eGraph != null && !this.eGraph.isTypeGraph())
			this.eGraph.setStaticNodePosition(enable);
	}

	// private

	/* (non-Javadoc)
	 * @see agg.gui.impl.GraphEditor#showPopupMenu(java.awt.event.MouseEvent)
	 */
	public void showPopupMenu(MouseEvent e) {
		if (e.getSource() == this.graphPanel.getCanvas()
				&& (e.getX() > 0 && e.getY() > 0)) {
			
			if (this.editPopupMenu != null) {
				this.editPopupMenu.setEditor(this);
				this.editPopupMenu.setParentFrame(this.applFrame);
			}
			if (this.editSelPopupMenu != null) {
				this.editSelPopupMenu.setEditor(this);
				this.editSelPopupMenu.setParentFrame(this.applFrame);
			}		

//			isEditPopupMenu = false;
//			isEditSelPopupMenu = false;
			if (this.modePopupMenu != null
					&& this.modePopupMenu.invoked(this, this.graphPanel, e.getX(), e.getY()))
				this.modePopupMenu.show(e.getComponent(), e.getX(), e.getY());
			else if (this.editPopupMenu != null
					&& this.editPopupMenu.invoked(this.graphPanel, e.getX(), e.getY())) {
//				isEditPopupMenu = true;
				this.editPopupMenu.setMapEnabled(false);
				this.editPopupMenu.setUnmapEnabled(true);
				this.editPopupMenu.showMe(e.getComponent(), e.getX(), e.getY());
			} else if (this.editSelPopupMenu != null
					&& this.editSelPopupMenu.invoked(this.graphPanel, e.getX(), e.getY())) {
//				isEditSelPopupMenu = true;
				this.editSelPopupMenu.setMapEnabled(false);
				this.editSelPopupMenu.setUnmapEnabled(true);
				this.editSelPopupMenu.showMe(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	protected final JFrame applFrame;

	int prefW = 500;
	
	int prefH = 200;
	
	private final GraGraEditor gragraEditor;

//	private AttrTopEditor attrEditor;

	protected GraphPanel graphPanel;
	
	private boolean graphAttrsVisible = true;
	
	private boolean typeGraphAttrsVisible = true;
	
	private final JLabel title;

	private String titleKind = " ";
	
	private String graphName;

	private String gragraName;

	private EdGraGra eGra;

	private EdGraph eGraph;

	private EdGraph sourceOfCopy;

	private String msg = "";

	private ModePopupMenu modePopupMenu;

	private EditPopupMenu editPopupMenu;

	private EditSelPopupMenu editSelPopupMenu;

//	private boolean isEditPopupMenu;

//	private boolean isEditSelPopupMenu;
	
	private boolean doNotShowPopupMenu;
	
	private boolean isEmpty;

	protected GraphicsExportJPEG exportJPEG;

	private boolean staticNodePositionForGraphLayouter;
	 
	private boolean straightenArcs;

	private final JButton exportJPEGButton;

	private final JPanel buttonPanel, titlePanel;
	
	

}
