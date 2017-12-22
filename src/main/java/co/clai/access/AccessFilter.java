package co.clai.access;

/**
 * The access filter as it is in the database
 * 
 * @author simon
 *
 */
public class AccessFilter {

	/**
	 * The module
	 */
	private final String module;
	private final String function;

	/**
	 * The assetId and communityId. Will be used by AccessibleAsset
	 */
	private final int communityId;
	private final int assetId;

	/**
	 * Game id. Will be used by AccessibleServer
	 */
	private final int gameId;

	/**
	 * For modules
	 * 
	 * @param path
	 *            the module (path)
	 */
	public AccessFilter(String path) {
		this(path, -1, -1, -1);
	}

	/**
	 * complete constructor
	 * 
	 * @param level
	 *            the access level
	 * @param path
	 *            the module (path)
	 * @param assetId
	 *            the gameserver id
	 * @param communityId
	 *            the community id
	 * @param gameId
	 *            the game id
	 */
	public AccessFilter(String path, int assetId, int communityId, int gameId) {

		String[] p = path.split("\\.");

		if (p.length < 1) {
			throw new RuntimeException("Path is empty in AccessFilter()");
		}

		this.module = p[0];

		if (p.length == 1) {
			this.function = null;
		} else {
			this.function = p[1];
		}

		this.assetId = assetId;
		this.communityId = communityId;
		this.gameId = gameId;
	}

	/**
	 * returns the assetId, can be 0
	 * 
	 * @return
	 */
	public int getAssetId() {
		return assetId;
	}

	/**
	 * returns the community, can be 0
	 * 
	 * @return
	 */
	public int getCommunityId() {
		return communityId;
	}

	/**
	 * returns the game, can be 0
	 * 
	 * @return
	 */
	public int getGameId() {
		return gameId;
	}

	/**
	 * returns the module
	 * 
	 * @return
	 */
	public String getModule() {
		return module;
	}

	/**
	 * returns the function, can be null
	 * 
	 * @return
	 */
	public String getFunction() {
		return function;
	}
}
