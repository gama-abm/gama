/*********************************************************************************************
 *
 * 'SWTGLAnimator.java, in plugin ummisco.gama.opengl, is part of the source code of the GAMA modeling and simulation
 * platform. (v. 1.8.1)
 *
 * (c) 2007-2020 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package ummisco.gama.opengl.view;

import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;

import msi.gama.common.preferences.GamaPreferences;
import ummisco.gama.dev.utils.DEBUG;

/**
 * Simple Animator (with target FPS)
 *
 * @author AqD (aqd@5star.com.tw)
 */
public class SingleThreadGLAnimator implements Runnable, GLAnimatorControl, GLAnimatorControl.UncaughtExceptionHandler {

	static {
		// DEBUG.ON();
	}

	protected boolean capFPS = GamaPreferences.Displays.OPENGL_CAP_FPS.getValue();
	protected int targetFPS = GamaPreferences.Displays.OPENGL_FPS.getValue();
	protected final Thread animatorThread;
	protected final GLAutoDrawable drawable;

	protected volatile boolean stopRequested = false;
	protected volatile boolean pauseRequested = false;
	protected volatile boolean animating = false;
	protected volatile long startingTime, lastUpdateTime, fpsPeriod;
	Semaphore pause = new Semaphore(0);

	protected int frames = 0;

	public SingleThreadGLAnimator(final GLAutoDrawable drawable) {
		GamaPreferences.Displays.OPENGL_FPS.onChange(newValue -> targetFPS = newValue);
		this.drawable = drawable;
		drawable.setAnimator(this);
		this.animatorThread = new Thread(this, "Animator thread");
	}

	@Override
	public void setUpdateFPSFrames(final int frames, final PrintStream out) {}

	@Override
	public void resetFPSCounter() {
		startingTime = System.currentTimeMillis();
	}

	@Override
	public int getUpdateFPSFrames() {
		return 0;
	}

	@Override
	public long getFPSStartTime() {
		return startingTime;
	}

	@Override
	public long getLastFPSUpdateTime() {
		return lastUpdateTime;
	}

	@Override
	public long getLastFPSPeriod() {
		return fpsPeriod;
	}

	@Override
	public float getLastFPS() {
		return frames / (getTotalFPSDuration() / 1000f);
	}

	@Override
	public int getTotalFPSFrames() {
		return frames;
	}

	@Override
	public long getTotalFPSDuration() {
		return lastUpdateTime - startingTime;
	}

	@Override
	public float getTotalFPS() {
		return frames / ((System.currentTimeMillis() - startingTime) / 1000f);
	}

	@Override
	public boolean isStarted() {
		return this.animatorThread.isAlive();
	}

	@Override
	public boolean isAnimating() {
		return this.animating && !pauseRequested;
	}

	@Override
	public boolean isPaused() {
		return isStarted() && pauseRequested;
	}

	@Override
	public Thread getThread() {
		return this.animatorThread;
	}

	@Override
	public boolean start() {
		this.stopRequested = false;
		this.pauseRequested = false;
		this.animatorThread.start();
		startingTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public boolean stop() {
		this.stopRequested = true;
		try {
			pause.release();
			this.animatorThread.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} finally {
			this.stopRequested = false;
		}
		return true;
	}

	@Override
	public boolean pause() {
		pauseRequested = true;
		return true;
	}

	@Override
	public boolean resume() {
		pause.release();
		return true;
	}

	@Override
	public void add(final GLAutoDrawable drawable) {}

	@Override
	public void remove(final GLAutoDrawable drawable) {}

	@Override
	public void run() {
		while (!this.stopRequested) {
			if (pauseRequested) {
				try {
					pause.drainPermits();
					pause.acquire();
					pauseRequested = false;
				} catch (InterruptedException e1) {}
			}

			final long timeBegin = System.currentTimeMillis();
			this.displayGL();
			frames++;
			lastUpdateTime = System.currentTimeMillis();
			fpsPeriod = lastUpdateTime - timeBegin;
			// DEBUG.OUT("Animator main loop in " + fpsPeriod + "ms");
			if (capFPS) {
				final long frameDuration = 1000 / targetFPS;
				final long timeSleep = frameDuration - fpsPeriod;
				try {
					if (timeSleep >= 0) { Thread.sleep(timeSleep); }
				} catch (final InterruptedException e) {}
			}
		}
	}

	protected void displayGL() {
		this.animating = true;
		try {
			if (drawable.isRealized()) { drawable.display(); }
		} catch (final RuntimeException ex) {
			DEBUG.ERR("Exception in OpenGL:" + ex.getMessage());
			ex.printStackTrace();
		} finally {
			this.animating = false;
		}
	}

	@Override
	public UncaughtExceptionHandler getUncaughtExceptionHandler() {
		return this;
	}

	@Override
	public void setUncaughtExceptionHandler(final UncaughtExceptionHandler handler) {}

	@Override
	public void uncaughtException(final GLAnimatorControl animator, final GLAutoDrawable drawable,
			final Throwable cause) {
		DEBUG.ERR("Uncaught exception in animator & drawable:");
		cause.printStackTrace();

	}
}