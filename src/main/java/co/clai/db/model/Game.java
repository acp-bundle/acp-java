package co.clai.db.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.clai.db.DatabaseConnector;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;
import co.clai.game.AbstractGame;
import co.clai.game.AbstractGameUtil;
import co.clai.util.IntStringPair;
import co.clai.util.ValueValuePair;

public class Game extends AbstractDbTable {

	public static final String DB_TABLE_NAME = "game";
	public static final String DB_TABLE_COLUMN_NAME_ID = "id";
	public static final String DB_TABLE_COLUMN_NAME_KEY = "_key";
	public static final String DB_TABLE_COLUMN_NAME_NAME = "name";

	private final static Map<String, DbValueType> columnMap = new HashMap<>();
	{
		columnMap.put(DB_TABLE_COLUMN_NAME_ID, DbValueType.INTEGER);
		columnMap.put(DB_TABLE_COLUMN_NAME_KEY, DbValueType.STRING);
		columnMap.put(DB_TABLE_COLUMN_NAME_NAME, DbValueType.STRING);
	}

	public static final Game dummyGame = new Game();

	private final int id;
	private final String key;
	private final String name;

	public Game() {
		this(-1, null, null);
	}

	private Game(int id, String key, String name) {
		super(DB_TABLE_NAME, columnMap);

		this.id = id;
		this.name = name;
		this.key = key;
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

	public static void populateGameTable(DatabaseConnector dbCon) {
		List<AbstractGame> games = AbstractGameUtil.getAllGames();

		for (AbstractGame g : games) {
			dbCon.insert(DB_TABLE_NAME, Arrays.asList(DB_TABLE_COLUMN_NAME_KEY, DB_TABLE_COLUMN_NAME_NAME),
					Arrays.asList(new DbValue(g.getKey()), new DbValue(g.getName())));
		}
	}

	public static Game getGameById(DatabaseConnector dbCon, int gameId) {

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, DB_TABLE_COLUMN_NAME_ID, new DbValue(gameId),
				dummyGame.getColumns());

		if (results.isEmpty()) {
			return null;
		}

		return getGameFromResult(results.get(0));
	}

	private static Game getGameFromResult(Map<String, DbValue> parameters) {
		return new Game(parameters.get(DB_TABLE_COLUMN_NAME_ID).getInt(),
				parameters.get(DB_TABLE_COLUMN_NAME_KEY).getString(),
				parameters.get(DB_TABLE_COLUMN_NAME_NAME).getString());
	}

	public static List<Game> getAllGame(DatabaseConnector dbCon) {
		List<Game> retList = new ArrayList<>();

		List<Map<String, DbValue>> results = dbCon.select(DB_TABLE_NAME, dummyGame.getColumns());

		for (Map<String, DbValue> r : results) {
			retList.add(getGameFromResult(r));
		}

		return retList;
	}

	public static List<ValueValuePair> getGameListSelection(DatabaseConnector dbCon) {
		List<ValueValuePair> retList = new ArrayList<>();

		for (Game g : getAllGame(dbCon)) {
			retList.add(new IntStringPair(g.getId(), g.getName()));
		}

		return retList;
	}

}
