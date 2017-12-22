package co.clai.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Level;

import org.json.JSONObject;

import co.clai.AcpSession;
import co.clai.access.AccessibleFunctionHelper;
import co.clai.access.GeneralAsset;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Server;
import co.clai.db.model.Storage;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlForm.ButtonType;
import co.clai.html.HtmlPage;
import co.clai.html.HtmlTable;
import co.clai.module.FunctionResult.Status;
import co.clai.storage.StorageType;
import co.clai.util.IntStringPair;
import co.clai.util.StringStringPair;
import co.clai.util.ValueValuePair;

public class EditStorage extends AbstractModule {

	public static final String FUNCTION_NAME_ADD_STORAGE = "addStorage";
	public static final String FUNCTION_NAME_EDIT_STORAGE = "editStorage";
	public static final String FUNCTION_NAME_EDIT_STORAGE_CONFIG = "editStorageConfig";
	public static final String FUNCTION_NAME_DELETE_STORAGE = "deleteStorage";

	public static final String LOCATION = "editStorage";

	private static final String GET_PARAM = "edit";

	public EditStorage(DatabaseConnector dbCon) {
		super(LOCATION, dbCon);
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {
		HtmlPage p = new HtmlPage(dbCon, "Edit Storage", null, null, null, s);

		if ((parameters.get(GET_PARAM) == null) || (parameters.get(GET_PARAM).length == 0)) {

			p.writeH1("Edit Storage");
			HtmlTable ht = new HtmlTable();
			ht.addHeader(Arrays.asList("ID", "key", "Name", "Server", "type", "indexed", "Edit", "Delete"));
			ht.startBody();

			List<Storage> allStorages = Storage.getAllStorage(dbCon);

			for (Storage stor : allStorages) {

				if (s.getThisUser().hasAccess(getAccessibleHelper(), stor.getAsset(dbCon))) {

					HtmlTable.HtmlTableRow r = ht.new HtmlTableRow();
					r.writeText(stor.getId() + "");
					r.writeText(stor.getKey());
					r.writeText(stor.getName());

					Server server = null;
					try {
						server = Server.getServerById(dbCon, stor.getServerId());
					} catch (Exception e) {
						logger.log(Level.WARNING,
								"storage with ID " + stor.getId() + " failed to get Server name: " + e.getMessage());
					}

					r.writeText(server == null ? stor.getServerId() + "" : server.getName());

					r.writeText(stor.getType().name().toLowerCase());
					r.writeText(stor.isHasLocalIndex() ? "x" : "");

					if (s.getThisUser().hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_STORAGE_CONFIG),
							stor.getAsset(dbCon))) {
						HtmlForm storEditForm = new HtmlForm(LOCATION, HtmlForm.Method.GET);
						storEditForm.addHiddenElement(GET_PARAM, FUNCTION_NAME_EDIT_STORAGE_CONFIG);
						storEditForm.addHiddenElement(Storage.DB_TABLE_COLUMN_NAME_ID, stor.getId() + "");
						storEditForm.addSubmit("Edit", ButtonType.WARNING);
						r.write(storEditForm);
					} else {
						r.writeText("");
					}

					if (s.getThisUser().hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_DELETE_STORAGE),
							stor.getAsset(dbCon))) {
						HtmlForm storEditForm = new HtmlForm(LOCATION + "." + FUNCTION_NAME_DELETE_STORAGE,
								HtmlForm.Method.POST);
						storEditForm.addHiddenElement(Storage.DB_TABLE_COLUMN_NAME_ID, stor.getId() + "");
						storEditForm.addSubmit("Delete", ButtonType.DANGER);
						r.write(storEditForm);
					}

					ht.write(r);
				}
			}

			p.write(ht);

			if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_STORAGE))) {

				p.writeH2("Add new Storage:");
				List<ValueValuePair> selectionServerValues = new ArrayList<>();
				for (Server ser : Server.getAllServer(dbCon)) {
					if (s.getThisUser().hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_STORAGE), ser.getAsset())) {
						selectionServerValues.add(new IntStringPair(ser.getId(), ser.getName()));
					}
				}

				HtmlForm addForm = new HtmlForm(LOCATION + "." + FUNCTION_NAME_ADD_STORAGE, HtmlForm.Method.POST);
				addForm.addTextElement("Key", Storage.DB_TABLE_COLUMN_NAME_KEY, "");
				addForm.addTextElement("Name", Storage.DB_TABLE_COLUMN_NAME_NAME, "");
				addForm.addSelectionDropdown("Server", Storage.DB_TABLE_COLUMN_NAME_SERVER_ID, selectionServerValues);
				List<ValueValuePair> typeSelector = new ArrayList<>();
				for (StorageType t : StorageType.values()) {
					typeSelector.add(new StringStringPair(t.name().toLowerCase(), t.name().toLowerCase()));
				}
				addForm.addSelectionDropdown("Type", Storage.DB_TABLE_COLUMN_NAME_TYPE, typeSelector);
				addForm.addSubmit("Add new Storage", HtmlForm.ButtonType.SUCCESS);

				p.write(addForm);
			}
		} else {

			Storage stor = Storage.getStorageById(dbCon,
					Integer.parseInt(parameters.get(Storage.DB_TABLE_COLUMN_NAME_ID)[0]));

			if (!s.getThisUser().hasAccess(
					new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_STORAGE_CONFIG),
					new GeneralAsset(stor.getId(), stor.getServer(dbCon).getCommunityId()))) {
				p.writeText("no access");
				return p.finish().getBytes();
			}

			p.writeH1("Edit Storage " + stor.getName());

			p.writeH2("Edit general info:");

			HtmlForm editF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_EDIT_STORAGE, HtmlForm.Method.POST);

			List<ValueValuePair> selectionServerValues = new ArrayList<>();
			for (Server ser : Server.getAllServer(dbCon)) {
				if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_STORAGE),
						ser.getAsset())) {
					selectionServerValues.add(new IntStringPair(ser.getId(), ser.getName()));
				}
			}

			editF.addTextElement("Key", Storage.DB_TABLE_COLUMN_NAME_KEY, stor.getKey());
			editF.addTextElement("Name", Storage.DB_TABLE_COLUMN_NAME_NAME, stor.getName());
			editF.addSelectionDropdown("Server", Storage.DB_TABLE_COLUMN_NAME_SERVER_ID, selectionServerValues,
					stor.getServerId() + "");
			editF.addCheckbox("Has Local Index", Storage.DB_TABLE_COLUMN_NAME_HAS_LOCAL_INDEX, "true",
					stor.isHasLocalIndex());
			editF.addHiddenElement(Storage.DB_TABLE_COLUMN_NAME_ID, stor.getId() + "");
			editF.addSubmit("Update Values", ButtonType.PRIMARY);

			p.write(editF);

			p.writeH2("Edit config:");
			HtmlForm editConfF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_EDIT_STORAGE_CONFIG, HtmlForm.Method.POST);

			editConfF.addTextArea(Storage.DB_TABLE_COLUMN_NAME_CONFIG, stor.getConfig().toString(4), 20, 120);
			editConfF.addHiddenElement(Storage.DB_TABLE_COLUMN_NAME_ID, stor.getId() + "");
			editConfF.addSubmit("Update Config", ButtonType.PRIMARY);

			p.write(editConfF);

		}

		return p.finish().getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> retMap = new HashMap<>();

		retMap.put(FUNCTION_NAME_ADD_STORAGE, this::addStorage);
		retMap.put(FUNCTION_NAME_EDIT_STORAGE, this::editStorage);
		retMap.put(FUNCTION_NAME_EDIT_STORAGE_CONFIG, this::editStorageConfig);
		retMap.put(FUNCTION_NAME_DELETE_STORAGE, this::deleteStorage);

		return retMap;
	}

	private FunctionResult addStorage(AcpSession s, Map<String, String[]> parameter) {

		String key = parameter.get(Storage.DB_TABLE_COLUMN_NAME_KEY)[0];
		String name1 = parameter.get(Storage.DB_TABLE_COLUMN_NAME_NAME)[0];
		int serverId = Integer.parseInt(parameter.get(Storage.DB_TABLE_COLUMN_NAME_SERVER_ID)[0]);
		String typeS = parameter.get(Storage.DB_TABLE_COLUMN_NAME_TYPE)[0];
		StorageType type = StorageType.valueOf(typeS.toUpperCase());

		Server serv = Server.getServerById(dbCon, serverId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_STORAGE),
				serv.getAsset())) {
			return new FunctionResult(Status.NO_ACCESS, LOCATION);
		}

		Storage.addNewStorage(dbCon, key, name1, serverId, type, new JSONObject());

		FunctionResult fR = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		fR.getBuilder().addParameter(GET_PARAM, FUNCTION_NAME_EDIT_STORAGE_CONFIG);
		fR.getBuilder().addParameter(Storage.DB_TABLE_COLUMN_NAME_ID, Storage.getStorageByKey(dbCon, key).getId() + "");

		return fR;
	}

	private FunctionResult editStorage(AcpSession s, Map<String, String[]> parameter) {

		int StorageId = Integer.parseInt(parameter.get(Storage.DB_TABLE_COLUMN_NAME_ID)[0]);

		Storage stor = Storage.getStorageById(dbCon, StorageId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_STORAGE_CONFIG),
				new GeneralAsset(stor.getId(), stor.getServer(dbCon).getCommunityId()))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		stor.edit(dbCon, parameter.get(Storage.DB_TABLE_COLUMN_NAME_KEY)[0],
				parameter.get(Storage.DB_TABLE_COLUMN_NAME_NAME)[0],
				Integer.parseInt(parameter.get(Storage.DB_TABLE_COLUMN_NAME_SERVER_ID)[0]),
				parameter.containsKey(Storage.DB_TABLE_COLUMN_NAME_HAS_LOCAL_INDEX));

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(GET_PARAM, "true");
		r.getBuilder().addParameter(Storage.DB_TABLE_COLUMN_NAME_ID, stor.getId() + "");

		return r;
	}

	private FunctionResult editStorageConfig(AcpSession s, Map<String, String[]> parameter) {

		int StorageId = Integer.parseInt(parameter.get(Storage.DB_TABLE_COLUMN_NAME_ID)[0]);

		Storage stor = Storage.getStorageById(dbCon, StorageId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_STORAGE_CONFIG),
				new GeneralAsset(stor.getId(), stor.getServer(dbCon).getCommunityId()))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		stor.changeConfig(dbCon, parameter.get(Storage.DB_TABLE_COLUMN_NAME_CONFIG)[0]);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(GET_PARAM, "true");
		r.getBuilder().addParameter(Storage.DB_TABLE_COLUMN_NAME_ID, stor.getId() + "");

		return r;
	}

	private FunctionResult deleteStorage(AcpSession s, Map<String, String[]> parameter) {

		int StorageId = Integer.parseInt(parameter.get(Storage.DB_TABLE_COLUMN_NAME_ID)[0]);

		Storage stor = Storage.getStorageById(dbCon, StorageId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_DELETE_STORAGE),
				new GeneralAsset(stor.getId(), stor.getServer(dbCon).getCommunityId()))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		stor.delete(dbCon);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		return r;
	}

}
