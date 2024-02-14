/*******************************************************************************************************
 *
 * GamlResourceIndexer.java, in msi.gama.lang.gaml, is part of the source code of the GAMA modeling and simulation
 * platform (v.1.9.3).
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package msi.gama.lang.gaml.indexer;

import static msi.gama.lang.gaml.resource.GamlResourceServices.properlyEncodedURI;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com.google.inject.Singleton;

import msi.gama.lang.gaml.gaml.GamlPackage;
import msi.gama.lang.gaml.gaml.Import;
import msi.gama.lang.gaml.gaml.StandaloneExperiment;
import msi.gama.lang.gaml.gaml.impl.StandaloneModelImpl;
import msi.gama.lang.gaml.resource.GamlResource;
import msi.gama.lang.gaml.resource.GamlResourceInfoProvider;
import msi.gama.lang.gaml.resource.ImportedResources;
import msi.gama.util.GamaMapFactory;
import msi.gama.util.IMap;
import ummisco.gama.dev.utils.DEBUG;

/**
 * The Class GamlResourceIndexer.
 */
@Singleton
@SuppressWarnings ({ "unchecked", "rawtypes" })
public class GamlResourceIndexer {

	/** The index. */
	static GamlResourceGraph index = new GamlResourceGraph();

	static {
		DEBUG.ON();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(event -> {
			if (event.getBuildKind() == IncrementalProjectBuilder.CLEAN_BUILD) { eraseIndex(); }
		}, IResourceChangeEvent.PRE_BUILD);
	}

	/** The Constant EMPTY_MAP. */
	protected final static IMap EMPTY_MAP = GamaMapFactory.create();

	/** The Constant IMPORTED_URIS. */
	public static final Object IMPORTED_URIS = "ImportedURIs";

	/**
	 * Gets the imports as absolute URIS.
	 *
	 * @param baseURI
	 *            the base URI
	 * @param m
	 *            the m
	 * @return the imports as absolute URIS
	 */
	private static Map<URI, String> getImportsAsAbsoluteURIS(final URI baseURI, final EObject m) {
		Map<URI, String> result = EMPTY_MAP;
		if (m instanceof StandaloneModelImpl model && model.eIsSet(GamlPackage.STANDALONE_MODEL__IMPORTS)) {
			List<Import> imports = model.getImports();
			if (imports.isEmpty()) return result;
			result = GamaMapFactory.createOrdered();
			for (final Import e : imports) {
				final String u = e.getImportURI();
				if (u != null) {
					URI uri = URI.createURI(u, true);
					try {
						uri = uri.resolve(baseURI);
					} catch (Exception e1) {
						DEBUG.LOG("Error in resolving " + uri + " against base " + baseURI);
					}
					uri = properlyEncodedURI(uri);
					result.put(uri, e.getName());
				}
			}
		} else if (m instanceof StandaloneExperiment expe) {
			final String u = expe.getImportURI();
			if (u != null) {
				URI uri = URI.createURI(u, true);
				uri = properlyEncodedURI(uri.resolve(baseURI));
				result = Collections.singletonMap(uri, null);
			}
		}
		return result;
	}

	/**
	 * Find import.
	 *
	 * @param contents
	 *            the model
	 * @param uri
	 *            the uri
	 * @return the e object
	 */
	static private EObject findImport(final EObject contents, final URI baseURI, final URI uri) {
		if (contents instanceof StandaloneExperiment expe) {
			String u = expe.getImportURI();
			if (u.contains(URI.decode(uri.lastSegment())) || uri.equals(baseURI) && u.isEmpty()) return contents;
		} else if (contents instanceof StandaloneModelImpl model) {
			for (final Import imp : model.getImports()) {
				if (imp.getImportURI().contains(URI.decode(uri.lastSegment()))) return imp;
			}
		}
		return null;
	}

	/**
	 * Synchronized method to avoid concurrent errors in the graph in case of a parallel resource loader
	 */
	public static synchronized EObject updateImports(final GamlResource r) {
		final URI baseURI = r.getURI();
		final Map<URI, String> existingEdges = index.outgoingEdgesOf(baseURI);
		if (r.getContents().isEmpty()) return null;
		final EObject contents = r.getContents().get(0);
		if (contents == null) return null;
		final Map<URI, String> newEdges = getImportsAsAbsoluteURIS(baseURI, contents);
		for (Map.Entry<URI, String> entry : newEdges.entrySet()) {
			URI uri = entry.getKey();
			if (baseURI.equals(uri)) { continue; }
			String label = entry.getValue();
			if (!existingEdges.containsKey(uri)) {
				if (!EcoreUtil2.isValidUri(r, uri)) return findImport(contents, baseURI, uri);
				final boolean alreadyThere = index.imports.containsVertex(uri);
				final URI to = uri;
				final String label1 = label;
				index.addEdge(baseURI, to, label1);
				if (!alreadyThere) {
					// This call should trigger the recursive call to updateImports()
					r.getResourceSet().getResource(uri, true);
				}
			} else {
				index.addEdge(baseURI, uri, existingEdges.remove(uri));
			}
		}
		index.removeAllEdges(baseURI, existingEdges);
		return null;

	}

	/**
	 * Validate the imports of a resource by reconstructing the associated resources and verifying their status.
	 *
	 * @param resource
	 *            the resource
	 * @return the linked hash multimap
	 */
	public static ImportedResources validateImportsOf(final GamlResource resource) {
		final Map<URI, String> uris = allImportsOf(resource);
		ImportedResources imports = null;
		if (!uris.isEmpty()) {
			imports = new ImportedResources();
			for (Map.Entry<URI, String> entry : uris.entrySet()) {
				final GamlResource r = (GamlResource) resource.getResourceSet().getResource(entry.getKey(), true);
				if (r == resource) { continue; }
				if (r.hasErrors()) {
					resource.invalidate(r, "Errors detected");
					return null;
				}
				imports.add(entry.getValue(), r);
			}
		}
		return imports;
	}

	/**
	 * All labeled imports of.
	 *
	 * @param r
	 *            the r
	 * @return the i map
	 */
	public static Map<URI, String> allImportsOf(final GamlResource r) {
		return r.getCache().get(IMPORTED_URIS, r, () -> allImportsOfProperlyEncoded(r.getURI()));
	}

	/**
	 * Erase index.
	 */
	public static void eraseIndex() {
		index.reset();
	}

	/**
	 * Checks if is imported.
	 *
	 * @param r
	 *            the r
	 * @return true, if is imported
	 */
	public static boolean isImported(final GamlResource r) {
		return !directImportersOf(r.getURI()).isEmpty();
	}

	/**
	 * Direct importers of.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param uri
	 *            the uri
	 * @return the sets the
	 * @date 14 janv. 2024
	 */
	public static Set<URI> directImportersOf(final URI uri) {
		return index.predecessorsOf(uri);
	}

	/**
	 * Direct imports of.
	 *
	 * @param uri
	 *            the uri
	 * @return the sets the
	 */
	public static Set<URI> directImportsOf(final URI uri) {
		return index.successorsOf(uri);
	}

	/**
	 * All labeled imports of.
	 *
	 * @param uri
	 *            the uri
	 * @return the i map
	 */
	public static Map<URI, String> allImportsOf(final URI uri) {
		return allImportsOfProperlyEncoded(properlyEncodedURI(uri));
	}

	/**
	 * All imports of properly encoded.
	 *
	 * @param uri
	 *            the uri
	 * @return the map
	 */
	public static Map<URI, String> allImportsOfProperlyEncoded(final URI uri) {
		return index.sortedDepthFirstSearchWithLabels(uri);
	}

	/**
	 * All types declared in.
	 *
	 * @author Alexis Drogoul (alexis.drogoul@ird.fr)
	 * @param uri
	 *            the uri
	 * @return the sets the
	 * @date 13 févr. 2024
	 */
	public static Set<String> allTypesDeclaredIn(final URI uri, final String contents) {
		Set<String> result;
		if (contents == null) {
			result = GamlResourceInfoProvider.INSTANCE.getInfo(uri).getSpecies();
		} else {
			result = GamlResourceInfoProvider.INSTANCE.getInfo(uri, contents, 0).getSpecies();
		}
		allImportsOf(uri).forEach((u, s) -> result.addAll(allTypesDeclaredIn(u, null)));
		return result;
	}

}
