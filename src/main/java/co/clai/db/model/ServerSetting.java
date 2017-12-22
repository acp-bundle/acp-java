package co.clai.db.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;

public class ServerSetting extends AbstractDbTable {

	public static final String DB_TABLE_NAME = "server_setting";
	public static final String DB_TABLE_COLUMN_NAME_ID = "id";
	public static final String DB_TABLE_COLUMN_NAME_SERVER_ID = "server_id";
	public static final String DB_TABLE_COLUMN_NAME_SETTING_KEY = "setting_key";
	public static final String DB_TABLE_COLUMN_NAME_DATA = "data";

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_SERVER_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_SETTING_KEY, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_DATA, DbValueType.BLOB);
	}

	public static final ServerSetting dummyServerSetting = new ServerSetting();

	private final int id;
	private final int serverId;
	private final String settingKey;
	private final byte[] data;

	public ServerSetting() {
		this(-1, -1, null, null);
	}

	private ServerSetting(int id, int serverId, String settingKey, byte[] data) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.serverId = serverId;
		this.settingKey = settingKey;
		this.data = data;
	}

	public int getId() {
		return id;
	}

	public int getServerId() {
		return serverId;
	}

	public String getSettingKey() {
		return settingKey;
	}

	public byte[] getData() {
		return data;
	}

	public String getDataAsString() {
		return new String(data);
	}

	public static ServerSetting getServerSettingById(DatabaseConnector dbCon, int serverSettingId) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID,
				new DbValue(serverSettingId), dummyServerSetting.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getServerSettingFromResult(results.get(0));
	}

	private static ServerSetting getServerSettingFromResult(Map<String, DbValue> parameters) {
		return new ServerSetting(parameters.get(DB_TABLE_COLUMN_NAME_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_SERVER_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_SETTING_KEY).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_DATA).getBlobAsByteArr());
	}

	public static List<ServerSetting> getAllServerSettingByServerId(DatabaseConnector dbCon, int serverId) {

		List<ServerSetting> retList = new ArrayList<>();

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_SERVER_ID,
				new DbValue(serverId), dummyServerSetting.getColumns());

		for (Map<String, DbValue> r : results) {
			retList.add(getServerSettingFromResult(r));
		}

		return retList;
	}

	public static void addNewServerSetting(DatabaseConnector dbCon, int serverId, String settingKey, String data) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_SERVER_ID, DB_TABLE_COLUMN_NAME_SETTING_KEY,
						DB_TABLE_COLUMN_NAME_DATA),
				Arrays.asList(new DbValue(serverId), new DbValue(settingKey), DbValue.newBlob(data)));
	}

	public void edit(DatabaseConnector dbCon, String newData) {
		dbCon.updateValue(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_DATA),
				Arrays.asList(DbValue.newBlob(newData)), DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public void delete(DatabaseConnector dbCon) {
		dbCon.deleteFrom(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public static ServerSetting getServerSettingByServerIdAndKey(DatabaseConnector dbCon, int serverId,
			String configKey) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_SERVER_ID, DB_TABLE_COLUMN_NAME_SETTING_KEY),
				Arrays.asList(new DbValue(serverId), new DbValue(configKey)), dummyServerSetting.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getServerSettingFromResult(results.get(0));
	}

}