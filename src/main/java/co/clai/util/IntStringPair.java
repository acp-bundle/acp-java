package co.clai.util;

public class IntStringPair extends ValueValuePair {

	private final int id;
	private final String name;

	public IntStringPair(int id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String getId() {
		return id + "";
	}

	@Override
	public String getName() {
		return name;
	}

}
