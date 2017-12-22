package co.clai.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.logging.Level;

import co.clai.AcpSession;
import co.clai.access.AccessibleHelper;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Game;
import co.clai.db.model.Server;
import co.clai.game.AbstractGame;
import co.clai.game.AbstractGameUtil;

public class Query extends AbstractModule {

	private static final String QUERY_GET_KEY_SERVER_KEY = "server_key";
	private static final Object QUERY_GET_KEY_COMMAND = "command";

	public static final String LOCATION = "query";

	private static final Map<String, AbstractGame> gameMap = new HashMap<>();

	public Query(DatabaseConnector dbCon) {
		super(LOCATION, dbCon, new AccessibleHelper(true));

		List<AbstractGame> tmpList = AbstractGameUtil.getAllGames();

		for (AbstractGame aG : tmpList) {
			gameMap.put(aG.getKey(), aG);
		}
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {

		try {
			Server server = Server.getServerByKey(dbCon, parameters.get(QUERY_GET_KEY_SERVER_KEY)[0]);
			AbstractGame game = gameMap.get(Game.getGameById(dbCon, server.getGameId()).getKey());

			return game.executeQuery(dbCon, parameters.get(QUERY_GET_KEY_COMMAND)[0], server, parameters, s).getBytes();

		} catch (Exception e) {

			StringBuilder sb = new StringBuilder("query failed for parameter: " + e.getMessage() + ": ");

			for (Entry<String, String[]> e1 : parameters.entrySet()) {
				sb.append(e1.getKey() + "=" + e1.getValue()[0] + ", ");
			}

			logger.log(Level.WARNING, sb.toString());

			// return nothing on purpose!
			return "".getBytes();
		}
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		return new HashMap<>();
	}

}
