package ca.rldesigns.casa.android.glass;

import static ca.rldesigns.casa.android.glass.ApplicationData.DATABASE_NAME;

import java.util.Calendar;

import ca.rldesigns.casa.android.glass.model.Landmarks;
import ca.rldesigns.casa.android.glass.model.SavedPreference;
import ca.rldesigns.casa.android.glass.util.ActionParams;
import ca.rldesigns.casa.android.glass.util.MathUtils;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.gms.maps.model.LatLng;
import com.google.zxing.client.glass.CaptureActivity;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

/**
 * The main application service that manages the lifetime of the compass live card and the objects that help out with orientation tracking and
 * landmarks.
 */
public class CasaService extends Service {

	private static final String TAG = "CASA";

	private SharedPreferences savedSettings;

	private static final String LIVE_CARD_TAG = "casa";
	private final CasaBinder mBinder = new CasaBinder();

	private OrientationManager mOrientationManager;
	private Landmarks mLandmarks;
	private TextToSpeech mSpeech;

	private LiveCard mLiveCard;
	private Renderer mRenderer;

	/**
	 * A binder that gives other components access to the speech capabilities provided by the service.
	 */
	public class CasaBinder extends Binder {

		public CasaService getService() {
			return CasaService.this;
		}

		/**
		 * Read the current heading aloud using the text-to-speech engine.
		 */
		public void readHeadingAloud() {
			float heading = mOrientationManager.getHeading();

			Resources res = getResources();
			String[] spokenDirections = res.getStringArray(R.array.spoken_directions);
			String directionName = spokenDirections[MathUtils.getHalfWindIndex(heading)];

			int roundedHeading = Math.round(heading);
			int headingFormat;
			if (roundedHeading == 1) {
				headingFormat = R.string.spoken_heading_format_one;
			} else {
				headingFormat = R.string.spoken_heading_format;
			}

			String headingText = res.getString(headingFormat, roundedHeading, directionName);
			mSpeech.speak(headingText, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("CASA", "onCreate Service");
		// Even though the text-to-speech engine is only used in response to a menu action, we
		// initialize it when the application starts so that we avoid delays that could occur
		// if we waited until it was needed to start it up.
		mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				// Do nothing.
			}
		});

		SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		mOrientationManager = new OrientationManager(sensorManager, locationManager);

		mLandmarks = new Landmarks(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mLiveCard == null) {
			Log.d("CASA", "onStartCommand");

			mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
			mRenderer = new Renderer(this, mOrientationManager, mLandmarks);
			mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mRenderer);

			// Display the options menu when the live card is tapped.
			Intent menuIntent = new Intent(this, MenuActivity.class);
			menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
			mLiveCard.attach(this);
			mLiveCard.publish(PublishMode.REVEAL);
			savedSettings = getSharedPreferences(DATABASE_NAME, 0);
			boolean setupDone = savedSettings.getBoolean(ApplicationData.SETUP_DONE, false);

			if (!setupDone) {
				Intent intentGetSettings = new Intent(getBaseContext(), CaptureActivity.class);
				intentGetSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getApplication().startActivity(intentGetSettings);
			} else {
				loadSettings();
				Log.d(TAG, "Settings found");
			
				mLandmarks = new Landmarks(this, ActionParams.SelectedLatLng);
			}
		} else {
			mLiveCard.navigate();
		}

		return START_STICKY;
	}

	private void loadSettings() {
		double lat = (double) savedSettings.getFloat(ApplicationData.SELECTED_LAT, 0);
		double lng = (double) savedSettings.getFloat(ApplicationData.SELECTED_LNG, 0);
		ActionParams.SelectedLatLng = new LatLng(lat, lng);

		Calendar cal = Calendar.getInstance();
		long oneYear = (long) 3.154E10;
		cal.setTimeInMillis(cal.getTimeInMillis() - oneYear);
		int year = cal.get(Calendar.YEAR);
		int monthOfYear = cal.get(Calendar.MONTH);
		int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		String defaultDate = Integer.toString(monthOfYear) + "/" + Integer.toString(dayOfMonth) + "/" + Integer.toString(year);
		ActionParams.Date = savedSettings.getString(ApplicationData.START_DATE, defaultDate);

		ActionParams.PriceMinValue = Integer.toString(savedSettings.getInt(ApplicationData.PRICE_MIN, 0));
		ActionParams.PriceMaxValue = Integer.toString(savedSettings.getInt(ApplicationData.PRICE_MAX, 0));
		ActionParams.BedroomMinValue = Integer.toString(savedSettings.getInt(ApplicationData.BEDROOM_MIN, 0));
		ActionParams.BedroomMaxValue = Integer.toString(savedSettings.getInt(ApplicationData.BEDROOM_MAX, 0));
		ActionParams.BathroomMinValue = Integer.toString(savedSettings.getInt(ApplicationData.BATHROOM_MIN, 0));
		ActionParams.BathroomMaxValue = Integer.toString(savedSettings.getInt(ApplicationData.BATHROOM_MAX, 0));
		ActionParams.StoriesMinValue = Integer.toString(savedSettings.getInt(ApplicationData.STORIES_MIN, 0));
		ActionParams.StoriesMaxValue = Integer.toString(savedSettings.getInt(ApplicationData.STORIES_MAX, 0));

		SavedPreference savedPreference = new SavedPreference(ActionParams.SelectedLatLng, ActionParams.Date, ActionParams.PriceMinValue,
				ActionParams.PriceMaxValue, ActionParams.BedroomMinValue, ActionParams.BedroomMaxValue, ActionParams.BathroomMinValue,
				ActionParams.BathroomMaxValue, ActionParams.StoriesMinValue, ActionParams.StoriesMaxValue);

		ActionParams.savedPreference = savedPreference;
	}

	public void startLandmarks() {
		mLandmarks = new Landmarks(this, ActionParams.SelectedLatLng);
	}

	@Override
	public void onDestroy() {
		Log.d("CASA", "onDestroy");

		if (mLiveCard != null && mLiveCard.isPublished()) {
			mLiveCard.unpublish();
			mLiveCard = null;
		}

		mSpeech.shutdown();

		mSpeech = null;
		mOrientationManager = null;
		mLandmarks = null;

		super.onDestroy();
	}
}