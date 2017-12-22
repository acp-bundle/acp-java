package co.clai.util.cache;

import java.util.HashMap;
import java.util.Map;

public class PermanentCache<T> implements Cache<T> {

	private static Map<String, CacheEntry> data = new HashMap<>();

	public static final String SEPERATOR = ":@:";

	private final String id;

	public PermanentCache(String id) {
		this.id = id;
	}

	@Override
	public void put(String key, T Value) {
		data.put(getFinalKey(key), new CacheEntry(Value.getClass(), Value));
	}

	@Override
	public boolean contains(String key) {
		return data.containsKey(getFinalKey(key));
	}

	@Override
	public void delete(String key) {
		data.remove(getFinalKey(key));
	}

	@SuppressWarnings("unchecked")
	@Override
	public T retrieve(String key) {
		CacheEntry cacheEntry = data.get(getFinalKey(key));

		if (cacheEntry == null) {
			return null;
		}

		return (T) cacheEntry.getData();
	}

	private String getFinalKey(String key) {
		return buildFinalKey(id, SEPERATOR, key);
	}

}
