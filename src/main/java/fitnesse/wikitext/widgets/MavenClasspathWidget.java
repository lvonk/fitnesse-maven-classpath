package fitnesse.wikitext.widgets;

import static org.apache.maven.embedder.MavenEmbedder.*;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.settings.Settings;

import fitnesse.html.HtmlUtil;
import fitnesse.wiki.PageData;
import fitnesse.wikitext.WidgetBuilder;

public class MavenClasspathWidget extends ParentWidget implements WidgetWithTextArgument  {

  static {
    PageData.classpathWidgetBuilder.addWidgetClass(MavenClasspathWidget.class);
  }
  
  private String pomFile;
	private File userSettingsFile = MavenEmbedder.DEFAULT_USER_SETTINGS_FILE;
	private File globalSettingsFile = MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE;
	
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
    if(!new File(pomFile).exists()) {
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

  private List<String> getMavenClasspath() throws MavenEmbedderException, DependencyResolutionRequiredException {
    Configuration configuration = mavenConfiguration();
    ensureMavenConfigurationIsValid(configuration);
    MavenExecutionRequest request = createExecutionRequest(projectRootDirectory());
    List<String> classpathElements = getClasspathElements(configuration, request);
    return classpathElements;
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

  private void ensureMavenConfigurationIsValid(Configuration configuration) {
    ConfigurationValidationResult validationResult = validateMavenConfiguration(configuration);
    if (!validationResult.isValid()) {
      throw new IllegalStateException("Unable to create valid Maven Configuration.");
    }
  }

	private ConfigurationValidationResult validateMavenConfiguration(Configuration configuration) {
	  return validateConfiguration(configuration);
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

  private List<String> getClasspathElements(Configuration configuration, MavenExecutionRequest request)
      throws MavenEmbedderException, DependencyResolutionRequiredException {
    MavenEmbedder embedder = new MavenEmbedder(configuration);
    MavenExecutionResult executionResult = embedder.readProjectWithDependencies(request);
    @SuppressWarnings("unchecked")
    List<String> classpathElements = executionResult.getProject().getTestClasspathElements();
    return classpathElements;
  }

  private File projectRootDirectory() {
    String root = pomFile.substring(0, pomFile.lastIndexOf("/"));
    File projectDirectory = new File(root);
    return projectDirectory;
  }

  private MavenExecutionRequest createExecutionRequest(File projectDirectory) {
    return new DefaultMavenExecutionRequest().setBaseDirectory(projectDirectory).setPomFile(pomFile);
  }

  // protected for test purposes
  protected Configuration mavenConfiguration() {
    Configuration configuration = new DefaultConfiguration()
      .setUserSettingsFile(userSettingsFile)
      .setClassLoader(Thread.currentThread().getContextClassLoader())
    	.setMavenEmbedderLogger(new MavenEmbedderConsoleLogger());
    
    if (globalSettingsFile != null && globalSettingsFile.exists()) {
    	configuration.setGlobalSettingsFile(globalSettingsFile);
    }
    
    if (hasNonDefaultLocalRepository(configuration)) {
      configuration.setLocalRepository(getLocalRepository(configuration));
    }
    
    return configuration;
  }

  private boolean hasNonDefaultLocalRepository(Configuration configuration) {
    return getLocalRepository(configuration) != null;
  }
  
  /*
   * can be overridden for test purposes.
   */
  protected File getLocalRepository(Configuration configuration) {
  	String localRepositoryPath = null;

  	ConfigurationValidationResult validateMavenConfiguration = validateMavenConfiguration(configuration);
  	Settings userSettings = validateMavenConfiguration.getUserSettings();
  	
		if (userSettings != null) {
			localRepositoryPath = userSettings.getLocalRepository();
		} else if (validateMavenConfiguration.getGlobalSettings() != null) {
			Settings globalSettings = validateMavenConfiguration.getGlobalSettings();
			localRepositoryPath = globalSettings.getLocalRepository();
		} else {
			return MavenEmbedder.defaultUserLocalRepository;
		}
		
		return getLocalRepositoryLocation(localRepositoryPath);
  }

	private File getLocalRepositoryLocation(String localRepositoryPath) {
	  if (localRepositoryPath == null) {
			return MavenEmbedder.defaultUserLocalRepository;
		}
		
  	return new File(localRepositoryPath);
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
