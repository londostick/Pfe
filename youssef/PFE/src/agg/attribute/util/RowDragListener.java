/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.attribute.util;

/**
 * Listener for dragging of table rows.
 * 
 * @version $Id: RowDragListener.java,v 1.2 2007/09/10 13:05:53 olga Exp $
 * @author $Author: olga $
 */
public interface RowDragListener {

	public void draggingStarted(RowDragEvent ev);

	public void draggingStopped(RowDragEvent ev);

	public void draggingMoved(RowDragEvent ev);

}

/*
 * $Log: RowDragListener.java,v $
 * Revision 1.2  2007/09/10 13:05:53  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.1 2005/08/25 11:56:57 enrico ***
 * empty log message ***
 * 
 * Revision 1.1 2005/05/30 12:58:04 olga Version with Eclipse
 * 
 * Revision 1.2 2003/03/05 18:24:28 komm sorted/optimized import statements
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:06 olga Imported sources
 * 
 * Revision 1.3 2000/04/05 12:11:19 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 */
