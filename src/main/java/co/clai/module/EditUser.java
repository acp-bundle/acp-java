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
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Community;
import co.clai.db.model.User;
import co.clai.db.model.UserAccessFilter;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlPage;
import co.clai.html.HtmlTable;
import co.clai.util.IntStringPair;
import co.clai.util.ValueValuePair;

public class EditUser extends AbstractModule {

	public static final String LOCATION = "editUser";

	private static final String GET_PARAM = "edit";

	public static final String FUNCTION_NAME_CREATE_USER = "createUser";
	public static final String FUNCTION_NAME_CHANGE_USERNAME = "changeUsername";
	public static final String FUNCTION_NAME_SET_PASSWORD = "setPassword";
	public static final String FUNCTION_NAME_CHANGE_COMMUNITY_ID = "changeCommunityId";
	public static final String FUNCTION_NAME_DELETE_USER = "deleteUser";

	public EditUser(DatabaseConnector dbCon) {
		super(LOCATION, dbCon);
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {
		HtmlPage p = new HtmlPage(dbCon, "Edit User", null, null, null, s);

		p.writeWithoutEscaping(HtmlPage.getMessage(parameters));

		List<ValueValuePair> selectionCommunityValues = new ArrayList<>();
		if (s.getThisUser().getIsRoot()) {
			selectionCommunityValues.add(new IntStringPair(0, "Community Wide"));
		}
		for (Community c : Community.getAllCommunity(dbCon)) {
			if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_CREATE_USER),
					new CommunityAsset(c.getId()))) {
				selectionCommunityValues.add(new IntStringPair(c.getId(), c.getName()));
			}
		}

		if ((parameters.get(GET_PARAM) == null) || (parameters.get(GET_PARAM).length == 0)) {
			p.writeH1("Edit User");

			HtmlTable t = new HtmlTable();
			t.addHeader(Arrays.asList("id", "name", "Community", "isRoot", "Edit"));
			t.startBody();

			List<User> allUser = User.getAllLocalUser(dbCon);

			User thisUser = s.getThisUser();

			for (User u : allUser) {
				if (thisUser.hasAccess(getAccessibleHelper(), new CommunityAsset(u.getCommunityId()))) {

					HtmlTable.HtmlTableRow row = t.new HtmlTableRow();

					row.writeText(u.getId() + "");
					row.writeText(u.getUsername() + "");
					row.writeText(Community.getCommunityById(dbCon, u.getCommunityId()).getName());
					row.writeText(u.getIsRoot() + "");

					HtmlForm fb = new HtmlForm(LOCATION, HtmlForm.Method.GET);
					fb.addHiddenElement(GET_PARAM, "true");
					fb.addHiddenElement(User.DB_TABLE_COLUMN_NAME_ID, u.getId() + "");
					fb.addSubmit("Edit User", HtmlForm.ButtonType.WARNING);
					row.write(fb);

					HtmlForm editUAccessF = new HtmlForm(EditUserAccess.LOCATION, HtmlForm.Method.GET);
					editUAccessF.addHiddenElement(EditUserAccess.GET_PARAM, EditUserAccess.GET_PARAM_VALUE_USER_ACCESS);
					editUAccessF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID, u.getId() + "");
					editUAccessF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID, "0");
					editUAccessF.addSubmit("Edit Access", HtmlForm.ButtonType.WARNING);
					row.write(editUAccessF);

					t.write(row);
				}
			}

			p.write(t);

			if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_CREATE_USER))) {
				p.writeH1("Add new User");

				HtmlForm addCommFr = new HtmlForm(LOCATION + "." + FUNCTION_NAME_CREATE_USER, HtmlForm.Method.POST);
				addCommFr.addTextElement("Name", User.DB_TABLE_COLUMN_NAME_USERNAME, "");
				addCommFr.addTextElement("Password", User.DB_TABLE_COLUMN_NAME_PASSWORD, "");
				addCommFr.addSelectionDropdown("Community Id", User.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						selectionCommunityValues);
				addCommFr.addSubmit("Add new User", HtmlForm.ButtonType.SUCCESS);
				p.write(addCommFr);
			}

		} else {

			User u = User.getUserByLocationId(dbCon, 0,
					Integer.parseInt(parameters.get(User.DB_TABLE_COLUMN_NAME_ID)[0]));
			if (!s.getThisUser().hasAccess(getAccessibleHelper(), new CommunityAsset(u.getCommunityId()))) {
				return (p.finish() + "no access").getBytes();
			}

			p.writeH1("Edit User " + u.getUsername());

			p.writeHline();

			p.writeH2("Edit Username");
			HtmlForm editNameR = new HtmlForm(LOCATION + "." + FUNCTION_NAME_CHANGE_USERNAME, HtmlForm.Method.POST);
			editNameR.addTextElement("new Username", User.DB_TABLE_COLUMN_NAME_USERNAME, u.getUsername());
			editNameR.addHiddenElement(User.DB_TABLE_COLUMN_NAME_ID, u.getId() + "");
			editNameR.addSubmit("Change Username", HtmlForm.ButtonType.WARNING);
			p.write(editNameR);
			p.writeHline();

			p.writeH2("Set New Password");
			HtmlForm setNewPaswdR = new HtmlForm(LOCATION + "." + FUNCTION_NAME_SET_PASSWORD, HtmlForm.Method.POST);
			setNewPaswdR.addPasswordElement("New Password", User.DB_TABLE_COLUMN_NAME_PASSWORD);
			setNewPaswdR.addHiddenElement(User.DB_TABLE_COLUMN_NAME_ID, u.getId() + "");
			setNewPaswdR.addSubmit("Set new Password", HtmlForm.ButtonType.WARNING);
			p.write(setNewPaswdR);
			p.writeHline();

			p.writeH2("Change Community Id");
			HtmlForm changeCommunityIdR = new HtmlForm(LOCATION + "." + FUNCTION_NAME_CHANGE_COMMUNITY_ID,
					HtmlForm.Method.POST);
			changeCommunityIdR.addSelectionDropdown("new Community Id", User.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
					selectionCommunityValues, u.getCommunityId() + "");
			changeCommunityIdR.addHiddenElement(User.DB_TABLE_COLUMN_NAME_ID, u.getId() + "");
			changeCommunityIdR.addSubmit("Change Community", HtmlForm.ButtonType.WARNING);
			p.write(changeCommunityIdR);
			p.writeHline();

			p.writeH2("Delete User");
			HtmlForm deleteUserR = new HtmlForm(LOCATION + "." + FUNCTION_NAME_DELETE_USER, HtmlForm.Method.POST);
			deleteUserR.addHiddenElement(User.DB_TABLE_COLUMN_NAME_ID, u.getId() + "");
			deleteUserR.addSubmit("Delete User", HtmlForm.ButtonType.DANGER);
			p.write(deleteUserR);
		}

		return p.finish().getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> retMap = new HashMap<>();

		retMap.put(FUNCTION_NAME_CREATE_USER, this::createUser);
		retMap.put(FUNCTION_NAME_CHANGE_USERNAME, this::changeUsername);
		retMap.put(FUNCTION_NAME_SET_PASSWORD, this::setPassword);
		retMap.put(FUNCTION_NAME_CHANGE_COMMUNITY_ID, this::changeCommunityId);
		retMap.put(FUNCTION_NAME_DELETE_USER, this::deleteUser);

		return retMap;
	}

	private FunctionResult createUser(AcpSession s, Map<String, String[]> parameter) {

		String username = parameter.get(User.DB_TABLE_COLUMN_NAME_USERNAME)[0];
		String password = parameter.get(User.DB_TABLE_COLUMN_NAME_PASSWORD)[0];
		int communityId = Integer.parseInt(parameter.get(User.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_CREATE_USER),
				new CommunityAsset(communityId))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		User.addNewLocalUser(dbCon, username, password, communityId, false);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(User.DB_TABLE_COLUMN_NAME_ID,
				User.getUserByLocationName(dbCon, 0, username).getId() + "");
		r.getBuilder().addParameter(GET_PARAM, "true");

		return r;
	}

	private FunctionResult changeUsername(AcpSession s, Map<String, String[]> parameter) {

		int userId = Integer.parseInt(parameter.get(User.DB_TABLE_COLUMN_NAME_ID)[0]);
		String newUsername = parameter.get(User.DB_TABLE_COLUMN_NAME_USERNAME)[0];

		User u = User.getUserByLocationId(dbCon, 0, userId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_CHANGE_USERNAME),
				new CommunityAsset(u.getCommunityId()))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		u.setNewUsername(dbCon, newUsername);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(User.DB_TABLE_COLUMN_NAME_ID, u.getId() + "");
		r.getBuilder().addParameter(GET_PARAM, "true");

		return r;
	}

	private FunctionResult setPassword(AcpSession s, Map<String, String[]> parameter) {

		int userId = Integer.parseInt(parameter.get(User.DB_TABLE_COLUMN_NAME_ID)[0]);
		String newPassword = parameter.get(User.DB_TABLE_COLUMN_NAME_PASSWORD)[0];

		User u = User.getUserByLocationId(dbCon, 0, userId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_SET_PASSWORD),
				new CommunityAsset(u.getCommunityId()))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		u.setNewPassword(dbCon, newPassword);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(User.DB_TABLE_COLUMN_NAME_ID, u.getId() + "");
		r.getBuilder().addParameter(GET_PARAM, "true");

		return r;
	}

	private FunctionResult changeCommunityId(AcpSession s, Map<String, String[]> parameter) {

		int userId = Integer.parseInt(parameter.get(User.DB_TABLE_COLUMN_NAME_ID)[0]);
		int newCommId = Integer.parseInt(parameter.get(User.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);

		User u = User.getUserByLocationId(dbCon, 0, userId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_CHANGE_COMMUNITY_ID),
				new CommunityAsset(u.getCommunityId()))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_CHANGE_COMMUNITY_ID),
				new CommunityAsset(newCommId))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		u.setNewCommunityId(dbCon, newCommId);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(User.DB_TABLE_COLUMN_NAME_ID, u.getId() + "");
		r.getBuilder().addParameter(GET_PARAM, "true");

		return r;
	}

	private FunctionResult deleteUser(AcpSession s, Map<String, String[]> parameter) {

		int userId = Integer.parseInt(parameter.get(User.DB_TABLE_COLUMN_NAME_ID)[0]);

		User u = User.getUserByLocationId(dbCon, 0, userId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_CHANGE_COMMUNITY_ID),
				new CommunityAsset(u.getCommunityId()))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		u.deleteUser(dbCon);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		return r;
	}

}
