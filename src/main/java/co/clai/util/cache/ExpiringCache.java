package co.clai.util.cache;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import co.clai.util.log.LoggingUtil;

public class ExpiringCache<T> implements Cache<T> {

	private static Map<String, ExpiringCacheEntry> data = new HashMap<>();

	public static final String SEPERATOR = ":@:";

	private final String id;

	public ExpiringCache(String id) {
		this.id = id;
	}

	@Override
	public void put(String key, T Value) {
		if (cacheRemover == null) {
			cacheRemover = startNewCacheRemover();
		}

		final String thisKey = getFinalKey(key);

		final ExpiringCacheEntry entry = new ExpiringCacheEntry(Value.getClass(), Value, cacheBuffer.size(), thisKey);
		data.put(thisKey, entry);
		cacheBuffer.add(entry);
	}

	@Override
	public boolean contains(String key) {
		return data.containsKey(getFinalKey(key));
	}

	@Override
	public void delete(String key) {
		cacheBuffer.remove(data.remove(getFinalKey(key)).getPosition());
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

	private static final List<ExpiringCacheEntry> cacheBuffer = new LinkedList<>();

	private static Thread cacheRemover = null;

	private static final Thread startNewCacheRemover() {
		Thread retThread = new Thread(ExpiringCache::cacheRemoverFunction);

		retThread.start();

		return retThread;
	}

	public static long cacheTime = 5 * 60 * 1000; // 5 Minutes in Milliseconds

	private static void cacheRemoverFunction() {
		Logger logger = LoggingUtil.getDefaultLogger();

		logger.log(Level.INFO, "[Expiring Cache] starting cache.");

		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			logger.log(Level.INFO, "[Expiring Cache] starting sleep interrupted: " + e.getMessage());
		}

		while (!cacheBuffer.isEmpty()) {
			long timeToSleep = (cacheBuffer.get(0).getCreatedTimeStamp() + cacheTime) - System.currentTimeMillis();
			if (timeToSleep > 0) {
				try {
					Thread.sleep(timeToSleep);
				} catch (Exception e) {
					logger.log(Level.INFO, "[Expiring Cache] Sleep interrupted: " + e.getMessage());
				}
			}

			try {
				ExpiringCacheEntry tmpE = cacheBuffer.remove(0);
				if (data.remove(tmpE.getKey()) == null) {
					logger.log(Level.WARNING, "key " + tmpE.getKey() + " was not found!");
				}
			} catch (Exception e) {
				logger.log(Level.INFO, "[Expiring Cache] Error while removing key: " + e.getMessage());
			}
		}

		logger.log(Level.INFO, "[Expiring Cache] stopping cache.");

		cacheRemover = null;
	}

	public static void stop() {

		if (cacheRemover == null) {
			return;
		}

		cacheBuffer.clear();

		cacheRemover.interrupt();
	}

}
