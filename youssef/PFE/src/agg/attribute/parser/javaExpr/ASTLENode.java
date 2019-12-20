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
 * @version $Id: ASTLENode.java,v 1.5 2010/07/29 10:09:19 olga Exp $
 * @author $Author: olga $
 */
public class ASTLENode extends NUMxNUMtoBOOLnode {

	static final long serialVersionUID = 1L;

	ASTLENode(String id) {
		super(id);
	}

	public static Node jjtCreate(String id) {
		return new ASTLENode(id);
	}

	public void interpret() {
		Node child1 = jjtGetChild(0);
		Node child2 = jjtGetChild(1);

		child1.interpret();
		child2.interpret();

		Object op1Result = stack.get(top-1);
		Object op2Result = stack.get(top);
		Object result;
		Class<?> commonType = commonNumberType((SimpleNode)child1, (SimpleNode)child2);

		if (typeCode(commonType) <= typeCode(Integer.TYPE)) {
			result = new Boolean(
					((Number) op1Result).intValue() <= ((Number) op2Result)
							.intValue());
		} else {
			result = new Boolean(
					((Number) op1Result).floatValue() <= ((Number) op2Result)
							.floatValue());
		}
//		stack[--top] = result;
		stack.set(--top, result);
	}

	public String getString() {
		Node left = jjtGetChild(0);
		Node right = jjtGetChild(1);
		return left.getString() + "<=" + right.getString();
	}
}
/*
 * $Log: ASTLENode.java,v $
 * Revision 1.5  2010/07/29 10:09:19  olga
 * Array stack changed to Vector stack
 *
 * Revision 1.4  2010/03/31 21:10:49  olga
 * tuning
 *
 * Revision 1.3  2007/11/01 09:58:17  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.2  2007/09/10 13:05:48  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.1 2005/08/25 11:56:51 enrico *** empty
 * log message ***
 * 
 * Revision 1.1 2005/05/30 12:58:01 olga Version with Eclipse
 * 
 * Revision 1.2 2002/09/23 12:24:02 komm added type graph in xt_basis, editor
 * and GUI
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:03 olga Imported sources
 * 
 * Revision 1.6 2000/04/05 12:10:16 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 * Revision 1.5 2000/03/14 10:59:11 shultzke Transformieren von Variablen auf
 * Variablen sollte jetzt funktionieren Ueber das Design der Copy-Methode des
 * abstrakten Syntaxbaumes sollte unbedingt diskutiert werden.
 * 
 */
