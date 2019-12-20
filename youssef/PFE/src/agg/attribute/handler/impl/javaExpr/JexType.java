/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.attribute.handler.impl.javaExpr;

import java.io.IOException;

import agg.attribute.handler.HandlerType;

/**
 * @version $Id: JexType.java,v 1.5 2010/09/23 08:13:35 olga Exp $
 * @author $Author: olga $
 */
public class JexType implements HandlerType {
	static final long serialVersionUID = 8053541082320950101L;

	protected JexHandler handler = null;

	protected String text = null;

	protected Class<?> clazz = null;

	public JexType(JexHandler handler, String typeString, Class<?> clazz) {
		this.handler = handler;
		this.text = typeString.trim();
		this.clazz = clazz;
	}

	/**
	 * Getting the string representation of this type. Overrides the
	 * "toString()" method of the "Object" class.
	 */
	public String toString() {
		return this.text;
	}

	public boolean equals(Object obj) {
		if (obj instanceof JexType) {
			JexType t = (JexType) obj;
			return this.clazz == t.clazz && this.handler == t.handler
					&& this.text.equals(t.text);
		} 
		return false;
	}

	public Class<?> getClazz() {
		return this.clazz;
	}

	/**
	 * Overriding of the standard implementation is required, because
	 * representations for primitive types (Integer.TYPE, Character.TYPE etc.)
	 * are not serializable. They are not written on the stream.
	 * 
	 * @see #readObject
	 */
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(this.handler);
		out.writeObject(this.text);
		if (!this.clazz.isPrimitive()) {
			out.writeObject(this.clazz);
		}
	}

	/**
	 * Overriding of the standard implementation is required, because
	 * representations for primitive types (Integer.TYPE, Character.TYPE etc.)
	 * are not serializable. They are not read from the stream. Instead, they
	 * are restored according to their string representation.
	 * 
	 * @see #writeObject
	 */
	@SuppressWarnings("rawtypes")
	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		this.handler = (JexHandler) in.readObject();
		this.text = (String) in.readObject();
		this.clazz = this.handler.classResolver.forName(this.text);
		if (this.clazz == null || !this.clazz.isPrimitive()) {
			this.clazz = (Class) in.readObject();
		}
	}
}
/*
 * $Log: JexType.java,v $
 * Revision 1.5  2010/09/23 08:13:35  olga
 * tuning
 *
 * Revision 1.4  2010/03/08 15:37:01  olga
 * code optimizing
 *
 * Revision 1.3  2007/11/01 09:58:19  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.2  2007/09/10 13:05:46  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.1 2005/08/25 11:57:00 enrico *** empty log
 * message ***
 * 
 * Revision 1.1 2005/05/30 12:58:04 olga Version with Eclipse
 * 
 * Revision 1.2 2003/03/05 18:24:28 komm sorted/optimized import statements
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:00 olga Imported sources
 * 
 * Revision 1.5 2000/06/15 06:53:14 shultzke equals fuer JexType anstatt
 * Objectvergleich
 * 
 * Revision 1.4 2000/04/05 12:08:47 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 */
