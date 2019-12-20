/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.xt_basis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Date;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

import agg.cons.AtomConstraint;
import agg.util.IntComparator;
import agg.util.OrderedSet;
import agg.util.Pair;
import agg.xt_basis.agt.RuleScheme;


public class PriorityGraTraImpl extends GraTra {

	Random ran = new Random();

	private boolean appliedOnce;

//	private boolean allRulesEnabled = false;

	private boolean priorityGraTra = false;

	private Vector<Pair<Integer, HashSet<Rule>>> sortedRules;

	private File f;

	private FileOutputStream os;

	private String protocolFileName = "";
	
	private boolean grammarChecked;
	
//	private long time0;
	
	public PriorityGraTraImpl() {
		this.sortedRules = new Vector<Pair<Integer, HashSet<Rule>>>();
	}

	public void dispose() {
		if (this.sortedRules != null)
			this.sortedRules.clear();	
		super.dispose();
	}
	
	/** not implemented yet! * */
	public Pair<Morphism, Morphism> derivation(Match m){
		return (null);
	}

	public boolean atLeastOneRuleHasPriority() {
		for (int i = 0; i < this.currentRuleSet.size(); i++) {
			Rule r = this.currentRuleSet.get(i);
			if (r.getPriority() > 0)
				return true;
		}
		return false;
	}

	private void sortByPriority(Vector<Rule> rules) {
		RulePriority priority = new RulePriority(rules);
		Integer startPriority = priority.getStartPriority();
		Hashtable<Integer, HashSet<Rule>> invertedRulePriority = priority.invertPriority();
		
		OrderedSet<Integer> rulePrioritySet = new OrderedSet<Integer>(new IntComparator<Integer>());
		for (Enumeration<Integer> en = invertedRulePriority.keys(); en
				.hasMoreElements();) {
			rulePrioritySet.add(en.nextElement());
		}
		int i = 0;
		
		Integer maxPriorityInt = null;
		HashSet<Rule> priority0Set = null;
		Integer currentPriority = startPriority;
		boolean nextPriorityExists = true;
		while (nextPriorityExists && (currentPriority != null)) {
			HashSet<Rule> rulesForPriority = invertedRulePriority.get(currentPriority);
			Pair<Integer, HashSet<Rule>> p = new Pair<Integer, HashSet<Rule>>(
					currentPriority, rulesForPriority);
			if (currentPriority.intValue() > 0)
				this.sortedRules.add(p);
			else {
				priority0Set = rulesForPriority;
			}
			maxPriorityInt = currentPriority;

			// set next Layer
			i++;
			if (i < rulePrioritySet.size()) {
				currentPriority = rulePrioritySet.get(i);
			}
			else {
				nextPriorityExists = false;
			}
		}
		if (priority0Set != null && maxPriorityInt != null) {
			int maxPriority = maxPriorityInt.intValue() + 1;
			Pair<Integer, HashSet<Rule>> p0 = new Pair<Integer, HashSet<Rule>>(new Integer(
					maxPriority), priority0Set);
			this.sortedRules.add(p0);
		}
	}

	public boolean apply() {
		boolean applied = false;
		if (atLeastOneRuleHasPriority()) {
			this.priorityGraTra = true;
			sortByPriority(this.currentRuleSet);
			applied = applyPriorityGraTra();
			return applied;
		} 
		return false;
	}

	@SuppressWarnings("rawtypes")
	private boolean applyPriorityGraTra() {
		boolean result = false;

		Pair<Integer, HashSet<Rule>> pFirst = this.sortedRules.get(0);
//		int firstpriority = pFirst.first.intValue();
		HashSet ruleSetFirst = pFirst.second;
		
		boolean asLongAsPossible = true;
		boolean applied = true;
		while (!this.stopping && applied) {
			if (!this.stopping) {
				// apply rule with highest priority non-deterministically. 
				// as long as possible
				asLongAsPossible = true;
				applied = applyRandomly(ruleSetFirst, asLongAsPossible);
				
//				if (applied && options.hasOption(GraTraOptions.CONSISTENCY_CHECK_AFTER_GRAPH_TRAFO)) {
//					this.checkGraphConsistencyForLayer(firstpriority);
//				}
			}

			asLongAsPossible = false;
			Pair<Integer, HashSet<Rule>> p = null;
			HashSet<Rule> ruleSet = null;
			for (int i = 1; !this.stopping && i < this.sortedRules.size(); i++) {
				p = this.sortedRules.get(i);
//				int priority = p.first.intValue();
				ruleSet = p.second;
				if (applyRandomly(ruleSet, asLongAsPossible)) {
					applied = true;
//					if (options.hasOption(GraTraOptions.CONSISTENCY_CHECK_AFTER_GRAPH_TRAFO)) {
//						this.checkGraphConsistencyForLayer(priority);
//					}
					break;
				}
			}
			if (applied)
				result = true;
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	private boolean applyRandomly(Vector<Rule> rules, boolean asLongAsPossible) {
		boolean result = false;
		boolean applied = true;
		while (applied) {
			applied = false;
			Vector<?> v = (Vector) rules.clone();
			while (!v.isEmpty()) {
				int j = this.ran.nextInt(v.size());
				this.currentRule = (Rule) v.get(j);
				
				if (this.currentRule instanceof RuleScheme) {
	    			applied = apply((RuleScheme) this.currentRule);
	    		} 
				else {
					applied = this.currentRule.canMatch(this.hostgraph, this.strategy)
									&& applyRule(this.currentRule);
	    		}
				
				if (applied) {
					result = true;
					if (asLongAsPossible)
						break;
					
					return result;
				} 
				v.remove(this.currentRule);
			}
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	private boolean applyRandomly(HashSet rules, boolean asLongAsPossible) {
		Vector<Rule> rulesVec = new Vector<Rule>(rules.size());
		Iterator<?> en = rules.iterator();
		while (en.hasNext()) {
			Rule r = (Rule) en.next();
			rulesVec.add(r);
		}
		return applyRandomly(rulesVec, asLongAsPossible);
	}

	private boolean applyRule(Rule r) {
		boolean applied = false;
		while (apply(r)) {
			applied = true;
			break;
		}
		String s0 = r.getName() + " \t applied:  " + applied;
		System.out.println(s0);
		writeTransformProtocol(s0);
		String s1 = "";
		if (!applied) {
			s1 = s1 + getErrorMsg();
			writeTransformProtocol(s1);
		}
		return applied;
	}

	/*
	private boolean applyDefaultGraTra() {
		boolean applied = false;
		while (!this.stopping && (currentRuleSet.size() > 0) && !applied) {
			int i = ran.nextInt(currentRuleSet.size());
			// System.out.println("random i: "+i);
			this.currentRule = currentRuleSet.elementAt(i);

			if (this.currentRule instanceof RuleScheme) {
    			applied = apply((RuleScheme) this.currentRule);
    		} 
			else {
				applied = this.currentRule.canMatch(hostgraph, this.strategy)
									&& apply(this.currentRule);
    		}
			
			String s0 = this.currentRule.getName() + " \t applied:  " + applied;
			writeTransformProtocol(s0);
			String s1 = "";
			if (!applied) {
				s1 = s1 + getErrorMsg();
				writeTransformProtocol(s1);

				currentRuleSet.removeElementAt(i);
				String ss1 = getRuleNames(currentRuleSet);
				writeTransformProtocol(ss1);
			} else {
				appliedOnce = true;
				if (!isGraphConsistent())
					this.stopping = true;
			}
		}
		return applied;
	}
*/
	
	public void transform(List<Rule> ruleSet) {
//		allRulesEnabled = true;
		this.currentRuleSet.clear();
		this.currentRuleSet.addAll(ruleSet);
		boolean result = apply();
		if (!result && !this.priorityGraTra)
			writeTransformProtocol("Graph transformation failed. No rule priority used.");
	}
	
	public void transform() {
		this.stopping = false;
		
		if(!this.grammar.getListOfRules().isEmpty() && this.currentRuleSet.isEmpty())
			setRuleSet();
		
		if (this.writeLogFile) {
			String dirName = this.grammar.getDirName();
			String fileName = this.grammar.getFileName();
			if ((fileName == null) || fileName.equals(""))
				fileName = this.grammar.getName();
			openTransformProtocol(dirName, fileName);
			String version = "Version:  AGG " + Version.getID() + "\n";
			writeTransformProtocol(version);
			String s0 = "Graph transformation by rule priority of : "
					+ this.grammar.getName();
			String s1 = "at graph : " + this.grammar.getGraph().getName();
			String s2 = getRuleNames(this.currentRuleSet);
			writeTransformProtocol(s0);
			writeTransformProtocol(s1);
			writeTransformProtocol(s2);
			writeTransformProtocol("\n");
		}
		// first check the rules, the graph
		if (!this.grammarChecked) {
			Pair<Object, String> pair = this.grammar.isReadyToTransform(true);
			if (pair != null) {
				Object test = pair.first;
				if (test != null) {
					String s0 = pair.second + "\nTransformation is stopped.";

					if (test instanceof Type)
						((GraTra) this).fireGraTra(new GraTraEvent(this,
								GraTraEvent.ATTR_TYPE_FAILED, s0));
					else if (test instanceof Rule)
						((GraTra) this).fireGraTra(new GraTraEvent(this,
								GraTraEvent.RULE_FAILED, s0));
					else if (test instanceof AtomConstraint)
						((GraTra) this).fireGraTra(new GraTraEvent(this,
								GraTraEvent.ATOMIC_GC_FAILED, s0));
					transformFailed(s0);
					return;
				}
			} else if (!this.grammar.isGraphReadyForTransform()) {
				String s0 = "Graph of the grammar is not fine."
						+ "\nPlease check attribute settings of the objects. \nTransformation is stopped.";
				((GraTra) this).fireGraTra(new GraTraEvent(this,
						GraTraEvent.GRAPH_FAILED, s0));
				transformFailed(s0);
				return;
			}
			else if (!this.checkGraphConsistency()) {
				String s = "Graph consistency failed."
						+ "\nPlease check the host graph against the graph constraints."
						+ "\nTransformation is stopped.";
					((GraTra) this).fireGraTra(new GraTraEvent(this,
							GraTraEvent.GRAPH_FAILED, s));
					transformFailed(s);
					return;
			}
			this.grammarChecked = true;
		}

		// stop start time
		long startTime = System.currentTimeMillis();
//		time0 = startTime;
		
		Vector<Rule> ruleSet = getEnabledRules(this.currentRuleSet);
		transform(ruleSet);

		if (this.options.hasOption(GraTraOptions.CONSISTENCY_CHECK_AFTER_GRAPH_TRAFO)) {
			this.checkGraphConsistency();
		}
		
		// stop time
		System.out.println("Used time for graph transformation:  "
					+ (System.currentTimeMillis() - startTime) + "ms");

		if (this.writeLogFile) {
			writeUsedTimeToProtocol("Used time for graph transformation: ", startTime);
			writeTransformProtocol("\nGraph transformation finished");
			closeTransformProtocol();
		}
			
		fireGraTra(new GraTraEvent(this, GraTraEvent.TRANSFORM_FINISHED,
				this.errorMsg));
	}

	private void writeUsedTimeToProtocol(String text, long beginTime) {
		writeTransformProtocol(text+
				+ (System.currentTimeMillis()-beginTime) + "ms");

//		time0 = System.currentTimeMillis();
	}
	
	private Vector<Rule> getEnabledRules(Vector<Rule> ruleSet) {
		Vector<Rule> vec = new Vector<Rule>(ruleSet.size());
		for (int j = 0; j < ruleSet.size(); j++) {
			if (ruleSet.elementAt(j).isEnabled())
				vec.add(ruleSet.elementAt(j));
		}
		return vec;
	}

	private void transformFailed(String text) {
		System.out.println(text);
		writeTransformProtocol(text);
		writeTransformProtocol("\nGraph transformation failed");
		// fireGraTra(new GraTraEvent(this,GraTraEvent.TRANSFORM_FAILED,
		// this.errorMsg));
		fireGraTra(new GraTraEvent(this, GraTraEvent.TRANSFORM_FINISHED,
				this.errorMsg));
		closeTransformProtocol();
	}

	public boolean transformationDone() {
		return this.appliedOnce;
	}

	public String getProtocolName() {
		return this.protocolFileName;
	}

	private String getRuleNames(Vector<Rule> rules) {
		String names = "[ ";
		for (int j = 0; j < rules.size(); j++) {
			Rule r = rules.elementAt(j);
			names = names + r.getName() + " ";
		}
		names = names + "]";
		return names;
	}

	private void openTransformProtocol(String dirName, String fileName) {
		String dName = dirName;
		String fName = "PriorityGraTra.log";
		// System.out.println("PriorityGraTraImpl.openTransformProtocol:
		// dirName: "+dirName);
		// System.out.println("PriorityGraTraImpl.openTransformProtocol:
		// fileName: "+fileName);
		if ((fileName != null) && !fileName.equals("")) {
			if (fileName.endsWith(".ggx"))
				fName = fileName.substring(0, fileName.length() - 4)
						+ "_GraTra.log";
			else
				fName = fileName + "_GraTra.log";
		}
		// System.out.println(fName);

		if ((dName != null) && !dName.equals("")) {
			this.f = new File(dirName);
			if (this.f.exists()) {
				if (this.f.isFile()) {
					if (this.f.getParent() != null)
						dName = this.f.getParent() + File.separator;
					else
						dName = "." + File.separator;
				} else if (this.f.isDirectory()) {
					// System.out.println(dirName);
					dName = this.f.getPath() + File.separator;
				} else
					dName = "." + File.separator;
			} else
				dName = "." + File.separator;
			this.f = new File(dirName + fName);
		} else
			this.f = new File(fName);

		try {
			this.os = new FileOutputStream(this.f);
			this.protocolFileName = this.f.getName();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}

		writeTransformProtocol((new Date()).toString());
	}

	private void writeTransformProtocol(String s) {
		if (this.os == null)
			return;
		try {
			if (!s.equals("\n"))
				this.os.write(s.getBytes());
			this.os.write('\n');
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void closeTransformProtocol() {
		if (this.os == null)
			return;
		try {
			this.os.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}
