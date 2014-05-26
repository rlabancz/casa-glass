/*
 * Copyright (C) 2014 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.glass;

import static ca.rldesigns.casa.android.glass.ApplicationData.DATABASE_NAME;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import ca.rldesigns.casa.android.glass.ApplicationData;
import ca.rldesigns.casa.android.glass.CasaService;
import ca.rldesigns.casa.android.glass.CasaService.CasaBinder;
import ca.rldesigns.casa.android.glass.R;
import ca.rldesigns.casa.android.glass.model.SavedPreference;
import ca.rldesigns.casa.android.glass.util.ActionParams;

import com.google.android.gms.maps.model.LatLng;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.client.result.TextParsedResult;
import com.google.zxing.client.result.URIParsedResult;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {

	private static final String TAG = CaptureActivity.class.getSimpleName();
	private static final String SCAN_ACTION = "com.google.zxing.client.android.SCAN";

	private SharedPreferences savedSettings;
	SharedPreferences.Editor editor;

	private boolean hasSurface;
	private boolean returnResult;
	private SurfaceHolder holderWithCallback;
	private Camera camera;
	private DecodeRunnable decodeRunnable;
	private Result result;

	private CasaService casaService;
	boolean mBound = false;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		savedSettings = getSharedPreferences(DATABASE_NAME, 0);
		editor = savedSettings.edit();

		// returnResult should be true if activity was started using startActivityForResult() with SCAN_ACTION intent
		Intent intent = getIntent();
		returnResult = intent != null && SCAN_ACTION.equals(intent.getAction());

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.capture);
	}

	@Override
	public void onStart() {
		super.onStart();
		Intent intent = new Intent(this, CasaService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			CasaBinder binder = (CasaBinder) service;
			casaService = binder.getService();
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBound = false;
		}

	};

	@Override
	public synchronized void onResume() {
		super.onResume();
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder?");
		}
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			holderWithCallback = surfaceHolder;
		}
	}

	@Override
	public synchronized void onPause() {
		result = null;
		if (decodeRunnable != null) {
			decodeRunnable.stop();
			decodeRunnable = null;
		}
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
		if (holderWithCallback != null) {
			holderWithCallback.removeCallback(this);
			holderWithCallback = null;
		}
		super.onPause();
	}

	@Override
	public synchronized void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "Surface created");
		holderWithCallback = null;
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// do nothing
	}

	@Override
	public synchronized void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "Surface destroyed");
		holderWithCallback = null;
		hasSurface = false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (result != null) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
				handleResult(result);
				return true;
			case KeyEvent.KEYCODE_BACK:
				reset();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void initCamera(SurfaceHolder holder) {
		if (camera != null) {
			throw new IllegalStateException("Camera not null on initialization");
		}
		camera = Camera.open();
		if (camera == null) {
			throw new IllegalStateException("Camera is null");
		}

		CameraConfigurationManager.configure(camera);

		try {
			camera.setPreviewDisplay(holder);
			camera.startPreview();
		} catch (IOException e) {
			Log.e(TAG, "Cannot start preview", e);
		}

		decodeRunnable = new DecodeRunnable(this, camera);
		new Thread(decodeRunnable).start();
		reset();
	}

	void setResult(Result result) {
		if (returnResult) {
			Intent scanResult = new Intent("com.google.zxing.client.android.SCAN");
			scanResult.putExtra("SCAN_RESULT", result.getText());
			setResult(RESULT_OK, scanResult);
			finish();
		} else {
			TextView statusView = (TextView) findViewById(R.id.status_view);
			String text = result.getText();
			if (text != null) {
				try {

					JSONObject settings = new JSONObject(text);
					String lat = (String) settings.get("y");
					editor.putFloat(ApplicationData.SELECTED_LAT, Float.parseFloat(lat));
					String lng = (String) settings.get("x");
					editor.putFloat(ApplicationData.SELECTED_LNG, Float.parseFloat(lng));
					LatLng selectedLatLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
					ActionParams.SelectedLatLng = selectedLatLng;

					String date = (String) settings.get("d");
					date = date.replace('.', '/');
					editor.putString(ApplicationData.START_DATE, date);
					ActionParams.Date = date;

					int priceMin = (int) settings.get("p-");
					priceMin = priceMin * 500000;
					editor.putInt(ApplicationData.PRICE_MIN, priceMin);
					ActionParams.PriceMinValue = Integer.toString(priceMin);
					int priceMax = (int) settings.get("p+");
					priceMax = priceMax * 500000;
					editor.putInt(ApplicationData.PRICE_MAX, priceMax);
					ActionParams.PriceMaxValue = Integer.toString(priceMax);

					int bedroomMin = (int) settings.get("e-");
					editor.putInt(ApplicationData.BEDROOM_MIN, bedroomMin);
					ActionParams.BedroomMinValue = Integer.toString(bedroomMin);
					int bedroomMax = (int) settings.get("e+");
					editor.putInt(ApplicationData.BEDROOM_MAX, bedroomMax);
					ActionParams.BedroomMaxValue = Integer.toString(bedroomMax);

					int bathroomMin = (int) settings.get("a-");
					editor.putInt(ApplicationData.BATHROOM_MIN, bathroomMin);
					ActionParams.BathroomMinValue = Integer.toString(bathroomMin);
					int bathroomMax = (int) settings.get("a+");
					editor.putInt(ApplicationData.BATHROOM_MAX, bathroomMax);
					ActionParams.BathroomMaxValue = Integer.toString(bathroomMax);

					int storiesMin = (int) settings.get("s-");
					editor.putInt(ApplicationData.STORIES_MIN, storiesMin);
					ActionParams.StoriesMinValue = Integer.toString(storiesMin);
					int storiesMax = (int) settings.get("s+");
					editor.putInt(ApplicationData.STORIES_MAX, storiesMax);
					ActionParams.StoriesMaxValue = Integer.toString(storiesMax);

					editor.putBoolean(ApplicationData.SETUP_DONE, true);
					ActionParams.SetupDone = true;

					editor.commit();

					SavedPreference savedPreference = new SavedPreference(ActionParams.SelectedLatLng, ActionParams.Date, ActionParams.PriceMinValue,
							ActionParams.PriceMaxValue, ActionParams.BedroomMinValue, ActionParams.BedroomMaxValue, ActionParams.BathroomMinValue,
							ActionParams.BathroomMaxValue, ActionParams.StoriesMinValue, ActionParams.StoriesMaxValue);

					ActionParams.savedPreference = savedPreference;

					if (mBound) {
						Log.d("CASA", "sending bind");
						casaService.startLandmarks();
						this.finish();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			statusView.setText(text);
			statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(14, 56 - text.length() / 4));
			statusView.setVisibility(View.VISIBLE);
			this.result = result;
		}
	}

	private void handleResult(Result result) {
		ParsedResult parsed = ResultParser.parseResult(result);
		Intent intent;
		if (parsed.getType() == ParsedResultType.URI) {
			intent = new Intent(Intent.ACTION_VIEW, Uri.parse(((URIParsedResult) parsed).getURI()));
		} else {
			intent = new Intent(Intent.ACTION_WEB_SEARCH);
			intent.putExtra("query", ((TextParsedResult) parsed).getText());
		}
		startActivity(intent);
	}

	private synchronized void reset() {
		TextView statusView = (TextView) findViewById(R.id.status_view);
		statusView.setVisibility(View.GONE);
		result = null;
		decodeRunnable.startScanning();
	}
}