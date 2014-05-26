package ca.rldesigns.casa.android.glass.model;

import ca.rldesigns.casa.android.glass.R;
import ca.rldesigns.casa.android.glass.util.ActionParams;
import ca.rldesigns.casa.android.glass.util.Formatter;
import ca.rldesigns.casa.android.glass.util.MathUtils;
import ca.rldesigns.casa.android.glass.util.ResultCodes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Landmarks {

	public ArrayList<Property> properties;

	private static final String TAG = "CASA";

	/**
	 * The list of landmarks loaded from resources.
	 */
	private ArrayList<Place> mPlaces;

	/**
	 * Initializes a new {@code Landmarks} object by loading the landmarks from the resource bundle.
	 */
	public Landmarks(Context context) {
		mPlaces = new ArrayList<Place>();

		properties = new ArrayList<Property>();
		Log.d(TAG, "Landmarks init");
	}

	public Landmarks(Context context, LatLng coordinates) {
		mPlaces = new ArrayList<Place>();
		properties = new ArrayList<Property>();

		ActionParams.firstProperty = null;
		ActionParams.secondProperty = null;
		ActionParams.thirdProperty = null;
		ActionParams.fourthProperty = null;

		if (coordinates != null) {
			Log.d(TAG, "sending web request");
			new SendDataAsync().execute(this, coordinates.latitude, coordinates.longitude, 5);
		} else {
			Log.d(TAG, "missing coordinates");
		}
	}

	public List<Place> getNearbyLandmarks(int results) {
		if (mPlaces.isEmpty()) {
			mPlaces = ActionParams.placeList;
		}
		return mPlaces;
	}

	public class SendDataAsync extends AsyncTask<Object, Boolean, String> {
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

		protected String doInBackground(Object... params) {
			lat = (Double) params[1];
			lon = (Double) params[2];

			results = (Integer) params[3];
			double delta = 0.025;
			String data = "";
			try {
				SavedPreference savedPreference = ActionParams.savedPreference;
				if (savedPreference == null) {
					Log.d(TAG, "error with savedPref");
					return ResultCodes.ERROR;
				}

				String addressString = "";

				double minLat = lat - delta;
				String minLatString = Formatter.formatCoordinate(minLat);
				double maxLat = lat + delta;
				String maxLatString = Formatter.formatCoordinate(maxLat);
				double minLon = lon - delta;
				String minLonString = Formatter.formatCoordinate(minLon);
				double maxLon = lon + delta;
				String maxLonString = Formatter.formatCoordinate(maxLon);

				addressString = "http://www.realtor.ca/handlers/MapSearchHandler.ashx?xml=%3CListingSearchMap%3E";
				addressString += "%3CCulture%3Een-CA%3C/Culture%3E";
				addressString += "%3COrderBy%3E1%3C/OrderBy%3E";
				addressString += "%3COrderDirection%3EA%3C/OrderDirection%3E";
				addressString += "%3CCulture%3Een-CA%3C/Culture%3E";
				addressString += "%3CLatitudeMax%3E" + maxLatString + "%3C/LatitudeMax%3E";
				addressString += "%3CLatitudeMin%3E" + minLatString + "%3C/LatitudeMin%3E";
				addressString += "%3CLeaseRentMax%3E" + "0" + "%3C/LeaseRentMax%3E";
				addressString += "%3CLeaseRentMin%3E" + "0" + "%3C/LeaseRentMin%3E";
				addressString += "%3CListingStartDate%3E01/02/2014%3C/ListingStartDate%3E%";
				addressString += "3CLongitudeMax%3E" + maxLonString + "%3C/LongitudeMax%3E";
				addressString += "%3CLongitudeMin%3E" + minLonString + "%3C/LongitudeMin%3E";
				addressString += "%3CPriceMax%3E" + "1500000" + "%3C/PriceMax%3E";
				addressString += "%3CPriceMin%3E" + "100000" + "%3C/PriceMin%3E";
				addressString += "%3CPropertyTypeID%3E300%3C/PropertyTypeID%3E";
				addressString += "%3CTransactionTypeID%3E2%3C/TransactionTypeID%3E";
				addressString += "%3CMinBath%3E" + savedPreference.getBathroomMinValue().toString() + "%3C/MinBath%3E";
				addressString += "%3CMaxBath%3E" + savedPreference.getBathroomMaxValue().toString() + "%3C/MaxBath%3E";
				addressString += "%3CMinBed%3E" + savedPreference.getBedroomMinValue().toString() + "%3C/MinBed%3E";
				addressString += "%3CMaxBed%3E" + savedPreference.getBedroomMaxValue().toString() + "%3C/MaxBed%3E";
				addressString += "%3CStoriesTotalMin%3E" + savedPreference.getStoriesMinValue().toString() + "%3C/StoriesTotalMin%3E";
				addressString += "%3CStoriesTotalMax%3E" + savedPreference.getStoriesMaxValue().toString() + "%3C/StoriesTotalMax%3E";
				addressString += "%3C/ListingSearchMap%3E";
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

		private boolean parse(String jsonString) {
			try {
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

						if (ActionParams.Range > 0) {
							if (MathUtils.getDistance(ActionParams.SelectedLatLng.latitude, ActionParams.SelectedLatLng.longitude, lat, lng) <= ActionParams.Range) {
								properties.add(new Property(address, price, lat, lng, picture, bedroom, bathroom));
							}
						} else {
							properties.add(new Property(address, price, lat, lng, picture, bedroom, bathroom));
						}
					}
					Log.d(TAG, "done parsing");

					int size;
					Property property = null;
					Log.d(TAG, "size: " + Integer.toString(properties.size()) + " results: " + results);
					if (properties.size() < results) {
						size = properties.size();
					} else {
						size = results;
					}

					Place newPlace = null;
					int icon = R.drawable.place_mark;
					for (int i = 0; i < size; i++) {
						property = properties.get(i);

						if (i == 0) {
							icon = R.drawable.yellow_mini;
							ActionParams.firstProperty = properties.get(i);
							ActionParams.firstProperty.setId(i);
							ActionParams.firstProperty.setIcon(icon);
							new DownloadImagesTask().execute(ActionParams.firstProperty);
						}
						if (i == 1) {
							icon = R.drawable.blue_mini;
							ActionParams.secondProperty = properties.get(i);
							ActionParams.secondProperty.setId(i);
							ActionParams.secondProperty.setIcon(icon);
							new DownloadImagesTask().execute(ActionParams.secondProperty);
						}
						if (i == 2) {
							icon = R.drawable.green_mini;
							ActionParams.thirdProperty = properties.get(i);
							ActionParams.thirdProperty.setId(i);
							ActionParams.thirdProperty.setIcon(icon);
							new DownloadImagesTask().execute(ActionParams.thirdProperty);
						}
						if (i == 3) {
							icon = R.drawable.red_mini;
							ActionParams.fourthProperty = properties.get(i);
							ActionParams.fourthProperty.setId(i);
							ActionParams.fourthProperty.setIcon(icon);
							new DownloadImagesTask().execute(ActionParams.fourthProperty);
						}
						Log.d(TAG, "adding " + property.getAddress());
						newPlace = new Place(icon, property.getLat(), property.getLng(), property.getAddress(), property.getPrice());
						if (!mPlaces.contains(newPlace)) {
							mPlaces.add(newPlace);
						}
					}
					if (!mPlaces.isEmpty()) {
						ActionParams.placeList = mPlaces;
					}

					return true;
				} else {
					return false;
				}
			} catch (JSONException e) {
				e.printStackTrace();
				Log.d(TAG, e.toString());
				return false;
			}
		}
	}

	private class DownloadImagesTask extends AsyncTask<Property, Void, Bitmap> {
		Property mProperty;

		@Override
		protected Bitmap doInBackground(Property... property) {
			mProperty = property[0];
			Bitmap bm = null;
			try {
				URL aURL = new URL(mProperty.getPicture());
				URLConnection conn = aURL.openConnection();
				conn.connect();
				InputStream is = conn.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(is);
				bm = BitmapFactory.decodeStream(bis);
				bis.close();
				is.close();
			} catch (IOException e) {
				Log.e(TAG, "Error getting the image from server : " + e.getMessage().toString());
			}
			return bm;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			int num = mProperty.getId();
			switch (num) {
			case 0:
				ActionParams.firstProperty.setPictureBitmap(result);
				break;
			case 1:
				ActionParams.secondProperty.setPictureBitmap(result);
				break;
			case 2:
				ActionParams.thirdProperty.setPictureBitmap(result);
				break;
			case 3:
				ActionParams.fourthProperty.setPictureBitmap(result);
				break;
			}
		}
	}
}