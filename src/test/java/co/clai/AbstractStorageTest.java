package co.clai;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import co.clai.db.model.Storage;
import co.clai.storage.AbstractStorage;
import co.clai.util.log.LoggingUtil;
import junit.framework.TestCase;

public class AbstractStorageTest extends TestCase {

	public AbstractStorageTest(String name) {
		super(name);
	}

	public void testGetTypes() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		List<AbstractStorage> storList = AbstractStorage.getAllAbstractStorage();

		for (AbstractStorage stor : storList) {
			logger.log(Level.INFO, "Has storage: " + stor.getStorageTypeName());
		}

		Storage stor = new Storage(1, "key", "name", 1, "log",
				"{\"type\":\"single_http_remote\",\"url\": \"https://www.example.com/\"}", false);

		AbstractStorage testStor = AbstractStorage.getRemoteFromLocation(stor);

		logger.log(Level.INFO, new String(testStor.getData("file")));

		Storage stor2 = new Storage(1, "key", "name", 1, "log",
				"{\"httpUser\":\"user\",\"httpPwd\":\"passwd\",\"type\":\"single_http_remote\",\"url\": \"https://httpbin.org/basic-auth/user/passwd\"}",
				false);

		AbstractStorage testStor2 = AbstractStorage.getRemoteFromLocation(stor2);

		logger.log(Level.INFO, new String(testStor2.getData("file")));

	}
}
