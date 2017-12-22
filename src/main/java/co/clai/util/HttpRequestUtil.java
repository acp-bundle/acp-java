package co.clai.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;
import java.util.Map.Entry;

import org.apache.http.client.utils.URIBuilder;

import com.amazonaws.util.IOUtils;

public class HttpRequestUtil {

	public static String httpRequest(String url) {
		return httpRequest(url, null);
	}

	public static String httpRequest(String queryScriptLocation, Map<String, String> parameter) {

		URL testUrl;
		URLConnection testCon = null;
		try {
			URIBuilder builder = new URIBuilder(queryScriptLocation);
			if (parameter != null) {
				for (Entry<String, String> s : parameter.entrySet()) {
					builder.addParameter(s.getKey(), s.getValue());
				}
			}

			testUrl = new URL(builder.toString());
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

	public static String httpPostRequestAsString(String queryScriptLocation, Map<String, String> parameter)
			throws Exception {

		return new String(httpPostRequest(queryScriptLocation, parameter));
	}

	public static byte[] httpPostRequest(String queryScriptLocation, Map<String, String> parameter) throws Exception {
		return httpPostRequest(queryScriptLocation, parameter, null, null);
	}

	public static byte[] httpPostRequest(String queryScriptLocation, Map<String, String> parameter, String username,
			String password) throws Exception {

		StringJoiner sj = new StringJoiner("&");
		for (Map.Entry<String, String> entry : parameter.entrySet()) {
			sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
		}
		byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
		int length = out.length;

		URL url = new URL(queryScriptLocation);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		if (username != null) {
			String userpass = username + ":" + password;
			String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

			conn.setRequestProperty("Authorization", basicAuth);
		}

		conn.setDoOutput(true);
		conn.setInstanceFollowRedirects(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("charset", "utf-8");
		conn.setRequestProperty("Content-Length", Integer.toString(length));
		conn.setUseCaches(false);
		try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
			wr.write(out);
		}

		try (InputStream inStr = conn.getInputStream()) {
			return IOUtils.toByteArray(inStr);
		}
	}

	public static byte[] httpGetRequest(String url, Map<String, String> parameter, String username, String password) {

		try {
			URIBuilder builder = new URIBuilder(url);
			if (parameter != null) {
				for (Entry<String, String> s : parameter.entrySet()) {
					builder.addParameter(s.getKey(), s.getValue());
				}
			}

			HttpURLConnection conn = (HttpURLConnection) new URL(builder.toString()).openConnection();

			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");

			if (username != null) {
				String userpass = username + ":" + password;
				String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

				conn.setRequestProperty("Authorization", basicAuth);
			}

			try (InputStream inStr = conn.getInputStream()) {
				return IOUtils.toByteArray(inStr);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
}
