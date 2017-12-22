package co.clai;

import java.util.Arrays;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Community;
import co.clai.db.model.Location;
import co.clai.module.FunctionResult;
import co.clai.remote.DummyConnection;
import co.clai.util.log.LoggingUtil;
import junit.framework.TestCase;

public class RemoteLoginTest extends TestCase implements HttpTest {

	public RemoteLoginTest(String name) {
		super(name);
	}

	public void testLogin() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		JSONObject jData = getRandomDbAndListeningConfig();
		String baseUrl = getIpAndPort(jData);

		MainHttpListener l = new MainHttpListener(jData);
		startHttpListener(l);

		DatabaseConnector.initializeDatabase(l.getDbCon());

		JSONObject jO = DummyConnection.getPlainDataObject();
		DummyConnection.addUser(jO, 1, "test1User", "pwd1", Arrays.asList(new Integer(1), new Integer(2)),
				"something@a1.de");
		DummyConnection.addUser(jO, 2, "test2User", "pwd2", Arrays.asList(new Integer(2), new Integer(3)),
				"erewr@a2.de");

		DummyConnection.addUserGroup(jO, 1, "uGroup1");
		DummyConnection.addUserGroup(jO, 2, "userGroup2");
		DummyConnection.addUserGroup(jO, 3, "uGroup3_etc");

		Community.addNewCommunity(l.getDbCon(), "test1", "Test Community 1");

		Location.addNewLocation(l.getDbCon(), "Test Location", 1, jO.toString());

		logger.log(Level.INFO, "testing wrong credentials");
		assertTrue(httpRequest(baseUrl + "/index.login?location=1&username=test1User&password=wrongPassword").trim()
				.equals(FunctionResult.Status.FAILED.name()));

		logger.log(Level.INFO, "testing correct credentials");
		assertTrue(httpRequest(baseUrl + "/index.login?location=1&username=test1User&password=pwd1").trim()
				.equals(FunctionResult.Status.OK.name()));

	}
}
