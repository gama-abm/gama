/*********************************************************************************************
 * 
 *
 * 'TypeConverter.java', in plugin 'msi.gama.processor', is part of the source code of the 
 * GAMA modeling and simulation platform.
 * (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 * 
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 * 
 * 
 **********************************************************************************************/
package msi.gama.precompiler.doc.utils;

import java.util.HashMap;

import msi.gama.precompiler.IOperatorCategory;

public class TypeConverter {
	
	HashMap<String, String> properNameTypeMap;
	HashMap<String, String> properCategoryNameMap;
	HashMap<Integer, String> typeStringFromIType;
	HashMap<Integer, String> symbolKindStringFromISymbolKind;
	
	public TypeConverter(){
		properNameTypeMap = initProperNameTypeMap();
		properCategoryNameMap = initProperNameCategoriesMap();
		typeStringFromIType = initNameTypeFromIType();
		symbolKindStringFromISymbolKind = initSymbolKindStringFromISymbolKind();
	}

	private HashMap<Integer, String> initSymbolKindStringFromISymbolKind() {
		HashMap<Integer, String> hm = new HashMap<Integer, String>();
		hm.put(0, "species");
		hm.put(1, "model");
		hm.put(2, "single_statement");
		hm.put(3, "behavior");
		hm.put(4, "parameter");
		hm.put(5, "output");
		hm.put(6, "layer");
		hm.put(7, "skill");
		hm.put(8, "batch_section");
		hm.put(9, "batch_method");
		hm.put(10, "environment");
		hm.put(11, "sequence_statement or action");
		hm.put(13, "experiment");
		hm.put(14, "abstract_section");
		
		return hm;
	}
	
	private HashMap<String, String> initProperNameTypeMap() {
		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put("msi.gama.metamodel.shape.IShape", "geometry");
		hm.put("msi.gama.metamodel.shape.GamaShape", "geometry");
		
		hm.put("java.lang.Integer", "int");
		hm.put("java.lang.Double", "float");	
		hm.put("java.lang.Long", "float");		
		// Matrix
		hm.put("msi.gama.util.matrix.IMatrix<T>", "matrix");
		hm.put("msi.gama.util.matrix.IMatrix", "matrix");
		hm.put("msi.gama.util.matrix.GamaMatrix", "matrix");		
		hm.put("msi.gama.util.matrix.GamaIntMatrix", "matrix");
		hm.put("msi.gama.util.matrix.GamaMatrix<T>", "matrix");
		hm.put("msi.gama.util.matrix.GamaMatrix<java.lang.Double>", "matrix<float>");
		// Files
		hm.put("msi.gama.util.file.IGamaFile", "file");
		hm.put("msi.gama.util.file.GamaFile", "file");		
		hm.put("msi.gama.jogl.files.Gama3DSFile", "file");
		hm.put("msi.gama.jogl.files.GamaObjFile", "file");
		// Colors
		hm.put("msi.gama.util.GamaColor", "rgb");
		// List
		hm.put("msi.gama.util.IList", "list");
		hm.put("msi.gama.util.GamaList", "list");
		hm.put("java.util.List", "list");
		hm.put("java.util.List<T>", "list");
		hm.put("msi.gama.util.IList<T>", "list");
		hm.put("msi.gama.util.IList<java.lang.Object>", "list");
		hm.put("java.util.List<java.lang.Object>", "list");
		hm.put("msi.gama.util.IList<msi.gama.util.IList<T>>", "list<list>");
		hm.put("java.util.List<java.util.List>", "list<list>");
		hm.put("msi.gama.util.IList<msi.gama.metamodel.shape.IShape>", "list<geometry>");
		hm.put("msi.gama.util.IList<msi.gama.metamodel.shape.GamaPoint>", "list<point>");
		hm.put("msi.gama.util.IList<msi.gama.metamodel.agent.IAgent>", "list<agent>");
		hm.put("java.util.List<java.util.List<msi.gama.metamodel.agent.IAgent>>", "list<list<agent>>");
		hm.put("msi.gama.util.IList<msi.gama.util.IList<msi.gama.metamodel.agent.IAgent>>", "list<list<agent>>");
		hm.put("msi.gama.util.GamaList<msi.gama.metamodel.agent.IAgent>", "list<agent>");
		hm.put("msi.gama.util.GamaList<msi.gama.metamodel.shape.IShape>", "list<geometry>");
		hm.put("msi.gama.util.GamaList<msi.gama.util.GamaList<msi.gama.metamodel.shape.GamaPoint>>","list<list<point>>");
		hm.put("msi.gama.util.IList<msi.gama.metamodel.shape.ILocation>", "list<point>");		
		hm.put("java.util.List<msi.gama.util.path.GamaSpatialPath>", "list<path>");
		
		hm.put("msi.gama.util.GamaList<java.lang.Double>", "list<float>");
		hm.put("msi.gama.util.IList<java.lang.Double>", "list<float>");
		
		hm.put("msi.gama.metamodel.shape.GamaPoint", "point");
		hm.put("msi.gama.metamodel.shape.ILocation", "point");
		hm.put("java.lang.Object", "unknown");
		hm.put("msi.gama.util.GamaPair", "pair");
		hm.put("java.lang.Boolean", "bool");
		hm.put("msi.gama.metamodel.agent.IAgent", "agent");
		hm.put("java.lang.String", "string");
		hm.put("msi.gama.util.graph.IGraph", "graph");
		hm.put("msi.gama.util.graph.GamaGraph", "graph");
		hm.put("msi.gama.metamodel.topology.ITopology", "topology");
		hm.put("msi.gama.util.GamaMap", "map");
		hm.put("msi.gaml.expressions.IExpression", "any expression");
		hm.put("msi.gaml.species.ISpecies", "species");
		hm.put("msi.gama.util.IContainer", "container");
		hm.put("msi.gama.util.IContainer<KeyType,ValueType>", "container");
		hm.put("msi.gama.util.IContainer<?,msi.gama.metamodel.shape.IShape>", "container<geometry>");
		hm.put("msi.gama.util.IContainer<?,msi.gama.metamodel.agent.IAgent>", "container<agent>");
		
		hm.put("java.util.Map", "map");
		hm.put("java.util.Map<java.lang.String,java.lang.Object>", "map<string,unknown>");
		hm.put("msi.gama.util.GamaMap<java.lang.String,java.lang.Object>", "map<string,unknown>");
		
		hm.put("msi.gama.util.IPath", "path");		
		hm.put("msi.gama.util.path.IPath", "path");
		hm.put("double", "float");	
		hm.put("boolean", "bool");
		
		hm.put("msi.gama.util.IAddressableContainer<java.lang.Integer,msi.gama.metamodel.agent.IAgent,java.lang.Integer,msi.gama.metamodel.agent.IAgent>","???");
		return hm;
	}
	
	// FROM IType.java
	private HashMap<Integer,String> initNameTypeFromIType(){
		HashMap<Integer, String> hm = new HashMap<Integer, String>();
		hm.put(0, "unknown");  // NONE
		hm.put(1, "int");
		hm.put(2, "float");
		hm.put(3, "boolean");
		hm.put(4, "string");
		hm.put(5, "list");
		hm.put(6, "rgb");
		hm.put(7, "point");
		hm.put(8, "matrix");
		hm.put(9, "pair");
		hm.put(10, "map");
		hm.put(11, "agent");		
		hm.put(12, "file");
		hm.put(13, "geometry");
		hm.put(14, "species");
		hm.put(15, "graph");
		hm.put(16, "container");
		hm.put(17, "path");
		hm.put(18, "topology");
		hm.put(50, "available_types");
		hm.put(99, "message");
		hm.put(100, "species_types");
		
		hm.put(-200, "label");
		hm.put(-201, "variable id");
		hm.put(-202, "type_id");
		hm.put(-203, "new variable id");
		hm.put(-204, "new temp variable id");
		return hm;
	}
	
	private HashMap<String, String> initProperNameCategoriesMap() {
		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put("Cast", 					IOperatorCategory.CASTING);
		hm.put("Colors", 			IOperatorCategory.COLOR);
		hm.put("Comparison", 			IOperatorCategory.COMPARISON);
		hm.put("IContainer", 		IOperatorCategory.CONTAINER);
		hm.put("Containers", 		IOperatorCategory.CONTAINER);
		hm.put("GamaMap", 			IOperatorCategory.CONTAINER);
		hm.put("Files", 				IOperatorCategory.FILE);
		hm.put("GamaFileType", 			IOperatorCategory.FILE);
		hm.put("MessageType",		IOperatorCategory.FIPA);
		hm.put("ConversationType", 	IOperatorCategory.FIPA);
		hm.put("Graphs", 				IOperatorCategory.GRAPH);
		hm.put("GraphsGraphstream", 	IOperatorCategory.GRAPH);
		hm.put("Logic", 			IOperatorCategory.LOGIC);
		hm.put("Maths", 				IOperatorCategory.ARITHMETIC);
		hm.put("GamaFloatMatrix", 	IOperatorCategory.MATRIX);
		hm.put("GamaIntMatrix", 	IOperatorCategory.MATRIX);
		hm.put("GamaMatrix", 		IOperatorCategory.MATRIX);
		hm.put("GamaObjectMatrix", 	IOperatorCategory.MATRIX);
		hm.put("IMatrix", 			IOperatorCategory.MATRIX);
		hm.put("SingleEquationStatement", IOperatorCategory.EDP);
		hm.put("Creation", 			IOperatorCategory.SPATIAL);
		hm.put("Operators", 		IOperatorCategory.SPATIAL);
		hm.put("Points", 			IOperatorCategory.SPATIAL);
		hm.put("Properties", 		IOperatorCategory.SPATIAL);
		hm.put("Punctal", 			IOperatorCategory.SPATIAL);
		hm.put("Queries", 			IOperatorCategory.SPATIAL);
		hm.put("ThreeD", 			IOperatorCategory.SPATIAL);
		hm.put("Statistics", 		IOperatorCategory.SPATIAL);
		hm.put("Transformations", 	IOperatorCategory.SPATIAL);
		hm.put("Relations", 		IOperatorCategory.SPATIAL);
		hm.put("Random", 				IOperatorCategory.RANDOM);
		hm.put("Stats", 			IOperatorCategory.STATISTICAL);
		hm.put("Strings", 				IOperatorCategory.STRING);		
		hm.put("System", 			IOperatorCategory.SYSTEM);
		hm.put("Types", 				IOperatorCategory.TYPE);
		hm.put("WaterLevel", 		IOperatorCategory.WATER);
		return hm;
	}
	
	public String getProperType(String rawName) {
		if ( properNameTypeMap.containsKey(rawName) ) {
			return properNameTypeMap.get(rawName);
		} else {
			return rawName;
		}
	}

	public String getProperOperatorName(String opName) {
		// if("*".equals(opName)) return "`*`";
		return opName;
	}

	public String getProperCategory(String rawName) {
		if ( properCategoryNameMap.containsKey(rawName) ) {
			return properCategoryNameMap.get(rawName);
		} else {
			return rawName;
		}
	}
	
	public String getTypeString(Integer i){
		if ( typeStringFromIType.containsKey(i) ) {
			return typeStringFromIType.get(i);
		} else {
			return ""+i;
		}		
	}	

	public String getSymbolKindStringFromISymbolKind(Integer i){
		if ( symbolKindStringFromISymbolKind.containsKey(i) ) {
			return symbolKindStringFromISymbolKind.get(i);
		} else {
			return ""+i;
		}		
	}
}
