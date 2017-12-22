package co.clai.util.cache;

public class ExpiringCacheEntry extends CacheEntry {

	private final String key;
	private final long createdTimeStamp;
	private final int position;

	ExpiringCacheEntry(Class<? extends Object> type, Object data, int position, String key) {
		super(type, data);

		this.position = position;
		createdTimeStamp = System.currentTimeMillis();
		this.key = key;
	}

	public long getCreatedTimeStamp() {
		return createdTimeStamp;
	}

	public int getPosition() {
		return position;
	}

	public String getKey() {
		return key;
	}

}
