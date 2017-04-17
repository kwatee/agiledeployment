///*
// * ${kwatee_copyright}
// */
//
//package net.kwatee.agiledeployment.webapp.controller;
//
//import org.apache.commons.lang3.StringUtils;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//public class JsonHelper {
//
//	public static JSONObject toJsonObject(String serializedObject) {
//		return StringUtils.isEmpty(serializedObject) ? new JSONObject() : new JSONObject(serializedObject);
//	}
//
//	public static JSONArray toJsonArray(String serializedObject) {
//		return serializedObject == null ? new JSONArray() : new JSONArray(serializedObject);
//	}
//
//	public static void set(JSONObject jsonObject, String key, String value) {
//		if (StringUtils.isNotEmpty(value)) {
//			try {
//				jsonObject.put(key, value);
//			} catch (JSONException e) {}
//		}
//	}
//
//	public static void set(JSONObject jsonObject, String key, char value) {
//		try {
//			jsonObject.put(key, value);
//		} catch (JSONException e) {}
//	}
//
//	public static void set(JSONObject jsonObject, String key, Integer value) {
//		if (value != null) {
//			try {
//				jsonObject.put(key, value);
//			} catch (JSONException e) {}
//		}
//	}
//
//	public static void set(JSONObject jsonObject, String key, Long value) {
//		if (value != null) {
//			try {
//				jsonObject.put(key, value);
//			} catch (JSONException e) {}
//		}
//	}
//
//	public static void set(JSONObject jsonObject, String key, boolean value) {
//		if (value) {
//			try {
//				jsonObject.put(key, value);
//			} catch (JSONException e) {}
//		}
//	}
//
//	public static void set(JSONObject jsonObject, String key, JSONObject value) {
//		if (value != null) {
//			try {
//				jsonObject.put(key, value);
//			} catch (JSONException e) {}
//		}
//	}
//
//	public static void set(JSONObject jsonObject, String key, JSONArray value) {
//		if (value != null) {
//			try {
//				jsonObject.put(key, value);
//			} catch (JSONException e) {}
//		}
//	}
//
//	public static String getOptString(JSONObject jsonObject, String key) {
//		if (jsonObject.isNull(key)) {
//			return null;
//		}
//		return jsonObject.optString(key, null);
//	}
//
//	public static Integer getOptInt(JSONObject jsonObject, String key) {
//		if (jsonObject.isNull(key)) {
//			return null;
//		}
//		String s = jsonObject.optString(key, null);
//		if (s.isEmpty())
//			return 0;
//		try {
//			return Integer.decode(s);
//		} catch (NumberFormatException e) {
//			return null;
//		}
//	}
//}
