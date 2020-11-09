package at.sfischer.jacocoReader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoSession.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Represents a session inside the JaCoCo coverage report
 */
public class JacocoSession {

	/**
	 * Session ID
	 */
	private final String id;

	/**
	 * Coverage data of this session, per method
	 */
	private final Map<String, JacocoMethodCoverage> coverage;
	
	public JacocoSession(String id) {
		super();
		this.id = id;
		this.coverage = new HashMap<>();
	}

	public String getId() {
		return id;
	}

	public Map<String, JacocoMethodCoverage> getCoverage() {
		return coverage;
	}

	public void addCoverage(JacocoMethodCoverage methodCoverage) {
		this.coverage.put(methodCoverage.getMethod().getFullName(), methodCoverage); 
	}

	public JacocoMethodCoverage getCoverage(String fullName) {
		return this.coverage.get(fullName);
	}

	public int getNumberOfCoveredMethods() {
		return this.coverage.size();
	}

	/**
	 * Add the coverage data of session to this.
	 * Union operation.
	 * @param session
	 */
	public void add(JacocoSession session) {
		for(Map.Entry<String, JacocoMethodCoverage> entry : session.coverage.entrySet()) {
			JacocoMethodCoverage jMethodCoverage = this.coverage.get(entry.getKey());
			if(jMethodCoverage == null) {
				Set<Integer> linesCovered = new HashSet<>();
				linesCovered.addAll(entry.getValue().getLinesCovered());
				jMethodCoverage = new JacocoMethodCoverage(entry.getValue().getMethod(), linesCovered);
				this.coverage.put(entry.getKey(), jMethodCoverage);
			} else {
				jMethodCoverage.addLinesCovered(entry.getValue().getLinesCovered());
			}
		}
	}

	/**
	 * Remove the coverage data of session from this
	 * @param session
	 */
	public void remove(JacocoSession session) {
		for(Map.Entry<String, JacocoMethodCoverage> entry : session.coverage.entrySet()) {
			JacocoMethodCoverage jMethodCoverage = this.coverage.get(entry.getKey());
			if(jMethodCoverage != null) {
				jMethodCoverage.removeLinesCovered(entry.getValue().getLinesCovered());
				if(jMethodCoverage.getNumberOfLinesCovered() == 0){
					this.coverage.remove(entry.getKey());
				}
			}
		}
	}

	/**
	 * Retail the coverage data of session in this.
	 * Intersection operation.
	 * @param session
	 */
	public void retain(JacocoSession session) {
		Set<String> toRemove = new HashSet<String>();
		for(Map.Entry<String, JacocoMethodCoverage> entry : this.coverage.entrySet()) {
			JacocoMethodCoverage jMethodCoverage = session.coverage.get(entry.getKey());
			if(jMethodCoverage != null) {
				entry.getValue().retainLinesCovered(jMethodCoverage.getLinesCovered());
				if(entry.getValue().getNumberOfLinesCovered() == 0){
					toRemove.add(entry.getKey());
				}
			} else {
				toRemove.add(entry.getKey());
			}
		}
		for(String key : toRemove) {
			this.coverage.remove(key);
		}
	}
}
