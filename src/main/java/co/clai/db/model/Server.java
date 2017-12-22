package co.clai.db.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.clai.access.AssetServer;
import co.clai.access.CommunityAsset;
import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;

public class Server extends AbstractDbTable {

	public static final String DB_TABLE_NAME = "server";
	public static final String DB_TABLE_COLUMN_NAME_ID = "id";
	public static final String DB_TABLE_COLUMN_NAME_KEY = "_key";
	public static final String DB_TABLE_COLUMN_NAME_NAME = "name";
	public static final String DB_TABLE_COLUMN_NAME_GAME_ID = "game_id";
	public static final String DB_TABLE_COLUMN_NAME_COMMUNITY_ID = "community_id";
	public static final String DB_TABLE_COLUMN_NAME_BANLIST_IDS = "banlist_ids";

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_KEY, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_NAME, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_GAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_BANLIST_IDS, DbValueType.STRING);
	}

	public static final Server dummyServer = new Server();

	private final int id;
	private final String key;
	private final String name;
	private final int gameId;
	private final int communityId;
	private final List<Integer> banlistIds = new ArrayList<>();

	public Server() {
		this(-1, null, null, -1, -1, null);
	}

	private Server(int id, String key, String name, int gameId, int communityId, String banlistIds) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.name = name;
		this.key = key;
		this.gameId = gameId;
		this.communityId = communityId;

		if (banlistIds != null) {
			for (String i : banlistIds.split(",")) {
				if (!"".equals(i)) {
					this.banlistIds.add(new Integer(i));
				}
			}
		}
	}

	public int getId() {
		return id;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public int getGameId() {
		return gameId;
	}

	public int getCommunityId() {
		return communityId;
	}

	public List<Integer> getBanlistIds() {
		return banlistIds;
	}

	public static Server getServerById(DatabaseConnector dbCon, int serverId) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(serverId),
				dummyServer.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getServerFromResult(results.get(0));
	}

	private static Server getServerFromResult(Map<String, DbValue> parameters) {
		return new Server(parameters.get(DB_TABLE_COLUMN_NAME_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_KEY).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_NAME).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_GAME_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_COMMUNITY_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_BANLIST_IDS).getString());
	}

	public CommunityAsset getAsset() {
		return new AssetServer(id, communityId, gameId);
	}

	public static Server getServerByKey(DatabaseConnector dbCon, String key) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_KEY, new DbValue(key),
				dummyServer.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getServerFromResult(results.get(0));
	}

	public static List<Server> getAllServer(DatabaseConnector dbCon) {

		List<Server> retList = new ArrayList<>();

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, dummyServer.getColumns());

		for (Map<String, DbValue> r : results) {
			retList.add(getServerFromResult(r));
		}

		return retList;
	}

	public static void addNewServer(DatabaseConnector dbCon, String key, String newName, int newGameId,
			int newCommunityId) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_KEY, DB_TABLE_COLUMN_NAME_NAME, DB_TABLE_COLUMN_NAME_GAME_ID,
						DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DB_TABLE_COLUMN_NAME_BANLIST_IDS),
				Arrays.asList(new DbValue(key), new DbValue(newName), new DbValue(newGameId),
						new DbValue(newCommunityId), new DbValue("")));
	}

	public void edit(DatabaseConnector dbCon, String newKey, String newName, int newGameId, int newCommunityId,
			String newBanlistIds) {
		dbCon.updateValue(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_KEY, DB_TABLE_COLUMN_NAME_NAME, DB_TABLE_COLUMN_NAME_GAME_ID,
						DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DB_TABLE_COLUMN_NAME_BANLIST_IDS),
				Arrays.asList(new DbValue(newKey), new DbValue(newName), new DbValue(newGameId),
						new DbValue(newCommunityId), new DbValue(newBanlistIds)),
				DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public void delete(DatabaseConnector dbCon) {
		dbCon.deleteFrom(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

}
