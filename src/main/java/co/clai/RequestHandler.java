package co.clai;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import co.clai.db.DatabaseConnector;
import co.clai.module.AbstractModule;
import co.clai.module.Index;
import co.clai.module.ModuleUtil;
import co.clai.util.FileUtil;
import co.clai.util.ResourceUtil;
import co.clai.util.log.LoggingUtil;

public class RequestHandler extends AbstractHandler {

	private final Logger logger = LoggingUtil.getDefaultLogger();

	private static final String MIME_TYPE_TEXT_HTML_CHARSET_UTF_8 = "text/html;charset=utf-8";
	private static final String MIME_TYPE_TEXT_CSS = "text/css;charset=utf-8";
	private static final String MIME_TYPE_TEXT_TEXT = "text/text;charset=utf-8";
	private static final String MIME_TYPE_TEXT_JAVASCRIPT = "text/javascript";
	private static final String MIME_TYPE_TEXT_JSON = "application/json;charset=utf-8";
	private static final String MIME_TYPE_IMAGE_X_ICON = "image/x-icon";
	private static final String MIME_TYPE_IMAGE_PNG = "image/png";

	private final DatabaseConnector dbCon;

	private final MainHttpListener listener;

	private final Map<String, AbstractModule> moduleMap = new HashMap<>();

	private class StaticContent {
		public StaticContent(byte[] content, String mimeType) {
			this.content = content;
			this.mimeType = mimeType;
		}

		public byte[] content;
		public String mimeType;
	}

	private final Map<String, StaticContent> staticContent = new HashMap<>();

	public RequestHandler(DatabaseConnector dbCon, MainHttpListener listener) {
		this.dbCon = dbCon;

		this.listener = listener;

		Set<Class<? extends AbstractModule>> allClasses = ModuleUtil.getModuleClasses();

		logger.log(Level.INFO, "total Classes: " + allClasses.size());

		for (Class<? extends AbstractModule> c : allClasses) {
			if (!Modifier.isAbstract(c.getModifiers())) {
				Constructor<? extends AbstractModule> cons;
				try {
					cons = c.getConstructor(DatabaseConnector.class);
					AbstractModule m = cons.newInstance(this.dbCon);
					logger.log(Level.INFO,
							"Adding module \"" + m.getModuleName() + "\" from class \"" + m.toString() + "\"");
					moduleMap.put(m.getModuleName(), m);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		logger.log(Level.INFO, "loading resources:");
		for (String s : ResourceUtil.getResourceInClasspath("static")) {
			String urlOfResource = s.replace("static/", "");
			String mimeType = getMemetype(s);

			logger.log(Level.INFO, "loading resource: " + s + " with meme type " + mimeType);

			staticContent.put(urlOfResource, new StaticContent(ResourceUtil.getResourceAsByteArr("/" + s), mimeType));
		}

		for (String s : ResourceUtil.getResourceInFilepath("static")) {
			String urlOfResource = s.replace("static/", "");
			String mimeType = getMemetype(s);

			logger.log(Level.INFO, "loading resource from file system: " + s + " with meme type " + mimeType);

			staticContent.put(urlOfResource, new StaticContent(FileUtil.getFileAsByteArr(s), mimeType));
			if (urlOfResource.endsWith("index.html")) {
				staticContent.put(urlOfResource.replace("index.html", ""),
						new StaticContent(FileUtil.getFileAsByteArr(s), mimeType));
			}
		}
	}

	private static String getMemetype(String filename) {
		String mimeType = MIME_TYPE_TEXT_TEXT;
		if (filename.endsWith(".ico")) {
			mimeType = MIME_TYPE_IMAGE_X_ICON;
		} else if (filename.endsWith(".css")) {
			mimeType = MIME_TYPE_TEXT_CSS;
		} else if (filename.endsWith(".js")) {
			mimeType = MIME_TYPE_TEXT_JAVASCRIPT;
		} else if (filename.endsWith(".html")) {
			mimeType = MIME_TYPE_TEXT_HTML_CHARSET_UTF_8;
		} else if (filename.endsWith(".json")) {
			mimeType = MIME_TYPE_TEXT_JSON;
		} else if (filename.endsWith(".png")) {
			mimeType = MIME_TYPE_IMAGE_PNG;
		}
		return mimeType;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		logger.log(Level.INFO, "target: " + target);

		String path = null;
		if (target.equals("") || target.equals("/")) {
			path = Index.INDEX_LOCATION;
		} else {
			if (target.startsWith("/")) {
				path = target.substring(1);
			} else {
				path = target;
			}
		}

		String[] targetPath = path.split("\\.");

		if (path.equals(Index.INDEX_LOCATION)) {
			if (!listener.getSiteUrl().contains(request.getServerName())) {
				response.sendRedirect(listener.getSiteUrl());
				return;
			}
		}

		response.setStatus(HttpServletResponse.SC_OK);

		StaticContent tmpConClass = staticContent.get(target.substring(1));
		if (tmpConClass != null) {
			byte[] tmpContent = tmpConClass.content;
			response.setHeader("Cache-Control", "public, max-age=3600");
			response.setContentType(tmpConClass.mimeType);
			response.getOutputStream().write(tmpContent);
			response.getOutputStream().flush();
			baseRequest.setHandled(true);
			return;
		}

		response.setContentType(MIME_TYPE_TEXT_HTML_CHARSET_UTF_8);

		AcpSession s = new AcpSession(dbCon, request, response);

		try (OutputStream outS = response.getOutputStream()) {
			Map<String, String[]> parameterMap = baseRequest.getParameterMap();

			try {
				processRequest(response, targetPath, s, outS, parameterMap);
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				outS.write("Error:<br>".getBytes());
				String message = e.getMessage();
				if (message == null) {
					message = "unknown error";
					e.printStackTrace();
				}
				outS.write(message.getBytes());
				outS.write("<br><a href='/'>return to main page</a>".getBytes());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Fatal Error during error catching: " + e.getMessage());
			e.printStackTrace();
		}

		baseRequest.setHandled(true);
	}

	public void processRequest(HttpServletResponse response, String[] targetPath, AcpSession s, OutputStream outS,
			Map<String, String[]> parameterMap) throws Exception {
		if (moduleMap.containsKey(targetPath[0])) {
			AbstractModule invokedModule = moduleMap.get(targetPath[0]);

			boolean hasAccess = false;

			if (s.getThisUser() != null) {
				hasAccess = s.getThisUser().hasAccess(invokedModule.getAccessibleHelper());
			} else {
				try {
					hasAccess = invokedModule.getAccessibleHelper().hasAccess(null);
				} catch (Exception e) {
					e.getMessage(); // discard
					hasAccess = false;
				}
			}

			if (hasAccess) {

				String function = null;

				if (targetPath.length >= 2) {
					function = targetPath[1];
				}

				byte[] result = invokedModule.invoke(response, s, function, parameterMap);
				if (result == null) {
					result = new byte[0];
				}
				outS.write(result);
				outS.flush();
			} else {
				PrintWriter responseWriter = new PrintWriter(outS);
				responseWriter.println("no access");
				responseWriter.println(StringEscapeUtils.escapeHtml4(targetPath[0]) + "<br>");
				responseWriter.println("<a href='/'>return to main page</a>");
				responseWriter.flush();
			}
		} else {
			PrintWriter responseWriter = new PrintWriter(outS);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			responseWriter.println("Not found:<br>");
			responseWriter.println(targetPath[0] + "<br>");
			responseWriter.println("<a href='/'>return to main page</a>");
			responseWriter.flush();
		}

	}

}
