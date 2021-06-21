package msi.gama.precompiler;

import static java.util.Collections.sort;

// import static gama.processor.annotations.GamlProperties.GAML;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ProcessorContext implements ProcessingEnvironment, RoundEnvironment, Constants {
	private final static boolean PRODUCES_DOC = true;
	public static final Charset CHARSET = Charset.forName("UTF-8");
	public static final String ADDITIONS_PACKAGE_BASE = "gaml.additions";
	public static final String ADDITIONS_CLASS_NAME = "GamlAdditions";
	private final static boolean PRODUCES_WARNING = true;
	public static final StandardLocation OUT = StandardLocation.SOURCE_OUTPUT;
	private final ProcessingEnvironment delegate;
	private RoundEnvironment round;
	private TypeMirror iSkill, iAgent, iVarAndActionSupport, iScope, string;
	public volatile String currentPlugin;
	public volatile String shortcut;
	public List<String> roots;
	public static final DocumentBuilder xmlBuilder;

	static {
		DocumentBuilder temp = null;
		try {
			temp = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException e) {}
		xmlBuilder = temp;
	}

	public ProcessorContext(final ProcessingEnvironment pe) {
		delegate = pe;
	}

	public DocumentBuilder getBuilder() {
		return xmlBuilder;
	}

	public String nameOf(final TypeElement e) {
		if (e.getNestingKind() == NestingKind.TOP_LEVEL) return e.getQualifiedName().toString();
		return nameOf((TypeElement) e.getEnclosingElement()) + "." + e.getSimpleName().toString();
	}

	/**
	 * Introduced to handle issue #1671
	 *
	 * @param env
	 * @param annotationClass
	 * @return
	 */
	public List<? extends Element> sortElements(final Class<? extends Annotation> annotationClass) {
		final Set<? extends Element> elements = getElementsAnnotatedWith(annotationClass);
		final List<? extends Element> result = new ArrayList<>(elements);
		sort(result, (o1, o2) -> o1.toString().compareTo(o2.toString()));
		return result;
	}

	public final Map<String, List<Element>> groupElements(final Class<? extends Annotation> annotationClass) {
		final Map<String, List<Element>> result = getElementsAnnotatedWith(annotationClass).stream()
				.collect(Collectors.groupingBy((k) -> getRootClassOf(k)));
		// result.forEach((s, l) -> sort(l, (o1, o2) -> o1.toString().compareTo(o2.toString())));
		return result;
	}

	private String getRootClassOf(final Element e) {
		final ElementKind kind = e.getKind();
		final Element enclosing = e.getEnclosingElement();
		final ElementKind enclosingKind = enclosing.getKind();
		if ((kind == ElementKind.CLASS || kind == ElementKind.INTERFACE)
				&& !(enclosingKind == ElementKind.CLASS || enclosingKind == ElementKind.INTERFACE))
			return e.toString();
		return getRootClassOf(enclosing);
	}

	public TypeMirror getISkill() {
		if (iSkill == null) { iSkill = getType("msi.gama.common.interfaces.ISkill"); }
		return iSkill;
	}

	public TypeMirror getIScope() {
		if (iScope == null) { iScope = getType("msi.gama.runtime.IScope"); }
		return iScope;
	}

	public TypeMirror getString() {
		if (string == null) { string = getType("java.lang.String"); }
		return string;
	}

	public TypeMirror getType(final String qualifiedName) {
		TypeElement e = delegate.getElementUtils().getTypeElement(qualifiedName);
		if (e == null) return null;
		return e.asType();
	}

	public TypeMirror getIVarAndActionSupport() {
		if (iVarAndActionSupport == null) {
			iVarAndActionSupport = getType("msi.gama.common.interfaces.IVarAndActionSupport");
		}
		return iVarAndActionSupport;
	}

	TypeMirror getIAgent() {
		if (iAgent == null) { iAgent = getType("msi.gama.metamodel.agent.IAgent"); }
		return iAgent;
	}

	@Override
	public Map<String, String> getOptions() {
		return delegate.getOptions();
	}

	@Override
	public Messager getMessager() {
		return delegate.getMessager();
	}

	@Override
	public Filer getFiler() {
		return delegate.getFiler();
	}

	@Override
	public Elements getElementUtils() {
		return delegate.getElementUtils();
	}

	@Override
	public Types getTypeUtils() {
		return delegate.getTypeUtils();
	}

	@Override
	public SourceVersion getSourceVersion() {
		return delegate.getSourceVersion();
	}

	@Override
	public Locale getLocale() {
		return delegate.getLocale();
	}

	public void emitWarning(final String s) {
		emitWarning(s, (Element) null);
	}

	public void emitError(final String s) {
		emitError(s, (Element) null);
	}

	public void emitWarning(final String s, final Element e) {
		emit(Kind.WARNING, s, e);
	}

	public void emitError(final String s, final Element e) {
		emit(Kind.ERROR, s, e);
	}

	public void emit(final Kind kind, final String s, final Element e) {
		if (!PRODUCES_WARNING) return;
		if (e == null) {
			getMessager().printMessage(kind, "GAML: " + s);
		} else {
			getMessager().printMessage(kind, "GAML: " + s, e);
		}
	}

	public void emitError(final String s, final Exception e1) {
		emit(Kind.ERROR, s, e1, null);
	}

	public void emitWarning(final String s, final Exception e1) {
		emit(Kind.WARNING, s, e1, null);
	}

	public void emitError(final String s, final Exception e1, final Element element) {
		emit(Kind.ERROR, s, e1, element);
	}

	public void emitWarning(final String s, final Exception e1, final Element element) {
		emit(Kind.WARNING, s, e1, element);
	}

	public void emit(final Kind kind, final String s, final Exception e1, final Element element) {
		final StringBuilder sb = new StringBuilder();
		sb.append(s);
		sb.append(e1.getMessage());
		for (final StackTraceElement t : e1.getStackTrace()) {
			sb.append("\n");
			sb.append(t.toString());
		}
		emit(kind, sb.toString(), element);
	}

	public void setRoundEnvironment(final RoundEnvironment env) {
		round = env;
		roots = round.getRootElements().stream().map(e -> e.toString()).collect(Collectors.toList());
	}

	@Override
	public boolean processingOver() {
		return round.processingOver();
	}

	@Override
	public boolean errorRaised() {
		return round.errorRaised();
	}

	@Override
	public Set<? extends Element> getRootElements() {
		return round.getRootElements();
	}

	@Override
	public Set<? extends Element> getElementsAnnotatedWith(final TypeElement a) {
		return round.getElementsAnnotatedWith(a);
	}

	@Override
	public Set<? extends Element> getElementsAnnotatedWith(final Class<? extends Annotation> a) {
		return round.getElementsAnnotatedWith(a);
	}

	public Writer createWriter(final String s) {
		try {
			final OutputStream output = getFiler().createResource(OUT, "", s, (Element[]) null).openOutputStream();
			final Writer writer = new OutputStreamWriter(output, CHARSET);
			return writer;
		} catch (final Exception e) {
			emitWarning("", e);
		}
		return null;
	}

	void initCurrentPlugin() {
		try {
			final FileObject temp = getFiler().createSourceFile("gaml.additions.package-info", (Element[]) null);
			emit(Kind.NOTE, "GAML Processor: creating " + temp.toUri(), (Element) null);
			final String plugin2 = temp.toUri().toASCIIString().replace("/target/gaml/additions/package-info.java", "")
					.replace("/gaml/gaml/additions/package-info.java", "");
			currentPlugin = plugin2.substring(plugin2.lastIndexOf('/') + 1);
			shortcut = currentPlugin.substring(currentPlugin.lastIndexOf('.') + 1);
		} catch (IOException e) {
			emitWarning("Exception raised while reading the current plugin name " + e.getMessage(), e);
		}
	}

	public FileObject createSource() {
		initCurrentPlugin();
		try {

			final FileObject obj = getFiler().createSourceFile(
					ADDITIONS_PACKAGE_BASE + "." + shortcut + "." + ADDITIONS_CLASS_NAME, (Element[]) null);
			return obj;
		} catch (final Exception e) {
			emitWarning("Exception raised while creating the source file: " + e.getMessage(), e);
		}
		return null;
	}

	public Writer createTestWriter() {
		return createTestWriter(getTestFileName());
	}

	public Writer createTestWriter(final String fileName) {
		createTestsFolder();
		try {
			final OutputStream output =
					getFiler().createResource(OUT, getTestFolderName() + ".models", fileName, (Element[]) null)
							.openOutputStream();
			final Writer writer = new OutputStreamWriter(output, CHARSET);
			return writer;
		} catch (final Exception e) {
			e.printStackTrace();
			emitWarning("Impossible to create test file " + fileName + ": ", e);
		}
		return null;
	}

	private String getTestFileName() {
		final String title = currentPlugin.substring(currentPlugin.lastIndexOf('.') + 1);
		return Constants.capitalizeFirstLetter(title) + " Tests.experiment";
	}

	private String getTestFolderName() {
		final String title = currentPlugin.substring(currentPlugin.lastIndexOf('.') + 1);
		return "tests.Generated From " + Constants.capitalizeFirstLetter(title);
	}

	public void createTestsFolder() {
		FileObject obj = null;
		try {
			obj = getFiler().createResource(OUT, getTestFolderName(), ".project", (Element[]) null);
		} catch (final FilerException e) {
			// Already exists. Simply return
			return;
		} catch (final IOException e) {
			// More serious problem
			emitWarning("Cannot create tests folder: ", e);
			return;
		}
		try (final OutputStream output = obj.openOutputStream();
				final Writer writer = new OutputStreamWriter(output, CHARSET);) {
			writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<projectDescription>\n"
					+ "	<name>Generated tests in " + currentPlugin + "</name>\n" + "	<comment>" + currentPlugin
					+ "</comment>\n" + "	<projects>\n" + "	</projects>\n" + "	<buildSpec>\n"
					+ "		<buildCommand>\n" + "			<name>org.eclipse.xtext.ui.shared.xtextBuilder</name>\n"
					+ "			<arguments>\n" + "			</arguments>\n" + "		</buildCommand>\n"
					+ "	</buildSpec>\n" + "	<natures>\n"
					+ "		<nature>org.eclipse.xtext.ui.shared.xtextNature</nature>\n"
					+ "		<nature>msi.gama.application.gamaNature</nature>\n"
					+ "		<nature>msi.gama.application.testNature</nature>\n" + "	</natures>\n"
					+ "</projectDescription>\n" + "");
		} catch (final IOException t) {
			emitWarning("", t);
		}
	}

	public Writer createSourceWriter(final FileObject file) {
		try {
			final OutputStream output = file.openOutputStream();
			final Writer writer = new OutputStreamWriter(output, CHARSET);
			return writer;
		} catch (final Exception e) {
			emitWarning("Error in creating source writer", e);
		}
		return null;
	}

	public boolean shouldProduceDoc() {
		return "true".equals(getOptions().get("doc")) || PRODUCES_DOC;
	}

	public InputStream getInputStream(final String string) throws IOException {
		return getFiler().getResource(ProcessorContext.OUT, "", string).openInputStream();
	}

	public List<Annotation> getUsefulAnnotationsOn(final Element e) {
		final List<Annotation> result = new ArrayList<>();
		for (final Class<? extends Annotation> clazz : processors.keySet()) {
			final Annotation a = e.getAnnotation(clazz);
			if (a != null) { result.add(a); }
		}
		return result;
	}

	public List<String> getRoots() {
		return roots;
	}

}