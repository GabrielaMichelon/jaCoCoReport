package at.sfischer.statementCoverage;

import at.sfischer.jacocoReader.*;
import at.sfischer.splinter.project.Project;
import at.sfischer.splinter.source.artifacts.Artifact;
import at.sfischer.splinter.source.artifacts.iterator.ArtifactTreeIterator;
import at.sfischer.splinter.source.java.JavaSourceType;
import at.sfischer.splinter.source.java.utils.JDTArtifactTypes;
import at.sfischer.splinter.source.java.utils.JDTArtifactUtils;
import at.sfischer.splinter.source.parser.SourceParser;
import at.sfischer.splinter.source.parser.java.JavaJDTParser;
import at.sfischer.splinter.source.service.artifact.diff.MatchArtifact;
import at.sfischer.splinter.source.service.artifact.diff.SimpleMatchService;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class TestClassArgoUML {

    private static final String TEMPLATE = "C:\\Users\\gabil\\Desktop\\teste\\Scenarios\\ArgoUML\\test_coverage\\USECASEDIAGRAM.1";

    private static final String VARIANTS = "C:\\Users\\gabil\\Desktop\\teste\\Scenarios\\ArgoUML\\test_coverage\\USECASEDIAGRAM";
//    private static final String VARIANTS = "C:\\Users\\sfischer\\ArgoUMLAllFeaturesVariant\\";

    private static final String RESULTS = "C:\\Users\\gabil\\Desktop\\teste\\Scenarios\\ArgoUML\\test_coverage\\results";

    public static void main(String[] args) throws IOException {

        Project project = new Project("ArgoUML");
        project.setSourceTypesToHandle(JavaSourceType.getInstance());

        File fullDir = new File(TEMPLATE);

        Set<Artifact> fullArtifacts = new HashSet<>();
        parserArgoUMLVariant(fullDir, project, fullArtifacts);

        File results = new File(RESULTS);
        File variants = new File(VARIANTS);
        //for(File variantDir : variants.listFiles()){
        //if(variantDir.isDirectory()){
        File resultDir = new File(results, variants.getName());
        analyseCoverage(project, fullArtifacts, variants, resultDir, fullArtifacts);
        //}
        // }
    }

    private static void parserArgoUMLVariant(File dir, Project project, Set<Artifact> artifacts) {
        File appSrc = new File(dir, "src\\org");
        if (appSrc.exists()) {
            Set<Artifact> moduleArtifacts = SourceParser.parseSources(appSrc, project);
            artifacts.addAll(moduleArtifacts);
        }
    }

    private static JacocoCoverageReport parserArgoUMLVariantCoverage(File dir) throws IOException {
        Set<JacocoPackage> packages = new HashSet<>();
        Set<JacocoSession> sessions = new HashSet<>();
        File app = dir;
        //if (app.exists()) {
        File jacocoReport = new File(app, "jacoco.exec");
        File binDir = new File(app, "bin\\org");
        if (jacocoReport.exists() && binDir.exists()) {
            JacocoCoverageReport report = JacocoReportGenerator.parseExecFile(jacocoReport, binDir);
            packages.addAll(report.getPackages());
            sessions.addAll(report.getSessions());
        }
        //}

        JacocoCoverageReport coverageReport = new JacocoCoverageReport(packages);
        coverageReport.addSession(JacocoCoverageReport.union(sessions));
        return coverageReport;
    }

    private static void analyseCoverage(Project project, Set<Artifact> fullArtifacts, File variantDir, File resultDir, Set<Artifact> artifacts) throws IOException {
        Set<Artifact> varArtifacts = new HashSet<>();
        //varArtifacts = artifacts;
        parserArgoUMLVariant(variantDir, project, varArtifacts);
        System.out.println("entered analyseCoverage");
        JacocoCoverageReport coverageReport = parserArgoUMLVariantCoverage(variantDir);
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
        Map<String, ArrayList<Integer>> linesMethodoCover = new HashMap<>();

        while (it.hasNext()) {
            MatchArtifact<Artifact> a = it.next();

            if (a.getLeftArtifact() == null) {
//                System.err.println(a);
                continue;
            }

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
                    methodCoverage = null;
                }
            }

            if (JDTArtifactTypes.isStatement(a.getLeftArtifact())) {
                if (!JDTArtifactTypes.isBlock(a.getLeftArtifact())) {
                    if (a.getRightArtifact() != null) {

//                        if(methodCoverage == null){
////                            System.err.println("No coverage report found for: " + method.getFullName() + " : type");
////                        }

                        Optional<Integer> lineOpt = a.getRightArtifact().getProperty(JavaJDTParser.LINE);
                        if (lineOpt.isPresent()) {
                            Integer line = lineOpt.get();
                            //check if it was executed
                            boolean covered = methodCoverage != null && methodCoverage.getLinesCovered().contains(line.intValue());
                            if (covered) {
                                varStatementCovered++;

                                if (!statementInMethodCovered) {
                                    statementInMethodCovered = true;
                                    varMethodCovered++;
                                    coveredMethods.add(method.getFullName());
                                    ArrayList<Integer> numberLines = new ArrayList<>();
                                    for (Integer lineA : methodCoverage.getLinesCovered()) {
                                        numberLines.add(lineA);
                                    }
                                    linesMethodoCover.put(method.getFullName(), numberLines);
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


        resultDir.mkdirs();
        File resultDir2 = new File(resultDir, "FilesRuntime");
        resultDir2.mkdirs();
        //create file to each class covered and add the line numbers of lines covered
        for (Map.Entry<String, ArrayList<Integer>> entry : linesMethodoCover.entrySet()) {
            File f = new File(resultDir2, entry.getKey().substring(0, entry.getKey().lastIndexOf(".")) + ".runtime");
            if (!f.exists()) {
                PrintStream coveredClass = new PrintStream(f);
                coveredClass.println(entry.getKey().substring(0, entry.getKey().lastIndexOf(".")).replace(".", "\\"));
                for (Integer lineNum : entry.getValue()) {
                    //coveredClass.println(entry.getKey().substring(entry.getKey().lastIndexOf(".")+1)+": "+lineNum);
                    coveredClass.println(lineNum);
                }
                coveredClass.flush();
                coveredClass.close();

            } else {
                try {
                    String covered = "";
                    for (Integer lineNum : entry.getValue()) {
                        covered += lineNum + "\r\n";//entry.getKey().substring(entry.getKey().lastIndexOf(".")+1)+": "+lineNum+"\r\n";
                    }
                    Files.write(Paths.get(f.getAbsolutePath()), covered.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
                } catch (IOException e) {
                    //exception handling left as an exercise for the reader
                }
            }
        }

        System.out.println("Static statement coverage: " + varStatementCount + " / " + fullStatementCount);
        System.out.println("Static method coverage: " + varMethodCount + " / " + fullMethodCount);

        System.out.println("Dynamic statement coverage: " + varStatementCovered + " / " + fullStatementCount);
        System.out.println("Dynamic method coverage: " + varMethodCovered + " / " + fullMethodCount);


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
        if (coverageReport == null) {
            return null;
        }
        for (JacocoPackage aPackage : coverageReport.getPackages()) {
            if (aPackage.getName().equals(package_)) {
                return aPackage;
            }
        }
        return null;
    }

    private static JacocoClass getClass(String class_, JacocoPackage package_) {
        if (package_ == null) {
            return null;
        }
        for (JacocoClass aClass : package_.getClasses()) {
            if (aClass.getName().equals(class_)) {
                return aClass;
            }
        }
        return null;
    }

    private static JacocoMethod getMethod(String methodSignature, JacocoClass class_) {
        if (class_ == null) {
            return null;
        }
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
        int index1 = methodSignature1.indexOf('(');
        int index2 = methodSignature2.indexOf('(');
        if (index1 < 0 && index2 < 0) {
            methodSignature1.equals(methodSignature2);
        } else if (index1 < 0 || index2 < 0) {
            return false;
        }
        String methodName1 = methodSignature1.substring(0, index1);
        String methodName2 = methodSignature2.substring(0, index2);
        return methodName1.equals(methodName2);
    }

}
