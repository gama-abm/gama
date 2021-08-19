/*********************************************************************************************
 *
 * 'GamlResourceServices.java, in plugin msi.gama.lang.gaml, is part of the source code of the GAMA modeling and
 * simulation platform. (v. 1.8.1)
 *
 * (c) 2007-2020 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package msi.gama.lang.gaml.resource;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;

import msi.gama.common.interfaces.IDocManager;
import msi.gama.common.interfaces.IGamlDescription;
import msi.gama.common.interfaces.IKeyword;
import msi.gama.lang.gaml.documentation.GamlResourceDocumenter;
import msi.gama.lang.gaml.indexer.GamlResourceIndexer;
import msi.gama.lang.gaml.parsing.GamlSyntacticConverter;
import msi.gama.lang.gaml.validation.IGamlBuilderListener;
import msi.gama.runtime.GAMA;
import msi.gama.util.GamaMapFactory;
import msi.gama.util.IMap;
import msi.gaml.compilation.ast.ISyntacticElement;
import msi.gaml.descriptions.IDescription;
import msi.gaml.descriptions.ModelDescription;
import msi.gaml.descriptions.ValidationContext;
import ummisco.gama.dev.utils.DEBUG;

@SuppressWarnings ({ "unchecked", "rawtypes" })
public class GamlResourceServices {

	static {
		DEBUG.OFF();
	}

	private static int resourceCount = 0;
	private static IDocManager documenter = new GamlResourceDocumenter();
	private static GamlSyntacticConverter converter = new GamlSyntacticConverter();
	private static final Map<URI, IGamlBuilderListener> resourceListeners = GamaMapFactory.createUnordered();
	private static final Map<URI, ValidationContext> resourceErrors = GamaMapFactory.createUnordered();
	private static volatile XtextResourceSet poolSet;
	private static final LoadingCache<URI, IMap<EObject, IGamlDescription>> documentationCache =
			CacheBuilder.newBuilder().build(new CacheLoader<URI, IMap<EObject, IGamlDescription>>() {

				@Override
				public IMap load(final URI key) throws Exception {
					return GamaMapFactory.createUnordered();
				}
			});

	public static IMap<EObject, IGamlDescription> getDocumentationCache(final Resource r) {
		return documentationCache.getUnchecked(properlyEncodedURI(r.getURI()));
	}

	/**
	 * Properly encodes and partially verifies the uri passed in parameter. In the case of an URI that does not use the
	 * "resource:" scheme, it is first converted into a file URI so that headless operations that do not use a workspace
	 * can still perform correctly
	 *
	 * @param uri
	 * @return null if the parameter is null or does not represent neither a file or a resource, otherwise a properly
	 *         encoded version of the parameter.
	 */
	public static URI properlyEncodedURI(final URI uri) {
		if (uri == null) return null;
		URI pre_properlyEncodedURI = uri;
		if (GAMA.isInHeadLessMode() && !uri.isPlatformResource()) {
			final String filePath = uri.toFileString();
			if (filePath == null) return null;
			final File file = new File(filePath);
			try {
				pre_properlyEncodedURI = URI.createFileURI(file.getCanonicalPath());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		// if (DEBUG.IS_ON()) {
		// DEBUG.OUT("Original URI: " + uri + " => " + result);
		// }
		return URI.createURI(pre_properlyEncodedURI.toString(), true);
	}

	public static boolean isEdited(final URI uri) {
		return resourceListeners.containsKey(properlyEncodedURI(uri));
	}

	public static boolean isEdited(final Resource r) {
		return isEdited(r.getURI());
	}

	public static void updateState(final URI uri, final ModelDescription model, final boolean newState,
			final ValidationContext status) {
		// DEBUG.LOG("Beginning updating the state of editor in
		// ResourceServices for " + uri.lastSegment());
		final URI newURI = properlyEncodedURI(uri);

		final IGamlBuilderListener listener = resourceListeners.get(newURI);
		if (listener == null) return;
		// DEBUG.LOG("Finishing updating the state of editor for " +
		// uri.lastSegment());
		final Iterable exps = model == null ? newState ? Collections.EMPTY_SET : null
				: Iterables.filter(model.getExperiments(), each -> !each.isAbstract());
		listener.validationEnded(exps, status);
	}

	public static void addResourceListener(final URI uri, final IGamlBuilderListener listener) {
		final URI newURI = properlyEncodedURI(uri);
		resourceListeners.put(newURI, listener);
	}

	public static void removeResourceListener(final IGamlBuilderListener listener) {
		// URI toRemove = null;
		final Iterator<Map.Entry<URI, IGamlBuilderListener>> it = resourceListeners.entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry<URI, IGamlBuilderListener> entry = it.next();
			if (entry.getValue() == listener) {
				it.remove();
				return;
			}
		}
		// for (final Map.Entry<URI, IGamlBuilderListener> entry : resourceListeners.entrySet()) {
		// if (entry.getValue() == listener) {
		// toRemove = properlyEncodedURI(entry.getKey());
		// }
		// }
		// if (toRemove != null) {
		// resourceListeners.remove(toRemove);
		// documentationCache.invalidate(toRemove);
		// }

	}

	public static ValidationContext getValidationContext(final GamlResource r) {
		final URI newURI = properlyEncodedURI(r.getURI());
		if (!resourceErrors.containsKey(newURI)) {
			resourceErrors.put(newURI, new ValidationContext(newURI, r.hasErrors(), getResourceDocumenter()));
		}
		final ValidationContext result = resourceErrors.get(newURI);
		result.hasInternalSyntaxErrors(r.hasErrors());
		return result;
	}

	public static void discardValidationContext(final Resource r) {
		resourceErrors.remove(properlyEncodedURI(r.getURI()));
	}

	/**
	 * Returns the path from the root of the workspace
	 *
	 * @return an IPath. Never null.
	 */
	public static IPath getPathOf(final Resource r) {
		IPath path;
		final URI uri = r.getURI();
		if (uri.isPlatform()) {
			path = new Path(uri.toPlatformString(false));
		} else if (uri.isFile()) {
			path = new Path(uri.toFileString());
		} else {
			path = new Path(uri.path());
		}
		try {
			path = new Path(URLDecoder.decode(path.toOSString(), "UTF-8"));
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return path;

	}

	public static String getModelPathOf(final Resource r) {
		// Likely in a headless scenario (w/o workspace)
		if (r.getURI().isFile())
			return new Path(r.getURI().toFileString()).toOSString();
		else {
			final IPath path = getPathOf(r);
			final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			final IPath fullPath = file.getLocation();
			return fullPath == null ? "" : fullPath.toOSString();
		}
	}

	private static boolean isProject(final File f) {
		final String[] files = f.list();
		if (files != null) {
			for (final String s : files) {
				if (".project".equals(s)) return true;
			}
		}
		return false;
	}

	public static String getProjectPathOf(final Resource r) {
		if (r == null) return "";
		final URI uri = r.getURI();
		if (uri == null) return "";
		// Cf. #2983 -- we are likely in a headless scenario
		if (uri.isFile()) {
			File project = new File(uri.toFileString());
			while (project != null && !isProject(project)) {
				project = project.getParentFile();
			}
			return project == null ? "" : project.getAbsolutePath();
		} else {
			final IPath path = getPathOf(r);
			final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			final IPath fullPath = file.getProject().getLocation();
			return fullPath == null ? "" : fullPath.toOSString();
		}
	}

	// AD The removal of synchronized solves an issue where threads at startup would end up waiting for
	// GamlResourceServices to become free
	public/* synchronized */ static GamlResource getTemporaryResource(final IDescription existing) {
		ResourceSet rs = null;
		GamlResource r = null;
		if (existing != null) {
			final ModelDescription desc = existing.getModelDescription();
			if (desc != null) {
				final EObject e = desc.getUnderlyingElement();
				if (e != null) {
					r = (GamlResource) e.eResource();
					if (r != null) { rs = r.getResourceSet(); }
				}
			}
		}
		if (rs == null) { rs = getPoolSet(); }
		final URI uri = URI.createURI(IKeyword.SYNTHETIC_RESOURCES_PREFIX + resourceCount++ + ".gaml", false);
		// TODO Modifier le cache de la resource ici ?
		final GamlResource result = (GamlResource) rs.createResource(uri);
		final IMap<URI, String> imports = GamaMapFactory.create();
		imports.put(uri, null);
		if (r != null) {
			imports.put(r.getURI(), null);
			final Map<URI, String> uris = GamlResourceIndexer.allLabeledImportsOf(r);
			imports.putAll(uris);
		}
		result.getCache().getOrCreate(result).set(GamlResourceIndexer.IMPORTED_URIS, imports);
		return result;
	}

	public static void discardTemporaryResource(final GamlResource temp) {
		try {
			temp.delete(null);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public static IDocManager getResourceDocumenter() {
		return documenter;
	}

	public static ISyntacticElement buildSyntacticContents(final GamlResource r) {
		return converter.buildSyntacticContents(r.getParseResult().getRootASTElement(), null);
	}

	private static XtextResourceSet getPoolSet() {
		if (poolSet == null) {
			poolSet = new XtextResourceSet() {
				{
					setClasspathURIContext(GamlResourceServices.class);
				}

			};
		}
		return poolSet;
	}

}
