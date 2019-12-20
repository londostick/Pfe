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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
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


public class LayeredGraTraImpl extends GraTra {

	Random ran = new Random();
	
	private boolean appliedOnce;

	private boolean stopLayerAndWait;

	private int layerToStop;
	
//	private boolean stopLayerOpt, stopLayer;

	private boolean breakLayerOpt, breakLayer;

	private boolean breakAllLayerOpt, breakAllLayer;

	private boolean applyContinue;
	
	private boolean nextLayerExists;
	
	private boolean waitAfterLayer;
		
	private boolean waitingAfterLayer;
	
	private boolean layeredLoop, resetGraphBeforeLoop;

	private Integer startLayer;

	private RuleLayer layer;

	private Hashtable<Integer, HashSet<Rule>> invertedRuleLayer;

	private OrderedSet<Integer> ruleLayer;
	
	private Integer currentLayer;

	private boolean startTransform;
	
	private boolean grammarChecked;
	
	private long time0; //, time=0; 

	File f;

	FileOutputStream os;

	String protocolFileName = "";

	
	public LayeredGraTraImpl() {		
		this.nextLayerExists = true;
	}

	public void dispose() {	
		if (this.layer != null)
			this.layer.dispose();		
		if (this.invertedRuleLayer != null)
			this.invertedRuleLayer.clear();
		if (this.ruleLayer != null)
			this.ruleLayer.clear();
		
		super.dispose();	
	}
	
	public void setGraTraOptions(Vector<String> newOptions) {
		super.setGraTraOptions(newOptions);
		if (newOptions.contains(GraTraOptions.BREAK_ALL_LAYER))
			this.breakAllLayerOpt = true;
		else if (newOptions.contains(GraTraOptions.BREAK_LAYER))
			this.breakLayerOpt = true;
		if (newOptions.contains(GraTraOptions.LOOP_OVER_LAYER))
			this.layeredLoop = true;
		if (newOptions.contains(GraTraOptions.RESET_GRAPH))
			this.resetGraphBeforeLoop = true;
	}

	public void stop() {
		if (this.breakLayerOpt) {
			this.breakLayer = true;
			this.breakAllLayer = false;
			this.stopping = this.breakAllLayer;
			this.waitAfterLayer = false;
		} else if (this.breakAllLayerOpt) {
			this.breakAllLayer = true;
			this.stopping = this.breakAllLayer;
			this.breakLayer = false;
			this.waitAfterLayer = false;
		} else if(this.waitAfterLayer) {
			this.stopping = false;
			this.stoppingRule = true;
			this.pauseRule = false;
			this.breakLayer = true;
			this.breakAllLayer = false;
	    }
//	    else{
//	        stopping = true; 
//	        stoppingRule = true;
//	        pauseRule = false;
//	        breakLayer = true;	
//			breakAllLayer = true;
//	    } 
		
	}

	public void unsetStop() {
		super.unsetStop();
		this.breakLayer = false;
		this.breakAllLayer = false;
		this.waitAfterLayer = false;
	}
	
	public void nextLayer() {
		transformLayers(true);
	}

	public int getCurrentLayer() {
		if (this.currentLayer != null)
			return this.currentLayer.intValue();
		
		return -1;
	}

	public void waitAfterLayer(boolean b) {		
		this.waitAfterLayer = b;
	}
	
	public boolean isWaitingAfterLayer(){
    	return this.waitingAfterLayer;
    }
	
	/** not implemented yet! * */
	public Pair<Morphism, Morphism> derivation(Match m) {
		return (null);
	}

	public boolean apply() {
		// if(!allRulesEnabled) {
		// // remove disabled rules from currentRuleSet
		// for(int j=0; j<currentRuleSet.size(); j++) {
		// if(!((Rule) currentRuleSet.elementAt(j)).isEnabled()) {
		// currentRuleSet.removeElementAt(j);
		// j--;
		// }
		// }
		// allRulesEnabled = true;
		// }

		if (this.currentRuleSet.isEmpty())
			return false;

		boolean applied = false;
		// first try to apply a trigger rule
		int i;
		String trigger = "";
		this.currentRule = this.currentRuleSet.elementAt(0);
		if (this.currentRule.isTriggerOfLayer()) {
			if (this.currentRule.isEnabled()) {
				// System.out.println("trigger rule: "+currentRule.getName());
				trigger = "(trigger of layer)";

				applied = this.currentRule.canMatch(this.hostgraph, this.strategy)
						&& apply(this.currentRule);

//				System.out.println(currentRule.getName() + " \t applied:  "+ applied);

				if (applied) {
					System.out.println(this.currentRule.getName() + " \t applied:  "+ applied);
					
					this.appliedOnce = true;
					if (!isGraphConsistent())
						this.stopping = true;

					this.currentRule.setEnabled(false);
					this.currentRuleSet.removeElement(this.currentRule);

					if (this.os != null)
						writeTransformProtocol(this.currentRule.getName() + " "
								+ trigger + " \t applied:  " + applied);
				} else {
					if (this.os != null) {
						writeTransformProtocol(this.currentRule.getName() + " "
								+ trigger + " \t applied:  " + applied);
						writeTransformProtocol(getErrorMsg());
						writeTransformProtocol("The trigger rule of the current layer failed. \nContinue with the next layer.");
					}
					this.currentRuleSet.removeAllElements();
				}

				applied = false;
			} else
				this.currentRuleSet.removeElement(this.currentRule);
		}

		while (!this.stopping && !this.breakLayer && !applied
				&& this.currentRuleSet.size() > 0) {
			
			i = this.ran.nextInt(this.currentRuleSet.size());
			this.currentRule = this.currentRuleSet.elementAt(i);

			if (this.currentRule instanceof RuleScheme) {
    			applied = apply((RuleScheme) this.currentRule);
    		} 
			else {
    			applied = this.currentRule.canMatch(this.hostgraph, this.strategy)
								&& apply(this.currentRule);
    		}

			if (this.os != null)
				writeTransformProtocol(this.currentRule.getName() + " \t applied:  "
						+ applied);
			if (!applied) {
				this.currentRuleSet.remove(this.currentRule);
				
				if (this.os != null) {
					writeTransformProtocol(getErrorMsg());
					writeTransformProtocol(getRuleNames(this.currentRuleSet));
				}
			} else {
//				System.out.println(this.currentRule.getName() + " \t applied:  "+ applied);
				
				this.appliedOnce = true;
				if (!isGraphConsistent())
					this.stopping = true;
			}
		}
		return applied;
	}

	public void transform(List<Rule> ruleSet) {
		this.layer = new RuleLayer(ruleSet);
		this.startLayer = this.layer.getStartLayer();
		this.invertedRuleLayer = this.layer.invertLayer();
		
		this.ruleLayer = new OrderedSet<Integer>(new IntComparator<Integer>());
		for (Enumeration<Integer> en = invertedRuleLayer.keys(); en
				.hasMoreElements();) {
			this.ruleLayer.add(en.nextElement());
		}
		
		this.startTransform = true;
		transformLayers(true);
	}
	
	public void transformContinue() { 
		this.applyContinue = true;
		this.pauseRule = false;   
		this.breakLayer = false;
		  	  
		transformCurrentLayer();
		    	
		if (this.pauseRule) return;
		    	
		if (this.layeredLoop && !this.stopping) {
		    if (this.currentLayer == null 
		    		&& (!this.resetGraphBeforeLoop
		    				//|| noMoreStopBeforeResetGraph
		    				)) {
		        this.startTransform = true;
		    }
		}
//		else if (this.currentLayer == null || this.stopping) {
//		     writeTransformProtocol("\nGraph transformation is finished");
//		     fireGraTra(new GraTraEvent(this,GraTraEvent.TRANSFORM_FINISHED));
//		     closeTransformProtocol();     
//		}
	}
	
	public void transformContinueWithNextLayer(){
		this.breakLayer = false;
				
		if (this.stoppingRule)
			this.stoppingRule = false;
				
		transformCurrentLayer();
		    	        
		if (this.layeredLoop && !this.stopping) {
		    if (this.currentLayer == null && !this.resetGraphBeforeLoop) {
		    	this.startTransform = true;
		    }
		}
		else if(this.currentLayer == null || this.stopping) {
			this.stopping = true;
		    writeFinishToProtocol(this.time0);
		}
	}
	
	public void transformContinueWithNextStep() {     	
		this.breakLayer = false;
		    	
		if (this.stoppingRule)
			this.stoppingRule = false;
		    	
		transformCurrentLayer();
		      	   	
		if (this.layeredLoop && !this.stopping) {
		    if (this.currentLayer == null && !this.resetGraphBeforeLoop) {
		        this.startTransform = true; 
		    }
		}
		 else if (this.currentLayer == null || this.stopping) {
			 writeFinishToProtocol(this.time0); 
		}
	}
	
	public void setStartTransform(boolean b) {
		this.startTransform = b;
	}

	int i;
	@SuppressWarnings({ "unused", "rawtypes" })
	private void transformCurrentLayer() {
		boolean oneApplied = false;
		this.waitingAfterLayer = false;
		if (this.startTransform) {
			this.currentLayer = this.startLayer;
			i = 0;
		}		
//		System.out.println("LayeredGraTraImpl.transformCurrentLayer()... "+this.currentLayer);
		
		this.startTransform = false;
		this.nextLayerExists = true;
					
		if (!this.stopping && this.nextLayerExists && (this.currentLayer != null)) {
			Vector<Rule> rules = new Vector<Rule>();
			if (!this.applyContinue) {
				// get rules of the current this.layer
				HashSet rulesForLayer = this.invertedRuleLayer.get(this.currentLayer);
				Iterator<?> en = rulesForLayer.iterator();
				while (en.hasNext()) {
					Rule rule = (Rule) en.next();
					// add trigger rule as the first element
					if (rule.isTriggerOfLayer())
						rules.add(0, rule);
					else
						rules.add(rule);
				}
				
				writeTransformProtocol("\n");
				writeTransformProtocol("Layer: " + this.currentLayer.toString());

				System.out.println("Layer " + this.currentLayer.toString() + ":  "
							+ getRuleNames(rules) + "{*}");
			} else
    			rules = this.currentRuleSet;
							
			boolean applied = true;
			while (!this.stopping && applied) {
				if(this.breakLayer) 
					break;
		    	
				if(!this.applyContinue){
					this.currentRuleSet.clear();
					this.currentRuleSet.addAll(rules);
				}
				
				applied = apply();

				if (applied)
					oneApplied = true;
					
//				 in case of input parameter
				if(this.pauseRule) {
					return;
				}
				if (this.breakLayer) {
						break;
				}
				
				if(applied && this.waitAfterStep) {
					return;
				}
			}

			if (this.options.hasOption(GraTraOptions.CONSISTENCY_CHECK_AFTER_GRAPH_TRAFO)) {
				if (!this.checkGraphConsistencyForLayer(this.currentLayer.intValue()))
					this.stopping = true;
			}
			
			if (!this.breakLayer) {
					System.out.println("Layer " + this.currentLayer.toString()
							+ "  used time: "+ (System.currentTimeMillis()-this.time0) + "ms");
			}
			else {
				System.out.println("Layer " + this.currentLayer.toString()
							+ "   broken");
				this.breakLayer = false;
			}

			writeUsedTimeToProtocol("used time: ",this.time0);

			enableTriggerRuleOfLayer(rules);

			// get next Layer
			i++;
			if (i < this.ruleLayer.size()) {
				this.currentLayer = this.ruleLayer.get(i);
			}
			else {
				this.nextLayerExists = false;
			}
			
			this.breakLayer = false;
			this.waitingAfterLayer = true;
			
			if (this.nextLayerExists && this.currentLayer != null) {
				fireGraTra(new GraTraEvent(this, GraTraEvent.LAYER_FINISHED));
			} else if (this.layeredLoop && !this.resetGraphBeforeLoop && this.appliedOnce) {										
					this.startTransform = true;
					
					fireGraTra(new GraTraEvent(this,
								GraTraEvent.LAYER_FINISHED));
			} else {
				writeFinishToProtocol(this.time0);
				fireGraTra(new GraTraEvent(this,
								GraTraEvent.TRANSFORM_FINISHED));
			}
		}		
	}
	
	@SuppressWarnings("rawtypes")
	private void transformLayers(boolean anApply) {
//		System.out.println("LayeredGraTraImpl:: this.layeredLoop: "+this.layeredLoop
//				+"   anApply: "+anApply+"  this.startTransform: "+this.startTransform
//				+"   reset graph: "+resetGraph);
		
		boolean oneApplied = anApply;
		String layerStr = "";
		while (oneApplied) {			
			if (this.startTransform) {
				fireGraTra(new GraTraEvent(this,
						GraTraEvent.TRANSFORM_START));
				
				this.currentLayer = this.startLayer;
				i = 0;
				this.appliedOnce = false;
			}		
			this.startTransform = false;
			oneApplied = false;
			this.nextLayerExists = true;
			
			while (!this.stopping && this.nextLayerExists && this.currentLayer != null) {
				layerStr = String.valueOf(this.currentLayer.intValue());
				
				// get rules of the current this.layer
				HashSet rulesForLayer = this.invertedRuleLayer.get(this.currentLayer);
				Vector<Rule> rules = new Vector<Rule>();
				Iterator<?> en = rulesForLayer.iterator();
				while (en.hasNext()) {
					Rule rule = (Rule) en.next();
					// add trigger rule as the first element
					if (rule.isTriggerOfLayer())
						rules.add(0, rule);
					else
						rules.add(rule);
				}
				
				writeTransformProtocol("\n");
				writeTransformProtocol("Layer: " + this.currentLayer.toString());

				System.out.println("Layer " + this.currentLayer.toString() + ":  "
						+ getRuleNames(rules) + "{*}");

				boolean applied = true;
				while (!this.stopping && applied) {
					String s = getRuleNames(rules);
					writeTransformProtocol(s);

					this.currentRuleSet.clear();
					this.currentRuleSet.addAll(rules);
					
					applied = apply();

					if (applied) {
						oneApplied = true;
					}
//					in case of input parameter
					if(this.pauseRule) {
						return; 
					}
					if (this.breakLayer) {
						break;
					}
				}
				
				if (this.options.hasOption(GraTraOptions.CONSISTENCY_CHECK_AFTER_GRAPH_TRAFO)) {
					if (!this.checkGraphConsistencyForLayer(this.currentLayer.intValue()))
						this.stopping = true;
				}
				
				if (!this.breakLayer) {
					System.out.println("Layer " + this.currentLayer.toString()
							+ "  used time: "+ (System.currentTimeMillis()-this.time0) + "ms");
				}
				else {
					System.out.println("Layer " + this.currentLayer.toString()
							+ "   broken");
					this.breakLayer = false;
				}
				
//				time = time+this.time0;
				
				writeUsedTimeToProtocol("used time: ",this.time0);
				
				enableTriggerRuleOfLayer(rules);
				
				// get next Layer
				i++;
				if (i < this.ruleLayer.size()) {
					this.currentLayer = this.ruleLayer.get(i);
				}
				else {
					this.nextLayerExists = false;
				}
				
				if (!this.nextLayerExists || this.currentLayer == null) {
					this.nextLayerExists = false;
					if (this.layeredLoop) {
						if(this.appliedOnce && !this.resetGraphBeforeLoop && !this.stopping) {
							this.nextLayerExists = true;
							this.startTransform = true;	
							break;
						} else {							
							writeFinishToProtocol(this.time0);
							fireGraTra(new GraTraEvent(this,
									GraTraEvent.TRANSFORM_FINISHED));
						}
					}
				}
				
				if (stopLayerAndWait(Integer.valueOf(layerStr).intValue())) {						
					break;
				} 
				
//				fireGraTra(new GraTraEvent(this, GraTraEvent.LAYER_FINISHED, layerStr));										
			}

			if ((!"".equals(layerStr) && stopLayerAndWait(Integer.valueOf(layerStr).intValue()))
					|| !this.layeredLoop) {				
				break;
			}
		}
		
		if ((!"".equals(layerStr) && stopLayerAndWait(Integer.valueOf(layerStr).intValue()))
				&& this.nextLayerExists && !this.stopping) {
			fireGraTra(new GraTraEvent(this, GraTraEvent.LAYER_FINISHED, layerStr));
		} else if (!this.startTransform) {
			writeFinishToProtocol(this.time0);			
			fireGraTra(new GraTraEvent(this,
					GraTraEvent.TRANSFORM_FINISHED));
		}
	}

	private boolean stopLayerAndWait(int layerNm) {
		if (this.stopLayerAndWait) {	
			if (this.layerToStop == -1 
					|| (this.currentLayer != null && this.layerToStop == layerNm) ){
				return true;
			}
		}
		return false;
	}
	
	public void transform() {
		this.stopping = false;
		
		if(!this.grammar.getListOfRules().isEmpty() && this.currentRuleSet.isEmpty()) {
			setRuleSet();
		}
		
		if (this.time0 > 0
				&& this.options.hasOption(GraTraOptions.RESET_GRAPH)
				&& this.options.hasOption(GraTraOptions.LOOP_OVER_LAYER)) {
			this.transformWhenResetGraph();
			return;
		}		
		
		if (this.writeLogFile) {
			String dirName = this.grammar.getDirName();
			String fileName = this.grammar.getFileName();
			if ((fileName == null) || fileName.equals(""))
				fileName = this.grammar.getName();
			openTransformProtocol(dirName, fileName);
			String version = "Version:  AGG " + Version.getID() + "\n";
			writeTransformProtocol(version);
			String s0 = "Layered graph transformation of : " + this.grammar.getName();
			String s1 = "on graph : " + this.grammar.getGraph().getName();
			String s2 = getRuleNames(this.currentRuleSet);
			writeTransformProtocol(s0);
			writeTransformProtocol(s1);
			writeTransformProtocol(s2);
		}
		
		// first check the rules, the graph
		if (!this.grammarChecked) {
			Pair<Object, String> checkpair = this.grammar.isReadyToTransform(true);
			if (checkpair != null) {
				Object test = checkpair.first;
				if (test != null) {
					String s0 = checkpair.second + "\nTransformation stopped.";

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
			}
			// now check the host graph
			else if (!this.grammar.isGraphReadyForTransform()) {
				String s0 = "Transformation stopped.\nThe graph  <"
						+ this.grammar.getGraph().getName()
						+">  isn't fine."
						+ "\nPlease check attribute settings of nodes and edges.";			
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

//		System.out.println(this.options.getOptions());

		// stop start time
		long startTime = System.currentTimeMillis();
		this.time0 = startTime;
	
		Vector<Rule> ruleSet = getEnabledRules(this.currentRuleSet);
				
		transform(ruleSet);
		
		if (this.options.hasOption(GraTraOptions.CONSISTENCY_CHECK_AFTER_GRAPH_TRAFO)) {
			this.checkGraphConsistency();
		}
		
		
		System.out.println("Used time for graph transformation:1  "
				+ (System.currentTimeMillis() - startTime) + "ms");

		if (this.writeLogFile) {
			writeUsedTimeToProtocol("Used time for graph transformation:1 ", startTime);			
			writeTransformProtocol("\nGraph transformation finished");
			closeTransformProtocol();
		}

	}
		
	private void transformWhenResetGraph() {
//		System.out.println(this.options.getOptions());

		// stop start time
		long startTime = System.currentTimeMillis();
		this.time0 = startTime;
	
		Vector<Rule> ruleSet = getEnabledRules(this.currentRuleSet);
				
		transform(ruleSet);
		
		if (this.options.hasOption(GraTraOptions.CONSISTENCY_CHECK_AFTER_GRAPH_TRAFO)) {
			this.checkGraphConsistency();
		}	
		
		System.out.println("Used time for graph transformation:  "
				+ (System.currentTimeMillis() - startTime) + "ms");

		if (this.writeLogFile) {
			writeUsedTimeToProtocol("Used time for graph transformation: ", startTime);			
			writeTransformProtocol("\nGraph transformation finished");
			closeTransformProtocol();
		}
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
		// errorMsg));
		fireGraTra(new GraTraEvent(this, GraTraEvent.TRANSFORM_FINISHED,
				this.errorMsg));
		closeTransformProtocol();
	}

	private void writeUsedTimeToProtocol(String text, long beginTime) {
		writeTransformProtocol(text+
				+ (System.currentTimeMillis()-beginTime) + "ms");

		this.time0 = System.currentTimeMillis();
	}

	private void writeFinishToProtocol(long beginTime) {
		writeTransformProtocol("\nNo more layers.\nGraph transformation finished");
//		fireGraTra(new GraTraEvent(this, GraTraEvent.TRANSFORM_FINISHED));
		closeTransformProtocol();
	}

	
	public void setLayeredLoop(boolean b) {
		this.layeredLoop = b;
	}

	public void setResetGraphBeforeLoop(boolean b) {
		this.resetGraphBeforeLoop = b;
	}
	
	public void setStopLayerAndWait(boolean b) {
		this.stopLayerAndWait = b;
		this.layerToStop = -1;
	}

	public void setLayerToStop(int l) {
		this.layerToStop = l;
	}
	
	public void setBreakLayer(boolean b) {
		this.breakLayerOpt = b;
	}

	public void setBreakAllLayer(boolean b) {
		this.breakAllLayerOpt = b;
	}

	public boolean transformationDone() {
		return this.appliedOnce;
	}

	public String getProtocolName() {
		return this.protocolFileName;
	}

//	public long getUsedTime() {
//		return time;
//	}
	
	

	private void enableTriggerRuleOfLayer(Vector<Rule> rules) {
		for (int j = 0; j < rules.size(); j++) {
			Rule r = rules.elementAt(j);
			if (r.isTriggerOfLayer()) {
				r.setEnabled(true);
				break;
			}
		}
	}

	private String getRuleNames(List<Rule> rules) {
		String names = "[  ";
		for (int j = 0; j < rules.size(); j++) {
			Rule r = rules.get(j);
			names = names + r.getName() + "  ";
		}
		names = names + "]";
		return names;
	}

	private void openTransformProtocol(String dirName, String fileName) {
		String dName = dirName;
		String fName = "LayeredGraTra.log";
		// System.out.println("DefaultGraTraImpl.openTransformProtocol: dirName:
		// "+dirName);
		// System.out.println("DefaultGraTraImpl.openTransformProtocol:
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
			this.f = new File(dName);
			if (this.f.exists()) {
				if (this.f.isFile()) {
					if (this.f.getParent() != null)
						dName = this.f.getParent() + File.separator;
					else
						dName = "." + File.separator;
				} else if (this.f.isDirectory())
					dName = this.f.getPath() + File.separator;
				else
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
		if (!this.os.getChannel().isOpen())
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
