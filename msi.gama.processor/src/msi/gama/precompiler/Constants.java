package msi.gama.precompiler;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import msi.gama.precompiler.GamlAnnotations.action;
import msi.gama.precompiler.GamlAnnotations.constant;
import msi.gama.precompiler.GamlAnnotations.display;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.experiment;
import msi.gama.precompiler.GamlAnnotations.factory;
import msi.gama.precompiler.GamlAnnotations.file;
import msi.gama.precompiler.GamlAnnotations.listener;
import msi.gama.precompiler.GamlAnnotations.operator;
import msi.gama.precompiler.GamlAnnotations.skill;
import msi.gama.precompiler.GamlAnnotations.species;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.GamlAnnotations.tests;
import msi.gama.precompiler.GamlAnnotations.type;
import msi.gama.precompiler.GamlAnnotations.vars;
import msi.gama.precompiler.doc.DocProcessor;
import msi.gama.precompiler.tests.TestProcessor;

public interface Constants {

	static String capitalizeFirstLetter(final String original) {
		if (original == null || original.length() == 0) return original;
		return original.substring(0, 1).toUpperCase() + original.substring(1);
	}

	static String capitalizeAllWords(final String str) {
		if (str == null || str.length() == 0) return str;
		final int strLen = str.length();
		final StringBuffer buffer = new StringBuffer(strLen);
		boolean capitalizeNext = true;
		for (int i = 0; i < strLen; i++) {
			final char ch = str.charAt(i);
			if (' ' == ch) {
				buffer.append(ch);
				capitalizeNext = true;
			} else if (capitalizeNext) {
				buffer.append(Character.toTitleCase(ch));
				capitalizeNext = false;
			} else {
				buffer.append(ch);
			}
		}
		return buffer.toString();

	}

	static String getAlphabetOrder(final String name) {
		String order = "";
		final String lastChar = "z";

		for (int i = 0; i < cuttingLettersOperatorDoc.length; i++) {
			final Character previousChar = i == 0 ? 'a' : cuttingLettersOperatorDoc[i - 1];
			final Character c = cuttingLettersOperatorDoc[i];

			if (i == 0 && name.toLowerCase().compareTo(c.toString().toLowerCase()) < 0
					|| name.toLowerCase().compareTo(previousChar.toString().toLowerCase()) >= 0
							&& name.toLowerCase().compareTo(c.toString().toLowerCase()) < 0) {
				order = previousChar.toString() + ((Character) Character.toChars(c - 1)[0]).toString();
			}
		}
		if ("".equals(order)) {
			order = cuttingLettersOperatorDoc[cuttingLettersOperatorDoc.length - 1].toString() + lastChar;
		}

		return order;
	}

	String BASIC_SKILL = "msi.gaml.skills.Skill";

	Character[] cuttingLettersOperatorDoc = { 'b', 'd', 'i', 'n', 's' };

	String DOC_SEP = "~";

	String ln = "\n";
	String tab = "\t";
	String in = ln;
	String IAGENT = "IAgent", IPOPULATION = "IPopulation", ISIMULATION = "ISimulation", ISKILL = "ISkill",
			ISYMBOL = "ISymbol", IDESC = "IDescription", ISCOPE = "IScope", OBJECT = "Object", IVALUE = "IValue",
			IEXPRESSION = "IExpression", INTEGER = "Integer", DOUBLE = "Double", BOOLEAN = "Boolean";

	String[] EXPLICIT_IMPORTS = new String[] { "msi.gaml.operators.Random", "msi.gaml.operators.Maths",
			"msi.gaml.operators.Points", "msi.gaml.operators.Spatial.Properties", "msi.gaml.operators.System" };

	List<String> ss1 = Arrays.asList("const", "true", "false", "name", "type");
	List<String> ss2 = Arrays.asList("CONST", "TRUE", "FALSE", "NAME", "TYPE");
	Map<String, String> CLASS_NAMES = new HashMap<>() {
		{
			put("IAgent", "IA");
			put("IGamlAgent", "IG");
			put("GamaColor", "GC");
			put("GamaPair", "GP");
			put("GamaShape", "GS");
			put("Object", "O");
			put("Integer", "I");
			put("Double", "D");
			put("Boolean", "B");
			put("IExpression", "IE");
			put("IShape", "IS");
			put("IMap", "GM");
			put("IContainer", "IC");
			put("GamaPoint", "IL");
			put("IMatrix", "IM");
			put("String", "S");
			put("GamaPoint", "P");
			put("MovingSkill", "MSK");
			put("WorldSkill", "WSK");
			put("GridSkill", "GSK");
			put("IGamaFile", "GF");
			put("IPath", "IP");
			put("IList", "LI");
			put("ITopology", "IT");
			put("GamlAgent", "GA");
			put("ISpecies", "SP");
			put("IScope", "SC");
			put("GamaDate", "GD");
			put("SimulationAgent", "SA");
			put("ExperimentAgent", "EA");
			put("DeprecatedOperators", "DO");
			put("PlatformAgent", "PA");
			put("double", "d");
			put("int", "i");
			put("boolean", "b");

		}
	};
	Map<String, String> RETURN_WHEN_NULL = new HashMap<>() {
		{
			put(DOUBLE, " 0d");
			put(INTEGER, " 0");
			put(BOOLEAN, " false");
		}
	};

	Map<String, String> CHECK_PRIM = new HashMap<>() {
		{
			put("int", INTEGER);
			put("short", INTEGER);
			put("long", INTEGER);
			put("double", DOUBLE);
			put("float", DOUBLE);
			put("boolean", BOOLEAN);
		}
	};

	String PACKAGE_NAME = "gaml.additions";

	Map<Class<? extends Annotation>, IProcessor<?>> processors =
			new LinkedHashMap<>() {
				{
					// Order is important
					put(type.class, new TypeProcessor());
					// Doc built first, so that test generation can happen subsequently
					put(doc.class, new DocProcessor());
					// Then all the processors for specific annotations

					put(factory.class, new FactoryProcessor());
					put(species.class, new SpeciesProcessor());
					put(symbol.class, new SymbolProcessor());
					put(vars.class, new VarsProcessor());
					put(listener.class, new ListenerProcessor());
					put(operator.class, new OperatorProcessor());
					put(file.class, new FileProcessor());
					put(action.class, new ActionProcessor());
					put(skill.class, new SkillProcessor());
					put(display.class, new DisplayProcessor());
					put(experiment.class, new ExperimentProcessor());
					put(constant.class, new ConstantProcessor());
					// TestProcessor actually processes both @tests and @test annotations
					put(tests.class, new TestProcessor());
				}
			};

}
