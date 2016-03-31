package com.ibm.informix.route;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class Directions {
	public static final double EARTHS_RADIUS = 6371000.00;
	public static final int READ_INTERVAL_IN_SECONDS = 15;
	public static final long TOTAL_TIME = TimeUnit.MILLISECONDS.convert(18L, TimeUnit.HOURS);
	public static final String ENCODING = "UTF-8";
	public static final Date NOW = new GregorianCalendar(2015,0,1,0,0,0).getTime();
	public static final Date END = new GregorianCalendar(2015,0,1,18,0,0).getTime();
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final String READING_FORMAT = "%s|%f|%f\n";
	public static final Random randomizer = new Random();
	public static final double STOP_PERCENTAGE = .01;
	public static final Long MAX_STOP_TIME_IN_SECONDS = TimeUnit.SECONDS.convert(10, TimeUnit.MINUTES);
	public static final double SKIP_PERCENTAGE = .01;
	public static final int SKIPS_IN_A_ROW = 20;
	public static final UUID routeID = UUID.randomUUID();
	public static final StringBuffer buffer = new StringBuffer();

	public static int SKIPPED = 0;

	//	private static final String key = "AIzaSyCPuOVLoMbdbtxxsrG1tSyPZzQi__zfnrY";
	public static void main(String[] args) throws UnsupportedEncodingException {
		final String key = args[0];
		final String homeLocation = args[1].contains(" ") ? URLEncoder.encode(args[1],ENCODING) : args[1];
		final String waypoints = args[2].contains(" ") ? URLEncoder.encode(args[2],ENCODING) : args[2]; 

		final RestHandler handler = new RestHandler();

		final JsonObject json = handler.get("https://maps.googleapis.com/maps/api/directions/json?origin=" + homeLocation + "&destination=" + homeLocation + "&waypoints=" + waypoints + "&key=" + key, null);
		//System.out.println(json);

		final JsonArray routes = json.getJsonArray("routes");
		if(routes == null || routes.isEmpty()) {
			throw new RuntimeException("Route was null or was empty.");
		}

		final JsonObject route = routes.getJsonObject(0);
		final JsonArray legs = route.getJsonArray("legs");
		for(final JsonValue legValue : legs) {
			final JsonObject leg = (JsonObject) legValue;
			final JsonArray steps = leg.getJsonArray("steps");
			for(final JsonValue stepValue : steps) {
				if(NOW.after(END)) break;

				final JsonObject step = (JsonObject) stepValue;
				final JsonObject startLocation = step.getJsonObject("start_location");
				final JsonObject endLocation = step.getJsonObject("end_location");
				final double startLatitude = startLocation.getJsonNumber("lat").doubleValue();
				final double startLongitude = startLocation.getJsonNumber("lng").doubleValue();
				final double endLatitude = endLocation.getJsonNumber("lat").doubleValue();
				final double endLongitude = endLocation.getJsonNumber("lng").doubleValue();

				final double startLatitudeRadians = Math.toRadians(startLatitude);
				final double startLongitudeRadians = Math.toRadians(startLongitude);
				final double endLatitudeRadians = Math.toRadians(endLatitude);
				final double endLongitudeRadians = Math.toRadians(endLongitude);

				final double y = Math.sin(endLongitudeRadians - startLongitudeRadians) * Math.cos(endLatitudeRadians);
				final double x = ( Math.cos(startLatitudeRadians) * Math.sin(endLatitudeRadians) ) - ( Math.sin(startLatitudeRadians) * Math.cos(endLatitudeRadians) * Math.cos(endLongitudeRadians - startLongitudeRadians) );
				final double bearing = Math.toDegrees(Math.atan2(y, x));
				final double normalizedBearing = (bearing + 360) % 360;

				final JsonObject duration = step.getJsonObject("duration");
				final int seconds = duration.getInt("value");
				final int numberOfReads = seconds / READ_INTERVAL_IN_SECONDS;

				final JsonObject distance = step.getJsonObject("distance");
				final int meters = distance.getInt("value");

				if(numberOfReads != 0) {
					final int metersPerRead = meters / numberOfReads;

					for(int i = metersPerRead; i <= meters; i = i + metersPerRead) {
						final double angularDistance = i/EARTHS_RADIUS; 
						final double traveledLatitudeRadians = Math.asin( (Math.sin(startLatitudeRadians) * Math.cos(angularDistance) + (Math.cos(startLatitudeRadians) * Math.sin(angularDistance) * Math.cos(normalizedBearing))));
						final double traveledLongitudeRadians = startLongitudeRadians + Math.atan2( (Math.sin(normalizedBearing) * Math.sin(angularDistance) * Math.cos(startLatitudeRadians)), (Math.cos(angularDistance) - (Math.sin(startLatitudeRadians) * Math.sin(traveledLatitudeRadians))));

						final double traveledLatitude = Math.toDegrees(traveledLatitudeRadians);
						final double traveledLongitude = Math.toDegrees(traveledLongitudeRadians);

						printReading(traveledLatitude,traveledLongitude);
					}
				}
				printReading(endLatitude,endLongitude);
			}
		}
		writeBufferToFile();
	}

	@SuppressWarnings("deprecation")
	public static void printReading(final double latitude, final double longitude) {
		if(NOW.before(END)) { 
			NOW.setSeconds(NOW.getSeconds() + READ_INTERVAL_IN_SECONDS);
			final float stop = randomizer.nextFloat();
			if(stop < STOP_PERCENTAGE ) {
				final int stopTime = randomizer.nextInt(MAX_STOP_TIME_IN_SECONDS.intValue());
				NOW.setSeconds(NOW.getSeconds() + stopTime);
			}

			if(SKIPPED == 0) {
				final float skip = randomizer.nextFloat();   
				if(skip > SKIP_PERCENTAGE) {
					buffer.append(String.format(READING_FORMAT,DATE_FORMAT.format(NOW),latitude,longitude));
				} else {
					SKIPPED = SKIPS_IN_A_ROW;
				}
			} else {
				--SKIPPED;
			}
		}
	}

	public static void writeBufferToFile() {
		try {
			final File file = new File("/tmp/" + routeID.toString() + ".cvs");
			if(!file.exists()) {
				file.createNewFile();
			}
			final FileWriter fw = new FileWriter(file.getAbsoluteFile());
			final BufferedWriter bw = new BufferedWriter(fw);
			bw.write(buffer.toString());
			bw.close();
			System.out.println("PRINTED TO: /tmp/" + routeID.toString() + ".cvs");
			System.out.println("DONE");
		} catch(IOException ioex) {
			ioex.printStackTrace();
		}
	}
}
