package co.clai.html;

public class HtmlResponsiveColumns extends AbstractRenderer {

	public HtmlResponsiveColumns() {
		super("<div class='row'>");
	}

	@Override
	public void write(Builder b) {
		appendData(b.finish());
	}

	private boolean isInColumn = false;

	public void startColumn(int width) {
		if (isInColumn) {
			appendData("</div>");
		}
		appendData("<div class='col-sm-" + width + "'>");
		isInColumn = true;
	}

	@Override
	public String finish() {
		if (isInColumn) {
			appendData("</div>");
		}
		return super.finish() + "</div>";
	}
}
