/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
/**
 * 
 */
package agg.layout.evolutionary;

import java.awt.Point;

import agg.editor.impl.EdNode;
import agg.util.XMLHelper;
import agg.util.XMLObject;

/**
 * This class contains information about a node, such as actual position, age,
 * repulsive force and some more, that are used by Springembedder Layout.
 */
public class LayoutNode implements XMLObject {

	private EdNode enode;

	private Point akt;

	private Point opt;

	private Point sim;

	private Point kom;

	private int age;

	private int realage;

	private int force;

	private int zone;

	private int distx;

	private int disty;

	private int dist;

	private boolean overlap;

	private boolean frozen;

	private boolean frozenAsDefault;

	/**
	 * Initialize the layout information of the specified EdNode by default
	 * values.
	 */
	public LayoutNode(EdNode e) {
		this.enode = e;
		this.akt = new Point(e.getX(), e.getY());
		this.opt = new Point(0, 0);
		this.sim = new Point(0, 0);
		this.kom = new Point(0, 0);
		this.age = 0;
		this.realage = 0;
		this.force = 10;
		this.zone = Math.max(Math.max(e.getHeight(), e.getWidth()) * 2, 50);// 50
		// als
		// minimalzone
		// willkuerlich
		this.dist = 0;
		this.distx = 0;
		this.disty = 0;
		this.overlap = false;
	}

	public void dispose() {
		this.enode = null;
	}
	
	/**
	 * Initialize the layout information of the specified EdArc by specified
	 * parameters.
	 * 
	 * @param e
	 *            the node which will get this layout
	 * @param x
	 *            X of the node aktual position
	 * @param y
	 *            Y of the node aktual position
	 * @param a
	 *            age of the node
	 * @param f
	 *            repulsive force of der node
	 * @param zone
	 *            security area around the node
	 */
	public LayoutNode(EdNode e, int x, int y, int a, int f, int zone) {
		this.enode = e;
		this.akt = new Point(x, y);
		this.age = a;
		this.realage = a;
		this.force = f;
		this.opt = new Point(0, 0);
		this.sim = new Point(0, 0);
		this.kom = new Point(0, 0);
		this.zone = Math.max(zone, 50);
		this.dist = 0;
		this.distx = 0;
		this.disty = 0;
	}

	public EdNode getEdNode() {
		return this.enode;
	}

	public void setAkt(Point a) {
		this.akt = a;
	}

	public Point getAkt() {
		return this.akt;
	}

	public void setOpt(Point o) {
		this.opt = o;
	}

	public Point getOpt() {
		return this.opt;
	}

	public void setSim(Point s) {
		this.sim = s;
	}

	public Point getSim() {
		return this.sim;
	}

	public void setKom(Point k) {
		this.kom = k;
	}

	public Point getKom() {
		return this.kom;
	}

	public void setForce(int f) {
		this.force = f;
	}

	public int getForce() {
		return this.force;
	}

	public void setFrozen(boolean b) {
		this.frozen = b;
	}

	public boolean isFrozen() {
		return this.frozen;
	}

	public void setFrozenByDefault(boolean b) {
		this.frozenAsDefault = b;
	}

	public boolean isFrozenByDefault() {
		return this.frozenAsDefault;
	}

	public void setAge(int a) {
		this.age = a;
	}

	public int getAge() {
		return this.age;
	}

	/*
	 * hier wird das Alter eines knoten um eins erhoeht Dabei wird ueberprueft,
	 * ob der knoten zwischenzeitlich kuenstlich gealtert ist. Ist das der Fall
	 * wird nur das reale alter des knotens erhoeht. das heisst der knoten
	 * altert solange nicht mehr, bis das reale alter das kuenstlich
	 * hochgesetzte alter erreicht hat.
	 * 
	 */
	public void incAge() {
		// System.out.println("LayoutNode.incAge:: before:: age: "+age+"
		// realage: "+realage);
		if (this.age == this.realage) {
			this.age++;
			this.realage++;
		} else if (this.age > this.realage) {
			this.realage++;
		} else {// der fall, dass das alter eines knotens kleiner als sein
			// realage ist,
			// sollte eigentlich nie vorkommen, wird hier aber
			// sichherheitshalber trotzdem betrachtet
			this.realage++;
			this.age = this.realage;
		}
		// System.out.println("LayoutNode.incAge:: after:: age: "+age+" realage:
		// "+realage);
	}

	public void setZone(int z) {
		this.zone = z;
	}

	public int getZone() {
		return this.zone;
	}

	public void setDist(int d) {
		this.dist = d;
	}

	public int getDist() {
		return this.dist;
	}

	public void setDistX(int d) {
		this.distx = d;
	}

	public int getDistX() {
		return this.distx;
	}

	public void setDistY(int d) {
		this.disty = d;
	}

	public int getDistY() {
		return this.disty;
	}

	public boolean isOverlapping() {
		return this.overlap;
	}

	public void setOverlap() {
		this.overlap = true;
	}

	public void unsetOverlap() {
		this.overlap = false;
	}

	public void XwriteObject(XMLHelper h) {
		if (h.openObject(this.enode.getBasisNode(), this)) {
			h.openSubTag("additionalLayout");
			if (this.frozen)
				h.addAttr("frozen", "true");
			else
				h.addAttr("frozen", "false");
			h.addAttr("age", this.age);
			h.addAttr("force", this.force);
			h.addAttr("zone", this.zone);
			// h.addAttr("akt_x",this.akt.x);
			// h.addAttr("akt_y",this.akt.y);
			// h.addAttr("opt_x",this.opt.x);
			// h.addAttr("opt_y",this.opt.y);
			// h.addAttr("sim_x",this.sim.x);
			// h.addAttr("sim_y",this.sim.y);
			// h.addAttr("kom_x",this.kom.x);
			// h.addAttr("kom_y",this.kom.y);
			h.close();
			h.close();
		}

	}

	public void XreadObject(XMLHelper h) {
		String s;
		h.peekObject(this.enode.getBasisNode(), this);
		if (h.readSubTag("additionalLayout")) {
			s = h.readAttr("frozen");
			this.frozen = true;
			if (s.equals("false")) {
				this.frozen = false;
			}
			s = h.readAttr("age");
			if (s == null || s.length() == 0) {
				this.age = 0;
			} else {
				this.age = Integer.parseInt(s);
			}
			s = h.readAttr("force");
			if (s == null || s.length() == 0) {
				this.force = 0;
			} else {
				this.force = Integer.parseInt(s);
			}
			s = h.readAttr("zone");
			if (s == null || s.length() == 0) {
				this.zone = 50;
			} else {
				this.zone = Integer.parseInt(s);
			}
			s = h.readAttr("akt_x");
			if (s == null || s.length() == 0) {
				this.akt.x = this.enode.getX();
			} else {
				this.akt.x = Integer.parseInt(s);
			}
			s = h.readAttr("akt_y");
			if (s == null || s.length() == 0) {
				this.akt.y = this.enode.getY();
			} else {
				this.akt.y = Integer.parseInt(s);
			}
			s = h.readAttr("opt_x");
			if (s == null || s.length() == 0) {
				this.opt.x = 0;
			} else {
				this.opt.x = Integer.parseInt(s);
			}
			s = h.readAttr("opt_y");
			if (s == null || s.length() == 0) {
				this.opt.y = 0;
			} else {
				this.opt.y = Integer.parseInt(s);
			}
			s = h.readAttr("sim_x");
			if (s == null || s.length() == 0) {
				this.sim.x = 0;
			} else {
				this.sim.x = Integer.parseInt(s);
			}
			s = h.readAttr("sim_y");
			if (s == null || s.length() == 0) {
				this.sim.y = 0;
			} else {
				this.sim.y = Integer.parseInt(s);
			}
			s = h.readAttr("kom_x");
			if (s == null || s.length() == 0) {
				this.kom.x = 0;
			} else {
				this.kom.x = Integer.parseInt(s);
			}
			s = h.readAttr("kom_y");
			if (s == null || s.length() == 0) {
				this.kom.y = 0;
			} else {
				this.kom.y = Integer.parseInt(s);
			}

			this.frozen = (this.age == 0)? true: false;
			h.close();
		}
		h.close();
	}

}
