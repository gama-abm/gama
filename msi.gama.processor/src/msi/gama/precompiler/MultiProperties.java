/*
 * GAMA - V1.4 http://gama-platform.googlecode.com
 * 
 * (c) 2007-2011 UMI 209 UMMISCO IRD/UPMC & Partners (see below)
 * 
 * Developers :
 * 
 * - Alexis Drogoul, UMI 209 UMMISCO, IRD/UPMC (Kernel, Metamodel, GAML), 2007-2012
 * - Vo Duc An, UMI 209 UMMISCO, IRD/UPMC (SWT, multi-level architecture), 2008-2012
 * - Patrick Taillandier, UMR 6228 IDEES, CNRS/Univ. Rouen (Batch, GeoTools & JTS), 2009-2012
 * - Beno�t Gaudou, UMR 5505 IRIT, CNRS/Univ. Toulouse 1 (Documentation, Tests), 2010-2012
 * - Phan Huy Cuong, DREAM team, Univ. Can Tho (XText-based GAML), 2012
 * - Pierrick Koch, UMI 209 UMMISCO, IRD/UPMC (XText-based GAML), 2010-2011
 * - Romain Lavaud, UMI 209 UMMISCO, IRD/UPMC (RCP environment), 2010
 * - Francois Sempe, UMI 209 UMMISCO, IRD/UPMC (EMF model, Batch), 2007-2009
 * - Edouard Amouroux, UMI 209 UMMISCO, IRD/UPMC (C++ initial porting), 2007-2008
 * - Chu Thanh Quang, UMI 209 UMMISCO, IRD/UPMC (OpenMap integration), 2007-2008
 */
package msi.gama.precompiler;

import java.io.*;
import java.util.*;

/**
 * Written by drogoul Modified on 27 juil. 2010
 * 
 * @todo Description
 * 
 */
public class MultiProperties {

	private final static Map<String, MultiProperties> cache = new HashMap();

	public static MultiProperties readCache(final String s) {
		// System.out.println("Reading cached properties " + s);
		return cache.get(s);
	}

	public static void writeCache(final String s, final MultiProperties mp) {
		// System.out.println("Caching properties " + s + "; Keys : " + mp.keySet());
		cache.put(s, mp);
	}

	Map<String, Set<String>> map;

	public final static String GRAMMAR = "std.gaml";

	public final static String SKILLS = "skills.properties";

	public final static String UNARIES = "unaries.properties";

	public final static String BINARIES = "binaries.properties";

	public final static String TYPES = "types.properties";

	public final static String SYMBOLS = "symbols.properties";

	public final static String CHILDREN = "children.properties";

	public final static String FACETS = "facets.properties";

	public final static String KINDS = "kinds.properties";

	public final static String FACTORIES = "factories.properties";

	public final static String SPECIES = "species.properties";

	public final static String VARS = "vars.properties";

	public static final String[] FILES = new String[] { SKILLS, UNARIES, BINARIES, TYPES, SYMBOLS,
	CHILDREN, FACETS, KINDS, FACTORIES, SPECIES, VARS };

	static final String NULL = "";

	public MultiProperties() {
		map = new HashMap();
	}

	public Set<String> keySet() {
		return map.keySet();
	}

	public Set<String> values() {
		Set<String> result = new HashSet();
		for ( Map.Entry<String, Set<String>> entry : map.entrySet() ) {
			result.addAll(entry.getValue());
		}
		return result;
	}

	public Set<String> get(final String key) {
		return map.get(key);
	}

	public String getFirst(final String key) {
		Set<String> result = get(key);
		if ( result == null ) { return null; }
		for ( String s : result ) {
			return s;
		}
		return null;
	}

	public void put(final String key, final String value) {
		if ( !map.containsKey(key) ) {
			map.put(key, new HashSet<String>());
		}
		map.get(key).add(value);
	}

	public void put(final String key, final Set<String> values) {
		if ( !map.containsKey(key) ) {
			map.put(key, values);
		} else {
			map.get(key).addAll(values);
		}
	}

	public void putAll(final MultiProperties m) {
		for ( String key : m.keySet() ) {
			put(key, m.get(key));
		}
	}

	public void store(final Writer writer) {
		Properties prop = new Properties();
		for ( String key : map.keySet() ) {
			prop.setProperty(key, toString(map.get(key)));
		}
		try {
			prop.store(writer, NULL);
			writer.flush();
			writer.close();
		} catch (IOException e) {}

	}

	public static String toString(final Set<String> strings) {
		if ( !strings.isEmpty() ) {
			StringBuilder sb = new StringBuilder();
			for ( String value : strings ) {
				sb.append(value).append(',');
			}
			sb.setLength(sb.length() - 1);
			return sb.toString();
		}
		return NULL;

	}

	/**
	 * same than toString, without "set"
	 * @param strings
	 * @return toString
	 */
	public static String toStringWoSet(final Set<String> strings) {
		if ( !strings.isEmpty() ) {
			StringBuilder sb = new StringBuilder();
			for ( String value : strings ) {
				if ( !value.trim().equals("set") ) {
					sb.append(value).append(',');
				}
			}
			sb.setLength(sb.length() - 1);
			return sb.toString();
		}
		return NULL;

	}

	public void load(final Reader reader) {
		Properties prop = new Properties();
		try {
			prop.load(reader);
		} catch (IOException e) {
			try {
				reader.close();
			} catch (IOException e1) {}
			return;
		}
		for ( String s : prop.stringPropertyNames() ) {
			String[] array = prop.getProperty(s, "").split(",");
			Set<String> values = new HashSet(Arrays.asList(array));
			put(s, values);
		}
		try {
			reader.close();
		} catch (IOException e) {}
	}

	public static MultiProperties loadFrom(final File file, final String title) {
		if ( file == null ) { return new MultiProperties(); }
		if ( cache.containsKey(title) ) { return readCache(title); }
		MultiProperties mp = new MultiProperties();
		try {
			mp.load(new FileReader(file));
		} catch (FileNotFoundException e) {}
		writeCache(title, mp);
		return mp;
	}

	public static MultiProperties loadFrom(final InputStream stream, final String title) {
		if ( stream == null ) { return new MultiProperties(); }
		if ( cache.containsKey(title) ) { return readCache(title); }
		MultiProperties mp = new MultiProperties();
		mp.load(new InputStreamReader(stream));
		writeCache(title, mp);
		return mp;

	}
}
