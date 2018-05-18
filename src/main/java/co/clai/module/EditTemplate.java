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
import co.clai.db.model.Template;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlPage;
import co.clai.html.HtmlTable;
import co.clai.util.IntStringPair;
import co.clai.util.ValueValuePair;

public class EditTemplate extends AbstractModule {

	public static final String LOCATION = "editTemplate";
	public static final String TITLE = "Edit Templates";

	public static final String FUNCTION_NAME_ADD_TEMPLATE = "addTemplate";
	public static final String FUNCTION_NAME_COPY_TEMPLATE = "copyTemplate";
	public static final String FUNCTION_NAME_EDIT_TEMPLATE = "editTemplate";
	public static final String FUNCTION_NAME_DELETE_TEMPLATE = "deleteTemplate";
	public static final String FUNCTION_NAME_UPDATE_TEMPLATE = "updateTemplate";

	private static final String GET_PARAM = "edit";

	public EditTemplate(DatabaseConnector dbCon) {
		super(LOCATION, dbCon);
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {
		HtmlPage p = new HtmlPage(dbCon, TITLE, null, null, null, s);

		p.writeH1("Edit Templates");

		HtmlTable ht = new HtmlTable();
		ht.addHeader(Arrays.asList("ID", "key", "name", "community", "edit", "delete", "update"));
		ht.startBody();

		List<Template> allTemplates = Template.getAllTemplates(dbCon);

		for (Template t : allTemplates) {

			if (s.getThisUser().hasAccess(getAccessibleHelper(), new GeneralAsset(t.getId(), t.getCommunityId()))) {

				HtmlTable.HtmlTableRow r = ht.new HtmlTableRow();
				r.writeText(t.getId() + "");
				r.writeText(t.getKey());
				r.writeText(t.getName());

				Community thisC = Community.getCommunityById(dbCon, t.getCommunityId());
				r.writeText(thisC == null ? "Unknown Community" : thisC.getName());

				HtmlForm editForm = new HtmlForm(LOCATION, HtmlForm.Method.GET);
				editForm.addHiddenElement(GET_PARAM, "true");
				editForm.addHiddenElement(Template.DB_TABLE_COLUMN_NAME_ID, t.getId() + "");
				editForm.addSubmit("Edit", HtmlForm.ButtonType.PRIMARY);

				r.write(editForm);

				if (s.getThisUser().hasAccess(
						new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_DELETE_TEMPLATE), t.getAsset())) {

					HtmlForm deleteForm = new HtmlForm(LOCATION + "." + FUNCTION_NAME_DELETE_TEMPLATE,
							HtmlForm.Method.POST);
					deleteForm.addHiddenElement(Template.DB_TABLE_COLUMN_NAME_ID, t.getId() + "");
					deleteForm.addSubmit("Delete", HtmlForm.ButtonType.DANGER);

					r.write(deleteForm);
				}

				ht.write(r);
			}
		}

		p.write(ht);

		if ((parameters.get(GET_PARAM) == null) || (parameters.get(GET_PARAM).length == 0)) {
			if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_TEMPLATE))) {

				p.writeH2("Add new Template:");

				List<ValueValuePair> selectionCommunityValues = new ArrayList<>();
				if (s.getThisUser().getIsRoot()) {
					selectionCommunityValues.add(new IntStringPair(0, "Community Wide"));
				}
				for (Community c : Community.getAllCommunity(dbCon)) {
					if (s.getThisUser().hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_TEMPLATE),
							new CommunityAsset(c.getId()))) {
						selectionCommunityValues.add(new IntStringPair(c.getId(), c.getName()));
					}
				}

				HtmlForm addForm = new HtmlForm(LOCATION + "." + FUNCTION_NAME_ADD_TEMPLATE, HtmlForm.Method.POST);
				addForm.addTextElement("Name", Template.DB_TABLE_COLUMN_NAME_NAME, "");
				addForm.addTextElement("Key", Template.DB_TABLE_COLUMN_NAME_KEY, "");
				addForm.addSelectionDropdown("Community ID", Template.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						selectionCommunityValues);
				addForm.addSubmit("Add new Template", HtmlForm.ButtonType.SUCCESS);

				p.write(addForm);
			}

			if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_COPY_TEMPLATE))) {

				p.writeH2("Copy Existing Template:");

				List<ValueValuePair> selectionCommunityValues = new ArrayList<>();
				if (s.getThisUser().getIsRoot()) {
					selectionCommunityValues.add(new IntStringPair(0, "Community Wide"));
				}
				for (Community c : Community.getAllCommunity(dbCon)) {
					if (s.getThisUser().hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_TEMPLATE),
							new CommunityAsset(c.getId()))) {
						selectionCommunityValues.add(new IntStringPair(c.getId(), c.getName()));
					}
				}

				List<ValueValuePair> selectionTemplateValues = new ArrayList<>();
				for (Template t : Template.getAllTemplates(dbCon)) {
					if (t.getCommunityId() == 0) {
						selectionTemplateValues.add(new IntStringPair(t.getId(), t.getName()));
					} else if (s.getThisUser().hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_COPY_TEMPLATE), t.getAsset())) {
						selectionTemplateValues.add(new IntStringPair(t.getId(), t.getName()));
					}
				}

				HtmlForm addForm = new HtmlForm(LOCATION + "." + FUNCTION_NAME_COPY_TEMPLATE, HtmlForm.Method.POST);
				addForm.addTextElement("Name", Template.DB_TABLE_COLUMN_NAME_NAME, "");
				addForm.addTextElement("Key", Template.DB_TABLE_COLUMN_NAME_KEY, "");
				addForm.addSelectionDropdown("Community ID", Template.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						selectionCommunityValues);
				addForm.addSelectionDropdown("Copy from Template", Template.DB_TABLE_COLUMN_NAME_ID,
						selectionTemplateValues);
				addForm.addSubmit("Add new Template", HtmlForm.ButtonType.SUCCESS);

				p.write(addForm);
			}
		} else {

			p.writeHline();

			Template t = Template.getTemplateById(dbCon,
					Integer.parseInt(parameters.get(Template.DB_TABLE_COLUMN_NAME_ID)[0]));

			if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_TEMPLATE),
					t.getAsset())) {
				p.writeText("no access");
				return p.finish().getBytes();
			}

			p.writeH1("Edit Template " + t.getName());

			HtmlForm editF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_EDIT_TEMPLATE, HtmlForm.Method.POST);
			editF.addHiddenElement(Template.DB_TABLE_COLUMN_NAME_ID, t.getId() + "");
			editF.addTextArea(Template.DB_TABLE_COLUMN_NAME_DATA, new String(t.getData()), 20, 120);
			editF.addSubmit("Change Template", HtmlForm.ButtonType.WARNING);

			p.write(editF);
		}

		return p.finish().getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> retMap = new HashMap<>();

		retMap.put(FUNCTION_NAME_ADD_TEMPLATE, this::addTemplate);
		retMap.put(FUNCTION_NAME_COPY_TEMPLATE, this::copyTemplate);
		retMap.put(FUNCTION_NAME_EDIT_TEMPLATE, this::editTemplate);
		retMap.put(FUNCTION_NAME_DELETE_TEMPLATE, this::deleteTemplate);
		retMap.put(FUNCTION_NAME_UPDATE_TEMPLATE, this::updateTemplate);

		return retMap;
	}

	private FunctionResult addTemplate(AcpSession s, Map<String, String[]> parameters) {

		String templateName = parameters.get(Template.DB_TABLE_COLUMN_NAME_NAME)[0];
		String key = parameters.get(Template.DB_TABLE_COLUMN_NAME_KEY)[0];
		int communityId = Integer.parseInt(parameters.get(Template.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_TEMPLATE),
				new CommunityAsset(communityId))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		Template.addNewTemplate(dbCon, templateName, key, communityId);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(GET_PARAM, "true");
		r.getBuilder().addParameter(Template.DB_TABLE_COLUMN_NAME_ID,
				Template.getTemplateByKey(dbCon, key).getId() + "");

		return r;
	}

	private FunctionResult copyTemplate(AcpSession s, Map<String, String[]> parameters) {

		int id = Integer.parseInt(parameters.get(Template.DB_TABLE_COLUMN_NAME_ID)[0]);

		String templateName = parameters.get(Template.DB_TABLE_COLUMN_NAME_NAME)[0];
		String key = parameters.get(Template.DB_TABLE_COLUMN_NAME_KEY)[0];
		int communityId = Integer.parseInt(parameters.get(Template.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);

		Template t = Template.getTemplateById(dbCon, id);

		if ((t.getCommunityId() != 0) && (!s.getThisUser()
				.hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_TEMPLATE), t.getAsset()))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_TEMPLATE),
				new CommunityAsset(communityId))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		byte[] data = t.getData();

		Template.addNewTemplate(dbCon, templateName, key, communityId);

		Template newTemplate = Template.getTemplateByKey(dbCon, key);

		newTemplate.edit(dbCon, new String(data));

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(GET_PARAM, "true");
		r.getBuilder().addParameter(Template.DB_TABLE_COLUMN_NAME_ID, newTemplate.getId() + "");

		return r;
	}

	private FunctionResult editTemplate(AcpSession s, Map<String, String[]> parameters) {

		int id = Integer.parseInt(parameters.get(Template.DB_TABLE_COLUMN_NAME_ID)[0]);

		Template t = Template.getTemplateById(dbCon, id);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_TEMPLATE),
				t.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		String data = parameters.get(Template.DB_TABLE_COLUMN_NAME_DATA)[0];

		t.edit(dbCon, data);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(GET_PARAM, "true");
		r.getBuilder().addParameter(Template.DB_TABLE_COLUMN_NAME_ID, id + "");

		return r;
	}

	private FunctionResult deleteTemplate(AcpSession s, Map<String, String[]> parameters) {

		int id = Integer.parseInt(parameters.get(Template.DB_TABLE_COLUMN_NAME_ID)[0]);

		Template t = Template.getTemplateById(dbCon, id);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_TEMPLATE),
				t.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		t.delete(dbCon);

		return new FunctionResult(FunctionResult.Status.OK, LOCATION);
	}

	private FunctionResult updateTemplate(AcpSession s, Map<String, String[]> parameters) {

		int id = Integer.parseInt(parameters.get(Template.DB_TABLE_COLUMN_NAME_ID)[0]);

		Template t = Template.getTemplateById(dbCon, id);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_UPDATE_TEMPLATE),
				t.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		t.updateTemplate(dbCon);

		return new FunctionResult(FunctionResult.Status.OK, LOCATION);
	}

}