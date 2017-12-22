package co.clai.db.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.clai.access.AccessibleHelper;
import co.clai.access.CommunityAsset;
import co.clai.access.GeneralAsset;
import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;
import co.clai.util.StringStringPair;
import co.clai.util.ValueValuePair;

public class Template extends AbstractDbTable {

	public static final String DB_TABLE_NAME = "template";
	public static final String DB_TABLE_COLUMN_NAME_ID = "id";
	public static final String DB_TABLE_COLUMN_NAME_KEY = "_key";
	public static final String DB_TABLE_COLUMN_NAME_NAME = "name";
	public static final String DB_TABLE_COLUMN_NAME_COMMUNITY_ID = "community_id";
	public static final String DB_TABLE_COLUMN_NAME_DATA = "data";

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_KEY, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_NAME, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_COMMUNITY_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_DATA, DbValueType.BLOB);
	}

	public static final Template dummyTemplate = new Template();

	private final int id;
	private final String key;
	private final String name;
	private final int communityId;
	private final byte[] data;

	public Template() {
		this(-1, null, null, -1, null);
	}

	private Template(int id, String key, String name, int communityId, byte[] data) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.name = name;
		this.key = key;
		this.communityId = communityId;
		this.data = data;
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

	public int getCommunityId() {
		return communityId;
	}

	public byte[] getData() {
		return data;
	}

	public CommunityAsset getAsset() {
		return new GeneralAsset(id, communityId);
	}

	private static Template getTemplateFromResult(Map<String, DbValue> r) {
		int rId = r.get(DB_TABLE_COLUMN_NAME_ID).getInt();
		String rKey = r.get(DB_TABLE_COLUMN_NAME_KEY).getString();
		String rName = r.get(DB_TABLE_COLUMN_NAME_NAME).getString();
		int rCommunityId = r.get(DB_TABLE_COLUMN_NAME_COMMUNITY_ID).getInt();
		byte[] rData = r.get(DB_TABLE_COLUMN_NAME_DATA).getBlobAsByteArr();

		Template l = new Template(rId, rKey, rName, rCommunityId, rData);
		return l;
	}

	public static Template getTemplateById(DatabaseConnector dbCon, int id) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(id),
				dummyTemplate.getColumns());

		return getTemplateFromResult(results.get(0));
	}

	public static Template getTemplateByKey(DatabaseConnector dbCon, String key) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_KEY, new DbValue(key),
				dummyTemplate.getColumns());

		return getTemplateFromResult(results.get(0));
	}

	public static List<Template> getAllTemplates(DatabaseConnector dbCon) {

		List<Map<String, DbValue>> rs = dbCon.select(DB_TABLE_NAME, dummyTemplate.getColumns());

		List<Template> retList = new ArrayList<>();

		for (Map<String, DbValue> r : rs) {
			retList.add(getTemplateFromResult(r));
		}

		return retList;
	}

	public static void addNewTemplate(DatabaseConnector dbCon, String name, String key, int communityId) {
		dbCon.insert(DB_TABLE_NAME,
				Arrays.asList(DB_TABLE_COLUMN_NAME_KEY, DB_TABLE_COLUMN_NAME_NAME, DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						DB_TABLE_COLUMN_NAME_DATA),
				Arrays.asList(new DbValue(key), new DbValue(name), new DbValue(communityId), DbValue.newBlob("")));
	}

	public void edit(DatabaseConnector dbCon, String newData) {
		dbCon.updateValue(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_DATA),
				Arrays.asList(DbValue.newBlob(newData)), DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public void delete(DatabaseConnector dbCon) {
		dbCon.deleteFrom(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(id));
	}

	public static List<ValueValuePair> getAccessibleTemplates(AccessibleHelper accHelper, DatabaseConnector dbCon,
			User thisUser) {
		List<ValueValuePair> retList = new ArrayList<>();

		for (Template t : getAllTemplates(dbCon)) {
			if (t.getCommunityId() == 0) {
				retList.add(new StringStringPair(t.getKey(), t.getName()));
			} else if (thisUser.hasAccess(accHelper, t.getAsset())) {
				retList.add(new StringStringPair(t.getKey(), t.getName()));
			}
		}

		return retList;
	}
}
