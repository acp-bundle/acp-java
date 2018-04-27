package co.clai.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import co.clai.AcpSession;
import co.clai.db.DatabaseConnector;
import co.clai.util.log.LoggingUtil;
import co.clai.access.AccessibleHelper;
import co.clai.access.AccessibleModuleHelper;

public abstract class AbstractModule {

	protected final String MESSAGE_GET_VAR = "message";

	protected final String name;
	protected final DatabaseConnector dbCon;
	protected final Logger logger;
	protected final Logger userLog;

	private final AccessibleHelper accessibleHelper;

	public AbstractModule(String name, DatabaseConnector dbCon) {
		this(name, dbCon, new AccessibleModuleHelper(name));
	}

	public AbstractModule(String name, DatabaseConnector dbCon, AccessibleHelper accessibleHelper) {
		this.accessibleHelper = accessibleHelper;
		this.name = name;
		this.dbCon = dbCon;
		logger = LoggingUtil.getLoggerFromModule(getClass());
		userLog = LoggingUtil.getUserLogFromModule(getClass());
	}

	public String getModuleName() {
		return name;
	}

	public final byte[] invoke(final HttpServletResponse response, AcpSession s, String function,
			Map<String, String[]> parameters) {
		try {
			if ((function == null) || function.equals("")) {
				byte[] result = invokePlain(s, parameters);
				if (result == null) {
					return "invoke plain returns null".getBytes();
				}
				return result;
			}

			StringBuilder logEntry = new StringBuilder();
			if (s.getThisUser() != null) {
				logEntry.append(s.getThisUser().getLocationId() + ":" + s.getThisUser().getId() + ":"
						+ s.getThisUser().getUsername() + ": ");
			} else {
				logEntry.append(-1 + ":" + -1 + ":Unknown: ");
			}
			logEntry.append(getModuleName() + "." + function + " ");

			JSONObject logData = new JSONObject();

			for (Entry<String, String[]> e : parameters.entrySet()) {
				if (e.getKey().contains("password")) {
					logData.put(e.getKey(), "*");
				} else {
					logData.put(e.getKey(), e.getValue()[0]);
				}
			}

			logEntry.append(logData.toString());

			userLog.log(Level.INFO, logEntry.toString());

			BiFunction<AcpSession, Map<String, String[]>, FunctionResult> f = functionMap.get(function);

			if (f == null) {
				return FunctionResult.Status.NOT_FOUND.name().getBytes();
			}

			FunctionResult r = f.apply(s, parameters);

			URIBuilder b = r.getBuilder();
			if (r.getStatus() != FunctionResult.Status.NONE) {
				b.addParameter(MESSAGE_GET_VAR, r.getMessage());
			}

			if (response != null) {
				response.addHeader("Location", b.build().toString());
				response.setStatus(HttpServletResponse.SC_FOUND);
			}

			return r.getStatus().name().getBytes();
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage().getBytes();
		}
	}

	protected abstract byte[] invokePlain(AcpSession s, Map<String, String[]> parameters);

	protected final Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> functionMap = loadFunctions();

	protected abstract Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions();

	public List<String> getFunctionList() {

		if (functionMap == null) {
			throw new RuntimeException("functionMap from " + getModuleName() + " has function Map null");
		}

		List<String> retList = new ArrayList<>();

		for (String s : functionMap.keySet()) {
			retList.add(s);
		}
		return retList;
	}

	public AccessibleHelper getAccessibleHelper() {
		return accessibleHelper;
	}

}
