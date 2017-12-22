package co.clai.access;

public class AccessibleModuleHelper extends AccessibleHelper {

	private final String moduleName;

	public AccessibleModuleHelper(String moduleName) {
		super(false);
		this.moduleName = moduleName;
	}

	@Override
	public boolean hasAccess(AccessFilter f) {
		return (f.getModule().equals(moduleName));
	}

	@Override
	public boolean hasAccess(AccessFilter f, CommunityAsset a) {
		if (f.getModule().equals(moduleName)) {
			if (a.matchesAsset(f)) {
				return true;
			}
		}
		return false;
	}
}
