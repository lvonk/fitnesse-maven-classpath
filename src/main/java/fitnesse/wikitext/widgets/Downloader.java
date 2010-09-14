/**
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fitnesse.wikitext.widgets;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class Downloader {

    public static Log log = LogFactory.getLog(Downloader.class);

    File localRepository;

    public List<String> getArtifactAndDependencies(String pomUrl) throws MavenEmbedderException,
            XmlPullParserException, IOException, DependencyResolutionRequiredException, DownloadException {

        File pom = downloadPom(pomUrl);

        MavenEmbedder embedder = new MavenEmbedder(createConfiguration());

        Model model = embedder.readModel(pom);
        File tmpPom = createPomWithProjectAsDependency(pom, model);

        DefaultMavenExecutionRequest defaultMavenExecutionRequest = new DefaultMavenExecutionRequest();
        defaultMavenExecutionRequest.setUpdateSnapshots(true);
        MavenExecutionResult mavenResult =
                embedder.readProjectWithDependencies(defaultMavenExecutionRequest.setPom(tmpPom));

        List compileClasspathElements = mavenResult.getProject().getCompileClasspathElements();
        // little hack for now to remove source dir
        // TODO: find a way to have only jars elements, not sources elements
        compileClasspathElements.remove(0);
        return compileClasspathElements;
    }

    private File createPomWithProjectAsDependency(File pom, Model model) throws IOException, XmlPullParserException {

        StringBuilder sb = new StringBuilder();
        sb.append("<project>\n");
        sb.append("  <modelVersion>4.0.0</modelVersion>\n");
        sb.append("  <groupId>org.fitnesse.tmp</groupId>\n");
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
        File userSettings = MavenEmbedder.DEFAULT_USER_SETTINGS_FILE;
        if (userSettings.exists() && userSettings.isFile()) {
            configuration.setUserSettingsFile(userSettings);
        }
    }

    void setGlobalSettingsIfPresents(Configuration configuration) {
        File globalSettings = MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE;
        if (globalSettings.exists() && globalSettings.isFile()) {
            configuration.setGlobalSettingsFile(globalSettings);
        }
    }

    File downloadPom(String pomUrl) throws DownloadException {
        String fileName = pomUrl.substring(pomUrl.lastIndexOf('/') + 1);
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "MavenClasspathWidgetTest/downloadedPomFiles");
        File res = new File(tmpDir, fileName);
        if (!res.exists() || fileName.contains("SNAPSHOT")) {
            try {
                downloadFile(pomUrl, res);
            } catch (DownloadException e) {
                if (res.exists()) {
                    // file has already been downloaded, we will use this one, should be correct
                    log.debug("Unable to download pom [" + pomUrl + "], using previous file");
                } else {
                    // unable to download this is a problem, throw exception
                    log.warn("Unable to download pom [" + pomUrl + "], and there is no local file");
                    throw e;
                }
            }
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
                ensureParentFileExist(res);
                FileUtils.fileWrite(res.getAbsolutePath(), method.getResponseBodyAsString(100000));
            }
        } catch (HttpException e) {
            throw new DownloadException("Impossible to retrieve " + pomUrl, e);
        } catch (IOException e) {
            throw new DownloadException("Impossible to retrieve " + pomUrl, e);
        }
    }

    private void ensureParentFileExist(File file) {
        File parent = file.getParentFile();
        if (!parent.exists()) {
            FileUtils.mkdir(parent.getAbsolutePath());
        }
    }

    public void overrideLocalRepository(File repository) {
        localRepository = repository;
    }

}
