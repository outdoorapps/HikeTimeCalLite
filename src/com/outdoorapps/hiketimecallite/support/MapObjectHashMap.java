package com.outdoorapps.hiketimecallite.support;

import java.util.HashMap;

import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;


@SuppressWarnings("serial")
public class MapObjectHashMap<K,V> extends HashMap<String,V> {
	
	public static final String ROUTE_IDENTIFIER = "_r";
	public static final String TRACK_IDENTIFIER = "_t";
	
	public MapObjectHashMap() {
		super();
	}
	
	public void put(String key, MapObjectType type, V value) {
		if(type==MapObjectType.route) {
			key += ROUTE_IDENTIFIER;
			super.put(key, value);
		} else {
			if(type==MapObjectType.track) {
				key += TRACK_IDENTIFIER;
				super.put(key, value);
			}
		}
	}
	
	public V get(String key, MapObjectType type) {
		if(type==MapObjectType.route) {
			key += ROUTE_IDENTIFIER;
			return super.get(key);
		} else {
			if(type==MapObjectType.track) {
				key += TRACK_IDENTIFIER;
				return  super.get(key);
			} else
				return null; // Invalid key
		}
	}
	
	public V remove(String key, MapObjectType type) {
		if(type==MapObjectType.route) {
			key += ROUTE_IDENTIFIER;
			return super.remove(key);
		} else {
			if(type==MapObjectType.track) {
				key += TRACK_IDENTIFIER;
				return super.remove(key);
			} else
				return null; // Invalid key
		}
	}
}
