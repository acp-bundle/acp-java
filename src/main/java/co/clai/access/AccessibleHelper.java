package co.clai.access;

public class AccessibleHelper {

	private final boolean access;

	public AccessibleHelper(boolean access) {
		this.access = access;
	}

	public boolean hasAccess(@SuppressWarnings("unused") AccessFilter f) {
		return access;
	}

	@SuppressWarnings("unused")
	public boolean hasAccess(AccessFilter f, CommunityAsset a) {
		return access;
	}
}
