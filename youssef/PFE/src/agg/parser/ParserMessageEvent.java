/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.parser;

//****************************************************************************+
/**
 * ParserMessageEvent sends messages.
 * 
 * @author $Author: olga $ Parser Group
 * @version $Id: ParserMessageEvent.java,v 1.4 2010/09/23 08:25:00 olga Exp $
 */
@SuppressWarnings("serial")
public class ParserMessageEvent extends ParserEvent {

	int key = -1;

	/**
	 * This constructor is inherited. The message must be set.
	 * 
	 * @param source
	 *            The source of the event.
	 */
	public ParserMessageEvent(Object source) {
		this(source, "");
	}

	/**
	 * Creates a new message event to inform about interessting stuff.
	 * 
	 * @param _message
	 *            The message.
	 * @param source
	 *            The source of the event.
	 */
	public ParserMessageEvent(Object source, String _message) {
		super(source);
		setMessage(_message);
	}

	public ParserMessageEvent(Object source, int _key, String _message) {
		super(source);
		this.key = _key;
		setMessage(_message);
	}

	public int getKey() {
		return this.key;
	}

}

/*
 * End of ParserMessageEvent.java
 * ---------------------------------------------------------------------- $Log:
 * ParserMessageEvent.java,v $ Revision 1.2 2007/03/28 10:00:57 olga - extensive
 * changes of Node/Edge Type Editor, - first Undo implementation for graphs and
 * Node/edge Type editing and transformation, - new / reimplemented options for
 * layered transformation, for graph layouter - enable / disable for NACs, attr
 * conditions, formula - GUI tuning
 * 
 * Revision 1.1 2005/08/25 11:56:57 enrico *** empty log message ***
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:24 olga Imported sources
 * 
 * Revision 1.2 2001/03/08 10:42:53 olga Die Parser Version aus parser branch
 * wurde in Head uebernommen.
 * 
 * Revision 1.1.2.1 2001/01/28 13:14:57 shultzke API fertig
 * 
 * Revision 1.1 2000/06/13 08:57:41 shultzke Initial version, very alpha
 * 
 * Revision 1.8 1999/09/14 10:52:39 shultzke Kommentare hinzugefuegt
 * 
 * Revision 1.7 1999/09/08 17:36:25 shultzke Check fuer 3a implementiert und
 * etwas getestet
 * 
 * Revision 1.6 1999/07/20 19:29:48 shultzke es wurden nur fuer javadoc einige
 * tags hizugfuegt
 * 
 * Revision 1.5 1999/07/20 10:04:55 shultzke diese klassen sind nicht mehr
 * abstrakt
 * 
 * Revision 1.4 1999/07/11 09:22:45 shultzke *** empty log message ***
 * 
 * Revision 1.3 1999/07/04 18:48:11 shultzke Docu erneuert. Events
 * implementiert.
 * 
 * Revision 1.2 1999/06/30 21:24:14 shultzke added rcs key and tried to check in
 * remote
 */
