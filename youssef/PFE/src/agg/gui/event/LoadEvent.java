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

import java.awt.Component;
import java.util.EventObject;

import agg.editor.impl.EdGraGra;
import agg.gui.saveload.GraGraLoad;

/**
 * A LoadEvent is used to notify the loading state of the loaded gragra or
 * errors.
 * 
 * @author olga
 * @version $ID:$
 */
@SuppressWarnings("serial")
public class LoadEvent extends EventObject {

	public static final int LOAD = 0;

	public static final int PROGRESS_BEGIN = 1;

	public static final int PROGRESS_FINISHED = 2;

	public static final int SECURITY_ERROR = 3;

	public static final int STREAM_ERROR = 4;

	public static final int CLASS_NOT_FOUND_ERROR = 5;

	public static final int INVALID_CLASS_ERROR = 6;

	public static final int DATA_ERROR = 7;

	public static final int IO_ERROR = 8;

	public static final int UNKNOWN_ERROR = 9;

	public static final int EMPTY_ERROR = 10;

	public static final int STACK_OVERFLOW_ERROR = 11;

	public static final int LOADED = 12;

	public LoadEvent(Object source, int key, String fileName) {
		super(source);
		if (source instanceof GraGraLoad)
			this.gragra = ((GraGraLoad) source).getGraGra();
		this.msgkey = key;
		this.name = fileName;
	}

	public LoadEvent(Object source, int key, Object obj, String addMsg) {
		this(source, key, "");
		// System.out.println("LoadEvent : this.msgkey: "+this.msgkey);
		if (obj instanceof String) {
			this.name = (String) obj;
			// System.out.println("File: "+this.name);
		} else if (obj instanceof Component) {
			this.component = (Component) obj;
			// System.out.println("ProgressBar: "+this.component);
		} else {
			this.name = "";
			this.component = null;
		}
		this.msg1 = addMsg;
	}

	public String getFileName() {
		return this.name;
	}

	public int getMsg() {
		return this.msgkey;
	}

	public String getMessage() {
		if (this.msgkey == LOAD)
			this.msg = "Please wait. Loading ....";
		else if (this.msgkey == LOADED)
			this.msg = "File  " + this.name + "  is loaded.";
		else if (this.msgkey == PROGRESS_BEGIN)
			this.msg = "";
		else if (this.msgkey == PROGRESS_FINISHED)
			this.msg = "";
		else if (this.msgkey == EMPTY_ERROR)
			this.msg = "Empty file name. Loading is failed or canceled.";
		else if (this.msgkey == CLASS_NOT_FOUND_ERROR)
			this.msg = "Could not find file  " + this.name + ".";
		else if (this.msgkey == INVALID_CLASS_ERROR)
			this.msg = "Invalid class in file " + this.name + "  " + this.msg1;
		else if (this.msgkey == SECURITY_ERROR)
			this.msg = "Security problems while loading file  " + this.name + "  " + this.msg1;
		else if (this.msgkey == STREAM_ERROR)
			this.msg = "Corrupt stream from file  " + this.name + ".";
		else if (this.msgkey == IO_ERROR)
			this.msg = "Error occured while loading file  " + this.name + "  " + this.msg1
					+ ".";
		else if (this.msgkey == DATA_ERROR)
			this.msg = "Optional data in file  " + this.name + "  " + this.msg1 + ".";
		else if (this.msgkey == STACK_OVERFLOW_ERROR)
			this.msg = "Stack overflow error occured while loading file  " + this.name
					+ "  " + this.msg1 + ".";

		else
			this.msg = this.msg1;
		return this.msg;
	}

	public Component getUsedComponent() {
		return this.component;
	}

	public EdGraGra getGraGra() {
		return this.gragra;
	}

	private int msgkey;

	private String msg;

	private String msg1;

	private String name;

	private EdGraGra gragra;

	private Component component;

}
