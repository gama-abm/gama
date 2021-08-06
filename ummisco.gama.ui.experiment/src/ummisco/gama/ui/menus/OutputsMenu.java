package ummisco.gama.ui.menus;

import static ummisco.gama.ui.resources.GamaIcons.create;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.google.common.collect.Iterables;

import msi.gama.kernel.experiment.ExperimentAgent;
import msi.gama.kernel.experiment.IExperimentPlan;
import msi.gama.kernel.simulation.SimulationAgent;
import msi.gama.outputs.IDisplayOutput;
import msi.gama.outputs.IOutputManager;
import msi.gama.outputs.LayeredDisplayOutput;
import msi.gama.runtime.GAMA;
import msi.gama.runtime.IScope;
import ummisco.gama.ui.resources.IGamaIcons;

public class OutputsMenu extends ContributionItem {

	@FunctionalInterface
	private interface ISelecter extends SelectionListener {

		@Override
		default void widgetDefaultSelected(final SelectionEvent e) {
			widgetSelected(e);
		}

	}

	public OutputsMenu() {
		super("ummisco.gama.ui.experiment.outputs.menu");
	}

	@Override
	public void fill(final Menu main, final int index) {
		IExperimentPlan exp = GAMA.getExperiment();
		if (exp == null) return;
		ExperimentAgent agent = exp.getAgent();
		if (agent == null) return;
		for (final SimulationAgent sim : agent.getSimulationPopulation()) {
			managementSubMenu(main, sim.getScope(), sim.getOutputManager());
		}
		GamaMenu.separate(main);
		menuItem(main, e -> GAMA.getExperiment().pauseAllOutputs(), create(IGamaIcons.DISPLAY_TOOLBAR_PAUSE).image(),
				"Pause all");
		menuItem(main, e -> GAMA.getExperiment().refreshAllOutputs(), create("action.update2").image(), "Update all");
		menuItem(main, e -> GAMA.getExperiment().resumeAllOutputs(),
				create(IGamaIcons.DISPLAY_TOOLBAR_PAUSE).disabled(), "Resume all");
		menuItem(main, e -> GAMA.getExperiment().synchronizeAllOutputs(),
				create(IGamaIcons.DISPLAY_TOOLBAR_SYNC).image(), "Synchronize all");
		menuItem(main, e -> GAMA.getExperiment().unSynchronizeAllOutputs(),
				create(IGamaIcons.DISPLAY_TOOLBAR_SYNC).disabled(), "Desynchronize all");
	}

	public void managementSubMenu(final Menu main, final IScope scope, final IOutputManager manager) {
		if (Iterables.isEmpty(manager.getDisplayOutputs())) return;
		final MenuItem item = new MenuItem(main, SWT.CASCADE);
		item.setText(manager.toString());
		final Menu sub = new Menu(item);
		item.setMenu(sub);
		for (final IDisplayOutput output : manager.getDisplayOutputs()) {
			outputSubMenu(sub, scope, manager, output);
		}
	}

	public void outputSubMenu(final Menu main, final IScope scope, final IOutputManager manager,
			final IDisplayOutput output) {
		final MenuItem item = new MenuItem(main, SWT.CASCADE);
		item.setText(output.getOriginalName());
		final Menu sub = new Menu(item);
		item.setMenu(sub);
		if (output.isOpen()) {
			// menuItem(sub, e -> output.close(), null, "Close");
			if (output.isPaused()) {
				menuItem(sub, e -> output.setPaused(false), null, "Resume");
			} else {
				menuItem(sub, e -> output.setPaused(true), create(IGamaIcons.DISPLAY_TOOLBAR_PAUSE).image(), "Pause");
			}
			menuItem(sub, e -> output.update(), create("action.update2").image(), "Force update");
			if (output.isSynchronized()) {
				menuItem(sub, e -> output.setSynchronized(false), create(IGamaIcons.DISPLAY_TOOLBAR_SYNC).image(),
						"Desynchronize");
			} else {
				menuItem(sub, e -> output.setSynchronized(true), create(IGamaIcons.DISPLAY_TOOLBAR_SYNC).image(),
						"Synchronize");
			}
			if (output instanceof LayeredDisplayOutput) {
				LayeredDisplayOutput ldo = (LayeredDisplayOutput) output;
				GamaMenu.separate(sub);
				menuItem(sub, e -> ldo.zoom(1), create(IGamaIcons.DISPLAY_TOOLBAR_ZOOMIN).image(), "Zoom in");
				menuItem(sub, e -> ldo.zoom(0), create(IGamaIcons.DISPLAY_TOOLBAR_ZOOMFIT).image(), "Zoom to fit view");
				menuItem(sub, e -> ldo.zoom(-1), create(IGamaIcons.DISPLAY_TOOLBAR_ZOOMOUT).image(), "Zoom out");
				GamaMenu.separate(sub);
				menuItem(sub, e -> ldo.getView().takeSnapshot(), create(IGamaIcons.DISPLAY_TOOLBAR_SNAPSHOT).image(),
						"Take a snapshot");
				menuItem(sub, e -> ldo.getView().toggleFullScreen(), create("display.fullscreen2").image(),
						"Toggle fullscreen");
			}
		} else {
			menuItem(sub, e -> manager.open(scope, output), null, "Reopen");
		}

	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	private static MenuItem menuItem(final Menu parent, final ISelecter listener, final Image image,
			final String prefix) {
		final MenuItem result = new MenuItem(parent, SWT.PUSH);
		result.setText(prefix);
		if (listener != null) { result.addSelectionListener(listener); }
		if (image != null) { result.setImage(image); }
		return result;
	}
}
