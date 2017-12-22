package co.clai.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Location;
import co.clai.db.model.User;
import co.clai.remote.OAuth2Helper.OAuth2Data;
import co.clai.util.RandomUtil;

public class DummyConnection extends AbstractRemoteConnection {

	public static final String CONNECTION_TYPE = "remoteDummy";

	public static final String JSON_KEY_USERGROUPS = "usergroups";
	private static final String JSON_KEY_PASSWORD = "password";
	private static final String JSON_KEY_USER_NAME_ID_MAP = "userNameIdMap";
	private static final String JSON_KEY_EMAIL = "email";
	private static final String JSON_KEY_USERNAME = "username";
	private static final String JSON_KEY_USER_ID = "userId";
	private static final String JSON_KEY_USER = "user";

	private final JSONObject data;

	public DummyConnection(JSONObject data) {
		this.data = data;
	}

	public DummyConnection(Location l) {
		JSONObject tmpData;

		if (l == null) {
			tmpData = null;
		} else {
			tmpData = l.getConfig();
		}

		this.data = tmpData;
	}

	@Override
	protected String getRemoteTypeName() {
		return CONNECTION_TYPE;
	}

	@Override
	public RemoteUserData getUserDataByUserId(int userId) {

		JSONObject userJSON = data.getJSONObject(JSON_KEY_USER).getJSONObject(userId + "");

		List<Integer> userGroups = getUsergroupsFromUserId(userId);

		return new RemoteUserData(Integer.parseInt(userJSON.getString(JSON_KEY_USER_ID)),
				userJSON.getString(JSON_KEY_USERNAME), userGroups, userJSON.getString(JSON_KEY_EMAIL));
	}

	@Override
	public RemoteUserData getUserDataByUserName(String userName) {

		int userid = Integer.parseInt(data.getJSONObject(JSON_KEY_USER_NAME_ID_MAP).getString(userName));

		return getUserDataByUserId(userid);
	}

	@Override
	public int loginUser(String username, String password) {

		int userId = Integer.parseInt(data.getJSONObject(JSON_KEY_USER_NAME_ID_MAP).getString(username));

		JSONObject userJSON = data.getJSONObject(JSON_KEY_USER).getJSONObject(userId + "");

		if (BCrypt.checkpw(password, userJSON.getString(JSON_KEY_PASSWORD))) {
			return Integer.parseInt(userJSON.getString(JSON_KEY_USER_ID));
		}

		return -1;
	}

	@Override
	public List<Integer> getUsergroupsFromUserId(int id) {

		JSONObject userJSON = data.getJSONObject(JSON_KEY_USER).getJSONObject(id + "");

		List<Integer> retList = new ArrayList<>();

		JSONArray uGroups = userJSON.getJSONArray(JSON_KEY_USERGROUPS);

		for (int i = 0; i < uGroups.length(); i++) {
			retList.add(new Integer(Integer.parseInt(uGroups.getString(i))));
		}

		return retList;
	}

	@Override
	public String getUsergroupNameById(int userGroupId) {
		return data.getJSONObject(JSON_KEY_USERGROUPS).getString(userGroupId + "");
	}

	@Override
	public JSONObject getDefaultConfig() {
		return getPlainDataObject();
	}

	public static JSONObject getPlainDataObject() {
		JSONObject retObj = new JSONObject();

		retObj.put(UNIQUE_ID_KEY, RandomUtil.getRandomString());

		retObj.put(JSON_KEY_USER, new JSONObject());
		retObj.put(JSON_KEY_USER_NAME_ID_MAP, new JSONObject());
		retObj.put(JSON_KEY_USERGROUPS, new JSONObject());
		retObj.put(AbstractRemoteConnection.REMOTE_LOCATION_CONFIG_KEY_TYPE, CONNECTION_TYPE);

		return retObj;
	}

	public static void addUser(JSONObject jData, int id, String name, String password, List<Integer> userGroups,
			String email) {

		JSONObject newUser = new JSONObject();
		newUser.put(JSON_KEY_USER_ID, id + "");
		newUser.put(JSON_KEY_USERNAME, name);
		newUser.put(JSON_KEY_EMAIL, email);
		newUser.put(JSON_KEY_PASSWORD, BCrypt.hashpw(password, BCrypt.gensalt()));

		JSONArray usergroupJA = new JSONArray();
		for (Integer i : userGroups) {
			usergroupJA.put(i + "");
		}

		newUser.put(JSON_KEY_USERGROUPS, usergroupJA);

		jData.getJSONObject(JSON_KEY_USER).put(id + "", newUser);
		jData.getJSONObject(JSON_KEY_USER_NAME_ID_MAP).put(name, id + "");
	}

	public static void addUserGroup(JSONObject jData, int id, String userGroupName) {
		if (!jData.has(JSON_KEY_USERGROUPS)) {
			jData.put(JSON_KEY_USERGROUPS, new JSONObject());
		}

		jData.getJSONObject(JSON_KEY_USERGROUPS).put(id + "", userGroupName);
	}

	@Override
	public JSONObject getConfig() {
		return new JSONObject(data.toString());
	}

	@Override
	public boolean canDoPasswordLogin() {
		return true;
	}

	@Override
	public boolean canDoOAuth2Login() {
		return false;
	}

	@Override
	public OAuth2Data getOAuth2Data() {
		return null;
	}

	@Override
	public User getUserWithOAuth2Code(DatabaseConnector dbCon, Map<String, String[]> parameters, int locationId) {
		return null;
	}

	@Override
	public RemoteUserData getUserDataByUserEmail(String userEmail) {
		return null;
	}

	@Override
	public List<Integer> getUserIdsFromUserGroup(int userGroupId) {
		// TODO implement
		return new ArrayList<>();
	}

	@Override
	public String getUserFieldContentFromUserId(String fieldId, int userId) {
		// TODO implement
		return "";
	}
}
