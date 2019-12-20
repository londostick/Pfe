package agg.modification;

import agg.cons.AtomConstraint;
import agg.util.Pair;
import agg.xt_basis.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.NodeType;

public class AGGRunner {

	//	XtBasisUsing e =new XtBasisUsing();
	private final BaseFactory bf = BaseFactory.theFactory();

    private static GraGra graphGrammar,graphGrammar2;
    private static GraGra ruleGrammar ,ruleGrammar2;
    private final MorphCompletionStrategy strategy = CompletionStrategySelector.getDefault();

    private static final Boolean DEBUG = false;
    Graph g =new Graph();

    public AGGRunner() throws Exception {
    	
    	/*String filename="/Users/Youssef Rebai/eclipse-workspace/test2.ggx";
        BaseFactory bf = BaseFactory.theFactory();
        graphGrammar = bf.createGraGra();
        graphGrammar.load(filename);
        
        ruleGrammar = bf.createGraGra();
        ruleGrammar.load(filename);
        
        ruleGrammar = graphGrammar;*/
       //graphGrammarTransform(filename);
      /*  String filename2="/Users/Youssef Rebai/eclipse-workspace/test.ggx";
        BaseFactory bf2 = BaseFactory.theFactory();
        graphGrammar2 = bf2.createGraGra();
        graphGrammar2.load(filename2);
        
        ruleGrammar2 = bf2.createGraGra();
        ruleGrammar2.load(filename2);
        
        ruleGrammar2 = graphGrammar2;
       //graphGrammarTransform(filename2);*/
        Test();
    }
	public static void main(String[] args) throws Exception {
		new AGGRunner();
	}

    
   
   /* public  void showGraph() throws Exception {
    	
        System.out.println("\nGraph: " + graphGrammar.getGraph().getName() + " {");
        
        
        Node nl1 = null, nl2 = null, nr1 = null, nr2 = null;    	
    	String filename="/Users/Youssef Rebai/eclipse-workspace/test2.ggx";
    	Rule Rule,Rule1 = null;
    	

	String a,b;

        BaseFactory bf = BaseFactory.theFactory();
        graphGrammar = bf.createGraGra();
        graphGrammar.load(filename);
        
        ruleGrammar = bf.createGraGra();
        ruleGrammar.load(filename);
        
        ruleGrammar = graphGrammar;
        graphGrammarTransform(filename);
        Rule= ruleGrammar.getRule(0);
        Rule1= ruleGrammar.getRule(1);
		Graph left = Rule1.getLeft();
		Graph right = Rule1.getRight();
		
		Graph left0 = Rule.getLeft();
		Graph right0 = Rule.getRight();
		
        Iterator<?> e = graphGrammar.getGraph().getArcsSet().iterator();
        while (e.hasNext()) {
            Arc arc = (Arc) e.next();
            Node src = (Node) arc.getSource();
            Node trg = (Node) arc.getTarget();
            System.out.println(src.toString() + " ---"
                    + arc.toString() + "---> "
                    + trg.toString());
        }
    
        e = graphGrammar.getGraph().getNodesSet().iterator();
		while (e.hasNext()) {
			Node node = (Node) e.next();
			if (node.getIncomingArcsSet().isEmpty()
					&& node.getOutgoingArcsSet().isEmpty())
				System.out.println(node.toString());
		}
		System.out.println(" }\n");
    }*/
    
    private void Test() throws Exception
    
    
    
    {	
    	Graph nodeType ;
    	Graph nodeType2 ;
    	
   /* 	Node nl1 = null, nl2 = null, nr1 = null, nr2 = null;    	
    	//String filename2="/Users/Youssef Rebai/eclipse-workspace/CarLogisticsScenario.ggx";
    	String filename="/Users/Youssef Rebai/eclipse-workspace/test.ggx";
    	String filename2="/Users/Youssef Rebai/eclipse-workspace/youssef.ggx";

    	//Rule Rule,Rule1,Rule2 = null;
    	
    	
    	
		//Type arcType = ruleGrammar.createArcType(false);
		
			
			
	//String a,b;
	
	
	  BaseFactory bf2 = BaseFactory.theFactory();
      graphGrammar2 = bf2.createGraGra();
      graphGrammar2.load(filename2);
      
      ruleGrammar2 = bf2.createGraGra();
      ruleGrammar2.load(filename2);
      
      ruleGrammar2 = graphGrammar2;
      graphGrammarTransform(filename2);

      //************************************************************************
        BaseFactory bf = BaseFactory.theFactory();
        graphGrammar = bf.createGraGra();
        graphGrammar.load(filename);
        
        ruleGrammar = bf.createGraGra();
        ruleGrammar.load(filename);
        
        ruleGrammar = graphGrammar;
        graphGrammarTransform(filename);
        //************************************************************************

     /*   Rule= ruleGrammar.getRule(0);
        Rule1= ruleGrammar.getRule(1);
        Rule1= ruleGrammar.getRule(1);*/

		/*Graph left = Rule1.getLeft();
		Graph right = Rule1.getRight();

		Graph left0 = Rule.getLeft();
		Graph right0 = Rule.getRight();*/
		
   		
		/*Type nodeType = ruleGrammar.createNodeType(true);
		Type arcType = ruleGrammar.createArcType(false);

		nodeType.setStringRepr("ACM");
		nodeType.setAdditionalRepr("[NODE]");
        Rule2= ruleGrammar.createRule();      

		arcType.setStringRepr("Link");
		arcType.setAdditionalRepr("[EDGE]");
		    Graph left2 = Rule2.getLeft();
	   		Graph right2= Rule2.getRight();

	   		
	   		nl1 = left2.createNode(nodeType);
			nl2 = left2.createNode(nodeType);
	    	  nr1 = right2.createNode(nodeType);
				nr2 = right2.createNode(nodeType);
			//	right.createArc(arcType, nr1, nr2);

	   		   Rule2.setName("NEW ONE");
	   		System.out.println("Rule2: " + Rule2.getName());*/

    //   if (left.compareTo(left0))
      // {*/

    	   System.out.println("LEFT egaux");
    	   
    	   
    	   
    	   
    	   /*HashSet<Node> a1 =nodeType.getNodesSet();
    	   HashSet<Arc> a3 =nodeType.getArcsSet();

    	   Iterator<Node> i = a1.iterator(); 
    	   Iterator<Arc> i2 = a3.iterator(); 

           while (i2.hasNext()) {
               System.out.println(i2.next().toString()); 

           while (i.hasNext()) {
           System.out.println(i.next().toString()); 
           }}*/
   		/*HashSet<Node> nodes = ruleGrammar.getGraph().getNodesSet();		
        Node e = null;
		nodes.add(e);*/
    	   
    	//   nodeType= graphGrammarTransform2("/Users/Youssef Rebai/eclipse-workspace/test2.ggx");
    	  // nodeType2= graphGrammarTransform2("/Users/Youssef Rebai/eclipse-workspace/test.ggx");

    	   System.out.println("aaa");
    	   //g.merge(nodeType, nodeType2);
    	   creatNewGraph();
           System.out.println("aa");

   		//showGraph();
   		
      // }
    /*   else {
       System.out.println("NO egaux");

       
       }*/
       
       /*Iterator<?> e1 = left.getNodesSet().iterator();
		Iterator<?> e0 = left.getNodesSet().iterator();
		while (e1.hasNext() && e0.hasNext()) {
			GraphObject o = (GraphObject) e1.next();
			GraphObject o = (GraphObject) e1.next();
			if (o.get) {
				result = false;
			}*/
    }


   
    Graph creatNewGraph () throws Exception
    {
    	Graph nodeTy ;
    	Graph nodeTy2 ;
    	List la = new ArrayList();
    	 
    	  nodeTy= graphGrammarTransform2("/Users/Youssef Rebai/eclipse-workspace/test2.ggx");
    	   nodeTy2= graphGrammarTransform2("/Users/Youssef Rebai/eclipse-workspace/test.ggx");

    	   System.out.println("aaa");
    	    la =g.merge(nodeTy, nodeTy2);
    	    
            LinkedHashSet<String> listToSet = new LinkedHashSet<String>(la);
    	    List<String> listWithoutDuplicates = new ArrayList<String>(listToSet);

  		graphGrammar = this.bf.createGraGra();

  		System.out.println("Create graphGrammar  <Fusion> ...");
		graphGrammar = this.bf.createGraGra();
		graphGrammar.setName("Fusion");

		// Start graph
		Graph graph = graphGrammar.getGraph();
		graph.setName("HostGraph");
//		String graphName = graph.getName();
		/** Typing of graphGrammar */
		// Create a type with attr for nodes
		// Create a type without attr for edges
		
		
		 
		//System.out.println(listWithoutDuplicates);

			for (int i = 4; i < la.size();) {
	  	
	    	  String a=	la.get(i).toString();
	   
	    	  
	    	  Type nodeType1 = graphGrammar.createNodeType(true);

	    			System.out.println(a);
			nodeType1.setStringRepr(a);
			nodeType1.setAdditionalRepr("[NODE]");	 
			
			 i++ ;
	    	    
	       }
			for (int i = 1; i < 4;) {
			  	
		    	  String a=	la.get(i).toString();
		   
		    	  

		    			System.out.println(a);
		    			Type arcType = graphGrammar.createArcType(false);

		    			arcType.setStringRepr(a);
		    		    arcType.setAdditionalRepr("[EDGE]");

				 i++ ;
		    	    
		       }
			
			
			
		String fn = "ac.ggx";
		 
		graphGrammar.save(fn);
		
	   
	   return graph;
    	 /*  nodeType= graphGrammarTransform2("/Users/Youssef Rebai/eclipse-workspace/test2.ggx");
    	   nodeType2= graphGrammarTransform2("/Users/Youssef Rebai/eclipse-workspace/test.ggx");

    	   System.out.println("aaa");
    	    l =g.merge(nodeType, nodeType2);*/
    	   
    	   
    	   //Graph fusion=ruleGrammar.getGraph();
			  
			/*System.out.println("Create ruleGrammar  FUSION ...");
			ruleGrammar = bf.createGraGra();
			ruleGrammar.setName("FUSION");

			/// Typing of ruleGrammar 
			//Type nodeType = ruleGrammar.createNodeType(true);
			Type arcType = ruleGrammar.createArcType(false);
		
		    // for (int counter = 0; counter < l.size(); counter++) { 		      

			arcType.setStringRepr(l.toString());
			arcType.setAdditionalRepr("[EDGE]");   
			
			
		   //   }*/
		
    	   
    	
    	
    }


    private void graphGrammarTransform(String filename) {
        Pair<Object, String> pair = graphGrammar.isReadyToTransform(true);
        Object test = null;
        if (pair != null)
            test = pair.first;
        if (test != null) {
            if (test instanceof Graph) {
                System.out.println("Grammar  " + graphGrammar.getName()
                        + "  graph: " + graphGrammar.getGraph().getName()
                        + "  is not ready for transform");
            } else if (test instanceof AtomConstraint) {
                String s0 = "Atomic graph constraint  \""
                        + ((AtomConstraint) test).getAtomicName()
                        + "\" is not valid. "
                        + "\nPlease check: "
                        + "\n  - graph morphism ( injective and total )  "
                        + "\n  - attribute context ( variable and condition declarations ).";
                System.out.println(s0);
            } else if (test instanceof Rule) {
                String s0 = "Rule  \""
                        + ((Rule) test).getName()
                        + "\" : "
                        + ((Rule) test).getErrorMsg()
                        + "\nPlease check: \n  - attribute settings of the new objects of the RHS \n  - attribute context ( variable and condition declarations ) of this rule.\nThe grammar is not ready to transform.";
                System.out.println(s0);
            }
            System.out.println("Grammar  " + graphGrammar.getName()
                    + "  CANNOT TRANSFORM!");
            return;
        }
        if(DEBUG) {
            System.out.println("Grammar  " + graphGrammar.getName() + "  is ready to transform");
            System.out.println("Matching and graph transformation ");
        }

        // Get all completion strategies
        // Enumeration<MorphCompletionStrategy> strategies = CompletionStrategySelector.getStrategies();

        // default strategy is injective, with dangling condition (DPO), with
        // NACs.
        if(DEBUG) {
            System.out.println(this.strategy);
            this.strategy.showProperties();
        }

        /* an example to set / clear strategy properties */
        /*
         * BitSet activebits = strategy.getProperties();
         * activebits.clear(CompletionPropertyBits.INJECTIVE);
         * activebits.clear(CompletionPropertyBits.DANGLING);
         * activebits.clear(CompletionPropertyBits.IDENTIFICATION);
         * activebits.clear(CompletionPropertyBits.NAC);
         * System.out.println(strategy.getProperties());
         * strategy.showProperties();
         * activebits.set(CompletionPropertyBits.INJECTIVE);
         * activebits.set(CompletionPropertyBits.DANGLING); //
         * activebits.set(CompletionPropertyBits.IDENTIFICATION);
         * activebits.set(CompletionPropertyBits.NAC);
         * System.out.println(strategy.getProperties());
         * strategy.showProperties();
         */

        // Set graph transformation options
        Vector<String> gratraOptions = new Vector<String>();
        gratraOptions.add("CSP");
        gratraOptions.add("injective");
        gratraOptions.add("dangling");
        gratraOptions.add("NACs");
        gratraOptions.add("PACs");
        gratraOptions.add("GACs");
        gratraOptions.add("consistency");
        graphGrammar.setGraTraOptions(gratraOptions);

        // Set file name and save grammar
        String fn = filename + "-output.ggx";
        graphGrammar.save(fn);

        if(DEBUG) {
            System.out.println("Grammar " + graphGrammar.getName() + " saved in " + fn);
            System.out.println("Continue ...");
        }

        Match match = null;
        
        /***************************************************************************/
/********************************************************************************************/
        // an example to applay a rule
        List<Rule> rules = graphGrammar.getListOfRules();

        boolean[] done = new boolean[rules.size()];
        Arrays.fill(done, true);
        for (int i = 0; i < rules.size(); i++) {
            // an example to apply rule2
            Rule rule2 = rules.get(i);

            if(DEBUG) {
                System.out.println("Apply  rule2  " + rule2.getName() + " so long as possible");
                System.out.println("Rule2  " + rule2.getName() + "    >> create match");
            }

            match = graphGrammar.createMatch(rule2);
            match.setCompletionStrategy(this.strategy, true);
            try {
                while (match.nextCompletion()) {
                    done[i] = false;
                    //showGraph(graphGrammar);

                    /*
                     * test output of attribute conditions: ac =
                     * match.getAttrContext(); ContextView cv = (ContextView) ac;
                     * AttrConditionTuple condTuple = (AttrConditionTuple)
                     * cv.getConditions(); for (int i = 0; i <
                     * condTuple.getNumberOfEntries(); i++) { AttrConditionMember cm =
                     * (AttrConditionMember) condTuple.getMemberAt(i);
                     * System.out.println("Condition "+i+": name="+cm.getName()+"
                     * val="+cm.getExprAsText()); } System.out.println("Condition is
                     * satisfied :" + condTuple.isTrue());
                     */

                    /*
                     * test output of variables with its values for (int i = 0; i <
                     * ac.getVariables().getNumberOfEntries(); i++) { am =
                     * (AttrInstanceMember) ac.getVariables().getMemberAt(i);
                     * System.out.println("Variable "+i+": name="+am.getName()+"
                     * value="+am.getExprAsText()); }
                     */
                    if (DEBUG) {
                        System.out.println("Rule : match is complete");
                    }
                    if (match.isValid()) {
                        if (DEBUG) {
                            System.out.println("Rule :  match is valid");
                        }
//					Step step = new Step();
                        try {
                            StaticStep.execute(match);
                            if (DEBUG) {
                                System.out.println("Rule  " + match.getRule().getName() + " : step is done");
                            }
                        } catch (TypeException ex) {
                            ex.printStackTrace();
                            graphGrammar.destroyMatch(match);
                            if (DEBUG) {
                                System.out.println("Rule  " + match.getRule().getName() + " : match failed! " + ex.getMessage());
                            }
                        }
                    } else {
                        if (DEBUG) {
                            System.out.println("Rule  " + match.getRule().getName() + " : match is not valid");
                        }
                    }
                }
            } catch(Exception e){
                System.err.println("Exception processing some match; skipping it");
                //done[i] = true;
            }

            if(DEBUG) {
                System.out.println("Rule  " + match.getRule().getName()+ " : match has no more completion");
            }

            graphGrammar.destroyMatch(match);
            //showGraph(graphGrammar);
            graphGrammar.save(fn);

            if(DEBUG) {
                System.out.println("After apply rule2  graphGrammar  saved in  " + fn);
            }

            // shitty looping
            if(i==rules.size()-1) {
                if(!doneDoingShit(done)){
                    i=-1;
                    Arrays.fill(done, true);
                }
            }
        }


        //Rule rule3 = rules.get(2);

        // Create a transformation unit

        // if you want to use rule layers (layered graph transformation)
        // add option "layered".
        gratraOptions.add("layered");
        graphGrammar.setGraTraOptions(gratraOptions);
        // Set layer : rule1 "NewPerson" layer 0
        // rule2 "SetRelation" layer 2
        // rule3 "RemoveRelation" layer 1

        //rule2.setLayer(2);
        //rule3.setLayer(1);
    }
    boolean doneDoingShit(boolean[] a){
        boolean ret = true;
        for(boolean ba : a){
            ret &= ba;
        }
        if(DEBUG) {
            System.out.println("AB");
            System.out.println(Arrays.toString(a));
            System.out.println(ret);
        }
        return ret;
    }
    



    private Graph graphGrammarTransform2(String filename) throws Exception {
    	   BaseFactory bf = BaseFactory.theFactory();
           graphGrammar = bf.createGraGra();
           graphGrammar.load(filename);
           
           ruleGrammar = bf.createGraGra();
           ruleGrammar.load(filename);
           
           ruleGrammar = graphGrammar;
    	Graph g = ruleGrammar.getGraph();
    	
    	
        Pair<Object, String> pair = graphGrammar.isReadyToTransform(true);
        Object test = null;
       
        // Set graph transformation options
        Vector<String> gratraOptions = new Vector<String>();
        gratraOptions.add("CSP");
        gratraOptions.add("injective");
        gratraOptions.add("dangling");
        gratraOptions.add("NACs");
        gratraOptions.add("PACs");
        gratraOptions.add("GACs");
        gratraOptions.add("consistency");
        graphGrammar.setGraTraOptions(gratraOptions);

        // Set file name and save grammar
      

        Match match = null;
        /***************************************************************************/
/********************************************************************************************/
        // an example to applay a rule
        List<Rule> rules = graphGrammar.getListOfRules();

        boolean[] done = new boolean[rules.size()];
        Arrays.fill(done, true);
        for (int i = 0; i < rules.size(); i++) {
            // an example to apply rule2
            Rule rule2 = rules.get(i);

            if(DEBUG) {
                System.out.println("Apply  rule2  " + rule2.getName() + " so long as possible");
                System.out.println("Rule2  " + rule2.getName() + "    >> create match");
            }
            String fn = filename + "-output.ggx";
            graphGrammar.save(fn);

            if(DEBUG) {
                System.out.println("Grammar " + graphGrammar.getName() + " saved in " + fn);
                System.out.println("Continue ...");
            }
            match = graphGrammar.createMatch(rule2);
            match.setCompletionStrategy(this.strategy, true);
            try {
                while (match.nextCompletion()) {
                    done[i] = false;
                    
                    
                    if (DEBUG) {
                        System.out.println("Rule : match is complete");

                    }
                    if (match.isValid()) {
                        if (DEBUG) {
                            System.out.println("Rule :  match is valid");
                        }
//					Step step = new Step();
                        try {
                            StaticStep.execute(match);
                            if (DEBUG) {
                                System.out.println("Rule  " + match.getRule().getName() + " : step is done");
                            }
                        } catch (TypeException ex) {
                            ex.printStackTrace();
                            graphGrammar.destroyMatch(match);
                            if (DEBUG) {
                                System.out.println("Rule  " + match.getRule().getName() + " : match failed! " + ex.getMessage());
                            }
                        }
                    } else {
                        if (DEBUG) {
                            System.out.println("Rule  " + match.getRule().getName() + " : match is not valid");
                        }
                    }
                }
            } catch(Exception e){
                System.err.println("Exception processing some match; skipping it");
                //done[i] = true;
            }

            if(DEBUG) {
                System.out.println("Rule  " + match.getRule().getName()+ " : match has no more completion");
            }

            //graphGrammar.destroyMatch(match);
            //showGraph(graphGrammar);
          graphGrammar.save(fn);

            if(DEBUG) {
                System.out.println("After apply rule2  graphGrammar  saved in  " + fn);
            }

            // shitty looping
            if(i==rules.size()-1) {
                if(!doneDoingShit(done)){
                    i=-1;
                    Arrays.fill(done, true);
                }
            }
        }


        
        return g;
    }
    
}
