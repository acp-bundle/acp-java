package co.clai.db.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;

public class UserGroupAccessFilter extends AbstractDbTable {

	public static String DB_TABLE_NAME = "user_group_access";
	public static String DB_TABLE_COLUMN_NAME_ID = "id";
	public static String DB_TABLE_COLUMN_NAME_LOCATION_ID = "location_id";
	public static String DB_TABLE_COLUMN_NAME_USER_GROUP_ID = "user_group_id";

	public static String DB_TABLE_COLUMN_NAME_PATH = "path";

	public static String DB_TABLE_COLUMN_NAME_COMMUNITY_ID = "community_id";
	public static String DB_TABLE_COLUMN_NAME_GAME_ID = "game_id";
	public static String DB_TABLE_COLUMN_NAME_ASSET_ID = "asset_id";

	private final int id;
	private final int locationId;
	private final int userGroupId;
	private final String path;
	private final int communityId;
	private final int gameId;
	private final int assetId;

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_LOCATION_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_USER_GROUP_ID, DbValueType.INTEGER);

		columnMap.put(DB_TABLE_COLUMN_NAME_PATH, DbValueType.STRING);

		columnMap.put(DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_GAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_ASSET_ID, DbValueType.INTEGER);
	}

	public static UserGroupAccessFilter dummyAccessFilter = new UserGroupAccessFilter();

	public UserGroupAccessFilter() {
		this(-1, -1, -1, null, -1, -1, -1);
	}

	private UserGroupAccessFilter(int id, int locationId, int userGroupId, String path, int communityId, int gameId,
			int assetId) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.locationId = locationId;
		this.userGroupId = userGroupId;
		this.path = path;
		this.communityId = communityId;
		this.gameId = gameId;
		this.assetId = assetId;
	}

	public static List<UserGroupAccessFilter> getFilterByLocationUserGroup(DatabaseConnector dbCon, int locationId,
			int userGroupId) {
		List<UserGroupAccessFilter> retList = new ArrayList<>();

		List<Map<String, DbValue>> result = dbCon.select(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_LOCATION_ID, DB_TABLE_COLUMN_NAME_USER_GROUP_ID),
				Arrays.asList(new DbValue(locationId), new DbValue(userGroupId)), dummyAccessFilter.getColumns());

		for (Map<String, DbValue> r : result) {
			retList.add(getFilterFromResult(r));
		}

		return retList;
	}

	private static UserGroupAccessFilter getFilterFromResult(Map<String, DbValue> r) {
		UserGroupAccessFilter f = new UserGroupAccessFilter(r.get(DB_TABLE_COLUMN_NAME_ID).getInt(),
				r.get(DB_TABLE_COLUMN_NAME_LOCATION_ID).getInt(), r.get(DB_TABLE_COLUMN_NAME_USER_GROUP_ID).getInt(),
				r.get(DB_TABLE_COLUMN_NAME_PATH).getString(), r.get(DB_TABLE_COLUMN_NAME_COMMUNITY_ID).getInt(),
				r.get(DB_TABLE_COLUMN_NAME_GAME_ID).getInt(), r.get(DB_TABLE_COLUMN_NAME_ASSET_ID).getInt());
		return f;
	}

	public static UserGroupAccessFilter getFilterById(DatabaseConnector dbCon, int filterId) {
		List<Map<String, DbValue>> result = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(filterId),
				dummyAccessFilter.getColumns());

		return getFilterFromResult(result.get(0));
	}

	public static void addNewGroupAccessFilter(DatabaseConnector dbCon, int locationId, int userGroupId, String path,
			int communityId, int gameId, int assetId) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_LOCATION_ID, DB_TABLE_COLUMN_NAME_USER_GROUP_ID,
						DB_TABLE_COLUMN_NAME_PATH, DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DB_TABLE_COLUMN_NAME_GAME_ID,
						DB_TABLE_COLUMN_NAME_ASSET_ID),
				Arrays.asList(new DbValue(locationId), new DbValue(userGroupId), new DbValue(path),
						new DbValue(communityId), new DbValue(gameId), new DbValue(assetId)));
	}

	public int getId() {
		return id;
	}

	public int getLocationId() {
		return locationId;
	}

	public int getUserGroupId() {
		return userGroupId;
	}

	public String getPath() {
		return path;
	}

	public int getCommunityId() {
		return communityId;
	}

	public int getGameId() {
		return gameId;
	}

	public int getAssetId() {
		return assetId;
	}

	public static void addNewUserGroupAccessFilter(DatabaseConnector dbCon, int locationId, int userGroupId,
			String path, int communityId, int gameId, int assetId) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_LOCATION_ID, DB_TABLE_COLUMN_NAME_USER_GROUP_ID,
						DB_TABLE_COLUMN_NAME_PATH, DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DB_TABLE_COLUMN_NAME_GAME_ID,
						DB_TABLE_COLUMN_NAME_ASSET_ID),
				Arrays.asList(new DbValue(locationId), new DbValue(userGroupId), new DbValue(path),
						new DbValue(communityId), new DbValue(gameId), new DbValue(assetId)));
	}

	public void changeFilter(DatabaseConnector dbCon, String newPath, int newCommunityId, int newGameId,
			int newAssetId) {

		dbCon.updateValue(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_PATH, DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						DB_TABLE_COLUMN_NAME_GAME_ID, DB_TABLE_COLUMN_NAME_ASSET_ID),
				Arrays.asList(new DbValue(newPath), new DbValue(newCommunityId), new DbValue(newGameId),
						new DbValue(newAssetId)),
				DB_TABLE_COLUMN_NAME_ID, new DbValue(getId()));
	}

	public void deleteFilter(DatabaseConnector dbCon) {
		dbCon.deleteFrom(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(getId()));
	}

	public static List<UserGroupAccessFilter> getAllFilter(DatabaseConnector dbCon) {

		List<UserGroupAccessFilter> retList = new ArrayList<>();

		List<Map<String, DbValue>> result = dbCon.select(DB_TABLE_NAME, dummyAccessFilter.getColumns());

		for (Map<String, DbValue> r : result) {
			retList.add(getFilterFromResult(r));
		}

		return retList;
	}
}
