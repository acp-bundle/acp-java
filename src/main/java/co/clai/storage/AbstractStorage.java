package co.clai.storage;

import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.http.client.utils.URIBuilder;
import org.reflections.Reflections;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Storage;
import co.clai.db.model.StorageIndex;
import co.clai.html.HtmlPage;
import co.clai.module.Search;
import co.clai.remote.AbstractCachedQueryConnection;
import co.clai.util.ValueValuePair;

public abstract class AbstractStorage extends AbstractCachedQueryConnection {

	public static final String REMOTE_LOCATION_CONFIG_KEY_TYPE = "type";
	public static final String UNIQUE_ID_KEY = "uniqueId";

	private static final Map<String, Class<? extends AbstractStorage>> allStorages = loadAbstractStorage();

	private static Map<String, Class<? extends AbstractStorage>> loadAbstractStorage() {

		Map<String, Class<? extends AbstractStorage>> reMap = new HashMap<>();

		Reflections reflections = new Reflections("co.clai.storage");
		Set<Class<? extends AbstractStorage>> allClasses = reflections.getSubTypesOf(AbstractStorage.class);

		for (Class<? extends AbstractStorage> c : allClasses) {
			logger.log(Level.INFO, "loading abstract Storage class " + c.getName());
			String name = null;
			try {
				Constructor<? extends AbstractStorage> cons = c.getConstructor(Storage.class);
				AbstractStorage r = cons.newInstance(new Object[] { null });
				name = r.getStorageTypeName();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			logger.log(Level.INFO, "with name " + name);
			reMap.put(name, c);
		}

		return reMap;
	}

	public static List<AbstractStorage> getAllAbstractStorage() {

		List<AbstractStorage> retList = new ArrayList<>();

		Reflections reflections = new Reflections("co.clai.storage");

		Set<Class<? extends AbstractStorage>> allClasses = reflections.getSubTypesOf(AbstractStorage.class);

		for (Class<? extends AbstractStorage> c : allClasses) {
			try {
				Constructor<? extends AbstractStorage> cons = c.getConstructor(Storage.class);
				AbstractStorage m = cons.newInstance(new Object[] { null });

				retList.add(m);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

		return retList;
	}

	public static AbstractStorage getRemoteFromLocation(Storage stor) {
		Class<? extends AbstractStorage> c = allStorages
				.get(stor.getConfig().getString(REMOTE_LOCATION_CONFIG_KEY_TYPE));

		try {
			Constructor<? extends AbstractStorage> cons = c.getConstructor(Storage.class);

			return cons.newInstance(new Object[] { stor });
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error while creating RemoteConnection", e);
			return null;
		}
	}

	protected static List<StorageSearchEntry> getSearchResult(String identifier, String name, String regExp,
			String content) {
		List<StorageSearchEntry> retList = new ArrayList<>();

		for (String s : content.split("\n")) {
			if (s.contains(regExp) || s.matches(regExp)) {
				retList.add(new StorageSearchEntry(identifier, name, s));
			}
		}

		return retList;
	}

	public static List<String> getAllTypes() {
		List<String> retList = new ArrayList<>();
		retList.addAll(allStorages.keySet());
		return retList;
	}

	public abstract String getStorageTypeName();

	public abstract boolean needsApproval();

	public abstract boolean isCloud();

	public abstract byte[] getData(String identifier);

	public List<ValueValuePair> getFileList(DatabaseConnector dbCon) {
		return getFileListByDate(dbCon, new Date(0L), new Date(System.currentTimeMillis()));
	}

	public abstract List<ValueValuePair> getFileListByDate(DatabaseConnector dbCon, Date from, Date to);

	public abstract List<StorageSearchEntry> searchForEntries(DatabaseConnector dbCon, String searchQuery);

	public abstract List<StorageSearchEntry> searchForEntries(DatabaseConnector dbCon, String searchQuery, Date start,
			Date end);

	public abstract void pushIndex(DatabaseConnector dbCon, String identifier, String name, long timestamp,
			String clientIp);

	public void renderContent(HtmlPage p, String identifier) {
		p.writePre(new String(getData(identifier)));
	}

	public abstract boolean forceDownload();

	public abstract boolean isSearchable();

	@SuppressWarnings({ "static-method", "unused" })
	public String getViewLink(String siteURL, int storageId, String identifier) throws URISyntaxException {
		URIBuilder bu = new URIBuilder(Search.LOCATION);
		bu.addParameter(Search.GET_PARAM, Search.GET_PARAM_VALUE_VIEW);
		bu.addParameter(StorageIndex.DB_TABLE_COLUMN_NAME_STORAGE_ID, storageId + "");
		bu.addParameter(StorageIndex.DB_TABLE_COLUMN_NAME_IDENTIFIER, identifier);

		return bu.toString();
	}
}
