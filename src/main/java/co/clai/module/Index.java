package co.clai.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Level;

import co.clai.AcpSession;
import co.clai.access.AccessibleHelper;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Location;
import co.clai.db.model.User;
import co.clai.html.HtmlForm;
import co.clai.html.HtmlPage;
import co.clai.html.Menu;
import co.clai.html.Menu.MenuEntry;
import co.clai.remote.AbstractRemoteConnection;
import co.clai.util.IntStringPair;
import co.clai.util.ValueValuePair;

public class Index extends AbstractModule {

	public static final String FUNCTION_NAME_LOGOUT = "logout";
	public static final String FUNCTION_NAME_LOGIN = "login";

	public static final String INDEX_LOCATION = "index";

	public static final String LOGIN_FORM_NAME_USERNAME = "username";
	public static final String LOGIN_FORM_NAME_PASSWORD = "password";
	public static final String LOGIN_FORM_NAME_LOCATION = "location";

	public Index(DatabaseConnector dbCon) {
		super(INDEX_LOCATION, dbCon, new AccessibleHelper(true));
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {

		HtmlPage b;

		List<Location> locations = Location.getAllLocations(dbCon);

		User thisUser = s.getThisUser();
		if (thisUser == null) {
			b = new HtmlPage(dbCon, "Login", null, null, null, s);
			b.writeWithoutEscaping(HtmlPage.getMessage(parameters));

			b.writeH1("Log in with OAuth2");

			for (Location l : locations) {
				try {
					if (AbstractRemoteConnection.getRemoteFromLocation(l).canDoOAuth2Login()) {

						HtmlForm loginFormOAuth2 = new HtmlForm(OAuth2.LOCATION + "." + OAuth2.FUNCTION_NAME_LOGIN,
								HtmlForm.Method.POST);

						loginFormOAuth2.addHiddenElement(Location.DB_TABLE_COLUMN_NAME_ID, l.getId() + "");
						loginFormOAuth2.addSubmit("Login as " + l.getName(), HtmlForm.ButtonType.SUCCESS);

						b.write(loginFormOAuth2);
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, "error creating location", e);
				}
			}

			b.writeHline();

			b.writeH1("Log in with Username and Password");

			HtmlForm r = new HtmlForm(INDEX_LOCATION + "." + FUNCTION_NAME_LOGIN, HtmlForm.Method.POST);

			r.addTextElement("Username", LOGIN_FORM_NAME_USERNAME, "");
			r.addPasswordElement("Password", LOGIN_FORM_NAME_PASSWORD);

			List<ValueValuePair> locs = new ArrayList<>();
			locs.add(new IntStringPair(0, "Local"));
			for (Location l : locations) {
				try {
					if (AbstractRemoteConnection.getRemoteFromLocation(l).canDoPasswordLogin()) {
						locs.add(new IntStringPair(l.getId(), l.getName()));
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, "error creating location", e);
				}
			}
			r.addSelectionDropdown("Location", LOGIN_FORM_NAME_LOCATION, locs);
			r.addSubmit("Login", HtmlForm.ButtonType.SUCCESS);

			b.write(r);

			return b.finish().getBytes();
		}

		b = new HtmlPage(dbCon, "Overview", null, null, null, s);
		b.writeWithoutEscaping(HtmlPage.getMessage(parameters));
		b.writeText("Logged in as User: ");
		b.writeText(thisUser.getLocationId() + ":" + thisUser.getUsername());
		b.writeText("; Usergroups: ");
		for (Integer i : thisUser.getUserGroups()) {
			b.writeText(i + ", ");
		}

		b.writeHline();

		b.writeH1("You have access to the following modules:");

		b.writeWithoutEscaping("<ul>");
		List<MenuEntry> accessibleModules = Menu.loadMenuData(s.getThisUser());
		for (MenuEntry e : accessibleModules) {
			if (e.url == null) {
				b.writeWithoutEscaping("<li>");
				b.writeH3(e.name);
				b.writeWithoutEscaping("</li>");
				b.writeWithoutEscaping("<ul>");
				for (MenuEntry e2 : e.subMenu) {
					b.writeWithoutEscaping("<li><a href='" + e2.url + "'>" + e2.name + "</a></li>");
				}
				b.writeWithoutEscaping("</ul>");
			} else {
				b.writeWithoutEscaping("<li><a href='" + e.url + "'>" + e.name + "</a></li>");
			}
		}
		b.writeWithoutEscaping("</ul>");

		return b.finish().getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> allFunctions = new HashMap<>();

		allFunctions.put(FUNCTION_NAME_LOGIN, this::login);
		allFunctions.put(FUNCTION_NAME_LOGOUT, this::logout);

		return allFunctions;
	}

	private FunctionResult login(AcpSession s, Map<String, String[]> parameters) {

		if (s.getThisUser() != null) {
			return new FunctionResult(FunctionResult.Status.MALFORMED_REQUEST, getModuleName());
		}

		try {
			int location = Integer.parseInt(parameters.get(LOGIN_FORM_NAME_LOCATION)[0]);
			String username = parameters.get(LOGIN_FORM_NAME_USERNAME)[0];
			String password = parameters.get(LOGIN_FORM_NAME_PASSWORD)[0];

			User u = User.login(dbCon, location, username, password);

			if (u == null) {
				return new FunctionResult(FunctionResult.Status.FAILED, getModuleName(), "failed login");
			}

			s.setUser(u);
		} catch (Exception e) {
			e.printStackTrace();
			return new FunctionResult(FunctionResult.Status.INTERNAL_ERROR, getModuleName());
		}

		return new FunctionResult(FunctionResult.Status.OK, getModuleName());
	}

	private FunctionResult logout(AcpSession s, @SuppressWarnings("unused") Map<String, String[]> parameters) {
		if (s.getThisUser() == null) {
			return new FunctionResult(FunctionResult.Status.MALFORMED_REQUEST, getModuleName(), "not logged in");
		}

		s.setUser(null);
		s.clear();

		return new FunctionResult(FunctionResult.Status.OK, getModuleName());
	}

}
