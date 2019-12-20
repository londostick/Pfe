/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.typeeditor;

import java.util.Vector;
import java.awt.Color;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.Icon;
import javax.swing.ListCellRenderer;
import javax.swing.border.TitledBorder;
import javax.swing.SwingUtilities;

import agg.editor.impl.EdType;
import agg.gui.editor.EditorConstants;
import agg.gui.editor.GraphPanel;

@SuppressWarnings("serial")
public class TypePalette extends JPanel implements MouseListener, MouseMotionListener {

	final Vector<JLabel> nodeTypes, edgeTypes;

	@SuppressWarnings("rawtypes")
	final JList nodeTypeList, edgeTypeList; // element: String

	private final JSplitPane splitPane;

	private final JPanel panel1, panel2, editNodeTypeP, editEdgeTypeP;

	final JButton modifyNodeTypeB, newNodeTypeB, deleteNodeTypeB,
			modifyEdgeTypeB, newEdgeTypeB, deleteEdgeTypeB;

	final TypeEditor typeEditor;

	private int dividerLocation = 0;

	private int width = 220;
	
	private boolean selectionDone;
	private int indxN=-1, indxE=-1;
	private Point p = new Point(0,0);
	private Point lp = new Point(-1,-1);
	private boolean dragged;
	
	@SuppressWarnings("rawtypes")
	public TypePalette(final TypeEditor typeeditor) {
		super();
		this.typeEditor = typeeditor;

		this.nodeTypes = new Vector<JLabel>();
		this.nodeTypeList = new JList();
		this.nodeTypeList.setToolTipText("Double click to get attribute editor.");

		this.edgeTypes = new Vector<JLabel>();
		this.edgeTypeList = new JList();
		this.edgeTypeList.setToolTipText("Double click to get attribute editor.");

		this.panel1 = new JPanel(new BorderLayout());
		this.newNodeTypeB = new JButton("New");
		this.modifyNodeTypeB = new JButton("Edit");
		this.deleteNodeTypeB = new JButton("Del");
		this.editNodeTypeP = handleNodeTypeButtons();

		this.panel2 = new JPanel(new BorderLayout());
		this.newEdgeTypeB = new JButton("New");
		this.modifyEdgeTypeB = new JButton("Edit");
		this.deleteEdgeTypeB = new JButton("Del");
		this.editEdgeTypeP = handleArcTypeButtons();

		this.splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, null, null);
		this.splitPane.setDividerSize(15);
		add(this.splitPane, BorderLayout.CENTER);

		validate();
	}

	public void setEnabled(boolean b) {
		super.setEnabled(b);
		this.nodeTypeList.setEnabled(b);
		this.edgeTypeList.setEnabled(b);
		if (!b) {
			this.newNodeTypeB.setEnabled(b);
			this.modifyNodeTypeB.setEnabled(b);
			this.deleteNodeTypeB.setEnabled(b);

			this.newEdgeTypeB.setEnabled(b);
			this.modifyEdgeTypeB.setEnabled(b);
			this.deleteEdgeTypeB.setEnabled(b);
		} else {
			this.newNodeTypeB.setEnabled(b);
			this.newEdgeTypeB.setEnabled(b);
			if (this.nodeTypeList.getModel().getSize() > 0) {
				this.modifyNodeTypeB.setEnabled(b);
				this.deleteNodeTypeB.setEnabled(b);
			} else {
				this.modifyNodeTypeB.setEnabled(false);
				this.deleteNodeTypeB.setEnabled(false);
			}
			if (this.edgeTypeList.getModel().getSize() > 0) {
				this.modifyEdgeTypeB.setEnabled(b);
				this.deleteEdgeTypeB.setEnabled(b);
			} else {
				this.modifyEdgeTypeB.setEnabled(false);
				this.deleteEdgeTypeB.setEnabled(false);
			}

		}
	}

	private JPanel handleNodeTypeButtons() {
		final JPanel p = new JPanel();

		this.newNodeTypeB.setToolTipText(" Create Node Type ");
		this.newNodeTypeB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TypePalette.this.typeEditor.getNodeTypePropertyEditor()
						.setNewTypeDefaultProperty();

				TypePalette.this.typeEditor.getNodeTypePropertyEditor()
						.enableChangeButton(false);
				TypePalette.this.typeEditor.getNodeTypePropertyEditor()
						.enableDeleteButton(false);

				Point point = ((JButton) e.getSource()).getLocationOnScreen();
				TypePalette.this.typeEditor.showNodeTypePropertyEditorl(point.x, point.y);
			}
		});

		this.modifyNodeTypeB.setToolTipText(" Modify Node Type ");
		this.modifyNodeTypeB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TypePalette.this.typeEditor.getNodeTypePropertyEditor().enableChangeButton(true);
				TypePalette.this.typeEditor.getNodeTypePropertyEditor().enableDeleteButton(true);

				EdType type = TypePalette.this.typeEditor.getSelectedNodeType();
				TypePalette.this.typeEditor.getNodeTypePropertyEditor().setSelectedTypeProperty(
						type);

				Point point = ((JButton) e.getSource()).getLocationOnScreen();
				TypePalette.this.typeEditor.showNodeTypePropertyEditorl(point.x, point.y);
			}
		});

		this.deleteNodeTypeB.setToolTipText(" Delete Node Type ");
		this.deleteNodeTypeB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TypePalette.this.typeEditor.deleteSelectedNodeType(true);
			}
		});

		p.add(this.newNodeTypeB);
		p.add(this.modifyNodeTypeB);
		p.add(this.deleteNodeTypeB);
		return p;
	}

	
	private JPanel handleArcTypeButtons() {
		final JPanel p = new JPanel();

		this.newEdgeTypeB.setToolTipText("Create Edge Type");
		this.newEdgeTypeB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TypePalette.this.typeEditor.getArcTypePropertyEditor()
						.setNewTypeDefaultProperty();

				TypePalette.this.typeEditor.getArcTypePropertyEditor().enableChangeButton(false);
				TypePalette.this.typeEditor.getArcTypePropertyEditor().enableDeleteButton(false);

				Point point = ((JButton) e.getSource()).getLocationOnScreen();
				TypePalette.this.typeEditor.showArcTypePropertyEditorl(point.x, point.y);
			}
		});

		this.modifyEdgeTypeB.setToolTipText("Modify Edge Type");
		this.modifyEdgeTypeB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TypePalette.this.typeEditor.getArcTypePropertyEditor().enableChangeButton(true);
				TypePalette.this.typeEditor.getArcTypePropertyEditor().enableDeleteButton(true);

				EdType type = TypePalette.this.typeEditor.getSelectedArcType();
				TypePalette.this.typeEditor.getArcTypePropertyEditor().setSelectedTypeProperty(
						type);

				Point point = ((JButton) e.getSource()).getLocationOnScreen();
				TypePalette.this.typeEditor.showArcTypePropertyEditorl(point.x, point.y);
			}
		});

		this.deleteEdgeTypeB.setToolTipText("Delete Edge Type");
		this.deleteEdgeTypeB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TypePalette.this.typeEditor.deleteSelectedArcType(true);
			}
		});

		p.add(this.newEdgeTypeB);
		p.add(this.modifyEdgeTypeB);
		p.add(this.deleteEdgeTypeB);
		return p;
	}

	public void setTypes(Vector<JLabel> nodetypes, Vector<JLabel> edgetypes) {
		this.panel1.removeAll();
		this.panel2.removeAll();

		indxN=(nodetypes.size() > 0)? 0: -1; 
		indxE=(edgetypes.size() > 0)? 0: -1;
		setNodeTypes(nodetypes);
		setArcTypes(edgetypes);

		// nodeTypeList = createTypeList(nodetypes);
		createTypeList(this.nodeTypeList, nodetypes);
		try {
			final JScrollPane scrollPane1 = new JScrollPane(this.nodeTypeList);
			scrollPane1.setBorder(new TitledBorder("  Node Types  "));
			this.nodeTypeList
					.setSelectedIndex(this.nodeTypeList.getModel().getSize() - 1);
			if (this.panel1.getComponentCount() == 0) {
				this.panel1.add(scrollPane1, BorderLayout.CENTER);
				this.panel1.add(this.editNodeTypeP, BorderLayout.SOUTH);
				this.splitPane.setTopComponent(this.panel1);
			}
		} catch (IllegalArgumentException ex) {
		}

		// this.edgeTypeList = createTypeList(edgetypes);
		createTypeList(this.edgeTypeList, edgetypes);
		try {
			final JScrollPane scrollPane2 = new JScrollPane(this.edgeTypeList);
			scrollPane2.setBorder(new TitledBorder("  Edge Types  "));
			this.edgeTypeList
					.setSelectedIndex(this.edgeTypeList.getModel().getSize() - 1);
			if (this.panel2.getComponentCount() == 0) {
				this.panel2.add(scrollPane2, BorderLayout.CENTER);
				this.panel2.add(this.editEdgeTypeP, BorderLayout.SOUTH);
				this.splitPane.setBottomComponent(this.panel2);
			}
		} catch (IllegalArgumentException ex) {
		}

		this.splitPane.revalidate();
		if (this.dividerLocation == 0)
			this.splitPane.setDividerLocation(getHeight() / 2);
	}

	public boolean isEmpty() {
		if (this.nodeTypes.isEmpty() && this.edgeTypes.isEmpty())
			return true;
		
		return false;
	}

	public boolean isNodeTypesEmpty() {
		if (this.nodeTypes.isEmpty())
			return true;
		
		return false;
	}

	public boolean isArcTypesEmpty() {
		if (this.edgeTypes.isEmpty())
			return true;
		
		return false;
	}

	public void enableNodeTypeModifyButton(boolean b) {
		this.modifyNodeTypeB.setEnabled(b);
	}

	public void enableNodeTypeDeleteButton(boolean b) {
		this.deleteNodeTypeB.setEnabled(b);
	}

	public void enableArcTypeModifyButton(boolean b) {
		this.modifyEdgeTypeB.setEnabled(b);
	}

	public void enableArcTypeDeleteButton(boolean b) {
		this.deleteEdgeTypeB.setEnabled(b);
	}

	public boolean isOpen() {
		if (this.splitPane.getWidth() > 0)
			return true;
		
		return false;
	}

	public void clear() {
		this.nodeTypes.clear();
		this.edgeTypes.clear();
		this.nodeTypeList.removeAll();
		this.edgeTypeList.removeAll();
		this.splitPane.setTopComponent(null);
		this.splitPane.setBottomComponent(null);
	}

	public void setDividerLocation(int d) {
		this.splitPane.setDividerLocation(d);
		this.dividerLocation = this.splitPane.getDividerLocation();
	}

	public int getDividerLocation() {
		if (this.dividerLocation != 0
				&& this.dividerLocation != this.splitPane.getDividerLocation())
			this.dividerLocation = this.splitPane.getDividerLocation();
		return this.dividerLocation;
	}

	public Component getTypePaletteComponent() {
		return this.splitPane;
	}

	public int getWidthOfPalette() {
		return this.width;
	}

	public Dimension getMinimumSize() {
		return new Dimension(0, 0);
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(200, 500);
	}

	public JButton getEditNodeTypeButton() {
		return this.modifyNodeTypeB;
	}

	public JButton getNewNodeTypeButton() {
		return this.newNodeTypeB;
	}

	public JButton getDeleteNodeTypeButton() {
		return this.deleteNodeTypeB;
	}

	public JButton getEditArcTypeButton() {
		return this.modifyEdgeTypeB;
	}

	public JButton getNewArcTypeButton() {
		return this.newEdgeTypeB;
	}

	public JButton getDeleteArcTypeButton() {
		return this.deleteEdgeTypeB;
	}

	@SuppressWarnings("rawtypes")
	public JList getNodeTypeList() {
		return this.nodeTypeList;
	}

	@SuppressWarnings("rawtypes")
	public JList getArcTypeList() {
		return this.edgeTypeList;
	}

	public void setSelectedNodeTypeIndex(int index) {
		if (index >= 0)
			this.nodeTypeList.setSelectedIndex(index);
		else {
			this.modifyNodeTypeB.setEnabled(false);
			this.deleteNodeTypeB.setEnabled(false);
			this.typeEditor.getNodeTypePropertyEditor().enableChangeButton(false);
			this.typeEditor.getNodeTypePropertyEditor().enableDeleteButton(false);
		}
	}

	public int getSelectedNodeTypeIndex() {
		return this.nodeTypeList.getSelectedIndex();
	}

	public JLabel getSelectedNodeTypeLabel() {
		int i = this.nodeTypeList.getSelectedIndex();
		if (i < 0)
			return new JLabel("");
		return this.nodeTypes.get(i);
	}
	
	public void setSelectedArcTypeIndex(int index) {
		if (index >= 0)
			this.edgeTypeList.setSelectedIndex(index);
		else {
			this.modifyEdgeTypeB.setEnabled(false);
			this.deleteEdgeTypeB.setEnabled(false);
			this.typeEditor.getArcTypePropertyEditor().enableChangeButton(false);
			this.typeEditor.getArcTypePropertyEditor().enableDeleteButton(false);
		}
	}

	public int getSelectedArcTypeIndex() {		
		return this.edgeTypeList.getSelectedIndex();
	}

	public JLabel getSelectedArcTypeLabel() {
		int i = this.edgeTypeList.getSelectedIndex();
		if (i < 0)
			return new JLabel("");
		
		return this.edgeTypes.get(i);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int addNodeType(JLabel l) {
		this.nodeTypes.add(l);
		int index = this.nodeTypes.size() - 1;
		((DefaultListModel) this.nodeTypeList.getModel()).addElement(l.getText());
		return index;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addNodeType(JLabel l, int index) {
		if (index < this.nodeTypes.size()) {
			this.nodeTypes.add(index, l);
			((DefaultListModel) this.nodeTypeList.getModel())
					.add(index, l.getText());
		} else {
			this.nodeTypes.add(l);
			((DefaultListModel) this.nodeTypeList.getModel())
					.addElement(l.getText());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int addArcType(JLabel l) {
		this.edgeTypes.add(l);
		int index = this.edgeTypes.size() - 1;
		((DefaultListModel) this.edgeTypeList.getModel()).addElement(l.getText());
		return index;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addArcType(JLabel l, int index) {
		if (index < this.edgeTypes.size()) {
			this.edgeTypes.add(index, l);
			((DefaultListModel) this.edgeTypeList.getModel())
					.add(index, l.getText());
		} else {
			this.edgeTypes.add(l);
			((DefaultListModel) this.edgeTypeList.getModel())
					.addElement(l.getText());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void changeNodeType(JLabel l, int index) {
		if (index < this.nodeTypeList.getModel().getSize()) {
			this.nodeTypes.set(index, l);
			((DefaultListModel) this.nodeTypeList.getModel()).setElementAt(l
					.getText(), index);
		}
	}

	@SuppressWarnings("rawtypes")
	public void deleteSelectedNodeType() {
		int index = getSelectedNodeTypeIndex();
		this.nodeTypes.remove(index);
		((DefaultListModel) this.nodeTypeList.getModel()).remove(index);
	}

	@SuppressWarnings("rawtypes")
	public void deleteNodeTypeAt(int index) {
		if (index < this.nodeTypeList.getModel().getSize()) {
			this.nodeTypes.remove(index);
			((DefaultListModel) this.nodeTypeList.getModel()).remove(index);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void refreshNodeType(JLabel l, int index) {
		if (index < this.nodeTypeList.getModel().getSize()) {
			this.nodeTypes.set(index, l);
			((DefaultListModel) this.nodeTypeList.getModel()).setElementAt(l
					.getText(), index);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void changeArcType(JLabel l, int index) {
		if (index < this.edgeTypeList.getModel().getSize()) {
			this.edgeTypes.set(index, l);
			((DefaultListModel) this.edgeTypeList.getModel()).setElementAt(l
					.getText(), index);
		}
	}

	@SuppressWarnings("rawtypes")
	public void deleteSelectedEdgeType() {
		int index = getSelectedArcTypeIndex();
		((DefaultListModel) this.edgeTypeList.getModel()).remove(index);
		this.edgeTypes.remove(index);
	}

	@SuppressWarnings("rawtypes")
	public void deleteArcTypeAt(int index) {
		if (index < this.edgeTypeList.getModel().getSize()) {
			((DefaultListModel) this.edgeTypeList.getModel()).remove(index);
			this.edgeTypes.remove(index);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void refreshArcType(JLabel l, int index) {
		if (index < this.edgeTypeList.getModel().getSize()) {
			this.edgeTypes.set(index, l);
			((DefaultListModel) this.edgeTypeList.getModel()).setElementAt(l
					.getText(), index);
		}
	}

	private void setNodeTypes(Vector<JLabel> types) {
		this.nodeTypes.clear();
		for (int i = 0; i < types.size(); i++) {
			JLabel l = types.get(i);
			this.nodeTypes.add(l);
		}
	}

	private void setArcTypes(Vector<JLabel> types) {
		this.edgeTypes.clear();
		for (int i = 0; i < types.size(); i++) {
			JLabel l = types.get(i);
			this.edgeTypes.add(l);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JList createTypeList(JList list, Vector<JLabel> types) {
		final Vector<String> v = new Vector<String>(types.size());
		for (int i = 0; i < types.size(); i++) {
			v.add(types.get(i).getText());
		}

		list.setModel(new DefaultListModel());
		list.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		for (int i = 0; i < v.size(); i++) {
			String text = v.get(i);
			((DefaultListModel) list.getModel()).addElement(text);
		}
		list.setPrototypeCellValue("Index 1234567890");
		final MyCellRenderer cellRenderer = new MyCellRenderer();
		list.setCellRenderer(cellRenderer);
		list.addMouseListener(this);
		list.addMouseMotionListener(this);
		return list;
	}

	/**
	 * An node/edge type is selected, if the mouse is clicked inside of the
	 * node/edge type list. The edit mode of the gragra editor will change to
	 * DRAW.
	 */
	@SuppressWarnings("rawtypes")
	public void mouseClicked(MouseEvent e) {
		if (!this.isEnabled()) {
			this.transferFocusBackward();
			return;
		}

		if (SwingUtilities.isLeftMouseButton(e)) {
			if (!this.selectionDone)
				selectType(e);
		}
		if ((e.getClickCount() == 2)
				|| this.typeEditor.getGraGraEditor().hasAttrEditorOnTop()) {
			if (((JList) e.getSource()) == this.nodeTypeList) {
				if (!this.nodeTypes.isEmpty())
					this.typeEditor.getGraGraEditor().setAttrEditorOnTopForType(
						this.typeEditor.getSelectedNodeType().getBasisType());
			} else if (((JList) e.getSource()) == this.edgeTypeList) {
				if (!this.edgeTypes.isEmpty())
					this.typeEditor.getGraGraEditor().setAttrEditorOnTopForType(						
						this.typeEditor.getSelectedArcType().getBasisType());
			}
		}
	}

	/** not implemented */
	public void mouseEntered(MouseEvent e) {}

	/** not implemented */
	public void mouseExited(MouseEvent e) {}

	/**
	 * A type is selected, if the mouse is clicked inside of the
	 * node/edge type list. The edit mode of the gragra editor will change to
	 * DRAW.
	 */
	public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {			
			if (e.getPoint().x > 0 && e.getPoint().y > 0) {
				selectType(e);
			}
			else {
				this.nodeTypeList.setSelectedIndex(indxN);
				this.edgeTypeList.setSelectedIndex(indxE);
				if (dragged && e.getSource() == this.nodeTypeList) {
					GraphPanel panel = this.typeEditor.getGraGraEditor().getPanelOfLocationOnScreen(e.getLocationOnScreen());
					if (panel != null) {
						Point pt = this.getLocationOnCanvas(panel, e.getLocationOnScreen());
						if (pt != null) {
							typeEditor.getGraGraEditor().updateGraphics();
							panel.getCanvas().addNode(pt.x, pt.y);
						}					
					}
				}
			}
		}
		dragged = false;
	}

	/**
	 * An node/edge type is selected, if the mouse is clicked inside of the
	 * node/edge type list. The edit mode of the gragra editor will change to
	 * DRAW.
	 */
	public void mousePressed(MouseEvent e) {
		dragged = false;
		p = e.getPoint();
		if (SwingUtilities.isLeftMouseButton(e) && p.x>0 && p.y>0) {
			this.selectionDone = false;
			selectType(e);			
		}
	}

	@SuppressWarnings("rawtypes")
	private void selectType(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e) && p.x > 0 && p.y > 0) {
			if (((JList) e.getSource()) == this.nodeTypeList) {
				if (this.nodeTypeList.getModel().getSize() > 0) {
					int index = this.nodeTypeList.locationToIndex(e.getPoint());
					if (index == -1 || index >= this.nodeTypes.size())
						return;
	
					indxN = index;
	//				JLabel tlabel = this.nodeTypes.get(index);
					EdType type = this.typeEditor.selectNodeTypeAtIndex(index);
					this.typeEditor.getNodeTypePropertyEditor().setSelectedTypeProperty(
							type);
	
					this.modifyNodeTypeB.setEnabled(true);
					this.deleteNodeTypeB.setEnabled(true);
					this.typeEditor.getNodeTypePropertyEditor().enableChangeButton(true);
					this.typeEditor.getNodeTypePropertyEditor().enableDeleteButton(true);
	
					// set DRAW mode of the gragra editor
					this.typeEditor.getGraGraEditor().setEditMode(EditorConstants.DRAW);
					this.typeEditor.getGraGraEditor().forwardModeCommand("Draw");
					this.selectionDone = true;
				}
			} else if (((JList) e.getSource()) == this.edgeTypeList) {
				if (this.edgeTypeList.getModel().getSize() > 0) {
					int index = this.edgeTypeList.locationToIndex(e.getPoint());
					if (index == -1 || index >= this.edgeTypes.size())
						return;
	
					indxE = index;
	//				JLabel tlabel = this.edgeTypes.get(index);
					EdType type = this.typeEditor.selectArcTypeAtIndex(index);
	
					this.typeEditor.getArcTypePropertyEditor().setSelectedTypeProperty(
							type);
	
					this.modifyEdgeTypeB.setEnabled(true);
					this.deleteEdgeTypeB.setEnabled(true);
					this.typeEditor.getArcTypePropertyEditor().enableChangeButton(true);
					this.typeEditor.getArcTypePropertyEditor().enableDeleteButton(true);
	
					// set DRAW mode of the gragra editor
					this.typeEditor.getGraGraEditor().setEditMode(EditorConstants.DRAW);
					this.typeEditor.getGraGraEditor().forwardModeCommand("Draw");
					this.selectionDone = true;
				}
			}
		}
	}

	// Display an icon and a string for each object in the list.
	@SuppressWarnings("rawtypes")
	class MyCellRenderer extends JLabel implements ListCellRenderer {
		// This is the only method defined by ListCellRenderer.
		// We just reconfigure the JLabel each time we're called.

		public Component getListCellRendererComponent(JList list, Object value, // value
				// to
				// display
				int index, // cell index
				boolean isSelected, // is the cell selected
				boolean cellHasFocus) // the list and the cell have the focus
		{
			
			String str = value.toString();
			setText(str);
			if (list == TypePalette.this.nodeTypeList && !TypePalette.this.nodeTypes.isEmpty()) {				
				Icon icon = TypePalette.this.nodeTypes.get(index).getIcon();
				setIcon(icon);
				setForeground(TypePalette.this.nodeTypes.get(index).getForeground());
				if (isSelected /*&& index == indxN*/)
					setBackground(list.getSelectionBackground());
				else
					setBackground(list.getBackground());
			} 
			else if (list == TypePalette.this.edgeTypeList && !TypePalette.this.edgeTypes.isEmpty()) {
				Icon icon = TypePalette.this.edgeTypes.get(index).getIcon();
				setIcon(icon);
				setForeground(TypePalette.this.edgeTypes.get(index).getForeground());
				if (isSelected /*&& index == indxE*/)
					setBackground(list.getSelectionBackground());
				else
					setBackground(list.getBackground());
			}
			setEnabled(list.isEnabled());
			setFont(list.getFont());
			setOpaque(true);
			
			return this;
		}

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		this.p = e.getPoint();
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (this.p.x < 0 || this.p.y < 0) {
				this.nodeTypeList.setSelectedIndex(this.indxN);
				this.edgeTypeList.setSelectedIndex(this.indxE);
				if (e.getSource() == this.nodeTypeList) {
					int px = e.getLocationOnScreen().x - typeEditor.getGraGraEditor().getLocationOnScreen().x;
					int py = e.getLocationOnScreen().y - typeEditor.getGraGraEditor().getLocationOnScreen().y;
					if (lp.x > 0 && lp.y > 0) {
						typeEditor.getGraGraEditor().setForeground(Color.WHITE);
						typeEditor.getGraGraEditor().getGraphics().drawRect(lp.x, lp.y, 14, 10);
						typeEditor.getGraGraEditor().setForeground(Color.BLACK);
					}
					typeEditor.getGraGraEditor().getGraphics().drawRect(px, py, 14, 10);
					lp.x = px; lp.y = py;
					dragged = true;
				}
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	
	private Point getLocationOnCanvas(GraphPanel panel, Point pointOnScreen) {
		Point pt = new Point(20, 20);		
		pt.x = pointOnScreen.x - panel.getLocationOnScreen().x + panel.getHorizontalScrollBar().getValue();
		pt.y = pointOnScreen.y - panel.getLocationOnScreen().y + panel.getVerticalScrollBar().getValue();
		
		return pt;
	}
	

}
