package co.clai.db.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;
import co.clai.remote.AbstractRemoteConnection;
import co.clai.util.log.LoggingUtil;

public class Location extends AbstractDbTable {

	public static String DB_TABLE_NAME = "location";
	public static String DB_TABLE_COLUMN_NAME_ID = "id";
	public static String DB_TABLE_COLUMN_NAME_NAME = "name";
	public static String DB_TABLE_COLUMN_NAME_COMMUNITY_ID = "community_id";
	public static String DB_TABLE_COLUMN_NAME_CONFIG = "config";

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_NAME, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_CONFIG, DbValueType.BLOB);
	}

	private final int id;
	private final String name;
	private final int communityId;
	private final JSONObject config;

	private final AbstractRemoteConnection remoteLocation;

	public static Location dummyLocation = new Location();

	public Location() {
		this(-1, null, -1, null);
	}

	private Location(int id, String name, int communityId, String config) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.name = name;
		this.communityId = communityId;
		if (config != null) {
			this.config = new JSONObject(config);
		} else {
			this.config = null;
		}

		AbstractRemoteConnection tmpRemoteLocation = null;
		try {
			if (getConfig() != null) {
				tmpRemoteLocation = AbstractRemoteConnection.getRemoteFromLocation(this);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error while creating Location with id " + id + ": " + e.getMessage());
		}

		remoteLocation = tmpRemoteLocation;
	}

	public static Location getLocationById(DatabaseConnector dbCon, int id) {

		final Logger logger = LoggingUtil.getDefaultLogger();

		if (id == 0) {
			return new Location(0, "Local", 0, null);
		}

		if (dbCon == null) {
			logger.log(Level.WARNING, "dbCon is null when trying to get Location");
			return null;
		}

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(id),
				dummyLocation.getColumns());

		if (results.isEmpty()) {
			logger.log(Level.WARNING, "Cannot find Location with id " + id);
			return null;
		}

		return getLocationFromResult(results.get(0));
	}

	public static Location getLocationByName(DatabaseConnector dbCon, String name) {

		if (dbCon == null) {
			return null;
		}

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_NAME, new DbValue(name),
				dummyLocation.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getLocationFromResult(results.get(0));
	}

	private static Location getLocationFromResult(Map<String, DbValue> r) {
		int rId = r.get(DB_TABLE_COLUMN_NAME_ID).getInt();
		String rName = r.get(DB_TABLE_COLUMN_NAME_NAME).getString();
		int rCommunityId = r.get(DB_TABLE_COLUMN_NAME_COMMUNITY_ID).getInt();
		String rConfig = r.get(DB_TABLE_COLUMN_NAME_CONFIG).getBlobAsString();

		Location l = new Location(rId, rName, rCommunityId, rConfig);
		return l;
	}

	public List<Integer> getUsergroupsByUserId(int userId) {
		return remoteLocation.getUsergroupsFromUserId(userId);
	}

	public String getUserGroupNameById(int userGroupId) {
		return remoteLocation.getUsergroupNameById(userGroupId);
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getCommunityId() {
		return communityId;
	}

	public JSONObject getConfig() {
		return config;
	}

	public static void addNewLocation(DatabaseConnector dbCon, String name, int communityId, String config) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_NAME, DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						DB_TABLE_COLUMN_NAME_CONFIG),
				Arrays.asList(new DbValue(name), new DbValue(communityId), DbValue.newBlob(config)));
	}

	public static List<Location> getAllLocations(DatabaseConnector dbCon) {

		Logger logger = LoggingUtil.getDefaultLogger();

		List<Map<String, DbValue>> rs = dbCon.select(DB_TABLE_NAME, dummyLocation.getColumns());

		List<Location> retList = new ArrayList<>();

		for (Map<String, DbValue> r : rs) {
			try {
				retList.add(getLocationFromResult(r));
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Error while creating Location with id "
						+ r.get(DB_TABLE_COLUMN_NAME_ID).getInt() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}

		return retList;
	}

	public void changeConfig(DatabaseConnector dbCon, String newConfig) {

		try {
			@SuppressWarnings("unused") // user for catching json ERRORS
			JSONObject jO = new JSONObject(newConfig);
		} catch (Exception e) {
			throw new RuntimeException("Malformed JSON Data given: " + e.getMessage());
		}

		dbCon.updateValue(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_CONFIG),
				Arrays.asList(DbValue.newBlob(newConfig)), DB_TABLE_COLUMN_NAME_ID, new DbValue(getId()));
	}

	public void delete(DatabaseConnector dbCon) {
		dbCon.deleteFrom(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

}
