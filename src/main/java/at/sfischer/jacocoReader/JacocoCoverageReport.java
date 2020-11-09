package at.sfischer.jacocoReader;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoCoverageReport.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Representation for the entire Coverage report from JaCoCo.
 * Including source code structure and coverage data.
 */
public class JacocoCoverageReport {

	/**
	 * Packages, as reference to the source code structure.
	 */
	private final Collection<JacocoPackage> packages;

	/**
	 * Sessions of the at.sfischer.jacocoReader.JacocoCoverageReport, by their ID.
	 */
	private final Map<String, JacocoSession> sessions;
	
	public JacocoCoverageReport(Collection<JacocoPackage> packages) {
		super();
		this.packages = packages;
		this.sessions = new HashMap<String, JacocoSession>();
	}

	public Collection<JacocoPackage> getPackages() {
		return packages;
	}

	public void addSession(JacocoSession session) {
		this.sessions.put(session.getId(), session);
	}
	
	public JacocoSession getSession(String id) {
		return this.sessions.get(id);
	}

	public Collection<JacocoSession> getSessions() {
		return this.sessions.values();
	}
	
	public int numberOfSessions() {
		return this.sessions.size();
	}

	/**
	 * @param sessionIDs - IDs for the sessions we should return
	 * @return - sessions in the report with the IDs in sessionIDs
	 */
	public Set<JacocoSession> getSessions(Collection<String> sessionIDs) {
		Set<JacocoSession> sessions = new HashSet<JacocoSession>();
		if(sessionIDs != null) {
			for(String sessionID : sessionIDs) {
				JacocoSession session = getSession(sessionID);
				if(session != null) {
					sessions.add(session);
				}
			}
		}
		return sessions;
	}

	/**
	 * @return Union of all sessions in the report.
	 */
	public JacocoSession union() {
		return JacocoCoverageReport.union(getSessions());
	}

	/**
	 * Intersection of all sessions in the report.
	 * @return
	 */
	public JacocoSession intersection() {
		return JacocoCoverageReport.intersection(getSessions());
	}

	/**
	 * @param sessions - sessions to be in the union.
	 * @return - Union of all sessions in sessions.
	 */
	public static JacocoSession union(Collection<JacocoSession> sessions) {
		JacocoSession union = new JacocoSession("union " + System.currentTimeMillis());
		for(JacocoSession session : sessions) {
			union.add(session);
		}
		return union;
	}

	/**
	 * @param sessions - sessions to be in the intersection.
	 * @return - Intersection of all sessions in sessions.
	 */
	public static JacocoSession intersection(Collection<JacocoSession> sessions) {
		JacocoSession intersection = new JacocoSession("intersection " + System.currentTimeMillis());
		boolean first = true;
		for(JacocoSession session : sessions) {
			if(first) {
				intersection.add(session);
			} else {
				intersection.retain(session);
				if(intersection.getNumberOfCoveredMethods() == 0) {
					break;
				}
			}
			first = false;
		}
		return intersection;
	}

	/**
	 * @param session - session we want the unique coverage contribution for.
	 * @return - coverage data that only session contains and no other session in the report has.
	 */
	public JacocoSession getUniqeContribution(JacocoSession session) {
		JacocoSession unique = new JacocoSession("unique " + session.getId());
		unique.add(session);
		for(JacocoSession jSession : getSessions()) {
			if(jSession != session) {
				unique.remove(jSession);
				if(unique.getNumberOfCoveredMethods() == 0) {
					break;
				}
			}
		}
		return unique;
	}

	/**
	 * @param sessions - sessions we want the unique coverage contribution for.
	 * @return - coverage data that only the union of sessions contains and no other session in the report has.
	 */
	public JacocoSession getUniqeContribution(Collection<JacocoSession> sessions) {
		JacocoSession unique = JacocoCoverageReport.union(sessions);
		for(JacocoSession jSession : getSessions()) {
			if(!sessions.contains(jSession)) {
				unique.remove(jSession);
				if(unique.getNumberOfCoveredMethods() == 0) {
					break;
				}
			}
		}
		return unique;
	}
}
