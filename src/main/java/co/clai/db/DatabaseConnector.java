package co.clai.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.logging.Level;

import org.json.JSONObject;

import co.clai.MainHttpListener;
import co.clai.db.model.Game;
import co.clai.util.log.LoggingUtil;
import co.clai.db.DbValue;
import co.clai.db.DbValueType;

public class DatabaseConnector {

	private final String dbPath;
	private final String dbUser;
	private final String dbPassword;

	private final MainHttpListener listener;

	private final java.util.logging.Logger logger = LoggingUtil.getDefaultLogger();

	public DatabaseConnector(MainHttpListener listener, JSONObject dbConfig) {
		dbPath = dbConfig.getString("path");
		dbUser = dbConfig.getString("username");
		dbPassword = dbConfig.getString("password");

		this.listener = listener;
	}

	private Connection openConnection() throws SQLException {
		return DriverManager.getConnection(dbPath, dbUser, dbPassword);
	}

	public void executeUpdatePreparedQuery(List<DbValue> values, String preparedStatementString) {
		try (Connection con = openConnection();
				PreparedStatement stmt = con.prepareStatement(preparedStatementString)) {

			addParametersToStatement(values, stmt, con);

			logger.log(Level.INFO, stmt.toString());

			stmt.executeUpdate();
			// con.commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void insert(String dbName, List<String> columns, List<DbValue> values) {
		StringBuilder sb = new StringBuilder();

		sb.append("INSERT INTO " + dbName + " (");

		sb.append(generateCommaSeperatedList(columns));

		sb.append(") VALUES (");

		sb.append(generateCommaSeperatedQuestionMarks(values.size()));

		sb.append(")");

		executeUpdatePreparedQuery(values, sb.toString());
	}

	public List<Map<String, DbValue>> select(String dbName, Map<String, DbValueType> returnTypes) {
		return select(dbName, Arrays.asList(), Arrays.asList(), returnTypes);
	}

	public List<Map<String, DbValue>> select(String dbName, String columns, DbValue selector,
			Map<String, DbValueType> returnTypes) {
		return select(dbName, Arrays.asList(columns), Arrays.asList(selector), returnTypes);
	}

	public List<Map<String, DbValue>> select(String dbName, List<String> columns, List<DbValue> selector,
			Map<String, DbValueType> returnTypes) {
		return select(dbName, columns, selector, returnTypes, "");
	}

	public List<Map<String, DbValue>> select(String dbName, String columns, DbValue selector,
			Map<String, DbValueType> returnTypes, String append) {
		return select(dbName, Arrays.asList(columns), Arrays.asList(selector), returnTypes, append);
	}

	public List<Map<String, DbValue>> select(String dbName, List<String> columns, List<DbValue> selector,
			Map<String, DbValueType> returnTypes, String append) {
		StringBuilder sb = new StringBuilder();

		sb.append("SELECT * FROM " + dbName + " WHERE ");

		for (String c : columns) {
			sb.append(c + " = ? AND ");
		}

		sb.append(" TRUE " + (append == null ? "" : append) + " ;");

		List<Map<String, DbValue>> retList = new ArrayList<>();

		try (Connection con = openConnection(); PreparedStatement stmt = con.prepareStatement(sb.toString())) {

			addParametersToStatement(selector, stmt, con);

			logger.log(Level.INFO, stmt.toString());

			try (ResultSet rs = stmt.executeQuery()) {
				ResultSetMetaData rsmd = rs.getMetaData();

				Map<String, Integer> columnIdMap = new HashMap<>();

				for (int j = 1; j < (rsmd.getColumnCount() + 1); j++) {
					columnIdMap.put(rsmd.getColumnName(j), new Integer(j));
				}

				while (rs.next()) {
					Map<String, DbValue> tmpMap = new HashMap<>();

					for (Entry<String, DbValueType> e : returnTypes.entrySet()) {

						int columnNumber = columnIdMap.get(e.getKey()).intValue();

						switch (e.getValue()) {
						case BLOB:
							tmpMap.put(e.getKey(), new DbValue(rs.getBlob(columnNumber)));
							break;

						case STRING:
							tmpMap.put(e.getKey(), new DbValue(rs.getString(columnNumber)));
							break;

						case INTEGER:
							tmpMap.put(e.getKey(), new DbValue(rs.getInt(columnNumber)));
							break;

						case REAL:
							tmpMap.put(e.getKey(), new DbValue(rs.getDouble(columnNumber)));
							break;

						case TIMESTAMP:
							tmpMap.put(e.getKey(), new DbValue(new Timestamp(rs.getLong(columnNumber))));
							break;

						default:
							throw new RuntimeException("unknown value type in DatabaseConnector::executePreparedQuery");

						}
					}
					retList.add(tmpMap);

				}

			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return retList;
	}

	private static void addParametersToStatement(List<DbValue> selector, PreparedStatement stmt, Connection con)
			throws SQLException {
		int i = 1;
		for (DbValue v : selector) {
			switch (v.getDbValueType()) {
			case BLOB:
				stmt.setBlob(i, v.getBlob(con));
				break;

			case STRING:
				stmt.setString(i, v.getString());
				break;

			case INTEGER:
				stmt.setInt(i, v.getInt());
				break;

			case REAL:
				stmt.setDouble(i, v.getDouble());
				break;

			case TIMESTAMP:
				stmt.setLong(i, v.getTimestamp().getTime());
				break;

			default:
				throw new RuntimeException("unknown value type in DatabaseConnector::executePreparedQuery");
			}
			i++;
		}
	}

	private static String generateCommaSeperatedList(List<String> columns) {
		StringJoiner sj = new StringJoiner(",");

		for (String c : columns) {
			sj.add(c);
		}

		return sj.toString();
	}

	private static String generateCommaSeperatedQuestionMarks(int size) {
		StringJoiner sj = new StringJoiner(",");
		for (int i = 0; i < size; i++) {
			sj.add("?");
		}
		return sj.toString();
	}

	public void updateValue(String tableName, List<String> valueColumns, List<DbValue> valueValues,
			String selectorColumns, DbValue selectorIds) {
		updateValue(tableName, valueColumns, valueValues, Arrays.asList(selectorColumns), Arrays.asList(selectorIds));
	}

	public void updateValue(String tableName, List<String> valueColumns, List<DbValue> valueValues,
			List<String> selectorColumns, List<DbValue> selectorIds) {

		StringBuilder sb = new StringBuilder();

		sb.append("UPDATE " + tableName + " SET ");

		StringJoiner sj = new StringJoiner(" , ");

		for (String c : valueColumns) {
			sj = sj.add(c + " = ?");
		}

		sb.append(sj.toString());

		sb.append(" WHERE ");

		for (String c : selectorColumns) {
			sb.append(c + " = ? AND ");
		}

		sb.append(" TRUE;");

		String query = sb.toString();
		// logger.log(Level.INFO, query);

		try (Connection con = openConnection(); PreparedStatement stmt = con.prepareStatement(query)) {
			List<DbValue> allParams = new ArrayList<>();
			allParams.addAll(valueValues);
			allParams.addAll(selectorIds);

			addParametersToStatement(allParams, stmt, con);

			logger.log(Level.INFO, stmt.toString());

			stmt.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public void deleteFrom(String tableName, String selectorColumn, DbValue selectorIds) {
		deleteFrom(tableName, Arrays.asList(selectorColumn), Arrays.asList(selectorIds));
	}

	public void deleteFrom(String tableName, List<String> selectorColumns, List<DbValue> selectorIds) {

		StringBuilder sb = new StringBuilder();

		sb.append("DELETE FROM " + tableName + " WHERE ");

		for (String c : selectorColumns) {
			sb.append(c + " = ? AND ");
		}

		sb.append(" TRUE;");

		String query = sb.toString();

		try (Connection con = openConnection(); PreparedStatement stmt = con.prepareStatement(query)) {
			addParametersToStatement(selectorIds, stmt, con);

			logger.log(Level.INFO, stmt.toString());

			stmt.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public MainHttpListener getListener() {
		return listener;
	}

	public static void initializeDatabase(DatabaseConnector dbCon) {

		DbUtil.createAllTables(dbCon);

		Game.populateGameTable(dbCon);
	}

}
