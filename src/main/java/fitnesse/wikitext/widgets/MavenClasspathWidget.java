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

import static org.apache.maven.embedder.MavenEmbedder.validateConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import fitnesse.html.HtmlUtil;
import fitnesse.wiki.PageData;
import fitnesse.wikitext.WidgetBuilder;

public class MavenClasspathWidget extends ParentWidget implements WidgetWithTextArgument {

  static {
    PageData.classpathWidgetBuilder.addWidgetClass(MavenClasspathWidget.class);
  }

  private String pomFile;
  public static final String REGEXP = "^!pomFile [^\r\n]*";
  private static final Pattern pattern = Pattern.compile("^!pomFile (.*)");
  private Downloader downloader = new Downloader();

  public void setDownloader(Downloader downloader) {
    this.downloader = downloader;
  }

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
    // TODO: check that the link really exist
    if (pomFile.startsWith("http://")) {
      return;
    }
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

  private List<String> getMavenClasspath() throws MavenEmbedderException, DependencyResolutionRequiredException, XmlPullParserException, IOException, DownloadException {
    Configuration configuration = downloader.createConfiguration();
    ensureMavenConfigurationIsValid(configuration);

    List<String> classpathElements = getClasspathElements(configuration);
    return classpathElements;
  }

  private List<String> getClasspathElements(Configuration configuration) throws MavenEmbedderException, DependencyResolutionRequiredException, XmlPullParserException, IOException, DownloadException  {
    List<String> classpathElements;
    if (pomFile.startsWith("http://")) {
      classpathElements = downloader.getArtifactAndDependencies(pomFile);
    } else {
      MavenExecutionRequest request = createExecutionRequest(projectRootDirectory());
      classpathElements = getClasspathElements(configuration, request);
    }
    return classpathElements;
  }

  @Override
  public String render() throws MavenEmbedderException, DependencyResolutionRequiredException, XmlPullParserException, IOException {
    List<String> classpathElements;
    try {
      classpathElements = getMavenClasspath();
      String classpathForRender = "";
      for (String element : classpathElements) {
        classpathForRender += HtmlUtil.metaText("classpath: " + element) + HtmlUtil.BRtag;
      }
      return classpathForRender;
    } catch (DownloadException e) {
      return "Error : unable to download pom, check the url and connexion settings. url="+pomFile;
    }
  }

  private void ensureMavenConfigurationIsValid(Configuration configuration) {
    ConfigurationValidationResult validationResult = validateConfiguration(configuration);
    if (!validationResult.isValid()) {
      throw new IllegalStateException("Unable to create valid Maven Configuration.");
    }
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

  private List<String> getClasspathElements(Configuration configuration, MavenExecutionRequest request) throws MavenEmbedderException,
      DependencyResolutionRequiredException {
    MavenEmbedder embedder = new MavenEmbedder(configuration);
    MavenExecutionResult executionResult = embedder.readProjectWithDependencies(request);
    List<String> classpathElements = executionResult.getProject().getCompileClasspathElements();
    return classpathElements;
  }

  private File projectRootDirectory() {
    File projectDirectory = new File(pomFile).getParentFile();
    return projectDirectory;
  }

  private MavenExecutionRequest createExecutionRequest(File projectDirectory) {
    MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory(projectDirectory).setPomFile(pomFile);
    return request;
  }

}
