package ca.rldesigns.casa.android.glass;

import ca.rldesigns.casa.android.glass.model.Landmarks;
import ca.rldesigns.casa.android.glass.model.Place;

import com.google.android.glass.timeline.DirectRenderingCallback;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The surface callback that provides the rendering logic for the live card. This callback also manages the lifetime of the sensor and location event
 * listeners (through {@link OrientationManager}) so that tracking only occurs when the card is visible.
 */
public class Renderer implements DirectRenderingCallback {

	private static final String TAG = "CASA";

	/**
	 * The (absolute) pitch angle beyond which the app will display a message telling the user that his or her head is at too steep an angle to be
	 * reliable.
	 */
	private static final float TOO_STEEP_PITCH_DEGREES = 70.0f;

	/** The refresh rate, in frames per second, of the app. */
	private static final int REFRESH_RATE_FPS = 45;

	/** The duration, in milliseconds, of one frame. */
	private static final long FRAME_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1) / REFRESH_RATE_FPS;

	private SurfaceHolder mHolder;
	private boolean mTooSteep;
	private boolean mInterference;
	private RenderThread mRenderThread;
	private int mSurfaceWidth;
	private int mSurfaceHeight;

	private boolean mRenderingPaused;

	private final FrameLayout mLayout;
	private final CasaView mCasaView;
	private final RelativeLayout mTipsContainer;
	private final TextView mTipsView;
	private final OrientationManager mOrientationManager;
	private final Landmarks mLandmarks;

	private final OrientationManager.OnChangedListener mCasaListener = new OrientationManager.OnChangedListener() {

		@Override
		public void onOrientationChanged(OrientationManager orientationManager) {
			mCasaView.setHeading(orientationManager.getHeading());

			boolean oldTooSteep = mTooSteep;
			mTooSteep = (Math.abs(orientationManager.getPitch()) > TOO_STEEP_PITCH_DEGREES);
			if (mTooSteep != oldTooSteep) {
				updateTipsView();
			}
		}

		@Override
		public void onLocationChanged(OrientationManager orientationManager) {
			// Location location = orientationManager.getLocation();
			Log.d(TAG, "onLocationChanged");
			// List<Place> places = mLandmarks.getNearbyLandmarks(location.getLatitude(), location.getLongitude(), 5);
			// mCasaView.setNearbyPlaces(places);
		}

		@Override
		public void onAccuracyChanged(OrientationManager orientationManager) {
			mInterference = orientationManager.hasInterference();
			Log.d(TAG, "onAccuracyChanged");
			// updateTipsView();
		}
	};

	/**
	 * Creates a new instance of the {@code Renderer} with the specified context, orientation manager, and landmark collection.
	 */
	public Renderer(Context context, OrientationManager orientationManager, Landmarks landmarks) {
		LayoutInflater inflater = LayoutInflater.from(context);
		mLayout = (FrameLayout) inflater.inflate(R.layout.main, null);
		mLayout.setWillNotDraw(false);

		mCasaView = (CasaView) mLayout.findViewById(R.id.casaView);
		mTipsContainer = (RelativeLayout) mLayout.findViewById(R.id.tips_container);
		mTipsView = (TextView) mLayout.findViewById(R.id.tips_view);

		mOrientationManager = orientationManager;
		mLandmarks = landmarks;

		mCasaView.setOrientationManager(mOrientationManager);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		doLayout();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The creation of a new Surface implicitly resumes the rendering.
		mRenderingPaused = false;
		mHolder = holder;
		updateRenderingState();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mHolder = null;
		updateRenderingState();
	}

	@Override
	public void renderingPaused(SurfaceHolder holder, boolean paused) {
		mRenderingPaused = paused;
		updateRenderingState();
	}

	/**
	 * Starts or stops rendering according to the {@link LiveCard}'s state.
	 */
	private void updateRenderingState() {
		boolean shouldRender = (mHolder != null) && !mRenderingPaused;
		boolean isRendering = (mRenderThread != null);

		if (shouldRender != isRendering) {
			if (shouldRender) {
				mOrientationManager.addOnChangedListener(mCasaListener);
				mOrientationManager.start();

				if (mOrientationManager.hasLocation()) {
					// Location location = mOrientationManager.getLocation();
					Log.d(TAG, "updateRenderingState");
					List<Place> nearbyPlaces = mLandmarks.getNearbyLandmarks(5);
					mCasaView.setNearbyPlaces(nearbyPlaces);
				}

				mRenderThread = new RenderThread();
				mRenderThread.start();
			} else {
				mRenderThread.quit();
				mRenderThread = null;

				mOrientationManager.removeOnChangedListener(mCasaListener);
				mOrientationManager.stop();
			}
		}
	}

	/**
	 * Requests that the views redo their layout. This must be called manually every time the tips view's text is updated because this layout doesn't
	 * exist in a GUI thread where those requests will be enqueued automatically.
	 */
	private void doLayout() {
		// Measure and update the layout so that it will take up the entire surface space
		// when it is drawn.
		int measuredWidth = View.MeasureSpec.makeMeasureSpec(mSurfaceWidth, View.MeasureSpec.EXACTLY);
		int measuredHeight = View.MeasureSpec.makeMeasureSpec(mSurfaceHeight, View.MeasureSpec.EXACTLY);

		mLayout.measure(measuredWidth, measuredHeight);
		mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
	}

	/**
	 * Repaints the canvas.
	 */
	private synchronized void repaint() {
		Canvas canvas = null;

		try {
			canvas = mHolder.lockCanvas();
		} catch (RuntimeException e) {
			Log.d(TAG, "lockCanvas failed", e);
		}

		if (canvas != null) {
			canvas.drawColor(Color.BLACK);
			mLayout.draw(canvas);

			try {
				mHolder.unlockCanvasAndPost(canvas);
			} catch (RuntimeException e) {
				Log.d(TAG, "unlockCanvasAndPost failed", e);
			}
		}
	}

	/**
	 * Shows or hides the tip view with an appropriate message based on the current accuracy.
	 */
	private void updateTipsView() {
		int stringId = 0;

		// Only one message (with magnetic interference being higher priority than pitch too steep)
		// will be displayed in the tip.
		if (mInterference) {
			stringId = R.string.magnetic_interference;
		} else if (mTooSteep) {
			stringId = R.string.pitch_too_steep;
		}

		boolean show = (stringId != 0);

		if (show) {
			mTipsView.setText(stringId);
			doLayout();
		}

		if (mTipsContainer.getAnimation() == null) {
			float newAlpha = (show ? 1.0f : 0.0f);
			mTipsContainer.animate().alpha(newAlpha).start();
		}
	}

	/**
	 * Redraws the background.
	 */
	private class RenderThread extends Thread {
		private boolean mShouldRun;

		/**
		 * Initializes the background rendering thread.
		 */
		public RenderThread() {
			mShouldRun = true;
		}

		/**
		 * Returns true if the rendering thread should continue to run.
		 * 
		 * @return true if the rendering thread should continue to run
		 */
		private synchronized boolean shouldRun() {
			return mShouldRun;
		}

		/**
		 * Requests that the rendering thread exit at the next opportunity.
		 */
		public synchronized void quit() {
			mShouldRun = false;
		}

		@Override
		public void run() {
			while (shouldRun()) {
				long frameStart = SystemClock.elapsedRealtime();
				repaint();
				long frameLength = SystemClock.elapsedRealtime() - frameStart;

				long sleepTime = FRAME_TIME_MILLIS - frameLength;
				if (sleepTime > 0) {
					SystemClock.sleep(sleepTime);
				}
			}
		}
	}
}