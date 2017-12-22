package co.clai.html;

import java.util.List;

import static co.clai.html.Builder.escapeForHtml;

public class HtmlTable extends AbstractRenderer {

	public class HtmlTableRow extends AbstractRenderer {

		public HtmlTableRow() {
			super("<tr>");
		}

		@Override
		public void write(Builder b) {
			appendData("<td>" + b.finish() + "</td>");
		}

		@Override
		public void writeText(String text) {
			appendData("<td>" + escapeForHtml(text) + "</td>");
		}

		@Override
		public void writeLink(String location, String text) {
			writeLink(location, text, false);
		}

		public void writeLink(String location, String text, boolean newTab) {
			if (newTab) {
				appendData("<td><a target='_blank' href='" + location + "'>" + escapeForHtml(text) + "</a></td>");
			} else {
				appendData("<td><a href='" + location + "'>" + escapeForHtml(text) + "</a></td>");
			}
		}

		@Override
		public String finish() {
			appendData("</tr>");
			return super.finish();
		}

	}

	public HtmlTable() {
		super("<table class='table table-responsive table-striped table-hover table-sm'>");
	}

	public void addHeader(List<String> content) {
		appendData("<thead>");
		addRow(content);
		appendData("</thead>");
	}

	public void addRow(List<String> content) {
		appendData("<tr>");
		for (String s : content) {
			appendData("<td>" + escapeForHtml(s) + "</td>");
		}
		appendData("</tr>");
	}

	private boolean hasBody = false;

	public void startBody() {
		hasBody = true;
		appendData("<tbody>");
	}

	@Override
	public String finish() {
		if (hasBody) {
			appendData("</tbody>");
		}
		appendData("</table>");
		return super.finish();
	}

	@Override
	public void write(Builder b) {
		/// TODO: maybe Table should (later). No use case yet.
		throw new RuntimeException("Table should not write a Builder!");
	}

	public void write(HtmlTableRow r) {
		appendData(r.finish());
	}
}
