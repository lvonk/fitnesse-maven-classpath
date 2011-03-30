package fitnesse.wikitext.widgets;

import fitnesse.html.HtmlUtil;
import fitnesse.wiki.PageData;
import fitnesse.wikitext.WidgetBuilder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenClasspathWidget extends ParentWidget implements WidgetWithTextArgument {

    private DependencyResolvingMavenEmbedder mavenEmbedder;

    static {
        PageData.classpathWidgetBuilder.addWidgetClass(MavenClasspathWidget.class);

    }

    private String pomFile;
    private File userSettingsFile = MavenCli.DEFAULT_USER_SETTINGS_FILE;
    private File globalSettingsFile = MavenCli.DEFAULT_GLOBAL_SETTINGS_FILE;

    public static final String REGEXP = "^!pomFile [^\r\n]*";
    private static final Pattern pattern = Pattern.compile("^!pomFile (.*)");

    public MavenClasspathWidget(ParentWidget parent, String text) throws Exception {
        super(parent);

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String matchedGroup = matcher.group(1);
            addChildWidgets(matchedGroup);
            this.pomFile = childHtml();
            ensurePomFileExists();
        } else {
            throw new IllegalArgumentException("no pom file specified.");
        }
    }

    private void ensurePomFileExists() {
        if (!new File(pomFile).exists()) {
            throw new IllegalArgumentException(pomFile + " does not exist");
        }
    }

    @Override
    public String asWikiText() throws Exception {
        return "!pomFile " + pomFile;
    }

    @Override
    public WidgetBuilder getBuilder() {
        return WidgetBuilder.variableEvaluatorWidgetBuilder;
    }

    public String getText() throws Exception {
        List<String> classpathElements = getMavenClasspath();
        return createClasspath(classpathElements);
    }

    private List<String> getMavenClasspath() throws DependencyResolutionRequiredException, MavenEmbedderException, ProjectBuildingException, ComponentLookupException {
        MavenRequest mavenRequest = mavenConfiguration();
        mavenRequest.setResolveDependencies(true);
        mavenRequest.setBaseDirectory(projectRootDirectory().getAbsolutePath());
        mavenRequest.setPom(new File(projectRootDirectory(), "pom.xml").getAbsolutePath());

        DependencyResolvingMavenEmbedder dependencyResolvingMavenEmbedder = new DependencyResolvingMavenEmbedder(getClass().getClassLoader(), mavenRequest);

        ProjectBuildingResult projectBuildingResult = dependencyResolvingMavenEmbedder.buildProject(new File(projectRootDirectory(), "pom.xml"));
        return projectBuildingResult.getProject().getTestClasspathElements();
    }

    @Override
    public String render() throws Exception {
        List<String> classpathElements = getMavenClasspath();

        String classpathForRender = "";
        for (String element : classpathElements) {
            classpathForRender += HtmlUtil.metaText("classpath: " + element) + HtmlUtil.BRtag;

        }
        return classpathForRender;

    }

    private String createClasspath(List<String> classpathElements) {
        String classpath = "";
        for (String element : classpathElements) {
            classpath += element + File.pathSeparator;
        }
        return removeTrailingPathSeparator(classpath);
    }

    private String removeTrailingPathSeparator(String classpath) {
        return classpath.substring(0, classpath.length() - 1);
    }

    private File projectRootDirectory() {
        String root = pomFile.substring(0, pomFile.lastIndexOf("/"));
        File projectDirectory = new File(root);
        return projectDirectory;
    }

    private MavenExecutionRequest createExecutionRequest(File projectDirectory) {
        return new DefaultMavenExecutionRequest().setBaseDirectory(projectDirectory).setPom(new File(pomFile));
    }

    // protected for test purposes
    protected MavenRequest mavenConfiguration() throws MavenEmbedderException, ComponentLookupException {
        MavenRequest mavenRequest = new MavenRequest();

        if (userSettingsFile != null && userSettingsFile.exists()) {
            mavenRequest.setUserSettingsFile(userSettingsFile.getAbsolutePath());
        }
        if (globalSettingsFile != null && globalSettingsFile.exists()) {
            mavenRequest.setGlobalSettingsFile(globalSettingsFile.getAbsolutePath());
        }

        DependencyResolvingMavenEmbedder mavenEmbedder = new DependencyResolvingMavenEmbedder(MavenClasspathWidget.class.getClassLoader(), mavenRequest);
        mavenEmbedder.getMavenRequest().setLocalRepositoryPath(getLocalRepository(mavenEmbedder.getSettings().getLocalRepository()));

        return mavenEmbedder.getMavenRequest();
    }

    /*
    * can be overridden for test purposes.
    */
    protected String getLocalRepository(String localRepository) {
        return localRepository;
    }


    // protected for test purposes
    protected void setMavenUserSettingsFile(File userSettingsFile) {
        this.userSettingsFile = userSettingsFile;
    }

    // protected for test purposes
    protected void setMavenGlobalSettingsFile(File globalSettingsFile) {
        this.globalSettingsFile = globalSettingsFile;
    }
}
