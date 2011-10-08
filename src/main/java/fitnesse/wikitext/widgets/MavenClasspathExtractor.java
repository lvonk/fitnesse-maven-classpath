package fitnesse.wikitext.widgets;

import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.util.List;

/**
 * Utiity class to extract classpath elements from Maven projects. Heavily based on code copied from Jenkin's Maven
 * support.
 */
public class MavenClasspathExtractor {

	public final static String DEFAULT_SCOPE = "test";
	
    private File userSettingsFile;
    private File globalSettingsFile;

	public List<String> extractClasspathEntries(File pomFile) {
		return extractClasspathEntries(pomFile, DEFAULT_SCOPE);
	}

    public List<String> extractClasspathEntries(File pomFile, String scope) throws MavenClasspathExtractionException {

        try {
            MavenRequest mavenRequest = mavenConfiguration();
            mavenRequest.setResolveDependencies(true);
            mavenRequest.setBaseDirectory(pomFile.getParent());
            mavenRequest.setPom(pomFile.getAbsolutePath());

            DependencyResolvingMavenEmbedder dependencyResolvingMavenEmbedder =
                    new DependencyResolvingMavenEmbedder(getClass().getClassLoader(), mavenRequest);

            ProjectBuildingResult projectBuildingResult = dependencyResolvingMavenEmbedder.buildProject(pomFile);
            return getClasspathForScope(projectBuildingResult, scope);

        } catch (MavenEmbedderException mee) {
            throw new MavenClasspathExtractionException(mee);
        } catch (ComponentLookupException cle) {
            throw new MavenClasspathExtractionException(cle);
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
    protected MavenRequest mavenConfiguration() throws MavenEmbedderException, ComponentLookupException {
        MavenRequest mavenRequest = new MavenRequest();

        if (userSettingsFile != null && userSettingsFile.exists()) {
            mavenRequest.setUserSettingsFile(userSettingsFile.getAbsolutePath());
        }
        if (globalSettingsFile != null && globalSettingsFile.exists()) {
            mavenRequest.setGlobalSettingsFile(globalSettingsFile.getAbsolutePath());
        }

        DependencyResolvingMavenEmbedder mavenEmbedder = new DependencyResolvingMavenEmbedder(MavenClasspathExtractor.class.getClassLoader(), mavenRequest);
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
