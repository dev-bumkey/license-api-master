package run.acloud.commons.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;

@Slf4j
public final class JsonUtils {
	public final static Gson gson = new Gson();

	public static <T> String toPrettyString(T target) {
		try {
			return new GsonBuilder()
					.registerTypeAdapter(DateTime.class, (JsonSerializer<DateTime>) (json, typeOfSrc, context) -> new JsonPrimitive(ISODateTimeFormat.dateTime().print(json)))
					.registerTypeAdapter(DateTime.class, (JsonDeserializer<DateTime>) (json, typeOfT, context) -> {
						DateTime dt = ISODateTimeFormat.dateTime().parseDateTime(json.getAsString());
						return dt;
					})
					.setPrettyPrinting().create().toJson(target);
		} catch (Exception e) {
			log.error("Error JsonUtils.toPrettyString", e);
			return null;
		}
	}
	
	public static String toGson(Object obj) {
		return gson.toJson(obj);
	}

	public static <T> T fromGson(String json, Class<T> classOfT) {
		return gson.fromJson(json, classOfT);
	}

	public static <T> T fromJackson(String json, Class<T> classOfT) throws Exception {
		return ObjectMapperUtils.getDateTimeMapper().readValue(json, classOfT);
	}

	public static <T> T fromJackson(String json, TypeReference<T> valueTypeRef) throws Exception {
		return ObjectMapperUtils.getDateTimeMapper().readValue(json, valueTypeRef);
	}

	public static <T> T fromGson(String json, Type typeOfT) {
		return gson.fromJson(json, typeOfT);
	}

	public static <T> T fromGson(JsonElement json, Class<T> classOfT) {
		return gson.fromJson(json, classOfT);
	}

	public static <T> T fromGson(JsonElement json, Type typeOfT) {
		return gson.fromJson(json, typeOfT);
	}

	public static JsonObject toJsonObject(Object obj) {
		String json = gson.toJson(obj);
//		log.debug(json);
		JsonParser jp = new JsonParser();
		return jp.parse(json).getAsJsonObject();
	}

	public static JsonObject toJsonObject(String json) {
		JsonParser jp = new JsonParser();
		return jp.parse(json).getAsJsonObject();
	}

	public static JsonArray toJsonArray(String json) {
		JsonParser jp = new JsonParser();
		return jp.parse(json).getAsJsonArray();
	}

	public static JsonObject getResultSpec(boolean error, String message, JsonElement results) {
		JsonObject jo = new JsonObject();
		jo.addProperty("error", error);
		jo.addProperty("message", message);
		jo.add("results", results);
		return jo;
	}

	public static <T> T copyObject(Object obj, Type typeOfT) {
		String str = toGson(obj);
		return fromGson(str, typeOfT);
	}

	public static String escape(String input) {
		StringBuilder output = new StringBuilder();

		for(int i=0; i<input.length(); i++) {
			char ch = input.charAt(i);
			int chx = (int) ch;

			// let's not put any nulls in our strings
			assert(chx != 0);

			if(ch == '\n') {
				output.append("\\n");
			} else if(ch == '\t') {
				output.append("\\t");
			} else if(ch == '\r') {
				output.append("\\r");
			} else if(ch == '\\') {
				output.append("\\\\");
			} else if(ch == '"') {
				output.append("\\\"");
			} else if(ch == '\b') {
				output.append("\\b");
			} else if(ch == '\f') {
				output.append("\\f");
			} else if(chx >= 0x10000) {
				assert false : "Java stores as u16, so it should never give us a character that's bigger than 2 bytes. It literally can't.";
			} else if(chx > 127) {
				output.append(String.format("\\u%04x", chx));
			} else {
				output.append(ch);
			}
		}

		return output.toString();
	}

	public static String unescape(String input) {
		StringBuilder builder = new StringBuilder();

		int i = 0;
		while (i < input.length()) {
			char delimiter = input.charAt(i); i++; // consume letter or backslash

			if(delimiter == '\\' && i < input.length()) {

				// consume first after backslash
				char ch = input.charAt(i); i++;

				if(ch == '\\' || ch == '/' || ch == '"' || ch == '\'') {
					builder.append(ch);
				}
				else if(ch == 'n') builder.append('\n');
				else if(ch == 'r') builder.append('\r');
				else if(ch == 't') builder.append('\t');
				else if(ch == 'b') builder.append('\b');
				else if(ch == 'f') builder.append('\f');
				else if(ch == 'u') {

					StringBuilder hex = new StringBuilder();

					// expect 4 digits
					if (i+4 > input.length()) {
						throw new RuntimeException("Not enough unicode digits! ");
					}
					for (char x : input.substring(i, i + 4).toCharArray()) {
						if(!Character.isLetterOrDigit(x)) {
							throw new RuntimeException("Bad character in unicode escape.");
						}
						hex.append(Character.toLowerCase(x));
					}
					i+=4; // consume those four digits.

					int code = Integer.parseInt(hex.toString(), 16);
					builder.append((char) code);
				} else {
					throw new RuntimeException("Illegal escape sequence: \\"+ch);
				}
			} else { // it's not a backslash, or it's the last character.
				builder.append(delimiter);
			}
		}

		return builder.toString();
	}
}
