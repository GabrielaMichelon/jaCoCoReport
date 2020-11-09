package at.sfischer.jacocoReader;

import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.report.JavaNames;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/*
 * TestCovAnalyzer
 * at.sfischer.jacocoReader.JacocoReportGenerator.java
 * -----------------------------------------------------------------------------
 * Created by Software Competence Center Hagenberg GmbH (SCCH)
 * on [24.07.2019] by Stefan Fischer
 */

public class JacocoReportGenerator {

	private JacocoReportGenerator() {
		super();
	}

	public static JacocoCoverageReport parseExecFile(final File execFile, final File binDir, final File... jars) throws IOException {
		Set<File> jarsSet = new HashSet<>(Arrays.asList(jars));
		return parseExecFile(execFile, binDir, jarsSet);
	}

	/**
	 * Parse the exec file generate by JaCoCo
	 * @param execFile - exec file
	 * @param binDir - directory containing class files
	 * @param jars - jar files that should be part of the analysis
	 * @return
	 * @throws IOException
	 */
	public static JacocoCoverageReport parseExecFile(final File execFile, final File binDir, final Set<File> jars)
			throws IOException {

		ExecutionDataVisitor visitor = new ExecutionDataVisitor();
		ExecutionDataReader reader = new ExecutionDataReader(new FileInputStream(execFile));
		reader.setExecutionDataVisitor(visitor);
		reader.setSessionInfoVisitor(visitor);
		reader.read();

		// read merged results, over all sessions
		CoverageBuilder mergedBuilder = new CoverageBuilder();
		Analyzer analyzer = new Analyzer(visitor.getMerged(), mergedBuilder);
		analyzeBins(analyzer, binDir, jars);

		Map<String, JacocoPackage> packages = new HashMap<String, JacocoPackage>();
		Map<String, JacocoMethod> methods = new HashMap<String, JacocoMethod>();
		for (final IClassCoverage cc : mergedBuilder.getClasses()) {
			String pkg = getPackageName(cc);
			JacocoPackage jPkg = packages.get(pkg);
			if(jPkg == null) {
				jPkg = new JacocoPackage(pkg);
				packages.put(pkg, jPkg);
			}
			String className = getClassName(cc);
			JacocoClass jClass = new JacocoClass(jPkg, className);
			for (final IMethodCoverage mc : cc.getMethods()) {
				String signature = getMethodSgnature(cc, mc);
				Set<Integer> lines = getLines(mc, pkg, className, signature);
				JacocoMethod jMethod = new JacocoMethod(jClass, signature, lines);
				methods.put(jMethod.getFullName(), jMethod);
			}
		}
		
		JacocoCoverageReport report = new JacocoCoverageReport(packages.values());

		for (Map.Entry<String, ExecutionDataStore> entry : visitor.getSessions().entrySet()) {
			if(entry.getKey().equals("No-Test")) {
				continue;
			}
			
			CoverageBuilder coverageBuilder = new CoverageBuilder();
			analyzer = new Analyzer(entry.getValue(), coverageBuilder);
			analyzeBins(analyzer, binDir, jars);

			JacocoSession session = new JacocoSession(entry.getKey());
			
			for (final IClassCoverage cc : coverageBuilder.getClasses()) {
				String pkg = getPackageName(cc);
				String className = getClassName(cc);
				for (final IMethodCoverage mc : cc.getMethods()) {
					String signature = getMethodSgnature(cc, mc);
					String key = pkg + "." + className + "." + signature;
					JacocoMethod jMethod = methods.get(key);
					Set<Integer> linesCovered = getLinesCovered(mc, pkg, className, signature);
					if(!linesCovered.isEmpty()) {
						JacocoMethodCoverage methodCoverage = new JacocoMethodCoverage(jMethod, linesCovered);
						session.addCoverage(methodCoverage);
					}
				}
			}
			report.addSession(session);
		}
		return report;
	}

	private static void analyzeBins(final Analyzer analyzer, final File binDir, final Set<File> jars)
			throws IOException {
		analyzer.analyzeAll(binDir);
		if (jars != null) {
			for (File jar : jars) {
				try {
					analyzer.analyzeAll(jar);
				} catch (IOException e) {
					System.err.println("ERROR: " + e.getMessage());
				}
			}
		}
	}

	private static Set<Integer> getLines(final IMethodCoverage mc, String pkg, String className, String methodName) {
		Set<Integer> coveredLines = new HashSet<>();
		for (int i = mc.getFirstLine(); i <= mc.getLastLine(); i++) {
			ILine line = mc.getLine(i);
			if(line.getStatus() != ICounter.EMPTY) {
				coveredLines.add(i);
			}
		}
		return coveredLines;
	}
	
	private static Set<Integer> getLinesCovered(final IMethodCoverage mc, String pkg, String className, String methodName) {
		Set<Integer> coveredLines = new HashSet<>();
		for (int i = mc.getFirstLine(); i <= mc.getLastLine(); i++) {
			ILine line = mc.getLine(i);
			if(line.getStatus() != ICounter.EMPTY && line.getStatus() != ICounter.NOT_COVERED) {
				coveredLines.add(i);
			}
		}
		return coveredLines;
	}

	private static String getPackageName(final IClassCoverage cc) {
		String pkg = cc.getPackageName();
		return pkg.replace("/", ".");
	}

	private static String getClassName(final IClassCoverage cc) {
		String[] pkg = cc.getName().split("/");
		return pkg[pkg.length - 1];
	}

	private static String getMethodSgnature(final IClassCoverage cc, final IMethodCoverage mc) {
		JavaNames names = new JavaNames();
		return names.getMethodName(cc.getName(), mc.getName(), mc.getDesc(), mc.getSignature());
	}

}
