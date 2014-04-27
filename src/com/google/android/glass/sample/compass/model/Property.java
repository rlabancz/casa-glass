package com.google.android.glass.sample.compass.model;

public class Property {
	private String address;
	private String price;
	private double lat;
	private double lng;
	private String picture;

	public Property(String address, String price, double lat, double lng, String picture) {
		this.address = address;
		this.price = price;
		this.lat = lat;
		this.lng = lng;
		this.picture = picture;
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

}
