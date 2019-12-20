/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.attribute;

/**
 * Attribute event interface for delivering information about attribute changes
 * to clients.
 * 
 * @see agg.attribute.AttrTuple
 * 
 * @version $Id: AttrEvent.java,v 1.5 2008/09/22 13:12:14 olga Exp $
 * @author $Author: olga $
 */
public interface AttrEvent {

	/** Change not specified. (The event receiver should update the whole tuple */
	public static final int GENERAL_CHANGE = 0;

	/** A new member was added. */
	public static final int MEMBER_ADDED = 10;

	/** A member was deleted. */
	public static final int MEMBER_DELETED = 20;

	/** A member to delete. */
	public static final int MEMBER_TO_DELETE = 21;
	
	/** A member was modified, no further specification. */
	public static final int MEMBER_MODIFIED = 40;

	/** A member was renamed. */
	public static final int MEMBER_RENAMED = 50;

	/** A member was retyped. */
	public static final int MEMBER_RETYPED = 60;

	/** The value of an attribute was modified. */
	public static final int MEMBER_VALUE_MODIFIED = 70;

	/** The state of correctness of a member value has changed. */
	public static final int MEMBER_VALUE_CORRECTNESS = 80;

	public static final int MEMBER_DISABLED = 90;

	public static final int MEMBER_MARK = 91;

	/**
	 * The highest id value for this interface. Extending interfaces must not
	 * have id constants below this value.
	 */
	public static final int ATTR_EVENT_MAX_ID = 200;

	// Public Methods

	/** Getting the originator of the event. */
	public AttrTuple getSource();

	/** Getting the message id. */
	public int getID();

	/** Getting the first position index. */
	public int getIndex();

	/** Getting the first position index. */
	public int getIndex0();

	/** Getting the second position index. */
	public int getIndex1();
	
	/** Getting the changed attribute member of the event. 
	 * Returns null when <code>getIndex0() != getIndex1()</code>.
	 */
	public AttrMember getAttrMember();
}
/*
 * $Log: AttrEvent.java,v $
 * Revision 1.5  2008/09/22 13:12:14  olga
 * new AttrEvent: MEMBER_TO_DELETE
 *
 * Revision 1.4  2007/09/10 13:05:31  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.3 2007/03/28 10:01:14 olga - extensive
 * changes of Node/Edge Type Editor, - first Undo implementation for graphs and
 * Node/edge Type editing and transformation, - new / reimplemented options for
 * layered transformation, for graph layouter - enable / disable for NACs, attr
 * conditions, formula - GUI tuning
 * 
 * Revision 1.2 2005/10/24 09:04:49 olga GUI tuning
 * 
 * Revision 1.1 2005/08/25 11:56:55 enrico *** empty log message ***
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.4 2004/06/09 11:32:53 olga Attribute-Eingebe/Bedingungen : NAC
 * kann jetzt eigene Variablen und Bedingungen haben. CP Berechnung korregiert.
 * 
 * Revision 1.3 2004/04/15 10:49:47 olga Kommentare
 * 
 * Revision 1.2 2002/09/23 12:23:45 komm added type graph in xt_basis, editor
 * and GUI
 * 
 * Revision 1.1.1.1 2002/07/11 12:16:55 olga Imported sources
 * 
 * Revision 1.3 2000/04/05 12:06:45 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 */
