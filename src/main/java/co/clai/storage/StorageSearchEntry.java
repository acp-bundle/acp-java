package co.clai.storage;

public class StorageSearchEntry {

	private final String identifier;
	private final String name;
	private final String data;

	public StorageSearchEntry(String identifier, String name, String data) {
		this.identifier = identifier;
		this.name = name;
		this.data = data;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getName() {
		return name;
	}

	public String getData() {
		return data;
	}

}
