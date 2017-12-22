package co.clai;

import java.util.Arrays;

import org.apache.http.client.utils.URIBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Community;
import co.clai.db.model.Location;
import co.clai.db.model.User;
import co.clai.db.model.UserAccessFilter;
import co.clai.db.model.UserGroupAccessFilter;
import co.clai.module.EditUser;
import co.clai.module.EditUserAccess;
import co.clai.module.FunctionResult;
import co.clai.module.Index;
import co.clai.module.Search;
import co.clai.remote.DummyConnection;
import co.clai.util.log.LoggingUtil;
import junit.framework.TestCase;

public class CommunitySetupTest extends TestCase implements HttpTest {

	private static final String USERNAME_COMMUNITY_LEADER = "communityLeader";
	private static final String PASSWORD_COMMUNIY_LEADER = "correctPassword";

	private static final String USERNAME_NORMAL_ADMIN = "normalAdmin";
	private static final String PASSWORD_NORMAL_ADMIN = "normalAdminPwd";

	public CommunitySetupTest(String name) {
		super(name);
	}

	public void testSettingUpLocalCommunity() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());
		try {

			JSONObject jData = getRandomDbAndListeningConfig();
			String baseUrl = getIpAndPort(jData);

			MainHttpListener l = new MainHttpListener(jData);
			startHttpListener(l);

			DatabaseConnector.initializeDatabase(l.getDbCon());

			User.addNewLocalUser(l.getDbCon(), USERNAME_COMMUNITY_LEADER, PASSWORD_COMMUNIY_LEADER, 1, false);
			Community.addNewCommunity(l.getDbCon(), "test", "Test Community");
			UserAccessFilter.addNewUserAccessFilter(l.getDbCon(), 0, 1, EditUser.LOCATION + ".*", 1, 0, 0);
			UserAccessFilter.addNewUserAccessFilter(l.getDbCon(), 0, 1, EditUserAccess.LOCATION + ".*", 1, 0, 0);

			// Adding normal Admin
			URIBuilder uB1 = new URIBuilder(
					baseUrl + "/" + EditUser.LOCATION + "." + EditUser.FUNCTION_NAME_CREATE_USER);
			uB1.addParameter(User.DB_TABLE_COLUMN_NAME_USERNAME, USERNAME_NORMAL_ADMIN);
			uB1.addParameter(User.DB_TABLE_COLUMN_NAME_PASSWORD, PASSWORD_NORMAL_ADMIN);
			uB1.addParameter(User.DB_TABLE_COLUMN_NAME_COMMUNITY_ID, "1");

			assertTrue(
					httpRequestAsUser(0, USERNAME_COMMUNITY_LEADER, PASSWORD_COMMUNIY_LEADER, baseUrl, uB1.toString())
							.trim().equals(FunctionResult.Status.OK.name()));

			// try logging in:
			URIBuilder uB2 = new URIBuilder(baseUrl + "/" + Index.INDEX_LOCATION + "." + Index.FUNCTION_NAME_LOGIN);
			uB2.addParameter(Index.LOGIN_FORM_NAME_USERNAME, USERNAME_NORMAL_ADMIN);
			uB2.addParameter(Index.LOGIN_FORM_NAME_PASSWORD, PASSWORD_NORMAL_ADMIN);
			uB2.addParameter(Index.LOGIN_FORM_NAME_LOCATION, "0");
			assertTrue(httpRequest(uB2.toString()).trim().equals(FunctionResult.Status.OK.name()));

			// allow Admin to search:
			URIBuilder uB3 = new URIBuilder(baseUrl + "/" + EditUserAccess.LOCATION + "."
					+ EditUserAccess.FUNCTION_NAME_CREATE_USER_ACCESS_FILTER);
			uB3.addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID, 0 + "");
			uB3.addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_USER_ID, 2 + "");
			uB3.addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_PATH, Search.LOCATION);
			uB3.addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_COMMUNITY_ID, 1 + "");
			uB3.addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_GAME_ID, "0");
			uB3.addParameter(UserAccessFilter.DB_TABLE_COLUMN_NAME_ASSET_ID, "0");

			assertTrue(!
					httpRequestAsUser(0, USERNAME_COMMUNITY_LEADER, PASSWORD_COMMUNIY_LEADER, baseUrl, uB3.toString())
							.trim().equals(FunctionResult.Status.OK.name()));

			UserAccessFilter.addNewUserAccessFilter(l.getDbCon(), 0, 1, Search.LOCATION + ".*", 1, 0, 0);
			
			assertTrue(
					httpRequestAsUser(0, USERNAME_COMMUNITY_LEADER, PASSWORD_COMMUNIY_LEADER, baseUrl, uB3.toString())
							.trim().equals(FunctionResult.Status.OK.name()));

			assertTrue(httpRequestAsUser(0, USERNAME_NORMAL_ADMIN, PASSWORD_NORMAL_ADMIN, baseUrl,
					baseUrl + "/" + Search.LOCATION).contains(Search.TITLE));

			l.stop_join();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void testSettingUpRemoteCommunity() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());
		try {

			JSONObject jData = getRandomDbAndListeningConfig();
			String baseUrl = getIpAndPort(jData);

			MainHttpListener l = new MainHttpListener(jData);
			startHttpListener(l);

			DatabaseConnector.initializeDatabase(l.getDbCon());

			JSONObject jO = DummyConnection.getPlainDataObject();
			DummyConnection.addUser(jO, 1, USERNAME_NORMAL_ADMIN, PASSWORD_NORMAL_ADMIN, Arrays.asList(new Integer(1)),
					"something@a1.de");
			DummyConnection.addUser(jO, 2, USERNAME_COMMUNITY_LEADER, PASSWORD_COMMUNIY_LEADER,
					Arrays.asList(new Integer(2)), "erewr@a2.de");

			DummyConnection.addUserGroup(jO, 1, "normal Admin");
			DummyConnection.addUserGroup(jO, 2, "Community Staff");

			Location.addNewLocation(l.getDbCon(), "Location for Communtiy1", 1, jO.toString());

			Community.addNewCommunity(l.getDbCon(), "test", "Test Community");

			UserAccessFilter.addNewUserAccessFilter(l.getDbCon(), 1, 2, EditUser.LOCATION + ".*", 1, 0, 0);
			UserAccessFilter.addNewUserAccessFilter(l.getDbCon(), 1, 2, EditUserAccess.LOCATION + ".*", 1, 0, 0);

			// try logging in:
			URIBuilder uB2 = new URIBuilder(baseUrl + "/" + Index.INDEX_LOCATION + "." + Index.FUNCTION_NAME_LOGIN);
			uB2.addParameter(Index.LOGIN_FORM_NAME_USERNAME, USERNAME_NORMAL_ADMIN);
			uB2.addParameter(Index.LOGIN_FORM_NAME_PASSWORD, PASSWORD_NORMAL_ADMIN);
			uB2.addParameter(Index.LOGIN_FORM_NAME_LOCATION, "1");
			assertTrue(httpRequest(uB2.toString()).trim().equals(FunctionResult.Status.OK.name()));

			// allow Admin to search:
			URIBuilder uB3 = new URIBuilder(baseUrl + "/" + EditUserAccess.LOCATION + "."
					+ EditUserAccess.FUNCTION_NAME_CREATE_GROUP_ACCESS_FILTER);
			uB3.addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_LOCATION_ID, 1 + "");
			uB3.addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_USER_GROUP_ID, 1 + "");
			uB3.addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_PATH, Search.LOCATION);
			uB3.addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_COMMUNITY_ID, 1 + "");
			uB3.addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_GAME_ID, "0");
			uB3.addParameter(UserGroupAccessFilter.DB_TABLE_COLUMN_NAME_ASSET_ID, "0");

			assertTrue(!
					httpRequestAsUser(1, USERNAME_COMMUNITY_LEADER, PASSWORD_COMMUNIY_LEADER, baseUrl, uB3.toString())
							.trim().equals(FunctionResult.Status.OK.name()));

			UserGroupAccessFilter.addNewGroupAccessFilter(l.getDbCon(), 1, 2, Search.LOCATION + ".*", 1, 0, 0);

			assertTrue(
					httpRequestAsUser(1, USERNAME_COMMUNITY_LEADER, PASSWORD_COMMUNIY_LEADER, baseUrl, uB3.toString())
							.trim().equals(FunctionResult.Status.OK.name()));

			assertTrue(httpRequestAsUser(1, USERNAME_NORMAL_ADMIN, PASSWORD_NORMAL_ADMIN, baseUrl,
					baseUrl + "/" + Search.LOCATION).contains(Search.TITLE));

			l.stop_join();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
