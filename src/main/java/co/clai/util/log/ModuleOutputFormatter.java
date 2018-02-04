package co.clai.util.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class ModuleOutputFormatter extends Formatter {

	@Override
	public String format(LogRecord rec) {
		StringBuilder sb = new StringBuilder();

		sb.append("[" + rec.getLevel() + " @ ");
		sb.append(calcDate(rec.getMillis()) + "] ");
		sb.append(formatMessage(rec) + "\n");

		return sb.toString();
	}

	private static String calcDate(long millisecs) {
		SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Date resultdate = new Date(millisecs);
		return date_format.format(resultdate);
	}

	@Override
	public String getHead(Handler h) {
		return "Log starting @" + (new Date()) + "\n";
	}

	@Override
	public String getTail(Handler h) {
		return "Log ended @" + (new Date()) + "\n";
	}
}