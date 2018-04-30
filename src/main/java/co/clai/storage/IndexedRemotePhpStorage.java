package co.clai.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Storage;
import co.clai.db.model.StorageIndex;
import co.clai.util.StringStringPair;
import co.clai.util.ValueValuePair;
import co.clai.util.cache.Cache;
import co.clai.util.cache.PermanentCache;

public class IndexedRemotePhpStorage extends AbstractStorage {

	private final Cache<byte[]> dataCache;

	private final String JSON_CONFIG_KEY_BASE_URL = "baseUrl";
	private final String JSON_CONFIG_KEY_HTTPUSER = "httpUser";
	private final String JSON_CONFIG_KEY_HTTPPWD = "httpPwd";
	private final String JSON_CONFIG_KEY_IP_WHITELIST = "ip_whitelist";

	private final Storage storage;

	private final boolean useHttpPwd;
	private final String baseUrl;
	private final String httpUser;
	private final String httpPwd;

	private final Set<String> ipWhiteList;

	public IndexedRemotePhpStorage(Storage storage) {
		this.storage = storage;

		if ((storage == null) || (storage.getConfig() == null)) {
			useHttpPwd = false;
			baseUrl = null;
			httpUser = null;
			httpPwd = null;
			dataCache = null;
			ipWhiteList = null;
		} else {
			if (!storage.isHasLocalIndex()) {
				logger.log(Level.WARNING, "storage is not indexed, no results will be produced");
			}

			JSONObject config = storage.getConfig();
			baseUrl = config.getString(JSON_CONFIG_KEY_BASE_URL);

			if (config.has(JSON_CONFIG_KEY_IP_WHITELIST)) {
				JSONArray jWhiteArr = config.getJSONArray(JSON_CONFIG_KEY_IP_WHITELIST);

				ipWhiteList = new HashSet<>();

				for (int i = 0; i < jWhiteArr.length(); i++) {
					ipWhiteList.add(jWhiteArr.getString(i));
				}
			} else {
				ipWhiteList = null;
			}

			/// TODO change this to database cache
			dataCache = new PermanentCache<>(baseUrl);
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
		return "indexed_remote_php";
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
		String url = baseUrl + identifier;

		if (useHttpPwd) {
			return requestCachedPOSTData(url, new ArrayList<>(), dataCache, httpUser, httpPwd);
		}
		return requestCachedPOSTData(url, new ArrayList<>(), dataCache);
	}

	@Override
	public List<ValueValuePair> getFileListByDate(DatabaseConnector dbCon, Date from, Date to) {
		List<ValueValuePair> retList = new ArrayList<>();

		List<StorageIndex> list = StorageIndex.getAllStorageIndexByStorageId(dbCon, storage.getId());

		for (StorageIndex stInd : list) {
			try {
				if (stInd.getDatetime().after(from) && stInd.getDatetime().before(to)) {
					retList.add(new StringStringPair(stInd.getIdentifier(), stInd.getName()));
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "Storage Index " + stInd.getId() + " has date null: " + e.getMessage());
			}
		}

		return retList;
	}

	@Override
	public List<StorageSearchEntry> searchForEntries(DatabaseConnector dbCon, String searchQuery) {
		return searchForEntries(dbCon, searchQuery, new Date(0L), new Date(System.currentTimeMillis()));
	}

	@Override
	public List<StorageSearchEntry> searchForEntries(DatabaseConnector dbCon, String searchQuery, Date start,
			Date end) {

		List<StorageSearchEntry> resultList = new ArrayList<>();

		List<ValueValuePair> fileList = getFileListByDate(dbCon, start, end);

		for (ValueValuePair pair : fileList) {
			resultList.addAll(
					getSearchResult(pair.getId(), pair.getName(), searchQuery, new String(getData(pair.getId()))));
		}

		return resultList;
	}

	@Override
	public void pushIndex(DatabaseConnector dbCon, String identifier, String name, long timestamp, String clientIp) {
		if ((ipWhiteList == null) || ipWhiteList.contains(clientIp)) {
			StorageIndex.addNewStorage(dbCon, name, identifier, storage.getId(), timestamp);
		} else {
			throw new RuntimeException("client IP " + clientIp + " not in whitelist: ");
		}
	}

	@Override
	public boolean forceDownload() {
		return false;
	}

	@Override
	public boolean isSearchable() {
		return true;
	}

}
