package co.clai.access;

/**
 * Accessible for a function. Not tied to any asset or community.
 */
public class AccessibleFunctionHelper extends AccessibleModuleHelper {

	private final String functionName;

	public AccessibleFunctionHelper(String moduleName, String functionName) {
		super(moduleName);
		this.functionName = functionName;
	}

	@Override
	public boolean hasAccess(AccessFilter f) {
		String filterFunction = f.getFunction();
		if (filterFunction == null) {
			return false;
		}
		if (filterFunction.equals("*") || filterFunction.equals(functionName)) {
			if (super.hasAccess(f)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasAccess(AccessFilter f, CommunityAsset a) {
		String filterFunction = f.getFunction();
		if (filterFunction == null) {
			return false;
		}
		if (filterFunction.equals("*") || filterFunction.equals(functionName)) {
			return super.hasAccess(f, a);
		}
		return false;
	}
}
