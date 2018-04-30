package co.clai.storage;

import java.io.File;
import java.util.Date;

import co.clai.db.model.Storage;

import java.text.SimpleDateFormat;

public class PRBF2LogLocalStorage extends LocalFileSystemStorage {

	SimpleDateFormat[] POSSIBLE_DATE_FORMATS = new SimpleDateFormat[] { new SimpleDateFormat("yyyymmdd_kkmm"),
			new SimpleDateFormat("yyyy-mm-dd_kkmm") };

	public PRBF2LogLocalStorage(Storage storage) {
		super(storage);
	}

	@Override
	protected Date getDateFromFile(File f) {
		if (f.getName().equals("cdhash.log") || f.getName().equals("ra_adminlog.txt")) {
			return new Date(System.currentTimeMillis());
		}

		String dateString = f.getName().replace("chatlog_", "").replace(".txt", "");

		for (SimpleDateFormat format : POSSIBLE_DATE_FORMATS) {
			try {
				Date date = format.parse(dateString);
				return date;
			} catch (Exception e) {
				e.getMessage(); // discard
			}
		}

		return super.getDateFromFile(f);
	}

	@Override
	public String getStorageTypeName() {
		return "local_file_system_prbf2_log";
	}

}
