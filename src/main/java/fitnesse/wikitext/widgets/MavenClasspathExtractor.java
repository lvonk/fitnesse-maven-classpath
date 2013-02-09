package fitnesse.wikitext.widgets;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.project.*;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.*;
import org.codehaus.plexus.*;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLoggerManager;
import org.codehaus.plexus.util.Os;
import org.sonatype.aether.RepositorySystemSession;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class to extract classpath elements from Maven projects. Heavily based on code copied from Jenkin's Maven
 * support.
 */
public class MavenClasspathExtractor {

	public final static String DEFAULT_SCOPE = "test";

	private final Logger logger = new ConsoleLoggerManager().getLoggerForComponent("maven-classpath-plugin");
	
	private PlexusContainer plexusContainer;

    // Ensure M2_HOME variable is handled in a way similar to the mvn executable (script). To the extend possible.
    static {
        String m2Home = System.getenv().get("M2_HOME");
        if (m2Home != null && System.getProperty("maven.home") == null) {
            System.setProperty("maven.home", m2Home);
        }
    }

    public MavenClasspathExtractor() throws PlexusContainerException {
    	plexusContainer = buildPlexusContainer(getClass().getClassLoader(), null);
    }
    
    public List<String> extractClasspathEntries(File pomFile) {
		return extractClasspathEntries(pomFile, DEFAULT_SCOPE);
	}

    public List<String> extractClasspathEntries(File pomFile, String scope) throws MavenClasspathExtractionException {

        try {
            MavenExecutionRequest mavenExecutionRequest = mavenConfiguration();
            mavenExecutionRequest.setBaseDirectory(pomFile.getParentFile());
            mavenExecutionRequest.setPom(pomFile);

            ProjectBuildingResult projectBuildingResult = buildProject(pomFile, mavenExecutionRequest);
            
            return getClasspathForScope(projectBuildingResult, scope);

        } catch (ComponentLookupException e) {
            throw new MavenClasspathExtractionException(e);
        } catch (DependencyResolutionRequiredException e) {
            throw new MavenClasspathExtractionException(e);
        } catch (ProjectBuildingException e) {
            throw new MavenClasspathExtractionException(e);
		}
    }

	private List<String> getClasspathForScope(
			ProjectBuildingResult projectBuildingResult, String scope)
			throws DependencyResolutionRequiredException {
		MavenProject project = projectBuildingResult.getProject();
		
		if ("compile".equalsIgnoreCase(scope)) {
			return project.getCompileClasspathElements();
		} else if ("runtime".equalsIgnoreCase(scope)) {
			return project.getRuntimeClasspathElements();
		}
		return project.getTestClasspathElements();
		
	}

    // protected for test purposes
    protected MavenExecutionRequest mavenConfiguration() throws MavenClasspathExtractionException {
        MavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest();

    	try {
			MavenExecutionRequestPopulator executionRequestPopulator = lookup(MavenExecutionRequestPopulator.class);
	        MavenExecutionRequestPopulator populator = lookup(MavenExecutionRequestPopulator.class);
	
	    	mavenExecutionRequest.setInteractiveMode(false);
	    	
	    	mavenExecutionRequest.setSystemProperties(System.getProperties());
	    	mavenExecutionRequest.getSystemProperties().putAll(getEnvVars());
	    	
	        Settings settings = getSettings(mavenExecutionRequest);
	
	        executionRequestPopulator.populateFromSettings(mavenExecutionRequest, settings);
	        populator.populateDefaults(mavenExecutionRequest);
	        
	        logger.debug( "Local repository " + mavenExecutionRequest.getLocalRepository());
	
		} catch (ComponentLookupException e) {
            throw new MavenClasspathExtractionException(e);
		} catch (SettingsBuildingException e) {
            throw new MavenClasspathExtractionException(e);
		} catch (MavenExecutionRequestPopulationException e) {
            throw new MavenClasspathExtractionException(e);
		}
        return mavenExecutionRequest;
    }

	private Settings getSettings(MavenExecutionRequest mavenExecutionRequest)
			throws ComponentLookupException, SettingsBuildingException {
		
		SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();

		// TODO: should be configurable by system properties?
		File globalSettingsFile = MavenCli.DEFAULT_GLOBAL_SETTINGS_FILE;
		File userSettingsFile = MavenCli.DEFAULT_USER_SETTINGS_FILE;

		settingsRequest.setGlobalSettingsFile(globalSettingsFile);
		settingsRequest.setUserSettingsFile(userSettingsFile);

		settingsRequest.setSystemProperties(mavenExecutionRequest
				.getSystemProperties());
		settingsRequest.setUserProperties(mavenExecutionRequest
				.getUserProperties());

		logger.debug("Reading global settings from " + settingsRequest.getGlobalSettingsFile());
		logger.debug("Reading user settings from "	+ settingsRequest.getUserSettingsFile());

		SettingsBuilder settingsBuilder = lookup(SettingsBuilder.class);

		SettingsBuildingResult settingsResult = settingsBuilder
				.build(settingsRequest);

		if (!settingsResult.getProblems().isEmpty()) {
			logger.warn("");
			logger.warn("Some problems were encountered while building the effective settings");

			for (SettingsProblem problem : settingsResult.getProblems()) {
				logger.warn(problem.getMessage() + " @ "
						+ problem.getLocation());
			}

			logger.warn("");
		}

		return settingsResult.getEffectiveSettings();
	}

    private Properties getEnvVars() {
        Properties envVars = new Properties();
        boolean caseSensitive = !Os.isFamily(Os.FAMILY_WINDOWS);
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = "env." + (caseSensitive ? entry.getKey() : entry.getKey().toUpperCase(Locale.ENGLISH));
            envVars.setProperty(key, entry.getValue());
        }
        return envVars;
    }


    public ProjectBuildingResult buildProject(File mavenProject, MavenExecutionRequest mavenExecutionRequest) throws ProjectBuildingException, ComponentLookupException {
        //ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try {
            //Thread.currentThread().setContextClassLoader(this.plexusContainer.getContainerRealm());
            ProjectBuilder projectBuilder = lookup(ProjectBuilder.class);
            ProjectBuildingRequest projectBuildingRequest = mavenExecutionRequest.getProjectBuildingRequest();

            //projectBuildingRequest.setValidationLevel(this.mavenRequest.getValidationLevel());

            RepositorySystemSession repositorySystemSession = buildRepositorySystemSession(mavenExecutionRequest);

            projectBuildingRequest.setRepositorySession(repositorySystemSession);

            projectBuildingRequest.setProcessPlugins(false); //mavenRequest.isProcessPlugins());

            projectBuildingRequest.setResolveDependencies(true); //this.mavenRequest.isResolveDependencies());

            return projectBuilder.build(mavenProject, projectBuildingRequest);
        } finally {
            //Thread.currentThread().setContextClassLoader(originalCl);
        }

    }
    
    public <T> T lookup(Class<T> clazz) throws ComponentLookupException {
        return plexusContainer.lookup(clazz);
    }

    private RepositorySystemSession buildRepositorySystemSession(MavenExecutionRequest mavenExecutionRequest) throws ComponentLookupException {
        DefaultMaven defaultMaven = (DefaultMaven) lookup(Maven.class);
        return defaultMaven.newRepositorySession(mavenExecutionRequest);
    }

    public static PlexusContainer buildPlexusContainer(ClassLoader mavenClassLoader, ClassLoader parent) throws PlexusContainerException {
        DefaultContainerConfiguration conf = new DefaultContainerConfiguration();

        ClassWorld classWorld = new ClassWorld();

        ClassRealm classRealm = new ClassRealm( classWorld, "maven", mavenClassLoader );
        classRealm.setParentRealm( new ClassRealm( classWorld, "maven-parent",
                                                   parent == null ? Thread.currentThread().getContextClassLoader()
                                                                   : parent ) );
        conf.setRealm( classRealm );

        return buildPlexusContainer(conf);
    }

    private static PlexusContainer buildPlexusContainer(ContainerConfiguration containerConfiguration ) throws PlexusContainerException {
        DefaultPlexusContainer plexusContainer = new DefaultPlexusContainer( containerConfiguration );
        return plexusContainer;
    }

}
