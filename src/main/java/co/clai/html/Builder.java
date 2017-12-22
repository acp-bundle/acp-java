package co.clai.html;

import org.apache.commons.text.StringEscapeUtils;

public interface Builder {

	void writeWithoutEscaping(String data);

	void write(Builder b);

	String finish();

	public static String escapeForHtml(String data) {
		return StringEscapeUtils.escapeHtml4(data);
	}
}
