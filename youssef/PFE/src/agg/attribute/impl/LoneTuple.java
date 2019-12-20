/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.attribute.impl;

/**
 * A tuple whose type is not shared by others. Provides functionality for
 * reordering of its members with the help of FormForcingTuple. To be extended
 * by VarTuple and CondTuple.
 * 
 * @author $Author: olga $
 * @version $Id: LoneTuple.java,v 1.4 2007/09/24 09:42:34 olga Exp $
 */

@SuppressWarnings("serial")
public class LoneTuple extends ValueTuple {

	public LoneTuple(AttrTupleManager manager, ContextView context,
			ValueTuple parent) {
		super(manager, null, context, null);

		DeclTuple parentType = (parent == null ? null : parent.getTupleType());
		DeclTuple t = new DeclTuple(manager, parentType);
		t.loneDeclaration = true;
		setType(t);
		assignParent(parent);

		setForm(manager.getFixedViewSetting());

	}
}
/*
 * $Log: LoneTuple.java,v $
 * Revision 1.4  2007/09/24 09:42:34  olga
 * AGG transformation engine tuning
 *
 * Revision 1.3  2007/09/10 13:05:19  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.2 2006/08/02 09:00:57 olga Preliminary
 * version 1.5.0 with - multiple node type inheritance, - new implemented
 * evolutionary graph layouter for graph transformation sequences
 * 
 * Revision 1.1 2005/08/25 11:56:57 enrico *** empty log message ***
 * 
 * Revision 1.2 2005/06/20 13:37:03 olga Up to now the version 1.2.8 will be
 * prepared.
 * 
 * Revision 1.1 2005/05/30 12:58:04 olga Version with Eclipse
 * 
 * Revision 1.2 2003/03/05 18:24:22 komm sorted/optimized import statements
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:02 olga Imported sources
 * 
 * Revision 1.5 2000/04/05 12:09:15 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 * Revision 1.4 1999/07/26 10:17:48 shultzke kopieren der in-out parameter aus
 * loneTuple konstruktor nach VarMember.copy() verschoben
 */
