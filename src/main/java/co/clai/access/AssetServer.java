package co.clai.access;

public class AssetServer extends GeneralAsset {

	private final int accessGameId;

	public AssetServer(int accessAssetId, int accessCommunityId, int accessGameId) {
		super(accessAssetId, accessCommunityId);
		this.accessGameId = accessGameId;
	}

	@Override
	public boolean matchesAsset(AccessFilter f) {
		if ((f.getGameId() == 0) || (f.getGameId() == accessGameId)) {
			if (super.matchesAsset(f)) {
				return true;
			}
		}
		return false;
	}
}
