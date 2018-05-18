package co.clai;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.Template;
import co.clai.db.model.User;
import co.clai.module.EditTemplate;
import co.clai.module.FunctionResult;
import junit.framework.TestCase;

public class TemplateTest extends TestCase implements HttpTest {

	public TemplateTest(String name) {
		super(name);
	}

	public void testTemplateFetching() throws Exception {

		JSONObject jData = getRandomDbAndListeningConfig();
		String baseUrl = getIpAndPort(jData);

		MainHttpListener l = new MainHttpListener(jData);
		startHttpListener(l);

		DatabaseConnector.initializeDatabase(l.getDbCon());

		User.addNewLocalUser(l.getDbCon(), "name", "pwd", 1, true);

		// Add new Template
		URIBuilder uB1 = new URIBuilder(
				baseUrl + "/" + EditTemplate.LOCATION + "." + EditTemplate.FUNCTION_NAME_ADD_TEMPLATE);
		uB1.addParameter(Template.DB_TABLE_COLUMN_NAME_NAME, "Test for Serversettings");
		uB1.addParameter(Template.DB_TABLE_COLUMN_NAME_KEY, "prbf2_serversettings_default");
		uB1.addParameter(Template.DB_TABLE_COLUMN_NAME_COMMUNITY_ID, "1");

		assertTrue(httpRequestAsUser(0, "name", "pwd", baseUrl, uB1.toString()).trim()
				.equals(FunctionResult.Status.OK.name()));
		
		// Fetch newest Template
		URIBuilder uB2 = new URIBuilder(
				baseUrl + "/" + EditTemplate.LOCATION + "." + EditTemplate.FUNCTION_NAME_UPDATE_TEMPLATE);
		uB2.addParameter(Template.DB_TABLE_COLUMN_NAME_ID, "1");

		assertTrue(httpRequestAsUser(0, "name", "pwd", baseUrl, uB2.toString()).trim()
				.equals(FunctionResult.Status.OK.name()));

	}

}
