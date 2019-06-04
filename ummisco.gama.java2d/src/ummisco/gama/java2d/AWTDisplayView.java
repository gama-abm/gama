/*********************************************************************************************
 *
 * 'AWTDisplayView.java, in plugin ummisco.gama.java2d, is part of the source code of the GAMA modeling and simulation
 * platform. (c) 2007-2016 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package ummisco.gama.java2d;

import javax.swing.JComponent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import ummisco.gama.dev.utils.DEBUG;
import ummisco.gama.java2d.swing.SwingControl;
import ummisco.gama.ui.views.displays.LayeredDisplayView;

public class AWTDisplayView extends LayeredDisplayView {

	static {
		DEBUG.ON();
	}

	@Override
	protected Composite createSurfaceComposite(final Composite parent) {

		if (getOutput() == null) { return null; }

		surfaceComposite = new SwingControl(parent, SWT.NO_FOCUS) {

			@Override
			protected JComponent createSwingComponent() {
				return (Java2DDisplaySurface) getDisplaySurface();
			}

		};
		surfaceComposite.setEnabled(false);
		WorkaroundForIssue1594.installOn(this, parent, surfaceComposite, (Java2DDisplaySurface) getDisplaySurface());
		WorkaroundForIssue2476.installOn(((SwingControl) surfaceComposite).getTopLevelContainer(), getDisplaySurface());
		WorkaroundForIssue2745.installOn(this);
		surfaceComposite.addPaintListener(e -> {
			DEBUG.OUT("-- Surface asked to repaint");
		});
		return surfaceComposite;
	}

	@Override
	public void forceLayout() {
		surfaceComposite.requestLayout();
		// final Composite parent = getSash().getParent();
		// if (parent != null && !parent.isDisposed()) {
		// parent.
		// parent.layout(true, true);
		// }
	}

}