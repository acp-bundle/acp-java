package co.clai.db.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import co.clai.access.AssetServer;
import co.clai.access.CommunityAsset;
import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;

public class StorageIndex extends AbstractDbTable {

	public static final String DB_TABLE_NAME = "storage_index";
	public static final String DB_TABLE_COLUMN_NAME_ID = "id";
	public static final String DB_TABLE_COLUMN_NAME_IDENTIFIER = "identifier";
	public static final String DB_TABLE_COLUMN_NAME_NAME = "name";
	public static final String DB_TABLE_COLUMN_NAME_STORAGE_ID = "storage_id";
	public static final String DB_TABLE_COLUMN_NAME_DATETIME = "datetime";
	public static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_IDENTIFIER, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_NAME, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_STORAGE_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_DATETIME, DbValueType.STRING);
	}

	public static final StorageIndex dummyStorageIndex = new StorageIndex();

	private final int id;
	private final String identifier;
	private final String name;
	private final int storageId;
	private final Date datetime;

	public StorageIndex() {
		this(-1, null, null, -1, null);
	}

	private StorageIndex(int id, String identifier, String name, int storageId, String datetime) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.identifier = identifier;
		this.name = name;
		this.storageId = storageId;

		Date tmpDateTime = null;
		try {
			tmpDateTime = StorageIndex.DEFAULT_TIME_FORMAT.parse(datetime);
		} catch (Exception e) {
			logger.log(Level.WARNING, "error while initializing datetime for index " + id + ": " + e.getMessage());
		}
		this.datetime = tmpDateTime;
	}

	public int getId() {
		return id;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getName() {
		return name;
	}

	public int getStorageId() {
		return storageId;
	}

	public Date getDatetime() {
		return datetime;
	}

	public static StorageIndex getStorageById(DatabaseConnector dbCon, int storageId) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID,
				new DbValue(storageId), dummyStorageIndex.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getStorageIndexFromResult(results.get(0));
	}

	private static StorageIndex getStorageIndexFromResult(Map<String, DbValue> parameters) {
		return new StorageIndex(parameters.get(DB_TABLE_COLUMN_NAME_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_IDENTIFIER).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_NAME).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_STORAGE_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_DATETIME).getString());
	}

	public static List<StorageIndex> getAllStorageIndex(DatabaseConnector dbCon) {

		List<StorageIndex> retList = new ArrayList<>();

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, dummyStorageIndex.getColumns());

		for (Map<String, DbValue> r : results) {
			retList.add(getStorageIndexFromResult(r));
		}

		return retList;
	}

	public static List<StorageIndex> getAllStorageIndexByStorageId(DatabaseConnector dbCon, int storageId) {

		List<StorageIndex> retList = new ArrayList<>();

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_STORAGE_ID,
				new DbValue(storageId), dummyStorageIndex.getColumns());

		for (Map<String, DbValue> r : results) {
			retList.add(getStorageIndexFromResult(r));
		}

		return retList;
	}

	public static void addNewStorage(DatabaseConnector dbCon, String name, String identifier, int storageId,
			long timestamp) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_NAME, DB_TABLE_COLUMN_NAME_IDENTIFIER,
						DB_TABLE_COLUMN_NAME_STORAGE_ID, DB_TABLE_COLUMN_NAME_DATETIME),
				Arrays.asList(new DbValue(name), new DbValue(identifier), new DbValue(storageId),
						new DbValue(DEFAULT_TIME_FORMAT.format(new Date(timestamp)))));
	}

	public void delete(DatabaseConnector dbCon) {
		dbCon.deleteFrom(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public CommunityAsset getAsset(DatabaseConnector dbCon) {
		Storage stor = Storage.getStorageById(dbCon, storageId);
		Server serv = stor.getServer(dbCon);
		return new AssetServer(id, serv.getCommunityId(), serv.getGameId());
	}
}