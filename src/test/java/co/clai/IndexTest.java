package co.clai;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import co.clai.MainHttpListener;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.User;
import co.clai.module.FunctionResult;
import co.clai.util.log.LoggingUtil;
import junit.framework.TestCase;

public class IndexTest extends TestCase implements HttpTest {

	public IndexTest(String name) {
		super(name);
	}

	public void testInvokeAnonymous() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		JSONObject jData = getRandomDbAndListeningConfig();

		MainHttpListener l = new MainHttpListener(jData);

		DatabaseConnector.initializeDatabase(l.getDbCon());

		startHttpListener(l);

		String baseUrl = getIpAndPort(jData);

		logger.log(Level.INFO, "requesting \"\"");
		httpRequest(baseUrl);

		logger.log(Level.INFO, "requesting \"/\"");
		httpRequest(baseUrl + "/");

		logger.log(Level.INFO, "requesting \"/index\"");
		httpRequest(baseUrl + "/index");

		logger.log(Level.INFO, "requesting \"/index.login\"");
		httpRequest(baseUrl + "/index.login");

		logger.log(Level.INFO, "requesting \"/index.logout\"");
		httpRequest(baseUrl + "/index.logout");

		l.stop_join();
	}

	public void testLogin() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		JSONObject jData = getRandomDbAndListeningConfig();
		String baseUrl = getIpAndPort(jData);

		MainHttpListener l = new MainHttpListener(jData);

		DatabaseConnector.initializeDatabase(l.getDbCon());

		User.addNewLocalUser(l.getDbCon(), "correctUser", "correctPassword", 0, false);

		startHttpListener(l);

		logger.log(Level.INFO, "testing login with no params");
		assertTrue(httpRequest(baseUrl + "/index.login").trim().equals(FunctionResult.Status.INTERNAL_ERROR.name()));

		logger.log(Level.INFO, "testing wrong credentials");
		assertTrue(httpRequest(baseUrl + "/index.login?location=0&username=wrongUser&password=wrongPassword").trim()
				.equals(FunctionResult.Status.FAILED.name()));

		logger.log(Level.INFO, "testing correct credentials");
		assertTrue(httpRequest(baseUrl + "/index.login?location=0&username=correctUser&password=correctPassword").trim()
				.equals(FunctionResult.Status.OK.name()));

		logger.log(Level.INFO, "changing password");

		User thisUser = User.getUserByLocationId(l.getDbCon(), 0, 1);
		thisUser.setNewPassword(l.getDbCon(), "newPassword");

		logger.log(Level.INFO, "testing new changed credentials");
		assertTrue(httpRequest(baseUrl + "/index.login?location=0&username=correctUser&password=newPassword").trim()
				.equals(FunctionResult.Status.OK.name()));

		l.stop_join();
	}

}
