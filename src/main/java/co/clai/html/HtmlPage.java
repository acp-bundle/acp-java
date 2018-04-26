package co.clai.html;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;

import co.clai.AcpSession;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Community;
import co.clai.db.model.User;
import co.clai.html.Menu.MenuEntry;
import co.clai.util.RandomUtil;

import static co.clai.html.Builder.escapeForHtml;

public class HtmlPage implements Builder {

	private final StringBuilder sb = new StringBuilder();

	private static final String PAGE_HTML_RESOURCE_NAME = "/html/page.html";
	private static List<String> partsEndMarker = loadPartsEndMarker();
	private static List<String> partsContent = loadPartsContent(partsEndMarker);

	private final String footer;
	private final AcpSession session;
	private final DatabaseConnector dbCon;

	public HtmlPage(DatabaseConnector dbCon, String title, String script, String style, String footer,
			AcpSession session) {
		this.session = session;
		this.dbCon = dbCon;

		write(partsContent.get(0)); // Before Title
		write(escapeForHtml(title));
		write(partsContent.get(1)); // Before Script
		write((script == null) ? "" : script);
		write(partsContent.get(2)); // Before Style
		write((style == null) ? "" : style);
		write(partsContent.get(3)); // Before Menu

		write("\n" + "<nav class='navbar navbar-expand-md navbar-light bg-light rounded'>\n"
				+ "<a class='navbar-brand visible-md' href='#'>Menu</a>\n"
				+ "<button class='navbar-toggler' type='button' data-toggle='collapse' "
				+ " data-target='#navbarsExample04' aria-controls='navbarsExample04' "
				+ " aria-expanded='false' aria-label='Toggle navigation'>\n"
				+ "<span class='navbar-toggler-icon'></span>\n </button> "
				+ "<div class='collapse navbar-collapse' id='navbarsExample04'>");

		User thisUser = session.getThisUser();

		if (thisUser == null) {
			write("<ul class='navbar-nav mr-auto navbar-right'>");
			write("<li class='nav-item'><a class='nav-link' href='index'>Login</a>\n</li>");
			write("</ul>");
		} else {
			write("<ul class='navbar-nav mr-auto'>");
			for (MenuEntry m : Menu.loadMenuData(thisUser)) {
				write(renderSubMenu(m));
			}
			write("</ul>");
			write("<ul class='nav navbar-nav navbar-right'>");
			write(renderSubMenu(Menu.loadUserMenuData(thisUser.getUsername())));
			write("</ul>");
		}
		write("\n</div></nav><br/>");
		write(partsContent.get(4)); // Before Content

		// save the footer for later:
		this.footer = escapeForHtml(footer);
	}

	private static String renderSubMenu(MenuEntry menuData) {
		StringBuilder stream = new StringBuilder();

		if (menuData.subMenu == null) {
			stream.append("<li class='nav-item'><a class='nav-link' a href='" + menuData.url + "'>"
					+ escapeForHtml(menuData.name) + "</a></li>" + "\n");
		} else {
			String id = "aria_id_" + RandomUtil.getRandomString(30);

			stream.append("<li class='nav-item dropdown'>");

			stream.append("<a class='nav-link dropdown-toggle' href='#' " + "id='" + id
					+ "' data-toggle='dropdown' aria-haspopup='true' " + "aria-expanded='false'>"
					+ escapeForHtml(menuData.name) + "</a>");

			stream.append("<div class='dropdown-menu' aria-labelledby='" + id + "'>");

			for (MenuEntry m : menuData.subMenu) {
				stream.append("<a class='dropdown-item' href='" + m.url + "'>" + escapeForHtml(m.name) + "</a>" + "\n");
			}
			stream.append("</div></li>");
		}
		return stream.toString();
	}

	private static List<String> loadPartsEndMarker() {
		List<String> retList = new ArrayList<>();

		retList.add("TITLE");
		retList.add("SCRIPT");
		retList.add("STYLE");
		retList.add("MENU");
		retList.add("CONTENT");
		retList.add("FOOTER");

		return retList;
	}

	private static List<String> loadPartsContent(List<String> endMarkerList) {

		List<String> retList = new ArrayList<>();

		try (BufferedReader b = new BufferedReader(
				new InputStreamReader(endMarkerList.getClass().getResourceAsStream(PAGE_HTML_RESOURCE_NAME)))) {

			for (String s : endMarkerList) {
				StringBuilder sb = new StringBuilder();

				while (b.ready()) {
					String tmp = b.readLine();
					if (tmp.contains("-- " + s + " --")) {
						break;
					}
					sb.append(tmp + "\n");
				}

				retList.add(sb.toString());
			}

			StringBuilder sb = new StringBuilder();

			while (b.ready()) {
				String tmp = b.readLine();
				sb.append(tmp + "\n");
			}

			retList.add(sb.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return retList;
	}

	private void write(String html) {
		sb.append(html + "\n");

	}

	@Override
	public void writeWithoutEscaping(String html) {
		write(html);
	}

	@Override
	public String finish() {
		sb.append(partsContent.get(5)); // Before Footer
		User thisUser = session.getThisUser();
		int communityId;
		if (thisUser == null) {
			communityId = 0;
		} else {
			communityId = thisUser.getCommunityId();
		}

		sb.append((footer == null)
				? ("Powered by <a href=\"https://github.com/ClundXIII/acp-java\">acp-java</a>, hosted by "
						+ Community.getCommunityById(dbCon, communityId).getName())
				: footer);
		sb.append(partsContent.get(6)); // After Footer
		return sb.toString();
	}

	public static String getMessage(Map<String, String[]> parameters) {
		String[] messageParams = parameters.get("message");
		if ((messageParams == null) || (messageParams.length == 0)) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("<div id=\"function_return_message\">");
		sb.append(escapeForHtml(messageParams[0]));
		sb.append("</div>");

		return sb.toString();
	}

	public void writeH1(String string) {
		write("<h1>" + escapeForHtml(string) + "</h1>");
	}

	public void writeH2(String string) {
		write("<h2>" + escapeForHtml(string) + "</h2>");
	}

	public void writeH3(String string) {
		write("<h3>" + escapeForHtml(string) + "</h3>");
	}

	public void writeText(String string) {
		write(escapeForHtml(string));
	}

	@Override
	public void write(Builder b) {
		write(b.finish());
	}

	public void writeLink(String location, String text) {
		write("<a href='" + location + "'>" + escapeForHtml(text) + "</a>");
	}

	public void writeLink(URIBuilder location, String text) {
		writeLink(location.toString(), text);
	}

	public void writeHline() {
		write("<hr>");
	}

	public void writePre(String string) {
		write("<pre>" + escapeForHtml(string) + "</pre>");
	}

}
