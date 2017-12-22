package co.clai.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.reflections.Reflections;

import co.clai.db.model.AbstractDbTable;
import co.clai.util.log.LoggingUtil;

public class DbUtil {

	private final static Logger logger = LoggingUtil.getDefaultLogger();

	public static Set<Class<? extends AbstractDbTable>> getTableClasses() {

		Reflections reflections = new Reflections("co.clai.db.model");

		return reflections.getSubTypesOf(AbstractDbTable.class);
	}

	public static List<AbstractDbTable> getTableSet() {

		Set<Class<? extends AbstractDbTable>> allClasses = getTableClasses();

		List<AbstractDbTable> retList = new ArrayList<>();

		for (Class<? extends AbstractDbTable> c : allClasses) {

			try {
				logger.log(Level.INFO, "instanciating " + c.getName());
				retList.add(c.newInstance());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

		return retList;

	}

	public static void createAllTables(DatabaseConnector dbCon) {
		List<AbstractDbTable> tables = DbUtil.getTableSet();

		for (AbstractDbTable t : tables) {
			logger.log(Level.INFO, "creating table " + t.getTableName());
			AbstractDbTable.createTable(dbCon, t);
		}
	}

}
