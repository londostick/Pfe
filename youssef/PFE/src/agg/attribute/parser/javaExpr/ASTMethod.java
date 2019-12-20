/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.attribute.parser.javaExpr;

/* JJT: 0.2.2 */

/**
 * @version $Id: ASTMethod.java,v 1.4 2010/11/28 22:12:22 olga Exp $
 * @author $Author: olga $
 */
public class ASTMethod extends OpMemberNode {

	/*
	 * Inherits: protected Method method = null; protected Object receivingObj,
	 * returnObj;
	 */

	static final long serialVersionUID = 1L;

	ASTMethod(String id) {
		super(id);
	}

	public static Node jjtCreate(String id) {
		return new ASTMethod(id);
	}

	public String getString() {
		String argList = "";
		int nChildren = jjtGetNumChildren();
		Node meth = jjtGetChild(0);
		for (int i = 1; i < nChildren; i++) {
			if (i > 1)
				argList += ",";
			argList += jjtGetChild(i).getString();
		}
		String methStr = "." + meth.getString() + "(" + argList + ")";
		return methStr;
	}
}
/*
 * $Log: ASTMethod.java,v $
 * Revision 1.4  2010/11/28 22:12:22  olga
 * tuning
 *
 * Revision 1.3  2007/09/24 09:42:38  olga
 * AGG transformation engine tuning
 *
 * Revision 1.2  2007/09/10 13:05:47  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.1 2005/08/25 11:56:52 enrico *** empty
 * log message ***
 * 
 * Revision 1.1 2005/05/30 12:58:01 olga Version with Eclipse
 * 
 * Revision 1.3 2003/03/05 18:24:16 komm sorted/optimized import statements
 * 
 * Revision 1.2 2002/09/23 12:24:02 komm added type graph in xt_basis, editor
 * and GUI
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:03 olga Imported sources
 * 
 * Revision 1.6 2000/04/05 12:10:22 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 * Revision 1.5 2000/03/14 10:59:16 shultzke Transformieren von Variablen auf
 * Variablen sollte jetzt funktionieren Ueber das Design der Copy-Methode des
 * abstrakten Syntaxbaumes sollte unbedingt diskutiert werden.
 * 
 */
