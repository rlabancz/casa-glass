package ca.rldesigns.casa.android.glass;

import static ca.rldesigns.casa.android.glass.ApplicationData.DATABASE_NAME;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.Runnable;
import java.util.Calendar;

import com.google.zxing.client.glass.CaptureActivity;

import ca.rldesigns.casa.android.glass.model.Landmarks;
import ca.rldesigns.casa.android.glass.util.ActionParams;
import ca.rldesigns.casa.android.glass.util.RequestCodes;
import ca.rldesigns.casa.android.glass.util.ResultCodes;

/**
 * This activity manages the options menu that appears when the user taps on the compass's live card.
 */
public class MenuActivity extends Activity {

	private final Handler mHandler = new Handler();

	private CasaService.CasaBinder mCompassService;
	private boolean mAttachedToWindow;
	private boolean mOptionsMenuOpen;
	public Menu menu;
	public Landmarks mLandmarks;
	private SharedPreferences savedSettings;

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d("CASA", "onService");
			if (service instanceof CasaService.CasaBinder) {
				mCompassService = (CasaService.CasaBinder) service;
				openOptionsMenu();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// Do nothing.
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		savedSettings = getSharedPreferences(DATABASE_NAME, 0);
		Log.d("CASA", "onCreate");
		bindService(new Intent(this, CasaService.class), mConnection, 0);
	}

	private void loadSettings() {
		boolean setupDone = savedSettings.getBoolean(ApplicationData.SETUP_DONE, false);
		if (!setupDone) {
			Intent intentGetSettings = new Intent(this, CaptureActivity.class);
			startActivityForResult(intentGetSettings, RequestCodes.REQUEST_SETTINGS);
		} else {

			String selectedAddress = savedSettings.getString(ApplicationData.SELECTED_ADDRESS, "");
			ActionParams.SELECTED_ADDRESS = selectedAddress;

			Calendar cal = Calendar.getInstance();
			long oneYear = (long) 3.154E10;
			cal.setTimeInMillis(cal.getTimeInMillis() - oneYear);
			ActionParams.Year = savedSettings.getInt(ApplicationData.START_DATE_YEAR, cal.get(Calendar.YEAR));
			ActionParams.MonthOfYear = savedSettings.getInt(ApplicationData.START_DATE_MONTH, cal.get(Calendar.MONTH));
			ActionParams.DayOfMonth = savedSettings.getInt(ApplicationData.START_DATE_DAY, cal.get(Calendar.DAY_OF_MONTH));
			ActionParams.PriceMinValue = Integer.toString(savedSettings.getInt(ApplicationData.PRICE_MIN, 0));
			ActionParams.PriceMaxValue = Integer.toString(savedSettings.getInt(ApplicationData.PRICE_MAX, 0));
			ActionParams.BedroomMinValue = Integer.toString(savedSettings.getInt(ApplicationData.BEDROOM_MIN, 0));
			ActionParams.BedroomMaxValue = Integer.toString(savedSettings.getInt(ApplicationData.BEDROOM_MAX, 0));
			ActionParams.BathroomMinValue = Integer.toString(savedSettings.getInt(ApplicationData.BATHROOM_MIN, 0));
			ActionParams.BathroomMaxValue = Integer.toString(savedSettings.getInt(ApplicationData.BATHROOM_MAX, 0));
			ActionParams.StoriesMinValue = Integer.toString(savedSettings.getInt(ApplicationData.STORIES_MIN, 0));
			ActionParams.StoriesMaxValue = Integer.toString(savedSettings.getInt(ApplicationData.STORIES_MAX, 0));
		}
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mAttachedToWindow = true;
		openOptionsMenu();
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mAttachedToWindow = false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		this.menu = menu;
		return true;
	}

	@Override
	public void openOptionsMenu() {
		if (!mOptionsMenuOpen && mAttachedToWindow && mCompassService != null) {
			super.openOptionsMenu();
			updateMenuTitles();
		}
	}

	private void updateMenuTitles() {
		MenuItem menuFirstProperty = this.menu.findItem(R.id.open_place);
		MenuItem menuSecondProperty = this.menu.findItem(R.id.open_place2);
		if (ActionParams.firstProperty != null) {
			menuFirstProperty.setTitle(ActionParams.firstProperty.getPrice() + " " + ActionParams.firstProperty.getAddress());
		} else {
			menuFirstProperty.setTitle("loading...");
		}
		if (ActionParams.secondProperty != null) {
			menuSecondProperty.setTitle(ActionParams.secondProperty.getPrice() + ActionParams.secondProperty.getAddress());
		} else {
			menuSecondProperty.setTitle("loading...");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.menu = menu;
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.read_aloud:
			mCompassService.readHeadingAloud();
			return true;
		case R.id.exit:
			// Stop the service at the end of the message queue for proper options menu
			// animation. This is only needed when starting an Activity or stopping a Service
			// that published a LiveCard.
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					stopService(new Intent(MenuActivity.this, CasaService.class));
				}
			});
			return true;

		case R.id.open_place:
			ActionParams.selectedProperty = ActionParams.firstProperty;
			startActivity(new Intent(this, PropertyMenuActivity.class));
			return true;

		case R.id.open_place2:
			ActionParams.selectedProperty = ActionParams.secondProperty;
			startActivity(new Intent(this, PropertyMenuActivity.class));
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		mOptionsMenuOpen = false;

		unbindService(mConnection);

		// We must call finish() from this method to ensure that the activity ends either when an
		// item is selected from the menu or when the menu is dismissed by swiping down.
		finish();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case RequestCodes.REQUEST_SETTINGS:
			if (resultCode == ResultCodes.SETTINGS_RECIEVED) {
				bindService(new Intent(this, CasaService.class), mConnection, 0);
			}
			break;
		}
	}

}