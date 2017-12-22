package co.clai.module;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.reflections.Reflections;

import co.clai.db.DatabaseConnector;
import co.clai.util.log.LoggingUtil;

import java.util.Set;

public class ModuleUtil {

	private static final Map<String, List<String>> moduleFunctionMap = loadModuleFunctionMap();
	private static final List<String> functionList = loadFunctionList(moduleFunctionMap);

	private static final Set<Class<? extends AbstractModule>> moduleList = loadModuleClasses();

	private static Set<Class<? extends AbstractModule>> loadModuleClasses() {

		Reflections reflections = new Reflections("co.clai.module");

		Set<Class<? extends AbstractModule>> retSet = reflections.getSubTypesOf(AbstractModule.class);

		for (Class<? extends AbstractModule> c : retSet) {
			LoggingUtil.createLoggerForModule(c);
		}

		return retSet;
	}

	public static Set<Class<? extends AbstractModule>> getModuleClasses() {
		return moduleList;
	}

	private static Map<String, List<String>> loadModuleFunctionMap() {
		Set<Class<? extends AbstractModule>> allClasses = new Reflections("co.clai.module")
				.getSubTypesOf(AbstractModule.class);

		Map<String, List<String>> retMap = new HashMap<>();

		for (Class<? extends AbstractModule> c : allClasses) {

			List<String> tmpList = null;
			String name = null;
			try {
				Constructor<? extends AbstractModule> cons = c.getConstructor(DatabaseConnector.class);
				AbstractModule m = cons.newInstance(new Object[] { null });

				tmpList = m.getFunctionList();

				if (tmpList == null) {
					throw new RuntimeException("Module " + m.getModuleName() + " returns null as functionList");
				}

				name = m.getModuleName();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			retMap.put(name, tmpList);
		}

		return retMap;
	}

	private static List<String> loadFunctionList(Map<String, List<String>> tmpModuleFunctionMap) {
		final List<String> retList = new ArrayList<>();
		for (Entry<String, List<String>> s : tmpModuleFunctionMap.entrySet()) {
			String moduleName = s.getKey();
			for (String f : s.getValue()) {
				retList.add(moduleName + "." + f);
			}
		}
		return retList;
	}

	public static Set<String> getModules() {
		return moduleFunctionMap.keySet();
	}

	public static List<String> getFunctionList() {
		return functionList;
	}

}
