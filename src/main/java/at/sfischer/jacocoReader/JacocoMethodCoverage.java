package at.sfischer.jacocoReader;

import java.util.Set;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoMethodCoverage.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Coverage data for a method
 */
public class JacocoMethodCoverage {

	/**
	 * Method the coverage data is for
	 */
	private final JacocoMethod method;

	/**
	 * Line numbers inside the method the were covered.
	 */
	private final Set<Integer> linesCovered;
	
	public JacocoMethodCoverage(JacocoMethod method, Set<Integer> linesCovered) {
		super();
		this.method = method;
		this.linesCovered = linesCovered;
	}

	public JacocoMethod getMethod() {
		return method;
	}

	public Set<Integer> getLinesCovered() {
		return linesCovered;
	}

	public int getNumberOfLinesCovered() {
		return this.linesCovered.size();
	}

	/**
	 * Add line numbers to the coverage data. (Union)
	 * @param linesCovered - line numbers to add
	 */
	public void addLinesCovered(Set<Integer> linesCovered) {
		this.linesCovered.addAll(linesCovered);
	}

	/**
	 * Remove line numbers from the coverage data.
	 * @param linesCovered - line numbers to remove
	 */
	public void removeLinesCovered(Set<Integer> linesCovered) {
		this.linesCovered.removeAll(linesCovered);
	}

	/**
	 * Retain only line numbers in the coverage data from linesCovered. (Intersection)
	 * @param linesCovered - line numbers to retain
	 */
	public void retainLinesCovered(Set<Integer> linesCovered) {
		this.linesCovered.retainAll(linesCovered);
	}
	
}
