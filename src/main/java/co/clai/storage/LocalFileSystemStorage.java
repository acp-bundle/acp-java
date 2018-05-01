package co.clai.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Storage;
import co.clai.html.HtmlPage;
import co.clai.util.FileUtil;
import co.clai.util.ResourceUtil;
import co.clai.util.StringStringPair;
import co.clai.util.StringUtil;
import co.clai.util.ValueValuePair;
import co.clai.util.cache.Cache;
import co.clai.util.cache.ExpiringCache;

public class LocalFileSystemStorage extends AbstractStorage {

	// Date : Identifier
	private final Cache<List<DateIdPair>> dataCache;

	private final String path;

	private final String storageKey;

	protected final Storage storage;

	public LocalFileSystemStorage(Storage storage) {

		this.storage = storage;

		if (storage == null) {
			storageKey = null;
			dataCache = null;
			path = null;
		} else {
			storageKey = "local_file_system#" + storage.getKey();
			dataCache = new ExpiringCache<>(storageKey);

			JSONObject data = storage.getConfig();

			path = data.getString("path");
		}
	}

	@Override
	public String getStorageTypeName() {
		return "local_file_system";
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
		if (!StringUtil.isValidFilename(identifier)) {
			logger.log(Level.WARNING, "WARNING: someone tried to get file: \"" + path + "/" + identifier + "\"");
			return new byte[0];
		}

		return FileUtil.getFileAsByteArr(path + "/" + identifier);
	}

	@Override
	public List<ValueValuePair> getFileList(DatabaseConnector dbCon) {
		List<DateIdPair> fileList = getFileIdListByDates();

		List<ValueValuePair> retList = new ArrayList<>();
		for (DateIdPair e : fileList) {
			retList.add(new StringStringPair(e.getId(), e.getId()));
		}

		return retList;
	}

	@SuppressWarnings("static-method")
	protected Date getDateFromFile(File f) {
		try {
			return new Date(ResourceUtil.getCreationTime(f).toMillis());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<DateIdPair> getFileIdListByDates() {
		List<DateIdPair> fileList = dataCache.retrieve("");
		if (fileList == null) {
			File f = new File(path);

			fileList = new ArrayList<>();

			for (File f2 : f.listFiles()) {
				fileList.add(new DateIdPair(getDateFromFile(f2), f2.getName()));
			}
			Collections.sort(fileList, (p1, p2) -> p2.getDate().compareTo(p1.getDate()));
			dataCache.put("", fileList);
		}

		return fileList;
	}

	@Override
	public List<ValueValuePair> getFileListByDate(DatabaseConnector dbCon, Date from, Date to) {
		List<DateIdPair> fileList = getFileIdListByDates();

		List<ValueValuePair> retList = new ArrayList<>();

		for (DateIdPair pair : fileList) {
			Date pairDate = pair.getDate();
			if (pairDate.after(from) && pairDate.before(to)) {
				retList.add(pair);
			}
		}

		return retList;
	}

	@Override
	public List<StorageSearchEntry> searchForEntries(DatabaseConnector dbCon, String searchQuery) {
		List<ValueValuePair> pairList = getFileList(dbCon);
		List<StorageSearchEntry> retList = new ArrayList<>();

		for (ValueValuePair pair : pairList) {

			byte[] data = getData(pair.getId());

			if (data != null) {
				retList.addAll(getSearchResult(pair.getId(), pair.getName(), searchQuery, new String(data)));
			} else {
				logger.log(Level.WARNING, "file with id=\"" + pair.getId() + "\" does not exist!");
			}
		}

		return retList;
	}

	@Override
	public List<StorageSearchEntry> searchForEntries(DatabaseConnector dbCon, String searchQuery, Date start,
			Date end) {
		List<ValueValuePair> pairList = getFileListByDate(dbCon, start, end);
		List<StorageSearchEntry> retList = new ArrayList<>();

		for (ValueValuePair pair : pairList) {
			retList.addAll(
					getSearchResult(pair.getId(), pair.getName(), searchQuery, new String(getData(pair.getId()))));
		}

		return retList;
	}

	@Override
	public void pushIndex(DatabaseConnector dbCon, String identifier, String name, long timestamp, String clientIp) {
		throw new RuntimeException("Local File System storage cannot be pushed!");
	}

	@Override
	public void renderContent(HtmlPage p, String identifier) {
		p.writePre(new String(getData(identifier)));
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
