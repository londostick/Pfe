/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
/**
 * 
 */
package agg.parser;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import agg.attribute.AttrContext;
import agg.attribute.AttrInstance;
import agg.attribute.AttrType;
import agg.attribute.impl.CondMember;
import agg.attribute.impl.CondTuple;
import agg.attribute.impl.DeclTuple;
import agg.attribute.impl.ValueMember;
import agg.attribute.impl.ValueTuple;
import agg.attribute.impl.VarMember;
import agg.attribute.impl.VarTuple;
import agg.util.Pair;
import agg.util.Triple;
import agg.xt_basis.Arc;
import agg.xt_basis.BadMappingException;
import agg.xt_basis.BaseFactory;
import agg.xt_basis.Graph;
import agg.xt_basis.GraphObject;
import agg.xt_basis.Match;
import agg.xt_basis.Node;
import agg.xt_basis.OrdinaryMorphism;
import agg.xt_basis.Rule;
import agg.xt_basis.Type;
import agg.xt_basis.TypeError;
import agg.xt_basis.TypeSet;

/**
 * @author olga
 *
 */
public final class ExcludePairHelper {

	public static boolean isMatchValid(
			final Rule r, 
			final Match m,
			OrdinaryMorphism nac_NotToCheck, 
			boolean firstRule,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> overlapping) {

		boolean withNACs = true;
		m.getTarget().setCompleteGraph(false);
		if (!m.isTotal()) {
			return false;
		} else if (!m.isValid(true)) {
			return false;
		} 
//		else if (!tryToValidateAttrCondition(r, m, overlapping)) {
//			return false;
//		}
		else {
			if (withNACs) {
				boolean nacOK = true;
				final List<OrdinaryMorphism> nacs = r.getNACsList();
				for (int l=0; l<nacs.size() && nacOK ; l++) {
					final OrdinaryMorphism nac = nacs.get(l);				
					if (nac.isEnabled()
							&& ((nac_NotToCheck == null) || (nac_NotToCheck != nac))) {
						OrdinaryMorphism nacStar = (OrdinaryMorphism) m.checkNAC(
								nac, true);
						if (nacStar != null) {
							if (firstRule) {
								nacStar.dispose();
								nacStar = null;
								return false;
							} else if (!hasVariableInContext(nac, r.getAttrContext())
									&& !hasConstantToVariableMappingInContext(nacStar)
									&& !hasConstantInAttrOfNewObj(nac)
									) {
								nacStar.dispose();
								nacStar = null;
								return false;
							} else {
								nacStar.dispose();
								nacStar = null;
							}
						}
					}
				}
			}
		}
		return true;
	}

	protected static boolean hasVariableInContext(final OrdinaryMorphism morph, 
			final AttrContext relatedAttrContext) {	
		
		final VarTuple vars = (VarTuple) morph.getAttrContext().getVariables();
		final CondTuple conds = (CondTuple) morph.getAttrContext().getConditions();
		
		return hasVarInContext(vars, conds, morph,  relatedAttrContext,
								morph.getTarget().getNodesSet().iterator())
				|| hasVarInContext(vars, conds, morph,  relatedAttrContext,
						morph.getTarget().getArcsSet().iterator());
	}

	protected static boolean hasVarInContext(
			final VarTuple vars,
			final CondTuple conds,
			final OrdinaryMorphism morph, 
			final AttrContext relatedAttrContext,
			final Iterator<?> elems) {	
		
		while (elems.hasNext()) {
			GraphObject o = (GraphObject) elems.next();
			if (o.getAttribute() == null) {
				continue;
			}
			if (!morph.getInverseImage(o).hasMoreElements()) {
				ValueTuple vt = (ValueTuple) o.getAttribute();
				for (int k = 0; k < vt.getSize(); k++) {
					ValueMember vm = vt.getValueMemberAt(k);
					if (vm.isSet()) {
						if (vm.getExpr().isVariable()) {
							VarMember varm = null;
							if (relatedAttrContext == null) 
								varm = vars.getVarMemberAt(vm.getExprAsText());
							else {
								varm = ((VarTuple)relatedAttrContext.getVariables()).getVarMemberAt(vm.getExprAsText());
							}
							if (varm != null && varm.isInputParameter()) {
								return true;								
							}
							
							for (int i = 0; i < conds.getSize(); i++) {
								CondMember cm = (CondMember) conds.getValueMemberAt(i);
								if (cm.getAllVariableNamesOfExpression()
											.contains(vm.getExprAsText())) {
									return true;
								}
							}
							
						}
					}
				}
			} else {
				GraphObject src = morph.getInverseImage(o).nextElement();
				ValueTuple srcvt = (ValueTuple) src.getAttribute();
				ValueTuple vt = (ValueTuple) o.getAttribute();
				for (int k = 0; k < vt.getSize(); k++) {
					ValueMember vm = vt.getValueMemberAt(k);
					ValueMember srcvm = srcvt.getValueMemberAt(k);
					
					if (srcvm != null && vm.isSet()) {
						if (vm.getExpr().isVariable()
								&& !srcvm.isSet()) {
							VarMember varm = null;
							if (relatedAttrContext == null) 
								varm = vars.getVarMemberAt(vm.getExprAsText());
							else {
								varm = ((VarTuple)relatedAttrContext.getVariables()).getVarMemberAt(vm.getExprAsText());
							}
							if (varm != null) {
								if (varm.isInputParameter()) {
									return true;								
								}
								
								for (int i = 0; i < conds.getSize(); i++) {
									CondMember cm = (CondMember) conds.getValueMemberAt(i);
									if (cm.getAllVariableNamesOfExpression()
											.contains(vm.getExprAsText())) {
										return true;
									}
								}
								
							}
						}
					} 
//					else {
//						if (srcvm.isSet()
//								&& srcvm.getExpr().isVariable()) {
//						}
//					}
				}
			}
		}		
		return false;
	}

	
	protected static boolean hasConstantToVariableMappingInContext(final OrdinaryMorphism morph) {
		final Enumeration<GraphObject> e = morph.getDomain();
		while (e.hasMoreElements()) {
			final GraphObject o = e.nextElement();
			final GraphObject img = morph.getImage(o); 
			if (o.getAttribute() == null || img.getAttribute() == null)
				continue;
			 
			final ValueTuple vt = (ValueTuple) o.getAttribute();
			final ValueTuple vtimg = (ValueTuple) img.getAttribute();
			for (int k = 0; k < vt.getSize(); k++) {
				final ValueMember vm = vt.getValueMemberAt(k);
				final ValueMember vmimg = vtimg.getValueMemberAt(vm.getName());
//				System.out.println("hasConstantToVariableMappingInContext :  "+vm.getExpr()+"    "+vmimg.getExpr());
				if (vmimg != null &&
						(vm.getExpr() != null) && vm.getExpr().isConstant()
						&& (vmimg.getExpr() != null) && vmimg.getExpr().isVariable()) {
					return true;
				}
			}
		}
		return false;
	}
	
	protected static boolean hasConstantInAttrOfNewObj(final OrdinaryMorphism morph) {
		
		return hasConstInAttrOfNewObj(morph, morph.getTarget().getNodesSet().iterator())
				|| hasConstInAttrOfNewObj(morph, morph.getTarget().getArcsSet().iterator());
	}
	
	protected static boolean hasConstInAttrOfNewObj(
			final OrdinaryMorphism morph,
			final Iterator<?> elems) {
 
		while (elems.hasNext()) {
			GraphObject o = (GraphObject) elems.next();
			if (o.getAttribute() == null)
				continue;
			if (!morph.getInverseImage(o).hasMoreElements()) {
				ValueTuple vt = (ValueTuple) o.getAttribute();
				for (int k = 0; k < vt.getSize(); k++) {
					ValueMember vm = vt.getValueMemberAt(k);
					if ((vm.getExpr() != null) && vm.getExpr().isConstant()) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	protected static boolean hasVariableInAttrOfNewObj(final OrdinaryMorphism morph) {
		
		return hasVarInAttrOfNewObj(morph, morph.getTarget().getNodesSet().iterator())
				|| hasVarInAttrOfNewObj(morph, morph.getTarget().getArcsSet().iterator());
	}

	protected static boolean hasVarInAttrOfNewObj(
			final OrdinaryMorphism morph,
			final Iterator<?> elems) {
 
		while (elems.hasNext()) {
			GraphObject o = (GraphObject) elems.next();
			if (o.getAttribute() == null)
				continue;
			if (!morph.getInverseImage(o).hasMoreElements()) {
				ValueTuple vt = (ValueTuple) o.getAttribute();
				for (int k = 0; k < vt.getSize(); k++) {
					ValueMember vm = vt.getValueMemberAt(k);
					if ((vm.getExpr() != null) && vm.getExpr().isVariable()) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	protected static boolean graphSatisfiesTypeMaxMultiplicity(
			final OrdinaryMorphism morph) {
		
		TypeError error = morph.getTarget().getTypeSet()
										.checkTypeMaxMultiplicity(morph.getTarget(),
													TypeSet.ENABLED_MAX);
		if (error != null) {
//			System.out.println(error.getMessage());
			return false;
		}
		
		return true;
	}
	
	
	//TODO: NACs with attr. condition which might be evaluated
	
	
	protected static boolean isAttrMemberChangedFromLeftToRight(
			final Rule rule1,
			final Rule rule2,
			final ValueMember changedLeftMemberOfRule1,		
			final GraphObject lhsRule1Object,
			final GraphObject lhsRule2Object,
			final OrdinaryMorphism nacInsideOverlapGraph,
			final GraphObject overlapObject) {
		
		boolean result = false;
		
		final ValueMember rightMember = (ValueMember) lhsRule2Object
				.getAttribute().getMemberAt(changedLeftMemberOfRule1.getName());
		if (rightMember == null)
			return result;
		
		final GraphObject r1Object = rule1.getImage(lhsRule1Object);
		final ValueMember l1Member = (ValueMember) lhsRule1Object
										.getAttribute().getMemberAt(
												changedLeftMemberOfRule1.getName());
		final ValueMember r1Member = (ValueMember) r1Object
										.getAttribute().getMemberAt(
												changedLeftMemberOfRule1.getName());
		
		if (rightMember.isSet()) {
			if (rightMember.getExpr().isConstant()) {
				if (r1Member.isSet()) {
					if (r1Member.getExpr().isConstant()) {
						if (!r1Member.getExprAsText().equals(
								rightMember.getExprAsText())) {
							// attr. set to another constant
							if (nacInsideOverlapGraph != null) {
								if (overlapObject.getContextUsage() 
										== nacInsideOverlapGraph.hashCode()) {
									final ValueMember vm = (ValueMember) overlapObject
											.getAttribute()
											.getMemberAt(
													r1Member
															.getName());
									if (vm.getExpr().isConstant()) {
										result = true;
									}
								}
							} else
								// overlapObject inside LHS of r2
								result = true;
						} else {
							if (nacInsideOverlapGraph != null) {
								if (overlapObject.getContextUsage()
										 == nacInsideOverlapGraph.hashCode()) {
									final ValueMember vm = (ValueMember) 
													overlapObject
														.getAttribute()
															.getMemberAt(r1Member.getName());
									if (vm.getExpr().isConstant()) {
										if (!vm.getExprAsText()
												.equals(r1Member.getExprAsText()))
											result = true;
									}
								}
							}
						}
					} else if (r1Member.getExpr().isVariable()) {
						if (!l1Member.isSet()) {
							result = true;
						} else if (!l1Member.getExprAsText()
								.equals(r1Member.getExprAsText())) {
							result = true;
						}
					} else { // if(r1Member.getExpr().isComplex()){
						result = true;
					}
				}
			} else if (rightMember.getExpr().isVariable()) {
				// changed attribute used from another rule
				if (!rightMember.isTransient()) {
					result = true;
				}
				// kommt variable mehrfach in l2 vor
				final String variableName = rightMember.getExprAsText();
				Iterator<?> en = rule2.getLeft().getNodesSet().iterator();
				while (!result && en.hasNext()) {
					final GraphObject grob = (GraphObject) en.next();
					Vector<ValueMember> 
					variableMember = findMemberWhichUsesVariable(variableName, grob);
					if (variableMember != null) {
						for (int k = 0; k < variableMember.size()
								&& !result; k++) {
							final ValueMember vm = variableMember.elementAt(k);
							if (vm.hashCode() != rightMember.hashCode()) {
								result = true;
							}
						}
						variableMember = null;
					}
				}
				en = rule2.getLeft().getArcsSet().iterator();
				while (!result && en.hasNext()) {
					final GraphObject grob = (GraphObject) en
							.next();
					Vector<ValueMember> 
					variableMember = findMemberWhichUsesVariable(variableName, grob);
					if (variableMember != null) {
						for (int k = 0; k < variableMember.size()
								&& !result; k++) {
							final ValueMember vm = variableMember.elementAt(k);
							if (vm.hashCode() != rightMember.hashCode()) {
								result = true;
							}
						}
					}
					variableMember = null;
				}
			} 
		}
		else {
			// attr. set to another constant
			if (nacInsideOverlapGraph != null) {
				if (overlapObject.getContextUsage() 
						== nacInsideOverlapGraph.hashCode()) {
					final ValueMember vm = (ValueMember) overlapObject.getAttribute()
													.getMemberAt(r1Member.getName());
					if (vm.getExpr().isConstant()
							/*&& !vm.getExprAsText().equals(r1Member.getExprAsText())*/) {
						result = true;
					}
				}
			} 
		}
		return result;
	}
	
	protected static boolean isAttrMemberChangedFromPACRule2ToRight(
			final Rule rule1, 
			final Rule rule2, 
			final GraphObject l1Object,
			final GraphObject overlapObject,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> helpPair) {
		
		boolean result = false;
		// when l2Objects are not found inside of LHS2
		// try to find it inside of PAC part of the extended LHS2
		// helpPair: Pair(extendedByPACsL2iso, rStar)
		final Enumeration<GraphObject> l2Objects = helpPair.second.getInverseImage(overlapObject);
		if (l2Objects.hasMoreElements()) {
			GraphObject l2Object = l2Objects.nextElement();
			Vector<ValueMember> 
			changedMembersR1 = ExcludePairHelper.getChangedAttributeMember(rule1,l1Object);
			if (changedMembersR1 != null) {
				for (int j = 0; j < changedMembersR1.size() && !result; j++) {
					ValueMember leftMember = changedMembersR1.elementAt(j);
					ValueMember rightMember = (ValueMember) l2Object
							.getAttribute().getMemberAt(
									leftMember.getName());
					if (rightMember.isSet()) {
						GraphObject r1Object = rule1.getImage(l1Object);
						ValueMember l1Member = (ValueMember) l1Object
								.getAttribute().getMemberAt(
										leftMember.getName());
						ValueMember r1Member = (ValueMember) r1Object
								.getAttribute().getMemberAt(
										leftMember.getName());
						if (rightMember.getExpr().isConstant()) {
							if (r1Member.isSet()
									&& (!r1Member.getExpr().isConstant() 
											|| (!r1Member.getExpr().equals(l1Member.getExpr())
													&& !r1Member.getExpr().equals(rightMember.getExpr())))) {
								// PAC attribut is constant
								// and r1 changed the attr value =>
								// conflict!
								result = true;
							}
						} else if (rightMember.getExpr().isVariable()) {
							if (!rightMember.isTransient()) {
								result = true;
							}
						}
					}
				}
				changedMembersR1 = null;
			}
		}
		return result;
	}
	
	protected static boolean isAttrMemberChangedFromNACRule2ToRight(
			final Rule rule1, 
			final Rule rule2, 
			final Pair<OrdinaryMorphism, OrdinaryMorphism> overlapping,
			final OrdinaryMorphism nacInsideOverlapGraph,
			final List<GraphObject> changedAttributesR1,
			final List<Type> forbiddenTypesR2 ,
			final Hashtable<ValueMember, Pair<String, String>> attrMember2Constant) {
		
		boolean result = false;
		Match match2 = (Match) overlapping.second;
		// check the NACs of the rule2
		final List<OrdinaryMorphism> nacs = rule2.getNACsList();
		for (int l=0; l<nacs.size() && !result; l++) {
			final OrdinaryMorphism nac = nacs.get(l);		
			if (!nac.isEnabled())
				continue;
			
			OrdinaryMorphism nacStar = (OrdinaryMorphism) match2.checkNAC(nac, true);
			if (nacInsideOverlapGraph == null) {
				if (nacStar != null) {
					if (!hasVariableInContext(nac)) {
						return false;
					}
				} else if (hasConstantInAttrOfNewObj(nac)) {
					Hashtable<ValueMember, String> var2const = new Hashtable<ValueMember, String>();
					var2const = replaceConstantByNull(rule2, nac);
					if (!var2const.isEmpty()) {
						nacStar = (OrdinaryMorphism) match2.checkNAC(nac,
								true);
						replaceNullByConstant(rule2, nac, var2const);
						var2const.clear();
					}
					var2const = null;
				}
			} 
//			else if ((nacStar != null)
//						&& (nacInsideOverlapGraph == nac)
//						&& !hasVariableInAttrOfNewObj(nac)
//						&& hasConstantInAttrOfNewObj(nac)) {
//				return false;
//			}
			
			if (nacStar != null) {
				Iterator<?> l1Objs = overlapping.first.getSource().getNodesSet().iterator();
				boolean nodechanged = checkNACStarAttributes(
						rule1, rule2, 
						nac, nacStar,
						match2, l1Objs,
						overlapping,
						changedAttributesR1, 
						forbiddenTypesR2,
						attrMember2Constant);
				if (!nacStar.isEnabled()) 
					return false;

				l1Objs = overlapping.first.getSource().getArcsSet().iterator();
				boolean arcchanged = checkNACStarAttributes(
							rule1, rule2, 
							nac, nacStar, 
							match2, l1Objs, 
							overlapping,
							changedAttributesR1, 
							forbiddenTypesR2,
							attrMember2Constant);
				if (!nacStar.isEnabled()) 
					return false;
					
				if (!nodechanged && !arcchanged)
					result = false;
				else {
					overlapping.first.getTarget().setHelpInfo(nac.getName());
					result = true;
				}
			}
		}
		return result;
	}
	
	public static void renameContextVariableOfOverlappingPair(
			final Rule r1,
			final Rule r2,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> pair,
			final String prefix1, 
			final String prefix2) {
		
		List<String> variableEqualitiesList = new Vector<String>();
		List<GraphObject> checked = new Vector<GraphObject>();		
		List<GraphObject> tocheck = new Vector<GraphObject>();
		
		count1 = 1;
		count2 = 1;

		final OrdinaryMorphism morph1 = pair.first;
		final OrdinaryMorphism morph2 = pair.second;
		
		final Graph graph = morph1.getImage();
		Iterator<?> objs = graph.getNodesSet().iterator();
		while (objs.hasNext()) {
			final GraphObject o = (GraphObject) objs.next();			
			if (morph1.getInverseImage(o).hasMoreElements()
					&& morph2.getInverseImage(o).hasMoreElements()) {				
				checked.add(o);				
				if (o.getAttribute() != null) {
					renameVariableOfOverlapObj(o, morph1, morph2, prefix1, prefix2, variableEqualitiesList);
				}
			}
			else {
				tocheck.add(o);
			}
		}
		objs = graph.getArcsSet().iterator();
		while (objs.hasNext()) {
			final GraphObject o = (GraphObject) objs.next();			
			if (morph1.getInverseImage(o).hasMoreElements()
					&& morph2.getInverseImage(o).hasMoreElements()) {				
				checked.add(o);				
				if (o.getAttribute() != null) {
					renameVariableOfOverlapObj(o, morph1, morph2, prefix1, prefix2, variableEqualitiesList);
				}
			}
			else {
				tocheck.add(o);
			}
		}
		
		renameSimilarVariables(r1, r2, morph1, morph2, checked, tocheck, prefix1, prefix2, variableEqualitiesList);
	
		if (!variableEqualitiesList.isEmpty()) {
			// save variable equalities as help info of the overlapping graph
			graph.setHelpInfo(graph.getHelpInfo()+":VariableEquality:"+variableEqualitiesList.toString());
		}
		variableEqualitiesList = null;
		checked = null;
		tocheck = null;
	}
	
	public static void renameContextVariableOfOverlapping(
			final Rule r1,
			final Rule r2,
			final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>> pair,
			final String prefix1, 
			final String prefix2) {
		
		renameContextVariableOfOverlappingPair(r1, r2, pair.first, prefix1, prefix2);
	}
	
	static int count1;
	static int count2;
	
	private static void renameVariableOfOverlapObj(
			final GraphObject o,
			final OrdinaryMorphism morph1,
			final OrdinaryMorphism morph2,
			final String prefix1,
			final String prefix2,
			final List<String> variableEqualitiesList) {
		
		// expected: o1 != null and o2 != null
		final GraphObject o1 = morph1.getInverseImage(o).nextElement();
		final GraphObject o2 = morph2.getInverseImage(o).nextElement();

		final ValueTuple vt = (ValueTuple) o.getAttribute();
		for (int i=0; i<vt.getNumberOfEntries(); i++) {
			final ValueMember vm = vt.getValueMemberAt(i);
			final ValueMember vm1 = ((ValueTuple) o1.getAttribute()).getValueMemberAt(vm.getName());
//			boolean const1 = false;
//			boolean const2 = false;
			if (vm.isSet() && vm.getExpr().isVariable()) {				
				// extend variable name of o1 by prefix1 "r1_"
				String part1 = "";
				if (vm1 != null && vm1.isSet()) {
					if (vm1.getExpr().isVariable()) {	
						if (!vm1.isTransient()) {
							part1 = vm1.getExprAsText();
							part1 = prefix1+part1;
						}
//						else {
//							part1 = vm.getExprAsText();
//							part1 = prefix1+part1;
//						}
					}
					else if (vm1.getExpr().isComplex()) {
						part1 = "expr" + count1;
						count1++;
						part1 = prefix1+part1;
						final String variableEquality = part1+"="+vm1.getExprAsText();
						saveVariableEquality(variableEqualitiesList, variableEquality);
					}
//					else if (vm1.getExpr().isConstant()) {
//						const1 = true;
//						part1 = "const" + count1;
//						count1++;
//						part1 = prefix1+part1;
//						final String variableEquality = part1+"="+vm1.getExprAsText();
//						saveVariableEquality(variableEqualitiesList, variableEquality);
//					}					
				}
				
				// extend variable name of o1 by prefix2 "r2_"	
				final ValueMember vm2 = ((ValueTuple) o2.getAttribute()).getValueMemberAt(vm.getName());
				String part2 = "";
				if (vm2 != null && vm2.isSet()) {
					if (vm2.getExpr().isVariable()) {
						if (!vm2.isTransient()) {
							part2 = vm2.getExprAsText();
							part2 = prefix2+part2;
						}
//						else {
//							part2 = vm.getExprAsText();
//							part2 = prefix2+part2;
//						}
					}
					else if (vm2.getExpr().isComplex()) {
						part2 = "expr" + count2;
						count2++;
						part2 = prefix2+part2;
						final String variableEquality = part2+"="+vm2.getExprAsText();
						saveVariableEquality(variableEqualitiesList, variableEquality);
					}
//					else if (vm2.getExpr().isConstant()) {
//						const2 = true;
//						part2 = "const" + count2;
//						count2++;
//						part2 = prefix2+part2;
//						final String variableEquality = part2+"="+vm2.getExprAsText();
//						saveVariableEquality(variableEqualitiesList, variableEquality);
//					}
				}

//				if (const2) {
//					vm.setExprAsObject(vm2.getExpr().getCopy());
//				} else if (const1) {
//					vm.setExprAsObject(vm1.getExpr().getCopy());
//				}
				
				// build variable name
				String varname = "";
				
				if (!part1.equals("") && !part2.equals("")) {
					varname = part1+"_"+part2;
					// save variable equality
					final String variableEquality = part1+"="+part2;										
					saveVariableEquality(variableEqualitiesList, variableEquality);
				} 
				else if (!part2.equals("")) {
					varname = part2;
				} 
				else if (!part1.equals("")) {
					varname = part1;
				} 
				
				if (!varname.equals("")
						&& vm.isSet() && vm.getExpr().isVariable()) {
					// rename variable of morph1
					final VarMember var1 = ((VarTuple) morph1.getAttrContext().getVariables())
													.getVarMemberAt(vm.getExprAsText());
					if (var1 != null) {
						var1.getDeclaration().setName(varname);
					}
					// rename variable of morph2
					final VarMember var2 = ((VarTuple) morph2.getAttrContext().getVariables())
													.getVarMemberAt(vm.getExprAsText());
					if (var2 != null) {
						var2.getDeclaration().setName(varname);
					}
					// set value of attr member
					vm.setExprAsText(varname);
				}
				// the value will be shown in the object
				vm.setTransient(false);
			}
		}
	}
	
	private static void saveVariableEquality(
			final List<String> variableEqualitiesList, 
			final String variableEquality) {
		boolean found = false;
		for (int i=0; i<variableEqualitiesList.size() && !found; i++) {
			String s = variableEqualitiesList.get(i);
			found = s.equals(variableEquality);
		}
		if (!found)
			variableEqualitiesList.add(variableEquality);
	}
		
	private static void renameSimilarVariables(
			final Rule r1, 
			final Rule r2,
			final OrdinaryMorphism morph1,
			final OrdinaryMorphism morph2,
			final List<GraphObject> checked,
			final List<GraphObject> tocheck,
			final String prefix1, 
			final String prefix2,
			final List<String> variableEqualitiesList) {

		List<GraphObject> checked2 = new Vector<GraphObject>();		
		for (int j=0; j<tocheck.size(); j++) {
			final GraphObject o = tocheck.get(j);
			if (o.getAttribute() == null) {
				continue;
			}
			
			OrdinaryMorphism morph = null;			
			GraphObject src1 = null;
			GraphObject src2 = null;
			ValueMember srcvm = null;
			
			if (morph1.getInverseImage(o).hasMoreElements()) {
				morph = morph1;
				src1 = morph1.getInverseImage(o).nextElement();
			}		
			else if (morph2.getInverseImage(o).hasMoreElements()) {
				morph = morph2;
				src2 = morph2.getInverseImage(o).nextElement();
			} 
			
			final ValueTuple value = (ValueTuple) o.getAttribute();
			for (int i = 0; i < value.getNumberOfEntries(); i++) {
				final ValueMember valuem = value.getValueMemberAt(i);
				String memberName = valuem.getName();
				
				if (valuem.isSet() 
						&& valuem.getExpr().isVariable()
						&& !valuem.isTransient()) {
					
					List<GraphObject> list = new Vector<GraphObject>();
					
					final String varName = valuem.getExprAsText();					

					if (src1 != null) {
						srcvm = ((ValueTuple) src1.getAttribute()).getValueMemberAt(memberName);
						list.addAll(getObjsWithVariable(
								varName, 
								src1, 
								morph1.getSource(), r1));
					}
					else if (src2 != null) {
						srcvm = ((ValueTuple) src2.getAttribute()).getValueMemberAt(memberName);
						list.addAll(getObjsWithVariable(
								varName, 
								src2,
								morph2.getSource(), r2));
					}
					
					if (list.size() > 1) {
						GraphObject src_l = null;
						GraphObject tar_l = null;
						for (int l=1; l<list.size(); l++) {
							src_l = list.get(l);
							if (morph != null)
								tar_l = morph.getImage(src_l);
							if (checked.contains(tar_l)
									|| checked2.contains(tar_l)) {
								break;
							} 
							tar_l = null;
						}						
						
						if (src_l != null && tar_l != null) {						
							final ValueTuple src_l_value = (ValueTuple) src_l.getAttribute();
							final ValueTuple tar_l_value = (ValueTuple) tar_l.getAttribute();
							 	
							final ValueMember src_l_vm = src_l_value.getValueMemberAt(memberName);
							final ValueMember tar_l_vm = tar_l_value.getValueMemberAt(memberName);
							if (src_l_vm != null && tar_l_vm != null) {
								String part1 = "";
								String part2 = "";
//								boolean const1 = false;
//								boolean const2 = false;	
								if (morph == morph1) {	
//									System.out.println(srcvm.getExprAsText()+"    "+valuem.getExprAsText()+"  (1)  "+src_l_vm.getExprAsText());
									if (src_l_vm.isSet()) {	
										if (src_l_vm.getExpr().isVariable()) {
											if (!src_l_vm.isTransient()) {
												part1 = src_l_vm.getExprAsText();
											} else {
												part1 = valuem.getExprAsText();
											}
										}
										else if (src_l_vm.getExpr().isComplex()) {
											part1 = "expr"+count1;
											final String variableEquality = prefix1+part1+"="+src_l_vm.getExprAsText();
											saveVariableEquality(variableEqualitiesList, variableEquality);	
										}
										if (!part1.equals(""))
											part1 = prefix1+part1;	
									}																			
									if (morph2.getInverseImage(tar_l).hasMoreElements()) {									
										src2 = morph2.getInverseImage(tar_l).nextElement();
										if (src2 != null) {
											final ValueMember src2vm = ((ValueTuple) src2.getAttribute()).getValueMemberAt(memberName);										
	//										System.out.println(srcvm.getExprAsText()+"    "+valuem.getExprAsText()+"  (2)  "+src2vm.getExprAsText());
											if (src2vm != null && src2vm.isSet()) {
												if (src2vm.getExpr().isVariable()) {
													if (!src2vm.isTransient()
															&& src2vm.getExprAsText().equals(varName)) {									
														part2 = src2vm.getExprAsText();
													} else if (src_l_vm.isTransient()){
														part2 = valuem.getExprAsText();
														part1 = "";
													}
												}
	//											else if (src2vm.getExpr().isComplex()) {
	//												part2 = "expr"+count2;
	//												final String variableEquality = prefix2+part2+"="+src2vm.getExprAsText();
	//												saveVariableEquality(variableEqualitiesList, variableEquality);
	//											}
												if (!part2.equals(""))
													part2 = prefix2+part2;
											}									
										} 
										if (!part2.equals("")) {
											valuem.setExprAsText(part2);
										} else if (!part1.equals("")) {
											valuem.setExprAsText(part1);
										} 
										valuem.setTransient(false);
									}
								}
								else if (morph == morph2) {	
//									System.out.println(srcvm.getExprAsText()+"    "+valuem.getExprAsText()+"  (2)  "+src_l_vm.getExprAsText());
									if (src_l_vm.isSet()) {
										if (src_l_vm.getExpr().isVariable()) {
											if (!src_l_vm.isTransient()) {
												part2 = src_l_vm.getExprAsText();	
											}
											else {
												part2 = valuem.getExprAsText();
											}
										}
										else if (src_l_vm.getExpr().isComplex()) {
											part2 = "expr"+count2;
											final String variableEquality = prefix2+part2+"="+src_l_vm.getExprAsText();
											saveVariableEquality(variableEqualitiesList, variableEquality);
										}
										if (!part2.equals(""))
											part2 = prefix2+part2;
									}
									if (morph1.getInverseImage(tar_l).hasMoreElements()) {
										src1 = morph1.getInverseImage(tar_l).nextElement();
										if (src1 != null) {
											final ValueMember src1vm = ((ValueTuple) src1.getAttribute()).getValueMemberAt(memberName);
	//										System.out.println(srcvm.getExprAsText()+"    "+valuem.getExprAsText()+"  (1)  "+src1vm.getExprAsText());
											if (src1vm != null && src1vm.isSet()) {
												if (src1vm.getExpr().isVariable()) {
													if (!src1vm.isTransient()
															&& src1vm.getExprAsText().equals(varName)) {
														part1 = src1vm.getExprAsText();
													} 
													else if (src_l_vm.isTransient()){
														part1 = valuem.getExprAsText();
														part2 = "";	
													}
												}
	//											else if (src1vm.getExpr().isComplex()) {
	//												part1 = "expr"+count1;
	//												final String variableEquality = prefix1+part1+"=1"+src1vm.getExprAsText();
	//												saveVariableEquality(variableEqualitiesList, variableEquality);
	//											}
												if (!part1.equals(""))
													part1 = prefix1+part1;
											}										
										}
	//									System.out.println(part1+"   "+part2);
										if (!part2.equals("")) {
											valuem.setExprAsText(part2);
										}
										else if (!part1.equals("")) {
											valuem.setExprAsText(part1);
										} 
										valuem.setTransient(false);
									}
								}
								
								if (!part1.equals("") && !part2.equals("")) {
										final String variableEquality = part1+"="+part2;
										saveVariableEquality(variableEqualitiesList, variableEquality);	
								}
							}
							
							checked2.add(o);
						}
					}
					else {
						if (srcvm != null) {
//							System.out.println(valuem.getExprAsText()+"  (0)  "+srcvm.getExprAsText());
							if (srcvm.isSet()) {
								String part = "";	
								if (srcvm.getExpr().isVariable() && !srcvm.isTransient()) { 
									if (!srcvm.isTransient()) {
										part = srcvm.getExprAsText();									
									} else {
										part = valuem.getExprAsText();
									}
								}
								else if (srcvm.getExpr().isComplex()) {
									if (morph == morph1) {
										part = "expr"+count1;
										count1++;
										String variableEquality = prefix1+part+"="+srcvm.getExprAsText();
										saveVariableEquality(variableEqualitiesList, variableEquality);	
									}
									else if (morph == morph2) {
										part = "expr"+count2;
										count2++;
										String variableEquality = prefix2+part+"="+srcvm.getExprAsText();
										saveVariableEquality(variableEqualitiesList, variableEquality);	
									} 
								}
								if (part.equals("")) {
									part = valuem.getExprAsText();
								}
								if (!part.equals("")) {
									if (morph == morph2) {
										part = prefix2+part;
										valuem.setExprAsText(part);
									}
									else if (morph == morph1) {
										part = prefix1+part;
										valuem.setExprAsText(part);
									}
								}
								valuem.setTransient(false);
							}						
						}
					}
					list = null;
				}
			}
		}
		checked2 = null;
	}
	
	
	private static List<GraphObject> getObjsWithVariable(
			final String varName, 
			final GraphObject startObj,
			final Graph graph, 
			final Rule r) {
		
		final Vector<GraphObject> list = new Vector<GraphObject>();
		list.add(startObj);
			
		objsWithVariable(varName, startObj,
				graph.getNodesSet().iterator(),
				list);
		objsWithVariable(varName, startObj,
				graph.getArcsSet().iterator(),
				list);
		list.trimToSize();		
		return list;
	}
	
	private static List<GraphObject> objsWithVariable(
			final String varName, 
			final GraphObject startObj,
			final Iterator<?> objs,
			final List<GraphObject> result) {
						
		while (objs.hasNext()) {
			final GraphObject o = (GraphObject) objs.next();
			if (o.getAttribute() == null || o == startObj) {
				continue;
			}			
			ValueTuple value = (ValueTuple) o.getAttribute();
			ValueMember member = value.getEntryWithValueAsText(varName);
			if (member != null)  {
				result.add(o);
			}					
		}
		return result;
	}
	
	/**
	 * Searches in a graph object for a given variable name of the attributes.
	 * 
	 * @param variablenName
	 *            The name of the variable
	 * @param obj
	 *            The graph object to search in.
	 * @return A set of all attribute members which contain the variable with
	 *         the given name, otherwise null.
	 */
	protected static Vector<ValueMember> findMemberWhichUsesVariable(
			final String variablenName,
			final GraphObject obj) {
		
		Vector<ValueMember> resultVector = null;
		if (obj.getAttribute() != null) {
			for (int i = 0; i < obj.getAttribute().getNumberOfEntries(); i++) {
				final ValueMember vm = (ValueMember) obj.getAttribute().getMemberAt(i);
				final String value = vm.getExprAsText();
				if ((value != null) && value.equals(variablenName)) {
					if (resultVector == null) {
						resultVector = new Vector<ValueMember>(5);
					}
					resultVector.addElement(vm);
				}
			}
			if (resultVector != null) 
				resultVector.trimToSize();
		}
		return resultVector;
	}
	
	protected static Vector<Type> getForbiddenTypesRule2(
			final Rule rule2,
			final Hashtable<ValueMember, Pair<String, String>> attrMember2Constant) {
	
		Vector<Type> forbiddenTypesR2 = new Vector<Type>(2);
		Enumeration<OrdinaryMorphism> nacsR2 = rule2.getNACs();
		while (nacsR2.hasMoreElements()) {
			final OrdinaryMorphism nac = nacsR2.nextElement();		
			if (nac.isEnabled()) {
				final Iterator<Node> en1 = nac.getTarget().getNodesSet().iterator();
				while (en1.hasNext()) {
					final GraphObject obj = en1.next();					
					final Type t = getTypeWhenDifferentAttrValue(
												nac, obj, attrMember2Constant);
					if (t != null
							&& !forbiddenTypesR2.contains(t)) {
						forbiddenTypesR2.add(obj.getType());
					}
				}
				final Iterator<Arc> en2 = nac.getTarget().getArcsSet().iterator();
				while (en2.hasNext()) {
					final GraphObject obj = en2.next();
					final Type t = getTypeWhenDifferentAttrValue(
												nac, obj, attrMember2Constant);
					if (t != null
							&& !forbiddenTypesR2.contains(t)) {
						forbiddenTypesR2.add(obj.getType());
					}
				}
			}
		}
		forbiddenTypesR2.trimToSize();
		return forbiddenTypesR2;
	}
	
	
	/**
	 * Returns the <code>Type</code> of the <code>GraphObject imageObj</code> 
	 * if 1) the <code>Morphism morph</code> does not define any inverse image,
	 * or 2) the <code>Morphism morph</code> defines an inverse image and an attribute member of it
	 * is in the <code>Hashtable attrMember2Constant</code>.
	 * The <code>Hashtable attrMember2Constant</code> contains Pairs of Strings
	 * where the first string is the name of a variable,
	 * the second string is a constant value of the same variable.
	 * 
	 * Otherwise, returns null.
	 * 
	 * @param morph
	 * 				the OrdinaryMorphism to search
	 * @param imageObj 
	 * 				the GraphObject to check
	 * @param attrMember2Constant
	 * 				the <code>Hashtable</code>
	 * 				of Pairs of Strings an the keys are ValuMembers.
	 * @return Type 
	 * 				the Type of the imageObj
	 */
	protected static Type getTypeWhenDifferentAttrValue(
			final OrdinaryMorphism morph,
			final GraphObject imageObj,
			final Hashtable<ValueMember, Pair<String, String>> attrMember2Constant) {
		
		if (imageObj.getAttribute() == null)
			return null;
		
		if (!morph.getInverseImage(imageObj).hasMoreElements()) {
			return imageObj.getType();			
		} 
		// if urimage in LHS exists with different attr. value
		final ValueTuple vt = (ValueTuple) imageObj.getAttribute();
		for (int i = 0; i < vt.getNumberOfEntries(); i++) {
			final ValueMember vm = (ValueMember) vt.getMemberAt(i);
			if (attrMember2Constant.get(vm) != null)
				return imageObj.getType();
			else {
				if (vm.isSet()) {
					GraphObject o = morph.getInverseImage(imageObj).nextElement();
					if (o.getAttribute() != null) {
						ValueTuple vt_o = (ValueTuple) o.getAttribute();
						ValueMember vm_o = (ValueMember) vt_o.getMemberAt(vm.getName());
						if (!vm_o.isSet() || !vm.getExprAsText().equals(vm_o.getExprAsText()))
							return imageObj.getType();
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns a set of all changed attribute members for the given graph object
	 * of the left-hand side of the given rule.
	 * An attribute member is changed if the appropriate right attribute member is set
	 * to a different variable, a different constant or an expression.
	 * 
	 * @return a vector of changed members, otherwise null
	 * @param r
	 *            the rule.
	 * @param leftObj
	 *            the graph object of the left-hand side of the given rule 
	 */
	public static Vector<ValueMember> getChangedAttributeMember(
			final Rule r,
			final GraphObject leftObj) {
		
		Vector<ValueMember> resultVector = null;
		if (leftObj.getAttribute() != null) {
			final GraphObject goRight = r.getImage(leftObj);
			final AttrInstance leftAttr = leftObj.getAttribute();
			final AttrInstance rightAttr = goRight.getAttribute();
			for (int i = 0; i < leftAttr.getNumberOfEntries(); i++) {
				final ValueMember leftMember = (ValueMember) leftAttr.getMemberAt(i);
				final ValueMember rightMember = (ValueMember) rightAttr.getMemberAt(i);
				if (rightMember.isSet()) {
					if (!leftMember.isSet()) {
						if (resultVector == null)
							resultVector = new Vector<ValueMember>(5);
						resultVector.addElement(leftMember);
					}
					else {
						if (rightMember.getExpr().isVariable()) {
							if ((leftMember.getExpr().isVariable() 
										&& !leftMember.getExprAsText().equals(
												rightMember.getExprAsText()))
									|| leftMember.getExpr().isConstant()) {
								if (resultVector == null)
									resultVector = new Vector<ValueMember>(5);
								resultVector.add(leftMember);
							}
						} else if (rightMember.getExpr().isConstant()) {
							if ((leftMember.getExpr().isConstant() && !leftMember
									.getExprAsText().equals(
											rightMember.getExprAsText()))
									|| leftMember.getExpr().isVariable()) {
								if (resultVector == null)
									resultVector = new Vector<ValueMember>(5);
								resultVector.addElement(leftMember);
							}
						} else if (rightMember.getExpr().isComplex()) {
							if (resultVector == null)
								resultVector = new Vector<ValueMember>(5);
							resultVector.addElement(leftMember);
						}
					}
				}
			}
			if (resultVector != null)
				resultVector.trimToSize();
		}
		return resultVector;
	}
	
	protected static boolean doesRuleChangeAttr(
			final Rule r,
			final GraphObject leftObj) {
		
		if (leftObj.getAttribute() != null) {
			final GraphObject goRight = r.getImage(leftObj);
			final AttrInstance leftAttr = leftObj.getAttribute();
			final AttrInstance rightAttr = goRight.getAttribute();
			for (int i = 0; i < leftAttr.getNumberOfEntries(); i++) {
				final ValueMember leftMember = (ValueMember) leftAttr.getMemberAt(i);
				final ValueMember rightMember = (ValueMember) rightAttr.getMemberAt(i);
				if (rightMember.isSet()) {
					if (!leftMember.isSet())
						return true;
					else {
						if (rightMember.getExpr().isVariable()) {
							if ((leftMember.getExpr().isVariable() 
										&& !leftMember.getExprAsText().equals(
												rightMember.getExprAsText()))
									|| leftMember.getExpr().isConstant())
								return true;
						} else if (rightMember.getExpr().isConstant()) {
							if ((leftMember.getExpr().isConstant() && !leftMember
									.getExprAsText().equals(
											rightMember.getExprAsText()))
									|| leftMember.getExpr().isVariable()) {
								return true;
							}
						} else if (rightMember.getExpr().isComplex())
							return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns true if the specified GraphObject has an attribute member
	 * with constant value or a variable which is used as an input parameter.
	 * Otherwise, returns false.
	 * 
	 * @param r
	 * 			the rule
	 * @param obj
	 * 			the GraphObject
	 */
	protected static boolean isAttributeRestricted(
			final Rule r, 
			final GraphObject obj,
			final GraphObject refobj) {
		
		if (obj.getAttribute() == null)
			return false;
		
		for (int i = 0; i < obj.getAttribute().getNumberOfEntries(); i++) {
			final ValueMember vm = (ValueMember) obj.getAttribute().getMemberAt(i);
			if (vm.isSet()) {
				ValueMember refvm = (refobj != null)?
						(ValueMember) refobj.getAttribute().getMemberAt(vm.getName()):null;
				if (refvm == null || !refvm.isSet()
						|| !vm.getExprAsText().equals(refvm.getExprAsText())) 
					return true;
				/*
				if (vm.getExpr().isConstant()) 
						return true;				
				else if (vm.getExpr().isVariable()) {
					VarMember var = ((VarTuple) r.getAttrContext().getVariables())
													.getVarMemberAt(vm.getExprAsText());
					if (var != null && var.isInputParameter())
						return true;
				}
				*/
			}
		}
		return false;
	}
	
	protected static boolean isAttrRestrictedByConstant(
			final Rule r, 
			final GraphObject obj) {
		
		if (obj.getAttribute() == null)
			return false;
		
		for (int i = 0; i < obj.getAttribute().getNumberOfEntries(); i++) {
			final ValueMember vm = (ValueMember) obj.getAttribute().getMemberAt(i);
			if (vm.isSet() && vm.getExpr().isConstant())
				return true;				
		}
		return false;
	}
	
	protected static boolean isAttrRestrictedByVariable(
			final Rule r, 
			final GraphObject obj,
			boolean onlyInputParam) {
		
		if (obj.getAttribute() == null)
			return false;
		
		for (int i = 0; i < obj.getAttribute().getNumberOfEntries(); i++) {
			final ValueMember vm = (ValueMember) obj.getAttribute().getMemberAt(i);
			if (vm.isSet() &&vm.getExpr().isVariable()) {
				VarMember var = ((VarTuple) r.getAttrContext().getVariables())
							.getVarMemberAt(vm.getExprAsText());
				if (var != null) {	
					if (!onlyInputParam)
						return true;
					else if (var.isInputParameter())			
						return true;
				}
			}
		}
		return false;
	}
	
	protected static boolean hasVariableInContext(final OrdinaryMorphism morph) {
		VarTuple vars = (VarTuple) morph.getAttrContext().getVariables();
		CondTuple conds = (CondTuple) morph.getAttrContext().getConditions();
		Iterator<?> e = morph.getTarget().getNodesSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (o.getAttribute() == null)
				continue;
			ValueTuple vt = (ValueTuple) o.getAttribute();
			for (int k = 0; k < vt.getSize(); k++) {
				ValueMember vm = vt.getValueMemberAt(k);
				if (vm.getExpr() != null) {
					if (vm.getExpr().isVariable()) {
						VarMember test_vm = (VarMember) vars.getValueMemberAt(vm.getExprAsText());
						if (test_vm != null && test_vm.isInputParameter())
							return true;
						
						for (int i = 0; i < conds.getSize(); i++) {
							CondMember cm = (CondMember) conds.getValueMemberAt(i);
							if (cm.getAllVariableNamesOfExpression()
									.contains(vm.getExprAsText()))
								return true;
						}
						
					}
				}
			}
		}
		e = morph.getTarget().getArcsSet().iterator();
		while (e.hasNext()) {
			GraphObject o = (GraphObject) e.next();
			if (o.getAttribute() == null)
				continue;
			ValueTuple vt = (ValueTuple) o.getAttribute();
			for (int k = 0; k < vt.getSize(); k++) {
				ValueMember vm = vt.getValueMemberAt(k);
				if (vm.getExpr() != null) {
					if (vm.getExpr().isVariable()) {
						VarMember test_vm = (VarMember) vars.getValueMemberAt(
								vm.getExprAsText());
						if (test_vm != null && test_vm.isInputParameter())
							return true;
						
						for (int i = 0; i < conds.getSize(); i++) {
							CondMember cm = (CondMember) conds
									.getValueMemberAt(i);
							if (cm.getAllVariableNamesOfExpression()
									.contains(vm.getExprAsText()))
								return true;
						}
						
					}
				}
			}
		}
		return false;
	}
	
	
	private static void replaceNullByConstant(
			final Rule r, 
			final OrdinaryMorphism nac,
			final Hashtable<ValueMember, String> var2const) {
		
		Iterator<?> en = nac.getTarget().getNodesSet().iterator();
		while (en.hasNext()) {
			GraphObject o = (GraphObject) en.next();
			if (o.getAttribute() == null)
				continue;
			ValueTuple vt = (ValueTuple) o.getAttribute();
			for (int i = 0; i < vt.getNumberOfEntries(); i++) {
				ValueMember vm = vt.getValueMemberAt(i);
				if (var2const.containsKey(vm)) {
					vm.setExprAsText(var2const.get(vm));
				}
			}
		}
		en = nac.getTarget().getArcsSet().iterator();
		while (en.hasNext()) {
			GraphObject o = (GraphObject) en.next();
			if (o.getAttribute() == null)
				continue;
			ValueTuple vt = (ValueTuple) o.getAttribute();
			for (int i = 0; i < vt.getNumberOfEntries(); i++) {
				ValueMember vm = vt.getValueMemberAt(i);
				if (var2const.containsKey(vm)) {
					vm.setExprAsText(var2const.get(vm));
				}
			}
		}
	}
	
	private static Hashtable<ValueMember, String> replaceConstantByNull(
			final Rule r,
			final OrdinaryMorphism nac) {
		Hashtable<ValueMember, String> var2const = new Hashtable<ValueMember, String>();
		Iterator<?> en = nac.getTarget().getNodesSet().iterator();
		while (en.hasNext()) {
			GraphObject o = (GraphObject) en.next();
			if (o.getAttribute() == null)
				continue;
			if (!nac.getInverseImage(o).hasMoreElements()) {
				ValueTuple vt = (ValueTuple) o.getAttribute();
				for (int i = 0; i < vt.getNumberOfEntries(); i++) {
					ValueMember vm = vt.getValueMemberAt(i);
					if (vm.isSet() && vm.getExpr().isConstant()) {
						var2const.put(vm, vm.getExprAsText());
						vm.setExpr(null);
					}
				}
			}
		}
		en = nac.getTarget().getArcsSet().iterator();
		while (en.hasNext()) {
			GraphObject o = (GraphObject) en.next();
			if (o.getAttribute() == null)
				continue;
			if (!nac.getInverseImage(o).hasMoreElements()) {
				ValueTuple vt = (ValueTuple) o.getAttribute();
				for (int i = 0; i < vt.getNumberOfEntries(); i++) {
					ValueMember vm = vt.getValueMemberAt(i);
					if (vm.isSet() && vm.getExpr().isConstant()) {
						var2const.put(vm, vm.getExprAsText());
						vm.setExpr(null);
					}
				}
			}
		}
		return var2const;
	}
	
	private static boolean checkNACStarAttributes(
			final Rule r1, 
			final Rule r2,
			final OrdinaryMorphism nac, 
			final OrdinaryMorphism nacStar,
			final Match match2, 
			final Iterator<?> l1Objs,
			final Pair<OrdinaryMorphism, OrdinaryMorphism> overlapping,
			final List<GraphObject> changedAttributesR1,
			final List<Type> forbiddenObjTypesR2,
			final Hashtable<ValueMember, Pair<String, String>> attrMember2Constant) {
		
		boolean result = false;
		boolean nacStarFailed = false;

		while (l1Objs.hasNext()) {
			GraphObject l1Obj = (GraphObject) l1Objs.next();
			GraphObject overlapObj = overlapping.first.getImage(l1Obj);
			if (changedAttributesR1.contains(l1Obj)) {
				if (forbiddenObjTypesR2.contains(overlapObj.getType())) {
					
					Enumeration<GraphObject> e = nacStar.getInverseImage(overlapObj);
					if (e.hasMoreElements()) {
						
						GraphObject nacObj = e.nextElement();
						ValueTuple vtNac = (ValueTuple) nacObj.getAttribute();
						if (!nac.getInverseImage(nacObj).hasMoreElements()) {
							Vector<ValueMember> 
							changedMembers = getChangedAttributeMember(r1, l1Obj);	
							if (changedMembers != null) {
								for (int j=0; j<vtNac.getNumberOfEntries(); j++) {
									ValueMember vmNacObj = vtNac.getValueMemberAt(j);
									ValueMember vmL1 = ((ValueTuple)l1Obj.getAttribute()).getValueMemberAt(vmNacObj.getName());
									if (vmL1 != null) {
										if (!changedMembers.contains(vmL1)) {
											if (vmNacObj.isSet() && vmNacObj.getExpr().isConstant()
													&& vmL1.isSet() && vmL1.getExpr().isConstant()
													&& !vmNacObj.getExprAsText().equals(vmL1.getExprAsText())) {
												nacStar.setEnabled(false);
												return false;
											}
										} else if (vmNacObj.isSet()) {									
											ValueMember vmr1Obj = ((ValueTuple) r1
													.getImage(l1Obj).getAttribute())
													.getValueMemberAt(vmL1.getName());
											if (vmr1Obj != null && vmr1Obj.isSet()) {										
												if (vmNacObj.getExpr().isVariable()) {											
													if (attrMember2Constant.get(vmNacObj) != null) {								
														Pair<String,String> p = attrMember2Constant.get(vmNacObj);											
														if (vmNacObj.getExprAsText().equals(p.first)
																&& vmr1Obj.getExpr().isConstant()
																&& p.second.equals(vmr1Obj.getExprAsText())) {
															result = true;
															overlapObj.setCritical(true);
														}
													} else {
														result = true;
														overlapObj.setCritical(true);
													}
												} else if (vmNacObj.getExpr().isConstant()) {
													if (!vmNacObj.getExprAsText().equals(
															vmL1.getExprAsText())) {
														if (vmr1Obj.getExpr().isConstant()
																&& vmNacObj
																		.getExprAsText()
																		.equals(
																				vmr1Obj
																						.getExprAsText())) {
															result = true;
															overlapObj.setCritical(true);
														} else {
															nacStarFailed = true;
														}
													} else
														nacStarFailed = true;
												}
											}
										}
									}
								}
								changedMembers = null;
							}
						} else {
							// nac obj of rule2 has an urimage with different attr. value
							ValueTuple nacObjVT = (ValueTuple) nacObj.getAttribute();
							 for (int i=0; i<nacObjVT.getNumberOfEntries(); i++) {
								 ValueMember nacObjVM = nacObjVT.getValueMemberAt(i);
								 if (attrMember2Constant.get(nacObjVM) != null) {								 
									 ValueTuple overlapObjVT = (ValueTuple) overlapObj.getAttribute();
									 ValueMember overlapObjVM = overlapObjVT.getValueMemberAt(nacObjVM.getName());
									 if (overlapObjVM != null) {
										 ValueMember rhs1ObjVM = ((ValueTuple) r1
													.getImage(l1Obj).getAttribute())
													.getValueMemberAt(nacObjVM.getName());	
//										 System.out.println(rhs1ObjVM+"   "+rhs1ObjVM.getExpr());
										 if (rhs1ObjVM != null && rhs1ObjVM.isSet()
												 && (rhs1ObjVM.getExprAsText().equals(
														 attrMember2Constant.get(nacObjVM).second)
														 || rhs1ObjVM.getExpr().isVariable())) {
											 result = true;
											 overlapObj.setCritical(true);
										 }
									 }
								 }
							 }
							
						}
					}
				} else {
					if (overlapping.second.getInverseImage(overlapObj).hasMoreElements()) {
						GraphObject l2Obj = overlapping.second.getInverseImage(overlapObj).nextElement();
						ValueTuple vtL1Obj = (ValueTuple) l1Obj.getAttribute();
						for (int i = 0; i < vtL1Obj.getNumberOfEntries(); i++) {
							ValueMember vmL1 = vtL1Obj.getValueMemberAt(i);
							ValueMember vmL2 = ((ValueTuple) l2Obj
									.getAttribute()).getValueMemberAt(i);
							if (vmL2 != null && vmL1.isSet() && vmL1.getExpr().isVariable()) {
								if (vmL2.isSet() && !vmL2.isTransient()) {
									result = true;
									overlapObj.setCritical(true);
								}
							}
						}
					}
				}
			} else {
				if (!overlapping.second.getInverseImage(overlapObj).hasMoreElements()) {
					if (overlapObj.getAttribute() == null)
						continue;
					ValueTuple vt = (ValueTuple) overlapObj.getAttribute();
					for (int i = 0; i < vt.getNumberOfEntries(); i++) {
						ValueMember m = vt.getValueMemberAt(i);
						if (m.isSet() && m.getExpr().isConstant()) {
							if (nac.getInverseImage(l1Obj).hasMoreElements()) {
								GraphObject nacObj = nac
										.getInverseImage(l1Obj).nextElement();
								if (nacObj.getAttribute() == null)
									break;
								ValueMember mnacObj = vt.getValueMemberAt(m.getName());
								if (mnacObj != null && mnacObj.isSet()) {
									if (mnacObj.getExpr().isConstant()
											&& mnacObj.getExprAsText().equals(
													m.getExprAsText()))
										;// return false;
									else if (mnacObj.getExpr().isVariable()) {
										VarMember var = (VarMember) match2
												.getAttrContext()
												.getVariables()
												.getMemberAt(
														mnacObj.getExprAsText());
										if (!var.isInputParameter())
											;// return false;
										else
											result = true;
									}
								}
							}
						}
					}
				} else {
//					 System.out.println(overlapObj.getType().getName()+"   is in second overlap morphism");
					 GraphObject nacObj = nac.getImage(l1Obj);
					 if (nacObj != null) {
						 if (forbiddenObjTypesR2.contains(overlapObj.getType())) {
							 ValueTuple nacObjVT = (ValueTuple) nacObj.getAttribute();
							 for (int i=0; i<nacObjVT.getNumberOfEntries(); i++) {
								 ValueMember nacObjVM = nacObjVT.getValueMemberAt(i);
								 if (attrMember2Constant.get(nacObjVM) != null) {								 
									 ValueTuple overlapObjVT = (ValueTuple) overlapObj.getAttribute();
									 ValueMember overlapObjVM = overlapObjVT.getValueMemberAt(nacObjVM.getName());
									 if (overlapObjVM != null) {
										 ValueMember rhs1ObjVM = ((ValueTuple) r1
													.getImage(l1Obj).getAttribute())
													.getValueMemberAt(nacObjVM.getName());										 
										 if (rhs1ObjVM != null 
												 && rhs1ObjVM.getExpr() != null
												 && rhs1ObjVM.getExprAsText().equals(
														 attrMember2Constant.get(nacObjVM).second)) {
											 result = true;
											 overlapObj.setCritical(true);
										 }										 
									 }
								 }
							 }
						 }
					 }
				}
			}
		}
		if (!result && !nacStarFailed)
			return false;
		
		return true;
	}
	
	private static void doReplaceConstantByInputParam(
			final Rule r, OrdinaryMorphism nac,
			final Hashtable<ValueMember, String> var2const) {

		VarTuple vars = (VarTuple) r.getAttrContext().getVariables();

		doReplaceConstantByInputParam(vars, nac.getTarget().getNodesSet().iterator(), nac,
				var2const);
		doReplaceConstantByInputParam(vars, nac.getTarget().getArcsSet().iterator(), nac,
				var2const);
	}

	protected static Hashtable<ValueMember, String> replaceConstantByInputParam(
			final Rule r, 
			final Hashtable<ValueMember, String> var2const) {
		final List<OrdinaryMorphism> nacs = r.getNACsList();
		for (int l=0; l<nacs.size(); l++) {
			final OrdinaryMorphism nac = nacs.get(l);		
			doReplaceConstantByInputParam(r, nac, var2const);
		}
		return var2const;
	}

	protected static void doReplaceConstantByInputParam(
			final VarTuple vars,
			final Iterator<?> en, final OrdinaryMorphism nac,
			final Hashtable<ValueMember, String> var2const) {
		
		while (en.hasNext()) {
			GraphObject o = (GraphObject) en.next();
			if (o.getAttribute() == null)
				continue;
			if (!nac.getInverseImage(o).hasMoreElements()) {
				ValueTuple vt = (ValueTuple) o.getAttribute();
				for (int i = 0; i < vt.getNumberOfEntries(); i++) {
					ValueMember vm = vt.getValueMemberAt(i);
					if (vm.isSet() && vm.getExpr().isConstant()) {
						var2const.put(vm, vm.getExprAsText());
						String varname = vm.getName() + i;
						vm.setExprAsText(varname);
						vm.setTransient(true);
						VarMember var = vars.getVarMemberAt(varname);
						if (var != null) {
							var.setInputParameter(true);
						}
					}
				}
			}
		}
	}

	protected static void replaceInputParamByConstant(
			final Rule r,
			final Hashtable<ValueMember, String> var2const) {
		
		final List<OrdinaryMorphism> nacs = r.getNACsList();
		for (int l=0; l<nacs.size(); l++) {
			final OrdinaryMorphism nac = nacs.get(l);		
			doReplaceInputParamByConstant(r, nac, var2const);
		}
	}

	private static void doReplaceInputParamByConstant(
			final Rule r, 
			final OrdinaryMorphism nac,
			final Hashtable<ValueMember, String> var2const) {
		
		VarTuple vars = (VarTuple) r.getAttrContext().getVariables();

		doReplaceInputParamByConstant(vars, nac.getTarget().getNodesSet().iterator(),
				var2const);
		doReplaceInputParamByConstant(vars, nac.getTarget().getArcsSet().iterator(),
				var2const);
	}

	private static void doReplaceInputParamByConstant(
			final VarTuple vars,
			final Iterator<?> en, 
			final Hashtable<ValueMember, String> var2const) {
		while (en.hasNext()) {
			GraphObject o = (GraphObject) en.next();
			if (o.getAttribute() == null)
				continue;
			ValueTuple vt = (ValueTuple) o.getAttribute();
			for (int i = 0; i < vt.getNumberOfEntries(); i++) {
				ValueMember vm = vt.getValueMemberAt(i);
				if (var2const.containsKey(vm)) {
					String varname = vm.getExprAsText();
					vm.setExprAsText(var2const.get(vm));
					vm.setTransient(false);
					if (vars.getVarMemberAt(varname) != null) {
						vars.getTupleType().deleteMemberAt(varname);
					}
				}
			}
		}
	}

	protected static Vector<Vector<GraphObject>> combineGraphLikeInclusionsOf(
			int maxSize,
			final Vector<GraphObject> nodeSet,
			final Vector<GraphObject> arcs) {
		
		Vector<Vector<GraphObject>> result = new Vector<Vector<GraphObject>>();	
		Vector<GraphObject> combi = null;
		result.add(nodeSet);
		if (nodeSet.size() < maxSize) {
						
			for (int j=0; j<arcs.size(); j++) {
				GraphObject o = arcs.get(j);
				if (nodeSet.contains(((Arc) o).getSource())
						&& nodeSet.contains(((Arc) o).getTarget())) {
					// new combi with nodes
					combi = new Vector<GraphObject>(nodeSet);						
					// add edge
					combi.add(o);
					// store new combi
					result.add(combi);
						
					if (j < arcs.size()-1) {
						// create new combi and fill from the previous once for the next
						combi = new Vector<GraphObject>(result.get(result.size()-1));
					}
				} else
					continue;
				
				if (combi != null && !combi.isEmpty()) {
					for (int k=j+1; k<arcs.size(); k++) {
						GraphObject o1 = arcs.get(k);
						
						if (nodeSet.contains(((Arc) o1).getSource())
								&& nodeSet.contains(((Arc) o1).getTarget())) {
							if (combi.size() < maxSize) {
								// add more edge
								combi.add(o1);
								// store combi
								result.add(combi);
								if (k<arcs.size()-1) {
									// create combi and fill from the previous for the next						
									combi = new Vector<GraphObject>(result.get(result.size()-1));
								}
							} else
								break;
						}
					}
				}
			}
		}
		return result;
	}
		
	protected static Vector<Vector<GraphObject>> combineGraphLikeInclusions(
			int maxSize,
			final Vector<Vector<GraphObject>> nodeSets,
			final Vector<Vector<GraphObject>> arcSets) {
		
		Vector<Vector<GraphObject>> result = new Vector<Vector<GraphObject>>();
		Vector<GraphObject> combi = null;
		for (int i=0; i<nodeSets.size(); i++) {
			Vector<GraphObject> nodeSet = nodeSets.get(i);
			if (nodeSet.size() <= maxSize)
				result.add(nodeSet);
			if (nodeSet.size() < maxSize) {
				// extend node inclusions by arc and add to result
				for (int j=0; j<arcSets.size(); j++) {
					Vector<GraphObject> arcSet = arcSets.get(j);
					boolean ok = true;
					for (int k=0; k<arcSet.size(); k++) {
						GraphObject o = arcSet.get(k);					
						if (!nodeSet.contains(((Arc) o).getSource())
								|| !nodeSet.contains(((Arc) o).getTarget())) {
							ok = false;
							break;
						}
					}
					if (ok && (nodeSet.size()+arcSet.size()) <= maxSize) {
						combi = new Vector<GraphObject>(nodeSet);						
						combi.addAll(arcSet);
						combi.trimToSize();
						result.add(combi);						
					}
				}		
			}
		}
		return result;
	}
	



	
	protected static Vector<Vector<GraphObject>> combinePlainInclusions(
			int maxSize,
			final Vector<Vector<GraphObject>> nodeSets,
			final Vector<Vector<GraphObject>> arcSets) {
		
		Vector<Vector<GraphObject>> result = new Vector<Vector<GraphObject>>();
		Vector<GraphObject> combi = null;
		
		for (int i=0; i<nodeSets.size(); i++) {
			Vector<GraphObject> nodeSet = nodeSets.get(i);
			// add node inclusions
			if (nodeSet.size() <= maxSize)
				result.add(nodeSet);
			// extend node inclusions by arc and add to result
			if (nodeSet.size() < maxSize) {				
				for (int j=0; j<arcSets.size(); j++) {
					Vector<GraphObject> arcSet = arcSets.get(j);					
					// new combi with nodes
					if (arcSet.size() <= maxSize)
						result.add(arcSet);
					if ((nodeSet.size()+ arcSet.size()) <= maxSize) {
						combi = new Vector<GraphObject>(nodeSet);
						combi.addAll(arcSet);
						combi.trimToSize();
						result.add(combi);	
					}
				}
			}
		}
		result.trimToSize();
		return result;
	}
			
	protected static Vector<Vector<GraphObject>> combineInclusionsOf(
			int maxSize,
			final Vector<Vector<GraphObject>> set1, 
			final Vector<GraphObject> set2,
			final Vector<?> set3) {
		
		// combine set1 with set2 above elements from set3;
		// result contains graph like inclusions only
		
		Vector<Vector<GraphObject>> result = new Vector<Vector<GraphObject>>();
		
		if (set1 == null || set1.isEmpty()
				|| set2 == null || set2.isEmpty()) {
			return result;
		}
		for (int i = 0; i < set1.size(); i++) {
			Vector<GraphObject> v1i = set1.get(i);
			
			/*
			 * boundary is not used now Vector bound1 = new Vector(2); 
			 * for(int ii = 0; ii < v1i.size() && !stop; ii++){
			 * GraphObject o = (GraphObject)v1i.get(ii); 
			 * if(set3.contains(o)) bound1.add(o); }
			 */
			
			/*
			 * boundary is not used now boolean bound2 = true; for(int jj =
			 * 0; jj < v2j.size() && !stop; jj++){ GraphObject o =
			 * (GraphObject)v2j.get(jj); if(set3.contains(o)){
			 * if(!bound1.contains(o)) bound2 = false; } }
			 * if(!bound2)
			 * continue;
			 */
			Vector<GraphObject> combi = new Vector<GraphObject>();
				
			combi.addAll(v1i);

			// handle nodes first
			for (int jj = 0; jj < set2.size(); jj++) {
				GraphObject o = set2.get(jj);
				if (o.isNode()) {
					if (!combi.contains(o)) {				
						combi.add(o);
					}
				}
			}
			// handle edges now, to guarantee graph like structure
			boolean arcOK = true;
			for (int jj = 0; jj < set2.size() && arcOK; jj++) {
				GraphObject o = set2.get(jj);
				if (o.isArc()) {
					if (!combi.contains(o)) {					
						if (combi.contains(((Arc) o).getSource())
								&& combi.contains(((Arc) o).getTarget())) {
							combi.add(o);						
						} else {
							arcOK = false;
						}
					}
				}
			}
			if (!arcOK) {
				continue;
			}
			result.add(combi);						
		}
		
		return result;
	}
	
	protected static Vector<Vector<GraphObject>> combineFirstWithSecondAboveThird(
			int maxSize,
			final Vector<GraphObject> set1, 
			final Vector<Vector<GraphObject>> set2,
			final Vector<?> set3) {
		
		// combine set1 with set2 above elements from set3;
		// result contains graph like inclusions only!!!
		
		Vector<Vector<GraphObject>> result = new Vector<Vector<GraphObject>>();		
		if ((set1 != null) && !set1.isEmpty()) {
			result.add(set1);
		}
		if (set1.size() < maxSize && set2 != null && !set2.isEmpty()) {	
			/*
			 * boundary is not used now Vector bound1 = new Vector(2); 
			 * for(int ii = 0; ii < v1i.size() && !stop; ii++){
			 * GraphObject o = (GraphObject)v1i.get(ii); 
			 * if(set3.contains(o)) bound1.add(o); }
			 */
//			Vector<GraphObject> combi = new Vector<GraphObject>();
			for (int j = 0; j < set2.size(); j++) {
				Vector<GraphObject> v2j = set2.get(j);
				/*
				 * boundary is not used now boolean bound2 = true; for(int jj =
				 * 0; jj < v2j.size() && !stop; jj++){ GraphObject o =
				 * (GraphObject)v2j.get(jj); if(set3.contains(o)){
				 * if(!bound1.contains(o)) bound2 = false; } }
				 * if(!bound2)
				 * continue;
				 */
//				if (!combi.isEmpty())
				Vector<GraphObject> combi = new Vector<GraphObject>(set1);				
				// handle edges now, to guarantee graph like structure
				for (int jj = 0; jj < v2j.size(); jj++) {
					GraphObject o = v2j.get(jj);
					if (o.isArc()) {
						if (//combi.size() < maxSize && 
								!combi.contains(o)) {					
							if (!combi.contains(((Arc) o).getSource()))
								combi.add(((Arc) o).getSource());
							if (!combi.contains(((Arc) o).getTarget())) 
								combi.add(((Arc) o).getTarget());
							combi.add(o);							
						}
					}
				}
				// handle nodes
				for (int jj = 0; jj < v2j.size(); jj++) {
					GraphObject o = v2j.get(jj);
					if (o.isNode()) {
						if (//combi.size() < maxSize && 
								!combi.contains(o)) {				
							combi.add(o);
						}
					}
				}
				addIfNotContained(result, combi);
//				result.add(combi);	
			}
		}
		return result;
	}
	
	private static void addIfNotContained(Vector<Vector<GraphObject>> list, Vector<GraphObject> vec) {
		boolean found = false;		
		for (int i=0; i<list.size(); i++) {
			Vector<GraphObject> l = list.get(i);
			if (l.size() == vec.size() && l.containsAll(vec)) {
				found = true;
				break;
			}
		}
		if (!found)
			list.add(vec);	
	}
	
	protected static Vector<Vector<GraphObject>> combineFirstWithSecondAboveThirdOLD(
			int maxSize,
			final Vector<GraphObject> set1, 
			final Vector<Vector<GraphObject>> set2,
			final Vector<?> set3) {
		
		// combine set1 with set2 above elements from set3;
		// result contains graph like inclusions only!!!
		
		Vector<Vector<GraphObject>> result = new Vector<Vector<GraphObject>>();		
		if ((set1 != null) && !set1.isEmpty()) {
			result.add(set1);
		}
		if (set2 != null && !set2.isEmpty()) {	
			/*
			 * boundary is not used now Vector bound1 = new Vector(2); 
			 * for(int ii = 0; ii < v1i.size() && !stop; ii++){
			 * GraphObject o = (GraphObject)v1i.get(ii); 
			 * if(set3.contains(o)) bound1.add(o); }
			 */
			Vector<GraphObject> combi = new Vector<GraphObject>();
			for (int j = 0; j < set2.size(); j++) {
				Vector<GraphObject> v2j = set2.get(j);
				/*
				 * boundary is not used now boolean bound2 = true; for(int jj =
				 * 0; jj < v2j.size() && !stop; jj++){ GraphObject o =
				 * (GraphObject)v2j.get(jj); if(set3.contains(o)){
				 * if(!bound1.contains(o)) bound2 = false; } }
				 * if(!bound2)
				 * continue;
				 */
				if (!combi.isEmpty())
					combi = new Vector<GraphObject>();				
				combi.addAll(set1);	
				// handle nodes first
				for (int jj = 0; jj < v2j.size(); jj++) {
					GraphObject o = v2j.get(jj);
					if (o.isNode()) {
						if (!combi.contains(o)) {				
							combi.add(o);
						}
					}
				}
				// handle edges now, to guarantee graph like structure
				boolean arcOK = true;
				for (int jj = 0; jj < v2j.size()/* && arcOK*/; jj++) {
					GraphObject o = v2j.get(jj);
					if (o.isArc()) {
						if (!combi.contains(o)) {					
							if (combi.contains(((Arc) o).getSource())
									&& combi.contains(((Arc) o).getTarget())) {
								combi.add(o);						
							} 
						}
					}
				}
				if (!arcOK) {
					combi.clear();
					continue;
				}
				result.add(combi);				
			}
		}
		return result;
	}
	
	protected static Vector<Vector<GraphObject>> combineInclusions(
			int maxSize,
			final Vector<Vector<GraphObject>> set1, 
			final Vector<Vector<GraphObject>> set2,
			final Vector<?> set3) {
		
		// combine set1 with set2 above elements from set3;
		// result contains graph like inclusions only!!!
		
		Vector<Vector<GraphObject>> result = new Vector<Vector<GraphObject>>();
		
		if ((set1 != null) && !set1.isEmpty()) {
			if ((set2 == null) || set2.isEmpty()) {
				result.addAll(set1);
			} else {
				for (int i = 0; i < set1.size(); i++) {
					Vector<GraphObject> v1i = set1.get(i);
					Vector<Vector<GraphObject>> 
					res_i = combineFirstWithSecondAboveThird(maxSize, v1i, set2, set3);
					result.addAll(res_i);
				}
			}
		}
		return result;
	}
	
	protected static Vector<Vector<GraphObject>> getInclusions(
			final Graph g, 
			int size,
			final Vector<GraphObject> set, 
			boolean graphLikeIncl) {

		final Vector<Vector<GraphObject>> combis = new Vector<Vector<GraphObject>>();
		
		// split set to node subset and edge subset
		final Vector<GraphObject> nodeSubset = new Vector<GraphObject>(5);
		final Vector<GraphObject> arcSubset = new Vector<GraphObject>(5);
		
		ExcludePairHelper.split(set, nodeSubset, arcSubset);
		Vector<Vector<GraphObject>> arcIncls = ExcludePairHelper.getArcInclusions(size, arcSubset);
		
		int tmpSize = nodeSubset.size();
		if (tmpSize > size)
			tmpSize = size;
		// get inclusions of nodes		
		for (int i = 1; i <= tmpSize; i++) {
			final Vector<Vector<GraphObject>> 
			incls = new Vector<Vector<GraphObject>>(5);			
			ExcludePairHelper.generateAllSubsetsWithInclusionsOfSize(g, i, nodeSubset, incls, false);			
			if (!incls.isEmpty()) {
				if (graphLikeIncl) {
					combis.addAll(ExcludePairHelper.combineGraphLikeInclusions(size, incls, arcIncls));
				}
				else {
					combis.addAll(ExcludePairHelper.combinePlainInclusions(size, incls, arcIncls));
				}
			}
		}
		combis.trimToSize();	
		return combis;
	}
	
	protected static Vector<Vector<GraphObject>> getNodeInclusions(
			final Graph g, 
			int size,
			final Vector<GraphObject> nodes) {
		
		int tmpSize = nodes.size();
		if (tmpSize > size)
			tmpSize = size;
		// get inclusions of nodes
		final Vector<Vector<GraphObject>> 
		nodeSubsets = new Vector<Vector<GraphObject>>();
		for (int i = 1; i <= tmpSize; i++) {
			Vector<Vector<GraphObject>> 
			incls = new Vector<Vector<GraphObject>>();
			generateAllSubsetsWithInclusionsOfSize(g, i, nodes, incls, false);
			if (!incls.isEmpty()) {
				nodeSubsets.addAll(incls);
			}
		}
		nodeSubsets.trimToSize();
		return nodeSubsets;
	}
	
	protected static Vector<Vector<GraphObject>> getArcInclusions(
			int size, final Vector<GraphObject> arcs) {
		
		final Vector<Vector<GraphObject>> 
		arcSubsets = new Vector<Vector<GraphObject>>();
		int tmpSize = arcs.size();
		if (tmpSize > size)
			tmpSize = size;
		for (int i = 1; i <= tmpSize; i++) {
			Vector<Vector<GraphObject>> incls = new Vector<Vector<GraphObject>>(5);
			ExcludePairHelper.generateAllSubsetsWithInclusionsOfSize(i, arcs, incls, false);
			if (!incls.isEmpty()) { 				
				arcSubsets.addAll(incls);
			}
		}
		arcSubsets.trimToSize();
		return arcSubsets;
	}
	
	@SuppressWarnings("unused")
	private static void delEqualIncls(Vector<Vector<GraphObject>> incls) {
		Vector<Integer> del = new Vector<Integer>();
		for (int i = 0; i<incls.size(); i++) {
			if (!del.contains(Integer.valueOf(i))) {
				Vector<GraphObject> incl1 = incls.get(i);
				for (int j = 0; j<incls.size(); j++) {
					if (j!=i && !del.contains(Integer.valueOf(j))) {
						Vector<GraphObject> incl2 = incls.get(j);
						if (incl1.size() == incl2.size()) {
							boolean ok = true;
							for (int k = 0; k<incl1.size(); k++) {
								if (!incl2.contains(incl1.get(k))) {
									ok = false;
									break;
								}
							}
							if (ok)
								del.add(Integer.valueOf(j));
						}
					}
				}
			}
		}
		for (int i = 0; i<del.size(); i++) {
			incls.remove(del.get(i).intValue());
		}
	}
	
	protected static Vector<Vector<GraphObject>> getPlainCombinedInclusions(
			final Vector<GraphObject> setToCombine,
			int size,
			final Graph g) {
		
		final Vector<GraphObject> nodes = new Vector<GraphObject>();
		final Vector<GraphObject> arcs = new Vector<GraphObject>();
		split(setToCombine, nodes, arcs);
		final Vector<Vector<GraphObject>> 
		nodeSets = getNodeInclusions(g, size, nodes);
		Vector<Vector<GraphObject>> arcIncls = ExcludePairHelper.getArcInclusions(size, arcs);
		if (nodeSets.size() > 0) {
			return ExcludePairHelper.combinePlainInclusions(size, nodeSets, arcIncls);
		}
		else {
			return arcIncls;
		}
	}
	
	protected static void split(final Vector<GraphObject> set,
			final Vector<GraphObject> outNodeSubset,
			final Vector<GraphObject> outArcSubset) {
		 
		if (!set.isEmpty()) {
			for (int i = 0; i < set.size(); i++) {
				GraphObject go = set.get(i);
				if (go.isNode())
					outNodeSubset.add(go);
				else
					outArcSubset.add(go);
			}
			outNodeSubset.trimToSize();
			outArcSubset.trimToSize();
		}
	}
	
	protected static Vector<Vector<GraphObject>> generateAllSubsetsWithInclusionsOfSize(
			final Graph g, 
			int i,
			final Vector<GraphObject> itsGOSet, 
			Vector<Vector<GraphObject>> inclusions,
			boolean graphLike) {
				
		if (i > 0 && i <= itsGOSet.size()) {
			
			final Vector<Integer> select = new Vector<Integer>(i);
			for (int j = 0; j<i; j++) {
				select.addElement(new Integer(j));
			}
			computeSelection(//g, 
					1, itsGOSet, select, inclusions, graphLike);
			inclusions.trimToSize();
		}

		return inclusions;
	}

	protected static Vector<Vector<GraphObject>> generateAllSubsetsWithInclusionsOfSize(
			int i,
			final Vector<GraphObject> itsGOSet, 
			Vector<Vector<GraphObject>> inclusions,
			boolean graphLike) {
				
		if (i > 0 && i <= itsGOSet.size()) {			
			final Vector<Integer> select = new Vector<Integer>(i);
			for (int j = 0; j<i; j++) {
				select.addElement(new Integer(j));
			}
			computeSelection(1, itsGOSet, select, inclusions, graphLike);
			inclusions.trimToSize();
		}

		return inclusions;
	}
	
	private static Vector<Vector<GraphObject>> computeSelection(
//			final Graph g, 
			int s,
			final Vector<GraphObject> itsGOSet, 
			final Vector<Integer> select,
			Vector<Vector<GraphObject>> inclusions,
			boolean graphLike) {

		int max = itsGOSet.size();
		int selSize = select.size();
		int v;
		Vector<GraphObject> goSet;
		if (s <= selSize && s >= 1) {
			try {
				v = select.elementAt(s - 1).intValue();
				while ((v < max - selSize + s)) {
					inclusions = computeSelection(//g, 
							s + 1, itsGOSet, select, inclusions, graphLike);
					if (s == selSize) {
						goSet = makeGraphObjectSet(select, itsGOSet);
						inclusions = putGraphInclusionSet(goSet, inclusions, graphLike);
					}
					select.setElementAt(new Integer(v + 1), s - 1);
					v = select.elementAt(s - 1).intValue();
				}
				if (s > 1) {
					v = select.elementAt(s - 2).intValue();
					if (v < max - selSize + s + 1) {
						select.setElementAt(new Integer(v + 1), s - 2);
						for (int j = 1; j <= selSize - s + 1; j++) {
							select.setElementAt(new Integer(v + 1 + j), s + j
									- 2);
						}
					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {
//				System.out.println(e.getStackTrace());
			}
		}
		return (inclusions);
	}

	private static Vector<GraphObject> makeGraphObjectSet(
			final Vector<Integer> select,
			final Vector<GraphObject> itsGOSet) {
		
		Vector<GraphObject> tmp = new Vector<GraphObject>();
		for (int i = 0; i < select.size(); i++) {
			int v = select.elementAt(i).intValue();
			tmp.addElement(itsGOSet.elementAt(v));
		}
		return (tmp);
	}

	private static Vector<Vector<GraphObject>> putGraphInclusionSet(
			final Vector<GraphObject> goSet,
			Vector<Vector<GraphObject>> inclusions,
			boolean graphLike) {

//		Vector<GraphObject> incl = new Vector<GraphObject>(5);

		if (graphLike) {
//			for (int i = 0; i < goSet.size(); i++) {
//				GraphObject go = goSet.elementAt(i);
//				if (go.isNode())
//					incl.add(go);
//			}
			for (int i = 0; i < goSet.size(); i++) {
				GraphObject go = goSet.elementAt(i);
				if (go.isArc()) {
//					if (incl.contains(((Arc) go).getSource()) 
//							&& incl.contains(((Arc) go).getTarget()))
//						incl.add(go);
					
					if (!goSet.contains(((Arc) go).getSource()) 
							|| !goSet.contains(((Arc) go).getTarget())) {
						goSet.remove(i);
						i--;
					}
				}
			}
		}
//		else {
//			for (int i = 0; i < goSet.size(); i++) {
//				GraphObject go = goSet.elementAt(i);
//				incl.add(go);
//			}
//		}
//		incl.trimToSize();
//		inclusions.addElement(incl);
		
		goSet.trimToSize();
		inclusions.addElement(goSet);
		
		return (inclusions);
	}

	/**
	 *  Try to shift the specified application condition <code>cond</code> 
	 *  of the given Rule <code>rule</code> over the morphism <code>morph</code>.
	 *  
	 * @param rule	
	 * 			the rule
	 * @param cond	
	 * 			the application condition at the graph <code>morph.getSource()</code>
	 * @param morph	
	 * 			the morphism for shift over 
	 * @return	list of application conditions at the graph <code>morph.getTarget()</code> 
	 */
	public static List<Pair<OrdinaryMorphism, OrdinaryMorphism>> shiftCondOverMorphism(
			final Rule rule,
			final OrdinaryMorphism cond,
			final OrdinaryMorphism morph) {	
				
		final Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		list = new Vector<Pair<OrdinaryMorphism, OrdinaryMorphism>>();	
				
		// make an iso-copy of the source graph of the condition
		final OrdinaryMorphism condSrcIsom = cond.getSource().isomorphicCopy();
		if (condSrcIsom == null) {
			list.trimToSize();
			return list;
		}
		
		// extend the target graph of condSrcIsom by elements of the target graph of cond
//		final OrdinaryMorphism 
//		condExt = BaseFactory.theFactory().extendTargetGraph1ByTargetGraph2(condSrcIsom, cond);
		// get the extended result graph
		final Graph dCondGraph = condSrcIsom.getTarget();
			
		final Vector<GraphObject> condDom = condSrcIsom.getDomainObjects();
		final List<Object> requiredObjs = new Vector<Object>(condDom.size());
		final Hashtable<Object,Object> objmap = new Hashtable<Object,Object>(condDom.size());
		// fill a map with objects required 
		// for the graph overlappings of dCondGraph and morph.getTarget()
		for (int j=0; j<condDom.size(); j++) {
			GraphObject go = condDom.get(j);
			GraphObject go1 = condSrcIsom.getImage(go);	
			GraphObject go2 = morph.getImage(go);
			if (go1 != null && go2 != null) {
				requiredObjs.add(go1);
				objmap.put(go1, go2);
			}
		}
		// make graph overlappings above required objects				
		final Enumeration<Pair<OrdinaryMorphism, OrdinaryMorphism>> 
		overlaps = BaseFactory.theFactory().getOverlappingByPartialPredefinedIntersection(
										dCondGraph, 
										morph.getTarget(), 
										requiredObjs, 
										objmap,
										true);
		// add created conditions to the list					
		while (overlaps.hasMoreElements()) {
			Pair<OrdinaryMorphism, OrdinaryMorphism> p = overlaps.nextElement();
			// get an application condition after shifting
			OrdinaryMorphism condSh = p.second;
			condSh.setEnabled(cond.isEnabled());
			condSh.setName(cond.getName().concat("_").concat(String.valueOf(list.size())));
			
			list.add(p);			
		}
		
		return list;
	}
	
	/**
	 * Tries to construct a left application condition : r.LHS --> criticalGraph.
	 * The specified Pair p is one of the critical overlapping pairs of the corresponding rule pair.
	 * The specified rule is one of the rules of the corresponding rule pair.
	 * The criticalGraph is the target graph of the specified critical pair.
	 * Use <tt>Rule.addNAC(OrdinaryMorphism)</tt> to be able to use the result as a Negative AC, 
	 * or <tt>Rule.addPAC(OrdinaryMorphism)</tt> to be able to use the result as a Positive AC,
	 * or <tt>Rule.addNestedAC(OrdinaryMorphism)</tt> to be able to use the result as a General AC,
	 */
	public static OrdinaryMorphism makeLeftACFromGraph(
			final Pair<Pair<OrdinaryMorphism, OrdinaryMorphism>, Pair<OrdinaryMorphism, OrdinaryMorphism>>
			p,		
			final Rule r, 
			boolean isFirstRule,
			boolean nested) {
			
		Graph criticalGraph = p.first.first.getTarget();
		OrdinaryMorphism iso = criticalGraph.isoCopy();
		if (iso == null)
			return null;
		
		iso.getTarget().setName(iso.getTarget().getName().replace("_copy", ""));
		// create an ac morphism : r.LHS --> iso.target
		OrdinaryMorphism ac = (nested)? 
							BaseFactory.theFactory().createGeneralMorphism(r.getLeft(), iso.getTarget())
							: BaseFactory.theFactory().createMorphism(r.getLeft(), iso.getTarget());
							
		// get critical pair
		Pair<OrdinaryMorphism,OrdinaryMorphism> cp1 = p.first;
		Pair<OrdinaryMorphism,OrdinaryMorphism> cp2 = p.second;
		OrdinaryMorphism o1 = cp1.first;
		OrdinaryMorphism o2 = cp1.second;
		
//		Hashtable<String,String> 
//		varEqualName = VariableEqualityDialog.getVarNameEquality(
//							criticalGraph.getHelpInfoAboutVariableEquality());
////		System.out.println(varEqualName);
//		String errMsg = "";
		
		boolean mapOK = true;
		// set nac mappings
		if (isFirstRule) {				
			Enumeration<GraphObject> dom = o1.getDomain();
			while (dom.hasMoreElements()) {
				GraphObject go = dom.nextElement();
				try {
					if (go.getContext() == r.getLeft()) {
						ac.addMapping(go, iso.getImage(o1.getImage(go)));
					} else if (go.getContext() == r.getRight()) {
						Enumeration<GraphObject> inverse = r.getInverseImage(go);
						if (inverse.hasMoreElements()) {
							GraphObject goL = inverse.nextElement(); 				
							ac.addMapping(goL, iso.getImage(o1.getImage(go)));
						} else {
//							errMsg = "One of critical objects has reference to a new RHS object.";						
							mapOK = false;
							break;
						}
					} 
				} catch (BadMappingException ex) {
//					System.out.println(ex.getMessage());
					mapOK = false;
				}
			}
			mapOK = mapOK && !ac.isEmpty();
		} else {
			Enumeration<GraphObject> dom = o2.getDomain();
			while (dom.hasMoreElements()) {
				GraphObject go = dom.nextElement();
				try {
					if (go.getContext() == r.getLeft()) {
						ac.addMapping(go, iso.getImage(o2.getImage(go)));
					} else if (go.getContext() == cp2.first.getTarget()) {
						Enumeration<GraphObject> inverse = cp2.first.getInverseImage(go);
						if (inverse.hasMoreElements()) {
							GraphObject goL = inverse.nextElement(); 				
							ac.addMapping(goL, iso.getImage(o2.getImage(go)));
						} 
					}
				} catch (BadMappingException ex) {
//					System.out.println(ex.getMessage());
					mapOK = false;
				}
			}
			mapOK = mapOK && !ac.isEmpty();
		}
		iso.dispose();
		if (!mapOK) {
			ac.dispose(false, true);
			return null;
		}
		
		return ac;
	}
	
	
	protected static boolean isCriticalPAC(OrdinaryMorphism pac, List<GraphObject> criticalContext) {
		boolean pacCritical = false;
		for (int j = 0; j < criticalContext.size() && !pacCritical; j++) {
			GraphObject o = criticalContext.get(j);
			Vector<GraphObject> 
			v = pac.getTarget().getElementsOfTypeAsVector(o.getType());
			if (!v.isEmpty()) {
				for (int i = 0; i < v.size(); i++) {
					GraphObject go = v.get(i);
					if (!pac.getInverseImage(go).hasMoreElements()) {
						pacCritical = true;
						break;
					}
				}
			}
		}
		return pacCritical;
	}
	
	protected static List<GraphObject> getObjsWithAttrValue(OrdinaryMorphism om, boolean isConst, boolean isVar) {
		List<GraphObject> list = new Vector<GraphObject>(1);
		Enumeration<GraphObject> objs = om.getTarget().getElements();
		while (objs.hasMoreElements()) {
			GraphObject o = objs.nextElement();
			if (o.getAttribute() != null) {
				boolean added = false;
				AttrInstance attr = o.getAttribute();
				for (int i = 0; i < attr.getNumberOfEntries(); i++) {
					ValueMember vm = (ValueMember) attr.getMemberAt(i);
					if (vm.isSet() 
							&& ((isConst && vm.getExpr().isConstant()) 
									|| (isVar && vm.getExpr().isVariable()))) {
						if (!added) {
							list.add(o); added = true;
						}
					}
				}
			}
		}
		if (list.isEmpty()) list = null;
		return list;
	}
	
	protected static List<GraphObject> getImgOfObj(
				OrdinaryMorphism om, List<GraphObject> objs) {
		
		if (objs == null || objs.isEmpty())
			return null;
		
		List<GraphObject> list = new Vector<GraphObject>(1);
		for(GraphObject o: objs) {
			GraphObject i = om.getImage(o);
			if (i!=null) list.add(i);
		}
		if (list.isEmpty()) list = null;
		return list;
	}
	
	protected static List<Triple<GraphObject,ValueMember,String>> getObjsWithConstOrVarDuetoImg(
			AttrContext attrCtxt,	// attr context of test match
			List<GraphObject> objs,	// overlap graph critical objs dueto NAC of rule2
			List<GraphObject> srcObjs, // lhs objs of rule1 critical dueto NAC of rule2
			OrdinaryMorphism om,	// rule1 morphism
			OrdinaryMorphism om1,	// NAC of rule2 morphism
			OrdinaryMorphism om2,	// NAC of rule2 into overlap graph
			boolean isConst, boolean isVar) {	// what to attent
		
		if (srcObjs == null || srcObjs.isEmpty() 
				|| objs == null || objs.isEmpty())
			return null;
		//TODO testen
		List<Triple<GraphObject,ValueMember,String >> 
		list = new Vector<Triple<GraphObject,ValueMember,String >>(1);
		for(GraphObject s: srcObjs) {
			AttrInstance s_attr = s.getAttribute();
			for(GraphObject obj: objs) {
				if (s.getType() == obj.getType()
						|| obj.getType().isParentOf(s.getType())
						|| s.getType().isParentOf(obj.getType())) {
					GraphObject t = om.getImage(s);
					if (t != null) {
						Enumeration<GraphObject> en2 = om2.getInverseImage(obj);
						if (en2.hasMoreElements()) {
							GraphObject n2_obj = en2.nextElement();
														
							List<Pair<GraphObject,ValueMember>> 
							tmp = new Vector<Pair<GraphObject,ValueMember>>(1);
						
							AttrInstance t_attr = t.getAttribute();
							AttrInstance obj_attr = obj.getAttribute();
							AttrInstance n2_obj_attr = n2_obj.getAttribute();
							for (int i = 0; i < s_attr.getNumberOfEntries(); i++) {
								ValueMember s_vm = (ValueMember) s_attr.getMemberAt(i);
								ValueMember t_vm = (ValueMember) t_attr.getMemberAt(s_vm.getName());
								ValueMember obj_vm = (ValueMember) obj_attr.getMemberAt(s_vm.getName());
								ValueMember n2_obj_vm = (ValueMember) n2_obj_attr.getMemberAt(s_vm.getName());
								
								if (obj_vm != null && n2_obj_vm != null && n2_obj_vm.isSet()) {								
									// ?is s_vm != t_vm already given?
									if (!t_vm.isSet()
											|| (s_vm.isSet() && s_vm.getExprAsText().equals(t_vm.getExprAsText())))					
										continue;
								
									// now t_vm.isSet()
									if (isConst && n2_obj_vm.getExpr().isConstant()) {			
										if (t_vm.getExpr().isConstant()) {
											if (t_vm.getExprAsText().equals(n2_obj_vm.getExprAsText())) {					
												// collect potential critical attr. member
												tmp.add(new Pair<GraphObject,ValueMember>(obj, obj_vm));
											}
											else {
												// at least one constant failed 
												tmp.clear();
												break;
											}
										}
										else {
											if (!t_vm.getExprAsText().equals(s_vm.getExprAsText())) {					
												// collect potential critical attr. member
												tmp.add(new Pair<GraphObject,ValueMember>(obj, obj_vm));
											}
											else {
												// at least one constant failed 
												tmp.clear();
												break;
											}
										}
										
									}
									else if (isVar && n2_obj_vm.getExpr().isVariable()) {								
										// collect potential critical attr. member
										tmp.add(new Pair<GraphObject,ValueMember>(obj, obj_vm));
									}									
								}
							}
							
							if (!tmp.isEmpty()) {
								for(Pair<GraphObject,ValueMember> p: tmp) {
									ValueMember obj_vm = p.second;
									list.add(new Triple<GraphObject,ValueMember,String>(obj, obj_vm, obj_vm.getExprAsText()));
									
									/*
									if (obj_vm.getExpr().isConstant()) {
	//									String vname = "v"+String.valueOf(obj_vm.hashCode());
										String vname = obj_vm.getExprAsText().replaceAll("\"", "");
										VarTuple vars = (VarTuple)attrCtxt.getVariables();
										vars.declare(obj_vm.getDeclaration().getHandler(), 
														obj_vm.getDeclaration().getTypeName(), 
														vname);
										vars.getEntryAt(vname).setTransient(true);						
										obj_vm.setExprAsText(vname);
									}
									*/
								}
							}
						}
					}
				}
			}
		}
		if (list.isEmpty()) list = null;
		return list;
	}
		
	protected static boolean markObjDuetoNAC(
			List<Triple<GraphObject,ValueMember,String>> objs, 
			OrdinaryMorphism om1,
			OrdinaryMorphism om2) {
		boolean critical = false;
		if (objs != null) {
			for (int i=0; i<objs.size(); i++) {
				Triple<GraphObject,ValueMember,String> trio = objs.get(i);
				GraphObject img2 = om2.getImage(trio.first);
				if (img2 != null) {
					img2.setCritical(true);
					critical = true;
				}
			}
		}
		return critical;
	}
	
	public static boolean bothOrigAndImgCriticalOrNot(OrdinaryMorphism om){
		Enumeration<GraphObject> dom = om.getDomain();
		while (dom.hasMoreElements()) {
			GraphObject o1 = dom.nextElement();
			GraphObject o2 = om.getImage(o1);
			if (o2 != null) {
				if ((o1.isCritical() && o2.isCritical())		
						|| (!o1.isCritical() && !o2.isCritical())) {
					// continue
				}
				else
					return false;
			}
		}
		return true;
	}

	public static boolean checkIfMorphSimilar(Graph overlap1, Graph overlap2,
												OrdinaryMorphism first1, OrdinaryMorphism first2,
												OrdinaryMorphism second1, OrdinaryMorphism second2) {

		OrdinaryMorphism morph1 = (BaseFactory.theFactory()).createMorphism(overlap1, overlap2);
		OrdinaryMorphism morph2 = (BaseFactory.theFactory()).createMorphism(overlap1, overlap2);
		if (morph1.makeDiagram(first1, first2) && morph1.getSize() > 0) {
			if (!ExcludePairHelper.bothOrigAndImgCriticalOrNot(morph1)) {
				return false;
			}
			if (morph2.makeDiagram(second1, second2) && morph2.getSize() > 0) {
				if (!ExcludePairHelper.bothOrigAndImgCriticalOrNot(morph2)) {
					return false;
				}
				if (morph1.isPartialIsomorphicTo(morph2)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean node2ToNode1_ChildToParentMap(
			final Rule rule1,
			final OrdinaryMorphism overlap1,
			final OrdinaryMorphism overlap2) {	
		Iterator<Node> en1 = overlap1.getSource().getNodesSet().iterator();
		while (en1.hasNext()) {			
			Node go1 = en1.next();
			if (rule1.getImage(go1) == null) {
				Node img = (Node) overlap1.getImage(go1);
				Enumeration<GraphObject> en = overlap2.getInverseImage(img);
				if (en.hasMoreElements()) {
					Node go2 = (Node) en.nextElement();					
					if (go2.getType().isChildOf(go1.getType())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static boolean newNode_TypeToTypeMap(
			final Rule rule1,
			final OrdinaryMorphism overlap1) {	
				
		Iterator<Node> en1 = overlap1.getSource().getNodesSet().iterator();
		while (en1.hasNext()) {			
			Node go1 = en1.next();
			Node img = (Node) overlap1.getImage(go1);
			Enumeration<GraphObject> en = rule1.getInverseImage(go1);
			if (!en.hasMoreElements() && !go1.getType().compareTo(img.getType())) {
				return false;
			}
		}
		return true;
	}
	
	public static Vector<Pair<ValueMember, ValueMember>> getAttrMemberByParentType(
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> attrs,
			final AttrType attrtype) {
	
		Enumeration<AttrType> keys = attrs.keys();
		while (keys.hasMoreElements()) {
			AttrType t = keys.nextElement();
			if (((DeclTuple)attrtype).hasChild((DeclTuple)t))
				return attrs.get(t);				
		}
		return null;
	}
	
	public static Vector<Pair<ValueMember, ValueMember>> getAttrMemberByChildType(
			final Hashtable<AttrType, Vector<Pair<ValueMember, ValueMember>>> attrs,
			final AttrType attrtype) {
	
		Enumeration<AttrType> keys = attrs.keys();
		while (keys.hasMoreElements()) {
			AttrType t = keys.nextElement();
			if (((DeclTuple)attrtype).hasParent((DeclTuple)t))
				return attrs.get(t);				
		}
		return null;
	}
}
