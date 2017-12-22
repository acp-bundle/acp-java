package co.clai.db.model;

import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;
import co.clai.module.ModuleUtil;
import co.clai.remote.AbstractRemoteConnection;
import co.clai.remote.RemoteUserData;
import co.clai.util.log.LoggingUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;

import co.clai.access.AccessFilter;
import co.clai.access.AccessibleHelper;
import co.clai.access.AccessibleModuleHelper;
import co.clai.access.CommunityAsset;

public class User extends AbstractDbTable {

	public static final String DB_TABLE_NAME = "user";
	public static final String DB_TABLE_COLUMN_NAME_ID = "id";
	public static final String DB_TABLE_COLUMN_NAME_USERNAME = "username";
	public static final String DB_TABLE_COLUMN_NAME_PASSWORD = "password";
	public static final String DB_TABLE_COLUMN_NAME_COMMUNITY_ID = "community_id";
	public static final String DB_TABLE_COLUMN_NAME_IS_ROOT = "is_root";

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_USERNAME, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_PASSWORD, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_IS_ROOT, DbValueType.INTEGER);
	}

	private final List<AccessFilter> accessFilter = new ArrayList<>();

	private static final List<AccessFilter> defaultAccessFilter = new ArrayList<>();
	{
		defaultAccessFilter.add(new AccessFilter("settings"));
	}

	private final boolean isRoot;

	private final String username;
	private final String encryptedPassword;

	private final int locationId;
	private final int id;
	private final int communityId;

	private final List<Integer> remoteGroupIds = new ArrayList<>();

	public static User dummyUser = new User();

	public User() {
		this(null, null, -1, -1, -1, null, false);
	}

	public User(DatabaseConnector dbCon, String username, int id, int locationId, int communityId,
			String encryptedPassword, boolean isRoot) {

		super(DB_TABLE_NAME, columnMap);

		this.isRoot = isRoot;

		this.username = username;
		this.encryptedPassword = encryptedPassword;

		this.locationId = locationId;
		this.id = id;
		this.communityId = communityId;

		if (locationId > 0) {
			List<Integer> tempL = Location.getLocationById(dbCon, locationId).getUsergroupsByUserId(id);
			if (tempL != null) {
				remoteGroupIds.addAll(tempL);
			} else {
				logger.log(Level.WARNING, "Location returned null for user group list");
			}
		}

		if (dbCon != null) {
			List<UserAccessFilter> af = UserAccessFilter.getFilterByLocationUser(dbCon, locationId, id);
			for (UserAccessFilter f : af) {
				accessFilter.add(new AccessFilter(f.getPath(), f.getAssetId(), f.getCommunityId(), f.getGameId()));
			}

			for (Integer i : remoteGroupIds) {
				List<UserGroupAccessFilter> gf = UserGroupAccessFilter.getFilterByLocationUserGroup(dbCon, locationId,
						i.intValue());
				for (UserGroupAccessFilter f : gf) {
					accessFilter.add(new AccessFilter(f.getPath(), f.getAssetId(), f.getCommunityId(), f.getGameId()));
				}
			}
		}

		for (AccessFilter f : defaultAccessFilter) {
			accessFilter.add(f);
		}
	}

	public static User login(DatabaseConnector dbCon, int location, String username, String password) {

		if (location == 0) {

			List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_USERNAME,
					new DbValue(username), dummyUser.getColumns());

			if (results.isEmpty()) {
				return null;
			}

			Map<String, DbValue> result = results.get(0);

			if (BCrypt.checkpw(password, result.get(DB_TABLE_COLUMN_NAME_PASSWORD).getString())) {

				User u = getUserFromDbResult(dbCon, result);

				return u;
			}

		} else {

			Location locationById = Location.getLocationById(dbCon, location);
			AbstractRemoteConnection remoteByConfig = AbstractRemoteConnection.getRemoteFromLocation(locationById);

			int userId = remoteByConfig.loginUser(username, password);

			if (userId <= 0) {
				return null;
			}

			RemoteUserData userdata = remoteByConfig.getUserDataByUserId(userId);

			return new User(dbCon, userdata.getUsername(), userdata.getId(), location, locationById.getCommunityId(),
					null, false);
		}

		return null;
	}

	private static User getUserFromDbResult(DatabaseConnector dbCon, Map<String, DbValue> result) {
		String username = result.get(DB_TABLE_COLUMN_NAME_USERNAME).getString();
		int id = result.get(DB_TABLE_COLUMN_NAME_ID).getInt();
		int communityId = result.get(DB_TABLE_COLUMN_NAME_COMMUNITY_ID).getInt();
		String encryptedPassword = result.get(DB_TABLE_COLUMN_NAME_PASSWORD).getString();
		boolean isRoot = !(result.get(DB_TABLE_COLUMN_NAME_IS_ROOT).getInt() == 0);

		// User coming from the database is always a local user
		return new User(dbCon, username, id, 0, communityId, encryptedPassword, isRoot);
	}

	public static User getUserByLocationId(DatabaseConnector dbCon, int location, int userId) {

		final Logger logger = LoggingUtil.getDefaultLogger();

		if (location == 0) {

			List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID,
					new DbValue(userId), dummyUser.getColumns());

			if (results.isEmpty()) {
				logger.log(Level.WARNING, "Cannot find local User with id " + userId);
				return null;
			}

			Map<String, DbValue> result = results.get(0);

			User u = getUserFromDbResult(dbCon, result);

			return u;
		}

		Location locationById = Location.getLocationById(dbCon, location);

		if (locationById == null) {
			logger.log(Level.WARNING, "Cannot find Location with id " + location);
			return null;
		}

		RemoteUserData userdata = AbstractRemoteConnection.getRemoteFromLocation(locationById)
				.getUserDataByUserId(userId);

		return new User(dbCon, userdata.getUsername(), userdata.getId(), location, locationById.getCommunityId(), null,
				false);
	}

	public boolean hasAccess(AccessibleHelper a) {
		if (isRoot) {
			return true;
		}

		for (AccessFilter f : accessFilter) {
			if (a.hasAccess(f)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasAccess(AccessibleHelper a, CommunityAsset as) {
		if (isRoot) {
			return true;
		}

		for (AccessFilter f : accessFilter) {
			if (a.hasAccess(f, as)) {
				return true;
			}
		}

		return false;
	}

	public boolean getIsRoot() {
		return isRoot;
	}

	public String getUsername() {
		return username;
	}

	public String getEncryptedPassword() {
		return encryptedPassword;
	}

	public int getLocationId() {
		return locationId;
	}

	public int getId() {
		return id;
	}

	public int getCommunityId() {
		return communityId;
	}

	public List<String> getAccessibleModules() {

		Set<String> modules = ModuleUtil.getModules();

		List<String> retModules = new ArrayList<>();

		for (String m : modules) {
			if (this.hasAccess(new AccessibleModuleHelper(m))) {
				retModules.add(m);
			}
		}

		return new ArrayList<>();
	}

	public static void addNewLocalUser(DatabaseConnector dbCon, String username, String password, int community_id,
			boolean isRoot) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_USERNAME, DB_TABLE_COLUMN_NAME_PASSWORD,
						DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DB_TABLE_COLUMN_NAME_IS_ROOT),
				Arrays.asList(new DbValue(username), new DbValue(BCrypt.hashpw(password, BCrypt.gensalt())),
						new DbValue(community_id), new DbValue((isRoot == true) ? 1 : 0)));
	}

	public void setNewPassword(DatabaseConnector dbCon, String newPassword) {
		if (getLocationId() != 0) {
			throw new RuntimeException("remote user cannot change password!");
		}

		dbCon.updateValue(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_PASSWORD),
				Arrays.asList(new DbValue(BCrypt.hashpw(newPassword, BCrypt.gensalt()))), DB_TABLE_COLUMN_NAME_ID,
				new DbValue(getId()));
	}

	public void setNewUsername(DatabaseConnector dbCon, String newUsername) {
		if (getLocationId() != 0) {
			throw new RuntimeException("remote user cannot change Username!");
		}

		dbCon.updateValue(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_USERNAME),
				Arrays.asList(new DbValue(newUsername)), DB_TABLE_COLUMN_NAME_ID, new DbValue(getId()));
	}

	public void setNewCommunityId(DatabaseConnector dbCon, int newCommId) {
		if (getLocationId() != 0) {
			throw new RuntimeException("remote user cannot change community!");
		}

		dbCon.updateValue(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_COMMUNITY_ID),
				Arrays.asList(new DbValue(newCommId)), DB_TABLE_COLUMN_NAME_ID, new DbValue(getId()));
	}

	public static User getUserByLocationName(DatabaseConnector dbCon, int location, String username) {

		if (location == 0) {

			List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_USERNAME,
					new DbValue(username), dummyUser.getColumns());

			if (results.isEmpty()) {
				return null;
			}

			Map<String, DbValue> result = results.get(0);

			User u = getUserFromDbResult(dbCon, result);

			return u;
		}

		Location locationById = Location.getLocationById(dbCon, location);
		RemoteUserData userdata = AbstractRemoteConnection.getRemoteFromLocation(locationById)
				.getUserDataByUserName(username);

		return new User(dbCon, userdata.getUsername(), userdata.getId(), location, locationById.getCommunityId(), null,
				false);
	}

	public static List<User> getAllLocalUser(DatabaseConnector dbCon) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, dummyUser.getColumns());

		List<User> retList = new ArrayList<>();

		for (Map<String, DbValue> r : results) {
			retList.add(getUserFromDbResult(dbCon, r));
		}

		return retList;
	}

	public void deleteUser(DatabaseConnector dbCon) {
		dbCon.deleteFrom(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(this.getId()));

		UserAccessFilter.deleteByUserLocationId(dbCon, locationId, id);
	}

	public List<Integer> getUserGroups() {
		return remoteGroupIds;
	}

}
