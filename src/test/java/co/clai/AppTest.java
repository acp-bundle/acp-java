package co.clai;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import co.clai.MainHttpListener;
import co.clai.db.DatabaseConnector;
import co.clai.util.RandomUtil;
import co.clai.util.log.LoggingUtil;
import junit.framework.TestCase;

/**
 * Main class for testing
 */
public class AppTest extends TestCase implements HttpTest {

	public AppTest(String testName) {
		super(testName);
	}

	public void testHttpServer() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		JSONObject jData = getRandomDbAndListeningConfig();
		String url = getIpAndPort(jData);

		MainHttpListener l = new MainHttpListener(jData);
		startHttpListener(l);

		DatabaseConnector.initializeDatabase(l.getDbCon());

		httpRequest(url);

		l.stop_join();

		assertTrue(true);
	}

	public static void testBcrypt() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "generating Test Password:");
		logger.log(Level.INFO, BCrypt.hashpw("testLogin", BCrypt.gensalt()));
	}

	public static void testRandomString() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "generating Test Random String:");
		logger.log(Level.INFO, RandomUtil.getRandomString());

	}
}
