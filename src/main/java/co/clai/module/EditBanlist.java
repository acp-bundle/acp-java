package co.clai.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import co.clai.AcpSession;
import co.clai.access.AccessibleFunctionHelper;
import co.clai.access.AccessibleModuleHelper;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Ban;
import co.clai.db.model.Banlist;
import co.clai.db.model.Community;
import co.clai.db.model.Game;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlForm.ButtonType;
import co.clai.html.HtmlPage;
import co.clai.html.HtmlTable;
import co.clai.util.StringStringPair;

public class EditBanlist extends AbstractModule {

	private static final String GET_PARAM_PERMABAN = "permaban";
	private static final String GET_PREFIX_EXTRA_INFO = "extra_info_";
	public static final String LOCATION = "banlist";
	public static final String TITLE = "Ban and unban people";

	public static final String FUNCTION_NAME_ADD_BAN = "addBan";
	public static final String FUNCTION_NAME_EDIT_BAN = "editBan";
	public static final String FUNCTION_NAME_UNBAN = "unban";

	public static final String FUNCTION_NAME_ADD_BANLIST = "addBanlist";
	public static final String FUNCTION_NAME_EDIT_BANLIST = "editBanlist";
	public static final String FUNCTION_NAME_REMOVE_BANLIST = "removeBanlist";

	private static final String GET_KEY = "edit";
	private static final String FUNCTION_NAME_EDIT_USER = "editUser";

	public EditBanlist(DatabaseConnector dbCon) {
		super(LOCATION, dbCon);
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {
		HtmlPage p = new HtmlPage(dbCon, TITLE, null, null, null, s);

		if ((parameters.get(GET_KEY) == null) || (parameters.get(GET_KEY).length == 0)) {
			p.writeH1("Edit Banlist");

			List<Banlist> banlists = Banlist.getAllBanlists(dbCon);

			HtmlTable t = new HtmlTable();
			t.addHeader(Arrays.asList("id", "key", "name", "game", "community", "edit banned user",
					"edit banlist settings"));
			t.startBody();

			for (Banlist banl : banlists) {
				if (s.getThisUser().hasAccess(new AccessibleModuleHelper(getModuleName()), banl.getAsset())) {
					HtmlTable.HtmlTableRow r = t.new HtmlTableRow();

					r.writeText(banl.getId() + "");
					r.writeText(banl.getKey());
					r.writeText(banl.getName());

					String gameName = "";
					try {
						Game g = Game.getGameById(dbCon, banl.getGameId());
						gameName = g.getName();
					} catch (Exception e) {
						logger.log(Level.SEVERE,
								"Error while creating game by id " + banl.getId() + ": " + e.getMessage(), e);
						gameName = "unknown";
					} finally {
						r.writeText(gameName);
					}

					String communityName = "";
					try {
						Community c = Community.getCommunityById(dbCon, banl.getCommunityId());
						communityName = c.getName();
					} catch (Exception e) {
						logger.log(Level.SEVERE,
								"Error while creating community by id " + banl.getCommunityId() + ": " + e.getMessage(),
								e);
						communityName = "unknown";
					} finally {
						r.writeText(communityName);
					}

					HtmlForm f = new HtmlForm(LOCATION, HtmlForm.Method.GET);
					f.addHiddenElement(GET_KEY, FUNCTION_NAME_EDIT_BAN);
					f.addHiddenElement(Banlist.DB_TABLE_COLUMN_NAME_ID, banl.getId() + "");
					f.addSubmit("Edit User", ButtonType.WARNING);
					r.write(f);

					if (s.getThisUser().hasAccess(
							new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_BANLIST),
							banl.getAsset())) {
						HtmlForm f2 = new HtmlForm(LOCATION, HtmlForm.Method.GET);
						f2.addHiddenElement(GET_KEY, FUNCTION_NAME_EDIT_BANLIST);
						f2.addHiddenElement(Banlist.DB_TABLE_COLUMN_NAME_ID, banl.getId() + "");
						f2.addSubmit("Edit Banlist", ButtonType.WARNING);
						r.write(f2);
					}

					t.write(r);
				}
			}

			p.write(t);

			if (s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_BANLIST))) {

				p.writeHline();

				p.writeH2("Add new Banlist:");

				HtmlForm addBanlistForm = new HtmlForm(LOCATION + "." + FUNCTION_NAME_ADD_BANLIST,
						HtmlForm.Method.POST);

				addBanlistForm.addTextElement("Key", Banlist.DB_TABLE_COLUMN_NAME_KEY, "");
				addBanlistForm.addTextElement("Name", Banlist.DB_TABLE_COLUMN_NAME_NAME, "");
				addBanlistForm.addSelectionDropdown("Game", Banlist.DB_TABLE_COLUMN_NAME_GAME_ID,
						Game.getGameListSelection(dbCon));
				addBanlistForm.addSelectionDropdown("Community", Banlist.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						Community.getCommunityListSelection(dbCon, s.getThisUser(),
								new AccessibleFunctionHelper(LOCATION, FUNCTION_NAME_ADD_BANLIST)));

				addBanlistForm.addSubmit("Add new Banlist", ButtonType.SUCCESS);
				p.write(addBanlistForm);
			}

		} else {

			boolean editUser = false;

			Banlist list = null;

			switch (parameters.get(GET_KEY)[0]) {

			case FUNCTION_NAME_EDIT_BANLIST: {

				Banlist b = Banlist.getBanlistById(dbCon,
						Integer.parseInt(parameters.get(Banlist.DB_TABLE_COLUMN_NAME_ID)[0]));

				p.writeH1("Edit " + b.getName());

				HtmlForm editBanlistForm = new HtmlForm(getModuleName() + "." + FUNCTION_NAME_EDIT_BANLIST,
						HtmlForm.Method.POST);

				editBanlistForm.addHiddenElement(Banlist.DB_TABLE_COLUMN_NAME_ID, b.getId() + "");
				editBanlistForm.addTextElement("Key", Banlist.DB_TABLE_COLUMN_NAME_KEY, b.getKey());
				editBanlistForm.addTextElement("Name", Banlist.DB_TABLE_COLUMN_NAME_NAME, b.getName());
				editBanlistForm.addSelectionDropdown("Game", Banlist.DB_TABLE_COLUMN_NAME_GAME_ID,
						Game.getGameListSelection(dbCon), b.getGameId() + "");
				editBanlistForm.addSelectionDropdown("Community", Banlist.DB_TABLE_COLUMN_NAME_COMMUNITY_ID,
						Community.getCommunityListSelection(dbCon, s.getThisUser(),
								new AccessibleFunctionHelper(LOCATION, FUNCTION_NAME_ADD_BANLIST)),
						b.getCommunityId() + "");

				String infoColumnsData = b.getInfoColumnsRaw();

				try {
					infoColumnsData = new JSONArray(b.getInfoColumnsRaw()).toString(4);
				} catch (Exception e) {
					logger.log(Level.WARNING, "Failed to parse banlist info columns for banlist with id " + b.getId()
							+ ": " + e.getMessage());
				}

				editBanlistForm.addTextArea(Banlist.DB_TABLE_COLUMN_NAME_INFO_COLUMNS, infoColumnsData, 20, 50);

				editBanlistForm.addSubmit("Edit Banlist", ButtonType.WARNING);

				p.write(editBanlistForm);

				if (s.getThisUser()
						.hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_REMOVE_BANLIST))) {

					p.writeHline();

					p.writeH2("Remove Banlist:");

					HtmlForm removeBanlistForm = new HtmlForm(LOCATION + "." + FUNCTION_NAME_REMOVE_BANLIST,
							HtmlForm.Method.POST);
					removeBanlistForm.addHiddenElement(Banlist.DB_TABLE_COLUMN_NAME_ID, b.getId() + "");
					removeBanlistForm.addSubmit("Remove Banlist", ButtonType.DANGER);
					p.write(removeBanlistForm);
				}

				break;
			}

			case FUNCTION_NAME_EDIT_USER: {

				int banId = Integer.parseInt(parameters.get(Ban.DB_TABLE_COLUMN_NAME_ID)[0]);

				Ban b = Ban.getBanById(dbCon, banId);
				list = Banlist.getBanlistById(dbCon, b.getBanlistId());

				p.writeH1("Ban and unban player");

				p.writeH2("Edit ban:");

				HtmlForm f = new HtmlForm(getModuleName() + "." + FUNCTION_NAME_EDIT_BAN, HtmlForm.Method.POST);
				f.addHiddenElement(Ban.DB_TABLE_COLUMN_NAME_ID, b.getId() + "");
				f.addTextElement("Hash", Ban.DB_TABLE_COLUMN_NAME_HASH, b.getHash());

				f.addTextElement("Start", Ban.DB_TABLE_COLUMN_NAME_START, Ban.DEFAULT_TIME_FORMAT.format(b.getStart()));
				f.addTextElement("End", Ban.DB_TABLE_COLUMN_NAME_END,
						(b.getEnd() == null) ? "" : Ban.DEFAULT_TIME_FORMAT.format(b.getEnd()));
				f.addCheckbox("Permaban", GET_PARAM_PERMABAN, "1", b.isPermaBan());

				for (StringStringPair field : list.getInfoColumns()) {
					f.addTextElement(field.getName(), GET_PREFIX_EXTRA_INFO + field.getId(), b.getInfo(field.getId()));
				}

				f.addSubmit("Edit Ban", ButtonType.WARNING);

				p.write(f);

				editUser = true;
			}

			//$FALL-THROUGH$
			case FUNCTION_NAME_EDIT_BAN: {

				Banlist bList;
				if (editUser) {
					bList = list;
				} else {
					bList = Banlist.getBanlistById(dbCon,
							Integer.parseInt(parameters.get(Banlist.DB_TABLE_COLUMN_NAME_ID)[0]));
				}

				if (bList == null) {
					throw new RuntimeException("This should not happen! EditBanlist::case FUNCTION_NAME_EDIT_BAN");
				}

				if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_BAN),
						bList.getAsset())) {
					p.writeText("No Access");
					return p.finish().getBytes();
				}

				if (!editUser) {
					p.writeH1("Ban and unban player");

					p.writeH2("Add new Ban");
					HtmlForm f = new HtmlForm(getModuleName() + "." + FUNCTION_NAME_ADD_BAN, HtmlForm.Method.POST);
					f.addHiddenElement(Ban.DB_TABLE_COLUMN_NAME_BANLIST_ID, bList.getId() + "");
					f.addTextElement("Hash", Ban.DB_TABLE_COLUMN_NAME_HASH, "");

					f.addTextElement("Start", Ban.DB_TABLE_COLUMN_NAME_START,
							Ban.DEFAULT_TIME_FORMAT.format(new Date(System.currentTimeMillis())));
					f.addTextElement("End", Ban.DB_TABLE_COLUMN_NAME_END,
							Ban.DEFAULT_TIME_FORMAT.format(new Date(System.currentTimeMillis())));
					f.addCheckbox("Permaban", GET_PARAM_PERMABAN, "1");

					for (StringStringPair field : bList.getInfoColumns()) {
						f.addTextElement(field.getName(), GET_PREFIX_EXTRA_INFO + field.getId(), "");
					}

					f.addSubmit("Add Ban", ButtonType.DANGER);

					p.write(f);
				}

				p.writeH2("Existing Bans");

				HtmlTable ht = new HtmlTable();

				List<String> header = new ArrayList<>();
				header.addAll(Arrays.asList("id", "hash", "start", "end", "permaban"));

				List<StringStringPair> extraColumns = new ArrayList<>();
				Set<String> isLink = bList.getInfoColumnLinks();

				try {
					extraColumns.addAll(bList.getInfoColumns());
					for (StringStringPair l : extraColumns) {
						header.add(l.getName());
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Cannot instanciate info columns: " + e.getMessage(), e);
				}

				header.addAll(Arrays.asList("Edit", "Unban"));
				ht.addHeader(header);
				ht.startBody();

				List<Ban> expiredBans = new ArrayList<>();

				for (Ban b : Ban.getBansFromBanlistId(dbCon, bList.getId())) {

					if (b.isExpired()) {
						expiredBans.add(b);
						continue;
					}

					HtmlTable.HtmlTableRow row = ht.new HtmlTableRow();

					row.writeText(b.getId() + "");
					row.writeText(b.getHash());
					row.writeText(Ban.DEFAULT_TIME_FORMAT.format(b.getStart()));
					if (b.isPermaBan()) {
						row.writeText("---");
						/// TODO add a nice looking image here
						row.writeText("x");
					} else {
						row.writeText(Ban.DEFAULT_TIME_FORMAT.format(b.getEnd()));
						row.writeText("");
					}

					try {
						for (StringStringPair l : extraColumns) {
							String thisId = l.getId();

							String thisInfo = b.getInfo(thisId);

							if (isLink.contains(thisId)) {
								if ("".equals(thisInfo)) {
									row.writeText(thisInfo); // aka write ""
								} else {
									row.writeLink(thisInfo, l.getName(), true);
								}
							} else {
								row.writeText(thisInfo);
							}
						}
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Cannot write info column data: " + e.getMessage(), e);
					}

					HtmlForm editBanLink = new HtmlForm(getModuleName(), HtmlForm.Method.GET);
					editBanLink.addHiddenElement(GET_KEY, FUNCTION_NAME_EDIT_USER);
					editBanLink.addHiddenElement(Ban.DB_TABLE_COLUMN_NAME_ID, b.getId() + "");
					editBanLink.addSubmit("Edit", ButtonType.WARNING);
					row.write(editBanLink);

					if (b.isExpired()) {
						row.writeText("Ban expired");
					} else {
						HtmlForm unbanLink = new HtmlForm(getModuleName() + "." + FUNCTION_NAME_UNBAN,
								HtmlForm.Method.POST);
						unbanLink.addHiddenElement(Ban.DB_TABLE_COLUMN_NAME_ID, b.getId() + "");
						unbanLink.addSubmit("Unban", ButtonType.SUCCESS);
						row.write(unbanLink);
					}

					ht.write(row);
				}

				for (Ban b : expiredBans) {

					HtmlTable.HtmlTableRow row = ht.new HtmlTableRow();

					row.writeText(b.getId() + "");
					row.writeText(b.getHash());
					row.writeText(Ban.DEFAULT_TIME_FORMAT.format(b.getStart()));
					if (b.isPermaBan()) {
						row.writeText("---");
						/// TODO add a nice looking image here
						row.writeText("x");
					} else {
						row.writeText(Ban.DEFAULT_TIME_FORMAT.format(b.getEnd()));
						row.writeText("");
					}

					try {
						for (StringStringPair l : extraColumns) {
							String thisId = l.getId();

							String thisInfo = b.getInfo(thisId);

							if (isLink.contains(thisId)) {
								if ("".equals(thisInfo)) {
									row.writeText(thisInfo); // aka write ""
								} else {
									row.writeLink(thisInfo, l.getName(), true);
								}
							} else {
								row.writeText(thisInfo);
							}
						}
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Cannot write info column data: " + e.getMessage(), e);
					}

					HtmlForm editBanLink = new HtmlForm(getModuleName(), HtmlForm.Method.GET);
					editBanLink.addHiddenElement(GET_KEY, FUNCTION_NAME_EDIT_USER);
					editBanLink.addHiddenElement(Ban.DB_TABLE_COLUMN_NAME_ID, b.getId() + "");
					editBanLink.addSubmit("Edit", ButtonType.WARNING);
					row.write(editBanLink);

					row.writeText("Ban expired");

					ht.write(row);
				}

				p.write(ht);

				break;
			}

			default:
				p.writeText("unknown edit parameter");
			}
		}
		return p.finish().getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> retMap = new HashMap<>();

		retMap.put(FUNCTION_NAME_ADD_BAN, this::addBan);
		retMap.put(FUNCTION_NAME_EDIT_BAN, this::editBan);
		retMap.put(FUNCTION_NAME_UNBAN, this::unban);

		retMap.put(FUNCTION_NAME_ADD_BANLIST, this::addBanlist);
		retMap.put(FUNCTION_NAME_EDIT_BANLIST, this::editBanlist);
		retMap.put(FUNCTION_NAME_REMOVE_BANLIST, this::removeBanlist);

		return retMap;
	}

	private FunctionResult addBan(AcpSession s, Map<String, String[]> parameters) {

		int bListId = Integer.parseInt(parameters.get(Ban.DB_TABLE_COLUMN_NAME_BANLIST_ID)[0]);
		String hash = parameters.get(Ban.DB_TABLE_COLUMN_NAME_HASH)[0];

		Banlist bList = Banlist.getBanlistById(dbCon, bListId);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_BAN),
				bList.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		JSONObject jO = new JSONObject();

		for (StringStringPair field : bList.getInfoColumns()) {
			if (parameters.containsKey(GET_PREFIX_EXTRA_INFO + field.getId())) {
				jO.put(field.getId(), parameters.get(GET_PREFIX_EXTRA_INFO + field.getId())[0]);
			}
		}

		String start = parameters.get(Ban.DB_TABLE_COLUMN_NAME_START)[0];
		String end = parameters.get(Ban.DB_TABLE_COLUMN_NAME_END)[0];
		boolean perm = parameters.containsKey(GET_PARAM_PERMABAN);

		Ban.addNewBan(dbCon, hash, bList.getId(), jO.toString(), start, end, perm);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(GET_KEY, FUNCTION_NAME_EDIT_BAN);
		r.getBuilder().addParameter(Banlist.DB_TABLE_COLUMN_NAME_ID, bList.getId() + "");

		return r;
	}

	private FunctionResult editBan(AcpSession s, Map<String, String[]> parameters) {

		int banId = Integer.parseInt(parameters.get(Ban.DB_TABLE_COLUMN_NAME_ID)[0]);
		String hash = parameters.get(Ban.DB_TABLE_COLUMN_NAME_HASH)[0];

		Ban b = Ban.getBanById(dbCon, banId);
		Banlist bList = Banlist.getBanlistById(dbCon, b.getBanlistId());

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_BAN),
				bList.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		JSONObject jO = new JSONObject();

		for (StringStringPair field : bList.getInfoColumns()) {
			if (parameters.containsKey(GET_PREFIX_EXTRA_INFO + field.getId())) {
				jO.put(field.getId(), parameters.get(GET_PREFIX_EXTRA_INFO + field.getId())[0]);
			}
		}

		String start = parameters.get(Ban.DB_TABLE_COLUMN_NAME_START)[0];
		String end = parameters.get(Ban.DB_TABLE_COLUMN_NAME_END)[0];
		boolean perm = parameters.containsKey(GET_PARAM_PERMABAN);

		b.edit(dbCon, hash, jO.toString(), start, end, perm);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(GET_KEY, FUNCTION_NAME_EDIT_BAN);
		r.getBuilder().addParameter(Banlist.DB_TABLE_COLUMN_NAME_ID, bList.getId() + "");

		return r;
	}

	private FunctionResult unban(AcpSession s, Map<String, String[]> parameters) {

		int banId = Integer.parseInt(parameters.get(Ban.DB_TABLE_COLUMN_NAME_ID)[0]);

		Ban b = Ban.getBanById(dbCon, banId);

		Banlist bList = Banlist.getBanlistById(dbCon, b.getBanlistId());

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_BAN),
				bList.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		b.unban(dbCon);

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(GET_KEY, FUNCTION_NAME_EDIT_BAN);
		r.getBuilder().addParameter(Banlist.DB_TABLE_COLUMN_NAME_ID, bList.getId() + "");

		return r;
	}

	private FunctionResult addBanlist(AcpSession s, Map<String, String[]> parameters) {

		String key = parameters.get(Banlist.DB_TABLE_COLUMN_NAME_KEY)[0];
		String name1 = parameters.get(Banlist.DB_TABLE_COLUMN_NAME_NAME)[0];
		int gameId = Integer.parseInt(parameters.get(Banlist.DB_TABLE_COLUMN_NAME_GAME_ID)[0]);
		int communityId = Integer.parseInt(parameters.get(Banlist.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_ADD_BANLIST),
				Community.getCommunityById(dbCon, communityId).getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		Banlist.addNewBanlist(dbCon, key, name1, gameId, communityId);

		return new FunctionResult(FunctionResult.Status.OK, LOCATION);
	}

	private FunctionResult editBanlist(AcpSession s, Map<String, String[]> parameters) {

		int id = Integer.parseInt(parameters.get(Banlist.DB_TABLE_COLUMN_NAME_ID)[0]);
		String key = parameters.get(Banlist.DB_TABLE_COLUMN_NAME_KEY)[0];
		String name1 = parameters.get(Banlist.DB_TABLE_COLUMN_NAME_NAME)[0];
		int gameId = Integer.parseInt(parameters.get(Banlist.DB_TABLE_COLUMN_NAME_GAME_ID)[0]);
		int communityId = Integer.parseInt(parameters.get(Banlist.DB_TABLE_COLUMN_NAME_COMMUNITY_ID)[0]);
		String infoColumnsRaw = parameters.get(Banlist.DB_TABLE_COLUMN_NAME_INFO_COLUMNS)[0];

		JSONArray infoColumns = new JSONArray(infoColumnsRaw);

		Banlist b = Banlist.getBanlistById(dbCon, id);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_EDIT_BANLIST),
				b.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		b.editBanlist(dbCon, key, name1, gameId, communityId, infoColumns.toString());

		FunctionResult r = new FunctionResult(FunctionResult.Status.OK, LOCATION);

		r.getBuilder().addParameter(GET_KEY, FUNCTION_NAME_EDIT_BANLIST);
		r.getBuilder().addParameter(Banlist.DB_TABLE_COLUMN_NAME_ID, b.getId() + "");

		return r;
	}

	private FunctionResult removeBanlist(AcpSession s, Map<String, String[]> parameters) {

		int id = Integer.parseInt(parameters.get(Banlist.DB_TABLE_COLUMN_NAME_ID)[0]);

		Banlist b = Banlist.getBanlistById(dbCon, id);

		if (!s.getThisUser().hasAccess(new AccessibleFunctionHelper(getModuleName(), FUNCTION_NAME_REMOVE_BANLIST),
				b.getAsset())) {
			return new FunctionResult(FunctionResult.Status.NO_ACCESS, LOCATION);
		}

		b.removeBanlist(dbCon);

		return new FunctionResult(FunctionResult.Status.OK, LOCATION);
	}

}
