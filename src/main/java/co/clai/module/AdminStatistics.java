package co.clai.module;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import co.clai.AcpSession;
import co.clai.db.DatabaseConnector;
import co.clai.html.HtmlPage;

public class AdminStatistics extends AbstractModule {

	public static final String LOCATION = "adminStats";
	public static final String TITLE = "Show admin statistics";

	public static final String FUNCTION_NAME_SHOW_STATS = "showAdminStatistics";

	public AdminStatistics(DatabaseConnector dbCon) {
		super(LOCATION, dbCon);
	}

	@Override
	protected byte[] invokePlain(AcpSession s, Map<String, String[]> parameters) {
		HtmlPage p = new HtmlPage(dbCon, TITLE, null, null, null, s);

		return p.finish().getBytes();
	}

	@Override
	protected Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> loadFunctions() {
		Map<String, BiFunction<AcpSession, Map<String, String[]>, FunctionResult>> retMap = new HashMap<>();

		retMap.put(FUNCTION_NAME_SHOW_STATS, this::showAdminStatistics);

		return retMap;
	}

	private FunctionResult showAdminStatistics(AcpSession s, Map<String, String[]> parameters) {

		return new FunctionResult(FunctionResult.Status.OK, LOCATION);
	}

}
