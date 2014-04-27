/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.google.android.glass.sample.compass.model;

import com.google.android.glass.sample.compass.ActionParams;
import com.google.android.glass.sample.compass.R;
import com.google.android.glass.sample.compass.util.MathUtils;
import com.google.android.glass.sample.compass.util.ResultCodes;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides access to a list of hard-coded landmarks (located in {@code res/raw/landmarks.json}) that will appear on the compass
 * when the user is near them.
 */
public class Landmarks {

	public ArrayList<Property> properties;

	private static final String TAG = Landmarks.class.getSimpleName();

	/**
	 * The threshold used to display a landmark on the compass.
	 */
	private static final double MAX_DISTANCE_KM = 15;

	/**
	 * The list of landmarks loaded from resources.
	 */
	private final ArrayList<Place> mPlaces;

	/**
	 * Initializes a new {@code Landmarks} object by loading the landmarks from the resource bundle.
	 */
	public Landmarks(Context context) {
		mPlaces = new ArrayList<Place>();

		properties = new ArrayList<Property>();
		// This class will be instantiated on the service's main thread, and doing I/O on the
		// main thread can be dangerous if it will block for a noticeable amount of time. In
		// this case, we assume that the landmark data will be small enough that there is not
		// a significant penalty to the application. If the landmark data were much larger,
		// we may want to load it in the background instead.
		// String jsonString = readLandmarksResource(context);
		new SendDataAsync().execute(this, 43.6609214, -79.3867236, 3);
	}

	/**
	 * Gets a list of landmarks that are within ten kilometers of the specified coordinates. This function will never return null; if there
	 * are no locations within that threshold, then an empty list will be returned.
	 */
	public List<Place> getNearbyLandmarks(double latitude, double longitude, int results) {

		mPlaces.clear();
		new SendDataAsync().execute(this, latitude, longitude, results);
		Log.d(TAG, "got places");

		int size;
		Property property;
		if (properties.size() < results) {
			size = properties.size();
		} else {
			size = results;
		}
		Log.d(TAG, "size: " + Integer.toString(size));
		for (int i = 0; i < size; i++) {
			property = properties.get(i);
			ActionParams.firstProperty = property;
			Log.d(TAG, "adding " + property.getAddress());
			mPlaces.add(new Place(property.getLat(), property.getLng(), property.getAddress()));
		}

		return mPlaces;
	}

	/**
	 * Gets a list of landmarks that are within ten kilometers of the specified coordinates. This function will never return null; if there
	 * are no locations within that threshold, then an empty list will be returned.
	 */
	/*
	 * public List<Place> getNearbyLandmarks(double latitude, double longitude) { ArrayList<Place> nearbyPlaces = new ArrayList<Place>();
	 * 
	 * for (Place knownPlace : mPlaces) { if (MathUtils.getDistance(latitude, longitude, knownPlace.getLatitude(),
	 * knownPlace.getLongitude()) <= MAX_DISTANCE_KM) { nearbyPlaces.add(knownPlace); } }
	 * 
	 * return nearbyPlaces; }
	 */
	/**
	 * Populates the internal places list from places found in a JSON string. This string should contain a root object with a "landmarks"
	 * property that is an array of objects that represent places. A place has three properties: name, latitude, and longitude.
	 */
	private void populatePlaceList(String jsonString) {
		try {
			JSONObject json = new JSONObject(jsonString);
			JSONArray array = json.optJSONArray("landmarks");

			if (array != null) {
				for (int i = 0; i < array.length(); i++) {
					JSONObject object = array.optJSONObject(i);
					Place place = jsonObjectToPlace(object);
					if (place != null) {
						mPlaces.add(place);
					}
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "Could not parse landmarks JSON string", e);
		}
	}

	/**
	 * Converts a JSON object that represents a place into a {@link Place} object.
	 */
	private Place jsonObjectToPlace(JSONObject object) {
		String name = object.optString("name");
		double latitude = object.optDouble("latitude", Double.NaN);
		double longitude = object.optDouble("longitude", Double.NaN);

		if (!name.isEmpty() && !Double.isNaN(latitude) && !Double.isNaN(longitude)) {
			return new Place(latitude, longitude, name);
		} else {
			return null;
		}
	}

	public class SendDataAsync extends AsyncTask<Object, Boolean, String> {
		public String TAG = "COMPASS";
		public static final int ONE_SECOND = 1000;
		public static final int RECONNECT_TIMEOUT = 30000; // 30 seconds
		public static final int DATA_TIMEOUT = 8000; // 8 seconds

		public static final int ERROR = 0;
		public static final int CONNECTING = 1;
		public static final int UPDATE = 2;

		private JSONObject jsonObject;

		private double lat;
		private double lon;
		int results;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		protected String doInBackground(Object... params) {

			Log.d("MLS", "doInBackground");

			lat = (Double) params[1];
			lon = (Double) params[2];
			results = (Integer) params[3];
			double delta = 0.013;
			String data = "";
			try {
				String addressString = "";

				double minLat = lat - delta;
				double maxLat = lat + delta;
				double minLon = lon - delta;
				double maxLon = lon + delta;

				addressString = "http://www.realtor.ca/handlers/MapSearchHandler.ashx?xml=";
				addressString += "%3CListingSearchMap%3E" + "%3CCulture%3Een-CA%3C/Culture%3E" + "%3COrderBy%3E1%3C/OrderBy%3E"
						+ "%3COrderDirection%3EA%3C/OrderDirection%3E" + "%3CCulture%3Een-CA%3C/Culture%3E" + "%3CLatitudeMax%3E"
						+ maxLat
						+ "%3C/LatitudeMax%3E"
						+ "%3CLatitudeMin%3E"
						+ minLat
						+ "%3C/LatitudeMin%3E"
						+ "%3CLeaseRentMax%3E0%3C/LeaseRentMax%3E"
						+ "%3CLeaseRentMin%3E0%3C/LeaseRentMin%3E"
						+ "%3CListingStartDate%3E01/02/2014%3C/ListingStartDate%3E"
						+ "%3CLongitudeMax%3E"
						+ maxLon
						+ "%3C/LongitudeMax%3E"
						+ "%3CLongitudeMin%3E"
						+ minLon
						+ "%3C/LongitudeMin%3E"
						+ "%3CPriceMax%3E1000000%3C/PriceMax%3E"
						+ "%3CPriceMin%3E500000%3C/PriceMin%3E"
						+ "%3CPropertyTypeID%3E300%3C/PropertyTypeID%3E"
						+ "%3CTransactionTypeID%3E2%3C/TransactionTypeID%3E"
						+ "%3CMinBath%3E1%3C/MinBath%3E"
						+ "%3CMaxBath%3E2%3C/MaxBath%3E"
						+ "%3CMinBed%3E1%3C/MinBed%3E"
						+ "%3CMaxBed%3E2%3C/MaxBed%3E"
						+ "%3CStoriesTotalMin%3E0"
						+ "%3C/StoriesTotalMin%3E"
						+ "%3CStoriesTotalMax%3E0"
						+ "%3C/StoriesTotalMax%3E"
						+ "%3C/ListingSearchMap%3E";
				addressString = addressString.toString();

				if (addressString.equals(""))
					return ResultCodes.ERROR;

				HttpGet httpGet = new HttpGet(addressString);
				HttpClient httpClient = new DefaultHttpClient();
				HttpParams httpParameters = httpClient.getParams();
				// Set the timeout in milliseconds until a connection is established.
				// The default value is zero, that means the timeout is not used.
				HttpConnectionParams.setConnectionTimeout(httpParameters, DATA_TIMEOUT);
				// Set the default socket timeout (SO_TIMEOUT)
				// in milliseconds which is the timeout for waiting for data.
				HttpConnectionParams.setSoTimeout(httpParameters, DATA_TIMEOUT);
				HttpContext localContext = new BasicHttpContext();

				HttpResponse response = httpClient.execute(httpGet, localContext);
				StatusLine sL = response.getStatusLine();
				String responseCode = sL.toString();
				Log.d(TAG, responseCode);
				if (responseCode.contains("200")) {
					ResponseHandler<String> handler = new BasicResponseHandler();
					data = httpClient.execute(httpGet, handler);
					return data;
				} else {
					Log.d(TAG, responseCode);
					return ResultCodes.ERROR;
				}
			} catch (ConnectTimeoutException e) {
				Log.e(TAG, e.toString() + " CONNECT TIMEOUT");
				e.printStackTrace();
				return ResultCodes.TIMEDOUT;
			} catch (SocketTimeoutException e) {
				Log.e(TAG, e.toString() + " SOCKET TIMEOUT");
				e.printStackTrace();
				return ResultCodes.TIMEDOUT;
			} catch (UnknownHostException e) {
				Log.e(TAG, e.toString() + " UNKNOWN HOST");
				e.printStackTrace();
				return ResultCodes.TIMEDOUT;
			} catch (Exception e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
				return ResultCodes.ERROR;
			}
		}

		@Override
		protected void onPostExecute(String data) {

			try {
				parse(data);
			} catch (Exception e) {
				e.printStackTrace();
			}
			super.onPostExecute(data);
		}

		private boolean parse(String jsonString) throws Exception {
			jsonObject = new JSONObject(jsonString);
			if (jsonObject.has("MapSearchResults")) {
				JSONArray jArr = jsonObject.getJSONArray("MapSearchResults");

				for (int i = 0; i < jArr.length(); i++) {

					JSONObject obj = jArr.getJSONObject(i);
					String address = obj.getString("Address");

					String price = obj.getString("Price");
					double lat = obj.getDouble("Latitude");
					double lng = obj.getDouble("Longitude");
					String picture = obj.getString("PropertyImagePath");
					String bedroom = obj.getString("Bedrooms");
					String bathroom = obj.getString("Bathrooms");
					properties.add(new Property(address, price, lat, lng, picture, bedroom, bathroom));
				}

				Log.d("MLS", "done parsing");

				return true;
			} else {
				return false;
			}

		}
	}
}
