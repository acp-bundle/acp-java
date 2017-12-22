package co.clai.module;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import co.clai.AcpSession;
import co.clai.db.DatabaseConnector;
import co.clai.html.HtmlPage;

public class Statistics extends AbstractModule {

	public static final String LOCATION = "statistics";
	public static final String TITLE = "Show map statistics";

	public static final String FUNCTION_NAME_SHOW_STATS = "showStatistics";

	public Statistics(DatabaseConnector dbCon) {
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

		retMap.put(FUNCTION_NAME_SHOW_STATS, this::showStatistics);

		return retMap;
	}

	private FunctionResult showStatistics(AcpSession s, Map<String, String[]> parameters) {

		return new FunctionResult(FunctionResult.Status.OK, LOCATION);
	}

}
