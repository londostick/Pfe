/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.parser.event;

import java.util.EventObject;

/**
 * The event is used to show some information at the status bar of AGG
 * 
 * @version $Id: StatusMessageEvent.java,v 1.3 2010/09/23 08:21:12 olga Exp $
 * @author $Author: olga $
 */
@SuppressWarnings("serial")
public class StatusMessageEvent extends EventObject {

	/**
	 * The label
	 * 
	 * @serial All events are serializable.
	 */
	private String label;

	/**
	 * The message
	 * 
	 * @serial All events are serializable
	 */
	private String message;

	/**
	 * This is the main constructor. It is possible to specify a text for the
	 * green box and a message for the status bar. If the empty string is set
	 * the field is unchanged.
	 * 
	 * @param source
	 *            the source of the event
	 * @param theLabel
	 *            the little green box
	 * @param theMessage
	 *            the message for the status bar
	 */
	public StatusMessageEvent(Object source, String theLabel, String theMessage) {
		super(source);
		this.label = theLabel;
		this.message = theMessage;
	}

	/**
	 * Sets the text for the little green box
	 * 
	 * @param source
	 *            the source of the event
	 * @param theLabel
	 *            the little green box
	 */
	public StatusMessageEvent(Object source, String theLabel) {
		this(source, theLabel, "");
	}

	/**
	 * sets only the message at the status bar
	 * 
	 * @param theMessage
	 *            the massege for the status bar
	 * @param source
	 *            the source of the event
	 */
	public StatusMessageEvent(String theMessage, Object source) {
		this(source, "", theMessage);
	}

	/**
	 * let you know what the message is
	 * 
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * shows the little text of the green box
	 * 
	 * @return the green little box text
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * makes a human readable text from this object
	 * 
	 * @return a readable text
	 */
	public String toString() {
		String result = super.toString();
		result += " \"" + getLabel() + "\"";
		result += " \"" + getMessage() + "\"";
		return result;
	}
}
/*
 * $Log: StatusMessageEvent.java,v $
 * Revision 1.3  2010/09/23 08:21:12  olga
 * tuning
 *
 * Revision 1.2  2007/09/10 13:05:50  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.1 2005/08/25 11:56:59 enrico ***
 * empty log message ***
 * 
 * Revision 1.1 2005/05/30 12:58:04 olga Version with Eclipse
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:21 olga Imported sources
 * 
 * Revision 1.2 2001/03/08 11:03:15 olga er Anbindung gema Stand nach AGG GUI
 * Reimplementierung. Stand nach der AGG GUI Reimplementierung.Das ist Stand
 * nach der AGG GUI Reimplementierung und Parser Anbindung.
 * 
 * Revision 1.1.2.3 2001/01/28 13:14:50 shultzke API fertig
 * 
 * Revision 1.1.2.2 2000/11/14 14:01:41 shultzke Kommentare added
 * 
 * Revision 1.1.2.1 2000/08/10 12:22:31 shultzke Einige Klassen wurden
 * umbenannt. Alle Events sind in ein eigenes Eventpackage geflogen.
 * 
 * Revision 1.1.2.1 2000/07/09 17:12:46 shultzke grob die GUI eingebunden
 * 
 */
