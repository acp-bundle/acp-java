package co.clai.storage;

import java.util.Date;

import co.clai.util.ValueValuePair;

public class DateIdPair extends ValueValuePair {

	private final Date date;
	private final String id;

	public DateIdPair(Date date, String id) {
		this.date = date;
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return id;
	}

}
