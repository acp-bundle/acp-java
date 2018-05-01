package co.clai.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;

import org.apache.http.client.utils.URIBuilder;

import co.clai.AcpSession;
import co.clai.access.AccessibleFunctionHelper;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Ban;
import co.clai.db.model.Server;
import co.clai.db.model.Storage;
import co.clai.db.model.StorageIndex;
import co.clai.html.Builder;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlPage;
import co.clai.html.HtmlTable;
import co.clai.html.HtmlTable.HtmlTableRow;
import co.clai.html.HtmlForm.ButtonType;
import co.clai.html.HtmlForm.Method;
import co.clai.storage.AbstractStorage;
import co.clai.storage.StorageSearchEntry;
import co.clai.storage.StorageType;
import co.clai.util.IntStringPair;
import co.clai.util.ValueValuePair;

public class Search extends AbstractModule {

	public static final String LOCATION = "search";
	public static final String TITLE = "Search logs and User";

	public static final String FUNCTION_NAME_SEARCH_USER = "searchForUser";
	public static final String FUNCTION_NAME_SEARCH_LOGS = "searchLogs";
	public static final String FUNCTION_NAME_VIEW_FILE = "viewFile";

	public static final String GET_PARAM = "do";
	private static final String GET_PARAM_VALUE_SEARCH_LOGS = "searchLogs";
	private static final String GET_PARAM_VALUE_SEARCH_USER = "searchUser";
	private static final String GET_PARAM_VALUE_LIST = "list";
	public static final String GET_PARAM_VALUE_VIEW = "view";
	public static final String GET_PARAM_VALUE_DOWNLOAD = "download";

	public static final String GET_PARAM_SEARCH_FOR = "search_for";
	private static final String GET_PARAM_SERVER_IDS = "server_ids";
	public static final String GET_PARAM_SERVER_START = "search_end";
	public static final String GET_PARAM_SERVER_END = "search_start";
	public static final String GET_PARAM_ALL_TIME = "all_time";

	private static final long THREE_MONTHS_IN_MILLIS = 1000 * 60 * 60 * 24 * 32 * 3;

	public Search(DatabaseConnector dbCon) {
		super(LOCATION, dbCon);
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {

		if ((parameters.get(GET_PARAM) == null) || (parameters.get(GET_PARAM).length == 0)) {
			HtmlPage p = new HtmlPage(dbCon, TITLE, null, null, null, s);

			p.writeH1("Search logs & access Files");

			HtmlForm searchLogs = new HtmlForm(LOCATION, Method.GET);
			searchLogs.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_SEARCH_LOGS);
			searchLogs.addSubmit("Search chatlogs from server", ButtonType.SUCCESS);
			p.write(searchLogs);

			HtmlForm searchUser = new HtmlForm(LOCATION, Method.GET);
			searchUser.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_SEARCH_USER);
			searchUser.addSubmit("Search for user", ButtonType.SUCCESS);
			p.write(searchUser);

			HtmlForm accessFilesF = new HtmlForm(LOCATION, Method.GET);
			accessFilesF.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_LIST);
			accessFilesF.addSubmit("Access files from Storage", ButtonType.SUCCESS);
			p.write(accessFilesF);

			return p.finish().getBytes();
		}

		Builder b;

		switch (parameters.get(GET_PARAM)[0]) {
		case GET_PARAM_VALUE_SEARCH_LOGS: {
			HtmlPage p = new HtmlPage(dbCon, TITLE, null, null, null, s);
			b = p;

			p.writeH2("Search logs:");

			List<ValueValuePair> serverSelection = new ArrayList<>();
			Set<String> selectedServer = new HashSet<>();

			List<Server> servers = Server.getAllServer(dbCon);
			for (Server ser : servers) {
				if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(LOCATION, FUNCTION_NAME_SEARCH_LOGS),
						ser.getAsset())) {
					serverSelection.add(new IntStringPair(ser.getId(), ser.getName()));
				}
			}

			// if no server are being selected aka if this page in invoked plainly, select
			// all server by default
			if (parameters.containsKey(GET_PARAM_SERVER_IDS)) {
				for (String serverId : parameters.get(GET_PARAM_SERVER_IDS)) {
					selectedServer.add(serverId);
				}
			} else {
				for (ValueValuePair servPair : serverSelection) {
					selectedServer.add(servPair.getId());
				}
			}

			HtmlForm searchForm = new HtmlForm(LOCATION, Method.GET);
			searchForm.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_SEARCH_LOGS);
			if (parameters.containsKey(GET_PARAM_SEARCH_FOR)) {
				searchForm.addTextElement("Search for", GET_PARAM_SEARCH_FOR, parameters.get(GET_PARAM_SEARCH_FOR)[0]);
			} else {
				searchForm.addTextElement("Search for", GET_PARAM_SEARCH_FOR, "");
			}
			searchForm.addCheckboxGroup("Server", GET_PARAM_SERVER_IDS, serverSelection, selectedServer);
			searchForm.addTextElement("Start", GET_PARAM_SERVER_START,
					Ban.DEFAULT_TIME_FORMAT.format(new Date(System.currentTimeMillis() - THREE_MONTHS_IN_MILLIS)));
			searchForm.addTextElement("End", GET_PARAM_SERVER_END,
					Ban.DEFAULT_TIME_FORMAT.format(new Date(System.currentTimeMillis())));
			searchForm.addCheckbox("All Time", GET_PARAM_ALL_TIME, "true", parameters.containsKey(GET_PARAM_ALL_TIME));
			searchForm.addSubmit("Search", ButtonType.SUCCESS);
			p.write(searchForm);

			if (parameters.containsKey(GET_PARAM_SEARCH_FOR) && !"".equals(parameters.get(GET_PARAM_SEARCH_FOR)[0])) {
				p.writeH2("Results:");

				HtmlTable resultTable = new HtmlTable();
				resultTable.startBody();

				String searchQuery = parameters.get(GET_PARAM_SEARCH_FOR)[0];

				boolean allTime = parameters.containsKey(GET_PARAM_ALL_TIME);
				Date start = null, end = null;
				if (!allTime) {
					try {
						start = Ban.DEFAULT_TIME_FORMAT.parse(parameters.get(GET_PARAM_SERVER_START)[0]);
						end = Ban.DEFAULT_TIME_FORMAT.parse(parameters.get(GET_PARAM_SERVER_START)[0]);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				for (String serverIdString : parameters.get(GET_PARAM_SERVER_IDS)) {
					Server serv = Server.getServerById(dbCon, Integer.parseInt(serverIdString));

					if (!s.getThisUser().hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_SEARCH_USER),
							serv.getAsset())) {
						p.writeText("no access!");
						continue;
					}

					List<Storage> storages = Storage.getAllStorageByServerId(dbCon, serv.getId());

					for (Storage stor : storages) {
						if (stor.getType() != StorageType.LOG) {
							// only log search here pls
							continue;
						}

						resultTable.addRow(Arrays.asList(serv.getName(), stor.getName()));

						try {
							AbstractStorage thisRemoteStor = AbstractStorage.getRemoteFromLocation(stor);

							List<StorageSearchEntry> results;
							if (allTime) {
								results = thisRemoteStor.searchForEntries(dbCon, searchQuery);
							} else {
								results = thisRemoteStor.searchForEntries(dbCon, searchQuery, start, end);
							}

							boolean canView = s.getThisUser().hasAccess(
									new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_VIEW_FILE),
									stor.getAsset(dbCon));

							for (StorageSearchEntry result : results) {

								HtmlTableRow resultRow = resultTable.new HtmlTableRow();

								if (canView) {
									URIBuilder builder = new URIBuilder(LOCATION);
									builder.addParameter(GET_PARAM, GET_PARAM_VALUE_VIEW);
									builder.addParameter(StorageIndex.DB_TABLE_COLUMN_NAME_STORAGE_ID,
											stor.getId() + "");
									builder.addParameter(StorageIndex.DB_TABLE_COLUMN_NAME_IDENTIFIER,
											result.getIdentifier());

									resultRow.writeLink(builder.toString(), result.getName(), true);
								} else {
									resultRow.writeText(result.getName());
								}

								resultRow.writeText(result.getData());

								resultTable.write(resultRow);
							}
						} catch (Exception e) {
							logger.log(Level.WARNING,
									"Error while searching through storage " + stor.getName() + ": " + e.getMessage());
							e.printStackTrace();
						}
					}
				}

				p.write(resultTable);
			}

			break;
		}

		case GET_PARAM_VALUE_SEARCH_USER: {
			HtmlPage p = new HtmlPage(dbCon, TITLE, null, null, null, s);
			b = p;

			break;
		}

		case GET_PARAM_VALUE_LIST: {
			HtmlPage p = new HtmlPage(dbCon, TITLE, null, null, null, s);
			b = p;

			HtmlTable ht = new HtmlTable();

			ht.addHeader(Arrays.asList("Id", "Key", "Name", "Server", "Type", "Local Indexed", "List"));

			ht.startBody();

			List<Storage> allStor = Storage.getAllStorage(dbCon);

			for (Storage stor : allStor) {

				if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_VIEW_FILE),
						stor.getAsset(dbCon))) {
					continue;
				}

				String serName = stor.getId() + "";

				try {
					serName = Server.getServerById(dbCon, stor.getServerId()).getName();
				} catch (Exception e) {
					logger.log(Level.WARNING, "Error while getting server name from server #" + stor.getServerId()
							+ ": " + e.getMessage());
				}

				HtmlTableRow row = ht.new HtmlTableRow();

				row.writeText(stor.getId() + "");
				row.writeText(stor.getKey());
				row.writeText(stor.getName());
				row.writeText(serName);
				row.writeText(stor.getType().name().toLowerCase());
				row.writeText(stor.isHasLocalIndex() + "");

				HtmlForm listF = new HtmlForm(LOCATION, Method.GET);
				listF.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_LIST);
				listF.addHiddenElement(Storage.DB_TABLE_COLUMN_NAME_ID, stor.getId() + "");
				listF.addSubmit("List Files", ButtonType.SUCCESS);
				row.write(listF);

				ht.write(row);
			}

			p.write(ht);

			if (parameters.containsKey(Storage.DB_TABLE_COLUMN_NAME_ID)) {

				Storage stor = Storage.getStorageById(dbCon,
						Integer.parseInt(parameters.get(Storage.DB_TABLE_COLUMN_NAME_ID)[0]));

				if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_VIEW_FILE),
						stor.getAsset(dbCon))) {
					p.writeText("no access");
					return p.finish().getBytes();
				}

				AbstractStorage abStor = AbstractStorage.getRemoteFromLocation(stor);

				HtmlTable fileList = new HtmlTable();

				fileList.addHeader(Arrays.asList("ID", "Name", "View"));

				fileList.startBody();

				for (ValueValuePair f : abStor.getFileList(dbCon)) {
					HtmlTableRow row = fileList.new HtmlTableRow();

					row.writeText(f.getId());
					row.writeText(f.getName());

					if (!abStor.forceDownload()) {
						try {
							row.writeLink(abStor.getViewLink(dbCon.getListener().getSiteUrl(), stor.getId(), f.getId()),
									"view", true);
						} catch (Exception e) {
							logger.log(Level.WARNING, "Error while building view link: " + e.getMessage());
						}
					} else {
						row.writeText("");
					}

					URIBuilder bu;
					try {
						bu = new URIBuilder(LOCATION);
						bu.addParameter(GET_PARAM, GET_PARAM_VALUE_DOWNLOAD);
						bu.addParameter(StorageIndex.DB_TABLE_COLUMN_NAME_STORAGE_ID, stor.getId() + "");
						bu.addParameter(StorageIndex.DB_TABLE_COLUMN_NAME_IDENTIFIER, f.getId());

						row.writeLink(bu.toString(), "download", true);
					} catch (Exception e) {
						logger.log(Level.WARNING, "Error while building download link: " + e.getMessage());
					}

					fileList.write(row);
				}

				p.write(fileList);
			}

			break;
		}

		case GET_PARAM_VALUE_VIEW: {
			HtmlPage p = new HtmlPage(dbCon, TITLE, null, null, null, s);
			b = p;

			int storageId = Integer.parseInt(parameters.get(StorageIndex.DB_TABLE_COLUMN_NAME_STORAGE_ID)[0]);
			String identifier = parameters.get(StorageIndex.DB_TABLE_COLUMN_NAME_IDENTIFIER)[0];

			Storage stor = Storage.getStorageById(dbCon, storageId);

			if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_VIEW_FILE),
					stor.getAsset(dbCon))) {
				return "no access".getBytes();
			}

			p.writeH2("Content: ");

			AbstractStorage.getRemoteFromLocation(stor).renderContent(p, identifier);

			break;
		}

		case GET_PARAM_VALUE_DOWNLOAD: {

			int storageId = Integer.parseInt(parameters.get(StorageIndex.DB_TABLE_COLUMN_NAME_STORAGE_ID)[0]);
			String identifier = parameters.get(StorageIndex.DB_TABLE_COLUMN_NAME_IDENTIFIER)[0];

			Storage stor = Storage.getStorageById(dbCon, storageId);

			if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_VIEW_FILE),
					stor.getAsset(dbCon))) {
				return "no access".getBytes();
			}

			s.getResponse().setContentType("application/octet-stream");
			s.getResponse().addHeader("Content-Disposition", "inline; filename=\"" + identifier + "\"");
			return AbstractStorage.getRemoteFromLocation(stor).getData(identifier);
		}

		default: {
			HtmlPage p = new HtmlPage(dbCon, TITLE, null, null, null, s);
			b = p;

			p.writeText("unknown do Param");
		}

		}

		return b.finish().getBytes();

	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> retMap = new HashMap<>();

		retMap.put(FUNCTION_NAME_SEARCH_USER, null);
		retMap.put(FUNCTION_NAME_SEARCH_LOGS, null);
		retMap.put(FUNCTION_NAME_VIEW_FILE, null);

		return retMap;
	}
}
