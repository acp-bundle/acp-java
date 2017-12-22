package co.clai.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Storage;
import co.clai.util.StringStringPair;
import co.clai.util.ValueValuePair;
import co.clai.util.cache.Cache;
import co.clai.util.cache.ExpiringCache;

public class SingleHttpRemoteStorage extends AbstractStorage {

	private static final String GENERIC_IDENTIFIER = "file";

	private final Cache<byte[]> dataCache;

	private final String JSON_CONFIG_KEY_URL = "url";
	private final String JSON_CONFIG_KEY_HTTPUSER = "httpUser";
	private final String JSON_CONFIG_KEY_HTTPPWD = "httpPwd";

	private final Storage storage;

	private final boolean useHttpPwd;
	private final String url;
	private final String httpUser;
	private final String httpPwd;

	public SingleHttpRemoteStorage(Storage storage) {
		this.storage = storage;

		if ((storage == null) || (storage.getConfig() == null)) {
			useHttpPwd = false;
			url = null;
			httpUser = null;
			httpPwd = null;
			dataCache = null;
		} else {
			JSONObject config = storage.getConfig();
			url = config.getString(JSON_CONFIG_KEY_URL);
			dataCache = new ExpiringCache<>(url);
			if (config.has(JSON_CONFIG_KEY_HTTPUSER)) {
				useHttpPwd = true;
				httpUser = config.getString(JSON_CONFIG_KEY_HTTPUSER);
				httpPwd = config.getString(JSON_CONFIG_KEY_HTTPPWD);
			} else {
				useHttpPwd = false;
				httpUser = null;
				httpPwd = null;
			}
		}
	}

	@Override
	public String getStorageTypeName() {
		return "single_http_remote";
	}

	@Override
	public boolean needsApproval() {
		return false;
	}

	@Override
	public boolean isCloud() {
		return false;
	}

	@Override
	public byte[] getData(String identifier) {
		return getFileContent();
	}

	@Override
	public List<ValueValuePair> getFileListByDate(DatabaseConnector dbCon, Date from, Date to) {
		return getFileList(dbCon);
	}

	@Override
	public List<ValueValuePair> getFileList(DatabaseConnector dbCon) {
		return Arrays.asList(new StringStringPair(GENERIC_IDENTIFIER, storage.getName()));
	}

	@Override
	public List<StorageSearchEntry> searchForEntries(DatabaseConnector dbCon, String regExp) {

		String content = new String(getFileContent());

		return getSearchResult(GENERIC_IDENTIFIER, storage == null ? "file" : storage.getName(), regExp, content);
	}

	private byte[] getFileContent() {

		if (useHttpPwd) {
			return requestCachedPOSTData(url, new ArrayList<>(), dataCache, httpUser, httpPwd);
		}
		return requestCachedPOSTData(url, new ArrayList<>(), dataCache);
	}

	@Override
	public List<StorageSearchEntry> searchForEntries(DatabaseConnector dbCon, String searchQuery, Date start,
			Date end) {
		return searchForEntries(dbCon, searchQuery);
	}

	@Override
	public void pushIndex(DatabaseConnector dbCon, String identifier, String name, long timestamp, String clientIp) {
		throw new RuntimeException("operation not supported");
	}

}
