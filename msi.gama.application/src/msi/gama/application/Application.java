/*********************************************************************************************
 *
 * 'Application.java, in plugin msi.gama.application, is part of the source code of the
 * GAMA modeling and simulation platform.
 * (v. 1.8.1)
 *
 * (c) 2007-2020 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package msi.gama.application;

import static java.lang.System.setProperty;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.application.DelayedEventsProcessor;
import org.eclipse.ui.internal.util.PrefUtil;
import msi.gama.application.workbench.ApplicationWorkbenchAdvisor;
import msi.gama.application.workspace.PickWorkspaceDialog;
import msi.gama.application.workspace.WorkspaceModelsManager;
import msi.gama.application.workspace.WorkspacePreferences;
import ummisco.gama.dev.utils.DEBUG;

/** This class controls all aspects of the application's execution */
public class Application implements IApplication {

	{
		DEBUG.OFF();
	}

	public static OpenDocumentEventProcessor processor;

	public static final String CLEAR_WORKSPACE = "clearWorkspace";

	public static void ClearWorkspace(final boolean clear) {
		PrefUtil.getInternalPreferenceStore().setValue(CLEAR_WORKSPACE, Boolean.valueOf(clear).toString());
		PrefUtil.saveInternalPrefs();
	}

	public static boolean IsClearWorkspace() {
		final boolean result = PrefUtil.getInternalPreferenceStore().getBoolean(CLEAR_WORKSPACE);
		DEBUG.OUT("Value of clearWorkspace pref: " + result);
		return result;
	}

	public static class OpenDocumentEventProcessor extends DelayedEventsProcessor {

		OpenDocumentEventProcessor(final Display display) {
			super(display);
		}

		private final ArrayList<String> filesToOpen = new ArrayList<>(1);

		@Override
		public void handleEvent(final Event event) {
			if ( event.text != null ) {
				filesToOpen.add(event.text);
				DEBUG.OUT("RECEIVED FILE TO OPEN: " + event.text);
			}
		}

		@Override
		public void catchUp(final Display display) {
			if ( filesToOpen.isEmpty() ) { return; }

			final String[] filePaths = filesToOpen.toArray(new String[filesToOpen.size()]);
			filesToOpen.clear();

			for ( final String path : filePaths ) {
				WorkspaceModelsManager.instance.openModelPassedAsArgument(path);
			}
		}
	}

	public static void createProcessor() {
		final Display display = Display.getDefault();
		if ( display == null ) { return; }
		processor = new OpenDocumentEventProcessor(display);
	}

	@Override
	public Object start(final IApplicationContext context) throws Exception {

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			if ( e instanceof OutOfMemoryError ) {
				final boolean close = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Out of memory",
					"GAMA is out of memory and will likely crash. Do you want to close now ?");
				if ( close ) {
					this.stop();
				}
				e.printStackTrace();
			}

		});
		Display.setAppName("Gama Platform");
		Display.setAppVersion("1.8.1");
		// DEBUG.OUT(System.getProperties());
		createProcessor();
		final Object check = checkWorkspace();
		if ( EXIT_OK.equals(check) ) { return EXIT_OK; }
		// if ( check == EXIT_RESTART ) {
		// ClearWorkspace(true);
		// No need to restart : the value will be checked later
		// return EXIT_RESTART;
		// }
		Display display = null;
		try {
			display = Display.getDefault();
			checkWorkbenchXMI();
			// final String splash = getProperty("org.eclipse.equinox.launcher.splash.location");
			// if ( splash != null ) {
			// setProperty("org.eclipse.equinox.launcher.splash.location", splash.replace("bmp", "png"));
			// }
			final int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
			if ( returnCode == PlatformUI.RETURN_RESTART ) { return IApplication.EXIT_RESTART; }
			return IApplication.EXIT_OK;
		} finally {
			if ( display != null ) {
				display.dispose();
			}
			final Location instanceLoc = Platform.getInstanceLocation();
			if ( instanceLoc != null ) {
				instanceLoc.release();
			}
		}

	}

	private void checkWorkbenchXMI() {
		final boolean removeWorkbenchXMI = IsClearWorkspace();
		if ( removeWorkbenchXMI ) {
			setProperty(org.eclipse.e4.ui.workbench.IWorkbench.CLEAR_PERSISTED_STATE, "true");
			ClearWorkspace(false);
		}

	}

	public static Object checkWorkspace() throws IOException, MalformedURLException {
		final Location instanceLoc = Platform.getInstanceLocation();
		if ( instanceLoc == null ) {
			// -data @none was specified but GAMA requires a workspace
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
				"A workspace is required to run GAMA");
			return EXIT_OK;
		}
		boolean remember = false;
		String lastUsedWs = null;
		if ( instanceLoc.isSet() ) {
			lastUsedWs = instanceLoc.getURL().getFile();
			final String ret = WorkspacePreferences.checkWorkspaceDirectory(lastUsedWs, false, false, false);
			if ( ret != null ) {
				// if ( ret.equals("Restart") ) { return EXIT_RESTART; }
				/* If we dont or cant remember and the location is set, we cant do anything as we need a workspace */
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"The workspace provided cannot be used. Please change it");
				if ( PlatformUI.isWorkbenchRunning() ) {
					PlatformUI.getWorkbench().close();
				}
				System.exit(0);
				return EXIT_OK;
			}
		} else {

			/* Get what the user last said about remembering the workspace location */
			remember = WorkspacePreferences.isRememberWorkspace();
			/* Get the last used workspace location */
			lastUsedWs = WorkspacePreferences.getLastSetWorkspaceDirectory();
			/* If we have a "remember" but no last used workspace, it's not much to remember */
			if ( remember && (lastUsedWs == null || lastUsedWs.length() == 0) ) {
				remember = false;
			}
			if ( remember ) {
				/*
				 * If there's any problem with the workspace, force a dialog
				 */
				final String ret = WorkspacePreferences.checkWorkspaceDirectory(lastUsedWs, false, false, false);
				if ( ret != null ) {
					// if ( ret.equals("Restart") ) { return EXIT_RESTART; }
					if ( ret.equals("models") ) {
						final MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(),
							"Different version of the models library",
							Display.getCurrent().getSystemImage(SWT.ICON_QUESTION),
							"The workspace contains a different version of the models library. Do you want to use another workspace ?",
							MessageDialog.QUESTION, 1, "Use another workspace", "No, thanks");
						remember = dialog.open() == 1;

					} else {
						remember = false;
					}
				}
			}
		}

		/* If we don't remember the workspace, show the dialog */
		if ( !remember ) {
			final int pick = new PickWorkspaceDialog().open();
			/* If the user cancelled, we can't do anything as we need a workspace */
			if ( pick == 1 /* Window.CANCEL */ && WorkspacePreferences.getSelectedWorkspaceRootLocation() == null ) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"The application can not start without a workspace and will now exit.");
				System.exit(0);
				return IApplication.EXIT_OK;
			}
			/* Tell Eclipse what the selected location was and continue */
			instanceLoc.set(new URL("file", null, WorkspacePreferences.getSelectedWorkspaceRootLocation()), false);
			if ( WorkspacePreferences.applyPrefs() ) {
				WorkspacePreferences.applyEclipsePreferences(WorkspacePreferences.getSelectedWorkspaceRootLocation());
			}
		} else {
			if ( !instanceLoc.isSet() ) {
				/* Set the last used location and continue */
				instanceLoc.set(new URL("file", null, lastUsedWs), false);
			}

		}

		return null;
	}

	@Override
	public void stop() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if ( workbench == null ) { return; }
		final Display display = workbench.getDisplay();
		display.syncExec(() -> {
			if ( !display.isDisposed() ) {
				workbench.close();
			}
		});
	}

}
