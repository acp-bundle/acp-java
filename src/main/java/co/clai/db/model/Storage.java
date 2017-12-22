package co.clai.db.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.json.JSONObject;

import co.clai.access.AssetServer;
import co.clai.access.CommunityAsset;
import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;
import co.clai.storage.StorageType;

public class Storage extends AbstractDbTable {

	public static final String DB_TABLE_NAME = "storage";
	public static final String DB_TABLE_COLUMN_NAME_ID = "id";
	public static final String DB_TABLE_COLUMN_NAME_KEY = "_key";
	public static final String DB_TABLE_COLUMN_NAME_NAME = "name";
	public static final String DB_TABLE_COLUMN_NAME_SERVER_ID = "server_id";
	public static final String DB_TABLE_COLUMN_NAME_TYPE = "type";
	public static final String DB_TABLE_COLUMN_NAME_CONFIG = "config";
	public static final String DB_TABLE_COLUMN_NAME_HAS_LOCAL_INDEX = "has_local_index";

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_KEY, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_NAME, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_SERVER_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_TYPE, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_CONFIG, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_HAS_LOCAL_INDEX, DbValueType.INTEGER);
	}

	public static final Storage dummyStorage = new Storage();

	private final int id;
	private final String key;
	private final String name;
	private final int serverId;
	private final StorageType type;
	private final JSONObject config;
	private final boolean hasLocalIndex;

	public Storage() {
		this(-1, null, null, -1, null, null, false);
	}

	public Storage(int id, String key, String name, int serverId, String type, String data, boolean hasLocalIndex) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.key = key;
		this.name = name;
		this.serverId = serverId;
		if (type == null) {
			this.type = null;
		} else {
			this.type = StorageType.valueOf(type.toUpperCase());
		}
		if (data == null) {
			this.config = null;
		} else {
			JSONObject tmpData = null;
			try {
				tmpData = new JSONObject(data);
			} catch (Exception e) {
				logger.log(Level.WARNING,
						"Error while parsing data from Storage with id " + id + ": " + e.getMessage());
			}
			this.config = tmpData;
		}

		this.hasLocalIndex = hasLocalIndex;
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

	public int getServerId() {
		return serverId;
	}

	public StorageType getType() {
		return type;
	}

	public JSONObject getConfig() {
		return config;
	}

	public boolean isHasLocalIndex() {
		return hasLocalIndex;
	}

	public static Storage getStorageById(DatabaseConnector dbCon, int storageId) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID,
				new DbValue(storageId), dummyStorage.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getStorageFromResult(results.get(0));
	}

	private static Storage getStorageFromResult(Map<String, DbValue> parameters) {
		return new Storage(parameters.get(DB_TABLE_COLUMN_NAME_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_KEY).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_NAME).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_SERVER_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_TYPE).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_CONFIG).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_HAS_LOCAL_INDEX).getIntegerAsBool());
	}

	public static List<Storage> getAllStorage(DatabaseConnector dbCon) {

		List<Storage> retList = new ArrayList<>();

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, dummyStorage.getColumns());

		for (Map<String, DbValue> r : results) {
			retList.add(getStorageFromResult(r));
		}

		return retList;
	}

	public static List<Storage> getAllStorageByServerId(DatabaseConnector dbCon, int serverId) {

		List<Storage> retList = new ArrayList<>();

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_SERVER_ID,
				new DbValue(serverId), dummyStorage.getColumns());

		for (Map<String, DbValue> r : results) {
			retList.add(getStorageFromResult(r));
		}

		return retList;
	}

	public static void addNewStorage(DatabaseConnector dbCon, String key, String name, int serverId, StorageType type,
			JSONObject config) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_KEY, DB_TABLE_COLUMN_NAME_NAME, DB_TABLE_COLUMN_NAME_SERVER_ID,
						DB_TABLE_COLUMN_NAME_TYPE, DB_TABLE_COLUMN_NAME_CONFIG, DB_TABLE_COLUMN_NAME_HAS_LOCAL_INDEX),
				Arrays.asList(new DbValue(key), new DbValue(name), new DbValue(serverId),
						new DbValue(type.name().toLowerCase()), new DbValue(config.toString()), new DbValue(0)));
	}

	public void delete(DatabaseConnector dbCon) {
		dbCon.deleteFrom(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public CommunityAsset getAsset(DatabaseConnector dbCon) {
		Server serv = Server.getServerById(dbCon, serverId);
		return new AssetServer(id, serv.getCommunityId(), serv.getGameId());
	}

	public Server getServer(DatabaseConnector dbCon) {
		return Server.getServerById(dbCon, serverId);
	}

	public void edit(DatabaseConnector dbCon, String newKey, String newName, int newServerId,
			boolean newHasLocalIndex) {
		dbCon.updateValue(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_KEY, DB_TABLE_COLUMN_NAME_NAME, DB_TABLE_COLUMN_NAME_SERVER_ID,
						DB_TABLE_COLUMN_NAME_HAS_LOCAL_INDEX),
				Arrays.asList(new DbValue(newKey), new DbValue(newName), new DbValue(newServerId),
						DbValue.newBooleanAsInteger(newHasLocalIndex)),
				DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public void changeConfig(DatabaseConnector dbCon, String newConfig) {
		JSONObject jConf = new JSONObject(newConfig);

		dbCon.updateValue(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_CONFIG),
				Arrays.asList(new DbValue(jConf.toString())), DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public static Storage getStorageByKey(DatabaseConnector dbCon, String key) {
		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_KEY, new DbValue(key),
				dummyStorage.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getStorageFromResult(results.get(0));
	}
}