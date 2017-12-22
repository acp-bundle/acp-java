package co.clai.game;

import java.util.List;
import java.util.Map;

import co.clai.AcpSession;
import co.clai.db.DatabaseConnector;
import co.clai.db.model.Server;
import co.clai.db.model.ServerSetting;
import co.clai.html.Builder;
import co.clai.util.StringStringPair;

public abstract class AbstractGame {

	protected final String key;
	protected final String name;

	protected AbstractGame(String key, String name) {
		this.key = key;
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public abstract List<StringStringPair> getAvailableOptions();

	public abstract Builder renderOption(AcpSession s, ServerSetting setting);

	public abstract List<StringStringPair> getAvailableQueries();

	public abstract String executeQuery(DatabaseConnector dbCon, String command, Server server,
			Map<String, String[]> parameters, AcpSession session);

	public abstract String getDefaultOption(String settingKey);

}
