/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.event;

import java.util.EventObject;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import agg.editor.impl.EdGraGra;
import agg.editor.impl.EdGraph;
import agg.editor.impl.EdNAC;
import agg.editor.impl.EdRule;
import agg.gui.treeview.nodedata.GraGraTreeNodeData;

/**
 * A TreeViewEvent is used to notify the state of the GraGraTreeView.
 * 
 * @author $Author: olga $
 * @version $ID:$
 */
@SuppressWarnings("serial")
public class TreeViewEvent extends EventObject {

	public static final int ERROR = -1;

	public static final int NEW = 0;

	public static final int SAVE = 1;

	public static final int LOAD = 2;

	public static final int SAVED = 3;

	public static final int LOADED = 4;

	public static final int RELOADED = 5;

	public static final int SELECT = 6;

	public static final int SELECTED = 61;

	public static final int DELETED = 7;
	
	public static final int WANT_DELETE = 71;
	
	public static final int RULE_DELETED = 72;
	
	public static final int RULE_ADDED = 73;
	
	public static final int RULE_COPY = 74;
	
	public static final int SHOW = 8;

	public static final int HIDE = 9;

	public static final int PRINT = 10;

	public static final int EMPTY = 11;

	public static final int CONVERT_STEP = 12;

	public static final int CHECK = 13;

	public static final int CHECK_DONE = 14;

	public static final int CONVERTED = 15;

	public static final int TRY_RESET = 16;

	public static final int RESET_GRAPH = 160;

	public static final int TRY_IMPORT = 17;

	public static final int IMPORT_TYPE_GRAPH = 170;

	public static final int IMPORT_GRAPH = 171;

	public static final int ADD_IMPORT_GRAPH = 172;

	public static final int IMPORT_RULE = 173;

	public static final int IMPORT_ATOMIC = 174;
	
	public static final int IMPORT_CONSTRAINT = 175;
	
	public static final int CHECK_RULE_APPLICABILITY = 18;

	public static final int DISMISS_RULE_APPLICABILITY = 181;

	public static final int GRAPH_CHANGED = 19;

	public static final int LAYERED = 20;

	public static final int TYPE_ERROR = 21;

	public static final int NO_TYPE_ERROR = 211;
	
	public static final int UNDO_DELETE = 22;

	public static final int EXPORT_JPEG = 23;

	public static final int TRANSFER_SHORTKEY = 24;

	public static final int BACKGROUND_CLICK = 25;
	
	public static final int RULE_LAYER = 26;
	
	public static final int RULE_PRIORITY = 27;
	 
	public static final int INPUT_PARAMETER_NOT_SET = 28;

	public static final int REFRESH_GRAGRA = 29;
	
	public static final int RULE_SEQUENCE = 30;
	
	public static final int RULE_SEQUENCE_SHOWN = 31;
	
	public static final int RULE_SEQUENCE_DELETED = 32;
	
	public static final int EXIT = 999;
	
	
	
	public TreeViewEvent(Object source, int key) {
		super(source);
		this.msgkey = key;
		this.msg = "";
	}

	public TreeViewEvent(Object source, int key, String message) {
		this(source, key);
		this.msg = message;
	}

	public TreeViewEvent(Object source, int key, Object anObject) {
		this(source, key);
		this.obj = anObject;
		if (anObject instanceof TreePath) {
			this.data = setData((TreePath) anObject);
		}
		this.msg = "";
	}

	public void dispose() {
		this.data = null;
		this.obj = null;
	}
	
	public int getMsg() {
		return this.msgkey;
	}

	public GraGraTreeNodeData getData() {
		return this.data;
	}

	public void setMessage(String newm) {
		this.msg = newm;
	}

	public String getMessage() {
		return this.msg;
	}

	public Object getObject() {
		return this.obj;
	}

	/** Returns an EdGraGra as the used object of the TreeNode */
	public EdGraGra getGraGra() {
		return this.data.getGraGra();
	}

	/** Returns an EdGraph as the used object of the TreeNode */
	public EdGraph getGraph() {
		return this.data.getGraph();
	}

	/** Returns an EdRule as the used object of the TreeNode */
	public EdRule getRule() {
		return this.data.getRule();
	}

	/** Returns an EdNAC as the used object of the TreeNode */
	public EdNAC getNAC() {
		return this.data.getNAC();
	}

	private GraGraTreeNodeData setData(TreePath path) {
		GraGraTreeNodeData d = (GraGraTreeNodeData) ((DefaultMutableTreeNode) path
				.getLastPathComponent()).getUserObject();
		if (path.getPath().length > 1) { // != Root
		}
		return d;
	}

	private int msgkey;

	private String msg;

	private Object obj;
	
	private GraGraTreeNodeData data;	

}
