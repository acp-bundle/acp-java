package co.clai.game.squad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Location;
import co.clai.db.model.Server;
import co.clai.db.model.ServerSetting;
import co.clai.game.Squad;
import co.clai.module.Query;
import co.clai.remote.AbstractRemoteConnection;
import co.clai.util.StringUtil;
import co.clai.util.log.LoggingUtil;

public class QuerySquad {

	public static void generateAdminConfig(DatabaseConnector dbCon, StringBuilder sb, Server server) {

		Logger logger = LoggingUtil.getLoggerFromModule(Query.class);

		ServerSetting adminConfig = ServerSetting.getServerSettingByServerIdAndKey(dbCon, server.getId(),
				Squad.CONFIG_KEY_ADMIN_GROUPS);

		if (adminConfig == null) {
			return;
		}

		JSONObject jAdminConfig = new JSONObject(new String(adminConfig.getData()));

		JSONArray jRoles = jAdminConfig.getJSONArray("roles");

		for (int i = 0; i < jRoles.length(); i++) {
			sb.append(jRoles.getString(i) + "\n");
		}

		sb.append("\n");

		Map<String, String> userRoleMap = new HashMap<>();

		JSONArray locationArr = jAdminConfig.getJSONArray("group_access");

		for (int i = 0; i < locationArr.length(); i++) {
			JSONObject locationConfig = locationArr.getJSONObject(i);

			Location location = Location.getLocationById(dbCon, Integer.parseInt(locationConfig.getString("location")));

			if (location.getCommunityId() != server.getCommunityId()) {
				logger.log(Level.WARNING, "server config tried to pull admin data from wrong community, serverid="
						+ server.getId() + ", key=" + Squad.CONFIG_KEY_ADMIN_GROUPS);
				continue;
			}

			String fieldId = locationConfig.getString("field_id");

			JSONArray userGroupConfig = locationConfig.getJSONArray("groups");

			AbstractRemoteConnection con = AbstractRemoteConnection.getRemoteFromLocation(location);

			for (int j = 0; j < userGroupConfig.length(); j++) {
				JSONObject groupConfig = userGroupConfig.getJSONObject(j);

				String role = groupConfig.getString("role");
				String[] groups = groupConfig.getString("groups").split(",");

				List<Integer> groupIds = new ArrayList<>();

				for (String gr : groups) {
					if (!"".equals(gr)) {
						groupIds.add(new Integer(gr));
					}
				}

				for (Integer groupId : groupIds) {
					List<Integer> userIdList = con.getUserIdsFromUserGroup(groupId.intValue());

					for (Integer userId : userIdList) {
						String adminHash = con.getUserFieldContentFromUserId(fieldId, userId.intValue());

						if ((adminHash != null)
								&& (StringUtil.containsOnlyNumbers(adminHash) && (!"".equals(adminHash)))) {
							userRoleMap.put(adminHash, role);
						}
					}
				}
			}
		}

		for (Entry<String, String> e : userRoleMap.entrySet()) {
			sb.append("Admin=" + e.getKey() + ":" + e.getValue() + "\n");
		}
	}

	public static void generateReservedSlots(DatabaseConnector dbCon, StringBuilder sb, Server server) {

		Logger logger = LoggingUtil.getLoggerFromModule(Query.class);

		ServerSetting adminConfig = ServerSetting.getServerSettingByServerIdAndKey(dbCon, server.getId(),
				Squad.CONFIG_KEY_RESERVED_GROUPS);

		if (adminConfig == null) {
			return;
		}

		JSONArray locationArr = new JSONArray(new String(adminConfig.getData()));

		Set<String> userMap = new HashSet<>();

		sb.append("Group=ReservedSlot:reserve\n\n");

		for (int i = 0; i < locationArr.length(); i++) {
			JSONObject locationConfig = locationArr.getJSONObject(i);

			Location location = Location.getLocationById(dbCon, Integer.parseInt(locationConfig.getString("location")));

			if (location.getCommunityId() != server.getCommunityId()) {
				logger.log(Level.WARNING, "server config tried to pull admin data from wrong community, serverid="
						+ server.getId() + ", key=" + Squad.CONFIG_KEY_ADMIN_GROUPS);
				continue;
			}

			String fieldId = locationConfig.getString("field_id");

			String[] userGroups = locationConfig.getString("groups").split(",");

			AbstractRemoteConnection con = AbstractRemoteConnection.getRemoteFromLocation(location);

			for (String groupId : userGroups) {
				if ("".equals(groupId)) {
					continue;
				}

				List<Integer> userIdList = con.getUserIdsFromUserGroup(Integer.parseInt(groupId));

				for (Integer userId : userIdList) {
					String adminHash = con.getUserFieldContentFromUserId(fieldId, userId.intValue());

					if ((adminHash != null) && (StringUtil.containsOnlyNumbers(adminHash) && (!"".equals(adminHash)))) {
						userMap.add(adminHash);
					}
				}
			}
		}

		for (String s : userMap) {
			sb.append("Admin=" + s + ":ReservedSlot\n");
		}
	}
}
