package com.planview.replicator;

import com.planview.replicator.leankit.AccessCache;

public class AccessConfig extends Access {
	AccessCache cache = null;
	String user = null;
	
	public AccessConfig() {

	}
	
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public AccessCache getCache() {
		return cache;
	}

	public void setCache(AccessCache cache) {
		this.cache = cache;
	}

	public AccessConfig( String url, String name, String apikey) {
		Url = url;
		BoardName = name;
		ApiKey = apikey;
	}
}
