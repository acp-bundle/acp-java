package co.clai;

import co.clai.access.AccessibleHelper;

import java.util.logging.Level;
import java.util.logging.Logger;

import co.clai.access.AccessFilter;
import co.clai.access.AccessibleFunctionHelper;
import co.clai.access.AccessibleModuleHelper;
import co.clai.access.GeneralAsset;
import co.clai.util.log.LoggingUtil;
import junit.framework.TestCase;

public class PermissionTest extends TestCase {

	public PermissionTest(String testName) {
		super(testName);
	}

	public void testAccessibleAccess() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		AccessibleHelper a1 = new AccessibleHelper(false);

		assertTrue(a1.hasAccess(new AccessFilter("")) == false);

		AccessibleHelper a2 = new AccessibleHelper(true);

		assertTrue(a2.hasAccess(null) == true);
	}

	public void testAccessibleModuleAccess() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		AccessibleModuleHelper m1 = new AccessibleModuleHelper("m1");
		AccessibleModuleHelper m2 = new AccessibleModuleHelper("m2");

		AccessFilter f1 = new AccessFilter("m1");
		AccessFilter f2 = new AccessFilter("m2.*");

		assertTrue(m1.hasAccess(f1) == true);
		assertTrue(m2.hasAccess(f1) == false);

		assertTrue(m1.hasAccess(f2) == false);
		assertTrue(m2.hasAccess(f2) == true);
	}

	public void testAccessibleFunctionAccess() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		AccessibleFunctionHelper fu1 = new AccessibleFunctionHelper("m1", "do1");
		AccessibleFunctionHelper fu2 = new AccessibleFunctionHelper("m1", "do2");
		AccessibleFunctionHelper fu3 = new AccessibleFunctionHelper("m2", "do3");

		AccessFilter f1 = new AccessFilter("m1.*");
		AccessFilter f2 = new AccessFilter("m1.do2");
		AccessFilter f3 = new AccessFilter("m2");

		assertTrue(fu1.hasAccess(f1) == true);
		assertTrue(fu2.hasAccess(f1) == true);
		assertTrue(fu3.hasAccess(f1) == false);

		assertTrue(fu1.hasAccess(f2) == false);
		assertTrue(fu2.hasAccess(f2) == true);
		assertTrue(fu3.hasAccess(f2) == false);

		assertTrue(fu1.hasAccess(f3) == false);
		assertTrue(fu2.hasAccess(f3) == false);
		assertTrue(fu3.hasAccess(f3) == false);
	}

	public void testAccessibleFunctionAssetAccess() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		GeneralAsset a1 = new GeneralAsset(1, 0);
		GeneralAsset a2 = new GeneralAsset(2, 1);
		GeneralAsset a3 = new GeneralAsset(3, 2);

		AccessibleFunctionHelper fu1 = new AccessibleFunctionHelper("m1", "do1");
		AccessibleFunctionHelper fu2 = new AccessibleFunctionHelper("m1", "do2");
		AccessibleFunctionHelper fu3 = new AccessibleFunctionHelper("m2", "do3");
		AccessibleModuleHelper m1 = new AccessibleModuleHelper("m1");
		AccessibleModuleHelper m2 = new AccessibleModuleHelper("m2");

		AccessFilter f1 = new AccessFilter("m1.*", 0, 1, 0); // All Assets of
																// community 1
		AccessFilter f2 = new AccessFilter("m1.do2", 0, 1, 0); // All Assets of
																// community 1
		AccessFilter f3 = new AccessFilter("m2", 3, 2, 0); // Assets of
															// community 2
		AccessFilter f4 = new AccessFilter("m1.*");

		assertTrue(fu1.hasAccess(f1, a1) == false);
		assertTrue(fu1.hasAccess(f1, a2) == true);
		assertTrue(fu1.hasAccess(f1, a3) == false);

		assertTrue(fu2.hasAccess(f1, a1) == false);
		assertTrue(fu2.hasAccess(f1, a2) == true);
		assertTrue(fu2.hasAccess(f1, a3) == false);

		assertTrue(fu3.hasAccess(f1, a1) == false);
		assertTrue(fu3.hasAccess(f1, a2) == false);
		assertTrue(fu3.hasAccess(f1, a3) == false);

		assertTrue(fu1.hasAccess(f2, a1) == false);
		assertTrue(fu1.hasAccess(f2, a2) == false);
		assertTrue(fu1.hasAccess(f2, a3) == false);

		assertTrue(fu2.hasAccess(f2, a1) == false);
		assertTrue(fu2.hasAccess(f2, a2) == true);
		assertTrue(fu2.hasAccess(f2, a3) == false);

		assertTrue(fu3.hasAccess(f2, a1) == false);
		assertTrue(fu3.hasAccess(f2, a2) == false);
		assertTrue(fu3.hasAccess(f2, a3) == false);

		assertTrue(fu1.hasAccess(f3, a1) == false);
		assertTrue(fu1.hasAccess(f3, a2) == false);
		assertTrue(fu1.hasAccess(f3, a3) == false);

		assertTrue(fu2.hasAccess(f3, a1) == false);
		assertTrue(fu2.hasAccess(f3, a2) == false);
		assertTrue(fu2.hasAccess(f3, a3) == false);

		assertTrue(fu3.hasAccess(f3, a1) == false);
		assertTrue(fu3.hasAccess(f3, a2) == false);
		assertTrue(fu3.hasAccess(f3, a3) == false);

		assertTrue(fu1.hasAccess(f4, a1) == false);
		assertTrue(fu1.hasAccess(f4, a2) == false);
		assertTrue(fu1.hasAccess(f4, a3) == false);

		assertTrue(fu2.hasAccess(f4, a1) == false);
		assertTrue(fu2.hasAccess(f4, a2) == false);
		assertTrue(fu2.hasAccess(f4, a3) == false);

		assertTrue(fu3.hasAccess(f4, a1) == false);
		assertTrue(fu3.hasAccess(f4, a2) == false);
		assertTrue(fu3.hasAccess(f4, a3) == false);

		assertTrue(m1.hasAccess(f4) == true);
		assertTrue(m2.hasAccess(f4) == false);

		assertTrue(true);
	}

}
