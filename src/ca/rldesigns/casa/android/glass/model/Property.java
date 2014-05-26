package ca.rldesigns.casa.android.glass.model;

import android.graphics.Bitmap;

public class Property {
	private int id;
	private int icon;
	private String address;
	private String price;
	private double lat;
	private double lng;
	private String picture;
	private Bitmap pictureBitmap;
	private String bedrooms;
	private String bathrooms;

	public Property(String address, String price, double lat, double lng, String picture, String bedrooms, String bathrooms) {
		this.address = address;
		this.price = price;
		this.lat = lat;
		this.lng = lng;
		this.picture = picture;
		this.bedrooms = bedrooms;
		this.bathrooms = bathrooms;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getIcon() {
		return icon;
	}

	public void setIcon(int icon) {
		this.icon = icon;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLng() {
		return lng;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public Bitmap getPictureBitmap() {
		return pictureBitmap;
	}

	public void setPictureBitmap(Bitmap pictureBitmap) {
		this.pictureBitmap = pictureBitmap;
	}

	public String getBedrooms() {
		return bedrooms;
	}

	public void setBedrooms(String bedrooms) {
		this.bedrooms = bedrooms;
	}

	public String getBathrooms() {
		return bathrooms;
	}

	public void setBathrooms(String bathrooms) {
		this.bathrooms = bathrooms;
	}
}