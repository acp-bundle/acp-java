package co.clai.db.model;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import co.clai.db.DatabaseConnector;
import co.clai.db.DbValueType;
import co.clai.util.log.LoggingUtil;

public abstract class AbstractDbTable {

	private final String tableName;
	private final Map<String, DbValueType> columns;

	protected final Logger logger;

	public AbstractDbTable(String tableName, Map<String, DbValueType> columns) {
		this.tableName = tableName;
		this.columns = columns;

		logger = LoggingUtil.getDefaultLogger();
	}

	public String getTableName() {
		return tableName;
	}

	public Map<String, DbValueType> getColumns() {
		return columns;
	}

	public static void createTable(DatabaseConnector dbCon, AbstractDbTable t) {

		StringBuilder sb = new StringBuilder();

		sb.append("CREATE TABLE " + t.getTableName() + " ( ");

		for (Entry<String, DbValueType> e : t.getColumns().entrySet()) {
			switch (e.getValue()) {

			case STRING:
				sb.append(e.getKey() + " VARCHAR(8192), ");
				break;

			case BLOB:
				sb.append(e.getKey() + " BLOB, ");
				break;

			case INTEGER:
				sb.append(e.getKey() + " INTEGER, ");
				break;

			case REAL:
				sb.append(e.getKey() + " REAL, ");
				break;

			case TIMESTAMP:
				sb.append(e.getKey() + " LONG, ");
				break;

			default:
				throw new RuntimeException("unknown value type in AbstractDbTable::createTable");

			}
		}

		sb.append(" PRIMARY KEY ( id ));");

		dbCon.executeUpdatePreparedQuery(new ArrayList<>(), sb.toString());

		dbCon.executeUpdatePreparedQuery(new ArrayList<>(),
				"ALTER TABLE " + t.getTableName() + " MODIFY COLUMN id INT auto_increment");

	}
}
