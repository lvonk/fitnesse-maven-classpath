package fitnesse.wikitext.widgets;

import fitnesse.html.HtmlUtil;
import fitnesse.wikitext.parser.*;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import util.Maybe;

import java.io.File;
import java.util.List;

/**
 * FitNesse SymbolType implementation which enables Maven classpath integration for FitNesse.
 */
public class MavenClasspathSymbolType extends SymbolType implements Rule, Translation {

    private String pomFile;
    private File userSettingsFile = MavenCli.DEFAULT_USER_SETTINGS_FILE;
    private File globalSettingsFile = MavenCli.DEFAULT_GLOBAL_SETTINGS_FILE;


    public MavenClasspathSymbolType() {
        super("MavenClasspathSymbolType");

        wikiMatcher(new Matcher().startLineOrCell().string("!pomFile"));

        wikiRule(this);
        htmlTranslation(this);
    }

    @Override
    public String toTarget(Translator translator, Symbol symbol) {
        pomFile = symbol.childAt(0).getContent();

        try {
            List<String> classpathElements = getMavenClasspath();

            String classpathForRender = "";
            for (String element : classpathElements) {
                classpathForRender += HtmlUtil.metaText("classpath: " + element) + HtmlUtil.BRtag;

            }
            return classpathForRender;

        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalArgumentException(e);
        } catch (MavenEmbedderException e) {
            throw new IllegalArgumentException(e);
        } catch (ProjectBuildingException e) {
            throw new IllegalArgumentException(e);
        } catch (ComponentLookupException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Maybe<Symbol> parse(Symbol symbol, Parser parser) {
        Symbol next = parser.moveNext(1);

        if (!next.isType(SymbolType.Whitespace)) return Symbol.nothing;

        symbol.add(parser.moveNext(1).getContent());


        return new Maybe<Symbol>(symbol);
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

    private List<String> getMavenClasspath() throws DependencyResolutionRequiredException, MavenEmbedderException, ProjectBuildingException, ComponentLookupException {
        MavenRequest mavenRequest = mavenConfiguration();
        mavenRequest.setResolveDependencies(true);
        mavenRequest.setBaseDirectory(projectRootDirectory().getAbsolutePath());
        mavenRequest.setPom(new File(projectRootDirectory(), "pom.xml").getAbsolutePath());

        DependencyResolvingMavenEmbedder dependencyResolvingMavenEmbedder = new DependencyResolvingMavenEmbedder(getClass().getClassLoader(), mavenRequest);

        ProjectBuildingResult projectBuildingResult = dependencyResolvingMavenEmbedder.buildProject(new File(projectRootDirectory(), "pom.xml"));
        return projectBuildingResult.getProject().getTestClasspathElements();
    }


}


