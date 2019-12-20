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

import java.io.File;

import agg.util.XMLHelper;
import agg.convert.ConverterXML;

public class AGGBasicAppl implements GraTraEventListener {

	private static GraGra gragra;

	private static Graph impGraph;

	private static GraTra gratra;

	static int NN;
	
	private int msgGraTra;

	private static boolean layered = false, ruleSequence = false,
			priority = false;;

	private static boolean didTransformation = false;

	private static String fileName;

	private static String impFileName;

	private static String outputFileName;

	private static String error;

	private static boolean writeLogFile = false;

	public AGGBasicAppl() {
	}

	public AGGBasicAppl(String filename) {
		fileName = filename;
		System.out.println("File name:  " + fileName);
		/* load gragra */
		gragra = load(fileName);

		// or so:
		// gragra = BaseFactory.theFactory().createGraGra();
		// gragra.load(fileName);

		if (gragra != null) {
			gragra.getLevelOfTypeGraphCheck();
			/* do transform */
			transform(gragra, this);

			if (didTransformation) {
				/* save gragra */
				String out = "_out.ggx";

				save(gragra, out);

				// or so:
				// gragra.save(out);

				System.out.println("Output file:  " + outputFileName);

				if (writeLogFile) {

					if (gratra instanceof DefaultGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((DefaultGraTraImpl) gratra)
										.getProtocolName());
					else if (gratra instanceof PriorityGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((PriorityGraTraImpl) gratra)
										.getProtocolName());
					else if (gratra instanceof LayeredGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((LayeredGraTraImpl) gratra)
										.getProtocolName());
					else if (gratra instanceof RuleSequencesGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((RuleSequencesGraTraImpl) gratra)
										.getProtocolName());

				}
			
			} else {
				System.out.println("Grammar:  " + gragra.getName()
						+ "   could not perform any transformations!");
			}
			
			gratra.dispose();
			BaseFactory.theFactory().destroyGraGra(gragra);
			gragra = null;
			gratra = null;		
		} else
			System.out.println("Grammar:  " + filename + "   FAILED!");
	}

	public AGGBasicAppl(String filename, int nn) {
		NN = nn;	
		
		fileName = filename;
		System.out.println("File name: " + fileName+"   iterations: "+NN);
		/* load gragra */
		gragra = load(fileName);

		// or so:
		// gragra = BaseFactory.theFactory().createGraGra();
		// gragra.load(fileName);

		if (gragra != null) {
			gragra.getLevelOfTypeGraphCheck();
			/* do transform */
			transform(gragra, this);

			if (didTransformation) {
				/* save gragra */
				String out = "_out.ggx";

				save(gragra, out);

				// or so:
				// gragra.save(out);

				System.out.println("Output file:  " + outputFileName);

				if (writeLogFile) {

					if (gratra instanceof DefaultGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((DefaultGraTraImpl) gratra)
										.getProtocolName());
					else if (gratra instanceof PriorityGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((PriorityGraTraImpl) gratra)
										.getProtocolName());
					else if (gratra instanceof LayeredGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((LayeredGraTraImpl) gratra)
										.getProtocolName());
					else if (gratra instanceof RuleSequencesGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((RuleSequencesGraTraImpl) gratra)
										.getProtocolName());
				}
			
			} else {
				System.out.println("Grammar:  " + gragra.getName()
						+ "   could not perform any transformations!");
			}
			
			gratra.dispose();
			BaseFactory.theFactory().destroyGraGra(gragra);
			gragra = null;
			gratra = null;		
		} else
			System.out.println("Grammar:  " + filename + "   FAILED!");
	}
	
	
	public AGGBasicAppl(String filename, String impFilename) {
		fileName = filename;
		impFileName = impFilename;
		System.out.println("File name:  " + fileName);
		gragra = load(fileName);
		if (gragra != null) {
			int levelOfTGcheck = gragra.getLevelOfTypeGraphCheck();
			System.out.println("Import file name:  " + impFileName);
			impGraph = importGraph(impFilename);
			if (impGraph != null) {
				gragra.setLevelOfTypeGraphCheck(TypeSet.DISABLED);
				// System.out.println("0:: "+impGraph.attributesToString());

				if (!gragra.importGraph(impGraph)) {
					System.out
							.println("Error:  Import graph has failed! Please check types of the import. ");
					return;
				}
				// System.out.println("2:: "+impGraph.attributesToString());
				System.out.println("Importing graph was successful.");

				if (gragra.getTypeSet().hasInheritance()) {
					if (levelOfTGcheck != TypeSet.DISABLED)
						gragra.setLevelOfTypeGraphCheck(levelOfTGcheck);
					else
						gragra.setLevelOfTypeGraphCheck(TypeSet.ENABLED);
				} else
					gragra.setLevelOfTypeGraphCheck(levelOfTGcheck);
				// System.out.println("LevelOfTypeGraphCheck:
				// "+gragra.getLevelOfTypeGraphCheck());

				// save current grammar with imported graph
				save(gragra, "Impot_" + fileName);
				System.out.println("Import is written into:  " + "Impot_"
						+ fileName);

				/* do transform */
				transform(gragra, this);
				if (didTransformation) {
					/* save gragra */
					String out = "_out.ggx";
					/*
					 * File f = new File(fileName); if(f.exists()){
					 * if(f.getParent() != null) out =
					 * f.getParent()+File.separator+out; else out =
					 * "."+File.separator+out; }
					 */

					save(gragra, out);

					// or so:
					// gragra.save(out);
					System.out.println("Output file:  " + outputFileName);
					if (gratra instanceof DefaultGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((DefaultGraTraImpl) gratra)
										.getProtocolName());
					else if (gratra instanceof PriorityGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((PriorityGraTraImpl) gratra)
										.getProtocolName());
					else if (gratra instanceof LayeredGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((LayeredGraTraImpl) gratra)
										.getProtocolName());
					else if (gratra instanceof RuleSequencesGraTraImpl)
						System.out.println("Transformation protocol: "
								+ ((RuleSequencesGraTraImpl) gratra)
										.getProtocolName());
					
				} else {
					System.out.println("Grammar:  " + gragra.getName()
							+ "   could not perform any transformations!");
				}
				
				gratra.dispose();
				BaseFactory.theFactory().destroyGraGra(gragra);
				gragra = null;
				gratra = null;		
				
			} else
				System.out.println(impFilename + "   FAILED! "+error);
		} else
			System.out.println("Grammar:  " + filename + "   FAILED!");
	}

	public static void main(String[] args) {
		
		final String ver = agg.xt_basis.Version.getID();
		
		System.out.println(
				">>> This AGG version 2.1 \n"+
				"    is made available under the terms of the Eclipse Public License v1.0 \n" +
				"    which accompanies this distribution, and is available at \n" +
				"    http://www.eclipse.org/legal/.");
		System.out.println(">>> ");
		System.out.println(">>> Java Version "+System.getProperty("java.version"));
		System.out.println(">>> AGG Version "+ver+" runs under JVM 1.8.0_60 and higher.");
		
		
		if (args.length == 0) {
			warning();
			return;
		}

		if (args.length == 1) {
			if ((args[0]).compareToIgnoreCase("-logfile") != 0) {
				new AGGBasicAppl(args[0]);
				writeLogFile = false;
			}
		} else if (args.length == 2) {
			if (args[0].compareToIgnoreCase("-logfile") == 0) {
				writeLogFile = true;
				new AGGBasicAppl(args[1]);
			} else {	
				int iters = 0;
				try {
					iters = (Integer.valueOf(args[0])).intValue();
					new AGGBasicAppl(args[1], iters);
				} catch (NumberFormatException ex) {
					try {
						iters = (Integer.valueOf(args[1])).intValue();
						new AGGBasicAppl(args[0], iters);
					} catch (NumberFormatException ex1) {
						iters = 0;
					}
				}
				if (iters ==0) {				
					new AGGBasicAppl(args[0], args[1]);
				}
			}
		} else if (args.length == 3) {
			if (args[0].compareToIgnoreCase("-logfile") == 0) {
				writeLogFile = true;
				new AGGBasicAppl(args[1], args[2]);
			} else
				warning();
		}
		
	}

	static void warning() {
		System.out
				.println("Usage unaliased: java -oss3m -Xmx1000m agg.xt_basis.AGGBasicAppl [-logfile] grammar [import]");
		System.out.println("Usage aliased:");
		System.out.println("aggengine [-logfile] grammar [import]");
		System.out
				.println("(aggengine:     aliased to java -oss3m -Xmx1000m agg.xt_basis.AGGBasicAppl)");
		System.out.println("");
		System.out.println("  -logfile \twrite transformation logfile");
		System.out
				.println("  grammar \tfull file name of  '.ggx'  file in XML format");
		System.out
				.println("  import	\tfull file name of a GXL file '.gxl' in XML format \n\t\tthat contains the graph to import.");
		System.out.println("also possible");
		System.out
				.println("  import	\tfull file name of an OMONDO XMI file '.ecore' in XML format \n\t\tthat contains the graph to import.");
		System.out.println("");
	}

	static XMLHelper h;

	public static GraGra load(String fName) {
		// System.out.println(fileName.endsWith(".ggx"));
		if (fName.endsWith(".ggx")) {
			h = new XMLHelper();
			/*
			 * if(XMLHelper.hasGermanSpecialCh(fileName)){
			 * System.out.println("\nRead file name exception occurred! "
			 * +"\nMaybe the German umlaut like ä, ö, ü or ß were used. "
			 * +"\nPlease replace it by ae, oe, ue or ss " +"\nand try again.");
			 * return null; }
			 */
			if (h.read_from_xml(fName)) {

				// create a gragra
				GraGra gra = BaseFactory.theFactory().createGraGra(false);
				
				h.getTopObject(gra);
				gra.setFileName(fName);
				// System.out.println("BF.getGraGras():: count:
				// "+BaseFactory.theFactory().getGraGras());
				return gra;
			} 
			return null;
		} 
		return null;
	}

	public static Graph importGraph(String filename) {
		// System.out.println(filename.endsWith(".gxl"));
		if (filename.endsWith(".ggx"))
			return importGraphGGX(filename);
		else if (filename.endsWith(".gxl"))
			return importGraphGXL(filename);
		else if (filename.endsWith(".gtxl"))
			return importGraphGTXL(filename);
		else if (filename.endsWith(".ecore"))
			return importGraphOMONDO_XMI(filename);
		else {
			error = "Import failed!   < "
					+ filename
					+ " >  should be < .ggx > , < .gxl >  or  < .ecore >  file.";
			return null;
		}
	}

	private static Graph importGraphGGX(String filename) {
		// System.out.println("importGraphGGX: "+filename);
		GraGra impGra = load(filename);
		if (impGra != null) {
			// save(impGra, "_outImportGrammar.ggx"); //test
			return impGra.getGraph();
		} 
		return null;
	}

	private static Graph importGraphGTXL(String filename) {
		return null;
	}

	private static Graph importGraphGXL(String filename) {
		String fd = ".";
		String fn = filename;
		String fnOut = "";
		File gxldtd = null;
		File gtsdtd = null;
		File source = null;
		File layout = null;
		error = "";

		File f = new File(fn);
		if (f.exists()) {
			if (f.isFile())
				fd = f.getParent();
		}
		if (fd != null)
			fd = fd + File.separator;
		else
			fd = "." + File.separator;
		// System.out.println("dir: "+fd);
		// System.out.println("file: "+fn);

		/*
		 * if(XMLHelper.hasGermanSpecialCh(fn)){ System.out.println("File name:
		 * "+fn); System.out.println("\nRead file name exception occurred! "
		 * +"\nMaybe the German umlaut like ä, ö, ü or ß were used. " +"\nPlease
		 * replace it by ae, oe, ue or ss " +"\nand try again."); return null; }
		 */

		ConverterXML converter = new ConverterXML();
		fnOut = fn.substring(0, fn.length() - 4) + "_gxl.ggx";
		source = converter.copyFile(fd, "gxl2ggx.xsl");
		gxldtd = converter.copyFile(fd, "gxl.dtd");
		gtsdtd = converter.copyFile(fd, "gts.dtd");
		layout = converter.copyFile(fd, "agglayout.dtd");
		if (source == null) {
			error = "Import is failed! File   < gxl2ggx.xsl >  is not found.";
			return null;
		} else if (gxldtd == null) {
			error = "Import is failed! File   < gxl.dtd >  is not found.";
			return null;
		} else if (gtsdtd == null) {
			error = "Import is failed! File   < gts.dtd >  is not found.";
			return null;
		} else if (layout == null) {
			error = "Import is failed! File   < agglayout.dtd >  is not found.";
			return null;
		}

		String in = fn;
		String out = fnOut;
		GraGra impGra = null;
		if (converter.gxl2ggx(in, out, fd + "gxl2ggx.xsl")) {
			if (out.endsWith(".ggx")) {
				h = new XMLHelper();
				if (h.read_from_xml(out))
					impGra = (GraGra) h.getTopObject(
							BaseFactory.theFactory().createGraGra());

				if (impGra != null)
					return impGra.getGraph();
			}
		}
		error = "Import is failed! Please check format of the  GXL  file.";
		return null;
	}

	private static Graph importGraphOMONDO_XMI(String filename) {
		String fd = ".";
		String fn = filename;
		String fnOut = "";
		File gxldtd = null;
		File gtsdtd = null;
		File source = null;
		File layout = null;
		File omondo = null;
		error = "";

		File f = new File(fn);
		if (f.exists()) {
			if (f.isFile())
				fd = f.getParent();
		}
		if (fd != null)
			fd = fd + File.separator;
		else
			fd = "." + File.separator;
		// System.out.println("dir: "+fd);
		// System.out.println("file: "+fn);

		ConverterXML converter = new ConverterXML();

		fnOut = fn.substring(0, fn.length() - 6) + "_ecore.ggx";
		source = converter.copyFile(fd, "gxl2ggx.xsl");
		gxldtd = converter.copyFile(fd, "gxl.dtd");
		gtsdtd = converter.copyFile(fd, "gts.dtd");
		layout = converter.copyFile(fd, "agglayout.dtd");
		omondo = converter.copyFile(fd, "omondoxmi2gxl.xsl");

		if (source == null) {
			error = "Import failed! File   < gxl2ggx.xsl >  is not found.";
			return null;
		} else if (gxldtd == null) {
			error = "Import failed! File   < gxl.dtd >  is not found.";
			return null;
		} else if (gtsdtd == null) {
			error = "Import failed! File   < gts.dtd >  is not found.";
			return null;
		} else if (layout == null) {
			error = "Import failed! File   < agglayout.dtd >  is not found.";
			return null;
		} else if (omondo == null) {
			error = "Import failed! File   < omondoxmi2gxl.xsl >  is not found.";
			return null;
		}

		String in = fn;
		String out = fnOut;
		GraGra impGra = null;
		if (converter.omondoxmi2ggx(in, out, fd + "omondoxmi2gxl.xsl", fd
				+ "gxl2ggx.xsl")) {
			if (out.endsWith(".ggx")) {
				h = new XMLHelper();
				if (h.read_from_xml(out))
					impGra = (GraGra) h.getTopObject(
							BaseFactory.theFactory().createGraGra());

				if (impGra != null) {
					return impGra.getGraph();
				}
			}
		}
		error = "Import failed! Please check format of the  GXL  file.";
		return null;
	}

	public static void transform(GraGra grammar, GraTraEventListener l) {
		if (grammar == null)
			return;
		/*
		 * test: there is a one way to set transformation options Vector gto =
		 * new Vector(); gto.add("layered"); gto.add("CSP");
		 * gto.add("injective"); gto.add("dangling");
		 * gragra.setGraTraOptions(gto);
		 */

		// create trafo
		// System.out.println(gragra.getGraTraOptions().toString());
		if (grammar.getGraTraOptions().contains("priority")) {
			gratra = new PriorityGraTraImpl();
			priority = true;
			System.out.println("Transformation by rule priority ...");
		} else if (grammar.getGraTraOptions().contains("layered")) {
			gratra = new LayeredGraTraImpl();
			layered = true;
			System.out.println("Layered transformation ...");
		} else if (grammar.getGraTraOptions().contains("ruleSequence")) {
			gratra = new RuleSequencesGraTraImpl();
			ruleSequence = true;
			System.out.println("Transformation by rule sequences ...");
		} else {
			gratra = new DefaultGraTraImpl();
			((DefaultGraTraImpl) gratra).setMaxOfCounter(NN);
			System.out.println("Transformation  non-deterministically ...");
		}

		gratra.addGraTraListener(l);
		gratra.setGraGra(grammar);
		gratra.setHostGraph(grammar.getGraph());
		gratra.enableWriteLogFile(writeLogFile);

		MorphCompletionStrategy strategy = CompletionStrategySelector
				.getDefault();
		// strategy = new Completion_NAC(new Completion_InjCSP());

		if (grammar.getGraTraOptions().isEmpty()) {
			grammar.setGraTraOptions(strategy);
			gratra.setCompletionStrategy(strategy);
		} else {
			if (grammar.getGraTraOptions().contains("showGraphAfterStep"))
				grammar.getGraTraOptions().remove("showGraphAfterStep");
			gratra.setGraTraOptions(grammar.getGraTraOptions());
			System.out.println("Options:  " + grammar.getGraTraOptions());
			System.out.println();
		}

		grammar.destroyAllMatches();

		if (priority)
			((PriorityGraTraImpl) gratra).transform();
		else if (layered)
			((LayeredGraTraImpl) gratra).transform();
		else if (ruleSequence)
			((RuleSequencesGraTraImpl) gratra).transform();
		else
			((DefaultGraTraImpl) gratra).transform();
	}

	public static void save(GraGra gra, String outFileName) {
		// System.out.println("Output into: "+outFileName);
		if (outFileName.equals(""))
			outputFileName = gra.getName() + "_out.ggx";
		else if (outFileName.equals("_out.ggx"))
			outputFileName = fileName.substring(0, fileName.length() - 4)
					+ "_out.ggx";
		else if (outFileName.indexOf(".ggx") == -1)
			outputFileName = outFileName.concat(".ggx");
		else if (outFileName.equals(fileName))
			outputFileName = fileName.substring(0, fileName.length() - 4)
					+ "_out.ggx";
		else {
			outputFileName = outFileName;
		}
		// System.out.println("save :: Output into: "+outputFileName);
		if (outputFileName.endsWith(".ggx")) {
			XMLHelper xmlh = new XMLHelper();
			xmlh.addTopObject(gra);
			xmlh.save_to_xml(outputFileName);
		}
	}

	/** Implements GraTraEventListener.graTraEventOccurred */
	public void graTraEventOccurred(GraTraEvent event) {
		// System.out.println("AGGBasicAppl.graTraEventOccurred
		// "+event.getMessage());
//		Match match = event.getMatch();
		
//		String ruleName = "Rule";
//		if (match != null)
//			ruleName = match.getRule().getName();

		this.msgGraTra = event.getMessage();
		if (this.msgGraTra == GraTraEvent.TRANSFORM_FINISHED) {
			gratra.stop();
			didTransformation = gratra.transformationDone();
			// System.out.println("GraTraEvent message : TRANSFORM_FINISHED");
		} else if ((this.msgGraTra == GraTraEvent.INPUT_PARAMETER_NOT_SET)) {
			System.out.println("GraTraEvent message : PARAMETER NOT SET!");
		} else if((this.msgGraTra == GraTraEvent.STEP_COMPLETED)) { //
//			System.out.println("Rule : "+ruleName+" ==> STEP DONE" ); 
//			if (match != null)
//				match.getCoMorphism().dispose();
		}
		 /* 
		 * else if (msgGraTra == GraTraEvent.NO_COMPLETION) {
		 * //System.out.println("Rule : "+ ruleName+" ==> NO_COMPLETION"); }
		 * else if (msgGraTra == GraTraEvent.CANNOT_TRANSFORM){
		 * //System.out.println("Rule : "+ ruleName+" ==> CANNOT_TRANSFORM"); }
		 */
	}

}
