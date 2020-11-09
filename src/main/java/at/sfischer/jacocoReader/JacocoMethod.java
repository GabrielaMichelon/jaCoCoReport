package at.sfischer.jacocoReader;

import java.util.Set;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoMethod.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Representation of a Method in the source code structure of the system under test.
 */
public class JacocoMethod {

	/**
	 * Signature of the method.
	 */
	private final String signature;

	/**
	 * Class this method is contained in.
	 */
	private final JacocoClass clazz;

	/**
	 * Line numbers inside the method.
	 */
	private final Set<Integer> lines;
	
	public JacocoMethod(JacocoClass clazz, String signature, Set<Integer> lines) {
		super();
		this.clazz = clazz;
		this.clazz.addMethod(this);
		this.signature = signature;
		this.lines = lines;
	}

	public JacocoClass getClazz() {
		return clazz;
	}

	public String getSignature() {
		return signature;
	}

	public Set<Integer> getLines() {
		return lines;
	}

	/**
	 * @return - number of lines contained in this method
	 */
	public int getNumberOfLines() {
		return this.lines.size();
	}

	/**
	 * Get the full name of this method inside its class and package.
	 * @return package name and class name and method signature
	 */
	public String getFullName() {
		return clazz.getFullName() + "." + this.signature;
	}
}
