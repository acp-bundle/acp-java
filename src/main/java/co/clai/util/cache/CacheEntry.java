package co.clai.util.cache;

public class CacheEntry {

	private final Class<? extends Object> type;
	private final Object data;

	CacheEntry(Class<? extends Object> type, Object data) {
		this.type = type;
		this.data = data;
	}

	public Class<? extends Object> getType() {
		return type;
	}

	public Object getData() {
		return data;
	}
}
