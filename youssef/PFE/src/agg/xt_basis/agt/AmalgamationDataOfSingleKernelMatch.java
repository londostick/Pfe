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
package agg.xt_basis.agt;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import agg.xt_basis.Rule;

/**
 * This class stores the data of amalgamation process for a single match
 * of the kernel rule (subrule) 
 * and all possible matches of multi rules (extending rules) 
 * of an interaction rule scheme. 
 * 
 * @author olga
 *
 */
public class AmalgamationDataOfSingleKernelMatch {

	/**
	 * Store object of the amalgamation help morphisms of the kernel rule.
	 */
	final public AmalgamationRuleData kernelInstData;
	
	/**
	 * Store object of the amalgamation help morphisms of multi rules.
	 */
	final public Hashtable<Rule, List<AmalgamationRuleData>> instMultiData;
	
	
	/**
	 * Creates a store object of help morphisms data of multi rules for
	 * only one match of the kernel rule.
	 * Each multi rule is referenced to a list of <code>AmalgamationRuleData</code>.
	 * 
	 *  
	 * @param kernelData 	help data of kernel rule
	 */
	public AmalgamationDataOfSingleKernelMatch(final AmalgamationRuleData kernelData) {
		this.kernelInstData = kernelData;
		
		this.instMultiData = new Hashtable<Rule, List<AmalgamationRuleData>>();
	}
		
	/**
	 * Adds new data to the list of data of the specified rule.
	 * @param rule	the rule of a rule scheme
	 * @param data	new data object
	 */
	public void put(final Rule rule, final AmalgamationRuleData data) {
		if (this.instMultiData.get(rule) == null)
			this.instMultiData.put(rule, new Vector<AmalgamationRuleData>());
		
		this.instMultiData.get(rule).add(data);
	}
	
	/**
	 * Returns computed amalgamation data lists of rules.
	 */
	public Hashtable<Rule, List<AmalgamationRuleData>> getData() {
		return this.instMultiData;
	}
	
	/**
	 * 
	 * @param rule 	the rule of a rule scheme
	 * @return	true, if the data list of the specified rule is empty,
	 * otherwise false.
	 */
	public boolean isEmpty(final Rule rule) {
		if (this.instMultiData.get(rule) != null)
			return this.instMultiData.get(rule).isEmpty();
		
		return true;
	}
	
	public List<AmalgamationRuleData> getRuleData(final Rule rule) {
		return this.instMultiData.get(rule);
	}
	
	public AmalgamationRuleData getRuleData(final Rule rule, int indx) {
		if (this.instMultiData.get(rule) != null
				&& (indx >= 0 && indx < this.instMultiData.get(rule).size()))
			return this.instMultiData.get(rule).get(indx);
		
		return null;
	}
	
	public boolean removeRuleData(final Rule rule, final AmalgamationRuleData data) {
		if (this.instMultiData.get(rule) != null)
			return this.instMultiData.get(rule).remove(data);
		
		return false;
	}
	
	public AmalgamationRuleData removeRuleData(final Rule rule, int indx) {
		if (this.instMultiData.get(rule) != null)
			return this.instMultiData.get(rule).remove(indx);
		
		return null;
	}
	
	public void removeRule(final Rule rule) {
		if (this.instMultiData.get(rule) != null)
			this.instMultiData.remove(rule);
	}
	
	public int size() {
		return this.instMultiData.size();
	}
	
	public int ruleDataSize(final Rule rule) {
		if (this.instMultiData.get(rule) != null)
			return this.instMultiData.get(rule).size();
		
		return 0;
	} 
	
	public AmalgamationRuleData getKernelData() {
		return this.kernelInstData;
	}
	
	
}
