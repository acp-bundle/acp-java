package co.clai.remote;

import java.util.List;

public class RemoteUserData {

	private final int id;
	private final String username;
	private final List<Integer> userGroups;
	private final String email;

	private final String passwordHash;
	private final String passwordSalt;

	RemoteUserData(int id, String username, List<Integer> userGroups, String email) {
		this(id, username, userGroups, email, null, null);
	}

	RemoteUserData(int id, String username, List<Integer> userGroups, String email, String passwordHash,
			String passwordSalt) {
		this.id = id;
		this.username = username;
		this.userGroups = userGroups;
		this.email = email;
		this.passwordHash = passwordHash;
		this.passwordSalt = passwordSalt;
	}

	public int getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public List<Integer> getUserGroups() {
		return userGroups;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getPasswordSalt() {
		return passwordSalt;
	}

}
