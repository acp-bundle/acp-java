package co.clai.remote;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.json.JSONObject;
import org.reflections.Reflections;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Location;
import co.clai.db.model.User;
import co.clai.remote.OAuth2Helper.OAuth2Data;

public abstract class AbstractRemoteConnection extends AbstractCachedQueryConnection {

	public static final String REMOTE_LOCATION_CONFIG_KEY_TYPE = "type";
	public static final String UNIQUE_ID_KEY = "uniqueId";
	protected static final String JSON_KEY_ALLOW_PW_LOGIN = "allowPwLogin";

	private static final Map<String, Class<? extends AbstractRemoteConnection>> allRemoteTypes = loadRemoteTypes();
	private static final Map<String, JSONObject> defaultConfig = loadDefaultTypes();

	private static Map<String, Class<? extends AbstractRemoteConnection>> loadRemoteTypes() {

		Map<String, Class<? extends AbstractRemoteConnection>> reMap = new HashMap<>();

		Reflections reflections = new Reflections("co.clai.remote");
		Set<Class<? extends AbstractRemoteConnection>> allClasses = reflections
				.getSubTypesOf(AbstractRemoteConnection.class);

		for (Class<? extends AbstractRemoteConnection> c : allClasses) {

			String name = null;
			try {
				Constructor<? extends AbstractRemoteConnection> cons = c.getConstructor(Location.class);
				AbstractRemoteConnection r = cons.newInstance(new Object[] { null });
				name = r.getRemoteTypeName();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			logger.log(Level.INFO, "loading remote Location " + name + ", " + c.getName());
			reMap.put(name, c);
		}

		return reMap;
	}

	private static Map<String, JSONObject> loadDefaultTypes() {

		Map<String, JSONObject> retMap = new HashMap<>();

		Reflections reflections = new Reflections("co.clai.remote");
		Set<Class<? extends AbstractRemoteConnection>> allClasses = reflections
				.getSubTypesOf(AbstractRemoteConnection.class);

		for (Class<? extends AbstractRemoteConnection> c : allClasses) {

			JSONObject defConfig;
			String name = null;
			try {
				Constructor<? extends AbstractRemoteConnection> cons = c.getConstructor(Location.class);
				AbstractRemoteConnection r = cons.newInstance(new Object[] { null });
				name = r.getRemoteTypeName();
				defConfig = r.getDefaultConfig();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			logger.log(Level.INFO, "loading Default Location config " + name + ", " + c.getName());
			retMap.put(name, defConfig);
		}

		return retMap;
	}

	public static AbstractRemoteConnection getRemoteFromLocation(Location l) {
		Class<? extends AbstractRemoteConnection> c = allRemoteTypes
				.get(l.getConfig().getString(REMOTE_LOCATION_CONFIG_KEY_TYPE));

		try {
			Constructor<? extends AbstractRemoteConnection> cons = c.getConstructor(Location.class);

			return cons.newInstance(new Object[] { l });
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error while creating RemoteConnection", e);
			return null;
		}
	}

	public static List<String> getAllTypes() {
		List<String> retList = new ArrayList<>();
		retList.addAll(allRemoteTypes.keySet());
		return retList;
	}

	public static String getDefaultConfig(String type) {
		return defaultConfig.get(type).toString();
	}

	protected abstract String getRemoteTypeName();

	public abstract RemoteUserData getUserDataByUserId(int userId);

	public abstract List<Integer> getUserIdsFromUserGroup(int userGroupId);

	public abstract String getUserFieldContentFromUserId(String fieldId, int userId);

	public abstract RemoteUserData getUserDataByUserName(String userName);

	public abstract RemoteUserData getUserDataByUserEmail(String userEmail);

	public abstract int loginUser(String username, String password);

	public abstract List<Integer> getUsergroupsFromUserId(int id);

	public abstract String getUsergroupNameById(int userGroupId);

	public abstract JSONObject getDefaultConfig();

	public abstract JSONObject getConfig();

	public abstract boolean canDoPasswordLogin();

	public abstract boolean canDoOAuth2Login();

	public abstract OAuth2Data getOAuth2Data();

	public abstract User getUserWithOAuth2Code(DatabaseConnector dbCon, Map<String, String[]> parameters,
			int locationId);
}
