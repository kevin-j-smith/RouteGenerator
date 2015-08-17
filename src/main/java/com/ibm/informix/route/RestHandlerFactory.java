package com.ibm.informix.route;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RestHandlerFactory {
	private RestHandlerFactory(){};
	
	private static final Map<Object,RestHandler> handlers = new HashMap<Object,RestHandler>();
	
	public static RestHandler getInstance(UUID uuid) {
		if(handlers.containsKey(uuid)) 
			return handlers.get(uuid);
		else {
			acceptAllCertificates();
			RestHandler handler = new RestHandler();
			handlers.put(uuid, handler);
			return handler;
		}
	}
	
	public static RestHandler getInstance(String username, String password) {
		String basicUsernameAndPassword = "Basic: " + username + ":" + "password";
		if(handlers.containsKey(basicUsernameAndPassword)) {
			return handlers.get(basicUsernameAndPassword);
		} else {
			acceptAllCertificates();
			RestHandler handler = new RestHandler(username,password);
			handlers.put(basicUsernameAndPassword, handler);
			return handler;
		}
	}
	
	public static void closeInstance(UUID uuid) {
		if(handlers.containsKey(uuid)) { 
			handlers.get(uuid).close();
			handlers.remove(uuid);
		}	
	}
	
	public static void acceptAllCertificates() {
		//MANUALLY DISABLING ALL SELF SIGNED CERTIFICATES
		TrustManager[] trustAllCerts = new TrustManager[] { 
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() { 
						return null;
					} 
					public void checkClientTrusted(X509Certificate[] certs, String authType) {} 
					public void checkServerTrusted(X509Certificate[] certs, String authType) {}
				} 
		}; 

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(new KeyManager[0], trustAllCerts, new java.security.SecureRandom());
			SSLContext.setDefault(sc);
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (GeneralSecurityException e) {
		} 
		//MANUALLY DISABLING ALL SELF SIGNED CERTIFICATES
	}
}
