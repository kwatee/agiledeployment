package net.kwatee.agiledeployment.mavenplugin;

class JsonUtils {

	static String jsonDescription(String description) {
		if (description == null || description.isEmpty())
			return null;
		return "{\"description\" : \"" + description + "\"}";
	}
}
