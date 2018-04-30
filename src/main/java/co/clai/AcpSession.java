package co.clai;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import co.clai.db.DatabaseConnector;
import co.clai.db.model.User;

public class AcpSession {

	private final HttpSession session;
	private final User thisUser;
	private final DatabaseConnector dbCon;
	private final String clientIp;

	private final String hostname;
	private final HttpServletResponse response;

	AcpSession(DatabaseConnector dbCon, HttpServletRequest request, HttpServletResponse response) {
		session = request.getSession();
		hostname = request.getServerName();

		this.response = response;
		this.dbCon = dbCon;

		String tmpClientIp = request.getRemoteAddr();
		if (tmpClientIp.equals("127.0.0.1")) {
			if (request.getHeader("X-Real-IP") != null) {
				tmpClientIp = request.getHeader("X-Real-IP");
			} else if (request.getHeader("X-Forwarded-For") != null) {
				tmpClientIp = request.getHeader("X-Forwarded-For");
			}
		}

		this.clientIp = tmpClientIp;

		if ((session.getAttribute("userLocation") == null) || (session.getAttribute("userId") == null)) {
			thisUser = null;
		} else {
			int location = ((Integer) session.getAttribute("userLocation")).intValue();
			int id = ((Integer) session.getAttribute("userId")).intValue();
			thisUser = User.getUserByLocationId(dbCon, location, id);
		}
	}

	public User getThisUser() {
		return thisUser;
	}

	public void setUser(User u) {
		if (u == null) {
			session.removeAttribute("userLocation");
			session.removeAttribute("userId");
			return;
		}
		session.setAttribute("userLocation", new Integer(u.getLocationId()));
		session.setAttribute("userId", new Integer(u.getId()));
	}

	public HttpSession getSession() {
		return session;
	}

	public void clear() {
		session.invalidate();
	}

	public DatabaseConnector getDbCon() {
		return dbCon;
	}

	public String getClientIp() {
		return clientIp;
	}

	public String getHostname() {
		return hostname;
	}

	public HttpServletResponse getResponse() {
		return response;
	}
}
