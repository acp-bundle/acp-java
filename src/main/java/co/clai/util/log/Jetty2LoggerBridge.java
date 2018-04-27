package co.clai.util.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;

import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.Logger;

/**
 * User: Robert Franz Date: 2015-08-24 Time: 20:35
 */
public class Jetty2LoggerBridge extends AbstractLogger {

	private static final String LOGGER_NAME_JETTY_LOG = "jetty-log";

	private final java.util.logging.Logger logger;
	private final String name;

	private boolean debugEnabled = true;

	public Jetty2LoggerBridge(@SuppressWarnings("unused") String nameToBeIgnored) {
		this.name = LOGGER_NAME_JETTY_LOG;
		logger = java.util.logging.Logger.getLogger(name);
		logger.setLevel(Level.WARNING);
		logger.setUseParentHandlers(false);

		ConsoleHandler hCon = new ConsoleHandler();
		hCon.setFormatter(new GeneralOutputFormatter());
		logger.addHandler(hCon);

		try {
			FileHandler fileHandler = new FileHandler(LoggingUtil.LOGGING_DESTINATION_DIR + "jetty.log", true);
			fileHandler.setFormatter(new ModuleOutputFormatter());
			logger.addHandler(fileHandler);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	protected Logger newLogger(String fullname) {
		return new Jetty2LoggerBridge(fullname);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void warn(String msg, Object... args) {
		logger.log(Level.WARNING, msg, args);
	}

	@Override
	public void warn(Throwable thrown) {
		warn(thrown.getMessage(), thrown);
	}

	@Override
	public void warn(String msg, Throwable thrown) {
		logger.log(Level.WARNING, msg + " " + thrown.getMessage());
	}

	@Override
	public void info(String msg, Object... args) {
		logger.log(Level.INFO, msg, args);
	}

	@Override
	public void info(Throwable thrown) {
		info(thrown.getMessage(), thrown);
	}

	@Override
	public void info(String msg, Throwable thrown) {
		logger.log(Level.INFO, msg + " " + thrown.getMessage());
	}

	@Override
	public boolean isDebugEnabled() {
		return (debugEnabled && logger.isLoggable(Level.FINE));
	}

	@Override
	public void setDebugEnabled(boolean enabled) {
		debugEnabled = enabled;
	}

	@Override
	public void debug(String msg, Object... args) {
		if (debugEnabled) {
			logger.log(Level.FINE, msg, args);
		}
	}

	@Override
	public void debug(Throwable thrown) {
		debug(thrown.getMessage(), thrown.getMessage());
	}

	@Override
	public void debug(String msg, Throwable thrown) {
		if (debugEnabled) {
			logger.log(Level.FINE, msg + " " + thrown.getMessage());
		}
	}

	@Override
	public void ignore(Throwable ignored) {
		// ignoring this call
	}
}