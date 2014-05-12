package ca.rldesigns.casa.android.glass;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.Runnable;

import ca.rldesigns.casa.android.glass.model.Landmarks;

/**
 * This activity manages the options menu that appears when the user taps on the compass's live card.
 */
public class CompassMenuActivity extends Activity {

	private final Handler mHandler = new Handler();

	private CompassService.CompassBinder mCompassService;
	private boolean mAttachedToWindow;
	private boolean mOptionsMenuOpen;
	public Menu menu;
	public Landmarks mLandmarks;

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (service instanceof CompassService.CompassBinder) {
				mCompassService = (CompassService.CompassBinder) service;
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
		bindService(new Intent(this, CompassService.class), mConnection, 0);
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
		getMenuInflater().inflate(R.menu.compass, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.read_aloud:
			mCompassService.readHeadingAloud();
			return true;
		case R.id.stop:
			// Stop the service at the end of the message queue for proper options menu
			// animation. This is only needed when starting an Activity or stopping a Service
			// that published a LiveCard.
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					stopService(new Intent(CompassMenuActivity.this, CompassService.class));
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
}
