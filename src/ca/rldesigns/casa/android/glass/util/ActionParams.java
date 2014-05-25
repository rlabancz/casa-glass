package ca.rldesigns.casa.android.glass.util;

import ca.rldesigns.casa.android.glass.model.Landmarks;
import ca.rldesigns.casa.android.glass.model.Place;
import ca.rldesigns.casa.android.glass.model.Property;

import com.google.android.gms.maps.model.LatLng;

public class ActionParams {
	public static String PREVIOUS_TITLE = "";
	public static LatLng SELECTED_POSITION = null;
	public static String SELECTED_ADDRESS = "";

	public static double LAST_LAT;
	public static double LAST_LON;

	public static Landmarks firstLandmark = null;
	public static Place firstPlace = null;
	public static Property selectedProperty = null;
	public static Property firstProperty = null;
	public static Property secondProperty = null;

	public static boolean SetupDone = false;
	public static LatLng SelectedLatLng = null;
	public static String Date = "";
	public static int Year = 0;
	public static int MonthOfYear = 0;
	public static int DayOfMonth = 0;
	public static int PriceMinValue = 0;
	public static int PriceMaxValue = 0;
	public static int BedroomMinValue = 0;
	public static int BedroomMaxValue = 0;
	public static int BathroomMinValue = 0;
	public static int BathroomMaxValue = 0;
	public static int StoriesMinValue = 0;
	public static int StoriesMaxValue = 0;
}