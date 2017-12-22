package co.clai.module;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.logging.Level;

import co.clai.AcpSession;
import co.clai.access.AccessibleHelper;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Location;
import co.clai.remote.AbstractRemoteConnection;
import co.clai.remote.OAuth2Helper;

public class OAuth2 extends AbstractModule {

	private static final String SESSION_KEY_OAUTH2_LOCATION = "oauth2Location";
	public static final String FUNCTION_NAME_CALLBACK = "callback";
	public static final String FUNCTION_NAME_LOGIN = "login";

	public static final String LOCATION = "oauth2";

	public OAuth2(DatabaseConnector dbCon) {
		super(LOCATION, dbCon, new AccessibleHelper(true));
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {
		// TODO Auto-generated method stub
		return "".getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> allFunctions = new HashMap<>();

		allFunctions.put(FUNCTION_NAME_LOGIN, this::login);
		allFunctions.put(FUNCTION_NAME_CALLBACK, this::callback);

		return allFunctions;
	}

	private FunctionResult login(AcpSession s, Map<String, String[]> parameters) {

		try {
			int locationId = Integer.parseInt(parameters.get(Location.DB_TABLE_COLUMN_NAME_ID)[0]);

			s.getSession().setAttribute(SESSION_KEY_OAUTH2_LOCATION, new Integer(locationId));

			AbstractRemoteConnection rl = AbstractRemoteConnection
					.getRemoteFromLocation(Location.getLocationById(dbCon, locationId));

			FunctionResult r = OAuth2Helper.generateRedirect(dbCon.getListener().getSiteUrl(), rl.getOAuth2Data());

			return r;
		} catch (Exception e) {
			logger.log(Level.INFO, "OAuth2 Error: ", e);
			return new FunctionResult(FunctionResult.Status.FAILED, Index.INDEX_LOCATION, "failed to get OAuth2 path!");
		}
	}

	private FunctionResult callback(AcpSession s, Map<String, String[]> parameters) {

		try {
			for (Entry<String, String[]> e : parameters.entrySet()) {
				logger.log(Level.INFO, e.getKey() + ":" + e.getValue()[0]);
			}

			int locationId = ((Integer) s.getSession().getAttribute(SESSION_KEY_OAUTH2_LOCATION)).intValue();
			s.getSession().removeAttribute(SESSION_KEY_OAUTH2_LOCATION);

			AbstractRemoteConnection c = AbstractRemoteConnection
					.getRemoteFromLocation(Location.getLocationById(dbCon, locationId));

			if (!c.canDoOAuth2Login()) {
				return new FunctionResult(FunctionResult.Status.FAILED, Index.INDEX_LOCATION,
						"Location does not Support OAuth2 login.");
			}

			s.setUser(c.getUserWithOAuth2Code(dbCon, parameters, locationId));

			return new FunctionResult(FunctionResult.Status.FAILED, Index.INDEX_LOCATION, "Successfully logged in!");

		} catch (Exception e) {
			e.printStackTrace();
			return new FunctionResult(FunctionResult.Status.FAILED, Index.INDEX_LOCATION, "Error during callback.");
		}
	}

}
