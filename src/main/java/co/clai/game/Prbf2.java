package co.clai.game;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import co.clai.AcpSession;
import co.clai.access.AccessibleFunctionHelper;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Banlist;
import co.clai.db.model.Server;
import co.clai.db.model.ServerSetting;
import co.clai.db.model.Storage;
import co.clai.db.model.StorageIndex;
import co.clai.db.model.Template;
import co.clai.game.prbf2.QueryPrbf2;
import co.clai.html.Builder;
import co.clai.html.GenericBuffer;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlForm.ButtonType;
import co.clai.module.EditServer;
import co.clai.module.Query;
import co.clai.storage.AbstractStorage;
import co.clai.util.StringStringPair;
import co.clai.util.log.LoggingUtil;

public class Prbf2 extends AbstractGame {

	private static final String QUERY_KEY_SERVERSETTINGS = "serversettings";
	private static final String QUERY_KEY_REALITYCONFIG_ADMIN = "realityconfig_admin";
	private static final String QUERY_KEY_MAPLIST = "maplist";
	private static final String QUERY_KEY_BANLIST = "banlist";
	private static final String QUERY_KEY_PUT_CHATLOG = "put_chatlog";

	public static final String CONFIG_KEY_IP = "ip";
	public static final String CONFIG_KEY_PORT = "port";
	public static final String CONFIG_KEY_QUERY = "query";
	private static final String CONFIG_KEY_TEMPLATE_SERVERSETTINGS = "serversettings";
	private static final String CONFIG_KEY_TEMPLATE_REALITYCONFIG_ADMIN = "template_realityconfig_admin";
	private static final String CONFIG_KEY_MAPLIST = "maplist";

	public static final String CONFIG_KEY_LOCAL_IP = "local_ip";
	public static final String CONFIG_KEY_SV_ADMIN_SCRIPT = "sv_adminScript";
	public static final String CONFIG_KEY_SV_BROWSER_NAME = "browser_name";
	public static final String CONFIG_KEY_SV_PASSWORD = "sv_password";
	public static final String CONFIG_KEY_SV_WELCOMEMESSAGE = "sv_welcomeMessage";
	public static final String CONFIG_KEY_SV_LOGO_URL = "sv_LogoURL";
	public static final String CONFIG_KEY_SV_MAX_PLAYERS = "sv_maxPlayers";

	public static final String CONFIG_KEY_ADMIN_GROUPS = "admin_groups";
	public static final String CONFIG_KEY_SMB_EXCLUDE_LIST = "smb_excludeList";

	public final static String KEY = "pfbf2";
	public final static String NAME = "Project Reality: Battlefield 2";

	private final Logger logger = LoggingUtil.getLoggerFromModule(Query.class);

	protected Prbf2() {
		super(KEY, NAME);
	}

	private final Map<String, String> options = loadOptions();

	private static Map<String, String> loadOptions() {
		Map<String, String> retMap = new HashMap<>();

		retMap.put(CONFIG_KEY_IP, "IP Adress");
		retMap.put(CONFIG_KEY_PORT, "Game Port");
		retMap.put(CONFIG_KEY_QUERY, "Query Port");
		retMap.put(CONFIG_KEY_TEMPLATE_SERVERSETTINGS, "Template for serversettings.con");
		retMap.put(CONFIG_KEY_TEMPLATE_REALITYCONFIG_ADMIN, "Template for realityconfig_admin.py");
		retMap.put(CONFIG_KEY_MAPLIST, "maplist.con");
		retMap.put(CONFIG_KEY_LOCAL_IP, "Local IP Adress");
		retMap.put(CONFIG_KEY_SV_ADMIN_SCRIPT, "Admin script");
		retMap.put(CONFIG_KEY_SV_BROWSER_NAME, "In Game Name");
		retMap.put(CONFIG_KEY_SV_PASSWORD, "Password");
		retMap.put(CONFIG_KEY_SV_WELCOMEMESSAGE, "Welcome message");
		retMap.put(CONFIG_KEY_SV_LOGO_URL, "logo url");
		retMap.put(CONFIG_KEY_SV_MAX_PLAYERS, "maximum player count");
		retMap.put(CONFIG_KEY_ADMIN_GROUPS, "Admin Usergroups");
		retMap.put(CONFIG_KEY_SMB_EXCLUDE_LIST, "Smartbalance excluded Clan Tags");

		return retMap;
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
		Builder b = new GenericBuffer("");
		HtmlForm hf = new HtmlForm(EditServer.LOCATION + "." + EditServer.FUNCTION_NAME_EDIT_SETTING,
				HtmlForm.Method.POST, null);

		final String setKey = setting.getSettingKey();

		boolean hasDeleteButton = false;

		switch (setKey) {
		case CONFIG_KEY_ADMIN_GROUPS:
			hf.writeText(options.get(setKey) + ":");
			hf.addTextArea(ServerSetting.DB_TABLE_COLUMN_NAME_DATA, new String(setting.getData()), 5, 15);
			hasDeleteButton = true;
			break;

		case CONFIG_KEY_LOCAL_IP:
		case CONFIG_KEY_SV_ADMIN_SCRIPT:
		case CONFIG_KEY_SV_BROWSER_NAME:
		case CONFIG_KEY_SV_PASSWORD:
		case CONFIG_KEY_SV_WELCOMEMESSAGE:
		case CONFIG_KEY_SV_LOGO_URL:
		case CONFIG_KEY_SMB_EXCLUDE_LIST:
		case CONFIG_KEY_SV_MAX_PLAYERS:
			hasDeleteButton = true;
			//$FALL-THROUGH$
		case CONFIG_KEY_IP:
		case CONFIG_KEY_PORT:
		case CONFIG_KEY_QUERY:
			hf.addTextElement(options.get(setKey), ServerSetting.DB_TABLE_COLUMN_NAME_DATA,
					new String(setting.getData()));
			break;

		case CONFIG_KEY_TEMPLATE_SERVERSETTINGS:
		case CONFIG_KEY_TEMPLATE_REALITYCONFIG_ADMIN:
		case CONFIG_KEY_MAPLIST:
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

		retOptions.addAll(
				Arrays.asList(new StringStringPair(QUERY_KEY_REALITYCONFIG_ADMIN, "realityconfig_admin.py file"),
						new StringStringPair(QUERY_KEY_SERVERSETTINGS, "serversettings.con file"),
						new StringStringPair(QUERY_KEY_MAPLIST, "maplist.con file"),
						new StringStringPair(QUERY_KEY_BANLIST, "banlist.con file"),
						new StringStringPair(QUERY_KEY_PUT_CHATLOG, "puts a chatlog into storage")));

		return retOptions;
	}

	@Override
	public String executeQuery(DatabaseConnector dbCon, String command, Server server, Map<String, String[]> parameters,
			AcpSession session) {
		switch (command) {
		case QUERY_KEY_SERVERSETTINGS: {
			ServerSetting setting = ServerSetting.getServerSettingByServerIdAndKey(dbCon, server.getId(),
					CONFIG_KEY_TEMPLATE_SERVERSETTINGS);

			if (setting == null) {
				return "";
			}

			StringBuilder sb = new StringBuilder(
					new String((Template.getTemplateByKey(dbCon, new String(setting.getData())).getData())));

			QueryPrbf2.appendServerSettings(dbCon, sb, server);

			return sb.toString();
		}

		case QUERY_KEY_REALITYCONFIG_ADMIN: {
			ServerSetting setting = ServerSetting.getServerSettingByServerIdAndKey(dbCon, server.getId(),
					CONFIG_KEY_TEMPLATE_REALITYCONFIG_ADMIN);

			if (setting == null) {
				return "";
			}

			StringBuilder sb = new StringBuilder(
					new String((Template.getTemplateByKey(dbCon, new String(setting.getData())).getData())));

			QueryPrbf2.appendAdminHashes(dbCon, sb, server);

			return sb.toString();
		}

		case QUERY_KEY_MAPLIST: {
			ServerSetting setting = ServerSetting.getServerSettingByServerIdAndKey(dbCon, server.getId(),
					CONFIG_KEY_MAPLIST);

			if (setting == null) {
				return "";
			}

			return new String(Template.getTemplateByKey(dbCon, new String(setting.getData())).getData());
		}

		case QUERY_KEY_BANLIST: {

			List<Integer> banlistIds = server.getBanlistIds();

			Set<String> bannedHashes = new HashSet<>();

			for (Integer i : banlistIds) {
				Banlist list = Banlist.getBanlistById(dbCon, i.intValue());
				if (list.getCommunityId() != server.getCommunityId()) {
					logger.log(Level.WARNING, "server with id " + server.getId()
							+ " trying to pull bans from other community; banlist id: " + list.getId());
					continue;
				}

				bannedHashes.addAll(list.getActiveBans(dbCon));
			}

			StringBuilder sb = new StringBuilder();

			for (String s : bannedHashes) {
				sb.append("admin.addKeyToBanList " + s + " Perm\n");
			}

			return sb.toString();
		}

		case QUERY_KEY_PUT_CHATLOG: {

			try {
				Storage stor = Storage.getStorageByKey(dbCon,
						parameters.get(StorageIndex.DB_TABLE_COLUMN_NAME_STORAGE_ID)[0]);

				AbstractStorage absStor = AbstractStorage.getRemoteFromLocation(stor);

				String identifier = parameters.get(StorageIndex.DB_TABLE_COLUMN_NAME_IDENTIFIER)[0];

				StringBuilder indexName = new StringBuilder();

				String chatlog[] = new String(absStor.getData(identifier)).split("\n");
				indexName.append(chatlog[0].substring(15) + " ");
				indexName.append(chatlog[1].substring(15) + " ");
				indexName.append(chatlog[2].substring(15));

				List<SimpleDateFormat> dateFormList = new ArrayList<>();
				dateFormList.add(new SimpleDateFormat("yyyy-mm-dd kk:mm"));
				dateFormList.add(new SimpleDateFormat("yyyymmdd kk:mm"));

				String timeString = chatlog[5].substring(15);

				long timestamp = -1;

				for (SimpleDateFormat f : dateFormList) {
					try {
						timestamp = f.parse(timeString).getTime();
					} catch (Exception e) {
						System.out.println("tried parsing: " + e.getMessage());
					}
				}

				absStor.pushIndex(dbCon, identifier, indexName.toString(), timestamp, session.getClientIp());
			} catch (Exception e) {
				e.printStackTrace();
				return "error: " + e.getMessage();
			}

			return "success";
		}

		default:
			logger.log(Level.WARNING, "Unknown query command '" + command + "'!");
			return "";
		}
	}

	@Override
	public String getDefaultOption(String settingKey) {
		switch (settingKey) {
		case CONFIG_KEY_ADMIN_GROUPS:
			return "[\n" + "{\n" + "    \"location\" : \"<location id>\",\n    \"field_id\" : \"<field id>\",\n"
					+ "    \"groups\" : [\n        {\n            \"role\" : \"<role 1>\",\n"
					+ "            \"groups\" : \"<groups 1>\"\n        },\n        {\n"
					+ "            \"role\" : \"<role 2>\",\n            \"groups\" : \"<groups 2>\"\n"
					+ "        }\n}\n]";
		case CONFIG_KEY_SV_ADMIN_SCRIPT:
			return "prism";
		case CONFIG_KEY_PORT:
			return "16567";
		case CONFIG_KEY_QUERY:
			return "29900";
		case CONFIG_KEY_SMB_EXCLUDE_LIST:
			return "[\"[R-DEV]*\",\"[R-CON]*\"]";
		case CONFIG_KEY_SV_MAX_PLAYERS:
			return "100";
		default:
			return "";
		}
	}

}
