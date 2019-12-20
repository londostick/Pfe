/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.editor.impl;

import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Hashtable;
import javax.swing.ImageIcon;
import javax.swing.undo.*;

import agg.attribute.AttrEvent;
import agg.attribute.AttrInstance;
import agg.attribute.impl.AttrTupleManager;
import agg.attribute.impl.DeclMember;
import agg.attribute.impl.ValueTuple;
import agg.attribute.impl.ValueMember;
import agg.attribute.impl.VarTuple;
import agg.attribute.impl.VarMember;
import agg.attribute.impl.ContextView;
import agg.attribute.view.AttrViewEvent;
import agg.attribute.view.AttrViewObserver;
import agg.attribute.view.AttrViewSetting;
import agg.util.XMLHelper;
import agg.util.XMLObject;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.Node;
import agg.xt_basis.Arc;
import agg.xt_basis.TypeException;
import agg.gui.editor.EditorConstants;
import agg.gui.editor.GraphPanel;
import agg.layout.evolutionary.LayoutNode;

/**
 * EdNode specifies a node layout of an agg.xt_basis.Node object
 * 
 * @author $Author: olga $
 * @version $Id: EdNode.java,v 1.61 2010/11/14 12:59:11 olga Exp $
 */
public class EdNode extends EdGraphObject implements AttrViewObserver,
		XMLObject, StateEditable {
	
	private Node bNode;

	private LayoutNode lNode;

	private int nodeid;

	private Vector<Integer> cluster, oldcluster;
		
	private Color ownColor = null;
	
	
	/**
	 * Creates a node layout specified by the EdType eType for an used object
	 * specified by the Node bNode.
	 */
	public EdNode(Node bNode, EdType eType) {
		super(eType);
		this.bNode = bNode;

		if (this.bNode != null) {
			this.contextUsage = String.valueOf(this.hashCode());

			// this as AttrViewObserver register
			if (this.bNode.getAttribute() != null) {
				addToAttributeViewObserver();
			}
		}
		
		this.lNode = new LayoutNode(this);
		this.nodeid = -1;
		
		this.x = 100;
		this.y = 100;
		this.w = 20;
		this.h = 20;
	}

	/**
	 * Creates a node layout specified by the EdType eType for an used object of
	 * the class agg.xt_basis.Node that would be created from the graph
	 * specified by the Graph bGraph
	 */
	public EdNode(Graph bGraph, EdType eType) throws TypeException {
		this((bGraph != null) ? bGraph.createNode(eType.bType) : null, eType);
	}

	
	/** Disposes myself */
	public void dispose() {
		if (this.attrObserver) {
			removeFromAttributeViewObserver();	
		}
		this.view = null;
		if (this.lNode != null)
			this.lNode.dispose();
		this.lNode = null;
		this.eGraph = null;
		this.eType = null;
		this.bNode = null;
		this.myGraphPanel = null;
//		System.out.println("EdNode.dispose:: DONE  "+this);
	}

	public void finalize() {
//		System.out.println("EdNode.finalize()  called  "+this);
	}
	
	
	public void storeState(Hashtable<Object, Object> state) {
		NodeReprData data = new NodeReprData(this);
		state.put(Integer.valueOf(this.hashCode()), data);			
		state.put(Integer.valueOf(data.hashCode()), data);
		this.itsUndoReprDataHC = Integer.valueOf(data.hashCode());
	}

	public void restoreState(Hashtable<?, ?> state) {
//		System.out.println("EdNode.restoreState:: "+state.get(Integer.valueOf(this.hashCode()))+"   "+state.get(this.itsUndoReprDataHC));
		NodeReprData data = (NodeReprData) state.get(Integer.valueOf(this.hashCode()));	
		if (data == null) {
			data = (NodeReprData) state.get(this.itsUndoReprDataHC);	
		}
		if (data != null) {
			data.restoreNodeFromNodeRepr(this);
			this.attrChanged = false;
		}
	}

	public void restoreState(Hashtable<?, ?> state, String hashCode) {
//		System.out.println("### EdNode.restoreState:: "+state.get(Integer.valueOf(hashCode))+"   "+state.get(this.itsUndoReprDataHC));
		NodeReprData data = (NodeReprData) state.get(Integer.valueOf(hashCode));
		if (data == null) {
			data = (NodeReprData) state.get(this.itsUndoReprDataHC);
		}
		if (data == null) {
			data = (NodeReprData) state.get(Integer.valueOf(this.hashCode()));
		}
		
		if (data != null) {
			data.restoreNodeFromNodeRepr(this);
			this.attrChanged = false;
		}
	}
	
	public void restoreState(NodeReprData data) {
		data.restoreNodeFromNodeRepr(this);
		this.attrChanged = false;
	}
	
	/** Returns an open view of my attribute */
	protected AttrViewSetting getView() { 
		if (!this.init || this.view == null) {
			this.view = ((AttrTupleManager)AttrTupleManager.getDefaultManager()).getDefaultOpenView();
//			this.view.setAllVisible(this.bNode.getAttribute(), true);
			this.view.setVisible(this.bNode.getAttribute());
			this.init = true;
		} 
		return this.view;
	}
	
	public void setAttrViewSetting(AttrViewSetting aView) {
		this.view = aView;
		if (!this.attrObserver) {
			this.view.addObserver(this, this.bNode.getAttribute());
			this.attrObserver = true;
		}
		this.init = true;
	}
	
	public void addToAttributeViewObserver() {
		getView().addObserver(this, this.bNode.getAttribute());
		this.attrObserver = true;
	}
	
	public void removeFromAttributeViewObserver() {
		if (this.view != null
				&& this.bNode != null 
				&& this.bNode.getAttribute() != null) {
			this.view.removeObserver(this, this.bNode.getAttribute());
			this.view.getMaskedView()
					.removeObserver(this, this.bNode.getAttribute());
		}
	}
	
	public void createAttributeInstance() {
		if (this.bNode != null && this.bNode.getAttribute() == null) {
			this.bNode.createAttributeInstance();
			addToAttributeViewObserver();
		}
	}

	public void refreshAttributeInstance() {
		if (this.bNode != null && this.bNode.getAttribute() != null) {
			((ValueTuple) this.bNode.getAttribute()).getTupleType().refreshParents();
			addToAttributeViewObserver();
		}
	}

	/**
	 * Returns the layout node of this node.
	 */
	public LayoutNode getLNode() {
		return this.lNode;
	}

	/** Returns the used object */
	public Node getBasisNode() {
		return this.bNode;
	}

	/** Returns the used object */
	public GraphObject getBasisObject() {
		return this.bNode;
	}

	/** Returns TRUE */
	public boolean isNode() {
		return true;
	}

	/** Returns FALSE */
	public boolean isArc() {
		return false;
	}

	public void setCritical(boolean b) {
		this.bNode.setCritical(b);
	}

	public boolean isCritical() {
		return this.bNode.isCritical();
	}

	/**
	 * States how to draw critical objects of CPA critical overlapping graphs:
	 * <code>EdGraphObject.CRITICAL_GREEN</code> or
	 * <code>EdGraphObject.CRITICAL_BLACK_BOLD</code>.
	 */
	public void setDrawingStyleOfCriticalObject(int criticalStyle) {
		this.criticalStyle = criticalStyle;
	}
	
	public boolean isVisible() {
		if (this.bNode != null) {			
			this.visible = this.bNode.isVisible();
			
			if (this.getContext().getBasisGraph().isCompleteGraph()) {
				this.visible = this.visible
					&& this.getType().getBasisType().isObjectOfTypeGraphNodeVisible();
			}
			
			return this.visible;
		} 
		return this.visible;
	}

	/** Returns this */
	public EdNode getNode() {
		return this;
	}

	/** Returns NULL */
	public EdArc getArc() {
		return null;
	}

	/**
	 * @return count of all incoming edges (also loops)
	 */
	public int getInArcsCount() {
		return this.bNode.getIncomingArcsSet().size();
	}

	/**
	 * @return count of all outgoing edges (also loops)
	 */
	public int getOutArcsCount() {
		return this.bNode.getOutgoingArcsSet().size();
	}

	/**
	 * @return count of all incoming and outgoing edges (without loops!)
	 */
	public int getInOutArcsCount() {
		return this.bNode.getOutgoingArcsSet().size()
				+this.bNode.getIncomingArcsSet().size();
	}

	/**
	 * @return count of all loop edges
	 */
	public int getLoopArcsCount() {
		int c = 0;
		Iterator<Arc> e = this.bNode.getOutgoingArcsSet().iterator();
		while (e.hasNext()) {
			if (e.next().isLoop())
				c++;
		}
		return c;
	}

	/**
	 * Returns the attributes which are shown
	 */
	public Vector<Vector<String>> getAttributes() {
		Vector<Vector<String>> attrs = new Vector<Vector<String>>();
		if (this.bNode != null) {
			AttrInstance attributes = this.bNode.getAttribute();
			if (attributes != null && getView() != null) {
				AttrViewSetting mvs = this.view.getMaskedView();
				
				int number = mvs.getSize(attributes);								
				for (int i = 0; i < number; i++) {
					Vector<String> tmpAttrVector = new Vector<String>(3);
					int index = mvs.convertSlotToIndex(attributes, i);
					DeclMember currentMember = (DeclMember) attributes
							.getType().getMemberAt(index);
					
					if (this.elemOfTG
							&& (currentMember != null)
							&& (currentMember.getHoldingTuple() != attributes
									.getType())) {
						
//						if (!((ValueMember)attributes.getMemberAt(index)).isSet()) 
							continue;
					}
					
					if ("".equals(attributes.getTypeAsString(index))
							|| "".equals(attributes.getNameAsString(index))) {
						continue;
					}
					
					tmpAttrVector.addElement(attributes.getTypeAsString(index));
					tmpAttrVector.addElement(attributes.getNameAsString(index));
					tmpAttrVector
							.addElement(attributes.getValueAsString(index));
					attrs.addElement(tmpAttrVector);
				}
			} else {
				attrs = setAttributes(this.bNode);
			}
		}
		return attrs;
	}

	/** Sets my attribute value to the attributes specified by the Node.
	 *  Returns a list of lists with type, name, value of attribute members.
	 */
	public Vector<Vector<String>> setAttributes(Node bNode) {
		Vector<Vector<String>> attrs = new Vector<Vector<String>>();
		if (bNode == null)
			return attrs;
		if (bNode.getAttribute() == null)
			return attrs;

		int nattrs = bNode.getAttribute().getNumberOfEntries();
		if (nattrs != 0) {
			for (int i = 0; i < nattrs; i++) {
				Vector<String> attr = new Vector<String>();
				attr.addElement(bNode.getAttribute().getTypeAsString(i));
				attr.addElement(bNode.getAttribute().getNameAsString(i));
				attr.addElement(bNode.getAttribute().getValueAsString(i));
				attrs.addElement(attr);
			}
		}
		return attrs;
	}

	/** Sets my attributes to the attributes specified by the GraphObject*/
	public Vector<Vector<String>> setAttributes(GraphObject obj) {
		return setAttributes((Node) obj);
	}

	/** Sets my used object specified by the Node bNode */
	public void setBasisNode(Node bNode) {
		this.bNode = bNode;
	}

	/** Sets a new basis node */
	public void changeBasisNode(Node newNode) {
		this.bNode = newNode;
		/*
		 * der alte basis knoten darf nicht zerstoert werden, da er vielleicht
		 * noch in einem anderen graphen existiert.
		 */
	}

	/** Sets my position, visibility, selection */
	public void setReps(int nX, int nY, boolean nVisible, boolean nSelect) {
		setXY(nX, nY);
		setVisible(nVisible);
		setSelected(nSelect);
	}

	/**
	 * Makes a copy based on the same basis node.
	 */
	public EdNode copy() {
		EdNode newNode = new EdNode(this.bNode, this.eType);
		newNode.myGraphPanel = this.myGraphPanel;
		newNode.x = this.x;
		newNode.y = this.y;
		newNode.w = this.w;
		newNode.h = this.h;
		return newNode;
	}

	/**
	 * Returns TRUE if the specified point (X, Y) is inside of my shape.
	 */
	public boolean inside(int X, int Y) {
		Rectangle r = new Rectangle(this.x - this.w/2, this.y - this.h/2, this.w, this.h);
		if (r.contains(X, Y))
			return true;
		
		return false;
	}

	public void applyScale(double scale) {
//		System.out.println("EdNode.applyScale::  "+this.itsScale+"   to  "+scale);
		if (scale != this.itsScale) {
			setX((int) ((this.x/this.itsScale) * scale));
			setY((int) ((this.y/this.itsScale) * scale));		
			this.itsScale = scale;
		}
//		System.out.println("EdNode.applyScale::  "+this.itsScale);
	}
	
	public void drawShadowGraphic(Graphics grs) {
		if (this.visible) {
			Graphics2D g = (Graphics2D) grs;
			// save color, font style
			Color lastColor = g.getColor();
			
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);			
			g.setPaint(Color.LIGHT_GRAY);
			g.setStroke(EditorConstants.defaultStroke);
			g.draw(new Rectangle2D.Double(this.x-10, this.y-10, 20, 20));
			// reset font style, color
			g.setFont(EditorConstants.defaultFont);
			g.setPaint(lastColor);
		}
	}
	
	
	/** Draws myself in the graphics specified by the Graphics g */
	public void drawGraphic(Graphics grs) {
//		synchronized (this) 
		{
		if (!this.visible || this.bNode == null || this.bNode.getType() == null) {
			return;
		} 

		this.criticalStyle = this.eGraph.criticalStyle;
		
		Graphics2D g = (Graphics2D) grs;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(EditorConstants.defaultStroke);
		
		// save the old color
		Color lastColor = g.getColor();
		
		myUpdate(g.getFontMetrics());

		if (getType().isIconable()) {
			String fname = getType().imageFileName;
			URL url = ClassLoader.getSystemClassLoader().getResource(fname);
			if (url != null) {
				ImageIcon icon = new ImageIcon(url);
				if (selected) {
					g.setPaint(EditorConstants.selectColor);
					g.fill(new Rectangle2D.Double(this.x - this.w/2 - 2, this.y - this.h/2 - 2,
							this.w + 4, this.h + 4));
				} else if (isCritical()) {
					if (this.criticalStyle == 0) {
						g.setPaint(EditorConstants.criticalColor);	
						g.setStroke(EditorConstants.criticalStroke);
						g.draw(new Rectangle2D.Double(this.x - this.w/2 - 2, this.y - this.h/2 - 2,
								this.w + 4, this.h + 4));
					}
					else { //if (this.criticalStyle == 1) {
						g.setPaint(Color.BLACK);
						g.setStroke(EditorConstants.criticalStroke);
						g.draw(new Rectangle2D.Double(this.x - this.w/2 - 2, this.y - this.h/2 - 2, 
								this.w+4, this.h+4 ));
					} 
				}
				g.setPaint(this.getColor());
				g.drawImage(icon.getImage(), this.x - this.w/2, this.y - this.h/2, null);
				return;
			} 
		}

		if (this.backgroundColor != null && this.backgroundColor != Color.white) {
			g.setPaint(this.backgroundColor);
			g.fill(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w + 6, this.h + 6));
		}

		int sh = getShape();
		
		boolean hiddenObjOfType = this.eGraph.isTypeGraph() 
						&& !this.eType.getBasisType().isObjectOfTypeGraphNodeVisible();
		switch (sh) {
		case EditorConstants.RECT:
			if (selected) {
				g.setPaint(EditorConstants.selectColor);
				g.fill(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
				g.setPaint(this.getColor());
				g.draw(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
			} else if (weakselected) {
				g.setPaint(Color.white);
				g.fill(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
				g.setPaint(EditorConstants.weakselectColor);
				g.draw(new Rectangle2D.Double(this.x - this.w/2-1, this.y - this.h/2-1, this.w+2, this.h+2));
				g.draw(new Rectangle2D.Double(this.x - this.w/2+1, this.y - this.h/2+1, this.w-2, this.h-2));
				g.setPaint(this.getColor());
				g.draw(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
			} else if (hiddenObjOfType) {
				g.setPaint(EditorConstants.hideColor);
				g.fill(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
				g.setPaint(this.getColor());
				g.draw(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
			} else if (isCritical()) {
				if (this.criticalStyle == 0) {
					if (this.eType.filled) 
						g.setPaint(this.getColor());
					else
						g.setPaint(Color.white);
					g.fill(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
					g.setPaint(EditorConstants.criticalColor);	
	//				g.fill(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
	//				g.setPaint(this.getColor());
	//				g.draw(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
					g.setStroke(EditorConstants.criticalStroke);
					g.setFont(EditorConstants.criticalFont);
					g.draw(new Rectangle2D.Double(this.x - this.w/2 - 4, this.y - this.h/2 - 4,
								this.w + 8, this.h + 8));
					if (this.eType.filled) 
						g.setPaint(Color.white);
				}
				else {//if (this.criticalStyle == 1) {
					g.setPaint(Color.BLACK);
					g.setStroke(EditorConstants.criticalStroke);
					g.draw(new Rectangle2D.Double(this.x - this.w/2 -2, this.y - this.h/2 -2, this.w+4, this.h+4));
				} 
			} else if (this.ownColor != null) {
				g.setPaint(this.ownColor);
				g.fill(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
				g.setPaint(Color.white);
			} else if (this.eType.filled) {
				g.setPaint(this.getColor());
				g.fill(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
				g.setPaint(Color.white);
			} else {
				g.setPaint(Color.white);
				g.fill(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
				g.setPaint(this.getColor());
				g.draw(new Rectangle2D.Double(this.x - this.w/2, this.y - this.h/2, this.w, this.h));
			}
			break;
		case EditorConstants.ROUNDRECT:
			if (selected) {
				g.setPaint(EditorConstants.selectColor);
				g.fillRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
				g.setPaint(this.getColor());
				g.drawRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
			}
			else if (weakselected) {
				g.setPaint(Color.white);
				g.fillRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
				g.setPaint(EditorConstants.weakselectColor);
				g.drawRoundRect(this.x - this.w/2-1, this.y - this.h/2-1, this.w+2, this.h+2, 10, 10);
				g.drawRoundRect(this.x - this.w/2+1, this.y - this.h/2+1, this.w-2, this.h-2, 10, 10);
				g.setPaint(this.getColor());
				g.drawRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
			}
			else if (hiddenObjOfType) {
				g.setPaint(EditorConstants.hideColor);
				g.fillRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
				g.setPaint(this.getColor());
				g.drawRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
			}
			else if (isCritical()) {
				if (this.criticalStyle == 0) {
					if (this.eType.filled)
						g.setPaint(this.getColor());
					else
						g.setPaint(Color.white);					
					g.fillRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
					g.setPaint(EditorConstants.criticalColor);
	//				g.fillRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);					
	//				g.setPaint(this.getColor());
	//				g.drawRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
					g.setStroke(EditorConstants.criticalStroke);
					g.setFont(EditorConstants.criticalFont);
					g.drawRoundRect(this.x - this.w/2 -4, this.y - this.h/2 -4, this.w+8, this.h+8, 10, 10);
					if (this.eType.filled)
						g.setPaint(Color.white);
				}
				else { //if (this.criticalStyle == 1) {
					g.setStroke(EditorConstants.criticalStroke);
					g.setPaint(Color.BLACK);
					g.drawRoundRect(this.x - this.w/2 -2, this.y - this.h/2 -2, this.w+4, this.h+4, 10, 10);
				} 
			} else if (this.ownColor != null) {
				g.setPaint(this.ownColor);
				g.fillRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
				g.setPaint(Color.white);
			} else if (this.eType.filled) {
				g.setPaint(this.getColor());
				g.fillRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
				g.setPaint(Color.white);
			} else {
				g.setPaint(Color.white);
				g.fillRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
				g.setPaint(this.getColor());
				g.drawRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
			}
			break;
		case EditorConstants.CIRCLE:
			int d = this.w-2;
			if (selected) {
				g.setPaint(EditorConstants.selectColor);
				g.fillOval(this.x - d/2, this.y - d/2, d, d);
				g.setPaint(this.getColor());
				g.drawOval(this.x - d/2, this.y - d/2, d, d);
			}
			else if (weakselected) {
				g.setPaint(Color.white);
				g.fillOval(this.x - d/2, this.y - d/2, d, d);
				g.setPaint(EditorConstants.weakselectColor);
				g.drawOval(this.x - d/2-1, this.y - d/2-1, d+2, d+2);
				g.drawOval(this.x - d/2+1, this.y - d/2+1, d-2, d-2);
				g.setPaint(this.getColor());
				g.drawOval(this.x - d/2, this.y - d/2, d, d);
			}
			else if (hiddenObjOfType) {
				g.setPaint(EditorConstants.hideColor);
				g.fillOval(this.x - d/2, this.y - d/2, d, d);
				g.setPaint(this.getColor());
				g.drawOval(this.x - d/2, this.y - d/2, d, d);
			}
			else if (isCritical()) {
				if (this.criticalStyle == 0) {
					if (this.eType.filled)
						g.setPaint(this.getColor());
					else
						g.setPaint(Color.white);						
					g.fillOval(this.x - d/2, this.y - d/2, d, d);
					g.setPaint(EditorConstants.criticalColor);
	//				g.fillOval(this.x - d/2, this.y - d/2, d, d);
	//				g.setPaint(this.getColor());
	//				g.drawOval(this.x - d/2, this.y - d/2, d, d);
					g.setStroke(EditorConstants.criticalStroke);
					g.setFont(EditorConstants.criticalFont);
					g.drawOval(this.x - d/2 -4, this.y - d/2 -4, d+8, d+8);
					if (this.eType.filled)
						g.setPaint(Color.white);
				}
				else { //if (this.criticalStyle == 1) {
					g.setStroke(EditorConstants.criticalStroke);
					g.setPaint(Color.BLACK);
					g.drawOval(this.x - d/2 -2, this.y - d/2 -2, d+4, d+4);
				} 
			} else if (this.ownColor != null) {
				g.setPaint(this.ownColor);
				g.fillOval(this.x - d/2, this.y - d/2, d, d);
				g.setPaint(Color.white);
			} else if (this.eType.filled) {
				g.setPaint(this.getColor());
				g.fillOval(this.x - d/2, this.y - d/2, d, d);
				g.setPaint(Color.white);
			} else {
				g.setPaint(Color.white);
				g.fillOval(this.x - d/2, this.y - d/2, d, d);
				g.setPaint(this.getColor());
				g.drawOval(this.x - d/2, this.y - d/2, d, d);
			}
			break;
		case EditorConstants.OVAL:
			d = this.w-2;
			if (selected) {
				g.setPaint(EditorConstants.selectColor);
				g.fillOval(this.x - d/2, this.y - this.h/2, d, this.h);
				g.setPaint(this.getColor());
				g.drawOval(this.x - d/2, this.y - this.h/2, d, this.h);
			}
			else if (weakselected) {
				g.setPaint(Color.white);
				g.fillOval(this.x - d/2, this.y - this.h/2, d, this.h);
				g.setPaint(EditorConstants.weakselectColor);
				g.drawOval(this.x - d/2-1, this.y - this.h/2-1, d+2, this.h+2);
				g.drawOval(this.x - d/2+1, this.y - this.h/2+1, d-2, this.h-2);
				g.setPaint(this.getColor());
				g.drawOval(this.x - d/2, this.y - this.h/2, d, this.h);
			}
			else if (hiddenObjOfType) {
				g.setPaint(EditorConstants.hideColor);
				g.fillOval(this.x - d/2, this.y - this.h/2, d, this.h);
				g.setPaint(this.getColor());
				g.drawOval(this.x - d/2, this.y - this.h/2, d, this.h);
			}
			else if (isCritical()) {
				if (this.criticalStyle == 0) {
					if (this.eType.filled) 
						g.setPaint(this.getColor());
					else
						g.setPaint(Color.white);
					g.fillOval(this.x - d/2, this.y - this.h/2, d, this.h);
					g.setPaint(EditorConstants.criticalColor);
	//				g.fillOval(this.x - d/2, this.y - this.h/2, d, this.h);
	//				g.setPaint(this.getColor());
	//				g.drawOval(this.x - d/2, this.y - this.h/2, d, this.h);
					g.setStroke(EditorConstants.criticalStroke);
					g.setFont(EditorConstants.criticalFont);
					g.drawOval(this.x - d/2 -4, this.y - this.h/2 -4, d+8, this.h+8);
					if (this.eType.filled)
						g.setPaint(Color.white);
				}
				else if (this.criticalStyle == 1) {
					g.setStroke(EditorConstants.criticalStroke);
					g.setPaint(Color.BLACK);
					g.drawOval(this.x - d/2 -2, this.y - this.h/2 -2, d+4, this.h+4);
				}
			} else if (this.ownColor != null) {
				g.setPaint(this.ownColor);
				g.fillOval(this.x - d/2, this.y - this.h/2, d, this.h);
				g.setPaint(Color.white);
			} else if (this.eType.filled) {
				g.setPaint(this.getColor());
				g.fillOval(this.x - d/2, this.y - this.h/2, d, this.h);
				g.setPaint(Color.white);
			} else {
				g.setPaint(Color.white);
				g.fillOval(this.x - d/2, this.y - this.h/2, d, this.h);
				g.setPaint(this.getColor());
				g.drawOval(this.x - d/2, this.y - this.h/2, d, this.h);
			}
			break;
		default:
			break;
		} 

		if (this.errorMode) {
			// if there was an error print in green
			g.setPaint(Color.green);
		}

		// Text		
		if (this.bNode.getType().isAbstract()) 
			g.setFont(new Font("Dialog", Font.ITALIC, g.getFont().getSize()));

		g.setStroke(EditorConstants.defaultStroke);
		drawText(g, this.x, this.y);

		g.setFont(EditorConstants.defaultFont);
		g.setPaint(lastColor);
		}
	}

	/** Updates my width and height */
	public void myUpdate(FontMetrics fm) {
		prepareGraphics(fm);
	}

	/** Erases my graphic */
	public void eraseGraphic(Graphics grs) {
		Color c = grs.getColor();
		grs.setColor(Color.white);
		grs.fillRect(this.x - this.w/2 - 1, this.y - this.h/2 - 1, this.w + 2, this.h + 2);
		grs.setColor(c);
	}

	/**
	 * Implements the AttrViewObserver. Makes update graphics if the attributes
	 * of my used object are changed.
	 */
	public void attributeChanged(AttrViewEvent ev) {
//		 System.out.println("EdNode.attributeChanged: "+ev.getID());		
//		 ((ValueTuple)this.bNode.getAttribute()).showValue();
		 
		if (ev.getID() == AttrEvent.GENERAL_CHANGE // 0
				||ev.getID() == AttrEvent.MEMBER_RETYPED 
				|| ev.getID() == AttrEvent.MEMBER_RENAMED
				|| ev.getID() == AttrEvent.MEMBER_DELETED 
				|| ev.getID() == AttrViewEvent.MEMBER_VISIBILITY 
				|| ev.getID() == AttrViewEvent.MEMBER_MOVED) {
			
			if (ev.getSource().getTupleType().isValid()) {
				this.attrChanged = true;
			}
			
		} else if (ev.getID() == AttrEvent.MEMBER_VALUE_CORRECTNESS // 70
				|| ev.getID() == AttrEvent.MEMBER_VALUE_MODIFIED) { // 80
		
			if (ev.getSource().isValid()) {
				this.attrChanged = true;
				if (this.myGraphPanel != null) {
					if (this.myGraphPanel.isAttrEditorActivated()) {
						if (this.bNode.getContext().getAttrContext() != null) {
							ValueMember val = ((ValueTuple) this.bNode
									.getAttribute()).getValueMemberAt(ev
									.getIndex());
							if (val.isSet() && val.getExpr().isVariable()) {
								ContextView viewContext = (ContextView) ((ValueTuple) val
										.getHoldingTuple()).getContext();
								VarTuple variable = (VarTuple) viewContext
										.getVariables();
								VarMember var = variable.getVarMemberAt(val
										.getExprAsText());
								if (var == null)
									return;
						
								if (this.bNode.getContext().isNacGraph())
									var.setMark(VarMember.NAC);
								else if (this.bNode.getContext().isPacGraph())
									var.setMark(VarMember.PAC);
								else if (viewContext
										.doesAllowComplexExpressions())
									var.setMark(VarMember.RHS);
								else
									var.setMark(VarMember.LHS);
							}
						}
					}
				}
			} else {
				ValueTuple attr = (ValueTuple) this.bNode.getAttribute();
				for (int i = 0; i < attr.getSize(); i++) {
					ValueMember am = (ValueMember) attr.getMemberAt(i);
					if (!am.isValid())
						break;
				}
			}
		}
	}

	public void setGraphPanel(GraphPanel gp) {
		this.myGraphPanel = gp;
	}

	/** Gets the bounding rectangle around myself */
	public Rectangle toRectangle() {
		return new Rectangle(getX() - getWidth()/2, getY() - getHeight()/2,
				getWidth(), getHeight());
	}
	
	/** Prepares my graphics */
	private void prepareGraphics(FontMetrics fm) {
		int h1 = getTextHeight(fm) + 4;
		if (h1 < 20)
			h1 = 20; // min height = 20
		int w1 = getTextWidth(fm) + 6;
		if (w1 < 20)
			w1 = h1; // min width 20
		setWidth(w1);
		setHeight(h1);

		if (getType().isIconable()) {
			// String fname = getType().resourcesPath+getType().imageFileName;
			// URL url = ClassLoader.getSystemClassLoader().getResource(fname);
			String fname = getType().imageFileName;
			URL url = ClassLoader.getSystemClassLoader().getResource(fname);
			// System.out.println("URL: "+url);
			if (url != null) {
				ImageIcon icon = new ImageIcon(url);
				h1 = icon.getIconHeight();
				w1 = icon.getIconWidth();
				setWidth(w1);
				setHeight(h1);
				return;

			}
		}

		int sh = getShape();
		switch (sh) {
		case EditorConstants.RECT:
			break;
		case EditorConstants.ROUNDRECT:
			break;
		case EditorConstants.CIRCLE:
			int d1 = (int) Math.sqrt((double) (this.w * this.w) + (double) (this.h * this.h));
			int d2 = this.w;
			if (this.h > this.w)
				d2 = this.h;
			int d = (d1 + d2)/2;
			setWidth(d);
			setHeight(d);
			break;
		case EditorConstants.OVAL:
			int hor = 0,
			ver = 0;
			hor = (int) Math.sqrt((double) (this.w * this.w) + (double) (this.h * this.h));
			if (this.w == this.h) {
				ver = hor - hor/4;
			} else if (this.h > this.w) {
				ver = hor;
				hor = ver + ver/4;
			} else if (this.w > this.h) {
				ver = hor - hor/4;
			}
			setWidth(hor);
			setHeight(ver);
			break;
		default:
			break;
		}
	}

	protected String getMultiplicityString() {
		String s = "";
		int min = this.bNode.getType().getSourceMin();
		int max = this.bNode.getType().getSourceMax();
//		System.out.println("EdNode.getMultiplicityString:: "+min+"   "+max);
		if (min != -1) {
			s = s.concat(String.valueOf(min));
			s = s.concat("..");
			if (max == -1)
				s = s.concat("*");
		} else { // min == -1
			if (max != -1)
				s = s.concat("0..");
			else
				s = "*";
		}
		if (max != -1) {
			if (min != max)
				s = s.concat(String.valueOf(max));
			else
				s = String.valueOf(max);
		}
		return s;
	}

	private void drawText(Graphics grs, int centerX, int centerY) {
		// System.out.println("EdNode.showText ");
		Graphics2D g = (Graphics2D) grs;
		boolean underlined = false;
		int tx1, ty1;
		FontMetrics fm = g.getFontMetrics();
		// System.out.println("FontWidth (m): "+ fm.stringWidth("m"));
		// System.out.println("FontHeight: "+ fm.getHeight());
		// System.out.println("FontDescent: "+ fm.getDescent());
		// System.out.println("FontAscent: "+ fm.getAscent());
		int tw = getTextWidth(fm);
		int th = getTextHeight(fm);
		int tx = centerX - tw/2;
		int ty = centerY - th/2;
		// System.out.println("h: "+th);
		// type name string
		String typeStr = getTypeString();
		
		if (this.elemOfTG) {
			if (this.bNode.getType().isAbstract()) {
				if (!typeStr.equals(""))
					typeStr = "{" + typeStr + "}";
				else
					typeStr = "{ }";
			}
			String multiplicityStr = getMultiplicityString();
			if (!multiplicityStr.equals("")) {
				tx1 = centerX + tw/2 - fm.stringWidth(multiplicityStr);
				ty1 = ty + fm.getHeight()/2 + fm.getDescent()/2;
				g.drawString(multiplicityStr, tx1, ty1);
			}
		}
		if (!typeStr.equals("")) {
			tx1 = tx;
			ty1 = ty + (fm.getHeight() - fm.getDescent());
			g.drawString(typeStr, tx1, ty1);
			ty = ty + fm.getHeight();
		} else
			ty = ty + fm.getHeight() + fm.getDescent()/4;

		if ((g.getFont().getSize() < 8)
				|| !this.attrVisible)
			return;

		// Attribute anzeigen
		Vector<Vector<String>>attrs = getAttributes();
		if (attrs != null && !attrs.isEmpty()) {
			for (int i = 0; i < attrs.size(); i++) {
				Vector<String> attr = attrs.elementAt(i);
				if (!this.elemOfTG && (attr.elementAt(2).length() != 0)) {
					String attrStr = attr.elementAt(1);
					attrStr = attr.elementAt(1) + "=";
					attrStr = attrStr + attr.elementAt(2);
					if (!underlined) {
						g.drawLine(tx, ty, tx + tw, ty);
						underlined = true;
					}
					ty1 = ty + (fm.getHeight() - fm.getDescent());
					g.drawString(attrStr, tx, ty1);
					ty = ty + fm.getHeight();
				} 
				else if (this.elemOfTG && (attr.elementAt(1) != null)) {
					String attrStr = attr.elementAt(0);
					attrStr = attrStr + "  ";
					attrStr = attrStr + attr.elementAt(1);
					// Type graph: default attr value 
					if (attr.elementAt(2).length() != 0) {
						attrStr = attrStr + "=" + attr.elementAt(2);
					}
					
					if (!underlined) {
						g.drawLine(tx, ty, tx + tw, ty);
						underlined = true;
					}
					ty1 = ty + (fm.getHeight() - fm.getDescent());
					g.drawString(attrStr, tx, ty1);
					ty = ty + fm.getHeight();
				}
			}
		}
	}

	public void drawNameAttrOnly(Graphics grs) {		
//		if (!this.isVisible()) {
		if (!this.visible) {
			return;
		} 
			
		this.criticalStyle = this.eGraph.criticalStyle;
		
		Graphics2D g = (Graphics2D) grs;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		// g.setStroke(stroke);
		g.setStroke(new BasicStroke(2.0f));
		updateNameAttrOnly(g.getFontMetrics());
		g.setPaint(Color.white);
		g.fillRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
		g.setPaint(this.getColor());
		g.drawRoundRect(this.x - this.w/2, this.y - this.h/2, this.w, this.h, 10, 10);
		showNameAttrOnly(g, this.x, this.y);
		g.setStroke(EditorConstants.defaultStroke);
	}

	public void updateNameAttrOnly(FontMetrics fm) {
		Vector<Vector<String>> attrs = getAttributes();
		int nn = 1; // attrs number always 1
		int h1 = 0;
		// die Hoehe einer Zeile
		if (fm == null)
			h1 = 17; // default
		else
			h1 = fm.getHeight();
		// gesamte Hoehe
		if (h1 < 20)
			h1 = 30;

		nn = 6; // default char width
		int w1 = 0;
		if (attrs != null) {
			for (int i = 0; i < attrs.size(); i++) {
				Vector<String> attr = attrs.elementAt(i);
				if (attr.elementAt(1).equals("name")) {
					if (attr.elementAt(2).length() != 0) {
						String tstStr = attr.elementAt(2);
						if (fm == null)
							w1 = nn * tstStr.length() + 6;
						else
							w1 = fm.stringWidth(tstStr) + 6;
					}
				}
			}
		}
		if (w1 < 20)
			w1 = h1; // min width 20
		setWidth(w1);
		setHeight(h1);
	}

	private void showNameAttrOnly(Graphics grs, int centerX, int centerY) {
		// System.out.println("EdNode.showAttrNameOnly ");
		Graphics2D g = (Graphics2D) grs;
		g.setStroke(new BasicStroke(2.0f));
		FontMetrics fm = g.getFontMetrics();
		Vector<Vector<String>> attrs = getAttributes();
		int th = getHeight();
		int tw = getWidth();
		int tx = centerX - tw/2 + 9;
		int ty = centerY - th/2 + 5;
		ty = ty + fm.getHeight(); // + fm.getDescent()/4;
		// Attribute anzeigen
		if (attrs != null && !attrs.isEmpty()) {
			for (int i = 0; i < attrs.size(); i++) {
				Vector<String> attr = attrs.elementAt(i);
				if (attr.elementAt(1).equals("name")) {
					if (attr.elementAt(2).length() != 0) {
						String attrStr = attr.elementAt(2);
						// ty1 = ty+(fm.getHeight()-fm.getDescent());
						g.drawString(
								attrStr.substring(1, attrStr.length() - 1), tx,
								ty);
						return;
					}
				}
			}
		}
	}

	public void XwriteObject(XMLHelper xmlh) {
		if (xmlh.openObject(this.bNode, this)) {
			xmlh.openSubTag("NodeLayout");
			
			int outX = (int) (this.x/this.itsScale);
			int outY = (int) (this.y/this.itsScale);

			xmlh.addAttr("X", outX);
			xmlh.addAttr("Y", outY);

//			System.out.println("EdNode.XwriteObject:: X,Y: "+outX+" , "+outY);
			xmlh.close();
			// LayoutNode speichern:
			if (this.lNode != null) {
				xmlh.addObject("", this.lNode, true);
			}
			xmlh.close();
		}
	}

	public void XreadObject(XMLHelper xmlh) {		
		xmlh.peekObject(this.bNode, this);
					
		if (xmlh.readSubTag("NodeLayout")) {
			this.hasDefaultLayout = true;
			String s = xmlh.readAttr("X");
			if (s.length() == 0) {
				this.x = 20;
			} else {
				this.x = (new Integer(s)).intValue();
			}
			s = xmlh.readAttr("Y");
			if (s.length() == 0) {
				this.y = 20;
			}
			else {
				this.y = (new Integer(s)).intValue();
			}
			xmlh.close();
		} else {
			this.x = 20;
			this.y = 20;
		}

		// layoutNode einlesen:
		xmlh.enrichObject(this.lNode);
		
		xmlh.close();

		if (this.bNode.xyAttr && this.getContext().getBasisGraph().isCompleteGraph()) {
			ValueMember xattr = ((ValueTuple)this.bNode.getAttribute()).getValueMemberAt("thisX");
			if (!xattr.isSet())
				((ValueTuple)this.bNode.getAttribute()).getValueMemberAt("thisX").setExprAsObject(this.x);
			ValueMember yattr = ((ValueTuple)this.bNode.getAttribute()).getValueMemberAt("thisY");
			if (!yattr.isSet())
				((ValueTuple)this.bNode.getAttribute()).getValueMemberAt("thisY").setExprAsObject(this.y);
		}
		
		this.attrVisible = true;
		this.attrChanged = false;
				
	}

	/**
	 * Checks whether the basis Nodes of this EdNode and the specified EdNode
	 * <code>enode</code> are equal.
	 * 
	 * @param enode
	 * @return true, if the basis nodes are equal, otherwise - false
	 */
	public boolean equalByBasisNode(EdNode enode) {
		if (this.getBasisNode().equals(enode.getBasisNode())) {
			return true;
		} 
		return false;
	}

	/**
	 * Searchs through the specified vector for an EdNode with the same ID
	 * number. Such ID is set by the method <code>setNodeID(int)</code>.
	 * 
	 * @param enodes
	 *            nodes to search
	 * @return index of a node with the same ID, if found, otherwise returns -1.
	 */
	public int isInVectorByBasisNode(List<EdNode> enodes) {
		int ret = -1;
		EdNode node;
		for (int i = 0; i < enodes.size(); i++) {
			node = enodes.get(i);
			if (this.getNodeID() == node.getNodeID()) {
				ret = i;
				break;
			}
		}
		return ret;
	}

	public void setNodeID(int id) {
		this.nodeid = id;
	}

	public void setOwnColor(final Color c) {
		this.ownColor = c;
	}
	
	public Color getOwnColor() {
		return this.ownColor;
	}
	
	public int getNodeID() {
		return this.nodeid;
	}

	public void setCluster(Vector<Integer> clus) {
		this.cluster = new Vector<Integer>(clus);
	}

	public Vector<Integer> getCluster() {
		return this.cluster;
	}

	public void calculateCluster(int epsilon, List<EdNode> nodes) {
		EdNode node;
		int xdist, ydist, dist;
		this.oldcluster = this.cluster;
		this.cluster = new Vector<Integer>();
		for (int i = 0; i < nodes.size(); i++) {
			node = nodes.get(i);
			if (!this.equals(node)) {
				xdist = Math.abs(node.getX() - this.getX());
				ydist = Math.abs(node.getY() - this.getY());
				// if(xdist > 0 || ydist > 0)
				{
					dist = (int) Math.round(Math.sqrt((xdist * xdist)
							+ (ydist * ydist)));
					if (dist <= epsilon)
						this.cluster.addElement(new Integer(node.getNodeID()));
				}
			}
		}

	}

	public Vector<Integer> getOldCluster() {
		return this.oldcluster;
	}

}
// $Log: EdNode.java,v $
// Revision 1.61  2010/11/14 12:59:11  olga
// tuning
//
// Revision 1.60  2010/11/12 15:14:53  olga
// tuning
//
// Revision 1.59  2010/11/10 01:14:49  olga
// tuning
//
// Revision 1.58  2010/11/09 16:41:25  olga
// tuning
//
// Revision 1.57  2010/08/25 00:33:06  olga
// tuning
//
// Revision 1.56  2010/03/19 14:46:49  olga
// tuning
//
// Revision 1.55  2010/03/18 18:17:27  olga
// tuning
//
// Revision 1.54  2010/03/17 21:38:36  olga
// tuning
//
// Revision 1.53  2010/03/08 15:40:04  olga
// code optimizing
//
// Revision 1.52  2010/03/04 14:08:22  olga
// code optimizing
//
// Revision 1.51  2010/02/22 15:08:11  olga
// code optimizing
//
// Revision 1.50  2010/01/31 16:42:58  olga
// tuning
//
// Revision 1.49  2009/10/14 07:53:05  olga
// GUI bug fixed
//
// Revision 1.48  2009/09/07 08:59:42  olga
// RuleSequence.copyObjectFlow - improved
//
// Revision 1.47  2009/07/30 14:52:11  olga
// new method - setOwnColor(Color)
// to unset own color use  - setOwnColor(null)
//
// Revision 1.46  2009/06/30 09:50:19  olga
// agg.xt_basis.GraphObject: added: setObjectName(String), getObjectName()
// agg.xt_basis.Node, Arc: changed: save, load the object name
// agg.editor.impl.EdGraphObject: changed: String getTypeString() - contains object name if set
//
// workaround of Applicability of Rule Sequences and Object Flow
//
// Revision 1.45  2009/04/14 09:18:34  olga
// Edge Type Multiplicity check - bug fixed
//
// Revision 1.44  2009/03/30 13:50:49  olga
// some tests
//
// Revision 1.43  2009/03/25 15:19:14  olga
// code tuning
//
// Revision 1.42  2009/02/04 10:11:29  olga
// Termination check and GUI tuning
//
// Revision 1.41  2008/12/04 14:30:02  olga
// Node Type Inheritance: init of parent attributes - bug fixed
//
// Revision 1.40  2008/11/06 08:45:36  olga
// Graph layout is extended by Zest Graph Layout ( eclipse zest plugin)
//
// Revision 1.39  2008/10/29 09:04:04  olga
// new sub packages of the package agg.gui: typeeditor, editor, trafo, cpa, options, treeview, popupmenu, saveload
//
// Revision 1.38  2008/10/15 07:51:22  olga
// Delete attr. member of parent type : error message dialog to warn the user
//
// Revision 1.37  2008/09/11 09:22:25  olga
// Some changes in CPA: new computing of conflicts after an option changed,
// Graph layout of overlapping graphs
//
// Revision 1.36  2008/09/04 07:48:42  olga
// GUI extension: hide nodes, edges
//
// Revision 1.35  2008/07/21 10:03:28  olga
// Code tuning
//
// Revision 1.34  2008/07/17 15:51:50  olga
// GraphEditor - graph scaling tuning
//
// Revision 1.33  2008/07/16 15:52:49  olga
// Import GXL file - bug fixed
//
// Revision 1.32  2008/07/14 07:35:47  olga
// Applicability of RS - new option added, more tuning
// Node animation - new animation parameter added,
// Undo edit manager - possibility to disable it when graph transformation
// because it costs much more time and memory
//
// Revision 1.31  2008/07/02 17:14:36  olga
// Code tuning
//
// Revision 1.30  2008/06/30 10:47:40  olga
// Applicability of Rule Sequence - tuning
// Node animation - first steps
//
// Revision 1.29  2008/06/26 14:18:47  olga
// Graph visualization tuning
//
// Revision 1.28  2008/04/17 10:11:07  olga
// Undo, redo edit and graph layout tuning,
//
// Revision 1.27  2008/04/11 13:29:05  olga
// Memory usage - tuning
//
// Revision 1.26  2008/04/10 10:53:14  olga
// Draw graphics tuning
//
// Revision 1.25  2008/04/07 09:36:50  olga
// Code tuning: refactoring + profiling
// Extension: CPA - two new options added
//
// Revision 1.24  2008/01/23 15:03:18  olga
// Tuning of usability of the gragra editor.
//
// Revision 1.23  2007/12/17 08:33:30  olga
// Editing inheritance relations - bug fixed;
// CPA: dependency of rules - bug fixed
//
// Revision 1.22  2007/11/19 08:48:39  olga
// Some GUI usability mistakes fixed.
// Default values in node/edge of a type graph implemented.
// Code tuning.
//
// Revision 1.21  2007/11/05 09:18:17  olga
// code tuning
//
// Revision 1.20  2007/11/01 09:58:11  olga
// Code refactoring: generic types- done
//
// Revision 1.19  2007/10/11 08:05:04  olga
// Enumeration typing
//
// Revision 1.18  2007/09/24 09:42:33  olga
// AGG transformation engine tuning
//
// Revision 1.17 2007/09/10 13:05:16 olga
// In this update:
// - package xerces2.5.0 is not used anymore;
// - class com.objectspace.jgl.Pair is replaced by the agg own generic class
// agg.util.Pair;
// - bugs fixed in: usage of PACs in rules; match completion;
// usage of static method calls in attr. conditions
// - graph editing: added some new features
//
// Revision 1.16 2007/07/02 08:27:33 olga
// Help docu update,
// Source tuning
//
// Revision 1.15 2007/06/13 08:32:47 olga
// Update: V161
//
// Revision 1.14 2007/04/19 07:52:34 olga
// Tuning of: Undo/Redo, Graph layouter, loading grammars
//
// Revision 1.13 2007/04/11 10:03:35 olga
// Undo, Redo tuning,
// Simple Parser- bug fixed
//
// Revision 1.12 2007/03/28 10:00:27 olga
// - extensive changes of Node/Edge Type Editor,
// - first Undo implementation for graphs and Node/edge Type editing and
// transformation,
// - new / reimplemented options for layered transformation, for graph layouter
// - enable / disable for NACs, attr conditions, formula
// - GUI tuning
//
// Revision 1.11 2007/01/22 08:28:29 olga
// GUI bugs fixed
//
// Revision 1.10 2006/11/01 11:17:29 olga
// Optimized agg sources of CSP algorithm, match usability,
// graph isomorphic copy,
// node/edge type multiplicity check for injective rule and match
//
// Revision 1.9 2006/08/09 07:42:18 olga
// API docu
//
// Revision 1.8 2006/08/02 09:00:57 olga
// Preliminary version 1.5.0 with
// - multiple node type inheritance,
// - new implemented evolutionary graph layouter for
// graph transformation sequences
//
// Revision 1.7 2006/05/29 07:59:41 olga
// GUI, undo delete - tuning.
//
// Revision 1.6 2006/03/01 09:55:46 olga
// - new CPA algorithm, new CPA GUI
//
// Revision 1.5 2005/12/21 14:49:20 olga
// GUI tuning
//
// Revision 1.4 2005/11/07 09:38:07 olga
// Null pointer during retype attr. member fixed.
//
// Revision 1.3 2005/10/10 08:05:16 olga
// Critical Pair GUI and CPA graph
//
// Revision 1.2 2005/09/19 09:12:14 olga
// CPA GUI tuning
//
// Revision 1.1 2005/08/25 11:56:56 enrico
// *** empty log message ***
//
// Revision 1.4 2005/07/13 08:13:37 olga
// Some code optimization only
//
// Revision 1.3 2005/07/11 09:30:20 olga
// This is test version AGG V1.2.8alfa .
// What is new:
// - saving rule option <disabled>
// - setting trigger rule for layer
// - display attr. conditions in gragra tree view
// - CPA algorithm <dependencies>
// - creating and display CPA graph with conflicts and/or dependencies
// based on (.cpx) file
//
// Revision 1.2.2.1 2005/08/16 09:52:55 enrico
// Masking of attributes inherited from parent
//
// Revision 1.2 2005/06/20 13:37:04 olga
// Up to now the version 1.2.8 will be prepared.
//
// Revision 1.1 2005/05/30 12:58:02 olga
// Version with Eclipse
//
// Revision 1.29 2005/03/03 13:48:42 olga
// - Match with NACs and attr. conditions with mixed variables - error corrected
// - save/load class packages written by user
// - PACs : creating T-equivalents - improved
// - save/load matches of the rules (only one match of a rule)
// - more friendly graph/rule editor GUI
// - more syntactical checks in attr. editor
//
// Revision 1.28 2005/02/14 09:27:01 olga
// -PAC;
// -GUI, layered graph transformation anzeigen;
// -CPs.
//
// Revision 1.27 2005/01/28 14:02:32 olga
// -Fehlerbehandlung beim Typgraph check
// -Erweiterung CP GUI / CP Menu
// -Fehlerbehandlung mit identification option
// -Fehlerbehandlung bei Rule PAC
//
// Revision 1.26 2004/12/20 14:53:48 olga
// Changes because of matching optimisation.
//
// Revision 1.25 2004/11/15 11:24:45 olga
// Neue Optionen fuer Transformation;
// verbesserter default Graphlayout;
// Close GraGra mit Abfrage wenn was geaendert wurde statt Delete GraGra
//
// Revision 1.24 2004/10/25 14:24:37 olga
// Fehlerbehandlung bei CPs und Aenderungen im zusammenhang mit
// termination-Modul
// in AGG
//
// Revision 1.23 2004/09/13 10:21:14 olga
// Einige Erweiterungen und Fehlerbeseitigung bei CPs und
// Graph Grammar Transformation
//
// Revision 1.22 2004/07/16 14:25:16 olga
// :::
//
// Revision 1.21 2004/07/16 13:02:02 olga
// TypeGraph OK
//
// Revision 1.20 2004/07/15 11:13:10 olga
// CPs letzter Schliff
//
// Revision 1.19 2004/06/14 12:34:19 olga
// CP Analyse and Transformation
//
// Revision 1.18 2004/06/09 11:32:54 olga
// Attribute-Eingebe/Bedingungen : NAC kann jetzt eigene Variablen und
// Bedingungen
// haben.
// CP Berechnung korregiert.
//
// Revision 1.17 2004/05/26 16:17:40 olga
// Observer / observable
//
// Revision 1.16 2004/04/28 12:46:38 olga
// test CSP
//
// Revision 1.15 2003/12/18 16:26:23 olga
// Copy method and Layout
//
// Revision 1.14 2003/04/10 09:05:24 olga
// Aenderungen wegen serializable Ausgabe
//
// Revision 1.13 2003/04/10 08:50:33 olga
// Tests mit serializable Ausgabe
//
// Revision 1.12 2003/03/20 13:34:11 olga
// Delete TypeGraph eingefuegt
//
// Revision 1.11 2003/03/17 15:33:00 olga
// Kleine Korrektur von Xread, Xwrite
//
// Revision 1.10 2003/03/05 18:24:24 komm
// sorted/optimized import statements
//
// Revision 1.9 2002/11/25 15:03:51 olga
// Arbeit an den Typen.
//
// Revision 1.8 2002/11/14 14:29:39 olga
// Anzeige von Multiplicity -- ein Versuch.
//
// Revision 1.7 2002/11/11 09:42:53 olga
// Nur Testausgaben
//
// Revision 1.6 2002/11/11 09:20:56 olga
// Tests
//
// Revision 1.5 2002/10/02 18:30:42 olga
// XXX
//
// Revision 1.4 2002/09/30 10:08:27 komm
// insert TypeException and type error marks
//
// Revision 1.3 2002/09/30 10:02:29 olga
// Layout
//
// Revision 1.2 2002/09/19 16:21:23 olga
// Layout von Knoten geaendert.
//
// Revision 1.1.1.1 2002/07/11 12:17:08 olga
// Imported sources
//
// Revision 1.21 2001/05/14 12:00:41 olga
// Graph Layout und Graphobject Layout optimiert.
//
// Revision 1.20 2001/03/08 10:53:20 olga
// Das ist Stand nach der AGG GUI Reimplementierung.
//
// Revision 1.19 2000/12/21 09:48:52 olga
// In dieser Version wurden XML und GUI Reimplementierung zusammen gefuehrt.
//
// Revision 1.18 2000/12/07 14:23:36 matzmich
// XML-Kram
// Man beachte: xerces (/home/tfs/gragra/AGG/LIB/Xerces/xerces.jar) wird
// jetzt im CLASSPATH benoetigt.
//
// Revision 1.17.8.5 2000/12/13 13:21:18 olga
// Neu: removeRules()
//
// Revision 1.17.8.4 2000/12/07 14:31:36 olga
// Korrektur der Pfeile.
//
// Revision 1.17.8.3 2000/12/06 16:30:49 olga
// *** empty log message ***
//
// Revision 1.17.8.2 2000/12/06 10:13:21 olga
// Rendering hints in Graphics2D gesetzt.
//
// Revision 1.17.8.1 2000/11/06 09:32:32 olga
// Erste Version fuer neue GUI (Branch reimpl)
//
// Revision 1.17 1999/10/06 14:25:51 olga
// *** empty log message ***
//
// Revision 1.16 1999/09/30 08:58:46 olga
// *** empty log message ***
//
// Revision 1.15 1999/09/27 16:17:27 olga
// *** empty log message ***
//
// Revision 1.14 1999/09/23 14:00:59 olga
// *** empty log message ***
//
// Revision 1.13 1999/09/20 10:58:24 olga
// *** empty log message ***
//
// Revision 1.12 1999/09/20 10:43:55 olga
// *** empty log message ***
//
// Revision 1.11 1999/09/16 13:57:20 olga
// *** empty log message ***
//
// Revision 1.10 1999/09/15 12:54:03 olga
// *** empty log message ***
//
// Revision 1.9 1999/08/03 13:23:08 shultzke
// das Laden und Speichern der Views der Attribute
// wurde in read- und writeObject von EdNode
// und EdArc gelegt. Es ist im Moment noch etwas
// unsauber. Aber es funktioniert.
//
// Revision 1.8 1999/07/28 11:23:41 shultzke
// Events werden richtig benutzt
// Konstruktoren verschoenert
//
// Revision 1.7 1999/07/26 10:25:09 shultzke
// Views koennen zwar benutzt werden. Sie werden
// aber noch nicht nach dem Laden rekonstruiert.
//
