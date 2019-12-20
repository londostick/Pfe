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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import agg.attribute.AttrContext;
import agg.attribute.AttrEvent;
import agg.attribute.AttrInstanceMember;
import agg.attribute.AttrMapping;
import agg.attribute.AttrTuple;
import agg.attribute.AttrTypeMember;
import agg.attribute.handler.AttrHandler;
import agg.attribute.handler.AttrHandlerException;
import agg.attribute.handler.HandlerExpr;
import agg.attribute.handler.HandlerType;
import agg.attribute.parser.javaExpr.Node;
import agg.attribute.parser.javaExpr.SimpleNode;
import agg.util.XMLHelper;

/**
 * Holds an attribute handler expression, its type and the functionality for
 * matching and transforming thereof.
 * 
 * @author $Author: olga $
 * @version $Id: ValueMember.java,v 1.45 2010/11/29 08:42:25 olga Exp $
 */
@SuppressWarnings("serial")
public class ValueMember extends Member implements AttrInstanceMember,
		AttrMsgCode {

	/** This string is shown for an empty value. */
	static public final String EMPTY_VALUE_SYMBOL = "";

	// Protected instance variables.

	/** Declaration */
	protected DeclMember decl;

	/** Instance tuple that this value is a member of. */
	protected ValueTuple tuple;

	/** Attribute handler expression. */
	protected HandlerExpr expression;

	protected String expressionText;

	protected Object expressionObject;

	transient protected Exception currentException;

	transient protected String errorMsg;

	protected boolean isTransient;

	// Public constructors.

	/**
	 * Creating a new instance with the specified type.
	 * 
	 * @param tuple
	 *            Instance tuple that this value is a member of.
	 * @param decl
	 *            Declaration for this member.
	 */
	public ValueMember(ValueTuple tuple, DeclMember decl) {
		this.tuple = tuple;
		this.decl = decl;
		rawSetExpr(null);
		checkValidity();
		this.errorMsg = "";
		this.isTransient = false;
		
//		logPrintln(VerboseControl.logContextOfInstances,
//				"Member created for Tuple:" + tuple);
	}

	public String getErrorMsg() {
		return this.errorMsg;
	}

	public boolean isValid() {
		return this.currentException == null;
	}

	public String getValidityReport() {
		if (isValid() && getDeclaration().isValid())
			return null;
		String report = getDeclaration().getValidityReport();
		if (isValid())
			return report;
		if (report == null)
			report = "";
		// report += "-------- VALUE : --------\n";
		if (this.currentException != null)
			report += this.currentException.getMessage();
		return report;
	}

	public AttrTypeMember getDeclaration() {
		return getDecl();
	}

	public Object getExprAsObject() {
		if (getExpr() != null)
			return getExpr().getValue();
	
		return null;
	}

	public HandlerExpr getExpr() {
		return rawGetExpr();
	}

	public String getExprAsText() {
		if (getExpr() == null)
			return this.expressionText;
		
		return getExpr().toString();
	}

	/** Setting the value and fire event <code>AttrEvent.MEMBER_VALUE_MODIFIED</code>. */
	public void setExpr(HandlerExpr expr) {
		if (expr == null) {
			this.expressionText = null;
			this.expressionObject = null;
			this.expression = null;
			this.errorMsg = "";
			this.currentException = null;
		}
		else
			setCheckedExpr(expr);
		fireChanged(AttrEvent.MEMBER_VALUE_MODIFIED);
	}

	/** Setting the value and fire event <code>AttrEvent.MEMBER_VALUE_MODIFIED</code>. */
	public void setExprAsObject(Object obj) {
		rawSetExprAsObject(obj);
		fireChanged(AttrEvent.MEMBER_VALUE_MODIFIED);
	}

	protected void rawSetExprAsObject(Object obj) {
		// System.out.println("ValueMember.rawSetExprAsObject:: "+obj );
		try {
			this.expressionText = null;
			this.expressionObject = obj;
			if ((getType() != null) && (obj != null))
				this.expression = getHandler().newHandlerValue(getType(), obj);
			checkValidity();
		} catch (AttrHandlerException ex) {
			this.currentException = ex;
		}
	}

	/**
	 * Set expression and try to initialize it if it should be a new instance of a Class.
	 * Exmpl: "new Integer(x)"
	 */
	public void setExprAsText(String exprText, boolean initialize) throws AttrImplException {
		setExprAsText(exprText);
		// try initialize variable of attr. context
		if (initialize
				&& getExpr() != null 
				&& getExpr().isComplex()
				&& (exprText.indexOf("new ") == 0)) {
			apply(getExpr());
		}
	}
	
	/** Setting the value and fire event <code>AttrEvent.MEMBER_VALUE_MODIFIED</code>. */
	public void setExprAsText(String exprText) {
		String s = exprText;
		if (exprText == null)
			s = new String();

		if (s.length() == 0) {
			ContextView context = getContext();
			if (context == null || context.doesAllowEmptyValues()) {
				setExpr(null);
				return;
			}
		}
		
		if ((s.length() != 0) && (s.charAt(0) == '(')) {
//			char ch = 
			s.charAt(0);
			for (int i = 1; i < s.length(); i++) {
				if (s.charAt(i) == ')') {
					if (i == (s.length() - 1)) {
						String ss = s.substring(1, s.length() - 1);
						s = ss;
						i = s.length() + 1;
					} else
						i = s.length() + 1;
				}
			}
		}
		if (!s.equals(exprText))
			rawSetExprAsText(s);
		else
			rawSetExprAsText(exprText);

		if (this.currentException == null)
			fireChanged(AttrEvent.MEMBER_VALUE_MODIFIED);
	}

	/** Setting the value. */
	protected void rawSetExprAsText(String exprText) {
		String lastExpressionText = this.expressionText;
		HandlerExpr lastExpression = this.expression;
		Object lastExpressionObject = this.expressionObject;
		this.currentException = null;
		try {
			this.expressionText = exprText;
			this.expressionObject = null;
			HandlerExpr test = null;
			
			if (exprText != null) {
				if (exprText.length() != 0) {
					test = getHandler().newHandlerExpr(getType(), exprText);
					checkValidity(test);

					if (this.currentException == null) {
						this.expression = test;
						this.expressionText = exprText;
					} else {
						fireChanged(AttrEvent.MEMBER_VALUE_CORRECTNESS);
						this.expression = test;
						return;
					}
				}
			}
			
			checkValidity();
			// try to initialize attr. member in a graph (host graph)
			if (this.currentException == null
					&& getContext().getAllowedMapping() == AttrMapping.GRAPH_MAP) {
				if (test != null) {
					apply(test);															
					if (this.currentException != null) {
						this.expression = null;
						this.expressionText = "";
						this.expressionObject = null;
						fireChanged(AttrEvent.MEMBER_VALUE_CORRECTNESS);
					}
				}
			}
			
		} catch (AttrImplException aiex) {
			this.currentException = aiex;
			this.errorMsg = aiex.getMessage();
			this.expressionText = lastExpressionText;
			this.expressionObject = lastExpressionObject;
			this.expression = lastExpression;
		} catch (AttrHandlerException ex) {
			this.currentException = ex;
			this.errorMsg = ex.getMessage();
			this.expressionText = lastExpressionText;
			this.expressionObject = lastExpressionObject;
			this.expression = lastExpression;
		}
	}

	/** Setting the value and fire event <code>AttrEvent.MEMBER_VALUE_MODIFIED</code>. */
	public void setExprAsEvaluatedText(String exprText) {
		try {
			this.expressionText = exprText;
			this.expressionObject = null;
			this.expression = getHandler().newHandlerExpr(getType(), exprText);
			this.expression.evaluate(getContext());
			// After evaluation the string representation is not reliable
			// anymore. Switching to object storing.
			this.expressionText = null;
			this.expressionObject = this.expression.getValue();
			checkValidity();
			fireChanged(AttrEvent.MEMBER_VALUE_MODIFIED);
		} catch (AttrHandlerException ex) {
			this.currentException = ex;
			this.expression = null;
		}
	}

	/** 
	 * Setting the value and fire event <code>AttrEvent.MEMBER_VALUE_MODIFIED</code>. 
	 * This method is recommended when using AGG engine  
	 * and the attribute value of an object of RHS of a rule is a complex expression
	 * containing calls of class methods. 
	 */
	public void trySetExprAsText(String exprText) throws AttrHandlerException {
		try {
			String newText = AttrTupleManager.getDefaultManager()
										.getStaticMethodCall(exprText);	
			HandlerExpr exprssn = this.getHandler().newHandlerExpr(
								this.getDeclaration().getType(), newText);
			this.checkValidity(exprssn);			
			if (this.isValid()) {
				this.setExprAsText(newText);
			} else if (this.currentException != null) {
				throw new AttrHandlerException(this.currentException.getLocalizedMessage());
			}
			else {
				throw new AttrHandlerException("Not valid expression: "+exprText);
			}
		} catch (AttrHandlerException ex) {
			throw ex;
		}
	}
	
	public void typeChanged() {
		boolean prevValidity = isValid();

		this.expression = null;
		try {
			if (this.expressionText != null) {
				this.expression = getHandler().newHandlerExpr(getType(),
						this.expressionText);
			} else if (this.expressionObject != null) {
				this.expression = getHandler().newHandlerValue(getType(),
						this.expressionObject);
			}
		} catch (Exception ex) {
			this.currentException = ex;
			this.expressionText = null;
			this.expressionObject = null;
		}

		checkValidity();
		if (prevValidity != isValid())
			fireChanged(AttrEvent.MEMBER_VALUE_CORRECTNESS);
	}

	/** Check if no expression yet. */
	public boolean isEmpty() {
		return this.getExpr() == null;
	}

	/** Check if set. */
	public boolean isSet() {
		return !(this.getExpr() == null);
	}

	/**
	 * This method is used inside of the method rawSetExprAsText(String text) to
	 * initialize the attribute members with type of Java class. For example,
	 * an expression like  "new Integer(7)"  will be  initialized to 7.
	 * 
	 * @param expr
	 */
	protected void apply(HandlerExpr expr) {
		if (expr != null && !expr.toString().equals("null")) {
			HandlerExpr oldExpr = getExpr();
			this.expression = expr.getCopy();
			HandlerExpr exp = null;
			try {
				exp = this.getExpr();
				if (exp != null) {	
					exp.evaluate(getContext());
					if (exp.getValue() == null) {
						this.expressionText = "";
						this.expression = null;
						this.expressionObject = null;
					} else {
						fireChanged(AttrEvent.MEMBER_VALUE_MODIFIED);
					}
				}
			} catch (AttrHandlerException ex1) {
				this.currentException = ex1;
				this.expression = oldExpr;
				
				throw new AttrImplException(EXPR_EVAL_ERR, ex1.getMessage()
						+ "\n" + expr + "  don't match to  " + this.expression);
			} catch (Exception ex) {
				this.currentException = ex;
				this.expression = oldExpr;
				throw new AttrImplException(EXPR_EVAL_ERR, ex.getMessage()
						+ "\n" + expr + "  don't match to  " + this.expression);
			}
		}
	}

	/**
	 * Transformation application
	 * 
	 * @param rightSide
	 *            The expression from the right rule side
	 * @param context
	 *            The match context.
	 */
	public void apply(ValueMember rightSide, AttrContext context) {
//		if (this.getName().equals("HASHCODE")) return;

		if ((rightSide != null) && (rightSide.getExpr() != null)) {
			HandlerExpr oldExpr = getExpr();
			this.expression = rightSide.getExpr().getCopy();
			HandlerExpr exp = null;
			try {				
				exp = this.getExpr();
				if (exp != null) {		
					exp.evaluate(context);	
					if (exp.getValue() == null) {
						setExprAsText(exp.getString());
					} else {
						fireChanged(AttrEvent.MEMBER_VALUE_MODIFIED);
					}
				}
			} catch (AttrHandlerException ex1) {
				this.expression = oldExpr;
				throw new AttrImplException(EXPR_EVAL_ERR, ex1.getMessage()
						+ "\n" + rightSide.getExpr() + "  don't match to  "
						+ this.expression);
			} catch (Exception ex) {
				this.expression = oldExpr;				
				throw new AttrImplException(EXPR_EVAL_ERR, ex.getMessage()
						+ "\n" + rightSide.getExpr() + "  don't match to  "
						+ this.expression);
			}
		}
	}

	/**
	 * Transform application like apply( ValueMember, AttrContext), 
	 * additionally do allow using variables without value as
	 * value of attribute member.
	 */
	public void apply(final ValueMember rightSide, 
			final AttrContext context,
			boolean allowVariableWithoutValue) {
		apply(rightSide, context, allowVariableWithoutValue , false);
	}
	
	/**
	 * Transform application like apply( ValueMember, AttrContext), 
	 * additionally do allow using variables without value as
	 * value of attribute member. 
	 * If equalVariableName is TRUE, then the name of the variable from rightSide
	 * must be equal to the name of the current variable.
	 * The equalVariableName option is only used when allowVariableWithoutValue is TRUE.
	 */
	public void apply(final ValueMember rightSide, 
			final AttrContext context,
			boolean allowVarWithoutValue, 
			boolean equalVarName) {
//		if (this.getName().equals("HASHCODE"))	return;

		if (!allowVarWithoutValue) {
			apply(rightSide, context);
			return;
		} 
		
		if ((rightSide != null) && (rightSide.getExpr() != null)) {
		
			if (equalVarName
					&& this.getExpr() != null
					&& this.getExpr().isVariable() 
					&& rightSide.getExpr().isVariable()) {
				
				if (this.getExprAsText().equals(rightSide.getExprAsText())) {
					return;
				}
				
				if (!this.isTransient && !rightSide.isTransient()) {										
					throw new AttrImplException(EXPR_EVAL_ERR, 
							rightSide.getExpr() + "  don't match to  "
							+ this.expression);
				}			
			}
			
//			System.out.println(this.isTransient+"   "+this.getExpr()+"    "+rightSide.getExprAsText()
//					+"   "+rightSide.getExpr().isVariable()+"   "+rightSide.isTransient()
//					+"   "+context.getExpr(rightSide.getExprAsText()));
			
			if (rightSide.getExpr().isVariable() 
					&& (rightSide.isTransient() 
							|| context.getExpr(rightSide.getExprAsText()) != null)) {
				if (getExpr() == null) {
					this.expression = rightSide.getExpr().getCopy();
					this.isTransient = rightSide.isTransient();  //TODO  test!!!
				}
				return;
			}
			
			if (getExpr() == null 
					|| (this.getExpr().isConstant() && rightSide.getExpr().isConstant())
					|| this.isTransient ) {
								
				HandlerExpr oldExpr = getExpr();
				HandlerExpr exp = null;
				this.expression = rightSide.getExpr().getCopy();				
				try {				
					exp = this.getExpr();
					if (exp != null) {
						exp.evaluate(context);												
						if (exp.getValue() == null) {							
							setExprAsText(exp.getString());
							if (!this.isTransient) {
								setTransient(rightSide.isTransient());
							}
						} else if (exp.getValue() != null) {
							fireChanged(AttrEvent.MEMBER_VALUE_MODIFIED);
						}
					}
				} catch (AttrHandlerException ex1) {
					if (exp != null && exp.getValue() == null) {
						setExprAsText(exp.getString());
						if (!this.isTransient)
							setTransient(rightSide.isTransient());
					}
				} catch (Exception ex) {
					if (exp != null && exp.getValue() == null) {
						setExprAsText(exp.getString());
						if (!this.isTransient)
							setTransient(rightSide.isTransient());
					} else {
						this.expression = oldExpr;
					}
				}
			}
		}
	}

	/**
	 * Check if matching is possible into 'target' within the match context
	 * 'context'.
	 * 
	 * @return 'true' if possible, 'false' otherwise.
	 */
	public boolean canMatchTo(ValueMember target, ContextView context) {
		this.errorMsg = "";
		HandlerExpr tar = null;
		if (target != null) 
			tar = target.getExpr();
		if (tar != null) {
			HandlerExpr src = this.getExpr();
			if (src == null)
				return true;
				
			boolean result = false;
			result = result || tar.isVariable(); // auf Variable matchen
			result = result || tar.isComplex();
			if (!result) {								
				boolean r2, r3, r4=true;
				r3 = !src.isVariable();
				r3 = r3 || context.canSetValue(src.toString(), target);				
				if (r3) {
					r4 = src.isUnifiableWith(tar, context);						
					if (!r4) {
						if (src.getString().equals(tar.getString())
								&& context.isVariableContext()) {
							r4 = true;
						}
						else if (src.isConstant() && tar.isConstant()
								&& context.isIgnoreConstContext()) {
							r4 = true;
						}
//						else if (((src.isVariable() && tar.isConstant())
//								|| (tar.isVariable() && src.isConstant()))
//								&& context.isVariableContext()) {
//							r4 = true;
//						}
					}
				}				
				r2 = r3 && r4;	
				result = result || r2;				
			}			
			if (result)
				return true;
			
			this.errorMsg = this.errorMsg.concat(context.getErrorMsg());
			/*
				if (!src.isUnifiableWith(tar, context)) {
					if (!errorMsg.equals(""))
						errorMsg = errorMsg + "\nSource attribute "
									+ src.toString()
									+ " does not unify with target attribute "
									+ tar.toString() + ".";
					else
						errorMsg = "Source attribute does not unify target attribute.";
				} else if (target != null) {
					errorMsg = this.getName() + " cannot match "
									+ target.getName();
				} else 
					errorMsg = this.getName() + " cannot match ";
					
				if (src.isVariable()
						&& !context.canSetValue(src.toString(), target)) {
					if (!errorMsg.equals(""))
						errorMsg = errorMsg
									+ "\nCannot set value for variable "
									+ src.toString() + " in context.";
					else
						errorMsg = "Cannot set value in context.";
				} else if (target != null) {
					errorMsg = this.getName() + " cannot match "
								+ target.getName();
				} else 
					errorMsg = this.getName() + " cannot match ";
		 */
			return false;				
		} 
		else if (context.getAllowedMapping() == AttrMapping.MATCH_MAP) {
			this.errorMsg = "Source attribute: <" + this.getName()
					+ "> cannot match to its target attribute because it is not set!";
			return false;
		} else {
//			if (context.getAllowedMapping() == AttrMapping.PLAIN_MAP
//				|| context.getAllowedMapping() == AttrMapping.OBJECT_FLOW_MAP) {
			return true;
		} 
	}

	/**
	 * Performs matching with 'target' in the match context 'context'.
	 * 
	 * @return The name of the variable that this match has affected, i.e.
	 *         assigned value to. If no variable is concerned, returns null.
	 */
	public String matchTo(ValueMember target, ContextView context) {
//		if (this.getName().equals("HASHCODE")) return null;
		
		HandlerExpr srcExpr = this.getExpr();
		String varName = null;

		if (srcExpr != null && srcExpr.isVariable()) {
			varName = srcExpr.toString();			
			try {
				context.setValue(varName, target);
			} catch (NoSuchVariableException e) {
			}
		}
		return varName;
	}

	/** Copies the contents of the specified entry instance into this instance. */
	public void copy(ValueMember fromInstance) {
		HandlerExpr srcExpr = fromInstance.getExpr();
		HandlerExpr tarExpr = null;
		if (srcExpr != null) {
			tarExpr = srcExpr.getCopy();
			if (tarExpr != null) {
				this.expression = tarExpr;
				if (fromInstance.expressionText != null)
					this.expressionText = fromInstance.expressionText.toString();
				else
					this.expressionText = null;
				this.expressionObject = this.expression.getValue();
				checkValidity();
				setTransient(fromInstance.isTransient());
				fireChanged(AttrEvent.MEMBER_VALUE_MODIFIED);
			}
		} else {
			this.setExpr(null);
		}
	}

	/** Tests if the handler expressions are equal. */
	public boolean equals(ValueMember testObject) {

		if (this.getName().equals("HASHCODE"))
			return true;

		if (testObject == null)
			return false;

		if (getExpr() != null)
			return getExpr().equals(testObject.getExpr());
		if (this.expressionText != null)
			return this.expressionText.equals(testObject.expressionText);
		if (this.expressionObject != null)
			return this.expressionObject.equals(testObject.expressionObject);
		return (testObject.expressionText == null
				&& testObject.expressionObject == null && testObject.getExpr() == null);
	}

	public boolean compareTo(ValueMember member) {
		if (this.getName().equals("HASHCODE"))
			return true;
		
		if (member == null)
			return false;

		if (getExpr() != null) {
			if (member.getExpr() != null)
				return getExpr().toString().equals(member.getExpr().toString());
			
			return false;
		}
		
		if (this.expressionText != null)
			return this.expressionText.equals(member.expressionText);
		
		return false;
	}

	/**
	 * @return The textual representation of the expression or
	 *         'EMPTY_VALUE_SYMBOL' if that is null.
	 */
	public String toString() {
		if (this.getExpr() != null)
			return this.getExpr().toString();
		if (this.expressionText != null)
			return this.expressionText;
		if (this.expressionObject != null)
			return this.expressionObject.toString();
		return EMPTY_VALUE_SYMBOL;
	}

	/**
	 * @return The instance tuple that contains this value as a member.
	 */
	public AttrTuple getHoldingTuple() {
		return this.tuple;
	}

	// Internal access.
	//

	/**
	 * Getting the instance tuple that contains this value as a member.
	 */
	protected ValueTuple getTuple() {
		return this.tuple;
	}

	/** Getting the context of this value. */
	protected ContextView getContext() {
		return (ContextView) getTuple().getContext();
	}

	/** Getting the declaration for this value */
	protected DeclMember getDecl() {
		return this.decl;
	}

	/** Getting the handler of this value. */
	public AttrHandler getHandler() {
		return getDecl().getHandler();
	}

	/** Getting the type of this value. */
	protected HandlerType getType() {
		return getDecl().getType();
	}

	/** Getting the name of this value member. */
	public String getName() {
		return getDecl().getName();
	}

	public Vector<String> getAllVariableNamesOfExpression() {
		Vector<String> resultVector = new Vector<String>();
		if (getExpr() != null)
			getExpr().getAllVariables(resultVector);
		return resultVector;
	}
	
	public Vector<Node> getChildrenOfExpression() {
		Vector<Node> resultVector = new Vector<Node>();
		if (getExpr() != null) {
			if (getExpr().getAST() != null) {
				SimpleNode node = (SimpleNode) getExpr().getAST();
				for (int i = 0; i < node.jjtGetNumChildren(); i++) {
					// System.out.println("ValueMember.getChildrenOfExpression()
					// :: "+node.jjtGetChild(i));
					resultVector.addElement(node.jjtGetChild(i));
				}
			}
		}
		return resultVector;
	}

	protected HandlerExpr rawGetExpr() {
		return this.expression;
	}

	protected void rawSetExpr(HandlerExpr expr) {
		this.expression = expr;
	}

	/** Setting the value and checking validity. */
	protected void setCheckedExpr(HandlerExpr expr) {
		rawSetExpr(expr);
		checkValidity();
	}

	/** Checking its own expression validity with respect to the context. */
	// protected void checkValidity(){
	public void checkValidity() {
		try {
			checkInContext(getExpr(), getContext());
			this.currentException = null;
		} catch (Exception ex) {
			this.currentException = ex;
			this.errorMsg = ex.getMessage();
		}
	}

	/** Checking the specified expression validity with respect to the context. */
	public void checkValidity(HandlerExpr hExpr) {
		try {
			checkInContext(hExpr, getContext());
			this.errorMsg = "";
			this.currentException = null;
		} catch (Exception ex) {
			this.currentException = ex;
			this.errorMsg = ex.getMessage();
		}
	}

	/**
	 * Checking the validity of the expression 'hExpr' relative to the context
	 * 'ctx'.
	 */
	protected void checkInContext(HandlerExpr hExpr, AttrContext ctx) {
		ContextView context = (ContextView) ctx;
		if (hExpr == null) {
			if (context == null || !context.doesAllowEmptyValues()) {
				throw new AttrImplException(EXPR_REQUIRED);
			} 
			return;	
		}
		
		String exprText = hExpr.toString();
		AttrHandler handler = getHandler();
		HandlerType type = getType();
		if (context == null) {
			if (!hExpr.isConstant()) {
//				throw new AttrImplException(EXPR_MUST_BE_CONST);
				this.errorMsg = "Expression must be a constant: ".concat(hExpr.toString());
				this.currentException = new AttrImplException(EXPR_MUST_BE_CONST, this.errorMsg);
				throw (AttrImplException)this.currentException;
			}
		} else if (!context.doesAllowComplexExpressions()) {
			if (hExpr.isComplex()) {
//				throw new AttrImplException(EXPR_MUST_BE_CONST_OR_VAR);
				this.errorMsg = "Complex expression is not allowed: ".concat(hExpr.toString());
				this.currentException = new AttrImplException(EXPR_MUST_BE_CONST_OR_VAR, this.errorMsg);
				throw (AttrImplException)this.currentException;
			}
		} else if (context.getAllowedMapping() == AttrMapping.GRAPH_MAP) {
			
		}
		
		if (hExpr.isConstant()) {
			try {
				hExpr.checkConstant(context);
			} catch (AttrHandlerException ex) {
				this.errorMsg = ex.getMessage();
				this.currentException = ex;
				throw new AttrImplException(EXPR_PARSE_ERR, this.errorMsg);
			}
		}
		else if (hExpr.isVariable()) {
			if(context != null) {
				if (context.isDeclared(exprText)) {
					try {
						hExpr.check(context);
					} catch (AttrHandlerException ex) {
						this.errorMsg = ex.getMessage();
						this.currentException = ex;
						throw new AttrImplException(EXPR_PARSE_ERR, ex.getMessage());
					}
				} else { // New variable, not yet declared
					if (context.doesAllowNewVariables()) {
						context.addDecl(handler, type.toString(), exprText);
					} else { // New variable declarations not allowed in this context
						this.errorMsg = "undeclared variable found: ".concat(hExpr.toString());
						this.currentException = new AttrImplException(VAR_NOT_DECLARED, this.errorMsg);
						throw (AttrImplException) this.currentException;
					}
				}
			}
		} else if (hExpr.isComplex()) {
			// new: to test!
			if (context.isVariableContext()
					&& this.expressionObject == null) {
				return;
			}
				
			try {
				hExpr.check(context);
			} catch (AttrHandlerException ex) {
				this.errorMsg = ex.getMessage();
				this.currentException = ex;
				throw new AttrImplException(EXPR_PARSE_ERR, this.errorMsg);
			}
		}
	}

	public String getAttrHandlerExceptionMsg() {
		if (this.currentException != null) {
			return  this.currentException.getMessage();
		} 
		return "";
	}

	public void setTransient(boolean trans) {
		this.isTransient = trans;
	}

	public boolean isTransient() {
		return this.isTransient;
	}

	public void XwriteObject(XMLHelper h) {
		if ((this.decl == null) || (this.decl.getType() == null))
			return;

		h.openSubTag("Attribute");
		h.addObject("type", this.decl, false);
		HandlerExpr ex = getExpr();
		if (h.getDocumentVersion().equals("1.0")) {
			if (ex.isConstant()) {
				Object v = ex.getValue();
				h.openSubTag("Value");
				if (this.decl.getType().toString().equals("String")) {
					// handle site effect of CPA
					while (v instanceof HandlerExpr) {
						v = ((HandlerExpr)v).getValue();
					}
					if (v instanceof String)
						h.addAttrValue("string", v); 
				} else
					h.addAttrValue(this.decl.getType().toString(), v);
				h.close();
				h.addAttr("constant", "true");
			} else if (ex.isVariable()) {
				Object v = ex.getString();
				h.openSubTag("Value");
				h.addAttrValue("string", v);
				h.close();
				h.addAttr("variable", "true");
			} else { // expression
				Object v = ex.getString();
				h.openSubTag("Value");
				h.addAttrValue("string", v); //h.addAttrValue("String", v);
				h.close();
			}
		} else {
			if (ex.isConstant()) {
				Object v = ex.getValue();
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				try {
					ObjectOutputStream oos = new ObjectOutputStream(os);
					oos.writeObject(v);
					h.addAttr("value", h.escapeString(os.toString()));
				} catch (IOException e) {
					h.addAttr("value", h.escapeString(v.toString()));
				}
				h.addAttr("constant", "true");
			} else {
				h.addAttr("value", ex.getString());
			}
		}
		h.close();
	}

	public void XreadObject(XMLHelper h) {
		/*
		 * when reading we are already filled out from ValueTuple, and as we
		 * ourself are no entity (with an ID), we don't even call isTag(), so
		 * this is empty.
		 */
		if ((this.decl == null) || (this.decl.getType() == null)) {
			System.out
					.println("ValueMember.XreadObject: WARNING!\n\t Attribute  \""
							+ this.decl.getName()
							+ "\" :  type declaration of attribute value is null.");
			return;
		}

//		String readError = null;
		if (h.getDocumentVersion().equals("1.0")) {
			if (h.readAttr("constant") != "") {
				if (h.readSubTag("Value")) {
					Object obj = null;
					if (this.decl.getType().toString().equals("String")) {
						String javaTag = h.readSubTag();
						if (javaTag.equals("java")) {							
							obj = h.getAttrValue("String");
							if (obj == null)
								obj = h.getAttrValue("string");
						}
						if (javaTag.equals("string") || javaTag.equals("String")) {								
							obj = h.getElementData(h.top());
						}
						h.close();

						if (obj == null)
							obj = new String();
						else if (obj instanceof String) {
							// loesche '\n' und mehrere Leerzeichen 
							String objStr = formString((String) obj);
							obj = objStr;
						}
					} else {
						obj = h.getAttrValue(this.decl.getType().toString());
					}
					if (obj == null) {
						h.close();
						return;
					}

					if (obj instanceof String) {
						// test if value is null
						if (((String) obj).equals("null")) {
							setExprAsText((String) obj);
							// readError = getValidityReport();
							h.close();
							return;
						}
					}

					String valueAsString = "";
					if (this.decl.getType().toString().equals("String")) {
						setExprAsObject(obj);
					} else if (this.decl.getType().toString().equals("int")) {
						valueAsString = ((Integer) obj).toString();
						setExprAsEvaluatedText(valueAsString);
					} else if (this.decl.getType().toString().equals("boolean")) {
						valueAsString = ((Boolean) obj).toString();
						setExprAsEvaluatedText(valueAsString);
					} else if (this.decl.getType().toString().equals("float")) {
						valueAsString = ((Float) obj).toString();
						setExprAsEvaluatedText(valueAsString);
					} else if (this.decl.getType().toString().equals("double")) {
						valueAsString = ((Double) obj).toString();
						setExprAsEvaluatedText(valueAsString);
					} else if (this.decl.getType().toString().equals("long")) {
						valueAsString = ((Long) obj).toString();
						setExprAsEvaluatedText(valueAsString);
					} else if (this.decl.getType().toString().equals("short")) {
						valueAsString = ((Short) obj).toString();
						setExprAsEvaluatedText(valueAsString);
					} else if (this.decl.getType().toString().equals("byte")) {
						valueAsString = ((Byte) obj).toString();
						setExprAsEvaluatedText(valueAsString);
					} else if (this.decl.getType().toString().equals("char")) {
						valueAsString = ((Character) obj).toString();
						setExprAsText("\'" + valueAsString.charAt(0) + "\'");
					} else {
						setExprAsObject(obj);
					}
					// readError = getValidityReport();
					h.close();
				} else
					System.out
							.println("ValueMember.XreadObject:  Tag  <Value>  of  '"
									+ getName() + "'  not found");
			} else if (h.readAttr("variable") != "") {
				if (h.readSubTag("Value")) {
					Object obj = h.getAttrValue("String");
					if (obj == null)
						obj = h.getAttrValue("string");
					if (obj == null) {
						h.close();
						return;
					}
					if (obj instanceof String) {
						setExprAsText((String) obj);
						// readError = getValidityReport();
					}
					h.close();
				}
			} else {
				if (h.readSubTag("Value")) {
					Object obj = null;
					String javaTag = h.readSubTag();
					if (javaTag.equals("java")) {							
						obj = h.getAttrValue("String");
						if (obj == null)
							obj = h.getAttrValue("string");
					}
					if (javaTag.equals("string") || javaTag.equals("String")) {								
						obj = h.getElementData(h.top());
					}
					h.close();					
//					obj = h.getAttrValue("String"); // old code, before Java7
					if (obj == null) {
						h.close();
						return;
					}
					if (obj instanceof String) {
						// loesche '\n' und mehrere Leerzeichen aus dem object-String
						String objStr = formString((String) obj);
						setExprAsText(objStr);
					}
					h.close();
				}
			}
		} else { // lese altes XML Fileformat
			// System.out.println("ValueMember.XreadObject: Version before 1.0");
			String val = h.readAttr("value");
			if (h.readAttr("constant") != "") {
				String v = h.unescapeString(val);
				byte[] buf = v.getBytes();
				ByteArrayInputStream is = new ByteArrayInputStream(buf);
				try {
					ObjectInputStream ois = new ObjectInputStream(is);
					Object vo = ois.readObject();
					if (this.decl.getType().toString().equals("int")
							|| this.decl.getType().toString().equals("boolean")
							|| this.decl.getType().toString().equals("float")
							|| this.decl.getType().toString().equals("double")
							|| this.decl.getType().toString().equals("long")
							|| this.decl.getType().toString().equals("short")) {
						setExprAsEvaluatedText(vo.toString());
					} else {
						setExprAsObject(vo);
					}
					// readError = getValidityReport();

				} catch (Exception e) {
				}
			} else {
				setExprAsText(val);
				// readError = getValidityReport();
			}
		}
	}

	// loesche '\n' und mehrere Leerzeichen aus dem s
	private String formString(String s) {
		String s1 = s.toString();
		String s2 = s1.replaceAll("\n", "");
		while (s2.indexOf("  ") != -1) {
			s1 = s2.replaceAll("  ", " ");
			s2 = s1.toString();
		}

		return s1;
	}

	public void removeErrorMsg() {
		this.errorMsg = "";
		this.currentException = null;
	}
}
/*
 * ============================================================================
 * $Log: ValueMember.java,v $
 * Revision 1.45  2010/11/29 08:42:25  olga
 * tuning
 *
 * Revision 1.44  2010/11/29 08:31:37  olga
 * new method: trySetExprAsText(String)
 *
 * Revision 1.43  2010/11/28 22:11:37  olga
 * new method
 *
 * Revision 1.42  2010/11/04 10:56:25  olga
 * tuning
 *
 * Revision 1.41  2010/09/23 08:14:09  olga
 * tuning
 *
 * Revision 1.40  2010/08/05 14:12:32  olga
 * tuning
 *
 * Revision 1.39  2010/05/05 16:15:43  olga
 * tuning and tests
 *
 * Revision 1.38  2010/05/03 21:58:48  olga
 * tuning
 *
 * Revision 1.37  2010/04/29 15:14:07  olga
 * tuning and tests
 *
 * Revision 1.36  2010/04/28 15:15:39  olga
 * tuning
 *
 * Revision 1.35  2010/04/01 10:12:44  olga
 * tuning
 *
 * Revision 1.34  2010/03/31 21:08:13  olga
 * tuning
 *
 * Revision 1.33  2010/03/21 21:22:54  olga
 * tuning
 *
 * Revision 1.32  2010/03/08 15:37:42  olga
 * code optimizing
 *
 * Revision 1.31  2009/11/23 08:53:32  olga
 * tuning
 *
 * Revision 1.30  2009/07/02 11:10:50  olga
 * improved: getExprAsText(String, boolean)
 *
 * Revision 1.29  2009/06/30 09:50:22  olga
 * agg.xt_basis.GraphObject: added: setObjectName(String), getObjectName()
 * agg.xt_basis.Node, Arc: changed: save, load the object name
 * agg.editor.impl.EdGraphObject: changed: String getTypeString() - contains object name if set
 *
 * workaround of Applicability of Rule Sequences and Object Flow
 *
 * Revision 1.28  2009/03/19 09:31:06  olga
 * CPE: attr check improved
 *
 * Revision 1.27  2009/03/12 10:57:44  olga
 * some changes in CPA of managing names of the attribute variables.
 *
 * Revision 1.26  2008/05/19 09:19:32  olga
 * Applicability of Rule Sequence - reworking
 *
 * Revision 1.25  2008/05/08 14:59:19  olga
 * Tuning Applicability of Rule Sequences
 *
 * Revision 1.24  2008/04/07 09:36:50  olga
 * Code tuning: refactoring + profiling
 * Extension: CPA - two new options added
 *
 * Revision 1.23  2007/12/10 08:42:58  olga
 * CPA of grammar with node type inheritance for attributed graphs - bug fixed
 *
 * Revision 1.22  2007/11/05 09:18:17  olga
 * code tuning
 *
 * Revision 1.21  2007/11/01 09:58:13  olga
 * Code refactoring: generic types- done
 *
 * Revision 1.20  2007/09/24 09:42:34  olga
 * AGG transformation engine tuning
 *
 * Revision 1.19  2007/09/17 10:50:00  olga
 * Bug fixed in graph transformation by rules with NACs and PACs .
 * AGG help docus extended by new implemented features.
 *
 * Revision 1.18  2007/09/10 13:05:19  olga
 * In this update:
 * - package xerces2.5.0 is not used anymore;
 * - class com.objectspace.jgl.Pair is replaced by the agg own generic class agg.util.Pair;
 * - bugs fixed in:  usage of PACs in rules;  match completion;
 * 	usage of static method calls in attr. conditions
 * - graph editing: added some new features
 * Revision 1.17 2007/06/13 08:33:09 olga Update:
 * V161
 * 
 * Revision 1.16 2007/05/07 07:59:30 olga CSP: extentions of CSP variables
 * concept
 * 
 * Revision 1.15 2007/04/19 18:02:02 olga Gluing nodes bug fixed
 * 
 * Revision 1.14 2007/04/19 14:50:04 olga Loading grammar - tuning
 * 
 * Revision 1.13 2007/03/28 10:00:32 olga - extensive changes of Node/Edge Type
 * Editor, - first Undo implementation for graphs and Node/edge Type editing and
 * transformation, - new / reimplemented options for layered transformation, for
 * graph layouter - enable / disable for NACs, attr conditions, formula - GUI
 * tuning
 * 
 * Revision 1.12 2006/12/18 08:33:48 olga Code optimization
 * 
 * Revision 1.11 2006/12/13 13:32:58 enrico reimplemented code
 * 
 * Revision 1.10 2006/11/02 10:41:26 enrico interface extension for AGG
 * 
 * Revision 1.9 2006/08/02 09:00:57 olga Preliminary version 1.5.0 with -
 * multiple node type inheritance, - new implemented evolutionary graph layouter
 * for graph transformation sequences
 * 
 * Revision 1.8 2006/05/29 07:59:42 olga GUI, undo delete - tuning.
 * 
 * Revision 1.7 2006/04/03 08:57:50 olga New: Import Type Graph and some bugs
 * fixed
 * 
 * Revision 1.6 2006/03/01 09:55:47 olga - new CPA algorithm, new CPA GUI
 * 
 * Revision 1.5 2006/01/16 09:36:43 olga Extended attr. setting
 * 
 * Revision 1.4 2005/12/21 14:50:09 olga tests
 * 
 * Revision 1.3 2005/11/07 09:38:07 olga Null pointer during retype attr. member
 * fixed.
 * 
 * Revision 1.2 2005/09/19 09:12:15 olga CPA GUI tuning
 * 
 * Revision 1.1 2005/08/25 11:56:57 enrico *** empty log message ***
 * 
 * Revision 1.3 2005/07/11 09:30:20 olga This is test version AGG V1.2.8alfa .
 * What is new: - saving rule option <disabled> - setting trigger rule for layer -
 * display attr. conditions in gragra tree view - CPA algorithm <dependencies> -
 * creating and display CPA graph with conflicts and/or dependencies based on
 * (.cpx) file
 * 
 * Revision 1.2 2005/06/20 13:37:03 olga Up to now the version 1.2.8 will be
 * prepared.
 * 
 * Revision 1.1 2005/05/30 12:58:03 olga Version with Eclipse
 * 
 * Revision 1.34 2005/03/03 13:48:42 olga - Match with NACs and attr. conditions
 * with mixed variables - error corrected - save/load class packages written by
 * user - PACs : creating T-equivalents - improved - save/load matches of the
 * rules (only one match of a rule) - more friendly graph/rule editor GUI - more
 * syntactical checks in attr. editor
 * 
 * Revision 1.33 2005/01/28 14:02:32 olga -Fehlerbehandlung beim Typgraph check
 * -Erweiterung CP GUI / CP Menu -Fehlerbehandlung mit identification option
 * -Fehlerbehandlung bei Rule PAC
 * 
 * Revision 1.32 2005/01/05 08:56:13 olga Source tuning
 * 
 * Revision 1.31 2004/12/20 14:53:47 olga Changes because of matching
 * optimisation.
 * 
 * Revision 1.30 2004/09/23 08:26:43 olga Fehler bei CPs weg, Debug output in
 * file
 * 
 * Revision 1.29 2004/09/13 10:21:14 olga Einige Erweiterungen und
 * Fehlerbeseitigung bei CPs und Graph Grammar Transformation
 * 
 * Revision 1.28 2004/07/16 13:02:02 olga TypeGraph OK
 * 
 * Revision 1.27 2004/07/15 11:13:10 olga CPs letzter Schliff
 * 
 * Revision 1.26 2004/06/09 11:32:54 olga Attribute-Eingebe/Bedingungen : NAC
 * kann jetzt eigene Variablen und Bedingungen haben. CP Berechnung korregiert.
 * 
 * Revision 1.25 2004/05/06 17:23:26 olga graph matching OK
 * 
 * Revision 1.24 2004/04/28 12:46:38 olga test CSP
 * 
 * Revision 1.23 2004/03/01 15:47:55 olga Tests
 * 
 * Revision 1.22 2004/01/07 09:37:15 olga test
 * 
 * Revision 1.21 2003/12/18 16:25:49 olga Tests.
 * 
 * Revision 1.20 2003/10/16 08:20:41 olga XML Ausgabe angepasst
 * 
 * Revision 1.19 2003/09/25 09:36:17 olga XreadObject, XwriteObject : Ausgabe
 * von String Typ umgestellt
 * 
 * Revision 1.18 2003/07/17 17:20:16 olga Primitive Datentypen
 * 
 * Revision 1.17 2003/07/16 14:49:19 olga Korrektur an den primitiven Datentypen
 * 
 * Revision 1.16 2003/07/10 17:41:55 olga Parameter
 * 
 * Revision 1.15 2003/07/10 14:03:26 olga Fehler bei XML Ausgabe fuer
 * char/Character eingebaut
 * 
 * Revision 1.14 2003/03/05 18:24:22 komm sorted/optimized import statements
 * 
 * Revision 1.13 2003/01/15 16:29:06 olga apply angepasst
 * 
 * Revision 1.12 2003/01/15 11:35:44 olga Neue VerboseControl Konstante
 * :logJexParser zum Testen
 * 
 * Revision 1.11 2002/12/20 11:25:06 olga Tests
 * 
 * Revision 1.10 2002/12/19 14:24:28 olga XML Ausgabe korregiert
 * 
 * Revision 1.9 2002/12/18 16:28:51 olga Einlesen von komplexen Ausdruecken in
 * der rechten Regelseite DONE
 * 
 * Revision 1.8 2002/12/18 14:14:54 olga Fehler beim Einlesen von leeren Strings
 * bei Attributen im Arbeitsgraphen korregiert
 * 
 * Revision 1.7 2002/11/25 14:56:27 olga Der Fehler unter Windows 2000 im
 * AttributEditor ist endlich behoben. Es laeuft aber mit Java1.3.0 laeuft
 * endgueltig nicht. Also nicht Java1.3.0 benutzen!
 * 
 * Revision 1.6 2002/11/11 09:41:16 olga Nur Testausgaben
 * 
 * Revision 1.5 2002/10/31 14:51:14 olga Verbesserung der Fehlerbehandlung beim
 * laden.
 * 
 * Revision 1.4 2002/10/30 18:24:35 olga Attribute
 * 
 * Revision 1.3 2002/10/30 18:06:43 olga Aenderung an der XML Ausgabe von
 * Values.
 * 
 * Revision 1.2 2002/09/23 12:23:57 komm added type graph in xt_basis, editor
 * and GUI
 * 
 * Revision 1.1.1.1 2002/07/11 12:17:02 olga Imported sources
 * 
 * Revision 1.22 2001/05/14 12:10:43 olga Aenderungen wegen GenGEd: Match
 * Variable auf Variable - als Ergebnis nach Termersaetzung erscheint die
 * Variable in Klemmern: (x). Nur im Fall mit nur eine Variable werden die
 * Klammern weggelassen. Andere Aenderungen: Tests.
 * 
 * Revision 1.21 2001/03/22 15:48:41 olga NULL Abfrage eingebaut.
 * 
 * Revision 1.20 2001/03/15 18:19:20 olga *** empty log message ***
 * 
 * Revision 1.19 2001/03/15 17:52:51 olga *** empty log message ***
 * 
 * Revision 1.18 2001/03/15 17:18:58 olga *** empty log message ***
 * 
 * Revision 1.17 2001/03/14 17:30:19 olga Test
 * 
 * Revision 1.16 2001/02/21 15:49:15 olga Neue Methoden fuer den Parser.
 * 
 * Revision 1.15 2001/02/15 16:00:03 olga Einige Aenderungen wegen XML
 * 
 * Revision 1.14 2000/12/21 09:48:48 olga In dieser Version wurden XML und GUI
 * Reimplementierung zusammen gefuehrt.
 * 
 * Revision 1.13 2000/12/07 14:23:35 matzmich XML-Kram Man beachte: xerces
 * (/home/tfs/gragra/AGG/LIB/Xerces/xerces.jar) wird jetzt im CLASSPATH
 * benoetigt.
 * 
 * Revision 1.12.6.1 2000/12/04 13:25:52 olga Erste Stufe der GUI
 * Reimplementierung abgeschlossen: - AGGAppl.java optimiert - Print eingebaut
 * (GraGraPrint.java) - GraGraTreeView.java, GraGraEditor.java optimiert - Event
 * eingebaut - GraTra umgestellt
 * 
 * ======= Revision 1.12.6.1 2000/12/04 13:25:52 olga Erste Stufe der GUI
 * Reimplementierung abgeschlossen: - AGGAppl.java optimiert - Print eingebaut
 * (GraGraPrint.java) - GraGraTreeView.java, GraGraEditor.java optimiert - Event
 * eingebaut - GraTra umgestellt
 * 
 * ======= 1.12.6.1 Revision 1.12 2000/06/05 14:07:54 shultzke Debugausgaben
 * fuer V1.0.0b geloescht
 * 
 * Revision 1.11 2000/05/24 10:02:32 olga Nur Testausgaben eingebaut bei der
 * Suche nach dem Fehler in DISAGG : Match Konstante auf Konstante
 * 
 * Revision 1.10 2000/05/17 11:57:01 olga Testversion an Gabi mit diversen
 * Aenderungen. Fehler sind moeglich!!
 * 
 * Revision 1.9 2000/04/05 12:09:26 shultzke serialVersionUID aus V1.0.0
 * generiert
 * 
 * Revision 1.8 2000/03/15 08:18:25 olga Die Aenderungen betraffen nur
 * serialVersionUID in einigen Files, um alte Beispiele zu laden. Noch zu
 * klaeren od wir die alte Beispiele am Leben erhalten wollen.
 * 
 * Revision 1.7 2000/03/14 10:57:34 shultzke Transformieren von Variablen auf
 * Variablen sollte jetzt funktionieren
 * 
 * Revision 1.6 2000/03/03 11:40:53 shultzke Einige Zeilen Code aus einenader
 * gezogen, damit ich besser debuggen kann
 * 
 * Revision 1.5 1999/10/11 09:31:58 olga *** empty log message ***
 * 
 * Revision 1.4 1999/09/23 13:12:31 shultzke Exception ist transient, damit sie
 * nicht abgespeichert wird.
 */
