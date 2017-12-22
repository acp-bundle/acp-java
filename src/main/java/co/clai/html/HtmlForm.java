package co.clai.html;

import static co.clai.html.Builder.escapeForHtml;

import java.util.List;
import java.util.Set;

import co.clai.util.ValueValuePair;

public class HtmlForm extends AbstractRenderer {

	public enum Method {
		POST, GET
	}

	public enum InputType {
		TEXT, PASSWORD, HIDDEN, SUBMIT, TEXTAREA, CHECKBOX
	}

	public enum ButtonType {
		PRIMARY, SECONDARY, SUCCESS, INFO, WARNING, DANGER, LINK
	}

	public enum Direction {
		HORIZONTAL, VERTICAL
	}

	public HtmlForm(String actionTarget, Method method) {
		this(actionTarget, method, Direction.HORIZONTAL);
	}

	public HtmlForm(String actionTarget, Method method, Direction direction) {
		super("<form action=\"" + actionTarget + "\" method=" + method.name().toLowerCase()
				+ appendCssIdClassIfNotNull(null, direction == null ? "" : direction.name().toLowerCase()) + ">");
	}

	public void addTextElement(String label, String name, String value) {
		addInputElement(label, name, InputType.TEXT, value, false);
	}

	public void addPasswordElement(String label, String name) {
		addInputElement(label, name, InputType.PASSWORD, null, false);
	}

	public void addHiddenElement(String name, String value) {
		addInputElement(null, name, InputType.HIDDEN, value, false);
	}

	public void addCheckbox(String label, String name, String value) {
		addInputElement(label, name, InputType.CHECKBOX, value, false);
	}

	public void addCheckbox(String label, String name, String value, boolean checked) {
		addInputElement(label, name, InputType.CHECKBOX, value, checked);
	}

	public void addTextArea(String name, String value, int rows, int columns) {
		appendData("<div class='form-group'><div class='col-sm-offset-2 col-sm-10'>");
		appendData("<textarea name='" + name + "' class='form-control form-control-sm' ");
		appendData("rows='" + rows + "' cols='" + columns + "' >");
		appendData((value == null) ? "" : escapeForHtml(value));
		appendData("</textarea>");
		appendData("</div></div>");
	}

	public void addSelectionDropdown(String label, String name, List<ValueValuePair> values) {
		addSelectionDropdown(label, name, values, "-1");
	}

	public void addSelectionDropdown(String label, String name, List<ValueValuePair> values, String selectedValue) {
		appendData("<div class='form-group'>");
		if (label != null) {
			appendData("<label for='" + name + "' class='col-sm-2 control-label'>" + label + ":</label>");
			appendData("<div class='col-sm-10'>");
		} else {
			appendData("<div class='col-sm-offset-2 col-sm-10'>");
		}
		appendData("<select name='" + name + "' class='form-control form-control-sm'>");
		for (ValueValuePair p : values) {
			if (p.getId().equals(selectedValue)) {
				appendData("<option value='" + p.getId() + "' selected='selected' >" + p.getName() + "</option>");
			} else {
				appendData("<option value='" + p.getId() + "'>" + p.getName() + "</option>");
			}
		}
		appendData("</select></div></div>");
	}

	public void addSubmit(String value) {
		addSubmit(value, ButtonType.SECONDARY);
	}

	public void addSubmit(String value, ButtonType type) {
		appendData("<input value='" + value + "' type='submit' class='btn btn-sm btn-" + type.name().toLowerCase()
				+ "' / >");
	}

	public void addCheckboxGroup(String label, String name, List<ValueValuePair> values, Set<String> selectedValues) {
		appendData("<div class='form-group'>");
		appendData("<label for='" + name + "' class='control-label'>" + label + ":</label>");
		appendData("<div class='col-sm-offset-2'>");
		for (ValueValuePair pair : values) {
			final String value = pair.getId();
			addInputElement(pair.getName(), name, InputType.CHECKBOX, value, selectedValues.contains(value));
		}
		appendData("</div></div>");
	}

	private void addInputElement(String label, String name, InputType type, String value, boolean checked) {
		if (type != InputType.HIDDEN) {
			appendData("<div class='form-group'>");
			if (label != null) {
				appendData("<label for='" + name + "' class='col-sm-4 control-label'>" + label + ":</label>");
				appendData("<div class='col-sm-8'>");
			} else {
				appendData("<div class='col-sm-offset-4 col-sm-8'>");
			}
		}
		appendData("<input type='" + type.name().toLowerCase() + "' name='" + name + "' ");
		appendData(
				"value='" + ((value == null) ? "" : escapeForHtml(value)) + "' class='form-control form-control-sm'");
		if (checked) {
			appendData(" checked=\"checked\" ");
		}
		appendData(" />");
		if (type != InputType.HIDDEN) {
			appendData("</div></div>");
		}
	}

	@Override
	public String finish() {
		appendData("</form>");
		return super.finish();
	}

	@Override
	public void write(Builder b) {
		/// TODO: maybe he should (later). No use case yet.
		throw new RuntimeException("Form Renderer should not write a Builder!");
	}

}
