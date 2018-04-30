package co.clai.storage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import co.clai.db.model.Storage;

public class PRBF2DemoLocalStorage extends LocalFileSystemStorage {// 2017_03_21_20_07_25
	SimpleDateFormat[] POSSIBLE_DATE_FORMATS = new SimpleDateFormat[] { new SimpleDateFormat("yyyy_mm_dd_kk_mm_ss") };

	public PRBF2DemoLocalStorage(Storage storage) {
		super(storage);
	}

	@Override
	protected Date getDateFromFile(File f) {
		String dateString = f.getName().replace("auto_", "").replace(".bf2demo", "");

		for (SimpleDateFormat format : POSSIBLE_DATE_FORMATS) {
			try {
				Date date = format.parse(dateString);
				return date;
			} catch (Exception e) {
				e.getMessage();
			}
		}

		return super.getDateFromFile(f);
	}

	@Override
	public String getStorageTypeName() {
		return "local_file_system_prbf2_demo";
	}

	@Override
	public boolean forceDownload() {
		return true;
	}

	@Override
	public boolean isSearchable() {
		return false;
	}
}
