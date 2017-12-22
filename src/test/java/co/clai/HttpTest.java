package co.clai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import co.clai.MainHttpListener;
import co.clai.module.FunctionResult;
import co.clai.util.CookieManager;
import co.clai.util.ResourceUtil;
import co.clai.util.log.LoggingUtil;

public interface HttpTest {

	default JSONObject getRandomDbAndListeningConfig() {
		JSONObject jData = ResourceUtil.loadJSONResource("/apptest.json");

		jData.remove("listen");
		jData.put("listen", new JSONArray(
				"[{\"ip\": \"127.0.0.1\",\"ports\":[\"" + (int) (8000 + (Math.random() * 1000.f)) + "\"]}]"));

		jData.remove("db");
		jData.put("db", new JSONObject("{\"path\":\"jdbc:h2:mem:test" + (int) (Math.random() * 1000.f)
				+ ";DB_CLOSE_DELAY=-1;database_to_upper=false\",\"username\":\"\",\"password\":\"\"}"));
		return jData;
	}

	default String getIpAndPort(JSONObject jData) {

		JSONObject jO = jData.getJSONArray("listen").getJSONObject(0);

		String url = "http://" + jO.getString("ip") + ":" + jO.getJSONArray("ports").getString(0);

		return url;
	}

	default void startHttpListener(MainHttpListener l) {

		l.run();

		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	default String httpRequest(String url) {

		HttpURLConnection.setFollowRedirects(false);

		URL testUrl;
		URLConnection testCon = null;
		try {
			testUrl = new URL(url);
			testCon = testUrl.openConnection();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		StringBuilder sb = new StringBuilder();

		try (BufferedReader in = new BufferedReader(new InputStreamReader(testCon.getInputStream()));) {

			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine + "\n");
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return sb.toString();
	}

	default String httpRequestAsUser(int location, String username, String password, String host, String url) {

		Logger logger = LoggingUtil.getDefaultLogger();

		CookieManager cm = new CookieManager();

		HttpURLConnection.setFollowRedirects(false);

		URL loginUrl;
		URLConnection loginCon = null;
		try {
			loginUrl = new URL(host + "/index.login?location=" + location + "&username=" + username + "&password="
					+ password + "");
			loginCon = loginUrl.openConnection();
			cm.storeCookies(loginCon);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try (BufferedReader in = new BufferedReader(new InputStreamReader(loginCon.getInputStream()));) {
			String returnValue;
			returnValue = in.readLine();
			if (!returnValue.equals(FunctionResult.Status.OK.name())) {
				logger.log(Level.WARNING, "Failed logging in: " + returnValue);
				throw new RuntimeException("Failed Log in");
			}
			while (in.readLine() != null) {
				// just logging in
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		URL testUrl;
		URLConnection testCon = null;
		try {
			testUrl = new URL(url);
			testCon = testUrl.openConnection();
			cm.setCookies(testCon);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		StringBuilder sb = new StringBuilder();

		try (BufferedReader in = new BufferedReader(new InputStreamReader(testCon.getInputStream()));) {

			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine + "\n");
				// logger.log(Level.INFO, "Result: " + inputLine);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return sb.toString();
	}

}
