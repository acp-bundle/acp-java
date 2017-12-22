package co.clai.module;

import org.apache.http.client.utils.URIBuilder;

public class FunctionResult {

	private final Status status;
	private final String message;
	private final URIBuilder builder;

	public FunctionResult(Status status, String redirect, String message) {
		this.status = status;
		this.message = message;
		try {
			this.builder = new URIBuilder(redirect);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public FunctionResult(Status status, URIBuilder builder) {
		this.status = status;
		this.message = status.name();
		this.builder = builder;
	}

	public FunctionResult(Status status, String redirect) {
		this.status = status;
		this.message = status.name();
		try {
			this.builder = new URIBuilder(redirect);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Status getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public URIBuilder getBuilder() {
		return builder;
	}

	public enum Status {
		OK, NOT_FOUND, FAILED, NO_ACCESS, INTERNAL_ERROR, MALFORMED_REQUEST, NONE,
	}
}
