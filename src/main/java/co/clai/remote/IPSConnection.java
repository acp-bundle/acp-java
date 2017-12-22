package co.clai.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Location;
import co.clai.db.model.User;
import co.clai.html.Builder;
import co.clai.remote.OAuth2Helper.OAuth2Data;
import co.clai.util.HttpRequestUtil;
import co.clai.util.RandomUtil;
import co.clai.util.StringStringPair;
import co.clai.util.StringUtil;
import co.clai.util.cache.Cache;
import co.clai.util.cache.ExpiringCache;
import co.clai.util.cache.PermanentCache;

public class IPSConnection extends AbstractRemoteConnection {

	private static final String IPS_USERDATA_RETURN_KEY_ID = "id";
	private static final String IPS_USERDATA_RETURN_KEY_USERNAME = "username";

	public final static String REMOTE_TYPE_NAME = "ips";

	private final static String CONFIG_KEY_TOKEN = "token";
	private final String secureToken;
	private static final String JSON_KEY_REMOTE_PW_CHECKING = "remote_pw_checking";
	private final boolean remotePwChecking;
	private static final String JSON_KEY_COLLAB_ID = "collabId";
	private final int collabId;
	private static final String JSON_KEY_QUERY_SCRIPT_LOCATION = "queryScriptLocation";
	private final String queryScriptLocation;
	private final boolean allowPwLogin;

	private final JSONObject data;
	private final Location location;

	private final OAuth2Helper.OAuth2Data oAuth2Data;

	private final Cache<String> usernameCache;
	private final Cache<List<Integer>> userGroupCache;

	private final Cache<JSONObject> generalHttpRequestCache;

	public IPSConnection(Location location) {
		JSONObject tmpData;

		this.location = location;

		if (location == null) {
			tmpData = null;
		} else {
			tmpData = location.getConfig();
		}

		this.data = tmpData;
		if (tmpData == null) {
			collabId = -1;
			queryScriptLocation = null;
			secureToken = null;
			oAuth2Data = null;
			allowPwLogin = false;
			usernameCache = null;
			userGroupCache = null;
			generalHttpRequestCache = null;
			remotePwChecking = false;
			return;
		}

		oAuth2Data = OAuth2Helper.getOAuth2Data(tmpData);
		String uniqueId = tmpData.getString(AbstractRemoteConnection.UNIQUE_ID_KEY);

		if (tmpData.has(JSON_KEY_QUERY_SCRIPT_LOCATION)) {
			queryScriptLocation = tmpData.getString(JSON_KEY_QUERY_SCRIPT_LOCATION);
			secureToken = tmpData.getString(CONFIG_KEY_TOKEN);

			if (tmpData.has(JSON_KEY_REMOTE_PW_CHECKING)) {
				remotePwChecking = Boolean.parseBoolean(tmpData.getString(JSON_KEY_REMOTE_PW_CHECKING));
			} else {
				remotePwChecking = true;
			}

			if (tmpData.has(JSON_KEY_COLLAB_ID)) {
				collabId = Integer.parseInt(tmpData.getString(JSON_KEY_COLLAB_ID));
			} else {
				collabId = -1;
			}

			if (tmpData.has(JSON_KEY_ALLOW_PW_LOGIN)) {
				allowPwLogin = Boolean.parseBoolean(tmpData.getString(JSON_KEY_ALLOW_PW_LOGIN));
			} else {
				allowPwLogin = true;
			}
			usernameCache = null;
			userGroupCache = null;
			generalHttpRequestCache = new ExpiringCache<>(uniqueId + "generalCache");
		} else {
			queryScriptLocation = null;
			secureToken = null;
			collabId = -1;
			allowPwLogin = false;
			usernameCache = new PermanentCache<>(uniqueId + "usernameCache");
			userGroupCache = new PermanentCache<>(uniqueId + "userGroupCache");
			generalHttpRequestCache = null;
			remotePwChecking = false;
		}

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

		retObj.put(JSON_KEY_COLLAB_ID, "0");
		retObj.put(JSON_KEY_QUERY_SCRIPT_LOCATION, "http://forum.yourcommunity.com/clai_acp/ips_query.php");
		retObj.put(JSON_KEY_ALLOW_PW_LOGIN, "false");

		retObj.put(OAuth2Data.JSON_KEY_OAUTH2_DATA, OAuth2Helper.generateOAuth2ConfigSkeleton());

		return retObj;
	}

	@Override
	public RemoteUserData getUserDataByUserId(int userId) {

		if (this.queryScriptLocation == null) {
			if (usernameCache != null) {
				return new RemoteUserData(userId, usernameCache.retrieve(userId + ""), new ArrayList<>(),
						"unknown@unknown.com");
			}

			return new RemoteUserData(userId, "Unknown User", new ArrayList<>(), "unknown@unknown.com");
		}

		JSONObject userData = requestCachedJSONData(queryScriptLocation,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserDataByUserId"),
						new StringStringPair("userId", userId + "")),
				generalHttpRequestCache);

		List<Integer> userGroupList = getUsergroupsFromUserId(userId);

		return new RemoteUserData(userId, userData.getString("name"), userGroupList, userData.getString("email"),
				userData.getString("members_pass_hash"), userData.getString("members_pass_salt"));
	}

	@Override
	public RemoteUserData getUserDataByUserName(String userName) {
		if (this.queryScriptLocation == null) {
			throw new RuntimeException("cannot get User data without query Script");
		}

		JSONObject userData = requestCachedJSONData(queryScriptLocation,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserDataByUserName"),
						new StringStringPair("userName", userName + "")),
				generalHttpRequestCache);

		int userId = Integer.parseInt(userData.getString("member_id"));

		List<Integer> userGroupList = getUsergroupsFromUserId(userId);

		return new RemoteUserData(userId, userData.getString("name"), userGroupList, userData.getString("email"),
				userData.getString("members_pass_hash"), userData.getString("members_pass_salt"));
	}

	@Override
	public RemoteUserData getUserDataByUserEmail(String userEmail) {
		if (this.queryScriptLocation == null) {
			throw new RuntimeException("cannot get User data without query Script");
		}

		JSONObject userData = requestCachedJSONData(queryScriptLocation,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserDataByUserEmail"),
						new StringStringPair("userEmail", userEmail + "")),
				generalHttpRequestCache);

		int userId = Integer.parseInt(userData.getString("member_id"));

		List<Integer> userGroupList = getUsergroupsFromUserId(userId);

		return new RemoteUserData(userId, userData.getString("name"), userGroupList, userData.getString("email"),
				userData.getString("members_pass_hash"), userData.getString("members_pass_salt"));
	}

	@Override
	public int loginUser(String username, String password) {

		if (remotePwChecking) {

			JSONObject userGroupsData = requestCachedJSONData(queryScriptLocation,
					Arrays.asList(new StringStringPair("action", "ips_query"),
							new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
							new StringStringPair("method", "loginUser"), new StringStringPair("userName", username),
							new StringStringPair("plainPass", password)),
					generalHttpRequestCache);

			if (userGroupsData.has("member_id")) {
				return Integer.parseInt(userGroupsData.getString("member_id"));
			}

			return -1;

		}
		logger.log(Level.INFO, "logging in user " + username);

		if (this.queryScriptLocation == null) {
			throw new RuntimeException("cannot login without query Script");
		}

		if (!allowPwLogin) {
			logger.log(Level.WARNING, "User tried to login despite being forbidden.");
			throw new RuntimeException("loginUser not allowed with this location");
		}

		RemoteUserData uData = getUserDataByUserName(username);

		if (uData == null) {
			uData = getUserDataByUserEmail(username);
		}

		if (uData == null) {
			logger.log(Level.WARNING, "User with Username " + username + " not found in Location " + location.getId());
			throw new RuntimeException("User not found");
		}

		String passwordHash = uData.getPasswordHash();

		logger.log(Level.INFO, passwordHash + " -->");

		String[] hashvars = passwordHash.split("\\$");

		logger.log(Level.INFO, "0: " + hashvars[0] + " 1: " + hashvars[1] + " 2: " + hashvars[2] + " " + " 3: "
				+ hashvars[3] + " " + uData.getPasswordSalt());

		String tempSalt = hashvars[2] + "$" + hashvars[3] + "$" + uData.getPasswordSalt();

		logger.log(Level.INFO, "--> " + tempSalt);

		String hashedProvidedPw = BCrypt.hashpw(password, tempSalt);

		if (StringUtil.checkBytes(passwordHash.getBytes(), hashedProvidedPw.getBytes())) {
			return uData.getId();
		}

		/// TODO fix this password crap

		return -1;
	}

	@Override
	public List<Integer> getUsergroupsFromUserId(int id) {
		if (this.queryScriptLocation == null) {
			if (usernameCache != null) {
				return userGroupCache.retrieve(id + "");
			}

			return new ArrayList<>();
		}

		List<Integer> retList = new ArrayList<>();

		if (collabId >= 0) {

			JSONObject userGroupsData = requestCachedJSONData(queryScriptLocation,
					Arrays.asList(new StringStringPair("action", "ips_query"),
							new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
							new StringStringPair("collabId", collabId + ""),
							new StringStringPair("method", "getCollabGroupsFromUserId"),
							new StringStringPair("userId", id + "")),
					generalHttpRequestCache);

			if (userGroupsData.has("roles")) {

				String rolesNumber[] = userGroupsData.getString("roles").split(",");

				for (String s : rolesNumber) {
					if (!"".equals(s)) {
						retList.add(new Integer(s));
					}
				}
			} else {
				logger.log(Level.WARNING,
						"User with id " + id + " has no roles for collab id " + collabId + "; " + queryScriptLocation);
			}
		} else {

			JSONObject userGroupsData = requestCachedJSONData(queryScriptLocation,
					Arrays.asList(new StringStringPair("action", "ips_query"),
							new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
							new StringStringPair("method", "getUserGroupsFromUserId"),
							new StringStringPair("userId", id + "")),
					generalHttpRequestCache);

			retList.add(new Integer(userGroupsData.getString("member_group_id")));

			String rolesNumber[] = userGroupsData.getString("mgroup_others").split(",");

			for (String s : rolesNumber) {
				retList.add(new Integer(s));
			}
		}
		return retList;
	}

	@Override
	public String getUsergroupNameById(int userGroupId) {
		if (this.queryScriptLocation == null) {
			try {
				return data.getJSONObject(DummyConnection.JSON_KEY_USERGROUPS).getString(userGroupId + "");
			} catch (Exception e) {
				throw new RuntimeException("cannot get User Group Name without query Script: " + e.getMessage());
			}
		}

		if (collabId >= 0) {

			JSONObject userGroupsData = requestCachedJSONData(queryScriptLocation,
					Arrays.asList(new StringStringPair("action", "ips_query"),
							new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
							new StringStringPair("method", "getCollabGroupNameById"),
							new StringStringPair("groupId", userGroupId + ""),
							new StringStringPair("collabId", collabId + "")),
					generalHttpRequestCache);

			if (!userGroupsData.has("name")) {
				return "Unknown Usergroup";
			}

			return userGroupsData.getString("name");
		}

		JSONObject userGroupsData = requestCachedJSONData(queryScriptLocation,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserGroupNameById"),
						new StringStringPair("groupId", userGroupId + "")),
				generalHttpRequestCache);

		if (!userGroupsData.has("word_default")) {
			return "Unknown Usergroup";
		}

		return userGroupsData.getString("word_default");
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
			String tokenRequestData = HttpRequestUtil.httpPostRequestAsString(oAuth2Data.accessTokenUrl,
					OAuth2Helper.buildTokenRequest(siteUrl, parameters.get("code")[0], oAuth2Data));

			logger.log(Level.INFO, tokenRequestData);

			JSONObject jTokenData = new JSONObject(tokenRequestData);

			String token = jTokenData.getString(OAuth2Helper.OAUTH_DATA_KEY_ACCESS_TOKEN);

			String userRequestData = HttpRequestUtil.httpRequest(oAuth2Data.profileUrl + "?"
					+ OAuth2Helper.OAUTH_DATA_KEY_ACCESS_TOKEN + "=" + Builder.escapeForHtml(token));

			logger.log(Level.INFO, userRequestData);

			JSONObject jUserData = new JSONObject(userRequestData);

			final String username = jUserData.getString(IPS_USERDATA_RETURN_KEY_USERNAME);
			final int userId = jUserData.getInt(IPS_USERDATA_RETURN_KEY_ID);

			if (usernameCache != null) {
				usernameCache.put(userId + "", username);
			}

			List<Integer> usergroups = new ArrayList<>();

			usergroups.add(new Integer(jUserData.getInt("group")));

			JSONArray secondaryGroups = jUserData.getJSONArray("group_others");

			for (int i = 0; i < secondaryGroups.length(); i++) {
				String userGroup = secondaryGroups.getString(i);
				if (!"".equals(userGroup)) {
					usergroups.add(new Integer(Integer.parseInt(userGroup)));
				}
			}

			if (userGroupCache != null) {
				userGroupCache.put(userId + "", usergroups);
				boolean changed = false;
				for (int uGroupId : usergroups) {
					if (!(data.has(DummyConnection.JSON_KEY_USERGROUPS)
							&& data.getJSONObject(DummyConnection.JSON_KEY_USERGROUPS).has(uGroupId + ""))) {
						DummyConnection.addUserGroup(data, uGroupId, "unknown Group");

						changed = true;
					}
				}

				if (changed) {
					location.changeConfig(dbCon, data.toString());
				}

			}

			return new User(dbCon, username, userId, locationId,
					Location.getLocationById(dbCon, locationId).getCommunityId(), null, false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Integer> getUserIdsFromUserGroup(int userGroupId) {
		if (this.queryScriptLocation == null) {
			throw new RuntimeException("cannot get User ids from collab groups without query Script");
		}

		if (collabId < 0) {
			logger.log(Level.WARNING, "no collabId specified when trying to get UserField Content");

			JSONObject userGroupsData = requestCachedJSONData(queryScriptLocation,
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

		JSONObject userGroupsData = requestCachedJSONData(queryScriptLocation,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserIdsFromCollabGroup"),
						new StringStringPair("groupId", userGroupId + ""),
						new StringStringPair("collabId", collabId + "")),
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
		if (this.queryScriptLocation == null) {
			throw new RuntimeException("cannot get User Field Content without query Script");
		}

		JSONObject fieldContentData = requestCachedJSONData(queryScriptLocation,
				Arrays.asList(new StringStringPair("action", "ips_query"),
						new StringStringPair(CONFIG_KEY_TOKEN, secureToken),
						new StringStringPair("method", "getUserFieldContentFromUserId"),
						new StringStringPair("userId", userId + ""), new StringStringPair("fieldId", fieldId)),
				generalHttpRequestCache);

		if (!fieldContentData.has(fieldId) || fieldContentData.isNull(fieldId)) {
			return null;
		}

		return fieldContentData.getString(fieldId);
	}
}
