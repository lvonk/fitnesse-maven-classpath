package fitnesse.wikitext.widgets;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class Downloader {

	File localRepository;

  
	public List<String> getArtifactAndDependencies(String pomUrl) throws DownloadException, Exception {

		File pom = downloadPom(pomUrl);

		MavenEmbedder embedder = new MavenEmbedder(createConfiguration());

		Model model = embedder.readModel(pom);
		File tmpPom = createPomWithProjectAsDependency(pom, model);

		MavenExecutionResult mavenResult = embedder.readProjectWithDependencies(new DefaultMavenExecutionRequest().setPom(tmpPom));

		List compileClasspathElements = mavenResult.getProject().getCompileClasspathElements();
		// little hack for now to remove source dir
		//TODO: find a way to have only jars elements, not sources elements
		compileClasspathElements.remove(0);
		return compileClasspathElements;
	}

	private File createPomWithProjectAsDependency(File pom, Model model) throws IOException, XmlPullParserException {

		StringBuilder sb = new StringBuilder();
		sb.append("<project>\n");
		sb.append("  <modelVersion>4.0.0</modelVersion>\n");
		sb.append("  <groupId>com.octo.tmp</groupId>\n");
		sb.append("  <artifactId>").append(model.getArtifactId()).append("-for-dependencies</artifactId>\n");
		sb.append("  <version>0.0.1-SNAPSHOT</version>\n");
		sb.append("  <dependencies>\n");
		sb.append("    <dependency>\n");
		sb.append("      <groupId>").append(model.getGroupId()).append("</groupId>\n");
		sb.append("      <artifactId>").append(model.getArtifactId()).append("</artifactId>\n");
		sb.append("      <version>").append(model.getVersion()).append("</version>\n");
		sb.append("    </dependency>\n");
		sb.append("  </dependencies>\n");
		sb.append("</project>\n");

		File file = new File(pom.getParentFile(), model.getArtifactId() + "-for-dependencies-0.0.1-SNAPSHOT.pom");
		FileUtils.fileWrite(file.getAbsolutePath(), sb.toString());
		return file;
	}

	Configuration createConfiguration() {
		Configuration configuration = new DefaultConfiguration();
		configuration.setClassLoader(Thread.currentThread().getContextClassLoader()).setMavenEmbedderLogger(
				new MavenEmbedderConsoleLogger());

		setGlobalSettingsIfPresents(configuration);
		setUserSettingsIfPresents(configuration);
		if (localRepository != null) {
			configuration.setLocalRepository(localRepository);
		}
		return configuration;
	}

	void setUserSettingsIfPresents(Configuration configuration) {
		String m2Home = System.getProperty("user.home");
		if (m2Home != null) {
			File userSettings = new File(m2Home, ".m2/settings.xml");
			configuration.setUserSettingsFile(userSettings);
		}
	}

	void setGlobalSettingsIfPresents(Configuration configuration) {
		String m2Home = System.getenv("M2_HOME");
		if (m2Home != null) {
			File globalSettings = new File(m2Home, "conf/settings.xml");
			configuration.setGlobalSettingsFile(globalSettings);
		}
	}

	File downloadPom(String pomUrl) throws DownloadException {
		String fileName = pomUrl.substring(pomUrl.lastIndexOf('/') + 1);
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		File res = new File(tmpDir, fileName);
		if (!res.exists() || fileName.contains("SNAPSHOT")) {
			downloadFile(pomUrl, res);
		}
		return res;
	}

	private void downloadFile(String pomUrl, File res) throws DownloadException {
		try {
			HttpClient httpClient = new HttpClient();
			GetMethod method = new GetMethod(pomUrl);
			int statusCode = httpClient.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				throw new DownloadException("Impossible to retrieve " + pomUrl + ", error code: " + statusCode);
			} else {
				FileUtils.fileWrite(res.getAbsolutePath(), method.getResponseBodyAsString(100000));
			}
		} catch (HttpException e) {
			throw new DownloadException("Impossible to retrieve " + pomUrl, e);
		} catch (IOException e) {
			throw new DownloadException("Impossible to retrieve " + pomUrl, e);
		}
	}

	public void overrideLocalRepository(File repository) {
		localRepository = repository;
	}

}
