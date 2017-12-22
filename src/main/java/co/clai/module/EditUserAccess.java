package co.clai.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import co.clai.AcpSession;
import co.clai.access.AccessFilter;
import co.clai.access.AccessibleFunctionHelper;
import co.clai.access.AccessibleModuleHelper;
import co.clai.access.CommunityAsset;
import co.clai.access.GeneralAsset;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Community;
import co.clai.db.model.Game;
import co.clai.db.model.Location;
import co.clai.db.model.User;
import co.clai.db.model.UserAccessFilter;
import co.clai.db.model.UserGroupAccessFilter;
import co.clai.html.GenericBuffer;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlPage;
import co.clai.html.HtmlTable;
import co.clai.remote.AbstractRemoteConnection;
import co.clai.util.IntStringPair;
import co.clai.util.StringStringPair;
import co.clai.util.ValueValuePair;

public class EditUserAccess extends AbstractModule {

	public static final String LOCATION = "editUserAccess";

	public static final String GET_PARAM = "edit";
	public static final String GET_PARAM_VALUE_USER_ACCESS = "user_access";
	public static final String GET_PARAM_VALUE_USER_ACCESS_FILTER = "userAccessFilter";
	public static final String GET_PARAM_VALUE_GROUP_ACCESS = "group_access";
	public static final String GET_PARAM_VALUE_GROUP_ACCESS_FILTER = "groupAccessFilter";

	public static final String GET_PARAM_VALUE_LIST_USER_ACCESS_FILTER_ALL = "listAllUserAccessFilter";
	public static final String GET_PARAM_VALUE_LIST_GROUP_ACCESS_FILTER_ALL = "listAllGroupAccessFilter";

	public static final String FUNCTION_NAME_CREATE_USER_ACCESS_FILTER = "createUserAccessFilter";
	public static final String FUNCTION_NAME_CREATE_GROUP_ACCESS_FILTER = "createGroupAccessFilter";

	public static final String FUNCTION_NAME_CHANGE_USER_ACCESS_FILTER = "changeUserAccessFilter";
	public static final String FUNCTION_NAME_CHANGE_GROUP_ACCESS_FILTER = "changeGroupAccessFilter";

	public static final String FUNCTION_NAME_DELETE_USER_ACCESS_FILTER = "deleteUserAccessFilter";
	public static final String FUNCTION_NAME_DELETE_GROUP_ACCESS_FILTER = "deleteGroupAccessFilter";

	public EditUserAccess(DatabaseConnector dbCon) {
		super(LOCATION, dbCon);
	}

	public final static Set<String> ignorePathList = loadIgnorePaths();

	private static Set<String> loadIgnorePaths() {
		Set<String> retSet = new HashSet<>();
		retSet.add("settings.changeOwnPassword");
		retSet.add("index.logout");
		retSet.add("index.login");
		retSet.add("oauth2.callback");
		retSet.add("oauth2.login");

		return retSet;
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {
		HtmlPage p = new HtmlPage(dbCon, "Edit User Access", null, null, null, s);

		p.writeWithoutEscaping(HtmlPage.getMessage(parameters));

		List<ValueValuePair> gameList = new ArrayList<>();
		gameList.add(new IntStringPair(0, "All Games"));
		gameList.addAll(Game.getGameListSelection(dbCon));

		List<ValueValuePair> selectionPathValues = new ArrayList<>();
		Set<String> alreadyAdded = new HashSet<>();
		for (String pathname : ModuleUtil.getFunctionList()) {
			if (!ignorePathList.contains(pathname)) {

				AccessFilter newAF = new AccessFilter(pathname, 0, 0, 0);

				if (newAF.getFunction() == null) {
					if (!s.getThisUser().hasAccess(new AccessibleModuleHelper(newAF.getModule()))) {
						continue;
					}
				} else {
					if (!s.getThisUser()
							.hasAccess(new AccessibleFunctionHelper(newAF.getModule(), newAF.getFunction()))) {
						continue;
					}
				}

				if (pathname.contains(".")) {
					// Adding wildcard access, only one time please!
					final String moduleName = pathname.split("\\.")[0];
					String firstModuleWithStar = moduleName + ".*";
					if (alreadyAdded.add(firstModuleWithStar)) {
						selectionPathValues.add(new StringStringPair(moduleName, moduleName));
						selectionPathValues.add(new StringStringPair(firstModuleWithStar, firstModuleWithStar));
					}
				}

				selectionPathValues.add(new StringStringPair(pathname, pathname));
			}
		}

		List<ValueValuePair> selectionCommunityValues = new ArrayList<>();
		if (s.getThisUser().getIsRoot()) {
			selectionCommunityValues.add(new IntStringPair(0, "Community Wide"));
		}
		for (Community c : Community.getAllCommunity(dbCon)) {
			if (s.getThisUser().hasAccess(
					new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_CHANGE_USER_ACCESS_FILTER),
					new CommunityAsset(c.getId()))) {
				selectionCommunityValues.add(new IntStringPair(c.getId(), c.getName()));
			}
		}

		if ((parameters.get(GET_PARAM) == null) || (parameters.get(GET_PARAM).length == 0)) {

			p.writeH1("Edit User Access");

			p.writeText("Edit a User Access by using his location and id:");
			HtmlForm userAccessForm = new HtmlForm(LOCATION, HtmlForm.Method.GET);
			List<Location> locations = Location.getAllLocations(dbCon);
			List<ValueValuePair> selectionValues = new ArrayList<>();
			selectionValues.add(new IntStringPair(0, "Local"));
			for (Location l : locations) {
				if (s.getThisUser().hasAccess(getAccessibleHelper(), new GeneralAsset(l.getId(), l.getCommunityId()))) {
					selectionValues.add(new IntStringPair(l.getId(), l.getName()));
				}
			}
			userAccessForm.addSelectionDropdown("Location", UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID,
					selectionValues);
			userAccessForm.addTextElement("User id", UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID, "");
			userAccessForm.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_USER_ACCESS);
			userAccessForm.addSubmit("Edit this Users AccessFilters", HtmlForm.ButtonType.WARNING);
			p.write(userAccessForm);

			p.writeH2("Edit Group Access");
			HtmlForm groupAccessForm = new HtmlForm(LOCATION, HtmlForm.Method.GET);
			groupAccessForm.addSelectionDropdown("Location", UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID,
					selectionValues);
			groupAccessForm.addTextElement("Group id", UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_USER_GROUP_ID, "");
			groupAccessForm.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_GROUP_ACCESS);
			groupAccessForm.addSubmit("Edit this Groups AccessFilters", HtmlForm.ButtonType.WARNING);
			p.write(groupAccessForm);

			if (s.getThisUser().getIsRoot()) {

				p.writeH2("List all Access Filter:");

				HtmlForm listAllUserF = new HtmlForm(LOCATION, HtmlForm.Method.GET);
				listAllUserF.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_LIST_USER_ACCESS_FILTER_ALL);
				listAllUserF.addSubmit("Show all user access filter", HtmlForm.ButtonType.WARNING);
				p.write(listAllUserF);

				HtmlForm listAllGroupF = new HtmlForm(LOCATION, HtmlForm.Method.GET);
				listAllGroupF.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_LIST_GROUP_ACCESS_FILTER_ALL);
				listAllGroupF.addSubmit("Show all group access filter", HtmlForm.ButtonType.WARNING);
				p.write(listAllGroupF);
			}
		} else {

			GenericBuffer appendExtra = null;

			switch (parameters.get(GET_PARAM)[0]) {

			case (GET_PARAM_VALUE_USER_ACCESS_FILTER): {
				appendExtra = new GenericBuffer("<h2>Edit Filter:</h2>");

				HtmlForm createF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_CHANGE_USER_ACCESS_FILTER,
						HtmlForm.Method.POST);
				createF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID,
						parameters.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID)[0]);

				UserAccessFilter f = UserAccessFilter.getFilterById(dbCon,
						Integer.parseInt(parameters.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID)[0]));

				createF.addSelectionDropdown("Path", UserAccessFilter.DB_TABLE_COLUMN_NAME_PATH, selectionPathValues,
						f.getPath());

				createF.addSelectionDropdown("Community", UserAccessFilter.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						selectionCommunityValues, f.getCommunityId() + "");
				createF.addSelectionDropdown("Game", UserAccessFilter.DB_TABLE_COLUMN_NAME_GAME_ID, gameList,
						f.getGameId() + "");
				createF.addTextElement("Asset", UserAccessFilter.DB_TABLE_COLUMN_NAME_ASSET_ID, f.getAssetId() + "");

				createF.addSubmit("Edit User Access Filter", HtmlForm.ButtonType.WARNING);

				appendExtra.write(createF);

			}

			//$FALL-THROUGH$
			case (GET_PARAM_VALUE_USER_ACCESS): {

				int locationId = Integer.parseInt(parameters.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID)[0]);
				int userId = Integer.parseInt(parameters.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID)[0]);

				User u = User.getUserByLocationId(dbCon, locationId, userId);

				if (u == null) {
					p.writeText("User not found, ");
					p.writeLink(LOCATION, "try again");
					return p.finish().getBytes();
				}

				p.writeH1("Edit User Access for User " + u.getLocationId() + ":" + u.getUsername());

				HtmlTable t = new HtmlTable();

				t.addHeader(Arrays.asList("ID", "Location", "User", "Path", "Community", "Game", "Asset ID", "Edit",
						"Delete"));
				t.startBody();

				List<UserAccessFilter> fs = UserAccessFilter.getFilterByLocationUser(dbCon, u.getLocationId(),
						u.getId());

				for (UserAccessFilter f : fs) {
					HtmlTable.HtmlTableRow row = t.new HtmlTableRow();

					row.writeText(f.getId() + "");
					Location l = Location.getLocationById(dbCon, f.getLocationId());
					row.writeText((l == null) ? "unknown Location" : l.getName());
					User u1 = User.getUserByLocationId(dbCon, f.getLocationId(), f.getUserId());
					row.writeText((u1 == null) ? "unknown User" : u1.getUsername());
					row.writeText(f.getPath());
					Community c = Community.getCommunityById(dbCon, f.getCommunityId());
					row.writeText((c == null) ? "unknown Community" : c.getName());
					row.writeText(
							(f.getGameId() == 0 ? "All Games" : Game.getGameById(dbCon, f.getGameId()).getName()));
					row.writeText(f.getAssetId() + "");

					HtmlForm editF = new HtmlForm(LOCATION, HtmlForm.Method.GET);
					editF.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_USER_ACCESS_FILTER);
					editF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID, f.getId() + "");
					editF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID, f.getLocationId() + "");
					editF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID, f.getUserId() + "");
					editF.addSubmit("Edit", HtmlForm.ButtonType.WARNING);

					row.write(editF);

					HtmlForm deleteF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_DELETE_USER_ACCESS_FILTER,
							HtmlForm.Method.POST);
					deleteF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID, f.getId() + "");
					deleteF.addSubmit("Delete", HtmlForm.ButtonType.WARNING);

					row.write(deleteF);

					t.write(row);
				}

				p.write(t);

				if (appendExtra != null) {

					p.write(appendExtra);

				} else {

					User thisUser = s.getThisUser();

					if (thisUser.hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_CREATE_USER_ACCESS_FILTER),
							new CommunityAsset(u.getCommunityId()))) {
						p.writeH2("Create new Filter:");

						HtmlForm createF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_CREATE_USER_ACCESS_FILTER,
								HtmlForm.Method.POST);
						createF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID,
								u.getLocationId() + "");
						createF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID, u.getId() + "");

						createF.addSelectionDropdown("Path", UserAccessFilter.DB_TABLE_COLUMN_NAME_PATH,
								selectionPathValues);
						createF.addSelectionDropdown("Community", UserAccessFilter.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
								selectionCommunityValues);
						createF.addSelectionDropdown("Game", UserAccessFilter.DB_TABLE_COLUMN_NAME_GAME_ID, gameList);
						createF.addTextElement("Asset", UserAccessFilter.DB_TABLE_COLUMN_NAME_ASSET_ID, "");

						createF.addSubmit("Add User Access Filter", HtmlForm.ButtonType.SUCCESS);

						p.write(createF);
					}
				}

				break;
			}

			case (GET_PARAM_VALUE_GROUP_ACCESS_FILTER): {

				appendExtra = new GenericBuffer("<h2>Edit Filter:</h2>");

				HtmlForm createF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_CHANGE_GROUP_ACCESS_FILTER,
						HtmlForm.Method.POST);
				createF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ID,
						parameters.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ID)[0]);

				UserGroupAccessFilter f = UserGroupAccessFilter.getFilterById(dbCon,
						Integer.parseInt(parameters.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ID)[0]));

				createF.addSelectionDropdown("Path", UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_PATH,
						selectionPathValues, f.getPath());

				createF.addSelectionDropdown("Community", UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						selectionCommunityValues, f.getCommunityId() + "");
				createF.addSelectionDropdown("Game", UserAccessFilter.DB_TABLE_COLUMN_NAME_GAME_ID, gameList,
						f.getGameId() + "");
				createF.addTextElement("Asset", UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ASSET_ID,
						f.getAssetId() + "");

				createF.addSubmit("Edit Group Access Filter", HtmlForm.ButtonType.WARNING);

				appendExtra.write(createF);
			}

			//$FALL-THROUGH$
			case (GET_PARAM_VALUE_GROUP_ACCESS): {

				int locationId = Integer
						.parseInt(parameters.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID)[0]);
				int userGroupId = Integer
						.parseInt(parameters.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_USER_GROUP_ID)[0]);

				Location l = Location.getLocationById(dbCon, locationId);

				if (l == null) {
					p.writeText("Location not found, ");
					p.writeLink(LOCATION, "try again");
					return p.finish().getBytes();
				}

				p.writeH1("Edit User Access for Usergroup " + l.getId() + ":" + l.getUserGroupNameById(userGroupId));

				HtmlTable t = new HtmlTable();

				t.addHeader(Arrays.asList("ID", "Location", "User Group", "Path", "Community", "Game", "Asset ID"));
				t.startBody();

				List<UserGroupAccessFilter> fs = UserGroupAccessFilter.getFilterByLocationUserGroup(dbCon, l.getId(),
						userGroupId);

				for (UserGroupAccessFilter f : fs) {
					HtmlTable.HtmlTableRow row = t.new HtmlTableRow();

					row.writeText(f.getId() + "");
					Location l1 = Location.getLocationById(dbCon, f.getLocationId());
					row.writeText((l1 == null) ? "unknown Location" : l1.getName());
					String usergroup;
					if (l1 != null) {
						usergroup = AbstractRemoteConnection.getRemoteFromLocation(l1)
								.getUsergroupNameById(f.getUserGroupId());
					} else {
						usergroup = "Location error";
					}
					row.writeText((usergroup == null) ? "unknown UserGroup" : usergroup);
					row.writeText(f.getPath());
					Community c = Community.getCommunityById(dbCon, f.getCommunityId());
					row.writeText((c == null) ? "unknown Community" : c.getName());
					row.writeText(
							(f.getGameId() == 0 ? "All Games" : Game.getGameById(dbCon, f.getGameId()).getName()));
					row.writeText(f.getAssetId() + "");

					HtmlForm editF = new HtmlForm(LOCATION, HtmlForm.Method.GET);
					editF.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_GROUP_ACCESS_FILTER);
					editF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ID, f.getId() + "");
					editF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID,
							f.getLocationId() + "");
					editF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_USER_GROUP_ID,
							f.getUserGroupId() + "");
					editF.addSubmit("Edit", HtmlForm.ButtonType.WARNING);

					row.write(editF);

					HtmlForm deleteF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_DELETE_GROUP_ACCESS_FILTER,
							HtmlForm.Method.POST);
					deleteF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ID, f.getId() + "");
					deleteF.addSubmit("Delete", HtmlForm.ButtonType.DANGER);

					row.write(deleteF);

					t.write(row);
				}

				p.write(t);

				if (appendExtra != null) {

					p.write(appendExtra);

				} else {

					User thisUser = s.getThisUser();

					if (thisUser.hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_CREATE_GROUP_ACCESS_FILTER),
							new GeneralAsset(l.getId(), l.getCommunityId()))) {
						p.writeH2("Create new Filter:");

						HtmlForm createF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_CREATE_GROUP_ACCESS_FILTER,
								HtmlForm.Method.POST);
						createF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID,
								l.getId() + "");
						createF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_USER_GROUP_ID,
								userGroupId + "");

						createF.addSelectionDropdown("Path", UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_PATH,
								selectionPathValues);
						createF.addSelectionDropdown("Community",
								UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_COMMUNITY_ID, selectionCommunityValues);
						createF.addSelectionDropdown("Game", UserAccessFilter.DB_TABLE_COLUMN_NAME_GAME_ID, gameList);
						createF.addTextElement("Asset", UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ASSET_ID, "");

						createF.addSubmit("Add Group Access Filter", HtmlForm.ButtonType.SUCCESS);

						p.write(createF);
					}
				}

				break;
			}

			case GET_PARAM_VALUE_LIST_USER_ACCESS_FILTER_ALL: {

				if (!s.getThisUser().getIsRoot()) {
					p.writeText("no access!");
					return p.finish().getBytes();
				}

				HtmlTable t = new HtmlTable();

				t.addHeader(Arrays.asList("ID", "Location", "User", "Path", "Community", "Game", "Asset ID", "Edit",
						"Delete"));
				t.startBody();

				List<UserAccessFilter> fs = UserAccessFilter.getAllFilter(dbCon);

				for (UserAccessFilter f : fs) {
					HtmlTable.HtmlTableRow row = t.new HtmlTableRow();

					row.writeText(f.getId() + "");
					Location l = Location.getLocationById(dbCon, f.getLocationId());
					row.writeText((l == null) ? "unknown Location" : l.getName());
					User u1 = User.getUserByLocationId(dbCon, f.getLocationId(), f.getUserId());
					row.writeText((u1 == null) ? "unknown User" : u1.getUsername());
					row.writeText(f.getPath());
					Community c = Community.getCommunityById(dbCon, f.getCommunityId());
					row.writeText((c == null) ? "unknown Community" : c.getName());
					row.writeText(f.getGameId() == 0 ? "All Games" : Game.getGameById(dbCon, f.getGameId()).getName());
					row.writeText(f.getAssetId() + "");

					HtmlForm editF = new HtmlForm(LOCATION, HtmlForm.Method.GET);
					editF.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_USER_ACCESS_FILTER);
					editF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID, f.getId() + "");
					editF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID, f.getLocationId() + "");
					editF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID, f.getUserId() + "");
					editF.addSubmit("Edit", HtmlForm.ButtonType.WARNING);

					row.write(editF);

					HtmlForm deleteF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_DELETE_USER_ACCESS_FILTER,
							HtmlForm.Method.POST);
					deleteF.addHiddenElement(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID, f.getId() + "");
					deleteF.addSubmit("Delete", HtmlForm.ButtonType.WARNING);

					row.write(deleteF);

					t.write(row);
				}

				p.write(t);
				break;
			}

			case GET_PARAM_VALUE_LIST_GROUP_ACCESS_FILTER_ALL: {

				if (!s.getThisUser().getIsRoot()) {
					p.writeText("no access!");
					return p.finish().getBytes();
				}

				HtmlTable t = new HtmlTable();

				t.addHeader(Arrays.asList("ID", "Location", "User Group", "Path", "Community", "Game", "Asset ID"));
				t.startBody();

				List<UserGroupAccessFilter> fs = UserGroupAccessFilter.getAllFilter(dbCon);

				for (UserGroupAccessFilter f : fs) {
					HtmlTable.HtmlTableRow row = t.new HtmlTableRow();

					row.writeText(f.getId() + "");
					Location l1 = Location.getLocationById(dbCon, f.getLocationId());
					row.writeText((l1 == null) ? "unknown Location" : l1.getName());
					String usergroup;
					if (l1 != null) {
						usergroup = AbstractRemoteConnection.getRemoteFromLocation(l1)
								.getUsergroupNameById(f.getUserGroupId());
					} else {
						usergroup = "Location error";
					}
					row.writeText((usergroup == null) ? "unknown UserGroup" : usergroup);
					row.writeText(f.getPath());
					Community c = Community.getCommunityById(dbCon, f.getCommunityId());
					row.writeText((c == null) ? "unknown Community" : c.getName());
					row.writeText(f.getGameId() == 0 ? "All Games" : Game.getGameById(dbCon, f.getGameId()).getName());
					row.writeText(f.getAssetId() + "");

					HtmlForm editF = new HtmlForm(LOCATION, HtmlForm.Method.GET);
					editF.addHiddenElement(GET_PARAM, GET_PARAM_VALUE_GROUP_ACCESS_FILTER);
					editF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ID, f.getId() + "");
					editF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID,
							f.getLocationId() + "");
					editF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_USER_GROUP_ID,
							f.getUserGroupId() + "");
					editF.addSubmit("Edit", HtmlForm.ButtonType.WARNING);

					row.write(editF);

					HtmlForm deleteF = new HtmlForm(LOCATION + "." + FUNCTION_NAME_DELETE_GROUP_ACCESS_FILTER,
							HtmlForm.Method.POST);
					deleteF.addHiddenElement(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ID, f.getId() + "");
					deleteF.addSubmit("Delete", HtmlForm.ButtonType.DANGER);

					row.write(deleteF);

					t.write(row);
				}

				p.write(t);

				break;
			}

			default: {
				p.writeText("unknown function");
				return p.finish().getBytes();
			}
			}
		}

		return p.finish().getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> retMap = new HashMap<>();

		retMap.put(FUNCTION_NAME_CREATE_USER_ACCESS_FILTER, this::createUserAccessFilter);
		retMap.put(FUNCTION_NAME_CREATE_GROUP_ACCESS_FILTER, this::createGroupAccessFilter);

		retMap.put(FUNCTION_NAME_CHANGE_USER_ACCESS_FILTER, this::changeUserAccessFilter);
		retMap.put(FUNCTION_NAME_CHANGE_GROUP_ACCESS_FILTER, this::changeGroupAccessFilter);

		retMap.put(FUNCTION_NAME_DELETE_USER_ACCESS_FILTER, this::deleteUserAccessFilter);
		retMap.put(FUNCTION_NAME_DELETE_GROUP_ACCESS_FILTER, this::deleteGroupAccessFilter);

		return retMap;
	}

	private FunctionResult createUserAccessFilter(AcpSession s, Map<String, String[]> parameter) {

		int locationId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID)[0]);
		int userId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID)[0]);
		String path = parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_PATH)[0];
		int communityId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);
		int gameId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_GAME_ID)[0]);
		int assetId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_ASSET_ID)[0]);

		User u = User.getUserByLocationId(dbCon, locationId, userId);

		User thisUser = s.getThisUser();
		AccessibleFunctionHelper accFuncHelper = new AccessibleFunctionHelper(getModuleName(),
				FUNCTION_NAME_CREATE_USER_ACCESS_FILTER);

		Location newLocation = Location.getLocationById(dbCon, locationId);
		int communityIdFromNewLocation = getCommunityIdFromLocation(newLocation);

		if ((!thisUser.hasAccess(accFuncHelper, new CommunityAsset(u.getCommunityId())))
				|| (!thisUser.hasAccess(accFuncHelper, new CommunityAsset(communityId)))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		if ((newLocation != null) && (newLocation.getId() != 0)
				&& (!thisUser.hasAccess(accFuncHelper, new CommunityAsset(communityIdFromNewLocation)))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		AccessFilter newAF = new AccessFilter(path, assetId, communityId, gameId);

		if (newAF.getFunction() == null) {
			if (!thisUser.hasAccess(new AccessibleModuleHelper(newAF.getModule()))) {
				return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
			}
		} else {
			if (!thisUser.hasAccess(new AccessibleFunctionHelper(newAF.getModule(), newAF.getFunction()))) {
				return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
			}
		}

		UserAccessFilter.addNewUserAccessFilter(dbCon, locationId, userId, path, communityId, gameId, assetId);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);
		r.getBuilder().addParameter(GET_PARAM, GET_PARAM_VALUE_USER_ACCESS);

		r.getBuilder().addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID,
				(newLocation == null) ? "0" : (newLocation.getId() + ""));
		r.getBuilder().addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID, u.getId() + "");

		return r;
	}

	private FunctionResult createGroupAccessFilter(AcpSession s, Map<String, String[]> parameter) {

		int locationId = Integer.parseInt(parameter.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID)[0]);
		int userGroupId = Integer.parseInt(parameter.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_USER_GROUP_ID)[0]);
		String path = parameter.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_PATH)[0];
		int communityId = Integer.parseInt(parameter.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);
		int gameId = Integer.parseInt(parameter.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_GAME_ID)[0]);
		int assetId = Integer.parseInt(parameter.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ASSET_ID)[0]);

		User thisUser = s.getThisUser();
		AccessibleFunctionHelper accFuncHelper = new AccessibleFunctionHelper(getModuleName(),
				FUNCTION_NAME_CREATE_USER_ACCESS_FILTER);

		if ((!thisUser.hasAccess(accFuncHelper, new CommunityAsset(communityId))) || (!thisUser.hasAccess(accFuncHelper,
				new CommunityAsset(Location.getLocationById(dbCon, locationId).getCommunityId())))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		AccessFilter newAF = new AccessFilter(path, assetId, communityId, gameId);

		if (newAF.getFunction() == null) {
			if (!thisUser.hasAccess(new AccessibleModuleHelper(newAF.getModule()))) {
				return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
			}
		} else {
			if (!thisUser.hasAccess(new AccessibleFunctionHelper(newAF.getModule(), newAF.getFunction()))) {
				return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
			}
		}

		UserGroupAccessFilter.addNewUserGroupAccessFilter(dbCon, locationId, userGroupId, path, communityId, gameId,
				assetId);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);
		r.getBuilder().addParameter(GET_PARAM, GET_PARAM_VALUE_GROUP_ACCESS);
		r.getBuilder().addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID, locationId + "");
		r.getBuilder().addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_USER_GROUP_ID, userGroupId + "");

		return r;
	}

	private FunctionResult changeUserAccessFilter(AcpSession s, Map<String, String[]> parameter) {

		int filterId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID)[0]);
		UserAccessFilter f = UserAccessFilter.getFilterById(dbCon, filterId);

		String path = parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_PATH)[0];
		int communityId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);
		int gameId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_GAME_ID)[0]);
		int assetId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_ASSET_ID)[0]);

		User u = User.getUserByLocationId(dbCon, f.getLocationId(), f.getUserId());

		User thisUser = s.getThisUser();
		AccessibleFunctionHelper accFuncHelper = new AccessibleFunctionHelper(getModuleName(),
				FUNCTION_NAME_CREATE_USER_ACCESS_FILTER);

		Location oldLocationById = Location.getLocationById(dbCon, f.getLocationId());
		int oldcommunityId = getCommunityIdFromLocation(oldLocationById);

		if ((!thisUser.hasAccess(accFuncHelper, new CommunityAsset(u.getCommunityId())))
				|| (!thisUser.hasAccess(accFuncHelper, new CommunityAsset(communityId)))
				|| (!thisUser.hasAccess(accFuncHelper, new CommunityAsset(oldcommunityId)))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		if ((!thisUser.hasAccess(accFuncHelper, new CommunityAsset(f.getCommunityId())))
				|| (!thisUser.hasAccess(accFuncHelper, new CommunityAsset(oldcommunityId)))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		AccessFilter newAF = new AccessFilter(path, assetId, communityId, gameId);

		if (newAF.getFunction() == null) {
			if (!thisUser.hasAccess(new AccessibleModuleHelper(newAF.getModule()))) {
				return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
			}
		} else {
			if (!thisUser.hasAccess(new AccessibleFunctionHelper(newAF.getModule(), newAF.getFunction()))) {
				return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
			}
		}

		f.changeFilter(dbCon, path, communityId, gameId, assetId);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);
		r.getBuilder().addParameter(GET_PARAM, GET_PARAM_VALUE_USER_ACCESS);
		r.getBuilder().addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID, f.getLocationId() + "");
		r.getBuilder().addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID, f.getUserId() + "");

		return r;
	}

	private FunctionResult changeGroupAccessFilter(AcpSession s, Map<String, String[]> parameter) {

		int filterId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID)[0]);
		UserGroupAccessFilter f = UserGroupAccessFilter.getFilterById(dbCon, filterId);

		String path = parameter.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_PATH)[0];
		int communityId = Integer.parseInt(parameter.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);
		int gameId = Integer.parseInt(parameter.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_GAME_ID)[0]);
		int assetId = Integer.parseInt(parameter.get(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ASSET_ID)[0]);

		User thisUser = s.getThisUser();
		AccessibleFunctionHelper accFuncHelper = new AccessibleFunctionHelper(getModuleName(),
				FUNCTION_NAME_CREATE_GROUP_ACCESS_FILTER);

		if ((!thisUser.hasAccess(accFuncHelper, new CommunityAsset(communityId)))
				|| (!thisUser.hasAccess(accFuncHelper,
						new CommunityAsset(Location.getLocationById(dbCon, f.getLocationId()).getCommunityId())))
				|| (!thisUser.hasAccess(accFuncHelper, new CommunityAsset(f.getCommunityId())))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		AccessFilter newAF = new AccessFilter(path, assetId, communityId, gameId);

		if (newAF.getFunction() == null) {
			if (!thisUser.hasAccess(new AccessibleModuleHelper(newAF.getModule()))) {
				return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
			}
		} else {
			if (!thisUser.hasAccess(new AccessibleFunctionHelper(newAF.getModule(), newAF.getFunction()))) {
				return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
			}
		}

		f.changeFilter(dbCon, path, communityId, gameId, assetId);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);
		r.getBuilder().addParameter(GET_PARAM, GET_PARAM_VALUE_GROUP_ACCESS);
		r.getBuilder().addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID, f.getLocationId() + "");
		r.getBuilder().addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_USER_GROUP_ID, f.getUserGroupId() + "");

		return r;
	}

	private FunctionResult deleteUserAccessFilter(AcpSession s, Map<String, String[]> parameter) {
		int filterId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID)[0]);
		UserAccessFilter f = UserAccessFilter.getFilterById(dbCon, filterId);

		User thisUser = s.getThisUser();
		AccessibleFunctionHelper accFuncHelper = new AccessibleFunctionHelper(getModuleName(),
				FUNCTION_NAME_CREATE_GROUP_ACCESS_FILTER);

		int communityIdFromLocation = getCommunityIdFromLocation(Location.getLocationById(dbCon, f.getLocationId()));

		if ((!thisUser.hasAccess(accFuncHelper, new CommunityAsset(communityIdFromLocation)))
				|| (!thisUser.hasAccess(accFuncHelper, new CommunityAsset(f.getCommunityId())))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		f.deleteFilter(dbCon);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);
		r.getBuilder().addParameter(GET_PARAM, GET_PARAM_VALUE_USER_ACCESS);
		r.getBuilder().addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID, f.getLocationId() + "");
		r.getBuilder().addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID, f.getUserId() + "");

		return r;
	}

	private FunctionResult deleteGroupAccessFilter(AcpSession s, Map<String, String[]> parameter) {
		int filterId = Integer.parseInt(parameter.get(UserAccessFilter.DB_TABLE_COLUMN_NAME_ID)[0]);
		UserGroupAccessFilter f = UserGroupAccessFilter.getFilterById(dbCon, filterId);

		User thisUser = s.getThisUser();
		AccessibleFunctionHelper accFuncHelper = new AccessibleFunctionHelper(getModuleName(),
				FUNCTION_NAME_CREATE_GROUP_ACCESS_FILTER);

		if ((!thisUser.hasAccess(accFuncHelper,
				new CommunityAsset(Location.getLocationById(dbCon, f.getLocationId()).getCommunityId())))
				|| (!thisUser.hasAccess(accFuncHelper, new CommunityAsset(f.getCommunityId())))) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		f.deleteFilter(dbCon);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);
		r.getBuilder().addParameter(GET_PARAM, GET_PARAM_VALUE_GROUP_ACCESS);
		r.getBuilder().addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID, f.getLocationId() + "");
		r.getBuilder().addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_USER_GROUP_ID, f.getUserGroupId() + "");

		return r;
	}

	private static int getCommunityIdFromLocation(Location location) {
		int communityIdFromLocation;
		if (location != null) {
			communityIdFromLocation = location.getCommunityId();
		} else {
			communityIdFromLocation = 0;
		}
		return communityIdFromLocation;
	}

}
