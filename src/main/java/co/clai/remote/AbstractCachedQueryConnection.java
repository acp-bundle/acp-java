package co.clai.remote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import co.clai.util.HttpRequestUtil;
import co.clai.util.StringStringPair;
import co.clai.util.cache.Cache;
import co.clai.util.log.LoggingUtil;

public abstract class AbstractCachedQueryConnection {

	protected final static Logger logger = LoggingUtil.getDefaultLogger();

	private final static String serializeParameterList(String scriptLocation, List<StringStringPair> parameters) {
		StringBuilder sb = new StringBuilder();

		sb.append(scriptLocation + "@");

		for (StringStringPair p : parameters) {
			sb.append(p.getId() + "=" + p.getName() + "@");
		}

		return sb.toString();
	}

	protected final static JSONObject requestCachedJSONData(String scriptLocation, List<StringStringPair> parameters,
			Cache<JSONObject> cache) {

		String identifier = serializeParameterList(scriptLocation, parameters);

		JSONObject retrievedData = cache.retrieve(identifier);
		if (retrievedData != null) {
			return retrievedData;
		}

		Map<String, String> newParams = new HashMap<>();

		for (StringStringPair p : parameters) {
			newParams.put(p.getId(), p.getName());
		}

		try {
			String data = HttpRequestUtil.httpPostRequestAsString(scriptLocation, newParams);

			JSONObject jData = new JSONObject(data);

			if (jData.has("errorId")) {
				logger.log(Level.WARNING, "request from remote script returned error " + jData.getInt("errorId")
						+ ", \"" + jData.getString("message") + "\"");
			}

			cache.put(identifier, jData);

			return jData;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "cannot request data from remote script location", e);
			return new JSONObject();
		}
	}

	protected final static byte[] requestCachedPOSTData(String scriptLocation, List<StringStringPair> parameters,
			Cache<byte[]> cache) {
		return requestCachedPOSTData(scriptLocation, parameters, cache, null, null);
	}

	protected final static byte[] requestCachedPOSTData(String scriptLocation, List<StringStringPair> parameters,
			Cache<byte[]> cache, String httpUser, String httpPwd) {

		String identifier = serializeParameterList(scriptLocation, parameters);

		byte[] retrievedData = cache.retrieve(identifier);
		if (retrievedData != null) {
			return retrievedData;
		}

		Map<String, String> newParams = new HashMap<>();

		for (StringStringPair p : parameters) {
			newParams.put(p.getId(), p.getName());
		}

		try {
			byte[] data = HttpRequestUtil.httpGetRequest(scriptLocation, newParams, httpUser, httpPwd);

			cache.put(identifier, data);

			return data;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "cannot request data from remote script location", e);
			return new byte[0];
		}
	}

	protected static final String postHttpContent(String queryScriptLocation, Map<String, String> parameter) {
		try {
			return HttpRequestUtil.httpPostRequestAsString(queryScriptLocation, parameter);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected static final JSONObject postHttpContentAsJSON(String queryScriptLocation, Map<String, String> parameter) {
		return new JSONObject(postHttpContent(queryScriptLocation, parameter));
	}

	protected static final String getHttpContent(String queryScriptLocation, Map<String, String> parameter) {
		return HttpRequestUtil.httpRequest(queryScriptLocation, parameter);
	}

	protected static final JSONObject getHttpContentAsJSON(String queryScriptLocation, Map<String, String> parameter) {
		return new JSONObject(getHttpContent(queryScriptLocation, parameter));
	}

}
