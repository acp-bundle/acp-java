package co.clai.db;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import co.clai.util.StringUtil;

public class DbValue {

	private final DbValueType dbValueType;

	public DbValueType getDbValueType() {
		return dbValueType;
	}

	private final Object data;

	public DbValue(Blob b) throws SQLException {
		this(StringUtil.byteArrFromInputStream(b.getBinaryStream()), DbValueType.BLOB);
	}

	public DbValue(String s) {
		this(s, DbValueType.STRING);
	}

	public DbValue(int i) {
		this(new Integer(i), DbValueType.INTEGER);
	}

	public DbValue(double d) {
		this(new Double(d), DbValueType.REAL);
	}

	public DbValue(Timestamp t) {
		this(t, DbValueType.TIMESTAMP);
	}

	public DbValue(Object data, DbValueType dbValueType) {
		this.data = data;
		this.dbValueType = dbValueType;
	}

	public String getString() {
		if (dbValueType != DbValueType.STRING) {
			throw new RuntimeException("DbValue is not String in DBValue::getString");
		}
		return (String) data;
	}

	public Integer getInteger() {
		if (dbValueType != DbValueType.INTEGER) {
			throw new RuntimeException("DbValue is not Integer in DBValue::getInteger");
		}
		return (Integer) data;
	}

	public boolean getIntegerAsBool() {
		return (getInt() != 0);
	}

	public int getInt() {
		if (dbValueType != DbValueType.INTEGER) {
			throw new RuntimeException("DbValue is not Integer in DBValue::getInteger");
		}
		return ((Integer) data).intValue();
	}

	public Double getReal() {
		if (dbValueType != DbValueType.REAL) {
			throw new RuntimeException("DbValue is not REAL in DBValue::getREAL");
		}
		return (Double) data;
	}

	public double getDouble() {
		if (dbValueType != DbValueType.REAL) {
			throw new RuntimeException("DbValue is not REAL in DBValue::getREAL");
		}
		return ((Double) data).doubleValue();
	}

	public Timestamp getTimestamp() {
		if (dbValueType != DbValueType.TIMESTAMP) {
			throw new RuntimeException("DbValue is not Timestamp in DBValue::getTimestamp");
		}
		return (Timestamp) data;
	}

	public Blob getBlob(Connection con) {
		if (dbValueType != DbValueType.BLOB) {
			throw new RuntimeException("DbValue is not Blob in DBValue::getString");
		}

		Blob blob;
		try {
			blob = con.createBlob();
			blob.setBytes(1, (byte[]) data);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return blob;
	}

	public String getBlobAsString() {
		if (dbValueType != DbValueType.BLOB) {
			throw new RuntimeException("DbValue is not Blob in DBValue::getString");
		}

		return new String((byte[]) data);
	}

	public byte[] getBlobAsByteArr() {
		if (dbValueType != DbValueType.BLOB) {
			throw new RuntimeException("DbValue is not Blob in DBValue::getBlobAsByteArr");
		}

		return (byte[]) data;
	}

	public static DbValue newBlob(String config) {
		return new DbValue(config.getBytes(), DbValueType.BLOB);
	}

	public static DbValue newBooleanAsInteger(boolean data) {
		if (data) {
			return new DbValue(1);
		}
		return new DbValue(0);
	}
}
