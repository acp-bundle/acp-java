package co.clai.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.logging.Level;

import co.clai.AcpSession;
import co.clai.access.AccessibleFunctionHelper;
import co.clai.access.AccessibleModuleHelper;
import co.clai.access.AssetServer;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Community;
import co.clai.db.model.Game;
import co.clai.db.model.Server;
import co.clai.db.model.ServerSetting;
import co.clai.game.AbstractGame;
import co.clai.game.AbstractGameUtil;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlForm.ButtonType;
import co.clai.html.HtmlForm.Method;
import co.clai.util.IntStringPair;
import co.clai.util.StringStringPair;
import co.clai.util.ValueValuePair;
import co.clai.html.HtmlPage;
import co.clai.html.HtmlResponsiveColumns;
import co.clai.html.HtmlTable;

public class EditServer extends AbstractModule {

	public static final String LOCATION = "editServer";
	public static final String TITLE = "Edit server settings";

	public static final String FUNCTION_NAME_ADD_SERVER = "addServer";
	public static final String FUNCTION_NAME_EDIT_SERVER = "editServer";
	public static final String FUNCTION_NAME_REMOVE_SERVER = "removeServer";
	public static final String FUNCTION_NAME_ADD_SETTING = "addSetting";
	public static final String FUNCTION_NAME_EDIT_SETTING = "editSetting";
	public static final String FUNCTION_NAME_REMOVE_SETTING = "removeSetting";

	private static final String GET_PARAM = "edit";

	private static final Map<String, AbstractGame> gameMap = new HashMap<>();

	public EditServer(DatabaseConnector dbCon) {
		super(LOCATION, dbCon);

		List<AbstractGame> tmpList = AbstractGameUtil.getAllGames();

		for (AbstractGame aG : tmpList) {
			logger.log(Level.INFO,
					"Adding game with key \"" + aG.getKey() + "\", class \"" + aG.getClass().toString() + "\"");
			gameMap.put(aG.getKey(), aG);
		}
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {
		HtmlPage p = new HtmlPage(dbCon, TITLE, null, null, null, s);

		p.writeH1("Edit Server Settings");

		HtmlTable serverListT = new HtmlTable();

		serverListT.addHeader(Arrays.asList("id", "key", "name", "game", "community", "banlist_ids", "edit", "delete"));

		List<Server> servers = Server.getAllServer(dbCon);

		for (Server ser : servers) {
			if (s.getThisUser().hasAccess(new AccessibleModuleHelper(LOCATION), ser.getAsset())) {
				HtmlTable.HtmlTableRow r = serverListT.new HtmlTableRow();

				r.writeText(ser.getId() + "");
				r.writeText(ser.getKey());
				r.writeText(ser.getName());
				try {
					r.writeText(Game.getGameById(dbCon, ser.getGameId()).getName());
				} catch (Exception e) {
					logger.log(Level.SEVERE,
							"Error while creating game with id " + ser.getGameId() + ": " + e.getMessage());
					r.writeText("unknown game");
				}
				try {
					r.writeText(Community.getCommunityById(dbCon, ser.getCommunityId()).getName());
				} catch (Exception e) {
					logger.log(Level.SEVERE,
							"Error while creating Community with id " + ser.getCommunityId() + ": " + e.getMessage());
					r.writeText("unknown community");
				}
				StringJoiner sb = new StringJoiner(",");

				for (Integer i : ser.getBanlistIds()) {
					sb.add(i.toString());
				}
				r.writeText(sb.toString());

				if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(LOCATION, FUNCTION_NAME_EDIT_SERVER),
						ser.getAsset())) {
					HtmlForm editForm = new HtmlForm(getModuleName(), Method.GET);
					editForm.addHiddenElement(GET_PARAM, FUNCTION_NAME_EDIT_SERVER);
					editForm.addHiddenElement(Server.DB_TABLE_COLUMN_NAME_ID, ser.getId() + "");
					editForm.addSubmit("Edit", ButtonType.WARNING);
					r.write(editForm);
				}

				if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(LOCATION, FUNCTION_NAME_REMOVE_SERVER),
						ser.getAsset())) {
					HtmlForm removeForm = new HtmlForm(getModuleName() + "." + FUNCTION_NAME_REMOVE_SERVER,
							Method.POST);
					removeForm.addHiddenElement(Server.DB_TABLE_COLUMN_NAME_ID, ser.getId() + "");
					removeForm.addSubmit("Remove", ButtonType.DANGER);
					r.write(removeForm);
				}

				serverListT.write(r);
			}
		}

		p.writeHline();

		p.write(serverListT);

		p.writeHline();

		List<ValueValuePair> selectionCommunityValues = new ArrayList<>();
		for (Community c : Community.getAllCommunity(dbCon)) {
			if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_SERVER),
					c.getAsset())) {
				selectionCommunityValues.add(new IntStringPair(c.getId(), c.getName()));
			}
		}

		if ((parameters.get(GET_PARAM) == null) || (parameters.get(GET_PARAM).length == 0)) {
			if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(LOCATION, FUNCTION_NAME_ADD_SERVER))) {
				p.writeH2("Add new Server:");

				HtmlForm addNewServerForm = new HtmlForm(getModuleName() + "." + FUNCTION_NAME_ADD_SERVER, Method.POST);
				addNewServerForm.addTextElement("key", Server.DB_TABLE_COLUMN_NAME_KEY, "");
				addNewServerForm.addTextElement("name", Server.DB_TABLE_COLUMN_NAME_NAME, "");
				addNewServerForm.addSelectionDropdown("Game", Server.DB_TABLE_COLUMN_NAME_GAME_ID,
						Game.getGameListSelection(dbCon));
				addNewServerForm.addSelectionDropdown("Community", Server.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						selectionCommunityValues);
				addNewServerForm.addSubmit("Add new Server", ButtonType.SUCCESS);
				p.write(addNewServerForm);
			}
		} else {
			HtmlResponsiveColumns cols = new HtmlResponsiveColumns();
			cols.startColumn(6);

			cols.writeH3("Edit Server default settings:");
			Server server = Server.getServerById(dbCon,
					Integer.parseInt(parameters.get(Server.DB_TABLE_COLUMN_NAME_ID)[0]));

			if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(LOCATION, FUNCTION_NAME_EDIT_SERVER),
					server.getAsset())) {
				p.writeText("No access");
				return p.finish().getBytes();
			}

			HtmlForm editServerForm = new HtmlForm(getModuleName() + "." + FUNCTION_NAME_EDIT_SERVER, Method.POST);
			editServerForm.addHiddenElement(Server.DB_TABLE_COLUMN_NAME_ID, server.getId() + "");
			editServerForm.addTextElement("key", Server.DB_TABLE_COLUMN_NAME_KEY, server.getKey());
			editServerForm.addTextElement("name", Server.DB_TABLE_COLUMN_NAME_NAME, server.getName());
			editServerForm.addSelectionDropdown("Game", Server.DB_TABLE_COLUMN_NAME_GAME_ID,
					Game.getGameListSelection(dbCon), server.getGameId() + "");
			editServerForm.addSelectionDropdown("Community", Server.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
					selectionCommunityValues, server.getCommunityId() + "");
			StringJoiner sj = new StringJoiner(",");
			for (Integer i : server.getBanlistIds()) {
				sj.add(i.toString());
			}
			editServerForm.addTextElement("Banlists", Server.DB_TABLE_COLUMN_NAME_BANLIST_IDS, sj.toString());
			editServerForm.addSubmit("Edit Settings", ButtonType.WARNING);

			cols.write(editServerForm);

			cols.startColumn(6);
			cols.writeH3("Edit all Server settings:");

			Set<String> usedOptions = new HashSet<>();

			AbstractGame game = gameMap.get(Game.getGameById(dbCon, server.getGameId()).getKey());

			for (ServerSetting set : ServerSetting.getAllServerSettingByServerId(dbCon, server.getId())) {
				usedOptions.add(set.getSettingKey());

				cols.write(game.renderOption(s, set));
				cols.writeHline();
			}

			cols.writeHline();

			cols.writeH3("Add new option:");

			List<ValueValuePair> selectionList = new ArrayList<>();

			for (StringStringPair pair : game.getAvailableOptions()) {
				if (!usedOptions.contains(pair.getId())) {
					selectionList.add(new StringStringPair(pair.getId(), pair.getName()));
				}
			}

			HtmlForm addNewOptionForm = new HtmlForm(getModuleName() + "." + FUNCTION_NAME_ADD_SETTING, Method.POST);
			addNewOptionForm.addHiddenElement(ServerSetting.DB_TABLE_COLUMN_NAME_SERVER_ID, server.getId() + "");
			addNewOptionForm.addSelectionDropdown("Option", ServerSetting.DB_TABLE_COLUMN_NAME_SETTING_KEY,
					selectionList);
			addNewOptionForm.addSubmit("Add new Option", ButtonType.SUCCESS);
			cols.write(addNewOptionForm);

			p.write(cols);
		}

		return p.finish().getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> retMap = new HashMap<>();

		retMap.put(FUNCTION_NAME_ADD_SERVER, this::addServer);
		retMap.put(FUNCTION_NAME_EDIT_SERVER, this::editServer);
		retMap.put(FUNCTION_NAME_REMOVE_SERVER, this::removeServer);
		retMap.put(FUNCTION_NAME_ADD_SETTING, this::addSetting);
		retMap.put(FUNCTION_NAME_EDIT_SETTING, this::editSetting);
		retMap.put(FUNCTION_NAME_REMOVE_SETTING, this::removeSetting);

		return retMap;
	}

	private FunctionResult addServer(AcpSession s, Map<String, String[]> parameters) {

		String key = parameters.get(Server.DB_TABLE_COLUMN_NAME_KEY)[0];
		String name1 = parameters.get(Server.DB_TABLE_COLUMN_NAME_NAME)[0];
		int gameId = Integer.parseInt(parameters.get(Server.DB_TABLE_COLUMN_NAME_GAME_ID)[0]);
		int communityId = Integer.parseInt(parameters.get(Server.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_SERVER),
				new AssetServer(0, communityId, gameId))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		Server.addNewServer(dbCon, key, name1, gameId, communityId);

		return new FunctionResult(FunctionResult.Status.OK, LOCATION);
	}

	private FunctionResult editServer(AcpSession s, Map<String, String[]> parameters) {

		int id = Integer.parseInt(parameters.get(Server.DB_TABLE_COLUMN_NAME_ID)[0]);
		String key = parameters.get(Server.DB_TABLE_COLUMN_NAME_KEY)[0];
		String name1 = parameters.get(Server.DB_TABLE_COLUMN_NAME_NAME)[0];
		int gameId = Integer.parseInt(parameters.get(Server.DB_TABLE_COLUMN_NAME_GAME_ID)[0]);
		int communityId = Integer.parseInt(parameters.get(Server.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);
		String banlistIds = parameters.get(Server.DB_TABLE_COLUMN_NAME_BANLIST_IDS)[0];

		Server server = Server.getServerById(dbCon, id);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_SERVER),
				server.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		server.edit(dbCon, key, name1, gameId, communityId, banlistIds);

		return new FunctionResult(FunctionResult.Status.OK, LOCATION, "Server successfully edited!");
	}

	private FunctionResult removeServer(AcpSession s, Map<String, String[]> parameters) {

		int id = Integer.parseInt(parameters.get(Server.DB_TABLE_COLUMN_NAME_ID)[0]);

		Server server = Server.getServerById(dbCon, id);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_REMOVE_SERVER),
				server.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		server.delete(dbCon);

		return new FunctionResult(FunctionResult.Status.OK, LOCATION, "Server removed.");
	}

	private FunctionResult addSetting(AcpSession s, Map<String, String[]> parameters) {

		int serverId = Integer.parseInt(parameters.get(ServerSetting.DB_TABLE_COLUMN_NAME_SERVER_ID)[0]);
		String settingKey = parameters.get(ServerSetting.DB_TABLE_COLUMN_NAME_SETTING_KEY)[0];

		Server server = Server.getServerById(dbCon, serverId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_SETTING),
				server.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		AbstractGame game = gameMap.get(Game.getGameById(dbCon, server.getGameId()).getKey());

		ServerSetting.addNewServerSetting(dbCon, serverId, settingKey, game.getDefaultOption(settingKey));

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);
		r.getBuilder().addParameter(GET_PARAM, GET_PARAM);
		r.getBuilder().addParameter(Server.DB_TABLE_COLUMN_NAME_ID, server.getId() + "");

		return r;
	}

	private FunctionResult editSetting(AcpSession s, Map<String, String[]> parameters) {

		int id = Integer.parseInt(parameters.get(ServerSetting.DB_TABLE_COLUMN_NAME_ID)[0]);
		String data = parameters.get(ServerSetting.DB_TABLE_COLUMN_NAME_DATA)[0];

		ServerSetting setting = ServerSetting.getServerSettingById(dbCon, id);

		Server server = Server.getServerById(dbCon, setting.getServerId());

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_SETTING),
				server.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		setting.edit(dbCon, data);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);
		r.getBuilder().addParameter(GET_PARAM, GET_PARAM);
		r.getBuilder().addParameter(Server.DB_TABLE_COLUMN_NAME_ID, server.getId() + "");

		return r;
	}

	private FunctionResult removeSetting(AcpSession s, Map<String, String[]> parameters) {

		int id = Integer.parseInt(parameters.get(ServerSetting.DB_TABLE_COLUMN_NAME_ID)[0]);

		ServerSetting setting = ServerSetting.getServerSettingById(dbCon, id);

		Server server = Server.getServerById(dbCon, setting.getServerId());

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_SETTING),
				server.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		setting.delete(dbCon);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);
		r.getBuilder().addParameter(GET_PARAM, GET_PARAM);
		r.getBuilder().addParameter(Server.DB_TABLE_COLUMN_NAME_ID, setting.getServerId() + "");

		return r;
	}

}