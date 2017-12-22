package co.clai.db.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import co.clai.access.AccessibleHelper;
import co.clai.access.CommunityAsset;
import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;
import co.clai.util.IntStringPair;
import co.clai.util.ValueValuePair;

public class Community extends AbstractDbTable {

	public static final String DB_TABLE_NAME = "community";
	public static final String DB_TABLE_COLUMN_NAME_ID = "id";
	public static final String DB_TABLE_COLUMN_NAME_KEY = "_key";
	public static final String DB_TABLE_COLUMN_NAME_NAME = "longname";
	public static final String DB_TABLE_COLUMN_NAME_FEATURES = "features";
	public static final String DB_TABLE_COLUMN_NAME_SETTINGS = "settings";

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_KEY, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_NAME, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_FEATURES, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_SETTINGS, DbValueType.STRING);
	}

	public static final Community dummyCommunity = new Community();

	private final int id;
	private final String key;
	private final String name;
	private final JSONObject features;
	private final JSONObject settings;

	public Community() {
		this(-1, null, null, null, null);
	}

	private Community(int id, String key, String name, String features, String settings) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.name = name;
		this.key = key;
		this.features = (features == null) ? null : new JSONObject(features);
		this.settings = (settings == null) ? null : new JSONObject(settings);
	}

	public static Community getCommunityById(DatabaseConnector dbCon, int communityId) {

		if (communityId < 0) {
			return null;
		}

		if (communityId == 0) {
			return new Community(0, "local", "Local", null, null);
		}

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID,
				new DbValue(communityId), dummyCommunity.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getCommunityFromResult(results.get(0));
	}

	private static Community getCommunityFromResult(Map<String, DbValue> result) {
		Community c = new Community(result.get(DB_TABLE_COLUMN_NAME_ID).getInt(),
				result.get(DB_TABLE_COLUMN_NAME_KEY).getString(), result.get(DB_TABLE_COLUMN_NAME_NAME).getString(),
				result.get(DB_TABLE_COLUMN_NAME_FEATURES).getString(),
				result.get(DB_TABLE_COLUMN_NAME_SETTINGS).getString());
		return c;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public JSONObject getFeatures() {
		return features;
	}

	public JSONObject getSettings() {
		return settings;
	}

	public String getKey() {
		return key;
	}

	public static void addNewCommunity(DatabaseConnector dbCon, String key, String name) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_NAME, DB_TABLE_COLUMN_NAME_KEY, DB_TABLE_COLUMN_NAME_FEATURES,
						DB_TABLE_COLUMN_NAME_SETTINGS),
				Arrays.asList(new DbValue(name), new DbValue(key), new DbValue("{}"), new DbValue("{}")));
	}

	public static List<Community> getAllCommunity(DatabaseConnector dbCon) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, dummyCommunity.getColumns());

		List<Community> retList = new ArrayList<>();

		for (Map<String, DbValue> r : results) {
			retList.add(getCommunityFromResult(r));
		}

		return retList;
	}

	public void setFeatures(DatabaseConnector dbCon, String features) {
		dbCon.updateValue(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_FEATURES),
				Arrays.asList(new DbValue(features)), DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public void setSettings(DatabaseConnector dbCon, String settings) {
		dbCon.updateValue(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_SETTINGS),
				Arrays.asList(new DbValue(settings)), DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public static Community getCommunityByName(DatabaseConnector dbCon, String communityName) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_NAME,
				new DbValue(communityName), dummyCommunity.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getCommunityFromResult(results.get(0));
	}

	public static List<ValueValuePair> getCommunityListSelection(DatabaseConnector dbCon, User thisUser,
			AccessibleHelper accessibleHelper) {

		List<ValueValuePair> retList = new ArrayList<>();

		for (Community c : getAllCommunity(dbCon)) {
			if (thisUser.hasAccess(accessibleHelper, c.getAsset())) {
				retList.add(new IntStringPair(c.getId(), c.getName()));
			}
		}

		return retList;
	}

	public CommunityAsset getAsset() {
		return new CommunityAsset(id);
	}

}
