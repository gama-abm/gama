/*******************************************************************************************************
 *
 * GamaIcons.java, in ummisco.gama.ui.shared, is part of the source code of the GAMA modeling and simulation platform
 * (v.1.9.0).
 *
 * (c) 2007-2023 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package ummisco.gama.ui.resources;

import static ummisco.gama.dev.utils.DEBUG.PAD;
import static ummisco.gama.dev.utils.DEBUG.TIMER_WITH_EXCEPTIONS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.osgi.framework.Bundle;

import msi.gama.application.workbench.IIconProvider;
import ummisco.gama.dev.utils.DEBUG;
import ummisco.gama.ui.resources.GamaColors.GamaUIColor;
import ummisco.gama.ui.utils.WorkbenchHelper;

/**
 * Class GamaIcons.
 *
 * @author drogoul
 * @since 12 sept. 2013
 *
 */
public class GamaIcons implements IIconProvider {

	static {
		DEBUG.ON();
	}

	/** The Constant PLUGIN_ID. */
	public static final String PLUGIN_ID = "ummisco.gama.ui.shared";

	/** The instance. */
	static private GamaIcons instance = new GamaIcons();

	/**
	 * Gets the single instance of GamaIcons.
	 *
	 * @return single instance of GamaIcons
	 */
	public static GamaIcons getInstance() { return instance; }

	/** The Constant DEFAULT_PATH. */
	static public final String DEFAULT_PATH = "/icons/";

	/** The Constant DVG_PATH. */
	static public final String SVG_PATH = "/svg/";

	/** The Constant SIZER_PREFIX. */
	static final String SIZER_PREFIX = "sizer_";

	/** The Constant COLOR_PREFIX. */
	static final String COLOR_PREFIX = "color_";

	/** The icon cache. */
	Map<String, GamaIcon> iconCache = new HashMap<>();

	/** The image cache. */
	Map<String, Image> imageCache = new HashMap<>();

	/**
	 * Gets the icon.
	 *
	 * @param name
	 *            the name
	 * @return the icon
	 */
	GamaIcon getIcon(final String name) {
		return iconCache.get(name);
	}

	/**
	 * Put image in cache.
	 *
	 * @param name
	 *            the name
	 * @param image
	 *            the image
	 * @return the image
	 */
	Image putImageInCache(final String name, final Image image) {
		imageCache.put(name, image);
		return image;

	}

	/**
	 * Put icon in cache.
	 *
	 * @param name
	 *            the name
	 * @param icon
	 *            the icon
	 */
	void putIconInCache(final String name, final GamaIcon icon) {
		iconCache.put(name, icon);
	}

	/**
	 * Gets the image in cache.
	 *
	 * @param code
	 *            the code
	 * @return the image in cache
	 */
	Image getImageInCache(final String code) {
		return imageCache.get(code);
	}

	/**
	 * Creates the sizer.
	 *
	 * @param color
	 *            the color
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 * @return the gama icon
	 */
	public static GamaIcon createSizer(final Color color, final int width, final int height) {
		final String name = SIZER_PREFIX + width + "x" + height + color.hashCode();
		GamaIcon sizer = getInstance().getIcon(name);
		if (sizer == null) {
			final Image sizerImage = new Image(WorkbenchHelper.getDisplay(), width, height);
			final GC gc = new GC(sizerImage);
			gc.setBackground(color);
			gc.fillRectangle(0, 0, width, height);
			gc.dispose();
			sizer = new GamaIcon(name);
			getInstance().putImageInCache(name, sizerImage);
			getInstance().putIconInCache(name, sizer);
		}
		return sizer;
	}

	/**
	 * Creates the.
	 *
	 * @param s
	 *            the s
	 * @return the gama icon
	 */
	public static GamaIcon create(final String s) {
		return create(s, PLUGIN_ID);
	}

	/**
	 * Creates the.
	 *
	 * @param code
	 *            the code
	 * @param plugin
	 *            the plugin
	 * @return the gama icon
	 */
	private static GamaIcon create(final String code, final String plugin) {
		return create(code, code, plugin);
	}

	/**
	 * Creates the.
	 *
	 * @param code
	 *            the code
	 * @param path
	 *            the path
	 * @param plugin
	 *            the plugin
	 * @return the gama icon
	 */
	private static GamaIcon create(final String code, final String path, final String plugin) {
		GamaIcon result = getInstance().getIcon(code);
		if (result == null) {
			result = new GamaIcon(code, path, plugin);
			getInstance().putIconInCache(code, result);
		}
		return result;
	}

	/**
	 * Creates the.
	 *
	 * @param code
	 *            the code
	 * @param stream
	 *            the stream
	 * @return the gama icon
	 */
	private static GamaIcon create(final String code, final InputStream stream) {
		GamaIcon result = getInstance().getIcon(code);
		if (result == null) {
			result = new GamaIcon(code, stream);
			getInstance().putIconInCache(code, result);
		}
		return result;
	}

	/**
	 * Creates the color icon.
	 *
	 * @param s
	 *            the s
	 * @param gcolor
	 *            the gcolor
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 * @return the gama icon
	 */
	public static GamaIcon createColorIcon(final String s, final GamaUIColor gcolor, final int width,
			final int height) {
		final String name = COLOR_PREFIX + s;
		GamaIcon icon = getInstance().getIcon(s);
		if (icon == null) {
			// Color color = gcolor.color();
			// RGB c = new RGB(color.getRed(), color.getGreen(),
			// color.getBlue());
			final Image image = new Image(WorkbenchHelper.getDisplay(), width, height);
			final GC gc = new GC(image);
			gc.setAntialias(SWT.ON);
			gc.setBackground(gcolor.color());
			gc.fillRectangle(0, 0, width, height);
			gc.dispose();
			final ImageData data = image.getImageData();
			// data.transparentPixel = data.palette.getPixel(new RGB(255, 255, 255));
			icon = new GamaIcon(name);
			getInstance().putImageInCache(name, new Image(WorkbenchHelper.getDisplay(), data));
			image.dispose();
			getInstance().putIconInCache(name, icon);
		}
		return icon;
	}

	/**
	 * Creates an icon that needs to be disposed afterwards
	 *
	 * @param gcolor
	 * @param width
	 * @param height
	 * @return
	 */
	public static Image createTempColorIcon(final GamaUIColor gcolor) {

		final String name = "color" + gcolor.getRGB().toString();
		final GamaIcon icon = getInstance().getIcon(name);
		if (icon != null) return icon.image();
		int size = 16;
		ImageData imData = new ImageData(size, size, 24, new PaletteData(0xff0000, 0x00ff00, 0x0000ff));
		Image image = new Image(WorkbenchHelper.getDisplay(), imData);
		final GC gc = new GC(image);
		gc.setAntialias(SWT.ON);
		gc.setBackground(IGamaColors.GRAY.color());
		gc.fillRectangle(0, 0, size, size);
		gc.setBackground(IGamaColors.WHITE.color());
		gc.setForeground(IGamaColors.BLACK.color());

		gc.fillRoundRectangle(0, 0, size - 1, size - 1, 4, 4);
		gc.drawRoundRectangle(0, 0, size - 1, size - 1, 4, 4);
		gc.setBackground(gcolor.color());
		gc.fillRoundRectangle(size / 4, size / 4, size / 2, size / 2, 4, 4);
		// See Issue #3138 about weird artefacts in handmade icons.
		gc.dispose();

		ImageData imageData = image.getImageData();
		imageData.transparentPixel = imageData.palette.getPixel(IGamaColors.GRAY.getRGB());
		image = new Image(WorkbenchHelper.getDisplay(), imageData);
		getInstance().putImageInCache(name, image);
		getInstance().putIconInCache(name, new GamaIcon(name));
		return image;
	}

	/**
	 * Creates the temp round color icon.
	 *
	 * @param gcolor
	 *            the gcolor
	 * @return the image
	 */
	public static Image createTempRoundColorIcon(final GamaUIColor gcolor) {
		final String name = "roundcolor" + gcolor.getRGB().toString();
		final GamaIcon icon = getInstance().getIcon(name);
		if (icon != null) return icon.image();

		// The mask gray value needs to be > tolerance, < 255 - tolerance, < gcolor.red - tolerance, > gcolor.red +
		// tolerance
		int tolerance = 10;
		int gray = gcolor.getRGB().red > 127 ? 63 : 190;
		Color mask = GamaColors.get(gray, gray, gray).color();
		int size = 24;
		ImageData imData = new ImageData(size, size, 24, new PaletteData(0xff0000, 0x00ff00, 0x0000ff));
		Image image = new Image(WorkbenchHelper.getDisplay(), imData);
		final GC gc = new GC(image);
		gc.setAntialias(SWT.ON);
		gc.setBackground(mask);
		gc.fillRectangle(0, 0, size, size);
		gc.setBackground(IGamaColors.WHITE.color());
		gc.setForeground(IGamaColors.BLACK.color());
		gc.fillOval(0, 0, size - 1, size - 1);
		gc.drawOval(0, 0, size - 1, size - 1);
		gc.setBackground(gcolor.color());
		gc.fillOval(size / 4, size / 4, size / 2, size / 2);
		// See Issue #3138 about weird artefacts in handmade icons.
		gc.dispose();

		imData = image.getImageData();

		int grayPixelValue = imData.palette.getPixel(mask.getRGB());
		int[] lineData = new int[imData.width];
		for (int y = 0; y < imData.height; y++) {
			imData.getPixels(0, y, size, lineData, 0);
			// Analyze each pixel value in the line
			for (int x = 0; x < lineData.length; x++) {
				// Extract the red, green and blue component
				int p = lineData[x];
				int r = (p & 0xff0000) >> 16;
				int g = (p & 0x00ff00) >> 8;
				int b = (p & 0x0000ff) >> 0;
				if (r > gray - tolerance && r < gray + tolerance && g > gray - tolerance && g < gray + tolerance
						&& b > gray - tolerance && b < gray + tolerance) {
					imData.setPixel(x, y, grayPixelValue);
				}
			}
		}
		// mask.dispose();
		imData.transparentPixel = grayPixelValue;
		image = new Image(WorkbenchHelper.getDisplay(), imData);
		getInstance().putImageInCache(name, image);
		getInstance().putIconInCache(name, new GamaIcon(name));

		return image;
	}

	@Override
	public ImageDescriptor desc(final String name) {
		final GamaIcon icon = create(name);
		return icon.descriptor();
	}

	/**
	 * Preload icons.
	 *
	 * @param bundle
	 *            the bundle
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static void preloadIcons() {
		// CompletableFuture.runAsync(() -> {
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		final URL fileURL = bundle.getEntry(GamaIcons.DEFAULT_PATH);
		URL resolvedFileURL;
		try {
			resolvedFileURL = FileLocator.toFileURL(fileURL);
		} catch (IOException e) {
			return;
		}
		// We need to use the 3-arg constructor of URI in order to properly escape file system chars
		URI resolvedURI;
		try {
			resolvedURI = new URI(resolvedFileURL.getProtocol(), resolvedFileURL.getPath(), null).normalize();
		} catch (URISyntaxException e) {
			return;
		}
		Path path = new File(resolvedURI).toPath();
		List<String> files;
		try {
			files = Files.walk(path).map(f -> path.relativize(f).toString()).filter(n -> n.endsWith(".png")).toList();
		} catch (IOException e) {
			return;
		}
		TIMER_WITH_EXCEPTIONS(PAD("> GAMA: Preloading " + files.size() + " icons", 55, ' ') + PAD(" done in", 15, '_'),
				() -> files.forEach(n -> create(n.replace(".png", "")).image()));
		// });
	}

	/**
	 * Builds the icons.
	 */
	public static void buildIcons() {
		// CompletableFuture.runAsync(() -> {
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		final URL fileURL = bundle.getEntry(GamaIcons.SVG_PATH);
		URL resolvedFileURL;
		try {
			resolvedFileURL = FileLocator.toFileURL(fileURL);
		} catch (IOException e) {
			return;
		}
		// We need to use the 3-arg constructor of URI in order to properly escape file system chars
		URI resolvedURI;
		try {
			resolvedURI = new URI(resolvedFileURL.getProtocol(), resolvedFileURL.getPath(), null).normalize();
		} catch (URISyntaxException e) {
			return;
		}
		Path path = new File(resolvedURI).toPath();
		List<Path> files;
		try {
			files = Files.walk(path).filter(n -> n.toString().endsWith(".svg")).toList();
		} catch (IOException e) {
			return;
		}
		PNGTranscoder pt = new PNGTranscoder();
		// pt.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, 24f);
		// pt.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, 24f);
		TIMER_WITH_EXCEPTIONS(PAD("> GAMA: Preloading " + files.size() + " icons", 55, ' ') + PAD(" done in", 15, '_'),
				() -> files.forEach(n -> {
					try {

						// 1. Lire le SVG en inputStream + taille préférée (ou la mettre dans le fichier ... ?) et
						// conserver dans GamaIcon sous la forme d'un ImageDataProvider
						// 2. Fournir l'icone en tenant compte du zoom (le descripteur gardé en mémoire en fonction du
						// zoom
						// 3. Fournir en parallèle les icones décrites dans plugin.xml sous la forme de @2x...

						// Use this to Read a Local Path
						//
						// Read Remote Location for SVG
						String svgUriInputLocation = n.toUri().toURL().toString();
						TranscoderInput transcoderInput = new TranscoderInput(svgUriInputLocation);

						// Define OutputStream Location
						try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
							TranscoderOutput transcoderOutput = new TranscoderOutput(outputStream);

							// Convert SVG to PNG and Save to File System

							pt.transcode(transcoderInput, transcoderOutput);
							// Clean Up
							outputStream.flush();
							InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
							String p = path.relativize(n).toString().replace(".svg", "");
							create(p, is);
						}
					} catch (IOException | TranscoderException ex) {
						System.out.println("Exception Thrown: " + ex);
					}
				}));

	}

}
