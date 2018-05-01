package co.clai.storage;

import java.io.File;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.apache.http.client.utils.URIBuilder;

import co.clai.db.model.Storage;
import co.clai.db.model.StorageIndex;
import co.clai.module.Search;

public class PRBF2TrackerLocalStorage extends LocalFileSystemStorage {
	SimpleDateFormat[] POSSIBLE_DATE_FORMATS = new SimpleDateFormat[] { new SimpleDateFormat("yyyy_mm_dd_kk_mm_ss") };

	public PRBF2TrackerLocalStorage(Storage storage) {
		super(storage);
	}

	@Override
	protected Date getDateFromFile(File f) {
		try {
			String dateString = f.getName().replace("tracker_", "").substring(0, 19);

			for (SimpleDateFormat format : POSSIBLE_DATE_FORMATS) {
				try {
					Date date = format.parse(dateString);
					return date;
				} catch (Exception e) {
					e.getMessage();
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error parsing date " + f.getName() + ": " + e.getMessage());
		}

		return super.getDateFromFile(f);
	}

	@Override
	public String getViewLink(String siteURL, int storageId, String identifier) throws URISyntaxException {
		URIBuilder downloadBuilder = new URIBuilder(siteURL + "/" + Search.LOCATION);
		downloadBuilder.addParameter(Search.GET_PARAM, Search.GET_PARAM_VALUE_DOWNLOAD);
		downloadBuilder.addParameter(StorageIndex.DB_TABLE_COLUMN_NAME_STORAGE_ID, storageId + "");
		downloadBuilder.addParameter(StorageIndex.DB_TABLE_COLUMN_NAME_IDENTIFIER, identifier);

		URIBuilder bu = new URIBuilder("tracker_viewer/");
		bu.addParameter("demo", downloadBuilder.toString());

		return bu.toString();
	}

	@Override
	public String getStorageTypeName() {
		return "local_file_system_prbf2_tracker";
	}

	@Override
	public boolean forceDownload() {
		return false;
	}

	@Override
	public boolean isSearchable() {
		return false;
	}
}
