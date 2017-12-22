package co.clai;

import java.util.Arrays;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import co.clai.remote.DummyConnection;
import co.clai.util.log.LoggingUtil;
import junit.framework.TestCase;

public class LocationTest extends TestCase {

	public LocationTest(String name) {
		super(name);
	}

	/*
	 * public void testUtil() { logger.log(Level.INFO, "running test " + getName());
	 * 
	 * AbstractRemoteConnection l =
	 * AbstractRemoteConnection.getRemoteFromLocation(DummyConnection.
	 * getPlainDataObject());
	 * 
	 * assertTrue(l instanceof DummyConnection);
	 * 
	 * DummyConnection dumCon = (DummyConnection) l; logger.log(Level.INFO,
	 * "class: " + dumCon.getClass());
	 * 
	 * logger.log(Level.INFO, l.getConfig().toString(4));
	 * 
	 * assertTrue(true); }
	 */

	public void testDummyConnection() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		JSONObject jO = DummyConnection.getPlainDataObject();
		DummyConnection.addUser(jO, 1, "test1User", "pwd1", Arrays.asList(new Integer(1), new Integer(2)),
				"something@a1.de");
		DummyConnection.addUser(jO, 2, "test2User", "pwd2", Arrays.asList(new Integer(2), new Integer(3)),
				"erewr@a2.de");

		DummyConnection.addUserGroup(jO, 1, "uGroup1");
		DummyConnection.addUserGroup(jO, 2, "userGroup2");
		DummyConnection.addUserGroup(jO, 3, "uGroup3_etc");

		logger.log(Level.INFO, jO.toString(4));

		DummyConnection c = new DummyConnection(jO);

		logger.log(Level.INFO, "Username user 1 by ID: " + c.getUserDataByUserId(1).getUsername());

		logger.log(Level.INFO,
				"User email user \"test2User\" by Username: " + c.getUserDataByUserName("test2User").getEmail());

		logger.log(Level.INFO, "User Group 2 name: " + c.getUsergroupNameById(2));

		System.out.print("User groups from user 2: ");
		for (Integer i : c.getUsergroupsFromUserId(2)) {
			System.out.print(i + ", ");
		}
		System.out.println();
	}
}
