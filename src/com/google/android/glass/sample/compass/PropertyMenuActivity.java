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

package com.google.android.glass.sample.compass;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;

import com.google.android.glass.app.Card;
import com.google.android.glass.sample.compass.model.Property;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This activity manages the options menu that appears when the user taps on the compass's live card.
 */
public class PropertyMenuActivity extends Activity {

	private CompassService.CompassBinder mCompassService;
	private boolean mAttachedToWindow;
	private boolean mOptionsMenuOpen;

	private Property mProperty;
	Menu menu;
	Card card;
	
	private List<Card> mCards;
	private CardScrollView mCardScrollView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.d("PropertyMenuActivity", "inited");
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.property_main);
		mProperty = ActionParams.firstProperty;



//		RelativeLayout background = (RelativeLayout) findViewById(R.id.background);
///		TextView price = (TextView) findViewById(R.id.price);
	//	price.setText(mProperty.getPrice());
	//	background.setBackground(LoadImageFromWebOperations(mProperty.getPicture()));
		mCards = new ArrayList<Card>();
		
		card = new Card(this);
		card.setText(mProperty.getAddress());
		card.setFootnote(mProperty.getPrice());
		mCards.add(card);
		
		createCards();

		mCardScrollView = new CardScrollView(this);
		ExampleCardScrollAdapter adapter = new ExampleCardScrollAdapter();
		mCardScrollView.setAdapter(adapter);
		mCardScrollView.activate();
		setContentView(mCardScrollView);
	}

	public static Drawable LoadImageFromWebOperations(String url) {
		try {
			InputStream is = (InputStream) new URL(url).getContent();
			Drawable d = Drawable.createFromStream(is, "src name");
			return d;
		} catch (Exception e) {
			return null;
		}
	}

	private void createCards() {
	

	

	

		card = new Card(this);
		card.setText("This card has a puppy background image.");
		card.setFootnote("How can you resist?");
		card.setImageLayout(Card.ImageLayout.FULL);
		// card.addImage(R.drawable.puppy_bg);
		mCards.add(card);

		card = new Card(this);
		card.setText("This card has a mosaic of puppies.");
		card.setFootnote("Aren't they precious?");
		card.setImageLayout(Card.ImageLayout.LEFT);
		// card.addImage(R.drawable.puppy_small_1);
		// card.addImage(R.drawable.puppy_small_2);
		// card.addImage(R.drawable.puppy_small_3);
        mCards.add(card);


        Card fireStationCard = new Card(this);
        getAdditionalInfo(mProperty, fireStationCard);
        mCards.add(fireStationCard);

	}

	private class ExampleCardScrollAdapter extends CardScrollAdapter {

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

		/**
		 * Returns the amount of view types.
		 */
		@Override
		public int getViewTypeCount() {
			return Card.getViewTypeCount();
		}

		/**
		 * Returns the view type of this card so the system can figure out if it can be recycled.
		 */
		@Override
		public int getItemViewType(int position) {
			return mCards.get(position).getItemViewType();
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
		if (!mOptionsMenuOpen && mAttachedToWindow && mCompassService != null) {
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
		case R.id.read_aloud:
			// mCompassService.readHeadingAloud();
			return true;
		case R.id.stop:
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

    private void getAdditionalInfo(Property property, Card fireStationCard) {
        new MyAsyncTask().execute(this,
                "http://conversationboard.com:8019/assessment?latitude="+property.getLat()+"&longitude="+property.getLng(),
                fireStationCard
        );


    }

    private class MyAsyncTask extends AsyncTask<Object, Boolean, String> {
        private Card fireStationCard;
        @Override
        protected String doInBackground(Object... params) {
            HttpClient httpClient = new DefaultHttpClient();
            String textv = "";
            BufferedReader in = null;
            String url = (String)params[1];
            HttpGet request = new HttpGet(url);
            fireStationCard = (Card)params[2];
            try {
                HttpResponse response = httpClient.execute(request);
                in = new BufferedReader(new InputStreamReader(
                        response.getEntity().getContent()));

                // NEW CODE
                String l = "";
                while ((l = in.readLine()) != null) {
                    textv+= l;
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            return textv;

        }


        @Override
        protected void onPostExecute(String data) {
            Log.d("PropertyMenuActivity", "data : "+data);
            try {
                JSONObject obj = new JSONObject(data);
                JSONObject fireStation = obj.getJSONObject("fire_station");
                int distance = fireStation.getInt("distance");
                Log.d("PropertyMenuActivity", "firstation distance : " + distance);
                fireStationCard.setText("Closest Firestation:\n"+distance+"m");

            } catch (JSONException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }
    }
}
