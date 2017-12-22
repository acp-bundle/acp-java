package co.clai.util.cache;

public interface Cache<T> {

	void put(String key, T Value);

	boolean contains(String key);

	void delete(String key);

	T retrieve(String key);

	default String buildFinalKey(String id, String seperator, String key) {
		return id + seperator + key;
	}
}
