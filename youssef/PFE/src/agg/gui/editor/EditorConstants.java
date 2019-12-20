/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
// $Id: EditorConstants.java,v 1.8 2010/11/28 22:13:07 olga Exp $

package agg.gui.editor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

/**
 * 
 * @author $Author: olga $
 * @version $ID
 */
public class EditorConstants {


	// Edit mode constants
	public static final int VIEW = 9;

	public static final int DRAW = 11;

	public static final int ANIMATION = 110;
	
	public static final int NODE = 112;

	public static final int ARC = 113;

	public static final int ATTRIBUTES = 114;

	public static final int MAP = 115;

	public static final int UNMAP = 116;

	public static final int REMOVE_MAP = 1161;

	public static final int MAPSEL = 117;

	public static final int UNMAPSEL = 118;

	public static final int REMOVE_MAPSEL = 1181;

	public static final int IMAGE = 119;

	public static final int SET_PARENT = 1201;

	public static final int UNSET_PARENT = 1202;

	public static final int SELECT = 12;

	public static final int SELECT_ALL = 121;

	public static final int DESELECT_ALL = 122;

	public static final int MOVE = 13;

	public static final int SYNCHRON_MOVE = 131;

	public static final int STRAIGHT = 14;

	public static final int COPY = 15;

	public static final int COPY_ARC = 151;
	
	public static final int PASTE = 152;

	public static final int DELETE = 16;

	public static final int ATTRS = 17;

	public static final int TYPECHANGE = 171;

	public static final int TYPE = 18;

	public static final int TYPE_INPUT = 181;

	public static final int UPDATE_GRAPHICS = 19;

	public static final int UPDATE_TYPES = 191;

	public static final int INTERFACE_MODE = 20;

	public static final int INTERFACE_SELECT = 21;

	public static final int INTERFACE_CLOSE = 22;

	public static final int IDENTIC_RULE = 30;

	public static final int INTERACT_RULE = 31;

	public static final int REMOVE_RULE = 32;

	public static final int CREATE_MATCH = 40;

	public static final int AUTOMATIC_MATCH = 41;

	public static final int INTERACT_MATCH = 42;

	public static final int REMOVE_MATCH = 43;

	public static final int DESTROY_MATCH = 44;

	// Node shape constants
	public static final int NODE_SHAPE = 50;

	public static final int RECT = 51;

	public static final int CIRCLE = 52;

	public static final int OVAL = 53;

	public static final int ROUNDRECT = 54;

	public static final int ICON = 55;

	// Arc shape constants
	public static final int ARC_SHAPE = 60;

	public static final int SOLID = 61;

	public static final int DASH = 62;

	public static final int DOT = 63;

	// Color constants
	public static final int COLOR = 70;

	// NAC
	public static final int IDENTIC_NAC = 80;

	public static final int INTERACT_NAC = 81;

	public static final int REMOVE_NAC = 82;

	// PAC
	public static final int IDENTIC_PAC = 800;

	public static final int INTERACT_PAC = 801;

	public static final int REMOVE_PAC = 802;

	// (Nested)AC
	public static final int IDENTIC_AC = 8000;

	public static final int INTERACT_AC = 8010;

	public static final int REMOVE_AC = 8020;
	
	// ATTR_CONTEX
	public static final int ATTR_CONTEXT = 90;

	public static final int ATTR_RULE_CONTEXT = 91;

	public static final int ATTR_MATCH_CONTEXT = 92;

	// Transformation
	public static final int PRIORITY = 100;

	public static final int START = 101;

	public static final int STOP = 1011;

	public static final int AUTOMATICALLY = 1012;

	public static final int MANUALLY = 1013;

	public static final int OPTIONS = 1014;

	public static final int NEXT_COMPLETION = 1015;

	public static final int NEXT_RULE = 1016;

	public static final int STEP = 1008;

	public static final int STOP_MARK = 1009;

	// File
	public static final int NEW = 102;

	public static final int GRAGRA = 1021;

	public static final int RULE = 1022;

	public static final int NAC = 1023;

	public static final int CONTROL = 1024;

	public static final int SAVE = 1025;

	public static final int SAVE_AS = 1026;

	public static final int OPEN = 1027;

	public static final int FILE_DELETE = 1028;

	public static final int QUIT = 0;

	public static final int HELP = 10;

	public static final int ABOUT = 103;

	
	// Font constants
	public static final String FONT_NAME = "Dialog"; // "Helvetica-Bold";

	public static final int FONT_STYLE = Font.PLAIN; // Font.BOLD;

	public static final int FONT_SIZE = 12; // 14;

	public static final Font defaultFont = new Font(Font.DIALOG, Font.PLAIN, 12);
	
	public static final Font criticalFont = new Font(Font.DIALOG, Font.BOLD, 12);
	
	public static final Color selectColor = Color.green;
	
	public static final Color weakselectColor = Color.gray;

	public static final Color criticalColor = new Color(0,155,55); //Color.green;
		
	public static final BasicStroke criticalStroke = new BasicStroke(5.0f);
			
	public static final BasicStroke criticalColorStroke = new BasicStroke(4.0f);
	
	public final static BasicStroke defaultStroke = new BasicStroke(1.0f);
	
	public final static BasicStroke boldStroke = new BasicStroke(3.0f);
	
	public static final Color backgroundColor = Color.white;
	
	public static final Color hideColor = new Color(153, 153, 153);
	
	
	public static final String getModeOfID(int id) {
		switch (id) {
		case 11: return "Draw";
		case 13: return "Move";
		case 12: return "Select";
		case 17: return "Attributes";
		case 115: return "Map";
		case 116: return "Unmap";
		}
		return "";
	}
	
}
