package co.clai.access;

public class GeneralAsset extends CommunityAsset {

	private final int accessAssetId;

	public GeneralAsset(int accessAssetId, int accessCommunityId) {
		super(accessCommunityId);
		this.accessAssetId = accessAssetId;
	}

	@Override
	public boolean matchesAsset(AccessFilter f) {
		if ((f.getAssetId() == 0) || (f.getAssetId() == accessAssetId)) {
			return super.matchesAsset(f);
		}
		return false;
	}
}
