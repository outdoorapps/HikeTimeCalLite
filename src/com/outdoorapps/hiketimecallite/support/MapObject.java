package com.outdoorapps.hiketimecallite.support;

public class MapObject {
	public enum MapObjectType {route, track, invalid};

	private String name;
	private MapObjectType type;

	public MapObject(String name, MapObjectType type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public MapObjectType getType() {
		return type;
	}

	public static int mapObjectTypeToInt(MapObjectType type) {
		int result;
		switch(type) {
		case route:
			result = 0;
			break;
		case track:
			result = 1;
			break;
		case invalid:
			result = -1;
			break;
		default:
			result = -1;
			break;
		}
		return result;
	}

	public static MapObjectType intToMapObjectType(int typeInt) {
		MapObjectType type;
		switch(typeInt) {
		case -1:
			type = MapObjectType.invalid;
			break;
		case 0:
			type = MapObjectType.route;
			break;
		case 1:
			type = MapObjectType.track;
			break;
		default:
			type = MapObjectType.invalid;
			break;
		}
		return type;
	}
	
}
