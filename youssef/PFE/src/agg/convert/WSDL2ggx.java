/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.convert;

import agg.attribute.handler.AttrHandler;
import agg.attribute.handler.impl.javaExpr.JexHandler;
import agg.attribute.impl.AttrTupleManager;
import agg.attribute.impl.VarMember;
import agg.attribute.impl.VarTuple;
import agg.util.XMLHelper;
import agg.util.XMLObject;
import agg.xt_basis.GraGra;
import agg.xt_basis.Node;
import agg.xt_basis.NodeTypeImpl;
import agg.xt_basis.Rule;
import agg.xt_basis.Type;
import agg.xt_basis.TypeException;


public class WSDL2ggx implements XMLObject {
	
	private GraGra gragra;
	private boolean done;
	
	
	public WSDL2ggx(GraGra gra) {
		this.gragra = gra;
	}

	public boolean isSuccessful() {
		return done;
	}
	
	private Node createNodeType(String name) {
		NodeTypeImpl nt = (NodeTypeImpl) gragra.getTypeSet().createNodeType(true);
		nt.setStringRepr(name);
		try {
			return gragra.getTypeSet().getTypeGraph().createNode(nt);
		} catch (TypeException ex) {}
		return null;
	}
	
	private void addAttr(AttrHandler attrHandler, Type nodeType, String aType, String aName) {
		nodeType.getAttrType().addMember(attrHandler, aType, aName);		
	}
	
	private void addVar(AttrHandler attrHandler, VarTuple varTuple, String aType, String aName, boolean inputPar) {
		varTuple.getType().addMember(attrHandler, aType, aName);		
		VarMember var = (VarMember) varTuple.getMemberAt(aName);
		var.setInputParameter(inputPar);
	}
	
	private String getJavaType(String sType) {
		if (sType.startsWith("s:")) {
			String str = sType.substring(2);
			if (str.equals("string"))
				str = "String";
			return str;
		}
		return "";
	}
	
	
	@Override
	public void XreadObject(XMLHelper h) {
		h.push(h.top());
		String tag = h.readSubTag();
		System.out.println(tag);
		if (tag.equals("wsdl:types")) {	
			this.done = true;
			AttrHandler attrHandler = AttrTupleManager.getDefaultManager().getHandler(JexHandler.getLabelName());
//			Node ws = 
			createNodeType("WebService");
			if (h.readSubTag("s:schema")) {
				System.out.println("s:schema");
				tag = h.readSubTag();
				while (tag != null && !tag.isEmpty()) {
					if (tag.equals("s:element")) {
						String eName = h.readAttr("name");	
						System.out.println(tag+"     "+eName);
						if (!eName.endsWith("Response")) {
							Rule r = gragra.createRule();
							r.setName(eName);
							VarTuple vars = (VarTuple)r.getAttrContext().getVariables();
							tag = h.readSubTag();
							if (tag != null && tag.equals("s:complexType")) {
								tag = h.readSubTag();
								if (tag != null && tag.equals("s:sequence")) {
									tag = h.readSubTag();
									while (tag != null && tag.equals("s:element")) {
										String aName = h.readAttr("name");	
										String sType = h.readAttr("type");
										String aType = getJavaType(sType);
										if (!aName.equals("") && !aType.equals("")) {
											addVar(attrHandler, vars, getJavaType(sType), aName, true);
										}
										h.close();
										tag = h.readSubTag();
									}
									h.close();
								}
								h.close();
							}
							r.initSignatur();
						}
						h.close();
					}
					else if (tag.equals("s:complexType")) {
						String eName = h.readAttr("name");	
						System.out.println(tag+"     "+eName);
						if (!eName.startsWith("Array")) {
							Node n = createNodeType(eName);
							tag = h.readSubTag();
							if (tag != null && tag.equals("s:sequence")) {
								tag = h.readSubTag();
								while (tag != null && tag.equals("s:element")) {
									String aName = h.readAttr("name");	
									String sType = h.readAttr("type");
									String aType = getJavaType(sType);
									if (!aName.equals("") && !aType.equals("")) {
										addAttr(attrHandler, n.getType(), getJavaType(sType), aName);
									}
									h.close();
									tag = h.readSubTag();
								}						
								h.close();
							}
						}
						h.close();
					}
					tag = h.readSubTag();
				}
				h.close();
			}
			h.close();		
		}
		h.close();
	}
	
	@Override
	public void XwriteObject(XMLHelper h) {
		// TODO Auto-generated method stub		
	}
}
