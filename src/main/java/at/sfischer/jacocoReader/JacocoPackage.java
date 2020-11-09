package at.sfischer.jacocoReader;

import java.util.HashSet;
import java.util.Set;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoPackage.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

/**
 * Representation of a Package in the source code structure of the system under test.
 */
public class JacocoPackage {

	/**
	 * Name of the package.
	 */
	private final String name;

	/**
	 * Classes inside the package.
	 */
	private final Set<JacocoClass> classes;

	public JacocoPackage(String name) {
		super();
		this.name = name;
		this.classes = new HashSet<JacocoClass>();
	}
	
	public String getName() {
		return name;
	}

	public Set<JacocoClass> getClasses() {
		return classes;
	}

	/**
	 * Adds a class to this package
	 * @param jacocoClass - class to be added
	 */
	protected void addClass(JacocoClass jacocoClass) {
		this.classes.add(jacocoClass);
	}
	
}
