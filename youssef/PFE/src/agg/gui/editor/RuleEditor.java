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
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.Insets;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;

import agg.attribute.gui.AttrTopEditor;
import agg.editor.impl.EdArc;
import agg.editor.impl.EdAtomic;
import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdGraph;
import agg.editor.impl.EdGraphObject;
import agg.editor.impl.EdNAC;
import agg.editor.impl.EdNestedApplCond;
import agg.editor.impl.EdNode;
import agg.editor.impl.EdPAC;
//import agg.editor.impl.EdNode;
import agg.editor.impl.EdRule;
import agg.editor.impl.EdRuleScheme;
import agg.editor.impl.Loop;
import agg.gui.AGGAppl;
import agg.gui.editor.RuleEditorMouseAdapter;
import agg.gui.editor.RuleEditorMouseMotionAdapter;
import agg.gui.popupmenu.EditPopupMenu;
import agg.gui.popupmenu.EditSelPopupMenu;
import agg.gui.popupmenu.ModePopupMenu;
import agg.gui.saveload.GraphicsExportJPEG;
import agg.gui.treeview.nodedata.NACTreeNodeData;
import agg.gui.treeview.nodedata.NestedACTreeNodeData;
import agg.gui.treeview.nodedata.PACTreeNodeData;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.GraphObject;
import agg.xt_basis.Node;
import agg.xt_basis.Arc;
import agg.xt_basis.agt.AmalgamatedRule;
import agg.xt_basis.agt.KernelRule;
import agg.xt_basis.agt.MultiRule;
import agg.xt_basis.agt.RuleScheme;

/**
 * The class RuleEditor specifies a rule editor for editing a rule of the class
 * EdRule.
 * 
 * @author $Author: olga $
 */
@SuppressWarnings("serial")
public class RuleEditor extends JPanel {

	private final RuleEditorMouseAdapter mouseAdapter;
	private final RuleEditorMouseMotionAdapter mouseMotionAdapter;
	
	/**
	 * Creates a rule editor. The main editor is specified by the GraGraEditor
	 * anEditor or NULL
	 */
	public RuleEditor(GraGraEditor anEditor) {
		super(new BorderLayout());
		this.mainPanel = this;
		
		this.mouseAdapter = new RuleEditorMouseAdapter(this);
		this.mouseMotionAdapter = new RuleEditorMouseMotionAdapter(this);
		
		this.leftPanel = new GraphPanel(this);		
		final JPanel lPanel = new JPanel(new BorderLayout());
		lPanel.setPreferredSize(new Dimension(250, 150));
		lPanel.add(this.leftPanel, BorderLayout.CENTER);

		this.rightPanel = new GraphPanel(this);		
		final JPanel rPanel = new JPanel(new BorderLayout());
		rPanel.setPreferredSize(new Dimension(250, 150));
		rPanel.add(this.rightPanel, BorderLayout.CENTER);
		
		this.ruleSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lPanel,
				rPanel);
		this.ruleSplitPane.setBackground(Color.WHITE);
		this.ruleSplitPane.setDividerSize(10);
		this.ruleSplitPane.setContinuousLayout(true);
		this.ruleSplitPane.setOneTouchExpandable(true);
		this.ruleDividerLocation = 250;
		this.ruleSplitPane.setDividerLocation(this.ruleDividerLocation);

		final JPanel panelRule = new JPanel(new BorderLayout());
		panelRule.setPreferredSize(new Dimension(500, 150));
		this.title = new JLabel("  ");

		// title.setIcon(ruleExportJPEG.getIcon());
		this.exportJPEGButton = createExportJPEGButton();
		final JPanel rtitlePanel = new JPanel(new BorderLayout());
		rtitlePanel.add(this.title, BorderLayout.WEST);
		if (this.exportJPEGButton != null)
			rtitlePanel.add(this.exportJPEGButton, BorderLayout.EAST);
		panelRule.add(rtitlePanel, BorderLayout.NORTH);

		panelRule.add(this.ruleSplitPane, BorderLayout.CENTER);

		this.leftCondPanel = new GraphPanel(this);
		this.titleAC = new JLabel("  ");
		this.leftCondPanel.add(this.titleAC, BorderLayout.NORTH);

		this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, null, panelRule);
		this.splitPane.setContinuousLayout(true);
		this.splitPane.setOneTouchExpandable(true);
		this.splitPane.setDividerSize(0);
		this.acDividerLocation = 150;

		this.dividerLocationSet = new Hashtable<Object, Integer>(0);

		add(this.splitPane, BorderLayout.CENTER);

		this.gragraEditor = anEditor;
		if (this.gragraEditor != null) {
			this.applFrame = anEditor.getParentFrame();
			if (this.gragraEditor.getGraphEditor() != null)
				this.graphEditor = this.gragraEditor.getGraphEditor();
		} else
			this.applFrame = null;

		this.leftPanel.getCanvas().addMouseListener(this.mouseAdapter);
		this.rightPanel.getCanvas().addMouseListener(this.mouseAdapter);
		this.leftCondPanel.getCanvas().addMouseListener(this.mouseAdapter);

		this.leftPanel.getCanvas().addMouseMotionListener(this.mouseMotionAdapter);
		this.rightPanel.getCanvas().addMouseMotionListener(this.mouseMotionAdapter);
		this.leftCondPanel.getCanvas().addMouseMotionListener(this.mouseMotionAdapter);
	}
	
	public JFrame getApplFrame() {
		return this.applFrame;
	}
	
	public RuleEditorMouseAdapter getMouseAdapter() {
		return this.mouseAdapter;
	}
	
	public RuleEditorMouseMotionAdapter getMouseMotionAdapter() {
		return this.mouseMotionAdapter;
	}
	
	public void setCursorOfApplFrame(Cursor cursor) {
		if (this.applFrame != null)
			this.applFrame.setCursor(cursor);
	}
	
	private JButton createExportJPEGButton() {
		java.net.URL url = ClassLoader.getSystemClassLoader()
								.getResource("agg/lib/icons/print.gif");
		if (url != null) {
		ImageIcon image = new ImageIcon(url);
		// System.out.println(image);
			JButton b = new JButton(image);
			b.setToolTipText("Export Rule JPEG");
			b.setMargin(new Insets(-5, 0, -5, 0));
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (RuleEditor.this.exportJPEG != null)
						if (!RuleEditor.this.exportJPEG.save(RuleEditor.this.mainPanel)) {
							JOptionPane.showMessageDialog(RuleEditor.this.applFrame, 
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

	public void setMoveCursorWhenLoop(EdGraphObject ego) {
		if (this.applFrame == null || ego == null || !ego.isArc()
				|| ((EdArc) ego).isLine())
			return;
		EdArc ea = (EdArc) ego;
		if (ea.getAnchorID() == Loop.CENTER)
			this.applFrame.setCursor(new Cursor(Cursor.MOVE_CURSOR));
		else if (ea.getAnchorID() == Loop.UPPER_LEFT)
			this.applFrame.setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR));
		else if (ea.getAnchorID() == Loop.UPPER_RIGHT)
			this.applFrame.setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR));
		else if (ea.getAnchorID() == Loop.BOTTOM_RIGHT)
			this.applFrame.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
		else if (ea.getAnchorID() == Loop.BOTTOM_LEFT)
			this.applFrame.setCursor(new Cursor(Cursor.SW_RESIZE_CURSOR));
	}

	public boolean setRuleMapping(EdGraphObject leftgo, EdGraphObject rightgo) {
		if (leftgo == null || rightgo == null)
			return false;
		
		if (this.eRule.getBasisRule() instanceof MultiRule) {
			if ( ((MultiRule) this.eRule.getBasisRule())
					.isTargetOfEmbeddingLeft(leftgo.getBasisObject())
						|| ((MultiRule) this.eRule.getBasisRule())
						.isTargetOfEmbeddingRight(rightgo.getBasisObject()) ) {
				if (!this.eRule.getBasisRule().getInverseImage(rightgo.getBasisObject()).hasMoreElements()) {
					JOptionPane.showMessageDialog(
							this.applFrame,
							"Mapping failed!"
							+"\nNew objects of kernel rule cannot be mapped from a multi rule.",
							"Mapping Error",
							JOptionPane.ERROR_MESSAGE);
					return false;	
				} else {
					// evntl. error msg
//					JOptionPane.showMessageDialog(
//							this.applFrame,
//							"Mapping failed!"
//							+"\nObjects of kernel rule cannot be mapped from a multi rule.",
//							"Mapping Error",
//							JOptionPane.ERROR_MESSAGE);
//					return false;
				}
			}
		}
		
		this.eRule.addCreatedMappingToUndo(leftgo, rightgo);
		this.eRule.interactRule(leftgo, rightgo);
		this.eRule.propagateAddRuleMappingToMultiRule(leftgo, rightgo);
		
		this.leftPanel.updateGraphics();
		this.rightPanel.updateGraphics();
		if (this.eRule.isBadMapping()) {
			this.eRule.undoManagerLastEditDie();
			this.msg = this.eRule.getMsg();

			if (leftgo.isArc()) {
				javax.swing.JOptionPane
						.showMessageDialog(
								this.applFrame,
								this.msg,
								"Mapping Error", javax.swing.JOptionPane.ERROR_MESSAGE);
			}
			
			return false;
		} 
		this.eRule.undoManagerEndEdit();
		if (this.gragraEditor != null)
			this.gragraEditor.updateUndoButton();
		
		// if (this.applFrame != null)
		// this.applFrame.setCursor(this.rightPanel.getEditCursor());
		return true;
	}
	
	public boolean removeRuleMapping(EdGraphObject go, boolean ruleLHS) {
		if (go == null) 
			return false;
		
		if (ruleLHS && go.getContext() == this.eRule.getLeft()) {
			if (this.eRule.getBasisRule() instanceof MultiRule) {
				if (!go.getMorphismMark().isEmpty()
						&& ((MultiRule) this.eRule.getBasisRule())
							.isTargetOfEmbeddingLeft(go.getBasisObject())) {
					
					JOptionPane.showMessageDialog(this.applFrame, 
							"Cannot remove this mapping. It should be removed from the kernel rule.", 
							"Remove Rule Object Mapping", 
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
			
			EdGraphObject rgo = null;
			if (this.eRule instanceof EdAtomic) {
				rgo = this.eRule.getRight().findGraphObject(
						((EdAtomic) this.eRule).getBasisAtomic().getImage(
								go.getBasisObject()));
			}
			else {
				this.eRule.propagateRemoveRuleMappingToMultiRule(go);
				
				rgo = this.eRule.getRight().findGraphObject(
						this.eRule.getBasisRule().getImage(go.getBasisObject()));
			}
			
			if (rgo != null) {
				this.eRule.addDeletedMappingToUndo(go, rgo);
				this.eRule.removeRuleMapping(go);
				this.eRule.undoManagerEndEdit();
				
				if (this.gragraEditor != null) {
					this.gragraEditor.updateUndoButton();
					this.gragraEditor.getGraGra().setChanged(true);
				}
								
				return true;
			} 
			return false;
		} 
		
		// ruleRHS
		if (go.getContext() == this.eRule.getRight()) {
			if (this.eRule.getBasisRule() instanceof MultiRule) {
				if (!go.getMorphismMark().isEmpty()
						&& ((MultiRule) this.eRule.getBasisRule())
									.isTargetOfEmbeddingRight(go.getBasisObject())) {
					JOptionPane.showMessageDialog(this.applFrame, 
							"Cannot remove this mapping. It should be removed from the kernel rule.", 
							"Remove Graph Object Mapping", 
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
				
			Vector<EdGraphObject> vec = this.eRule.getOriginal(go);
			boolean result = false;
			for (int i = 0; i < vec.size(); i++) {
				EdGraphObject lgo = vec.get(i);
				result = removeRuleMapping(lgo, true) || result;
			}
			if (result && this.gragraEditor != null)
				this.gragraEditor.getGraGra().setChanged(true);
				
			return result;	
		}
		return false;
	}

	public void setMappingRule(final EdGraphObject lobj, final EdGraphObject robj) {
		if (lobj != null && robj != null) {
			if (this.isEditPopupMenuShown() 
					&& this.getEditPopupMenu().isMapping()) {
				this.setObjMapping(true);
			}
			if (this.setRuleMapping(lobj, robj)) {
				this.getLeftPanel().updateGraphics();
				this.getRightPanel().updateGraphics();
			}
		}
		if (lobj != null)
			this.getLeftPanel().updateGraphics(); // wegen weak selected										
		this.setObjMapping(false);

		if (this.isEditPopupMenuShown()
				&& this.getEditPopupMenu().isMapping()
				&& !this.isObjMapping())
			this.resetEditModeAfterMapping();
	}
	
	public void setMappingRule(final List<EdGraphObject> lobjs, final EdGraphObject robj) {		
		if (lobjs != null && robj != null) {
			if (this.isEditSelPopupMenuShown() 
					&& this.getEditSelPopupMenu().isMapping()) {
				this.setObjMapping(true);
			}			
			boolean done = !lobjs.isEmpty();
			for (EdGraphObject lobj: lobjs) {
				if (this.setRuleMapping(lobj, robj)) {
					done = true;
				}
			}
			if (done) {
				this.getLeftPanel().updateGraphics();
				this.getRightPanel().updateGraphics();
			}
			this.setObjMapping(false);
		}
		if (this.isEditPopupMenuShown()
				&& this.getEditPopupMenu().isMapping()
				&& !this.isObjMapping())
			this.resetEditModeAfterMapping();
	}
	
	public void setMappingApplCond(final EdGraphObject lobj, final EdGraphObject cobj) {
		if (lobj != null && cobj != null) {
			if (this.isEditPopupMenuShown() 
					&& this.getEditPopupMenu().isMapping())
				this.setObjMapping(true);
			if (this.getNAC() != null 
					&& this.setNACMapping(lobj, cobj)) {
				this.getLeftPanel().updateGraphics();
				this.getLeftCondPanel().updateGraphics();
			} else if (this.getPAC() != null
					&& this.setPACMapping(lobj, cobj)) {
				this.getLeftPanel().updateGraphics();
				this.getLeftCondPanel().updateGraphics();
			} else if (this.getNestedAC() != null 
					&& this.setNestedACMapping(lobj, cobj)) {
				this.getLeftPanel().updateGraphics();
				this.getLeftCondPanel().updateGraphics();
			}
		}
		if (lobj != null)
			this.getLeftPanel().updateGraphics(); // wegen weak selected
		this.setObjMapping(false);

		if (this.isEditPopupMenuShown()
				&& this.getEditPopupMenu().isMapping()
				&& !this.isObjMapping()) 
			this.resetEditModeAfterMapping();
	}
	
	public void setMappingApplCond(final List<EdGraphObject> lobjs, final EdGraphObject cobj) {
		if (lobjs != null && cobj != null) {
			if (this.isEditSelPopupMenuShown() 
					&& this.getEditSelPopupMenu().isMapping())
				this.setObjMapping(true);
			boolean done = !lobjs.isEmpty();
			for (EdGraphObject lobj : lobjs) {
				if (this.getNAC() != null 
						&& this.setNACMapping(lobj, cobj)) {
					done = true;
				} else if (this.getPAC() != null
						&& this.setPACMapping(lobj, cobj)) {
					done = true;
				} else if (this.getNestedAC() != null 
						&& this.setNestedACMapping(lobj, cobj)) {
					done = true;
				}
			}
			
			if (done) {
				this.getLeftPanel().updateGraphics();
				this.getLeftCondPanel().updateGraphics();
			}
		}
		this.setObjMapping(false);

		if (this.isEditPopupMenuShown()
				&& this.getEditPopupMenu().isMapping()
				&& !this.isObjMapping()) 
			this.resetEditModeAfterMapping();
	}
	
	public void setMappingGraph(final EdGraphObject lobj, final EdGraphObject gobj) {
		if (lobj != null && gobj != null) {
			if (this.isEditPopupMenuShown() 
					&& this.getEditPopupMenu().isMapping())
				this.setObjMapping(true);
			if (this.setMatchMapping(lobj, gobj)) {
				this.getLeftPanel().updateGraphics();
				this.getGraphEditor().getGraphPanel().updateGraphics();
			}
		}
		if (lobj != null)
			this.getLeftPanel().updateGraphics(); // wegen weak selected
		this.setObjMapping(false);

		if (this.isEditPopupMenuShown() 
				&& this.getEditPopupMenu().isMapping()
				&& !this.isObjMapping())
			this.resetEditModeAfterMapping();
	}
	
	public void setMappingGraph(final List<EdGraphObject> lobjs, final EdGraphObject gobj) {
		if (lobjs != null && gobj != null) {
			if (this.isEditSelPopupMenuShown() 
					&& this.getEditSelPopupMenu().isMapping())
				this.setObjMapping(true);
			boolean done = !lobjs.isEmpty();
			for (EdGraphObject lobj: lobjs) {
				if (this.setMatchMapping(lobj, gobj))
					done = true;
			}
			if (done) {
				this.getLeftPanel().updateGraphics();
				this.getGraphEditor().getGraphPanel().updateGraphics();
			}
		}
		this.setObjMapping(false);

		if (this.isEditPopupMenuShown() 
				&& this.getEditPopupMenu().isMapping()
				&& !this.isObjMapping())
			this.resetEditModeAfterMapping();
	}
	
	public void removeMappingLeft(final EdGraphObject obj) {
		if (obj == null)
			return;
		
		boolean unmapdone = false;
		Vector<EdGraphObject> l = new Vector<EdGraphObject>(1);
		EdGraphObject lgo = null;
		if (obj.isSelected()) 
			l.addAll(this.getRule().getLeft().getSelectedObjs());
		else
			l.add(obj);		
		for (int i = 0; i < l.size(); i++) {
			lgo = l.elementAt(i);
			if (this.removeRuleMapping(lgo, true)
					|| (this.getNAC() != null 
						&& this.removeNacMapping(lgo, true))
					|| (this.getPAC() != null 
						&& this.removePacMapping(lgo, true))
					|| (this.getNestedAC() != null 
						&& this.removeNestedACMapping(obj, true)))
				unmapdone = true;
			if (this.getRule().getMatch() != null 
						&& this.removeMatchMapping(lgo, true))
				unmapdone = true;
		}
		if (unmapdone) {
			this.leftPanel.updateGraphics();
			this.rightPanel.updateGraphics();
			if (this.getNAC() != null)
				this.leftCondPanel.updateGraphics();
			else if (this.getPAC() != null)
				this.leftCondPanel.updateGraphics();
			else if (this.getNestedAC() != null)						
				this.leftCondPanel.updateGraphics();
			if (this.getRule().getMatch() != null 
					&& this.getGraphEditor() != null)
				this.getGraphEditor().getGraphPanel().updateGraphics();
		}
	}
	
	public void removeMappingRight(final EdGraphObject obj) {
		if (obj == null)
			return;
		
		Vector<EdGraphObject> vec = null;
		boolean unmapdone = false;
		EdGraphObject imageObj = null;
		Vector<EdGraphObject> l = new Vector<EdGraphObject>(1);
		if (obj.isSelected()) 
			l.addAll(this.getRule().getRight().getSelectedObjs());
		else
			l.add(obj);				
		for (int i = 0; i < l.size(); i++) {
			imageObj = l.elementAt(i);
			vec = this.getRule().getOriginal(imageObj);
			for (int j = 0; j < vec.size(); j++) {
				EdGraphObject go = vec.get(j);
				if (this.removeRuleMapping(go, true))
					unmapdone = true;
			}
		}					
		if (unmapdone) {
			this.leftPanel.updateGraphics();
			this.rightPanel.updateGraphics();
		}
	}
	
	public void removeMappingApplCond(final EdGraphObject obj) {
		if (obj == null)
			return;
//		this.leftCondObj = this.editor.setLeftCondGraphObject(this.editor.getNACPanel().getGraph().getPicked(x, y));
		boolean unmapdone = false;
		Vector<EdGraphObject> vec = null;
		EdGraphObject imageObj = null;
		EdGraphObject go = null;
		Vector<EdGraphObject> l = new Vector<EdGraphObject>(1);
		if (this.getNAC() != null) {
			if (obj.isSelected()) 
				l.addAll(this.getNAC().getSelectedObjs());
			else
				l.add(obj);
			for (int i = 0; i < l.size(); i++) {
				imageObj = l.elementAt(i);
				vec = this.getNAC().getOriginal(imageObj);
				for (int j = 0; j < vec.size(); j++) {
					go = vec.get(j);
					if (this.removeNacMapping(go, true))
						unmapdone = true;
				}
			}
		}
		else if (this.getPAC() != null) {
			if (obj.isSelected()) 
				l.addAll(this.getPAC().getSelectedObjs());
			else
				l.add(obj);
			for (int i = 0; i < l.size(); i++) {
				imageObj = l.elementAt(i);
				vec = this.getPAC().getOriginal(imageObj);
				for (int j = 0; j < vec.size(); j++) {
					go = vec.get(j);
					if (this.removePacMapping(go, true))
						unmapdone = true;
				}
			}
		}
		else if (this.getNestedAC() != null) {
			if (obj.isSelected()) 
				l.addAll(this.getNestedAC().getSelectedObjs());
			else
				l.add(obj);
			for (int i = 0; i < l.size(); i++) {
				imageObj = l.elementAt(i);
				vec = this.getNestedAC().getOriginal(imageObj);
				for (int j = 0; j < vec.size(); j++) {
					go = vec.get(j);
					if (this.removeNestedACMapping(go, true))
						unmapdone = true;
				}
			}
		}
		if (unmapdone) {
			this.leftPanel.updateGraphics();
			this.leftCondPanel.updateGraphics();
		}
	}
	
	public void removeMappingGraph(final EdGraphObject obj) {
		if (this.getGraphEditor() != null && obj != null) {
			boolean unmapdone = false;
			Enumeration<GraphObject> inverse = null;
			EdGraphObject lgo = null;
			if (obj.isSelected()) {
				EdGraphObject imageObj = null;
				for (int i = 0; i < this.getGraphEditor().getGraph()
						.getSelectedObjs().size(); i++) {
					imageObj = this.getGraphEditor().getGraph().getSelectedObjs().get(i);
					inverse = this.getRule().getMatch()
							.getInverseImage(imageObj.getBasisObject());
					while (inverse.hasMoreElements()) {
						lgo = this.getRule().getLeft().findGraphObject(inverse.nextElement());
						if (this.removeMatchMapping(lgo, true))
							unmapdone = true;
					}
				}
			} else if (this.getRule().getMatch() != null) {
				inverse = this.getRule().getMatch().getInverseImage(obj.getBasisObject());
				while (inverse.hasMoreElements()) {
					lgo = this.getRule().getLeft().findGraphObject(inverse.nextElement());
					if (this.removeMatchMapping(lgo, true))
						unmapdone = true;
				}
			}
			if (unmapdone) {
				this.leftPanel.updateGraphics();
				this.getGraphEditor().getGraphPanel().updateGraphics();
			}	
		}
	}
	
	public boolean setNACMapping(EdGraphObject leftgo, EdGraphObject nacgo) {
		if (leftgo == null || nacgo == null)
			return false;

		this.eRule.addCreatedNACMappingToUndo(leftgo, nacgo);
		this.eRule.interactNAC(leftgo, nacgo, this.eNAC.getMorphism());
		
		if (!this.eRule.getBasisRule()
				.compareConstantAttributeValue(leftgo.getBasisObject(), nacgo.getBasisObject())) {
			JOptionPane.showMessageDialog(
					this.applFrame,
					"NAC attribute value failed!"
					+"\nThe value of an attribute member of a NAC "
					+ "\nhas to be equal to the correspondent "
					+ "\nattribute value of the LHS of a rule."
					+"\nNAC attribute value will be unset.",
					"Attribute value changed",
					JOptionPane.ERROR_MESSAGE);
		}
		
		this.leftPanel.updateGraphics();
		this.leftCondPanel.updateGraphics();
		if (this.eRule.isBadMapping()) {
			this.eRule.undoManagerLastEditDie();
			this.msg = this.eRule.getMsg();
			if (leftgo.isArc()) {
				javax.swing.JOptionPane
						.showMessageDialog(
								this.applFrame,
								this.msg,
								"Mapping Error", javax.swing.JOptionPane.ERROR_MESSAGE);
			}
			return false;
		} 
		this.eRule.undoManagerEndEdit();
		if (this.gragraEditor != null) {
			this.gragraEditor.updateUndoButton();
			this.gragraEditor.getGraGra().setChanged(true);
		}
		
		return true;
	}

	/**
	 * Removes morphism mapping of the given object.
	 * The given object must to belong to the LHS of a rule when left is true,
	 * otherwise it is an object of the current NAC.
	 * 
	 * @return true if remove was successful, otherwise - false
	 */
	public boolean removeNacMapping(final EdGraphObject go, boolean left) {
		if (go == null || this.eNAC == null)
			return false;
		if (left) {			
			EdGraphObject ngo = this.eNAC.findGraphObject(this.eNAC.getMorphism()
					.getImage(go.getBasisObject()));
			if (ngo != null) {
				this.eRule.addDeletedNACMappingToUndo(go, ngo);
				this.eRule.removeNACMapping(go, this.eNAC.getMorphism());
				this.eRule.undoManagerEndEdit();
				if (this.gragraEditor != null) {
					this.gragraEditor.updateUndoButton();
					this.gragraEditor.getGraGra().setChanged(true);
				}
				return true;
			} 
			return false;
		}
		
		boolean res = false;
		Vector<EdGraphObject> vec = this.eNAC.getOriginal(go);
		for (int i = 0; i < vec.size(); i++) {
			EdGraphObject lgo = vec.get(i);
			res = removeNacMapping(lgo, true) || res;
		}			
		return res;
	}

	/**
	 * Removes morphism mapping from the given object of the LHS of a rule 
	 * to the image objects of all its NACs. 
	 * 
	 * @return true if remove was successful, otherwise - false
	 */
	public boolean removeNacMapping(final EdGraphObject left) {
		if (left == null)
			return false;
		boolean result = false;
		for (int i = 0; i < this.eRule.getNACs().size(); i++) {
			EdNAC nac = this.eRule.getNACs().get(i);
			EdGraphObject ngo = nac.findGraphObject(nac.getMorphism().getImage(
					left.getBasisObject()));
			if (ngo != null) {
				this.eRule.addDeletedNACMappingToUndo(left, ngo);
				this.eRule.removeNACMapping(left, nac.getMorphism());
				this.eRule.undoManagerEndEdit();
				result = true;
			}
		}
		if (result && this.gragraEditor != null) {
			this.gragraEditor.updateUndoButton();
			this.gragraEditor.getGraGra().setChanged(true);
		}
		return result;
	}

	public boolean setPACMapping(EdGraphObject leftgo, EdGraphObject pacgo) {
		if (leftgo == null || pacgo == null)
			return false;

		this.eRule.addCreatedACMappingToUndo(leftgo, pacgo);
		this.eRule.interactPAC(leftgo, pacgo, this.ePAC.getMorphism());
		
		if (!this.eRule.getBasisRule()
				.compareConstantAttributeValue(leftgo.getBasisObject(), pacgo.getBasisObject())) {
			JOptionPane.showMessageDialog(
					this.applFrame,
					"PAC attribute value failed!"
					+"\nThe value of each attribute of a PAC "
					+ "\nhas to be equal to the correspondent "
					+ "\nattribute value of the LHS of a rule."
					+"\nThe PAC attribute value will be unset.",
					"Attribute value changed",
					JOptionPane.ERROR_MESSAGE);
		}
		
		this.leftPanel.updateGraphics();
		this.leftCondPanel.updateGraphics();
		if (this.eRule.isBadMapping()) {
			this.eRule.undoManagerLastEditDie();
			this.msg = this.eRule.getMsg();
			if (leftgo.isArc()) {
				javax.swing.JOptionPane
						.showMessageDialog(
								this.applFrame,
								this.msg,
								"Mapping Error", javax.swing.JOptionPane.ERROR_MESSAGE);
			}
			return false;
		} 
		this.eRule.undoManagerEndEdit();
		if (this.gragraEditor != null) {
			this.gragraEditor.updateUndoButton();
			this.gragraEditor.getGraGra().setChanged(true);
		}
		
		return true;
	}

	/**
	 * Removes morphism mapping of the given object.
	 * The given object must to belong to the LHS of a rule when left is true,
	 * otherwise it is an object of the current PAC.
	 * 
	 * @return true if remove was successful, otherwise - false
	 */
	public boolean removePacMapping(final EdGraphObject go, boolean left) {
		if (go == null || this.ePAC == null)
			return false;
		if (left) {
			EdGraphObject pgo = this.ePAC.findGraphObject(this.ePAC.getMorphism()
					.getImage(go.getBasisObject()));
			if (pgo != null) {
				this.eRule.addDeletedPACMappingToUndo(go, pgo);
				this.eRule.removePACMapping(go, this.ePAC.getMorphism());
				this.eRule.undoManagerEndEdit();
				if (this.gragraEditor != null) {
					this.gragraEditor.updateUndoButton();
					this.gragraEditor.getGraGra().setChanged(true);
				}
				return true;
			}
			return false;
		} 
		boolean res = false;
		Vector<EdGraphObject> vec = this.ePAC.getOriginal(go);
		for (int i = 0; i < vec.size(); i++) {
			EdGraphObject lgo = vec.get(i);
			res = removePacMapping(lgo, true) || res;
		}
		return res;	
	}

	/**
	 * Removes morphism mapping from the given object of the LHS of a rule 
	 * to the image objects of all its PACs. 
	 * 
	 * @return true if remove was successful, otherwise - false
	 */
	public boolean removePacMapping(final EdGraphObject goLHS) {
		if (goLHS == null)
			return false;
		boolean result = false;
		for (int i = 0; i < this.eRule.getPACs().size(); i++) {
			EdPAC pac = this.eRule.getPACs().get(i);
			EdGraphObject pgo = pac.findGraphObject(pac.getMorphism().getImage(
					goLHS.getBasisObject()));
			if (pgo != null) {
				this.eRule.addDeletedPACMappingToUndo(goLHS, pgo);
				this.eRule.removePACMapping(goLHS, pac.getMorphism());
				this.eRule.undoManagerEndEdit();
				result = true;
			}
		}
		if (result && this.gragraEditor != null) {
			this.gragraEditor.updateUndoButton();
			this.gragraEditor.getGraGra().setChanged(true);
		}
		return result;
	}

	public boolean setNestedACMapping(EdGraphObject leftgo, EdGraphObject acgo) {
		if (leftgo == null || acgo == null)
			return false;

		this.eRule.addCreatedACMappingToUndo(leftgo, acgo);
		this.eRule.interactNestedAC(leftgo, acgo, this.eGAC.getNestedMorphism());
		
		this.eRule.updateNestedAC(this.eGAC);
		this.leftPanel.updateGraphics();
		this.leftCondPanel.updateGraphics();
		if (this.eRule.isBadMapping()) {
			this.eRule.undoManagerLastEditDie();
			this.msg = this.eRule.getMsg();
			if (leftgo.isArc()) {
				javax.swing.JOptionPane
						.showMessageDialog(
								this.applFrame,
								this.msg,
								"Mapping Error", javax.swing.JOptionPane.ERROR_MESSAGE);
			}
			return false;
		} 
		this.eRule.undoManagerEndEdit();
		if (this.gragraEditor != null) {
			this.gragraEditor.updateUndoButton();
			this.gragraEditor.getGraGra().setChanged(true);
		}
		
		return true;
	}
	
	/**
	 * Removes morphism mapping of the given object.
	 * The given object must to belong to the LHS of a rule when left is true,
	 * otherwise it is an object of the current AC.
	 * 
	 * @return true if remove was successful, otherwise - false
	 */
	public boolean removeNestedACMapping(final EdGraphObject go, boolean left) {
		if (go == null || this.eGAC == null)
			return false;
		if (left) {
			EdGraphObject pgo = this.eGAC.findGraphObject(this.eGAC.getMorphism()
					.getImage(go.getBasisObject()));
			if (pgo != null) {
				if (this.eGAC.getParent() == null) { // rule case
					this.eRule.addDeletedACMappingToUndo(go, pgo);
					this.eRule.removeNestedACMapping(go, this.eGAC.getNestedMorphism());
					this.eRule.undoManagerEndEdit();
				} else { // nested appl cond case
					this.eGAC.getParent().addDeletedMappingToUndo(go, pgo);
					this.eGAC.getParent().removeNestedACMapping(go, this.eGAC);
					this.eGAC.undoManagerEndEdit();
				}
				if (this.gragraEditor != null) {
					this.gragraEditor.updateUndoButton();
					this.gragraEditor.getGraGra().setChanged(true);
				}
				return true;
			}
			return false;
		} 
		boolean res = false;
		Vector<EdGraphObject> vec = this.eGAC.getOriginal(go);
		for (int i = 0; i < vec.size(); i++) {
			EdGraphObject lgo = vec.get(i);
			res = res || removeNestedACMapping(lgo, true) || res;
		}
		if (res)
			this.eGAC.updateNestedACs();
		return res;	
	}

	/**
	 * Removes morphism mapping from the given object of the LHS of a rule 
	 * to the image objects of all its nested ACs. 
	 */
	public boolean removeNestedACMapping(final EdGraphObject left) {
		if (left == null)
			return false;
		boolean result = false;
		for (int i = 0; i < this.eRule.getNestedACs().size(); i++) {
			EdNestedApplCond ac = (EdNestedApplCond) this.eRule.getNestedACs().get(i);
			EdGraphObject go = ac.findGraphObject(ac.getMorphism().getImage(
					left.getBasisObject()));
			if (go != null) {
				this.eRule.addDeletedACMappingToUndo(left, go);
				this.eRule.removeNestedACMapping(left, ac.getNestedMorphism());
				this.eRule.undoManagerEndEdit();
				result = true;
			} 
		}
		if (result && this.gragraEditor != null) {
			this.gragraEditor.updateUndoButton();
			this.gragraEditor.getGraGra().setChanged(true);
		}
		return result;
	}

	/**
	 * Removes morphism mapping of the given object.
	 * The given object must to belong to the LHS of a rule.
	 * The image objects can be objects of the ACs of the given list.
	 * 
	 * @return true if remove was successful, otherwise - false
	 */
	public boolean removeNestedACMapping(
			final EdGraphObject left, 
			final EdNestedApplCond cond,
			final List<EdNestedApplCond> list) {
		if (left == null)
			return false;
		boolean result = false;
		for (int i = 0; i < list.size(); i++) {
			EdNestedApplCond ac = list.get(i);
			EdGraphObject go = ac.findGraphObject(ac.getMorphism().getImage(
					left.getBasisObject()));
			if (go != null) {
				ac.addDeletedMappingToUndo(left, go);
				cond.removeNestedACMapping(left, ac);
				ac.undoManagerEndEdit();
				result = true;
			} 
		}
		if (result && this.gragraEditor != null) {
			this.gragraEditor.updateUndoButton();
			this.gragraEditor.getGraGra().setChanged(true);
		}
		return result;
	}
	
	public boolean setMatchMapping(EdGraphObject leftgo, EdGraphObject graphgo) {
//		System.out.println("RuleEditor.setMatchMapping");
		if (leftgo == null || graphgo == null)
			return false;

		if (this.eRule.getBasisRule().getMatch() == null)
			this.eRule.getGraGra().getBasisGraGra()
					.createMatch(this.eRule.getBasisRule());
		this.eRule.getBasisRule().getMatch().setCompletionStrategy(
				this.eRule.getGraGra().getBasisGraGra()
						.getMorphismCompletionStrategy());
		
		this.eRule.addCreatedMatchMappingToUndo(leftgo, graphgo);
		this.eRule.interactMatch(leftgo, graphgo);
		if (this.eRule.isBadMapping()) {
			this.eRule.undoManagerLastEditDie();
			this.msg = this.eRule.getMsg();
			
			javax.swing.JOptionPane
						.showMessageDialog(
								this.applFrame,
								this.msg,
								"Mapping Error", javax.swing.JOptionPane.ERROR_MESSAGE);
				
			return false;
		} 
		this.eRule.undoManagerEndEdit();
		if (this.gragraEditor != null) 
			this.gragraEditor.updateUndoButton();
		if (this.graphEditor != null)
			this.graphEditor.getGraph().update();
		
		return true;
	}

	public boolean removeMatchMapping(EdGraphObject go, boolean left) {
		if (go == null || this.eRule.getMatch() == null)
			return false;
		
		if (left && this.graphEditor != null) {
			EdGraphObject ggo = this.graphEditor.getGraph().findGraphObject(
						this.eRule.getMatch().getImage(go.getBasisObject()));
			if (ggo != null) {
				this.eRule.addDeletedMatchMappingToUndo(go, ggo);
				this.eRule.removeMatchMapping(go);
				this.eRule.undoManagerEndEdit();
				if (this.gragraEditor != null)
					this.gragraEditor.updateUndoButton();
					
				this.graphEditor.getGraph().update();
				return true;
			}
			return false;
		} 
			
		boolean res = false;
		Enumeration<GraphObject> inverse = this.eRule.getMatch().getInverseImage(
						go.getBasisObject());
		while (inverse.hasMoreElements()) {
			GraphObject o = inverse.nextElement();
			EdGraphObject lgo = this.eRule.getLeft().findGraphObject(o);
			res = removeMatchMapping(lgo, true) || res;
		}
		return res;
	}

	/** **** End of the implementing interface MouseMotionListener ****** */

	/**
	 * Enables or disables synchronized moving of mapped nodes and edges of the
	 * current rule.
	 */
	public void enableSynchronMoveOfMappedObjects(boolean b) {
		this.synchrMoveOfMapObjs = b;
	}

	public boolean isSynchronMoveOfMappedObjectsEnabled() {
		return this.synchrMoveOfMapObjs;
	}

	public boolean isLeftDragging(){
		return this.draggingL;
	}
	
	public void setLeftDragging(boolean b){
		this.draggingL = b;
	}
	
	public boolean isRightDragging(){
		return this.draggingR;
	}
	
	public void setRightDragging(boolean b){
		this.draggingR = b;
	}
	
	public boolean isLeftCondDragging(){
		return this.draggingC;
	}
	
	public void setLeftCondDragging(boolean b){
		this.draggingC = b;
	}
	
	public Vector<EdGraphObject> getImages(EdGraph imageGraph,
			OrdinaryMorphism morph, Vector<EdGraphObject> objs) {
		Vector<EdGraphObject> res = new Vector<EdGraphObject>(5);
		for (int i = 0; i < objs.size(); i++) {
			EdGraphObject go = objs.get(i);
			GraphObject img = morph.getImage(go.getBasisObject());
			if (img != null) {
				EdGraphObject obj = imageGraph.findGraphObject(img);
				if (obj == null)
					continue;

				if (img instanceof Node) {
					res.add(obj);

					Iterator<Arc> e = ((Node) img).getIncomingArcsSet().iterator();
					while (e.hasNext()) {
						Arc a = e.next();
						if (morph.getInverseImage(a).hasMoreElements()) {
							EdArc ea = imageGraph.findArc(a);
							if (ea != null && res.contains(ea.getSource())
									&& !res.contains(ea))
								res.add(ea);
						}
					}
					e = ((Node) img).getOutgoingArcsSet().iterator();
					while (e.hasNext()) {
						Arc a = e.next();
						if (morph.getInverseImage(a).hasMoreElements()) {
							EdArc ea = imageGraph.findArc(a);
							if (ea != null && res.contains(ea.getTarget())
									&& !res.contains(ea))
								res.add(ea);
						}
					}

				} else if (!res.contains(obj)) {
					// System.out.println(obj);
					res.add(obj);
				}
			}
		}
		return res;
	}

	public Vector<EdGraphObject> getInverseImages(EdGraph imageGraph,
			OrdinaryMorphism morph, Vector<EdGraphObject> objs) {
		Vector<EdGraphObject> res = new Vector<EdGraphObject>(5);
		for (int i = 0; i < objs.size(); i++) {
			EdGraphObject go = objs.get(i);
			Enumeration<GraphObject> en = morph.getInverseImage(go.getBasisObject());
			while (en.hasMoreElements()) {
				GraphObject img = en.nextElement();
				if (img != null) {
					EdGraphObject obj = imageGraph.findGraphObject(img);
					if (obj == null)
						continue;

					if (img instanceof Node) {
						res.add(obj);

						Iterator<Arc> e = ((Node) img).getIncomingArcsSet().iterator();
						while (e.hasNext()) {
							Arc a = e.next();
							if (morph.getImage(a) != null) {
								EdArc ea = imageGraph.findArc(a);
								if (ea != null && res.contains(ea.getSource())
										&& !res.contains(ea))
									res.add(ea);
							}
						}
						e = ((Node) img).getOutgoingArcsSet().iterator();
						while (e.hasNext()) {
							Arc a = e.next();
							if (morph.getImage(a) != null) {
								EdArc ea = imageGraph.findArc(a);
								if (ea != null && res.contains(ea.getTarget())
										&& !res.contains(ea))
									res.add(ea);
							}
						}

					} else if (!res.contains(obj)) {
						// System.out.println(obj);
						res.add(obj);
					}
				}
			}
		}
		return res;
	}

	/*
	 * ********************************** KeyListener methods
	 * **********************************
	 */

	public void keyPressed(KeyEvent e) {
		if (this.leftPanel.getEditMode() == EditorConstants.VIEW)
			return;
		Object source = e.getSource();
		int keyCode = e.getKeyCode();
		// System.out.println(">>>RuleEditor keyPressed "+keyCode);
		if (source == this.leftPanel.getCanvas()) {
			// || source == this.rightPanel.getCanvas()
			// || source == leftCondPanel.getCanvas()) {
			switch (keyCode) {
			case KeyEvent.VK_DELETE:
				System.out.println("KeyEvent.VK_DELETE");
				removeProc();
				break;
			default:
				break;
			}
		}
	}

	public void keyReleased(KeyEvent e) {
		if (this.leftPanel.getEditMode() == EditorConstants.VIEW)
			return;

		Object source = e.getSource();
		int keyCode = e.getKeyCode();
		if (source == this.leftPanel.getCanvas()) {
			switch (keyCode) {
			case KeyEvent.VK_DELETE: // 127:
				if (this.leftPanel.getEditMode() == EditorConstants.REMOVE_RULE) {
					this.leftPanel.setEditMode(EditorConstants.INTERACT_RULE);
					this.leftPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
					if (this.applFrame != null)
						this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
				} else if (this.leftPanel.getEditMode() == EditorConstants.REMOVE_NAC) {
					this.leftPanel.setEditMode(EditorConstants.INTERACT_NAC);
					this.leftPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
					if (this.applFrame != null)
						this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
				} else if (this.leftPanel.getEditMode() == EditorConstants.REMOVE_PAC) {
					this.leftPanel.setEditMode(EditorConstants.INTERACT_PAC);
					this.leftPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
					if (this.applFrame != null)
						this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
				} else if (this.leftPanel.getEditMode() == EditorConstants.REMOVE_AC) {
					this.leftPanel.setEditMode(EditorConstants.INTERACT_AC);
					this.leftPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
					if (this.applFrame != null)
						this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
				} else if (this.leftPanel.getEditMode() == EditorConstants.REMOVE_MATCH) {
					this.leftPanel.setEditMode(EditorConstants.INTERACT_MATCH);
					this.leftPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
					if (this.applFrame != null)
						this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
					// this.graphEditor.setEditCursor(new
					// Cursor(Cursor.HAND_CURSOR));
				} else if (this.leftPanel.getEditMode() == EditorConstants.REMOVE_MAP) {
					this.leftPanel.setEditMode(EditorConstants.MAP);
					this.leftPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
					if (this.applFrame != null)
						this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
				} else if (this.leftPanel.getEditMode() == EditorConstants.REMOVE_MAPSEL) {
					this.leftPanel.setEditMode(EditorConstants.MAPSEL);
					this.leftPanel.setEditCursor(new Cursor(Cursor.HAND_CURSOR));
					if (this.applFrame != null)
						this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
				break;
			default:
				break;
			}
		}
	}

	public void keyTyped(KeyEvent e) {
	}

	/* End of KeyListener methods */

	/** Gets my minimum dimension */
	public Dimension getMinimumSize() {
		return new Dimension(100, 100);
	}

	/** Gets my preferred dimension */
	public Dimension getPreferredSize() {
		return new Dimension(500, 200);
	}

	public String getTitle() {
		return this.title.getText();
	}

	/** Sets my title */
	public void setTitle(String str) {
		this.title.setText("  " + str);
	}

	/**
	 * Sets my title. The String str1 specifies the first part of title (may be
	 * graph name or empty), the String str2 specifies the second part of title
	 * (may be gragra name or empty)
	 */
	public void setRuleTitle(String str1, String str2) {
		if (!str1.equals("") && !str2.equals("")) {
			this.ruleName = str1;
			this.gragraName = str2;
			this.title.setText(this.titleKind + this.ruleName + "  of  " + this.gragraName);
		} else if (!str1.equals("") && str2.equals("")) {
			this.ruleName = str1;
			this.title.setText(this.titleKind + this.ruleName);
		} else if (str1.equals("") && !str2.equals("")) {
			this.gragraName = str2;
			this.title.setText(this.titleKind + this.gragraName);
		} else
			this.title.setText(this.titleKind);
	}

	/**
	 * Sets my title. The String str1 specifies the first part of title (may be
	 * conclusion name or empty), the String str2 specifies the second part of
	 * title (may be atomic name or empty)
	 */
	public void setAtomicTitle(String str1, String str2) {
		if (!str1.equals("") && !str2.equals("")) {
			this.conclusionName = str1;
			this.atomicName = str2;
			this.title.setText(this.titleKind + this.atomicName + "  ->  " + this.conclusionName);
		} else if (!str1.equals("") && str2.equals("")) {
			this.conclusionName = str1;
			this.title.setText(this.titleKind + this.conclusionName);
		} else if (str1.equals("") && !str2.equals("")) {
			this.atomicName = str2;
			this.title.setText(this.titleKind + this.atomicName);
		} else
			this.title.setText(this.titleKind);
	}

	/** Sets the title of my NAC */
	public void setNACTitle(String str) {
		this.titleAC.setText(this.titleKindAC + str);
	}

	/** Sets the title of my PAC */
	public void setPACTitle(String str) {
		this.titleAC.setText(this.titleKindAC + str);
	}

	public void setLeftApplCondTitle(String str) {
		this.titleAC.setText(this.titleKindAC + str);
	}
	
	/** Returns my parent editor */
	public GraGraEditor getGraGraEditor() {
		return this.gragraEditor;
	}

	public GraphEditor getGraphEditor() {
		return this.graphEditor;
	}
	
	/** Returns my left panel */
	public GraphPanel getLeftPanel() {
		return this.leftPanel;
	}

	/** Returns my right panel */
	public GraphPanel getRightPanel() {
		return this.rightPanel;
	}

	public boolean isEditPopupMenuShown() {
		return this.isEditPopupMenu;
	}
	
	public EditPopupMenu getEditPopupMenu() {
		return this.editPopupMenu;
	}
	
	public boolean isEditSelPopupMenuShown() {
		return this.isEditSelPopupMenu;
	}
	
	public EditSelPopupMenu getEditSelPopupMenu() {
		return this.editSelPopupMenu;
	}
	
	public void allowToShowPopupMenu(boolean b) {
		this.doNotShowPopupMenu = !b;
	}
	
	public boolean isPopupMenuAllowed() {
		return !this.doNotShowPopupMenu;
	}
	
	public GraphPanel getLeftCondPanel() {
		return this.leftCondPanel;
	}
	
	/** Returns my NAC panel */
	public GraphPanel getNACPanel() {
		return this.leftCondPanel;
	}

	/** Returns my PAC panel */
	public GraphPanel getPACPanel() {
		return this.leftCondPanel;
	}

	/** Returns my active panel */
	public GraphPanel getActivePanel() {
		return this.activePanel;
	}
	
	public GraphPanel getPanelOf(EdGraph g) {
		if (this.leftPanel.getGraph() == g)
			return this.leftPanel;
		if (this.rightPanel.getGraph() == g)
			return this.rightPanel;
		if (this.leftCondPanel.getGraph() == g)
			return this.leftCondPanel;
		return null;
	}

	public GraphPanel getPanelOfLocationOnScreen(Point p) {
		if (p == null)
			return null;
		Point p2 = new Point(20, 20);
		p2.x = p.x - this.leftPanel.getLocationOnScreen().x;
		p2.y = p.y - this.leftPanel.getLocationOnScreen().y;
		if (this.leftPanel.contains(p2))
			return this.leftPanel;
		p2.x = p.x - this.rightPanel.getLocationOnScreen().x;
		p2.y = p.y - this.rightPanel.getLocationOnScreen().y;
		if (this.rightPanel.contains(p2))
			return this.rightPanel;
		p2.x = p.x - this.leftCondPanel.getLocationOnScreen().x;
		p2.y = p.y - this.leftCondPanel.getLocationOnScreen().y;
		if (this.leftCondPanel.contains(p2))
			return this.leftCondPanel;
		return null;
	}
	
	/** Returns my mode */
	public int getEditMode() {
		return this.leftPanel.getEditMode();
	}

	/** Returns the gragra of my rule */
	public EdGraGra getGraGra() {
		return this.eGra;
	}

	/** Returns my rule */
	public EdRule getRule() {
		return this.eRule;
	}

	/** Returns an atomic constraint of the gragra */
	public EdAtomic getAtomic() {
		if (this.eRule != null && this.eRule instanceof EdAtomic)
			return (EdAtomic) this.eRule;
		return null;
	}

	/** Returns a NAC of my rule */
	public EdNAC getNAC() {
		return this.eNAC;
	}

	/** Returns a PAC of my rule */
	public EdPAC getPAC() {
		return this.ePAC;
	}

	/** Returns a nested AC of my rule */
	public EdNestedApplCond getNestedAC() {
		return this.eGAC;
	}
	
	/** Returns my current message */
	public String getMsg() {
		return this.msg;
	}

	public void setMsg(String s) {
		this.msg = s;
	}

	/** Tests myself if a rule is in editor */
	public boolean hasRule() {
		if (this.eRule != null)
			return true;
		
		return false;
	}

	public EdGraphObject setLeftGraphObject(EdGraphObject go) {
		this.leftObj = go;
		return this.leftObj;
	}
	
	public EdGraphObject setRightGraphObject(EdGraphObject go) {
		this.rightObj = go;
		return this.rightObj;
	}
	
	public EdGraphObject setLeftCondGraphObject(EdGraphObject go) {
		this.leftCondObj = go;
		return this.leftCondObj;
	}
	
	public EdGraphObject setHostGraphObject(EdGraphObject go) {
		this.graphObj = go;
		return this.graphObj;
	}
	
	public boolean isObjMapping() {
		return this.mapping;
	}

	public void setObjMapping(boolean b) {
		this.mapping = b;
	}

	/** Sets a graph editor */
	public void setGraphEditor(GraphEditor gEditor) {
		this.graphEditor = gEditor;
	}

	/** Sets my rule specified by the EdRule er */
	public void setRule(EdRule er) {
		if (this.eRule != null) {
			if (this.dividerLocationSet.get(this.eRule) == null) {
				this.ruleDividerLocation = this.ruleSplitPane.getDividerLocation();
				this.dividerLocationSet.put(this.eRule, Integer.valueOf(this.ruleDividerLocation));
			}
			else {
				this.ruleDividerLocation = ((Integer)this.dividerLocationSet.get(this.eRule)).intValue();
			}
		}
		this.eRule = er;
		this.titleKind = " ";
		
		if (this.eRule == null) {
			this.ruleDividerLocation = this.ruleSplitPane.getDividerLocation();
			setTitle("    ");
			setBorder(this.leftPanel.canvas, "  LHS  ");
			setBorder(this.rightPanel.canvas, "  RHS  ");
			this.leftPanel.setGraph(null);			
			this.rightPanel.setGraph(null);
			setNAC(null);
			setPAC(null);
			setNestedAC(null);
			this.eGra = null;
			if (this.exportJPEGButton != null)
				this.exportJPEGButton.setEnabled(false);
			return;
		}

		this.eGra = this.eRule.getGraGra();
		
		if (this.eRule.getBasisRule() instanceof RuleScheme) {
			this.eRule = ((EdRuleScheme) this.eRule).getKernelRule();
		} 
		makeRuleTitle();
				
		setBorder(this.leftPanel.canvas, "  LHS  ");
		setBorder(this.rightPanel.canvas, "  RHS  ");
		
		this.eRule.updateRule();
		this.leftPanel.setGraph(this.eRule.getLeft(), true);
		this.rightPanel.setGraph(this.eRule.getRight(), true);
		
		this.showRightPanel();
		
		if (this.attrEditor != null) {
			this.attrEditor.setContext(this.eRule.getBasisRule().getAttrContext());
			
		}
		if (this.dividerLocationSet.get(this.eRule) != null) {
			this.ruleSplitPane.setDividerLocation((this.dividerLocationSet.get(this.eRule))
					.intValue());
		}
		else {
			this.ruleSplitPane.setDividerLocation(this.ruleDividerLocation);
		}
		if (this.exportJPEGButton != null
				&& this.exportJPEG != null) {
			this.exportJPEGButton.setEnabled(true);
		}
	}

	private void setBorder(JPanel p, String txt) {
		p.setBorder(new TitledBorder(
				null, txt, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, 
				new Color(204,204,204)));
//				Color.black));
	}
	
	public void adjustLeftRightBorderTitle() {
		setBorder(this.leftPanel.canvas, "  LHS  ");
		setBorder(this.rightPanel.canvas, "  RHS  ");
	
	}
	
	/** Resets my rule*/
	public void resetRule() {
		if (this.eRule != null) {
			this.showRightPanel();
			
			setNAC(null);
			setPAC(null);
			setNestedAC(null);
				
			if (this.eRule.getBasisRule() instanceof RuleScheme) {
				this.eRule = ((EdRuleScheme) this.eRule).getKernelRule();
			} 
			makeRuleTitle();
					
			setBorder(this.leftPanel.canvas, "  LHS  ");
			setBorder(this.rightPanel.canvas, "  RHS  ");
			
			this.eRule.updateRule();
			this.leftPanel.setGraph(this.eRule.getLeft(), true);
			this.rightPanel.setGraph(this.eRule.getRight(), true);
	
			if (this.attrEditor != null) {
				this.attrEditor.setContext(this.eRule.getBasisRule().getAttrContext());
			}
			if (this.dividerLocationSet.get(this.eRule) != null) {
				this.ruleDividerLocation = ((Integer)this.dividerLocationSet.get(this.eRule)).intValue();
				this.ruleSplitPane.setDividerLocation(this.ruleDividerLocation);
			}
			else {
				this.ruleDividerLocation = this.ruleSplitPane.getDividerLocation();
				this.ruleSplitPane.setDividerLocation(this.ruleDividerLocation);
				this.dividerLocationSet.put(this.eRule, new Integer(this.ruleDividerLocation));
			}
			if (this.exportJPEGButton != null
					&& this.exportJPEG != null) {
				this.exportJPEGButton.setEnabled(true);
			}
		}
	}
	
	private void makeRuleTitle() {
		String titleStr = "";
		if (this.eRule.getBasisRule() instanceof RuleScheme) {
			titleStr = ((RuleScheme)this.eRule.getBasisRule()).getName();
		} else if (this.eRule.getBasisRule() instanceof KernelRule) {
			titleStr = this.eRule.getBasisRule().getName()
					+"  of  "+
					((KernelRule)this.eRule.getBasisRule()).getRuleScheme().getName();
		} else if (this.eRule.getBasisRule() instanceof MultiRule) {
			titleStr = this.eRule.getBasisRule().getName()
						+"  of  "+
						((MultiRule)this.eRule.getBasisRule()).getRuleScheme().getName();
		} else if (this.eRule.getBasisRule() instanceof AmalgamatedRule
				&& ((AmalgamatedRule)this.eRule.getBasisRule()).getRuleScheme() != null) {
			titleStr = this.eRule.getBasisRule().getName()
						+"  of  "+
						((AmalgamatedRule)this.eRule.getBasisRule()).getRuleScheme().getName();
		} 
		else {
			titleStr = this.eRule.getBasisRule().getName();
		}
			
		if (this.eGra != null) {
			setRuleTitle(titleStr, this.eGra.getName());
		}
		else {
			setRuleTitle(titleStr, "");
		}
	}
	
//	public int getRuleDividerLocation() {
//		return this.ruleDividerLocation;
//	}

//	public int getNACDividerLocation() {
//		return nacDividerLocation;
//	}
//
//	public int getPACDividerLocation() {
//		return pacDividerLocation;
//	}

	public void setDividerLocation(int indx, int i) {
		if (indx == 0) {
			this.splitPane.setDividerLocation(i);
		} else if (indx == 1) {
			this.ruleSplitPane.setDividerLocation(i);
		}
	}

	/** Sets atomic constraint */
	public void setAtomic(EdAtomic a) {
		if (this.eRule != null) {
			this.ruleDividerLocation = this.ruleSplitPane.getDividerLocation();
			this.dividerLocationSet.put(this.eRule, Integer.valueOf(this.ruleDividerLocation));
		}
		this.eRule = a;
		this.titleKind = "  ";
		
		if (this.eRule == null) {
			this.ruleDividerLocation = this.ruleSplitPane.getDividerLocation();
			setTitle("    ");
			setBorder(this.leftPanel.canvas, "  LHS  ");
			setBorder(this.rightPanel.canvas, "  RHS  ");
			this.leftPanel.setGraph(null);			
			this.rightPanel.setGraph(null);
			setNAC(null);
			setPAC(null);
			setNestedAC(null);
			this.eGra = null;
			if (this.exportJPEGButton != null)
				this.exportJPEGButton.setEnabled(false);
			
			return;
		}
		
		setNAC(null);
		setPAC(null);
		setNestedAC(null);
		
		this.eGra = this.eRule.getGraGra();
		
		String s = a.getBasisAtomic().getAtomicName();
		setAtomicTitle(this.eRule.getMorphism().getName(), s);

		setBorder(this.leftPanel.canvas, "  P  ");
		setBorder(this.rightPanel.canvas, "  C  ");
		
		this.leftPanel.setGraph(this.eRule.getLeft());
		this.rightPanel.setGraph(this.eRule.getRight());

		if (this.attrEditor != null) {
			this.attrEditor.setContext(a.getBasisAtomic().getAttrContext());
		}

		if (this.dividerLocationSet.get(this.eRule) != null)
			this.ruleSplitPane.setDividerLocation(this.dividerLocationSet.get(this.eRule)
						.intValue());
		else
			this.ruleSplitPane.setDividerLocation(this.ruleDividerLocation);

		if (this.exportJPEGButton != null
				&& this.exportJPEG != null)
			this.exportJPEGButton.setEnabled(true); 
	}

	/** Sets my NAC specified by the EdNAC enac */
	public void setNAC(EdNAC enac) {
		this.showRightPanel();
		
		if (this.eNAC != null) {
			this.acDividerLocation = this.splitPane.getDividerLocation();			
			this.dividerLocationSet.put(this.eNAC, Integer.valueOf(this.acDividerLocation));			
		} else if (this.ePAC != null) {	
			this.dividerLocationSet.put(this.ePAC, Integer.valueOf(this.splitPane.getDividerLocation()));
		} else if (this.eGAC != null) {		
			this.dividerLocationSet.put(this.eGAC, Integer.valueOf(this.splitPane.getDividerLocation()));
		}
		
		this.eNAC = enac;
		this.titleKindAC = "  ";
		
		if (this.eNAC == null) {
			this.leftCondPanel.setGraph(null);
			setNACTitle("");
			hideLeftApplCond();
			return;
		}

		this.ePAC = null;
		this.eGAC = null;
		
		makeRuleTitle();
		setBorder(this.leftPanel.canvas, "  LHS  ");
		setBorder(this.rightPanel.canvas, "  RHS  ");

		setBorder(this.leftCondPanel.canvas, "  NAC  ");
		setNACTitle(this.eNAC.getBasisGraph().getName());
		this.eRule.updateRule();
		this.eRule.updateNAC(this.eNAC);
		this.leftCondPanel.setGraph(this.eNAC, true);
		
		setDividerLocationOfLeftApplCond(this.eNAC);
		if (this.dividerLocationSet.get(this.eRule) != null) {
			this.ruleDividerLocation = (this.dividerLocationSet.get(this.eRule)).intValue();
			this.ruleSplitPane.setDividerLocation((this.dividerLocationSet.get(this.eRule))
					.intValue());
		}
		else {
			this.ruleSplitPane.setDividerLocation(this.ruleDividerLocation);
		}
//		this.showRightPanel();
	}


	/** Sets my PAC specified by the EdPAC epac */
	public void setPAC(EdPAC epac) {	
		this.showRightPanel();
		
		if (this.ePAC != null) {
			this.acDividerLocation = this.splitPane.getDividerLocation();
			this.dividerLocationSet.put(this.ePAC, Integer.valueOf(this.acDividerLocation));
		} else if (this.eNAC != null) {
			this.dividerLocationSet.put(this.eNAC, Integer.valueOf(this.splitPane.getDividerLocation()));
		}  else if (this.eGAC != null) {	
			this.dividerLocationSet.put(this.eGAC, Integer.valueOf(this.splitPane.getDividerLocation()));
		}
		
		this.ePAC = epac;
		this.titleKindAC = "  ";
		
		if (this.ePAC == null) {
			this.leftCondPanel.setGraph(null);
			this.leftCondPanel.updateGraphics();
			setPACTitle("");
			hideLeftApplCond();
			return;
		}
		
		this.eNAC = null;
		this.eGAC = null;
		
		makeRuleTitle();
		setBorder(this.leftPanel.canvas, "  LHS  ");
		setBorder(this.rightPanel.canvas, "  RHS  ");

		setBorder(this.leftCondPanel.canvas, "  PAC  ");
		setPACTitle(this.ePAC.getBasisGraph().getName());
		this.eRule.updateRule();
		this.eRule.updatePAC(this.ePAC);
		this.leftCondPanel.setGraph(this.ePAC, true);
		
		setDividerLocationOfLeftApplCond(this.ePAC);
		if (this.dividerLocationSet.get(this.eRule) != null) {
			this.ruleDividerLocation = (this.dividerLocationSet.get(this.eRule)).intValue();
			this.ruleSplitPane.setDividerLocation((this.dividerLocationSet.get(this.eRule))
					.intValue());
		}
		else {
			this.ruleSplitPane.setDividerLocation(this.ruleDividerLocation);
		}
//		this.showRightPanel();
	}

	private void setDividerLocationOfLeftApplCond(EdGraph ac) {
		if (this.splitPane.getLeftComponent() == null) {
			this.splitPane.setDividerSize(10);
			this.splitPane.setLeftComponent(this.leftCondPanel);
		}
		if (this.dividerLocationSet.get(ac) != null) {
			this.acDividerLocation = this.dividerLocationSet.get(ac).intValue();
			if (this.acDividerLocation < 10)
				this.acDividerLocation = 300;
			this.splitPane.setDividerLocation(this.acDividerLocation);
		} else {
			this.acDividerLocation = 300;
			this.splitPane.setDividerLocation(this.acDividerLocation);
			this.dividerLocationSet.put(ac, Integer.valueOf(this.acDividerLocation));
		}
	}
	
	/** Sets my nested AC specified by the EdNestedApplCond ac */
	public void setNestedAC(EdNestedApplCond ac) {
		
		if (this.eGAC != null) {
			this.acDividerLocation = this.splitPane.getDividerLocation();
			this.dividerLocationSet.put(this.eGAC, Integer.valueOf(this.acDividerLocation));
		} else if (this.eNAC != null) {
			this.dividerLocationSet.put(this.eNAC, Integer.valueOf(this.splitPane.getDividerLocation()));
		}
		else if (this.ePAC != null) {
			this.dividerLocationSet.put(this.ePAC, Integer.valueOf(this.splitPane.getDividerLocation()));
		}
		
		this.eGAC = ac;
		this.titleKindAC = "  ";
		
		if (this.eGAC == null) {
			this.leftCondPanel.setGraph(null);
			this.leftCondPanel.updateGraphics();
			setLeftApplCondTitle("");
			hideLeftApplCond();
			return;
		}
	
		this.eNAC = null;
		this.ePAC = null;

		setBorder(this.leftCondPanel.canvas, "  GAC  ");
		this.updateNestedAC(this.eGAC);	
		if (this.eGAC.getParent() == null) {	
			setBorder(this.leftPanel.canvas, "  LHS  ");
			this.title.setText(" <-  LHS  of  "+this.eRule.getName());
			this.eRule.updateRule();
			this.leftPanel.setGraph(this.eRule.getLeft());
		} else {
			EdNestedApplCond parAC = this.eGAC.getParent();
			setBorder(this.leftPanel.canvas, "  GAC  ");
			this.title.setText(" <-  "+parAC.getName());
			this.eRule.updateNestedAC(parAC);
			this.leftPanel.setGraph(parAC);
		} 
		
		setLeftApplCondTitle(this.eGAC.getBasisGraph().getName());
		this.eRule.updateNestedAC(this.eGAC);
		this.leftCondPanel.setGraph(this.eGAC, true);		
		
		setDividerLocationOfLeftApplCond(this.eGAC);
		
		if (this.eGAC.getParent() != null)
			this.hideRightPanel();
		else if (!this.isRightPanelVisible())
			this.showRightPanel();
	}
	
	public void hideLeftApplCond() {
		this.splitPane.setLeftComponent(null);
	}
	
	@SuppressWarnings("unused")
	private void hideLeftPanel() {
		this.ruleSplitPane.setLeftComponent(null);		
	}
	
	protected void hideRightPanel() {
//		this.ruleSplitPane.setDividerLocation(this.ruleSplitPane.getWidth());
		
		this.ruleSplitPane.setRightComponent(null);
	}
	
	@SuppressWarnings("unused")
	private void showLeftPanel() {
		if (this.ruleSplitPane.getLeftComponent() == null) {
			if (this.dividerLocationSet.get(this.eRule) != null)
				this.ruleDividerLocation = this.dividerLocationSet.get(this.eRule).intValue();
			this.ruleSplitPane.setLeftComponent(this.leftPanel);
			this.ruleSplitPane.setDividerLocation(this.ruleDividerLocation);
		}	
	}
	
	protected void showRightPanel() {
		if (this.ruleSplitPane.getRightComponent() == null && this.eRule != null) {
			if (this.dividerLocationSet.get(this.eRule) != null)
				this.ruleDividerLocation = this.dividerLocationSet.get(this.eRule).intValue();
			else
				this.ruleDividerLocation = 250;
			this.ruleSplitPane.setRightComponent(this.rightPanel);
			this.ruleSplitPane.setDividerLocation(this.ruleDividerLocation);			
		}
	}
	
	protected boolean isLeftPanelVisible() {
		return this.ruleSplitPane.getLeftComponent() != null;
	}
	
	protected boolean isRightPanelVisible() {
		return this.ruleSplitPane.getRightComponent() != null;
	}
	
	public void setRuleDividerLocation(int l) {
		this.ruleSplitPane.setDividerLocation(l);
	}

	/** Tests myself if there is only one selected graph object */
	public boolean hasOneSelection() {
		if (this.eRule == null)
			return false;
		if (this.leftPanel.getGraph().hasSelection()
				&& !this.rightPanel.getGraph().hasSelection()
				&& (this.leftCondPanel.getGraph() == null || !this.leftCondPanel
						.getGraph().hasSelection()))
			return true;
		else if (this.rightPanel.getGraph().hasSelection()
				&& !this.leftPanel.getGraph().hasSelection()
				&& (this.leftCondPanel.getGraph() == null || !this.leftCondPanel
						.getGraph().hasSelection()))
			return true;
		else if (this.leftCondPanel.getGraph() != null
				&& this.leftCondPanel.getGraph().hasSelection()
				&& !this.leftPanel.getGraph().hasSelection()
				&& !this.rightPanel.getGraph().hasSelection())
			return true;
		else
			return false;
	}

	/** Tests myself if some graph object are selected */
	public boolean hasSelection() {
		return (this.leftPanel.hasSelection()
						|| this.rightPanel.hasSelection() 
						|| (this.leftCondPanel.hasSelection()))? true: false;
	}

	/** Tests myself if some graph object are selected */
	public boolean hasSelection(GraphPanel gPanel) {
		return gPanel.hasSelection();
	}

	public void updateNestedAC(final EdNestedApplCond ac) {
		if (ac != null) {
			if (ac.getParent() == null) {
				this.eRule.updateRule();
				this.eRule.updateNestedAC(ac);
			} else {
				this.eRule.updateNestedAC(ac.getParent());
				this.eRule.updateNestedAC(ac);
			}
		}
	}
	
	
	/** Updates graphics of my left, right, nac graph panels */
	public void updateGraphics() {
		synchronized(this) {
		this.leftPanel.updateGraphics();
		this.rightPanel.updateGraphics();
		this.leftCondPanel.updateGraphics();
		}
	}

	/** Updates graphics of my left, right, nac graph panels */
	public void updateGraphics(boolean graphDimensionCheck) {
		synchronized(this) {
		this.leftPanel.updateGraphics(graphDimensionCheck);
		this.rightPanel.updateGraphics(graphDimensionCheck);
		this.leftCondPanel.updateGraphics(graphDimensionCheck);
		}
	}


	/** Clears my graph panels. Sets my rule at null. */
	public void clear() {
		setRule(null);
		updateGraphics();
	}

	/** Clears my NAC graph panel.*/
	public void clearNAC() {
		setNAC(null);
		this.leftCondPanel.updateGraphics();
		updateGraphics();
	}

	/** Clears my PAC graph panel.*/
	public void clearPAC() {
		setPAC(null);
		this.leftCondPanel.updateGraphics();
		updateGraphics();
	}

	/** Clears my (Nested) Application Condition graph panel.*/
	public void clearNestedAC() {
		setNestedAC(null);
		this.leftCondPanel.updateGraphics();
		updateGraphics();
	}
	
	/** Sets my attribute editor */
	public void setAttrEditor(AttrTopEditor attrEditor) {
		this.attrEditor = attrEditor;
	}

	/** Sets my mode popupmenu */
	public void setModePopupMenu(ModePopupMenu pm) {
		this.modePopupMenu = pm;
	}

	/** Setes my edit popupmenu */
	public void setEditPopupMenu(EditPopupMenu pm) {
		this.editPopupMenu = pm;
	}

	/** Sets my select popupmenu */
	public void setEditSelPopupMenu(EditSelPopupMenu pm) {
		this.editSelPopupMenu = pm;
	}

	/* Draws graphic of the graphobject go in the panel p */
/*
	private void drawGraphic(EdGraphObject go, GraphPanel p, boolean map,
			boolean erase) {
		if (map) {
			Vector<EdGraphObject> v = p.getGraph().getChangedGraphObjects();
			// System.out.println("ChangedGraphObjects: "+v.size());
			for (int i = 0; i < v.size(); i++) {
				EdGraphObject o = v.elementAt(i);
				if (erase)
					eraseGraphic(o, p);
				drawGraphic(o, p);
			}
		} else if (go != null)
			drawGraphic(go, p);
	}
*/
	
	/* Draws graphic of the graphobject go in the panel p */
/*
	private void drawGraphic(EdGraphObject go, GraphPanel p) {
		if (go.isNode()) {
			p.getGraph().drawNode(p.getCanvas().getGraphics(), (EdNode) go);
			Vector<EdArc> v = p.getGraph().getIncomingArcs((EdNode) go);
			for (int i = 0; i < v.size(); i++) {
				v.elementAt(i).drawText(p.getCanvas().getGraphics(),
						p.getCanvas().getScale());
			}
			v = p.getGraph().getOutgoingArcs((EdNode) go);
			for (int i = 0; i < v.size(); i++) {
				v.elementAt(i).drawText(p.getCanvas().getGraphics(),
						p.getCanvas().getScale());
			}
		} else {
			((EdArc) go).drawText(p.getCanvas().getGraphics(), p.getCanvas()
					.getScale());
		}
	}
*/
	
	/* Erases graphic of the graphobject go in the panel p */
	/*
	private void eraseGraphic(EdGraphObject go, GraphPanel p) {
		if (go.isNode()) {
			((EdNode) go).eraseGraphic(p.getCanvas().getGraphics());
			Vector<EdArc> v = p.getGraph().getIncomingArcs((EdNode) go);
			for (int i = 0; i < v.size(); i++) {
				v.elementAt(i).eraseText(p.getCanvas().getGraphics());
			}
			v = p.getGraph().getOutgoingArcs((EdNode) go);
			for (int i = 0; i < v.size(); i++) {
				v.elementAt(i).eraseText(p.getCanvas().getGraphics());
			}
		} else
			((EdArc) go).eraseText(p.getCanvas().getGraphics());
	}
*/
	
	/*
	 * ************************************* Mode menu procedures
	 * *************************************
	 */

	/** Sets my current mode. */
	public void setEditMode(int mode) {

		handleMouseListenerFromGraphEditor(mode);
		if (mode != EditorConstants.INTERACT_MATCH)
			resetEditModeAfterInteractMatch();

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
			copyModeProc();
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
		case EditorConstants.MAPSEL:
			mapselModeProc();
			break;
		case EditorConstants.UNMAPSEL:
			unmapselModeProc();
			break;
		case EditorConstants.VIEW:
			viewModeProc();
			break;
		default:
			break;
		}
	}

	/** Returns my current mode */
	public int getMode() {
		return this.leftPanel.getEditMode();
	}

	/** Return my previous mode */
	public int getPreviousMode() {
		return this.leftPanel.getLastEditMode();
	}

	/** Sets cursor specified by the Cursor cur */
	public void setEditCursor(Cursor cur) {
		this.leftPanel.setEditCursor(cur);
		this.rightPanel.setEditCursor(cur);
		this.leftCondPanel.setEditCursor(cur);
	}

	/*
	 * public void setInteractMatchMode(GraphEditor gEditor) { // wird in
	 * GraGraTransform benutzt graphEditor = gEditor;
	 * setEditMode(EditorConstants.INTERACT_MATCH); //matchDefModeProc(); }
	 */
	public void resetAfterInteractMatch() {
		// wird in GraGraTransform benutzt
		setEditMode(this.leftPanel.getLastEditMode());
	}

	public void setAttributeVisible(boolean vis){
		this.leftPanel.getCanvas().setAttributeVisible(vis);
		this.rightPanel.getCanvas().setAttributeVisible(vis);
		this.leftCondPanel.getCanvas().setAttributeVisible(vis);
		this.updateGraphics();
	}
	
	public void unsetDragging() {
		this.draggingL = false;
		this.draggingR = false;
		this.draggingC = false;
	}
	
	private void resetEditModeAfterInteractMatch() {
		if (this.eRule == null
				|| this.leftPanel.getEditMode() != EditorConstants.INTERACT_MATCH)
			return;

		// this.leftPanel.getCanvas().removeKeyListener(this);
		// System.out.println(">>> this.leftPanel removeKeyListener");

		if (this.graphEditor != null) {
			if (this.leftPanel.getLastEditMode() == EditorConstants.VIEW) 
				this.graphEditor.setEditMode(this.leftPanel.getLastEditMode());
		 	else 
				this.graphEditor.setEditMode(this.leftPanel.getLastEditMode());
		}
	}

	private void drawModeProc() {
		setPanelEditMode(EditorConstants.DRAW);
		this.leftPanel.setLastEditMode(EditorConstants.MOVE);
		setEditCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		this.msg = "Click on the background to get a node / on a source node and a target node to get an edge.";
	}

	private void selectModeProc() {
		setPanelEditMode(EditorConstants.SELECT);
		this.leftPanel.setLastEditMode(EditorConstants.MOVE);
		setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
		this.msg = "Click on an object to select it.";
	}

	private void moveModeProc() {
		setPanelEditMode(EditorConstants.MOVE);
		this.leftPanel.setLastEditMode(EditorConstants.MOVE);
		setEditCursor(new Cursor(Cursor.MOVE_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.MOVE_CURSOR));
		this.msg = "Press and drag the button when the cursor points to an object.";
	}

	private void attributesModeProc() {
		setPanelEditMode(EditorConstants.ATTRIBUTES);
		setEditCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		this.msg = "Click on an object to activate the attribute editor.";
	}

	@SuppressWarnings("unused")
	private void ruleDefModeProc() {
		setPanelEditMode(EditorConstants.INTERACT_RULE);
		setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
		this.msg = "Click on a source object and a target object to get a mapping pair.";
		// this.leftPanel.getCanvas().addKeyListener(this);
	}

	@SuppressWarnings("unused")
	private void nacDefModeProc() {
		setPanelEditMode(EditorConstants.INTERACT_NAC);
		setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
		this.msg = "Click on a source object and a target object to get a mapping pair.";
		// this.leftPanel.getCanvas().addKeyListener(this);
	}

	@SuppressWarnings("unused")
	private void pacDefModeProc() {
		setPanelEditMode(EditorConstants.INTERACT_PAC);
		setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
		this.msg = "Click on a source object and a target object to get a mapping pair.";
		// this.leftPanel.getCanvas().addKeyListener(this);
	}
	
	@SuppressWarnings("unused")
	private void acDefModeProc() {
		setPanelEditMode(EditorConstants.INTERACT_AC);
		setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
		this.msg = "Click on a source object and a target object to get a mapping pair.";
		// this.leftPanel.getCanvas().addKeyListener(this);
	}
	
	private void matchDefModeProc() {
		if (this.eRule == null || this.graphEditor == null)
			return;
		
		setLastEditModeBeforMatch(this.leftPanel);
		setPanelEditMode(EditorConstants.INTERACT_MATCH);
		setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
		// this.leftPanel.getCanvas().addKeyListener(this);

		this.graphEditor.setEditMode(EditorConstants.INTERACT_MATCH);
		this.graphEditor.setEditCursor(new Cursor(Cursor.HAND_CURSOR));

		this.leftObj = null;
		this.graphObj = null;
		this.msg = "Click on a source object and a target object to get a mapping pair.";
	}

	private void mapModeProc() {
		setLastEditModeBeforMapping(this.leftPanel);
		setPanelEditMode(EditorConstants.MAP);
		setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
		this.msg = "Click on a source object and a target object to get a mapping.";
		// this.leftPanel.getCanvas().addKeyListener(this);
	}

	private void unmapModeProc() {
		setLastEditModeBeforMapping(this.leftPanel);
		setPanelEditMode(EditorConstants.UNMAP);
		setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
		this.msg = "Click on the source of the mapping to destroy it.";
	}

	private void mapselModeProc() {
		if (this.leftPanel.getEditMode() == EditorConstants.MAPSEL)
			return;

		setLastEditModeBeforMapping(this.leftPanel);
		setPanelEditMode(EditorConstants.MAPSEL);
		setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
		this.msg = "Click on a source object and a target object to get a mapping.";
		// this.leftPanel.getCanvas().addKeyListener(this);
	}

	private void unmapselModeProc() {
		if (this.leftPanel.getEditMode() == EditorConstants.UNMAPSEL)
			return;

		setLastEditModeBeforMapping(this.leftPanel);
		setPanelEditMode(EditorConstants.UNMAPSEL);
		setEditCursor(new Cursor(Cursor.HAND_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.HAND_CURSOR));
		this.msg = "Click on the source object of the mapping to destroy it.";
	}

	private void copyModeProc() {
		if (this.eRule == null)
			return;
		saveLastEditMode();
		setPanelEditMode(EditorConstants.COPY);
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		this.msg = "To place a copy click on the background of the panel.";
	}

	private void pasteModeProc() {
		if (this.eRule == null)
			return;
		setPanelEditMode(EditorConstants.PASTE);
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		this.msg = "To place a copy click on the background of the panel.";
	}
	
	private void saveLastEditMode() {
		this.leftPanel.setLastEditMode(this.leftPanel.getEditMode());
		this.leftPanel.setLastEditCursor(this.leftPanel.getEditCursor());

		this.rightPanel.setLastEditMode(this.rightPanel.getEditMode());
		this.rightPanel.setLastEditCursor(this.rightPanel.getEditCursor());

		if (this.leftCondPanel.getGraph() != null) {
			this.leftCondPanel.setLastEditMode(this.leftCondPanel.getEditMode());
			this.leftCondPanel.setLastEditCursor(this.leftCondPanel.getEditCursor());
		}
	}

	private void viewModeProc() {
		this.leftPanel.setEditMode(EditorConstants.VIEW);
		this.rightPanel.setEditMode(EditorConstants.VIEW);
		this.leftCondPanel.setEditMode(EditorConstants.VIEW);
	}

	/*
	 * ******************************************* Edit menu procedures
	 * *******************************************
	 */

	/** Deletes selected nodes and edges */
	public boolean deleteProc() {
		if ((this.leftPanel.getEditMode() == EditorConstants.VIEW)
				|| (this.rightPanel.getEditMode() == EditorConstants.VIEW))
			return false;
		if (this.eRule == null)
			return false;
		boolean result = false;
		if (hasSelection(this.leftPanel)) {
			// this.leftPanel.getGraph().eraseSelected(this.leftPanel.getCanvas().getGraphics(),this.leftPanel.getCanvas().getScale(),
			// false);
			if (this.getAtomic() != null)
				unmapSelectedGraphObjects(this.leftPanel, "P", true);
			else if (this.getRule() != null)
			unmapSelectedGraphObjects(this.leftPanel, "LHS", true);
			this.leftPanel.deleteSelected();
			result = true;
		}
		if (hasSelection(this.rightPanel)) {
			// this.rightPanel.getGraph().eraseSelected(this.rightPanel.getCanvas().getGraphics(),this.rightPanel.getCanvas().getScale(),
			// false);
			if (this.getAtomic() != null)
				unmapSelectedGraphObjects(this.rightPanel, "C", true);
			else if (this.getRule() != null)
			unmapSelectedGraphObjects(this.rightPanel, "RHS", true);
			this.rightPanel.deleteSelected();
			result = true;
		}
		if (hasSelection(this.leftCondPanel)) {
			// nacPanel.getGraph().eraseSelected(nacPanel.getCanvas().getGraphics(),nacPanel.getCanvas().getScale(),
			// false);
			if (getNAC() != null)
				unmapSelectedGraphObjects(this.leftCondPanel, "NAC", true);
			else if (getPAC() != null)
				unmapSelectedGraphObjects(this.leftCondPanel, "PAC", true);
			else if (getNestedAC() != null)
				unmapSelectedGraphObjects(this.leftCondPanel, "AC", true);
			
			this.leftCondPanel.deleteSelected();
			result = true;
		}
		if (result) {
			this.eRule.update();
			this.leftPanel.updateGraphicsAfterDelete();
			this.rightPanel.updateGraphicsAfterDelete();
			this.leftCondPanel.updateGraphicsAfterDelete();
			if (this.graphEditor != null)
				this.graphEditor.getGraphPanel().updateGraphicsAfterDelete();
		}
		return result;
	}

	protected void unmapSelectedGraphObjects(GraphPanel gp, String kind,
			boolean wantDeleteGraphObject) {
		Vector<EdGraphObject> selObjs = gp.getGraph().getSelectedObjs();
		if (kind.equals("LHS")) {
			for (int i = 0; i < selObjs.size(); i++) {
				EdGraphObject origObj = selObjs.elementAt(i);
				boolean isRuleObj = this.getRule().getLeft() == origObj.getContext();
				if (!wantDeleteGraphObject) {
					if (getNAC() != null)
						this.removeNacMapping(origObj, true);
					else if (getPAC() != null)
						this.removePacMapping(origObj, true);
					else if (this.getNestedAC() != null)
						this.removeNestedACMapping(origObj, true);
					if (getRule().getMatch() != null && isRuleObj)
						this.removeMatchMapping(origObj, true);
					this.removeRuleMapping(origObj, true);
				} else if (isRuleObj) {
					// want to delete origObj, so remove all relevant mappings
					this.removeNacMapping(origObj);
					this.removePacMapping(origObj);
					this.removeNestedACMapping(origObj);
					this.removeMatchMapping(origObj, true);
					this.removeRuleMapping(origObj, true);
				}
			}
		} else if (kind.equals("RHS")) {
			for (int i = 0; i < selObjs.size(); i++) {
				EdGraphObject imageObj = selObjs.elementAt(i);
				this.removeRuleMapping(imageObj, false);
			}
		} else if (kind.equals("NAC")) {
			for (int i = 0; i < selObjs.size(); i++) {
				EdGraphObject imageObj = selObjs.elementAt(i);
				this.removeNacMapping(imageObj, false);
			}
		} else if (kind.equals("PAC")) {
			for (int i = 0; i < selObjs.size(); i++) {
				EdGraphObject imageObj = selObjs.elementAt(i);
				this.removePacMapping(imageObj, false);
			}
		} else if (kind.equals("AC")) {
			for (int i = 0; i < selObjs.size(); i++) {
				EdGraphObject imageObj = selObjs.elementAt(i);
				this.removeNestedACMapping(imageObj, false);
			}
		} else if (kind.equals("P")) {
			for (int i = 0; i < selObjs.size(); i++) {
				this.removeRuleMapping(selObjs.elementAt(i), true);
			}
		} else if (kind.equals("C")) {
			for (int i = 0; i < selObjs.size(); i++) {
				this.removeRuleMapping(selObjs.elementAt(i), false);
			}
		}
	
		if (this.gragraEditor != null)
			this.gragraEditor.updateUndoButton();
	}

	/** Copies selected nodes and edges */
	public void copyProc() {
		if (this.eRule == null)
			return;
		if (!hasSelection()) {
			this.msg = "Cannot copy. There isn't any object selected";
			return;
		}
		saveLastEditMode();
		this.msg = "";
		setEditMode(EditorConstants.COPY);
	}

	/** Selects all nodes and edges */
	public void selectAllProc() {
		if (this.eRule == null)
			return;
		this.leftPanel.selectAll();
		this.rightPanel.selectAll();
		if (this.leftCondPanel.getGraph() != null)
			this.leftCondPanel.selectAll();
	}

	/** Selects nodes of selected node type */
	public void selectNodeTypeProc() {
		if (this.eRule == null)
			return;
		this.leftPanel.selectNodesOfSelectedNodeType();
		this.rightPanel.selectNodesOfSelectedNodeType();
		if (this.leftCondPanel.getGraph() != null)
			this.leftCondPanel.selectNodesOfSelectedNodeType();
	}

	/** Selects edges of selected edge type */
	public void selectArcTypeProc() {
		if (this.eRule == null)
			return;
		this.leftPanel.selectArcsOfSelectedArcType();
		this.rightPanel.selectArcsOfSelectedArcType();
		if (this.leftCondPanel.getGraph() != null)
			this.leftCondPanel.selectArcsOfSelectedArcType();
	}

	/** Deselects all nodes and edges */
	public void deselectAllProc() {
		if (this.eRule == null)
			return;
		this.leftPanel.deselectAll();
		this.rightPanel.deselectAll();
		if (this.leftCondPanel.getGraph() != null)
			this.leftCondPanel.deselectAll();
	}

	public void setStraightenArcs(boolean b) {
		if (this.eRule == null)
			return;
		this.leftPanel.getGraph().setStraightenArcs(b);
		this.rightPanel.getGraph().setStraightenArcs(b);
		if (this.leftCondPanel.getGraph() != null) {
			this.leftCondPanel.getGraph().setStraightenArcs(b);
		}		
	}
	
	/** Straigths all selected arcs */
	public void straightenArcsProc() {
		if (this.eRule == null)
			return;
		this.leftPanel.straightenSelectedArcs();
		this.rightPanel.straightenSelectedArcs();
		if (this.leftCondPanel.getGraph() != null)
			this.leftCondPanel.straightenSelectedArcs();
	}

	/** Create an identical rule */
	public void doIdenticRule() {
		this.msg = "";
		if ((this.eRule == null)
				|| (this.leftPanel.getEditMode() == EditorConstants.VIEW)
				|| (this.rightPanel.getEditMode() == EditorConstants.VIEW))
			return;

		if (this.eRule.getBasisRule() instanceof MultiRule
				&& !((MultiRule)this.eRule.getBasisRule())
						.getRuleScheme().getKernelRule().getSource().isEmpty()) {
						
			this.msg = "It is not possible to make identical multi rule\n because of non-empty kernel rule.";
			JOptionPane.showMessageDialog(
					this.applFrame,
					this.msg,
					"Failed",
					JOptionPane.ERROR_MESSAGE);		
			return;			
		} 
		
		// remove rule morphism mappings
		removeMapppingBeforeIdenticRule(this.eRule.getLeft().getNodes(), 
											this.eRule.getLeft().getArcs());	
		// remove RHS graph objects
		removeObjectsOfGraph(false, this.eRule.getRight(), 
								this.eRule.getRight().getNodes(),
								this.eRule.getRight().getArcs());
		
		// copy LHS graph objects to RHS and add morphism mappings
		this.eRule.identicRule();
		this.msg = this.eRule.getMsg();

		if (this.msg.equals("")) {
			if (this.gragraEditor != null)
				this.gragraEditor.updateUndoButton();

			this.leftPanel.updateGraphics();
			this.rightPanel.updateGraphics(true);
		}
	}

	private void removeMapppingBeforeIdenticRule(final List<EdNode> nodesList, final List<EdArc> arcsList) {
		Iterator<EdArc> arcs = arcsList.iterator();
		while (arcs.hasNext()) {
			EdGraphObject obj = arcs.next();
			removeRuleMapping(obj, true);
		}
		Iterator<EdNode> nodes = nodesList.iterator();
		while (nodes.hasNext()) {
			EdGraphObject obj = nodes.next();
			removeRuleMapping(obj, true);
		}
	}
	
	private void removeObjectsOfGraph(boolean left,
			final EdGraph g,
			final List<EdNode> nodesList, 
			final List<EdArc> arcsList) {
		
		final GraphCanvas canvas = left? this.leftPanel.getCanvas(): this.rightPanel.getCanvas();
		Iterator<EdArc> arcs = arcsList.iterator();
		while (arcs.hasNext()) {
			EdGraphObject obj = arcs.next();
			canvas.deleteObj(obj);	
			arcs = arcsList.iterator();
		}
		Iterator<EdNode> nodes = nodesList.iterator();
		while (nodes.hasNext()) {
			EdGraphObject obj = nodes.next();
			canvas.deleteObj(obj);
			nodes = nodesList.iterator();
		}
	}
	
	/** Create an identical NAC */
	public void doIdenticNAC() {
		if ((this.eRule == null) //|| (this.eNAC == null)
				|| (this.leftPanel.getEditMode() == EditorConstants.VIEW)
				|| (this.rightPanel.getEditMode() == EditorConstants.VIEW))
			return;

		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) ((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView()
				.getSelectedPath().getLastPathComponent();
		
		if (this.eNAC != null && this.eNAC.getRule() == this.eRule
								&& treeNode.getUserObject() instanceof NACTreeNodeData
								&& ((NACTreeNodeData) treeNode.getUserObject()).getNAC() == this.eNAC) {			
			if (!this.eNAC.getBasisGraph().isEmpty()) {
				Vector<?> elems = this.eRule.getLeft().getArcs();
				for (int i = 0; i < elems.size(); i++) {
					removeNacMapping((EdGraphObject) elems.get(i), true);
				}
				elems = this.eRule.getLeft().getNodes();
				for (int i = 0; i < elems.size(); i++) {
					removeNacMapping((EdGraphObject) elems.get(i), true);
				}
		
				Vector<EdGraphObject> vec = new Vector<EdGraphObject>();
				vec.addAll(this.eNAC.getNodes());
				vec.addAll(this.eNAC.getArcs());
				this.eNAC.addDeletedToUndo(vec);
			}
		}
		else if (this.eNAC == null || this.eNAC.getRule() != this.eRule || treeNode.getUserObject() != this.eNAC) {
			EdNAC enac = ((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView().addNAC(false);
			((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView().selectPath(
					((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView().getTreePathOfGrammarElement(enac));
		}

		this.eRule.identicNAC(this.eNAC);
		this.msg = this.eRule.getMsg();

		if (this.msg.equals("")) {
			if (this.gragraEditor != null)
				this.gragraEditor.updateUndoButton();

			this.leftPanel.updateGraphics();
			this.leftCondPanel.updateGraphics(true);
		}
	}

	/** Create an identical PAC */
	public void doIdenticPAC() {
		if ((this.eRule == null) //|| (this.ePAC == null)
				|| (this.leftPanel.getEditMode() == EditorConstants.VIEW)
				|| (this.rightPanel.getEditMode() == EditorConstants.VIEW))
			return;

		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) ((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView()
				.getSelectedPath().getLastPathComponent();
		
		if (this.ePAC != null && this.ePAC.getRule() == this.eRule
								&& treeNode.getUserObject() instanceof PACTreeNodeData
								&& ((PACTreeNodeData) treeNode.getUserObject()).getPAC() == this.ePAC) {
			if (!this.ePAC.getBasisGraph().isEmpty()) {
				Vector<?> elems = this.eRule.getLeft().getArcs();
				for (int i = 0; i < elems.size(); i++) {
					removePacMapping((EdGraphObject) elems.get(i), true);
				}
				elems = this.eRule.getLeft().getNodes();
				for (int i = 0; i < elems.size(); i++) {
					removePacMapping((EdGraphObject) elems.get(i), true);
				}
	
				Vector<EdGraphObject> vec = new Vector<EdGraphObject>();
				vec.addAll(this.ePAC.getNodes());
				vec.addAll(this.ePAC.getArcs());
				this.ePAC.addDeletedToUndo(vec);
			}
		}
		else if (this.ePAC == null || this.ePAC.getRule() != this.eRule
									|| treeNode.getUserObject() != this.ePAC) {
			EdPAC epac = ((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView().addPAC();
			((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView().selectPath(
					((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView().getTreePathOfGrammarElement(epac));
		}
		
		this.eRule.identicPAC(this.ePAC);
		this.msg = this.eRule.getMsg();

		if (this.msg.equals("")) {
			if (this.gragraEditor != null)
				this.gragraEditor.updateUndoButton();

			this.leftPanel.updateGraphics();
			this.leftCondPanel.updateGraphics(true);
		}
	}

	public void doIdenticGAC() {
		if ((this.eRule == null) //|| (this.eGAC == null)
				|| (this.leftPanel.getEditMode() == EditorConstants.VIEW)
				|| (this.rightPanel.getEditMode() == EditorConstants.VIEW))
			return;

		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) ((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView()
				.getSelectedPath().getLastPathComponent();
				
		if (this.eGAC != null && treeNode.getUserObject() instanceof NestedACTreeNodeData
								&& ((NestedACTreeNodeData) treeNode.getUserObject()).getNestedAC() == this.eGAC) {
			if (!this.eGAC.getBasisGraph().isEmpty()) {
				EdGraph srcGraph = this.eGAC.getSource()==null? this.eRule.getLeft(): this.eGAC.getSource();
				Vector<?> elems = srcGraph.getArcs();
				for (int i = 0; i < elems.size(); i++) {
					removeNestedACMapping((EdGraphObject) elems.get(i), true);
				}
				elems = srcGraph.getNodes();
				for (int i = 0; i < elems.size(); i++) {
					removeNestedACMapping((EdGraphObject) elems.get(i), true);
				}
	
				Vector<EdGraphObject> vec = new Vector<EdGraphObject>();
				vec.addAll(this.eGAC.getNodes());
				vec.addAll(this.eGAC.getArcs());
				this.eGAC.addDeletedToUndo(vec);
			}
		}
		else if (this.eGAC == null || this.eGAC.getRule() != this.eRule || treeNode.getUserObject() != this.eGAC) {
			EdNestedApplCond egac = ((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView().addNestedAC(false);
			((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView().selectPath(
					((AGGAppl)this.getGraGraEditor().getParentFrame()).getGraGraTreeView().getTreePathOfGrammarElement(egac));
		}
		
		this.eRule.identicNestedAC(this.eGAC);
		this.msg = this.eRule.getMsg();

		if (this.msg.equals("")) {
			if (this.gragraEditor != null)
				this.gragraEditor.updateUndoButton();

			this.leftPanel.updateGraphics();
			this.leftCondPanel.updateGraphics(true);
		}
	}
	
	/** Create a GAC from the rule RHS*/
	public void doGACDuetoRHS() {
		if ((this.eRule == null) || (this.eGAC == null)
				|| (this.leftPanel.getEditMode() == EditorConstants.VIEW)
				|| (this.rightPanel.getEditMode() == EditorConstants.VIEW))
			return;

		if (!this.eGAC.getBasisGraph().isEmpty()) {
			EdGraph srcGraph = this.eGAC.getSource()==null? this.eRule.getLeft(): this.eGAC.getSource();
			Vector<?> elems = srcGraph.getArcs();
			for (int i = 0; i < elems.size(); i++) {
				removeNestedACMapping((EdGraphObject) elems.get(i), true);
			}
			elems = srcGraph.getNodes();
			for (int i = 0; i < elems.size(); i++) {
				removeNestedACMapping((EdGraphObject) elems.get(i), true);
			}

			Vector<EdGraphObject> vec = new Vector<EdGraphObject>();
			vec.addAll(this.eGAC.getNodes());
			vec.addAll(this.eGAC.getArcs());
			this.eGAC.addDeletedToUndo(vec);
		}

		this.eRule.makeGACDuetoRHS(this.eGAC);
		this.msg = this.eRule.getMsg();

		if (this.msg.equals("")) {
			if (this.gragraEditor != null)
				this.gragraEditor.updateUndoButton();

			this.leftPanel.updateGraphics();
			this.leftCondPanel.updateGraphics(true);
		}
	}

	/** Create a NAC from the rule RHS*/
	public void doNACDuetoRHS() {
		if ((this.eRule == null) || (this.eNAC == null)
				|| (this.leftPanel.getEditMode() == EditorConstants.VIEW)
				|| (this.rightPanel.getEditMode() == EditorConstants.VIEW))
			return;

		if (!this.eNAC.getBasisGraph().isEmpty()) {
			Vector<?> elems = this.eRule.getLeft().getArcs();
			for (int i = 0; i < elems.size(); i++) {
				removeNacMapping((EdGraphObject) elems.get(i), true);
			}
			elems = this.eRule.getLeft().getNodes();
			for (int i = 0; i < elems.size(); i++) {
				removeNacMapping((EdGraphObject) elems.get(i), true);
			}

			Vector<EdGraphObject> vec = new Vector<EdGraphObject>();
			vec.addAll(this.eNAC.getNodes());
			vec.addAll(this.eNAC.getArcs());
			this.eNAC.addDeletedToUndo(vec);
		}

		this.eRule.makeNACDuetoRHS(this.eNAC);
		this.msg = this.eRule.getMsg();

		if (this.msg.equals("")) {
			if (this.gragraEditor != null)
				this.gragraEditor.updateUndoButton();

			this.leftPanel.updateGraphics();
			this.leftCondPanel.updateGraphics(true);
		}
	}
	
	public void setGraphToCopy(EdGraph g) {
		if (this.eRule != null) {
			this.eRule.getLeft().setGraphToCopy(g);
			this.eRule.getRight().setGraphToCopy(g);
			if (this.leftCondPanel.getGraph() != null)
				this.leftCondPanel.getGraph().setGraphToCopy(g);
		}
	}

	public EdGraph getSelectedAsGraph() {
		if (this.eRule == null)
			return null;
		EdGraph g = this.eRule.getLeft().getSelectedAsGraph();
		if (g != null) {
			this.sourceOfCopy = this.eRule.getLeft();
			return g;
		}
		g = this.eRule.getRight().getSelectedAsGraph();
		if (g != null) {
			this.sourceOfCopy = this.eRule.getRight();
			return g;
		}
		if (this.leftCondPanel.getGraph() != null) {
			g = this.leftCondPanel.getGraph().getSelectedAsGraph();
			if (g != null) {
				this.sourceOfCopy = this.leftCondPanel.getGraph();
				return g;
			}
			return null;
		}
		return null;
	}

	public EdGraph getSourceOfCopy() {
		return this.sourceOfCopy;
	}

	public void setSourceOfCopy(EdGraph g) {
		this.sourceOfCopy = g;
	}

	/* privates */

	private void setLastEditModeBeforMatch(GraphPanel gp) {
		if (gp.getEditMode() == EditorConstants.DRAW
				|| gp.getEditMode() == EditorConstants.ARC
				|| gp.getEditMode() == EditorConstants.SELECT
				|| gp.getEditMode() == EditorConstants.MOVE
				|| gp.getEditMode() == EditorConstants.ATTRIBUTES
				|| gp.getEditMode() == EditorConstants.INTERACT_RULE
				|| gp.getEditMode() == EditorConstants.INTERACT_NAC
				|| gp.getEditMode() == EditorConstants.INTERACT_PAC
				|| gp.getEditMode() == EditorConstants.INTERACT_AC
				|| gp.getEditMode() == EditorConstants.VIEW)
			gp.setLastEditMode(gp.getEditMode());
	}

	private void handleMouseListenerFromGraphEditor(int mode) {
		switch (mode) {
		case EditorConstants.DRAW:
		case EditorConstants.ARC:
		case EditorConstants.SELECT:
		case EditorConstants.MOVE:
		case EditorConstants.ATTRIBUTES:
//		case EditorConstants.INTERACT_RULE:
//		case EditorConstants.INTERACT_NAC:
//		case EditorConstants.INTERACT_PAC:
//		case EditorConstants.INTERACT_AC:	
			if (this.mouseListenerFromGraphEditorAdded) {
				if (this.graphEditor != null) {
					this.graphEditor.getGraphPanel().getCanvas().removeMouseListener(
							this.mouseAdapter);
					this.mouseListenerFromGraphEditorAdded = false;
				}
			}
			break;
		case EditorConstants.INTERACT_MATCH:
		case EditorConstants.MAP:
		case EditorConstants.UNMAP:
		case EditorConstants.MAPSEL:
		case EditorConstants.UNMAPSEL:
			if (!this.mouseListenerFromGraphEditorAdded) {
				if (this.graphEditor != null) {
					this.graphEditor.getGraphPanel().getCanvas().addMouseListener(
							this.mouseAdapter);
					this.mouseListenerFromGraphEditorAdded = true;
				}
			}
			break;
		default:
			break;
		}
	}

	private void removeProc() {
//		if (this.leftPanel.getEditMode() == EditorConstants.INTERACT_RULE)
//			removeRuleMappingProc();
//		else if (this.leftPanel.getEditMode() == EditorConstants.INTERACT_NAC)
//			removeNACMappingProc();
//		else if (this.leftPanel.getEditMode() == EditorConstants.INTERACT_PAC)
//			removePACMappingProc();
//		else if (this.leftPanel.getEditMode() == EditorConstants.INTERACT_AC)
//			removeNestedACMappingProc();
//		else if (this.leftPanel.getEditMode() == EditorConstants.INTERACT_MATCH)
//			removeMatchMappingProc();
//		else 
		if (this.leftPanel.getEditMode() == EditorConstants.MAP)
			removeMappingProc();
		else if (this.leftPanel.getEditMode() == EditorConstants.MAPSEL)
			removeMappingSelProc();
	}

	@SuppressWarnings("unused")
	private void removeRuleMappingProc() {
		this.leftPanel.setEditMode(EditorConstants.REMOVE_RULE);
		this.leftPanel.setEditCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	@SuppressWarnings("unused")
	private void removeNACMappingProc() {
		this.leftPanel.setEditMode(EditorConstants.REMOVE_NAC);
		this.leftPanel.setEditCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	@SuppressWarnings("unused")
	private void removePACMappingProc() {
		this.leftPanel.setEditMode(EditorConstants.REMOVE_PAC);
		this.leftPanel.setEditCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	@SuppressWarnings("unused")
	private void removeNestedACMappingProc() {
		this.leftPanel.setEditMode(EditorConstants.REMOVE_AC);
		this.leftPanel.setEditCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}
	
	@SuppressWarnings("unused")
	private void removeMatchMappingProc() {
		this.leftPanel.setEditMode(EditorConstants.REMOVE_MATCH);
		this.leftPanel.setEditCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	private void removeMappingProc() {
		this.leftPanel.setEditMode(EditorConstants.REMOVE_MAP);
		this.leftPanel.setEditCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	private void removeMappingSelProc() {
		this.leftPanel.setEditMode(EditorConstants.REMOVE_MAPSEL);
		this.leftPanel.setEditCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		if (this.applFrame != null)
			this.applFrame.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	public void resetEditModeAfterMapping() {
		// only after popupmenu item Map
		this.mapping = false;
		this.isEditPopupMenu = false;
		this.isEditSelPopupMenu = false;
		setEditMode(this.leftPanel.getLastEditMode());
		if (this.graphEditor != null)
			this.graphEditor.setEditMode(this.leftPanel.getLastEditMode());
		if (this.gragraEditor != null)
			this.gragraEditor.setMsg(getMsg());

	}

	public void setExportJPEG(GraphicsExportJPEG jpg) {
		this.exportJPEG = jpg;
	}

	private void setLastEditModeBeforMapping(GraphPanel gp) {
		// only befor Map, Uunmap, Map Selected, Unmap Selected from
		// ObjectPopupMenu
		if (gp.getEditMode() == EditorConstants.DRAW
				|| gp.getEditMode() == EditorConstants.ARC
				|| gp.getEditMode() == EditorConstants.SELECT
				|| gp.getEditMode() == EditorConstants.MOVE
				|| gp.getEditMode() == EditorConstants.ATTRIBUTES
				|| gp.getEditMode() == EditorConstants.INTERACT_RULE
				|| gp.getEditMode() == EditorConstants.INTERACT_NAC
				|| gp.getEditMode() == EditorConstants.INTERACT_PAC
				|| gp.getEditMode() == EditorConstants.INTERACT_AC
				|| gp.getEditMode() == EditorConstants.INTERACT_MATCH
				|| gp.getEditMode() == EditorConstants.MAP
				|| gp.getEditMode() == EditorConstants.UNMAP)
			gp.setLastEditMode(gp.getEditMode());
	}

	/*
	private void removeKeyListenerAfterMapping(GraphPanel gp) {
		if (gp.getEditMode() == EditorConstants.INTERACT_RULE
				|| gp.getEditMode() == EditorConstants.INTERACT_NAC
				|| gp.getEditMode() == EditorConstants.INTERACT_PAC
				|| gp.getEditMode() == EditorConstants.INTERACT_MATCH
				|| gp.getEditMode() == EditorConstants.MAP
				|| gp.getEditMode() == EditorConstants.MAPSEL)
			; // gp.getCanvas().removeKeyListener(this);
	}

	private void addKeyListenerBeforMapping(GraphPanel gp) {
		if (gp.getEditMode() == EditorConstants.INTERACT_RULE
				|| gp.getEditMode() == EditorConstants.INTERACT_NAC
				|| gp.getEditMode() == EditorConstants.INTERACT_PAC
				|| gp.getEditMode() == EditorConstants.INTERACT_MATCH
				|| gp.getEditMode() == EditorConstants.MAP
				|| gp.getEditMode() == EditorConstants.MAPSEL)
			; // gp.getCanvas().addKeyListener(this);
	}
*/
	
	public GraphPanel setActivePanel(Object src) {
		if (src instanceof GraphCanvas) 
			this.activePanel = ((GraphCanvas) src).getViewport();
		else
			this.activePanel = null;
		
		return this.activePanel;
	}

	public void showPopupMenu(MouseEvent e) {
		if (this.activePanel != null
				&& (e.getX() > 0 && e.getY() > 0)) {

			if (this.editPopupMenu != null) {
				this.editPopupMenu.setEditor(this);
				this.editPopupMenu.setParentFrame(this.applFrame);
			}
			if (this.editSelPopupMenu != null) {
				this.editSelPopupMenu.setEditor(this);
				this.editSelPopupMenu.setParentFrame(this.applFrame);
			}
	
			this.isEditPopupMenu = false;
			this.isEditSelPopupMenu = false;
			if (this.activePanel == this.leftPanel) {
				if (this.modePopupMenu != null
						&& this.modePopupMenu.invoked(this, this.activePanel, e.getX(), e.getY())) {
					this.modePopupMenu.show(e.getComponent(), e.getX(), e.getY());
				} else if (this.editPopupMenu != null
						&& this.editPopupMenu.invoked(this.activePanel, e.getX(), e.getY())) {
					this.isEditPopupMenu = true;
					this.editPopupMenu.setMapEnabled(true);
					this.editPopupMenu.setUnmapEnabled(true);
					this.editPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				} else if (this.editSelPopupMenu != null
						&& this.editSelPopupMenu.invoked(this.activePanel, e.getX(), e.getY())) {
					this.isEditSelPopupMenu = true;
					this.editSelPopupMenu.setMapEnabled(true);
					this.editSelPopupMenu.setUnmapEnabled(true);
					this.editSelPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			} else {
				if (this.modePopupMenu != null
						&& this.modePopupMenu.invoked(this, this.activePanel, e.getX(), e.getY()))
					this.modePopupMenu.show(e.getComponent(), e.getX(), e.getY());
				else if (this.editPopupMenu != null
						&& this.editPopupMenu.invoked(this.activePanel, e.getX(), e.getY())) {
					this.isEditPopupMenu = true;
					this.editPopupMenu.setMapEnabled(false);
					this.editPopupMenu.setUnmapEnabled(true);
					this.editPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				} else if (this.editSelPopupMenu != null
						&& this.editSelPopupMenu.invoked(this.activePanel, e.getX(), e.getY())) {
					this.isEditSelPopupMenu = true;
					this.editSelPopupMenu.setMapEnabled(false);
					this.editSelPopupMenu.setUnmapEnabled(true);
					this.editSelPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}
	}

	public void deselectAllWeakselected() {
		if (this.leftPanel.eGraph != null)
			this.leftPanel.eGraph.deselectAllWeakselected();
		if (this.rightPanel.eGraph != null)
			this.rightPanel.eGraph.deselectAllWeakselected();
		if (this.leftCondPanel.eGraph != null)
			this.leftCondPanel.eGraph.deselectAllWeakselected();
	}
	
	private void setPanelEditMode(int mode) {
		this.leftPanel.setEditMode(mode);
		this.rightPanel.setEditMode(mode);
		this.leftCondPanel.setEditMode(mode);
	}

	
	
	protected final JFrame applFrame;

	private final GraGraEditor gragraEditor;

	private GraphEditor graphEditor;

	private AttrTopEditor attrEditor;

	private final JLabel title;

	private String titleKind = " ";
	
	private final JLabel titleAC;

	private String titleKindAC = " ";
	
	private String ruleName;

	private String gragraName;

	private String conclusionName;

	private String atomicName;

	private final JSplitPane splitPane;

	private final JSplitPane ruleSplitPane;

	private int ruleDividerLocation, acDividerLocation;
//				pacDividerLocation, nacDividerLocation;

	private final Hashtable<Object, Integer> dividerLocationSet;

	private final GraphPanel leftPanel;

	private final GraphPanel rightPanel;

	private final GraphPanel leftCondPanel;

	private GraphPanel activePanel;

//	private String lastText;

	private String msg = "";

	private EdGraGra eGra;

	private EdRule eRule;

	private EdNAC eNAC;

	private EdPAC ePAC;
	
	private EdNestedApplCond eGAC;

	private EdGraph sourceOfCopy;

//	private boolean isPopupTrigger = false;

	private ModePopupMenu modePopupMenu;

	private EditPopupMenu editPopupMenu;

	private EditSelPopupMenu editSelPopupMenu;

	private boolean isEditPopupMenu = false;

	private boolean isEditSelPopupMenu = false;

	private boolean doNotShowPopupMenu;
	
	private EdGraphObject leftObj;

	private EdGraphObject rightObj;

	private EdGraphObject leftCondObj;

	private EdGraphObject graphObj;

//	private int lastEditMode = 0;

//	private Cursor lastEditModeCursor;

	private boolean synchrMoveOfMapObjs = false;

	private boolean draggingL = false, draggingR = false, draggingC = false;

	private boolean mapping = false;

	private boolean mouseListenerFromGraphEditorAdded = false;

	GraphicsExportJPEG exportJPEG;

	private final JButton exportJPEGButton;

	final JPanel mainPanel;

}
