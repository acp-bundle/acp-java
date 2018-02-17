package co.clai.game.prbf2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Location;
import co.clai.db.model.Server;
import co.clai.db.model.ServerSetting;
import co.clai.game.Prbf2;
import co.clai.module.Query;
import co.clai.remote.AbstractRemoteConnection;
import co.clai.util.StringUtil;
import co.clai.util.log.LoggingUtil;

public class QueryPrbf2 {

	private static final Logger logger = LoggingUtil.getLoggerFromModule(Query.class);

	public static void appendAdminHashes(DatabaseConnector dbCon, StringBuilder sb, Server server) {

		ServerSetting adminGroupSetting = ServerSetting.getServerSettingByServerIdAndKey(dbCon, server.getId(),
				Prbf2.CONFIG_KEY_ADMIN_GROUPS);

		StringBuilder adminConfigSb = new StringBuilder();
		try {
			if (adminGroupSetting != null) {
				Map<String, String> userRoleMap = new HashMap<>();

				String adminSettingData = new String(adminGroupSetting.getData());

				JSONArray locationArr = new JSONArray(adminSettingData);

				for (int i = 0; i < locationArr.length(); i++) {
					JSONObject locationConfig = locationArr.getJSONObject(i);

					Location location = Location.getLocationById(dbCon,
							Integer.parseInt(locationConfig.getString("location")));

					if (location.getCommunityId() != server.getCommunityId()) {
						logger.log(Level.WARNING,
								"server config tried to pull admin data from wrong community, serverid="
										+ server.getId() + ", key=" + Prbf2.CONFIG_KEY_ADMIN_GROUPS);
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

								if ((adminHash != null) && (StringUtil.containsOnlyNumbersAndLetters(adminHash))) {
									userRoleMap.put(adminHash, role);
								}
							}
						}
					}
				}

				adminConfigSb.append("adm_adminHashes = {\n");

				for (Entry<String, String> e : userRoleMap.entrySet()) {
					adminConfigSb.append("\"" + e.getKey() + "\" : " + e.getValue() + ",\n");
				}

				adminConfigSb.append("}\n");
				sb.append(adminConfigSb.toString());
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "error occured while trying to fetch remote Admin access from config, serverid="
					+ server.getId() + ", key=" + Prbf2.CONFIG_KEY_ADMIN_GROUPS + ": " + e.getMessage());
			e.printStackTrace();
		}

		ServerSetting ambExcludeListSetting = ServerSetting.getServerSettingByServerIdAndKey(dbCon, server.getId(),
				Prbf2.CONFIG_KEY_SMB_EXCLUDE_LIST);

		try {
			if (ambExcludeListSetting != null) {
				sb.append("smb_excludeList = " + ambExcludeListSetting.getDataAsString());
			}

		} catch (Exception e) {
			logger.log(Level.WARNING, "error occured while trying to fetch remote Admin access from config, serverid="
					+ server.getId() + ", key=" + Prbf2.CONFIG_KEY_ADMIN_GROUPS + ": " + e.getMessage());
			e.printStackTrace();
		}

	}

	public static void appendServerSettings(DatabaseConnector dbCon, StringBuilder sb, Server server) {
		try {

			ServerSetting localIpSetting = ServerSetting.getServerSettingByServerIdAndKey(dbCon, server.getId(),
					Prbf2.CONFIG_KEY_LOCAL_IP);

			if (localIpSetting != null) {
				sb.append("sv.serverIP \"" + localIpSetting.getDataAsString() + "\"\n");
			} else {
				ServerSetting ipSetting = ServerSetting.getServerSettingByServerIdAndKey(dbCon, server.getId(),
						Prbf2.CONFIG_KEY_IP);
				sb.append("sv.serverIP \"" + ipSetting.getDataAsString() + "\"\n");
			}

			appendSettingIfExists(dbCon, sb, server, "sv.serverPort ", "\n", Prbf2.CONFIG_KEY_PORT);
			appendSettingIfExists(dbCon, sb, server, "sv.gameSpyPort ", "\n", Prbf2.CONFIG_KEY_QUERY);
			appendSettingIfExists(dbCon, sb, server, "sv.adminScript \"", "\"\n", Prbf2.CONFIG_KEY_SV_ADMIN_SCRIPT);
			appendSettingIfExists(dbCon, sb, server, "sv.serverName \"", "\"\n", Prbf2.CONFIG_KEY_SV_BROWSER_NAME);
			appendSettingIfExists(dbCon, sb, server, "sv.password \"", "\"\n", Prbf2.CONFIG_KEY_SV_PASSWORD);
			appendSettingIfExists(dbCon, sb, server, "sv.welcomeMessage \"", "\"\n",
					Prbf2.CONFIG_KEY_SV_WELCOMEMESSAGE);
			appendSettingIfExists(dbCon, sb, server, "sv.sponsorText \"", "\"\n", Prbf2.CONFIG_KEY_SV_WELCOMEMESSAGE);
			appendSettingIfExists(dbCon, sb, server, "sv.sponsorLogoURL \"", "\"\n", Prbf2.CONFIG_KEY_SV_LOGO_URL);
			appendSettingIfExists(dbCon, sb, server, "sv.communityLogoURL \"", "\"\n", Prbf2.CONFIG_KEY_SV_LOGO_URL);
			appendSettingIfExists(dbCon, sb, server, "sv.maxPlayers ", "\n", Prbf2.CONFIG_KEY_SV_MAX_PLAYERS);

		} catch (Exception e) {
			logger.log(Level.WARNING, "error occured while trying to fetch remote Admin access from config, serverid="
					+ server.getId() + ", key=" + Prbf2.CONFIG_KEY_ADMIN_GROUPS + ": " + e.getMessage());
			e.printStackTrace();
		}

	}

	private static void appendSettingIfExists(DatabaseConnector dbCon, StringBuilder sb, Server server, String infront,
			String behind, String key) {
		ServerSetting setting = ServerSetting.getServerSettingByServerIdAndKey(dbCon, server.getId(), key);

		if (setting != null) {
			sb.append(infront + setting.getDataAsString() + behind);
		}
	}

}
