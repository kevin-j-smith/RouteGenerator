package com.ibm.informix.route;

import javax.json.JsonObject;

public class Directions {
	public static final String key = "AIzaSyAEuMTYA5NHfOeCYkzRcCXOIi5_EDkPHPs";
	public static void main(String[] args) {
		final RestHandler handler = new RestHandler();
		
		final JsonObject json = handler.get("https://maps.googleapis.com/maps/api/directions/json?origin=Kansas+City&destination=Estes+Park", null);
		System.out.println(json);
	}
}
