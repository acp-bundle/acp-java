package co.clai.db.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import co.clai.access.CommunityAsset;
import co.clai.access.GeneralAsset;
import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;
import co.clai.util.StringStringPair;

public class Banlist extends AbstractDbTable {

	private static final String INFO_COLUMNS_JSON_KEY_NAME = "name";
	private static final String INFO_COLUMNS_JSON_KEY_KEY = "key";

	public static final String DB_TABLE_NAME = "banlist";
	public static final String DB_TABLE_COLUMN_NAME_ID = "id";
	public static final String DB_TABLE_COLUMN_NAME_KEY = "_key";
	public static final String DB_TABLE_COLUMN_NAME_NAME = "name";
	public static final String DB_TABLE_COLUMN_NAME_GAME_ID = "game_id";
	public static final String DB_TABLE_COLUMN_NAME_COMMUNITY_ID = "community_id";
	public static final String DB_TABLE_COLUMN_NAME_INFO_COLUMNS = "info_columns";

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_KEY, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_NAME, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_GAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_INFO_COLUMNS, DbValueType.STRING);
	}

	public static final Banlist dummyBanlist = new Banlist();

	private final int id;
	private final String key;
	private final String name;
	private final int gameId;
	private final int communityId;
	private final List<StringStringPair> infoColumns = new ArrayList<>();
	private final Set<String> infoColumnLinks = new HashSet<>();

	private final String infoColumnsRaw;

	public Banlist() {
		this(-1, null, null, -1, -1, null);
	}

	private Banlist(int id, String key, String name, int gameId, int communityId, String infoColumns) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.name = name;
		this.key = key;
		this.gameId = gameId;
		this.communityId = communityId;
		this.infoColumnsRaw = infoColumns;

		try {
			if (infoColumns != null) {
				JSONArray jA = new JSONArray(infoColumns);
				for (int i = 0; i < jA.length(); i++) {
					JSONObject thisElement = jA.getJSONObject(i);
					this.infoColumns.add(new StringStringPair(thisElement.getString(INFO_COLUMNS_JSON_KEY_KEY),
							thisElement.getString(INFO_COLUMNS_JSON_KEY_NAME)));
					if (thisElement.has("type") && thisElement.getString("type").equals("link")) {
						infoColumnLinks.add(thisElement.getString(INFO_COLUMNS_JSON_KEY_KEY));
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE,
					"error while creating extra columns for BanlistMetadata with id " + id + ": " + e.getMessage(), e);
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

	public List<StringStringPair> getInfoColumns() {
		return infoColumns;
	}

	public Set<String> getInfoColumnLinks() {
		return infoColumnLinks;
	}

	public static void addNewBanlist(DatabaseConnector dbCon, String key, String name, int gameId, int communityId) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_KEY, DB_TABLE_COLUMN_NAME_NAME, DB_TABLE_COLUMN_NAME_GAME_ID,
						DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DB_TABLE_COLUMN_NAME_INFO_COLUMNS),
				Arrays.asList(new DbValue(key), new DbValue(name), new DbValue(gameId), new DbValue(communityId),
						new DbValue("[]")));
	}

	public static Banlist getBanlistById(DatabaseConnector dbCon, int banId) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(banId),
				dummyBanlist.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getBanlistFromResult(results.get(0));
	}

	private static Banlist getBanlistFromResult(Map<String, DbValue> parameters) {
		return new Banlist(parameters.get(DB_TABLE_COLUMN_NAME_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_KEY).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_NAME).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_GAME_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_COMMUNITY_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_INFO_COLUMNS).getString());
	}

	public static List<Banlist> getAllBanlists(DatabaseConnector dbCon) {
		List<Banlist> retBanlist = new ArrayList<>();

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, columnMap);

		for (Map<String, DbValue> r : results) {
			retBanlist.add(getBanlistFromResult(r));
		}

		return retBanlist;
	}

	public CommunityAsset getAsset() {
		return new GeneralAsset(id, communityId);
	}

	public String getInfoColumnsRaw() {
		return infoColumnsRaw;
	}

	public void editBanlist(DatabaseConnector dbCon, String newKey, String newName, int newGameId, int newCommunityId,
			String newInfoColumns) {
		dbCon.updateValue(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_KEY, DB_TABLE_COLUMN_NAME_NAME, DB_TABLE_COLUMN_NAME_GAME_ID,
						DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DB_TABLE_COLUMN_NAME_INFO_COLUMNS),
				Arrays.asList(new DbValue(newKey), new DbValue(newName), new DbValue(newGameId),
						new DbValue(newCommunityId), new DbValue(newInfoColumns)),
				DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public void removeBanlist(DatabaseConnector dbCon) {
		dbCon.deleteFrom(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(id));

		/// TODO add a backup here!

		// removing all bans:
		dbCon.deleteFrom(Ban.DB_TABLE_NAME, Ban.DB_TABLE_COLUMN_NAME_BANLIST_ID, new DbValue(id));
	}

	public List<String> getActiveBans(DatabaseConnector dbCon) {
		List<String> retList = new ArrayList<>();

		List<Ban> banList = Ban.getBansFromBanlistId(dbCon, id);

		Date today = new Date(System.currentTimeMillis());

		for (Ban ban : banList) {
			if (ban.isPermaBan()) {
				retList.add(ban.getHash());
			} else if (ban.getEnd().after(today)) {
				retList.add(ban.getHash());
			}
		}

		return retList;
	}

}