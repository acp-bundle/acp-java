package co.clai.html;

import java.util.ArrayList;
import java.util.List;

import co.clai.access.AccessibleModuleHelper;
import co.clai.db.model.User;
import co.clai.module.AdminStatistics;
import co.clai.module.EditBanlist;
import co.clai.module.EditCommunity;
import co.clai.module.EditLocation;
import co.clai.module.EditServer;
import co.clai.module.EditStorage;
import co.clai.module.EditTemplate;
import co.clai.module.EditUser;
import co.clai.module.EditUserAccess;
import co.clai.module.Index;
import co.clai.module.Search;
import co.clai.module.ServerControl;
import co.clai.module.Settings;
import co.clai.module.Statistics;

public class Menu {

	private static Menu dummyMenu = new Menu();

	public class MenuEntry {

		public final String name;
		public final String url;
		public final List<MenuEntry> subMenu;

		public MenuEntry(String name, String url) {
			this.name = name;
			this.url = url;
			this.subMenu = null;
		}

		public MenuEntry(String name, List<MenuEntry> subMenu) {
			this.name = name;
			this.url = null;
			this.subMenu = subMenu;
		}
	}

	public static List<MenuEntry> loadMenuData(User u) {
		List<MenuEntry> retList = new ArrayList<>();
		retList.add(dummyMenu.new MenuEntry("Overview", Index.INDEX_LOCATION));

		List<MenuEntry> adminToolsList = new ArrayList<>();
		if (u.hasAccess(new AccessibleModuleHelper(Search.LOCATION))) {
			adminToolsList.add(dummyMenu.new MenuEntry("Logs and Recordings", Search.LOCATION));
		}
		if (u.hasAccess(new AccessibleModuleHelper(Statistics.LOCATION))) {
			adminToolsList.add(dummyMenu.new MenuEntry("Map & Server Statistics", Statistics.LOCATION));
		}
		if (u.hasAccess(new AccessibleModuleHelper(AdminStatistics.LOCATION))) {
			adminToolsList.add(dummyMenu.new MenuEntry("Admin Statistics", AdminStatistics.LOCATION));
		}
		if (!adminToolsList.isEmpty()) {
			retList.add(dummyMenu.new MenuEntry("Admin Tools", adminToolsList));
		}

		List<MenuEntry> seniorAdminToolsList = new ArrayList<>();
		if (u.hasAccess(new AccessibleModuleHelper(EditBanlist.LOCATION))) {
			seniorAdminToolsList.add(dummyMenu.new MenuEntry("Edit Banlist", EditBanlist.LOCATION));
		}
		if (u.hasAccess(new AccessibleModuleHelper(EditServer.LOCATION))) {
			seniorAdminToolsList.add(dummyMenu.new MenuEntry("Edit Server", EditServer.LOCATION));
		}
		if (u.hasAccess(new AccessibleModuleHelper(ServerControl.LOCATION))) {
			seniorAdminToolsList.add(dummyMenu.new MenuEntry("Control Server", ServerControl.LOCATION));
		}
		if (u.hasAccess(new AccessibleModuleHelper(EditTemplate.LOCATION))) {
			seniorAdminToolsList.add(dummyMenu.new MenuEntry("Edit Template", EditTemplate.LOCATION));
		}
		if (u.hasAccess(new AccessibleModuleHelper(EditStorage.LOCATION))) {
			seniorAdminToolsList.add(dummyMenu.new MenuEntry("Edit Storage", EditStorage.LOCATION));
		}
		if (!seniorAdminToolsList.isEmpty()) {
			retList.add(dummyMenu.new MenuEntry("Senior Admin Tools", seniorAdminToolsList));
		}

		List<MenuEntry> communityLeaderList = new ArrayList<>();
		if (u.hasAccess(new AccessibleModuleHelper(EditCommunity.LOCATION))) {
			communityLeaderList.add(dummyMenu.new MenuEntry("Edit Community", EditCommunity.LOCATION));
		}
		if (u.hasAccess(new AccessibleModuleHelper(EditUserAccess.LOCATION))) {
			communityLeaderList.add(dummyMenu.new MenuEntry("Edit User Access", EditUserAccess.LOCATION));
		}
		if (u.hasAccess(new AccessibleModuleHelper(EditUser.LOCATION))) {
			communityLeaderList.add(dummyMenu.new MenuEntry("Edit User", EditUser.LOCATION));
		}
		if (u.hasAccess(new AccessibleModuleHelper(EditLocation.LOCATION))) {
			communityLeaderList.add(dummyMenu.new MenuEntry("Edit Location", EditLocation.LOCATION));
		}
		if (!communityLeaderList.isEmpty()) {
			retList.add(dummyMenu.new MenuEntry("Community Leader Tools", communityLeaderList));
		}

		return retList;
	}

	public static MenuEntry loadUserMenuData(String username) {
		List<MenuEntry> userList = new ArrayList<>();

		userList.add(dummyMenu.new MenuEntry("Settings", Settings.LOCATION));
		userList.add(dummyMenu.new MenuEntry("Logout", Index.INDEX_LOCATION + "." + Index.FUNCTION_NAME_LOGOUT));
		return dummyMenu.new MenuEntry(username, userList);
	}

}
