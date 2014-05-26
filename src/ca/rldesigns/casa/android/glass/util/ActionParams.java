package ca.rldesigns.casa.android.glass.util;

import java.util.ArrayList;

import ca.rldesigns.casa.android.glass.model.Landmarks;
import ca.rldesigns.casa.android.glass.model.Place;
import ca.rldesigns.casa.android.glass.model.Property;
import ca.rldesigns.casa.android.glass.model.SavedPreference;

import com.google.android.gms.maps.model.LatLng;

public class ActionParams {
	public static String PREVIOUS_TITLE = "";
	public static LatLng SELECTED_POSITION = null;
	public static String SELECTED_ADDRESS = "";

	public static ArrayList<Place> placeList = null;

	public static double LAST_LAT;
	public static double LAST_LON;

	public static Landmarks firstLandmark = null;
	public static Place firstPlace = null;
	public static Property selectedProperty = null;
	public static Property firstProperty = null;
	public static Property secondProperty = null;
	public static Property thirdProperty = null;
	public static Property fourthProperty = null;

	public static boolean SetupDone = false;

	public static SavedPreference savedPreference = null;
	public static LatLng SelectedLatLng = null;
	public static double Range = 0;
	public static String Date = "";
	public static String PriceMinValue = "";
	public static String PriceMaxValue = "";
	public static String BedroomMinValue = "";
	public static String BedroomMaxValue = "";
	public static String BathroomMinValue = "";
	public static String BathroomMaxValue = "";
	public static String StoriesMinValue = "";
	public static String StoriesMaxValue = "";
}