package co.clai.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Location;
import co.clai.db.model.User;
import co.clai.remote.OAuth2Helper.OAuth2Data;
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

	public XenforoConnection(Location l) {

		JSONObject data;

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
		this.secureToken = data.getString(CONFIG_KEY_TOKEN);
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
		return new JSONObject();
	}

	@Override
	public RemoteUserData getUserDataByUserId(int userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int loginUser(String username, String password) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Integer> getUsergroupsFromUserId(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RemoteUserData getUserDataByUserName(String userName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUsergroupNameById(int userGroupId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject getConfig() {
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RemoteUserData getUserDataByUserEmail(String userEmail) {
		return null;
	}

	@Override
	public List<Integer> getUserIdsFromUserGroup(int userGroupId) {
		// TODO Auto-generated method stub
		return new ArrayList<>();
	}

	@Override
	public String getUserFieldContentFromUserId(String fieldId, int userId) {
		// TODO Auto-generated method stub
		return "";
	}
}
