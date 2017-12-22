package co.clai.util;

import java.io.InputStream;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.google.common.io.ByteStreams;

import co.clai.util.log.LoggingUtil;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

public class ResourceUtil {

	private final static Logger logger = LoggingUtil.getDefaultLogger();

	public static String getResourceAsString(String resourceName) {
		return getResourceAsString((new ResourceUtil()).getClass(), resourceName);
	}

	public static String getResourceAsString(Class<?> c, String resourceName) {

		String text = "";

		try (Scanner scanner = new Scanner(c.getResourceAsStream(resourceName), "UTF-8")) {
			text = scanner.useDelimiter("\\A").next();
		} catch (Exception e) {
			logger.log(Level.INFO, "cannot open resource " + resourceName + ": " + e.getMessage());
		}

		return text;
	}

	public static JSONObject loadJSONResource(String resourceName) {
		final String jsonContent = ResourceUtil.getResourceAsString(resourceName);

		return new JSONObject(jsonContent);
	}

	public static byte[] getResourceAsByteArr(String name) {
		try (InputStream in = (new ResourceUtil()).getClass().getResourceAsStream(name)) {
			return ByteStreams.toByteArray(in);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Set<String> getResourceInClasspath(String classPath) {
		Reflections reflections = new Reflections(classPath, new ResourcesScanner());
		return reflections.getResources(x -> true);
	}
}
