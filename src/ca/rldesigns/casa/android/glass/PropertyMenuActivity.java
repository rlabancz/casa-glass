package ca.rldesigns.casa.android.glass;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ca.rldesigns.casa.android.glass.model.Property;
import ca.rldesigns.casa.android.glass.util.ActionParams;

import com.google.android.glass.app.Card;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import org.json.JSONException;
import org.json.JSONObject;

public class PropertyMenuActivity extends Activity {
	public static final String TAG = "CASA";

	private CasaService.CasaBinder mCasaService;
	private boolean mAttachedToWindow;
	private boolean mOptionsMenuOpen;

	private Property mProperty;
	Menu menu;
	private List<Card> mCards;
	private Card card;
	private Card schoolCard;
	private Card bikeStationCard;
	private Card fireStationCard;
	private Card ambulanceStationCard;
	private Card policeStationCard;

	private CardScrollView mCardScrollView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mProperty = ActionParams.selectedProperty;
		// new DownloadImagesTask().execute(mProperty.getPicture());

		mCards = new ArrayList<Card>();

		card = new Card(this);
		card.setText(mProperty.getAddress());
		card.setFootnote(mProperty.getPrice());
		// card.addImage(R.drawable.gradient50);
		card.addImage(mProperty.getPictureBitmap());
		mCards.add(card);

		card = new Card(this);
		card.setText(mProperty.getBedrooms() + " / " + mProperty.getBathrooms());
		card.setFootnote("Bedrooms / Bathrooms");
		card.setImageLayout(Card.ImageLayout.FULL);
		card.addImage(mProperty.getPictureBitmap());
		mCards.add(card);

		createCards();

		mCardScrollView = new CardScrollView(this);
		PropertyCardScrollAdapter adapter = new PropertyCardScrollAdapter();
		mCardScrollView.setAdapter(adapter);
		mCardScrollView.activate();
		setContentView(mCardScrollView);
	}

	private void createCards() {
		schoolCard = new Card(this);
		bikeStationCard = new Card(this);
		fireStationCard = new Card(this);
		ambulanceStationCard = new Card(this);
		policeStationCard = new Card(this);

		getAdditionalInfo(mProperty, fireStationCard, bikeStationCard, schoolCard, ambulanceStationCard, policeStationCard);
	}

	private class PropertyCardScrollAdapter extends CardScrollAdapter {

		@Override
		public int getPosition(Object item) {
			return mCards.indexOf(item);
		}

		@Override
		public int getCount() {
			return mCards.size();
		}

		@Override
		public Object getItem(int position) {
			return mCards.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return mCards.get(position).getView(convertView, parent);
		}
	}

	// Misc stuff
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
		if (!mOptionsMenuOpen && mAttachedToWindow && mCasaService != null) {
			super.openOptionsMenu();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.menu = menu;
		getMenuInflater().inflate(R.menu.property, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.exit:
			this.finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		mOptionsMenuOpen = false;
		// We must call finish() from this method to ensure that the activity ends either when an
		// item is selected from the menu or when the menu is dismissed by swiping down.
		finish();
	}

	private void getAdditionalInfo(Property property, Card fireStationCard, Card bikeStationCard, Card schoolCard, Card ambulanceStationCard, Card policeStationCard) {
		new MyAsyncTask().execute(this,
				"http://conversationboard.com:8019/assessment?latitude=" + property.getLat() + "&longitude=" + property.getLng(), fireStationCard,
				bikeStationCard, schoolCard, ambulanceStationCard, policeStationCard);

	}

	private class MyAsyncTask extends AsyncTask<Object, Boolean, String> {
		private Card fireStationCard;
		private Card bikeStationCard;
		private Card schoolCard;
		private Card ambulanceStationCard;
		private Card policeStationCard;

		@Override
		protected String doInBackground(Object... params) {
			HttpClient httpClient = new DefaultHttpClient();
			String textv = "";
			BufferedReader in = null;
			String url = (String) params[1];
			HttpGet request = new HttpGet(url);
			fireStationCard = (Card) params[2];
			bikeStationCard = (Card) params[3];
			schoolCard = (Card) params[4];
			ambulanceStationCard = (Card) params[5];
			policeStationCard = (Card) params[6];
			
			try {
				HttpResponse response = httpClient.execute(request);
				in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				// NEW CODE
				String l = "";
				while ((l = in.readLine()) != null) {
					textv += l;
				}
			} catch (IOException e) {
				e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
			}
			return textv;
		}

		@Override
		protected void onPostExecute(String data) {
			try {
				JSONObject obj = new JSONObject(data);
				JSONObject fireStation = obj.getJSONObject("fire_station");
				int distance = fireStation.getInt("distance");

				fireStationCard.setText("Closest Firestation:\n" + distance + "m");
				mCards.add(fireStationCard);

				
				JSONObject ambulanceStation = obj.getJSONObject("ambulance_station");
				String address = ambulanceStation.getString("EMS_ADDRES");
				distance = ambulanceStation.getInt("distance");
				ambulanceStationCard.setText("Closest Ambulance Station:\n" + address + "\n" + distance + "m");
				mCards.add(ambulanceStationCard);				
				
				JSONObject bikeStation = obj.getJSONObject(("bike_station"));
				String name = bikeStation.getString("stationName");
				distance = bikeStation.getInt("distance");
				bikeStationCard.setText("Closest Bixi Stations:\n" + name + "\n" + distance + "m");
				mCards.add(bikeStationCard);

				JSONObject school = obj.getJSONObject("school");
				name = school.getString("SCHNAME");
				distance = school.getInt("distance");
				schoolCard.setText("Closest School:\n" + name + "\n" + distance + "m");
				mCards.add(schoolCard);
				

				
				JSONObject policeStation = obj.getJSONObject("police_station");
				address = policeStation.getString("FULL_ADDRE");
				distance = policeStation.getInt("distance");
				policeStationCard.setText("Closest Police Station:\n" + address + "\n" + distance +"m");
				mCards.add(policeStationCard);
				
				mCardScrollView.activate();

			} catch (JSONException e) {
				e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
			}
		}
	}
}