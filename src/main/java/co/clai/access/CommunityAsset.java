package co.clai.access;

public class CommunityAsset {

	private final int accessCommunityId;

	public CommunityAsset(int accessCommunityId) {
		this.accessCommunityId = accessCommunityId;
	}

	public boolean matchesAsset(AccessFilter f) {
		if ((f.getCommunityId() == 0) || (f.getCommunityId() == accessCommunityId)) {
			return true;
		}
		return false;
	}

}
