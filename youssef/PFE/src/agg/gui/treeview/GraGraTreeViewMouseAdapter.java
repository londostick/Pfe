/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.treeview;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import agg.gui.treeview.nodedata.GraGraTreeNodeData;

public class GraGraTreeViewMouseAdapter extends MouseAdapter {

	private GraGraTreeView treeView;
	
	public GraGraTreeViewMouseAdapter(GraGraTreeView treeview) {
		super();
		this.treeView = treeview;
		this.treeView.addMouseListener(this);
	}

	public void mousePressed(MouseEvent e) {
		if (e.getSource() == this.treeView.tree) {
			this.treeView.tree.setEditable(false);
			if (this.treeView.tree.getRowForLocation(e.getX(), e.getY()) != -1
					&& this.treeView.tree.getPathForLocation(e.getX(), e.getY()) != this.treeView.selPath) {
				this.treeView.selPath = this.treeView.tree.getPathForLocation(e.getX(), e.getY());
				this.treeView.setFlagForNew();
			}
			
			if (e.isPopupTrigger()) {
				this.treeView.popupLocation = new Point(e.getX(), e.getY());
				if (this.treeView.tree.getRowForLocation(e.getX(), e.getY()) == -1) {
					// click on background
					this.treeView.filePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.filePopupMenu.getLocationOnScreen();
				} else if (this.treeView.gragraPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.gragraPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.gragraPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.ruleSchemePopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.ruleSchemePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.ruleSchemePopupMenu.getLocationOnScreen();	
				} else if (this.treeView.kernRulePopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.kernRulePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.kernRulePopupMenu.getLocationOnScreen();
				} else if (this.treeView.multiRulePopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.multiRulePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.multiRulePopupMenu.getLocationOnScreen();
				} 
				else if (this.treeView.amalgamRulePopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.amalgamRulePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.amalgamRulePopupMenu.getLocationOnScreen();
				} 
				else if (this.treeView.rulePopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.rulePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.rulePopupMenu.getLocationOnScreen();
				} else if (this.treeView.nacPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.nacPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.nacPopupMenu.getLocationOnScreen();
				} else if (this.treeView.pacPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.pacPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.pacPopupMenu.getLocationOnScreen();
				} else if (this.treeView.acPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.acPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.acPopupMenu.getLocationOnScreen();
				} else if (this.treeView.constraintPopupMenu.invoked(e.getX(), e
						.getY())) {
					this.treeView.constraintPopupMenu.show(e.getComponent(),
							e.getX(), e.getY());
					this.treeView.popupLocation = this.treeView.constraintPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.atomicPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.atomicPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.atomicPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.conclusionPopupMenu.invoked(e.getX(), e
						.getY())) {
					this.treeView.conclusionPopupMenu.show(e.getComponent(),
							e.getX(), e.getY());
					this.treeView.popupLocation = this.treeView.conclusionPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.typeGraphPopupMenu.invoked(e.getX(), e
						.getY())) {
					this.treeView.typeGraphPopupMenu.show(e.getComponent(), e.getX(),
							e.getY());
					this.treeView.popupLocation = this.treeView.typeGraphPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.graphPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.graphPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.graphPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.attrConditionPopupMenu.invoked(e.getX(), e
						.getY())) {
					this.treeView.attrConditionPopupMenu.show(e.getComponent(), e
							.getX(), e.getY());
					this.treeView.popupLocation = this.treeView.attrConditionPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.applFormulaPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.applFormulaPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.applFormulaPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.ruleSequencePopupMenu.invoked(e.getX(), e
						.getY())) {
					this.treeView.ruleSequencePopupMenu.show(e.getComponent(), e
							.getX(), e.getY());
				}
			} else 
			if (SwingUtilities.isMiddleMouseButton(e)) {
				if (this.treeView.selPath != null) {
					if (this.treeView.selPath == this.treeView.tree.getSelectionPath())
						this.treeView.isSelected = true;
					else
						this.treeView.isSelected = false;

					DefaultMutableTreeNode aNode = (DefaultMutableTreeNode) this.treeView.selPath
							.getLastPathComponent();
					GraGraTreeNodeData sd = (GraGraTreeNodeData) aNode
							.getUserObject();
					if ((sd != null) && (sd.isRule())) {
						this.treeView.movedNode = aNode;
						if (this.treeView.tree.isExpanded(this.treeView.selPath))
							this.treeView.tree.collapsePath(this.treeView.selPath);
						this.treeView.movedPoint.x = e.getX();
						this.treeView.movedPoint.y = e.getY();
						this.treeView.applFrame.setCursor(new Cursor(
								Cursor.MOVE_CURSOR));
						this.treeView.movedRect = this.treeView.tree.getRowBounds(this.treeView.tree
								.getRowForPath(this.treeView.selPath));
						this.treeView.tree.getGraphics().drawRect(this.treeView.movedRect.x - 2,
								this.treeView.movedRect.y - 2, this.treeView.movedRect.width + 2,
								this.treeView.movedRect.height + 2);
					}
				}
			} else if (SwingUtilities.isLeftMouseButton(e)) {
				if (this.treeView.selPath != null 
						&& this.treeView.movedNode == null) {
					this.treeView.pressedMouseLeft = true;
					this.treeView.applFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		} else if (e.getSource() == this.treeView.trash) {
			this.treeView.gragraStore.setLocation(this.treeView.trash.getLocationOnScreen().x,
					this.treeView.trash.getLocationOnScreen().y);
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == this.treeView.tree) {
			if (SwingUtilities.isLeftMouseButton(e) && this.treeView.selPath != null) {
				if (e.getClickCount() == 2) {
					this.treeView.tree.setSelectionPath(this.treeView.selPath);
					this.treeView.setFlagForNew();
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.treeView.selPath
							.getLastPathComponent();
					if (((GraGraTreeNodeData) node.getUserObject())
									.isTreeTextEditable()) {								
						if (!this.treeView.tree.isExpanded(this.treeView.selPath))
							this.treeView.tree.expandPath(this.treeView.selPath);
						// CellEditor will be activated						
						this.treeView.tree.setEditable(true);
						this.treeView.tree.startEditingAtPath(this.treeView.selPath);
					}
					this.treeView.pressedMouseLeft = false;
				} 
			} 
		} else if (e.getSource() == this.treeView.trash) {
			this.treeView.gragraStore.setLocation(this.treeView.trash.getLocationOnScreen().x,
					this.treeView.trash.getLocationOnScreen().y);
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (e.getSource() == this.treeView.tree) {
			if (this.treeView.pressedMouseLeft && SwingUtilities.isLeftMouseButton(e)) {
				this.treeView.propagateSelectedTreeItem();
				this.treeView.pressedMouseLeft = false;
			} 
			else if (e.isPopupTrigger()) {
				if (this.treeView.tree.getRowForLocation(e.getX(), e.getY()) == -1) {
					this.treeView.popupLocation = new Point(e.getX(), e.getY());
					// click on background
					this.treeView.filePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.filePopupMenu.getLocationOnScreen();
				} else if (this.treeView.gragraPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.gragraPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.gragraPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.ruleSchemePopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.ruleSchemePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.ruleSchemePopupMenu.getLocationOnScreen();	
				} else if (this.treeView.kernRulePopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.kernRulePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.kernRulePopupMenu.getLocationOnScreen();
				} else if (this.treeView.multiRulePopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.multiRulePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.multiRulePopupMenu.getLocationOnScreen();
				} 
				else if (this.treeView.amalgamRulePopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.amalgamRulePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.amalgamRulePopupMenu.getLocationOnScreen();
				} 
				else if (this.treeView.rulePopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.rulePopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.rulePopupMenu.getLocationOnScreen();
				} else if (this.treeView.nacPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.nacPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.nacPopupMenu.getLocationOnScreen();
				} else if (this.treeView.pacPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.pacPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.pacPopupMenu.getLocationOnScreen();								
				} else if (this.treeView.acPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.acPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.acPopupMenu.getLocationOnScreen();
				} else if (this.treeView.constraintPopupMenu.invoked(e.getX(), e
						.getY())) {
					this.treeView.constraintPopupMenu.show(e.getComponent(),
							e.getX(), e.getY());
					this.treeView.popupLocation = this.treeView.constraintPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.atomicPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.atomicPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.atomicPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.conclusionPopupMenu.invoked(e.getX(), e
						.getY())) {
					this.treeView.conclusionPopupMenu.show(e.getComponent(),
							e.getX(), e.getY());
					this.treeView.popupLocation = this.treeView.conclusionPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.typeGraphPopupMenu.invoked(e.getX(), e
						.getY())) {
					this.treeView.typeGraphPopupMenu.show(e.getComponent(), e.getX(),
							e.getY());
					this.treeView.popupLocation = this.treeView.typeGraphPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.graphPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.graphPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.graphPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.attrConditionPopupMenu.invoked(e.getX(), e
						.getY())) {
					this.treeView.attrConditionPopupMenu.show(e.getComponent(), e
							.getX(), e.getY());
					this.treeView.popupLocation = this.treeView.attrConditionPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.applFormulaPopupMenu.invoked(e.getX(), e.getY())) {
					this.treeView.applFormulaPopupMenu.show(e.getComponent(), e.getX(), e
							.getY());
					this.treeView.popupLocation = this.treeView.applFormulaPopupMenu
							.getLocationOnScreen();
				} else if (this.treeView.ruleSequencePopupMenu.invoked(e.getX(), e
						.getY())) {
					this.treeView.ruleSequencePopupMenu.show(e.getComponent(), e
							.getX(), e.getY());
				}
			} else if (this.treeView.movedNode != null) {
				this.treeView.applFrame.setCursor(new Cursor(Cursor.MOVE_CURSOR));
				if (this.treeView.tree.getRowForLocation(e.getX(), e.getY()) != -1) {
					DefaultMutableTreeNode aNode = (DefaultMutableTreeNode) this.treeView.tree
							.getPathForLocation(e.getX(), e.getY())
							.getLastPathComponent();
					GraGraTreeNodeData sd = (GraGraTreeNodeData) aNode
							.getUserObject();
					if ((aNode != this.treeView.movedNode)
							&& sd.isRule()
							&& (this.treeView.movedNode.getParent() == aNode
									.getParent())) {
						int oldIndx = this.treeView.treeModel.getIndexOfChild(
								this.treeView.movedNode.getParent(), this.treeView.movedNode);
						DefaultMutableTreeNode movedCopy = (DefaultMutableTreeNode) this.treeView.movedNode
								.clone();
						for (int i = 0; i < this.treeView.movedNode.getChildCount();)
							movedCopy
									.add((DefaultMutableTreeNode) this.treeView.movedNode
											.getChildAt(i));

						this.treeView.treeModel.insertNodeInto(movedCopy,
								(DefaultMutableTreeNode) aNode
										.getParent(),
								((DefaultMutableTreeNode) aNode
										.getParent()).getIndex(aNode));
						int newIndx = this.treeView.treeModel.getIndexOfChild(
								this.treeView.movedNode.getParent(), movedCopy);

						if ((this.treeView.tmpSelNode != null)
								&& this.treeView.movedNode.equals(this.treeView.tmpSelNode))
							this.treeView.isSelected = true;

						this.treeView.treeModel.removeNodeFromParent(this.treeView.movedNode);

						if (this.treeView.isSelected) {
							if (newIndx < oldIndx)
								this.treeView.tree.setSelectionRow(this.treeView.tree
										.getRowForLocation(e.getX(), e
												.getY()));
							else
								this.treeView.tree.setSelectionRow(this.treeView.tree
										.getRowForLocation(e.getX(), e
												.getY()) - 1);
						} else if (this.treeView.tmpSelPath != null) {
							this.treeView.tree.setSelectionPath(this.treeView.tmpSelPath);
						}
						this.treeView.selPath = this.treeView.tree.getPathForLocation(e.getX(), e
								.getY());
						this.treeView.refreshGraGraRules((DefaultMutableTreeNode) movedCopy
								.getParent());
					}
				}
				this.treeView.movedNode = null;
				this.treeView.tmpSelNode = null;
				this.treeView.tmpSelPath = null;
				this.treeView.isSelected = false;
				this.treeView.applFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				this.treeView.wasMoved = false;
			} 
		} else if (e.getSource() == this.treeView.trash) {
			this.treeView.gragraStore.setLocation(this.treeView.trash.getLocationOnScreen().x,
					this.treeView.trash.getLocationOnScreen().y);
		}
		this.treeView.requestFocusInWindow();
	}

	public void mouseDragged(MouseEvent e) {
		if (e.getSource() == this.treeView.tree) {
			if (this.treeView.movedNode != null) {
				int dx = e.getX() - this.treeView.movedPoint.x;
				int dy = e.getY() - this.treeView.movedPoint.y;
				this.treeView.movedPoint.x = e.getX();
				this.treeView.movedPoint.y = e.getY();
				this.treeView.movedRect.x = this.treeView.movedRect.x + dx;
				this.treeView.movedRect.y =this.treeView. movedRect.y = dy;
				this.treeView.tree.getGraphics().drawRect(this.treeView.movedRect.x, this.treeView.movedRect.y,
						this.treeView.movedRect.width, this.treeView.movedRect.height);
			}
		}
	}
				
	public void mouseEntered(MouseEvent e) {
		this.treeView.requestFocusInWindow();
	}

}
