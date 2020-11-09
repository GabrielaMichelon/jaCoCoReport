package at.sfischer.statementCoverage;

import at.sfischer.jacocoReader.*;
import at.sfischer.splinter.project.Project;
import at.sfischer.splinter.source.artifacts.Artifact;
import at.sfischer.splinter.source.artifacts.iterator.ArtifactTreeIterator;
import at.sfischer.splinter.source.artifacts.printer.ArtifactPrinter;
import at.sfischer.splinter.source.java.JavaSourceType;
import at.sfischer.splinter.source.java.utils.JDTArtifactTypes;
import at.sfischer.splinter.source.java.utils.JDTArtifactUtils;
import at.sfischer.splinter.source.parser.SourceParser;
import at.sfischer.splinter.source.parser.java.JavaJDTParser;
import at.sfischer.splinter.source.service.artifact.diff.MatchArtifact;
import at.sfischer.splinter.source.service.artifact.diff.SimpleMatchService;
import org.eclipse.jdt.core.IJavaElement;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ComputeStatementCoverageStack {

    private static final String TEMPLATE = "C:\\Stefan_Paper\\actualGit\\2019-conf-Teclo-paper\\CaseStudies\\Stack\\StackSPL";

    private static final String VARIANTS = "C:\\Users\\gabil\\Again_StackSPL2\\splIndTest2\\";

    private static final String RESULTS = "C:\\Users\\gabil\\Again_StackSPL2\\coverage_original_tests\\spl_pairwise_feat2";//(new File(System.getProperty("user.home")), "StackSPL_Coverage").getAbsolutePath();

    public static void main(String[] args) throws IOException {

        Project project = new Project("StackSPL");
        project.setSourceTypesToHandle(JavaSourceType.getInstance());

        File fullDir = new File(TEMPLATE);

        Set<Artifact> fullArtifacts = SourceParser.parseSources(new File(fullDir, "src\\main\\java"), project);

        File results = new File(RESULTS);
        File variants = new File(VARIANTS);
        for (File variantDir : variants.listFiles()) {
            if (variantDir.isDirectory()) {
                File resultDir = new File(results, variantDir.getName());
                analyseCoverage(project, fullArtifacts, variantDir, resultDir);
            }
        }
    }

    private static void analyseCoverage(Project project, Set<Artifact> fullArtifacts, File variantDir, File resultDir) throws IOException {
        Set<Artifact> varArtifacts = SourceParser.parseSources(new File(variantDir, "src\\main\\java"), project);

        File jacocoReport = new File(variantDir, "target\\jacoco.exec");
        File binDir = new File(variantDir, "target\\classes");

        JacocoCoverageReport coverageReport = JacocoReportGenerator.parseExecFile(jacocoReport, binDir);
        JacocoSession session = coverageReport.union();

        Set<MatchArtifact<Artifact>> match = SimpleMatchService.process(fullArtifacts, varArtifacts);

        ArtifactTreeIterator<MatchArtifact<Artifact>> it = new ArtifactTreeIterator<>(match);

        JacocoPackage package_ = null;
        JacocoClass type = null;
        JacocoMethod method = null;
        JacocoMethodCoverage methodCoverage = null;

        int fullStatementCount = 0;
        int varStatementCount = 0;
        int fullMethodCount = 0;
        int varMethodCount = 0;
        int varStatementCovered = 0;
        int varMethodCovered = 0;

        boolean statementInMethodCovered = false;

        List<String> coveredMethods = new LinkedList<>();

        while (it.hasNext()) {
            MatchArtifact<Artifact> a = it.next();

            if (JDTArtifactTypes.isCompilationUnit(a.getLeftArtifact())) {
                Artifact pkg = JDTArtifactUtils.getChildPropertyDescriptorContent(a.getLeftArtifact(), "package");
                if (pkg != null) {
                    Artifact name = JDTArtifactUtils.getChildPropertyDescriptorContent(pkg, "name");
                    package_ = getPackage(name.getId(), coverageReport);
                }
            }

            if (JDTArtifactTypes.isTypeDeclaration(a.getLeftArtifact())) {
                type = getClass(a.getLeftArtifact().getId(), package_);
            }
            if (JDTArtifactTypes.isMethodDeclaration(a.getLeftArtifact())) {
                method = getMethod(a.getLeftArtifact().getId(), type);
                if (method != null) {
                    methodCoverage = session.getCoverage(method.getFullName());
                } else {
                    it.skipChildren();
                }
            }

            if (JDTArtifactTypes.isStatement(a.getLeftArtifact())) {
                if (!JDTArtifactTypes.isBlock(a.getLeftArtifact())) {
                    if (a.getRightArtifact() != null) {

                        if (methodCoverage == null) {
                            System.err.println("No coverage report found for: " + method.getFullName() + " : type");
                        }

                        Optional<Integer> lineOpt = a.getRightArtifact().getProperty(JavaJDTParser.LINE);
                        if (lineOpt != null) {
                            Integer line = lineOpt.get();
                            //check if it was executed
                            boolean covered = methodCoverage != null && methodCoverage.getLinesCovered().contains(line.intValue());
                            if (covered) {
                                varStatementCovered++;

                                if (!statementInMethodCovered) {
                                    statementInMethodCovered = true;
                                    varMethodCovered++;
                                    coveredMethods.add(method.getFullName());
                                }
                            }

                        } else {
                            System.err.println("No line number for: " + a.getRightArtifact());
                        }
                        varStatementCount++;
                    }
                    fullStatementCount++;

                    //skip artifacts below expression
                    if (JDTArtifactTypes.isExpressionStatement(a.getLeftArtifact())) {
                        it.skipChildren();
                    }
                }
            }

            if (JDTArtifactTypes.isMethodDeclaration(a.getLeftArtifact())) {
                if (a.getRightArtifact() != null) {
                    //check if it was called; any statement inside was executed
                    statementInMethodCovered = false;
                    varMethodCount++;
                }
                fullMethodCount++;
            }

            //skip artifacts below expression
            if (JDTArtifactTypes.isExpression(a.getLeftArtifact())) {
                it.skipChildren();
            }
        }

        System.out.println("Static statement coverage: " + varStatementCount + " / " + fullStatementCount);
        System.out.println("Static method coverage: " + varMethodCount + " / " + fullMethodCount);

        System.out.println("Dynamic statement coverage: " + varStatementCovered + " / " + fullStatementCount);
        System.out.println("Dynamic method coverage: " + varMethodCovered + " / " + fullMethodCount);

        resultDir.mkdirs();

        //print coverage results into results directory
        PrintStream fullStatement = new PrintStream(new File(resultDir, "fullStatementCount.csv"));
        PrintStream fullMethod = new PrintStream(new File(resultDir, "fullMethodCount.csv"));
        PrintStream varStatement = new PrintStream(new File(resultDir, "varStatementCount.csv"));
        PrintStream varMethod = new PrintStream(new File(resultDir, "varMethodCount.csv"));
        PrintStream statementCovered = new PrintStream(new File(resultDir, "varStatementCovered.csv"));
        PrintStream methodCovered = new PrintStream(new File(resultDir, "varMethodCovered.csv"));

        fullStatement.println(fullStatementCount);
        fullMethod.println(fullMethodCount);
        varStatement.println(varStatementCount);
        varMethod.println(varMethodCount);
        statementCovered.println(varStatementCovered);
        methodCovered.println(varMethodCovered);

        fullStatement.flush();
        fullStatement.close();
        fullMethod.flush();
        fullMethod.close();
        varStatement.flush();
        varStatement.close();
        varMethod.flush();
        varMethod.close();
        statementCovered.flush();
        statementCovered.close();
        methodCovered.flush();
        methodCovered.close();

        PrintStream coveredMethodsStream = new PrintStream(new File(resultDir, "coveredMethods.txt"));
        for (String s : coveredMethods) {
            coveredMethodsStream.println(s);
        }
        coveredMethodsStream.flush();
        coveredMethodsStream.close();

    }

    private static JacocoPackage getPackage(String package_, JacocoCoverageReport coverageReport) {
        for (JacocoPackage aPackage : coverageReport.getPackages()) {
            if (aPackage.getName().equals(package_)) {
                return aPackage;
            }
        }
        return null;
    }

    private static JacocoClass getClass(String class_, JacocoPackage package_) {
        for (JacocoClass aClass : package_.getClasses()) {
            if (aClass.getName().equals(class_)) {
                return aClass;
            }
        }
        return null;
    }

    private static JacocoMethod getMethod(String methodSignature, JacocoClass class_) {
        for (JacocoMethod method : class_.getMethods()) {
            if (method.getSignature().equals(methodSignature)) {
                return method;
            }
        }
        for (JacocoMethod method : class_.getMethods()) {
            if (equalMethodNames(method.getSignature(), methodSignature)) {
                return method;
            }
        }
        return null;
    }

    private static boolean equalMethodNames(String methodSignature1, String methodSignature2) {
        String methodName1 = methodSignature1.substring(0, methodSignature1.indexOf('('));
        String methodName2 = methodSignature2.substring(0, methodSignature2.indexOf('('));
        return methodName1.equals(methodName2);
    }


}
