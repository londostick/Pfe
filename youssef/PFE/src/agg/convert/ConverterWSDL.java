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
import java.io.IOException;

import agg.util.XMLHelper;
import agg.xt_basis.GraGra;
import agg.xt_basis.TypeException;
import agg.convert.WSDL2ggx;


public class ConverterWSDL {

	//private static final String filePath = "C:\\localapp\\aggEngine_V202\\Examples_V164\\BasisUsing\\btsICGT6.ggx";
	//private static final String dataPath = "C:\\localapp\\";
	
//	private static final String filePath = "C:\\Users\\olga\\Desktop\\Tamim\\Analysis\\exmpls\\BTSysService.wsdl";
//	private static final String dataPath = "C:\\Users\\olga\\Desktop\\Tamim\\Analysis\\exmpls\\";


	private XMLHelper xmlH;
	private GraGra gragra;
	private String fileName;
	private String outputFileName;
	private WSDL2ggx wsdl2agg;
	
	public ConverterWSDL(String filename) throws IOException, TypeException {
		fileName = filename;
		System.out.println("File name:  " + fileName);
		if (load(fileName)
				&& convert()) {
			outputFileName = fileName.replace(".wsdl", ".ggx");
			gragra.save(outputFileName);
		}
	}
	
	public GraGra getGrammar() {
		return this.gragra;
	}
	
	
 	boolean load(String fName) {
		if (fName.endsWith(".wsdl")) {
			xmlH = new XMLHelper();
			return xmlH.read_from_xml(fName);
		} 
		return false;
	}
	
	boolean convert() {
		// create a new gragra with empty TypeGraph and a host graph
		gragra =  new GraGra(true);
		gragra.getTypeSet().createTypeGraph();
		
		wsdl2agg = new WSDL2ggx(gragra);				
		xmlH.getTopObject(wsdl2agg);
		return 	wsdl2agg.isSuccessful();	
	}
	
	public static void main(String[] args) throws IOException, TypeException {
		String arg = args[0];
//		String arg = filePath;
		new ConverterWSDL(arg);
	}
	
}
