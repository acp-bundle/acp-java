package co.clai;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import co.clai.MainHttpListener;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Community;
import co.clai.db.model.User;
import co.clai.db.model.UserAccessFilter;
import co.clai.module.EditCommunity;
import co.clai.util.log.LoggingUtil;
import junit.framework.TestCase;

public class CommunityTest extends TestCase implements HttpTest {

	public CommunityTest(String name) {
		super(name);
	}

	public void testEditCommunityModuleAccess() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		JSONObject jData = getRandomDbAndListeningConfig();
		String baseUrl = getIpAndPort(jData);

		MainHttpListener l = new MainHttpListener(jData);

		DatabaseConnector.initializeDatabase(l.getDbCon());

		User.addNewLocalUser(l.getDbCon(), "normalUser", "correctPassword", 1, false);

		startHttpListener(l);

		Community.addNewCommunity(l.getDbCon(), "test1", "Test Community");
		Community.addNewCommunity(l.getDbCon(), "test2", "Test Community2");

		String listCommResult = httpRequestAsUser(0, "normalUser", "correctPassword", baseUrl,
				baseUrl + "/" + EditCommunity.LOCATION);

		assertTrue(!listCommResult.contains("Test Community"));

		User u = User.getUserByLocationName(l.getDbCon(), 0, "normalUser");
		UserAccessFilter.addNewUserAccessFilter(l.getDbCon(), 0, u.getId(), EditCommunity.LOCATION, 1, 0, 0);

		String listCommResult2 = httpRequestAsUser(0, "normalUser", "correctPassword", baseUrl,
				baseUrl + "/" + EditCommunity.LOCATION);

		assertTrue(listCommResult2.contains("<td>Test Community</td>"));
		assertTrue(!listCommResult2.contains("<td>Test Community2</td>"));

		UserAccessFilter.addNewUserAccessFilter(l.getDbCon(), 0, u.getId(), EditCommunity.LOCATION, 0, 0, 0);

		String listCommResult3 = httpRequestAsUser(0, "normalUser", "correctPassword", baseUrl,
				baseUrl + "/" + EditCommunity.LOCATION);

		assertTrue(listCommResult3.contains("<td>Test Community2</td>"));

		l.stop_join();
	}

	public void testEditCommunityModuleFunctions() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		JSONObject jData = getRandomDbAndListeningConfig();
		String baseUrl = getIpAndPort(jData);

		MainHttpListener l = new MainHttpListener(jData);

		DatabaseConnector.initializeDatabase(l.getDbCon());

		User.addNewLocalUser(l.getDbCon(), "normalUser", "correctPassword", 1, true);

		startHttpListener(l);

		httpRequestAsUser(0, "normalUser", "correctPassword", baseUrl,
				baseUrl + "/" + EditCommunity.LOCATION + "." + EditCommunity.FUNCTION_NAME_ADD_COMMUNITY + "?"
						+ Community.DB_TABLE_COLUMN_NAME_NAME + "=newTestCommunity&"
						+ Community.DB_TABLE_COLUMN_NAME_KEY + "=testNew");

		assertTrue(Community.getCommunityById(l.getDbCon(), 1).getName().equals("newTestCommunity"));

		String newFeatures = "{\"test\":\"value\"}";

		try {
			URIBuilder b = new URIBuilder(
					baseUrl + "/" + EditCommunity.LOCATION + "." + EditCommunity.FUNCTION_NAME_EDIT_FEATURES);
			b.addParameter(Community.DB_TABLE_COLUMN_NAME_ID, "1");
			b.addParameter(Community.DB_TABLE_COLUMN_NAME_FEATURES, newFeatures);

			httpRequestAsUser(0, "normalUser", "correctPassword", baseUrl, b.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		assertTrue(Community.getCommunityById(l.getDbCon(), 1).getFeatures().toString()
				.equals(new JSONObject(newFeatures).toString()));

		String newSettings = "{\"test\":\"value123\"}";

		try {
			URIBuilder b = new URIBuilder(
					baseUrl + "/" + EditCommunity.LOCATION + "." + EditCommunity.FUNCTION_NAME_EDIT_SETTINGS);
			b.addParameter(Community.DB_TABLE_COLUMN_NAME_ID, "1");
			b.addParameter(Community.DB_TABLE_COLUMN_NAME_SETTINGS, newSettings);

			httpRequestAsUser(0, "normalUser", "correctPassword", baseUrl, b.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		assertTrue(Community.getCommunityById(l.getDbCon(), 1).getSettings().toString()
				.equals(new JSONObject(newSettings).toString()));

		l.stop_join();
	}
}
