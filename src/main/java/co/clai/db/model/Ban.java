package co.clai.db.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;

public class Ban extends AbstractDbTable {

	private static final long ONE_DAY_IN_MILLIS = 24 * 3600 * 1000l;

	private static final String DB_VALUE_END_PERM = "perm";

	public static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	public static final String DB_TABLE_NAME = "ban";
	public static final String DB_TABLE_COLUMN_NAME_ID = "id";
	public static final String DB_TABLE_COLUMN_NAME_HASH = "hash";
	public static final String DB_TABLE_COLUMN_NAME_BANLIST_ID = "banlist_id";
	public static final String DB_TABLE_COLUMN_NAME_INFO = "info";
	public static final String DB_TABLE_COLUMN_NAME_START = "start";
	public static final String DB_TABLE_COLUMN_NAME_END = "end";

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_HASH, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_BANLIST_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_INFO, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_START, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_END, DbValueType.STRING);
	}

	public static final Ban dummyBan = new Ban();

	private final int id;
	private final String hash;
	private final int banlistId;
	private final JSONObject info;
	private final Date start;
	private final Date end;
	private final boolean isPermaBan;

	public Ban() {
		this(-1, null, -1, "{}", "0000-00-00", "0000-00-00");
	}

	private Ban(int id, String hash, int banlistId, String info, String start, String end) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.hash = hash;
		this.banlistId = banlistId;
		if ((info == null) || (info.equals(""))) {
			this.info = new JSONObject();
		} else {
			JSONObject tmpO = null;
			try {
				tmpO = new JSONObject(info);
			} catch (Exception e) {
				logger.log(Level.SEVERE,
						"error while creating json Object for Ban with id " + id + ": " + e.getMessage(), e);
				tmpO = new JSONObject();
			} finally {
				this.info = tmpO;
			}
		}

		Date tmpStart = null;
		Date tmpEnd = null;
		try {
			tmpStart = DEFAULT_TIME_FORMAT.parse(start);
			if (!DB_VALUE_END_PERM.equals(end)) {
				tmpEnd = DEFAULT_TIME_FORMAT.parse(end);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while parsing TimeDateFormat for Ban with id " + id, e);
		}

		this.start = tmpStart;

		if (DB_VALUE_END_PERM.equals(end)) {
			this.end = null;
			isPermaBan = true;
		} else {
			this.end = tmpEnd;
			isPermaBan = false;
		}
	}

	public int getId() {
		return id;
	}

	public String getHash() {
		return hash;
	}

	public int getBanlistId() {
		return banlistId;
	}

	public JSONObject getInfo() {
		return info;
	}

	public String getInfo(String key) {
		String retString = "";

		if (info.has(key)) {
			retString = info.getString(key);
		}

		return retString;
	}

	public Date getStart() {
		return start;
	}

	public Date getEnd() {
		return end;
	}

	public boolean isPermaBan() {
		return isPermaBan;
	}

	public static Ban getBanById(DatabaseConnector dbCon, int banId) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(banId),
				dummyBan.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getBanFromResult(results.get(0));
	}

	private static Ban getBanFromResult(Map<String, DbValue> parameters) {
		return new Ban(parameters.get(DB_TABLE_COLUMN_NAME_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_HASH).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_BANLIST_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_INFO).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_START).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_END).getString());
	}

	public static List<Ban> getBansFromBanlistId(DatabaseConnector dbCon, int banlistId) {

		List<Ban> retList = new ArrayList<>();

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_BANLIST_ID,
				new DbValue(banlistId), dummyBan.getColumns(), " ORDER BY " + DB_TABLE_COLUMN_NAME_START + " DESC ");

		for (Map<String, DbValue> r : results) {
			retList.add(getBanFromResult(r));
		}

		return retList;
	}

	public static void addNewBan(DatabaseConnector dbCon, String hash, int banlistId, String info, String start,
			String end, boolean permaban) {

		try {
			DEFAULT_TIME_FORMAT.parse(start);
			if (!permaban) {
				DEFAULT_TIME_FORMAT.parse(end);
			}
		} catch (Exception e) {
			throw new RuntimeException("Cannot instanciate Date from Arguments: " + e.getMessage());
		}

		String newEnd = end;

		if (permaban) {
			newEnd = DB_VALUE_END_PERM;
		}

		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_HASH, DB_TABLE_COLUMN_NAME_BANLIST_ID, DB_TABLE_COLUMN_NAME_INFO,
						DB_TABLE_COLUMN_NAME_START, DB_TABLE_COLUMN_NAME_END),
				Arrays.asList(new DbValue(hash), new DbValue(banlistId), new DbValue(info), new DbValue(start),
						new DbValue(newEnd)));
	}

	public void edit(DatabaseConnector dbCon, String newHash, String newInfo, String newStart, String newEnd2,
			boolean newPermaban) {

		try {
			DEFAULT_TIME_FORMAT.parse(newStart);
			if (!newPermaban) {
				DEFAULT_TIME_FORMAT.parse(newEnd2);
			}
		} catch (Exception e) {
			throw new RuntimeException("Cannot instanciate Date from Arguments: " + e.getMessage());
		}

		String newEnd = newEnd2;

		if (newPermaban) {
			newEnd = DB_VALUE_END_PERM;
		}

		dbCon.updateValue(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_HASH, DB_TABLE_COLUMN_NAME_INFO, DB_TABLE_COLUMN_NAME_START,
						DB_TABLE_COLUMN_NAME_END),
				Arrays.asList(new DbValue(newHash), new DbValue(newInfo), new DbValue(newStart), new DbValue(newEnd)),
				DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public void unban(DatabaseConnector dbCon) {

		Date endDate = new Date(System.currentTimeMillis() - ONE_DAY_IN_MILLIS);

		dbCon.updateValue(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_END),
				Arrays.asList(new DbValue(DEFAULT_TIME_FORMAT.format(endDate))), DB_TABLE_COLUMN_NAME_ID,
				new DbValue(id));
	}

	public boolean isExpired() {

		if (isPermaBan) {
			return false;
		}

		Date yesterday = new Date(System.currentTimeMillis() - ONE_DAY_IN_MILLIS);

		if (end.compareTo(yesterday) <= 0) {
			return true;
		}

		return false;
	}
}