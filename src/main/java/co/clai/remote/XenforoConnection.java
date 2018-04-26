package co.clai.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Location;
import co.clai.db.model.User;
import co.clai.html.Builder;
import co.clai.remote.OAuth2Helper.OAuth2Data;
import co.clai.util.HttpRequestUtil;
import co.clai.util.RandomUtil;
import co.clai.util.StringStringPair;
import co.clai.util.cache.Cache;
import co.clai.util.cache.ExpiringCache;

public class XenforoConnection extends AbstractRemoteConnection {

	public final static String REMOTE_TYPE_NAME = "xenforo";

	private static final String JSON_KEY_REMOTE_PW_CHECKING = "remote_pw_checking";
	private final boolean remotePwChecking;
	private final static String CONFIG_KEY_REMOTESCRIPT = "remote";
	private final String remoteScript;
	private final static String CONFIG_KEY_TOKEN = "token";
	private final String secureToken;

	private final boolean allowPwLogin;

	private final OAuth2Data oAuth2Data;

	private final Cache<JSONObject> generalHttpRequestCache;

	private final JSONObject data;

	public XenforoConnection(Location l) {

		if (l == null) {
			data = null;
		} else {
			data = l.getConfig();
		}

		if (data == null) {
			remoteScript = null;
			secureToken = null;
			allowPwLogin = false;
			oAuth2Data = null;
			generalHttpRequestCache = null;
			remotePwChecking = false;
			return;
		}

		if (data.has(JSON_KEY_REMOTE_PW_CHECKING)) {
			remotePwChecking = Boolean.parseBoolean(data.getString(JSON_KEY_REMOTE_PW_CHECKING));
		} else {
			remotePwChecking = true;
		}

		if (data.has(JSON_KEY_ALLOW_PW_LOGIN)) {
			this.allowPwLogin = Boolean.parseBoolean(data.getString(JSON_KEY_ALLOW_PW_LOGIN));
		} else {
			this.allowPwLogin = true;
		}
		this.remoteScript = data.getString(CONFIG_KEY_REMOTESCRIPT);

		if (data.has(CONFIG_KEY_TOKEN)) {
			this.secureToken = data.getString(CONFIG_KEY_TOKEN);
		} else {
			this.secureToken = null;
		}
		this.oAuth2Data = OAuth2Helper.getOAuth2Data(data);
		String uniqueId = data.getString(AbstractRemoteConnection.UNIQUE_ID_KEY);
		generalHttpRequestCache = new ExpiringCache<>(uniqueId + "generalCache");
	}

	@Override
	protected String getRemoteTypeName() {
		return REMOTE_TYPE_NAME;
	}

	@Override
	public JSONObject getDefaultConfig() {
		JSONObject retObj = new JSONObject();

		retObj.put(UNIQUE_ID_KEY, RandomUtil.getRandomString());

		retObj.put(REMOTE_LOCATION_CONFIG_KEY_TYPE, REMOTE_TYPE_NAME);

		retObj.put(CONFIG_KEY_REMOTESCRIPT, "http://forum.yourcommunity.com/xf_query.php");
		retObj.put(JSON_KEY_ALLOW_PW_LOGIN, "false");

		retObj.put(CONFIG_KEY_TOKEN, "secret token here");

		retObj.put(OAuth2Data.JSON_KEY_OAUTH2_DATA, OAuth2Helper.generateOAuth2ConfigSkeleton());

		return retObj;
	}

	@Override
	public RemoteUserData getUserDataByUserId(int userId) {
		JSONObject userData = requestCachedJSONData(remoteScript,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserDataByUserId"),
						new StringStringPair("userId", userId + "")),
				generalHttpRequestCache);

		List<Integer> userGroupList = getUsergroupsFromUserId(userId);

		return new RemoteUserData(userId, userData.getString("username"), userGroupList, userData.getString("email"),
				"", "");
	}

	@Override
	public int loginUser(String username, String password) {
		logger.log(Level.WARNING, "login user not yet implemented in XenforoConnection");
		/// TODO implement
		return 0;
	}

	@Override
	public List<Integer> getUsergroupsFromUserId(int id) {
		List<Integer> retList = new ArrayList<>();

		JSONObject userGroupsData = requestCachedJSONData(remoteScript,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserGroupsFromUserId"),
						new StringStringPair("userId", id + "")),
				generalHttpRequestCache);

		if (!userGroupsData.has("ids")) {
			return retList;
		}

		JSONArray userGroupArr = userGroupsData.getJSONArray("ids");

		for (int i = 0; i < userGroupArr.length(); i++) {
			retList.add(new Integer(userGroupArr.getString(i)));
		}

		return retList;
	}

	@Override
	public RemoteUserData getUserDataByUserName(String userName) {
		JSONObject userData = requestCachedJSONData(remoteScript,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserDataByUserName"),
						new StringStringPair("userName", userName)),
				generalHttpRequestCache);

		int userId = Integer.parseInt(userData.getString("id"));

		List<Integer> userGroupList = getUsergroupsFromUserId(userId);

		return new RemoteUserData(userId, userData.getString("name"), userGroupList, userData.getString("email"), "",
				"");
	}

	@Override
	public String getUsergroupNameById(int userGroupId) {
		JSONObject userGroupsData = requestCachedJSONData(remoteScript,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserGroupNameById"),
						new StringStringPair("groupId", userGroupId + "")),
				generalHttpRequestCache);

		if (!userGroupsData.has("title")) {
			return "Unknown Usergroup";
		}

		return userGroupsData.getString("title");
	}

	@Override
	public JSONObject getConfig() {
		return data;
	}

	@Override
	public boolean canDoPasswordLogin() {
		return allowPwLogin;
	}

	@Override
	public boolean canDoOAuth2Login() {
		return !(oAuth2Data == null);
	}

	@Override
	public OAuth2Data getOAuth2Data() {
		return oAuth2Data;
	}

	@Override
	public User getUserWithOAuth2Code(DatabaseConnector dbCon, Map<String, String[]> parameters, int locationId) {
		try {
			final String siteUrl = dbCon.getListener().getSiteUrl();

			logger.log(Level.INFO, oAuth2Data.accessTokenUrl.toString());

			final Map<String, String> buildTokenRequest = OAuth2Helper.buildTokenRequest(siteUrl,
					parameters.get("code")[0], oAuth2Data);

			logger.log(Level.INFO, buildTokenRequest.toString());

			String tokenRequestData = HttpRequestUtil.httpPostRequestAsString(oAuth2Data.accessTokenUrl,
					buildTokenRequest);

			logger.log(Level.INFO, tokenRequestData);

			JSONObject jTokenData = new JSONObject(tokenRequestData);

			String token = jTokenData.getString(OAuth2Helper.OAUTH_DATA_KEY_ACCESS_TOKEN);

			final int userId = jTokenData.getInt("user_id");
			String userRequestData = HttpRequestUtil.httpRequest(oAuth2Data.profileUrl + "?" + "users/:" + userId
					+ OAuth2Helper.OAUTH_DATA_KEY_ACCESS_TOKEN + "=" + Builder.escapeForHtml(token));

			logger.log(Level.INFO, userRequestData);

			JSONObject jUserData = new JSONObject(userRequestData);

			if (!jUserData.has("links")) {
				return null;
			}

			RemoteUserData userdata = getUserDataByUserId(userId);

			return new User(dbCon, userdata.getUsername(), userId, locationId,
					Location.getLocationById(dbCon, locationId).getCommunityId(), null, false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public RemoteUserData getUserDataByUserEmail(String userEmail) {
		JSONObject userData = requestCachedJSONData(remoteScript,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserDataByUserName"),
						new StringStringPair("userEmail", userEmail)),
				generalHttpRequestCache);

		int userId = Integer.parseInt(userData.getString("id"));

		List<Integer> userGroupList = getUsergroupsFromUserId(userId);

		return new RemoteUserData(userId, userData.getString("name"), userGroupList, userData.getString("email"), "",
				"");
	}

	@Override
	public List<Integer> getUserIdsFromUserGroup(int userGroupId) {

		JSONObject userGroupsData = requestCachedJSONData(remoteScript,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserIdsFromUserGroup"),
						new StringStringPair("groupId", userGroupId + "")),
				generalHttpRequestCache);

		List<Integer> retList = new ArrayList<>();

		if (!userGroupsData.has("ids")) {
			return retList;
		}

		JSONArray userGroupArr = userGroupsData.getJSONArray("ids");

		for (int i = 0; i < userGroupArr.length(); i++) {
			retList.add(new Integer(userGroupArr.getString(i)));
		}

		return retList;
	}

	@Override
	public String getUserFieldContentFromUserId(String fieldId, int userId) {
		JSONObject fieldContentData = requestCachedJSONData(remoteScript,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserFieldContentFromUserId"),
						new StringStringPair("userId", userId + ""), new StringStringPair("fieldId", fieldId)),
				generalHttpRequestCache);

		if (!fieldContentData.has("field_value") || fieldContentData.isNull("field_value")) {
			return null;
		}

		return fieldContentData.getString("field_value");
	}
}
