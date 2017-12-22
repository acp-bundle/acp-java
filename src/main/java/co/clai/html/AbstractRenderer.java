package co.clai.html;

import static co.clai.html.Builder.escapeForHtml;

public abstract class AbstractRenderer implements Builder {

	private StringBuilder buffer = new StringBuilder();

	public AbstractRenderer(String begin) {
		buffer.append(begin);
	}

	protected final static String appendCssIdClassIfNotNull(String cssId, String cssClass) {
		return ((cssId == null) ? " " : (" id='" + cssId + "' "))
				+ ((cssClass == null) ? " " : (" class='" + cssClass + "' "));
	}

	protected final void appendData(String data) {
		buffer.append(data + "\n");
	}

	@Override
	public final void writeWithoutEscaping(String data) {
		appendData(data);
	}

	@Override
	public String finish() {
		return buffer.toString();
	}

	public void writeH1(String string) {
		appendData("<h1>" + escapeForHtml(string) + "</h1>");
	}

	public void writeH2(String string) {
		appendData("<h2>" + escapeForHtml(string) + "</h2>");
	}

	public void writeH3(String string) {
		appendData("<h3>" + escapeForHtml(string) + "</h3>");
	}

	public void writeHline() {
		appendData("<hr>");
	}

	public void writeText(String string) {
		appendData(escapeForHtml(string));
	}

	public void writeLink(String location, String text) {
		appendData("<a href='" + location + "'>" + escapeForHtml(text) + "</a>");
	}

}
