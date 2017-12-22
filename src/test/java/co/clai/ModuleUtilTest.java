package co.clai;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import co.clai.db.DatabaseConnector;
import co.clai.module.AbstractModule;
import co.clai.module.ModuleUtil;
import co.clai.util.log.LoggingUtil;
import junit.framework.TestCase;

public class ModuleUtilTest extends TestCase {

	public ModuleUtilTest(String name) {
		super(name);
	}

	public void testLoadingFunctions() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		logger.log(Level.INFO, "getting Module List:");

		Set<String> modules = ModuleUtil.getModules();
		for (String s : modules) {
			logger.log(Level.INFO, "Module: " + s);
		}

		logger.log(Level.INFO, "getting Function List:");

		List<String> functions = ModuleUtil.getFunctionList();
		for (String s : functions) {
			logger.log(Level.INFO, "Function: " + s);
		}
	}

	public void testGetModuleClasses() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "running test " + getName());

		Set<Class<? extends AbstractModule>> allModules = ModuleUtil.getModuleClasses();

		for (Class<? extends AbstractModule> m : allModules) {
			if (!Modifier.isAbstract(m.getModifiers())) {
				Constructor<? extends AbstractModule> cons;
				try {
					cons = m.getConstructor(DatabaseConnector.class);
					AbstractModule module = cons.newInstance((DatabaseConnector) null);
					logger.log(Level.INFO, "Module: " + module.getModuleName());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

}
