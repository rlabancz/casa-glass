package ca.rldesigns.casa.android.glass.model;

import com.google.android.gms.maps.model.LatLng;

public class SavedPreference {

	private LatLng SelectedLatLng;
	private String Date;
	private String PriceMinValue;
	private String PriceMaxValue;
	private String BedroomMinValue;
	private String BedroomMaxValue;
	private String BathroomMinValue;
	private String BathroomMaxValue;
	private String StoriesMinValue;
	private String StoriesMaxValue;

	public SavedPreference(LatLng SelectedLatLng, String Date, String PriceMinValue, String PriceMaxValue, String BedroomMinValue,
			String BedroomMaxValue, String BathroomMinValue, String BathroomMaxValue, String StoriesMinValue, String StoriesMaxValue) {

		this.SelectedLatLng = SelectedLatLng;
		this.Date = Date;
		this.PriceMinValue = PriceMinValue;
		this.PriceMaxValue = PriceMaxValue;
		this.BedroomMinValue = BedroomMinValue;
		this.BedroomMaxValue = BedroomMaxValue;
		this.BathroomMinValue = BathroomMinValue;
		this.BathroomMaxValue = BathroomMaxValue;
		this.StoriesMinValue = StoriesMinValue;
		this.StoriesMaxValue = StoriesMaxValue;
	}

	public LatLng getSelectedLatLng() {
		return SelectedLatLng;
	}

	public void setSelectedLatLng(LatLng selectedLatLng) {
		SelectedLatLng = selectedLatLng;
	}

	public String getDate() {
		return Date;
	}

	public void setDate(String date) {
		Date = date;
	}

	public String getPriceMinValue() {
		return PriceMinValue;
	}

	public void setPriceMinValue(String priceMinValue) {
		PriceMinValue = priceMinValue;
	}

	public String getPriceMaxValue() {
		return PriceMaxValue;
	}

	public void setPriceMaxValue(String priceMaxValue) {
		PriceMaxValue = priceMaxValue;
	}

	public String getBedroomMinValue() {
		return BedroomMinValue;
	}

	public void setBedroomMinValue(String bedroomMinValue) {
		BedroomMinValue = bedroomMinValue;
	}

	public String getBedroomMaxValue() {
		return BedroomMaxValue;
	}

	public void setBedroomMaxValue(String bedroomMaxValue) {
		BedroomMaxValue = bedroomMaxValue;
	}

	public String getBathroomMinValue() {
		return BathroomMinValue;
	}

	public void setBathroomMinValue(String bathroomMinValue) {
		BathroomMinValue = bathroomMinValue;
	}

	public String getBathroomMaxValue() {
		return BathroomMaxValue;
	}

	public void setBathroomMaxValue(String bathroomMaxValue) {
		BathroomMaxValue = bathroomMaxValue;
	}

	public String getStoriesMinValue() {
		return StoriesMinValue;
	}

	public void setStoriesMinValue(String storiesMinValue) {
		StoriesMinValue = storiesMinValue;
	}

	public String getStoriesMaxValue() {
		return StoriesMaxValue;
	}

	public void setStoriesMaxValue(String storiesMaxValue) {
		StoriesMaxValue = storiesMaxValue;
	}
}