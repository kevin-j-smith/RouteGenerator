package com.ibm.informix.route;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;


public class RestHandler {
    private final Client client = ClientBuilder.newClient();
    private final Map<String,Cookie> cookies = new HashMap<String,Cookie>();
    
    public RestHandler() {
    	
    }
    
    public RestHandler(String username, String password) {
    	HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(username, password);
    	client.register(feature);
    }
    
    private JsonObject parseString(final String entity) {
    	final StringReader stringReader = new StringReader(entity);
    	final JsonReader jsonReader = Json.createReader(stringReader);
    	final JsonObject jsonObject = jsonReader.readObject();
    	jsonReader.close();
    	return jsonObject;
    }
    
    public JsonObject get(String uri, QueryPair query) {
    	WebTarget target = client.target(uri);
    	if(query != null) {
    		try {target = target.queryParam(query.key, URLEncoder.encode(query.value.toString(),"UTF-8"));} catch (UnsupportedEncodingException e) {e.printStackTrace();}
    	}
        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
        for(Entry<String,Cookie> cookie : this.cookies.entrySet())
        	invocationBuilder.cookie(cookie.getValue());
//        System.out.println("Running GET " + uri);
    	Response response = invocationBuilder.get();
    	String entity = response.readEntity(String.class);
    	cookies.putAll(response.getCookies());
    	response.close();
    	return  parseString(entity);
    }
    
    public JsonObject post(String uri, QueryPair query, JsonObject payload) {
    	WebTarget target = client.target(uri);
    	if(query != null) {
    		try {target = target.queryParam(query.key, URLEncoder.encode(query.value.toString(),"UTF-8"));} catch (UnsupportedEncodingException e) {e.printStackTrace();}
    	}
        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
        for(Entry<String,Cookie> cookie : cookies.entrySet())
        	invocationBuilder.cookie(cookie.getValue());

//        System.out.println("Running POST " + uri + " " + payload);
    	Response response = invocationBuilder.post(Entity.json(payload.toString()));
    	String entity = response.readEntity(String.class);
    	cookies.putAll(response.getCookies());
    	response.close();
    	return  parseString(entity); 
    }
    
    public JsonObject put(String uri, QueryPair query, JsonObject payload) {
    	WebTarget target = client.target(uri);
    	if(query != null) {
    		try {target = target.queryParam(query.key, URLEncoder.encode(query.value.toString(),"UTF-8"));} catch (UnsupportedEncodingException e) {e.printStackTrace();}
    	}
        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON);
    	for(Entry<String,Cookie> cookie : cookies.entrySet())
    		invocationBuilder.cookie(cookie.getValue());
    	
//    	System.out.println("Running PUT " + uri + " " + payload);
    	Response response = invocationBuilder.put(Entity.json(payload.toString()));
    	String entity = response.readEntity(String.class);
    	cookies.putAll(response.getCookies());
    	response.close();
    	return  parseString(entity); 
    }
    
    public JsonObject delete(String uri, QueryPair... queries) {
    	WebTarget target = client.target(uri);
    	String fullQueryString = "?";
    	if(queries != null) {
    		for(QueryPair query : queries) {
    			if(query != null) {
    				try {target = target.queryParam(query.key, URLEncoder.encode(query.value.toString(),"UTF-8"));} catch (UnsupportedEncodingException e) {e.printStackTrace();}
    				fullQueryString = fullQueryString + query.key + "=" + query.value.toString() + "&";
    			}
    		}
    	}
    	
    	Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
        for(Entry<String,Cookie> cookie : cookies.entrySet())
        	invocationBuilder.cookie(cookie.getValue());

//        System.out.println("Running POST " + uri + fullQueryString);
        Response response = invocationBuilder.delete();
    	String entity = response.readEntity(String.class);
    	cookies.putAll(response.getCookies());
    	response.close();
    	return  parseString(entity); 
    }
    
    public void close() {
    	client.close();
    }
}
