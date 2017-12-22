package co.clai.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import co.clai.AcpSession;
import co.clai.access.AccessibleFunctionHelper;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Server;
import co.clai.db.model.ServerSetting;
import co.clai.db.model.Template;
import co.clai.game.squad.QuerySquad;
import co.clai.html.Builder;
import co.clai.html.GenericBuffer;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlForm.ButtonType;
import co.clai.module.EditServer;
import co.clai.module.Query;
import co.clai.util.StringStringPair;
import co.clai.util.log.LoggingUtil;

public class Squad extends AbstractGame {

	private static final String QUERY_KEY_SERVER_CFG = "server_cfg";
	private static final String QUERY_KEY_BAN_CFG = "ban_cfg";
	private static final String QUERY_KEY_ADMINS_CFG = "admins_cfg";
	private static final String QUERY_KEY_RESERVEDSLOTS = "reserved_slots";

	private static final String CONFIG_KEY_IP = "ip";
	private static final String CONFIG_KEY_PORT = "port";
	private static final String CONFIG_KEY_QUERY = "query";
	private static final String CONFIG_KEY_TEMPLATE_SERVER_CFG = "template_server_cfg";

	public static final String CONFIG_KEY_ADMIN_GROUPS = "admin_groups";
	public static final String CONFIG_KEY_RESERVED_GROUPS = "reserved_groups";

	public final static String KEY = "squad";
	public final static String NAME = "Squad";

	private final Map<String, String> options = loadOptions();

	private final Logger logger = LoggingUtil.getLoggerFromModule(Query.class);

	private static Map<String, String> loadOptions() {
		Map<String, String> retMap = new HashMap<>();

		retMap.put(CONFIG_KEY_IP, "IP Adress");
		retMap.put(CONFIG_KEY_PORT, "Game Port");
		retMap.put(CONFIG_KEY_QUERY, "Query Port");
		retMap.put(CONFIG_KEY_TEMPLATE_SERVER_CFG, "Template for Server.cfg");
		retMap.put(CONFIG_KEY_ADMIN_GROUPS, "Admin Usergroups");
		retMap.put(CONFIG_KEY_RESERVED_GROUPS, "Groups with reserved Slot");

		return retMap;
	}

	protected Squad() {
		super(KEY, NAME);
	}

	@Override
	public List<StringStringPair> getAvailableOptions() {
		List<StringStringPair> retOptions = new ArrayList<>();

		for (Entry<String, String> e : options.entrySet()) {
			retOptions.add(new StringStringPair(e.getKey(), e.getValue()));
		}

		return retOptions;
	}

	@Override
	public Builder renderOption(AcpSession s, ServerSetting setting) {
		HtmlForm hf = new HtmlForm(EditServer.LOCATION + "." + EditServer.FUNCTION_NAME_EDIT_SETTING,
				HtmlForm.Method.POST);
		Builder b = new GenericBuffer("");
		final String setKey = setting.getSettingKey();

		boolean hasDeleteButton = false;

		switch (setKey) {
		case CONFIG_KEY_ADMIN_GROUPS:
		case CONFIG_KEY_RESERVED_GROUPS:
			hf.writeText(options.get(setKey) + ":");
			hf.addTextArea(ServerSetting.DB_TABLE_COLUMN_NAME_DATA, new String(setting.getData()), 5, 15);
			hasDeleteButton = true;
			break;

		case CONFIG_KEY_IP:
		case CONFIG_KEY_PORT:
		case CONFIG_KEY_QUERY:
			hf.addTextElement(options.get(setKey), ServerSetting.DB_TABLE_COLUMN_NAME_DATA,
					new String(setting.getData()));
			break;

		case CONFIG_KEY_TEMPLATE_SERVER_CFG:
			hf.addSelectionDropdown(options.get(setKey), ServerSetting.DB_TABLE_COLUMN_NAME_DATA,
					Template.getAccessibleTemplates(
							new AccessibleFunctionHelper(EditServer.LOCATION, EditServer.FUNCTION_NAME_EDIT_SETTING),
							s.getDbCon(), s.getThisUser()),
					new String(setting.getData()));
			break;

		default:
			hf.addTextElement("Unknown option " + setKey, ServerSetting.DB_TABLE_COLUMN_NAME_DATA,
					new String(setting.getData()));
			break;
		}

		hf.addHiddenElement(ServerSetting.DB_TABLE_COLUMN_NAME_ID, setting.getId() + "");
		hf.addSubmit("Change Value", ButtonType.WARNING);

		b.write(hf);

		if (hasDeleteButton) {
			HtmlForm deleteButtonF = new HtmlForm(EditServer.LOCATION + "." + EditServer.FUNCTION_NAME_REMOVE_SETTING,
					HtmlForm.Method.POST, null);

			deleteButtonF.addHiddenElement(ServerSetting.DB_TABLE_COLUMN_NAME_ID, setting.getId() + "");

			deleteButtonF.addSubmit("Delete Setting", ButtonType.DANGER);
			b.write(deleteButtonF);
		}

		return b;
	}

	@Override
	public List<StringStringPair> getAvailableQueries() {
		List<StringStringPair> retOptions = new ArrayList<>();

		retOptions.addAll(Arrays.asList(new StringStringPair(QUERY_KEY_ADMINS_CFG, "Admins.cfg file"),
				new StringStringPair(QUERY_KEY_BAN_CFG, "Bans.cfg file"),
				new StringStringPair(QUERY_KEY_SERVER_CFG, "Server.cfg file"),
				new StringStringPair(QUERY_KEY_RESERVEDSLOTS, "Reserved Slots Admin config")));

		return retOptions;
	}

	@Override
	public String executeQuery(DatabaseConnector dbCon, String command, Server server, Map<String, String[]> parameters,
			AcpSession session) {
		switch (command) {
		case QUERY_KEY_ADMINS_CFG: {

			StringBuilder sb = new StringBuilder();

			QuerySquad.generateAdminConfig(dbCon, sb, server);

			return sb.toString();
		}

		case QUERY_KEY_RESERVEDSLOTS: {

			StringBuilder sb = new StringBuilder();

			QuerySquad.generateReservedSlots(dbCon, sb, server);

			return sb.toString();
		}

		default:
			logger.log(Level.WARNING, "Unknown query command '" + command + "'!");
			return "";
		}
	}

	@Override
	public String getDefaultOption(String settingKey) {
		switch (settingKey) {
		case CONFIG_KEY_RESERVED_GROUPS:
			return "[\n" + "{\n" + "    \"location\" : \"<location id>\",\n" + "    \"field_id\" : \"<field id>\",\n"
					+ "    \"groups\" : \"<group 1>,<group 2>\"\n" + "}\n" + "]\n";
		case CONFIG_KEY_ADMIN_GROUPS:
			return "{\n" + "\"roles\": [\n" + "\"Group=Admin:kick,changemap,immunity,chat,canseeadminchat\",\n" + "],\n"
					+ "\"group_access\" : [\n" + "{\n" + "    \"location\" : \"<location id>\",\n"
					+ "    \"field_id\" : \"<field id>\",\n" + "    \"groups\" : [\n" + "        {\n"
					+ "            \"role\" : \"<role 1>\",\n" + "            \"groups\" : \"<group 1>,<group 2>\"\n"
					+ "        }\n" + "}\n" + "]\n" + "}";
		case CONFIG_KEY_PORT:
			return "7787";
		case CONFIG_KEY_QUERY:
			return "27165";
		default:
			return "";
		}
	}

}
