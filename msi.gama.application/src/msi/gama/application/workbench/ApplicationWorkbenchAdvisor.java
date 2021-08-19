/*********************************************************************************************
 *
 * 'ApplicationWorkbenchAdvisor.java, in plugin msi.gama.application, is part of the source code of the GAMA modeling
 * and simulation platform. (v. 1.8.1)
 *
 * (c) 2007-2020 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package msi.gama.application.workbench;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.PluginActionBuilder;
import org.eclipse.ui.internal.ide.application.IDEWorkbenchAdvisor;
import org.eclipse.ui.statushandlers.AbstractStatusHandler;
import org.eclipse.ui.statushandlers.StatusAdapter;

import msi.gama.application.Application;
import msi.gama.application.workspace.WorkspaceModelsManager;
import msi.gama.common.interfaces.IEventLayerDelegate;
import msi.gama.common.interfaces.IGui;
import msi.gama.common.util.FileUtils;
import msi.gama.outputs.layers.EventLayerStatement;
import msi.gama.runtime.GAMA;
import msi.gama.runtime.concurrent.GamaExecutorService;
import ummisco.gama.dev.utils.DEBUG;

public class ApplicationWorkbenchAdvisor extends IDEWorkbenchAdvisor {

	{
		DEBUG.OFF();
	}

	public ApplicationWorkbenchAdvisor() {
		super(Application.processor);
	}

	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(final IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(this, configurer);
	}

	@Override
	public void initialize(final IWorkbenchConfigurer configurer) {

		ResourcesPlugin.getPlugin().getStateLocation();
		try {
			super.initialize(configurer);

			IDE.registerAdapters();
			configurer.setSaveAndRestore(true);

			final IDecoratorManager dm = configurer.getWorkbench().getDecoratorManager();
			dm.setEnabled("org.eclipse.pde.ui.binaryProjectDecorator", false);
			dm.setEnabled("org.eclipse.team.svn.ui.decorator.SVNLightweightDecorator", false);
			dm.setEnabled("msi.gama.application.decorator", true);
			dm.setEnabled("org.eclipse.ui.LinkedResourceDecorator", false);
			dm.setEnabled("org.eclipse.ui.VirtualResourceDecorator", false);
			dm.setEnabled("org.eclipse.xtext.builder.nature.overlay", false);
			if (Display.getCurrent() != null) {
				Display.getCurrent().getThread().setUncaughtExceptionHandler(GamaExecutorService.EXCEPTION_HANDLER);
			}
		} catch (final CoreException e) {
			// e.printStackTrace();
		}
		PluginActionBuilder.setAllowIdeLogging(false);
		ThemeHelper.install();
	}

	@Override
	public void postStartup() {
		super.postStartup();
		FileUtils.cleanCache();
		final String[] args = Platform.getApplicationArgs();
		if (false) { DEBUG.LOG("Arguments received by GAMA : " + Arrays.toString(args)); }
		if (args.length > 0 && args[0].contains("launcher.defaultAction")
				&& !args[0].contains("--launcher.defaultAction"))
			return;
		if (args.length >= 1) {

			if (args[args.length - 1].endsWith(".gamr")) {
				for (final IEventLayerDelegate delegate : EventLayerStatement.delegates) {
					if (delegate.acceptSource(null, "launcher")) {
						delegate.createFrom(null, args[args.length - 1], null);
					}
				}
			} else {
				WorkspaceModelsManager.instance.openModelPassedAsArgument(args[args.length - 1]);
			}
		}
	}

	protected boolean checkCopyOfBuiltInModels() {

		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject[] projects = workspace.getRoot().getProjects();
		// If no projects are registered at all, we are facing a fresh new workspace
		if (projects.length == 0) return true;
		return false;
		// Following is not ready for prime time !
		// // If there are projects, we must be careful to distinguish user projects from built-in projects
		// List<IProject> builtInProjects = new ArrayList<>();
		// for ( IProject p : projects ) {
		// try {
		// // Assumption here : a non-accessible / linked project means a built-in model that is not accessible
		// // anymore. Maybe false sometimes... But how to check ?
		// DEBUG.OUT("Project = " + p.getName());
		// DEBUG.OUT(" ==== > Accessible : " + p.isAccessible());
		// DEBUG.OUT(" ==== > Open : " + p.isOpen());
		// DEBUG.OUT(" ==== > Linked : " + p.isLinked());
		// if ( !p.isAccessible() && p.isLinked() ) {
		// builtInProjects.add(p);
		// } else if ( p.isOpen() && p.getPersistentProperty(BUILTIN_PROPERTY) != null ) {
		// builtInProjects.add(p);
		// }
		// } catch (CoreException e) {
		// e.printStackTrace();
		// }
		// }
		// if ( builtInProjects.isEmpty() ) {
		// // only user projects there
		// return true;
		// }
		// String workspaceStamp = null;
		// try {
		// workspaceStamp = workspace.getRoot().getPersistentProperty(BUILTIN_PROPERTY);
		// DEBUG.OUT("Version of the models in workspace = " + workspaceStamp);
		// } catch (CoreException e) {
		// e.printStackTrace();
		// }
		// String gamaStamp = getCurrentGamaStampString();
		// // We dont know when the builtin models have been created -- there is probably a problem, but we do not try
		// to
		// // solve it
		// if ( gamaStamp == null ) {
		// DEBUG.ERR("Problem when trying to gather the date of creation of built-in models");
		// return false;
		// }
		// if ( gamaStamp.equals(workspaceStamp) ) {
		// // It's ok. The models in the workspace and in GAMA have the same time stamp
		// return false;
		// }
		// // We now have to (1) ask the user if he/she wants to update the models
		// boolean create =
		// MessageDialog
		// .openConfirm(
		// Display.getDefault().getActiveShell(),
		// "Update the models library",
		// "A new version of the built-in library of models is available. Would you like to update the ones present in
		// the workspace?");
		// // (2) erase the built-in projects from the workspace
		// if ( !create ) { return false; }
		// for ( IProject p : builtInProjects ) {
		// try {
		// p.delete(true, null);
		// } catch (CoreException e) {
		// e.printStackTrace();
		// }
		// }
		// return true;
	}

	@Override
	public String getInitialWindowPerspectiveId() {
		return IGui.PERSPECTIVE_MODELING_ID;
	}

	/**
	 * A workbench pre-shutdown method calls to prompt a confirmation of the shutdown and perform a saving of the
	 * workspace
	 */
	@Override
	public boolean preShutdown() {
		try {
			// saveEclipsePreferences();
			GAMA.closeAllExperiments(true, true);
			PerspectiveHelper.deleteCurrentSimulationPerspective();
			// So that they are not saved to the workbench.xmi file
			PerspectiveHelper.cleanPerspectives();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return super.preShutdown();

	}

	@Override
	public void postShutdown() {
		try {
			super.postShutdown();
		} catch (final Exception e) {
			// Remove the trace of exceptions
			// e.printStackTrace();
		}
	}

	@Override
	public void preStartup() {
		// Suspend background jobs while we startup
		Job.getJobManager().suspend();
		// super.preStartup();
		/* Linking the stock models with the workspace if they are not already */
		if (checkCopyOfBuiltInModels()) { WorkspaceModelsManager.linkSampleModelsToWorkspace(); }

	}
	//
	// private void saveEclipsePreferences() {
	// final IPreferencesService service = Platform.getPreferencesService();
	//
	// try (final FileOutputStream outputStream =
	// new FileOutputStream(Platform.getInstanceLocation().getURL().getPath() + "/.gama.epf")) {
	// service.exportPreferences(service.getRootNode(), WorkspacePreferences.getPreferenceFilters(), outputStream);
	// } catch (final CoreException | IOException e1) {}
	//
	// }

	/**
	 * Method getWorkbenchErrorHandler()
	 *
	 * @see org.eclipse.ui.internal.ide.application.IDEWorkbenchAdvisor#getWorkbenchErrorHandler()
	 */
	@Override
	public synchronized AbstractStatusHandler getWorkbenchErrorHandler() {
		return new AbstractStatusHandler() {

			@Override
			public void handle(final StatusAdapter statusAdapter, final int style) {
				final int severity = statusAdapter.getStatus().getSeverity();
				if (severity == IStatus.INFO || severity == IStatus.CANCEL) return;
				final Throwable e = statusAdapter.getStatus().getException();
				if (e instanceof OutOfMemoryError) {
					GamaExecutorService.EXCEPTION_HANDLER.uncaughtException(Thread.currentThread(), e);
				}
				final String message = statusAdapter.getStatus().getMessage();
				// Stupid Eclipse
				if (!message.contains("File toolbar contribution item") && !message.contains("Duplicate template id")) {
					DEBUG.OUT("GAMA Caught a workbench message : " + message);
				}
				if (e != null) { e.printStackTrace(); }
			}
		};
	}

}
