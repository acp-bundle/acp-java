package co.clai.util.log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import co.clai.module.AbstractModule;

public class LoggingUtil {

	public final static String LOGGING_DESTINATION_DIR = "log/";
	public final static String USER_LOG_DESTINATION_DIR = "user_log/";

	private static final Map<String, Logger> moduleLoggerList = new HashMap<>();

	public static Logger getDefaultLogger() {
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

	private static boolean isSetup = false;

	public static void setup() {

		if (isSetup) {
			return;
		}

		isSetup = true;

		createLogDir();

		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		logger.setLevel(Level.FINE);
		logger.setUseParentHandlers(false);

		FileHandler fileTxt;
		try {
			fileTxt = new FileHandler(LOGGING_DESTINATION_DIR + "general.log", true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// create a TXT formatter
		Formatter formatterTxt = new GeneralOutputFormatter();
		fileTxt.setFormatter(formatterTxt);
		logger.addHandler(fileTxt);

		ConsoleHandler hCon = new ConsoleHandler();
		hCon.setFormatter(new GeneralOutputFormatter());
		logger.addHandler(hCon);
	}

	public static void createLoggerForModule(Class<? extends AbstractModule> c) {

		createLogDir();

		try {

			Logger l = Logger.getLogger("module-" + c.getName(), null);
			l.setLevel(Level.FINE);
			l.setUseParentHandlers(false);

			moduleLoggerList.put(c.getName(), l);

			l.setLevel(Level.ALL);
			FileHandler fileHandler = new FileHandler(LOGGING_DESTINATION_DIR + c.getSimpleName() + ".log", true);

			// create a TXT formatter
			ModuleOutputFormatter formatter = new ModuleOutputFormatter();
			fileHandler.setFormatter(formatter);
			l.addHandler(fileHandler);

			ConsoleHandler hCon = new ConsoleHandler();
			hCon.setFormatter(new GeneralOutputFormatter());
			l.addHandler(hCon);

			Logger l1 = Logger.getLogger("user-log-module-" + c.getName(), null);
			l1.setLevel(Level.FINE);
			l1.setUseParentHandlers(false);

			moduleLoggerList.put(c.getName(), l1);

			l1.setLevel(Level.ALL);
			FileHandler fileHandler2 = new FileHandler(USER_LOG_DESTINATION_DIR + c.getSimpleName() + ".log", true);

			// create a TXT formatter
			UserLogOutputFormatter formatter2 = new UserLogOutputFormatter();
			fileHandler2.setFormatter(formatter2);
			l1.addHandler(fileHandler2);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private static void createLogDir() {
		File f = new File(LOGGING_DESTINATION_DIR);

		if (f.isFile()) {
			throw new RuntimeException(LOGGING_DESTINATION_DIR + " should be a directory and not a file!");
		}

		if (!f.isDirectory()) {
			f.mkdir();
		}

		File f2 = new File(USER_LOG_DESTINATION_DIR);

		if (f2.isFile()) {
			throw new RuntimeException(USER_LOG_DESTINATION_DIR + " should be a directory and not a file!");
		}

		if (!f2.isDirectory()) {
			f2.mkdir();
		}
	}

	public static Logger getLoggerFromModule(Class<? extends AbstractModule> c) {
		return Logger.getLogger("module-" + c.getName(), null);
	}

	public static Logger getUserLogFromModule(Class<? extends AbstractModule> c) {
		return Logger.getLogger("user-log-module-" + c.getName(), null);
	}
}
