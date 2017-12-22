package co.clai.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import co.clai.AcpSession;
import co.clai.access.AccessibleFunctionHelper;
import co.clai.access.CommunityAsset;
import co.clai.access.GeneralAsset;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Community;
import co.clai.db.model.Location;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlPage;
import co.clai.html.HtmlTable;
import co.clai.remote.AbstractRemoteConnection;
import co.clai.util.IntStringPair;
import co.clai.util.StringStringPair;
import co.clai.util.ValueValuePair;

public class EditLocation extends AbstractModule {

	public static final String FUNCTION_NAME_ADD_LOCATION = "addLocation";
	public static final String FUNCTION_NAME_EDIT_LOCATION = "editLocation";
	public static final String FUNCTION_NAME_DELETE_LOCATION = "deleteLocation";

	public static final String LOCATION = "editLocation";

	private static final String GET_PARAM = "edit";

	public EditLocation(DatabaseConnector dbCon) {
		super(LOCATION, dbCon);
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {
		HtmlPage p = new HtmlPage(dbCon, "Edit Location", null, null, null, s);

		if ((parameters.get(GET_PARAM) == null) || (parameters.get(GET_PARAM).length == 0)) {

			p.writeH1("Edit Location");
			p.writeText(
					"A location is a Forum running on a different server. Currently IPS, Xenforo and general oauth2 is supported");

			HtmlTable ht = new HtmlTable();
			ht.addHeader(Arrays.asList("ID", "name", "Community", "Edit", "Delete"));
			ht.startBody();

			List<Location> allLocations = Location.getAllLocations(dbCon);

			for (Location l : allLocations) {

				if (s.getThisUser().hasAccess(getAccessibleHelper(), new GeneralAsset(l.getId(), l.getCommunityId()))) {

					HtmlTable.HtmlTableRow r = ht.new HtmlTableRow();
					r.writeText(l.getId() + "");
					r.writeText(l.getName());
					r.writeText(Community.getCommunityById(dbCon, l.getCommunityId()).getName());

					HtmlForm editForm = new HtmlForm(LOCATION, HtmlForm.Method.GET);
					editForm.addHiddenElement(GET_PARAM, "true");
					editForm.addHiddenElement(Location.DB_TABLE_COLUMN_NAME_ID, l.getId() + "");
					editForm.addSubmit("Edit", HtmlForm.ButtonType.PRIMARY);

					r.write(editForm);

					if (s.getThisUser().hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_DELETE_LOCATION),
							new GeneralAsset(l.getId(), l.getCommunityId()))) {

						HtmlForm deleteForm = new HtmlForm(LOCATION + "." + FUNCTION_NAME_DELETE_LOCATION,
								HtmlForm.Method.POST);
						deleteForm.addHiddenElement(Location.DB_TABLE_COLUMN_NAME_ID, l.getId() + "");
						deleteForm.addSubmit("Delete", HtmlForm.ButtonType.DANGER);

						r.write(deleteForm);
					}

					ht.write(r);
				}
			}

			p.write(ht);

			if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_LOCATION))) {

				List<ValueValuePair> selectionCommunityValues = new ArrayList<>();
				if (s.getThisUser().getIsRoot()) {
					selectionCommunityValues.add(new IntStringPair(0, "Community Wide"));
				}
				for (Community c : Community.getAllCommunity(dbCon)) {
					if (s.getThisUser().hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_LOCATION),
							new CommunityAsset(c.getId()))) {
						selectionCommunityValues.add(new IntStringPair(c.getId(), c.getName()));
					}
				}

				HtmlForm addForm = new HtmlForm(LOCATION + "." + FUNCTION_NAME_ADD_LOCATION, HtmlForm.Method.POST);
				addForm.addTextElement("Name", Location.DB_TABLE_COLUMN_NAME_NAME, "");
				addForm.addSelectionDropdown("Community ID", Location.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						selectionCommunityValues);
				List<ValueValuePair> typeSelector = new ArrayList<>();
				for (String t : AbstractRemoteConnection.getAllTypes()) {
					typeSelector.add(new StringStringPair(t, t));
				}
				addForm.addSelectionDropdown("Type", AbstractRemoteConnection.REMOTE_LOCATION_CONFIG_KEY_TYPE,
						typeSelector);
				addForm.addSubmit("Add new Location", HtmlForm.ButtonType.SUCCESS);

				p.write(addForm);
			}
		} else {

			Location l = Location.getLocationById(dbCon,
					Integer.parseInt(parameters.get(Location.DB_TABLE_COLUMN_NAME_ID)[0]));

			if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_LOCATION),
					new GeneralAsset(l.getId(), l.getCommunityId()))) {
				p.writeText("no access");
				return p.finish().getBytes();
			}

			p.writeH1("Edit Location " + l.getName());

			HtmlForm editF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_EDIT_LOCATION, HtmlForm.Method.POST);
			editF.addHiddenElement(Location.DB_TABLE_COLUMN_NAME_ID, l.getId() + "");
			editF.addTextArea(Location.DB_TABLE_COLUMN_NAME_CONFIG, l.getConfig().toString(4), 20, 120);
			editF.addSubmit("Change Settings", HtmlForm.ButtonType.WARNING);

			p.write(editF);
		}

		return p.finish().getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> retMap = new HashMap<>();

		retMap.put(FUNCTION_NAME_ADD_LOCATION, this::addLocation);
		retMap.put(FUNCTION_NAME_EDIT_LOCATION, this::editLocation);
		retMap.put(FUNCTION_NAME_DELETE_LOCATION, this::deleteLocation);

		return retMap;
	}

	private FunctionResult addLocation(AcpSession s, Map<String, String[]> parameter) {

		String thisName = parameter.get(Location.DB_TABLE_COLUMN_NAME_NAME)[0];
		int communityId = Integer.parseInt(parameter.get(Location.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_LOCATION),
				new CommunityAsset(communityId))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		String defData = AbstractRemoteConnection
				.getDefaultConfig(parameter.get(AbstractRemoteConnection.REMOTE_LOCATION_CONFIG_KEY_TYPE)[0]);

		Location.addNewLocation(dbCon, thisName, communityId, defData);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);
		r.getBuilder().addParameter(GET_PARAM, "true");
		r.getBuilder().addParameter(Location.DB_TABLE_COLUMN_NAME_ID,
				Location.getLocationByName(dbCon, thisName).getId() + "");

		return r;
	}

	private FunctionResult editLocation(AcpSession s, Map<String, String[]> parameter) {

		int locationId = Integer.parseInt(parameter.get(Location.DB_TABLE_COLUMN_NAME_ID)[0]);

		Location l = Location.getLocationById(dbCon, locationId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_LOCATION),
				new GeneralAsset(l.getId(), l.getCommunityId()))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		l.changeConfig(dbCon, parameter.get(Location.DB_TABLE_COLUMN_NAME_CONFIG)[0]);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(GET_PARAM, "true");
		r.getBuilder().addParameter(Location.DB_TABLE_COLUMN_NAME_ID, l.getId() + "");

		return r;
	}

	private FunctionResult deleteLocation(AcpSession s, Map<String, String[]> parameter) {

		int locationId = Integer.parseInt(parameter.get(Location.DB_TABLE_COLUMN_NAME_ID)[0]);

		Location l = Location.getLocationById(dbCon, locationId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_DELETE_LOCATION),
				new GeneralAsset(l.getId(), l.getCommunityId()))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		l.delete(dbCon);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		return r;
	}

}
