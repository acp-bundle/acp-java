package co.clai.module;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.json.JSONObject;

import co.clai.AcpSession;
import co.clai.access.AccessibleFunctionHelper;
import co.clai.access.CommunityAsset;
import co.clai.access.GeneralAsset;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Community;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlPage;
import co.clai.html.HtmlTable;

public class EditCommunity extends AbstractModule {

	public static final String FUNCTION_NAME_ADD_COMMUNITY = "addCommunity";
	public static final String FUNCTION_NAME_EDIT_FEATURES = "editFeatures";
	public static final String FUNCTION_NAME_EDIT_SETTINGS = "editSettings";

	public static final String LOCATION = "editCommunity";

	public EditCommunity(DatabaseConnector dbCon) {
		super(LOCATION, dbCon);
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {
		HtmlPage p = new HtmlPage(dbCon, "Edit Community", null, null, null, s);

		p.writeWithoutEscaping(HtmlPage.getMessage(parameters));

		if ((parameters.get("edit") == null) || (parameters.get("edit").length == 0)) {
			p.writeH1("Edit Communities");

			HtmlTable t = new HtmlTable();
			t.addHeader(Arrays.asList("id", "key", "name", "Edit"));
			t.startBody();

			List<Community> allCommunities = Community.getAllCommunity(dbCon);

			for (Community c : allCommunities) {
				if (s.getThisUser().hasAccess(getAccessibleHelper(), new CommunityAsset(c.getId()))) {

					HtmlTable.HtmlTableRow row = t.new HtmlTableRow();

					row.writeText(c.getId() + "");
					row.writeText(c.getKey());
					row.writeText(c.getName() + "");

					HtmlForm fb = new HtmlForm(LOCATION, HtmlForm.Method.GET);
					fb.addHiddenElement("edit", "true");
					fb.addHiddenElement(Community.DB_TABLE_COLUMN_NAME_ID, c.getId() + "");
					fb.addSubmit("Edit Community", HtmlForm.ButtonType.PRIMARY);
					row.write(fb);

					t.write(row);
				}
			}

			p.write(t);

			if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_COMMUNITY))) {

				HtmlForm addCommFr = new HtmlForm(LOCATION + "." + FUNCTION_NAME_ADD_COMMUNITY, HtmlForm.Method.POST);
				addCommFr.addTextElement("Key", Community.DB_TABLE_COLUMN_NAME_KEY, "");
				addCommFr.addTextElement("Name", Community.DB_TABLE_COLUMN_NAME_NAME, "");
				addCommFr.addSubmit("Add new Community", HtmlForm.ButtonType.SUCCESS);
				p.write(addCommFr);
			}

		} else {
			int editedCommunityId = Integer.parseInt(parameters.get(Community.DB_TABLE_COLUMN_NAME_ID)[0]);
			Community c = Community.getCommunityById(dbCon, editedCommunityId);
			if (s.getThisUser().hasAccess(getAccessibleHelper(), new GeneralAsset(c.getId(), c.getId()))) {
				p.writeH2("Edit Community " + c.getName());

				p.writeH3("Features:");

				HtmlForm featureFb = new HtmlForm(LOCATION + "." + FUNCTION_NAME_EDIT_FEATURES, HtmlForm.Method.POST);
				String formattedFeatures = c.getFeatures().toString(4);
				featureFb.addTextArea(Community.DB_TABLE_COLUMN_NAME_FEATURES, formattedFeatures, 10, 75);
				featureFb.addHiddenElement(Community.DB_TABLE_COLUMN_NAME_ID, c.getId() + "");

				if (s.getThisUser().hasAccess(
						new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_FEATURES),
						new GeneralAsset(c.getId(), c.getId()))) {
					featureFb.addSubmit("Change Features", HtmlForm.ButtonType.WARNING);
				}

				p.write(featureFb);
				p.writeH3("Settings:");

				HtmlForm settingsFb = new HtmlForm(LOCATION + "." + FUNCTION_NAME_EDIT_SETTINGS, HtmlForm.Method.POST);
				String formattedSettings = c.getSettings().toString(4);
				settingsFb.addTextArea(Community.DB_TABLE_COLUMN_NAME_SETTINGS, formattedSettings, 10, 75);
				settingsFb.addHiddenElement(Community.DB_TABLE_COLUMN_NAME_ID, c.getId() + "");

				if (s.getThisUser().hasAccess(
						new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_FEATURES),
						new GeneralAsset(c.getId(), c.getId()))) {
					settingsFb.addSubmit("Change Settings", HtmlForm.ButtonType.WARNING);
				}

				p.write(settingsFb);

			} else {
				p.writeText("access denied!");
				return p.finish().getBytes();
			}

		}

		return p.finish().getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {

		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> retMap = new HashMap<>();

		retMap.put(FUNCTION_NAME_ADD_COMMUNITY, this::addCommunity);
		retMap.put(FUNCTION_NAME_EDIT_FEATURES, this::editFeatures);
		retMap.put(FUNCTION_NAME_EDIT_SETTINGS, this::editSettings);

		return retMap;
	}

	protected FunctionResult addCommunity(AcpSession s, Map<String, String[]> parameters) {

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_COMMUNITY))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		String communityName = parameters.get(Community.DB_TABLE_COLUMN_NAME_NAME)[0];
		String keyName = parameters.get(Community.DB_TABLE_COLUMN_NAME_KEY)[0];
		Community.addNewCommunity(dbCon, keyName, communityName);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter("edit", "true");
		r.getBuilder().addParameter(Community.DB_TABLE_COLUMN_NAME_ID,
				Community.getCommunityByName(dbCon, communityName).getId() + "");

		return r;
	}

	protected FunctionResult editFeatures(AcpSession s, Map<String, String[]> parameters) {

		Community c = Community.getCommunityById(dbCon,
				Integer.parseInt(parameters.get(Community.DB_TABLE_COLUMN_NAME_ID)[0]));

		if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_FEATURES),
				new GeneralAsset(c.getId(), c.getId()))) {

			JSONObject jO = new JSONObject(parameters.get(Community.DB_TABLE_COLUMN_NAME_FEATURES)[0]);

			c.setFeatures(dbCon, jO.toString());

		} else {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter("edit", "true");
		r.getBuilder().addParameter(Community.DB_TABLE_COLUMN_NAME_ID, c.getId() + "");

		return r;
	}

	protected FunctionResult editSettings(AcpSession s, Map<String, String[]> parameters) {

		Community c = Community.getCommunityById(dbCon,
				Integer.parseInt(parameters.get(Community.DB_TABLE_COLUMN_NAME_ID)[0]));

		if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_SETTINGS),
				new GeneralAsset(c.getId(), c.getId()))) {

			JSONObject jO = new JSONObject(parameters.get(Community.DB_TABLE_COLUMN_NAME_SETTINGS)[0]);

			c.setSettings(dbCon, jO.toString());

		} else {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter("edit", "true");
		r.getBuilder().addParameter(Community.DB_TABLE_COLUMN_NAME_ID, c.getId() + "");

		return r;
	}

}
