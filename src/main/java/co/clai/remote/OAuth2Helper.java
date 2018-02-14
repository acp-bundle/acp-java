package co.clai.remote;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import co.clai.module.FunctionResult;
import co.clai.module.OAuth2;

public class OAuth2Helper {

	public static final String OAUTH_DATA_KEY_ACCESS_TOKEN = "access_token";
	public static final String OAUTH_DATA_KEY_CLIENT_ID = "client_id";
	public static final String OAUTH_DATA_KEY_REDIRECT_URI = "redirect_uri";
	public static final String OAUTH_DATA_KEY_CLIENT_SECRET = "client_secret";

	public static String OAUTH_DATA_KEY_CODE = "code";

	public class OAuth2Data {

		public static final String JSON_KEY_OAUTH2_DATA = "oauth2data";

		public static final String JSON_KEY_CLIENT_ID = "clientId";
		public static final String JSON_KEY_CLIENT_SECRET = "clientSecret";
		public static final String JSON_KEY_AUTHORIZATION_URL = "authorizationUrl";
		public static final String JSON_KEY_ACCESS_TOKEN_URL = "accessTokenUrl";
		public static final String JSON_KEY_PROFILE_URL = "profileUrl";

		public final String clientId;
		public final String clientSecret;
		public final String authorizationUrl;
		public final String accessTokenUrl;
		public final String profileUrl;

		public OAuth2Data(JSONObject data) {
			this.clientId = data.getString(JSON_KEY_CLIENT_ID);
			this.clientSecret = data.getString(JSON_KEY_CLIENT_SECRET);
			this.authorizationUrl = data.getString(JSON_KEY_AUTHORIZATION_URL);
			this.accessTokenUrl = data.getString(JSON_KEY_ACCESS_TOKEN_URL);
			this.profileUrl = data.getString(JSON_KEY_PROFILE_URL);
		}

	}

	public static OAuth2Data getOAuth2Data(JSONObject data) {

		if (data == null) {
			return null;
		}

		if (!data.has(OAuth2Data.JSON_KEY_OAUTH2_DATA)) {
			return null;
		}

		return (new OAuth2Helper()).new OAuth2Data(data.getJSONObject(OAuth2Data.JSON_KEY_OAUTH2_DATA));
	}

	public static JSONObject generateOAuth2ConfigSkeleton() {
		JSONObject retObj = new JSONObject();

		retObj.put(OAuth2Data.JSON_KEY_CLIENT_ID, "the Client comes here");
		retObj.put(OAuth2Data.JSON_KEY_CLIENT_SECRET, "the Client secret here");
		retObj.put(OAuth2Data.JSON_KEY_AUTHORIZATION_URL, "the Authorization Url here");
		retObj.put(OAuth2Data.JSON_KEY_ACCESS_TOKEN_URL, "the Access Tokel Url here");
		retObj.put(OAuth2Data.JSON_KEY_PROFILE_URL, "the Profile Url here");

		return retObj;
	}

	public static FunctionResult generateRedirect(String acpSiteUrl, OAuth2Data oAuth2Data) {
		try {
			String remoteLoginUrl = oAuth2Data.authorizationUrl;

			URIBuilder b = new URIBuilder(remoteLoginUrl);

			b.addParameter(OAUTH_DATA_KEY_CLIENT_ID, oAuth2Data.clientId);
			b.addParameter(OAUTH_DATA_KEY_REDIRECT_URI,
					acpSiteUrl + "/" + OAuth2.LOCATION + "." + OAuth2.FUNCTION_NAME_CALLBACK);
			b.addParameter("response_type", "code");

			return new FunctionResult(FunctionResult.Status.NONE, b);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Map<String, String> buildTokenRequest(String acpSiteUrl, String code, OAuth2Data oAuth2Data) {

		Map<String, String> retMap = new HashMap<>();

		retMap.put(OAUTH_DATA_KEY_CODE, code);
		retMap.put(OAUTH_DATA_KEY_CLIENT_ID, oAuth2Data.clientId);
		retMap.put(OAUTH_DATA_KEY_CLIENT_SECRET, oAuth2Data.clientSecret);
		retMap.put(OAUTH_DATA_KEY_REDIRECT_URI,
				acpSiteUrl + "/" + OAuth2.LOCATION + "." + OAuth2.FUNCTION_NAME_CALLBACK);
		retMap.put("grant_type", "authorization_code");

		return retMap;
	}

	public static Map<String, String> buildUserRequest(String token) {

		Map<String, String> retMap = new HashMap<>();

		retMap.put(OAUTH_DATA_KEY_ACCESS_TOKEN, token);
		return retMap;
	}

}
